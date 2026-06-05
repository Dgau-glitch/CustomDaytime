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
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.mayahive.customdaytime.common.event.EventBus;
import xyz.mayahive.customdaytime.common.event.type.WorldPlayerCountChangeEvent;
import xyz.mayahive.customdaytime.folia.platform.FoliaWorld;
import xyz.mayahive.customdaytime.folia.service.FoliaWorldSnapshotStore;

@RequiredArgsConstructor
public class WorldActivityListener implements Listener {

    private final EventBus eventBus;
    private final FoliaWorldSnapshotStore snapshotStore;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        snapshotStore.playerJoined(player);
        firePlayerCountChange(player.getWorld());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        snapshotStore.playerQuit(player);
        firePlayerCountChange(world);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        snapshotStore.playerChangedWorld(player, event.getFrom());
        firePlayerCountChange(event.getFrom());
        firePlayerCountChange(player.getWorld());
    }

    private void firePlayerCountChange(World world) {
        eventBus.fire(new WorldPlayerCountChangeEvent(new FoliaWorld(world, snapshotStore)));
    }
}
