package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.pipelines.RecordPreprocessor;
import edu.mcw.rgd.process.FileDownloader;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Date;
import java.util.Map;
import java.util.zip.GZIPInputStream;


/**
 * @author mtutaj
 * @since 1/13/12
 */
public class PreProcessor extends RecordPreprocessor {
    private String fileName;

    private Dao dao;
    private String srcPipeline;

    protected final Logger logger = Logger.getLogger("status");
    private Map<String, String> experimentEvidences;


    @Override
    public void process() throws Exception {

        process(getFileName());
    }

    private void process(String fileName) throws Exception {

        logger.info("Processing file " + fileName);

        FileDownloader downloader = new FileDownloader();
        downloader.setExternalFile(fileName);
        downloader.setLocalFile("data/" + new File(fileName).getName());
        downloader.setAppendDateStamp(true);
        downloader.setUseCompression(true);
        String localFileName = downloader.downloadNew();

        // open the input file
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(localFileName))))) {
            // there could be multiple lines for one promoter
            String line;
            int rowCount = 0, recNo = 0;

            EPDRecord rec = new EPDRecord();
            rec.setRecNo(++recNo);
            rec.setDao(dao);

            while ((line = reader.readLine()) != null) {

                // skip lines shorter than 2 characters and lines starting with 'XX'
                ++rowCount;
                if (line.length() < 2 || line.startsWith("XX"))
                    continue;

                // detect end of record
                if (line.startsWith("//")) {

                    // create expression data if applicable
                    if (rec.getExperimentMethods() != null || rec.getExpressionData() != null || rec.getRegulationData() != null) {
                        ExpressionData ed = new ExpressionData();
                        ed.setExperimentMethods(rec.getExperimentMethods());
                        ed.setTissue(rec.getExpressionData());
                        ed.setRegulation(rec.getRegulationData());
                        rec.getAttrs().getIncomingList().add(ed);
                    }

                    // new promoter detected: store previous data to database
                    getSession().putRecordToFirstQueue(rec);
                    getSession().incrementCounter("RECORDS_PROCESSED", 1);

                    // and initialize the new promoter
                    rec = new EPDRecord();
                    rec.setRecNo(++recNo);
                    rec.setDao(dao);
                    continue;
                }

                if (processID(line, rec))
                    continue;
                if (processAC(line, rec))
                    continue;
                if (processDT(line, rec))
                    continue;
                if (processDE(line, rec))
                    continue;
                if (processOS(line, rec))
                    continue;
                if (processHG(line, rec))
                    continue;
                if (processAP(line, rec))
                    continue;
                if (processNP(line, rec))
                    continue;
                if (processDR(line, rec))
                    continue;
                if (processRN_RX_RA_RT_RL(line, rec))
                    continue;
                if (processME(line, rec))
                    continue;
                if (processFP(line, rec))
                    continue;
                if (processDO(line, rec))
                    continue;
                if (processSE(line, rec))
                    continue;
                if (processGN(line, rec))
                    continue;
            }

            logger.info("Lines read from file " + fileName + ": " + rowCount);
        }

    }

    private void appendToNotes( GenomicElement ge, String notes ) {

        if( notes==null || notes.trim().isEmpty() )
            return;

        String str = ge.getNotes();
        if( str==null )
            str = "";
        if( !str.isEmpty() )
            str += "; ";
        ge.setNotes(str+notes.trim());
    }

    private boolean processID(String line, EPDRecord rec) {

        if( !line.startsWith("ID") )
            return false;

        //ID   ENTRY_NAME data class; initiation site type; TAXONOMIC DIVISION.
        String[] fields = line.split("[ ;]+");
        rec.getPromoter().setSource(getSrcPipeline());
        rec.getPromoter().setName(fields[1]);

        //String dataClass = fields[2].trim();
        //if( !dataClass.isEmpty() )
        //    appendToNotes(rec.getPromoter(), "quality of the information: " + dataClass);

        String iss = fields[3].trim();
        if( !iss.isEmpty() ) {
            if( iss.equals("single") )
                iss = "single initiation site";
            else if( iss.equals("multiple") )
                iss = "multiple initiation site";
            else if( iss.equals("region") )
                iss = "initiation region";
            else {
                if( getSrcPipeline().equals("EPD") )
                    iss = "undefined";
                else
                    iss = "preliminary";
            }
            rec.getPromoter().setObjectType(iss);
        }

        // parse part of entry name between first underscore and optional second underscore
        // for example extract 'TRIC' as gene symbol from id 'MM_TRIC_2' or id 'MM_TRIC'
        // Note: only for 'EPD' pipeline
        if( getSrcPipeline().equals("EPD") ) {
            String geneSymbol = null;
            int pos1 = fields[1].indexOf('_');
            int pos2 = fields[1].indexOf('_', pos1 + 1);
            if (pos1 > 0) {
                if (pos2 > 0)
                    geneSymbol = fields[1].substring(pos1 + 1, pos2);
                else
                    geneSymbol = fields[1].substring(pos1 + 1);
            }
            rec.addGeneId(geneSymbol);
        }
        return true;
    }

    private boolean processAC(String line, EPDRecord rec) {

        if( !line.startsWith("AC") )
            return false;

        //AC   EP30085;
        String[] fields = line.split("[ ;]+");
        rec.getPromoter().setSymbol(fields[1]);
        return true;
    }

    private boolean processDT(String line, EPDRecord rec) {

        if( !line.startsWith("DT") )
            return false;

        //DT   ??-MAR-1992 (Rel. 30, created)
        //DT   09-OCT-2002 (Rel. 73, Last annotation update).
        //
        // IGNORED
        return true;
    }

    private boolean processDE(String line, EPDRecord rec) {

        if( !line.startsWith("DE") )
            return false;
        // description can span multiple lines:
        //
        //DE   c-myc (cellular homologue of myelocytomatosis virus 29 oncogene),
        //DE   promoter 2.
        String description = rec.getPromoter().getDescription();
        if( description==null )
            description = "";
        description += line.substring(5);
        rec.getPromoter().setDescription(description);
        return true;
    }

    private boolean processOS(String line, EPDRecord rec) {

        if( !line.startsWith("OS") )
            return false;

        //OS   Mus musculus (house mouse)
        String lineLC = line.toLowerCase();
        if( lineLC.contains("rattus norvegicus") ) {
            rec.getPromoter().setSpeciesTypeKey(SpeciesType.RAT);
        } else if( lineLC.contains("mus musculus") ) {
            rec.getPromoter().setSpeciesTypeKey(SpeciesType.MOUSE);
        } else if( lineLC.contains("homo sapiens") ) {
            rec.getPromoter().setSpeciesTypeKey(SpeciesType.HUMAN);
        } else if( lineLC.contains("canis familiaris") ) {
            rec.getPromoter().setSpeciesTypeKey(SpeciesType.DOG);
        }
        return true;
    }

    private boolean processHG(String line, EPDRecord rec) {

        if( !line.startsWith("HG") )
            return false;

        //HG   Homology group 53; Mammalian c-myc proto-oncogene, promoter 2
        //
        // The homology group <http://epd.vital-it.ch/current/HG.html> line is
        // optional, it contains 2 fields: a homology group number that allows
        // identification of all sequence-wise similar promoters in EPD, and a
        // homology group name.

        // skip undefined homology group
        String homologyGroup = line.substring(5);
        if( homologyGroup.equals("none.") )
            return true;

        // also, use only first line as the homology group name
        // what will be done automatically by rec.addIncomingObject
        String hg = homologyGroup.trim();
        if( !hg.isEmpty() )
            appendToNotes(rec.getPromoter(), "homology_group="+hg);

        return true;
    }

    private boolean processAP(String line, EPDRecord rec) {

        if( !line.startsWith("AP") )
            return false;

        //AP   Alternative promoter #2 of 2; 5' exon 1; site 2; major promoter.
        //
        //    The AP line is optional and provides information on alternative
        //promoters <http://epd.vital-it.ch/current/AP.html> of the same gene (for
        //more details, see Section 4.3.1.). It contains 3 or 4 fields, separated
        //by semicolons, providing the following types of information:
        String str = line.substring(5).trim();
        if( !str.isEmpty() && !str.equals("none.") ) {
            rec.setAltPromoterInfo(str);
        }
        return true;
    }

    private boolean processNP(String line, EPDRecord rec) {

        if( !line.startsWith("NP") )
            return false;

        //NP   Neighbouring Promoter; EP23008; MM_H2B1; [-209; -].

        //The NP line is optional and provides information on promoters which are
        //physically closer to each other than 1000 bp. It contains 3 fields,
        //separated by semicolons, providing the following types of information:

        String str = line.substring(5).trim();
        if( !str.isEmpty() && !str.equals("none.") ) {
            // extract EPD acc id
            int pos1 = line.indexOf(';');
            int pos2 = line.indexOf(';', pos1 + 1);
            if( pos1>0 && pos2>0 ) {
                String accId = line.substring(pos1+1, pos2).trim().toUpperCase();
                rec.addNeighboringPromoter(accId);
            }
        }
        return true;
    }

    private boolean processGN(String line, EPDRecord rec) {

        if( !line.startsWith("GN") )
            return false;

        // GN   Name=ENPP1;
        int pos1 = line.indexOf("Name=");
        int pos2 = line.lastIndexOf(';');
        if( pos1>0 && pos2>pos1 ) {
            String geneSymbol = line.substring(pos1 + 5, pos2).toUpperCase();
            rec.addGeneId(geneSymbol);
        }
        return true;
    }

    private boolean processDR(String line, EPDRecord rec) {

        if( !line.startsWith("DR") )
            return false;

        //DR   CLEANEX; MM_TNNI3.
        //extract potential gene symbol
        if( line.contains("CLEANEX;") ) {
            int pos1 = line.lastIndexOf('_');
            int pos2 = line.indexOf('.', pos1+1);
            if( pos1>0 && pos2>0 ) {
                String geneSymbol = line.substring(pos1 + 1, pos2).toUpperCase();
                rec.addGeneId(geneSymbol);
            }
            return true;
        }

        //DR   SWISS-PROT; P48787; TRIC_MOUSE.
        //extract potential gene symbol
        if( line.contains("SWISS-PROT;") ) {
            int pos1 = line.lastIndexOf(';');
            int pos2 = line.indexOf('_', pos1 + 1);
            if( pos1>0 && pos2>0 ) {
                String geneSymbol = line.substring(pos1+1, pos2).trim().toUpperCase();
                rec.addGeneId(geneSymbol);
            }

            // and create swiss-prot id
            pos1 = line.indexOf(';');
            pos2 = line.indexOf(';', pos1 + 1);
            if( pos1>0 && pos2>0 ) {
                String accId = line.substring(pos1+1, pos2).trim().toUpperCase();
                XdbId xdbId = new XdbId();
                xdbId.setAccId(accId);
                xdbId.setLinkText(accId);
                xdbId.setCreationDate(new Date());
                xdbId.setModificationDate(xdbId.getCreationDate());
                xdbId.setSrcPipeline(getSrcPipeline());
                xdbId.setXdbKey(XdbId.XDB_KEY_UNIPROT);
                rec.getXdbIds().addIncomingObject(xdbId);
            }
            return true;
        }

        //DR   MGD; MGI:98783; Tnni3.
        //extract potential gene symbol
        if( line.contains("MGD;") ) {
            int pos1 = line.lastIndexOf(';');
            int pos2 = line.indexOf('.', pos1+1);
            if( pos1>0 && pos2>0 ) {
                String geneSymbol = line.substring(pos1+1, pos2).trim().toUpperCase();
                rec.addGeneId(geneSymbol);
            }

            // and create xdb id
            pos1 = line.indexOf(';');
            pos2 = line.indexOf(';', pos1 + 1);
            if( pos1>0 && pos2>0 ) {
                String accId = line.substring(pos1+1, pos2).trim().toUpperCase();
                XdbId xdbId = new XdbId();
                xdbId.setAccId(accId);
                xdbId.setLinkText(accId);
                xdbId.setCreationDate(new Date());
                xdbId.setModificationDate(xdbId.getCreationDate());
                xdbId.setSrcPipeline(getSrcPipeline());
                xdbId.setXdbKey(XdbId.XDB_KEY_MGD);
                rec.getXdbIds().addIncomingObject(xdbId);
            }
            return true;
        }

        //DR   RefSeq; NM_078548.2.
        if( line.contains("RefSeq;") ) {
            // and create xdb id
            int pos1 = line.indexOf(';');
            int pos2 = line.indexOf('.', pos1 + 1);
            if( pos1>0 && pos2>0 ) {
                String accId = line.substring(pos1+1, pos2).trim().toUpperCase();
                XdbId xdbId = new XdbId();
                xdbId.setAccId(accId);
                xdbId.setLinkText(accId);
                xdbId.setCreationDate(new Date());
                xdbId.setModificationDate(xdbId.getCreationDate());
                xdbId.setSrcPipeline(getSrcPipeline());
                xdbId.setXdbKey(XdbId.XDB_KEY_GENEBANKNU);
                rec.getXdbIds().addIncomingObject(xdbId);
            }
            return true;
        }

        //DR   MIM; 180680.
        if( line.contains("MIM;") ) {
            // and create xdb id
            int pos1 = line.indexOf(';');
            int pos2 = line.indexOf('.', pos1 + 1);
            if( pos1>0 && pos2>0 ) {
                String accId = line.substring(pos1+1, pos2).trim().toUpperCase();
                XdbId xdbId = new XdbId();
                xdbId.setAccId(accId);
                xdbId.setLinkText(accId);
                xdbId.setCreationDate(new Date());
                xdbId.setModificationDate(xdbId.getCreationDate());
                xdbId.setSrcPipeline(getSrcPipeline());
                xdbId.setXdbKey(XdbId.XDB_KEY_OMIM);
                rec.getXdbIds().addIncomingObject(xdbId);
            }
            return true;
        }

        // DR   EPD; EP59001; MM_GPT_5; alternative promoter; [+513; +].
        if( line.contains("EPD;") ) {
            // extract EPD acc id
            int pos1 = line.indexOf(';');
            int pos2 = line.indexOf(';', pos1 + 1);
            if( pos1>0 && pos2>0 ) {
                String accId = line.substring(pos1+1, pos2).trim().toUpperCase();
                rec.addAltPromoter(accId);
            }
            return true;

        }
        return true;
    }

    private boolean processRN_RX_RA_RT_RL(String line, EPDRecord rec) {

        // ignore non-relevant lines from references: we only use pubmed ids
        if( line.startsWith("RN") )
            return true;
        if( line.startsWith("RA") )
            return true;
        if( line.startsWith("RT") )
            return true;
        if( line.startsWith("RL") )
            return true;

        if( !line.startsWith("RX") )
            return false;

        //RX   MEDLINE; 6159587.
        int pos1 = line.indexOf(';');
        int pos2 = line.indexOf('.', pos1 + 1);
        if( pos1>0 && pos2>0 ) {
            String accId = line.substring(pos1+1, pos2).trim().toUpperCase();
            XdbId xdbId = new XdbId();
            xdbId.setAccId(accId);
            xdbId.setLinkText(accId);
            xdbId.setCreationDate(new Date());
            xdbId.setModificationDate(xdbId.getCreationDate());
            xdbId.setSrcPipeline(getSrcPipeline());
            xdbId.setXdbKey(XdbId.XDB_KEY_PUBMED);
            rec.getXdbIds().addIncomingObject(xdbId);
        }
        return true;
    }

    private boolean processME(String line, EPDRecord rec) {

        if( !line.startsWith("ME") )
            return false;

        //ME   Nuclease protection; transfected or transformed cells [3].

        //The method lines describe experiments defining the transcription
        //initiation site. The format of the ME line is as follows:

        //ME   Method_description [; Qualifier...] [n,...].

        String str = line.substring(5).trim();
        if( !str.isEmpty() || !str.equals("none.") ) {
            // Nuclease protection; transfected or transformed cells [3].
            // removes the last occurrence of square brackets
            int pos = str.lastIndexOf('[');
            if( pos > 0 )
                str = str.substring(0, pos).trim();

            String ems = rec.getExperimentMethods();
            if( ems==null )
                ems = str;
            else
                ems += "; "+str;
            rec.setExperimentMethods(ems);
        }

        return true;
    }

    private boolean processFP(String line, EPDRecord rec) throws Exception {

        if( !line.startsWith("FP") )
            return false;

        // don't process this line if species is not set
        int species = rec.getPromoter().getSpeciesTypeKey();
        if( species==SpeciesType.ALL )
            return false;

        //FP   Hs c-myc         P2+:+S  EU:NC_000008.9       1+ 128817660; 11148.053 010*2

        // we extract locusAccId: NC_000008.9 and position within locus: 128817660
        // usually columns: [34..50]  [54..63]
        int startPos = line.indexOf("EU:NC_");
        if( startPos>=0 ) {
            startPos += 3;

            String locusAccId = line.substring(startPos, startPos+17).trim();
            String pos = line.substring(startPos+20, startPos+30).trim();

            // get chromosome for our locus
            Chromosome chr = Manager.getInstance().getLocusInfoManager().getChromosome(locusAccId, getDao());
            if( chr!=null ) {
                MapData md = new MapData();

                md.setMapKey(chr.getMapKey());

                md.setNotes("created for locus " + locusAccId);
                md.setSrcPipeline(getSrcPipeline());
                md.setChromosome(chr.getChromosome());
                md.setStartPos(Integer.parseInt(pos));
                md.setStopPos(md.getStartPos()+60);
                // note: sequence line shows a short sequence segment corresponding to the -49 to +10 region of the promoter

                MapsDataCollection mdCollection = rec.mds.get(md.getMapKey());
                if( mdCollection==null ) {
                    mdCollection = new MapsDataCollection();
                    mdCollection.setDao(getDao());
                    mdCollection.setMapKey(md.getMapKey());
                    rec.mds.put(md.getMapKey(), mdCollection);
                }
                mdCollection.addIncomingObject(md);
            }
        } else {
            getSession().incrementCounter("MAPS_DATA_CANNOT_PARSE_NON_CHROMOSOME_LOCI", 1);
            //startPos = line.indexOf("EM:");
            //System.out.println("--FP input cannot parse transcripts--" + line);
        }


        // optionally parse acc id
        if( rec.getPromoter().getSymbol()==null ) {
            int dotpos = line.lastIndexOf(".");
            int spacepos = line.lastIndexOf(" ", dotpos);
            if( dotpos>0 && spacepos>0 && spacepos<dotpos ) {
                String accId = line.substring(spacepos, dotpos).trim();
                rec.getPromoter().setSymbol(getSrcPipeline()+"_"+SpeciesType.getTaxonomicName(species).substring(0, 1).toUpperCase()+accId);
            }
        }
        return true;
    }

    private boolean processDO(String line, EPDRecord rec) {

        if( !line.startsWith("DO") )
            return false;

        //Documentation of promoter entries is presented on lines starting with
        //"DO". They are essentially free format and so far not processed by
        //specific programs. In the present release, there are two DO lines per
        //entry, the first referring to the transcript mapping experiments that
        //define the promoter, the second giving information about expression and
        //regulation.The varies experimental techniques are identified by number

        //DO        Experimental evidence: 3
        //DO        Expression/Regulation: liver, bone marrow (megakaryocytes)

        if( line.contains("Experimental evidence") ) {

            // turned off processing of exp evidence
            //processExperimentalEvidence(line, rec);
        }
        else if( line.contains("Expression/Regulation") ) {

            String ev = line.substring(line.indexOf(':')+1).trim();
            if( !ev.isEmpty() ) {

                if( ev.contains("+") )
                    ev = ev.replaceAll("[+]", " {induced by or strongly expressed in} ");
                if( ev.contains("-") )
                    ev = ev.replaceAll("[-]", " {repressed by or weakly expressed in} ");
                if( ev.contains("~") )
                    ev = ev.replaceAll("[~]", " {modulated by} ");

                String str = ev.trim();
                if( !str.isEmpty() ) {
                    if( str.contains("{") ) {
                        String ed = rec.getRegulationData();
                        if( ed==null )
                            ed = str;
                        else
                            ed += "; " + str;
                        rec.setRegulationData(ed.replace('{','(').replace('}',')'));
                    }
                    else {
                        String ed = rec.getExpressionData();
                        if( ed==null )
                            ed = str;
                        else
                            ed += "; " + str;
                        rec.setExpressionData(ed);
                    }
                }
            }
        }
        return true;
    }

    private void processExperimentalEvidence(String line, EPDRecord rec) {

        String ev = line.substring(line.indexOf(':')+1).trim();
        for( String evCode: ev.split("[,]") ) {

            // look for extra codes at the end
            while( evCode.length()>1 ) {
                char c = evCode.charAt(evCode.length()-1);
                if( !Character.isDigit(c) ) {

                    String str = getExperimentEvidences().get(Character.toString(c));
                    if( !str.isEmpty() )
                        appendToNotes(rec.getPromoter(), "experimental_evidence="+str);

                    evCode = evCode.substring(0, evCode.length()-1);
                }
                else
                    break;
            }

            String str = getExperimentEvidences().get(evCode);
            if( str==null )
                str = evCode;
            if( !str.isEmpty() )
                appendToNotes(rec.getPromoter(), "experimental_evidence="+str);
        }
    }

    private boolean processSE(String line, EPDRecord rec) throws Exception {

        if( !line.startsWith("SE") )
            return false;

        //The sequence line shows a short sequence segment corresponding to the
        //-49 to +10 region of the promoter. Transcribed and untranscribed
        //nucleotides are represented by upper and lower case characters,
        //respectively. This line type is not meant to provide sequence data but
        //serves as a control string for sequence extraction.
        Sequence seq = new Sequence();
        seq.setSeqData(line.substring(2).trim());
        seq.setSeqType("promoter_region");
        rec.getSeq().addIncomingObject(seq);
        return true;
    }

    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setExperimentEvidences(Map<String,String> experimentEvidences) {
        this.experimentEvidences = experimentEvidences;
    }

    public Map<String,String> getExperimentEvidences() {
        return experimentEvidences;
    }

    public String getSrcPipeline() {
        return srcPipeline;
    }

    public void setSrcPipeline(String srcPipeline) {
        this.srcPipeline = srcPipeline;
    }
}
