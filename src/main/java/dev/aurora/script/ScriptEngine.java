package dev.aurora.script;

import dev.aurora.struct.CommandContext;
import org.bukkit.command.CommandSender;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.logging.Logger;

/**
 * Manages script execution for commands.
 * Supports JavaScript through Java's built-in ScriptEngine.
 */
public class ScriptEngine {
    private final javax.script.ScriptEngine jsEngine;
    private final Logger logger;

    public ScriptEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.jsEngine = manager.getEngineByName("javascript");
        this.logger = Logger.getLogger("AuroraCommands");

        if (jsEngine == null) {
            logger.warning("JavaScript engine not available. Script features will be limited.");
        }
    }

    /**
     * Execute a JavaScript script.
     *
     * @param script The script code
     * @param sender The command sender
     * @param context The command context
     * @return Script result, or null if failed
     */
    public Object executeScript(String script, CommandSender sender, CommandContext context) {
        if (jsEngine == null) {
            logger.severe("Cannot execute script: JavaScript engine not available");
            return null;
        }

        try {
            // Provide context to script
            jsEngine.put("sender", sender);
            jsEngine.put("context", context);
            jsEngine.put("server", sender.getServer());

            // Execute script
            return jsEngine.eval(script);
        } catch (ScriptException e) {
            logger.severe("Script execution error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if scripting is available.
     *
     * @return True if JavaScript engine is available
     */
    public boolean isAvailable() {
        return jsEngine != null;
    }
}
