package org.cloudwarp.probablychests.entity;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import org.cloudwarp.probablychests.ProbablyChests;
import org.cloudwarp.probablychests.entity.ai.MimicMoveControl;
import org.cloudwarp.probablychests.entity.ai.PCMeleeAttackGoal;
import org.cloudwarp.probablychests.entity.ai.PCMimicEscapeDangerGoal;
import org.cloudwarp.probablychests.registry.PCSounds;
import org.cloudwarp.probablychests.utils.MimicDifficulty;
import org.cloudwarp.probablychests.utils.PCConfig;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.EnumSet;
import java.util.Random;

public class PCChestMimic extends PCTameablePetWithInventory implements IAnimatable, Monster {
	// Animations
	public static final AnimationBuilder IDLE = new AnimationBuilder().loop("jump");
	public static final AnimationBuilder JUMP = new AnimationBuilder().playOnce("jump").loop("flying");
	public static final AnimationBuilder CLOSE = new AnimationBuilder().playOnce("land").loop("idle");
	public static final AnimationBuilder SLEEPING = new AnimationBuilder().loop("sleeping");
	public static final AnimationBuilder FLYING = new AnimationBuilder().loop("flying");
	public static final AnimationBuilder LOW_WAG = new AnimationBuilder().loop("lowWag");
	public static final AnimationBuilder FLYING_WAG = new AnimationBuilder().loop("flyingWag");
	public static final AnimationBuilder IDLE_WAG = new AnimationBuilder().loop("idleWag");
	public static final AnimationBuilder NO_WAG = new AnimationBuilder().loop("noWag");
	private static final String MIMIC_CONTROLLER = "mimicController";
	private static final String TONGUE_CONTROLLER = "tongueController";

	private static double moveSpeed = 1.5D;
	private static int maxHealth = 50;
	private static int maxDamage = 5;

	private AnimationFactory factory = new AnimationFactory(this);
	private boolean onGroundLastTick;
	private int timeUntilSleep = 0;
	private int jumpEndTimer = 10;
	private int spawnWaitTimer = 10;
	private boolean isAttemptingToSleep = false;

	public PCChestMimic (EntityType<? extends PCTameablePetWithInventory> entityType, World world) {
		super(entityType, world);
		this.ignoreCameraFrustum = true;
		this.moveControl = new MimicMoveControl(this);
		this.experiencePoints = 10;
	}

	public static DefaultAttributeContainer.Builder createMobAttributes () {
		MimicDifficulty mimicDifficulty = ProbablyChests.loadedConfig.mimicSettings.mimicDifficulty;
		moveSpeed = mimicDifficulty.getSpeed();
		return LivingEntity.createLivingAttributes().add(EntityAttributes.GENERIC_FOLLOW_RANGE, 12.0D)
				.add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 2)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, mimicDifficulty.getDamage())
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 1)
				.add(EntityAttributes.GENERIC_MAX_HEALTH, mimicDifficulty.getHealth())
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5D);
	}


	protected void initGoals () {
		this.goalSelector.add(7, new PCTameablePetWithInventory.IdleGoal(this));
		this.goalSelector.add(5, new PCMeleeAttackGoal(this, 1.0, true));
		this.goalSelector.add(6, new PCTameablePetWithInventory.SleepGoal(this));
		this.goalSelector.add(1, new PCTameablePetWithInventory.SwimmingGoal(this));
		this.targetSelector.add(3, (new RevengeGoal(this, new Class[0])).setGroupRevenge(new Class[0]));
		this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, livingEntity -> Math.abs(livingEntity.getY() - this.getY()) <= 4.0D));
	}

	private <E extends IAnimatable> PlayState chestMovement (AnimationEvent<E> animationEvent) {
		int state = this.getMimicState();
		animationEvent.getController().setAnimationSpeed(1D);
		switch (state) {
			case IS_SLEEPING:
				animationEvent.getController().setAnimation(SLEEPING);
				break;
			case IS_IN_AIR:
				animationEvent.getController().setAnimation(FLYING);
				break;
			case IS_LANDING:
				animationEvent.getController().setAnimation(CLOSE);
				break;
			case IS_IDLE:
				animationEvent.getController().setAnimation(IDLE);
				break;
			case IS_JUMPING:
				animationEvent.getController().setAnimationSpeed(2D);
				animationEvent.getController().setAnimation(JUMP);
				break;
			default:
				animationEvent.getController().setAnimation(SLEEPING);
				break;
		}
		return PlayState.CONTINUE;
	}
	private <E extends IAnimatable> PlayState tongueMovement (AnimationEvent<E> animationEvent) {
		int state = this.getMimicState();
		animationEvent.getController().setAnimationSpeed(1D);
		//animationEvent.getController().transitionLengthTicks = 1;
		if (state == IS_IN_AIR) {
			animationEvent.getController().setAnimation(FLYING_WAG);
		} else if (state == IS_IDLE) {
			animationEvent.getController().setAnimationSpeed(1.5D);
			animationEvent.getController().setAnimation(IDLE_WAG);
		} else if (state == IS_JUMPING) {
			animationEvent.getController().setAnimationSpeed(2D);
			animationEvent.getController().setAnimation(FLYING_WAG);
		} else if (state == IS_SLEEPING) {
			animationEvent.getController().setAnimation(NO_WAG);
		} else {
		}
		return PlayState.CONTINUE;
	}

	@Override
	public void registerControllers (AnimationData animationData) {
		animationData.addAnimationController(new AnimationController(this, MIMIC_CONTROLLER, 6, this::chestMovement));
		animationData.addAnimationController(new AnimationController(this, TONGUE_CONTROLLER, 6, this::tongueMovement));
	}

	@Override
	public AnimationFactory getFactory () {
		return this.factory;
	}


	protected void jump () {
		Vec3d vec3d = this.getVelocity();
		LivingEntity livingEntity = this.getTarget();
		double jumpStrength;
		if (livingEntity == null) {
			jumpStrength = 1D;
		} else {
			jumpStrength = livingEntity.getY() - this.getY();
			jumpStrength = jumpStrength <= 0 ? 1.0D : Math.min(jumpStrength / 3.5D + 1.0D, 2.5D);
		}
		this.setVelocity(vec3d.x, (double) this.getJumpVelocity() * jumpStrength, vec3d.z);
		this.velocityDirty = true;
		if (this.isOnGround() && this.jumpEndTimer <= 0) {
			this.jumpEndTimer = 10;
			this.setMimicState(IS_JUMPING);
		}
	}

	protected boolean canAttack () {
		return this.canMoveVoluntarily();
	}

	protected float getDamageAmount () {
		return (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
	}

	public boolean tryAttack (Entity target) {
		boolean bl = target.damage(DamageSource.mob(this), (float) ((int) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)));
		if (bl) {
			this.playSound(this.getHurtSound(), this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 0.7F);
			this.playSound(PCSounds.MIMIC_BITE, this.getSoundVolume(), 1.5F + getPitchOffset(0.2F));
			this.applyDamageEffects(this, target);
		}

		return bl;
	}

	public double squaredDistanceToEntity(LivingEntity entity) {
		Vec3d vector  = entity.getPos();
		double d = this.getX() - vector.x;
		double e = this.getY() - (vector.y+0.6D);
		double f = this.getZ() - vector.z;
		return d * d + e * e + f * f;
	}

	public int getTicksUntilNextJump () {
		return this.random.nextInt(40) + 5;
	}

	public void tick () {
		super.tick();
		if(this.world.isClient()) {
			return;
		}
		if (jumpEndTimer >= 0) {
			jumpEndTimer -= 1;
		}if (spawnWaitTimer > 0) {
			spawnWaitTimer -= 1;
		} else {
			if (this.onGround) {
				if (this.onGroundLastTick) {
					if (this.getMimicState() != IS_SLEEPING && ! isAttemptingToSleep) {
						timeUntilSleep = 150;
						isAttemptingToSleep = true;
						this.setMimicState(IS_IDLE);
					}
					if (isAttemptingToSleep) {
						timeUntilSleep -= 1;
						if (timeUntilSleep <= 0) {
							timeUntilSleep = 0;
							this.setMimicState(IS_SLEEPING);
						}
					}
				} else {
					isAttemptingToSleep = false;
					this.setMimicState(IS_LANDING);
					this.playSound(this.getLandingSound(), this.getSoundVolume(),
							((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
				}
			} else {
				isAttemptingToSleep = false;
				if (this.getMimicState() != IS_JUMPING) {
					this.setMimicState(IS_IN_AIR);
				}
			}
		}
		this.onGroundLastTick = this.onGround;
	}

	protected boolean isDisallowedInPeaceful () {
		return true;
	}

	public void writeCustomDataToNbt (NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("wasOnGround", this.onGroundLastTick);
	}

	public void readCustomDataFromNbt (NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.onGroundLastTick = nbt.getBoolean("wasOnGround");
	}

	@Override
	protected void initDataTracker () {
		this.dataTracker.startTracking(getMimicStateVariable(), IS_SLEEPING);
		super.initDataTracker();
	}

	public boolean cannotDespawn () {
		return this.hasVehicle() || ! this.inventory.isEmpty();
	}
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return true;
	}

	public static boolean isSpawnDark (ServerWorldAccess world, BlockPos pos, Random random) {
		if (world.getLightLevel(LightType.SKY, pos) > random.nextInt(32)) {
			return false;
		} else if (world.getLightLevel(LightType.BLOCK, pos) > 0) {
			return false;
		} else {
			PCConfig config = ProbablyChests.loadedConfig;
			int i = world.toServerWorld().isThundering() ? world.getLightLevel(pos, 10) : world.getLightLevel(pos);
			return i <= random.nextInt(8) * config.mimicSettings.naturalMimicSpawnRate;
		}
	}

	public static boolean canSpawn (EntityType<PCChestMimic> pcChestMimicEntityType, ServerWorldAccess serverWorldAccess, SpawnReason spawnReason, BlockPos blockPos, Random random) {
		return isSpawnDark(serverWorldAccess, blockPos, random);
	}

}
