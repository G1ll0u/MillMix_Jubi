package com.jubitus.millmix.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.utilities.Point;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

// Here is the main class of the old MillMix
@Mixin(MillVillager.class)
public abstract class MixinMillVillager extends EntityCreature {

    private static final UUID SWIM_SPEED_UUID = UUID.fromString("b8b6d4a6-0e2f-4f89-a8d0-0f0ed7c69b2f");
    // Operation 1 = add percent of base (good for scaling); 0.6 = +60% speed.
    private static final AttributeModifier SWIM_SPEED_MOD =
            new AttributeModifier(SWIM_SPEED_UUID, "Millenaire swim speed", 0.8D, 1);
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

    @Inject(method = "<init>(Lnet/minecraft/world/World;)V", at = @At("TAIL"), remap = false)
    private void millenaire_swim_addAI(net.minecraft.world.World world, CallbackInfo ci) {
        MillVillager self = (MillVillager) (Object) this;
        // tasks is usually accessible as self.tasks in MCP; SRG field_70714_bg otherwise
        self.tasks.addTask(0, new EntityAISwimming(self));
    }

    @Inject(method = "func_70071_h_", at = @At("TAIL"), remap = false)
    private void millenaire_swim_speedTick(CallbackInfo ci) {
        IAttributeInstance speed = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed == null) return;

        boolean inWater = this.isInWater();

        if (inWater) {
            if (!speed.hasModifier(SWIM_SPEED_MOD)) {
                speed.applyModifier(SWIM_SPEED_MOD);
            }
        } else {
            if (speed.getModifier(SWIM_SPEED_UUID) != null) {
                speed.removeModifier(SWIM_SPEED_MOD);
            }
        }
    }
}