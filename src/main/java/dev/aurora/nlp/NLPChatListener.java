package dev.aurora.nlp;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Listens to chat messages and converts NLP commands into executable commands.
 *
 * Example:
 *   Player types in chat: "aurora give me 64 diamonds"
 *   Listener converts to command: "/give Player123 DIAMOND 64"
 *   Command is executed automatically
 */
public class NLPChatListener implements Listener {
    private final NLPParser parser;
    private final JavaPlugin plugin;
    private final Logger logger;
    private boolean enabled;

    /**
     * Create an NLP chat listener.
     *
     * @param parser The NLP parser
     * @param plugin The plugin instance
     */
    public NLPChatListener(NLPParser parser, JavaPlugin plugin) {
        this.parser = parser;
        this.plugin = plugin;
        this.logger = Logger.getLogger("AuroraCommands");
        this.enabled = true;
    }

    /**
     * Handle player chat messages.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();

        // Check if this is an NLP command
        if (!parser.isNLPCommand(message)) {
            return;
        }

        // Cancel the chat event (don't broadcast the command)
        event.setCancelled(true);

        logger.info("NLP command detected from " + player.getName() + ": " + message);

        // Parse the NLP command
        String command = parser.parse(player, message);

        if (command == null) {
            player.sendMessage("§cI didn't understand that command. Try rephrasing?");
            player.sendMessage("§7Example: §e" + parser.getTriggerPrefix() + " give me 64 diamonds");
            logger.info("Failed to parse NLP command: " + message);
            return;
        }

        // Execute the command on the main thread
        final String finalCommand = command;
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                logger.info("Executing NLP-generated command: /" + finalCommand);

                // Execute as player
                boolean success = Bukkit.dispatchCommand(player, finalCommand);

                if (!success) {
                    player.sendMessage("§cCommand execution failed!");
                    logger.warning("NLP command execution failed: /" + finalCommand);
                }
            } catch (Exception e) {
                player.sendMessage("§cError executing command!");
                logger.severe("Error executing NLP command: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Enable the NLP chat listener.
     */
    public void enable() {
        this.enabled = true;
        logger.info("NLP chat listener enabled");
    }

    /**
     * Disable the NLP chat listener.
     */
    public void disable() {
        this.enabled = false;
        logger.info("NLP chat listener disabled");
    }

    /**
     * Check if the listener is enabled.
     *
     * @return True if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the NLP parser.
     *
     * @return The parser
     */
    public NLPParser getParser() {
        return parser;
    }
}
