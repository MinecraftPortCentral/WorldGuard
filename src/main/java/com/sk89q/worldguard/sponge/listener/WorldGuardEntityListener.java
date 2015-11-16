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

import static com.sk89q.worldguard.protection.flags.DefaultFlag.OTHER_EXPLOSION;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Predicate;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.sponge.ConfigurationManager;
import com.sk89q.worldguard.sponge.RegionQuery;
import com.sk89q.worldguard.sponge.WorldConfiguration;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import com.sk89q.worldguard.sponge.util.Causes;
import com.sk89q.worldguard.sponge.util.Entities;
import com.sk89q.worldguard.sponge.util.Materials;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.TameableData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.explosive.Explosive;
import org.spongepowered.api.entity.explosive.PrimedTNT;
import org.spongepowered.api.entity.hanging.ItemFrame;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.animal.Pig;
import org.spongepowered.api.entity.living.animal.Wolf;
import org.spongepowered.api.entity.living.complex.EnderDragon;
import org.spongepowered.api.entity.living.monster.Creeper;
import org.spongepowered.api.entity.living.monster.Enderman;
import org.spongepowered.api.entity.living.monster.Wither;
import org.spongepowered.api.entity.living.monster.Zombie;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.explosive.ExplosiveProjectile;
import org.spongepowered.api.entity.projectile.explosive.WitherSkull;
import org.spongepowered.api.entity.projectile.explosive.fireball.Fireball;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.entity.vehicle.minecart.MinecartTNT;
import org.spongepowered.api.entity.weather.Lightning;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.health.HealTypes;
import org.spongepowered.api.event.cause.entity.health.HealthModifier;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.HealEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.world.ConstructPortalEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class WorldGuardEntityListener extends AbstractListener {

    /**
     * Construct the object;
     *
     * @param plugin The plugin instance
     */
    public WorldGuardEntityListener(WorldGuardPlugin plugin) {
        super(plugin);
    }

    @Listener
    public void onEntityInteract(InteractBlockEvent event) {
        if (event.getCause().first(Player.class).isPresent()) {
            Entity entity = event.getCause().first(Entity.class).get();
            Location<World> block = event.getTargetBlock().getLocation().get();

            if (block.getBlockType().equals(BlockTypes.FARMLAND)) {
                if (!(entity instanceof Player)) {
                    if (getWorldConfig(entity.getWorld()).disableCreatureCropTrampling) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @Listener
    public void onPlayerDeath(DestructEntityEvent event) {
        if (event.getTargetEntity() instanceof Player) {
            if (getWorldConfig(event.getTargetEntity().getWorld()).disableDeathMessages) {
                // TODO sponge event.(Texts.of());
            }
        }
    }

    private void onEntityDamageByBlock(DamageEntityEvent event) {
        Entity defender = event.getTargetEntity();
        Cause type = event.getCause();
        // TODO causes

        WorldConfiguration wcfg = getWorldConfig(defender.getWorld());

        if (defender instanceof Wolf && defender.get(TameableData.class).get().owner().exists()) {
            if (wcfg.antiWolfDumbness && Causes.protectWolf(type)) {
                event.setCancelled(true);
                return;
            }
        } else if (defender instanceof Player) {
            Player player = (Player) defender;

            if (wcfg.disableLavaDamage && Causes.isLava(type)) {
                event.setCancelled(true);
                // player.setFireTicks(0);
                return;
            }

            if (wcfg.disableContactDamage && Causes.isContact(type)) {
                event.setCancelled(true);
                return;
            }

            if (wcfg.teleportOnVoid && Causes.isVoid(type)) {
                player.setLocationSafely(player.getLocation());
                event.setCancelled(true);
                return;
            }

            if (wcfg.disableVoidDamage && Causes.isVoid(type)) {
                event.setCancelled(true);
                return;
            }

            if (Causes.isExplosion(type)
                    && (wcfg.disableExplosionDamage || wcfg.blockOtherExplosions || (wcfg.explosionFlagCancellation && !getPlugin()
                            .getRegionContainer().createQuery().testState(player.getLocation(), (Player) null, OTHER_EXPLOSION)))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void onEntityDamageByEntity(DamageEntityEvent event) {

        Entity attacker = Causes.getAttacker(event);
        if (attacker instanceof Projectile) {
            onEntityDamageByProjectile(event);
            return;
        }

        Entity defender = event.getTargetEntity();

        if (defender instanceof ItemFrame) {
            if (checkItemFrameProtection(attacker, (ItemFrame) defender)) {
                event.setCancelled(true);
                return;
            }
        }

        if (defender instanceof Player) {
            Player player = (Player) defender;
            LocalPlayer localPlayer = getPlugin().wrapPlayer(player);

            WorldConfiguration wcfg = getWorldConfig(player);

            if (wcfg.disableLightningDamage && attacker instanceof Lightning) {
                event.setCancelled(true);
                return;
            }

            if (wcfg.disableExplosionDamage && Causes.isExplosion(event.getCause())) {
                event.setCancelled(true);
                return;
            }

            if (attacker != null) {
                if (attacker instanceof PrimedTNT || attacker instanceof MinecartTNT) {
                    // The check for explosion damage should be handled
                    // already... But... What ever...
                    if (wcfg.blockTNTExplosions) {
                        event.setCancelled(true);
                        return;
                    }
                }

                if (attacker instanceof Living && !(attacker instanceof Player)) {
                    if (attacker instanceof Creeper && wcfg.blockCreeperExplosions) {
                        event.setCancelled(true);
                        return;
                    }

                    if (wcfg.disableMobDamage) {
                        event.setCancelled(true);
                        return;
                    }

                    if (wcfg.useRegions) {
                        ApplicableRegionSet set = getPlugin().getRegionContainer().createQuery().getApplicableRegions(defender.getLocation());

                        if (!set.testState(localPlayer, DefaultFlag.MOB_DAMAGE) && !(attacker.get(TameableData.class).isPresent())) {
                            event.setCancelled(true);
                            return;
                        }

                        if (attacker instanceof Creeper) {
                            if (!set.testState(localPlayer, DefaultFlag.CREEPER_EXPLOSION) && wcfg.explosionFlagCancellation) {
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void onEntityDamageByProjectile(DamageEntityEvent event) {
        Entity defender = event.getTargetEntity();
        Entity attacker = null;
        ProjectileSource source = Causes.getProjectileSource(event);
        if (source instanceof Entity) {
            attacker = (Entity) source;
        }

        if (defender instanceof Player) {
            Player player = (Player) defender;
            LocalPlayer localPlayer = getPlugin().wrapPlayer(player);

            WorldConfiguration wcfg = getWorldConfig(player);

            if (attacker instanceof ExplosiveProjectile) {
                if (attacker instanceof WitherSkull) {
                    if (wcfg.blockWitherSkullExplosions) {
                        event.setCancelled(true);
                        return;
                    }
                } else {
                    if (wcfg.blockFireballExplosions) {
                        event.setCancelled(true);
                        return;
                    }
                }
                if (wcfg.useRegions) {
                    RegionQuery query = getPlugin().getRegionContainer().createQuery();
                    if (!query.testState(defender.getLocation(), (Player) defender, DefaultFlag.GHAST_FIREBALL) && wcfg.explosionFlagCancellation) {
                        event.setCancelled(true);
                        return;
                    }

                }
            }

            // Check Mob
            if (attacker != null && !(attacker instanceof Player)) {
                if (wcfg.disableMobDamage) {
                    event.setCancelled(true);
                    return;
                }
                if (wcfg.useRegions) {
                    if (!getPlugin().getRegionContainer().createQuery().testState(defender.getLocation(), localPlayer, DefaultFlag.MOB_DAMAGE)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        } else if (defender instanceof ItemFrame) {
            if (checkItemFrameProtection(attacker, (ItemFrame) defender)) {
                event.setCancelled(true);
                return;
            }
        }

    }

    @Listener
    public void onEntityDamage(DamageEntityEvent event) {

        if (event.getCause().first(Entity.class).isPresent()) {
            this.onEntityDamageByEntity(event);
            return;
        } else if (event.getCause().first(Location.class).isPresent()) {
            this.onEntityDamageByBlock(event);
            return;
        }

        Entity defender = event.getTargetEntity();
        Cause type = event.getCause();

        ConfigurationManager cfg = getPlugin().getGlobalStateManager();
        WorldConfiguration wcfg = getWorldConfig(defender.getWorld());

        if ((defender instanceof Wolf) && Entities.isTamed(defender)) {
            if (wcfg.antiWolfDumbness) {
                event.setCancelled(true);
                return;
            }
        } else if (defender instanceof Player) {
            Player player = (Player) defender;

            if (Causes.isWither(type)) {
                // wither boss DoT tick
                if (wcfg.disableMobDamage) {
                    event.setCancelled(true);
                    return;
                }

                if (wcfg.useRegions) {
                    if (!getPlugin().getRegionContainer().createQuery()
                            .testState(player.getLocation(), getPlugin().wrapPlayer(player), DefaultFlag.MOB_DAMAGE)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }

            if (Causes.isDrowning(type)) {
                boolean cancel = false;
                if (cfg.hasAmphibiousMode(player)) {
                    cancel = true;
                }
                Optional<ItemStack> helmet = player.getHelmet();
                if (wcfg.pumpkinScuba && helmet.isPresent()
                        && (helmet.get().getItem().equals(ItemTypes.PUMPKIN) || helmet.get().getItem().equals(ItemTypes.LIT_PUMPKIN))) {
                    cancel = true;
                }
                if (wcfg.disableDrowningDamage) {
                    cancel = true;
                }
                if (cancel) {
                    player.offer(Keys.REMAINING_AIR, player.get(Keys.MAX_AIR).get());
                    event.setCancelled(true);
                    return;
                }
            }

            if (wcfg.disableFallDamage && Causes.isFalling(type)) {
                event.setCancelled(true);
                return;
            }

            if (wcfg.disableFireDamage && Causes.isFire(type)) {
                event.setCancelled(true);
                return;
            }

            if (Causes.isSuffocation(type)) {
                if (wcfg.teleportOnSuffocation) {
                    player.setLocationSafely(player.getLocation());
                }
                if (wcfg.disableSuffocationDamage) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    /*
     * Called on entity explode.
     */
    @Listener
    public void onEntityExplode(ExplosionEvent.Detonate event) {
        if (event.getCause().first(Entity.class).isPresent()) {
            Entity ent = event.getCause().first(Entity.class).get();
            ConfigurationManager cfg = getPlugin().getGlobalStateManager();
            Vector3d loc = event.getExplosion().getOrigin();
            World world = event.getTargetWorld();
            WorldConfiguration wcfg = cfg.get(world);
            Optional<Explosive> exp = event.getExplosion().getSourceExplosive();

            if (cfg.activityHaltToggle) {
                if (exp.isPresent()) {
                    exp.get().remove();
                }
                event.filter(new InvalidateAllPredicate());
                return;
            }

            if (ent instanceof Creeper) {
                if (wcfg.blockCreeperExplosions) {
                    event.setCancelled(true);
                    return;
                }
                if (wcfg.blockCreeperBlockDamage) {
                    event.getTransactions().clear();
                    return;
                }
            } else if (ent instanceof EnderDragon) {
                if (wcfg.blockEnderDragonBlockDamage) {
                    event.filter(new InvalidateAllPredicate());
                    return;
                }
            } else if (ent instanceof PrimedTNT || ent instanceof MinecartTNT) {
                if (wcfg.blockTNTExplosions) {
                    event.setCancelled(true);
                    return;
                }
                if (wcfg.blockTNTBlockDamage) {
                    event.filter(new InvalidateAllPredicate());
                    return;
                }
            } else if (ent instanceof Fireball) {
                if (wcfg.blockFireballExplosions) {
                    event.setCancelled(true);
                    return;
                }
                if (wcfg.blockFireballBlockDamage) {
                    event.filter(new InvalidateAllPredicate());
                    return;
                }
                // allow wither skull blocking since there is no dedicated flag
                // atm
                if (wcfg.useRegions) {
                    RegionManager mgr = getPlugin().getRegionContainer().get(world);

                    for (Transaction<BlockSnapshot> block : event.getTransactions()) {
                        // todo nullsafe?
                        Optional<Location<World>> filterLoc = block.getOriginal().getLocation();
                        if (!filterLoc.isPresent())
                            continue;
                        if (!getPlugin().getRegionContainer().createQuery().getApplicableRegions(filterLoc.get())
                                .testState(null, DefaultFlag.GHAST_FIREBALL)) {
                            event.filter(new InvalidateAllPredicate());
                            if (wcfg.explosionFlagCancellation)
                                event.setCancelled(true);
                            return;
                        }
                    }
                }
            } else if (ent instanceof WitherSkull) {
                if (wcfg.blockWitherSkullExplosions) {
                    event.setCancelled(true);
                    return;
                }
                if (wcfg.blockWitherSkullBlockDamage) {
                    event.filter(new InvalidateAllPredicate());
                    return;
                }
            } else if (ent instanceof Wither) {
                if (wcfg.blockWitherExplosions) {
                    event.setCancelled(true);
                    return;
                }
                if (wcfg.blockWitherBlockDamage) {
                    event.filter(new InvalidateAllPredicate());
                    return;
                }
            } else {
                // unhandled entity
                if (wcfg.blockOtherExplosions) {
                    event.setCancelled(true);
                    return;
                }
                if (wcfg.useRegions) {
                    RegionManager mgr = getPlugin().getRegionContainer().get(world);

                    for (Transaction<BlockSnapshot> block : event.getTransactions()) {
                        // todo nullsafe?
                        Optional<Location<World>> filterLoc = block.getOriginal().getLocation();
                        if (!filterLoc.isPresent())
                            continue;
                        if (!getPlugin().getRegionContainer().createQuery().getApplicableRegions(filterLoc.get())
                                .testState(null, DefaultFlag.OTHER_EXPLOSION)) {
                            event.filter(new InvalidateAllPredicate());
                            if (wcfg.explosionFlagCancellation)
                                event.setCancelled(true);
                            return;
                        }
                    }
                }
            }

            if (wcfg.signChestProtection) {
                for (Transaction<BlockSnapshot> block : event.getTransactions()) {
                    // todo nullsafe?
                    Optional<Location<World>> filterLoc = block.getOriginal().getLocation();
                    if (!filterLoc.isPresent())
                        continue;
                    if (wcfg.isChestProtected(filterLoc.get())) {
                        event.filter(new InvalidateAllPredicate());
                        return;
                    }
                }
            }
        }
    }

    /*
     * Called on explosion prime
     */
    @Listener
    public void onExplosionPrime(ExplosionEvent.Pre event) {
        ConfigurationManager cfg = getPlugin().getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(event.getTargetWorld());
        Explosive ent = event.getExplosion().getSourceExplosive().orElse(null);

        if (cfg.activityHaltToggle) {
            event.setCancelled(true);
            if (ent != null)
                ent.remove();
            return;
        }

        if (ent == null)
            return;

        if (ent.getType().equals(EntityTypes.WITHER)) {
            if (wcfg.blockWitherExplosions) {
                event.setCancelled(true);
                return;
            }
        } else if (ent.getType().equals(EntityTypes.WITHER_SKULL)) {
            if (wcfg.blockWitherSkullExplosions) {
                event.setCancelled(true);
                return;
            }
        } else if (ent.getType().equals(EntityTypes.FIREBALL)) {
            if (wcfg.blockFireballExplosions) {
                event.setCancelled(true);
                return;
            }
        } else if (ent.getType().equals(EntityTypes.CREEPER)) {
            if (wcfg.blockCreeperExplosions) {
                event.setCancelled(true);
                return;
            }
        } else if (ent.getType().equals(EntityTypes.PRIMED_TNT) || ent.getType().equals(EntityTypes.TNT_MINECART)) {
            if (wcfg.blockTNTExplosions) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @Listener
    public void onCreatureSpawn(SpawnEntityEvent event) {
        ConfigurationManager cfg = getPlugin().getGlobalStateManager();

        if (cfg.activityHaltToggle) {
            event.setCancelled(true);
            return;
        }

        for (Entity entity : event.getEntities()) {
            WorldConfiguration wcfg = cfg.get(entity.getWorld());

            // allow spawning of creatures from plugins
            if (!wcfg.blockPluginSpawning && Causes.isPluginSpawn(event.getCause())) {
                return;
            }

            if (wcfg.allowTamedSpawns && entity.get(Keys.TAMED_OWNER).isPresent()) {
                return;
            }

            EntityType entityType = entity.getType();

            if (wcfg.blockCreatureSpawn.contains(entityType)) {
                event.setCancelled(true);
                return;
            }

            Transform<World> eventLoc = entity.getTransform();

            if (wcfg.useRegions && cfg.useRegionsCreatureSpawnEvent) {
                ApplicableRegionSet set = getPlugin().getRegionContainer().createQuery().getApplicableRegions(eventLoc.getLocation());

                if (!set.testState(null, DefaultFlag.MOB_SPAWNING)) {
                    event.setCancelled(true);
                    return;
                }

                Set<EntityType> entityTypes = set.queryValue(null, DefaultFlag.DENY_SPAWN);
                if (entityTypes != null && entityTypes.contains(entityType)) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (wcfg.blockGroundSlimes && entityType.equals(EntityTypes.SLIME) && eventLoc.getPosition().getFloorY() >= 60
                    && Causes.isNaturalSpawn(event.getCause())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @Listener
    public void onCreatePortal(ConstructPortalEvent event) {
        ConfigurationManager cfg = getPlugin().getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(event.getPortalLocation().getExtent());

        if (event.getCause().first(Entity.class).orElse(null) instanceof EnderDragon) {
            if (wcfg.blockEnderDragonPortalCreation) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onEntityStruckByLightning(InteractEntityEvent.Primary event) {
        if (event.getCause().first(Lightning.class).isPresent()) {
            Lightning lightning = event.getCause().first(Lightning.class).get();
            WorldConfiguration wcfg = getWorldConfig(lightning.getWorld());
            if (wcfg.disablePigZap && event.getTargetEntity() instanceof Pig) {
                event.setCancelled(true);
                return;
            }
            if (wcfg.disableCreeperPower && event.getTargetEntity() instanceof Creeper) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @Listener
    public void onEntityRegainHealth(HealEntityEvent event) {
        Entity ent = event.getTargetEntity();
        World world = ent.getWorld();

        ConfigurationManager cfg = getPlugin().getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(world);

        if (wcfg.disableHealthRegain) {
            for (Tuple<HealthModifier, Function<? super Double, Double>> func : event.getModifiers()) {
                if (func.getFirst().getType().equals(HealTypes.FOOD)) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    /**
     * Called when an entity changes a block somehow
     *
     * @param event Relevant event details
     */
    @Listener
    public void onEntityChangeBlock(ChangeBlockEvent event) {
        if (event.getCause().first(Entity.class).isPresent()) {
            Entity ent = event.getCause().first(Entity.class).get();
            ConfigurationManager cfg = getPlugin().getGlobalStateManager();
            WorldConfiguration wcfg = cfg.get(ent.getWorld());
            if (ent instanceof Enderman) {
                if (wcfg.disableEndermanGriefing) {
                    event.setCancelled(true);
                    return;
                }

                if (wcfg.useRegions) {
                    final RegionQuery query = getPlugin().getRegionContainer().createQuery();
                    event.filter(new Predicate<Location<World>>() {

                        @Override
                        public boolean apply(Location<World> loc) {
                            return query.testState(loc, (RegionAssociable) null, DefaultFlag.ENDER_BUILD);
                        }
                    });
                }
            } else if (ent.getType().equals(EntityTypes.WITHER)) {
                if (wcfg.blockWitherBlockDamage || wcfg.blockWitherExplosions) {
                    event.setCancelled(true);
                    return;
                }
            } else if (ent instanceof Zombie) {
                if (wcfg.blockZombieDoorDestruction) {
                    event.filter(new Predicate<Location<World>>() {

                        @Override
                        public boolean apply(Location<World> loc) {
                            return Materials.isWoodDoor(loc.getBlockType());
                        }
                    });
                    return;
                }
            }
        }
    }

    /**
     * Checks regions and config settings to protect items from being knocked
     * out of item frames.
     * 
     * @param attacker attacking entity
     * @param defender item frame being damaged
     * @return true if the event should be cancelled
     */
    private boolean checkItemFrameProtection(Entity attacker, ItemFrame defender) {
        World world = attacker.getWorld();
        WorldConfiguration wcfg = getWorldConfig(world);
        if (wcfg.useRegions) {
            if (!(attacker instanceof Player)) {
                if (!getPlugin().getRegionContainer().createQuery()
                        .testState(defender.getLocation(), (RegionAssociable) null, DefaultFlag.ENTITY_ITEM_FRAME_DESTROY)) {
                    return true;
                }
            }
        }
        if (wcfg.blockEntityItemFrameDestroy && !(attacker instanceof Player)) {
            return true;
        }
        return false;
    }

    private static class InvalidateAllPredicate implements Predicate<Location<World>> {

        @Override
        public boolean apply(Location<World> location) {
            return false;
        }
    }
}
