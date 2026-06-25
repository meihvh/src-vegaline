package ru.govno.client.module.modules;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemLingeringPotion;
import net.minecraft.item.ItemSplashPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketClientSettings;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import org.lwjgl.input.Keyboard;
import ru.govno.client.Client;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventSetSneak;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;

public class Derping extends Module {
   public static Derping get;
   public ModeSettings HandShaking;
   public ModeSettings SkinBlinking;
   public ModeSettings SneakFlexing;
   public ModeSettings HeadDerping;
   public ModeSettings HeadDerpMode;
   public BoolSettings DopOffhandSwingSend;
   private EnumHandSide primaryHandI;
   private int modelPartMasksI;
   private Set<EnumPlayerModelParts> setModelPartsI;
   private boolean wasInitI;
   private boolean modelPartsChangedOnDerping;
   private boolean resetChangesSendTrigger;
   private boolean renderHandFixOnSilentTrigger;
   private boolean lastSneakStatus;
   private boolean resetSneakTrigger;
   private int counterOfHandSideChanges;
   private int counterOfSneakStatusChanges;
   private final TimerHelper sneakDelayChange = TimerHelper.TimerHelperReseted();

   private long getSneakDelayChanging() {
      return 50L;
   }

   public Derping() {
      super("Derping", 0, Module.Category.MISC);
      this.settings.add(this.HandShaking = new ModeSettings("HandShaking", "None", this, new String[]{"None", "Client&Server", "Server"}));
      this.settings.add(this.SkinBlinking = new ModeSettings("SkinBlinking", "None", this, new String[]{"None", "Client&Server", "Server"}));
      this.settings.add(this.SneakFlexing = new ModeSettings("SneakFlexing", "None", this, new String[]{"None", "Client&Server", "Server"}));
      this.settings.add(this.HeadDerping = new ModeSettings("HeadDerping", "None", this, new String[]{"None", "Client", "Client&Server", "Server"}));
      this.settings
         .add(
            this.HeadDerpMode = new ModeSettings(
               "HeadDerpMode",
               "UP",
               this,
               new String[]{"UP", "DP", "YR", "YR&UP", "YR&DP", "UP&F", "DP&F", "YR&F", "YR&UP&F", "YR&DP&F"},
               () -> !this.HeadDerping.getMode().equalsIgnoreCase("None")
            )
         );
      this.settings.add(this.DopOffhandSwingSend = new BoolSettings("DopOffhandSwingSend", false, this));
      this.setDemand(0, 1);
      get = this;
   }

   private int getModelPartMasksInt(Set<EnumPlayerModelParts> setIn) {
      Set<EnumPlayerModelParts> setCopied = new HashSet<>();
      if (setIn != null) {
         setCopied.addAll(this.getModelPartsRandom());
      } else {
         setCopied.addAll(mc.gameSettings.getModelParts());
      }

      int i = 0;

      for (EnumPlayerModelParts enumplayermodelparts : setCopied) {
         i |= enumplayermodelparts.getPartMask();
      }

      return i;
   }

   private Set<EnumPlayerModelParts> getModelPartsRandom() {
      Set<EnumPlayerModelParts> set = new HashSet<>();

      for (EnumPlayerModelParts enumplayermodelparts : EnumPlayerModelParts.values()) {
         if (Math.random() >= 0.5) {
            set.add(enumplayermodelparts);
         }
      }

      return set;
   }

   private boolean initDefaultsInWorld() {
      if (this.wasInitI) {
         return true;
      } else if (Minecraft.player == null) {
         return false;
      } else {
         this.primaryHandI = EnumHandSide.RIGHT;
         this.setModelPartsI = mc.gameSettings.setModelParts;
         this.modelPartMasksI = this.getModelPartMasksInt(this.setModelPartsI);
         return this.wasInitI = true;
      }
   }

   private void resetToDefaultsOnToggle() {
      if (this.wasInitI && Minecraft.player != null) {
         mc.gameSettings.mainHand = this.primaryHandI;
         Minecraft.player.setPrimaryHand(this.primaryHandI);
         mc.gameSettings.setModelParts.clear();
         mc.gameSettings.setModelParts.addAll(this.setModelPartsI);
         this.setModelPartsI.clear();
         this.setModelPartsI.addAll(List.of(EnumPlayerModelParts.values()));
         if (mc.getConnection() != null) {
            mc.getConnection()
               .sendPacket(
                  new CPacketClientSettings(
                     mc.gameSettings.language,
                     mc.gameSettings.renderDistanceChunks,
                     mc.gameSettings.chatVisibility,
                     mc.gameSettings.chatColours,
                     this.getModelPartMasksInt(this.setModelPartsI),
                     this.primaryHandI
                  )
               );
         }

         this.renderHandFixOnSilentTrigger = false;
         if (this.resetSneakTrigger && this.lastSneakStatus) {
            boolean trueSneaking = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())
               && (mc.currentScreen == null || InvWalk.get.isActived() && InvWalk.get.AbilitySneak.getBool());
            if (trueSneaking != Minecraft.player.isSneaking()) {
               mc.getConnection()
                  .sendPacket(
                     new CPacketEntityAction(
                        Minecraft.player, trueSneaking ? CPacketEntityAction.Action.START_SNEAKING : CPacketEntityAction.Action.STOP_SNEAKING
                     )
                  );
            }
         }

         this.wasInitI = false;
      }
   }

   private void updateDerping(String handDerp, String skinDerp, String sneakDerp) {
      if (this.wasInitI) {
         boolean sendingChangesSettingsPacket = false;
         int modelPartMasksToSend = this.modelPartMasksI;
         EnumHandSide primaryHandToSend = this.primaryHandI;
         if (!handDerp.equalsIgnoreCase("None")) {
            primaryHandToSend = this.counterOfHandSideChanges % 2 == 0 ? EnumHandSide.RIGHT : EnumHandSide.LEFT;
            if (handDerp.contains("Client")) {
               mc.gameSettings.mainHand = primaryHandToSend;
               Minecraft.player.setPrimaryHand(primaryHandToSend);
               this.renderHandFixOnSilentTrigger = false;
            } else {
               mc.gameSettings.mainHand = this.primaryHandI;
               Minecraft.player.setPrimaryHand(this.primaryHandI);
               this.renderHandFixOnSilentTrigger = true;
            }

            this.counterOfHandSideChanges++;
            sendingChangesSettingsPacket = true;
            this.resetChangesSendTrigger = true;
         } else {
            this.renderHandFixOnSilentTrigger = false;
            this.counterOfHandSideChanges = 0;
         }

         if (!skinDerp.equalsIgnoreCase("None")) {
            if (skinDerp.contains("Client")) {
               mc.gameSettings.setModelParts.clear();
               mc.gameSettings.setModelParts.addAll(this.getModelPartsRandom());
               modelPartMasksToSend = this.getModelPartMasksInt(mc.gameSettings.setModelParts);
               this.modelPartsChangedOnDerping = true;
            } else {
               if (this.modelPartsChangedOnDerping) {
                  mc.gameSettings.setModelParts.clear();
                  mc.gameSettings.setModelParts.addAll(this.setModelPartsI);
                  this.modelPartsChangedOnDerping = false;
               }

               modelPartMasksToSend = this.getModelPartMasksInt(this.getModelPartsRandom());
            }

            sendingChangesSettingsPacket = true;
            this.resetChangesSendTrigger = true;
         }

         if (mc.getConnection() != null) {
            if (sendingChangesSettingsPacket) {
               mc.getConnection()
                  .sendPacket(
                     new CPacketClientSettings(
                        mc.gameSettings.language,
                        mc.gameSettings.renderDistanceChunks,
                        mc.gameSettings.chatVisibility,
                        mc.gameSettings.chatColours,
                        modelPartMasksToSend,
                        primaryHandToSend
                     )
                  );
            } else if (this.resetChangesSendTrigger) {
               mc.getConnection()
                  .sendPacket(
                     new CPacketClientSettings(
                        mc.gameSettings.language,
                        mc.gameSettings.renderDistanceChunks,
                        mc.gameSettings.chatVisibility,
                        mc.gameSettings.chatColours,
                        this.getModelPartMasksInt(this.setModelPartsI),
                        this.primaryHandI
                     )
                  );
               this.resetChangesSendTrigger = false;
            }
         }
      }
   }

   private void updateDerpingEventSetSneak(EventSetSneak event, String sneakDerp) {
      if (event != null) {
         if (this.sneakDelayChange.hasReached((double)(this.getSneakDelayChanging() - 5L))) {
            boolean canDerp = !sneakDerp.equalsIgnoreCase("None");
            if (canDerp) {
               boolean sneakSet = this.counterOfSneakStatusChanges % 2 == 0;
               if (sneakDerp.contains("Client")) {
                  this.lastSneakStatus = sneakSet;
               }

               if (sneakDerp.equalsIgnoreCase("Server")) {
                  mc.getConnection()
                     .sendPacket(
                        new CPacketEntityAction(
                           Minecraft.player, sneakSet ? CPacketEntityAction.Action.START_SNEAKING : CPacketEntityAction.Action.STOP_SNEAKING
                        )
                     );
                  this.lastSneakStatus = sneakSet;
               }

               this.counterOfSneakStatusChanges++;
               this.sneakDelayChange.reset();
               this.resetSneakTrigger = true;
            } else {
               this.counterOfSneakStatusChanges = 0;
               if (this.resetSneakTrigger) {
                  boolean trueSneaking = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())
                     && (mc.currentScreen == null || InvWalk.get.isActived() && InvWalk.get.AbilitySneak.getBool());
                  if (this.lastSneakStatus) {
                     mc.getConnection()
                        .sendPacket(
                           new CPacketEntityAction(
                              Minecraft.player, trueSneaking ? CPacketEntityAction.Action.START_SNEAKING : CPacketEntityAction.Action.STOP_SNEAKING
                           )
                        );
                  }

                  this.resetSneakTrigger = false;
               }
            }
         }

         if (this.counterOfSneakStatusChanges > 0 && this.lastSneakStatus && sneakDerp.contains("Client")) {
            event.setSneaking(this.lastSneakStatus);
         }
      }
   }

   private boolean canRotate() {
      if (HitAura.get.isActived() && HitAura.get.canRotateUpdated) {
         return false;
      } else if (ScaffWalk.get.isActived() && ScaffWalk.get.tempRotationStatus) {
         return false;
      } else if (Strafe.get.isActived() && Strafe.get.canRotate()) {
         return false;
      } else if (BowAimbot.get.isActived() && BowAimbot.get.doRotate) {
         return false;
      } else if ((!MiddleClick.get.isActived() || !MiddleClick.get.callThrowPearl && !MiddleClick.get.callThrowPearl2)
         && (!PotionThrower.get.isActived() || !PotionThrower.get.callThrowPotions)
         && (!PotionThrower.get.isActived() || !PotionThrower.get.forceThrow)
         && (!ThrowFollow.get.isActived() || ThrowFollow.get.runTicks <= 0)) {
         if (Timer.get.Stamina.getBool() && Timer.percent < 1.0 && Minecraft.player.getSpeed() < 0.008) {
            return false;
         } else if (AirStuck.ifStopMotionOrder()) {
            return false;
         } else if (Minecraft.player == null || !Minecraft.player.getFlag(7) && Minecraft.player.inventory.armorItemInSlot(2).getItem() != Items.ELYTRA) {
            ItemStack stackInHand = Minecraft.player.getHeldItemMainhand();
            if (stackInHand != null) {
               Item item = stackInHand.getItem();
               if (item instanceof ItemEnderPearl
                  || item instanceof ItemBow
                  || item instanceof ItemSplashPotion
                  || item instanceof ItemLingeringPotion
                  || item instanceof ItemFishingRod) {
                  return false;
               }
            }

            return !FreeCam.get.isActived() || FreeCam.fakePlayer == null;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void updateDerpingEventUpdate(EventPlayerMotionUpdate event, String headDerp, String headDerpMode) {
      if (event != null && Minecraft.player != null) {
         if (!headDerp.equalsIgnoreCase("None")) {
            float curYaw = Minecraft.player.rotationYawHead;
            float curPitch = Minecraft.player.rotationPitchHead;
            boolean hasRotateChanges = false;
            if (!headDerpMode.contains("F") || Minecraft.player.ticksExisted % 2 == 0) {
               if (headDerpMode.contains("UP")) {
                  curPitch = -90.0F;
               } else if (headDerpMode.contains("DP")) {
                  curPitch = 90.0F;
               }

               if (headDerpMode.contains("YR")) {
                  curYaw += 180.0F;
                  curPitch = -curPitch;
               }

               hasRotateChanges = true;
               curYaw = MathUtils.wrapAngleTo180_float(curYaw);
            }

            if (hasRotateChanges && this.canRotate()) {
               if (headDerp.contains("Client")) {
                  Minecraft.player.rotationYawHead = curYaw;
                  Minecraft.player.prevRotationYawHead = Minecraft.player.rotationYawHead;
                  Minecraft.player.rotationPitchHead = curPitch;
                  Minecraft.player.prevRotationPitchHead = Minecraft.player.rotationPitchHead;
                  Minecraft.player.renderYawOffset = RotationUtil.calcYawOffset(curYaw);
                  Minecraft.player.prevRenderYawOffset = Minecraft.player.renderYawOffset;
               }

               if (headDerp.contains("Server")) {
                  event.setYaw(curYaw);
                  event.setPitch(curPitch);
               }
            }
         }
      }
   }

   @Override
   public void onUpdate() {
      if (this.initDefaultsInWorld()) {
         this.updateDerping(this.HandShaking.getMode(), this.SkinBlinking.getMode(), this.SneakFlexing.getMode());
      }

      if (this.HandShaking.getMode().equalsIgnoreCase("None")
         && this.SkinBlinking.getMode().equalsIgnoreCase("None")
         && this.SneakFlexing.getMode().equalsIgnoreCase("None")
         && this.HeadDerping.getMode().equalsIgnoreCase("None")
         && !this.DopOffhandSwingSend.getBool()) {
         Client.msg("§f§lModules:§r §7[§l" + this.getName() + "§r§7]: включите что-нибудь.", false);
         this.toggle(false);
      }
   }

   @EventTarget
   public void onPlayerEvent(EventPlayerMotionUpdate event) {
      if (this.initDefaultsInWorld()) {
         this.updateDerpingEventUpdate(event, this.HeadDerping.getMode(), this.HeadDerpMode.getMode());
      }
   }

   @EventTarget
   public void onSetSneak(EventSetSneak event) {
      if (this.initDefaultsInWorld()) {
         this.updateDerpingEventSetSneak(event, this.SneakFlexing.getMode());
      }
   }

   public void onSwingMain() {
      if (this.DopOffhandSwingSend.getBool()) {
         mc.getConnection().sendPacket(new CPacketAnimation(EnumHand.OFF_HAND));
      }
   }

   @Override
   public void alwaysRender3D() {
      if (this.isActived() && this.renderHandFixOnSilentTrigger && Minecraft.player != null) {
         mc.gameSettings.mainHand = this.primaryHandI;
         Minecraft.player.setPrimaryHand(this.primaryHandI);
      }
   }

   @Override
   public void onToggled(boolean actived) {
      if (actived) {
         this.wasInitI = false;
      }

      this.resetToDefaultsOnToggle();
   }
}
