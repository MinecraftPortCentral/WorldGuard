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
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;

public class LoggingBlockPlaceEvent extends AbstractEvent implements ChangeBlockEvent.Place, CancelLogging {

    private final CancelLogger logger = new CancelLogger();

    public LoggingBlockPlaceEvent(Location placedBlock, Direction placed, BlockSnapshot replacedBlockState, Player player) {
        this.replacement = replacedBlockState;
        this.direction = placed;
        this.block = placedBlock;
        this.state = placedBlock.getBlock();
        this.cause = Cause.of(player);
        this.game = WorldGuardPlugin.inst().getGame();
    }

    private boolean cancelled;
    private BlockSnapshot replacement;
    private Location block;
    private BlockState state;
    private Cause cause;
    private Game game;
    private Direction direction;

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
    public List<Transaction<BlockSnapshot>> getTransactions() {
        return null;
    }

    @Override
    public World getTargetWorld() {
        return null;
    }
}
