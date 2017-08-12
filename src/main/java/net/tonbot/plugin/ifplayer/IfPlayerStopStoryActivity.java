package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.tonberry.tonbot.common.Activity;
import com.tonberry.tonbot.common.BotUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.MissingPermissionsException;

import java.util.List;
import java.util.Optional;

class IfPlayerStopStoryActivity implements Activity {

    private static final Logger LOG = LoggerFactory.getLogger(IfPlayerStopStoryActivity.class);

    private static final List<String> ROUTE = ImmutableList.of("if", "stop");

    private final SessionManager sessionManager;

    @Inject
    public IfPlayerStopStoryActivity(SessionManager sessionManager) {
        this.sessionManager = Preconditions.checkNotNull(sessionManager, "sessionManager must be non-null.");
    }

    @Override
    public List<String> getRoute() {
        return ROUTE;
    }

    @Override
    public String getDescription() {
        return "Stops playing the current story.";
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
            sessionManager.endSession(sessionKey);
            channel.changeTopic("");
            BotUtils.sendMessage(messageReceivedEvent.getChannel(), "Game has stopped.");
        } catch (MissingPermissionsException e) {
            LOG.debug("Couldn't get permissions on channel.", e);
        }
    }
}
