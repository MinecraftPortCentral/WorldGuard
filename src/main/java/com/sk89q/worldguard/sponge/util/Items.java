package com.sk89q.worldguard.sponge.util;

import java.util.Optional;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.item.ItemBlock;
import org.spongepowered.api.item.inventory.ItemStack;

public class Items {
    public static Optional<ItemStack> toItemStack(BlockType type) {
        Optional<ItemBlock> optHeld = type.getHeldItem();
        if (optHeld.isPresent()) {
            ItemStack is = WorldGuardPlugin.inst().getGame().getRegistry().createItemBuilder().itemType(optHeld.get()).build();
            return Optional.of(is);
        }
        return Optional.absent();
    }

    public static Optional<ItemStack> toItemStack(BlockType type, int quantity) {
        Optional<ItemBlock> optHeld = type.getHeldItem();
        if (optHeld.isPresent()) {
            ItemStack is = WorldGuardPlugin.inst().getGame().getRegistry().createItemBuilder().itemType(optHeld.get()).quantity(quantity).build();
            return Optional.of(is);
        }
        return Optional.absent();
    }
}
