package edu.mcw.rgd.pipelines.EPD;

/**
 * @author mtutaj
 * @since 4/16/12
 * collection of associations between promoter and genes (type 'promoter_to_gene')
 */
public class GeneAssociationCollection extends AssociationCollection {

    // THREAD SAFE SINGLETON -- start
    // private instance, so that it can be accessed by only by getInstance() method
    private static GeneAssociationCollection instance;

    private GeneAssociationCollection() {
        // private constructor
    }

    //synchronized method to control simultaneous access
    synchronized public static GeneAssociationCollection getInstance() {
        if (instance == null) {
            // if instance is null, initialize
            instance = new GeneAssociationCollection();
        }
        return instance;
    }
    // THREAD SAFE SINGLETON -- end


    @Override
    public String getAssocType() {
        return "promoter_to_gene";
    }

    @Override
    public String getLogPrefix() {
        return "GENE_ASSOC";
    }

    @Override
    public String getLogName() {
        return "assoc_genes";
    }
}
