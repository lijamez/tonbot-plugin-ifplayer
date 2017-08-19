package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import sx.blah.discord.handle.obj.IChannel;

class SessionManagerImpl implements SessionManager {

	private final ConcurrentHashMap<SessionKey, Session> sessions;

	private final File saveDirectory;

	/**
	 * Constructor.
	 * 
	 * @param saveDirectory
	 *            The directory for save files. Non-null.
	 */
	@Inject
	public SessionManagerImpl(
			@SaveDir File saveDirectory) {

		Preconditions.checkNotNull(saveDirectory, "saveDirectory must be non-null.");
		Preconditions.checkArgument(saveDirectory.exists(), "saveDirectory must exist.");
		Preconditions.checkArgument(saveDirectory.isDirectory(), "saveDirectory must be a directory.");
		this.saveDirectory = saveDirectory;

		this.sessions = new ConcurrentHashMap<>();
	}

	@Override
	public Optional<Session> getSession(SessionKey sessionKey) {
		Preconditions.checkNotNull(sessionKey, "sessionKey must be non-null.");
		Session session = sessions.get(sessionKey);

		return Optional.ofNullable(session);
	}

	@Override
	public Session createSession(SessionKey sessionKey, IChannel channel, File storyFile) {
		Preconditions.checkNotNull(sessionKey, "sessionKey must be non-null.");
		Preconditions.checkNotNull(channel, "channel must be non-null.");
		Preconditions.checkNotNull(storyFile, "storyFile must be non-null.");

		String channelId = channel.getStringID();
		String saveFileName = channelId + "-" + storyFile.getName() + ".save";
		File saveFile = new File(saveDirectory.getAbsolutePath() + "/" + saveFileName);

		String sessionName = storyFile.getName();

		Session session = new Session(sessionKey, sessionName, storyFile, saveFile, channel);

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
