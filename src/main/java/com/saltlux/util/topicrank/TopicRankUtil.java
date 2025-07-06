package com.saltlux.util.topicrank;

import com.google.gson.Gson;

import java.util.*;

public class TopicRankUtil {

	public TopicRankGraph getTopicRank(List<TopicRankDocument> topicRankDocument, int depth, String topicTerm, int maxByNode) {
		return getTopicRank(topicRankDocument, depth, topicTerm, 10, 5, maxByNode);
	}
	
	public TopicRankGraph getTopicRank(List<TopicRankDocument> topicRankDocument, int depth , String topicTerm, int topNClusters, int maxDocByClust, int maxByNode) {
		TopicRankGraph rst = null;;
		
		
		try {
			TopicRankSearcher.Documents documents = new TopicRankSearcher.Documents((long)topicRankDocument.size(), topicRankDocument);
		
			final LinkedHashMap<String, List<TopicRankDocument>> queriesDocsets = new LinkedHashMap<String, List<TopicRankDocument>>();
			queriesDocsets.put(topicTerm, documents.docs);	    
			
			final ClusteringEngine clustEngine = new ClusteringEngine(topNClusters);
			clustEngine.initResource();
			final HashMap<Feature, DocVector> result = clustEngine.getTopicRankOwlim(queriesDocsets, depth, maxDocByClust);
			
			//처리한 결과를 전달한다.
			rst =  getOwlimFormat(documents, topicTerm, result, depth, maxByNode);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return rst;
	}
	
	public String getTopicRankJson(List<TopicRankDocument> topicRankDocument, int depth, String topicTerm, int maxByNode) {
		
		TopicRankGraph topicRankGraph =  getTopicRank(topicRankDocument, depth, topicTerm, 10, 5, maxByNode);
		
		Gson gson = new Gson();
		gson.toJson(topicRankGraph);
		
		return gson.toJson(topicRankGraph);
	}
	

	private static TopicRankGraph getOwlimFormat(TopicRankSearcher.Documents documents, String topic, HashMap<Feature, DocVector> topics, int depth, int maxByNode)
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
