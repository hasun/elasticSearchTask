package com.saltlux.util.topicrank;

public class Topic implements Comparable<Topic> {
	public final String name;
	public final double weight;
	public final DocVector features;		//같이 나온 공기어 Term들=>유사도 계산에 사용한다.

	public Topic(String name, double weight, DocVector features) {
		this.name = name;
		this.weight = weight;
		this.features = features;
	}

	public int compareTo(Topic o) {

		if (weight < o.weight) return 1;
		if (weight > o.weight) return -1;
		return name.compareTo(o.name);
	}

	public String toString() {
		return name + " " + weight;
	}
}
