package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import net.tonbot.common.Activity;
import net.tonbot.common.ActivityDescriptor;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;

class IfPlayerStopStoryActivity implements Activity {

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

        sessionManager.endSession(sessionKey);
    }
}
