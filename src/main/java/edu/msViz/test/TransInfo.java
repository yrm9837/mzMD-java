package edu.msViz.test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVWriter;

import edu.msViz.mzMD.Configure;

public class TransInfo {
	//mzMin,mzMax,rtMin,rtMax,pointNumber,queryTime,sumNumber,sumTime,density,densNumber
		private List<String[]> infoStrs;

		private String infoFilePath;

		private String originFilePath;
		
		public TransInfo(String filePath)
		{
			this.originFilePath = filePath;
			infoStrs = new ArrayList<String[]>();
			infoStrs.add(new String[]{"pointNumber","spendTime","storeSpace"});
		}
		
		public void addInfo(int pointNumber, long spendTime, long storeSpace)
		{
			infoStrs.add(new String[]{Integer.toString(pointNumber),Long.toString(spendTime),Long.toString(storeSpace)});
		}
		
		public void export(long spend)
	    {
//			String suggestedFileName = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss").format(new java.util.Date());
			this.infoFilePath = this.originFilePath + "-" + spend + 
					".csv";
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
