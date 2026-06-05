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

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import xyz.mayahive.customdaytime.api.model.WorldKey;
import xyz.mayahive.customdaytime.api.platform.PlatformScheduler;
import xyz.mayahive.customdaytime.common.service.ConfigService;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class FoliaSleepBossBarService {

    private static final boolean DEFAULT_ENABLED = true;
    private static final String DEFAULT_MESSAGE = "&bSleeping: &f{sleeping}/{required} &7({percentage}%)";
    private static final String DEFAULT_COLOR = "BLUE";
    private static final String DEFAULT_OVERLAY = "PROGRESS";
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final ConfigService configService;
    private final PlatformScheduler scheduler;
    private final FoliaWorldSnapshotStore snapshotStore;
    private final ConcurrentHashMap<WorldKey, BossBar> bossBars = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WorldKey, Set<UUID>> shownPlayers = new ConcurrentHashMap<>();

    public void update(World world) {
        WorldKey key = FoliaWorldSnapshotStore.key(world);
        scheduler.global().run(() -> update(key));
    }

    private void update(WorldKey key) {
        if (!enabled(key)) {
            hide(key);
            return;
        }

        int sleeping = snapshotStore.sleepingPlayerCount(key);
        int total = snapshotStore.playerCount(key);
        if (sleeping <= 0 || total <= 0) {
            hide(key);
            return;
        }

        int threshold = playersSleepingPercentage(key);
        int required = Math.max(1, (int) Math.ceil(total * (threshold / 100.0)));
        double percentage = Math.min(100.0, ((double) sleeping / total) * 100.0);
        float progress = (float) Math.max(0.0, Math.min(1.0, (double) sleeping / required));

        BossBar bossBar = bossBars.computeIfAbsent(key, ignored -> BossBar.bossBar(
                Component.empty(),
                progress,
                color(key),
                overlay(key)
        ));
        bossBar.name(LEGACY_SERIALIZER.deserialize(message(key, sleeping, total, required, threshold, percentage)));
        bossBar.progress(progress);
        bossBar.color(color(key));
        bossBar.overlay(overlay(key));

        Set<UUID> currentViewers = snapshotStore.viewerIds(key);
        Set<UUID> previousViewers = shownPlayers.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet());
        previousViewers.stream()
                .filter(playerId -> !currentViewers.contains(playerId))
                .map(Bukkit::getPlayer)
                .forEach(player -> hide(player, bossBar));
        previousViewers.retainAll(currentViewers);
        currentViewers.stream()
                .map(Bukkit::getPlayer)
                .forEach(player -> show(player, bossBar));
        previousViewers.addAll(currentViewers);
    }

    public void hide(WorldKey key) {
        BossBar bossBar = bossBars.remove(key);
        if (bossBar == null) {
            shownPlayers.remove(key);
            return;
        }
        shownPlayers.getOrDefault(key, Set.of()).stream()
                .map(Bukkit::getPlayer)
                .forEach(player -> hide(player, bossBar));
        shownPlayers.remove(key);
    }

    public void hideAll() {
        Set.copyOf(bossBars.keySet()).forEach(this::hide);
    }

    private void show(Player player, BossBar bossBar) {
        if (player == null) return;
        scheduler.entity(player).run(() -> player.showBossBar(bossBar));
    }

    private void hide(Player player, BossBar bossBar) {
        if (player == null) return;
        scheduler.entity(player).run(() -> player.hideBossBar(bossBar));
    }

    private boolean enabled(WorldKey key) {
        return configService.getConfigValue(Boolean.class, DEFAULT_ENABLED, key.asString(), "sleepBossBarEnabled");
    }

    private String message(WorldKey key, int sleeping, int total, int required, int threshold, double percentage) {
        String template = configService.getConfigValue(String.class, DEFAULT_MESSAGE, key.asString(), "sleepBossBarMessage");
        return template
                .replace("{sleeping}", Integer.toString(sleeping))
                .replace("{total}", Integer.toString(total))
                .replace("{required}", Integer.toString(required))
                .replace("{threshold}", Integer.toString(threshold))
                .replace("{percentage}", Integer.toString((int) Math.round(percentage)))
                .replace("{world}", key.asString());
    }

    private int playersSleepingPercentage(WorldKey key) {
        int percentage = configService.getConfigValue(Integer.class, 50, key.asString(), "playersSleepingPercentage");
        return Math.max(0, Math.min(100, percentage));
    }

    private BossBar.Color color(WorldKey key) {
        String value = configService.getConfigValue(String.class, DEFAULT_COLOR, key.asString(), "sleepBossBarColor");
        try {
            return BossBar.Color.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BossBar.Color.BLUE;
        }
    }

    private BossBar.Overlay overlay(WorldKey key) {
        String value = configService.getConfigValue(String.class, DEFAULT_OVERLAY, key.asString(), "sleepBossBarOverlay");
        try {
            return BossBar.Overlay.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }
}
