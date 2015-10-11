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

package com.sk89q.worldguard.sponge.listener;

import java.util.Optional;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.blacklist.event.*;
import com.sk89q.worldguard.sponge.ConfigurationManager;
import com.sk89q.worldguard.sponge.WorldConfiguration;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import com.sk89q.worldguard.sponge.event.block.BreakBlockEvent;
import com.sk89q.worldguard.sponge.event.block.PlaceBlockEvent;
import com.sk89q.worldguard.sponge.event.block.UseBlockEvent;
import com.sk89q.worldguard.sponge.event.entity.DestroyEntityEvent;
import com.sk89q.worldguard.sponge.event.entity.SpawnEntityEvent;
import com.sk89q.worldguard.sponge.event.inventory.UseItemEvent;
import com.sk89q.worldguard.sponge.util.Materials;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.inventory.DropItemStackEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackTransaction;

import static com.sk89q.worldguard.sponge.util.WorldEditTransforms.toVector;

/**
 * Handle events that need to be processed by the blacklist.
 */
public class BlacklistListener extends AbstractListener {

    /**
     * Construct the listener.
     *
     * @param plugin an instance of WorldGuardPlugin
     */
    public BlacklistListener(WorldGuardPlugin plugin) {
        super(plugin);
    }

    @Listener
    public void onBreakBlock(final BreakBlockEvent event) {
        final Player player = event.getCause().getFirstPlayer();

        if (player == null) {
            return;
        }

        final LocalPlayer localPlayer = getPlugin().wrapPlayer(player);
        final WorldConfiguration wcfg = getWorldConfig(player);

        // Blacklist guard
        if (wcfg.getBlacklist() == null) {
            return;
        }

        event.filter(target -> {
            if (!wcfg.getBlacklist().check(
                    new BlockBreakBlacklistEvent(localPlayer, toVector(target), createTarget(target.getBlock(), event.getEffectiveMaterial())), false, false)) {
                return false;
            } else if (!wcfg.getBlacklist().check(
                    new ItemDestroyWithBlacklistEvent(localPlayer, toVector(target), createTarget(player.getItemInHand())), false, false)) {
                return false;
            }

            return true;
        });
    }

    @Listener
    public void onPlaceBlock(final PlaceBlockEvent event) {
        Player player = event.getCause().getFirstPlayer();

        if (player == null) {
            return;
        }

        final LocalPlayer localPlayer = getPlugin().wrapPlayer(player);
        final WorldConfiguration wcfg = getWorldConfig(player);

        // Blacklist guard
        if (wcfg.getBlacklist() == null) {
            return;
        }

        event.filter(
                target -> wcfg.getBlacklist().check(
                    new BlockPlaceBlacklistEvent(
                            localPlayer,
                            toVector(target),
                            createTarget(target.getBlock(), event.getEffectiveMaterial())),
                    false,
                    false
                )
        );
    }

    @Listener
    public void onUseBlock(final UseBlockEvent event) {
        Player player = event.getCause().getFirstPlayer();

        if (player == null) {
            return;
        }

        final LocalPlayer localPlayer = getPlugin().wrapPlayer(player);
        final WorldConfiguration wcfg = getWorldConfig(player);

        // Blacklist guard
        if (wcfg.getBlacklist() == null) {
            return;
        }

        event.filter(
                target -> wcfg.getBlacklist().check(
                        new BlockInteractBlacklistEvent(
                                localPlayer,
                                toVector(target),
                                createTarget(target.getBlock(), event.getEffectiveMaterial())),
                        false,
                        false
                )
        );
    }

    @Listener
    public void onSpawnEntity(SpawnEntityEvent event) {
        Player player = event.getCause().getFirstPlayer();

        if (player == null) {
            return;
        }

        LocalPlayer localPlayer = getPlugin().wrapPlayer(player);
        WorldConfiguration wcfg = getWorldConfig(player);

        // Blacklist guard
        if (wcfg.getBlacklist() == null) {
            return;
        }

        Material material = Materials.getRelatedMaterial(event.getEffectiveType());
        if (material != null) {
            if (!wcfg.getBlacklist().check(new ItemUseBlacklistEvent(localPlayer, toVector(event.getTarget()), createTarget(material)), false, false)) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onDestroyEntity(DestroyEntityEvent event) {
        Player player = event.getCause().getFirstPlayer();

        if (player == null) {
            return;
        }

        LocalPlayer localPlayer = getPlugin().wrapPlayer(player);
        Entity target = event.getEntity();
        WorldConfiguration wcfg = getWorldConfig(player);

        // Blacklist guard
        if (wcfg.getBlacklist() == null) {
            return;
        }

        if (target instanceof Item) {
            Item item = (Item) target;
            if (!wcfg.getBlacklist().check(
                    new ItemAcquireBlacklistEvent(localPlayer,
                            toVector(target.getLocation()), createTarget(item.getItemStack())), false, true)) {
                event.setCancelled(true);
                return;
            }
        }

        Material material = Materials.getRelatedMaterial(target.getType());
        if (material != null) {
            // Not really a block but we only have one on-break blacklist event
            if (!wcfg.getBlacklist().check(new BlockBreakBlacklistEvent(localPlayer, toVector(event.getTarget()), createTarget(material)), false, false)) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onUseItem(UseItemEvent event) {
        Player player = event.getCause().getFirstPlayer();

        if (player == null) {
            return;
        }

        LocalPlayer localPlayer = getPlugin().wrapPlayer(player);
        ItemStack target = event.getItemStack();
        WorldConfiguration wcfg = getWorldConfig(player);

        // Blacklist guard
        if (wcfg.getBlacklist() == null) {
            return;
        }

        if (!wcfg.getBlacklist().check(new ItemUseBlacklistEvent(localPlayer, toVector(player.getLocation()), createTarget(target)), false, false)) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onPlayerDropItem(DropItemStackEvent.Pre event) {
        Optional<Player> optPlayer = event.getCause().first(Player.class);

        if (optPlayer.isPresent()) {
            Player player = optPlayer.get();

            ConfigurationManager cfg = getPlugin().getGlobalStateManager();
            WorldConfiguration wcfg = cfg.get(player.getWorld());

            if (wcfg.getBlacklist() != null) {
                // TODO we can't get location data from this
                for (ItemStackTransaction ta : event.getDroppedItems()) {
                    if (!wcfg.getBlacklist().check(
                            new ItemDropBlacklistEvent(getPlugin().wrapPlayer(player),
                                    toVector(ci.getLocation()), createTarget(ci.getItemStack())), false, false)) {
                        ta.setIsValid(false);
                    }
                }
            }
        }

        // TODO potentially handle block causes here
    }

    @Listener
    public void onBlockDispense(BlockDispenseEvent event) {
        ConfigurationManager cfg = getPlugin().getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(event.getBlock().getWorld());

        if (wcfg.getBlacklist() != null) {
            if (!wcfg.getBlacklist().check(new BlockDispenseBlacklistEvent(null, toVector(event.getBlock()), createTarget(event.getItem())), false, false)) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onInventoryClick(Inventory event) {
        HumanEntity entity = event.getWhoClicked();
        Inventory inventory = event.getInventory();
        ItemStack item = event.getCurrentItem();

        if (item != null && entity instanceof Player) {
            Player player = (Player) entity;
            ConfigurationManager cfg = getPlugin().getGlobalStateManager();
            WorldConfiguration wcfg = cfg.get(entity.getWorld());
            LocalPlayer localPlayer = getPlugin().wrapPlayer(player);

            if (wcfg.getBlacklist() != null && !wcfg.getBlacklist().check(
                    new ItemAcquireBlacklistEvent(localPlayer, toVector(entity.getLocation()), createTarget(item)), false, false)) {
                event.setCancelled(true);

                if (inventory.getHolder().equals(player)) {
                    event.setCurrentItem(null);
                }
            }
        }
    }

    @Listener
    public void onInventoryCreative(InventoryCreativeEvent event) {
        HumanEntity entity = event.getWhoClicked();
        ItemStack item = event.getCursor();

        if (item != null && entity instanceof Player) {
            Player player = (Player) entity;
            ConfigurationManager cfg = getPlugin().getGlobalStateManager();
            WorldConfiguration wcfg = cfg.get(entity.getWorld());
            LocalPlayer localPlayer = getPlugin().wrapPlayer(player);

            if (wcfg.getBlacklist() != null && !wcfg.getBlacklist().check(
                    new ItemAcquireBlacklistEvent(localPlayer, toVector(entity.getLocation()), createTarget(item)), false, false)) {
                event.setCancelled(true);
                event.setCursor(null);
            }
        }
    }

    @Listener
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Inventory inventory = player.getInventory();
        ItemStack item = inventory.getItem(event.getNewSlot());

        if (item != null) {
            ConfigurationManager cfg = getPlugin().getGlobalStateManager();
            WorldConfiguration wcfg = cfg.get(player.getWorld());
            LocalPlayer localPlayer = getPlugin().wrapPlayer(player);

            if (wcfg.getBlacklist() != null && !wcfg.getBlacklist().check(
                    new ItemAcquireBlacklistEvent(localPlayer, toVector(player.getLocation()), createTarget(item)), false, false)) {
                inventory.setItem(event.getNewSlot(), null);
            }
        }
    }

}
