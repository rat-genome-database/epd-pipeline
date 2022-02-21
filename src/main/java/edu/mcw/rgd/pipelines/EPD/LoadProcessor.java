package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.Association;
import edu.mcw.rgd.datamodel.GenomicElement;
import edu.mcw.rgd.datamodel.Sequence;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.CounterPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

/**
 * @author mtutaj
 * Date: 1/13/12
 */
public class LoadProcessor {

    private Dao dao;
    private String srcPipeline;
    private CounterPool counters;

    protected final Logger newPromoterLogger = LogManager.getLogger("insert_promoter");

    public void process(EPDRecord rec) throws Exception {

        GenomicElement promoter = rec.getPromoter();

        if( rec.isFlagSet("LOAD_SKIP") ) {
            getCounters().increment("MATCH_TIER0_NO_MATCH_OTHER_SPECIES");
            getCounters().increment("SPECIES_SKIPPED_OTHER");
            return;
        }

        String speciesName = SpeciesType.getCommonName(promoter.getSpeciesTypeKey()).toUpperCase();
        getCounters().increment("SPECIES_"+speciesName);

        if( rec.isFlagSet("LOAD_INSERT") ) {
            getCounters().increment("PROMOTERS_INSERTED");

            // create new promoter with promoter rgd id
            if( dao.insertPromoter(promoter, promoter.getSpeciesTypeKey()) != 0 ) {
                newPromoterLogger.info("INSERT "+promoter.dump("|"));
            }
        }
        else if( rec.isFlagSet("LOAD_UPDATE") ) {
            getCounters().increment("PROMOTERS_MATCHING");

            dao.updateLastModifiedDate(promoter.getRgdId());

            if( rec.isFlagSet("FULL_UPDATE") ) {
                dao.updatePromoter(promoter);
                getCounters().increment("PROMOTERS_UPDATED");
            }
        }

        // sync map positions and position statistics
        String notes = "created by EPD pipeline for " + promoter.getSymbol() + ", " + new java.util.Date();

        rec.setRgdIdForMapData(promoter.getRgdId(), notes);
        rec.setRgdIdForExpressionData(promoter.getRgdId(), notes);
        rec.setRgdIdForXdbIds(promoter.getRgdId());

        qcAlternativePromoters(rec, promoter.getRgdId());
        qcNeighboringPromoters(rec, promoter.getRgdId());

        // sync sequences
        qcSequences(rec, promoter.getRgdId());

        // create promoter_to_gene association
        if( rec.getGene()!=null ) {
            Association assoc = new Association();
            assoc.setAssocType("promoter_to_gene");
            assoc.setDetailRgdId(rec.getGene().getRgdId());
            assoc.setMasterRgdId(promoter.getRgdId());
            assoc.setCreationDate(new Date());
            assoc.setSrcPipeline(getSrcPipeline());
            GeneAssociationCollection.getInstance().addIncoming(assoc);
        }
    }

    void qcAlternativePromoters(EPDRecord rec, int promoterRgdId) throws Exception {

        // create a list of incoming alternative promoters
        for( String accId: rec.getAltPromoters() ) {
            GenomicElement ge = dao.getPromoterById(accId, rec.getPromoter().getSpeciesTypeKey(), getSrcPipeline());
            if( ge==null ) {
                getCounters().increment("IGNORED_ALTERNATIVE_PROMOTERS");
            }
            else {
                Association assoc = new Association();
                assoc.setAssocType("alternative_promoter");
                assoc.setSrcPipeline(getSrcPipeline());
                assoc.setMasterRgdId(promoterRgdId);
                assoc.setDetailRgdId(ge.getRgdId());
                assoc.setCreationDate(new Date());
                AlternativePromoterCollection.getInstance().addIncoming(assoc);
            }
        }
    }

    void qcNeighboringPromoters(EPDRecord rec, int promoterRgdId) throws Exception {

        // create a list of incoming neighboring promoters
        for( String accId: rec.getNeighboringPromoters() ) {
            GenomicElement ge = dao.getPromoterById(accId, rec.getPromoter().getSpeciesTypeKey(), getSrcPipeline());
            if( ge==null ) {
                getCounters().increment("IGNORED_NEIGHBORING_PROMOTERS");
            }
            else {
                Association assoc = new Association();
                assoc.setAssocType("neighboring_promoter");
                assoc.setSrcPipeline(getSrcPipeline());
                assoc.setMasterRgdId(promoterRgdId);
                assoc.setDetailRgdId(ge.getRgdId());
                assoc.setCreationDate(new Date());
                NeighborPromoterCollection.getInstance().addIncoming(assoc);
            }
        }
    }

    void qcSequences(EPDRecord rec, int rgdId) throws Exception {
        for( Sequence seq: rec.seqs ) {
            seq.setRgdId(rgdId);
            SequenceCollection.getInstance().addIncoming(seq);
        }
    }

    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public String getSrcPipeline() {
        return srcPipeline;
    }

    public void setSrcPipeline(String srcPipeline) {
        this.srcPipeline = srcPipeline;
    }

    public CounterPool getCounters() {
        return counters;
    }

    public void setCounters(CounterPool counters) {
        this.counters = counters;
    }
}
