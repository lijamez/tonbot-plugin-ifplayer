package net.tonbot.plugin.ifplayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.tonberry.tonbot.common.Activity;
import com.tonberry.tonbot.common.TonbotPlugin;
import com.tonberry.tonbot.common.TonbotPluginArgs;
import com.tonberry.tonbot.common.TonbotTechnicalFault;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class IfPlayerPlugin extends TonbotPlugin {

    private Injector injector;

    public IfPlayerPlugin(TonbotPluginArgs tonbotPluginArgs) {
        super(tonbotPluginArgs);
        System.out.println("HELLO!!!!!!!!!!!!!!!\n\n\n\n");

        File configFile = tonbotPluginArgs.getConfigFile()
                .orElseThrow(() -> new TonbotTechnicalFault("Config file does not exist."));

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Config config = objectMapper.readValue(configFile, Config.class);

            File storyDir = new File(config.getStoriesDir());
            File savesDir = new File(config.getSavesDir());

            this.injector = Guice.createInjector(new IfPlayerModule(tonbotPluginArgs.getPrefix(), storyDir, savesDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not read configuration file.", e);
        }
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
