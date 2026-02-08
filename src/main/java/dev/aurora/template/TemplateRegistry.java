package dev.aurora.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Registry for managing command templates.
 * Allows registering, retrieving, and applying templates.
 */
public class TemplateRegistry {
    private final Map<String, CommandTemplate> templates;
    private final Logger logger;

    public TemplateRegistry() {
        this.templates = new HashMap<>();
        this.logger = Logger.getLogger("AuroraCommands");
        registerBuiltInTemplates();
    }

    /**
     * Register built-in templates.
     */
    private void registerBuiltInTemplates() {
        register(CommandTemplate.ADMIN);
        register(CommandTemplate.PLAYER);
        register(CommandTemplate.DANGEROUS);
        register(CommandTemplate.ECONOMY);
        register(CommandTemplate.UTILITY);
        register(CommandTemplate.CONSOLE);
        logger.info("Registered " + templates.size() + " built-in command templates");
    }

    /**
     * Register a template.
     *
     * @param template The template to register
     */
    public void register(CommandTemplate template) {
        templates.put(template.getName().toLowerCase(), template);
        logger.fine("Registered template: " + template.getName());
    }

    /**
     * Get a template by name.
     *
     * @param name Template name (case-insensitive)
     * @return The template, or null if not found
     */
    public CommandTemplate getTemplate(String name) {
        return templates.get(name.toLowerCase());
    }

    /**
     * Check if a template exists.
     *
     * @param name Template name (case-insensitive)
     * @return True if template exists
     */
    public boolean hasTemplate(String name) {
        return templates.containsKey(name.toLowerCase());
    }

    /**
     * Unregister a template.
     *
     * @param name Template name (case-insensitive)
     * @return True if template was removed
     */
    public boolean unregister(String name) {
        return templates.remove(name.toLowerCase()) != null;
    }

    /**
     * Get all registered template names.
     *
     * @return Set of template names
     */
    public Set<String> getTemplateNames() {
        return templates.keySet();
    }

    /**
     * Get all registered templates.
     *
     * @return Map of template names to templates
     */
    public Map<String, CommandTemplate> getTemplates() {
        return new HashMap<>(templates);
    }

    /**
     * Clear all registered templates.
     * This will remove built-in templates too.
     */
    public void clear() {
        templates.clear();
        logger.fine("Cleared all templates");
    }
}
