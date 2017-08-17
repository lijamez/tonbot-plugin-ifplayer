package net.tonbot.plugin.ifplayer;

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
    
    @Inject
    public SessionOrchestratorImpl(
    		SessionManager sessionManager,
    		ScreenStateRenderer screenStateRenderer) {
		this.sessionManager = Preconditions.checkNotNull(sessionManager, "sessionManager must be non-null.");
		this.screenStateRenderer = Preconditions.checkNotNull(screenStateRenderer, "screenStateRenderer must be non-null.");
    }
	
    @Override
	public void advance(Session session, String input, IChannel channel) {
		Preconditions.checkNotNull(session, "session must be non-null.");
		Preconditions.checkNotNull(channel, "channel must be non-null.");
		
        GameMachine gameMachine = session.getGameMachine();
        
        Optional<ScreenState> screenState = null;
        try {
        		screenState = gameMachine.takeTurn(input);
        } catch (GameMachineException e) {
    			BotUtils.sendMessage(channel, "Error: " + e.getMessage());
        } catch (Exception e) {
        		LOG.error("GameMachine has thrown an unexpected exception.", e);
        		BotUtils.sendMessage(channel, "The player has crashed! :(");
        }
        
        if (screenState != null) {
            	if (screenState.isPresent()) {
                screenStateRenderer.render(session, screenState.get(), channel);
            } else {
            		sessionManager.endSession(session.getSessionKey());
            		BotUtils.sendMessage(channel, "Story '" + gameMachine.getStory().getName() + "' has stopped.");
            }
        }
	}
}
