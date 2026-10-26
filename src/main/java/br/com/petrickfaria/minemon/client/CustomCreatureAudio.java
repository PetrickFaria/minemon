package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.sound.CustomSoundFolders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryUtil;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Local-only MP3/OGG playback through Minecraft's OpenAL context. */
@Mod.EventBusSubscriber(modid = Minemon.MOD_ID, value = Dist.CLIENT)
public final class CustomCreatureAudio {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long MAX_FILE_BYTES = 8L * 1024 * 1024;
    private static final int MAX_SECONDS = 30;
    private static final int MAX_ACTIVE = 4;
    private static final Executor DECODER = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(16), runnable -> {
        Thread thread = new Thread(runnable, "Minemon-audio-decoder");
        thread.setDaemon(true);
        return thread;
    }, new ThreadPoolExecutor.AbortPolicy());
    private static final AtomicInteger RESERVED = new AtomicInteger();
    private static final List<Playback> ACTIVE = new ArrayList<>();
    private static final java.util.concurrent.ConcurrentHashMap<UUID, java.util.concurrent.atomic.AtomicBoolean> REQUESTED = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile Executor audioExecutor;
    private static volatile UUID lastStartedEntity;

    public static void play(int entityId, UUID uuid) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isDaytime(minecraft)) return;
        var entity = minecraft.level.getEntity(entityId);
        if (!(entity instanceof MinemonCreature creature) || !creature.getUUID().equals(uuid) || !creature.isAlive()) return;
        var cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
        if (REQUESTED.putIfAbsent(uuid, cancelled) != null) return;
        if (RESERVED.incrementAndGet() > MAX_ACTIVE) {
            RESERVED.decrementAndGet();
            REQUESTED.remove(uuid, cancelled);
            return;
        }
        Path file;
        try {
            file = CustomSoundFolders.findAudio(creature.getSpeciesSlug());
            if (file == null) { releaseRequest(uuid, cancelled); return; }
        } catch (Exception exception) {
            releaseRequest(uuid, cancelled);
            LOGGER.warn("Could not inspect custom audio for {}", creature.getSpeciesSlug(), exception);
            return;
        }
        try { DECODER.execute(() -> {
            try {
                Pcm pcm = decode(file);
                minecraft.execute(() -> {
                    var current = minecraft.level == null ? null : minecraft.level.getEntity(entityId);
                    if (!(current instanceof MinemonCreature live) || !live.getUUID().equals(uuid) || !live.isAlive()
                            || cancelled.get() || !isDaytime(minecraft)) {
                        MemoryUtil.memFree(pcm.bytes);
                        releaseRequest(uuid, cancelled);
                        return;
                    }
                    Playback playback = new Playback(entityId, uuid, cancelled, pcm, live.getX(), live.getY() + live.getBbHeight() * 0.5, live.getZ());
                    ACTIVE.add(playback);
                    if (!executeAudio(() -> start(playback))) {
                        MemoryUtil.memFree(pcm.bytes);
                        playback.finished = true;
                    }
                });
            } catch (Exception exception) {
                releaseRequest(uuid, cancelled);
                LOGGER.warn("Rejected custom audio file {}: {}", file, exception.getMessage());
            }
        }); } catch (java.util.concurrent.RejectedExecutionException full) {
            releaseRequest(uuid, cancelled);
        }
    }

    public static boolean wasStarted(UUID entity) { return entity != null && entity.equals(lastStartedEntity); }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean daytime = isDaytime(minecraft);
        if (!daytime) {
            REQUESTED.forEach((uuid, cancelled) -> {
                cancelled.set(true);
                REQUESTED.remove(uuid, cancelled);
            });
        }
        if (ACTIVE.isEmpty()) return;
        for (Playback playback : List.copyOf(ACTIVE)) {
            var entity = minecraft.level == null ? null : minecraft.level.getEntity(playback.entityId);
            if (!daytime || !(entity instanceof MinemonCreature creature) || !creature.getUUID().equals(playback.uuid) || !creature.isAlive()) {
                playback.cancelled.set(true);
                if (!playback.stopping) {
                    playback.stopping = true;
                    if (!executeAudio(() -> stop(playback))) playback.finished = true;
                }
            } else {
                double x = creature.getX(), y = creature.getY() + creature.getBbHeight() * 0.5, z = creature.getZ();
                float gain = minecraft.options.getSoundSourceVolume(SoundSource.MASTER)
                        * minecraft.options.getSoundSourceVolume(SoundSource.NEUTRAL);
                if (!executeAudio(() -> update(playback, x, y, z, gain))) playback.finished = true;
            }
            if (playback.finished) {
                ACTIVE.remove(playback);
                RESERVED.decrementAndGet();
                REQUESTED.remove(playback.uuid, playback.cancelled);
            }
        }
    }

    private static void releaseRequest(UUID uuid, java.util.concurrent.atomic.AtomicBoolean cancelled) {
        REQUESTED.remove(uuid, cancelled);
        RESERVED.decrementAndGet();
    }

    private static boolean isDaytime(Minecraft minecraft) {
        return minecraft.level != null
                && (!minecraft.level.dimensionType().hasSkyLight() || !minecraft.level.isNight());
    }

    static Pcm decode(Path file) throws Exception {
        return decode(file, CustomSoundFolders.root());
    }

    static Pcm decode(Path file, Path allowedRoot) throws Exception {
        if (!file.normalize().startsWith(allowedRoot.normalize())
                || Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("not a regular file in the Minemon sounds folder");
        }
        long length = Files.size(file);
        if (length < 16 || length > MAX_FILE_BYTES) throw new IllegalArgumentException("file size is outside 16 bytes–8 MiB");
        String lower = file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (!lower.endsWith(".ogg") && !lower.endsWith(".mp3")) throw new IllegalArgumentException("only .ogg and .mp3 are supported");
        return lower.endsWith(".ogg") ? decodeOgg(file, Math.toIntExact(length)) : decodeMp3(file);
    }

    private static Pcm decodeOgg(Path file, int length) throws Exception {
        ByteBuffer compressed = MemoryUtil.memAlloc(length);
        try {
            try (InputStream input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
                byte[] chunk = new byte[8192];
                int read;
                while ((read = input.read(chunk)) >= 0) {
                    if (compressed.remaining() < read) throw new IllegalArgumentException("audio changed while being read");
                    compressed.put(chunk, 0, read);
                }
            }
            compressed.flip();
            IntBuffer error = MemoryUtil.memAllocInt(1);
            long decoder = 0L;
            STBVorbisInfo info = null;
            ByteBuffer pcm = null;
            try {
                decoder = STBVorbis.stb_vorbis_open_memory(compressed, error, null);
                if (decoder == 0L) throw new IllegalArgumentException("invalid or unsupported OGG stream (decoder error " + error.get(0) + ")");
                info = STBVorbisInfo.malloc();
                STBVorbis.stb_vorbis_get_info(decoder, info);
                int channelCount = info.channels(), sampleRate = info.sample_rate();
                if (channelCount < 1 || channelCount > 2 || sampleRate < 8000 || sampleRate > 48000)
                    throw new IllegalArgumentException("OGG channel count or sample rate is unsupported");
                int sampleFrames = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
                if (sampleFrames <= 0 || sampleFrames > sampleRate * MAX_SECONDS)
                    throw new IllegalArgumentException("OGG duration is unknown or exceeds 30 seconds");
                int bytes = Math.multiplyExact(Math.multiplyExact(sampleFrames, channelCount), 2);
                validatePcm(bytes, sampleRate, channelCount);
                pcm = MemoryUtil.memAlloc(bytes).order(ByteOrder.LITTLE_ENDIAN);
                ShortBuffer samples = pcm.asShortBuffer();
                int totalFrames = 0;
                while (samples.hasRemaining()) {
                    int frames = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channelCount, samples);
                    if (frames == 0) break;
                    totalFrames += frames;
                    samples.position(samples.position() + frames * channelCount);
                }
                if (totalFrames != sampleFrames) throw new IllegalArgumentException("truncated OGG stream");
                pcm.limit(bytes).position(0);
                Pcm result = new Pcm(pcm, sampleRate, channelCount);
                pcm = null;
                return result;
            } finally {
                if (pcm != null) MemoryUtil.memFree(pcm);
                if (info != null) info.free();
                if (decoder != 0L) STBVorbis.stb_vorbis_close(decoder);
                MemoryUtil.memFree(error);
            }
        } finally { MemoryUtil.memFree(compressed); }
    }

    private static Pcm decodeMp3(Path file) throws Exception {
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        int sampleRate = -1, channels = -1;
        try (InputStream input = new FilterInputStream(Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            private long consumed;
            private int account(int count) throws IOException {
                if (count > 0) {
                    consumed += count;
                    if (consumed > MAX_FILE_BYTES) throw new IOException("MP3 grew beyond the 8 MiB limit while reading");
                }
                return count;
            }
            @Override public int read() throws IOException {
                int value = super.read();
                if (value >= 0) account(1);
                return value;
            }
            @Override public int read(byte[] bytes, int offset, int length) throws IOException { return account(super.read(bytes, offset, length)); }
        }) {
            Bitstream stream = new Bitstream(input);
            try {
            Decoder decoder = new Decoder();
            Header header;
            while ((header = stream.readFrame()) != null) {
                SampleBuffer frame = (SampleBuffer) decoder.decodeFrame(header, stream);
                if (sampleRate < 0) { sampleRate = frame.getSampleFrequency(); channels = frame.getChannelCount(); }
                if (frame.getSampleFrequency() != sampleRate || frame.getChannelCount() != channels)
                    throw new IllegalArgumentException("MP3 changes channel count or sample rate");
                int bytes = frame.getBufferLength() * 2;
                validatePcm(pcm.size() + bytes, sampleRate, channels);
                short[] data = frame.getBuffer();
                for (int i = 0; i < frame.getBufferLength(); i++) {
                    pcm.write(data[i] & 0xff);
                    pcm.write((data[i] >>> 8) & 0xff);
                }
                stream.closeFrame();
            }
            } finally { stream.close(); }
        }
        if (sampleRate < 8000 || sampleRate > 48000 || channels < 1 || channels > 2 || pcm.size() == 0)
            throw new IllegalArgumentException("empty or unsupported MP3 stream");
        ByteBuffer data = MemoryUtil.memAlloc(pcm.size()).order(ByteOrder.LITTLE_ENDIAN);
        data.put(pcm.toByteArray()).flip();
        return new Pcm(data, sampleRate, channels);
    }

    private static void validatePcm(int bytes, int rate, int channels) {
        long max = (long) rate * channels * 2 * MAX_SECONDS;
        if (bytes <= 0 || bytes > max || bytes > 12L * 1024 * 1024)
            throw new IllegalArgumentException("decoded audio exceeds 30 seconds or 12 MiB");
    }

    private static void start(Playback playback) {
        try {
            if (playback.cancelled.get()) { playback.finished = true; return; }
            int buffer = AL10.alGenBuffers();
            int source = AL10.alGenSources();
            playback.buffer = buffer; playback.source = source;
            AL10.alBufferData(buffer, playback.pcm.channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16,
                    playback.pcm.bytes, playback.pcm.sampleRate);
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
            AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
            AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 4.0f);
            AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, 32.0f);
            AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 1.0f);
            AL10.alSourcef(source, AL10.AL_GAIN, 1.0f);
            AL10.alSource3f(source, AL10.AL_POSITION, (float)playback.x, (float)playback.y, (float)playback.z);
            AL10.alSourcePlay(source);
            lastStartedEntity = playback.uuid;
        } catch (Throwable error) {
            LOGGER.warn("Could not start Minemon positional audio", error);
            try { stop(playback); } catch (Throwable cleanupError) { LOGGER.debug("Could not clean up failed OpenAL playback", cleanupError); }
            playback.finished = true;
        } finally { MemoryUtil.memFree(playback.pcm.bytes); }
    }

    private static void update(Playback playback, double x, double y, double z, float gain) {
        if (playback.source == 0 || playback.finished) return;
        if (playback.stopping || AL10.alGetSourcei(playback.source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
            stop(playback); return;
        }
        AL10.alSource3f(playback.source, AL10.AL_POSITION, (float)x, (float)y, (float)z);
        AL10.alSourcef(playback.source, AL10.AL_GAIN, Math.max(0, Math.min(1, gain)));
    }

    private static void stop(Playback playback) {
        if (playback.source != 0) { AL10.alSourceStop(playback.source); AL10.alDeleteSources(playback.source); playback.source = 0; }
        if (playback.buffer != 0) { AL10.alDeleteBuffers(playback.buffer); playback.buffer = 0; }
        playback.finished = true;
    }

    private static boolean executeAudio(Runnable task) {
        try { executor().execute(task); return true; }
        catch (Exception exception) { LOGGER.warn("Minecraft audio executor is unavailable", exception); return false; }
    }

    private static Executor executor() throws ReflectiveOperationException {
        Executor cached = audioExecutor;
        if (cached != null) return cached;
        Object soundManager = Minecraft.getInstance().getSoundManager();
        Object soundEngine = null;
        for (Field field : soundManager.getClass().getDeclaredFields()) {
            if (field.getType() == SoundEngine.class) {
                field.setAccessible(true); soundEngine = field.get(soundManager); break;
            }
        }
        if (soundEngine == null) throw new NoSuchFieldException("SoundEngine field in SoundManager");
        for (Field field : soundEngine.getClass().getDeclaredFields()) {
            if (!Executor.class.isAssignableFrom(field.getType())) continue;
            field.setAccessible(true);
            Object candidate = field.get(soundEngine);
            if (candidate instanceof Executor executor) return audioExecutor = executor;
        }
        throw new NoSuchFieldException("SoundEngineExecutor field");
    }

    record Pcm(ByteBuffer bytes, int sampleRate, int channels) {}
    private static final class Playback {
        final int entityId; final UUID uuid; final java.util.concurrent.atomic.AtomicBoolean cancelled; final Pcm pcm; final double x, y, z;
        volatile int source, buffer;
        volatile boolean finished, stopping;
        Playback(int entityId, UUID uuid, java.util.concurrent.atomic.AtomicBoolean cancelled, Pcm pcm, double x, double y, double z) { this.entityId = entityId; this.uuid = uuid; this.cancelled = cancelled; this.pcm = pcm; this.x=x; this.y=y; this.z=z; }
    }

    private CustomCreatureAudio() {}
}
