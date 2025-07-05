package com.jubitus.millmix;

import org.spongepowered.asm.mixin.Mixins;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

public class MillMixLateLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.millmix.json");
    }
}