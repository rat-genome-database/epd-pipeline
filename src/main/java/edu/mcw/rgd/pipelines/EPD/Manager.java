package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.pipelines.PipelineManager;
import edu.mcw.rgd.process.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    protected final Log logger = LogFactory.getLog("status");
    private LocusInfoManager locusInfoManager;

    private static Manager manager = null;
    private String epdFileName;
    private List<String> epdNewFileNames;
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

        logger.info(getVersion());

        Date startDate = Utils.addDaysToDate(new Date(), -2); // remove stale XDB_IDS that are older than 2 days
        // Note: if we use current date as the cutoff date, freshly added/modified xdb ids could be incorrectly
        //    classified as stale and dropped! Apparently there is some issue with passing to Oracle the right
        //    cutoff date and time; however, the workaround proposed works nad stale xdb ids are handled properly :-)
        int staleXdbIdsDeleted;

        // process old EPD file
        run("EPD", getEpdFileName());

        staleXdbIdsDeleted = dao.deleteStaleXdbIds(startDate, "EPD", getStaleXdbIdsDeleteThreshold());
        System.out.println("stale xdb ids deleted for EPD: "+staleXdbIdsDeleted);
        System.out.println();

        // process EPDNEW files
        for( String epdNewFileName: getEpdNewFileNames() ) {
            run("EPDNEW", epdNewFileName);
        }
        staleXdbIdsDeleted = dao.deleteStaleXdbIds(startDate, "EPDNEW", getStaleXdbIdsDeleteThreshold());
        System.out.println("stale xdb ids deleted for EPDNEW: "+staleXdbIdsDeleted);
    }

    public void run(String srcPipeline, String fileName) throws Exception {

        PipelineManager pman = new PipelineManager();

        preProcessor.setDao(getDao());
        qcProcessor.setDao(getDao());
        loadProcessor.setDao(getDao());

        preProcessor.setSrcPipeline(srcPipeline);
        qcProcessor.setSrcPipeline(srcPipeline);

        preProcessor.setFileName(fileName);

        pman.addPipelineWorkgroup(preProcessor, "PP", 1, 0);
        pman.addPipelineWorkgroup(qcProcessor, "QC", qcProcessor.getQcThreadCount(), 0);
        pman.addPipelineWorkgroup(loadProcessor, "LD", 1, 0);

        pman.run();

        // dump counter statistics
        pman.dumpCounters();
        System.out.println();
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

    public void setEpdFileName(String epdFileName) {
        this.epdFileName = epdFileName;
    }

    public String getEpdFileName() {
        return epdFileName;
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
}