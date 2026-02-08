package dev.aurora.config;

import dev.aurora.struct.CommandContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

/**
 * Executes commands from configuration files.
 * Supports various action types like messages, healing, teleporting, etc.
 */
public class ConfigurableExecutor implements BiConsumer<CommandSender, CommandContext> {
    private final List<CommandDefinition.ExecutionAction> actions;
    private final Logger logger;

    public ConfigurableExecutor(List<CommandDefinition.ExecutionAction> actions) {
        this.actions = actions;
        this.logger = Logger.getLogger("AuroraCommands");
    }

    @Override
    public void accept(CommandSender sender, CommandContext context) {
        for (CommandDefinition.ExecutionAction action : actions) {
            try {
                executeAction(action, sender, context);
            } catch (Exception e) {
                logger.severe("Error executing action " + action.getType() + ": " + e.getMessage());
                sender.sendMessage("§cError executing command action!");
            }
        }
    }

    /**
     * Execute a single action.
     *
     * @param action The action to execute
     * @param sender The command sender
     * @param context The command context
     */
    private void executeAction(CommandDefinition.ExecutionAction action, CommandSender sender, CommandContext context) {
        String type = action.getType();
        if (type == null) {
            logger.warning("Action type is null, skipping");
            return;
        }

        switch (type.toLowerCase()) {
            case "message":
                executeMessage(action, sender, context);
                break;

            case "broadcast":
                executeBroadcast(action, sender, context);
                break;

            case "heal":
                executeHeal(action, sender, context);
                break;

            case "feed":
                executeFeed(action, sender, context);
                break;

            case "teleport":
                executeTeleport(action, sender, context);
                break;

            case "command":
                executeCommand(action, sender, context);
                break;

            case "effect":
                executeEffect(action, sender, context);
                break;

            case "give":
                executeGive(action, sender, context);
                break;

            default:
                logger.warning("Unknown action type: " + type);
                break;
        }
    }

    /**
     * Execute a message action.
     */
    private void executeMessage(CommandDefinition.ExecutionAction action, CommandSender sender, CommandContext context) {
        String text = replacePlaceholders(action.getText(), sender, context);
        String target = action.getTarget();

        if (target == null || target.equalsIgnoreCase("sender")) {
            sender.sendMessage(text);
        } else if (target.equalsIgnoreCase("all")) {
            Bukkit.broadcastMessage(text);
        } else {
            Player player = Bukkit.getPlayer(target);
            if (player != null) {
                player.sendMessage(text);
            }
        }
    }

    /**
     * Execute a broadcast action.
     */
    private void executeBroadcast(CommandDefinition.ExecutionAction action, CommandSender sender, CommandContext context) {
        String text = replacePlaceholders(action.getText(), sender, context);
        Bukkit.broadcastMessage(text);
    }

    /**
     * Execute a heal action.
     */
    private void executeHeal(CommandDefinition.ExecutionAction action, CommandSender sender, CommandContext context) {
        Player target = getTargetPlayer(action.getTarget(), sender, context);
        if (target != null) {
            target.setHealth(target.getMaxHealth());
            logger.info("Healed player: " + target.getName());
        }
    }

    /**
     * Execute a feed action.
     */
    private void executeFeed(CommandDefinition.ExecutionAction action, CommandSender sender, CommandContext context) {
        Player target = getTargetPlayer(action.getTarget(), sender, context);
        if (target != null) {
            target.setFoodLevel(20);
            target.setSaturation(20.0f);
            logger.info("Fed player: " + target.getName());
        }
    }

    /**
     * Execute a teleport action.
     */
    private void executeTeleport(CommandDefinition.ExecutionAction action, CommandSender sender, CommandContext context) {
        Player target = getTargetPlayer(action.getTarget(), sender, context);
        if (target == null) return;

        Object locationParam = action.getParameter("location");
        if (locationParam != null) {
            Location location = (Location) context.getArgument(locationParam.toString());
            if (location != null) {
                target.teleport(location);
                logger.info("Teleported " + target.getName() + " to location");
            }
        }
    }

    /**
     * Execute a command action.
     */
    private void executeCommand(CommandDefinition.ExecutionAction action, CommandSender sender, CommandContext context) {
        String command = replacePlaceholders(action.getText(), sender, context);
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        logger.info("Executed command: /" + command);
    }

    /**
     * Execute an effect action.
     */
    private void executeEffect(CommandDefinition.ExecutionAction action, CommandSender sender, CommandContext context) {
        Player target = getTargetPlayer(action.getTarget(), sender, context);
        if (target == null) return;

        Object effectParam = action.getParameter("effect");
        if (effectParam != null) {
            @SuppressWarnings("deprecation")
            PotionEffectType effectType = PotionEffectType.getByName(effectParam.toString());
            if (effectType != null) {
                int duration = getIntParameter(action, "duration", 200);
                int amplifier = getIntParameter(action, "amplifier", 0);
                target.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
                logger.info("Applied effect " + effectType.getName() + " to " + target.getName());
            }
        }
    }

    /**
     * Execute a give action.
     */
    private void executeGive(CommandDefinition.ExecutionAction action, CommandSender sender, CommandContext context) {
        Player target = getTargetPlayer(action.getTarget(), sender, context);
        if (target == null) return;

        // This would require ItemStack parsing - simplified version
        logger.info("Give action would give items to " + target.getName());
        // Implementation depends on existing ItemStackArgumentType
    }

    /**
     * Get the target player for an action.
     */
    private Player getTargetPlayer(String target, CommandSender sender, CommandContext context) {
        if (target == null || target.equalsIgnoreCase("sender")) {
            if (sender instanceof Player) {
                return (Player) sender;
            } else {
                logger.warning("Cannot target sender - sender is not a player");
                return null;
            }
        }

        // Check if target is an argument name
        Object argValue = context.getArgument(target);
        if (argValue instanceof Player) {
            return (Player) argValue;
        }

        // Try to find player by name
        Player player = Bukkit.getPlayer(target);
        if (player == null) {
            logger.warning("Target player not found: " + target);
        }
        return player;
    }

    /**
     * Replace placeholders in text.
     */
    private String replacePlaceholders(String text, CommandSender sender, CommandContext context) {
        if (text == null) return "";

        text = text.replace("{sender}", sender.getName());

        // Replace argument placeholders
        for (Map.Entry<String, Object> entry : context.getArguments().entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "null";
            text = text.replace(placeholder, value);
        }

        // Color codes
        text = text.replace("&", "§");

        return text;
    }

    /**
     * Get an integer parameter from action.
     */
    private int getIntParameter(CommandDefinition.ExecutionAction action, String key, int defaultValue) {
        Object param = action.getParameter(key);
        if (param instanceof Number) {
            return ((Number) param).intValue();
        }
        return defaultValue;
    }
}
