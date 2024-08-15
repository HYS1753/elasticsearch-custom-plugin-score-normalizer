package elasticsearch.custom.plugin.rescorer.normalizer;

import elasticsearch.custom.plugin.rescorer.NormalizedCustomRescorer;
import org.apache.lucene.search.TopDocs;

public interface CustomNormalizer {

    TopDocs normalize(TopDocs topDocs, NormalizedCustomRescorer.NormalizerRescorerContext rescorerContext);
}
