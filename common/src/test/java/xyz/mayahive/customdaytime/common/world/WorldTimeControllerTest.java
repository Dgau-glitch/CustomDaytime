/*
 *     Copyright (c) 2026 Seedim
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.mayahive.customdaytime.common.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.mayahive.customdaytime.api.model.PlatformType;
import xyz.mayahive.customdaytime.api.model.WorldKey;
import xyz.mayahive.customdaytime.api.platform.Platform;
import xyz.mayahive.customdaytime.api.platform.PlatformLogger;
import xyz.mayahive.customdaytime.api.platform.PlatformScheduler;
import xyz.mayahive.customdaytime.api.platform.PlatformTask;
import xyz.mayahive.customdaytime.api.platform.PlatformTaskScheduler;
import xyz.mayahive.customdaytime.api.platform.PlatformWorld;
import xyz.mayahive.customdaytime.common.context.CustomDaytimeContext;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldTimeControllerTest {

    private static final WorldKey WORLD_KEY = new WorldKey("minecraft", "overworld");

    @TempDir
    Path tempDir;

    @Test
    void advancesDayByConfiguredIncrementOnGlobalTick() {
        TestContext testContext = contextWithWorld(0, 0, 0, 100);
        new WorldTimeController(testContext.context(), WORLD_KEY).start();

        testContext.scheduler().tickRepeatingTasks();

        assertEquals(1, testContext.world().currentTime());
    }

    @Test
    void doesNotAccelerateNightWhenThereAreNoPlayers() {
        TestContext testContext = contextWithWorld(13_000, 0, 0, 0);
        new WorldTimeController(testContext.context(), WORLD_KEY).start();

        testContext.scheduler().tickRepeatingTasks();

        assertEquals(13_001, testContext.world().currentTime());
    }

    @Test
    void acceleratesNightWhenSleepingSnapshotMeetsThreshold() {
        TestContext testContext = contextWithWorld(13_000, 2, 2, 100);
        new WorldTimeController(testContext.context(), WORLD_KEY).start();

        testContext.scheduler().tickRepeatingTasks();

        assertEquals(13_100, testContext.world().currentTime());
    }

    @Test
    void resynchronizesAfterExternalTimeChange() {
        TestContext testContext = contextWithWorld(0, 0, 0, 100);
        new WorldTimeController(testContext.context(), WORLD_KEY).start();

        testContext.scheduler().tickRepeatingTasks();
        testContext.world().currentTime(1_000);
        testContext.scheduler().tickRepeatingTasks();

        assertEquals(1_001, testContext.world().currentTime());
    }

    @Test
    void worldTimeManagerStopIsIdempotentAndAllowsRestart() {
        TestContext testContext = contextWithWorld(0, 0, 0, 100);
        WorldTimeManager manager = testContext.context().worldTimeManager();

        manager.start(WORLD_KEY);
        manager.start(WORLD_KEY);
        assertEquals(1, testContext.scheduler().activeRepeatingTaskCount());

        manager.stop(WORLD_KEY);
        manager.stop(WORLD_KEY);
        assertEquals(0, testContext.scheduler().activeRepeatingTaskCount());

        manager.start(WORLD_KEY);
        assertEquals(1, testContext.scheduler().activeRepeatingTaskCount());
    }

    private TestContext contextWithWorld(long time, int players, int sleepingPlayers, int sleepingPercentage) {
        TestScheduler scheduler = new TestScheduler();
        TestWorld world = new TestWorld(time, players, sleepingPlayers, sleepingPercentage);
        TestPlatform platform = new TestPlatform(tempDir, scheduler);
        CustomDaytimeContext context = new CustomDaytimeContext(platform);
        context.worldCache().registerWorld(world);
        return new TestContext(context, scheduler, world);
    }

    private record TestContext(CustomDaytimeContext context, TestScheduler scheduler, TestWorld world) { }

    private static final class TestWorld implements PlatformWorld {

        private long currentTime;
        private final int playerCount;
        private final int sleepingPlayerCount;
        private final int sleepingPercentage;

        private TestWorld(long currentTime, int playerCount, int sleepingPlayerCount, int sleepingPercentage) {
            this.currentTime = currentTime;
            this.playerCount = playerCount;
            this.sleepingPlayerCount = sleepingPlayerCount;
            this.sleepingPercentage = sleepingPercentage;
        }

        @Override
        public WorldKey key() {
            return WORLD_KEY;
        }

        @Override
        public String keyAsString() {
            return WORLD_KEY.asString();
        }

        @Override
        public Optional<Long> time() {
            return Optional.of(currentTime);
        }

        @Override
        public boolean time(long time) {
            currentTime = time;
            return true;
        }

        @Override
        public int playerCount() {
            return playerCount;
        }

        @Override
        public int sleepingPlayerCount() {
            return sleepingPlayerCount;
        }

        @Override
        public boolean gameRuleAdvanceTime() {
            return true;
        }

        @Override
        public int gameRulePlayerSleepingPercentage() {
            return sleepingPercentage;
        }

        private long currentTime() {
            return currentTime;
        }

        private void currentTime(long currentTime) {
            this.currentTime = currentTime;
        }
    }

    private static final class TestPlatform implements Platform {

        private final Path configDirectory;
        private final TestScheduler scheduler;

        private TestPlatform(Path configDirectory, TestScheduler scheduler) {
            this.configDirectory = configDirectory;
            this.scheduler = scheduler;
        }

        @Override
        public PlatformType platform() {
            return PlatformType.FOLIA;
        }

        @Override
        public String minecraftVersion() {
            return "test";
        }

        @Override
        public String projectVersion() {
            return "test";
        }

        @Override
        public Path configDirectory() {
            return configDirectory;
        }

        @Override
        public PlatformLogger logger() {
            return new NoOpLogger();
        }

        @Override
        public PlatformScheduler scheduler() {
            return scheduler;
        }

        @Override
        public Optional<PlatformWorld> world(WorldKey key) {
            return Optional.empty();
        }

        @Override
        public List<PlatformWorld> worlds() {
            return List.of();
        }

        @Override
        public boolean debug() {
            return false;
        }
    }

    private static final class TestScheduler implements PlatformScheduler {

        private final TestTaskScheduler globalScheduler = new TestTaskScheduler();

        @Override
        public PlatformTaskScheduler global() {
            return globalScheduler;
        }

        @Override
        public PlatformTaskScheduler async() {
            return globalScheduler;
        }

        @Override
        public PlatformTaskScheduler region(WorldKey worldKey, int chunkX, int chunkZ) {
            return globalScheduler;
        }

        @Override
        public PlatformTaskScheduler entity(Object entityHandle) {
            return globalScheduler;
        }

        private void tickRepeatingTasks() {
            globalScheduler.tickRepeatingTasks();
        }

        private int activeRepeatingTaskCount() {
            return globalScheduler.activeRepeatingTaskCount();
        }
    }

    private static final class TestTaskScheduler implements PlatformTaskScheduler {

        private final List<TestTask> repeatingTasks = new ArrayList<>();

        @Override
        public PlatformTask run(Runnable runnable) {
            runnable.run();
            return () -> { };
        }

        @Override
        public PlatformTask runLater(Runnable runnable, long delayTicks) {
            runnable.run();
            return () -> { };
        }

        @Override
        public PlatformTask runRepeating(Runnable runnable, long intervalTicks) {
            TestTask task = new TestTask(runnable);
            repeatingTasks.add(task);
            return task;
        }

        private void tickRepeatingTasks() {
            List.copyOf(repeatingTasks).forEach(TestTask::run);
        }

        private int activeRepeatingTaskCount() {
            return repeatingTasks.size();
        }

        private final class TestTask implements PlatformTask {

            private final Runnable runnable;

            private TestTask(Runnable runnable) {
                this.runnable = runnable;
            }

            @Override
            public void cancel() {
                repeatingTasks.remove(this);
            }

            private void run() {
                runnable.run();
            }
        }
    }

    private static final class NoOpLogger implements PlatformLogger {

        @Override
        public void info(String message) {
        }

        @Override
        public void error(String message) {
        }
    }
}
