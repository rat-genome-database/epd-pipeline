package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.CounterPool;
import edu.mcw.rgd.process.Utils;

import java.util.*;


/**
 * @author mtutaj
 * Date: 1/13/12
 */
public class QCProcessor {

    private Dao dao;
    private String soAccId;
    private String srcPipeline;
    private CounterPool counters;
    private int maxPromoter2GeneDistance = 10000;

    public void process(EPDRecord rec) throws Exception {

        GenomicElement promoter = rec.getPromoter(); // incoming data promoter

        // skip qc for promoters of unknown species
        if( promoter.getSpeciesTypeKey() == SpeciesType.ALL ) {
            rec.setFlag("LOAD_SKIP");
            return;
        }
        promoter.setSoAccId(getSoAccId());

        // match promoter by promoter id
        GenomicElement promoterInRgd = getDao().getPromoterById(promoter.getSymbol(), promoter.getSpeciesTypeKey(), getSrcPipeline());
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
                !Utils.stringsAreEqual(promoter.getNotes(), promoterInRgd.getNotes()) ) {

                rec.setFlag("FULL_UPDATE");
            }
        }

        qcGeneIds(rec);
    }


    // first match genes by position; if there is only one gene hit, use that gene
    // if there are multiple ones, analyse
    void qcGeneIds(EPDRecord rec) throws Exception {

        // try to match REFSEQ nucleotide ids first
        int[] xdbKeys = { XdbId.XDB_KEY_GENEBANKNU, XdbId.XDB_KEY_ENSEMBL_GENES, XdbId.XDB_KEY_MGD, XdbId.XDB_KEY_UNIPROT, XdbId.XDB_KEY_OMIM };
        List<String> ids = rec.getAccIds(xdbKeys);
        // finally match by gene ids
        ids.addAll(rec.getGeneIds());

        for( String geneId: ids ) {

            if( matchGene(geneId, rec) )
                break;
        }
    }

    boolean matchGene(String geneId, EPDRecord rec) throws Exception {

        rec.setGene(null);

        // match by REFSEQ nucleotide
        List<Gene> genesByNucleotideId = dao.getGenesByNucleotideId(geneId, rec.getPromoter().getSpeciesTypeKey());
        removeGenesWithNonMatchingPositions(genesByNucleotideId, rec);
        if( !genesByNucleotideId.isEmpty() ) {

            rec.setGene(genesByNucleotideId.get(0));
            getCounters().increment("MATCH_TIER1_BY_REFSEQ_ID");

            if( genesByNucleotideId.size()>1 ) {
                getCounters().increment("MULTIMATCH_BY_REFSEQ_ID");
            }
            return true;
        }

        // match by ensembl gene
        List<Gene> genesByEnsemblId = dao.getGenesByEnsemblId(geneId, rec.getPromoter().getSpeciesTypeKey());
        removeGenesWithNonMatchingPositions(genesByEnsemblId, rec);
        if( !genesByEnsemblId.isEmpty() ) {

            rec.setGene(genesByEnsemblId.get(0));
            getCounters().increment("MATCH_TIER2_BY_ENSEMBL_ID");

            if( genesByEnsemblId.size()>1 ) {
                getCounters().increment("MULTIMATCH_BY_ENSEMBL_ID");
            }
            return true;
        }

        // match by swissprot
        List<Gene> genesByProteinId = dao.getGenesByProteinId(geneId, rec.getPromoter().getSpeciesTypeKey());
        removeGenesWithNonMatchingPositions(genesByProteinId, rec);
        if( !genesByProteinId.isEmpty() ) {

            rec.setGene(genesByProteinId.get(0));
            getCounters().increment("MATCH_TIER3_BY_UNIPROT_ID");

            if( genesByProteinId.size()>1 ) {
                getCounters().increment("MULTIMATCH_BY_UNIPROT_ID");
            }
            return true;
        }

        // match by MGD Id
        List<Gene> genesByMgdId = dao.getGenesByMgdId(geneId, rec.getPromoter().getSpeciesTypeKey());
        removeGenesWithNonMatchingPositions(genesByMgdId, rec);
        if( !genesByMgdId.isEmpty() ) {

            rec.setGene(genesByMgdId.get(0));
            getCounters().increment("MATCH_TIER4_BY_MGD_ID");

            if( genesByMgdId.size()>1 ) {
                getCounters().increment("MULTIMATCH_BY_MGD_ID");
            }
            return true;
        }

        // match by symbol and optionally position
        List<Gene> genesBySymbol = dao.getGenesBySymbol(geneId, rec.getPromoter().getSpeciesTypeKey());
        removeGenesWithNonMatchingPositions(genesBySymbol, rec);
        if( !genesBySymbol.isEmpty() ) {

            rec.setGene(genesBySymbol.get(0));
            getCounters().increment("MATCH_TIER5_BY_GENE_SYMBOL");

            if( genesBySymbol.size()>1 ) {
                getCounters().increment("MULTIMATCH_BY_GENE_SYMBOL");
            }
            return true;
        }


        // match by gene alias
        List<Gene> genesByAlias = dao.getGenesByAlias(geneId, rec.getPromoter().getSpeciesTypeKey());
        removeGenesWithNonMatchingPositions(genesByAlias, rec);
        if( !genesByAlias.isEmpty() ) {

            rec.setGene(genesByAlias.get(0));
            getCounters().increment("MATCH_TIER6_BY_GENE_ALIAS");

            if( genesByAlias.size()>1 ) {
                getCounters().increment("MULTIMATCH_BY_GENE_ALIAS");
            }
            return true;
        }

        getCounters().increment("MATCH_TIER7_NO_MATCH");
        return false;
    }

    void removeGenesWithNonMatchingPositions(List<Gene> genes, EPDRecord rec) throws Exception {

        // determine available map keys
        Set<Integer> mapKeys = new HashSet<>();
        for( MapData md: rec.getMapData() ) {
            mapKeys.add(md.getMapKey());
        }

        for( int mapKey: mapKeys ) {
            Iterator<Gene> it = genes.iterator();
            while (it.hasNext()) {
                Gene gene = it.next();
                if (!genePositionMatches(gene, rec.getMapData(), mapKey)) {
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

    public CounterPool getCounters() {
        return counters;
    }

    public void setCounters(CounterPool counters) {
        this.counters = counters;
    }
}
