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

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.World;
import xyz.mayahive.customdaytime.api.model.WorldKey;
import xyz.mayahive.customdaytime.api.platform.PlatformWorld;
import xyz.mayahive.customdaytime.folia.service.FoliaWorldSnapshotStore;

import java.util.Optional;

@RequiredArgsConstructor
public class FoliaWorld implements PlatformWorld {

    private final WorldKey key;
    private final String keyAsString;
    private final FoliaWorldSnapshotStore snapshotStore;

    public FoliaWorld(World world, FoliaWorldSnapshotStore snapshotStore) {
        this(key(world), world.getKey().asString(), snapshotStore);
        snapshotStore.registerWorld(world);
    }

    @Override
    public WorldKey key() {
        return key;
    }

    @Override
    public String keyAsString() {
        return keyAsString;
    }

    @Override
    public Optional<Long> time() {
        return currentWorld().map(World::getFullTime);
    }

    @Override
    public boolean time(long time) {
        Optional<World> world = currentWorld();
        if (world.isEmpty()) {
            return false;
        }
        world.get().setFullTime(time);
        return true;
    }

    @Override
    public int playerCount() {
        return snapshotStore.playerCount(key);
    }

    @Override
    public int sleepingPlayerCount() {
        return snapshotStore.sleepingPlayerCount(key);
    }

    @Override
    public boolean gameRuleAdvanceTime() {
        return currentWorld()
                .map(world -> Boolean.TRUE.equals(world.getGameRuleValue(GameRules.ADVANCE_TIME)))
                .orElse(false);
    }

    @Override
    public int gameRulePlayerSleepingPercentage() {
        int value = currentWorld()
                .map(world -> world.getGameRuleValue(GameRules.PLAYERS_SLEEPING_PERCENTAGE))
                .orElse(100);
        return Math.max(0, Math.min(100, value));
    }

    private Optional<World> currentWorld() {
        return Optional.ofNullable(Bukkit.getWorld(keyAsString));
    }

    private static WorldKey key(World world) {
        Key key = world.getKey();
        return new WorldKey(key.namespace(), key.value());
    }
}
