/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.msViz.storage;

import edu.msViz.base.ImportState;
import edu.msViz.mzMD.MsBounds;
import edu.msViz.mzMD.MsDataMap;
import edu.msViz.mzMD.MsDataPoint;
import edu.msViz.mzMD.MzTreeNode;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.zip.DataFormatException;

/**
 *
 * @author Kyle
 */
public interface StorageFacade
{
    /**
     * Initializes persistent storage at the given filepath
     * @param filepath path to output file
     * @param numPoints number of points that will be saved (null if unknown)
     * @throws DataFormatException 
     */
    public void init(String filepath, Integer numPoints) throws Exception;
    
    /**
     * Copies the data storage to a new location
     * @param targetFilepath
     * @throws Exception 
     */
    public void copy(Path targetFilepath) throws Exception;

    /**
     * Queries the number of points stored
     * @return number of points
     */
    public Integer getPointCount() throws Exception;

    
    /**
     * Performs any commits or updates that are required to flush
     * any potentially pending changes to disk
     * @throws java.lang.Exception
     */
    public void flush() throws Exception;
    
    /**
     * Gets the path to the file containing the persistent save
     * @return the file path to the storage file (if applicable)
     */
    public String getFilePath();
    
    /**
     * Finalizes and closes any resources managed by the storage solution
     */
    public void close();
    
    public void appendFileWithBufferedWriter(String fileName, String appendStr) throws IOException;

    public class SavePointsTask {
        public MzTreeNode node;
        public List<MsDataPoint> dataset;
        public SavePointsTask(MzTreeNode inNode, List<MsDataPoint> inDataset)
        {
            this.node = inNode;
            this.dataset = inDataset;
        }
    }
    //**********************************************//
    //                  SAVE MAP                   //
    //**********************************************//
    
    public void saveMap(MsDataMap dataMap) throws SQLException;

	public List<MsDataPoint> loadPointsInBounds(double mzMin, double mzMax, float rtMin, float rtMax, int mapLevel, int number) throws SQLException;

	public void saveDataSet(List<MsDataPoint> dataset, double density) throws SQLException;
	
	void setPartition();

	public void saveBaseInfomation(MsBounds msBounds) throws SQLException;
	public MsBounds getBaseInfomation() throws SQLException;

	public List<Double> getDensities() throws SQLException;
	

}
