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

package xyz.mayahive.customdaytime.folia.listener;

import lombok.RequiredArgsConstructor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import xyz.mayahive.customdaytime.api.platform.PlatformScheduler;
import xyz.mayahive.customdaytime.common.event.EventBus;
import xyz.mayahive.customdaytime.common.event.type.WorldSleepingPlayerCountChangeEvent;
import xyz.mayahive.customdaytime.folia.platform.FoliaWorld;
import xyz.mayahive.customdaytime.folia.service.FoliaWorldSnapshotStore;

@RequiredArgsConstructor
public class BedActivityListener implements Listener {

    private final EventBus eventBus;
    private final PlatformScheduler scheduler;
    private final FoliaWorldSnapshotStore snapshotStore;

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        scheduler.entity(player).runLater(
                () -> updateSleepingSnapshot(player, player.isSleeping()),
                1
        );
    }

    @EventHandler
    public void onBedLeave(PlayerBedLeaveEvent event) {
        updateSleepingSnapshot(event.getPlayer(), false);
    }

    private void updateSleepingSnapshot(Player player, boolean sleeping) {
        World world = player.getWorld();
        snapshotStore.sleeping(player, sleeping);
        eventBus.fire(new WorldSleepingPlayerCountChangeEvent(new FoliaWorld(world, snapshotStore)));
    }
}
