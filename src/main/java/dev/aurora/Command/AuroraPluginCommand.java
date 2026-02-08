package dev.aurora.Command;

import dev.aurora.Manager.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class AuroraPluginCommand  extends Command {

    private final CommandManager commandManager;
    private final AuroraCommand command;

    public AuroraPluginCommand(String name, JavaPlugin plugin, CommandManager manager, AuroraCommand command) {
        super("name");
        this.commandManager = manager;
        this.command = command;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return commandManager.onCommand(sender, this, commandLabel, args);
    }

    public List<String > tabComplete(CommandSender sender, String alias, String[] args){
        return command.getTabCompletions(sender, args);
    }

}
