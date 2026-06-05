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

import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.bukkit.entity.Player;
import xyz.mayahive.customdaytime.api.model.WorldKey;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores player-count snapshots that can be read by common code without
 * traversing Folia entity collections from the wrong scheduler context.
 */
public class FoliaWorldSnapshotStore {

    private final ConcurrentHashMap<WorldKey, WorldSnapshot> snapshots = new ConcurrentHashMap<>();

    public void registerWorld(World world) {
        snapshots.computeIfAbsent(key(world), ignored -> new WorldSnapshot());
    }

    public void unregisterWorld(WorldKey key) {
        snapshots.remove(key);
    }

    public void playerJoined(Player player) {
        WorldKey key = key(player.getWorld());
        snapshot(key).players().add(player.getUniqueId());
    }

    public void playerQuit(Player player) {
        UUID playerId = player.getUniqueId();
        snapshots.values().forEach(snapshot -> {
            snapshot.players().remove(playerId);
            snapshot.sleepingPlayers().remove(playerId);
        });
    }

    public void playerChangedWorld(Player player, World from) {
        UUID playerId = player.getUniqueId();
        WorldSnapshot fromSnapshot = snapshot(key(from));
        fromSnapshot.players().remove(playerId);
        fromSnapshot.sleepingPlayers().remove(playerId);

        WorldKey toKey = key(player.getWorld());
        snapshot(toKey).players().add(playerId);
    }

    public void sleeping(Player player, boolean sleeping) {
        WorldKey key = key(player.getWorld());
        WorldSnapshot snapshot = snapshot(key);
        UUID playerId = player.getUniqueId();
        snapshot.players().add(playerId);
        if (sleeping) {
            snapshot.sleepingPlayers().add(playerId);
        } else {
            snapshot.sleepingPlayers().remove(playerId);
        }
    }

    public int playerCount(WorldKey key) {
        return snapshot(key).players().size();
    }

    public int sleepingPlayerCount(WorldKey key) {
        return snapshot(key).sleepingPlayers().size();
    }

    private WorldSnapshot snapshot(WorldKey key) {
        return snapshots.computeIfAbsent(key, ignored -> new WorldSnapshot());
    }

    private static WorldKey key(World world) {
        Key key = world.getKey();
        return new WorldKey(key.namespace(), key.value());
    }

    private record WorldSnapshot(Set<UUID> players, Set<UUID> sleepingPlayers) {

        private WorldSnapshot() {
            this(ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet());
        }
    }
}
