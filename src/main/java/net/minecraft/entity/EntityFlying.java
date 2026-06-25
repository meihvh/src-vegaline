package net.minecraft.entity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public abstract class EntityFlying extends EntityLiving {
   public EntityFlying(World worldIn) {
      super(worldIn);
   }

   @Override
   public void fall(float distance, float damageMultiplier) {
   }

   @Override
   protected void updateFallState(double y, boolean onGroundIn, IBlockState state, BlockPos pos) {
   }

   @Override
   public void func_191986_a(float p_191986_1_, float p_191986_2_, float p_191986_3_) {
      if (this.isInWater()) {
         this.func_191958_b(p_191986_1_, p_191986_2_, p_191986_3_, 0.02F);
         this.moveEntity(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
         this.motionX *= 0.8F;
         this.motionY *= 0.8F;
         this.motionZ *= 0.8F;
      } else if (this.isInLava()) {
         this.func_191958_b(p_191986_1_, p_191986_2_, p_191986_3_, 0.02F);
         this.moveEntity(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
         this.motionX *= 0.5;
         this.motionY *= 0.5;
         this.motionZ *= 0.5;
      } else {
         float f = 0.91F;
         if (this.onGround) {
            f = this.world
                  .getBlockState(new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.getEntityBoundingBox().minY) - 1, MathHelper.floor(this.posZ)))
                  .getBlock()
                  .slipperiness
               * 0.91F;
         }

         float f1 = 0.16277136F / (f * f * f);
         this.func_191958_b(p_191986_1_, p_191986_2_, p_191986_3_, this.onGround ? 0.1F * f1 : 0.02F);
         f = 0.91F;
         if (this.onGround) {
            f = this.world
                  .getBlockState(new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.getEntityBoundingBox().minY) - 1, MathHelper.floor(this.posZ)))
                  .getBlock()
                  .slipperiness
               * 0.91F;
         }

         this.moveEntity(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
         this.motionX *= (double)f;
         this.motionY *= (double)f;
         this.motionZ *= (double)f;
      }

      this.prevLimbSwingAmount = this.limbSwingAmount;
      double d1 = this.posX - this.prevPosX;
      double d0 = this.posZ - this.prevPosZ;
      float f2 = MathHelper.sqrt(d1 * d1 + d0 * d0) * 4.0F;
      if (f2 > 1.0F) {
         f2 = 1.0F;
      }

      this.limbSwingAmount = this.limbSwingAmount + (f2 - this.limbSwingAmount) * 0.4F;
      this.limbSwing = this.limbSwing + this.limbSwingAmount;
   }

   @Override
   public boolean isOnLadder() {
      return false;
   }
}
