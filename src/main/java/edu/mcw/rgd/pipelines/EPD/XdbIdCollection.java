package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.XdbId;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author mtutaj
 * Date: 4/13/12
 * list of XdbIds present in incoming data
 */
public class XdbIdCollection {

    // THREAD SAFE SINGLETON -- start
    // private instance, so that it can be accessed by only by getInstance() method
    private static XdbIdCollection instance;

    private XdbIdCollection() {
        // private constructor
    }

    //synchronized method to control simultaneous access
    synchronized public static XdbIdCollection getInstance() {
        if (instance == null) {
            // if instance is null, initialize
            instance = new XdbIdCollection();
        }
        return instance;
    }
    // THREAD SAFE SINGLETON -- end


    Logger log = Logger.getLogger("status");

    private final Set<XdbId> incoming = new HashSet<>();

    public void addIncoming(XdbId id) {

        // there is only one instance of this class
        synchronized (incoming) {
            incoming.add(id);
        }
    }

    synchronized public void qc(Dao dao, String[] sources, String staleIdsDeleteThreshold) throws Exception {

        List<XdbId> xdbIdsInRgd = dao.getXdbIds(sources);

        // determine xdb ids for insertion
        Collection<XdbId> forInsert = CollectionUtils.subtract(incoming, xdbIdsInRgd);

        // determine xdb ids for deletion
        Collection<XdbId> forDelete = CollectionUtils.subtract(xdbIdsInRgd, incoming);

        Collection<XdbId> matching = CollectionUtils.intersection(xdbIdsInRgd, incoming);


        if( !forInsert.isEmpty() ) {
            dao.insertXdbIds(forInsert);
            log.info("XDB_IDS_INSERTED: "+forInsert.size());
        }

        if( !forDelete.isEmpty() ) {
            dao.deleteXdbIds(forDelete, sources, staleIdsDeleteThreshold);
            log.info("XDB_IDS_DELETED: "+forDelete.size());
        }

        int matchingXdbIds = matching.size();
        if( matchingXdbIds!=0 ) {
            dao.updateLastModDateForXdbIds(matching);
            log.info("XDB_IDS_MATCHING: "+matchingXdbIds);
        }
    }
}
