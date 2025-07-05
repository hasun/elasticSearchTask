package elastic.search.dto;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;

@Data
@Builder
public class SearchRequest {
    private String query;
    private int size;
    private int page;
    private String [] requery;
    private HashMap sort;
    private String [] termAggregationField;
    private String [] categories;
    private Operator operator;
}
