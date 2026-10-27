package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.sound.CustomSoundFolders;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

public class CustomCreatureAudioTest {
    @Test public void distanceGainMatchesTheExactLinearTableAndFractionalDistances() {
        double[] distances = {1, 2, 3, 5, 10, 15, 20, 21, 22};
        float[] expected = {1, .95F, .90F, .80F, .55F, .30F, .05F, 0, 0};
        for (int i = 0; i < distances.length; i++)
            assertEquals("gain at " + distances[i] + " blocks", expected[i],
                    CustomCreatureAudio.spatialAttenuationForDistance(distances[i]), .0001F);
        assertEquals(.925F, CustomCreatureAudio.spatialAttenuationForDistance(2.5), .0001F);
        assertEquals(1.0F, CustomCreatureAudio.spatialAttenuationForDistance(0), 0.0001F);
        assertEquals(0.0F, CustomCreatureAudio.spatialAttenuationForDistance(Double.NaN), 0.0F);
        assertTrue("different listeners receive different local gains",
                CustomCreatureAudio.spatialAttenuationForDistance(5)
                        > CustomCreatureAudio.spatialAttenuationForDistance(10));
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

    @Test public void stereoIsDownmixedOnlyInMemoryForDirectionalSpatialPlayback() {
        var stereo = org.lwjgl.system.MemoryUtil.memAlloc(8).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        stereo.asShortBuffer().put(new short[]{1200, 800, -2000, 1000});
        stereo.limit(8).position(0);
        var mono = CustomCreatureAudio.toMonoForSpatialPlayback(new CustomCreatureAudio.Pcm(stereo, 22050, 2));
        try {
            assertEquals(1, mono.channels());
            assertEquals(22050, mono.sampleRate());
            assertEquals(4, mono.bytes().remaining());
            var samples = mono.bytes().order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            assertEquals(1000, samples.get(0));
            assertEquals(-500, samples.get(1));
        } finally {
            org.lwjgl.system.MemoryUtil.memFree(mono.bytes());
        }
    }

    @Test public void listsAllDirectOggAndMp3FilesAndIgnoresNestedOrUnsupportedFiles() throws Exception {
        Path root = Files.createTempDirectory("minemon-candidate-test-");
        Path directory = Files.createDirectory(root.resolve("species"));
        Path nested = Files.createDirectory(directory.resolve("nested"));
        Path mp3 = directory.resolve("som.mp3"), ogg1 = directory.resolve("z-cry.ogg");
        Path ogg2 = directory.resolve("a-cry.ogg"), note = directory.resolve("readme.txt");
        try {
            assertTrue(CustomSoundFolders.listAudioCandidates(root, directory).isEmpty());
            Files.write(mp3, new byte[]{1}); Files.write(ogg1, new byte[]{1});
            Files.write(ogg2, new byte[]{1}); Files.write(note, new byte[]{1});
            Files.write(nested.resolve("hidden.ogg"), new byte[]{1});
            var candidates = CustomSoundFolders.listAudioCandidates(root, directory);
            assertEquals(3, candidates.size());
            assertTrue(candidates.containsAll(java.util.List.of(mp3, ogg1, ogg2)));
            assertTrue(CustomSoundFolders.randomizedCandidateOrder(candidates, new Random(42)).containsAll(candidates));
        } finally {
            Files.deleteIfExists(mp3); Files.deleteIfExists(ogg1); Files.deleteIfExists(ogg2); Files.deleteIfExists(note);
            Files.deleteIfExists(nested.resolve("hidden.ogg")); Files.deleteIfExists(nested);
            Files.deleteIfExists(directory); Files.deleteIfExists(root);
        }
    }

    @Test public void randomSelectionCoversMixedValidFilesAndSkipsCorruptCandidates() throws Exception {
        Path root = Files.createTempDirectory("minemon-random-audio-test-");
        Path directory = Files.createDirectory(root.resolve("species"));
        Path invalid = directory.resolve("00-corrupt.ogg");
        Path oggA = directory.resolve("01-a.ogg"), oggB = directory.resolve("02-b.ogg");
        Path mp3 = directory.resolve("03-c.mp3");
        try {
            try (var resource = getClass().getResourceAsStream("/audio/synthetic.ogg")) {
                assertNotNull(resource); Files.copy(resource, oggA);
            }
            Files.copy(oggA, oggB);
            try (var resource = getClass().getResourceAsStream("/audio/synthetic.mp3")) {
                assertNotNull(resource); Files.copy(resource, mp3);
            }
            Files.write(invalid, new byte[]{'O','g','g','S',0,0,0,0,1,2,3,4,5,6,7,8});
            var candidates = CustomSoundFolders.listAudioCandidates(root, directory);
            assertEquals(4, candidates.size());
            Map<Path, Integer> counts = new HashMap<>();
            Random random = new Random(7654);
            for (int i = 0; i < 120; i++) {
                var selected = CustomCreatureAudio.decodeRandomValid(candidates, root, random);
                assertNotNull("invalid candidate must fall through to a valid file", selected);
                assertNotEquals(invalid, selected.file());
                counts.merge(selected.file(), 1, Integer::sum);
                org.lwjgl.system.MemoryUtil.memFree(selected.pcm().bytes());
            }
            assertEquals(java.util.Set.of(oggA, oggB, mp3), counts.keySet());
            for (int count : counts.values()) assertTrue("random selection should reach each valid format/file", count > 10);
        } finally {
            Files.deleteIfExists(invalid); Files.deleteIfExists(oggA); Files.deleteIfExists(oggB); Files.deleteIfExists(mp3);
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
