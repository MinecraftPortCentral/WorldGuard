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

package com.sk89q.worldguard.sponge.listener;

import java.util.Optional;
import com.google.common.base.Predicate;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.sponge.ConfigurationManager;
import com.sk89q.worldguard.sponge.RegionQuery;
import com.sk89q.worldguard.sponge.WorldConfiguration;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import com.sk89q.worldguard.sponge.util.Materials;
import org.spongepowered.api.block.BlockTransaction;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.DurabilityData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.BreakBlockEvent;
import org.spongepowered.api.event.block.PlaceBlockEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;

/**
 * The listener for block events.
 *
 * @author sk89q
 */
public class WorldGuardBlockListener extends AbstractListener {

    /**
     * Construct the listener.
     *
     * @param plugin an instance of WorldGuardPlugin
     */
    public WorldGuardBlockListener(WorldGuardPlugin plugin) {
        super(plugin);
    }

    /*
     * Called when a block is broken.
     */
    @Listener
    public void onBlockBreak(BreakBlockEvent event) {
        Player player = event.getCause().getFirst(Player.class).orNull();
        if (player == null) return;
        WorldConfiguration wcfg = getWorldConfig(player);

        if (!wcfg.itemDurability) {
            ItemStack held = player.getItemInHand().orNull();
            if (held != null && held.supports(Keys.ITEM_DURABILITY)) {
                held.offer(Keys.ITEM_DURABILITY, held.get(DurabilityData.class).get().durability().getMaxValue());
                player.setItemInHand(held);
            }
        }
    }

    /*
     * Called when fluids flow.
     */
    @Listener
    public void onBlockFromTo(final PlaceBlockEvent event) {
        Location<World> block = event.getCause().getFirst(Location.class).orNull();
        if (block == null) return;
        final World world = block.getExtent();
        List<BlockTransaction> changes = event.getTransactions();

        // todo add || event.getCause().getFirst(BlockSnapshot.class) to test for liquid movement
        boolean isWater = Materials.isWater(block.getBlockType());
        boolean isLava = Materials.isLava(block.getBlockType());
        boolean isAir = block.getBlockType().equals(BlockTypes.AIR);

        ConfigurationManager cfg = getPlugin().getGlobalStateManager();
        final WorldConfiguration wcfg = getWorldConfig(world);

        if (cfg.activityHaltToggle) {
            event.setCancelled(true);
            return;
        }

        if (wcfg.simulateSponge && isWater) {
            event.filter(new Predicate<Location<World>>() {
                @Override
                public boolean apply(Location<World> loc) {
                    int ox = loc.getBlockX();
                    int oy = loc.getBlockY();
                    int oz = loc.getBlockZ();

                    for (int cx = -wcfg.spongeRadius; cx <= wcfg.spongeRadius; cx++) {
                        for (int cy = -wcfg.spongeRadius; cy <= wcfg.spongeRadius; cy++) {
                            for (int cz = -wcfg.spongeRadius; cz <= wcfg.spongeRadius; cz++) {
                                Location<World> sponge = world.getLocation(ox + cx, oy + cy, oz + cz);
                                if (sponge.getBlockType().equals(BlockTypes.SPONGE)
                                        && (!wcfg.redstoneSponges || !sponge.getPoweredBlockFaces().isEmpty())) {
                                    return false;
                                }
                            }
                        }
                    }
                    return true;
                }
            });
        }

        /*if (plugin.classicWater && isWater) {
        int blockBelow = blockFrom.getRelative(0, -1, 0).getTypeId();
        if (blockBelow != 0 && blockBelow != 8 && blockBelow != 9) {
        blockFrom.setTypeId(9);
        if (blockTo.getTypeId() == 0) {
        blockTo.setTypeId(9);
        }
        return;
        }
        }*/

        // Check the fluid block (from) whether it is air.
        // If so and the target block is protected, cancel the event
        if (!wcfg.preventWaterDamage.isEmpty()) {
            if ((isAir || isWater)) {
                event.filter(new Predicate<Location<World>>() {
                    @Override
                    public boolean apply(Location<World> loc) {
                        return !wcfg.preventWaterDamage.contains(loc.getBlockType());
                    }
                });
            }
        }

        if (!wcfg.allowedLavaSpreadOver.isEmpty() && isLava) {
            event.filter(new Predicate<Location<World>>() {
                @Override
                public boolean apply(Location<World> loc) {
                    return !wcfg.allowedLavaSpreadOver.contains(loc.getRelative(Direction.DOWN).getBlockType());
                }
            });
        }

        if (wcfg.highFreqFlags && isWater) {
            final RegionQuery query = getPlugin().getRegionContainer().createQuery();
            event.filter(new Predicate<Location<World>>() {
                @Override
                public boolean apply(Location<World> loc) {
                    return query.testState(loc, (RegionAssociable) null, DefaultFlag.WATER_FLOW);
                }
            });
        }

        if (wcfg.highFreqFlags && isLava) {
            final RegionQuery query = getPlugin().getRegionContainer().createQuery();
            event.filter(new Predicate<Location<World>>() {
                @Override
                public boolean apply(Location<World> loc) {
                    return query.testState(loc, (RegionAssociable) null, DefaultFlag.LAVA_FLOW);
                }
            });
        }

        if (wcfg.disableObsidianGenerators && (isAir || isLava)) {
            event.filter(new Predicate<Location<World>>() {
                @Override
                public boolean apply(Location<World> loc) {
                    if (loc.getBlockType().equals(BlockTypes.REDSTONE_WIRE) || loc.getBlockType().equals(BlockTypes.TRIPWIRE)) {
                        loc.setBlockType(BlockTypes.AIR);
                    }
                    return true;
                }
            });
        }
    }

    /*
     * Called when a block gets ignited.
     */
    @Listener
    public void onBlockIgnite(PlaceBlockEvent event) {
        final ConfigurationManager cfg = getPlugin().getGlobalStateManager();
        Optional<Location<World>> optWorld = event.getTransactions().get(0).getFinalReplacement().getLocation();
        if (!optWorld.isPresent()) return; // PANIC!!!!
        WorldConfiguration wcfg = cfg.get(optWorld.get().getExtent());

        event.filter(new Predicate<Location<World>>() {
            @Override
            public boolean apply(Location<World> location) {
                if (!location.getBlockType().equals(BlockTypes.FIRE)) return true;
                if (cfg.activityHaltToggle) return false;

                return true;
            }
        });

        IgniteCause cause = event.getCause();
        Block block = event.getBlock();
        World world = block.getWorld();

        boolean isFireSpread = cause == IgniteCause.SPREAD;

        if (wcfg.preventLightningFire && cause == IgniteCause.LIGHTNING) {
            event.setCancelled(true);
            return;
        }

        if (wcfg.preventLavaFire && cause == IgniteCause.LAVA) {
            event.setCancelled(true);
            return;
        }

        if (wcfg.disableFireSpread && isFireSpread) {
            event.setCancelled(true);
            return;
        }

        if (wcfg.blockLighter && (cause == IgniteCause.FLINT_AND_STEEL || cause == IgniteCause.FIREBALL)
                && event.getPlayer() != null
                && !plugin.hasPermission(event.getPlayer(), "worldguard.override.lighter")) {
            event.setCancelled(true);
            return;
        }

        if (wcfg.fireSpreadDisableToggle && isFireSpread) {
            event.setCancelled(true);
            return;
        }

        if (!wcfg.disableFireSpreadBlocks.isEmpty() && isFireSpread) {
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();

            if (wcfg.disableFireSpreadBlocks.contains(world.getBlockTypeIdAt(x, y - 1, z))
                    || wcfg.disableFireSpreadBlocks.contains(world.getBlockTypeIdAt(x + 1, y, z))
                    || wcfg.disableFireSpreadBlocks.contains(world.getBlockTypeIdAt(x - 1, y, z))
                    || wcfg.disableFireSpreadBlocks.contains(world.getBlockTypeIdAt(x, y, z - 1))
                    || wcfg.disableFireSpreadBlocks.contains(world.getBlockTypeIdAt(x, y, z + 1))) {
                event.setCancelled(true);
                return;
            }
        }

        if (wcfg.useRegions) {
            ApplicableRegionSet set = plugin.getRegionContainer().createQuery().getApplicableRegions(block.getLocation());

            if (wcfg.highFreqFlags && isFireSpread
                    && !set.allows(DefaultFlag.FIRE_SPREAD)) {
                event.setCancelled(true);
                return;
            }

            if (wcfg.highFreqFlags && cause == IgniteCause.LAVA
                    && !set.allows(DefaultFlag.LAVA_FIRE)) {
                event.setCancelled(true);
                return;
            }

            if (cause == IgniteCause.FIREBALL && event.getPlayer() == null) {
                // wtf bukkit, FIREBALL is supposed to be reserved to players
                if (!set.allows(DefaultFlag.GHAST_FIREBALL)) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (cause == IgniteCause.LIGHTNING && !set.allows(DefaultFlag.LIGHTNING)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /*
     * Called when a block is destroyed from burning.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        ConfigurationManager cfg = plugin.getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(event.getBlock().getWorld());

        if (cfg.activityHaltToggle) {
            event.setCancelled(true);
            return;
        }

        if (wcfg.disableFireSpread) {
            event.setCancelled(true);
            return;
        }

        if (wcfg.fireSpreadDisableToggle) {
            Block block = event.getBlock();
            event.setCancelled(true);
            checkAndDestroyAround(block.getWorld(), block.getX(), block.getY(), block.getZ(), BlockID.FIRE);
            return;
        }

        if (wcfg.disableFireSpreadBlocks.size() > 0) {
            Block block = event.getBlock();

            if (wcfg.disableFireSpreadBlocks.contains(block.getTypeId())) {
                event.setCancelled(true);
                checkAndDestroyAround(block.getWorld(), block.getX(), block.getY(), block.getZ(), BlockID.FIRE);
                return;
            }
        }

        if (wcfg.isChestProtected(event.getBlock())) {
            event.setCancelled(true);
            return;
        }

        if (wcfg.useRegions) {
            Block block = event.getBlock();
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();
            ApplicableRegionSet set = plugin.getRegionContainer().createQuery().getApplicableRegions(block.getLocation());

            if (!set.allows(DefaultFlag.FIRE_SPREAD)) {
                checkAndDestroyAround(block.getWorld(), x, y, z, BlockID.FIRE);
                event.setCancelled(true);
            }

        }
    }

    private void checkAndDestroyAround(World world, int x, int y, int z, int required) {
        checkAndDestroy(world, x, y, z + 1, required);
        checkAndDestroy(world, x, y, z - 1, required);
        checkAndDestroy(world, x, y + 1, z, required);
        checkAndDestroy(world, x, y - 1, z, required);
        checkAndDestroy(world, x + 1, y, z, required);
        checkAndDestroy(world, x - 1, y, z, required);
    }

    private void checkAndDestroy(World world, int x, int y, int z, int required) {
        if (world.getBlockTypeIdAt(x, y, z) == required) {
            world.getBlockAt(x, y, z).setTypeId(BlockID.AIR);
        }
    }

    /*
     * Called when block physics occurs.
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        ConfigurationManager cfg = plugin.getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(event.getBlock().getWorld());

        if (cfg.activityHaltToggle) {
            event.setCancelled(true);
            return;
        }

        int id = event.getChangedTypeId();

        if (id == 13 && wcfg.noPhysicsGravel) {
            event.setCancelled(true);
            return;
        }

        if (id == 12 && wcfg.noPhysicsSand) {
            event.setCancelled(true);
            return;
        }

        if (id == 90 && wcfg.allowPortalAnywhere) {
            event.setCancelled(true);
            return;
        }

        if (wcfg.ropeLadders && event.getBlock().getType() == Material.LADDER) {
            if (event.getBlock().getRelative(0, 1, 0).getType() == Material.LADDER) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /*
     * Called when a player places a block.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block target = event.getBlock();
        World world = target.getWorld();

        ConfigurationManager cfg = plugin.getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(world);

        if (wcfg.simulateSponge && target.getType() == Material.SPONGE) {
            if (wcfg.redstoneSponges && target.isBlockIndirectlyPowered()) {
                return;
            }

            int ox = target.getX();
            int oy = target.getY();
            int oz = target.getZ();

            SpongeUtil.clearSpongeWater(plugin, world, ox, oy, oz);
        }
    }

    /*
     * Called when redstone changes.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        Block blockTo = event.getBlock();
        World world = blockTo.getWorld();

        ConfigurationManager cfg = plugin.getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(world);

        if (wcfg.simulateSponge && wcfg.redstoneSponges) {
            int ox = blockTo.getX();
            int oy = blockTo.getY();
            int oz = blockTo.getZ();

            for (int cx = -1; cx <= 1; cx++) {
                for (int cy = -1; cy <= 1; cy++) {
                    for (int cz = -1; cz <= 1; cz++) {
                        Block sponge = world.getBlockAt(ox + cx, oy + cy, oz + cz);
                        if (sponge.getTypeId() == 19
                                && sponge.isBlockIndirectlyPowered()) {
                            SpongeUtil.clearSpongeWater(plugin, world, ox + cx, oy + cy, oz + cz);
                        } else if (sponge.getTypeId() == 19
                                && !sponge.isBlockIndirectlyPowered()) {
                            SpongeUtil.addSpongeWater(plugin, world, ox + cx, oy + cy, oz + cz);
                        }
                    }
                }
            }

            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        ConfigurationManager cfg = plugin.getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(event.getBlock().getWorld());

        if (cfg.activityHaltToggle) {
            event.setCancelled(true);
            return;
        }

        if (wcfg.disableLeafDecay) {
            event.setCancelled(true);
            return;
        }

        if (wcfg.useRegions) {
            if (!plugin.getGlobalRegionManager().allows(DefaultFlag.LEAF_DECAY,
                    event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    /*
     * Called when a block is formed based on world conditions.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        ConfigurationManager cfg = plugin.getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(event.getBlock().getWorld());

        if (cfg.activityHaltToggle) {
            event.setCancelled(true);
            return;
        }

        int type = event.getNewState().getTypeId();

        if (event instanceof EntityBlockFormEvent) {
            if (((EntityBlockFormEvent) event).getEntity() instanceof Snowman) {
                if (wcfg.disableSnowmanTrails) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }

        if (type == BlockID.ICE) {
            if (wcfg.disableIceFormation) {
                event.setCancelled(true);
                return;
            }
            if (wcfg.useRegions && !plugin.getGlobalRegionManager().allows(
                    DefaultFlag.ICE_FORM, event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
        }

        if (type == BlockID.SNOW) {
            if (wcfg.disableSnowFormation) {
                event.setCancelled(true);
                return;
            }
            if (wcfg.allowedSnowFallOver.size() > 0) {
                int targetId = event.getBlock().getRelative(0, -1, 0).getTypeId();

                if (!wcfg.allowedSnowFallOver.contains(targetId)) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (wcfg.useRegions && !plugin.getGlobalRegionManager().allows(
                    DefaultFlag.SNOW_FALL, event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /*
     * Called when a block spreads based on world conditions.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        ConfigurationManager cfg = plugin.getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(event.getBlock().getWorld());

        if (cfg.activityHaltToggle) {
            event.setCancelled(true);
            return;
        }

        int fromType = event.getSource().getTypeId();

        if (fromType == BlockID.RED_MUSHROOM || fromType == BlockID.BROWN_MUSHROOM) {
            if (wcfg.disableMushroomSpread) {
                event.setCancelled(true);
                return;
            }
            if (wcfg.useRegions && !plugin.getGlobalRegionManager().allows(
                    DefaultFlag.MUSHROOMS, event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
        }

        if (fromType == BlockID.GRASS) {
            if (wcfg.disableGrassGrowth) {
                event.setCancelled(true);
                return;
            }
            if (wcfg.useRegions && !plugin.getGlobalRegionManager().allows(
                    DefaultFlag.GRASS_SPREAD, event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
        }

        if (fromType == BlockID.MYCELIUM) {
            if (wcfg.disableMyceliumSpread) {
                event.setCancelled(true);
                return;
            }

            if (wcfg.useRegions
                    && !plugin.getGlobalRegionManager().allows(
                            DefaultFlag.MYCELIUM_SPREAD, event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
        }

        if (fromType == BlockID.VINE) {
            if (wcfg.disableVineGrowth) {
                event.setCancelled(true);
                return;
            }

            if (wcfg.useRegions
                    && !plugin.getGlobalRegionManager().allows(
                            DefaultFlag.VINE_GROWTH, event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /*
     * Called when a block fades.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {

        ConfigurationManager cfg = plugin.getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(event.getBlock().getWorld());

        switch (event.getBlock().getTypeId()) {
        case BlockID.ICE:
            if (wcfg.disableIceMelting) {
                event.setCancelled(true);
                return;
            }

            if (wcfg.useRegions && !plugin.getGlobalRegionManager().allows(
                    DefaultFlag.ICE_MELT, event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
            break;

        case BlockID.SNOW:
            if (wcfg.disableSnowMelting) {
                event.setCancelled(true);
                return;
            }

            if (wcfg.useRegions && !plugin.getGlobalRegionManager().allows(
                    DefaultFlag.SNOW_MELT, event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
            break;

        case BlockID.SOIL:
            if (wcfg.disableSoilDehydration) {
                event.setCancelled(true);
                return;
            }
            if (wcfg.useRegions && !plugin.getGlobalRegionManager().allows(
                    DefaultFlag.SOIL_DRY, event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
            break;
        }

    }

}
