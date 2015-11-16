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

import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.util.Tuple;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class LoggingEntityDamageByEntityEvent extends AbstractEvent implements DamageEntityEvent, CancelLogging {

    private final CancelLogger logger = new CancelLogger();

    public LoggingEntityDamageByEntityEvent(Living damagee, Cause cause, double damage) {
        this.damagee = damagee;
        this.cause = cause;
        this.game = WorldGuardPlugin.inst().getGame();
    }

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
    public Cause getCause() {
        return cause;
    }

    @Override
    public Game getGame() {
        return game;
    }

    @Override
    public Entity getTargetEntity() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getBaseDamage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getDamage(DamageModifier arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getFinalDamage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<Tuple<DamageModifier, Function<? super Double, Double>>> getModifiers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getOriginalDamage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Map<DamageModifier, Double> getOriginalDamages() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getOriginalFinalDamage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<Tuple<DamageModifier, Function<? super Double, Double>>> getOriginalFunctions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getOriginalModifierDamage(DamageModifier arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isModifierApplicable(DamageModifier arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setBaseDamage(double arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDamage(DamageModifier arg0, Function<? super Double, Double> arg1) {
        // TODO Auto-generated method stub
        
    }
}
