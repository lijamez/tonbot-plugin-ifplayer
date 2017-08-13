package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zmpp.base.DefaultMemory0;
import org.zmpp.base.Memory;
import org.zmpp.zcode.Machine;

import com.google.common.base.Preconditions;
import com.tonberry.tonbot.common.TonbotTechnicalFault;

import lombok.Getter;

/**
 * A Z-code story.
 */
class Story {

    private static final Logger LOG = LoggerFactory.getLogger(Story.class);

    private static final byte MIN_VERSION = 1;
    private static final byte MAX_VERISON = 8;

    @Getter
    private final String name;
    private final byte[] data;

    private Story(String name, byte[] data) {
        this.name = Preconditions.checkNotNull(name, "name must be non-null.");
        this.data = Preconditions.checkNotNull(data, "data must be non-null.");

        Preconditions.checkArgument(data.length > 0, "data must not be empty.");
    }

    /**
     * Loads a story from a file.
     * @param file The {@link File} to be loaded. Non-null.
     * @return A {@link Story}
     * @throws TonbotTechnicalFault if the file could not be loaded.
     * @throws IllegalArgumentException if the file is not deemed to be a story file.
     */
    public static Story loadFrom(File file) {
        Preconditions.checkNotNull(file, "file must be non-null.");
        Preconditions.checkArgument(!file.isDirectory(), "file must be a file, not a directory.");

        byte[] fileBytes = new byte[(int) file.length()];
        try (FileInputStream is = new FileInputStream(file)) {
            int numBytesRead = is.read(fileBytes);

            if (numBytesRead != file.length()) {
                throw new TonbotTechnicalFault("Couldn't fully read the file.");
            }
        } catch (IOException e) {
            throw new TonbotTechnicalFault("Couldn't read file.", e);
        }

        LOG.debug("Successfully read {} bytes of story file.", fileBytes.length);

        Story story = new Story(file.getName(), fileBytes);

        Preconditions.checkArgument(story.getVersion() >= MIN_VERSION && story.getVersion() <= MAX_VERISON,
                "File is not a story.");

        return story;
    }

    /**
     * Gets the version of the Z-machine that this story is intended to be run on.
     * @return The z-code machine version.
     */
    public byte getVersion() {
        // The first byte is the version.
        // http://inform-fiction.org/zmachine/standards/z1point1/sect11.html
        return data[0];
    }

    /**
     * Gets the story as {@link Memory} for the {@link Machine}.
     * @return {@link Memory}. Non-null.
     */
    public Memory getMemory() {
        Memory mem = new DefaultMemory0(data);
        return mem;
    }
}
