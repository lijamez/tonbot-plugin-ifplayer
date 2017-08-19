package net.tonbot.plugin.ifplayer;

import java.util.List;

interface SaveManager {

	/**
	 * Gets the maximum number of slots.
	 * 
	 * @return The maximum number of slots.
	 */
	int getMaxSlots();

	/**
	 * Lists all of the {@link SaveFile}s for the given {@link Story}.
	 * 
	 * @param channelId
	 *            The channel ID.
	 * @param story
	 *            The {@link Story} to get {@link SaveFile}s of. Non-null.
	 * 
	 * @return A list of {@link SaveFile}s.
	 */
	List<SaveFile> getSaveFiles(long channelId, Story story);

	/**
	 * Gets a save file for a given story.
	 * 
	 * @param channelId
	 *            The channel ID.
	 * @param story
	 *            The {@link Story}. Non-null.
	 * @param slot
	 *            The slot number. Must be non-negative and less than the value
	 *            returned by {@code getMaxSlots()}
	 * 
	 * @return A {@link SaveFile} which has a backed file on the file system.
	 */
	SaveFile getSaveFile(long channelId, Story story, int slot);

	/**
	 * The onSaved callback function. This function is to be called whenever a file
	 * is saved, so that the SaveManager can update its metadata with the provided
	 * {@link SaveFileMetadata} and then return a new {@link SaveFile} with the
	 * updated metadata.
	 * 
	 * @param channelId
	 *            Channel ID.
	 * @param saveFile
	 *            The save file. Non-null.
	 * @param newSaveFileMetadata
	 *            The new save file metadata. Non-null.
	 * @return A new {@link SaveFile} with the updated metadata.
	 */
	SaveFile saveNewMetadata(long channelId, SaveFile saveFile, SaveFileMetadata newSaveFileMetadata);

	/**
	 * Deletes a save slot.
	 * 
	 * @param channelId
	 *            The channel ID.
	 * @param story
	 *            {@link Story}. Non-null.
	 * @param slot
	 *            The slot number to delete.
	 */
	void deleteSaveFile(long channelId, Story story, int slot);
}
