package net.tonbot.plugin.ifplayer;

import net.tonbot.common.TonbotBusinessException;
import sx.blah.discord.handle.obj.IChannel;

/**
 * Responsible for sending input to a session and then updating Discord to
 * reflect the updates to the game state.
 */
interface SessionOrchestrator {

	/**
	 * Creates (or replaces) a session for this channel.
	 * 
	 * @param channel
	 *            {@link IChannel}.
	 * @param storyName
	 *            The name of the story to start. Non-null.
	 * @param username
	 *            The name of the user who tried to create a session.
	 * @throws TonbotBusinessException
	 *             if there is no story with the provided name, or if there are
	 *             multiple matches.
	 */
	void create(IChannel channel, String storyName, String username);

	/**
	 * Advances the session. No-op if the given channel has no session.
	 * 
	 * @param input
	 *            The input to to provide to the game session. Nullable.
	 * @param channel
	 *            {@link IChannel}. Non-null.
	 * @param username
	 *            The name of the user who sent this input. Non-null.
	 */
	void advance(String input, IChannel channel, String username);

	/**
	 * Switches the save slot. No-op if there is no session.
	 * 
	 * @param channel
	 *            {@link IChannel}. Non-null.
	 * @param saveSlot
	 *            The desired save slot.
	 */
	void switchSave(IChannel channel, int saveSlot);

	/**
	 * Ends the session. No-op if the given channel has no session.
	 * 
	 * @param channel
	 *            {@link IChannel}. Non-null.
	 * @return True if the session ended. False if there was no session to be ended.
	 */
	boolean end(IChannel channel);

	/**
	 * Determines if the given channel has a session.
	 * 
	 * @param channel
	 *            {@link IChannel}. Non-null.
	 * @return True iff a session exists.
	 */
	boolean hasSession(IChannel channel);

	/**
	 * Lists the slots for the current story. No-op if there is no session.
	 * 
	 * @param channel
	 *            {@link IChannel}. Non-null.
	 */
	void listSlots(IChannel channel);

	/**
	 * Deletes a save slot for the current story. No-op if there is no session.
	 * 
	 * @param channel
	 *            {@link IChannel}. Non-null.
	 * @param slotNumber
	 *            The slot number.
	 */
	void deleteSaveSlot(IChannel channel, int slotNumber);
}
