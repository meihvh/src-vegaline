package net.minecraft.entity.passive;

import javax.annotation.Nullable;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;

public class EntityDonkey extends AbstractChestHorse {
   public EntityDonkey(World p_i47298_1_) {
      super(p_i47298_1_);
   }

   public static void func_190699_b(DataFixer p_190699_0_) {
      AbstractChestHorse.func_190694_b(p_190699_0_, EntityDonkey.class);
   }

   @Nullable
   @Override
   protected ResourceLocation getLootTable() {
      return LootTableList.field_191190_H;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      super.getAmbientSound();
      return SoundEvents.ENTITY_DONKEY_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      super.getDeathSound();
      return SoundEvents.ENTITY_DONKEY_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_184601_1_) {
      super.getHurtSound(p_184601_1_);
      return SoundEvents.ENTITY_DONKEY_HURT;
   }

   @Override
   public boolean canMateWith(EntityAnimal otherAnimal) {
      if (otherAnimal == this) {
         return false;
      } else {
         return !(otherAnimal instanceof EntityDonkey) && !(otherAnimal instanceof EntityHorse)
            ? false
            : this.canMate() && ((AbstractHorse)otherAnimal).canMate();
      }
   }

   @Override
   public EntityAgeable createChild(EntityAgeable ageable) {
      AbstractHorse abstracthorse = (AbstractHorse)(ageable instanceof EntityHorse ? new EntityMule(this.world) : new EntityDonkey(this.world));
      this.func_190681_a(ageable, abstracthorse);
      return abstracthorse;
   }
}
