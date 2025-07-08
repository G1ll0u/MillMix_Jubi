package com.jubitus.millmix;

import com.jubitus.millmix.command.CommandGetReputation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION, dependencies = "required-after:millenaire")
public class MillMix {

    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandGetReputation());
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File configFile = event.getSuggestedConfigurationFile();
        ModConfig.init(configFile);
    }
}