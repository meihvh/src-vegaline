package ru.govno.client.clickgui;

import java.io.BufferedInputStream;
import java.io.InputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;
import net.minecraft.client.Minecraft;
import ru.govno.client.module.modules.ClientTune;
import ru.govno.client.utils.MusicHelper;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.AnimationUtils;

public class GuiMusicTuner {
   String musicName;
   String forceMusicName;
   String path = "/assets/minecraft/vegaline/sounds/";
   String format = ".wav";
   AnimationUtils volume = new AnimationUtils(0.0F, 0.0F, 0.01F);
   float maxVolume;
   boolean wantToChangeTrack;
   private Clip clip;
   private AudioInputStream stream;
   private String temporaryTrackLoc;
   private boolean crashed = false;
   private float lastVolume = 0.0F;

   public GuiMusicTuner(String musicName, float normalVolume) {
      this.musicName = musicName;
      this.maxVolume = normalVolume;
   }

   public void setVolumePC(float value) {
      float to = MathUtils.clamp(value, 0.0F, 1.0F);
      this.volume.setAnim(to);
      this.volume.to = to;
   }

   public void setPlaying(boolean playing) {
      this.setVolumeSmoothPC(playing ? 1.0F : 0.0F);
   }

   public float getMaxVolumeVal() {
      return this.maxVolume / 3.0F * (Minecraft.getMinecraft().isGamePaused() ? 0.2F : 1.0F);
   }

   public void setMaxVolume(float value) {
      this.maxVolume = MathUtils.clamp(value, 0.0F, 1.0F);
   }

   public void multipleVolume(float mul) {
      this.maxVolume *= mul;
      this.maxVolume = this.maxVolume < 0.0F ? 0.0F : (this.maxVolume > 1.0F ? 1.0F : this.maxVolume);
   }

   public void setVolumeSmoothPC(float value) {
      this.volume.speed = (value == 1.0F ? 0.00225F : 0.002666F) * 2.5F;
      this.volume.to = MathUtils.clamp(value * this.getMaxVolumeVal(), 0.0F, 1.0F);
   }

   public void setVolumeChangeSpeed(float value) {
      this.volume.speed = value;
   }

   public void setTrackName(String name) {
      if (this.forceMusicName == null) {
         this.forceMusicName = name;
      }

      this.wantToChangeTrack = !this.forceMusicName.equalsIgnoreCase(this.musicName);
      this.forceMusicName = name;
   }

   public void setTrackNameForce(String name) {
      this.musicName = name;
      this.forceMusicName = name;
   }

   public String getTrackLoc() {
      return this.path + this.musicName + this.format;
   }

   public String getForceTrackLoc() {
      return this.path + this.forceMusicName + this.format;
   }

   public float getVolumeVal() {
      float padding = 0.005F * this.getMaxVolumeVal();
      if (MathUtils.getDifferenceOf(this.volume.to, this.volume.anim) < padding) {
         this.volume.setAnim(this.volume.to);
         this.lastVolume = this.volume.anim;
      } else {
         this.lastVolume = this.volume.getAnim();
      }

      return this.lastVolume * ClientTune.getVolumeMuteMultiplierAlways();
   }

   float getVolumeForMixer(float volume) {
      return (float)(Math.log((double)MathUtils.clamp(volume, 0.0F, 1.0F)) / Math.log(10.0) * 20.0);
   }

   public boolean canPlayTrack() {
      return this.lastVolume > 0.0F;
   }

   public boolean wantPlayTrack() {
      return this.lastVolume > 0.0F;
   }

   public float lastVolume() {
      return this.lastVolume;
   }

   public void controlTrackUpdater() {
      if (Panic.stop || this.crashed) {
         if (this.crashed) {
//            System.out.println(this.musicName + " GuiMusicTuner crashed fucking hell");
         }

         this.setPlaying(false);
          return;
      }

      float volPC;
      if (this.wantToChangeTrack) {
         this.setVolumeSmoothPC(0.0F);
         if ((volPC = this.getVolumeVal()) == 0.0F || this.musicName == null) {
            this.setTrackNameForce(this.forceMusicName);
            this.wantToChangeTrack = false;
         }
      } else {
         volPC = this.getVolumeVal();
      }

      String trackLoc = this.getTrackLoc();
      float volume = this.getVolumeForMixer(volPC);
      boolean play = volPC != 0.0F || this.clip != null && this.clip.isRunning() && this.clip.isControlSupported(Type.MASTER_GAIN);
      if (play || this.clip != null) {
         if (trackLoc != null && (!play || !trackLoc.equalsIgnoreCase(this.temporaryTrackLoc))) {
            this.temporaryTrackLoc = trackLoc;
            if (this.clip != null) {
               this.clip.stop();
               this.clip.close();
               this.clip = null;
               this.stream = null;
            }
         }

         if (play) {
            try {
               if (this.stream == null) {
                  InputStream inputStream = MusicHelper.class.getResourceAsStream(trackLoc);
                  BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                  this.stream = AudioSystem.getAudioInputStream(bufferedInputStream);
               }
            } catch (Exception var8) {
                var8.printStackTrace();
               this.crashed = true;
            }

            if (this.stream != null) {
               try {
                  if (this.clip == null || this.clip != null && !this.clip.isOpen()) {
                     this.clip = AudioSystem.getClip();
                  }
               } catch (Exception var9) {
                   var9.printStackTrace();
                  this.crashed = true;
               }
            }

            if (this.clip != null) {
               if (!this.clip.isOpen()) {
                  try {
                     this.clip.open(this.stream);
                  } catch (Exception var7) {
                      var7.printStackTrace();
                     this.crashed = true;
                  }
               }

               if (this.clip.isOpen()) {
                  if (!this.clip.isRunning() || this.clip.getMicrosecondPosition() == this.clip.getMicrosecondLength()) {
                     this.clip.setMicrosecondPosition(0L);
                     ((FloatControl)this.clip.getControl(Type.MASTER_GAIN)).setValue((float)((int)volume));
                     this.clip.start();
                  }

                  if (this.clip.isRunning()) {
                     FloatControl volumeControl = this.clip == null ? null : (FloatControl)this.clip.getControl(Type.MASTER_GAIN);
                     if (volumeControl != null) {
                        volume = MathUtils.clamp(volume, volumeControl.getMinimum(), volumeControl.getMaximum());
                        if (volumeControl.getValue() != (float)((int)volume)) {
                           volumeControl.setValue((float)((int)volume));
                        }
                     }
                  }
               }
            }
         } else if (this.clip != null && this.clip.isRunning()) {
            this.clip.setMicrosecondPosition(0L);
            ((FloatControl)this.clip.getControl(Type.MASTER_GAIN)).setValue((float)(Math.log(0.0) / Math.log(10.0) * 20.0));
         }
      }
   }

   public boolean isPlaying() {
      return this.clip != null && this.clip.isRunning();
   }
}
