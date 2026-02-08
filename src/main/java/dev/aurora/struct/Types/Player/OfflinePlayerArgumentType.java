package dev.aurora.struct.Types.Player;

import dev.aurora.exception.ArgumentParseException;
import dev.aurora.struct.ArgumentType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ArgumentType for parsing OfflinePlayer objects.
 * Searches online players first, then offline players by name or UUID.
 * Useful for ban/unban commands and other operations that work with offline players.
 */
public class OfflinePlayerArgumentType implements ArgumentType<OfflinePlayer> {

    @Override
    public String getName() {
        return "offlinePlayer";
    }

    @Override
    public OfflinePlayer parse(CommandSender sender, String input) throws ArgumentParseException {
        if (input == null || input.trim().isEmpty()) {
            throw new ArgumentParseException("Player name cannot be empty!");
        }

        // Try online players first (case-insensitive)
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getName().equalsIgnoreCase(input)) {
                return onlinePlayer;
            }
        }

        // Try parsing as UUID
        try {
            UUID uuid = UUID.fromString(input);
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (player.hasPlayedBefore() || player.isOnline()) {
                return player;
            }
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, continue with name lookup
        }

        // Get offline player by name
        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(input);

        // Note: getOfflinePlayer always returns a player object even if they've never played,
        // but we should still return it as the user might be referring to a future player
        return offlinePlayer;
    }

    @Override
    public List<String> getCompletions(CommandSender sender) {
        List<String> completions = new ArrayList<>();

        // Add online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            completions.add(player.getName());
        }

        // Add recently seen offline players (players who have played before)
        // Limited to avoid performance issues
        OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
        int count = 0;
        for (OfflinePlayer player : offlinePlayers) {
            if (count >= 50) break; // Limit to 50 offline players
            if (player.hasPlayedBefore() && !player.isOnline()) {
                String name = player.getName();
                if (name != null && !name.isEmpty()) {
                    completions.add(name);
                    count++;
                }
            }
        }

        return completions;
    }
}
