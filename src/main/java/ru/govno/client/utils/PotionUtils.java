package ru.govno.client.utils;

import java.util.Arrays;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class PotionUtils {
   private static final Minecraft mc = Minecraft.getMinecraft();

   public static String getPotionType(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return "null";
      } else {
         String minecraftPotionType = itemStack.getItem().getUnlocalizedName();

         return switch (minecraftPotionType) {
            case "item.potion" -> "Drink";
            case "item.splash_potion" -> "Throw";
            case "item.lingering_potion" -> "Suspens";
            default -> "null";
         };
      }
   }

   public static OptionalInt findSlotByItem(Container container, Item item) {
      return IntStream.range(0, container.inventorySlots.size()).filter(i -> {
         ItemStack stack = container.getSlot(i).getStack();
         return !stack.isEmpty() && stack.getItem() == item;
      }).findFirst();
   }

   public static int[] findSlotsByItemsExcludingBlackSlots(Container container, Item[] items, int[] blackSlots) {
      return IntStream.range(0, items.length)
         .mapToObj(i -> findSlotByItem(container, items[i]))
         .filter(optionalSlot -> optionalSlot.isPresent() && !IntStream.of(blackSlots).anyMatch(slot -> slot == optionalSlot.getAsInt()))
         .mapToInt(OptionalInt::getAsInt)
         .toArray();
   }

   public static String getPotionEffectType(ItemStack itemStack) {
      if (!itemStack.isEmpty() && itemStack.getTagCompound() != null) {
         NBTTagCompound tagCompound = itemStack.getTagCompound();
         String effectType = tagCompound.toString();

         return switch (effectType) {
            case "{Potion:\"minecraft:water\"}" -> "Water";
            case "{Potion:\"minecraft:mundane\"}" -> "Mundane";
            case "{Potion:\"minecraft:awkward\"}" -> "Awkward";
            case "{Potion:\"minecraft:thick\"}" -> "Thick";
            case "{Potion:\"minecraft:strength\"}" -> "Strength";
            case "{Potion:\"minecraft:long_strength\"}" -> "LongStrength";
            case "{Potion:\"minecraft:strong_strength\"}" -> "StrongStrength";
            case "{Potion:\"minecraft:swiftness\"}" -> "Speed";
            case "{Potion:\"minecraft:long_swiftness\"}" -> "LongSpeed";
            case "{Potion:\"minecraft:strong_swiftness\"}" -> "StrongSpeed";
            case "{Potion:\"minecraft:fire_resistance\"}" -> "Resistance";
            case "{Potion:\"minecraft:long_fire_resistance\"}" -> "LongResistance";
            case "{Potion:\"minecraft:regeneration\"}" -> "Regeneration";
            case "{Potion:\"minecraft:long_regeneration\"}" -> "LongRegeneration";
            case "{Potion:\"minecraft:strong_regeneration\"}" -> "StrongRegeneration";
            case "{Potion:\"minecraft:healing\"}" -> "Healing";
            case "{Potion:\"minecraft:strong_healing\"}" -> "StrongHealing";
            default -> "null";
         };
      } else {
         return "null";
      }
   }

   public static boolean checkSlotsForPotion(String[] desiredTypes, String[] desiredEffectTypes, int... slots) {
      int matchingCount = (int)Arrays.stream(slots).filter(i -> {
         Slot slot = Minecraft.player.openContainer.inventorySlots.get(i);
         ItemStack itemStack = slot.getStack();
         String potionType = getPotionType(itemStack);
         String effectType = getPotionEffectType(itemStack);
         return Arrays.stream(desiredTypes).anyMatch(potionType::equals) && Arrays.stream(desiredEffectTypes).anyMatch(effectType::equals);
      }).count();
      return matchingCount > 0;
   }

   public static int findPotionWithEffect(String desiredType, String desiredEffectType) {
      for (int i = 0; i < 45; i++) {
         ItemStack itemStack = Minecraft.player.inventory.getStackInSlot(i);
         String potionType = getPotionType(itemStack);
         String effectType = getPotionEffectType(itemStack);
         if (desiredType.equals(potionType) && desiredEffectType.equals(effectType)) {
            return i < 9 ? i + 36 : i;
         }
      }

      return -1;
   }

   public static int findPotionWithEffectContainer(String[] desiredTypes, String[] desiredEffectTypes, int... blackslots) {
      return IntStream.range(0, Minecraft.player.openContainer.inventorySlots.size())
         .filter(
            i -> {
               Slot slot = Minecraft.player.openContainer.inventorySlots.get(i);
               ItemStack itemStack = slot.getStack();
               String potionType = getPotionType(itemStack);
               String effectType = getPotionEffectType(itemStack);
               return Arrays.stream(desiredTypes).anyMatch(potionType::equals)
                  && Arrays.stream(desiredEffectTypes).anyMatch(effectType::equals)
                  && Arrays.stream(blackslots).noneMatch(blackslot -> blackslot == i);
            }
         )
         .findFirst()
         .orElse(-1);
   }

   public static int findFirstMatchingSlot(String[] desiredTypes, String[] desiredEffectTypes, int... slots) {
      return IntStream.of(slots).filter(i -> {
         Slot slot = Minecraft.player.openContainer.inventorySlots.get(i);
         ItemStack itemStack = slot.getStack();
         String potionType = getPotionType(itemStack);
         String effectType = getPotionEffectType(itemStack);
         return Arrays.stream(desiredTypes).anyMatch(potionType::equals) && Arrays.stream(desiredEffectTypes).anyMatch(effectType::equals);
      }).findFirst().orElse(-1);
   }

   public static int countPotionsWithEffectInInventory(String[] desiredTypes, String[] desiredEffectTypes, int... blackslots) {
      return (int)IntStream.range(0, 45).filter(i -> Arrays.stream(blackslots).noneMatch(blackslot -> blackslot == i)).filter(i -> {
         ItemStack itemStack = Minecraft.player.inventory.getStackInSlot(i);
         if (itemStack.isEmpty()) {
            return false;
         } else {
            String potionType = getPotionType(itemStack);
            String effectType = getPotionEffectType(itemStack);
            return Arrays.stream(desiredTypes).anyMatch(potionType::equals) && Arrays.stream(desiredEffectTypes).anyMatch(effectType::equals);
         }
      }).count();
   }
}
