package com.saltlux.util.topicrank;

import java.util.*;

public class DocVector {

	private final HashMap<String, Double> featWeightList;
	private boolean isdirty = true;
	private Feature[] feature = null;
	
	public void clear() {
		featWeightList.clear();
	}

	public DocVector() {
		featWeightList = new HashMap<String, Double>();
	}

	public Iterator<String> iterator() {
		return featWeightList.keySet().iterator();
	}

	public int size() {
		return featWeightList.size();
	}

	public void multiply(double value) {
		for (String token : featWeightList.keySet()) {
			double w = featWeightList.get(token);
			double neww = w * value;
			setWeight(token, neww);
		}
	}

	public void add(DocVector thatDoc) {
		for (String token : thatDoc.keySet()) {
			double w = thatDoc.featWeightList.get(token);
			double old = getWeight(token);
			setWeight(token, old + w);
		}
	}

	public void setWeight(String feat, double weight) {
		isdirty = true;
		featWeightList.put(feat, weight);
	}

	public void setWeight(Feature psd) {
		isdirty = true;
		featWeightList.put(psd._string, psd._d);
	}

	public boolean exists(String feat) {
		return featWeightList.containsKey(feat);
	}

	public double getWeight(String feat) {
		Double weight = featWeightList.get(feat);
		if (weight == null) return 0;
		else return weight;
	}

	public Set<String> keySet() {
		return featWeightList.keySet();
	}

	public void increment(String feat) {
		double freq = getWeight(feat);
		setWeight(feat, freq + 1);
	}

	public void removeLessThanWeight(double d) {
		isdirty = true;
		Set<String> candi = new HashSet<String>();
		for(String key : featWeightList.keySet()) {
			double w = featWeightList.get(key);
			if( w < d) candi.add(key);
		}
		removeInList(candi);
	}

	public void removeMoreThanWeight(double d) {
		
		isdirty = true;
		Set<String> candi = new HashSet<String>();
		for(String key : featWeightList.keySet()) {
			double w = featWeightList.get(key);
			if(w >= d) candi.add(key);
		}
		removeInList(candi);
	}

	public void remove(String feat) {
		isdirty = true;
		featWeightList.remove(feat);
	}
	
	public void removeInList(Set<String> toRemove) {
		isdirty = true;
		for(String feat : toRemove) {
			featWeightList.remove(feat);
		}
	}
	
	public Feature[] sortByWeight() {
		
		if(isdirty || feature == null) {
			feature = new Feature[featWeightList.size()];
			int i = 0;
			for (String feat : keySet()) {
				feature[i++] = new Feature(feat, getWeight(feat));
			}
			Arrays.sort(feature, Feature.CompByWeight());
			isdirty = false;
		}
		return feature;
	}

	public double getSumWeight() {
		double norm1 = 0;
		for (String token : featWeightList.keySet()) {
			double w = featWeightList.get(token);
			norm1 += w;
		}
		return norm1;
	}

	public String toString() {
		StringBuilder buffer = new StringBuilder();

		Feature[] result = sortByWeight(); 
		buffer.append("DocVector(").append(result.length).append(")={");
		for (int i = 0; i < Math.min(50, result.length); i++) {
			String token = result[i]._string;
			double weight = result[i]._d;
			buffer.append(token).append(":").append(weight).append(", ");
		}
		buffer.append("}");

		return buffer.toString();
	}
}
