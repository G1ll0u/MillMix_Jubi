package vict.millmix.command;


import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraftforge.fml.common.Loader;
import org.apache.commons.io.FileUtils;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.world.MillWorldData;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CommandMillDebugExportWorld extends CommandBase {

    private static final int EXPORT_RADIUS_PER_VILLAGE = 120;

    @Override
    public String getName() {
        return "millDebugExportWorld";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/millDebugExportWorld <scanRadiusBlocks>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP)) {
            throw new CommandException("This command must be run by a player.");
        }

        if (args.length != 1) {
            throw new WrongUsageException(getUsage(sender));
        }

        int scanRadius = parseInt(args[0], 1, 200000);

        EntityPlayerMP player = (EntityPlayerMP) sender.getCommandSenderEntity();
        World world = player.world;
        BlockPos playerPos = player.getPosition();

        MillWorldData millWorld = Mill.getMillWorld(world);
        if (millWorld == null) {
            throw new CommandException("MillWorldData is null for this world.");
        }

        List<Point> selectedCenters = collectNearbyVillageCenters(millWorld, playerPos, scanRadius);

        if (selectedCenters.isEmpty()) {
            notifyCommandListener(sender, this, "No Millénaire villages/lonebuildings found within %s blocks.", scanRadius);
            return;
        }

        Set<ChunkCoord> chunksToExport = new HashSet<>();
        for (Point center : selectedCenters) {
            addChunksAround(chunksToExport, center, EXPORT_RADIUS_PER_VILLAGE);
        }

        File worldDir = getWorldDirectory(world);
        if (worldDir == null || !worldDir.exists()) {
            throw new CommandException("Could not resolve world save directory.");
        }

        String worldName = sanitizeName(worldDir.getName());
        String dimFolderName = getDimensionFolderName(world.provider.getDimension());
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());

        File debugDir = new File(Loader.instance().getConfigDir(), "millmix/debug");
        if (!debugDir.exists() && !debugDir.mkdirs()) {
            throw new CommandException("Could not create debug output directory: " + debugDir.getAbsolutePath());
        }

        File tempExportRoot = new File(debugDir, worldName + "_" + dimFolderName + "_" + timestamp);
        File zipFile = new File(debugDir, worldName + "_" + dimFolderName + "_" + timestamp + ".zip");

        try {
            if (tempExportRoot.exists()) {
                FileUtils.deleteDirectory(tempExportRoot);
            }
            if (!tempExportRoot.mkdirs()) {
                throw new IOException("Could not create temp export dir: " + tempExportRoot.getAbsolutePath());
            }

            notifyCommandListener(sender, this, "Found %s village/lonebuilding centers. Exporting %s chunks...",
                    selectedCenters.size(), chunksToExport.size());

            exportWorldSubset(world, worldDir, tempExportRoot, chunksToExport);

            zipDirectory(tempExportRoot, zipFile);

            notifyCommandListener(sender, this, "Export complete: %s", zipFile.getAbsolutePath());

        } catch (Exception e) {
            throw new CommandException("Export failed: " + e.getMessage());
        } finally {
            try {
                if (tempExportRoot.exists()) {
                    FileUtils.deleteDirectory(tempExportRoot);
                }
            } catch (IOException ignored) {
            }
        }
    }

    private static List<Point> collectNearbyVillageCenters(MillWorldData millWorld, BlockPos playerPos, int scanRadius) {
        List<Point> out = new ArrayList<>();
        double maxSq = (double) scanRadius * (double) scanRadius;

        collectPointsInRange(millWorld.villagesList.pos, playerPos, maxSq, out);
        collectPointsInRange(millWorld.loneBuildingsList.pos, playerPos, maxSq, out);

        return out;
    }

    private static void addChunksAround(Set<ChunkCoord> out, Point center, int radiusBlocks) {
        int minX = center.getiX() - radiusBlocks;
        int maxX = center.getiX() + radiusBlocks;
        int minZ = center.getiZ() - radiusBlocks;
        int maxZ = center.getiZ() + radiusBlocks;

        int minChunkX = floorDiv(minX, 16);
        int maxChunkX = floorDiv(maxX, 16);
        int minChunkZ = floorDiv(minZ, 16);
        int maxChunkZ = floorDiv(maxZ, 16);

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                out.add(new ChunkCoord(cx, cz));
            }
        }
    }

    private static File getWorldDirectory(World world) {
        try {
            return world.getSaveHandler().getWorldDirectory();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String sanitizeName(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String getDimensionFolderName(int dim) {
        if (dim == 0) return "overworld";
        if (dim == -1) return "nether";
        if (dim == 1) return "end";
        return "DIM" + dim;
    }

    private static void exportWorldSubset(World world, File srcWorldDir, File dstWorldDir, Set<ChunkCoord> chunksToExport) throws Exception {
        copyIfExists(new File(srcWorldDir, "level.dat"), new File(dstWorldDir, "level.dat"));
        copyIfExists(new File(srcWorldDir, "level.dat_old"), new File(dstWorldDir, "level.dat_old"));
        copyIfExists(new File(srcWorldDir, "icon.png"), new File(dstWorldDir, "icon.png"));

        copyDirectoryIfExists(new File(srcWorldDir, "millenaire"), new File(dstWorldDir, "millenaire"));

        copyDirectoryIfExists(new File(srcWorldDir, "data"), new File(dstWorldDir, "data"));

        copyDirectoryIfExists(new File(srcWorldDir, "playerdata"), new File(dstWorldDir, "playerdata"));
        copyDirectoryIfExists(new File(srcWorldDir, "advancements"), new File(dstWorldDir, "advancements"));
        copyDirectoryIfExists(new File(srcWorldDir, "stats"), new File(dstWorldDir, "stats"));

        int dim = world.provider.getDimension();
        File srcDimRoot = getDimensionRoot(srcWorldDir, dim);
        File dstDimRoot = getDimensionRoot(dstWorldDir, dim);

        if (!dstDimRoot.exists() && !dstDimRoot.mkdirs()) {
            throw new IOException("Could not create dimension root: " + dstDimRoot.getAbsolutePath());
        }

        File srcRegionDir = new File(srcDimRoot, "region");
        File dstRegionDir = new File(dstDimRoot, "region");
        if (!dstRegionDir.exists() && !dstRegionDir.mkdirs()) {
            throw new IOException("Could not create region dir: " + dstRegionDir.getAbsolutePath());
        }

        exportSelectedChunks(srcRegionDir, dstRegionDir, chunksToExport);

        copyDirectoryIfExists(new File(srcDimRoot, "data"), new File(dstDimRoot, "data"));
        copyIfExists(new File(srcDimRoot, "forcedchunks.dat"), new File(dstDimRoot, "forcedchunks.dat"));

        copyIfExists(new File(srcWorldDir, "forcedchunks.dat"), new File(dstWorldDir, "forcedchunks.dat"));
    }

    private static void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {

            String rootPath = sourceDir.getAbsolutePath();

            Deque<File> stack = new ArrayDeque<>();
            stack.push(sourceDir);

            while (!stack.isEmpty()) {
                File current = stack.pop();
                File[] files = current.listFiles();
                if (files == null) continue;

                for (File f : files) {
                    String entryName = f.getAbsolutePath().substring(rootPath.length() + 1).replace('\\', '/');

                    if (f.isDirectory()) {
                        stack.push(f);
                    } else {
                        zos.putNextEntry(new ZipEntry(entryName));
                        try (FileInputStream fis = new FileInputStream(f)) {
                            byte[] buf = new byte[8192];
                            int r;
                            while ((r = fis.read(buf)) != -1) {
                                zos.write(buf, 0, r);
                            }
                        }
                        zos.closeEntry();
                    }
                }
            }
        }
    }

    private static void collectPointsInRange(List<Point> points, BlockPos playerPos, double maxSq, List<Point> out) {
        for (Point p : points) {
            if (p == null) continue;
            double dx = p.getiX() - playerPos.getX();
            double dz = p.getiZ() - playerPos.getZ();
            double distSq = dx * dx + dz * dz;
            if (distSq <= maxSq) {
                out.add(p);
            }
        }
    }

    private static int floorDiv(int a, int b) {
        return Math.floorDiv(a, b);
    }

    private static void copyIfExists(File src, File dst) throws IOException {
        if (!src.exists()) return;
        File parent = dst.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent.getAbsolutePath());
        }
        Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void copyDirectoryIfExists(File src, File dst) throws IOException {
        if (!src.exists()) return;
        FileUtils.copyDirectory(src, dst);
    }

    private static File getDimensionRoot(File worldDir, int dim) {
        if (dim == 0) return worldDir;
        if (dim == -1) return new File(worldDir, "DIM-1");
        if (dim == 1) return new File(worldDir, "DIM1");
        return new File(worldDir, "DIM" + dim);
    }

    private static void exportSelectedChunks(File srcRegionDir, File dstRegionDir, Set<ChunkCoord> chunks) throws IOException {
        Map<RegionCoord, List<ChunkCoord>> byRegion = new HashMap<>();

        for (ChunkCoord cc : chunks) {
            RegionCoord rc = new RegionCoord(floorDiv(cc.chunkX, 32), floorDiv(cc.chunkZ, 32));
            byRegion.computeIfAbsent(rc, k -> new ArrayList<>()).add(cc);
        }

        for (Map.Entry<RegionCoord, List<ChunkCoord>> entry : byRegion.entrySet()) {
            RegionCoord rc = entry.getKey();
            List<ChunkCoord> regionChunks = entry.getValue();

            File srcRegionFile = new File(srcRegionDir, "r." + rc.regionX + "." + rc.regionZ + ".mca");
            if (!srcRegionFile.exists()) {
                continue;
            }

            File dstRegionFile = new File(dstRegionDir, "r." + rc.regionX + "." + rc.regionZ + ".mca");

            RegionFile src = null;
            RegionFile dst = null;
            try {
                src = new RegionFile(srcRegionFile);
                dst = new RegionFile(dstRegionFile);

                for (ChunkCoord cc : regionChunks) {
                    int localX = cc.chunkX & 31;
                    int localZ = cc.chunkZ & 31;

                    if (!src.isChunkSaved(localX, localZ)) {
                        continue;
                    }

                    try (DataInputStream in = src.getChunkDataInputStream(localX, localZ)) {
                        if (in == null) {
                            continue;
                        }

                        byte[] chunkNbt = readAllBytes(in);

                        try (DataOutputStream out = dst.getChunkDataOutputStream(localX, localZ)) {
                            out.write(chunkNbt);
                        }
                    }
                }
            } finally {
                if (src != null) src.close();
                if (dst != null) dst.close();
            }
        }
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) != -1) {
            baos.write(buf, 0, r);
        }
        return baos.toByteArray();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    private static final class ChunkCoord {
        final int chunkX;
        final int chunkZ;

        ChunkCoord(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public int hashCode() {
            return Objects.hash(chunkX, chunkZ);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChunkCoord)) return false;
            ChunkCoord that = (ChunkCoord) o;
            return chunkX == that.chunkX && chunkZ == that.chunkZ;
        }
    }

    private static final class RegionCoord {
        final int regionX;
        final int regionZ;

        RegionCoord(int regionX, int regionZ) {
            this.regionX = regionX;
            this.regionZ = regionZ;
        }

        @Override
        public int hashCode() {
            return Objects.hash(regionX, regionZ);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RegionCoord)) return false;
            RegionCoord that = (RegionCoord) o;
            return regionX == that.regionX && regionZ == that.regionZ;
        }
    }
}