package vict.millmix.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.millenaire.common.commands.CommandUtilities;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.MillWorldData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CommandRenameLoneBuilding implements ICommand {

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
        return "millRenameLoneBuilding";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.millrenameLonebuilding.usage";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        World world = sender.getEntityWorld();
        if (world.isRemote) {
            return;
        }
        if (args.length != 2 && args.length != 3) {
            throw new WrongUsageException(this.getUsage(sender));
        }

        MillWorldData worldData = Mill.getMillWorld(world);
        List<Building> matches = getMatchingLoneBuildings(worldData, args[0]);

        if (matches.isEmpty()) {
            throw new CommandException(LanguageUtilities.string("command.tp_nomatchingvillage"), new Object[0]);
        }

        Building loneBuilding;
        if (matches.size() == 1) {
            loneBuilding = matches.get(0);
        } else if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            loneBuilding = matches.stream()
                    .min(Comparator.comparingDouble(b -> b.getPos().distanceTo((Entity) player)))
                    .get();
        } else {
            loneBuilding = matches.get(0);
        }

        String oldName = loneBuilding.getVillageQualifiedName();
        loneBuilding.changeVillageName(args[1]);
        if (args.length > 2) {
            loneBuilding.changeVillageQualifier(args[2]);
        } else {
            loneBuilding.changeVillageQualifier("");
        }

        for (EntityPlayer player : world.playerEntities) {
            ServerSender.sendTranslatedSentence(player, '9', "command.renamevillage_notification",
                    sender.getName(), oldName, loneBuilding.getVillageQualifiedName());
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            World world = sender.getEntityWorld();
            MillWorldData worldData = Mill.getMillWorld(world);
            List<Building> matches = getMatchingLoneBuildings(worldData, args[0]);
            List<String> completions = new ArrayList<>();
            for (Building b : matches) {
                completions.add(CommandUtilities.normalizeString(b.getVillageQualifiedName()));
            }
            return completions;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    private List<Building> getMatchingLoneBuildings(MillWorldData worldData, String param) {
        param = CommandUtilities.normalizeString(param);
        List<Building> result = new ArrayList<>();
        for (Point pos : worldData.loneBuildingsList.pos) {
            Building b = worldData.getBuilding(pos);
            if (b == null) continue;
            if (CommandUtilities.normalizeString(b.getVillageQualifiedName()).startsWith(param)) {
                result.add(b);
            }
        }
        return result;
    }
}