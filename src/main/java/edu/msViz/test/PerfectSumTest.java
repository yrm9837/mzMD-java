package edu.msViz.test;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

import javax.xml.stream.XMLStreamException;

import edu.msViz.base.MzmlReader;
import edu.msViz.mzMD.Configure;
import edu.msViz.mzMD.MsDataPoint;

public class PerfectSumTest {
	private static final Logger LOGGER = Logger.getLogger(PerfectSumTest.class.getName());
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("**************** Perfect Summarization Test ****************");

		String openFilePath = "/Users/yangrunmin/Study/data/CPTAC_Intact_rep8_15Jan15_Bane_C2-14-08-02RZ.mzML";

	    TestInfomation testInfo = new TestInfomation(openFilePath+"-PerfectSumTest");
	    
	    Simulation importSim = new Simulation(openFilePath+".csv");
	    List<double[]> importBounds = importSim.loadCSV();
        Quantification quant = new Quantification();
	    int sumNumber = 10000;
        try 
        {
            
            // mzml load

            // initialize MzmlReader
            MzmlReader mzmlReader = new MzmlReader(openFilePath);
            for(int i = 0; i < importBounds.size(); i++)
		    {
	//	    	System.out.println(importBounds.get(i)[0]+",\t"+importBounds.get(i)[1]+",\t"+importBounds.get(i)[2]+",\t"+importBounds.get(i)[3]);
		        long start = System.currentTimeMillis();
		        mzmlReader.setBounds(importBounds.get(i));
		        List<MsDataPoint> points = mzmlReader.getPoints();
		        long queryTime = System.currentTimeMillis() - start;
		        start = System.currentTimeMillis();
		        List<MsDataPoint> sumPoints = quant.getBestSum(points, sumNumber, importBounds.get(i));
		        long sumTime = System.currentTimeMillis() - start;
		        testInfo.addInfo(importBounds.get(i)[0],importBounds.get(i)[1],importBounds.get(i)[2],importBounds.get(i)[3], points.size(), 
		        		queryTime, sumPoints.size(), sumTime, quant.coverage, quant.efficiency);
	//	    	tm.queryData(importBounds.get(i)[0],importBounds.get(i)[1],importBounds.get(i)[2],importBounds.get(i)[3],0);
		        double per = (double)(i+1)*20/(double)importBounds.size();
		        if(testMain.isInteger(per))
		        {
		        	System.out.println("Querying "+((i+1)*100/importBounds.size())+"%");
		        }
		    }
		    testInfo.export(0);
            
        } 
        catch (DataFormatException | XMLStreamException | IOException ex) 
        {
        	LOGGER.log(Level.WARNING, "MD Build Error!", ex);
        }
	}

}
