package net.tonbot.plugin.ifplayer;

import java.util.List;
import java.util.Optional;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
class ScreenState {
	
	private final String statusLineObjectName;
	private final String statusLineScoreOrTime;
	
	@NonNull
	private final List<String> windowContents;
	
	public Optional<String> getStatusLineObjectName() {
		return Optional.ofNullable(statusLineObjectName);
	}
	
	public Optional<String> getStatusLineScoreOrTime() {
		return Optional.ofNullable(statusLineScoreOrTime);
	}
}
