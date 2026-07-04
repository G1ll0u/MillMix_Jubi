package vict.millmix.mixin.building;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Adds fsync() before the temp-to-final rename in Building$SaveWorker.run().
 *
 * Without this, a machine crash after Files.move() but before the OS flushes
 * the page cache produces 0-byte .gz files: the journal replays the rename
 * (so the file exists) but the data pages were never written to disk.
 *
 * Files.move() on a same-filesystem rename is atomic, but only for the
 * *directory entry* — not for the file data. fsync() on the temp file first
 * ensures the data is on disk before the rename is journalled.
 */
@Mixin(targets = "org.millenaire.common.village.Building$SaveWorker", remap = false)
public class MixinBuilding_FsyncSave {

    @Redirect(
        method = "run",
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
            // Non-fatal: log and continue — an unsynced save is still better than no save
            org.millenaire.common.utilities.MillLog.error(null, "MillMix: fsync failed before building rename: " + e.getMessage());
        }
        return Files.move(source, target, options);
    }
}
