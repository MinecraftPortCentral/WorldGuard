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

package com.sk89q.worldguard.blacklist.target;

import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.item.ItemType;

import java.util.regex.Pattern;

public class TargetMatcherParser {

    private static final Pattern RANGE_PATTERN = Pattern.compile("^([0-9]+)\\s*-\\s*([0-9]+)$");

    public TargetMatcher fromInput(String input) throws TargetMatcherParseException {
        input = input.trim();
        final BlockType material = WorldGuardPlugin.inst().getGame().getRegistry().getType(BlockType.class, input).orElse(null);
        if (material != null) {
            return new BlockTargetMatcher(material);
        }
        final ItemType itemType = WorldGuardPlugin.inst().getGame().getRegistry().getType(ItemType.class, input).orElse(null);
        if (itemType != null) {
            return new ItemTargetMatcher(itemType);
        }
        throw new TargetMatcherParseException("Unknown block or item name: " + input);
    }

    private static final class BlockTargetMatcher implements TargetMatcher {

        private BlockTargetMatcher(BlockType material) {
            this.material = material;
        }

        private BlockType material;

        @Override
        public int getMatchedTypeId() {
            return material.getDefaultState().hashCode();
        }

        @Override
        public boolean test(Target target) {
            if (target instanceof BlockTarget) {
                return ((BlockTarget) target).getState().getType().equals(material);
            }
            return false;
        }
    }

    private static final class ItemTargetMatcher implements TargetMatcher {

        private ItemTargetMatcher(ItemType itemType) {
            this.itemType = itemType;
        }

        private ItemType itemType;

        @Override
        public int getMatchedTypeId() {
            return itemType.hashCode();
        }

        @Override
        public boolean test(Target target) {
            if (target instanceof ItemTarget) {
                return ((ItemTarget) target).getStack().getItem().equals(itemType);
            }
            return false;
        }
    }
}
