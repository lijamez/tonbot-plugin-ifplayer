package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Optional;

import com.google.common.base.Preconditions;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
class SaveFile {

	private final File file;
	private final String storyName;
	private final int slot;
	private final SaveFileMetadata metadata;

	public SaveFile(File file, String storyName, int slot, SaveFileMetadata metadata) {
		this.metadata = metadata;

		Preconditions.checkArgument(slot >= 0, "slot must be non-negative.");
		this.slot = slot;

		Preconditions.checkNotNull(file, "file must be non-null.");
		Preconditions.checkArgument(file.exists(), "file must exist.");
		this.file = file;

		this.storyName = Preconditions.checkNotNull(storyName, "storyName must be non-null.");
	}

	public Optional<SaveFileMetadata> getMetadata() {
		return Optional.ofNullable(metadata);
	}

	public OutputStream getOutputStream() {
		try {
			return new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			throw new UncheckedIOException(e);
		}
	}

	public InputStream getInputStream() {
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new UncheckedIOException(e);
		}
	}

	public URI getURI() {
		return file.toURI();
	}
}
