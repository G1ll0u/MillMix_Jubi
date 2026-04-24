package vict.millmix.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.VillagerRecord;
import org.millenaire.common.world.MillWorldData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandDebugResetLoneBuildingsVillagers implements ICommand {

    @Override
    public int compareTo(ICommand o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(3, this.getName());
    }

    @Override
    public String getName() {
        return "millDebugResetLoneBuildingVillagers";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        World world = sender.getEntityWorld();

        if (!world.isRemote) {
            if (!(sender instanceof EntityPlayer)) {
                throw new WrongUsageException(this.getUsage(sender));
            }

            EntityPlayer senderPlayer = (EntityPlayer) sender;
            MillWorldData worldData = Mill.getMillWorld(world);

            if (worldData == null) {
                throw new CommandException("MillWorldData is null.");
            }

            Building loneBuilding = getClosestLoneBuilding(worldData, new Point(senderPlayer));

            if (loneBuilding == null || loneBuilding.getPos().distanceTo(senderPlayer) > 50.0D) {
                throw new CommandException("No lone building within 50 blocks.");
            }

            int despawnedVillagers = 0;
            int respawnedVillagers = 0;

            for (VillagerRecord vr : loneBuilding.getAllVillagerRecords()) {
                List<MillVillager> matchingVillagers = new ArrayList<>();

                for (MillVillager villager : worldData.getAllKnownVillagers()) {
                    if (villager.getVillagerId() == vr.getVillagerId()) {
                        matchingVillagers.add(villager);
                    }
                }

                for (int i = matchingVillagers.size() - 1; i >= 0; --i) {
                    MillVillager villager = matchingVillagers.get(i);

                    if (villager.isDead) {
                        villager.despawnVillagerSilent();
                        ++despawnedVillagers;
                        matchingVillagers.remove(i);
                    }
                }

                if (matchingVillagers.isEmpty()) {
                    loneBuilding.respawnVillager(vr, vr.getHouse().location.sleepingPos);
                    ++respawnedVillagers;
                }
            }

            senderPlayer.sendMessage(new TextComponentString(
                    TextFormatting.DARK_GREEN
                            + "Repaired the villager list of lone building "
                            + loneBuilding.getVillageQualifiedName()
                            + ". Despawned "
                            + despawnedVillagers
                            + " dead villager(s) and respawned "
                            + respawnedVillagers
                            + " villager(s)."
            ));
        }
    }

    private Building getClosestLoneBuilding(MillWorldData worldData, Point point) {
        int bestDistance = Integer.MAX_VALUE;
        Building bestBuilding = null;

        for (Point loneBuildingPos : worldData.loneBuildingsList.pos) {
            int dist = (int) point.distanceToSquared(loneBuildingPos);

            if (bestBuilding == null || dist < bestDistance) {
                Building building = worldData.getBuilding(loneBuildingPos);
                if (building != null) {
                    bestBuilding = building;
                    bestDistance = dist;
                }
            }
        }

        return bestBuilding;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands." + this.getName().toLowerCase() + ".usage";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }
}