package net.tonbot.plugin.ifplayer;

import javax.annotation.Nonnull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.tonbot.common.Param;

@EqualsAndHashCode()
@ToString()
public class IfPlayerDeleteSaveSlotRequest {

	@Param(name = "slot number", ordinal = 0, description = "The number of the slot to delete.")
	@Nonnull
	@Getter
	private int slotNumber;
}
