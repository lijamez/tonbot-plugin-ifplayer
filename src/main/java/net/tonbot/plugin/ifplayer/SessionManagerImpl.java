package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import net.tonbot.common.TonbotBusinessException;
import sx.blah.discord.handle.obj.IChannel;

class SessionManagerImpl implements SessionManager {

	private final ConcurrentHashMap<SessionKey, Session> sessions;

	private final StoryLibrary storyLibrary;
	private final File saveDirectory;

	/**
	 * Constructor.
	 * 
	 * @param storyLibrary
	 *            {@link StoryLibrary}. Non-null.
	 * @param saveDirectory
	 *            The directory for save files. Non-null.
	 */
	@Inject
	public SessionManagerImpl(
			StoryLibrary storyLibrary,
			@SaveDir File saveDirectory) {
		Preconditions.checkNotNull(storyLibrary, "storyLibrary must be non-null.");
		this.storyLibrary = storyLibrary;

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
	public Session createSession(SessionKey sessionKey, IChannel channel, String storyName) {
		Preconditions.checkNotNull(sessionKey, "sessionKey must be non-null.");
		Preconditions.checkNotNull(channel, "channel must be non-null.");
		Preconditions.checkNotNull(storyName, "storyName must be non-null.");

		List<File> foundFiles = storyLibrary.findStories(storyName);
		if (foundFiles.isEmpty()) {
			throw new TonbotBusinessException("There is no story with that name.");
		} else if (foundFiles.size() > 1) {
			throw new TonbotBusinessException("You're going to have to be more specific than that.");
		}

		File storyFile = foundFiles.get(0);

		String channelId = channel.getStringID();
		String saveFileName = channelId + "-" + storyFile.getName() + ".save";
		File saveFile = new File(saveDirectory.getAbsolutePath() + "/" + saveFileName);

		String sessionName = storyFile.getName();

		Session session = new Session(sessionKey, sessionName, storyFile, saveFile, channel);

		sessions.put(sessionKey, session);

		return session;
	}

	@Override
	public void endSession(SessionKey sessionKey) {
		Preconditions.checkNotNull(sessionKey, "sessionKey must be non-null.");

		Session session = sessions.get(sessionKey);

		if (session != null) {
			sessions.remove(sessionKey, session);
		}
	}
}
