package elastic.index.controller;

import elastic.index.domain.NewsIndex;
import elastic.index.service.IndexCreateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news/index")
public class IndexCreateController {
    private final IndexCreateService indexCreateService;

    public IndexCreateController(IndexCreateService indexCreateService) {
        this.indexCreateService = indexCreateService;
    }

    @PostMapping("/create")
    public ResponseEntity <Boolean> newIndexCreate (@Validated @RequestBody List<NewsIndex> newsIndexList) {
        boolean indexCreated = indexCreateService.newsIndexCreate(newsIndexList);
        return new ResponseEntity<>(indexCreated, HttpStatus.OK);
    }
}
