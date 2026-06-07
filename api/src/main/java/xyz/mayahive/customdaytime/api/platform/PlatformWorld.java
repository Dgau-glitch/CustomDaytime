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

package xyz.mayahive.customdaytime.api.platform;

import xyz.mayahive.customdaytime.api.model.WorldKey;

import java.util.Optional;

/**
 * Represents a world in a platform-agnostic way.
 * <p>
 * The key methods are immutable identity and can be used from common logic.
 * State methods must be called from the scheduler context that owns the
 * relevant platform state. On Folia, world time and gamerules belong to
 * {@link PlatformScheduler#global()}, while player/sleeping counts should be
 * provided by platform snapshots instead of iterating entities from arbitrary
 * contexts.
 */
public interface PlatformWorld {

    /**
     * Returns the world key such as "minecraft:overworld".
     *
     * @return the world's key
     */
    WorldKey key();

    /**
     * Returns a human-readable key for this world, e.g., "minecraft:overworld".
     *
     * @return the world key as a string
     */
    String keyAsString();

    /**
     * Returns the current time of the world if available.
     * Must be called from the platform context that owns world time.
     *
     * @return an Optional containing the world time, or empty if unavailable
     */
    Optional<Long> time();

    /**
     * Sets the current time of the world.
     * Must be called from the platform context that owns world time.
     *
     * @param time the new time value
     * @return true if the time was successfully set, false otherwise
     */
    boolean time(long time);


    /**
     * Returns a platform-maintained player count snapshot for this world.
     *
     * @return player count
     */
    int playerCount();

    /**
     * Returns a platform-maintained sleeping player count snapshot for this world.
     *
     * @return number of sleeping players
     */
    int sleepingPlayerCount();

    /**
     * Returns whether the "doDaylightCycle" or equivalent game rule is enabled.
     * Must be called from the platform context that owns gamerules.
     *
     * @return true if time advances automatically, false otherwise
     */
    boolean gameRuleAdvanceTime();
}
