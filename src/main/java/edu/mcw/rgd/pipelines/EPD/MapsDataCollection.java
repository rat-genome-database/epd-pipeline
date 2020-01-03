package edu.mcw.rgd.pipelines.EPD;


import edu.mcw.rgd.datamodel.MapData;
import edu.mcw.rgd.process.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author mtutaj
 * @since 4/19/12
 * Synchronizes positions between incoming data and data in RGD.
 */
public class MapsDataCollection {

    // THREAD SAFE SINGLETON -- start
    // private instance, so that it can be accessed by only by getInstance() method
    private static MapsDataCollection instance;

    private MapsDataCollection() {
        // private constructor
    }

    //synchronized method to control simultaneous access
    synchronized public static MapsDataCollection getInstance() {
        if (instance == null) {
            // if instance is null, initialize
            instance = new MapsDataCollection();
        }
        return instance;
    }
    // THREAD SAFE SINGLETON -- end
    //


    Logger log = Logger.getLogger("status");

    private final Set<MapDataEx> incoming = new HashSet<>();

    public void addIncoming(MapData md) {

        // there is only one instance of this class
        synchronized (incoming) {
            incoming.add(new MapDataEx(md));
        }
    }

    synchronized public void qc(Dao dao, String[] sources) throws Exception {

        List<MapDataEx> mdsInRgdEx = getMapDataInRgd(dao, sources);

        // determine map data for insertion
        Collection<MapDataEx> forInsert = CollectionUtils.subtract(incoming, mdsInRgdEx);

        // determine map data for deletion
        Collection<MapDataEx> forDelete = CollectionUtils.subtract(mdsInRgdEx, incoming);

        Collection<MapDataEx> matching = CollectionUtils.intersection(mdsInRgdEx, incoming);


        if( !forInsert.isEmpty() ) {
            dao.insertMapData(new ArrayList<>(forInsert));
            log.info("MAPS_DATA_INSERTED: "+forInsert.size());
        }

        if( !forDelete.isEmpty() ) {
            dao.deleteMapData(new ArrayList<>(forDelete));
            log.info("MAPS_DATA_DELETED: "+forDelete.size());
        }

        int matchingMapData = matching.size();
        if( matchingMapData!=0 ) {
            log.info("MAPS_DATA_MATCHING: "+matchingMapData);
        }
    }

    List<MapDataEx> getMapDataInRgd(Dao dao, String[] sources) throws Exception {

        // determine which map keys are present in the incoming data
        Set<Integer> processedMapKeys = new HashSet<>();
        for( MapData md: incoming ) {
            processedMapKeys.add(md.getMapKey());
        }

        // load all map positions in rgd
        int inRgdDataSuppressed = 0;
        List<MapData> mdsInRgd = dao.getMapData(sources);
        List<MapDataEx> mdsInRgdEx = new ArrayList<>(mdsInRgd.size());
        for( MapData md: mdsInRgd ) {
            if( processedMapKeys.contains(md.getMapKey()) ) {
                mdsInRgdEx.add(new MapDataEx(md));
            } else {
                inRgdDataSuppressed++;
            }
        }
        if( inRgdDataSuppressed!=0 ) {
            log.info("  MAPS_DATA suppressed in RGD positions because their map keys not present in incoming data: " + inRgdDataSuppressed);
        }
        return mdsInRgdEx;
    }

    /// this class is same as MapData, but it has refined equals() and hashCode() methods suitable for sorting of genomic positions on multiple assemblies
    class MapDataEx extends MapData {

        public MapDataEx(MapData md) {
            setKey(md.getKey());
            setMapKey(md.getMapKey());
            setRgdId(md.getRgdId());
            setChromosome(md.getChromosome());
            setStartPos(md.getStartPos());
            setStopPos(md.getStopPos());
            setSrcPipeline(md.getSrcPipeline());
            setNotes(md.getNotes());
        }

        public boolean equals(Object o) {
            MapData md = (MapData) o;
            return super.equals(o)
                && Utils.stringsAreEqual(getChromosome(), md.getChromosome())
                && Utils.intsAreEqual(getStartPos(), md.getStartPos())
                && Utils.intsAreEqual(getStopPos(), md.getStopPos())
                && Utils.stringsAreEqual(getSrcPipeline(), md.getSrcPipeline());
        }

        public int hashCode() {
            return super.hashCode()
                ^ Utils.defaultString(getChromosome()).hashCode()
                ^ (getStartPos()==null ? 0 : getStartPos())
                ^ (getStopPos()==null ? 0 : getStopPos())
                ^ Utils.defaultString(getSrcPipeline()).hashCode();
        }
    }
}
