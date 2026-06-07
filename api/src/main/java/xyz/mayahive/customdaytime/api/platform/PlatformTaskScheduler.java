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

/**
 * Scheduler bound to a concrete platform execution context.
 * <p>
 * Tick-based contexts interpret delays and periods as server ticks. Async
 * contexts use the same tick values as plugin-level timing units and may map
 * them to wall-clock duration on platforms that do not tick asynchronously.
 */
public interface PlatformTaskScheduler {

    /**
     * Runs a task in this scheduler context as soon as the platform allows.
     *
     * @param runnable the task to execute
     * @return PlatformTask that can be canceled when the platform exposes a handle
     */
    PlatformTask run(Runnable runnable);

    /**
     * Runs a task once after the given delay.
     *
     * @param runnable the task to execute
     * @param delayTicks number of ticks to wait before execution (non-negative)
     * @return PlatformTask that can be canceled
     */
    PlatformTask runLater(Runnable runnable, long delayTicks);

    /**
     * Runs a task repeatedly with the given interval.
     *
     * @param runnable the task to execute
     * @param intervalTicks number of ticks between executions (positive)
     * @return PlatformTask that can be canceled
     */
    PlatformTask runRepeating(Runnable runnable, long intervalTicks);
}
