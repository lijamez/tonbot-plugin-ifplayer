package net.tonberry.tonbot.plugin.ifplayer;

import com.google.inject.Guice;
import com.tonberry.tonbot.common.PluginResources;
import com.tonberry.tonbot.common.TonbotPlugin;
import com.tonberry.tonbot.common.TonbotPluginArgs;

import java.io.File;

public class IfPlayerPlugin implements TonbotPlugin {

    private static final File STORY_DIR = new File("/Users/lijamez/zmpp-plugin/stories/");
    private static final File SAVES_DIR = new File("/Users/lijamez/zmpp-plugin/saves/");

    private IfPlayerModule module;

    @Override
    public void initialize(TonbotPluginArgs tonbotPluginArgs) {
        this.module = new IfPlayerModule(tonbotPluginArgs.getPrefix(), STORY_DIR, SAVES_DIR);
    }

    @Override
    public PluginResources build() {
        return Guice.createInjector(module)
                .getInstance(PluginResources.class);
    }
}
