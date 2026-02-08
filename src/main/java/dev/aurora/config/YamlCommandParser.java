package dev.aurora.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Parses YAML files into CommandDefinition objects.
 * Supports loading commands from YAML configuration files.
 */
public class YamlCommandParser {
    private final Logger logger;

    public YamlCommandParser() {
        this.logger = Logger.getLogger("AuroraCommands");
    }

    /**
     * Parse a YAML file into a CommandDefinition.
     *
     * @param file The YAML file to parse
     * @return CommandDefinition, or null if parsing fails
     */
    public CommandDefinition parse(File file) {
        if (!file.exists()) {
            logger.warning("Command file not found: " + file.getPath());
            return null;
        }

        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            CommandDefinition def = new CommandDefinition();

            // Required fields
            def.setName(yaml.getString("name"));
            if (def.getName() == null || def.getName().isEmpty()) {
                logger.severe("Command name is required in " + file.getName());
                return null;
            }

            // Optional fields
            def.setDescription(yaml.getString("description", "A configurable command"));
            def.setPermission(yaml.getString("permission"));
            def.setCooldown(yaml.getLong("cooldown", 0));
            def.setAsync(yaml.getBoolean("async", false));
            def.setRequiresPlayer(yaml.getBoolean("requiresPlayer", false));

            // Aliases
            if (yaml.contains("aliases")) {
                def.setAliases(yaml.getStringList("aliases"));
            }

            // Arguments
            if (yaml.contains("arguments")) {
                List<CommandDefinition.ArgumentDefinition> arguments = new ArrayList<>();
                List<?> argList = yaml.getList("arguments");

                if (argList != null) {
                    for (Object argObj : argList) {
                        if (argObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> argMap = (Map<String, Object>) argObj;
                            CommandDefinition.ArgumentDefinition argDef = new CommandDefinition.ArgumentDefinition();

                            argDef.setName((String) argMap.get("name"));
                            argDef.setType((String) argMap.get("type"));
                            argDef.setDefaultValue(argMap.get("default"));
                            argDef.setDescription((String) argMap.get("description"));

                            if (argDef.getName() == null || argDef.getType() == null) {
                                logger.warning("Argument missing name or type in " + file.getName());
                                continue;
                            }

                            arguments.add(argDef);
                        }
                    }
                }
                def.setArguments(arguments);
            }

            // Execution actions
            if (yaml.contains("execution")) {
                ConfigurationSection execution = yaml.getConfigurationSection("execution");
                if (execution != null && execution.contains("actions")) {
                    List<CommandDefinition.ExecutionAction> actions = new ArrayList<>();
                    List<?> actionList = execution.getList("actions");

                    if (actionList != null) {
                        for (Object actionObj : actionList) {
                            if (actionObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> actionMap = (Map<String, Object>) actionObj;
                                CommandDefinition.ExecutionAction action = new CommandDefinition.ExecutionAction();

                                action.setType((String) actionMap.get("type"));
                                action.setTarget((String) actionMap.get("target"));
                                action.setText((String) actionMap.get("text"));

                                // Copy all other parameters
                                Map<String, Object> params = new HashMap<>();
                                for (Map.Entry<String, Object> entry : actionMap.entrySet()) {
                                    if (!entry.getKey().equals("type") &&
                                        !entry.getKey().equals("target") &&
                                        !entry.getKey().equals("text")) {
                                        params.put(entry.getKey(), entry.getValue());
                                    }
                                }
                                action.setParameters(params);

                                actions.add(action);
                            }
                        }
                    }
                    def.setActions(actions);
                }
            }

            // Custom messages
            if (yaml.contains("messages")) {
                ConfigurationSection messages = yaml.getConfigurationSection("messages");
                if (messages != null) {
                    Map<String, String> customMessages = new HashMap<>();
                    for (String key : messages.getKeys(false)) {
                        customMessages.put(key, messages.getString(key));
                    }
                    def.setCustomMessages(customMessages);
                }
            }

            // Subcommands
            if (yaml.contains("subcommands")) {
                def.setSubcommands(yaml.getStringList("subcommands"));
            }

            logger.info("Successfully parsed command definition: " + def.getName() + " from " + file.getName());
            return def;

        } catch (Exception e) {
            logger.severe("Failed to parse command file " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
