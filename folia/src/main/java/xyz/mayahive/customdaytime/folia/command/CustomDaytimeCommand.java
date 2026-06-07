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

package xyz.mayahive.customdaytime.folia.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.mayahive.customdaytime.api.platform.PlatformScheduler;
import xyz.mayahive.customdaytime.common.service.ReloadService;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class CustomDaytimeCommand implements BasicCommand {

    public static final String NAME = "customdaytime";
    public static final String DESCRIPTION = "CustomDaytime administration command";
    public static final List<String> ALIASES = List.of("cdt");
    public static final String RELOAD_PERMISSION = "customdaytime.command.reload";
    private static final String RELOAD_ARGUMENT = "reload";

    private final Plugin plugin;
    private final PlatformScheduler scheduler;
    private final ReloadService reloadService;

    @Override
    public void execute(@NotNull CommandSourceStack source, String @NotNull [] args) {
        CommandSender sender = source.getSender();
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            reply(sender, "§cYou do not have permission to reload CustomDaytime.");
            return;
        }

        if (args.length != 1 || !RELOAD_ARGUMENT.equalsIgnoreCase(args[0])) {
            reply(sender, "§eUsage: /" + NAME + " reload");
            return;
        }

        scheduler.global().run(() -> {
            reloadService.reload();
            plugin.getLogger().info("CustomDaytime configuration reloaded by " + sender.getName() + ".");
            reply(sender, "§aCustomDaytime reloaded.");
        });
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source, String @NotNull [] args) {
        CommandSender sender = source.getSender();
        if (!sender.hasPermission(RELOAD_PERMISSION) || args.length != 1) {
            return List.of();
        }

        String query = args[0].toLowerCase();
        if (RELOAD_ARGUMENT.startsWith(query)) {
            return List.of(RELOAD_ARGUMENT);
        }
        return List.of();
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission(RELOAD_PERMISSION);
    }

    @Override
    public @Nullable String permission() {
        return RELOAD_PERMISSION;
    }

    private void reply(CommandSender sender, String message) {
        if (sender instanceof Entity entity) {
            scheduler.entity(entity).run(() -> sender.sendMessage(message));
            return;
        }
        scheduler.global().run(() -> sender.sendMessage(message));
    }
}
