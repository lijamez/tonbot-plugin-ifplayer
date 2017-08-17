package net.tonbot.plugin.ifplayer;

import sx.blah.discord.handle.obj.IChannel;

/**
 * Responsible for sending input to a session and then updating Discord to reflect the updates to the game state. 
 */
interface SessionOrchestrator {

	/**
	 * Advances the session.
	 * @param session {@link Session} to be advanced. Non-null.
	 * @param input The input to to provide to the game session. Nullable.
	 * @param channel {@link IChannel}. Non-null.
	 */
	void advance(Session session, String input, IChannel channel);
}
