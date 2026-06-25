package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.server.SPacketCloseWindow;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventReceivePacket;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.utils.MusicHelper;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;

public class ChestStealer extends Module {
   public static ChestStealer get;
   final TimerHelper timerLoot = TimerHelper.TimerHelperReseted();
   final TimerHelper timeSeen = TimerHelper.TimerHelperReseted();
   private final Random RANDOM = new Random(1234567890L);
   public boolean toSeen;
   FloatSettings Delay;
   public BoolSettings InstantSteal;
   public BoolSettings SilentWindow;
   public BoolSettings IgnoreCustomItems;
   public BoolSettings RandomSlots;
   public BoolSettings ChestAura;
   public BoolSettings CheckCooldown;
   public BoolSettings HasStealNotify;
   private final ArrayList<ChestStealer.ItemStackWithSlot> stacks = new ArrayList<>();
   public List<Integer> editedSlots = new ArrayList<>();
   public List<ChestStealer.ItemStackWithSlot> memSlots = new ArrayList<>();
   private boolean changed;
   private boolean hasLootProcess;
   List<BlockPos> opennedChestPoses = new ArrayList<>();
   BlockPos targetChestPos;
   BlockPos lastOpennedChest;
   boolean hasFlagOnOpen = false;

   public ChestStealer() {
      super("ChestStealer", 0, Module.Category.PLAYER);
      this.settings.add(this.InstantSteal = new BoolSettings("InstantSteal", false, this));
      this.settings.add(this.Delay = new FloatSettings("Delay", 70.0F, 500.0F, 0.0F, this, () -> !this.InstantSteal.getBool()));
      this.settings.add(this.SilentWindow = new BoolSettings("SilentWindow", true, this, () -> !this.InstantSteal.getBool()));
      this.settings.add(this.IgnoreCustomItems = new BoolSettings("IgnoreCustomItems", true, this));
      this.settings.add(this.RandomSlots = new BoolSettings("RandomSlots", true, this));
      this.settings.add(this.ChestAura = new BoolSettings("ChestAura", false, this));
      this.settings.add(this.CheckCooldown = new BoolSettings("CheckCooldown", false, this));
      this.settings.add(this.HasStealNotify = new BoolSettings("HasStealNotify", true, this, () -> this.InstantSteal.getBool()));
      this.setDemand(0, 1);
      get = this;
   }

   private void playNotify() {
      MusicHelper.playSound("hasstealed.wav", 0.175F);
   }

   @Override
   public String getDisplayName() {
      return this.getDisplayByInt(this.Delay.getInt());
   }

   private boolean itemHasCustom(ItemStack stack) {
      return stack.hasDisplayName();
   }

   private ArrayList<ChestStealer.ItemStackWithSlot> getItemStackListFromContainer(ContainerChest container) {
      this.stacks.clear();
      if (container != null && container.getLowerChestInventory() != null) {
         for (int index = 0; index < container.getLowerChestInventory().getSizeInventory(); index++) {
            if (container.inventorySlots.get(index).getHasStack()) {
               this.stacks.add(new ChestStealer.ItemStackWithSlot(container.inventorySlots.get(index).getStack(), index));
            }
         }
      }

      return this.stacks;
   }

   private boolean hasCustomItemInISWSList(ArrayList<ChestStealer.ItemStackWithSlot> itemStacks) {
      return itemStacks.stream().anyMatch(stackWS -> this.itemHasCustom(stackWS.getStack()));
   }

   private boolean hasEmptySlotInInventory() {
      boolean hasAir = false;

      for (int slotNum = 0; slotNum < 36; slotNum++) {
         if (Minecraft.player.inventory.getStackInSlot(slotNum).getItem() instanceof ItemAir) {
            hasAir = true;
         }
      }

      return hasAir;
   }

   private int getFirstEmptyInvSlot() {
      for (int slotNum = 0; slotNum < 36; slotNum++) {
         if (Minecraft.player.inventory.getStackInSlot(slotNum).getItem() instanceof ItemAir) {
            return slotNum;
         }
      }

      return -1;
   }

   private boolean checkCooldown(ItemStack stack) {
      return !this.CheckCooldown.getBool() && Minecraft.player.getCooldownTracker().getCooldown(stack.getItem(), 1.0F) != 0.0F;
   }

   private boolean lootSlotsFromListStacks(
      List<ChestStealer.ItemStackWithSlot> itemStacks, ContainerChest container, boolean randomSlots, TimerHelper delayController, long delay
   ) {
      if (container != null && container.getLowerChestInventory() != null && !itemStacks.isEmpty()) {
         int currectSlot = randomSlots && itemStacks.size() > 2 ? MathUtils.clamp(this.RANDOM.nextInt(itemStacks.size()), 0, itemStacks.size()) : 0;
         ChestStealer.ItemStackWithSlot currentISWS = itemStacks.get(currectSlot);
         if (itemStacks.get(currectSlot) != null) {
            ItemStack stackInSlot = currentISWS.getStack();
            int slot = currentISWS.getSlot();
            int firstSlot = this.getFirstEmptyInvSlot();
            if (delayController.hasReached((double)delay)) {
               if (this.checkCooldown(stackInSlot) || firstSlot == -1 || !this.getHasLootAction(stackInSlot, slot, container, false)) {
                  return false;
               }

               delayController.reset();
               itemStacks.remove(currectSlot);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public void updateMemory(int stage) {
      switch (stage) {
         case 0:
            this.editedSlots.clear();
            this.memSlots.clear();
            Container slots = (new GuiInventory(Minecraft.player)).inventorySlots;

            for (int slotNumx = 0; slotNumx < slots.inventorySlots.size(); slotNumx++) {
               Slot slot = slots.inventorySlots.get(slotNumx);
               this.memSlots.add(new ChestStealer.ItemStackWithSlot(slot.getStack(), slot.slotNumber));
            }
            break;
         case 1:
            Container slotsx = (new GuiInventory(Minecraft.player)).inventorySlots;

            for (int slotNum = 0; slotNum < slotsx.inventorySlots.size() && !this.memSlots.isEmpty() && this.memSlots.get(slotNum) != null; slotNum++) {
               ChestStealer.ItemStackWithSlot slotStackIndexed = this.memSlots.get(slotNum);
               if (slotStackIndexed.getStack() != slotsx.inventorySlots.get(slotNum).getStack()) {
                  this.editedSlots.add(slotStackIndexed.getSlot());
               }
            }
            break;
         case 2:
            this.editedSlots.clear();
            this.memSlots.clear();
      }
   }

   private boolean speedLootAllSlotsFromListStacks(List<ChestStealer.ItemStackWithSlot> itemStacks, ContainerChest container, boolean randomSlots) {
      if (container != null && container.getLowerChestInventory() != null && !itemStacks.isEmpty()) {
         if (randomSlots) {
            itemStacks = itemStacks.stream().sorted(Comparator.comparingDouble(a -> Math.random())).toList();
         }

         boolean hasLoot = false;

         for (ChestStealer.ItemStackWithSlot currentISWS : itemStacks) {
            if (currentISWS != null) {
               int slot = currentISWS.getSlot();
               ItemStack stackInSlot = currentISWS.getStack();
               int firstSlot = this.getFirstEmptyInvSlot();
               if (firstSlot != -1) {
                  if (!this.checkCooldown(stackInSlot) && this.getHasLootAction(stackInSlot, slot, container, true)) {
                     hasLoot = true;
                     continue;
                  }

                  return false;
               }
               break;
            }
         }

         return hasLoot;
      } else {
         return false;
      }
   }

   private boolean getHasLootAction(ItemStack stackIn, int slotIn, ContainerChest chestIn, boolean rcAttempts) {
      if (stackIn == null) {
         this.changed = true;
         return false;
      } else {
         mc.playerController
            .windowClick(chestIn.windowId, slotIn, rcAttempts && (this.changed = !this.changed) ? 1 : 0, ClickType.QUICK_MOVE, Minecraft.player);
         return true;
      }
   }

   @Override
   public void onUpdate() {
      if (this.toSeen && this.timeSeen.hasReached(1000.0)) {
         if (mc.currentScreen instanceof GuiInventory inventory) {
            if (!ComfortUi.get.isContainerAnim()) {
               Minecraft.player.closeScreen();
            } else {
               inventory.colose = true;
               GuiContainer.inter.to = 0.0F;
               GuiContainer.inter.setAnim(!Panic.stop && ComfortUi.get.isContainerAnim() ? 1.0F : 0.0F);
            }
         }

         this.toSeen = false;
      }

      if (!this.InstantSteal.getBool()) {
         Container opennedContainer = Minecraft.player.openContainer;
         if (opennedContainer != null && opennedContainer instanceof ContainerChest chestContainer) {
            List<ChestStealer.ItemStackWithSlot> iswsList = this.getItemStackListFromContainer(chestContainer);
            List<ChestStealer.ItemStackWithSlot> iswsListFiltered = iswsList.stream()
               .filter(isws -> !this.IgnoreCustomItems.getBool() || !this.itemHasCustom(isws.getStack()))
               .collect(Collectors.toList());
            if (this.hasEmptySlotInInventory() && !iswsListFiltered.isEmpty()) {
               if (this.lootSlotsFromListStacks(iswsListFiltered, chestContainer, this.RandomSlots.getBool(), this.timerLoot, (long)this.Delay.getInt())) {
                  this.hasLootProcess = true;
                  this.updateMemory(1);
                  if (this.SilentWindow.getBool()
                     && mc.currentScreen instanceof GuiContainer
                     && !(mc.currentScreen instanceof GuiInventory)
                     && this.hasLootProcess) {
                     mc.currentScreen = null;
                     mc.setIngameFocus();
                  }
               }
            } else if (this.hasLootProcess || this.timeSeen.hasReached(100.0)) {
               this.hasLootProcess = false;
               Minecraft.player.closeScreen();
               mc.currentScreen = null;
               if (this.HasStealNotify.getBool() && this.InstantSteal.getBool() && !Minecraft.player.isCreative()) {
                  this.playNotify();
                  mc.displayGuiScreen(new GuiInventory(Minecraft.player));
                  mc.setIngameFocus();
                  this.toSeen = true;
                  this.timeSeen.reset();
               }
            }
         } else {
            if (!this.toSeen) {
               this.updateMemory(0);
            }

            this.timerLoot.reset();
         }
      }
   }

   public boolean stackIsLastLooted(Slot slot) {
      return this.editedSlots.stream().anyMatch(slot1 -> slot1.equals(slot.slotNumber));
   }

   public void onStacksUpdate() {
      if (!Panic.stop && this.isActived() && this.InstantSteal.getBool()) {
         Container opennedContainer = Minecraft.player.openContainer;
         this.updateMemory(0);
         if (this.hasEmptySlotInInventory() && opennedContainer instanceof ContainerChest chestContainer) {
            ArrayList<ChestStealer.ItemStackWithSlot> iswsList = this.getItemStackListFromContainer(chestContainer);
            if (iswsList.isEmpty()) {
               return;
            }

            List<ChestStealer.ItemStackWithSlot> iswsListFiltered = iswsList.stream()
               .filter(isws -> !this.IgnoreCustomItems.getBool() || !this.itemHasCustom(isws.getStack()))
               .collect(Collectors.toList());
            if (!iswsListFiltered.isEmpty() && this.speedLootAllSlotsFromListStacks(iswsListFiltered, chestContainer, this.RandomSlots.getBool())) {
               Minecraft.player.closeScreen();
               mc.currentScreen = null;
               this.updateMemory(1);
               if (this.HasStealNotify.getBool()) {
                  this.playNotify();
                  mc.displayGuiScreen(new GuiInventory(Minecraft.player));
                  mc.setIngameFocus();
                  this.toSeen = true;
                  this.timeSeen.reset();
               }
            }
         }
      }
   }

   List<BlockPos> allChestPoses(double inRange) {
      return mc.world
         .getLoadedTileEntityList()
         .stream()
         .filter(tile -> tile instanceof TileEntityChest)
         .filter(tile -> Minecraft.player.getDistanceAtEye((double)tile.getX() + 0.5, (double)tile.getY() + 0.5, (double)tile.getZ() + 0.5) < inRange)
         .map(TileEntity::getPos)
         .filter(pos -> !this.opennedChestPoses.stream().anyMatch(blackPos -> blackPos.equals(pos)))
         .filter(
            pos -> {
               RayTraceResult result = mc.world
                  .rayTraceBlocks(
                     Minecraft.player.getPositionEyes(1.0F),
                     new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5),
                     false,
                     true,
                     false
                  );
               return result != null && result.getBlockPos() != null && result.getBlockPos().equals(pos);
            }
         )
         .collect(Collectors.toList());
   }

   void nullateAll() {
      this.lastOpennedChest = null;
      this.hasFlagOnOpen = false;
      this.targetChestPos = null;
      this.opennedChestPoses.clear();
   }

   @Override
   public void onToggled(boolean actived) {
      this.nullateAll();
      super.onToggled(actived);
   }

   @EventTarget
   public void onUpdateEntitySelf(EventPlayerMotionUpdate event) {
      if (this.ChestAura.getBool()) {
         if (Minecraft.player.ticksExisted < 10) {
            this.nullateAll();
            return;
         }

         if (this.hasFlagOnOpen) {
            if (this.lastOpennedChest != null) {
               for (int i = 0; i < this.opennedChestPoses.size(); i++) {
                  if (this.opennedChestPoses.get(i) != null && this.opennedChestPoses.get(i).equals(this.lastOpennedChest)) {
                     this.opennedChestPoses.remove(i);
                  }
               }
            }

            this.hasFlagOnOpen = false;
         }

         if (!this.hasEmptySlotInInventory()) {
            return;
         }

         List<BlockPos> targetChests = this.allChestPoses(4.6);
         if (targetChests.isEmpty() || HitAura.get.actived && HitAura.TARGET_ROTS == null) {
            this.targetChestPos = null;
         } else {
            this.targetChestPos = targetChests.get(0);
         }

         if (this.targetChestPos != null && Minecraft.player.openContainer instanceof ContainerPlayer && !this.hasLootProcess) {
            float[] rotate = RotationUtil.getNeededFacing(new Vec3d(this.targetChestPos).addVector(0.5, 0.5, 0.5), true, Minecraft.player, false);
            event.setYaw(rotate[0]);
            event.setPitch(rotate[1]);
            float prevYaw = Minecraft.player.rotationYaw;
            float prevPitch = Minecraft.player.rotationPitch;
            Minecraft.player.rotationYaw = rotate[0];
            Minecraft.player.rotationPitch = rotate[1];
            Minecraft.player.renderYawOffset = rotate[0];
            Minecraft.player.rotationYawHead = rotate[0];
            Minecraft.player.rotationPitchHead = rotate[1];
            mc.entityRenderer.getMouseOver(mc.getRenderPartialTicks());
            RayTraceResult result = mc.objectMouseOver;
            Minecraft.player.rotationYaw = prevYaw;
            Minecraft.player.rotationPitch = prevPitch;
            mc.entityRenderer.getMouseOver(mc.getRenderPartialTicks());
            if (result != null && result.getBlockPos() != null) {
               BlockPos pos = result.getBlockPos();
               if (pos.equals(this.targetChestPos)) {
                  boolean canClick = result.hitVec != null && result.sideHit != null;
                  if (canClick) {
                     boolean clientIsSneaking = Minecraft.player.isSneaking();
                     if (clientIsSneaking) {
                        mc.getConnection().sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.STOP_SNEAKING));
                     }

                     if (mc.playerController.processRightClickBlock(Minecraft.player, mc.world, pos, result.sideHit, result.hitVec, EnumHand.MAIN_HAND)
                        == EnumActionResult.SUCCESS) {
                        this.opennedChestPoses.add(pos);
                        this.lastOpennedChest = pos;
                        this.targetChestPos = null;
                     }

                     if (clientIsSneaking) {
                        mc.getConnection().sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_SNEAKING));
                     }
                  }
               }
            }
         }
      } else {
         this.nullateAll();
      }
   }

   @EventTarget
   public void onReceive(EventReceivePacket event) {
      if (event.getPacket() instanceof SPacketCloseWindow close
         && Minecraft.player.openContainer != null
         && close.windowId == Minecraft.player.openContainer.windowId) {
         this.hasFlagOnOpen = true;
      }
   }

   private class ItemStackWithSlot {
      private final ItemStack stack;
      private final int slot;

      public ItemStackWithSlot(ItemStack stack, int slot) {
         this.stack = stack;
         this.slot = slot;
      }

      public ItemStack getStack() {
         return this.stack;
      }

      public int getSlot() {
         return this.slot;
      }
   }
}
