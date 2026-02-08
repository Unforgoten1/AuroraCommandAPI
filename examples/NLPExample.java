package examples;

import dev.aurora.Command.AuroraCommand;
import dev.aurora.Manager.CommandManager;
import dev.aurora.struct.Types.Integers.IntegerArgumentType;
import dev.aurora.struct.Types.Item.ItemStackArgumentType;
import dev.aurora.struct.Types.Player.OnlinePlayerArgumentType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Example demonstrating Natural Language Processing (NLP) for commands.
 * Players can use natural language in chat instead of formal command syntax.
 *
 * Examples:
 *   Chat: "aurora give me 64 diamonds"
 *   Command executed: /give Player123 DIAMOND 64
 *
 *   Chat: "aurora heal me"
 *   Command executed: /heal Player123
 *
 *   Chat: "aurora teleport me to Player456"
 *   Command executed: /tp Player123 Player456
 */
public class NLPExample {

    public static void register(CommandManager manager, JavaPlugin plugin) {

        // Example 1: Give command with NLP
        new AuroraCommand("give", manager)
                .setDescription("Give items to a player")
                .setPermission("server.give")
                .addArgument("player", new OnlinePlayerArgumentType())
                .addArgument("item", new ItemStackArgumentType())
                .addArgument("amount", new IntegerArgumentType())
                .addExecution((sender, context) -> {
                    Player target = (Player) context.getArgument("player");
                    ItemStack item = (ItemStack) context.getArgument("item");
                    int amount = (int) context.getArgument("amount");

                    item.setAmount(amount);
                    target.getInventory().addItem(item);
                    target.sendMessage("§aYou received §e" + amount + " " + item.getType() + "§a!");
                    sender.sendMessage("§aGave §e" + amount + " " + item.getType() + " §ato " + target.getName());
                })
                // Add NLP patterns
                .addNLPPattern("give me {amount} {item}")
                .addNLPPattern("give {player} {amount} {item}")
                .addNLPPattern("give {amount} {item} to {player}")
                .register();

        // Example 2: Heal command with NLP
        new AuroraCommand("heal", manager)
                .setDescription("Heal a player")
                .setPermission("server.heal")
                .addArgument("player", new OnlinePlayerArgumentType())
                .addExecution((sender, context) -> {
                    Player target = (Player) context.getArgument("player");
                    target.setHealth(target.getMaxHealth());
                    target.setFoodLevel(20);
                    target.sendMessage("§aYou have been healed!");
                    if (!sender.equals(target)) {
                        sender.sendMessage("§aHealed " + target.getName());
                    }
                })
                // Add NLP patterns
                .addNLPPattern("heal me")
                .addNLPPattern("heal {player}")
                .register();

        // Example 3: Teleport command with NLP
        new AuroraCommand("tp", manager)
                .setDescription("Teleport to a player")
                .setPermission("server.tp")
                .addArgument("source", new OnlinePlayerArgumentType())
                .addArgument("destination", new OnlinePlayerArgumentType())
                .addExecution((sender, context) -> {
                    Player source = (Player) context.getArgument("source");
                    Player destination = (Player) context.getArgument("destination");

                    source.teleport(destination.getLocation());
                    source.sendMessage("§aTeleported to " + destination.getName());
                    sender.sendMessage("§aTeleported " + source.getName() + " to " + destination.getName());
                })
                // Add NLP patterns
                .addNLPPattern("teleport me to {destination}")
                .addNLPPattern("tp to {destination}")
                .addNLPPattern("teleport {source} to {destination}")
                .register();

        // Example 4: Feed command with NLP
        new AuroraCommand("feed", manager)
                .setDescription("Feed a player")
                .setPermission("server.feed")
                .addArgument("player", new OnlinePlayerArgumentType())
                .addExecution((sender, context) -> {
                    Player target = (Player) context.getArgument("player");
                    target.setFoodLevel(20);
                    target.setSaturation(20.0f);
                    target.sendMessage("§aYou have been fed!");
                    if (!sender.equals(target)) {
                        sender.sendMessage("§aFed " + target.getName());
                    }
                })
                // Add NLP patterns
                .addNLPPattern("feed me")
                .addNLPPattern("feed {player}")
                .addNLPPattern("give me food")
                .register();

        /*
         * HOW TO USE:
         *
         * 1. The trigger prefix is "aurora" by default
         * 2. Players type in chat (not as a command):
         *    - "aurora give me 64 diamonds"
         *    - "aurora heal me"
         *    - "aurora teleport me to Player456"
         *
         * 3. The system automatically:
         *    - Detects the NLP trigger
         *    - Parses the natural language
         *    - Extracts entities (players, numbers, items)
         *    - Normalizes synonyms ("diamonds" -> "DIAMOND")
         *    - Generates formal command
         *    - Executes the command
         *
         * SUPPORTED ENTITY TYPES:
         * - {player} - Player names, "me", "myself"
         * - {item} - Materials, with synonyms (diamonds, emeralds, etc.)
         * - {amount} - Numbers, including words (one, two, stack=64, etc.)
         * - {world} - World names
         * - {location} - Locations
         *
         * CONFIGURATION:
         * - Enable/disable NLP: manager.setNLPEnabled(true/false)
         * - Change trigger prefix: Create new NLPParser with custom prefix
         * - Add custom synonyms: manager.getNLPParser().getSynonyms().addSynonym(...)
         * - Add custom patterns: command.addNLPPattern(...)
         *
         * BENEFITS:
         * - More intuitive for players
         * - Reduces command memorization
         * - Feels like talking to an AI assistant
         * - Works alongside traditional commands
         */

        plugin.getLogger().info("╔════════════════════════════════════╗");
        plugin.getLogger().info("║  NLP Examples Registered!          ║");
        plugin.getLogger().info("║  Try: aurora give me 64 diamonds   ║");
        plugin.getLogger().info("║  Try: aurora heal me               ║");
        plugin.getLogger().info("║  Try: aurora tp to Player123       ║");
        plugin.getLogger().info("╚════════════════════════════════════╝");
    }
}
