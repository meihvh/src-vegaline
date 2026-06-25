package net.minecraft.cape.layer;

import net.minecraft.cape.sim.StickSimulation;
import net.minecraft.cape.util.Matrix4f;
import net.minecraft.cape.util.Mth;
import net.minecraft.cape.util.PoseStack;
import net.minecraft.cape.util.Vector3f;
import net.minecraft.cape.util.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemAir;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import ru.govno.client.module.modules.ClientColors;
import ru.govno.client.module.modules.NoRender;
import ru.govno.client.module.modules.WallHack;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.ColorUtils;

public class SmoothCapeRenderer {
   public void renderSmoothCape(CustomCapeRenderLayer layer, AbstractClientPlayer abstractClientPlayer, float delta) {
      BufferBuilder worldrenderer = Tessellator.getInstance().getBuffer();
      worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      PoseStack poseStack = new PoseStack();
      poseStack.pushPose();
      Matrix4f oldPositionMatrix = null;
      int color1 = -1;
      int color2 = color1;
      float zW = 0.03F;

      for (int part = 0; part < CustomCapeRenderLayer.partCount; part++) {
         if (ClientColors.get != null) {
            color1 = ClientColors.getColor1(9000 - (int)((float)part / (float)CustomCapeRenderLayer.partCount * 180.0F * 9.0F));
            color2 = ClientColors.getColor1(9000 - (int)((float)(part + 1) / (float)CustomCapeRenderLayer.partCount * 180.0F * 9.0F));
         }

         this.modifyPoseStack(layer, poseStack, abstractClientPlayer, delta, part);
         if (oldPositionMatrix == null) {
            oldPositionMatrix = poseStack.last().pose();
         }

         if (part == 0) {
            addTopVertex(worldrenderer, poseStack.last().pose(), oldPositionMatrix, 0.3F, 0.0F, 0.0F, -0.3F, 0.0F, -zW, part, color1, color2);
         }

         if (part == CustomCapeRenderLayer.partCount - 1) {
            addBottomVertex(
               worldrenderer,
               poseStack.last().pose(),
               poseStack.last().pose(),
               0.3F,
               (float)(part + 1) * (0.96F / (float)CustomCapeRenderLayer.partCount),
               0.0F,
               -0.3F,
               (float)(part + 1) * (0.96F / (float)CustomCapeRenderLayer.partCount),
               -zW,
               part,
               color1,
               color2
            );
         }

         addLeftVertex(
            worldrenderer,
            poseStack.last().pose(),
            oldPositionMatrix,
            -0.3F,
            (float)(part + 1) * (0.96F / (float)CustomCapeRenderLayer.partCount),
            0.0F,
            -0.3F,
            (float)part * (0.96F / (float)CustomCapeRenderLayer.partCount),
            -zW,
            part,
            color1,
            color2
         );
         addRightVertex(
            worldrenderer,
            poseStack.last().pose(),
            oldPositionMatrix,
            0.3F,
            (float)(part + 1) * (0.96F / (float)CustomCapeRenderLayer.partCount),
            0.0F,
            0.3F,
            (float)part * (0.96F / (float)CustomCapeRenderLayer.partCount),
            -zW,
            part,
            color1,
            color2
         );
         addFrontVertex(
            worldrenderer,
            oldPositionMatrix,
            poseStack.last().pose(),
            0.3F,
            (float)(part + 1) * (0.96F / (float)CustomCapeRenderLayer.partCount),
            0.0F,
            -0.3F,
            (float)part * (0.96F / (float)CustomCapeRenderLayer.partCount),
            0.0F,
            part,
            color1,
            color2
         );
         addBackVertex(
            worldrenderer,
            poseStack.last().pose(),
            oldPositionMatrix,
            0.3F,
            (float)(part + 1) * (0.96F / (float)CustomCapeRenderLayer.partCount),
            -zW,
            -0.3F,
            (float)part * (0.96F / (float)CustomCapeRenderLayer.partCount),
            -zW,
            part,
            color1,
            color2
         );
         oldPositionMatrix = poseStack.last().pose();
         poseStack.popPose();
      }

      if (abstractClientPlayer.isChild()) {
         GlStateManager.scale(0.5, 0.5, 0.4);
         GlStateManager.translate(0.0, abstractClientPlayer.isSneaking() ? 1.375 : 1.525, 0.05F);
      }

      GL11.glDisable(2896);
      Runnable draw = () -> {
         GL11.glShadeModel(7425);
         GL11.glDepthMask(false);
         Tessellator.getInstance().vboUploader.draw(Tessellator.getInstance().worldRenderer);
         GL11.glBlendFunc(776, 769);
         Tessellator.getInstance().vboUploader.draw(Tessellator.getInstance().worldRenderer);
         GL11.glBlendFunc(770, 771);
         GL11.glShadeModel(7424);
      };
      if (WallHack.get.actived) {
         WallHack.get.preRenderLivingBase(abstractClientPlayer, draw, false);
      }

      draw.run();
      if (WallHack.get.actived) {
         WallHack.get.postRenderLivingBase(abstractClientPlayer, draw, false);
      }

      Tessellator.getInstance().worldRenderer.finishDrawing();
      GL11.glDepthMask(true);
      GL11.glEnable(2896);
   }

   void modifyPoseStack(CustomCapeRenderLayer layer, PoseStack poseStack, AbstractClientPlayer abstractClientPlayer, float h, int part) {
      this.modifyPoseStackVanilla(layer, poseStack, abstractClientPlayer, h, part);
   }

   private void modifyPoseStackSimulation(CustomCapeRenderLayer layer, PoseStack poseStack, AbstractClientPlayer abstractClientPlayer, float delta, int part) {
      Minecraft mc = Minecraft.getMinecraft();
      StickSimulation simulation = abstractClientPlayer.getSimulation();
      poseStack.pushPose();
      poseStack.translate(0.0, 0.0, 0.1);
      double d = Mth.lerp((double)delta, abstractClientPlayer.prevChasingPosX, abstractClientPlayer.chasingPosX)
         - Mth.lerp((double)delta, abstractClientPlayer.prevPosX, abstractClientPlayer.posX);
      double e = Mth.lerp((double)delta, abstractClientPlayer.prevChasingPosY, abstractClientPlayer.chasingPosY)
         - Mth.lerp((double)delta, abstractClientPlayer.prevPosY, abstractClientPlayer.posY);
      double m = Mth.lerp((double)delta, abstractClientPlayer.prevChasingPosZ, abstractClientPlayer.chasingPosZ)
         - Mth.lerp((double)delta, abstractClientPlayer.prevPosZ, abstractClientPlayer.posZ);
      float z = (float)(
         (double)((simulation.points.get(part).getLerpX(delta) - simulation.points.get(0).getLerpX(delta)) * 1.1F)
            / (abstractClientPlayer.isChild() ? 1.0 : 1.5)
      );
      float swing = (float)(Math.abs(d) + Math.abs(m)) * ((float)part / (float)(CustomCapeRenderLayer.partCount * 2)) * 180.0F;
      if (swing > 435.0F) {
         swing = 435.0F;
      }

      if (z > 0.0F) {
         z = 0.0F;
      }

      float y = (float)(
            (double)(simulation.points.get(0).getLerpY(delta) - (float)part - simulation.points.get(part).getLerpY(delta))
               / (abstractClientPlayer.isChild() ? 1.0 : 1.5)
         )
         / 5.0F;
      float partRotation = (float)(-Math.atan2((double)y, (double)z));
      partRotation = Math.max(partRotation, 0.0F);
      if (partRotation != 0.0F) {
         partRotation = (float)(Math.PI - (double)partRotation);
      }

      int nst = 2500;
      float ttn = (float)((System.currentTimeMillis() - (long)((int)((float)(part * nst) / (float)CustomCapeRenderLayer.partCount))) % (long)nst);
      ttn = (float)MathUtils.easeInOutQuadWave((double)(ttn / (float)nst)) * (float)nst;
      float partPC = (float)part / (float)CustomCapeRenderLayer.partCount;
      swing = (float)(
         (double)swing
            + (double)(ttn * 0.75F)
               * MathUtils.easeInOutQuadWave((double)partPC)
               * (double)part
               / (double)((float)nst)
               * (1.0 - MathUtils.clamp(Math.sqrt(d * d + m * m) * 10.0, 0.0, 1.0))
      );
      float height = -partPC * 10.0F;
      if (abstractClientPlayer.isSneaking()) {
         if (!abstractClientPlayer.isLay) {
            height += 25.0F;
         }

         poseStack.translate(0.0, 0.15F, 0.0);
      }

      poseStack.scale(0.875F, 1.0F, 0.875F);
      poseStack.translate(
         0.0,
         !(abstractClientPlayer.inventory.armorInventory.get(2).getItem() instanceof ItemAir) && (!NoRender.get.actived || !NoRender.get.ArmorLayers.getBool())
            ? -0.05F
            : 0.005F,
         abstractClientPlayer.inventory.armorInventory.get(2).getItem() instanceof ItemAir ? 0.01F : 0.075F
      );
      swing = MathUtils.clamp(swing / 10.0F, 0.0F, 10000.0F);
      poseStack.mulPose(Vector3f.XP.rotationDegrees(16.0F + height + swing));
      poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F));
      poseStack.translate(
         0.0,
         (double)(y / (float)CustomCapeRenderLayer.partCount),
         (double)(z / (float)CustomCapeRenderLayer.partCount - swing / 1000.0F)
            + (double)(y * partPC) * -Math.sqrt(d * d + m * m) / (double)CustomCapeRenderLayer.partCount
      );
      float offset = 0.0F;
      poseStack.translate(0.0, (double)(-offset * 3.0F) + 0.03, -0.03);
      poseStack.translate(0.0, (double)(part / CustomCapeRenderLayer.partCount), 0.0);
      poseStack.mulPose(Vector3f.XP.rotationDegrees(-partRotation));
      poseStack.mulPose(
         Vector3f.ZN
            .rotationDegrees(
               partPC
                  * simulation.points.get(part).getLerpX(delta)
                  * (
                     (
                           MathUtils.lerp(abstractClientPlayer.prevRenderYawOffset, abstractClientPlayer.renderYawOffset, delta)
                              - MathUtils.lerp(abstractClientPlayer.prevRotationYaw, abstractClientPlayer.rotationYaw, delta)
                        )
                        / 360.0F
                        / 90.0F
                  )
            )
      );
      poseStack.translate(0.0, (double)(-part / CustomCapeRenderLayer.partCount), 0.0);
      poseStack.translate(0.0, -0.03, 0.03);
   }

   public static StickSimulation.Vector2 getOverallVector2(StickSimulation.Vector2 first, StickSimulation.Vector2 second, float pc) {
      double x1 = (double)first.x;
      double y1 = (double)first.y;
      double x2 = (double)second.x;
      double y2 = (double)second.y;
      double diffX = x2 - x1;
      double diffY = y2 - y1;
      return new StickSimulation.Vector2((float)(x1 + diffX * (double)pc), (float)(y1 + diffY * (double)pc));
   }

   private float getRotation(float delta, int part, StickSimulation simulation) {
      return part == CustomCapeRenderLayer.partCount - 1
         ? this.getRotation(delta, part - 1, simulation)
         : (float)this.getAngle(
            getOverallVector2(simulation.points.get(part).prevPosition, simulation.points.get(part).position, delta),
            getOverallVector2(simulation.points.get(part + 1).prevPosition, simulation.points.get(part + 1).position, delta)
         );
   }

   private double getAngle(StickSimulation.Vector2 a, StickSimulation.Vector2 b) {
      StickSimulation.Vector2 angle = b.subtract(a);
      return Math.toDegrees(Math.atan2((double)angle.x, (double)angle.y)) + 180.0;
   }

   private float getNatrualWindSwing(int part, boolean underwater) {
      long highlightedPart = System.currentTimeMillis() / (long)(underwater ? 9 : 3) % 360L;
      float PART_COUNT = (float)CustomCapeRenderLayer.partCount;
      float relativePart = (float)(part + 1) / PART_COUNT;
      return MathHelper.sin(MathHelper.toRadians(relativePart * 360.0F - (float)highlightedPart)) * 3.0F;
   }

   private void modifyPoseStackVanilla(CustomCapeRenderLayer layer, PoseStack poseStack, AbstractClientPlayer abstractClientPlayer, float h, int part) {
      StickSimulation simulation = abstractClientPlayer.getSimulation();
      poseStack.pushPose();
      poseStack.translate(0.0, 0.0, 0.125);
      float PART_COUNT = (float)CustomCapeRenderLayer.partCount;
      float x = (simulation.points.get(0).getLerpX(h) - simulation.points.get(part).getLerpX(h)) * easeOutSine((float)part / PART_COUNT);
      float y = simulation.points.get(0).getLerpY(h) - (float)part + x / 10.0F - simulation.points.get(part).getLerpY(h);
      float sidewaysRotationOffset = 0.0F;
      float partRotation = this.getRotation(h, part, simulation);
      boolean child = abstractClientPlayer.isChild();
      if (child) {
         poseStack.scale(0.825F, 0.825F, 0.825F);
      }

      float height = (float)(-part) / PART_COUNT;
      if (abstractClientPlayer.isSneaking()) {
         height += child ? 30.0F : 25.0F;
         if (child) {
            poseStack.translate(0.0, 0.0825F, 0.0);
         } else {
            poseStack.translate(0.0, 0.1F, -0.0225F);
         }
      }

      double dx = abstractClientPlayer.prevChasingPosX - abstractClientPlayer.chasingPosX;
      double dy = abstractClientPlayer.prevChasingPosY - abstractClientPlayer.chasingPosY;
      double dz = abstractClientPlayer.prevChasingPosZ - abstractClientPlayer.chasingPosZ;
      double speed = Math.sqrt(dx * dx + dz * dz) / 2.0 + Math.sqrt(dy * dy) * 1.5 * MathUtils.clamp(Math.sqrt(dx * dx + dz * dz) * 2.0, 0.0, 1.0);
      if (speed < 0.0) {
         speed = 0.0;
      }

      simulation.moveSpeed.to = (float)speed;
      speed = (double)simulation.moveSpeed.getAnim();
      simulation.moveSpeed.speed = 0.02F;
      if (speed > 1.0) {
         speed = 1.0;
      }

      float naturalWindSwing = this.getNatrualWindSwing(part, abstractClientPlayer.isInWater()) / 3.0F * (1.0F - (float)speed) + (float)speed * 75.0F;
      if (child) {
         poseStack.translate(
            0.0,
            !(abstractClientPlayer.inventory.armorInventory.get(2).getItem() instanceof ItemAir)
                  && (!NoRender.get.actived || !NoRender.get.ArmorLayers.getBool())
               ? -0.0125F
               : -0.025F,
            !(abstractClientPlayer.inventory.armorInventory.get(2).getItem() instanceof ItemAir)
                  && (!NoRender.get.actived || !NoRender.get.ArmorLayers.getBool())
               ? 0.06F
               : -0.04F
         );
      } else {
         poseStack.translate(
            0.0,
            !(abstractClientPlayer.inventory.armorInventory.get(2).getItem() instanceof ItemAir)
                  && (!NoRender.get.actived || !NoRender.get.ArmorLayers.getBool())
               ? 0.0125F
               : 0.0,
            !(abstractClientPlayer.inventory.armorInventory.get(2).getItem() instanceof ItemAir)
                  && (!NoRender.get.actived || !NoRender.get.ArmorLayers.getBool())
               ? 0.062F
               : 0.01F
         );
      }

      poseStack.mulPose(Vector3f.XP.rotationDegrees(6.0F + height + naturalWindSwing));
      poseStack.mulPose(Vector3f.ZP.rotationDegrees(sidewaysRotationOffset / 2.0F));
      poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - sidewaysRotationOffset / 2.0F));
      poseStack.translate(0.0, (double)(y / PART_COUNT), (double)(-x / PART_COUNT));
      poseStack.translate(0.0, 0.03, -0.03);
      poseStack.translate(0.0, (double)((float)part * 1.0F / PART_COUNT), (double)(0.0F / PART_COUNT));
      poseStack.mulPose(Vector3f.XP.rotationDegrees(-partRotation));
      poseStack.translate(0.0, (double)((float)(-part) * 1.0F / PART_COUNT), (double)(0.0F / PART_COUNT));
      poseStack.translate(0.0, -0.03, 0.03);
   }

   private static void addBackVertex(
      BufferBuilder worldrenderer,
      Matrix4f matrix,
      Matrix4f oldMatrix,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      int part,
      int color1,
      int color2
   ) {
      if (x1 < x2) {
         float i = x1;
         x1 = x2;
         x2 = i;
      }

      if (y1 < y2) {
         float i = y1;
         y1 = y2;
         y2 = i;
         Matrix4f k = matrix;
         matrix = oldMatrix;
         oldMatrix = k;
      }

      float minU = 0.015625F;
      float maxU = 0.171875F;
      float minV = 0.03125F;
      float maxV = 0.53125F;
      float deltaV = maxV - minV;
      float vPerPart = deltaV / (float)CustomCapeRenderLayer.partCount;
      maxV = minV + vPerPart * (float)(part + 1);
      minV += vPerPart * (float)part;
      vertex(worldrenderer, oldMatrix, x1, y2, z1).tex((double)maxU, (double)minV).color(color1).endVertex();
      vertex(worldrenderer, oldMatrix, x2, y2, z1).tex((double)minU, (double)minV).color(color1).endVertex();
      vertex(worldrenderer, matrix, x2, y1, z2).tex((double)minU, (double)maxV).color(color2).endVertex();
      vertex(worldrenderer, matrix, x1, y1, z2).tex((double)maxU, (double)maxV).color(color2).endVertex();
   }

   private static void addFrontVertex(
      BufferBuilder worldrenderer,
      Matrix4f matrix,
      Matrix4f oldMatrix,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      int part,
      int color1,
      int color2
   ) {
      if (x1 < x2) {
         float i = x1;
         x1 = x2;
         x2 = i;
      }

      if (y1 < y2) {
         float i = y1;
         y1 = y2;
         y2 = i;
         Matrix4f k = matrix;
         matrix = oldMatrix;
         oldMatrix = k;
      }

      float minU = 0.1875F;
      float maxU = 0.34375F;
      float minV = 0.03125F;
      float maxV = 0.53125F;
      float deltaV = maxV - minV;
      float vPerPart = deltaV / (float)CustomCapeRenderLayer.partCount;
      maxV = minV + vPerPart * (float)(part + 1);
      minV += vPerPart * (float)part;
      vertex(worldrenderer, oldMatrix, x1, y1, z1).tex((double)maxU, (double)maxV).color(color2).endVertex();
      vertex(worldrenderer, oldMatrix, x2, y1, z1).tex((double)minU, (double)maxV).color(color2).endVertex();
      vertex(worldrenderer, matrix, x2, y2, z2).tex((double)minU, (double)minV).color(color1).endVertex();
      vertex(worldrenderer, matrix, x1, y2, z2).tex((double)maxU, (double)minV).color(color1).endVertex();
   }

   private static void addLeftVertex(
      BufferBuilder worldrenderer,
      Matrix4f matrix,
      Matrix4f oldMatrix,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      int part,
      int color1,
      int color2
   ) {
      if (x1 < x2) {
         x2 = x1;
      }

      if (y1 < y2) {
         float i = y1;
         y1 = y2;
         y2 = i;
      }

      float minU = 0.0F;
      float maxU = 0.015625F;
      float minV = 0.03125F;
      float maxV = 0.53125F;
      float deltaV = maxV - minV;
      float vPerPart = deltaV / (float)CustomCapeRenderLayer.partCount;
      maxV = minV + vPerPart * (float)(part + 1);
      minV += vPerPart * (float)part;
      vertex(worldrenderer, matrix, x2, y1, z1)
         .tex((double)maxU, (double)maxV)
         .color(ColorUtils.swapAlpha(0, (float)ColorUtils.getAlphaFromColor(color2)))
         .endVertex();
      vertex(worldrenderer, matrix, x2, y1, z2).tex((double)minU, (double)maxV).color(color2).endVertex();
      vertex(worldrenderer, oldMatrix, x2, y2, z2).tex((double)minU, (double)minV).color(color1).endVertex();
      vertex(worldrenderer, oldMatrix, x2, y2, z1)
         .tex((double)maxU, (double)minV)
         .color(ColorUtils.swapAlpha(0, (float)ColorUtils.getAlphaFromColor(color1)))
         .endVertex();
   }

   private static void addRightVertex(
      BufferBuilder worldrenderer,
      Matrix4f matrix,
      Matrix4f oldMatrix,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      int part,
      int color1,
      int color2
   ) {
      if (x1 < x2) {
         x2 = x1;
      }

      if (y1 < y2) {
         float i = y1;
         y1 = y2;
         y2 = i;
      }

      float minU = 0.171875F;
      float maxU = 0.1875F;
      float minV = 0.03125F;
      float maxV = 0.53125F;
      float deltaV = maxV - minV;
      float vPerPart = deltaV / (float)CustomCapeRenderLayer.partCount;
      maxV = minV + vPerPart * (float)(part + 1);
      minV += vPerPart * (float)part;
      vertex(worldrenderer, matrix, x2, y1, z2).tex((double)minU, (double)maxV).color(color2).endVertex();
      vertex(worldrenderer, matrix, x2, y1, z1)
         .tex((double)maxU, (double)maxV)
         .color(ColorUtils.swapAlpha(0, (float)ColorUtils.getAlphaFromColor(color2)))
         .endVertex();
      vertex(worldrenderer, oldMatrix, x2, y2, z1)
         .tex((double)maxU, (double)minV)
         .color(ColorUtils.swapAlpha(0, (float)ColorUtils.getAlphaFromColor(color1)))
         .endVertex();
      vertex(worldrenderer, oldMatrix, x2, y2, z2).tex((double)minU, (double)minV).color(color1).endVertex();
   }

   private static void addBottomVertex(
      BufferBuilder worldrenderer,
      Matrix4f matrix,
      Matrix4f oldMatrix,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      int part,
      int color1,
      int color2
   ) {
      if (x1 < x2) {
         float i = x1;
         x1 = x2;
         x2 = i;
      }

      if (y1 < y2) {
         float i = y1;
         y1 = y2;
         y2 = i;
      }

      float minU = 0.171875F;
      float maxU = 0.328125F;
      float minV = 0.0F;
      float maxV = 0.03125F;
      float deltaV = maxV - minV;
      float vPerPart = deltaV / (float)CustomCapeRenderLayer.partCount;
      maxV = minV + vPerPart * (float)(part + 1);
      minV += vPerPart * (float)part;
      vertex(worldrenderer, oldMatrix, x1, y2, z2).tex((double)maxU, (double)minV).color(color2).endVertex();
      vertex(worldrenderer, oldMatrix, x2, y2, z2).tex((double)minU, (double)minV).color(color2).endVertex();
      vertex(worldrenderer, matrix, x2, y1, z1)
         .tex((double)minU, (double)maxV)
         .color(ColorUtils.swapAlpha(0, (float)ColorUtils.getAlphaFromColor(color1)))
         .endVertex();
      vertex(worldrenderer, matrix, x1, y1, z1)
         .tex((double)maxU, (double)maxV)
         .color(ColorUtils.swapAlpha(0, (float)ColorUtils.getAlphaFromColor(color1)))
         .endVertex();
   }

   private static BufferBuilder vertex(BufferBuilder worldrenderer, Matrix4f matrix4f, float f, float g, float h) {
      Vector4f vector4f = new Vector4f(f, g, h, 1.0F);
      vector4f.transform(matrix4f);
      worldrenderer.pos((double)vector4f.x(), (double)vector4f.y(), (double)vector4f.z());
      return worldrenderer;
   }

   private static void addTopVertex(
      BufferBuilder worldrenderer,
      Matrix4f matrix,
      Matrix4f oldMatrix,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      int part,
      int color1,
      int color2
   ) {
      if (x1 < x2) {
         float i = x1;
         x1 = x2;
         x2 = i;
      }

      if (y1 < y2) {
         float i = y1;
         y1 = y2;
         y2 = i;
      }

      float minU = 0.015625F;
      float maxU = 0.171875F;
      float minV = 0.0F;
      float maxV = 0.03125F;
      float deltaV = maxV - minV;
      float vPerPart = deltaV / (float)CustomCapeRenderLayer.partCount;
      maxV = minV + vPerPart * (float)(part + 1);
      minV += vPerPart * (float)part;
      vertex(worldrenderer, oldMatrix, x1, y2, z1)
         .tex((double)maxU, (double)maxV)
         .color(ColorUtils.swapAlpha(0, (float)ColorUtils.getAlphaFromColor(color1)))
         .endVertex();
      vertex(worldrenderer, oldMatrix, x2, y2, z1)
         .tex((double)minU, (double)maxV)
         .color(ColorUtils.swapAlpha(0, (float)ColorUtils.getAlphaFromColor(color1)))
         .endVertex();
      vertex(worldrenderer, matrix, x2, y1, z2).tex((double)minU, (double)minV).color(color2).endVertex();
      vertex(worldrenderer, matrix, x1, y1, z2).tex((double)maxU, (double)minV).color(color2).endVertex();
   }

   private static float easeOutSine(float x) {
      return (float)Math.sin((double)x * Math.PI / 2.0);
   }
}
