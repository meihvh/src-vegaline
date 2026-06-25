package ru.govno.client.module.modules;

import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.utils.TPSDetect;

public class GameSyncTPS extends Module {
   public static GameSyncTPS instance;
   public FloatSettings SyncPercent;
   public BoolSettings OnlyAura;

   public GameSyncTPS() {
      super("GameSyncTPS", 0, Module.Category.PLAYER);
      this.settings.add(this.SyncPercent = new FloatSettings("SyncPercent", 0.15F, 1.5F, 0.0F, this));
      this.settings.add(this.OnlyAura = new BoolSettings("OnlyAura", true, this));
      this.setDemand(0, 0);
      instance = this;
   }

   public static double getConpenseMath(double val, float strenghZeroToOne) {
      double out = val - (double)((1.0F - TPSDetect.getTPSServer() / 20.0F) * strenghZeroToOne);
      return out < 0.075F ? 0.075F : out;
   }

   public static double getGameConpense(double prevTimerSpeed, float percentCompense) {
      return !instance.isActived() || instance.OnlyAura.getBool() && HitAura.TARGET == null ? prevTimerSpeed : getConpenseMath(prevTimerSpeed, percentCompense);
   }

   @Override
   public String getDisplayName() {
      return this.getDisplayByDouble((double)this.SyncPercent.getFloat());
   }
}
