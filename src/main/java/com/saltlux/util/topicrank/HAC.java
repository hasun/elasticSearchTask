package com.saltlux.util.topicrank;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class HAC {

	public final int maxClustSize;				//= 7;
	public final boolean withTopicsDF;		// = false;

	public final int ITERATIONS;				// = 20;
	
	public final boolean local= false;
	
	public HAC(final int maxCluterSize, final boolean withTopicsDF) {
		this(maxCluterSize, withTopicsDF, 20);
	}
	
	public HAC(final int maxCluterSize, final int  ITERATIONS) {
		this(maxCluterSize, false, ITERATIONS);
	}

	HAC(final int maxCluterSize, boolean withTopicsDF,  final int  ITERATIONS) {
		this.maxClustSize=maxCluterSize;
		this.withTopicsDF=withTopicsDF;
		this.ITERATIONS	=ITERATIONS;
	}
	

	public static double dist(Topic a, Topic b) {
		DocVector dv1 = a.features;
		DocVector dv2 = b.features;

		int s1 = dv1.size();
		int s2 = dv2.size();

		if (s1 == 0 || s2 == 0) return 0;

		double sum = 0;
		for (String key : dv1.keySet()) {
			double tf1 = dv1.getWeight(key);
			double tf2 = dv2.getWeight(key);
			sum += tf1 * tf2;
		}

		return sum;

	}

	// IMPLEMENT HIERARCHICAL CLUSTERING ALGORITHM
	public List<List<Topic>> doTopicsClustering(List<Topic> topics) {
		List<List<Topic>> result = new ArrayList<List<Topic>>();

		if (topics.size() < 3) {
			result.add(topics);
			return result;
		}

		List<Cluster> clusters = new ArrayList<Cluster>();
		for (int i = 0; i < topics.size(); i++) {
			clusters.add(new Cluster(topics.get(i)));
		}

		boolean moreToMerge = true;

		for (int iteration = 0; iteration < ITERATIONS && moreToMerge; iteration++) {
			double[] near = new double[1];
			int[] closest = findmin(clusters, near);
			if (near[0] <= 0.00001)
				moreToMerge = false;

			Cluster c = new Cluster(clusters.get(closest[0]), clusters.get(closest[1]));

			if (clusters.size() > 0) clusters.remove(closest[1]);

			if (clusters.size() > 0) clusters.remove(closest[0]);

			clusters.add(c);
		}

		// result
		for (int i = 0; i < clusters.size(); i++) {
			Cluster c = clusters.get(i);
			List<Topic> els = c.points();
			Vector<Topic> cl = new Vector<Topic>();
			for (Topic t : els) {
				cl.add(t);
			}
			result.add(cl);
		}

		return result;
	}

	// IMPLEMENT HIERARCHICAL CLUSTERING ALGORITHM
	public List<List<Topic>> doClustering(List<Topic> topics) {
		List<List<Topic>> result = new ArrayList<List<Topic>>();

		if (topics.size() < 3) {
			result.add(topics);
			return result;
		}

		List<Cluster> clusters = new ArrayList<Cluster>();
		for (int i = 0; i < topics.size(); i++) {
			clusters.add(new Cluster(topics.get(i)));
		}

		boolean moreToMerge = true;
		for (int iteration = 0; iteration < ITERATIONS && moreToMerge; iteration++) {
			double[] near = new double[1];
			int[] closest = findmin(clusters, near);

			if (near[0] <= 0) moreToMerge = false;

			if (clusters.size() == 10) moreToMerge = false;

			Cluster c = new Cluster(clusters.get(closest[0]), clusters.get(closest[1]));

			if (clusters.size() > 0) clusters.remove(closest[1]);

			if (clusters.size() > 0) clusters.remove(closest[0]);

			clusters.add(c);
		}

		// result
		for (int i = 0; i < clusters.size(); i++) {
			Cluster c = clusters.get(i);
			List<Topic> els = c.points();
			Vector<Topic> cl = new Vector<Topic>();
			for (Topic t : els) {
				cl.add(t);
			}
			result.add(cl);
		}

		return result;
	}

	enum ClusterType {
		INTERNAL, TERMINAL
	};

	static class Cluster {
		ClusterType type;
		Cluster parent;
		Cluster child1, child2;
		Topic point;
		int count;

		Cluster(Cluster child1, Cluster child2) {
			this.type = ClusterType.INTERNAL;
			this.child1 = child1;
			this.child2 = child2;
			this.child1.parent = this;
			this.child2.parent = this;
			this.count = this.child1.count + this.child2.count;
		}

		Cluster(Topic point) {
			this.type = ClusterType.TERMINAL;
			this.point = point;
			this.count = 1;
		}

		List<Topic> points() {
			List<Topic> pts = new ArrayList<Topic>();
			points(pts);
			return pts;
		}

		void points(List<Topic> pts) {
			if (type == ClusterType.TERMINAL) {
				pts.add(point);
			} else if (type == ClusterType.INTERNAL) {
				child1.points(pts);
				child2.points(pts);
			}
		}

		public String toString() {
			if (point != null)
				return point.name;

			return "";
		}
	}

	static double similarityAvg(Cluster c1, Cluster c2) {
		List<Topic> pts1 = c1.points();
		List<Topic> pts2 = c2.points();

		double sum = 0;
		for (int i = 0; i < pts1.size(); i++) {
			Topic v1 = pts1.get(i);
			for (int j = 0; j < pts2.size(); j++) {
				Topic v2 = pts2.get(j);
				double d = dist(v1, v2);
				sum += d;
			}
		}
		return sum / (pts1.size() * pts2.size());
	}

	// neardis[0]: nearest distance
	int[] findmin(List<Cluster> clusters, double[] neardis) {
		double d = 0;
		int[] pair = new int[2];
		for (int i = 0; i < clusters.size(); i++) {
			for (int j = i + 1; j < clusters.size(); j++) {
				double e = 0;
				e = similarityAvg(clusters.get(i), clusters.get(j));

				if (e > d && 
					(clusters.get(i).points().size() + clusters.get(j).points().size()) <= maxClustSize) {
					d = e;
					pair[0] = i;
					pair[1] = j;
				}
			}
		}
		neardis[0] = d;
		return pair;
	}
}
