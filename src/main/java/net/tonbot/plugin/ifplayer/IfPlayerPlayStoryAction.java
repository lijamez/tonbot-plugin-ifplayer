package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.tonberry.tonbot.common.Activity;
import com.tonberry.tonbot.common.BotUtils;
import com.tonberry.tonbot.common.TonbotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.MissingPermissionsException;

import java.util.List;
import java.util.Optional;

class IfPlayerPlayStoryAction implements Activity {

    private static final Logger LOG = LoggerFactory.getLogger(IfPlayerPlayStoryAction.class);

    private static final List<String> ROUTE = ImmutableList.of("if", "play");

    private final SessionManager sessionManager;

    @Inject
    public IfPlayerPlayStoryAction(SessionManager sessionManager) {
        this.sessionManager = Preconditions.checkNotNull(sessionManager, "sessionManager must be non-null.");
    }

    @Override
    public List<String> getRoute() {
        return ROUTE;
    }

    @Override
    public String getDescription() {
        return "Play a story";
    }

    @Override
    public Optional<String> getUsage() {
        return Optional.empty();
    }

    @Override
    public void enact(MessageReceivedEvent messageReceivedEvent, String args) {
        IChannel channel = messageReceivedEvent.getChannel();
        SessionKey sessionKey = new SessionKey(channel.getStringID());

        try {
            Session session = sessionManager.createSession(sessionKey, channel, args);
            BotUtils.sendMessage(messageReceivedEvent.getChannel(), "Starting game...");

            try {
                channel.changeTopic("Now playing: " + session.getStoryName());
            } catch (MissingPermissionsException e) {
                LOG.debug("Couldn't get permissions on channel.", e);
            }

        } catch (TonbotException e) {
            BotUtils.sendMessage(messageReceivedEvent.getChannel(), e.getMessage());
        }
    }
}
