package elasticsearch.custom.plugin.rescorer.normalizer;

import elasticsearch.custom.plugin.enumeration.NormalizerFactorOperation;
import elasticsearch.custom.plugin.rescorer.NormalizedCustomRescorer;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class ZScoreNormalizer implements CustomNormalizer {

    /**
     * 사용자 쿼리를 통해 매칭된 도큐먼트들의 score를 z-score 정규화 합니다.
     * (similarity score의 평균을 0, 표준편차를 1로 조정)
     *
     * @param topDocs   각 샤드에서 전달 받은 상위 매칭 도큐먼트
     * @param rescorerContext   Rescorer context (환경 정의 변수)
     * @return
     */
    @Override
    public TopDocs normalize(TopDocs topDocs, NormalizedCustomRescorer.NormalizerRescorerContext rescorerContext) {

        if (topDocs.scoreDocs.length == 0) {
            return topDocs;
        }

        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        float meanScore = getMeanScore(scoreDocs);
        float standardDeviation = getStandardDeviation(scoreDocs, meanScore);
        // 분모 0 나누기 방지
        standardDeviation = (standardDeviation == 0.0f) ? 1.0f : standardDeviation;

        for (ScoreDoc scoreDoc : scoreDocs) {
            float normalizedScore = calculateZScore(scoreDoc.score, meanScore, standardDeviation);
            scoreDoc.score = applyFactorToNormalizedScore(
                    rescorerContext.getFactorMode(),
                    rescorerContext.getFactor(),
                    normalizedScore);
        }
        return topDocs;
    }


    /**
     * 매칭된 도큐먼트 들의 평균 score 값 계산. (
     *
     * @param scoreDocs
     * @return
     */
    private float getMeanScore(ScoreDoc[] scoreDocs) {
        float total = 0.0f;

        for (ScoreDoc scoreDoc : scoreDocs) {
            total = total + scoreDoc.score;
        }

        return total / scoreDocs.length;
    }

    /**
     * 모집단(검색결과) documents 의 score 표준편차 값 계산
     * - 계산식
     *   σ = √(Σ((xi - μ)^2) / N)
     *   [xi = 각 데이터 값, μ = 모집단 평균, N = 데이터 값의 개수]
     *
     * @param scoreDocs
     * @param meanScore
     * @return
     */
    private float getStandardDeviation(ScoreDoc[] scoreDocs, float meanScore) {
        float totalSumDeviationSquare = 0.0f;
        for (ScoreDoc scoreDoc : scoreDocs) {
            float deviation = scoreDoc.score - meanScore;
            float deviationSquare = deviation * deviation;
            totalSumDeviationSquare += deviationSquare;
        }
        return (float) Math.sqrt(totalSumDeviationSquare / scoreDocs.length);
    }

    /**
     * z-score normalization scale calculate
     *
     * @param currentScore  계산 대상 document score
     * @param meanScore     documents 의 score 값 평균치
     * @param standardDeviation documents의 score 값 표준편차
     * @return
     */
    private static float calculateZScore(float currentScore, float meanScore, float standardDeviation) {
        return (currentScore - meanScore) / standardDeviation;
    }

    /**
     * factor mode 에 따른 factor 값 normalized 결과에 적용
     *
     * @param factorMode    지정 가능 모드 (sum, multiply, increase_by_percent)
     * @param factor        factor 값.
     * @param normalizedScore   min max normalized document score
     * @return
     */
    private static float applyFactorToNormalizedScore(String factorMode, float factor, float normalizedScore) {

        if (factorMode.equals(NormalizerFactorOperation.sum.name())) {
            normalizedScore += factor;
        } else if (factorMode.equals(NormalizerFactorOperation.multiply.name())) {
            normalizedScore = Math.abs(normalizedScore) * factor;
        } else {
            if (normalizedScore == 0.0f) {
                normalizedScore = factor;
            } else {
                if (factor < 0 || factor > 1) {
                    throw new IllegalArgumentException(
                            "increase_by_percent factorMode allowed factor range 0 ~ 1");
                }
                normalizedScore = normalizedScore + (Math.abs(normalizedScore) * factor);
            }
        }
        return normalizedScore;
    }

}
