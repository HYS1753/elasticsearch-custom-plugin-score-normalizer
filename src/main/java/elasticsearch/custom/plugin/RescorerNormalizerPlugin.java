package elasticsearch.custom.plugin;

import elasticsearch.custom.plugin.builder.RescorerNormalizerBuilder;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;

import java.util.List;

import static java.util.Collections.singletonList;

public class RescorerNormalizerPlugin extends Plugin implements SearchPlugin {

    @Override
    public List<RescorerSpec<?>> getRescorers() {
        return singletonList(
                new RescorerSpec<>(RescorerNormalizerBuilder.NAME, RescorerNormalizerBuilder::new, RescorerNormalizerBuilder::fromXContent));
    }
}
