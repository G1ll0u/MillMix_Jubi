package vict.millmix.mixin.building;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vict.millmix.util.SyncingFileOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Replaces FileOutputStream construction in ConstructionIP.writeBblocks() with
 * SyncingFileOutputStream so _bblocks_*.bin files are fsynced before close.
 */
@Mixin(targets = "org.millenaire.common.village.ConstructionIP", remap = false)
public class MixinConstructionIP_FsyncBblocks {

    @Redirect(
        method = "writeBblocks",
        at = @At(value = "NEW", target = "java/io/FileOutputStream"),
        remap = false
    )
    private FileOutputStream newSyncingFOS(File file) throws FileNotFoundException {
        return new SyncingFileOutputStream(file);
    }
}
