package net.minecraft.cape;

import net.minecraft.cape.sim.StickSimulation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;

public interface CapeHolder {
   StickSimulation getSimulation();

   default void updateSimulation(EntityPlayer abstractClientPlayer, int partCount) {
      StickSimulation simulation = this.getSimulation();
      boolean dirty = false;
      if (simulation.points.size() != partCount) {
         simulation.points.clear();
         simulation.sticks.clear();

         for (int i = 0; i < partCount; i++) {
            StickSimulation.Point point = new StickSimulation.Point();
            point.position.y = (float)(-i);
            point.locked = i == 0;
            simulation.points.add(point);
            if (i > 0) {
               simulation.sticks.add(new StickSimulation.Stick(simulation.points.get(i - 1), point, 1.0F));
            }
         }

         dirty = true;
      }

      if (dirty) {
         this.simulate(abstractClientPlayer);
      }
   }

   default void simulate(EntityPlayer abstractClientPlayer) {
      StickSimulation simulation = this.getSimulation();
      if (simulation != null) {
         if (!simulation.points.isEmpty()) {
            simulation.points.get(0).prevPosition.copy(simulation.points.get(0).position);
            double d = abstractClientPlayer.chasingPosX - abstractClientPlayer.posX;
            double m = abstractClientPlayer.chasingPosZ - abstractClientPlayer.posZ;
            float n = abstractClientPlayer.prevRenderYawOffset + abstractClientPlayer.renderYawOffset - abstractClientPlayer.prevRenderYawOffset;
            double o = Math.sin((double)(n * (float) (Math.PI / 180.0)));
            double p = -Math.cos((double)(n * (float) (Math.PI / 180.0)));
            float heightMul = 5.0F;
            double fallHack = MathHelper.clamp((double)simulation.points.get(0).position.y - abstractClientPlayer.posY * (double)heightMul, 0.0, 1.0);
            StickSimulation.Vector2 var10000 = simulation.points.get(0).position;
            var10000.x = (float)((double)var10000.x + (d * o + m * p) / 8.0 + fallHack * 2.0);
            simulation.points.get(0).position.y = (float)(abstractClientPlayer.posY * (double)heightMul + (double)(abstractClientPlayer.isSneaking() ? -4 : 0));
            simulation.simulate();
         }
      }
   }
}
