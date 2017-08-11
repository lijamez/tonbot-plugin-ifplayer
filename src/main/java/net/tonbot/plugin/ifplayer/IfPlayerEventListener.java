package net.tonbot.plugin.ifplayer;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.tonberry.tonbot.common.EventDispatcher;
import com.tonberry.tonbot.common.Prefix;

public class IfPlayerEventListener extends EventDispatcher {

    @Inject
    public IfPlayerEventListener(
            @Prefix String prefix,
            IfPlayerListStoriesAction listStoriesAction,
            IfPlayStoryAction playStoryAction,
            IfPlayerStopStoryAction stopStoryAction) {
        super(prefix, ImmutableSet.of(listStoriesAction, playStoryAction, stopStoryAction));
    }
}
