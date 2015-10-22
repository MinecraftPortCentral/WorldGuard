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
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.LazyLocation;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.handler.GameModeFlag;
import com.sk89q.worldguard.sponge.ConfigurationManager;
import com.sk89q.worldguard.sponge.WorldConfiguration;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import com.sk89q.worldguard.sponge.event.player.ProcessPlayerEvent;
import com.sk89q.worldguard.sponge.permission.RegionPermissionModel;
import com.sk89q.worldguard.sponge.util.Causes;
import com.sk89q.worldguard.sponge.util.Entities;
import com.sk89q.worldguard.sponge.util.Events;
import com.sk89q.worldguard.util.command.CommandFilter;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.command.MessageSinkEvent;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.living.human.ChangeGameModeEvent;
import org.spongepowered.api.event.entity.living.player.RespawnPlayerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.source.ConsoleSource;
import org.spongepowered.api.util.command.source.LocatedSource;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Handles all events thrown in relation to a player.
 */
public class WorldGuardPlayerListener extends AbstractListener {

    private static final Logger log = Logger.getLogger(WorldGuardPlayerListener.class.getCanonicalName());
    private static final Pattern opPattern = Pattern.compile("^/op(\\s.*)?", Pattern.CASE_INSENSITIVE);

    /**
     * Construct the object;
     *
     * @param plugin
     */
    public WorldGuardPlayerListener(WorldGuardPlugin plugin) {
        super(plugin);
    }

    @Listener
    public void onPlayerGameModeChange(ChangeGameModeEvent.TargetPlayer event) {
        Player player = event.getTargetEntity();
        WorldConfiguration wcfg = getWorldConfig(player);
        GameModeFlag handler = getPlugin().getSessionManager().get(player).getHandler(GameModeFlag.class);
        if (handler != null && wcfg.useRegions && !(new RegionPermissionModel(getPlugin(), player).mayIgnoreRegionProtection(player.getWorld()))) {
            GameMode expected = handler.getSetGameMode();
            if (handler.getOriginalGameMode() != null && expected != null && expected != event.getGameMode()) {
                log.info("Game mode change on " + player.getName() + " has been blocked due to the region GAMEMODE flag");
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        Player player = event.getTargetEntity();
        World world = player.getWorld();

        ConfigurationManager cfg = getPlugin().getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(world);

        if (cfg.activityHaltToggle) {
            player.sendMessage(Texts.of(TextColors.YELLOW, "Intensive server activity has been HALTED."));
            int removed = 0;

            for (Entity entity : world.getEntities()) {
                if (Entities.isIntensiveEntity(entity)) {
                    entity.remove();
                    removed++;
                }
            }

            if (removed > 10) {
                log.info("Halt-Act: " + removed + " entities (>10) auto-removed from " + player.getWorld().getName());
            }
        }

        if (wcfg.fireSpreadDisableToggle) {
            player.sendMessage(Texts.of(TextColors.YELLOW, "Fire spread is currently globally disabled for this world."));
        }

        Events.fire(new ProcessPlayerEvent(player));

        getPlugin().getSessionManager().get(player); // Initializes a session
    }

    @Listener
    public void onPlayerChat(MessageSinkEvent.Chat event) {
        if (!event.getCause().any(Player.class)) {
            return;
        }
        Player player = event.getCause().first(Player.class).get();
        WorldConfiguration wcfg = getPlugin().getGlobalStateManager().get(player.getWorld());
        if (wcfg.useRegions) {
            if (!getPlugin().getRegionContainer().createQuery().testState(player.getLocation(), player, DefaultFlag.SEND_CHAT)) {
                player.sendMessage(Texts.of(TextColors.RED, "You don't have permission to chat in this region!"));
                event.setCancelled(true);
                return;
            }

            for (Iterator<CommandSource> i = event.getSink().getRecipients().iterator(); i.hasNext();) {
                CommandSource cs = i.next();
                if (cs instanceof LocatedSource) {
                    if (!getPlugin().getRegionContainer().createQuery()
                            .testState(((LocatedSource) cs).getLocation(), (Player) null, DefaultFlag.RECEIVE_CHAT)) {
                        i.remove();
                    }
                }
            }
        }
    }

    @Listener
    public void onPlayerLogin(ClientConnectionEvent.Auth event) {
        ConfigurationManager cfg = getPlugin().getGlobalStateManager();

        String hostKey = cfg.hostKeys.get(event.getProfile().getName().toLowerCase());
        if (hostKey != null) {
            String hostname = event.getConnection().getVirtualHost().getHostName();
            int colonIndex = hostname.indexOf(':');
            if (colonIndex != -1) {
                hostname = hostname.substring(0, colonIndex);
            }

            if (!hostname.equals(hostKey)) {
                event.setCancelled(true);
                event.setMessage(Texts.of("You did not join with the valid host key!"));
                log.warning("WorldGuard host key check: " + event.getProfile().getName() + " joined with '" + hostname + "' but '" + hostKey
                        + "' was expected. Kicked!");
                return;
            }
        }

        if (cfg.deopOnJoin) {
            // TODO sponge?
            // player.setOp(false);
        }
    }

    @Listener
    public void onPlayerInteract(InteractBlockEvent event) {
        if (event.getCause().first(Player.class).isPresent()) {
            Player player = (Player) event.getCause().first(Player.class).get();
            checkInfiniteStacks(player);
        }
    }

    @Listener
    public void onPlayerInteract(InteractEntityEvent event) {
        if (event.getCause().first(Player.class).isPresent()) {
            Player player = (Player) event.getCause().first(Player.class).get();
            checkInfiniteStacks(player);
        }
    }

    private void checkInfiniteStacks(Player player) {
        if (getWorldConfig(player).removeInfiniteStacks && !getPlugin().hasPermission(player, "worldguard.override.infinite-stack")) {
            Optional<ItemStack> item = player.getItemInHand();
            if (!item.isPresent())
                return;
            ItemStack heldItem = item.get();
            if (heldItem != null && heldItem.getQuantity() < 0) {
                player.setItemInHand(null);
                player.sendMessage(Texts.of(TextColors.RED, "Infinite stack removed."));
            }
            // TODO this is probably inefficient to do on every interact
            for (Slot slot : player.getInventory().) {
                if (slot.getStackSize() < 0) {
                    slot.clear();
                }
            }
        }
    }

    /**
     * Called when a player right clicks a block.
     *
     * @param event Thrown event
     */
    @Listener
    public void handleBlockRightClick(InteractBlockEvent.Secondary event) {
        if (event.getCause().first(Player.class).isPresent()) {
            Player player = (Player) event.getCause().first(Player.class).get();
            if (event.isCancelled()) {
                return;
            }
            World world = player.getWorld();
            Location<World> block = event.getTargetBlock().getLocation().get();

            WorldConfiguration wcfg = getWorldConfig(world);

            if (wcfg.useRegions) {
                ApplicableRegionSet set = getPlugin().getRegionContainer().createQuery().getApplicableRegions(block);
                LocalPlayer localPlayer = getPlugin().wrapPlayer(player);

                if (player.getItemInHand().isPresent() && player.getItemInHand().get().getItem().equals(wcfg.regionWand)
                        && getPlugin().hasPermission(player, "worldguard.region.wand")) {
                    if (set.size() > 0) {
                        player.sendMessage(Texts.of(TextColors.YELLOW, "Can you build? "
                                + (set.testState(localPlayer, DefaultFlag.BUILD) ? "Yes" : "No")));

                        StringBuilder str = new StringBuilder();
                        for (Iterator<ProtectedRegion> it = set.iterator(); it.hasNext();) {
                            str.append(it.next().getId());
                            if (it.hasNext()) {
                                str.append(", ");
                            }
                        }

                        player.sendMessage(Texts.of(TextColors.YELLOW, "Applicable regions: " + str));
                    } else {
                        player.sendMessage(Texts.of(TextColors.YELLOW, "WorldGuard: No defined regions here!"));
                    }

                    event.setCancelled(true);
                }
            }
        }
    }

    @Listener
    public void onPlayerRespawn(RespawnPlayerEvent event) {
        Player player = event.getTargetEntity();
        Location location = player.getLocation();

        WorldConfiguration wcfg = getWorldConfig(player);

        if (wcfg.useRegions) {
            ApplicableRegionSet set = getPlugin().getRegionContainer().createQuery().getApplicableRegions(location);

            LocalPlayer localPlayer = getPlugin().wrapPlayer(player);
            LazyLocation spawn = set.queryValue(localPlayer, DefaultFlag.SPAWN_LOC);

            if (spawn != null) {
                event.setToTransform(spawn.getTransform());
            }
        }
    }

    @Listener
    public void onPlayerTeleport(DisplaceEntityEvent.Teleport event) {
        if (!(event.getTargetEntity() instanceof Player)) {
            return;
        }
        World world = event.getFromTransform().getExtent();
        Player player = ((Player) event.getTargetEntity());
        ConfigurationManager cfg = getPlugin().getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(world);

        if (wcfg.useRegions) {
            ApplicableRegionSet set = getPlugin().getRegionContainer().createQuery().getApplicableRegions(event.getToTransform().getLocation());
            ApplicableRegionSet setFrom = getPlugin().getRegionContainer().createQuery().getApplicableRegions(event.getFromTransform().getLocation());
            LocalPlayer localPlayer = getPlugin().wrapPlayer(player);

            if (cfg.usePlayerTeleports) {
                if (null != getPlugin().getSessionManager().get(player).testMoveTo(player, event.getToTransform(), MoveType.TELEPORT)) {
                    event.setCancelled(true);
                    return;
                }
            }

            // TODO sponge see what sponge does with cause when this gets thrown
            if (Causes.isEnderPearlTeleport(event.getCause())) {
                if (!(new RegionPermissionModel(getPlugin(), player).mayIgnoreRegionProtection(world))
                        && !(set.testState(localPlayer, DefaultFlag.ENDERPEARL) && setFrom.testState(localPlayer, DefaultFlag.ENDERPEARL))) {
                    player.sendMessage(Texts.of(TextColors.DARK_RED, "You're not allowed to go there."));
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @Listener
    public void onCommand(SendCommandEvent event) {
        if (event.getCause().first(CommandSource.class).isPresent()) {
            CommandSource cs = (CommandSource) event.getCause().first(CommandSource.class).get();
            ConfigurationManager cfg = getPlugin().getGlobalStateManager();

            if (cfg.blockInGameOp && !(cs instanceof ConsoleSource)) {
                if (opPattern.matcher(event.getCommand()).matches()) {
                    cs.sendMessage(Texts.of(TextColors.RED, "/op can only be used in console (as set by a WG setting)."));
                    event.setCancelled(true);
                    return;
                }
            }

            if (!(cs instanceof LocatedSource))
                return;
            LocatedSource ls = (LocatedSource) cs;
            World world = ls.getWorld();
            WorldConfiguration wcfg = cfg.get(world);

            if (wcfg.useRegions && !(new RegionPermissionModel(getPlugin(), cs).mayIgnoreRegionProtection(world))) {
                ApplicableRegionSet set = getPlugin().getRegionContainer().createQuery().getApplicableRegions(ls.getLocation());

                LocalPlayer localPlayer = null;
                if (ls instanceof Player) {
                    localPlayer = getPlugin().wrapPlayer(((Player) ls));
                }
                Set<String> allowedCommands = set.queryValue(localPlayer, DefaultFlag.ALLOWED_CMDS);
                Set<String> blockedCommands = set.queryValue(localPlayer, DefaultFlag.BLOCKED_CMDS);
                CommandFilter test = new CommandFilter(allowedCommands, blockedCommands);

                if (!test.apply(event.getCommand() + " " + event.getArguments())) {
                    cs.sendMessage(Texts.of(TextColors.RED, " '" + event.getCommand() + " " + event.getArguments() + "' is not allowed in this area."));
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
