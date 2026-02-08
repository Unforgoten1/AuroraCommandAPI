package dev.aurora.pagination;

import net.md_5.bungee.api.chat.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles sending clickable pagination navigation in chat using Spigot's chat components.
 * Falls back to simple text for console senders.
 */
public class ChatPaginator {

    /**
     * Send clickable navigation buttons to a sender.
     *
     * @param sender The command sender
     * @param currentPage Current page number
     * @param totalPages Total number of pages
     * @param command Base command (e.g., "/list")
     * @param previousLabel Previous button label
     * @param nextLabel Next button label
     */
    public static void sendNavigation(CommandSender sender, int currentPage, int totalPages,
                                     String command, String previousLabel, String nextLabel) {
        if (sender instanceof Player) {
            try {
                sendClickableNavigation((Player) sender, currentPage, totalPages, command, previousLabel, nextLabel);
            } catch (Exception e) {
                // Fallback to text if Spigot API not available
                sendTextNavigation(sender, currentPage, totalPages, previousLabel, nextLabel);
            }
        } else {
            sendTextNavigation(sender, currentPage, totalPages, previousLabel, nextLabel);
        }
    }

    /**
     * Send clickable navigation to a player using Spigot's chat components.
     *
     * @param player The player
     * @param currentPage Current page number
     * @param totalPages Total number of pages
     * @param command Base command
     * @param previousLabel Previous button label
     * @param nextLabel Next button label
     */
    @SuppressWarnings("deprecation")
    private static void sendClickableNavigation(Player player, int currentPage, int totalPages,
                                                String command, String previousLabel, String nextLabel) {
        boolean hasPrevious = currentPage > 1;
        boolean hasNext = currentPage < totalPages;

        // Build component array
        TextComponent previousComponent;
        if (hasPrevious) {
            previousComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', previousLabel));
            previousComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command + " " + (currentPage - 1)));
            previousComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new BaseComponent[]{new TextComponent("§7Click to go to page " + (currentPage - 1))}));
        } else {
            previousComponent = new TextComponent(ChatColor.DARK_GRAY + "[< Previous]");
        }

        // Page info
        TextComponent pageInfo = new TextComponent("  §7Page §e" + currentPage + "§7/§e" + totalPages + "  ");

        // Next button
        TextComponent nextComponent;
        if (hasNext) {
            nextComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', nextLabel));
            nextComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command + " " + (currentPage + 1)));
            nextComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new BaseComponent[]{new TextComponent("§7Click to go to page " + (currentPage + 1))}));
        } else {
            nextComponent = new TextComponent(ChatColor.DARK_GRAY + "[Next >]");
        }

        // Send as array
        player.spigot().sendMessage(previousComponent, pageInfo, nextComponent);
    }

    /**
     * Send simple text navigation to a sender (console fallback).
     *
     * @param sender The command sender
     * @param currentPage Current page number
     * @param totalPages Total number of pages
     * @param previousLabel Previous button label
     * @param nextLabel Next button label
     */
    private static void sendTextNavigation(CommandSender sender, int currentPage, int totalPages,
                                          String previousLabel, String nextLabel) {
        StringBuilder nav = new StringBuilder("§7");

        if (currentPage > 1) {
            nav.append(previousLabel).append("  ");
        }

        nav.append("Page ").append(currentPage).append("/").append(totalPages);

        if (currentPage < totalPages) {
            nav.append("  ").append(nextLabel);
        }

        sender.sendMessage(nav.toString());
    }

    /**
     * Create a simple paginated list and send it.
     *
     * @param sender The command sender
     * @param items List of items to display
     * @param page Current page (1-indexed)
     * @param itemsPerPage Items per page
     * @param command Command for navigation
     * @param <T> Item type
     */
    public static <T> void sendSimple(CommandSender sender, java.util.List<T> items, int page,
                                     int itemsPerPage, String command) {
        PaginationBuilder.create(items)
                .itemsPerPage(itemsPerPage)
                .command(command)
                .send(sender, page);
    }
}

