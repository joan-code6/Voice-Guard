
package de.joancode;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.Deque;
import java.util.UUID;

public class AudioBuffer {
    private final Deque<AudioChunk> buffer = new ConcurrentLinkedDeque<>();
    private final int maxChunks;

    public AudioBuffer(int maxChunks) {
        this.maxChunks = maxChunks;
    }

    public void addChunk(AudioChunk chunk) {
        buffer.addLast(chunk);
        while (buffer.size() > maxChunks) {
            buffer.pollFirst();
        }
    }

    public AudioChunk[] getAllChunks() {
        return buffer.toArray(new AudioChunk[0]);
    }

    public void clear() {
        buffer.clear();
    }
}
