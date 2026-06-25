package net.minecraft.cape.layer;

import net.minecraft.cape.sim.StickSimulation;
import net.minecraft.cape.util.Mth;
import net.minecraft.cape.util.PoseStack;
import net.minecraft.cape.util.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.module.modules.FreeCam;
import ru.govno.client.module.modules.NoRender;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;

public class CustomCapeRenderLayer implements LayerRenderer<AbstractClientPlayer> {
   static int partCount = 64;
   private final RenderPlayer playerRenderer;
   private ModelRenderer[] customCape = new ModelRenderer[partCount];
   private final SmoothCapeRenderer smoothCapeRenderer = new SmoothCapeRenderer();
   public static ResourceLocation[] capeFrames;

   public CustomCapeRenderLayer(RenderPlayer playerRenderer, ModelBase model) {
      this.playerRenderer = playerRenderer;
      this.buildMesh(model);
   }

   private static float easeOutSine(float x) {
      return (float)Math.sin((double)x * Math.PI / 2.0);
   }

   private void buildMesh(ModelBase model) {
      this.customCape = new ModelRenderer[partCount];

      for (int i = 0; i < partCount; i++) {
         ModelRenderer base = new ModelRenderer(model, 0, i);
         base.setTextureSize(64, 32);
         this.customCape[i] = base.addBox(-5.0F, (float)i, -1.0F, 10, 1, 1);
      }
   }

   ResourceLocation getCapeOfFrame(int usedFrame) {
      return capeFrames[usedFrame - 1];
   }

   private int indexByTime(float percentMs) {
      int min = 1;
      int max = 60;
      return (int)MathUtils.clamp((float)(max - min) * percentMs * (float)(1 + min / max), (float)min, (float)max);
   }

   public void doRenderLayer(
      AbstractClientPlayer abstractClientPlayer,
      float paramFloat1,
      float paramFloat2,
      float deltaTick,
      float animationTick,
      float paramFloat5,
      float paramFloat6,
      float paramFloat7
   ) {
      if ((!NoRender.get.actived || !NoRender.get.ClientCape.getBool()) && !RenderLivingBase.silentMode) {
         if (RenderLivingBase.unsetRenderCape) {
            RenderLivingBase.unsetRenderCape = false;
         } else {
            boolean render = abstractClientPlayer instanceof EntityPlayerSP
               || FreeCam.fakePlayer != null && abstractClientPlayer == FreeCam.fakePlayer
               || Client.friendManager.isFriend(abstractClientPlayer.getName());
            if (render) {
               ItemStack itemstack = abstractClientPlayer.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
               if (Panic.stop || itemstack.getItem() == Items.ELYTRA || abstractClientPlayer.isInvisible()) {
                  render = false;
               }

               if (render) {
                  abstractClientPlayer.updateSimulation(abstractClientPlayer, partCount);
                  int ms = 500;
                  long time = System.currentTimeMillis() % (long)ms;
                  float pcs = (float)time / (float)ms;
                  ResourceLocation finalCape = this.getCapeOfFrame(this.indexByTime(pcs));
                  if (finalCape == null) {
                     return;
                  }

                  this.playerRenderer.bindTexture(finalCape);
                  Minecraft.getMinecraft().entityRenderer.disableLightmap();
                  GL11.glEnable(3042);
                  GL11.glEnable(3553);
                  GL11.glDisable(2884);
                  GL11.glTexParameteri(3553, 10240, 9729);
                  this.smoothCapeRenderer.renderSmoothCape(this, abstractClientPlayer, deltaTick);
                  GL11.glTexParameteri(3553, 10240, 9728);
               }
            }
         }
      }
   }

   private void modifyPoseStack(AbstractClientPlayer abstractClientPlayer, float h, int part) {
      this.modifyPoseStackSimulation(abstractClientPlayer, h, part);
   }

   private void modifyPoseStackSimulation(AbstractClientPlayer abstractClientPlayer, float delta, int part) {
      StickSimulation simulation = abstractClientPlayer.getSimulation();
      GlStateManager.translate(0.0, 0.0, 0.125);
      float z = simulation.points.get(part).getLerpX(delta) - simulation.points.get(0).getLerpX(delta);
      if (z > 0.0F) {
         z = 0.0F;
      }

      float y = simulation.points.get(0).getLerpY(delta) - (float)part - simulation.points.get(part).getLerpY(delta);
      float sidewaysRotationOffset = 0.0F;
      float partRotation = (float)(-Math.atan2((double)y, (double)z));
      partRotation = Math.max(partRotation, 0.0F);
      if (partRotation != 0.0F) {
         partRotation = (float)(Math.PI - (double)partRotation);
      }

      partRotation = (float)((double)partRotation * 57.2958);
      float height = 0.0F;
      if (abstractClientPlayer.isSneaking()) {
         height += 25.0F;
         GlStateManager.translate(0.0F, 0.15F, 0.0F);
      }

      float naturalWindSwing = this.getNatrualWindSwing(part);
      GlStateManager.rotate(6.0F + height + naturalWindSwing, 1.0F, 0.0F, 0.0F);
      GlStateManager.rotate(sidewaysRotationOffset / 2.0F, 0.0F, 0.0F, 1.0F);
      GlStateManager.rotate(-sidewaysRotationOffset / 2.0F, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
      GlStateManager.translate(0.0F, y / (float)partCount, z / (float)partCount);
      GlStateManager.translate(0.0, 0.03, -0.03);
      GlStateManager.translate(0.0F, (float)part * 1.0F / (float)partCount, (float)(0 / partCount));
      GlStateManager.translate(0.0F, (float)(-part) * 1.0F / (float)partCount, (float)(0 / partCount));
      GlStateManager.translate(0.0, -0.03, 0.03);
   }

   void modifyPoseStackVanilla(AbstractClientPlayer abstractClientPlayer, float h, int part) {
      GlStateManager.translate(0.0, 0.0, 0.125);
      double d = Mth.lerp((double)h, abstractClientPlayer.prevChasingPosX, abstractClientPlayer.chasingPosX)
         - Mth.lerp((double)h, abstractClientPlayer.prevPosX, abstractClientPlayer.posX);
      double e = Mth.lerp((double)h, abstractClientPlayer.prevChasingPosY, abstractClientPlayer.chasingPosY)
         - Mth.lerp((double)h, abstractClientPlayer.prevPosY, abstractClientPlayer.posY);
      double m = Mth.lerp((double)h, abstractClientPlayer.prevChasingPosZ, abstractClientPlayer.chasingPosZ)
         - Mth.lerp((double)h, abstractClientPlayer.prevPosZ, abstractClientPlayer.posZ);
      float n = abstractClientPlayer.renderYawOffset;
      double o = Math.sin((double)(n * (float) (Math.PI / 180.0)));
      double p = -Math.cos((double)(n * (float) (Math.PI / 180.0)));
      float height = (float)e * 10.0F;
      height = MathHelper.clamp(height, -6.0F, 32.0F);
      float swing = (float)(d * o + m * p) * easeOutSine(1.0F / (float)partCount * (float)part) * 100.0F;
      swing = MathHelper.clamp(swing, 0.0F, 150.0F * easeOutSine(1.0F / (float)partCount * (float)part));
      float sidewaysRotationOffset = (float)(d * p - m * o) * 100.0F;
      sidewaysRotationOffset = MathHelper.clamp(sidewaysRotationOffset, -20.0F, 20.0F);
      float t = Mth.lerp(h, abstractClientPlayer.prevCameraYaw, abstractClientPlayer.cameraYaw);
      height = (float)(
         (double)height
            + Math.sin((double)(Mth.lerp(h, abstractClientPlayer.prevDistanceWalkedModified, abstractClientPlayer.distanceWalkedModified) * 6.0F))
               * 32.0
               * (double)t
      );
      if (abstractClientPlayer.isSneaking()) {
         height += 25.0F;
         GlStateManager.translate(0.0F, 0.15F, 0.0F);
      }

      float naturalWindSwing = this.getNatrualWindSwing(part);
      GlStateManager.rotate(6.0F + swing / 2.0F + height + naturalWindSwing, 1.0F, 0.0F, 0.0F);
      GlStateManager.rotate(sidewaysRotationOffset / 2.0F, 0.0F, 0.0F, 1.0F);
      GlStateManager.rotate(-sidewaysRotationOffset / 2.0F, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
   }

   float getNatrualWindSwing(int part) {
      return 0.0F;
   }

   @Override
   public boolean shouldCombineTextures() {
      return false;
   }

   void modifyPoseStack(CustomCapeRenderLayer layer, PoseStack poseStack, AbstractClientPlayer abstractClientPlayer, float h, int part) {
      this.modifyPoseStackVanilla(layer, poseStack, abstractClientPlayer, h, part);
      this.modifyPoseStackVanillaCmodel(layer, poseStack, abstractClientPlayer, h, part);
   }

   private void modifyPoseStackSimulation(CustomCapeRenderLayer layer, PoseStack poseStack, AbstractClientPlayer abstractClientPlayer, float delta, int part) {
      StickSimulation simulation = abstractClientPlayer.getSimulation();
      poseStack.pushPose();
      poseStack.translate(0.0, 0.0, 0.125);
      float z = simulation.points.get(part).getLerpX(delta) - simulation.points.get(0).getLerpX(delta);
      if (z > 0.0F) {
         z = 0.0F;
      }

      float y = simulation.points.get(0).getLerpY(delta) - (float)part - simulation.points.get(part).getLerpY(delta);
      float sidewaysRotationOffset = 0.0F;
      float partRotation = (float)(-Math.atan2((double)y, (double)z));
      partRotation = Math.max(partRotation, 0.0F);
      if (partRotation != 0.0F) {
         partRotation = (float)(Math.PI - (double)partRotation);
      }

      partRotation = (float)((double)partRotation * 57.2958);
      partRotation *= 2.0F;
      float height = 0.0F;
      if (abstractClientPlayer.isSneaking()) {
         height += 25.0F;
         poseStack.translate(0.0, 0.15F, 0.0);
      }

      float naturalWindSwing = layer.getNatrualWindSwing(part);
      poseStack.mulPose(Vector3f.XP.rotationDegrees(6.0F + height + naturalWindSwing));
      poseStack.mulPose(Vector3f.ZP.rotationDegrees(sidewaysRotationOffset / 2.0F));
      poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - sidewaysRotationOffset / 2.0F));
      poseStack.translate(0.0, (double)(y / (float)partCount), (double)(z / (float)partCount));
      poseStack.translate(0.0, 0.03, -0.03);
      poseStack.translate(0.0, (double)((float)part * 1.0F / (float)partCount), (double)(0 / partCount));
      poseStack.mulPose(Vector3f.XP.rotationDegrees(-partRotation));
      poseStack.translate(0.0, (double)((float)(-part) * 1.0F / (float)partCount), (double)(0 / partCount));
      poseStack.translate(0.0, -0.03, 0.03);
   }

   private void modifyPoseStackVanilla(CustomCapeRenderLayer layer, PoseStack poseStack, AbstractClientPlayer abstractClientPlayer, float h, int part) {
      poseStack.pushPose();
      poseStack.translate(0.0, 0.0, 0.125);
      double d = Mth.lerp((double)h, abstractClientPlayer.prevChasingPosX, abstractClientPlayer.chasingPosX)
         - Mth.lerp((double)h, abstractClientPlayer.prevPosX, abstractClientPlayer.posX);
      double e = Mth.lerp((double)h, abstractClientPlayer.prevChasingPosY, abstractClientPlayer.chasingPosY)
         - Mth.lerp((double)h, abstractClientPlayer.prevPosY, abstractClientPlayer.posY);
      double m = Mth.lerp((double)h, abstractClientPlayer.prevChasingPosZ, abstractClientPlayer.chasingPosZ)
         - Mth.lerp((double)h, abstractClientPlayer.prevPosZ, abstractClientPlayer.posZ);
      float n = abstractClientPlayer.prevRenderYawOffset + abstractClientPlayer.renderYawOffset - abstractClientPlayer.prevRenderYawOffset;
      double o = Math.sin((double)(n * (float) (Math.PI / 180.0)));
      double p = -Math.cos((double)(n * (float) (Math.PI / 180.0)));
      float height = (float)e * 10.0F;
      height = MathHelper.clamp(height, -6.0F, 32.0F);
      float swing = (float)(d * o + m * p) * easeOutSine(1.0F / (float)partCount * (float)part) * 100.0F;
      swing = MathHelper.clamp(swing, 0.0F, 150.0F * easeOutSine(1.0F / (float)partCount * (float)part));
      float sidewaysRotationOffset = (float)(d * p - m * o) * 100.0F;
      sidewaysRotationOffset = MathHelper.clamp(sidewaysRotationOffset, -20.0F, 20.0F);
      float t = Mth.lerp(h, abstractClientPlayer.prevCameraYaw, abstractClientPlayer.cameraYaw);
      height = (float)(
         (double)height
            + Math.sin((double)(Mth.lerp(h, abstractClientPlayer.prevDistanceWalkedModified, abstractClientPlayer.distanceWalkedModified) * 6.0F))
               * 32.0
               * (double)t
      );
      if (abstractClientPlayer.isSneaking()) {
         height += 25.0F;
         poseStack.translate(0.0, 0.15F, 0.0);
      }

      float naturalWindSwing = layer.getNatrualWindSwing(part);
      poseStack.mulPose(Vector3f.XP.rotationDegrees(6.0F + swing / 2.0F + height + naturalWindSwing));
      poseStack.mulPose(Vector3f.ZP.rotationDegrees(sidewaysRotationOffset / 2.0F));
      poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - sidewaysRotationOffset / 2.0F));
   }

   private void modifyPoseStackVanillaCmodel(CustomCapeRenderLayer layer, PoseStack poseStack, AbstractClientPlayer abstractClientPlayer, float h, int part) {
   }
}
