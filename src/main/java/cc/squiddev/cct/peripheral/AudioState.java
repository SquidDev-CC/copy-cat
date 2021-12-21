package cc.squiddev.cct.peripheral;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTable;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.webaudio.AudioBuffer;
import org.teavm.jso.webaudio.AudioBufferSourceNode;

import javax.annotation.Nonnull;
import java.util.Optional;

import static cc.squiddev.cct.peripheral.SpeakerPeripheral.SAMPLE_RATE;
import static cc.squiddev.cct.peripheral.SpeakerPeripheral.audioContext;

class AudioState {
    /**
     * The minimum size of the client's audio buffer. Once we have less than this on the client, we should send another
     * batch of audio.
     */
    private static final double CLIENT_BUFFER = 0.5;

    private AudioBuffer nextBuffer;
    private double nextTime = audioContext.getCurrentTime();

    boolean pushBuffer(LuaTable<?, ?> table, int size, @Nonnull Optional<Double> volume) throws LuaException {
        if (nextBuffer != null) return false;

        AudioBuffer buffer = nextBuffer = audioContext.createBuffer(1, size, SAMPLE_RATE);
        Float32Array contents = buffer.getChannelData(0);

        for (int i = 0; i < size; i++) contents.set(i, table.getInt(i + 1) / 128.0f);

        // So we really should go via DFPWM here, but I do not have enough faith in our performance to do this properly.

        if (shouldSendPending()) playNext();
        return true;
    }

    boolean isPlaying() {
        return audioContext != null && nextTime >= audioContext.getCurrentTime();
    }

    boolean shouldSendPending() {
        return nextBuffer != null && audioContext.getCurrentTime() >= nextTime - CLIENT_BUFFER;
    }

    void playNext() {
        AudioBufferSourceNode source = audioContext.createBufferSource();
        source.setBuffer(nextBuffer);
        source.connect(audioContext.getDestination());
        source.start(nextTime);

        nextTime += nextBuffer.getDuration();
        nextBuffer = null;
    }
}
