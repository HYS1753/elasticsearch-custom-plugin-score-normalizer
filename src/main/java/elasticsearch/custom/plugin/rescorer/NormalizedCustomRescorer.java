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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NormalizedCustomRescorer implements Rescorer {

    public static final Rescorer INSTANCE = new NormalizedCustomRescorer();

    public static class DocScoreDebugInfo {
        private final float originalScore;
        private final float normalizedScoreBeforeFactor;
        private final float finalScore;
        private final String factorMode;
        private final float factor;

        public DocScoreDebugInfo(
                float originalScore,
                float normalizedScoreBeforeFactor,
                float finalScore,
                String factorMode,
                float factor) {
            this.originalScore = originalScore;
            this.normalizedScoreBeforeFactor = normalizedScoreBeforeFactor;
            this.finalScore = finalScore;
            this.factorMode = factorMode;
            this.factor = factor;
        }

        public float getOriginalScore() { return originalScore; }
        public float getNormalizedScoreBeforeFactor() { return normalizedScoreBeforeFactor; }
        public float getFinalScore() { return finalScore; }
        public String getFactorMode() { return factorMode; }
        public float getFactor() { return factor; }
    }

    public static class NormalizerRescorerContext extends RescoreContext {
        // Factors
        private String normalizerType;
        private float minScore;
        private float maxScore;
        private float factor;
        private String factorMode;
        private String minMaxSameScoreStrategy;

        // explain
        private final Map<String, Float> stats = new HashMap<>();
        private final Map<Integer, DocScoreDebugInfo> debugInfoByDocId = new HashMap<>();

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

        public void putStat(String key, float value) {
            stats.put(key, value);
        }

        public Map<String, Float> getStats() {
            return stats;
        }

        public void putDebugInfo(int docId, DocScoreDebugInfo info) {
            debugInfoByDocId.put(docId, info);
        }

        public DocScoreDebugInfo getDebugInfo(int docId) {
            return debugInfoByDocId.get(docId);
        }
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
        DocScoreDebugInfo debugInfo = context.getDebugInfo(topLevelDocId);

        if (debugInfo == null) {
            return Explanation.match(
                    explanation.getValue().floatValue(),
                    "rescoring was not applied for docId=" + topLevelDocId +
                            " (document may be outside the rescore window)",
                    List.of(explanation)
            );
        }

        String normalizerType = context.getNormalizerType();
        String factorMode = context.getFactorMode();

        List<Explanation> topLevelDetails = new ArrayList<>();

        // 1) original query score
        topLevelDetails.add(
                Explanation.match(
                        debugInfo.getOriginalScore(),
                        "original query score",
                        List.of(explanation)
                )
        );

        // 2) normalized score explanation
        List<Explanation> normalizationDetails = new ArrayList<>();

        if ("min_max".equals(normalizerType)) {
            normalizationDetails.add(Explanation.match(
                    context.getStats().getOrDefault("topDocsMinScore", Float.NaN),
                    "top docs min score"
            ));
            normalizationDetails.add(Explanation.match(
                    context.getStats().getOrDefault("topDocsMaxScore", Float.NaN),
                    "top docs max score"
            ));
            normalizationDetails.add(Explanation.match(
                    context.getStats().getOrDefault("userMinScore", Float.NaN),
                    "configured min score"
            ));
            normalizationDetails.add(Explanation.match(
                    context.getStats().getOrDefault("userMaxScore", Float.NaN),
                    "configured max score"
            ));

            topLevelDetails.add(
                    Explanation.match(
                            debugInfo.getNormalizedScoreBeforeFactor(),
                            "normalized score using min_max",
                            normalizationDetails
                    )
            );

        } else if ("z_score".equals(normalizerType)) {
            normalizationDetails.add(Explanation.match(
                    context.getStats().getOrDefault("meanScore", Float.NaN),
                    "mean score"
            ));
            normalizationDetails.add(Explanation.match(
                    context.getStats().getOrDefault("standardDeviation", Float.NaN),
                    "standard deviation"
            ));

            topLevelDetails.add(
                    Explanation.match(
                            debugInfo.getNormalizedScoreBeforeFactor(),
                            "normalized score using z_score",
                            normalizationDetails
                    )
            );

        } else if ("robust".equals(normalizerType)) {
            normalizationDetails.add(Explanation.match(
                    context.getStats().getOrDefault("median", Float.NaN),
                    "median"
            ));
            normalizationDetails.add(Explanation.match(
                    context.getStats().getOrDefault("iqr", Float.NaN),
                    "interquartile range (IQR)"
            ));

            topLevelDetails.add(
                    Explanation.match(
                            debugInfo.getNormalizedScoreBeforeFactor(),
                            "normalized score using robust scaling",
                            normalizationDetails
                    )
            );

        } else {
            topLevelDetails.add(
                    Explanation.match(
                            debugInfo.getNormalizedScoreBeforeFactor(),
                            "normalized score using unknown normalizer [" + normalizerType + "]"
                    )
            );
        }

        // 3) factor-applied final score explanation
        List<Explanation> factorDetails = new ArrayList<>();
        factorDetails.add(Explanation.match(
                debugInfo.getNormalizedScoreBeforeFactor(),
                "score before factor application"
        ));
        factorDetails.add(Explanation.match(
                context.getFactor(),
                "factor"
        ));
        factorDetails.add(Explanation.match(
                0f,
                "factor mode [" + factorMode + "]"
        ));

        if (("z_score".equals(normalizerType) || "robust".equals(normalizerType))
                && ("multiply".equals(factorMode) || "increase_by_percent".equals(factorMode))) {
            factorDetails.add(Explanation.match(
                    0f,
                    "abs(normalized score) is used before factor application"
            ));
        }

        topLevelDetails.add(
                Explanation.match(
                        debugInfo.getFinalScore(),
                        "score after factor application",
                        factorDetails
                )
        );

        return Explanation.match(
                debugInfo.getFinalScore(),
                "score_normalizer_rescore[type=" + normalizerType + "]",
                topLevelDetails
        );
    }
}
