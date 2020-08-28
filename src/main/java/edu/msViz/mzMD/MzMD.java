package edu.msViz.mzMD;


import edu.msViz.base.ImportState;
import edu.msViz.base.MzmlParser;
import edu.msViz.storage.StorageFacade;
import edu.msViz.storage.StorageFacadeFactory;
import edu.msViz.base.ImportState.ImportStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;


	/**
	 * Multiple R-Tree Database implementation for storing and accessing MS data
	 * @author runmin
	 */
	/**
	 * @author yangr
	 *
	 */
	public class MzMD 
	{
	    private static final Logger LOGGER = Logger.getLogger(MzMD.class.getName());
	    
	    private MsBounds msBounds = new MsBounds();
	    
	    public List<Double> densities = new ArrayList<Double>();

	    public static final boolean STORE_ORIGIN_DATA = true;
	    
		public Path outPath = null;

//		public int totalNumber = 0;
		public double originDensity = 0;
		
		// store options, bottomUp first store level 0 map, top down store level 0 map at last
	    private Configure.StoreType storeChoice;
	    
	    // disk storage implementation
	    public StorageFacade dataStorage;
	    
	    // static storage interface choice
	    private static final StorageFacadeFactory.Facades STORAGE_INTERFACE_CHOICE = StorageFacadeFactory.Facades.NormalDB;
	    
	    // import progress monitor
	    private ImportState importState;
	        
	    /**
	     * No argument constructor for basic initialization
	     * @param importMonitor import progress monitor
	     */
	    public MzMD() {
	        this.importState = new ImportState();
	        this.storeChoice = Configure.storeChoice;
	    }


	    // A custom ConvertDestinationProvider can be set on the MzTree
	    // which will be used to determine where to save a converted MzTree file on import of csv or mzML
	    public interface ConvertDestinationProvider {
	        Path getDestinationPath(Path suggestedFilePath) throws Exception;
	    }

	    private ConvertDestinationProvider convertDestinationProvider = null;

	    public void setConvertDestinationProvider(ConvertDestinationProvider convertDestinationProvider) {
	        System.out.println(Thread.currentThread().getStackTrace()[1].getMethodName());
	        this.convertDestinationProvider = convertDestinationProvider;
	    }

	    //***********************************************//
	    //                     LOAD                      //
	    //***********************************************//

	    private Path getConvertDestinationPath(Path sourceFilePath) throws Exception {
	        // date time file name
//	        String suggestedFileName = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss").format(new java.util.Date()) + ".mzTree";
	        String suggestedFileName = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss").format(new java.util.Date());
	        Path suggestedFilePath = sourceFilePath.resolveSibling(suggestedFileName);
	        this.outPath = suggestedFilePath;
//	        judeDirExists(suggestedFilePath);
	        suggestedFilePath = suggestedFilePath.resolve("result.mzMD");

	        // if a ConvertDestinationProvider has been given, use it to determine the output path
	        if (convertDestinationProvider == null) {
	            return suggestedFilePath;
	        } else {
	            return convertDestinationProvider.getDestinationPath(suggestedFilePath);
	        }
	    }

	    /**
	     * 
	     * Loads an mzTree either by building from mzML or by reconnecting to an mzTree
	     * @param filePath The path to the mzML or mzTree file
	     * @param summarizationStrategy strategy to be used for summarization of points
	     * @throws Exception 
	     */
	    public void load(String filePath) throws Exception
	    {  

	        // create ImportState and set source file location
	        this.importState.reset();
	        importState.setSourceFilePath(filePath);
	        
	        try 
	        {
	            long start = System.currentTimeMillis();

	            // csv load
	            if(filePath.endsWith(".csv"))
	            {
	                this.csvLoad(filePath);
	            }
	            
	            // mzml load
	            else 
	            {
	                // initialize mzmlParser
	                MzmlParser mzmlParser = new MzmlParser(filePath);
	
	                // if the user specified a memory-conservative load
	                // and the partitioned load is necessary then perform a partitioned load
	                if(this.partitionedLoadConfiguration(mzmlParser, Paths.get(filePath)))
	                {
	                    this.partitionedLoad(mzmlParser);
	                }
	
	                // else perform standard, memory-apathetic load
	                else
	                {
	                    // initialize mzmlParser
	                    this.standardLoad(mzmlParser, filePath);
	                }
	            }


	            importState.setImportStatus(ImportStatus.READY);
	            LOGGER.log(Level.INFO, "MD Build Real Time: " + (System.currentTimeMillis() - start));
	            
	        } 
	        catch (DataFormatException | XMLStreamException ex) 
	        {
	            // try as an mzDB file instead of XML+build in the 
	            // occurence of DataFormatException or XMLStreamException

	            // inform the importState of an .mzDB load
	            this.importState.setImportStatus(ImportStatus.LOADING_MZDB);
	            
	            // initialize data storage on mzDB file
	            this.initDataStorage(STORAGE_INTERFACE_CHOICE, filePath, null);

	            msBounds.update(this.dataStorage.getBaseInfomation());
	            
	            densities = this.dataStorage.getDensities();
			    LOGGER.log(Level.INFO, "After:"+densities.toString());

	            // inform importState that mzTree load has finished
	            this.importState.setImportStatus(ImportStatus.READY);
	        }
	    }
	    
	    /**
	     * Performs a standard, memory-apathetic load
	     * @param mzmlParser input file parser initialized w/ target file
	     * @throws IOException
	     * @throws XMLStreamException
	     * @throws DataFormatException 
	     */
	    private void standardLoad(MzmlParser mzmlParser, String filePath) throws Exception
	    {   
	        importState.setImportStatus(ImportStatus.PARSING);

	        List<MsDataPoint> dataset = mzmlParser.readAllData();

	        this.buildTreeFromRoot(dataset, Paths.get(filePath));
	    }
	    

	    /**
	     * Loads MS data in csv format (mz, rt, intensity, meta1)
	     * @param filePath path to csv file
	     * @throws FileNotFoundException
	     * @throws IOException
	     * @throws Exception 
	     */
	    private void csvLoad(String filePath) throws FileNotFoundException, IOException, Exception
	    {
	        this.importState.setImportStatus(ImportStatus.PARSING);
	        
	        ArrayList<MsDataPoint> points = new ArrayList<>();
	        
	        // open csv reader on targetted csv file
	        CSVReader reader = new CSVReader(new FileReader(filePath));
	        
	        // first line might be a header
	        String[] line = reader.readNext();
	        
	        // if the first line is not a header
	        if(line != null && StringUtils.isNumeric(line[0]))
	        {
	            // convert to msdatapoint, collect
	            MsDataPoint point = this.csvRowToMsDataPoint(line);
	            points.add(point);
	            msBounds.update(point.mz, point.rt);
	        }
	        
	        // read the remaining lines (now guaranteed no header)
	        while((line = reader.readNext()) != null)
	        {
	            // convert to msdatapoint, collect
	            MsDataPoint point = this.csvRowToMsDataPoint(line);
	            points.add(point);
	            msBounds.update(point.mz, point.rt);
	        }
	        
	        // build that tree!
	        this.buildTreeFromRoot(points, Paths.get(filePath));
	        
	    }
	    
	    /**
	     * Constructs an MzTree from the dataset, starting at the root node (so no partitioned load)
	     * @param dataset
	     * @throws Exception 
	     */
	    public void buildTreeFromRoot(List<MsDataPoint> dataset, Path sourceFilePath) throws Exception
	    {
		    LOGGER.log(Level.INFO, "Building MzMD from " + dataset.size() + " points");
	    	if(sourceFilePath != null)
	    	{
		        this.initDataStorage(STORAGE_INTERFACE_CHOICE, getConvertDestinationPath(sourceFilePath).toString(), dataset.size());
	    	}
		    LOGGER.log(Level.INFO, "Before:"+msBounds.toString());
		    
		    MsDataMap dataMap = new MsDataMap(dataset,this.originDensity);
	        switch(this.storeChoice){
            default:
            case bottomUp:
    	    	LOGGER.log(Level.INFO, dataMap.toString());
		    	if(MzMD.STORE_ORIGIN_DATA)
		    	{
			    	storeDataSet(dataset, dataMap.density);
		    	}
		    	storeMap(dataMap);
		    	densities.add(dataMap.density);
		    	while(dataMap.nextLevel())
		    	{
		    		dataMap = new MsDataMap(dataMap);
			    	LOGGER.log(Level.INFO, dataMap.toString());
			    	storeMap(dataMap);
			    	densities.add(dataMap.density);
		    	}
		    	msBounds.update(dataMap.mzMin, dataMap.mzMax, dataMap.rtMin, dataMap.rtMax);
		    	LOGGER.log(Level.INFO, "Total build "+(dataMap.mapLevel+1)+" maps.");
		    	break;
            case topDown:
            	List<MsDataMap> dataMaps = new ArrayList<MsDataMap>();
            	dataMaps.add(dataMap);
		    	densities.add(dataMap.density);
		    	while(dataMap.nextLevel())
		    	{
		    		dataMap = new MsDataMap(dataMap);
		    		dataMaps.add(dataMap);
			    	densities.add(dataMap.density);
		    	}
            	for(int i = dataMaps.size();i > 0; i--)
            	{
            		dataMap = dataMaps.get(i-1);
			    	storeMap(dataMap);
            	}
            	if(MzMD.STORE_ORIGIN_DATA)
		    	{
			    	storeDataSet(dataset, dataMap.density);
		    	}
		    	msBounds.update(dataMap.mzMin, dataMap.mzMax, dataMap.rtMin, dataMap.rtMax);
		    	LOGGER.log(Level.INFO, "Total build "+dataMaps.size()+" maps.");
		    	break;
            case jump:
            	LOGGER.log(Level.INFO, dataMap.toString());
		    	if(MzMD.STORE_ORIGIN_DATA)
		    	{
			    	storeDataSet(dataset, dataMap.density);
		    	}
		    	storeMap(dataMap);
		    	densities.add(dataMap.density);
		    	MsDataMap dataMapJump = new MsDataMap(dataset,this.originDensity,true);
		    	LOGGER.log(Level.INFO, dataMapJump.toString());
		    	storeMap(dataMapJump);
		    	densities.add(dataMapJump.density);
		    	while(dataMapJump.nextLevel())
		    	{
		    		MsDataMap dataMapTmp = new MsDataMap(dataMap,true);
			    	LOGGER.log(Level.INFO, dataMapTmp.toString());
			    	storeMap(dataMapTmp);
			    	densities.add(dataMapTmp.density);
			    	dataMap = dataMapJump;
			    	dataMapJump = dataMapTmp;
		    	}
		    	msBounds.update(dataMap.mzMin, dataMap.mzMax, dataMap.rtMin, dataMap.rtMax);
		    	LOGGER.log(Level.INFO, "Total build "+(dataMap.mapLevel+1)+" maps.");
	        }
	        

		    LOGGER.log(Level.INFO, "After:"+msBounds.toString());
		    LOGGER.log(Level.INFO, "After:"+densities.toString());
		    
		    StoreBaseInfomation(dataset.size());

	    	
	    }


		public void buildTreeFromRootWithPath(List<MsDataPoint> dataset, Path sourceFilePath) throws Exception
	    { 

	        LOGGER.log(Level.INFO, "Building MzMD from " + dataset.size() + " points");
	        this.initDataStorage(STORAGE_INTERFACE_CHOICE, sourceFilePath.toString(), dataset.size());

	        MsDataMap dataMap = new MsDataMap(dataset,this.originDensity);
	        switch(this.storeChoice){
            default:
            case bottomUp:
    	    	LOGGER.log(Level.INFO, dataMap.toString());
		    	if(MzMD.STORE_ORIGIN_DATA)
		    	{
			    	storeDataSet(dataset, dataMap.density);
		    	}
		    	storeMap(dataMap);
		    	densities.add(dataMap.density);
		    	while(dataMap.nextLevel())
		    	{
		    		dataMap = new MsDataMap(dataMap);
			    	LOGGER.log(Level.INFO, dataMap.toString());
			    	storeMap(dataMap);
			    	densities.add(dataMap.density);
		    	}
		    	msBounds.set(dataMap.mzMin, dataMap.mzMax, dataMap.rtMin, dataMap.rtMax);
		    	LOGGER.log(Level.INFO, "Total build "+(dataMap.mapLevel+1)+" maps.");
		    	break;
            case topDown:
            	List<MsDataMap> dataMaps = new ArrayList<MsDataMap>();
            	dataMaps.add(dataMap);
		    	densities.add(dataMap.density);
		    	while(dataMap.nextLevel())
		    	{
		    		dataMap = new MsDataMap(dataMap);
		    		dataMaps.add(dataMap);
			    	densities.add(dataMap.density);
		    	}
            	for(int i = dataMaps.size();i > 0; i--)
            	{
            		dataMap = dataMaps.get(i-1);
			    	LOGGER.log(Level.INFO, dataMap.toString());
			    	storeMap(dataMap);
            	}
            	if(MzMD.STORE_ORIGIN_DATA)
		    	{
			    	storeDataSet(dataset, dataMap.density);
		    	}
		    	msBounds.set(dataMap.mzMin, dataMap.mzMax, dataMap.rtMin, dataMap.rtMax);
		    	LOGGER.log(Level.INFO, "Total build "+(dataMap.mapLevel+1)+" maps.");
	        }

	    	
	    }
		

	    private void StoreBaseInfomation(int count) {
			// TODO Auto-generated method stub
	    	try{
	    		msBounds.setCount(count);
                this.dataStorage.saveBaseInfomation(msBounds);
            }
            catch(Exception e)
            {
                LOGGER.log(Level.WARNING, "Could not save base infomation: " + count + "|" + msBounds.toString(), e);
            }
	        try {
	            // commit all entries
	            this.dataStorage.flush();
	        } catch (Exception ex) {
	            LOGGER.log(Level.WARNING, "Could not persist data to storage", ex);
	        }
		}

	    
	    private void storeMap(MsDataMap dataMap)
	    {
//	        long start = System.currentTimeMillis();
	    	try{
                this.dataStorage.saveMap(dataMap);
            }
            catch(Exception e)
            {
                LOGGER.log(Level.WARNING, "Could not save points to datastorage for map level: " + dataMap.mapLevel, e);
            }
	        try {
	            // commit all entries
	            this.dataStorage.flush();
	        } catch (Exception ex) {
	            LOGGER.log(Level.WARNING, "Could not persist data to storage", ex);
	        }
//            LOGGER.log(Level.INFO, "Level "+dataMap.mapLevel+" MzMD load time: " + (System.currentTimeMillis() - start));
	    }
	    private void storeDataSet(List<MsDataPoint> dataset, double density)
	    {
	        long start = System.currentTimeMillis();
	    	try{
                this.dataStorage.saveDataSet(dataset, density);
            }
            catch(Exception e)
            {
                LOGGER.log(Level.WARNING, "Could not save points to datastorage for map level: " + 0, e);
            }
	        try {
	            // commit all entries
	            this.dataStorage.flush();
	        } catch (Exception ex) {
	            LOGGER.log(Level.WARNING, "Could not persist data to storage", ex);
	        }
            LOGGER.log(Level.INFO, "Level 0 MzMD load time: " + (System.currentTimeMillis() - start));
	    }
	    

	    /**
	     * Upon the user selecting a memory-conservative load, configures the tree for
	     * a partitioned load according to available memory.
	     *      - A minimum branching factor must be reached by the partitioned configuration,
	     *        a branching factor smaller than the minimum creates a very tall tree. 
	     *        Increasing the branching factor decreases partition size, 
	     *        thus not endangering memory consumption. 
	     *      - If the partitioned configuration returns a branching factor of 1 
	     *        then the entire dataset will fit into memory. Revert to default configuration.
	     * @param mzmlParser input mzml file parser
	     * @return false if partitioned load is unnecessary (entire dataset will fit in RAM), otherwise true 
	     * @throws XMLStreamException
	     * @throws FileNotFoundException
	     * @throws IOException
	     * @throws DataFormatException 
	     */
	    private boolean partitionedLoadConfiguration(MzmlParser mzmlParser, Path sourceFilePath) throws Exception
	    {
	        
	        this.importState.setImportStatus(ImportStatus.PARSING);
	        
	        if(!Configure.isPartitionedLoad)
	        {
	        	return false;
	        }

	        // number of available bytes in java heap
	        long numBytesInHeap = (long) (Runtime.getRuntime().maxMemory());
	        
	        LOGGER.log(Level.INFO, "numBytesInHeap " + numBytesInHeap);
	        
	        // count the number of points in the mzML file
	        int numPoints = mzmlParser.countPoints();

	        this.originDensity = numPoints/mzmlParser.msBounds.area();

			LOGGER.log(Level.INFO, "Point number:"+numPoints);
			LOGGER.log(Level.INFO, "Bounds:"+mzmlParser.msBounds.toString());

	           
            double partitionStep = (mzmlParser.msBounds.get()[3]-mzmlParser.msBounds.get()[2]) / (double)Configure.partition;
            
            // prepare parser for partitioned read
            // recalculate partition size
            mzmlParser.initPartitionedReadbyRT(partitionStep);
            
            this.initDataStorage(STORAGE_INTERFACE_CHOICE, getConvertDestinationPath(sourceFilePath).toString(), numPoints);
            
            // inform importState of the amount of work to do
            this.importState.setTotalWork(numPoints);
            
            return true;
	    }

	    /**
	     * Performs a partitioned load of the data set, resulting in conservative memory consumption
	     * @param mzmlParser input mzml file parser
	     * @throws Exception 
	     */
	    private void partitionedLoad(MzmlParser mzmlParser) throws Exception
	    {

	        LOGGER.log(Level.INFO, "Partitioned load " + Configure.partition + " partitions");
	        
	        // signal to the import monitor that tree building has begun
	        this.importState.setImportStatus(ImportStatus.CONVERTING);
	        
	        
	        // iterate through each level 1 node, loading partition and
	        // constructing separately
	        for(int i = 0; i < Configure.partition; i++)
	        {            
	            if(i>0)
	            {
	            	this.dataStorage.setPartition();
	            }
	            // load level 1 node's partition
	            List<MsDataPoint> curPartition = mzmlParser.readPartitionbyRT();
	            
	            this.buildTreeFromRoot(curPartition, null);
	            
				LOGGER.log(Level.INFO, "Completed partition " + i + "," + curPartition.size() + "points");
	        }
	    }
	    public ImportState getImportState() {
	        return importState;
	    }

	    public ImportState.ImportStatus getLoadStatus() {
	        return importState.getImportStatus();
	    }

	    public String getLoadStatusString() {
	        return importState.getStatusString();
	    }

	    /**
	     * inits the data storage module
	     * @param storageChoice Storage interface selection
	     * @param filePath (optional) location to create storage file
	     * @param numPoints (Hybrid only) number of points that will be saved in file
	     * @throws Exception 
	     */
	    private void initDataStorage(StorageFacadeFactory.Facades storageChoice, String filePath, Integer numPoints) throws Exception
	    {
	    	System.out.println(filePath);
	        // init data storage module
	        this.dataStorage = StorageFacadeFactory.create(storageChoice);
	        this.dataStorage.init(filePath, numPoints);

	        this.importState.setMzTreeFilePath(this.dataStorage.getFilePath());
	    }
	    
	    //***********************************************//
	    //                    QUERY                      //
	    //***********************************************//
	    
	    /**
	     * Queries the MzTree for points contained with the mz, rt bounds
	     *
	     * @param mzMin query mz lower bound
	     * @param mzMax query mz upper bound
	     * @param rtMin query rt lower bound
	     * @param rtMax query rt upper bound
	     * @param numPoints number of points to be returned; 0 to return all points possible from the leaf depth and not use the cache
	     * @return 2-dimensional double array
	     */
	    public List<MsDataPoint> query(double mzMin, double mzMax,
	                                   float rtMin, float rtMax, int RTnumber, int MZnumber)
	    {
	    	int numPoints = RTnumber * MZnumber;
	        boolean sumData = (numPoints > 0);

	        int mapLevel = 0;
	        if(!MzMD.STORE_ORIGIN_DATA)
	        {
	        	mapLevel = 1;
	        }
	        if(sumData)
	        {
	        	double density = numPoints/((mzMax-mzMin)*(rtMax-rtMin));
//	        	System.out.println(density);
//	        	System.out.println(densities);
	        	for(int i = 1; i < this.densities.size(); i++)
	        	{
	        		if(density > densities.get(i))
	        		{
	        			mapLevel = i+1;
	        			break;
	        		}
	        	}
	        	if(density <= densities.get(densities.size()-1))
	        	{
	        		mapLevel = this.densities.size();
	        	}
	        }
//	        System.out.println(this.densities.size()+":"+(mapLevel-1));
//	        if(mapLevel>0)
//	        	System.out.println("MapLevel:"+mapLevel+", max num:"+this.densities.get(mapLevel-1)*(mzMax-mzMin)*(rtMax-rtMin));
	        
	        try {
                // use the leaf-node optimized query
                return this.getBestSum(this.dataStorage.loadPointsInBounds(mzMin, mzMax, rtMin, rtMax, mapLevel, numPoints), 
                		RTnumber, MZnumber, mzMin, mzMax, Double.parseDouble(String.valueOf(rtMin)) , Double.parseDouble(String.valueOf(rtMax)));
            } catch(Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load points from the database", e);
                return null;
            }
	        
	    }
	    public double getQueryDensity(double mzMin, double mzMax,
                float rtMin, float rtMax, int numPoints)
		{
			boolean sumData = (numPoints > 0);
			
			int mapLevel = 0;
			if(!MzMD.STORE_ORIGIN_DATA)
			{
				mapLevel = 1;
			}
			if(sumData)
			{
				double density = numPoints/((mzMax-mzMin)*(rtMax-rtMin));
//				System.out.println(density);
//				System.out.println(densities);
				for(int i = 1; i < this.densities.size(); i++)
				{
					if(density > densities.get(i))
					{
						mapLevel = i+1;
						break;
					}
				}
				if(density <= densities.get(densities.size()-1))
				{
				mapLevel = this.densities.size();
				}
			}
			return this.densities.get(mapLevel-1)*(mzMax-mzMin)*(rtMax-rtMin);

	    }
	    

		public List<MsDataPoint> getBestSum(List<MsDataPoint> points, int RTnumber, int MZnumber, double mzMin, double mzMax, double rtMin, double rtMax)
		{
			if(RTnumber == 0 || MZnumber == 0)
				return points;
			double mzStep = (mzMax - mzMin)/MZnumber;
			double rtStep = (rtMax - rtMin)/RTnumber;
			int[][] bestPoints = new int[MZnumber][RTnumber];
			for(int i = 0; i < MZnumber; i++)
			{
				for(int j = 0; j < RTnumber; j++)
				{
					bestPoints[i][j] = -1;
				}
			}
			for(int i = 0; i < points.size(); i++)
			{
				MsDataPoint point = points.get(i);
				int mzIndex = (int) Math.floor((point.mz - mzMin)/mzStep);
				if(mzIndex == MZnumber)
				{
					mzIndex--;
				}
				int rtIndex = (int) Math.floor((point.rt - rtMin)/rtStep);
				if(rtIndex == RTnumber)
				{
					rtIndex--;
				}
				if(bestPoints[mzIndex][rtIndex] == -1)
				{
					bestPoints[mzIndex][rtIndex] = i;
				}else
				{
					if(point.intensity > points.get(bestPoints[mzIndex][rtIndex]).intensity)
					{
						bestPoints[mzIndex][rtIndex] = i;
					}
				}
			}
			List<MsDataPoint> bestSumPoints = new ArrayList<>();
			for(int i = 0; i < MZnumber; i++)
			{
				for(int j = 0; j < RTnumber; j++)
				{
					if(bestPoints[i][j] > -1)
					{
						bestSumPoints.add(points.get(bestPoints[i][j]));
					}
				}
			}
			return bestSumPoints;
		}

	    
	    
	    //***********************************************//
	    //                    CSV EXPORT                 //
	    //***********************************************//

	    public void saveAs(Path targetFilepath) throws Exception {
	        // source file path
	        Path sourceFilepath = Paths.get(this.dataStorage.getFilePath());

	        try{
	            // close current storage connection
	            this.dataStorage.close();

	            // copy current output location to new output location
	            this.dataStorage.copy(targetFilepath);

	            // init connection to new database
	            this.dataStorage.init(targetFilepath.toString(), null);
	        }
	        catch(Exception e){
	            LOGGER.log(Level.WARNING, "Could not create copy at " + targetFilepath.toString(), e);
	            try{
	                // revert back to previous connection
	                this.dataStorage.init(sourceFilepath.toString(), null);
	            }
	            catch(Exception ex){
	                LOGGER.log(Level.WARNING, "After failed copy, could not revert back to " + sourceFilepath.toString(), ex);
	            }
	            throw e;
	        }
	    }

	    /**
	     * Exports the given data range into a csv at filepath
	     * @param filepath out location
	     * @param minMZ lower mz bound
	     * @param maxMZ upper mz bound
	     * @param minRT lower rt bound
	     * @param maxRT upper rt bound
	     * @throws java.io.IOException
	     */
	    public int export(String filepath, double minMZ, double maxMZ, float minRT, float maxRT, int m, int n) throws IOException
	    {
	        //append csv extension if not already there
	        if(!filepath.endsWith(".csv"))
	            filepath = filepath + ".csv";
	        
	        try ( CSVWriter writer = new CSVWriter(new FileWriter(filepath)) ) 
	        {
	            writer.writeNext(new String[] {"m/z","RT","intensity","meta1"});
	            
	            // get the points of the data range
	            // THIS IS WHERE THE OPTIMIZATION PROBLEM STARTS
	            // currently loads all pertinent points into memory (could be the whole file)
	            List<MsDataPoint> points = this.query(minMZ, maxMZ, minRT, maxRT, m, n);
	            
	            // write away!
	            for (MsDataPoint p : points)
	                writer.writeNext( new String[] {Double.toString(p.mz), Float.toString(p.rt), Double.toString(p.intensity), Integer.toString(p.meta1) });
	            

	            return points.size();
	        }
	    }
	    
	    /**
	     * If import status is ready, returns mz x rt bounds in order: mzmin, mzmax, rtmin, rtmax
	     * Else returns null
	     * @return mz x rt bounds (mzmin, mzmax, rtmin, rtmax) or null
	     */
	    public double[] getDataBounds()
	    {
	        // if not ready, then cannot access data bounds
	        if(importState.getImportStatus() != ImportStatus.READY)
	            return null;
	        
	        else
	            return msBounds.get();
	    }
	    
	    //***********************************************//
	    //                   HELPERS                     //
	    //***********************************************//


	    /**
	     * Converts a csv row MsDataPoint to MsDataPoint object
	     * @param line
	     * @return
	     */
	    private MsDataPoint csvRowToMsDataPoint(String[] line)
	    {
	        double mz = Double.parseDouble(line[0]);
	        float rt = Float.parseFloat(line[1]);
	        double intensity = Double.parseDouble(line[2]);
	        int meta1 = Integer.parseInt(line[3]);
	        MsDataPoint point = new MsDataPoint(0, mz, rt, intensity);
	        point.meta1 = meta1;
	        return point;
	    }
	    
	    public void close() 
	    {
	        if (this.dataStorage != null) {
	            this.dataStorage.close();
	            dataStorage = null;
	        }
	    }

		public Path getOutPath() {
			// TODO Auto-generated method stub
			return this.outPath;
		}
	}



	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	