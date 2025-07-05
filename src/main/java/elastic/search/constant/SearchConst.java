package elastic.search.constant;

import co.elastic.clients.elasticsearch._types.SortOrder;
import lombok.Data;
import lombok.Getter;


public class SearchConst {
    @Getter
    public static enum Analyzer {
        ANALYZER_KOR ("bigo_analyzer_korean"),
        ANALYZER_BIGRAM ("bigo_analyzer_bigram");

        public String value;
        Analyzer(String value) {
            this.value = value;
        }
    }

    public static enum BoolQuery {
        SHOULD, MUST, MUST_NOT
    }

    // 회사생활 가이드 검색 필드
    public static enum SearchField {
        TITLE_MORPH ("title.morph",10),
        CONTENT_MORPH ("content.morph",2),
        CONTENT_BIGRAM ("content.bigram", 1),
        COLLAPSE_FIELD ("doc_id", 1);

        private String value;
        private float boost;
        private SearchField(String value, float boost) {
            this.value = value;
            this.boost = boost;
        }
        public String getValue() {return this.value;}
        public float getBoost() {return this.boost;}
    }

    // 회사생활 가이드 검색 필드
    public static enum SortField {
        CATEGORY ("category", SortOrder.Desc),
        DATE_NEW ("regist_date",SortOrder.Desc),
        DATE_OLD ("regist_date",SortOrder.Asc),
        SCORE ("_score",SortOrder.Desc);

        private String value;
        private SortOrder sortOrder;
        private SortField(String value, SortOrder sortOrder) {
            this.value = value;
            this.sortOrder = sortOrder;
        }
        public String getValue() {return this.value;}
        public SortOrder getSortOrder() {return this.sortOrder;}
    }

    public static enum HighlightFieldParams {
        TITLE (200,0, 20, "<em>", "</em>"),
        CONTENT (40,0,5,"<em>", "</em>");

        private int fragmentSize;
        private int fragmentOffset;
        private int fragmentNum;
        private String preTag;
        private String postTag;


        private HighlightFieldParams(int fragmentSize, int fragmentOffset, int fragmentNum, String preTag, String postTag) {
            this.fragmentSize = fragmentSize;
            this.fragmentOffset = fragmentOffset;
            this.fragmentNum = fragmentNum;
            this.preTag = preTag;
            this.postTag = postTag;
        }
        public int getFragmentSize() {return this.fragmentSize;}
        public int getFragmentOffset() {return this.fragmentOffset;}
        public int getFragmentNum() {return this.fragmentNum;}
        public String getPreTag() {return this.preTag;}
        public String getPostTag() {return this.postTag;}
    }

}
