package net.minecraft.client.particle;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import ru.govno.client.module.modules.WorldRender;

public class ParticleEmitter extends Particle {
   private final Entity attachedEntity;
   private float age;
   private final int lifetime;
   private final EnumParticleTypes particleTypes;

   public ParticleEmitter(World worldIn, Entity p_i46279_2_, EnumParticleTypes particleTypesIn) {
      this(worldIn, p_i46279_2_, particleTypesIn, 3);
   }

   public ParticleEmitter(World p_i47219_1_, Entity p_i47219_2_, EnumParticleTypes p_i47219_3_, int p_i47219_4_) {
      super(
         p_i47219_1_,
         p_i47219_2_.posX,
         p_i47219_2_.getEntityBoundingBox().minY + (double)(p_i47219_2_.height / 2.0F),
         p_i47219_2_.posZ,
         p_i47219_2_.motionX,
         p_i47219_2_.motionY,
         p_i47219_2_.motionZ
      );
      this.attachedEntity = p_i47219_2_;
      this.lifetime = p_i47219_4_;
      this.particleTypes = p_i47219_3_;
      this.onUpdate();
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
   }

   @Override
   public void onUpdate() {
      for (int i = 0;
         i
            < Math.min(
               Math.max(
                  WorldRender.get.particleReCount(16) / (this.particleTypes == EnumParticleTypes.TOTEM && WorldRender.decreaseTotemParticles() ? 6 : 1), 0
               ),
               56
            );
         i++
      ) {
         double d0 = (double)(this.rand.nextFloat() * 2.0F - 1.0F);
         double d1 = (double)(this.rand.nextFloat() * 2.0F - 1.0F);
         double d2 = (double)(this.rand.nextFloat() * 2.0F - 1.0F);
         if (d0 * d0 + d1 * d1 + d2 * d2 <= 1.0) {
            double d3 = this.attachedEntity.posX + d0 * (double)this.attachedEntity.width / 4.0;
            double d4 = this.attachedEntity.boundingBox.minY + (double)(this.attachedEntity.height / 2.0F) + d1 * (double)this.attachedEntity.height / 4.0;
            double d5 = this.attachedEntity.posZ + d2 * (double)this.attachedEntity.width / 4.0;
            this.worldObj.spawnParticle(this.particleTypes, false, d3, d4, d5, d0, d1 + 0.2, d2);
         }
      }

      if (this.age++ >= (float)this.lifetime) {
         this.setExpired();
      }
   }

   @Override
   public int getFXLayer() {
      return 3;
   }
}
