package edu.mcw.rgd.pipelines.EPD;


import edu.mcw.rgd.datamodel.ExpressionData;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author mtutaj
 * @since 4/9/12
 */
public class ExpressionDataCollection {

    // THREAD SAFE SINGLETON -- start
    // private instance, so that it can be accessed by only by getInstance() method
    private static ExpressionDataCollection instance;

    private ExpressionDataCollection() {
        // private constructor
    }

    //synchronized method to control simultaneous access
    synchronized public static ExpressionDataCollection getInstance() {
        if (instance == null) {
            // if instance is null, initialize
            instance = new ExpressionDataCollection();
        }
        return instance;
    }
    // THREAD SAFE SINGLETON -- end
    //



    Logger log = LogManager.getLogger("status");

    private final Set<ExpressionData> incoming = new HashSet<>();

    public void addIncoming(ExpressionData md) {

        // there is only one instance of this class
        synchronized (incoming) {
            incoming.add(md);
        }
    }

    synchronized public void qc(Dao dao, String[] sources) throws Exception {

        List<ExpressionData> expressionDataInRgd = dao.getExpressionData(sources);

        // determine expression data for insertion
        Collection<ExpressionData> forInsert = CollectionUtils.subtract(incoming, expressionDataInRgd);

        // determine expression data for deletion
        Collection<ExpressionData> forDelete = CollectionUtils.subtract(expressionDataInRgd, incoming);

        Collection<ExpressionData> matching = CollectionUtils.intersection(expressionDataInRgd, incoming);


        if( !forInsert.isEmpty() ) {
            dao.insertExpressionData(forInsert);
            log.info("EXPRESSION_DATA_INSERTED: "+forInsert.size());
        }

        if( !forDelete.isEmpty() ) {
            dao.deleteExpressionData(forDelete);
            log.info("EXPRESSION_DATA_DELETED: "+forDelete.size());
        }

        int matchingData = matching.size();
        if( matchingData!=0 ) {
            log.info("EXPRESSION_DATA_MATCHING: "+matchingData);
        }
    }

}
