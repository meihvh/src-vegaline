package ru.govno.client.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventMove2;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventSendPacket;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Command.impl.Clip;
import ru.govno.client.utils.Math.BlockUtils;
import ru.govno.client.utils.Math.MathHelper;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;

public class Phase extends Module {
   TimerHelper timer = new TimerHelper();
   boolean flagg;
   ModeSettings Mode;
   int tickNoFalling = 0;
   int ticksCollideH = 0;
   int pushTimerTicksOut;

   public Phase() {
      super("Phase", 0, Module.Category.PLAYER);
      this.settings
         .add(
            this.Mode = new ModeSettings(
               "Mode", "MatrixSkip", this, new String[]{"MatrixNotOpaque", "MatrixSkip", "PacketNitro", "UndergroundFly10T", "Matrix7"}
            )
         );
      this.setDemand(1, 1);
   }

   @EventTarget
   public void onPacket(EventSendPacket event) {
      if (this.Mode.getMode().equalsIgnoreCase("MatrixNotOpaque")
         && Minecraft.player != null
         && mc.world != null
         && !Minecraft.player.isDead
         && this.actived
         && event.getPacket() instanceof CPacketConfirmTeleport) {
         this.flagg = true;
      }

      if (this.Mode.getMode().equalsIgnoreCase("MatrixSkip") && event.getPacket() instanceof SPacketPlayerPosLook look) {
         float x = (float)(look.x - Minecraft.player.posX);
         float y = (float)(look.y - Minecraft.player.posY);
         float z = (float)(look.z - Minecraft.player.posZ);
         float distance = MathHelper.sqrt((double)(x * x + y * y + z * z));
         if (distance <= 8.0F) {
            event.setCancelled(true);
            Minecraft.player.connection.sendPacket(new CPacketPlayer.PositionRotation(look.x, look.y, look.z, look.getYaw(), look.getPitch(), true));
         }
      }
   }

   @EventTarget
   public void onPUpdate(EventPlayerMotionUpdate event) {
      if (this.Mode.getMode().equalsIgnoreCase("PacketNitro") && this.actived) {
         boolean opaue = !mc.world
            .getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ))
            .getMaterial()
            .isReplaceable();
         if (!Minecraft.player.isCollidedHorizontally) {
            this.timer.reset();
         }

         if (this.timer.hasReached(150.0) && !opaue) {
            double dst = 0.65;
            double xe = Minecraft.player.posX
               - (double)net.minecraft.util.math.MathHelper.sin(MoveMeHelp.moveYaw(Minecraft.player.rotationYaw) * (float) (Math.PI / 180.0)) * dst;
            double ye = Minecraft.player.posY;
            double ze = Minecraft.player.posZ
               + (double)net.minecraft.util.math.MathHelper.cos(MoveMeHelp.moveYaw(Minecraft.player.rotationYaw) * (float) (Math.PI / 180.0)) * dst;
            Minecraft.player.forceUpdatePlayerServerPosition(xe, ye, ze, Minecraft.player.rotationYaw, Minecraft.player.rotationPitch, true);
            Minecraft.player.connection.sendPacket(new CPacketPlayer.Position(xe, ye, ze, true));
            Minecraft.player.connection.sendPacket(new CPacketPlayer.Position(xe, ye + 0.1, ze, true));
            Minecraft.player.connection.sendPacket(new CPacketPlayer.Position(xe, ye, ze, true));
            Minecraft.player.connection.sendPacket(new CPacketPlayer.Position(xe, ye, ze, true));
            MoveMeHelp.setSpeed(0.0);
            MoveMeHelp.setCuttingSpeed(0.0);
            this.timer.reset();
         }
      }
   }

   @EventTarget
   public void onMove(EventMove2 move) {
      if (this.isActived()
         && this.Mode.getMode().equalsIgnoreCase("Matrix7")
         && Minecraft.player != null
         && Minecraft.player.onGround
         && !Minecraft.player.isJumping()
         && !Minecraft.player.isSneaking()) {
         boolean isInCollide = mc.world != null && !mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox).isEmpty();
         if (isInCollide && MoveMeHelp.isMoving()) {
            move.setIgnoreHorizontalCollision();
            Minecraft.player.onGround = false;
            if (new BlockPos(move.to()).distanceSq(new BlockPos(move.from())) > 0.0) {
               mc.timer.tempSpeed = 1.0;
               if (this.pushTimerTicksOut < 3) {
                  this.pushTimerTicksOut = 3;
               }

               MoveMeHelp.setSpeed(0.1F);
            } else {
               mc.timer.tempSpeed = 2.0;
               MoveMeHelp.setSpeed(0.25);
            }
         }
      }
   }

   @Override
   public void onUpdate() {
      if (this.Mode.getMode().equalsIgnoreCase("Matrix7") && Minecraft.player != null && !Minecraft.player.isJumping()) {
         boolean isInCollide = mc.world != null && !mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox).isEmpty();
         if (Minecraft.player.fallDistance == 0.0F) {
            this.tickNoFalling++;
         } else {
            this.tickNoFalling = 0;
         }

         if (Minecraft.player.isCollidedHorizontally) {
            this.ticksCollideH++;
         } else if (!isInCollide) {
            this.ticksCollideH = 0;
         }

         if (Minecraft.player.isCollidedHorizontally && MoveMeHelp.isMoving() && this.ticksCollideH > 3) {
            float moveYaw = MoveMeHelp.moveYaw(Minecraft.player.rotationYaw);
            float oldRotationYaw = Minecraft.player.rotationYaw;
            Minecraft.player.rotationYaw = moveYaw;
            if (!isInCollide) {
               Clip.goClip(0.0, isInCollide ? 0.01F : 0.05F, false);
               MoveMeHelp.setSpeed(0.2F);
            }

            Minecraft.player.rotationYaw = oldRotationYaw;
            if (!isInCollide) {
               this.pushTimerTicksOut = 10;
            }
         } else if (Minecraft.player.isSneaking() && !MoveMeHelp.isMoving() && Minecraft.player.onGround && this.tickNoFalling > 6) {
            Clip.goClip(-0.02F, 0.0, false);
            Minecraft.player.motionY = 0.01F;
            this.pushTimerTicksOut = 5;
         }

         if (MoveMeHelp.isMoving() || Minecraft.player.isSneaking() && !MoveMeHelp.isMoving()) {
            if (this.pushTimerTicksOut > 0) {
               float speedTime = this.pushTimerTicksOut == 1 ? 0.5F : 2.0F;
               mc.timer.tempSpeed = (double)speedTime;
               Timer.forceTimer(speedTime);
               this.pushTimerTicksOut--;
               if (this.pushTimerTicksOut <= 0 && MoveMeHelp.isMoving()) {
                  MoveMeHelp.setTempSpeedPost(-0.2F);
                  Minecraft.player.onGround = false;
                  Minecraft.player.motionY = 0.0391F;
               }
            }
         } else {
            this.pushTimerTicksOut = 0;
         }
      }

      if (this.Mode.getMode().equalsIgnoreCase("UndergroundFly10T")
         && Minecraft.player != null
         && mc.world != null
         && (Minecraft.player.isJumping() || Minecraft.player.isSneaking() || MoveMeHelp.isMoving())) {
         int ticksDelay = 1;
         if (ticksDelay <= 1 || Minecraft.player.ticksExisted % ticksDelay == 0) {
            float moveYawx = MoveMeHelp.moveYaw(Minecraft.player.rotationYaw);
            float movePitch = (float)(Minecraft.player.isJumping() ? -1 : (Minecraft.player.isSneaking() ? 1 : 0)) * (MoveMeHelp.isMoving() ? 45.0F : 90.0F);
            Vec3d playerPos = Minecraft.player.getPositionVector();
            Vec3d cut1mMoveVec = Entity.getVectorForRotation(movePitch, moveYawx);
            Vec3d lastOffset = null;
            int speedCrate = 2;
            int speedCrateMax = 5;

            for (int crateNum = 0; crateNum <= speedCrateMax; crateNum++) {
               Vec3d offset = playerPos.add(cut1mMoveVec.scale((double)(11.0F * (float)crateNum)));
               BlockPos pos = new BlockPos(offset);
               BlockPos posUp = pos.up();
               if (!BlockUtils.blockMaterialIsCurrent(pos) && !BlockUtils.blockMaterialIsCurrent(posUp)) {
                  speedCrate++;
               }
            }

            for (int crateNumx = 0; crateNumx < speedCrate; crateNumx++) {
               Vec3d offset = playerPos.add(cut1mMoveVec.scale((double)(11.0F * (float)crateNumx)));
               lastOffset = offset;
               mc.getConnection().sendPacket(new CPacketPlayer.Position(offset.xCoord, offset.yCoord, offset.zCoord, false));
            }

            if (lastOffset != null) {
               Minecraft.player.setPositionAndUpdate(lastOffset.xCoord, lastOffset.yCoord, lastOffset.zCoord);
               Minecraft.player.setVelocity(0.0, 0.0, 0.0);
               mc.timer.tempSpeed = 0.5;
               Timer.forceTimer(0.5F);
            }
         }
      }

      if (this.Mode.getMode().equalsIgnoreCase("MatrixNotOpaque")) {
         for (int i = 0; i < 2; i++) {
            if (Minecraft.player.isCollidedHorizontally) {
               if (mc.gameSettings.keyBindForward.pressed) {
                  Minecraft.player
                     .connection
                     .sendPacket(
                        new CPacketPlayer.Position(
                           Minecraft.player.posX
                              - (double)(
                                 net.minecraft.util.math.MathHelper.sin(net.minecraft.util.math.MathHelper.toRadians(Minecraft.player.rotationYaw)) * 0.25F
                              ),
                           Minecraft.player.posY,
                           Minecraft.player.posZ
                              + (double)(
                                 net.minecraft.util.math.MathHelper.cos(net.minecraft.util.math.MathHelper.toRadians(Minecraft.player.rotationYaw)) * 0.25F
                              ),
                           true
                        )
                     );
                  Minecraft.player
                     .connection
                     .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + 15000.0, Minecraft.player.posZ, true));
               }

               Minecraft.player
                  .connection
                  .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + 9000.0, Minecraft.player.posZ, true));
               Minecraft.player
                  .connection
                  .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY - 20.0, Minecraft.player.posZ, false));
            }
         }
      }

      if (this.Mode.getMode().equalsIgnoreCase("MatrixSkip")) {
         if ((double)Minecraft.player.fallDistance > 0.08
            && Minecraft.player.posY != (double)((int)Minecraft.player.posY)
            && Minecraft.player.posY != (double)((float)((int)Minecraft.player.posY) + 0.5F)) {
            this.toggle(false);
         }

         if (Minecraft.player.onGround) {
            this.flagg = true;
            Minecraft.player
               .setPosition(
                  Minecraft.player.posX
                     - (double)(net.minecraft.util.math.MathHelper.sin(net.minecraft.util.math.MathHelper.toRadians(Minecraft.player.rotationYaw)) * 1.999F),
                  Minecraft.player.posY - 1.0E-5,
                  Minecraft.player.posZ
                     + (double)(net.minecraft.util.math.MathHelper.cos(net.minecraft.util.math.MathHelper.toRadians(Minecraft.player.rotationYaw)) * 1.999F)
               );
            Minecraft.player.motionY = -0.0476;
            Entity.motiony = 0.01F;
         }
      }
   }
}
