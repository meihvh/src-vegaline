package ru.govno.client.module.modules;

import java.util.LinkedList;
import java.util.Random;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CPacketConfirmTransaction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventMove2;
import ru.govno.client.event.events.EventReceivePacket;
import ru.govno.client.event.events.EventSendPacket;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;

public class Velocity extends Module {
   public static Velocity get;
   Random RANDOM = new Random(1231231234L);
   public ModeSettings KnockType;
   public ModeSettings GrimKnock;
   public BoolSettings OnKnockBack;
   public BoolSettings NoFishingHook;
   public BoolSettings ReduceStopSprint;
   public BoolSettings NoBlockPush;
   public BoolSettings NoWaterPush;
   public BoolSettings NoEntityPush;
   public FloatSettings MaxReducement;
   public FloatSettings ReduceMS;
   public FloatSettings KnockFlagPauseMS;
   public FloatSettings IgnoreKnockChance;
   public FloatSettings SneakTicks;
   public static boolean pass;
   static int cancelTransactionCounter = -5;
   TimerHelper lastPauseTime = new TimerHelper();
   private static float[] motion;
   private static boolean moveBoost;
   private static boolean motionChange;
   public static Vec3d lastVelocityMotion = Vec3d.ZERO;
   private boolean sendGrim;
   private boolean reduceStatus;
   private boolean sneakStatus;
   private final TimerHelper reduceTimer = new TimerHelper();
   public int sneakTicks;
   public static boolean stopGrim = false;
   private final LinkedList<Runnable> cancelRunnables = new LinkedList<>();

   public Velocity() {
      super("Velocity", 0, Module.Category.COMBAT);
      this.settings.add(this.OnKnockBack = new BoolSettings("OnKnockBack", true, this));
      this.settings
         .add(
            this.KnockType = new ModeSettings(
               "KnockType",
               "Cancel",
               this,
               new String[]{"Cancel", "Stationary", "MoveBoost", "StrafeOrCancel", "GrimAc", "AAC5.2.0", "Reduce", "Sneaking"},
               () -> this.OnKnockBack.getBool()
            )
         );
      this.settings
         .add(
            this.GrimKnock = new ModeSettings(
               "GrimKnock",
               "Cancel",
               this,
               new String[]{"Cancel", "Cancel&BP", "AltCancel", "AltCancel&BP"},
               () -> this.OnKnockBack.getBool() && this.KnockType.currentMode.equalsIgnoreCase("GrimAC")
            )
         );
      this.settings
         .add(
            this.MaxReducement = new FloatSettings(
               "MaxReducement", 0.7F, 1.0F, 0.0F, this, () -> this.OnKnockBack.getBool() && this.KnockType.currentMode.equalsIgnoreCase("Reduce")
            )
         );
      this.settings
         .add(
            this.ReduceMS = new FloatSettings(
               "ReduceMS", 800.0F, 1000.0F, 50.0F, this, () -> this.OnKnockBack.getBool() && this.KnockType.currentMode.equalsIgnoreCase("Reduce")
            )
         );
      this.settings
         .add(
            this.ReduceStopSprint = new BoolSettings(
               "ReduceStopSprint", true, this, () -> this.OnKnockBack.getBool() && this.KnockType.currentMode.equalsIgnoreCase("Reduce")
            )
         );
      this.settings
         .add(
            this.KnockFlagPauseMS = new FloatSettings(
               "KnockFlagPauseMS", 3000.0F, 8000.0F, 0.0F, this, () -> this.OnKnockBack.getBool() && this.KnockType.currentMode.equalsIgnoreCase("GrimAc")
            )
         );
      this.settings
         .add(
            this.IgnoreKnockChance = new FloatSettings(
               "IgnoreKnockChance", 80.0F, 100.0F, 0.0F, this, () -> this.OnKnockBack.getBool() && this.KnockType.currentMode.equalsIgnoreCase("GrimAc")
            )
         );
      this.settings
         .add(
            this.SneakTicks = new FloatSettings(
               "SneakTicks", 2.0F, 10.0F, 1.0F, this, () -> this.OnKnockBack.getBool() && this.KnockType.currentMode.equalsIgnoreCase("Sneaking")
            )
         );
      this.settings.add(this.NoFishingHook = new BoolSettings("NoFishingHook", true, this));
      this.settings.add(this.NoBlockPush = new BoolSettings("NoBlockPush", true, this));
      this.settings.add(this.NoWaterPush = new BoolSettings("NoWaterPush", true, this));
      this.settings.add(this.NoEntityPush = new BoolSettings("NoEntityPush", true, this));
      this.setDemand(0, 1);
      get = this;
   }

   private int cancelPacketsGrim() {
      return 8;
   }

   private static double getVelocitySpeed(Vec3d velocityMotion) {
      double xMotion = velocityMotion.xCoord;
      double zMotion = velocityMotion.zCoord;
      return Math.sqrt(xMotion * xMotion + zMotion * zMotion);
   }

   private static void addSpeedAtVelocity(Entity entityFor, Vec3d velocityMotion, boolean[] abuses) {
      if (abuses[1]) {
         motion = new float[]{(float)velocityMotion.xCoord / 9.0F, (float)velocityMotion.zCoord / 9.0F};
         moveBoost = getVelocitySpeed(velocityMotion) / 8.0 <= 0.23;
         motionChange = false;
      } else {
         if (abuses[0]) {
            double speedAdd = getVelocitySpeed(velocityMotion);
            float moveYaw = MoveMeHelp.moveYaw(entityFor.rotationYaw);
            double sin = (double)(-MathHelper.sin(MathHelper.toRadians(moveYaw))) * speedAdd;
            double cos = (double)MathHelper.cos(MathHelper.toRadians(moveYaw)) * speedAdd;
            motion = new float[]{(float)sin, (float)cos};
            moveBoost = Math.hypot((double)motion[0], (double)motion[1]) * 20.0 > MoveMeHelp.getCuttingSpeed();
            motionChange = true;
         }

         if (abuses[3]) {
            double speedAdd = getVelocitySpeed(velocityMotion);
            float moveYaw = MoveMeHelp.moveYaw(entityFor.rotationYaw);
            double sin = (double)(-MathHelper.sin(MathHelper.toRadians(moveYaw)));
            double cos = (double)MathHelper.cos(MathHelper.toRadians(moveYaw));
            motion = new float[]{(float)(sin * speedAdd), (float)(cos * speedAdd)};
            moveBoost = speedAdd > 0.04F;
            motionChange = true;
         }
      }
   }

   private static void sendAAC520_BP() {
      mc.getConnection().sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Double.MAX_VALUE, Minecraft.player.posZ, true));
   }

   private static void sendGrim_BP() {
      BlockPos pos = new BlockPos(Minecraft.player.getPositionVector());
      mc.getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, EnumFacing.UP));
      mc.getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, EnumFacing.UP));
   }

   @EventTarget
   public void onMove(EventMove2 move) {
      if (moveBoost) {
         if (!motionChange) {
            move.motion().xCoord = (double)motion[0];
            move.motion().zCoord = (double)motion[1];
            Entity.motionx = move.motion().xCoord;
            Entity.motionz = move.motion().zCoord;
            if (!MoveMeHelp.moveKeysPressed()) {
               Minecraft.player.addVelocity(Entity.motionx / 2.93, 0.0, Entity.motionz / 2.93);
            }

            moveBoost = false;
            return;
         }

         float speed = (float)getVelocitySpeed(new Vec3d((double)motion[0], 0.0, (double)motion[1]));
         float yaw = (float)Math.toDegrees(MathHelper.atan2((double)motion[1], (double)motion[0]));
         yaw = yaw < 0.0F ? yaw + 360.0F : yaw;
         float xB = -MathHelper.sin(MathHelper.toRadians(yaw - 90.0F)) * speed;
         float zB = MathHelper.cos(MathHelper.toRadians(yaw - 90.0F)) * speed;
         Minecraft.player.setVelocity((double)xB, Minecraft.player.motionY, (double)zB);
         motionChange = false;
         move.motion().xCoord = Entity.Getmotionx;
         move.motion().zCoord = Entity.Getmotionz;
         moveBoost = false;
      }
   }

   public static boolean[] canAbuseVelocity(Entity entityFor, Vec3d velocityMotion) {
      boolean isSelf = entityFor instanceof EntityPlayerSP;
      boolean canMoveBoostAbuse = false;
      boolean canStationaryAbuse = false;
      boolean canAAC520Abuse = false;
      boolean canGrimNewCancel = false;
      boolean canStrafeOrCancelCancel = false;
      if (isSelf) {
         String mode = get.KnockType.getMode();
         boolean abuseMoveBoost = mode.equalsIgnoreCase("MoveBoost");
         boolean abuseMoveStationary = mode.equalsIgnoreCase("Stationary");
         boolean doCancelTrans = mode.equalsIgnoreCase("GrimAc");
         boolean abuseAAC520 = mode.equalsIgnoreCase("AAC5.2.0");
         boolean abuseStrafe = mode.equalsIgnoreCase("StrafeOrCancel");
         lastVelocityMotion = velocityMotion;
         canMoveBoostAbuse = abuseMoveBoost
            && getVelocitySpeed(velocityMotion) > 0.026
            && MoveMeHelp.isMoving()
            && MoveMeHelp.getDirDiffOfMotions(velocityMotion.xCoord, velocityMotion.zCoord) < 20.0;
         canStationaryAbuse = abuseMoveStationary && MoveMeHelp.getSpeed() < 0.146;
         canAAC520Abuse = abuseAAC520 && !Minecraft.player.isCollidedHorizontally;
         canStrafeOrCancelCancel = abuseStrafe && MoveMeHelp.isMoving();
      }

      return new boolean[]{isSelf && canMoveBoostAbuse, isSelf && canStationaryAbuse, isSelf && canAAC520Abuse, isSelf && canStrafeOrCancelCancel};
   }

   public static void abuseVelocity(Entity entityFor, Vec3d velocityMotion) {
      boolean[] abuse = canAbuseVelocity(entityFor, velocityMotion);
      if (abuse[0] || abuse[1] || abuse[3]) {
         addSpeedAtVelocity(entityFor, velocityMotion, abuse);
      }

      if (abuse[2]) {
         sendAAC520_BP();
      }
   }

   @Override
   public void onUpdate() {
      if (this.OnKnockBack.getBool()) {
         pass = mc.world.getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ)).getBlock() instanceof BlockBasePressurePlate
            || isGrim()
            || this.KnockType.currentMode.equalsIgnoreCase("Reduce")
            || this.KnockType.currentMode.equalsIgnoreCase("Sneaking");
         if (this.KnockType.currentMode.equalsIgnoreCase("Sneaking")) {
            if (this.sneakTicks > 0) {
               mc.gameSettings.keyBindSneak.pressed = true;
               this.sneakTicks--;
               if (this.sneakTicks == 0) {
                  mc.gameSettings.keyBindSneak.pressed = this.sneakStatus;
               }
            } else {
               this.sneakStatus = Minecraft.player.isSneaking();
            }
         }
      }

      if (this.NoEntityPush.getBool()) {
         Minecraft.player.entityCollisionReduction = 1.0F;
      }

      if (isGrim()) {
         cancelTransactionCounter--;
         if (stopGrim && this.lastPauseTime.hasReached((double)this.KnockFlagPauseMS.getFloat())) {
            stopGrim = false;
         }

         if (this.sendGrim) {
            sendGrim_BP();
            this.sendGrim = false;
         }

         if (cancelTransactionCounter <= 0 && !this.cancelRunnables.isEmpty()) {
            for (int i = 0; i < this.cancelRunnables.size(); i++) {
               this.cancelRunnables.removeLast().run();
            }
         }
      } else {
         if (cancelTransactionCounter != 0) {
            cancelTransactionCounter = 0;
         }

         if (stopGrim) {
            stopGrim = false;
         }

         if (this.sendGrim) {
            this.sendGrim = false;
         }

         if (this.OnKnockBack.getBool() && this.KnockType.currentMode.equalsIgnoreCase("Reduce")) {
            if (!this.reduceStatus) {
               this.reduceTimer.reset();
            } else if (this.reduceTimer.hasReached((double)(this.ReduceMS.getFloat() + 50.0F))) {
               this.reduceStatus = false;
            }

            if (this.reduceStatus) {
               float reduce = this.MaxReducement.getFloat();
               Minecraft.player.multiplyMotionXZ(reduce);
               if (this.ReduceStopSprint.getBool()) {
                  Minecraft.player.setFlag(3, false);
               }
            }
         }
      }
   }

   static boolean isGrim() {
      return get.actived && get.OnKnockBack.getBool() && get.KnockType.currentMode.equalsIgnoreCase("GrimAc");
   }

   @EventTarget
   public void onReceivePackets(EventReceivePacket eventPacket) {
      if (eventPacket.getPacket() instanceof SPacketPlayerPosLook look
         && cancelTransactionCounter > 0
         && Minecraft.player.getDistance(look.getX(), look.getY(), look.getZ()) < 1.0) {
         stopGrim = this.KnockFlagPauseMS.getInt() >= 20;
         this.lastPauseTime.reset();
      }

      if (this.OnKnockBack.getBool()
         && this.KnockType.currentMode.equalsIgnoreCase("Sneaking")
         && eventPacket.getPacket() instanceof SPacketEntityVelocity velocity
         && Minecraft.player != null
         && velocity.getEntityID() == Minecraft.player.getEntityId()) {
         double veloSpeed = getVelocitySpeed(new Vec3d((double)velocity.motionX / 8000.0, (double)velocity.motionY / 8000.0, (double)velocity.motionZ / 8000.0));
         if (veloSpeed < 3.6 && veloSpeed > 0.06) {
            this.sneakTicks = this.SneakTicks.getInt() + 1;
         }
      }

      if (!stopGrim
         && eventPacket.getPacket() instanceof SPacketEntityVelocity velocityx
         && Minecraft.player != null
         && velocityx.getEntityID() == Minecraft.player.getEntityId()
         && isGrim()
         && mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.getEntityBoundingBoxCLCompact().addExpandXZ(0.15)).isEmpty()) {
         double velocitySpeedSumSqrt = getVelocitySpeed(
            new Vec3d((double)velocityx.motionX / 8000.0, (double)velocityx.motionY / 8000.0, (double)velocityx.motionZ / 8000.0)
         );
         boolean invalidVelocity = velocitySpeedSumSqrt > 3.6 && velocitySpeedSumSqrt < 0.1;
         if (cancelTransactionCounter > 0 || this.RANDOM.nextInt(100) > this.IgnoreKnockChance.getInt() || invalidVelocity) {
            stopGrim = true;
            return;
         }

         cancelTransactionCounter = this.cancelPacketsGrim();
         this.sendGrim = this.GrimKnock.getMode().contains("&BP");
         eventPacket.cancel();
      }

      if (this.OnKnockBack.getBool()
         && this.KnockType.currentMode.equalsIgnoreCase("Reduce")
         && this.MaxReducement.getFloat() != 0.0F
         && eventPacket.getPacket() instanceof SPacketEntityVelocity velocityx
         && Minecraft.player != null
         && velocityx.getEntityID() == Minecraft.player.getEntityId()) {
         double veloSpeed = getVelocitySpeed(
            new Vec3d((double)velocityx.motionX / 8000.0, (double)velocityx.motionY / 8000.0, (double)velocityx.motionZ / 8000.0)
         );
         if (veloSpeed < 3.6 && veloSpeed > 0.06) {
            this.reduceStatus = true;
         }
      }
   }

   @EventTarget
   public void onSendPacket(EventSendPacket eventPacket) {
      if (!stopGrim
         && eventPacket.getPacket() instanceof CPacketConfirmTransaction packetConfirmTransaction
         && cancelTransactionCounter > 0
         && isGrim()
         && this.GrimKnock.getMode().contains("Alt")) {
         this.cancelRunnables.add(() -> mc.getConnection().sendPacket(packetConfirmTransaction));
         eventPacket.cancel();
      }
   }

   @Override
   public void onToggled(boolean actived) {
      cancelTransactionCounter = 0;
      stopGrim = false;
      super.onToggled(actived);
   }
}
