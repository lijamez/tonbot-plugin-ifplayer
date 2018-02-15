package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

class SaveManagerImpl implements SaveManager {

	private final File saveDir;
	private final int maxSlots;
	private final ObjectMapper objectMapper;

	@Inject
	public SaveManagerImpl(@SaveDir File saveDir, @MaxSaveSlots int maxSlots) {
		this.saveDir = Preconditions.checkNotNull(saveDir, "saveDir must be non-null.");

		Preconditions.checkArgument(maxSlots > 0, "maxSlots must be a positive number.");
		this.maxSlots = maxSlots;

		this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}

	@Override
	public int getMaxSlots() {
		return maxSlots;
	}

	@Override
	public List<SaveFile> getSaveFiles(long channelId, Story story) {
		Preconditions.checkNotNull(story, "story must be non-null.");

		ImmutableList.Builder<SaveFile> saveFilesListBuilder = ImmutableList.builder();

		for (int i = 0; i < maxSlots; i++) {
			getSaveFile(channelId, story, i, false).ifPresent(saveFile -> saveFilesListBuilder.add(saveFile));
		}

		return saveFilesListBuilder.build();
	}

	@Override
	public SaveFile getSaveFile(long channelId, Story story, int slot) {
		Preconditions.checkArgument(slot >= 0, "slot must be non-negative.");
		Preconditions.checkArgument(slot < maxSlots, "slot must be less than the max slots.");

		SaveFile saveFile = getSaveFile(channelId, story, slot, true).get();

		return saveFile;
	}

	private Optional<SaveFile> getSaveFile(long channelId, Story story, int slot, boolean create) {
		String expectedSaveFileName = getSaveFileName(channelId, story.getName(), slot);

		File file = new File(saveDir.getAbsolutePath() + "/" + expectedSaveFileName);

		if (!file.exists()) {
			if (create) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			} else {
				return Optional.empty();
			}
		}

		SaveFileMetadata metadata = null;

		String expectedMetadataFileName = getMetadataFileName(channelId, story.getName(), slot);
		File metadataFile = new File(saveDir.getAbsolutePath() + "/" + expectedMetadataFileName);
		if (metadataFile.exists()) {
			try {
				metadata = objectMapper.readValue(metadataFile, SaveFileMetadata.class);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		SaveFile saveFile = SaveFile.builder().file(file).slot(slot).storyName(story.getName()).metadata(metadata)
				.build();

		return Optional.of(saveFile);
	}

	@Override
	public SaveFile saveNewMetadata(long channelId, SaveFile saveFile, SaveFileMetadata newSaveFileMetadata) {
		Preconditions.checkNotNull(saveFile, "saveFile must be non-null.");
		Preconditions.checkNotNull(newSaveFileMetadata, "newSaveFileMetadata must be non-null.");

		// Persist the new save file metadata
		String metadataFileName = getMetadataFileName(channelId, saveFile.getStoryName(), saveFile.getSlot());

		File metadataFile = new File(saveDir.getAbsolutePath() + "/" + metadataFileName);

		try {
			objectMapper.writeValue(metadataFile, newSaveFileMetadata);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return SaveFile.builder().file(saveFile.getFile()).slot(saveFile.getSlot()).storyName(saveFile.getStoryName())
				.metadata(newSaveFileMetadata).build();
	}

	private String getSaveFileName(long channelId, String storyName, int slot) {
		String fileName = String.format("%s_%s_slot%s.save", channelId, storyName, slot);
		return fileName;
	}

	private String getMetadataFileName(long channelId, String storyName, int slot) {
		String fileName = String.format("%s_%s_slot%s.meta", channelId, storyName, slot);
		return fileName;
	}

	@Override
	public void deleteSaveFile(long channelId, Story story, int slot) {
		Preconditions.checkNotNull(story, "story must be non-null.");
		Preconditions.checkArgument(slot >= 0, "slot must be non-negative.");
		Preconditions.checkArgument(slot < maxSlots, "slot must be less that the maximum number of slots.");

		String saveFileName = getSaveFileName(channelId, story.getName(), slot);
		String metadataFileName = getMetadataFileName(channelId, story.getName(), slot);

		File saveFile = new File(saveDir.getAbsolutePath() + "/" + saveFileName);
		File metadataFile = new File(saveDir.getAbsolutePath() + "/" + metadataFileName);

		saveFile.delete();
		metadataFile.delete();
	}
}
