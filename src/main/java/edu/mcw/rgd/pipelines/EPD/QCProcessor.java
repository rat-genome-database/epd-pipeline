package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordProcessor;
import edu.mcw.rgd.process.Utils;

import java.util.*;


/**
 * @author mtutaj
 * Date: 1/13/12
 */
public class QCProcessor extends RecordProcessor {

    private Dao dao;
    private String soAccId;
    private String srcPipeline;
    private int maxPromoter2GeneDistance = 10000;
    private int qcThreadCount;

    @Override
    public void process(PipelineRecord pipelineRecord) throws Exception {

        EPDRecord rec = (EPDRecord) pipelineRecord;
        GenomicElement promoter = rec.getPromoter(); // incoming data promoter

        // skip qc for promoters of unknown species
        if( promoter.getSpeciesTypeKey() == SpeciesType.ALL ) {
            rec.setFlag("LOAD_SKIP");
            return;
        }
        promoter.setSoAccId(getSoAccId());

        // match promoter by promoter id
        GenomicElement promoterInRgd = getDao().getPromoterById(promoter.getSymbol());
        if( promoterInRgd==null ) {
            // new promoter
            rec.setFlag("LOAD_INSERT");
        }
        else {
            promoter.setRgdId(promoterInRgd.getRgdId());
            rec.setFlag("LOAD_UPDATE");

            // if any of promoter properties changed, set flag full update
            if( !Utils.stringsAreEqual(promoter.getObjectType(), promoterInRgd.getObjectType()) ||
                !Utils.stringsAreEqual(promoter.getName(), promoterInRgd.getName()) ||
                !Utils.stringsAreEqual(promoter.getDescription(), promoterInRgd.getDescription()) ||
                !Utils.stringsAreEqual(promoter.getSource(), promoterInRgd.getSource()) ||
                !Utils.stringsAreEqual(promoter.getSoAccId(), promoterInRgd.getSoAccId()) ||
                !Utils.stringsAreEqual(promoter.getNotes(), promoterInRgd.getNotes()) ||
                true ) {
                rec.setFlag("FULL_UPDATE");
            }
        }

        // run QC for expression data
        rec.getAttrs().qc(promoter.getRgdId());

        qcGeneIds(rec);

        // run QC for xdbids
        rec.getXdbIds().qc(promoter.getRgdId(), getSrcPipeline(), getDao());

        // run QC for associations
        rec.getGeneAssocs().qc(promoter.getRgdId(), getDao());

        // run QC for genomic position
        for( MapsDataCollection md: rec.mds.values() ) {
            md.qc(promoter.getRgdId());
        }

        // run QC for sequence
        rec.getSeq().qc(promoter.getRgdId(), getDao());

        qcAlternativePromoters(rec);
        qcNeighboringPromoters(rec);
    }

    void qcAlternativePromoters(EPDRecord rec) throws Exception {

        // is there anything to do?
        if( rec.getAltPromoters()==null )
            return;

        // create a list of incoming alternative promoters
        for( String accId: rec.getAltPromoters() ) {
            GenomicElement ge = dao.getPromoterById(accId);
            if( ge==null ) {
                getSession().incrementCounter("IGNORED_ALTERNATIVE_PROMOTERS", 1);
            }
            else {
                Association assoc = new Association();
                assoc.setAssocType("alternative_promoter");
                assoc.setAssocSubType(rec.getAltPromoterInfo());
                assoc.setSrcPipeline(getSrcPipeline());
                assoc.setDetailRgdId(ge.getRgdId());
                assoc.setCreationDate(new Date());
                rec.getAlternativePromoterAssocs().getIncomingList().add(assoc);
            }
        }

        // qc list of alternative promoters
        rec.getAlternativePromoterAssocs().qc(rec.getPromoter().getRgdId());
    }

    void qcNeighboringPromoters(EPDRecord rec) throws Exception {

        // is there anything to do?
        if( rec.getNeighboringPromoters()==null )
            return;

        // create a list of incoming neighboring promoters
        for( String accId: rec.getNeighboringPromoters() ) {
            GenomicElement ge = dao.getPromoterById(accId);
            if( ge==null ) {
                getSession().incrementCounter("IGNORED_NEIGHBORING_PROMOTERS", 1);
            }
            else {
                Association assoc = new Association();
                assoc.setAssocType("neighboring_promoter");
                assoc.setSrcPipeline(getSrcPipeline());
                assoc.setDetailRgdId(ge.getRgdId());
                assoc.setCreationDate(new Date());
                rec.getNeighboringPromoterAssocs().getIncomingList().add(assoc);
            }
        }

        // qc list of neighboring promoters
        rec.getNeighboringPromoterAssocs().qc(rec.getPromoter().getRgdId());
    }

    // first match genes by position; if there is only one gene hit, use that gene
    // if there are multiple ones, analyse
    void qcGeneIds(EPDRecord rec) throws Exception {

        // try to match REFSEQ nucleotide ids first
        List<String> ids = new ArrayList<String>();
        ids.addAll(rec.getXdbIds().getAccIds(XdbId.XDB_KEY_GENEBANKNU));
        // then by MGD ids
        ids.addAll(rec.getXdbIds().getAccIds(XdbId.XDB_KEY_MGD));
        // then by SWISS-PROT ids
        ids.addAll(rec.getXdbIds().getAccIds(XdbId.XDB_KEY_UNIPROT));
        // then by OMIM ids
        ids.addAll(rec.getXdbIds().getAccIds(XdbId.XDB_KEY_OMIM));
        // finally match by gene ids
        ids.addAll(rec.getGeneIds());

        for( String geneId: ids ) {

            if( matchGene(geneId, rec) )
                break;
        }

        if( rec.getGene()!=null ) {
            // create association for every gene id
            Association assoc = new Association();
            assoc.setAssocType("promoter_to_gene");
            assoc.setDetailRgdId(rec.getGene().getRgdId());
            assoc.setCreationDate(new Date());
            assoc.setSrcPipeline(getSrcPipeline());
            rec.getGeneAssocs().addIncomingObject(assoc);
        } else {

            getSession().incrementCounter("GENEID_NO_MATCH", 1);
        }
    }

    boolean matchGene(String geneId, EPDRecord rec) throws Exception {

        // match by REFSEQ nucleotide
        List<Gene> genesByNucleotideId = dao.getGenesByNucleotideId(geneId, rec.getPromoter().getSpeciesTypeKey());
        removeGenesWithNonMatchingPositions(genesByNucleotideId, rec);
        if( !genesByNucleotideId.isEmpty() ) {

            if( rec.getGene()==null ) {
                rec.setGene(genesByNucleotideId.get(0));
                getSession().incrementCounter("MATCH_BY_REFSEQ", 1);
                return true;
            }
        }

        // match by swissprot
        List<Gene> genesByProteinId = dao.getGenesByProteinId(geneId, rec.getPromoter().getSpeciesTypeKey());
        removeGenesWithNonMatchingPositions(genesByProteinId, rec);
        if( !genesByProteinId.isEmpty() ) {

            if( rec.getGene()==null ) {
                rec.setGene(genesByProteinId.get(0));
                getSession().incrementCounter("MATCH_BY_PROTEIN_ID", 1);
                return true;
            }
        }

        // match by MGD Id
        List<Gene> genesByMgdId = dao.getGenesByMgdId(geneId, rec.getPromoter().getSpeciesTypeKey());
        removeGenesWithNonMatchingPositions(genesByMgdId, rec);
        if( !genesByMgdId.isEmpty() ) {

            if( rec.getGene()==null ) {
                rec.setGene(genesByMgdId.get(0));
                getSession().incrementCounter("MATCH_BY_MGD_ID", 1);
                return true;
            }
        }

        // match by gene position
        /*
        List<Gene> genesByPos = null;
        for( MapData md: (List<MapData>) rec.getMapsData().getIncomingList() ) {
            genesByPos = dao.getGenesByPosition(md.getChromosome(), md.getStartPos()-10000, md.getStopPos()+10000, rec.getPromoter().getSpeciesTypeKey());
            if( !genesByPos.isEmpty() ) {
                if( genesByPos.size()==1 ) {
                    rec.setGene(genesByPos.get(0));
                    getSession().incrementCounter("MATCH_BY_GENE_POS", 1);
                    return true;
                }
                break;
            }
        }
        */

        // match by symbol and optionally position
        List<Gene> genesBySymbol = dao.getGenesBySymbol(geneId, rec.getPromoter().getSpeciesTypeKey());
        removeGenesWithNonMatchingPositions(genesBySymbol, rec);
        if( !genesBySymbol.isEmpty() ) {

            if( rec.getGene()==null ) {
                rec.setGene(genesBySymbol.get(0));
                getSession().incrementCounter("MATCH_BY_GENE_SYMBOL", 1);
                return true;
            }
        }


        // match by gene alias
        List<Gene> genesByAlias = dao.getGenesByAlias(geneId, rec.getPromoter().getSpeciesTypeKey());
        removeGenesWithNonMatchingPositions(genesByAlias, rec);
        if( !genesByAlias.isEmpty() ) {

            if( rec.getGene()==null ) {
                rec.setGene(genesByAlias.get(0));
                getSession().incrementCounter("MATCH_BY_GENE_ALIAS", 1);
                return true;
            }
        }

        return false;
    }

    void removeGenesWithNonMatchingPositions(List<Gene> genes, EPDRecord rec) throws Exception {

        for( MapsDataCollection md: rec.mds.values() ) {
            int mapKey = md.getMapKey();
            Iterator<Gene> it = genes.iterator();
            while (it.hasNext()) {
                Gene gene = it.next();
                if (!genePositionMatches(gene, md.getIncomingList(), mapKey)) {
                    it.remove();
                }
            }
        }
    }

    boolean genePositionMatches( Gene gene, List<MapData> incomingPos, int mapKey ) throws Exception {

        // get map positions for gene
        for( MapData mdGene: dao.getMapData(gene.getRgdId(), mapKey) ) {

            // see if every of incoming positions is within 10k range of the gene position
            for( MapData mdPromoter: incomingPos ) {

                // chromosome must be the same
                if( !Utils.stringsAreEqual(mdGene.getChromosome(), mdPromoter.getChromosome()) )
                    continue;
                // chromosomes are the same
                if( mdPromoter.getStopPos() >= mdGene.getStartPos()-this.getMaxPromoter2GeneDistance()
                   &&
                    mdPromoter.getStartPos() <= mdGene.getStopPos()+this.getMaxPromoter2GeneDistance() ) {
                    // position of promoter is within 10kb of gene
                    return true;
                }
            }
        }

        return false;
    }

    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public void setSoAccId(String soAccId) {
        this.soAccId = soAccId;
    }

    public String getSoAccId() {
        return soAccId;
    }

    public String getSrcPipeline() {
        return srcPipeline;
    }

    public void setSrcPipeline(String srcPipeline) {
        this.srcPipeline = srcPipeline;
    }

    public int getMaxPromoter2GeneDistance() {
        return maxPromoter2GeneDistance;
    }

    public void setMaxPromoter2GeneDistance(int maxPromoter2GeneDistance) {
        this.maxPromoter2GeneDistance = maxPromoter2GeneDistance;
    }

    public void setQcThreadCount(int qcThreadCount) {
        this.qcThreadCount = qcThreadCount;
    }

    public int getQcThreadCount() {
        return qcThreadCount;
    }
}
