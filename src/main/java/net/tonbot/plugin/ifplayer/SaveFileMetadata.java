package net.tonbot.plugin.ifplayer;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
class SaveFileMetadata {

	@JsonProperty("createdBy")
	private final String createdBy;

	@JsonProperty("creationDate")
	private final ZonedDateTime creationDate;

	public SaveFileMetadata(@JsonProperty("createdBy") String createdBy,
			@JsonProperty("creationDate") ZonedDateTime creationDate) {
		this.createdBy = Preconditions.checkNotNull(createdBy, "createdBy must be non-null.");
		this.creationDate = Preconditions.checkNotNull(creationDate, "creationDate must be non-null.");
	}
}
