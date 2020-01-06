package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.pipelines.PipelineManager;
import edu.mcw.rgd.process.CounterPool;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.util.Date;
import java.util.List;

public class Manager {

    private QCProcessor qcProcessor;
    private LoadProcessor loadProcessor;
    private PreProcessor preProcessor;
    private Dao dao;
    private String version;

    protected final Logger logger = Logger.getLogger("status");
    private LocusInfoManager locusInfoManager;

    private static Manager manager = null;
    private List<String> epdFileNames;
    private List<String> epdNewFileNames;
    private List<String> epdNewNcFileNames;
    private String staleXdbIdsDeleteThreshold;

    public static void main(String[] args) throws Exception {

        // process args
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));

        manager = (Manager) (bf.getBean("manager"));

        try {
            manager.run();
        }catch( Exception e ) {
            manager.logger.error(e.getMessage());
            e.printStackTrace();
            throw new Exception(e);
        }
    }

    public static Manager getInstance() {
        return manager;
    }

    /**
     * process species specific file
     * @throws Exception
     */
    public void run() throws Exception {

        long time0 = System.currentTimeMillis();

        logger.info(getVersion());

        Date startDate = Utils.addHoursToDate(new Date(), -2); // remove stale XDB_IDS that are older than 2 hours
        // Note: if we use current timestamp as the cutoff timestamp, freshly added/modified xdb ids could be incorrectly
        //    classified as stale and dropped! (due to possible clock differences between db and app servers)


        String[] sources = {"EPD", "EPDNEW", "EPDNEWNC"};

        run("EPD", getEpdFileNames());
        run("EPDNEW", getEpdNewFileNames());
        run("EPDNEWNC", getEpdNewNcFileNames());


        // post processing
        // ---
        AlternativePromoterCollection.getInstance().qc(dao, sources);
        NeighborPromoterCollection.getInstance().qc(dao, sources);

        ExpressionDataCollection.getInstance().qc(dao, sources);

        MapsDataCollection.getInstance().qc(dao, sources);

        // 'promoter_to_gene' associations
        GeneAssociationCollection.getInstance().qc(dao, sources);

        // 'promoter_region' sequences
        SequenceCollection.getInstance().qc(dao);

        XdbIdCollection.getInstance().qc(dao, sources, getStaleXdbIdsDeleteThreshold());

        System.out.println("=== OK ===  elapsed  "+Utils.formatElapsedTime(time0, System.currentTimeMillis()));
    }

    void run(String srcPipeline, List<String> epdFileNames) throws Exception {

        for( String epdFileName: epdFileNames ) {
            run(srcPipeline, epdFileName);
        }
    }

    public void run(String srcPipeline, String fileName) throws Exception {

        CounterPool counters = new CounterPool();
        PipelineManager pman = new PipelineManager();

        preProcessor.setDao(getDao());
        qcProcessor.setDao(getDao());
        loadProcessor.setDao(getDao());

        preProcessor.setCounters(counters);
        qcProcessor.setCounters(counters);
        loadProcessor.setCounters(counters);

        preProcessor.setSrcPipeline(srcPipeline);
        qcProcessor.setSrcPipeline(srcPipeline);
        loadProcessor.setSrcPipeline(srcPipeline);

        preProcessor.setFileName(fileName);

        pman.addPipelineWorkgroup(preProcessor, "PP", 1, 0);
        pman.addPipelineWorkgroup(qcProcessor, "QC", qcProcessor.getQcThreadCount(), 0);
        pman.addPipelineWorkgroup(loadProcessor, "LD", qcProcessor.getQcThreadCount(), 0);

        pman.run();

        logger.info(counters.dumpAlphabetically());
    }

    public void setQcProcessor(QCProcessor qcProcessor) {
        this.qcProcessor = qcProcessor;
    }

    public QCProcessor getQcProcessor() {
        return qcProcessor;
    }

    public void setLoadProcessor(LoadProcessor loadProcessor) {
        this.loadProcessor = loadProcessor;
    }

    public LoadProcessor getLoadProcessor() {
        return loadProcessor;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public Dao getDao() {
        return dao;
    }

    public void setPreProcessor(PreProcessor preProcessor) {
        this.preProcessor = preProcessor;
    }

    public PreProcessor getPreProcessor() {
        return preProcessor;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setLocusInfoManager(LocusInfoManager locusInfoManager) {
        this.locusInfoManager = locusInfoManager;
    }

    public LocusInfoManager getLocusInfoManager() {
        return locusInfoManager;
    }

    public void setEpdFileNames(List<String> epdFileNames) {
        this.epdFileNames = epdFileNames;
    }

    public List<String> getEpdFileNames() {
        return epdFileNames;
    }

    public void setEpdNewFileNames(List<String> epdNewFileNames) {
        this.epdNewFileNames = epdNewFileNames;
    }

    public List<String> getEpdNewFileNames() {
        return epdNewFileNames;
    }

    public void setStaleXdbIdsDeleteThreshold(String staleXdbIdsDeleteThreshold) {
        this.staleXdbIdsDeleteThreshold = staleXdbIdsDeleteThreshold;
    }

    public String getStaleXdbIdsDeleteThreshold() {
        return staleXdbIdsDeleteThreshold;
    }

    public void setEpdNewNcFileNames(List<String> epdNewNcFileNames) {
        this.epdNewNcFileNames = epdNewNcFileNames;
    }

    public List<String> getEpdNewNcFileNames() {
        return epdNewNcFileNames;
    }
}