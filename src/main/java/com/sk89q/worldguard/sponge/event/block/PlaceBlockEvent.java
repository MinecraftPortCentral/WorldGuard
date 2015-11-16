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

package com.sk89q.worldguard.sponge.event.block;

import com.sk89q.worldguard.sponge.cause.Cause;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;

import javax.annotation.Nullable;

/**
 * This event is an internal event. We do not recommend handling or throwing
 * this event or its subclasses as the interface is highly subject to change.
 *
 * <p>Thrown when a block is placed.</p>
 */
public class PlaceBlockEvent extends AbstractBlockEvent {

    public PlaceBlockEvent(@Nullable Event originalEvent, Cause cause, World world, List<Location<World>> blocks, BlockType effectiveMaterial) {
        super(originalEvent, cause, world, blocks, effectiveMaterial);
    }

    public PlaceBlockEvent(@Nullable Event originalEvent, Cause cause, Location block) {
        super(originalEvent, cause, block);
    }

    public PlaceBlockEvent(@Nullable Event originalEvent, Cause cause, Location target, BlockType effectiveMaterial) {
        super(originalEvent, cause, target, effectiveMaterial);
    }
}
