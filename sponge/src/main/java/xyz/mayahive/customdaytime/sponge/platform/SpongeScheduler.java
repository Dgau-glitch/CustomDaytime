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

package xyz.mayahive.customdaytime.sponge.platform;

import lombok.RequiredArgsConstructor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import xyz.mayahive.customdaytime.api.model.WorldKey;
import xyz.mayahive.customdaytime.api.platform.PlatformScheduler;
import xyz.mayahive.customdaytime.api.platform.PlatformTask;
import xyz.mayahive.customdaytime.api.platform.PlatformTaskScheduler;
import xyz.mayahive.customdaytime.sponge.CustomDaytimeSponge;

@RequiredArgsConstructor
public class SpongeScheduler implements PlatformScheduler {

    private final CustomDaytimeSponge plugin;

    @Override
    public PlatformTaskScheduler global() {
        return new SpongeTaskScheduler(Sponge.server().scheduler());
    }

    @Override
    public PlatformTaskScheduler async() {
        return new SpongeTaskScheduler(Sponge.asyncScheduler());
    }

    @Override
    public PlatformTaskScheduler region(WorldKey worldKey, int chunkX, int chunkZ) {
        return global();
    }

    @Override
    public PlatformTaskScheduler entity(Object entityHandle) {
        return global();
    }

    @RequiredArgsConstructor
    private class SpongeTaskScheduler implements PlatformTaskScheduler {

        private final Scheduler scheduler;

        @Override
        public PlatformTask run(Runnable runnable) {
            return submit(Task.builder().execute(runnable));
        }

        @Override
        public PlatformTask runLater(Runnable runnable, long delayTicks) {
            return submit(Task.builder().execute(runnable).delay(Ticks.of(delayTicks)));
        }

        @Override
        public PlatformTask runRepeating(Runnable runnable, long intervalTicks) {
            return submit(Task.builder().execute(runnable).interval(Ticks.of(intervalTicks)));
        }

        private PlatformTask submit(Task.Builder builder) {
            ScheduledTask task = scheduler.submit(builder.plugin(plugin.container()).build());
            return new SpongeTask(task);
        }
    }
}
