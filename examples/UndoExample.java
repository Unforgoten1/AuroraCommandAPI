package examples;

import dev.aurora.Command.AuroraCommand;
import dev.aurora.Manager.CommandManager;
import dev.aurora.struct.Types.Integers.IntegerArgumentType;
import dev.aurora.struct.Types.Item.ItemStackArgumentType;
import dev.aurora.struct.Types.Player.OnlinePlayerArgumentType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Example demonstrating command history and undo functionality.
 * Players can undo their recent commands with /undo.
 */
public class UndoExample {

    public static void register(CommandManager manager, JavaPlugin plugin) {

        // Example 1: Undoable give command
        new AuroraCommand("give", manager)
                .setDescription("Give items to a player (undoable)")
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
                // Make it undoable - define what happens when undone
                .makeUndoable((sender, context) -> {
                    Player target = (Player) context.getArgument("player");
                    ItemStack item = (ItemStack) context.getArgument("item");
                    int amount = (int) context.getArgument("amount");

                    // Return undo action - removes the items
                    return () -> {
                        ItemStack removeItem = item.clone();
                        removeItem.setAmount(amount);
                        target.getInventory().removeItem(removeItem);
                        target.sendMessage("§cItems removed by undo!");
                        sender.sendMessage("§aUndid giving items to " + target.getName());
                    };
                })
                .register();

        // Example 2: Undoable teleport command
        new AuroraCommand("tp", manager)
                .setDescription("Teleport to a player (undoable)")
                .setPermission("server.tp")
                .addArgument("source", new OnlinePlayerArgumentType())
                .addArgument("destination", new OnlinePlayerArgumentType())
                .addExecution((sender, context) -> {
                    Player source = (Player) context.getArgument("source");
                    Player destination = (Player) context.getArgument("destination");

                    // Store original location in context for undo
                    Location originalLocation = source.getLocation().clone();
                    context.addArgument("originalLocation", originalLocation);

                    source.teleport(destination.getLocation());
                    source.sendMessage("§aTeleported to " + destination.getName());
                    sender.sendMessage("§aTeleported " + source.getName() + " to " + destination.getName());
                })
                // Make it undoable - teleport back to original location
                .makeUndoable((sender, context) -> {
                    Player source = (Player) context.getArgument("source");
                    Location originalLocation = (Location) context.getArgument("originalLocation");

                    if (originalLocation != null) {
                        return () -> {
                            source.teleport(originalLocation);
                            source.sendMessage("§aTeleported back to original location!");
                            sender.sendMessage("§aUndid teleport for " + source.getName());
                        };
                    }
                    return null; // Cannot undo if location not stored
                })
                .register();

        // Example 3: Undoable gamemode command
        new AuroraCommand("gamemode", manager)
                .setDescription("Change gamemode (undoable)")
                .setPermission("server.gamemode")
                .addArgument("mode", new IntegerArgumentType()) // 0=survival, 1=creative, etc.
                .addArgument("player", new OnlinePlayerArgumentType())
                .addExecution((sender, context) -> {
                    int mode = (int) context.getArgument("mode");
                    Player target = (Player) context.getArgument("player");

                    // Store original gamemode for undo
                    org.bukkit.GameMode originalMode = target.getGameMode();
                    context.addArgument("originalMode", originalMode);

                    // Set new gamemode
                    org.bukkit.GameMode newMode = org.bukkit.GameMode.values()[mode];
                    target.setGameMode(newMode);
                    target.sendMessage("§aGamemode set to " + newMode.name());
                    sender.sendMessage("§aSet " + target.getName() + "'s gamemode to " + newMode.name());
                })
                // Make it undoable - restore original gamemode
                .makeUndoable((sender, context) -> {
                    Player target = (Player) context.getArgument("player");
                    org.bukkit.GameMode originalMode = (org.bukkit.GameMode) context.getArgument("originalMode");

                    return () -> {
                        target.setGameMode(originalMode);
                        target.sendMessage("§aGamemode restored to " + originalMode.name() + "!");
                        sender.sendMessage("§aRestored " + target.getName() + "'s gamemode");
                    };
                })
                .register();

        // Example 4: Command that is NOT undoable (doesn't call makeUndoable)
        new AuroraCommand("heal", manager)
                .setDescription("Heal a player (not undoable)")
                .setPermission("server.heal")
                .addArgument("player", new OnlinePlayerArgumentType())
                .addExecution((sender, context) -> {
                    Player target = (Player) context.getArgument("player");
                    target.setHealth(target.getMaxHealth());
                    target.setFoodLevel(20);
                    target.sendMessage("§aYou have been healed!");
                    sender.sendMessage("§aHealed " + target.getName());
                })
                // Notice: NO .makeUndoable() call - this command cannot be undone
                .register();

        /*
         * HOW TO USE:
         *
         * 1. Execute an undoable command:
         *    /give Player123 DIAMOND 64
         *
         * 2. View your command history:
         *    /history
         *    Shows:
         *    0. /give Player123 DIAMOND 64 (5s ago) [Can undo]
         *
         * 3. Undo the last command:
         *    /undo
         *    Removes the 64 diamonds from Player123
         *
         * 4. Undo a specific command by index:
         *    /undo 0
         *    Undoes command #0 from history
         *
         * UNDO DESIGN PATTERNS:
         *
         * 1. Store Original State:
         *    - Save original values in context during execution
         *    - Restore them in undo action
         *    - Example: originalLocation, originalGameMode
         *
         * 2. Inverse Operation:
         *    - Give items -> Remove items
         *    - Add money -> Subtract money
         *    - Teleport to -> Teleport back
         *
         * 3. Snapshot Pattern:
         *    - Take snapshot before changes
         *    - Restore snapshot on undo
         *
         * 4. When NOT to make undoable:
         *    - Commands that broadcast to all players
         *    - Commands that affect global state
         *    - Commands where "undo" doesn't make logical sense
         *    - Example: /heal (you can't "unheal")
         *
         * BUILT-IN COMMANDS:
         * - /undo - Undo your last command
         * - /undo <number> - Undo a specific command
         * - /history - View your command history
         *
         * FEATURES:
         * - Per-player history (50 commands per player)
         * - Thread-safe with ConcurrentHashMap
         * - Automatic cleanup of old entries
         * - History shows timestamps
         * - Shows which commands can be undone
         * - Shows which commands have been undone (strikethrough)
         */

        plugin.getLogger().info("╔════════════════════════════════════╗");
        plugin.getLogger().info("║  Undo Examples Registered!         ║");
        plugin.getLogger().info("║  Try: /give Player123 DIAMOND 64   ║");
        plugin.getLogger().info("║  Then: /undo                       ║");
        plugin.getLogger().info("║  View: /history                    ║");
        plugin.getLogger().info("╚════════════════════════════════════╝");
    }
}
