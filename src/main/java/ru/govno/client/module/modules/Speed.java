package ru.govno.client.module.modules;

import java.util.Arrays;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemLingeringPotion;
import net.minecraft.item.ItemSplashPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.govno.client.Client;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventMove2;
import ru.govno.client.event.events.EventMoveKeys;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventReceivePacket;
import ru.govno.client.event.events.EventRotationJump;
import ru.govno.client.event.events.EventRotationStrafe;
import ru.govno.client.event.events.EventSprintBlock;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.BlockUtils;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;
import ru.govno.client.utils.Movement.MovementHelper;

public class Speed extends Module {
   public static Speed get;
   public ModeSettings AntiCheat;
   public FloatSettings SpeedF;
   public FloatSettings MulMinBaseSpeed;
   public FloatSettings MulEffectSpeedBoost;
   public FloatSettings MulAttribSpeedBoost;
   public FloatSettings JumpBoostMul;
   public FloatSettings SleepTimeMS;
   public FloatSettings DamageMotionMul;
   public BoolSettings OnlyMove;
   public BoolSettings Bhop;
   public BoolSettings BhopOnlyDamage;
   public BoolSettings DamageBoost;
   public BoolSettings IceSpeed;
   public BoolSettings AirBoost;
   public BoolSettings GroundBoost;
   public BoolSettings Yport;
   public BoolSettings SnowBoost;
   public BoolSettings StrafeDamageHop;
   public BoolSettings LongHop;
   public BoolSettings OnGround;
   public BoolSettings UseTimer;
   public BoolSettings Caress;
   public BoolSettings RuleNoLiquid;
   public BoolSettings SleepIfFlag;
   public BoolSettings PullDown;
   public BoolSettings ArmorCheckAttributes;
   public BoolSettings KTGroundBoost;
   public BoolSettings MoveSideRotate;
   public BoolSettings TryAutoSettings;
   public BoolSettings AdvancedSpeedSettings;
   public BoolSettings JumpTickBoost;
   public BoolSettings DamageMotionBoost;
   public BoolSettings Degree45Boost;
   private boolean enabledWithModeVanillaAir;
   public static boolean snowGo = false;
   public static boolean snowGround = false;
   private final TimerHelper areaTimer = new TimerHelper();
   public boolean cancelStrafe;
   public static float ncpSpeed = 0.0F;
   public static boolean iceGo;
   private final TimerHelper ncpIceTimer = new TimerHelper();
   private final TimerHelper forGuardianTimer = new TimerHelper();
   private final TimerHelper forRipServerTimer = new TimerHelper();
   private final TimerHelper timeGrop = TimerHelper.TimerHelperReseted();
   public boolean droped = false;
   private boolean isAutoAdvSettings;
   private boolean useTimer;
   private boolean ruleNoLiquid;
   private boolean sleepIfFlag;
   private boolean pullDown;
   private boolean armorCheckAttributes;
   private boolean ktGroundBoost;
   private boolean moveSideRotate;
   private boolean advancedSpeedSettings;
   private boolean jumpTickBoost;
   private boolean damageMotionBoost;
   private float sleepTimeMS;
   private float mulMinBaseSpeed;
   private float mulEffectSpeedBoost;
   private float mulAttribSpeedBoost;
   private float jumpBoostMul;
   private float damageMotionMul;
   public static double speedUpdated = 0.0;
   private boolean prevIsCollided;
   private boolean isCollided;
   private int ticksNoCollide;
   private int pauseTicksOutEly;
   private boolean degreeOffset;
   private int sideDegreeInt = 1;
   private boolean preGround;
   private float preFallDistance;
   public boolean sleep;
   private final TimerHelper lastFlagTimer = TimerHelper.TimerHelperReseted();
   public int sleepTicks;
   private float lastSlicedYaw;

   public Speed() {
      super("Speed", 0, Module.Category.MOVEMENT);
      get = this;
      this.settings
         .add(
            this.AntiCheat = new ModeSettings(
               "AntiCheat",
               "Matrix",
               this,
               new String[]{
                  "Matrix",
                  "AAC",
                  "NCP",
                  "Guardian",
                  "RipServer",
                  "Intave",
                  "Vanilla",
                  "Vulcan",
                  "Strict",
                  "Grim",
                  "VanillaAir",
                  "Matrix&NCP",
                  "GrimDrop",
                  "MetaStrafe",
                  "Average",
                  "Matrix7",
                  "Degree45"
               }
            )
         );
      this.settings.add(this.SpeedF = new FloatSettings("Speed", 0.8F, 2.0F, 0.23F, this, () -> this.AntiCheat.getMode().contains("Vanilla")));
      this.settings.add(this.OnlyMove = new BoolSettings("OnlyMove", false, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("Vanilla")));
      this.settings
         .add(
            this.Bhop = new BoolSettings(
               "Bhop", true, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("Matrix") || this.AntiCheat.getMode().equalsIgnoreCase("Matrix&NCP")
            )
         );
      this.settings
         .add(
            this.BhopOnlyDamage = new BoolSettings(
               "BhopOnlyDamage", false, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("Matrix") && this.Bhop.getBool()
            )
         );
      this.settings
         .add(
            this.DamageBoost = new BoolSettings(
               "DamageBoost",
               true,
               this,
               () -> this.AntiCheat.getMode().equalsIgnoreCase("Matrix")
                     || this.AntiCheat.getMode().equalsIgnoreCase("NCP")
                     || this.AntiCheat.getMode().equalsIgnoreCase("Matrix&NCP")
            )
         );
      this.settings.add(this.IceSpeed = new BoolSettings("IceSpeed", true, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("NCP")));
      this.settings.add(this.AirBoost = new BoolSettings("AirBoost", false, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("Matrix")));
      this.settings
         .add(
            this.GroundBoost = new BoolSettings(
               "GroundBoost", false, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("Matrix") || this.AntiCheat.getMode().equalsIgnoreCase("Matrix&NCP")
            )
         );
      this.settings
         .add(
            this.Yport = new BoolSettings(
               "Yport", false, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("Matrix") || this.AntiCheat.getMode().equalsIgnoreCase("NCP")
            )
         );
      this.settings.add(this.SnowBoost = new BoolSettings("SnowBoost", false, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("Matrix")));
      this.settings.add(this.StrafeDamageHop = new BoolSettings("StrafeDamageHop", true, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("Matrix")));
      this.settings.add(this.LongHop = new BoolSettings("LongHop", true, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("AAC")));
      this.settings.add(this.OnGround = new BoolSettings("OnGround", true, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("AAC")));
      this.settings
         .add(
            this.UseTimer = new BoolSettings(
               "UseTimer",
               true,
               this,
               () -> this.AntiCheat.getMode().equalsIgnoreCase("NCP")
                     || this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe") && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings.add(this.Caress = new BoolSettings("Caress", false, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("Intave")));
      this.settings.add(this.TryAutoSettings = new BoolSettings("TryAutoSettings", false, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe")));
      this.settings
         .add(
            this.RuleNoLiquid = new BoolSettings(
               "RuleNoLiquid", true, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe") && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.SleepIfFlag = new BoolSettings(
               "SleepIfFlag", true, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe") && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.SleepTimeMS = new FloatSettings(
               "SleepTimeMS",
               3000.0F,
               10000.0F,
               100.0F,
               this,
               () -> this.AntiCheat.getMode().contains("MetaStrafe") && this.SleepIfFlag.getBool() && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.PullDown = new BoolSettings(
               "PullDown", true, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe") && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.ArmorCheckAttributes = new BoolSettings(
               "ArmorCheckAttributes", false, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe") && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.KTGroundBoost = new BoolSettings(
               "KTGroundBoost", false, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe") && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.MoveSideRotate = new BoolSettings(
               "MoveSideRotate", false, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe") && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.AdvancedSpeedSettings = new BoolSettings(
               "AdvancedSpeedSettings", false, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe") && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.MulMinBaseSpeed = new FloatSettings(
               "MulMinBaseSpeed",
               1.0F,
               1.5F,
               0.75F,
               this,
               () -> this.AntiCheat.getMode().contains("MetaStrafe") && this.AdvancedSpeedSettings.getBool() && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.MulEffectSpeedBoost = new FloatSettings(
               "MulEffectSpeedBoost",
               1.0F,
               1.5F,
               0.5F,
               this,
               () -> this.AntiCheat.getMode().contains("MetaStrafe") && this.AdvancedSpeedSettings.getBool() && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.MulAttribSpeedBoost = new FloatSettings(
               "MulAttribSpeedBoost",
               1.0F,
               2.0F,
               0.5F,
               this,
               () -> this.AntiCheat.getMode().contains("MetaStrafe") && this.AdvancedSpeedSettings.getBool() && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.JumpTickBoost = new BoolSettings(
               "JumpTickBoost",
               false,
               this,
               () -> this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe") && this.AdvancedSpeedSettings.getBool() && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.JumpBoostMul = new FloatSettings(
               "JumpBoostMul",
               2.0F,
               3.5F,
               1.0F,
               this,
               () -> this.AntiCheat.getMode().contains("MetaStrafe")
                     && this.AdvancedSpeedSettings.getBool()
                     && this.JumpTickBoost.getBool()
                     && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.DamageMotionBoost = new BoolSettings(
               "DamageMotionBoost", false, this, () -> this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe") && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.DamageMotionMul = new FloatSettings(
               "DamageMotionMul",
               1.25F,
               2.0F,
               1.0F,
               this,
               () -> this.AntiCheat.getMode().contains("MetaStrafe") && this.DamageMotionBoost.getBool() && !this.isActiveAutoAdvSettings()
            )
         );
      this.settings
         .add(
            this.Degree45Boost = new BoolSettings(
               "Degree45Boost",
               false,
               this,
               () -> this.AntiCheat.getMode().equalsIgnoreCase("Average")
                     || this.AntiCheat.getMode().equalsIgnoreCase("Matrix7")
                     || this.AntiCheat.getMode().equalsIgnoreCase("Matrix&NCP")
            )
         );
      this.setDemand(1, 2);
   }

   public static boolean posBlock(double x, double y, double z) {
      return mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.AIR
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.WATER
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.LAVA
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.BED
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.CAKE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.TALLGRASS
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.STONE_BUTTON
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.WOODEN_BUTTON
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.FLOWER_POT
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.CHORUS_FLOWER
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.RED_FLOWER
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.YELLOW_FLOWER
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.SAPLING
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.VINE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.ACACIA_FENCE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.ACACIA_FENCE_GATE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.BIRCH_FENCE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.BIRCH_FENCE_GATE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.DARK_OAK_FENCE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.DARK_OAK_FENCE_GATE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.JUNGLE_FENCE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.JUNGLE_FENCE_GATE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.NETHER_BRICK_FENCE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.OAK_FENCE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.OAK_FENCE_GATE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.SPRUCE_FENCE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.SPRUCE_FENCE_GATE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.ENCHANTING_TABLE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.END_PORTAL_FRAME
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.DOUBLE_PLANT
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.STANDING_SIGN
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.WALL_SIGN
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.SKULL
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.DAYLIGHT_DETECTOR
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.DAYLIGHT_DETECTOR_INVERTED
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.PURPUR_SLAB
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.STONE_SLAB
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.WOODEN_SLAB
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.CARPET
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.DEADBUSH
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.REDSTONE_WIRE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.WALL_BANNER
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.REEDS
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.UNLIT_REDSTONE_TORCH
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.TORCH
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.REDSTONE_WIRE
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.WATERLILY
         && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.SNOW_LAYER;
   }

   public static boolean canMatrixBoost() {
      double x = Minecraft.player.posX;
      double y = Minecraft.player.posY;
      double z = Minecraft.player.posZ;
      float ex = 0.39F;
      if ((
            mc.world.getBlockState(new BlockPos(x, y - (double)ex, z)).getBlock() == Blocks.PURPUR_SLAB
               || mc.world.getBlockState(new BlockPos(x, y - (double)ex, z)).getBlock() == Blocks.STONE_SLAB2
               || mc.world.getBlockState(new BlockPos(x, y - (double)ex, z)).getBlock() == Blocks.STONE_SLAB
               || mc.world.getBlockState(new BlockPos(x, y - (double)ex, z)).getBlock() == Blocks.WOODEN_SLAB
         )
         && Minecraft.player.posY + 0.5 >= (double)((int)Minecraft.player.posY)) {
         ex += 0.62F;
      }

      return (double)Minecraft.player.fallDistance > 0.1
            && !(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra)
            && (
               posBlock(x, y - (double)ex, z)
                  || posBlock(x - 0.3, y - (double)ex, z - 0.3)
                  || posBlock(x + 0.3, y - (double)ex, z + 0.3)
                  || posBlock(x - 0.3, y - (double)ex, z + 0.3)
                  || posBlock(x + 0.3, y - (double)ex, z - 0.3)
                  || posBlock(x + 0.3, y - (double)ex, z)
                  || posBlock(x - 0.3, y - (double)ex, z)
                  || posBlock(x, y - (double)ex, z - 0.3)
                  || posBlock(x, y - (double)ex, z + 0.3)
            )
         ? MoveMeHelp.getSpeed() > MoveMeHelp.getSpeedByBPS(4.3)
         : false;
   }

   @Override
   public void onMovement() {
      if (Minecraft.player != null && mc.world != null) {
         try {
            this.speedMove(this.AntiCheat.currentMode);
         } catch (Exception var2) {
            var2.printStackTrace();
         }
      }
   }

   @Override
   public String getDisplayName() {
      return this.getDisplayByMode(this.AntiCheat.currentMode);
   }

   @Override
   public void onUpdate() {
      if (Minecraft.player != null && mc.world != null) {
         try {
            this.speed(this.AntiCheat.currentMode);
         } catch (Exception var2) {
            var2.printStackTrace();
         }
      }
   }

   @Override
   public void onToggled(boolean actived) {
      if (!actived) {
         this.onDisableSpeed();
      }

      if (actived && this.AntiCheat.currentMode.equalsIgnoreCase("VanillaAir")) {
         this.enabledWithModeVanillaAir = true;
      }

      get.cancelStrafe = false;
      speedUpdated = 0.0;
      this.isCollided = false;
      this.prevIsCollided = false;
      if (Minecraft.player != null) {
         this.ticksNoCollide = Minecraft.player.ticksExisted;
      }

      if (actived) {
         if (Minecraft.player != null) {
            this.lastSlicedYaw = Minecraft.player.rotationYaw;
         }
      } else {
         this.lastSlicedYaw = 0.0F;
      }

      this.degreeOffset = false;
      super.onToggled(actived);
   }

   private void onDisableSpeed() {
      if (this.AntiCheat.currentMode.equalsIgnoreCase("NCP")) {
         this.forNCPoff(this.UseTimer.getBool());
      }

      if (this.AntiCheat.currentMode.equalsIgnoreCase("MetaStrafe")
         && this.KTGroundBoost.getBool()
         && !Minecraft.player.isJumping()
         && FriendsSLink.getPVPTimeSecInt() > 0) {
         Minecraft.player.jump();
      }

      if (this.AntiCheat.currentMode.equalsIgnoreCase("AAC") && mc.timer.speed == 1.2) {
         mc.timer.speed = 1.0;
      }

      if ((this.AntiCheat.currentMode.equalsIgnoreCase("Intave") || this.AntiCheat.currentMode.equalsIgnoreCase("Strict")) && mc.timer.speed != 1.0) {
         this.forIntaveOrStrictOff();
      }

      if (this.AntiCheat.currentMode.equalsIgnoreCase("Matrix")) {
         snowGo = false;
         snowGround = false;
      }

      if (this.AntiCheat.currentMode.equalsIgnoreCase("VanillaAir") || this.enabledWithModeVanillaAir) {
         this.forVanillaAirOff();
      }

      this.cancelStrafe = false;
      ncpSpeed = 0.0F;
   }

   private void speed(String antiCheat) {
      if ((antiCheat.equalsIgnoreCase("NCP") || antiCheat.equalsIgnoreCase("MetaStrafe")) && TargetStrafe.goStrafe()
         || antiCheat != null && MoveMeHelp.isMoving()) {
         if (antiCheat.equalsIgnoreCase("Matrix")) {
            this.forMatrix(Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ);
         }

         if (antiCheat.equalsIgnoreCase("Matrix&NCP")) {
            this.forMatrixNCP(Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ);
         }

         if (antiCheat.equalsIgnoreCase("AAC")) {
            this.forAAC();
         }

         if (antiCheat.equalsIgnoreCase("NCP")) {
            this.forNCP(
               this.UseTimer.getBool(),
               this.DamageBoost.getBool(),
               this.IceSpeed.getBool(),
               this.Yport.getBool(),
               Minecraft.player.posX,
               Minecraft.player.posY,
               Minecraft.player.posZ
            );
         } else if (Minecraft.player.speedInAir == 0.06F || Minecraft.player.speedInAir == 0.05F) {
            Minecraft.player.speedInAir = 0.02F;
         }

         if (antiCheat.equalsIgnoreCase("Guardian")) {
            this.forGuardian();
         }

         if (antiCheat.equalsIgnoreCase("RipServer")) {
            this.forRipServer();
         }

         if (antiCheat.equalsIgnoreCase("Intave")) {
            this.forIntave(Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ, this.Caress.getBool());
         }

         if (antiCheat.equalsIgnoreCase("Vulcan")) {
            this.forVulcan();
         }

         if (antiCheat.equalsIgnoreCase("Strict")) {
            this.forStrict();
         }

         if (antiCheat.equalsIgnoreCase("Grim")) {
            this.forGrim();
         }

         if (antiCheat.equalsIgnoreCase("VanillaAir")) {
            this.forVanillaAir();
         }

         if (antiCheat.equalsIgnoreCase("GrimDrop")) {
            this.forGrimDrop();
         }

         if (antiCheat.equalsIgnoreCase("MetaStrafe")) {
            this.forMetaStrafe(false);
         }

         if (antiCheat.equalsIgnoreCase("Average")) {
            this.forAverage(false);
         }

         if (antiCheat.equalsIgnoreCase("Matrix7")) {
            this.forMatrix7(false);
         }

         if (antiCheat.equalsIgnoreCase("Test")) {
            this.forTest(false);
         }
      }
   }

   private void forVulcan() {
      PotionEffect active = Minecraft.player.getActivePotionEffect(MobEffects.SPEED);
      boolean isSpeed = active != null && active.getAmplifier() >= 0 && active.getDuration() > 9;
      boolean isSpeed2 = active != null && active.getAmplifier() >= 1 && active.getDuration() > 9;
      int ticks = Minecraft.player.ticksExisted;
      AxisAlignedBB bx = Minecraft.player.boundingBox;
      if (Minecraft.player.movementInput.jump) {
         if (Minecraft.player.onGround) {
            return;
         }

         if (Minecraft.player.fallDistance != 0.0F && (double)Minecraft.player.fallDistance < 0.5) {
            Minecraft.player.motionY -= 0.1F;
            mc.timer.field_194147_b = 0.1F;
         }

         if (!isSpeed && !isSpeed2) {
            MoveMeHelp.setSpeed(MathUtils.clamp(MoveMeHelp.getSpeed(), 0.29F, MoveMeHelp.getSpeedByBPS(5.25)));
            return;
         }

         MoveMeHelp.setSpeed(
            MathUtils.clamp(MoveMeHelp.getSpeed(), Minecraft.player.onGround ? 0.12F : (isSpeed ? 0.374F : (isSpeed2 ? 0.449F : 0.29F)), MoveMeHelp.getSpeed())
         );
      } else if (!mc.world.getCollisionBoxes(Minecraft.player, new AxisAlignedBB(bx.minX, bx.minY - 0.08F, bx.minZ, bx.maxX, bx.minY, bx.maxZ)).isEmpty()
         && mc.world.getCollisionBoxes(Minecraft.player, bx).isEmpty()
         && MoveMeHelp.getSpeed() > 0.1) {
         double speed = isSpeed2 ? 0.4 : (isSpeed ? 0.35 : 0.29);
         if (Minecraft.player.onGround && Minecraft.player.ticksExisted % 3 == 0) {
            Minecraft.player.motionY = 0.0391;
            speed = isSpeed2 ? 0.59 : 0.49;
         }

         if (ticks % 3 == 2) {
            mc.timer.field_194147_b = 0.1F;
         }

         MoveMeHelp.setSpeed(speed);
      }
   }

   private void forStrict() {
      mc.timer.field_194147_b = Minecraft.player.ticksExisted % 6 == 0 ? 1.3F : 0.0F;
      if (Minecraft.player.isJumping() && Entity.Getmotiony < 0.1 && Minecraft.player.fallDistance <= 1.0F) {
         Entity.motiony = Entity.Getmotiony - 0.079;
      }
   }

   private void speedMove(String antiCheat) {
      if (antiCheat != null) {
         if (antiCheat.equalsIgnoreCase("Vanilla")) {
            this.forVanilla();
         }

         if (antiCheat.equalsIgnoreCase("RipServer")) {
            this.forRipServerMove();
         }

         if (antiCheat.equalsIgnoreCase("MetaStrafe")) {
            this.forMetaStrafe(true);
         }

         if (antiCheat.equalsIgnoreCase("Average")) {
            this.forAverage(true);
         }

         if (antiCheat.equalsIgnoreCase("Matrix7")) {
            this.forMatrix7(true);
         }

         if (antiCheat.equalsIgnoreCase("Test")) {
            this.forTest(true);
         }
      }
   }

   private void forVanilla() {
      MoveMeHelp.setMotionSpeed(true, this.OnlyMove.getBool(), (double)(this.SpeedF.getFloat() * (Minecraft.player.ticksExisted % 2 == 0 ? 1.0F : 0.99F)));
   }

   private void forIntave(double x, double y, double z, boolean caress) {
      mc.timer.speed = Minecraft.player.fallDistance == 0.0F && !Minecraft.player.onGround ? 1.12F : 1.08F;
      if (!caress) {
         if ((double)Minecraft.player.fallDistance > 0.1) {
            Minecraft.player.jumpMovementFactor = (float)((double)Minecraft.player.jumpMovementFactor + 1.3E-4);
         }

         float ex = 0.38F;
         Minecraft.player.setSprinting(Minecraft.player.fallDistance != 0.0F);
         Minecraft.player.serverSprintState = Minecraft.player.isSprinting();
         if (Minecraft.player.serverSprintState) {
            for (int i = 0; i < 2; i++) {
               Minecraft.player.connection.sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_SPRINTING));
            }
         }

         Minecraft.player.jumpTicks = 0;
         if (Minecraft.player.isCollidedVertically && MoveMeHelp.getSpeed() >= 0.4) {
            Minecraft.player.onGround = false;
         }

         if ((double)Minecraft.player.fallDistance > 0.1 && posBlock(x, y - (double)ex, z) && Minecraft.player.isJumping()) {
            mc.timer.speed = 1.5;
            Minecraft.player.fallDistance = 0.0F;
            Minecraft.player.fall(16.0F, 20.0F);
            Minecraft.player.onGround = true;
            Minecraft.player.motionY /= 1.002F;
            Minecraft.player.posY -= 0.0034;
         }
      }

      if ((!Minecraft.player.onGround || !Minecraft.player.isJumping()) && !Minecraft.player.isMoving()) {
         Minecraft.player.multiplyMotionXZ(1.0013F);
      }

      if (Minecraft.player.onGround && !Minecraft.player.isJumping() && !Minecraft.player.isMoving() && !this.Caress.getBool()) {
         MoveMeHelp.setSpeed(MoveMeHelp.getSpeed() * 1.016);
      }
   }

   private void forIntaveOrStrictOff() {
      if (mc.timer.speed != 1.0) {
         mc.timer.speed = 1.0;
      }
   }

   private void forVanillaAirOff() {
      Minecraft.player.speedInAir = 0.02F;
      this.enabledWithModeVanillaAir = false;
   }

   private void forMatrix(double x, double y, double z) {
      float w = Minecraft.player.width / 2.0F - 0.025F;
      boolean posed = posBlock(x, y - 1.0E-10, z)
         || posBlock(x + (double)w, y - 1.0E-10, z + (double)w)
         || posBlock(x - (double)w, y - 1.0E-10, z - (double)w)
         || posBlock(x + (double)w, y - 1.0E-10, z - (double)w)
         || posBlock(x - (double)w, y - 1.0E-10, z + (double)w)
         || posBlock(x + (double)w, y - 1.0E-10, z)
         || posBlock(x - (double)w, y - 1.0E-10, z)
         || posBlock(x, y - 1.0E-10, z + (double)w)
         || posBlock(x, y - 1.0E-10, z - (double)w);
      boolean yPort = !Minecraft.player.onGround
         && (double)Minecraft.player.fallDistance >= 0.068
         && posBlock(x, y - (this.AirBoost.getBool() ? 0.9 : 0.5), z)
         && this.Yport.getBool();
      boolean bHop = this.Bhop.getBool()
         && (EntityLivingBase.isMatrixDamaged || !this.BhopOnlyDamage.getBool())
         && Minecraft.player.isJumping()
         && (canMatrixBoost() || yPort)
         && !Minecraft.player.onGround
         && !Minecraft.player.isSneaking();
      boolean dBoost = this.DamageBoost.getBool()
         && EntityLivingBase.isMatrixDamaged
         && (canMatrixBoost() && bHop || Minecraft.player.onGround && Minecraft.player.isCollidedVertically && posed)
         && MoveMeHelp.getCuttingSpeed() < 1.2;
      boolean airBoost = Minecraft.player.fallDistance == 0.0F
         && posBlock(x, y - 1.0, z)
         && this.AirBoost.getBool()
         && Minecraft.player.isJumping()
         && MoveMeHelp.isMoving()
         && !Minecraft.player.isSneaking()
         && (!Minecraft.player.isCollidedVertically || Minecraft.player.posY == (double)((int)Minecraft.player.posY) || Minecraft.player.ticksExisted % 2 != 0)
         && !EntityLivingBase.isMatrixDamaged;
      boolean gBoost = Minecraft.player.onGround
         && Minecraft.player.isCollidedVertically
         && !Minecraft.player.isJumping()
         && this.GroundBoost.getBool()
         && posBlock(x, y - 1.0E-10, z);
      boolean snowBoost = this.SnowBoost.getBool();
      if (bHop && !dBoost) {
         double bSpeed = MoveMeHelp.getSpeed() * 1.9987;
         MoveMeHelp.setSpeed(bSpeed);
         MoveMeHelp.setCuttingSpeed(bSpeed / 1.06F);
      }

      if (dBoost) {
         if (bHop) {
            double bSpeed = MoveMeHelp.getSpeed() * 2.461F;
            MoveMeHelp.setSpeed(bSpeed);
            MoveMeHelp.setCuttingSpeed(bSpeed / 1.06F);
            if (Minecraft.player.stepHeight == 0.0F) {
               Minecraft.player.stepHeight = 0.6F;
            }
         } else if (!NoClip.get.actived) {
            if (Minecraft.player.stepHeight == 0.6F) {
               Minecraft.player.stepHeight = 0.0F;
            }

            float dir1 = -MathHelper.sin(MovementHelper.getDirection()) * (mc.gameSettings.keyBindBack.isKeyDown() ? -1.0F : 1.0F);
            float dir2 = MathHelper.cos(MovementHelper.getDirection()) * (mc.gameSettings.keyBindBack.isKeyDown() ? -1.0F : 1.0F);
            if (MoveMeHelp.isMoving()) {
               if (MoveMeHelp.getSpeed() < 0.08) {
                  MoveMeHelp.setSpeed(0.42);
               } else {
                  Minecraft.player.addVelocity((double)dir1 * 9.8 / 25.0, 0.0, (double)dir2 * 9.8 / 25.0);
                  MoveMeHelp.setSpeed(MoveMeHelp.getSpeed());
               }
            }
         }
      } else if (Minecraft.player.stepHeight == 0.0F) {
         Minecraft.player.stepHeight = 0.6F;
      }

      if (airBoost && !dBoost) {
         Minecraft.player.onGround = true;
      }

      if (gBoost && !dBoost && Minecraft.player.onGround) {
         Minecraft.player.motionY--;
         if (!Minecraft.player.isJumping && Minecraft.player.ticksExisted % 3 == 0) {
            Minecraft.player.multiplyMotionXZ(1.35F);
            Minecraft.player.setPosition(x, y + 9.234E-7, z);
            Minecraft.player.posY -= 9.234E-7;
         }
      }

      if (yPort) {
         Minecraft.player.motionY = -0.22;
         Entity.motiony = -4.76;
      }

      if (snowBoost) {
         if (MoveMeHelp.getSpeed() < 0.36) {
            snowGround = false;
         }

         if ((
               mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.SNOW_LAYER
                  || mc.world.getBlockState(new BlockPos(x, y - 1.0, z)).getBlock() == Blocks.SNOW_LAYER
            )
            && (
               mc.world.getBlockState(new BlockPos(x, y - 1.0E-10, z)).getBlock() != Blocks.SNOW_LAYER
                  || Minecraft.player.isJumping() && (Minecraft.player.fallDistance != 0.0F || Minecraft.player.onGround)
            )
            && Minecraft.player.getItemInUseMaxCount() == 0
            && !Minecraft.player.isSneaking
            && (!snowGo || !(MoveMeHelp.getSpeed() > 0.4) || !Minecraft.player.isJumping)) {
            if (!Minecraft.player.isJumping() && Minecraft.player.onGround) {
               snowGround = true;
            } else if (MoveMeHelp.getSpeed() > 0.35 && snowGround) {
               MoveMeHelp.setSpeed(0.35);
            }

            if (snowGround) {
               MoveMeHelp.setCuttingSpeed(0.6205);
               snowGo = true;
            } else if (Minecraft.player.onGround && MoveMeHelp.getSpeed() < 0.14) {
               MoveMeHelp.setSpeed(0.18);
            }
         } else {
            snowGo = false;
         }
      }
   }

   private void forMatrixNCP(double x, double y, double z) {
      get.cancelStrafe = false;
      int speedLvl = Minecraft.player.isPotionActive(MobEffects.SPEED) ? Minecraft.player.getActivePotionEffect(MobEffects.SPEED).getAmplifier() + 1 : 0;
      if (this.GroundBoost.getBool() && Minecraft.player.onGround && Minecraft.player.isCollidedVertically && !Minecraft.player.isJumping()) {
         boolean diagonal = (MoveMeHelp.w() || MoveMeHelp.s()) && (MoveMeHelp.a() || MoveMeHelp.d());
         float addSpeed = 0.0F;
         float mulSpeed = 1.0F;
         double speed = MoveMeHelp.getSpeed();
         switch (speedLvl) {
            case 0:
               if (speed < 0.2F) {
                  mulSpeed = diagonal ? 1.003F : 1.0195F;
               }
               break;
            case 1:
               if (speed < 0.2F) {
                  mulSpeed = diagonal ? 1.0026F : 1.0192F;
               }
               break;
            case 2:
               if (speed < 0.22F) {
                  mulSpeed = diagonal ? 1.0021F : 1.0187F;
               }
               break;
            case 3:
               mulSpeed = diagonal ? 1.00195F : 1.0185F;
         }

         mc.timer.tempSpeed = Minecraft.player.ticksExisted % 7 < 2 ? 1.4F : 1.04F;
         Minecraft.player.multiplyMotionXZ(mulSpeed);
         MoveMeHelp.setMotionSpeed(false, true, MoveMeHelp.getSpeed() + (double)addSpeed);
         get.cancelStrafe = MoveMeHelp.w() && !MoveMeHelp.s() && (MoveMeHelp.w() || MoveMeHelp.s() || !MoveMeHelp.a() && !MoveMeHelp.d());
      } else {
         get.cancelStrafe = false;
      }

      if (this.DamageBoost.getBool() && EntityLivingBase.isNcpDamaged && !Minecraft.player.isInWeb && (MoveMeHelp.isMoving() || TargetStrafe.goStrafe())) {
         if (MoveMeHelp.getSpeed() > 0.25 || Minecraft.player.onGround) {
            if (Minecraft.player.isJumping() && canMatrixBoost()) {
               ncpSpeed = 0.999F;
            } else if (Minecraft.player.onGround && !Minecraft.player.isJumping() || ncpSpeed >= 0.554F || Minecraft.player.motionY < -0.25) {
               ncpSpeed = Minecraft.player.onGround ? 0.6F : 0.554F - (Minecraft.player.ticksExisted % 2 == 0 ? 0.001F : 0.0F);
            }

            if (ncpSpeed != 0.0F && !TargetStrafe.goStrafe()) {
               MoveMeHelp.setCuttingSpeed((double)ncpSpeed / 1.06);
            }

            Minecraft.player.rotationYaw = Minecraft.player.rotationYaw + (Minecraft.player.ticksExisted % 2 == 0 ? -0.01F : 0.01F);
            this.cancelStrafe = false;
         }
      } else {
         ncpSpeed = 0.0F;
         if (Minecraft.player.isJumping() && this.Bhop.getBool() && (!EntityLivingBase.isMatrixDamaged || !this.DamageBoost.getBool())) {
            if ((
                  mc.world.getBlockState(new BlockPos(x, y - 0.51, z)).getBlock() == Block.getBlockById(212)
                     || mc.world.getBlockState(new BlockPos(x, y - 0.51, z)).getBlock() == Block.getBlockById(79)
                     || mc.world.getBlockState(new BlockPos(x, y - 0.51, z)).getBlock() == Block.getBlockById(174)
                     || mc.world.getBlockState(new BlockPos(x, y - 0.95, z)).getBlock() == Block.getBlockById(212)
                     || mc.world.getBlockState(new BlockPos(x, y - 0.95, z)).getBlock() == Block.getBlockById(79)
                     || mc.world.getBlockState(new BlockPos(x, y - 0.95, z)).getBlock() == Block.getBlockById(174)
               )
               && !BlockUtils.getBlockWithExpand(0.3, BlockUtils.getEntityBlockPos(Minecraft.player), Blocks.WATER)
               && !BlockUtils.getBlockWithExpand(0.3, BlockUtils.getEntityBlockPos(Minecraft.player), Blocks.LAVA)) {
               get.cancelStrafe = false;
            } else {
               Minecraft.player.jumpMovementFactor = 0.0265F;
               Minecraft.player.speedInAir = 0.0204F;
               double selfSpeed = Minecraft.player.getSpeed();
               double minNormalBPS = 150.0;
               switch (speedLvl) {
                  case 0:
                     minNormalBPS = 4.7;
                     break;
                  case 1:
                     minNormalBPS = 4.85;
                     break;
                  case 2:
                     minNormalBPS = 5.1;
                     break;
                  case 3:
                     minNormalBPS = 5.2;
               }

               boolean threadSpeedStable = MoveMeHelp.getSpeedByBPS(minNormalBPS) / 1.05 <= selfSpeed;
               Minecraft.player.motionY -= 0.003F;
               if (threadSpeedStable) {
                  if (Minecraft.player.fallDistance > 0.0F && Minecraft.player.fallDistance <= 1.0F) {
                     mc.timer.tempSpeed = 1.0 + MathUtils.easeInOutExpo((double)Minecraft.player.fallDistance) * 0.3F;
                  } else {
                     mc.timer.tempSpeed = 1.055F;
                  }
               }
            }
         }
      }
   }

   private void forAAC() {
      boolean longHop = this.LongHop.getBool() && (Minecraft.player.isJumping() || Minecraft.player.fallDistance != 0.0F);
      boolean onGround = this.OnGround.getBool()
         && !Minecraft.player.isJumping()
         && Minecraft.player.onGround
         && Minecraft.player.isCollidedVertically
         && MoveMeHelp.getSpeed() < 0.9;
      mc.timer.speed = 1.2;
      if (longHop) {
         Minecraft.player.jumpMovementFactor = 0.17F;
         Minecraft.player.multiplyMotionXZ(1.005F);
      }

      if (onGround) {
         Minecraft.player.multiplyMotionXZ(1.212F);
      }
   }

   private void forNCP(boolean timer, boolean damageBoost, boolean iceSpeed, boolean yPort, double x, double y, double z) {
      if (timer) {
         Timer.forceTimer(1.075F);
      }

      double speed = 0.0;
      if (yPort) {
         speed = MoveMeHelp.getSpeed();
         if (Minecraft.player.isPotionActive(Potion.getPotionById(1))) {
            Minecraft.player.speedInAir = 0.06F;
         } else {
            Minecraft.player.speedInAir = 0.05F;
         }

         if (Minecraft.player.onGround) {
            Minecraft.player.jump();
            if (Minecraft.player.isPotionActive(Potion.getPotionById(1))) {
               Minecraft.player.jump();
            }

            Minecraft.player.motionY /= 1.05;
         } else {
            if (!Minecraft.player.isCollidedHorizontally) {
               Minecraft.player.motionY--;
            }

            if (Minecraft.player.isPotionActive(Potion.getPotionById(1))) {
               speed = 0.45;
            } else {
               speed = 0.32;
            }
         }

         MoveMeHelp.setSpeed(speed);
      } else if (Minecraft.player.speedInAir == 0.06F || Minecraft.player.speedInAir == 0.05F) {
         Minecraft.player.speedInAir = 0.02F;
      }

      if (EntityLivingBase.isNcpDamaged
         && !Minecraft.player.onGround
         && !Minecraft.player.isInWeb
         && (MoveMeHelp.isMoving() || TargetStrafe.goStrafe())
         && MoveMeHelp.getSpeed() > 0.3) {
         speed = 0.55F;
      }

      if (speed != 0.0 && !TargetStrafe.goStrafe()) {
         MoveMeHelp.setCuttingSpeed(speed / 1.06);
      }

      ncpSpeed = (float)speed;
      if (iceSpeed) {
         if (mc.world.getBlockState(new BlockPos(x, y - 0.51, z)).getBlock() != Block.getBlockById(212)
            && mc.world.getBlockState(new BlockPos(x, y - 0.51, z)).getBlock() != Block.getBlockById(79)
            && mc.world.getBlockState(new BlockPos(x, y - 0.51, z)).getBlock() != Block.getBlockById(174)
            && mc.world.getBlockState(new BlockPos(x, y - 0.95, z)).getBlock() != Block.getBlockById(212)
            && mc.world.getBlockState(new BlockPos(x, y - 0.95, z)).getBlock() != Block.getBlockById(79)
            && mc.world.getBlockState(new BlockPos(x, y - 0.95, z)).getBlock() != Block.getBlockById(174)) {
            if (this.ncpIceTimer.hasReached(800.0)) {
               iceGo = false;
            }
         } else if (!BlockUtils.getBlockWithExpand(0.3, BlockUtils.getEntityBlockPos(Minecraft.player), Blocks.WATER)
            && !BlockUtils.getBlockWithExpand(0.3, BlockUtils.getEntityBlockPos(Minecraft.player), Blocks.LAVA)) {
            this.ncpIceTimer.reset();
            iceGo = true;
         }
      } else {
         iceGo = false;
      }

      if (iceSpeed && iceGo) {
         boolean isSpeedPot2 = Minecraft.player.isPotionActive(MobEffects.SPEED)
            && Minecraft.player.getActivePotionEffect(MobEffects.SPEED).getAmplifier() >= 1;
         if (Minecraft.player.isJumping) {
            if (mc.gameSettings.keyBindForward.isKeyDown()) {
               MoveMeHelp.setSpeed(isSpeedPot2 ? 0.879 : 0.623);
            } else {
               MoveMeHelp.setSpeed(isSpeedPot2 ? 0.91 : 0.63);
            }
         } else {
            MoveMeHelp.setSpeed(isSpeedPot2 ? 0.9685 : 0.687);
         }
      }
   }

   private void forNCPoff(boolean timer) {
      if (Minecraft.player.speedInAir == 0.06F || Minecraft.player.speedInAir == 0.05F) {
         Minecraft.player.speedInAir = 0.02F;
      }

      ncpSpeed = 0.0F;
      iceGo = false;
   }

   private void forGuardian() {
      if ((!Strafe.get.actived || !Strafe.get.Mode.currentMode.equalsIgnoreCase("Matrix5") || !Strafe.moves()) && EntityLivingBase.isSunRiseDamaged) {
         if (MoveMeHelp.moveKeysPressed()) {
            double speed = MathUtils.clamp(MoveMeHelp.getSpeed(), 0.2499, 9.9);
            if (Minecraft.player.onGround && !Minecraft.player.isJumping()) {
               speed *= 1.8;
            } else if (Minecraft.player.isJumping()) {
               speed *= canMatrixBoost() ? 1.8 : 1.0;
            }

            speed = MathUtils.clamp(speed, 0.2499, 1.17455998);
            MoveMeHelp.setSpeed(speed);
            MoveMeHelp.setCuttingSpeed(speed / 1.06);
         }
      } else {
         this.forGuardianTimer.reset();
      }
   }

   private void forRipServer() {
      if (this.forRipServerTimer.hasReached(10.0)
         && (
            !ElytraBoost.get.actived
               || !ElytraBoost.get.Mode.getMode().equalsIgnoreCase("MatrixFly") && !ElytraBoost.get.Mode.getMode().equalsIgnoreCase("MatrixSpeed")
               || !ElytraBoost.canElytra()
         )) {
         if (Minecraft.player.onGround && !Minecraft.player.isJumping()) {
            MoveMeHelp.setSpeed(
               MathUtils.clamp(
                  MoveMeHelp.getSpeed() * (Minecraft.player.rayGround ? 1.8 : 0.8), 0.2, MoveMeHelp.w() && Minecraft.player.isSprinting() ? 1.7155F : 1.745F
               )
            );
            Minecraft.player.rayGround = Minecraft.player.onGround;
         } else {
            Minecraft.player.serverSprintState = true;
            MoveMeHelp.setSpeed(
               MathUtils.clamp(MoveMeHelp.getSpeed() * (!Minecraft.player.onGround && !Minecraft.player.rayGround ? 1.2 : 1.0), 0.195, 1.823585F), 0.12F
            );
            Minecraft.player.rayGround = Minecraft.player.onGround;
         }

         this.forRipServerTimer.reset();
      }
   }

   private void forRipServerMove() {
      if (MoveMeHelp.isMoving()) {
         MoveMeHelp.setCuttingSpeed(MoveMeHelp.getCuttingSpeed() / 1.06F);
      }
   }

   private void forGrim() {
      if (Minecraft.player.onGround || Minecraft.player.motionY < 0.0 && !Minecraft.player.onGround) {
         Timer.forceTimer(Minecraft.player.isJumping() ? 1.02F : 1.015F);
      }

      Minecraft.player.rotationYaw = Minecraft.player.rotationYaw + (Minecraft.player.ticksExisted % 2 == 0 ? -0.25F : 0.25F);
      if (Minecraft.player.ticksExisted % 2 == 0 && Minecraft.player.fallDistance != 0.0F) {
         Minecraft.player.motionY -= 0.003F;
      }

      Minecraft.player.motionX = Minecraft.player.motionX * (Minecraft.player.onGround && !Minecraft.player.isJumping() ? 1.02844F : 1.002446F);
      Minecraft.player.motionZ = Minecraft.player.motionZ * (Minecraft.player.onGround && !Minecraft.player.isJumping() ? 1.02844F : 1.002446F);
   }

   private boolean dropItem() {
      int index = -1;
      if (Minecraft.player.getHeldItemOffhand().getItem() instanceof ItemBlock) {
         index = 45;
         mc.playerController.windowClick(0, 45, 0, ClickType.THROW, Minecraft.player);
      } else {
         for (int i = 0; i < 45; i++) {
            ItemStack stack = Minecraft.player.inventory.getStackInSlot(i);
            if (stack.getItem() instanceof ItemBlock) {
               index = i;
               if (i < 9) {
                  int oldSlot = Minecraft.player.inventory.currentItem;
                  Minecraft.player.inventory.currentItem = i;
                  mc.playerController.updateController();
                  Minecraft.player.dropItem(false);
                  Minecraft.player.inventory.currentItem = oldSlot;
                  mc.playerController.updateController();
               } else if (Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8)
                  .stream()
                  .noneMatch(slot -> Minecraft.player.inventory.getStackInSlot(slot).getItem() == Items.air)) {
                  mc.playerController.windowClick(0, i, 0, ClickType.THROW, Minecraft.player);
               } else if (stack.stackSize > 1) {
                  mc.playerController.windowClick(0, i, 0, ClickType.THROW, Minecraft.player);
                  mc.playerController.windowClick(0, i, 1, ClickType.QUICK_MOVE, Minecraft.player);
               } else {
                  mc.playerController.windowClick(0, i, 0, ClickType.THROW, Minecraft.player);
               }
               break;
            }
         }
      }

      return index != -1;
   }

   private void forGrimDrop() {
      if (!(MoveMeHelp.getSpeed() <= 0.75)) {
         this.droped = false;
      } else {
         if (this.droped) {
            List<Entity> collides = mc.world
               .getLoadedEntityList()
               .stream()
               .filter(entity -> entity instanceof EntityItem)
               .filter(ent -> Minecraft.player.getDistanceToEntity(ent) <= 1.75F)
               .toList();
            if (!collides.isEmpty()) {
               Minecraft.player.multiplyMotionXZ(Minecraft.player.onGround && !Minecraft.player.isJumping() ? 1.8F : 1.25F);
            }

            this.droped = false;
         }

         if (this.timeGrop.hasReached(50.0)) {
            this.droped = this.dropItem();
            this.timeGrop.reset();
         }
      }
   }

   private void forVanillaAir() {
      if (!Minecraft.player.isJumping) {
         Minecraft.player.onGround = false;
      }

      if (Minecraft.player.isJumping && Minecraft.player.onGround) {
         Minecraft.player.motionY = 0.42F;
      }

      Minecraft.player.speedInAir = this.SpeedF.getFloat();
      if (!Minecraft.player.onGround) {
         Minecraft.player.motionX /= 5.0;
         Minecraft.player.motionZ /= 5.0;
      }
   }

   @Override
   public void alwaysUpdateLimitedDelay() {
      if (this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe")) {
         this.updateAutoAdvSettings();
      }
   }

   private boolean updateAutoAdvSettings() {
      this.isAutoAdvSettings = this.TryAutoSettings.getBool();
      this.useTimer = this.UseTimer.getBool();
      this.ruleNoLiquid = this.RuleNoLiquid.getBool();
      this.sleepIfFlag = this.SleepIfFlag.getBool();
      this.sleepTimeMS = this.SleepTimeMS.getFloat();
      this.pullDown = this.PullDown.getBool();
      this.armorCheckAttributes = this.ArmorCheckAttributes.getBool();
      this.ktGroundBoost = this.KTGroundBoost.getBool();
      this.moveSideRotate = this.MoveSideRotate.getBool();
      this.advancedSpeedSettings = this.AdvancedSpeedSettings.getBool();
      this.mulMinBaseSpeed = this.MulMinBaseSpeed.getFloat();
      this.mulEffectSpeedBoost = this.MulEffectSpeedBoost.getFloat();
      this.mulAttribSpeedBoost = this.MulAttribSpeedBoost.getFloat();
      this.jumpTickBoost = this.JumpTickBoost.getBool();
      this.jumpBoostMul = this.JumpBoostMul.getFloat();
      this.damageMotionBoost = this.DamageMotionBoost.getBool();
      this.damageMotionMul = this.DamageMotionMul.getFloat();
      if (this.isAutoAdvSettings) {
         boolean useAuto = true;
         if (useAuto) {
            String getIp = mc.isSingleplayer()
               ? "-"
               : (mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null ? mc.getCurrentServerData().serverIP : "s-");
            String getIpLower = getIp.toLowerCase();
            if (getIp.equalsIgnoreCase("-")) {
               this.useTimer = false;
               this.ruleNoLiquid = false;
               this.sleepIfFlag = false;
               this.sleepTimeMS = 0.0F;
               this.pullDown = false;
               this.armorCheckAttributes = true;
               this.ktGroundBoost = false;
               this.moveSideRotate = false;
               this.advancedSpeedSettings = true;
               this.mulMinBaseSpeed = 1.5F;
               this.mulEffectSpeedBoost = 1.5F;
               this.mulAttribSpeedBoost = 2.0F;
               this.jumpTickBoost = true;
               this.jumpBoostMul = 1.25F;
               this.damageMotionBoost = true;
               this.damageMotionMul = 1.75F;
            } else if (getIp.equalsIgnoreCase("s-")) {
               this.useTimer = false;
               this.ruleNoLiquid = true;
               this.sleepIfFlag = true;
               this.sleepTimeMS = 500.0F;
               this.pullDown = false;
               this.armorCheckAttributes = false;
               this.ktGroundBoost = false;
               this.moveSideRotate = true;
               this.advancedSpeedSettings = false;
               this.mulMinBaseSpeed = 1.0F;
               this.mulEffectSpeedBoost = 1.0F;
               this.mulAttribSpeedBoost = 1.0F;
               this.jumpTickBoost = false;
               this.jumpBoostMul = 1.0F;
               this.damageMotionBoost = false;
               this.damageMotionMul = 1.0F;
            } else if (getIpLower.contains("metahvh")) {
               if (Minecraft.player != null) {
                  int speedLvl = Minecraft.player.isPotionActive(MobEffects.SPEED)
                     ? Minecraft.player.getActivePotionEffect(MobEffects.SPEED).getAmplifier() + 1
                     : 0;
                  boolean speedShar = false;
                  if (OffHand.get != null) {
                     OffHand.ItemStackWithSlot iswsGeted1 = new OffHand.ItemStackWithSlot(Minecraft.player.inventoryContainer.getSlot(45).getStack(), 45);
                     double val = OffHand.get
                        .getAttributeValueFromAll(OffHand.get.getStackAttributesWithoutPotions(iswsGeted1), OffHand.AttributeType.SPEED_UP);
                     if (this.MTIgetArmorCheckAttributes()) {
                        for (int slot = 5; slot <= 8; slot++) {
                           iswsGeted1 = new OffHand.ItemStackWithSlot(Minecraft.player.inventoryContainer.getSlot(5).getStack(), 5);
                           val += OffHand.get
                              .getAttributeValueFromAll(OffHand.get.getStackAttributesWithoutPotions(iswsGeted1), OffHand.AttributeType.SPEED_UP);
                        }
                     }

                     if (val > 0.0 && val < 4.0) {
                        speedShar = true;
                     }
                  }

                  this.useTimer = false;
                  this.ruleNoLiquid = true;
                  this.sleepIfFlag = Minecraft.player.ticksExisted > 105;
                  this.sleepTimeMS = 3050.0F;
                  this.pullDown = HitAura.TARGET_ROTS == null && speedLvl > 0;
                  this.armorCheckAttributes = false;
                  this.ktGroundBoost = false;
                  this.moveSideRotate = false;
                  this.advancedSpeedSettings = true;
                  if (speedLvl == 3 && speedShar) {
                     this.mulMinBaseSpeed = 1.003F;
                     this.mulEffectSpeedBoost = 1.33F;
                     this.mulAttribSpeedBoost = 1.095F;
                     this.jumpTickBoost = true;
                     this.jumpBoostMul = 1.05F;
                     this.damageMotionBoost = true;
                     this.damageMotionMul = 1.03F;
                  } else if (speedLvl == 2 && speedShar) {
                     this.mulMinBaseSpeed = 1.0F;
                     this.mulEffectSpeedBoost = 1.32F;
                     this.mulAttribSpeedBoost = 1.0F;
                     this.jumpTickBoost = true;
                     this.jumpBoostMul = 1.05F;
                     this.damageMotionBoost = true;
                     this.damageMotionMul = 1.08F;
                  } else if (speedLvl == 3) {
                     this.mulMinBaseSpeed = 1.003F;
                     this.mulEffectSpeedBoost = 1.375F;
                     this.mulAttribSpeedBoost = 1.0F;
                     this.jumpTickBoost = true;
                     this.jumpBoostMul = 1.05F;
                     this.damageMotionBoost = true;
                     this.damageMotionMul = 1.08F;
                  } else if (speedLvl == 2) {
                     this.mulMinBaseSpeed = 1.0F;
                     this.mulEffectSpeedBoost = 1.33F;
                     this.mulAttribSpeedBoost = 1.0F;
                     this.jumpTickBoost = true;
                     this.jumpBoostMul = 1.05F;
                     this.damageMotionBoost = true;
                     this.damageMotionMul = 1.05F;
                  } else if (speedShar) {
                     this.mulMinBaseSpeed = 1.003F;
                     this.mulEffectSpeedBoost = 1.33F;
                     this.mulAttribSpeedBoost = 1.0F;
                     this.jumpTickBoost = true;
                     this.jumpBoostMul = 1.5F;
                     this.damageMotionBoost = true;
                     this.damageMotionMul = 1.25F;
                  } else {
                     this.mulMinBaseSpeed = 1.003F;
                     this.mulEffectSpeedBoost = 1.0F;
                     this.mulAttribSpeedBoost = 1.0F;
                     this.jumpTickBoost = true;
                     this.jumpBoostMul = 1.05F;
                     this.damageMotionBoost = true;
                     this.damageMotionMul = 1.38F;
                  }
               } else {
                  this.isAutoAdvSettings = false;
               }
            } else if (getIpLower.contains("funsky")) {
               if (Minecraft.player != null) {
                  int speedLvlx = Minecraft.player.isPotionActive(MobEffects.SPEED)
                     ? Minecraft.player.getActivePotionEffect(MobEffects.SPEED).getAmplifier() + 1
                     : 0;
                  boolean speedSharx = false;
                  if (OffHand.get != null) {
                     OffHand.ItemStackWithSlot iswsGeted1x = new OffHand.ItemStackWithSlot(Minecraft.player.inventoryContainer.getSlot(45).getStack(), 45);
                     double valx = OffHand.get
                        .getAttributeValueFromAll(OffHand.get.getStackAttributesWithoutPotions(iswsGeted1x), OffHand.AttributeType.SPEED_UP);
                     if (this.MTIgetArmorCheckAttributes()) {
                        for (int slot = 5; slot <= 8; slot++) {
                           iswsGeted1x = new OffHand.ItemStackWithSlot(Minecraft.player.inventoryContainer.getSlot(5).getStack(), 5);
                           valx += OffHand.get
                              .getAttributeValueFromAll(OffHand.get.getStackAttributesWithoutPotions(iswsGeted1x), OffHand.AttributeType.SPEED_UP);
                        }
                     }

                     if (valx > 0.0 && valx < 4.0) {
                        speedSharx = true;
                     }
                  }

                  boolean slowing = Minecraft.player.isPotionActive(MobEffects.BLINDNESS) || Minecraft.player.isPotionActive(MobEffects.SLOWNESS);
                  float slowMul = slowing ? 0.8F : 1.0F;
                  this.useTimer = false;
                  this.ruleNoLiquid = true;
                  this.sleepIfFlag = Minecraft.player.ticksExisted > 105;
                  this.sleepTimeMS = 3050.0F;
                  this.pullDown = !slowing;
                  this.armorCheckAttributes = false;
                  this.ktGroundBoost = false;
                  this.moveSideRotate = false;
                  this.advancedSpeedSettings = true;
                  if (speedLvlx == 3 && speedSharx) {
                     this.mulMinBaseSpeed = slowMul;
                     this.mulEffectSpeedBoost = 1.4F;
                     this.mulAttribSpeedBoost = 10.5F;
                     this.jumpTickBoost = !slowing;
                     this.jumpBoostMul = 1.074F;
                     this.damageMotionBoost = false;
                     this.damageMotionMul = 1.0F;
                  } else if (speedLvlx == 2 && speedSharx) {
                     this.mulMinBaseSpeed = slowMul;
                     this.mulEffectSpeedBoost = 1.0F;
                     this.mulAttribSpeedBoost = 1.0F;
                     this.jumpTickBoost = !slowing;
                     this.jumpBoostMul = 1.05F;
                     this.damageMotionBoost = false;
                     this.damageMotionMul = 1.0F;
                  } else if (speedLvlx == 3) {
                     this.mulMinBaseSpeed = slowMul;
                     this.mulEffectSpeedBoost = 1.4F;
                     this.mulAttribSpeedBoost = 1.0F;
                     this.jumpTickBoost = !slowing;
                     this.jumpBoostMul = 1.074F;
                     this.damageMotionBoost = false;
                     this.damageMotionMul = 1.0F;
                  } else if (speedLvlx == 2) {
                     this.mulMinBaseSpeed = slowMul;
                     this.mulEffectSpeedBoost = 1.0F;
                     this.mulAttribSpeedBoost = 1.0F;
                     this.jumpTickBoost = !slowing;
                     this.jumpBoostMul = 1.05F;
                     this.damageMotionBoost = false;
                     this.damageMotionMul = 1.0F;
                  } else if (speedSharx) {
                     this.mulMinBaseSpeed = slowMul;
                     this.mulEffectSpeedBoost = 1.0F;
                     this.mulAttribSpeedBoost = 12.0F;
                     this.jumpTickBoost = !slowing;
                     this.jumpBoostMul = 1.074F;
                     this.damageMotionBoost = false;
                     this.damageMotionMul = 1.0F;
                  } else {
                     this.mulMinBaseSpeed = slowMul;
                     this.mulEffectSpeedBoost = 1.0F;
                     this.mulAttribSpeedBoost = 1.0F;
                     this.jumpTickBoost = !slowing;
                     this.jumpBoostMul = 1.074F;
                     this.damageMotionBoost = false;
                     this.damageMotionMul = 1.0F;
                  }
               } else {
                  this.isAutoAdvSettings = false;
               }
            }

            return true;
         }
      }

      return false;
   }

   private boolean isActiveAutoAdvSettings() {
      return this.isAutoAdvSettings;
   }

   private boolean MTIgetUseTimer() {
      return this.isActiveAutoAdvSettings() ? this.useTimer : this.UseTimer.getBool();
   }

   private boolean MTIgetRuleNoLiquid() {
      return this.isActiveAutoAdvSettings() ? this.ruleNoLiquid : this.RuleNoLiquid.getBool();
   }

   private boolean MTIgetSleepIfFlag() {
      return this.isActiveAutoAdvSettings() ? this.sleepIfFlag : this.SleepIfFlag.getBool();
   }

   private float MTIgetSleepTimeMS() {
      return this.isActiveAutoAdvSettings() ? this.sleepTimeMS : this.SleepTimeMS.getFloat();
   }

   private boolean MTIgetPullDown() {
      return this.isActiveAutoAdvSettings() ? this.pullDown : this.PullDown.getBool();
   }

   private boolean MTIgetArmorCheckAttributes() {
      return this.isActiveAutoAdvSettings() ? this.armorCheckAttributes : this.ArmorCheckAttributes.getBool();
   }

   private boolean MTIgetKTGroundBoost() {
      return this.isActiveAutoAdvSettings() ? this.ktGroundBoost : this.KTGroundBoost.getBool();
   }

   private boolean MTIgetMoveSideRotate() {
      return this.isActiveAutoAdvSettings() ? this.moveSideRotate : this.MoveSideRotate.getBool();
   }

   private boolean MTIgetAdvancedSpeedSettings() {
      return this.isActiveAutoAdvSettings() ? this.advancedSpeedSettings : this.AdvancedSpeedSettings.getBool();
   }

   private float MTIgetMulMinBaseSpeed() {
      return this.isActiveAutoAdvSettings() ? this.mulMinBaseSpeed : this.MulMinBaseSpeed.getFloat();
   }

   private float MTIgetMulEffectSpeedBoost() {
      return this.isActiveAutoAdvSettings() ? this.mulEffectSpeedBoost : this.MulEffectSpeedBoost.getFloat();
   }

   private float MTIgetMulAttribSpeedBoost() {
      return this.isActiveAutoAdvSettings() ? this.mulAttribSpeedBoost : this.MulAttribSpeedBoost.getFloat();
   }

   private boolean MTIgetJumpTickBoost() {
      return this.isActiveAutoAdvSettings() ? this.jumpTickBoost : this.JumpTickBoost.getBool();
   }

   private float MTIgetJumpBoostMul() {
      return this.isActiveAutoAdvSettings() ? this.jumpBoostMul : this.JumpBoostMul.getFloat();
   }

   private boolean MTIgetDamageMotionBoost() {
      return this.isActiveAutoAdvSettings() ? this.damageMotionBoost : this.DamageMotionBoost.getBool();
   }

   private float MTIgetDamageMotionMul() {
      return this.isActiveAutoAdvSettings() ? this.damageMotionMul : this.DamageMotionMul.getFloat();
   }

   private void forMetaStrafe(boolean onMove) {
      if ((!LongJump.get.isActived() || !LongJump.get.Type.getMode().equalsIgnoreCase("Glide"))
         && !Minecraft.player.capabilities.isFlying
         && !Minecraft.player.isElytraFlying()) {
         if (!onMove) {
            this.prevIsCollided = this.isCollided;
            this.isCollided = Minecraft.player.isCollidedHorizontally;
            if (this.isCollided) {
               this.ticksNoCollide = 0;
            } else {
               this.ticksNoCollide++;
            }

            if (this.sleepTicks > 0) {
               this.sleepTicks--;
            }

            if (this.pauseTicksOutEly > 0) {
               this.pauseTicksOutEly--;
            }

            if (Minecraft.player.getFlag(7)
               || this.MTIgetRuleNoLiquid()
                  && Minecraft.player.isInWater()
                  && (
                     JesusSpeed.getBlockWithExpand(0.29000002F, Minecraft.player.posX, Minecraft.player.posY + 0.9F, Minecraft.player.posZ, Blocks.WATER)
                        || JesusSpeed.getBlockWithExpand(
                           0.29000002F,
                           Minecraft.player.posX,
                           Minecraft.player.posY + (double)Minecraft.player.height - 0.01F,
                           Minecraft.player.posZ,
                           Blocks.WATER
                        )
                  )) {
               this.pauseTicksOutEly = 6;
            }
         } else if (this.MTIgetSleepIfFlag()) {
            this.sleep = !this.lastFlagTimer.hasReached((double)((int)this.MTIgetSleepTimeMS()));
         } else {
            this.sleep = false;
         }
      }
   }

   private void forAverage(boolean onMove) {
      if (!onMove) {
         boolean benchmarkAverageSpeed = false;
         double selfSpeedCalc = Minecraft.player.getSpeed();
         if (benchmarkAverageSpeed) {
            speedUpdated += selfSpeedCalc;
            if (Minecraft.player.ticksExisted % 100 == 0) {
               speedUpdated = 0.0;
            }
         } else {
            boolean calcTresHold = false;
            float maxOff = 0.9F;
            float thresholdMin = 0.5F;
            float thresholdMax = 0.25F;
            double avgSpeed = 0.35580945F;
            double baseSpeedMul = 1.0075F;
            double lrpTo1IfNegative = 0.0;
            if (Minecraft.player.isPotionActive(MobEffects.SPEED)) {
               switch (Minecraft.player.getActivePotionEffect(MobEffects.SPEED).getAmplifier()) {
                  case 0:
                     avgSpeed = 0.3676697081605209;
                     break;
                  case 1:
                     avgSpeed = 0.3804663432374285;
                  case 2:
               }
            }

            if (this.isActive45DegreeSpeed()) {
               avgSpeed *= 1.01942642739;
            }

            double timeMul = MathUtils.clamp(1.0 / (selfSpeedCalc / (avgSpeed * baseSpeedMul)), 0.100000024F, 1.9F);
            if (timeMul < 1.0 && lrpTo1IfNegative > 0.0) {
               timeMul = MathUtils.lerp(timeMul, 1.0, lrpTo1IfNegative);
            }

            if (timeMul >= 0.5 && timeMul < 1.25) {
               mc.timer.tempSpeed = timeMul;
            }
         }
      }
   }

   private void forMatrix7(boolean onMove) {
      if (!onMove) {
         boolean timerBoost = true;
         boolean timerBoostGround = timerBoost && Minecraft.player.onGround && Minecraft.player.isCollidedVertically && Minecraft.player.isJumping();
         boolean airBoost = true;
         boolean yPortJumping = false;
         boolean bHopYport = true;
         if (Minecraft.player.isJumping()) {
            if (airBoost
               && Minecraft.player.fallDistance == 0.0F
               && mc.world != null
               && mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.offsetMinDown(0.5)).isEmpty()
               && !mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.offsetMinDown(0.99999)).isEmpty()) {
               Minecraft.player.onGround = true;
               timerBoostGround = true;
               Minecraft.player.fallDistance = 0.0F;
            }

            if (timerBoostGround) {
               mc.timer.tempSpeed = 1.1F;
            } else if (timerBoost) {
               mc.timer.tempSpeed = 1.03F;
            }

            if (!mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.offsetMinDown(0.8)).isEmpty()
               && Minecraft.player.fallDistance > 0.5F) {
               if (yPortJumping) {
                  Entity.motiony = -1.0;
                  if (bHopYport) {
                     Minecraft.player.onGround = true;
                  }
               }

               Minecraft.player.fallDistance = 0.0F;
            }
         } else if (Minecraft.player.onGround && Minecraft.player.isCollidedVertically) {
            if (Minecraft.player.isSprinting()) {
               if (Minecraft.player.moveStrafing == 0.0F) {
                  Minecraft.player.multiplyMotionXZ(1.019F);
                  MoveMeHelp.setSpeed(MoveMeHelp.getSpeed());
               } else {
                  Minecraft.player.multiplyMotionXZ(1.003F);
                  MoveMeHelp.setSpeed(MoveMeHelp.getSpeed());
               }
            } else if (Minecraft.player.moveForward <= 0.0F) {
               if (Minecraft.player.moveStrafing == 0.0F) {
                  Minecraft.player.multiplyMotionXZ(1.206F);
                  MoveMeHelp.setSpeed(MoveMeHelp.getSpeed());
               } else {
                  Minecraft.player.multiplyMotionXZ(1.194F);
                  MoveMeHelp.setSpeed(MoveMeHelp.getSpeed());
               }
            }
         }
      }
   }

   private void forTest(boolean onMove) {
      if (!onMove) {
         Client.msg(Minecraft.player.motionY, true);
         if (Minecraft.player.motionY > 0.0) {
            Minecraft.player.motionY *= 1.026F;
            Minecraft.player.jumpMovementFactor *= 1.2F;
         } else {
            Minecraft.player.motionY *= 0.96F;
            if ((double)Minecraft.player.fallDistance > 1.1 && (double)Minecraft.player.fallDistance < 1.5) {
               Minecraft.player.jumpMovementFactor *= 7.0F;
            }
         }

         this.cancelStrafe = true;
      } else {
         MoveMeHelp.setCuttingSpeed(MoveMeHelp.getCuttingSpeed() / 1.06);
         MoveMeHelp.setSpeed(MoveMeHelp.getSpeed());
      }
   }

   private float getDegreeOffsetFromSide() {
      return (float)this.sideDegreeInt * 45.0F;
   }

   public boolean isActive45DegreeSpeed() {
      return this.isActived() && (this.AntiCheat.getMode().equalsIgnoreCase("Degree45") || this.Degree45Boost.getBool() && this.Degree45Boost.isVisible());
   }

   public boolean canRotate() {
      if (Minecraft.player == null) {
         return false;
      } else if (ThrowFollow.get.runTicks == 1 || Minecraft.player.isRiding()) {
         return false;
      } else if (MiddleClick.get.callThrowPearl
         || MiddleClick.get.callThrowPearl2
         || HitAura.get.canRotateUpdated
         || PotionThrower.get.callThrowPotions
         || PotionThrower.get.forceThrow) {
         return false;
      } else if (HitAura.get.canRotateUpdated) {
         return false;
      } else {
         ItemStack stackInHand = Minecraft.player.getHeldItemMainhand();
         if (stackInHand != null) {
            Item item = stackInHand.getItem();
            if (item instanceof ItemEnderPearl
               || item instanceof ItemBow
               || item instanceof ItemSplashPotion
               || item instanceof ItemLingeringPotion
               || item instanceof ItemFishingRod) {
               return false;
            }
         }

         if (get.isActive45DegreeSpeed()) {
            return false;
         } else {
            return MoveHelper.instance.isActived()
                  && MoveHelper.instance.NoSlowDown.getBool()
                  && MoveHelper.instance.NoSlowMode.getMode().equalsIgnoreCase("NCPNew")
               ? Minecraft.player.getActiveItemStack() == null || Minecraft.player.getActiveItemStack().getItem() != Items.SHIELD
               : true;
         }
      }
   }

   @EventTarget
   public void onMove(EventMove2 event) {
      if (this.isActived() && this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe") && Minecraft.player != null) {
         boolean canSpeed = (this.ticksNoCollide > 0 || event.isCollidedHorizontal())
            && (!this.MTIgetRuleNoLiquid() || !Minecraft.player.isInsideOfMaterial(Material.WATER) && !Minecraft.player.isInsideOfMaterial(Material.LAVA))
            && !this.sleep
            && this.sleepTicks <= 0
            && this.pauseTicksOutEly <= 0
            && !Minecraft.player.isElytraFlying()
            && !JesusSpeed.getBlockWithExpand(Minecraft.player.width / 2.0F, Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ, Blocks.WEB)
            && (
               !JesusSpeed.getBlockWithExpand(
                     Minecraft.player.width / 2.0F, Minecraft.player.posX, Minecraft.player.posY - 1.0, Minecraft.player.posZ, Blocks.SOUL_SAND
                  )
                  || MoveHelper.instance.isActived() && MoveHelper.instance.NoSlowSoul.getBool()
            )
            && (!Minecraft.player.isSneaking() || MoveHelper.instance.isActived() && MoveHelper.instance.NoSlowSneak.getBool());
         if (canSpeed) {
            boolean ktGroundBoost = this.MTIgetKTGroundBoost();
            boolean canAnyBoost = MoveMeHelp.w() && !MoveMeHelp.s();
            boolean jumpBoosting = canAnyBoost && this.MTIgetAdvancedSpeedSettings() && this.MTIgetJumpTickBoost();
            if (ktGroundBoost && !Minecraft.player.isJumping() && FriendsSLink.getPVPTimeSecInt() > 0) {
               float moveYawRad = MathHelper.toRadians(
                  TargetStrafe.isCallSpeedRotationMetaStrafe() ? TargetStrafe.callSpeedRotation : MoveMeHelp.moveYaw(Minecraft.player.rotationYaw)
               );
               double hRange = 11.0;
               double yDown = 0.2F;
               double setX = Minecraft.player.posX - (double)MathHelper.sin(moveYawRad) * hRange;
               double setY = (double)((int)(Minecraft.player.posY + 0.999F)) - yDown;
               double setZ = Minecraft.player.posZ + (double)MathHelper.cos(moveYawRad) * hRange;
               if (posBlock(setX, setY, setZ) && !posBlock(setX, setY + 0.25, setZ)) {
                  ClickTeleport.matrixTp(setX - Minecraft.player.posX, setY - Minecraft.player.posY, setZ - Minecraft.player.posZ, false);
               }

               if (posBlock(Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ)) {
                  Minecraft.player.motionY = 0.1;
               }

               speedUpdated = 0.0;
            } else {
               double mulAttrib = 1.0;
               boolean slowing = false;
               if (OffHand.get != null) {
                  OffHand.ItemStackWithSlot iswsGeted1 = new OffHand.ItemStackWithSlot(Minecraft.player.inventoryContainer.getSlot(45).getStack(), 45);
                  double val = OffHand.get.getAttributeValueFromAll(OffHand.get.getStackAttributesWithoutPotions(iswsGeted1), OffHand.AttributeType.SPEED_UP);
                  if (this.MTIgetArmorCheckAttributes()) {
                     for (int slot = 5; slot <= 8; slot++) {
                        iswsGeted1 = new OffHand.ItemStackWithSlot(Minecraft.player.inventoryContainer.getSlot(5).getStack(), 5);
                        val += OffHand.get.getAttributeValueFromAll(OffHand.get.getStackAttributesWithoutPotions(iswsGeted1), OffHand.AttributeType.SPEED_UP);
                     }
                  }

                  if (val < 0.0) {
                     slowing = true;
                  }

                  if (val < 4.0) {
                     mulAttrib = 1.0 + val;
                  }
               }

               double speedNUpdated = MoveMeHelp.getSpeed();
               double moveSpeed = speedNUpdated;
               int speedLVL = Minecraft.player.isPotionActive(MobEffects.SPEED) && Minecraft.player.getActivePotionEffect(MobEffects.SPEED).getDuration() > 20
                  ? Minecraft.player.getActivePotionEffect(MobEffects.SPEED).getAmplifier() + 1
                  : 0;
               int slowLVL = Minecraft.player.isPotionActive(MobEffects.SLOWNESS)
                  ? Minecraft.player.getActivePotionEffect(MobEffects.SLOWNESS).getAmplifier() + 1
                  : 0;
               boolean jes = !Minecraft.player.isInWater()
                  && mc.world.getBlockState(new BlockPos(Minecraft.player.posX, Minecraft.player.posY - 0.001, Minecraft.player.posZ)).getBlock()
                     == Blocks.WATER;
               boolean ground = Minecraft.player.onGround && !jes;
               float fallDistance = Minecraft.player.fallDistance;
               boolean hopped = Minecraft.player.isJumping() && ground && Minecraft.player.posY > Minecraft.player.lastTickPosY;
               boolean postWater = !Minecraft.player.isInWater()
                  && JesusSpeed.getBlockWithExpand(0.3F, Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ, Blocks.WATER)
                  && !JesusSpeed.getBlockWithExpand(0.3F, Minecraft.player.posX, Minecraft.player.posY + 0.02, Minecraft.player.posZ, Blocks.WATER)
                  && posBlock(Minecraft.player.posX, Minecraft.player.posY - 1.0, Minecraft.player.posZ);
               boolean preWaterGround = !jes
                  && JesusSpeed.getBlockWithExpand(0.3F, Minecraft.player.posX, Minecraft.player.posY + 0.06, Minecraft.player.posZ, Blocks.WATER)
                  && JesusSpeed.getBlockWithExpand(0.3F, Minecraft.player.posX, Minecraft.player.posY + 0.9, Minecraft.player.posZ, Blocks.AIR);
               float advMulMinSpeed = this.MTIgetAdvancedSpeedSettings() ? this.MTIgetMulMinBaseSpeed() : 1.0F;
               float advMulSpeedLVLSpeed = this.MTIgetAdvancedSpeedSettings() ? this.MTIgetMulEffectSpeedBoost() : 1.0F;
               float advMulAttributeBoostSpeed = this.MTIgetAdvancedSpeedSettings() ? this.MTIgetMulAttribSpeedBoost() : 1.0F;
               float advJumpBoostMul = this.MTIgetAdvancedSpeedSettings() && this.MTIgetJumpTickBoost() ? this.MTIgetJumpBoostMul() : 1.5F;
               double mulSpeedOfYawChange = 1.0;
               double damageSpeedMul = this.MTIgetDamageMotionBoost() && EntityLivingBase.isNcpDamaged ? (double)this.MTIgetDamageMotionMul() : 1.0;
               boolean sprinting = Minecraft.player.isSprinting();
               double minSpeed = (
                     0.3909 * (double)advMulMinSpeed
                        + (slowing ? 0.0 : (double)((float)speedLVL * advMulSpeedLVLSpeed * (!sprinting && !ground ? 0.0F : 0.07799F)))
                  )
                  * (
                     preWaterGround
                        ? 0.8
                        : (
                           postWater
                              ? ((mulAttrib - 1.0) * (double)advMulAttributeBoostSpeed + 1.0) * 0.95
                              : ((mulAttrib - 1.0) * (double)advMulAttributeBoostSpeed + 1.0)
                                 * 0.996F
                                 * (double)(1.0F - (float)speedLVL * 0.04F)
                                 * mulSpeedOfYawChange
                        )
                  )
                  * damageSpeedMul;
               double mulSpeed = 1.0;
               float jumpBoostMultiplier = (speedLVL <= 0 && !(mulAttrib > 0.0) ? 1.0F : 2.0F) * advJumpBoostMul;
               float jumpBoostAdding = 0.0F;
               if (slowing && mulAttrib < 1.0) {
                  mulSpeed -= 1.0 - Math.abs(mulAttrib);
                  speedNUpdated *= 1.0 - Math.abs(mulAttrib);
               }

               if (jes) {
                  mulSpeed *= 0.93;
               }

               if (canAnyBoost && hopped && !this.preGround && sprinting && jumpBoosting) {
                  minSpeed *= (double)jumpBoostMultiplier;
                  minSpeed += (double)jumpBoostAdding;
               }

               double currentSetSpeed = speedNUpdated * mulSpeed;
               if (slowLVL > 0) {
                  minSpeed = 0.0;
               }

               if (currentSetSpeed < minSpeed * mulSpeed) {
                  currentSetSpeed = minSpeed * mulSpeed;
               } else if (preWaterGround) {
                  currentSetSpeed *= 0.79;
               }

               if (!MoveMeHelp.isMoving() && !TargetStrafe.isCallSpeedRotationMetaStrafe()) {
                  speedUpdated = 0.0;
               } else {
                  speedUpdated = currentSetSpeed;
                  if (TargetStrafe.goStrafe() && TargetStrafe.get.SpeedLimitToDistance.getBool() && TargetStrafe.target != null) {
                     Vec3d virtPos = TargetStrafe.get.getEntityVirtPos(TargetStrafe.target, TargetStrafe.get.TargetBoxSync.getBool());
                     double dst = (double)Minecraft.player
                        .getSmoothDistanceToCoordXZ((float)virtPos.xCoord, (float)Minecraft.player.posY, (float)virtPos.zCoord);
                     if (speedUpdated >= dst * 0.6F) {
                        speedUpdated = Math.min(speedUpdated, dst * 0.6F);
                     }
                  }
               }

               float deltaDelThreshold = 2.0F;
               if (speedLVL > 0) {
                  deltaDelThreshold -= Math.min((float)speedLVL * (float)mulAttrib / 5.0F, 0.8F);
               }

               if (mulAttrib - 1.0 > 0.0) {
                  deltaDelThreshold -= Math.min((float)(mulAttrib - 1.0), deltaDelThreshold);
               }

               double minMotion = speedUpdated / (double)(1.0F + deltaDelThreshold);
               float moveYawFinal = MoveMeHelp.moveYaw(Minecraft.player.rotationYaw);
               boolean hasPingEvents = false;
               if (this.lastSlicedYaw != 0.0F && this.canRotate()) {
                  if (this.MTIgetMoveSideRotate()) {
                     float difference = RotationUtil.getAngleDifference(this.lastSlicedYaw, moveYawFinal);
                     hasPingEvents = difference == -90.0F || difference == 90.0F || difference == 180.0F || difference == 45.0F;
                  }

                  if (hasPingEvents) {
                     speedUpdated = 0.0;
                  }

                  moveYawFinal = this.MTIgetMoveSideRotate() ? this.lastSlicedYaw : MoveMeHelp.moveYaw(this.lastSlicedYaw);
               }

               if (TargetStrafe.isCallSpeedRotationMetaStrafe()) {
                  moveYawFinal = TargetStrafe.callSpeedRotation;
               }

               double speedMoveFinal = speedUpdated;
               boolean doSetSpeedFinal = speedUpdated != 0.0 && !hasPingEvents && (MoveMeHelp.isMoving() || TargetStrafe.isCallSpeedRotationMetaStrafe());
               boolean doSetMotionFinal = speedUpdated != 0.0 && (MoveMeHelp.isMoving() || TargetStrafe.isCallSpeedRotationMetaStrafe());
               if (doSetSpeedFinal) {
                  if (!Minecraft.player.isCollidedHorizontally && !MoveMeHelp.isBlockAboveHead() && slowLVL == 0 && this.MTIgetPullDown()) {
                     Minecraft.player.motionY -= 0.004999F;
                  }

                  if (this.MTIgetUseTimer() && Timer.percent >= (double)(Timer.get.BoundUp.getFloat() + 0.05F) && !Timer.get.isActived()) {
                     mc.timer.tempSpeed = 1.003F;
                     Timer.forceTimer(1.003F);
                  }

                  if (!canAnyBoost || !jumpBoosting || moveSpeed < speedMoveFinal) {
                     event.motion().xCoord = (double)(-((float)Math.sin(Math.toRadians((double)moveYawFinal)))) * speedMoveFinal;
                     event.motion().zCoord = (double)((float)Math.cos(Math.toRadians((double)moveYawFinal))) * speedMoveFinal;
                  } else if (hopped && jumpBoosting) {
                     Minecraft.player.motionX = (double)(-((float)Math.sin(Math.toRadians((double)moveYawFinal)))) * speedMoveFinal;
                     Minecraft.player.motionZ = (double)((float)Math.cos(Math.toRadians((double)moveYawFinal))) * speedMoveFinal;
                  } else {
                     speedMoveFinal = MoveMeHelp.getSpeed();
                     if (speedMoveFinal >= speedMoveFinal - 0.1F) {
                        Minecraft.player.motionX = (double)(-MathHelper.sin(MathHelper.toRadians(moveYawFinal))) * speedMoveFinal;
                        Minecraft.player.motionZ = (double)MathHelper.cos(MathHelper.toRadians(moveYawFinal)) * speedMoveFinal;
                     } else {
                        event.motion().xCoord = (double)(-((float)Math.sin(Math.toRadians((double)moveYawFinal)))) * speedMoveFinal;
                        event.motion().zCoord = (double)((float)Math.cos(Math.toRadians((double)moveYawFinal))) * speedMoveFinal;
                     }
                  }
               }

               if (doSetMotionFinal && MoveMeHelp.getSpeed() < minMotion) {
                  Minecraft.player.motionX = (double)(-((float)Math.sin(Math.toRadians((double)moveYawFinal)))) * minMotion;
                  Minecraft.player.motionZ = (double)((float)Math.cos(Math.toRadians((double)moveYawFinal))) * minMotion;
               }

               this.preGround = ground;
               this.preFallDistance = fallDistance;
            }
         }
      }
   }

   @EventTarget
   public void onSilentSideStrafe(EventRotationStrafe event) {
      if (this.actived && this.isActive45DegreeSpeed()) {
         event.setYaw(MoveMeHelp.moveYaw(Minecraft.player.rotationYaw) - (this.degreeOffset ? this.getDegreeOffsetFromSide() : 0.0F));
      }
   }

   @EventTarget
   public void onMoveKeysPress(EventMoveKeys event) {
      if (this.actived && this.isActive45DegreeSpeed()) {
         if ((event.isForwardKeyDown() || event.isBackKeyDown()) && event.isForwardKeyDown() != event.isBackKeyDown()
            || (event.isRightKeyDown() || event.isLeftKeyDown()) && event.isRightKeyDown() != event.isLeftKeyDown()) {
            event.setForwardKeyDown(true);
            event.setBackKeyDown(false);
            event.setLeftKeyDown(this.degreeOffset && this.sideDegreeInt == -1);
            event.setRightKeyDown(this.degreeOffset && this.sideDegreeInt == 1);
         }
      } else if (this.actived && TargetStrafe.goStrafe() && TargetStrafe.isCallSpeedRotationMetaStrafe() && !this.sleep) {
         event.setForwardKeyDown(true);
         event.setBackKeyDown(false);
         event.setLeftKeyDown(false);
         event.setRightKeyDown(false);
      }
   }

   @EventTarget
   public void onSilentStrafe(EventRotationStrafe event) {
      if (this.actived && this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe") && TargetStrafe.isCallSpeedRotationMetaStrafe() && !this.sleep) {
         event.setYaw(TargetStrafe.callSpeedRotation);
      }
   }

   @EventTarget
   public void onSilentSideJump(EventRotationJump event) {
      if (this.actived && this.isActive45DegreeSpeed() && !this.sleep) {
         event.setYaw(MoveMeHelp.moveYaw(Minecraft.player.rotationYaw));
      }
   }

   @EventTarget
   public void onReceive(EventReceivePacket event) {
      if (this.actived
         && event.getPacket() instanceof SPacketPlayerPosLook look
         && this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe")
         && this.MTIgetSleepIfFlag()
         && Minecraft.player != null
         && MoveMeHelp.isMoving()) {
         double dst = Minecraft.player.getDistance(look.getX(), look.getY(), look.getZ());
         double dstXZ = Minecraft.player.getDistanceXZ(look.getX(), look.getZ());
         if (dstXZ <= speedUpdated * 2.0 + 0.1F && dstXZ > speedUpdated / 1.1F && dst <= speedUpdated + 1.4F) {
            this.lastFlagTimer.reset();
            this.sleep = true;
         } else {
            this.sleepTicks = 5;
         }
      }
   }

   @EventTarget
   public void onPlayerUpdate(EventPlayerMotionUpdate event) {
      if (this.actived && this.isActive45DegreeSpeed()) {
         float yaw = MoveMeHelp.moveYaw(Minecraft.player.rotationYaw) + (this.degreeOffset ? this.getDegreeOffsetFromSide() : 0.0F);
         event.setYaw(yaw);
         Minecraft.player.rotationYawHead = yaw;
         Minecraft.player.renderYawOffset = yaw;
         if (MoveMeHelp.isMoving()) {
            if (Minecraft.player.onGround && !Minecraft.player.isJumping() && Minecraft.player.getSpeed() > 0.2F) {
               this.degreeOffset = true;
               this.sideDegreeInt = Minecraft.player.ticksExisted % 6 < 3 ? -1 : 1;
            } else if (Minecraft.player.isJumping() && Minecraft.player.getSpeed() > 0.1F) {
               this.degreeOffset = !Minecraft.player.onGround;
               if (!this.degreeOffset) {
                  this.sideDegreeInt = this.sideDegreeInt == 1 ? -1 : 1;
               }
            } else {
               this.degreeOffset = false;
            }
         } else {
            this.degreeOffset = false;
         }
      } else if (this.actived
         && this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe")
         && this.MTIgetMoveSideRotate()
         && this.canRotate()
         && MoveMeHelp.isMoving()
         && (!TargetStrafe.goStrafe() || TargetStrafe.isCallSpeedRotationMetaStrafe())
         && !this.sleep) {
         float yaw = TargetStrafe.isCallSpeedRotationMetaStrafe() ? TargetStrafe.callSpeedRotation : MoveMeHelp.moveYaw(Minecraft.player.rotationYaw);
         event.setYaw(yaw);
         Minecraft.player.rotationYawHead = yaw;
         Minecraft.player.renderYawOffset = yaw;
         this.lastSlicedYaw = yaw;
      } else {
         this.lastSlicedYaw = Minecraft.player != null ? Minecraft.player.rotationYaw : 0.0F;
      }
   }

   @EventTarget
   public void onSprintBlock(EventSprintBlock event) {
      if (this.isActived()
         && this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe")
         && (MoveMeHelp.isMoving() || TargetStrafe.goStrafe())
         && (this.MTIgetMoveSideRotate() && this.canRotate() || !this.MTIgetMoveSideRotate())
         && !this.sleep) {
         event.cancel();
      }
   }

   @EventTarget
   public void onJumpBoost(EventRotationJump event) {
      if (this.isActived() && this.AntiCheat.getMode().equalsIgnoreCase("MetaStrafe") && (MoveMeHelp.isMoving() || TargetStrafe.goStrafe()) && !this.sleep) {
         event.setYaw(
            TargetStrafe.goStrafe() && TargetStrafe.isCallSpeedRotationMetaStrafe()
               ? TargetStrafe.callSpeedRotation
               : MoveMeHelp.moveYaw(Minecraft.player.rotationYaw)
         );
      }
   }
}
