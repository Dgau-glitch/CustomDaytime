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

package xyz.mayahive.customdaytime.common.cache;

import org.junit.jupiter.api.Test;
import xyz.mayahive.customdaytime.api.model.WorldKey;
import xyz.mayahive.customdaytime.api.platform.PlatformWorld;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorldCacheTest {

    private static final WorldKey WORLD_KEY = new WorldKey("minecraft", "overworld");

    @Test
    void repeatedRegisterAndUnregisterAreIdempotent() {
        WorldCache cache = new WorldCache();
        PlatformWorld world = new IdentityOnlyWorld(WORLD_KEY);

        cache.registerWorld(world);
        cache.registerWorld(world);
        assertEquals(1, cache.getWorlds().size());
        assertEquals(world, cache.getWorld(WORLD_KEY));

        cache.unregisterWorld(world);
        cache.unregisterWorld(world);
        assertNull(cache.getWorld(WORLD_KEY));
        assertEquals(0, cache.getWorlds().size());
    }

    private record IdentityOnlyWorld(WorldKey key) implements PlatformWorld {

        @Override
        public String keyAsString() {
            return key.asString();
        }

        @Override
        public Optional<Long> time() {
            return Optional.empty();
        }

        @Override
        public boolean time(long time) {
            return false;
        }

        @Override
        public int playerCount() {
            return 0;
        }

        @Override
        public int sleepingPlayerCount() {
            return 0;
        }

        @Override
        public boolean gameRuleAdvanceTime() {
            return false;
        }

    }
}
