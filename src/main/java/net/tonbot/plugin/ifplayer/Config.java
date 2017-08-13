package net.tonbot.plugin.ifplayer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

import lombok.Data;

@Data
class Config {

    private final String storiesDir;
    private final String savesDir;

    @JsonCreator
    public Config(
            @JsonProperty("storiesDir") String storiesDir,
            @JsonProperty("savesDir") String savesDir) {
        this.storiesDir = Preconditions.checkNotNull(storiesDir, "storiesDir must be non-null.");
        this.savesDir = Preconditions.checkNotNull(savesDir, "savesDir must be non-null.");
    }
}
