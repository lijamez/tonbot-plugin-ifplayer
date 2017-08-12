package net.tonbot.plugin.ifplayer;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.tonberry.tonbot.common.Activity;
import com.tonberry.tonbot.common.TonbotPlugin;
import com.tonberry.tonbot.common.TonbotPluginArgs;

import java.io.File;
import java.util.Set;

class IfPlayerPlugin extends TonbotPlugin {

    private static final File STORY_DIR = new File("/Users/lijamez/zmpp-plugin/stories/");
    private static final File SAVES_DIR = new File("/Users/lijamez/zmpp-plugin/saves/");

    private Injector injector;

    public IfPlayerPlugin(TonbotPluginArgs tonbotPluginArgs) {
        super(tonbotPluginArgs);
        this.injector = Guice.createInjector(new IfPlayerModule(tonbotPluginArgs.getPrefix(), STORY_DIR, SAVES_DIR));
    }

    @Override
    public String getFriendlyName() {
        return "Interactive Fiction Player";
    }

    @Override
    public String getActionDescription() {
        return "Play Interactive Fiction";
    }

    @Override
    public Set<Activity> getActivities() {
        return injector.getInstance(Key.get(new TypeLiteral<Set<Activity>>() {}));
    }

    @Override
    public Set<Object> getRawEventListeners() {
        IfPlayerSendLineListener sendLineListener = injector.getInstance(IfPlayerSendLineListener.class);
        return ImmutableSet.of(sendLineListener);
    }
}
