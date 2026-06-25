package net.minecraft.client.particle;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

public class ParticleSpell extends Particle {
   private static final Random RANDOM = new Random();
   private int baseSpellTextureIndex = 128;

   protected ParticleSpell(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double p_i1229_8_, double ySpeed, double p_i1229_12_) {
      super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0.5 - RANDOM.nextDouble(), ySpeed, 0.5 - RANDOM.nextDouble());
      this.motionY *= 0.2F;
      if (p_i1229_8_ == 0.0 && p_i1229_12_ == 0.0) {
         this.motionX *= 0.1F;
         this.motionZ *= 0.1F;
      }

      this.particleScale *= 0.75F;
      this.particleMaxAge = (int)(8.0 / (Math.random() * 0.8 + 0.2));
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

      this.setParticleTextureIndex((int)((float)this.baseSpellTextureIndex + (7.0F - this.particleAge * 8.0F / (float)this.particleMaxAge)));
      this.motionY += 0.004;
      this.moveEntity(this.motionX, this.motionY, this.motionZ);
      if (this.posY == this.prevPosY) {
         this.motionX *= 1.1;
         this.motionZ *= 1.1;
      }

      this.motionX *= 0.96F;
      this.motionY *= 0.96F;
      this.motionZ *= 0.96F;
      if (this.isCollided) {
         this.motionX *= 0.7F;
         this.motionZ *= 0.7F;
      }
   }

   public void setBaseSpellTextureIndex(int baseSpellTextureIndexIn) {
      this.baseSpellTextureIndex = baseSpellTextureIndexIn;
   }

   public static class AmbientMobFactory implements IParticleFactory {
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
         Particle particle = new ParticleSpell(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
         particle.setAlphaF(0.15F);
         particle.setRBGColorF((float)xSpeedIn, (float)ySpeedIn, (float)zSpeedIn);
         return particle;
      }
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
         return new ParticleSpell(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
      }
   }

   public static class InstantFactory implements IParticleFactory {
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
         Particle particle = new ParticleSpell(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
         ((ParticleSpell)particle).setBaseSpellTextureIndex(144);
         return particle;
      }
   }

   public static class MobFactory implements IParticleFactory {
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
         Particle particle = new ParticleSpell(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
         particle.setRBGColorF((float)xSpeedIn, (float)ySpeedIn, (float)zSpeedIn);
         return particle;
      }
   }

   public static class WitchFactory implements IParticleFactory {
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
         Particle particle = new ParticleSpell(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
         ((ParticleSpell)particle).setBaseSpellTextureIndex(144);
         float f = worldIn.rand.nextFloat() * 0.5F + 0.35F;
         particle.setRBGColorF(f, 0.0F * f, f);
         return particle;
      }
   }
}
