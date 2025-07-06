package com.saltlux.util.topicrank;

import java.io.Serializable;
import java.util.Comparator;

public class Feature implements Serializable, Comparable<Feature> {
	public String _string;
	public double _d;

	public Feature(String s, double aDouble) {
		_string = s;
		_d = aDouble;
	}

	public int compareTo(Feature obj) {
		Feature tmp = (Feature) obj;
		if (_d < tmp._d) {
			return -1;
		}
		else if (_d > tmp._d) {
			return 1;
		}
		else {
			return _string.compareTo(tmp._string);
		}
	}

	@Override
	public boolean equals(Object obj) {
		Feature tmp = (Feature) obj;
		if (_d == tmp._d) {
			if (_string.compareTo(tmp._string) == 0) {
				return true;
			}
		}
		return false;
	}

	public String toString() {
		return _string + ":" + _d;
	}
	
	public static Comparator<Feature> CompByString()
	{
		return new PairStringDoubleCompByString();
	}
	
	public static Comparator<Feature> CompByWeight()
	{
		return new PairStringDoubleCompByWeight();
	}
	
	private static class PairStringDoubleCompByString implements Comparator<Feature>
	{
	    public int compare(Feature psd1, Feature psd2)
	    {
	    	return psd1._string.compareTo(psd2._string);
	    }
	}
	
	private static class PairStringDoubleCompByWeight implements Comparator<Feature>
	{
	    public int compare(Feature psd1, Feature psd2)
	    {
	    	if(psd1._d<psd2._d) return 1;
	    	if(psd1._d>psd2._d) return -1;
	    	else return psd1._string.compareTo(psd2._string);
	    }
	}
}
