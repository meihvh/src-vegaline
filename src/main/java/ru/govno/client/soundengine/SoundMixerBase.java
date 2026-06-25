package ru.govno.client.soundengine;

import net.minecraft.util.math.Vec3d;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.SoundSystemException;
import ru.govno.client.soundengine.filters.FilterLowPass;
import ru.govno.client.soundengine.filters.FilterReverb;
import ru.govno.client.utils.Math.MathUtils;

public class SoundMixerBase {
   private float echoPercent;
   private float reflectPercent;
   private float lowPassGain;
   private float lowPassGainHF;
   private final FilterLowPass lowPassFilter = new FilterLowPass();
   private final FilterReverb reverbFilter = new FilterReverb();
   private boolean locked;
   private Vec3d lastSoundPosition;

   public float getEchoPercent() {
      return this.echoPercent;
   }

   public void setEchoPercent(float echoPercent) {
      this.echoPercent = echoPercent;
   }

   public float getReflectPercent() {
      return this.reflectPercent;
   }

   public void setReflectPercent(float reflectPercent) {
      this.reflectPercent = reflectPercent;
   }

   public float getLowPassGain() {
      return this.lowPassGain;
   }

   public void setLowPassGain(float lowPassGain) {
      this.lowPassGain = lowPassGain;
   }

   public float getLowPassGainHF() {
      return this.lowPassGainHF;
   }

   public void setLowPassGainHF(float lowPassGainHF) {
      this.lowPassGainHF = lowPassGainHF;
   }

   public FilterLowPass getLowPassFilter() {
      return this.lowPassFilter;
   }

   public FilterReverb getReverbFilter() {
      return this.reverbFilter;
   }

   public SoundMixerBase(Class libraryClass) {
      if (SoundSystemConfig.getLibraries() != null) {
         SoundSystemConfig.getLibraries().clear();
      }

      try {
         SoundSystemConfig.addLibrary(libraryClass);
      } catch (SoundSystemException var3) {
         var3.fillInStackTrace();
      }
   }

   public static SoundMixerBase loadMixer() {
      return new SoundMixerBase(ModifiedLWJGLOpenALLibrary.class);
   }

   public void setEchoEffect(float longest, float reflect) {
      if (!this.locked) {
         this.echoPercent = MathUtils.lerp(this.echoPercent, longest, this.echoPercent > longest ? 0.75F : 0.55F);
         this.reflectPercent = MathUtils.lerp(this.reflectPercent, reflect, this.reflectPercent > reflect ? 0.75F : 0.55F);
      }
   }

   public void setLowPass(float gain, float gainHF) {
      if (!this.locked) {
         this.lowPassGain = gain;
         this.lowPassGainHF = gainHF;
      }
   }

   public void cleanupEffects() {
      if (!this.locked) {
         this.echoPercent = 0.0F;
         this.reflectPercent = 0.0F;
         this.lowPassGain = 1.0F;
         this.lowPassGainHF = 1.0F;
      }
   }

   public void lockup() {
      this.locked = true;
   }

   public void unlock() {
      this.locked = false;
   }

   public void updateTemporalSoundPosition(Vec3d positionIn) {
      if (!this.locked) {
         if (positionIn == null) {
            this.lastSoundPosition = null;
         } else {
            this.lastSoundPosition = new Vec3d(positionIn.xCoord, positionIn.yCoord, positionIn.zCoord);
         }
      }
   }

   public Vec3d getLastSoundPosition() {
      return this.lastSoundPosition;
   }
}
