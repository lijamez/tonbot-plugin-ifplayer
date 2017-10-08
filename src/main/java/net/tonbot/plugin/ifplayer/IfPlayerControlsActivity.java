package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import net.tonbot.common.Activity;
import net.tonbot.common.ActivityDescriptor;
import net.tonbot.common.BotUtils;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.util.EmbedBuilder;

class IfPlayerControlsActivity implements Activity {

	private static final ActivityDescriptor ACTIVITY_DESCRIPTOR = ActivityDescriptor.builder()
			.route("if controls")
			.description("Displays information about controls.")
			.build();

	private final BotUtils botUtils;

	@Inject
	public IfPlayerControlsActivity(BotUtils botUtils) {
		this.botUtils = Preconditions.checkNotNull(botUtils, "botUtils must be non-null.");
	}

	@Override
	public ActivityDescriptor getDescriptor() {
		return ACTIVITY_DESCRIPTOR;
	}

	@Override
	public void enact(MessageReceivedEvent messageReceivedEvent, String args) {
		EmbedBuilder embedBuilder = new EmbedBuilder();
		embedBuilder.withTitle("Interactive Fiction Player Controls");
		embedBuilder.withDescription("When you are playing a story, simply enter the responses in the channel.\n\n"
				+ "To save a game, say ``save`` while you are playing it.\n"
				+ "To load a game, start the story and then say ``restore``.\n\n"
				+ "There may be situations where you need to enter a certain key that's not allowed by Discord, such as Enter. "
				+ "To enter one of these special keys, say the following instead:");

		embedBuilder.appendField("Enter or return", "``<enter>`` or ``<return>``", false);
		embedBuilder.appendField("Arrow keys", "``<up>``, ``<down>``, ``<left>``, ``<right>``", false);
		embedBuilder.appendField("Function keys", "``<f#>``    e.g: ``<f4>``, ``<f12>``", false);
		embedBuilder.appendField("Numpad keys", "``<num#>``    e.g: ``<num2>``", false);
		embedBuilder.appendField("Escape", "``<esc>``", false);

		botUtils.sendEmbed(messageReceivedEvent.getChannel(), embedBuilder.build());
	}
}
