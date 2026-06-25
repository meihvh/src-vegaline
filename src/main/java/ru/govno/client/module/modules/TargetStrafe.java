package ru.govno.client.module.modules;

import java.util.List;
import java.util.Objects;
import net.minecraft.block.BlockWeb;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.Event3D;
import ru.govno.client.event.events.EventAction;
import ru.govno.client.event.events.EventMove2;
import ru.govno.client.event.events.EventPostMove;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.BlockUtils;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Movement.MatrixStrafeMovement;
import ru.govno.client.utils.Movement.MoveMeHelp;
import ru.govno.client.utils.Render.RenderUtils;

public class TargetStrafe extends Module {
   public static TargetStrafe get;
   public static int b = 1;
   public static double speed = 0.23F;
   public static boolean needSprintState;
   public FloatSettings Distance;
   public FloatSettings SpeedF;
   public FloatSettings DamageSpeed;
   public BoolSettings SmartSpeed;
   public BoolSettings CollideBoost;
   public BoolSettings DamageBoost;
   public BoolSettings AutoJump;
   public BoolSettings RenderCurrentDist;
   public BoolSettings SmartReverse;
   public BoolSettings TargetBoxSync;
   public BoolSettings SpeedLimitToDistance;
   public BoolSettings TrickNoElytraRule;
   public BoolSettings TrickPardonElytraSTap;
   public BoolSettings TrickNoForwardRule;
   public BoolSettings ByterAdobeOnElytra;
   public ModeSettings DistanceTricks;
   private boolean elyCallUpdated = false;
   static EntityLivingBase target = null;
   private static double tempUpdatedSpeed;
   public static float callSpeedRotation;

   public TargetStrafe() {
      super("TargetStrafe", 0, Module.Category.MOVEMENT);
      get = this;
      this.settings.add(this.DistanceTricks = new ModeSettings("DistanceTricks", "MoveWSDistance", this, new String[]{"None", "MoveWSDistance", "RangeByter"}));
      this.settings
         .add(this.TrickNoElytraRule = new BoolSettings("TrickNoElytraRule", true, this, () -> !this.DistanceTricks.getMode().equalsIgnoreCase("None")));
      this.settings
         .add(
            this.TrickPardonElytraSTap = new BoolSettings(
               "TrickPardonElytraSTap", false, this, () -> this.DistanceTricks.getMode().equalsIgnoreCase("RangeByter") && this.TrickNoElytraRule.getBool()
            )
         );
      this.settings
         .add(this.TrickNoForwardRule = new BoolSettings("TrickNoForwardRule", false, this, () -> this.DistanceTricks.getMode().equalsIgnoreCase("RangeByter")));
      this.settings
         .add(
            this.ByterAdobeOnElytra = new BoolSettings(
               "ByterAdobeOnElytra",
               false,
               this,
               () -> this.DistanceTricks.getMode().equalsIgnoreCase("RangeByter") && ElytraBoost.get.Mode.getMode().equalsIgnoreCase("Firework")
            )
         );
      this.settings
         .add(
            this.Distance = new FloatSettings(
               "Distance",
               2.0F,
               9.0F,
               0.0F,
               this,
               () -> !this.DistanceTricks.getMode().equalsIgnoreCase("RangeByter") || this.TrickNoElytraRule.getBool() || this.TrickNoForwardRule.getBool()
            )
         );
      this.settings.add(this.SpeedF = new FloatSettings("Speed", 0.24F, 1.0F, 0.0F, this, () -> !this.SmartSpeed.getBool()));
      this.settings.add(this.SmartSpeed = new BoolSettings("SmartSpeed", true, this));
      this.settings
         .add(
            this.CollideBoost = new BoolSettings(
               "CollideBoost", false, this, () -> !this.SmartSpeed.getBool() && this.SpeedF.getFloat() >= 0.24F && !this.SmartSpeed.getBool()
            )
         );
      this.settings.add(this.DamageBoost = new BoolSettings("DamageBoost", false, this));
      this.settings.add(this.DamageSpeed = new FloatSettings("DamageSpeed", 0.6F, 2.0F, 0.0F, this, () -> this.DamageBoost.getBool()));
      this.settings.add(this.AutoJump = new BoolSettings("AutoJump", true, this));
      this.settings.add(this.RenderCurrentDist = new BoolSettings("RenderCurrentDist", true, this));
      this.settings.add(this.SmartReverse = new BoolSettings("SmartReverse", true, this));
      this.settings.add(this.TargetBoxSync = new BoolSettings("TargetBoxSync", false, this));
      this.settings.add(this.SpeedLimitToDistance = new BoolSettings("SpeedLimitToDistance", false, this));
      this.setDemand(0, 2);
   }

   private boolean isElyCall() {
      return ElytraBoost.get.isActived()
            && ElytraBoost.get.Mode.getMode().equalsIgnoreCase("Firework")
            && !ElytraBoost.get.GrimStrafed.getBool()
            && ElytraBoost.get.StrafeDirs.getBool()
         ? !ElytraBoost.get.GrimStrafed.getBool()
            && ElytraBoost.get.StrafeDirs.getBool()
            && ElytraBoost.get.StaticYMotions.getBool()
            && ElytraBoost.get.NormalizeDirection.getBool()
         : false;
   }

   private boolean isSmartSpeed() {
      return this.SmartSpeed.getBool() && !this.elyCallUpdated;
   }

   public float getRedistance(float defaultDistance) {
      if (Minecraft.player != null && Minecraft.player.isElytraFlying() && target != null && target.isEntityAlive()) {
         defaultDistance--;
      }

      String useMode = this.DistanceTricks.getMode();
      switch (useMode) {
         case "RangeByter":
            boolean t = false;
            if ((!this.TrickNoElytraRule.getBool() || !target.isElytraFlying() || this.TrickPardonElytraSTap.getBool() && MoveMeHelp.s())
               && this.ByterAdobeOnElytra.getBool()
               && ElytraBoost.get.isActived()
               && ElytraBoost.get.Mode.getMode().equalsIgnoreCase("Firework")
               && Minecraft.player.ticksElytraFlying > 0
               && !target.isElytraFlying()
               && goStrafe()
               && target != null
               && get.DistanceTricks.getMode().equalsIgnoreCase("RangeByter")
               && get.ByterAdobeOnElytra.getBool()
               && Minecraft.player.ticksElytraFlying > 0
               && !target.isElytraFlying()
               && mc.world != null) {
               float offsetUp = 4.0F;
               double checkDistanceAdobe = (double)(HitAura.get.getAuraRange(target) + offsetUp + 1.0F);
               Vec3d meFrom = Minecraft.player.getPositionVector().addVector(0.0, (double)Minecraft.player.height, 0.0);
               Vec3d meTo = meFrom.addVector(0.0, checkDistanceAdobe, 0.0);
               RayTraceResult rayMeAdobe = mc.world.rayTraceBlocks(meFrom, meTo);
               Vec3d targetFrom = get.getEntityVirtPos(target, get.TargetBoxSync.getBool()).addVector(0.0, (double)Minecraft.player.height, 0.0);
               Vec3d targetTo = meFrom.addVector(0.0, checkDistanceAdobe, 0.0);
               RayTraceResult rayTargetAdobe = mc.world.rayTraceBlocks(targetFrom, targetTo);
               if (rayMeAdobe == null
                  || rayTargetAdobe == null
                  || rayMeAdobe.typeOfHit == RayTraceResult.Type.MISS && rayTargetAdobe.typeOfHit == RayTraceResult.Type.MISS) {
                  t = true;
                  defaultDistance++;
               }
            }

            if (!t
               && target != null
               && (!this.TrickNoElytraRule.getBool() || !target.isElytraFlying() || this.TrickPardonElytraSTap.getBool() && MoveMeHelp.s())
               && (!this.TrickNoForwardRule.getBool() || !MoveMeHelp.w())) {
               float range = HitAura.get.getAuraRange(HitAura.TARGET_ROTS) - (float)speed - (Minecraft.player.isElytraFlying() ? 0.8F : 0.0F);
               if (HitAura.get.isActived() && HitAura.TARGET_ROTS != null && HitAura.TARGET_ROTS.getEntityId() == target.getEntityId() && HitAura.get.tpHit) {
                  range = HitAura.get.getAuraRange(HitAura.TARGET_ROTS) + TPInfluence.get.MaxRange.getFloat() / 200.0F * 8.0F;
               }

               int cooledTimeOffset = (int)(
                  1.0F / ((float)speed / range * (HitAura.get.msCooldown() / 50.0F - (float)(ElytraBoost.get.isTargetStrafeCalling() ? 1 : 0))) * 50.0F
               );
               float cooledPC = Math.min((float)(HitAura.cooldown.getTime() + (long)cooledTimeOffset) / HitAura.get.msCooldown(), 1.0F);
               float rangeAdd = Minecraft.player.isElytraFlying() ? 3.0F : 0.75F;
               Vec3d targetPos = this.getEntityVirtPos(target, this.TargetBoxSync.getBool());
               float targetMoveYaw = RotationUtil.getVecNeeded(new Vec3d(target.posX - target.lastTickPosX, 0.0, target.posZ - target.lastTickPosZ), Vec3d.ZERO)[0];
               float targetFaceYawToSelf = RotationUtil.getVecNeeded(Minecraft.player.getPositionVector(), targetPos)[0];
               float targetMovementDiffYaw = (float)RotationUtil.angleDifference(targetMoveYaw, targetFaceYawToSelf);
               float diffYawPC = Math.min(Math.abs(targetMovementDiffYaw) / 180.0F, 1.0F);
               float targetSpeedExpandNegPosMul = target.getSpeed() < 0.02F
                  ? 0.0F
                  : MathUtils.lerp(4.5F, -6.5F, diffYawPC) * (Minecraft.player.isElytraFlying() ? 0.0F : 1.0F);
               float expandOfDistance = Math.min((1.0F - cooledPC) * (HitAura.get.msCooldown() / 50.0F - rangeAdd - 2.0F), 1.0F) * rangeAdd
                  + (float)(target.getSpeed() * (double)targetSpeedExpandNegPosMul);
               defaultDistance = Math.max(range + expandOfDistance, 0.025F);
            }
            break;
         case "MoveWSDistance":
            if (target == null || !this.TrickNoElytraRule.getBool() || !target.isElytraFlying()) {
               int wdMulDistanceOffset = MoveMeHelp.w() ? -1 : (MoveMeHelp.s() ? 1 : 0);
               defaultDistance += defaultDistance / 1.5F * (float)wdMulDistanceOffset;
            }
      }

      return defaultDistance;
   }

   boolean onCanReverseBecauseChecks(Entity ent) {
      double dx = Minecraft.player.posX - Minecraft.player.lastTickPosX;
      double dz = Minecraft.player.posZ - Minecraft.player.lastTickPosZ;
      if (ent != null && (double)((int)ent.posY) >= Minecraft.player.posY - 1.2) {
         double downPadding = 5.0;
         AxisAlignedBB seflAABBDown = Minecraft.player.boundingBox.offsetMinDown(downPadding);
         double selfSpeedOffsetBox = 1.0;
         if (mc.world != null
            && mc.world.getCollisionBoxes(Minecraft.player, seflAABBDown.addExpandXZ(-0.25)).isEmpty()
            && !mc.world.getCollisionBoxes(Minecraft.player, seflAABBDown.addExpandXZ(selfSpeedOffsetBox)).isEmpty()) {
            return this.SmartReverse.getBool();
         }
      }

      if (!Minecraft.player.isInWeb) {
         for (double minOffsetXZMul = 1.0; minOffsetXZMul < 3.0; minOffsetXZMul += 0.25) {
            double dxT = -dx * minOffsetXZMul;
            double dzT = -dz * minOffsetXZMul;
            BlockPos predictPos = new BlockPos(Minecraft.player.posX - dxT, Minecraft.player.posY, Minecraft.player.posZ - dzT);
            if (mc.world != null
               && (
                  mc.world.getBlockState(predictPos).getBlock() instanceof BlockWeb || mc.world.getBlockState(predictPos.down()).getBlock() instanceof BlockWeb
               )) {
               return this.SmartReverse.getBool();
            }
         }
      }

      if (!Minecraft.player.isInLava() && !Minecraft.player.isPotionActive(MobEffects.FIRE_RESISTANCE)) {
         for (double minOffsetXZMulx = 1.0; minOffsetXZMulx < 3.0; minOffsetXZMulx += 0.25) {
            double dxT = -dx * minOffsetXZMulx;
            double dzT = -dz * minOffsetXZMulx;
            BlockPos predictPos = new BlockPos(Minecraft.player.posX - dxT, Minecraft.player.posY, Minecraft.player.posZ - dzT);
            if (mc.world != null
               && (
                  mc.world.getBlockState(predictPos).getBlock() == Blocks.LAVA
                     || mc.world.getBlockState(predictPos.down()).getBlock() == Blocks.LAVA
                     || mc.world.getBlockState(predictPos).getBlock() == Blocks.FIRE
                     || mc.world.getBlockState(predictPos.down()).getBlock() == Blocks.FIRE
               )) {
               return this.SmartReverse.getBool();
            }
         }
      }

      return false;
   }

   float[] getRotations(Entity ent) {
      Vec3d to = this.getEntityVirtPos(target, this.TargetBoxSync.getBool()).addVector(0.0, (double)ent.getEyeHeight(), 0.0);
      return this.getRotationFromPosition(to.xCoord, to.zCoord, to.yCoord);
   }

   float[] getRotationFromPosition(double x, double z, double y) {
      double px = RenderUtils.interpolate(Minecraft.player.posX, Minecraft.player.lastTickPosX, (double)mc.getRenderPartialTicks());
      double py = RenderUtils.interpolate(Minecraft.player.posY, Minecraft.player.lastTickPosY, (double)mc.getRenderPartialTicks());
      double pz = RenderUtils.interpolate(Minecraft.player.posZ, Minecraft.player.lastTickPosZ, (double)mc.getRenderPartialTicks());
      double xDiff = x - px;
      double zDiff = z - pz;
      double yDiff = y - py;
      double dist = (double)MathHelper.sqrt(xDiff * xDiff + zDiff * zDiff);
      float yaw = (float)(Math.atan2(zDiff, xDiff) * 180.0 / Math.PI) - 90.0F;
      float pitch = (float)(-(Math.atan2(yDiff, dist) * 180.0 / Math.PI));
      return new float[]{yaw, pitch};
   }

   private static float getMaxRange() {
      return HitAura.get != null && HitAura.get.isActived() && HitAura.get.tpHit && TPInfluence.get != null
         ? TPInfluence.get.MaxRange.getFloat()
         : Math.max(16.0F, HitAura.get.getAuraRange(null) + HitAura.get.getAuraPreRange() + 3.0F);
   }

   public static boolean isSmartKeep() {
      return true;
   }

   private static int keepPercent100() {
      return 45;
   }

   public Vec3d getEntityVirtPos(Entity entity, boolean boxSync) {
      if (entity == null) {
         return Vec3d.ZERO;
      } else {
         float pTicks = mc.getRenderPartialTicks();
         Vec3d pos = new Vec3d(
            MathUtils.lerp(entity.lastTickPosX, entity.posX, (double)pTicks),
            MathUtils.lerp(entity.lastTickPosY, entity.posY, (double)pTicks),
            MathUtils.lerp(entity.lastTickPosZ, entity.posZ, (double)pTicks)
         );
         if (!boxSync) {
            return pos;
         } else {
            Vec3d predictAny = EntityBox.hitboxModAddVec(
               entity,
               EntityBox.hitboxModPredictSize(entity) + (Minecraft.player != null && Minecraft.player.isElytraFlying() && entity.isEntityAlive() ? 4.5F : 2.0F)
            );
            return pos.add(predictAny);
         }
      }
   }

   void Motion(double d, float f, double d2, double d3, boolean onMove, boolean smartKeep) {
      double d4 = d3;
      double d5 = d2;
      float keep = 90.0F - (float)keepPercent100() * 0.9F;
      Vec3d targetPos = this.getEntityVirtPos(target, this.TargetBoxSync.getBool());
      float dst = Minecraft.player.getSmoothDistanceToCoordXZ((float)targetPos.xCoord, (float)targetPos.yCoord, (float)targetPos.zCoord);
      float cdst = this.getRedistance(this.Distance.getFloat());
      if (smartKeep) {
         double dstPardon = 0.25 + d + 0.2F;
         keep = 90.0F - (float)Math.min((double)MathUtils.getDifferenceOf(dst, cdst) * dstPardon, 1.0) * 90.0F;
      }

      if (this.elyCallUpdated) {
         double dstPardon = 6.0;
         keep = MathUtils.lerp(30.0F, 70.0F, (float)Math.min((double)MathUtils.getDifferenceOf(dst, cdst) / dstPardon, 1.0));
      }

      float f2 = f;
      if (d3 == 0.0 && d2 == 0.0) {
         if (onMove) {
            Entity.motionx = 1.0E-13;
            Entity.motionz = 1.0E-13;
         } else {
            Minecraft.player.motionX = 0.0;
            Minecraft.player.motionZ = 0.0;
         }
      } else {
         if (d3 != 0.0) {
            if (d2 > 0.0) {
               f2 = f + (d3 > 0.0 ? -keep : keep);
            } else if (d2 < 0.0) {
               f2 = f + (d3 > 0.0 ? keep : -keep);
            }

            d5 = 0.0;
            if (d3 > 0.0) {
               d4 = 1.0;
            } else if (d3 < 0.0) {
               d4 = -1.0;
            }
         }

         double d6 = (double)MathHelper.cos(MathHelper.toRadians(f2 + 93.5F));
         double d7 = (double)MathHelper.sin(MathHelper.toRadians(f2 + 93.5F));
         if (onMove) {
            Entity.motionx = (d4 * d * d6 + d5 * d * d7) / 1.06;
            Entity.motionz = (d4 * d * d7 - d5 * d * d6) / 1.06;
         } else {
            Minecraft.player.motionX = d4 * d * d6 + d5 * d * d7;
            Minecraft.player.motionZ = d4 * d * d7 - d5 * d * d6;
         }
      }
   }

   void Motion2(EventMove2 move, double d, float f, double d2, double d3, boolean smartKeep) {
      double d4 = d3;
      double d5 = d2;
      float keep = 90.0F - (float)keepPercent100() * 0.9F;
      Vec3d targetPos = this.getEntityVirtPos(target, this.TargetBoxSync.getBool());
      float dst = Minecraft.player.getSmoothDistanceToCoordXZ((float)targetPos.xCoord, (float)targetPos.yCoord, (float)targetPos.zCoord);
      float cdst = this.getRedistance(this.Distance.getFloat());
      if (smartKeep) {
         double dstPardon = 0.25 + d + 0.2F;
         keep = MathUtils.clamp((float)dstPardon - MathUtils.getDifferenceOf(dst, cdst), 0.0F, 1.0F) * 90.0F;
      }

      float f2 = f;
      if (d3 == 0.0 && d2 == 0.0) {
         MatrixStrafeMovement.oldSpeed = 0.0;
         move.motion().xCoord = 0.0;
         move.motion().zCoord = 0.0;
      } else {
         if (d3 != 0.0) {
            if (d2 > 0.0) {
               f2 = f + (d3 > 0.0 ? -keep : keep);
            } else if (d2 < 0.0) {
               f2 = f + (d3 > 0.0 ? keep : -keep);
            }

            d5 = 0.0;
            if (d3 > 0.0) {
               d4 = 1.0;
            } else if (d3 < 0.0) {
               d4 = -1.0;
            }
         }

         double d6 = (double)MathHelper.cos(MathHelper.toRadians(f2 + 90.0F));
         double d7 = (double)MathHelper.sin(MathHelper.toRadians(f2 + 90.0F));
         move.motion().xCoord = d4 * d * d6 + d5 * d * d7;
         move.motion().zCoord = d4 * d * d7 - d5 * d * d6;
      }
   }

   double[] Motion2OnlyGetMotions(double d, float f, double d2, double d3, boolean smartKeep) {
      double d4 = d3;
      double d5 = d2;
      float keep = 90.0F - (float)keepPercent100() * 0.9F;
      Vec3d targetPos = this.getEntityVirtPos(target, this.TargetBoxSync.getBool());
      float dst = Minecraft.player.getSmoothDistanceToCoordXZ((float)targetPos.xCoord, (float)targetPos.yCoord, (float)targetPos.zCoord);
      float cdst = this.getRedistance(this.Distance.getFloat());
      if (smartKeep) {
         double dstPardon = 0.25 + d + 0.2F;
         keep = MathUtils.clamp((float)dstPardon - MathUtils.getDifferenceOf(dst, cdst), 0.0F, 1.0F) * 90.0F;
      }

      float f2 = f;
      if (d3 == 0.0 && d2 == 0.0) {
         return new double[]{0.0, 0.0};
      } else {
         if (d3 != 0.0) {
            if (d2 > 0.0) {
               f2 = f + (d3 > 0.0 ? -keep : keep);
            } else if (d2 < 0.0) {
               f2 = f + (d3 > 0.0 ? keep : -keep);
            }

            d5 = 0.0;
            if (d3 > 0.0) {
               d4 = 1.0;
            } else if (d3 < 0.0) {
               d4 = -1.0;
            }
         }

         float moveYaw = f2 + 90.0F;
         double d6 = (double)MathHelper.cos(MathHelper.toRadians(moveYaw));
         double d7 = (double)MathHelper.sin(MathHelper.toRadians(moveYaw));
         double xCoord = d4 * d * d6 + d5 * d * d7;
         double zCoord = d4 * d * d7 - d5 * d * d6;
         return new double[]{xCoord, zCoord};
      }
   }

   float Motion2OnlyGetYaw(double d, float f, double d2, double d3, boolean smartKeep) {
      float keep = 90.0F - (float)keepPercent100() * 0.9F;
      Vec3d targetPos = this.getEntityVirtPos(target, this.TargetBoxSync.getBool());
      float dst = Minecraft.player.getSmoothDistanceToCoordXZ((float)targetPos.xCoord, (float)targetPos.yCoord, (float)targetPos.zCoord);
      float cdst = this.getRedistance(this.Distance.getFloat());
      if (smartKeep) {
         double dstPardon = 0.25 + d + 0.2F;
         keep = MathUtils.clamp((float)dstPardon - MathUtils.getDifferenceOf(dst, cdst), 0.0F, 1.0F) * 90.0F;
      }

      float f2 = f;
      if (d3 == 0.0 && d2 == 0.0) {
         return f;
      } else {
         if (d3 != 0.0) {
            if (d2 > 0.0) {
               f2 = f + (d3 > 0.0 ? -keep : keep);
            } else if (d2 < 0.0) {
               f2 = f + (d3 > 0.0 ? keep : -keep);
            }

            double d5 = 0.0;
            if (d3 > 0.0) {
               double d4 = 1.0;
            } else if (d3 < 0.0) {
               double var19 = -1.0;
            }
         }

         return f2 + 90.0F;
      }
   }

   static double getCurrentSpeed(boolean cutting) {
      if (Minecraft.player == null) {
         return 0.0;
      } else {
         TargetStrafe targetStrafe = get;
         double speed1 = (double)targetStrafe.SpeedF.getFloat();
         if (speed1 >= 0.2499 && speed1 < 0.2599) {
            speed1 -= (double)(Minecraft.player.ticksExisted % 2 + 1) * 1.0E-6;
         }

         if (Minecraft.player.hurtTime != 0 && targetStrafe.DamageBoost.getBool()) {
            speed1 = (double)targetStrafe.DamageSpeed.getFloat();
         } else if (!cutting && !get.isSmartSpeed() && get.CollideBoost.getBool()) {
            double motionSpeed = MoveMeHelp.getSpeed();
            if (motionSpeed < speed1) {
               motionSpeed = speed1;
            }

            if (motionSpeed >= 0.2399 && (double)targetStrafe.SpeedF.getFloat() >= 0.2399) {
               List<EntityLivingBase> bases = mc.world
                  .getLoadedEntityList()
                  .stream()
                  .map(Entity::getLivingBaseOf)
                  .filter(Objects::nonNull)
                  .filter(
                     base -> base != Minecraft.player
                           && !(base instanceof EntityArmorStand)
                           && base.canBeCollidedWith()
                           && Minecraft.player.boundingBox.expandXyz(Minecraft.player.onGround ? 0.7 : 0.55).intersectsWith(base.boundingBox)
                  )
                  .toList();
               if (!bases.isEmpty()) {
                  float boostCrate = bases.size() > 1 ? 2.0F : 1.0F;
                  double boostAddition = Minecraft.player.onGround
                        && !Minecraft.player.isJumping()
                        && !get.AutoJump.getBool()
                        && !Minecraft.player.isInLiquid()
                     ? 0.2
                     : (boostCrate == 2.0F ? 0.056 : 0.05);
                  speed1 = motionSpeed + boostAddition;
               }
            }
         }

         if (targetStrafe.isSmartSpeed()) {
            speed1 = speed;
            if (Speed.get.actived
               && (
                  Speed.get.AntiCheat.currentMode.equalsIgnoreCase("Guardian") && EntityLivingBase.isSunRiseDamaged
                     || Speed.get.AntiCheat.currentMode.equalsIgnoreCase("Matrix") && Speed.get.DamageBoost.getBool() && EntityLivingBase.isMatrixDamaged
               )) {
               if (Minecraft.player.onGround) {
                  speed1 *= speed < 0.62 ? 1.64 : 1.53;
               } else if (Minecraft.player.isJumping()
                  && Speed.get.actived
                  && Speed.get.AntiCheat.getMode().equalsIgnoreCase("Guardian")
                  && (!targetStrafe.DamageBoost.getBool() || !EntityLivingBase.isMatrixDamaged)) {
                  speed1 *= Minecraft.player.fallDistance == 0.0F
                     ? 1.001
                     : (
                        !Speed.canMatrixBoost()
                              || Minecraft.player.isHandActive()
                              || !(speed < 0.4) && (!((double)Minecraft.player.fallDistance > 0.65) || !(speed < 0.6))
                           ? 1.0
                           : 1.9
                     );
               }

               speed1 = MathUtils.clamp(
                  speed1,
                  0.2499 - (Minecraft.player.ticksExisted % 2 == 0 ? 5.0E-7 : 0.0),
                  1.17455998 - (Minecraft.player.ticksExisted % 2 == 0 ? 1.0E-7 : 0.0)
               );
            }

            boolean elytra = ElytraBoost.get.actived
               && (ElytraBoost.get.Mode.currentMode.equalsIgnoreCase("MatrixSpeed2") || ElytraBoost.get.Mode.currentMode.equalsIgnoreCase("MatrixFly3"))
               && ElytraBoost.canElytra();
            if (elytra) {
               if (ElytraBoost.flSpeed > speed) {
                  speed1 = ElytraBoost.flSpeed / 1.011;
               }
            } else if (Speed.get.actived
               && (
                  Speed.get.AntiCheat.currentMode.equalsIgnoreCase("Guardian") && EntityLivingBase.isSunRiseDamaged
                     || Speed.get.AntiCheat.currentMode.equalsIgnoreCase("Matrix") && Speed.get.DamageBoost.getBool() && EntityLivingBase.isMatrixDamaged
               )) {
               if (Minecraft.player.onGround) {
                  speed1 *= speed1 < 0.62 ? 1.64 : 1.528;
               } else if (Minecraft.player.isJumping() && Speed.get.actived && Speed.get.AntiCheat.currentMode.equalsIgnoreCase("Guardian")) {
                  speed1 *= Minecraft.player.fallDistance == 0.0F
                     ? 1.001
                     : (
                        !Speed.canMatrixBoost()
                              || Minecraft.player.isHandActive()
                              || !(speed1 < 0.4) && (!((double)Minecraft.player.fallDistance > 0.65) || !(speed1 < 0.6))
                           ? 1.0
                           : 1.9
                     );
               }

               speed1 = MathUtils.clamp(speed1, 0.2499 - (Minecraft.player.ticksExisted % 2 == 0 ? 5.0E-7 : 0.0), 1.3);
            }

            if (Speed.get.actived
               && Speed.get.AntiCheat.currentMode.equalsIgnoreCase("NCP")
               && (double)Speed.ncpSpeed > speed1
               && Speed.get.DamageBoost.getBool()) {
               speed1 = (double)Speed.ncpSpeed;
            }

            if (elytra && ElytraBoost.flSpeed > speed1) {
               speed1 = ElytraBoost.flSpeed;
            }

            if (Speed.iceGo) {
               speed1 = (double)((float)(Minecraft.player.isPotionActive(Potion.getPotionById(1)) ? 0.91 : 0.63) * 1.07F);
            }

            if (WaterSpeed.get.actived && WaterSpeed.get.Mode.getMode().equalsIgnoreCase("Matrix") && WaterSpeed.speedInWater / 1.061 > speed1) {
               speed1 = WaterSpeed.speedInWater / 1.061;
            }

            if (Minecraft.player.isElytraFlying() && (double)EntityLivingBase.getElytraSpeed > speed1) {
               speed1 = (double)EntityLivingBase.getElytraSpeed;
            }

            if (Fly.get.actived && MathUtils.clamp(Fly.flySpeed, 0.195F, 1.2F) > speed1) {
               speed1 = MathUtils.clamp(Fly.flySpeed, 0.195F, 1.2F);
            }

            if (Speed.get.isActived() && Speed.get.AntiCheat.currentMode.equalsIgnoreCase("MetaStrafe")) {
            }
         }

         if (Speed.get.actived
            && Speed.get.AntiCheat.currentMode.equalsIgnoreCase("Vulcan")
            && speed > 0.1
            && Minecraft.player.onGround
            && Minecraft.player.ticksExisted % 3 == 0) {
            Minecraft.player.motionY = 0.0391;
         }

         if (JesusSpeed.get.actived && JesusSpeed.isJesused) {
            if (!((double)Minecraft.player.fallDistance > 0.02) && !Minecraft.player.hasNewVersionMoves) {
               speed1 = 0.12;
            } else {
               Enchantment depth = Enchantments.DEPTH_STRIDER;
               int depthLvl = EnchantmentHelper.getEnchantmentLevel(depth, Minecraft.player.inventory.armorItemInSlot(0));
               boolean isSpeedPot = Minecraft.player.isPotionActive(MobEffects.SPEED)
                  && Minecraft.player.getActivePotionEffect(MobEffects.SPEED).getAmplifier() > 0;
               speed1 = 1.16F;
               if (!Minecraft.player.capabilities.allowFlying) {
                  int speedLvl = 0;
                  if (isSpeedPot) {
                     speedLvl = Minecraft.player.getActivePotionEffect(MobEffects.SPEED).getAmplifier();
                  }

                  if (isSpeedPot && depthLvl > 0) {
                     speed1 = speedLvl == 2 ? 2.249F : (speedLvl == 1 ? 1.6099F : 1.51F);
                  }
               }
            }
         }

         if (JesusSpeed.get.actived && JesusSpeed.get.JesusMode.currentMode.equalsIgnoreCase("Matrix7.0.2") && JesusSpeed.ticksLinked > 1) {
            speed1 = (double)JesusSpeed.getM7Speed();
         }

         if (Minecraft.player.isInWeb) {
            speed1 /= 2.0;
         }

         return speed1;
      }
   }

   void targetStrafeElement() {
      if (Minecraft.player.isInWater()) {
         mc.gameSettings.keyBindJump.pressed = true;
      } else if (mc.gameSettings.keyBindJump.pressed && !Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
         mc.gameSettings.keyBindJump.pressed = false;
      }

      if (Minecraft.player.onGround
         && (
            !JesusSpeed.get.isActived()
               || !JesusSpeed.get.LockJumping.getBool()
               || !JesusSpeed.get.JesusMode.getMode().equalsIgnoreCase("NcpNew")
               || mc.world.getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - 0.001, Minecraft.player.posZ)).getBlock() != Blocks.WATER
               || Minecraft.player.posY == (double)((int)Minecraft.player.posY)
         )
         && !Minecraft.player.isInWater()
         && !Minecraft.player.isInLava()
         && !Minecraft.player.isInWeb
         && this.AutoJump.getBool()
         && !Minecraft.player.isJumping()) {
         Minecraft.player.jump();
      }

      if (Minecraft.player.isCollidedHorizontally && Minecraft.player.ticksExisted % 2 == 0) {
         b = -b;
      }

      b = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()) ? 1 : (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()) ? -1 : b);
   }

   public static final boolean goStrafe() {
      return target != null
            && (!LongJump.get.actived || !MoveMeHelp.moveKeysPressed())
            && !PearlFlight.go
            && !AirStuck.canStopMotion()
            && !FreeCam.get.isActived()
         ? get.actived
         : false;
   }

   @Override
   public String getDisplayName() {
      return this.getDisplayByDouble(getCurrentSpeed(false));
   }

   @Override
   public void onToggled(boolean actived) {
      if (actived) {
         speed = 0.0;
         tempUpdatedSpeed = 0.0;
         target = HitAura.TARGET_ROTS != null
            ? HitAura.TARGET_ROTS
            : (mc.world.getLoadedEntityList().stream().anyMatch(entity -> entity == target) ? target : null);
         if (!goStrafe()) {
            ElytraBoost.get.targetStrafeCallReset();
            target = null;
         } else {
            this.elyCallUpdated = this.isElyCall();
         }
      } else {
         speed = 0.0;
         needSprintState = false;
         target = null;
         ElytraBoost.get.targetStrafeCallReset();
         this.elyCallUpdated = false;
         tempUpdatedSpeed = 0.0;
      }

      super.onToggled(actived);
   }

   @Override
   public void onUpdate() {
      target = HitAura.TARGET_ROTS != null
         ? HitAura.TARGET_ROTS
         : (mc.world.getLoadedEntityList().stream().anyMatch(entity -> entity == target) ? target : null);
      if (!goStrafe()) {
         ElytraBoost.get.targetStrafeCallReset();
         target = null;
         this.elyCallUpdated = false;
      } else {
         this.elyCallUpdated = this.isElyCall();
      }

      if (target != null
         && (!HitAura.get.actived || target != null && target.getHealth() == 0.0F || Minecraft.player.getSmoothDistanceToEntity(target) > getMaxRange())) {
         target = null;
         this.elyCallUpdated = false;
      }

      if (!goStrafe() && mc.currentScreen == null) {
         mc.gameSettings.keyBindJump.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
      }

      if (goStrafe()) {
         this.targetStrafeElement();
         if (target != null && this.onCanReverseBecauseChecks(target)) {
            b = -b;
         }
      }

      if (target != null) {
         if (!this.updateCanSpeedRotationMetaStrafe()) {
            if (!this.isSmartSpeed()
               && (get.CollideBoost.getBool() && (double)this.SpeedF.getFloat() >= 0.2399 || this.elyCallUpdated)
               && !isCallSpeedRotationMetaStrafe()) {
               tempUpdatedSpeed = getCurrentSpeed(false);
               if (!this.elyCallUpdated) {
                  this.getStrafe(
                     tempUpdatedSpeed, Minecraft.player.getSmoothDistanceToEntityXZ(target), this.getRedistance(this.Distance.getFloat()), false, isSmartKeep()
                  );
               }

               if (this.elyCallUpdated) {
                  float maxXZSpeed = 1.953F;
                  if (ElytraBoost.get.CustomSpeed.getBool() && ElytraBoost.get.StrafeDirs.getBool()) {
                     maxXZSpeed = MathUtils.lerp(
                        ElytraBoost.get.MotionXZ.getFloat() * 0.25F,
                        ElytraBoost.get.MotionXZ.getFloat(),
                        Math.min((float)Minecraft.player.ticksElytraFlying / 5.0F, 1.0F)
                     );
                  }

                  boolean speedNormalize = !ElytraBoost.get.GrimStrafed.getBool()
                     && ElytraBoost.get.StrafeDirs.getBool()
                     && ElytraBoost.get.StaticYMotions.getBool()
                     && ElytraBoost.get.NormalizeDirection.getBool();
                  if (speedNormalize) {
                     float speedBoostStepPerTick = ElytraBoost.get.CustomSpeed.getBool() ? ElytraBoost.get.MotionRunupDirect.getFloat() : 0.3F;
                     maxXZSpeed = (float)Math.min(MoveMeHelp.getCuttingSpeed() + (double)speedBoostStepPerTick, (double)ElytraBoost.get.MotionXZ.getFloat());
                  }

                  tempUpdatedSpeed = (double)maxXZSpeed;
                  double prevMCMotionX = Minecraft.player.motionX;
                  double prevMCMotionZ = Minecraft.player.motionZ;
                  this.getStrafe(
                     tempUpdatedSpeed, Minecraft.player.getSmoothDistanceToEntityXZ(target), this.getRedistance(this.Distance.getFloat()), false, isSmartKeep()
                  );
                  ElytraBoost.get.onTargetStrafeCallMotionsToFireworkMode(new double[]{Minecraft.player.motionX, Minecraft.player.motionZ});
                  Minecraft.player.motionX = prevMCMotionX;
                  Minecraft.player.motionZ = prevMCMotionZ;
               }
            } else {
               tempUpdatedSpeed = 0.0;
            }
         }
      }
   }

   @Override
   public void onMovement() {
      if (goStrafe() && !isCallSpeedRotationMetaStrafe()) {
         this.targetStrafeElement();
         if (!this.isSmartSpeed() && !this.elyCallUpdated) {
            double speed = Math.max(tempUpdatedSpeed, getCurrentSpeed(true));
            if (Speed.get.actived
               && Speed.get.AntiCheat.currentMode.equalsIgnoreCase("NCP")
               && (double)Speed.ncpSpeed > speed
               && Speed.get.DamageBoost.getBool()) {
               speed = (double)Speed.ncpSpeed;
            }

            this.getStrafe(speed, Minecraft.player.getSmoothDistanceToEntityXZ(target), this.getRedistance(this.Distance.getFloat()), true, isSmartKeep());
         }
      }
   }

   @EventTarget
   public void onMovementHui(EventPostMove move) {
      if (this.actived && this.isSmartSpeed() && !isCallSpeedRotationMetaStrafe()) {
         MatrixStrafeMovement.postMove(move.getHorizontalMove());
      }
   }

   @EventTarget
   public void onMovementHui2(EventAction move) {
      if (this.actived && goStrafe() && this.isSmartSpeed() && !isCallSpeedRotationMetaStrafe()) {
         if (!HitAura.get.noRotateTick) {
            MatrixStrafeMovement.actionEvent(move);
         } else {
            move.setSprintState(false);
         }
      }
   }

   public static boolean isCallSpeedRotationMetaStrafe() {
      return get != null && get.actived && callSpeedRotation != -1377.0F;
   }

   private boolean updateCanSpeedRotationMetaStrafe() {
      callSpeedRotation = -1377.0F;
      if (Minecraft.player != null
         && this.isSmartSpeed()
         && Speed.get.isActived()
         && Speed.get.AntiCheat.currentMode.equalsIgnoreCase("MetaStrafe")
         && !Speed.get.sleep
         && !Minecraft.player.isElytraFlying()) {
         Vec3d targetPos = this.getEntityVirtPos(target, this.TargetBoxSync.getBool());
         if (targetPos == Vec3d.ZERO) {
            return false;
         }

         float dst = Minecraft.player.getSmoothDistanceToCoordXZ((float)targetPos.xCoord, (float)targetPos.yCoord, (float)targetPos.zCoord);
         float current = this.getRedistance(this.Distance.getFloat());
         float pardon = 0.05F;
         int forward = dst > current + pardon ? 1 : (dst < current - pardon ? -1 : 0);
         float yaw = this.getRotations(target)[0];
         float keep = 90.0F - (float)keepPercent100() * 0.9F;
         float cdst = this.getRedistance(this.Distance.getFloat());
         if (isSmartKeep()) {
            double dstPardon = 0.25 + Speed.speedUpdated + 0.2F;
            keep = MathUtils.clamp((float)dstPardon - MathUtils.getDifferenceOf(dst, cdst), 0.0F, 1.0F) * 90.0F;
         }

         float f2 = yaw;
         double d4 = (double)forward;
         double d5 = (double)b;
         if (d4 != 0.0 || d5 != 0.0) {
            if (d4 != 0.0) {
               if (d5 > 0.0) {
                  f2 = yaw + (d4 > 0.0 ? -keep : keep);
               } else if (d5 < 0.0) {
                  f2 = yaw + (d4 > 0.0 ? keep : -keep);
               }

               d5 = 0.0;
               if (d4 > 0.0) {
                  d4 = 1.0;
               } else if (d4 < 0.0) {
                  d4 = -1.0;
               }
            }

            double d6 = (double)MathHelper.cos(MathHelper.toRadians(f2 + 90.0F));
            double d7 = (double)MathHelper.sin(MathHelper.toRadians(f2 + 90.0F));
            double mx = d4 * Speed.speedUpdated * d6 + d5 * Speed.speedUpdated * d7;
            double mz = d4 * Speed.speedUpdated * d7 - d5 * Speed.speedUpdated * d6;
            float moveYaw = RotationUtil.getVecNeeded(new Vec3d(0.0, 0.0, 0.0), new Vec3d(mx, 0.0, mz))[0];
            callSpeedRotation = moveYaw - 180.0F;
         }
      }

      return isCallSpeedRotationMetaStrafe();
   }

   @EventTarget
   public void onMovements(EventMove2 move) {
      if (this.actived) {
         if (!this.updateCanSpeedRotationMetaStrafe()) {
            boolean canBoost = !mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.addCoord(0.0, -0.09, 0.0)).isEmpty();
            boolean matrixSpeedDamageHop = Speed.get.actived
               && Speed.get.AntiCheat.currentMode.equalsIgnoreCase("Matrix")
               && Speed.get.StrafeDamageHop.getBool()
               && EntityLivingBase.isMatrixDamaged;
            speed = MatrixStrafeMovement.calculateSpeed(
               matrixSpeedDamageHop && Minecraft.player.fallDistance > 0.0F || Strafe.get.Mode.currentMode.equalsIgnoreCase("Strict"),
               move,
               matrixSpeedDamageHop || EntityLivingBase.isMatrixDamaged && this.DamageBoost.getBool() && canBoost,
               matrixSpeedDamageHop ? 0.15 : (double)this.DamageSpeed.getFloat()
            );
            if (EntityLivingBase.isMatrixDamaged && canBoost) {
               speed = MathUtils.clamp(speed, speed, 2.9F);
            }

            boolean elytra = ElytraBoost.get.actived
               && (ElytraBoost.get.Mode.currentMode.equalsIgnoreCase("MatrixSpeed2") || ElytraBoost.get.Mode.currentMode.equalsIgnoreCase("MatrixFly3"))
               && ElytraBoost.canElytra();
            if (elytra && ElytraBoost.flSpeed > speed) {
               speed = ElytraBoost.flSpeed;
            }

            if (Speed.iceGo) {
               speed = (double)((float)(Minecraft.player.isPotionActive(Potion.getPotionById(1)) ? 0.91 : 0.63) * 1.07F);
            }

            if (WaterSpeed.get.actived && WaterSpeed.get.Mode.getMode().equalsIgnoreCase("Matrix") && WaterSpeed.speedInWater / 1.06 > speed) {
               speed = WaterSpeed.speedInWater / 1.06;
            }

            if (Minecraft.player.isElytraFlying() && (double)EntityLivingBase.getElytraSpeed > speed) {
               speed = (double)EntityLivingBase.getElytraSpeed;
            }

            if (Fly.get.actived && Fly.flySpeed > speed) {
               speed = Fly.flySpeed / 5.0;
            }

            if (Minecraft.player.isHandActive() && !Minecraft.player.isJumping() && Minecraft.player.onGround) {
            }

            if (JesusSpeed.get.actived && JesusSpeed.get.JesusMode.currentMode.equalsIgnoreCase("Matrix7.0.2") && JesusSpeed.ticksLinked > 0) {
               speed = (double)JesusSpeed.getM7Speed();
            }

            boolean onlyCallElytraBoostFirework = false;
            if (ElytraBoost.get.isActived()
               && ElytraBoost.get.Mode.getMode().equalsIgnoreCase("Firework")
               && !ElytraBoost.get.GrimStrafed.getBool()
               && ElytraBoost.get.StrafeDirs.getBool()) {
               float maxXZSpeed = ElytraBoost.get.CustomSpeed.getBool() ? ElytraBoost.get.MotionXZ.getFloat() : 1.953F;
               onlyCallElytraBoostFirework = true;
               speed = Minecraft.player.ticksElytraFlying > 0
                  ? (double)MathUtils.lerp(maxXZSpeed * 0.25F, maxXZSpeed, Math.min((float)Minecraft.player.ticksElytraFlying / 5.0F, 1.0F))
                  : 0.0;
            }

            double finalSpeed = getCurrentSpeed(true);
            if (goStrafe() && this.isSmartSpeed() && this.actived && target != null) {
               Vec3d targetPos = this.getEntityVirtPos(target, this.TargetBoxSync.getBool());
               float dst = Minecraft.player.getSmoothDistanceToCoordXZ((float)targetPos.xCoord, (float)targetPos.yCoord, (float)targetPos.zCoord);
               float current = this.getRedistance(this.Distance.getFloat());
               float pardon = 0.05F;
               int feya = dst > current + pardon ? 1 : (dst < current - pardon ? -1 : 0);
               float yaw = this.getRotations(target)[0];
               if (this.SpeedLimitToDistance.getBool()) {
                  finalSpeed = Math.min(speed, (double)Math.max(dst, 0.05F));
               }

               this.Motion2(move, finalSpeed, yaw, (double)b, (double)feya, isSmartKeep());
            }
         }
      }
   }

   public void getStrafe(double speed, float getDist, float dist, boolean onMove, boolean smartKeep) {
      if (target != null) {
         float pardon = 0.05F;
         int feya = getDist > dist + pardon ? 1 : (getDist < dist - pardon ? -1 : 0);
         float yaw = this.getRotations(target)[0];
         if (this.SpeedLimitToDistance.getBool()) {
            Vec3d targetPos = this.getEntityVirtPos(target, this.TargetBoxSync.getBool());
            float dst = Minecraft.player.getSmoothDistanceToCoordXZ((float)targetPos.xCoord, (float)targetPos.yCoord, (float)targetPos.zCoord);
            speed = Math.min(speed, (double)dst);
         }

         this.Motion(speed, yaw, (double)b, (double)feya, onMove, smartKeep);
      }
   }

   @EventTarget
   public void onRender3D(Event3D event) {
      if (target != null && Minecraft.player.getSmoothDistanceToEntityXZ(target) <= getMaxRange() && this.RenderCurrentDist.getBool()) {
         float xzDistance = this.Distance.getFloat();
         float sataDistance = Minecraft.player.getSmoothDistanceToEntityXZ(target) / 2.0F;
         Vec3d pos = this.getEntityVirtPos(target, this.TargetBoxSync.getBool() && EntityBox.isRenderModelSyncPosAABB());
         double eX = pos.xCoord + (target.posX - target.lastTickPosX) * (double)event.getPartialTicks();
         double eY = pos.yCoord + (target.posY - target.lastTickPosY) * (double)event.getPartialTicks();
         double eZ = pos.zCoord + (target.posZ - target.lastTickPosZ) * (double)event.getPartialTicks();
         double meX = Minecraft.player.lastTickPosX + (Minecraft.player.posX - Minecraft.player.lastTickPosX) * (double)event.getPartialTicks();
         double meY = Minecraft.player.lastTickPosY + (Minecraft.player.posY - Minecraft.player.lastTickPosY) * (double)event.getPartialTicks();
         double meZ = Minecraft.player.lastTickPosZ + (Minecraft.player.posZ - Minecraft.player.lastTickPosZ) * (double)event.getPartialTicks();
         Vec3d overSataVec = BlockUtils.getOverallVec3d(new Vec3d(eX, eY, eZ), new Vec3d(meX, meY, meZ), 0.5F);
         double sataX = overSataVec.xCoord;
         double sataY = overSataVec.yCoord;
         double sataZ = overSataVec.zCoord;
         double glX = RenderManager.viewerPosX;
         double glY = RenderManager.viewerPosY;
         double glZ = RenderManager.viewerPosZ;
         boolean bloom = false;
         GL11.glPushMatrix();
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            bloom ? GlStateManager.DestFactor.ONE : GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
         );
         mc.entityRenderer.disableLightmap();
         GL11.glEnable(3042);
         GL11.glEnable(2832);
         GL11.glLineWidth(1.0F);
         GL11.glDisable(3553);
         GL11.glDisable(2929);
         GL11.glDisable(2896);
         GL11.glShadeModel(7425);
         GL11.glTranslated(-glX, -glY, -glZ);
         GL11.glBegin(3);

         for (int i = 0; i <= 360; i += 6) {
            double x = sataX - (double)(MathHelper.sin(MathHelper.toRadians((float)i)) * sataDistance);
            double z = sataZ + (double)(MathHelper.cos(MathHelper.toRadians((float)i)) * sataDistance);
            int c = ClientColors.getColor1((int)((float)i * 3.0F));
            RenderUtils.glColor(c);
            GL11.glVertex3d(x, sataY, z);
         }

         GL11.glEnd();
         GL11.glBegin(3);

         for (int i = 0; i <= 360; i += 6) {
            float r = sataDistance * 2.0F;
            double x = eX - (double)(MathHelper.sin(MathHelper.toRadians((float)i)) * r);
            double z = eZ + (double)(MathHelper.cos(MathHelper.toRadians((float)i)) * r);
            int c = ClientColors.getColor1((int)((float)i * 3.0F), 0.333333F);
            RenderUtils.glColor(c);
            GL11.glVertex3d(x, sataY, z);
         }

         GL11.glEnd();
         GL11.glBegin(3);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glVertex3d(eX, sataY, eZ);
         GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.0F);
         GL11.glVertex3d(eX - (eX - meX) / 2.0, sataY, eZ - (eZ - meZ) / 2.0);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glVertex3d(meX, sataY, meZ);
         GL11.glEnd();
         GL11.glPointSize(8.0F);
         GL11.glBegin(0);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glVertex3d(eX, sataY, eZ);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glVertex3d(meX, sataY, meZ);
         GL11.glEnd();
         GL11.glPointSize(1.0F);
         float yaw = (float)Math.toDegrees(Math.atan2(Minecraft.player.posZ - Minecraft.player.prevPosZ, Minecraft.player.posX - Minecraft.player.prevPosX));
         yaw = yaw < 0.0F ? yaw + 360.0F : yaw;
         yaw -= (yaw - yaw) * event.getPartialTicks();
         double dx = Minecraft.player.posX - Minecraft.player.prevPosX;
         double dz = Minecraft.player.posZ - Minecraft.player.prevPosZ;
         double antsRange = Math.sqrt(dx * dx + dz * dz);
         GL11.glBegin(3);
         GL11.glVertex3d(meX, sataY, meZ);
         GL11.glVertex3d(
            meX - (double)MathHelper.sin(MathHelper.toRadians(yaw - 90.0F)) * antsRange,
            sataY,
            meZ + (double)MathHelper.cos(MathHelper.toRadians(yaw - 90.0F)) * antsRange
         );
         GL11.glEnd();
         GL11.glPointSize(8.0F);
         GL11.glBegin(0);
         GL11.glVertex3d(
            meX - (double)MathHelper.sin(MathHelper.toRadians(yaw - 90.0F)) * antsRange,
            sataY,
            meZ + (double)MathHelper.cos(MathHelper.toRadians(yaw - 90.0F)) * antsRange
         );
         GL11.glEnd();
         yaw = (float)Math.toDegrees(Math.atan2(target.posZ - Minecraft.player.prevPosZ, target.posX - target.prevPosX));
         yaw = yaw < 0.0F ? yaw + 360.0F : yaw;
         yaw -= (yaw - yaw) * event.getPartialTicks();
         dx = target.posX - target.prevPosX;
         dz = target.posZ - target.prevPosZ;
         antsRange = Math.sqrt(dx * dx + dz * dz);
         GL11.glBegin(3);
         GL11.glVertex3d(eX, sataY, eZ);
         GL11.glVertex3d(
            eX - (double)MathHelper.sin(MathHelper.toRadians(yaw - 90.0F)) * antsRange,
            sataY,
            eZ + (double)MathHelper.cos(MathHelper.toRadians(yaw - 90.0F)) * antsRange
         );
         GL11.glEnd();
         GL11.glPointSize(8.0F);
         GL11.glBegin(0);
         GL11.glVertex3d(
            eX - (double)MathHelper.sin(MathHelper.toRadians(yaw - 90.0F)) * antsRange,
            sataY,
            eZ + (double)MathHelper.cos(MathHelper.toRadians(yaw - 90.0F)) * antsRange
         );
         GL11.glEnd();
         GL11.glPointSize(1.0F);
         GL11.glTranslated(glX, glY, glZ);
         GL11.glLineWidth(1.0F);
         GL11.glShadeModel(7424);
         GL11.glEnable(3553);
         GL11.glEnable(2929);
         GlStateManager.enableAlpha();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
         );
         GlStateManager.resetColor();
         GL11.glPopMatrix();
      }
   }
}
