package ru.govno.client.module.modules;

import net.minecraft.client.Minecraft;
import ru.govno.client.Client;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.UControllers.DualsenseSettings;

public class DualsenseSupport extends Module {
   public static DualsenseSupport get;
   public FloatSettings inDeadZoneAdaptive;
   public ModeSettings dxUiShowMode;
   public ModeSettings dxMoveStickMode;
   private boolean flagShowGuides = false;

   public DualsenseSupport() {
      super("DualsenseSupport", 0, Module.Category.MISC);
      this.settings.add(this.inDeadZoneAdaptive = new FloatSettings("InDeadZoneAdaptive", 0.08F, 0.825F, 0.01F, this));
      this.settings.add(this.dxUiShowMode = new ModeSettings("UiShowMode", "Always", this, new String[]{"Always", "Bindings&Settings", "SettingsOnly"}));
      this.settings
         .add(this.dxMoveStickMode = new ModeSettings("MoveStickMode", "HertzWasd", this, new String[]{"HertzWasd", "Wasd", "HertzWasd&Jump", "Wasd&Jump"}));
      this.setDemand(3, 1);
      get = this;
   }

   @Override
   public boolean isBetaModule() {
      return true;
   }

   public static boolean canUseDualsenseGamepadForGaming() {
      return get != null && get.isActived();
   }

   public static float getInDeadZoneAdaptive() {
      return get == null ? 0.1F : get.inDeadZoneAdaptive.getFloat();
   }

   public static String getUiShowMode() {
      return get == null ? "Never" : get.dxUiShowMode.getMode();
   }

   public static String getMoveMode() {
      return get == null ? "HertzWasd" : get.dxMoveStickMode.getMode();
   }

   @Override
   public void onUpdateLimitedDelay() {
      if (this.flagShowGuides && mc.world != null && Minecraft.player != null && Minecraft.player.ticksExisted > 40) {
         Client.msg("§f§lModules:§r §7[§l" + this.getName() + "§r§7] гайд по использованию.", false);

         for (String lineGuide : DualsenseSettings.bindingGuideTexts()) {
            Client.msg(lineGuide, false);
         }

         this.flagShowGuides = false;
      }
   }
}
