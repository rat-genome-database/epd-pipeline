package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.Association;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author mtutaj
 * @since 4/16/12
 * collection of associations between promoter and genes (type 'promoter_to_gene')
 */
public class GeneAssociationCollection {

    // THREAD SAFE SINGLETON -- start
    // private instance, so that it can be accessed by only by getInstance() method
    private static GeneAssociationCollection instance;

    private GeneAssociationCollection() {
        // private constructor
    }

    //synchronized method to control simultaneous access
    synchronized public static GeneAssociationCollection getInstance() {
        if (instance == null) {
            // if instance is null, initialize
            instance = new GeneAssociationCollection();
        }
        return instance;
    }
    // THREAD SAFE SINGLETON -- end


    Logger log = Logger.getLogger("status");

    private final Set<Association> incoming = new HashSet<>();

    public void addIncoming(Association assoc) throws Exception {

        if( assoc.getDetailRgdId()==0 || assoc.getMasterRgdId()==0 ) {
            throw new Exception("unexpected1");
        }
        if( !assoc.getAssocType().equals("promoter_to_gene") ) {
            throw new Exception("unexpected2");
        }

        // there is only one instance of this class
        synchronized (incoming) {
            incoming.add(assoc);
        }
    }

    synchronized public void qc(Dao dao, String[] sources) throws Exception {

        List<Association> inRgdAssocs = new ArrayList<>();
        for( String source: sources ) {
            inRgdAssocs.addAll(dao.getAssociations("promoter_to_gene", source));
        }

        // determine new associations for insertion
        List<Association> forInsert = new ArrayList<>(incoming);
        forInsert.removeAll(inRgdAssocs);

        // determine new associations for deletion
        List<Association> forDelete = new ArrayList<>(inRgdAssocs);
        forDelete.removeAll(incoming);

        List<Association> matching = new ArrayList<>(inRgdAssocs);
        matching.retainAll(incoming);


        // update the database
        if( !forInsert.isEmpty() ) {
            for( Association assoc: forInsert ) {
                dao.insertAssociation(assoc);
            }
            log.info("GENE_ASSOC_INSERTED: "+forInsert.size());
        }

        if( !forDelete.isEmpty() ) {
            for( Association assoc: forDelete ) {
                dao.deleteAssociation(assoc);
            }
            log.info("GENE_ASSOC_DELETED: "+forDelete.size());
        }

        int matchingAssocs = incoming.size() - forInsert.size();
        if( matchingAssocs!=0 ) {
            log.info("GENE_ASSOC_MATCHED: "+matchingAssocs);
        }
        if( matchingAssocs!=matching.size() ) {
            log.warn("mismatch in GENE_ASSOC_MATCHED: "+matchingAssocs+" "+matching.size());
            System.out.println("mismatch in GENE_ASSOC_MATCHED: "+matchingAssocs+" "+matching.size());
        }
    }
}
