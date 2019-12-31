package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.GenomicElement;
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
    private ExpressionDataCollection attrs = new ExpressionDataCollection();
    private AlternativePromoterCollection apassocs = new AlternativePromoterCollection();
    private NeighborPromoterCollection npassocs = new NeighborPromoterCollection();
    private XdbIdCollection xdbIds = new XdbIdCollection();
    Map<Integer, MapsDataCollection> mds = new HashMap<>();
    private SequenceCollection seq = new SequenceCollection();

    private String altPromoterInfo; // informational text about
    private List<String> altPromoters; // list of acc ids of alternative promoters
    private List<String> neighPromoters; // list of acc ids of neighboring promoters

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

    public GenomicElement getPromoter() {
        return promoter;
    }

    public ExpressionDataCollection getAttrs() {
        return attrs;
    }

    public AssociationCollection getAlternativePromoterAssocs() {
        return apassocs;
    }

    public AssociationCollection getNeighboringPromoterAssocs() {
        return npassocs;
    }

    public XdbIdCollection getXdbIds() {
        return xdbIds;
    }

    public SequenceCollection getSeq() {
        return seq;
    }

    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
        attrs.setDao(dao);
        apassocs.setDao(dao);
        npassocs.setDao(dao);

        for( MapsDataCollection md: mds.values() ) {
            md.setDao(dao);
        }
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
        if( altPromoters==null )
            altPromoters = new ArrayList<String>();
        altPromoters.add(accId);
    }

    public List<String> getNeighboringPromoters() {
        return neighPromoters;
    }
    public void addNeighboringPromoter(String accId) {
        if( neighPromoters==null )
            neighPromoters = new ArrayList<String>();
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
