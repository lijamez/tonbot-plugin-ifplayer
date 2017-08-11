package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.tonberry.tonbot.common.BotUtils;
import com.tonberry.tonbot.common.MessageReceivedAction;
import com.tonberry.tonbot.common.Prefix;
import org.apache.commons.lang3.StringUtils;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.util.EmbedBuilder;

import java.util.List;

public class IfPlayerListStoriesAction implements MessageReceivedAction {

    private static final List<String> ROUTE = ImmutableList.of("if", "ls");

    private final String prefix;
    private final StoryLibrary storyLibrary;

    @Inject
    public IfPlayerListStoriesAction(@Prefix String prefix, StoryLibrary storyLibrary) {
        this.prefix = Preconditions.checkNotNull(prefix, "prefix must be non-null.");
        this.storyLibrary = Preconditions.checkNotNull(storyLibrary, "storyLibrary must be non-null.");
    }

    @Override
    public List<String> getRoute() {
        return ROUTE;
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
