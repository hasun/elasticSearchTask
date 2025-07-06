package elastic.index.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "news")
public class NewsIndex {
    @Id
    @Field(type = FieldType.Keyword, name = "news_id")
    private String news_id;
    @Field(type = FieldType.Keyword, name = "published_at") // 날짜 타입 필드
    private String published_at;
    @Field(type = FieldType.Keyword, name = "category")
    private List<String> category;
    @Field(type = FieldType.Keyword, name = "provider_code")
    private String provider_code;
    @Field(type = FieldType.Keyword, name = "byline")
    private String byline;
    @Field(type = FieldType.Keyword, name = "raw_stream_index", index = false, store = true)
    private String raw_stream_index;
    @Field(type = FieldType.Keyword, name = "ne_stream_index", index = false, store = true)
    private String ne_stream_index;

    @Field(type = FieldType.Keyword, name = "ne_organization", index = false, store = true)
    private List<String> ne_organization;
    @Field(type = FieldType.Keyword, name = "ne_person", index = false, store = true)
    private List<String> ne_person;
    @Field(type = FieldType.Keyword, name = "ne_location", index = false, store = true)
    private List<String> ne_location;

    @MultiField(
            mainField = @Field(type = FieldType.Text, name = "title"),
            otherFields = {
                    @InnerField(suffix = "morph", type = FieldType.Text, analyzer = "bigo_analyzer_korean", searchAnalyzer = "bigo_analyzer_korean", index = true, store = false),
                    @InnerField(suffix = "bigram", type = FieldType.Text, analyzer = "bigo_analyzer_bigram", searchAnalyzer = "bigo_analyzer_bigram", index = true, store = false)
            }
    )
    private String title;

    @MultiField(
            mainField = @Field(type = FieldType.Keyword, name = "content"),
            otherFields = {
                    @InnerField(suffix = "morph", type = FieldType.Text, analyzer ="bigo_analyzer_korean", searchAnalyzer = "bigo_analyzer_korean", index = true, store = false),
                    @InnerField(suffix = "bigram", type = FieldType.Text, analyzer ="bigo_analyzer_bigram", searchAnalyzer = "bigo_analyzer_bigram", index = true, store = false)
            }
    )
    private String content;
}

