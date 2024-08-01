package elasticsearch.custom.plugin.rescorer.normalizer;

import elasticsearch.custom.plugin.rescorer.NormalizerRescorer;
import elasticsearch.custom.plugin.enumeration.NormalizerFactorOperation;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.util.Arrays;

public class RobustNormalizer implements CustomNormalizer {

    /**
     * 사용자 쿼리를 통해 매칭된 도큐먼트들의 score를 Robust 정규화 합니다.
     * (similarity score의 이상치에 영향을 최소화하여 정규화 합니다.
     *  단, 데이터 분포가 넓어질 가능성이 있습니다.)
     *
     * @param topDocs   각 샤드에서 전달 받은 상위 매칭 도큐먼트
     * @param rescorerContext   Rescorer context (환경 정의 변수)
     * @return
     */
    @Override
    public TopDocs normalize(TopDocs topDocs, NormalizerRescorer.NormalizerRescorerContext rescorerContext) {

        if (topDocs.scoreDocs.length == 0) {
            return topDocs;
        }

        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        float[] scores = getSortedScores(scoreDocs);
        float median = getMedianScore(scores);
        float IQR = getIQRScore(scores);
        // 분모 0 나누기 방지
        IQR = (IQR == 0.0f) ? 1.0f : IQR;

        for (ScoreDoc scoreDoc : scoreDocs) {
            float normalizedScore = calculateRobustScore(scoreDoc.score, median, IQR);
            scoreDoc.score = applyFactorToNormalizedScore(
                    rescorerContext.getFactorMode(),
                    rescorerContext.getFactor(),
                    normalizedScore);
        }
        return topDocs;
    }

    /**
     * 매칭된 각 도큐먼트들의 정렬된 Score array 반환.
     *
     * @param scoreDocs
     * @return
     */
    private float[] getSortedScores(ScoreDoc[] scoreDocs) {
        // Score 추출
        float[] scores = new float[scoreDocs.length];
        for (int i = 0; i < scoreDocs.length; i++) {
            scores[i] = scoreDocs[i].score;
        }
        // Scores 정렬
        Arrays.sort(scores);

        return scores;
    }

    /**
     * 매칭된 도큐먼트 들의 score 중앙값 계산.
     * (도큐먼트 수가 짝수 일 경우 중간 두 값의 평균, 홀수 일 경우 중앙 값 반환)
     *
     * @param scores
     * @return
     */
    private float getMedianScore(float[] scores) {
        int scoresLength = scores.length;
        if (scoresLength % 2 == 0) {
            return (scores[scoresLength / 2 - 1] + scores[scoresLength / 2]) / 2.0f;
        } else {
            return scores[scoresLength / 2];
        }
    }

    /**
     * 매칭된 도큐먼트 들의 Score IQR 값 계산 (Q3 - Q1)
     *
     * @param scores
     * @return
     */
    public static float getIQRScore(float[] scores) {
        float q1 = calculatePercentile(scores, 25);    // 1사분위 수(Q1)
        float q3 = calculatePercentile(scores, 75);    // 3사분위 수(Q3)
        return q3 - q1;
    }

    /**
     * 지정한 백분위수에 위치한 백분위(점수) 계산.
     * 위치가 소수점 일 경우 선형 보간법을 통해 백분위 반환
     *
     * @param scores        scores array
     * @param percentile    백분위수
     * @return
     */
    private static float calculatePercentile(float[] scores, float percentile) {
        int scoresLength = scores.length;
        // 지정한 백분위수에 따른 scores 위치
        float index = (percentile / 100) * (scoresLength - 1);
        int lower = (int) Math.floor(index);    // 올림
        int upper = (int) Math.ceil(index);     // 내림
        float weight = index - lower;

        if (lower == upper) {
            return scores[lower];
        } else {
            return scores[lower] * (1 - weight) + scores[upper] * weight;
        }
    }

    /**
     * Robust normalization scale calculate
     *
     * @param currentScore  계산 대상 document score
     * @param median        documents 의 Score 중앙값
     * @param IQR           documents 의 Score IQR 값
     * @return
     */
    private static float calculateRobustScore(float currentScore, float median, float IQR) {
        return (currentScore - median) / IQR;
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
