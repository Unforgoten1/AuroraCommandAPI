package dev.aurora.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a command definition loaded from YAML/JSON.
 * Contains all configuration for a command including execution logic.
 */
public class CommandDefinition {
    private String name;
    private String description;
    private String permission;
    private List<String> aliases;
    private long cooldown;
    private List<ArgumentDefinition> arguments;
    private List<ExecutionAction> actions;
    private boolean async;
    private boolean requiresPlayer;
    private Map<String, String> customMessages;
    private List<String> subcommands;

    public CommandDefinition() {
        this.aliases = new ArrayList<>();
        this.arguments = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.customMessages = new HashMap<>();
        this.subcommands = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public long getCooldown() {
        return cooldown;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }

    public List<ArgumentDefinition> getArguments() {
        return arguments;
    }

    public void setArguments(List<ArgumentDefinition> arguments) {
        this.arguments = arguments;
    }

    public List<ExecutionAction> getActions() {
        return actions;
    }

    public void setActions(List<ExecutionAction> actions) {
        this.actions = actions;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public boolean isRequiresPlayer() {
        return requiresPlayer;
    }

    public void setRequiresPlayer(boolean requiresPlayer) {
        this.requiresPlayer = requiresPlayer;
    }

    public Map<String, String> getCustomMessages() {
        return customMessages;
    }

    public void setCustomMessages(Map<String, String> customMessages) {
        this.customMessages = customMessages;
    }

    public List<String> getSubcommands() {
        return subcommands;
    }

    public void setSubcommands(List<String> subcommands) {
        this.subcommands = subcommands;
    }

    /**
     * Represents an argument definition in YAML/JSON.
     */
    public static class ArgumentDefinition {
        private String name;
        private String type;
        private Object defaultValue;
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * Represents an execution action in YAML/JSON.
     */
    public static class ExecutionAction {
        private String type;
        private String target;
        private String text;
        private Map<String, Object> parameters;

        public ExecutionAction() {
            this.parameters = new HashMap<>();
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        public Object getParameter(String key) {
            return parameters.get(key);
        }
    }
}
