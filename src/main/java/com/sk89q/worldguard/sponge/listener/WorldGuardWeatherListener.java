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

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.sponge.WorldConfiguration;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import com.sk89q.worldguard.sponge.internal.TargetMatcherSet;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.weather.Lightning;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.world.ChangeWorldWeatherEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.weather.Weathers;

public class WorldGuardWeatherListener extends AbstractListener {

    /**
     * Construct the object;
     *
     * @param plugin The plugin instance
     */
    public WorldGuardWeatherListener(WorldGuardPlugin plugin) {
        super(plugin);
    }

    @Listener
    public void onWeatherChange(ChangeWorldWeatherEvent event) {
        WorldConfiguration wcfg = getWorldConfig((World) event.getTargetWorld());

        if (event.getWeather().equals(Weathers.RAIN)) {
            if (wcfg.disableWeather) {
                event.setWeather(event.getInitialWeather());
            }
        } else {
            if (!wcfg.disableWeather && wcfg.alwaysRaining) {
                event.setWeather(event.getInitialWeather());
            }
        }
    }

    @Listener
    public void onLightningStrike(SpawnEntityEvent event) {
        for (Entity entity : event.getEntities()) {
            if (!(entity instanceof Lightning))
                return;
            WorldConfiguration wcfg = getWorldConfig(entity.getWorld());

            final TargetMatcherSet matcherSet = wcfg.disallowedLightningBlocks;

            if (wcfg.useRegions) {
                Location<World> loc = entity.getLocation();
                ApplicableRegionSet set = getPlugin().getRegionContainer().createQuery().getApplicableRegions(loc);

                if (!set.testState(null, DefaultFlag.LIGHTNING)) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
