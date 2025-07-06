package com.saltlux.util.topicrank;

import java.util.ArrayList;
import java.util.List;

public class TopicRankExample {

	public static void main(String[] args) {

		TopicRankUtil tru = new TopicRankUtil();
		
		/* params 
			List <TopicRankDocument> topicRankDocument : dataset
			int depth : brench depth
			String topicTerm : root node term
			int maxByNode : 노드 수
		*/
		String trg = tru.getTopicRankJson(getTmsList(), 1, "", 10);
		System.out.println(trg);
		
		System.exit(0);
	}
	
	// dataset 생성
	// id, terms 형태로 생성
	private static List<TopicRankDocument> getTmsList() {
		
		List<TopicRankDocument> documents = new ArrayList<TopicRankDocument>();
		//TopicRankDocument document = new TopicRankDocument("id", "TMS_RAW_STREAM");
		
		return documents;
	}
}
