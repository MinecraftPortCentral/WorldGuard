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

package com.sk89q.worldguard.protection.flags;

import java.util.Optional;
import com.sk89q.worldguard.sponge.WorldGuardPlugin;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.util.command.CommandSource;

import java.util.Collection;

/**
 * Stores an enum value.
 */
public class EnumFlag<T extends CatalogType> extends Flag<T> {

    private Class<T> enumClass;

    public EnumFlag(String name, Class<T> enumClass, RegionGroup defaultGroup) {
        super(name, defaultGroup);
        this.enumClass = enumClass;
    }

    public EnumFlag(String name, Class<T> enumClass) {
        super(name);
        this.enumClass = enumClass;
    }

    /**
     * Get the enum class.
     *
     * @return the enum class
     */
    public Class<T> getEnumClass() {
        return enumClass;
    }

    private T findValue(String input) throws IllegalArgumentException {
        if (input != null) {
            input = input.toUpperCase();
        }

        try {
            Collection<T> allTypes = WorldGuardPlugin.inst().getGame().getRegistry().getAllOf(enumClass);
            for (T type : allTypes) {
                if (type.getName().equalsIgnoreCase(input)) {
                    return type;
                }
            }
            throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            T val = detectValue(input);

            if (val != null) {
                return val;
            }

            throw e;
        }
    }

    /**
     * Fuzzy detect the value if the value is not found.
     *
     * @param input string input
     * @return value or null
     */
    public T detectValue(String input) {
        return null;
    }

    @Override
    public T parseInput(WorldGuardPlugin plugin, CommandSource sender, String input) throws InvalidFlagFormat {
        try {
            return findValue(input);
        } catch (IllegalArgumentException e) {
            throw new InvalidFlagFormat("Unknown value '" + input + "' in "
                    + enumClass.getName());
        }
    }

    @Override
    public T unmarshal(Object o) {
        Optional<T> opt = WorldGuardPlugin.inst().getGame().getRegistry().getType(enumClass, String.valueOf(o));
        return opt.isPresent() ? opt.get() : null;
    }

    @Override
    public Object marshal(T o) {
        return o.getName();
    }

}
