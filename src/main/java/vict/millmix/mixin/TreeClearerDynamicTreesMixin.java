package vict.millmix.mixin;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import org.millenaire.common.buildingplan.TreeClearer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Mixin(value = TreeClearer.class, remap = false)
public class TreeClearerDynamicTreesMixin {

    @Unique
    private static Class<?> millmix$blockBranchClass;

    @Unique
    private static Class<?> millmix$iTreePartClass;

    @Unique
    private static Method millmix$getTreePartTypeMethod;

    @Unique
    private static Object millmix$leavesEnumValue;

    @Unique
    private static boolean millmix$dtInitDone = false;

    @Inject(method = "isLogBlock", at = @At("HEAD"), cancellable = true)
    private void millmix$isLogBlock_dt(Block block, CallbackInfoReturnable<Boolean> cir) {
        millmix$initDT();

        if (millmix$blockBranchClass != null && millmix$blockBranchClass.isInstance(block)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static void millmix$initDT() {
        if (millmix$dtInitDone) return;
        millmix$dtInitDone = true;

        try {
            millmix$blockBranchClass = Class.forName(
                    "com.ferreusveritas.dynamictrees.blocks.BlockBranch"
            );

            millmix$iTreePartClass = Class.forName(
                    "com.ferreusveritas.dynamictrees.api.treedata.ITreePart"
            );

            millmix$getTreePartTypeMethod = millmix$iTreePartClass.getMethod("getTreePartType");

            Class<?> treePartTypeClass = Class.forName(
                    "com.ferreusveritas.dynamictrees.api.treedata.ITreePart$TreePartType"
            );

            @SuppressWarnings("unchecked")
            Object leaves = Enum.valueOf((Class<? extends Enum>) treePartTypeClass, "LEAVES");
            millmix$leavesEnumValue = leaves;

        } catch (Throwable t) {
            millmix$blockBranchClass = null;
            millmix$iTreePartClass = null;
            millmix$getTreePartTypeMethod = null;
            millmix$leavesEnumValue = null;
        }
    }

    @Inject(method = "isLeaveBlock", at = @At("HEAD"), cancellable = true)
    private void millmix$isLeaveBlock_dt(Block block, CallbackInfoReturnable<Boolean> cir) {
        millmix$initDT();

        if (millmix$iTreePartClass == null || millmix$getTreePartTypeMethod == null || millmix$leavesEnumValue == null) {
            return;
        }

        try {
            if (millmix$iTreePartClass.isInstance(block)) {
                Object type = millmix$getTreePartTypeMethod.invoke(block);
                if (millmix$leavesEnumValue.equals(type)) {
                    cir.setReturnValue(true);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "isLeaveBlock", at = @At("HEAD"), cancellable = true)
    private void millmix$isLeaveBlock_snow(Block block, CallbackInfoReturnable<Boolean> cir) {
        if (block == Blocks.SNOW_LAYER) {
            cir.setReturnValue(true);
        }
    }
}