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
    private final FoliaPlayerEligibilityService eligibilityService = new FoliaPlayerEligibilityService();

    public void registerWorld(World world) {
        snapshots.computeIfAbsent(key(world), ignored -> new WorldSnapshot());
    }

    public void unregisterWorld(WorldKey key) {
        snapshots.remove(key);
    }

    public void refresh(Player player) {
        WorldKey key = key(player.getWorld());
        WorldSnapshot snapshot = snapshot(key);
        UUID playerId = player.getUniqueId();
        snapshot.viewers().add(playerId);
        boolean countable = eligibilityService.shouldCount(player);
        if (countable) {
            snapshot.players().add(playerId);
        } else {
            snapshot.players().remove(playerId);
        }
        if (countable && player.isSleeping()) {
            snapshot.sleepingPlayers().add(playerId);
        } else {
            snapshot.sleepingPlayers().remove(playerId);
        }
    }

    public void playerQuit(Player player) {
        remove(player.getUniqueId());
    }

    public void playerChangedWorld(Player player, World from) {
        remove(player.getUniqueId(), key(from));
        refresh(player);
    }

    public void sleeping(Player player, boolean sleeping) {
        refresh(player);
        if (!sleeping) {
            snapshot(key(player.getWorld())).sleepingPlayers().remove(player.getUniqueId());
        }
    }

    public int playerCount(WorldKey key) {
        return snapshot(key).players().size();
    }

    public int sleepingPlayerCount(WorldKey key) {
        return snapshot(key).sleepingPlayers().size();
    }

    public Set<UUID> viewerIds(WorldKey key) {
        return Set.copyOf(snapshot(key).viewers());
    }

    private void remove(UUID playerId) {
        snapshots.values().forEach(snapshot -> {
            snapshot.players().remove(playerId);
            snapshot.sleepingPlayers().remove(playerId);
            snapshot.viewers().remove(playerId);
        });
    }

    private void remove(UUID playerId, WorldKey key) {
        WorldSnapshot snapshot = snapshot(key);
        snapshot.players().remove(playerId);
        snapshot.sleepingPlayers().remove(playerId);
        snapshot.viewers().remove(playerId);
    }

    private WorldSnapshot snapshot(WorldKey key) {
        return snapshots.computeIfAbsent(key, ignored -> new WorldSnapshot());
    }

    public static WorldKey key(World world) {
        Key key = world.getKey();
        return new WorldKey(key.namespace(), key.value());
    }

    private record WorldSnapshot(Set<UUID> players, Set<UUID> sleepingPlayers, Set<UUID> viewers) {

        private WorldSnapshot() {
            this(ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet());
        }
    }
}
