package edu.msViz.test;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.msViz.mzMD.MsDataPoint;
import edu.msViz.mzMD.MzMD;

public class transTest {
	private static final Logger LOGGER = Logger.getLogger(transTest.class.getName());
	private MzMD mzMD;
	public transTest()
    {
    }
	
	public long loadPoints(List<MsDataPoint> points, String filePath) {
	
	    // disconnect and drop the previous mzMD
	    closemzMD();
	
	    this.mzMD = new MzMD();
	
	    // process mzMD on new thread so that UI thread remains responsive
	    return loadmzMD(points, filePath);
	}

    
	private long loadmzMD(List<MsDataPoint> points, String filePath) {
	    long start = System.currentTimeMillis();
	    long spend = 0;
	    try
	    {
	        // attempt to create mzMD
	    	this.mzMD.buildTreeFromRootWithPath(points, Paths.get(filePath));
	        spend = (System.currentTimeMillis() - start);
	    }
	    catch (Exception ex)
	    {
	        this.mzMD = null;

            LOGGER.log(Level.WARNING, "Could not open requested file", ex);
	    }
	    return spend;
	}
	
	private void closemzMD() {
	    if (this.mzMD != null) {
	    	this.mzMD.close();
	    	this.mzMD = null;
	    }
	}
	
	// 递归方式 计算文件的大小
    private long getTotalSizeOfFilesInDir(File file) {
        if (file.isFile())
            return file.length();
        File[] children = file.listFiles();
        long total = 0;
        if (children != null)
            for (File child : children)
                total += getTotalSizeOfFilesInDir(child);
        return total;
    }
    public void deleteFile(File file){
        //判断文件不为null或文件目录存在
        if (file == null || !file.exists()){
            System.out.println("文件删除失败,请检查文件路径是否正确");
            return;
        }
        //取得这个目录下的所有子文件对象
        File[] files = file.listFiles();
        //遍历该目录下的文件对象
        for (File f: files){
            //打印文件名
            String name = file.getName();
            System.out.println(name);
            //判断子目录是否存在子目录,如果是文件则删除
            if (f.isDirectory()){
                deleteFile(f);
            }else {
                f.delete();
            }
        }
        //删除空文件夹  for循环已经把上一层节点的目录清空。
        file.delete();
    }
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String openFilePath = "/Users/yangrunmin/Study/data/CPTAC_Intact_rep8_15Jan15_Bane_C2-14-08-02RZ.mzML";
		Simulation importSim = new Simulation("/Users/yangrunmin/Study/data/simtrans.csv");
		String storeFilesPath = "/Users/yangrunmin/Study/data/result";
		String storeFilePath = storeFilesPath + "/result.mzMD";
		List<double[]> importBounds = importSim.loadCSV();
	    testMain tm = new testMain();
		long spend = tm.openFile(openFilePath);
		TransInfo testInfo = new TransInfo(openFilePath+"-Trans");
		for(int i=0; i < importBounds.size(); i++)
		{
			List<MsDataPoint> points = tm.mzMD.query(importBounds.get(i)[0],importBounds.get(i)[1], (float)importBounds.get(i)[2], (float)importBounds.get(i)[3], 0, 0);
			transTest tt = new transTest();
			long tspend = tt.loadPoints(points, storeFilePath);
			long total = tt.getTotalSizeOfFilesInDir(new File(storeFilesPath));
			tt.deleteFile(new File(storeFilesPath));
			
			System.out.println(points.size()+","+tspend+"ms,"+total+"byte");
			testInfo.addInfo(points.size(),tspend,total);
		}
		testInfo.export(spend);
	}

}
