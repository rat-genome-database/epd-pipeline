package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.Sequence;
import edu.mcw.rgd.pipelines.PipelineSession;
import edu.mcw.rgd.process.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mtutaj
 * @since 4/16/12
 * collection of promoter sequences
 */
public class SequenceCollection {

    private List<Sequence> incomingSeqs = new ArrayList<>();
    private List<Sequence> forInsertSeqs = new ArrayList<>();
    private List<Sequence> forDeleteSeqs = new ArrayList<>();

    public void addIncomingObject(Sequence seq) throws Exception {
        String md5 = Utils.generateMD5(seq.getSeqData());
        seq.setSeqMD5(md5);
        incomingSeqs.add(seq);
    }

    public void qc(int rgdId, Dao dao) throws Exception {

        for( Sequence seq: incomingSeqs ) {
            seq.setRgdId(rgdId);
        }

        List<Sequence> inRgdSeqs = dao.getSequences(rgdId);

        // determine new sequences
        for( Sequence seqIncoming: incomingSeqs ) {

            boolean incomingSequenceIsInRgd = false;
            for( Sequence seqInRgd: inRgdSeqs ) {
                if( seqInRgd.getSeqMD5().equals(seqIncoming.getSeqMD5()) ) {
                    incomingSequenceIsInRgd = true;
                    break;
                }
            }

            if( !incomingSequenceIsInRgd ) {
                forInsertSeqs.add(seqIncoming);
            }
        }

        // determine to be deleted sequences
        for( Sequence seqInRgd: inRgdSeqs ) {

            boolean inRgdSeqMatchesIncoming = false;
            for( Sequence seqIncoming: incomingSeqs ) {
                if( seqInRgd.getSeqMD5().equals(seqIncoming.getSeqMD5()) ) {
                    inRgdSeqMatchesIncoming = true;
                    break;
                }
            }

            if( !inRgdSeqMatchesIncoming ) {
                forDeleteSeqs.add(seqInRgd);
            }
        }
    }

    public void sync(int rgdId, Dao dao, PipelineSession session) throws Exception {

        if( !forInsertSeqs.isEmpty() ) {
            for( Sequence seq: forInsertSeqs ) {
                seq.setRgdId(rgdId);
                dao.insertSequence(seq);
            }
            session.incrementCounter("SEQ_INSERTED", forInsertSeqs.size());
        }

        if( !forDeleteSeqs.isEmpty() ) {
            for( Sequence seq: forDeleteSeqs ) {
                dao.deleteSequence(seq);
            }
            session.incrementCounter("SEQ_DELETED", forDeleteSeqs.size());
        }

        int matchingSeqs = incomingSeqs.size() - forInsertSeqs.size();
        if( matchingSeqs!=0 ) {
            session.incrementCounter("SEQ_MATCHED", matchingSeqs);
        }
    }
}
