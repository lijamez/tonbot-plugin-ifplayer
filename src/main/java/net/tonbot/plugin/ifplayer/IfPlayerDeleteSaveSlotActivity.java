package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import net.tonbot.common.Activity;
import net.tonbot.common.ActivityDescriptor;
import net.tonbot.common.ActivityUsageException;
import net.tonbot.common.Enactable;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;

class IfPlayerDeleteSaveSlotActivity implements Activity {

	private static final ActivityDescriptor ACTIVITY_DESCRIPTOR = ActivityDescriptor.builder().route("if delslot")
			.parameters(ImmutableList.of("<slot number>")).description("Deletes a save slot for the current story.")
			.build();

	private final SessionOrchestrator sessionOrchestrator;
	private final SaveManager saveManager;

	@Inject
	public IfPlayerDeleteSaveSlotActivity(SessionOrchestrator sessionOrchestrator, SaveManager saveManager) {
		this.sessionOrchestrator = Preconditions.checkNotNull(sessionOrchestrator,
				"sessionOrchestrator must be non-null.");
		this.saveManager = Preconditions.checkNotNull(saveManager, "saveManager must be non-null.");
	}

	@Override
	public ActivityDescriptor getDescriptor() {
		return ACTIVITY_DESCRIPTOR;
	}

	@Enactable
	public void enact(MessageReceivedEvent messageReceivedEvent, IfPlayerDeleteSaveSlotRequest request) {
		IChannel channel = messageReceivedEvent.getChannel();

		if (request.getSlotNumber() < 0 || request.getSlotNumber() >= saveManager.getMaxSlots()) {
			throw new ActivityUsageException("You need to enter a number from 0 to " + (saveManager.getMaxSlots() - 1));
		}

		sessionOrchestrator.deleteSaveSlot(channel, request.getSlotNumber());
	}
}
