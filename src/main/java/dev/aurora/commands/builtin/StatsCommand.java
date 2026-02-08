package dev.aurora.commands.builtin;

import dev.aurora.Command.AuroraCommand;
import dev.aurora.Manager.CommandManager;
import dev.aurora.metrics.CommandMetrics;
import dev.aurora.metrics.MetricData;
import dev.aurora.struct.CommandContext;
import dev.aurora.struct.Types.Strings.StringArgumentType;
import org.bukkit.command.CommandSender;

/**
 * Built-in command to display command execution metrics.
 * Usage: /commandstats [commandName]
 */
public class StatsCommand {

    /**
     * Register the /commandstats command.
     *
     * @param manager Command manager
     * @param metrics Metrics instance
     */
    public static void register(CommandManager manager, CommandMetrics metrics) {
        AuroraCommand statsCommand = new AuroraCommand("commandstats", manager)
                .setDescription("View command execution statistics")
                .setPermission("aurora.admin.stats")
                .addArgument("command", new StringArgumentType())
                .addExecution((sender, context) -> {
                    String commandName = context.getArgument("command");

                    if (commandName == null) {
                        // Show overall summary
                        sender.sendMessage(metrics.getSummaryReport());
                    } else {
                        // Show specific command stats
                        MetricData data = metrics.getMetrics(commandName);

                        if (data.getTotalExecutions() == 0) {
                            sender.sendMessage("§cNo metrics found for command: " + commandName);
                            return;
                        }

                        sender.sendMessage(data.getSummary());
                    }
                });

        manager.registerCommand(statsCommand);
    }
}
