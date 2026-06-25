package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemFirework;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Keyboard;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventElytraVector;
import ru.govno.client.event.events.EventMovementInput;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventReceivePacket;
import ru.govno.client.event.events.EventSprintBlock;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;

public class ElytraBoost extends Module {
   public static ElytraBoost get;
   public ModeSettings Mode;
   public FloatSettings SpeedF;
   public FloatSettings SpeedXZ;
   public FloatSettings BoostValue;
   public FloatSettings MotionXZ;
   public FloatSettings MotionY;
   public FloatSettings MotionDirectMax;
   public FloatSettings MotionRunupDirect;
   public BoolSettings NoTimerDefunction;
   public BoolSettings StrafeDirs;
   public BoolSettings StaticYMotions;
   public BoolSettings NormalizeDirection;
   public BoolSettings GrimStrafed;
   public BoolSettings CustomSpeed;
   public BoolSettings MoveSideRotate;
   private int pauseTicksOutFlagged;
   TimerHelper timer = new TimerHelper();
   TimerHelper timer2 = new TimerHelper();
   float moveElytraYaw;
   float moveElytraPitch;
   private boolean targetStrafeIsCalling;
   private double calledXM;
   private double calledZM;
   public static Item oldSlot = null;
   String strafeMode = null;
   boolean strafeActived = false;
   public static boolean hitTick = false;
   double curPosY;
   TimerHelper wait = new TimerHelper();
   public static double flSpeed;
   int boostTicks;

   public ElytraBoost() {
      super("ElytraBoost", 0, Module.Category.MOVEMENT);
      this.settings
         .add(
            this.Mode = new ModeSettings(
               "Mode",
               "MatrixFly",
               this,
               new String[]{
                  "MatrixFly",
                  "MatrixFly2",
                  "MatrixFly3",
                  "MatrixSpeed",
                  "MatrixSpeed2",
                  "MatrixSpeed3",
                  "NcpFly",
                  "Vanilla",
                  "StrafeSync",
                  "Firework",
                  "VulcanSpeed",
                  "VulcanPulse",
                  "NcpMiBoost"
               }
            )
         );
      this.settings
         .add(
            this.CustomSpeed = new BoolSettings(
               "CustomSpeed",
               false,
               this,
               () -> this.Mode.currentMode.equalsIgnoreCase("Firework")
                     && !this.GrimStrafed.getBool()
                     && (this.StrafeDirs.getBool() || this.StaticYMotions.getBool())
            )
         );
      this.settings
         .add(
            this.MotionXZ = new FloatSettings(
               "MotionXZ",
               1.0F,
               5.0F,
               1.0F,
               this,
               () -> this.Mode.currentMode.equalsIgnoreCase("Firework")
                     && this.CustomSpeed.getBool()
                     && !this.GrimStrafed.getBool()
                     && this.StrafeDirs.getBool()
            )
         );
      this.settings
         .add(
            this.MotionY = new FloatSettings(
               "MotionY",
               1.0F,
               5.0F,
               0.5F,
               this,
               () -> this.Mode.currentMode.equalsIgnoreCase("Firework")
                     && this.CustomSpeed.getBool()
                     && !this.GrimStrafed.getBool()
                     && this.StaticYMotions.getBool()
            )
         );
      this.settings
         .add(
            this.SpeedF = new FloatSettings(
               "Speed",
               3.0F,
               10.0F,
               1.0F,
               this,
               () -> !this.Mode.currentMode.equalsIgnoreCase("Vanilla")
                     && !this.Mode.currentMode.equalsIgnoreCase("Firework")
                     && !this.Mode.currentMode.equalsIgnoreCase("MatrixSpeed2")
                     && !this.Mode.currentMode.equalsIgnoreCase("MatrixFly2")
                     && !this.Mode.currentMode.equalsIgnoreCase("MatrixFly3")
                     && !this.Mode.currentMode.equalsIgnoreCase("MatrixSpeed3")
                     && !this.Mode.currentMode.equalsIgnoreCase("VulcanSpeed")
                     && !this.Mode.currentMode.equalsIgnoreCase("StrafeSync")
                     && !this.Mode.currentMode.equalsIgnoreCase("VulcanPulse")
                     && !this.Mode.getMode().equalsIgnoreCase("NcpMiBoost")
            )
         );
      this.settings.add(this.SpeedXZ = new FloatSettings("SpeedXZ", 1.0F, 3.0F, 0.25F, this, () -> this.Mode.currentMode.equalsIgnoreCase("MatrixFly3")));
      this.settings
         .add(this.NoTimerDefunction = new BoolSettings("NoTimerDefunction", false, this, () -> this.Mode.currentMode.equalsIgnoreCase("MatrixFly2")));
      this.settings
         .add(
            this.GrimStrafed = new BoolSettings(
               "GrimStrafed",
               false,
               this,
               () -> this.Mode.currentMode.equalsIgnoreCase("Firework") && !this.StrafeDirs.getBool() && !this.StaticYMotions.getBool()
            )
         );
      this.settings
         .add(
            this.BoostValue = new FloatSettings(
               "BoostValue",
               0.1F,
               2.0F,
               0.0F,
               this,
               () -> this.Mode.currentMode.equalsIgnoreCase("Firework") && !this.StrafeDirs.getBool() && !this.StaticYMotions.getBool()
            )
         );
      this.settings
         .add(
            this.StrafeDirs = new BoolSettings(
               "StrafeDirs", false, this, () -> this.Mode.currentMode.equalsIgnoreCase("Firework") && !this.GrimStrafed.getBool()
            )
         );
      this.settings
         .add(
            this.MoveSideRotate = new BoolSettings(
               "MoveSideRotate",
               false,
               this,
               () -> this.Mode.currentMode.equalsIgnoreCase("Firework") && (this.GrimStrafed.getBool() || this.StrafeDirs.getBool())
            )
         );
      this.settings
         .add(
            this.StaticYMotions = new BoolSettings(
               "StaticYMotions", true, this, () -> this.Mode.currentMode.equalsIgnoreCase("Firework") && !this.GrimStrafed.getBool()
            )
         );
      this.settings
         .add(
            this.NormalizeDirection = new BoolSettings(
               "NormalizeDirection",
               true,
               this,
               () -> this.Mode.currentMode.equalsIgnoreCase("Firework")
                     && !this.GrimStrafed.getBool()
                     && this.StrafeDirs.getBool()
                     && this.StaticYMotions.getBool()
            )
         );
      this.settings
         .add(
            this.MotionDirectMax = new FloatSettings(
               "MotionDirectMax",
               1.9F,
               5.0F,
               0.5F,
               this,
               () -> this.Mode.currentMode.equalsIgnoreCase("Firework")
                     && this.CustomSpeed.getBool()
                     && !this.GrimStrafed.getBool()
                     && this.StrafeDirs.getBool()
                     && this.StaticYMotions.getBool()
                     && this.NormalizeDirection.getBool()
            )
         );
      this.settings
         .add(
            this.MotionRunupDirect = new FloatSettings(
               "MotionRunupDirect",
               0.2F,
               1.0F,
               0.05F,
               this,
               () -> this.Mode.currentMode.equalsIgnoreCase("Firework")
                     && this.CustomSpeed.getBool()
                     && !this.GrimStrafed.getBool()
                     && this.StrafeDirs.getBool()
                     && this.StaticYMotions.getBool()
                     && this.NormalizeDirection.getBool()
            )
         );
      this.setDemand(0, 0);
      get = this;
   }

   public static void eq() {
      if (!(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra)) {
         oldSlot = Minecraft.player.inventory.armorItemInSlot(2).getItem();
      }

      if (!(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra)) {
         if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemAir) {
            mc.playerController.windowClick(0, getItemElytra(), 1, ClickType.QUICK_MOVE, Minecraft.player);
         } else if (!(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra)) {
            mc.playerController.windowClick(0, 6, 1, ClickType.QUICK_MOVE, Minecraft.player);
            mc.playerController.windowClick(0, getItemElytra(), 1, ClickType.QUICK_MOVE, Minecraft.player);
         }
      }
   }

   public static void deq() {
      if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra) {
         mc.playerController.windowClick(0, 6, 1, ClickType.QUICK_MOVE, Minecraft.player);
         mc.playerController.windowClick(0, getOldItem(), 1, ClickType.QUICK_MOVE, Minecraft.player);
         Speed.get.sleepTicks = 5;
      }
   }

   boolean canFly() {
      return Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra;
   }

   boolean putFirework() {
      if (Minecraft.player.getHeldItemOffhand().getItem() instanceof ItemFirework
         && !(Minecraft.player.getHeldItemMainhand().getItem() instanceof ItemFirework)) {
         Minecraft.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
         return true;
      } else {
         int slot = -1;
         int handSlot = Minecraft.player.inventory.currentItem;

         for (int i = 44; i > 0; i--) {
            ItemStack itemStack = Minecraft.player.inventoryContainer.getSlot(i).getStack();
            if (itemStack.getItem() instanceof ItemFirework) {
               slot = i;
            }
         }

         if (slot == -1) {
            return false;
         } else {
            if (Minecraft.player.getHeldItemMainhand().getItem() instanceof ItemFirework) {
               Minecraft.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
            } else if (GETGOVNO(Items.FIREWORKS) != -1) {
               slot = GETGOVNO(Items.FIREWORKS);
               if (Minecraft.player.isHandActive() && Minecraft.player.getActiveHand() == EnumHand.MAIN_HAND) {
                  mc.playerController.windowClick(0, 45, slot, ClickType.SWAP, Minecraft.player);
                  mc.playerController.syncCurrentPlayItem();
                  Minecraft.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.OFF_HAND));
                  mc.playerController.windowClick(0, 45, slot, ClickType.SWAP, Minecraft.player);
               } else {
                  if (handSlot != slot) {
                     Minecraft.player.inventory.currentItem = slot;
                     mc.playerController.syncCurrentPlayItem();
                  }

                  Minecraft.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
                  if (handSlot != slot) {
                     Minecraft.player.inventory.currentItem = handSlot;
                     mc.playerController.syncCurrentPlayItem();
                  }
               }
            } else if (GETGOVNO(Items.air) != -1) {
               mc.playerController.windowClick(0, slot, 1, ClickType.QUICK_MOVE, Minecraft.player);
               if (GETGOVNO(Items.FIREWORKS) != -1) {
                  slot = GETGOVNO(Items.FIREWORKS);
                  if (handSlot != slot) {
                     Minecraft.player.inventory.currentItem = slot;
                     mc.playerController.syncCurrentPlayItem();
                  }

                  Minecraft.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
                  if (handSlot != slot) {
                     Minecraft.player.inventory.currentItem = handSlot;
                     mc.playerController.syncCurrentPlayItem();
                  }
               }
            } else {
               mc.playerController.windowClick(0, slot, Minecraft.player.inventory.currentItem, ClickType.SWAP, Minecraft.player);
               Minecraft.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
               mc.playerController.windowClickMemory(0, slot, Minecraft.player.inventory.currentItem, ClickType.SWAP, Minecraft.player, 250);
            }

            return slot != -1;
         }
      }
   }

   void efly() {
      if (this.canFly()) {
         if (!Minecraft.player.onGround
            && (Minecraft.player.fallDistance > 0.0F || Minecraft.player.hasNewVersionMoves && MathUtils.getDifferenceOf(Minecraft.player.motionY, 0.0) < 0.3)
            && this.boostTicks == 0
            && !Minecraft.player.getFlag(7)) {
            badPacket();
            Minecraft.player.setFlag(7, true);
            this.boostTicks = 1;
         }

         if (!Minecraft.player.isElytraFlying()) {
            this.boostTicks = 0;
         }

         if (Minecraft.player.onGround && !Minecraft.player.isJumping() && !Minecraft.player.isInWater()) {
            Minecraft.player.jump();
         }

         if (Minecraft.player.ticksElytraFlying > 0 && this.boostTicks >= 0) {
            if (this.boostTicks == 1) {
               this.putFirework();
            }

            if (this.BoostValue.getFloat() > 0.0F && this.GrimStrafed.getBool() && !this.StrafeDirs.getBool() && !this.StaticYMotions.getBool()) {
               float mul = 1.0F + Math.min(this.BoostValue.getFloat(), 7.0F) / 10.0F;
               Minecraft.player.motionX *= (double)mul;
               Minecraft.player.motionZ *= (double)mul;
            }

            this.boostTicks++;
            boolean isTargetStrafe = TargetStrafe.goStrafe();
            boolean speedTrigger = Math.sqrt(
                  Minecraft.player.motionX * Minecraft.player.motionX
                     + Minecraft.player.motionY * Minecraft.player.motionY
                     + Minecraft.player.motionZ * Minecraft.player.motionZ
               )
               < 1.0;
            float moveEEE = MoveMeHelp.moveYaw(0.0F);
            boolean sideNotForward = moveEEE < -45.0F || moveEEE > 45.0F;
            boolean cooledTrigger = Minecraft.player.getCooledAttackStrength(1.0F) < 0.1F || Minecraft.player.getCooledAttackStrength(1.0F) > 0.92F;
            if (this.boostTicks
                  > (isTargetStrafe ? 30 : (Minecraft.player.isJumping() && this.StaticYMotions.getBool() ? (MoveMeHelp.moveKeysAnySideMove() ? 20 : 8) : 28))
               || this.boostTicks > (speedTrigger ? 10 : (this.GrimStrafed.getBool() && sideNotForward && !this.MoveSideRotate.getBool() ? 20 : 25))
                  && (isTargetStrafe || MoveMeHelp.isMoving())
                  && cooledTrigger
                  && (HitAura.TARGET_ROTS == null || !HitAura.cooldown.hasReached((double)(HitAura.get.msCooldown() - 100.0F)))) {
               this.boostTicks = 0;
            }
         }
      } else if (!AirStuck.get.isActived() && AirStuck.get.ticksPostDisabled >= 10) {
         this.toggle(false);
      } else if (!AirStuck.get.isActived()) {
         eq();
      }
   }

   public void onTargetStrafeCallMotionsToFireworkMode(double[] motionsXZ) {
      this.calledXM = motionsXZ[0];
      this.calledZM = motionsXZ[1];
      this.targetStrafeIsCalling = true;
   }

   public boolean isTargetStrafeCalling() {
      return this.targetStrafeIsCalling && (this.calledXM != 0.0 || this.calledZM != 0.0);
   }

   public void targetStrafeCallReset() {
      this.targetStrafeIsCalling = false;
      this.calledXM = 0.0;
      this.calledZM = 0.0;
   }

   @EventTarget
   public void onSprintBlock(EventSprintBlock event) {
      if (this.actived && this.Mode.currentMode.equalsIgnoreCase("Firework") && Minecraft.player != null && Minecraft.player.ticksElytraFlying > 0) {
         event.cancel();
      }
   }

   @EventTarget
   public void onElytraRotateFly(EventElytraVector event) {
      if (event.getEntityIn() instanceof EntityPlayerSP sp
         && this.actived
         && this.Mode.currentMode.equalsIgnoreCase("Firework")
         && Minecraft.player.ticksElytraFlying > 0) {
         boolean targetStrafeCalled = this.isTargetStrafeCalling();
         boolean legalStrafe = this.GrimStrafed.getBool();
         float maxXZSpeed = 1.953F;
         float maxYSpeed = 1.0F;
         if (this.CustomSpeed.getBool()) {
            if (this.StrafeDirs.getBool()) {
               maxXZSpeed = MathUtils.lerp(
                  this.MotionXZ.getFloat() * 0.25F, this.MotionXZ.getFloat(), Math.min((float)Minecraft.player.ticksElytraFlying / 5.0F, 1.0F)
               );
            }

            if (this.StaticYMotions.getBool()) {
               maxYSpeed = Minecraft.player.ticksElytraFlying > 3
                  ? MathUtils.lerp(this.MotionY.getFloat() * 0.25F, this.MotionY.getFloat(), Math.min((float)Minecraft.player.ticksElytraFlying / 5.0F, 1.0F))
                  : 0.0F;
            }
         }

         boolean speedNormalize = !this.GrimStrafed.getBool()
            && this.StrafeDirs.getBool()
            && this.StaticYMotions.getBool()
            && this.NormalizeDirection.getBool();
         if (speedNormalize) {
            float speedBoostStepPerTick = this.CustomSpeed.getBool() ? this.MotionRunupDirect.getFloat() : 0.3F;
            maxXZSpeed = (float)Math.min(MoveMeHelp.getCuttingSpeed() + (double)speedBoostStepPerTick, (double)this.MotionXZ.getFloat());
            maxYSpeed = Minecraft.player.ticksElytraFlying > 1
               ? (float)Math.min(
                  Math.max(
                        Math.abs(Minecraft.player.lastTickPosY - Minecraft.player.prevChasingPosY),
                        (double)Math.max(this.MotionY.getFloat(), speedBoostStepPerTick)
                     )
                     + (double)speedBoostStepPerTick,
                  (double)this.MotionY.getFloat()
               )
               : 0.0F;
         }

         float dropDown = 0.0F;
         if (this.StrafeDirs.getBool()) {
            if (TargetStrafe.goStrafe() && TargetStrafe.target != null) {
               this.moveElytraYaw = RotationUtil.getYawToEntity(TargetStrafe.target) - (float)TargetStrafe.b * 90.0F;
               this.moveElytraPitch = speedNormalize ? 0.0F : -6.0F;
            } else if (!targetStrafeCalled) {
               if (MoveMeHelp.isMoving()) {
                  this.moveElytraYaw = MoveMeHelp.moveYaw(sp.rotationYaw);
                  int d = MoveMeHelp.isMoving() ? 45 : 90;
                  this.moveElytraPitch = MathUtils.clamp(
                     Minecraft.player.isSneaking() ? (float)d : (Minecraft.player.isJumping() ? (float)(-d) : (speedNormalize ? 0.0F : -6.0F)), -90.0F, 90.0F
                  );
                  MoveMeHelp.setSpeed(
                     MathUtils.clamp(
                        MoveMeHelp.getSpeed() * (MoveMeHelp.w() && !MoveMeHelp.s() ? 1.12 : 1.4), (double)Math.max(1.0F, maxXZSpeed), (double)maxXZSpeed
                     )
                  );
               } else if (!speedNormalize) {
                  Minecraft.player.motionX = 0.0;
                  Minecraft.player.motionZ = 0.0;
                  Entity.motionx = 1.0E-13;
                  Entity.motionz = 1.0E-13;
                  this.moveElytraPitch = -6.0F;
               } else {
                  Minecraft.player.motionX = 0.0;
                  Minecraft.player.motionZ = 0.0;
                  Entity.motionx = speedNormalize ? (double)(0.01F * (-1.0F + (float)Math.random() * 2.0F)) : 0.0;
                  Entity.motionz = speedNormalize ? (double)(0.01F * (-1.0F + (float)Math.random() * 2.0F)) : 0.0;
                  this.moveElytraYaw = getYawPitchFromDelta3(Entity.motionx, Entity.motiony, Entity.motionz)[0];
                  this.moveElytraPitch = -0.0F;
               }
            }

            event.setVectorAsYawPitch(this.moveElytraYaw, this.moveElytraPitch);
            legalStrafe = false;
         }

         if (!TargetStrafe.goStrafe() && this.StrafeDirs.getBool() && !this.GrimStrafed.getBool() && Minecraft.player != null) {
            try {
               mc.gameSettings.keyBindForward.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())
                  && (mc.currentScreen == null || InvWalk.get.isActived());
            } catch (Exception var29) {
            }
         }

         if (this.StaticYMotions.getBool() && Minecraft.player.isElytraFlying()) {
            double currentMotion = Minecraft.player.isJumping()
               ? (double)maxYSpeed
               : (
                  Minecraft.player.isSneaking()
                     ? (double)(-maxYSpeed)
                     : (speedNormalize ? (double)(Minecraft.player.ticksElytraFlying % 4 < 2 ? -0.03F : 0.03F) : (this.boostTicks == 1 ? 0.1F : -0.003F))
               );
            if (!Minecraft.player.isJumping() && !Minecraft.player.isSneaking() && TargetStrafe.goStrafe()) {
               double targetSyncY = TargetStrafe.target.isElytraFlying() && TargetStrafe.target.getSpeed() > 0.8F
                  ? TargetStrafe.get.getEntityVirtPos(TargetStrafe.target, TargetStrafe.get.TargetBoxSync.getBool()).yCoord + 0.7F
                  : (
                     (mc.world == null || !mc.world.getCollisionBoxes(TargetStrafe.target, TargetStrafe.target.boundingBox.offsetMinDown(2.3)).isEmpty())
                           && TargetStrafe.target.posY != TargetStrafe.target.lastTickPosY
                        ? (double)((int)(TargetStrafe.get.getEntityVirtPos(TargetStrafe.target, TargetStrafe.get.TargetBoxSync.getBool()).yCoord + 1.5))
                        : TargetStrafe.get.getEntityVirtPos(TargetStrafe.target, TargetStrafe.get.TargetBoxSync.getBool()).yCoord + 1.5
                  );
               if (mc.world != null) {
                  RayTraceResult result = mc.world
                     .rayTraceBlocks(
                        TargetStrafe.target.getPositionVector(), TargetStrafe.target.getPositionVector().addVector(0.0, -2.5, 0.0), false, true, true
                     );
                  if (result != null && result.hitVec != null && result.getBlockPos() != null) {
                     targetSyncY = (double)((float)result.getBlockPos().getY() + 2.2F);
                  }
               }

               if (TargetStrafe.goStrafe()
                  && TargetStrafe.target != null
                  && TargetStrafe.get.DistanceTricks.getMode().equalsIgnoreCase("RangeByter")
                  && TargetStrafe.get.ByterAdobeOnElytra.getBool()
                  && Minecraft.player.ticksElytraFlying > 0
                  && !TargetStrafe.target.isElytraFlying()
                  && mc.world != null) {
                  float offsetUp = 4.0F;
                  double checkDistanceAdobe = (double)(HitAura.get.getAuraRange(TargetStrafe.target) + offsetUp + 1.0F);
                  Vec3d meFrom = Minecraft.player.getPositionVector().addVector(0.0, (double)Minecraft.player.height, 0.0);
                  Vec3d meTo = meFrom.addVector(0.0, checkDistanceAdobe, 0.0);
                  RayTraceResult rayMeAdobe = mc.world.rayTraceBlocks(meFrom, meTo);
                  Vec3d targetFrom = TargetStrafe.get
                     .getEntityVirtPos(TargetStrafe.target, TargetStrafe.get.TargetBoxSync.getBool())
                     .addVector(0.0, (double)Minecraft.player.height, 0.0);
                  Vec3d targetTo = meFrom.addVector(0.0, checkDistanceAdobe, 0.0);
                  RayTraceResult rayTargetAdobe = mc.world.rayTraceBlocks(targetFrom, targetTo);
                  if (rayMeAdobe == null
                     || rayTargetAdobe == null
                     || rayMeAdobe.typeOfHit == RayTraceResult.Type.MISS && rayTargetAdobe.typeOfHit == RayTraceResult.Type.MISS) {
                     double upMinMeters = Math.max(
                        (double)(HitAura.get.getAuraRange(TargetStrafe.target) - Minecraft.player.height - 0.65F)
                           - Math.min(Minecraft.player.getDistanceXZ(targetFrom.xCoord, targetFrom.zCoord) * 1.347F, 2.0),
                        1.0
                     );
                     double upMaxMeters = upMinMeters + (double)offsetUp;
                     float preTickMoveOver = (float)((upMaxMeters - upMinMeters) / (double)maxYSpeed) + (float)(this.isTargetStrafeCalling() ? 1 : 0);
                     float cooldownPC = Math.max(
                        Math.min(Math.max((float)HitAura.cooldown.getTime() + preTickMoveOver * 50.0F, 0.0F) / HitAura.get.msCooldown(), 1.0F), 0.0F
                     );
                     Vec3d moveTo = targetFrom.addVector(0.0, cooldownPC == 1.0F ? upMinMeters : upMaxMeters, 0.0);
                     targetSyncY = moveTo.yCoord;
                  }
               }

               double dY = Minecraft.player.posY - targetSyncY;
               double speedY = (double)maxYSpeed;
               double moveY = MathUtils.clamp(-dY, -speedY, speedY);
               if (Math.abs(dY) > (double)(this.isTargetStrafeCalling() ? 0.15F : 0.7F)) {
                  currentMotion = Minecraft.player.ticksElytraFlying < 3 ? 0.0 : moveY;
               }
            }

            Minecraft.player.motionY = this.pauseTicksOutFlagged <= 0 ? currentMotion : Entity.motiony;
            if (!Minecraft.player.isJumping() && Minecraft.player.hasNewVersionMoves && currentMotion > 0.0) {
               Minecraft.player.fallDistance = 0.0F;
            }

            legalStrafe = false;
         }

         if (legalStrafe) {
            this.moveElytraYaw = MoveMeHelp.moveYaw(sp.rotationYaw);
            float maxP = Minecraft.player.isMoving() ? 60.0F : 90.0F;
            this.moveElytraPitch = MathUtils.clamp(Minecraft.player.isSneaking() ? maxP : (Minecraft.player.isJumping() ? -maxP : -1.7F), -90.0F, 90.0F);
            if (Minecraft.player.isMoving()) {
               MoveMeHelp.setSpeed(MoveMeHelp.getSpeed());
            }

            event.setVectorAsYawPitch(this.moveElytraYaw, this.moveElytraPitch);
         }

         if (dropDown != 0.0F) {
            Entity.motiony = (double)(-dropDown);
         }

         if (targetStrafeCalled) {
            Minecraft.player.motionX = this.calledXM;
            Minecraft.player.motionZ = this.calledZM;
            this.moveElytraYaw = getYawPitchFromDelta3(this.calledXM, 0.0, this.calledZM)[0];
         }

         if (speedNormalize) {
            double dy = Minecraft.player.motionY;
            double dx = Minecraft.player.motionX;
            double dz = Minecraft.player.motionZ;
            double sqXYZ = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double sqXZ = Math.sqrt(dx * dx + dz * dz);
            float speed3dLimit = this.CustomSpeed.getBool() ? this.MotionDirectMax.getFloat() : 1.953F;
            float[] dirs = getYawPitchFromDelta3(dx, dy, dz);
            double[] newXYZ = getVec3FromAngles(dirs[0], dirs[1], Math.min(sqXYZ, (double)speed3dLimit));
            if (Math.abs(dy) > 1.0E-4F) {
               Minecraft.player.motionY = newXYZ[1];
            } else {
               Minecraft.player.motionY = (double)(Minecraft.player.ticksElytraFlying % 4 < 2 ? -0.03F : 0.03F);
            }

            if (sqXZ > 1.0E-4F) {
               Minecraft.player.motionX = newXYZ[0];
               Minecraft.player.motionZ = newXYZ[2];
            } else {
               this.moveElytraYaw = Minecraft.player.rotationYaw;
               Minecraft.player.motionX = 0.0;
               Minecraft.player.motionZ = 0.0;
            }

            dirs = getYawPitchFromDelta3(Minecraft.player.motionX, Minecraft.player.motionY, Minecraft.player.motionZ);
            this.moveElytraYaw = dirs[0];
            this.moveElytraPitch = dirs[1];
         }

         Minecraft.player.motionY += 0.02F;
      }
   }

   private static float normalizeAngle(float angle) {
      angle = (angle + 180.0F) % 360.0F;
      if (angle < 0.0F) {
         angle += 360.0F;
      }

      return angle - 180.0F;
   }

   public static float[] getYawPitchFromDelta3(double motionX, double motionY, double motionZ) {
      float yaw = get.moveElytraYaw;
      float pitch = 0.0F;
      double xzLength = Math.sqrt(motionX * motionX + motionZ * motionZ);
      if (xzLength > 1.0E-6) {
         yaw = (float)Math.toDegrees(Math.atan2(motionZ, motionX)) - 90.0F;
         yaw = normalizeAngle(yaw);
      }

      if (xzLength > 1.0E-6 || Math.abs(motionY) > 1.0E-6) {
         pitch = (float)Math.toDegrees(-Math.atan2(motionY, xzLength));
         pitch = MathUtils.clamp(pitch, -90.0F, 90.0F);
      }

      return new float[]{yaw, pitch};
   }

   public static double[] getVec3FromAngles(float yaw, float pitch, double length) {
      double yawRadians = Math.toRadians((double)yaw);
      double pitchRadians = Math.toRadians((double)pitch);
      double x = Math.cos(pitchRadians) * Math.sin(yawRadians);
      double y = Math.sin(pitchRadians);
      double z = Math.cos(pitchRadians) * Math.cos(yawRadians);
      return new double[]{x * -length, y * -length, z * length};
   }

   public static int getItemElytra() {
      List<Integer> slots = new ArrayList<>();

      for (int i = 0; i < 45; i++) {
         ItemStack itemStack = Minecraft.player.inventoryContainer.getSlot(i).getStack();
         if (itemStack.getItem() == Items.ELYTRA) {
            slots.add(i);
         }
      }

      if (slots.size() > 1) {
         slots.sort(
            Comparator.comparing(
               slot -> -EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, Minecraft.player.inventoryContainer.getSlot(slot).getStack())
            )
         );
      }

      return slots.isEmpty() ? -1 : slots.get(0);
   }

   public static int getOldItem() {
      List<Integer> slots = new ArrayList<>();

      for (int i = 0; i < 45; i++) {
         ItemStack itemStack = Minecraft.player.inventoryContainer.getSlot(i).getStack();
         if (oldSlot != null && itemStack.getItem() == oldSlot) {
            slots.add(i);
         }
      }

      if (slots.size() > 1) {
         slots.sort(
            Comparator.comparing(
               slot -> -EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, Minecraft.player.inventoryContainer.getSlot(slot).getStack())
            )
         );
      }

      return slots.isEmpty() ? -1 : slots.get(0);
   }

   public static boolean itemOne() {
      for (int i = 0; i < 45; i++) {
         ItemStack itemStack = Minecraft.player.inventoryContainer.getSlot(i).getStack();
         if ((itemStack.getItem() instanceof ItemAir || itemStack.getItem() == oldSlot) && itemStack.stackSize == 1) {
            return true;
         }
      }

      return false;
   }

   public static void equipElytra() {
      if (Minecraft.player.inventory.armorItemInSlot(2).getItem() != Items.air) {
         mc.playerController.windowClick(0, 6, 1, ClickType.PICKUP, Minecraft.player);
      }

      mc.playerController.windowClick(0, getItemElytra(), 0, ClickType.PICKUP, Minecraft.player);
      mc.playerController.windowClick(0, 6, 1, ClickType.PICKUP, Minecraft.player);
   }

   public static void dequipElytra() {
      if (Minecraft.player.inventory.armorItemInSlot(2).getItem() != Items.air) {
         mc.playerController.windowClick(0, 6, 1, ClickType.PICKUP, Minecraft.player);
      }

      mc.playerController.windowClick(0, getOldItem(), 0, ClickType.PICKUP, Minecraft.player);
      mc.playerController.windowClick(0, 6, 1, ClickType.PICKUP, Minecraft.player);
      Speed.get.sleepTicks = 5;
   }

   public static int GETGOVNO(Item Item) {
      List<Integer> slots = new ArrayList<>();

      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = Minecraft.player.inventory.getStackInSlot(i);
         if (Item != null && itemStack.getItem() == Item) {
            slots.add(i);
         }
      }

      if (slots.size() > 1) {
         slots.sort(
            Comparator.comparing(slot -> -EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, Minecraft.player.inventory.getStackInSlot(slot)))
         );
      }

      return slots.isEmpty() ? -1 : slots.get(0);
   }

   public static boolean equipElytra2() {
      int slot = GETGOVNO(Items.ELYTRA);
      if (slot != -1) {
         mc.playerController.windowClick(0, 6, slot, ClickType.SWAP, Minecraft.player);
      }

      return slot != -1;
   }

   public static boolean dequipElytra2() {
      int slot = GETGOVNO(oldSlot);
      if (slot != -1) {
         mc.playerController.windowClick(0, 6, slot, ClickType.SWAP, Minecraft.player);
         Speed.get.sleepTicks = 5;
      }

      return slot != -1;
   }

   public static void badPacket() {
      mc.getConnection().sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_FALL_FLYING));
   }

   public static void badPacketElytra() {
      equipElytra();

      for (int i = 0; i < 2; i++) {
         badPacket();
      }

      dequipElytra();
   }

   public static boolean canElytra() {
      for (int i = 0; i < 45; i++) {
         ItemStack itemStack = Minecraft.player.inventoryContainer.getSlot(i).getStack();
         if (itemStack.getItem() == Items.ELYTRA) {
            return true;
         }
      }

      return Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra;
   }

   @Override
   public void onToggled(boolean actived) {
      if (this.Mode.currentMode.equalsIgnoreCase("NcpMiBoost") && Minecraft.player != null) {
         if (actived) {
            eq();
         } else {
            deq();
            if (Minecraft.player.ticksElytraFlying > 0) {
               badPacket();
               Minecraft.player.setFlag(7, false);
            }
         }
      }

      if (actived) {
         if (this.Mode.currentMode.equalsIgnoreCase("StrafeSync")) {
            if (canElytra()) {
               if (this.Mode.currentMode.equalsIgnoreCase("MatrixSpeed3")) {
                  equipElytra();
                  badPacket();
               }

               this.strafeMode = Strafe.get.Mode.currentMode;
               this.strafeActived = Strafe.get.actived;
               Strafe.get.toggle(true);
               Strafe.get.Mode.currentMode = "Matrix5";
            } else {
               this.toggle(false);
            }
         }

         if (!(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra)) {
            oldSlot = Minecraft.player.inventory.armorItemInSlot(2).getItem();
         }

         if (this.Mode.currentMode.equalsIgnoreCase("MatrixFly")
            || this.Mode.currentMode.equalsIgnoreCase("MatrixSpeed")
            || this.Mode.currentMode.equalsIgnoreCase("StrafeSync")) {
            this.timer.reset();
            badPacketElytra();
         }
      }

      if (this.Mode.currentMode.equalsIgnoreCase("VulcanPulse")) {
         if (actived) {
            this.wait.reset();
         } else {
            deq();
         }
      }

      if (this.Mode.currentMode.equalsIgnoreCase("VulcanSpeed") && canElytra() && !actived) {
         deq();
      }

      if (this.Mode.currentMode.equalsIgnoreCase("MatrixFly2") && canElytra()) {
         if (actived) {
            eq();
         } else {
            deq();
         }
      }

      hitTick = false;
      if (this.Mode.currentMode.equalsIgnoreCase("Firework")) {
         if (this.actived) {
            this.moveElytraYaw = Minecraft.player.rotationYaw;
            this.moveElytraPitch = Minecraft.player.rotationPitch;
            if (Minecraft.player.onGround) {
               Minecraft.player.jump();
            }

            if (!AirStuck.get.isActived() && AirStuck.get.ticksPostDisabled >= 10) {
               if (Minecraft.player.inventory.armorItemInSlot(2).getItem() != Items.air) {
                  oldSlot = Minecraft.player.inventory.armorItemInSlot(2).getItem();
               }

               int ely = -1;
               if (GETGOVNO(Items.ELYTRA) != -1) {
                  equipElytra2();
               } else if ((ely = getItemElytra()) != -1) {
                  try {
                     int handSlot = Minecraft.player.inventory.currentItem;
                     mc.playerController.windowClick(0, ely, handSlot, ClickType.SWAP, Minecraft.player);
                     mc.playerController.windowClick(0, 6, handSlot, ClickType.SWAP, Minecraft.player);
                     mc.playerController.windowClick(0, ely, handSlot, ClickType.SWAP, Minecraft.player);
                  } catch (Exception var5) {
                     var5.printStackTrace();
                  }
               }
            } else if (!AirStuck.get.isActived()) {
               eq();
            }
         } else {
            if (this.Mode.getMode().equalsIgnoreCase("Firework") && this.StrafeDirs.getBool() && !this.GrimStrafed.getBool() && Minecraft.player != null) {
               try {
                  mc.gameSettings.keyBindForward.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())
                     && (mc.currentScreen == null || InvWalk.get.isActived());
               } catch (Exception var6) {
               }
            }

            int old = -1;
            if (GETGOVNO(oldSlot) != -1) {
               dequipElytra2();
            } else if ((old = getOldItem()) != -1) {
               try {
                  int handSlot = Minecraft.player.inventory.currentItem;
                  mc.playerController.windowClick(0, old, handSlot, ClickType.SWAP, Minecraft.player);
                  mc.playerController.windowClick(0, 6, handSlot, ClickType.SWAP, Minecraft.player);
                  mc.playerController.windowClick(0, old, handSlot, ClickType.SWAP, Minecraft.player);
               } catch (Exception var4) {
                  var4.printStackTrace();
               }
            }

            if (Minecraft.player.getFlag(7)) {
               badPacket();
               Minecraft.player.setFlag(7, false);
               Minecraft.player.motionY = -0.0784000015258789;
               Minecraft.player.multiplyMotionXZ((float)(0.36F / MoveMeHelp.getCuttingSpeed()));
            }
         }
      }

      if (canElytra() && !actived && this.Mode.currentMode.equalsIgnoreCase("StrafeSync")) {
         if (this.strafeActived != Strafe.get.actived) {
            Strafe.get.toggle(this.strafeActived);
         }

         Strafe.get.Mode.currentMode = this.strafeMode;
      }

      flSpeed = 0.0;
      this.curPosY = Minecraft.player.posY;
      if (canElytra()) {
         if (this.Mode.currentMode.equalsIgnoreCase("NcpFly")) {
            if (actived) {
               if (!(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra)) {
                  if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemAir) {
                     mc.playerController.windowClick(0, getItemElytra(), 1, ClickType.QUICK_MOVE, Minecraft.player);
                  } else {
                     mc.playerController.windowClick(0, 6, 1, ClickType.QUICK_MOVE, Minecraft.player);
                     mc.playerController.windowClick(0, getItemElytra(), 1, ClickType.QUICK_MOVE, Minecraft.player);
                  }
               }
            } else if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra) {
               mc.playerController.windowClick(0, 6, 1, ClickType.QUICK_MOVE, Minecraft.player);
               mc.playerController.windowClick(0, getOldItem(), 1, ClickType.QUICK_MOVE, Minecraft.player);
            }

            MoveMeHelp.setSpeed(0.0);
            Minecraft.player.jumpMovementFactor = 0.0F;
         }

         if (this.Mode.currentMode.equalsIgnoreCase("Vanilla")) {
            if (actived && this.Mode.currentMode.equalsIgnoreCase("NcpFly")) {
               equipElytra();
            } else {
               dequipElytra();
               Minecraft.player.multiplyMotionXZ(0.14F);
            }
         }

         if (!actived && (this.Mode.currentMode.equalsIgnoreCase("MatrixFly3") || this.Mode.currentMode.equalsIgnoreCase("MatrixSpeed3"))) {
            MoveMeHelp.setSpeed(0.0);
            MoveMeHelp.setCuttingSpeed(0.0);
            Minecraft.player.jumpMovementFactor = 0.0F;
            Minecraft.player.motionY = 0.0;
         }
      }

      if ((
            this.Mode.currentMode.equalsIgnoreCase("MatrixFly")
               || this.Mode.currentMode.equalsIgnoreCase("MatrixSpeed")
               || this.Mode.currentMode.equalsIgnoreCase("StrafeSync")
         )
         && !actived
         && Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra
         && canElytra()) {
         dequipElytra();
         this.timer.lastMS = 1500L;
      }

      if (this.Mode.currentMode.equalsIgnoreCase("MatrixFly2")) {
         eq();
         Minecraft.player.fallDistance = 0.1F;
         if (!this.NoTimerDefunction.getBool()) {
            mc.timer.speed = 1.0;
         }

         this.wait.reset();
         if (actived) {
            if (!(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra)) {
               if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemAir) {
                  mc.playerController.windowClick(0, getItemElytra(), 1, ClickType.QUICK_MOVE, Minecraft.player);
               } else {
                  mc.playerController.windowClick(0, 6, 1, ClickType.QUICK_MOVE, Minecraft.player);
                  mc.playerController.windowClick(0, getItemElytra(), 1, ClickType.QUICK_MOVE, Minecraft.player);
               }
            }

            badPacket();
         } else {
            if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra) {
               mc.playerController.windowClick(0, 6, 1, ClickType.QUICK_MOVE, Minecraft.player);
               mc.playerController.windowClick(0, getOldItem(), 1, ClickType.QUICK_MOVE, Minecraft.player);
            }

            MoveMeHelp.setSpeed(0.0);
            MoveMeHelp.setCuttingSpeed(0.0);
            Minecraft.player.jumpMovementFactor = 0.0F;
            Minecraft.player.motionY = -0.228;
         }
      }

      if (this.Mode.currentMode.equalsIgnoreCase("NcpFly")) {
         if (actived) {
            Minecraft.player.connection.sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_FALL_FLYING));
         } else {
            Minecraft.player.capabilities.isFlying = false;
            Minecraft.player.capabilities.setFlySpeed(0.05F);
            if (!Minecraft.player.capabilities.isCreativeMode) {
               Minecraft.player.capabilities.allowFlying = false;
            }
         }
      }

      super.onToggled(actived);
   }

   @EventTarget
   public void onPlayerMotionUpdate(EventPlayerMotionUpdate e) {
      if (this.actived
         && this.Mode.currentMode.equalsIgnoreCase("Firework")
         && Minecraft.player.ticksElytraFlying > 0
         && (this.GrimStrafed.getBool() || this.StrafeDirs.getBool())
         && this.MoveSideRotate.getBool()) {
         if (!this.GrimStrafed.getBool()
            && this.StrafeDirs.getBool()
            && this.StaticYMotions.getBool()
            && Minecraft.player != null
            && Minecraft.player.getFlag(7)) {
            this.pauseTicksOutFlagged--;
         }

         float yaw = this.moveElytraYaw;
         float pitch = this.moveElytraPitch;
         boolean canRotate = !HitAura.get.isActived() || !HitAura.get.canRotateUpdated;
         if (canRotate) {
            e.setYaw(yaw);
            e.setPitch(pitch);
            Minecraft.player.rotationYawHead = e.getYaw();
            Minecraft.player.renderYawOffset = RotationUtil.calcYawOffset(Minecraft.player.rotationYawHead);
            Minecraft.player.rotationPitchHead = e.getPitch();
         }
      }

      if (this.actived
         && this.Mode.currentMode.equalsIgnoreCase("Vanilla")
         && Minecraft.player.isSneaking()
         && Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra) {
         e.ground = true;
      }

      if (this.Mode.currentMode.equalsIgnoreCase("MatrixSpeed3") && this.actived && canElytra() && Minecraft.player.onGround) {
         e.ground = false;
         if (!Minecraft.player.isJumping()) {
            e.setPosY(e.getPosY() + (Minecraft.player.ticksExisted % 3 == 0 ? 0.00215 : 0.0));
         }
      }
   }

   @EventTarget
   public void onMoveKeys(EventMovementInput event) {
      if (this.actived
         && this.Mode.currentMode.equalsIgnoreCase("Firework")
         && this.StrafeDirs.getBool()
         && !this.GrimStrafed.getBool()
         && Minecraft.player != null
         && Minecraft.player.isElytraFlying()
         && TargetStrafe.goStrafe()) {
         mc.gameSettings.keyBindForward.pressed = true;
      }
   }

   @Override
   public void onMovement() {
      if (this.Mode.currentMode.equalsIgnoreCase("MatrixFly2") && canElytra() && Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra) {
         MoveMeHelp.setSpeed(MoveMeHelp.getSpeed() * 1.12);
      }

      if (this.Mode.currentMode.equalsIgnoreCase("NcpFly")
         && (MoveMeHelp.isBlockAboveHead() ? (double)Minecraft.player.fallDistance >= 0.06 : Minecraft.player.fallDistance != 0.0F)) {
         Entity.motiony = -1.0E-45;
         mc.getConnection().sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_FALL_FLYING));
         MoveMeHelp.setSpeed((double)this.SpeedF.getFloat() * 0.3);
         Minecraft.player.motionY = 0.0;
         Minecraft.player.setSprinting(!Minecraft.player.isElytraFlying());
      }
   }

   @Override
   public void onUpdate() {
      if (this.Mode.getMode().equalsIgnoreCase("NcpMiBoost")) {
         if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra) {
            int ticksFly = Minecraft.player.ticksElytraFlying;
            if (ticksFly > 0) {
               double motionY = Minecraft.player.ticksExisted % 20 == 0 ? 0.01F : -5.2631577E-4F;
               boolean yLifting = false;
               if (Minecraft.player.isJumping()) {
                  motionY = Math.max(0.6F, Minecraft.player.motionY + 0.06F);
                  double limitSpeedUp = 1.0;
                  if (motionY > limitSpeedUp) {
                     motionY = limitSpeedUp;
                  }

                  yLifting = true;
               } else if (Minecraft.player.isSneaking()) {
                  motionY = Math.min(-0.6F, Minecraft.player.motionY - 0.15F);
                  double limitSpeedDown = 1.5;
                  if (motionY < -limitSpeedDown) {
                     motionY = -limitSpeedDown;
                  }

                  yLifting = true;
               }

               Minecraft.player.motionY = motionY;
               Entity.motiony = motionY;
               double motionLimitMax = yLifting ? 0.0 : 1.953F;
               double motionLimitMin = MoveMeHelp.getSpeed() < 0.1F ? 0.23F : 0.76F;
               double multiplierMotionMax = 1.02F;
               double multiplierMotionMin = 1.01F;
               if (MoveMeHelp.isMoving() && ticksFly > 2) {
                  Minecraft.player.setSprinting(!Minecraft.player.isSneaking());
                  Minecraft.player
                     .multiplyMotionXZ(
                        (float)MathUtils.lerp(
                           multiplierMotionMax,
                           multiplierMotionMin,
                           motionLimitMax <= 0.0 ? 1.0 : MathUtils.clamp(MoveMeHelp.getSpeed() / motionLimitMax, 0.0, 1.0)
                        )
                     );
               } else {
                  MoveMeHelp.setSpeed(0.0);
               }

               if (MoveMeHelp.getSpeed() >= motionLimitMax) {
                  MoveMeHelp.setFixedMotionYaw(
                     motionLimitMax,
                     RotationUtil.getVecNeeded(Vec3d.ZERO, new Vec3d(Minecraft.player.motionX, Minecraft.player.motionY, Minecraft.player.motionZ))[0],
                     false
                  );
               } else if (MoveMeHelp.getSpeed() < motionLimitMin && MoveMeHelp.isMoving()) {
                  MoveMeHelp.setSpeed(motionLimitMin);
               }

               MoveMeHelp.setSmoothSpeed(MoveMeHelp.getSpeed(), 0.1F, false);
               float[] calcRotations = RotationUtil.getVecNeeded(
                  Vec3d.ZERO, new Vec3d(Minecraft.player.motionX, Minecraft.player.motionY, Minecraft.player.motionZ)
               );
               this.moveElytraYaw = calcRotations[0];
               this.moveElytraPitch = calcRotations[1];
            } else if (Minecraft.player.hasNewVersionMoves
               ? !Minecraft.player.onGround && Minecraft.player.motionY < 0.333F
               : Minecraft.player.fallDistance > 0.0F) {
               if (!Minecraft.player.isElytraFlying()) {
                  badPacket();
               }
            } else if (Minecraft.player.onGround) {
               Minecraft.player.jump();
            }
         } else if (getItemElytra() != -1) {
            eq();
         } else {
            this.toggle();
         }
      }

      if (this.actived && this.Mode.currentMode.equalsIgnoreCase("Firework")) {
         this.efly();
      }

      if (this.Mode.currentMode.equalsIgnoreCase("VulcanPulse") && canElytra()) {
         if (Minecraft.player.fallDistance > 0.0F
            && (double)Minecraft.player.fallDistance < 0.12
            && Minecraft.player.inventory.armorInventory.get(2).getItem() instanceof ItemElytra
            && !Minecraft.player.getFlag(7)) {
            badPacket();
         }

         if ((double)Minecraft.player.fallDistance > 0.3 && Minecraft.player.fallDistance < 1.0F && !Minecraft.player.getFlag(7)) {
            eq();
            this.wait.reset();
            badPacket();
            Minecraft.player.setFlag(7, true);
         }

         if (!Minecraft.player.isElytraFlying()) {
            this.wait.reset();
         }

         if (Minecraft.player.inventory.armorInventory.get(2).getItem() instanceof ItemElytra) {
            if (Minecraft.player.onGround) {
               this.wait.reset();
               if (!mc.gameSettings.keyBindJump.isKeyDown()) {
                  if (Minecraft.player.rayGround && Minecraft.player.onGround) {
                     Minecraft.player.jump();
                  }
               } else {
                  mc.gameSettings.keyBindJump.pressed = Minecraft.player.rayGround && Minecraft.player.onGround;
               }
            } else {
               Minecraft.player.onGround = false;
               if (this.wait.hasReached(100.0) && !this.wait.hasReached(150.0)) {
                  Minecraft.player.motionY = 1.4;
                  MoveMeHelp.setSpeed(3.0);
                  MoveMeHelp.setCuttingSpeed(2.830188679245283);
               }

               if (!this.wait.hasReached(100.0)) {
                  MoveMeHelp.setSpeed(0.0);
               }
            }

            Minecraft.player.rayGround = Minecraft.player.onGround;
         } else {
            eq();
         }
      }

      if (this.Mode.currentMode.equalsIgnoreCase("VulcanSpeed")) {
         if (canElytra()) {
            double speed = MoveMeHelp.getSpeed() * 2.5;
            if (speed < 1.3) {
               speed = 1.3;
            }

            if (speed > 1.93) {
               speed = 1.93;
            }

            if (!(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra)) {
               oldSlot = Minecraft.player.inventory.armorItemInSlot(2).getItem();
            }

            if ((
                  (double)Minecraft.player.fallDistance > 0.1 && (double)Minecraft.player.fallDistance < 0.3
                     || (double)Minecraft.player.fallDistance > 1.05 && (double)Minecraft.player.fallDistance < 1.3
               )
               && !Minecraft.player.onGround
               && Speed.posBlock(Minecraft.player.posX, Minecraft.player.posY + Entity.Getmotiony, Minecraft.player.posZ)) {
               eq();
               badPacket();
               this.boostTicks = 0;
            }

            this.boostTicks++;
            if (this.boostTicks == 1) {
               MoveMeHelp.setSpeed(speed);
               flSpeed = speed;
            } else {
               flSpeed = 0.0;
            }

            if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra) {
               if (this.boostTicks == 1) {
                  badPacket();
               }

               if (this.boostTicks > 4) {
                  deq();
               }
            }
         } else {
            this.toggle(false);
         }
      }

      if (!(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra)) {
         oldSlot = Minecraft.player.inventory.armorItemInSlot(2).getItem();
      }

      if (this.Mode.currentMode.equalsIgnoreCase("NcpFly") && Minecraft.player.onGround) {
         Minecraft.player.motionY = 0.42F;
      }

      if (this.Mode.currentMode.equalsIgnoreCase("MatrixSpeed3") && this.actived) {
         if (canElytra() && Minecraft.player.fallDistance < 2.0F) {
            if (Minecraft.player.ticksExisted % 2 == 0) {
               if (!(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra)) {
                  if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemAir) {
                     mc.playerController.windowClick(0, getItemElytra(), 1, ClickType.QUICK_MOVE, Minecraft.player);
                  } else {
                     mc.playerController.windowClick(0, 6, 1, ClickType.QUICK_MOVE, Minecraft.player);
                     mc.playerController.windowClick(0, getItemElytra(), 1, ClickType.QUICK_MOVE, Minecraft.player);
                  }
               }

               this.boostTicks++;
               badPacket();
               if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra) {
                  mc.playerController.windowClick(0, 6, 1, ClickType.QUICK_MOVE, Minecraft.player);
                  mc.playerController.windowClick(0, getOldItem(), 1, ClickType.QUICK_MOVE, Minecraft.player);
               }
            }

            Minecraft.player.connection.sendPacket(new CPacketCloseWindow(0));
            if (Minecraft.player.onGround && Minecraft.player.isJumping()) {
               Minecraft.player.motionY = 1.0;
               if (!Minecraft.player.isCollidedHorizontally) {
                  Entity.motiony = 1.0E-4;
               }
            } else if (!Minecraft.player.isCollidedHorizontally) {
               Minecraft.player.motionY -= 0.1;
            }

            double speedx = MathUtils.clamp(
               MoveMeHelp.getSpeed() * (Minecraft.player.isSprinting() ? 1.1 : 1.15),
               Minecraft.player.isHandActive() && Minecraft.player.isJumping() ? 0.3 : 1.0,
               Minecraft.player.isHandActive() && Minecraft.player.isJumping() ? 0.3 : 1.6
            );
            flSpeed = speedx;
            MoveMeHelp.setSpeed(flSpeed);
            MoveMeHelp.setCuttingSpeed(flSpeed / 1.06);
         } else {
            flSpeed = 0.0;
         }
      }

      if (this.Mode.currentMode.equalsIgnoreCase("MatrixFly3") && this.actived) {
         boolean move = MoveMeHelp.moveKeysPressed() || TargetStrafe.goStrafe();
         boolean canFly = Minecraft.player.fallDistance > 0.06F || !Minecraft.player.onGround && MathUtils.getDifferenceOf(Entity.Getmotiony, 0.0) < 0.4;
         if (!canFly && canElytra()) {
            this.boostTicks = 0;
            if (Minecraft.player.onGround && !Keyboard.isKeyDown(this.bind)) {
               Minecraft.player.motionY = 0.42;
            }

            Minecraft.player.jumpMovementFactor = 0.0F;
            MoveMeHelp.setSpeed(0.0);
            MoveMeHelp.setCuttingSpeed(0.0);
         }

         if (canElytra() && canFly) {
            if (Minecraft.player.fallDistance < 1.0F && Entity.Getmotiony > 0.15) {
               Minecraft.player.fallDistance = 1.0F;
            }

            Minecraft.player.onGround = false;
            Minecraft.player.motionY = 0.0;
            Entity.motiony = Minecraft.player.ticksExisted % 3 == 0 ? 0.05 : -0.025;
            if (Minecraft.player.ticksExisted % 2 == 0) {
               if (!(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra)) {
                  if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemAir) {
                     mc.playerController.windowClick(0, getItemElytra(), 1, ClickType.QUICK_MOVE, Minecraft.player);
                  } else {
                     mc.playerController.windowClick(0, 6, 1, ClickType.QUICK_MOVE, Minecraft.player);
                     mc.playerController.windowClick(0, getItemElytra(), 1, ClickType.QUICK_MOVE, Minecraft.player);
                     if (flSpeed < 0.1) {
                        hitTick = true;
                     }
                  }
               }

               badPacket();
               if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra) {
                  mc.playerController.windowClick(0, 6, 1, ClickType.QUICK_MOVE, Minecraft.player);
                  mc.playerController.windowClick(0, getOldItem(), 1, ClickType.QUICK_MOVE, Minecraft.player);
               }

               this.boostTicks++;
            } else {
               hitTick = false;
            }

            float ymo = this.boostTicks > 1 ? (Minecraft.player.isJumping() ? 0.93F : 0.6F) : Float.MIN_VALUE;
            if (MoveMeHelp.getSpeed() == 0.0 && !MoveMeHelp.isMoving()) {
               ymo *= 2.0F;
            }

            double motionYx = Minecraft.player.isJumping() ? (double)ymo : (Minecraft.player.isSneaking() ? (double)(-ymo) : 0.0);
            boolean can = !Speed.posBlock(Minecraft.player.posX, Minecraft.player.posY - 1.0, Minecraft.player.posZ)
               && !Speed.posBlock(Minecraft.player.posX, Minecraft.player.posY - 1.15, Minecraft.player.posZ);
            if (motionYx != 0.0 && can) {
               Entity.motiony = motionYx;
            }

            if (!can && Minecraft.player.isJumping()) {
               Minecraft.player.onGround = true;
               Entity.motiony = Entity.Getmotiony + 0.06876;
            }

            float speedVal = this.SpeedXZ.getFloat() - (Minecraft.player.ticksExisted % 2 == 0 ? 0.0F : 0.005F);
            double speedx = (double)(speedVal - 0.046F);
            if (move) {
               double a;
               if (flSpeed < (a = TargetStrafe.goStrafe() ? TargetStrafe.getCurrentSpeed(false) : MoveMeHelp.getSpeed())) {
                  flSpeed = a;
               }

               flSpeed += 0.1F;
               if (flSpeed >= speedx) {
                  flSpeed = speedx;
               }
            } else {
               flSpeed = 0.09;
            }

            MoveMeHelp.setSpeed(flSpeed, 0.6F);
            if (move) {
               if (flSpeed < 1.1 && flSpeed > 0.03) {
                  Minecraft.player.jump();
               }

               MoveMeHelp.setCuttingSpeed(flSpeed / 1.06);
            } else if (MoveMeHelp.getSpeed() < 0.1) {
               MoveMeHelp.setCuttingSpeed(0.0);
            }
         }
      }

      if (this.Mode.currentMode.equalsIgnoreCase("MatrixFly2") && canElytra()) {
         if (Minecraft.player.onGround && !Minecraft.player.isJumping()) {
            Minecraft.player.jump();
         }

         if (Minecraft.player.fallDistance != 0.0F) {
            badPacket();
            if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra && this.wait.hasReached(150.0)) {
               double speedxx = MathUtils.clamp(MoveMeHelp.getSpeed() * 1.12, 1.0, 1.953);
               flSpeed = speedxx;
               if (MoveMeHelp.isMoving()) {
                  MoveMeHelp.setSpeed(speedxx);
                  MoveMeHelp.setCuttingSpeed(speedxx / 1.06);
               }

               Minecraft.player.motionY = Minecraft.player.isJumping() ? 0.42 : (Minecraft.player.isSneaking() ? -0.42 : 0.01);
               if (!Minecraft.player.isJumping() && !Minecraft.player.isSneaking()) {
                  Entity.motiony = 1.0E-5;
               }
            } else {
               this.wait.reset();
            }

            mc.timer.speed = Minecraft.player.fallDistance != 0.0F && (!this.NoTimerDefunction.getBool() || mc.timer.speed != 0.5) ? 0.5 : 1.0;
            if (Minecraft.player.fallDistance != 0.0F && !this.NoTimerDefunction.getBool()) {
               Timer.forceTimer(0.5F);
            }
         }
      }

      if (this.actived && this.Mode.currentMode.equalsIgnoreCase("Vanilla") && canElytra()) {
         double speedxxx = MoveMeHelp.getSpeed() < 0.2 ? 0.2499 - (Minecraft.player.ticksExisted % 2 == 0 ? 0.01 : 0.0) : MoveMeHelp.getSpeed() * 1.03;
         if (Minecraft.player.ticksExisted % 10 == 0) {
            if (!(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra)) {
               speedxxx = (double)(this.SpeedF.getFloat() / 2.0F);
               equipElytra();
               this.wait.reset();
            }
         } else if (this.wait.hasReached(100.0)
            && !this.wait.hasReached(250.0)
            && Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra) {
            dequipElytra();
         }

         if (MoveMeHelp.isMoving()) {
            MoveMeHelp.setSpeed(speedxxx);
         }

         Minecraft.player.motionY = Minecraft.player.isJumping() ? 1.0 : (Minecraft.player.isSneaking() ? -1.0 : 0.0);
         if (Minecraft.player.isSneaking()) {
            Minecraft.player.fallDistance = 0.1F;
            Minecraft.player.onGround = true;
         }
      }

      if (this.Mode.currentMode.equalsIgnoreCase("MatrixSpeed2") && canElytra()) {
         flSpeed = 0.0;
         if (Minecraft.player.isInWater() || Minecraft.player.isInLava() || Minecraft.player.isInWeb) {
            return;
         }

         if (Minecraft.player.fallDistance != 0.0F && (double)Minecraft.player.fallDistance < 0.1 && Minecraft.player.motionY < -0.1) {
            if (!(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra)) {
               if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemAir) {
                  mc.playerController.windowClick(0, getItemElytra(), 1, ClickType.QUICK_MOVE, Minecraft.player);
               } else {
                  mc.playerController.windowClick(0, 6, 1, ClickType.QUICK_MOVE, Minecraft.player);
                  mc.playerController.windowClick(0, getItemElytra(), 1, ClickType.QUICK_MOVE, Minecraft.player);
               }
            }

            badPacket();
            badPacket();
            if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra) {
               mc.playerController.windowClick(0, 6, 1, ClickType.QUICK_MOVE, Minecraft.player);
               mc.playerController.windowClick(0, getOldItem(), 1, ClickType.QUICK_MOVE, Minecraft.player);
            }
         }

         boolean targetStrafe = TargetStrafe.get.actived && TargetStrafe.target != null && HitAura.get.actived;
         boolean air = AirJump.get.actived && AirJump.get.Mode.currentMode.equalsIgnoreCase("Matrix");
         int ex = air ? 2 : 1;
         double cur = targetStrafe ? TargetStrafe.getCurrentSpeed(false) : MoveMeHelp.getCuttingSpeed();
         double speedxxxx = MathUtils.clamp(
            cur > 10.0 ? 1.96 : cur * (air ? 1.2 : 1.4),
            0.2499 - (Minecraft.player.ticksExisted % 2 == 0 ? 0.01 : 0.0),
            (
                  Minecraft.player.isHandActive() && Minecraft.player.fallDistance > 0.0F
                     ? 1.6 - MathUtils.clamp((double)(Minecraft.player.fallDistance * 2.0F), 0.0, 1.4)
                     : 1.6
               )
               / (air ? 1.45 : 1.0)
         );
         if ((double)Minecraft.player.fallDistance >= 0.15
            && (MoveMeHelp.isMoving() || targetStrafe)
            && (
               Speed.posBlock(Minecraft.player.posX, Minecraft.player.posY - (double)ex, Minecraft.player.posZ)
                  || Speed.posBlock(Minecraft.player.posX, Minecraft.player.posY - ((double)ex + 0.2), Minecraft.player.posZ)
                  || Speed.canMatrixBoost()
            )
            && !MoveMeHelp.isBlockAboveHead()
            && !Speed.posBlock(Minecraft.player.posX, Minecraft.player.posY - ((double)ex - 0.8), Minecraft.player.posZ)) {
            if (!targetStrafe) {
               MoveMeHelp.setSpeed(speedxxxx);
               MoveMeHelp.setCuttingSpeed(speedxxxx / 1.06);
            }

            flSpeed = speedxxxx / 1.01;
         }
      }

      if (this.Mode.currentMode.equalsIgnoreCase("MatrixFly")
         || this.Mode.currentMode.equalsIgnoreCase("MatrixSpeed")
         || this.Mode.currentMode.equalsIgnoreCase("StrafeSync")) {
         if (canElytra()) {
            if (this.Mode.currentMode.equalsIgnoreCase("MatrixFly")) {
               boolean isDowned = false;
               boolean isFeared = false;
               if (this.timer2.hasReached(1050.0)) {
                  isDowned = true;
               }

               if (this.timer2.hasReached(1100.0)) {
                  isDowned = false;
                  isFeared = true;
                  if (this.timer2.hasReached(1200.0)) {
                     this.timer2.reset();
                     badPacketElytra();
                  }
               }

               float yaws = Minecraft.player.rotationYaw * (float) (Math.PI / 180.0);
               float sp = MoveMeHelp.getSpeed() < 0.3 ? 0.02F : 0.0F;
               if (Minecraft.player.ticksExisted % 2 == 0) {
                  Minecraft.player.motionX = Minecraft.player.motionX + (double)(MathHelper.sin(yaws + 45.0F) * sp * 2.0F);
                  Minecraft.player.motionZ = Minecraft.player.motionZ - (double)(MathHelper.cos(yaws + 45.0F) * sp * 2.0F);
               }

               Minecraft.player.motionX = Minecraft.player.motionX - (double)(MathHelper.sin(yaws + 45.0F) * sp);
               Minecraft.player.motionZ = Minecraft.player.motionZ + (double)(MathHelper.cos(yaws + 45.0F) * sp);
               Minecraft.player.setSprinting(false);
               if (Minecraft.player.isSneaking()) {
                  if (Minecraft.player.motionY > -0.2) {
                     Minecraft.player.motionY = -0.2;
                  }

                  if (Minecraft.player.motionY < -1.0) {
                     Minecraft.player.motionY -= 0.1;
                  }
               } else {
                  Minecraft.player.jump();
               }

               if (MoveMeHelp.getSpeed() < (double)(this.SpeedF.getFloat() * 0.89F) * 0.889 && MoveMeHelp.isMoving()) {
                  MoveMeHelp.setSpeed(
                     MathUtils.clamp(
                        MoveMeHelp.getSpeed() * (double)(MoveMeHelp.getSpeed() > 2.2F ? (MoveMeHelp.getSpeed() > 7.5 ? 1.1F : 1.12F) : 1.2F),
                        0.03,
                        (double)(this.SpeedF.getFloat() * 0.89F) * 0.889
                     )
                  );
               } else {
                  Minecraft.player.motionX /= 1.02;
                  Minecraft.player.motionZ /= 1.02;
               }

               float yport = 0.0765F;
               if (!Minecraft.player.isSneaking()) {
                  if (!isDowned && Minecraft.player.isJumping() || !Minecraft.player.isJumping()) {
                     Minecraft.player.motionY = Minecraft.player.isJumping()
                        ? 0.499
                        : (
                           Minecraft.player.ticksExisted % 8 == 0
                              ? (double)(yport / 2.0F)
                              : (Minecraft.player.ticksExisted % 8 == 1 ? (double)(-yport / 2.0F) : 0.0)
                        );
                  }

                  if (isDowned && Minecraft.player.isJumping()) {
                     Minecraft.player.motionY = -0.1;
                  }

                  Minecraft.player.motionY /= isFeared ? 1.05F : 1.03F;
               }

               Minecraft.player.rotationYaw = (float)((double)Minecraft.player.rotationYaw + (Minecraft.player.ticksExisted % 2 == 0 ? 1.0E-4 : -1.0E-4));
               if (Minecraft.player.ticksExisted % 3 == 0) {
                  Minecraft.player.fallDistance = 0.0F;
               } else {
                  Minecraft.player.fallDistance = (float)(1.0 + Minecraft.player.motionY);
               }
            }

            if (this.Mode.currentMode.equalsIgnoreCase("MatrixSpeed")) {
               if (!Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) && Minecraft.player.onGround) {
                  Minecraft.player.motionY = 0.1;
                  Minecraft.player.onGround = false;
               }

               if (!Minecraft.player.onGround
                  && MoveMeHelp.getSpeed() < (double)(this.SpeedF.getFloat() * 0.89F)
                  && (Minecraft.player.isMoving() || mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown())) {
                  MoveMeHelp.setSpeed(
                     MoveMeHelp.getSpeed() * (double)(MoveMeHelp.getSpeed() > 2.2F ? (MoveMeHelp.getSpeed() > 7.5 ? 1.1F : 1.125F) : 1.2F), 0.9F
                  );
               } else {
                  Minecraft.player.motionX /= 1.36;
                  Minecraft.player.motionZ /= 1.36;
               }

               if (MoveMeHelp.getSpeed() < 0.195) {
                  MoveMeHelp.setSpeed(0.195, 1);
               }
            }
         } else {
            this.timer.lastMS = 1360L;
         }
      }
   }

   @EventTarget
   public void onPacket(EventReceivePacket event) {
      if (this.Mode.currentMode.equalsIgnoreCase("NcpFly")
         && Minecraft.player != null
         && mc.world != null
         && !Minecraft.player.isDead
         && this.actived
         && event.getPacket() instanceof SPacketEntityVelocity) {
         event.setCancelled(true);
      }

      if (this.actived
         && this.Mode.getMode().equalsIgnoreCase("Firework")
         && !this.GrimStrafed.getBool()
         && this.StrafeDirs.getBool()
         && this.StaticYMotions.getBool()
         && Minecraft.player != null
         && Minecraft.player.getFlag(7)
         && event.getPacket() instanceof SPacketPlayerPosLook look) {
         Vec3d lookPosVec = new Vec3d(look.getX(), look.getY(), look.getZ());
         if (Minecraft.player.getDistanceToVec3d(lookPosVec) < 2.0) {
            this.pauseTicksOutFlagged = 1;
         }

         if (Minecraft.player.getDistanceToVec3d(lookPosVec) < 6.0) {
            badPacket();
            Minecraft.player.setFlag(7, false);
         }
      }
   }
}
