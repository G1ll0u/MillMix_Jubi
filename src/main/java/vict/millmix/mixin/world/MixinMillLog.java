package vict.millmix.mixin.world;

import net.minecraftforge.fml.common.FMLLog;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

@Mixin(MillLog.class)
public class MixinMillLog {

    @Shadow
    private static FileWriter writer;

    /**
     * @author Jubitus
     * @reason Serialize concurrent FileWriter writes during parallel plan loading.
     *         FMLLog.info (Log4j) is already thread-safe; only the FileWriter path needs a lock.
     */
    @Overwrite(remap = false)
    public static void writeText(String s) {
        if (MillLog.console) {
            FMLLog.info((String) (Mill.proxy.logPrefix() + LanguageUtilities.removeAccent(s)), (Object[]) new Object[0]);
        }
        if (writer != null) {
            synchronized (MillLog.class) {
                try {
                    writer.write("8.1.2 " + MillLog.now() + " " + s + MillConfigValues.NEOL);
                    writer.flush();
                } catch (IOException e) {
                    System.out.println("Failed to write line to log file.");
                }
            }
        }
    }

    /**
     * @author Jubitus
     * @reason Rotate Millénaire log files instead of letting millenaire.log grow unbounded.
     */
    @Overwrite(remap = false)
    public static void initLogFileWriter() {
        try {
            File baseDir = MillCommonUtilities.getMillenaireCustomContentDir();

            File logDir = new File(baseDir, "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));
            if (logFiles != null && logFiles.length > 5) {
                Arrays.sort(logFiles, Comparator.comparingLong(File::lastModified).reversed());
                for (int i = 5; i < logFiles.length; i++) {
                    logFiles[i].delete();
                }
            }

            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File logFile = new File(logDir, "millenaire_" + timestamp + ".log");

            writer = new FileWriter(logFile, false);

        } catch (Exception e) {
            System.out.println("Failed to initialize Millénaire log file writer: " + e);
            e.printStackTrace();
        }
    }
}
