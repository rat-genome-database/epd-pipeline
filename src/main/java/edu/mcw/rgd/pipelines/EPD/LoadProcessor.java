package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.GenomicElement;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordProcessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author mtutaj
 * Date: 1/13/12
 */
public class LoadProcessor extends RecordProcessor {

    private Dao dao;

    protected final Log newPromoterLogger = LogFactory.getLog("insert_promoter");

    @Override
    public void process(PipelineRecord pipelineRecord) throws Exception {

        EPDRecord rec = (EPDRecord) pipelineRecord;
        GenomicElement promoter = rec.getPromoter();

        if( rec.isFlagSet("LOAD_SKIP") ) {
            getSession().incrementCounter("SPECIES_SKIPPED_OTHER", 1);
            return;
        }

        String speciesName = SpeciesType.getCommonName(promoter.getSpeciesTypeKey()).toUpperCase();
        getSession().incrementCounter("SPECIES_"+speciesName, 1);

        if( rec.isFlagSet("LOAD_INSERT") ) {
            getSession().incrementCounter("PROMOTERS_INSERTED", 1);

            // create new promoter with promoter rgd id
            if( dao.insertPromoter(promoter, promoter.getSpeciesTypeKey()) != 0 ) {
                newPromoterLogger.info("INSERT "+promoter.dump("|"));
            }
        }
        else if( rec.isFlagSet("LOAD_UPDATE") ) {
            getSession().incrementCounter("PROMOTERS_MATCHING", 1);

            dao.updateLastModifiedDate(promoter.getRgdId());

            if( rec.isFlagSet("FULL_UPDATE") ) {
                dao.updatePromoter(promoter);
                getSession().incrementCounter("PROMOTERS_UPDATED", 1);
            }
        }

        // sync map positions and position statistics
        String notes = "created by EPD pipeline for " + promoter.getSymbol() + ", " + new java.util.Date();

        // sync maps_data
        for( MapsDataCollection md: rec.mds.values() ) {
            md.sync(promoter.getRgdId(), notes);
            md.incrementCounters(getSession(), "MAPS_DATA_");
        }

        // sync expression data
        rec.getAttrs().sync(promoter.getRgdId(), notes);
        rec.getAttrs().incrementCounters(getSession(), "EXPRESSION_DATA_");

        // sync xdb ids
        rec.getXdbIds().sync(promoter.getRgdId(), getDao(), getSession());

        // sync associations of promoter to gene
        rec.getGeneAssocs().sync(promoter.getRgdId(), getDao(), getSession());

        rec.getAlternativePromoterAssocs().sync(promoter.getRgdId(), notes);
        rec.getAlternativePromoterAssocs().incrementCounters(getSession(), "ALT_PROMOTER_ASSOC_");

        rec.getNeighboringPromoterAssocs().sync(promoter.getRgdId(), notes);
        rec.getNeighboringPromoterAssocs().incrementCounters(getSession(), "NEIGHBOR_PROMOTER_ASSOC_");

        // sync sequences
        rec.getSeq().sync(promoter.getRgdId(), getDao(), getSession());
    }

    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }
}
