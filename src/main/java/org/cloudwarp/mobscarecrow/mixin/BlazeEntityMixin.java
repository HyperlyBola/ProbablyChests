package org.cloudwarp.mobscarecrow.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import org.cloudwarp.mobscarecrow.blockdetails.MobScarecrowBlockTags;
import org.cloudwarp.mobscarecrow.goals.AvoidScarecrowGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlazeEntity.class)
public abstract class BlazeEntityMixin extends MobEntity {

    protected BlazeEntityMixin(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(at = @At("HEAD"), method = "initGoals()V")
    private void mixinInitGoals(CallbackInfo ci){
        this.goalSelector.add(0, new AvoidScarecrowGoal((BlazeEntity) (Object) this, MobScarecrowBlockTags.MOB_SCARECROW));
    }
}
