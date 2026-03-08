package com.jubitus.millmix;

import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

public class MillMixLateLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        System.out.println("[MillMix] ILateMixinLoader called! Queuing mixins.millmix.json");
        return Collections.singletonList("mixins.millmix.json");
    }
}