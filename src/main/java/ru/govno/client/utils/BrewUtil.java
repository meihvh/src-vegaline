package ru.govno.client.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerBrewingStand;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import ru.govno.client.module.modules.PotionBrewer;
import ru.govno.client.utils.Math.ReplaceStrUtils;

public class BrewUtil {
   public final int[] SLOTS_BOTTLES = new int[]{0, 1, 2};
   public final int SLOT_INGRIDIENT = 3;
   public final int SLOT_POWER = 4;
   public static final Minecraft mc = Minecraft.getMinecraft();
   public ru.govno.client.utils.Math.TimerHelper operationTimer = ru.govno.client.utils.Math.TimerHelper.TimerHelperReseted();
   private int countOfSuccessBrews;
   private int countOfIngridientPuts;

   private boolean isBrewingProgress(ContainerBrewingStand container) {
      return container.tileBrewingStand.getField(0) > 0;
   }

   public int getIndexSearchSlotInContainer(Container container, Item item, int... blackslots) {
      for (int i = 0; i < container.inventorySlots.size(); i++) {
         Slot slot = container.inventorySlots.get(i);
         ItemStack stack = slot.getStack();
         BrewUtil.PotionItemData opt;
         if (stack != null
            && stack.getItem() == item
            && (item != Items.POTIONITEM || (opt = new BrewUtil.PotionItemData(stack)).isWaterBottle() && opt.getMainEffect() == null)) {
            boolean any = false;

            for (int blackslot : blackslots) {
               if (blackslot == i) {
                  any = true;
                  break;
               }
            }

            if (!any) {
               return i;
            }
         }
      }

      return -1;
   }

   public int getCountSearchItemInContainer(Container container, Item item, int... blackslots) {
      int countItemInContainer = 0;

      for (int i = 0; i < container.inventorySlots.size(); i++) {
         Slot slot = container.inventorySlots.get(i);
         ItemStack stack = slot.getStack();
         BrewUtil.PotionItemData opt;
         if (stack != null
            && stack.getItem() == item
            && (item != Items.POTIONITEM || (opt = new BrewUtil.PotionItemData(stack)).isWaterBottle() && opt.getMainEffect() == null)) {
            boolean any = false;

            for (int blackslot : blackslots) {
               if (blackslot == i) {
                  any = true;
                  break;
               }
            }

            if (!any) {
               countItemInContainer += stack.stackSize;
            }
         }
      }

      return countItemInContainer;
   }

   public int getCountSearchItemInInventory(Item item) {
      int countItemInContainer = 0;

      for (int i = 0; i < 45; i++) {
         ItemStack stack = Minecraft.player.inventory.getStackInSlot(i);
         BrewUtil.PotionItemData opt;
         if (stack != null
            && stack.getItem() == item
            && (item != Items.POTIONITEM || (opt = new BrewUtil.PotionItemData(stack)).isWaterBottle() && opt.getMainEffect() == null)) {
            countItemInContainer += stack.stackSize;
         }
      }

      return countItemInContainer;
   }

   private boolean bottlesSlotsHasAir(ContainerBrewingStand container, boolean full) {
      int airCount = 0;

      for (int slotBottle : this.SLOTS_BOTTLES) {
         ItemStack itemStack = container.inventorySlots.get(slotBottle).getStack();
         if (new BrewUtil.PotionItemData(itemStack).isAirStack()) {
            if (!full) {
               return true;
            }

            airCount++;
         }
      }

      return airCount == 3;
   }

   private boolean removeIngridientOfStand(ContainerBrewingStand container) {
      if (container.inventorySlots.get(3).getStack().getItem() != Items.air) {
         mc.playerController.windowClick(container.windowId, 3, 0, ClickType.QUICK_MOVE, Minecraft.player);
         if (container.inventorySlots.get(3).getStack().getItem() != Items.air) {
            mc.playerController.windowClick(container.windowId, 3, 1, ClickType.THROW, Minecraft.player);
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean putBottlesToStand(ContainerBrewingStand container, boolean fullPut) {
      boolean hasNext = false;
      int slot = this.getIndexSearchSlotInContainer(container, Items.POTIONITEM, this.SLOTS_BOTTLES);

      for (int bottleSlot : this.SLOTS_BOTTLES) {
         if (slot != -1) {
            if (container.inventorySlots.get(bottleSlot).getStack().getItem() == Items.air) {
               if (container.inventorySlots.get(slot).getStack().stackSize == 1 && bottleSlot == this.SLOTS_BOTTLES[0]) {
                  mc.playerController.windowClick(container.windowId, slot, 0, ClickType.QUICK_MOVE, Minecraft.player);
               } else {
                  mc.playerController.windowClick(container.windowId, slot, 0, ClickType.PICKUP, Minecraft.player);
                  mc.playerController.windowClick(container.windowId, bottleSlot, 1, ClickType.PICKUP, Minecraft.player);
                  mc.playerController.windowClick(container.windowId, slot, 0, ClickType.PICKUP, Minecraft.player);
               }

               hasNext = true;
               if (!fullPut) {
                  return true;
               }
            }

            slot = this.getIndexSearchSlotInContainer(container, Items.POTIONITEM, this.SLOTS_BOTTLES);
         }
      }

      return hasNext;
   }

   private boolean stealAllPotionsIsLast(ContainerBrewingStand container, boolean boostEffect, boolean splash, boolean fullSteal, boolean ignoreIfFullInv) {
      int stealled = 0;

      for (int bottleSlot : this.SLOTS_BOTTLES) {
         BrewUtil.PotionItemData data = new BrewUtil.PotionItemData(container.inventorySlots.get(bottleSlot).getStack());
         if (data.getMainEffect() != null
            && (!splash || data.isSplashBottle())
            && (!boostEffect || data.isLongestEffect(data.getMainEffect()) || data.isLeveledEffect())) {
            boolean throwing = !ignoreIfFullInv
               && this.getIndexSearchSlotInContainer(container, Items.air, this.SLOTS_BOTTLES[0], this.SLOTS_BOTTLES[1], this.SLOTS_BOTTLES[2], 4, 3) == -1;
            mc.playerController
               .windowClick(container.windowId, bottleSlot, throwing ? 1 : stealled % 2, throwing ? ClickType.THROW : ClickType.QUICK_MOVE, Minecraft.player);
            if (!fullSteal) {
               break;
            }

            stealled++;
         }
      }

      return stealled != 0;
   }

   private int putSinglePowerToStandActionInt(ContainerBrewingStand container) {
      if (container.tileBrewingStand.getField(1) <= 0) {
         int slot = this.getIndexSearchSlotInContainer(container, Items.BLAZE_POWDER, 4);
         if (slot == -1) {
            return -1;
         }

         if (container.inventorySlots.get(4).getStack().getItem() != Items.BLAZE_POWDER) {
            mc.playerController.windowClick(container.windowId, slot, 0, ClickType.PICKUP, Minecraft.player);
            mc.playerController.windowClick(container.windowId, 4, 1, ClickType.PICKUP, Minecraft.player);
            mc.playerController.windowClick(container.windowId, slot, 0, ClickType.PICKUP, Minecraft.player);
            if (container.inventorySlots.get(4).getStack().getItem() == Items.BLAZE_POWDER) {
               container.tileBrewingStand.setField(1, 20);
            }

            return 1;
         }
      } else if (container.inventorySlots.get(4).getStack().getItem() == Items.BLAZE_POWDER) {
         mc.playerController.windowClick(container.windowId, 4, 0, ClickType.QUICK_MOVE, Minecraft.player);
         if (container.inventorySlots.get(4).getStack().getItem() == Items.BLAZE_POWDER) {
            mc.playerController.windowClick(container.windowId, 4, 1, ClickType.THROW, Minecraft.player);
         }

         return container.inventorySlots.get(4).getStack().getItem() == Items.BLAZE_POWDER ? 2 : 1;
      }

      return 0;
   }

   private boolean putIngridientToStand(ContainerBrewingStand container, PotionEffect brewEffect, boolean boostEffect, boolean splash, boolean fastMode) {
      int currentIngridientSlot = -1;
      Item currentIngridient = null;

      for (int bottleSlot : this.SLOTS_BOTTLES) {
         currentIngridient = new BrewUtil.PotionItemData(container.inventorySlots.get(bottleSlot).getStack())
            .getCurrentBrewItemToBottle(brewEffect, boostEffect, splash);
      }

      if (currentIngridient != null) {
         currentIngridientSlot = this.getIndexSearchSlotInContainer(container, currentIngridient, 3, 4);
      }

      if (currentIngridientSlot != -1) {
         if (currentIngridient != Items.NETHER_WART
            && currentIngridient != Items.REDSTONE
            && currentIngridient != Items.GLOWSTONE_DUST
            && currentIngridient != Items.GUNPOWDER) {
            this.countOfIngridientPuts++;
         }

         ItemStack inIngridientSlotStack = container.inventorySlots.get(3).getStack();
         if (fastMode && inIngridientSlotStack.getItem() != currentIngridient && inIngridientSlotStack.getItem() != Items.air) {
            mc.playerController.windowClick(container.windowId, 3, 0, ClickType.QUICK_MOVE, Minecraft.player);
            if (inIngridientSlotStack.getItem() != currentIngridient) {
               mc.playerController.windowClick(container.windowId, 3, 1, ClickType.THROW, Minecraft.player);
            }
         }

         if (inIngridientSlotStack.getItem() != currentIngridient) {
            if (fastMode && inIngridientSlotStack.getItem() != currentIngridient && inIngridientSlotStack.getItem() != Items.air) {
               mc.playerController.windowClick(container.windowId, 3, 0, ClickType.QUICK_MOVE, Minecraft.player);
            }

            if (inIngridientSlotStack.getItem() == Items.air
               && container.inventorySlots.get(currentIngridientSlot).getStack().stackSize == 1
               && currentIngridient != Items.BLAZE_POWDER) {
               mc.playerController.windowClick(container.windowId, currentIngridientSlot, 0, ClickType.QUICK_MOVE, Minecraft.player);
            } else {
               mc.playerController.windowClick(container.windowId, currentIngridientSlot, 0, ClickType.PICKUP, Minecraft.player);
               mc.playerController.windowClick(container.windowId, 3, 1, ClickType.PICKUP, Minecraft.player);
               mc.playerController.windowClick(container.windowId, currentIngridientSlot, 0, ClickType.PICKUP, Minecraft.player);
            }

            return true;
         }
      }

      return false;
   }

   private boolean canBrewEffect(ContainerBrewingStand container, boolean boostEffect, boolean splash, PotionEffect brewEffect) {
      if (this.getCountSearchItemInContainer(container, Items.NETHER_WART, 3) == 0) {
         return false;
      } else {
         Item findIngridient = null;
         if (brewEffect.getPotion() == MobEffects.STRENGTH) {
            findIngridient = Items.BLAZE_POWDER;
         } else if (brewEffect.getPotion() == MobEffects.SPEED) {
            findIngridient = Items.SUGAR;
         } else if (brewEffect.getPotion() == MobEffects.HEALTH_BOOST) {
            findIngridient = Items.SPECKLED_MELON;
         } else if (brewEffect.getPotion() == MobEffects.REGENERATION) {
            findIngridient = Items.GHAST_TEAR;
         } else if (brewEffect.getPotion() == MobEffects.FIRE_RESISTANCE) {
            findIngridient = Items.MAGMA_CREAM;
         }

         return findIngridient != null && this.getCountSearchItemInContainer(container, findIngridient, 3, 4) != 0
            ? (
                  !boostEffect
                     || this.getCountSearchItemInContainer(
                           container, brewEffect.getPotion() == MobEffects.FIRE_RESISTANCE ? Items.REDSTONE : Items.GLOWSTONE_DUST, 4
                        )
                        > 0
               )
               && (!splash || this.getCountSearchItemInContainer(container, Items.GUNPOWDER, 3) > 0)
            : false;
      }
   }

   private boolean updateStatsContainer(PotionBrewer.BrewStand brewStand, ContainerBrewingStand container) {
      boolean anyChanged = brewStand.updateStacksOfContainer(container);
      brewStand.updateOnBrewingProcessStarted();
      this.operationTimer.reset();
      return anyChanged;
   }

   public boolean getIfCanBrewingAnyEffect(
      PotionBrewer.BrewStand brewStand,
      boolean has3WaterBottles,
      int powerCountBrew,
      boolean boostEffect,
      boolean splash,
      boolean brewStrength,
      boolean brewSpeed,
      boolean brewResistance,
      boolean brewHealing,
      boolean brewRegen
   ) {
      if (!brewStrength && !brewSpeed && !brewResistance && !brewHealing && !brewRegen) {
         return false;
      } else {
         try {
            Container container = Minecraft.player.openContainer;
            if (container instanceof ContainerBrewingStand) {
               return false;
            } else {
               boolean flag = Arrays.stream(brewStand.getStacksInBottleSlots())
                  .filter(Objects::nonNull)
                  .map(stack -> new BrewUtil.PotionItemData(stack))
                  .anyMatch(
                     data -> data.getMainEffect() != null
                           && (!splash || data.isSplashBottle())
                           && (!boostEffect || data.isLeveledEffect() || data.isLongestEffect(data.getMainEffect()))
                  );
               if (powerCountBrew > 0 || this.getCountSearchItemInInventory(Items.BLAZE_POWDER) > 0 || flag) {
                  if (flag) {
                     return true;
                  }

                  List<BrewUtil.PotionItemData> bottlesData = Arrays.asList(brewStand.getStacksInBottleSlots())
                     .stream()
                     .filter(Objects::nonNull)
                     .map(stack -> new BrewUtil.PotionItemData(stack))
                     .toList();
                  PotionEffect firstMainEffect = bottlesData.isEmpty()
                     ? null
                     : bottlesData.stream().map(data -> data.getMainEffect()).filter(Objects::nonNull).findFirst().orElse(null);
                  if (firstMainEffect == null || firstMainEffect.getPotion().getName().toLowerCase().contains("awkward")) {
                     if (brewStrength
                        && this.getCountSearchItemInContainer(container, Items.BLAZE_POWDER, 4, 3) == 0
                        && (firstMainEffect == null || firstMainEffect.getPotion() == MobEffects.STRENGTH)) {
                        brewStrength = false;
                     }

                     if (brewSpeed
                        && this.getCountSearchItemInContainer(container, Items.SUGAR, 3) == 0
                        && (firstMainEffect == null || firstMainEffect.getPotion() == MobEffects.SPEED)) {
                        brewSpeed = false;
                     }

                     if (brewResistance
                        && this.getCountSearchItemInContainer(container, Items.MAGMA_CREAM, 3) == 0
                        && (firstMainEffect == null || firstMainEffect.getPotion() == MobEffects.FIRE_RESISTANCE)) {
                        brewResistance = false;
                     }

                     if (brewHealing
                        && this.getCountSearchItemInContainer(container, Items.SPECKLED_MELON, 3) == 0
                        && (firstMainEffect == null || firstMainEffect.getPotion() == MobEffects.HEALTH_BOOST)) {
                        brewHealing = false;
                     }

                     if (brewRegen
                        && this.getCountSearchItemInContainer(container, Items.GHAST_TEAR, 3) == 0
                        && (firstMainEffect == null || firstMainEffect.getPotion() == MobEffects.REGENERATION)) {
                        brewRegen = false;
                     }
                  }

                  if (brewStrength || brewSpeed || brewResistance || brewHealing || brewRegen) {
                     if ((
                           bottlesData.stream().anyMatch(data -> data.isWaterBottle())
                              || this.getCountSearchItemInContainer(container, Items.POTIONITEM, this.SLOTS_BOTTLES) >= 3
                        )
                        && this.getCountSearchItemInContainer(container, Items.NETHER_WART, 3) > 0) {
                        flag = true;
                     } else if (firstMainEffect != null
                        && bottlesData.stream().noneMatch(data -> !boostEffect || data.isLeveledEffect() || data.isLongestEffect(data.getMainEffect()))
                        && this.getCountSearchItemInContainer(
                              container, firstMainEffect.getPotion() == MobEffects.REGENERATION ? Items.REDSTONE : Items.GLOWSTONE_DUST, 3
                           )
                           > 0) {
                        flag = true;
                     } else if (firstMainEffect != null
                        && (!splash || bottlesData.stream().noneMatch(data -> data.isSplashBottle()))
                        && this.getCountSearchItemInContainer(container, Items.GUNPOWDER, 3) > 0) {
                        flag = true;
                     } else if (bottlesData.stream().anyMatch(data -> data.isActivatedBottle())) {
                        flag = true;
                     }
                  }
               }

               return flag;
            }
         } catch (Exception var15) {
            var15.printStackTrace();
            return false;
         }
      }
   }

   public boolean handleBrewingStand(
      PotionBrewer.BrewStand brewStand,
      ContainerBrewingStand container,
      boolean boostEffect,
      boolean splash,
      long delayOperation,
      Runnable closeScreenAction,
      boolean brewStrength,
      boolean brewSpeed,
      boolean brewResistance,
      boolean brewHealing,
      boolean brewRegen
   ) {
      if (!brewStrength && !brewSpeed && !brewResistance && !brewHealing && !brewRegen) {
         return false;
      } else {
         if ((double)this.countOfSuccessBrews > 1.0E10) {
            this.countOfSuccessBrews = 0;
         }

         if ((double)this.countOfIngridientPuts > 1.0E10) {
            this.countOfIngridientPuts = 0;
         }

         List<PotionEffect> toBrewEffects = new ArrayList<>();
         List<BrewUtil.PotionItemData> bottlesData = Arrays.stream(this.SLOTS_BOTTLES)
            .mapToObj(bottleSlot -> container.inventorySlots.get(bottleSlot).getStack())
            .filter(stack -> stack != null)
            .map(stack -> new BrewUtil.PotionItemData(stack))
            .toList();
         boolean flag = false;
         boolean flag2 = bottlesData.stream()
            .anyMatch(
               data -> data.getMainEffect() != null
                     && (!splash || data.isSplashBottle())
                     && (!boostEffect || data.isLeveledEffect() || data.isLongestEffect(data.getMainEffect()))
            );
         if (container.tileBrewingStand.getField(1) > 0 || this.getCountSearchItemInContainer(container, Items.BLAZE_POWDER, 4) > 0 || flag) {
            if (flag2) {
               toBrewEffects.addAll(Collections.singletonList(MobEffects.STRENGTH).stream().map(potion -> new PotionEffect(potion)).toList());
            } else {
               PotionEffect mainEffect = bottlesData.stream().map(data -> data.getMainEffect()).filter(Objects::nonNull).findAny().orElse(null);
               if (mainEffect == null || mainEffect.getPotion().getName().toLowerCase().contains("awkward")) {
                  if (brewStrength && this.getCountSearchItemInContainer(container, Items.BLAZE_POWDER, 4, 3) == 0) {
                     brewStrength = false;
                  }

                  if (brewSpeed && this.getCountSearchItemInContainer(container, Items.SUGAR, 3) == 0) {
                     brewSpeed = false;
                  }

                  if (brewResistance && this.getCountSearchItemInContainer(container, Items.MAGMA_CREAM, 3) == 0) {
                     brewResistance = false;
                  }

                  if (brewHealing && this.getCountSearchItemInContainer(container, Items.SPECKLED_MELON, 3) == 0) {
                     brewHealing = false;
                  }

                  if (brewRegen && this.getCountSearchItemInContainer(container, Items.GHAST_TEAR, 3) == 0) {
                     brewRegen = false;
                  }
               }

               if (brewStrength || brewSpeed || brewResistance || brewHealing || brewRegen) {
                  if ((
                        bottlesData.stream().anyMatch(data -> data.isWaterBottle())
                           || this.getCountSearchItemInContainer(container, Items.POTIONITEM, this.SLOTS_BOTTLES) >= 3
                     )
                     && this.getCountSearchItemInContainer(container, Items.NETHER_WART, 3) > 0) {
                     flag = true;
                  } else if (mainEffect != null
                     && bottlesData.stream().noneMatch(data -> !boostEffect || data.isLeveledEffect() || data.isLongestEffect(data.getMainEffect()))
                     && this.getCountSearchItemInContainer(
                           container, mainEffect.getPotion() == MobEffects.REGENERATION ? Items.REDSTONE : Items.GLOWSTONE_DUST, 3
                        )
                        > 0) {
                     flag = true;
                  } else if (mainEffect != null
                     && (!splash || bottlesData.stream().noneMatch(data -> data.isSplashBottle()))
                     && this.getCountSearchItemInContainer(container, Items.GUNPOWDER, 3) > 0) {
                     flag = true;
                  } else if (bottlesData.stream().anyMatch(data -> data.isActivatedBottle())) {
                     flag = true;
                  }
               }
            }

            if (flag) {
               if (brewStrength) {
                  toBrewEffects.add(new PotionEffect(MobEffects.STRENGTH));
               }

               if (brewSpeed) {
                  toBrewEffects.add(new PotionEffect(MobEffects.SPEED));
               }

               if (brewResistance) {
                  toBrewEffects.add(new PotionEffect(MobEffects.FIRE_RESISTANCE));
               }

               if (brewHealing) {
                  toBrewEffects.add(new PotionEffect(MobEffects.HEALTH_BOOST));
               }

               if (brewRegen) {
                  toBrewEffects.add(new PotionEffect(MobEffects.REGENERATION));
               }
            }
         }

         try {
            PotionEffect primaryBrewEffect = (toBrewEffects.isEmpty() ? null : toBrewEffects.get(this.countOfIngridientPuts % toBrewEffects.size())).copy();
            return this.handleBrewingStand(brewStand, container, boostEffect, splash, primaryBrewEffect, delayOperation, closeScreenAction);
         } catch (Exception var18) {
            var18.printStackTrace();
            return false;
         }
      }
   }

   private PotionEffect syncBrewPotionEffectToCurrentResources(ContainerBrewingStand container, PotionEffect prevPotionEffect) {
      int ampf = prevPotionEffect == null ? 0 : prevPotionEffect.amplifier;
      ItemStack firstEffectedPotionStack = Arrays.asList(this.SLOTS_BOTTLES[0], this.SLOTS_BOTTLES[1], this.SLOTS_BOTTLES[1])
         .stream()
         .map(slotBottle -> container.inventorySlots.get(slotBottle).getStack())
         .filter(stack -> new BrewUtil.PotionItemData(stack).getMainEffect() != null)
         .findFirst()
         .orElse(null);
      PotionEffect truePotionEffect = (firstEffectedPotionStack == null
            ? prevPotionEffect
            : new BrewUtil.PotionItemData(firstEffectedPotionStack).getMainEffect())
         .copy();
      if (truePotionEffect != null && !truePotionEffect.getEffectName().equalsIgnoreCase(prevPotionEffect.getEffectName())) {
         truePotionEffect.amplifier = ampf;
      }

      return truePotionEffect;
   }

   public boolean handleBrewingStand(
      PotionBrewer.BrewStand brewStand,
      ContainerBrewingStand container,
      boolean boostEffect,
      boolean splash,
      PotionEffect brewEffect,
      long delayOperation,
      Runnable closeScreenAction
   ) {
      brewStand.updateStacksOfContainer(container);
      if (brewStand == null) {
         closeScreenAction.run();
         return false;
      } else if (brewEffect == null) {
         brewStand.setTimeOutBlockingOpenAction();
         closeScreenAction.run();
         return false;
      } else {
         if (boostEffect) {
            brewEffect.amplifier = 1;
         }

         brewStand.updateCurrentPotionEffectToBrew(brewEffect);
         brewEffect = this.syncBrewPotionEffectToCurrentResources(container, brewEffect);
         if (boostEffect
            && this.getCountSearchItemInContainer(
                  container,
                  brewEffect.getPotion() == MobEffects.FIRE_RESISTANCE ? Items.REDSTONE : Items.GLOWSTONE_DUST,
                  3,
                  4,
                  this.SLOTS_BOTTLES[0],
                  this.SLOTS_BOTTLES[1],
                  this.SLOTS_BOTTLES[2]
               )
               == 0) {
            boostEffect = false;
         }

         if (container != null && this.operationTimer.hasReached((double)delayOperation)) {
            boolean fastMode = false;
            switch (this.putSinglePowerToStandActionInt(container)) {
               case -1:
                  if (this.stealAllPotionsIsLast(container, boostEffect, splash, fastMode, false)) {
                     this.updateStatsContainer(brewStand, container);
                  } else {
                     this.updateStatsContainer(brewStand, container);
                     brewStand.setTimeOutBlockingOpenAction();
                     closeScreenAction.run();
                  }
               case 0:
               default:
                  if (this.bottlesSlotsHasAir(container, false)) {
                     if (this.removeIngridientOfStand(container)) {
                        this.updateStatsContainer(brewStand, container);
                        return true;
                     }

                     if (this.putBottlesToStand(container, fastMode)) {
                        this.updateStatsContainer(brewStand, container);
                        return true;
                     }
                  }

                  if (this.isBrewingProgress(container)) {
                     brewStand.updateStacksOfContainer(container);
                     brewStand.updateOnBrewingProcessStarted();
                     closeScreenAction.run();
                     return true;
                  } else if (!this.bottlesSlotsHasAir(container, false) && this.putIngridientToStand(container, brewEffect, boostEffect, splash, fastMode)) {
                     if (this.updateStatsContainer(brewStand, container)) {
                        try {
                           brewStand.getTile().update();
                        } catch (Exception var11) {
                           var11.printStackTrace();
                        }

                        if (this.isBrewingProgress(container)) {
                           brewStand.updateStacksOfContainer(container);
                           closeScreenAction.run();
                        }
                     }

                     return true;
                  } else if (this.stealAllPotionsIsLast(container, boostEffect, splash, fastMode, false)) {
                     this.updateStatsContainer(brewStand, container);
                     brewStand.updateOnBrewingProcessEnded();
                     this.countOfSuccessBrews++;
                     closeScreenAction.run();
                     return false;
                  } else {
                     if (!this.isBrewingProgress(container)
                        && this.getCountSearchItemInContainer(
                              container, Items.POTIONITEM, 4, 3, this.SLOTS_BOTTLES[0], this.SLOTS_BOTTLES[1], this.SLOTS_BOTTLES[2]
                           )
                           < 3) {
                        brewStand.updateStacksOfContainer(container);
                        brewStand.updateOnBrewingProcessEnded();
                        brewStand.setTimeOutBlockingOpenAction();
                        closeScreenAction.run();
                        return true;
                     }

                     return true;
                  }
               case 1:
               case 2:
                  this.updateStatsContainer(brewStand, container);
                  return true;
            }
         } else {
            return true;
         }
      }
   }

   public boolean handleOfflineBrewingStandOnReceiveSound(PotionBrewer.BrewStand brewStand) {
      if (brewStand == null) {
         return false;
      } else {
         ItemStack stackIngridient = brewStand.getStackIngridient();
         if (stackIngridient != null && !stackIngridient.isEmpty()) {
            ItemStack[] stacksInBottleSlots = brewStand.getStacksInBottleSlots();
            boolean hasReaction = false;
            int index = -1;

            for (ItemStack stackBottle : stacksInBottleSlots) {
               index++;
               if (stackBottle != null && stackBottle.getItem() instanceof ItemPotion) {
                  ItemStack newStackBottle = PotionHelper.doReaction(stackIngridient, stackBottle);
                  if (newStackBottle != null && !newStackBottle.equals(stackBottle)) {
                     stacksInBottleSlots[index] = newStackBottle;
                     hasReaction = true;
                  }
               }
            }

            if (hasReaction) {
               brewStand.setBottleStacks(stacksInBottleSlots[0], stacksInBottleSlots[1], stacksInBottleSlots[2]);
               if (stackIngridient.stackSize > 1) {
                  brewStand.getStackIngridient().stackSize--;
               } else {
                  brewStand.setIngridientStack(new ItemStack(Items.air, 1));
               }

               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public static class PotionItemData {
      private final ItemStack stack;
      private final String lower;
      private PotionEffect mainEffect;

      public PotionItemData(ItemStack stack) {
         this.stack = stack;
         this.lower = stack == null ? "null" : stack.getDisplayName().toLowerCase();
         if (this.stack != null && !this.stack.isEmpty() && this.stack.getItem() instanceof ItemPotion) {
            List<PotionEffect> effects = net.minecraft.potion.PotionUtils.getEffectsFromStack(stack);
            this.mainEffect = effects.isEmpty() ? null : effects.get(0);
         }
      }

      public boolean isWaterBottle() {
         return this.lower.contains("пузырёк воды") || this.lower.contains("water bottle");
      }

      public boolean isAirStack() {
         return this.stack == null || this.stack.isEmpty() || this.stack.getItem() == Items.air;
      }

      public boolean isActivatedBottle() {
         return this.lower.contains("мутное") || this.lower.contains("awkward");
      }

      public boolean isSplashBottle() {
         return this.lower.contains("взрывное") || this.lower.contains("splash");
      }

      public PotionEffect getMainEffect() {
         return this.mainEffect;
      }

      private List<String> getLoreLinesAsStack(ItemStack stack) {
         List<String> tooltipsLines = stack.getTooltip(Minecraft.player, ITooltipFlag.TooltipFlags.NORMAL);

         for (int lineIndex = 0; lineIndex < tooltipsLines.size(); lineIndex++) {
            if (lineIndex == 0) {
               tooltipsLines.set(lineIndex, stack.getRarity().rarityColor + tooltipsLines.get(lineIndex));
            } else {
               tooltipsLines.set(lineIndex, TextFormatting.GRAY + tooltipsLines.get(lineIndex));
            }
         }

         return tooltipsLines.stream()
            .filter(str -> str.length() > 1)
            .map(str -> new TextComponentString(str).getFormattedText())
            .filter(Objects::nonNull)
            .map(str -> ReplaceStrUtils.fixString(ReplaceStrUtils.deformatString(str, 1)))
            .collect(Collectors.toList());
      }

      public boolean isLongestEffect(PotionEffect currentEffect) {
         if (currentEffect == null) {
            return false;
         } else {
            String contain;
            if (currentEffect.getPotion() != MobEffects.STRENGTH
               && currentEffect.getPotion() != MobEffects.SPEED
               && currentEffect.getPotion() != MobEffects.FIRE_RESISTANCE) {
               if (currentEffect.getPotion() == MobEffects.HEALTH_BOOST) {
                  return false;
               }

               if (currentEffect.getPotion() != MobEffects.REGENERATION) {
                  return false;
               }

               contain = "1:30";
            } else {
               contain = "8:00";
            }

            return this.getLoreLinesAsStack(this.stack).stream().anyMatch(lore -> lore.contains(contain));
         }
      }

      public boolean isLeveledEffect() {
         return this.getLoreLinesAsStack(this.stack).stream().anyMatch(lore -> lore.contains("II"));
      }

      public Item getCurrentBrewItemToBottle(PotionEffect currentEffect, boolean boostEffect, boolean splash) {
         PotionEffect mainEffect = this.getMainEffect();
         Potion currentPotion = currentEffect.getPotion();
         if (this.isWaterBottle()) {
            return Items.NETHER_WART;
         } else {
            if (this.isActivatedBottle()) {
               if (currentPotion == MobEffects.STRENGTH) {
                  return Items.BLAZE_POWDER;
               }

               if (currentPotion == MobEffects.SPEED) {
                  return Items.SUGAR;
               }

               if (currentPotion == MobEffects.HEALTH_BOOST) {
                  return Items.SPECKLED_MELON;
               }

               if (currentPotion == MobEffects.REGENERATION) {
                  return Items.GHAST_TEAR;
               }

               if (currentPotion == MobEffects.FIRE_RESISTANCE) {
                  return Items.MAGMA_CREAM;
               }
            } else {
               if (boostEffect
                  && mainEffect != null
                  && mainEffect.getEffectName().equalsIgnoreCase(currentEffect.getEffectName())
                  && (!splash || this.isSplashBottle())) {
                  return currentEffect.getEffectName().equalsIgnoreCase(MobEffects.FIRE_RESISTANCE.getName()) && !this.isLongestEffect(currentEffect)
                     ? Items.REDSTONE
                     : (!this.isLeveledEffect() ? Items.GLOWSTONE_DUST : null);
               }

               if (splash && mainEffect != null && !this.isSplashBottle()) {
                  return Items.GUNPOWDER;
               }
            }

            return null;
         }
      }
   }
}
