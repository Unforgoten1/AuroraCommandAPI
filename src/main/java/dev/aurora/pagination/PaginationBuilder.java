package dev.aurora.pagination;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.function.Function;

/**
 * Fluent builder for configuring pagination display.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * PaginationBuilder.create(playerList)
 *     .itemsPerPage(10)
 *     .header("§6===== Players =====")
 *     .footer("§7Use /list <page> to navigate")
 *     .formatter(player -> "§e- " + player.getName())
 *     .command("/list")
 *     .send(sender, 1);
 * }</pre>
 *
 * @param <T> The type of items to paginate
 */
public class PaginationBuilder<T> {
    private final List<T> items;
    private int itemsPerPage = 10;
    private String header = null;
    private String footer = null;
    private Function<T, String> formatter = Object::toString;
    private String command = null;
    private String nextLabel = "§a[Next >]";
    private String previousLabel = "§a[< Previous]";
    private String pageInfo = "§7Page §e%current%§7/§e%total%";

    private PaginationBuilder(List<T> items) {
        this.items = items;
    }

    /**
     * Create a new pagination builder.
     *
     * @param items The items to paginate
     * @param <T> The item type
     * @return New builder
     */
    public static <T> PaginationBuilder<T> create(List<T> items) {
        return new PaginationBuilder<>(items);
    }

    /**
     * Set the number of items per page.
     *
     * @param itemsPerPage Items per page
     * @return This builder
     */
    public PaginationBuilder<T> itemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
        return this;
    }

    /**
     * Set the header text displayed above the list.
     *
     * @param header Header text (null for no header)
     * @return This builder
     */
    public PaginationBuilder<T> header(String header) {
        this.header = header;
        return this;
    }

    /**
     * Set the footer text displayed below the list.
     *
     * @param footer Footer text (null for no footer)
     * @return This builder
     */
    public PaginationBuilder<T> footer(String footer) {
        this.footer = footer;
        return this;
    }

    /**
     * Set the formatter function to convert items to strings.
     *
     * @param formatter Function to format each item
     * @return This builder
     */
    public PaginationBuilder<T> formatter(Function<T, String> formatter) {
        this.formatter = formatter;
        return this;
    }

    /**
     * Set the command for navigation (e.g., "/list").
     * If set, clickable navigation will be added.
     *
     * @param command The base command
     * @return This builder
     */
    public PaginationBuilder<T> command(String command) {
        this.command = command;
        return this;
    }

    /**
     * Set the "Next" button label.
     *
     * @param label Next button text
     * @return This builder
     */
    public PaginationBuilder<T> nextLabel(String label) {
        this.nextLabel = label;
        return this;
    }

    /**
     * Set the "Previous" button label.
     *
     * @param label Previous button text
     * @return This builder
     */
    public PaginationBuilder<T> previousLabel(String label) {
        this.previousLabel = label;
        return this;
    }

    /**
     * Set the page info format.
     * Use %current% and %total% placeholders.
     *
     * @param format Page info format
     * @return This builder
     */
    public PaginationBuilder<T> pageInfo(String format) {
        this.pageInfo = format;
        return this;
    }

    /**
     * Send the paginated list to a sender.
     *
     * @param sender The command sender
     * @param page The page number (1-indexed)
     */
    public void send(CommandSender sender, int page) {
        PaginatedList<T> paginated = new PaginatedList<>(items, itemsPerPage);

        // Validate page
        if (!paginated.isValidPage(page)) {
            sender.sendMessage("§cInvalid page number! Valid pages: 1-" + paginated.getTotalPages());
            return;
        }

        // Send header
        if (header != null) {
            sender.sendMessage(header);
        }

        // Send page info
        String info = pageInfo
                .replace("%current%", String.valueOf(page))
                .replace("%total%", String.valueOf(paginated.getTotalPages()));
        sender.sendMessage(info);

        // Send items
        List<T> pageItems = paginated.getPage(page);
        for (T item : pageItems) {
            sender.sendMessage(formatter.apply(item));
        }

        // Send navigation (clickable if command is set)
        if (command != null) {
            ChatPaginator.sendNavigation(sender, page, paginated.getTotalPages(), command, previousLabel, nextLabel);
        } else {
            // Simple text navigation
            StringBuilder nav = new StringBuilder("§7");
            if (paginated.hasPreviousPage(page)) {
                nav.append(previousLabel).append("  ");
            }
            nav.append("Page ").append(page).append("/").append(paginated.getTotalPages());
            if (paginated.hasNextPage(page)) {
                nav.append("  ").append(nextLabel);
            }
            sender.sendMessage(nav.toString());
        }

        // Send footer
        if (footer != null) {
            sender.sendMessage(footer);
        }
    }
}
