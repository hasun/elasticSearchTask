package elastic.tms.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.lea.api.util.Tag.Analysis;
import com.knu.lea.api.util.Tag.NamedEntity;
import com.knu.lea.api.util.Tag.Sentence;
import com.knu.lea.util.JsonUtil;
import com.saltlux.tms.api.IN2TMSAnalyzer;
import com.saltlux.tms3.util.TYPE;

import java.util.*;

public class TmsUtils {
    static final IN2TMSAnalyzer tmsAnalyzer = init();
    static final String tms_ip = "localhost";
    static final int tmsPort = 10100;
    public static IN2TMSAnalyzer init () {
        IN2TMSAnalyzer tmsAnalyzer = new IN2TMSAnalyzer();
        tmsAnalyzer.setServer(tms_ip, tmsPort);
        tmsAnalyzer.newCommand();
        return tmsAnalyzer;
    }

    public static String getTmsRawStream (String sentence) {
        long langMode = TYPE.LANG_KOR;
        long analyzerMode = TYPE.TYPE_CHUNK;
        Map<Long,String> result = tmsAnalyzer.getRawStream(sentence , langMode|analyzerMode);
        return result.get(langMode|analyzerMode);
    }

    public static Map<String, List<String>> getTmsNeStr (String sentence) {
        long langMode = TYPE.LANG_KOR;
        long analyzerMode = TYPE.TYPE_NE_STR;
        Map<String, List<String>> resultMap = new HashMap<>();
        List <String> ogList = new ArrayList<>();
        List <String> psList = new ArrayList<>();
        List <String> lcList = new ArrayList<>();
        Map<Long,String> result = tmsAnalyzer.getRawStream(sentence , langMode|analyzerMode);
        Analysis analysis = JsonUtil.JsonToTaggedObject(result.get(langMode|analyzerMode));
        for ( Sentence sent : analysis.sentence) {
            for (NamedEntity namedEntity : sent.ne) {
                if (namedEntity.tag.equals("OG")) {
                    ogList.add(namedEntity.text);
                } else if (namedEntity.tag.equals("PS")) {
                    psList.add(namedEntity.text);
                } else if (namedEntity.tag.equals("LC")) {
                    lcList.add(namedEntity.text);
                }
            }
        }
        resultMap.put("OG" , ogList);
        resultMap.put("PS" , psList);
        resultMap.put("LC" , lcList);
        return resultMap;
    }

    public static Map<String, Object> getTextAnalyzer(String data) throws Exception {
        Map<String, Object> result = new HashMap();
        Map<Long, String> EmptyMap = Collections.emptyMap();
        long modeLang = 4294967296L;
        long modeFunc = 0L;
        modeFunc |= 4294967296L;
        modeFunc |= 128L;
        modeFunc |= 32L;
        modeFunc |= 2048L;
        modeFunc |= 256L;
        modeFunc |= 1024L;
        modeFunc |= 512L;
        modeFunc |= 131072L;
        Map<Long, String> oMap = modeFunc != 0L ? getRawStream(data, modeLang, modeFunc) : EmptyMap;
        if (oMap == null) {
            return result;
        } else {
            String tmsResult = (String)oMap.get(modeLang | 128L);
            String morph = tmsResult != null ? tmsResult.trim().toLowerCase() : "";
            tmsResult = (String)oMap.get(32L);
            String bigram = tmsResult != null ? tmsResult.trim().toLowerCase() : "";
            tmsResult = (String)oMap.get(modeLang | 2048L);
            String neStr = tmsResult != null ? tmsResult.trim().toLowerCase() : "";
            tmsResult = (String)oMap.get(modeLang | 256L);
            String tms_raw_stream_store = tmsResult != null ? tmsResult.trim().toLowerCase() : "";
            tmsResult = (String)oMap.get(modeLang | 1024L);
            String tms_raw_stream_index = tmsResult != null ? tmsResult.trim().toLowerCase() : "";
            tmsResult = (String)oMap.get(modeLang | 512L);
            String tms_feature = tmsResult != null ? tmsResult.trim().toLowerCase() : "";
            tmsResult = (String)oMap.get(modeLang | 131072L);
            String saStr = tmsResult != null ? tmsResult.trim().toLowerCase() : "";
            Map<String, List<String>> getNeMap = getNeStr(neStr);
            Map<String, Object> getSaMap = getSaStr(saStr);
            result.put("tms_morph", morph);
            result.put("tms_bigram", bigram);
            result.put("tms_feature", tms_feature);
            result.put("tms_raw_stream_index", tms_raw_stream_index);
            result.put("tms_raw_stream_store", tms_raw_stream_store);
            result.put("tms_entity_name_person", getNeMap.get("PS") != null ? getNeMap.get("PS") : new ArrayList());
            result.put("tms_entity_name_location", getNeMap.get("LC") != null ? getNeMap.get("LC") : new ArrayList());
            result.put("tms_entity_name_organization", getNeMap.get("OG") != null ? getNeMap.get("OG") : new ArrayList());
            result.put("tms_sentiment_polarity_score", getSaMap.get("score") != null ? getSaMap.get("score") : 0);
            result.put("tms_sentiment_positive_text", getSaMap.get("POS") != null ? getSaMap.get("POS") : new ArrayList());
            result.put("tms_sentiment_negative_text", getSaMap.get("NEG") != null ? getSaMap.get("NEG") : new ArrayList());
            return result;
        }
    }

    public static Map<String, List<String>> getNeStr(String neStr) {
        Map<String, List<String>> map = new HashMap();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNodeNeStr = objectMapper.readTree(neStr);
            JsonNode jsonArray = jsonNodeNeStr.get("sentence");
            Iterator var6 = jsonArray.iterator();

            while(var6.hasNext()) {
                JsonNode jsonNode = (JsonNode)var6.next();
                JsonNode neNode = jsonNode.get("ne");
                setNeCode(neNode, map);
            }
        } catch (Exception var9) {
            Exception e = var9;
            e.printStackTrace();
        }

        return map;
    }

    public static Map<String, Object> getSaStr(String saStr) {
        Map<String, Object> map = new HashMap();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNodeNeStr = objectMapper.readTree(saStr);
            JsonNode jsonArray = jsonNodeNeStr.get("sentence");
            Iterator var6 = jsonArray.iterator();

            while(var6.hasNext()) {
                JsonNode jsonNode = (JsonNode)var6.next();
                JsonNode neNode = jsonNode.get("sa");
                setSaCode(neNode, map);
            }
        } catch (Exception var9) {
            Exception e = var9;
            e.printStackTrace();
        }

        return map;
    }

    private static void setNeCode(JsonNode neNode, Map<String, List<String>> map) {
        Iterator var3 = neNode.iterator();

        while(var3.hasNext()) {
            JsonNode node = (JsonNode)var3.next();
            String tag = node.get("tag").textValue();
            String text = node.get("text").textValue();
            List<String> list;
            if (tag.equalsIgnoreCase("PS")) {
                list = map.get("PS");
                if (list == null) {
                    list = new ArrayList();
                }

                if (!list.contains(text)) {
                    list.add(text);
                }
                map.put("PS", list);
            } else if (tag.equalsIgnoreCase("OG")) {
                list = map.get("OG");
                if (list == null) {
                    list = new ArrayList();
                }

                if (!list.contains(text)) {
                    list.add(text);
                }

                map.put("OG", list);
            } else if (tag.equalsIgnoreCase("LC")) {
                list = map.get("LC");
                if (list == null) {
                    list = new ArrayList();
                }

                if (!list.contains(text)) {
                    list.add(text);
                }

                map.put("LC", list);
            }
        }

    }

    private static void setSaCode(JsonNode saNode, Map<String, Object> map) {
        if (saNode != null) {
            long polarity = saNode.get("polarity") != null ? saNode.get("polarity").longValue() : 0L;
            long score = saNode.get("score") != null ? saNode.get("score").longValue() : 0L;
            JsonNode sentiWordNode = saNode.get("sentiword");
            Object list;
            Iterator var9;
            JsonNode jsonNode;
            String text;
            if (polarity == 1L) {
                list = (List)map.get("POS");
                if (list == null) {
                    list = new ArrayList();
                }

                var9 = sentiWordNode.iterator();

                while(var9.hasNext()) {
                    jsonNode = (JsonNode)var9.next();
                    text = jsonNode.textValue();
                    if (!((List)list).contains(text)) {
                        ((List)list).add(text);
                    }
                }

                map.put("POS", list);
            } else if (polarity == -1L) {
                list = (List)map.get("NEG");
                if (list == null) {
                    list = new ArrayList();
                }

                var9 = sentiWordNode.iterator();

                while(var9.hasNext()) {
                    jsonNode = (JsonNode)var9.next();
                    text = jsonNode.textValue();
                    if (!((List)list).contains(text)) {
                        ((List)list).add(text);
                    }
                }

                map.put("NEG", list);
            }

            long beforeScore = map.get("score") != null ? (Long)map.get("score") : 0L;
            score += beforeScore;
            map.put("score", score);
        }

    }


    private static Map<Long, String> getRawStream(final String text, final long modeLang, final long modeFunc) throws Exception {
        Map<Long, String> result = null;
        IN2TMSAnalyzer analyzer = new IN2TMSAnalyzer();
        analyzer.setServer(tms_ip, tmsPort);
        analyzer.setSocketTimeOut(30000);
        result = analyzer.getRawStream(text, modeLang | modeFunc);
        return result;
    }
}
