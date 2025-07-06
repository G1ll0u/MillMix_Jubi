package com.jubitus.millmix.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.utilities.Point;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MillVillager.class)
public abstract class MixinMillVillager extends EntityCreature {
    public MixinMillVillager(World world) {
        super(world); //hush compiler's cry for (unused) superclass constructor
    }

    @Inject(method = "jumpToDest", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private void jumpToDestTest(CallbackInfo ci) {
        EntityLivingBase target = this.getAttackTarget();
        if (target != null && target instanceof EntityPlayer) {
            ci.cancel();
        }
    }

    /**
     * idea: change millvillager's toggleDoor to use BlockDoor's toggleDoor
     *
     * @author Vict
     * @reason toggleDoor doesn't make use of BlockDoor.toggleDoor(),
     * causing issues when other mods override the behaviour of doors.
     * This way we don't manually set block states, but instead tell
     * the blocks to change their states.
     */
    @Overwrite(remap = false)
    private void toggleDoor(Point p) {
        if (p == null) {
            return;
        }
        IBlockState state = p.getBlockActualState(this.world);
        boolean isOpen = state.getValue((IProperty) BlockDoor.OPEN).equals(true);
        BlockPos doorPos = p.getBlockPos();
        Block block = p.getBlock(world);
        if (block != null && block instanceof BlockDoor) {
            BlockDoor door = (BlockDoor) block;
            door.toggleDoor(world, doorPos, !isOpen);
        }
    }

    //this was method="processInteract" but for some reason can't find mappings
    // I dunno llol I'm fucking lost this'll do for now
    @Inject(method = "func_184645_a", at = @At("HEAD"), cancellable = true, remap = false)
    private void processInteract(EntityPlayer entityplayer,
                                 EnumHand hand,
                                 CallbackInfoReturnable ci) {
        EntityLivingBase target = this.getAttackTarget();
        if (target != null && target.equals(entityplayer)) {
            ci.cancel();
        }
    }

    @Inject(method = "interactSpecial", at = @At("HEAD"), cancellable = true, remap = false)
    private void interactSpecial(EntityPlayer entityplayer,
                                 CallbackInfoReturnable ci) {
        EntityLivingBase target = this.getAttackTarget();
        if (target != null && target.equals(entityplayer)) {
            ci.cancel();
        }

    }
}
