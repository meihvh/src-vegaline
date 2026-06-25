package ru.govno.client.module.modules;

import net.minecraft.client.gui.ScaledResolution;
import optifine.Config;
import ru.govno.client.Client;
import ru.govno.client.cfg.GuiConfig;
import ru.govno.client.clickgui.ClickGuiScreen;
import ru.govno.client.clickgui.Panel;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Render.AnimationUtils;

public class ClickGui extends Module {
   public static ClickGui instance;
   public static AnimationUtils categoryColorFactor = new AnimationUtils(0.0F, 0.0F, 0.015F);
   public BoolSettings Images;
   public BoolSettings Gradient;
   public BoolSettings BlurBackground;
   public BoolSettings Descriptions;
   public BoolSettings CustomCursor;
   public BoolSettings Darkness;
   public BoolSettings Particles;
   public BoolSettings Epilepsy;
   public BoolSettings FuturisticMods;
   public BoolSettings ModsPreviewImages;
   public BoolSettings AlwaysModsPreview;
   public BoolSettings PreviewDemand;
   public BoolSettings CategoryColor;
   public BoolSettings MusicInGui;
   public BoolSettings ScanLinesOverlay;
   public BoolSettings ScreenBounds;
   public BoolSettings SaveMusic;
   public FloatSettings GradientAlpha;
   public FloatSettings BlurStrengh;
   public FloatSettings ImageBlurOpacity;
   public FloatSettings DarkOpacity;
   public FloatSettings MusicVolume;
   public ModeSettings Image;
   public ModeSettings Song;
   boolean doForceMusicChange = true;
   int savedScale = -1;
   private static boolean onDisableMusicInGuiIsEnabled = false;
   public float FuturisticModsGet;
   public float PreviewDemandGet;

   public ClickGui() {
      super("ClickGui", 29, Module.Category.RENDER);
      instance = this;
      int x = 80;

      for (int i = 0; i < 5; i++) {
         this.settings.add(new FloatSettings("P" + i + "X", (float)x, 10000.0F, -10000.0F, this, () -> false));
         this.settings.add(new FloatSettings("P" + i + "Y", 20.0F, 10000.0F, -10000.0F, this, () -> false));
         x += 135;
      }

      this.settings.add(this.Images = new BoolSettings("Images", false, this));
      this.settings
         .add(
            this.Image = new ModeSettings(
               "Image",
               "Sage",
               this,
               new String[]{
                  "Nolik",
                  "Succubbus",
                  "SuccubbusHot",
                  "AstolfoHot",
                  "Furry",
                  "Lake",
                  "Kiskis",
                  "IceGirl",
                  "LoliGirl",
                  "LoliGirl2",
                  "PandaPo",
                  "Sage",
                  "SonicGenerations",
                  "SonicMovie",
                  "PlayStationSFW",
                  "PlayStationNSFW"
               },
               () -> this.Images.getBool()
            )
         );
      this.settings.add(this.Gradient = new BoolSettings("Gradient", false, this));
      this.settings.add(this.GradientAlpha = new FloatSettings("GradientAlpha", 70.0F, 255.0F, 0.0F, this, () -> this.Gradient.getBool()));
      this.settings.add(this.BlurBackground = new BoolSettings("BlurBackground", true, this, () -> System.getProperty("os.name").startsWith("Windows")));
      this.settings
         .add(
            this.BlurStrengh = new FloatSettings(
               "BlurStrengh", 1.6F, 3.0F, 0.25F, this, () -> this.BlurBackground.getBool() && System.getProperty("os.name").startsWith("Windows")
            )
         );
      this.settings
         .add(
            this.ImageBlurOpacity = new FloatSettings(
               "ImageBlurOpacity",
               190.0F,
               255.0F,
               0.0F,
               this,
               () -> this.BlurBackground.getBool() && this.Images.getBool() && System.getProperty("os.name").startsWith("Windows")
            )
         );
      this.settings.add(this.Descriptions = new BoolSettings("Descriptions", true, this));
      this.settings.add(this.CustomCursor = new BoolSettings("CustomCursor", false, this));
      this.settings.add(this.Darkness = new BoolSettings("Darkness", false, this));
      this.settings.add(this.DarkOpacity = new FloatSettings("DarkOpacity", 170.0F, 255.0F, 0.0F, this, () -> this.Darkness.getBool()));
      this.settings.add(this.Particles = new BoolSettings("Particles", false, this));
      this.settings.add(this.Epilepsy = new BoolSettings("Epilepsy", false, this));
      this.settings.add(this.FuturisticMods = new BoolSettings("FuturisticMods", true, this).setAnimSpeed(0.035F));
      this.settings.add(this.ModsPreviewImages = new BoolSettings("ModsPreviewImages", true, this).setAnimSpeed(0.035F));
      this.settings
         .add(this.AlwaysModsPreview = new BoolSettings("AlwaysModsPreview", false, this, () -> this.ModsPreviewImages.getBool()).setAnimSpeed(0.035F));
      this.settings.add(this.PreviewDemand = new BoolSettings("PreviewDemand", true, this));
      BoolSettings set;
      this.settings.add(this.CategoryColor = set = new BoolSettings("CategoryColor", false, this));
      categoryColorFactor.to = set.getBool() ? 1.0F : 0.0F;
      categoryColorFactor.setAnim(set.getBool() ? 1.0F : 0.0F);
      this.settings.add(this.MusicInGui = new BoolSettings("MusicInGui", true, this));
      this.settings
         .add(
            this.Song = new ModeSettings(
               "Song",
               "PerUpdate",
               this,
               new String[]{
                  "PerUpdate",
                  "Ost-RA-1",
                  "Ost-RA-2",
                  "Ost-RA-3",
                  "Ost-RA-4",
                  "Astronomical",
                  "CoolingSprings",
                  "Ost-SF-1",
                  "Ost-SF-2",
                  "Ost-SF-3",
                  "Ambient-1",
                  "Ambient-2"
               },
               () -> this.MusicInGui.getBool()
            )
         );
      this.settings.add(this.MusicVolume = new FloatSettings("MusicVolume", 60.0F, 200.0F, 5.0F, this, () -> this.MusicInGui.getBool()));
      this.settings.add(this.ScanLinesOverlay = new BoolSettings("ScanLinesOverlay", false, this, () -> System.getProperty("os.name").startsWith("Windows")));
      this.settings.add(this.ScreenBounds = new BoolSettings("ScreenBounds", false, this));
      this.settings.add(this.SaveMusic = new BoolSettings("SaveMusic", false, this, () -> false));
      this.setDemand(3, 3);
   }

   public static float[] getPositionPanel(Panel curPanel) {
      float X = 0.0F;
      float Y = 0.0F;
      int i = 0;

      for (Panel panel : Client.clickGuiScreen.panels) {
         if (panel == curPanel) {
            X = instance.currentFloatValue("P" + i + "X");
            Y = instance.currentFloatValue("P" + i + "Y");
         }

         i++;
      }

      return new float[]{X, Y};
   }

   public static void setPositionPanel(Panel curPanel, float x, float y) {
      int i = 0;

      for (Panel panel : Client.clickGuiScreen.panels) {
         if (panel == curPanel) {
            ((FloatSettings)instance.settings.get(Integer.valueOf(i))).setFloat(x);
            ((FloatSettings)instance.settings.get(Integer.valueOf(i + 1))).setFloat(y);
         }

         i += 2;
      }
   }

   @Override
   public void onToggled(boolean actived) {
      boolean playMusic = (
            actived
               || this.SaveMusic.getBool() && (!RadioPlayer.get.isActived() || RadioPlayer.get.radioPlayer == null || !RadioPlayer.get.radioPlayer.isPlaying())
         )
         && this.MusicInGui.getBool();
      Client.clickGuiMusic.setPlaying(playMusic);
      if (actived) {
         if (Client.clickGuiScreen != null) {
            mc.displayGuiScreen(Client.clickGuiScreen);
         }

         int i = 0;

         for (Panel panel : Client.clickGuiScreen.panels) {
            panel.X = instance.currentFloatValue("P" + i + "X");
            panel.Y = instance.currentFloatValue("P" + i + "Y");
            panel.posX.to = panel.X;
            panel.posY.to = panel.Y;
            panel.posX.setAnim(panel.posX.to);
            panel.posY.setAnim(panel.posY.to);
            i++;
         }
      } else if (mc.currentScreen == Client.clickGuiScreen) {
         ClickGuiScreen.colose = true;
         ClickGuiScreen.scale.to = 0.0F;
         ClickGuiScreen.globalAlpha.to = 0.0F;
         ClientTune.get.playGuiScreenOpenOrCloseSong(false);
      }

      super.onToggled(actived);
   }

   public static void updateAll() {
      if (instance != null) {
         if (instance.MusicInGui.getBool()) {
            if (RadioPlayer.get.isActived() && !RadioPlayer.get.StopInClickGui.getBool() && !onDisableMusicInGuiIsEnabled) {
               onDisableMusicInGuiIsEnabled = instance.MusicInGui.getBool();
               Client.msg("§f§lModules:§r §7[§l" + instance.getName() + "§r§7]: MusicInGui недоступен", false);
               Client.msg("§f§lModules:§r §7[§l" + instance.getName() + "§r§7]: Включен мод RadioPlayer", false);
               instance.MusicInGui.setBool(false);
            }
         } else if (onDisableMusicInGuiIsEnabled && !RadioPlayer.get.isActived() && !RadioPlayer.get.StopInClickGui.getBool() && !instance.MusicInGui.getBool()
            )
          {
            Client.msg("§f§lModules:§r §7[§l" + instance.getName() + "§r§7]: MusicInGui снова доступен и работает", false);
            instance.MusicInGui.setBool(onDisableMusicInGuiIsEnabled);
            onDisableMusicInGuiIsEnabled = false;
         }

         boolean playMusic = instance.MusicInGui.getBool();
         String track = "foneticmusic1";
         String var2 = instance.Song.currentMode;
         switch (var2) {
            case "PerUpdate":
               track = "foneticmusicPUVL";
               break;
            case "Ost-RA-1":
               track = "foneticmusic1";
               break;
            case "Ost-RA-2":
               track = "foneticmusic2";
               break;
            case "Ost-RA-3":
               track = "foneticmusic3";
               break;
            case "Ost-RA-4":
               track = "foneticmusic4";
               break;
            case "Astronomical":
               track = "foneticmusic5";
               break;
            case "CoolingSprings":
               track = "foneticmusic6";
               break;
            case "Ost-SF-1":
               track = "foneticmusic7";
               break;
            case "Ost-SF-2":
               track = "foneticmusic8";
               break;
            case "Ost-SF-3":
               track = "foneticmusic9";
               break;
            case "Ambient-1":
               track = "foneticmusic10";
               break;
            case "Ambient-2":
               track = "foneticmusic11";
         }

         if (playMusic) {
            if (instance.doForceMusicChange) {
               Client.clickGuiMusic.setTrackNameForce(track);
               instance.doForceMusicChange = false;
            } else {
               Client.clickGuiMusic.setTrackName(track);
            }

            Client.clickGuiMusic.setMaxVolume(instance.MusicVolume.getFloat() / 200.0F);
         }

         Client.clickGuiMusic
            .setPlaying(
               playMusic
                  && (
                     mc.currentScreen == Client.clickGuiScreen && !ClickGuiScreen.colose
                        || mc.currentScreen instanceof GuiConfig
                        || instance.SaveMusic.getBool()
                           && (!RadioPlayer.get.isActived() || RadioPlayer.get.radioPlayer == null || !RadioPlayer.get.radioPlayer.isPlaying())
                  )
                  && !Client.mainGuiNoise.wantPlayTrack()
            );
         if (Client.mainGuiNoise.wantPlayTrack()) {
            Client.clickGuiMusic.setVolumePC(Client.clickGuiMusic.lastVolume() / 3.0F);
         }
      }
   }

   @Override
   public void alwaysRender2D(ScaledResolution sr) {
      this.FuturisticModsGet = this.FuturisticMods.getAnimation();
      this.PreviewDemandGet = this.PreviewDemand.getAnimation();
   }

   @Override
   public void onUpdate() {
      if (mc.currentScreen != Client.clickGuiScreen && !(mc.currentScreen instanceof GuiConfig)) {
         this.toggleSilent(false);
      } else {
         boolean categoryFactored = this.CategoryColor.getBool();
         if (categoryColorFactor.to == 1.0F != categoryFactored) {
            categoryColorFactor.to = categoryFactored ? 1.0F : 0.0F;
         }

         if (Config.isShaders() && this.BlurBackground.getBool()) {
            this.BlurBackground.setBool(false);
            ClientTune.get.playGuiScreenCheckBox(false);
            Client.msg("§f§lModules:§r §7[§l" + this.name + "§r§7]: выключите шейдеры для использования BlurBackground.", false);
         }
      }
   }
}
