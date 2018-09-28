package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.Chromosome;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mtutaj
 * @since 4/19/12
 */
public class LocusInfoManager {

    private Map<String, Chromosome> locus2chr = new HashMap<>();

    synchronized public Chromosome getChromosome(String locus, Dao dao) throws Exception {

        // check if we have the locus in the map
        Chromosome chr = locus2chr.get(locus);
        if( chr!=null ) {
            return chr;
        }

        // locus is not in the map: download from NCBI
        chr = dao.getChromosome(locus);
        if( chr!=null ) {
            locus2chr.put(locus, chr);
        }
        return chr;
    }
}
