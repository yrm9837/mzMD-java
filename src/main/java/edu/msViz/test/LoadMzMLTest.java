package edu.msViz.test;

import java.io.IOException;
import java.util.zip.DataFormatException;

import javax.xml.stream.XMLStreamException;

import edu.msViz.base.MzmlParser;

public class LoadMzMLTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	    String openFilePath = "/Users/yangrunmin/Study/data/ms12/CPTAC_Intact_rep8_15Jan15_Bane_C2-14-08-02RZ.mzML";
	    openFilePath = "/Users/yangrunmin/Study/data/ms12/CPTAC_Intact_rep8_15Jan15_Bane_C2-14-08-02RZ-nopick.mzML";
		MzmlParser mzmlParser = new MzmlParser(openFilePath);
		try {
//			List<MsDataPoint> dataset = mzmlParser.readAllData();
			
//			System.out.println("Point number:"+dataset.size());
			System.out.println("Point number:"+mzmlParser.countPoints());
		} catch (IOException | XMLStreamException | DataFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
