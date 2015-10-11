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
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import com.sk89q.worldguard.sponge.util.Locations;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MountEntityEvent;
import org.spongepowered.api.event.entity.living.player.RespawnPlayerEvent;

public class PlayerMoveListener {

    private final WorldGuardPlugin plugin;

    public PlayerMoveListener(WorldGuardPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerEvents() {

        if (plugin.getGlobalStateManager().usePlayerMove) {
            plugin.getGame().getEventManager().registerListeners(plugin, this);
        }
    }

    @Listener
    public void onPlayerRespawn(RespawnPlayerEvent event) {
        Player player = event.getTargetEntity();

        Session session = plugin.getSessionManager().get(player);
        session.testMoveTo(player, event.getToTransform(), MoveType.RESPAWN, true);
    }

    @Listener
    public void onVehicleEnter(MountEntityEvent event) {
        Entity entity = event.getTargetEntity();
        Optional<Player> optPlayer = event.getCause().first(Player.class);
        if (optPlayer.isPresent()) {
            Player player = optPlayer.get();
            Session session = plugin.getSessionManager().get(player);
            if (null != session.testMoveTo(player, Locations.toTransform(entity.getLocation()), MoveType.EMBARK, true)) {
                event.setCancelled(true);
            }
        }
    }

    /*
    @Listener(order = Order.LAST)
    public void onPlayerMove(PlayerMoveEvent event) {
        final Player player = event.getPlayer();

        Session session = plugin.getSessionManager().get(player);
        final Location override = session.testMoveTo(player, event.getTo(), MoveType.MOVE);

        if (override != null) {
            override.setX(override.getBlockX() + 0.5);
            override.setY(override.getBlockY());
            override.setZ(override.getBlockZ() + 0.5);
            override.setPitch(event.getTo().getPitch());
            override.setYaw(event.getTo().getYaw());

            event.setTo(override.clone());

            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                vehicle.eject();

                Entity current = vehicle;
                while (current != null) {
                    current.eject();
                    vehicle.setVelocity(new Vector());
                    if (vehicle instanceof LivingEntity) {
                        vehicle.teleport(override.clone());
                    } else {
                        vehicle.teleport(override.clone().add(0, 1, 0));
                    }
                    current = current.getVehicle();
                }

                player.teleport(override.clone().add(0, 1, 0));

                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        player.teleport(override.clone().add(0, 1, 0));
                    }
                }, 1);
            }
        }
    }
    */

}
