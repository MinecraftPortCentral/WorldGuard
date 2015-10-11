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
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldguard.sponge.WorldConfiguration;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import com.sk89q.worldguard.sponge.event.DelegateEvent;
import com.sk89q.worldguard.sponge.event.block.BreakBlockEvent;
import com.sk89q.worldguard.sponge.event.block.PlaceBlockEvent;
import com.sk89q.worldguard.sponge.event.block.UseBlockEvent;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

/**
 * Handle events that need to be processed by the chest protection.
 */
public class ChestProtectionListener extends AbstractListener {

    /**
     * Construct the listener.
     *
     * @param plugin an instance of WorldGuardPlugin
     */
    public ChestProtectionListener(WorldGuardPlugin plugin) {
        super(plugin);
    }

    private void sendMessage(DelegateEvent event, Player player, Text message) {
        if (!event.isSilent()) {
            player.sendMessage(message);
        }
    }

    @Listener
    public void onPlaceBlock(final PlaceBlockEvent event) {
        final Player player = event.getCause().getFirstPlayer();

        if (player != null) {
            final WorldConfiguration wcfg = getWorldConfig(player);

            // Early guard
            if (!wcfg.signChestProtection) {
                return;
            }

            event.filter(target -> {
                if (wcfg.getChestProtection().isChest(event.getEffectiveMaterial()) && wcfg.isChestProtected(target, player)) {
                    sendMessage(event, player, Texts.of(TextColors.DARK_RED, "This spot is for a chest that you don't have permission for."));
                    return false;
                }

                return true;
            }, true);
        }
    }

    @Listener
    public void onBreakBlock(final BreakBlockEvent event) {
        final Player player = event.getCause().getFirstPlayer();

        final WorldConfiguration wcfg = getWorldConfig(event.getWorld());

        // Early guard
        if (!wcfg.signChestProtection) {
            return;
        }

        if (player != null) {
            event.filter(target -> {
                if (wcfg.isChestProtected(target, player)) {
                    sendMessage(event, player, Texts.of(TextColors.DARK_RED, "This chest is protected."));
                    return false;
                }

                return true;
            }, true);
        } else {
            event.filter(target -> !wcfg.isChestProtected(target));
        }
    }

    @Listener
    public void onUseBlock(final UseBlockEvent event) {
        final Player player = event.getCause().getFirstPlayer();

        final WorldConfiguration wcfg = getWorldConfig(event.getWorld());

        // Early guard
        if (!wcfg.signChestProtection) {
            return;
        }

        if (player != null) {
            event.filter(target -> {
                if (wcfg.isChestProtected(target, player)) {
                    sendMessage(event, player, Texts.of(TextColors.DARK_RED, "This chest is protected."));
                    return false;
                }

                return true;
            }, true);
        } else {
            event.filter(target -> !wcfg.isChestProtected(target));
        }
    }

    @Listener
    public void onSignChange(ChangeSignEvent event) {
        Optional<Player> optPlayer = event.getCause().first(Player.class);

        if (!optPlayer.isPresent()) {
            return;
        }

        Player player = optPlayer.get();
        WorldConfiguration wcfg = getWorldConfig(player);

        ListValue<Text> lines = event.getText().lines();
        if (wcfg.signChestProtection) {
            if (Texts.toPlain(lines.get(0)).equalsIgnoreCase("[Lock]")) {
                Location<World> targetLoc = event.getTargetTile().getLocation();
                BlockState targetBlock = event.getTargetTile().getBlock();

                if (wcfg.isChestProtectedPlacement(targetLoc, player)) {
                    player.sendMessage(Texts.of(TextColors.DARK_RED, "You do not own the adjacent chest."));

                    event.getBlock().breakNaturally();
                    event.setCancelled(true);
                    return;
                }

                if (targetBlock.getType() != BlockTypes.STANDING_SIGN) {
                    player.sendMessage(
                            Texts.of(TextColors.DARK_RED, "The [Lock] sign must be a sign post, not a wall sign.")
                    );

                    event.getBlock().breakNaturally();
                    event.setCancelled(true);
                    return;
                }

                if (!Texts.toPlain(lines.get(1)).equalsIgnoreCase(player.getName())) {
                    player.sendMessage(
                            Texts.of(TextColors.DARK_RED, "The first owner line must be your name.")
                    );

                    event.getBlock().breakNaturally();
                    event.setCancelled(true);
                    return;
                }

                int below = event.getBlock().getRelative(0, -1, 0).getTypeId();

                if (below == BlockID.TNT || below == BlockID.SAND
                        || below == BlockID.GRAVEL || below == BlockID.SIGN_POST) {
                    player.sendMessage(
                            Texts.of(TextColors.DARK_RED, "That is not a safe block that you're putting this sign on.")
                    );

                    event.getBlock().breakNaturally();
                    event.setCancelled(true);
                    return;
                }

                event.setLine(0, "[Lock]");
                player.sendMessage(Texts.of(TextColors.YELLOW, "A chest or double chest above is now protected."));
            }
        } else if (!wcfg.disableSignChestProtectionCheck) {
            if (Texts.toPlain(lines.get(0)).equalsIgnoreCase(Texts.of("[Lock]"))) {
                player.sendMessage(Texts.of(TextColors.DARK_RED, "WorldGuard's sign chest protection is disabled."));

                event.getBlock().breakNaturally();
                event.setCancelled(true);
            }
        }
    }

}
