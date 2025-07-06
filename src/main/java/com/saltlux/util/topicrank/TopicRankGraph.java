package com.saltlux.util.topicrank;

import java.util.List;

public class TopicRankGraph {
	public static class Node {
		public final int    id;
		public final String name;
		
		//public final float depth; //정확한 수치가 아니어 보인다, 사용하지 말자
		
		public final double weight;
		
		public Node(int id, String name, float depth) {
			this.id=id;
			this.name=name;
			//this.depth=depth;
			this.weight=0.0;
		}
		
		public Node(int id, String name, float depth, double weight) {
			this.id=id;
			this.name=name;
			//this.depth=depth;
			this.weight=weight;
		}

		@Override
		public String toString() {
			return "Node [id=" + id + ", name=" + name + ", weight=" + weight + "]";
		}		
		
		
	}
	
	public static class Edge {
		public final int from;
		public final int to;
		public Edge(int from, int to) {
			this.from	=from;
			this.to		=to;
		}
		@Override
		public String toString() {
			return "Edge [from=" + from + ", to=" + to + "]";
		}
		
		
		
	}	

	////////////////////////////////////////////////////////////////////////////
	public final long total_hits;
	public final List<Node> nodes;
	public final List<Edge> edges;


	////////////////////////////////////////////////////////////////////////////
	public TopicRankGraph(long total_hits, List<Node> nodes, List<Edge> edges) {
		this.total_hits	=total_hits;
		this.nodes=nodes;
		this.edges=edges;
	}


	@Override
	public String toString() {
		return "TopicRankGraph [total_hits=" + total_hits + ", nodes=" + nodes.toString() + ", edges=" + edges.toString() + "]";
	}	
	
	
}
