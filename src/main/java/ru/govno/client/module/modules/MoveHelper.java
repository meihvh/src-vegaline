package ru.govno.client.module.modules;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemShield;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.potion.Potion;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Keyboard;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventSlowLay;
import ru.govno.client.event.events.EventSlowSneak;
import ru.govno.client.event.events.EventSprintBlock;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.BlockHelper;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;
import ru.govno.client.utils.Versions.NewPhisicsFixes;

public class MoveHelper extends Module {
   public static MoveHelper instance;
   public BoolSettings NoJumpDelay;
   public BoolSettings FastLadders;
   public BoolSettings GmFlyStrafe;
   public BoolSettings MatrixSnowFix;
   public BoolSettings StairSpeed;
   public BoolSettings NoSlowDown;
   public BoolSettings NoJumpSlowGrim;
   public BoolSettings NCPFapBypass;
   public BoolSettings InWebMotion;
   public BoolSettings WebZoom;
   public BoolSettings AnchoorHole;
   public BoolSettings Step;
   public BoolSettings ReverseStep;
   public BoolSettings NoSlowSneak;
   public BoolSettings NoSlowLay;
   public BoolSettings TrapdoorSpeed;
   public BoolSettings GroundHalt;
   public BoolSettings LevitateControl;
   public BoolSettings NoSlowSoul;
   public BoolSettings FastPilingUp;
   public BoolSettings IgnoreHunger;
   public BoolSettings SkipWorldboder;
   public BoolSettings AutoСrouch;
   public FloatSettings WebSpeedXZ;
   public FloatSettings WebSpeedYPlus;
   public FloatSettings WebSpeedYMinus;
   public FloatSettings GMFlySpeedXZ;
   public FloatSettings GMFlySpeedY;
   public FloatSettings NoSlowPercent;
   public ModeSettings FlyStrafeMode;
   public ModeSettings NoSlowMode;
   public ModeSettings WebMotionMode;
   public ModeSettings StepUpMode;
   public ModeSettings SneakSlowBypass;
   public ModeSettings LaySlowBypass;
   private final TimerHelper notRotTime = TimerHelper.TimerHelperReseted();
   private boolean sneakPush;
   private boolean canPress;
   public static boolean stairTick = false;
   private static int tickCounter;
   public static boolean holeTick = false;
   public static int holeTicks = 0;

   public MoveHelper() {
      super("MoveHelper", 0, Module.Category.MOVEMENT);
      instance = this;
      this.settings.add(this.NoJumpDelay = new BoolSettings("NoJumpDelay", true, this));
      this.settings.add(this.FastLadders = new BoolSettings("FastLadders", true, this));
      this.settings.add(this.GmFlyStrafe = new BoolSettings("GmFlyStrafe", true, this));
      this.settings
         .add(
            this.FlyStrafeMode = new ModeSettings(
               "FlyStrafeMode",
               "Matrix",
               this,
               new String[]{"Vanilla", "Matrix", "NCP", "Matrix&AAC", "Matrix&NCP", "NcpNew"},
               () -> this.GmFlyStrafe.getBool()
            )
         );
      this.settings
         .add(
            this.GMFlySpeedXZ = new FloatSettings(
               "GMFlySpeedXZ", 3.4F, 7.0F, 1.0F, this, () -> this.GmFlyStrafe.getBool() && this.FlyStrafeMode.getMode().equalsIgnoreCase("Vanilla")
            )
         );
      this.settings
         .add(
            this.GMFlySpeedY = new FloatSettings(
               "GMFlySpeedY", 5.0F, 7.0F, 0.1F, this, () -> this.GmFlyStrafe.getBool() && this.FlyStrafeMode.getMode().equalsIgnoreCase("Vanilla")
            )
         );
      this.settings.add(this.MatrixSnowFix = new BoolSettings("MatrixSnowFix", false, this));
      this.settings.add(this.StairSpeed = new BoolSettings("StairSpeed", true, this));
      this.settings.add(this.NoSlowDown = new BoolSettings("NoSlowDown", true, this));
      this.settings
         .add(
            this.NoSlowMode = new ModeSettings(
               "NoSlowMode",
               "MatrixLatest",
               this,
               new String[]{"Vanilla", "MatrixOld", "MatrixLatest", "AACOld", "NCP+", "Grim", "Intave", "NCPNew"},
               () -> this.NoSlowDown.getBool()
            )
         );
      this.settings
         .add(
            this.NoSlowPercent = new FloatSettings(
               "NoSlowPercent", 100.0F, 100.0F, 0.0F, this, () -> this.NoSlowDown.getBool() && this.NoSlowMode.getMode().equalsIgnoreCase("Vanilla")
            )
         );
      this.settings
         .add(
            this.NoJumpSlowGrim = new BoolSettings(
               "NoJumpSlowGrim",
               true,
               this,
               () -> this.NoSlowDown.getBool()
                     && (this.NoSlowMode.currentMode.equalsIgnoreCase("Grim") || this.NoSlowMode.currentMode.equalsIgnoreCase("Intave"))
            )
         );
      this.settings
         .add(
            this.NCPFapBypass = new BoolSettings(
               "NCP+FapBypass", true, this, () -> this.NoSlowDown.getBool() && this.NoSlowMode.currentMode.equalsIgnoreCase("NCP+")
            )
         );
      this.settings.add(this.InWebMotion = new BoolSettings("InWebMotion", true, this));
      this.settings
         .add(
            this.WebMotionMode = new ModeSettings(
               "WebMotionMode", "Matrix", this, new String[]{"Custom", "Matrix", "NoCollide"}, () -> this.InWebMotion.getBool()
            )
         );
      this.settings
         .add(
            this.WebZoom = new BoolSettings(
               "WebZoom", false, this, () -> this.InWebMotion.getBool() && this.WebMotionMode.currentMode.equalsIgnoreCase("Matrix")
            )
         );
      this.settings
         .add(
            this.WebSpeedXZ = new FloatSettings(
               "WebSpeedXZ", 0.5F, 1.0F, 0.1F, this, () -> this.InWebMotion.getBool() && this.WebMotionMode.currentMode.equalsIgnoreCase("Custom")
            )
         );
      this.settings
         .add(
            this.WebSpeedYPlus = new FloatSettings(
               "WebSpeedY+", 0.5F, 1.0F, 0.0F, this, () -> this.InWebMotion.getBool() && this.WebMotionMode.currentMode.equalsIgnoreCase("Custom")
            )
         );
      this.settings
         .add(
            this.WebSpeedYMinus = new FloatSettings(
               "WebSpeedY-", 0.4F, 1.0F, 0.0F, this, () -> this.InWebMotion.getBool() && this.WebMotionMode.currentMode.equalsIgnoreCase("Custom")
            )
         );
      this.settings.add(this.AnchoorHole = new BoolSettings("AnchoorHole", true, this));
      this.settings.add(this.Step = new BoolSettings("Step", false, this));
      this.settings
         .add(
            this.StepUpMode = new ModeSettings(
               "StepUpMode", "Vanilla2m", this, new String[]{"Vanilla1m", "Vanilla2m", "Vanilla3m", "Vanilla4m", "Matrix"}, () -> this.Step.getBool()
            )
         );
      this.settings.add(this.ReverseStep = new BoolSettings("ReverseStep", false, this));
      this.settings.add(this.NoSlowSneak = new BoolSettings("NoSlowSneak", false, this));
      this.settings
         .add(
            this.SneakSlowBypass = new ModeSettings(
               "SneakSlowBypass", "Vanilla", this, new String[]{"Vanilla", "Matrix", "NCP", "Grim"}, () -> this.NoSlowSneak.getBool()
            )
         );
      this.settings.add(this.NoSlowLay = new BoolSettings("NoSlowLay", false, this, () -> Minecraft.player.hasNewVersionMoves));
      this.settings
         .add(
            this.LaySlowBypass = new ModeSettings(
               "LaySlowBypass",
               "Matrix",
               this,
               new String[]{"Vanilla", "Matrix", "NCP", "Grim"},
               () -> this.NoSlowLay.getBool() && Minecraft.player.hasNewVersionMoves
            )
         );
      this.settings.add(this.TrapdoorSpeed = new BoolSettings("TrapdoorSpeed", false, this));
      this.settings.add(this.GroundHalt = new BoolSettings("GroundHalt", false, this));
      this.settings.add(this.LevitateControl = new BoolSettings("LevitateControl", false, this));
      this.settings.add(this.NoSlowSoul = new BoolSettings("NoSlowSoul", false, this));
      this.settings.add(this.FastPilingUp = new BoolSettings("FastPilingUp", false, this));
      this.settings.add(this.IgnoreHunger = new BoolSettings("IgnoreHunger", false, this));
      this.settings.add(this.SkipWorldboder = new BoolSettings("SkipWorldboder", false, this));
      this.settings.add(this.AutoСrouch = new BoolSettings("AutoСrouch", false, this, () -> NewPhisicsFixes.isNewVersion()));
      this.setDemand(1, 3);
   }

   public boolean isPreCrouchSpoof() {
      if (this.actived && this.AutoСrouch.getBool() && Minecraft.player.hasNewVersionMoves && Minecraft.player.onGround) {
         float predictPC = 2.5F;
         Vec3d pos = new Vec3d(Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ);
         Vec3d nextPos = pos.addVector(MathUtils.clamp(Entity.Getmotionx * 2.5, -1.0, 1.0), 0.0, MathUtils.clamp(Entity.Getmotionz * 2.5, -1.0, 1.0));
         double wD2 = (double)Minecraft.player.width / 2.0 - 1.0E-4F;
         double h = (double)Minecraft.player.height;
         double h2 = Math.min((double)Minecraft.player.height, 1.5);
         AxisAlignedBB aabbMin = new AxisAlignedBB(pos.addVector(-wD2, 0.1F, -wD2), pos.addVector(wD2, h2, wD2));
         if (mc.world.getCollisionBoxes(Minecraft.player, aabbMin).isEmpty()) {
            AxisAlignedBB nextAABB = new AxisAlignedBB(nextPos.addVector(-wD2, 0.1F, -wD2), nextPos.addVector(wD2, h, wD2));
            if (!mc.world.getCollisionBoxes(Minecraft.player, nextAABB).isEmpty()) {
               AxisAlignedBB nextAABBMin = new AxisAlignedBB(nextPos.addVector(-wD2, 0.1F, -wD2), nextPos.addVector(wD2, h2, wD2));
               return mc.world.getCollisionBoxes(Minecraft.player, nextAABBMin).isEmpty();
            }
         }
      }

      return false;
   }

   @EventTarget
   public void onSlowSneak(EventSlowSneak event) {
      if (this.NoSlowSneak.getBool()) {
         if (!Minecraft.player.movementInput.sneak && !Minecraft.player.isSneaking()) {
            return;
         }

         if (Velocity.get.isActived()
            && Velocity.get.OnKnockBack.getBool()
            && !Velocity.pass
            && Velocity.get.KnockType.currentMode.equalsIgnoreCase("Sneaking")
            && Velocity.get.sneakTicks > 0) {
            return;
         }

         String var2 = this.SneakSlowBypass.currentMode;
         switch (var2) {
            case "Vanilla":
               event.cancel();
               break;
            case "Matrix":
               event.cancel();
               if (Minecraft.player.ticksExisted % 2 == 0 && Minecraft.player.onGround && !Minecraft.player.isJumping()) {
                  Minecraft.player.multiplyMotionXZ(Minecraft.player.moveStrafing == 0.0F ? 0.5F : 0.4F);
               } else if ((double)Minecraft.player.fallDistance > 0.725 && (double)Minecraft.player.fallDistance <= 2.5
                  || (double)Minecraft.player.fallDistance > 2.5) {
                  Minecraft.player
                     .multiplyMotionXZ(
                        (double)Minecraft.player.fallDistance > 1.15
                           ? ((double)Minecraft.player.fallDistance > 1.4 ? 0.9375F : 0.9575F)
                           : (Minecraft.player.moveStrafing == 0.0F ? 0.9725F : 0.9675F)
                     );
               }
               break;
            case "NCP":
               if (Minecraft.player.isJumping()) {
                  event.setSlowFactor(0.82);
               } else if (Minecraft.player.onGround) {
                  event.setSlowFactor(Minecraft.player.moveStrafing == 0.0F ? 0.62 : 0.44);
               }
               break;
            case "Grim":
               if (MoveMeHelp.getSpeed() < 0.2 && Minecraft.player.onGround) {
                  event.setSlowFactor(Minecraft.player.moveStrafing == 0.0F ? 0.799F : 0.65F);
               }
         }
      }
   }

   @EventTarget
   public void onSlowLay(EventSlowLay event) {
      if (this.NoSlowLay.getBool()) {
         String var2 = this.LaySlowBypass.currentMode;
         switch (var2) {
            case "Vanilla":
               event.cancel();
               break;
            case "Matrix":
               event.cancel();
               if (Minecraft.player.ticksExisted % 2 == 0 && Minecraft.player.onGround && !Minecraft.player.isJumping()) {
                  Minecraft.player.multiplyMotionXZ(Minecraft.player.moveStrafing == 0.0F ? 0.5F : 0.4F);
               } else if ((double)Minecraft.player.fallDistance > 0.725 && (double)Minecraft.player.fallDistance <= 2.5
                  || (double)Minecraft.player.fallDistance > 2.5) {
                  Minecraft.player
                     .multiplyMotionXZ(
                        (double)Minecraft.player.fallDistance > 1.15
                           ? ((double)Minecraft.player.fallDistance > 1.4 ? 0.9375F : 0.9575F)
                           : (Minecraft.player.moveStrafing == 0.0F ? 0.9725F : 0.9675F)
                     );
               }
               break;
            case "NCP":
               if (Minecraft.player.isJumping()) {
                  event.setSlowFactor(0.82);
               } else if (Minecraft.player.onGround) {
                  event.setSlowFactor(Minecraft.player.moveStrafing == 0.0F ? 0.62 : 0.44);
               }
               break;
            case "Grim":
               if (MoveMeHelp.getSpeed() < 0.2 && Minecraft.player.onGround) {
                  event.setSlowFactor(Minecraft.player.moveStrafing == 0.0F ? 0.799F : 0.65F);
               }
         }
      }
   }

   public static boolean stopSlowingSoul(Entity entity) {
      boolean can = false;
      if (entity instanceof EntityPlayerSP SP && instance.actived && instance.NoSlowSoul.getBool()) {
         SP.multiplyMotionXZ(SP.movementInput.jump ? 0.8F : 0.855F);
         can = true;
      }

      return can;
   }

   @Override
   public void onUpdate() {
      if (this.TrapdoorSpeed.getBool()
         && MoveMeHelp.getSpeed() > 0.14
         && !Minecraft.player.isSneaking()
         && MoveMeHelp.trapdoorAdobedEntity(Minecraft.player)
         && (!Minecraft.player.isJumping() || Minecraft.player.jumpTicks != 0)
         && Minecraft.player.onGround) {
         Minecraft.player.jump();
      }

      if (this.FastPilingUp.getBool()) {
         AxisAlignedBB B = Minecraft.player.boundingBox;
         if (!Minecraft.player.onGround
            && Minecraft.player.motionY == 0.08307781780646721
            && !mc.world.getCollisionBoxes(Minecraft.player, B.offsetMinDown(0.25)).isEmpty()
            && mc.world.getCollisionBoxes(Minecraft.player, new AxisAlignedBB(B.minX, B.minY, B.minZ, B.maxX, B.minY + 1.0, B.maxZ)).isEmpty()) {
            Entity.motiony = -1.0;
         }
      }

      if (this.LevitateControl.getBool()) {
         boolean isLevitating = Minecraft.player.isPotionActive(Potion.getPotionById(25));
         double motionY = Minecraft.player.motionY;
         if (isLevitating) {
            motionY = Minecraft.player.isJumping() ? 0.8 - 0.08 * Math.random() : (Minecraft.player.isSneaking() ? 0.0 : motionY);
         }

         Minecraft.player.motionY = motionY;
      }

      if (this.GroundHalt.getBool()
         && Minecraft.player.onGround
         && Minecraft.player.isCollidedVertically
         && MoveMeHelp.getSpeed() < 0.15
         && !MoveMeHelp.moveKeysPressed()) {
         Minecraft.player.multiplyMotionXZ(0.45F);
      }

      if (this.NoJumpDelay.getBool()
         && (
            MoveMeHelp.isBlockAboveHead()
               || (double)Minecraft.player.fallDistance < 0.25
               || !mc.world
                  .getCollisionBoxes(Minecraft.player, new AxisAlignedBB(Minecraft.player.getPositionVector()).expand(0.3, 0.0, 0.3).offsetMinDown(0.25))
                  .isEmpty()
         )) {
         Minecraft.player.jumpTicks = 0;
      }

      if (this.GmFlyStrafe.getBool() && Minecraft.player.capabilities.isFlying) {
         float min = 0.23F;
         float max = 1.199F;
         float motY = (float)(Entity.Getmotiony + (double)(Minecraft.player.isJumping() ? 1 : (Minecraft.player.isSneaking() ? -1 : 0)));
         if (this.FlyStrafeMode.currentMode.equalsIgnoreCase("Vanilla")) {
            min = this.GMFlySpeedXZ.getFloat() / 2.0F;
            max = this.GMFlySpeedXZ.getFloat();
            motY = (float)(
               Entity.Getmotiony
                  + (double)(Minecraft.player.isJumping() ? this.GMFlySpeedY.getFloat() : (Minecraft.player.isSneaking() ? -this.GMFlySpeedY.getFloat() : 0.0F))
            );
         } else if (this.FlyStrafeMode.currentMode.equalsIgnoreCase("NCP")) {
            min = 0.6F;
            max = 1.152F;
            motY = (float)(
               Entity.Getmotiony
                  + (
                     Minecraft.player.isJumping()
                        ? (Minecraft.player.isInWater() ? 0.61 : 0.7)
                        : (
                           Minecraft.player.isSneaking()
                              ? -(Speed.posBlock(Minecraft.player.posX, Minecraft.player.posY - 3.0, Minecraft.player.posZ) ? 0.5 : 2.6)
                              : 0.0
                        )
                  )
            );
         }

         if (!this.FlyStrafeMode.currentMode.equalsIgnoreCase("Matrix&AAC")) {
            if (this.FlyStrafeMode.getMode().equalsIgnoreCase("Matrix&NCP")) {
               if (Minecraft.player.rotationYaw - Minecraft.player.lastReportedYaw == 0.0F
                  && Minecraft.player.rotationPitch - EntityPlayerSP.lastReportedPitch == 0.0F
                  && !MoveMeHelp.isMoving()
                  && !Minecraft.player.isSneaking()
                  && !Minecraft.player.isJumping()) {
                  this.notRotTime.reset();
               }

               int tickFly = (int)(this.notRotTime.getTime() / 50L);
               if (tickFly > 10) {
                  mc.timer.tempSpeed *= tickFly % 2 == 0 ? 1.075F : 1.15F;
               }

               min = 0.9F;
               float calcMotionY = (float)Math.max(Math.abs(Minecraft.player.motionY) * 3.0, 0.8F);
               float speedY = MathUtils.clamp(calcMotionY, -4.0F, 4.0F);
               motY = (float)(Entity.Getmotiony + (double)(Minecraft.player.isJumping() ? speedY : (Minecraft.player.isSneaking() ? -speedY : 0.0F)));
            } else if (this.FlyStrafeMode.getMode().equalsIgnoreCase("NcpNew")) {
               max = 1.2672F;
               if (Minecraft.player.rotationYaw - Minecraft.player.lastReportedYaw == 0.0F
                  && Minecraft.player.rotationPitch - EntityPlayerSP.lastReportedPitch == 0.0F
                  && !MoveMeHelp.isMoving()
                  && !Minecraft.player.isSneaking()
                  && !Minecraft.player.isJumping()) {
                  this.notRotTime.reset();
               }

               int tickFly = (int)(this.notRotTime.getTime() / 50L);
               if (tickFly > 20) {
                  mc.timer.tempSpeed = mc.timer.tempSpeed * (double)(1.05F + MathUtils.valWave01((float)(tickFly % 6) / 6.0F) * 0.2F);
               }

               min = 0.9F;
               motY = Minecraft.player.isJumping()
                  ? (Minecraft.player.isInLiquid() ? 2.0F : 2.1F)
                  : (Minecraft.player.isSneaking() ? -(Minecraft.player.isInLiquid() ? 1.5F : 4.0F) : 0.0F);
               if (Minecraft.player.isSneaking()) {
                  max /= 2.0F;
               }
            } else {
               this.notRotTime.reset();
               mc.timer.tempSpeed = 1.0;
            }
         } else {
            if (Minecraft.player.rotationYaw - Minecraft.player.lastReportedYaw != 0.0F
               || Minecraft.player.rotationPitch - EntityPlayerSP.lastReportedPitch != 0.0F
               || !MoveMeHelp.isMoving()
               || Minecraft.player.isSneaking()) {
               this.notRotTime.reset();
            }

            if (mc.world != null) {
               double offMotion = 8.0;
               if (mc.world
                        .rayTraceBlocks(
                           Minecraft.player.getPositionVector().addVector(-offMotion, 0.0, 0.0),
                           Minecraft.player.getPositionVector().addVector(offMotion, 0.0, 0.0)
                        )
                     != null
                  || mc.world
                        .rayTraceBlocks(
                           Minecraft.player.getPositionVector().addVector(0.0, -offMotion, 0.0),
                           Minecraft.player.getPositionVector().addVector(0.0, offMotion, 0.0)
                        )
                     != null
                  || mc.world
                        .rayTraceBlocks(
                           Minecraft.player.getPositionVector().addVector(0.0, 0.0, -offMotion),
                           Minecraft.player.getPositionVector().addVector(0.0, 0.0, offMotion)
                        )
                     != null) {
                  this.notRotTime.reset();
               }
            }

            mc.timer.tempSpeed = 1.0;
            if (this.notRotTime.hasReached(50.0)) {
               mc.timer.tempSpeed = 1.0 + MathUtils.easeInOutQuadWave((double)(((float)this.notRotTime.getTime() - 50.0F) / 500.0F)) * 2.75;
               if (this.notRotTime.hasReached(550.0)) {
                  this.notRotTime.reset();
               }

               Timer.get.setTempCancel();
            }
         }

         double speed = MathUtils.clamp(MoveMeHelp.getSpeed() * 1.5, (double)min, (double)max);
         double speed2 = MathUtils.clamp(MoveMeHelp.getSpeed() * 1.5, (double)min, (double)max);
         MoveMeHelp.setSpeed(speed2, 0.8F);
         if (MoveMeHelp.isMoving()) {
            MoveMeHelp.setCuttingSpeed(speed / 1.06);
         }

         Minecraft.player.motionY = (double)motY;
         Minecraft.player.motionY /= 2.0;
      }

      if (this.FastLadders.getBool()) {
         if (Minecraft.player.isOnLadder()) {
            MoveMeHelp.setCuttingSpeed((Minecraft.player.ticksExisted % 2 == 0 ? 0.2498 : 0.2499) / 1.06);
            Minecraft.player.motionY = 0.0;
            Entity.motiony = Minecraft.player.isJumping()
               ? 0.12
               : (Minecraft.player.isSneaking() ? -1.0 : (Minecraft.player.ticksExisted % 2 == 0 ? 0.0032 : -0.0032));
            if (mc.timer.speed == 1.0) {
               mc.timer.speed = 1.04832343;
            }
         } else if (mc.timer.speed == 1.04832343) {
            mc.timer.speed = 1.0;
         }
      }

      if (this.MatrixSnowFix.getBool()) {
         if (BlockHelper.getBlock(new BlockPos(Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ)) == Blocks.SNOW_LAYER
            && BlockHelper.getBlock(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - 1.0, Minecraft.player.posZ)) == Blocks.SOUL_SAND) {
            this.canPress = true;
            float ex = 1.0F;
            float ex2 = 1.0F;
            Minecraft.player.jumpTicks = 0;
            if ((
                  mc.world.getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - (double)ex, Minecraft.player.posZ)).getBlock()
                        != Blocks.AIR
                     || mc.world.getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - (double)ex, Minecraft.player.posZ)).getBlock()
                        != Blocks.AIR
                     || mc.world
                           .getBlockState(
                              new BlockPos(Minecraft.player.posX - (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ - (double)ex2)
                           )
                           .getBlock()
                        != Blocks.AIR
                     || mc.world
                           .getBlockState(
                              new BlockPos(Minecraft.player.posX + (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ + (double)ex2)
                           )
                           .getBlock()
                        != Blocks.AIR
                     || mc.world
                           .getBlockState(
                              new BlockPos(Minecraft.player.posX - (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ + (double)ex2)
                           )
                           .getBlock()
                        != Blocks.AIR
                     || mc.world
                           .getBlockState(
                              new BlockPos(Minecraft.player.posX + (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ - (double)ex2)
                           )
                           .getBlock()
                        != Blocks.AIR
                     || mc.world
                           .getBlockState(new BlockPos(Minecraft.player.posX + (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ))
                           .getBlock()
                        != Blocks.AIR
                     || mc.world
                           .getBlockState(new BlockPos(Minecraft.player.posX - (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ))
                           .getBlock()
                        != Blocks.AIR
                     || mc.world
                           .getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - (double)ex, Minecraft.player.posZ + (double)ex2))
                           .getBlock()
                        != Blocks.AIR
                     || mc.world
                           .getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - (double)ex, Minecraft.player.posZ - (double)ex2))
                           .getBlock()
                        != Blocks.AIR
               )
               && mc.world.getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - (double)ex, Minecraft.player.posZ)).getMaterial()
                  != Material.SNOW
               && mc.world.getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - (double)ex, Minecraft.player.posZ)).getMaterial()
                  != Material.SNOW
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX - (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ - (double)ex2))
                     .getMaterial()
                  != Material.SNOW
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX + (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ + (double)ex2))
                     .getMaterial()
                  != Material.SNOW
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX - (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ + (double)ex2))
                     .getMaterial()
                  != Material.SNOW
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX + (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ - (double)ex2))
                     .getMaterial()
                  != Material.SNOW
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX + (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ))
                     .getMaterial()
                  != Material.SNOW
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX - (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ))
                     .getMaterial()
                  != Material.SNOW
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - (double)ex, Minecraft.player.posZ + (double)ex2))
                     .getMaterial()
                  != Material.SNOW
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - (double)ex, Minecraft.player.posZ - (double)ex2))
                     .getMaterial()
                  != Material.SNOW
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - (double)ex, Minecraft.player.posZ))
                     .getMaterial()
                     .getMaterialMapColor()
                     .colorIndex
                  != 7
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - (double)ex, Minecraft.player.posZ))
                     .getMaterial()
                     .getMaterialMapColor()
                     .colorIndex
                  != 7
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX - (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ - (double)ex2))
                     .getMaterial()
                     .getMaterialMapColor()
                     .colorIndex
                  != 7
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX + (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ + (double)ex2))
                     .getMaterial()
                     .getMaterialMapColor()
                     .colorIndex
                  != 7
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX - (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ + (double)ex2))
                     .getMaterial()
                     .getMaterialMapColor()
                     .colorIndex
                  != 7
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX + (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ - (double)ex2))
                     .getMaterial()
                     .getMaterialMapColor()
                     .colorIndex
                  != 7
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX + (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ))
                     .getMaterial()
                     .getMaterialMapColor()
                     .colorIndex
                  != 7
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX - (double)ex2, Minecraft.player.posY - (double)ex, Minecraft.player.posZ))
                     .getMaterial()
                     .getMaterialMapColor()
                     .colorIndex
                  != 7
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - (double)ex, Minecraft.player.posZ + (double)ex2))
                     .getMaterial()
                     .getMaterialMapColor()
                     .colorIndex
                  != 7
               && mc.world
                     .getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - (double)ex, Minecraft.player.posZ - (double)ex2))
                     .getMaterial()
                     .getMaterialMapColor()
                     .colorIndex
                  != 7
               && !MoveMeHelp.isBlockAboveHead()
               && !Minecraft.player.isCollidedHorizontally) {
               Minecraft.player.onGround = true;
               MoveMeHelp.setSpeed(MoveMeHelp.getSpeed());
            }
         } else {
            this.canPress = false;
         }

         if (mc.currentScreen == null || this.canPress) {
            mc.gameSettings.keyBindJump.pressed = this.canPress || Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
         }
      }

      if (this.StairSpeed.getBool()) {
         if (stairTick
            && (this.NoJumpDelay.getBool() || ++tickCounter % 2 == 0)
            && !Minecraft.player.isJumping()
            && Minecraft.player.rayGround == Minecraft.player.onGround) {
            double prev = Minecraft.player.motionY;
            Minecraft.player.jump();
            Minecraft.player.motionY = prev;
         }

         Minecraft.player.rayGround = Minecraft.player.onGround;
         if (!stairTick) {
            tickCounter = 0;
         }

         stairTick = false;
      }

      if (this.NoSlowDown.getBool()) {
         if (this.NoSlowMode.currentMode.equalsIgnoreCase("Vanilla")) {
            Minecraft.player.skipStopSprintOnEat = true;
            Minecraft.player.tempSlowEatingFactor = MathUtils.clamp(this.NoSlowPercent.getFloat() / 100.0F, 0.0F, 1.0F);
         } else if (this.NoSlowMode.currentMode.equalsIgnoreCase("MatrixOld")) {
            if ((!Minecraft.player.isEating() || !Minecraft.player.isBlocking() || !Minecraft.player.isBowing() || !Minecraft.player.isDrinking())
               && mc.timer.speed == 1.094745F) {
               mc.timer.speed = 1.0;
            }

            if (Minecraft.player.isEating() || Minecraft.player.isBlocking() || Minecraft.player.isBowing() || Minecraft.player.isDrinking()) {
               if (mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown() || Minecraft.player.isMoving()) {
                  Minecraft.player.applyEntityCollision(Minecraft.player);
                  mc.timer.speed = 1.094745F;
               }

               if (mc.gameSettings.keyBindSprint.isKeyDown() && mc.gameSettings.keyBindForward.isKeyDown() && !Minecraft.player.isSprinting()) {
                  Minecraft.player.setSprinting(this.actived);
               }

               if (!Minecraft.player.isMoving() && mc.gameSettings.keyBindForward.isKeyDown() && !Minecraft.player.isJumping()) {
                  Minecraft.player.motionX *= 0.31F;
                  Minecraft.player.motionZ *= 0.31F;
               }

               if (Minecraft.player.isJumping() && mc.gameSettings.keyBindForward.isKeyDown() && !Minecraft.player.isMoving()) {
                  Minecraft.player.motionX *= 0.97245F;
                  Minecraft.player.motionZ *= 0.97245F;
               }

               if (Minecraft.player.isMoving() && mc.gameSettings.keyBindForward.isKeyDown() && !Minecraft.player.isJumping()) {
                  Minecraft.player.motionX *= 0.425F;
                  Minecraft.player.motionZ *= 0.425F;
               }

               if (Minecraft.player.isMoving() && mc.gameSettings.keyBindForward.isKeyDown() && Minecraft.player.isJumping()) {
                  Minecraft.player.motionX *= 0.9725F;
                  Minecraft.player.motionZ *= 0.9725F;
               }

               if (!Minecraft.player.isMoving() && mc.gameSettings.keyBindBack.isKeyDown() && !Minecraft.player.isJumping()) {
                  Minecraft.player.motionX *= 0.6645F;
                  Minecraft.player.motionZ *= 0.6645F;
               }

               if (Minecraft.player.isJumping() && mc.gameSettings.keyBindBack.isKeyDown() && !Minecraft.player.isMoving()) {
                  Minecraft.player.motionX *= 0.9845F;
                  Minecraft.player.motionZ *= 0.9845F;
               }

               if (Minecraft.player.isMoving() && mc.gameSettings.keyBindBack.isKeyDown() && Minecraft.player.isJumping()) {
                  Minecraft.player.motionX *= 0.9845F;
                  Minecraft.player.motionZ *= 0.9845F;
               }

               if (Minecraft.player.isMoving() && mc.gameSettings.keyBindBack.isKeyDown() && !Minecraft.player.isJumping()) {
                  Minecraft.player.motionX *= 0.64F;
                  Minecraft.player.motionZ *= 0.64F;
               }

               if (Minecraft.player.isMoving() && !mc.gameSettings.keyBindBack.isKeyDown() && !Minecraft.player.isJumping()) {
                  Minecraft.player.motionX *= 0.6645F;
                  Minecraft.player.motionZ *= 0.6645F;
               }
            }

            Minecraft.player.skipStopSprintOnEat = true;
            Minecraft.player.tempSlowEatingFactor = 1.0F;
         } else if (this.NoSlowMode.currentMode.equalsIgnoreCase("MatrixLatest")) {
            boolean stop = Minecraft.player.isInWater()
               || Minecraft.player.isInLava()
               || Minecraft.player.isInWeb
               || Minecraft.player.capabilities.isFlying
               || Minecraft.player.getTicksElytraFlying() > 1
               || !MoveMeHelp.isMoving();
            boolean ignoreOffHandGround = false;
            if (Minecraft.player.isHandActive() && Minecraft.player.getItemInUseMaxCount() > 3 && !stop) {
               if (Minecraft.player.ticksExisted % 2 != 0
                  || !Minecraft.player.onGround
                  || Minecraft.player.isJumping()
                  || ignoreOffHandGround && Minecraft.player.getActiveHand() != null && Minecraft.player.getActiveHand() == EnumHand.OFF_HAND) {
                  if ((double)Minecraft.player.fallDistance > 0.725 && (double)Minecraft.player.fallDistance <= 2.5
                     || (double)Minecraft.player.fallDistance > 2.5) {
                     Minecraft.player
                        .multiplyMotionXZ(
                           (double)Minecraft.player.fallDistance > 1.15
                              ? ((double)Minecraft.player.fallDistance > 1.4 ? 0.9375F : 0.9575F)
                              : (Minecraft.player.moveStrafing == 0.0F ? 0.9725F : 0.9675F)
                        );
                  }
               } else {
                  Minecraft.player.multiplyMotionXZ(Minecraft.player.moveStrafing == 0.0F ? 0.5F : 0.4F);
               }
            }

            Minecraft.player.skipStopSprintOnEat = true;
            Minecraft.player.tempSlowEatingFactor = 1.0F;
         } else if (this.NoSlowMode.currentMode.equalsIgnoreCase("AACOld")) {
            Minecraft.player.skipStopSprintOnEat = true;
            Minecraft.player.tempSlowEatingFactor = 1.0F;
            if (Minecraft.player.isHandActive()) {
               if (mc.timer.speed == 1.1F) {
                  mc.timer.speed = 1.0;
               }

               if (Minecraft.player.onGround && !Minecraft.player.isJumping()) {
                  Minecraft.player.multiplyMotionXZ(0.601F);
               }

               if (Minecraft.player.isJumping()) {
                  mc.timer.speed = 1.0;
                  if (Minecraft.player.onGround) {
                     mc.timer.speed = 1.1F;
                     Minecraft.player.multiplyMotionXZ(0.45F);
                  } else {
                     Minecraft.player.jumpMovementFactor = 0.02F;
                  }
               }
            } else if (mc.timer.speed == 1.1) {
               mc.timer.speed = 1.0;
            }
         } else if (this.NoSlowMode.currentMode.equalsIgnoreCase("NCP+")) {
            boolean bypassed = Minecraft.player.getActiveHand() == EnumHand.MAIN_HAND && this.NCPFapBypass.getBool();
            if (!bypassed
               && Minecraft.player.isHandActive()
               && Minecraft.player.getActiveHand() == EnumHand.MAIN_HAND
               && !Minecraft.player.isBlocking()
               && Minecraft.player.getActiveHand() == EnumHand.OFF_HAND
               && !EntityLivingBase.isMatrixDamaged
               && !Minecraft.player.isInWater()
               && !Minecraft.player.isInLava()) {
               float pc = MathUtils.clamp((float)Minecraft.player.getItemInUseMaxCount() / 28.0F, 0.0F, 1.0F);
               float noslowPercent = 1.0F - MathUtils.clamp((Minecraft.player.onGround ? 0.43F : (Entity.Getmotiony > 0.0 ? 0.57F : 0.24F)) * pc, 0.0F, 0.3F);
               Minecraft.player.multiplyMotionXZ(noslowPercent);
               Minecraft.player.skipStopSprintOnEat = true;
               Minecraft.player.tempSlowEatingFactor = 1.0F;
            }

            if (bypassed && Minecraft.player.getItemInUseMaxCount() == 1) {
               Minecraft.player.connection.sendPacket(new CPacketHeldItemChange((Minecraft.player.inventory.currentItem + 1) % 8));
               Minecraft.player.connection.sendPacket(new CPacketHeldItemChange(Minecraft.player.inventory.currentItem));
            }

            boolean stop = Minecraft.player.isInWater()
               || Minecraft.player.isInLava()
               || Minecraft.player.isInWeb
               || Minecraft.player.capabilities.isFlying
               || Minecraft.player.getTicksElytraFlying() > 1
               || !MoveMeHelp.isMoving();
            if (Minecraft.player.isHandActive()
               && Minecraft.player.getItemInUseMaxCount() > 3
               && !stop
               && Minecraft.player.ticksExisted % 2 == 0
               && Minecraft.player.onGround
               && !Minecraft.player.isJumping()) {
               Minecraft.player.multiplyMotionXZ(Minecraft.player.moveStrafing == 0.0F ? 0.5F : 0.4F);
            }

            Minecraft.player.skipStopSprintOnEat = true;
            Minecraft.player.tempSlowEatingFactor = 1.0F;
         } else if (this.NoSlowMode.currentMode.equalsIgnoreCase("Grim")) {
            if (Minecraft.player.isHandActive()) {
               if (Minecraft.player.getItemInUseMaxCount() >= 2 || !Minecraft.player.onGround) {
                  Minecraft.player.tempSlowEatingFactor = 1.0F;
                  Minecraft.player.skipStopSprintOnEat = true;
               }

               if (Minecraft.player.onGround && !Minecraft.player.isJumping()) {
                  boolean speed = Minecraft.player.getActivePotionEffect(MobEffects.SPEED) != null;
                  Minecraft.player
                     .multiplyMotionXZ(
                        speed
                           ? (Minecraft.player.getItemInUseMaxCount() % 2 == 0 ? 0.7F : 0.719F)
                           : (Minecraft.player.getItemInUseMaxCount() % 2 == 0 ? 0.85F : 0.9F)
                     );
               } else if (!this.NoJumpSlowGrim.getBool()
                  && Minecraft.player.getItemInUseMaxCount() == 32
                  && Minecraft.player.getActiveItemStack() != null
                  && Minecraft.player.getActiveItemStack().getItem() instanceof ItemFood) {
                  Minecraft.player.connection.sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.STOP_SPRINTING));
                  Minecraft.player.multiplyMotionXZ(0.5F);
               }
            }
         } else if (this.NoSlowMode.currentMode.equalsIgnoreCase("Intave")) {
            if (Minecraft.player.isHandActive()) {
               boolean slowRight = Minecraft.player.getActiveHand().equals(EnumHand.MAIN_HAND) && !this.NoJumpSlowGrim.getBool();
               if (slowRight) {
                  List<EntityLivingBase> players = mc.world
                     .getLoadedEntityList()
                     .stream()
                     .map(Entity::getLivingBaseOf)
                     .filter(Objects::nonNull)
                     .filter(player -> player != Minecraft.player && player.isEntityAlive() && (double)Minecraft.player.getDistanceToEntity(player) < 1.8)
                     .collect(Collectors.toList());
                  if (!players.isEmpty()) {
                     slowRight = false;
                  }
               }

               int activeTicks = Minecraft.player.getItemInUseMaxCount();
               if (activeTicks == 1 || activeTicks % 4 == 3) {
                  if (Minecraft.player.getActiveHand().equals(EnumHand.OFF_HAND)) {
                     Minecraft.player.connection.sendPacket(new CPacketHeldItemChange((Minecraft.player.inventory.currentItem + 1) % 8));
                     Minecraft.player.connection.sendPacket(new CPacketHeldItemChange(Minecraft.player.inventory.currentItem));
                  } else if (!slowRight) {
                     Minecraft.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.OFF_HAND));
                  }
               }

               if (MathUtils.getDifferenceOf(Minecraft.player.lastTickPosY, Minecraft.player.posY) > 0.0) {
                  Minecraft.player.skipStopSprintOnEat = !slowRight;
                  Minecraft.player.tempSlowEatingFactor = slowRight ? 0.2F : 1.0F;
               } else if (Minecraft.player.onGround) {
                  boolean speedPot = Minecraft.player.isPotionActive(MobEffects.SPEED);
                  Minecraft.player.skipStopSprintOnEat = activeTicks > (speedPot ? 4 : 3) && !slowRight;
                  if (activeTicks == 1) {
                     Minecraft.player.multiplyMotionXZ(speedPot && slowRight ? 0.25F : 0.5F);
                     if (Minecraft.player.isSprinting()) {
                        Minecraft.player.setSprinting(false);
                        Minecraft.player.connection.sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.STOP_SPRINTING));
                     }
                  }

                  if (!slowRight && activeTicks <= 2) {
                     Minecraft.player.tempSlowEatingFactor = 0.1F;
                  } else {
                     Minecraft.player.tempSlowEatingFactor = 1.0F;
                  }

                  if (slowRight) {
                     boolean f = Minecraft.player.movementInput.forwardKeyDown;
                     boolean b = Minecraft.player.movementInput.backKeyDown;
                     boolean l = Minecraft.player.movementInput.leftKeyDown;
                     boolean r = Minecraft.player.movementInput.rightKeyDown;
                     boolean diagonal = (f || b) && f != b && (l || r) && l != r;
                     Minecraft.player.multiplyMotionXZ((diagonal ? 0.614F : 0.659F) * (speedPot ? 0.8F : 1.0F));
                  }
               }
            }
         } else if (this.NoSlowMode.getMode().equalsIgnoreCase("NCPNew")) {
            boolean shield = Minecraft.player.getActiveItemStack() != null && Minecraft.player.getActiveItemStack().getItem() instanceof ItemShield;
            boolean set = !shield && Minecraft.player.isHandActive() && !Minecraft.player.isInWater() && !this.sneakPush;
            if (set != this.sneakPush) {
               this.sneakPush = set;
               if (!set || !Minecraft.player.isSneaking()) {
                  mc.getConnection()
                     .sendPacket(
                        new CPacketEntityAction(Minecraft.player, set ? CPacketEntityAction.Action.START_SNEAKING : CPacketEntityAction.Action.STOP_SNEAKING)
                     );
               }
            }

            if (this.sneakPush) {
               if (Minecraft.player.onGround && !Minecraft.player.isJumping()) {
                  Minecraft.player.skipStopSprintOnEat = true;
                  Minecraft.player.tempSlowEatingFactor = Minecraft.player.getItemInUseMaxCount() % 2 == 0 ? 0.9F : 1.0F;
               } else {
                  Minecraft.player.skipStopSprintOnEat = true;
                  Minecraft.player.tempSlowEatingFactor = 1.0F;
               }
            }

            if (shield) {
               Minecraft.player.skipStopSprintOnEat = false;
               Minecraft.player.tempSlowEatingFactor = 0.2F;
            }
         }
      }

      if (this.InWebMotion.getBool()) {
         if (this.WebMotionMode.currentMode.equalsIgnoreCase("NoCollide")) {
            Minecraft.player.isInWeb = false;
         } else if (this.WebMotionMode.currentMode.equalsIgnoreCase("Custom")) {
            if (Minecraft.player.isInWeb) {
               Minecraft.player.jumpMovementFactor = this.WebSpeedXZ.getFloat();
               Minecraft.player.motionY = 0.0;
               if (mc.gameSettings.keyBindJump.isKeyDown()) {
                  Minecraft.player.motionY = Minecraft.player.motionY + (double)(this.WebSpeedYPlus.getFloat() * 4.0F - 0.001F);
               }

               if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                  Minecraft.player.motionY = Minecraft.player.motionY - (double)(this.WebSpeedYMinus.getFloat() * 4.0F - 0.001F);
               }
            }
         } else if (this.WebMotionMode.currentMode.equalsIgnoreCase("Matrix")) {
            float ex = 0.1F;
            float ex2 = 0.01F;
            double x = Minecraft.player.posX;
            double y = Minecraft.player.posY;
            double z = Minecraft.player.posZ;
            if (Minecraft.player.isInWeb) {
               Minecraft.player.motionY = 0.0;
               Minecraft.player.jumpMovementFactor = 0.49F;
               if (mc.gameSettings.keyBindJump.isKeyDown()) {
                  Minecraft.player.motionY++;
               } else if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                  Minecraft.player.motionY--;
               }
            } else if (!getBlockWithExpand(Minecraft.player.width / 2.0F, x, y + (double)ex2, z, Blocks.WEB)
               && getBlockWithExpand(Minecraft.player.width / 2.0F, x, y - (double)ex, z, Blocks.WEB)
               && !Minecraft.player.isCollidedHorizontally
               && !Minecraft.player.onGround) {
               if (this.WebZoom.getBool() && Minecraft.player.motionY == -0.0784000015258789 && MoveMeHelp.getSpeed() < 0.2499) {
                  MoveMeHelp.setSpeed(1.484);
               }

               Minecraft.player.motionY = -0.06F;
            }
         }
      }
   }

   @Override
   public void onUpdateMovement() {
      if (this.AnchoorHole.getBool()) {
         holeTick = false;
         float w = Minecraft.player.width / 2.0F;
         double x = Minecraft.player.posX;
         double y = Minecraft.player.posY + (Minecraft.player.lastTickPosY - Minecraft.player.posY) / 2.0;
         double z = Minecraft.player.posZ;
         if ((
               this.isHoledPosFull(new BlockPos(x, y, z))
                  || this.isHoledPosFull(new BlockPos(x, y - 1.0, z))
                  || this.isHoledPosFull(new BlockPos(x, y - 1.3, z))
            )
            && getBlockFullWithExpand(w, x, y - 1.0, z, Blocks.AIR)
            && getBlockFullWithExpand(w, x, y - 1.3, z, Blocks.AIR)) {
            Minecraft.player.jumpMovementFactor = 0.0F;
            MoveMeHelp.setSpeed(0.0);
            MoveMeHelp.setCuttingSpeed(0.0);
            if (this.ReverseStep.getBool() && !Minecraft.player.isJumping() && Minecraft.player.fallDistance < 0.9F) {
               mc.timer.tempSpeed = 3.0;
               holeTicks = 0;
            } else {
               holeTicks = -10;
            }

            holeTick = true;
         }
      }

      if (this.Step.getBool()
         && this.StepUpMode.currentMode.equalsIgnoreCase("Matrix")
         && !Minecraft.player.isJumping()
         && Minecraft.player.isCollidedHorizontally
         && MoveMeHelp.isMoving()) {
         float moveYaw = (float)MoveMeHelp.getMotionYaw();
         float offsetY = 1.001336F;
         float extendXZ = 1.0E-5F;
         float sin = -MathHelper.sin(MathHelper.toRadians(moveYaw)) * extendXZ;
         float cos = MathHelper.cos(MathHelper.toRadians(moveYaw)) * extendXZ;
         AxisAlignedBB aabb = Minecraft.player.getEntityBoundingBoxCLCompact().offset(0.0, -0.42, 0.0);
         AxisAlignedBB aabbOff = aabb.offset((double)sin, (double)offsetY + 0.42, (double)cos);
         if (mc.world.getCollisionBoxes(Minecraft.player, aabbOff).isEmpty() && !mc.world.getCollisionBoxes(Minecraft.player, aabb).isEmpty()) {
            Minecraft.player.onGround = true;
            Minecraft.player.jump();
         }
      }

      if (this.ReverseStep.getBool()
         && Minecraft.player.onGround
         && Minecraft.player.isCollidedVertically
         && Minecraft.player.motionY < 0.0
         && !Speed.posBlock(Minecraft.player.posX, Minecraft.player.posY - 0.6, Minecraft.player.posZ)
         && OnFalling.getDistanceTofall() < 4.0
         && !Minecraft.player.isJumping()) {
         Entity.motiony = -2.0;
         holeTicks = 0;
      }

      if (this.Step.getBool()
         && this.StepUpMode.currentMode.contains("Vanilla")
         && !Minecraft.player.isInWater()
         && !Minecraft.player.isInLava()
         && !Minecraft.player.isInWeb
         && !Minecraft.player.isOnLadder()
         && !JesusSpeed.isSwimming) {
         if (!Minecraft.player.isSneaking() && MoveMeHelp.moveKeysPressed()) {
            holeTicks++;
         }

         if (holeTicks > 5 && !Minecraft.player.isSneaking() && MoveMeHelp.moveKeysPressed()) {
            float stepH = this.StepUpMode.currentMode.contains("4m")
               ? 4.000121F
               : (this.StepUpMode.currentMode.contains("3m") ? 3.000121F : (this.StepUpMode.currentMode.contains("2m") ? 2.000121F : 1.0001212F));
            Minecraft.player.stepHeight = stepH;
         }
      } else if (Minecraft.player.stepHeight == 4.000121F
         || Minecraft.player.stepHeight == 3.000121F
         || Minecraft.player.stepHeight == 2.000121F
         || Minecraft.player.stepHeight == 1.0001212F) {
         Minecraft.player.stepHeight = 0.6F;
      }
   }

   @Override
   public void onToggled(boolean actived) {
      if (!actived) {
         holeTicks = 0;
         holeTick = false;
         if (Minecraft.player.stepHeight == 4.000121F
            || Minecraft.player.stepHeight == 3.000121F
            || Minecraft.player.stepHeight == 2.000121F
            || Minecraft.player.stepHeight == 1.0001212F) {
            Minecraft.player.stepHeight = 0.6F;
         }

         this.sneakPush = false;
      }

      super.onToggled(actived);
   }

   private Block getBlock(BlockPos position) {
      return mc.world != null ? mc.world.getBlockState(position).getBlock() : Blocks.AIR;
   }

   private boolean isBedrock(BlockPos position) {
      Block state = Blocks.BEDROCK;
      return this.getBlock(position) == state;
   }

   private boolean isObsidian(BlockPos position) {
      Block state = Blocks.OBSIDIAN;
      return this.getBlock(position) == state;
   }

   private boolean isCurrentBlock(BlockPos position) {
      return this.isBedrock(position) || this.isObsidian(position);
   }

   private boolean isHoled(BlockPos position) {
      Block state = Blocks.AIR;
      return this.isCurrentBlock(position.add(1, 0, 0))
         && this.isCurrentBlock(position.add(-1, 0, 0))
         && this.isCurrentBlock(position.add(0, 0, 1))
         && this.isCurrentBlock(position.add(0, 0, -1))
         && Speed.posBlock((double)position.add(0, -1, 0).getX(), (double)position.add(0, -1, 0).getY(), (double)position.add(0, -1, 0).getZ())
         && this.getBlock(position) == state
         && this.getBlock(position.add(0, 1, 0)) == state
         && this.getBlock(position.add(0, 2, 0)) == state;
   }

   private boolean isHoledPosFull(BlockPos pos) {
      return this.isHoled(new BlockPos(pos.getX(), pos.getY(), pos.getZ())) && !Speed.posBlock((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
   }

   public static boolean getBlockWithExpand(float expand, double x, double y, double z, Block block) {
      return mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x + (double)expand, y, z + (double)expand)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x - (double)expand, y, z - (double)expand)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x + (double)expand, y, z - (double)expand)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x - (double)expand, y, z + (double)expand)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x + (double)expand, y, z)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x - (double)expand, y, z)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x, y, z + (double)expand)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x, y, z - (double)expand)).getBlock() == block;
   }

   public static boolean getBlockFullWithExpand(float expand, double x, double y, double z, Block block) {
      return mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == block
         && mc.world.getBlockState(new BlockPos(x + (double)expand, y, z + (double)expand)).getBlock() == block
         && mc.world.getBlockState(new BlockPos(x - (double)expand, y, z - (double)expand)).getBlock() == block
         && mc.world.getBlockState(new BlockPos(x + (double)expand, y, z - (double)expand)).getBlock() == block
         && mc.world.getBlockState(new BlockPos(x - (double)expand, y, z + (double)expand)).getBlock() == block
         && mc.world.getBlockState(new BlockPos(x + (double)expand, y, z)).getBlock() == block
         && mc.world.getBlockState(new BlockPos(x - (double)expand, y, z)).getBlock() == block
         && mc.world.getBlockState(new BlockPos(x, y, z + (double)expand)).getBlock() == block
         && mc.world.getBlockState(new BlockPos(x, y, z - (double)expand)).getBlock() == block;
   }

   @EventTarget
   public void onSprintSetEvent(EventSprintBlock event) {
   }
}
