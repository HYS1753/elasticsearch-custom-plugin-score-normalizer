package elasticsearch.custom.plugin.rescorer;

import elasticsearch.custom.plugin.enumeration.NormalizerType;
import elasticsearch.custom.plugin.rescorer.normalizer.CustomNormalizerSelector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.search.rescore.RescoreContext;
import org.elasticsearch.search.rescore.Rescorer;

import java.io.IOException;

import static java.util.Collections.singletonList;

public class NormalizerRescorer implements Rescorer {

    public static final Rescorer INSTANCE = new NormalizerRescorer();

    /**
     * Describes the score computation for document and query.
     * @param topLevelDocId
     * @param indexSearcher
     * @param rescoreContext
     * @param explanation
     * @return
     * @throws IOException
     */
    @Override
    public Explanation explain(
            int topLevelDocId,
            IndexSearcher indexSearcher,
            RescoreContext rescoreContext,
            Explanation explanation) throws IOException {

        NormalizerRescorerContext context = (NormalizerRescorerContext) rescoreContext;
        String factorMode = context.getFactorMode();
        float factor = context.getFactor();
        String operation = factorMode + " using " + factor + " on:";

        return Explanation.match(
                0.0f,
                "Final score -> normalize using, " + context.getNormalizerType() + " and then " + operation,
                singletonList(explanation)
        );
    }

    /**
     * 각 Elasticsearch Shard 에서 상위 K 개의 normalized 된 문서를 반환합니다.
     *
     * @param topDocs 주어진 쿼리를 통해 매칭된 상위 문서
     * @param indexSearcher index searcher
     * @param rescoreContext rescore 함수 를 위한 Context, params
     * @return 각 Elasticsearch Shard 에서 상위 K 개의 normalized 된 문서
     * @throws IOException
     */

    @Override
    public TopDocs rescore(
            TopDocs topDocs,
            IndexSearcher indexSearcher,
            RescoreContext rescoreContext) throws IOException {

        // rescore context 는 반드시 존재함.
        assert rescoreContext != null;

        /* TopDocs.scoreDocs : The Top hits for the query
           TopDocs.totalHits : TheTotal number of hits for the query. */
        if(topDocs == null || topDocs.scoreDocs.length == 0) {
            return topDocs;
        }

        // 기본 rescoreContext 에 추가로 Normalizer 에 필요한 context 정의
        NormalizerRescorerContext context = (NormalizerRescorerContext) rescoreContext;
        String normalizerType = context.normalizerType;

        // context에 따른 documents Normalize 실행.
        topDocs = CustomNormalizerSelector
                .getCustomNormalizer(NormalizerType.valueOf(normalizerType))
                .normalize(topDocs, context);

        return topDocs;
    }

    public static class NormalizerRescorerContext extends RescoreContext {
        private String normalizerType;
        private float minScore;
        private float maxScore;
        private float factor;
        private String factorMode;
        private String minMaxSameScoreStrategy;

        public NormalizerRescorerContext(
                int windowSize,
                @Nullable String normalizerType,
                @Nullable float minScore,
                @Nullable float maxScore,
                @Nullable float factor,
                @Nullable String factorMode,
                @Nullable String minMaxSameScoreStrategy) {
            super(windowSize, INSTANCE);
            this.minScore = minScore;
            this.maxScore = maxScore;
            this.normalizerType = normalizerType;
            this.factorMode = factorMode;
            this.factor = factor;
            this.minMaxSameScoreStrategy = minMaxSameScoreStrategy;
        }

        public String getNormalizerType() {
            return normalizerType;
        }

        public void setNormalizerType(String normalizerType) {
            this.normalizerType = normalizerType;
        }

        public float getMinScore() {
            return minScore;
        }

        public void setMinScore(float minScore) {
            this.minScore = minScore;
        }

        public float getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(float maxScore) {
            this.maxScore = maxScore;
        }

        public float getFactor() {
            return factor;
        }

        public void setFactor(float factor) {
            this.factor = factor;
        }

        public String getFactorMode() {
            return factorMode;
        }

        public void setFactorMode(String factorMode) {
            this.factorMode = factorMode;
        }

        public String getMinMaxSameScoreStrategy() {
            return minMaxSameScoreStrategy;
        }

        public void setMinMaxSameScoreStrategy(String minMaxSameScoreStrategy) {
            this.minMaxSameScoreStrategy = minMaxSameScoreStrategy;
        }
    }
}
