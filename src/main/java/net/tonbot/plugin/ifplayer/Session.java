package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;

import lombok.Data;
import sx.blah.discord.handle.obj.IChannel;

@Data
class Session {

	private final SessionKey sessionKey;
    private final String name;
    private final GameMachine gameMachine;

    public Session(
    		SessionKey sessionKey, 
    		String name, 
    		Story story, 
    		SaveFile saveFile, 
    		IChannel channel,
    		OnSavedCallback onSavedCallback) {
    	
    		this.sessionKey = Preconditions.checkNotNull(sessionKey, "sessionKey must be non-null.");
        this.name = Preconditions.checkNotNull(name, "name must be non-null.");

        Preconditions.checkNotNull(story, "story must be non-null.");
        Preconditions.checkNotNull(saveFile, "saveFile must be non-null.");
        Preconditions.checkNotNull(onSavedCallback, "onSavedCallback must be non-null.");
        
        this.gameMachine = new GameMachine(story, channel.getLongID(), onSavedCallback);
        this.gameMachine.setSaveFile(saveFile);
    }
}
