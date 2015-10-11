/*
 * WorldGuard, a suite of tools for Minecraft
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldGuard team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldguard.sponge.commands;

import java.util.Optional;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.GodMode;
import com.sk89q.worldguard.sponge.ConfigurationManager;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.FoodData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandSource;

public class GeneralCommands {
    private final WorldGuardPlugin plugin;

    public GeneralCommands(WorldGuardPlugin plugin) {
        this.plugin = plugin;
    }
    
    @SuppressWarnings("deprecation")
    @Command(aliases = {"god"}, usage = "[player]",
            desc = "Enable godmode on a player", flags = "s", max = 1)
    public void god(CommandContext args, CommandSource sender) throws CommandException {
        ConfigurationManager config = plugin.getGlobalStateManager();
        
        Iterable<? extends Player> targets = null;
        boolean included = false;
        
        // Detect arguments based on the number of arguments provided
        if (args.argsLength() == 0) {
            targets = plugin.matchPlayers(plugin.checkPlayer(sender));
            
            // Check permissions!
            plugin.checkPermission(sender, "worldguard.god");
        } else {
            targets = plugin.matchPlayers(sender, args.getString(0));
            
            // Check permissions!
            plugin.checkPermission(sender, "worldguard.god.other");
        }

        for (Player player : targets) {
            Session session = plugin.getSessionManager().get(player);

            if (GodMode.set(player, session, true)) {
                // TODO sponge
                //player.setFireTicks(0);

                // Tell the user
                if (player.equals(sender)) {
                    player.sendMessage(Texts.of(TextColors.YELLOW, "God mode enabled! Use /ungod to disable."));

                    // Keep track of this
                    included = true;
                } else {
                    player.sendMessage(Texts.of(TextColors.YELLOW, "God enabled by " + plugin.toName(sender) + "."));

                }
            }
        }
        
        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included && args.hasFlag('s')) {
            sender.sendMessage(Texts.of(TextColors.YELLOW, "Players now have god mode."));
        }
    }
    
    @SuppressWarnings("deprecation")
    @Command(aliases = {"ungod"}, usage = "[player]",
            desc = "Disable godmode on a player", flags = "s", max = 1)
    public void ungod(CommandContext args, CommandSource sender) throws CommandException {
        Iterable<? extends Player> targets = null;
        boolean included = false;
        
        // Detect arguments based on the number of arguments provided
        if (args.argsLength() == 0) {
            targets = plugin.matchPlayers(plugin.checkPlayer(sender));
            
            // Check permissions!
            plugin.checkPermission(sender, "worldguard.god");
        } else {
            targets = plugin.matchPlayers(sender, args.getString(0));
            
            // Check permissions!
            plugin.checkPermission(sender, "worldguard.god.other");
        }

        for (Player player : targets) {
            Session session = plugin.getSessionManager().get(player);

            if (GodMode.set(player, session, false)) {
                // Tell the user
                if (player.equals(sender)) {
                    player.sendMessage(Texts.of(TextColors.YELLOW, "God mode disabled!"));

                    // Keep track of this
                    included = true;
                } else {
                    player.sendMessage(Texts.of(TextColors.YELLOW, "God disabled by " + plugin.toName(sender) + "."));

                }
            }
        }
        
        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included && args.hasFlag('s')) {
            sender.sendMessage(Texts.of(TextColors.YELLOW, "Players no longer have god mode."));
        }
    }
    
    @Command(aliases = {"heal"}, usage = "[player]", desc = "Heal a player", flags = "s", max = 1)
    public void heal(CommandContext args, CommandSource sender) throws CommandException {

        Iterable<? extends Player> targets = null;
        boolean included = false;
        
        // Detect arguments based on the number of arguments provided
        if (args.argsLength() == 0) {
            targets = plugin.matchPlayers(plugin.checkPlayer(sender));
            
            // Check permissions!
            plugin.checkPermission(sender, "worldguard.heal");
        } else if (args.argsLength() == 1) {            
            targets = plugin.matchPlayers(sender, args.getString(0));
            
            // Check permissions!
            plugin.checkPermission(sender, "worldguard.heal.other");
        }

        for (Player player : targets) {
            player.offer(Keys.HEALTH, player.get(Keys.MAX_HEALTH).get());
            Optional<FoodData> data = player.get(FoodData.class);
            if (data.isPresent()) {
                FoodData fd = data.get();
                fd.exhaustion().set(fd.exhaustion().getMaxValue());
                fd.foodLevel().set(fd.foodLevel().getMaxValue());
                fd.saturation().set(fd.saturation().getMaxValue());
                player.offer(fd);
            }

            // Tell the user
            if (player.equals(sender)) {
                player.sendMessage(Texts.of(TextColors.YELLOW, "Healed!"));
                
                // Keep track of this
                included = true;
            } else {
                player.sendMessage(Texts.of(TextColors.YELLOW, "Healed by " + plugin.toName(sender) + "."));
                
            }
        }
        
        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included && args.hasFlag('s')) {
            sender.sendMessage(Texts.of(TextColors.YELLOW, "Players healed."));
        }
    }
    
    @Command(aliases = {"slay"}, usage = "[player]", desc = "Slay a player", flags = "s", max = 1)
    public void slay(CommandContext args, CommandSource sender) throws CommandException {
        
        Iterable<? extends Player> targets = null;
        boolean included = false;
        
        // Detect arguments based on the number of arguments provided
        if (args.argsLength() == 0) {
            targets = plugin.matchPlayers(plugin.checkPlayer(sender));
            
            // Check permissions!
            plugin.checkPermission(sender, "worldguard.slay");
        } else if (args.argsLength() == 1) {            
            targets = plugin.matchPlayers(sender, args.getString(0));
            
            // Check permissions!
            plugin.checkPermission(sender, "worldguard.slay.other");
        }

        for (Player player : targets) {
            player.offer(player.getHealthData().health().set(0D));
            
            // Tell the user
            if (player.equals(sender)) {
                player.sendMessage(Texts.of(TextColors.YELLOW,  "Slain!"));
                
                // Keep track of this
                included = true;
            } else {
                player.sendMessage(Texts.of(TextColors.YELLOW, "Slain by "
                        + plugin.toName(sender) + "."));
                
            }
        }
        
        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included && args.hasFlag('s')) {
            sender.sendMessage(Texts.of(TextColors.YELLOW,  "Players slain."));
        }
    }
    
    @Command(aliases = {"locate"}, usage = "[player]", desc = "Locate a player", max = 1)
    @CommandPermissions({"worldguard.locate"})
    public void locate(CommandContext args, CommandSource sender) throws CommandException {
        
        Player player = plugin.checkPlayer(sender);
        
        if (args.argsLength() == 0) {
            // TODO sponge
            //player.setCompassTarget(player.getWorld().getSpawnLocation());
            
            sender.sendMessage(Texts.of(TextColors.YELLOW,  "Compass reset to spawn."));
        } else {
            Player target = plugin.matchSinglePlayer(sender, args.getString(0));
            // TODO sponge
            // player.setCompassTarget(target.getLocation());
            
            sender.sendMessage(Texts.of(TextColors.YELLOW,  "Compass repointed."));
        }
    }
    
    @Command(aliases = {"stack", ";"}, usage = "", desc = "Stack items", max = 0)
    @CommandPermissions({"worldguard.stack"})
    public void stack(CommandContext args, CommandSource sender) throws CommandException {
        
        Player player = plugin.checkPlayer(sender);
        boolean ignoreMax = plugin.hasPermission(player, "worldguard.stack.illegitimate");
        boolean ignoreDamaged = plugin.hasPermission(player, "worldguard.stack.damaged");

        // TODO idk iterate stuff
        /*ItemStack[] items = player.getInventory();
        int len = items.length;

        int affected = 0;
        
        for (int i = 0; i < len; i++) {
            ItemStack item = items[i];

            // Avoid infinite stacks and stacks with durability
            if (item == null || item.getAmount() <= 0
                    || (!ignoreMax && item.getMaxStackSize() == 1)) {
                continue;
            }

            int max = ignoreMax ? 64 : item.getMaxStackSize();

            if (item.getAmount() < max) {
                int needed = max - item.getAmount(); // Number of needed items until max

                // Find another stack of the same type
                for (int j = i + 1; j < len; j++) {
                    ItemStack item2 = items[j];

                    // Avoid infinite stacks and stacks with durability
                    if (item2 == null || item2.getAmount() <= 0
                            || (!ignoreMax && item.getMaxStackSize() == 1)) {
                        continue;
                    }

                    // Same type?
                    // Blocks store their color in the damage value
                    if (item2.getTypeId() == item.getTypeId() &&
                            ((!ItemType.usesDamageValue(item.getTypeId()) && ignoreDamaged)
                                    || item.getDurability() == item2.getDurability()) &&
                                    ((item.getItemMeta() == null && item2.getItemMeta() == null)
                                            || (item.getItemMeta() != null &&
                                                item.getItemMeta().equals(item2.getItemMeta())))) {
                        // This stack won't fit in the parent stack
                        if (item2.getAmount() > needed) {
                            item.setAmount(max);
                            item2.setAmount(item2.getAmount() - needed);
                            break;
                        // This stack will
                        } else {
                            items[j] = null;
                            item.setAmount(item.getAmount() + item2.getAmount());
                            needed = max - item.getAmount();
                        }

                        affected++;
                    }
                }
            }
        }

        if (affected > 0) {
            player.getInventory().setContents(items);
        }

        player.sendMessage(ChatColor.YELLOW + "Items compacted into stacks!");*/
    }
}
