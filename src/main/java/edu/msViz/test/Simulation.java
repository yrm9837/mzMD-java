package edu.msViz.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import edu.msViz.base.MzmlParser;
import edu.msViz.mzMD.Configure;
import edu.msViz.mzMD.MsDataPoint;

public class Simulation {
	private static final Logger LOGGER = Logger.getLogger(Simulation.class.getName());
	
	private String filePath;
	
	private Configure.SimulationType SimulationChoice;
	
	// minimum mz at this map
	private double mzMin;
    // maximum mz at this map
	private double mzMax;
    // minimum rt at this map
	private double rtMin;
    // maximum rt at this map
	private double rtMax;
	
	public Simulation(String filePath)
	{
		this.filePath = filePath;
		this.SimulationChoice = Configure.SimulationChoice;
	}
	
	 /**
	 * Discovers min/max mz/rt
	 */
	public double[] loadXML()
	{
		MzmlParser mzmlParser = new MzmlParser(filePath);
		try {
			List<MsDataPoint> msData = mzmlParser.readAllData();
			
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

			
		} catch (IOException | XMLStreamException | DataFormatException e) {
			// TODO Auto-generated catch block
			LOGGER.log(Level.WARNING, "Couldn't load mzML file.");
		}
		return new double[] {this.mzMin, this.mzMax, this.rtMin, this.rtMax};
	}
	
	public List<double[]> loadCSV()
	{
		List<double[]> allBounds = new ArrayList<double[]>();
	        
        // open csv reader on targetted csv file
        CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(filePath));
        
	        // first line might be a header
	        String[] line = reader.readNext();
	        
	        // if the first line is not a header
	        if(line != null && StringUtils.isNumeric(line[0]))
	        {
	        	double[] bound = new double[] {Double.parseDouble(line[0]),
	        			Double.parseDouble(line[1]),Double.parseDouble(line[2]),Double.parseDouble(line[3]),Double.parseDouble(line[4])};
	        	allBounds.add(bound);
	        }
	        
	        // read the remaining lines (now guaranteed no header)
	        while((line = reader.readNext()) != null)
	        {
	        	double[] bound = new double[] {Double.parseDouble(line[0]),
	        			Double.parseDouble(line[1]),Double.parseDouble(line[2]),Double.parseDouble(line[3]),Double.parseDouble(line[4])};
	        	allBounds.add(bound);
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return allBounds;
	}
	
	public double[] getDataBounds()
	{
		return new double[] {this.mzMin, this.mzMax, this.rtMin, this.rtMax};
	}
	
	public double[] randomSim(int minPercent, int maxPercent)
	{
		double mz = this.mzMax - this.mzMin;
		double rt = this.rtMax - this.rtMin;
		double mzPercent = (minPercent + (maxPercent-minPercent)*Math.random())/100;
		double rtPercent = mzPercent;
		
		switch(this.SimulationChoice){
        default:
        case Random:
        	rtPercent = (minPercent + (maxPercent-minPercent)*Math.random())/100;
        	break;
        case SameShape:
        	break;
    	}
		
		double mzStart = this.mzMin + mz*(1-mzPercent)*Math.random();
		double rtStart = this.rtMin + rt*(1-rtPercent)*Math.random();
		return new double[] {mzStart,mzStart+mz*mzPercent,rtStart,rtStart+rt*rtPercent};
	}
	
	public void export(List<double[]> allBounds)
    {
	        //append csv extension if not already there
	        if(!filePath.endsWith(".csv"))
	        	filePath = filePath + ".csv";
        
        try ( CSVWriter writer = new CSVWriter(new FileWriter(filePath)) ) 
        {
            writer.writeNext(new String[] {"mzMin","mzMax","rtMin","rtMax","pointNumber"});
            
            // write away!
            for (double[] bounds : allBounds)
                writer.writeNext( new String[] {Double.toString(bounds[0]), Double.toString(bounds[1]), Double.toString(bounds[2]), Double.toString(bounds[3]), Double.toString(bounds[4]) });
            
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
            return;
    }
	public void simTransArea(double minInt, double maxInt, double stepInt)
	{
		List<double[]> allBounds = new ArrayList<double[]>();
		List<double[]> newBounds = new ArrayList<double[]>();
	        
        // open csv reader on targetted csv file
        CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(filePath));
        
	        // first line might be a header
	        String[] line = reader.readNext();
	        
	        // if the first line is not a header
	        if(line != null && StringUtils.isNumeric(line[0]))
	        {
	        	double[] bound = new double[] {Double.parseDouble(line[0]),
	        			Double.parseDouble(line[1]),Double.parseDouble(line[2]),
	        			Double.parseDouble(line[3]),Double.parseDouble(line[4])};
	        	allBounds.add(bound);
	        }
	        
	        // read the remaining lines (now guaranteed no header)
	        while((line = reader.readNext()) != null)
	        {
	        	double[] bound = new double[] {Double.parseDouble(line[0]),
	        			Double.parseDouble(line[1]),Double.parseDouble(line[2]),
	        			Double.parseDouble(line[3]),Double.parseDouble(line[4])};
	        	allBounds.add(bound);
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		double nowInt = minInt;
		for(int i = 0; i < allBounds.size(); i++)
		{
			if(allBounds.get(i)[4] > maxInt)
			{
				break;
			}
			if(allBounds.get(i)[4] > nowInt)
			{
				newBounds.add(allBounds.get(i));
				nowInt = nowInt + stepInt;
			}
		}
		
        filePath = filePath + ".csv";
    
	    try ( CSVWriter writer = new CSVWriter(new FileWriter(filePath)) ) 
	    {
	        writer.writeNext(new String[] {"mzMin","mzMax","rtMin","rtMax","number"});
	        
	        // write away!
	        for (double[] bounds : newBounds)
	            writer.writeNext( new String[] {Double.toString(bounds[0]), Double.toString(bounds[1]), 
	            		Double.toString(bounds[2]), Double.toString(bounds[3]), Double.toString(bounds[4]) });
	        
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return;
	}

	public void exportSum(int index, List<MsDataPoint> points) {

        Path sourceFilePath = Paths.get(filePath + ".sum");
        Path suggestedFilePath = sourceFilePath.resolve(index+".csv");
        judeDirExists(suggestedFilePath.toString());
    
	    try ( CSVWriter writer = new CSVWriter(new FileWriter(suggestedFilePath.toString())) ) 
	    {
	        writer.writeNext(new String[] {"mz","rt","intensity"});
	        
	        // write away!
	        for (int j = 0; j < points.size(); j++)
	            writer.writeNext( new String[] {Double.toString(points.get(j).mz), Double.toString(points.get(j).rt), Double.toString(points.get(j).intensity)});
	        
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	        return;
			
	}
	

    /**
     * Loads MS data in csv format (mz, rt, intensity, meta1)
     * @param filePath path to csv file
     * @return 
     * @throws FileNotFoundException
     * @throws IOException
     * @throws Exception 
     */
    public ArrayList<MsDataPoint> csvSumLoad(String filePath, int index)
    {
        ArrayList<MsDataPoint> points = new ArrayList<>();
        
        Path sourceFilePath = Paths.get(filePath + ".sum");
        Path suggestedFilePath = sourceFilePath.resolve(index+".csv");
        
        // open csv reader on targetted csv file
        CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(suggestedFilePath.toString()));
        
	        // first line might be a header
	        String[] line = reader.readNext();
	        
	        // if the first line is not a header
	        if(line != null && StringUtils.isNumeric(line[0]))
	        {
	            // convert to msdatapoint, collect
	            MsDataPoint point = this.csvRowToMsDataPoint(line);
	            points.add(point);
	        }
	        
	        // read the remaining lines (now guaranteed no header)
	        while((line = reader.readNext()) != null)
	        {
	            // convert to msdatapoint, collect
	            MsDataPoint point = this.csvRowToMsDataPoint(line);
	            points.add(point);
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return points;
        
    }

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
        MsDataPoint point = new MsDataPoint(0, mz, rt, intensity);
        return point;
    }

    /**
     * create dir if not exists
     * @param myPath
     */
    private static void judeDirExists(String myPath) {
    	File myDir = new File(Paths.get(myPath).getParent().toString());
        if (!myDir.exists()) {
        	myDir.mkdirs(); 
            System.out.println("Create path:"+ myDir.getPath());
        }
    }
	
	public static void main(String[] args) {
		String openFilePath = "/Users/yangrunmin/Study/data/sim.csv";
		Simulation sim = new Simulation(openFilePath);
		sim.simTransArea(100000,11000000,100000);
	}

}


