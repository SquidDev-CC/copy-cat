package cc.tweaked.web.peripheral;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaTable;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.teavm.jso.webaudio.AudioContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

import static dan200.computercraft.api.lua.LuaValues.checkFinite;

public class SpeakerPeripheral implements TickablePeripheral {
    static AudioContext audioContext;

    public static final int SAMPLE_RATE = 48000;

    private IComputerAccess computer;
    private AudioState state;

    @Nonnull
    @Override
    public String getType() {
        return "speaker";
    }

    @Override
    public void attach(@Nonnull IComputerAccess computer) {
        this.computer = computer;
    }

    @Override
    public void detach(@Nonnull IComputerAccess computer) {
        this.computer = null;
    }

    @Override
    public void tick() {
        if (audioContext == null) state = null;
        if (state != null && state.shouldSendPending()) {
            state.playNext();
            computer.queueEvent("speaker_audio_empty", computer.getAttachmentName());
        }
    }

    @LuaFunction
    public final boolean playNote(String instrumentA, Optional<Double> volumeA, Optional<Double> pitchA) throws LuaException {
        throw new LuaException("Cannot play notes outside of Minecraft");
    }

    @LuaFunction
    public final boolean playSound(String name, Optional<Double> volumeA, Optional<Double> pitchA) throws LuaException {
        throw new LuaException("Cannot play sounds outside of Minecraft");
    }

    @LuaFunction(unsafe = true)
    public final boolean playAudio(LuaTable<?, ?> audio, Optional<Double> volume) throws LuaException {
        checkFinite(1, volume.orElse(0.0));

        int length = audio.length();
        if (length <= 0) throw new LuaException("Cannot play empty audio");
        if (length > 128 * 1024) throw new LuaException("Audio data is too large");

        if (audioContext == null) audioContext = AudioContext.create();
        if (state == null || !state.isPlaying()) state = new AudioState();

        return state.pushBuffer(audio, length, volume);
    }

    @LuaFunction
    public final void stop() {
        if (audioContext != null) {
            audioContext.close();
            audioContext = null;
        }
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof SpeakerPeripheral;
    }
}
