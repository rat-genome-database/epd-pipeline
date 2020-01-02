package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.dao.impl.*;
import edu.mcw.rgd.dao.spring.IntStringMapQuery;
import edu.mcw.rgd.datamodel.*;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author mtutaj
 * @since 1/13/12
 */
public class Dao {

    private AssociationDAO associationDAO = new AssociationDAO();
    private GeneDAO geneDAO = associationDAO.getGeneDAO();
    private RGDManagementDAO managementDAO = new RGDManagementDAO();
    private MapDAO mapDAO = new MapDAO();
    private XdbIdDAO xdbDAO = new XdbIdDAO();
    private GenomicElementDAO genomicElementDAO = new GenomicElementDAO();
    private SequenceDAO sequenceDAO = new SequenceDAO();

    Logger logAssocGenes = Logger.getLogger("assoc_genes");
    Logger logMapPos = Logger.getLogger("genomic_pos");
    Logger logSeq = Logger.getLogger("sequences");
    Logger logXdbIds = Logger.getLogger("xdb_ids");


    /**
     * get GenomicElement by promoter id
     * @param promoterId unique promoter id
     * @return GenomicElement object or null if promoter id is invalid
     * @throws Exception when unexpected error in spring framework occurs
     */
    public GenomicElement getPromoterById(String promoterId) throws Exception {

        List<GenomicElement> elements = genomicElementDAO.getElementsBySymbol(promoterId, RgdId.OBJECT_KEY_PROMOTERS);
        if( elements.isEmpty() ) {
            return null;
        }
        if( elements.size()==1 ) {
            return elements.get(0);
        }
        throwException("multiple promoters found for symbol "+promoterId);
        return null;
    }

    /**
     * get expression data for given genomic element
     * @param rgdId rgd id of genomic element
     * @return List of ExpressionData objects
     * @throws Exception when unexpected error in spring framework occurs
     */
    public List<ExpressionData> getExpressionData(int rgdId) throws Exception {

        return genomicElementDAO.getExpressionData(rgdId);
    }

    /**
     * insert a list of expression data objects
     * @param list list of expression data objects
     * @return count of rows affected
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int insertExpressionData(List<ExpressionData> list) throws Exception {

        return genomicElementDAO.insertExpressionData(list);
    }

    /**
     * update last modified date for specified rgd id
     * @param rgdId rgd id
     * @throws Exception when unexpected error in spring framework occurs
     */
    public void updateLastModifiedDate(int rgdId) throws Exception {

        managementDAO.updateLastModifiedDate(rgdId);
    }

    /**
     * insert a new promoter into the database; new RGD_ID object is also created
     * @param promoter GenomicElement fully setup
     * @param speciesTypeKey species type key
     * @return count of rows affected
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int insertPromoter(GenomicElement promoter, int speciesTypeKey) throws Exception {

        // if promoter rgd id is not given, create a new rgd id
        if( promoter.getRgdId()<=0 ) {
            RgdId rgdId = managementDAO.createRgdId(RgdId.OBJECT_KEY_PROMOTERS, "ACTIVE", "created by EPD pipeline", speciesTypeKey);
            promoter.setRgdId(rgdId.getRgdId());
            promoter.setObjectStatus(rgdId.getObjectStatus());
            promoter.setSpeciesTypeKey(rgdId.getSpeciesTypeKey());
        }

        return genomicElementDAO.insertElement(promoter);
    }

    /**
     * Update genomic element in table GENOMIC_ELEMENTS given rgdID
     *
     * @param ge GenomicElement object
     * @return count of rows affected
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int updatePromoter(GenomicElement ge) throws Exception{

        return genomicElementDAO.updateElement(ge);
    }

    public List<Gene> getGenesBySymbol (String geneSymbol, int speciesTypeKey) throws Exception {

        return geneDAO.getAllGenesBySymbol(geneSymbol, speciesTypeKey);
    }

    public List<Gene> getGenesByAlias(String geneSymbolAlias, int speciesTypeKey) throws Exception {

        return geneDAO.getGenesByAlias(geneSymbolAlias, speciesTypeKey);
    }

    public List<Gene> getGenesByMgdId(String mgdId, int speciesTypeKey) throws Exception {

        return xdbDAO.getActiveGenesByXdbId(XdbId.XDB_KEY_MGD, mgdId);
    }

    public List<Gene> getGenesByProteinId(String proteinId, int speciesTypeKey) throws Exception {

        return xdbDAO.getActiveGenesByXdbId(XdbId.XDB_KEY_UNIPROT, proteinId);
    }

    public List<Gene> getGenesByEnsemblId(String ensemblId, int speciesTypeKey) throws Exception {

        return xdbDAO.getActiveGenesByXdbId(XdbId.XDB_KEY_ENSEMBL_GENES, ensemblId);
    }

    public List<Gene> getGenesByNucleotideId(String nuclId, int speciesTypeKey) throws Exception {

        List<Gene> genes1 = xdbDAO.getActiveGenesByXdbId(XdbId.XDB_KEY_GENEBANKNU, nuclId);
        List<Gene> genes2 = xdbDAO.getActiveGenesByXdbId(XdbId.XDB_KEY_NCBI_NU, nuclId);

        // merge genes
        if( genes2.isEmpty() )
            return genes1;
        if( genes1.isEmpty() )
            return genes2;
        // both genes1 and genes2 are not empty
        // since Gene.equals returns true if either rgdId or geneKey match, it is easy to add genes
        genes1.addAll(genes2);
        return genes1;
    }

    public List<XdbId> getXdbIds(String[] srcPipelines) throws Exception {

        List<XdbId> xdbIds = new ArrayList<>();

        for( String srcPipeline: srcPipelines ) {
            XdbId filter = new XdbId();
            filter.setSrcPipeline(srcPipeline);
            xdbIds.addAll(xdbDAO.getXdbIds(filter));
        }
        return xdbIds;
    }

    public int insertXdbIds(Collection<XdbId> xdbIds) throws Exception {
        int r = xdbDAO.insertXdbs(new ArrayList<>(xdbIds));
        for( XdbId xdbId: xdbIds ) {
            logXdbIds.debug("INSERT "+xdbId.dump("|"));
        }
        return r;
    }

    public int updateLastModDateForXdbIds(Collection<XdbId> xdbIds) throws Exception {

        List<Integer> keys = new ArrayList<>(xdbIds.size());
        for( XdbId id: xdbIds ) {
            keys.add(id.getKey());
        }
        return xdbDAO.updateModificationDate(keys);
    }

    public int deleteXdbIds(Collection<XdbId> xdbIdsForDelete, String[] srcPipelines, String deleteThresholdStr) throws Exception {

        // extract delete threshold in percent
        int percentPos = deleteThresholdStr.indexOf('%');
        int deleteThreshold = Integer.parseInt(deleteThresholdStr.substring(0, percentPos));

        int currentXdbIdCount = 0;
        for( String srcPipeline: srcPipelines ) {
            currentXdbIdCount += getCountOfXdbIdsForSrcPipeline(srcPipeline);
        }

        int xdbIdsForDeleteCount = xdbIdsForDelete.size();
        int xdbIdsForDeleteThreshold = (deleteThreshold * currentXdbIdCount) / 100; // 5% delete threshold
        if( xdbIdsForDeleteCount > xdbIdsForDeleteThreshold ) {
            System.out.println(" XDB IDS DELETE THRESHOLD ("+deleteThresholdStr+") -- "+xdbIdsForDeleteThreshold);
            System.out.println(" XDB IDS TAGGED FOR DELETE     -- "+xdbIdsForDeleteCount);
            System.out.println(" XDB IDS DELETE THRESHOLD ("+deleteThresholdStr+") EXCEEDED -- no xdb ids deleted");
            return 0;
        }

        for( XdbId xdbId: xdbIdsForDelete ) {
            logXdbIds.debug("DELETE "+xdbId.dump("|"));
        }
        return xdbDAO.deleteXdbIds(new ArrayList<>(xdbIdsForDelete));
    }

    int getCountOfXdbIdsForSrcPipeline(String srcPipeline) throws Exception {
        return xdbDAO.getCount("SELECT COUNT(*) FROM rgd_acc_xdb WHERE src_pipeline=?", srcPipeline);
    }

    public int insertMapData(List<MapData> mds) throws Exception {
        for( MapData md: mds ) {
            logMapPos.debug("INSERT " +md.dump("|"));
        }
        return mapDAO.insertMapData(mds);
    }

    public int deleteMapData(List<MapData> mds) throws Exception {
        for( MapData md: mds ) {
            logMapPos.debug("DELETE " +md.dump("|"));
        }
        return mapDAO.deleteMapData(mds);
    }

    public List<MapData> getMapData(String[] srcPipelines) throws Exception {

        List<MapData> mds = new ArrayList<>();

        for( String srcPipeline: srcPipelines ) {
            mds.addAll(getMapDataForSource(srcPipeline));
        }
        return mds;
    }

    List<MapData> getMapDataForSource(String src) throws Exception {
        String sql = "SELECT * FROM maps_data WHERE src_pipeline=?";
        return mapDAO.executeMapDataQuery(sql, src);
    }

    public List<MapData> getMapData(int rgdId, int mapKey) throws Exception {
        return mapDAO.getMapData(rgdId, mapKey);
    }

    /**
     * return list of all association for given master rgd id and association type
     * @param masterRgdId master rgd id
     * @param assocType association type
     * @return list of Association objects; never null, but returned list could be empty
     * @throws Exception when unexpected error in spring framework occurs
     */
    public List<Association> getAssociations(int masterRgdId, String assocType) throws Exception {

        return associationDAO.getAssociationsForMasterRgdId(masterRgdId, assocType);
    }

    public List<Association> getAssociations(String assocType, String source) throws Exception {

        return associationDAO.getAssociationsByTypeAndSource(assocType, source);
    }

    /**
     * insert a new association into RGD_ASSOCIATIONS table; assoc_key will be automatically taken from database sequence
     * <p>Note: assoc_key and assoc_subkey are always made lower-case and src_pipeline is always made uppercase</p>
     * @param assoc Association object to be inserted
     * @return value of generated assoc_key
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int insertAssociation( Association assoc ) throws Exception {
        int assocKey = associationDAO.insertAssociation(assoc);
        logAssocGenes.info("INSERT "+assoc.dump("|"));
        return assocKey;
    }

    public int deleteAssociation( Association assoc ) throws Exception {
        logAssocGenes.info("DELETE "+assoc.dump("|"));
        return associationDAO.deleteAssociations(assoc.getMasterRgdId(), assoc.getDetailRgdId(), assoc.getAssocType());
    }

    public void insertSequence(Sequence seq) throws Exception {

        if( seq.getRgdId()==0 ) {
            throw new EpdDaoException("cannot insert a sequence with RGD_ID=0");
        }
        sequenceDAO.insertSequence(seq);
        logSeq.debug("INSERT "+seq.dump("|"));
    }

    public int deleteSequence(Sequence seq) throws Exception {

        if( seq.getSeqKey()==0 ) {
            // ensure seq_key is loaded
            List<Sequence> seqs = sequenceDAO.getObjectSequences(seq.getRgdId(), "promoter_region");
            for( Sequence seqInRgd: seqs ) {
                if( seqInRgd.getSeqMD5().equals(seq.getSeqMD5()) ) {
                    seq.setSeqKey(seqInRgd.getSeqKey());
                }
            }
            if( seq.getSeqKey()==0 ) {
                throw new EpdDaoException("cannot find 'promoter_region' sequence for RGD:" + seq.getRgdId());
            }
        }

        int r = sequenceDAO.deleteSequence(seq);
        logSeq.debug("DELETE "+(r!=0?"OK":"FAILED")+" "+seq.dump("|"));
        return r;
    }

    public List<Sequence> getPromoterSequences() throws Exception {

        List<IntStringMapQuery.MapPair> md5s = sequenceDAO.getMD5ForObjectSequences(RgdId.OBJECT_KEY_PROMOTERS, "promoter_region");
        List<Sequence> seqs = new ArrayList<>(md5s.size());
        for( IntStringMapQuery.MapPair pair: md5s ) {
            Sequence seq = new Sequence();
            seq.setRgdId(pair.keyValue);
            seq.setSeqMD5(pair.stringValue);
            seq.setSeqType("promoter_region");
            seqs.add(seq);
        }
        return seqs;
    }

    public Chromosome getChromosome(String locus) throws Exception {
        return mapDAO.getChromosome(locus);
    }

    public void throwException(String msg) throws Exception {
        throw new EpdDaoException(msg);
    }

    public class EpdDaoException extends Exception {

        public EpdDaoException(String msg) {
            super(msg);
        }
    }
}
