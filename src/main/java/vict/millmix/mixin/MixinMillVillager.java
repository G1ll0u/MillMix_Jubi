package vict.millmix.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
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
@Mixin(value = MillVillager.class, remap = false)
public abstract class MixinMillVillager {

    private static final UUID SWIM_SPEED_UUID = UUID.fromString("b8b6d4a6-0e2f-4f89-a8d0-0f0ed7c69b2f");
    private static final AttributeModifier SWIM_SPEED_MOD =
            new AttributeModifier(SWIM_SPEED_UUID, "Millenaire swim speed", 0.8D, 1);


    @Inject(method = "jumpToDest", at = @At(value = "HEAD"), cancellable = true)
    private void jumpToDestTest(CallbackInfo ci) {
        MillVillager self = (MillVillager) (Object) this;
        EntityLivingBase target = self.getAttackTarget();
        if (target instanceof EntityPlayer) {
            ci.cancel();
        }
    }

    @Overwrite
    private void toggleDoor(Point p) {
        if (p == null) return;

        MillVillager self = (MillVillager) (Object) this;

        IBlockState state = p.getBlockActualState(self.world);
        boolean isOpen = state.getValue((IProperty) BlockDoor.OPEN).equals(true);
        BlockPos doorPos = p.getBlockPos();
        Block block = p.getBlock(self.world);

        if (block instanceof BlockDoor) {
            ((BlockDoor) block).toggleDoor(self.world, doorPos, !isOpen);
        }
    }

    @Inject(method = "func_184645_a", at = @At("HEAD"), cancellable = true)
    private void processInteract(EntityPlayer entityplayer, EnumHand hand, CallbackInfoReturnable<Boolean> cir) {
        MillVillager self = (MillVillager) (Object) this;
        EntityLivingBase target = self.getAttackTarget();
        if (target != null && target.equals(entityplayer)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "interactSpecial", at = @At("HEAD"), cancellable = true)
    private void interactSpecial(EntityPlayer entityplayer, CallbackInfoReturnable<Boolean> cir) {
        MillVillager self = (MillVillager) (Object) this;
        EntityLivingBase target = self.getAttackTarget();
        if (target != null && target.equals(entityplayer)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;)V", at = @At("TAIL"))
    private void millenaire_swim_addAI(World world, CallbackInfo ci) {
        MillVillager self = (MillVillager) (Object) this;
        self.tasks.addTask(0, new EntityAISwimming(self));
    }

    @Inject(method = "func_70071_h_", at = @At("TAIL"))
    private void millenaire_swim_speedTick(CallbackInfo ci) {
        MillVillager self = (MillVillager) (Object) this;
        IAttributeInstance speed = self.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed == null) return;

        if (self.isInWater()) {
            if (!speed.hasModifier(SWIM_SPEED_MOD)) speed.applyModifier(SWIM_SPEED_MOD);
        } else {
            if (speed.getModifier(SWIM_SPEED_UUID) != null) speed.removeModifier(SWIM_SPEED_MOD);
        }
    }
}
