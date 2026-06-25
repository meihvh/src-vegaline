package ru.govno.client.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import javax.vecmath.Vector2f;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketCreativeInventoryAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.potion.Potion;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.clickgui.ClickGuiScreen;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventMovementInput;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventRotationJump;
import ru.govno.client.event.events.EventRotationStrafe;
import ru.govno.client.module.Module;
import ru.govno.client.module.modules.AntiCrystal;
import ru.govno.client.module.modules.ClientColors;
import ru.govno.client.module.modules.Fly;
import ru.govno.client.module.modules.FreeCam;
import ru.govno.client.module.modules.HitAura;
import ru.govno.client.module.modules.OffHand;
import ru.govno.client.module.modules.PlayerHelper;
import ru.govno.client.module.modules.TPInfluence;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.BlockUtils;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Movement.MoveMeHelp;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Versions.NewPhisicsFixes;

public class CrystalField extends Module {
   public static CrystalField get;
   public ModeSettings ProcessRateLevel;
   public ModeSettings CrystalHand;
   public ModeSettings ObsidianHand;
   public ModeSettings Swing;
   public ModeSettings SpamRule;
   public FloatSettings OnHealth;
   public FloatSettings MaxTargetsCount;
   public BoolSettings Rotations;
   public BoolSettings RotateMoveSide;
   public BoolSettings PlaceObsidian;
   public BoolSettings PlaceIgnoreWalls;
   public BoolSettings UseInventory;
   public BoolSettings NoSuicide;
   public BoolSettings FirstPosSpammer;
   public BoolSettings HitAuraPause;
   public BoolSettings CPSBypass;
   public BoolSettings CheckCrystalOwner;
   public BoolSettings ReplaceCrystalOnHit;
   public BoolSettings SyncPacketMineAura;
   public BoolSettings AdobeHeadTrapBomb;
   public BoolSettings SetSurroundToTarget;
   public int sleepUpdatePlaceObsAdobeHeadTicks;
   public int sleepUpdatePlaceCrysAdobeHeadTicks;
   public int sleepUpdateBreakCrysAdobeHeadTicks;
   private final ru.govno.client.utils.Math.TimerHelper placeObsDelay = new ru.govno.client.utils.Math.TimerHelper();
   private final ru.govno.client.utils.Math.TimerHelper placeCrysDelay = new ru.govno.client.utils.Math.TimerHelper();
   private final ru.govno.client.utils.Math.TimerHelper attackDelay = new ru.govno.client.utils.Math.TimerHelper();
   private final ru.govno.client.utils.Math.TimerHelper tpaDelay = new ru.govno.client.utils.Math.TimerHelper();
   final List<EntityLivingBase> notCurrents = new CopyOnWriteArrayList<>();
   final List<BlockPos> positionsCrys = new ArrayList<>();
   final List<BlockPos> positionsObs = new ArrayList<>();
   public static BlockPos forCrystalPos = null;
   public static BlockPos forObsidianPos = null;
   private final List<BlockPos> sphere = new ArrayList<>();
   public static EntityEnderCrystal crystal = null;
   public static List<EntityLivingBase> targetezs = new ArrayList<>();
   final List<EntityEnderCrystal> crystals = new CopyOnWriteArrayList<>();
   private static boolean hasActionToCPSHint = false;
   private float cpsDelta = 0.0F;
   private int ticks;
   private boolean skipTicks = false;
   private boolean callRotateUpOnHitCrystal;
   private final ArrayList<CrystalField.PopEffect> hitPops = new ArrayList<>();
   AnimationUtils pointEffaPC = new AnimationUtils(0.0F, 0.0F, 0.05F);
   AnimationUtils pointXSmooth = new AnimationUtils(0.0F, 0.0F, 0.2F);
   AnimationUtils pointYSmooth = new AnimationUtils(0.0F, 0.0F, 0.2F);
   AnimationUtils pointZSmooth = new AnimationUtils(0.0F, 0.0F, 0.2F);
   Vec3d lastRotatedVec;
   Vec3d lastRotatedVecNotNulled;
   float callYawMoveYaw = -1.2345679E8F;
   private final List<CrystalField.TickCachedBlockPos> CRYSTAL_PLACE_CACHE = new ArrayList<>();
   private final List<BlockPos> positionsMining = new ArrayList<>();
   private BlockPos forMiningPos = null;
   private boolean hasMiningTempCheck;

   public CrystalField() {
      super("CrystalField", 0, Module.Category.COMBAT);
      this.settings
         .add(this.ProcessRateLevel = new ModeSettings("ProcessRateLevel", "Normal", this, new String[]{"Low", "Normal", "High", "Powerful", "Impossible"}));
      this.settings.add(this.Rotations = new BoolSettings("Rotations", false, this));
      this.settings.add(this.RotateMoveSide = new BoolSettings("RotateMoveSide", false, this, () -> this.Rotations.getBool()));
      this.settings.add(this.CrystalHand = new ModeSettings("CrystalHand", "Auto", this, new String[]{"OffHand", "MainHand", "Auto"}));
      this.settings.add(this.PlaceObsidian = new BoolSettings("PlaceObsidian", true, this));
      this.settings
         .add(
            this.ObsidianHand = new ModeSettings(
               "ObsidianHand", "MainHand", this, new String[]{"OffHand", "MainHand", "Auto"}, () -> this.PlaceObsidian.getBool()
            )
         );
      this.settings.add(this.Swing = new ModeSettings("Swing", "Packet", this, new String[]{"None", "Packet", "Client"}));
      this.settings.add(this.PlaceIgnoreWalls = new BoolSettings("PlaceIgnoreWalls", true, this, () -> this.PlaceObsidian.getBool()));
      this.settings
         .add(
            this.UseInventory = new BoolSettings(
               "UseInventory",
               true,
               this,
               () -> !this.CrystalHand.currentMode.equalsIgnoreCase("OffHand")
                     || this.PlaceObsidian.getBool() && !this.ObsidianHand.currentMode.equalsIgnoreCase("OffHand")
            )
         );
      this.settings.add(this.NoSuicide = new BoolSettings("NoSuicide", true, this));
      this.settings.add(this.FirstPosSpammer = new BoolSettings("FirstPosSpammer", true, this));
      this.settings
         .add(this.SpamRule = new ModeSettings("SpamRule", "Always", this, new String[]{"Never", "Always", "LowHP", "LowArm", "LowHpOrArm", "WhileSneak"}));
      this.settings.add(this.OnHealth = new FloatSettings("OnHealth", 10.0F, 20.0F, 1.0F, this, () -> this.SpamRule.currentMode.toLowerCase().contains("hp")));
      this.settings.add(this.MaxTargetsCount = new FloatSettings("MaxTargetsCount", 3.0F, 6.0F, 1.0F, this));
      this.settings.add(this.HitAuraPause = new BoolSettings("AuraPause", true, this));
      this.settings.add(this.CPSBypass = new BoolSettings("CPSBypass", false, this));
      this.settings.add(this.CheckCrystalOwner = new BoolSettings("CheckCrystalOwner", false, this));
      this.settings.add(this.ReplaceCrystalOnHit = new BoolSettings("ReplaceCrystalOnHit", false, this));
      this.settings.add(this.SyncPacketMineAura = new BoolSettings("SyncPacketMineAura", false, this));
      this.settings
         .add(
            this.AdobeHeadTrapBomb = new BoolSettings("AdobeHeadTrapBomb", false, this, () -> this.PlaceObsidian.getBool() && this.SyncPacketMineAura.getBool())
         );
      this.settings
         .add(
            this.SetSurroundToTarget = new BoolSettings(
               "SetSurroundToTarget", false, this, () -> this.PlaceObsidian.getBool() && this.SyncPacketMineAura.getBool() && this.AdobeHeadTrapBomb.getBool()
            )
         );
      this.setDemand(2, 3);
      get = this;
   }

   private boolean isSyncedWithPacketMine() {
      return this.SyncPacketMineAura.getBool()
         && PlayerHelper.get.isActived()
         && PlayerHelper.get.SpeedMine.getBool()
         && PlayerHelper.get.MineMode.getMode().equalsIgnoreCase("Packet");
   }

   private boolean isAdobeHeadTrap() {
      return this.AdobeHeadTrapBomb.getBool() && this.PlaceObsidian.getBool() && this.SyncPacketMineAura.getBool();
   }

   private boolean isSetSurroundToTargetPredictedMove() {
      return this.PlaceObsidian.getBool() && this.isSyncedWithPacketMine() && this.AdobeHeadTrapBomb.getBool() && this.SetSurroundToTarget.getBool();
   }

   public boolean getIsAuraPause() {
      return this.isActived()
         && this.HitAuraPause.getBool()
         && get != null
         && !get.getTargets().isEmpty()
         && (crystal != null || forCrystalPos != null || forObsidianPos != null || this.forMiningPos != null);
   }

   private float[] getDelaysAsProcessRateLevel(boolean isNewVersion) {
      float[] delays = new float[2];
      String var3 = this.ProcessRateLevel.currentMode;
      switch (var3) {
         case "Low":
            delays[0] = 245.0F;
            delays[1] = 245.0F;
            break;
         case "Normal":
            delays[0] = 145.0F;
            delays[1] = 145.0F;
            break;
         case "High":
            delays[0] = 45.0F;
            delays[1] = 95.0F;
            break;
         case "Powerful":
            delays[0] = 25.0F;
            delays[1] = 25.0F;
            break;
         case "Impossible":
            delays[0] = 5.0F;
            delays[1] = 15.0F;
      }

      return delays;
   }

   private int getPosChangeDelayAsProcessRateLevel() {
      String var2 = this.ProcessRateLevel.currentMode;

      return switch (var2) {
         case "Low" -> 4;
         case "Normal" -> 2;
         case "High", "Powerful", "Impossible" -> 1;
         default -> 2;
      };
   }

   private float getTPDelayAsProcessRateLevel() {
      String var2 = this.ProcessRateLevel.currentMode;

      return switch (var2) {
         case "Low" -> 800.0F;
         case "Normal" -> 400.0F;
         case "High" -> 100.0F;
         case "Powerful" -> 50.0F;
         case "Impossible" -> 20.0F;
         default -> 400.0F;
      };
   }

   private String getAuraUpdaterAsProcessRateLevel() {
      String var2 = this.ProcessRateLevel.currentMode;

      return switch (var2) {
         case "Low", "Normal" -> "Default";
         case "High", "Powerful" -> "FpsThread";
         case "Impossible" -> "OverrideAll";
         default -> "Default";
      };
   }

   private float[] getAuraRanges() {
      return new float[]{5.0F, 7.2F};
   }

   private float getTpRange() {
      return Fly.get.isActived() ? 60.0F : (Minecraft.player != null && Minecraft.player.onGround ? 30.0F : 45.0F);
   }

   private boolean canCrystalSetDeadAsProcessRateLevel() {
      return this.is1l13lPlusVersion()
         && (this.ProcessRateLevel.currentMode.equalsIgnoreCase("Powerful") || this.ProcessRateLevel.currentMode.equalsIgnoreCase("Impossible"));
   }

   private boolean isTPA(BlockPos pos) {
      return TPInfluence.get.forCrystalFieldRule() && (pos == null || pos != null && this.getMe().getDistanceToBlockPos(pos) > 5.0);
   }

   private void setTeleportForActs(Runnable actions, Vec3d toPos, boolean usingThis) {
      if (!usingThis) {
         actions.run();
      } else {
         double xDiff = this.getMe().posX - toPos.xCoord;
         double yDiff = this.getMe().posY - toPos.yCoord;
         double zDiff = this.getMe().posZ - toPos.zCoord;
         float dst = (float)Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
         int step = (int)(dst / 8.67F) + 1;
         boolean gr = this.getMe().onGround;

         for (int crate = gr ? -1 : 0; crate < step; crate++) {
            Minecraft.player.connection.preSendPacket(new CPacketPlayer(false));
         }

         Minecraft.player.connection.preSendPacket(new CPacketPlayer.Position(toPos.xCoord, toPos.yCoord, toPos.zCoord, false));
         Vec3d pos = Minecraft.player.getPositionVector();
         Minecraft.player.setPosition(toPos.xCoord, toPos.yCoord, toPos.zCoord);
         this.updatePointTP(toPos, true);
         actions.run();
         Minecraft.player.setPosition(pos.xCoord, pos.yCoord, pos.zCoord);
         Minecraft.player.connection.preSendPacket(new CPacketPlayer.Position(this.getMe().posX, this.getMe().posY + 0.42, this.getMe().posZ, false));
      }
   }

   private void setTeleportForActs(Runnable actions, BlockPos toPos, boolean usingThis) {
      Vec3d toPosFORCE = new Vec3d(toPos).addVector(0.5, 0.0, 0.5);
      this.setTeleportForActs(actions, toPosFORCE, usingThis);
   }

   private BlockPos bestCurrentablePosStandOpacityNeareble(BlockPos pos, int r) {
      if (pos == null) {
         pos = BlockPos.ORIGIN;
      }

      List<BlockPos> curs = new ArrayList<>();

      for (int x = pos.getX() - r; x < pos.getX() + r; x++) {
         for (int y = pos.getY(); y > pos.getY() - r; y--) {
            for (int z = pos.getZ() - r; z < pos.getZ() + r; z++) {
               BlockPos adds = new BlockPos(x, y, z);
               if (adds.getDistanceToBlockPos(pos) < (double)r
                  && mc.world.isAirBlock(adds)
                  && mc.world.isAirBlock(adds.up())
                  && (adds.getX() != pos.getX() || adds.getZ() != pos.getZ())) {
                  curs.add(adds);
               }
            }
         }
      }

      BlockPos pos2 = pos;
      if (curs != null && curs.size() > 1) {
         curs.sort(Comparator.comparing(POS -> POS.getDistanceToBlockPos(pos2)));
      }

      return curs != null && curs.size() != 0 && curs.get(0) != null ? curs.get(0) : pos;
   }

   private Vec3d vecOfPos(BlockPos pos) {
      return new Vec3d((double)((float)pos.getX() + 0.5F), (double)pos.getY(), (double)((float)pos.getZ() + 0.5F));
   }

   private boolean switchIsSilent() {
      return true;
   }

   private float getStackDurPC(ItemStack stackIn) {
      return stackIn.isItemDamaged() ? 1.0F - MathUtils.clamp((float)stackIn.getItemDamage() / (float)stackIn.getMaxDamage(), 0.0F, 1.0F) : 0.0F;
   }

   private float[] getLivingArmorPC(EntityLivingBase baseIn) {
      float armPC = 0.0F;
      float armMax = 4.0F;
      int armC = 0;
      Iterable<ItemStack> stacks = baseIn.getArmorInventoryList();
      if (stacks != null) {
         for (ItemStack stack : stacks) {
            Item itemInStack = stack.getItem();
            if (itemInStack instanceof ItemArmor) {
               armPC += this.getStackDurPC(stack);
               armC++;
            }
         }
      }

      return new float[]{armPC * 4.0F / armMax, (float)armC};
   }

   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private boolean spammingCrystals(EntityLivingBase target, boolean l1l13lplus, boolean forPlaceObsCheck) {
      String var5 = this.SpamRule.currentMode;

      return switch (var5) {
         case "Always" -> true;
         case "LowHP" -> target.getHealth() <= this.OnHealth.getFloat();
         case "LowArm" -> {
            float[] armorPCS = this.getLivingArmorPC(target);
            yield armorPCS[0] < 2.25F || armorPCS[1] < 4.0F;
         }
         case "LowHpOrArm" -> {
            yield target.getHealth() <= this.OnHealth.getFloat();
//            <unknown> var4;
//            if (!var4_1) {
//               float[] armorPCS = this.getLivingArmorPC(target);
//               yield armorPCS[0] < 2.25F || armorPCS[1] < 4.0F;
//            }
         }
         case "WhileSneak" -> Minecraft.player.isSneaking() && mc.gameSettings.keyBindSneak.isKeyDown();
         default -> false;
      } && (forPlaceObsCheck || !BlockUtils.canAttackFeetEntity(target, l1l13lplus));
   }

   private boolean is1l13lPlusVersion() {
      return NewPhisicsFixes.isNewVersion();
   }

   private List<EntityLivingBase> doNotBlowUpEnts(float[] ranges) {
      this.notCurrents.clear();
      Iterator<Entity> entities = mc.world.getLoadedEntityList().iterator();
      EntityPlayer self = this.getMe();

      while (entities.hasNext()) {
         Entity entity = entities.next();
         if (entity != null && entity instanceof EntityLivingBase) {
            EntityLivingBase base = (EntityLivingBase)entity;
            if ((double)self.getDistanceToEntity(base) < (double)(ranges[0] + ranges[1]) + 3.1) {
               String name = base.getName();
               if (Client.friendManager.isFriend(name) || base.equals(self)) {
                  this.notCurrents.add(base);
               }
            }
         }
      }

      return this.notCurrents;
   }

   private boolean posIsAcceptable(BlockPos pos, EntityLivingBase target, List<EntityLivingBase> friendsOrSelf, boolean headPlace, boolean isObsPlaceCheck) {
      boolean blowAnyTarget = false;
      boolean blowAnyFriendOrSelf = false;
      if ((!(target instanceof EntityPlayer player) || !player.isCreative() && !player.isSpectator())
         && (
            headPlace && BlockUtils.canPosBeSeenEntity(pos.up(), target, BlockUtils.bodyElement.CHEST, true)
               || BlockUtils.canPosBeSeenEntity(pos.up(), target, BlockUtils.bodyElement.LEGS, true)
         )) {
         blowAnyTarget = true;
         if (blowAnyTarget) {
            for (EntityLivingBase bases : friendsOrSelf) {
               if (!isObsPlaceCheck || pos.getY() < BlockUtils.getEntityBlockPos(bases).getY()) {
                  if (bases instanceof EntityPlayer) {
                     EntityPlayer playerx = (EntityPlayer)bases;
                     if (playerx.isCreative() || playerx.isSpectator()) {
                        continue;
                     }
                  }

                  if (BlockUtils.canPosBeSeenEntity(pos.up(), bases, BlockUtils.bodyElement.FEET, true)) {
                     blowAnyFriendOrSelf = true;
                  }
               }
            }
         }
      }

      return blowAnyTarget && !blowAnyFriendOrSelf;
   }

   private boolean isFatalPosition(BlockPos pos, Entity entityFor) {
      BlockPos entityPos = BlockUtils.getEntityBlockPos(entityFor);
      boolean any = false;
      if (entityPos != null && pos != null) {
         for (int y = entityPos.getY() - 4; y < entityPos.getY() - 1; y++) {
            if (MathUtils.getDifferenceOf(pos.getX(), entityPos.getX()) < 2 || MathUtils.getDifferenceOf(pos.getZ(), entityPos.getZ()) < 2) {
               any = true;
            }
         }
      }

      return any;
   }

   private BlockPos posCrystal(List<BlockPos> sphere, float[] ranges, List<EntityLivingBase> targets, boolean isAdobeHeadTrap) {
      this.positionsCrys.clear();
      boolean is1l13lPlusVersion = this.is1l13lPlusVersion();
      boolean nofirstPosSpammer = !this.FirstPosSpammer.getBool();
      List<EntityLivingBase> doNotBlowUpEnts = this.doNotBlowUpEnts(ranges);
      EntityPlayer self = this.getMe();

      for (EntityLivingBase target : targets) {
         if (target != null) {
            for (EnumFacing hFace : EnumFacing.HORIZONTALS) {
               BlockPos offsetToPlace = BlockUtils.getEntityBlockPos(target).offset(hFace).down();
               if (BlockUtils.canPlaceCrystal(offsetToPlace, is1l13lPlusVersion)) {
                  BlockPos offsetFeetPos = offsetToPlace.up();
                  if (BlockUtils.isOccupiedByEnt(offsetFeetPos, !nofirstPosSpammer)
                     && (is1l13lPlusVersion || !BlockUtils.isOccupiedByEnt(offsetFeetPos.up(), nofirstPosSpammer))
                     && this.posIsAcceptable(offsetToPlace, target, doNotBlowUpEnts, false, false)) {
                     this.positionsCrys.add(offsetToPlace);
                  }
               }
            }
         }
      }

      if (this.positionsCrys.isEmpty()) {
         for (BlockPos pos : sphere) {
            if (BlockUtils.canPlaceCrystal(pos, is1l13lPlusVersion)
               && !BlockUtils.isOccupiedByEnt(pos.up(), nofirstPosSpammer)
               && (is1l13lPlusVersion || !BlockUtils.isOccupiedByEnt(pos.up(2), nofirstPosSpammer))) {
               for (EntityLivingBase targetx : targets) {
                  if (!(BlockUtils.getDistanceAtPosToVec(pos, BlockUtils.getEntityVec3dPos(targetx)) > 7.2)
                     && this.posIsAcceptable(pos, targetx, doNotBlowUpEnts, false, false)) {
                     this.positionsCrys.add(pos);
                  }
               }
            }
         }
      }

      if (this.positionsCrys.isEmpty()) {
         for (BlockPos posx : sphere) {
            if (BlockUtils.canPlaceCrystal(posx, is1l13lPlusVersion)
               && !BlockUtils.isOccupiedByEnt(posx.up(), nofirstPosSpammer)
               && (is1l13lPlusVersion || !BlockUtils.isOccupiedByEnt(posx.up(2), nofirstPosSpammer))) {
               for (EntityLivingBase targetxx : targets) {
                  if (!(BlockUtils.getDistanceAtPosToVec(posx, BlockUtils.getEntityVec3dPos(targetxx)) > 7.2)
                     && this.posIsAcceptable(posx, targetxx, doNotBlowUpEnts, this.spammingCrystals(targetxx, is1l13lPlusVersion, false), false)) {
                     this.positionsCrys.add(posx);
                  }
               }
            }
         }
      }

      if (this.positionsCrys.isEmpty()
         && isAdobeHeadTrap
         && crystal == null
         && forObsidianPos == null
         && (
            this.forMiningPos == null
               || PlayerHelper.get.progressPacket < 0.1F
               || !(PlayerHelper.get.progressPacket > 0.0)
               || !(PlayerHelper.get.progressPacket < 0.8F)
         )) {
         for (EntityLivingBase targetxxx : targets) {
            BlockPos targetBasePos = BlockUtils.getEntityBlockPos(targetxxx);
            BlockPos placeCrystalPosAdobeHead = targetBasePos.up(2);
            if (!(
                  self.getDistanceAtEye(
                        (double)placeCrystalPosAdobeHead.getX(), (double)placeCrystalPosAdobeHead.getY(), (double)placeCrystalPosAdobeHead.getZ()
                     )
                     > (double)ranges[0]
               )
               && mc.world.getBlockState(placeCrystalPosAdobeHead).getBlock() == Blocks.OBSIDIAN
               && this.spoofBlocksForCheckSeenEntities2(placeCrystalPosAdobeHead, 1, Blocks.AIR, targetxxx, self)
               && BlockUtils.canPlaceCrystal(placeCrystalPosAdobeHead, is1l13lPlusVersion)
               && !BlockUtils.isOccupiedByEnt(placeCrystalPosAdobeHead.up(), false)) {
               this.positionsCrys.add(placeCrystalPosAdobeHead);
            }
         }
      }

      for (BlockPos posCrys : this.positionsCrys) {
         if (posCrys == null) {
            break;
         }

         this.cacheControlOrAdd(posCrys, this.CheckCrystalOwner.getBool());
      }

      if (this.positionsCrys.size() > 1 && targets != null && targets.size() > 0) {
         this.positionsCrys
            .sort(
               Comparator.comparing(
                  posxx -> BlockUtils.getDistanceAtVecToVec(new Vec3d(posxx).addVector(0.5, 0.1, 0.5), BlockUtils.getEntityVec3dPos(targets.get(0)))
               )
            );
      }

      return this.positionsCrys.size() > 0 ? this.positionsCrys.get(0) : null;
   }

   private BlockPos posObsidian(
      List<BlockPos> sphere, float[] ranges, List<EntityLivingBase> targets, boolean ignoreWalls, boolean isAdobeHeadTrap, boolean isSurroundAura
   ) {
      this.positionsObs.clear();
      boolean is1l13lPlusVersion = this.is1l13lPlusVersion();
      List<EntityLivingBase> doNotBlowUpEnts = this.doNotBlowUpEnts(ranges);
      EntityPlayer self = this.getMe();
      if (isAdobeHeadTrap && forCrystalPos == null && this.forMiningPos == null) {
         for (EntityLivingBase target : targets) {
            BlockPos targetPos = BlockUtils.getEntityBlockPos(target);
            BlockPos adobeHeadPos = targetPos.up(2);
            BlockPos targetPosDown = targetPos.down();
            if (mc.world.isAirBlock(adobeHeadPos)
               && BlockUtils.blockMaterialIsCurrent(targetPosDown)
               && !BlockUtils.blockMaterialIsCurrent(targetPos.up())
               && !BlockUtils.blockMaterialIsCurrent(targetPos.up(3))
               && (is1l13lPlusVersion || !BlockUtils.blockMaterialIsCurrent(targetPos.up(4)))) {
               boolean fullSurround0 = true;

               for (EnumFacing hFace : EnumFacing.HORIZONTALS) {
                  BlockPos offset = targetPos.offset(hFace);
                  if (!BlockUtils.blockMaterialIsCurrent(offset)) {
                     fullSurround0 = false;
                     break;
                  }
               }

               if (fullSurround0) {
                  boolean anyCrystalAdobeHeadBlock = false;
                  BlockPos presumptiveCrystalPosAdobeHead = adobeHeadPos.up();

                  for (Entity entity : mc.world.getLoadedEntityList()) {
                     if (entity != null && entity instanceof EntityEnderCrystal crystal) {
                        BlockPos crystalPos = BlockUtils.getEntityBlockPos(crystal);
                        if (crystalPos.getX() == presumptiveCrystalPosAdobeHead.getX()
                           && crystalPos.getY() == presumptiveCrystalPosAdobeHead.getY()
                           && crystalPos.getZ() == presumptiveCrystalPosAdobeHead.getZ()) {
                           anyCrystalAdobeHeadBlock = true;
                           break;
                        }
                     }
                  }

                  if (!anyCrystalAdobeHeadBlock) {
                     boolean hasAllBlocksAroundHead = true;

                     for (EnumFacing hFacex : EnumFacing.HORIZONTALS) {
                        BlockPos offsetUp = targetPos.offset(hFacex).up();
                        if (!BlockUtils.blockMaterialIsCurrent(offsetUp)) {
                           if (self.getDistanceAtEye((double)offsetUp.getX() + 0.5, (double)offsetUp.getY() + 0.5, (double)offsetUp.getZ() + 0.5)
                                 < (double)ranges[0]
                              && !BlockUtils.isOccupiedByEnt(offsetUp, false)) {
                              this.positionsObs.add(offsetUp);
                           }

                           hasAllBlocksAroundHead = false;
                        }
                     }

                     if (hasAllBlocksAroundHead) {
                        if (BlockUtils.canPlaceBlock(adobeHeadPos)) {
                           if (BlockUtils.blockMaterialIsCurrentWithSideSetsCount(adobeHeadPos, true) != 4) {
                              isSurroundAura = false;
                           } else if (self.getDistanceAtEye(
                                    (double)adobeHeadPos.getX() + 0.5, (double)adobeHeadPos.getY() + 0.5, (double)adobeHeadPos.getZ() + 0.5
                                 )
                                 < (double)ranges[0]
                              && !BlockUtils.isOccupiedByEnt(adobeHeadPos, false)) {
                              this.positionsObs.add(adobeHeadPos);
                              continue;
                           }
                        }

                        if (!BlockUtils.canPlaceBlock(adobeHeadPos) || BlockUtils.blockMaterialIsCurrentWithSideSetsCount(adobeHeadPos, true) != 4) {
                           for (EnumFacing hFacexx : EnumFacing.HORIZONTALS) {
                              BlockPos offsetAdobeHead = targetPos.offset(hFacexx).up(2);
                              if (BlockUtils.canPlaceBlock(offsetAdobeHead)
                                 && self.getDistanceAtEye(
                                       (double)offsetAdobeHead.getX() + 0.5, (double)offsetAdobeHead.getY() + 0.5, (double)offsetAdobeHead.getZ() + 0.5
                                    )
                                    < (double)ranges[0]) {
                                 this.positionsObs.add(offsetAdobeHead);
                                 if (offsetAdobeHead.getX() == adobeHeadPos.getX() && offsetAdobeHead.getZ() == adobeHeadPos.getZ()) {
                                    for (EnumFacing hFace2 : EnumFacing.HORIZONTALS) {
                                       BlockPos offsetPos = offsetAdobeHead.offset(hFace2);
                                       if (!BlockUtils.blockMaterialIsCurrent(offsetPos)
                                          && self.getDistanceAtEye(
                                                (double)offsetPos.getX() + 0.5, (double)offsetPos.getY() + 0.5, (double)offsetPos.getZ() + 0.5
                                             )
                                             < (double)ranges[0]
                                          && !BlockUtils.isOccupiedByEnt(offsetPos, false)) {
                                          this.positionsObs.add(offsetPos);
                                       }
                                    }
                                 }

                                 isSurroundAura = false;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (this.positionsObs.isEmpty()) {
         for (BlockPos pos : sphere) {
            if (BlockUtils.canPlaceObsidian(pos, ranges[0], true, true, is1l13lPlusVersion)
               && !BlockUtils.isOccupiedByEnt(pos, false)
               && (ignoreWalls || BlockUtils.getPlaceableSideSeen(pos, self) != null)) {
               for (EntityLivingBase targetx : targets) {
                  if (pos != BlockUtils.getEntityBlockPos(targetx)
                     && !((double)pos.getY() > targetx.posY - 0.3780711)
                     && CrystalField.crystal == null
                     && (
                        forCrystalPos == null
                           || (
                                 !(BlockUtils.getDistanceAtVecToVec(new Vec3d(forCrystalPos).addVector(0.5, 0.5, 0.5), targetx.getPositionVector()) < 2.85)
                                    || !BlockUtils.canPosBeSeenEntity(forCrystalPos.up(), targetx, BlockUtils.bodyElement.LEGS, true)
                              )
                              && !((double)forCrystalPos.getY() > targetx.posY + (targetx.posY - targetx.lastTickPosY > 0.0 ? 0.01 : 0.3780711))
                              && !((double)pos.getY() > targetx.posY + (targetx.posY - targetx.lastTickPosY > 0.0 ? 0.01 : 0.3780711))
                     )
                     && !(BlockUtils.getDistanceAtVecToVec(new Vec3d(pos).addVector(0.5, 1.0, 0.5), targetx.getPositionVector()) >= 7.2)
                     && this.posIsAcceptable(pos, targetx, doNotBlowUpEnts, false, true)) {
                     this.positionsObs.add(pos);
                  }
               }
            }
         }

         if (this.positionsObs.isEmpty()) {
            for (BlockPos posx : sphere) {
               if (BlockUtils.canPlaceObsidian(posx, ranges[0], true, true, is1l13lPlusVersion)
                  && !BlockUtils.isOccupiedByEnt(posx, false)
                  && (ignoreWalls || BlockUtils.getPlaceableSideSeen(posx, self) != null)) {
                  for (EntityLivingBase targetxx : targets) {
                     if (!((double)posx.getY() > targetxx.posY + 1.0) && posx != BlockUtils.getEntityBlockPos(targetxx)) {
                        boolean spamCrystals = this.spammingCrystals(targetxx, is1l13lPlusVersion, true);
                        if (spamCrystals
                           && (
                              forCrystalPos == null
                                 || !(BlockUtils.getDistanceAtVecToVec(new Vec3d(forCrystalPos).addVector(0.5, 0.5, 0.5), targetxx.getPositionVector()) < 3.85)
                                    && !((double)forCrystalPos.getY() > targetxx.posY + (targetxx.posY - targetxx.lastTickPosY > 0.0 ? 0.01 : 0.3780711))
                                    && !((double)posx.getY() > targetxx.posY + (targetxx.posY - targetxx.lastTickPosY > 0.0 ? 0.01 : 0.3780711))
                           )
                           && !(BlockUtils.getDistanceAtVecToVec(new Vec3d(posx).addVector(0.5, 1.0, 0.5), targetxx.getPositionVector()) >= 7.2)
                           && this.posIsAcceptable(posx, targetxx, doNotBlowUpEnts, spamCrystals, true)) {
                           this.positionsObs.add(posx);
                        }
                     }
                  }
               }
            }
         }
      }

      for (BlockPos posObs : this.positionsObs) {
         BlockPos upPosCrys;
         if (posObs == null || (upPosCrys = posObs.up()) == null) {
            break;
         }

         this.cacheControlOrAdd(upPosCrys, this.CheckCrystalOwner.getBool());
      }

      if (this.positionsObs.size() > 1 && targets != null && targets.size() > 0) {
         this.positionsObs
            .sort(
               Comparator.comparing(
                  posxx -> BlockUtils.getDistanceAtVecToVec(new Vec3d(posxx).addVector(0.5, 1.0, 0.5), BlockUtils.getEntityVec3dPos(targets.get(0)))
               )
            );
      }

      if (this.positionsObs.isEmpty() && isSurroundAura) {
         for (EntityLivingBase targetxxx : targets) {
            for (BlockPos surroundSide : this.getPredictedSurroundTargetPositions(targetxxx, isSurroundAura, 2.5F, ranges)) {
               if (!BlockUtils.isOccupiedByEnt(surroundSide, false)
                  && BlockUtils.getPlaceableSide(surroundSide) != null
                  && this.posIsAcceptable(surroundSide, targetxxx, doNotBlowUpEnts, true, true)) {
                  this.positionsObs.add(surroundSide);
               }
            }
         }
      }

      return !this.positionsObs.isEmpty() ? this.positionsObs.get(0) : null;
   }

   private Vec3d selfVirtPos() {
      BlockPos virtAt = !listIsEmptyOrNull(this.getTargets()) ? BlockUtils.getEntityBlockPos(this.getTargets().get(0)) : null;
      return virtAt != null && this.isTPA(virtAt) ? new Vec3d(virtAt).addVector(0.5, 0.5, 0.5) : BlockUtils.getEntityVec3dPos(this.getMe());
   }

   private boolean updateSphere(List<BlockPos> sphere, float range, Vec3d atMyVirtPos) {
      sphere.clear();
      List<EntityLivingBase> targets = this.getTargets();
      BlockUtils.getSphere(atMyVirtPos.addVector(0.0, (double)this.getMe().getEyeHeight(), 0.0), range).forEach(pos -> {
         if (pos != null) {
            if (targets.isEmpty()) {
               sphere.add(pos);
            } else {
               double maxY = 0.0;

               for (EntityLivingBase target : targets) {
                  if (!(maxY >= target.posY)) {
                     maxY = target.posY;
                  }
               }

               if ((double)pos.getY() <= maxY + 1.0) {
                  sphere.add(pos);
               }
            }
         }
      });
      return !listIsEmptyOrNull(sphere);
   }

   private void updatePosForPlace(
      List<EntityLivingBase> targets,
      float[] radiuses,
      Vec3d atMyVirtPos,
      boolean canUseInventory,
      boolean onlyReset,
      boolean ignoreWalls,
      boolean isAdobeHeadTrap,
      boolean isSurroundAura
   ) {
      if (forCrystalPos != null) {
         this.cacheControlOrAdd(forCrystalPos, this.CheckCrystalOwner.getBool());
      }

      if (forObsidianPos != null) {
         this.cacheControlOrAdd(forObsidianPos, this.CheckCrystalOwner.getBool());
      }

      if (!onlyReset
         && !listIsEmptyOrNull(this.getTargets())
         && this.updateSphere(this.sphere, radiuses[0], atMyVirtPos)
         && this.haveItem(this.itemCrystal(), canUseInventory)
         && !listIsEmptyOrNull(targets)
         && !this.stopBreaks(this.NoSuicide.getBool())) {
         forCrystalPos = this.sleepUpdatePlaceCrysAdobeHeadTicks <= 0 && this.haveItem(this.itemCrystal(), canUseInventory)
            ? this.posCrystal(this.sphere, radiuses, targets, isAdobeHeadTrap)
            : null;
         AntiCrystal.get.removeCache(forCrystalPos);
         forObsidianPos = this.PlaceObsidian.getBool() && this.sleepUpdatePlaceObsAdobeHeadTicks <= 0 && this.haveItem(this.itemObsidian(), canUseInventory)
            ? this.posObsidian(this.sphere, radiuses, targets, ignoreWalls, isAdobeHeadTrap, isSurroundAura)
            : null;
         AntiCrystal.get.removeCache(forObsidianPos);
      } else {
         forCrystalPos = null;
         forObsidianPos = null;
      }
   }

   private Item itemCrystal() {
      return Items.END_CRYSTAL;
   }

   private Item itemObsidian() {
      return Item.getItemFromBlock(Blocks.OBSIDIAN);
   }

   public static int getItem(Item designatedItem) {
      for (int i = 0; i < 44; i++) {
         Item item = Minecraft.player.inventory.getStackInSlot(i).getItem();
         if (item instanceof Item && item.equals(designatedItem)) {
            return i;
         }
      }

      return -1;
   }

   public boolean haveItemInInventory(Item item, boolean searchInInventory) {
      return this.getSlotForItem(item, searchInInventory) != -1;
   }

   public boolean haveItem(Item item, boolean searchInInventory) {
      return (this.haveItemInInventory(item, searchInInventory) || Minecraft.player.getHeldItemOffhand().getItem() == item)
         && this.getUsedHand(item, searchInInventory) != null;
   }

   public int getSlotForItem(Item item, boolean canUseInventory) {
      int slot = getItem(item);
      return slot > 8 && !canUseInventory ? -1 : slot;
   }

   public boolean itemInOffHand(Item item) {
      return Minecraft.player.getHeldItemOffhand().getItem() == item;
   }

   private boolean canAddTargetez(EntityLivingBase target, boolean players, boolean mobs) {
      return target != null
         && target.isEntityAlive()
         && target.getHealth() > 0.0F
         && target != Minecraft.player
         && target != this.getMe()
         && (players && mobs || target instanceof EntityPlayer && players || !(target instanceof EntityPlayer) && mobs)
         && !Client.friendManager.isFriend(target.getName())
         && !(target instanceof EntityArmorStand)
         && !Client.summit(target);
   }

   private double getDistanceToTargetEntity(Entity target) {
      return this.getMe().getSmartDistanceToAABB(RotationUtil.getLookRots(this.getMe(), target), target);
   }

   private double getEntityValueToSort(EntityLivingBase target) {
      if (target != null && (double)target.getHealth() > 0.0) {
         double value = 1.0;
         double health = (double)target.getHealth() + (double)target.getAbsorptionAmount();
         double maxHealth = (double)target.getMaxHealth() + (double)target.getAbsorptionAmount();
         value *= MathUtils.clamp(health / maxHealth * 3.0, 0.25, 1.0);
         value *= MathUtils.clamp((double)target.getTotalArmorValue() / 10.0, 0.65, 1.0);
         return value * MathUtils.clamp(1.0 - (double)BlockUtils.feetCrackPosesCount(target, this.is1l13lPlusVersion()) / 4.0, 0.0, 1.0);
      } else {
         return 0.0;
      }
   }

   private boolean canBreakCrystal(EntityEnderCrystal crys, float[] ranges, boolean isAdobeHeadTrap) {
      if (crys == null) {
         return false;
      } else {
         Vec3d vecCrystalDown = BlockUtils.getEntityVec3dPos(crys).addVector(0.0, -1.0, 0.0);
         BlockPos crystalDownBlockPos = new BlockPos(vecCrystalDown);
         boolean checkNearState = false;
         if (forCrystalPos != null) {
            for (int xOff = -2; xOff < 2; xOff++) {
               for (int zOff = -2; zOff < 2; zOff++) {
                  BlockPos offset = crystalDownBlockPos.add(xOff, 0, zOff);
                  if (forCrystalPos.getX() == offset.getX() && forCrystalPos.getY() == offset.getY() && forCrystalPos.getZ() == offset.getZ()) {
                     return offset.getX() != crystalDownBlockPos.getX()
                        || offset.getY() != crystalDownBlockPos.getY()
                        || offset.getZ() != crystalDownBlockPos.getZ();
                  }
               }
            }
         }

         boolean checksCache = this.hasInCacheEntityEnderCrystal(crys, this.CheckCrystalOwner.getBool());
         boolean isCrystalAdobeAnyTarget = false;
         if (!checksCache) {
            if (isAdobeHeadTrap) {
               for (EntityLivingBase target : this.getTargets()) {
                  if (target != null
                     && (int)target.posY + 3 == (int)crys.posY
                     && mc.world.getBlockState(BlockUtils.getEntityBlockPos(target).up(2)).getBlock() == Blocks.AIR
                     && mc.world.isAirBlock(BlockUtils.getEntityBlockPos(crys).down())) {
                     isCrystalAdobeAnyTarget = true;
                     break;
                  }
               }
            }

            if (!isCrystalAdobeAnyTarget) {
               return false;
            }
         }

         boolean hasAnyBlowTargets = checkNearState;
         Vec3d crystalVec = crys.getPositionVector();
         if (!checkNearState) {
            boolean is1l13lPlusVersion = this.is1l13lPlusVersion();

            for (EntityLivingBase targetx : this.getTargets()) {
               if (targetx != null
                  && !(targetx.getSmoothDistanceToEntity(crys) >= 7.2F)
                  && (!(targetx instanceof EntityPlayer player) || !player.isCreative() && !player.isSpectator())) {
                  Vec3d targetVec = targetx.getPositionVector();
                  if (BlockUtils.canPosBeSeenEntityWithCustomVec(crystalVec, targetx, targetVec, BlockUtils.bodyElement.LEGS, true)
                     || this.spammingCrystals(targetx, is1l13lPlusVersion, false)
                        && BlockUtils.canPosBeSeenEntityWithCustomVec(crystalVec, targetx, targetVec, BlockUtils.bodyElement.CHEST, true)) {
                     hasAnyBlowTargets = true;
                     break;
                  }
               }
            }
         }

         boolean hasAnyBlowSelfOrFriends = false;
         if (hasAnyBlowTargets) {
            EntityPlayer self = this.getMe();

            for (EntityLivingBase frOrSelf : this.doNotBlowUpEnts(ranges)) {
               if (frOrSelf != null
                  && !(frOrSelf.getSmoothDistanceToEntity(crys) >= 7.2F)
                  && (!(frOrSelf instanceof EntityPlayer playerx) || !playerx.isCreative() && !playerx.isSpectator())) {
                  Vec3d virtFrOrSelfPos = frOrSelf == self ? this.selfVirtPos() : BlockUtils.getEntityVec3dPos(frOrSelf);
                  if (BlockUtils.canPosBeSeenEntityWithCustomVec(
                        vecCrystalDown, frOrSelf, virtFrOrSelfPos, isCrystalAdobeAnyTarget ? BlockUtils.bodyElement.FEET : BlockUtils.bodyElement.LEGS, true
                     )
                     && (!(frOrSelf.getDistanceToVec3d(crystalVec) > 2.7F) || !(frOrSelf.posY < crys.posY))) {
                     hasAnyBlowSelfOrFriends = true;
                     break;
                  }
               }
            }
         }

         return hasAnyBlowTargets ? !hasAnyBlowSelfOrFriends : false;
      }
   }

   public static boolean listIsEmptyOrNull(List list) {
      return list == null || list.isEmpty();
   }

   private void updateTargets(float[] ranges, int maxCountTargets, boolean onlyReset) {
      targetezs.clear();
      if (!onlyReset) {
         for (Entity entity : mc.world.getLoadedEntityList()) {
            if (entity != null && entity instanceof EntityLivingBase) {
               EntityLivingBase base = (EntityLivingBase)entity;
               if (this.canAddTargetez(base, true, false)
                  && this.getDistanceToTargetEntity(base) <= (double)(ranges[0] + ranges[1])
                  && targetezs.size() < maxCountTargets) {
                  targetezs.add(base);
               }
            }
         }

         if (listIsEmptyOrNull(targetezs)) {
            for (Entity entityx : mc.world.getLoadedEntityList()) {
               if (entityx != null && entityx instanceof EntityLivingBase) {
                  EntityLivingBase base = (EntityLivingBase)entityx;
                  if (this.canAddTargetez(base, false, true)
                     && this.getDistanceToTargetEntity(base) <= (double)(ranges[0] + ranges[1])
                     && targetezs.size() < maxCountTargets) {
                     targetezs.add(base);
                  }
               }
            }
         }

         if (!listIsEmptyOrNull(targetezs) && targetezs.size() > 1) {
            targetezs.sort(Comparator.comparing(this::getEntityValueToSort));
         }
      }
   }

   private void updateCrystals(float[] ranges, boolean onlyReset, boolean isAdobeHeadTrap) {
      this.crystals.clear();
      if (onlyReset) {
         CrystalField.crystal = null;
      } else {
         for (Entity entity : mc.world.getLoadedEntityList()) {
            if (entity != null && entity instanceof EntityEnderCrystal) {
               EntityEnderCrystal crystal = (EntityEnderCrystal)entity;
               if (this.getMe().getDistanceAtEye(crystal.posX, crystal.posY, crystal.posZ)
                     <= (double)(ranges[this.isTPA(BlockUtils.getEntityBlockPos(crystal)) ? 2 : 0] + crystal.width / 1.36F)
                  && crystal != null
                  && !crystal.isDead
                  && this.canBreakCrystal(crystal, ranges, isAdobeHeadTrap)
                  && (crystal.ticksExisted % 2 != 1 || crystal.ticksExisted >= 4)) {
                  this.crystals.add(crystal);
               }
            }
         }

         if (!listIsEmptyOrNull(this.crystals)) {
            if (this.crystals.size() > 1) {
               this.crystals
                  .sort(Comparator.comparing(crystalx -> BlockUtils.getDistanceAtVecToVec(this.selfVirtPos(), BlockUtils.getEntityVec3dPos(crystalx))));
            }

            CrystalField.crystal = this.stopBreaks(this.NoSuicide.getBool()) ? null : this.crystals.get(0);
         }
      }
   }

   private EntityPlayer getMe() {
      return (EntityPlayer)(FreeCam.get.actived && FreeCam.fakePlayer != null ? FreeCam.fakePlayer : Minecraft.player);
   }

   private float getSmartBlowupWarnHP() {
      float hp = 4.0F;
      int armorCount = 0;
      EntityPlayer p = this.getMe();
      float absorbDT = 1.0F;

      for (int i = 0; i < 4; i++) {
         if (!BlockUtils.isArmor(p, BlockUtils.armorElementByInt(i))) {
            hp++;
            absorbDT += 0.5F;
         } else {
            armorCount++;
         }
      }

      if (p.getActivePotionEffect(Potion.getPotionById(10)) != null) {
         hp -= 0.5F;
      }

      if (p.isHandActive() && p.getActiveItemStack().getItem() instanceof ItemAppleGold && p.getItemInUseMaxCount() > 30) {
         hp--;
      }

      if (p.getAbsorptionAmount() > 0.0F) {
         hp -= p.getAbsorptionAmount() / MathUtils.clamp(absorbDT, 1.0F, 2.0F);
      }

      if (p.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra || armorCount < 4) {
         hp += 3.0F;
      }

      return MathUtils.clamp(hp + 2.0F, 5.0F, 20.0F);
   }

   private boolean stopBreaks(boolean checkCrystal) {
      return checkCrystal
         && !Minecraft.player.isCreative()
         && !Minecraft.player.isSpectator()
         && (
            OffHand.totemTaken
               || OffHand.crystalWarn(7.2F)
               || Minecraft.player.getHealth() / Minecraft.player.getMaxHealth() < this.getSmartBlowupWarnHP() / Minecraft.player.getMaxHealth()
         );
   }

   private boolean slotIsNan(Item itemIn, boolean canUseInventory) {
      int sl = this.getSlotForItem(itemIn, canUseInventory);
      return sl < 0 || sl > (canUseInventory ? 44 : 8);
   }

   private void switcherForAction(EnumHand placeHand, boolean packetSwap, Item swapTo, Runnable action, boolean useInventory) {
      if (swapTo != null) {
         int slotHand = Minecraft.player.inventory.currentItem;
         int slotItem = this.getSlotForItem(swapTo, false);
         boolean hasInvUse = false;
         if (useInventory && slotItem == -1 && placeHand == EnumHand.MAIN_HAND) {
            slotItem = this.getSlotForItem(swapTo, useInventory);
            hasInvUse = true;
         }

         boolean isInHand = slotHand == slotItem;
         if (placeHand == EnumHand.OFF_HAND) {
            action.run();
         } else if (!this.slotIsNan(swapTo, hasInvUse)) {
            if (slotItem < 9) {
               if (placeHand == EnumHand.MAIN_HAND && !isInHand && !this.slotIsNan(swapTo, false)) {
                  boolean packetSync = Minecraft.player.inventory.currentItem != slotItem;
                  Minecraft.player.inventory.currentItem = slotItem;
                  if (packetSync) {
                     mc.playerController.syncCurrentPlayItem();
                  }
               }

               if (isInHand || !this.slotIsNan(swapTo, false)) {
                  action.run();
               }

               if (placeHand == EnumHand.MAIN_HAND && !isInHand && !this.slotIsNan(swapTo, false) && packetSwap) {
                  Minecraft.player.inventory.currentItem = slotHand;
                  mc.playerController.syncCurrentPlayItem();
               }
            } else if (hasInvUse) {
               ItemStack stack = Minecraft.player.inventory.getStackInSlot(slotItem);
               ItemStack prevHandStack = Minecraft.player.getHeldItemMainhand();
               if (prevHandStack != null
                  && Minecraft.player.isCreative()
                  && (Minecraft.player.openContainer == null || Minecraft.player.openContainer instanceof ContainerPlayer)) {
                  Minecraft.player.connection.sendPacket(new CPacketCreativeInventoryAction(Minecraft.player.inventory.currentItem + 36, stack));
                  mc.playerController.syncCurrentPlayItem();
                  action.run();
                  int slotPrevStack = -1;

                  for (int i = 0; i < 44; i++) {
                     ItemStack geted = Minecraft.player.inventory.getStackInSlot(i);
                     if (geted == prevHandStack) {
                        slotPrevStack = i;
                     }
                  }

                  mc.playerController.windowClick(0, slotPrevStack, Minecraft.player.inventory.currentItem, ClickType.SWAP, Minecraft.player);
                  return;
               }

               mc.playerController.windowClick(0, slotItem, Minecraft.player.inventory.currentItem, ClickType.SWAP, Minecraft.player);
               action.run();
               mc.playerController.windowClick(0, slotItem, Minecraft.player.inventory.currentItem, ClickType.SWAP, Minecraft.player);
            }
         }
      }
   }

   private void placeCrystal(
      BlockPos pos, EnumHand placeHand, boolean packetSwap, ru.govno.client.utils.Math.TimerHelper placeCrysDelay, boolean useTeleport, boolean useInventory
   ) {
      this.setTeleportForActs(() -> this.switcherForAction(placeHand, packetSwap, this.itemCrystal(), () -> {
            mc.playerController.processRightClickBlock(Minecraft.player, mc.world, pos, EnumFacing.UP, new Vec3d(0.5, 0.0, 0.5), placeHand);
            if (placeCrysDelay != null) {
               placeCrysDelay.reset();
            }

            hasActionToCPSHint = true;
         }, useInventory), this.vecOfPos(this.bestCurrentablePosStandOpacityNeareble(pos, 5)), useTeleport);
   }

   private void placeObsidian(
      BlockPos pos, EnumHand placeHand, boolean packetSwap, ru.govno.client.utils.Math.TimerHelper placeObsDelay, boolean useTeleport, boolean useInventory
   ) {
      if (pos != null) {
         this.setTeleportForActs(
            () -> this.switcherForAction(
                  placeHand,
                  packetSwap,
                  this.itemObsidian(),
                  () -> {
                     EnumFacing enumFace = BlockUtils.getPlaceableSideSeen(pos, this.getMe());
                     if (enumFace == null) {
                        enumFace = BlockUtils.getPlaceableSide(pos);
                        if (enumFace != null) {
                           EnumFacing faceOpposite = enumFace.getOpposite();
                           BlockPos offsetPos = pos.offset(enumFace);
                           Vec3d facingVec = new Vec3d(offsetPos).addVector(0.5, 0.5, 0.5).add(new Vec3d(faceOpposite.getDirectionVec()).scale(0.5));
                           mc.playerController.processRightClickBlock(Minecraft.player, mc.world, offsetPos, faceOpposite, facingVec, placeHand);
                           this.swingAction(placeHand);
                        }
                     } else {
                        Vec3d placeFaceVec = new Vec3d(pos)
                           .addVector(0.5, 0.5, 0.5)
                           .addVector(
                              (double)enumFace.getFrontOffsetX() * 0.5, (double)enumFace.getFrontOffsetY() * 0.5, (double)enumFace.getFrontOffsetZ() * 0.5
                           );
                        if (this.getMe() == Minecraft.player
                           && Minecraft.player.getDistanceAtEye(placeFaceVec.xCoord, placeFaceVec.yCoord, placeFaceVec.zCoord) <= 5.0) {
                           float[] rotate = RotationUtil.getNeededFacing(placeFaceVec, false, Minecraft.player, false);
                           float prevYaw = Minecraft.player.rotationYaw;
                           float prevPitch = Minecraft.player.rotationPitch;
                           Minecraft.player.rotationYaw = rotate[0];
                           Minecraft.player.rotationPitch = rotate[1];
                           HitAura.get.rotations = rotate;
                           mc.playerController.setBlockReachDistances(6.5F, 5.5F);
                           mc.entityRenderer.getMouseOver(1.0F);
                           mc.playerController.setBlockReachDistances(5.0F, 4.5F);
                           BlockPos placePos = mc.objectMouseOver.getBlockPos();
                           if (placePos != null) {
                              placePos = placePos.offset(enumFace);
                              if (placePos != null && mc.objectMouseOver.sideHit != null && mc.objectMouseOver.hitVec != null) {
                                 mc.playerController
                                    .processRightClickBlock(
                                       Minecraft.player,
                                       mc.world,
                                       mc.objectMouseOver.getBlockPos(),
                                       mc.objectMouseOver.sideHit,
                                       mc.objectMouseOver.hitVec,
                                       placeHand
                                    );
                                 this.swingAction(placeHand);
                              }
                           }

                           Minecraft.player.rotationYaw = prevYaw;
                           Minecraft.player.rotationPitch = prevPitch;
                           mc.entityRenderer.getMouseOver(1.0F);
                        }

                        this.skipTicks = !listIsEmptyOrNull(targetezs);
                     }
                  },
                  useInventory
               ),
            this.vecOfPos(this.bestCurrentablePosStandOpacityNeareble(pos, 5)),
            useTeleport
         );
         placeObsDelay.reset();
      }
   }

   private void swingAction(EnumHand breakHand) {
      String var2 = this.Swing.currentMode;
      switch (var2) {
         case "Packet":
            Minecraft.player.connection.sendPacket(new CPacketAnimation(breakHand));
            break;
         case "Client":
            Minecraft.player.swingArm(breakHand);
      }
   }

   private void breakCrystal(
      EntityEnderCrystal crystal,
      EnumHand breakHand,
      boolean setDead,
      ru.govno.client.utils.Math.TimerHelper attackDelay,
      boolean useTeleport,
      boolean replaceCrystal
   ) {
      this.cacheControlOrAdd(crystal.getPosition(), this.CheckCrystalOwner.getBool());
      this.setTeleportForActs(
         () -> {
            mc.playerController.attackEntity(Minecraft.player, crystal);
            mc.getConnection().sendPacket(new CPacketUseEntity(crystal));
            this.swingAction(breakHand);
            if (replaceCrystal
               && this.haveItem(this.itemCrystal(), false)
               && (!Minecraft.player.isHandActive() || Minecraft.player.getActiveHand() != EnumHand.MAIN_HAND)) {
               this.placeCrystal(crystal.getPosition(), this.getUsedHand(Items.END_CRYSTAL, true), true, null, false, true);
            }

            attackDelay.reset();
            hasActionToCPSHint = true;
            if (this.canSpawnPopEffect()) {
               this.addPopsEffToPos(BlockUtils.getEntityBlockPos(crystal));
            }

            if (setDead) {
               crystal.setDead();
            }

            boolean var10001;
            label22: {
               if (!crystal.isDead) {
                  double var10002 = (double)Minecraft.player.height;
                  if (this.selfVirtPos().yCoord + var10002 / 1.65 <= crystal.posY) {
                     var10001 = true;
                     break label22;
                  }
               }

               var10001 = false;
            }

            this.callRotateUpOnHitCrystal = var10001;
         },
         this.vecOfPos(this.bestCurrentablePosStandOpacityNeareble(BlockUtils.getEntityBlockPos(crystal).down(), 6)),
         useTeleport
      );
   }

   private EnumHand getUsedHand(Item itemIn, boolean canUseInventory) {
      String mode = itemIn == Items.END_CRYSTAL ? this.CrystalHand.currentMode : this.ObsidianHand.currentMode;
      if (mode == null) {
         return null;
      } else {
         switch (mode) {
            case "OffHand":
               return this.itemInOffHand(itemIn) ? EnumHand.OFF_HAND : null;
            case "MainHand":
               return this.haveItemInInventory(itemIn, canUseInventory) ? EnumHand.MAIN_HAND : null;
            case "Auto":
               return this.itemInOffHand(itemIn) ? EnumHand.OFF_HAND : (this.haveItemInInventory(itemIn, canUseInventory) ? EnumHand.MAIN_HAND : null);
            default:
               return null;
         }
      }
   }

   @Override
   public void onToggled(boolean actived) {
      targetezs.clear();
      crystal = null;
      forCrystalPos = null;
      forObsidianPos = null;
      this.skipTicks = true;
      this.ticks = 1;
      this.sphere.clear();
      this.sleepUpdatePlaceObsAdobeHeadTicks = 0;
      this.sleepUpdatePlaceCrysAdobeHeadTicks = 0;
      this.sleepUpdateBreakCrysAdobeHeadTicks = 0;
      this.updateCacheList(true);
      this.packetMineCallWhileCanMiningAuto(null, false);
      super.onToggled(actived);
   }

   private float[] getRanges(boolean teleportMode) {
      float[] ranges = this.getAuraRanges();
      float tpRange = this.getTpRange();
      return new float[]{ranges[0], teleportMode ? tpRange : ranges[1], tpRange};
   }

   private float getCPSRandomizer(boolean doRandom) {
      if (doRandom && hasActionToCPSHint) {
         this.cpsDelta = Math.random() > 0.75 ? 45.0F : 0.0F;
         hasActionToCPSHint = false;
      }

      return doRandom ? this.cpsDelta : 0.0F;
   }

   private float[] getDelays(boolean teleportMode, boolean cpsBypass) {
      float random = this.getCPSRandomizer(cpsBypass && !this.ProcessRateLevel.currentMode.equalsIgnoreCase("Powerful"));
      if (teleportMode) {
         float DL = Math.max(this.getTPDelayAsProcessRateLevel() + random, 0.0F);
         return new float[]{DL, DL};
      } else {
         float[] delays = this.getDelaysAsProcessRateLevel(this.is1l13lPlusVersion());
         return new float[]{Math.max(delays[0] + random, 0.0F), Math.max(delays[1] + random, 0.0F)};
      }
   }

   private ru.govno.client.utils.Math.TimerHelper[] getTimers(boolean teleportMode) {
      return teleportMode
         ? new ru.govno.client.utils.Math.TimerHelper[]{this.tpaDelay, this.tpaDelay, this.tpaDelay}
         : new ru.govno.client.utils.Math.TimerHelper[]{this.placeCrysDelay, this.placeObsDelay, this.attackDelay};
   }

   @Override
   public void onRenderUpdate() {
      String rateLevelMode = this.getAuraUpdaterAsProcessRateLevel();
      if (rateLevelMode.equalsIgnoreCase("FpsThread") || rateLevelMode.equalsIgnoreCase("OverrideAll")) {
         this.updateCrystalAura(
            this.isTPA(
               forObsidianPos != null
                  ? forObsidianPos
                  : (forCrystalPos != null ? forCrystalPos : (crystal != null ? BlockUtils.getEntityBlockPos(crystal) : null))
            )
         );
         this.crystalAura();
         this.packetMineCallWhileCanMiningAuto(this.getTargets(), this.SyncPacketMineAura.getBool());
      }
   }

   @Override
   public void onUpdate() {
      if (this.MaxTargetsCount.getFloat() != (float)((int)this.MaxTargetsCount.getFloat())
         && mc.currentScreen instanceof ClickGuiScreen != Mouse.isButtonDown(0)) {
         this.MaxTargetsCount.setFloat((float)((int)this.MaxTargetsCount.getFloat()));
      }

      this.popsEffRemoveAuto();
      this.updateCacheList(!this.CheckCrystalOwner.getBool());
      String rateLevelMode = this.getAuraUpdaterAsProcessRateLevel();
      if (rateLevelMode.equalsIgnoreCase("Default") || rateLevelMode.equalsIgnoreCase("OverrideAll")) {
         this.updateCrystalAura(
            this.isTPA(
               forObsidianPos != null
                  ? forObsidianPos
                  : (forCrystalPos != null ? forCrystalPos : (crystal != null ? BlockUtils.getEntityBlockPos(crystal) : null))
            )
         );
         this.crystalAura();
         this.packetMineCallWhileCanMiningAuto(this.getTargets(), this.SyncPacketMineAura.getBool());
      }

      if (this.sleepUpdatePlaceObsAdobeHeadTicks > 0) {
         this.sleepUpdatePlaceObsAdobeHeadTicks--;
      }

      if (this.sleepUpdatePlaceCrysAdobeHeadTicks > 0) {
         this.sleepUpdatePlaceCrysAdobeHeadTicks--;
      }

      if (this.sleepUpdateBreakCrysAdobeHeadTicks > 0) {
         this.sleepUpdateBreakCrysAdobeHeadTicks--;
      }
   }

   public List<EntityLivingBase> getTargets() {
      return (List<EntityLivingBase>)(get == null ? new ArrayList<>() : targetezs);
   }

   private boolean canUseCrystalFieldNow() {
      return true;
   }

   private void updateCrystalAura(boolean teleportMode) {
      boolean canUseInventory = this.UseInventory.getBool() && this.UseInventory.isVisible();
      float[] ranges = this.getRanges(teleportMode);
      int delay = this.getPosChangeDelayAsProcessRateLevel();
      boolean canUseAuraNow = this.canUseCrystalFieldNow();
      boolean ignoreWalls = this.PlaceIgnoreWalls.getBool();
      boolean isAdobeHeadTrap = this.isAdobeHeadTrap();
      boolean isSurroundAura = this.isSetSurroundToTargetPredictedMove();
      this.updateCrystals(ranges, !canUseAuraNow, isAdobeHeadTrap);
      if (delay != 1 && delay != 2 ? this.ticks % delay == 0 || this.ticks % delay == 1 || this.skipTicks : this.ticks % delay == 0) {
         boolean f1 = delay <= 2 || this.ticks % delay == 0 || this.skipTicks;
         boolean f2 = delay <= 2 || this.ticks % delay == 1 || this.skipTicks;
         if (f1) {
            this.updateTargets(
               ranges,
               (int)MathUtils.clamp(this.MaxTargetsCount.getFloat(), 1.0F, 8.0F) * (this.getUsedHand(this.itemCrystal(), canUseInventory) != null ? 1 : 0),
               !canUseAuraNow
            );
         }

         if (f2) {
            this.updatePosForPlace(this.getTargets(), ranges, this.selfVirtPos(), canUseInventory, !canUseAuraNow, ignoreWalls, isAdobeHeadTrap, isSurroundAura);
         }

         this.skipTicks = false;
      }

      if (this.ticks > 1000) {
         this.ticks = 0;
      } else {
         this.ticks++;
      }
   }

   @Override
   public String getDisplayName() {
      boolean teleportAction = this.isTPA(
         forObsidianPos != null ? forObsidianPos : (forCrystalPos != null ? forCrystalPos : (crystal != null ? BlockUtils.getEntityBlockPos(crystal) : null))
      );
      float[] ranges = this.getRanges(teleportAction);
      return this.getName()
         + TextFormatting.GRAY
         + " - "
         + String.format("%.1f", ranges[0])
         + "+"
         + String.format("%.1f", ranges[1])
         + (targetezs == null ? "error" : "-c" + targetezs.size());
   }

   private void crystalAura() {
      boolean cpsBypass = this.CPSBypass.getBool();
      boolean teleportAction = this.isTPA(
         CrystalField.forObsidianPos != null
            ? CrystalField.forObsidianPos
            : (CrystalField.forCrystalPos != null ? CrystalField.forCrystalPos : (crystal != null ? BlockUtils.getEntityBlockPos(crystal) : null))
      );
      float[] delaysActions = this.getDelays(teleportAction, cpsBypass);
      ru.govno.client.utils.Math.TimerHelper[] timers = this.getTimers(teleportAction);
      boolean setDeadCrystal = this.canCrystalSetDeadAsProcessRateLevel();
      boolean canUseInventory = this.UseInventory.getBool() && this.UseInventory.isVisible();
      EnumHand crystalHand = this.getUsedHand(this.itemCrystal(), canUseInventory);
      if (crystalHand == null) {
         crystalHand = EnumHand.MAIN_HAND;
      }

      EnumHand obsidianHand = this.getUsedHand(this.itemObsidian(), canUseInventory);
      if (obsidianHand == null) {
         obsidianHand = EnumHand.MAIN_HAND;
      }

      boolean buseMainHand = Minecraft.player.isHandActive() && Minecraft.player.getActiveHand() == EnumHand.MAIN_HAND;
      boolean stopBuseCrys = crystalHand == EnumHand.MAIN_HAND && buseMainHand;
      boolean stopBuseObs = obsidianHand == EnumHand.MAIN_HAND && buseMainHand;
      boolean placeObsidian = this.PlaceObsidian.getBool();
      boolean hasAttackDelay = delaysActions[1] == 0.0F || timers[2].hasReached((double)delaysActions[1]);
      boolean hasPlaceObsDelay = delaysActions[0] == 0.0F || timers[1].hasReached((double)delaysActions[0]);
      boolean hasPlaceCrysDelay = delaysActions[0] == 0.0F || timers[0].hasReached((double)delaysActions[0]);
      boolean replacingCrys = this.ReplaceCrystalOnHit.getBool();
      boolean isImpossibleMode = this.ProcessRateLevel.getMode().equalsIgnoreCase("Impossible");
      boolean isAdobeHeadTrap = this.isAdobeHeadTrap();
      if (!listIsEmptyOrNull(this.getTargets()) && this.canUseCrystalFieldNow()) {
         if (crystal != null && hasAttackDelay) {
            this.breakCrystal(crystal, crystalHand != null ? crystalHand : EnumHand.MAIN_HAND, setDeadCrystal, timers[2], teleportAction, replacingCrys);
            crystal = null;
            if (isImpossibleMode) {
               this.updateCrystals(this.getRanges(teleportAction), !this.canUseCrystalFieldNow(), isAdobeHeadTrap);
            }
         }

         List<BlockPos> outObsidianPoses = new ArrayList<>();
         List<BlockPos> outCrystalPoses = new ArrayList<>();
         if (isImpossibleMode) {
            int countLimitSameTime = 4;
            if (countLimitSameTime == Integer.MAX_VALUE) {
               outCrystalPoses.addAll(this.positionsCrys);
               outObsidianPoses.addAll(this.positionsObs);
            } else {
               for (BlockPos forCrystalPos : this.positionsCrys) {
                  if (outCrystalPoses.size() < countLimitSameTime) {
                     outCrystalPoses.add(forCrystalPos);
                  }
               }

               for (BlockPos forObsidianPos : this.positionsObs) {
                  if (outObsidianPoses.size() < countLimitSameTime) {
                     outObsidianPoses.add(forObsidianPos);
                  }
               }
            }
         } else {
            if (CrystalField.forCrystalPos != null) {
               outCrystalPoses.add(CrystalField.forCrystalPos);
            }

            if (CrystalField.forObsidianPos != null) {
               outObsidianPoses.add(CrystalField.forObsidianPos);
            }
         }

         if (hasPlaceCrysDelay && this.haveItem(this.itemCrystal(), canUseInventory) && !stopBuseCrys) {
            for (BlockPos targetCrystalPos : outCrystalPoses) {
               if (targetCrystalPos != null) {
                  this.placeCrystal(targetCrystalPos, crystalHand, this.switchIsSilent(), timers[0], teleportAction, canUseInventory);
               }
            }
         }

         if (hasPlaceObsDelay
            && this.haveItem(this.itemCrystal(), canUseInventory)
            && this.haveItem(this.itemObsidian(), canUseInventory)
            && !stopBuseObs
            && placeObsidian) {
            for (BlockPos targetObsidianPos : outObsidianPoses) {
               if (targetObsidianPos != null) {
                  this.placeObsidian(targetObsidianPos, obsidianHand, this.switchIsSilent(), timers[1], teleportAction, canUseInventory);
               }
            }
         }
      } else {
         crystal = null;
      }
   }

   private void drawObsidianPosESP(BlockPos pos, int color) {
      AxisAlignedBB aabb = new AxisAlignedBB(
         (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)pos.getX() + 1.0, (double)pos.getY() + 1.0, (double)pos.getZ() + 1.0
      );
      int col2 = ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) / 3.0F);
      RenderUtils.drawCanisterBox(aabb, true, true, true, color, color, col2);
   }

   private void drawCrystalPosESP(BlockPos pos, int color) {
      AxisAlignedBB aabb = new AxisAlignedBB(
         (double)pos.getX(), (double)pos.getY() + 1.0, (double)pos.getZ(), (double)pos.getX() + 1.0, (double)pos.getY() + 1.025, (double)pos.getZ() + 1.0
      );
      int col1 = ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) / 2.0F);
      int col2 = ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) / 8.0F);
      RenderUtils.drawCanisterBox(aabb, true, false, true, col1, 0, col2);
   }

   private void drawAllPopEffects() {
      int i = 0;
      float aPC = this.stateAnim.getAnim();

      for (CrystalField.PopEffect effect : this.hitPops) {
         this.drawPopEffect(effect, ClientColors.getColor1(i, aPC));
         i += 60;
      }
   }

   @Override
   public void alwaysRender3D() {
      this.stateAnim.to = this.actived ? 1.0F : 0.0F;
      if (!((double)this.stateAnim.getAnim() < 0.03)) {
         this.drawPointTP();
         if (!listIsEmptyOrNull(this.hitPops)) {
            RenderUtils.setup3dForBlockPos(() -> this.drawAllPopEffects(), false);
         }

         if (forObsidianPos != null || forCrystalPos != null || Minecraft.player != null) {
            RenderUtils.setup3dForBlockPos(() -> {
               if (forObsidianPos != null) {
                  this.drawObsidianPosESP(forObsidianPos, ColorUtils.getColor(80, 0, 255, 95));
               }

               if (forCrystalPos != null) {
                  this.drawCrystalPosESP(forCrystalPos, ColorUtils.getColor(245, 180, 255));
               }
            }, true);
         }
      }
   }

   private boolean canSpawnPopEffect() {
      return forCrystalPos != null;
   }

   private final void popsEffRemoveAuto() {
      if (!listIsEmptyOrNull(this.hitPops)) {
         this.hitPops.removeIf(effect -> effect != null && effect.getDeltaTime() >= 1.0F);
      }
   }

   private float getPopsMaxTime() {
      return 1150.0F;
   }

   private void addPopsEffToPos(BlockPos toPos) {
      Vec3d pos = new Vec3d(toPos).addVector(0.5, 0.0, 0.5);
      this.hitPops.add(new CrystalField.PopEffect(this.getPopsMaxTime(), pos));
   }

   private void drawPopEffect(CrystalField.PopEffect effect, int startColor) {
      if (!((double)effect.getDeltaTime() > 1.0)) {
         float lineWidth = 0.001F + 5.999F * (1.0F - effect.getDeltaTime()) * this.stateAnim.anim;
         float aPC = effect.getDeltaTime();
         aPC = aPC > 0.5F ? 1.0F - effect.getDeltaTime() : effect.getDeltaTime();
         aPC *= 3.5F;
         aPC = aPC < 0.0F ? 0.0F : (aPC > 1.0F ? 1.0F : aPC);
         float range = 0.375F * (1.0F - aPC) * (1.0F - effect.getDeltaTime()) + 0.225F;
         int effectColor = ColorUtils.swapAlpha(startColor, (float)ColorUtils.getAlphaFromColor(startColor) * aPC);
         Vec3d vert = effect.getPos().addVector(0.0, (double)effect.getDeltaTime() * 0.3, 0.0);
         int index = 0;
         GL11.glEnable(2848);
         GL11.glHint(3154, 4354);
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
         );
         GL11.glLineWidth(lineWidth);
         RenderUtils.glColor(effectColor);
         GL11.glBegin(2);

         while (index <= 360) {
            float sin = -MathHelper.sin(MathHelper.toRadians((float)index)) * range * (index % 20 == 0 ? 0.9F : 1.0F);
            float cos = MathHelper.cos(MathHelper.toRadians((float)index)) * range * (index % 20 == 0 ? 0.9F : 1.0F);
            GL11.glVertex3d(vert.xCoord + (double)sin, vert.yCoord + (index % 20 == 0 ? 0.03 : 0.0), vert.zCoord + (double)cos);
            index += 10;
         }

         GL11.glEnd();
         int var14 = 0;
         GL11.glLineWidth(lineWidth + 6.0F);
         RenderUtils.glColor(ColorUtils.swapAlpha(effectColor, (float)ColorUtils.getAlphaFromColor(effectColor) / 8.0F));
         GL11.glBegin(2);

         while (var14 <= 360) {
            float sin = -MathHelper.sin(MathHelper.toRadians((float)var14)) * range * (var14 % 20 == 0 ? 0.9F : 1.0F);
            float cos = MathHelper.cos(MathHelper.toRadians((float)var14)) * range * (var14 % 20 == 0 ? 0.9F : 1.0F);
            GL11.glVertex3d(vert.xCoord + (double)sin, vert.yCoord + (var14 % 20 == 0 ? 0.03 : 0.0), vert.zCoord + (double)cos);
            var14 += 10;
         }

         GL11.glEnd();
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
         );
         GL11.glHint(3154, 4352);
         GL11.glDisable(2848);
      }
   }

   void updatePointTP(Vec3d vec, boolean force) {
      if (force) {
         this.pointEffaPC.setAnim(1.0F);
         if (MathUtils.getDifferenceOf((double)this.pointXSmooth.getAnim(), vec.xCoord) > 16.0) {
            this.pointXSmooth.setAnim((float)vec.xCoord);
         }

         if (MathUtils.getDifferenceOf((double)this.pointYSmooth.getAnim(), vec.yCoord) > 16.0) {
            this.pointYSmooth.setAnim((float)vec.yCoord);
         }

         if (MathUtils.getDifferenceOf((double)this.pointZSmooth.getAnim(), vec.zCoord) > 16.0) {
            this.pointZSmooth.setAnim((float)vec.zCoord);
         }
      }

      for (int i = 0; i < 4; i++) {
         if (BlockUtils.blockMaterialIsCurrent(
            new BlockPos((double)this.pointXSmooth.getAnim(), (double)this.pointYSmooth.getAnim(), (double)this.pointZSmooth.getAnim())
         )) {
            this.pointYSmooth.setAnim(this.pointYSmooth.getAnim() + 1.22F);
         }
      }

      if (force) {
         this.pointXSmooth.to = (float)vec.xCoord;
         this.pointYSmooth.to = (float)vec.yCoord;
         this.pointZSmooth.to = (float)vec.zCoord;
      }
   }

   void drawPointTP() {
      if (!((double)this.pointEffaPC.getAnim() < 0.03)) {
         this.updatePointTP(null, false);
         RenderUtils.setup3dForBlockPos(
            () -> {
               int c = ColorUtils.swapAlpha(-1, 150.0F * this.pointEffaPC.getAnim() * this.stateAnim.getAnim());
               float x = this.pointXSmooth.getAnim();
               float y = this.pointYSmooth.getAnim();
               float z = this.pointZSmooth.getAnim();
               float w = this.getMe().width / 2.0F;
               float h = this.getMe().height;
               RenderUtils.drawGradientAlphaBox(
                  new AxisAlignedBB((double)(x - w), (double)y, (double)(z - w), (double)(x + w), (double)(y + h), (double)(z + w)), true, true, c, c
               );
               c = ColorUtils.swapAlpha(c, (float)ColorUtils.getAlphaFromColor(c) / 3.0F);
               RenderUtils.drawCanisterBox(
                  new AxisAlignedBB((double)(x - w), (double)y, (double)(z - w), (double)(x + w), (double)(y + h), (double)(z + w)), true, true, true, c, c, c
               );
            },
            false
         );
      }
   }

   private float[] getRotateToBlockPos(BlockPos pos) {
      return RotationUtil.getNeededFacing(
         new Vec3d((double)pos.getX() + 0.5, (double)((float)pos.getY() + 0.5F), (double)pos.getZ() + 0.5), false, Minecraft.player, false
      );
   }

   private float[] getRotateToVec3d(Vec3d pos) {
      return RotationUtil.getNeededFacing(new Vec3d(pos.xCoord, pos.yCoord, pos.zCoord), true, Minecraft.player, false);
   }

   private boolean canRotate() {
      return this.actived && this.Rotations.getBool() && this.rotatePos() != null && this.getMe() != null;
   }

   private BlockPos rotatePos() {
      return forObsidianPos != null
         ? forObsidianPos
         : (
            forCrystalPos != null
               ? forCrystalPos
               : (crystal != null ? BlockUtils.getEntityBlockPos(crystal) : (this.forMiningPos != null ? this.forMiningPos : null))
         );
   }

   private Vec3d getOverallVec3dOfVec3ds(Vec3d first, Vec3d second, float pc) {
      double dx = (second.xCoord - first.xCoord) * (double)pc;
      double dy = (second.yCoord - first.yCoord) * (double)pc;
      double dz = (second.zCoord - first.zCoord) * (double)pc;
      return first.addVector(dx, dy, dz);
   }

   private void rotateToBlockPos(EventPlayerMotionUpdate event, BlockPos pos, boolean silentMove) {
      if (this.getMe() != null && pos != null) {
         Vec3d toRot = new Vec3d(pos).addVector(0.5, 0.5, 0.5);
         RayTraceResult result = MathUtils.getPointed(new Vector2f(this.getRotateToVec3d(toRot)[0], this.getRotateToVec3d(toRot)[1]), 200.0, 1.0F, true);
         if (crystal != null && result != null && result.entityHit != crystal && !this.callRotateUpOnHitCrystal) {
            Vec3d var10001 = crystal.getBestVec3dOnEntityBox(false);
            Vec3d var10002 = crystal.getPositionVector();
            double var10005 = (double)Minecraft.player.height;
            toRot = this.getOverallVec3dOfVec3ds(
               var10001, var10002.addVector(0.0, this.selfVirtPos().yCoord + var10005 / 1.6 > crystal.posY ? 0.0 : 0.6, 0.0), 0.35F
            );
         } else if (forCrystalPos != null) {
            toRot = new Vec3d(forCrystalPos).addVector(0.5, 0.5, 0.5);
            if (!this.getMe().canEntityBeSeenVec3d(toRot)) {
               toRot = toRot.addVector(0.0, 0.501, 0.0);
            }

            if (!this.getMe().canEntityBeSeenVec3d(toRot)) {
               toRot = toRot.addVector(0.0, -0.501, 0.0);
            }

            if (this.callRotateUpOnHitCrystal) {
               toRot = toRot.addVector(0.0, 1.501, 0.0);
               this.callRotateUpOnHitCrystal = false;
            }
         } else if (pos == forObsidianPos) {
            EnumFacing face = BlockUtils.getPlaceableSide(pos);
            toRot = new Vec3d(pos)
               .addVector(0.5, 0.5, 0.5)
               .addVector((double)face.getFrontOffsetX() * 0.5, (double)face.getFrontOffsetY() * 0.5, (double)face.getFrontOffsetZ() * 0.5);
            this.callRotateUpOnHitCrystal = false;
         } else if (pos == this.forMiningPos) {
            toRot = new Vec3d(pos).addVector(0.5, 0.5, 0.5);
            this.callRotateUpOnHitCrystal = false;
         } else {
            this.callRotateUpOnHitCrystal = false;
         }

         this.lastRotatedVec = toRot;
         if (toRot != null) {
            this.lastRotatedVecNotNulled = this.lastRotatedVec;
         }

         float[] rotate = this.getRotateToVec3d(toRot);
         if (rotate != null) {
            event.setYaw(rotate[0]);
            event.setPitch(rotate[1]);
            this.getMe().rotationYawHead = rotate[0];
            this.getMe().renderYawOffset = rotate[0];
            this.getMe().rotationPitchHead = rotate[1];
            HitAura.get.rotations = rotate;
            boolean test = false;
            if (test) {
               Minecraft.player.rotationYaw = rotate[0];
               Minecraft.player.rotationPitch = rotate[1];
            }

            if (silentMove) {
               this.callYawMoveYaw = rotate[0];
               if (Minecraft.player.toCancelSprintTicks <= 1 && MathUtils.getDifferenceOf(Minecraft.player.rotationYaw, rotate[0]) >= 45.0F) {
                  Minecraft.player.toCancelSprintTicks = 2;
               }
            }
         }
      }
   }

   @EventTarget
   public void onSilentMoveStrafe(EventRotationStrafe event) {
      if (this.callYawMoveYaw != -1.2345679E8F) {
         event.setYaw(Minecraft.player.lastReportedYaw);
      }
   }

   @EventTarget
   public void onSilentMoveJump(EventRotationJump event) {
      if (this.callYawMoveYaw != -1.2345679E8F) {
         event.setYaw(Minecraft.player.lastReportedYaw);
      }
   }

   @EventTarget
   public void onMovementInput(EventMovementInput event) {
      if (this.callYawMoveYaw != -1.2345679E8F) {
         MoveMeHelp.fixDirMove(event, Minecraft.player.lastReportedYaw);
      }
   }

   @EventTarget
   public void onPreUpds(EventPlayerMotionUpdate event) {
      if (this.canRotate()) {
         this.rotateToBlockPos(event, this.rotatePos(), this.RotateMoveSide.getBool());
      } else if (this.callYawMoveYaw != -1.2345679E8F) {
         this.callYawMoveYaw = -1.2345679E8F;
      }
   }

   private void cacheControlOrAdd(BlockPos toCache, boolean cacheIsActive) {
      if (cacheIsActive) {
         if (this.CRYSTAL_PLACE_CACHE.isEmpty() || this.CRYSTAL_PLACE_CACHE.stream().noneMatch(cached -> cached.resetWhileMatch(toCache))) {
            int cacheTicksAlive = 8;
            this.CRYSTAL_PLACE_CACHE.add(new CrystalField.TickCachedBlockPos(toCache, cacheTicksAlive));
         }
      }
   }

   private void updateCacheList(boolean clear) {
      if (clear) {
         if (!this.CRYSTAL_PLACE_CACHE.isEmpty()) {
            this.CRYSTAL_PLACE_CACHE.clear();
         }
      } else {
         if (!this.CRYSTAL_PLACE_CACHE.isEmpty()) {
            this.CRYSTAL_PLACE_CACHE.forEach(CrystalField.TickCachedBlockPos::update);
            this.CRYSTAL_PLACE_CACHE.removeIf(CrystalField.TickCachedBlockPos::removeIf);
         }
      }
   }

   private boolean hasInCacheEntityEnderCrystal(EntityEnderCrystal crystal, boolean cacheIsActive) {
      return !cacheIsActive || this.CRYSTAL_PLACE_CACHE.stream().anyMatch(cache -> cache.equalsPos(new BlockPos(crystal.getPositionVector()).down()));
   }

   private boolean spoofBlocksForRayTrace(BlockPos pos0, Block spoofBlock0, BlockPos pos1, Block spoofBlock1, Supplier<Boolean> insert) {
      if (pos0 != null && spoofBlock0 != null && spoofBlock1 != null && spoofBlock1 != null && insert != null) {
         return false;
      } else {
         IBlockState state0 = mc.world.getBlockState(pos0);
         if (state0 == null) {
            return false;
         } else {
            IBlockState state1 = mc.world.getBlockState(pos1);
            if (state1 == null) {
               return false;
            } else {
               mc.world.notifyBlockUpdate(pos0, state0, spoofBlock0.getDefaultState(), 3);
               mc.world.notifyBlockUpdate(pos1, state1, spoofBlock1.getDefaultState(), 3);
               boolean returning = insert.get();
               mc.world.notifyBlockUpdate(pos0, mc.world.getBlockState(pos0), state0, 3);
               mc.world.notifyBlockUpdate(pos1, mc.world.getBlockState(pos1), state1, 3);
               return returning;
            }
         }
      }
   }

   private boolean spoofBlocksForCheckSeenEntity(BlockPos pos0, int yOffset, Block spoofBlock0, EntityLivingBase seen) {
      if (pos0 != null && spoofBlock0 != null && seen != null) {
         IBlockState state0 = mc.world.getBlockState(pos0);
         if (state0 == null) {
            return false;
         } else {
            mc.world.setBlockState(pos0, spoofBlock0.getDefaultState());
            boolean returning = BlockUtils.canPosBeSeenEntity(pos0.add(0.0, (double)yOffset, 0.0), seen, BlockUtils.bodyElement.LEGS, true);
            mc.world.setBlockState(pos0, state0);
            return returning;
         }
      } else {
         return false;
      }
   }

   private boolean spoofBlocksForCheckSeenEntities2(BlockPos pos0, int yOffset0, Block spoofBlock0, EntityLivingBase seen, EntityLivingBase notSeen) {
      if (pos0 != null && spoofBlock0 != null && seen != null && notSeen != null) {
         IBlockState state0 = mc.world.getBlockState(pos0);
         if (state0 == null) {
            return false;
         } else {
            mc.world.setBlockState(pos0, spoofBlock0.getDefaultState());
            boolean returning = BlockUtils.canPosBeSeenEntity(pos0.add(0.0, (double)yOffset0, 0.0), seen, BlockUtils.bodyElement.LEGS, true)
               && !BlockUtils.canPosBeSeenEntity(pos0.add(0.0, (double)yOffset0, 0.0), notSeen, BlockUtils.bodyElement.LEGS, true);
            mc.world.setBlockState(pos0, state0);
            return returning;
         }
      } else {
         return false;
      }
   }

   private boolean spoofBlocks2ForCheckSeenEntities2(
      BlockPos pos0, Block spoofBlock0, BlockPos pos1, Block spoofBlock1, EntityLivingBase seen, EntityLivingBase notSeen
   ) {
      if (pos0 != null && spoofBlock0 != null && pos1 != null && spoofBlock1 != null && seen != null && notSeen != null) {
         IBlockState state0 = mc.world.getBlockState(pos0);
         if (state0 == null) {
            return false;
         } else {
            IBlockState state1 = mc.world.getBlockState(pos1);
            if (state1 == null) {
               return false;
            } else {
               mc.world.setBlockState(pos0, spoofBlock0.getDefaultState());
               mc.world.setBlockState(pos1, spoofBlock1.getDefaultState());
               boolean returning = BlockUtils.canPosBeSeenEntity(pos0, seen, BlockUtils.bodyElement.LEGS, true)
                  && !BlockUtils.canPosBeSeenEntity(pos0, notSeen, BlockUtils.bodyElement.LEGS, true);
               mc.world.setBlockState(pos0, state0);
               mc.world.setBlockState(pos1, state1);
               return returning;
            }
         }
      } else {
         return false;
      }
   }

   private boolean spoofBlocks2ForCheckSeenEntities2(
      BlockPos pos0, int yOffset0, Block spoofBlock0, BlockPos pos1, Block spoofBlock1, EntityLivingBase seen, EntityLivingBase notSeen
   ) {
      if (pos0 != null && spoofBlock0 != null && pos1 != null && spoofBlock1 != null && seen != null && notSeen != null) {
         IBlockState state0 = mc.world.getBlockState(pos0);
         if (state0 == null) {
            return false;
         } else {
            IBlockState state1 = mc.world.getBlockState(pos1);
            if (state1 == null) {
               return false;
            } else {
               mc.world.setBlockState(pos0, spoofBlock0.getDefaultState());
               mc.world.setBlockState(pos1, spoofBlock1.getDefaultState());
               boolean returning = BlockUtils.canPosBeSeenEntity(pos0.up(yOffset0), seen, BlockUtils.bodyElement.LEGS, true)
                  && !BlockUtils.canPosBeSeenEntity(pos0.up(yOffset0), notSeen, BlockUtils.bodyElement.LEGS, true);
               mc.world.setBlockState(pos0, state0);
               mc.world.setBlockState(pos1, state1);
               return returning;
            }
         }
      } else {
         return false;
      }
   }

   private boolean spoofBlocks2ForCheckSeenEntities2OrElse(
      BlockPos pos0, Block spoofBlock0, BlockPos pos1, Block spoofBlock1, EntityLivingBase seen, EntityLivingBase notSeen
   ) {
      if (pos0 != null && spoofBlock0 != null && pos1 != null && spoofBlock1 != null && seen != null && notSeen != null) {
         IBlockState state0 = mc.world.getBlockState(pos0);
         if (state0 == null) {
            return false;
         } else {
            IBlockState state1 = mc.world.getBlockState(pos1);
            if (state1 == null) {
               return false;
            } else {
               mc.world.setBlockState(pos0, spoofBlock0.getDefaultState());
               mc.world.setBlockState(pos1, spoofBlock1.getDefaultState());
               boolean returning = BlockUtils.canPosBeSeenEntity(pos0, seen, BlockUtils.bodyElement.LEGS, true)
                  || !BlockUtils.canPosBeSeenEntity(pos0, notSeen, BlockUtils.bodyElement.LEGS, true);
               mc.world.setBlockState(pos0, state0);
               mc.world.setBlockState(pos1, state1);
               return returning;
            }
         }
      } else {
         return false;
      }
   }

   private boolean spoofBlocks2ForCheckSeenEntity(BlockPos pos0, Block spoofBlock0, BlockPos pos1, Block spoofBlock1, EntityLivingBase seen) {
      if (pos0 != null && spoofBlock0 != null && pos1 != null && spoofBlock1 != null && seen != null) {
         IBlockState state0 = mc.world.getBlockState(pos0);
         if (state0 == null) {
            return false;
         } else {
            IBlockState state1 = mc.world.getBlockState(pos1);
            if (state1 == null) {
               return false;
            } else {
               mc.world.setBlockState(pos0, spoofBlock0.getDefaultState());
               mc.world.setBlockState(pos1, spoofBlock1.getDefaultState());
               boolean returning = BlockUtils.canPosBeSeenEntity(pos0, seen, BlockUtils.bodyElement.LEGS, true);
               mc.world.setBlockState(pos0, state0);
               mc.world.setBlockState(pos1, state1);
               return returning;
            }
         }
      } else {
         return false;
      }
   }

   private BlockPos getBestTargetSurroundSelfSafetyMiningPos(
      EntityLivingBase target, boolean is1l13lPlusVersion, boolean autoObsidianIsActive, float blockRange, List<BlockPos> sphereGeted, boolean isAdobeHeadTrap
   ) {
      if (target != null && !listIsEmptyOrNull(this.sphere) && !(target.getSpeed() > (double)((1.0F - target.width) * 0.76335883F))) {
         EntityPlayer self = this.getMe();
         if (self == null) {
            return null;
         } else {
            BlockPos targetBlockPos = BlockUtils.getEntityBlockPos(target);
            List<BlockPos> sphereBlackListPositions = new ArrayList<>();
            sphereBlackListPositions.add(targetBlockPos);

            for (EnumFacing face : EnumFacing.VALUES) {
               sphereBlackListPositions.add(targetBlockPos.offset(face));
            }

            List<BlockPos> sphere = new ArrayList<>();
            sphere.addAll(sphereGeted);
            sphere.removeAll(sphereBlackListPositions);
            boolean isAnySurroundTarget = false;
            if (mc.world.isAirBlock(targetBlockPos) && BlockUtils.blockMaterialIsCurrent(targetBlockPos.down())) {
               for (EnumFacing hFace : EnumFacing.HORIZONTALS) {
                  BlockPos offset = targetBlockPos.offset(hFace);
                  if (BlockUtils.blockMaterialIsCurrent(offset) && (autoObsidianIsActive || mc.world.isObsidOrBdBlock(offset.down()))) {
                     isAnySurroundTarget = true;
                     break;
                  }
               }
            }

            List<BlockPos> whitePosesForMining = new ArrayList<>();
            if (isAnySurroundTarget) {
               List<EntityLivingBase> doNotBlowUpEnts = this.doNotBlowUpEnts(this.getRanges(false));

               for (EnumFacing hFacex : EnumFacing.HORIZONTALS) {
                  BlockPos offset = targetBlockPos.offset(hFacex);
                  BlockPos offsetDown = offset.down();
                  if (BlockUtils.blockMaterialIsCurrent(offset)
                     && (autoObsidianIsActive && BlockUtils.canPlaceBlock(offsetDown) || mc.world.isObsidOrBdBlock(offsetDown))
                     && !BlockUtils.isOccupiedByEnt(offset, true)
                     && (is1l13lPlusVersion || mc.world.isAirBlock(offset.up()))) {
                     boolean anyBlowSelfOrFriends = false;

                     for (EntityLivingBase frOrSelf : doNotBlowUpEnts) {
                        if (frOrSelf != null && BlockUtils.canPosBeSeenEntity(offset, frOrSelf, BlockUtils.bodyElement.LEGS, true)) {
                           anyBlowSelfOrFriends = true;
                           break;
                        }
                     }

                     if (!anyBlowSelfOrFriends) {
                        whitePosesForMining.add(offset);
                     }
                  }
               }

               if (whitePosesForMining.isEmpty()) {
                  BlockPos targetDownBlockPos = targetBlockPos.down();

                  for (BlockPos posSphere : sphere) {
                     if (posSphere.getY() == targetBlockPos.getY() || posSphere.getY() == targetDownBlockPos.getY()) {
                        BlockPos posSphereDown = posSphere.down();
                        if (self.getDistanceToBlockPos(posSphereDown) + 0.5 <= 3.9F
                           && target.getDistanceToBlockPos(posSphereDown) + 0.5 <= 3.9F
                           && mc.world.isAirBlock(posSphere)) {
                           BlockPos posSphereUp = posSphere.up();
                           if ((is1l13lPlusVersion || mc.world.isAirBlock(posSphereUp))
                              && (mc.world.isObsidOrBdBlock(posSphereDown) || autoObsidianIsActive && mc.world.isAirBlock(posSphereDown))) {
                              for (EnumFacing hFacexx : EnumFacing.HORIZONTALS) {
                                 BlockPos holeSide = targetBlockPos.offset(hFacexx);
                                 if (holeSide.getDistanceToBlockPos(posSphere) < 5.4F
                                    && mc.world.getBlockState(holeSide).getBlock() != Blocks.BEDROCK
                                    && this.spoofBlocks2ForCheckSeenEntities2(posSphereDown, 1, Blocks.OBSIDIAN, holeSide, Blocks.AIR, target, self)) {
                                    whitePosesForMining.add(holeSide);
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               if (isAdobeHeadTrap && whitePosesForMining.isEmpty()) {
                  BlockPos adobeHeadBlockPos = targetBlockPos.up(2);
                  if (mc.world.getBlockState(adobeHeadBlockPos).getBlock() == Blocks.AIR
                     && mc.world.getBlockState(adobeHeadBlockPos).getBlock() == Blocks.BEDROCK) {
                     return null;
                  }

                  boolean anyCrystalAdobeBlock = false;

                  for (Entity entity : mc.world.getLoadedEntityList()) {
                     if (entity != null && entity instanceof EntityEnderCrystal crystal) {
                        BlockPos crystalDownPos = BlockUtils.getEntityBlockPos(crystal).down();
                        if (crystalDownPos.getX() == adobeHeadBlockPos.getX()
                           && crystalDownPos.getY() == adobeHeadBlockPos.getY()
                           && crystalDownPos.getZ() == adobeHeadBlockPos.getZ()) {
                           anyCrystalAdobeBlock = true;
                           break;
                        }
                     }
                  }

                  if (anyCrystalAdobeBlock && self.getDistanceToBlockPos(adobeHeadBlockPos) + 0.5 <= (double)blockRange) {
                     whitePosesForMining.add(adobeHeadBlockPos);
                  }
               }
            }

            if (whitePosesForMining.isEmpty()) {
               return null;
            } else {
               if (whitePosesForMining.size() > 1) {
                  whitePosesForMining.sort(Comparator.comparing(self::getDistanceToBlockPos));
               }

               return whitePosesForMining.get(0);
            }
         }
      } else {
         return null;
      }
   }

   private void packetMineReset() {
      this.positionsMining.clear();
      if (this.forMiningPos != null && PlayerHelper.get.progressPacket > 0.75) {
         PlayerHelper.get.runPacketBreak(null);
      }

      this.forMiningPos = null;
      this.hasMiningTempCheck = false;
   }

   private void packetMineCallWhileCanMiningAuto(List<EntityLivingBase> targets, boolean callerEnabled) {
      if (callerEnabled && mc.world != null) {
         EntityPlayer self = this.getMe();
         if (self == null) {
            this.packetMineReset();
         } else if (Minecraft.player != null && Minecraft.player.isSneaking()) {
            this.packetMineReset();
         } else {
            boolean packetMineIsActive = this.isSyncedWithPacketMine();
            if (!packetMineIsActive) {
               this.packetMineReset();
            } else {
               float blockRange = this.getAuraRanges()[0];
               if (this.forMiningPos != null) {
                  if (mc.world.isAirBlock(this.forMiningPos)) {
                     BlockPos downFromMined = this.forMiningPos.down();
                     if (mc.world.isObsidOrBdBlock(downFromMined) && !BlockUtils.isOccupiedByEnt(this.forMiningPos, true)) {
                        forCrystalPos = downFromMined;
                     }

                     this.sleepUpdatePlaceObsAdobeHeadTicks = 2;
                     this.sleepUpdatePlaceCrysAdobeHeadTicks = 1;
                     this.sleepUpdateBreakCrysAdobeHeadTicks = 0;
                     PlayerHelper.get.ticksSleepRepeating = 2;
                     this.packetMineReset();
                     return;
                  }

                  if (self.getDistanceAtEye(
                        (double)this.forMiningPos.getX() + 0.5, (double)this.forMiningPos.getY() + 0.5, (double)this.forMiningPos.getZ() + 0.5
                     )
                     > (double)blockRange) {
                     this.packetMineReset();
                  }
               }

               if (this.hasMiningTempCheck) {
                  if (PlayerHelper.get.progressPacket == 0.0) {
                     this.packetMineReset();
                  }
               } else {
                  if (PlayerHelper.get.progressPacket > 0.01F) {
                     this.hasMiningTempCheck = true;
                  }

                  boolean is1l13lPlusVersion = this.is1l13lPlusVersion();
                  boolean canAutoObsidianPlaceCheck = this.PlaceObsidian.getBool() && this.haveItem(this.itemObsidian(), this.UseInventory.getBool());
                  boolean isAdobeHeadTrap = this.isAdobeHeadTrap();

                  for (EntityLivingBase target : targets) {
                     BlockPos targetMinePos = this.getBestTargetSurroundSelfSafetyMiningPos(
                        target, is1l13lPlusVersion, canAutoObsidianPlaceCheck, blockRange, this.sphere, isAdobeHeadTrap
                     );
                     if (targetMinePos != null
                        && (
                           this.positionsMining.isEmpty()
                              || this.positionsMining
                                 .stream()
                                 .noneMatch(
                                    pos -> pos.getX() == targetMinePos.getX() && pos.getY() == targetMinePos.getY() && pos.getZ() == targetMinePos.getZ()
                                 )
                        )) {
                        this.positionsMining.add(targetMinePos);
                     }
                  }

                  if (!this.positionsMining.isEmpty()) {
                     this.positionsMining
                        .sort(Comparator.comparing(posMine -> self.getDistanceAtEye((double)posMine.getX(), (double)posMine.getY(), (double)posMine.getZ())));
                     this.forMiningPos = this.positionsMining.get(0);
                  }

                  if (this.forMiningPos != null) {
                     ItemStack currentItemStack = PlayerHelper.get.getBestStack(this.forMiningPos, PlayerHelper.get.ToolSwapsInInv.getBool());
                     if (currentItemStack == null && self.inventory != null) {
                        currentItemStack = self.inventory.getCurrentItem();
                     }

                     double mineTimeMsForBlock = PlayerHelper.get.blockBrokenTime(this.forMiningPos, currentItemStack);
                     if (mineTimeMsForBlock < 5000.0) {
                        if (Minecraft.player.isCreative()) {
                           mc.playerController.clickBlock(this.forMiningPos, EnumFacing.DOWN);
                        } else {
                           PlayerHelper.get.runPacketBreak(this.forMiningPos);
                        }
                     }
                  }
               }
            }
         }
      } else {
         this.packetMineReset();
      }
   }

   private List<BlockPos> positionsAtAABBEntityLivingBase(
      EntityLivingBase baseIn, float predictXZTicks, boolean cancelDown, boolean filterCanPlaceAtBlock, boolean sort
   ) {
      List<BlockPos> positions = new ArrayList<>();
      if (baseIn == null) {
         return positions;
      } else {
         List<BlockPos> positionsAABBAll = new ArrayList<>();
         Vec3d targetPredictXZPos = baseIn.getPositionVector()
            .addVector((baseIn.posX - baseIn.lastTickPosX) * (double)predictXZTicks, 0.12F, (baseIn.posZ - baseIn.lastTickPosZ) * (double)predictXZTicks);
         float width = baseIn.width - 1.0E-6F;
         float height = baseIn.height;
         AxisAlignedBB aabbNew = new AxisAlignedBB(
            targetPredictXZPos.addVector((double)(-width / 2.0F), 0.0, (double)(-width / 2.0F)),
            targetPredictXZPos.addVector((double)(width / 2.0F), (double)height, (double)(width / 2.0F))
         );

         for (int x = (int)aabbNew.minX; x <= (int)aabbNew.maxX; x++) {
            for (int z = (int)aabbNew.minZ; z <= (int)aabbNew.maxZ; z++) {
               for (int y = (int)aabbNew.minY; y <= (int)aabbNew.maxY; y++) {
                  positionsAABBAll.add(new BlockPos(x, y, z));
               }
            }
         }

         List<EnumFacing> faces = new ArrayList<>();
         faces.add(EnumFacing.WEST);
         faces.add(EnumFacing.EAST);
         faces.add(EnumFacing.SOUTH);
         faces.add(EnumFacing.NORTH);
         faces.add(EnumFacing.UP);
         if (!cancelDown) {
            faces.add(EnumFacing.DOWN);
         }

         for (BlockPos aabbCollided : positionsAABBAll) {
            for (EnumFacing face : faces) {
               BlockPos faceOffset = aabbCollided.offset(face);
               boolean breakToNext = false;

               for (BlockPos aabbCollided_T : positionsAABBAll) {
                  if (aabbCollided_T.getX() == faceOffset.getX() && aabbCollided_T.getY() == faceOffset.getY() && aabbCollided_T.getZ() == faceOffset.getZ()) {
                     breakToNext = true;
                     break;
                  }
               }

               boolean cancelCanPlaceCheck = false;
               if (faceOffset.getY() == (int)baseIn.posY + 2 && !BlockUtils.canPlaceBlock(faceOffset)) {
                  for (EnumFacing hFace : EnumFacing.HORIZONTALS) {
                     BlockPos offset = faceOffset.offset(hFace);
                     if (BlockUtils.canPlaceBlock(offset)) {
                        positions.add(offset);
                        cancelCanPlaceCheck = true;
                        break;
                     }
                  }
               }

               if (!breakToNext
                  && (!filterCanPlaceAtBlock || !BlockUtils.isOccupiedByEnt(faceOffset, false) && (cancelCanPlaceCheck || BlockUtils.canPlaceBlock(faceOffset)))
                  )
                {
                  positions.add(faceOffset);
               }
            }
         }

         if (sort && positions.size() > 1) {
            positions.sort(Comparator.comparing(pos -> -baseIn.getDistanceToBlockPos(pos)));
         }

         return positions;
      }
   }

   private List<BlockPos> getPredictedSurroundTargetPositions(EntityLivingBase target, boolean isSurroundAura, float ticksPredict, float[] ranges) {
      List<BlockPos> predictedPositions = new ArrayList<>();
      if (target != null && mc.world != null && isSurroundAura) {
         EntityPlayer self = this.getMe();
         if (self == null) {
            return predictedPositions;
         } else if (!(target.width > 0.8F) && target.posY == (double)((int)target.posY) && !(self.getSmoothDistanceToEntity(target) < 1.0F)) {
            for (BlockPos surroundPos : this.positionsAtAABBEntityLivingBase(target, ticksPredict, true, true, true)) {
               if (!(
                  self.getDistanceAtEye((double)surroundPos.getX() + 0.5, (double)surroundPos.getY() + 0.5, (double)surroundPos.getZ() + 0.5)
                     > (double)ranges[0]
               )) {
                  predictedPositions.add(surroundPos);
               }
            }

            return predictedPositions;
         } else {
            return predictedPositions;
         }
      } else {
         return null;
      }
   }

   private class PopEffect {
      long time = System.currentTimeMillis();
      float maxTime;
      Vec3d to;

      PopEffect(float maxTime, Vec3d to) {
         this.maxTime = maxTime;
         this.to = to;
      }

      private final float getDeltaTime() {
         return (float)(System.currentTimeMillis() - this.time) / this.maxTime;
      }

      Vec3d getPos() {
         return this.to;
      }
   }

   private class TickCachedBlockPos extends BlockPos {
      private int ticksOut;
      private final int ticksOnInit;
      private boolean removeIf;

      public TickCachedBlockPos(BlockPos pos, int ticksMemory) {
         super(pos.getX(), pos.getY(), pos.getZ());
         this.ticksOnInit = ticksMemory;
         this.ticksOut = this.ticksOnInit;
      }

      public TickCachedBlockPos(int x, int y, int z, int ticksMemory) {
         super(x, y, z);
         this.ticksOnInit = ticksMemory;
         this.ticksOut = this.ticksOnInit;
      }

      public void update() {
         if (!this.removeIf) {
            this.ticksOut--;
            if (this.ticksOut <= 0) {
               this.removeIf = true;
            }
         }
      }

      public boolean equalsPos(BlockPos pos) {
         return pos.getX() == this.getX() && pos.getY() == this.getY() && pos.getZ() == this.getZ();
      }

      public boolean resetWhileMatch(BlockPos pos) {
         if (pos != null && pos.getX() == this.getX() && pos.getY() == this.getY() && pos.getZ() == this.getZ()) {
            this.ticksOut = this.ticksOnInit;
            this.removeIf = false;
            return true;
         } else {
            return false;
         }
      }

      public boolean removeIf() {
         return this.removeIf;
      }
   }
}
