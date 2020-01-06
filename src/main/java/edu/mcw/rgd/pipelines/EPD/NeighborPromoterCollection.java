package edu.mcw.rgd.pipelines.EPD;

/**
 * @author mtutaj
 * @since 4/16/12
 * collection of associations between promoter and promoter
 * (type 'neighboring_promoter')
 */
public class NeighborPromoterCollection extends AssociationCollection {

    // THREAD SAFE SINGLETON -- start
    // private instance, so that it can be accessed by only by getInstance() method
    private static NeighborPromoterCollection instance;

    private NeighborPromoterCollection() {
        // private constructor
    }

    //synchronized method to control simultaneous access
    synchronized public static NeighborPromoterCollection getInstance() {
        if (instance == null) {
            // if instance is null, initialize
            instance = new NeighborPromoterCollection();
        }
        return instance;
    }
    // THREAD SAFE SINGLETON -- end


    @Override
    public String getAssocType() {
        return "neighboring_promoter";
    }

    @Override
    public String getLogPrefix() {
        return "NEIGHBORING_PROMOTER_ASSOC";
    }

    @Override
    public String getLogName() {
        return "assoc_promoters";
    }
}
