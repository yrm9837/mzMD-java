/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.msViz.storage;

import edu.msViz.base.ImportState;
import edu.msViz.mzMD.Configure;
import edu.msViz.mzMD.MsBounds;
import edu.msViz.mzMD.MsDataMap;
import edu.msViz.mzMD.MsDataPoint;
import edu.msViz.mzMD.MzTreeNode;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

/**
 * StorageFacade implementation that utilized SQLite for persistance
 * @author Runmin
 */
public class NormalDBStorage implements StorageFacade
{
    private static final Logger LOGGER = Logger.getLogger(NormalDBStorage.class.getName());

    // SQL options
    private Configure.SqlType sqlChoice;
    private Configure.SqlMode sqlModeChoice;

    // connection to the SQLite database
    private Connection dbConnection;
    
    // SQL statement preparation and execution
    private SQLEngineFacade dbEngine;
    
    // path to the database and point files
    private String filePath;
//    private String pointFilePath;
    
    private int maxLevel = 0;
    
 // minimum mz at this map
	private double mzMin;
    
    // maximum mz at this map
	private double mzMax;
    
    // minimum rt at this map
	private double rtMin;
    
    // maximum rt at this map
	private double rtMax;
	
	public boolean firstPartition = true;
    
    //**********************************************//
    //                    INIT                      //
    //**********************************************//
    
    @Override
    public void init(String filePath, Integer numPoints) throws Exception
    {

        this.sqlChoice = Configure.sqlChoice;
        this.sqlModeChoice = Configure.sqlModeChoice;
        this.mzMax = -1;
        this.mzMin = -1;
        this.rtMax = -1;
        this.rtMin = -1;
        // generate output file location if none passed
        if(filePath == null)
        {
            throw new IllegalArgumentException("filePath must be specified");
        }
        
        this.filePath = filePath;
//        this.pointFilePath = filePath + "-points";
        
        judeDirExists(this.filePath);
        
        try{
            // link the JDBC-sqlite class
            Class.forName("org.sqlite.JDBC");
            
            // connect to sqlite database at specified location
            // if doesn't exist then new database will be created
//            this.dbConnection = DriverManager.getConnection("jdbc:sqlite:" + this.filePath);
            switch(this.sqlModeChoice){
            default:
            case File:
            	this.dbConnection = DriverManager.getConnection("jdbc:sqlite:" + this.filePath);
            	break;
            case Memory:
            	this.dbConnection = DriverManager.getConnection("jdbc:sqlite::memory:");
        	}
            

            // enable foreign keys
            Statement setupStatement = this.dbConnection.createStatement();
//            setupStatement.execute("PRAGMA foreign_keys=ON;");
            setupStatement.close();
            
            // disable auto commit (enables user defined transactions)
            this.dbConnection.setAutoCommit(false);
            
            // construct SQL Engine and Point Engine
//            this.dbEngine = new SQLEngine();
            switch(this.sqlChoice){
            default:
            case NormalSql:
            	this.dbEngine = new SQLEngine();
            	break;
            case RTreeSql:
            	this.dbEngine = new SQLEngineWithRTree();
            	break;
            case DeRedundancy:
            	this.dbEngine = new SQLEngineDeRedundancy();;
        	}
//            this.pointEngine = new PointEngine(pointFilePath);
            
            // reserve space for the number of incoming points
//            if(numPoints != null)
//                this.pointEngine.reserveSpace(numPoints);
            
        }
        catch(Exception e)
        {
            LOGGER.log(Level.WARNING, "Unable to initialize database connection at " + this.filePath, e);
            
            // SQLExceptions contain leading tags that describe what ACTUALLY happened
            // looking for case when the file chosen was not a database
            if(e.getMessage().contains("[SQLITE_NOTADB]"))
                throw new DataFormatException(e.getMessage());
            
            else throw e;
        }
    }

    //**********************************************//
    //                  SAVE MAP                   //
    //**********************************************//
    
    public void saveMap(MsDataMap dataMap) throws SQLException
    {
    	if(this.mzMax == -1)
    	{
    		this.mzMax = dataMap.mzMax;
            this.mzMin = dataMap.mzMin;
            this.rtMax = dataMap.rtMax;
            this.rtMin = dataMap.rtMin;

//    		System.out.println(this.mzMin+","+this.mzMax+","+this.rtMin+","+this.rtMax);
    	}

    	this.dbEngine.insertMap(dataMap.mapLevel, dataMap.density);

        if(this.maxLevel < dataMap.mapLevel)
        	this.maxLevel = dataMap.mapLevel;

    	for(int i = 0; i < dataMap.rtSize; i++) 
    	{
    		for(int j = 0; j < dataMap.mzSize; j++) 
    		{
    			int pointIndex = dataMap.dataMap[i][j]-1;
    			if(pointIndex != -1)
    			{
    				this.dbEngine.insertPoint(dataMap.msData.get(pointIndex), dataMap.mapLevel);
    			}
    		}
    	}
        return;
    } 
    public void saveDataSet(List<MsDataPoint> dataset, double density) throws SQLException
    {

    	this.dbEngine.insertMap(0, density*MsDataMap.SCALE_FIRST_LEVEL);
    	for(int i = 0; i < dataset.size(); i++) 
    	{
    		this.dbEngine.insertPoint(dataset.get(i), 0);
    	}
    	
        return;
    }
    
    @Override
	public void setPartition() {
		// TODO Auto-generated method stub
		this.dbEngine.setPartition();
	}

	@Override
	public void saveBaseInfomation(MsBounds msBounds) throws SQLException {
		MsBounds tempResult = this.dbEngine.getBaseInfomation();
		if(tempResult != null)
		{
			msBounds.update(tempResult);
			this.dbEngine.setBaseInfomation(msBounds.get(), msBounds.getCount(), false);
		}
		else
		{
			this.dbEngine.setBaseInfomation(msBounds.get(), msBounds.getCount(), true);
		}
		
	}
	
	public MsBounds getBaseInfomation() throws SQLException {
		return this.dbEngine.getBaseInfomation();
	}

	@Override
	public List<Double> getDensities() throws SQLException {
		// TODO Auto-generated method stub
		return this.dbEngine.getDensities();
	}
    
    
    //**********************************************//
    //                  LOAD POINTS                 //
    //**********************************************//
    
	@Override
	public List<MsDataPoint> loadPointsInBounds(double mzMin, double mzMax, float rtMin, float rtMax, int mapLevel, int number) throws SQLException {
		// TODO Auto-generated method stub
		List<MsDataPoint> results = new ArrayList<>();
		if(number > 0)
		{
			for(int i = this.maxLevel; i > -1; i--)
			{	
				List<MsDataPoint> tempResults = this.dbEngine.selectPointFromDBOne(mzMin, mzMax, rtMin, rtMax, i);
				if(tempResults.size()+results.size()<number)
				{
					results.addAll(tempResults);
				}
				else
				{
					break;
				}
			}
		}
		else
		{
			results.addAll(this.dbEngine.selectPointFromDB(mzMin, mzMax, rtMin, rtMax, mapLevel));
		}
        
//        
        return results;
	}
	
    //**********************************************//
    //                    FLUSH                     //
    //**********************************************//
    
    private static final int APPLICATION_ID = 223764263;

	@Override
    public void flush() throws SQLException, IOException
    {
        this.dbConnection.commit();
//        this.pointEngine.flush();
    }
    
    //**********************************************//
    //                 GET FILE PATH                //
    //**********************************************//
    
    @Override
    public String getFilePath()
    {
        return this.filePath;
    }
    
    //**********************************************//
    //                    CLOSE                     //
    //**********************************************//
    
    @Override
    public void close()
    {
        try {
            this.flush();
            this.dbConnection.close();
//            this.pointEngine.pointFile.close();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not cleanly close storage", e);
        } finally {
            this.dbConnection = null;
        }
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
    
    public void appendFileWithBufferedWriter(String fileName, String appendStr) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName,true));
        writer.append(appendStr);
        writer.close();
    }
    //**********************************************//
    //                 SQL ENGINE                   //
    //**********************************************//
    public interface SQLEngineFacade {

		int maxLevel = 0;

		void insertMap(int mapLevel, double density) throws SQLException;

		List<Double> getDensities() throws SQLException;

		public void setBaseInfomation(double[] bounds, int count, boolean isInsert) throws SQLException;

		MsBounds getBaseInfomation() throws SQLException;

		List<MsDataPoint> selectPointFromDBOne(double mzMin, double mzMax, float rtMin, float rtMax,
				int mapLevel) throws SQLException;

		public ArrayList<MsDataPoint> selectPointFromDB(double mzMin, double mzMax, float rtMin, float rtMax,
				int mapLevel) throws SQLException;

		void insertPoint(MsDataPoint msDataPoint, int mapLevel) throws SQLException;
		

		void setPartition();
    }

    /**
     * Inner class for preparing and executing normal SQL statements
     */
    private class SQLEngine implements SQLEngineFacade{
        
        private static final int USER_VERSION = 1;

        public boolean firstPartition = true;
        
        // ordered create infomation table statements
        public final String[] createMapsTableStatements = {
                "CREATE TABLE IF NOT EXISTS Maps (mapLevel INTEGER PRIMARY KEY, density DOUBLE NOT NULL);",
            };
        public final String[] createPointsTableStatements = {
                "CREATE TABLE IF NOT EXISTS Points? (pointId INTEGER PRIMARY KEY, mz DOUBLE NOT NULL, rt FLOAT NOT NULL, int DOUBLE);",
                "CREATE INDEX IF NOT EXISTS Points_index ON Points? (rt, mz);",
            };
        
        // insert statements 
        private final PreparedStatement insertMapsStatement; 
        private PreparedStatement insertPointStatement; 

        // select statements
        private final String selectPointInBoundsPre = "SELECT pointId, mz, rt, int FROM Points";
        private final String selectPointInBoundsExt = " WHERE mz>? AND mz<? AND rt>? AND rt<?;";
        
        /**
         * Default constructor
         * Ensures that tables exist within database and creates prepared statements
         */
        public SQLEngine() throws Exception
        {
			int appId;

            // check the application ID
            try (Statement checkAppIdStatement = dbConnection.createStatement()) {
                ResultSet appIdResult = checkAppIdStatement.executeQuery("PRAGMA application_id;");
                appIdResult.next();
                appId = appIdResult.getInt(1);
            }

            if (appId == 0) {
                // appId == 0 means it's not an mzMD or it's empty

                try (PreparedStatement checkEmpty = dbConnection.prepareStatement("SELECT count(*) FROM sqlite_master;")) {
                    ResultSet ers = checkEmpty.executeQuery();
                    ers.next();
                    int tables = ers.getInt(1);
                    if (tables != 0) {
                        throw new Exception("Not an mzMD file");
                    }
                }

                LOGGER.log(Level.INFO, "Creating a new mzMD file, version " + USER_VERSION);

                // initializing a new database with the current version
                try (Statement updateAppIdStatement = dbConnection.createStatement()) {
                    updateAppIdStatement.execute("PRAGMA application_id = " + APPLICATION_ID + ";");
                    updateAppIdStatement.execute("PRAGMA user_version = " + USER_VERSION + ";");
                }

                Statement statement = dbConnection.createStatement();
                for(String createTableStatement : this.createMapsTableStatements)
                    statement.execute(createTableStatement);
                statement.close();
                dbConnection.commit();

            } else if (appId != APPLICATION_ID) {
                throw new SQLException("Not an mzMD file.");
            }

            int userVersion;
            // check the user version for upgrades
            try (Statement userVersionStatement = dbConnection.createStatement()) {
                ResultSet userVersionResult = userVersionStatement.executeQuery("PRAGMA user_version;");
                userVersionResult.next();
                userVersion = userVersionResult.getInt(1);
            }

            // process version upgrades
            if (userVersion != USER_VERSION)
            {
                LOGGER.log(Level.INFO, "Converting mzMD file from version " + userVersion);

                // use switch fall-through (no "break" statement) to run multiple migrations
                // commented-out examples below
                switch(userVersion) {
                    case 0:
                        //convert_v0_v1();
                        break;
                    default:
                        throw new SQLException("Unsupported mzMD file version.");
                }

                try(Statement updateUserVersionStatement = dbConnection.createStatement()) {
                    updateUserVersionStatement.execute("PRAGMA user_version = " + USER_VERSION + ";");
                }

                LOGGER.log(Level.INFO, "mzTree file mzMD to version " + USER_VERSION);
            }
          
            // init insert statements 
            this.insertMapsStatement = dbConnection.prepareStatement("INSERT INTO Maps (mapLevel, density) VALUES (?,?);");

        }

        /**
         * Inserts an Point into the database
         * @param node MzTreeNode to insert
         * @param parentNodeID node's parentNodeID, 0 signals null parentNodeID (root node only)
         * @return ID of the node in the database
         * @throws SQLException
         */
        public void insertMap(int mapLevel, double density) throws SQLException
        {
        	
        	if(this.firstPartition)
        	{
            	// set values in prepared statement

                this.insertMapsStatement.setInt(1, mapLevel);
                this.insertMapsStatement.setDouble(2, density);
                
                // execute insert
                this.insertMapsStatement.executeUpdate();
        	}
            
            Statement statement = dbConnection.createStatement();
            for(String createTableStatement : this.createPointsTableStatements)
            {
//            	System.out.println(createTableStatement.replaceAll("\\?", mapLevel+""));
                statement.execute(createTableStatement.replaceAll("\\?", mapLevel+""));
            }
            statement.close();
            dbConnection.commit();
            
            this.insertPointStatement = dbConnection.prepareStatement("INSERT INTO Points"+mapLevel+" (pointId, mz, rt, int) VALUES (?,?,?,?);");

            return;
        }
        
        public void insertPoint(MsDataPoint point, int mapLevel) throws SQLException
        {
            // set values in prepared statement
            
            // set null for primary key, db autoincrements
            this.insertPointStatement.setNull(1, Types.INTEGER);
            
            // mz, rt and intensity bounds
            this.insertPointStatement.setDouble(2, point.mz);
            this.insertPointStatement.setDouble(3, point.rt);
            this.insertPointStatement.setDouble(4, point.intensity);

            // execute insert
            this.insertPointStatement.executeUpdate();

            return;
        }

        public ArrayList<MsDataPoint> selectPointFromDB(double mzMin, double mzMax, float rtMin, float rtMax, int mapLevel) throws SQLException
        {
            ResultSet results;
            PreparedStatement selectPointPrepStatement;
            
            selectPointPrepStatement = dbConnection.prepareStatement(this.selectPointInBoundsPre + mapLevel + this.selectPointInBoundsExt);
            selectPointPrepStatement.setDouble(1, mzMin);
            selectPointPrepStatement.setDouble(2, mzMax);
            selectPointPrepStatement.setFloat(3, rtMin);
            selectPointPrepStatement.setFloat(4, rtMax);
            results = selectPointPrepStatement.executeQuery();
                
            // flush result set to list of points
            ArrayList<MsDataPoint> points = new ArrayList<>();
            while(results.next())
            {
                // create new node and assign values
            	MsDataPoint point = new MsDataPoint(results.getInt(1),results.getDouble(2),results.getFloat(3),results.getDouble(4));

                // collect point
            	points.add(point);
            }
                
            // close prepared statement (also closes resultset)
            selectPointPrepStatement.close();
            
            return points;
        	
        }

		@Override
		public List<MsDataPoint> selectPointFromDBOne(double mzMin, double mzMax, float rtMin,
				float rtMax, int mapLevel) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setPartition() {
			// TODO Auto-generated method stub
			this.firstPartition = false;
		}

		@Override
		public MsBounds getBaseInfomation() throws SQLException {

            ResultSet results;
            PreparedStatement selectPointPrepStatement;
            
            selectPointPrepStatement = dbConnection.prepareStatement("SELECT mzmin, mzmax, rtmin, rtmax, count FROM Base WHERE id = 1;");
            results = selectPointPrepStatement.executeQuery();
                
            // flush result set
            MsBounds msBounds = null;
            while(results.next())
            {
            	msBounds = new MsBounds(results.getDouble(1),results.getDouble(2),results.getFloat(3),results.getFloat(4),results.getInt(5));
            }
                
            // close prepared statement (also closes resultset)
            selectPointPrepStatement.close();
            return msBounds;
		}

		@Override
		public void setBaseInfomation(double[] bounds, int count, boolean isInsert) throws SQLException {

	        PreparedStatement insertBase; 
	        
			if(isInsert)
			{
				insertBase = dbConnection.prepareStatement("INSERT INTO Base (id, mzmin, mzmax, rtmin, rtmax, count) VALUES (1,"
						+ bounds[0] +","+ bounds[1] +","+ bounds[2] +","+ bounds[3] +","+ count +");");
			}
			else
			{
				insertBase = dbConnection.prepareStatement("UPDATE Base SET mzmin = " + bounds[0] + ", mzmax = " + bounds[1] + ", rtmin = " 
						+ bounds[2] + ", rtmax = " + bounds[3] + ", count = " + count + " WHERE id = 1;");
			}
			// execute insert
			insertBase.executeUpdate();
			insertBase.close();
			
		}

		@Override
		public List<Double> getDensities() throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}
       
    }

    /**
     * Inner class for preparing and executing r-tree SQL statements
     */
    private class SQLEngineWithRTree implements SQLEngineFacade{
        
        private static final int USER_VERSION = 2;
        
        // ordered create infomation table statements
        public final String[] createMapsTableStatements = {
                "CREATE TABLE IF NOT EXISTS Maps (mapLevel INTEGER PRIMARY KEY, density DOUBLE NOT NULL);",
            };
//        public final String[] createPointsTableStatements = {
//                "CREATE TABLE IF NOT EXISTS Points? (pointId INTEGER PRIMARY KEY, mz DOUBLE NOT NULL, rt FLOAT NOT NULL, int DOUBLE);",
//                "CREATE INDEX IF NOT EXISTS Points_index ON Points? (rt, mz);",
//            };
        public final String[] createPointsTableStatements = {
                "CREATE VIRTUAL TABLE IF NOT EXISTS Points? USING rtree(pointId, minMz, maxMz, minRt, maxRt, +int DOUBLE);",
            };
        
        // insert statements 
        private final PreparedStatement insertMapsStatement; 
        private PreparedStatement insertPointStatement; 

        // select statements
        private final String selectPointInBoundsPre = "SELECT pointId, minMz as mz, minRt as rt, int FROM Points";
        private final String selectPointInBoundsExt = " WHERE maxMz>? AND minMz<? AND maxRt>? AND minRt<?;";
        
        /**
         * Default constructor
         * Ensures that tables exist within database and creates prepared statements
         */
        public SQLEngineWithRTree() throws Exception
        {
			int appId;

            // check the application ID
            try (Statement checkAppIdStatement = dbConnection.createStatement()) {
                ResultSet appIdResult = checkAppIdStatement.executeQuery("PRAGMA application_id;");
                appIdResult.next();
                appId = appIdResult.getInt(1);
            }

            if (appId == 0) {
                // appId == 0 means it's not an mzMD or it's empty

                try (PreparedStatement checkEmpty = dbConnection.prepareStatement("SELECT count(*) FROM sqlite_master;")) {
                    ResultSet ers = checkEmpty.executeQuery();
                    ers.next();
                    int tables = ers.getInt(1);
                    if (tables != 0) {
                        throw new Exception("Not an mzMD file");
                    }
                }

                LOGGER.log(Level.INFO, "Creating a new mzMD file, version " + USER_VERSION);

                // initializing a new database with the current version
                try (Statement updateAppIdStatement = dbConnection.createStatement()) {
                    updateAppIdStatement.execute("PRAGMA application_id = " + APPLICATION_ID + ";");
                    updateAppIdStatement.execute("PRAGMA user_version = " + USER_VERSION + ";");
                }

                Statement statement = dbConnection.createStatement();
                for(String createTableStatement : this.createMapsTableStatements)
                    statement.execute(createTableStatement);
                statement.close();
                dbConnection.commit();

            } else if (appId != APPLICATION_ID) {
                throw new SQLException("Not an mzMD file.");
            }

            int userVersion;
            // check the user version for upgrades
            try (Statement userVersionStatement = dbConnection.createStatement()) {
                ResultSet userVersionResult = userVersionStatement.executeQuery("PRAGMA user_version;");
                userVersionResult.next();
                userVersion = userVersionResult.getInt(1);
            }

            // process version upgrades
            if (userVersion != USER_VERSION)
            {
                LOGGER.log(Level.INFO, "Converting mzMD file from version " + userVersion);

                // use switch fall-through (no "break" statement) to run multiple migrations
                // commented-out examples below
                switch(userVersion) {
                    case 0:
                        //convert_v0_v1();
                        break;
                    default:
                        throw new SQLException("Unsupported mzMD file version.");
                }

                try(Statement updateUserVersionStatement = dbConnection.createStatement()) {
                    updateUserVersionStatement.execute("PRAGMA user_version = " + USER_VERSION + ";");
                }

                LOGGER.log(Level.INFO, "mzTree file mzMD to version " + USER_VERSION);
            }
          
            // init insert statements 
            this.insertMapsStatement = dbConnection.prepareStatement("INSERT INTO Maps (mapLevel, density) VALUES (?,?);");

        }

        /**
         * Inserts an Point into the database
         * @param node MzTreeNode to insert
         * @param parentNodeID node's parentNodeID, 0 signals null parentNodeID (root node only)
         * @return ID of the node in the database
         * @throws SQLException
         */
        public void insertMap(int mapLevel, double density) throws SQLException
        {
            // set values in prepared statement

            this.insertMapsStatement.setInt(1, mapLevel);
            this.insertMapsStatement.setDouble(2, density);
            
            // execute insert
            this.insertMapsStatement.executeUpdate();
            
            Statement statement = dbConnection.createStatement();
            for(String createTableStatement : this.createPointsTableStatements)
            {
//            	System.out.println(createTableStatement.replaceAll("\\?", mapLevel+""));
//            	System.out.println(createTableStatement.replaceAll("\\?", mapLevel+""));
                statement.execute(createTableStatement.replaceAll("\\?", mapLevel+""));
            }
            statement.close();
            dbConnection.commit();
            
            this.insertPointStatement = dbConnection.prepareStatement("INSERT INTO Points"+mapLevel+" (pointId, minMz, maxMz, minRt, maxRt, int) VALUES (?,?,?,?,?,?);");

            return;
        }
        
        public void insertPoint(MsDataPoint point, int mapLevel) throws SQLException
        {
            // set values in prepared statement
            
            // set null for primary key, db autoincrements
            this.insertPointStatement.setNull(1, Types.INTEGER);
            
            // mz, rt and intensity bounds
            this.insertPointStatement.setDouble(2, point.mz);
            this.insertPointStatement.setDouble(3, point.mz);
            this.insertPointStatement.setDouble(4, point.rt);
            this.insertPointStatement.setDouble(5, point.rt);
            this.insertPointStatement.setDouble(6, point.intensity);

            // execute insert
            this.insertPointStatement.executeUpdate();

            return;
        }

        public ArrayList<MsDataPoint> selectPointFromDB(double mzMin, double mzMax, float rtMin, float rtMax, int mapLevel) throws SQLException
        {
            ResultSet results;
            PreparedStatement selectPointPrepStatement;
            
            selectPointPrepStatement = dbConnection.prepareStatement(this.selectPointInBoundsPre + mapLevel + this.selectPointInBoundsExt);
            selectPointPrepStatement.setDouble(1, mzMin);
            selectPointPrepStatement.setDouble(2, mzMax);
            selectPointPrepStatement.setFloat(3, rtMin);
            selectPointPrepStatement.setFloat(4, rtMax);
            results = selectPointPrepStatement.executeQuery();
                
            // flush result set to list of points
            ArrayList<MsDataPoint> points = new ArrayList<>();
            while(results.next())
            {
                // create new node and assign values
            	MsDataPoint point = new MsDataPoint(results.getInt(1),results.getDouble(2),results.getFloat(3),results.getDouble(4));

                // collect point
            	points.add(point);
            }
                
            // close prepared statement (also closes resultset)
            selectPointPrepStatement.close();
            
            return points;
        	
        }

		@Override
		public List<MsDataPoint> selectPointFromDBOne(double mzMin, double mzMax, float rtMin,
				float rtMax, int mapLevel) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setPartition() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public MsBounds getBaseInfomation() throws SQLException {

            ResultSet results;
            PreparedStatement selectPointPrepStatement;
            
            selectPointPrepStatement = dbConnection.prepareStatement("SELECT mzmin, mzmax, rtmin, rtmax, count FROM Base WHERE id = 1;");
            results = selectPointPrepStatement.executeQuery();
                
            // flush result set
            MsBounds msBounds = null;
            while(results.next())
            {
            	msBounds = new MsBounds(results.getDouble(1),results.getDouble(2),results.getFloat(3),results.getFloat(4),results.getInt(5));
            }
                
            // close prepared statement (also closes resultset)
            selectPointPrepStatement.close();
            return msBounds;
		}

		@Override
		public void setBaseInfomation(double[] bounds, int count, boolean isInsert) throws SQLException {

	        PreparedStatement insertBase; 
	        
			if(isInsert)
			{
				insertBase = dbConnection.prepareStatement("INSERT INTO Base (id, mzmin, mzmax, rtmin, rtmax, count) VALUES (1,"
						+ bounds[0] +","+ bounds[1] +","+ bounds[2] +","+ bounds[3] +","+ count +");");
			}
			else
			{
				insertBase = dbConnection.prepareStatement("UPDATE Base SET mzmin = " + bounds[0] + ", mzmax = " + bounds[1] + ", rtmin = " 
						+ bounds[2] + ", rtmax = " + bounds[3] + ", count = " + count + " WHERE id = 1;");
			}
			// execute insert
			insertBase.executeUpdate();
			insertBase.close();
			
		}

		@Override
		public List<Double> getDensities() throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}
       
    }

    /**
     * Inner class for preparing and executing normal SQL statements
     */
    private class SQLEngineDeRedundancy implements SQLEngineFacade{
        
        private static final int USER_VERSION = 3;
        
        public int maxLevel = 0;
        
        public boolean firstPartition = true;
        
        // ordered create infomation table statements
        public final String[] createMapsTableStatements = {
                "CREATE TABLE IF NOT EXISTS Maps (mapLevel INTEGER PRIMARY KEY, density DOUBLE NOT NULL);",
                "CREATE TABLE IF NOT EXISTS Base (id INTEGER PRIMARY KEY,count INTEGER, mzmin DOUBLE NOT NULL, mzmax DOUBLE NOT NULL, rtmin FLOAT NOT NULL, rtmax FLOAT NOT NULL);",//*
            };
        public final String[] createPointsTableStatements = {
                "CREATE TABLE IF NOT EXISTS Points? (pointId INTEGER PRIMARY KEY, mz DOUBLE NOT NULL, rt FLOAT NOT NULL, int DOUBLE);",
                "CREATE INDEX IF NOT EXISTS Points_index ON Points? (rt, mz);",
            };
        
        // insert statements 
        private final PreparedStatement insertMapsStatement; 
        private PreparedStatement insertPointStatement; 
        private List<List<PreparedStatement>> insertPointStatements; 

        // select statements
        private final String selectPointInBoundsPre = "SELECT pointId, mz, rt, int FROM Points";
        private final String selectPointInBoundsExt = " WHERE mz>? AND mz<? AND rt>? AND rt<?;";
        
        /**
         * Default constructor
         * Ensures that tables exist within database and creates prepared statements
         */
        public SQLEngineDeRedundancy() throws Exception
        {
			int appId;

            // check the application ID
            try (Statement checkAppIdStatement = dbConnection.createStatement()) {
                ResultSet appIdResult = checkAppIdStatement.executeQuery("PRAGMA application_id;");
                appIdResult.next();
                appId = appIdResult.getInt(1);
            }

            if (appId == 0) {
                // appId == 0 means it's not an mzMD or it's empty

                try (PreparedStatement checkEmpty = dbConnection.prepareStatement("SELECT count(*) FROM sqlite_master;")) {
                    ResultSet ers = checkEmpty.executeQuery();
                    ers.next();
                    int tables = ers.getInt(1);
                    if (tables != 0) {
                        throw new Exception("Not an mzMD file");
                    }
                }

                LOGGER.log(Level.INFO, "Creating a new mzMD file, version " + USER_VERSION);

                // initializing a new database with the current version
                try (Statement updateAppIdStatement = dbConnection.createStatement()) {
                    updateAppIdStatement.execute("PRAGMA application_id = " + APPLICATION_ID + ";");
                    updateAppIdStatement.execute("PRAGMA user_version = " + USER_VERSION + ";");
                }

                Statement statement = dbConnection.createStatement();
                for(String createTableStatement : this.createMapsTableStatements)
                    statement.execute(createTableStatement);
                statement.close();
                dbConnection.commit();

            } else if (appId != APPLICATION_ID) {
                throw new SQLException("Not an mzMD file.");
            }

            int userVersion;
            // check the user version for upgrades
            try (Statement userVersionStatement = dbConnection.createStatement()) {
                ResultSet userVersionResult = userVersionStatement.executeQuery("PRAGMA user_version;");
                userVersionResult.next();
                userVersion = userVersionResult.getInt(1);
            }

            // process version upgrades
            if (userVersion != USER_VERSION)
            {
                LOGGER.log(Level.INFO, "Converting mzMD file from version " + userVersion);

                // use switch fall-through (no "break" statement) to run multiple migrations
                // commented-out examples below
                switch(userVersion) {
                    case 0:
                        //convert_v0_v1();
                        break;
                    default:
                        throw new SQLException("Unsupported mzMD file version.");
                }

                try(Statement updateUserVersionStatement = dbConnection.createStatement()) {
                    updateUserVersionStatement.execute("PRAGMA user_version = " + USER_VERSION + ";");
                }

                LOGGER.log(Level.INFO, "mzTree file mzMD to version " + USER_VERSION);
            }
          
            // init insert statements 
            this.insertMapsStatement = dbConnection.prepareStatement("INSERT INTO Maps (mapLevel, density) VALUES (?,?);");

        }

        /**
         * Inserts an Point into the database
         * @param node MzTreeNode to insert
         * @param parentNodeID node's parentNodeID, 0 signals null parentNodeID (root node only)
         * @return ID of the node in the database
         * @throws SQLException
         */
        public void insertMap(int mapLevel, double density) throws SQLException
        {
            
            if(mapLevel>899 && insertPointStatements == null)
            {
            	
            	insertPointStatements = new ArrayList<List<PreparedStatement>>();
            	for(int mzPer = 0; mzPer < 10; mzPer++)
            	{
            		ArrayList<PreparedStatement> oneInsertPointStatements = new ArrayList<PreparedStatement>();
                	for(int rtPer = 0; rtPer < 10; rtPer++)
                	{
                		Statement statement = dbConnection.createStatement();
                        for(String createTableStatement : this.createPointsTableStatements)
                        {
                            statement.execute(createTableStatement.replaceAll("\\?", (mzPer+rtPer*10+900)+""));
                        }
                        statement.close();
                        dbConnection.commit();
                        
                		oneInsertPointStatements.add(dbConnection.prepareStatement("INSERT INTO Points"+(mzPer+rtPer*10+900)+" (pointId, mz, rt, int) VALUES (?,?,?,?);"));
                	}
                	insertPointStatements.add(oneInsertPointStatements);
                }
            }
            else 
            {
            	if(this.firstPartition)
            	{
	            	// set values in prepared statement
	
	                this.insertMapsStatement.setInt(1, mapLevel);
	                this.insertMapsStatement.setDouble(2, density);
	                
	                // execute insert
	                this.insertMapsStatement.executeUpdate();
            	}
                
                Statement statement = dbConnection.createStatement();
                for(String createTableStatement : this.createPointsTableStatements)
                {
                    statement.execute(createTableStatement.replaceAll("\\?", mapLevel+""));
                }
                statement.close();
                dbConnection.commit();
            	this.insertPointStatement = dbConnection.prepareStatement("INSERT INTO Points"+mapLevel+" (pointId, mz, rt, int) VALUES (?,?,?,?);");
            }
            
            if(this.maxLevel < mapLevel && mapLevel < 899)
            	this.maxLevel = mapLevel;
//            System.out.println(this.maxLevel);
            return;
        }
        
        public void insertPoint(MsDataPoint point, int mapLevel) throws SQLException
        {
            // set values in prepared statement
            if(!point.isInserted)
            {
            	PreparedStatement tmpInsertPointStatement = this.insertPointStatement;
            	if(mapLevel>899)
            	{
            		int mzPer = mapLevel%10;
            		int rtPer = mapLevel%100/10;
            		tmpInsertPointStatement = insertPointStatements.get(mzPer).get(rtPer);
            	}
	            // set null for primary key, db autoincrements
            	tmpInsertPointStatement.setNull(1, Types.INTEGER);
	            
	            // mz, rt and intensity bounds
            	tmpInsertPointStatement.setDouble(2, point.mz);
            	tmpInsertPointStatement.setDouble(3, point.rt);
            	tmpInsertPointStatement.setDouble(4, point.intensity);
	
	            // execute insert
            	tmpInsertPointStatement.executeUpdate();
	            point.isInserted = true;
            }
            return;
        }

        public ArrayList<MsDataPoint> selectPointFromDBOne(double mzMin, double mzMax, float rtMin, float rtMax, int mapLevel) throws SQLException
        {
            ResultSet results;
            PreparedStatement selectPointPrepStatement;
            
            selectPointPrepStatement = dbConnection.prepareStatement(this.selectPointInBoundsPre + mapLevel + this.selectPointInBoundsExt);
            selectPointPrepStatement.setDouble(1, mzMin);
            selectPointPrepStatement.setDouble(2, mzMax);
            selectPointPrepStatement.setFloat(3, rtMin);
            selectPointPrepStatement.setFloat(4, rtMax);
            results = selectPointPrepStatement.executeQuery();
                
            // flush result set to list of points
            ArrayList<MsDataPoint> points = new ArrayList<>();
            while(results.next())
            {
                // create new node and assign values
            	MsDataPoint point = new MsDataPoint(results.getInt(1),results.getDouble(2),results.getFloat(3),results.getDouble(4));

                // collect point
            	points.add(point);
            }
                
            // close prepared statement (also closes resultset)
            selectPointPrepStatement.close();
//            System.out.println("selectPointFromDBOne Level:"+mapLevel+", size:"+points.size());
            return points;
        	
        }
        public ArrayList<MsDataPoint> selectPointFromDB(double mzMin, double mzMax, float rtMin, float rtMax, int mapLevel) throws SQLException
        {
        	 // flush result set to list of points
            ArrayList<MsDataPoint> points = new ArrayList<>();
           
            for(int i = mapLevel; i < this.maxLevel+1; i++)
            {
            	points.addAll(selectPointFromDBOne(mzMin, mzMax, rtMin, rtMax, i));
            }
            return points;
        	
        }

		@Override
		public void setPartition() {
			// TODO Auto-generated method stub
			this.firstPartition = false;
		}

		@Override
		public MsBounds getBaseInfomation() throws SQLException {

            ResultSet results;
            PreparedStatement selectPointPrepStatement;
            
            selectPointPrepStatement = dbConnection.prepareStatement("SELECT mzmin, mzmax, rtmin, rtmax, count FROM Base WHERE id = 1;");
            results = selectPointPrepStatement.executeQuery();
                
            // flush result set
            MsBounds msBounds = null;
            while(results.next())
            {
            	msBounds = new MsBounds(results.getDouble(1),results.getDouble(2),results.getFloat(3),results.getFloat(4),results.getInt(5));
            }
                
            // close prepared statement (also closes resultset)
            selectPointPrepStatement.close();
            return msBounds;
		}

		@Override
		public void setBaseInfomation(double[] bounds, int count, boolean isInsert) throws SQLException {

	        PreparedStatement insertBase; 
	        
			if(isInsert)
			{
				insertBase = dbConnection.prepareStatement("INSERT INTO Base (id, mzmin, mzmax, rtmin, rtmax, count) VALUES (1,"
						+ bounds[0] +","+ bounds[1] +","+ bounds[2] +","+ bounds[3] +","+ count +");");
			}
			else
			{
				insertBase = dbConnection.prepareStatement("UPDATE Base SET mzmin = " + bounds[0] + ", mzmax = " + bounds[1] + ", rtmin = " 
						+ bounds[2] + ", rtmax = " + bounds[3] + ", count = " + count + " WHERE id = 1;");
			}
			// execute insert
			insertBase.executeUpdate();
			insertBase.close();
			
		}

		@Override
		public List<Double> getDensities() throws SQLException {

            ResultSet results;
            PreparedStatement selectDensities;
            
            selectDensities = dbConnection.prepareStatement("SELECT mapLevel, density FROM Maps;");
            results = selectDensities.executeQuery();
                
            // flush result set
            List<Double> densities = new ArrayList<Double>();
            int lastLevel = -1;
            while(results.next())
            {
            	if(results.getInt(1) > 0)
            	{
	            	if(lastLevel == -1 || lastLevel < results.getInt(1))
	            	{
	            		densities.add(results.getDouble(2));
	            	}
	            	else
	            	{
	            		densities.add(0, results.getDouble(2));
	            	}
            	}
            	lastLevel = results.getInt(1);
            }
                
            // close prepared statement (also closes resultset)
            selectDensities.close();
            return densities;
		}
       
    }

	@Override
	public void copy(Path targetFilepath) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Integer getPointCount() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}


    



	



}
