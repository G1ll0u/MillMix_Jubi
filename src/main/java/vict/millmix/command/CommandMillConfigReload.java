package vict.millmix.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.forge.Mill;
import vict.millmix.MillMixModConfig;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class CommandMillConfigReload implements ICommand {

    @Override
    public int compareTo(ICommand o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(2, this.getName());
    }

    @Override
    public String getName() {
        return "millConfigReload";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        try {
            // Reset configParameters to their stored defaults so removed custom entries revert correctly
            for (org.millenaire.common.config.MillConfigParameter param : MillConfigValues.configParameters.values()) {
                if (param.defaultVal != null) {
                    param.setValue(param.defaultVal);
                }
            }

            // Reset special fields not tracked by configParameters
            MillConfigValues.DEV = false;
            MillConfigValues.stopDefaultVillages = false;
            MillConfigValues.main_language = "";
            MillConfigValues.forbiddenBlocks.clear();
            MillConfigValues.forcePreload = 0;
            MillConfigValues.questBiomeForest = "forest";
            MillConfigValues.questBiomeDesert = "desert";
            MillConfigValues.questBiomeMountain = "mountain";

            Method readConfigFile = MillConfigValues.class.getDeclaredMethod("readConfigFile", File.class, boolean.class);
            readConfigFile.setAccessible(true);

            // Re-read main config to restore server-default values (and refresh defaultVal)
            readConfigFile.invoke(null, Mill.proxy.getConfigFile(), true);
            // Re-read custom config to apply user overrides on top
            readConfigFile.invoke(null, Mill.proxy.getCustomConfigFile(), false);

            MillMixModConfig.reload();

            File customFile = Mill.proxy.getCustomConfigFile();
            sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "[MillMix] Millénaire and MillMix configs reloaded (Millénaire: " + customFile.getName() + ")"
            ));
        } catch (Exception e) {
            throw new CommandException("[MillMix] Failed to reload Millénaire config: " + e.getMessage());
        }
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/millConfigReload";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }
}
