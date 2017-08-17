package net.tonbot.plugin.ifplayer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
class SessionKey {

    private final long channelId;
}
