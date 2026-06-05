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

package xyz.mayahive.customdaytime.common.service;

import lombok.RequiredArgsConstructor;
import xyz.mayahive.customdaytime.common.context.CustomDaytimeContext;

@RequiredArgsConstructor
public class ReloadService {

    private final CustomDaytimeContext context;

    public void reload() {
        context.configService().reload();
        context.worldTimeManager().stopAll();
        context.worldCache().clear();
        new WorldInitializationService(context).syncExistingWorlds();
    }
}
