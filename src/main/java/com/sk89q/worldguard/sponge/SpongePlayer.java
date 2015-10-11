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

package com.sk89q.worldguard.sponge;

import com.flowpowered.math.vector.Vector3d;
import java.util.Optional;
import com.sk89q.worldguard.LocalPlayer;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.ban.Bans;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public class SpongePlayer extends LocalPlayer {

    private final WorldGuardPlugin plugin;
    private final Player player;
    private final String name;
    private final boolean silenced;

    public SpongePlayer(WorldGuardPlugin plugin, Player player) {
        this(plugin, player, false);
    }

    SpongePlayer(WorldGuardPlugin plugin, Player player, boolean silenced) {
        checkNotNull(plugin);
        checkNotNull(player);

        this.plugin = plugin;
        this.player = player;
        // getName() takes longer than before in newer versions of Minecraft
        this.name = player.getName();
        this.silenced = silenced;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public boolean hasGroup(String group) {
        return plugin.inGroup(player, group);
    }

    @Override
    public Vector3d getPosition() {
        return player.getLocation().getPosition();
    }

    @Override
    public void kick(String msg) {
        if (!silenced) {
            player.kick(Texts.of(msg));
        }
    }

    @Override
    public void ban(String msg) {
        if (!silenced) {
            Optional<BanService> service = plugin.getGame().getServiceManager().provide(BanService.class);
            if (service.isPresent()) {
                service.get().ban(Bans.builder().user(player).reason(Texts.of(msg)).build());
            }
            player.kick(Texts.of(msg));
        }
    }

    @Override
    public String[] getGroups() {
        return plugin.getGroups(player);
    }

    @Override
    public void printRaw(String msg) {
        if (!silenced) {
            player.sendMessage(Texts.of(msg));
        }
    }

    @Override
    public boolean hasPermission(String perm) {
        return plugin.hasPermission(player, perm);
    }

    public Player getPlayer() {
        return player;
    }

}
