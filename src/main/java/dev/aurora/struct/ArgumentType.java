package dev.aurora.struct;

import dev.aurora.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ArgumentType<T> {
    String getName();
    T parse(CommandSender sender, String input) throws ArgumentParseException;
    List<String> getCompletions(CommandSender sender);

    /**
     * Get tab completions asynchronously.
     * Default implementation wraps the synchronous getCompletions().
     * Override this for expensive operations like database queries.
     *
     * @param sender The command sender
     * @return CompletableFuture with the completions
     */
    default CompletableFuture<List<String>> getCompletionsAsync(CommandSender sender) {
        return CompletableFuture.completedFuture(getCompletions(sender));
    }
}