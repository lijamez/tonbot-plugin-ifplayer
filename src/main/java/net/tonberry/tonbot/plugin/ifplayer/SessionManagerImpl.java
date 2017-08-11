package net.tonberry.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.tonberry.tonbot.common.TonbotBusinessException;
import sx.blah.discord.handle.obj.IChannel;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class SessionManagerImpl implements SessionManager {

    private final ConcurrentHashMap<SessionKey, Session> sessions;

    private final StoryLibrary storyLibrary;
    private final File saveDirectory;

    /**
     * Constructor.
     * @param storyLibrary {@link StoryLibrary}. Non-null.
     * @param saveDirectory The directory for save files. Non-null.
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

        File storyFile = storyLibrary.getBestMatch(storyName)
            .orElseThrow(() -> new TonbotBusinessException("There is no story with that name."));

        String channelId = channel.getStringID();
        String saveFileName = channelId + "-" + storyFile.getName() + ".save";
        File saveFile = new File(saveDirectory.getAbsolutePath() + "/" + saveFileName);

        String sessionName = storyFile.getName();

        Session session = new Session(sessionName, storyFile, saveFile, channel);

        Session oldSession = sessions.put(sessionKey, session);
        if (oldSession != null) {
            oldSession.end();
        }

        return session;
    }

    @Override
    public void endSession(SessionKey sessionKey) {
        Preconditions.checkNotNull(sessionKey, "sessionKey must be non-null.");

        Session session = sessions.get(sessionKey);

        if (session != null) {
            session.end();
            sessions.remove(sessionKey, session);
        }
    }
}
