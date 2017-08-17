package net.tonbot.plugin.ifplayer;

import java.io.File;

import com.google.common.base.Preconditions;

import lombok.Data;
import sx.blah.discord.handle.obj.IChannel;

@Data
class Session {

	private final SessionKey sessionKey;
    private final String name;
    private final GameMachine gameMachine;

    public Session(SessionKey sessionKey, String name, File storyFile, File saveFile, IChannel channel) {
    		this.sessionKey = Preconditions.checkNotNull(sessionKey, "sessionKey must be non-null.");
        this.name = Preconditions.checkNotNull(name, "name must be non-null.");

        Preconditions.checkNotNull(storyFile, "storyFile must be non-null.");
        Preconditions.checkNotNull(saveFile, "saveFile must be non-null.");

        Story story = Story.loadFrom(storyFile);
        
        this.gameMachine = new GameMachine(story);
        this.gameMachine.setSaveFile(saveFile);
    }
}
