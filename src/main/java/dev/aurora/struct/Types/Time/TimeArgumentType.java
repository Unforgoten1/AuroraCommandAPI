package dev.aurora.struct.Types.Time;

import dev.aurora.exception.ArgumentParseException;
import dev.aurora.struct.ArgumentType;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ArgumentType for parsing time duration strings into milliseconds or ticks.
 * Supports formats like:
 * - "5s" = 5 seconds
 * - "10m" = 10 minutes
 * - "2h" = 2 hours
 * - "1d" = 1 day
 * - "1h30m" = 1 hour and 30 minutes
 * - "2d5h30m15s" = 2 days, 5 hours, 30 minutes, 15 seconds
 *
 * Returns: Long (milliseconds) or Integer (ticks) based on constructor flag
 */
public class TimeArgumentType implements ArgumentType<Long> {
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhd])");
    private final boolean returnTicks;

    /**
     * Creates a TimeArgumentType that returns milliseconds.
     */
    public TimeArgumentType() {
        this(false);
    }

    /**
     * Creates a TimeArgumentType with configurable output format.
     *
     * @param returnTicks If true, returns Minecraft ticks (20 ticks = 1 second). If false, returns milliseconds.
     */
    public TimeArgumentType(boolean returnTicks) {
        this.returnTicks = returnTicks;
    }

    @Override
    public String getName() {
        return returnTicks ? "timeTicks" : "timeMillis";
    }

    @Override
    public Long parse(CommandSender sender, String input) throws ArgumentParseException {
        if (input == null || input.trim().isEmpty()) {
            throw new ArgumentParseException("Time duration cannot be empty!");
        }

        input = input.toLowerCase().replaceAll("\\s+", "");
        Matcher matcher = TIME_PATTERN.matcher(input);

        long totalMillis = 0;
        boolean foundMatch = false;

        while (matcher.find()) {
            foundMatch = true;
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "s": // seconds
                    totalMillis += amount * 1000;
                    break;
                case "m": // minutes
                    totalMillis += amount * 60 * 1000;
                    break;
                case "h": // hours
                    totalMillis += amount * 60 * 60 * 1000;
                    break;
                case "d": // days
                    totalMillis += amount * 24 * 60 * 60 * 1000;
                    break;
            }
        }

        if (!foundMatch) {
            throw new ArgumentParseException(
                    "Invalid time format '" + input + "'! Use formats like: 5s, 10m, 2h, 1d, or 1h30m",
                    "time", input, "Example: '5m' for 5 minutes, '2h30m' for 2 hours 30 minutes"
            );
        }

        if (returnTicks) {
            // Convert milliseconds to Minecraft ticks (1 tick = 50ms, 20 ticks = 1 second)
            return totalMillis / 50;
        }

        return totalMillis;
    }

    @Override
    public List<String> getCompletions(CommandSender sender) {
        List<String> completions = new ArrayList<>();
        completions.add("30s");
        completions.add("1m");
        completions.add("5m");
        completions.add("10m");
        completions.add("30m");
        completions.add("1h");
        completions.add("2h");
        completions.add("12h");
        completions.add("1d");
        completions.add("7d");
        return completions;
    }

    /**
     * Checks if this argument type returns ticks instead of milliseconds.
     *
     * @return True if returning ticks, false if returning milliseconds
     */
    public boolean isReturnTicks() {
        return returnTicks;
    }
}
