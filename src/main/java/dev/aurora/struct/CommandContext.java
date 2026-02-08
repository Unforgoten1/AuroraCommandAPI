package dev.aurora.struct;

import dev.aurora.flags.CommandFlags;
import dev.aurora.pagination.PaginationBuilder;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CommandContext {
    private final Map<String, Object> arguments;
    private String commandName;

    public CommandContext() {
        this.arguments = new HashMap<>();
    }

    public void addArgument(String name, Object value) {
        arguments.put(name, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getArgument(String name) {
        return (T) arguments.get(name);
    }

    public Map<String, Object> getArguments(){
        return new HashMap<>(arguments);
    }

    /**
     * Get a flag value by name.
     * Convenience method for accessing flag values.
     *
     * @param flagName The flag name
     * @param <T> The value type
     * @return The flag value, or null if not set
     */
    @SuppressWarnings("unchecked")
    public <T> T getFlag(String flagName) {
        return getArgument("flag_" + flagName);
    }

    /**
     * Check if a flag is set (present in the command).
     *
     * @param flagName The flag name
     * @return True if the flag was set
     */
    public boolean hasFlag(String flagName) {
        CommandFlags.ParsedFlags flags = getArgument("flags");
        return flags != null && flags.has(flagName);
    }

    /**
     * Get the command name.
     *
     * @return The command name
     */
    public String getCommandName() {
        return commandName;
    }

    /**
     * Set the command name.
     *
     * @param commandName The command name
     */
    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    /**
     * Send a paginated list to the command sender.
     *
     * @param items List of items to paginate
     * @param page Page number (1-indexed)
     * @param itemsPerPage Items per page
     * @param command Base command for navigation
     * @param <T> Item type
     */
    public <T> void sendPaginated(List<T> items, int page, int itemsPerPage, String command) {
        CommandSender sender = getArgument("sender");
        if (sender != null) {
            PaginationBuilder.create(items)
                    .itemsPerPage(itemsPerPage)
                    .command(command)
                    .send(sender, page);
        }
    }

    /**
     * Send a paginated list with custom formatting.
     *
     * @param items List of items to paginate
     * @param page Page number (1-indexed)
     * @param itemsPerPage Items per page
     * @param command Base command for navigation
     * @param formatter Function to format each item
     * @param <T> Item type
     */
    public <T> void sendPaginatedFormatted(List<T> items, int page, int itemsPerPage,
                                          String command, Function<T, String> formatter) {
        CommandSender sender = getArgument("sender");
        if (sender != null) {
            PaginationBuilder.create(items)
                    .itemsPerPage(itemsPerPage)
                    .command(command)
                    .formatter(formatter)
                    .send(sender, page);
        }
    }

    /**
     * Send a paginated list with full customization.
     *
     * @param items List of items to paginate
     * @param page Page number (1-indexed)
     * @param itemsPerPage Items per page
     * @param command Base command for navigation
     * @param header Header text
     * @param footer Footer text
     * @param formatter Function to format each item
     * @param <T> Item type
     */
    public <T> void sendPaginatedCustom(List<T> items, int page, int itemsPerPage,
                                       String command, String header, String footer,
                                       Function<T, String> formatter) {
        CommandSender sender = getArgument("sender");
        if (sender != null) {
            PaginationBuilder.create(items)
                    .itemsPerPage(itemsPerPage)
                    .command(command)
                    .header(header)
                    .footer(footer)
                    .formatter(formatter)
                    .send(sender, page);
        }
    }
}