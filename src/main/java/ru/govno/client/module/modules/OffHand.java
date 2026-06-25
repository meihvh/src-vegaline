package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityMinecartTNT;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemEndCrystal;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBed;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventRender2D;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Math.BlockUtils;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.ReplaceStrUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Render.StencilUtil;
import ru.govno.client.utils.Versions.NewPhisicsFixes;

public class OffHand extends Module {
   public static TimerHelper timerDelay = new TimerHelper();
   public static TimerHelper timer3 = new TimerHelper();
   public static TimerHelper timer4 = new TimerHelper();
   public static boolean doTotem;
   public static boolean doBackSlot;
   public static boolean totemBackward;
   public static boolean totemTaken;
   public static boolean callNotSave;
   public static boolean fall;
   public static boolean clientSwap;
   public static Item saveSlot;
   public static Item oldSlot;
   public static Item prevOldSlot;
   public static OffHand get;
   private final BoolSettings CanHotbarSwap;
   private final BoolSettings CanNewVerClicks;
   private final BoolSettings TotemBackward;
   private final BoolSettings ShieldApple;
   private final BoolSettings CrystalApple;
   private final BoolSettings BallApple;
   private final BoolSettings AutoBall;
   private final BoolSettings ShieldBall;
   private final BoolSettings PutBecauseLack;
   private final TimerHelper afterGroundTime = new TimerHelper();
   private float fallDistance;
   private final List<OffHand.AttributeWithValue> attributeWithValues = new ArrayList<>();
   private final List<String> POTION_EFFECT_STRING_NAMES = Arrays.asList("Скорость", "Сопротивление", "Сила", "Огнестойкость", "Спешка");
   private Item curItem = Items.GOLDEN_APPLE;
   private final TimerHelper saveSwapBackTimer = TimerHelper.TimerHelperReseted();
   private boolean crystalappleTrigger;
   private boolean ballshieldTrigger;
   private boolean shieldappleTrigger;
   private boolean ballappleTrigger;
   private OffHand.AttributeType currentAttributeType;
   private OffHand.AttributeType prevAttributeType;
   public static AnimationUtils scaleAnim = new AnimationUtils(0.0F, 0.0F, 0.07F);
   public static AnimationUtils popAnim = new AnimationUtils(0.0F, 0.0F, 0.03F);

   public OffHand() {
      super("OffHand", 0, Module.Category.PLAYER);
      get = this;
      this.settings.add(this.CanHotbarSwap = new BoolSettings("CanHotbarSwap", true, this));
      this.settings.add(this.CanNewVerClicks = new BoolSettings("Can1_13Clicks", true, this, () -> NewPhisicsFixes.isNewVersion()));
      this.settings.add(this.PutBecauseLack = new BoolSettings("PutBecauseLack", false, this));
      this.settings.add(this.TotemBackward = new BoolSettings("TotemBackward", true, this));
      this.settings.add(this.ShieldApple = new BoolSettings("ShieldApple", true, this));
      this.settings.add(this.CrystalApple = new BoolSettings("CrystalApple", true, this));
      this.settings.add(this.BallApple = new BoolSettings("BallApple", true, this));
      this.settings.add(this.AutoBall = new BoolSettings("AutoBall", true, this));
      this.settings.add(this.ShieldBall = new BoolSettings("ShieldBall", true, this));
      this.setDemand(1, 3);
   }

   private List<Item> acceptableAttributedItems() {
      return this.ballshieldTrigger
         ? Arrays.asList(Items.SKULL, Items.FIRE_CHARGE, Items.MELON, Items.TOTEM, Items.CHORUS_FRUIT_POPPED, Items.PRISMARINE_SHARD, Items.CLAY_BALL)
         : Arrays.asList(
            Items.SKULL, Items.FIRE_CHARGE, Items.MELON, Items.TOTEM, Items.SHIELD, Items.CHORUS_FRUIT_POPPED, Items.PRISMARINE_SHARD, Items.CLAY_BALL
         );
   }

   @EventTarget
   public void onEventUpdate(EventPlayerMotionUpdate event) {
      if (this.actived) {
         if (!event.onGround()) {
            this.afterGroundTime.reset();
         }

         long timeOfGround = this.afterGroundTime.getTime();
         if (timeOfGround > 1000L) {
            this.fallDistance = 0.0F;
         } else {
            if (Minecraft.player.fallDistance != 0.0F) {
               this.fallDistance = Minecraft.player.fallDistance;
            }

            if (timeOfGround == 0L && Minecraft.player.hurtTime == 9) {
               this.fallDistance = 0.0F;
            }
         }
      }
   }

   private void updateEmptyHandFix() {
      if (this.PutBecauseLack.getBool()) {
         if (Minecraft.player.ticksExisted < 10) {
            if (!totemTaken) {
               if (this.getSlotByItem(Items.SKULL) != -1) {
                  oldSlot = Items.SKULL;
               } else if (this.getSlotByItem(Items.GOLDEN_APPLE) != -1) {
                  oldSlot = Items.GOLDEN_APPLE;
               }

               if (Minecraft.player.getHeldItemOffhand().getItem() == Items.TOTEM && !this.stackIsBall(Minecraft.player.getHeldItemOffhand())) {
                  int ballSlot = this.getSlotByItem(Items.SKULL);
                  if (ballSlot != -1) {
                     oldSlot = Minecraft.player.inventory.getStackInSlot(ballSlot).getItem();
                  } else if (this.getSlotByItem(Items.SHIELD) != -1) {
                     oldSlot = Items.SHIELD;
                  } else if (this.getSlotByItem(Items.GOLDEN_APPLE) != -1) {
                     oldSlot = Items.GOLDEN_APPLE;
                  }
               }
            }
         } else {
            ItemStack offStack = Minecraft.player.getHeldItemOffhand();
            if (offStack.getItem() instanceof ItemAir && !totemTaken && !doBackSlot && oldSlot != null && this.getSlotByItem(prevOldSlot) == -1) {
               List<Item> samples = Arrays.asList(Items.POTIONITEM, Items.BOW, Items.SKULL, Items.SHIELD, Items.END_CRYSTAL, Items.GOLDEN_APPLE);
               samples = samples.stream()
                  .filter(
                     sample -> sample != prevOldSlot
                           && (sample != Items.SHIELD || !(Minecraft.player.getCooldownTracker().getCooldown(Items.SHIELD, 0.0F) > 0.0F))
                           && (
                              sample != Items.GOLDEN_APPLE
                                 || (!PlayerHelper.get.actived || !PlayerHelper.checkApple)
                                    && !(Minecraft.player.getCooldownTracker().getCooldown(Items.GOLDEN_APPLE, 0.0F) > 0.0F)
                           )
                           && (
                              sample != Items.POTIONITEM
                                 || Minecraft.player.getActivePotionEffect(MobEffects.REGENERATION) != null && !(Minecraft.player.getAbsorptionAmount() < 1.0F)
                           )
                           && (
                              sample != Items.BOW
                                 || this.getSlotByItem(Items.ARROW) != -1
                                    && this.getSlotByItem(Items.SPECTRAL_ARROW) != -1
                                    && this.getSlotByItem(Items.TIPPED_ARROW) != -1
                           )
                  )
                  .collect(Collectors.toList());
               if (samples.isEmpty()) {
                  return;
               }

               samples = samples.stream().filter(sample -> this.getSlotByItem(sample) != -1).collect(Collectors.toList());
               if (samples.isEmpty()) {
                  return;
               }

               Collections.reverse(samples);
               oldSlot = samples.get(0);
               doBackSlot = true;
            }
         }
      }
   }

   private boolean haveShar() {
      return this.stackIsBall(Minecraft.player.getHeldItemOffhand()) || this.stackIsBall(Minecraft.player.getItemStackFromSlot(EntityEquipmentSlot.HEAD));
   }

   private float smartTriggerHP() {
      float hp = 4.25F;
      EntityPlayer p = getMe();
      float absorbDT = 1.0F;

      for (int i = 0; i < 4; i++) {
         if (!BlockUtils.isArmor(p, BlockUtils.armorElementByInt(i))) {
            hp += 2.6F;
            absorbDT += 0.5F;
         }
      }

      if (p.getActivePotionEffect(Potion.getPotionById(10)) != null) {
         hp -= 0.5F;
      }

      if (p.getActivePotionEffect(Potion.getPotionById(11)) != null) {
         hp -= 0.2F * Math.min((float)(p.getActivePotionEffect(Potion.getPotionById(11)).getAmplifier() + 2), 4.0F);
      }

      if (p.isHandActive() && p.getActiveItemStack().getItem() instanceof ItemAppleGold && p.getItemInUseMaxCount() > 25 && !totemTaken) {
         hp--;
      }

      if (p.getAbsorptionAmount() > 0.0F) {
         hp -= p.getAbsorptionAmount() / MathUtils.clamp(absorbDT, 1.0F, 2.0F);
      }

      if (p.inventory.armorItemInSlot(2).getItem() instanceof ItemElytra) {
         hp += 5.5F;
      }

      return MathUtils.clamp(hp, 0.0F, 20.0F);
   }

   public static boolean crystalWarn(float isInRange) {
      if (mc.world != null && mc.world.getDifficulty().getDifficultyId() == 0) {
         return false;
      } else {
         List<EntityEnderCrystal> enderCrystals = new CopyOnWriteArrayList<>();
         EntityPlayer self = getMe();
         if (mc.world != null) {
            for (Entity e : mc.world.getLoadedEntityList()) {
               if (e != null && e instanceof EntityEnderCrystal) {
                  EntityEnderCrystal crystal = (EntityEnderCrystal)e;
                  if (self.getDistanceToVec3d(crystal.getPositionVector()) < (double)isInRange) {
                     enderCrystals.add(crystal);
                  }
               }
            }
         }

         int balls = 0;

         for (int i = 0; i < 4; i++) {
            if (BlockUtils.isArmor(self, BlockUtils.armorElementByInt(i))) {
               balls++;
            }
         }

         boolean isFullArmor = balls == 4;

         for (EntityEnderCrystal crystal : enderCrystals) {
            if (crystal != null
               && !(self.getDistanceToVec3d(crystal.getPositionVector()) >= (double)isInRange)
               && BlockUtils.canPosBeSeenEntity(BlockUtils.getEntityVec3dPos(crystal), self, BlockUtils.bodyElement.LEGS, true)) {
               float rangePardon = 8.0F - (float)balls / 2.0F;
               boolean pardon = crystal.posY >= getMe().posY + 0.8 && BlockUtils.blockMaterialIsCurrent(BlockUtils.getEntityBlockPos(crystal).down())
                  || self.getDistanceToVec3d(crystal.getPositionVector()) > (double)rangePardon;
               if (!pardon
                  && self.getDistanceToVec3d(crystal.getPositionVector()) > 4.0
                  && self.getHealth() + self.getAbsorptionAmount() > 23.0F
                  && isFullArmor) {
                  pardon = true;
               }

               return !pardon;
            }
         }

         enderCrystals.clear();
         return false;
      }
   }

   private boolean tntWarn(float isInRange) {
      List<Entity> tntS = new CopyOnWriteArrayList<>();
      if (mc.world != null) {
         for (Entity e : mc.world.getLoadedEntityList()) {
            if (e != null && (e instanceof EntityTNTPrimed || e instanceof EntityMinecartTNT) && getMe().getDistanceToEntity(e) < isInRange) {
               tntS.add(e);
            }
         }
      }

      for (Entity tnt : tntS) {
         if (tnt != null && !(getMe().getDistanceToEntity(tnt) >= isInRange)) {
            return BlockUtils.canPosBeSeenEntity(new Vec3d(tnt.posX, tnt.posY + 0.5, tnt.posZ), getMe(), BlockUtils.bodyElement.LEGS, false);
         }
      }

      return false;
   }

   private double getDistanceToTileEntityAtEntity(Entity entity, TileEntity tileEtity) {
      return entity.getDistanceToBlockPos(tileEtity.getPos());
   }

   private boolean bedWarn(float isInRange) {
      if (Minecraft.player.dimension != 0) {
         List<TileEntityBed> bedTiles = new CopyOnWriteArrayList<>();
         if (mc.world != null && Minecraft.player.dimension != 0) {
            for (TileEntity t : mc.world.getLoadedTileEntityList()) {
               if (t != null && t instanceof TileEntityBed && this.getDistanceToTileEntityAtEntity(getMe(), t) < (double)isInRange) {
                  bedTiles.add((TileEntityBed)t);
               }
            }
         }

         for (TileEntityBed bed : bedTiles) {
            if (BlockUtils.canPosBeSeenEntity(
               new Vec3d((double)bed.getPos().getX() + 0.5, (double)bed.getPos().getY() + 0.4, (double)bed.getPos().getZ() + 0.5),
               getMe(),
               BlockUtils.bodyElement.LEGS,
               false
            )) {
               return true;
            }
         }
      }

      return false;
   }

   private double getCollideYPosition(BlockPos pos) {
      double value = (double)(pos.getY() + 1);
      IBlockState state = mc.world.getBlockState(pos);
      AxisAlignedBB aabb = state.getSelectedBoundingBox(mc.world, pos);
      return aabb == null ? value : aabb.maxY;
   }

   private boolean isCollidablePos(BlockPos pos) {
      return !mc.world.getCollisionBoxes(null, new AxisAlignedBB(pos)).isEmpty();
   }

   private boolean isLiquidPos(BlockPos pos) {
      Material material = mc.world.getBlockState(pos).getMaterial();
      return material.isLiquid() && material.getMaterialMapColor() == MapColor.WATER;
   }

   private double presentFallDistance(double appendOnPreY, int ticksPre) {
      EntityPlayer self = getMe();
      double fd = (double)this.fallDistance;
      if (fd > 3.0) {
         double underY = self.posY;
         double posX = self.posX;
         double posY = underY + (self.posY - self.lastTickPosY);
         double posZ = self.posZ;

         for (double y = underY; y > 0.0; y--) {
            BlockPos pos = new BlockPos(posX, y, posZ);
            if (this.isCollidablePos(pos)) {
               BlockPos posUp = pos.up();
               if (this.isLiquidPos(posUp)) {
                  return 0.0;
               }

               underY = this.getCollideYPosition(pos) - 1.0;
               break;
            }
         }

         double groundDiff = Math.abs(posY - underY);
         double fallSpeed = MathUtils.clamp(Minecraft.player.posY - Minecraft.player.lastTickPosY, 0.0, 10.0 * mc.timer.speed) * appendOnPreY;
         fd = !(groundDiff < 10.0) || !(fallSpeed >= groundDiff / (double)ticksPre) && !(groundDiff < 1.0) ? 0.0 : fd + groundDiff;
      }

      return fd;
   }

   private boolean fallWarn() {
      return !Minecraft.player.capabilities.allowFlying
         && !Minecraft.player.capabilities.disableDamage
         && (!Bypass.get.isActived() || !Bypass.get.NoServerGround.getBool() || Minecraft.player == null || Minecraft.player.ticksExisted <= 2)
         && Minecraft.player.fallDistanceIsUnsafe(this.presentFallDistance(10.0, 10), 5.0F);
   }

   private boolean isFallWarning() {
      return fall;
   }

   private void updatefallWarn() {
      int ping = 0;
      if (Minecraft.player != null && Minecraft.player.ticksExisted > 100 && mc.getConnection().getPlayerInfo(Minecraft.player.getUniqueID()) != null) {
         try {
            ping = MathUtils.clamp(mc.getConnection().getPlayerInfo(Minecraft.player.getUniqueID()).getResponseTime(), 0, 1000);
         } catch (Exception var3) {
            System.out.println("Module-OffHand: Vegaline failled check ping");
         }
      }

      if (this.fallWarn()) {
         fall = true;
         timer3.reset();
      } else if (fall && timer3.hasReached((double)Math.min((long)(100 + ping), 345L))) {
         fall = false;
         timer3.reset();
      }
   }

   private boolean healthWarn() {
      float health = this.smartTriggerHP();
      return Minecraft.player.getHealth() <= health;
   }

   private boolean deathWarned() {
      boolean warn = false;
      if (this.getSlotByItem(Item.getItemById(449)) != -1 || Minecraft.player.getHeldItemOffhand().getItem() == Item.getItemById(449)) {
         this.updatefallWarn();
         if (this.healthWarn()) {
            warn = true;
         } else {
            if (crystalWarn(7.2F)) {
               warn = true;
               totemBackward = false;
            }

            if (this.tntWarn(4.87F)) {
               warn = true;
               totemBackward = false;
            }

            if (this.bedWarn(7.92F)) {
               warn = true;
               totemBackward = false;
            }
         }

         this.updatefallWarn();
         if (this.isFallWarning()) {
            warn = true;
         }
      }

      return Minecraft.player.getHeldItemMainhand().getItem() != Items.TOTEM && !Minecraft.player.isCreative() && warn;
   }

   private boolean canUseItemMainHand() {
      return Minecraft.player.getHeldItemMainhand().getItem() instanceof ItemBlock
         && (mc.objectMouseOver.typeOfHit == null || mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK);
   }

   private static EntityPlayer getMe() {
      return (EntityPlayer)(FreeCam.fakePlayer != null && FreeCam.get.actived ? FreeCam.fakePlayer : Minecraft.player);
   }

   @Override
   public String getDisplayName() {
      int count = Minecraft.player == null ? 0 : this.getTotemCount();
      return count > 0 ? this.getDisplayByInt(count) + "T" : this.getName();
   }

   @Override
   public void onUpdate() {
      if (Minecraft.player != null && Minecraft.player.getHealth() != 0.0F && !Minecraft.player.isDead) {
         if (Minecraft.player.ticksExisted == 1) {
            oldSlot = null;
         }

         totemTaken = this.deathWarned() && !totemBackward;
         if (timer4.hasReached(this.TotemBackward.getBool() ? 1300.0 : 170.0)) {
            totemBackward = false;
         }

         if (callNotSave) {
            callNotSave = false;
         } else if ((!Keyboard.isKeyDown(mc.gameSettings.keyBindSwapHands.getKeyCode()) || oldSlot == null)
            && (GuiContainer.draggedStack == null || !(mc.currentScreen instanceof GuiInventory) || !Mouse.isButtonDown(0))) {
            if (Minecraft.player.getHeldItemOffhand().getItem() != Items.air
               && (Minecraft.player.getHeldItemOffhand().getItem() != Items.TOTEM || this.stackIsBall(Minecraft.player.getHeldItemOffhand()))) {
               oldSlot = Minecraft.player.getHeldItemOffhand().getItem();
            }
         } else {
            oldSlot = null;
         }

         this.updateOffHandHelps(this.CrystalApple.getBool(), this.ShieldApple.getBool(), this.BallApple.getBool(), this.ShieldBall.getBool());
         this.updateEmptyHandFix();
         if (this.AutoBall.getBool() && !this.ballshieldTrigger && !this.ballappleTrigger && !totemTaken) {
            this.currentAttributeType = this.getCurrentAttributeType(this.currentAttributeType);
            if (this.stackIsBall(Minecraft.player.getHeldItemOffhand()) && this.acceptableAttributedItems().stream().anyMatch(sample -> sample == oldSlot)) {
               OffHand.ItemStackWithSlot offISWS = new OffHand.ItemStackWithSlot(Minecraft.player.getHeldItemOffhand(), 45);
               List<OffHand.ItemStackWithSlot> iswsList = new ArrayList<>();
               iswsList.add(offISWS);

               for (Item curItem : this.acceptableAttributedItems()) {
                  int curSlot = this.getSlotByItem(curItem);
                  if (curSlot != -1 && curSlot != 45) {
                     OffHand.ItemStackWithSlot check = new OffHand.ItemStackWithSlot(Minecraft.player.inventory.getStackInSlot(curSlot), curSlot);
                     if (this.hasAttributeInStack(check, this.currentAttributeType)) {
                        iswsList.add(check);
                     }
                  }
               }

               if (iswsList.size() > 1) {
                  iswsList = this.getSortedByValuesStacks(iswsList);
                  OffHand.ItemStackWithSlot best = iswsList.get(0);
                  if (best != null && best.getItemStack() != null && best.getItemStack().getItem() != oldSlot) {
                     oldSlot = best.getItemStack().getItem();
                  }
               }
            }
         } else {
            this.currentAttributeType = null;
         }

         if (!totemTaken
            || totemBackward
            || this.getSlotByItem(Items.TOTEM) == -1
               && (Minecraft.player.getHeldItemOffhand().getItem() != Items.TOTEM || this.stackIsBall(Minecraft.player.getHeldItemOffhand()))) {
            if (oldSlot != null && this.getSlotByItem(oldSlot) != -1 && Minecraft.player.getHeldItemOffhand().getItem() != oldSlot) {
               doBackSlot = (
                     !Minecraft.player.isHandActive()
                        || Minecraft.player.getActiveHand() != EnumHand.MAIN_HAND
                        || Minecraft.player.getActiveItemStack().getItem() != oldSlot
                  )
                  && (!totemBackward || this.TotemBackward.getBool());
            } else if (this.AutoBall.getBool()
               && !this.ballshieldTrigger
               && !this.ballappleTrigger
               && this.currentAttributeType != null
               && this.acceptableAttributedItems().stream().anyMatch(sample -> sample == oldSlot)
               && !totemTaken) {
               OffHand.ItemStackWithSlot offStackWithSlot = new OffHand.ItemStackWithSlot(Minecraft.player.getHeldItem(EnumHand.OFF_HAND), 45);
               boolean hasCurrentAttributeInOffStack = this.hasAttributeInStack(offStackWithSlot, this.currentAttributeType);
               boolean hasBetterAttibuteStack = false;
               if (hasCurrentAttributeInOffStack) {
                  int slotGet = this.getSlotByItem(oldSlot);
                  if (slotGet != -1) {
                     OffHand.ItemStackWithSlot iswsGeted = new OffHand.ItemStackWithSlot(
                        Minecraft.player.inventoryContainer.getSlot(slotGet).getStack(), slotGet
                     );
                     if (this.getAttributeValueFromAll(this.getStackAttributes(iswsGeted), this.currentAttributeType)
                        > this.getAttributeValueFromAll(this.getStackAttributes(offStackWithSlot), this.currentAttributeType)) {
                        hasBetterAttibuteStack = true;
                     }
                  }
               }

               if ((!hasCurrentAttributeInOffStack || hasBetterAttibuteStack)
                  && !Minecraft.player.isHandActive()
                  && this.hasAttributeInInventory(this.currentAttributeType)) {
                  doBackSlot = true;
               }
            }
         } else if (Minecraft.player.getHeldItemOffhand().getItem() != Items.TOTEM) {
            doTotem = true;
         }

         this.doItem(45, this.CanHotbarSwap.getBool(), Minecraft.player.hasNewVersionMoves && this.CanNewVerClicks.getBool());
         if (this.AutoBall.getBool()) {
            this.prevAttributeType = this.currentAttributeType;
         }
      }
   }

   private boolean haveItem(Item itemIn) {
      return this.getSlotByItem(itemIn) != -1 || Minecraft.player.inventoryContainer.getSlot(45).getStack().getItem() == itemIn;
   }

   private boolean isBadOver() {
      if (mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null) {
         Block block = mc.world.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock();
         List<Integer> badBlockIDs = Arrays.asList(
            96,
            167,
            54,
            130,
            146,
            58,
            64,
            71,
            193,
            194,
            195,
            196,
            197,
            324,
            330,
            427,
            428,
            429,
            430,
            431,
            154,
            61,
            23,
            158,
            145,
            69,
            107,
            187,
            186,
            185,
            184,
            183,
            107,
            116,
            84,
            356,
            404,
            151,
            25,
            219,
            220,
            221,
            222,
            223,
            224,
            225,
            226,
            227,
            228,
            229,
            230,
            231,
            232,
            233,
            234,
            389,
            379,
            380,
            138,
            321,
            323,
            77,
            143,
            379
         );
         return !Minecraft.player.isSneaking()
            && mc.objectMouseOver != null
            && mc.objectMouseOver.getBlockPos() != null
            && block != null
            && badBlockIDs.stream().anyMatch(id -> Block.getIdFromBlock(block) == id);
      } else {
         return false;
      }
   }

   public boolean stackIsBall(ItemStack stack) {
      return stack != null && stack.getItem() != null && !this.acceptableAttributedItems().stream().noneMatch(sample -> stack.getItem().equals(sample))
         ? this.hasStackAttributes(stack)
         : false;
   }

   private OffHand.AttributeType getAttributeTypeByName(String name) {
      return Arrays.stream(OffHand.AttributeType.values()).filter(attributeType -> name.endsWith(attributeType.getName())).findAny().orElse(null);
   }

   public List<String> getLoresAsStack(ItemStack stack) {
      List<String> list = stack.getTooltip(Minecraft.player, ITooltipFlag.TooltipFlags.NORMAL);

      for (int i = 0; i < list.size(); i++) {
         if (i == 0) {
            list.set(i, stack.getRarity().rarityColor + list.get(i));
         } else {
            list.set(i, TextFormatting.GRAY + list.get(i));
         }
      }

      return list.stream()
         .filter(str -> str.length() > 1)
         .map(str -> new TextComponentString(str).getFormattedText())
         .filter(Objects::nonNull)
         .map(str -> ReplaceStrUtils.fixString(ReplaceStrUtils.deformatString(str, 1)))
         .collect(Collectors.toList());
   }

   private OffHand.AttributeType getAttributeTypeAsPotionName(String potionName) {
      switch (potionName) {
         case "Скорость":
            return OffHand.AttributeType.SPEED_UP;
         case "Сопротивление":
            return OffHand.AttributeType.ARMOR_UP;
         case "Сила":
            return OffHand.AttributeType.DAMAGE_UP;
         case "Спешка":
            return OffHand.AttributeType.COOLDOWN_UP;
         default:
            return null;
      }
   }

   private int getPotionEffectLevelAsRims(String rim) {
      return rim.contains("IV") ? 4 : (rim.contains("V") ? 5 : (rim.contains("III") ? 3 : (rim.contains("II") ? 2 : (rim.contains("(") ? 1 : -1))));
   }

   public boolean hasStackAttributes(ItemStack stack) {
      if (stack == null) {
         return false;
      } else {
         NBTTagCompound nbt = stack.getTagCompound();
         if (nbt != null && nbt.hasKey("AttributeModifiers", 9)) {
            NBTTagList attributeList = nbt.getTagList("AttributeModifiers", 10);
            return Arrays.stream(IntStream.rangeClosed(0, attributeList.tagCount() - 1).toArray())
               .mapToObj(index -> attributeList.getCompoundTagAt(index))
               .anyMatch(compound -> {
                  double value = compound.getDouble("Amount");
                  return value != 0.0 && this.getAttributeTypeByName(compound.getString("AttributeName")) != null;
               });
         } else {
            List<String> loresAsStack = this.getLoresAsStack(stack);
            if (loresAsStack.size() > 3) {
               for (String potionName : this.POTION_EFFECT_STRING_NAMES) {
                  for (String lore : loresAsStack) {
                     if (lore.length() >= 4 && lore.contains(potionName)) {
                        OffHand.AttributeType attributeTypeAsPotion = this.getAttributeTypeAsPotionName(potionName);
                        if (attributeTypeAsPotion != null) {
                           int attributeLevel = this.getPotionEffectLevelAsRims(lore);
                           if (attributeLevel != -1) {
                              return true;
                           }
                        }
                     }
                  }
               }
            }

            return false;
         }
      }
   }

   public List<OffHand.AttributeWithValue> getStackAttributes(OffHand.ItemStackWithSlot stackWithSlot) {
      if (!this.attributeWithValues.isEmpty()) {
         this.attributeWithValues.clear();
      }

      if (stackWithSlot.getItemStack() != null) {
         NBTTagCompound nbt = stackWithSlot.getItemStack().getTagCompound();
         if (nbt != null && nbt.hasKey("AttributeModifiers", 9)) {
            NBTTagList attributeList = nbt.getTagList("AttributeModifiers", 10);
            Arrays.stream(IntStream.rangeClosed(0, attributeList.tagCount() - 1).toArray())
               .mapToObj(index -> attributeList.getCompoundTagAt(index))
               .forEach(compound -> {
                  double value = compound.getDouble("Amount");
                  OffHand.AttributeType attributeType;
                  if (value != 0.0 && (attributeType = this.getAttributeTypeByName(compound.getString("AttributeName"))) != null) {
                     this.attributeWithValues.add(new OffHand.AttributeWithValue(attributeType, value, stackWithSlot));
                  }
               });
         } else {
            List<String> loresAsStack = this.getLoresAsStack(stackWithSlot.getItemStack());
            if (loresAsStack.size() > 3) {
               for (String potionName : this.POTION_EFFECT_STRING_NAMES) {
                  for (String lore : loresAsStack) {
                     if (lore.length() >= 4 && lore.contains(potionName)) {
                        OffHand.AttributeType attributeTypeAsPotion = this.getAttributeTypeAsPotionName(potionName);
                        if (attributeTypeAsPotion != null) {
                           int attributeLevel = this.getPotionEffectLevelAsRims(lore);
                           if (attributeLevel != -1) {
                              this.attributeWithValues.add(new OffHand.AttributeWithValue(attributeTypeAsPotion, (double)attributeLevel, stackWithSlot));
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return this.attributeWithValues;
   }

   public List<OffHand.AttributeWithValue> getStackAttributesWithoutPotions(OffHand.ItemStackWithSlot stackWithSlot) {
      if (!this.attributeWithValues.isEmpty()) {
         this.attributeWithValues.clear();
      }

      if (stackWithSlot.getItemStack() != null) {
         NBTTagCompound nbt = stackWithSlot.getItemStack().getTagCompound();
         if (nbt != null && nbt.hasKey("AttributeModifiers", 9)) {
            NBTTagList attributeList = nbt.getTagList("AttributeModifiers", 10);
            Arrays.stream(IntStream.rangeClosed(0, attributeList.tagCount() - 1).toArray())
               .mapToObj(index -> attributeList.getCompoundTagAt(index))
               .forEach(compound -> {
                  double value = compound.getDouble("Amount");
                  OffHand.AttributeType attributeType;
                  if (value != 0.0 && (attributeType = this.getAttributeTypeByName(compound.getString("AttributeName"))) != null) {
                     this.attributeWithValues.add(new OffHand.AttributeWithValue(attributeType, value, stackWithSlot));
                  }
               });
         }
      }

      return this.attributeWithValues;
   }

   public boolean hasAttributeInStack(OffHand.ItemStackWithSlot stack, OffHand.AttributeType type) {
      return type != null
         && this.getStackAttributes(stack).stream().map(OffHand.AttributeWithValue::getAttributeType).anyMatch(attributeType -> attributeType == type);
   }

   public double getAttributeValueFromAll(List<OffHand.AttributeWithValue> attributesInStack, OffHand.AttributeType currentType) {
      if (!attributesInStack.isEmpty()) {
         attributesInStack = this.getSortedByValues(
            attributesInStack.stream().filter(attributeInStack -> attributeInStack.getAttributeType() == currentType).toList()
         );
         if (!attributesInStack.isEmpty()) {
            return attributesInStack.get(0).getValue();
         }
      }

      return 0.0;
   }

   private boolean hasAttributeInInventory(OffHand.AttributeType attributeType) {
      boolean hasCurrentAttribute = false;

      for (int i = 0; i < Minecraft.player.inventory.getSizeInventory(); i++) {
         try {
            ItemStack itemStack = Minecraft.player.inventory.getStackInSlot(i);
            OffHand.ItemStackWithSlot isws;
            if (this.stackIsBall(itemStack)
               && this.hasAttributeInStack(isws = new OffHand.ItemStackWithSlot(itemStack, i), attributeType)
               && this.getStackAttributes(isws)
                  .stream()
                  .anyMatch(attributeWithValue -> attributeWithValue.getAttributeType() == attributeType && attributeWithValue.getValue() > 0.0)) {
               hasCurrentAttribute = true;
               break;
            }
         } catch (Exception var6) {
            var6.printStackTrace();
         }
      }

      return hasCurrentAttribute;
   }

   private boolean hasAttributeInStack(List<OffHand.AttributeWithValue> attributeWithValues, OffHand.ItemStackWithSlot stack, OffHand.AttributeType type) {
      return type != null
         && attributeWithValues.stream().map(OffHand.AttributeWithValue::getAttributeType).anyMatch(attributeType -> attributeType.equals(type));
   }

   private List<OffHand.AttributeWithValue> getSortedByValues(List<OffHand.AttributeWithValue> attributeWithValues) {
      return attributeWithValues.stream().sorted(Comparator.comparingDouble(OffHand.AttributeWithValue::getReverseValue)).toList();
   }

   private List<OffHand.ItemStackWithSlot> getSortedByValuesStacks(List<OffHand.ItemStackWithSlot> itemStackWithSlots) {
      return itemStackWithSlots.stream()
         .sorted(
            Comparator.comparingDouble(
               stackWithSlot -> this.getStackAttributes(stackWithSlot)
                     .stream()
                     .filter(attr -> attr.getAttributeType() == this.currentAttributeType)
                     .findFirst()
                     .map(OffHand.AttributeWithValue::getReverseValue)
                     .orElse(0.0)
            )
         )
         .toList();
   }

   private void updateOffHandHelps(boolean crystalApple, boolean shieldApple, boolean ballApple, boolean shieldBall) {
      boolean bad = this.isBadOver() || mc.currentScreen != null && !(mc.currentScreen instanceof GuiIngameMenu);
      Item offItem = Minecraft.player.getHeldItemOffhand().getItem();
      Item mainItem = Minecraft.player.getHeldItemMainhand().getItem();
      float shieldCooldown = Minecraft.player.getCooldownTracker().getCooldown(Items.SHIELD, 0.0F);
      boolean pcm = mc.gameSettings.keyBindUseItem.isKeyDown();
      if (pcm) {
         this.saveSwapBackTimer.reset();
      }

      if (!this.saveSwapBackTimer.hasReached(50.0)) {
         pcm = true;
      }

      boolean mainTeadled = Minecraft.player.isHandActive() && Minecraft.player.getActiveHand() == EnumHand.MAIN_HAND
         || mainItem == Items.ENDER_PEARL
         || Minecraft.player.getHeldItemMainhand().getItem() instanceof ItemPotion
         || (mainItem instanceof ItemBlock || mainItem instanceof ItemEndCrystal)
            && mc.objectMouseOver != null
            && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK;
      boolean autoAppleWantsReceiveOffHandApple = AutoApple.get != null && AutoApple.get.isOffHandWantToSupportAutoAppleHook();
      boolean appleCancelSet = PlayerHelper.isCheckApple(5);
      boolean pcmWithAppleRuleCancel = (pcm || autoAppleWantsReceiveOffHandApple) && !appleCancelSet;
      this.crystalappleTrigger = crystalApple
         && (offItem == Items.END_CRYSTAL && this.haveItem(Items.GOLDEN_APPLE) || offItem == Items.GOLDEN_APPLE && this.haveItem(Items.END_CRYSTAL))
         && !this.canUseItemMainHand()
         && mainItem != this.curItem
         && !mainTeadled
         && pcmWithAppleRuleCancel
         && !bad;
      if (this.crystalappleTrigger) {
         this.curItem = Items.GOLDEN_APPLE;
      }

      this.shieldappleTrigger = !this.ballshieldTrigger
         && shieldApple
         && (offItem == Items.SHIELD && this.haveItem(Items.GOLDEN_APPLE) || offItem == Items.GOLDEN_APPLE && this.haveItem(Items.SHIELD))
         && mainItem != this.curItem
         && (
            shieldCooldown >= 0.06F && shieldCooldown <= 1.0F
               || pcm && !EntityLivingBase.isMatrixDamaged && Minecraft.player.getHealth() < Minecraft.player.getMaxHealth() / 1.8F
         )
         && !bad
         && !mainTeadled
         && !(mainItem instanceof ItemFood);
      if (this.shieldappleTrigger) {
         this.curItem = Items.GOLDEN_APPLE;
      }

      this.ballshieldTrigger = !this.shieldappleTrigger
         && !this.crystalappleTrigger
         && shieldBall
         && (offItem == Items.SHIELD || this.haveItem(Items.SHIELD) || this.stackIsBall(Minecraft.player.getHeldItemOffhand()))
         && mainItem != Items.SHIELD
         && (Minecraft.player.getHealth() > Minecraft.player.getMaxHealth() / 2.2F || Minecraft.player.isBlocking() && Minecraft.player.hurtTime == 0)
         && shieldCooldown == 0.0F
         && (EntityLivingBase.isNcpDamaged || pcm || Minecraft.player.isBlocking())
         && pcm
         && !mainTeadled
         && !(mainItem instanceof ItemFood)
         && !bad;
      if (this.ballshieldTrigger) {
         this.curItem = Items.SHIELD;
      }

      boolean ballApplePrio = Minecraft.player.hurtTime > 1 || this.ballappleTrigger || !this.haveItem(Items.SHIELD);
      this.ballappleTrigger = !this.crystalappleTrigger
         && (ballApplePrio || !this.ballshieldTrigger)
         && ballApple
         && (offItem == Items.GOLDEN_APPLE || this.stackIsBall(Minecraft.player.getHeldItemOffhand()))
         && pcmWithAppleRuleCancel
         && this.haveItem(Items.GOLDEN_APPLE)
         && !bad
         && !mainTeadled
         && !(mainItem instanceof ItemFood);
      if (this.ballappleTrigger) {
         if (this.shieldappleTrigger) {
            this.shieldappleTrigger = false;
         }

         this.curItem = Items.GOLDEN_APPLE;
      }

      boolean isTriggered = this.crystalappleTrigger || this.shieldappleTrigger || this.ballshieldTrigger || this.ballappleTrigger;
      boolean cancelTriggerFast = false;
      if (this.ballshieldTrigger
            && !this.ballappleTrigger
            && !this.shieldappleTrigger
            && !this.crystalappleTrigger
            && this.AutoBall.getBool()
            && this.DAMAGE_UP_BALLCHECK()
         || pcm && !pcmWithAppleRuleCancel && (this.shieldappleTrigger || this.ballappleTrigger)) {
         cancelTriggerFast = true;
      }

      boolean resetTrigger = false;
      if (isTriggered) {
         if (saveSlot == null) {
            saveSlot = this.shieldappleTrigger
               ? Items.SHIELD
               : (this.crystalappleTrigger ? Items.END_CRYSTAL : (!this.ballappleTrigger && !this.ballshieldTrigger ? oldSlot : Items.SKULL));
         }

         if (this.getSlotByItem(saveSlot) == -1 && offItem != saveSlot) {
            saveSlot = null;
         }
      } else if (Minecraft.player.getHeldItemOffhand().getItem() == saveSlot) {
         resetTrigger = true;
      }

      if (isTriggered) {
         clientSwap = true;
      } else if (resetTrigger) {
         clientSwap = false;
         saveSlot = null;
      }

      if (cancelTriggerFast && saveSlot != null) {
         oldSlot = saveSlot;
         saveSlot = null;
         this.crystalappleTrigger = false;
         this.ballshieldTrigger = false;
         this.shieldappleTrigger = false;
         this.ballappleTrigger = false;
      }

      if (clientSwap && saveSlot != null) {
         Item i = !isTriggered && (offItem == Items.GOLDEN_APPLE ? !pcmWithAppleRuleCancel : !pcm) ? saveSlot : this.curItem;
         if (Minecraft.player.getHeldItemMainhand().getItem() != i
            && Minecraft.player.getHeldItemOffhand().getItem() != i
            && !Minecraft.player.isHandActive()
            && this.getSlotByItem(i) != -1) {
            oldSlot = i;
         }
      }
   }

   public void invClick(int slotId, boolean pcm) {
      ItemStack itemstack = Minecraft.player.inventoryContainer.slotClick(slotId, !pcm ? 0 : 1, ClickType.PICKUP, Minecraft.player);
      Minecraft.player
         .connection
         .sendPacket(
            new CPacketClickWindow(
               Minecraft.player.inventoryContainer != null ? Minecraft.player.inventoryContainer.windowId : 0,
               slotId,
               !pcm ? 0 : 1,
               ClickType.PICKUP,
               itemstack,
               Minecraft.player.inventoryContainer.getNextTransactionID(Minecraft.player.inventory)
            )
         );
   }

   public void invClick(int slotId, boolean pcm, int ms) {
      mc.playerController
         .windowClickMemory(
            Minecraft.player.inventoryContainer != null ? Minecraft.player.inventoryContainer.windowId : 0,
            slotId,
            !pcm ? 0 : 1,
            ClickType.PICKUP,
            Minecraft.player,
            ms
         );
   }

   public void doItem(int slotIn, boolean canHotbarSwap, boolean canNewClick) {
      if (Minecraft.player.openContainer instanceof ContainerPlayer) {
         int currentItem = doTotem ? this.getSlotByItem(Items.TOTEM) : (doBackSlot ? this.getSlotByItem(oldSlot) : -1);
         if (currentItem < 36 || currentItem > 44) {
            canHotbarSwap = false;
         }

         boolean aac = Bypass.get.isAACWinClick();
         if ((doTotem || doBackSlot)
            && currentItem != -1
            && timerDelay.hasReached(
               canHotbarSwap
                  ? 100.0
                  : (!canNewClick ? 250.0 : (double)(currentItem >= 36 && currentItem <= 44 ? (doTotem ? 150 : 250) : (aac ? 300 : (doTotem ? 150 : 250))))
            )) {
            boolean cancelClicks = !(Minecraft.player.inventoryContainer instanceof ContainerPlayer);
            EntityPlayer me = Minecraft.player;
            if (slotIn == 45 && me.isHandActive() && me.getActiveHand() == EnumHand.OFF_HAND) {
               mc.playerController.onStoppedUsingItem(me);
            }

            int handSlot = me.inventory.currentItem;
            int nextHandSlot = (handSlot + 1) % 9;
            boolean shakeOff = false;
            if (canHotbarSwap && slotIn == 45) {
               ItemStack stackInCurrentSlot = me.inventory.getStackInSlot(currentItem - 36);
               ItemStack stackInSlot = me.inventory.getStackInSlot(slotIn);
               boolean performSync = handSlot != currentItem - 36;
               if (performSync) {
                  mc.getConnection().sendPacket(new CPacketHeldItemChange(currentItem - 36));
               }

               mc.getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.SWAP_HELD_ITEMS, BlockPos.ORIGIN, EnumFacing.DOWN));
               Minecraft.player.setHeldItem(EnumHand.OFF_HAND, stackInCurrentSlot);
               if (performSync) {
                  mc.getConnection().sendPacket(new CPacketHeldItemChange(handSlot));
               }

               if (performSync) {
                  Minecraft.player.inventory.currentItem = currentItem - 36;
               }

               Minecraft.player.setHeldItem(EnumHand.MAIN_HAND, stackInSlot);
               if (performSync) {
                  Minecraft.player.inventory.currentItem = handSlot;
               }

               shakeOff = true;
            } else {
               if (cancelClicks) {
                  return;
               }

               if (canNewClick && slotIn == 45) {
                  boolean isInHotbar = currentItem >= 36 && currentItem <= 44;
                  mc.playerController.windowClick(0, isInHotbar ? 45 : currentItem, isInHotbar ? currentItem - 36 : slotIn - 5, ClickType.SWAP, me);
                  shakeOff = true;
               } else if (aac) {
                  mc.playerController.windowClick(0, currentItem, nextHandSlot, ClickType.SWAP, me);
                  mc.playerController.windowClickMemory(0, slotIn, nextHandSlot, ClickType.SWAP, me, 50);
                  mc.playerController.windowClickMemory(0, currentItem, nextHandSlot, ClickType.SWAP, me, 100);
                  shakeOff = true;
               } else {
                  mc.playerController.windowClick(0, currentItem, nextHandSlot, ClickType.SWAP, me);
                  mc.playerController.windowClick(0, slotIn, nextHandSlot, ClickType.SWAP, me);
                  mc.playerController.windowClick(0, currentItem, nextHandSlot, ClickType.SWAP, me);
               }
            }

            if (shakeOff) {
               me.setHeldItem(EnumHand.OFF_HAND, me.getHeldItemOffhand());
            }

            doBackSlot = false;
            doTotem = false;
            timerDelay.reset();
            callNotSave = true;
         }
      }
   }

   public int getSlotByItem(Item itemIn) {
      List<OffHand.ItemStackWithSlot> inventory = Arrays.stream(
            IntStream.rangeClosed(0, Minecraft.player.inventoryContainer.inventorySlots.size() - 1).toArray()
         )
         .mapToObj(index -> new OffHand.ItemStackWithSlot(Minecraft.player.inventoryContainer.getSlot(index).getStack(), index))
         .filter(Objects::nonNull)
         .toList();
      List<OffHand.ItemStackWithSlot> attributed = new ArrayList<>();
      List<OffHand.ItemStackWithSlot> defaultsPrio = new ArrayList<>();
      List<OffHand.ItemStackWithSlot> defaults = new ArrayList<>();

      for (OffHand.ItemStackWithSlot stackWithSlot : inventory) {
         ItemStack stack = stackWithSlot.getItemStack();
         int slotID = stackWithSlot.getSlot();
         if (slotID != 45) {
            boolean isBallStack = this.stackIsBall(stack);
            if ((
                  stack.getItem() == itemIn
                     || this.AutoBall.getBool()
                        && isBallStack
                        && !this.acceptableAttributedItems().stream().noneMatch(sample -> sample == itemIn)
                        && (itemIn != Items.TOTEM || !totemTaken || totemBackward)
               )
               && (!isBallStack || slotID != 5)) {
               if (isBallStack && slotID != 45 && this.hasAttributeInStack(stackWithSlot, this.currentAttributeType)) {
                  attributed.add(stackWithSlot);
               } else if (stackWithSlot.getItemStack().hasEffect()) {
                  defaultsPrio.add(stackWithSlot);
               } else {
                  defaults.add(stackWithSlot);
               }
            }
         }
      }

      List<OffHand.ItemStackWithSlot> finalInventory = new ArrayList<>();
      if (!attributed.isEmpty()) {
         finalInventory.addAll(this.getSortedByValuesStacks(attributed));
      }

      if (!defaults.isEmpty()) {
         finalInventory.addAll(defaults);
      }

      if (!defaultsPrio.isEmpty()) {
         finalInventory.addAll(defaultsPrio);
      }

      return !finalInventory.isEmpty() && finalInventory.get(0) != null ? finalInventory.get(0).getSlot() : -1;
   }

   private OffHand.AttributeType setCurrentAttribute(OffHand.AttributeType attributeType, OffHand.AttributeType currentAttributeType) {
      if (attributeType != currentAttributeType && this.hasAttributeInInventory(currentAttributeType)) {
         attributeType = currentAttributeType;
      }

      return attributeType;
   }

   private boolean DAMAGE_UP_BALLCHECK() {
      if (HitAura.TARGET_ROTS == null) {
         return false;
      } else {
         boolean preCooled = HitAura.cooldown.hasReached((double)(HitAura.get.msCooldown() - 100.0F))
            && !HitAura.cooldown.hasReached((double)(HitAura.get.msCooldown() - 50.0F));
         int armC = 0;

         for (int i = 0; i < 4; i++) {
            if (BlockUtils.isArmor(Minecraft.player, BlockUtils.armorElementByInt(i))) {
               armC++;
            }
         }

         return Minecraft.player.getHealth() + Minecraft.player.getAbsorptionAmount() >= Minecraft.player.getMaxHealth() / 1.5F
            && armC == 4
            && Minecraft.player.getTotalArmorValue() >= 10
            && preCooled
            && (
               EntityLivingBase.isNcpDamaged && Minecraft.player.hurtTime <= 1 && !(Minecraft.player.getHealth() >= Minecraft.player.getMaxHealth() - 2.0F)
                  ? HitAura.TARGET_ROTS.getHealth() + Minecraft.player.getAbsorptionAmount()
                     < HitAura.TARGET_ROTS.getMaxHealth() / (EntityLivingBase.isNcpDamaged ? 1.5F : 1.25F)
                  : HitAura.TARGET_ROTS.getHealth() < HitAura.TARGET_ROTS.getMaxHealth()
            )
            && this.hasAttributeInInventory(OffHand.AttributeType.DAMAGE_UP);
      }
   }

   private OffHand.AttributeType getCurrentAttributeType(OffHand.AttributeType prevAttributeType) {
      prevAttributeType = this.setCurrentAttribute(prevAttributeType, OffHand.AttributeType.ARMOR_UP);
      boolean skip = true;
      if (HitAura.TARGET_ROTS != null) {
         boolean preCooled = HitAura.cooldown.hasReached((double)(HitAura.get.msCooldown() - 100.0F))
            && !HitAura.cooldown.hasReached((double)HitAura.get.msCooldown());
         int armC = 0;

         for (int i = 0; i < 4; i++) {
            if (BlockUtils.isArmor(Minecraft.player, BlockUtils.armorElementByInt(i))) {
               armC++;
            }
         }

         if (!(Minecraft.player.getHealth() + Minecraft.player.getAbsorptionAmount() >= Minecraft.player.getMaxHealth() / 1.5F)
            || armC != 4
            || Minecraft.player.getTotalArmorValue() < 10
            || !preCooled
            || (
               EntityLivingBase.isNcpDamaged && Minecraft.player.hurtTime <= 1 && !(Minecraft.player.getHealth() >= Minecraft.player.getMaxHealth() - 2.0F)
                  ? !(
                     HitAura.TARGET_ROTS.getHealth() + Minecraft.player.getAbsorptionAmount()
                        < HitAura.TARGET_ROTS.getMaxHealth() / (EntityLivingBase.isNcpDamaged ? 1.5F : 1.25F)
                  )
                  : !(HitAura.TARGET_ROTS.getHealth() < HitAura.TARGET_ROTS.getMaxHealth())
            )) {
            if (Minecraft.player.getHealth() >= 18.0F
               && Criticals.get.isActived()
               && Criticals.get.EntityHit.getBool()
               && !Minecraft.player.isJumping()
               && (EntityLivingBase.isNcpDamaged || !Criticals.get.HitMode.getMode().equalsIgnoreCase("Matrix2"))
               && (!Criticals.get.HitMode.getMode().equalsIgnoreCase("MatrixStand") || Minecraft.player.onGround && MoveMeHelp.getSpeed() == 0.0)) {
               prevAttributeType = this.setCurrentAttribute(prevAttributeType, OffHand.AttributeType.COOLDOWN_UP);
               skip = false;
            }
         } else {
            prevAttributeType = this.setCurrentAttribute(prevAttributeType, OffHand.AttributeType.DAMAGE_UP);
            skip = false;
         }
      }

      if (skip) {
         if (Minecraft.player.getHealth() >= 15.0F
            && (MoveMeHelp.getSpeed() > 0.0 || TargetStrafe.goStrafe() || JesusSpeed.isJesused || MoveMeHelp.isMoving())
            && (
               Timer.get.actived
                  || !Fly.get.isActived()
                     && !ElytraBoost.get.isActived()
                     && mc.world
                        .playerEntities
                        .stream()
                        .map(Entity::getOtherPlayerOf)
                        .filter(Objects::nonNull)
                        .filter(player -> player.isEntityAlive() && !Client.friendManager.isFriend(player.getName()))
                        .noneMatch(player -> (double)Minecraft.player.getDistanceToEntity(player) < 6.1)
            )
            && !Minecraft.player.capabilities.isFlying) {
            prevAttributeType = this.setCurrentAttribute(prevAttributeType, OffHand.AttributeType.SPEED_UP);
         } else if (Velocity.get.isActived()
            && Velocity.get.OnKnockBack.getBool()
            && (
               !Velocity.pass
                  || !Velocity.get.isActived()
                  || !Velocity.get.OnKnockBack.getBool()
                  || !(Minecraft.player.getHealth() + Minecraft.player.getAbsorptionAmount() >= Minecraft.player.getMaxHealth() / 1.4F)
            )) {
            if (Minecraft.player.getHealth() >= Math.min(Minecraft.player.getMaxHealth(), 20.0F)
               && (
                  Minecraft.player.isPotionActive(MobEffects.REGENERATION)
                     || Minecraft.player.getFoodStats().getSaturationLevel() > 5.0F
                     || prevAttributeType == OffHand.AttributeType.HEALTH_UP
                     || Minecraft.player.getHealth() > 22.0F
               )) {
               prevAttributeType = this.setCurrentAttribute(prevAttributeType, OffHand.AttributeType.HEALTH_UP);
            }
         } else {
            prevAttributeType = this.setCurrentAttribute(prevAttributeType, OffHand.AttributeType.ANTI_KNOCKBACK);
         }
      }

      return prevAttributeType;
   }

   @EventTarget
   public void onRender2D(EventRender2D event) {
      int totemCount = this.getTotemCount();
      if (totemTaken || Minecraft.player.isSneaking() && !Minecraft.player.isJumping() && Minecraft.player.onGround && Minecraft.player.isCollidedVertically) {
         scaleAnim.to = 1.05F;
      }

      float scaleAnimVal = scaleAnim.getAnim();
      if (scaleAnimVal > 1.0F) {
         scaleAnim.setAnim(1.0F);
         scaleAnimVal = 1.0F;
      }

      if (scaleAnim.to == 0.0F && (double)scaleAnimVal < 0.1) {
         scaleAnim.setAnim(0.0F);
      }

      float popAnimVal = popAnim.getAnim();
      if (!totemTaken && !(popAnimVal > 0.0F) && scaleAnim.to != 0.0F) {
         scaleAnim.to = 0.0F;
      }

      if (popAnim.to == 0.0F && (double)popAnimVal < 0.03) {
         popAnim.setAnim(0.0F);
      }

      popAnim.speed = 0.02F;
      if (scaleAnimVal != 0.0F) {
         float x = (float)event.getResolution().getScaledWidth() / 2.0F
            + (mc.gameSettings.thirdPersonView != 0 ? -8.0F + 20.0F * AutoApple.get.scaleAnimation.anim : 12.0F);
         float y = (float)event.getResolution().getScaledHeight() / 2.0F - 8.0F;
         x += Crosshair.get.crossPosMotions[0];
         y += Crosshair.get.crossPosMotions[1];
         GL11.glPushMatrix();
         GL11.glDepthMask(false);
         GL11.glEnable(2929);
         GL11.glTranslatef(x, y, 0.0F);
         RenderUtils.customScaledObject2D(0.0F, 0.0F, 16.0F, 16.0F, (float)MathUtils.easeOutBack((double)scaleAnimVal));
         float popAnimPC = 1.0F - popAnimVal;
         if (popAnimVal * 4.0F * 255.0F >= 1.0F) {
            GL11.glPushMatrix();
            RenderUtils.customScaledObject2D(0.0F, 0.0F, 16.0F, 16.0F, popAnimPC);
            RenderUtils.customRotatedObject2D(
               5.0F + popAnimPC * 20.0F, 8.0F - popAnimPC * popAnimPC * popAnimPC * popAnimPC * 8.0F, 0.0F, 0.0F, (double)(-180.0F - popAnimPC * -180.0F)
            );
            GL11.glEnable(3042);
            GL11.glAlphaFunc(516, 0.003921569F);
            Fonts.noise_24
               .drawStringWithShadow(
                  "-1",
                  2.0F + popAnimPC * 20.0F,
                  4.0F - popAnimPC * popAnimPC * popAnimPC * popAnimPC * 8.0F,
                  ColorUtils.getColor(255, 0, 0, MathUtils.clamp(popAnimVal * 4.0F * 255.0F, 1.0F, 255.0F))
               );
            GL11.glAlphaFunc(516, 0.1F);
            GL11.glPopMatrix();
         }

         RenderUtils.customRotatedObject2D(0.0F, 0.0F, 16.0F, 16.0F, (double)(popAnimVal * popAnimVal * popAnimVal * 20.0F));
         RenderUtils.customScaledObject2D(0.0F, 0.0F, 16.0F, 16.0F, 1.0F + popAnimVal * popAnimVal * popAnimVal);
         ItemStack stack = new ItemStack(Items.TOTEM);
         if (popAnimVal != 0.0F) {
            float popAnimValMM = 1.0F - MathUtils.clamp(popAnimVal, 0.0F, 1.0F);
            float popAnimPC2 = ((double)popAnimValMM > 0.5 ? 1.0F - popAnimValMM : popAnimValMM) * 3.0F;
            popAnimPC2 = popAnimPC2 > 1.0F ? 1.0F : popAnimPC2;
            GL11.glPushMatrix();
            StencilUtil.initStencilToWrite();
            GL11.glTranslated((double)(popAnimPC2 * 16.0F), (double)(-popAnimPC2 * 6.0F), 0.0);
            RenderUtils.customScaledObject2D(0.0F, 0.0F, 16.0F, 16.0F, 0.5F + popAnimPC2 * popAnimVal);
            RenderUtils.customRotatedObject2D(0.0F, 0.0F, 16.0F, 16.0F, (double)(270.0F * -popAnimPC * popAnimPC * popAnimPC * popAnimPC * popAnimPC));
            mc.getRenderItem().renderItemIntoGUI(stack, 0, 0);
            StencilUtil.readStencilBuffer(1);
            mc.getRenderItem().renderItemIntoGUI(stack, 0, 0);
            RenderUtils.drawAlphedRect(-24.0, -24.0, 48.0, 48.0, ColorUtils.getColor(255, 255, 255, popAnimVal * 255.0F));
            StencilUtil.uninitStencilBuffer();
            GL11.glPopMatrix();
         }

         if (popAnimVal != 0.0F) {
            GL11.glPushMatrix();
            RenderUtils.customScaledObject2D(0.0F, 0.0F, 16.0F, 16.0F, MathUtils.clamp(popAnimPC * 1.5F, 0.0F, 1.0F));
            mc.getRenderItem().renderItemIntoGUI(stack, 0, 0);
            GL11.glPopMatrix();
         } else {
            mc.getRenderItem().renderItemIntoGUI(stack, 0, 0);
         }

         int c = totemCount == 0
            ? ColorUtils.fadeColor(ColorUtils.getColor(255, 80, 50, 80.0F * scaleAnimVal), ColorUtils.getColor(255, 80, 50, 255.0F * scaleAnimVal), 1.5F)
            : ColorUtils.getColor(255, 255, 255, 255.0F * scaleAnimVal);
         GL11.glEnable(3042);
         GL11.glAlphaFunc(516, 0.003921569F);
         if (ColorUtils.getAlphaFromColor(c) >= 33) {
            (totemCount == 0 ? Fonts.noise_20 : Fonts.mntsb_12)
               .drawStringWithShadow(totemCount + "x", totemCount == 0 ? 14.0F : 12.0F, totemCount == 0 ? 9.0F : 13.5F, c);
         }

         GL11.glAlphaFunc(516, 0.1F);
         GL11.glDepthMask(true);
         GL11.glPopMatrix();
      }
   }

   @Override
   public void onToggled(boolean actived) {
      if (!actived) {
         totemTaken = false;
      }

      super.onToggled(actived);
   }

   private int getTotemCount() {
      int totemCount = 0;

      for (int i = 0; i <= 45; i++) {
         ItemStack is = Minecraft.player.inventoryContainer.getSlot(i).getStack();
         if (is.getItem() == Items.TOTEM) {
            totemCount += is.stackSize;
         }
      }

      return totemCount;
   }

   public static enum AttributeType {
      HEALTH_UP("maxHealth"),
      ANTI_KNOCKBACK("knockbackResistance"),
      DAMAGE_UP("attackDamage"),
      COOLDOWN_UP("attackSpeed"),
      ARMOR_UP("armor"),
      ARMOR_DUR("armorToughness"),
      SPEED_UP("movementSpeed");

      String attributeName;

      private AttributeType(String attributeName) {
         this.attributeName = attributeName;
      }

      String getName() {
         return this.attributeName;
      }
   }

   public class AttributeWithValue {
      private final OffHand.AttributeType attributeType;
      private final double value;
      private final OffHand.ItemStackWithSlot itemStackWithSlot;

      public AttributeWithValue(OffHand.AttributeType attributeType, double value, OffHand.ItemStackWithSlot itemStackWithSlot) {
         this.attributeType = attributeType;
         this.value = value;
         this.itemStackWithSlot = itemStackWithSlot;
      }

      public OffHand.AttributeType getAttributeType() {
         return this.attributeType;
      }

      public double getValue() {
         return this.value;
      }

      public OffHand.ItemStackWithSlot getItemStackWithSlot() {
         return this.itemStackWithSlot;
      }

      public double getReverseValue() {
         return -this.value;
      }
   }

   public static class ItemStackWithSlot {
      private final ItemStack itemStack;
      private final int slot;

      public ItemStackWithSlot(ItemStack itemStack, int slot) {
         this.itemStack = itemStack;
         this.slot = slot;
      }

      public ItemStack getItemStack() {
         return this.itemStack;
      }

      public int getSlot() {
         return this.slot;
      }
   }
}
