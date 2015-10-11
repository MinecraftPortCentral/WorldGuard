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

package com.sk89q.worldguard.sponge.event.debug;

import java.util.Optional;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.HealthData;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.AbstractEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.living.LivingChangeHealthEvent;

import java.util.List;

public class LoggingEntityDamageByEntityEvent extends AbstractEvent implements LivingChangeHealthEvent, CancelLogging {

    private final CancelLogger logger = new CancelLogger();

    public LoggingEntityDamageByEntityEvent(Living damagee, Cause cause, double damage) {
        this.damagee = damagee;
        this.cause = cause;
        this.oldData = damagee.getHealthData();
        this.newData = damagee.getHealthData().set(Keys.HEALTH, getOldData().get(Keys.HEALTH).get() - damage);
        this.game = WorldGuardPlugin.inst().getGame();
    }

    private HealthData oldData;
    private HealthData newData;
    private Cause cause;
    private Living damagee;
    private boolean cancelled;
    private Game game;

    public List<CancelAttempt> getCancels() {
        return logger.getCancels();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.logger.log(isCancelled(), cancel, new Exception().getStackTrace());
        this.cancelled = cancel;
    }

    @Override
    public HealthData getOldData() {
        return oldData.copy();
    }

    @Override
    public HealthData getNewData() {
        return newData.copy();
    }

    @Override
    public void setNewData(HealthData newData) {
        this.newData = newData;
    }

    @Override
    public Optional<Cause> getCause() {
        return Optional.fromNullable(cause);
    }

    @Override
    public Living getEntity() {
        return damagee;
    }

    @Override
    public Game getGame() {
        return game;
    }
}
