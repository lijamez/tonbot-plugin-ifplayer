package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

import net.tonbot.common.Activity;
import net.tonbot.common.BotUtils;
import net.tonbot.common.Prefix;
import sx.blah.discord.api.IDiscordClient;

class IfPlayerModule extends AbstractModule {

	private static final int MAX_SAVE_SLOTS = 5;

	private final IDiscordClient discordClient;
	private final BotUtils botUtils;
	private final String prefix;
	private final File storyDir;
	private final File saveDir;

	public IfPlayerModule(IDiscordClient discordClient, BotUtils botUtils, String prefix, File storyDir, File saveDir) {
		this.discordClient = Preconditions.checkNotNull(discordClient, "discordClient must be non-null.");
		this.botUtils = Preconditions.checkNotNull(botUtils, "botUtils must be non-null.");
		this.prefix = Preconditions.checkNotNull(prefix, "prefix must be non-null.");

		Preconditions.checkNotNull(storyDir, "storyDir must be non-null.");
		Preconditions.checkArgument(storyDir.isDirectory(), "storyDir must be a directory.");
		this.storyDir = storyDir;

		Preconditions.checkNotNull(saveDir, "saveDir must be non-null.");
		Preconditions.checkArgument(saveDir.isDirectory(), "saveDir must be a directory.");
		this.saveDir = saveDir;
	}

	@Override
	protected void configure() {
		bind(IDiscordClient.class).toInstance(discordClient);
		bind(BotUtils.class).toInstance(botUtils);
		bind(String.class).annotatedWith(Prefix.class).toInstance(prefix);
		bind(File.class).annotatedWith(StoryDir.class).toInstance(storyDir);
		bind(File.class).annotatedWith(SaveDir.class).toInstance(saveDir);
		bind(Integer.class).annotatedWith(MaxSaveSlots.class).toInstance(MAX_SAVE_SLOTS);

		bind(SessionManager.class).to(SessionManagerImpl.class).in(Scopes.SINGLETON);
		bind(SessionOrchestrator.class).to(SessionOrchestratorImpl.class).in(Scopes.SINGLETON);
		bind(SaveManager.class).to(SaveManagerImpl.class).in(Scopes.SINGLETON);
	}

	@Provides
	@Singleton
	Set<Activity> activities(IfPlayerListStoriesActivity listStoriesActivity,
			IfPlayerPlayStoryActivity playStoryActivity, IfPlayerStopStoryActivity stopStoryActivity,
			IfPlayerControlsActivity controlsActivity, IfPlayerListSaveSlotsActivity listSaveSlotsActivity,
			IfPlayerSetSaveSlotActivity setSaveSlotActivity, IfPlayerDeleteSaveSlotActivity deleteSaveSlotActivity) {
		return ImmutableSet.of(listStoriesActivity, playStoryActivity, stopStoryActivity, controlsActivity,
				listSaveSlotsActivity, setSaveSlotActivity, deleteSaveSlotActivity);
	}
}
