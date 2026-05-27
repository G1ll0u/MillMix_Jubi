package vict.millmix;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import vict.millmix.command.CommandDebugResetLoneBuildingsVillagers;
import vict.millmix.command.CommandGetReputation;
import vict.millmix.command.CommandMillConfigReload;
import vict.millmix.command.CommandMillDebugExportWorld;
import vict.millmix.millmix.Tags;

import java.io.File;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, dependencies = "required-after:millenaire")
public class MillMix {


    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandGetReputation());
        event.registerServerCommand(new CommandMillDebugExportWorld());
        event.registerServerCommand(new CommandDebugResetLoneBuildingsVillagers());
        event.registerServerCommand(new CommandMillConfigReload());
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File configFile = event.getSuggestedConfigurationFile();
        MillMixModConfig.init(configFile);
    }


}
