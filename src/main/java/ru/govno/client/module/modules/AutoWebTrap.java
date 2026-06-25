package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketCreativeInventoryAction;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.govno.client.Client;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventMovementInput;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventRotationJump;
import ru.govno.client.event.events.EventRotationStrafe;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.BlockUtils;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;

public class AutoWebTrap extends Module {
   BoolSettings Rotations;
   BoolSettings Players;
   BoolSettings Mobs;
   BoolSettings OnlyAuraTarget;
   BoolSettings RotateMoveDir;
   BoolSettings PlaceFromInventory;
   BoolSettings Betonator;
   BoolSettings NoHandSwing;
   BoolSettings CooldownWebPlacing;
   private final TimerHelper delayTimer = TimerHelper.TimerHelperReseted();
   private final TimerHelper lastPlacedTimer = TimerHelper.TimerHelperReseted();
   private final ModeSettings PlaceRate;
   private final FloatSettings WebCooldownMS;
   private BlockPos currentToPlaceWeb;
   private BlockPos currentToPlaceBlock;
   private float[] lastRotated;

   public AutoWebTrap() {
      super("AutoWebTrap", 0, Module.Category.COMBAT);
      this.settings.add(this.Rotations = new BoolSettings("Rotations", true, this));
      this.settings.add(this.RotateMoveDir = new BoolSettings("RotateMoveDir", false, this, () -> this.Rotations.getBool()));
      this.settings.add(this.PlaceFromInventory = new BoolSettings("PlaceFromInventory", true, this));
      this.settings.add(this.OnlyAuraTarget = new BoolSettings("OnlyAuraTarget", true, this));
      this.settings.add(this.Players = new BoolSettings("Players", true, this, () -> !this.OnlyAuraTarget.getBool()));
      this.settings.add(this.Mobs = new BoolSettings("Mobs", false, this, () -> !this.OnlyAuraTarget.getBool()));
      this.settings.add(this.Betonator = new BoolSettings("Betonator", false, this));
      this.settings.add(this.NoHandSwing = new BoolSettings("NoHandSwing", false, this));
      this.settings.add(this.PlaceRate = new ModeSettings("PlaceRate", "Normal", this, new String[]{"Slow", "Normal", "Fast", "Turbo"}));
      this.settings.add(this.CooldownWebPlacing = new BoolSettings("CooldownWebPlacing", false, this));
      this.settings.add(this.WebCooldownMS = new FloatSettings("WebCooldownMS", 3000.0F, 5000.0F, 300.0F, this, () -> this.CooldownWebPlacing.getBool()));
      this.setDemand(0, 2);
   }

   private List<EntityLivingBase> currentEntities() {
      List<EntityLivingBase> players = (List<EntityLivingBase>)(this.OnlyAuraTarget.getBool()
         ? new ArrayList<>()
         : mc.world
            .getLoadedEntityList()
            .stream()
            .map(Entity::getLivingBaseOf)
            .filter(Objects::nonNull)
            .filter(e -> this.Players.getBool() && e instanceof EntityOtherPlayerMP || this.Mobs.getBool() && e instanceof EntityMob)
            .filter(EntityLivingBase::isEntityAlive)
            .filter(e -> e.getDistanceToEntity(Minecraft.player) <= 5.0F)
            .filter(e -> !Client.friendManager.isFriend(e.getName()))
            .filter(e -> !Client.summit(e))
            .collect(Collectors.toList()));
      if (this.OnlyAuraTarget.getBool() && HitAura.TARGET_ROTS != null && HitAura.TARGET_ROTS.hurtTime != 0) {
         players.add(HitAura.TARGET_ROTS);
      } else if (players.size() > 1) {
         players = players.stream().sorted(Comparator.comparingDouble(e -> (double)(-e.getDistanceToEntity(Minecraft.player)))).collect(Collectors.toList());
      }

      return players;
   }

   private boolean haveWebInOffhand() {
      return Minecraft.player.getHeldItemOffhand().getItem() == Item.getItemFromBlock(Blocks.WEB);
   }

   private boolean haveWebInInventory(boolean checkInInv) {
      return this.haveWebInOffhand() || !this.slotIsNan(Item.getItemFromBlock(Blocks.WEB), checkInInv);
   }

   private boolean canReplaceBlock(BlockPos pos) {
      if (pos != null) {
         if (BlockUtils.getBlockMaterial(pos).isReplaceable() && !BlockUtils.getBlockMaterial(pos).isLiquid()) {
            return BlockUtils.getPlaceableSide(pos) != null;
         } else {
            IBlockState state = mc.world.getBlockState(pos);
            return state.getBlockHardness(mc.world, pos) == 0.0F && BlockUtils.getPlaceableSide(pos) != null && !BlockUtils.getBlockMaterial(pos).isLiquid();
         }
      } else {
         return false;
      }
   }

   private List<BlockPos> getNotPosesToPlaceAsSelfAABB(AxisAlignedBB selfAabb) {
      List<BlockPos> blocks = new ArrayList<>();
      double wm = (selfAabb.maxX - selfAabb.minX) / 2.0;
      double h = selfAabb.maxY - selfAabb.minY;
      List<Vec3d> offsets = new ArrayList<>();

      for (int y = 0; (double)y < h; y++) {
         offsets.add(new Vec3d(-wm, (double)y, -wm));
         offsets.add(new Vec3d(wm, (double)y, wm));
         offsets.add(new Vec3d(-wm, (double)y, wm));
         offsets.add(new Vec3d(wm, (double)y, -wm));
      }

      for (Vec3d offset : offsets) {
         BlockPos pos = new BlockPos(
            selfAabb.minX + (selfAabb.maxX - selfAabb.minX) / 2.0 + offset.xCoord,
            selfAabb.minY,
            selfAabb.minZ + (selfAabb.maxZ - selfAabb.minZ) / 2.0 + offset.zCoord
         );
         if (blocks.isEmpty() || blocks.stream().noneMatch(pos1 -> pos1.getX() == pos.getX() && pos1.getY() == pos.getY() && pos1.getZ() == pos.getZ())) {
            blocks.add(pos);
         }
      }

      return blocks;
   }

   private List<BlockPos> getPosesToPlaceAsAABB(AxisAlignedBB aabb, List<BlockPos> notPlacePoses) {
      List<BlockPos> blocks = new ArrayList<>();
      double wm = (aabb.maxX - aabb.minX) / 2.0;
      double h = aabb.maxY - aabb.minY;
      List<Vec3d> offsets = new ArrayList<>();
      offsets.add(new Vec3d(-wm, 0.0, -wm));
      offsets.add(new Vec3d(wm, 0.0, wm));
      offsets.add(new Vec3d(-wm, 0.0, wm));
      offsets.add(new Vec3d(wm, 0.0, -wm));

      for (Vec3d offset : offsets) {
         BlockPos pos = new BlockPos(
            aabb.minX + (aabb.maxX - aabb.minX) / 2.0 + offset.xCoord, aabb.minY, aabb.minZ + (aabb.maxZ - aabb.minZ) / 2.0 + offset.zCoord
         );
         if (this.canReplaceBlock(pos.up())) {
            pos = pos.up();
         } else if (mc.world.getBlockState(pos.up()).getBlock() != Blocks.WEB && mc.world.getBlockState(pos).getBlock() != Blocks.WEB) {
            if (!this.canReplaceBlock(pos) && this.canReplaceBlock(pos.down())) {
               pos = pos.down();
            } else if (!this.canReplaceBlock(pos.down()) && this.canReplaceBlock(pos.down(2))) {
               pos = pos.down(2);
            }
         }

         if (!this.canReplaceBlock(pos) || mc.world.getBlockState(pos.up()).getBlock() == Blocks.WEB) {
            pos = null;
         }

         BlockPos finalPos = pos;
         if (finalPos != null
            && (
               blocks.isEmpty()
                  || blocks.stream().noneMatch(pos1 -> pos1.getX() == finalPos.getX() && pos1.getY() == finalPos.getY() && pos1.getZ() == finalPos.getZ())
            )
            && (
               notPlacePoses.isEmpty()
                  || notPlacePoses.stream()
                     .noneMatch(pos1 -> pos1.getX() == finalPos.getX() && pos1.getY() == finalPos.getY() && pos1.getZ() == finalPos.getZ())
            )
            && this.canReplaceBlock(pos)
            && Minecraft.player.getDistanceToBlockPos(pos) <= 5.0) {
            blocks.add(pos);
         }
      }

      return blocks;
   }

   private List<BlockPos> posesToPlaceWeb(List<EntityLivingBase> targets) {
      List<BlockPos> blocks = new ArrayList<>();
      double predictValue = 3.0;
      List<BlockPos> blPoses = this.getNotPosesToPlaceAsSelfAABB(Minecraft.player.boundingBox);

      for (EntityLivingBase entity : targets) {
         double x = entity.posX + (entity.posX - entity.lastTickPosX) * predictValue * (entity.isInWeb ? 4.0 : 1.0);
         double y = entity.posY + (entity.posY - entity.lastTickPosY) * (predictValue / 2.0) * (entity.isInWeb ? 4.0 : 1.0);
         double z = entity.posZ + (entity.posZ - entity.lastTickPosZ) * predictValue * (entity.isInWeb ? 4.0 : 1.0);
         AxisAlignedBB aabb = new AxisAlignedBB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.4, z + 0.3);
         blocks.addAll(this.getPosesToPlaceAsAABB(aabb, blPoses));
      }

      return blocks;
   }

   private void clickReplace(EnumHand hand, BlockPos pos) {
      boolean sn = Minecraft.player.isSneaking();
      if (!BlockUtils.getBlockMaterial(pos).isReplaceable()) {
         IBlockState state = mc.world.getBlockState(pos);
         if (state.getBlockHardness(mc.world, pos) <= 0.6F && mc.playerController.onPlayerDestroyBlock(pos)) {
            mc.getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, EnumFacing.UP));
            mc.getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, EnumFacing.UP));
         }
      }

      EnumFacing enumFace = BlockUtils.getPlaceableSideSeen(pos, Minecraft.player);
      if (enumFace == null) {
         enumFace = BlockUtils.getPlaceableSide(pos);
         if (enumFace == null) {
            return;
         }
      }

      EnumFacing faceOpposite = enumFace.getOpposite();
      BlockPos offsetPos = pos.offset(enumFace);
      Vec3d facingVec = new Vec3d(offsetPos).addVector(0.5, 0.5, 0.5).add(new Vec3d(faceOpposite.getDirectionVec()).scale(0.5));
      if (!sn) {
         mc.getConnection().sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_SNEAKING));
         Minecraft.player.movementInput.sneak = true;
      }

      mc.playerController.processRightClickBlock(Minecraft.player, mc.world, offsetPos, faceOpposite, facingVec, hand);
      if (!sn) {
         mc.getConnection().sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.STOP_SNEAKING));
         Minecraft.player.movementInput.sneak = false;
      }
   }

   private int getSlotForItem(Item item, boolean canUseInventory) {
      int slot = getItem(item);
      return slot > 8 && !canUseInventory ? -1 : slot;
   }

   private int getSlotForBlockItem(boolean canUseInventory) {
      int slot = getBlockItem(
         Item.getItemFromBlock(Blocks.WEB),
         Item.getItemFromBlock(Blocks.SKULL),
         Item.getItemFromBlock(Blocks.CHEST),
         Item.getItemFromBlock(Blocks.ENDER_CHEST),
         Item.getItemFromBlock(Blocks.TRAPPED_CHEST),
         Item.getItemFromBlock(Blocks.REDSTONE_WIRE),
         Item.getItemFromBlock(Blocks.TORCH),
         Item.getItemFromBlock(Blocks.REDSTONE_TORCH),
         Item.getItemFromBlock(Blocks.RAIL),
         Item.getItemFromBlock(Blocks.REEDS),
         Item.getItemFromBlock(Blocks.NETHER_WART)
      );
      return slot > 8 && !canUseInventory ? -1 : slot;
   }

   public static int getItem(Item designatedItem) {
      for (int i = 0; i < 44; i++) {
         Item item = Minecraft.player.inventory.getStackInSlot(i).getItem();
         if (item.equals(designatedItem)) {
            return i;
         }
      }

      return -1;
   }

   public static int getBlockItem(Item... blackItems) {
      for (int i = 0; i < 44; i++) {
         Item item = Minecraft.player.inventory.getStackInSlot(i).getItem();
         if (item instanceof ItemBlock blockItem && !Arrays.stream(blackItems).anyMatch(item1 -> Item.getIdFromItem(item) == Item.getIdFromItem(item1))) {
            return i;
         }
      }

      return -1;
   }

   private boolean slotIsNan(Item itemIn, boolean canUseInventory) {
      int sl = this.getSlotForItem(itemIn, canUseInventory);
      return sl < 0 || sl > (canUseInventory ? 44 : 8);
   }

   private void switcherForAction(
      EnumHand placeHand, boolean packetSwap, Item swapTo, ItemStack mainStack, Runnable action, boolean useInventory, boolean blockMode, long delayMs
   ) {
      if (swapTo != null) {
         int slotHand = Minecraft.player.inventory.currentItem;
         int slotItem = blockMode ? this.getSlotForBlockItem(false) : this.getSlotForItem(swapTo, false);
         boolean hasInvUse = false;
         if (useInventory && Minecraft.player.openContainer instanceof ContainerPlayer && slotItem == -1 && placeHand == EnumHand.MAIN_HAND) {
            slotItem = blockMode ? this.getSlotForBlockItem(useInventory) : this.getSlotForItem(swapTo, useInventory);
            hasInvUse = true;
         }

         if (!hasInvUse || this.delayTimer.hasReached((double)((float)delayMs * 2.0F))) {
            boolean isInHand = slotHand == slotItem;
            if (placeHand == EnumHand.OFF_HAND) {
               action.run();
            } else if (!this.slotIsNan(swapTo, hasInvUse)) {
               if (slotItem < 9) {
                  boolean slotNan = this.slotIsNan(swapTo, false);
                  boolean picked = false;
                  if (placeHand == EnumHand.MAIN_HAND && !isInHand && !slotNan) {
                     boolean packetSync = Minecraft.player.inventory.currentItem != slotItem;
                     Minecraft.player.inventory.currentItem = slotItem;
                     if (packetSync) {
                        mc.playerController.syncCurrentPlayItem();
                     }

                     picked = true;
                  }

                  if (isInHand || picked) {
                     action.run();
                     this.delayTimer.reset();
                  }

                  if (placeHand == EnumHand.MAIN_HAND && !isInHand && !slotNan && packetSwap) {
                     Minecraft.player.inventory.currentItem = slotHand;
                     mc.playerController.syncCurrentPlayItem();
                  }
               } else if (hasInvUse) {
                  ItemStack stack = Minecraft.player.inventory.getStackInSlot(slotItem);
                  ItemStack prevHandStack = Minecraft.player.getHeldItemMainhand();
                  if (prevHandStack != null && Minecraft.player.isCreative() && Minecraft.player.openContainer instanceof ContainerPlayer) {
                     Minecraft.player.connection.sendPacket(new CPacketCreativeInventoryAction(Minecraft.player.inventory.currentItem + 36, stack));
                     mc.playerController.syncCurrentPlayItem();
                     int slotPrevStack = -1;

                     for (int i = 0; i < 44; i++) {
                        ItemStack geted = Minecraft.player.inventory.getStackInSlot(i);
                        if (geted == prevHandStack) {
                           slotPrevStack = i;
                        }
                     }

                     action.run();
                     this.delayTimer.reset();
                     mc.playerController.windowClick(0, slotPrevStack, Minecraft.player.inventory.currentItem, ClickType.SWAP, Minecraft.player);
                     Minecraft.player.setHeldItem(EnumHand.MAIN_HAND, mainStack);
                     return;
                  }

                  mc.playerController.windowClick(0, slotItem, Minecraft.player.inventory.currentItem, ClickType.SWAP, Minecraft.player);
                  action.run();
                  this.delayTimer.reset();
                  this.lastPlacedTimer.reset();
                  mc.playerController.windowClick(0, slotItem, Minecraft.player.inventory.currentItem, ClickType.SWAP, Minecraft.player);
                  Minecraft.player.setHeldItem(EnumHand.MAIN_HAND, mainStack);
               }
            }
         }
      }
   }

   private void doModuleAction(long delayMs) {
      this.currentToPlaceWeb = null;
      this.currentToPlaceBlock = null;
      if (mc.world != null && Minecraft.player != null && this.delayTimer.hasReached((double)(delayMs - 5L))) {
         if (!this.CooldownWebPlacing.getBool() || this.lastPlacedTimer.hasReached((double)((long)this.WebCooldownMS.getInt() - 5L + 50L))) {
            boolean useInv = this.PlaceFromInventory.getBool();
            boolean haveWeb = this.haveWebInInventory(useInv);
            boolean haveBlocks = this.getSlotForBlockItem(useInv) != -1;
            boolean betonator = this.Betonator.getBool();
            if (haveWeb || haveBlocks && betonator) {
               List<EntityLivingBase> targets = this.currentEntities();
               if (!targets.isEmpty()) {
                  List<BlockPos> posesToWeb = this.posesToPlaceWeb(targets);
                  this.currentToPlaceWeb = posesToWeb.isEmpty() ? null : posesToWeb.get(0);
                  this.currentToPlaceBlock = null;
                  if (this.currentToPlaceWeb != null) {
                     this.currentToPlaceBlock = null;
                  } else if (betonator && haveBlocks) {
                     List<BlockPos> posesToBlock = new ArrayList<>();

                     for (BlockPos pos : BlockUtils.getSphere(Minecraft.player.getPositionEyes(1.0F), 6.0F)
                        .stream()
                        .sorted(Comparator.comparingDouble(obj -> -Minecraft.player.getDistanceToBlockPos(obj)))
                        .toList()) {
                        IBlockState state = mc.world.getBlockState(pos);
                        Block block = state.getBlock();
                        if (block == Blocks.WEB) {
                           BlockPos tempPos = pos;
                            BlockPos finalTempPos = tempPos;
                            BlockPos toPlace = Arrays.stream(EnumFacing.VALUES)
                              .map(face -> finalTempPos.add(face.getDirectionVec()))
                              .filter(BlockUtils::canPlaceBlock)
                              .findAny()
                              .orElse(null);

                           for (int yD = 0; yD < 3 && toPlace == null; yD++) {
                              tempPos = tempPos.down();
                               BlockPos finalTempPos1 = tempPos;
                               toPlace = Arrays.stream(EnumFacing.VALUES)
                                 .map(face -> finalTempPos1.add(face.getDirectionVec()))
                                 .filter(BlockUtils::canPlaceBlock)
                                 .findAny()
                                 .orElse(null);
                           }

                           if (toPlace != null && Minecraft.player.getDistanceToBlockPos(toPlace) <= 5.0) {
                              posesToBlock.add(toPlace);
                           }
                        }
                     }

                     this.currentToPlaceBlock = posesToBlock.isEmpty() ? null : posesToBlock.get(0);
                  }

                  ItemStack mainStack = Minecraft.player.getHeldItemMainhand();
                  BlockPos cur = this.currentToPlaceWeb != null ? this.currentToPlaceWeb : this.currentToPlaceBlock;
                  if (cur != null && Minecraft.player.openContainer instanceof ContainerPlayer) {
                     if (this.haveWebInOffhand() && this.currentToPlaceWeb != null
                        || this.currentToPlaceBlock != null
                           && Minecraft.player.getHeldItemOffhand().getItem() instanceof ItemBlock blockItem
                           && Arrays.stream(
                                 new Item[]{
                                    Item.getItemFromBlock(Blocks.WEB),
                                    Item.getItemFromBlock(Blocks.SKULL),
                                    Item.getItemFromBlock(Blocks.CHEST),
                                    Item.getItemFromBlock(Blocks.ENDER_CHEST),
                                    Item.getItemFromBlock(Blocks.TRAPPED_CHEST),
                                    Item.getItemFromBlock(Blocks.REDSTONE_WIRE),
                                    Item.getItemFromBlock(Blocks.TORCH),
                                    Item.getItemFromBlock(Blocks.REDSTONE_TORCH),
                                    Item.getItemFromBlock(Blocks.RAIL),
                                    Item.getItemFromBlock(Blocks.REEDS),
                                    Item.getItemFromBlock(Blocks.NETHER_WART)
                                 }
                              )
                              .noneMatch(item1 -> Item.getIdFromItem(blockItem) == Item.getIdFromItem(item1))) {
                        this.switcherForAction(EnumHand.OFF_HAND, true, Item.getItemFromBlock(Blocks.WEB), mainStack, () -> {
                           this.clickReplace(EnumHand.OFF_HAND, cur);
                           if (!this.NoHandSwing.getBool()) {
                              Minecraft.player.swingArm(EnumHand.OFF_HAND);
                           }
                        }, useInv, this.currentToPlaceBlock != null, delayMs);
                     } else {
                        this.switcherForAction(EnumHand.MAIN_HAND, true, Item.getItemFromBlock(Blocks.WEB), mainStack, () -> {
                           this.clickReplace(EnumHand.MAIN_HAND, cur);
                           if (!this.NoHandSwing.getBool()) {
                              Minecraft.player.swingArm();
                           }
                        }, useInv, this.currentToPlaceBlock != null, delayMs);
                     }

                     this.lastRotated = null;
                     if (cur != null && this.Rotations.getBool()) {
                        Vec3d posToRotate = new Vec3d(cur).addVector(0.5, 0.5, 0.5);
                        EnumFacing faceRotOff = BlockUtils.getPlaceableSideSeen(cur, Minecraft.player);
                        if (faceRotOff == null) {
                           faceRotOff = BlockUtils.getPlaceableSide(cur);
                        }

                        if (faceRotOff != null) {
                           posToRotate.add(new Vec3d(faceRotOff.getDirectionVec()).scale(0.5));
                        }

                        if (posToRotate != null) {
                           this.lastRotated = RotationUtil.getNeededFacing(posToRotate, true, Minecraft.player, false);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onUpdate() {
      long delay = 100L;
      String var3 = this.PlaceRate.getMode();
      switch (var3) {
         case "Turbo":
            delay = 40L;
            break;
         case "Fast":
            delay = 45L;
            break;
         case "Normal":
            delay = 100L;
            break;
         case "Slow":
            delay = 150L;
      }

      this.doModuleAction(delay);
   }

   @Override
   public void onRender2D(ScaledResolution sr) {
      if (this.PlaceRate.getMode().equalsIgnoreCase("Turbo")) {
         this.doModuleAction(33L);
      }
   }

   @EventTarget
   public void onPlayerUpdate(EventPlayerMotionUpdate event) {
      if (this.lastRotated != null) {
         Minecraft.player.rotationYawHead = this.lastRotated[0];
         Minecraft.player.renderYawOffset = this.lastRotated[0];
         Minecraft.player.rotationPitchHead = this.lastRotated[1];
         HitAura.get.rotations = new float[]{this.lastRotated[0], this.lastRotated[1]};
         if (this.RotateMoveDir.getBool()
            && Minecraft.player.toCancelSprintTicks <= 1
            && MathUtils.getDifferenceOf(Minecraft.player.rotationYaw, this.lastRotated[0]) >= 45.0F) {
            Minecraft.player.toCancelSprintTicks = 2;
         }

         event.setYaw(this.lastRotated[0]);
         event.setPitch(this.lastRotated[1]);
         this.lastRotated = null;
      }
   }

   @EventTarget
   public void onMovementInput(EventMovementInput event) {
      if (this.lastRotated != null && this.RotateMoveDir.getBool()) {
         MoveMeHelp.fixDirMove(event, this.lastRotated[0]);
      }
   }

   @EventTarget
   public void onSilentStrafe(EventRotationStrafe event) {
      if (this.lastRotated != null && this.RotateMoveDir.getBool()) {
         event.setYaw(this.lastRotated[0]);
      }
   }

   @EventTarget
   public void onSilentJump(EventRotationJump event) {
      if (this.lastRotated != null && this.RotateMoveDir.getBool()) {
         event.setYaw(this.lastRotated[0]);
      }
   }
}
