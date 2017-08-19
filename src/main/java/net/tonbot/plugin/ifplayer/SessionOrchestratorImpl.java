package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import net.tonbot.common.BotUtils;
import sx.blah.discord.handle.obj.IChannel;

class SessionOrchestratorImpl implements SessionOrchestrator {
	
	private static final Logger LOG = LoggerFactory.getLogger(SessionOrchestratorImpl.class);

	private final SessionManager sessionManager;
    private final ScreenStateRenderer screenStateRenderer;
    private final StoryLibrary storyLibrary;
    
    @Inject
    public SessionOrchestratorImpl(
    		SessionManager sessionManager,
    		ScreenStateRenderer screenStateRenderer,
    		StoryLibrary storyLibrary) {
		this.sessionManager = Preconditions.checkNotNull(sessionManager, "sessionManager must be non-null.");
		this.screenStateRenderer = Preconditions.checkNotNull(screenStateRenderer, "screenStateRenderer must be non-null.");
		this.storyLibrary = Preconditions.checkNotNull(storyLibrary, "storyLibrary must be non-null.");
    }
    
	@Override
	public void create(IChannel channel, String storyName) {
		Preconditions.checkNotNull(channel, "channel must be non-null.");
		Preconditions.checkNotNull(storyName, "storyName must be non-null.");
		
		List<File> foundFiles = storyLibrary.findStories(storyName);
		if (foundFiles.isEmpty()) {
			BotUtils.sendMessage(channel, "There is no story with that name.");
			return;
		} else if (foundFiles.size() > 1) {
			BotUtils.sendMessage(channel, "You're going to have to be more specific than that.");
			return;
		}
		
		File storyFile = foundFiles.get(0);
		
		// End the current session, if any.
		this.end(channel);
		
		// Create a new session.
		SessionKey sessionKey = new SessionKey(channel.getLongID());
		sessionManager.createSession(sessionKey, channel, storyFile);
		
		// Advance it one step
		this.advance(null, channel);
	}
    
    @Override
    public boolean hasSession(IChannel channel) {
    		return getSession(channel) != null;
    }
	
    @Override
	public void advance(String input, IChannel channel) {
		Preconditions.checkNotNull(channel, "channel must be non-null.");
		
		Session session = getSession(channel);
		
		if (session == null) {
			return;
		}
		
        GameMachine gameMachine = session.getGameMachine();
        
        Optional<ScreenState> screenState;
        try {
        		screenState = gameMachine.takeTurn(input);
        } catch (GameMachineException e) {
        		// Handle a non-fatal exception.
    			BotUtils.sendMessage(channel, "Error: " + e.getMessage());
    			return;
        } catch (Exception e) {
        		LOG.error("GameMachine has thrown an unexpected exception.", e);
        		BotUtils.sendMessage(channel, "The player has crashed! :(");
        		this.end(channel);
        		return;
        }
        
        screenStateRenderer.render(session, screenState.orElse(null), channel);
        
        	if (!screenState.isPresent()) {
        		sessionManager.removeSession(session.getSessionKey());
        		BotUtils.sendMessage(channel, "Story '" + gameMachine.getStory().getName() + "' has stopped.");            
        }
	}

	@Override
	public boolean end(IChannel channel) {
		Preconditions.checkNotNull(channel, "channel must be non-null.");
		
		Session session = getSession(channel);
		
		if (session == null) {
			return false;
		}
		
		session.getGameMachine().stop();
		sessionManager.removeSession(session.getSessionKey());
		
		screenStateRenderer.render(session, null, channel);
		BotUtils.sendMessage(channel, "Story '" + session.getGameMachine().getStory().getName() + "' has stopped.");
		return true;
	}
	
	private Session getSession(IChannel channel) {
		SessionKey sessionKey = new SessionKey(channel.getLongID());
		Session session = sessionManager.getSession(sessionKey).orElse(null);
		return session;
	}
}
