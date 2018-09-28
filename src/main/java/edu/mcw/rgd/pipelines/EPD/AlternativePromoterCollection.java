package edu.mcw.rgd.pipelines.EPD;

import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 4/16/12
 * Time: 12:59 PM
 * collection of associations between promoter and promoter
 * (type 'alternative_promoter')
 */
public class AlternativePromoterCollection extends AssociationCollection {

    public AlternativePromoterCollection() {
        setLog(LogFactory.getLog("assoc_promoters"));
    }

    @Override
    protected List getDataInRgd(int rgdId) throws Exception {
        Dao dao = (Dao) getDao();
        return dao.getAssociations(rgdId, "alternative_promoter");
    }

}