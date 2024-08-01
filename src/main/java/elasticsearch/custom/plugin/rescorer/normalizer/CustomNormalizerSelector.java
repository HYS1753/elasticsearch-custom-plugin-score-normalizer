package elasticsearch.custom.plugin.rescorer.normalizer;

import elasticsearch.custom.plugin.enumeration.NormalizerType;

public final class CustomNormalizerSelector {

    private CustomNormalizerSelector() {}

    private static final CustomNormalizer minMaxNormalizer = new MinMaxNormalizer();
    private static final CustomNormalizer zScoreNormalizer = new ZScoreNormalizer();
    private static final CustomNormalizer robustNormalizer = new RobustNormalizer();

    public static CustomNormalizer getCustomNormalizer(NormalizerType normalizerType) {
        if (normalizerType == NormalizerType.min_max) {
            return minMaxNormalizer;
        }
        if (normalizerType == NormalizerType.z_score) {
            return zScoreNormalizer;
        }
        if (normalizerType == NormalizerType.robust) {
            return robustNormalizer;
        }

        // default normalizer
        return minMaxNormalizer;
    }
}
