package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec2f;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Movement.MoveMeHelp;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class Penis extends Module {
   private final FloatSettings Length;
   private final BoolSettings HitAuraBonus;
   private Penis.PenisController mcPlayerPenis = null;

   public Penis() {
      super("Penis", 0, Module.Category.MISC);
      this.settings.add(this.Length = new FloatSettings("Length", 2.25F, 5.0F, 1.0F, this));
      this.settings.add(this.HitAuraBonus = new BoolSettings("HitAuraBonus", true, this));
      this.setDemand(1, 2);
   }

   @Override
   public boolean isBetaModule() {
      return true;
   }

   @Override
   public void onToggled(boolean actived) {
      if (actived) {
         if (Minecraft.player != null) {
            this.mcPlayerPenis = new Penis.PenisController(Minecraft.player, (double)this.Length.getAnimation());
         }
      } else {
         this.mcPlayerPenis = null;
      }

      super.onToggled(actived);
   }

   @Override
   public void onUpdate() {
      if (Minecraft.player != null && Minecraft.player.ticksExisted == 1) {
         this.mcPlayerPenis = new Penis.PenisController(Minecraft.player, (double)this.Length.getAnimation());
      }

      if (this.mcPlayerPenis != null) {
         this.mcPlayerPenis.update();
      }

      super.onUpdate();
   }

   @Override
   public void alwaysRender3D(float partialTicks) {
      if (this.isActived()) {
         if (this.mcPlayerPenis != null) {
            this.mcPlayerPenis.setLength((double)this.Length.getAnimation());
            RenderUtils.setup3dForBlockPos(() -> {
               if (mc.gameSettings.thirdPersonView == 0) {
                  Vec3d translate = WorldRender.get.getLastTranslated();
                  GL11.glTranslated(-translate.xCoord, -translate.yCoord, -translate.zCoord);
               }

               GL11.glEnable(2929);
               GL11.glDisable(2884);
               this.renderPenis(this.mcPlayerPenis, partialTicks);
               GL11.glEnable(2884);
            }, false);
         }
      }
   }

   private void renderPenis(Penis.PenisController penisController, float partialTicks) {
      Vec3d[] vecs3d2 = penisController.getTwoRenderVecs(partialTicks, false);
      Vec3d[] vecs3d22 = penisController.getTwoRenderVecs(partialTicks, true);
      Vec3d vecStart = vecs3d2[0];
      Vec3d vecEnd = vecs3d2[1];
      Vec3d vecStart2 = vecs3d22[0];
      Vec3d vecEnd2 = vecs3d22[1];
      float[] dirs = RotationUtil.getVecNeeded(vecEnd, vecStart);
      float[] dirsRad = new float[]{MathHelper.toRadians(dirs[0]), MathHelper.toRadians(dirs[1])};
      float[] dirsRadP90 = new float[]{MathHelper.toRadians(dirs[0] + 90.0F), dirsRad[1]};
      float[] dirsRadN90 = new float[]{MathHelper.toRadians(dirs[0] - 90.0F), dirsRad[1]};
      double length = vecStart.distanceTo(vecEnd);
      double lengthRender = vecStart2.distanceTo(vecEnd2);
      float zalupaWidth = (float)(length / 15.0);
      float golovkaSize = zalupaWidth * 1.2F;
      float eggsScale = zalupaWidth * 1.3F;
      double eggsExtendSide = (double)(eggsScale / 1.5F);
      double eggsExtendForward = (double)(-eggsScale / 3.0F);
      double eggsExtendY = (double)(eggsScale / 6.0F);
      int golovkaCol0 = ColorUtils.getColor(201, 125, 125);
      this.sph3d(vecEnd2, (double)golovkaSize, golovkaCol0, 36, 7);
      Vec3d leftEggVec = vecStart2.addVector(
         -Math.sin((double)dirsRadN90[0]) * eggsExtendSide - Math.sin((double)dirsRad[0]) * eggsExtendForward,
         eggsExtendY,
         Math.cos((double)dirsRadN90[0]) * eggsExtendSide + Math.cos((double)dirsRad[0]) * eggsExtendForward
      );
      Vec3d rightEggVec = vecStart2.addVector(
         -Math.sin((double)dirsRadP90[0]) * eggsExtendSide - Math.sin((double)dirsRad[0]) * eggsExtendForward,
         eggsExtendY,
         Math.cos((double)dirsRadP90[0]) * eggsExtendSide + Math.cos((double)dirsRad[0]) * eggsExtendForward
      );
      int eggCol0 = ColorUtils.getColor(148, 103, 94);
      this.sph3d(leftEggVec, (double)eggsScale, eggCol0, 36, 7);
      this.sph3d(rightEggVec, (double)eggsScale, eggCol0, 36, 7);
      int eggVolosikiCol0 = ColorUtils.toDark(eggCol0, 0.33333F);
      int eggVolosikiCol1 = ColorUtils.toDark(ColorUtils.swapAlpha(eggCol0, (float)ColorUtils.getAlphaFromColor(eggCol0) * 0.5F), 0.25F);
      float volosikiSize = eggsScale * 0.5F;
      GL11.glLineWidth(0.25F);
      GL11.glEnable(2848);
      GL11.glDepthMask(false);
      this.sph3dVolosiki(leftEggVec, (double)eggsScale, (double)(eggsScale + volosikiSize), eggVolosikiCol0, eggVolosikiCol1, 18, 1);
      this.sph3dVolosiki(rightEggVec, (double)eggsScale, (double)(eggsScale + volosikiSize), eggVolosikiCol0, eggVolosikiCol1, 18, 1);
      GL11.glDepthMask(true);
      GL11.glDisable(2848);
      GL11.glLineWidth(1.0F);
      int zalupaCol0 = ColorUtils.getColor(135, 87, 68);
      int zalupaCol1 = ColorUtils.getColor(166, 114, 104);
      this.cil3d(vecStart2, vecEnd2, lengthRender, (double)zalupaWidth, (double)zalupaWidth, zalupaCol0, zalupaCol1, 36, 8);
   }

   private void cil3d(Vec3d vec0, Vec3d vec1, double lengthMax, double radius0, double radius1, int c0, int c1, int segments, int begin) {
      double length = Math.max(vec0.distanceTo(vec1), lengthMax);
      float[] dirs = RotationUtil.getVecNeeded(vec1, vec0);
      dirs[1] += 90.0F;
      RenderUtils.buffer.begin(begin, DefaultVertexFormats.POSITION_COLOR);
      float yawStep = 360.0F / (float)segments;

      for (float yaw = 0.0F; yaw <= 360.0F; yaw += yawStep) {
         float rad = MathHelper.toRadians(yaw);
         RenderUtils.buffer.pos((double)MathHelper.sin(rad) * radius0, 0.0, (double)MathHelper.cos(rad) * radius0).color(c0).endVertex();
         RenderUtils.buffer.pos((double)MathHelper.sin(rad) * radius1, length, (double)MathHelper.cos(rad) * radius1).color(c1).endVertex();
      }

      GL11.glPushMatrix();
      GL11.glTranslated(vec0.xCoord, vec0.yCoord, vec0.zCoord);
      GL11.glRotatef(dirs[0], 0.0F, -1.0F, 0.0F);
      GL11.glRotatef(dirs[1], 1.0F, 0.0F, 0.0F);
      GL11.glCullFace(1028);
      RenderUtils.tessellator.draw();
      GL11.glCullFace(1029);
      GL11.glPopMatrix();
   }

   private Vec3d computeSphOff(Vec3d center, float yaw, float pitch, double range) {
      float radianYaw = MathHelper.toRadians(yaw);
      float sinYaw = MathHelper.sin(radianYaw);
      float cosYaw = -MathHelper.cos(radianYaw);
      float radianPitch = MathHelper.toRadians(pitch);
      float sinPitch = MathHelper.sin(radianPitch);
      float cosPitch = -MathHelper.cos(radianPitch);
      return center.addVector((double)(sinYaw * cosPitch) * range, (double)sinPitch * range, (double)(cosYaw * cosPitch) * range);
   }

   private void sph3d(Vec3d vec, double radius, int c, int segments2, int begin) {
      float radStep = 360.0F / (float)segments2;
      RenderUtils.buffer.begin(begin, DefaultVertexFormats.POSITION_COLOR);
      float prevPitch = -90.0F;

      for (float pitch = -90.0F + radStep; pitch <= 90.0F; pitch += radStep) {
         float prevYaw = -radStep;

         for (float yaw = 0.0F; yaw < 360.0F; yaw += radStep) {
            Vec3d p0 = this.computeSphOff(vec, prevYaw, prevPitch, radius);
            Vec3d p1 = this.computeSphOff(vec, yaw, prevPitch, radius);
            Vec3d p2 = this.computeSphOff(vec, yaw, pitch, radius);
            Vec3d p3 = this.computeSphOff(vec, prevYaw, pitch, radius);
            RenderUtils.buffer.pos(p0.xCoord, p0.yCoord, p0.zCoord).color(c).endVertex();
            RenderUtils.buffer.pos(p1.xCoord, p1.yCoord, p1.zCoord).color(c).endVertex();
            RenderUtils.buffer.pos(p2.xCoord, p2.yCoord, p2.zCoord).color(c).endVertex();
            RenderUtils.buffer.pos(p3.xCoord, p3.yCoord, p3.zCoord).color(c).endVertex();
            prevYaw = yaw;
         }

         prevPitch = pitch;
      }

      GL11.glCullFace(1028);
      RenderUtils.tessellator.draw();
      GL11.glCullFace(1029);
   }

   private void sph3dVolosiki(Vec3d vec, double radius0, double radius1, int c0, int c1, int segments2, int begin) {
      float radStep = 360.0F / (float)segments2;
      RenderUtils.buffer.begin(begin, DefaultVertexFormats.POSITION_COLOR);

      for (float pitch = -90.0F + radStep; pitch < 90.0F; pitch += radStep) {
         for (float yaw = 0.0F; yaw < 360.0F; yaw += radStep) {
            Vec3d p0 = this.computeSphOff(vec, yaw, pitch, radius0);
            Vec3d p1 = this.computeSphOff(vec, yaw, pitch, radius1);
            RenderUtils.buffer.pos(p0.xCoord, p0.yCoord, p0.zCoord).color(c0).endVertex();
            RenderUtils.buffer.pos(p1.xCoord, p1.yCoord, p1.zCoord).color(c1).endVertex();
         }
      }

      GL11.glCullFace(1028);
      RenderUtils.tessellator.draw();
      GL11.glCullFace(1029);
   }

   private float[] updateDirsFromStartACollide(
      float[] prevRot,
      Vec3d start,
      double length,
      int scanYawCount,
      int scanPitchCount,
      float clampYawMin,
      float clampYawMax,
      float clampPitchMin,
      float clampPitchMax
   ) {
      List<Vec2f> allYawPitchNoCollide = new ArrayList<>();
      if (mc.world != null) {
         Vec3d rayStaticOffset = new Vec3d(0.0, -length / 15.0, 0.0);
         float yaw = -180.0F;

         while (yaw < 180.0F) {
            float pitch = -90.0F;

            while (pitch < 90.0F) {
               float yawRad = MathHelper.toRadians(yaw);
               float pitchRad = MathHelper.toRadians(pitch);
               Vec3d end = start.addVector(
                  (double)(-MathHelper.sin(yawRad) * MathHelper.cos(pitchRad)) * length,
                  (double)MathHelper.sin(pitchRad) * -length,
                  (double)(MathHelper.cos(yawRad) * MathHelper.cos(pitchRad)) * length
               );
               RayTraceResult result = mc.world.rayTraceBlocks(start.add(rayStaticOffset), end.add(rayStaticOffset), false, true, true);
               if (result == null || result.typeOfHit == RayTraceResult.Type.MISS) {
                  float IclampYawMin = yaw + MathUtils.wrapAngleTo180_float(clampYawMin - yaw);
                  float IclampYawMax = yaw + MathUtils.wrapAngleTo180_float(clampYawMax - yaw);
                  if (yaw > IclampYawMin && yaw < IclampYawMax && pitch > clampPitchMin && pitch < clampPitchMax) {
                     allYawPitchNoCollide.add(new Vec2f(yaw, pitch));
                  }
               }

               pitch += 180.0F / (float)scanPitchCount;
            }

            yaw += 360.0F / (float)scanYawCount;
         }
      }

      if (allYawPitchNoCollide.isEmpty()) {
         return prevRot;
      } else {
         if (allYawPitchNoCollide.size() > 1) {
            allYawPitchNoCollide.sort(Comparator.comparing(toSort -> {
               float diffYaw = RotationUtil.getAngleDifference(prevRot[0], toSort.x);
               float diffPitch = Math.abs(prevRot[1] - toSort.y);
               return Math.abs(diffYaw) + Math.abs(diffPitch) * 0.8F;
            }));
         }

         return new float[]{allYawPitchNoCollide.get(0).x, allYawPitchNoCollide.get(0).y};
      }
   }

   private class PenisController {
      private final EntityPlayer player;
      private final Vec3d mainPos;
      private final Vec3d secondPos;
      private final Vec3d prevMainPos;
      private final Vec3d prevSecondPos;
      private double length;
      private double lengthRender;
      private double prevLengthRender;
      private float penisYaw;
      private float penisPitch;

      public PenisController(EntityPlayer player, double length) {
         this.player = player;
         this.length = length;
         this.lengthRender = length;
         this.prevLengthRender = length;
         float[] defaultPenisRot = this.calcPhysicsPenisDirectionRot();
         defaultPenisRot[0] = MathUtils.wrapAngleTo180_float(defaultPenisRot[0]);
         this.mainPos = this.findBestPenisMainPos(defaultPenisRot);
         this.secondPos = this.mainPos
            .addVector(
               (double)(-MathHelper.sin(MathHelper.toRadians(defaultPenisRot[0])) * MathHelper.cos(MathHelper.toRadians(defaultPenisRot[1]))) * length,
               (double)(-MathHelper.sin(MathHelper.toRadians(defaultPenisRot[1]))),
               (double)(MathHelper.cos(MathHelper.toRadians(defaultPenisRot[0])) * MathHelper.cos(MathHelper.toRadians(defaultPenisRot[1]))) * length
            );
         this.prevMainPos = new Vec3d(this.mainPos.xCoord, this.mainPos.yCoord, this.mainPos.zCoord);
         this.prevSecondPos = new Vec3d(this.secondPos.xCoord, this.secondPos.yCoord, this.secondPos.zCoord);
      }

      public void setLength(double length) {
         this.length = length;
      }

      private Vec3d findBestPenisMainPos(float[] rot) {
         double xzExt = (double)(this.player.width / 2.0F);
         double yLevel = (double)(this.player.height / 2.3F);
         if (this.player.isSneaking()) {
            yLevel -= 0.2;
            xzExt -= 0.1F;
         } else if (this.player.isLay || this.player.getFlag(7)) {
            yLevel = -0.2F;
            xzExt *= 2.8F;
         }

         return this.player
            .getPositionVector()
            .addVector((double)(-MathHelper.sin(MathHelper.toRadians(rot[0]))) * xzExt, yLevel, (double)MathHelper.cos(MathHelper.toRadians(rot[0])) * xzExt);
      }

      private float[] calcPhysicsPenisDirectionRot() {
         float moveYaw = 0.0F;
         boolean targetting = Penis.this.HitAuraBonus.getBool() && HitAura.TARGET != null && this.player instanceof EntityPlayerSP;
         float yaw = this.player.isLay || this.player.getFlag(7)
            ? this.player.rotationYaw
            : (
               targetting
                  ? MathUtils.wrapDegrees(RotationUtil.getYawToEntity(HitAura.TARGET_ROTS))
                  : (
                     Module.mc.gameSettings.thirdPersonView == 0
                        ? this.player.rotationYaw
                           + MathUtils.clamp((moveYaw = MoveMeHelp.moveYaw(0.0F)) <= 45.0F && moveYaw >= -45.0F ? moveYaw : 0.0F, -30.0F, 30.0F)
                        : this.player.renderYawOffset
                  )
            );
         float pitch = Math.min(this.player.rotationPitch / 1.333F, 60.0F);
         double mY = this.player.posY - this.player.prevPosY;
         pitch += targetting
            ? MathUtils.lerp(
               20.0F,
               -50.0F,
               HitAura.get.isOldCooldown()
                  ? MathUtils.valWave01((float)(this.player.ticksExisted % 4) / 4.0F)
                  : (float)(1.0 - MathUtils.easeInOutQuadWave((double)Math.min((float)HitAura.cooldown.getTime() / HitAura.get.msCooldown() * 2.0F, 1.0F)))
            )
            : (float)MathUtils.clamp(
               MathUtils.easeInOutQuad(MathUtils.clamp(Math.abs(mY) / (double)this.player.getJumpUpwardsMotion(), -1.0, 1.0))
                  * 5.0
                  * (double)(this.player.motionY > 0.0 ? 20.0F : -20.0F),
               -40.0,
               70.0
            );
         if (targetting) {
            this.player.renderYawOffset = yaw;
            this.player.rotationYawHead = yaw;
         }

         return new float[]{yaw, pitch};
      }

      private double getComputedRenderLength(double length, Vec3d start, Vec3d end, float speedRegen, double minLength, double maxLength) {
         double currentLength = MathUtils.lerp(length, maxLength, (double)speedRegen);
         if (Module.mc.world != null) {
            RayTraceResult ray = Module.mc.world.rayTraceBlocks(start, end, false, true, true);
            if (ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK && ray.hitVec != null) {
               currentLength = MathUtils.lerp(this.lengthRender, start.distanceTo(ray.hitVec), (double)speedRegen);
            }
         }

         return Math.min(Math.max(currentLength, minLength), maxLength);
      }

      public void update() {
         this.prevMainPos.xCoord = this.mainPos.xCoord;
         this.prevMainPos.yCoord = this.mainPos.yCoord;
         this.prevMainPos.zCoord = this.mainPos.zCoord;
         this.prevSecondPos.xCoord = this.secondPos.xCoord;
         this.prevSecondPos.yCoord = this.secondPos.yCoord;
         this.prevSecondPos.zCoord = this.secondPos.zCoord;
         this.prevLengthRender = this.lengthRender;
         float[] defaultPenisRot = this.calcPhysicsPenisDirectionRot();
         Vec3d mainPenisPosFounded = this.findBestPenisMainPos(defaultPenisRot);
         this.mainPos.xCoord = mainPenisPosFounded.xCoord;
         this.mainPos.yCoord = mainPenisPosFounded.yCoord;
         this.mainPos.zCoord = mainPenisPosFounded.zCoord;
         float yawStep = 45.0F;
         float pitchStep = 20.0F;
         float pitchClampingExt = pitchStep * 2.0F;
         int scanYawCount = 90;
         int scanPitchCount = 26;
         float scanYawFactor = (float)scanYawCount / 360.0F;
         float scanPitchFactor = (float)scanPitchCount / 360.0F;
         defaultPenisRot[0] = (float)((int)(defaultPenisRot[0] / scanYawFactor)) * scanYawFactor;
         defaultPenisRot[1] = (float)((int)(defaultPenisRot[1] / scanPitchFactor)) * scanPitchFactor;
         float[] newRotatePenis = Penis.this.updateDirsFromStartACollide(
            defaultPenisRot,
            this.mainPos,
            this.length,
            scanYawCount,
            scanPitchCount,
            this.penisYaw - yawStep,
            this.penisYaw + yawStep,
            this.penisPitch - pitchClampingExt,
            this.penisPitch + pitchClampingExt
         );
         this.penisYaw = defaultPenisRot[0] + MathUtils.wrapAngleTo180_float(this.penisYaw - defaultPenisRot[0]);
         newRotatePenis[0] = defaultPenisRot[0] + MathUtils.wrapAngleTo180_float(newRotatePenis[0] - defaultPenisRot[0]);
         this.penisYaw = MathUtils.harp(this.penisYaw, newRotatePenis[0], 0.95F);
         this.penisPitch = MathUtils.harp(this.penisPitch, newRotatePenis[1], 0.95F);
         float penisYawRad = MathHelper.toRadians(this.penisYaw);
         float penisPitchRad = MathHelper.toRadians(this.penisPitch);
         this.secondPos.xCoord = this.mainPos.xCoord + (double)(-MathHelper.sin(penisYawRad) * MathHelper.cos(penisPitchRad)) * this.length;
         this.secondPos.yCoord = this.mainPos.yCoord + (double)MathHelper.sin(penisPitchRad) * -this.length;
         this.secondPos.zCoord = this.mainPos.zCoord + (double)(MathHelper.cos(penisYawRad) * MathHelper.cos(penisPitchRad)) * this.length;
         this.lengthRender = this.getComputedRenderLength(this.lengthRender, this.mainPos, this.secondPos, 0.5F, this.length / 4.0, this.length);
      }

      public double getRenderLength(float partialTicks) {
         return MathUtils.lerp(this.prevLengthRender, this.lengthRender, (double)partialTicks);
      }

      public Vec3d[] getTwoRenderVecs(float partialTicks, boolean isRenderLength) {
         if (isRenderLength) {
            double deltaXYZ = this.getRenderLength(partialTicks) / this.length;
            return new Vec3d[]{
               new Vec3d(
                  MathUtils.lerp(this.prevMainPos.xCoord, this.mainPos.xCoord, (double)partialTicks),
                  MathUtils.lerp(this.prevMainPos.yCoord, this.mainPos.yCoord, (double)partialTicks),
                  MathUtils.lerp(this.prevMainPos.zCoord, this.mainPos.zCoord, (double)partialTicks)
               ),
               new Vec3d(
                  MathUtils.lerp(
                     MathUtils.lerp(this.prevMainPos.xCoord, this.mainPos.xCoord, (double)partialTicks),
                     MathUtils.lerp(this.prevSecondPos.xCoord, this.secondPos.xCoord, (double)partialTicks),
                     deltaXYZ
                  ),
                  MathUtils.lerp(
                     MathUtils.lerp(this.prevMainPos.yCoord, this.mainPos.yCoord, (double)partialTicks),
                     MathUtils.lerp(this.prevSecondPos.yCoord, this.secondPos.yCoord, (double)partialTicks),
                     deltaXYZ
                  ),
                  MathUtils.lerp(
                     MathUtils.lerp(this.prevMainPos.zCoord, this.mainPos.zCoord, (double)partialTicks),
                     MathUtils.lerp(this.prevSecondPos.zCoord, this.secondPos.zCoord, (double)partialTicks),
                     deltaXYZ
                  )
               )
            };
         } else {
            return new Vec3d[]{
               new Vec3d(
                  MathUtils.lerp(this.prevMainPos.xCoord, this.mainPos.xCoord, (double)partialTicks),
                  MathUtils.lerp(this.prevMainPos.yCoord, this.mainPos.yCoord, (double)partialTicks),
                  MathUtils.lerp(this.prevMainPos.zCoord, this.mainPos.zCoord, (double)partialTicks)
               ),
               new Vec3d(
                  MathUtils.lerp(this.prevSecondPos.xCoord, this.secondPos.xCoord, (double)partialTicks),
                  MathUtils.lerp(this.prevSecondPos.yCoord, this.secondPos.yCoord, (double)partialTicks),
                  MathUtils.lerp(this.prevSecondPos.zCoord, this.secondPos.zCoord, (double)partialTicks)
               )
            };
         }
      }
   }
}
