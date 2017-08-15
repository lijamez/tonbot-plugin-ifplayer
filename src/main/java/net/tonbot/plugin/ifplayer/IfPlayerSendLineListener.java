package net.tonbot.plugin.ifplayer;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.tonberry.tonbot.common.Prefix;
import com.vdurmont.emoji.EmojiParser;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.MessageTokenizer;

class IfPlayerSendLineListener {
	
    private final String prefix;
    private final SessionManager sessionManager;

    @Inject
    public IfPlayerSendLineListener(@Prefix String prefix, SessionManager sessionManager) {
        this.prefix = Preconditions.checkNotNull(prefix, "prefix must be non-null.");
        this.sessionManager = Preconditions.checkNotNull(sessionManager, "sessionManager must be non-null.");
    }

    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent messageReceivedEvent) {
    		
    		if (shouldMessageBeIgnored(messageReceivedEvent.getMessage())) {
    			return;
    		}

        SessionKey sessionKey = new SessionKey(messageReceivedEvent.getChannel().getStringID());

        Session session = sessionManager.getSession(sessionKey)
                .orElse(null);

        if (session != null) {
            String message = messageReceivedEvent.getMessage().getContent();
            session.sendText(message);
        }
    }
    
    private boolean shouldMessageBeIgnored(IMessage message) {
    		String originalMessage = message.getContent();
    	
    		if (StringUtils.isBlank(originalMessage)) {
    			return true;
    		}
    	
		MessageTokenizer tokenizer = new MessageTokenizer(message);
		
		// For some reason, @everyone is not a mention, so we need to check it explicitly.
		// hasNextEmoji doesn't return true for "standard" emojis, such as :D
		return tokenizer.hasNextEmoji() 
				|| tokenizer.hasNextMention() 
				|| tokenizer.hasNextInvite() 
				|| originalMessage.contains("@everyone")
				|| originalMessage.startsWith(prefix)
				|| !EmojiParser.removeAllEmojis(originalMessage).equals(originalMessage);
    }
}
