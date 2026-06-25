package net.minecraft.enchantment;

import net.minecraft.inventory.EntityEquipmentSlot;

public class EnchantmentWaterWorker extends Enchantment {
   public EnchantmentWaterWorker(Enchantment.Rarity rarityIn, EntityEquipmentSlot... slots) {
      super(rarityIn, EnumEnchantmentType.ARMOR_HEAD, slots);
      this.setName("waterWorker");
   }

   @Override
   public int getMinEnchantability(int enchantmentLevel) {
      return 1;
   }

   @Override
   public int getMaxEnchantability(int enchantmentLevel) {
      return this.getMinEnchantability(enchantmentLevel) + 40;
   }

   @Override
   public int getMaxLevel() {
      return 1;
   }
}
