package edu.msViz.mzMD;

public class MsBounds
{
	// minimum mz at this map
	private double mzMin;
    
    // maximum mz at this map
	private double mzMax;

    // minimum rt at this map
	private double rtMin;
    
    // maximum rt at this map
	private double rtMax;
	
	private int count;

	public MsBounds() 
	{
		this.mzMin = -1;
		this.mzMax = -1;
		this.rtMin = -1;
		this.rtMax = -1;
		this.count = 0;
	}
	public MsBounds(double mzMin, double mzMax, double rtMin, double rtMax, int count) 
	{
		this.mzMin = mzMin;
		this.mzMax = mzMax;
		this.rtMin = rtMin;
		this.rtMax = rtMax;
		this.count = count;
	}

	public void update(double mz, double rt)
	{
		if(this.mzMin < 0 || this.mzMin > mz)
		{
			this.mzMin = mz;
		}
		if(this.mzMax < mz)
		{
			this.mzMax = mz;
		}
		if(this.rtMin < 0 || this.rtMin > rt)
		{
			this.rtMin = rt;
		}
		if(this.rtMax < rt)
		{
			this.rtMax = rt;
		}
	}

	public void update(double mzMin, double mzMax, double rtMin, double rtMax)
	{
		if(this.mzMin < 0 || this.mzMin > mzMin)
		{
			this.mzMin = mzMin;
		}
		if(this.mzMax < mzMax)
		{
			this.mzMax = mzMax;
		}
		if(this.rtMin < 0 || this.rtMin > rtMin)
		{
			this.rtMin = rtMin;
		}
		if(this.rtMax < rtMax)
		{
			this.rtMax = rtMax;
		}
	}

	public void update(MsBounds newms)
	{
		double[] newbounds = newms.get();
		if(this.mzMin < 0 || this.mzMin > newbounds[0])
		{
			this.mzMin = newbounds[0];
		}
		if(this.mzMax < newbounds[1])
		{
			this.mzMax = newbounds[1];
		}
		if(this.rtMin < 0 || this.rtMin > newbounds[2])
		{
			this.rtMin = newbounds[2];
		}
		if(this.rtMax < newbounds[3])
		{
			this.rtMax = newbounds[3];
		}
		this.count = this.count + newms.count;
	}
	
	public void set(double mzMin, double mzMax, double rtMin, double rtMax)
	{
		this.mzMin = mzMin;
		this.mzMax = mzMax;
		this.rtMin = rtMin;
		this.rtMax = rtMax;
	}
	
	public double[] get()
	{
		return new double[] {this.mzMin, this.mzMax, this.rtMin, this.rtMax};
	}
	
	public double area()
	{
		return (this.mzMax-this.mzMin)*(this.rtMax-this.rtMin);
	}
	
	public String toString()
	{
		return "[" + this.mzMin +","+ this.mzMax +","+ this.rtMin +","+ this.rtMax+"]";
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
}
