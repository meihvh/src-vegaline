package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

public class ParticleSimpleAnimated extends Particle {
   private final int textureIdx;
   private final int numAgingFrames;
   private final float yAccel;
   private float field_191239_M = 0.91F;
   private float fadeTargetRed;
   private float fadeTargetGreen;
   private float fadeTargetBlue;
   private boolean fadingColor;

   public ParticleSimpleAnimated(World worldIn, double x, double y, double z, int textureIdxIn, int numFrames, float yAccelIn) {
      super(worldIn, x, y, z);
      this.textureIdx = textureIdxIn;
      this.numAgingFrames = numFrames;
      this.yAccel = yAccelIn;
   }

   public void setColor(int p_187146_1_) {
      float f = (float)((p_187146_1_ & 0xFF0000) >> 16) / 255.0F;
      float f1 = (float)((p_187146_1_ & 0xFF00) >> 8) / 255.0F;
      float f2 = (float)((p_187146_1_ & 0xFF) >> 0) / 255.0F;
      float f3 = 1.0F;
      this.setRBGColorF(f * 1.0F, f1 * 1.0F, f2 * 1.0F);
   }

   public void setColorFade(int rgb) {
      this.fadeTargetRed = (float)((rgb & 0xFF0000) >> 16) / 255.0F;
      this.fadeTargetGreen = (float)((rgb & 0xFF00) >> 8) / 255.0F;
      this.fadeTargetBlue = (float)((rgb & 0xFF) >> 0) / 255.0F;
      this.fadingColor = true;
   }

   @Override
   public boolean isTransparent() {
      return true;
   }

   @Override
   public void onUpdate() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      this.particleAge = this.particleAge + Minecraft.getMinecraft().particlesSpeed();
      if (this.particleAge >= (float)this.particleMaxAge) {
         this.setExpired();
      }

      if (this.particleAge > (float)(this.particleMaxAge / 2)) {
         this.setAlphaF(1.0F - (this.particleAge - (float)(this.particleMaxAge / 2)) / (float)this.particleMaxAge);
         if (this.fadingColor) {
            this.particleRed = this.particleRed + (this.fadeTargetRed - this.particleRed) * 0.2F;
            this.particleGreen = this.particleGreen + (this.fadeTargetGreen - this.particleGreen) * 0.2F;
            this.particleBlue = this.particleBlue + (this.fadeTargetBlue - this.particleBlue) * 0.2F;
         }
      }

      this.setParticleTextureIndex(
         (int)((float)this.textureIdx + ((float)(this.numAgingFrames - 1) - this.particleAge * (float)this.numAgingFrames / (float)this.particleMaxAge))
      );
      this.motionY = this.motionY + (double)this.yAccel;
      this.moveEntity(this.motionX, this.motionY, this.motionZ);
      this.motionX = this.motionX * (double)this.field_191239_M;
      this.motionY = this.motionY * (double)this.field_191239_M;
      this.motionZ = this.motionZ * (double)this.field_191239_M;
      if (this.isCollided) {
         this.motionX *= 0.7F;
         this.motionZ *= 0.7F;
      }
   }

   @Override
   public int getBrightnessForRender(float p_189214_1_) {
      return 15728880;
   }

   protected void func_191238_f(float p_191238_1_) {
      this.field_191239_M = p_191238_1_;
   }
}
