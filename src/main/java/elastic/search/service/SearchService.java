package elastic.search.service;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.FieldCollapse;
import co.elastic.clients.json.JsonData;
import elastic.index.domain.NewsIndex;
import elastic.search.constant.SearchConst;
import elastic.search.dto.PageResult;
import elastic.search.dto.SearchRequest;
import elastic.search.repository.NewsRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.document.Explanation;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
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

    public PageResult<NewsIndex> dynamicSearch (SearchRequest request, Pageable pageable) {
        NativeQueryBuilder queryBuilder = new NativeQueryBuilder();
        // Query
        queryBuilder.withQuery(getBoolMustQuery(request)._toQuery());

        // collapse filed
        queryBuilder.withFieldCollapse(getFieldCollapse());
//
////        queryBuilder.withAggregation("total", getCardinalityAggregation()._toAggregation());
        TermsAggregation termsAggregation = new TermsAggregation.Builder().field("category").build();
        Aggregation termAggregation = new Aggregation.Builder().terms(termsAggregation).build();
//        Aggregation cardinalityAggregation = new Aggregation.Builder().cardinality(getCardinalityAggregation()).build();
        Aggregation scriptedMetricAggregation = new Aggregation.Builder().scriptedMetric(getScriptedMetricAggregation()).build();
////        Aggregation totalAggregation = new Aggregation.Builder().aggregations("", termAggregation).aggregations("", cardinalityAggregation);
////        new Aggregation.Builder().aggregations("term" , termAggregation);
//
//
        queryBuilder.withAggregation("term", termAggregation);
//        queryBuilder.withAggregation("total", cardinalityAggregation);
        queryBuilder.withAggregation("total", scriptedMetricAggregation);
//
//        // highlight
        queryBuilder.withHighlightQuery(new HighlightQuery(getHighlight(), NewsIndex.class));
//
//        // page
        queryBuilder.withPageable(pageable);
        queryBuilder.withTrackTotalHits(true);
//
//        // return field
        queryBuilder.withFields("title","content","category", "published_at");
//        queryBuilder.withStoredFields(List.of("title","content","category","regist_date", "published_at", "index_name"));
//
//        // sort
        queryBuilder.withSort(getSortOptions());
//
//        // filter
        if (request.getCategories() != null) queryBuilder.withFilter(getFilterQuery(request));
//
//        // debug
        queryBuilder.withExplain(true);

        // page list return
        SearchPage<NewsIndex> indexSearchPage = SearchHitSupport.searchPageFor(operations.search(queryBuilder.build(), NewsIndex.class), pageable);

        // page result mapping // highlight filed mapping
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
//
        long totalCount = indexSearchPage.getTotalElements();
//
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

    private static CardinalityAggregation getCardinalityAggregation() {
        // collapse field 값에 대한 total count 값이 없을 때 해당 값에 대하여 Aggregation cardinality 함수 적용 카운트 값 확인 필요
        return AggregationBuilders.cardinality()
                .field(SearchConst.SearchField.COLLAPSE_FIELD.getValue())
                .precisionThreshold(1000)
                .build();
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
        sortOptions.add(SortOptions.of((b) -> {
            FieldSort fieldSort = new FieldSort.Builder().field("category").order(SortOrder.Desc).build();
            return b.field(fieldSort);
        }));
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
//                .withRequireFieldMatch(true)
                .withPreTags(SearchConst.HighlightFieldParams.TITLE.getPreTag())
                .withPostTags(SearchConst.HighlightFieldParams.TITLE.getPostTag())
                .build();

        // 하이라이트 필드 설정 (검색 필드에 해당하는 필드만 하이라이트 적용됨 (엘라스틱 서치 기준))
        HighlightField highlightTitle = new HighlightField(SearchConst.SearchField.TITLE_MORPH.getValue(), highlightParameters);
        HighlightField highlightContentMorph = new HighlightField(SearchConst.SearchField.CONTENT_MORPH.getValue(), highlightParameters);
//        HighlightField highlightContentBigram = new HighlightField(SearchConst.SearchField.CONTENT_BIGRAM.getValue(), highlightParameters);

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
        BoolQuery searchQueries = getBoolShouldQuery(request.getQuery(), request.getOperator());

        BoolQuery.Builder reSearchQueryBuilder = new BoolQuery.Builder();
        reSearchQueryBuilder.must(searchQueries._toQuery());
        // 재검색 필드 가중치 조합 불린 쿼리
        // 재검색은 5번까지 가능
        if (request.getRequery() != null) {
            for (String reQuery : request.getRequery()) {
                reSearchQueryBuilder.must(getBoolShouldQuery(reQuery, request.getOperator())._toQuery());
            }
        }

        if (request.getCategories()!= null) reSearchQueryBuilder.filter(getFilterQuery(request));

        return reSearchQueryBuilder.build();
    }

    private static Query getFilterQuery(SearchRequest request) {
        Query filterQuery = null;

        List<FieldValue> fieldValueList = new ArrayList<>();
        for (String category : request.getCategories()) {
            fieldValueList.add(new FieldValue.Builder().stringValue(category).build());
        }
        TermsQueryField termsQueryCategoryField = new TermsQueryField.Builder()
                .value(fieldValueList)
                .build();
        filterQuery = QueryBuilders.terms().field("-").terms(termsQueryCategoryField).build()._toQuery();

        return filterQuery;
    }



    private static BoolQuery getBoolShouldQuery(String query, Operator operator ) {

        QueryStringQuery korContentQuery =
                getQueryStringQuery(query, SearchConst.Analyzer.ANALYZER_KOR, SearchConst.SearchField.CONTENT_MORPH , operator);
        QueryStringQuery bigramContentQuery =
                getQueryStringQuery(query, SearchConst.Analyzer.ANALYZER_BIGRAM, SearchConst.SearchField.CONTENT_BIGRAM, operator);
        QueryStringQuery korTitleQuery =
                getQueryStringQuery(query, SearchConst.Analyzer.ANALYZER_KOR, SearchConst.SearchField.TITLE_MORPH, operator);

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        boolQueryBuilder.must(List.of(korContentQuery._toQuery(), bigramContentQuery._toQuery(), korTitleQuery._toQuery()));
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
