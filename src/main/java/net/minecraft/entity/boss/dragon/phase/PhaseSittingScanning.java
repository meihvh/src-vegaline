package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PhaseSittingScanning extends PhaseSittingBase {
   private int scanningTime;

   public PhaseSittingScanning(EntityDragon dragonIn) {
      super(dragonIn);
   }

   @Override
   public void doLocalUpdate() {
      this.scanningTime++;
      EntityLivingBase entitylivingbase = this.dragon.world.getNearestAttackablePlayer(this.dragon, 20.0, 10.0);
      if (entitylivingbase != null) {
         if (this.scanningTime > 25) {
            this.dragon.getPhaseManager().setPhase(PhaseList.SITTING_ATTACKING);
         } else {
            Vec3d vec3d = new Vec3d(entitylivingbase.posX - this.dragon.posX, 0.0, entitylivingbase.posZ - this.dragon.posZ).normalize();
            Vec3d vec3d1 = new Vec3d(
                  (double)MathHelper.sin(this.dragon.rotationYaw * (float) (Math.PI / 180.0)),
                  0.0,
                  (double)(-MathHelper.cos(this.dragon.rotationYaw * (float) (Math.PI / 180.0)))
               )
               .normalize();
            float f = (float)vec3d1.dotProduct(vec3d);
            float f1 = (float)(Math.acos((double)f) * (180.0 / Math.PI)) + 0.5F;
            if (f1 < 0.0F || f1 > 10.0F) {
               double d0 = entitylivingbase.posX - this.dragon.dragonPartHead.posX;
               double d1 = entitylivingbase.posZ - this.dragon.dragonPartHead.posZ;
               double d2 = MathHelper.clamp(
                  MathHelper.wrapDegrees(180.0 - MathHelper.atan2(d0, d1) * (180.0 / Math.PI) - (double)this.dragon.rotationYaw), -100.0, 100.0
               );
               this.dragon.randomYawVelocity *= 0.8F;
               float f2 = MathHelper.sqrt(d0 * d0 + d1 * d1) + 1.0F;
               float f3 = f2;
               if (f2 > 40.0F) {
                  f2 = 40.0F;
               }

               this.dragon.randomYawVelocity = (float)((double)this.dragon.randomYawVelocity + d2 * (double)(0.7F / f2 / f3));
               this.dragon.rotationYaw = this.dragon.rotationYaw + this.dragon.randomYawVelocity;
            }
         }
      } else if (this.scanningTime >= 100) {
         entitylivingbase = this.dragon.world.getNearestAttackablePlayer(this.dragon, 150.0, 150.0);
         this.dragon.getPhaseManager().setPhase(PhaseList.TAKEOFF);
         if (entitylivingbase != null) {
            this.dragon.getPhaseManager().setPhase(PhaseList.CHARGING_PLAYER);
            this.dragon
               .getPhaseManager()
               .getPhase(PhaseList.CHARGING_PLAYER)
               .setTarget(new Vec3d(entitylivingbase.posX, entitylivingbase.posY, entitylivingbase.posZ));
         }
      }
   }

   @Override
   public void initPhase() {
      this.scanningTime = 0;
   }

   @Override
   public PhaseList<PhaseSittingScanning> getPhaseList() {
      return PhaseList.SITTING_SCANNING;
   }
}
