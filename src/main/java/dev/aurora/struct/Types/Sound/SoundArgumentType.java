package dev.aurora.struct.Types.Sound;

import dev.aurora.exception.ArgumentParseException;
import dev.aurora.struct.ArgumentType;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * ArgumentType for parsing Bukkit Sound enum values.
 * Note: Sound enum was added in Minecraft 1.9. For 1.8 servers, this will gracefully fail.
 *
 * @since Minecraft 1.9+
 */
public class SoundArgumentType implements ArgumentType<Sound> {
    private static final boolean SOUND_AVAILABLE;

    static {
        boolean available;
        try {
            Class.forName("org.bukkit.Sound");
            available = true;
        } catch (ClassNotFoundException e) {
            available = false;
        }
        SOUND_AVAILABLE = available;
    }

    @Override
    public String getName() {
        return "sound";
    }

    @Override
    public Sound parse(CommandSender sender, String input) throws ArgumentParseException {
        if (!SOUND_AVAILABLE) {
            throw new ArgumentParseException("Sound enum is not available on this server version (requires Minecraft 1.9+)");
        }

        if (input == null || input.trim().isEmpty()) {
            throw new ArgumentParseException("Sound name cannot be empty!");
        }

        try {
            // Try direct enum parsing
            return Sound.valueOf(input.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // Try case-insensitive match
            for (Sound sound : Sound.values()) {
                if (sound.name().equalsIgnoreCase(input.replace(" ", "_"))) {
                    return sound;
                }
            }
            throw new ArgumentParseException("Sound '" + input + "' not found!");
        }
    }

    @Override
    public List<String> getCompletions(CommandSender sender) {
        List<String> completions = new ArrayList<>();
        if (!SOUND_AVAILABLE) {
            return completions;
        }

        for (Sound sound : Sound.values()) {
            completions.add(sound.name());
        }
        return completions;
    }

    /**
     * Checks if the Sound enum is available on this server version.
     *
     * @return True if Sound is available (Minecraft 1.9+), false otherwise
     */
    public static boolean isSoundAvailable() {
        return SOUND_AVAILABLE;
    }
}
