package net.tonbot.plugin.ifplayer;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.vdurmont.emoji.EmojiParser;

import net.tonbot.common.Prefix;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.MessageTokenizer;

class IfPlayerSendLineListener {

    private final String prefix;
    private final SessionOrchestrator sessionOrchestrator;

    @Inject
    public IfPlayerSendLineListener(
    		@Prefix String prefix, 
    		SessionOrchestrator sessionOrchestrator) {
        this.prefix = Preconditions.checkNotNull(prefix, "prefix must be non-null.");
        this.sessionOrchestrator = Preconditions.checkNotNull(sessionOrchestrator, "sessionOrchestrator must be non-null.");
    }

    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent messageReceivedEvent) {
    		
    		if (shouldMessageBeIgnored(messageReceivedEvent.getMessage())) {
    			return;
    		}

        String input = messageReceivedEvent.getMessage().getContent();
        sessionOrchestrator.advance(input, messageReceivedEvent.getChannel());
    }
    
    private boolean shouldMessageBeIgnored(IMessage message) {
    		String originalMessage = message.getContent();
    	
    		if (StringUtils.isBlank(originalMessage)) {
    			return true;
    		}
    	
		MessageTokenizer tokenizer = new MessageTokenizer(message);
		
		// For reasons only known to Discord devs, @everyone is not a mention so we need to check it explicitly.
		// hasNextEmoji doesn't return true for "standard" emojis, such as :D
		return tokenizer.hasNextEmoji() 
				|| tokenizer.hasNextMention() 
				|| tokenizer.hasNextInvite() 
				|| originalMessage.contains("@everyone")
				|| originalMessage.startsWith(prefix)
				|| !EmojiParser.removeAllEmojis(originalMessage).equals(originalMessage);
    }
}
