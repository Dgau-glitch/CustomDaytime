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
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import xyz.mayahive.customdaytime.api.model.PlatformType;
import xyz.mayahive.customdaytime.api.model.WorldKey;
import xyz.mayahive.customdaytime.api.platform.Platform;
import xyz.mayahive.customdaytime.api.platform.PlatformLogger;
import xyz.mayahive.customdaytime.api.platform.PlatformScheduler;
import xyz.mayahive.customdaytime.api.platform.PlatformWorld;
import xyz.mayahive.customdaytime.folia.service.FoliaWorldSnapshotStore;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class FoliaPlatform implements Platform {

    private final Plugin plugin;
    private final FoliaWorldSnapshotStore snapshotStore;

    @Override
    public PlatformType platform() {
        return PlatformType.FOLIA;
    }

    @Override
    public String minecraftVersion() {
        return Bukkit.getMinecraftVersion();
    }

    @Override
    public String projectVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public Path configDirectory() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public PlatformLogger logger() {
        return new FoliaLogger(plugin);
    }

    @Override
    public PlatformScheduler scheduler() {
        return new FoliaScheduler(plugin);
    }

    @Override
    public Optional<PlatformWorld> world(WorldKey key) {
        return Optional.ofNullable(plugin.getServer().getWorld(key.asString()))
                .map(world -> new FoliaWorld(world, snapshotStore));
    }

    @Override
    public List<PlatformWorld> worlds() {
        return plugin.getServer().getWorlds().stream()
                .map(world -> new FoliaWorld(world, snapshotStore))
                .collect(Collectors.toList());
    }

    @Override
    public boolean debug() {
        return false;
    }
}
