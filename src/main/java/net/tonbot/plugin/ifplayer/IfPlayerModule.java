package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.tonberry.tonbot.common.Activity;
import com.tonberry.tonbot.common.Prefix;

import java.io.File;
import java.util.Set;

class IfPlayerModule extends AbstractModule {

    private final String prefix;
    private final File storyDir;
    private final File saveDir;

    public IfPlayerModule(String prefix, File storyDir, File saveDir) {
        this.prefix = Preconditions.checkNotNull(prefix, "prefix must be non-null.");

        Preconditions.checkNotNull(storyDir, "storyDir must be non-null.");
        Preconditions.checkArgument(storyDir.isDirectory(), "storyDir must be a directory.");
        this.storyDir = storyDir;

        Preconditions.checkNotNull(saveDir, "saveDir must be non-null.");
        Preconditions.checkArgument(saveDir.isDirectory(), "saveDir must be a directory.");
        this.saveDir = saveDir;
    }

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Prefix.class).toInstance(prefix);
        bind(File.class).annotatedWith(StoryDir.class).toInstance(storyDir);
        bind(File.class).annotatedWith(SaveDir.class).toInstance(saveDir);

        bind(SessionManager.class).to(SessionManagerImpl.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    Set<Activity> activities(
            IfPlayerListStoriesActivity listStoriesActivity,
            IfPlayerPlayStoryAction playStoryActivity,
            IfPlayerStopStoryActivity stopStoryActivity) {
        return ImmutableSet.of(listStoriesActivity, playStoryActivity, stopStoryActivity);
    }
}
