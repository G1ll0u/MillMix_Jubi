package vict.millmix.mixin.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import org.millenaire.common.commands.CommandUtilities;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.MillWorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Comparator;
import java.util.List;

@Mixin(value = org.millenaire.common.commands.CommandRenameVillage.class, remap = false)
public class MixinCommandRenameVillage_ClosestOnDuplicate {

    /**
     * @author Jubitus
     * @reason When multiple villages share the same name prefix, pick the one closest to the
     *         player instead of throwing a "multiple matching villages" error.
     */
    @Overwrite(remap = false)
    public void func_184881_a(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        World world = sender.getEntityWorld();
        if (world.isRemote) {
            return;
        }
        if (args.length != 2 && args.length != 3) {
            throw new WrongUsageException("commands.millrenamevillage.usage", new Object[0]);
        }
        MillWorldData worldData = Mill.getMillWorld(world);
        List<Building> townHalls = CommandUtilities.getMatchingVillages(worldData, args[0]);
        if (townHalls.isEmpty()) {
            throw new CommandException(LanguageUtilities.string("command.tp_nomatchingvillage"), new Object[0]);
        }
        Building village;
        if (townHalls.size() == 1) {
            village = townHalls.get(0);
        } else if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            village = townHalls.stream()
                    .min(Comparator.comparingDouble(th -> th.getPos().distanceTo((Entity) player)))
                    .get();
        } else {
            village = townHalls.get(0);
        }
        String oldName = village.getVillageQualifiedName();
        village.changeVillageName(args[1]);
        if (args.length > 2) {
            village.changeVillageQualifier(args[2]);
        } else {
            village.changeVillageQualifier("");
        }
        for (EntityPlayer player : world.playerEntities) {
            ServerSender.sendTranslatedSentence(player, '9', "command.renamevillage_notification",
                    sender.getName(), oldName, village.getVillageQualifiedName());
        }
    }
}
