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

package com.sk89q.worldguard.sponge.internal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sk89q.worldguard.blacklist.target.BlockTarget;
import com.sk89q.worldguard.blacklist.target.ItemTarget;
import com.sk89q.worldguard.blacklist.target.Target;
import com.sk89q.worldguard.blacklist.target.TargetMatcher;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.item.ItemType;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public class TargetMatcherSet {

    private final Multimap<Integer, TargetMatcher> entries = HashMultimap.create();

    public boolean add(TargetMatcher matcher) {
        checkNotNull(matcher);
        return entries.put(matcher.getMatchedTypeId(), matcher);
    }

    public boolean test(Target target) {
        Collection<TargetMatcher> matchers = entries.get(target.getTypeId());

        for (TargetMatcher matcher : matchers) {
            if (matcher.test(target)) {
                return true;
            }
        }

        return false;
    }

    public boolean test(BlockType type) {
        return test(new BlockTarget(type.getDefaultState()));
    }
    public boolean test(BlockState state) {
        return test(new BlockTarget(state));
    }

    public boolean test(ItemType type) {
        return test(new ItemTarget(WorldGuardPlugin.inst().getGame().getRegistry().createItemBuilder().itemType(type).quantity(1).build()));
    }

    @Override
    public String toString() {
        return entries.toString();
    }

}
