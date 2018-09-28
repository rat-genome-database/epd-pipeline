package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.pipelines.PipelineSession;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mtutaj
 * Date: 4/13/12
 * list of XdbIds present in incoming data
 */
public class XdbIdCollection {

    private List<XdbId> incomingIds = new ArrayList<>();
    private List<XdbId> forInsertIds = new ArrayList<>();
    private List<XdbId> forDeleteIds = new ArrayList<>();
    private List<XdbId> matchingIds = new ArrayList<>();

    public List<String> getAccIds(int xdbKey) {
        List<String> accIds = new ArrayList<>();
        for( XdbId xdbId: incomingIds ) {
            if( xdbId.getXdbKey()==xdbKey ) {
                accIds.add(xdbId.getAccId());
            }
        }
        return accIds;
    }

    public void addIncomingObject(XdbId xdbId) {
        incomingIds.add(xdbId);
    }

    public void qc(int rgdId, String srcPipeline, Dao dao) throws Exception {

        for( XdbId id: incomingIds ) {
            id.setRgdId(rgdId);
        }

        List<XdbId> inRgdIds = dao.getXdbIdsByRgdId(rgdId, srcPipeline);

        // determine new ids
        for( XdbId idIncoming: incomingIds ) {

            XdbId idInRgdFound = null;
            for( XdbId idInRgd: inRgdIds ) {
                if( idInRgd.equals(idIncoming) ) {
                    idInRgdFound = idInRgd;
                    break;
                }
            }

            if( idInRgdFound==null ) {
                forInsertIds.add(idIncoming);
            } else {
                matchingIds.add(idInRgdFound);
            }
        }

        // determine to be deleted xdb ids
        for( XdbId idInRgd: inRgdIds ) {

            boolean inRgdIdMatchesIncoming = false;
            for( XdbId idIncoming: incomingIds ) {
                if( idInRgd.equals(idIncoming) ) {
                    inRgdIdMatchesIncoming = true;
                    break;
                }
            }

            if( !inRgdIdMatchesIncoming ) {
                forDeleteIds.add(idInRgd);
            }
        }
    }

    public void sync(int rgdId, Dao dao, PipelineSession session) throws Exception {

        if( !forInsertIds.isEmpty() ) {
            for( XdbId id: forInsertIds ) {
                id.setRgdId(rgdId);
            }
            dao.insertXdbIds(forInsertIds);
            session.incrementCounter("XDB_IDS_INSERTED", forInsertIds.size());
        }

        if( !forDeleteIds.isEmpty() ) {
            dao.deleteXdbIds(forDeleteIds);
            session.incrementCounter("XDB_IDS_DELETED", forDeleteIds.size());
        }

        if( !matchingIds.isEmpty() ) {
            dao.updateLastModDateForXdbIds(matchingIds);
            session.incrementCounter("XDB_IDS_MATCHING", matchingIds.size());
        }
    }
}
