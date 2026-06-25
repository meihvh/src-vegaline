package ru.govno.client.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.FloatControl.Type;
import net.minecraft.client.Minecraft;
import ru.govno.client.module.modules.ClientTune;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.UControllers.DXHaptics;

public class MusicHelper {
   private static AudioInputStream lastCreatedStream;
   private static final CopyOnWriteArrayList<Clip> CLIPS_LIST = new CopyOnWriteArrayList<>();
   private static final String packagePath = "/assets/minecraft/vegaline/sounds/";
   private static AudioFormat prevFormat;
   private static Info lastData;

   public static void playSound(String location, float volume) {
      if (System.getProperty("os.name").startsWith("Windows")) {
         volume *= ClientTune.getVolumeMuteMultiplierAlways();
          float finalVolume = volume;
          CompletableFuture.runAsync(() -> {
            if ((lastCreatedStream = getAudioInputStreamAsResLoc("/assets/minecraft/vegaline/sounds/" + location)) != null) {
               CLIPS_LIST.stream().filter(Line::isOpen).filter(clip -> !clip.isRunning()).forEach(Line::close);
               CLIPS_LIST.removeIf(clip -> !clip.isRunning());
               Clip createdClip;
               if ((createdClip = createClip(lastCreatedStream)) != null) {
                  CLIPS_LIST.add(createdClip);
               }

               CLIPS_LIST.stream().filter(Objects::nonNull).filter(clip -> !clip.isOpen()).forEach(clip -> {
                  try {
                     clip.open(lastCreatedStream);
                     setClipVolume(clip, finalVolume);
                     clip.start();
                  } catch (IOException | LineUnavailableException var3x) {
                     var3x.fillInStackTrace();
                  }
               });
            }
         });
      }
   }

   public static void playSoundInstant(String location, float volume) {
//      if (System.getProperty("os.name").startsWith("Windows")) {
         volume *= ClientTune.getVolumeMuteMultiplierAlways();
         if ((lastCreatedStream = getAudioInputStreamAsResLoc("/assets/minecraft/vegaline/sounds/" + location)) != null) {
            Clip createdClip;
            if ((createdClip = createClip(lastCreatedStream)) != null) {
               CLIPS_LIST.add(createdClip);
            }

            try {
               createdClip.open(lastCreatedStream);
               setClipVolume(createdClip, volume);
               createdClip.start();
            } catch (IOException | LineUnavailableException var4) {
               var4.fillInStackTrace();
            }
         }
//      }
   }

   public static void playSound(String location) {
      playSound(location, 0.45F);
   }

   public static void playSoundInstant(String location) {
      playSoundInstant(location, 0.45F);
   }

   public static void sendHapticsController(String location, float volume) {
      if (System.getProperty("os.name").startsWith("Windows")) {
         volume *= ClientTune.getVolumeMuteMultiplierAlways();
         if ((lastCreatedStream = getAudioInputStreamAsResLoc("/assets/minecraft/vegaline/sounds/" + location)) != null) {
            DXHaptics.playHapticsTry(lastCreatedStream, volume, 32);
         }
      }
   }

   public static void sendHapticsController(String location) {
      sendHapticsController(location, 0.45F);
   }

   private static Clip createClip(AudioInputStream stream) {
      AudioFormat format = stream.getFormat();
      if (prevFormat != format) {
         lastData = new Info(Clip.class, stream.getFormat());
         prevFormat = format;
      }

      try {
         return (Clip)AudioSystem.getLine(lastData);
      } catch (LineUnavailableException var3) {
         var3.fillInStackTrace();
         return null;
      }
   }

   public static void setClipVolume(Clip clip, float volume) {
      if (clip.isControlSupported(Type.MASTER_GAIN) && !(volume < 0.0F) && !(volume > 1.0F)) {
         FloatControl volumeControl = (FloatControl)clip.getControl(Type.MASTER_GAIN);
         float db = (float)(Math.log((double)Math.max(Math.min(volume, 1.0F), 0.0F)) / Math.log(10.0) * 20.0);
         volumeControl.setValue(MathUtils.clamp(db, volumeControl.getMinimum(), volumeControl.getMaximum()));
      }
   }

   private static AudioInputStream getAudioInputStreamAsResLoc(String resLoc) {
      try {
         return Minecraft.temporalImageSizeMoreThan16x
            ? AudioSystem.getAudioInputStream(new BufferedInputStream(Objects.requireNonNull(MusicHelper.class.getResourceAsStream(resLoc))))
            : null;
      } catch (IOException | UnsupportedAudioFileException var2) {
         var2.fillInStackTrace();
         return null;
      }
   }
}
