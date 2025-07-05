package elastic.search.controller;

import elastic.index.domain.NewsIndex;
import elastic.search.dto.PageResult;
import elastic.search.dto.SearchRequest;
import elastic.search.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/dynamic")
    public PageResult<NewsIndex> findNewsIndexDynamic (@Validated @RequestBody SearchRequest searchRequest ) {
        final PageResult<NewsIndex> searchPage = searchService.dynamicSearch(searchRequest, getPageable(searchRequest));
        extractedResultPrint(searchPage.getPageable());
        return new ResponseEntity<>(searchPage, HttpStatus.OK).getBody();
    }

    @PostMapping("/query")
    public Page<NewsIndex> findNewsIndexQueryDsl (@Validated @RequestBody SearchRequest searchRequest ) {
        final Page<NewsIndex> searchPage = searchService.searchNewsByQueryDsl(searchRequest.getQuery(), getPageable(searchRequest));
        extractedResultPrint(searchPage.getPageable());
        return new ResponseEntity<>(searchPage, HttpStatus.OK).getBody();
    }

    private static void extractedResultPrint(Pageable searchPage) {
        System.out.print("offset ::" + searchPage.getOffset() + "\t");
        System.out.print("pageNum ::" + searchPage.getPageNumber() + "\t");
        System.out.println("pageSize ::" + searchPage.getPageSize());
    }

    private static Pageable getPageable(SearchRequest searchRequest) {
        return Pageable.ofSize(searchRequest.getSize()).withPage(searchRequest.getPage());
    }

}
