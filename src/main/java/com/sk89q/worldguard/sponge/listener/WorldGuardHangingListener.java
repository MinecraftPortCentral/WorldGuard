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

import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.sponge.ConfigurationManager;
import com.sk89q.worldguard.sponge.WorldConfiguration;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.hanging.ItemFrame;
import org.spongepowered.api.entity.hanging.Painting;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.monster.Creeper;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.world.World;

public class WorldGuardHangingListener extends AbstractListener {

    /**
     * Construct the object;
     *
     * @param plugin The plugin instance
     */
    public WorldGuardHangingListener(WorldGuardPlugin plugin) {
        super(plugin);
    }

    @Listener
    public void onHangingBreak(DamageEntityEvent event) {
        if (!(event.getTargetEntity() instanceof Hanging)) return;
        Hanging hanging = ((Hanging) event.getTargetEntity());
        World world = hanging.getWorld();
        ConfigurationManager cfg = getPlugin().getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(world);

        Entity remover;
        if ((remover = event.getCause().first(Entity.class).orElse(null)) != null) {
            if (remover instanceof Projectile) {
                Projectile projectile = (Projectile) remover;
                ProjectileSource source = projectile.getShooter();
                remover = (source instanceof Living ? (Living) source : null);
            }

            if (!(remover instanceof Player)) {
                if (remover instanceof Creeper) {
                    if (wcfg.blockCreeperBlockDamage || wcfg.blockCreeperExplosions) {
                        event.setCancelled(true);
                        return;
                    }
                    if (wcfg.useRegions && !getPlugin().getRegionContainer().createQuery()
                            .testState(hanging.getLocation(), (RegionAssociable) null, DefaultFlag.CREEPER_EXPLOSION)) {
                        event.setCancelled(true);
                        return;
                    }
                }

                // this now covers dispensers as well, if removerEntity is null above,
                // due to a non-LivingEntity ProjectileSource
                if (hanging instanceof Painting
                        && (wcfg.blockEntityPaintingDestroy
                        || (wcfg.useRegions
                        && !getPlugin().getRegionContainer().createQuery()
                                .testState(hanging.getLocation(), (RegionAssociable) null, DefaultFlag.ENTITY_PAINTING_DESTROY)))) {
                    event.setCancelled(true);
                } else if (hanging instanceof ItemFrame
                        && (wcfg.blockEntityItemFrameDestroy
                        || (wcfg.useRegions
                        && !getPlugin().getRegionContainer().createQuery()
                            .testState(hanging.getLocation(), (RegionAssociable) null, DefaultFlag.ENTITY_ITEM_FRAME_DESTROY)))) {
                    event.setCancelled(true);
                }
            }
        }
    }

}
