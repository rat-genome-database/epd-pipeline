package edu.mcw.rgd.pipelines.EPD;

/**
 * @author mtutaj
 * @since 4/16/12
 * collection of associations between promoter and promoter
 * (type 'alternative_promoter')
 */
public class AlternativePromoterCollection extends AssociationCollection {

    // THREAD SAFE SINGLETON -- start
    // private instance, so that it can be accessed by only by getInstance() method
    private static AlternativePromoterCollection instance;

    private AlternativePromoterCollection() {
        // private constructor
    }

    //synchronized method to control simultaneous access
    synchronized public static AlternativePromoterCollection getInstance() {
        if (instance == null) {
            // if instance is null, initialize
            instance = new AlternativePromoterCollection();
        }
        return instance;
    }
    // THREAD SAFE SINGLETON -- end


    @Override
    public String getAssocType() {
        return "alternative_promoter";
    }

    @Override
    public String getLogPrefix() {
        return "ALTERNATIVE_PROMOTER_ASSOC";
    }

    @Override
    public String getLogName() {
        return "assoc_promoters";
    }
}
