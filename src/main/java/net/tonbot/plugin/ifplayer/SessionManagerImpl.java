package net.tonbot.plugin.ifplayer;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import sx.blah.discord.handle.obj.IChannel;

class SessionManagerImpl implements SessionManager {

	private final ConcurrentHashMap<SessionKey, Session> sessions;

	private final SaveManager saveManager;
	private final OnSavedCallback onSavedCallback;

	/**
	 * Constructor.
	 * 
	 * @param saveManager
	 *            {@link SaveManager}. Non-null.
	 */
	@Inject
	public SessionManagerImpl(SaveManager saveManager) {
		this.saveManager = Preconditions.checkNotNull(saveManager, "saveManager must be non-null.");
		this.onSavedCallback = new OnSavedCallback() {

			@Override
			public SaveFile getOnSavedCallback(long channelId, SaveFile saveFile,
					SaveFileMetadata newSaveFileMetadata) {
				return saveManager.saveNewMetadata(channelId, saveFile, newSaveFileMetadata);
			}

		};

		this.sessions = new ConcurrentHashMap<>();
	}

	@Override
	public Optional<Session> getSession(SessionKey sessionKey) {
		Preconditions.checkNotNull(sessionKey, "sessionKey must be non-null.");
		Session session = sessions.get(sessionKey);

		return Optional.ofNullable(session);
	}

	@Override
	public Session createSession(SessionKey sessionKey, IChannel channel, Story story) {
		Preconditions.checkNotNull(sessionKey, "sessionKey must be non-null.");
		Preconditions.checkNotNull(channel, "channel must be non-null.");
		Preconditions.checkNotNull(story, "story must be non-null.");

		SaveFile saveFile = saveManager.getSaveFile(channel.getLongID(), story, 0);

		String sessionName = story.getName();

		Session session = new Session(sessionKey, sessionName, story, saveFile, channel, onSavedCallback);

		sessions.put(sessionKey, session);

		return session;
	}

	@Override
	public void removeSession(SessionKey sessionKey) {
		Preconditions.checkNotNull(sessionKey, "sessionKey must be non-null.");

		Session session = sessions.get(sessionKey);

		if (session != null) {
			sessions.remove(sessionKey, session);
		}
	}
}
