package com.saltlux.util.topicrank;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

public class SimilarNounDetector {
private static final Logger logger = LoggerFactory.getLogger(SimilarNounDetector.class);
	
	private static boolean debug = false;
    private static Map<Character, Character> chosungList = new HashMap<Character, Character>();
	private static Map<Character, Character> jungsungList = new HashMap<Character, Character>();
	private static Map<Character, Character> jongsungList = new HashMap<Character, Character>();
	private static Map<Character, Character> engChosung = new HashMap<Character,Character>();
	private static Map<Character, Character> engJungsung = new HashMap<Character, Character>();
	private static Map<Character, Character> engJongsung = new HashMap<Character, Character>();
	private static final Pattern engENG_ = Pattern.compile("([a-z]|[A-Z]|_|-)+");
	
	private static char[] StrChoSung = {'F' , 'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ',
										'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'};
	private static char[] StrJungSung ={'F' , 'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ',
										'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'};
    private static char[] StrJongSung ={'F' , 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
										'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
										'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ', 'ㅇ' };

    private static final char[][] KorEngSimCho = {
    	{'ㄱ', 'k'}, {'ㅋ', 'k'}, {'ㄲ', 'k'}, {'ㄴ', 'n'}, {'ㄷ', 't'}, 
    	{'ㅌ', 't'}, {'ㄸ', 't'}, {'ㄹ', 'l'}, {'ㅁ', 'm'}, {'ㅂ', 'p'},
    	{'ㅍ', 'p'}, {'ㅃ', 'p'}, {'ㅅ', 's'}, {'ㅆ', 's'}, {'ㅈ', 'c'},
    	{'ㅊ', 'c'}, {'ㅉ', 'c'}, {'ㅎ', 'h'}, {'ㅇ', 'H'}
    };
    private static final char[][] KorEngSimJung = {
    	{'ㅏ', 'A'}, {'ㅓ', 'A'}, {'ㅗ', 'A'}, {'ㅕ', 'A'}, {'ㅛ', 'A'},
    	{'ㅘ', 'A'}, {'ㅝ', 'A'}, {'ㅔ', 'A'}, {'ㅐ', 'A'}, {'ㅖ', 'A'},
    	{'ㅒ', 'A'}, {'ㅑ', 'O'}, {'ㅣ', 'I'}, {'ㅟ', 'H'}, {'ㅞ', 'W'},
    	{'ㅚ', 'W'}, {'ㅙ', 'W'}, {'ㅠ', 'U'}, {'ㅜ', 'U'}, {'ㅡ', 'Y'},
    	{'ㅢ', 'Y'}
    };
    private static final char[][] KorEngSimJong = {
    	{'ㄱ', 'K'}, {'ㅋ', 'K'}, {'ㄲ', 'K'}, {'ㄳ', 'K'}, {'ㄴ', 'N'},
    	{'ㄵ', 'N'}, {'ㄶ', 'N'}, {'ㄷ', 'T'}, {'ㅌ', 'T'}, {'ㄹ', 'L'},
    	{'ㄺ', 'L'}, {'ㄻ', 'L'}, {'ㄼ', 'L'}, {'ㄽ', 'L'}, {'ㄾ', 'L'},
    	{'ㄿ', 'L'}, {'ㅀ', 'L'}, {'ㅁ', 'M'}, {'ㅂ', 'P'}, {'ㅄ', 'P'},
    	{'ㅍ', 'P'}, {'ㅅ', 'T'}, {'ㅆ', 'T'}, {'ㅊ', 'T'}, {'ㅈ', 'T'},
    	{'ㅎ', 'T'}, {'ㅇ', 'G'}
    };
    private static final char[][] EngKorSimCho = {
    	{'k', 'ㄱ'}, {'k', 'ㅋ'}, {'k', 'ㄲ'}, {'n', 'ㄴ'}, {'t', 'ㄷ'},
    	{'t', 'ㅌ'}, {'t', 'ㄸ'}, {'l', 'ㄹ'}, {'m', 'ㅁ'}, {'p', 'ㅂ'},
    	{'p', 'ㅍ'}, {'p', 'ㅃ'}, {'s', 'ㅅ'}, {'s', 'ㅆ'}, {'c', 'ㅈ'},
    	{'c', 'ㅊ'}, {'c', 'ㅉ'}, {'h', 'ㅎ'}, {'h', 'ㅇ'}
    };
    private static final char[][] EngKorSimJung = {
    	{'A', 'ㅏ'}, {'A', 'ㅓ'}, {'A', 'ㅗ'}, {'A', 'ㅕ'}, {'A', 'ㅑ'},
    	{'W', 'ㅟ'}, {'W', 'ㅚ'}, {'W', 'ㅢ'}, {'W', 'ㅙ'}, {'O', 'ㅛ'},
    	{'O', 'ㅠ'}, {'U', 'ㅜ'}, {'U', 'ㅡ'}, {'U', 'ㅘ'}, {'Y', 'ㅞ'},
    	{'Y', 'ㅝ'}
    };
    private static final char[][] EngKorSimJong = {
    	{'K', 'ㄱ'}, {'K', 'ㅋ'}, {'K', 'ㄲ'}, {'K', 'ㄳ'}, {'N', 'ㄴ'},
    	{'N', 'ㄵ'}, {'N', 'ㄶ'}, {'T', 'ㄷ'}, {'T', 'ㅌ'}, {'L', 'ㄹ'},
    	{'L', 'ㄺ'}, {'L', 'ㄻ'}, {'L', 'ㄼ'}, {'L', 'ㄽ'}, {'L', 'ㄾ'},
    	{'L', 'ㄿ'}, {'L', 'ㅀ'}, {'M', 'ㅁ'}, {'P', 'ㅂ'}, {'P', 'ㅄ'},
    	{'P', 'ㅍ'}, {'S', 'ㅅ'}, {'S', 'ㅆ'}, {'C', 'ㅊ'}, {'C', 'ㅈ'},
    	{'G', 'ㅎ'}, {'G', 'ㅇ'}
    };
    
    static {
    	for(int i = 0 ; i < KorEngSimCho.length; i++) chosungList.put(KorEngSimCho[i][0], KorEngSimCho[i][1]);
    	for(int i = 0 ; i < KorEngSimJung.length; i++) jungsungList.put(KorEngSimJung[i][0], KorEngSimJung[i][1]);
    	for(int i = 0 ; i < KorEngSimJong.length; i++) jongsungList.put(KorEngSimJong[i][0], KorEngSimJong[i][1]);
    	for(int i = 0 ; i < EngKorSimCho.length; i++) engChosung.put(EngKorSimCho[i][0], EngKorSimCho[i][1]);
    	for(int i = 0 ; i < EngKorSimJung.length; i++) engJungsung.put(EngKorSimJung[i][0], EngKorSimJung[i][1]);
    	for(int i = 0 ; i < EngKorSimJong.length; i++) engJongsung.put(EngKorSimJong[i][0], EngKorSimJong[i][1]);
    }

	//두 단어간의 유사도 정도, '사탕 vs 박하사탕'의 경우 0.50로 설정 시 유사어로 인식
	private static final double SIM_RATE = 0.59;
	//두 단어사이의 공통 단어 포함 정도, 0.33으로 설정 시 '도쿄돔  vs 후쿠오카 야후돔'의 유사도 검사
	private static final double SIM_WORD = 0.67;

    private static  IndexSearcher engkorSearcher = null;
    private static HashMap<String, TreeSet<String>> synonyms = new HashMap<String, TreeSet<String>>();
    
	public static void setDebugMode(boolean useDebug)
	{
		debug = useDebug;
	}
	
	public static void setengKorSearcher(IndexSearcher searcher)
	{
		engkorSearcher = searcher;
	}
	
	public static void setSynonymsDic(HashMap<String, TreeSet<String>> synonymsDic)
	{
		synonyms = synonymsDic;
	}
    /**
	 * 한글 단어의 초성, 중성, 종성일 읽어 사용자가 정의한 알파벳 규칙으로 변경해주는 기능을 합니다.
	 * @param keyword: 알파벳으로 변형될 단어
	 * @return String: 알파벳 형식으로 표현된 단어
	 */
    public static String getKorSoundHex(String keyword)
	{
		String code = transform(keyword, "EUMSO").replaceAll("H", "");
		return code;
	}

    public static HashMap<String, List<String>> removeSimilarRules(DocVector candidates)
	{
    	return removeSimilarRules(candidates, SIM_RATE, SIM_WORD);
	}

	/**
	 * 피쳐 배열에서 각각의 피쳐를 비교 (예, feature[0].string vs feature[1].string)<BR>
	 * clp: common longest prefix (공통 prefix 부분 int로 리턴)<BR>
	 * cls: common longest suffix (공통 suffix 부분 int로 리턴)<BR>
	 * prefix에 매치되는 단어가 한단어 이상이고 suffix가 >=0 일때,
	 * clp+cls/minimum length of A/B이 SIM_WORD 이상이면, '_'는 제거,
	 * string count(단어수) 와 unit size(단어 길이) 가 3개 이하 시 첫 단어를 비교하여 다른 단어로 인식<BR>
	 * 예)아이폰_구매 vs 아이폰_판매<BR>
	 * 그 외의 경우 모두 비슷한 Feature로 인식
	 * @param DocVector
	 * @param SIM_RATE - 두 단어간의 유사도 정도
	 * @param SIM_WORD - 두 단어사이의 공통 단어 포함 정도
	 * */
    public static HashMap<String, List<String>> removeSimilarRules(DocVector candidates, double SIM_RATE, double SIM_WORD)
	{
		HashMap<String, List<String>> result = new HashMap<String, List<String>>();
		Feature[] features = candidates.sortByWeight();
		TreeSet<String> similar = new TreeSet<String>();

		for(int i =0; i<features.length; i++) {
			String a = features[i]._string;
			if(!similar.contains(a)) {
				similar.add(a);
				for(int j=i+1; j<features.length; j++) {
					String b = features[j]._string;
					int alenght = a.length();
					int blength = b.length();
					int minLenght = Math.min(alenght, blength);
					StringTokenizer stA = new StringTokenizer(a, "_");
					StringTokenizer stB = new StringTokenizer(b, "_");
					int aCount = stA.countTokens();
					int bCount = stB.countTokens();

					int clp = commonLongestPrefix(a, b);
					int cls = commonLongestSuffix(a, b);
					if(clp >= 0 || cls >= 0) {
						if(engENG_.matcher(a).matches() && engENG_.matcher(b).matches()) {
							if (a.equalsIgnoreCase(b)) {
								List<String> sim = result.get(a);
								
								if(sim==null) sim = new Vector<String>();
								
								sim.add(b);
								similar.add(b);
								result.put(a, sim);
							}
						}
						else if(((double)(clp+cls)/(double)(minLenght))>= SIM_WORD) {

							if(a.equals(b)) continue;
							
							else if (0<clp || 0<cls) {
								String aUnit="";
								String bUnit="";
								Vector<String> list = new Vector<String>();
								while(stA.hasMoreTokens()) {
									aUnit = stA.nextToken();
									list.add(aUnit);
									while(stB.hasMoreTokens()) {
										bUnit = stB.nextToken();
										list.add(bUnit);
									}
								}
								list.add(a);
								list.add(b);

								String nospaceA = a.replaceAll("_","");
								String nospaceB = b.replaceAll("_","");
								int aSize=nospaceA.length();
								int bSize=nospaceB.length();
								int aUnitSize = aUnit.length();
								int bUnitSize = bUnit.length();

								if(aSize> bSize) {
									if((double)bSize/(double)aSize >= SIM_RATE) {
										//아이폰_구매 vs 아이폰_판매
										if(((aCount==2 && bCount==2 || aCount==3 && bCount==3) && aUnitSize == bUnitSize) ||
												((aCount==2 && bCount==2 || aCount==3 && bCount==3) && aUnitSize != bUnitSize)) {
											if((!aUnit.substring(0,1).equals((bUnit.substring(0,1))) && (cls <=1))) {
											}
											else {
												addsimilarTerm(result, similar, a, b);
											}
										}
										else {
											addsimilarTerm(result, similar, a, b);
										}
									}
								}
								else if (aSize < bSize) {
									if((double)aSize/(double)bSize >= SIM_RATE) {
										if(((aCount==2 && bCount==2) && aUnitSize == bUnitSize) || ((aCount==2 && bCount==2) && aUnitSize != bUnitSize) ||
												((aCount==3 && bCount==3) && aUnitSize == bUnitSize) || ((aCount==3 && bCount==3) && aUnitSize != bUnitSize)) {
											if((!aUnit.substring(0,1).equals((bUnit.substring(0,1))) && (cls <=1))) {
												;
											}
											else {
												addsimilarTerm(result, similar, a, b);
											}
										}
										else {
											addsimilarTerm(result, similar, a, b);
										}
									}
								}
								else if (aSize == bSize) {
									if(((aCount==2 && bCount==2) && aUnitSize == bUnitSize) || ((aCount==2 && bCount==2) && aUnitSize != bUnitSize) ||
											((aCount==3 && bCount==3) && aUnitSize == bUnitSize) || ((aCount==3 && bCount==3) && aUnitSize != bUnitSize)) {
										if((!aUnit.substring(0,1).equals((bUnit.substring(0,1))) && (cls <=2))) {
											;
										}
										else {
											addsimilarTerm(result, similar, a, b);
										}
									}
									else {
										addsimilarTerm(result, similar, a, b);
									}
								}
							}
							else {
								addsimilarTerm(result, similar, a, b);
							}
						}
					}
				}
			}
		}
		
		return result;
	}


    private static void addsimilarTerm(HashMap<String, List<String>> result, TreeSet<String> similar, String a, String b)
    {
    	List<String> sim;
    	
    	similar.add(b);
    	
		if((sim = result.get(a))==null) {
			sim = new Vector<String>();
		}
		
		sim.add(b);
		result.put(a, sim);
    }

	private static int commonLongestPrefix(String a, String b)
	{
		int alenght = a.length();
		int blength = b.length();
		int minLenght = Math.min(alenght, blength);
		int clp = 0;
		for(int i=0; i<minLenght; i++) {
			if(a.charAt(i) == b.charAt(i)) clp++;
			else break;
		}

		return clp;
	}

	private static int commonLongestSuffix(String a, String b)
	{
		int alenght = a.length();
		int blength = b.length();
		int minLenght = Math.min(alenght, blength);
		int cls = 0;
		for(int i=1; i<=minLenght; i++) {
			if(a.charAt(alenght-i) == b.charAt(blength-i)) cls++;
			else break;
		}

		return cls;
	}

	public static HashMap<String, Set<String>> removeSame(DocVector candidates)
	{
		HashMap<String, String> canonicalForms = new HashMap<String, String> ();
		HashMap<String, DocVector> seens = new HashMap<String, DocVector> ();
		HashMap<String, String> bestRepresentants = new HashMap<String, String> ();
		HashMap<String, Set<String>> similarSets = new HashMap<String, Set<String>>();

		Feature[] features = candidates.sortByWeight();
		for(int f =0; f<features.length; f++) {
			String key = features[f]._string;
			String[] subParts = key.split("_");
			TreeSet<String> alphaorder = new TreeSet<String>();
			for(String sub:subParts) {
				alphaorder.add(sub.toLowerCase());
			}

			String canonicalForm = "";
			Iterator<String> subIter = alphaorder.iterator();
			while(subIter.hasNext()) {
				String sub = subIter.next();
				canonicalForm+=sub;
			}
			canonicalForms.put(key, canonicalForm);

			if(key.indexOf("-")>-1) {
				canonicalForms.put(key, key.replaceAll("-", "").toLowerCase());
				canonicalForms.put(key, key.replaceAll("-", "_").toLowerCase());
				canonicalForm = key.replaceAll("-", "").toLowerCase();
			}

			DocVector similarsSet = seens.get(canonicalForm);
			if(similarsSet == null) {
				similarsSet = new DocVector();
				bestRepresentants.put(canonicalForm, key);
			}
			similarsSet.setWeight(key, features[f]._d);
			seens.put(canonicalForm, similarsSet);

			if(key.indexOf("-")>-1) {
				canonicalForm = key.replaceAll("-", "_").toLowerCase();
				similarsSet = seens.get(canonicalForm);
				if(similarsSet == null) {
					similarsSet = new DocVector();
					bestRepresentants.put(canonicalForm, key);
				}
				similarsSet.setWeight(key, features[f]._d);
				seens.put(canonicalForm, similarsSet);
			}
		}

		TreeSet<String> alreadySeen = new TreeSet<String>();

		for(int f = 0; f<features.length; f++) {
			String key = features[f]._string;
			if(!alreadySeen.contains(key)) {
				String canonicalForm = canonicalForms.get(key);
				String best = bestRepresentants.get(canonicalForm);
				if(best.equals(key)) {
					DocVector similars = seens.get(canonicalForm);

					candidates.setWeight(key, similars.getSumWeight());
					if(similars.size()>1) {
						alreadySeen.addAll(similars.keySet());
						similarSets.put(key, similars.keySet());
						
						if(debug)
							logger.debug("RemoveSame found similars: {} => bect={}, previous weight={}, new weight={}",
												similars.keySet(), key, features[f]._d, candidates.getWeight(key) );
					}
				}
			}
		}

		return similarSets;
	}


	public static DocVector removeSub(DocVector candidates)
	{
		HashMap<String,Double> seens = new HashMap<String,Double>();
		TreeSet<String> sames = new TreeSet<String>();
		Feature[] features = candidates.sortByWeight();

		for(int f =0; f<features.length; f++) {
			String key = features[f]._string;
			String feat = key.toLowerCase();

			double w = features[f]._d;
			if(seens.get(feat)==null) {
				seens.put(feat, w);
			}
			else {
				sames.add(key);
			}

			/**
			 * case1:
				feat is substring of longest = > remove it

				KORALL 2009 8
				Leipzig Max Planck:545.3523661499449
				Leipzig Max Planck연구소:545.3523661499449
				-------
				DOE 산하 재생:559.3036464459755
				DOE 산하 재생에너지:553.7610449481368
			 */

			feat = key;
			String[] subKeys = key.split(" ");
			TreeSet<String> prevElements = new TreeSet<String> ();
			boolean toBefilter = false;
			for(int i=0; i<subKeys.length && !toBefilter; i++) {
				if(prevElements.contains(subKeys[i]))
					toBefilter = true;
				prevElements.add(subKeys[i]);
			}

			if(toBefilter) {
				sames.add(feat);
				System.out.println(" found to be filtered:" + feat);
			}
			else {
				for(int i=2; i<feat.length(); i++) {
					String substring = feat.substring(0, i);
					if(candidates.exists(substring)) {
						sames.add(substring);
						if(seens.get(feat.toLowerCase()) == null) {
							seens.put(feat.toLowerCase(), candidates.getWeight(feat));
						}
						
						seens.put(feat.toLowerCase(), seens.get(feat.toLowerCase())+candidates.getWeight(feat));
					}
				}

				for(int i=1; i<feat.length()-1; i++) {
					String substring = feat.substring(i, feat.length());
					if(candidates.exists(substring)) {
						sames.add(substring);
						if(seens.get(feat.toLowerCase()) == null) {
							seens.put(feat.toLowerCase(), candidates.getWeight(feat));
						}
						
						seens.put(feat.toLowerCase(), seens.get(feat.toLowerCase())+candidates.getWeight(feat));
					}
				}
			}
		}

		DocVector result = new DocVector();
		for(String key : candidates.keySet()) {
			if(!sames.contains(key)) {
				String feat = key.toLowerCase();
				double w = seens.get(feat);
				result.setWeight(key, w);
			}
		}

		return result;
	}
	/**
	 * <p>단어들의 비슷한 발음 유사도를 기반으로 동일한 알파벳 코드로 인식되는 단어들을 제거합니다.
	 * @param candidates: DocVector
	 * @return DF가 가장 높은 단어와 weight의 sum이 포함된 DocVector
	 * */
	public static void removeSameSoundHex(DocVector candidates)
	{
		HashMap<String, String> canonicalForms = new HashMap<String, String> ();
		HashMap<String, DocVector> seens = new HashMap<String, DocVector> ();
		HashMap<String, String> bestRepresentants = new HashMap<String, String> ();

		Feature[] features = candidates.sortByWeight();
		for(int f =0; f<features.length; f++) {
			String key = features[f]._string;
			String canonicalForm = getKorSoundHex(key);
			canonicalForms.put(key, canonicalForm);
			DocVector similarsSet = seens.get(canonicalForm);
			if(similarsSet == null) {
				similarsSet = new DocVector();
				bestRepresentants.put(canonicalForm, key);
			}
			similarsSet.setWeight(key, features[f]._d);
			seens.put(canonicalForm, similarsSet);
		}

		for(int f = 0; f<features.length; f++) {
			String key = features[f]._string;
			String canonicalForm = canonicalForms.get(key);
			String best = bestRepresentants.get(canonicalForm);
			if(best.equals(key)) {
				DocVector similars = seens.get(canonicalForm);
				candidates.setWeight(key, similars.getSumWeight());
				if(similars.size()>1 && debug) {
					System.out.println("found similar SoundHex: " + similars.keySet() + " => best = " + key);
					System.out.println(key + " previous weight = " + features[f]._d + " => new weight = "+ candidates.getWeight(key));
				}
			}
		}

	}


	public static HashMap<String, Set<String>> groupSynomyms(DocVector candidates)
	{
		HashMap<String, Set<String>> similarSets = new HashMap<String, Set<String>>();
		Feature[] features = candidates.sortByWeight();
		TreeSet<String> synsSets = new TreeSet<String>();

		for(int f =0; f<features.length; f++) {
			String key = features[f]._string;
			if(!synsSets.contains(key)) {
				double newWeight = features[f]._d;
				Vector<String> t_synonyms = searchEngKor(key);
				TreeSet<String> synonymsWord = getSynonymsWord(key);
				if(synonymsWord != null) t_synonyms.addAll(synonymsWord); //ggomawitch test
				
				boolean foundSyn = false;
				for(String syn:t_synonyms) {
					if(!syn.equals(key)) {
						synsSets.add(syn);
						if(candidates.exists(syn)) {
							foundSyn = true;
							Set<String> similars = similarSets.get(key);
							if(similars==null) {
								similars = new HashSet<String>();
							}
							similars.add(syn);
							similarSets.put(key, similars);
							newWeight += candidates.getWeight(syn);
							if(debug)
								System.out.println(key + "(weight="+features[f]._d+")" + " / " + syn + " weight="+candidates.getWeight(syn) + ") are synonyms. => new weight:" + newWeight);
						}
					}
				}
				candidates.setWeight(key, newWeight);
				if(debug && foundSyn)
					System.out.println(key + " new weight="+ newWeight);
			}
		}

		return similarSets;
	}

	public static Vector<String>  searchEngKor(String key)
	{
		Vector<String> synonymsWord = new Vector<String>();

		if(engkorSearcher == null) return synonymsWord;
		try {
			TermQuery query = new TermQuery(new Term("key", key));
			TopDocs tfld = engkorSearcher.search(query, 10);
			ScoreDoc[] sdoc = tfld.scoreDocs;

			for(int d = 0 ; d < sdoc.length; d++) {
				Document doc = engkorSearcher.doc(sdoc[d].doc);
				String[] values = doc.get("values").split(" ");
				for(String v:values) {
					if(!synonymsWord.contains(v)) synonymsWord.add(v);
				}
			}
		}
		catch(Exception e) {
			;
		}

		return synonymsWord;
	}

	public static TreeSet<String> getSynonymsWord(String key)
	{
		return synonyms.get(key);
	}
	
	//Katakana
	public static boolean isKatakana(String source)
	{
		char wch;
		int nLen = source.length();

		for(int i = 0; i < nLen; i++) {
			wch = source.charAt(i);
			if (wch < 0x30a0) return false;
			if (wch > 0x30ff) return false;
		}
		return true;
	}

	//Hiragana
	public static boolean isHiragana(String source)
	{
		char wch;
		int nLen = source.length();

		for(int i = 0; i < nLen; i++) {
			wch = source.charAt(i);
			if (wch < 0x3040) return false;
			if (wch > 0x309f) return false;
		}
		return true;
	}

	//Chinese characters for Korean, Japanese, Chinese
	public static boolean isCJK(String source)
	{
		char wch;
		int nLen = source.length();

		for(int i = 0; i < nLen; i++) {
			wch = source.charAt(i);
			if (wch < 0x4e00) return false;
			if (wch > 0x9fff) return false;
		}
		return true;
	}

	public static boolean bIsHangule(String source)
	{
		char wch;
		int nLen = source.length();

		for(int i = 0; i < nLen; i++) {
			wch = source.charAt(i);
			if ( wch < 0xAC00 ) return false;
			if ( wch > 0xD7A3 ) return false;
		}
		return true;
	}

	private static String RemoveSpace(String str)
	{
		int nLen;
		char W;
		StringBuffer sb = new StringBuffer();

		nLen = str.length();
		for(int i = 0; i < nLen; i++) {
			W = str.charAt(i);
			if(W != ' ' && W != '\t') {
				sb.append(W);
			}
		}

		return sb.toString();
	}

	private static String GetTokenString(String strData)
	{
		// 키워드###키워드2###키워드3 처럼 입력됨
		// 전체 공백을 없앤 후에
		// ###을 찾아서 공백으로 대치한 후
		// 그 String을 반환한다
		int nSize;
		int nLoop;
		StringBuffer sb = new StringBuffer();
		char W;

		strData = RemoveSpace(strData);

		nSize = strData.length();

		for(nLoop = 0; nLoop < nSize; nLoop++) {
			W = strData.charAt(nLoop);

			if(nLoop + 2 < nSize) {
			   if(W  == '#' &&
  			      strData.charAt(nLoop+1) == '#' &&
  			      strData.charAt(nLoop+2) == '#') {
					// 공백 더하기
					sb.append(' ');
					nLoop += 2;
				}
				else {
					sb.append(W);
				}
			}
			else {
				sb.append(W);
			}
		}

		return sb.toString();
	}
	
	// 유니코드 초중성 분리
	private static int GetChoSungFromChar(int W)
	{
		W -= 0xAC00;
		return ((W / (21*28)) + 1);
	}

	private static int GetJungSungFromChar(int W)
	{
		W -= 0xAC00;
		W = W % 588;
		return ((W / 28) + 1);
	}

	private static int GetJongSungFromChar(int W)
	{
		W -= 0xAC00;
		W = W % 588;
		return (W % 28);
	}

	// 초중성 값으로 단일 음절 얻기
	private static char GetEumjeolFromChoSung(int nChoSung)
	{
		if(nChoSung >= 0 && nChoSung <= 19){
			return StrChoSung[nChoSung];
		}
		else {
			return ' ';
		}
	}

	private static char GetEumjeolFromJungSung(int nJungSung)
	{
		if(nJungSung >= 0 && nJungSung <= 21){
			return StrJungSung[nJungSung];
		}
		else {
			return ' ';
		}
	}
	private static char GetEumjeolFromJongSung(int nJongSung)
	{
		if(nJongSung >= 0 && nJongSung <= 28){
			return StrJongSung[nJongSung];
		}
		else {
			return ' ';
		}
	}

	private static String Bigram(String str)
	{
		int nSize;
		str = RemoveSpace(str);
		nSize = str.length();
		StringBuffer sb = new StringBuffer();

		for(int i = 0; i < nSize-1; i++) {
			sb.append(str.charAt(i));
			sb.append(str.charAt(i+1));
			sb.append(" ");
		}
		return sb.toString();
	}

	/**
	 * 이 메소드를 구현하면 Index Agent가 자동으로 이 메소드를 호출하게 되며 그 결과를 인덱싱 하게 된다.	 *
	 * @param data SQL 결과
	 * @param param Index Agent XML에서 정의한 PARAM
	 * @return 변형된 값을 리턴한다
	 * @throws Exception
	 */
	public static String transform(String data, String param)
	{
		String result = "";
		if (param.equals("EUMSO2")) {
			// 음소 유사성 처리 (종성 없으면 뒤의 초성 당기기)
			// 전체를 붙인 후
			// 한국어는 음소 분리를 하여 입력
			int nLen;
			String strRet;
			char W;
			char W2;
			int nChoSung, nJungSung, nJongSung;
			int nChoSung2;
			StringBuffer sb = new StringBuffer();
			strRet = RemoveSpace(data);

			nLen = strRet.length();

			for(int i = 0; i < nLen; i++) {
				W = strRet.charAt(i);

				if(W >= 0xAC00) {
					// 한글
					nChoSung = GetChoSungFromChar(W);
					nJungSung = GetJungSungFromChar(W);
					nJongSung = GetJongSungFromChar(W);

					sb.append(GetEumjeolFromChoSung(nChoSung));
					sb.append(GetEumjeolFromJungSung(nJungSung));

					if(GetEumjeolFromJongSung(nJongSung) != 'F') {
						sb.append(GetEumjeolFromJongSung(nJongSung));
					}
					else {
						// 종성이 없으면 다음 음절의 초성을 복사함
						if(i+1 < nLen) {
							W2 = strRet.charAt(i+1);

							if(W >= 0xAC00) {
								nChoSung2 = GetChoSungFromChar(W2);
								sb.append(GetEumjeolFromChoSung(nChoSung2));
							}
						}
					}
				}
				else {
					sb.append(W);
				}
			}

			strRet =  sb.toString();

			return strRet;
		}
		else if (param.equals("EUMSO")) {
			// 음소 유사성 처리
			// 전체를 붙인 후
			// 한국어는 음소 분리를 하여 입력
			int nLen;
			String strRet;
			char W;
			int nChoSung, nJungSung, nJongSung;
			StringBuffer sb = new StringBuffer();
			strRet = data.trim();
			nLen = strRet.length();

			for(int i = 0; i < nLen; i++) {
				W = strRet.charAt(i);

				if(W >= 0xAC00) {
					// 한글
					nChoSung = GetChoSungFromChar(W);
					nJungSung = GetJungSungFromChar(W);
					nJongSung = GetJongSungFromChar(W);

					Set<Character> choKeys = chosungList.keySet();
					Set<Character> jungKeys = jungsungList.keySet();
					Set<Character> jongKeys = jongsungList.keySet();

					Iterator<Character> choKeyIter = choKeys.iterator();
					Iterator<Character> jungKeyIter = jungKeys.iterator();
					Iterator<Character> jongKeyIter = jongKeys.iterator();

					while(choKeyIter.hasNext()) {
						Character key = choKeyIter.next();
						Character value = chosungList.get(key);
						if (key == (GetEumjeolFromChoSung(nChoSung))) {
							sb.append(value);
						}
					}
					while(jungKeyIter.hasNext()) {
						Character key = jungKeyIter.next();
						Character value = jungsungList.get(key);
						if (key == (GetEumjeolFromJungSung(nJungSung))) {
							sb.append(value);
						}
					}
					while(jongKeyIter.hasNext()) {
						Character key = jongKeyIter.next();
						Character value = jongsungList.get(key);
						if(key == GetEumjeolFromJongSung(nJongSung)) {
							sb.append(value);
						}
					}
				}
				else {
					sb.append(W);
				}
			}

			strRet =  sb.toString();

			return strRet;
		}
		else if (param.equals("REVERSE")) {
			// 전체를 reverse한 후 bigram
			 System.out.println("REVERSE");

			int nLen;
			char W;

			data = GetTokenString(data);

			StringBuffer sb = new StringBuffer();

			nLen = data.length();
			for(int i = nLen-1; i >= 00; i--) {
				W = data.charAt(i);
				sb.append(W);
			}

			result = sb.toString();
			if (result == null) return "";

			 System.out.println(result);
			return result;
		}
		else if (param.equals("KEYWORD")) {
			result = GetTokenString(data);
			if (result == null) return "";

			return result;
		}
		else if (param.equals("BIGRAM")) {
			System.out.println("Bigram");

			data = GetTokenString(data);
			result = Bigram(data);

			if (result == null) return "";

			return result;
		}
		else {
			System.out.println("Unknown param");
			System.out.println(param);

			return result;
		}
	}
}
