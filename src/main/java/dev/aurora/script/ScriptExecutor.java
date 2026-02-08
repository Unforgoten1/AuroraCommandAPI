package dev.aurora.script;

import dev.aurora.struct.CommandContext;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

/**
 * Executes scripts as command executors.
 */
public class ScriptExecutor implements BiConsumer<CommandSender, CommandContext> {
    private final String scriptCode;
    private final ScriptEngine scriptEngine;
    private final Logger logger;

    /**
     * Create a script executor.
     *
     * @param scriptCode The JavaScript code to execute
     * @param scriptEngine The script engine
     */
    public ScriptExecutor(String scriptCode, ScriptEngine scriptEngine) {
        this.scriptCode = scriptCode;
        this.scriptEngine = scriptEngine;
        this.logger = Logger.getLogger("AuroraCommands");
    }

    @Override
    public void accept(CommandSender sender, CommandContext context) {
        Object result = scriptEngine.executeScript(scriptCode, sender, context);
        if (result != null) {
            logger.fine("Script executed successfully: " + result);
        }
    }

    /**
     * Create a script executor from a file.
     *
     * @param scriptFile The script file
     * @param scriptEngine The script engine
     * @return ScriptExecutor, or null if file cannot be read
     */
    public static ScriptExecutor fromFile(File scriptFile, ScriptEngine scriptEngine) {
        try {
            String code = new String(Files.readAllBytes(scriptFile.toPath()));
            return new ScriptExecutor(code, scriptEngine);
        } catch (IOException e) {
            Logger.getLogger("AuroraCommands").severe("Failed to load script: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create a script executor from inline code.
     *
     * @param code The JavaScript code
     * @param scriptEngine The script engine
     * @return ScriptExecutor
     */
    public static ScriptExecutor fromCode(String code, ScriptEngine scriptEngine) {
        return new ScriptExecutor(code, scriptEngine);
    }
}
