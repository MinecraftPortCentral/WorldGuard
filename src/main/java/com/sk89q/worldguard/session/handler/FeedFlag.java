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

package com.sk89q.worldguard.session.handler;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.session.Session;
import org.spongepowered.api.data.manipulator.mutable.entity.FoodData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;

public class FeedFlag extends Handler {

    private long lastFeed = 0;

    public FeedFlag(Session session) {
        super(session);
    }

    @Override
    public void tick(Player player, ApplicableRegionSet set) {
        if (!getSession().isInvincible(player) && player.getGameModeData().type().get().equals(GameModes.SURVIVAL)) {
            long now = System.currentTimeMillis();

            LocalPlayer localPlayer = getPlugin().wrapPlayer(player);
            Integer feedAmount = set.queryValue(localPlayer, DefaultFlag.FEED_AMOUNT);
            Integer feedDelay = set.queryValue(localPlayer, DefaultFlag.FEED_DELAY);
            Integer minHunger = set.queryValue(localPlayer, DefaultFlag.MIN_FOOD);
            Integer maxHunger = set.queryValue(localPlayer, DefaultFlag.MAX_FOOD);

            FoodData data = player.get(FoodData.class).orElse(null);
            if (data == null) return;
            if (feedAmount == null || feedDelay == null || feedAmount == 0 || feedDelay < 0) {
                return;
            }
            if (minHunger == null) {
                minHunger = data.foodLevel().getMinValue();
            }
            if (maxHunger == null) {
                maxHunger = data.foodLevel().getMaxValue();
            }

            if (data.foodLevel().get() >= maxHunger && feedAmount > 0) {
                return;
            }

            if (feedDelay <= 0) {
                data.foodLevel().set(feedAmount > 0 ? maxHunger : minHunger);
                data.saturation().set(feedAmount > 0 ? data.saturation().getMaxValue() : data.saturation().getMinValue());
                data.exhaustion().set(feedAmount > 0 ? data.exhaustion().getMaxValue() : data.exhaustion().getMinValue());
                lastFeed = now;
            } else if (now - lastFeed > feedDelay * 1000) {
                // clamp health between minimum and maximum
                data.foodLevel().set(Math.min(maxHunger, Math.max(minHunger, data.foodLevel().get() + feedAmount)));
                data.saturation().set(feedAmount > 0 ? data.saturation().getMaxValue() : data.saturation().getMinValue());
                data.exhaustion().set(feedAmount > 0 ? data.exhaustion().getMaxValue() : data.exhaustion().getMinValue());
                lastFeed = now;
            }
            player.offer(data);
        }
    }

}
