package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.sound.CustomSoundFolders;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.*;

public class CustomCreatureAudioTest {
    @Test public void positionalAttenuationFadesSmoothlyForEachListener() {
        assertEquals(1.0F, CustomCreatureAudio.spatialAttenuationForDistance(0), 0.0001F);
        assertEquals(1.0F, CustomCreatureAudio.spatialAttenuationForDistance(4), 0.0001F);
        float nearby = CustomCreatureAudio.spatialAttenuationForDistance(8);
        float intermediate = CustomCreatureAudio.spatialAttenuationForDistance(16);
        float distant = CustomCreatureAudio.spatialAttenuationForDistance(28);
        assertTrue(nearby < 1.0F && nearby > intermediate);
        assertTrue(intermediate > distant && distant > 0.0F);
        assertEquals(0.0F, CustomCreatureAudio.spatialAttenuationForDistance(32), 0.0F);
        assertEquals(0.0F, CustomCreatureAudio.spatialAttenuationForDistance(48), 0.0F);
        assertTrue("near client hears more than far client",
                CustomCreatureAudio.spatialAttenuationForDistance(5)
                        > CustomCreatureAudio.spatialAttenuationForDistance(24));
    }

    @Test public void decodesSyntheticOggAndMp3WithinLimits() throws Exception {
        Path testRoot = Files.createTempDirectory("minemon-sound-test-");
        Path testFolder = Files.createDirectory(testRoot.resolve("species"));
        try {
            for (String ext : new String[]{"ogg", "mp3"}) {
                Path target = testFolder.resolve("synthetic." + ext);
                try (var resource = getClass().getResourceAsStream("/audio/synthetic." + ext)) {
                    assertNotNull("missing synthetic " + ext + " fixture", resource);
                    Files.copy(resource, target, StandardCopyOption.REPLACE_EXISTING);
                }
                var pcm = CustomCreatureAudio.decode(target, testRoot);
                assertTrue(pcm.bytes().remaining() > 0);
                assertTrue(pcm.sampleRate() >= 8000 && pcm.sampleRate() <= 48000);
                assertEquals(1, pcm.channels());
                org.lwjgl.system.MemoryUtil.memFree(pcm.bytes());
            }
        } finally {
            try (var paths = Files.list(testFolder)) { paths.forEach(path -> { try { Files.deleteIfExists(path); } catch (Exception ignored) { } }); }
            Files.deleteIfExists(testFolder); Files.deleteIfExists(testRoot);
        }
    }

    @Test public void oggHasFixedPriorityAndEmptyFolderHasNoAudio() throws Exception {
        Path root = Files.createTempDirectory("minemon-priority-test-");
        Path directory = Files.createDirectory(root.resolve("species"));
        Path mp3 = directory.resolve("som.mp3"), ogg = directory.resolve("z-cry.ogg"), note = directory.resolve("readme.txt");
        try {
            assertNull(CustomSoundFolders.selectAudio(root, directory));
            Files.write(mp3, new byte[]{1}); Files.write(ogg, new byte[]{1}); Files.write(note, new byte[]{1});
            assertEquals(ogg, CustomSoundFolders.selectAudio(root, directory));
            Files.delete(ogg);
            assertEquals(mp3, CustomSoundFolders.selectAudio(root, directory));
        } finally {
            Files.deleteIfExists(mp3); Files.deleteIfExists(ogg); Files.deleteIfExists(note);
            Files.deleteIfExists(directory); Files.deleteIfExists(root);
        }
    }

    @Test public void rejectsCorruptAndOversizedAudio() throws Exception {
        Path testRoot = Files.createTempDirectory("minemon-validation-test-");
        Path testFolder = Files.createDirectory(testRoot.resolve("species"));
        Path corrupt = testFolder.resolve("corrupt.ogg");
        Path oversized = testFolder.resolve("oversized.mp3");
        try {
            Files.write(corrupt, new byte[]{'O','g','g','S',0,0,0,0,1,2,3,4,5,6,7,8});
            try { CustomCreatureAudio.decode(corrupt, testRoot); fail("corrupt OGG must be rejected"); }
            catch (Exception expected) { }
            try (var file = java.nio.channels.FileChannel.open(oversized, java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE)) {
                file.position(8L * 1024 * 1024);
                file.write(java.nio.ByteBuffer.wrap(new byte[]{0}));
            }
            try { CustomCreatureAudio.decode(oversized, testRoot); fail("oversized audio must be rejected"); }
            catch (Exception expected) { }
        } finally {
            Files.deleteIfExists(corrupt); Files.deleteIfExists(oversized); Files.deleteIfExists(testFolder); Files.deleteIfExists(testRoot);
        }
    }
}
