package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.tonberry.tonbot.common.Prefix;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

class IfPlayerSendLineListener {

    private final String prefix;
    private final SessionManager sessionManager;

    @Inject
    public IfPlayerSendLineListener(@Prefix String prefix, SessionManager sessionManager) {
        this.prefix = Preconditions.checkNotNull(prefix, "prefix must be non-null.");
        this.sessionManager = Preconditions.checkNotNull(sessionManager, "sessionManager must be non-null.");
    }

    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent messageReceivedEvent) {

        String message = messageReceivedEvent.getMessage().getContent();

        if (message.startsWith(prefix)) {
            return;
        }

        SessionKey sessionKey = new SessionKey(messageReceivedEvent.getChannel().getStringID());

        Session session = sessionManager.getSession(sessionKey)
                .orElse(null);

        if (session != null) {
            session.sendText(message);
        }
    }
}
