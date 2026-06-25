package net.minecraft.client.model;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;

public class ModelElytra extends ModelBase {
   private final ModelRenderer rightWing;
   private final ModelRenderer leftWing = new ModelRenderer(this, 22, 0);

   public ModelElytra() {
      this.leftWing.addBox(-10.0F, 0.0F, 0.0F, 10, 20, 2, 1.0F);
      this.rightWing = new ModelRenderer(this, 22, 0);
      this.rightWing.mirror = true;
      this.rightWing.addBox(0.0F, 0.0F, 0.0F, 10, 20, 2, 1.0F);
   }

   @Override
   public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
      GlStateManager.disableRescaleNormal();
      GlStateManager.disableCull();
      if (entityIn instanceof EntityLivingBase && ((EntityLivingBase)entityIn).isChild()) {
         GlStateManager.pushMatrix();
         GlStateManager.scale(0.5F, 0.5F, 0.5F);
         GlStateManager.translate(0.0F, 1.5F, -0.1F);
         this.leftWing.render(scale);
         this.rightWing.render(scale);
         GlStateManager.popMatrix();
      } else {
         this.leftWing.render(scale);
         this.rightWing.render(scale);
      }
   }

   @Override
   public void setRotationAngles(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn
   ) {
      super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
      float f = (float) (Math.PI / 12);
      float f1 = (float) (-Math.PI / 12);
      float f2 = 0.0F;
      float f3 = 0.0F;
      if (entityIn instanceof EntityLivingBase && ((EntityLivingBase)entityIn).isElytraFlying()) {
         float f4 = 1.0F;
         if (entityIn.motionY < 0.0) {
            Vec3d vec3d = new Vec3d(entityIn.motionX, entityIn.motionY, entityIn.motionZ).normalize();
            f4 = 1.0F - (float)Math.pow(-vec3d.yCoord, 1.5);
         }

         f = f4 * (float) (Math.PI / 9) + (1.0F - f4) * f;
         f1 = f4 * (float) (-Math.PI / 2) + (1.0F - f4) * f1;
      } else if (entityIn.isSneaking()) {
         f = (float) Math.PI * 2.0F / 9.0F;
         f1 = (float) (-Math.PI / 4);
         f2 = 3.0F;
         f3 = 0.08726646F;
      }

      this.leftWing.rotationPointX = 5.0F;
      this.leftWing.rotationPointY = f2;
      if (entityIn instanceof AbstractClientPlayer abstractclientplayer) {
         abstractclientplayer.rotateElytraX = (float)((double)abstractclientplayer.rotateElytraX + (double)(f - abstractclientplayer.rotateElytraX) * 0.1);
         abstractclientplayer.rotateElytraY = (float)((double)abstractclientplayer.rotateElytraY + (double)(f3 - abstractclientplayer.rotateElytraY) * 0.1);
         abstractclientplayer.rotateElytraZ = (float)((double)abstractclientplayer.rotateElytraZ + (double)(f1 - abstractclientplayer.rotateElytraZ) * 0.1);
         this.leftWing.rotateAngleX = abstractclientplayer.rotateElytraX;
         this.leftWing.rotateAngleY = abstractclientplayer.rotateElytraY;
         this.leftWing.rotateAngleZ = abstractclientplayer.rotateElytraZ;
      } else {
         this.leftWing.rotateAngleX = f;
         this.leftWing.rotateAngleZ = f1;
         this.leftWing.rotateAngleY = f3;
      }

      this.rightWing.rotationPointX = -this.leftWing.rotationPointX;
      this.rightWing.rotateAngleY = -this.leftWing.rotateAngleY;
      this.rightWing.rotationPointY = this.leftWing.rotationPointY;
      this.rightWing.rotateAngleX = this.leftWing.rotateAngleX;
      this.rightWing.rotateAngleZ = -this.leftWing.rotateAngleZ;
   }
}
