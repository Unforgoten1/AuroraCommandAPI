package examples;

import dev.aurora.Command.AuroraCommand;
import dev.aurora.Manager.CommandManager;
import dev.aurora.pipeline.PipelineContext;
import dev.aurora.struct.CommandContext;
import dev.aurora.struct.Types.World.WorldArgumentType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Example demonstrating command pipelines.
 * Pipelines allow chaining commands with the pipe operator (|).
 *
 * Example usage:
 *   /listin world | healall
 *   /findrich 1000 | tp spawn
 */
public class PipelineExample {

    public static void register(CommandManager manager, JavaPlugin plugin) {

        // Command 1: List players in a world
        // This is the "source" command that outputs a list of players
        new AuroraCommand("listin", manager)
                .setDescription("List all players in a world")
                .setPermission("server.listin")
                .addArgument("world", new WorldArgumentType())
                .addExecution((sender, context) -> {
                    World world = (World) context.getArgument("world");
                    Collection<? extends Player> players = world.getPlayers();

                    sender.sendMessage("§7Players in world §e" + world.getName() + "§7:");
                    for (Player player : players) {
                        sender.sendMessage("§7- " + player.getName());
                    }

                    sender.sendMessage("§7Total: §e" + players.size() + " §7players");
                })
                // Enable pipeline support with output extractor
                .enablePipeline(ctx -> {
                    // Extract the list of players to pass to next command
                    World world = (World) ctx.getArgument("world");
                    return new ArrayList<>(world.getPlayers());
                })
                .register();

        // Command 2: Heal all players (from pipeline input)
        // This command can receive a list of players from the previous stage
        new AuroraCommand("healall", manager)
                .setDescription("Heal all players (works in pipelines)")
                .setPermission("server.healall")
                .addExecution((sender, context) -> {
                    List<Player> playersToHeal;

                    // Check if this is a pipeline execution
                    if (context instanceof PipelineContext) {
                        PipelineContext pipeContext = (PipelineContext) context;
                        Object input = pipeContext.getPipelineInput();

                        if (input instanceof List) {
                            // Use players from pipeline
                            playersToHeal = (List<Player>) input;
                            sender.sendMessage("§aHealing §e" + playersToHeal.size() + " §aplayers from pipeline...");
                        } else {
                            sender.sendMessage("§cNo player list received from pipeline!");
                            return;
                        }
                    } else {
                        // Standalone execution - heal all online players
                        playersToHeal = new ArrayList<>(plugin.getServer().getOnlinePlayers());
                        sender.sendMessage("§aHealing all §e" + playersToHeal.size() + " §aonline players...");
                    }

                    // Heal all players
                    int healed = 0;
                    for (Player player : playersToHeal) {
                        player.setHealth(player.getMaxHealth());
                        player.setFoodLevel(20);
                        player.sendMessage("§aYou have been healed!");
                        healed++;
                    }

                    sender.sendMessage("§a✔ Healed " + healed + " players!");
                })
                .enablePipeline() // Enable pipeline support
                .register();

        // Command 3: Find players with money above threshold
        // Demonstrates filtering in pipelines
        new AuroraCommand("findrich", manager)
                .setDescription("Find players with balance above threshold")
                .setPermission("server.findrich")
                .addArgument("minBalance", new dev.aurora.struct.Types.Double.DoubleArgumentType())
                .addExecution((sender, context) -> {
                    double minBalance = (double) context.getArgument("minBalance");

                    // Simplified example - normally would check economy plugin
                    List<Player> richPlayers = new ArrayList<>();
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        // Simulate balance check
                        double balance = Math.random() * 10000;
                        if (balance >= minBalance) {
                            richPlayers.add(player);
                        }
                    }

                    sender.sendMessage("§7Found §e" + richPlayers.size() + " §7players with balance ≥ $" + minBalance);
                    for (Player player : richPlayers) {
                        sender.sendMessage("§7- " + player.getName());
                    }
                })
                .enablePipeline(ctx -> {
                    // Output list of rich players
                    double minBalance = (double) ctx.getArgument("minBalance");
                    List<Player> richPlayers = new ArrayList<>();
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        double balance = Math.random() * 10000;
                        if (balance >= minBalance) {
                            richPlayers.add(player);
                        }
                    }
                    return richPlayers;
                })
                .register();

        // Command 4: Teleport players (from pipeline)
        // Can receive a list of players and teleport them all
        new AuroraCommand("tpall", manager)
                .setDescription("Teleport players to a location")
                .setPermission("server.tpall")
                .addArgument("location", new dev.aurora.struct.Types.Location.LocationArgumentType())
                .addExecution((sender, context) -> {
                    org.bukkit.Location location = (org.bukkit.Location) context.getArgument("location");
                    List<Player> playersToTp;

                    // Check for pipeline input
                    if (context instanceof PipelineContext) {
                        PipelineContext pipeContext = (PipelineContext) context;
                        Object input = pipeContext.getPipelineInput();

                        if (input instanceof List) {
                            playersToTp = (List<Player>) input;
                        } else {
                            playersToTp = new ArrayList<>(plugin.getServer().getOnlinePlayers());
                        }
                    } else {
                        playersToTp = new ArrayList<>(plugin.getServer().getOnlinePlayers());
                    }

                    // Teleport all players
                    for (Player player : playersToTp) {
                        player.teleport(location);
                        player.sendMessage("§aTeleported!");
                    }

                    sender.sendMessage("§a✔ Teleported " + playersToTp.size() + " players!");
                })
                .enablePipeline()
                .register();

        /*
         * USAGE EXAMPLES:
         *
         * 1. List players in world "survival" and heal them all:
         *    /listin survival | healall
         *
         * 2. Find rich players and teleport them:
         *    /findrich 5000 | tpall 100 64 100
         *
         * 3. List players in "pvp" world and teleport them to spawn:
         *    /listin pvp | tpall spawn
         *
         * Benefits of pipelines:
         * - Reduces code duplication
         * - Allows flexible command composition
         * - Each command works standalone AND in pipelines
         * - Clean separation of concerns
         */
    }
}
