package com.sk89q.worldguard.sponge.util;

import java.util.Optional;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.BlockDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSources;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnType;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.cause.entity.teleport.TeleportCause;
import org.spongepowered.api.event.cause.entity.teleport.TeleportTypes;
import org.spongepowered.api.event.entity.DamageEntityEvent;

public final class Causes {

    private Causes() {
    }


    public static boolean isEnderPearlTeleport(Cause cause) {
        if (cause instanceof TeleportCause) {
            return ((TeleportCause) cause).getTeleportType().equals(TeleportTypes.ENDER_PEARL);
        }
        return false;
    }

    public static boolean protectWolf(Cause cause) {
        return (!isVoid(cause));
    }

    public static boolean isLava(Cause cause) {
        Optional<DamageSource> source = cause.first(DamageSource.class);
        if (source.isPresent() && source.get() instanceof BlockDamageSource) {
            BlockType type = ((BlockDamageSource) source.get()).getBlockSnapshot().getState().getType();
            return type.equals(BlockTypes.LAVA) || type.equals(BlockTypes.FLOWING_LAVA);
        }
        return false;
    }

    public static boolean isVoid(Cause cause) {
        return findDamageSource(cause, DamageSources.VOID);
    }

    public static boolean isContact(Cause cause) {
        for (DamageSource source : cause.allOf(DamageSource.class)) {
            if (source.getDamageType().equals(DamageTypes.CONTACT)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExplosion(Cause cause) {
        return findDamageSource(cause, DamageSources.EXPLOSION);
    }

    public static boolean isWither(Cause cause) {
        return findDamageSource(cause, DamageSources.WITHER);
    }

    public static boolean isDrowning(Cause cause) {
        return findDamageSource(cause, DamageSources.DROWNING);
    }


    public static boolean isFalling(Cause cause) {
        return findDamageSource(cause, DamageSources.FALLING);
    }

    public static boolean isFire(Cause cause) {
        return findDamageSource(cause, DamageSources.FIRE_TICK) || findDamageSource(cause, DamageSources.IN_FIRE);
    }

    public static boolean isSuffocation(Cause cause) {
        for (DamageSource source : cause.allOf(DamageSource.class)) {
            if (source instanceof BlockDamageSource) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPluginSpawn(Cause cause) {
        return findSpawnCause(cause, SpawnTypes.PLUGIN);
    }

    public static boolean isNaturalSpawn(Cause cause) {
        return findSpawnCause(cause, SpawnTypes.PASSIVE);
    }


    private static boolean findDamageSource(Cause cause, DamageSource source) {
        for (DamageSource damageSource : cause.allOf(DamageSource.class)) {
            if (damageSource.equals(source)) {
                return true;
            }
        }
        return false;
    }

    private static boolean findSpawnCause(Cause cause, SpawnType type) {
        Optional<SpawnCause> spawnCause = cause.first(SpawnCause.class);
        return spawnCause.isPresent() && spawnCause.get().getType().equals(type);
    }

    public static Entity getAttacker(DamageEntityEvent event) {
        Optional<Entity> first = event.getCause().first(Entity.class);
        return first.isPresent() ? first.get() : null;
    }

    public static ProjectileSource getProjectileSource(DamageEntityEvent event) {
        Entity attacker = getAttacker(event);
        if (attacker instanceof Projectile) {
            return ((Projectile) attacker).getShooter();
        }
        return null;
    }
}
