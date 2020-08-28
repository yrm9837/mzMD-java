/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.msViz.base;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import edu.msViz.mzMD.MsDataPoint;

/**
 *
 * @author André
 */
public class MzmlParaerSum {
        
    /**
     * Accession values used by mzML to classify cvParam values
     */
    private static final String ACCESSION_SCAN_START_TIME = "MS:1000016"; // "scan start time"
    private static final String ACCESSION_MS_LEVEL = "MS:1000511"; // "ms level"
    private static final String ACCESSION_MZ_ARRAY = "MS:1000514"; // "m/z array"
    private static final String ACCESSION_INTENSITY_ARRAY = "MS:1000515"; // "intensity array"
    private static final String ACCESSION_32_BIT_FLOAT = "MS:1000521"; // "32-bit float"
    private static final String ACCESSION_64_BIT_FLOAT = "MS:1000523"; // "64-bit float"
    private static final String ACCESSION_ZLIB_COMPRESSION = "MS:1000574"; // "zlib compression"
	
    /**
     * Minimum intensity required for a point to be included in point retrieval
     */
    private static final double MIN_INTENSITY_THRESHOLD = 1;
    
    /**
     * XML stream reader (mzML is XML because... well who knows)
     */
    private XMLStreamReader reader;
    
    /**
     * path to targeted mzml file
     */
    private final String mzmlFilePath;
    
    /**
     * Number of points in the mzml file
     * Null implies no count has been performed yet
     */
    public int numPoints = -1;
    
    /**
     * Number of points to read per partition read
     */
    public int partitionSize;

    public double partitionRT;
    public double partitionStep;
    public boolean isPause = false;
    
    public double[] msBounds;
    
    private int MZnumber = 0;
    private int RTnumber = 0;
	double mzStep = 0;
	double rtStep = 0;
	MsDataPoint[][] bestPoints;
    
    /**
     * Default constructor, accepts path to mzML file
     * @param filePath path to mzML file to parse
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    public MzmlParaerSum(String filePath)
    {
        this.mzmlFilePath = filePath;  
        this.numPoints = 0;
    }
    
    public void init(double[] bounds, int m, int n)
    {
    	this.numPoints = 0;
    	this.msBounds = bounds;
    	this.MZnumber = m;
    	this.RTnumber = n;
    	this.mzStep = (bounds[1] - bounds[0])/this.MZnumber;
    	this.rtStep = (bounds[3] - bounds[2])/this.RTnumber;
    	this.bestPoints = new MsDataPoint[this.MZnumber][this.RTnumber];
    }
    
    public List<MsDataPoint> getBestSum(double[] bounds, int m, int n)
    {
    	this.init(bounds, m, n);
    	
        // instantiate xml reader on mzmlFilePath
        try {
			this.reader = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(this.mzmlFilePath));
        
	        // parsing loop
	        while (this.reader.hasNext()) 
	        {    
	            // proceed cursor
	            this.reader.next();
	
	            // look for the start of a "run" element
	            if (this.reader.getEventType() == XMLStreamReader.START_ELEMENT && this.reader.getLocalName().equals("run")) 
	            {
	                this.parseRun();
	        
	                this.reader.close();
	                
	                return getPointsFromTable();
	            }
	        }
	        
	        this.reader.close();
	        
		} catch (FileNotFoundException | XMLStreamException | FactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DataFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
        // if no run START_ELEMENT is found
        new DataFormatException("No run element found in mzML file: " + this.mzmlFilePath).printStackTrace();
		return null;
    }
    
    private List<MsDataPoint> getPointsFromTable() {

        // points collection for this partition
        List<MsDataPoint> results = new ArrayList<>();
        
        for(int i = 0; i < this.MZnumber; i++)
        {
        	for(int j = 0; j < this.RTnumber; j++)
            {
        		if(this.bestPoints[i][j] != null)
        			results.add(this.bestPoints[i][j]);
            }
        }
        
		return results;
	}

	/**
     * Parses a run element currently pointed to by reader, returning all contained MS datapoints
     * TODO current implementation will return the contents of the final spectrumList only, extend
     *      to return the contents of all spectrumList if more than one.
     * @param isCount flag indicating the data should merely be counted
     * @param count point count accumulator     
     * @throws XMLStreamException
     * @throws DataFormatException
     * @throws IOException 
     */
    private void parseRun() throws XMLStreamException, DataFormatException, IOException {
        
        // parsing loop, return at end of run element
        while (this.reader.hasNext()) 
        {
            // proceed parser
            this.reader.next();
            
            // wait for beginning of spectrumList tag
            if (this.reader.getEventType() == XMLStreamReader.START_ELEMENT && this.reader.getLocalName().equals("spectrumList")) 
            {
                // begin processing spectrumList
                this.parseSpectrumList();
            }
            
            // wait for end of run element to signal run is finished
            if (this.reader.getEventType() == XMLStreamReader.END_ELEMENT && this.reader.getLocalName().equals("run")) 
            {
                return;
            }  
        }
       
        // if no run END_ELEMENT is found
        throw new DataFormatException();
        
    }
    
    /**
     * Parses the contents of the spectrumList currently pointed to by reader, returns all contained MsDataPoints
     * @param isCount flag indicating the data should merely be counted
     * @param count point count accumulator
     * @return Array of MsDatapoints contained within the spectrum list
     * @throws XMLStreamException
     * @throws DataFormatException
     * @throws IOException 
     */
    private void parseSpectrumList() throws XMLStreamException, DataFormatException, IOException {
        
        // parsing loop
        while (reader.hasNext()) 
        {
            // proceed cursor
            reader.next();
            
            // parse discovered spectrums, collect contained points
            if (reader.getEventType() == XMLStreamReader.START_ELEMENT && reader.getLocalName().equals("spectrum")) 
            {
                this.parseSpectrum(0);
            }
            
            // return points at end of spectrumlist
            if (reader.getEventType() == XMLStreamReader.END_ELEMENT && reader.getLocalName().equals("spectrumList")) 
            {
                return;
            }
        }
        
        // if no spectrumList END_ELEMENT is found
        throw new DataFormatException();
    }
    
    /**
     * Parses a spectrum element, storing all discovered points in pointCollection
     * @param pointCollection collection to store all discovered points
     * @param isCount flag indicating data should only be counted
     * @param count data count accumulator
     * @throws XMLStreamException
     * @throws DataFormatException
     * @throws IOException 
     */
    private void parseSpectrum(int startIndex) throws XMLStreamException, DataFormatException, IOException {
       
        // a new SpectrumInformation object is created, this stores all the relevant data in cvParams for one spectrum
        SpectrumInformation spectrumInfo = new SpectrumInformation();
        
        // This BinaryDataArray stores values from cvParams examined until it is identified as an intensity array
        // or a m/z array. Then, currentBDA will be assigned to the relevent BinaryDataArray in currentSI.
        // a new currentBDA will be declare when the end of a BinaryDataArray element in the .mzML is reached
        EncodedData currentEncoding = new EncodedData(); 
        
        // assumably, each mzML spectrum element and each mzML binaryDataArray element will have all the necessary values
        // recorded in cvParam elements. There is not yet any behavior in this method to deal with missing data (such as throwing an
        // exception.
        
        // parsing loop
        while (this.reader.hasNext()) 
        {
            // proceed cursor
            this.reader.next();
            
            // START_ELEMENT events to handle
            if (this.reader.getEventType() == XMLStreamReader.START_ELEMENT) 
            {
                // cvParam element
                if (this.reader.getLocalName().equals("cvParam")) 
                {
                    this.updateSpectrumInformation(examineCVParam(this.reader), spectrumInfo, currentEncoding);
//                    System.out.println("scanStartTime="+spectrumInfo.scanStartTime);
                    // only process ms level 1 data
                    if(spectrumInfo.msLevel != 0 && spectrumInfo.msLevel != 1)
                        return;
                }
                
                // 64-bit encoded binary data
                if (this.reader.getLocalName().equals("binary")) 
                {
                    currentEncoding.encoding = reader.getElementText();
                    if(currentEncoding.isMz) spectrumInfo.mzEncoding = currentEncoding; 
                    else spectrumInfo.intensityEncoding = currentEncoding;
                }
            }

            if(spectrumInfo.scanStartTime <= this.msBounds[2])
            	continue;
            if(spectrumInfo.scanStartTime >= this.msBounds[3])
            	break;
            // END_ELEMENT events to handle
            if (this.reader.getEventType() == XMLStreamReader.END_ELEMENT) 
            {
                // finish 64-bit encoded data array
                if (this.reader.getLocalName().equals("binaryDataArray")) 
                { 
                    // reinstantiate currentEncoding for potential additional encoding 
                    currentEncoding = new EncodedData();
                }
                
                // spectrum finish
                if (this.reader.getLocalName().equals("spectrum")) 
                {
                    // decode 64-bit encoded data, collect data points 
                    this.decodeAndCollectData(spectrumInfo, startIndex);
                    return;
                }
                
            }
        }
    }
    
    /**
     * Decodes the encoded spectrum data, packages as MsDataPoints and inserts points into pointCollection
     * @param currentSpecInfo spectrum bundle to process
     * @param pointCollection container to place discovered MsDataPoints
     * @param isCount flag indicating the data should merely be counted
     * @param count point count accumulator
     * @throws DataFormatException
     * @throws IOException 
     */
    private void decodeAndCollectData(SpectrumInformation currentSpecInfo, int startIndex) throws DataFormatException, IOException 
    {
        // decode (and if necessary decompress) mz data encoding
        double[] mzArrayDoubles;
        if (currentSpecInfo.mzEncoding.isCompressed) 
            mzArrayDoubles = Decoder.decodeCompressed(currentSpecInfo.mzEncoding.encoding, currentSpecInfo.mzEncoding.bits == 64); 
        else
            mzArrayDoubles = Decoder.decodeUncompressed(currentSpecInfo.mzEncoding.encoding, currentSpecInfo.mzEncoding.bits == 64);

        // decode (and if necessary decompress) intensity data encoding
        double[] intensityArrayDoubles;
        if (currentSpecInfo.intensityEncoding.isCompressed)
            intensityArrayDoubles = Decoder.decodeCompressed(currentSpecInfo.intensityEncoding.encoding, currentSpecInfo.intensityEncoding.bits == 64);
        else
            intensityArrayDoubles = Decoder.decodeUncompressed(currentSpecInfo.intensityEncoding.encoding, currentSpecInfo.intensityEncoding.bits == 64);

        // creates a MsDataPoint for each (mz,rt,int) point and adds to the arrayList
        // terminates if pointCollection reaches pointLimit
        for (int i = startIndex; i < mzArrayDoubles.length; i++) 
        {
            
            // if the data point's intensity is below the min threshold then THROW IT OUT
            if (intensityArrayDoubles[i] >= MIN_INTENSITY_THRESHOLD) 
            {
            	if(mzArrayDoubles[i] <= this.msBounds[0])
            		continue;
            	if(mzArrayDoubles[i] >= this.msBounds[1])
            		break;
            	int mzIndex = (int) ((mzArrayDoubles[i] - this.msBounds[0])/this.mzStep);
            	int rtIndex = (int) (((float)currentSpecInfo.scanStartTime - this.msBounds[2])/this.rtStep);
                if(this.bestPoints[mzIndex][rtIndex] == null || this.bestPoints[mzIndex][rtIndex].intensity < intensityArrayDoubles[i])
                {
                	this.bestPoints[mzIndex][rtIndex] = new MsDataPoint(0, mzArrayDoubles[i], currentSpecInfo.scanStartTime, intensityArrayDoubles[i]);
                }
                this.numPoints++;
            }
        }           
    }
    
    /**
     * Updates currentSI with the provided accession entry
     * @param cvParamPair [accession, value] pair from cvParam
     * @param currentSpecInfo spectrum info for current spectrum
     * @param currentEncoding current encoded data object
     * @throws XMLStreamException 
     */
    private void updateSpectrumInformation(String[] cvParamPair, SpectrumInformation currentSpecInfo, EncodedData currentEncoding) {
        
        // null accession value implies badly formatted cvParam
        if (cvParamPair[0] == null) 
        {
            return;
        }
        switch (cvParamPair[0]) 
        {
            case MzmlParaerSum.ACCESSION_SCAN_START_TIME: // "scan start time"
                currentSpecInfo.scanStartTime = Float.parseFloat(cvParamPair[1]);
                break;
        
            case MzmlParaerSum.ACCESSION_MS_LEVEL: // "ms level"
                currentSpecInfo.msLevel = Short.parseShort(cvParamPair[1]);
                break;
            
            case MzmlParaerSum.ACCESSION_MZ_ARRAY: // "m/z array"
                currentEncoding.isMz = true;
                break;
                
            case MzmlParaerSum.ACCESSION_INTENSITY_ARRAY: // "intensity array"
                currentEncoding.isMz = false;
                break;
                
            case MzmlParaerSum.ACCESSION_32_BIT_FLOAT: // "32-bit float"
                currentEncoding.bits = 32;
                break;   
                
            case MzmlParaerSum.ACCESSION_64_BIT_FLOAT: // "64-bit float"
                currentEncoding.bits = 64;
                break;
                
            case MzmlParaerSum.ACCESSION_ZLIB_COMPRESSION: // "zlib compression"
                currentEncoding.isCompressed = true;
                break;
                
            default:
        }
                
    }
    
    /**
     * parses the cvParam pointed to by reader 
     * @param reader xml stream readers
     * @return String array with elements [accession, value]
     * @throws XMLStreamException 
     */
    private String[] examineCVParam(XMLStreamReader reader)
    {
        String accession = reader.getAttributeValue(null, "accession");
        String value = reader.getAttributeValue(null, "value");
        return new String[] {accession, value};
    }  
    
    /**
     * Override Object.finalize to include closing the reader on GC
     * @throws Throwable 
     */
    @Override
    protected void finalize() throws Throwable{
        super.finalize();
        this.reader.close();
    }
       
}
