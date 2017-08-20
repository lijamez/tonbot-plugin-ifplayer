package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import net.tonbot.common.Activity;
import net.tonbot.common.ActivityDescriptor;
import net.tonbot.common.TonbotBusinessException;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;

class IfPlayerStopStoryActivity implements Activity {

	private static final ActivityDescriptor ACTIVITY_DESCRIPTOR = ActivityDescriptor.builder()
			.route(ImmutableList.of("if", "stop"))
			.description("Stops playing the current story.")
			.build();

	private final SessionOrchestrator sessionOrchestrator;

	@Inject
	public IfPlayerStopStoryActivity(
			SessionOrchestrator sessionOrchestrator) {
		this.sessionOrchestrator = Preconditions.checkNotNull(sessionOrchestrator,
				"sessionOrchestrator must be non-null.");
	}

	@Override
	public ActivityDescriptor getDescriptor() {
		return ACTIVITY_DESCRIPTOR;
	}

	@Override
	public void enact(MessageReceivedEvent messageReceivedEvent, String args) {
		IChannel channel = messageReceivedEvent.getChannel();

		boolean sessionExisted = sessionOrchestrator.end(channel);
		if (!sessionExisted) {
			throw new TonbotBusinessException("You're not playing anything!");
		}
	}
}
