package ru.govno.client.soundengine;

import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.govno.client.module.modules.ClientTune;
import ru.govno.client.soundengine.filters.FilterLowPass;
import ru.govno.client.soundengine.filters.FilterReverb;
import ru.govno.client.utils.Math.MathUtils;

public class SoundMixFilter {
   private final SoundMixerBase mixer;
   private final SoundSurroundTool surround;
   private boolean state = true;
   private float tempLowPassGain;
   private float tempLowPassGainHF;
   private boolean canReturnValuesInPrePlay;

   public SoundMixerBase getMixer() {
      return this.mixer;
   }

   public SoundSurroundTool getSurround() {
      return this.surround;
   }

   public void setState(boolean state) {
      this.state = state;
   }

   private SoundMixFilter() {
      this.mixer = SoundMixerBase.loadMixer();
      this.surround = SoundSurroundTool.build();
   }

   public static SoundMixFilter makeDistorterMixer() {
      return new SoundMixFilter();
   }

   public void updateMixer() {
      float[] args = new float[]{0.0F, 0.0F, 1.0F, 1.0F};
      this.setState(ClientTune.get.getRTSoundSurround());
      if (this.state) {
         this.getSurround().setRtxDebug(ClientTune.get.getIsRTXDebugView());
         this.getSurround().setRtxOcclusion(ClientTune.get.getIsRTXOcclusion());
         this.getSurround().setPlayer(Minecraft.player);
         this.getSurround().setTooPerfomance(ClientTune.get.getRTPerfomanceMode());
         args = this.getSurround().getGainArgsFromWorld(this.getMixer());
      } else if (!this.getSurround().getListOfTestVecs().isEmpty()) {
         int sz = this.getSurround().getListOfTestVecs().size();

         for (int i = 0; (float)i < (float)sz / 2.0F; i++) {
            if (!this.getSurround().getListOfTestVecs().isEmpty()) {
               this.getSurround().getListOfTestVecs().remove(0);
            }
         }
      } else {
         this.getSurround().setRtxDebug(false);
      }

      this.getMixer().setEchoEffect(args[0] * 1.25F * (this.getSurround().isTooPerfomance() ? 0.333333F : 1.0F), args[1]);
      this.getMixer().setLowPass(args[2], args[3]);
      this.updateFiltersData(false);
   }

   private float biLerp(double x, double x1, double x2, float off1, float off2) {
      return (float)((x2 - x) / (x2 - x1) * (double)off1 + (x - x1) / (x2 - x1) * (double)off2);
   }

   public void updateMixerPrePlayInModdedLibrary(boolean pre) {
      if (pre) {
         FilterLowPass lowPassFilter = this.getMixer().getLowPassFilter();
         float lowPassGain = lowPassFilter.gain;
         float lowPassGainHF = lowPassFilter.gainHF;
         this.tempLowPassGain = lowPassGain;
         this.tempLowPassGainHF = lowPassGainHF;
         Vec3d lastUpdatedSoundPosition;
         if (this.getSurround().isRtxOcclusion() && (lastUpdatedSoundPosition = this.getMixer().getLastSoundPosition()) != null) {
            boolean hasNotSeen = (Minecraft.getMinecraft().world.getBlockState(new BlockPos(lastUpdatedSoundPosition)).getMaterial().blocksMovement()
                  ? Arrays.asList(
                     lastUpdatedSoundPosition,
                     lastUpdatedSoundPosition.addVector(0.5, 0.0, 0.0),
                     lastUpdatedSoundPosition.addVector(-0.5, 0.0, 0.0),
                     lastUpdatedSoundPosition.addVector(0.0, 0.5, 0.0),
                     lastUpdatedSoundPosition.addVector(0.0, -0.5, 0.0),
                     lastUpdatedSoundPosition.addVector(0.0, 0.0, 0.5),
                     lastUpdatedSoundPosition.addVector(0.0, 0.0, -0.5)
                  )
                  : List.of(lastUpdatedSoundPosition))
               .stream()
               .allMatch(vec -> Minecraft.getMinecraft().world.rayTraceBlocks(this.getSurround().getHeadPosition(), vec, false, true, false) != null);
            if (hasNotSeen) {
               double listenerDistance = this.getSurround().getLastHeadPosition().distanceTo(lastUpdatedSoundPosition);
               double maxDST = 14.5;
               double minDST = 3.625;
               float vol = this.tempLowPassGain;
               float push = this.tempLowPassGainHF;
               float maxPushPC = 0.7F;
               float outDSTMinPC = (float)Math.min(listenerDistance / 3.625, 1.0);
               float inDSTMaxPC = 1.0F - (float)Math.min(listenerDistance / (14.5 - Math.min(3.625, 13.5)), 1.0);
               float pushOutPC = 1.0F - outDSTMinPC * maxPushPC;
               pushOutPC = MathUtils.lerp(pushOutPC, 1.0F, 1.0F - inDSTMaxPC);
               push *= pushOutPC;
               push = Math.max(push, 0.0F);
               vol *= (push + vol) / 2.0F;
               vol = Math.max(vol, 0.25F);
               this.getMixer().setLowPass(vol, push);
               this.updateFiltersData(true);
               this.canReturnValuesInPrePlay = true;
            }
         }
      } else {
         if (this.canReturnValuesInPrePlay) {
            this.getMixer().setLowPass(this.tempLowPassGain, this.tempLowPassGainHF);
            this.updateFiltersData(true);
            this.canReturnValuesInPrePlay = false;
         }
      }
   }

   private void updateFiltersData(boolean instant) {
      SoundMixerBase mixer = this.getMixer();
      FilterReverb reverbFilter = mixer.getReverbFilter();
      FilterLowPass lowPassFilter = mixer.getLowPassFilter();
      float echoDelay = mixer.getEchoPercent() / 1.75F;
      float echoRev = mixer.getReflectPercent() / 1.25F;
      reverbFilter.decayTime = echoDelay;
      reverbFilter.reflectionsGain = echoRev * (0.05F + 0.05F * echoDelay);
      reverbFilter.reflectionsDelay = 0.125F * echoDelay;
      reverbFilter.lateReverbGain = echoRev * (1.26F + 0.2F * echoDelay);
      reverbFilter.lateReverbDelay = 0.01F * echoDelay;
      reverbFilter.checkParameters();
      float lLevelGain = mixer.getLowPassGain();
      float lLevelGainHF = mixer.getLowPassGainHF();
      lowPassFilter.gain = instant ? lLevelGain : MathUtils.lerp(lowPassFilter.gain, lLevelGain, lowPassFilter.gainHF > lLevelGainHF ? 0.05F : 0.1F);
      lowPassFilter.gainHF = instant ? lLevelGainHF : MathUtils.lerp(lowPassFilter.gainHF, lLevelGainHF, lowPassFilter.gainHF > lLevelGainHF ? 0.3F : 0.12F);
      lowPassFilter.checkParameters();
   }

   public boolean getHasMixerLoaded() {
      return this.mixer != null;
   }

   public void init() {
   }

   public void unload() {
      this.mixer.cleanupEffects();
   }
}
