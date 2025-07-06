package com.saltlux.util.topicrank;

import java.util.HashMap;

public class StatInformation {

	class CoocurrencesTable {
		//<단어, {공기어1:GLOBAL_TF, 공기어2:GLOBAL_TF, 공기어3:GLOBAL_TF}>
		final public HashMap<String, DocVector> coocsFreq = new HashMap<String, DocVector>();

		public void addCoocurrence(String key1, String key2) {
			DocVector lastCooc = coocsFreq.get(key1);
			if (lastCooc == null)
				lastCooc = new DocVector();
			lastCooc.increment(key2);
			coocsFreq.put(key1, lastCooc);

			lastCooc = coocsFreq.get(key2);
			if (lastCooc == null)
				lastCooc = new DocVector();
			lastCooc.increment(key1);
			coocsFreq.put(key2, lastCooc);
		}

		public DocVector getCooccurrences(String feat) {
			return coocsFreq.get(feat);
		}

		public DocVector setCooccurrences(String feat, DocVector coocs) {
			return coocsFreq.put(feat, coocs);
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//<문서 ID, [단어1->TF, 단어2->TF] >
	private HashMap<String, DocVector> url2docVectLocal = new HashMap<String, DocVector>();
	
	public DocVector getDocVectLocal(String url) {
		return url2docVectLocal.get(url);
	}

	public void setDocVectLocal(String url, DocVector docs)
	{
		url2docVectLocal.put(url, docs);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////
	//<단어, [ 문서ID1->TF, 문서ID2->TF2,.... ]> 단어로 출현한 문서 식별자와 local TF 값을 관리한다.
	private HashMap<String, DocVector> docsOfFeat = new HashMap<String, DocVector>();

	
	public DocVector getDocsUrl(String feat) {
		return docsOfFeat.get(feat);
	}

	public void setDocsUrl(String feat, DocVector docs) {
		docsOfFeat.put(feat, docs);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////
	private DocVector tfTable = new DocVector(); 		//주의) Global TF=>즉 각 문서에서 TF들의 합
	private DocVector dfTable = new DocVector();			//DF
	
	public void incrementTF(String feat, double weight) {
		double oldw = tfTable.getWeight(feat);
		tfTable.setWeight(feat, oldw + weight);
	}

	public void incrementDF(String feat) {
		dfTable.increment(feat);
	}

	public DocVector getDFVect() {
		return dfTable;
	}

	public DocVector getTFVect() {
		return tfTable;
	}

	public double getDF(String feat) {
		return dfTable.getWeight(feat);
	}

	public double getTF(String feat) {
		return tfTable.getWeight(feat);
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////
	public CoocurrencesTable coocsTable = new CoocurrencesTable();

	public void addCoocurrence(String feat1, String feat2) {
		coocsTable.addCoocurrence(feat1, feat2);
	}
}
