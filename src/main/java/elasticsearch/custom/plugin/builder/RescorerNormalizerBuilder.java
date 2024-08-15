package elasticsearch.custom.plugin.builder;

import elasticsearch.custom.plugin.enumeration.NormalizerFactorOperation;
import elasticsearch.custom.plugin.enumeration.NormalizerType;
import elasticsearch.custom.plugin.enumeration.MinMaxSameScoreStrategy;
import elasticsearch.custom.plugin.rescorer.NormalizedCustomRescorer;
import org.elasticsearch.TransportVersion;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.index.query.QueryRewriteContext;
import org.elasticsearch.index.query.SearchExecutionContext;
import org.elasticsearch.search.rescore.RescoreContext;
import org.elasticsearch.search.rescore.RescorerBuilder;
import org.elasticsearch.xcontent.*;

import java.io.IOException;

public class RescorerNormalizerBuilder extends RescorerBuilder<RescorerNormalizerBuilder> {

    // 상수 설정
    public static final String NAME = "score_normalizer";

    private static final ParseField NORMALIZER_TYPE = new ParseField("normalizer_type");
    private static final ParseField MIN_SCORE = new ParseField("min_score");
    private static final ParseField MAX_SCORE = new ParseField("max_score");
    private static final ParseField FACTOR = new ParseField("factor");
    private static final ParseField FACTOR_MODE = new ParseField("factor_mode");
    private static final ParseField MIN_MAX_SAME_SCORE_STRATEGY = new ParseField("min_max_same_score_strategy");

    private static final float DEFAULT_MIN_SCORE_V = 0.0f;
    private static final float DEFAULT_MAX_SCORE_V = 1.0f;
    private static final float DEFAULT_FACTOR = 0.0f;
    private static final String DEFAULT_MIN_MAX_SAME_SCORE_STRATEGY = MinMaxSameScoreStrategy.avg.name();
    private static final NormalizerType DEFAULT_NORMALIZER_TYPE = NormalizerType.min_max;
    private static final String DEFAULT_FACTOR_MODE = NormalizerFactorOperation.increase_by_percent.name();

    // 변수 설정
    private float minScore;
    private float maxScore;
    private String normalizerType;
    private float factor;
    private String factorMode;
    private String minMaxSameScoreStrategy;

    // ObjectParser 설정
    private static final ObjectParser<NormalizerParserBuilder, Void> NORMALIZER_PARSER = new ObjectParser<>(NAME, null);

    static {
        NORMALIZER_PARSER.declareString(NormalizerParserBuilder::setNormalizerType, NORMALIZER_TYPE);
        NORMALIZER_PARSER.declareFloat(NormalizerParserBuilder::setMinScore, MIN_SCORE);
        NORMALIZER_PARSER.declareFloat(NormalizerParserBuilder::setMaxScore, MAX_SCORE);
        NORMALIZER_PARSER.declareFloat(NormalizerParserBuilder::setFactor, FACTOR);
        NORMALIZER_PARSER.declareString(NormalizerParserBuilder::setFactorMode, FACTOR_MODE);
        NORMALIZER_PARSER.declareString(NormalizerParserBuilder::setMinMaxSameScoreStrategy, MIN_MAX_SAME_SCORE_STRATEGY);
    }

    // 기본 생성자
    public RescorerNormalizerBuilder() {}

    public RescorerNormalizerBuilder(StreamInput in) throws IOException {
        super(in);
        normalizerType = in.readOptionalString();
        minScore = in.readOptionalFloat();
        maxScore = in.readOptionalFloat();
        factor = in.readOptionalFloat();
        factorMode = in.readOptionalString();
        minMaxSameScoreStrategy = in.readOptionalString();
    }

    @Override
    protected void doWriteTo(StreamOutput streamOutput) throws IOException {
        streamOutput.writeString(normalizerType);
        streamOutput.writeFloat(minScore);
        streamOutput.writeFloat(maxScore);
        streamOutput.writeFloat(factor);
        streamOutput.writeString(factorMode);
        streamOutput.writeString(minMaxSameScoreStrategy);
    }

    @Override
    protected void doXContent(XContentBuilder xContentBuilder, Params params) throws IOException {
        xContentBuilder.startObject(NAME);
        xContentBuilder.field(NORMALIZER_TYPE.getPreferredName(), normalizerType);
        xContentBuilder.field(MIN_SCORE.getPreferredName(), minScore);
        xContentBuilder.field(MAX_SCORE.getPreferredName(), maxScore);
        xContentBuilder.field(FACTOR.getPreferredName(), factor);
        xContentBuilder.field(FACTOR_MODE.getPreferredName(), factorMode);
        xContentBuilder.field(MIN_MAX_SAME_SCORE_STRATEGY.getPreferredName(), minMaxSameScoreStrategy);
        xContentBuilder.endObject();
    }

    public static RescorerNormalizerBuilder fromXContent(XContentParser parser) throws IOException {
        NormalizerParserBuilder normalizerParserBuilder = NORMALIZER_PARSER.parse(
                parser, new NormalizerParserBuilder(), null
        );
        return normalizerParserBuilder.build();
    }

    @Override
    protected RescoreContext innerBuildContext(int windowSize, SearchExecutionContext searchExecutionContext) throws IOException {
        NormalizedCustomRescorer.NormalizerRescorerContext normalizerRescorerContext =
                new NormalizedCustomRescorer.NormalizerRescorerContext(
                        windowSize, normalizerType, minScore, maxScore, factor, factorMode, minMaxSameScoreStrategy
                );
        return normalizerRescorerContext;
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    public TransportVersion getMinimalSupportedVersion() {
        return null;
    }

    @Override
    public RescorerBuilder<RescorerNormalizerBuilder> rewrite(QueryRewriteContext queryRewriteContext) throws IOException {
        return this;
    }

    public void setMinScore(float minScore) {
        this.minScore = minScore;
    }

    public void setMaxScore(float maxScore) {
        this.maxScore = maxScore;
    }

    public void setNormalizerType(String normalizerType) {
        this.normalizerType = normalizerType;
    }

    public void setFactor(float factor) {
        this.factor = factor;
    }

    public void setFactorMode(String factorMode) {
        this.factorMode = factorMode;
    }

    public void setMinMaxSameScoreStrategy(String minMaxSameScoreStrategy) {
        this.minMaxSameScoreStrategy = minMaxSameScoreStrategy;
    }

    private static class NormalizerParserBuilder {
        private float minScore = DEFAULT_MIN_SCORE_V;
        private float maxScore = DEFAULT_MAX_SCORE_V;
        private String normalizerType = DEFAULT_NORMALIZER_TYPE.name();
        private float factor = DEFAULT_FACTOR;
        private String factorMode = DEFAULT_FACTOR_MODE;
        private String minMaxSameScoreStrategy = DEFAULT_MIN_MAX_SAME_SCORE_STRATEGY;

        RescorerNormalizerBuilder build() {
            RescorerNormalizerBuilder builder = new RescorerNormalizerBuilder();
            builder.setNormalizerType(normalizerType);
            builder.setMinScore(minScore);
            builder.setMaxScore(maxScore);
            builder.setFactor(factor);
            builder.setFactorMode(factorMode);
            builder.setMinMaxSameScoreStrategy(minMaxSameScoreStrategy);
            return builder;
        }

        public void setMinScore(float minScore) {
            this.minScore = minScore;
        }

        public void setMaxScore(float maxScore) {
            this.maxScore = maxScore;
        }

        public void setNormalizerType(String normalizerType) {
            if (normalizerType == null) {
                return;
            }
            if (NormalizerType.isValid(normalizerType)) {
                this.normalizerType = normalizerType;
            }
        }

        public void setFactor(float factor) {
            this.factor = factor;
        }

        public void setFactorMode(String factorMode) {
            this.factorMode = factorMode;
        }

        public void setMinMaxSameScoreStrategy(String minMaxSameScoreStrategy) {
            this.minMaxSameScoreStrategy = minMaxSameScoreStrategy;
        }
    }
}
