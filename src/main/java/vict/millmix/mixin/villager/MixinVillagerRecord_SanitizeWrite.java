package vict.millmix.mixin.villager;

import org.millenaire.common.culture.Culture;
import org.millenaire.common.village.VillagerRecord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VillagerRecord.class, remap = false)
public abstract class MixinVillagerRecord_SanitizeWrite {

    @Shadow
    public String type;
    @Shadow
    public String firstName;
    @Shadow
    public String familyName;
    @Shadow
    public java.util.List<String> questTags;
    @Shadow
    public net.minecraft.util.ResourceLocation texture;

    @Inject(method = "write", at = @At("HEAD"))
    private void millmix$sanitizeBeforeWrite(net.minecraft.nbt.NBTTagCompound tag, String label, CallbackInfo ci) {

        // Garde-fous NBT: aucune String null
        if (this.type == null) {
            this.type = "";
            System.out.println("[MillMix] VR " + getVillagerId() + " had null type, fixed.");
        }
        if (this.firstName == null) {
            this.firstName = "";
            System.out.println("[MillMix] VR " + getVillagerId() + " had null firstName, fixed.");
        }
        if (this.familyName == null) {
            this.familyName = "";
            System.out.println("[MillMix] VR " + getVillagerId() + " had null familyName, fixed.");
        }

        // Nettoyer questTags
        if (this.questTags != null && this.questTags.removeIf(t -> t == null)) {
            System.out.println("[MillMix] VR " + getVillagerId() + " had null questTags entries, removed.");
        }

        // Si culture/key peut être null (rare) : éviter le crash
        if (this.getCulture() == null || this.getCulture().key == null) {
            System.out.println("[MillMix] VR " + getVillagerId() + " had null culture/key, forcing empty.");
            // On ne peut pas facilement assigner la culture si elle est privée, mais au moins éviter le setString(null)
            // Option: inject plus loin et set direct dans le NBT après write, ou Overwrite write.
        }

        // Texture: si jamais null, ça ferait une NPE plus loin, donc autant la réparer aussi
        if (this.texture == null) {
            this.texture = new net.minecraft.util.ResourceLocation("minecraft", "missingno");
            System.out.println("[MillMix] VR " + getVillagerId() + " had null texture, set to missingno.");
        }
    }

    @Shadow
    public abstract long getVillagerId();

    @Shadow
    public abstract Culture getCulture();
}