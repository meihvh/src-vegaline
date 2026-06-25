package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiBossOverlay;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemShield;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.BossInfo;
import org.lwjgl.opengl.GL11;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.Event3D;
import ru.govno.client.event.events.EventReceivePacket;
import ru.govno.client.event.events.EventSendPacket;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.BlockUtils;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class TPInfluence extends Module {
   public static TPInfluence get;
   public BoolSettings UseOnHitAura;
   public BoolSettings UseOnCrystalField;
   public BoolSettings ShowTpPos;
   public BoolSettings CollisionsAvoid;
   public BoolSettings ThroughShieldHits;
   public BoolSettings AntiAttraction;
   public BoolSettings NoReturnTp;
   public BoolSettings LimitTpDelay;
   public BoolSettings UseOnlyWhileKT;
   public ModeSettings HitAuraRule;
   public ModeSettings TeleportAction;
   public FloatSettings SelfTicksAlive;
   public FloatSettings MaxRange;
   public FloatSettings VirtStrafeSpeed;
   public FloatSettings StepsLimit;
   public FloatSettings TpDelay;
   private final TimerHelper timerOfLastHit = TimerHelper.TimerHelperReseted();
   private Vec3d lastHandledVec = new Vec3d(0.0, 0.0, 0.0);
   private final List<TPInfluence.TimedRunnable> postDoingRuns = new ArrayList<>();
   private final List<TPInfluence.TimedVec> TIMED_VECS_LIST = new ArrayList<>();
   private final List<TPInfluence.TimedVec> waitedFlagTPPoses = new ArrayList<>();
   private Vec3d lastSelfOldPos;
   private final TimerHelper backTpMinDelayTimer = TimerHelper.TimerHelperReseted();

   public TPInfluence() {
      super("TPInfluence", 0, Module.Category.COMBAT);
      get = this;
      this.settings.add(this.UseOnHitAura = new BoolSettings("UseOnHitAura", false, this));
      this.settings
         .add(
            this.ThroughShieldHits = new BoolSettings(
               "ThroughShieldHits", true, this, () -> this.UseOnHitAura.getBool() && !this.TeleportAction.getMode().equalsIgnoreCase("SpeedStagesH")
            )
         );
      this.settings
         .add(
            this.HitAuraRule = new ModeSettings(
               "HitAuraRule",
               "Always",
               this,
               new String[]{"Always", "Stand", "HurtSync", "Fly", "Fly&SelfTicks", "ElytraFlying", "ElyBoost", "ElyBoost&Stand", "FlatGround"},
               () -> this.UseOnHitAura.getBool()
            )
         );
      this.settings
         .add(
            this.SelfTicksAlive = new FloatSettings(
               "SelfTicksAlive",
               400.0F,
               1000.0F,
               100.0F,
               this,
               () -> this.UseOnHitAura.getBool() && this.HitAuraRule.getMode().equalsIgnoreCase("Fly&SelfTicks")
            )
         );
      this.settings.add(this.UseOnCrystalField = new BoolSettings("UseOnCrystalField", false, this));
      this.settings
         .add(
            this.TeleportAction = new ModeSettings(
               "TeleportAction",
               "StepVH",
               this,
               new String[]{"StepVH", "StepV", "StepH", "StepHG", "VanillaVH", "VanillaH", "BlockTpRuleVH", "BlockTpRuleVH2", "SpeedStagesH"},
               () -> this.UseOnHitAura.getBool()
            )
         );
      this.settings
         .add(
            this.VirtStrafeSpeed = new FloatSettings(
               "VirtStrafeSpeed", 0.23F, 2.0F, 0.1F, this, () -> this.UseOnHitAura.getBool() && this.TeleportAction.getMode().equalsIgnoreCase("SpeedStagesH")
            )
         );
      this.settings
         .add(
            this.StepsLimit = new FloatSettings(
               "StepsLimit", 5.0F, 20.0F, 1.0F, this, () -> this.UseOnHitAura.getBool() && this.TeleportAction.getMode().equalsIgnoreCase("SpeedStagesH")
            )
         );
      this.settings
         .add(
            this.LimitTpDelay = new BoolSettings(
               "LimitTpDelay", false, this, () -> this.UseOnHitAura.getBool() && this.TeleportAction.getMode().equalsIgnoreCase("SpeedStagesH")
            )
         );
      this.settings
         .add(
            this.TpDelay = new FloatSettings(
               "TpDelay",
               700.0F,
               3000.0F,
               450.0F,
               this,
               () -> this.UseOnHitAura.getBool() && this.LimitTpDelay.getBool() && this.TeleportAction.getMode().equalsIgnoreCase("SpeedStagesH")
            )
         );
      this.settings
         .add(this.UseOnlyWhileKT = new BoolSettings("UseOnlyWhileKT", false, this, () -> this.UseOnHitAura.getBool() || this.UseOnCrystalField.getBool()));
      this.settings
         .add(
            this.MaxRange = new FloatSettings(
               "MaxRange",
               60.0F,
               200.0F,
               10.0F,
               this,
               () -> this.UseOnHitAura.getBool() && (this.TeleportAction.getMode().contains("Step") || this.TeleportAction.getMode().contains("BlockTpRuleVH"))
            )
         );
      this.settings.add(this.ShowTpPos = new BoolSettings("ShowTpPos", true, this, () -> this.UseOnHitAura.getBool()));
      this.settings.add(this.CollisionsAvoid = new BoolSettings("CollisionsAvoid", true, this, () -> this.UseOnHitAura.getBool()));
      this.settings
         .add(
            this.AntiAttraction = new BoolSettings(
               "AntiAttraction",
               true,
               this,
               () -> this.UseOnHitAura.getBool()
                        && !this.TeleportAction.getMode().contains("BlockTpRuleVH")
                        && !this.TeleportAction.getMode().equalsIgnoreCase("SpeedStagesH")
                     || this.UseOnCrystalField.getBool()
            )
         );
      this.settings
         .add(
            this.NoReturnTp = new BoolSettings(
               "NoBackTP", false, this, () -> this.UseOnHitAura.getBool() && this.TeleportAction.getMode().contains("BlockTpRuleVH")
            )
         );
      this.setDemand(2, 3);
   }

   private boolean isEnabledOffsetToSelfFromTargetVirtVec() {
      return this.TeleportAction.getMode().equalsIgnoreCase("SpeedStagesH");
   }

   private int getStepsLimitPerhapsSmart() {
      return this.StepsLimit.getInt();
   }

   private double getVirtStrafeSpeedPerhapsSmart() {
      return (double)this.VirtStrafeSpeed.getFloat();
   }

   private double multipliedRangeSum(double defaultRange, double speedStep, int stepsCountMax) {
      return defaultRange + speedStep * (double)stepsCountMax;
   }

   private double sqrtAt(double val1) {
      return Math.sqrt(val1 * val1);
   }

   private double sqrtAt(double val1, double val2) {
      return Math.sqrt(val1 * val1 + val2 * val2);
   }

   private double sqrtAt(double val1, double val2, double val3) {
      return Math.sqrt(val1 * val1 + val2 * val2 + val3 * val3);
   }

   private double positive(double val) {
      return val < 0.0 ? -val : val;
   }

   public boolean defaultRule() {
      return (!FreeCam.get.isActived() || FreeCam.fakePlayer == null)
         && Minecraft.player != null
         && this.isActived()
         && (
            !this.UseOnlyWhileKT.getBool()
               || (this.UseOnHitAura.getBool() || this.UseOnCrystalField.getBool())
                  && !GuiBossOverlay.mapBossInfos2.isEmpty()
                  && GuiBossOverlay.mapBossInfos2
                        .values()
                        .stream()
                        .map(BossInfo::getName)
                        .map(ITextComponent::getFormattedText)
                        .map(String::toLowerCase)
                        .filter(name -> name.contains("pvp") || name.contains("пвп") || name.contains("сек."))
                        .filter(Objects::nonNull)
                        .toList()
                        .size()
                     != 0
               || mc.world.getScoreboard() != null
                  && mc.world
                     .getScoreboard()
                     .getTeamNames()
                     .stream()
                     .map(String::toLowerCase)
                     .anyMatch(
                        str -> List.of("терка", "боя", "противник", "пвп", "pvp", "режим").stream().map(String::toLowerCase).anyMatch(bad -> str.contains(bad))
                     )
         );
   }

   public boolean entityRule(EntityLivingBase targetIn) {
      if (targetIn != null && targetIn.isEntityAlive()) {
         boolean selfCollided = Minecraft.player.boundingBox == null || mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox).isEmpty();
         boolean targetCollided = targetIn.boundingBox == null || mc.world.getCollisionBoxes(targetIn, targetIn.boundingBox).isEmpty();
         return selfCollided || !targetCollided;
      } else {
         return false;
      }
   }

   private void send(double x, double y, double z, boolean ground) {
      mc.getConnection().sendPacket(new CPacketPlayer.Position(x, y, z, ground));
   }

   private void send(double x, double y, double z, float yaw, float pitch, boolean ground) {
      mc.getConnection().sendPacket(new CPacketPlayer.PositionRotation(x, y, z, yaw, pitch, ground));
   }

   private void send(double x, double y, double z) {
      this.send(x, y, z, false);
   }

   private void send(double x, double y, double z, float yaw, float pitch) {
      this.send(x, y, z, yaw, pitch, false);
   }

   private void send(boolean ground) {
      mc.getConnection().sendPacket(new CPacketPlayer(ground));
   }

   private void send() {
      mc.getConnection().sendPacket(new CPacketPlayer());
   }

   private Vec3d axisEntityPoint(Entity entityOf) {
      AxisAlignedBB bb;
      return (bb = entityOf.getRenderBoundingBox()) != null
         ? new Vec3d(bb.minX + (bb.maxX - bb.minX) / 2.0, bb.minY, bb.minZ + (bb.maxZ - bb.minZ) / 2.0)
         : entityOf.getPositionVector();
   }

   public void teleportActionOfActionType(boolean pre, Vec3d to, String actionType, Entity copyRotateEnt) {
      if (copyRotateEnt == null) {
         this.teleportActionOfActionType(pre, to, actionType, false);
      } else {
         CPacketPlayer.lastSendedYaw = copyRotateEnt.rotationYaw;
         CPacketPlayer.lastSendedPitch = copyRotateEnt.rotationPitch;
         this.teleportActionOfActionType(pre, to, actionType, true);
      }
   }

   public void teleportActionOfActionType(boolean pre, Vec3d to, String actionType, boolean addRotEnd) {
      Vec3d self = Minecraft.player.getPositionVector();
      double dx = this.positive(self.xCoord - to.xCoord);
      double dy = this.positive(self.yCoord - to.yCoord);
      double dz = this.positive(self.zCoord - to.zCoord);
      int grInt = Minecraft.player.onGround ? 1 : 0;
      float distanceDensity = 1.0F;
      if (pre) {
         switch (actionType) {
            case "StepVH":
               double diffs = this.sqrtAt(dx, dy, dz);

               for (int packetCount = (int)(diffs / (9.64 * (double)distanceDensity)) + 1; packetCount > 0; packetCount--) {
                  this.send(false);
               }

               if (grInt == 1) {
                  this.send(to.xCoord, to.yCoord + 0.08, to.zCoord);
               }

               this.send(to.xCoord, to.yCoord + 0.01, to.zCoord);
               if (addRotEnd) {
                  this.send(to.xCoord, to.yCoord + 0.01, to.zCoord, CPacketPlayer.lastSendedYaw, CPacketPlayer.lastSendedPitch);
               }

               this.lastHandledVec = new Vec3d(to.xCoord, to.yCoord + 0.01, to.zCoord);
               break;
            case "StepV":
               double diffsxx = this.positive(dy);
               int packetCount = 1 + (int)(diffsxx / (9.73 * (double)distanceDensity));
               if (grInt == 1) {
                  this.send(to.xCoord, self.yCoord + 0.08, to.zCoord);
               }

               while (packetCount > 0) {
                  this.send(false);
                  packetCount--;
               }

               if (addRotEnd) {
                  this.send(to.xCoord, to.yCoord + 0.01, to.zCoord, CPacketPlayer.lastSendedYaw, CPacketPlayer.lastSendedPitch);
               } else {
                  this.send(to.xCoord, to.yCoord + 0.01, to.zCoord);
               }

               this.lastHandledVec = new Vec3d(self.xCoord, to.yCoord + 0.01, self.zCoord);
               break;
            case "StepH":
               double diffsx = this.sqrtAt(dx, dz);

               for (int packetCountx = (int)(diffsx / (8.953 * (double)distanceDensity)) + grInt; packetCountx > 0; packetCountx--) {
                  this.send(false);
               }

               if (addRotEnd) {
                  this.send(to.xCoord, to.yCoord, to.zCoord, CPacketPlayer.lastSendedYaw, CPacketPlayer.lastSendedPitch);
               } else {
                  this.send(to.xCoord, to.yCoord, to.zCoord);
               }

               this.lastHandledVec = new Vec3d(to.xCoord, self.yCoord, to.zCoord);
               break;
            case "StepHG":
               double diffsxxx = this.sqrtAt(dx, dz);

               int packetCountx;
               for (packetCountx = (int)(diffsxxx / (8.317 * (double)distanceDensity)) + grInt; packetCountx > 0; packetCountx--) {
                  this.send(false);
               }

               if (addRotEnd) {
                  this.send(
                     to.xCoord,
                     self.yCoord - (double)grInt * 1.0E-4 * (double)packetCountx,
                     to.zCoord,
                     CPacketPlayer.lastSendedYaw,
                     CPacketPlayer.lastSendedPitch
                  );
               } else {
                  this.send(to.xCoord, self.yCoord - (double)grInt * 1.0E-4 * (double)packetCountx, to.zCoord);
               }

               this.lastHandledVec = new Vec3d(to.xCoord, self.yCoord - (double)grInt * 1.0E-4 * (double)packetCountx, to.zCoord);
               if (grInt == 0) {
                  Minecraft.player.setPosY(self.yCoord);
               }
               break;
            case "VanillaVH":
               this.send(false);
               if (addRotEnd) {
                  this.send(to.xCoord, to.yCoord, to.zCoord, CPacketPlayer.lastSendedYaw, CPacketPlayer.lastSendedPitch);
               } else {
                  this.send(to.xCoord, to.yCoord, to.zCoord);
               }

               this.lastHandledVec = new Vec3d(to.xCoord, to.yCoord, to.zCoord);
               break;
            case "VanillaH":
               this.send(false);
               if (addRotEnd) {
                  this.send(to.xCoord, to.yCoord, to.zCoord, CPacketPlayer.lastSendedYaw, CPacketPlayer.lastSendedPitch);
               } else {
                  this.send(to.xCoord, to.yCoord, to.zCoord);
               }

               this.lastHandledVec = new Vec3d(to.xCoord, to.yCoord, to.zCoord);
               break;
            case "BlockTpRuleVH":
               if (grInt == 1) {
                  this.send(self.xCoord, self.yCoord + 0.08, self.zCoord, false);
                  this.send(self.xCoord, self.yCoord + 0.021, self.zCoord, false);
               }

               dy = this.positive(self.yCoord - (to.yCoord - 1.0));
               double diffsxxxx = this.sqrtAt(dx, dy, dz);

               for (int packetCountxx = (int)(diffsxxxx / (9.64 * (double)distanceDensity)); packetCountxx > 0; packetCountxx--) {
                  this.send(false);
               }

               this.send(to.xCoord, to.yCoord - 1.0, to.zCoord, false);
               this.lastHandledVec = new Vec3d(to.xCoord, to.yCoord, to.zCoord);
               break;
            case "BlockTpRuleVH2":
               if (grInt == 1) {
                  this.send(self.xCoord, self.yCoord + 0.08, self.zCoord, false);
                  this.send(self.xCoord, self.yCoord + 0.021, self.zCoord, false);
               }

               dy = this.positive(self.yCoord - (to.yCoord - 1.0));
               double diffsxxxxx = this.sqrtAt(dx, dy, dz);

               for (int packetCountxx = (int)(diffsxxxxx / (9.64 * (double)distanceDensity)); packetCountxx > 0; packetCountxx--) {
                  this.send(false);
               }

               this.send(to.xCoord, (double)((float)((int)to.yCoord) - 0.1F), to.zCoord, false);
               this.lastHandledVec = new Vec3d(to.xCoord, (double)((float)((int)to.yCoord) - 0.1F), to.zCoord);
               break;
            case "SpeedStagesH":
               boolean criticals = Criticals.get.isActived()
                  && Criticals.get.EntityHit.getBool()
                  && Criticals.get.HitMode.getMode().equalsIgnoreCase("MatrixLow&Ncp");
               double diffsxxxxxx = self.distanceXZTo(to);
               List<Vec3d> lrpVecs = new ArrayList<>();
               double virtStrafeSpeed = this.getVirtStrafeSpeedPerhapsSmart();
               int stagesMaxCount = this.getStepsLimitPerhapsSmart();
               int stages = Math.max(Math.min((int)(diffsxxxxxx / virtStrafeSpeed) + 1, stagesMaxCount), 0);

               for (int stage = 0; stage < stages; stage++) {
                  float pc01 = (float)stage / (float)stages;
                  Vec3d stepVec = self.addVector((to.xCoord - self.xCoord) * (double)pc01, 0.0, (to.zCoord - self.zCoord) * (double)pc01);
                  lrpVecs.add(stepVec);
               }

               int packetCountxx = lrpVecs.size();
               if (packetCountxx > 0) {
                  int i = 0;

                  for (Vec3d stepVec : lrpVecs) {
                     float ciclePC01 = (float)(i / lrpVecs.size());
                     boolean isLast = stepVec == lrpVecs.get(lrpVecs.size() - 1);
                     double offsetCriticals = criticals && isLast ? 0.08 : 0.0;
                     if (addRotEnd) {
                        if (isLast) {
                           this.send(
                              stepVec.xCoord,
                              stepVec.yCoord + offsetCriticals,
                              stepVec.zCoord,
                              CPacketPlayer.lastSendedYaw,
                              CPacketPlayer.lastSendedPitch,
                              offsetCriticals == 0.0
                           );
                        } else {
                           this.send(stepVec.xCoord, stepVec.yCoord + offsetCriticals, stepVec.zCoord, offsetCriticals == 0.0);
                        }
                     } else {
                        this.send(stepVec.xCoord, stepVec.yCoord + offsetCriticals, stepVec.zCoord, offsetCriticals == 0.0);
                     }

                     this.TIMED_VECS_LIST
                        .add(
                           new TPInfluence.TimedVec(
                              stepVec,
                              i + 1 == lrpVecs.size() ? null : lrpVecs.get(i + 1),
                              HitAura.get.msCooldown() * 1.5F / MathUtils.lerp(4.0F, 1.75F, ciclePC01)
                           )
                        );
                     i++;
                  }

                  if (criticals) {
                     this.send(to.xCoord, self.yCoord + 0.021, to.zCoord, false);
                  } else {
                     this.send(to.xCoord, self.yCoord, to.zCoord, true);
                  }
               }

               this.lastHandledVec = new Vec3d(to.xCoord, self.yCoord, to.zCoord);
         }
      } else {
         switch (actionType) {
            case "StepVH":
            case "StepH":
               this.send(self.xCoord, self.yCoord, self.zCoord);
               break;
            case "StepV":
               this.send(self.xCoord, self.yCoord, self.zCoord);
               this.send(self.xCoord, self.yCoord + (grInt == 1 ? 0.1 : -1.0E-13), self.zCoord);
               break;
            case "StepHG":
               this.send(self.xCoord, self.yCoord - this.positive((double)(grInt - 1)) * 1.0E-4 * 2.0, self.zCoord);
               break;
            case "VanillaVH":
               this.send(self.xCoord, self.yCoord + 0.0016, self.zCoord);
               break;
            case "VanillaH":
               this.send(self.xCoord, self.yCoord, self.zCoord);
               break;
            case "BlockTpRuleVH":
               this.teleportActionDoingPost(() -> this.send(to.xCoord, to.yCoord, to.zCoord, true), 0);
               if (this.NoReturnTp.getBool()) {
                  if (Minecraft.player != null) {
                     Minecraft.player.setPosition(to.xCoord, to.yCoord, to.zCoord);
                  }
               } else {
                  dy = this.positive(self.yCoord - to.yCoord);
                  double diffsx = this.sqrtAt(dx, dy, dz);
                  int packetCountxx = (int)(diffsx / (9.64 * (double)distanceDensity));
                  int finalPacketCount = packetCountxx;
                  this.teleportActionDoingPost(() -> {
                     for (int packetCount2 = finalPacketCount; packetCount2 > 0; packetCount2--) {
                        this.send(false);
                     }

                     this.send(self.xCoord, self.yCoord - 0.01F, self.zCoord, true);
                     Minecraft.player.motionY = 0.2F;
                  }, 1);
                  this.teleportActionDoingPost(() -> Minecraft.player.motionY = 0.2F, 4);
               }
               break;
            case "BlockTpRuleVH2":
               if (this.NoReturnTp.getBool()) {
                  if (Minecraft.player != null) {
                     Minecraft.player.setPosition(to.xCoord, to.yCoord, to.zCoord);
                  }
               } else {
                  dy = this.positive(self.yCoord - to.yCoord);
                  double diffsx = this.sqrtAt(dx, dy, dz);
                  int packetCountxx = (int)(diffsx / (9.64 * (double)distanceDensity));
                  this.teleportActionDoingPost(() -> {
                     for (int packetCount2 = packetCountxx; packetCount2 > 0; packetCount2--) {
                        this.send(false);
                     }

                     this.send(self.xCoord, self.yCoord - 0.01F, self.zCoord, true);
                  }, 1);
                  this.teleportActionDoingPost(() -> {
                     Minecraft.player.setPosition(self.xCoord, self.yCoord, self.zCoord);
                     Minecraft.player.motionY = 0.0394F;
                  }, 3);
               }
               break;
            case "SpeedStagesH":
               boolean criticals = Criticals.get.isActived()
                  && Criticals.get.EntityHit.getBool()
                  && Criticals.get.HitMode.getMode().equalsIgnoreCase("MatrixLow&Ncp");
               List<Vec3d> lrpVecs = new ArrayList<>();
               double virtStrafeSpeed = this.getVirtStrafeSpeedPerhapsSmart();
               double distanceXZ = self.distanceXZTo(to);
               int stagesMaxCount = this.getStepsLimitPerhapsSmart();
               int stages = Math.max(Math.min((int)(distanceXZ / virtStrafeSpeed) + 1, stagesMaxCount), 0);

               for (int stage = 0; stage < stages; stage++) {
                  float pc01 = (float)stage / (float)stages;
                  Vec3d stepBackVec = to.addVector((self.xCoord - to.xCoord) * (double)pc01, self.yCoord - to.yCoord, (self.zCoord - to.zCoord) * (double)pc01);
                  lrpVecs.add(stepBackVec);
               }

               int packetCountxx = lrpVecs.size();
               if (packetCountxx > 0) {
                  for (Vec3d stepVec : lrpVecs) {
                     if (stepVec != lrpVecs.get(0)) {
                        this.send(stepVec.xCoord, stepVec.yCoord, stepVec.zCoord, true);
                     }
                  }
               }

               boolean compenseTimerValidations = true;
               if (compenseTimerValidations) {
                  float multiplierGameSpeed = Math.min(
                     Math.max(1.0F - ((float)packetCountxx + (criticals ? 0.5F : 0.0F)) * 2.0F * (Minecraft.player.getSpeed() > 0.0 ? 0.05F : 0.025F), 0.15F),
                     1.0F
                  );
                  mc.timer.tempSpeed = (double)multiplierGameSpeed;
                  Timer.forceTimer(multiplierGameSpeed);
               }

               this.timerOfLastHit.reset();
         }
      }
   }

   public void teleportActionDoingPost(Runnable run, int ticksAfter) {
      this.postDoingRuns.add(new TPInfluence.TimedRunnable(run, (float)ticksAfter * 50.0F - 5.0F));
   }

   private void updatePostActionsRuns(boolean clear) {
      if (clear) {
         this.postDoingRuns.clear();
      } else if (!this.postDoingRuns.isEmpty()) {
         this.postDoingRuns.removeIf(TPInfluence.TimedRunnable::doIfRemove);
      }
   }

   public boolean vectorRule(Vec3d to, double defaultDistanceMax, double distanceMin) {
      String action = this.TeleportAction.getMode();
      Vec3d self = Minecraft.player.getPositionVector();
      double range = !action.contains("Step") && !action.contains("BlockTpRuleVH")
         ? (action.contains("Vanilla") ? 9.23 : defaultDistanceMax)
         : (double)this.MaxRange.getFloat();
      double dx = self.xCoord - to.xCoord;
      double dy = self.yCoord - to.yCoord;
      double dz = self.zCoord - to.zCoord;
      boolean isInRange = false;
      if (this.sqrtAt(dx, dy, dz) < distanceMin) {
         return false;
      } else {
         String var17 = this.TeleportAction.getMode();
         switch (var17) {
            case "StepVH":
            case "VanillaVH":
               isInRange = this.sqrtAt(dx, dy, dz) < range;
               break;
            case "BlockTpRuleVH":
            case "BlockTpRuleVH2":
               isInRange = this.sqrtAt(dx, dy, dz) < range && this.sqrtAt(dx, dy, dz) >= 9.0 + Minecraft.player.getSpeed();
               break;
            case "StepV":
               isInRange = this.sqrtAt(dx, dz) < defaultDistanceMax / 1.33333 && this.positive(dy) < range;
               break;
            case "StepH":
            case "StepHG":
               isInRange = this.positive(dy) < defaultDistanceMax && this.sqrtAt(dx, dz) + this.positive(dy) < range;
               break;
            case "VanillaH":
               isInRange = this.positive(dy) < defaultDistanceMax - 1.0 && this.sqrtAt(dx, dz) < range;
               break;
            case "SpeedStagesH":
               isInRange = this.positive(dy) < defaultDistanceMax - 1.0
                  && this.sqrtAt(dx, dz) > range
                  && this.sqrtAt(dx, dz) <= this.multipliedRangeSum(range, this.getVirtStrafeSpeedPerhapsSmart(), this.getStepsLimitPerhapsSmart())
                  && (!this.LimitTpDelay.getBool() || this.timerOfLastHit.hasReached((double)this.TpDelay.getFloat()));
         }

         return isInRange;
      }
   }

   private List<BlockPos> getBlockPosesAsAABB(AxisAlignedBB aabb) {
      List<BlockPos> poses = new ArrayList<>();
      double x1 = aabb.minX;
      double y1 = aabb.minY;
      double z1 = aabb.maxZ;
      double x2 = aabb.maxX;
      double y2 = aabb.maxY;
      double z2 = aabb.maxZ;

      for (BlockPos corner : Arrays.asList(
         new BlockPos(x1, y1, z1),
         new BlockPos(x2, y1, z1),
         new BlockPos(x2, y1, z2),
         new BlockPos(x1, y1, z2),
         new BlockPos(x1, y2, z1),
         new BlockPos(x2, y2, z1),
         new BlockPos(x2, y2, z2),
         new BlockPos(x1, y2, z2)
      )) {
         if (!poses.stream().anyMatch(pos -> BlockUtils.wasEqualsBlockPos(pos, corner))) {
            poses.add(corner);
         }
      }

      return poses;
   }

   private boolean anyCollisionMaterial(List<BlockPos> positions) {
      return positions.isEmpty()
         && positions.stream().map(pos -> mc.world.getBlockState(pos).getMaterial()).filter(Objects::nonNull).anyMatch(Material::blocksMovement);
   }

   private boolean anyCollisionMaterial(AxisAlignedBB aabb) {
      return aabb == null ? false : this.anyCollisionMaterial(this.getBlockPosesAsAABB(aabb));
   }

   private boolean hasCollisionForSelf(double coordX, double coordY, double coordZ) {
      return this.anyCollisionMaterial(
         Minecraft.player.boundingBox.offset(coordX - Minecraft.player.posX, coordY - Minecraft.player.posY, coordZ - Minecraft.player.posZ)
      );
   }

   public Vec3d targetWhitePos(EntityLivingBase target, double distanceMin, boolean doAccuracy, boolean offsetTargetPosToSelfXZ) {
      if (distanceMin < 0.0) {
         distanceMin = 0.0;
      }

      AxisAlignedBB targetAABB = target.getRenderBoundingBox();
      Vec3d vec = targetAABB != null
         ? new Vec3d(targetAABB.minX + (targetAABB.maxX - targetAABB.minX) / 2.0, targetAABB.minY, targetAABB.minZ + (targetAABB.maxZ - targetAABB.minZ) / 2.0)
         : target.getPositionVector();
      if (offsetTargetPosToSelfXZ) {
         EntityPlayer self = Minecraft.player;
         if (self != null) {
            double radianToSelfFromTarget = Math.toRadians((double)RotationUtil.getVecNeeded(vec, self.getPositionVector())[0]);
            double extX = Math.sin(radianToSelfFromTarget) * distanceMin;
            double extZ = -Math.cos(radianToSelfFromTarget) * distanceMin;
            vec.xCoord += extX;
            vec.zCoord += extZ;
         }
      }

      double targetX = vec.xCoord;
      double selfY = Minecraft.player.posY;
      double targetY = vec.yCoord;
      double yDst = this.positive(selfY - targetY);
      double selfW = (double)Minecraft.player.width / 2.0;
      double selfH = (double)Minecraft.player.height;
      double targetZ = vec.zCoord;
      if (mc.world != null && Minecraft.player != null) {
         AxisAlignedBB aabb = new AxisAlignedBB(targetX - selfW, targetY, targetZ - selfW, targetX + selfW, targetY + selfH, targetZ + selfW);
         if (doAccuracy && this.CollisionsAvoid.getBool() && !mc.world.getCollisionBoxes(null, aabb).isEmpty()) {
            int range = (int)distanceMin;
            List<Vec3d> toCheck = new ArrayList<>();
            float coordStep = 0.5F;
            float minX = (float)(targetX - (double)range);
            float maxX = (float)(targetX + (double)range);
            float minY = (float)(targetY - (double)range);
            float maxY = (float)(targetY + (double)range);
            float minZ = (float)(targetZ - (double)range);
            float maxZ = (float)(targetZ + (double)range);

            for (float xTemp = minX; xTemp < maxX; xTemp += 0.5F) {
               for (float yTemp = minY; yTemp < maxY; yTemp += 0.5F) {
                  for (float zTemp = minZ; zTemp < maxZ; zTemp += 0.5F) {
                     Vec3d tempVec = new Vec3d((double)xTemp, (double)yTemp, (double)zTemp);
                     if (!(tempVec.distanceTo(vec) > (double)range)) {
                        toCheck.add(tempVec);
                     }
                  }
               }
            }

            toCheck.sort(Comparator.comparing(vec1 -> vec1.distanceTo(vec)));

            for (Vec3d check : toCheck) {
               check.xCoord = (double)((int)check.xCoord) + 0.5;
               check.zCoord = (double)((int)check.zCoord) + 0.5;
               aabb = new AxisAlignedBB(
                  check.xCoord - selfW, check.yCoord, check.zCoord - selfW, check.xCoord + selfW, check.yCoord + selfH, check.zCoord + selfW
               );
               if (mc.world.getCollisionBoxes(null, aabb).isEmpty()) {
                  return check;
               }
            }

            return vec;
         } else {
            return vec;
         }
      } else {
         return vec;
      }
   }

   public boolean forHitAuraRule(EntityLivingBase target) {
      if (target != null && target.isEntityAlive()) {
         boolean sata = this.defaultRule() && this.entityRule(target) && this.UseOnHitAura.getBool();
         if (sata) {
            String rule = this.HitAuraRule.getMode();
            switch (rule) {
               case "Always":
                  sata = true;
                  break;
               case "Stand":
                  sata = MoveMeHelp.getSpeed() < 0.05 && !MoveMeHelp.moveKeysPressed();
                  break;
               case "HurtSync":
                  sata = target.hurtTime <= 1;
                  break;
               case "Fly":
                  sata = Fly.get.isActived();
                  break;
               case "Fly&SelfTicks":
                  sata = Fly.get.isActived() && (float)Minecraft.player.ticksExisted < this.SelfTicksAlive.getFloat();
                  break;
               case "ElytraFlying":
                  sata = Minecraft.player.getFlag(7);
                  break;
               case "ElyBoost":
                  sata = ElytraBoost.get.isActived() && ElytraBoost.canElytra();
                  break;
               case "ElyBoost&Stand":
                  sata = ElytraBoost.get.isActived()
                     && ElytraBoost.canElytra()
                     && MoveMeHelp.getSpeed() < 0.05
                     && !MoveMeHelp.moveKeysPressed()
                     && this.positive(Minecraft.player.motionY) < 0.24;
                  break;
               case "FlatGround":
                  sata = Minecraft.player.onGround && Minecraft.player.isCollidedVertically;
            }

            if (sata) {
               String var4 = this.TeleportAction.getMode();
               switch (var4) {
                  case "BlockTpRuleVH":
                  case "BlockTpRuleVH2":
                     sata = target.boundingBox != null
                        && mc.world != null
                        && !mc.world.getCollisionBoxes(target, target.boundingBox.offsetMinDown(0.999F)).isEmpty()
                        && !mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.offsetMinDown(0.999F)).isEmpty();
                     break;
                  case "SpeedStagesH":
                     EntityPlayer self = Minecraft.player;
                     if (self != null && target.boundingBox != null && mc.world != null) {
                        double auraRangeMin = MathUtils.clamp((double)HitAura.get.getAuraRange(HitAura.TARGET_ROTS), 3.0, 5.2 - (double)target.height);
                        double auraRangeMax = MathUtils.clamp((double)HitAura.get.getAuraRange(HitAura.TARGET_ROTS) - 0.1, 0.0, 5.2);
                        if (this.vectorRule(this.targetWhitePos(target, auraRangeMax, true, false), auraRangeMax, auraRangeMin)) {
                           List<Vec3d> lrpVecs = new ArrayList<>();
                           Vec3d selfVec = self.getPositionVector();
                           Vec3d trueTargetPos = this.targetWhitePos(target, auraRangeMax, true, this.isEnabledOffsetToSelfFromTargetVirtVec());
                           double virtStrafeSpeed = this.getVirtStrafeSpeedPerhapsSmart();
                           double distanceXZ = selfVec.distanceXZTo(trueTargetPos);
                           int stagesMaxCount = this.getStepsLimitPerhapsSmart();
                           int stages = Math.max(Math.min((int)(distanceXZ / virtStrafeSpeed) + 1, stagesMaxCount), 0);

                           for (int stage = 0; stage < stages; stage++) {
                              float pc01 = (float)stage / (float)stages;
                              Vec3d stepVec = selfVec.addVector(
                                 (trueTargetPos.xCoord - selfVec.xCoord) * (double)pc01, 0.0, (trueTargetPos.zCoord - selfVec.zCoord) * (double)pc01
                              );
                              lrpVecs.add(stepVec);
                           }

                           if (!lrpVecs.isEmpty()) {
                              double checkGroundOffsetDown = 0.01;
                              boolean notMatchMissFloor = true;

                              for (Vec3d stepVec : lrpVecs) {
                                 Vec3d offset = stepVec.add(selfVec.scale(-1.0));
                                 if (!this.hasCollisionForSelf(offset.xCoord, offset.yCoord, offset.zCoord)
                                    && this.hasCollisionForSelf(offset.xCoord, offset.yCoord - checkGroundOffsetDown, offset.zCoord)) {
                                    notMatchMissFloor = false;
                                    break;
                                 }
                              }

                              sata = notMatchMissFloor;
                           } else {
                              sata = false;
                           }
                        } else {
                           sata = false;
                        }
                     } else {
                        sata = false;
                     }
               }
            }
         }

         double auraRangeMin = MathUtils.clamp((double)HitAura.get.getAuraRange(HitAura.TARGET_ROTS), 3.0, 5.2 - (double)target.height);
         double auraRangeMax = MathUtils.clamp((double)HitAura.get.getAuraRange(HitAura.TARGET_ROTS) - 0.1, 0.0, 5.2);
         return this.isActived() && sata && this.vectorRule(this.targetWhitePos(target, auraRangeMax, false, false), auraRangeMax, auraRangeMin);
      } else {
         return false;
      }
   }

   public void hitAuraTPPre(EntityLivingBase target) {
      Vec3d truePos;
      Vec3d samiFalsePos;
      Vec3d var10002;
      String var10003;
      EntityLivingBase var10004;
      label24: {
         double auraRangeMax = MathUtils.clamp((double)HitAura.get.getAuraRange(HitAura.TARGET_ROTS) - 0.1, 0.0, 5.2);
         truePos = this.targetWhitePos(target, auraRangeMax, true, this.isEnabledOffsetToSelfFromTargetVirtVec());
         samiFalsePos = this.targetWhitePos(target, auraRangeMax, false, this.isEnabledOffsetToSelfFromTargetVirtVec());
         var10002 = this.targetWhitePos(target, auraRangeMax, true, this.isEnabledOffsetToSelfFromTargetVirtVec());
         var10003 = this.TeleportAction.getMode();
         if (this.ThroughShieldHits.getBool()
            && !this.TeleportAction.getMode().equalsIgnoreCase("SpeedStagesH")
            && target instanceof EntityPlayer player
            && (player.getHeldItemOffhand().getItem() instanceof ItemShield || player.getHeldItemMainhand().getItem() instanceof ItemShield)) {
            var10004 = target;
            break label24;
         }

         var10004 = null;
      }

      this.teleportActionOfActionType(true, var10002, var10003, var10004);
      if (this.ShowTpPos.getBool()) {
         this.addTimedVec(truePos, samiFalsePos);
      }
   }

   public void hitAuraTPPost(EntityLivingBase target) {
      double auraRangeMax = MathUtils.clamp((double)HitAura.get.getAuraRange(HitAura.TARGET_ROTS) - 0.1, 0.0, 5.2);
      this.teleportActionOfActionType(
         false, this.targetWhitePos(target, auraRangeMax, false, this.isEnabledOffsetToSelfFromTargetVirtVec()), this.TeleportAction.getMode(), false
      );
   }

   public boolean forCrystalFieldRule() {
      return this.defaultRule() && this.UseOnCrystalField.getBool();
   }

   @Override
   public void onToggled(boolean enable) {
      if (!this.TIMED_VECS_LIST.isEmpty()) {
         this.TIMED_VECS_LIST.clear();
      }

      if (!this.waitedFlagTPPoses.isEmpty()) {
         this.waitedFlagTPPoses.clear();
      }

      this.updatePostActionsRuns(true);
      super.onToggled(enable);
   }

   private void addTimedVec(Vec3d truePos, Vec3d samiFalsePos) {
      if (this.lastHandledVec != null) {
         this.TIMED_VECS_LIST
            .add(new TPInfluence.TimedVec(this.lastHandledVec, truePos == samiFalsePos ? null : samiFalsePos, HitAura.get.msCooldown() * 1.5F));
      }
   }

   private int getTpPointColor() {
      return -1;
   }

   @EventTarget
   public void onRender3D(Event3D event) {
      if (this.isActived()) {
         if (!this.TIMED_VECS_LIST.isEmpty()) {
            this.TIMED_VECS_LIST.removeIf(TPInfluence.TimedVec::isToRemove);
            RenderUtils.setup3dForBlockPos(
               () -> this.TIMED_VECS_LIST
                     .forEach(
                        timedVec -> {
                           float aPC = timedVec.getAlphaPC();
                           GL11.glLineWidth(0.1F);
                           if (aPC * 255.0F >= 1.0F) {
                              float range = 0.05F * (0.25F + 0.75F * (float)MathUtils.easeInOutQuad((double)(1.0F - timedVec.getTimePC())));
                              Vec3d vec = timedVec.getVec();
                              Vec3d falseVec = timedVec.getVecFalse();
                              AxisAlignedBB aabb = new AxisAlignedBB(
                                 vec.addVector((double)(-range), (double)(-range), (double)(-range)),
                                 vec.addVector((double)range, (double)range, (double)range)
                              );
                              int color = this.getTpPointColor();
                              if (aabb != null) {
                                 color = ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) * aPC * 0.5F);
                                 RenderUtils.drawCanisterBox(
                                    aabb, true, false, true, color, 0, ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) / 16.0F)
                                 );
                                 int iterations = 7;

                                 for (int i = 0; i < iterations; i++) {
                                    float cPC = (float)i / (float)iterations;
                                    aabb = aabb.expandXyz((double)(0.02F * (1.0F - aPC)));
                                    int c = ColorUtils.swapAlpha(
                                       color, (float)ColorUtils.getAlphaFromColor(color) / 6.0F * (float)MathUtils.easeInOutQuadWave((double)cPC)
                                    );
                                    RenderUtils.drawCanisterBox(aabb, true, true, false, c, c, c);
                                 }

                                 color = ColorUtils.swapAlpha(color, 255.0F * aPC);
                              }

                              if (falseVec != null) {
                                 double lineYTo = vec.yCoord < falseVec.yCoord ? vec.yCoord : falseVec.yCoord;
                                 GL11.glLineStipple(3, Short.reverseBytes((short)-24769));
                                 GL11.glEnable(2852);
                                 GL11.glEnable(2848);
                                 GL11.glHint(3154, 4354);
                                 GL11.glLineWidth(0.25F);
                                 GL11.glBegin(3);
                                 RenderUtils.glColor(0);
                                 GL11.glVertex3d(vec.xCoord, vec.yCoord, vec.zCoord);
                                 RenderUtils.glColor(color);
                                 GL11.glVertex3d(vec.xCoord, lineYTo, vec.zCoord);
                                 RenderUtils.glColor(0);
                                 GL11.glVertex3d(falseVec.xCoord, falseVec.yCoord, falseVec.zCoord);
                                 GL11.glEnd();
                                 GL11.glDisable(2852);
                                 GL11.glDisable(2848);
                                 GL11.glHint(3154, 4352);
                                 GL11.glLineWidth(1.0F);
                              }
                           }

                           GL11.glLineWidth(1.0F);
                        }
                     ),
               true
            );
         }
      }
   }

   @EventTarget
   public void onSend(EventSendPacket event) {
      if (this.isActived()
         && event.getPacket() instanceof CPacketPlayer packet
         && this.AntiAttraction.getBool()
         && !this.TeleportAction.getMode().contains("BlockTpRuleVH")
         && !this.TeleportAction.getMode().equalsIgnoreCase("SpeedStageH")) {
         Vec3d pos = null;
         if (packet instanceof CPacketPlayer.Position posPacket) {
            pos = new Vec3d(posPacket.getX(posPacket.x), posPacket.getY(posPacket.y), posPacket.getZ(posPacket.z));
         } else if (packet instanceof CPacketPlayer.PositionRotation posRotPacket) {
            pos = new Vec3d(posRotPacket.getX(posRotPacket.x), posRotPacket.getY(posRotPacket.y), posRotPacket.getZ(posRotPacket.z));
         }

         if (pos != null && pos.distanceTo(this.lastHandledVec) < 0.1) {
            this.waitedFlagTPPoses.add(new TPInfluence.TimedVec(pos, Minecraft.player.getPositionVector(), 650.0F));
         }
      }
   }

   @Override
   public void onUpdate() {
      this.updatePostActionsRuns(false);
      if (this.waitedFlagTPPoses.isEmpty()) {
         this.lastSelfOldPos = null;
      } else {
         this.waitedFlagTPPoses.removeIf(TPInfluence.TimedVec::isToRemove);
         if (this.waitedFlagTPPoses.isEmpty()) {
            this.lastSelfOldPos = null;
         } else {
            if (this.lastSelfOldPos != null) {
               double playerPosDiff = Minecraft.player.getPositionVector().distanceTo(this.lastSelfOldPos);
               double dx = Minecraft.player.posX - Minecraft.player.lastTickPosX;
               double dy = Minecraft.player.posY - Minecraft.player.lastTickPosY;
               double dz = Minecraft.player.posZ - Minecraft.player.lastTickPosZ;
               double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
               if (playerPosDiff > speed + 1.0) {
                  this.reduceOldPosSelf();
               }
            }

            this.lastSelfOldPos = Minecraft.player.getPositionVector();
         }
      }
   }

   private Vec3d[] getLastHandledBackPosVecs() {
      return !this.waitedFlagTPPoses.isEmpty() && this.waitedFlagTPPoses.get(this.waitedFlagTPPoses.size() - 1) != null
         ? new Vec3d[]{
            this.waitedFlagTPPoses.get(this.waitedFlagTPPoses.size() - 1).getVec(), this.waitedFlagTPPoses.get(this.waitedFlagTPPoses.size() - 1).getVecFalse()
         }
         : null;
   }

   private void reduceOldPosSelf() {
      Vec3d backPosVec = this.lastSelfOldPos;
      if (backPosVec != null) {
         int packets = (int)Math.min(backPosVec.distanceTo(Minecraft.player.getPositionVector()) / 9.953 + 2.0, 200.0);

         for (int num = 0; num < Math.max(packets - 1, 0); num++) {
            this.send(false);
         }

         this.send(false);
         Minecraft.player.setPositionAndUpdate(backPosVec.xCoord, backPosVec.yCoord, backPosVec.zCoord);
      }
   }

   @EventTarget
   public void onReceive(EventReceivePacket event) {
      if (this.isActived() && event.getPacket() instanceof SPacketPlayerPosLook posLookPacket && this.AntiAttraction.getBool()) {
         if (Minecraft.player == null) {
            return;
         }

         this.reduceOldPosSelf();
         Vec3d pos = new Vec3d(posLookPacket.getX(), posLookPacket.getY(), posLookPacket.getZ());
         if (pos == null) {
            return;
         }

         Vec3d[] backPosVecs = this.getLastHandledBackPosVecs();
         if (backPosVecs == null) {
            return;
         }

         if (!this.backTpMinDelayTimer.hasReached(500.0)) {
            return;
         }

         this.backTpMinDelayTimer.reset();
         double distance = pos.distanceTo(backPosVecs[0]);
         if (distance > 1.99999999) {
            return;
         }

         distance = pos.distanceTo(backPosVecs[1]);
         int packets = (int)Math.min(distance / 9.953 + 1.0, 20.0);

         for (int num = 0; num < Math.max(packets - 1, 0); num++) {
            this.send(false);
         }

         this.send(false);
         Minecraft.player.setPositionAndUpdate(backPosVecs[1].xCoord, backPosVecs[1].yCoord, backPosVecs[1].zCoord);
         Minecraft.player.connection.sendPacket(new CPacketConfirmTeleport(posLookPacket.getTeleportId()));
         event.cancel();
      }
   }

   private class TimedRunnable {
      private final long startTime = System.currentTimeMillis();
      private final float maxTime;
      private Runnable runnable;

      public TimedRunnable(Runnable runnable, float maxTime) {
         this.runnable = runnable;
         this.maxTime = maxTime;
      }

      private float getTimePC() {
         return MathUtils.clamp((float)(System.currentTimeMillis() - this.startTime) / this.maxTime, 0.0F, 1.0F);
      }

      public Runnable getRun() {
         return this.runnable;
      }

      public boolean doIfRemove() {
         if (this.getTimePC() == 1.0F && this.runnable != null) {
            this.runnable.run();
            this.runnable = null;
            return true;
         } else {
            return this.runnable == null;
         }
      }
   }

   private class TimedVec {
      private final long startTime = System.currentTimeMillis();
      private final float maxTime;
      private final Vec3d vec;
      private final Vec3d vecFalse;

      public TimedVec(Vec3d vec, Vec3d vecFalse, float maxTime) {
         this.vec = vec;
         this.vecFalse = vecFalse;
         this.maxTime = maxTime;
      }

      public float getTimePC() {
         return MathUtils.clamp((float)(System.currentTimeMillis() - this.startTime) / this.maxTime, 0.0F, 1.0F);
      }

      public float getAlphaPC() {
         float pc = 1.0F - this.getTimePC();
         return (float)MathUtils.easeOutCubic(MathUtils.easeInOutQuadWave((double)pc));
      }

      public Vec3d getVec() {
         return this.vec;
      }

      public Vec3d getVecFalse() {
         return this.vecFalse;
      }

      public boolean isToRemove() {
         return this.getVec() == null || this.getTimePC() == 1.0F;
      }
   }
}
