package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import net.tonbot.common.TonbotBusinessException;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.RequestBuilder;

class SessionOrchestratorImpl implements SessionOrchestrator {

	private static final Logger LOG = LoggerFactory.getLogger(SessionOrchestratorImpl.class);

	private final IDiscordClient discordClient;
	private final SessionManager sessionManager;
	private final StoryLibrary storyLibrary;
	private final SaveManager saveManager;

	@Inject
	public SessionOrchestratorImpl(IDiscordClient discordClient, SessionManager sessionManager,
			StoryLibrary storyLibrary, SaveManager saveManager) {
		this.discordClient = Preconditions.checkNotNull(discordClient, "discordClient must be non-null.");
		this.sessionManager = Preconditions.checkNotNull(sessionManager, "sessionManager must be non-null.");
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
			throw new TonbotBusinessException("There is no story with that name.");
		} else if (foundFiles.size() > 1) {
			throw new TonbotBusinessException("You're going to have to be more specific than that.");
		}

		File storyFile = foundFiles.get(0);
		Story story = Story.loadFrom(storyFile);

		// End the current session, if any.
		this.endInternal(channel);

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
		ScreenStateRenderer screenStateRenderer = session.getScreenStateRenderer();

		Optional<ScreenState> screenState;
		try {
			screenState = gameMachine.takeTurn(input, username);
		} catch (GameMachineException e) {
			// Handle a non-fatal exception.
			throw new TonbotBusinessException("Error: " + e.getMessage());
		} catch (Exception e) {
			LOG.error("GameMachine has thrown an unexpected exception.", e);
			this.sendMessage(channel, "The player has crashed! :(");
			this.endInternal(channel);
			screenStateRenderer.render(session, null, channel);
			throw e;
		}

		screenStateRenderer.render(session, screenState.orElse(null), channel);

		if (!screenState.isPresent()) {
			sessionManager.removeSession(session.getSessionKey());
			this.sendMessage(channel, "Story '" + gameMachine.getStory().getName() + "' has stopped.");
		}
	}

	@Override
	public void switchSave(IChannel channel, int slotNumber) {
		if (slotNumber < 0 || slotNumber >= saveManager.getMaxSlots()) {
			throw new TonbotBusinessException("Invalid slot number. Must be from 0-" + (saveManager.getMaxSlots() - 1));
		}

		Session session = getSession(channel);
		if (session == null) {
			throw new TonbotBusinessException("You need to be playing a story first.");
		}

		ScreenStateRenderer screenStateRenderer = session.getScreenStateRenderer();

		SaveFile saveFile = saveManager.getSaveFile(channel.getLongID(), session.getGameMachine().getStory(),
				slotNumber);
		session.getGameMachine().setSaveFile(saveFile);

		screenStateRenderer.render(session, null, channel);
		this.sendMessage(channel, "Switched to save slot " + slotNumber);
	}

	@Override
	public boolean end(IChannel channel) {
		Preconditions.checkNotNull(channel, "channel must be non-null.");

		Session session = getSession(channel);

		if (session != null) {
			this.endInternal(channel);
			this.sendMessage(channel, "Story '" + session.getGameMachine().getStory().getName() + "' has stopped.");
			return true;
		}

		return false;
	}

	/**
	 * Ends a session by channel.
	 * 
	 * @param channel
	 *            {@link IChannel}. Non-null.
	 * @return True if a session was ended. False otherwise.
	 */
	private boolean endInternal(IChannel channel) {
		Session session = getSession(channel);
		if (session == null) {
			return false;
		}

		session.getGameMachine().stop();

		ScreenStateRenderer screenStateRenderer = session.getScreenStateRenderer();
		screenStateRenderer.render(session, null, channel);
		sessionManager.removeSession(session.getSessionKey());

		return true;
	}

	private Session getSession(IChannel channel) {
		SessionKey sessionKey = new SessionKey(channel.getLongID());
		Session session = sessionManager.getSession(sessionKey).orElse(null);
		return session;
	}

	@Override
	public void deleteSaveSlot(IChannel channel, int slotNumber) {
		if (slotNumber < 0 || slotNumber >= saveManager.getMaxSlots()) {
			throw new TonbotBusinessException("Invalid slot number. Must be from 0-" + (saveManager.getMaxSlots() - 1));
		}

		Session session = getSession(channel);

		if (session == null) {
			throw new TonbotBusinessException("You need to play a story first.");
		}

		saveManager.deleteSaveFile(channel.getLongID(), session.getGameMachine().getStory(), slotNumber);

		this.sendMessage(channel, "Successfully deleted slot " + slotNumber);
	}

	private void sendMessage(IChannel channel, String message) {
		new RequestBuilder(discordClient).shouldBufferRequests(true).setAsync(true).doAction(() -> {
			channel.sendMessage(message);
			return true;
		}).execute();
	}
}
