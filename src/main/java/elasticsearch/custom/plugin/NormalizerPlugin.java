package elasticsearch.custom.plugin;

import elasticsearch.custom.plugin.builder.NormalizerBuilder;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;

import java.util.List;

import static java.util.Collections.singletonList;

public class NormalizerPlugin extends Plugin implements SearchPlugin {

    @Override
    public List<RescorerSpec<?>> getRescorers() {
        return singletonList(
                new RescorerSpec<>(NormalizerBuilder.NAME, NormalizerBuilder::new, NormalizerBuilder::fromXContent));
    }
}
