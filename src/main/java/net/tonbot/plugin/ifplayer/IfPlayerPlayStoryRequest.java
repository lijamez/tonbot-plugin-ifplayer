package net.tonbot.plugin.ifplayer;

import javax.annotation.Nonnull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.tonbot.common.Param;

@EqualsAndHashCode
@ToString()
class IfPlayerPlayStoryRequest {

	@Getter
	@Param(name = "story name", ordinal = 0, description = "The name of the story to play.", captureRemaining = true)
	@Nonnull
	private String storyName;
}
