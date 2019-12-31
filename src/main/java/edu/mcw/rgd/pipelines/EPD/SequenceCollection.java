package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.Sequence;
import edu.mcw.rgd.process.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author mtutaj
 * @since 4/16/12
 * collection of promoter sequences
 */
public class SequenceCollection {

    // THREAD SAFE SINGLETON -- start
    // private instance, so that it can be accessed by only by getInstance() method
    private static SequenceCollection instance;

    private SequenceCollection() {
        // private constructor
    }

    //synchronized method to control simultaneous access
    synchronized public static SequenceCollection getInstance() {
        if (instance == null) {
            // if instance is null, initialize
            instance = new SequenceCollection();
        }
        return instance;
    }
    // THREAD SAFE SINGLETON -- end


    Logger log = Logger.getLogger("status");

    private final Set<Sequence> incoming = new HashSet<>();

    public void addIncoming(Sequence seq) throws Exception {

        if( seq.getRgdId()==0 ) {
            throw new Exception("seq_unexpected1");
        }
        if( !seq.getSeqType().equals("promoter_region") ) {
            throw new Exception("seq_unexpected2");
        }

        String md5 = Utils.generateMD5(seq.getSeqData());
        seq.setSeqMD5(md5);

        // there is only one instance of this class
        synchronized (incoming) {
            incoming.add(seq);
        }
    }

    synchronized public void qc(Dao dao) throws Exception {

        // note: for better performance, only some fields are loaded: rgd-id, seq-type and seq-md5
        List<Sequence> inRgdSeqs = dao.getPromoterSequences();

        // determine new sequences for insertion
        Collection<Sequence> forInsert = CollectionUtils.subtract(incoming, inRgdSeqs);

        // determine new sequences for deletion
        Collection<Sequence> forDelete = CollectionUtils.subtract(inRgdSeqs, incoming);

        Collection<Sequence> matching = CollectionUtils.intersection(inRgdSeqs, incoming);


        // insert new sequences
        if( !forInsert.isEmpty() ) {
            for( Sequence seq: forInsert ) {
                dao.insertSequence(seq);
            }
            log.info("SEQ_INSERTED: "+forInsert.size());
        }

        // delete obsolete sequences
        if( !forDelete.isEmpty() ) {
            for( Sequence seq: forDelete ) {
                dao.deleteSequence(seq);
            }
            log.info("SEQ_DELETED: "+forDelete.size());
        }

        int matchingSeqs = matching.size();
        if( matchingSeqs!=0 ) {
            log.info("SEQ_MATCHED: "+matchingSeqs);
        }
    }
}
