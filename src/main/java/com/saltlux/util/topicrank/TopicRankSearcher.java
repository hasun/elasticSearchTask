package com.saltlux.util.topicrank;


abstract public class TopicRankSearcher {

	static {
		try {

		} catch (Exception e) {
			e.printStackTrace();
		}
		ClusteringEngine.initResource();		
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * request에서 토픽단어를 추출하여 반환
	 * @param request
	 * @return
	 */
	abstract protected String getRequestTopicTerm();
	abstract protected int getRequestLevel();
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	protected static class Documents{
		final long total_hits;
		final java.util.List<TopicRankDocument> docs;
		public Documents(final long total_hits, java.util.List<TopicRankDocument> docs) {
			this.total_hits=total_hits;
			this.docs		=docs;
		}
	};
	/**
	 * TopcRank 연산에 사용될 문서를 검색하여 반환한다.
	 * TopicRank 1.0, 2.0에서 호출됨
	 * @param request
	 * @return
	 * @throws Exception
	 */
	abstract protected Documents searchDocuments() throws Exception;
	
	/**
	 * TopicRank 2.0에서 호출됨
	 * 
	 * @param request
	 * @param queryField
	 * @param terms
	 * @param mx
	 * @return
	 * @throws Exception
	 */
	abstract protected long[] getDocFreq(final Feature[] terms, final int mx) throws Exception;
	
	/**
	 * TopicRank 2.0에서 호출됨
	 * 입력된 검색어와 AND 연산하여 DF를 구함
	 * 
	 * @param request
	 * @param queryField
	 * @param terms
	 * @param mx
	 * @return
	 * @throws Exception
	 */
	abstract protected long[] getCoodDF(Feature[] terms, int mx) throws Exception;
	
	/**
	 * TopicRank 2.0에서 호출됨
	 * Term들간의 AND 검색
	 * 
	 * @param request
	 * @param queryField
	 * @param topics
	 * @param mx
	 * @param total
	 * @return
	 * @throws Exception
	 */
	abstract protected long[] getCoOccurenceFreq(Feature[] topics, int mx, int total) throws Exception;
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	abstract protected boolean bIsStopWord(String term);
}
