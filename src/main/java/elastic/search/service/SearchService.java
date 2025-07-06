package elastic.search.service;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.FieldCollapse;
import co.elastic.clients.json.JsonData;
import com.saltlux.util.topicrank.TopicRankDocument;
import elastic.index.domain.NewsIndex;
import elastic.search.constant.SearchConst;
import elastic.search.dto.PageResult;
import elastic.search.dto.SearchRequest;
import elastic.search.repository.NewsRepository;
import com.saltlux.util.topicrank.TopicRankUtil;
import elastic.tms.util.TmsUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.document.Explanation;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private final ElasticsearchOperations operations;
    private final NewsRepository newsRepository;

    public SearchService(ElasticsearchOperations operations, NewsRepository newsRepository)  {
        this.operations = operations;
        this.newsRepository = newsRepository;
    }

    // 쿼리 메서드 활용
//    public List<NewsIndex> findProductsBySpecificConditions(String name, String category) {
//        return newsRepository.findByNameContainingAndCategory(name, category);
//    }
//
//    public List<NewsIndex> findProductsByNameOrDescription(String name, String description) {
//        return newsRepository.findByNameContainingOrDescriptionContaining(name, description);
//    }

    // @Query 어노테이션 활용
    public Page<NewsIndex> searchNewsByQueryDsl(@Valid String keyword, Pageable pageable) {
        return newsRepository.findByKeywordWithQuery(keyword, pageable);
    }

    public String topicRankResult (String query) {
        Pageable pageable = Pageable.ofSize(10).withPage(0);
        Page<NewsIndex> result = newsRepository.findByKeywordWithQuery(query, pageable);
        List<TopicRankDocument> topicRankDocumentList  = new ArrayList<>();

        for ( NewsIndex news : result.getContent()) {
            TopicRankDocument topicRankDocument = new TopicRankDocument(news.getNews_id(),news.getRaw_stream_index());
            topicRankDocumentList.add(topicRankDocument);
        }
        TopicRankUtil topicRankUtil = new TopicRankUtil();
        String text = topicRankUtil.getTopicRankJson(topicRankDocumentList,2,query,15);
        log.info("topicRank Result Graph {}", text);
        return text;
    }

    public String namedEntityResult (String query) {
        Pageable pageable = Pageable.ofSize(10).withPage(0);
        Page<NewsIndex> result = newsRepository.findByKeywordWithQuery(query, pageable);
        List<TopicRankDocument> topicRankDocumentList  = new ArrayList<>();

        for ( NewsIndex news : result.getContent()) {
            TopicRankDocument neDocument = new TopicRankDocument(news.getNews_id(), news.getNe_stream_index());
            topicRankDocumentList.add(neDocument);
        }
        TopicRankUtil topicRankUtil = new TopicRankUtil();
        String text = topicRankUtil.getTopicRankJson(topicRankDocumentList,2,query,10);
        log.info("neRank Result Graph {}", text);
        return text;
    }

    public Map <String, Object> getSentimentAnalyze (String sentence) {
        Map <String, Object> result;
        try {
            result = TmsUtils.getTextAnalyzer(sentence);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("sentiment Result {}", result);
        return result;
    }

    /**
     * Span Near 검색: "데이터"와 "분석" 단어가 최대 2단어 이내, 순서대로 나타나는 문서 검색
     *
     * @param keyword1 첫 번째 단어
     * @param keyword2 두 번째 단어
     * @param slop     최대 간격 (단어 수)
     * @param inOrder  순서 유지 여부
     * @param pageable 페이징 정보
     * @return 검색 결과 페이지
     */
    public Page<NewsIndex> searchBySpanNear(String keyword1, String keyword2, int slop, boolean inOrder, Pageable pageable) {
        // Build the span_near query
        SpanNearQuery.Builder spanNearQueryBuilder = new SpanNearQuery.Builder();
        SpanQuery.Builder spanQueryBuilder = new SpanQuery.Builder();
        SpanQuery spanQuery1 = spanQueryBuilder.spanTerm(SpanTermQuery.of(st -> st.field("title.morph").value(keyword1))).build();
        spanQueryBuilder = new SpanQuery.Builder();
        SpanQuery spanQuery2 = spanQueryBuilder.spanTerm(SpanTermQuery.of(st -> st.field("title.morph").value(keyword2))).build();
        spanNearQueryBuilder.clauses(spanQuery1, spanQuery2).slop(slop).inOrder(inOrder);
        SpanNearQuery spanNearQuery = spanNearQueryBuilder.build();

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(spanNearQuery._toQuery())
                .withPageable(pageable)
                .build();

        SearchHits<NewsIndex> searchHits = operations.search(searchQuery, NewsIndex.class);
        return new PageImpl<>(searchHits.stream().map(h -> h.getContent()).collect(Collectors.toList()), pageable, searchHits.getTotalHits());
    }

//    /**
//     * 불리언 연산자 + Span Near 쿼리 (Bool Query with Span Near)
//     * - "빅데이터" 또는 "AI"를 포함하고 (should)
//     * - "데이터"와 "분석"이 가까이 있는 (must)
//     * - "개발" 카테고리가 아닌 (must_not) 문서 검색
//     */
//    public Page<Article> searchComplexWithSpanNear(String generalKeyword1, String generalKeyword2, String spanKeyword1, String spanKeyword2, String excludeCategory, int slop, boolean inOrder, Pageable pageable) {
//        BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();
//
//        // SHOULD (OR): 일반 키워드 매치
//        boolQueryBuilder.should(
//                QueryBuilders.match(m -> m.field("content").query(generalKeyword1))._toQuery(),
//                QueryBuilders.match(m -> m.field("content").query(generalKeyword2))._toQuery()
//        ).minimumShouldMatch("1");
//
//        // MUST (AND): Span Near 쿼리
//        SpanNearQuery spanNearQuery = SpanNearQuery.of(sn -> sn
//                .clauses(
//                        SpanTermQuery.of(st -> st.field("content").value(spanKeyword1))._toQuery(),
//                        SpanTermQuery.of(st -> st.field("content").value(spanKeyword2))._toQuery()
//                )
//                .slop(slop)
//                .inOrder(inOrder)
//        );
//        boolQueryBuilder.must(spanNearQuery._toQuery());
//
//        // MUST_NOT (NOT): 특정 카테고리 제외
//        boolQueryBuilder.mustNot(
//                TermQuery.of(t -> t.field("author.keyword").value(excludeCategory))._toQuery()
//        );
//
//        NativeQuery searchQuery = NativeQuery.builder()
//                .withQuery(boolQueryBuilder.build()._toQuery())
//                .withPageable(pageable)
//                .build();
//
//        SearchHits<Article> searchHits = elasticsearchOperations.search(searchQuery, Article.class);
//        return new PageImpl<>(searchHits.stream().map(h -> h.getContent()).collect(Collectors.toList()), pageable, searchHits.getTotalHits());
//    }

    public PageResult<NewsIndex> dynamicSearch (SearchRequest request, Pageable pageable) {
        NativeQueryBuilder queryBuilder = new NativeQueryBuilder();

        if (request.getQuery().matches(SearchConst.PATTENS_SYN)) {
            queryBuilder.withQuery(getBoolBigramQuery(request.getQuery())._toQuery());
        } else {
            queryBuilder.withQuery(getBoolMustQuery(request)._toQuery());
        }
        queryBuilder.withFieldCollapse(getFieldCollapse());

        TermsAggregation categoryAggregation = new TermsAggregation.Builder().field("category").build();
        TermsAggregation dateAggregation = new TermsAggregation.Builder().field("published_at").build();
//        Aggregation scriptedMetricAggregation = new Aggregation.Builder().scriptedMetric(getScriptedMetricAggregation()).build();
        queryBuilder.withAggregation("date", new Aggregation.Builder().terms(dateAggregation).build());
        queryBuilder.withAggregation("category", new Aggregation.Builder().terms(categoryAggregation).build());
//        queryBuilder.withAggregation("total", scriptedMetricAggregation);
//        queryBuilder.withHighlightQuery(new HighlightQuery(getHighlight(), NewsIndex.class));

        queryBuilder.withPageable(pageable);
        queryBuilder.withTrackTotalHits(true);

        queryBuilder.withFields("title","content","category", "published_at");
        queryBuilder.withSort(getSortOptions());

        if (request.getCategories() != null) queryBuilder.withFilter(getFilterQuery(request));

        queryBuilder.withExplain(false);

        SearchPage<NewsIndex> indexSearchPage = SearchHitSupport.searchPageFor(operations.search(queryBuilder.build(), NewsIndex.class), pageable);

        final List <NewsIndex> companyGuideList = indexSearchPage.stream()
                .map(s-> {
                    final NewsIndex content = s.getContent();
                    final List <String> title =  s.getHighlightFields().get(SearchConst.SearchField.TITLE_MORPH.getValue());
                    final List <String> contentList =  s.getHighlightFields().get(SearchConst.SearchField.CONTENT_MORPH.getValue());
                    if (!CollectionUtils.isEmpty(title)) {
                        s.getContent().setTitle(title.getFirst());
                    }
                    if (!CollectionUtils.isEmpty(contentList)) {
                        s.getContent().setContent(contentList.getFirst());
                    }
                    return content;

                }).collect(Collectors.toList());

        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) indexSearchPage.getSearchHits().getAggregations();
        List<ElasticsearchAggregation> aggregationsList = aggregations.aggregations();

        long totalCount = indexSearchPage.getTotalElements();
        Map<String, Map<String, Long>> aggregatedData = new HashMap<>();
        Map <String, Long> aggregatedTermData = new HashMap<>();
        for ( ElasticsearchAggregation aggregation : aggregationsList) {
            if (aggregation.aggregation().getAggregate().isCardinality()) {
                CardinalityAggregate cardinalityAggregate = aggregation.aggregation().getAggregate().cardinality();
                if (queryBuilder.getFieldCollapse() != null) totalCount = cardinalityAggregate.value();
            } else if (aggregation.aggregation().getAggregate().isSterms()) {
                aggregatedTermData = new HashMap<>();
                Buckets<StringTermsBucket> buckets = aggregation.aggregation().getAggregate().sterms().buckets();
                aggregatedTermData = buckets.array().stream().collect(Collectors.toMap(bucket -> bucket.key().stringValue(), MultiBucketBase::docCount));
                aggregatedData.put( aggregation.aggregation().getName() ,aggregatedTermData );
            }
        }

        // page result mapping // highlight filed mapping
        final List <Explanation> explanationList = indexSearchPage.stream().map(SearchHit::getExplanation).toList();
        return new PageResult<NewsIndex>(totalCount, companyGuideList, pageable, explanationList, aggregatedData);
    }

    private static ScriptedMetricAggregation getScriptedMetricAggregation() {
        // collapse field 값에 대한 total count 값이 없을 때 해당 값에 대하여 Aggregation cardinality 함수 적용 카운트 값 확인 필요
        Script initScript = new Script.Builder().inline(new InlineScript.Builder().lang("painless").source("state.list = []").build()).build();
        Script mapScript = new Script.Builder().inline(new InlineScript.Builder().lang("painless").source("if(doc[params.fieldName] != null)\n" +
                "          state.list.add(doc[params.fieldName].value);").build()).build();
        Script combineScript = new Script.Builder().inline(new InlineScript.Builder().lang("painless").source("return state.list;").build()).build();
        Script reduceScript = new Script.Builder().inline(new InlineScript.Builder().lang("painless").source("Map uniqueValueMap = new HashMap(); \n" +
                "        int count = 0;\n" +
                "        for(shardList in states) {\n" +
                "          if(shardList != null) { \n" +
                "            for(key in shardList) {\n" +
                "              if(!uniqueValueMap.containsKey(key)) {\n" +
                "                count +=1;\n" +
                "                uniqueValueMap.put(key, key);\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        } \n" +
                "        return count;\n").build()).build();

        ScriptedMetricAggregation scriptedMetricAggregation = new ScriptedMetricAggregation.Builder()
                .params("fieldName" , JsonData.of("doc_id"))
                .initScript(initScript)
                .mapScript(mapScript)
                .combineScript(combineScript)
                .reduceScript(reduceScript)
                .build();
        return scriptedMetricAggregation;
    }

    private static List<SortOptions> getSortOptions() {
        List<SortOptions> sortOptions = new ArrayList<>();
//        sortOptions.add(SortOptions.of((b) -> {
//            FieldSort fieldSort = new FieldSort.Builder().field("category").order(SortOrder.Desc).build();
//            return b.field(fieldSort);
//        }));
        sortOptions.add(SortOptions.of((b) -> {
            FieldSort fieldSort = new FieldSort.Builder().field("_score").order(SortOrder.Desc).build();
            return b.field(fieldSort);
        }));
        return sortOptions;
    }


    private static Highlight getHighlight() {
        // 하이라이트 파라미터 설정
        HighlightFieldParameters highlightParameters = new HighlightFieldParameters.HighlightFieldParametersBuilder()
                .withFragmentOffset(SearchConst.HighlightFieldParams.TITLE.getFragmentOffset())
                .withFragmentSize(SearchConst.HighlightFieldParams.TITLE.getFragmentSize())
                .withNumberOfFragments(SearchConst.HighlightFieldParams.TITLE.getFragmentNum())
                .withRequireFieldMatch(true)
                .withPreTags(SearchConst.HighlightFieldParams.TITLE.getPreTag())
                .withPostTags(SearchConst.HighlightFieldParams.TITLE.getPostTag())
                .build();

        // 하이라이트 필드 설정 (검색 필드에 해당하는 필드만 하이라이트 적용됨 (엘라스틱 서치 기준))
        HighlightField highlightTitle = new HighlightField(SearchConst.SearchField.TITLE_MORPH.getValue(), highlightParameters);
        HighlightField highlightContentMorph = new HighlightField(SearchConst.SearchField.CONTENT_MORPH.getValue(), highlightParameters);

        Highlight highlight = new Highlight(List.of(highlightTitle, highlightContentMorph));

        return highlight;
    }


    private static FieldCollapse getFieldCollapse() {
        // 중복 키 필드 그룹핑 처리
        FieldCollapse fieldCollapse = new FieldCollapse.Builder()
                .field(SearchConst.SearchField.COLLAPSE_FIELD.getValue())
                .build();
        return fieldCollapse;
    }


    private static BoolQuery getBoolMustQuery(SearchRequest request) {
        // 검색 필드 가중치 조합 불린 쿼리
        BoolQuery searchQueries = getBoolShouldQuery(request.getQuery());

        BoolQuery.Builder reSearchQueryBuilder = new BoolQuery.Builder();
        reSearchQueryBuilder.must(searchQueries._toQuery());
        // 재검색 필드 가중치 조합 불린 쿼리
        // 재검색은 5번까지 가능
        if (request.getRequery() != null) {
            for (String reQuery : request.getRequery()) {
                reSearchQueryBuilder.must(getBoolShouldQuery(reQuery)._toQuery());
            }
        }
        return reSearchQueryBuilder.build();
    }

    private static Query getFilterQuery(SearchRequest request) {
        List<FieldValue> fieldValueList = new ArrayList<>();
        for (String category : request.getCategories()) {
            fieldValueList.add(new FieldValue.Builder().stringValue(category).build());
        }
        TermsQueryField termsQueryCategoryField = new TermsQueryField.Builder()
                .value(fieldValueList)
                .build();
        Query filterQuery = QueryBuilders.terms().field("category").terms(termsQueryCategoryField).build()._toQuery();

        return filterQuery;
    }

    private static BoolQuery getBoolShouldQuery(String query) {

        QueryStringQuery korContentQuery =
                getQueryStringQuery(query, SearchConst.Analyzer.ANALYZER_KOR, SearchConst.SearchField.CONTENT_MORPH , Operator.And);
        QueryStringQuery bigramContentQuery =
                getQueryStringQuery(query, SearchConst.Analyzer.ANALYZER_BIGRAM, SearchConst.SearchField.CONTENT_BIGRAM, Operator.And);
        QueryStringQuery korTitleQuery =
                getQueryStringQuery(query, SearchConst.Analyzer.ANALYZER_KOR, SearchConst.SearchField.TITLE_MORPH, Operator.And);

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        boolQueryBuilder.should(List.of(korContentQuery._toQuery(), bigramContentQuery._toQuery(), korTitleQuery._toQuery()));
        return boolQueryBuilder.build();
    }


    private static BoolQuery getBoolBigramQuery(String query) {
        QueryStringQuery bigramContentQuery =
                getQueryStringQuery(query, SearchConst.Analyzer.ANALYZER_BIGRAM, SearchConst.SearchField.CONTENT_BIGRAM, Operator.And);
        QueryStringQuery bigramTitleQuery =
                getQueryStringQuery(query, SearchConst.Analyzer.ANALYZER_BIGRAM, SearchConst.SearchField.TITLE_BIGRAM, Operator.And);

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        boolQueryBuilder.should(List.of(bigramContentQuery._toQuery(), bigramTitleQuery._toQuery()));
        return boolQueryBuilder.build();
    }


    private static QueryStringQuery getQueryStringQuery(String query, SearchConst.Analyzer analyzer, SearchConst.SearchField searchField, Operator operator) {
        return new QueryStringQuery.Builder()
                .analyzer(analyzer.getValue())
                .autoGenerateSynonymsPhraseQuery(false)
                .defaultOperator(operator)
                .fields(List.of(searchField.getValue()))
                .query(query)
                .boost(searchField.getBoost())
                .build();
    }


}
