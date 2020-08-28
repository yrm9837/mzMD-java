package edu.msViz.mzMD;


public class Configure {

	// store options, bottomUp first store level 0 map, top down store level 0 map at last, only topDown can deRedundancy
    public static enum StoreType { bottomUp, topDown, jump };
    public static StoreType storeChoice = StoreType.topDown;//
    
    // SQL options 
    public static enum SqlType { NormalSql, RTreeSql, DeRedundancy };
    public static SqlType sqlChoice = SqlType.DeRedundancy;//
    // use memory or file to store the sql database
    public static enum SqlMode { Memory, File };
    public static SqlMode sqlModeChoice = SqlMode.File;
    
    // the points number of the max level map
    public static int MIN_POINT_NUM = 1000;
    // the scale of the number of each to level
    public static double SCALE_EACH_LEVEL = 2;
    // the density of level 1 map compare with the origin density
    public static double SCALE_FIRST_LEVEL = 1;
    
//---partition setting
    public static boolean isPartitionedLoad = false;
//    public static int partition = (int) Math.ceil(Math.sqrt(Configure.MIN_POINT_NUM));
    public static int partition = 4;
//--
    
    // the type of simulation shape
    public static enum SimulationType {Random, SameShape};
    public static SimulationType SimulationChoice = SimulationType.Random;
    
    //Quantification
    public static enum QuantifyType {Standard, Ratio};
    public static QuantifyType QuantifyTypeChoice = QuantifyType.Ratio;
    
}
