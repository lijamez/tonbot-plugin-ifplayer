package net.tonbot.plugin.ifplayer

import spock.lang.Specification
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage

class IfPlayerSendLineListenerTest extends Specification {

	private static final String PREFIX = "t!"
	
	SessionOrchestrator sessionOrchestrator
	IfPlayerSendLineListener listener
	
	def setup() {
		this.sessionOrchestrator = Mock(SessionOrchestrator);
		
		this.listener = new IfPlayerSendLineListener(PREFIX, sessionOrchestrator);
	}
	
	def "ignorable messages"(String message) {
		given:
		MessageReceivedEvent mockedMre = Mock()
		IMessage mockedMessage = Mock()
		IDiscordClient mockedClient = Mock()
		
		when:
		listener.onMessageReceived(mockedMre)
		
		then:
		mockedMre.getMessage() >> mockedMessage
		mockedMessage.getClient() >> mockedClient
		mockedMessage.getContent() >> message
		
		then:
		0 * _
		
		where:
		message                       | _
		"<@80351110224678912>"        | _
		"<@!80351110224678912>"       | _
		"<#103735883630395392>"       | _
		"<@&165511591545143296>"      | _
		"<:mmLol:216154654256398347>" | _
		"t! something"                | _
		"lol 😄"                      | _
		"🤣"                          | _
		"yo @everyone"                | _
		"      "                      | _
		""                            | _
		"\t"                          | _
	}
	
	def "do nothing because there's no session"() {
		given:
		String message = "kick troll in face"
		MessageReceivedEvent mockedMre = Mock()
		IMessage mockedMessage = Mock()
		IDiscordClient mockedClient = Mock()
		IChannel mockedChannel = Mock()
		
		when:
		listener.onMessageReceived(mockedMre)
		
		then:
		mockedMre.getMessage() >> mockedMessage
		mockedMre.getChannel() >> mockedChannel
		mockedMessage.getClient() >> mockedClient
		mockedMessage.getContent() >> message
		
		then:
		1 * sessionOrchestrator.advance(message, mockedChannel)
		
		then:
		0 * _
	}
	
	def "send message to session"(String message) {
		given:
		MessageReceivedEvent mockedMre = Mock()
		IMessage mockedMessage = Mock()
		IDiscordClient mockedClient = Mock()
		IChannel mockedChannel = Mock()
		
		when:
		listener.onMessageReceived(mockedMre)
		
		then:
		mockedMre.getMessage() >> mockedMessage
		mockedMre.getChannel() >> mockedChannel
		mockedMessage.getClient() >> mockedClient
		mockedMessage.getContent() >> message
		
		1 * sessionOrchestrator.advance(message, mockedChannel)
		
		then:
		0 * _
		
		where:
		message              | _
		"kick troll in face" | _
		"scream"             | _
		":o"                 | _
		"><"                 | _
	}
}
