package ru.govno.client.module.modules;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventSendPacket;
import ru.govno.client.module.Module;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.BlockUtils;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Movement.MoveMeHelp;

public class FakePlayer extends Module {
   public static EntityOtherPlayerMP fakePlayer = null;
   private final UUID uuid = UUID.fromString("70ee432d-0a96-4137-a2c0-37cc9df67f03");

   public FakePlayer() {
      super("FakePlayer", 0, Module.Category.PLAYER);
      this.setDemand(0, 1);
   }

   @Override
   public void onRender2D(ScaledResolution sr) {
      if (mc.world != null && fakePlayer != null && Minecraft.player != null && fakePlayer != null) {
         EntityOtherPlayerMP e = fakePlayer;
         float[] rotate = RotationUtil.getNeededFacing(RotationUtil.getEyesPos(), false, e, false);
         e.rotationYaw = rotate[0];
         e.rotationYawHead = rotate[0];
         e.rotationPitch = rotate[1];
         e.rotationPitchHead = rotate[1];
         e.setPrimaryHand(Minecraft.player.getPrimaryHand());
         e.openContainer = Minecraft.player.openContainer;
         if (Minecraft.player.getActiveHand() != null && Minecraft.player.isHandActive()) {
            e.setActiveHand(Minecraft.player.getActiveHand());
         } else {
            e.resetActiveHand();
         }

         e.activeItemStack = Minecraft.player.activeItemStack;
      }
   }

   @EventTarget
   public void onPacket(EventSendPacket event) {
      if (this.actived && mc.world != null && fakePlayer != null) {
         if (event.getPacket() instanceof CPacketUseEntity packet
            && packet.getAction() == CPacketUseEntity.Action.ATTACK
            && packet.getEntityFromWorld(mc.world) != null
            && packet.getEntityFromWorld(mc.world) instanceof EntityOtherPlayerMP targetEntity
            && targetEntity == fakePlayer) {
            if (targetEntity.hurtTime > 0) {
               Minecraft.player
                  .world
                  .playSound(
                     Minecraft.player,
                     targetEntity.posX,
                     targetEntity.posY,
                     targetEntity.posZ,
                     SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE,
                     Minecraft.player.getSoundCategory(),
                     1.0F,
                     1.0F
                  );
               return;
            }

            targetEntity.hurtTime = 9;
            boolean cooled = (double)Minecraft.player.getCooledAttackStrength(1.0F) >= 0.82;
            boolean cancelGR = FreeCam.get.actived && FreeCam.fakePlayer != null && FreeCam.fakePlayer.fallDistance != 0.0F;
            if (Criticals.get.actived
               && Criticals.get.EntityHit.getBool()
               && Minecraft.player.onGround
               && !Minecraft.player.isJumping()
               && !Minecraft.player.isInWater()) {
               if (!Criticals.get.HitMode.currentMode.equalsIgnoreCase("NcpYPort") || HitAura.TARGET == null && Criticals.get.groundTicks % 3 != 0) {
                  if (Criticals.get.HitMode.currentMode.equalsIgnoreCase("MatrixStand") && MoveMeHelp.getSpeed() == 0.0) {
                     cancelGR = true;
                  } else if (Criticals.get.HitMode.currentMode.equalsIgnoreCase("MatrixElytra") && ElytraBoost.canElytra()) {
                     cancelGR = true;
                  } else if (Criticals.get.HitMode.currentMode.equalsIgnoreCase("NCP")) {
                     cancelGR = true;
                  } else if (Criticals.get.HitMode.currentMode.equalsIgnoreCase("Matrix")) {
                     cancelGR = true;
                  } else if (Criticals.get.HitMode.currentMode.equalsIgnoreCase("Matrix2") && EntityLivingBase.isMatrixDamaged) {
                     cancelGR = true;
                  } else if (!Criticals.get.HitMode.currentMode.equalsIgnoreCase("MatrixSmart")
                     || !EntityLivingBase.isMatrixDamaged && MoveMeHelp.getSpeed() != 0.0) {
                     if (Criticals.get.HitMode.currentMode.equalsIgnoreCase("MatrixLow&Ncp")) {
                        cancelGR = true;
                     }
                  } else {
                     cancelGR = true;
                  }
               } else if (!mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.offsetMinDown(1.0E-6F)).isEmpty()) {
                  cancelGR = true;
               }
            }

            boolean repulsive = Criticals.get.actived
               && Criticals.get.EntityHit.getBool()
               && Criticals.get.HitMode.currentMode.equalsIgnoreCase("Repulsive")
               && !Minecraft.player.isSprinting()
               && Minecraft.player.onGround
               && !MoveMeHelp.isMoving();
            boolean canCrit = cooled
               && !repulsive
               && (
                  !Minecraft.player.serverSprintState
                     || !Minecraft.player.isSprinting()
                     || !MoveMeHelp.moveKeysPressed() && !Minecraft.player.onGround && cancelGR
               )
               && (Minecraft.player.fallDistance != 0.0F && !Minecraft.player.onGround || cancelGR);
            boolean canKnock = cooled && !canCrit && (Minecraft.player.serverSprintState || Minecraft.player.isSprinting() || repulsive);
            boolean canSweep = cooled
               && (!Minecraft.player.serverSprintState || !Minecraft.player.isSprinting())
               && Minecraft.player.onGround
               && Minecraft.player.isCollidedVertically
               && Minecraft.player.getHeldItemMainhand().getItem() instanceof ItemSword;
            boolean canStrong = cooled && !canCrit && !canKnock && !canSweep && Minecraft.player.fallDistance == 0.0F;
            boolean canWeak = !canStrong;
            float f = (float)Minecraft.player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
            float f1 = EnchantmentHelper.getModifierForCreature(Minecraft.player.getHeldItemMainhand(), Minecraft.player.getCreatureAttribute());
            float f2 = Minecraft.player.getCooledAttackStrength(0.5F);
            f *= 0.2F + f2 * f2 * 0.8F;
            f1 *= f2;
            float damage = f1 / 2.0F + 1.6F;
            if (canCrit || canKnock || canSweep || canStrong || canWeak) {
               Minecraft.player
                  .world
                  .playSound(
                     Minecraft.player,
                     targetEntity.posX,
                     targetEntity.posY,
                     targetEntity.posZ,
                     ClientTune.get == null ? SoundEvents.ENTITY_PLAYER_HURT : ClientTune.get.reEventSound(SoundEvents.ENTITY_PLAYER_HURT),
                     Minecraft.player.getSoundCategory(),
                     1.0F,
                     1.0F
                  );
            }

            if (canCrit) {
               damage = (float)((double)damage * 1.5);
               Minecraft.player
                  .world
                  .playSound(
                     Minecraft.player,
                     targetEntity.posX,
                     targetEntity.posY,
                     targetEntity.posZ,
                     SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                     Minecraft.player.getSoundCategory(),
                     1.0F,
                     1.0F
                  );
            } else if (canKnock) {
               Minecraft.player
                  .world
                  .playSound(
                     Minecraft.player,
                     targetEntity.posX,
                     targetEntity.posY,
                     targetEntity.posZ,
                     SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK,
                     Minecraft.player.getSoundCategory(),
                     1.0F,
                     1.0F
                  );
            } else if (canSweep) {
               Minecraft.player
                  .world
                  .playSound(
                     Minecraft.player,
                     targetEntity.posX,
                     targetEntity.posY,
                     targetEntity.posZ,
                     SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                     Minecraft.player.getSoundCategory(),
                     1.0F,
                     1.0F
                  );
            } else if (canStrong) {
               Minecraft.player
                  .world
                  .playSound(
                     Minecraft.player,
                     targetEntity.posX,
                     targetEntity.posY,
                     targetEntity.posZ,
                     SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,
                     Minecraft.player.getSoundCategory(),
                     1.0F,
                     1.0F
                  );
            } else if (canWeak) {
               Minecraft.player
                  .world
                  .playSound(
                     Minecraft.player,
                     targetEntity.posX,
                     targetEntity.posY,
                     targetEntity.posZ,
                     SoundEvents.ENTITY_PLAYER_ATTACK_WEAK,
                     Minecraft.player.getSoundCategory(),
                     1.0F,
                     1.0F
                  );
            }

            if (targetEntity.getAbsorptionAmount() != 0.0F) {
               float addToDamageHP = targetEntity.getAbsorptionAmount() - damage / 2.0F + targetEntity.getAbsorptionAmount();
               targetEntity.setAbsorptionAmount(MathUtils.clamp(targetEntity.getAbsorptionAmount() - damage, 0.0F, 1000.0F));
               targetEntity.setHealth(MathUtils.clamp(targetEntity.getHealth() - addToDamageHP, 0.99F, 20.0F));
            } else {
               targetEntity.setHealth(MathUtils.clamp(targetEntity.getHealth() - damage + targetEntity.getAbsorptionAmount(), 0.99F, 20.0F));
            }

            targetEntity.limbSwingAmount = (float)targetEntity.hurtTime / 10.0F;
            if (targetEntity.getHealth() < 1.0F && targetEntity.getAbsorptionAmount() == 0.0F) {
               targetEntity.clearActivePotions();
               mc.effectRenderer.func_191271_a(targetEntity, EnumParticleTypes.TOTEM, 30);
               mc.world
                  .playSound(
                     targetEntity.posX, targetEntity.posY, targetEntity.posZ, SoundEvents.field_191263_gW, targetEntity.getSoundCategory(), 1.0F, 1.0F, false
                  );
               targetEntity.totemTick = true;
               targetEntity.totemsPopped++;
               targetEntity.totemTickOutHurt = 10;
               targetEntity.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 100, 0));
               targetEntity.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 880, 0));
               targetEntity.setAbsorptionAmount(8.0F);
               targetEntity.setHealth(1.0F);
               if (WorldRender.get.isActived() && WorldRender.get.CameraTweaks.getBool() && WorldRender.get.CameraFovRework.getBool()) {
                  WorldRender.get.fovMultiplier.to = 0.4F;
                  WorldRender.get.fovMultiplier.setAnim(WorldRender.get.fovMultiplier.to);
               }

               if (TotemPopESP.get.isActived()) {
                  TotemPopESP.get.addEffectedEntity(targetEntity);
               }
            }
         }

         if (event.getPacket() instanceof SPacketExplosion explosion) {
            if (fakePlayer.hurtTime > 1) {
               return;
            }

            Vec3d posExplosion = new Vec3d(explosion.getX(), explosion.getY(), explosion.getZ());
            boolean canDamage = BlockUtils.getDistanceAtVecToVec(fakePlayer.getPositionVector(), posExplosion) < 11.0;
            if (!canDamage) {
               return;
            }

            float damagex = (float)((9.0 - BlockUtils.getDistanceAtVecToVec(fakePlayer.getPositionVector(), posExplosion)) / 9.0) * 10.0F;
            float hp = (float)MathUtils.clamp((double)(fakePlayer.getHealth() - damagex + fakePlayer.getAbsorptionAmount()), 0.001, 20.0);
            if (fakePlayer.getAbsorptionAmount() != 0.0F) {
               fakePlayer.setAbsorptionAmount(MathUtils.clamp(fakePlayer.getAbsorptionAmount() - damagex, 0.0F, 8.0F));
            }

            if (fakePlayer.getAbsorptionAmount() == 0.0F || damagex > fakePlayer.getAbsorptionAmount()) {
               fakePlayer.setHealth((float)((int)MathUtils.clamp(hp + 1.0F, 1.0F, 20.0F)));
            }

            Minecraft.player
               .world
               .playSound(
                  Minecraft.player,
                  fakePlayer.posX,
                  fakePlayer.posY,
                  fakePlayer.posZ,
                  SoundEvents.ENTITY_PLAYER_DEATH,
                  Minecraft.player.getSoundCategory(),
                  0.5F,
                  1.0F
               );
            if (hp < 1.0F && fakePlayer.getAbsorptionAmount() < 4.0F && fakePlayer.getHeldItemOffhand().getItem() == Items.TOTEM) {
               fakePlayer.clearActivePotions();
               mc.effectRenderer.func_191271_a(fakePlayer, EnumParticleTypes.TOTEM, 30);
               mc.world
                  .playSound(fakePlayer.posX, fakePlayer.posY, fakePlayer.posZ, SoundEvents.field_191263_gW, fakePlayer.getSoundCategory(), 1.0F, 1.0F, false);
               fakePlayer.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 100, 1));
               fakePlayer.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 880, 0));
               fakePlayer.setHeldItem(EnumHand.OFF_HAND, new ItemStack(Item.getItemById(449), fakePlayer.getHeldItemOffhand().stackSize - 1));
               fakePlayer.setHealth(1.0F);
               fakePlayer.totemTick = true;
               fakePlayer.totemsPopped++;
               fakePlayer.totemTickOutHurt = 10;
               if (TotemPopESP.get.isActived()) {
                  TotemPopESP.get.addEffectedEntity(fakePlayer);
               }
            }
         }
      }
   }

   @Override
   public void onToggled(boolean actived) {
      if (!actived && mc.world != null && mc.world.getPlayerEntityByUUID(this.uuid) != null) {
         mc.world.removeEntityFromWorld(462462998);
         fakePlayer = null;
      } else if (actived && mc.world != null) {
         fakePlayer = new EntityOtherPlayerMP(mc.world, new GameProfile(UUID.fromString("70ee432d-0a96-4137-a2c0-37cc9df67f03"), "§6VegaLine§f > §cNPC§r"));
         mc.world.addEntityToWorld(462462998, fakePlayer);
         fakePlayer.copyLocationAndAnglesFrom(Minecraft.player);
         fakePlayer.setPosition(Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ);
         fakePlayer.inventory.copyInventory(Minecraft.player.inventory);
         ItemStack stack = Minecraft.player.getHeldItemOffhand();
         fakePlayer.setHeldItem(EnumHand.OFF_HAND, new ItemStack(Item.getItemById(449), 64));
         Minecraft.player.setHeldItem(EnumHand.OFF_HAND, stack);
         fakePlayer.setHealth(20.0F);
      }

      super.onToggled(actived);
   }

   @Override
   public void onUpdate() {
      if (mc.world == null) {
         this.toggleSilent(false);
      } else if (Minecraft.player != null && fakePlayer != null) {
         if (mc.world.isChunkLoaded((int)Math.floor(fakePlayer.posX), (int)Math.floor(fakePlayer.posZ), false)) {
            this.toggleSilent(false);
         }
      }
   }
}
