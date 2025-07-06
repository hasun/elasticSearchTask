package elastic.index.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import elastic.index.domain.NewsIndex;
import elastic.tms.util.TmsUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class IndexCreateService {
    private ElasticsearchOperations operations;
//    @Autowired
//    private NewsIndexRepository newsIndexRepository;
    public IndexCreateService(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    public boolean newsIndexCreate (List<NewsIndex> indexList) {
        long indexNewCount = 0;
        // =========================================================
        // NewsArticle 인덱스 생성 및 JSON 데이터 저장 로직 (추가)
        // =========================================================
        System.out.println("NewsIndex Elasticsearch 인덱스 생성 및 매핑 시작...");

        // NewsArticle 클래스에 해당하는 인덱스가 존재하지 않으면 생성합니다.
        boolean newsIndexDeleted = operations.indexOps(NewsIndex.class).delete();
        boolean newsIndexCreated = operations.indexOps(NewsIndex.class).create();
        System.out.println("NewsIndex 인덱스 생성 여부: " + newsIndexCreated);

        // NewsArticle 클래스의 매핑을 인덱스에 적용합니다.
        boolean newsMappingApplied = operations.indexOps(NewsIndex.class).putMapping(operations.indexOps(NewsIndex.class).createMapping());
        System.out.println("NewsIndex 매핑 적용 여부: " + newsMappingApplied);

        System.out.println("NewsIndex Elasticsearch 인덱스 생성 및 매핑 완료.");

        System.out.println("JSON 텍스트 파싱 및 NewsIndex 데이터 저장 시작...");

        // indexData.txt 파일에서 엔터 단위로 JSON 객체를 읽어와 리스트에 저장합니다.
        List<NewsIndex> newsArticlesFromFile = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource("indexData.txt").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            ObjectMapper objectMapper = new ObjectMapper(); // ObjectMapper는 루프 밖에서 한 번만 생성
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // 빈 줄 건너뛰기
                }
                try {
                    NewsIndex newsArticle = objectMapper.readValue(line, NewsIndex.class);
                    String tmsRawStream = TmsUtils.getTmsRawStream(newsArticle.getTitle() + " "+ newsArticle.getContent());
                    newsArticle.setRaw_stream_index(tmsRawStream);
                    Map<String,List<String>> neResultMap = TmsUtils.getTmsNeStr(newsArticle.getTitle() + " "+ newsArticle.getContent());
                    newsArticle.setNe_organization(neResultMap.get("OG"));
                    newsArticle.setNe_person(neResultMap.get("PS"));
                    newsArticle.setNe_location(neResultMap.get("LC"));
                    List<String> neAll = new ArrayList<>();
                    neAll.addAll(neResultMap.get("OG"));
                    neAll.addAll(neResultMap.get("PS"));
                    neAll.addAll(neResultMap.get("LC"));
                    StringBuilder neRawStrBuilder = getStringBuilder(tmsRawStream, neAll);
                    newsArticle.setNe_stream_index(neRawStrBuilder.toString());

                    newsArticlesFromFile.add(newsArticle);
                    System.out.println("JSON 라인 파싱 성공: " + newsArticle.getNews_id());
                } catch (JsonProcessingException jsonEx) {
                    System.err.println("JSON 라인 파싱 중 오류 발생 (라인 건너뛰기): " + line + " - " + jsonEx.getMessage());
                    // jsonEx.printStackTrace(); // 디버깅 시 필요
                }
            }
            System.out.println("indexData.txt 파일 읽기 및 모든 JSON 라인 처리 완료. 총 " + newsArticlesFromFile.size() + "개의 뉴스 기사 파싱됨.");
        } catch (Exception e) {
            System.err.println("indexData.txt 파일 읽기 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        if (!newsArticlesFromFile.isEmpty()) { // 파싱된 기사가 하나라도 있으면 저장
            operations.save(newsArticlesFromFile);

            // 기존 데이터 삭제 (선택 사항)
//            newsIndexRepository.deleteAll();
//            System.out.println("기존 NewsIndex 데이터 삭제 완료.");
//
//            // 파싱된 NewsArticle 리스트 저장
//            Iterable<NewsIndex> savedNewsArticles = newsIndexRepository.saveAll(newsArticlesFromFile);
//            System.out.println("저장된 NewsIndex 목록:");
//            savedNewsArticles.forEach(System.out::println);
//
//            indexNewCount = newsIndexRepository.count();
//            System.out.println("총 저장된 NewsIndex 개수: " + indexNewCount);

        } else {
            System.out.println("JSON 데이터를 읽어오지 못하거나 파싱된 NewsArticle이 없어 저장 건너뜀.");
        }

        System.out.println("JSON 텍스트 파싱 및 NewsArticle 데이터 저장 완료.");
        System.out.println("Elasticsearch의 'news_articles' 인덱스에 데이터가 저장되었는지 확인하세요.");

        return indexNewCount == newsArticlesFromFile.size();
    }

    private static StringBuilder getStringBuilder(String tmsRawStream, List<String> neAll) {
        StringBuilder neRawStrBuilder = new StringBuilder();

        for (String tokens : tmsRawStream.split("\n")) {
            for (String token : tokens.split(" ")) {
                if (neAll.contains(token) ) {
                    neRawStrBuilder.append(token).append(" ");
                }else if (token.contains("_") && (neAll.contains(token.split("_")[0]) || neAll.contains(token.split("_")[1])) ) {
                    neRawStrBuilder.append(token).append(" ");;
                }
            }
            neRawStrBuilder.append("\n");
        }
        return neRawStrBuilder;
    }
}
