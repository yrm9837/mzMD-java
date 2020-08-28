package edu.msViz.test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVWriter;

import edu.msViz.mzMD.Configure;
import edu.msViz.mzMD.MsDataMap;

public class TestInfomation {
	//mzMin,mzMax,rtMin,rtMax,pointNumber,queryTime,sumNumber,sumTime,density,densNumber
	private List<String[]> infoStrs;

	private String infoFilePath;

	private String originFilePath;
	
	public TestInfomation(String filePath)
	{
		this.originFilePath = filePath;
		infoStrs = new ArrayList<String[]>();
		infoStrs.add(new String[]{"mzMin","mzMax","rtMin","rtMax","pointNumber",
		 "queryTime","resultNumber","sumTime","coverage","efficiency"});
	}
	
	public void addInfo(double mzMin, double mzMax, double rtMin, double rtMax,
			int pointNumber, double queryTime, int resultNumber, 
			double sumTime, double coverage, double efficiency)
	{
		infoStrs.add(new String[]{Double.toString(mzMin),Double.toString(mzMax),Double.toString(rtMin),
				Double.toString(rtMax),Integer.toString(pointNumber),Double.toString(queryTime),
				Integer.toString(resultNumber),Double.toString(sumTime),
				Double.toString(coverage),Double.toString(efficiency)});
	}
	
	public void export(long spend)
    {
//		String suggestedFileName = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss").format(new java.util.Date());
		this.infoFilePath = this.originFilePath + "-" + spend + 
				"-" + Configure.storeChoice.toString() + "-" + Configure.sqlChoice.toString() + "-"+
				Configure.sqlModeChoice.toString() + "-" + MsDataMap.SCALE_EACH_LEVEL + ".csv";
        try ( CSVWriter writer = new CSVWriter(new FileWriter(infoFilePath)) ) 
        {
            // write away!
            for (String[] infoStr : infoStrs)
                writer.writeNext( infoStr);
            
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
            return;
    }

}
