package net.tonbot.plugin.ifplayer;

import javax.annotation.Nonnull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.tonbot.common.Param;

@EqualsAndHashCode
@ToString()
class IfPlayerSetSaveSlotRequest {

	@Getter
	@Param(name = "slot number", ordinal = 0, description = "The slot number to set.")
	@Nonnull
	private int slot;
}
