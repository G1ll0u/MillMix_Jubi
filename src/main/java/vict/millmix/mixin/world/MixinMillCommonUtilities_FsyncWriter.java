package vict.millmix.mixin.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vict.millmix.util.SyncingBufferedWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

/**
 * Replaces MillCommonUtilities.getWriter() to return a BufferedWriter that
 * calls fsync() on flush(). This covers all of Millénaire's text file saves:
 * villages.txt, lonebuildings.txt, tags.txt, config.txt, and all UserProfile
 * files (reputation, quests, unlocked content, action data).
 *
 * Millénaire's callers invoke flush() but not close(), so syncing only on
 * close() would be too late. SyncingBufferedWriter syncs on every flush().
 */
@Mixin(targets = "org.millenaire.common.utilities.MillCommonUtilities", remap = false)
public class MixinMillCommonUtilities_FsyncWriter {

    @Overwrite(remap = false)
    public static BufferedWriter getWriter(File file) throws UnsupportedEncodingException, FileNotFoundException {
        return new SyncingBufferedWriter(file);
    }
}
