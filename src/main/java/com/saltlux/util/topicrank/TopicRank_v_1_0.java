package com.saltlux.util.topicrank;

import java.util.*;

public class TopicRank_v_1_0 {
	final private TopicRankSearcher searcher;

	private int maxByNode = 15;
	private int maxDocByClust = 20;

	public void setMaxByNode(int maxByNode) {
		this.maxByNode = maxByNode;
	}
	public void setMaxDocByClust(int maxDocByClust) {
		this.maxDocByClust = maxDocByClust;
	}

	public TopicRank_v_1_0(TopicRankSearcher searcher) {
		this.searcher=searcher;
	}
	
	public TopicRankGraph search(int top_n_clusters) throws Exception {
		final int request_level=this.searcher.getRequestLevel();
		final String queryStr = this.searcher.getRequestTopicTerm();
		if ( Utils.isBlank(queryStr) ) throw new IllegalArgumentException(this.getClass().getName() +": query is empty!"); 
		final String topicTerm	=searcher.getRequestTopicTerm().replace(' ', '_'); //주의) tms_raw_stream_store에는 공백이 _로 되어 있다.
		
		//TopicRank 연산에 사용될 문서를 검색하여 가져온다.
		final TopicRankSearcher.Documents documents = this.searcher.searchDocuments();
	
		//TopicRank연산을 수행한다.<검색어, List<검색된 문서>>
		final LinkedHashMap<String, List<TopicRankDocument>> queriesDocsets = new LinkedHashMap<String, List<TopicRankDocument>>();
		queriesDocsets.put(topicTerm, documents.docs);	    

		final ClusteringEngine clustEngine = new ClusteringEngine(this.searcher, top_n_clusters);
		final HashMap<Feature, DocVector> result = clustEngine.getTopicRankOwlim(queriesDocsets, request_level, maxDocByClust);
		
		//처리한 결과를 전달한다.
		return getOwlimFormat(documents, topicTerm, result, request_level);
	}
	
	
	private TopicRankGraph getOwlimFormat(TopicRankSearcher.Documents documents, String topic, HashMap<Feature, DocVector> topics, int depth)
	{
		List<TopicRankGraph.Node> nodes=new ArrayList<TopicRankGraph.Node>();
		List<TopicRankGraph.Edge> edges=new ArrayList<TopicRankGraph.Edge>();

		int nodeNum = 1;

		//put the main topic in center
		nodes.add(new TopicRankGraph.Node(1, topic, 0));

		HashMap<String, DocVector> topicsString = new HashMap<String, DocVector>();
		LinkedHashMap<Feature, Integer> topicsToInt = new LinkedHashMap<Feature, Integer>();
		Iterator<Feature> featsIter = topics.keySet().iterator();
		DocVector temp = new DocVector();
		while(featsIter.hasNext())
		{
			Feature f = featsIter.next();
			temp.setWeight(f);
			topicsString.put(f._string, topics.get(f));
		}

		Feature[] orderedTopics = temp.sortByWeight();

		LinkedList<Feature> list = new LinkedList<Feature>();
		TreeSet<String> seenNodes = new TreeSet<String>();

		for(Feature psd : orderedTopics)
		{
			topicsToInt.put(psd, topicsToInt.size()+1);
			list.add(psd);
			seenNodes.add(psd._string);
		}

		int level1 = list.size()/4;
		int level2 = list.size()/2;

		//put 1 order topics
		Iterator<Feature> topicsIter = list.iterator();
		while(topicsIter.hasNext())
		{
			nodeNum++;
			Feature psd = topicsIter.next();
			int size = 0;
			int originalOrder = topicsToInt.get(psd);
			if(originalOrder<level1) size = 1;
			else if(originalOrder<level2) size = 2;
			else size=3;

			nodes.add(new TopicRankGraph.Node(nodeNum, psd._string, size, psd._d ));
			edges.add(new TopicRankGraph.Edge(1,nodeNum));

			if(depth==2)
			{
				Feature[] related = topicsString.get(psd._string).sortByWeight();
				int numNode1 = nodeNum;
				for(int i=0; i < Math.min(related.length, maxByNode); i++)
				{
					nodeNum++;
					psd = related[i];
					int subsize = 3;
					Integer subSize = topicsToInt.get(psd);
					if(subSize!=null)  subsize = subSize.intValue();

					nodes.add(new TopicRankGraph.Node(nodeNum, psd._string, subsize, psd._d ));

					//add links;
					edges.add(new TopicRankGraph.Edge(numNode1,nodeNum));
				}
			}
		}

		return new TopicRankGraph(documents.total_hits, nodes, edges);
	}
}
