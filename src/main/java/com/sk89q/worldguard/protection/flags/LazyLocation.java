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

package com.sk89q.worldguard.protection.flags;

import com.flowpowered.math.vector.Vector3d;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * A location that stores the name of the world in case the world is unloaded.
 */
public class LazyLocation {

    private String worldName;
    private Vector3d position;
    private Vector3d rotation;

    @Nullable
    private static World findWorld(String worldName) {
        Optional<World> world = WorldGuardPlugin.inst().getGame().getServer().getWorld(worldName);
        return world.isPresent() ? world.get() : null;
    }

    public LazyLocation(String worldName, Vector3d position, Vector3d rotation) {
        this(worldName, position);
        this.rotation = rotation;
    }

    public LazyLocation(String worldName, Vector3d position) {
        this.worldName = worldName;
        this.position = position;
    }

    public String getWorldName() {
        return worldName;
    }

    public Vector3d getPosition() {
        return position;
    }

    public Vector3d getRotation() {
        return rotation;
    }

    public Location<World> getLocation() {
        World world = findWorld(worldName);
        if (world == null) return null;
        return new Location<World>(world, position);
    }

    public Transform<World> getTransform() {
        World world = findWorld(worldName);
        if (world == null) return null;
        return new Transform<World>(world, rotation);
    }
}
