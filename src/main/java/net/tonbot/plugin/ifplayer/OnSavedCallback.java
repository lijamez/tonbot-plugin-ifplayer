package net.tonbot.plugin.ifplayer;

interface OnSavedCallback {

	SaveFile getOnSavedCallback(long channelId, SaveFile saveFile, SaveFileMetadata newSaveFileMetadata);
}
