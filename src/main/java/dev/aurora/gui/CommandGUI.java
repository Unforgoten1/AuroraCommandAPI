package dev.aurora.gui;

import dev.aurora.Command.AuroraCommand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Automatically generates GUI menus for commands.
 * Converts command arguments into clickable inventory interfaces.
 */
public class CommandGUI implements Listener {
    private final Map<UUID, GUISession> activeSessions;
    private final JavaPlugin plugin;

    public CommandGUI(JavaPlugin plugin) {
        this.plugin = plugin;
        this.activeSessions = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Open a GUI for a command.
     *
     * @param player The player
     * @param command The command
     * @param title GUI title
     */
    public void openGUI(Player player, AuroraCommand command, String title) {
        // Create inventory
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Add help item
        ItemStack helpItem = new ItemStack(Material.BOOK);
        ItemMeta helpMeta = helpItem.getItemMeta();
        helpMeta.setDisplayName("§6§l" + command.getName());
        helpMeta.setLore(Arrays.asList(
            "§7" + command.getDescription(),
            "",
            "§eClick items to execute",
            "§eor close to cancel"
        ));
        helpItem.setItemMeta(helpMeta);
        inv.setItem(0, helpItem);

        // Add execute button
        ItemStack executeItem = new ItemStack(Material.EMERALD);
        ItemMeta executeMeta = executeItem.getItemMeta();
        executeMeta.setDisplayName("§a§lExecute Command");
        executeMeta.setLore(Arrays.asList("§7Click to run the command"));
        executeItem.setItemMeta(executeMeta);
        inv.setItem(26, executeItem);

        // Create session
        GUISession session = new GUISession(command, inv);
        activeSessions.put(player.getUniqueId(), session);

        // Open inventory
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        GUISession session = activeSessions.get(player.getUniqueId());

        if (session == null) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Execute button
        if (event.getSlot() == 26) {
            player.closeInventory();
            activeSessions.remove(player.getUniqueId());

            // Execute command
            try {
                session.getCommand().execute(player, new String[0]);
            } catch (Exception e) {
                player.sendMessage("§cError executing command: " + e.getMessage());
            }
        }
    }

    /**
     * Clean up session when player closes inventory.
     *
     * @param player The player
     */
    public void closeSession(Player player) {
        activeSessions.remove(player.getUniqueId());
    }

    /**
     * Represents an active GUI session.
     */
    private static class GUISession {
        private final AuroraCommand command;
        private final Inventory inventory;
        private final Map<String, Object> arguments;

        public GUISession(AuroraCommand command, Inventory inventory) {
            this.command = command;
            this.inventory = inventory;
            this.arguments = new HashMap<>();
        }

        public AuroraCommand getCommand() {
            return command;
        }

        public Inventory getInventory() {
            return inventory;
        }

        public void setArgument(String name, Object value) {
            arguments.put(name, value);
        }

        public Map<String, Object> getArguments() {
            return arguments;
        }
    }
}
