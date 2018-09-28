package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.MapData;
import edu.mcw.rgd.pipelines.RgdObjectSyncer;
import edu.mcw.rgd.process.Utils;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * @author mtutaj
 * @since 4/19/12
 * <p>
 * Synchronizes positions between incoming data and data in RGD.
 * New positions are added or modified if needed.
 * If incoming data does not contain any data, NOTHING is deleted in database -- the assumption
 * is that some maps could be discontinued, but most likely we would like to preserve the positions
 * from last good load.
 */
public class MapsDataCollection extends RgdObjectSyncer {

    private int mapKey;

    public MapsDataCollection() {
        setLog(LogFactory.getLog("genomic_pos"));
    }

    @Override
    protected boolean equalsByUniqueKey(Object o, Object o1) {
        MapData md1 = (MapData) o;
        MapData md2 = (MapData) o1;
        return Utils.intsAreEqual(md1.getMapKey(), md2.getMapKey()) &&
               Utils.stringsAreEqual(md1.getSrcPipeline(), md2.getSrcPipeline());
    }

    @Override
    protected boolean equalsByContents(Object o, Object o1) {
        MapData md1 = (MapData) o;
        MapData md2 = (MapData) o1;
        return md1.equalsByGenomicCoords(md2) &&
               Utils.stringsAreEqual(md1.getSrcPipeline(), md2.getSrcPipeline());
    }

    @Override
    protected List getDataInRgd(int rgdId) throws Exception {
        Dao dao = (Dao) getDao();
        return dao.getMapData(rgdId, getMapKey());
    }

    @Override
    protected int insertDataIntoRgd(List list) throws Exception {
        Dao dao = (Dao) getDao();
        return dao.insertMapData(list);
    }

    @Override
    protected int updateDataInRgd(List list) throws Exception {
        Dao dao = (Dao) getDao();
        return dao.updateMapData(list);
    }

    @Override
    protected int deleteDataFromRgd(List list) throws Exception {
        Dao dao = (Dao) getDao();
        return dao.deleteMapData(list);
    }

    @Override
    protected void copyObjectUniqueKey(Object o, Object o1) {
        MapData md1 = (MapData) o;
        MapData md2 = (MapData) o1;
        md1.setKey(md2.getKey());
    }

    @Override
    protected void prepare(Object obj, int rgdId, Object userData, int context) {
        MapData md1 = (MapData) obj;
        md1.setRgdId(rgdId);
        md1.setNotes(userData.toString());
        md1.setMapKey(getMapKey());
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

    @Override
    public boolean isDeletable() {
        return true;
    }

    public int getMapKey() {
        return mapKey;
    }

    public void setMapKey(int mapKey) {
        this.mapKey = mapKey;
    }
}
