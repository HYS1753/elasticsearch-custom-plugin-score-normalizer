package elasticsearch.custom.plugin.enumeration;

public enum NormalizerType {
    min_max,
    z_score,
    robust;

    public static boolean isValid(String normalizerType) {
        try {
            NormalizerType.valueOf(normalizerType);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }
}
