package net.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.tonberry.tonbot.common.TonbotBusinessException;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class StoryLibrary {

    private final File storyDir;

    @Inject
    public StoryLibrary(@StoryDir File storyDir) {
        this.storyDir = Preconditions.checkNotNull(storyDir, "storyDir must be non-null.");
    }

    /**
     * Lists the names of files (not directories) in the story directory.
     * @return A list of story file names.
     */
    public List<String> listStories() {
        File[] files = storyDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });

        return Arrays.asList(files).stream()
                .map(file -> file.getName())
                .collect(Collectors.toList());
    }

    /**
     * Gets the best match given the story name.
     * @param storyName The story name. Non-null.
     * @return The {@link File} of the story.
     * @throws TonbotBusinessException if multiple files were found.
     */
    public Optional<File> getBestMatch(String storyName) {
        File[] foundFiles = storyDir.listFiles((dir, name) -> name.startsWith(storyName));

        if (foundFiles.length == 0) {
            return Optional.empty();
        } else if (foundFiles.length > 1){
            throw new TonbotBusinessException("You need to be a little more specific.");
        }

        File bestMatchStory = foundFiles[0];

        return Optional.of(bestMatchStory);
    }
}
