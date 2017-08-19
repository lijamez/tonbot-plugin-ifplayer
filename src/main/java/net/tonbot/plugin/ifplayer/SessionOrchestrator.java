package net.tonbot.plugin.ifplayer;

import sx.blah.discord.handle.obj.IChannel;

/**
 * Responsible for sending input to a session and then updating Discord to reflect the updates to the game state. 
 */
interface SessionOrchestrator {

	/**
	 * Creates (or replaces) a session for this channel.
	 * @param channel {@link IChannel}.
	 */
	void create(IChannel channel, String storyName);
	
	/**
	 * Advances the session. No-op if the given channel has no session.
	 * @param input The input to to provide to the game session. Nullable.
	 * @param channel {@link IChannel}. Non-null.
	 */
	void advance(String input, IChannel channel);
	
	/**
	 * Ends the session. No-op if the given channel has no session.
	 * @param channel {@link IChannel}. Non-null.
	 * @return True if the session ended. False if there was no session to be ended.
	 */
	boolean end(IChannel channel);
	
	/**
	 * Determines if the given channel has a session.
	 * @param channel {@link IChannel}. Non-null.
	 * @return True iff a session exists.
	 */
	boolean hasSession(IChannel channel);
}
