package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.Association;
import edu.mcw.rgd.pipelines.PipelineSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mtutaj
 * @since 4/16/12
 * collection of associations between promoter and genes (type 'promoter_to_gene')
 */
public class GeneAssociationCollection {

    Log log = LogFactory.getLog("assoc_genes");

    private List<Association> incoming = new ArrayList<>();
    private List<Association> forInsert = new ArrayList<>();
    private List<Association> forDelete = new ArrayList<>();

    public void addIncomingObject(Association assoc) throws Exception {
        incoming.add(assoc);
    }

    public void qc(int rgdId, Dao dao) throws Exception {

        for( Association assoc: incoming ) {
            assoc.setMasterRgdId(rgdId);
        }

        List<Association> inRgdAssocs = dao.getAssociations(rgdId, "promoter_to_gene");

        // determine new associations
        for( Association assocIncoming: incoming ) {

            boolean incomingAssocIsInRgd = false;
            for( Association assocInRgd: inRgdAssocs ) {
                if( assocInRgd.equals(assocIncoming) ) {
                    incomingAssocIsInRgd = true;
                    break;
                }
            }

            if( !incomingAssocIsInRgd ) {
                forInsert.add(assocIncoming);
            }
        }

        // determine to be deleted associations
        for( Association assocInRgd: inRgdAssocs ) {

            boolean inRgdAssocMatchesIncoming = false;
            for( Association assocIncoming: incoming ) {
                if( assocInRgd.equals(assocIncoming) ) {
                    inRgdAssocMatchesIncoming = true;
                    break;
                }
            }

            if( !inRgdAssocMatchesIncoming ) {
                forDelete.add(assocInRgd);
            }
        }
    }

    public void sync(int rgdId, Dao dao, PipelineSession session) throws Exception {

        if( !forInsert.isEmpty() ) {
            for( Association assoc: forInsert ) {
                assoc.setMasterRgdId(rgdId);
                dao.insertAssociation(assoc);
                log.debug("INSERT "+assoc.dump("|"));
            }
            session.incrementCounter("GENE_ASSOC_INSERTED", forInsert.size());
        }

        if( !forDelete.isEmpty() ) {
            for( Association assoc: forDelete ) {
                log.debug("DELETE "+assoc.dump("|"));
                dao.deleteAssociation(assoc);
            }
            session.incrementCounter("GENE_ASSOC_DELETED", forDelete.size());
        }

        int matchingAssocs = incoming.size() - forInsert.size();
        if( matchingAssocs!=0 ) {
            session.incrementCounter("GENE_ASSOC_MATCHED", matchingAssocs);
        }
    }

}
