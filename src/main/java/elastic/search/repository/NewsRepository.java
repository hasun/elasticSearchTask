package elastic.search.repository;

import elastic.index.domain.NewsIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface NewsRepository extends ElasticsearchRepository<NewsIndex, String> {

    // 1. 불리언 AND 검색: 이름 AND 카테고리 (must 쿼리)
    // ?0, ?1은 메서드 파라미터를 의미
    @Query("""
        {
            "multi_match": {
                "query": "?0",
                "fields": ["title.morph^2" , "content.morph^1", "content.bigram^0.5"]
            }
        }
    """)
    Page<NewsIndex> findByKeywordWithQuery(String keyword, Pageable pageable);

    // 2. 불리언 OR 검색: 이름 OR 설명 (should 쿼리)
    // should 내의 쿼리 중 하나 이상이 만족되면 됨. min_should_match로 만족해야 하는 최소 쿼리 수 지정 가능.
    @Query("""
        {
          "bool": {
            "should": [
              { "match": { "title": "?0" } },
              { "match": { "content": "?1" } }
            ],
            "minimum_should_match": 1
          }
        }
    """)
    Page<NewsIndex> findByNameOrDescriptionWithQuery(String nameKeyword, String descriptionKeyword, Pageable pageable);

    // 3. 불리언 NOT 검색: 이름 AND NOT 카테고리 (must_not 쿼리)
    @Query("""
        {
          "bool": {
            "must": [
              { "match": { "name": "?0" } }
            ],
            "must_not": [
              { "term": { "category.keyword": "?1" } }
            ]
          }
        }
    """)
    Page<NewsIndex> findByNameAndNotCategoryWithQuery(String nameKeyword, String excludedCategory, Pageable pageable);

    // 4. 복합 불리언 쿼리 (AND, OR, NOT 조합)
    // (이름에 'laptop'이 포함되거나 가격이 1000.0 이상이고) AND (카테고리가 'Electronics'이면서 available이 true가 아닌)
    @Query("""
        {
          "bool": {
            "must": [
              {
                "bool": {
                  "should": [
                    { "match": { "name": "?0" } },
                    { "range": { "published_at": { "gte": "?1" } } }
                  ],
                  "minimum_should_match": 1
                }
              },
              { "term": { "category.keyword": "?2" } }
            ],
            "must_not": [
              { "term": { "available": true } }
            ]
          }
        }
    """)
    Page<NewsIndex> findComplexProductsWithQuery(String nameKeyword, Double minPrice, String category, Pageable pageable);
}
