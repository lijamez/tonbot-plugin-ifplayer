package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.tonberry.tonbot.common.Activity;
import com.tonberry.tonbot.common.ActivityDescriptor;
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

    private static final ActivityDescriptor ACTIVITY_DESCRIPTOR = ActivityDescriptor.builder()
            .route(ImmutableList.of("if", "stop"))
            .description("Stops playing the current story.")
            .build();
    private final SessionManager sessionManager;

    @Inject
    public IfPlayerStopStoryActivity(SessionManager sessionManager) {
        this.sessionManager = Preconditions.checkNotNull(sessionManager, "sessionManager must be non-null.");
    }

    @Override
    public ActivityDescriptor getDescriptor() {
        return ACTIVITY_DESCRIPTOR;
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
