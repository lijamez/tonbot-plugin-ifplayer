package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import net.tonbot.common.Activity;
import net.tonbot.common.ActivityDescriptor;
import net.tonbot.common.Enactable;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

class IfPlayerPlayStoryActivity implements Activity {

	private static final ActivityDescriptor ACTIVITY_DESCRIPTOR = ActivityDescriptor.builder().route("if play")
			.parameters(ImmutableList.of("<story name>")).description("Plays a story in the current channel.").build();

	private final SessionOrchestrator sessionOrchestrator;

	@Inject
	public IfPlayerPlayStoryActivity(SessionOrchestrator sessionOrchestrator) {
		this.sessionOrchestrator = Preconditions.checkNotNull(sessionOrchestrator,
				"sessionOrchestrator must be non-null.");
	}

	@Override
	public ActivityDescriptor getDescriptor() {
		return ACTIVITY_DESCRIPTOR;
	}

	@Enactable
	public void enact(MessageReceivedEvent messageReceivedEvent, IfPlayerPlayStoryRequest request) {
		sessionOrchestrator.create(messageReceivedEvent.getChannel(), request.getStoryName(),
				messageReceivedEvent.getAuthor().getName());
	}
}
