package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.Event3D;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventReceivePacket;
import ru.govno.client.event.events.EventSendPacket;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.InventoryUtil;
import ru.govno.client.utils.MusicHelper;
import ru.govno.client.utils.Math.BlockUtils;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class Criticals extends Module {
   public static Criticals get;
   private final AnimationUtils usingProgress = new AnimationUtils(0.0F, 0.0F, 0.1F);
   private float indicatorScale = 0.0F;
   private float radiusPlus = 0.0F;
   public BoolSettings EntityHit;
   public BoolSettings Bowing;
   public BoolSettings BowingAwpSound;
   public BoolSettings SilentBurrowBlock;
   public BoolSettings VehicleInstakill;
   public ModeSettings HitMode;
   public ModeSettings BowMode;
   private int ticksInningShoot;
   private static boolean doAddPacket;
   private static boolean groundS;
   static TimerHelper timeCancel = new TimerHelper();
   static float yawS;
   static float pitchS;
   private final TimerHelper timeFix = TimerHelper.TimerHelperReseted();
   private final List<Criticals.TimedVec2s> TIMED_VECS2S_LIST = new ArrayList<>();
   public int groundTicks;

   public Criticals() {
      super("Criticals", 0, Module.Category.COMBAT);
      this.settings.add(this.EntityHit = new BoolSettings("EntityHit", true, this));
      this.settings
         .add(
            this.HitMode = new ModeSettings(
               "HitMode",
               "Matrix",
               this,
               new String[]{
                  "Matrix", "Matrix2", "NCP", "MatrixElytra", "MatrixStand", "MatrixSmart", "Grim", "MatrixLow&Ncp", "NcpYPort", "GrimStand", "Repulsive"
               },
               () -> this.EntityHit.getBool()
            )
         );
      this.settings.add(this.Bowing = new BoolSettings("Bowing", false, this));
      this.settings.add(this.BowingAwpSound = new BoolSettings("BowingAwpSound", true, this, () -> this.Bowing.getBool()));
      this.settings.add(this.BowMode = new ModeSettings("BowMode", "Vanilla", this, new String[]{"Vanilla", "Matrix6.4.0-"}, () -> this.Bowing.getBool()));
      this.settings
         .add(
            this.SilentBurrowBlock = new BoolSettings(
               "SilentBurrowBlock", true, this, () -> this.Bowing.getBool() && this.BowMode.getMode().equalsIgnoreCase("Vanilla")
            )
         );
      this.settings.add(this.VehicleInstakill = new BoolSettings("VehicleInstakill", true, this));
      this.setDemand(1, 0);
      get = this;
   }

   @Override
   public void onUpdate() {
      if (!this.EntityHit.getBool() && !this.Bowing.getBool() && !this.VehicleInstakill.getBool()) {
         Client.msg("§f§lModules:§r §7[§l" + this.name + "§r§7]: Сначала включите что-нибудь в настройках.", false);
         this.toggle(false);
      } else {
         if (this.actived
            && this.EntityHit.getBool()
            && this.HitMode.getMode().equalsIgnoreCase("GrimStand")
            && Minecraft.player != null
            && Minecraft.player.boundingBox != null
            && !Minecraft.player.isJumping()) {
            boolean canAttemptStack = !MoveMeHelp.isMoving();
            boolean canHoldFlagGround = canAttemptStack
               && !mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.offsetMinDown(-Minecraft.player.motionY * 2.0)).isEmpty()
               && Minecraft.player.posY - Minecraft.player.lastTickPosY < 0.0;
            if (canHoldFlagGround) {
               Minecraft.player.setPosY(Minecraft.player.posY - 0.001F);
               Minecraft.player.lastReportedPosY += 0.05F;
               Minecraft.player.fallDistance = 2.0F;
               Minecraft.player.onGround = false;
               this.groundTicks++;
            } else if (Minecraft.player.onGround) {
               if (canAttemptStack && !Minecraft.player.isJumping()) {
                  Minecraft.player.jump();
               }
            } else {
               this.groundTicks = 0;
            }
         }

         if (this.ticksInningShoot > 0) {
            this.ticksInningShoot--;
         }

         if (doAddPacket && yawS != 0.0F && pitchS != 0.0F) {
            Minecraft.player.connection.sendPacket(new CPacketPlayer.Rotation(yawS, pitchS, groundS));
            doAddPacket = false;
         }

         boolean debug = false;
         if (debug && Minecraft.player.isJumping()) {
            Client.msg(
               Minecraft.player.posY
                  + " | "
                  + Minecraft.player.fallDistance
                  + " | "
                  + Minecraft.player.motionY
                  + " | "
                  + ((double)((int)(Minecraft.player.posY + 1.0)) - Minecraft.player.posY),
               false
            );
         }

         if (this.Bowing.getBool()
            && this.SilentBurrowBlock.getBool()
            && this.BowMode.getMode().equalsIgnoreCase("Vanilla")
            && Minecraft.player != null
            && mc.world != null
            && Minecraft.player.onGround) {
            boolean isTickStartBowing = Minecraft.player.isBowing() && Minecraft.player.getItemInUseMaxCount() == 1;
            BlockPos selfBlockPos = BlockUtils.getEntityBlockPos(Minecraft.player);
            boolean hasBlockInFeet = !mc.world.getCollisionBoxes(Minecraft.player, new AxisAlignedBB(selfBlockPos)).isEmpty();
            if (isTickStartBowing && !hasBlockInFeet) {
               boolean haveBlocks = ScaffWalk.get.getAnyBlocksCount(true) > 0;
               if (haveBlocks) {
                  for (double offset : new double[]{0.41999998688698, 0.7531999805212, 0.99999999}) {
                     mc.getConnection()
                        .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + offset, Minecraft.player.posZ, false));
                  }

                  Minecraft.player.setPosY(Minecraft.player.posY + 2.0);
                  ScaffWalk.get
                     .afterSwitchActionHand(
                        true,
                        true,
                        true,
                        () -> {
                           EnumHand hand = ScaffWalk.get.getHasActiveHand;
                           if (hand != null) {
                              ScaffWalk.get
                                 .updateObjectMouseOverSilent(
                                    new float[]{Minecraft.player.rotationYaw, 90.0F},
                                    () -> {
                                       if (mc.objectMouseOver.getBlockPos() != null) {
                                          mc.getConnection()
                                             .sendPacket(
                                                new CPacketPlayerTryUseItemOnBlock(
                                                   mc.objectMouseOver.getBlockPos(),
                                                   mc.objectMouseOver.sideHit,
                                                   hand,
                                                   (float)mc.objectMouseOver.hitVec.xCoord,
                                                   (float)mc.objectMouseOver.hitVec.yCoord,
                                                   (float)mc.objectMouseOver.hitVec.zCoord
                                                )
                                             );
                                       }
                                    }
                                 );
                           }
                        }
                     );
                  mc.getConnection()
                     .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + 2.0, Minecraft.player.posZ, false), 150);
                  mc.getConnection()
                     .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + 2.0, Minecraft.player.posZ, false), 200);
                  mc.getConnection()
                     .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + 2.0, Minecraft.player.posZ, false), 250);
                  Minecraft.player.stopActiveHand();
               }
            }
         }
      }
   }

   @Override
   public String getDisplayName() {
      return !this.EntityHit.getBool() && !this.Bowing.getBool()
         ? this.getName()
         : (
            this.EntityHit.getBool() && this.Bowing.getBool()
               ? this.getDisplayByMode(this.HitMode.getMode() + " | " + this.BowMode.getMode())
               : (this.EntityHit.getBool() ? this.getDisplayByMode(this.HitMode.getMode()) : this.getDisplayByMode(this.BowMode.getMode()))
         );
   }

   public static void vehicleInstakill(Entity entity) {
      if ((entity instanceof EntityBoat || entity instanceof EntityMinecart) && !Client.friendManager.isFriend(entity.getName())) {
         for (int i = 0; i < 17; i++) {
            mc.playerController.attackEntity(Minecraft.player, entity);
         }
      }
   }

   public static void crits(Entity entity) {
      if (mc.world != null
         && get.EntityHit.getBool()
         && entity != null
         && entity instanceof EntityLivingBase base
         && (double)Minecraft.player.getDistanceToEntity(base) <= 8.0
         && Minecraft.player != null
         && (Minecraft.player.onGround || grimUpCriticals())
         && !Minecraft.player.isInWater()
         && !Minecraft.player.isInWeb
         && (!Minecraft.player.isJumping() || grimUpCriticals())
         && (!HitAura.get.isActived() || HitAura.TARGET == null || !HitAura.get.tpHit)) {
         Module mod = get;
         if (mod != null && mod.actived) {
            double x = Minecraft.player.posX;
            double y = Minecraft.player.posY;
            double z = Minecraft.player.posZ;
            String var9 = get.HitMode.getMode();
            switch (var9) {
               case "Matrix":
                  mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y + 1.0E-6, z, false));
                  mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y, z, false));
                  break;
               case "Matrix2":
                  if (EntityLivingBase.isMatrixDamaged) {
                     mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y + 1.0E-6, z, false));
                     mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y, z, false));
                  }
                  break;
               case "MatrixLow&Ncp":
                  if (!mc.world.getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ)).getMaterial().isLiquid()) {
                     mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y + 0.08, z, false));
                     mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y + 0.021, z, false));
                  }
                  break;
               case "MatrixSmart":
                  if (EntityLivingBase.isMatrixDamaged) {
                     mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y + 1.0E-6, z, false));
                     mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y, z, false));
                     float forcedown = 0.5F;
                     mc.timer.tempSpeed = (double)forcedown;
                  } else if (Minecraft.player.onGround && MoveMeHelp.getSpeed() == 0.0 && !Minecraft.player.isJumping()) {
                     float forcedown = 1.0F;
                     if (mc.world
                        .getCollisionBoxes(
                           Minecraft.player,
                           Minecraft.player.boundingBox.maxYToMinY(Minecraft.player.boundingBox.maxY - Minecraft.player.boundingBox.minY + 1.26)
                        )
                        .isEmpty()) {
                        for (double offset : new double[]{
                           0.42F,
                           0.7531999805212024,
                           1.0013359791121417,
                           1.1661092609382138,
                           1.252203340253729,
                           1.1767592750642422,
                           1.0244240882136921,
                           0.7967356006687112,
                           0.49520087700592796,
                           0.02
                        }) {
                           mc.getConnection()
                              .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + offset, Minecraft.player.posZ, false));
                           forcedown -= 0.0825F;
                        }

                        timeCancel.reset();
                        doAddPacket = true;
                        mc.timer.tempSpeed = (double)forcedown;
                        Timer.forceTimer(forcedown);
                     } else if (mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.maxYToMinY(2.499)).isEmpty()) {
                        for (double offset : new double[]{0.41999998688698, 0.70000004768372, 0.62160004615784, 0.46636804164123, 0.23584067272827, 0.02}) {
                           mc.getConnection()
                              .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + offset, Minecraft.player.posZ, false));
                           forcedown -= 0.0825F;
                        }

                        timeCancel.reset();
                        doAddPacket = true;
                        mc.timer.tempSpeed = (double)forcedown;
                        Timer.forceTimer(forcedown);
                     } else if (mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.maxYToMinY(1.999)).isEmpty()) {
                        for (double offset : new double[]{0.20000004768372, 0.12160004615784, 0.02}) {
                           mc.getConnection()
                              .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + offset, Minecraft.player.posZ, false));
                           forcedown -= 0.0825F;
                        }

                        timeCancel.reset();
                        doAddPacket = true;
                        mc.timer.tempSpeed = (double)forcedown;
                        Timer.forceTimer(forcedown);
                     } else if (mc.world
                        .getCollisionBoxes(
                           Minecraft.player,
                           Minecraft.player.boundingBox.maxYToMinY(Minecraft.player.boundingBox.maxY - Minecraft.player.boundingBox.minY + 0.01)
                        )
                        .isEmpty()) {
                        for (double offset : new double[]{0.01250004768372, 0.01}) {
                           mc.getConnection()
                              .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + offset, Minecraft.player.posZ, false));
                           forcedown -= 0.0825F;
                        }

                        timeCancel.reset();
                        doAddPacket = true;
                        mc.timer.tempSpeed = (double)forcedown;
                        Timer.forceTimer(forcedown);
                     }
                  }
                  break;
               case "NCP":
                  mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y + 0.05F, z, false));
                  mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y, z, false));
                  mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y + 0.012511F, z, false));
                  mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y, z, false));
                  break;
               case "MatrixElytra":
                  if (InventoryUtil.getElytra() != -1 && InventoryUtil.getItemInInv(Items.air) != -1) {
                     ElytraBoost.eq();
                     mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y + 0.0201, z, true));
                     mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y + 0.0201, z, false));
                     ElytraBoost.badPacket();
                     mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y + 0.02, z, false));
                     ElytraBoost.badPacket();
                     Minecraft.player.setFlag(7, false);
                     ElytraBoost.deq();
                  }
                  break;
               case "MatrixStand":
                  if (Minecraft.player.onGround && MoveMeHelp.getSpeed() == 0.0 && !Minecraft.player.isJumping()) {
                     if (mc.world
                        .getCollisionBoxes(
                           Minecraft.player,
                           Minecraft.player.boundingBox.maxYToMinY(Minecraft.player.boundingBox.maxY - Minecraft.player.boundingBox.minY + 1.26)
                        )
                        .isEmpty()) {
                        for (double offset : new double[]{
                           0.42F,
                           0.7531999805212024,
                           1.0013359791121417,
                           1.1661092609382138,
                           1.252203340253729,
                           1.1767592750642422,
                           1.0244240882136921,
                           0.7967356006687112,
                           0.49520087700592796,
                           0.02
                        }) {
                           mc.getConnection()
                              .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + offset, Minecraft.player.posZ, false));
                        }

                        timeCancel.reset();
                     } else if (mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.maxYToMinY(2.499)).isEmpty()) {
                        for (double offset : new double[]{0.41999998688698, 0.70000004768372, 0.62160004615784, 0.46636804164123, 0.23584067272827, 0.02}) {
                           mc.getConnection()
                              .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + offset, Minecraft.player.posZ, false));
                        }

                        timeCancel.reset();
                     } else if (mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.maxYToMinY(1.999)).isEmpty()) {
                        for (double offset : new double[]{0.20000004768372, 0.12160004615784, 0.02}) {
                           mc.getConnection()
                              .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + offset, Minecraft.player.posZ, false));
                        }

                        timeCancel.reset();
                     } else if (mc.world
                        .getCollisionBoxes(
                           Minecraft.player,
                           Minecraft.player.boundingBox.maxYToMinY(Minecraft.player.boundingBox.maxY - Minecraft.player.boundingBox.minY + 0.01)
                        )
                        .isEmpty()) {
                        for (double offset : new double[]{0.01250004768372, 0.01}) {
                           mc.getConnection()
                              .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + offset, Minecraft.player.posZ, false));
                        }

                        timeCancel.reset();
                     }
                  }
                  break;
               case "Grim":
                  double padding = 1.0E-14;
                  if (Minecraft.player.fallDistance == 0.0F && axisNotCollided(-padding)) {
                     mc.getConnection()
                        .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY - padding, Minecraft.player.posZ, false));
                     Minecraft.player.fallDistance = (float)((double)Minecraft.player.fallDistance + padding);
                  }
                  break;
               case "Repulsive":
                  if (!Minecraft.player.isSprinting() && Minecraft.player.onGround && !MoveMeHelp.isMoving()) {
                     mc.getConnection().sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_SPRINTING));
                     mc.getConnection().sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.STOP_SPRINTING), 50);
                     mc.getConnection().sendPacket(new CPacketPlayer());
                  }
            }
         }
      }
   }

   private static boolean axisNotCollided(double yOffset) {
      return mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.offset(0.0, yOffset, 0.0)).isEmpty();
   }

   public static boolean grimUpCriticals() {
      return get != null
         && get.isActived()
         && get.EntityHit.getBool()
         && get.HitMode.getMode().equalsIgnoreCase("Grim")
         && Minecraft.player.fallDistance == 0.0F
         && axisNotCollided(1.0E-14);
   }

   @EventTarget
   public void onPacketSend(EventSendPacket event) {
      if (event.getPacket() instanceof CPacketPlayer packet
         && (packet instanceof CPacketPlayer.PositionRotation || packet instanceof CPacketPlayer.Rotation)
         && (
            this.HitMode.getMode().equalsIgnoreCase("MatrixStand")
               || this.HitMode.getMode().equalsIgnoreCase("MatrixSmart") && !EntityLivingBase.isMatrixDamaged
         )
         && Minecraft.player != null
         && Minecraft.player.onGround
         && MoveMeHelp.getSpeed() == 0.0
         && !Minecraft.player.isJumping()
         && MoveMeHelp.getSpeed() == 0.0) {
         boolean replace = mc.world
               .getCollisionBoxes(
                  Minecraft.player, Minecraft.player.boundingBox.maxYToMinY(Minecraft.player.boundingBox.maxY - Minecraft.player.boundingBox.minY + 1.26)
               )
               .isEmpty()
            || mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.maxYToMinY(2.499)).isEmpty()
            || mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.maxYToMinY(1.999)).isEmpty()
            || mc.world
               .getCollisionBoxes(
                  Minecraft.player, Minecraft.player.boundingBox.maxYToMinY(Minecraft.player.boundingBox.maxY - Minecraft.player.boundingBox.minY + 0.01)
               )
               .isEmpty();
         if (replace) {
            if (packet instanceof CPacketPlayer.PositionRotation positionRotationPacket) {
               if (timeCancel.hasReached(900.0)) {
                  return;
               }

               if (Minecraft.player.ticksExisted < 100) {
                  return;
               }

               if (positionRotationPacket.pitch > 87.0F) {
                  return;
               }

               if (MathUtils.getDifferenceOf(positionRotationPacket.yaw, Minecraft.player.lastReportedYaw) > 10.0F && HitAura.TARGET == null) {
                  return;
               }

               mc.getConnection()
                  .sendPacket(
                     new CPacketPlayer.Position(positionRotationPacket.x, positionRotationPacket.y, positionRotationPacket.z, positionRotationPacket.onGround)
                  );
               event.cancel();
            }

            if (packet instanceof CPacketPlayer.Rotation rotationPacket) {
               if (timeCancel.hasReached(900.0)) {
                  return;
               }

               if (Minecraft.player.ticksExisted < 100) {
                  return;
               }

               if (rotationPacket.pitch > 87.0F) {
                  return;
               }

               if (MathUtils.getDifferenceOf(rotationPacket.yaw, Minecraft.player.lastReportedYaw) > 10.0F && HitAura.TARGET == null) {
                  return;
               }

               yawS = rotationPacket.yaw;
               pitchS = rotationPacket.pitch;
               groundS = rotationPacket.onGround;
               event.cancel();
            }
         }
      }
   }

   @EventTarget
   public void onPacketReceive(EventReceivePacket event) {
      if (this.actived
         && event.getPacket() instanceof SPacketSoundEffect soundEffect
         && this.Bowing.getBool()
         && this.BowingAwpSound.getBool()
         && soundEffect != null
         && soundEffect.getSound() != null
         && soundEffect.getSound().getSoundName() != null
         && (
            soundEffect.getSound().getSoundName() == SoundEvents.ENTITY_ARROW_SHOOT.getSoundName()
               || soundEffect.getSound().getSoundName() == SoundEvents.ENTITY_ARROW_HIT.getSoundName()
               || soundEffect.getSound().getSoundName() == SoundEvents.ENTITY_ARROW_HIT_PLAYER.getSoundName()
         )
         && this.ticksInningShoot > 0
         && Minecraft.player != null) {
         float dst = (float)Minecraft.player.getDistance(soundEffect.getX(), soundEffect.getY(), soundEffect.getZ());
         if ((double)dst < 92.0 && (double)dst > 1.0) {
            MusicHelper.playSound("awpshoot.wav", 0.2F * soundEffect.getVolume());
            this.addTimedVec2s(soundEffect);
            this.ticksInningShoot = 0;
            event.cancel();
         }
      }
   }

   private float getCurrentLongUseDamage(float packetsCount) {
      return 2.24F + packetsCount * 0.092159994F;
   }

   @EventTarget
   public void onSend(EventSendPacket event) {
      if (event.getPacket() instanceof CPacketPlayerDigging packet && this.Bowing.getBool() && this.timeFix.hasReached(300.0)) {
         if (packet != null && packet.getAction() == CPacketPlayerDigging.Action.RELEASE_USE_ITEM) {
            if (this.correctUseMod()) {
               this.damageMultiply(this.hasTautString(this.getTautPercent()), 100, true);
            }

            this.timeFix.reset();
         }

         return;
      }
   }

   private boolean correctUseMod() {
      return Minecraft.player.isBowing() && this.actived && this.Bowing.getBool();
   }

   private void drawIndicator(float scaling, ScaledResolution sr) {
      this.usingProgress.to = this.getTautPercent();
      float progress = MathUtils.clamp(this.usingProgress.getAnim(), 0.05F, 1.0F);
      float plusRad = this.radiusPlus;
      float width = 100.0F - 50.0F * plusRad;
      float height = 4.0F;
      float extendY = 30.0F;
      float x = (float)(sr.getScaledWidth() / 2) - width / 2.0F;
      float x2 = (float)(sr.getScaledWidth() / 2) + width / 2.0F;
      float x3 = (float)(sr.getScaledWidth() / 2) - width / 2.0F + width * progress;
      float y = (float)(sr.getScaledHeight() / 2) + 30.0F;
      float y2 = y + 4.0F;
      float alphed = scaling * scaling;
      int colorShadow = ColorUtils.getColor(5, (int)(plusRad * 255.0F), 14, (int)((90.0F + plusRad * 45.0F) * alphed));
      int colorLeft = ColorUtils.getOverallColorFrom(
         ColorUtils.getColor(255, 110, 70, (int)(140.0F * alphed)), ColorUtils.swapAlpha(colorShadow, alphed * 80.0F), plusRad
      );
      int colorRight = ColorUtils.getOverallColorFrom(
         ColorUtils.getColor(140, 255, 255, (int)(120.0F * alphed)), ColorUtils.swapAlpha(colorShadow, alphed * 95.0F), plusRad
      );
      GlStateManager.pushMatrix();
      RenderUtils.customScaledObject2D(x, y, width, 4.0F, scaling);
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         x, y, x3, y2, 2.0F, 0.0F, colorLeft, colorRight, colorRight, colorLeft, false, true, false
      );
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         x, y, x2, y2, 2.0F, 2.0F + plusRad * 3.25F, colorShadow, colorShadow, colorShadow, colorShadow, false, false, true
      );
      GlStateManager.popMatrix();
   }

   @Override
   public void alwaysRender2D(ScaledResolution sr) {
      if (this.correctUseMod() && this.indicatorScale != 1.0F || !this.correctUseMod() && this.indicatorScale != 0.0F) {
         this.indicatorScale = MathUtils.harp(this.indicatorScale, this.correctUseMod() ? 1.0F : 0.0F, (float)Minecraft.frameTime * 0.005F);
      }

      if (this.indicatorScale != 0.0F) {
         this.radiusPlus = MathUtils.harp(
            this.radiusPlus,
            this.hasTautString(this.getTautPercent()) && (double)this.usingProgress.getAnim() > 0.995 ? 1.0F : 0.0F,
            (float)Minecraft.frameTime * 0.01F
         );
         this.drawIndicator(this.indicatorScale, sr);
      }
   }

   private float getTautPercent() {
      return (float)Minecraft.player.getItemInUseMaxCount()
               / this.getCurrentLongUseDamage(this.BowMode.getMode().equalsIgnoreCase("Matrix6.4.0-") ? 26.0F : 100.0F)
            >= 1.0F
         ? 1.0F
         : (float)Minecraft.player.getItemInUseMaxCount()
            / this.getCurrentLongUseDamage(this.BowMode.getMode().equalsIgnoreCase("Matrix6.4.0-") ? 26.0F : 100.0F);
   }

   private boolean hasTautString(float used) {
      return used == 1.0F;
   }

   private void damageMultiply(boolean successfully, int packetsCount, boolean sendFakeMassage) {
      if (!successfully) {
         if (sendFakeMassage) {
            Client.msg("§f§lModules:§r §7[§l" + this.name + "§r§7]: Не могу увеличить урон лука.", false);
         }
      } else {
         float yaw = BowAimbot.get.getTarget() != null ? BowAimbot.getVirt()[0] : Minecraft.player.rotationYaw;
         float pitch = BowAimbot.get.getTarget() != null ? BowAimbot.getVirt()[1] : Minecraft.player.rotationPitch;
         if (this.BowMode.getMode().equalsIgnoreCase("Matrix6.4.0-")) {
            if (ElytraBoost.canElytra()) {
               ElytraBoost.eq();
               ElytraBoost.badPacket();
               Minecraft.player.connection.preSendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_SPRINTING));
               Minecraft.player.connection.preSendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.STOP_SPRINTING));
               Minecraft.player.connection.preSendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_SPRINTING));
               ElytraBoost.badPacket();
               ElytraBoost.deq();
               Minecraft.player.connection.preSendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_SPRINTING));

               for (int packet = 0; packet < 26; packet++) {
                  Minecraft.player
                     .connection
                     .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + 0.2, Minecraft.player.posZ, false));
                  Minecraft.player
                     .connection
                     .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ, false));
               }

               Minecraft.player.connection.sendPacket(new CPacketPlayer.Rotation(yaw, 4.2F, false));
            } else {
               Client.msg("§f§lModules:§r §7[§l" + this.name + "§r§7]: У вас нет элитры в инвентаре.", false);
            }
         } else {
            Minecraft.player.connection.preSendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_SPRINTING));
            int packets = packetsCount / 2;

            for (int packet = 0; packet < packets; packet++) {
               Minecraft.player
                  .connection
                  .preSendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + 1.0E-11, Minecraft.player.posZ, false));
               boolean isLast = packet == packetsCount - 1;
               if (isLast) {
                  Minecraft.player
                     .connection
                     .preSendPacket(
                        new CPacketPlayer.PositionRotation(
                           Minecraft.player.posX,
                           Minecraft.player.posY - 1.0E-11,
                           Minecraft.player.posZ,
                           yaw,
                           MathUtils.clamp(pitch * 3.0F, -90.0F, 90.0F),
                           false
                        )
                     );
               } else {
                  Minecraft.player
                     .connection
                     .preSendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY - 1.0E-11, Minecraft.player.posZ, true));
               }
            }
         }

         if (sendFakeMassage) {
            Client.msg("§f§lModules:§r §7[§l" + this.name + "§r§7]: Увеличваю урон лука.", false);
         }

         this.usingProgress.setAnim(0.0F);
         if (this.BowingAwpSound.getBool()) {
            this.ticksInningShoot = 12;
         }
      }
   }

   private void addTimedVec2s(SPacketSoundEffect effectPacket) {
      this.TIMED_VECS2S_LIST
         .add(
            new Criticals.TimedVec2s(
               new Vec3d(effectPacket.getX(), effectPacket.getY(), effectPacket.getZ()), Minecraft.player.getPositionEyes(mc.getRenderPartialTicks()), 4000.0F
            )
         );
   }

   @EventTarget
   public void onRender3D(Event3D event) {
      if (this.isActived()) {
         if (!this.TIMED_VECS2S_LIST.isEmpty()) {
            this.TIMED_VECS2S_LIST.removeIf(Criticals.TimedVec2s::isToRemove);
            RenderUtils.setup3dForBlockPos(() -> this.TIMED_VECS2S_LIST.forEach(timedVec -> {
                  float aPC = timedVec.getAlphaPC();
                  if (aPC * 255.0F >= 1.0F) {
                     Vec3d vec = timedVec.getVec();
                     Vec3d vec2 = timedVec.getVec2();
                     if (vec != null && vec2 != null) {
                        int color = -1;
                        color = ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) * aPC);
                        GL11.glEnable(2848);
                        GL11.glHint(3154, 4354);
                        float lw = 1.0F;

                        for (int i = 0; i < 12; i++) {
                           GL11.glLineWidth(0.25F + lw * aPC);
                           lw += 2.0F;
                           color = ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) / 1.65F);
                           RenderUtils.glColor(color);
                           GL11.glBegin(1);
                           GL11.glVertex3d(vec.xCoord, vec.yCoord, vec.zCoord);
                           GL11.glVertex3d(vec2.xCoord, vec2.yCoord, vec2.zCoord);
                           GL11.glEnd();
                        }

                        GL11.glLineWidth(1.0F);
                        GL11.glHint(3154, 4352);
                        GL11.glDisable(2848);
                     }
                  }
               }), true);
         }
      }
   }

   @EventTarget
   public void onPlayerUpdate(EventPlayerMotionUpdate event) {
      if (this.actived
         && this.EntityHit.getBool()
         && this.HitMode.getMode().equalsIgnoreCase("NcpYPort")
         && mc.world != null
         && Minecraft.player != null
         && Minecraft.player.boundingBox != null
         && !Minecraft.player.isJumping()) {
         float groundOffsetStage0 = 1.0E-4F;
         float groundOffsetStage1 = 1.0E-5F;
         float checkOffset = 2.0E-4F;
         if (Minecraft.player.onGround && !mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.offsetMinDown(1.0E-6F)).isEmpty()) {
            if (HitAura.cooldown.hasReached((double)(HitAura.get.msCooldown() - 130.0F))) {
               this.groundTicks++;
            } else {
               this.groundTicks = 0;
            }

            if (MoveMeHelp.getSpeed() < 0.1F) {
               Minecraft.player.lastReportedPosY += 0.05F;
            }
         } else {
            this.groundTicks = 0;
         }

         boolean canYPort = this.groundTicks != 0;
         if (canYPort) {
            boolean stage0 = this.groundTicks % 3 == 1;
            boolean stage1 = this.groundTicks % 3 == 2;
            boolean canUpHop = mc.world
               .getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.offsetMinDown((double)checkOffset).offset(0.0, (double)checkOffset, 0.0))
               .isEmpty();
            if (canUpHop) {
               event.setGround(false);
               if (stage0) {
                  event.setY(event.getPosY() + (double)groundOffsetStage0);
               } else if (stage1) {
                  event.setY(event.getPosY() + (double)groundOffsetStage1);
               } else {
                  Minecraft.player.fallDistance += 0.1F;
               }
            }
         }
      }
   }

   private class TimedVec2s {
      private final long startTime = System.currentTimeMillis();
      private final float maxTime;
      private final Vec3d vec;
      private final Vec3d vec2;

      public TimedVec2s(Vec3d vec, Vec3d vec2, float maxTime) {
         this.vec = vec;
         this.vec2 = vec2;
         this.maxTime = maxTime;
      }

      public float getTimePC() {
         return MathUtils.clamp((float)(System.currentTimeMillis() - this.startTime) / this.maxTime, 0.0F, 1.0F);
      }

      public float getAlphaPC() {
         return 1.0F - this.getTimePC() * this.getTimePC();
      }

      public Vec3d getVec() {
         return this.vec;
      }

      public Vec3d getVec2() {
         return this.vec2;
      }

      public boolean isToRemove() {
         return this.getVec() == null || this.getTimePC() == 1.0F;
      }
   }
}
