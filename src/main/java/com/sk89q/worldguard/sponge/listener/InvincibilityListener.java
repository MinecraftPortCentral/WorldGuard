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

import org.spongepowered.api.data.key.Keys;

import com.sk89q.worldguard.sponge.WorldConfiguration;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DamageEntityEvent;

public class InvincibilityListener extends AbstractListener {

    /**
     * Construct the listener.
     *
     * @param plugin an instance of WorldGuardPlugin
     */
    public InvincibilityListener(WorldGuardPlugin plugin) {
        super(plugin);
    }

    /**
     * Test whether a player should be invincible.
     *
     * @param player The player
     * @return True if invincible
     */
    private boolean isInvincible(Player player) {
        return getPlugin().getSessionManager().get(player).isInvincible(player);
    }

    @Listener
    public void onEntityDamage(DamageEntityEvent event) {
        Entity victim = event.getTargetEntity();
        WorldConfiguration worldConfig = getPlugin().getGlobalStateManager().get(victim.getWorld());

        if (victim instanceof Player) {
            Player player = (Player) victim;

            if (isInvincible(player)) {
                player.setFireTicks(0);
                event.setCancelled(true);

                if (event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent byEntityEvent = (EntityDamageByEntityEvent) event;
                    Entity attacker = byEntityEvent.getDamager();

                    if (worldConfig.regionInvinciblityRemovesMobs
                            && attacker instanceof LivingEntity && !(attacker instanceof Player)
                            && !(attacker instanceof Tameable && ((Tameable) attacker).isTamed())) {
                        attacker.remove();
                    }
                }
            }
        }
    }

    @Listener
    public void onEntityCombust(EntityCombustEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Player) {
            Player player = (Player) entity;

            if (isInvincible(player)) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (event.getFoodLevel() < player.getFoodData().get(Keys.FOOD_LEVEL) && isInvincible(player)) {
                event.setCancelled(true);
            }
        }
    }

}
