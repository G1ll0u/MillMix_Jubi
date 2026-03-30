package vict.millmix;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.util.Collections;
import java.util.List;

/**
 * Cleanroom coremod entrypoint (loaded very early).
 * Loads mixins before Minecraft classes like PathNavigateGround are defined.
 */
@IFMLLoadingPlugin.Name("MillMixEarlyLoader")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class MillMixEarlyLoader implements IEarlyMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.millmix.early.json");
    }
}

