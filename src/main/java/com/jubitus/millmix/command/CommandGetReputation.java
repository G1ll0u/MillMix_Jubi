package com.jubitus.millmix.command;

import net.minecraft.command.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import org.millenaire.common.commands.CommandUtilities;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.MillWorldData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandGetReputation implements ICommand {
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(this.getRequiredPermissionLevel(), this.getName());
    }

    public int compareTo(ICommand o) {
        return this.getName().compareTo(o.getName());
    }

    public void execute(MinecraftServer server, ICommandSender sender,
                        String[] args) throws CommandException {
        World world = sender.getEntityWorld();
        if (!world.isRemote) {
            if (args.length < 2) {
                throw new WrongUsageException(getUsage(sender), new Object[0]);
            }
            System.out.println("start");
            Entity entity = CommandBase.getEntity((MinecraftServer)server,
                    (ICommandSender)sender,
                    (String)args[0]);
            MillWorldData worldData = Mill.getMillWorld(world);
            List<Building> townHalls =
                    CommandUtilities.getMatchingVillages(worldData, args[1]);
            System.out.println(townHalls.toString());
            if (townHalls.size() == 0) {
                throw new CommandException
                        (LanguageUtilities.string("command.tp_nomatchingvillage"),
                                new Object[0]);
            }

            int checkarg = 1;
            if (townHalls.size() > 1) {
                System.out.println("Multiple matching villages, defaulting" +
                        " to first one unless index is specified.");
                if (args.length > 2) {

                    try {checkarg = Integer.parseInt(args[2]);}
                    catch (NumberFormatException ex) {
                        throw new CommandException("Third arg must be an integer");
                    }
                    if (checkarg > townHalls.size() || checkarg < 1) {
                        throw new CommandException
                                (String.format("Error: Valid index 1-%d",
                                        townHalls.size()));
                    }
                }
            }

            Building village = townHalls.get(checkarg-1);
            if (entity instanceof EntityPlayer) {
                EntityPlayer p = (EntityPlayer)entity;
                TextComponentString chat = new TextComponentString
                        (String.format ("Reputation: %d",village.getReputation(p)));
                p.sendMessage((ITextComponent)chat);
            }
        }
    }

    public List<String> getAliases() {
        return Collections.emptyList();
    }

    public String getName() {return "millGetRep";}

    public int getRequiredPermissionLevel() {
        return 3;
    }

    public List<String> getTabCompletions(MinecraftServer server,
                                          ICommandSender sender,
                                          String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord
                    ((String[])args, (String[])server.getOnlinePlayerNames());
        }
        if (args.length == 2) {
            World world = sender.getEntityWorld();
            MillWorldData worldData = Mill.getMillWorld(world);
            List<Building> townHalls =
                    CommandUtilities.getMatchingVillages(worldData, args[1]);
            ArrayList<String> possibleMatches = new ArrayList<String>();
            for (Building th : townHalls) {
                possibleMatches.add(CommandUtilities.normalizeString
                        (th.getVillageQualifiedName()));
            }
            return possibleMatches;
        }
        return Collections.emptyList();
    }

    public String getUsage(ICommandSender sender) {
        return "commands." + this.getName().toLowerCase() + ".usage";
    }

    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }
}