package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.network.play.client.CPacketClientSettings;
import net.minecraft.network.play.client.CPacketClientStatus;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketConfirmTransaction;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerAbilities;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketResourcePackStatus;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketCloseWindow;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.network.play.server.SPacketResourcePackSend;
import net.minecraft.network.play.server.SPacketStatistics;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldType;
import org.apache.commons.lang3.RandomStringUtils;
import org.lwjgl.input.Mouse;
import ru.govno.client.Client;
import ru.govno.client.clickgui.ClickGuiScreen;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventMove2;
import ru.govno.client.event.events.EventMovementInput;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventReceivePacket;
import ru.govno.client.event.events.EventRotationJump;
import ru.govno.client.event.events.EventRotationStrafe;
import ru.govno.client.event.events.EventSendPacket;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.IntaveDisabler;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;

public class Bypass extends Module {
   public static Bypass get;
   public BoolSettings NCPWinclick;
   public BoolSettings AACWinclick;
   public BoolSettings GrimWinclick;
   public BoolSettings VulcanStrafe;
   public BoolSettings VulcanLiquid;
   public BoolSettings MatrixElySpoofs;
   public BoolSettings NoServerGround;
   public BoolSettings NoServerRotate;
   public BoolSettings CloseScreens;
   public BoolSettings FixPearlFlag;
   public BoolSettings InvPopHitFix;
   public BoolSettings RegionUsingItem;
   public BoolSettings FixSettingsKick;
   public BoolSettings FixStackFlags;
   public BoolSettings FixShieldCooldown;
   public BoolSettings SpawnGodmode;
   public BoolSettings PortalGodmode;
   public BoolSettings ClientSpoof;
   public BoolSettings HideClient;
   public BoolSettings NoServerPack;
   public BoolSettings FullyVolume;
   public BoolSettings IntaveMovement;
   public BoolSettings LegitScreenshot;
   public BoolSettings GMFlySpoofIfCan;
   public BoolSettings NCPMovement;
   public BoolSettings ElytraClip;
   public BoolSettings BreakingClip;
   public BoolSettings ClipBlockTpRule;
   public BoolSettings AntiHunger;
   public BoolSettings AntiAfk;
   public BoolSettings TabHpPlayersSync;
   public BoolSettings PingSpoofing;
   public BoolSettings NoSwing;
   public BoolSettings NoSwingOnlyHitAura;
   public BoolSettings ChatPluginFilters;
   public BoolSettings FastTruePingSystem;
   public FloatSettings PingSpoofMS;
   public FloatSettings ChanceOppositeChars;
   public ModeSettings SwingCancelMode;
   public ModeSettings PingSpeedDetectionLevel;
   private final Random RANDOM = new Random();
   private final TimerHelper afkTime = TimerHelper.TimerHelperReseted();
   public static boolean callSneak = false;
   private static final boolean[] oldPreseds = new boolean[6];
   private static final TimerHelper timeAfterSneak = new TimerHelper();
   private final Bypass.FastTruePingDetector pingDetector = new Bypass.FastTruePingDetector(1000L);
   public int flagCPS;
   public int flagReduceTicks;
   boolean strafeHacked = false;
   Vec3d lastVecNote = Vec3d.ZERO;
   boolean vulcanStatusHacked;
   boolean noted;
   public boolean doReduceDestack;
   public static boolean hackVolume;
   boolean usingStartPlob;
   private final TimerHelper openContainerOutTime = new TimerHelper();
   public static boolean cancelPortal;
   private Runnable postPortalCancel = null;
   public static Vec3d callTeleportPos = new Vec3d(0.0, 0.0, 0.0);
   float sYaw;
   float sPitch;
   boolean callSRS;
   double gdX;
   double gdY;
   double gdZ;
   float gdYaw;
   float gdPitch;
   private float tempYaw;
   private float tempPitch;
   private int dodgeFromZero;
   public boolean ncpDisabledLocalBool;
   char[] ruAlphabetsLow = "aбвгдеёжзийклмнопрстуфхцчшщъыьэюя".toCharArray();
   char[] ruAlphabetsLowToEn = "a687дeeж3NйkлmHonpcTyфxц4wщъыь3юя".toCharArray();
   char[] enAlphabetsLow = "abcdefghijklmnoprstuvwxyz".toCharArray();
   char[] enAlphabetsLowToRu = "абсdеf9h1jкlмnорг5тuvшхуz".toCharArray();

   public Bypass() {
      super("Bypass", 0, Module.Category.MISC);
      this.settings.add(this.NCPWinclick = new BoolSettings("NCPWinclick", false, this));
      this.settings.add(this.AACWinclick = new BoolSettings("AACWinclick", false, this));
      this.settings.add(this.GrimWinclick = new BoolSettings("GrimWinclick", false, this));
      this.settings.add(this.VulcanStrafe = new BoolSettings("VulcanStrafe", false, this));
      this.settings.add(this.VulcanLiquid = new BoolSettings("VulcanLiquid", false, this));
      this.settings.add(this.MatrixElySpoofs = new BoolSettings("MatrixElySpoofs", false, this));
      this.settings.add(this.NoServerGround = new BoolSettings("NoServerGround", false, this));
      this.settings.add(this.NoServerRotate = new BoolSettings("NoServerRotate", true, this));
      this.settings.add(this.CloseScreens = new BoolSettings("CloseScreens", true, this));
      this.settings.add(this.FixPearlFlag = new BoolSettings("FixPearlFlag", false, this));
      this.settings.add(this.InvPopHitFix = new BoolSettings("InvPopHitFix", false, this));
      this.settings.add(this.RegionUsingItem = new BoolSettings("RegionUsingItem", false, this));
      this.settings.add(this.FixSettingsKick = new BoolSettings("FixSettingsKick", true, this));
      this.settings.add(this.FixStackFlags = new BoolSettings("FixStackFlags", false, this));
      this.settings.add(this.FixShieldCooldown = new BoolSettings("FixShieldCooldown", false, this));
      this.settings.add(this.SpawnGodmode = new BoolSettings("SpawnGodmode", false, this));
      this.settings.add(this.PortalGodmode = new BoolSettings("PortalGodmode", false, this));
      this.settings.add(this.ClientSpoof = new BoolSettings("ClientSpoof", false, this));
      this.settings.add(this.HideClient = new BoolSettings("HideClient", false, this, () -> this.ClientSpoof.getBool()));
      this.settings.add(this.NoServerPack = new BoolSettings("NoServerPack", true, this));
      this.settings.add(this.FullyVolume = new BoolSettings("FullyVolume", true, this));
      this.settings.add(this.IntaveMovement = new BoolSettings("IntaveMovement", false, this));
      this.settings.add(this.LegitScreenshot = new BoolSettings("LegitScreenshot", false, this));
      this.settings.add(this.GMFlySpoofIfCan = new BoolSettings("GMFlySpoofIfCan", false, this));
      this.settings.add(this.NCPMovement = new BoolSettings("NCPMovement", false, this));
      this.settings.add(this.ElytraClip = new BoolSettings("ElytraClip", true, this));
      this.settings.add(this.BreakingClip = new BoolSettings("BreakingClip", false, this));
      this.settings.add(this.ClipBlockTpRule = new BoolSettings("ClipBlockTpRule", false, this));
      this.settings.add(this.AntiHunger = new BoolSettings("AntiHunger", false, this));
      this.settings.add(this.AntiAfk = new BoolSettings("AntiAfk", true, this));
      this.settings.add(this.TabHpPlayersSync = new BoolSettings("TabHpPlayersSync", false, this));
      this.settings.add(this.PingSpoofing = new BoolSettings("PingSpoofing", false, this));
      this.settings.add(this.PingSpoofMS = new FloatSettings("PingSpoofMS", 3000.0F, 25000.0F, 1.0F, this, () -> this.PingSpoofing.getBool()));
      this.settings.add(this.NoSwing = new BoolSettings("NoSwing", false, this));
      this.settings
         .add(this.SwingCancelMode = new ModeSettings("SwingCancelMode", "Server", this, new String[]{"Server", "Client"}, () -> this.NoSwing.getBool()));
      this.settings.add(this.NoSwingOnlyHitAura = new BoolSettings("NoSwingOnlyHitAura", false, this, () -> this.NoSwing.getBool()));
      this.settings.add(this.ChatPluginFilters = new BoolSettings("ChatPluginFilters", false, this));
      this.settings.add(this.ChanceOppositeChars = new FloatSettings("ChanceOppositeChars", 0.2F, 1.0F, 0.1F, this, () -> this.ChatPluginFilters.getBool()));
      this.settings.add(this.FastTruePingSystem = new BoolSettings("FastTruePingSystem", false, this));
      this.settings
         .add(
            this.PingSpeedDetectionLevel = new ModeSettings(
               "PingSpeedDetectionLevel", "Sec", this, new String[]{"5Sec", "Sec", "500MSec", "100MSec", "Instant"}, () -> this.FastTruePingSystem.getBool()
            )
         );
      this.setDemand(1, 3);
      get = this;
   }

   public static boolean isClipBlockTpRule() {
      return get != null && get.isActived() && get.ClipBlockTpRule.getBool();
   }

   public static int injectFastTruePingDetectorOutData(NetworkPlayerInfo dataFromObj, int prevDataSelfNetwork) {
      if (!Panic.stop && dataFromObj != null && dataFromObj.getGameProfile() != null) {
         try {
            if (dataFromObj.getGameProfile().getName().equalsIgnoreCase(mc.getSession().getProfile().getName())
               && get != null
               && get.isActived()
               && get.pingDetector != null
               && get.pingDetector.isDetectorEnabled()) {
               return (int)get.pingDetector.getOutMsPing((float)prevDataSelfNetwork);
            }
         } catch (Exception var3) {
            var3.printStackTrace();
         }

         return prevDataSelfNetwork;
      } else {
         return prevDataSelfNetwork;
      }
   }

   public static boolean isCancelInvWalk() {
      return get.isActived() && get.GrimWinclick.getBool() && callSneak;
   }

   public static boolean onWinClick() {
      if (get.actived && get.GrimWinclick.getBool()) {
         timeAfterSneak.reset();
         callSneak = true;
         oldPreseds[0] = mc.gameSettings.keyBindForward.isKeyDown();
         oldPreseds[1] = mc.gameSettings.keyBindRight.isKeyDown();
         oldPreseds[2] = mc.gameSettings.keyBindLeft.isKeyDown();
         oldPreseds[3] = mc.gameSettings.keyBindBack.isKeyDown();
         oldPreseds[5] = mc.gameSettings.keyBindSneak.isKeyDown();
         return true;
      } else {
         return false;
      }
   }

   public boolean isAACWinClick() {
      return this.isActived() && this.AACWinclick.getBool();
   }

   private void sendPacket(Packet packet) {
      mc.getConnection().sendPacket(packet);
   }

   public void setStrafeHacked(boolean hack) {
      this.strafeHacked = hack;
   }

   public boolean getIsStrafeHacked() {
      return this.strafeHacked || !this.actived || !this.VulcanStrafe.getBool() || this.VulcanLiquid.getBool() && !this.strafeHacked;
   }

   public boolean canWinClickEdit() {
      return this.actived && this.NCPWinclick.getBool();
   }

   public boolean rayTrace(Entity me, double x, double y, double z) {
      return mc.world.rayTraceBlocks(new Vec3d(me.posX, me.posY, me.posZ), new Vec3d(x, y, z), false, true, false) == null
         || mc.world.rayTraceBlocks(new Vec3d(me.posX, me.posY + 1.0, me.posZ), new Vec3d(x, y + 1.0, z), false, true, false) == null;
   }

   public boolean statusVulcanDisabler() {
      return !this.actived || this.vulcanStatusHacked;
   }

   private final BlockPos waterNeared() {
      float r = 5.0F;
      float min = 4.5F;
      float max = 5.5F;

      for (float x = -5.0F; x < 5.0F; x++) {
         for (float y = -5.0F; y < 5.0F; y++) {
            for (float z = -5.0F; z < 5.0F; z++) {
               BlockPos pos = new BlockPos(
                  (double)((float)((int)Minecraft.player.posX) + x + 0.5F),
                  (double)((float)((int)Minecraft.player.posY) + y),
                  (double)((float)((int)Minecraft.player.posZ) + z + 0.5F)
               );
               if (pos != null
                  && mc.world.getBlockState(pos).getBlock() == Blocks.WATER
                  && (mc.world.getBlockState(pos.up()).getBlock() == Blocks.WATER || mc.world.getBlockState(pos.up()).getBlock() == Blocks.AIR)
                  && Minecraft.player
                        .getDistanceToVec3d(
                           new Vec3d(Minecraft.player.posX + (double)x + 0.5, Minecraft.player.posY + (double)y, Minecraft.player.posZ + (double)z + 0.5)
                        )
                     >= 4.5
                  && Minecraft.player
                        .getDistanceToVec3d(
                           new Vec3d(Minecraft.player.posX + (double)x + 0.5, Minecraft.player.posY + (double)y, Minecraft.player.posZ + (double)z + 0.5)
                        )
                     <= 5.5
                  && this.rayTrace(Minecraft.player, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5)) {
                  return pos;
               }
            }
         }
      }

      return null;
   }

   private void note() {
      float x = (float)this.waterNeared().getX() + 0.5F;
      float y = (float)this.waterNeared().getY() + 0.2F;
      float z = (float)this.waterNeared().getZ() + 0.5F;
      this.lastVecNote = new Vec3d((double)x, (double)y, (double)z);
      Minecraft.player.connection.preSendPacket(new CPacketPlayer.Position((double)x, (double)y + 0.19, (double)z, true));
      Client.msg("§f§lModules:§r §7[§lDisabler§r§7]: пытаюсь выключить Vulcan.", false);
      this.noted = true;
   }

   public double getDistanceAtVec3dToVec3d(Vec3d first, Vec3d second) {
      double xDiff = first.xCoord - second.xCoord;
      double yDiff = first.yCoord - second.yCoord;
      double zDiff = first.zCoord - second.zCoord;
      return Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
   }

   public Vec3d getEntityVecPosition(Entity entityIn) {
      return new Vec3d(entityIn.posX, entityIn.posY, entityIn.posZ);
   }

   @EventTarget
   public void onMovementState(EventMove2 event) {
      if (this.doReduceDestack) {
         event.motion().xCoord = 0.0;
         event.motion().zCoord = 0.0;
      }
   }

   void reduceStackFlags() {
      if (this.doReduceDestack) {
         if (this.flagReduceTicks > 0) {
            Minecraft.player.motionX = 0.0;
            Minecraft.player.motionY = 0.0;
            Minecraft.player.motionZ = 0.0;
            Entity.motionx = 1.0E-45;
            Entity.motiony = 1.0E-45;
            Entity.motionz = 1.0E-45;
            Minecraft.player.jumpMovementFactor = 0.0F;
            Minecraft.player.rotationYaw = Minecraft.player.rotationYaw + (float)(Math.random() * 0.01 - 0.005);
            Minecraft.player.rotationPitch = Minecraft.player.rotationPitch - (float)(Math.random() * 0.01 - 0.005);
            this.flagReduceTicks--;
         } else {
            this.doReduceDestack = false;
         }
      } else if (this.flagReduceTicks != 4) {
         this.flagReduceTicks = 4;
      }
   }

   private void doRandomPlayerAction() {
      switch (this.RANDOM.nextInt(0, 4)) {
         case 0:
            Minecraft.player.addVelocity((double)this.RANDOM.nextFloat(-0.02F, 0.02F), 0.0, (double)this.RANDOM.nextFloat(-0.02F, 0.02F));
            break;
         case 1:
            mc.getConnection().sendPacket(new CPacketAnimation(EnumHand.OFF_HAND));
            break;
         case 2:
            mc.getConnection().sendPacket(new CPacketChatMessage("/" + RandomStringUtils.randomAlphabetic(5)));
            break;
         case 3:
            Minecraft.player.rotationYaw = Minecraft.player.rotationYaw + this.RANDOM.nextFloat(-0.1F, 0.1F);
      }
   }

   @Override
   public void onUpdate() {
      if (this.pingDetector != null) {
         this.pingDetector.setEnabled(this.actived && this.FastTruePingSystem.getBool());
         String player = this.PingSpeedDetectionLevel.getMode();

         this.pingDetector.setDelaySendRequestMS(switch (player) {
            case "5Sec" -> 5000L;
            case "Sec" -> 1000L;
            case "500MSec" -> 500L;
            case "100MSec" -> 100L;
            case "Instant" -> 45L;
            default -> 10000L;
         });
         this.pingDetector.onUpdateTickLimited();
      }

      if (!this.PortalGodmode.getBool()) {
         if (cancelPortal) {
            Client.msg("§f§lModules:§r §7[§lBypass§r§7]: PortalGodmode -> Не активен", false);
            Client.msg("вы остановили работу PortalGodmode его отключением.", false);
            cancelPortal = false;
         }
      } else {
         if (this.postPortalCancel != null && Minecraft.player != null && Minecraft.player.ticksExisted > 2) {
            this.postPortalCancel.run();
            this.postPortalCancel = null;
         }

         if (cancelPortal && Minecraft.player != null && !Minecraft.player.inPortal) {
            Client.msg("§f§lModules:§r §7[§lBypass§r§7]: PortalGodmode -> Не активен", false);
            Client.msg("вы остановили работу PortalGodmode выходом из портала.", false);
            cancelPortal = false;
         }
      }

      ScoreObjective scoreobjective;
      if (this.TabHpPlayersSync.getBool()
         && mc.world != null
         && mc.world.getScoreboard() != null
         && (scoreobjective = mc.world.getScoreboard().getObjectiveInDisplaySlot(0)) != null) {
         for (EntityPlayer player : mc.world.playerEntities.stream().map(playerx -> playerx.getOtherPlayerOf()).filter(Objects::nonNull).toList()) {
            NetworkPlayerInfo netInfo = mc.getConnection().getPlayerInfo(player.getName());
            if (netInfo != null && netInfo.getGameProfile() != null) {
               try {
                  int tabHP = scoreobjective.getScoreboard().getOrCreateScore(netInfo.getGameProfile().getName(), scoreobjective).getScorePoints();
                  if (tabHP < netInfo.getLastHealth()) {
                     netInfo.setLastHealthTime(Minecraft.getSystemTime());
                     netInfo.setHealthBlinkTime((long)(mc.ingameGUI.getUpdateCounter() + 20));
                  } else if (tabHP > netInfo.getLastHealth()) {
                     netInfo.setLastHealthTime(Minecraft.getSystemTime());
                     netInfo.setHealthBlinkTime((long)(mc.ingameGUI.getUpdateCounter() + 10));
                  }

                  netInfo.setLastHealth(tabHP);
                  netInfo.setDisplayHealth(tabHP);
                  netInfo.setLastHealthTime(Minecraft.getSystemTime());
                  float playerHP = player.getHealth();
                  float trueHP = (float)netInfo.getDisplayHealth();
                  if ((trueHP > 0.0F || player.hurtTime == 8) && trueHP < 30.0F && MathUtils.getDifferenceOf(trueHP, playerHP) >= 0.01F) {
                     player.setHealth(trueHP);
                     player.isDead = trueHP == 0.0F;
                  }
               } catch (Exception var8) {
                  var8.fillInStackTrace();
               }
            }
         }
      }

      if (this.GMFlySpoofIfCan.getBool() && Minecraft.player != null && Minecraft.player.capabilities.allowFlying && Minecraft.player.ticksExisted == 2) {
         boolean preFly = Minecraft.player.capabilities.isFlying;
         Minecraft.player.capabilities.isFlying = true;
         mc.getConnection().sendPacket(new CPacketPlayerAbilities(Minecraft.player.capabilities));
         Minecraft.player.capabilities.isFlying = preFly;
      }

      if (this.AntiAfk.getBool() && this.afkTime.hasReached(40000.0)) {
         this.doRandomPlayerAction();
      }

      boolean samiNotAfk = Minecraft.player.rotationYaw - Minecraft.player.prevRotationYaw != 0.0F
         || Minecraft.player.rotationPitch - Minecraft.player.prevRotationPitch != 0.0F
         || Minecraft.player.movementInput.jump
         || Minecraft.player.movementInput.sneak
         || Minecraft.player.movementInput.forwardKeyDown
         || Minecraft.player.movementInput.backKeyDown
         || Minecraft.player.movementInput.rightKeyDown
         || Minecraft.player.movementInput.leftKeyDown;
      if (samiNotAfk || Minecraft.player.isSwingInProgress) {
         this.afkTime.reset();
      }

      if (this.dodgeFromZero > 0) {
         this.dodgeFromZero--;
      }

      IntaveDisabler.updateIntaveDisablerState(this.isActived() && this.IntaveMovement.getBool());
      if (this.GrimWinclick.getBool() && callSneak) {
         if (timeAfterSneak.hasReached(50.0)) {
            callSneak = false;
            mc.gameSettings.keyBindForward.pressed = oldPreseds[0];
            mc.gameSettings.keyBindRight.pressed = oldPreseds[1];
            mc.gameSettings.keyBindLeft.pressed = oldPreseds[2];
            mc.gameSettings.keyBindBack.pressed = oldPreseds[3];
            mc.gameSettings.keyBindSneak.pressed = oldPreseds[5];
         } else {
            boolean ticked = Minecraft.player.ticksExisted % 2 != 0;
            mc.gameSettings.keyBindForward.pressed = false;
            mc.gameSettings.keyBindRight.pressed = false;
            mc.gameSettings.keyBindLeft.pressed = false;
            mc.gameSettings.keyBindBack.pressed = false;
            mc.gameSettings.keyBindSneak.pressed = false;
         }
      }

      hackVolume = this.FullyVolume.getBool();
      if (this.SpawnGodmode.getBool() && mc.world.playerEntities.size() <= 2) {
         if (this.gdX != 0.0 || this.gdY != 0.0 || this.gdZ != 0.0) {
            Minecraft.player.setPosition(this.gdX, this.gdY, this.gdZ);
            Minecraft.player.multiplyMotionXZ(0.0F);
         }

         if (this.gdYaw != 0.0F || this.gdPitch != 0.0F) {
            Minecraft.player.rotationYawHead = this.gdYaw;
            Minecraft.player.renderYawOffset = this.gdYaw;
            Minecraft.player.rotationPitchHead = this.gdPitch;
         }
      }

      if (this.FixStackFlags.getBool()) {
         if (this.flagCPS > 2) {
            this.doReduceDestack = true;
            this.flagCPS = 0;
         }

         this.reduceStackFlags();
         if (this.flagCPS > 0 && Minecraft.player.ticksExisted % 5 == 0) {
            this.flagCPS--;
         }
      }

      if (this.VulcanLiquid.getBool()) {
         if (Minecraft.player.ticksExisted == 1) {
            this.noted = false;
            this.strafeHacked = false;
         }

         if (Minecraft.player.ticksExisted > 30 && !this.noted && !this.strafeHacked && this.waterNeared() != null) {
            this.note();
         } else if (!this.noted && Minecraft.player.ticksExisted == 5 && mc.world != null && (mc.world == null || mc.world.getWorldType() != WorldType.FLAT)) {
            Client.msg("§f§lModules:§r §7[§lDisabler§r§7]: подойди к воде.", false);
         }

         if (this.noted && Minecraft.player.ticksExisted % 10 == 0 && !this.strafeHacked) {
            this.noted = false;
         }
      } else if (this.noted || this.strafeHacked) {
         this.noted = false;
         this.strafeHacked = false;
      }

      if (this.VulcanStrafe.getBool()) {
         if (Minecraft.player.ticksExisted % 11 == 7) {
            this.sendPacket(
               new CPacketPlayerDigging(
                  CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, BlockPos.ORIGIN.down(61), Minecraft.player.getHorizontalFacing().getOpposite()
               )
            );
         }

         this.setStrafeHacked(Minecraft.player.ticksExisted > 8 && (!mc.playerController.isHittingBlock || !(mc.playerController.curBlockDamageMP > 0.0F)));
      }

      if (this.MatrixElySpoofs.getBool() && Minecraft.player.ticksExisted % 2 == 0 && (!ElytraBoost.get.actived || !ElytraBoost.canElytra())) {
         this.sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_FALL_FLYING));
         Minecraft.player.setFlag(7, !Minecraft.player.getFlag(7));
      }
   }

   public void onAttack(boolean preSendHit) {
      if (this.isActived() && this.GMFlySpoofIfCan.getBool() && Minecraft.player.capabilities.allowFlying) {
         if (preSendHit) {
            boolean allow = Minecraft.player.capabilities.allowFlying;
            Minecraft.player.capabilities.allowFlying = false;
            mc.getConnection().preSendPacket(new CPacketPlayerAbilities(Minecraft.player.capabilities));
            Minecraft.player.capabilities.allowFlying = allow;
            if (!Minecraft.player.onGround) {
               mc.getConnection()
                  .preSendPacket(
                     new CPacketPlayer.Position(
                        Minecraft.player.posX, Minecraft.player.posY + MathUtils.clamp(Minecraft.player.motionY, -0.08, -0.002), Minecraft.player.posZ, false
                     )
                  );
            }
         } else {
            mc.getConnection().preSendPacket(new CPacketPlayerAbilities(Minecraft.player.capabilities));
         }
      }
   }

   @EventTarget
   public void onSending(EventSendPacket event) {
      if (this.actived) {
         if (this.pingDetector != null) {
            this.pingDetector.onSendPacketEvent(event);
         }

         if (event.getPacket() instanceof CPacketChatMessage messagePacket && this.ChatPluginFilters.getBool()) {
            String msg = messagePacket.getMessage();
            if (msg != null && msg.length() > 6) {
               char[] bcS = "!0123456789@/.".toCharArray();
               boolean DO_NOT_BYPASS_PLEASE = false;

               for (char bc : bcS) {
                  String badStart = String.valueOf(bc);
                  if (msg.startsWith(badStart)) {
                     DO_NOT_BYPASS_PLEASE = true;
                     break;
                  }
               }

               Runnable DO_WHILE_CAN = () -> {
                  String[] msgSplits = msg.split(" ");
                  String outFilter = "";

                  for (String msgSplit : msgSplits) {
                     boolean last = msgSplit == msgSplits[msgSplits.length - 1];
                     boolean canFilter = true;

                     try {
                        for (String name : mc.getConnection()
                           .getPlayerInfoMap()
                           .stream()
                           .filter(networkPlayerInfo -> networkPlayerInfo.getGameProfile() != null)
                           .map(networkPlayerInfo -> networkPlayerInfo.getGameProfile().getName())
                           .toList()) {
                           if (msgSplit.toLowerCase().contains(name.toLowerCase())) {
                              canFilter = false;
                              break;
                           }
                        }
                     } catch (Exception var13x) {
                        canFilter = false;
                        var13x.printStackTrace();
                     }

                     if (canFilter) {
                        outFilter = outFilter
                           + this.stringSetLikeOppositeLangIfCan(msgSplit, MathUtils.clamp(this.ChanceOppositeChars.getFloat(), 0.0F, 1.0F)).trim()
                           + (last ? "" : " ");
                     } else {
                        outFilter = outFilter + msgSplit + (last ? "" : " ");
                     }
                  }

                  Client.msg(outFilter.isEmpty() ? msg : outFilter, true);
                  messagePacket.setMessage(outFilter.isEmpty() ? msg : outFilter);
               };
               int attemptsMax = DO_NOT_BYPASS_PLEASE ? 0 : 1;
               int attempts = 0;
               synchronized (this) {
                  while (attempts < attemptsMax) {
                     DO_WHILE_CAN.run();
                     attempts++;
                  }
               }
            }
         }

         if (event.getPacket() instanceof CPacketAnimation animation
            && this.isActived()
            && get.NoSwing.getBool()
            && this.SwingCancelMode.getMode().equalsIgnoreCase("Server")
            && !get.NoSwingOnlyHitAura.getBool()) {
            event.cancel();
         }

         if (event.getPacket() instanceof CPacketConfirmTeleport teleport
            && this.PortalGodmode.getBool()
            && mc.world != null
            && (cancelPortal || callTeleportPos.distanceTo(new Vec3d(0.0, 0.0, 0.0)) > 0.0)) {
            if (!cancelPortal) {
               cancelPortal = true;
               callTeleportPos = new Vec3d(0.0, 0.0, 0.0);
               this.postPortalCancel = () -> {
                  Client.msg("§f§lModules:§r §7[§lBypass§r§7]: PortalGodmode -> Активен", false);
                  Client.msg("для остановки PortalGodmode выйдите из портала.", false);
               };
            }

            event.cancel();
         }

         if (event.getPacket() instanceof CPacketEntityAction action
            && this.AntiHunger.getBool()
            && (action.getAction() == CPacketEntityAction.Action.START_SPRINTING || action.getAction() == CPacketEntityAction.Action.STOP_SPRINTING)) {
            event.cancel();
         }

         if (event.getPacket() instanceof CPacketPlayerAbilities abilities && this.GMFlySpoofIfCan.getBool() && abilities.allowFlying) {
            abilities.flying = true;
         }

         IntaveDisabler.onSendingPackets(event);
         if ((event.getPacket() instanceof CPacketPlayer || event.getPacket() instanceof CPacketConfirmTransaction) && this.SpawnGodmode.getBool()) {
            boolean nolo = mc.world.playerEntities.size() <= 2;
            if (event.getPacket() instanceof CPacketConfirmTransaction trans && trans.getWindowId() != 0) {
               nolo = false;
            }

            if (nolo) {
               event.cancel();
            }
         }

         if (!this.usingStartPlob && event.getPacket() instanceof CPacketPlayerTryUseItem && this.RegionUsingItem.getBool()) {
            this.usingStartPlob = !(Minecraft.player.getHeldItemMainhand().getItem() instanceof ItemBlock)
               && !(Minecraft.player.getHeldItemOffhand().getItem() instanceof ItemBlock);
         }

         if (event.getPacket() instanceof CPacketPlayerTryUseItem cPacketTryUse && this.RegionUsingItem.getBool() && Minecraft.player != null) {
            ItemStack activeStack = Minecraft.player.getHeldItem(cPacketTryUse.getHand());
            Item itemInStack = activeStack.getItem();
            if (this.dodgeFromZero == 0 && (itemInStack instanceof ItemShield || itemInStack instanceof ItemFood || itemInStack instanceof ItemBow)) {
               float[] calc = this.calcClearRayTraceRotate();
               if (calc[0] != Minecraft.player.rotationYaw && calc[0] != Minecraft.player.rotationPitch) {
                  this.dodgeFromZero = 4;
                  this.tempYaw = calc[0];
                  this.tempPitch = calc[1];
               }
            }
         }

         if (this.usingStartPlob
            && Minecraft.player != null
            && (
               event.getPacket() instanceof CPacketPlayerTryUseItemOnBlock
                  || event.getPacket() instanceof CPacketUseEntity use && use.getAction() != CPacketUseEntity.Action.ATTACK
                  || Minecraft.player.getHeldItemMainhand().getItem() instanceof ItemBlock
                  || Minecraft.player.getHeldItemOffhand().getItem() instanceof ItemBlock
            )) {
            if (!(Minecraft.player.getHeldItemMainhand().getItem() instanceof ItemBlock)
               && !(Minecraft.player.getHeldItemOffhand().getItem() instanceof ItemBlock)) {
               event.cancel();
            }

            this.usingStartPlob = false;
         }

         if (mc.world != null
            && mc.world.getWorldType() == WorldType.DEFAULT
            && event.getPacket() instanceof CPacketUseEntity useE
            && this.InvPopHitFix.getBool()
            && useE.getAction() == CPacketUseEntity.Action.ATTACK) {
            Entity used = useE.getEntityFromWorld(mc.world);
            if (used != null
               && used instanceof EntityLivingBase base
               && base.isEntityAlive()
               && (Minecraft.player.openContainer == null || Minecraft.player.openContainer instanceof ContainerPlayer)
               && !(mc.currentScreen instanceof GuiInventory)
               && this.openContainerOutTime.hasReached((double)(HitAura.get.msCooldown() * 4.0F))) {
               Minecraft.player.connection.preSendPacket(new CPacketCloseWindow(0));
               this.openContainerOutTime.reset();
            }
         }

         if (event.getPacket() instanceof CPacketPlayer packet && packet.isOnGround() && this.NoServerGround.getBool()) {
            packet.onGround = false;
         }

         if (event.getPacket() instanceof CPacketClientSettings packet
            && Minecraft.player != null
            && Minecraft.player.ticksExisted > 250
            && Minecraft.player.ticksExisted % 5 != 1
            && this.FixSettingsKick.getBool()
            && !Derping.get.isActived()) {
            event.cancel();
         }
      }
   }

   public boolean canCancelServerRots() {
      return this.actived && this.NoServerRotate.getBool() && !this.doReduceDestack;
   }

   public void callServerRotsSpoof(float sYaw1, float sPitch1) {
      this.sYaw = sYaw1;
      this.sPitch = sPitch1;
      this.callSRS = true;
   }

   public boolean canFixPearlFlag() {
      return this.actived && this.FixPearlFlag.getBool();
   }

   @EventTarget
   public void onSend(EventSendPacket event) {
      if (this.callSRS
         && event.getPacket() instanceof CPacketPlayer packet
         && (packet instanceof CPacketPlayer.Rotation fPacket || packet instanceof CPacketPlayer.PositionRotation var4)
         && (this.sYaw != 0.0F || this.sPitch != 0.0F)) {
         packet.setRotation(this.sYaw, this.sPitch);
         this.callSRS = false;
      }
   }

   @EventTarget
   public void onReceive(EventReceivePacket event) {
      if (this.actived) {
         if (this.pingDetector != null) {
            this.pingDetector.onReceivePacketEvent(event);
         }

         if (event.getPacket() instanceof SPacketPlayerListItem sPacketPlayerListItem && this.PingSpoofing.getBool() && Minecraft.player != null) {
            for (SPacketPlayerListItem.AddPlayerData data : sPacketPlayerListItem.getEntries()) {
               if (data.getProfile().getId().equals(Minecraft.player.getUniqueID())) {
                  data.setPing(MathUtils.clamp(this.PingSpoofMS.getInt(), 0, 29990));
               }
            }
         }

         if (this.AntiAfk.getBool()
            && (
               event.getPacket() instanceof CPacketChatMessage
                  || event.getPacket() instanceof CPacketTabComplete
                  || event.getPacket() instanceof CPacketPlayerTryUseItem
                  || event.getPacket() instanceof CPacketPlayerTryUseItemOnBlock
                  || event.getPacket() instanceof CPacketEntityAction
                  || mc.isSingleplayer()
            )) {
            this.afkTime.reset();
         }

         IntaveDisabler.onReceivePackets(event);
         if (event.getPacket() instanceof SPacketResourcePackSend && this.NoServerPack.getBool()) {
            Minecraft.player.connection.sendPacket(new CPacketResourcePackStatus(CPacketResourcePackStatus.Action.ACCEPTED));
            Minecraft.player.connection.sendPacket(new CPacketResourcePackStatus(CPacketResourcePackStatus.Action.SUCCESSFULLY_LOADED), 100);
            event.setCancelled(true);
            Client.msg("§f§lModules:§r §7[§lBypass§r§7]: Сервер просит установку пакета", false);
            Client.msg("§7ответ запрос был подменен на ложный", false);
            Client.msg("§7необходимость установки была игнорирована игрой.", false);
         }

         if (event.getPacket() instanceof SPacketPlayerPosLook look && this.SpawnGodmode.getBool()) {
            this.gdX = look.getX();
            this.gdY = look.getY();
            this.gdZ = look.getZ();
            this.gdYaw = look.getYaw();
            this.gdPitch = look.getPitch();
         }

         if (event.getPacket() instanceof SPacketEntityStatus status
            && status != null
            && mc.world != null
            && status.getEntity(mc.world) != null
            && status.getEntity(mc.world) instanceof EntityPlayerSP SP
            && this.FixShieldCooldown.getBool()
            && status.getOpCode() == 30
            && SP.isBlocking()) {
            SP.getCooldownTracker().setCooldown(Items.SHIELD, 100);
         }

         if (this.FixStackFlags.getBool()
            && event.getPacket() instanceof SPacketPlayerPosLook look
            && Minecraft.player != null
            && Minecraft.player.getDistance(look.getX(), look.getY(), look.getZ()) < 2.0) {
            this.flagCPS++;
         }

         if (event.getPacket() instanceof SPacketPlayerPosLook look && this.actived && this.VulcanLiquid.getBool()) {
            Vec3d packetVecFlag = new Vec3d(look.x, look.y, look.z);
            Vec3d badVec = this.lastVecNote;
            if (this.getDistanceAtVec3dToVec3d(packetVecFlag, badVec) < 0.2
               && this.getDistanceAtVec3dToVec3d(packetVecFlag, this.getEntityVecPosition(Minecraft.player)) > 0.1) {
               this.noted = false;
               this.strafeHacked = false;
               Client.msg("§f§lModules:§r §7[§lBypass§r§7]: выключить Vulcan не удалось.", false);
               Client.msg("§f§lModules:§r §7[§lBypass§r§7]: попытаюсь ещё раз.", false);
            } else if (this.noted && !this.strafeHacked) {
               this.strafeHacked = true;
               Client.msg("§f§lModules:§r §7[§lBypass§r§7]: античит Vulcan выключен.", false);
            }
         }

         if (event.getPacket() instanceof SPacketOpenWindow open && Minecraft.player != null && this.CloseScreens.getBool()) {
            GuiScreen openned = mc.currentScreen;
            if (openned != null
               && (
                  openned instanceof GuiChat
                        && !CommandGui.isHoveredToPanel(false)
                        && (!open.getGuiId().endsWith("container") || mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY()) == null)
                     || openned instanceof ClickGuiScreen
                     || openned instanceof GuiInventory
               )) {
               this.sendPacket(new CPacketCloseWindow(open.getWindowId()));
               event.setCancelled(true);
            }
         }

         if (event.getPacket() instanceof SPacketCloseWindow close && Minecraft.player != null && this.CloseScreens.getBool()) {
            GuiScreen openned = mc.currentScreen;
            if (openned != null
               && (
                  openned instanceof GuiChat && !CommandGui.isHoveredToPanel(false)
                     || openned instanceof ClickGuiScreen
                     || openned instanceof GuiInventory
                     || openned instanceof GuiOptions
                     || openned instanceof GuiScreenResourcePacks
                     || openned instanceof GuiIngameMenu
               )) {
               event.setCancelled(true);
            }
         }
      }
   }

   @Override
   public void onToggled(boolean actived) {
      if (actived) {
         this.doReduceDestack = false;
         this.flagCPS = 0;
         this.flagReduceTicks = 0;
         cancelPortal = false;
      } else {
         this.doReduceDestack = false;
         this.flagCPS = 0;
         this.flagReduceTicks = 0;
         this.setStrafeHacked(false);
         cancelPortal = false;
      }

      this.afkTime.reset();
      IntaveDisabler.resetDisabler();
      hackVolume = false;
      if (this.pingDetector != null) {
         this.pingDetector.setEnabled(actived && this.FastTruePingSystem.getBool());
      }

      super.onToggled(actived);
   }

   private float[] calcClearRayTraceRotate() {
      int countOffset = 4;
      float prevPitch = Minecraft.player.rotationPitch;
      float prevYaw = Minecraft.player.rotationYaw;
      float pTicks = mc.getRenderPartialTicks();
      float pitch = prevPitch;
      float yaw = prevYaw;
      List<Integer> yaws = new ArrayList<>();
      List<Integer> pitches = new ArrayList<>();

      for (int yaw1 = 0; yaw1 < 360; yaw1 += 3) {
         if (yaw1 + (int)yaw <= 360) {
            yaws.add(yaw1 + (int)yaw);
         }

         if (-yaw1 + (int)yaw >= 0) {
            yaws.add(-yaw1 + (int)yaw);
         }
      }

      for (int pitch1 = 0; pitch1 < 180; pitch1 += 3) {
         if (pitch1 + (int)pitch <= 90) {
            pitches.add(pitch1 + (int)pitch);
         }

         if (-pitch1 + (int)pitch >= -90) {
            pitches.add(-pitch1 + (int)pitch);
         }
      }

      boolean doBreak = false;
      mc.playerController.setBlockReachDistances(5.5F, 5.5F);
      mc.entityRenderer.getMouseOver(1.0F);
      RayTraceResult ray = mc.objectMouseOver;
      if (mc.objectMouseOver != null && ray.entityHit == null && ray.getBlockPos() != null && mc.world.isAirBlock(ray.getBlockPos())) {
         return new float[]{yaw, pitch};
      } else {
         label68:
         for (int numYaw : yaws) {
            int counter = countOffset;
            Minecraft.player.rotationYaw = (float)numYaw;
            Iterator var14 = pitches.iterator();

            while (true) {
               if (var14.hasNext()) {
                  int numPitch = (Integer)var14.next();
                  Minecraft.player.rotationPitch = (float)numPitch;
                  mc.entityRenderer.getMouseOver(1.0F);
                  ray = mc.objectMouseOver;
                  if (mc.objectMouseOver != null && ray.entityHit == null && ray.getBlockPos() != null && mc.world.isAirBlock(ray.getBlockPos())) {
                     counter--;
                  }

                  if (counter != 0) {
                     continue;
                  }

                  pitch = (float)numPitch;
                  yaw = (float)numYaw;
                  doBreak = true;
               }

               if (doBreak) {
                  break label68;
               }
               break;
            }
         }

         Minecraft.player.rotationPitch = prevPitch;
         mc.playerController.setBlockReachDistances(5.0F, 4.5F);
         mc.entityRenderer.getMouseOver(pTicks);
         Minecraft.player.rotationYaw = prevYaw;
         return new float[]{yaw, pitch};
      }
   }

   @EventTarget
   public void onUpdate(EventPlayerMotionUpdate event) {
      if (this.actived && this.NCPMovement.getBool() && ElytraBoost.canElytra()) {
         double x = Minecraft.player.posX;
         double y = Minecraft.player.posY;
         double z = Minecraft.player.posZ;
         boolean fl = true;
         if (!(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra)) {
            if (Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemAir) {
               mc.playerController.windowClick(0, ElytraBoost.getItemElytra(), 1, ClickType.QUICK_MOVE, Minecraft.player);
            } else {
               mc.playerController.windowClick(0, 6, 1, ClickType.QUICK_MOVE, Minecraft.player);
               mc.playerController.windowClick(0, ElytraBoost.getItemElytra(), 1, ClickType.QUICK_MOVE, Minecraft.player);
               Client.msg("§f§lModules:§r §7[§lBypass§r§7]: пытаюсь выключить NCP.", false);
            }
         }

         if (Minecraft.player.getFlag(7)) {
            event.setGround(false);
            double nY = event.getY() + 0.4 + (Minecraft.player.ticksExisted % 2 != 0 ? 0.001 : 0.0);
            event.setPosY(nY);
            Minecraft.player.posY = nY;
            this.ncpDisabledLocalBool = true;
            float pitch = event.getPitch();
            if (mc.objectMouseOver != null && mc.objectMouseOver.hitVec != null) {
               float prevHeight = Minecraft.player.height;
               Minecraft.player.height = 0.4F;
               pitch = RotationUtil.getNeededFacing(mc.objectMouseOver.hitVec, false, Minecraft.player, false)[1];
               Minecraft.player.height = prevHeight;
            }

            event.setPitch(MathUtils.clamp(pitch, -45.0F, 30.0F));
            Minecraft.player.prevRotationPitchHead = event.getPitch();
         } else if ((double)Minecraft.player.fallDistance > 0.1 && !this.ncpDisabledLocalBool) {
            Minecraft.player.connection.sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_FALL_FLYING));
            Minecraft.player.fallDistance = 0.0F;
            this.ncpDisabledLocalBool = true;
         } else if (Minecraft.player.onGround) {
            Minecraft.player.motionY = 0.42F;
            this.ncpDisabledLocalBool = false;
         }
      } else if (this.ncpDisabledLocalBool) {
         if (Minecraft.player.getFlag(7)) {
            Minecraft.player.connection.sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_FALL_FLYING));
            Minecraft.player.setFlag(7, false);
         }

         this.ncpDisabledLocalBool = false;
      }

      if (this.dodgeFromZero == 1) {
         event.setYaw(this.tempYaw);
         event.setPitch(this.tempPitch);
         Minecraft.player.rotationYawHead = this.tempYaw;
         Minecraft.player.renderYawOffset = this.tempYaw;
         Minecraft.player.rotationPitchHead = this.tempPitch;
         HitAura.get.rotations[0] = this.tempYaw;
         HitAura.get.rotations[1] = this.tempPitch;
         if (Minecraft.player.isHandActive() && Minecraft.player.getActiveHand() != null) {
            mc.getConnection().sendPacket(new CPacketPlayerTryUseItem(Minecraft.player.getActiveHand()));
            Minecraft.player.activeItemStackUseCount++;
         }
      }
   }

   @EventTarget
   public void onMovementInput(EventMovementInput event) {
      if (this.dodgeFromZero == 1) {
         MoveMeHelp.fixDirMove(event, this.tempYaw);
      }
   }

   @EventTarget
   public void onSilentStrafe(EventRotationStrafe event) {
      if (this.dodgeFromZero == 1) {
         event.setYaw(this.tempYaw);
      }
   }

   @EventTarget
   public void onSilentJump(EventRotationJump event) {
      if (this.dodgeFromZero == 1) {
         event.setYaw(this.tempYaw);
      }
   }

   private String stringSetLikeOppositeLangIfCan(String original, float chance01) {
      if (chance01 <= 0.0F) {
         return original;
      } else {
         Random random = new Random(2001299L + System.currentTimeMillis());
         String newString = "";

         for (String alphaBet : original.split("")) {
            newString = newString + (chance01 != 1.0F && !(random.nextFloat() <= chance01) ? alphaBet : this.alphabetReplaceOppositeIfCan(alphaBet));
         }

         return newString.length() < original.length() ? original : newString;
      }
   }

   private String alphabetReplaceOppositeIfCan(String alphaBetTarget) {
      String toSet = null;
      boolean isRu = false;
      int index = 0;

      for (char ru : this.ruAlphabetsLow) {
         if (alphaBetTarget.toLowerCase().equalsIgnoreCase(String.valueOf(ru))) {
            isRu = true;
            toSet = String.valueOf(this.ruAlphabetsLowToEn[index]);
            break;
         }

         index++;
      }

      boolean isEn = false;
      index = 0;
      if (!isRu) {
         for (char en : this.enAlphabetsLow) {
            if (alphaBetTarget.toLowerCase().equalsIgnoreCase(String.valueOf(en))) {
               isEn = true;
               toSet = String.valueOf(this.enAlphabetsLowToRu[index]);
               break;
            }

            index++;
         }
      }

      if (toSet != null && (isRu || isEn)) {
         return Character.isUpperCase(alphaBetTarget.charAt(0)) ? toSet.toUpperCase() : toSet;
      } else {
         return alphaBetTarget;
      }
   }

   private class FastTruePingDetector {
      private long nanosOnSend;
      private long nanosOnReceive;
      private boolean waitingReceive;
      private boolean hasGetLastData;
      private boolean detectorEnabled;
      private long delaySendRequestMS;
      private final TimerHelper timeoutSendsRequests;
      private float latestCalcPingMsGet;

      public FastTruePingDetector(long delaySendRequestMS) {
         this.delaySendRequestMS = delaySendRequestMS;
         this.timeoutSendsRequests = TimerHelper.TimerHelperReseted();
      }

      public void setDelaySendRequestMS(long delaySendRequestMS) {
         this.delaySendRequestMS = delaySendRequestMS;
      }

      public void setEnabled(boolean enabled) {
         this.detectorEnabled = enabled;
      }

      public boolean isDetectorEnabled() {
         return this.detectorEnabled;
      }

      protected float nanosToMs(long nanos) {
         return (float)nanos / 1000000.0F;
      }

      public float getOutMsPing(float dataWhileNull) {
         return this.hasGetLastData ? this.latestCalcPingMsGet : dataWhileNull;
      }

      public void onUpdateTickLimited() {
         boolean cancelProceed = Module.mc.world == null || !this.detectorEnabled;
         if (!cancelProceed) {
            boolean setReset = Minecraft.player.ticksExisted < 2;
            if (setReset) {
               this.timeoutSendsRequests.reset();
               this.hasGetLastData = false;
               this.latestCalcPingMsGet = (float)(this.nanosOnReceive = this.nanosOnSend = 0L);
            } else {
               if (this.delaySendRequestMS <= 0L || this.timeoutSendsRequests.hasReached((double)this.delaySendRequestMS)) {
                  this.timeoutSendsRequests.reset();

                  try {
                     Module.mc.getConnection().sendPacket(new CPacketClientStatus(CPacketClientStatus.State.REQUEST_STATS));
                  } catch (Exception var4) {
                     var4.printStackTrace();
                  }
               }
            }
         }
      }

      public void onSendPacketEvent(EventSendPacket event) {
         if (event.getPacket() instanceof CPacketClientStatus packet
            && this.detectorEnabled
            && packet.getStatus() == CPacketClientStatus.State.REQUEST_STATS
            && !this.waitingReceive) {
            this.nanosOnSend = System.nanoTime();
            this.waitingReceive = true;
         }
      }

      public void onReceivePacketEvent(EventReceivePacket event) {
         if (event.getPacket() instanceof SPacketStatistics packet && this.detectorEnabled && this.waitingReceive) {
            this.nanosOnReceive = !(this.hasGetLastData = packet.getStatisticMap() != null) ? this.nanosOnSend : System.nanoTime();
            this.latestCalcPingMsGet = this.nanosToMs(this.nanosOnReceive - this.nanosOnSend);
            this.waitingReceive = false;
         }
      }
   }
}
