package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketCustomSound;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import ru.govno.client.cfg.Config;
import ru.govno.client.clickgui.CheckBox;
import ru.govno.client.clickgui.ClickGuiScreen;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.DisplayCheck;
import ru.govno.client.utils.MusicHelper;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.MacroMngr.Macros;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.UControllers.DualsenseController;

public class ClientTune extends Module {
   public static ClientTune get;
   public BoolSettings Modules;
   public BoolSettings Module;
   public BoolSettings ClickGui;
   public BoolSettings MiddleClick;
   public BoolSettings Containers;
   public BoolSettings Macroses;
   public BoolSettings RTSoundSurround;
   public BoolSettings RTPerfomanceMode;
   public BoolSettings RTOcclusion;
   public BoolSettings RTXDebugView;
   public BoolSettings SoundsTimeSync;
   public BoolSettings HurtSounds;
   public BoolSettings ReturnPearlSFX;
   public BoolSettings MuteGameOnMinimized;
   public ModeSettings Module1;
   public ModeSettings GuiPreset;
   public ModeSettings HurtSound;
   public ModeSettings MuteVolumeLevel;
   public FloatSettings ModuleVolume;
   private String prevModule1SettingValue;
   private int soundTestTicks;
   private final List<EntityEnderPearl> pearlsList = new ArrayList<>();
   private static final AnimationUtils muffleVolumeSmooth = new AnimationUtils(1.0F, 1.0F, 0.05F);

   public ClientTune() {
      super("ClientTune", 0, Category.MISC, true);
      this.settings.add(this.Modules = new BoolSettings("Modules", true, this));
      this.settings
         .add(
            this.Module1 = new ModeSettings(
               "Module",
               "Echo",
               this,
               new String[]{
                  "VL",
                  "Dev",
                  "Discord",
                  "Sigma",
                  "Akrien",
                  "Hanabi",
                  "Tone",
                  "Alarm",
                  "Heavy",
                  "Speech",
                  "SpeechEcho",
                  "Frontiers",
                  "Sweep",
                  "Nurik",
                  "Basic",
                  "Baloon",
                  "BaloonFast",
                  "Ori",
                  "Ori2",
                  "Airpods",
                  "Wood",
                  "Relake",
                  "Retro",
                  "Echo",
                  "Vibra"
               },
               () -> this.Modules.getBool()
            )
         );
      this.settings.add(this.ModuleVolume = new FloatSettings("ModuleVolume", 120.0F, 200.0F, 10.0F, this, () -> this.Modules.getBool()));
      this.settings.add(this.ClickGui = new BoolSettings("ClickGui", true, this));
      this.settings.add(this.GuiPreset = new ModeSettings("GuiPreset", "Sleek", this, new String[]{"Sleek", "Modern"}, () -> this.ClickGui.getBool()));
      this.settings.add(this.MiddleClick = new BoolSettings("MiddleClick", true, this));
      this.settings.add(this.Containers = new BoolSettings("Containers", false, this));
      this.settings.add(this.Macroses = new BoolSettings("Macroses", true, this));
      this.settings.add(this.RTSoundSurround = new BoolSettings("RTSoundSurround", true, this));
      this.settings.add(this.RTPerfomanceMode = new BoolSettings("RTPerfomanceMode", false, this, () -> this.RTSoundSurround.getBool()));
      this.settings
         .add(this.RTOcclusion = new BoolSettings("RTOcclusion", false, this, () -> this.RTSoundSurround.getBool() && !this.RTPerfomanceMode.getBool()));
      this.settings.add(this.RTXDebugView = new BoolSettings("RTXDebugView", false, this, () -> this.RTSoundSurround.getBool()));
      this.settings.add(this.SoundsTimeSync = new BoolSettings("SoundsTimeSync", true, this));
      this.settings.add(this.HurtSounds = new BoolSettings("HurtSounds", false, this));
      this.settings
         .add(
            this.HurtSound = new ModeSettings(
               "HurtSound",
               "Blaze",
               this,
               new String[]{
                  "Cow",
                  "Chicken",
                  "Blaze",
                  "Wolf",
                  "Pig",
                  "Skeleton",
                  "Zombie",
                  "Thorns",
                  "HorseGallop",
                  "BlastFirework",
                  "ItemBreak",
                  "Shulker",
                  "Snow",
                  "Spit",
                  "ChickPutEgg",
                  "EnderEye",
                  "Bell",
                  "Wither",
                  "GlassBreak",
                  "PistonPush",
                  "Exp"
               },
               () -> this.HurtSounds.getBool()
            )
         );
      this.settings.add(this.ReturnPearlSFX = new BoolSettings("ReturnPearlSFX", true, this));
      this.settings.add(this.MuteGameOnMinimized = new BoolSettings("MuteGameOnMinimized", false, this));
      this.settings
         .add(
            this.MuteVolumeLevel = new ModeSettings(
               "MuteVolumeLevel",
               "FullMute",
               this,
               new String[]{"FullMute", "MuchQuieter", "Quieter", "LittleQuieter"},
               () -> this.MuteGameOnMinimized.getBool()
            )
         );
      this.setDemand(0, 3);
      get = this;
   }

   @Override
   public void onToggled(boolean actived) {
      this.soundTestTicks = 0;
      muffleVolumeSmooth.setAnim(1.0F);
      muffleVolumeSmooth.to = 1.0F;
      super.onToggled(actived);
   }

   @Override
   public void onAlwaysPostRenderThread() {
      boolean mute = this.isActived() && this.MuteGameOnMinimized.getBool() && !DisplayCheck.isVisible();
      if (mute) {
         String var2 = this.MuteVolumeLevel.getMode();
         switch (var2) {
            case "FullMute":
               muffleVolumeSmooth.to = 0.0F;
               break;
            case "MuchQuieter":
               muffleVolumeSmooth.to = 0.15F;
               break;
            case "Quieter":
               muffleVolumeSmooth.to = 0.375F;
               break;
            case "LittleQuieter":
               muffleVolumeSmooth.to = 0.5F;
         }
      } else {
         muffleVolumeSmooth.to = 1.0F;
         if (muffleVolumeSmooth.anim > 0.95F) {
            muffleVolumeSmooth.anim = 1.0F;
         }
      }

      if (this.isActived()) {
         muffleVolumeSmooth.getAnim();
         muffleVolumeSmooth.speed = 0.04F;
      }
   }

   @Override
   public void onUpdateLimitedDelay() {
      if (this.Modules.getBool()) {
         if (this.prevModule1SettingValue == null) {
            this.prevModule1SettingValue = this.Module1.getMode();
         }

         if (!this.prevModule1SettingValue.equalsIgnoreCase(this.Module1.getMode())) {
            this.soundTestTicks = 8;
            this.prevModule1SettingValue = this.Module1.getMode();
         }

         if (this.soundTestTicks > 0) {
            switch (this.soundTestTicks) {
               case 1:
                  this.playModule(false);
                  break;
               case 6:
                  this.playModule(true);
            }

            this.soundTestTicks--;
         }
      }

      if (this.ReturnPearlSFX.getBool() && mc.world != null) {
         for (EntityEnderPearl pearl : this.pearlsList) {
            if (pearl.isDead) {
               EntityPlayer firstNearedThrower = pearl.getFirstNeareblePlayerAsThrower();
               if (!pearl.check2
                  && (firstNearedThrower == null || firstNearedThrower.getDistanceToVec3d(pearl.getPositionVector()) <= firstNearedThrower.getSpeed() + 0.2)) {
                  mc.world
                     .playSound(
                        (EntityPlayer)(firstNearedThrower == null ? Minecraft.player : firstNearedThrower),
                        pearl.posX,
                        pearl.posY,
                        pearl.posZ,
                        SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F
                     );
                  pearl.check2 = true;
               }
            }
         }

         this.pearlsList.clear();
         List<EntityEnderPearl> pearlsInWorld = mc.world.getLoadedEntityList().stream().map(Entity::getEntityEnderPearlOf).filter(Objects::nonNull).toList();
         this.pearlsList.addAll(Objects.requireNonNull(pearlsInWorld));
      }
   }

   public static float getVolumeMuteMultiplierAlways() {
      return muffleVolumeSmooth == null ? 1.0F : muffleVolumeSmooth.anim;
   }

   public boolean onSoundIsCancel(SPacketSoundEffect packetIn) {
      if (this.actived
         && mc.world != null
         && packetIn != null
         && packetIn.getSound() != null
         && this.HurtSounds.getBool()
         && packetIn.getCategory() == SoundCategory.PLAYERS
         && packetIn.getSound().getSoundName().getResourcePath().contains("entity.player.hurt")
         && Minecraft.player != null
         && Minecraft.player.getDistance(packetIn.getX(), packetIn.getY(), packetIn.getZ()) > 0.3F) {
         mc.world
            .playSound(
               Minecraft.player,
               packetIn.getX(),
               packetIn.getY(),
               packetIn.getZ(),
               this.reEventSound(packetIn.getSound()),
               SoundCategory.PLAYERS,
               packetIn.getVolume(),
               packetIn.getPitch()
            );
         return true;
      } else {
         return false;
      }
   }

   public boolean onSoundIsCancel(SPacketCustomSound packetIn) {
      if (this.actived
         && mc.world != null
         && packetIn != null
         && this.HurtSounds.getBool()
         && packetIn.getCategory() == SoundCategory.PLAYERS
         && packetIn.getSoundName().contains("entity.player.hurt")
         && Minecraft.player != null
         && Minecraft.player.getDistance(packetIn.getX(), packetIn.getY(), packetIn.getZ()) > 0.3F) {
         mc.world
            .playSound(
               null,
               packetIn.getX(),
               packetIn.getY(),
               packetIn.getZ(),
               this.reEventSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT),
               SoundCategory.PLAYERS,
               packetIn.getVolume(),
               packetIn.getPitch()
            );
         return true;
      } else {
         return false;
      }
   }

   public SoundEvent reEventSound(SoundEvent prevEventSound) {
      if (this.HurtSounds.getBool() && this.isActived()) {
         String var2 = this.HurtSound.getMode();
         switch (var2) {
            case "Cow":
               prevEventSound = SoundEvents.ENTITY_COW_HURT;
               break;
            case "Chicken":
               prevEventSound = SoundEvents.ENTITY_CHICKEN_HURT;
               break;
            case "Blaze":
               prevEventSound = SoundEvents.ENTITY_BLAZE_HURT;
               break;
            case "Wolf":
               prevEventSound = SoundEvents.ENTITY_WOLF_HURT;
               break;
            case "Pig":
               prevEventSound = SoundEvents.ENTITY_PIG_HURT;
               break;
            case "Skeleton":
               prevEventSound = SoundEvents.ENTITY_SKELETON_HURT;
               break;
            case "Zombie":
               prevEventSound = SoundEvents.ENTITY_ZOMBIE_HURT;
               break;
            case "Thorns":
               prevEventSound = SoundEvents.ENCHANT_THORNS_HIT;
               break;
            case "HorseGallop":
               prevEventSound = SoundEvents.ENTITY_HORSE_GALLOP;
               break;
            case "BlastFirework":
               prevEventSound = SoundEvents.ENTITY_FIREWORK_BLAST_FAR;
               break;
            case "ItemBreak":
               prevEventSound = SoundEvents.ITEM_SHIELD_BREAK;
               break;
            case "Shulker":
               prevEventSound = SoundEvents.ENTITY_SHULKER_HURT_CLOSED;
               break;
            case "Snow":
               prevEventSound = SoundEvents.ENTITY_SNOWMAN_HURT;
               break;
            case "Spit":
               prevEventSound = SoundEvents.field_191255_dF;
               break;
            case "ChickPutEgg":
               prevEventSound = SoundEvents.field_191259_dX;
               break;
            case "EnderEye":
               prevEventSound = SoundEvents.field_193777_bb;
               break;
            case "Bell":
               prevEventSound = SoundEvents.field_193807_ew;
               break;
            case "Wither":
               prevEventSound = SoundEvents.field_193818_fh;
               break;
            case "GlassBreak":
               prevEventSound = SoundEvents.BLOCK_GLASS_BREAK;
               break;
            case "PistonPush":
               prevEventSound = SoundEvents.BLOCK_PISTON_EXTEND;
               break;
            case "Exp":
               prevEventSound = SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
         }
      }

      return prevEventSound;
   }

   public boolean getRTSoundSurround() {
      return get.isActived() && !Panic.stop && this.RTSoundSurround.getBool();
   }

   public boolean getRTPerfomanceMode() {
      return get.isActived() && !Panic.stop && this.RTPerfomanceMode.getBool();
   }

   public boolean getIsRTXDebugView() {
      return get.isActived() && !Panic.stop && this.RTXDebugView.getBool();
   }

   public boolean getIsRTXOcclusion() {
      return get.isActived() && !Panic.stop && this.RTOcclusion.getBool() && !this.RTPerfomanceMode.getBool();
   }

   public void playSong(String song) {
      MusicHelper.playSound(song);
   }

   public void playSong(String song, float volume) {
      MusicHelper.playSound(song, volume);
   }

   private boolean canPlaySong(Class at) {
      boolean play = at == Module.class && this.Modules.getBool() && this.isActived() || at == this.getClass() && this.Modules.getBool();
      if (play) {
         return true;
      } else {
         if ((at == ClickGuiScreen.class || at == CheckBox.class || at == ClientColors.class) && this.ClickGui.getBool()
            || at == TargetHUD.class
            || at == MiddleClick.class && this.MiddleClick.getBool()
            || at == GuiContainer.class && this.Containers.getBool()
            || at == Macros.class && this.Macroses.getBool()
            || at == Notifications.class && Notifications.get.isActived()
            || at == Config.class
            || at == DualsenseController.class) {
            play = true;
         }

         return this.actived && play;
      }
   }

   private String moduleSong(boolean enable) {
      return (enable ? "enable" : "disable") + this.Module1.getMode().toLowerCase() + ".wav";
   }

   private String guiScreenSong(boolean open) {
      String var2 = this.GuiPreset.getMode();
      byte var3 = -1;
      switch (var2.hashCode()) {
         case 79969970:
            if (var2.equals("Sleek")) {
               var3 = 0;
            }
         default:
            switch (var3) {
               case 0:
                  return open ? "guienabledev2.wav" : "guidisabledev2.wav";
               default:
                  return open ? "guienabledev4.wav" : "guidisabledev4.wav";
            }
      }
   }

   private String guiScreenFoneticOpenSong() {
      return "guifoneticonopen.wav";
   }

   private String guicolorsScreenSong(boolean open) {
      return open ? "guicolorsopen.wav" : "guicolorsclose.wav";
   }

   private String guiScreenMusicSaveToggleSong(boolean enable) {
      return enable ? "guisavemusonenable.wav" : "guisavemusondisable.wav";
   }

   private String macrosUseSong() {
      return "usemacros.wav";
   }

   private String targetSelectSong() {
      return "targetselect.wav";
   }

   private String guiScreenScrollSong() {
      return "guiscrolldev.wav";
   }

   private String guiScreenModeChangeSong(boolean hasChange) {
      return hasChange ? "guichangemode.wav" : "guichangemodemiss.wav";
   }

   private String guiScreenCheckOpenOrCloseSong(boolean open) {
      return "guicheck" + (open ? "open" : "close") + ".wav";
   }

   private String guiScreenCheckBoxSong(boolean enable) {
      return "gui" + (enable ? "enable" : "disable") + "checkbox.wav";
   }

   private String getSliderMoveSong() {
      return "guislidermovedev.wav";
   }

   private String guiScreenModuleOpenOrCloseSong(boolean open) {
      String var2 = this.GuiPreset.getMode();
      byte var3 = -1;
      switch (var2.hashCode()) {
         case 79969970:
            if (var2.equals("Sleek")) {
               var3 = 0;
            }
         default:
            switch (var3) {
               case 0:
                  return "guimodulepanel2" + (open ? "open" : "close") + ".wav";
               default:
                  return "guimodulepanel" + (open ? "open" : "close") + ".wav";
            }
      }
   }

   private String guiScreenModuleBindSong(boolean nonNullBind) {
      return "guibindset" + (nonNullBind ? "released" : "nulled") + ".wav";
   }

   private String guiScreenModuleBindToggleSong(boolean enable) {
      return "guibinding" + (enable ? "enable" : "disable") + ".wav";
   }

   private String guiScreenModuleBindHoldStatusSong(boolean reset) {
      return "guibindhold" + (reset ? "reset" : "start") + ".wav";
   }

   private String guiScreenPanelOpenOrCloseSong(boolean open) {
      return "guipanel" + (open ? "open" : "close") + ".wav";
   }

   private String guiScreenModuleHovering() {
      String var1 = this.GuiPreset.getMode();
      byte var2 = -1;
      switch (var1.hashCode()) {
         case 79969970:
            if (var1.equals("Sleek")) {
               var2 = 0;
            }
         default:
            switch (var2) {
               case 0:
                  return "guimodulehover2.wav";
               default:
                  return "guimodulehover.wav";
            }
      }
   }

   private String guiClientcolorModeChangeSong() {
      return "guiclientcolorchangemode.wav";
   }

   private String guiClientcolorPresetChangeSong() {
      return "guiclientcolorchangepreset.wav";
   }

   private String pressMiddleButtonSong() {
      return "middle_mouse_click.wav";
   }

   private String friendStatusUpdateSong(boolean addFriend) {
      return "friend" + (addFriend ? "add" : "remove") + ".wav";
   }

   private String guiContannerOpenOrCloseSong(boolean open) {
      return "guicontainer" + (open ? "open" : "close") + ".wav";
   }

   private String armorPreCrackSong() {
      return "armorPreCrack.wav";
   }

   private String loadConfigSong() {
      return "loadConfig.wav";
   }

   public void playUseMacros() {
      if (this.canPlaySong(Macros.class)) {
         this.playSong(this.macrosUseSong(), this.ModuleVolume.getFloat() / 600.0F);
      }
   }

   public void playModule(boolean enable) {
      if (this.canPlaySong(Module.class)) {
         this.playSong(this.moduleSong(enable), this.ModuleVolume.getFloat() / 200.0F);
      }
   }

   public void playModuleThis(boolean enable) {
      if (this.canPlaySong(this.getClass())) {
         this.playSong(this.moduleSong(enable), this.ModuleVolume.getFloat() / 200.0F);
      }
   }

   public void playGuiScreenOpenOrCloseSong(boolean open) {
      if (this.canPlaySong(ClickGuiScreen.class)) {
         this.playSong(this.guiScreenSong(open), open ? 1.0F : 0.6F);
      }
   }

   public void playGuiScreenFoneticSong() {
      if (this.canPlaySong(ClickGuiScreen.class)) {
         this.playSong(this.guiScreenFoneticOpenSong(), 0.2F);
      }
   }

   public void playGuiScreenScrollSong() {
      if (this.canPlaySong(ClickGuiScreen.class)) {
         this.playSong(this.guiScreenScrollSong());
      }
   }

   public void playGuiScreenCheckBox(boolean enable) {
      if (this.canPlaySong(ClickGuiScreen.class)) {
         this.playSong(this.guiScreenCheckBoxSong(enable));
      }
   }

   public void playGuiScreenChangeModeSong(boolean hasChange) {
      if (this.canPlaySong(ClickGuiScreen.class)) {
         this.playSong(this.guiScreenModeChangeSong(hasChange), hasChange ? 0.5F : 0.2F);
      }
   }

   public void playGuiCheckOpenOrCloseSong(boolean open) {
      if (this.canPlaySong(ClickGuiScreen.class)) {
         this.playSong(this.guiScreenCheckOpenOrCloseSong(open));
      }
   }

   public void playGuiSliderMoveSong() {
      if (this.canPlaySong(ClickGuiScreen.class)) {
         this.playSong(this.getSliderMoveSong());
      }
   }

   public void playGuiModuleOpenOrCloseSong(boolean open) {
      if (this.canPlaySong(ClickGuiScreen.class)) {
         this.playSong(this.guiScreenModuleOpenOrCloseSong(open), 0.5F);
      }
   }

   public void playGuiPenelOpenOrCloseSong(boolean open) {
      if (this.canPlaySong(ClickGuiScreen.class)) {
         this.playSong(this.guiScreenPanelOpenOrCloseSong(open));
      }
   }

   public void playGuiModuleBindSong(boolean nonNullBind) {
      if (this.canPlaySong(ClickGuiScreen.class)) {
         this.playSong(this.guiScreenModuleBindSong(nonNullBind));
      }
   }

   public void playGuiModuleBindingToggleSong(boolean enable) {
      if (this.canPlaySong(ClickGuiScreen.class)) {
         this.playSong(this.guiScreenModuleBindToggleSong(enable));
      }
   }

   public void playGuiModuleBindingHoldStatusSong(boolean reset) {
      if (this.canPlaySong(ClickGuiScreen.class)) {
         this.playSong(this.guiScreenModuleBindHoldStatusSong(reset), reset ? 0.4F : 0.7F);
      }
   }

   public void playGuiClientcolorsChangeModeSong() {
      if (this.canPlaySong(ClientColors.class)) {
         this.playSong(this.guiClientcolorModeChangeSong());
      }
   }

   public void playGuiClientcolorsChangePresetSong() {
      if (this.canPlaySong(ClientColors.class)) {
         this.playSong(this.guiClientcolorPresetChangeSong());
      }
   }

   public void playGuiColorsScreenOpenOrCloseSong(boolean open) {
      if (this.canPlaySong(ClientColors.class)) {
         this.playSong(this.guicolorsScreenSong(open));
      }
   }

   public void playGuiScreenModuleHoveringSong() {
      if (this.canPlaySong(ClickGuiScreen.class)) {
         this.playSong(this.guiScreenModuleHovering(), this.GuiPreset.getMode().equalsIgnoreCase("Sleek") ? 0.04F : 0.02F);
      }
   }

   public void playGuiScreenMusicSaveToggleSong(boolean enable) {
      if (this.canPlaySong(ClientColors.class)) {
         this.playSong(this.guiScreenMusicSaveToggleSong(enable), 0.25F);
      }
   }

   public void playTargetSelect() {
      if (this.canPlaySong(TargetHUD.class)) {
         this.playSong(this.targetSelectSong());
      }
   }

   public void playMiddleMouseSong() {
      if (this.canPlaySong(MiddleClick.class)) {
         this.playSong(this.pressMiddleButtonSong(), 0.05F);
      }
   }

   public void playFriendUpdateSong(boolean addFriend) {
      if (this.canPlaySong(MiddleClick.class)) {
         this.playSong(this.friendStatusUpdateSong(addFriend), 0.6F);
      }
   }

   public void playGuiContannerOpenOrCloseSong(boolean open) {
      if (this.canPlaySong(GuiContainer.class)) {
         this.playSong(this.guiContannerOpenOrCloseSong(open), 0.2F);
      }
   }

   public void playArmorPreCrackSong(boolean critical) {
      if (this.canPlaySong(Notifications.class)) {
         this.playSong(this.armorPreCrackSong(), critical ? 1.0F : 0.3F);
      }
   }

   public void playLoadConfigSong() {
      if (this.canPlaySong(Config.class)) {
         this.playSong(this.loadConfigSong(), 0.2F);
      }
   }

   public void playDualsenseSong(String soundTitle) {
      if (this.canPlaySong(DualsenseController.class)) {
         this.playSong("dualsense" + soundTitle + ".wav", 0.2F);
      }
   }
}
