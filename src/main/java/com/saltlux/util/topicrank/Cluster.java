package com.saltlux.util.topicrank;

import java.util.*;

public class Cluster implements Comparable<Cluster> {
	public double internSimilarity = 0;
	public DocVector keywords = null;
	public DocVector documents = null;
	public DocVector centroid = null;
	public int id = 0;
	public int parentID = 0;

	public List<Cluster> children = null;

	public Cluster() {
		keywords 	= new DocVector();
		documents 	= new DocVector();
		centroid 		= new DocVector();
	}

	public void ReplaceKeyword(Map<String, String> replaceDic)
	{
		Set<String> removeTerm = new HashSet<String>();
		ArrayList<Feature> newTermList = new ArrayList<Feature>();
		
		for(String term : keywords.keySet()) {
			String newTerm  = replaceDic.get(term);
			if(newTerm != null) {
				double weight = keywords.getWeight(term);
				removeTerm.add(term);
				newTermList.add(new Feature(newTerm, weight));
			}
		}
		
		keywords.removeInList(removeTerm);
		for(int i = 0 ; i < newTermList.size() ;i++) {
			keywords.setWeight(newTermList.get(i));
		}
	}
	
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(keywords.toString());
		if(children != null) {
			buf.append('[');
			buf.append(children.toString());
			buf.append(']');
		}
		return buf.toString();
	}

	public int compareTo(Cluster o) {
		if (this.documents.size() > o.documents.size())
			return -1;

		if (this.documents.size() < o.documents.size())
			return 1;

		if (this.internSimilarity > o.internSimilarity)
			return -1;

		if (this.internSimilarity < o.internSimilarity)
			return 1;

		return 0;
	}

}
