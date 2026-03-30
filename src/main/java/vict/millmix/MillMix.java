package vict.millmix;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import vict.millmix.command.CommandGetReputation;
import vict.millmix.command.CommandMillDebugExportWorld;

import java.io.File;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION, dependencies = "required-after:millenaire")
public class MillMix {


    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandGetReputation());
        event.registerServerCommand(new CommandMillDebugExportWorld());
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File configFile = event.getSuggestedConfigurationFile();
        MillMixModConfig.init(configFile);
    }


}
