package net.tonbot.plugin.ifplayer;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.tonberry.tonbot.common.Activity;
import com.tonberry.tonbot.common.ActivityDescriptor;
import com.tonberry.tonbot.common.BotUtils;
import com.tonberry.tonbot.common.Prefix;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.util.EmbedBuilder;

class IfPlayerListStoriesActivity implements Activity {

    private static final ActivityDescriptor ACTIVITY_DESCRIPTOR = ActivityDescriptor.builder()
            .route(ImmutableList.of("if", "ls"))
            .description("Lists the available stories for play.")
            .build();

    private final String prefix;
    private final StoryLibrary storyLibrary;

    @Inject
    public IfPlayerListStoriesActivity(@Prefix String prefix, StoryLibrary storyLibrary) {
        this.prefix = Preconditions.checkNotNull(prefix, "prefix must be non-null.");
        this.storyLibrary = Preconditions.checkNotNull(storyLibrary, "storyLibrary must be non-null.");
    }

    @Override
    public ActivityDescriptor getDescriptor() {
        return ACTIVITY_DESCRIPTOR;
    }

    @Override
    public void enact(MessageReceivedEvent messageReceivedEvent, String args) {
        List<String> storyNames = storyLibrary.listStories();

        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.withTitle("Interactive Fiction Stories");
        embedBuilder.withDesc(StringUtils.join(storyNames, "\n")
                + "\n\nType ``" + prefix + " if play <STORY NAME>`` to play them.");


        BotUtils.sendEmbeddedContent(messageReceivedEvent.getChannel(), embedBuilder.build());
    }
}
