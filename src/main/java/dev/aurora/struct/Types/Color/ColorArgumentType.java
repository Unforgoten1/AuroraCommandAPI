package dev.aurora.struct.Types.Color;

import dev.aurora.exception.ArgumentParseException;
import dev.aurora.struct.ArgumentType;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * ArgumentType for parsing color values.
 * Supports three formats:
 * 1. Hex colors: "#FF5733" or "FF5733"
 * 2. ChatColor names: "RED", "BLUE", etc. (returns Bukkit Color equivalent)
 * 3. RGB values: "255,87,51"
 *
 * Returns: org.bukkit.Color object
 */
public class ColorArgumentType implements ArgumentType<Color> {

    @Override
    public String getName() {
        return "color";
    }

    @Override
    public Color parse(CommandSender sender, String input) throws ArgumentParseException {
        if (input == null || input.trim().isEmpty()) {
            throw new ArgumentParseException("Color cannot be empty!");
        }

        input = input.trim();

        // Try hex format (#FF5733 or FF5733)
        if (input.startsWith("#") || (input.length() == 6 && isHex(input))) {
            try {
                String hex = input.startsWith("#") ? input.substring(1) : input;
                int rgb = Integer.parseInt(hex, 16);
                return Color.fromRGB(rgb);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        "Invalid hex color '" + input + "'! Use format #RRGGBB or RRGGBB",
                        "color", input, "Example: #FF5733 or FF5733"
                );
            }
        }

        // Try RGB format (255,87,51)
        if (input.contains(",")) {
            try {
                String[] parts = input.split(",");
                if (parts.length != 3) {
                    throw new ArgumentParseException(
                            "RGB color must have 3 values separated by commas!",
                            "color", input, "Example: 255,87,51"
                    );
                }
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());

                if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
                    throw new ArgumentParseException(
                            "RGB values must be between 0 and 255!",
                            "color", input
                    );
                }

                return Color.fromRGB(r, g, b);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        "Invalid RGB color '" + input + "'!",
                        "color", input, "Example: 255,87,51"
                );
            }
        }

        // Try ChatColor name (convert to approximate RGB)
        try {
            ChatColor chatColor = ChatColor.valueOf(input.toUpperCase());
            return chatColorToRGB(chatColor);
        } catch (IllegalArgumentException e) {
            throw new ArgumentParseException(
                    "Invalid color '" + input + "'! Use hex (#FF5733), RGB (255,87,51), or color name (RED, BLUE, etc.)",
                    "color", input
            );
        }
    }

    @Override
    public List<String> getCompletions(CommandSender sender) {
        List<String> completions = new ArrayList<>();
        // Add common color names
        completions.add("RED");
        completions.add("GREEN");
        completions.add("BLUE");
        completions.add("YELLOW");
        completions.add("ORANGE");
        completions.add("PURPLE");
        completions.add("WHITE");
        completions.add("BLACK");
        completions.add("GRAY");
        // Add hex format example
        completions.add("#FF5733");
        // Add RGB format example
        completions.add("255,87,51");
        return completions;
    }

    /**
     * Checks if a string is a valid hexadecimal color code.
     *
     * @param str The string to check
     * @return True if valid hex, false otherwise
     */
    private boolean isHex(String str) {
        return str.matches("[0-9A-Fa-f]{6}");
    }

    /**
     * Converts a ChatColor to an approximate Bukkit Color (RGB).
     * Note: ChatColor doesn't have exact RGB values, so these are approximations.
     *
     * @param chatColor The ChatColor to convert
     * @return The approximate Color
     */
    private Color chatColorToRGB(ChatColor chatColor) {
        switch (chatColor) {
            case BLACK:
                return Color.fromRGB(0, 0, 0);
            case DARK_BLUE:
                return Color.fromRGB(0, 0, 170);
            case DARK_GREEN:
                return Color.fromRGB(0, 170, 0);
            case DARK_AQUA:
                return Color.fromRGB(0, 170, 170);
            case DARK_RED:
                return Color.fromRGB(170, 0, 0);
            case DARK_PURPLE:
                return Color.fromRGB(170, 0, 170);
            case GOLD:
                return Color.fromRGB(255, 170, 0);
            case GRAY:
                return Color.fromRGB(170, 170, 170);
            case DARK_GRAY:
                return Color.fromRGB(85, 85, 85);
            case BLUE:
                return Color.fromRGB(85, 85, 255);
            case GREEN:
                return Color.fromRGB(85, 255, 85);
            case AQUA:
                return Color.fromRGB(85, 255, 255);
            case RED:
                return Color.fromRGB(255, 85, 85);
            case LIGHT_PURPLE:
                return Color.fromRGB(255, 85, 255);
            case YELLOW:
                return Color.fromRGB(255, 255, 85);
            case WHITE:
                return Color.fromRGB(255, 255, 255);
            default:
                return Color.fromRGB(255, 255, 255); // Default to white
        }
    }
}
