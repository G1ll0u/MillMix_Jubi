package vict.millmix.mixin.building;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vict.millmix.util.SyncingFileOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Replaces FileOutputStream construction in Building.writePaths() with
 * SyncingFileOutputStream so _paths.bin and _pathstoclear.bin are fsynced
 * before their file descriptor is closed.
 */
@Mixin(targets = "org.millenaire.common.village.Building", remap = false)
public class MixinBuilding_FsyncPaths {

    @Redirect(
        method = "writePaths",
        at = @At(value = "NEW", target = "java/io/FileOutputStream"),
        remap = false
    )
    private FileOutputStream newSyncingFOS(File file) throws FileNotFoundException {
        return new SyncingFileOutputStream(file);
    }
}
