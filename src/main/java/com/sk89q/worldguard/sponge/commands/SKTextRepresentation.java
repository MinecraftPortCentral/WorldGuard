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

package com.sk89q.worldguard.sponge.commands;

import com.google.common.collect.Lists;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.TextRepresentation;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextFormat;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.TextMessageException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SKTextRepresentation implements TextRepresentation {

    public final static SKTextRepresentation INSTANCE = new SKTextRepresentation();
    private static final char legacyChar = '`';
    private static Map<Character, TextFormat> skCodes = new HashMap<Character, TextFormat>();

    static {
        skCodes.put('R', new TextFormat(TextColors.DARK_RED));
        skCodes.put('r', new TextFormat(TextColors.RED));
        skCodes.put('Y', new TextFormat(TextColors.YELLOW));
        skCodes.put('y', new TextFormat(TextColors.GOLD));
        skCodes.put('g', new TextFormat(TextColors.GREEN));
        skCodes.put('G', new TextFormat(TextColors.DARK_GREEN));
        skCodes.put('c', new TextFormat(TextColors.AQUA));
        skCodes.put('C', new TextFormat(TextColors.DARK_AQUA));
        skCodes.put('b', new TextFormat(TextColors.BLUE));
        skCodes.put('B', new TextFormat(TextColors.DARK_BLUE));
        skCodes.put('p', new TextFormat(TextColors.LIGHT_PURPLE));
        skCodes.put('P', new TextFormat(TextColors.DARK_PURPLE));
        skCodes.put('0', new TextFormat(TextColors.BLACK));
        skCodes.put('1', new TextFormat(TextColors.GRAY));
        skCodes.put('2', new TextFormat(TextColors.DARK_GRAY));
        skCodes.put('w', new TextFormat(TextColors.WHITE));
        skCodes.put('k', new TextFormat(TextStyles.OBFUSCATED));
        skCodes.put('l', new TextFormat(TextStyles.BOLD));
        skCodes.put('m', new TextFormat(TextStyles.STRIKETHROUGH));
        skCodes.put('n', new TextFormat(TextStyles.UNDERLINE));
        skCodes.put('o', new TextFormat(TextStyles.ITALIC));
        skCodes.put('x', new TextFormat(TextStyles.RESET));
    }

    @Override
    public String to(Text text) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String to(Text text, Locale locale) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Text from(String input) throws TextMessageException {
        // via LegacyTextRepresentation
        int next = input.lastIndexOf(this.legacyChar, input.length() - 2);
        if (next == -1) {
            return Texts.of(input);
        }

        List<Text> parts = Lists.newArrayList();

        TextBuilder.Literal current = null;
        boolean reset = false;

        int pos = input.length();
        do {
            TextFormat format = skCodes.get(input.charAt(next + 1));
            if (format != null) {
                int from = next + 2;
                if (from != pos) {
                    if (current != null) {
                        if (reset) {
                            parts.add(current.build());
                            reset = false;
                            current = Texts.builder("");
                        } else {
                            current = Texts.builder("").append(current.build());
                        }
                    } else {
                        current = Texts.builder("");
                    }

                    current.content(input.substring(from, pos));
                } else if (current == null) {
                    current = Texts.builder("");
                }

                reset |= applyStyle(current, format);
                pos = next;
            }

            next = input.lastIndexOf(this.legacyChar, next - 1);
        } while (next != -1);

        if (current != null) {
            parts.add(current.build());
        }

        Collections.reverse(parts);
        return Texts.builder(pos > 0 ? input.substring(0, pos) : "").append(parts).build();
    }

    @Override
    public Text fromUnchecked(String input) {
        try {
            return from(input);
        } catch (TextMessageException e) {
            return Texts.of(input);
        }
    }

    private static boolean applyStyle(TextBuilder builder, TextFormat format) {
        if (format.getStyle().equals(TextStyles.RESET)) {
            return true;
        } else {
            builder.style(format.getStyle());
            builder.color(format.getColor());
            return true;
        }
    }
}
