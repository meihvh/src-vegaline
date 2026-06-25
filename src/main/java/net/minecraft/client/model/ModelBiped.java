package net.minecraft.client.model;

import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import ru.govno.client.module.modules.BadTrip;
import ru.govno.client.utils.Command.impl.Panic;

public class ModelBiped extends ModelBase {
   public ModelRenderer bipedHead;
   public ModelRenderer bipedHeadwear;
   public ModelRenderer bipedBody;
   public ModelRenderer bipedRightArm;
   public ModelRenderer bipedLeftArm;
   public ModelRenderer bipedRightLeg;
   public ModelRenderer bipedLeftLeg;
   public ModelBiped.ArmPose leftArmPose = ModelBiped.ArmPose.EMPTY;
   public ModelBiped.ArmPose rightArmPose = ModelBiped.ArmPose.EMPTY;
   public boolean isSneak;

   public ModelBiped() {
      this(0.0F);
   }

   public ModelBiped(float modelSize) {
      this(modelSize, 0.0F, 64, 32);
   }

   public ModelBiped(float modelSize, float p_i1149_2_, int textureWidthIn, int textureHeightIn) {
      this.textureWidth = textureWidthIn;
      this.textureHeight = textureHeightIn;
      this.bipedHead = new ModelRenderer(this, 0, 0);
      this.bipedHead.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, modelSize);
      this.bipedHead.setRotationPoint(0.0F, 0.0F + p_i1149_2_, 0.0F);
      this.bipedHeadwear = new ModelRenderer(this, 32, 0);
      this.bipedHeadwear.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, modelSize + 0.5F);
      this.bipedHeadwear.setRotationPoint(0.0F, 0.0F + p_i1149_2_, 0.0F);
      this.bipedBody = new ModelRenderer(this, 16, 16);
      this.bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, modelSize);
      this.bipedBody.setRotationPoint(0.0F, 0.0F + p_i1149_2_, 0.0F);
      this.bipedRightArm = new ModelRenderer(this, 40, 16);
      this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
      this.bipedRightArm.setRotationPoint(-5.0F, 2.0F + p_i1149_2_, 0.0F);
      this.bipedLeftArm = new ModelRenderer(this, 40, 16);
      this.bipedLeftArm.mirror = true;
      this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
      this.bipedLeftArm.setRotationPoint(5.0F, 2.0F + p_i1149_2_, 0.0F);
      this.bipedRightLeg = new ModelRenderer(this, 0, 16);
      this.bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize);
      this.bipedRightLeg.setRotationPoint(-1.9F, 12.0F + p_i1149_2_, 0.0F);
      this.bipedLeftLeg = new ModelRenderer(this, 0, 16);
      this.bipedLeftLeg.mirror = true;
      this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize);
      this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F + p_i1149_2_, 0.0F);
   }

   @Override
   public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
      this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
      GlStateManager.pushMatrix();
      if (this.isChild) {
         boolean humped = false;
         float f = 2.0F;
         GlStateManager.scale(0.75F, 0.75F, 0.75F);
         GlStateManager.translate(0.0F, 16.0F * scale, 0.0F);
         if (entityIn instanceof EntityOtherPlayerMP) {
            int hump = BadTrip.get.getPlayerHumpLevel();
            if (hump != 0) {
               float renderPC = BadTrip.get.HumpPlayers.getAnimation();
               GL11.glPushMatrix();
               switch (hump) {
                  case 1:
                     GL11.glTranslated(0.0, 0.0, (double)(-0.1F * renderPC));
                     break;
                  case 2:
                     GL11.glTranslated(0.0, (double)(0.1F * renderPC), -0.2 * (double)renderPC);
                     break;
                  case 3:
                     GL11.glTranslated(0.0, (double)(0.3F * renderPC), -0.12 * (double)renderPC);
               }

               this.bipedHeadwear.render(scale);
               this.bipedHead.render(scale);
               humped = true;
               GL11.glPopMatrix();
            } else {
               this.bipedHead.render(scale);
            }
         } else {
            this.bipedHead.render(scale);
         }

         GlStateManager.popMatrix();
         GlStateManager.pushMatrix();
         GlStateManager.scale(0.5F, 0.5F, 0.5F);
         GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
         this.bipedBody.render(scale);
         this.bipedRightArm.render(scale);
         this.bipedLeftArm.render(scale);
         this.bipedRightLeg.render(scale);
         this.bipedLeftLeg.render(scale);
         if (!humped) {
            this.bipedHeadwear.render(scale);
         }
      } else {
         boolean humpedx = false;
         if (entityIn.isSneaking()) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
         }

         if (entityIn instanceof EntityOtherPlayerMP) {
            int hump = BadTrip.get.getPlayerHumpLevel();
            if (hump != 0) {
               float renderPC = BadTrip.get.HumpPlayers.getAnimation();
               GL11.glPushMatrix();
               switch (hump) {
                  case 1:
                     GL11.glTranslated(0.0, 0.0, (double)(-0.1F * renderPC));
                     break;
                  case 2:
                     GL11.glTranslated(0.0, (double)(0.2F * renderPC), -0.08 * (double)renderPC);
                     break;
                  case 3:
                     GL11.glTranslated(0.0, (double)(0.5F * renderPC), -0.12 * (double)renderPC);
               }

               this.bipedHead.render(scale);
               this.bipedHeadwear.render(scale);
               humpedx = true;
               GL11.glPopMatrix();
            } else {
               this.bipedHead.render(scale);
            }
         } else {
            this.bipedHead.render(scale);
         }

         boolean var10000;
         label155: {
            this.bipedBody.render(scale);
            if (!Panic.stop && entityIn instanceof EntityLivingBase base && (base.isEating() || base.isDrinking())) {
               var10000 = true;
               break label155;
            }

            var10000 = false;
         }

         boolean trans = var10000;
         boolean transOff = trans && ((EntityLivingBase)entityIn).getActiveHand() == EnumHand.OFF_HAND;
         boolean transMain = trans && ((EntityLivingBase)entityIn).getActiveHand() == EnumHand.MAIN_HAND;
         if (transMain) {
            GlStateManager.rotate(-45.0F, 1.0F, 1.0F, 0.0F);
         }

         boolean selfBuff;
         float buffPC;
         label129: {
            selfBuff = entityIn instanceof EntityPlayerSP && BadTrip.get.isSelfBuff();
            buffPC = selfBuff ? BadTrip.get.SelfBuff.getAnimation() : 0.0F;
            if (selfBuff && entityIn instanceof EntityPlayerSP sp && sp.getHeldItemMainhand().getItem() != Items.air) {
               var10000 = true;
               break label129;
            }

            var10000 = false;
         }

         boolean handedr;
         label123: {
            handedr = var10000;
            if (selfBuff && entityIn instanceof EntityPlayerSP sp && sp.getHeldItemOffhand().getItem() != Items.air) {
               var10000 = true;
               break label123;
            }

            var10000 = false;
         }

         boolean handedl = var10000;
         if (selfBuff) {
            GL11.glTranslated(0.15 * (double)buffPC, 0.0, 0.0);
            GL11.glScaled(1.0 + 0.6 * (double)buffPC, 1.0, (double)(1.0F + 1.0F * buffPC));
            if (handedr) {
               GL11.glRotated((double)(7.0F * buffPC), 1.0, 0.0, 0.0);
            }

            this.bipedRightArm.render(scale);
            if (handedr) {
               GL11.glRotated((double)(7.0F * buffPC), -1.0, 0.0, 0.0);
            }

            GL11.glScaled(1.0 / (1.0 + 0.6 * (double)buffPC), 1.0, (double)(1.0F / (1.0F + 1.0F * buffPC)));
            GL11.glTranslated(-0.15 * (double)buffPC, 0.0, 0.0);
         } else {
            this.bipedRightArm.render(scale);
         }

         if (transMain) {
            GlStateManager.rotate(45.0F, 1.0F, 1.0F, 0.0F);
         }

         if (transOff) {
            GlStateManager.rotate(-45.0F, 1.0F, -1.0F, 0.0F);
         }

         if (selfBuff) {
            GL11.glTranslated(-0.15 * (double)buffPC, 0.0, 0.0);
            GL11.glScaled(1.0 + 0.6 * (double)buffPC, 1.0, (double)(1.0F + 1.0F * buffPC));
            if (handedl) {
               GL11.glRotated((double)(7.0F * buffPC), 1.0, 0.0, 0.0);
            }

            this.bipedLeftArm.render(scale);
            if (handedl) {
               GL11.glRotated((double)(7.0F * buffPC), -1.0, 0.0, 0.0);
            }

            GL11.glScaled(1.0 / (1.0 + 0.6 * (double)buffPC), 1.0, (double)(1.0F / (1.0F + 1.0F * buffPC)));
            GL11.glTranslated(0.15 * (double)buffPC, 0.0, 0.0);
         } else {
            this.bipedLeftArm.render(scale);
         }

         if (transOff) {
            GlStateManager.rotate(45.0F, 1.0F, -1.0F, 0.0F);
         }

         this.bipedRightLeg.render(scale);
         this.bipedLeftLeg.render(scale);
         if (!humpedx) {
            this.bipedHeadwear.render(scale);
         }
      }

      GlStateManager.popMatrix();
   }

   @Override
   public void setRotationAngles(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn
   ) {
      boolean flag = entityIn instanceof EntityLivingBase && ((EntityLivingBase)entityIn).getTicksElytraFlying() > 4;
      this.bipedHead.rotateAngleY = netHeadYaw * (float) (Math.PI / 180.0);
      if (flag) {
         this.bipedHead.rotateAngleX = (float) (-Math.PI / 4);
      } else {
         byte var10002;
         label90: {
            if (entityIn instanceof EntityLivingBase base && base.isLay) {
               var10002 = 60;
               break label90;
            }

            var10002 = 0;
         }

         this.bipedHead.rotateAngleX = (headPitch - (float)var10002) * (float) (Math.PI / 180.0);
      }

      this.bipedBody.rotateAngleY = 0.0F;
      this.bipedRightArm.rotationPointZ = 0.0F;
      this.bipedRightArm.rotationPointX = -5.0F;
      this.bipedLeftArm.rotationPointZ = 0.0F;
      this.bipedLeftArm.rotationPointX = 5.0F;
      float f = 1.0F;
      if (flag) {
         f = (float)(entityIn.motionX * entityIn.motionX + entityIn.motionY * entityIn.motionY + entityIn.motionZ * entityIn.motionZ);
         f /= 0.2F;
         f = f * f * f;
      }

      if (f < 1.0F) {
         f = 1.0F;
      }

      this.bipedRightArm.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F / f;
      this.bipedLeftArm.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F / f;
      if (entityIn instanceof EntityLivingBase base && base.isLay) {
         this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX
            - MathHelper.toRadians(base.getHeldItemMainhand().getItem() == Items.air ? 170.0F : 320.0F);
         this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX
            - MathHelper.toRadians(base.getHeldItemOffhand().getItem() == Items.air ? 170.0F : 320.0F);
      }

      this.bipedRightArm.rotateAngleZ = 0.0F;
      this.bipedLeftArm.rotateAngleZ = 0.0F;
      this.bipedRightLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount / f;
      this.bipedLeftLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount / f;
      this.bipedRightLeg.rotateAngleY = 0.0F;
      this.bipedLeftLeg.rotateAngleY = 0.0F;
      this.bipedRightLeg.rotateAngleZ = 0.0F;
      this.bipedLeftLeg.rotateAngleZ = 0.0F;
      if (this.isRiding) {
         this.bipedRightArm.rotateAngleX += (float) (-Math.PI / 5);
         this.bipedLeftArm.rotateAngleX += (float) (-Math.PI / 5);
         this.bipedRightLeg.rotateAngleX = -1.4137167F;
         this.bipedRightLeg.rotateAngleY = (float) (Math.PI / 10);
         this.bipedRightLeg.rotateAngleZ = 0.07853982F;
         this.bipedLeftLeg.rotateAngleX = -1.4137167F;
         this.bipedLeftLeg.rotateAngleY = (float) (-Math.PI / 10);
         this.bipedLeftLeg.rotateAngleZ = -0.07853982F;
      }

      this.bipedRightArm.rotateAngleY = 0.0F;
      this.bipedRightArm.rotateAngleZ = 0.0F;
      switch (this.leftArmPose) {
         case EMPTY:
            this.bipedLeftArm.rotateAngleY = 0.0F;
            break;
         case BLOCK:
            this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX * 0.5F - 0.9424779F;
            this.bipedLeftArm.rotateAngleY = (float) (Math.PI / 6);
            break;
         case ITEM:
            this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX * 0.5F - (float) (Math.PI / 10);
            this.bipedLeftArm.rotateAngleY = 0.0F;
      }

      switch (this.rightArmPose) {
         case EMPTY:
            this.bipedRightArm.rotateAngleY = 0.0F;
            break;
         case BLOCK:
            this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX * 0.5F - 0.9424779F;
            this.bipedRightArm.rotateAngleY = (float) (-Math.PI / 6);
            break;
         case ITEM:
            this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX * 0.5F - (float) (Math.PI / 10);
            this.bipedRightArm.rotateAngleY = 0.0F;
      }

      if (this.swingProgress > 0.0F) {
         EnumHandSide enumhandside = this.getMainHand(entityIn);
         ModelRenderer modelrenderer = this.getArmForSide(enumhandside);
         float f1 = this.swingProgress;
         this.bipedBody.rotateAngleY = MathHelper.sin(MathHelper.sqrt(f1) * (float) (Math.PI * 2)) * 0.2F;
         if (enumhandside == EnumHandSide.LEFT) {
            this.bipedBody.rotateAngleY *= -1.0F;
         }

         this.bipedRightArm.rotationPointZ = MathHelper.sin(this.bipedBody.rotateAngleY) * 5.0F;
         this.bipedRightArm.rotationPointX = -MathHelper.cos(this.bipedBody.rotateAngleY) * 5.0F;
         this.bipedLeftArm.rotationPointZ = -MathHelper.sin(this.bipedBody.rotateAngleY) * 5.0F;
         this.bipedLeftArm.rotationPointX = MathHelper.cos(this.bipedBody.rotateAngleY) * 5.0F;
         this.bipedRightArm.rotateAngleY = this.bipedRightArm.rotateAngleY + this.bipedBody.rotateAngleY;
         this.bipedLeftArm.rotateAngleY = this.bipedLeftArm.rotateAngleY + this.bipedBody.rotateAngleY;
         this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX + this.bipedBody.rotateAngleY;
         f1 = 1.0F - this.swingProgress;
         f1 *= f1;
         f1 *= f1;
         f1 = 1.0F - f1;
         float f2 = MathHelper.sin(f1 * (float) Math.PI);
         float f3 = MathHelper.sin(this.swingProgress * (float) Math.PI) * -(this.bipedHead.rotateAngleX - 0.7F) * 0.75F;
         modelrenderer.rotateAngleX = (float)((double)modelrenderer.rotateAngleX - ((double)f2 * 1.2 + (double)f3));
         modelrenderer.rotateAngleY = modelrenderer.rotateAngleY + this.bipedBody.rotateAngleY * 2.0F;
         modelrenderer.rotateAngleZ = modelrenderer.rotateAngleZ + MathHelper.sin(this.swingProgress * (float) Math.PI) * -0.4F;
      }

      if (!this.isSneak || entityIn instanceof EntityLivingBase base && base.isLay) {
         this.bipedBody.rotateAngleX = 0.0F;
         this.bipedRightLeg.rotationPointZ = 0.1F;
         this.bipedLeftLeg.rotationPointZ = 0.1F;
         this.bipedRightLeg.rotationPointY = 12.0F;
         this.bipedLeftLeg.rotationPointY = 12.0F;
         this.bipedHead.rotationPointY = 0.0F;
      } else {
         this.bipedBody.rotateAngleX = 0.5F;
         this.bipedRightArm.rotateAngleX += 0.4F;
         this.bipedLeftArm.rotateAngleX += 0.4F;
         this.bipedRightLeg.rotationPointZ = 4.0F;
         this.bipedLeftLeg.rotationPointZ = 4.0F;
         this.bipedRightLeg.rotationPointY = 9.0F;
         this.bipedLeftLeg.rotationPointY = 9.0F;
         this.bipedHead.rotationPointY = 1.0F;
      }

      this.bipedRightArm.rotateAngleZ = this.bipedRightArm.rotateAngleZ + MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
      this.bipedLeftArm.rotateAngleZ = this.bipedLeftArm.rotateAngleZ - (MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F);
      this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX + MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
      this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX - MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
      if (this.rightArmPose == ModelBiped.ArmPose.BOW_AND_ARROW) {
         this.bipedRightArm.rotateAngleY = -0.1F + this.bipedHead.rotateAngleY;
         this.bipedLeftArm.rotateAngleY = 0.1F + this.bipedHead.rotateAngleY + 0.4F;
         this.bipedRightArm.rotateAngleX = (float) (-Math.PI / 2) + this.bipedHead.rotateAngleX;
         this.bipedLeftArm.rotateAngleX = (float) (-Math.PI / 2) + this.bipedHead.rotateAngleX;
      } else if (this.leftArmPose == ModelBiped.ArmPose.BOW_AND_ARROW) {
         this.bipedRightArm.rotateAngleY = -0.1F + this.bipedHead.rotateAngleY - 0.4F;
         this.bipedLeftArm.rotateAngleY = 0.1F + this.bipedHead.rotateAngleY;
         this.bipedRightArm.rotateAngleX = (float) (-Math.PI / 2) + this.bipedHead.rotateAngleX;
         this.bipedLeftArm.rotateAngleX = (float) (-Math.PI / 2) + this.bipedHead.rotateAngleX;
      }

      copyModelAngles(this.bipedHead, this.bipedHeadwear);
   }

   @Override
   public void setModelAttributes(ModelBase model) {
      super.setModelAttributes(model);
      if (model instanceof ModelBiped modelbiped) {
         this.leftArmPose = modelbiped.leftArmPose;
         this.rightArmPose = modelbiped.rightArmPose;
         this.isSneak = modelbiped.isSneak;
      }
   }

   public void setInvisible(boolean invisible) {
      this.bipedHead.showModel = invisible;
      this.bipedHeadwear.showModel = invisible;
      this.bipedBody.showModel = invisible;
      this.bipedRightArm.showModel = invisible;
      this.bipedLeftArm.showModel = invisible;
      this.bipedRightLeg.showModel = invisible;
      this.bipedLeftLeg.showModel = invisible;
   }

   public void postRenderArm(float scale, EnumHandSide side) {
      this.getArmForSide(side).postRender(scale);
   }

   protected ModelRenderer getArmForSide(EnumHandSide side) {
      return side == EnumHandSide.LEFT ? this.bipedLeftArm : this.bipedRightArm;
   }

   protected EnumHandSide getMainHand(Entity entityIn) {
      if (entityIn instanceof EntityLivingBase entitylivingbase) {
         EnumHandSide enumhandside = entitylivingbase.getPrimaryHand();
         return entitylivingbase.swingingHand == EnumHand.MAIN_HAND ? enumhandside : enumhandside.opposite();
      } else {
         return EnumHandSide.RIGHT;
      }
   }

   public static enum ArmPose {
      EMPTY,
      ITEM,
      BLOCK,
      BOW_AND_ARROW;
   }
}
