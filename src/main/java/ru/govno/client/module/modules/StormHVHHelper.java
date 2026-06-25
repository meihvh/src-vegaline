package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiBossOverlay;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemEnderEye;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemRedstone;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayerAbilities;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.BossInfo;
import ru.govno.client.Client;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventSendPacket;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;

public class StormHVHHelper extends Module {
   public static StormHVHHelper get;
   BoolSettings AutoDuel;
   BoolSettings AutoResellDuel;
   BoolSettings OnlySneakDuel;
   BoolSettings SmartDuel;
   BoolSettings AutoKitSelect;
   BoolSettings NoPlayersOnSpawn;
   BoolSettings NoSpectate;
   BoolSettings AllowRtpType;
   BoolSettings AutoHeal;
   BoolSettings AutoFixall;
   BoolSettings AutoFlyMode;
   BoolSettings AutoNear;
   BoolSettings AutoStrafeSet;
   BoolSettings AbuseFlyMode;
   ModeSettings AutoDuelMode;
   ModeSettings DuelTypeMenu;
   ModeSettings DuelTypeSend;
   ModeSettings PlayersFindMode;
   ModeSettings KitSelect;
   private int lastRandom;
   private final TimerHelper timerSendDuel = TimerHelper.TimerHelperReseted();
   private static final List<StormHVHHelper.BPWID> verifyBlocks = Arrays.asList(
      new StormHVHHelper.BPWID(new BlockPos(-229, 91, -30), 61),
      new StormHVHHelper.BPWID(new BlockPos(-229, 94, 9), 44),
      new StormHVHHelper.BPWID(new BlockPos(-275, 84, -24), 2),
      new StormHVHHelper.BPWID(new BlockPos(-271, -47, -47), 170),
      new StormHVHHelper.BPWID(new BlockPos(-251, 84, -80), 38),
      new StormHVHHelper.BPWID(new BlockPos(-194, 87, -55), 17),
      new StormHVHHelper.BPWID(new BlockPos(-197, 88, 14), 3),
      new StormHVHHelper.BPWID(new BlockPos(172, 85, -7), 31),
      new StormHVHHelper.BPWID(new BlockPos(-275, 86, -48), 2),
      new StormHVHHelper.BPWID(new BlockPos(-191, 87, 8), 5),
      new StormHVHHelper.BPWID(new BlockPos(-204, 86, 34), 5),
      new StormHVHHelper.BPWID(new BlockPos(-243, 90, 17), 215),
      new StormHVHHelper.BPWID(new BlockPos(-274, 88, -10), 17)
   );
   private static final List<StormHVHHelper.BPWID> verifyBlocks2 = Arrays.asList(
      new StormHVHHelper.BPWID(new BlockPos(15, 87, -27), 138),
      new StormHVHHelper.BPWID(new BlockPos(37, 74, 8), 124),
      new StormHVHHelper.BPWID(new BlockPos(82, 54, 3), 124),
      new StormHVHHelper.BPWID(new BlockPos(107, 42, -47), 124),
      new StormHVHHelper.BPWID(new BlockPos(100, 33, -110), 124),
      new StormHVHHelper.BPWID(new BlockPos(-52, 30, -117), 96),
      new StormHVHHelper.BPWID(new BlockPos(-1, 0, -109), 43),
      new StormHVHHelper.BPWID(new BlockPos(61, 3, -108), 44),
      new StormHVHHelper.BPWID(new BlockPos(17, 4, -60), 77),
      new StormHVHHelper.BPWID(new BlockPos(20, 5, -153), 80),
      new StormHVHHelper.BPWID(new BlockPos(15, 88, -57), 130),
      new StormHVHHelper.BPWID(new BlockPos(15, 88, -89), 140),
      new StormHVHHelper.BPWID(new BlockPos(13, 89, 144), 35),
      new StormHVHHelper.BPWID(new BlockPos(15, 87, -199), 251),
      new StormHVHHelper.BPWID(new BlockPos(75, 87, -189), 35),
      new StormHVHHelper.BPWID(new BlockPos(60, 96, -122), 252),
      new StormHVHHelper.BPWID(new BlockPos(92, 92, -79), 124),
      new StormHVHHelper.BPWID(new BlockPos(-67, 88, -23), 124),
      new StormHVHHelper.BPWID(new BlockPos(-67, 92, -80), 124),
      new StormHVHHelper.BPWID(new BlockPos(-49, 89, -170), 35),
      new StormHVHHelper.BPWID(new BlockPos(-107, 88, -26), 124),
      new StormHVHHelper.BPWID(new BlockPos(-19, 87, 29), 252),
      new StormHVHHelper.BPWID(new BlockPos(-123, 87, 6), 35)
   );
   boolean goSword = false;
   boolean goDuel = false;
   boolean goDuel2 = false;
   TimerHelper waitResell = TimerHelper.TimerHelperReseted();
   TimerHelper waitHeal = TimerHelper.TimerHelperReseted();
   TimerHelper waitFixall = TimerHelper.TimerHelperReseted();
   TimerHelper waitFlyMode = TimerHelper.TimerHelperReseted();
   TimerHelper waitNear = TimerHelper.TimerHelperReseted();
   TimerHelper waitPvpTime = TimerHelper.TimerHelperReseted();
   TimerHelper waitPvpTime2 = TimerHelper.TimerHelperReseted();
   boolean runEQ = false;
   List<StormHVHHelper.ItemStackInfo> findArmor = new ArrayList<>();
   private boolean s0;
   private boolean s1;
   private boolean s2;
   public boolean[] isOnSpawn = new boolean[]{false, false};
   public boolean isInStormServer;
   public boolean noRenderPlayersInWorld;
   private String RTPTYPE;
   private int rtpWaitTicks = 0;

   public StormHVHHelper() {
      super("StormHVHHelper", 0, Module.Category.MISC);
      this.settings.add(this.AutoDuel = new BoolSettings("AutoDuel", true, this));
      this.settings.add(this.AutoResellDuel = new BoolSettings("AutoResellDuel", true, this, () -> this.AutoDuel.getBool()));
      this.settings.add(this.OnlySneakDuel = new BoolSettings("OnlySneakDuel", true, this, () -> this.AutoDuel.getBool()));
      this.settings
         .add(
            this.AutoDuelMode = new ModeSettings(
               "AutoDuelMode", "ClickSword", this, new String[]{"ClickSword", "SendToPlayers", "Override"}, () -> this.AutoDuel.getBool()
            )
         );
      String[] kitsMenu = new String[]{
         "Random", "Anarchy", "Crystals", "AnarchyPlus", "Elytra", "Shield", "OpLow", "OpLite", "OpSuper", "UHC", "Totem", "OpPlus"
      };
      this.settings
         .add(
            this.SmartDuel = new BoolSettings(
               "SmartDuel", true, this, () -> this.AutoDuel.getBool() && !this.AutoDuelMode.getMode().equalsIgnoreCase("SendToPlayers")
            )
         );
      this.settings
         .add(
            this.DuelTypeMenu = new ModeSettings(
               "DuelTypeMenu", kitsMenu[1], this, kitsMenu, () -> this.AutoDuel.getBool() && !this.AutoDuelMode.getMode().equalsIgnoreCase("SendToPlayers")
            )
         );
      String[] kitsSend = new String[]{
         "Random",
         "OpPlus",
         "OpSuper",
         "Elytra",
         "Op",
         "UHC",
         "Anarchy",
         "AnarchyPlus",
         "Crystal",
         "Shield",
         "OpLite",
         "Totem",
         "Shrek",
         "Sumo",
         "Bow",
         "UHCPlus",
         "Spliff",
         "NoEnchant",
         "Combo1_8",
         "UHC2",
         "NoDebuff",
         "Bridge",
         "Anarchy2",
         "Op2",
         "Combo1_9",
         "OpLite2"
      };
      this.settings
         .add(
            this.DuelTypeSend = new ModeSettings(
               "DuelTypeSend", kitsSend[8], this, kitsSend, () -> this.AutoDuel.getBool() && !this.AutoDuelMode.getMode().equalsIgnoreCase("ClickSword")
            )
         );
      this.settings
         .add(
            this.PlayersFindMode = new ModeSettings(
               "PlayersFindMode",
               "Near",
               this,
               new String[]{"Near", "Online"},
               () -> this.AutoDuel.getBool() && !this.AutoDuelMode.getMode().equalsIgnoreCase("ClickSword")
            )
         );
      this.settings.add(this.AutoKitSelect = new BoolSettings("AutoKitSelect", true, this));
      this.settings.add(this.KitSelect = new ModeSettings("KitSelect", "Duped", this, new String[]{"Duped", "Standart"}, () -> this.AutoKitSelect.getBool()));
      this.settings.add(this.NoPlayersOnSpawn = new BoolSettings("NoPlayersOnSpawn", false, this));
      this.settings.add(this.NoSpectate = new BoolSettings("NoSpectate", true, this));
      this.settings.add(this.AllowRtpType = new BoolSettings("AllowRtpType", true, this));
      this.settings.add(this.AutoHeal = new BoolSettings("AutoHeal", true, this));
      this.settings.add(this.AutoFixall = new BoolSettings("AutoFixall", true, this));
      this.settings.add(this.AutoFlyMode = new BoolSettings("AutoFlyMode", true, this));
      this.settings.add(this.AutoNear = new BoolSettings("AutoNear", true, this));
      this.settings.add(this.AutoStrafeSet = new BoolSettings("AutoStrafeSet", true, this));
      this.settings.add(this.AbuseFlyMode = new BoolSettings("AbuseFlyMode", false, this));
      this.setDemand(0, 2);
      get = this;
   }

   public static boolean noRenderPlayersInWorld() {
      return get.isActived() && get.NoPlayersOnSpawn.getBool();
   }

   private boolean canSelectKit(boolean dupe) {
      return Minecraft.player.inventory.getStackInSlot(dupe ? 0 : 8).getItem() == Items.ENCHANTED_BOOK;
   }

   private boolean canDisband() {
      return Minecraft.player.inventory.getStackInSlot(7).getItem() == Items.GLOWSTONE_DUST;
   }

   private void disbandGroup() {
      int slot = 7;
      if (this.canDisband()) {
         if (Minecraft.player.inventory.currentItem != slot) {
            Minecraft.player.connection.sendPacket(new CPacketHeldItemChange(slot));
         }

         Minecraft.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
         if (Minecraft.player.inventory.currentItem != slot) {
            Minecraft.player.connection.sendPacket(new CPacketHeldItemChange(Minecraft.player.inventory.currentItem));
         }
      }
   }

   private void selectKitAuto(boolean dupe) {
      int slot = dupe ? 0 : 8;
      if (this.canSelectKit(dupe)) {
         if (Minecraft.player.inventory.currentItem != slot) {
            Minecraft.player.connection.sendPacket(new CPacketHeldItemChange(slot));
         }

         Minecraft.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
         if (Minecraft.player.inventory.currentItem != slot) {
            Minecraft.player.connection.sendPacket(new CPacketHeldItemChange(Minecraft.player.inventory.currentItem));
         }
      }
   }

   private boolean canSpectateLeave(boolean leave) {
      return leave
         && Block.getBlockById(Item.getIdFromItem(Minecraft.player.inventory.getStackInSlot(8).getItem())).toString().contains("minecraft:red_flower");
   }

   private void clickSpectateLeave(boolean leave) {
      if (this.canSpectateLeave(leave)) {
         Minecraft.player.connection.sendPacket(new CPacketHeldItemChange(8));
         Minecraft.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
         Minecraft.player.connection.sendPacket(new CPacketHeldItemChange(Minecraft.player.inventory.currentItem));
      }
   }

   private boolean isAutoDuel() {
      return this.AutoDuel.getBool();
   }

   private boolean isAutoHeal() {
      return this.AutoHeal.getBool()
         && (Minecraft.player.getFoodStats().getFoodLevel() < 20 || Minecraft.player.getHealth() < Minecraft.player.getMaxHealth() / 2.0F);
   }

   private boolean isAutoFixall() {
      if (this.AutoFixall.getBool()) {
         for (int i = 0; i < 4; i++) {
            ItemStack stack = Minecraft.player.inventory.armorItemInSlot(i);
            if (!stack.isEmpty() && stack.isItemStackDamageable() && (float)stack.getItemDamage() / (float)stack.getMaxDamage() > 0.046F) {
               return true;
            }
         }

         for (int ix = 0; ix < 36; ix++) {
            ItemStack stack = Minecraft.player.inventory.getStackInSlot(ix);
            if (!stack.isEmpty() && stack.isItemStackDamageable() && (float)stack.getItemDamage() / (float)stack.getMaxDamage() > 0.1F) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean isAutoFlyMode() {
      boolean can = false;
      if (this.AutoFlyMode.getBool() && !Minecraft.player.capabilities.allowFlying) {
         can = !Minecraft.player.getDisplayName().getUnformattedText().equalsIgnoreCase(Minecraft.player.getName());
      }

      return can;
   }

   private boolean isAutoNear() {
      return this.AutoNear.getBool()
         && Minecraft.player.capabilities.allowFlying
         && Minecraft.player.capabilities.isFlying
         && Minecraft.player.getHealth() >= Minecraft.player.getMaxHealth() - 1.0F
         && MoveMeHelp.isMoving()
         && Minecraft.player.getHeldItemMainhand().getItem() == Items.DIAMOND_SWORD
         && Minecraft.player.getHeldItemOffhand().getItem() != Items.air
         && Minecraft.player.dimension == 0
         && mc.world
            .getLoadedEntityList()
            .stream()
            .map(Entity::getOtherPlayerOf)
            .filter(Objects::nonNull)
            .filter(player -> (double)player.getDistanceToEntity(Minecraft.player) < 38.0 && player.canEntityBeSeen(Minecraft.player))
            .filter(player -> !Client.friendManager.isFriend(player.getName()))
            .toList()
            .isEmpty();
   }

   private boolean canClickSword() {
      return Minecraft.player.inventory.getStackInSlot(0).getItem() == Items.IRON_SWORD
         || Minecraft.player.inventory.getStackInSlot(0).getItem() == Items.DIAMOND_SWORD;
   }

   private void clickSword() {
      if (this.canClickSword()) {
         Minecraft.player.connection.sendPacket(new CPacketHeldItemChange(0));
         Minecraft.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
         Minecraft.player.connection.sendPacket(new CPacketHeldItemChange(Minecraft.player.inventory.currentItem));
      }
   }

   private boolean canClickRedstone() {
      return Minecraft.player.inventory.getStackInSlot(8).getItem() == Items.REDSTONE;
   }

   private void clickRedstone() {
      if (this.canClickRedstone()) {
         if (Minecraft.player.inventory.currentItem != 8) {
            Minecraft.player.connection.sendPacket(new CPacketHeldItemChange(8));
         }

         Minecraft.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
         if (Minecraft.player.inventory.currentItem != 8) {
            Minecraft.player.connection.sendPacket(new CPacketHeldItemChange(Minecraft.player.inventory.currentItem));
         }
      }
   }

   private boolean canClickSlotSW(String duelType, boolean smart) {
      return this.getSlotByDuelSword(duelType, smart) != -1;
   }

   private int getSlotByDuelSword(String duelType, boolean smart) {
      int slot = -1;
      int smartSlot = -1;

      for (int index = 0; index < Minecraft.player.openContainer.inventorySlots.size(); index++) {
         if (Minecraft.player.openContainer.inventorySlots.get(index).getHasStack()) {
            ItemStack stack = Minecraft.player.openContainer.inventorySlots.get(index).getStack();
            Item item = stack.getItem();
            if (item != Items.air
               && GuiScreen.func_191927_a(stack)
                  .stream()
                  .anyMatch(str -> str.toLowerCase().contains("очереди") && (str.toLowerCase().contains("1") || str.toLowerCase().contains("2")))) {
               smartSlot = index;
               break;
            }
         }
      }

      if (smart && smartSlot != -1) {
         slot = smartSlot;
      } else {
         switch (duelType) {
            case "Random":
               int[] slots = new int[]{12, 13, 14, 20, 21, 22, 23, 24, 30, 31, 32};
               if (this.lastRandom == 0) {
                  this.lastRandom = slots[MathUtils.clamp((int)((double)slots.length * Math.random()), 0, slots.length - 1)];
               }

               slot = this.lastRandom;
               break;
            case "Anarchy":
               slot = 12;
               break;
            case "Crystals":
               slot = 13;
               break;
            case "AnarchyPlus":
               slot = 14;
               break;
            case "Elytra":
               slot = 20;
               break;
            case "Shield":
               slot = 21;
               break;
            case "OpLow":
               slot = 22;
               break;
            case "OpLite":
               slot = 23;
               break;
            case "OpSuper":
               slot = 24;
               break;
            case "UHC":
               slot = 30;
               break;
            case "Totem":
               slot = 31;
               break;
            case "OpPlus":
               slot = 32;
         }
      }

      return slot;
   }

   private boolean canClickSlotSend(String duelType) {
      return this.getSlotByDuelSend(duelType) != -1;
   }

   private int getSlotByDuelSend(String duelType) {
      int slot = -1;
      switch (duelType) {
         case "Random":
            int[] slots = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 21, 22, 23, 24, 25};
            if (this.lastRandom == 0) {
               this.lastRandom = slots[MathUtils.clamp((int)((double)slots.length * Math.random()), 0, slots.length - 1)];
            }

            slot = this.lastRandom;
            break;
         case "OpPlus":
            slot = 0;
            break;
         case "OpSuper":
            slot = 1;
            break;
         case "Elytra":
            slot = 2;
            break;
         case "Op":
            slot = 3;
            break;
         case "UHC":
            slot = 4;
            break;
         case "Anarchy":
            slot = 5;
            break;
         case "AnarchyPlus":
            slot = 6;
            break;
         case "Crystal":
            slot = 7;
            break;
         case "Shield":
            slot = 8;
            break;
         case "OpLite":
            slot = 9;
            break;
         case "Totem":
            slot = 10;
            break;
         case "Shrek":
            slot = 11;
            break;
         case "Sumo":
            slot = 12;
            break;
         case "Bow":
            slot = 13;
            break;
         case "UHCPlus":
            slot = 14;
            break;
         case "Spliff":
            slot = 15;
            break;
         case "NoEnchant":
            slot = 16;
            break;
         case "Combo1_8":
            slot = 17;
            break;
         case "UHC2":
            slot = 19;
            break;
         case "NoDebuff":
            slot = 20;
            break;
         case "Bridge":
            slot = 21;
            break;
         case "Anarchy2":
            slot = 22;
            break;
         case "Op2":
            slot = 23;
            break;
         case "Combo1_9":
            slot = 24;
            break;
         case "OpLite2":
            slot = 25;
      }

      return slot;
   }

   private void clickSlot(int slot, boolean silent) {
      mc.playerController.windowClick(Minecraft.player.openContainer.windowId, slot, 0, ClickType.PICKUP, Minecraft.player);
      if (silent) {
         Minecraft.player.closeScreen();
         mc.currentScreen = null;
      }
   }

   private boolean isInSelectDuelMenu() {
      boolean hasIronIngot = false;
      boolean hasPaper = false;
      boolean hasGoldIngot = false;
      int countGlass = 0;
      if (mc.currentScreen instanceof GuiContainer
         && !(mc.currentScreen instanceof GuiInventory)
         && Minecraft.player.openContainer instanceof ContainerChest
         && Minecraft.player.openContainer.inventorySlots.size() == 90) {
         for (int index = 0; index < Minecraft.player.openContainer.inventorySlots.size(); index++) {
            if (Minecraft.player.openContainer.inventorySlots.get(index).getHasStack()) {
               ItemStack stack = Minecraft.player.openContainer.inventorySlots.get(index).getStack();
               Item item = stack.getItem();
               if (item == Item.getItemFromBlock(Blocks.STAINED_GLASS_PANE) && stack.stackSize == 1) {
                  countGlass++;
               }

               if (item == Items.IRON_INGOT && stack.stackSize == 1) {
                  hasIronIngot = true;
               } else if (item == Items.PAPER && stack.stackSize == 1) {
                  hasPaper = true;
               } else if (item == Items.GOLD_INGOT && stack.stackSize == 1) {
                  hasGoldIngot = true;
               }
            }
         }
      }

      return countGlass == 6 && hasIronIngot && hasPaper && hasGoldIngot;
   }

   private boolean isInInterDuelSend() {
      int countPaper = 0;
      boolean hasSlot22Fireball = false;
      if (!(mc.currentScreen instanceof GuiInventory)
         && Minecraft.player.openContainer instanceof ContainerChest
         && Minecraft.player.openContainer.inventorySlots.size() == 90) {
         for (int index = 0; index < Minecraft.player.openContainer.inventorySlots.size(); index++) {
            if (Minecraft.player.openContainer.inventorySlots.get(index).getHasStack()) {
               ItemStack stack = Minecraft.player.openContainer.inventorySlots.get(index).getStack();
               Item item = stack.getItem();
               if (item == Items.PAPER && stack.stackSize == 1) {
                  countPaper++;
               } else if (item == Items.FIRE_CHARGE && index == this.getInterSlotDuelSend() && stack.stackSize == 1) {
                  hasSlot22Fireball = true;
               }
            }
         }
      }

      return countPaper == 5 && hasSlot22Fireball;
   }

   private int getInterSlotDuelSend() {
      return 22;
   }

   private boolean isInSelectDuelSend() {
      int countPaper = 0;
      int countAir = 0;
      int snowBallCount = 0;
      if (!(mc.currentScreen instanceof GuiInventory) && Minecraft.player.openContainer instanceof ContainerChest chest) {
         for (int index = 0; index < Minecraft.player.openContainer.inventorySlots.size(); index++) {
            ItemStack stack = Minecraft.player.openContainer.inventorySlots.get(index).getStack();
            Item item = stack.getItem();
            if (item == Items.PAPER && stack.stackSize == 1) {
               countPaper++;
            } else if (item == Items.air && stack.stackSize == 1) {
               countAir++;
            } else if (item == Items.SNOWBALL && stack.stackSize == 1) {
               snowBallCount++;
            }
         }
      }

      return countPaper == 5 && countAir >= 54 && snowBallCount == 2;
   }

   private boolean isInMapSelectDuelSend() {
      boolean sata = false;
      if (!(mc.currentScreen instanceof GuiInventory)
         && Minecraft.player.openContainer instanceof ContainerChest
         && Minecraft.player.openContainer.inventorySlots.size() == 90) {
         sata = true;

         for (int index = 0; index < Minecraft.player.openContainer.inventorySlots.size(); index++) {
            ItemStack stack = Minecraft.player.openContainer.inventorySlots.get(index).getStack();
            Item item = stack.getItem();
            if (stack.stackSize != 1) {
               sata = false;
               break;
            }

            switch (index) {
               case 10:
                  if (item != Item.getItemFromBlock(Blocks.DEADBUSH)) {
                     sata = false;
                  }
                  break;
               case 11:
                  if (item != Item.getItemFromBlock(Blocks.YELLOW_FLOWER)) {
                     sata = false;
                  }
                  break;
               case 23:
                  if (item != Items.NAME_TAG) {
                     sata = false;
                  }
                  break;
               case 29:
                  if (item != Item.getItemFromBlock(Blocks.TORCH)) {
                     sata = false;
                  }
            }
         }
      }

      return sata;
   }

   private int getMapRandomSelectSlotDuelSend() {
      List<Integer> whiteSlots = new ArrayList<>();
      String whiteLore = "Выбрать карту";
      int slot = -1;
      if (mc.currentScreen instanceof GuiContainer
         && !(mc.currentScreen instanceof GuiInventory)
         && Minecraft.player.openContainer instanceof ContainerChest
         && Minecraft.player.openContainer.inventorySlots.size() == 90) {
         for (int index = 0; index < Minecraft.player.openContainer.inventorySlots.size(); index++) {
            if (Minecraft.player.openContainer.inventorySlots.get(index).getHasStack()) {
               ItemStack stack = Minecraft.player.openContainer.inventorySlots.get(index).getStack();
               List<String> lores = OffHand.get.getLoresAsStack(stack);
               if (lores.stream().anyMatch(lore -> lore.toLowerCase().contains("Выбрать карту".toLowerCase()))) {
                  whiteSlots.add(index);
               }
            }
         }
      }

      if (!whiteSlots.isEmpty()) {
         if (whiteSlots.size() > 1) {
            slot = whiteSlots.get(MathUtils.clamp((int)((double)whiteSlots.size() * Math.random()), 0, whiteSlots.size() - 1));
         } else {
            slot = whiteSlots.get(0);
         }
      }

      return slot;
   }

   private boolean verifyPlayerName(String name) {
      if (name != null && !name.isEmpty() && name.length() >= 3) {
         String whiteABC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";
         List<String> whiteABCs = Arrays.asList("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_".split(""));

         for (char theChar : name.toCharArray()) {
            String alphaBet = String.valueOf(theChar);
            if (whiteABCs.stream().noneMatch(abc -> abc.equals(alphaBet))) {
               return false;
            }
         }

         return !name.equalsIgnoreCase(mc.getSession().getUsername());
      } else {
         return false;
      }
   }

   private boolean isInDonateMenu() {
      boolean hasPaper = false;
      int headCount = 0;
      if (mc.currentScreen instanceof GuiContainer
         && !(mc.currentScreen instanceof GuiInventory)
         && Minecraft.player.openContainer instanceof ContainerChest chest) {
         for (int index = 0; index < chest.inventorySlots.size(); index++) {
            if (chest.inventorySlots.get(index).getHasStack()) {
               ItemStack stack = chest.inventorySlots.get(index).getStack();
               Item item = stack.getItem();
               if (item == Items.PAPER) {
                  hasPaper = true;
               } else if (item == Items.SKULL) {
                  headCount++;
               }
            }
         }
      }

      return headCount == 8 && hasPaper;
   }

   private boolean isInHouseOfWhoresMenu() {
      int paperCount = 0;
      boolean hasAir22Slot = false;
      if (mc.currentScreen instanceof GuiContainer
         && !(mc.currentScreen instanceof GuiInventory)
         && Minecraft.player.openContainer instanceof ContainerChest chest) {
         for (int index = 0; index < chest.inventorySlots.size(); index++) {
            if (!chest.inventorySlots.get(index).getHasStack()) {
               if (index == this.getInterSlotDuelSend()) {
                  hasAir22Slot = true;
               }
            } else {
               ItemStack stack = chest.inventorySlots.get(index).getStack();
               Item item = stack.getItem();
               if (item == Items.PAPER) {
                  paperCount++;
               }
            }
         }
      }

      return paperCount == 5 && hasAir22Slot;
   }

   private boolean onCanSendDuelToPlayerSuccess(boolean onlyNear, long delayMin) {
      if (mc.world == null) {
         return false;
      } else {
         if (Minecraft.player.inventory.getStackInSlot(8).getItem() != Items.REDSTONE) {
            if (Minecraft.player.inventory.getStackInSlot(8).getItem() != Items.GHAST_TEAR) {
               return false;
            }

            if (Minecraft.player.inventory.getStackInSlot(7).getItem() != Items.FIREWORKS) {
               return false;
            }
         }

         boolean hasSend = false;
         List<String> names = onlyNear
            ? mc.world
               .playerEntities
               .stream()
               .filter(Objects::nonNull)
               .map(EntityPlayer::getName)
               .filter(Objects::nonNull)
               .filter(this::verifyPlayerName)
               .toList()
            : mc.getConnection()
               .getPlayerInfoMap()
               .stream()
               .filter(Objects::nonNull)
               .filter(map -> map.getGameType().isAdventure())
               .map(info -> info.getGameProfile() == null ? null : info.getGameProfile().getName())
               .filter(this::verifyPlayerName)
               .filter(Objects::nonNull)
               .toList();
         if (names.isEmpty()) {
            return false;
         } else {
            String randomName = names.size() > 1 ? names.get(MathUtils.clamp((int)((double)names.size() * Math.random()), 0, names.size() - 1)) : names.get(0);
            if (this.verifyPlayerName(randomName) && mc.getConnection() != null && this.timerSendDuel.hasReached((double)delayMin)) {
               this.timerSendDuel.reset();
               mc.getConnection().sendPacket(new CPacketChatMessage("/duel " + randomName));
               hasSend = true;
            }

            return hasSend;
         }
      }
   }

   public static boolean isInStormServer() {
      return !mc.isSingleplayer()
         && mc.getCurrentServerData() != null
         && mc.getCurrentServerData().serverIP != null
         && (mc.getCurrentServerData().serverIP.toLowerCase().contains("stormhvh") || mc.getCurrentServerData().serverIP.toLowerCase().contains("galaxyhvh"));
   }

   private static boolean hasVoidBedrock() {
      return mc.world.getBlockState(new BlockPos(Minecraft.player.posX, 0.0, Minecraft.player.posZ)).getBlock() == Blocks.BEDROCK;
   }

   private static boolean hasBlockVerifyInWorld(StormHVHHelper.BPWID bpwid) {
      boolean has = false;
      if (bpwid != null && bpwid.getPos() != null) {
         IBlockState state = mc.world.getBlockState(bpwid.getPos());
         if (Block.getIdFromBlock(state.getBlock()) == bpwid.getCurID()) {
            has = true;
         }
      }

      return has;
   }

   public static boolean isInStormSpawn(boolean isDuelsSpawn) {
      return isDuelsSpawn || !hasVoidBedrock() && verifyBlocks2.stream().anyMatch(StormHVHHelper::hasBlockVerifyInWorld);
   }

   public static boolean isInStormDuelsSpawn() {
      return !hasVoidBedrock() && verifyBlocks.stream().anyMatch(StormHVHHelper::hasBlockVerifyInWorld);
   }

   private boolean inPvpTime() {
      return !GuiBossOverlay.mapBossInfos2.isEmpty()
         && !GuiBossOverlay.mapBossInfos2
            .values()
            .stream()
            .map(BossInfo::getName)
            .map(ITextComponent::getUnformattedText)
            .map(String::toLowerCase)
            .filter(name -> name.contains("pvp") || name.contains("пвп") || name.contains("сек."))
            .filter(Objects::nonNull)
            .toList()
            .isEmpty();
   }

   private boolean compareItemStacksInfos(StormHVHHelper.ItemStackInfo first, StormHVHHelper.ItemStackInfo second) {
      int[] args = new int[]{
         first.stacksize == second.stacksize ? 1 : 0,
         first.type == second.type ? 1 : 0,
         first.itemId == second.itemId ? 1 : 0,
         first.enchHashes == second.enchHashes ? 1 : 0,
         first.displayInt == second.displayInt ? 1 : 0
      };
      return Arrays.stream(args).allMatch(arg -> arg == 1);
   }

   private boolean compareItemStacksInfos(StormHVHHelper.ItemStackInfo first, ItemStack second1) {
      StormHVHHelper.ItemStackInfo second = new StormHVHHelper.ItemStackInfo(second1);
      return this.compareItemStacksInfos(first, second);
   }

   @Override
   public void onToggled(boolean actived) {
      if (actived) {
      }

      this.resetStats();
      super.onToggled(actived);
   }

   private void resetStats() {
      this.s0 = false;
      this.s1 = false;
      this.s2 = false;
      this.goDuel = false;
      this.goDuel2 = false;
      this.goSword = false;
      this.lastRandom = 0;
      this.rtpWaitTicks = 0;
      this.isOnSpawn[0] = false;
      this.isOnSpawn[1] = false;
      this.isInStormServer = false;
      this.noRenderPlayersInWorld = false;
   }

   @Override
   public void onUpdate() {
      this.isInStormServer = isInStormServer();
      if (!this.isInStormServer) {
         this.resetStats();
      } else {
         this.noRenderPlayersInWorld = noRenderPlayersInWorld();
         this.isOnSpawn = new boolean[2];
         if (this.isInStormServer) {
            this.isOnSpawn[0] = isInStormDuelsSpawn();
            this.isOnSpawn[1] = isInStormSpawn(this.isOnSpawn[0]);
         }

         if (this.isInStormServer && this.isOnSpawn[1] && noRenderPlayersInWorld() && mc.world != null) {
            for (Entity entity : mc.world.getLoadedEntityList()) {
               if (entity instanceof EntityOtherPlayerMP) {
                  EntityOtherPlayerMP mp = (EntityOtherPlayerMP)entity;
                  if (mp != FreeCam.fakePlayer && mp.getEntityId() != 462462998 && !Client.friendManager.isFriend(mp.getName())) {
                     mc.world.removeEntityFromWorld(mp.getEntityId());
                  }
               }
            }

            mc.world.playerEntities.clear();
         }

         if (this.AutoKitSelect.getBool()
            && this.isInStormServer
            && !this.isOnSpawn[0]
            && mc.currentScreen == null
            && this.canSelectKit(this.KitSelect.currentMode.equalsIgnoreCase("Duped"))) {
            this.selectKitAuto(this.KitSelect.currentMode.equalsIgnoreCase("Duped"));
         }

         boolean isSendDuelMode = this.AutoDuelMode.getMode().equalsIgnoreCase("SendToPlayers") || this.AutoDuelMode.getMode().equalsIgnoreCase("Override");
         boolean isClickDuelMode = this.AutoDuelMode.getMode().equalsIgnoreCase("ClickSword") || this.AutoDuelMode.getMode().equalsIgnoreCase("Override");
         if (this.isAutoDuel()
            && isClickDuelMode
            && this.isInStormServer
            && (!isSendDuelMode || !this.s0 && !this.s1 && !this.s2 && !this.goDuel2)
            && this.isOnSpawn[0]
            && this.canClickSword()
            && mc.currentScreen == null
            && (!this.OnlySneakDuel.getBool() || Minecraft.player.isSneaking())) {
            if (this.canDisband()) {
               this.disbandGroup();
            } else {
               this.goSword = true;
            }
         }

         if (this.goSword && isClickDuelMode) {
            this.clickSword();
            this.goDuel = true;
            this.goSword = false;
         }

         this.clickSpectateLeave(this.NoSpectate.getBool());
         if (this.goDuel && isClickDuelMode && this.isInSelectDuelMenu()) {
            mc.currentScreen = null;
            if (this.canClickSlotSW(this.DuelTypeMenu.getMode(), this.SmartDuel.getBool())) {
               this.clickSlot(this.getSlotByDuelSword(this.DuelTypeMenu.getMode(), this.SmartDuel.getBool()), true);
               this.lastRandom = 0;
            }
         }

         if (this.AutoResellDuel.getBool() && this.isAutoDuel() && isClickDuelMode) {
            if (Minecraft.player.inventory.getStackInSlot(8).getItem() == Items.REDSTONE) {
               if (this.waitResell.hasReached(200.0)) {
                  this.clickRedstone();
                  this.waitResell.reset();
               }
            } else {
               this.waitResell.reset();
            }
         }

         long delaySends = 950L;
         if (this.isAutoDuel()
            && isSendDuelMode
            && this.isOnSpawn[0]
            && mc.currentScreen == null
            && (!this.OnlySneakDuel.getBool() || Minecraft.player.isSneaking())
            && !this.goDuel2
            && this.onCanSendDuelToPlayerSuccess(this.PlayersFindMode.getMode().equalsIgnoreCase("Near"), delaySends)) {
            this.goDuel2 = true;
            this.s0 = true;
         }

         if (!isSendDuelMode || !this.isInDonateMenu() && !this.isInHouseOfWhoresMenu()) {
            if (isSendDuelMode && this.goDuel2) {
               if (this.s0 && this.isInInterDuelSend() && this.canClickSlotSend(this.DuelTypeSend.getMode())) {
                  this.clickSlot(this.getInterSlotDuelSend(), false);
                  this.s0 = false;
                  this.s1 = true;
               } else if (this.s1 && this.isInSelectDuelSend()) {
                  this.clickSlot(this.getSlotByDuelSend(this.DuelTypeSend.getMode()), false);
                  this.lastRandom = 0;
                  this.s1 = false;
                  this.s2 = true;
               } else if (this.s2 && this.isInMapSelectDuelSend()) {
                  this.clickSlot(this.getMapRandomSelectSlotDuelSend(), false);
                  this.goDuel2 = false;
                  this.s2 = false;
               }
            }
         } else {
            Minecraft.player.closeScreen();
            mc.currentScreen = null;
            this.goDuel2 = false;
         }

         if (isSendDuelMode && !this.goDuel2 && (this.isInInterDuelSend() || this.isInSelectDuelSend() || this.isInMapSelectDuelSend())) {
            Minecraft.player.closeScreen();
            mc.currentScreen = null;
         }

         if (isSendDuelMode && (!this.isOnSpawn[0] || mc.currentScreen == null && this.timerSendDuel.hasReached((double)(delaySends - 50L)))) {
            this.goDuel2 = false;
         }

         if (!this.isInSelectDuelMenu() && (Minecraft.player.inventory.getStackInSlot(8).getItem() instanceof ItemRedstone || !this.isOnSpawn[0])) {
            this.goDuel = false;
         }

         if (this.rtpWaitTicks > 0) {
            if (this.RTPTYPE != null && this.isInRtpSelectGui()) {
               this.selectRtpTypeInMenu(this.RTPTYPE, true);
               this.RTPTYPE = null;
            }

            this.rtpWaitTicks--;
         } else {
            this.RTPTYPE = null;
         }

         boolean inPvpTime = this.inPvpTime();
         boolean damaged = Minecraft.player.hurtTime != 0;
         if (inPvpTime) {
            this.waitPvpTime.reset();
         }

         if (damaged) {
            this.waitPvpTime2.reset();
         }

         inPvpTime = !this.waitPvpTime.hasReached(150.0) || !this.waitPvpTime2.hasReached(5000.0);
         if (this.isInStormServer && this.isAutoHeal() && this.waitHeal.hasReached(60500.0) && !inPvpTime) {
            mc.getConnection().sendPacket(new CPacketChatMessage("/heal"));
            this.waitHeal.reset();
            this.waitNear.reset();
         }

         if (this.isInStormServer
            && (this.isAutoFixall() || this.runEQ)
            && !inPvpTime
            && Minecraft.player.openContainer instanceof ContainerPlayer inventoryContiner) {
            if (this.waitFixall.hasReached(250.0)) {
               if (!this.findArmor.isEmpty()) {
                  for (int slotI = 0; slotI < 36; slotI++) {
                     ItemStack stack = Minecraft.player.inventory.getStackInSlot(slotI);
                     if (stack.getItem() instanceof ItemArmor) {
                        boolean ae = this.findArmor.stream().anyMatch(stackFind -> this.compareItemStacksInfos(stackFind, stack));
                        if (ae) {
                           mc.playerController.windowClick(0, slotI, 1 - slotI % 2, ClickType.QUICK_MOVE, Minecraft.player);
                        }
                     }
                  }
               }

               ProContainer.autoArmorOFF = true;
               this.findArmor.clear();
               this.runEQ = false;
            }

            if (this.waitFixall.hasReached(60500.0)) {
               if (!mc.world
                  .getLoadedEntityList()
                  .stream()
                  .map(Entity::getOtherPlayerOf)
                  .filter(Objects::nonNull)
                  .filter(
                     player -> (double)player.getDistanceToEntity(Minecraft.player) < 7.0
                           && player.getTotalArmorValue() != 0
                           && !Client.friendManager.isFriend(player.getName())
                  )
                  .toList()
                  .isEmpty()) {
                  return;
               }

               int emptySlots = 0;
               int slotIx = 0;

               for (ItemStack stack : inventoryContiner.getInventory()) {
                  if (slotIx >= 9 && slotIx <= 44 && stack.getItem() == Items.air) {
                     if (++emptySlots == 4) {
                        break;
                     }
                  }

                  slotIx++;
               }

               slotIx = 0;
               if (emptySlots == 4 && !this.runEQ) {
                  for (ItemStack stack : inventoryContiner.getInventory()) {
                     if (slotIx >= 5 && slotIx < 5 + emptySlots && stack.getItem() instanceof ItemArmor && stack.isItemDamaged()) {
                        this.findArmor.add(new StormHVHHelper.ItemStackInfo(stack));
                        mc.playerController.windowClick(0, slotIx, 1 - slotIx % 2, ClickType.QUICK_MOVE, Minecraft.player);
                     }

                     slotIx++;
                  }

                  mc.getConnection().sendPacket(new CPacketChatMessage("/fix all"));
                  this.runEQ = true;
                  this.waitFixall.reset();
                  this.waitNear.reset();
               }
            }
         }

         if (this.isInStormServer && this.isAutoFlyMode() && this.waitFlyMode.hasReached(1300.0) && !inPvpTime) {
            mc.getConnection().sendPacket(new CPacketChatMessage("/fly"));
            boolean prevFly = Minecraft.player.capabilities.isFlying;
            Minecraft.player.capabilities.isFlying = true;
            mc.getConnection().sendPacket(new CPacketPlayerAbilities(Minecraft.player.capabilities));
            Minecraft.player.capabilities.isFlying = prevFly;
            this.waitFlyMode.reset();
            this.waitNear.reset();
         }

         if (this.isInStormServer && this.isAutoNear() && this.waitNear.hasReached(1500.0) && !inPvpTime) {
            mc.getConnection().sendPacket(new CPacketChatMessage("/near"));
            this.waitNear.reset();
         }

         boolean canStrict = false;
         if (this.isInStormServer && this.AbuseFlyMode.getBool()) {
            boolean hasBypass = Bypass.get.isActived() && Bypass.get.GMFlySpoofIfCan.getBool();
            if (!hasBypass) {
               this.AbuseFlyMode.setBool(false);
               ClientTune.get.playGuiScreenCheckBox(true);
               Client.msg("§f§lModules:§r §7[§lStormHVHHelper§r§7]: для абуза флай мода:", false);
               Client.msg("§f§lModules:§r §7[§lStormHVHHelper§r§7]: включите модуль Bypass.", false);
               Client.msg("§f§lModules:§r §7[§lStormHVHHelper§r§7]: включите в нём чек GMFlySpoofIfCan.", false);
            } else if (Minecraft.player.capabilities.allowFlying && !Minecraft.player.capabilities.isFlying && !inPvpTime) {
               canStrict = true;
               if (MoveMeHelp.isMoving()) {
                  if (Minecraft.player.onGround && !Minecraft.player.isJumping()) {
                     if (Strafe.get.actived && Strafe.get.Mode.currentMode.equalsIgnoreCase("Strict")) {
                        Minecraft.player.onGround = MoveMeHelp.getSpeed() < 0.16F;
                     }

                     MoveMeHelp.setSpeed(MathUtils.clamp(MoveMeHelp.getSpeed() * (double)(Minecraft.player.onGround ? 1.5F : 5.0F), 0.26, 1.0));
                  } else if (Minecraft.player.isMoving()) {
                     MoveMeHelp.setSpeed(
                        MathUtils.clamp(MoveMeHelp.getSpeed() * 1.2, 0.26, Minecraft.player.onGround && Minecraft.player.movementInput.jump ? 0.67F : 1.15F)
                     );
                  }
               }

               Speed.get.cancelStrafe = MoveMeHelp.getSpeed() > 0.3F || Minecraft.player.onGround && !Minecraft.player.isJumping();
            } else if (Speed.get.cancelStrafe) {
               Speed.get.cancelStrafe = false;
            }
         }

         if (this.isInStormServer && this.AutoStrafeSet.getBool()) {
            String strafeMode = Strafe.get.Mode.currentMode;
            String current = Minecraft.player.capabilities.allowFlying && !inPvpTime && canStrict ? "Strict" : "Matrix5";
            if (!strafeMode.equalsIgnoreCase(current)) {
               if (mc.currentScreen == Client.clickGuiScreen) {
                  Client.msg("§f§lModules:§r §7[§lStormHVHHelper§r§7]: для смены мода Strafe", false);
                  Client.msg("§f§lModules:§r §7[§lStormHVHHelper§r§7]: отключите чек AutoStrafeSet.", false);
               }

               Strafe.get.Mode.setMode(current);
            }

            strafeMode = AirJump.get.Mode.currentMode;
            current = Minecraft.player.capabilities.allowFlying && !inPvpTime ? (canStrict ? "Default" : "Matrix") : "Matrix2";
            if (!strafeMode.equalsIgnoreCase(current)) {
               if (mc.currentScreen == Client.clickGuiScreen) {
                  Client.msg("§f§lModules:§r §7[§lStormHVHHelper§r§7]: для смены мода AirJump", false);
                  Client.msg("§f§lModules:§r §7[§lStormHVHHelper§r§7]: отключите чек AutoStrafeSet.", false);
               }

               AirJump.get.Mode.setMode(current);
            }
         }
      }
   }

   @EventTarget
   public void onSendPacket(EventSendPacket event) {
      if (event.getPacket() instanceof CPacketChatMessage chatPacket
         && this.actived
         && this.RTPTYPE == null
         && this.AllowRtpType.getBool()
         && isInStormServer()) {
         this.RTPTYPE = null;
         String msg = chatPacket.getMessage();
         if (msg.toLowerCase().startsWith("/rtp")) {
            String var4 = msg.toLowerCase();
            switch (var4) {
               case "/rtp":
                  this.RTPTYPE = "normal";
                  break;
               case "/rtp far":
                  this.RTPTYPE = "far";
                  break;
               case "/rtp near":
                  this.RTPTYPE = "near";
            }

            if (this.RTPTYPE != null) {
               chatPacket.setMessage("/rtp");
               this.rtpWaitTicks = 20;
            }
         }
      }
   }

   private boolean isInRtpSelectGui() {
      int countGlass = 0;
      int hasProp = 0;
      int airCount = 0;
      if (Minecraft.player.openContainer instanceof ContainerChest chest && chest.inventorySlots.size() == 90) {
         for (int index = 0; index < chest.inventorySlots.size(); index++) {
            ItemStack stack = chest.inventorySlots.get(index).getStack();
            if (stack != null) {
               Item item = stack.getItem();
               if (stack.stackSize == 1) {
                  if (item instanceof ItemEnderPearl || item instanceof ItemEnderEye || item instanceof ItemSkull) {
                     hasProp++;
                  } else if (item == Items.air) {
                     airCount++;
                  }
               }
            }
         }
      }

      return countGlass == 0 && hasProp == 3 && airCount == 53;
   }

   private void selectRtpTypeInMenu(String rtpType, boolean msg) {
      int slot = -1;
      switch (rtpType) {
         case "normal":
            slot = 20;
            break;
         case "near":
            slot = 22;
            break;
         case "far":
            slot = 24;
      }

      if (slot != -1) {
         this.clickSlot(slot, true);
         if (msg) {
            Client.msg("§f§lModules:§r §7[§l" + this.name + "§r§7]: Помогаю использовать /rtp " + rtpType + ".", false);
         }
      }
   }

   public static boolean cancelRenderEntityLivingBase(EntityLivingBase entity) {
      if (entity instanceof EntityOtherPlayerMP mp
         && !RenderLivingBase.silentMode
         && get != null
         && get.noRenderPlayersInWorld
         && get.isInStormServer
         && get.isOnSpawn[1]) {
         return mp != FreeCam.fakePlayer && mp.getEntityId() != 462462998 && !Client.friendManager.isFriend(mp.getName());
      }

      return false;
   }

   private static class BPWID {
      private final BlockPos pos;
      private final int curID;

      public BlockPos getPos() {
         return this.pos;
      }

      public int getCurID() {
         return this.curID;
      }

      public BPWID(BlockPos pos, int curID) {
         this.pos = pos;
         this.curID = curID;
      }
   }

   private class ItemStackInfo {
      int stacksize = 0;
      int type = 0;
      int itemId = 0;
      int enchHashes = 0;
      int displayInt = 0;

      public ItemStackInfo(ItemStack stack) {
         this.stacksize = stack.stackSize;
         this.itemId = Item.getIdFromItem(stack.getItem());
         NBTTagList nbttaglist = stack.getItem() == Items.ENCHANTED_BOOK ? ItemEnchantedBook.getEnchantments(stack) : stack.getEnchantmentTagList();

         for (int i = 0; i < nbttaglist.tagCount(); i++) {
            NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
            Enchantment enchantment = Enchantment.getEnchantmentByID(nbttagcompound.getShort("id"));
            if (enchantment != null) {
               if (enchantment.type == EnumEnchantmentType.ARMOR) {
                  this.enchHashes = this.enchHashes + String.valueOf(enchantment.hashCode()).length();
               }

               if (enchantment.type != null) {
                  this.type = String.valueOf(enchantment.type.name().hashCode()).length();
               }
            }
         }

         for (char ch : stack.getDisplayName().toCharArray()) {
            this.displayInt = this.displayInt + String.valueOf(String.valueOf(ch).hashCode()).length();
         }
      }
   }
}
