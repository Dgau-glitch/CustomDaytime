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
import org.bukkit.World;
import org.bukkit.entity.Player;
import xyz.mayahive.customdaytime.api.platform.PlatformScheduler;
import xyz.mayahive.customdaytime.common.event.EventBus;
import xyz.mayahive.customdaytime.common.event.type.WorldLoadEvent;
import xyz.mayahive.customdaytime.common.event.type.WorldPlayerCountChangeEvent;
import xyz.mayahive.customdaytime.common.event.type.WorldSleepingPlayerCountChangeEvent;
import xyz.mayahive.customdaytime.common.event.type.WorldUnloadEvent;
import xyz.mayahive.customdaytime.folia.platform.FoliaWorld;

@RequiredArgsConstructor
public class FoliaEventAdapter {

    private final EventBus eventBus;
    private final PlatformScheduler scheduler;
    private final FoliaWorldSnapshotStore snapshotStore;

    public void worldLoaded(World world) {
        eventBus.fire(new WorldLoadEvent(platformWorld(world)));
    }

    public void worldUnloaded(World world) {
        FoliaWorld platformWorld = platformWorld(world);
        eventBus.fire(new WorldUnloadEvent(platformWorld));
        snapshotStore.unregisterWorld(platformWorld.key());
    }

    public void playerJoined(Player player) {
        snapshotStore.playerJoined(player);
        firePlayerCountChanged(player.getWorld());
    }

    public void playerQuit(Player player) {
        World world = player.getWorld();
        snapshotStore.playerQuit(player);
        firePlayerCountChanged(world);
    }

    public void playerChangedWorld(Player player, World from) {
        snapshotStore.playerChangedWorld(player, from);
        firePlayerCountChanged(from);
        firePlayerCountChanged(player.getWorld());
    }

    public void bedEntered(Player player) {
        scheduler.entity(player).runLater(
                () -> updateSleepingSnapshot(player, player.isSleeping()),
                1
        );
    }

    public void bedLeft(Player player) {
        updateSleepingSnapshot(player, false);
    }

    private void updateSleepingSnapshot(Player player, boolean sleeping) {
        World world = player.getWorld();
        snapshotStore.sleeping(player, sleeping);
        eventBus.fire(new WorldSleepingPlayerCountChangeEvent(platformWorld(world)));
    }

    private void firePlayerCountChanged(World world) {
        eventBus.fire(new WorldPlayerCountChangeEvent(platformWorld(world)));
    }

    private FoliaWorld platformWorld(World world) {
        return new FoliaWorld(world, snapshotStore);
    }
}
