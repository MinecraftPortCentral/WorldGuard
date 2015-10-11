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

package com.sk89q.worldguard.chest;

import java.util.Optional;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;

/**
 * Sign-based chest protection.
 * 
 * @author sk89q
 */
public class SignChestProtection implements ChestProtection {
    
    public boolean isProtected(Location block, Player player) {
        if (isChest(block.getBlockType())) {
            Location below = block.getRelative(Direction.DOWN);
            return isProtectedSignAround(below, player);
        } else if (block.getBlockType().equals(BlockTypes.STANDING_SIGN)) {
            return isProtectedSignAndChestBinary(block, player);
        } else {
            Location above = block.getRelative(Direction.UP);
            Boolean res = isProtectedSign(above, player);
            if (res != null) return res;
            return false;
        }
    }
    
    public boolean isProtectedPlacement(Location block, Player player) {
        return isProtectedSignAround(block, player);
    }
    
    private boolean isProtectedSignAround(Location searchBlock, Player player) {
        Location side;
        Boolean res;
        
        side = searchBlock;
        res = isProtectedSign(side, player);
        if (res != null && res) return res;
        
        side = searchBlock.getRelative(Direction.EAST);
        res = isProtectedSignAndChest(side, player);
        if (res != null && res) return res;
        
        side = searchBlock.getRelative(Direction.WEST);
        res = isProtectedSignAndChest(side, player);
        if (res != null && res) return res;
        
        side = searchBlock.getRelative(Direction.DOWN);
        res = isProtectedSignAndChest(side, player);
        if (res != null && res) return res;
        
        side = searchBlock.getRelative(Direction.UP);
        res = isProtectedSignAndChest(side, player);
        if (res != null && res) return res;
        
        return false;
    }
    
    private Boolean isProtectedSign(SignData sign, Player player) {
        if (Texts.toPlain(sign.lines().get(0)).equalsIgnoreCase("[Lock]")) {
            if (player == null) { // No player, no access
                return true;
            }
            
            String name = player.getName();
            if (name.equalsIgnoreCase(Texts.toPlain(sign.lines().get(1)))
                    || name.equalsIgnoreCase(Texts.toPlain(sign.lines().get(2)))
                    || name.equalsIgnoreCase(Texts.toPlain(sign.lines().get(3)))) {
                return false;
            }
            
            // No access!
            return true;
        }
        
        return null;
    }
    
    private Boolean isProtectedSign(Location block, Player player) {
        Optional<SignData> data = block.get(SignData.class);
        if (!data.isPresent()) {
            return null;
        }
        return isProtectedSign(data.get(), player);
    }
    
    private Boolean isProtectedSignAndChest(Location block, Player player) {
        if (!isChest(block.getRelative(Direction.UP).getBlockType())) {
            return null;
        }
        return isProtectedSign(block, player);
    }
    
    private boolean isProtectedSignAndChestBinary(Location block, Player player) {
        Boolean res = isProtectedSignAndChest(block, player);
        return !(res == null || !res);
    }

    public boolean isAdjacentChestProtected(Location searchBlock, Player player) {
        Location side;
        Boolean res;
        
        side = searchBlock;
        res = isProtected(side, player);
        if (res != null && res) return res;
        
        side = searchBlock.getRelative(Direction.WEST);
        res = isProtected(side, player);
        if (res != null && res) return res;
        
        side = searchBlock.getRelative(Direction.EAST);
        res = isProtected(side, player);
        if (res != null && res) return res;
        
        side = searchBlock.getRelative(Direction.DOWN);
        res = isProtected(side, player);
        if (res != null && res) return res;
        
        side = searchBlock.getRelative(Direction.UP);
        res = isProtected(side, player);
        if (res != null && res) return res;
        
        return false;
    }

    @Override
    public boolean isChest(BlockType type) {
        return type.equals(BlockTypes.CHEST)
                || type.equals(BlockTypes.DISPENSER)
                || type.equals(BlockTypes.FURNACE)
                || type.equals(BlockTypes.LIT_FURNACE);
    }
}
