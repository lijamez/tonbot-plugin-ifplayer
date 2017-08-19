package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import net.tonbot.common.BotUtils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

class SessionOrchestratorImpl implements SessionOrchestrator {

	private static final Logger LOG = LoggerFactory.getLogger(SessionOrchestratorImpl.class);

	private final SessionManager sessionManager;
	private final ScreenStateRenderer screenStateRenderer;
	private final StoryLibrary storyLibrary;
	private final SaveManager saveManager;

	@Inject
	public SessionOrchestratorImpl(
			SessionManager sessionManager,
			ScreenStateRenderer screenStateRenderer,
			StoryLibrary storyLibrary,
			SaveManager saveManager) {
		this.sessionManager = Preconditions.checkNotNull(sessionManager, "sessionManager must be non-null.");
		this.screenStateRenderer = Preconditions.checkNotNull(screenStateRenderer,
				"screenStateRenderer must be non-null.");
		this.storyLibrary = Preconditions.checkNotNull(storyLibrary, "storyLibrary must be non-null.");
		this.saveManager = Preconditions.checkNotNull(saveManager, "saveManager must be non-null.");
	}

	@Override
	public void create(IChannel channel, String storyName, String username) {
		Preconditions.checkNotNull(channel, "channel must be non-null.");
		Preconditions.checkNotNull(storyName, "storyName must be non-null.");
		Preconditions.checkNotNull(username, "username must be non-null.");

		List<File> foundFiles = storyLibrary.findStories(storyName);
		if (foundFiles.isEmpty()) {
			BotUtils.sendMessage(channel, "There is no story with that name.");
			return;
		} else if (foundFiles.size() > 1) {
			BotUtils.sendMessage(channel, "You're going to have to be more specific than that.");
			return;
		}

		File storyFile = foundFiles.get(0);
		Story story = Story.loadFrom(storyFile);

		// End the current session, if any.
		this.end(channel);

		// Create a new session.
		SessionKey sessionKey = new SessionKey(channel.getLongID());
		sessionManager.createSession(sessionKey, channel, story);

		// Advance it one step
		this.advance(null, channel, username);
	}

	@Override
	public boolean hasSession(IChannel channel) {
		return getSession(channel) != null;
	}

	@Override
	public void advance(String input, IChannel channel, String username) {
		Preconditions.checkNotNull(channel, "channel must be non-null.");
		Preconditions.checkNotNull(username, "username must be non-null.");

		Session session = getSession(channel);

		if (session == null) {
			return;
		}

		GameMachine gameMachine = session.getGameMachine();

		Optional<ScreenState> screenState;
		try {
			screenState = gameMachine.takeTurn(input, username);
		} catch (GameMachineException e) {
			// Handle a non-fatal exception.
			BotUtils.sendMessage(channel, "Error: " + e.getMessage());
			return;
		} catch (Exception e) {
			LOG.error("GameMachine has thrown an unexpected exception.", e);
			BotUtils.sendMessage(channel, "The player has crashed! :(");
			this.endInternal(channel);
			screenStateRenderer.render(session, null, channel);
			return;
		}

		screenStateRenderer.render(session, screenState.orElse(null), channel);

		if (!screenState.isPresent()) {
			sessionManager.removeSession(session.getSessionKey());
			BotUtils.sendMessage(channel, "Story '" + gameMachine.getStory().getName() + "' has stopped.");
		}
	}

	@Override
	public void switchSave(IChannel channel, int slotNumber) {
		if (slotNumber < 0 || slotNumber >= saveManager.getMaxSlots()) {
			BotUtils.sendMessage(channel, "Invalid slot number. Must be from 0-" + (saveManager.getMaxSlots() - 1));
			return;
		}

		Session session = getSession(channel);
		SaveFile saveFile = saveManager.getSaveFile(channel.getLongID(), session.getGameMachine().getStory(),
				slotNumber);
		session.getGameMachine().setSaveFile(saveFile);
		
		screenStateRenderer.render(session, null, channel);
		BotUtils.sendMessage(channel, "Switched to save slot " + slotNumber);
	}

	@Override
	public boolean end(IChannel channel) {
		Preconditions.checkNotNull(channel, "channel must be non-null.");

		Session session = getSession(channel);
		
		if (!this.endInternal(channel)) {
			return false;
		}
		
		screenStateRenderer.render(session, null, channel);
		BotUtils.sendMessage(channel, "Story '" + session.getGameMachine().getStory().getName() + "' has stopped.");
		return true;
	}
	
	/**
	 * Ends a session by channel.
	 * @param channel {@link IChannel}. Non-null.
	 * @return True if a session was ended. False otherwise.
	 */
	private boolean endInternal(IChannel channel) {
		Session session = getSession(channel);

		if (session == null) {
			return false;
		}

		session.getGameMachine().stop();
		sessionManager.removeSession(session.getSessionKey());
		return true;
	}

	private Session getSession(IChannel channel) {
		SessionKey sessionKey = new SessionKey(channel.getLongID());
		Session session = sessionManager.getSession(sessionKey).orElse(null);
		return session;
	}

	@Override
	public void listSlots(IChannel channel) {
		Session session = getSession(channel);

		if (session == null) {
			BotUtils.sendMessage(channel, "You need to play a story first.");
			return;
		}

		Story story = session.getGameMachine().getStory();

		List<SaveFile> saveFiles = saveManager.getSaveFiles(channel.getLongID(), story);
		Map<Integer, SaveFile> saveMap = saveFiles.stream()
				.collect(Collectors.toMap(sf -> sf.getSlot(), sf -> sf));

		SaveFile currentSaveFile = session.getGameMachine().getSaveFile().orElse(null);
		
		EmbedBuilder embedBuilder = new EmbedBuilder();
		embedBuilder.appendDescription("Save slots for **" + story.getName() + "** in channel **" + channel.getName() + "**:");

		for (int i = 0; i < saveManager.getMaxSlots(); i++) {
			StringBuilder contentsBuilder = new StringBuilder();
			SaveFile saveFileInSlot = saveMap.get(i);
			
			if (saveFileInSlot == null) {
				contentsBuilder.append("Free");
			} else if (saveFileInSlot.getMetadata().isPresent()) {
				SaveFileMetadata metadata = saveFileInSlot.getMetadata().get();
				contentsBuilder
						.append("Saved by: ")
						.append(metadata.getCreatedBy())
						.append("\n")
						.append("Saved on: ")
						.append(metadata.getCreationDate().toString());
			} else {
				contentsBuilder.append("Occupied");
			}

			StringBuilder topicBuilder = new StringBuilder();
			topicBuilder
					.append("Slot ")
					.append(i);
			
			if (currentSaveFile != null && currentSaveFile.getSlot() == i) {
				topicBuilder.append(" :point_left:");
			}

			embedBuilder.appendField(topicBuilder.toString(), contentsBuilder.toString(), false);
		}

		BotUtils.sendEmbeddedContent(channel, embedBuilder.build());
	}

	@Override
	public void deleteSaveSlot(IChannel channel, int slotNumber) {
		if (slotNumber < 0 || slotNumber >= saveManager.getMaxSlots()) {
			BotUtils.sendMessage(channel, "Invalid slot number. Must be from 0-" + (saveManager.getMaxSlots() - 1));
			return;
		}

		Session session = getSession(channel);

		if (session == null) {
			BotUtils.sendMessage(channel, "You need to play a story first.");
			return;
		}

		saveManager.deleteSaveFile(channel.getLongID(), session.getGameMachine().getStory(), slotNumber);

		BotUtils.sendMessage(channel, "Successfully deleted slot " + slotNumber);
	}
}
