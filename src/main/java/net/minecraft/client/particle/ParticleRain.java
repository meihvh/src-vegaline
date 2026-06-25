package net.minecraft.client.particle;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class ParticleRain extends Particle {
   protected ParticleRain(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn) {
      super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0.0, 0.0, 0.0);
      this.motionX *= 0.3F;
      this.motionY = Math.random() * 0.2F + 0.1F;
      this.motionZ *= 0.3F;
      this.particleRed = 1.0F;
      this.particleGreen = 1.0F;
      this.particleBlue = 1.0F;
      this.setParticleTextureIndex(19 + this.rand.nextInt(4));
      this.setSize(0.01F, 0.01F);
      this.particleGravity = 0.06F;
      this.particleMaxAge = (int)(8.0 / (Math.random() * 0.8 + 0.2));
   }

   @Override
   public void onUpdate() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      this.motionY = this.motionY - (double)this.particleGravity;
      this.moveEntity(this.motionX, this.motionY, this.motionZ);
      this.motionX *= 0.98F;
      this.motionY *= 0.98F;
      this.motionZ *= 0.98F;
      if (this.particleMaxAge-- <= 0) {
         this.setExpired();
      }

      if (this.isCollided) {
         if (Math.random() < 0.5) {
            this.setExpired();
         }

         this.motionX *= 0.7F;
         this.motionZ *= 0.7F;
      }

      BlockPos blockpos = new BlockPos(this.posX, this.posY, this.posZ);
      IBlockState iblockstate = this.worldObj.getBlockState(blockpos);
      Material material = iblockstate.getMaterial();
      if (material.isLiquid() || material.isSolid()) {
         double d0;
         if (iblockstate.getBlock() instanceof BlockLiquid) {
            d0 = (double)(1.0F - BlockLiquid.getLiquidHeightPercent(iblockstate.getValue(BlockLiquid.LEVEL)));
         } else {
            d0 = iblockstate.getBoundingBox(this.worldObj, blockpos).maxY;
         }

         double d1 = (double)MathHelper.floor(this.posY) + d0;
         if (this.posY < d1) {
            this.setExpired();
         }
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
         return new ParticleRain(worldIn, xCoordIn, yCoordIn, zCoordIn);
      }
   }
}
