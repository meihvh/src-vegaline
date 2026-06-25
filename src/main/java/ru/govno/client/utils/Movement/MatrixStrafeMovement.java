package ru.govno.client.utils.Movement;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import ru.govno.client.event.events.EventAction;
import ru.govno.client.event.events.EventMove2;
import ru.govno.client.module.modules.MoveHelper;
import ru.govno.client.module.modules.Strafe;
import ru.govno.client.module.modules.TargetStrafe;
import ru.govno.client.utils.Math.MathUtils;

public class MatrixStrafeMovement {
   public static double oldSpeed;
   public static double contextFriction;
   public static boolean needSwap;
   public static boolean prevSprint;
   public static int counter;
   public static int noSlowTicks;

   public static double calculateSpeed(boolean strict, EventMove2 move, boolean ely, double speed) {
      if (strict && !Minecraft.player.isJumping()) {
         strict = false;
      }

      boolean fromGround = Minecraft.player.onGround;
      boolean toGround = move.toGround();
      boolean jump = move.motion().yCoord > 0.0;
      boolean noSlow = MoveHelper.instance.NoSlowDown.getBool();
      boolean biposNoSlow = noSlow && Minecraft.player.getActiveHand() == EnumHand.MAIN_HAND && MoveHelper.instance.NCPFapBypass.getBool();
      String slowType = MoveHelper.instance.NoSlowMode.currentMode;
      Minecraft mc = Minecraft.getMinecraft();
      float speedAttributes = getAIMoveSpeed(Minecraft.player);
      float frictionFactor = getFrictionFactor(Minecraft.player, move);
      float n6 = strict ? 0.9149F : 0.91F;
      if (fromGround) {
         n6 = frictionFactor;
      }

      float n7 = 0.16277136F / (n6 * n6 * n6);
      float n8;
      if (fromGround) {
         n8 = speedAttributes * n7;
         if (jump) {
            if (strict) {
               boolean speed1 = Minecraft.player.getActivePotionEffect(MobEffects.SPEED) != null
                  && Minecraft.player.getActivePotionEffect(MobEffects.SPEED).getAmplifier() == 0;
               boolean speed2 = Minecraft.player.getActivePotionEffect(MobEffects.SPEED) != null
                  && Minecraft.player.getActivePotionEffect(MobEffects.SPEED).getAmplifier() == 1;
               n8 += speed2 ? 0.332F : (speed1 ? 0.279F : 0.242F);
            } else {
               n8 += 0.2F;
            }
         }
      } else {
         n8 = 0.0255F;
      }

      double max2 = oldSpeed + (double)n8;
      double max = 0.0;
      float noslowPercent = 1.0F;
      boolean cancelSlow = false;
      if (Minecraft.player.isHandActive()) {
         if (noSlow) {
            switch (slowType) {
               case "Vanilla":
                  noslowPercent = MoveHelper.instance.NoSlowPercent.getFloat() / 100.0F;
                  cancelSlow = noslowPercent == 1.0F;
                  break;
               case "MatrixOld":
                  noslowPercent = 0.92F;
                  break;
               case "MatrixLatest":
                  boolean a = false;
                  if (Minecraft.player.isHandActive()
                     && Minecraft.player.getItemInUseMaxCount() > 3
                     && !Minecraft.player.isInWater()
                     && !Minecraft.player.isInLava()
                     && !Minecraft.player.isInWeb
                     && !Minecraft.player.capabilities.isFlying
                     && Minecraft.player.getTicksElytraFlying() <= 1
                     && MoveMeHelp.isMoving()
                     && (
                        (double)Minecraft.player.fallDistance > (Minecraft.player.isJumping() ? 0.725 : 0.5) && (double)Minecraft.player.fallDistance <= 2.5
                           || (double)Minecraft.player.fallDistance > 2.5 && Minecraft.player.fallDistance % 2.0F == 0.0F
                     )) {
                     noslowPercent = 0.9925F;
                     a = true;
                  }

                  if (!a) {
                     cancelSlow = true;
                  }
                  break;
               case "AACOld":
                  noslowPercent = 1.0F;
                  cancelSlow = true;
                  break;
               case "NCP+":
                  if (Minecraft.player.isHandActive()
                     && Minecraft.player.getActiveHand() == EnumHand.MAIN_HAND
                     && Minecraft.player.isBlocking()
                     && Minecraft.player.getActiveHand() == EnumHand.OFF_HAND
                     && !EntityLivingBase.isMatrixDamaged
                     && !Minecraft.player.isInWater()
                     && !Minecraft.player.isInLava()
                     && !biposNoSlow) {
                     float pc = !Minecraft.player.isInWater() && !Minecraft.player.isInLava()
                        ? MathUtils.clamp((float)Minecraft.player.getItemInUseMaxCount() / (Minecraft.player.isJumping() ? 18.0F : 12.0F), 0.0F, 1.0F)
                        : 0.0F;
                     noslowPercent = 1.0F
                        - (Minecraft.player.onGround ? (Minecraft.player.isJumping() ? 0.5F : 0.43F) : (move.motion().yCoord > 0.0 ? 0.56F : 0.13F)) * pc;
                     boolean stop = Minecraft.player.isInWater()
                        || Minecraft.player.isInLava()
                        || Minecraft.player.isInWeb
                        || Minecraft.player.capabilities.isFlying
                        || Minecraft.player.getTicksElytraFlying() > 1
                        || !MoveMeHelp.isMoving();
                     if (!Minecraft.player.isHandActive()
                        || Minecraft.player.getItemInUseMaxCount() <= 3
                        || stop
                        || !Minecraft.player.onGround
                        || Minecraft.player.isJumping()) {
                        noslowPercent = 0.9925F;
                     }
                  } else {
                     boolean stop = Minecraft.player.isInWater()
                        || Minecraft.player.isInLava()
                        || Minecraft.player.isInWeb
                        || Minecraft.player.capabilities.isFlying
                        || Minecraft.player.getTicksElytraFlying() > 1
                        || !MoveMeHelp.isMoving();
                     if (!Minecraft.player.isHandActive()
                        || Minecraft.player.getItemInUseMaxCount() <= 3
                        || stop
                        || !Minecraft.player.onGround
                        || Minecraft.player.isJumping()) {
                        cancelSlow = true;
                     }
                  }
            }
         } else {
            noslowPercent = 0.4F;
            max2 /= 2.0;
         }
      }

      if (Minecraft.player.isHandActive() && move.motion().yCoord <= 0.0 && !cancelSlow) {
         if (max2
            > (
               max = Math.max(
                  0.043, oldSpeed + (double)n8 * 0.5 + 0.005F + (move.motion().yCoord != 0.0 && Math.abs(move.motion().yCoord) < 0.08 ? 0.055 : 0.0)
               )
            )) {
            noSlowTicks++;
         } else {
            noSlowTicks = Math.max(noSlowTicks - 1, 0);
         }
      } else {
         noSlowTicks = 0;
      }

      if (noSlowTicks > 0) {
         max2 = noSlowTicks > 0 ? max * (double)noslowPercent : Math.max(0.25, max2) - (counter++ % 2 == 0 ? 0.001 : 0.002);
      } else {
         max2 = Math.max(0.2499, max2) - (counter++ % 2 == 0 ? 0.001 : 0.002);
         max2 *= (double)noslowPercent;
      }

      contextFriction = (double)n6;
      if (!toGround && !fromGround) {
         needSwap = true;
      } else {
         prevSprint = false;
      }

      if (!fromGround && !toGround) {
         Strafe.needSprintState = !Minecraft.player.serverSprintState;
         TargetStrafe.needSprintState = !Minecraft.player.serverSprintState;
      }

      if (toGround && fromGround) {
         Strafe.needSprintState = false;
         TargetStrafe.needSprintState = false;
      }

      return max2 + (ely ? speed : 0.0);
   }

   public static void postMove(double horizontal) {
      oldSpeed = horizontal * contextFriction;
   }

   public static float getAIMoveSpeed(EntityPlayer contextPlayer) {
      boolean prevSprinting = contextPlayer.isSprinting();
      contextPlayer.setSprinting(false);
      float speed = contextPlayer.getAIMoveSpeed() * 1.3F;
      contextPlayer.setSprinting(prevSprinting);
      return speed;
   }

   public static void actionEvent(EventAction eventAction) {
      if (needSwap) {
         eventAction.setSprintState(!Minecraft.player.serverSprintState);
         needSwap = false;
      }

      if (Minecraft.player.isJumping() && Minecraft.player.onGround) {
         eventAction.setSprintState(true);
      }
   }

   private static float getFrictionFactor(EntityPlayer contextPlayer, EventMove2 move) {
      BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain(
         move.from().xCoord, move.getAABBFrom().minY - 1.0, move.from().zCoord
      );
      return contextPlayer.world.getBlockState(blockpos$pooledmutableblockpos).getBlock().slipperiness * 0.91F;
   }
}
