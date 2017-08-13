package net.tonbot.plugin.ifplayer;

import java.io.File;

import com.google.common.base.Preconditions;

import lombok.Getter;
import sx.blah.discord.handle.obj.IChannel;

class Session {

    @Getter
    private final String name;
    private final GameStateManager stateManager;

    public Session(String name, File storyFile, File saveFile, IChannel channel) {
        this.name = Preconditions.checkNotNull(name, "name must be non-null.");

        Preconditions.checkNotNull(storyFile, "storyFile must be non-null.");
        Preconditions.checkNotNull(saveFile, "saveFile must be non-null.");

        Story story = Story.loadFrom(storyFile);
        this.stateManager = new GameStateManager(story, saveFile, channel);
        this.stateManager.start();
    }

    public String getStoryName() {
        return this.stateManager.getStory().getName();
    }

    public void sendText(String text) {
        this.stateManager.supplyInput(text);
    }

    /**
     * Ends the session.
     */
    public void end() {
        this.stateManager.stop();
    }
}
