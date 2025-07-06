package com.saltlux.util.topicrank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ClusteringEngine {
	private static final Logger logger = LoggerFactory.getLogger(ClusteringEngine.class);

	enum ClusterMethod{
		HAC,			//HAC 만 사용되고 있네.....
	}
	
	private final int nbSnippetsLocal = 5; 				//검색된 문서 한개에서 질의어로 검색된 주변 영역들중, 앞에서 부터 n개 까지만 사용
	private final int winsizeLocal = 7;					//검색된 질의어의 앞과 뒤로 청크 단위로 가져올 영역 범위를 지정
	private final int nbSnippetsGlobal = 10; 
	private final int winsizeGlobal = 10;
	private final int maxFeats = 60;						//분석에 사용할 단어를 가장 점수가 높은것 부터 N개를 추출한다
	private final int maxSizeClusterLevel1 = 7;
	private final int maxClusters = 15;					//TopicRank 1.0에서 클러스터링 된 순위에서 반환할 갯수를 지정한다.
	
	private final boolean withSub = true;
	private final boolean withTopicsDF = false;
	private final boolean local = true;

	//불용어 목록 (topicrank/stopword_kor.txt, topicrank/userstopword.txt)
	private static TreeSet<String> stopwords;
	
	//등록된 명사가 포함된 문구들만 토픽 후보로 사용하려고 하나.(topicrank/ncdic.txt)
	private static TreeSet<String> ncDic;															

	//특정 단어에 가중치를 더추기 위한 사전(topicrank/corekeyword.txt)
	private static HashMap<String, Double> coreKeyword;
	
	//특정 단어와 연관이 없는 단어목록들(topicrank/unrelation.txt)
	private static HashMap<String, TreeSet<String>> unwantedwordsByKeywords;
	
	//특정 단어를 등록된 단어로 교체하기 위한 사전(topicrank/replace.txt)
	private static HashMap<String, String> replaceDic;
	
	//항상 빈값으로 초기화 되고 있다.
	private static UserStopWordPattern patternStopwordChecker;
	
	//NE_TOPICRANK에 사용될 Tag 값이었나 , 항상 빈값으로 초기화 되고 있다.
	private static ArrayList<String> NeTagInstance;											
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static final Pattern numPattern = Pattern.compile("([0-9]+(\\.|,|-|/)*([0-9]+)*(\\.|,|-|/)*(년|개|일|명|월|평|반|시|분|초|층|번지|통|편|달|리|칸)*)|[A-Z]|[a-z]");
	private static final Pattern eng = Pattern.compile("[a-z]+");
	private static final Pattern toLower = Pattern.compile("서울|미국|일본|한국");
	private static final Pattern date = Pattern.compile("[0-9]+[년|월|일|시|분|초|말]|이틀째|지난|초|새벽|지난번|오후|최근|오전|낮|저녁|밤|현지시간|시기|처음");
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//	private final TopicRankSearcher  searcher;
	private final int top_n_clusters;

	public ClusteringEngine(TopicRankSearcher searcher) {
//		this.searcher=searcher;
		this.top_n_clusters=maxClusters;
	}
	
	public ClusteringEngine(TopicRankSearcher searcher, int top_n_clusters) {
//		this.searcher=searcher;
		this.top_n_clusters=top_n_clusters;
	}
	
	public ClusteringEngine(int top_n_clusters) {
//		this.searcher=searcher;
		this.top_n_clusters=top_n_clusters;
	}

	static private Distance<DocVector> JACCARD_DISTANCEDV = new Distance<DocVector>() {
    	
    	public double distance(DocVector dv1, DocVector dv2)
    	{
    		int s1= dv1.size();
    		int s2= dv2.size();
    		
    		if(s1==0 || s2==0) return 0;
    		
    		int common = 0;
    		for(String key : dv1.keySet()) {
    			if(dv2.keySet().contains(key)) common++;
    		}

    		return ((double)(common*common)/(double)(s1*s2));
    	}
    };

	public static void initResource()
	{
		patternStopwordChecker = new UserStopWordPattern();
		NeTagInstance = new ArrayList<String>();
		stopwords = new TreeSet<String>();
		coreKeyword = new HashMap<String, Double>();
		unwantedwordsByKeywords = new HashMap<String, TreeSet<String>>();
		ncDic = new TreeSet<String>();
		replaceDic = new HashMap<String, String>();
	}
	
	/**
	 * checking if a given keyword is stopword or not routine
	 * @return true/false
	 */
	private boolean bIsStopword(String keyword)
	{
		//불용어 사전 사용 하지 않음
		return true;
		/*
		if ( this.searcher.bIsStopWord(keyword) ) return true; //외부에서 지정한 불용어 사전 검색
		
//		System.out.println("STOPWORD : " + keyword);
		if(keyword == null || keyword.length() < 2) return true;
		if(patternStopwordChecker != null && patternStopwordChecker.isPatternStopWord(keyword)) return true;
 		if(numPattern.matcher(keyword).matches() || date.matcher(keyword).find()) return true;
		return stopwords.contains(keyword.toLowerCase());
		*/
	}

	private void setWeight(DocVector dv, String term, boolean isFirst)
	{
		double weight = dv.getWeight(term);
		if(weight > 0) {
			if(isFirst) {
				int df = (int)(weight/10000);
				double w = weight - (df * 10000);
				weight = ((df+1) * 10000) + (w+1);
				dv.setWeight(term, weight);
			}
			else {
				weight = weight + 1;
				dv.setWeight(term, weight);
			}
		}
		else {
			weight = 1 * 10000 + 1;
			dv.setWeight(term, weight);
		}
	}
	
	private void calWeight(DocVector dv, int maxDoc)
	{
		HashSet<String> removeDf = new HashSet<String>();
		Iterator<String> dfIter = dv.iterator();
		while(dfIter.hasNext()) {
			String f = dfIter.next();
			double weight = dv.getWeight(f);
			int df = (int)(weight/10000);
			double w = weight - (df * 10000);
			dv.setWeight(f, w);

			double dfratio = (double)df/(double)maxDoc;
			if(maxDoc > 100 && dfratio > 0.95) {
				removeDf.add(f);
				System.out.println(f + "df = " + df + "maxdoc = " + maxDoc);
			}
		}
		dv.removeInList(removeDf);
		
	}
	
	public Feature[] getTopNFeature(String query, List<TopicRankDocument> documents) {
		
		DocVector totalDv = new DocVector();
		for(TopicRankDocument doc : documents) {
			String raw_stream = doc.analysis;
			HashSet<String> DfCheck = new HashSet<String>();
			StringTokenizer token = new StringTokenizer(raw_stream, " \n");
			int l;
			String pos;
			while(token.hasMoreTokens()) {
				String term = token.nextToken();
				
				if((l = term.indexOf('/')) > -1) {
					String tterm = term.substring(0, l);
					pos = term.substring(l);
					term = tterm;
				}
				else {
					pos = "";
				}
				if( term.length() > 0 && term.length() < 34 && 
						!bIsStopword(term)&&  
						!ncDic.contains(term) && 
						!eng.matcher(term).matches()) {
					
					if((coreKeyword.containsKey(term) ||  
							term.indexOf("_")>-1 || 
							(term.indexOf("_")<0 && !pos.equals("NC")  && !pos.equals("UW") && !(pos.indexOf("NN")>-1)))) {
						totalDv.increment(term);
						setWeight(totalDv, term, !DfCheck.contains(term));
						DfCheck.add(term);
					}
				}
			}
		}
		
		if(query != null) {
			calWeight(totalDv, documents.size());
			HashSet<String> queryWords = analyseQuery(query);
			HashSet<String> synomymsQuery = new HashSet<String>();
			for(String q:queryWords) {
				List<String> synonyms = SimilarNounDetector.searchEngKor(q);
				synomymsQuery.addAll(synonyms);
			}
	
			TreeSet<String> unwantedkeys = new TreeSet<String>();
			Iterator<String> qIter = queryWords.iterator();
			while(qIter.hasNext()) {
				TreeSet<String> unwantedForQ = unwantedwordsByKeywords.get(qIter.next());
				if(unwantedForQ!=null) unwantedkeys.addAll(unwantedForQ);
			}
			qIter = synomymsQuery.iterator();
			while(qIter.hasNext()) {
				TreeSet<String> unwantedForQ = unwantedwordsByKeywords.get(qIter.next());
				if(unwantedForQ!=null) unwantedkeys.addAll(unwantedForQ);
			}
			
			totalDv.removeInList(queryWords);
			totalDv.removeInList(synomymsQuery);
			totalDv.removeInList(unwantedkeys);
		}
		
		HashMap<String, Set<String>> synoSets =  SimilarNounDetector.groupSynomyms(totalDv);
		for(String s:synoSets.keySet()) {
			Set<String> simSet = synoSets.get(s);
			for(String simKey:simSet) {
				totalDv.remove(simKey);
			}
		}
		
		Iterator<String> dfIter = totalDv.iterator();
		while(dfIter.hasNext()) {
			String f = dfIter.next();
			double w = totalDv.getWeight(f);
			Double d = coreKeyword.get(f.toLowerCase());
			if(d != null) {	
				w = w * d;
				totalDv.setWeight(f, w);
			}
		}
		
		HashMap<String, List<String>> similarsIncluded = SimilarNounDetector.removeSimilarRules(totalDv);
		for(String s:similarsIncluded.keySet()) {
			List<String> simSet = similarsIncluded.get(s);
			double oldValue = totalDv.getWeight(s);

			for(String simKey:simSet) {
				oldValue += totalDv.getWeight(simKey);
				totalDv.remove(simKey);
			}
			totalDv.setWeight(s, oldValue);
		}

		SimilarNounDetector.removeSameSoundHex(totalDv);
		SimilarNounDetector.removeSame(totalDv);
		
		Feature[] fe = totalDv.sortByWeight();

		for(int i = 0 ; i < fe.length ; i++) {
			String key = fe[i]._string;
			String rep = replaceDic.get(key);
			if(rep != null) fe[i]._string = rep;
		}
		
		return fe;
		
	}
	
	public Feature[] getSubjects(String query, ArrayList<TopicRankDocument> documents)
	{
		logger.debug("getSubjects for: {}", query);

		//parameter for Features Selection
		int minDF = 2;

		//1. Query Analysis
		HashSet<String> queryWords = analyseQuery(query);
		
		HashSet<String> synomymsQuery = new HashSet<String>();
		for(String q:queryWords) {
			List<String> synonyms = SimilarNounDetector.searchEngKor(q);
			synomymsQuery.addAll(synonyms);
		}

		logger.debug("Found synomymsQuery: {}", synomymsQuery);

		//2. Features Extraction
		logger.debug("Start Features Extraction");		
		
		long startFE = System.currentTimeMillis();
		
		StatInformation statInf = extractFeatures(queryWords, documents, nbSnippetsGlobal, winsizeGlobal);
		
		logger.debug("Features Extraction done in: {} [ms]", (System.currentTimeMillis()-startFE) );		

		//3. choose minimum df function of number of documents
		if(documents.size() >=200) minDF = 3;
		else if(documents.size() >=100) minDF = 2;
		else minDF = 1;
		
		logger.debug("minDF is {}", minDF );		
		
		//3. Features Selection
		DocVector featuresSelected = featuresSelection(statInf, minDF, queryWords, synomymsQuery);
		Feature[] orderedFeatsSelected = featuresSelected.sortByWeight();

		for(int i = 0 ; i < orderedFeatsSelected.length ; i++) {
			String key = orderedFeatsSelected[i]._string;
			String rep = replaceDic.get(key);
			if(rep != null) orderedFeatsSelected[i]._string = rep;
		}
		
		return orderedFeatsSelected;
	}

	public Feature[] getTRCandidates(String query, List<TopicRankDocument> documents)
	{
		logger.debug("getTRCandidates for: {}", query );		
		
		//1. Query Analysis
		HashSet<String> queryWords = analyseQuery(query);
		HashSet<String> synomymsQuery = new HashSet<String>();
		
		for(String q:queryWords) {
			List<String> synonyms = SimilarNounDetector.searchEngKor(q);
			synomymsQuery.addAll(synonyms);
		}

		logger.debug("Found synomymsQuery: {}", synomymsQuery);		

		//2. Features Extraction
		logger.debug("Start Features Extraction");		
		
		long startFE = System.currentTimeMillis();
		System.out.println("queryWords : " + queryWords);
		StatInformation statInf = extractFeaturesForTRCandidates(queryWords, documents, nbSnippetsGlobal, winsizeGlobal);
		logger.debug("Features Extraction done in: {}[ms]", (System.currentTimeMillis()-startFE));		

		//3. Features Selection
		DocVector featuresSelected = featuresSelection(statInf, -1, queryWords, synomymsQuery);
		Feature[] orderedFeatsSelected = featuresSelected.sortByWeight();

		return orderedFeatsSelected;
	}

	private DocVector featuresSelection(StatInformation statInf, int minDF, HashSet<String> queryWords, HashSet<String> synomymsQuery)
	{
		final DocVector df = statInf.getDFVect();

		TreeSet<String> unwantedkeys = new TreeSet<String>();
		Iterator<String> qIter = queryWords.iterator();
		while(qIter.hasNext()) {
			TreeSet<String> unwantedForQ = unwantedwordsByKeywords.get(qIter.next());
			if(unwantedForQ!=null) unwantedkeys.addAll(unwantedForQ);
		}
		qIter = synomymsQuery.iterator();
		while(qIter.hasNext()) {
			TreeSet<String> unwantedForQ = unwantedwordsByKeywords.get(qIter.next());
			if(unwantedForQ!=null) unwantedkeys.addAll(unwantedForQ);
		}

		df.removeInList(unwantedkeys);

		final DocVector TF = statInf.getTFVect();
		final long startFS = System.currentTimeMillis();

		HashMap<String, Set<String>> synoSets =  SimilarNounDetector.groupSynomyms(df);
		for(String s:synoSets.keySet()) {
			Set<String> simSet = synoSets.get(s);
			for(String simKey:simSet) {
				df.remove(simKey);
				
				if(minDF != -1) {
					//here normalizing coocs / docs
					DocVector coocsSim = statInf.coocsTable.getCooccurrences(simKey);
					DocVector coocs = statInf.coocsTable.getCooccurrences(s);
					coocs.add(coocsSim);
					statInf.coocsTable.setCooccurrences(s, coocs);
				}
				
				DocVector docsSim = statInf.getDocsUrl(simKey);
				DocVector docs = statInf.getDocsUrl(s);
				docs.add(docsSim);
				statInf.setDocsUrl(s, docs);
				TF.setWeight(s, statInf.getTF(s)+statInf.getTF(simKey));
			}
		}

		Iterator<String> dfIter = df.iterator();
		while(dfIter.hasNext()) {
			String f = dfIter.next();
			double w = statInf.getDocsUrl(f).size();
			Double d = coreKeyword.get(f.toLowerCase());
			if(d != null) {	w = w * d; }
			
			df.setWeight(f, w);
		}

		if(minDF != -1) df.removeLessThanWeight(minDF);

		HashMap<String, List<String>> similarsIncluded = SimilarNounDetector.removeSimilarRules(df);
		for(String s:similarsIncluded.keySet()) {
			List<String> simSet = similarsIncluded.get(s);
			double oldValue = df.getWeight(s);

			for(String simKey:simSet) {
				oldValue += df.getWeight(simKey);
				df.remove(simKey);
				//here normalizing coocs / docs
				DocVector coocsSim = statInf.coocsTable.getCooccurrences(simKey);
				DocVector coocs = statInf.coocsTable.getCooccurrences(s);
				if(coocs!=null && coocsSim!=null) {
					coocs.add(coocsSim);
					statInf.coocsTable.setCooccurrences(s, coocs);
				}
				DocVector docsSim = statInf.getDocsUrl(simKey);
				DocVector docs = statInf.getDocsUrl(s);
				docs.add(docsSim);
				statInf.setDocsUrl(s, docs);
			}
			df.setWeight(s, oldValue);
		}

		SimilarNounDetector.removeSameSoundHex(df);
		SimilarNounDetector.removeSame(df);
		
		final Feature[] featsDF = df.sortByWeight();
		
		final DocVector harmonicMean = new DocVector();

		for(int i=0; i<featsDF.length; i++) {

			String feat = featsDF[i]._string;
			double score = featsDF[i]._d;
			
			if(minDF == -1 || score >= minDF) {
				if(!synomymsQuery.contains(feat) && !queryWords.contains(feat)) {
					boolean foundSimilar = false;
					Iterator<String> sIter = queryWords.iterator();
					while(sIter.hasNext() && !foundSimilar) {
						String qKey = sIter.next();
						if(qKey.equalsIgnoreCase(feat)) {
							foundSimilar = true;
						}

						DocVector dv = new DocVector();
						dv.setWeight(feat, 1);
						dv.setWeight(qKey, 1);
						HashMap<String, List<String>> result = SimilarNounDetector.removeSimilarRules(dv);
						if(result.size() == 1) foundSimilar = true;
					}

					if(!foundSimilar) {
						double hm = 2/(2/score+1/TF.getWeight(feat));
						harmonicMean.setWeight(feat, hm);
					}
				}
			}
		}

		final DocVector feats = new DocVector();
		final Feature[] orderHMeanFeats = harmonicMean.sortByWeight();
		for(int i=0; i<orderHMeanFeats.length; i++) {
			String feat = orderHMeanFeats[i]._string;
			double weight = orderHMeanFeats[i]._d;

			Iterator<String> subTermsQueryIter = queryWords.iterator();
			while(subTermsQueryIter.hasNext()) {
				String subTermsQuery = subTermsQueryIter.next();
				if(feat.indexOf(subTermsQuery)>-1 || toLower.matcher(feat).find()) {
					weight/=100;
					break;
				}
			}

			feats.setWeight(feat, weight);
		}

		logger.debug("Features Extraction done in: {}[ms]", (System.currentTimeMillis()-startFS));				
		return feats;
	}

	private StatInformation extractFeatures(HashSet<String> queryWords, List<TopicRankDocument> documents, int nbSnippets, int winsize)
	{
		return extractFeatureList(queryWords, documents, nbSnippets, winsize, 0);
	}
	
	private StatInformation extractFeaturesForTRCandidates(HashSet<String> queryWords, List<TopicRankDocument> documents, int nbSnippets, int winsize)
	{
		return extractFeatureList(queryWords, documents, nbSnippets, winsize, 1);
	}
	
	private StatInformation extractFeatureList(HashSet<String> queryWords, List<TopicRankDocument> documents, int nbSnippets, int winsize, int Case)
	{
		final StatInformation statInf = new StatInformation();

		StringBuilder queryPatten = new StringBuilder();
		for(String query:queryWords) {
			if(queryPatten.length() > 0) queryPatten.append('|');
			queryPatten.append(query);
		}
		
		//KAY_COMMENT 문자열 인데, Pattern으로 찾을 필요가 있는가?
		final Pattern pattern = Pattern.compile(queryPatten.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		for(int i=0; i < documents.size(); i++) { //문서 단위로 처리
			
			final TopicRankDocument doc = documents.get(i);
			
			//01.검색어 주변으로 지정한 청킹 단위(window)로 이동하면서, 가장 긴 청킹을 찾아서 최대 n개(nbSnippets)만큼을 추출한다.
			//결과 List=[ [첫번째 매칭에서 찾은 단어들], [두번째 매칭에서 찾은 단어들], [세번째 매칭에서 찾은 단어들] ] 
			final ArrayList<ArrayList<String>> nBestFeatures = getSnippetsLocal(doc.analysis, pattern, nbSnippets, winsize);
			
			//System.out.println(document.analysis);
			//System.out.println(nBestFeatures);
			//System.out.println("==========================================");

			//02. 검색어 주변으로 지정한 청킹 단위(window)로 이동하면서, 가장 긴 청킹을 찾아서 최대 n개(nbSnippets)만큼을 추출한다.
			final DocVector tfLabels = new DocVector(); //Local TF(문서내에서 단어 빈도수) 계산용
			if(Case == 0) _addCoocurrence(nBestFeatures, statInf, tfLabels);
			else _addCoocurrenceTR(nBestFeatures, statInf, tfLabels);	//TopicRank 2.0에서 호출 될 때

			for(String feat : tfLabels.keySet()) {
				statInf.incrementTF(feat, tfLabels.getWeight(feat));		//Global DF를 집계한다.
				statInf.incrementDF(feat);									//Global TF를 집계한다.
				
				DocVector docsOfFeat = statInf.getDocsUrl(feat);		
				if(docsOfFeat==null) docsOfFeat = new DocVector();
				docsOfFeat.setWeight(doc.doc_id/*문서 식별자*/, tfLabels.getWeight(feat));
				statInf.setDocsUrl(feat/*단어*/, docsOfFeat);  //<단어, [ 문서ID1->TF, 문서ID2->TF2,.... ]> 단어로 출현한 문서 식별자와 local TF 값을 구성
			}

			statInf.setDocVectLocal(doc.doc_id, tfLabels);
		}

		return statInf;
	}
	
	private ArrayList<ArrayList<String>> getSnippetsLocal(String s_paragraph, Pattern pattern, int maxSnippets, int winsize)
	{
		final ArrayList<ArrayList<String>> snipLabelsList = new ArrayList<ArrayList<String>>(10);
		
		if(s_paragraph==null) return snipLabelsList;
		
		final int maxDocDepth = 50000; //문서의 앞에서 부터 지정된 길이 이하에서만 발견된 단어를 추가하도록 한다.

		final int pLength = s_paragraph.length(); //입력 텍스트 전체 길이를 지정
		
		final Matcher matcher = pattern.matcher(s_paragraph);
//		System.out.println("s_paragraph : " + s_paragraph);
		
		int nbMatches = 0;
		int startingPos = 0;
//		System.out.println("startingPos : " + startingPos);
		while( (nbMatches < maxSnippets) && (startingPos>=0) &&(startingPos < maxDocDepth) && matcher.find(startingPos)) {
			
			nbMatches++;
			final int startMatch = matcher.start(); 	//검색어와 일치하는 시작 위치를 지정
			final int endMatch = matcher.end();		//검색어와 일치하는 끝 위치를 지정
			
			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
			//01. 검색된 단어를 기준으로 Windows 크기 만큼(한줄=청킹 단위)로 이전과 이후의 시작과 종료 위치를 찾는다.
			// KAY_COMMENT 영역이 겹치는 경우, 같은 단어가 여러번 추가될 것으로 보여 개선이 필요해 보임
			// =>관련 단어 추출 다음 단계(TF를 계산하는 로직)에서 여러번 계산될 것으로 추정(?)됨
			int nbKeysPrev = 0; 
			int startIndex = startMatch;
			for(int l=1; l<pLength && nbKeysPrev<=winsize;l++) {
				int idx = startMatch-l;
				if(idx>=0) {
					if(s_paragraph.charAt(idx)=='\n') {
						nbKeysPrev++;
						startIndex = idx;
					}
				}
			}

			int nbKeysPost = 0;
			int endIndex = endMatch;
			for(int l=1; l<pLength && nbKeysPost<=winsize;l++) {
				int idx = endMatch+l;
				if(idx <pLength) {
					if(s_paragraph.charAt(idx)=='\n') { //맨 마지막 청킹은 '\n'이 없으므로 누락 될 수 있는 버그 존재
						nbKeysPost++;
						endIndex = idx;
					}
				}
			}

			//System.out.println(String.format("start match : %d, end match: %d=>%s", startMatch, endMatch, s_paragraph.substring(startMatch, endMatch)));
			//System.out.println(String.format("start indexes : %d, end indexes: %d=>%s", startIndex, endIndex, s_paragraph.substring(startIndex, endIndex)));

			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
			//02. 검색된 주변 영역을 청킹 단위로 탐색하면서, 가장 긴 청킹만을 추출한다.
			final ArrayList<String> snipLabels = new  ArrayList<String>(100);
			final String text = s_paragraph.substring(startIndex, endIndex);   //검색된 단어 주변(windows size) 영역
			final int len = text.length();			

			int startLine = 0;
			for(int i=0;i<len;i++) {
				if(text.charAt(i)=='\n') {
					final String line = text.substring(startLine, i).trim();
					String longest = "", longestPOS = "";
					
					final StringTokenizer st = new StringTokenizer(line, " ");
					while(st.hasMoreTokens()) { 
						String chunk = st.nextToken();
						String pos = "";
						int pospos = chunk.indexOf("/");
						if(pospos>-1) {
							pos = chunk.substring(pospos+1);
							chunk  = chunk.substring(0,pospos);
						}

						if(chunk.length() >= longest.length()) {
							if(pos.length()>longestPOS.length()) {
								longestPOS = pos;
							}
							longest = chunk;
						}
					}
					
					//MUST CHANGE
					if( longest.length() > 0 && longest.length() < 34 ) {
						
						if((coreKeyword.containsKey(longest) ||  
								(line.indexOf(" ")!=line.lastIndexOf(" ")) || 
								longest.indexOf("_")>-1 || 
								(longest.indexOf("_")<0 && !longestPOS.equals("NC")  && !longestPOS.equals("UW") && !(longestPOS.indexOf("NN")>-1)))) {
							snipLabels.add(longest);
						}
					}

					startLine = i+1;
				}
			}
			snipLabelsList.add(snipLabels);
//			System.out.println(String.format("start match- : %d, end match-: %d", startMatch, endMatch));
//			System.out.println("s_paragraph : " + s_paragraph);
			startingPos = s_paragraph.indexOf("\n", endMatch);
//			System.out.println("startingPos : " + startingPos);
		}
		
		return snipLabelsList ;
	}
	
	private void _addCoocurrence(ArrayList<ArrayList<String>> nBestFeatures, StatInformation statInf, DocVector tfLabels)
	{
		for(int sn=0; sn<nBestFeatures.size(); sn++) {
			
			ArrayList<String> nbests = nBestFeatures.get(sn);
			for(String nbest1:nbests) {
			
				tfLabels.increment(nbest1);
				
				for(String nbest2:nbests) { //KAY_COMMENT 동일한 단어가 빈도수가 두번되는데 점수 계산에 상관 없을까.
					statInf.addCoocurrence(nbest1, nbest2); 
				
					if(withSub) { //복합 단어를 분할 하여 추가하기 위한 목장
						String[] simple1 = nbest1.split("_");
						String[] simple2 = nbest2.split("_");
						for(String s1:simple1) { //KAY_COMMENT 분할이 안되면 단어가 서로 또한번 추가가 되는데 역시 점수 계산은?
							for(String s2:simple2) { //KAY_COMMENT 동일한 단어가 빈도수가 두번되는데 점수 계산에 상관 없을까.
								statInf.addCoocurrence(s1, s2); 
							}
						}
					}
				}
			}
		}
	}
	
	private void _addCoocurrenceTR(ArrayList<ArrayList<String>> nBestFeatures, StatInformation statInf, DocVector tfLabels)
	{
		for(int sn=0; sn<nBestFeatures.size(); sn++) {
			ArrayList<String> nbests = nBestFeatures.get(sn);

			for(String nbest:nbests) {
				tfLabels.increment(nbest);
			}
		}
	}

	public HashMap<Feature, DocVector> getTopicRankOwlim(LinkedHashMap<String, List<TopicRankDocument>> queriesDocsets, 
			int depth, int maxDocByClust)
	{
		return getTopicRankOwlim(queriesDocsets, depth, ClusterMethod.HAC, maxDocByClust);
	}
	
	public HashMap<Feature, DocVector> getTopicRankOwlim(LinkedHashMap<String, List<TopicRankDocument>> queriesDocsets, 
																				int depth, final ClusterMethod clustMethod, int maxDocsByCluster)
	{
		final List<Cluster> combine =  new ArrayList<Cluster>();
		Iterator<String> queriesIter = queriesDocsets.keySet().iterator();
		while(queriesIter.hasNext()) {
			String query = queriesIter.next();
			List<Cluster> d0 = getDocumentsClustersWithQuery(query, queriesDocsets.get(query), depth, clustMethod, maxDocsByCluster);
			combine.addAll(d0);
		}

		HashMap<Feature, DocVector>  tops = getTopClustersGraph(combine);

		return tops;
	}
	
	private HashMap<Feature, DocVector> getTopClustersGraph(List<Cluster> clusters)
	{
		HashMap<Feature, DocVector> topClustersGraph = new HashMap<Feature, DocVector>();

		for(int i=0; i<clusters.size(); i++) {
			Cluster cluster = clusters.get(i);

			DocVector dv = cluster.keywords;
			Feature[] f = dv.sortByWeight();
			Feature label = f[0];
			DocVector dvAfter = new DocVector();
			for(int j=1; j<f.length; j++) {
				dvAfter.setWeight(f[j]);
			}

			if(cluster.children!=null && cluster.children.size()>0) {
				List<Cluster> subClusters = cluster.children;
				for(int j=0;j<subClusters.size();j++) {
					Cluster subCluster = subClusters.get(j);
					dvAfter.add(subCluster.keywords);
				}
			}

			topClustersGraph.put(label, dvAfter);
		}
		return topClustersGraph;
	}
	
	private HashSet<String> analyseQuery(String query)
	{
		HashSet<String> queryWords = new HashSet<String>();
		List<String> single_queries = new ArrayList<String>();
		List<String> queries = new ArrayList<String>();
		
		StringTokenizer qst = new StringTokenizer(query);
		while(qst.hasMoreTokens()) {
			String queryTerm = qst.nextToken().replaceAll("[\\[\\](){}:.^]", "");
			queryWords.add(queryTerm);
			single_queries.add(queryTerm);
		}

		for(int i=0; i<queryWords.size()-2;i++) {
			String trigram = (single_queries.get(i)+"_"+ single_queries.get(i+1)+"_"+ single_queries.get(i+2));
			queries.add(trigram);
		}

		for(int i=0; i<queryWords.size()-1;i++) {
			String bigram = (single_queries.get(i)+"_"+ single_queries.get(i+1));
			queries.add(bigram);
		}

		for(String addQuery:queries) {
			queryWords.add(addQuery);
		}

		return queryWords;
	}

	public List<Cluster> getDocumentsClustersWithQuery(String query, ArrayList<TopicRankDocument> documents, int level, int maxDocsByClust)
	{
		return getDocumentsClustersWithQuery(query, documents, level, ClusterMethod.HAC, maxDocsByClust);
	}
	
	private List<List<Topic>> CreateTopicWithHACandLocal(List<Topic>  topicsSet, HashSet<String> queryWords, DocVector featuresSelected, StatInformation statInf)
	{
		Feature[] orderedFeatsSelected = featuresSelected.sortByWeight();
		
		int mx = Math.min(maxFeats, orderedFeatsSelected.length);

		for(int i = 0 ; i < mx ; i++) {
			Feature psd = orderedFeatsSelected[i];
			
			//for each feature, associate the vector of documents it appears
			//유사도 계산을하기 위해 같이 나온 공기어 Term들을 추가한다.
			DocVector coocs = statInf.coocsTable.getCooccurrences(psd._string);

			if(coocs!=null) {
				coocs.removeInList(queryWords);
				Topic t = new Topic(psd._string, psd._d, coocs);
				topicsSet.add(t);
			}
		}
		
		List<List<Topic>> wordsCluster = new ArrayList<List<Topic>>();

		if(topicsSet.size() > 1) {
			wordsCluster = new HAC(maxSizeClusterLevel1, withTopicsDF).doClustering(topicsSet);
		}
		
		return wordsCluster;
	}
	
	private List<List<Topic>> CreateTopicWithHACandNotLocal(List<Topic>  topicsSet, HashSet<String> queryWords, DocVector featuresSelected, StatInformation statInf)
	{
		
		Feature[] orderedFeatsSelected = featuresSelected.sortByWeight();
		
		HashMap<String, Double> freqTopic = new HashMap<String, Double>();
		HashMap<String, Double> cofreqTopics = new HashMap<String, Double>();
		
		int mx = Math.min(maxFeats, orderedFeatsSelected.length);
		
		for(int i = 0 ; i < mx ; i++) {
			
			Feature psd = orderedFeatsSelected[i];

			double topicFreq = 0;
			
			if(freqTopic.containsKey(psd._string)) {
				topicFreq = freqTopic.get(psd._string);
			}
			else {
				freqTopic.put(psd._string, topicFreq);
			}
			
			DocVector coocs = new DocVector();

			for(int j = 0 ; j < mx ; j++) {
				Feature topic = orderedFeatsSelected[j];
				double coocFr = 0;
				String coocFeat = "";

				if(psd._string.compareTo(topic._string)<0) coocFeat = psd._string+"|"+topic._string;
				else coocFeat = topic._string +"|"+psd._string;

				if(cofreqTopics.containsKey(coocFeat)) {
					coocFr = cofreqTopics.get(coocFeat);
				}
				else {
					cofreqTopics.put(coocFeat, coocFr);
				}

				if(coocFr > 0) {
					double topic2Freq = 0;
					if(!freqTopic.containsKey(topic._string)) {
						topic2Freq = freqTopic.get(topic._string);
					}
					else { 
						topic2Freq = 0;
						freqTopic.put(topic._string, topic2Freq);
					}
					coocs.setWeight(topic._string, coocFr/topic2Freq * coocFr/topicFreq);
				}
			}

			if(coocs!=null) {
				coocs.removeInList(queryWords);
				Topic t = new Topic(psd._string, psd._d, coocs);
				topicsSet.add(t);
			}
		}
		
		List<List<Topic>> wordsCluster = new ArrayList<List<Topic>>();

		logger.debug("TRY HERE NEW HAC IMPLEMENTATION");				

		if(topicsSet.size() > 1) {
			wordsCluster = new HAC(maxSizeClusterLevel1, withTopicsDF).doClustering(topicsSet);
		}
		
		return wordsCluster;
	}
	
	private List<List<Topic>> CreateTopicWithoutHAC(List<Topic>  topicsSet, HashSet<String> queryWords, DocVector featuresSelected, 
			StatInformation statInf, int minDF)
	{

		Feature[] orderedFeatsSelected = featuresSelected.sortByWeight();
		HashMap<String, DocVector> topicString2Features = new HashMap<String, DocVector>();
		HashMap<String, Topic> topicString2Topic = new HashMap<String, Topic>();
		
		int mx = Math.min(maxFeats, orderedFeatsSelected.length);
		for(int i = 0; i < mx ; i++) {
			Feature psd = orderedFeatsSelected[i];
			// For each feature, associate the vector of co-ocurrent words
			DocVector coocs = statInf.coocsTable.getCooccurrences(psd._string);
			if(coocs!=null) {
				coocs.removeLessThanWeight(minDF-1);
				coocs.removeInList(queryWords);
				Topic t = new Topic(psd._string, psd._d, coocs);
				topicsSet.add(t);
				topicString2Features.put(psd._string, coocs);
				topicString2Topic.put(psd._string, t);
			}
		}
		
		List<List<Topic>> wordsCluster = new ArrayList<List<Topic>>();
		List<List<String>> wordsClusterString = new ArrayList<List<String>>();

		TreeSet<String> seensSeed = new TreeSet<String>();
		for(Topic t:topicsSet) {
			if(!seensSeed.contains(t.name)) {
				Feature[]  features = t.features.sortByWeight();
				List<String> clusterCandidate 	= new ArrayList<String>();
				List<Topic> clustTopic 			= new ArrayList<Topic>();
				List<String> clustTopicString 	= new ArrayList<String>();
				clusterCandidate.add(t.name);
				for(Feature f:features) {
					if(featuresSelected.exists(f._string)) {
						clusterCandidate.add(f._string);
					}
				}
				//here look at conformity of cluster
				//each member should be connected to at least 2 other topics
				for(String point:clusterCandidate) {
					DocVector featsOfPoint = topicString2Features.get(point);
					if(featsOfPoint!=null) {
						Iterator<String> fopIter = featsOfPoint.keySet().iterator();
						List<String> commons = new ArrayList<String>();
						int nbCommon = 0;
						while(fopIter.hasNext()) {
							String fop = fopIter.next();
							if(clusterCandidate.contains(fop)) {
								nbCommon++;
								commons.add(fop);
							}
						}
						if(nbCommon>=1 && !seensSeed.contains(point)) {
							clustTopic.add(topicString2Topic.get(point));
							clustTopicString.add(point);
						}
					}
				}

				if(clustTopic.size() <= maxSizeClusterLevel1) {
					for(Topic tempTop:clustTopic) {
						seensSeed.add(tempTop.name);
					}
				}
				else {
					clustTopic = new ArrayList<Topic>();
					clustTopicString = new ArrayList<String>();
				}

				if(clustTopic.size()>0) {
					wordsCluster.add(clustTopic);
					wordsClusterString.add(clustTopicString);
				}
			}
		}

		return wordsCluster;
	}
	
	public List<Cluster> getDocumentsClustersWithQuery(String query, List<TopicRankDocument> documents, 
																		int level, final ClusterMethod clustMethod, int maxDocByCluster)
	{
		//1. Query Analysis
		final HashSet<String> queryWords = analyseQuery(query);
		final HashSet<String> synomymsQuery = new HashSet<String>();
		for(String q:queryWords) {
			List<String> synonyms = SimilarNounDetector.searchEngKor(q);
			synomymsQuery.addAll(synonyms);
		}

		//2. Features Extraction
		final long startFE = System.currentTimeMillis();
		//각 단어의 Global DF, Global TF, 공기어(<단어, {공기어1:GLOBAL_TF, 공기어2:GLOBAL_TF, 공기어3:GLOBAL_TF}>)를 추출한다.
		//주의) 로직이 철저하지 않아, 중복되어 Global TF들이 집계되고 있다.
		final StatInformation statInf = extractFeatures(queryWords, documents, nbSnippetsLocal, winsizeLocal);
		

		//3. choose minimum df function of number of documents
		final int minDF;
		if(documents.size() >=200) minDF = 3;
		else if(documents.size() >=100) minDF = 2;
		else minDF = 1;
		
		//3. Features Selection
		//Global DF에 있는 Term들을 Global TF를 반영하여 높은 순서대로 정렬한다=>DF가 낮은 단어가 점수가 높다.
		final DocVector featuresSelected = featuresSelection(statInf, minDF, queryWords, synomymsQuery);

		logger.debug("Features Extraction done in: {} [ms]", (System.currentTimeMillis()-startFE) );		
		
		//needed for clustering of topics

		//4. Creation of the topics for Words Clustering from the maxFeats selected (labels) features
		final long startClustering = System.currentTimeMillis();
		
		final List<List<Topic>> wordsCluster;
		final List<Topic>  topicsSet = new ArrayList<Topic>();
		
		if(  clustMethod==null || clustMethod==ClusterMethod.HAC ) {
			if(local) {
				//Global DF에 있는 Term들과 같이 나온 공기어 Term들을 사용하여 HAC 클러스터링을 수행한다.
				wordsCluster = CreateTopicWithHACandLocal(topicsSet, queryWords, featuresSelected, statInf);
			} else {
				wordsCluster = CreateTopicWithHACandNotLocal(topicsSet, queryWords, featuresSelected, statInf);
			}
		} else {
			wordsCluster = CreateTopicWithoutHAC(topicsSet, queryWords, featuresSelected, statInf, minDF);
		}
		
		//5. Word clustering process
		//KAY_COMMENT 실제 클러스터링은 4에서 수행되고, 아래는 점수만 다시 계산하여 출력하는 것으로 추정된다. 
		final List<Cluster> Clusters = WordClustering(wordsCluster, topicsSet, featuresSelected, statInf, documents, maxDocByCluster);
		
		logger.debug("Clustering done in: {} [ms]", (System.currentTimeMillis()-startClustering) );		
		
		return Clusters;
	}

	private List<Cluster> WordClustering(List<List<Topic>> wordsCluster, List<Topic>  topicsSet, DocVector featuresSelected, 
			StatInformation statInf, List<TopicRankDocument> documents, int maxDocByCluster)
	{

		List<Cluster> CompleteCluster = new  ArrayList<Cluster>();
		
		if(topicsSet.size() <= 2) return CompleteCluster;

		PriorityQueue<Cluster> clustersQueue = new PriorityQueue<Cluster>();

		//클러스터들을 순위를 부여하여 정렬한다.
		final List<DocVector> originalWordsCluster = toDocVector(wordsCluster, featuresSelected);
		for(DocVector originalClust : originalWordsCluster) {
			Cluster cluster = new Cluster();
			cluster.keywords = originalClust;
			cluster.centroid = originalClust;
			cluster.internSimilarity = originalClust.getSumWeight();
			clustersQueue.add(cluster);
		}

		int numCl = 0;
		final List<Cluster> clusters = new ArrayList<Cluster>();
		while(clustersQueue.size()!=0 && numCl<(maxClusters<<1)) {
			numCl++;
			Cluster cluster = clustersQueue.poll();
			clusters.add(cluster);
		}

		Cluster FailCluster = new Cluster();
		clusters.add(FailCluster);
		FailCluster.keywords.setWeight("OTHERS", 1);

		// Documents classification
		for(TopicRankDocument doc: documents) {
			final DocVector tf = statInf.getDocVectLocal(doc.doc_id);
			
			if(tf.size() == 0) {
				FailCluster.documents.increment(doc.doc_id);
				continue;
			}
			
			double maxSim = 0;
			int maxCluster = -1;

			for(int c = 0; c < clusters.size()-1 ; c++) {
				double sim = JACCARD_DISTANCEDV.distance(clusters.get(c).centroid, tf);
				if(sim > 0 && sim > maxSim) {
					maxSim = sim;
					maxCluster = c;
				}
			}

			if(maxCluster == -1) {
				FailCluster.documents.increment(doc.doc_id);
			}
			else {
				clusters.get(maxCluster).documents.increment(doc.doc_id);
			}
		}

		// Documents classification at level 2
		for(int c = 0; c < clusters.size()-1 ; c++) {
			
			Cluster cluster = clusters.get(c);
			
			if(cluster.documents.size() <= maxDocByCluster || cluster.keywords.size() <= 1) continue;
			
			cluster.children = new ArrayList<Cluster>();

			Feature[] parentLabels = cluster.keywords.sortByWeight();
			Feature bestLabelParent = parentLabels[0];
			DocVector parentLabel = new DocVector();
			parentLabel.setWeight(bestLabelParent);
			cluster.keywords = parentLabel;

			for(int i = 1 ; i < parentLabels.length ; i++) {
				Cluster childClust = new Cluster();
				DocVector childLabels = new DocVector();
				childLabels.setWeight(parentLabels[i]);
				childClust.keywords = childLabels;
				cluster.children.add(childClust);
			}

			// Documents sub-classification
			Iterator<String> urlsIter = cluster.documents.iterator();
			
			while(urlsIter.hasNext()) {
				String url = urlsIter.next();
				DocVector tf = statInf.getDocVectLocal(url);
				
				if(tf.size() == 0) {
					FailCluster.documents.increment(url);
					continue;
				}
				
				double maxSim = 0;
				int maxCluster = -1;
				for(int sc=0; sc < cluster.children.size()-1; sc++) {
					double sim = JACCARD_DISTANCEDV.distance(cluster.children.get(sc).keywords, tf);
					if(sim > maxSim && sim > 0) {
						maxSim = sim;
						maxCluster = sc;
					}
				}

				if(maxCluster == -1) {
					FailCluster.documents.increment(url);
				}
				else {
					cluster.children.get(maxCluster).documents.increment(url);
				}
			}
		}

//		Vector<Cluster> finalSolution = new  Vector<Cluster>();
//		int mx = Math.min(maxClusters, clusters.size()-1);
//		for(int c = 0 ; c < mx ; c++) {
//			Cluster cluster = clusters.get(c);
//			if(cluster.documents.size()==1) {
//				FailCluster.documents.add(cluster.documents);
//			}
//			else if(cluster.documents.size() > 0) {
//				cluster.id = finalSolution.size()+1;
//				finalSolution.add(cluster);
//			}
//		}

		List<Cluster> trimmedClusters = new ArrayList<Cluster>();
		int numClust = 1;
		for(int c = 0 ; c < clusters.size()-1 ; c++) {
			Cluster cluster = clusters.get(c);
			if(cluster.documents.size() > 0) {
				cluster.id = numClust++;
				cluster.ReplaceKeyword(replaceDic);
				if(cluster.children!=null) {
					List<Cluster> trimChildren = new ArrayList<Cluster>();
					for(int cc = 0 ; cc < cluster.children.size() ; cc++) {
						Cluster child_cluster = cluster.children.get(cc);
						if(child_cluster.documents.size() > 0) {
							child_cluster.id = numClust++;
							child_cluster.ReplaceKeyword(replaceDic);
							trimChildren.add(child_cluster);
						}
					}
					cluster.children = trimChildren;
				}
				trimmedClusters.add(cluster);
			}
		}

		clustersQueue = new PriorityQueue<Cluster>();

		for(int c=0; c < trimmedClusters.size(); c++) {
			Cluster cluster = trimmedClusters.get(c);
			clustersQueue.add(cluster);
		}

		while(!clustersQueue.isEmpty() && CompleteCluster.size() <= this.top_n_clusters/*maxClusters*/) {
			Cluster c = clustersQueue.poll();
			if(c.documents.size()>0) CompleteCluster.add(c);
		}

		return CompleteCluster;
	}
	
	private List<DocVector> toDocVector(List<List<Topic>> wordsCluster, DocVector feats)
	{
		List<DocVector> result = new ArrayList<DocVector>();
		
		for(List<Topic> clust : wordsCluster) {
		
			DocVector topics = new DocVector();

			for(Topic topic : clust) {
				String topicName = topic.name;
				double weight = feats.getWeight(topicName);
				
				Double w = coreKeyword.get(topicName.toLowerCase());
				if(w != null) weight = w * weight;

				topics.setWeight(topicName, weight);
			}
			result.add(topics);
		}

		return result;
	}

	public Feature[] getTopicsRanked(DocVector candDF, DocVector candQueryCoocDF, int maxFeatures) 
	{
		logger.debug("getTopicsRanked: global re-rank ...");		
		
		DocVector topicsRanked = new DocVector();
		
		Feature[] candidates = candDF.sortByWeight();
		for(Feature feat:candidates) {
			double df =	feat._d;
			if(df>0) {
				double co_df =	candQueryCoocDF.getWeight(feat._string);
				double query_sim = (1+Math.log(df))*Math.sqrt(co_df*(1+Math.log10(10+co_df))/(100+df)/Math.log10(10+df));
				topicsRanked.setWeight(feat._string, query_sim);
			}
			else {
				logger.error("{} has df zero!!", feat._string);		
			}
		}
		
		candidates = topicsRanked.sortByWeight();
		int max = Math.min(maxFeatures, candidates.length);
		Feature[] result = new Feature[max];
		for(int i=0; i<max; i++) {
			result[i] = candidates[i];
			logger.debug("Topic({})={}", i, result[i]);		
		}
		
		return result;
	}
	
	/* New function for Distributed TopicRank */
	public List<Cluster> getClusters(Feature[] topicsDF, Feature[] related , HashMap<String, DocVector> coocurrencesDF)
	{
		List<List<Topic>> wordsCluster = new ArrayList<List<Topic>>();
		
		logger.debug("Start HAC");		

		long hacStart = System.currentTimeMillis();
		HashMap<String, DocVector> normalizedMutualInf = new HashMap<String, DocVector>();
		HashMap<String, Double> topicsRelatness = new HashMap<String, Double>();
		
		if(topicsDF.length>1) {
			List<Topic>  topicsSet = new ArrayList<Topic>();

			HashMap<String, Double> topicsDFHash = new HashMap<String, Double>();
			
			int i=0;
			for(Feature f:topicsDF) {
				String feat = f._string;
				topicsDFHash.put(feat, f._d);
				topicsRelatness.put(feat, related[i]._d);
				logger.debug("{} df={}", feat, f._d);		
				
				i++;
			}
			
			for(Feature f:topicsDF) {
				String feat = f._string;
				Double dffeat = topicsDFHash.get(feat);
				DocVector coocs = coocurrencesDF.get(feat);
				DocVector mutualInf = new DocVector();
				Iterator<String> coocsIter = coocs.iterator();
				
				while(coocsIter.hasNext()) {
					String cooc = coocsIter.next();
					double co_df = coocs.getWeight(cooc);
					double coocfreq = co_df/Math.sqrt(dffeat*topicsDFHash.get(cooc));
					mutualInf.setWeight(cooc, coocfreq);
				}

				logger.debug("mutualInf of {} => {}", feat, mutualInf);		
				
				normalizedMutualInf.put(feat, mutualInf);
			}
			
			logger.debug("normalized of {} => {}", coocurrencesDF, normalizedMutualInf);		
			
			for(Feature f:topicsDF) {
				Topic t = new Topic(f._string, f._d, normalizedMutualInf.get(f._string));
				topicsSet.add(t);
			}
			wordsCluster = new HAC(maxSizeClusterLevel1, 50).doTopicsClustering(topicsSet);
		}
		
		long hacEnd = System.currentTimeMillis();
		logger.debug("HAC done in: {}[ms]", (hacEnd-hacStart));		
	
		Iterator<List<Topic>> iterOnWC = wordsCluster.iterator();
		//5. Word clustering process
		List<Cluster> clusters = new ArrayList<Cluster>();
		PriorityQueue<Cluster> clustersQueue = new PriorityQueue<Cluster>();

		while(iterOnWC.hasNext()) {
			List<Topic> originalClust = iterOnWC.next();
			DocVector clust = new DocVector();
			for(int i=0; i<originalClust.size();i++) {
				Topic t = originalClust.get(i);
				clust.setWeight(t.name, topicsRelatness.get(t.name));
			}
			
			Cluster cluster = new Cluster();
			cluster.keywords = clust;
			cluster.internSimilarity = clust.getSumWeight();
			clustersQueue.add(cluster);
		}

		int numCl = 0;
		while(clustersQueue.size()!=0 && numCl<2*maxClusters) {
			numCl++;
			Cluster cluster = clustersQueue.poll();
			clusters.add(cluster);
		}
		logger.debug("Ranked Clusters:", clusters);		
		
		return clusters;
	}
	
	public String getNETopicRank(List<TopicRankDocument> documents, ArrayList<String> TagInstance)
	{
		// NE토픽 분석시 객체명에 따라 프로그램 코드 변경
		DocVector ne = extractNE(documents);

		Iterator<String> neIter = ne.iterator();

		HashMap<String, DocVector> neTagMap = new HashMap<String, DocVector>();
		
		for(int i = 0 ; i < TagInstance.size(); i++) {
			neTagMap.put(TagInstance.get(i), new DocVector());
		}

		while (neIter.hasNext()) {
			String featNE = neIter.next();
			int sep = featNE.indexOf(":");
			if (sep > -1) {
				String entity = featNE.substring(0, sep);
				if (!bIsStopword(entity)) {
					String type = featNE.substring(sep + 1);

					DocVector NeInfo = neTagMap.get(type);
					if(NeInfo != null) {
						NeInfo.setWeight(entity, ne.getWeight(featNE));
					}
				}
			}
		}

		StringBuilder result = new StringBuilder("{\"results\":[");
		
		Iterator<String> it = neTagMap.keySet().iterator();
		
		int EntryCnt = 0;
		while(it.hasNext()) {
			String NeKey = it.next();
			DocVector NeInfo = neTagMap.get(NeKey);
			SimilarNounDetector.removeSame(NeInfo);
			NeInfo = SimilarNounDetector.removeSub(NeInfo);
			
			if(EntryCnt > 0) result.append(',');
			result.append("{\"keywords\":[");
			Feature[] fe = NeInfo.sortByWeight();
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < Math.min(10, fe.length); i++) {
				if(i != 0) buffer.append(',');
				buffer.append('"');
				buffer.append(fe[i]._string);
				buffer.append('"');
			}
			if(buffer.length() > 0) result.append(buffer);
			result.append("],\"name\":\""+NeKey+"\"}");
			EntryCnt++;
		}

		result.append("]}");
		
		return result.toString();
	}

	public String getNETopicRank(List<TopicRankDocument> documents)
	{
		return getNETopicRank(documents, NeTagInstance);
	}

	private DocVector extractNE(List<TopicRankDocument> documents)
	{
		DocVector neTF = new DocVector();

		for(int i=0; i < documents.size(); i++) {
			TopicRankDocument document = documents.get(i);
			String ne = document.analysis;
			if(ne!=null) {
				ne = ne.replaceAll(", ", "|");
				ne = ne.replaceAll(",", "|");
				ne = ne.replaceAll(" ", "_");
				StringTokenizer nest = new StringTokenizer(ne, "|");
				while(nest.hasMoreTokens()) {
					neTF.increment(nest.nextToken());
				}
			}
		}

		return neTF;
	}
	
	private HashMap<String, TreeSet<String>> dummyUnRelationWord(){
		HashMap<String, TreeSet<String>> rst = new HashMap<String, TreeSet<String>>();
		
		TreeSet<String> wanted = new TreeSet<String>();
		wanted.add("과일");
		rst.put("아이폰", wanted);
		
		return rst;
		
	}
}
