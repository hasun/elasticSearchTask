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

    @MultiField(
            mainField = @Field(type = FieldType.Keyword, name = "title"),
            otherFields = {
                    @InnerField(suffix = "morph", type = FieldType.Text, analyzer ="bigo_analyzer_korean"),
                    @InnerField(suffix = "bigram", type = FieldType.Text, analyzer ="bigo_analyzer_bigram")
            }
    )
    private String title;

    @MultiField(
            mainField = @Field(type = FieldType.Keyword, name = "content"),
            otherFields = {
                    @InnerField(suffix = "morph", type = FieldType.Text, analyzer ="bigo_analyzer_korean"),
                    @InnerField(suffix = "bigram", type = FieldType.Text, analyzer ="bigo_analyzer_bigram")
            }
    )
    private String content;
}

