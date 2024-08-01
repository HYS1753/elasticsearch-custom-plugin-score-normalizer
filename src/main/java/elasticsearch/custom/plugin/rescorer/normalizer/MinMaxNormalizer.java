package elasticsearch.custom.plugin.rescorer.normalizer;

import elasticsearch.custom.plugin.rescorer.NormalizerRescorer;
import elasticsearch.custom.plugin.enumeration.MinMaxSameScoreStrategy;
import elasticsearch.custom.plugin.enumeration.NormalizerFactorOperation;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class MinMaxNormalizer implements CustomNormalizer {

    /**
     * 사용자 쿼리를 통해 매칭된 도큐먼트들의 score를 min max 정규화 합니다.
     * (similarity score 가 0과 1사이로 조정)
     *
     * @param topDocs 각 샤드에서 전달 받은 상위 매칭 도큐먼트
     * @param rescorerContext : Rescorer context (환경 정의 변수)
     * @return
     */
    @Override
    public TopDocs normalize(TopDocs topDocs, NormalizerRescorer.NormalizerRescorerContext rescorerContext) {

        if (rescorerContext.getMinScore() >= rescorerContext.getMaxScore()) {
            throw new IllegalArgumentException(
                    "maxScore value cannot be less than or equal to minScore value");
        }

        if (topDocs.scoreDocs.length == 0) {
            return topDocs;
        }

        if (topDocs.scoreDocs.length == 1) {
            topDocs.scoreDocs[0].score = applyFactorToNormalizedScore(
                    rescorerContext.getFactorMode(), rescorerContext.getFactor(), rescorerContext.getMaxScore());
            return topDocs;
        }

        float topDocsMaxScore = topDocs.scoreDocs[0].score;
        float topDocsMinScore = topDocs.scoreDocs[topDocs.scoreDocs.length - 1].score;
        float userCalibratedMaxScore = rescorerContext.getMaxScore();
        float userCalibratedMinScore = rescorerContext.getMinScore();

        if (Float.compare(topDocsMaxScore, topDocsMinScore) == 0) {
            // 상위 매칭 도큐먼트의 최대, 최소 score 가 동일 할 경우
            String userMinMaxSameScoreStrategy = rescorerContext.getMinMaxSameScoreStrategy();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                if(userMinMaxSameScoreStrategy.equals(MinMaxSameScoreStrategy.max.name())) {
                    scoreDoc.score = userCalibratedMaxScore;
                } else if (userMinMaxSameScoreStrategy.equals(MinMaxSameScoreStrategy.min.name())) {
                    scoreDoc.score = userCalibratedMinScore;
                } else {    // avg
                    scoreDoc.score = (userCalibratedMaxScore + userCalibratedMinScore) / 2;
                }
            }
        } else {
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                float normalizedScore = calculateMinMaxScore(
                        scoreDoc.score,
                        topDocsMaxScore,
                        topDocsMinScore,
                        userCalibratedMaxScore,
                        userCalibratedMinScore);

                scoreDoc.score = applyFactorToNormalizedScore(
                        rescorerContext.getFactorMode(),
                        rescorerContext.getFactor(),
                        normalizedScore);
            }
        }

        return topDocs;
    }

    /**
     * min max normalization scale calculate
     *
     * @param currentDocScore   계산 대상 document score
     * @param topDocsMaxScore   상위 매칭 documents 중 max score
     * @param topDocsMinScore   상위 매칭 documents 중 min score
     * @param userCalibratedMaxScore    사용자 지정 max score 보정 값(final score 최대치)
     * @param userCalibratedMinScore    사용자 지정 min score 보정 값(final score 최소치)
     * @return
     */
    private static float calculateMinMaxScore(
            float currentDocScore,
            float topDocsMaxScore,
            float topDocsMinScore,
            float userCalibratedMaxScore,
            float userCalibratedMinScore) {
        return ((currentDocScore - topDocsMinScore) / (topDocsMaxScore - topDocsMinScore))      // min-max normalization (0 ~ 1)
                * (userCalibratedMaxScore - userCalibratedMinScore) + userCalibratedMinScore;   // 사용자 지정 Min,Max score 보정
    }

    /**
     * factor mode 에 따른 factor 값 normalized 결과에 적용
     *
     * @param factorMode    지정 가능 모드 (sum, multiply, increase_by_percent)
     * @param factor    factor 값.
     * @param normalizedScore   min max normalized document score
     * @return
     */
    private static float applyFactorToNormalizedScore(String factorMode, float factor, float normalizedScore) {

        if (factorMode.equals(NormalizerFactorOperation.sum.name())) {
            normalizedScore = normalizedScore + factor;
        } else if (factorMode.equals(NormalizerFactorOperation.multiply.name())) {
            normalizedScore = normalizedScore * factor;
        } else {
            if (normalizedScore == 0.0f) {
                normalizedScore = factor;
            } else {
                if (factor < 0 || factor > 1) {
                    throw new IllegalArgumentException(
                            "increase_by_percent factorMode allowed factor range 0 ~ 1");
                }
                normalizedScore = normalizedScore + normalizedScore * factor;
            }
        }
        return normalizedScore;
    }

}
