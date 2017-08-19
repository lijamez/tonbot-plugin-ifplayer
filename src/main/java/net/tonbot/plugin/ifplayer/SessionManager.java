package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.util.Optional;

import net.tonbot.common.TonbotBusinessException;
import sx.blah.discord.handle.obj.IChannel;

interface SessionManager {

    /**
     * Creates a {@link Session} with the name of a story. Overwrites any existing session.
     * @param sessionKey {@link SessionKey}. Non-null.
     * @param channel {@link IChannel}. Non-null.
     * @param storyFile The file of the story. Non-null.
     * @return {@link Session}. Non-null.
     * @throws TonbotBusinessException if this method couldn't pick a story from the name.
     */
    Session createSession(SessionKey sessionKey, IChannel channel, File storyFile);

    /**
     * Gets a {@link Session} by its {@link SessionKey}.
     * @param sessionKey {@link SessionKey}. Non-null.
     * @return A {@link Session}.
     */
    Optional<Session> getSession(SessionKey sessionKey);

    /**
     * Removes a {@link Session}.
     * @param sessionKey The {@link SessionKey} of the session to remove. Non-null.
     */
    void removeSession(SessionKey sessionKey);
}
