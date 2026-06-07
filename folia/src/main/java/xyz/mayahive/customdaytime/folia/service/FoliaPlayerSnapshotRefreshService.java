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

package xyz.mayahive.customdaytime.folia.service;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import xyz.mayahive.customdaytime.api.platform.PlatformScheduler;
import xyz.mayahive.customdaytime.api.platform.PlatformTask;
import xyz.mayahive.customdaytime.common.event.EventBus;
import xyz.mayahive.customdaytime.common.event.type.WorldPlayerCountChangeEvent;
import xyz.mayahive.customdaytime.common.event.type.WorldSleepingPlayerCountChangeEvent;
import xyz.mayahive.customdaytime.folia.platform.FoliaWorld;

@RequiredArgsConstructor
public class FoliaPlayerSnapshotRefreshService {

    private static final long REFRESH_INTERVAL_TICKS = 20L;

    private final EventBus eventBus;
    private final PlatformScheduler scheduler;
    private final FoliaWorldSnapshotStore snapshotStore;
    private final FoliaSleepBossBarService bossBarService;
    private PlatformTask task;

    public void start() {
        task = scheduler.global().runRepeating(this::refreshAll, REFRESH_INTERVAL_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduler.entity(player).run(() -> refresh(player));
        }
    }

    private void refresh(Player player) {
        World world = player.getWorld();
        snapshotStore.refresh(player);
        FoliaWorld platformWorld = new FoliaWorld(world, snapshotStore);
        eventBus.fire(new WorldPlayerCountChangeEvent(platformWorld));
        eventBus.fire(new WorldSleepingPlayerCountChangeEvent(platformWorld));
        bossBarService.update(world);
    }
}
