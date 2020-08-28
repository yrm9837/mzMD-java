package edu.msViz.mzMD;

import java.util.List;

/**
 * Class embodiment of a Mass Spectrometry data map
 * @author Runmin
 */
public class MsDataMap {

    public static int MIN_POINT_NUM = 100;
    public static double SCALE_EACH_LEVEL = 1.5;
    public static double SCALE_FIRST_LEVEL = 4;
	
    // the msDataMap
//	private Map<Integer, Map<Integer, MsDataPoint>> dataMap = new LinkedHashMap<>();
    public int[][] dataMap;
	
    public List<MsDataPoint> msData;

	// the level of this msDataMap
	public int mapLevel;
	
	// the number of points in this msDataMap
	private int pointNumber;
	
	// the size of each single map
	public int mzSize;
	
	// the size of dataMap
	public int rtSize;
	
	// minimum mz at this map
	public double mzMin;
    
    // maximum mz at this map
	public double mzMax;
    
    // minimum rt at this map
	public double rtMin;
    
    // maximum rt at this map
	public double rtMax;
	
	// the density of this map
	public double density;
	private double rtDensity;
	private double mzDensity;
	
	// the density of origin dataset or last dataMap
	private double lastDensity;

    /**
     * Default constructor accepting dataset
     * @param dataset
     */
    public MsDataMap(List<MsDataPoint> msData, double originDensity)
    {
    	MIN_POINT_NUM = Configure.MIN_POINT_NUM;
    	SCALE_EACH_LEVEL = Configure.SCALE_EACH_LEVEL;
    	SCALE_FIRST_LEVEL = Configure.SCALE_FIRST_LEVEL;
    	this.msData = msData;
    	buildMapFromData(this.msData, originDensity);
    }
    public MsDataMap(List<MsDataPoint> msData, double originDensity, boolean jump)
    {
    	if(jump)
    	{
	    	MIN_POINT_NUM = Configure.MIN_POINT_NUM;
	    	SCALE_EACH_LEVEL = Math.pow(Configure.SCALE_EACH_LEVEL,2);
	    	SCALE_FIRST_LEVEL = Configure.SCALE_FIRST_LEVEL / Configure.SCALE_EACH_LEVEL;
    	}else
    	{
    		MIN_POINT_NUM = Configure.MIN_POINT_NUM;
        	SCALE_EACH_LEVEL = Configure.SCALE_EACH_LEVEL;
        	SCALE_FIRST_LEVEL = Configure.SCALE_FIRST_LEVEL;
    	}
    	this.msData = msData;
    	buildMapFromData(this.msData, originDensity);
    	this.mapLevel++;
    }

    /**
     * Default constructor accepting lower level msDataMap
     * @param dataset
     */
    public MsDataMap(MsDataMap lastDataMap)
    {
    	MIN_POINT_NUM = Configure.MIN_POINT_NUM;
    	SCALE_EACH_LEVEL = Configure.SCALE_EACH_LEVEL;
    	SCALE_FIRST_LEVEL = Configure.SCALE_FIRST_LEVEL;
    	buildMapFromMap(lastDataMap);
    }
    public MsDataMap(MsDataMap lastDataMap, boolean jump)
    {
    	if(jump)
    	{
	    	MIN_POINT_NUM = Configure.MIN_POINT_NUM;
	    	SCALE_EACH_LEVEL = Math.pow(Configure.SCALE_EACH_LEVEL,2);
	    	SCALE_FIRST_LEVEL = Configure.SCALE_FIRST_LEVEL / Configure.SCALE_EACH_LEVEL;
    	}else
    	{
        	MIN_POINT_NUM = Configure.MIN_POINT_NUM;
        	SCALE_EACH_LEVEL = Configure.SCALE_EACH_LEVEL;
        	SCALE_FIRST_LEVEL = Configure.SCALE_FIRST_LEVEL;
    	}
    	buildMapFromMap(lastDataMap);
    	this.mapLevel++;
    }

    /**
     * Constructs an msDataMap from the dataset
     * @param dataset
     */
    private void buildMapFromData(List<MsDataPoint> msData, double originDensity)
    {
    	this.pointNumber = 0;
    	this.mapLevel = 1;
    	findBoundOfData(msData);
    	if(originDensity == 0)
    	{
    		originDensity = msData.size()/((this.rtMax - this.rtMin)*(this.mzMax - this.mzMin));
    	}
    	this.lastDensity = originDensity*MsDataMap.SCALE_FIRST_LEVEL;
    	this.density = this.lastDensity;
    	if(Configure.isPartitionedLoad)
    	{
	    	this.rtDensity = Math.sqrt(this.density*(double)Configure.partition);
	    	this.mzDensity = Math.sqrt(this.density/(double)Configure.partition);
    	}
    	else
    	{
	    	this.rtDensity = Math.sqrt(this.density);
	    	this.mzDensity = Math.sqrt(this.density);
    	}
    	this.rtSize = (int) Math.ceil(this.rtDensity*(this.rtMax - this.rtMin));
    	this.mzSize = (int) Math.ceil(this.mzDensity*(this.mzMax - this.mzMin));
    	initMap();
    	addDataToMapFromDataset(msData);
    }

    private void initMap() 
    {
    	dataMap = new int[rtSize][mzSize];
    	for(int i = 0; i < rtSize; i++)
    	{
    		for(int j = 0; j < mzSize; j++)
    		{
    			dataMap[i][j] = 0;
    		}
    	}
    }
    void addDataToMapFromDataset(List<MsDataPoint> msData)
    {
    	for(int i = 0; i < msData.size(); i++) 
    	{
            MsDataPoint curPoint = msData.get(i);
            int rtIndex = (int) Math.floor((curPoint.rt-this.rtMin)*this.rtDensity);
            int mzIndex = (int) Math.floor((curPoint.mz-this.mzMin)*this.mzDensity);
            int pointIndex = dataMap[rtIndex][mzIndex] - 1;
            if (pointIndex == -1 || curPoint.intensity > msData.get(pointIndex).intensity)
            {
            	if(pointIndex == -1)
            	{
            		this.pointNumber++;
            	}
            	this.dataMap[rtIndex][mzIndex] = i + 1;
            }
    	}
    }
    
    /**
     * Discovers min/max mz/rt
     * @param msData dataset to process
     */
    private void findBoundOfData(List<MsDataPoint> msData)
    {
    	 // discover the minimums and maximums for mz,rt
    	if(msData.size() > 0)
    	{
    		 this.mzMax = msData.get(0).mz;
             this.mzMin = msData.get(0).mz;
             this.rtMax = msData.get(0).rt;
             this.rtMin = msData.get(0).rt;
    	}
        for (int i = 1; i < msData.size(); i++) {
            MsDataPoint curPoint = msData.get(i);
            
            // keep largest maxes, smallest mins
            // mz
            this.mzMax = (curPoint.mz > this.mzMax) ? curPoint.mz : this.mzMax;
            this.mzMin = (curPoint.mz < this.mzMin) ? curPoint.mz : this.mzMin;
            // rt
            this.rtMax = (curPoint.rt > this.rtMax) ? curPoint.rt : this.rtMax;
            this.rtMin = (curPoint.rt < this.rtMin) ? curPoint.rt : this.rtMin;
        }
    }
    
    /**
     * Constructs an msDataMap from the lower level msDataMap
     * @param dataset
     */
    private void buildMapFromMap(MsDataMap lastDataMap)
    {
    	this.pointNumber = 0;
    	this.mapLevel = lastDataMap.mapLevel + 1;
    	this.msData = lastDataMap.msData;

        this.mzMax = lastDataMap.mzMax;
        this.mzMin = lastDataMap.mzMin;
        this.rtMax = lastDataMap.rtMax;
        this.rtMin = lastDataMap.rtMin;
        
    	this.lastDensity = lastDataMap.density;
    	this.density = this.lastDensity/MsDataMap.SCALE_EACH_LEVEL;
    	if(Configure.isPartitionedLoad)
    	{
	    	this.rtDensity = Math.sqrt(this.density*(double)Configure.partition);
	    	this.mzDensity = Math.sqrt(this.density/(double)Configure.partition);
    	}
    	else
    	{
	    	this.rtDensity = Math.sqrt(this.density);
	    	this.mzDensity = Math.sqrt(this.density);
    	}
    	this.rtSize = (int) Math.ceil(this.rtDensity*(this.rtMax - this.rtMin));
    	this.mzSize = (int) Math.ceil(this.mzDensity*(this.mzMax - this.mzMin));
    	initMap();
    	addDataToMapFromMap(lastDataMap);
    }
    void addDataToMapFromMap(MsDataMap lastDataMap)
    {
    	for(int i = 0; i < lastDataMap.rtSize; i++) 
    	{
    		for(int j = 0; j < lastDataMap.mzSize; j++) 
    		{
    			int curDataIndex = lastDataMap.dataMap[i][j]-1;
    			if(curDataIndex != -1)
    			{
    				MsDataPoint curPoint = this.msData.get(curDataIndex);
    				int rtIndex = (int) Math.floor((curPoint.rt-this.rtMin)*this.rtDensity);
    	            int mzIndex = (int) Math.floor((curPoint.mz-this.mzMin)*this.mzDensity);
    	            int pointIndex = this.dataMap[rtIndex][mzIndex] - 1;

    	            if (pointIndex == -1 || curPoint.intensity > this.msData.get(pointIndex).intensity)
    	            {

    	            	if(pointIndex == -1)
    	            	{
    	            		this.pointNumber++;
    	            	}
    	            	this.dataMap[rtIndex][mzIndex] = curDataIndex + 1;
    	            }
    			}
    		}
    	}
    }
    
    public boolean nextLevel()
    {
    	if(this.pointNumber > MsDataMap.MIN_POINT_NUM)
    		return true;
    	else
    		return false;
    }

	@Override
	public String toString() {
		return "MsDataMap [mapLevel=" + mapLevel
				+ ", pointNumber=" + pointNumber + ", mzSize=" + mzSize + ", rtSize=" + rtSize + ", density=" + density + ", rtDensity="
				+ rtDensity + ", mzDensity=" + mzDensity + ", lastDensity=" + lastDensity + "]";
//		return "MsDataMap [mapLevel=" + mapLevel
//				+ ", pointNumber=" + pointNumber + ", mzSize=" + mzSize + ", rtSize=" + rtSize + ", mzMin=" + mzMin
//				+ ", mzMax=" + mzMax + ", rtMin=" + rtMin + ", rtMax=" + rtMax + ", density=" + density + ", rtDensity="
//				+ rtDensity + ", mzDensity=" + mzDensity + ", lastDensity=" + lastDensity + "]";
	}
    
    
}
