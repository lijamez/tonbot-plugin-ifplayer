package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zmpp.base.DefaultMemory0;
import org.zmpp.base.Memory;
import org.zmpp.iff.BlorbData;
import org.zmpp.iff.DefaultFormChunk;

import com.google.common.base.Preconditions;

import lombok.Data;
import net.tonbot.common.TonbotTechnicalFault;

/**
 * A Z-code story.
 */
@Data
class Story {

    private static final Logger LOG = LoggerFactory.getLogger(Story.class);

    private static final byte MIN_VERSION = 1;
    private static final byte MAX_VERISON = 8;

    private final String name;
    private final Memory memory;

    private Story(String name, Memory memory) {
        this.name = Preconditions.checkNotNull(name, "name must be non-null.");
        this.memory = Preconditions.checkNotNull(memory, "memory must be non-null.");
    }
    
    /**
     * Gets the Z-Machine version.
     * @return The Z-Machine version.
     */
    public int getVersion() {
    		return memory.byteAt(0);
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

        byte version = fileBytes[0];
        Story story = null;
        
        if (version >= MIN_VERSION && version <= MAX_VERISON) {
        		// It's a regular Z-code file.
        		story = new Story(file.getName(), new DefaultMemory0(fileBytes));
        }
        
        if (BlorbData.isBlorbFile(fileBytes)) {
        		// It's a blorb file.
        		BlorbData blorbData = new BlorbData(new DefaultFormChunk(new DefaultMemory0(fileBytes)));
        		if (blorbData.hasZcodeChunk()) {
        			story = new Story(file.getName(), blorbData.zcodeData());
        		}
        }

        if (story == null) {
            throw new IllegalArgumentException("File is not supported.");
        }

        return story;
    }
}
