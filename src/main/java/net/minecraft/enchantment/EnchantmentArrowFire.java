package net.minecraft.enchantment;

import net.minecraft.inventory.EntityEquipmentSlot;

public class EnchantmentArrowFire extends Enchantment {
   public EnchantmentArrowFire(Enchantment.Rarity rarityIn, EntityEquipmentSlot... slots) {
      super(rarityIn, EnumEnchantmentType.BOW, slots);
      this.setName("arrowFire");
   }

   @Override
   public int getMinEnchantability(int enchantmentLevel) {
      return 20;
   }

   @Override
   public int getMaxEnchantability(int enchantmentLevel) {
      return 50;
   }

   @Override
   public int getMaxLevel() {
      return 1;
   }
}
