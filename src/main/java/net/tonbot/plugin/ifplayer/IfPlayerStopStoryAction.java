package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.tonberry.tonbot.common.BotUtils;
import com.tonberry.tonbot.common.MessageReceivedAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.MissingPermissionsException;

import java.util.List;

public class IfPlayerStopStoryAction implements MessageReceivedAction {

    private static final Logger LOG = LoggerFactory.getLogger(IfPlayerStopStoryAction.class);

    private static final List<String> ROUTE = ImmutableList.of("if", "stop");

    private final SessionManager sessionManager;

    @Inject
    public IfPlayerStopStoryAction(SessionManager sessionManager) {
        this.sessionManager = Preconditions.checkNotNull(sessionManager, "sessionManager must be non-null.");
    }

    @Override
    public List<String> getRoute() {
        return ROUTE;
    }

    @Override
    public void enact(MessageReceivedEvent messageReceivedEvent, String args) {
        IChannel channel = messageReceivedEvent.getChannel();
        SessionKey sessionKey = new SessionKey(channel.getStringID());

        try {
            sessionManager.endSession(sessionKey);
            channel.changeTopic("");
            BotUtils.sendMessage(messageReceivedEvent.getChannel(), "Game has stopped.");
        } catch (MissingPermissionsException e) {
            LOG.debug("Couldn't get permissions on channel.", e);
        }
    }
}
