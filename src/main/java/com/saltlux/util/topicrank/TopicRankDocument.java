package com.saltlux.util.topicrank;

public class TopicRankDocument {

	private static final long serialVersionUID = -8296514071092330654L;
	
	final public String doc_id;			//고유 식별자
	final public String analysis; 	//TMS_RAW_STREAM 값
	
	public TopicRankDocument(String doc_id, String analysis) {
		this.doc_id	=doc_id;
		this.analysis	=analysis;
	}
}
