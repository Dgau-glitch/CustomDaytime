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

package xyz.mayahive.customdaytime.folia;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mayahive.customdaytime.api.platform.Platform;
import xyz.mayahive.customdaytime.api.platform.PlatformScheduler;
import xyz.mayahive.customdaytime.common.bootstrap.AbstractBootstrap;
import xyz.mayahive.customdaytime.common.event.EventBus;
import xyz.mayahive.customdaytime.common.service.ConfigService;
import xyz.mayahive.customdaytime.common.service.ReloadService;
import xyz.mayahive.customdaytime.folia.command.CustomDaytimeCommand;
import xyz.mayahive.customdaytime.folia.listener.BedActivityListener;
import xyz.mayahive.customdaytime.folia.listener.TimeSkipListener;
import xyz.mayahive.customdaytime.folia.listener.WorldActivityListener;
import xyz.mayahive.customdaytime.folia.listener.WorldListener;
import xyz.mayahive.customdaytime.folia.platform.FoliaPlatform;
import xyz.mayahive.customdaytime.folia.service.FoliaEventAdapter;
import xyz.mayahive.customdaytime.folia.service.FoliaPlayerSnapshotRefreshService;
import xyz.mayahive.customdaytime.folia.service.FoliaSleepBossBarService;
import xyz.mayahive.customdaytime.folia.service.FoliaWorldSnapshotStore;

public final class CustomDaytimeFolia extends JavaPlugin {

    private FoliaSleepBossBarService bossBarService;
    private FoliaPlayerSnapshotRefreshService snapshotRefreshService;

    @Override
    public void onEnable() {

        new Metrics(this, 26910);

        FoliaWorldSnapshotStore snapshotStore = new FoliaWorldSnapshotStore();

        AbstractBootstrap bootstrap = new AbstractBootstrap() {
            @Override
            protected Platform platform() {
                return new FoliaPlatform(CustomDaytimeFolia.this, snapshotStore);
            }
        };

        bootstrap.initialize();

        EventBus eventBus = bootstrap.context().eventBus();
        ConfigService configService = bootstrap.context().configService();
        PlatformScheduler scheduler = bootstrap.context().platform().scheduler();
        bossBarService = new FoliaSleepBossBarService(configService, scheduler, snapshotStore);
        snapshotRefreshService = new FoliaPlayerSnapshotRefreshService(eventBus, scheduler, snapshotStore, bossBarService);
        snapshotRefreshService.start();
        FoliaEventAdapter eventAdapter = new FoliaEventAdapter(eventBus, scheduler, snapshotStore, bossBarService);

        Bukkit.getPluginManager().registerEvents(new BedActivityListener(eventAdapter), this);
        Bukkit.getPluginManager().registerEvents(new TimeSkipListener(configService), this);
        Bukkit.getPluginManager().registerEvents(new WorldActivityListener(eventAdapter), this);
        Bukkit.getPluginManager().registerEvents(new WorldListener(eventAdapter), this);

        registerCommands(scheduler, new ReloadService(bootstrap.context()));
    }

    @Override
    public void onDisable() {
        if (snapshotRefreshService != null) {
            snapshotRefreshService.stop();
        }
        if (bossBarService != null) {
            bossBarService.hideAll();
        }
    }

    private void registerCommands(PlatformScheduler scheduler, ReloadService reloadService) {
        CustomDaytimeCommand command = new CustomDaytimeCommand(this, scheduler, reloadService);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(
                CustomDaytimeCommand.NAME,
                CustomDaytimeCommand.DESCRIPTION,
                CustomDaytimeCommand.ALIASES,
                command
        ));
    }
}
