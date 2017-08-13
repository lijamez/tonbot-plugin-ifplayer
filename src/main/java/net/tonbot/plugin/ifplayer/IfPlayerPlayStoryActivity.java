package net.tonbot.plugin.ifplayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.tonberry.tonbot.common.Activity;
import com.tonberry.tonbot.common.ActivityDescriptor;
import com.tonberry.tonbot.common.BotUtils;
import com.tonberry.tonbot.common.TonbotException;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.MissingPermissionsException;

class IfPlayerPlayStoryActivity implements Activity {

    private static final Logger LOG = LoggerFactory.getLogger(IfPlayerPlayStoryActivity.class);

    private static final ActivityDescriptor ACTIVITY_DESCRIPTOR = ActivityDescriptor.builder()
            .route(ImmutableList.of("if", "play"))
            .parameters(ImmutableList.of("story name"))
            .description("Play a story")
            .build();

    private final SessionManager sessionManager;

    @Inject
    public IfPlayerPlayStoryActivity(SessionManager sessionManager) {
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
