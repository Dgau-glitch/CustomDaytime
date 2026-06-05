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

package xyz.mayahive.customdaytime.folia.platform;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import xyz.mayahive.customdaytime.api.model.WorldKey;
import xyz.mayahive.customdaytime.api.platform.PlatformScheduler;
import xyz.mayahive.customdaytime.api.platform.PlatformTask;
import xyz.mayahive.customdaytime.api.platform.PlatformTaskScheduler;

import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

@RequiredArgsConstructor
public class FoliaScheduler implements PlatformScheduler {

    private static final long TICK_MILLIS = 50L;

    private final Plugin plugin;

    @Override
    public PlatformTaskScheduler global() {
        return new FoliaTaskScheduler(
                runnable -> Bukkit.getGlobalRegionScheduler().run(plugin, task -> runnable.run()),
                (runnable, delayTicks) -> Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> runnable.run(), delayTicks),
                (runnable, intervalTicks) -> Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> runnable.run(), 1, intervalTicks)
        );
    }

    @Override
    public PlatformTaskScheduler async() {
        return new FoliaTaskScheduler(
                runnable -> plugin.getServer().getAsyncScheduler().runNow(plugin, task -> runnable.run()),
                (runnable, delayTicks) -> plugin.getServer().getAsyncScheduler().runDelayed(plugin, task -> runnable.run(), toMillis(delayTicks), TimeUnit.MILLISECONDS),
                (runnable, intervalTicks) -> plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, task -> runnable.run(), TICK_MILLIS, toMillis(intervalTicks), TimeUnit.MILLISECONDS)
        );
    }

    @Override
    public PlatformTaskScheduler region(WorldKey worldKey, int chunkX, int chunkZ) {
        World world = plugin.getServer().getWorld(worldKey.asString());
        if (world == null) {
            throw new IllegalArgumentException("Unknown world for region scheduler: " + worldKey.asString());
        }

        return new FoliaTaskScheduler(
                runnable -> plugin.getServer().getRegionScheduler().run(plugin, world, chunkX, chunkZ, task -> runnable.run()),
                (runnable, delayTicks) -> plugin.getServer().getRegionScheduler().runDelayed(plugin, world, chunkX, chunkZ, task -> runnable.run(), delayTicks),
                (runnable, intervalTicks) -> plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, world, chunkX, chunkZ, task -> runnable.run(), 1, intervalTicks)
        );
    }

    @Override
    public PlatformTaskScheduler entity(Object entityHandle) {
        if (!(entityHandle instanceof Entity entity)) {
            throw new IllegalArgumentException("Folia entity scheduler requires an org.bukkit.entity.Entity handle.");
        }

        return new FoliaTaskScheduler(
                runnable -> entity.getScheduler().run(plugin, task -> runnable.run(), null),
                (runnable, delayTicks) -> entity.getScheduler().runDelayed(plugin, task -> runnable.run(), null, delayTicks),
                (runnable, intervalTicks) -> entity.getScheduler().runAtFixedRate(plugin, task -> runnable.run(), null, 1, intervalTicks)
        );
    }

    private static long toMillis(long ticks) {
        return Math.max(0, ticks) * TICK_MILLIS;
    }

    private record FoliaTaskScheduler(
            Function<Runnable, ScheduledTask> runOperation,
            BiFunction<Runnable, Long, ScheduledTask> laterOperation,
            BiFunction<Runnable, Long, ScheduledTask> repeatingOperation
    ) implements PlatformTaskScheduler {

        @Override
        public PlatformTask run(Runnable runnable) {
            return FoliaTask.from(runOperation.apply(runnable));
        }

        @Override
        public PlatformTask runLater(Runnable runnable, long delayTicks) {
            return FoliaTask.from(laterOperation.apply(runnable, delayTicks));
        }

        @Override
        public PlatformTask runRepeating(Runnable runnable, long intervalTicks) {
            return FoliaTask.from(repeatingOperation.apply(runnable, intervalTicks));
        }
    }
}
