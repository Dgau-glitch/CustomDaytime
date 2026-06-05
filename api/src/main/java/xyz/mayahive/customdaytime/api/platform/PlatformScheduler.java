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

/**
 * Access point for platform execution contexts.
 * <p>
 * Folia does not provide a single main thread for all server state. Callers must
 * choose the context that owns the data they are going to access: global for
 * world time, gamerules, weather and sleep skipping; region for chunk/block
 * state; entity for player/entity state; async for work outside the tick loop.
 */
public interface PlatformScheduler {

    /**
     * Returns scheduler for global server state such as world time, gamerules,
     * weather and sleep skipping.
     *
     * @return global execution context
     */
    PlatformTaskScheduler global();

    /**
     * Returns scheduler for asynchronous work outside server tick contexts.
     *
     * @return async execution context
     */
    PlatformTaskScheduler async();

    /**
     * Returns scheduler for a region that owns the given world chunk.
     * Do not use this context for entities that can move between regions.
     *
     * @param worldKey target world key
     * @param chunkX target chunk X
     * @param chunkZ target chunk Z
     * @return region execution context
     */
    PlatformTaskScheduler region(WorldKey worldKey, int chunkX, int chunkZ);

    /**
     * Returns scheduler for a platform-native entity or player object.
     * The object type is intentionally platform-native so platform modules can
     * bind to their own entity scheduler without leaking those types into the
     * common API.
     *
     * @param entityHandle platform-native entity/player handle
     * @return entity execution context
     */
    PlatformTaskScheduler entity(Object entityHandle);
}
