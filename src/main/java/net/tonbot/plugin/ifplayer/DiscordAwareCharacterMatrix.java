package net.tonbot.plugin.ifplayer;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * Discord has a maximum character limit per message, so this specialization of {@link CharacterMatrix} will remove any unnecessary whitespace.
 * This class will make the following optimizations:
 * <ul>
 *     <li>Trim all whitespace from the end of each line.</li>
 *     <li>Removes trailing empty lines.</li>
 *     <li>Replace 4 spaces with a tab. When code blocks are used, Discord displays 1 tab as 4 spaces, but only treats a tab as 1 character.
 * </ul>
 */
class DiscordAwareCharacterMatrix extends CharacterMatrix {
	
	public DiscordAwareCharacterMatrix() {
		super();
	}
	
	public DiscordAwareCharacterMatrix(int maxWidth, int maxHeight) {
		super(maxWidth, maxHeight);
	}
	
	@Override
	public String render() {
		List<String> listOfLines = matrix.stream()
		    .map(line -> {
				StringBuffer lineSb = new StringBuffer();
				line.forEach(character -> lineSb.append(character));
				return lineSb.toString()
						.replaceAll("\\s+$", "")
						.replaceAll("\\s{4}", "\t");
		    })
		    .collect(Collectors.toList());
		
		// Removes trailing empty lines
		String result = StringUtils.join(listOfLines, "\n")
				.replaceAll("\\s+$", "");
		
		return result;
	}

}
