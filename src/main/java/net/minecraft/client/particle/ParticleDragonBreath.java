package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class ParticleDragonBreath extends Particle {
   private final float oSize;
   private boolean hasHitGround;

   protected ParticleDragonBreath(World worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
      super(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
      this.motionX = xSpeed;
      this.motionY = ySpeed;
      this.motionZ = zSpeed;
      this.particleRed = MathHelper.nextFloat(this.rand, 0.7176471F, 0.8745098F);
      this.particleGreen = MathHelper.nextFloat(this.rand, 0.0F, 0.0F);
      this.particleBlue = MathHelper.nextFloat(this.rand, 0.8235294F, 0.9764706F);
      this.particleScale *= 0.75F;
      this.oSize = this.particleScale;
      this.particleMaxAge = (int)(20.0 / ((double)this.rand.nextFloat() * 0.8 + 0.2));
      this.hasHitGround = false;
   }

   @Override
   public void onUpdate() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      this.particleAge = this.particleAge + Minecraft.getMinecraft().particlesSpeed();
      if (this.particleAge >= (float)this.particleMaxAge) {
         this.setExpired();
      } else {
         this.setParticleTextureIndex((int)(3.0F * this.particleAge / (float)this.particleMaxAge + 5.0F));
         if (this.isCollided) {
            this.motionY = 0.0;
            this.hasHitGround = true;
         }

         if (this.hasHitGround) {
            this.motionY += 0.002;
         }

         this.moveEntity(this.motionX, this.motionY, this.motionZ);
         if (this.posY == this.prevPosY) {
            this.motionX *= 1.1;
            this.motionZ *= 1.1;
         }

         this.motionX *= 0.96F;
         this.motionZ *= 0.96F;
         if (this.hasHitGround) {
            this.motionY *= 0.96F;
         }
      }
   }

   @Override
   public void renderParticle(
      BufferBuilder worldRendererIn,
      Entity entityIn,
      float partialTicks,
      float rotationX,
      float rotationZ,
      float rotationYZ,
      float rotationXY,
      float rotationXZ
   ) {
      this.particleScale = this.oSize * MathHelper.clamp((this.particleAge + partialTicks) / (float)this.particleMaxAge * 32.0F, 0.0F, 1.0F);
      super.renderParticle(worldRendererIn, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
   }

   public static class Factory implements IParticleFactory {
      @Override
      public Particle createParticle(
         int particleID,
         World worldIn,
         double xCoordIn,
         double yCoordIn,
         double zCoordIn,
         double xSpeedIn,
         double ySpeedIn,
         double zSpeedIn,
         int... p_178902_15_
      ) {
         return new ParticleDragonBreath(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
      }
   }
}
