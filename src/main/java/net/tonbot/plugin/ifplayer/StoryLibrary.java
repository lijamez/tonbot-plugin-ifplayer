package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

class StoryLibrary {

	private final List<String> SUPPORTED_EXTENSIONS = ImmutableList.of("z1", "z2", "z3", "z4", "z5", "z7", "z8", "zblorb");
	
    private final File storyDir;

    @Inject
    public StoryLibrary(@StoryDir File storyDir) {
        this.storyDir = Preconditions.checkNotNull(storyDir, "storyDir must be non-null.");
    }

    /**
     * Lists the names of supported stories in the story directory. 
     * @return A list of story files. Does not include directories.
     */
    public List<File> listAllStories() {
		List<File> allFiles = Arrays.asList(storyDir.listFiles());
    	
		return allFiles.stream()
			.filter(file -> !file.isDirectory())
		    .filter(file -> {
		    		for (String supportedExtension : SUPPORTED_EXTENSIONS) {
		    			if (file.getName().endsWith("." + supportedExtension)) {
		    				return true;
		    			}
		    		}
		    		return false;
		    })
		    .collect(Collectors.toList());
    }

    /**
     * Gets the best match given the story name on a best effort basis.
     * @param storyName The story name. Non-null.
     * @return The search results.
     */
    public List<File> findStories(String storyName) {
    		List<File> allStories = listAllStories();
    	
    		File exactMatch = allStories.stream()
    		    .filter(file -> StringUtils.equals(file.getName(), storyName))
    		    .findAny()
    		    .orElse(null);
    		
    		if (exactMatch != null) {
    			return ImmutableList.of(exactMatch);
    		}
    		
        List<File> prefixMatchedFiles = allStories.stream()
        		.filter(file -> StringUtils.startsWithIgnoreCase(file.getName(), storyName))
        		.collect(Collectors.toList());
        
        return prefixMatchedFiles;
    }
}
