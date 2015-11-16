package com.sk89q.worldguard.sponge.util;

import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.Optional;

public class Items {
    public static Optional<ItemStack> toItemStack(BlockType type) {
        Optional<ItemType> optHeld = type.getItem();
        if (optHeld.isPresent()) {
            ItemStack is = WorldGuardPlugin.inst().getGame().getRegistry().createItemBuilder().itemType(optHeld.get()).build();
            return Optional.of(is);
        }
        return Optional.empty();
    }

    public static Optional<ItemStack> toItemStack(BlockType type, int quantity) {
        Optional<ItemType> optHeld = type.getItem();
        if (optHeld.isPresent()) {
            ItemStack is = WorldGuardPlugin.inst().getGame().getRegistry().createItemBuilder().itemType(optHeld.get()).quantity(quantity).build();
            return Optional.of(is);
        }
        return Optional.empty();
    }
}
