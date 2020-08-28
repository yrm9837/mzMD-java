package edu.msViz.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.msViz.base.ImportState;
import edu.msViz.base.MzmlParaerSum;
import edu.msViz.mzMD.Configure;
import edu.msViz.mzMD.MsDataPoint;
import edu.msViz.mzMD.MzMD;

public class testMain {
	private static final Logger LOGGER = Logger.getLogger(testMain.class.getName());
	// currently loaded MzTree instance
//    public MzTree mzTree;
    public MzMD mzMD;
    
    
    public testMain()
    {
    }
    
    public long openFile(String filePath) {
        System.out.println("Open file.");
        System.out.println(filePath);

        // disconnect and drop the previous mzTree
        closeMzTree();

        this.mzMD = new MzMD();

        // process mzTree on new thread so that UI thread remains responsive
        return loadMzTree(filePath);
    }
    
    private long loadMzTree(String filePath) {
        long start = System.currentTimeMillis();
        long spend = 0;
        try
        {
            // attempt to create mzMD
            this.mzMD.load(filePath);
//        	System.out.println("this.mzMD.outPath:"+this.mzMD.getOutPath());
            spend = (System.currentTimeMillis() - start);
            LOGGER.log(Level.INFO, "mzMD load time: " + spend);

//            SwingUtilities.invokeLater(this::updateFileState);
        }
        catch (Exception ex)
        {
            this.mzMD = null;

            LOGGER.log(Level.WARNING, "Could not open requested file", ex);
        }
        return spend;
    }

    private void closeMzTree() {
        if (this.mzMD != null) {
        	this.mzMD.close();
        	this.mzMD = null;
        }
        updateFileState();
    }
    
    private void updateFileState() {
        if (this.mzMD == null || this.mzMD.getLoadStatus() != ImportState.ImportStatus.READY) {
        } else {
            String resultFilePath = this.mzMD.dataStorage.getFilePath();
        }

    }
    
    private double[] allData()
    {
//    	printInfo();
        double[] bounds = this.mzMD.getDataBounds();
        if(bounds != null)
        {
//        	printInfo();
            // rounded to three decimal places
            // to be inclusive, floor on mins, ceil on maxes
        	double minMzRounded = (double)Math.floor(bounds[0] * 1000) / 1000f;
        	double maxMzRounded = (double)Math.ceil(bounds[1] * 1000) / 1000f;
        	double minRtRounded = (double)Math.floor(bounds[2] * 1000) / 1000f;
        	double maxRtRounded = (double)Math.ceil(bounds[3] * 1000) / 1000f;
            
            System.out.println("minMZ:"+minMzRounded+", maxMZ:"+maxMzRounded+", minRT:"+minRtRounded+", maxRT:"+maxRtRounded);
            return new double[] {minMzRounded,maxMzRounded,minRtRounded,maxRtRounded};
        }
        return new double[] {0,0,0,0};
    }

    private void exportQueryData(double d_minMZ, double d_maxMZ, double d_minRT, double d_maxRT, String outName) {
        long start = System.currentTimeMillis();
        System.out.println("this.mzMD.outPath:"+this.mzMD.getOutPath());
    	String filepath = this.mzMD.getOutPath().resolve(outName+"-"+String.format("%.0f",d_minMZ)+"_"+String.format("%.0f",d_maxMZ)+"_"+String.format("%.3f",d_minRT)+"_"+String.format("%.3f",d_maxRT)+".csv").toString();
    	System.out.println(filepath);
        try {
            int exported = this.mzMD.export(filepath, d_minMZ, d_maxMZ, (float) d_minRT, (float) d_maxRT,0,0);
            LOGGER.log(Level.INFO, "Exported " + exported + " points to CSV.");
            LOGGER.log(Level.INFO, "Export time: " + (System.currentTimeMillis() - start));

        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error when exporting CSV file", ex);
        }
    }
    private void queryData(double d_minMZ, double d_maxMZ, double d_minRT, double d_maxRT, int numPoints) {
        long start = System.currentTimeMillis();
        List<MsDataPoint> points = this.mzMD.query(d_minMZ, d_maxMZ, (float) d_minRT, (float) d_maxRT, (int) Math.floor(Math.sqrt(numPoints)), (int) Math.floor(Math.sqrt(numPoints)));
        LOGGER.log(Level.INFO, "Query " + numPoints + "/" + points.size() + " points to CSV.");
        LOGGER.log(Level.INFO, "Query time: " + (System.currentTimeMillis() - start));
    }

    
	private double[] getQueryArea(double[] allArea, double percent) {
		// TODO Auto-generated method stub
		double mz = allArea[1] - allArea[0];
		double rt = allArea[3] - allArea[2];
		double mzStart = allArea[0] + mz*(1-percent)*Math.random();
		double rtStart = allArea[2] + rt*(1-percent)*Math.random();
		return new double[] {mzStart,mzStart+mz*percent,rtStart,rtStart+rt*percent};
	}
  
	public static boolean isInteger(double d)
	{
		double eps = 1e-10;  // 精度范围
		if(d - (double)((int)d) < eps)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
    
	public static void main(String[] args) {
		boolean step1 = false;
		boolean step2 = false;
		boolean step3 = false;
		boolean step4 = false;
		boolean step5 = false;
		boolean step6 = false;
		boolean step7 = false;
		
	    // **************** STEP 1: OPEN FILE **************** 打开文件、加载MzTree或者MzMD
		step1 = true;
	    // **************** STEP 2: ALL DATA **************** 获取mz、rt范围
		step2 = true;
		// **************** STEP 3: QUERY DATA **************** 查询
//		step3 = true;
	    // **************** STEP 4: SIMULATE QUERY AREA **************** 模拟生成查询范围
//		step4 = true;
	    // **************** STEP 5: MULTY QUERY DATA **************** 多次查询并输出查询时间与结果数，与每次计算的完美摘要相比
//		step5 = true;
	    // **************** STEP 6: COMPUTE COVERAGE AND EFFICIENCY **************** 多次查询并输出查询时间与结果数 mztree
//		step6 = true;
	    // **************** STEP 7: MULTY QUERY DATA COMPARE WITH SUM FILE **************** 多次查询并输出查询时间与结果数,与完美摘要文件相比
		step7 = true;
		
		// TODO Auto-generated method stub
	    System.out.println("Test the mzTree here!");
	    //这里选择是用MzTree还是MzMD
	    testMain tm = new testMain();
	    String openFilePath = null;
	    long spend = 0;
	
	//        System.out.println("1tm.mzMD.outPath:"+tm.mzMD.getOutPath());
		    // Open a file, and convert it to mzTree.
//		    String openFilePath = "F:\\project\\example_data\\small.pwiz.1.1.mzML";
//		    openFilePath = "/Users/yangrunmin/eclipse-workspace/mzMD/example_data/small.pwiz.1.1.mzML";
//		    openFilePath = "/Users/yangrunmin/Study/data/CPTAC_Intact_rep8_15Jan15_Bane_C2-14-08-02RZ.mzML";
		    openFilePath = "/Users/yangrunmin/Study/data/ms12/CPTAC_Intact_rep8_15Jan15_Bane_C2-14-08-02RZ-nopick.mzML";
	//	    String openFilePath = "E:\\Study\\data\\mzML\\CPTAC_Intact_rep8_15Jan15_Bane_C2-14-08-02RZ.mzML";//378M
	//	    String openFilePath = "E:\\Study\\data\\mzML\\2006_08_16_ALS_C4_40_lipo_not_digested_30000.mzML";//134M
		    // **************** STEP 1: OPEN FILE **************** 打开文件、加载MzTree或者MzMD
		if(step1)
		{
		    System.out.println("**************** STEP 1: OPEN FILE ****************");
		    spend = tm.openFile(openFilePath);
		}
		    
		double[] aA = null;
		    // **************** STEP 2: ALL DATA **************** 获取mz、rt范围
		if(step2)
		{
		    System.out.println("**************** STEP 2: ALL DATA ****************");
		    aA = tm.allData();
		}
	
		    // **************** STEP 3: QUERY DATA **************** 查询
		if(step3)
		{
		    System.out.println("**************** STEP 3: QUERY DATA ****************");
		    tm.queryData(1247.58400, 1566.274979, 0.197109469, 0.325795634, 0);
		    tm.exportQueryData(aA[0],aA[1],aA[2],aA[3],"out1");
		    
		    for(int i = 1; i < 10; i++) {
			    double[] qA = tm.getQueryArea(aA,0.2);
			    tm.queryData(qA[0],qA[1],qA[2],qA[3],5000);
	//		    tm.exportQueryData(qA[0],qA[1],qA[2],qA[3],"out"+i);
		    };
		}
	
		    // **************** STEP 4: SIMULATE QUERY AREA **************** 模拟生成查询范围并生成完美摘要
		if(step4)
		{
		    System.out.println("**************** STEP 4: SIMULATE QUERY AREA ****************");
		    Simulation exportSim = new Simulation(openFilePath);
		    exportSim.loadXML();
		    List<double[]> allBounds = new ArrayList<double[]>();
		    int simCount = 1000;
		    int m = 100;
		    int n = 100;
		    MzmlParaerSum MzmlParaerSum = new MzmlParaerSum(openFilePath);
		    int lastPer = -1;
		    for(int i = 0; i < simCount; i++)
		    {
		    	double[] simBound = exportSim.randomSim(1, 100);
//		    	System.out.println("Sim " + i + " = {" + simBound[0] + "," + simBound[1] + "," + simBound[2] + "," + simBound[3] + "}" );
		    	List<MsDataPoint> bestPoints = MzmlParaerSum.getBestSum(simBound,m,n);
		    	int pointNumber = MzmlParaerSum.numPoints;
		    	exportSim.exportSum(i, bestPoints);
		    	double[] oneBound = {simBound[0],simBound[1],simBound[2],simBound[3],pointNumber};
		    	allBounds.add(oneBound);
		    	int per = (i+1)*20/simCount;
		        if(lastPer!=per)
		        {
		        	lastPer = per;
		        	System.out.println("Querying "+((i+1)*100/simCount)+"%");
		        }
		    }
		    exportSim.export(allBounds);
		    System.out.println("Success export " + simCount + " simulate bounds with path:"+openFilePath+".csv");
		    System.out.println("Success export " + simCount + " bestSum with path:"+openFilePath+".sum");
		}
		    
		    // **************** STEP 5: MULTY QUERY DATA **************** 多次查询并输出查询时间与结果数
		if(step5)
		{
		    System.out.println("**************** STEP 5: MULTY QUERY DATA ****************");
		    TestInfomation testInfo = new TestInfomation(openFilePath);
		    Simulation importSim = new Simulation(openFilePath+".csv");
	//	    System.out.println("mzMin,\tmzMax,\trtMin,\trtMax");
		    List<double[]> importBounds = importSim.loadCSV();
		    int sumNumber = 10000;
		    int lastPer = -1;
		    Quantification quant = new Quantification();
		    for(int i = 0; i < importBounds.size(); i++)
		    {
	//	    	System.out.println(importBounds.get(i)[0]+",\t"+importBounds.get(i)[1]+",\t"+importBounds.get(i)[2]+",\t"+importBounds.get(i)[3]);
		        long start = System.currentTimeMillis();
		        List<MsDataPoint> points = tm.mzMD.query(importBounds.get(i)[0],importBounds.get(i)[1], (float)importBounds.get(i)[2], (float)importBounds.get(i)[3], 0, 0);
		        long queryTime = System.currentTimeMillis() - start;
		        start = System.currentTimeMillis();
		        
		        List<MsDataPoint> sumPoints = tm.mzMD.query(importBounds.get(i)[0],importBounds.get(i)[1], (float)importBounds.get(i)[2], (float)importBounds.get(i)[3], (int) Math.floor(Math.sqrt(sumNumber)), (int) Math.floor(Math.sqrt(sumNumber)));
		        
		        sumPoints = quant.getBestSum(sumPoints, (int) Math.floor(Math.sqrt(sumNumber)), (int) Math.floor(Math.sqrt(sumNumber)), importBounds.get(i));
		        
		        long sumTime = System.currentTimeMillis() - start;
//		        double density = tm.mzMD.getQueryDensity(importBounds.get(i)[0],importBounds.get(i)[1], (float)importBounds.get(i)[2], (float)importBounds.get(i)[3], sumNumber);
//		        int densNumber = (int) (density*(importBounds.get(i)[1]-importBounds.get(i)[0])*(importBounds.get(i)[3]-importBounds.get(i)[2]));
		        quant.compare(points, sumPoints, sumNumber, importBounds.get(i));
		        testInfo.addInfo(importBounds.get(i)[0],importBounds.get(i)[1],importBounds.get(i)[2],importBounds.get(i)[3], points.size(), 
		        		queryTime, sumPoints.size(), sumTime, quant.coverage, quant.efficiency);
	//	    	tm.queryData(importBounds.get(i)[0],importBounds.get(i)[1],importBounds.get(i)[2],importBounds.get(i)[3],0);
		        int per = (i+1)*20/importBounds.size();
		        if(lastPer!=per)
		        {
		        	lastPer = per;
		        	System.out.println("Querying "+((i+1)*100/importBounds.size())+"%");
		        }
		    }
		    testInfo.export(spend);
		}
		    
		    // **************** STEP 6: COMPUTE COVERAGE AND EFFICIENCY **************** 多次查询并输出查询时间与结果数
		if(step6)
		{
		    System.out.println("**************** STEP 6: COMPUTE COVERAGE AND EFFICIENCY ****************");
		    Simulation importSim = new Simulation(openFilePath+".csv");
	//	    System.out.println("mzMin,\tmzMax,\trtMin,\trtMax");
		    List<double[]> importBounds = importSim.loadCSV();
		    int sumNumber = 10000;
		    int lowNumber = 5 * sumNumber;
		    int cmpCount = 0;
	
		    Quantification quant = new Quantification();
		    List<double[]> coverages = new ArrayList<double[]>();
		    List<double[]> efficiencys = new ArrayList<double[]>();
		    for(int i = 0; i < importBounds.size(); i++)
		    {
		    	double[] tempCoverages = new double[25];
		    	double[] tempEfficiencys = new double[25];
		    	
		        List<MsDataPoint> points = tm.mzMD.query(importBounds.get(i)[0],importBounds.get(i)[1], (float)importBounds.get(i)[2], (float)importBounds.get(i)[3], 0, 0);
		        if(points.size() > lowNumber)
		        {
				    for(int l = 1; l < 26; l++)
				    {
				    	int resultNumber = l * 1000;
				        List<MsDataPoint> sumPoints = tm.mzMD.query(importBounds.get(i)[0],importBounds.get(i)[1], (float)importBounds.get(i)[2], (float)importBounds.get(i)[3], (int) Math.floor(Math.sqrt(resultNumber)), (int) Math.floor(Math.sqrt(resultNumber)));
		
				        quant.compare(points, sumPoints, sumNumber, importBounds.get(i));
				        tempCoverages[l-1] = quant.coverage;
				        tempEfficiencys[l-1] = quant.efficiency;
				    }
				    coverages.add(tempCoverages);
				    efficiencys.add(tempEfficiencys);
				    cmpCount++;
		        }
		        double per = (double)(i+1)*20/(double)importBounds.size();
		        if(testMain.isInteger(per))
		        {
		        	System.out.println("Querying "+((i+1)*100/importBounds.size())+"%");
		        }
		    }
		    System.out.println("Computing average.");
	    	double[] allCoverages = new double[25];
	    	double[] allEfficiencys = new double[25];
		    for(int i = 0; i < 25; i++)
		    {
		    	final int nowI = i;
//		    	coverages.sort(new Comparator<double[]>(){
//		    		public int compare(double[] arg0, double[] arg1) {
//		    			return (int) (arg1[nowI] - arg0[nowI]);}
//		    	});
//		    	efficiencys.sort(new Comparator<double[]>(){
//		    		public int compare(double[] arg0, double[] arg1) {
//		    			return (int) (arg1[nowI] - arg0[nowI]);}
//		    	});
		    	allCoverages[i] = 0;
		    	allEfficiencys[i] = 0;
//		    	int delete = importBounds.size()/10;
		    	int delete = 0;
		    	for(int j = delete; j < coverages.size()-delete; j++)
		    	{
		    		allCoverages[i] = allCoverages[i] + coverages.get(j)[i];
		    		allEfficiencys[i] = allEfficiencys[i] + efficiencys.get(j)[i];
		    	}
		    }
	    	double[] avgCoverages = new double[25];
	    	double[] aveEfficiencys = new double[25];
	    	System.out.println("Count\tCoverage\tEfficiency");
	    	for(int i = 0; i < 25; i++)
		    {
		    	avgCoverages[i] = allCoverages[i]/cmpCount;
		    	aveEfficiencys[i] = allEfficiencys[i]/cmpCount;
		    	System.out.println((i+1)*1000+"\t"+avgCoverages[i]+"\t"+aveEfficiencys[i]);
		    }
		}

	    // **************** STEP 7: MULTY QUERY DATA COMPARE WITH SUM FILE **************** 多次查询并输出查询时间与结果数,与完美摘要文件相比
	if(step7)
	{
	    System.out.println("**************** STEP 7: MULTY QUERY DATA COMPARE WITH SUM FILE **************** 多次查询并输出查询时间与结果数,与完美摘要文件相比");
	    TestInfomation testInfo = new TestInfomation(openFilePath);
	    Simulation importSim = new Simulation(openFilePath+".csv");
	    List<double[]> importBounds = importSim.loadCSV();
	    int sumNumber = 10000;
	    int treeNumber = 9000;
	    treeNumber = sumNumber;
	    int lastPer = -1;
	    Quantification quant = new Quantification();
	    for(int i = 0; i < importBounds.size(); i++)
	    {
	        long start = System.currentTimeMillis();
//	        List<MsDataPoint> points = tm.mzMD.query(importBounds.get(i)[0],importBounds.get(i)[1], (float)importBounds.get(i)[2], (float)importBounds.get(i)[3], 0);
//	        int pointNumber = points.size();
	        int pointNumber = (int) importBounds.get(i)[4];
	        long queryTime = System.currentTimeMillis() - start;
	        start = System.currentTimeMillis();
	        List<MsDataPoint> sumPoints = tm.mzMD.query(importBounds.get(i)[0],importBounds.get(i)[1], (float)importBounds.get(i)[2], (float)importBounds.get(i)[3], (int) Math.floor(Math.sqrt(sumNumber)), (int) Math.floor(Math.sqrt(sumNumber)));
	        sumPoints = quant.getBestSum(sumPoints, (int) Math.floor(Math.sqrt(sumNumber)), (int) Math.floor(Math.sqrt(sumNumber)), importBounds.get(i));
	        long sumTime = System.currentTimeMillis() - start;
	        quant.compare(importSim.csvSumLoad(openFilePath, i), sumPoints, sumNumber, importBounds.get(i));
	        testInfo.addInfo(importBounds.get(i)[0],importBounds.get(i)[1],importBounds.get(i)[2],importBounds.get(i)[3], pointNumber, 
	        		queryTime, sumPoints.size(), sumTime, quant.coverage, quant.efficiency);
//	    	tm.queryData(importBounds.get(i)[0],importBounds.get(i)[1],importBounds.get(i)[2],importBounds.get(i)[3],0);
	        int per = (i+1)*20/importBounds.size();
	        if(lastPer!=per)
	        {
	        	lastPer = per;
	        	System.out.println("Querying "+((i+1)*100/importBounds.size())+"%");
	        }
	    }
	    testInfo.export(spend);
	}
		
		
	}

}
