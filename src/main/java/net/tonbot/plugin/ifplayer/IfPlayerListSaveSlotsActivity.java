package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import net.tonbot.common.Activity;
import net.tonbot.common.ActivityDescriptor;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;

class IfPlayerListSaveSlotsActivity implements Activity {

	private static final ActivityDescriptor ACTIVITY_DESCRIPTOR = ActivityDescriptor.builder()
			.route(ImmutableList.of("if", "slots"))
			.description("Lists the save slots for the current story.")
			.build();

	private final SessionOrchestrator sessionOrchestrator;

	@Inject
	public IfPlayerListSaveSlotsActivity(
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
		
		sessionOrchestrator.listSlots(channel);
	}
}
