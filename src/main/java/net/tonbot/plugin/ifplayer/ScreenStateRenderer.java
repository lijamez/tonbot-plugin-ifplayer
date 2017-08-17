package net.tonbot.plugin.ifplayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import net.tonbot.common.BotUtils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

class ScreenStateRenderer {

	private static final Logger LOG = LoggerFactory.getLogger(ScreenStateRenderer.class);

	/**
	 * Updates the channel with the new screen state.
	 * 
	 * @param sesison
	 *            {@link Session}
	 * @param screenState
	 *            {@link ScreenState}
	 * @param channel
	 *            {@link IChannel}
	 */
	public void render(Session session, ScreenState screenState, IChannel channel) {
		Preconditions.checkNotNull(session, "session must be non-null.");
		Preconditions.checkNotNull(channel, "channel must be non-null.");

		GameMachine gm = session.getGameMachine();

		if (gm.isStopped()) {
			Preconditions.checkNotNull(screenState, "screenState must be non-null when game machine isn't stopped.");
		} else {
			sendScreen(screenState, channel);
		}

		updateChannelTopic(session, screenState, channel);

	}

	private void sendScreen(ScreenState screenState, IChannel channel) {
		// TODO: Split the message if it exceeds Discord's maximum characters per
		// message (2000).
		StringBuffer discordMessageBuffer = new StringBuffer();

		for (String windowContent : screenState.getWindowContents()) {

			if (windowContent.length() != 0) {

				discordMessageBuffer.append("```");
				discordMessageBuffer.append(windowContent);
				discordMessageBuffer.append("```");
			}
		}

		String output = discordMessageBuffer.toString();
		
		BotUtils.sendMessage(channel, output);
	}

	private void updateChannelTopic(Session session, ScreenState screenState, IChannel channel) {
		StringBuffer sb = new StringBuffer();

		GameMachine gm = session.getGameMachine();

		if (!gm.isStopped()) {
			sb.append("Now playing: ");
			sb.append(gm.getStory().getName());

			screenState.getStatusLineObjectName().ifPresent(objName -> {
				sb.append(" | ");
				sb.append(objName);
			});

			screenState.getStatusLineScoreOrTime().ifPresent(scoreOrTime -> {
				sb.append(" | ");
				sb.append(scoreOrTime);
			});
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
}
