package ru.govno.client.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemArmor;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;

public class AirStuck extends Module {
   public static AirStuck get;
   public ModeSettings Type;
   public BoolSettings StopRotation;
   public BoolSettings DeEquipElytra;
   public BoolSettings EBFlyCompatible;
   public int ticksPostDisabled;
   private final float[] onEnableRotation = new float[2];
   private final TimerHelper swapTimer = TimerHelper.TimerHelperReseted();

   public AirStuck() {
      super("AirStuck", 0, Module.Category.MOVEMENT);
      this.settings.add(this.Type = new ModeSettings("Type", "StopMotion", this, new String[]{"SpoofDead", "StopMotion"}));
      this.settings.add(this.StopRotation = new BoolSettings("StopRotation", true, this, () -> this.Type.getMode().equalsIgnoreCase("StopMotion")));
      this.settings.add(this.DeEquipElytra = new BoolSettings("DeEquipElytra", true, this, () -> this.Type.getMode().equalsIgnoreCase("StopMotion")));
      this.settings
         .add(
            this.EBFlyCompatible = new BoolSettings(
               "EBFlyCompatible",
               false,
               this,
               () -> this.DeEquipElytra.getBool()
                     && this.Type.getMode().equalsIgnoreCase("StopMotion")
                     && ElytraBoost.get.Mode.getMode().equalsIgnoreCase("Firework")
            )
         );
      get = this;
   }

   public static boolean canStopMotion() {
      try {
         return get != null
            && get.isActived()
            && (
               !get.EBFlyCompatible.isVisible()
                  || !get.EBFlyCompatible.getBool()
                  || ElytraBoost.get.isActived()
                     && ElytraBoost.get.Mode.getMode().equalsIgnoreCase("Firework")
                     && ElytraBoost.canElytra()
                     && !MoveMeHelp.moveKeysAnySideMove()
                     && !Minecraft.player.isJumping()
                     && !Minecraft.player.isSneaking()
                     && !Minecraft.player.onGround
                     && (Minecraft.player.hasNewVersionMoves ? !Minecraft.player.onGround : Minecraft.player.fallDistance > 0.0F)
            );
      } catch (Exception var1) {
         return false;
      }
   }

   public static boolean ifStopMotionOrder() {
      if (get != null && get.isActived() && get.Type.getMode().equalsIgnoreCase("StopMotion") && Minecraft.player != null && canStopMotion()) {
         Minecraft.player.motionX = 0.0;
         Minecraft.player.motionY = 0.0;
         Minecraft.player.motionZ = 0.0;
         Minecraft.player.prevLimbSwingAmount = Minecraft.player.limbSwingAmount;
         return true;
      } else {
         return false;
      }
   }

   private void setDead(boolean set) {
      if (Minecraft.player != null) {
         Minecraft.player.isDead = set;
      }
   }

   @Override
   public void alwaysUpdateLimitedDelay() {
      if (this.isActived()) {
         this.ticksPostDisabled = 0;
      } else {
         this.ticksPostDisabled++;
      }
   }

   @Override
   public void onUpdate() {
      this.setDead(this.Type.getMode().equalsIgnoreCase("SpoofDead"));
      if (this.Type.getMode().equalsIgnoreCase("StopMotion") && this.DeEquipElytra.getBool() && Minecraft.player != null) {
         if (canStopMotion()) {
            this.elySwap(false);
         } else if (Minecraft.player != null) {
            this.onEnableRotation[0] = Minecraft.player.rotationYaw;
            this.onEnableRotation[1] = Minecraft.player.rotationPitch;
            if (ElytraBoost.get.isActived() && ElytraBoost.get.Mode.getMode().equalsIgnoreCase("Firework") && ElytraBoost.canElytra()) {
               this.elySwap(true);
            }
         }
      }

      super.onUpdate();
   }

   @EventTarget(0)
   public void onUpdate(EventPlayerMotionUpdate event) {
      if (this.isActived() && canStopMotion() && this.StopRotation.getBool() && Minecraft.player != null) {
         event.setYaw(this.onEnableRotation[0]);
         event.setPitch(this.onEnableRotation[1]);
         Minecraft.player.rotationYawHead = this.onEnableRotation[0];
         Minecraft.player.renderYawOffset = this.onEnableRotation[0];
         Minecraft.player.rotationPitchHead = this.onEnableRotation[1];
      }
   }

   @Override
   public void onToggled(boolean actived) {
      if (actived && Minecraft.player != null) {
         this.onEnableRotation[0] = Minecraft.player.rotationYaw;
         this.onEnableRotation[1] = Minecraft.player.rotationPitch;
      }

      this.setDead(actived && this.Type.getMode().equalsIgnoreCase("SpoofDead"));
      if (this.Type.getMode().equalsIgnoreCase("StopMotion") && this.DeEquipElytra.getBool() && !this.EBFlyCompatible.getBool()) {
         if (actived) {
            this.elySwap(false);
         } else {
            this.elySwap(true);
            mc.timer.tempSpeed = 0.5;
            Timer.forceTimer(0.5F);
         }
      }

      super.onToggled(actived);
   }

   private long delaySwap() {
      return 200L;
   }

   private void elySwap(boolean eq) {
      if (this.swapTimer.hasReached((double)this.delaySwap()) && Minecraft.player != null) {
         if (eq) {
            if (Minecraft.player.inventory.armorItemInSlot(2).getItem() != Items.ELYTRA) {
               if (Minecraft.player.inventory.armorItemInSlot(2).getItem() != Items.air) {
                  ElytraBoost.oldSlot = Minecraft.player.inventory.armorItemInSlot(2).getItem();
               }

               int ely = -1;
               if (ElytraBoost.GETGOVNO(Items.ELYTRA) != -1) {
                  ElytraBoost.equipElytra2();
                  this.swapTimer.reset();
               } else if ((ely = ElytraBoost.getItemElytra()) != -1) {
                  try {
                     int handSlot = Minecraft.player.inventory.currentItem;
                     mc.playerController.windowClick(0, ely, handSlot, ClickType.SWAP, Minecraft.player);
                     mc.playerController.windowClick(0, 6, handSlot, ClickType.SWAP, Minecraft.player);
                     mc.playerController.windowClick(0, ely, handSlot, ClickType.SWAP, Minecraft.player);
                     this.swapTimer.reset();
                  } catch (Exception var5) {
                     var5.printStackTrace();
                  }
               } else if (Minecraft.player.inventory.armorItemInSlot(2).getItem() != Items.ELYTRA) {
                  ElytraBoost.eq();
                  this.swapTimer.reset();
               }
            }
         } else if (!(Minecraft.player.inventory.armorItemInSlot(2).getItem() instanceof ItemArmor)) {
            int old = -1;
            if (ElytraBoost.GETGOVNO(ElytraBoost.oldSlot) != -1) {
               ElytraBoost.dequipElytra2();
               this.swapTimer.reset();
            } else if ((old = ElytraBoost.getOldItem()) != -1) {
               try {
                  int handSlot = Minecraft.player.inventory.currentItem;
                  mc.playerController.windowClick(0, old, handSlot, ClickType.SWAP, Minecraft.player);
                  mc.playerController.windowClick(0, 6, handSlot, ClickType.SWAP, Minecraft.player);
                  mc.playerController.windowClick(0, old, handSlot, ClickType.SWAP, Minecraft.player);
                  this.swapTimer.reset();
               } catch (Exception var4) {
                  var4.printStackTrace();
               }
            }

            if (Minecraft.player.getFlag(7)) {
               Minecraft.player.setFlag(7, false);
               Minecraft.player.motionY = 0.0;
               Minecraft.player.multiplyMotionXZ(0.6F);
            }
         }
      }
   }
}
