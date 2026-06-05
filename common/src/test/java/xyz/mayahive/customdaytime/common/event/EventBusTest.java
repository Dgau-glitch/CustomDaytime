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

package xyz.mayahive.customdaytime.common.event;

import org.junit.jupiter.api.Test;
import xyz.mayahive.customdaytime.api.model.WorldKey;
import xyz.mayahive.customdaytime.api.platform.PlatformScheduler;
import xyz.mayahive.customdaytime.api.platform.PlatformTask;
import xyz.mayahive.customdaytime.api.platform.PlatformTaskScheduler;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventBusTest {

    @Test
    void fireSerializesDispatchThroughGlobalScheduler() {
        QueuedScheduler scheduler = new QueuedScheduler();
        EventBus eventBus = new EventBus(scheduler);
        AtomicInteger handled = new AtomicInteger();
        eventBus.register(TestEvent.class, event -> handled.incrementAndGet());

        eventBus.fire(new TestEvent());
        assertEquals(0, handled.get());

        scheduler.runNextGlobalTask();
        assertEquals(1, handled.get());
    }

    private record TestEvent() implements Event { }

    private static final class QueuedScheduler implements PlatformScheduler {

        private final QueuedTaskScheduler globalScheduler = new QueuedTaskScheduler();

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

        private void runNextGlobalTask() {
            globalScheduler.runNext();
        }
    }

    private static final class QueuedTaskScheduler implements PlatformTaskScheduler {

        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public PlatformTask run(Runnable runnable) {
            tasks.add(runnable);
            return () -> tasks.remove(runnable);
        }

        @Override
        public PlatformTask runLater(Runnable runnable, long delayTicks) {
            return run(runnable);
        }

        @Override
        public PlatformTask runRepeating(Runnable runnable, long intervalTicks) {
            return run(runnable);
        }

        private void runNext() {
            tasks.remove().run();
        }
    }
}
