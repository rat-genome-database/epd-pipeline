package edu.mcw.rgd.pipelines.EPD;

import edu.mcw.rgd.datamodel.ExpressionData;
import edu.mcw.rgd.pipelines.RgdObjectSyncer;
import edu.mcw.rgd.process.Utils;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 4/9/12
 * Time: 4:16 PM
 * to efficiently handle synchronization between incoming data and rgd of genomic element attributes;
 * attributes loaded previously are never removed
 */
public class ExpressionDataCollection extends RgdObjectSyncer {

    public ExpressionDataCollection() {
        setLog(LogFactory.getLog("expression_data"));
    }


    @Override
    protected boolean equalsByUniqueKey(Object obj1, Object obj2) {
        return equalsByContents(obj1, obj2);
    }

    @Override
    protected boolean equalsByContents(Object obj1, Object obj2) {
        ExpressionData el1 = (ExpressionData) obj1;
        ExpressionData el2 = (ExpressionData) obj2;

        return Utils.stringsAreEqualIgnoreCase(el1.getTissue(), el2.getTissue()) &&
            Utils.stringsAreEqualIgnoreCase(el1.getExperimentMethods(), el2.getExperimentMethods()) &&
            Utils.stringsAreEqualIgnoreCase(el1.getRegulation(), el2.getRegulation());
    }

    @Override
    protected List getDataInRgd(int rgdId) throws Exception {
        Dao dao = (Dao) getDao();
        return dao.getExpressionData(rgdId);
    }

    @Override
    protected int insertDataIntoRgd(List list) throws Exception {
        Dao dao = (Dao) getDao();
        return dao.insertExpressionData(list);
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
        //
    }

    @Override
    protected void prepare(Object obj, int rgdId, Object userData, int context) {
        ExpressionData el1 = (ExpressionData) obj;
        el1.setRgdId(rgdId);
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
