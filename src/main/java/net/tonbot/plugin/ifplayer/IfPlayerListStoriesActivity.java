package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import net.tonbot.common.Activity;
import net.tonbot.common.ActivityDescriptor;
import net.tonbot.common.BotUtils;
import net.tonbot.common.Enactable;
import net.tonbot.common.Prefix;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.util.EmbedBuilder;

class IfPlayerListStoriesActivity implements Activity {

	private static final ActivityDescriptor ACTIVITY_DESCRIPTOR = ActivityDescriptor.builder().route("if list")
			.description("Lists the available stories for play.").build();

	private final BotUtils botUtils;
	private final String prefix;
	private final StoryLibrary storyLibrary;

	@Inject
	public IfPlayerListStoriesActivity(BotUtils botUtils, @Prefix String prefix, StoryLibrary storyLibrary) {
		this.botUtils = Preconditions.checkNotNull(botUtils, "botUtils must be non-null.");
		this.prefix = Preconditions.checkNotNull(prefix, "prefix must be non-null.");
		this.storyLibrary = Preconditions.checkNotNull(storyLibrary, "storyLibrary must be non-null.");
	}

	@Override
	public ActivityDescriptor getDescriptor() {
		return ACTIVITY_DESCRIPTOR;
	}

	@Enactable
	public void enact(MessageReceivedEvent messageReceivedEvent) {
		List<File> storyFiles = storyLibrary.listAllStories();
		List<String> storyNames = storyFiles.stream().map(File::getName).collect(Collectors.toList());

		EmbedBuilder embedBuilder = new EmbedBuilder();

		embedBuilder.withTitle("Interactive Fiction Stories");
		embedBuilder.withDesc(
				StringUtils.join(storyNames, "\n") + "\n\nType ``" + prefix + " if play <STORY NAME>`` to play them.");

		botUtils.sendEmbed(messageReceivedEvent.getChannel(), embedBuilder.build());
	}
}
