package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.govno.client.Client;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventSendPacket;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.RandomUtils;
import ru.govno.client.utils.Combat.GCDFix;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.MathUtils;

public class BowAimbot extends Module {
   public static BowAimbot get;
   ModeSettings ShotTo;
   ModeSettings RotationType;
   FloatSettings Range;
   BoolSettings Walls;
   BoolSettings Predict;
   public static float yaw;
   public static float pitch;
   public boolean doRotate = false;
   public static EntityLivingBase target = null;

   public BowAimbot() {
      super("BowAimbot", 0, Module.Category.COMBAT);
      this.settings.add(this.ShotTo = new ModeSettings("ShotTo", "Chestplate", this, new String[]{"Boots", "Leggings", "Chestplate", "Helmet"}));
      this.settings.add(this.RotationType = new ModeSettings("RotationType", "Silent", this, new String[]{"Silent", "Camera", "Packet"}));
      this.settings.add(this.Range = new FloatSettings("Range", 25.0F, 100.0F, 3.0F, this));
      this.settings.add(this.Walls = new BoolSettings("Walls", false, this));
      this.settings.add(this.Predict = new BoolSettings("Predict", true, this));
      this.setDemand(0, 1);
      get = this;
   }

   private boolean doEntityBoxPredictSync() {
      return true;
   }

   private float theta(double v, double g, double x, double y) {
      double yv = 2.0 * y * v * v;
      double gx = g * x * x;
      double g2 = g * (gx + yv);
      double insqrt = v * v * v * v - g2;
      double sqrt = Math.sqrt(insqrt);
      double numerator = v * v + sqrt;
      double numerator2 = v * v - sqrt;
      double atan1 = Math.atan2(numerator, g * x);
      double atan2 = Math.atan2(numerator2, g * x);
      return (float)Math.min(atan1, atan2);
   }

   private List<Vec3d> getPointsOfThrowable(int maxDensity, float[] rotation) {
      List<Vec3d> vecs = new ArrayList<>();
      if (mc.world == null) {
         return vecs;
      } else {
         EntityPlayer entityOf = getMe();
         if (entityOf != null && entityOf.isBowing()) {
            float rotYaw = rotation[0];
            float rotPitch = rotation[1];
            float[] selfHeadRotateWR = new float[]{rotYaw, rotPitch, MathHelper.toRadians(rotYaw), MathHelper.toRadians(rotPitch)};
            Vec3d playerVector = new Vec3d(entityOf.posX, entityOf.posY + (double)entityOf.getEyeHeight(), entityOf.posZ);
            double throwOfX = playerVector.xCoord;
            double throwOfY = playerVector.yCoord;
            double throwOfZ = playerVector.zCoord;
            double shiftX = (double)(-MathHelper.sin(selfHeadRotateWR[2]) * MathHelper.cos(selfHeadRotateWR[3]))
               + (entityOf.capabilities.isFlying ? 0.0 : entityOf.posX - entityOf.lastTickPosX);
            double shiftY = (double)(-MathHelper.sin(selfHeadRotateWR[3])) + (entityOf.capabilities.isFlying ? 0.0 : entityOf.posY - entityOf.lastTickPosY);
            double shiftZ = (double)(MathHelper.cos(selfHeadRotateWR[2]) * MathHelper.cos(selfHeadRotateWR[3]))
               + (entityOf.capabilities.isFlying ? 0.0 : entityOf.posZ - entityOf.lastTickPosZ);
            double throwMotion = Math.sqrt(shiftX * shiftX + shiftY * shiftY + shiftZ * shiftZ);
            shiftX /= throwMotion;
            shiftY /= throwMotion;
            shiftZ /= throwMotion;
            float tightPower = (72000.0F - (float)entityOf.getItemInUseCount()) / 20.0F;
            tightPower = (tightPower * tightPower + tightPower * 2.0F) / 3.0F;
            tightPower = tightPower < 0.1F ? 0.0F : (tightPower > 1.0F ? 1.0F : tightPower);
            tightPower *= 3.0F;
            shiftX *= (double)tightPower;
            shiftY *= (double)tightPower;
            shiftZ *= (double)tightPower;

            while (maxDensity > 0) {
               vecs.add(new Vec3d(throwOfX, throwOfY, throwOfZ));
               throwOfX += shiftX * 0.1;
               throwOfY += shiftY * 0.1;
               throwOfZ += shiftZ * 0.1;
               double asellate = 0.999;
               shiftX *= asellate;
               shiftY = shiftY * asellate - 0.005;
               shiftZ *= asellate;
               if (mc.world.rayTraceBlocks(playerVector, new Vec3d(throwOfX, throwOfY, throwOfZ)) != null) {
                  break;
               }

               maxDensity--;
            }

            return vecs;
         } else {
            return vecs;
         }
      }
   }

   private int getCurrentTicksArrowFly(EntityLivingBase base) {
      float[] rotation = this.getYawPitch(base, false);
      return this.getPointsOfThrowable(500, rotation).size();
   }

   private float getTotalPredictTicks(EntityLivingBase base, boolean calc) {
      float ticks = 0.0F;
      if (mc.world != null && getMe() != null && this.Predict.getBool()) {
         NetHandlerPlayClient connection;
         NetworkPlayerInfo info;
         if ((connection = mc.getConnection()) != null && (info = connection.getPlayerInfo(Minecraft.player.getUniqueID())) != null) {
            float pingedTicks = Math.max(Math.min(((float)info.getResponseTime() - 49.0F) / 50.0F, 20.0F), 0.0F);
            ticks += pingedTicks;
         }

         if (calc) {
            boolean awp = Criticals.get != null
               && Criticals.get.isActived()
               && Criticals.get.Bowing.getBool()
               && Criticals.get.BowMode.getMode().equalsIgnoreCase("Vanilla");
            if (awp) {
               ticks++;
            } else {
               ticks += (float)this.getCurrentTicksArrowFly(base) / 10.0F;
            }
         }

         return ticks;
      } else {
         return ticks;
      }
   }

   private Vec3d getValidBowingEntityRepos(EntityLivingBase base, boolean calc) {
      Vec3d pos = base.getPositionVector();
      if (this.doEntityBoxPredictSync()) {
         AxisAlignedBB aabb = base.getEntityBoundingBoxCL();
         if (aabb != null) {
            pos.xCoord = aabb.minX + (aabb.maxX - aabb.minX) / 2.0;
            pos.yCoord = aabb.minY;
            pos.zCoord = aabb.minZ + (aabb.maxZ - aabb.minZ) / 2.0;
         }
      }

      float predictValue = this.getTotalPredictTicks(base, calc);
      double deltaMotionX = (base.posX - base.prevPosX) * (double)predictValue;
      double deltaMotionY = (base.posY - base.prevPosY) * (double)predictValue;
      double deltaMotionZ = (base.posZ - base.prevPosZ) * (double)predictValue;
      deltaMotionX = Math.min(Math.max(deltaMotionX, -7.0), 7.0);
      deltaMotionY = Math.min(deltaMotionY / 4.0, 0.42F);
      deltaMotionZ = Math.min(Math.max(deltaMotionZ, -7.0), 7.0);
      return pos.addVector(deltaMotionX, deltaMotionY, deltaMotionZ);
   }

   private float getLaunchAngle(EntityLivingBase entity, double v, double g, Vec3d entityValidPos) {
      String mode = this.ShotTo.currentMode;
      float pc = mode.equalsIgnoreCase("Boots")
         ? entity.getEyeHeight() / 8.0F
         : (
            mode.equalsIgnoreCase("Leggings")
               ? entity.getEyeHeight() / 3.0F
               : (
                  mode.equalsIgnoreCase("Chestplate")
                     ? entity.getEyeHeight() / 1.85F
                     : (mode.equalsIgnoreCase("Helmet") ? entity.getEyeHeight() / 1.2F : entity.getEyeHeight())
               )
         );
      double xDiff = entityValidPos.xCoord - getMe().posX;
      double zDiff = entityValidPos.zCoord - getMe().posZ;
      double yDiff = entityValidPos.yCoord
         + (double)pc
         - (getMe().posY + (double)getMe().getEyeHeight())
         - (getMe().posY - getMe().lastTickPosY) * (double)((float)Math.sqrt(xDiff * xDiff + zDiff * zDiff)) / Math.PI;
      double xCoord = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
      return this.theta(v + 2.0, g, xCoord, yDiff);
   }

   private float[] getYawPitch(EntityLivingBase entity, boolean calc) {
      float akb = (float)getMe().getItemInUseMaxCount() / 20.0F;
      akb = (akb * akb + akb * 2.0F) / 3.0F;
      akb = MathHelper.clamp_float(akb, 0.0F, 1.0F);
      double v = (double)(akb * 3.0F);
      double g = 0.05F;
      if (akb > 1.0F) {
         akb = 1.0F;
      }

      Vec3d entityValidPos = this.getValidBowingEntityRepos(entity, calc);
      float bowTr = Criticals.get.isActived() && Criticals.get.Bowing.getBool() && Criticals.get.BowMode.getMode().equalsIgnoreCase("Vanilla")
         ? -3.0F
         : (float)((double)((float)(-Math.toDegrees((double)this.getLaunchAngle(entity, v, g, entityValidPos)))) - 4.35F);
      double diffX = entityValidPos.xCoord - getMe().posX;
      double diffZ = entityValidPos.zCoord - getMe().posZ;
      float tThetaYaw = (float)(Math.atan2(diffZ, diffX) * 180.0 / Math.PI - 90.0);
      tThetaYaw = getMe().rotationYaw + GCDFix.getFixedRotation(MathHelper.wrapAngleTo180_float(tThetaYaw - getMe().rotationYaw));
      return new float[]{tThetaYaw, MathUtils.clamp(bowTr, -90.0F, 90.0F)};
   }

   private static EntityPlayer getMe() {
      return Minecraft.player;
   }

   private boolean entityIsCurrentToFilter(EntityLivingBase entity) {
      return entity != null
         && entity.getHealth() != 0.0F
         && !(entity instanceof EntityPlayerSP)
         && !(entity instanceof EntityArmorStand)
         && !(entity instanceof EntityEnderman)
         && (this.Walls.getBool() || getMe().canEntityBeSeen(entity))
         && (!(entity instanceof EntityPlayer player) || !player.isCreative())
         && getMe().getDistanceToEntity(entity) <= this.Range.getFloat()
         && !Client.friendManager.isFriend(entity.getName())
         && !Client.summit(entity);
   }

   private double entitySortValue(EntityLivingBase entity) {
      if (entity != null) {
         boolean cameraRotate = this.RotationType.getMode().equalsIgnoreCase("Camera");
         float[] rotationsToCheck = cameraRotate
            ? this.getYawPitch(entity, true)
            : RotationUtil.getVecNeeded(this.getValidBowingEntityRepos(entity, false), Minecraft.player.getPositionEyes(mc.getRenderPartialTicks()));
         if (rotationsToCheck != null) {
            double yawDiff;
            double pitchDiff;
            if (cameraRotate) {
               yawDiff = (double)Math.abs(
                  RotationUtil.getAngleDifference(
                     rotationsToCheck[0], Minecraft.player.rotationYaw + (Minecraft.player.rotationYaw - Minecraft.player.lastReportedYaw)
                  )
               );
               pitchDiff = (double)MathUtils.getDifferenceOf(
                  rotationsToCheck[1], Minecraft.player.rotationPitch + (Minecraft.player.rotationPitch - EntityPlayerSP.lastReportedPitch)
               );
            } else {
               yawDiff = (double)Math.abs(RotationUtil.getAngleDifference(rotationsToCheck[0], Minecraft.player.rotationYaw));
               pitchDiff = (double)MathUtils.getDifferenceOf(rotationsToCheck[1], Minecraft.player.rotationPitch);
            }

            return Math.sqrt(yawDiff * yawDiff / 2.0 + pitchDiff * pitchDiff);
         }
      }

      return 360.0;
   }

   public final EntityLivingBase getCurrentTarget() {
      if (LongJump.get.isActived() && LongJump.doBow) {
         return null;
      } else {
         return !getMe().isBowing() || HitAura.TARGET_ROTS != null && HitAura.get.actived && !HitAura.get.Rotation.currentMode.equalsIgnoreCase("None")
            ? null
            : mc.world
               .getLoadedEntityList()
               .stream()
               .map(Entity::getLivingBaseOf)
               .filter(Objects::nonNull)
               .filter(e -> this.entityIsCurrentToFilter(e))
               .sorted(Comparator.comparingDouble(t -> this.entitySortValue(t)))
               .findFirst()
               .orElse(null);
      }
   }

   public final EntityLivingBase getTarget() {
      return target;
   }

   public static float[] getVirt() {
      return new float[]{yaw, Criticals.get.actived && Criticals.get.Bowing.getBool() ? -1.0F : pitch};
   }

   private void virtRotate(EventPlayerMotionUpdate e, EntityLivingBase entity) {
      if (getMe().isBowing() && this.entityIsCurrentToFilter(entity) && MathUtils.getDifferenceOf(getMe().rotationPitch, 0.0F) < 60.0F) {
         this.doRotate = true;
         yaw = this.getYawPitch(entity, true)[0];
         pitch = this.getYawPitch(entity, true)[1];
         float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
         float gcd = f * f * f * 1.2F + (float)RandomUtils.randomNumber((int)f, (int)(-f));
         yaw = yaw - yaw % gcd % gcd;
         pitch = pitch - pitch % gcd % gcd;
      } else if (MathUtils.getDifferenceOf(yaw, e.getYaw()) >= 1.0F && MathUtils.getDifferenceOf(pitch, e.getPitch()) >= 1.0F && this.doRotate) {
         yaw = yaw + MathUtils.clamp(e.getYaw() - yaw, -45.0F, 45.0F);
         pitch = pitch + MathUtils.clamp(e.getPitch() - pitch, -15.0F, 15.0F);
         float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
         float gcd = f * f * f * 1.2F + (float)RandomUtils.randomNumber((int)f, (int)(-f));
         yaw = yaw - yaw % gcd % gcd;
         pitch = pitch - pitch % gcd % gcd;
      } else {
         this.doRotate = false;
         yaw = e.getYaw();
         pitch = e.getPitch();
      }
   }

   private void rotate(EventPlayerMotionUpdate e) {
      if (e == null) {
         mc.getConnection().preSendPacket(new CPacketPlayer.Rotation(yaw, pitch, Minecraft.player.onGround));
         getMe().rotationYawHead = yaw;
         getMe().renderYawOffset = yaw;
         getMe().rotationPitchHead = pitch;
      } else {
         e.setYaw(yaw);
         e.setPitch(pitch);
         getMe().rotationYawHead = yaw;
         getMe().renderYawOffset = yaw;
         getMe().rotationPitchHead = pitch;
         if (this.RotationType.getMode().equalsIgnoreCase("Camera")) {
            getMe().rotationYaw = yaw;
            getMe().rotationPitch = pitch;
         }
      }
   }

   @EventTarget
   public void onPlayerMotionUpdate(EventPlayerMotionUpdate e) {
      if (this.actived && mc.world != null && getMe() != null) {
         target = this.getCurrentTarget();
         this.virtRotate(e, target);
         if (this.doRotate && !this.RotationType.getMode().equalsIgnoreCase("Packet")) {
            this.rotate(e);
         }
      }
   }

   @EventTarget
   public void onSend(EventSendPacket event) {
      if (this.isActived()
         && event.getPacket() instanceof CPacketPlayerDigging packet
         && packet.getAction() == CPacketPlayerDigging.Action.RELEASE_USE_ITEM
         && this.doRotate) {
         this.rotate(null);
      }
   }

   @Override
   public void onToggled(boolean actived) {
      target = null;
      this.doRotate = false;
      super.onToggled(actived);
   }
}
