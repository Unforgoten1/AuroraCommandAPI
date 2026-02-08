package dev.aurora.help;

import dev.aurora.Command.AuroraCommand;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates formatted help text for commands.
 * Traverses the command tree and creates user-friendly help displays.
 */
public class HelpGenerator {

    /**
     * Generates help text for a command.
     *
     * @param command The command to generate help for
     * @param sender  The sender requesting help (for permission checks)
     * @return A list of formatted help lines
     */
    public static List<String> generateHelp(AuroraCommand command, CommandSender sender) {
        List<String> lines = new ArrayList<>();

        // Header
        lines.add("§6§l" + command.getName().toUpperCase() + " Command Help");
        lines.add("§7" + repeatChar('-', 40));

        // Usage
        lines.add("§eUsage: §f/" + command.getName() + " " + command.getUsage());

        // Aliases
        if (!command.getAliases().isEmpty()) {
            lines.add("§eAliases: §f" + String.join(", ", command.getAliases()));
        }

        // Description (if available - would need to add this field to AuroraCommand)
        // lines.add("§eDescription: §f" + command.getDescription());

        // Permission
        String permission = getPermission(command);
        if (permission != null && !permission.isEmpty()) {
            lines.add("§ePermission: §f" + permission);
        }

        // Subcommands
        List<AuroraCommand> subCommands = command.getSubCommands();
        if (!subCommands.isEmpty()) {
            lines.add("");
            lines.add("§6Subcommands:");
            for (AuroraCommand sub : subCommands) {
                // Only show subcommands the sender has permission for
                if (sub.hasPermission(sender)) {
                    String subHelp = "  §e/" + command.getName() + " " + sub.getName();
                    if (!sub.getUsage().isEmpty()) {
                        subHelp += " §7" + sub.getUsage();
                    }
                    lines.add(subHelp);
                }
            }
        }

        lines.add("§7" + repeatChar('-', 40));

        return lines;
    }

    /**
     * Generates a compact help message for multiple commands.
     *
     * @param commands List of commands to show
     * @param sender   The sender requesting help
     * @return A list of formatted help lines
     */
    public static List<String> generateCommandList(List<AuroraCommand> commands, CommandSender sender) {
        List<String> lines = new ArrayList<>();

        lines.add("§6§lAvailable Commands");
        lines.add("§7" + repeatChar('-', 40));

        for (AuroraCommand command : commands) {
            if (command.hasPermission(sender)) {
                lines.add("§e/" + command.getName() + " §7- §f" + command.getUsage());
            }
        }

        lines.add("§7" + repeatChar('-', 40));
        lines.add("§7Use §e/help <command> §7for more details");

        return lines;
    }

    /**
     * Sends help text to a command sender.
     *
     * @param command The command to show help for
     * @param sender  The sender to send help to
     */
    public static void sendHelp(AuroraCommand command, CommandSender sender) {
        for (String line : generateHelp(command, sender)) {
            sender.sendMessage(line);
        }
    }

    /**
     * Generates a simple usage string without color codes.
     *
     * @param command The command
     * @return Plain usage string
     */
    public static String generatePlainUsage(AuroraCommand command) {
        return "/" + command.getName() + " " + command.getUsage();
    }

    /**
     * Generates detailed argument information.
     *
     * @param command The command
     * @return List of argument descriptions
     */
    public static List<String> generateArgumentInfo(AuroraCommand command) {
        List<String> lines = new ArrayList<>();
        // This would require access to ArgumentEntry list which is private
        // For now, return basic info
        lines.add("§6Arguments:");
        lines.add("§7Use the command to see available arguments");
        return lines;
    }

    private static String getPermission(AuroraCommand command) {
        // Would need to add a getter for permission in AuroraCommand
        // For now, return null
        return null;
    }

    private static String repeatChar(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
