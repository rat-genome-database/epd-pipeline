package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.process.Utils;

import java.util.*;

/**
 * @author mtutaj
 * @since 4/17/12
 * represents object processed by the pipeline framework
 */
public class EPDRecord extends PipelineRecord {

    private Dao dao;
    // gene symbols and symbol synonyms extracted from several places:
    // 1. ID name
    //    ID   HS_MYC_1     standard; single; VRT.
    //            MYC
    // 2. SWISS-PROT name
    //    DR   SWISS-PROT; P01106; MYC_HUMAN.
    //                             MYC
    // 3. CLEANEX name
    //    DR   CLEANEX; HS_MYC.
    //                     MYC
    // 4. MGD name
    //    DR   MGD; MGI:98783; Tnni3
    //                         TNNI3
    private Set<String> geneIds = new HashSet<String>();
    private Gene gene; // matching gene

    private GenomicElement promoter = new GenomicElement();
    private List<ExpressionData> expressionDataList = new ArrayList<>();
    private List<XdbId> xdbIds = new ArrayList<>();
    private List<MapData> mds = new ArrayList<>();
    List<Sequence> seqs = new ArrayList<>();

    private String altPromoterInfo; // informational text about
    private List<String> altPromoters = new ArrayList<>(); // list of acc ids of alternative promoters
    private List<String> neighPromoters = new ArrayList<>(); // list of acc ids of neighboring promoters

    private String experimentMethods;
    private String expressionData; // tissue
    private String regulationData;

    public Set<String> getGeneIds() {
        return geneIds;
    }

    public boolean addGeneId(String id) {
        if( Utils.isStringEmpty(id) ) {
            return false;
        }
        if( id.equals("1") || id.equals("2")) {
            System.out.println("warning: unexpected gene id length <2: "+id);
            return false;
        }
        geneIds.add(id);
        return true;
    }

    public void addXdbId(XdbId xdbId) {
        xdbIds.add(xdbId);
    }

    public List<String> getAccIds(int[] xdbKeys) {
        List<String> accIds = new ArrayList<>();
        for( int xdbKey: xdbKeys ) {
            for( XdbId xdbId: xdbIds ) {
                if( xdbId.getXdbKey()==xdbKey ) {
                    accIds.add(xdbId.getAccId());
                }
            }
            return accIds;
        }
        return accIds;
    }

    public void setRgdIdForXdbIds(int rgdId) {
        for( XdbId xdbId: xdbIds ) {
            xdbId.setRgdId(rgdId);
            XdbIdCollection.getInstance().addIncoming(xdbId);
        }
    }

    public void addMapData(MapData md) {
        mds.add(md);
    }

    public void setRgdIdForMapData(int rgdId, String notes) {
        for( MapData md: mds ) {
            md.setRgdId(rgdId);
            md.setNotes(notes);
            MapsDataCollection.getInstance().addIncoming(md);
        }
    }

    public void addExpressionData(ExpressionData ed) {
        expressionDataList.add(ed);
    }

    public void setRgdIdForExpressionData(int rgdId, String notes) {
        for( ExpressionData ed: expressionDataList ) {
            ed.setRgdId(rgdId);
            ed.setNotes(notes);
            ExpressionDataCollection.getInstance().addIncoming(ed);
        }
    }

    public List<MapData> getMapData() {
        return mds;
    }

    public GenomicElement getPromoter() {
        return promoter;
    }

    public void addSeq(Sequence seq) {
        seqs.add(seq);
    }

    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public Gene getGene() {
        return gene;
    }

    public void setGene(Gene gene) {
        this.gene = gene;
    }

    public String getAltPromoterInfo() {
        return altPromoterInfo;
    }

    public void setAltPromoterInfo(String altPromoterInfo) {
        this.altPromoterInfo = altPromoterInfo;
    }

    public List<String> getAltPromoters() {
        return altPromoters;
    }

    public void addAltPromoter(String accId) {
        altPromoters.add(accId);
    }

    public List<String> getNeighboringPromoters() {
        return neighPromoters;
    }
    public void addNeighboringPromoter(String accId) {
        neighPromoters.add(accId);
    }

    public String getExperimentMethods() {
        return experimentMethods;
    }

    public void setExperimentMethods(String experimentMethods) {
        this.experimentMethods = experimentMethods;
    }

    public String getExpressionData() {
        return expressionData;
    }

    public void setExpressionData(String expressionData) {
        this.expressionData = expressionData;
    }

    public String getRegulationData() {
        return regulationData;
    }

    public void setRegulationData(String regulationData) {
        this.regulationData = regulationData;
    }
}
