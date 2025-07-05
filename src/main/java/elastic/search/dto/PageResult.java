package elastic.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.document.Explanation;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class PageResult <T>{
    private long total;
    private List<T> list;
    private Pageable pageable;
    private Map<String, Map<String, Long>> aggregatedData;
    private List<Explanation> explanationList;

    public PageResult(long totalCount, List<T> dataList, Pageable pageable) {
        this.total = totalCount;
        this.list = dataList;
        this.pageable = pageable;
    }

    public PageResult( long totalCount, List<T> dataList, Pageable pageable, List<Explanation> explanationList, Map<String, Map<String, Long>> aggregatedData ) {
        this.total = totalCount;
        this.list = dataList;
        this.pageable = pageable;
        this.explanationList = explanationList;
        this.aggregatedData = aggregatedData;
    }
}
