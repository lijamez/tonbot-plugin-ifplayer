package net.tonberry.tonbot.plugin.ifplayer;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
class SessionKey {

    @NonNull
    private final String channelId;
}
