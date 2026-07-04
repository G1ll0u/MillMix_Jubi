package vict.millmix.mixin.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Adds fsync() before the temp-to-final rename in MillWorldData.saveVillagerRecords().
 * Same crash-safety fix as MixinBuilding_FsyncSave — see that class for full rationale.
 */
@Mixin(targets = "org.millenaire.common.world.MillWorldData", remap = false)
public class MixinMillWorldData_FsyncSave {

    @Redirect(
        method = "saveVillagerRecords",
        at = @At(
            value = "INVOKE",
            target = "Ljava/nio/file/Files;move(Ljava/nio/file/Path;Ljava/nio/file/Path;[Ljava/nio/file/CopyOption;)Ljava/nio/file/Path;"
        ),
        remap = false
    )
    private Path fsyncThenMove(Path source, Path target, CopyOption[] options) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(source.toFile(), "rw")) {
            raf.getFD().sync();
        } catch (IOException e) {
            org.millenaire.common.utilities.MillLog.error(null, "MillMix: fsync failed before villagerRecords rename: " + e.getMessage());
        }
        return Files.move(source, target, options);
    }
}
