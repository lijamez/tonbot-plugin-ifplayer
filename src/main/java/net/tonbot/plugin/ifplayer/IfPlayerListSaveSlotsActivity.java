package net.tonbot.plugin.ifplayer;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import net.tonbot.common.Activity;
import net.tonbot.common.ActivityDescriptor;
import net.tonbot.common.BotUtils;
import net.tonbot.common.Enactable;
import net.tonbot.common.TonbotBusinessException;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

class IfPlayerListSaveSlotsActivity implements Activity {

	private static final ActivityDescriptor ACTIVITY_DESCRIPTOR = ActivityDescriptor.builder().route("if slots")
			.description("Lists the save slots for the current story.").build();

	private final SessionManager sessionManager;
	private final SaveManager saveManager;
	private final BotUtils botUtils;
	private final Color accentColor;

	@Inject
	public IfPlayerListSaveSlotsActivity(
			SessionManager sessionManager, 
			SaveManager saveManager, 
			BotUtils botUtils,
			Color accentColor) {
		this.sessionManager = Preconditions.checkNotNull(sessionManager, "sessionManager must be non-null.");
		this.saveManager = Preconditions.checkNotNull(saveManager, "saveManager must be non-null.");
		this.botUtils = Preconditions.checkNotNull(botUtils, "botUtils must be non-null.");
		this.accentColor = Preconditions.checkNotNull(accentColor, "accentColor must be non-null.");
	}

	@Override
	public ActivityDescriptor getDescriptor() {
		return ACTIVITY_DESCRIPTOR;
	}

	@Enactable
	public void enact(MessageReceivedEvent messageReceivedEvent) {
		IChannel channel = messageReceivedEvent.getChannel();

		Session session = sessionManager.getSession(
				SessionKey.builder()
					.channelId(channel.getLongID())
					.build())
				.orElse(null);

		if (session == null) {
			throw new TonbotBusinessException("You need to play a story first.");
		}

		Story story = session.getGameMachine().getStory();

		List<SaveFile> saveFiles = saveManager.getSaveFiles(channel.getLongID(), story);
		Map<Integer, SaveFile> saveMap = saveFiles.stream().collect(Collectors.toMap(sf -> sf.getSlot(), sf -> sf));

		SaveFile currentSaveFile = session.getGameMachine().getSaveFile().orElse(null);

		EmbedBuilder embedBuilder = new EmbedBuilder();
		embedBuilder.appendDescription(
				"Save slots for **" + story.getName() + "** in channel **" + channel.getName() + "**:");

		for (int i = 0; i < saveManager.getMaxSlots(); i++) {
			StringBuilder contentsBuilder = new StringBuilder();
			SaveFile saveFileInSlot = saveMap.get(i);

			if (saveFileInSlot == null) {
				contentsBuilder.append("Free");
			} else if (saveFileInSlot.getMetadata().isPresent()) {
				SaveFileMetadata metadata = saveFileInSlot.getMetadata().get();
				contentsBuilder.append("Saved by: ").append(metadata.getCreatedBy()).append("\n").append("Saved on: ")
						.append(metadata.getCreationDate().toString());
			} else {
				contentsBuilder.append("Occupied");
			}

			StringBuilder topicBuilder = new StringBuilder();
			topicBuilder.append("Slot ").append(i);

			if (currentSaveFile != null && currentSaveFile.getSlot() == i) {
				topicBuilder.append(" :point_left:");
			}

			embedBuilder.appendField(topicBuilder.toString(), contentsBuilder.toString(), false);
		}
		embedBuilder.withColor(accentColor);

		botUtils.sendEmbed(channel, embedBuilder.build());
	}
}
