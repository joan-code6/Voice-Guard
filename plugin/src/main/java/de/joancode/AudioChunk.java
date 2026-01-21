
package de.joancode;

import java.util.UUID;

public class AudioChunk {
    private final long timestamp;
    private final byte[] data;
    private final UUID playerUuid;

    public AudioChunk(long timestamp, byte[] data, UUID playerUuid) {
        this.timestamp = timestamp;
        this.data = data;
        this.playerUuid = playerUuid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getData() {
        return data;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }
}
