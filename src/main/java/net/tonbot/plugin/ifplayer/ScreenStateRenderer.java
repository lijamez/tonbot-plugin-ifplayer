package net.tonbot.plugin.ifplayer;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import lombok.Builder;
import lombok.Data;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.RequestBuilder;

/**
 * This class will:
 * <ul>
 * <li>render the game's screen as messages</li>
 * <li>render the status lines in the channel's topic</li>
 * <li>render the IF player's save slot selection in the channel's topic</li>
 * </ul>
 * 
 * This class is stateful because when it does a partial update of the topic
 * (e.g. a slot number is updated), it needs to remember the last topic's
 * contents (in particular, the stauts line).
 */
class ScreenStateRenderer {

	private static final Logger LOG = LoggerFactory.getLogger(ScreenStateRenderer.class);
	private static final int SEPARATION = 15;

	private final IDiscordClient discordClient;

	private Topic lastTopic;

	@Inject
	public ScreenStateRenderer(IDiscordClient discordClient) {
		this.discordClient = Preconditions.checkNotNull(discordClient, "discordClient must be non-null.");
		this.lastTopic = Topic.builder()
				.build();
	}

	/**
	 * Updates the channel with the new state.
	 * 
	 * @param session
	 *            {@link Session} Non-null.
	 * @param screenState
	 *            {@link ScreenState} Nullable.
	 * @param channel
	 *            {@link IChannel} Non-null.
	 */
	public void render(Session session, ScreenState screenState, IChannel channel) {
		Preconditions.checkNotNull(session, "session must be non-null.");
		Preconditions.checkNotNull(channel, "channel must be non-null.");

		GameMachine gm = session.getGameMachine();
		SaveFile saveFile = gm.getSaveFile().orElse(null);

		// Render the screen
		if (!gm.isStopped() && screenState != null) {
			sendScreen(screenState.getWindowContents(), channel);
		}

		// Render the topic
		ScreenStateRenderer.Topic.TopicBuilder newTopicBuilder = Topic.builder();
		if (!gm.isStopped()) {
			newTopicBuilder.playingStoryName(gm.getStory().getName());

			if (screenState != null) {
				newTopicBuilder.statusLineObjectName(screenState.getStatusLineObjectName().orElse(null))
						.statusLineScoreOrTime(screenState.getStatusLineScoreOrTime().orElse(null));
			} else {
				// Retain the old status line
				newTopicBuilder.statusLineObjectName(lastTopic.getStatusLineObjectName())
						.statusLineScoreOrTime(lastTopic.getStatusLineScoreOrTime());
			}

			if (saveFile != null) {
				newTopicBuilder.saveSlot(saveFile.getSlot());
			}
		}

		Topic newTopic = newTopicBuilder.build();

		updateChannelTopic(newTopic, channel);

		this.lastTopic = newTopic;
	}

	private void sendScreen(List<String> windowContents, IChannel channel) {
		// TODO: Split the message if it exceeds Discord's maximum characters per
		// message (2000).
		StringBuffer discordMessageBuffer = new StringBuffer();

		for (String windowContent : windowContents) {

			if (windowContent.length() != 0) {

				discordMessageBuffer.append("```");
				discordMessageBuffer.append(windowContent);
				discordMessageBuffer.append("```");
			}
		}

		String output = discordMessageBuffer.toString();

		if (!StringUtils.isBlank(output)) {
			new RequestBuilder(discordClient)
					.shouldBufferRequests(true)
					.setAsync(true)
					.doAction(() -> {
						channel.sendMessage(output);
						return true;
					})
					.execute();
		} else {
			LOG.warn("Screens are blank.");
		}
	}

	private void updateChannelTopic(Topic topic, IChannel channel) {
		StringBuffer sb = new StringBuffer();

		if (topic.getPlayingStoryName() != null) {
			sb.append("Now playing: ")
					.append(topic.getPlayingStoryName());

			if (topic.getSaveSlot() != null) {
				sb.append("\u3000\u3000Save slot ")
						.append(topic.getSaveSlot());
			}

			StringBuffer statusLineBuffer = new StringBuffer();
			// Print status line
			if (topic.getStatusLineObjectName() != null) {
				statusLineBuffer.append(topic.getStatusLineObjectName());
			}

			if (topic.getStatusLineScoreOrTime() != null) {
				statusLineBuffer.append("\u3000\u3000")
						.append(topic.getStatusLineScoreOrTime());
			}

			if (statusLineBuffer.length() > 0) {
				// Separates the IF player state from the game's status line with a zero-width
				// no-break space. Regular spaces/tabs are not used because Discord will shorten
				// consecutive spaces to a single space.
				for (int i = 0; i < SEPARATION; i++) {
					sb.append("\u3000");
				}
				sb.append(statusLineBuffer);
			}
		} else {
			sb.append("Not playing anything.");
		}

		RequestBuffer.request(() -> {
			try {
				channel.changeTopic(sb.toString());
			} catch (MissingPermissionsException e) {
				// This is fine. Just ignore it.
				LOG.debug("Could not set channel topic, and therefore could not set status line.", e);
			} catch (DiscordException e) {
				LOG.error("Topic could not be set.", e);
			}
		});
	}

	@Data
	@Builder
	private static class Topic {

		private final String playingStoryName;
		private final Integer saveSlot;
		private final String statusLineObjectName;
		private final String statusLineScoreOrTime;
	}
}
