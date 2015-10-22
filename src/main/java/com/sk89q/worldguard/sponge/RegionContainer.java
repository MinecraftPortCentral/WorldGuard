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

package com.sk89q.worldguard.sponge;

import com.flowpowered.math.vector.Vector3i;
import com.sk89q.worldguard.protection.managers.RegionContainerImpl;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.migration.Migration;
import com.sk89q.worldguard.protection.managers.migration.MigrationException;
import com.sk89q.worldguard.protection.managers.migration.UUIDMigration;
import com.sk89q.worldguard.protection.managers.storage.RegionDriver;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;
import org.spongepowered.api.event.world.chunk.LoadChunkEvent;
import org.spongepowered.api.event.world.chunk.UnloadChunkEvent;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A region container creates {@link RegionManager}s for loaded worlds, which
 * allows access to the region data of a world. Generally, only data is
 * loaded for worlds that are loaded in the server.
 *
 * <p>This class is thread safe and its contents can be accessed from
 * multiple concurrent threads.</p>
 *
 * <p>An instance of this class can be retrieved using
 * {@link WorldGuardPlugin#getRegionContainer()}.</p>
 */
public class RegionContainer {

    private static final Logger log = Logger.getLogger(RegionContainer.class.getCanonicalName());

    /**
     * Invalidation frequency in ticks.
     */
    private static final int CACHE_INVALIDATION_INTERVAL = 2;

    private final Object lock = new Object();
    private final WorldGuardPlugin plugin;
    private final QueryCache cache = new QueryCache();
    private RegionContainerImpl container;

    /**
     * Create a new instance.
     *
     * @param plugin the plugin
     */
    RegionContainer(WorldGuardPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the region container.
     */
    void initialize() {
        ConfigurationManager config = plugin.getGlobalStateManager();
        container = new RegionContainerImpl(config.selectedRegionStoreDriver);

        // Migrate to UUIDs
        autoMigrate();

        loadWorlds();

        plugin.getGame().getEventManager().registerListeners(plugin, new Object() {
            @Listener
            public void onWorldLoad(LoadWorldEvent event) {
                load(event.getTargetWorld());
            }

            @Listener
            public void onWorldUnload(UnloadWorldEvent event) {
                unload(event.getTargetWorld());
            }

            @Listener
            public void onChunkLoad(LoadChunkEvent event) {
                RegionManager manager = get(event.getTargetChunk().getWorld());
                if (manager != null) {
                    manager.loadChunk(event.getTargetChunk().getPosition());
                }
            }

            @Listener
            public void onChunkUnload(UnloadChunkEvent event) {
                RegionManager manager = get(event.getTargetChunk().getWorld());
                if (manager != null) {
                    manager.unloadChunk(event.getTargetChunk().getPosition());
                }
            }
        });

        plugin.getGame().getScheduler().createTaskBuilder().name("WorldGuardRegionCache")
                .intervalTicks(CACHE_INVALIDATION_INTERVAL).delayTicks(CACHE_INVALIDATION_INTERVAL)
                .execute(new Runnable() {
                    @Override
                    public void run() {
                        cache.invalidateAll();
                    }
                }).submit(plugin);
    }

    /**
     * Save data and unload.
     */
    void unload() {
        synchronized (lock) {
            container.unloadAll();
        }
    }

    /**
     * Get the region store driver.
     *
     * @return the driver
     */
    public RegionDriver getDriver() {
        return container.getDriver();
    }

    /**
     * Try loading the region managers for all currently loaded worlds.
     */
    private void loadWorlds() {
        synchronized (lock) {
            for (World world : plugin.getGame().getServer().getWorlds()) {
                load(world);
            }
        }
    }

    /**
     * Reload the region container.
     *
     * <p>This method may block until the data for all loaded worlds has been
     * unloaded and new data has been loaded.</p>
     */
    public void reload() {
        synchronized (lock) {
            unload();
            loadWorlds();
        }
    }

    /**
     * Load the region data for a world if it has not been loaded already.
     *
     * @param world the world
     * @return a region manager, either returned from the cache or newly loaded
     */
    @Nullable
    private RegionManager load(World world) {
        checkNotNull(world);

        WorldConfiguration config = plugin.getGlobalStateManager().get(world);
        if (!config.useRegions) {
            return null;
        }

        RegionManager manager;

        synchronized (lock) {
            manager = container.load(world.getName());

            if (manager != null) {
                // Bias the region data for loaded chunks
                List<Vector3i> positions = new ArrayList<Vector3i>();
                for (Chunk chunk : world.getLoadedChunks()) {
                    positions.add(chunk.getPosition());
                }
                manager.loadChunks(positions);
            }
        }

        return manager;
    }

    /**
     * Unload the region data for a world.
     *
     * @param world a world
     */
    void unload(World world) {
        checkNotNull(world);

        synchronized (lock) {
            container.unload(world.getName());
        }
    }

    /**
     * Get the region manager for a world if one exists.
     *
     * <p>If you wish to make queries and performance is more important
     * than accuracy, use {@link #createQuery()} instead.</p>
     *
     * <p>This method may return {@code null} if region data for the given
     * world has not been loaded, has failed to load, or support for regions
     * has been disabled.</p>
     *
     * @param world the world
     * @return a region manager, or {@code null} if one is not available
     */
    @Nullable
    public RegionManager get(World world) {
        return container.get(world.getName());
    }

    /**
     * Get an immutable list of loaded {@link RegionManager}s.
     *
     * @return a list of managers
     */
    public List<RegionManager> getLoaded() {
        return Collections.unmodifiableList(container.getLoaded());
    }

    /**
     * Get the a set of region managers that are failing to save.
     *
     * @return a set of region managers
     */
    public Set<RegionManager> getSaveFailures() {
        return container.getSaveFailures();
    }

    /**
     * Create a new region query.
     *
     * @return a new query
     */
    public RegionQuery createQuery() {
        return new RegionQuery(plugin, cache);
    }

    /**
     * Execute a migration and block any loading of region data during
     * the migration.
     *
     * @param migration the migration
     * @throws MigrationException thrown by the migration on error
     */
    public void migrate(Migration migration) throws MigrationException {
        checkNotNull(migration);

        synchronized (lock) {
            try {
                log.info("Unloading and saving region data that is currently loaded...");
                unload();
                migration.migrate();
            } finally {
                log.info("Loading region data for loaded worlds...");
                loadWorlds();
            }
        }
    }

    /**
     * Execute auto-migration.
     */
    private void autoMigrate() {
        ConfigurationManager config = plugin.getGlobalStateManager();

        if (config.migrateRegionsToUuid) {
            RegionDriver driver = getDriver();
            UUIDMigration migrator = new UUIDMigration(driver, plugin.getProfileService());
            migrator.setKeepUnresolvedNames(config.keepUnresolvedNames);
            try {
                migrate(migrator);

                log.info("Regions saved after UUID migration! This won't happen again unless " +
                        "you change the relevant configuration option in WorldGuard's config.");

                config.disableUuidMigration();
            } catch (MigrationException e) {
                log.log(Level.WARNING, "Failed to execute the migration", e);
            }
        }
    }

}
