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

import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import com.sk89q.worldguard.sponge.event.block.BreakBlockEvent;
import com.sk89q.worldguard.sponge.event.block.PlaceBlockEvent;
import com.sk89q.worldguard.sponge.event.block.UseBlockEvent;
import com.sk89q.worldguard.sponge.event.entity.DamageEntityEvent;
import com.sk89q.worldguard.sponge.event.entity.DestroyEntityEvent;
import com.sk89q.worldguard.sponge.event.entity.SpawnEntityEvent;
import com.sk89q.worldguard.sponge.event.entity.UseEntityEvent;
import com.sk89q.worldguard.sponge.event.inventory.UseItemEvent;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.World;

public class BuildPermissionListener extends AbstractListener {

    /**
     * Construct the listener.
     *
     * @param plugin an instance of WorldGuardPlugin
     */
    public BuildPermissionListener(WorldGuardPlugin plugin) {
        super(plugin);
    }

    private boolean hasBuildPermission(CommandSource sender, String perm) {
        return getPlugin().hasPermission(sender, "worldguard.build." + perm);
    }

    private void tellErrorMessage(CommandSource sender, World world) {
        Text message = getWorldConfig(world).buildPermissionDenyMessage;
        // TODO this seems unnecessary
        // if (!message.isEmpty()) {
            sender.sendMessage(message);
        // }
    }

    @Listener
    public void onPlaceBlock(final PlaceBlockEvent event) {
        if (!getWorldConfig(event.getWorld()).buildPermissions) return;

        Object rootCause = event.getCause().getRootCause();

        if (rootCause instanceof Player) {
            final Player player = (Player) rootCause;
            final BlockType type = event.getEffectiveMaterial();

            if (!hasBuildPermission(player, "block." + type.getId().toLowerCase() + ".place")
                    && !hasBuildPermission(player, "block.place." + type.getId().toLowerCase())) {
                tellErrorMessage(player, event.getWorld());
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onBreakBlock(final BreakBlockEvent event) {
        if (!getWorldConfig(event.getWorld()).buildPermissions) return;

        Object rootCause = event.getCause().getRootCause();

        if (rootCause instanceof Player) {
            final Player player = (Player) rootCause;
            final BlockType type = event.getEffectiveMaterial();

            if (!hasBuildPermission(player, "block." + type.getId().toLowerCase() + ".remove")
                    && !hasBuildPermission(player, "block.remove." + type.getId().toLowerCase())) {
                tellErrorMessage(player, event.getWorld());
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onUseBlock(final UseBlockEvent event) {
        if (!getWorldConfig(event.getWorld()).buildPermissions) return;

        Object rootCause = event.getCause().getRootCause();

        if (rootCause instanceof Player) {
            final Player player = (Player) rootCause;
            final BlockType type = event.getEffectiveMaterial();

            if (!hasBuildPermission(player, "block." + type.getId().toLowerCase() + ".interact")
                    && !hasBuildPermission(player, "block.interact." + type.getId().toLowerCase())) {
                tellErrorMessage(player, event.getWorld());
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onSpawnEntity(SpawnEntityEvent event) {
        if (!getWorldConfig(event.getWorld()).buildPermissions) return;

        Object rootCause = event.getCause().getRootCause();

        if (rootCause instanceof Player) {
            final Player player = (Player) rootCause;
            final EntityType type = event.getEffectiveType();

            if (!hasBuildPermission(player, "entity." + type.getId().toLowerCase() + ".place")
                    && !hasBuildPermission(player, "entity.place." + type.getId().toLowerCase())) {
                tellErrorMessage(player, event.getWorld());
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onDestroyEntity(DestroyEntityEvent event) {
        if (!getWorldConfig(event.getWorld()).buildPermissions) return;

        Object rootCause = event.getCause().getRootCause();

        if (rootCause instanceof Player) {
            final Player player = (Player) rootCause;
            final EntityType type = event.getEntity().getType();

            if (!hasBuildPermission(player, "entity." + type.getId().toLowerCase() + ".remove")
                    && !hasBuildPermission(player, "entity.remove." + type.getId().toLowerCase())) {
                tellErrorMessage(player, event.getWorld());
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onUseEntity(UseEntityEvent event) {
        if (!getWorldConfig(event.getWorld()).buildPermissions) return;

        Object rootCause = event.getCause().getRootCause();

        if (rootCause instanceof Player) {
            final Player player = (Player) rootCause;
            final EntityType type = event.getEntity().getType();

            if (!hasBuildPermission(player, "entity." + type.getId().toLowerCase() + ".interact")
                    && !hasBuildPermission(player, "entity.interact." + type.getId().toLowerCase())) {
                tellErrorMessage(player, event.getWorld());
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onDamageEntity(DamageEntityEvent event) {
        if (!getWorldConfig(event.getWorld()).buildPermissions) return;

        Object rootCause = event.getCause().getRootCause();

        if (rootCause instanceof Player) {
            final Player player = (Player) rootCause;
            final EntityType type = event.getEntity().getType();

            if (!hasBuildPermission(player, "entity." + type.getId().toLowerCase() + ".damage")
                    && !hasBuildPermission(player, "entity.damage." + type.getId().toLowerCase())) {
                tellErrorMessage(player, event.getWorld());
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onUseItem(UseItemEvent event) {
        if (!getWorldConfig(event.getWorld()).buildPermissions) return;

        Object rootCause = event.getCause().getRootCause();

        if (rootCause instanceof Player) {
            Player player = (Player) rootCause;
            ItemType type = event.getItemStack().getItem();

            if (type.getBlock().isPresent()) {
                return;
            }

            if (!hasBuildPermission(player, "item." + type.getId().toLowerCase() + ".use")
                    && !hasBuildPermission(player, "item.use." + type.getId().toLowerCase())) {
                tellErrorMessage(player, event.getWorld());
                event.setCancelled(true);
            }
        }
    }

}
