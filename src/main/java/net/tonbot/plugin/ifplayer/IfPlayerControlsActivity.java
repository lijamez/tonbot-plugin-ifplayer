package net.tonbot.plugin.ifplayer;

import com.google.common.collect.ImmutableList;
import com.tonberry.tonbot.common.Activity;
import com.tonberry.tonbot.common.ActivityDescriptor;
import com.tonberry.tonbot.common.BotUtils;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.util.EmbedBuilder;

class IfPlayerControlsActivity implements Activity {

    private static final ActivityDescriptor ACTIVITY_DESCRIPTOR = ActivityDescriptor.builder()
            .route(ImmutableList.of("if", "controls"))
            .description("Displays information about controls.")
            .build();

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
        
        BotUtils.sendEmbeddedContent(messageReceivedEvent.getChannel(), embedBuilder.build());
    }
}
