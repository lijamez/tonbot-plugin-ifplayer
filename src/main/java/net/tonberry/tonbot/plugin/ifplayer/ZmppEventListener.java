package net.tonberry.tonbot.plugin.ifplayer;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.tonberry.tonbot.common.EventDispatcher;
import com.tonberry.tonbot.common.Prefix;

public class ZmppEventListener extends EventDispatcher {

    @Inject
    public ZmppEventListener(
            @Prefix String prefix,
            ZmppListStoriesAction listStoriesAction,
            ZmppPlayStoryAction playStoryAction,
            ZmppStopStoryAction stopStoryAction) {
        super(prefix, ImmutableSet.of(listStoriesAction, playStoryAction, stopStoryAction));
    }
}
