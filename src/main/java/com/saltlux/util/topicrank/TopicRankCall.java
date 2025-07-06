package com.saltlux.util.topicrank;

//import com.saltlux.dor.api.IN2StdSearcher;
//import com.saltlux.dor.api.common.query.IN2Query;
//import com.saltlux.util.topicRank.TopicRankSearcher.Documents;
//import org.apache.commons.lang.StringEscapeUtils;

import java.util.*;

public class TopicRankCall {

	private static int maxByNode = 1;
	private static int maxDocByClust = 1;

	public void setMaxByNode(int maxByNode) {
		this.maxByNode = maxByNode;
	}
	public void setMaxDocByClust(int maxDocByClust) {
		this.maxDocByClust = maxDocByClust;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("start");
		
//		getTopicRank(1, "", "", 1);
		
//		getTopicRankByUtil();
//		System.out.println("done!");
		
		System.exit(0);
	}
	
//	private static boolean getTopicRankByUtil() {
//		TopicRankUtil tru = new TopicRankUtil();
////		TopicRankGraph trg = tru.getTopicRank(getTmsList(), 1, "", 10);
//		String trg = tru.getTopicRankJson(getTmsList(), 1, "", 10);
//
//		System.out.println(trg.toString());
//
//		return true;
//	}
//
//	private static boolean getTopicRank(int requestLevel, String queryStr, String topicTerm, int topNClusters) {
//		boolean rst = false;
//
//
//		try {
//			List<TopicRankDocument> topicRankDocument = getTmsList();
//			Documents documents = new Documents((long)topicRankDocument.size(), topicRankDocument);
//
//			final LinkedHashMap<String, List<TopicRankDocument>> queriesDocsets = new LinkedHashMap<String, List<TopicRankDocument>>();
//			queriesDocsets.put(topicTerm, documents.docs);
//
//			final ClusteringEngine clustEngine = new ClusteringEngine(topNClusters);
//			clustEngine.initResource();
//			final HashMap<Feature, DocVector> result = clustEngine.getTopicRankOwlim(queriesDocsets, requestLevel, maxDocByClust);
//
//			//처리한 결과를 전달한다.
//			TopicRankGraph graph =  getOwlimFormat(documents, topicTerm, result, requestLevel);
//			System.out.println(graph);
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//
//		return rst;
//	}
//
//	private static List<TopicRankDocument> getTmsList() {
//		boolean rst = false;
//		List<TopicRankDocument> documents = new ArrayList<TopicRankDocument>();
//		try {
//
//
//
//			IN2StdSearcher searcher = new IN2StdSearcher();
//
//			searcher.setServer("127.0.0.1", 10000);
//			searcher.newQuery();
//
//			searcher.addIndex("buzz_250613");
//			searcher.setQuery(IN2Query.MatchingAllDocQuery());
//
//			searcher.addReturnField(new String[] {"_id", "TMS_RAW_STREAM"});
//
//			searcher.setReturnPositionCount(0, 700);
//
//			if(searcher.searchDocument()) {
//				for(int i = 0; i < searcher.getDocumentCount(); i++) {
//					documents.add(new TopicRankDocument(searcher.getValueInDocument(i, "_id"), StringEscapeUtils.unescapeJava(searcher.getValueInDocument(i, "TMS_RAW_STREAM"))));
//				}
//			} else {
//				System.out.println(searcher.getLastErrorMessage());
//			}
//
//
//			rst = true;
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//
//		return documents;
//	}
//
//	private static TopicRankGraph getOwlimFormat(TopicRankSearcher.Documents documents, String topic, HashMap<Feature, DocVector> topics, int depth)
//	{
//		List<TopicRankGraph.Node> nodes=new ArrayList<TopicRankGraph.Node>();
//		List<TopicRankGraph.Edge> edges=new ArrayList<TopicRankGraph.Edge>();
//
//		int nodeNum = 1;
//
//		//put the main topic in center
//		nodes.add(new TopicRankGraph.Node(1, topic, 0));
//
//		HashMap<String, DocVector> topicsString = new HashMap<String, DocVector>();
//		LinkedHashMap<Feature, Integer> topicsToInt = new LinkedHashMap<Feature, Integer>();
//		Iterator<Feature> featsIter = topics.keySet().iterator();
//		DocVector temp = new DocVector();
//		while(featsIter.hasNext())
//		{
//			Feature f = featsIter.next();
//			temp.setWeight(f);
//			topicsString.put(f._string, topics.get(f));
//		}
//
//		Feature[] orderedTopics = temp.sortByWeight();
//
//		LinkedList<Feature> list = new LinkedList<Feature>();
//		TreeSet<String> seenNodes = new TreeSet<String>();
//
//		for(Feature psd : orderedTopics)
//		{
//			topicsToInt.put(psd, topicsToInt.size()+1);
//			list.add(psd);
//			seenNodes.add(psd._string);
//		}
//
//		int level1 = list.size()/4;
//		int level2 = list.size()/2;
//
//		//put 1 order topics
//		Iterator<Feature> topicsIter = list.iterator();
//		while(topicsIter.hasNext())
//		{
//			nodeNum++;
//			Feature psd = topicsIter.next();
//			int size = 0;
//			int originalOrder = topicsToInt.get(psd);
//			if(originalOrder<level1) size = 1;
//			else if(originalOrder<level2) size = 2;
//			else size=3;
//
//			nodes.add(new TopicRankGraph.Node(nodeNum, psd._string, size, psd._d ));
//			edges.add(new TopicRankGraph.Edge(1,nodeNum));
//
//			if(depth==2)
//			{
//				Feature[] related = topicsString.get(psd._string).sortByWeight();
//				int numNode1 = nodeNum;
//				for(int i=0; i < Math.min(related.length, maxByNode); i++)
//				{
//					nodeNum++;
//					psd = related[i];
//					int subsize = 3;
//					Integer subSize = topicsToInt.get(psd);
//					if(subSize!=null)  subsize = subSize.intValue();
//
//					nodes.add(new TopicRankGraph.Node(nodeNum, psd._string, subsize, psd._d ));
//
//					//add links;
//					edges.add(new TopicRankGraph.Edge(numNode1,nodeNum));
//				}
//			}
//		}
//
//		return new TopicRankGraph(documents.total_hits, nodes, edges);
//	}
}
