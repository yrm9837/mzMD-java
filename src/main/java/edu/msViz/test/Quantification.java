package edu.msViz.test;

import java.util.ArrayList;
import java.util.List;

import edu.msViz.mzMD.Configure;
import edu.msViz.mzMD.MsDataPoint;

public class Quantification {

	public double coverage;
	public double efficiency;
	
	public Quantification()
	{
		this.coverage = 0;
		this.efficiency = 0;
	}

	public void compare(List<MsDataPoint> points, List<MsDataPoint> sumPoints, int number, double[] bounds)
	{
		switch(Configure.QuantifyTypeChoice){
        default:
        case Standard:
            compareStandard(points, sumPoints, number, bounds);
        case Ratio:
        	compareRatio(points, sumPoints, number, bounds);
		}
	}

	public void compareStandard(List<MsDataPoint> points, List<MsDataPoint> sumPoints, int number, double[] bounds)
	{
		this.coverage = 0;
		this.efficiency = 0;
		int bestNumber = 0;
		int hitNumber = 0;
		double mzMin = bounds[0];
		double mzMax = bounds[1];
		double rtMin = bounds[2];
		double rtMax = bounds[3];
		int divide= (int) Math.ceil(Math.sqrt(number));
		double mzStep = (mzMax - mzMin)/divide;
		double rtStep = (rtMax - rtMin)/divide;
		int[][] bestPoints = new int[divide][divide];
		for(int i = 0; i < divide; i++)
		{
			for(int j = 0; j < divide; j++)
			{
				bestPoints[i][j] = -1;
			}
		}
		for(int i = 0; i < points.size(); i++)
		{
			MsDataPoint point = points.get(i);
			int mzIndex = (int) Math.floor((point.mz - mzMin)/mzStep);
			if(mzIndex == divide)
			{
				mzIndex--;
			}
			int rtIndex = (int) Math.floor((point.rt - rtMin)/rtStep);
			if(rtIndex == divide)
			{
				rtIndex--;
			}
//			System.out.println(divide+","+divide+"|"+mzIndex+","+rtIndex);
			if(bestPoints[mzIndex][rtIndex] == -1)
			{
				bestPoints[mzIndex][rtIndex] = i;
				bestNumber++;
			}else
			{
				if(point.intensity > points.get(bestPoints[mzIndex][rtIndex]).intensity)
				{
					bestPoints[mzIndex][rtIndex] = i;
				}
			}
		}
		for(int i = 0; i < sumPoints.size(); i++)
		{
			MsDataPoint point = sumPoints.get(i);
			int mzIndex = (int) Math.floor((point.mz - mzMin)/mzStep);
			if(mzIndex == divide)
			{
				mzIndex--;
			}
			int rtIndex = (int) Math.floor((point.rt - rtMin)/rtStep);
			if(rtIndex == divide)
			{
				rtIndex--;
			}
			if(points.get(bestPoints[mzIndex][rtIndex]).equals(point))
			{
				hitNumber++;
			}
		}
//		System.out.println("hitNumber:"+hitNumber+",bestNumber:"+bestNumber+",sumPoints.size():"+sumPoints.size());
		coverage = Double.valueOf(hitNumber)/Double.valueOf(bestNumber);
		efficiency = Double.valueOf(hitNumber)/Double.valueOf(sumPoints.size());
	}
	
	public void compareRatio(List<MsDataPoint> points, List<MsDataPoint> sumPoints, int number, double[] bounds)
	{
		this.coverage = 0;
		this.efficiency = 0;
		int bestNumber = 0;
		double hitScore = 0;
		int sunNumber = sumPoints.size();
		double mzMin = bounds[0];
		double mzMax = bounds[1];
		double rtMin = bounds[2];
		double rtMax = bounds[3];
		int divide= (int) Math.ceil(Math.sqrt(number));
		double mzStep = (mzMax - mzMin)/divide;
		double rtStep = (rtMax - rtMin)/divide;
		int[][] bestPoints = new int[divide][divide];
		for(int i = 0; i < divide; i++)
		{
			for(int j = 0; j < divide; j++)
			{
				bestPoints[i][j] = -1;
			}
		}
		for(int i = 0; i < points.size(); i++)
		{
			MsDataPoint point = points.get(i);
			int mzIndex = (int) Math.floor((point.mz - mzMin)/mzStep);
			if(mzIndex == divide)
			{
				mzIndex--;
			}
			int rtIndex = (int) Math.floor((point.rt - rtMin)/rtStep);
			if(rtIndex == divide)
			{
				rtIndex--;
			}
			if(bestPoints[mzIndex][rtIndex] == -1)
			{
				bestPoints[mzIndex][rtIndex] = i;
				bestNumber++;
			}else
			{
				if(point.intensity > points.get(bestPoints[mzIndex][rtIndex]).intensity)
				{
					bestPoints[mzIndex][rtIndex] = i;
				}
			}
		}
		sumPoints = this.getBestSum(sumPoints, number, bounds);
		for(int i = 0; i < sumPoints.size(); i++)
		{
			MsDataPoint point = sumPoints.get(i);
			int mzIndex = (int) Math.floor((point.mz - mzMin)/mzStep);
			if(mzIndex == divide)
			{
				mzIndex--;
			}
			int rtIndex = (int) Math.floor((point.rt - rtMin)/rtStep);
			if(rtIndex == divide)
			{
				rtIndex--;
			}
			hitScore = hitScore + point.intensity/points.get(bestPoints[mzIndex][rtIndex]).intensity;
			if(points.get(bestPoints[mzIndex][rtIndex]).equals(point))
			{
			}
		}
//		System.out.println("hitNumber:"+hitNumber+",bestNumber:"+bestNumber+",sumPoints.size():"+sumPoints.size());
		coverage = Double.valueOf(hitScore)/Double.valueOf(bestNumber);
		efficiency = Double.valueOf(hitScore)/Double.valueOf(sunNumber);
	}

	public List<MsDataPoint> getBestSum(List<MsDataPoint> points, int number, double[] bounds)
	{
		double mzMin = bounds[0];
		double mzMax = bounds[1];
		double rtMin = bounds[2];
		double rtMax = bounds[3];
		int divide= (int) Math.ceil(Math.sqrt(number));
		double mzStep = (mzMax - mzMin)/divide;
		double rtStep = (rtMax - rtMin)/divide;
		int[][] bestPoints = new int[divide][divide];
		for(int i = 0; i < divide; i++)
		{
			for(int j = 0; j < divide; j++)
			{
				bestPoints[i][j] = -1;
			}
		}
		for(int i = 0; i < points.size(); i++)
		{
			MsDataPoint point = points.get(i);
			int mzIndex = (int) Math.floor((point.mz - mzMin)/mzStep);
			if(mzIndex == divide)
			{
				mzIndex--;
			}
			int rtIndex = (int) Math.floor((point.rt - rtMin)/rtStep);
			if(rtIndex == divide)
			{
				rtIndex--;
			}
			if(bestPoints[mzIndex][rtIndex] == -1)
			{
				bestPoints[mzIndex][rtIndex] = i;
			}else
			{
				if(point.intensity > points.get(bestPoints[mzIndex][rtIndex]).intensity)
				{
					bestPoints[mzIndex][rtIndex] = i;
				}
			}
		}
		List<MsDataPoint> bestSumPoints = new ArrayList<>();
		for(int i = 0; i < divide; i++)
		{
			for(int j = 0; j < divide; j++)
			{
				if(bestPoints[i][j] > -1)
				{
					bestSumPoints.add(points.get(bestPoints[i][j]));
				}
			}
		}
		return bestSumPoints;
	}
	
	public List<MsDataPoint> getBestSum(List<MsDataPoint> points, int RTnumber, int MZnumber, double[] bounds)
	{
		double mzMin = bounds[0];
		double mzMax = bounds[1];
		double rtMin = bounds[2];
		double rtMax = bounds[3];
		double mzStep = (mzMax - mzMin)/MZnumber;
		double rtStep = (rtMax - rtMin)/RTnumber;
		int[][] bestPoints = new int[MZnumber][RTnumber];
		for(int i = 0; i < MZnumber; i++)
		{
			for(int j = 0; j < RTnumber; j++)
			{
				bestPoints[i][j] = -1;
			}
		}
		for(int i = 0; i < points.size(); i++)
		{
			MsDataPoint point = points.get(i);
			int mzIndex = (int) Math.floor((point.mz - mzMin)/mzStep);
			if(mzIndex == MZnumber)
			{
				mzIndex--;
			}
			int rtIndex = (int) Math.floor((point.rt - rtMin)/rtStep);
			if(rtIndex == RTnumber)
			{
				rtIndex--;
			}
			if(bestPoints[mzIndex][rtIndex] == -1)
			{
				bestPoints[mzIndex][rtIndex] = i;
			}else
			{
				if(point.intensity > points.get(bestPoints[mzIndex][rtIndex]).intensity)
				{
					bestPoints[mzIndex][rtIndex] = i;
				}
			}
		}
		List<MsDataPoint> bestSumPoints = new ArrayList<>();
		for(int i = 0; i < MZnumber; i++)
		{
			for(int j = 0; j < RTnumber; j++)
			{
				if(bestPoints[i][j] > -1)
				{
					bestSumPoints.add(points.get(bestPoints[i][j]));
				}
			}
		}
		return bestSumPoints;
	}
}
