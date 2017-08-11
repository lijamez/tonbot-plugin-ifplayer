package net.tonberry.tonbot.plugin.ifplayer;

import com.tonberry.tonbot.common.TonbotBusinessException;
import sx.blah.discord.handle.obj.IChannel;

import java.util.Optional;

interface SessionManager {

    /**
     * Creates a {@link Session} with the name of a story. Overwrites any existing session.
     * @param sessionKey {@link SessionKey}. Non-null.
     * @param channel {@link IChannel}. Non-null.
     * @param storyName The name of the story. Non-null.
     * @return {@link Session}. Non-null.
     * @throws TonbotBusinessException if this method couldn't pick a story from the name.
     */
    Session createSession(SessionKey sessionKey, IChannel channel, String storyName);

    Optional<Session> getSession(SessionKey sessionKey);

    /**
     * Ends a {@link Session}.
     * @param sessionKey The {@link SessionKey}. Non-null.
     */
    void endSession(SessionKey sessionKey);
}
