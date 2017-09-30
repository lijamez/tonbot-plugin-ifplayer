package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import net.tonbot.common.Activity;
import net.tonbot.common.TonbotPlugin;
import net.tonbot.common.TonbotPluginArgs;
import net.tonbot.common.TonbotTechnicalFault;

public class IfPlayerPlugin extends TonbotPlugin {

	private Injector injector;

	public IfPlayerPlugin(TonbotPluginArgs tonbotPluginArgs) {
		super(tonbotPluginArgs);

		File configFile = tonbotPluginArgs.getConfigFile();
		if (!configFile.exists()) {
			throw new TonbotTechnicalFault("Config file does not exist.");
		}

		ObjectMapper objectMapper = new ObjectMapper();

		try {
			Config config = objectMapper.readValue(configFile, Config.class);

			File storyDir = new File(config.getStoriesDir());
			File savesDir = new File(config.getSavesDir());

			this.injector = Guice.createInjector(
					new IfPlayerModule(
							tonbotPluginArgs.getDiscordClient(),
							tonbotPluginArgs.getBotUtils(),
							tonbotPluginArgs.getPrefix(),
							storyDir,
							savesDir));
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
		return "Play Interactive Fiction (Experimental)";
	}

	@Override
	public Set<Activity> getActivities() {
		return injector.getInstance(Key.get(new TypeLiteral<Set<Activity>>() {
		}));
	}

	@Override
	public Set<Object> getRawEventListeners() {
		IfPlayerSendLineListener sendLineListener = injector.getInstance(IfPlayerSendLineListener.class);
		return ImmutableSet.of(sendLineListener);
	}
}
