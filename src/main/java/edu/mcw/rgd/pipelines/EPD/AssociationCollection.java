package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.Association;
import edu.mcw.rgd.pipelines.RgdObjectSyncer;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 4/16/12
 * Time: 12:59 PM
 * collection of associations for promoter
 */
abstract public class AssociationCollection extends RgdObjectSyncer {

    @Override
    protected boolean equalsByUniqueKey(Object obj1, Object obj2) {
        Association a1 = (Association) obj1;
        Association a2 = (Association) obj2;
        return a1.getDetailRgdId()==a2.getDetailRgdId() &&
               a1.getAssocType().equals(a2.getAssocType());
    }

    @Override
    protected boolean equalsByContents(Object obj1, Object obj2) {
        return false; //n/a
    }

    @Override
    protected int insertDataIntoRgd(List list) throws Exception {
        Dao dao = (Dao) getDao();
        for( Association assoc: (List<Association>) list ) {
            dao.insertAssociation(assoc);
        }
        return list.size();
    }

    @Override
    protected int updateDataInRgd(List list) throws Exception {
        return 0;
    }

    @Override
    protected int deleteDataFromRgd(List list) throws Exception {
        return 0;
    }

    @Override
    protected void copyObjectUniqueKey(Object toObj, Object fromObj) {
        Association x1 = (Association) toObj;
        Association x2 = (Association) fromObj;
        x1.setAssocKey(x2.getAssocKey());
    }

    @Override
    protected void prepare(Object obj, int rgdId, Object userData, int context) {
        Association x = (Association) obj;
        x.setMasterRgdId(rgdId);
    }

    @Override
    public boolean isUpdatable() {
        return false;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }
}
