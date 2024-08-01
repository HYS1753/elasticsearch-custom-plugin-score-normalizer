package elasticsearch.custom.plugin.rescorer.normalizer;

import elasticsearch.custom.plugin.rescorer.NormalizerRescorer;
import org.apache.lucene.search.TopDocs;

public interface CustomNormalizer {

    TopDocs normalize(TopDocs topDocs, NormalizerRescorer.NormalizerRescorerContext rescorerContext);
}
