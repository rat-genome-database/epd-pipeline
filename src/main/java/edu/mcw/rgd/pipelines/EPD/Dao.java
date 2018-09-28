package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.dao.impl.*;
import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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

    protected final Log logSeq = LogFactory.getLog("sequences");
    protected final Log logXdbIds = LogFactory.getLog("xdb_ids");


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

        return xdbDAO.getGenesByXdbId(XdbId.XDB_KEY_MGD, mgdId);
    }

    public List<Gene> getGenesByProteinId(String proteinId, int speciesTypeKey) throws Exception {

        return xdbDAO.getGenesByXdbId(XdbId.XDB_KEY_UNIPROT, proteinId);
    }

    public List<Gene> getGenesByNucleotideId(String nuclId, int speciesTypeKey) throws Exception {

        List<Gene> genes1 = xdbDAO.getGenesByXdbId(XdbId.XDB_KEY_GENEBANKNU, nuclId);
        List<Gene> genes2 = xdbDAO.getGenesByXdbId(XdbId.XDB_KEY_NCBI_NU, nuclId);

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

    public List<XdbId> getXdbIdsByRgdId(int rgdId, String srcPipeline) throws Exception {

        if( rgdId==0 ) {
            return Collections.emptyList();
        }

        XdbId filter = new XdbId();
        filter.setRgdId(rgdId);
        filter.setSrcPipeline(srcPipeline);
        return xdbDAO.getXdbIds(filter);
    }

    public int insertXdbIds(List<XdbId> xdbIds) throws Exception {
        int r = xdbDAO.insertXdbs(xdbIds);
        for( XdbId xdbId: xdbIds ) {
            logXdbIds.debug("INSERT "+xdbId.dump("|"));
        }
        return r;
    }

    public int deleteXdbIds(List<XdbId> xdbIds) throws Exception {
        for( XdbId xdbId: xdbIds ) {
            logXdbIds.debug("DELETE "+xdbId.dump("|"));
        }
        return xdbDAO.deleteXdbIds(xdbIds);
    }

    public int updateLastModDateForXdbIds(List<XdbId> xdbIds) throws Exception {

        List<Integer> keys = new ArrayList<>(xdbIds.size());
        for( XdbId id: xdbIds ) {
            keys.add(id.getKey());
        }
        return xdbDAO.updateModificationDate(keys);
    }

    public int deleteStaleXdbIds(Date cutoffDate, String srcPipeline, String deleteThresholdStr) throws Exception {

        // extract delete threshold in percent
        int percentPos = deleteThresholdStr.indexOf('%');
        int deleteThreshold = Integer.parseInt(deleteThresholdStr.substring(0, percentPos));

        int currentXdbIdCount = getCountOfXdbIdsForSrcPipeline(srcPipeline);
        List<XdbId> staleXdbIds = xdbDAO.getXdbIdsModifiedBefore(cutoffDate, srcPipeline, SpeciesType.ALL);

        int xdbIdsForDeleteCount = staleXdbIds.size();
        int xdbIdsForDeleteThreshold = (deleteThreshold * currentXdbIdCount) / 100; // 5% delete threshold
        if( xdbIdsForDeleteCount > xdbIdsForDeleteThreshold ) {
            System.out.println(" STALE XDB IDS DELETE THRESHOLD ("+deleteThresholdStr+") -- "+xdbIdsForDeleteThreshold);
            System.out.println(" STALE XDB IDS TAGGED FOR DELETE     -- "+xdbIdsForDeleteCount);
            System.out.println(" STALE XDB IDS DELETE THRESHOLD ("+deleteThresholdStr+") EXCEEDED -- no xdb ids deleted");
            return 0;
        }

        for( XdbId xdbId: staleXdbIds ) {
            logXdbIds.info("DELETE STALE "+xdbId.dump("|"));
        }
        return xdbDAO.deleteXdbIds(staleXdbIds);
    }

    int getCountOfXdbIdsForSrcPipeline(String srcPipeline) throws Exception {
        return xdbDAO.getCount("SELECT COUNT(*) FROM rgd_acc_xdb WHERE src_pipeline=?", srcPipeline);
    }

    public int insertMapData(List<MapData> mds) throws Exception {
        return mapDAO.insertMapData(mds);
    }

    public int updateMapData(List<MapData> mds) throws Exception {
        return mapDAO.updateMapData(mds);
    }

    public int deleteMapData(List<MapData> mds) throws Exception {
        return mapDAO.deleteMapData(mds);
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

    /**
     * insert a new association into RGD_ASSOCIATIONS table; assoc_key will be automatically taken from database sequence
     * <p>Note: assoc_key and assoc_subkey are always made lower-case and src_pipeline is always made uppercase</p>
     * @param assoc Association object to be inserted
     * @return value of generated assoc_key
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int insertAssociation( Association assoc ) throws Exception {
        return associationDAO.insertAssociation(assoc);
    }

    public int deleteAssociation( Association assoc ) throws Exception {
        return associationDAO.deleteAssociations(assoc.getMasterRgdId(), assoc.getDetailRgdId(), assoc.getAssocType());
    }

    public void insertSequence(Sequence2 seq) throws Exception {

        if( seq.getRgdId()==0 ) {
            throw new EpdDaoException("cannot insert a sequence with RGD_ID=0");
        }
        sequenceDAO.insertSequence(seq);
        logSeq.debug("INSERT "+seq.dump("|"));
    }

    public int deleteSequence(Sequence2 seq) throws Exception {
        if( seq.getRgdId()==0 ) {
            throw new EpdDaoException("cannot delete a sequence with RGD_ID=0");
        }
        int r = sequenceDAO.deleteSequence(seq);
        logSeq.debug("DELETE "+(r!=0?"OK":"FAILED")+" "+seq.dump("|"));
        return r;
    }

    public List<Sequence2> getSequences(int rgdId) throws Exception {
        return sequenceDAO.getObjectSequences2(rgdId);
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
