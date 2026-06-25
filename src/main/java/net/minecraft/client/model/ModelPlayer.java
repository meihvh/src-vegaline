package net.minecraft.client.model;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import ru.govno.client.utils.Command.impl.Panic;

public class ModelPlayer extends ModelBiped {
   public ModelRenderer bipedLeftArmwear;
   public ModelRenderer bipedRightArmwear;
   public ModelRenderer bipedLeftLegwear;
   public ModelRenderer bipedRightLegwear;
   public ModelRenderer bipedBodyWear;
   private final ModelRenderer bipedCape;
   private final boolean smallArms;

   public ModelPlayer(float modelSize, boolean smallArmsIn) {
      super(modelSize, 0.0F, 64, 64);
      this.smallArms = smallArmsIn;
      this.bipedCape = new ModelRenderer(this, 0, 0);
      this.bipedCape.setTextureSize(64, 32);
      this.bipedCape.addBox(-5.0F, 0.0F, -1.0F, 10, 16, 1, modelSize);
      if (smallArmsIn) {
         this.bipedLeftArm = new ModelRenderer(this, 32, 48);
         this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, modelSize);
         this.bipedLeftArm.setRotationPoint(5.0F, 2.5F, 0.0F);
         this.bipedRightArm = new ModelRenderer(this, 40, 16);
         this.bipedRightArm.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, modelSize);
         this.bipedRightArm.setRotationPoint(-5.0F, 2.5F, 0.0F);
         this.bipedLeftArmwear = new ModelRenderer(this, 48, 48);
         this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, modelSize + 0.25F);
         this.bipedLeftArmwear.setRotationPoint(5.0F, 2.5F, 0.0F);
         this.bipedRightArmwear = new ModelRenderer(this, 40, 32);
         this.bipedRightArmwear.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, modelSize + 0.25F);
         this.bipedRightArmwear.setRotationPoint(-5.0F, 2.5F, 10.0F);
      } else {
         this.bipedLeftArm = new ModelRenderer(this, 32, 48);
         this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
         this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
         this.bipedLeftArmwear = new ModelRenderer(this, 48, 48);
         this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
         this.bipedLeftArmwear.setRotationPoint(5.0F, 2.0F, 0.0F);
         this.bipedRightArmwear = new ModelRenderer(this, 40, 32);
         this.bipedRightArmwear.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
         this.bipedRightArmwear.setRotationPoint(-5.0F, 2.0F, 10.0F);
      }

      this.bipedLeftLeg = new ModelRenderer(this, 16, 48);
      this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize);
      this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);
      this.bipedLeftLegwear = new ModelRenderer(this, 0, 48);
      this.bipedLeftLegwear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
      this.bipedLeftLegwear.setRotationPoint(1.9F, 12.0F, 0.0F);
      this.bipedRightLegwear = new ModelRenderer(this, 0, 32);
      this.bipedRightLegwear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
      this.bipedRightLegwear.setRotationPoint(-1.9F, 12.0F, 0.0F);
      this.bipedBodyWear = new ModelRenderer(this, 16, 32);
      this.bipedBodyWear.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, modelSize + 0.25F);
      this.bipedBodyWear.setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   @Override
   public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
      boolean var10000;
      label77: {
         super.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
         GlStateManager.pushMatrix();
         if (!Panic.stop && entityIn instanceof EntityLivingBase base && (base.isEating() || base.isDrinking())) {
            var10000 = true;
            break label77;
         }

         var10000 = false;
      }

      boolean trans = var10000;
      boolean transOff = trans && ((EntityLivingBase)entityIn).getActiveHand() == EnumHand.OFF_HAND;
      boolean transMain = trans && ((EntityLivingBase)entityIn).getActiveHand() == EnumHand.MAIN_HAND;
      if (this.isChild) {
         GlStateManager.scale(0.5F, 0.5F, 0.5F);
         GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
         this.bipedLeftLegwear.render(scale);
         this.bipedRightLegwear.render(scale);
         if (transOff) {
            GlStateManager.rotate(-45.0F, 1.0F, -1.0F, 0.0F);
         }

         this.bipedLeftArmwear.render(scale);
         if (transOff) {
            GlStateManager.rotate(45.0F, 1.0F, -1.0F, 0.0F);
         }

         if (transMain) {
            GlStateManager.rotate(-45.0F, 1.0F, 1.0F, 0.0F);
         }

         this.bipedRightArmwear.render(scale);
         if (transMain) {
            GlStateManager.rotate(45.0F, 1.0F, 1.0F, 0.0F);
         }

         this.bipedBodyWear.render(scale);
      } else {
         if (entityIn.isSneaking()) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
         }

         this.bipedLeftLegwear.render(scale);
         this.bipedRightLegwear.render(scale);
         if (transMain) {
            GlStateManager.rotate(-45.0F, 1.0F, 1.0F, 0.0F);
         }

         this.bipedRightArmwear.render(scale);
         if (transMain) {
            GlStateManager.rotate(45.0F, 1.0F, 1.0F, 0.0F);
         }

         if (transOff) {
            GlStateManager.rotate(-45.0F, 1.0F, -1.0F, 0.0F);
         }

         this.bipedLeftArmwear.render(scale);
         if (transOff) {
            GlStateManager.rotate(45.0F, 1.0F, -1.0F, 0.0F);
         }

         this.bipedBodyWear.render(scale);
      }

      GlStateManager.popMatrix();
   }

   public void renderCape(float scale) {
      this.bipedCape.render(scale);
   }

   @Override
   public void setRotationAngles(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn
   ) {
      super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
      ModelBase.copyModelAngles(this.bipedLeftLeg, this.bipedLeftLegwear);
      ModelBase.copyModelAngles(this.bipedRightLeg, this.bipedRightLegwear);
      ModelBase.copyModelAngles(this.bipedLeftArm, this.bipedLeftArmwear);
      ModelBase.copyModelAngles(this.bipedRightArm, this.bipedRightArmwear);
      ModelBase.copyModelAngles(this.bipedBody, this.bipedBodyWear);
   }

   @Override
   public void setInvisible(boolean invisible) {
      super.setInvisible(invisible);
      this.bipedLeftArmwear.showModel = invisible;
      this.bipedRightArmwear.showModel = invisible;
      this.bipedLeftLegwear.showModel = invisible;
      this.bipedRightLegwear.showModel = invisible;
      this.bipedBodyWear.showModel = invisible;
      this.bipedCape.showModel = invisible;
   }

   @Override
   public void postRenderArm(float scale, EnumHandSide side) {
      ModelRenderer modelrenderer = this.getArmForSide(side);
      if (this.smallArms) {
         float f = 0.5F * (float)(side == EnumHandSide.RIGHT ? 1 : -1);
         modelrenderer.rotationPointX += f;
         modelrenderer.postRender(scale);
         modelrenderer.rotationPointX -= f;
      } else {
         modelrenderer.postRender(scale);
      }
   }
}
