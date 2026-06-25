package ru.govno.client.utils;

import com.google.common.collect.Lists;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import javazoom.jl.decoder.Equalizer;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.AnimationUtils;

public class RadioPlayerUtil {
   private boolean enhanceBass;
   private boolean enhanceVoice;
   private boolean enhanceClarity;
   private boolean quieterEnabled;
   private String currentURL;
   private boolean wantToChangeUrl;
   private final AnimationUtils smoothVolume = new AnimationUtils(0.0F, 0.0F, 0.0075F);
   private final AnimationUtils smoothLowPass = new AnimationUtils(0.0F, 0.0F, 0.02F);
   private final RadioPlayerUtil.IPlayer player;
   private final ru.govno.client.utils.Math.TimerHelper timerDelayChangeURL = ru.govno.client.utils.Math.TimerHelper.TimerHelperReseted();

   public void setEnhanceSound(boolean enhanceBass, boolean enhanceVoice, boolean enhanceClarity, boolean quieterEnabled) {
      this.enhanceBass = enhanceBass;
      this.enhanceVoice = enhanceVoice;
      this.enhanceClarity = enhanceClarity;
      this.quieterEnabled = quieterEnabled;
      this.smoothLowPass.to = this.quieterEnabled ? 1.0F : 0.0F;
   }

   public void setInstantLowPass() {
      this.smoothLowPass.to = 1.0F;
      this.smoothLowPass.setAnim(1.0F);
   }

   public boolean isEnhanceBass() {
      return this.enhanceBass;
   }

   public boolean isEnhanceVoice() {
      return this.enhanceVoice;
   }

   public boolean isEnhanceClarity() {
      return this.enhanceClarity;
   }

   public boolean isLowPass() {
      return this.quieterEnabled || this.getLowPassPC01() > 0.1F;
   }

   public float getLowPassPC01() {
      return this.smoothLowPass.anim;
   }

   private long delayChangeURL() {
      return 1000L;
   }

   public RadioPlayerUtil() {
      this(null);
   }

   public RadioPlayerUtil(String currentAudioURL) {
      this.player = new RadioPlayerUtil.IPlayer();
      this.setRadioURL(currentAudioURL);
      this.setVolume(0.0F, false);
   }

   public float getVolume01() {
      float mul = 0.55F * (1.0F - 0.55F * this.getLowPassPC01());
      float lowPassDePC = 1.0F - this.getLowPassPC01();
      if (this.isEnhanceBass()) {
         mul += 0.175F * lowPassDePC;
      }

      if (this.isEnhanceVoice()) {
         mul += 0.35F * lowPassDePC;
      }

      if (this.isEnhanceClarity()) {
         mul += 0.35F * lowPassDePC;
      }

      mul = Math.min(mul, 1.0F);
      return this.smoothVolume.anim * mul;
   }

   public boolean isPlaying() {
      return this.smoothVolume.anim != 0.0F
         && this.player != null
         && this.player.stream != null
         && this.player.playerThread != null
         && this.player.playerThread.isAlive()
         && this.player.getLastVolume() > 0.0F;
   }

   public void setRadioURLSmooth(String currentAudioURL) {
      if (!this.wantToChangeUrl && (this.currentURL == null || currentAudioURL != null && !this.currentURL.equalsIgnoreCase(currentAudioURL))) {
         this.wantToChangeUrl = true;
      }

      if (!this.timerDelayChangeURL.hasReached(1700.0)) {
         this.smoothVolume.speed = 0.001F;
      } else if (!this.timerDelayChangeURL.hasReached(2100.0)) {
         this.smoothVolume.speed = 0.01F;
      } else {
         this.smoothVolume.speed = this.smoothVolume.to == 0.0F ? 0.04F : 0.02F;
      }

      if (this.wantToChangeUrl) {
         this.smoothVolume.to = 0.0F;
         this.smoothVolume.speed = 0.04F;
         if (this.smoothVolume.anim < 0.005F && this.timerDelayChangeURL.hasReached((double)this.delayChangeURL())) {
            this.setRadioURL(currentAudioURL);
            this.smoothVolume.anim = 0.0F;
            this.smoothVolume.speed = 0.0F;
            this.wantToChangeUrl = false;
         }
      }
   }

   public boolean isCurrentActiveURL(String url) {
      return this.currentURL != null && this.currentURL.equalsIgnoreCase(url);
   }

   public boolean isWantToChangeUrl() {
      return this.wantToChangeUrl;
   }

   public boolean setRadioURL(String currentAudioURL) {
      if (currentAudioURL == null && this.currentURL != null && this.timerDelayChangeURL.hasReached((double)this.delayChangeURL())) {
         this.onChangeStation(true);
         this.currentURL = null;
         return true;
      } else if (this.currentURL == null || currentAudioURL != null && !this.currentURL.equalsIgnoreCase(currentAudioURL)) {
         if (this.timerDelayChangeURL.hasReached((double)this.delayChangeURL())) {
            this.currentURL = currentAudioURL;
            this.onChangeStation(false);
         }

         return true;
      } else {
         return false;
      }
   }

   public void setPlayingStatus(boolean playing, float volumePC, boolean smooth) {
      volumePC = playing ? volumePC : 0.0F;
      this.setVolume(volumePC, smooth);
   }

   public void setVolume(float volumePC, boolean smooth) {
      volumePC = MathUtils.clamp(volumePC * (!this.isPlaying() && smooth ? 5.0E-4F : 1.0F), 0.0F, 1.0F);
      if (!this.wantToChangeUrl) {
         this.smoothVolume.to = volumePC;
      }

      if (!smooth) {
         this.smoothVolume.anim = volumePC;
      }
   }

   private void onChangeStation(boolean isStop) {
      this.timerDelayChangeURL.reset();
      if (isStop) {
         this.setRadioURL(null);
         this.player.stop();
      }
   }

   public void updatePlayingOnTicks() {
      this.smoothVolume.getAnim();
      this.smoothLowPass.getAnim();
      this.smoothLowPass.speed = 0.0225F;
      this.player.getEqualizerRemote().resetEqualizer();
      this.player.eqStart();
      float lowPassPC01 = this.getLowPassPC01();
      float lowPassDePC01 = 1.0F - lowPassPC01;
      if (this.isEnhanceBass()) {
         this.player.eqProfileAppend(RadioPlayerUtil.EqProfiles.BASS, lowPassDePC01);
      }

      if (this.isEnhanceVoice()) {
         this.player.eqProfileAppend(RadioPlayerUtil.EqProfiles.VOICE, lowPassDePC01);
      }

      if (this.isEnhanceClarity()) {
         this.player.eqProfileAppend(RadioPlayerUtil.EqProfiles.CLARITY, lowPassDePC01);
      }

      if (this.isLowPass()) {
         this.player.eqMultiply(500, 20000, 1.0F - lowPassPC01 * 0.5F);
         this.player.getEqualizerRemote().normalizePostOver1Eq();
      }

      if (this.isEnhanceBass() || this.isEnhanceVoice() || this.isEnhanceClarity()) {
         this.player.getEqualizerRemote().equalizeFill(0.9F + 0.1F * lowPassPC01);
      }

      this.player.eqEnd(false);
      this.player.update(this.currentURL, this.getVolume01());
   }

   public void instantStopPlaying() {
      this.setRadioURL(null);
      this.player.stop();
      this.setVolume(0.0F, false);
   }

   public void stopPlayingUpdate(boolean instant) {
      if (instant) {
         this.instantStopPlaying();
      } else {
         if (this.getVolume01() < 0.005F) {
            this.setRadioURL(null);
            this.player.update(this.currentURL, 0.0F);
            this.setVolume(0.0F, false);
         } else {
            this.setVolume(0.0F, true);
            this.updatePlayingOnTicks();
         }
      }
   }

   public boolean waitLoadingUrlOrError() {
      return this.player != null && this.player.waitLoadingUrlOrError();
   }

   public float[] getTemporaryPlayerSamples(float scale, boolean abs) {
      return this.player == null ? new float[32] : this.player.getTemporaryPlayerSamples(scale, abs);
   }

   protected class EqLayerData {
      private final int lineEqNumber;
      private final List<Float> values;
      private boolean pushed;

      public EqLayerData(int lineEqNumber) {
         this.lineEqNumber = lineEqNumber;
         this.values = Lists.newArrayList();
      }

      public int getLineEqNumber() {
         return this.lineEqNumber;
      }

      public int getCurrentHZ() {
         float pc01 = (float)this.lineEqNumber / 32.0F;
         pc01 = pc01 == 0.0F ? 0.0F : (pc01 == 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, (double)(-10.0F * pc01)));
         return (int)(20.0F + 19980.0F * pc01);
      }

      public int currentHzDiffToOtherHz(int hz) {
         return Math.abs(this.getCurrentHZ() - hz);
      }

      public boolean compare(RadioPlayerUtil.EqLayerData other) {
         return other != null && other.getLineEqNumber() == this.lineEqNumber;
      }

      public void push() {
         if (!this.values.isEmpty()) {
            this.values.clear();
         }

         this.pushed = true;
      }

      public void addEq(float value) {
         if (this.pushed) {
            this.values.add(value);
         }
      }

      public void addEqPop(float value) {
         if (this.pushed) {
            this.addEq(value);
            this.pop();
         }
      }

      public void mullEq(float mul) {
         if (this.pushed) {
            if (this.values.isEmpty()) {
               this.values.add(1.0F);
            }

            List<Float> newValues = new ArrayList<>(this.values);
            this.values.clear();
            this.values.addAll(newValues.stream().map(value -> value * mul).toList());
         }
      }

      public void mullEqPop(float value) {
         if (this.pushed) {
            this.mullEq(value);
            this.pop();
         }
      }

      public void pop() {
         this.pushed = false;
      }

      public float getMinEq() {
         if (!this.values.isEmpty()) {
            float val = Float.MAX_VALUE;

            for (Float eq : this.values) {
               if (val > eq) {
                  val = eq;
               }
            }

            return val;
         } else {
            return 1.0F;
         }
      }

      public float getMidEq() {
         if (this.values.isEmpty()) {
            return 1.0F;
         } else {
            float val = 0.0F;

            for (Float eq : this.values) {
               val += eq;
            }

            return val / (float)this.values.size();
         }
      }

      public float getMaxEq() {
         if (!this.values.isEmpty()) {
            float val = Float.MIN_VALUE;

            for (Float eq : this.values) {
               if (val < eq) {
                  val = eq;
               }
            }

            return val;
         } else {
            return 1.0F;
         }
      }

      public float getFirstEq() {
         return this.values.isEmpty() ? 0.0F : this.values.get(0);
      }

      public float getLastEq() {
         return this.values.isEmpty() ? 0.0F : this.values.get(this.values.size() - 1);
      }
   }

   protected class EqMatrix32i {
      private final List<RadioPlayerUtil.EqLayerData> eqLayers = IntStream.rangeClosed(0, 32)
         .mapToObj(iInt -> RadioPlayerUtil.this.new EqLayerData(iInt))
         .toList();
      private boolean pushed;

      public EqMatrix32i() {
      }

      public boolean isPushed() {
         return this.pushed;
      }

      public void push() {
         this.eqLayers.forEach(RadioPlayerUtil.EqLayerData::push);
         this.pushed = true;
      }

      public void pop() {
         this.eqLayers.forEach(RadioPlayerUtil.EqLayerData::pop);
         this.pushed = false;
      }

      public void fillEq(float value) {
         this.addAllEq(value);
      }

      public void fillEqPop(float value) {
         this.fillEq(value);
         this.pop();
      }

      public void fillEqMul(float value) {
         this.mulAllEq(value);
      }

      public void fillEqMulPop(float value) {
         this.fillEqMul(value);
         this.pop();
      }

      public void addEq(float value, int... lineNumbers) {
         if (lineNumbers != null) {
            int[] var3 = lineNumbers;
            int var4 = lineNumbers.length;

            for (int var5 = 0; var5 < var4; var5++) {
               Integer lineNum = var3[var5];
               this.eqLayers.get(lineNum).addEq(value);
            }
         }
      }

      public void addEqPop(float value, int... lineNumbers) {
         if (lineNumbers != null) {
            int[] var3 = lineNumbers;
            int var4 = lineNumbers.length;

            for (int var5 = 0; var5 < var4; var5++) {
               Integer lineNum = var3[var5];
               this.eqLayers.get(lineNum).addEqPop(value);
            }
         }
      }

      public void addAllEq(float value) {
         this.eqLayers.forEach(iEqLayer -> this.addEq(value, iEqLayer.getLineEqNumber()));
      }

      public void addAllEqPop(float value) {
         this.eqLayers.forEach(iEqLayer -> this.addEqPop(value, iEqLayer.getLineEqNumber()));
      }

      public void mulEq(float value, int... lineNumbers) {
         if (lineNumbers != null) {
            int[] var3 = lineNumbers;
            int var4 = lineNumbers.length;

            for (int var5 = 0; var5 < var4; var5++) {
               Integer lineNum = var3[var5];
               this.eqLayers.get(lineNum).mullEq(value);
            }
         }
      }

      public void mulEqPop(float value, int... lineNumbers) {
         if (lineNumbers != null) {
            int[] var3 = lineNumbers;
            int var4 = lineNumbers.length;

            for (int var5 = 0; var5 < var4; var5++) {
               Integer lineNum = var3[var5];
               this.eqLayers.get(lineNum).mullEqPop(value);
            }
         }
      }

      public void mulAllEq(float value) {
         this.eqLayers.forEach(iEqLayer -> iEqLayer.mullEq(value));
      }

      public void mulAllEqPop(float value) {
         this.eqLayers.forEach(iEqLayer -> iEqLayer.mullEqPop(value));
      }

      public float getMinEq(int... lineNumbers) {
         if (lineNumbers != null && lineNumbers.length != 0) {
            float value = Float.MAX_VALUE;
            int[] var3 = lineNumbers;
            int var4 = lineNumbers.length;

            for (int var5 = 0; var5 < var4; var5++) {
               Integer lineNum = var3[var5];
               float minInEqLayer = this.eqLayers.get(lineNum).getMinEq();
               if (value > minInEqLayer) {
                  value = minInEqLayer;
               }
            }

            return value;
         } else {
            return 0.0F;
         }
      }

      public float getMidEq(int... lineNumbers) {
         if (lineNumbers != null && lineNumbers.length != 0) {
            float value = 0.0F;
            int[] var3 = lineNumbers;
            int var4 = lineNumbers.length;

            for (int var5 = 0; var5 < var4; var5++) {
               Integer lineNum = var3[var5];
               value += this.eqLayers.get(lineNum).getMidEq();
            }

            return value / (float)lineNumbers.length;
         } else {
            return 0.0F;
         }
      }

      public float getMaxEq(int... lineNumbers) {
         if (lineNumbers != null && lineNumbers.length != 0) {
            float value = Float.MIN_VALUE;
            int[] var3 = lineNumbers;
            int var4 = lineNumbers.length;

            for (int var5 = 0; var5 < var4; var5++) {
               Integer lineNum = var3[var5];
               float maxInEqLayer = this.eqLayers.get(lineNum).getMaxEq();
               if (value < maxInEqLayer) {
                  value = maxInEqLayer;
               }
            }

            return value;
         } else {
            return 0.0F;
         }
      }

      public RadioPlayerUtil.EqLayerData getOfIndex(int index) {
         return this.eqLayers.get(Math.max(Math.min(index, this.eqLayers.size()), 0));
      }

      public RadioPlayerUtil.EqLayerData getOfPC01(float pc01) {
         return this.eqLayers.get((int)((float)(this.eqLayers.size() - 1) * Math.max(Math.min(pc01, 1.0F), 0.0F)));
      }

      public List<RadioPlayerUtil.EqLayerData> getLayers() {
         return this.eqLayers;
      }

      public int[] getLineNumbersFromLayersList(List<RadioPlayerUtil.EqLayerData> eqLayers) {
         List<Integer> lineNumbers = eqLayers.stream().map(RadioPlayerUtil.EqLayerData::getLineEqNumber).toList();
         int[] indices = new int[lineNumbers.size()];
         int index = 0;

         for (Integer lineNum : lineNumbers) {
            indices[index] = lineNum;
            index++;
         }

         return indices;
      }

      public RadioPlayerUtil.EqLayerData getNearbyLayerEqToHz(int hz) {
         List<RadioPlayerUtil.EqLayerData> eqLayersCopy = new ArrayList<>(this.eqLayers);
         if (eqLayersCopy.isEmpty()) {
            return null;
         } else {
            if (eqLayersCopy.size() > 1) {
               eqLayersCopy = eqLayersCopy.stream().sorted(Comparator.comparing(iLayer -> iLayer.currentHzDiffToOtherHz(hz))).toList();
            }

            return eqLayersCopy.get(0);
         }
      }

      public List<RadioPlayerUtil.EqLayerData> getNearbyLayersEqToHz(int hz, int minHz, int maxHz) {
         List<RadioPlayerUtil.EqLayerData> eqLayersCopy = new ArrayList<>(this.eqLayers);
         if (eqLayersCopy.isEmpty()) {
            return null;
         } else {
            eqLayersCopy = eqLayersCopy.stream().filter(iEqLayer -> iEqLayer.getCurrentHZ() >= minHz && iEqLayer.getCurrentHZ() <= maxHz).toList();
            if (eqLayersCopy.size() > 1) {
               eqLayersCopy = eqLayersCopy.stream().sorted(Comparator.comparing(iLayer -> iLayer.currentHzDiffToOtherHz(hz))).toList();
            }

            return eqLayersCopy;
         }
      }
   }

   private static enum EqProfiles {
      BASS(20, 60, 1.1F),
      VOICE(350, 3000, 2.0F),
      CLARITY(1950, 20000, 1.525F);

      private final int hzMin;
      private final int hzMax;
      private final float value;

      public int getHzMin() {
         return this.hzMin;
      }

      public int getHzMax() {
         return this.hzMax;
      }

      public float getValueEq() {
         return this.value;
      }

      private EqProfiles(int hzMin, int hzMax, float value) {
         this.hzMin = hzMin;
         this.hzMax = hzMax;
         this.value = value;
      }
   }

   private class IEqualization {
      protected Equalizer equalizer = new Equalizer();
      protected final RadioPlayerUtil.EqMatrix32i eqMatrix = RadioPlayerUtil.this.new EqMatrix32i();
      private boolean anyChanges;

      public boolean hasAnyChanges() {
         return this.anyChanges;
      }

      public Equalizer getEqualizer() {
         return this.equalizer;
      }

      public boolean isPushed() {
         return this.eqMatrix.isPushed();
      }

      public RadioPlayerUtil.IEqualization push() {
         this.anyChanges = false;
         this.eqMatrix.push();
         return this.resetEqualizer();
      }

      public RadioPlayerUtil.IEqualization pop() {
         this.eqMatrix.pop();
         return this;
      }

      public RadioPlayerUtil.IEqualization normalizePostOver1Eq() {
         if (!this.isPushed()) {
            return this;
         } else {
            float maxExFind = Float.MIN_VALUE;

            for (RadioPlayerUtil.EqLayerData iEqLayer : this.eqMatrix.getLayers()) {
               float iLayerMaxEq = iEqLayer.getMaxEq();
               if (maxExFind < iLayerMaxEq) {
                  maxExFind = iLayerMaxEq;
               }
            }

            if (maxExFind > 1.0F) {
               this.equalizeFillMul(1.0F / maxExFind);
            }

            return this;
         }
      }

      public RadioPlayerUtil.IEqualization equalizeMinMaxHz(int minHz, int maxHz, float valueMul02) {
         if (!this.isPushed()) {
            return this;
         } else {
            this.eqMatrix.addEq(valueMul02, this.eqMatrix.getLineNumbersFromLayersList(this.getEqLayersOf(minHz, maxHz, false)));
            this.anyChanges = true;
            return this;
         }
      }

      public RadioPlayerUtil.IEqualization equalizeMinMaxHzMul(int minHz, int maxHz, float valueMul02) {
         if (!this.isPushed()) {
            return this;
         } else {
            this.eqMatrix.mulEq(valueMul02, this.eqMatrix.getLineNumbersFromLayersList(this.getEqLayersOf(minHz, maxHz, false)));
            this.anyChanges = true;
            return this;
         }
      }

      public RadioPlayerUtil.IEqualization equalizeFill(float valueMul02) {
         if (!this.isPushed()) {
            return this;
         } else {
            this.eqMatrix.fillEq(valueMul02);
            this.anyChanges = true;
            return this;
         }
      }

      public RadioPlayerUtil.IEqualization equalizeFillMul(float valueMul02) {
         if (!this.isPushed()) {
            return this;
         } else {
            this.eqMatrix.fillEqMul(valueMul02);
            this.anyChanges = true;
            return this;
         }
      }

      public RadioPlayerUtil.IEqualization equalizeRangeHz(int hz, int rangeHz, float valueMul02) {
         if (!this.isPushed()) {
            return this;
         } else {
            rangeHz = Math.abs(rangeHz);
            int minHz = Math.max(hz - rangeHz, 20);
            int maxHz = Math.min(hz + rangeHz, 20000);
            return this.equalizeMinMaxHz(minHz, maxHz, valueMul02);
         }
      }

      public RadioPlayerUtil.IEqualization equalizeRangeHzMul(int hz, int rangeHz, float valueMul02) {
         if (!this.isPushed()) {
            return this;
         } else {
            if (hz > 20000) {
               hz = 20000;
            }

            if (hz < 20) {
               hz = 20;
            }

            rangeHz = Math.abs(rangeHz);
            int minHz = Math.max(hz - rangeHz, 20);
            int maxHz = Math.min(hz + rangeHz, 20000);
            return this.equalizeMinMaxHzMul(minHz, maxHz, valueMul02);
         }
      }

      public RadioPlayerUtil.IEqualization applyMin() {
         if (!this.isPushed()) {
            this.eqMatrix
               .getLayers()
               .forEach(
                  iEqLayer -> this.equalizer.setBand(iEqLayer.getLineEqNumber(), this.eqValueByPercent(this.eqMatrix.getMinEq(iEqLayer.getLineEqNumber())))
               );
         }

         return this;
      }

      public RadioPlayerUtil.IEqualization applyMid() {
         if (!this.isPushed()) {
            this.eqMatrix
               .getLayers()
               .forEach(
                  iEqLayer -> this.equalizer.setBand(iEqLayer.getLineEqNumber(), this.eqValueByPercent(this.eqMatrix.getMidEq(iEqLayer.getLineEqNumber())))
               );
         }

         return this;
      }

      public RadioPlayerUtil.IEqualization applyMax() {
         if (!this.isPushed()) {
            this.eqMatrix
               .getLayers()
               .forEach(
                  iEqLayer -> this.equalizer.setBand(iEqLayer.getLineEqNumber(), this.eqValueByPercent(this.eqMatrix.getMaxEq(iEqLayer.getLineEqNumber())))
               );
         }

         return this;
      }

      public Equalizer applyMinToEqualizer(Equalizer equalizer) {
         if (!this.isPushed()) {
            equalizer.setFrom(this.applyMin().getEqualizer());
         }

         return this.getEqualizer();
      }

      public Equalizer applyMidToEqualizer(Equalizer equalizer) {
         if (!this.isPushed()) {
            equalizer.setFrom(this.applyMid().getEqualizer());
         }

         return this.getEqualizer();
      }

      public Equalizer applyMaxToEqualizer(Equalizer equalizer) {
         if (!this.isPushed()) {
            equalizer.setFrom(this.applyMax().getEqualizer());
         }

         return this.getEqualizer();
      }

      public RadioPlayerUtil.IEqualization resetEqualizer() {
         this.equalizer = new Equalizer();
         this.anyChanges = true;
         return this;
      }

      public Equalizer resetEqualizerGet() {
         this.resetEqualizer();
         return this.getEqualizer();
      }

      protected float eqValueByPercent(float percent02) {
         if (percent02 == 1.0F) {
            return 0.0F;
         } else {
            float maxUpBound = Math.max(Math.min(percent02, 100.0F), 2.0F);
            percent02 = MathUtils.clamp(percent02, 0.0F, maxUpBound);
            if (percent02 != 1.0F) {
               percent02 *= 1.0F - (float)Math.pow(2.0, (double)(-10.0F * (percent02 % (maxUpBound - 1.0F)) / (maxUpBound - 1.0F)));
            }

            return percent02 < 1.0F ? MathUtils.lerp(-5.5F, 0.0F, percent02) : MathUtils.lerp(0.0F, 2.0F, percent02 / (maxUpBound - 1.0F));
         }
      }

      protected int[] getHzArrayEq() {
         return this.getHzArrayEqOf(20, 20000);
      }

      protected int[] getHzArrayEqOf(int minHZ, int maxHZ) {
         int[] hzArray = new int[32];

         for (int lineNum = 0; lineNum < hzArray.length; lineNum++) {
            float endingPC = (float)lineNum / (float)hzArray.length;
            hzArray[lineNum] = minHZ + (int)((float)(maxHZ - minHZ) * this.getPercentAtMinToMaxHzOf(minHZ + (int)((float)(maxHZ - minHZ) * endingPC)));
         }

         return hzArray;
      }

      protected float getPercentAtMinToMaxHzOf(int hz) {
         return ((float)hz - 20.0F) / 1980.0F;
      }

      protected int getHzByPercent(double percent) {
         return (int)MathUtils.lerp(20.0, 20000.0, percent);
      }

      protected List<RadioPlayerUtil.EqLayerData> getEqLayersOf(int minHZ, int maxHZ, boolean researchOnceForMiss) {
         float minIndex = this.getPercentAtMinToMaxHzOf(minHZ) * 32.0F;
         float maxIndex = this.getPercentAtMinToMaxHzOf(maxHZ) * 32.0F;
         float indexStep = Math.max(624.375F / (float)(maxHZ - minHZ) * 10.0F, 1.0F);
         List<RadioPlayerUtil.EqLayerData> layersEq = Lists.newArrayList();

         for (float index = minIndex; index < maxIndex; index += indexStep) {
            float indexPC01 = index / 32.0F;
            List<RadioPlayerUtil.EqLayerData> layersEqSort = this.eqMatrix.getNearbyLayersEqToHz(this.getHzByPercent((double)indexPC01), minHZ, maxHZ);
            if (!layersEqSort.isEmpty()) {
               RadioPlayerUtil.EqLayerData firstSortedLayer = layersEqSort.get(0);
               if (firstSortedLayer != null) {
                  if (layersEq.isEmpty() || layersEq.stream().noneMatch(firstSortedLayer::compare)) {
                     layersEq.add(firstSortedLayer);
                  } else if (researchOnceForMiss) {
                     RadioPlayerUtil.EqLayerData secondSortedLayer = layersEqSort.get(0);
                     if (layersEq.isEmpty() || layersEq.stream().noneMatch(secondSortedLayer::compare)) {
                        layersEq.add(secondSortedLayer);
                     }
                  }
               }
            }
         }

         return layersEq;
      }

      protected List<RadioPlayerUtil.EqLayerData> getEqLayers() {
         return this.eqMatrix.getLayers();
      }
   }

   private class IPlayer {
      private AdvancedPlayer player;
      private Thread playerThread;
      private InputStream stream;
      private String currentURL;
      private float lastVolume;
      private final RadioPlayerUtil.IEqualization equalizerRemote = RadioPlayerUtil.this.new IEqualization();
      private boolean waitPlaying;
      private final List<Float> tempPlayedSamples = new ArrayList<>();
      public ru.govno.client.utils.Math.TimerHelper lastChangeTimerOutSamples = ru.govno.client.utils.Math.TimerHelper.TimerHelperReseted();

      public float getLastVolume() {
         return this.lastVolume;
      }

      public boolean waitLoadingUrlOrError() {
         return this.waitPlaying;
      }

      private RadioPlayerUtil.IEqualization getEqualizerRemote() {
         return this.equalizerRemote;
      }

      public RadioPlayerUtil.IPlayer eqStart() {
         RadioPlayerUtil.IEqualization remoteEq = this.getEqualizerRemote();
         remoteEq.push();
         return this;
      }

      public RadioPlayerUtil.IPlayer eqAppend(int minHZ, int maxHZ, float valueMulEq) {
         RadioPlayerUtil.IEqualization remoteEq = this.getEqualizerRemote();
         remoteEq.equalizeMinMaxHz(minHZ, maxHZ, valueMulEq);
         return this;
      }

      public RadioPlayerUtil.IPlayer eqMultiply(int minHZ, int maxHZ, float valueMulEq) {
         RadioPlayerUtil.IEqualization remoteEq = this.getEqualizerRemote();
         remoteEq.equalizeMinMaxHzMul(minHZ, maxHZ, valueMulEq);
         return this;
      }

      public RadioPlayerUtil.IPlayer eqProfileAppend(RadioPlayerUtil.EqProfiles profile) {
         if (profile == null) {
            return this;
         } else {
            RadioPlayerUtil.IEqualization remoteEq = this.getEqualizerRemote();
            remoteEq.equalizeMinMaxHzMul(profile.getHzMin(), profile.getHzMax(), profile.getValueEq());
            return this;
         }
      }

      public RadioPlayerUtil.IPlayer eqProfileAppend(RadioPlayerUtil.EqProfiles profile, float percentInfluence01) {
         if (profile == null) {
            return this;
         } else {
            RadioPlayerUtil.IEqualization remoteEq = this.getEqualizerRemote();
            remoteEq.equalizeMinMaxHzMul(profile.getHzMin(), profile.getHzMax(), MathUtils.lerp(1.0F, profile.getValueEq(), percentInfluence01));
            return this;
         }
      }

      public void eqEnd(boolean normalizeOver1Equalization) {
         RadioPlayerUtil.IEqualization remoteEq = this.getEqualizerRemote();
         if (normalizeOver1Equalization) {
            remoteEq.normalizePostOver1Eq().pop();
         } else {
            remoteEq.pop();
         }
      }

      public void stop() {
         if (this.player != null && !this.player.closed) {
            try {
               this.player.close();
               this.player.closed = true;
            } catch (Exception var2) {
               this.player = null;
               var2.printStackTrace();
            }

            System.out.println("stop player");
         } else {
            if (this.player == null) {
               this.playerThread = null;
            }

            this.stream = null;
            this.currentURL = null;
            this.waitPlaying = false;
            this.updatePlayerSamples(true);
         }
      }

      public void update(String url, float volume) {
         if (url != null && !(volume <= 0.0F) && !(volume > 1.0F)) {
            if (this.player != null) {
               this.updatePlayerSamples(false);
               this.setupEqualizationToPlayer();
               this.player.setVolumePC01(volume);
               this.lastVolume = volume;
            } else {
               this.lastVolume = 0.0F;
            }

            if (!url.equalsIgnoreCase(this.currentURL)) {
               this.stop();
               this.currentURL = url;
               System.out.println("set player url stream");
               this.waitPlaying = true;

               try {
                  CompletableFuture.runAsync(() -> {
                     try {
                        try {
                           this.stream = null;
                           URL theURL = null;
                           if (url != null) {
                              try {
                                 theURL = new URL(url);
                              } catch (MalformedURLException var9) {
                                 var9.printStackTrace();
                              }

                              if (theURL != null) {
                                 InputStream inputStream = null;
                                 if (theURL != null) {
                                    try {
                                       inputStream = theURL.openStream();
                                    } catch (IOException var8) {
                                       var8.printStackTrace();
                                    }

                                    if (inputStream != null) {
                                       BufferedInputStream buffered = null;

                                       try {
                                          buffered = new BufferedInputStream(inputStream);
                                       } catch (Exception var7) {
                                          var7.printStackTrace();
                                       }

                                       if (buffered != null) {
                                          this.stream = buffered;
                                       }
                                    }
                                 }
                              }
                           }
                        } catch (Exception var10) {
                           this.stream = null;
                           var10.printStackTrace();
                        }

                        System.out.println("try create player stream");
                        if (this.stream != null) {
                           this.startPlaying(this.stream, volume);
                           System.out.println("start player playing");
                        } else {
                           this.currentURL = null;
                        }
                     } catch (Exception var11) {
                        this.stream = null;
                        this.currentURL = null;
                        var11.printStackTrace();
                        System.out.println("player playback catch");
                     }
                  });
               } catch (Exception var4) {
                  var4.printStackTrace();
               }

               System.out.println("was player started may be");
            }

            if (this.playBackBreakPostTimeReached(4500.0F)) {
               System.out.println("player playing playback outdated 4500ms pre");
               this.playerThread = null;
               this.player = null;
               this.stream = null;
               this.lastVolume = 0.0F;
               this.waitPlaying = true;
               System.out.println("player playing playback outdated 4500ms post");
            }
         } else {
            this.stop();
            this.lastVolume = 0.0F;
         }
      }

      private boolean playBackBreakPostTimeReached(float reachTime) {
         return this.player != null
            && this.player.isPlaying()
            && !this.waitPlaying
            && !this.tempPlayedSamples.isEmpty()
            && this.lastChangeTimerOutSamples.hasReached((double)reachTime);
      }

      public void updatePlayerSamples(boolean clear) {
         if (clear) {
            if (!this.tempPlayedSamples.isEmpty()) {
               this.tempPlayedSamples.clear();
               this.lastChangeTimerOutSamples.reset();
            }
         } else {
            boolean checkChangeScaleAny = false;
            int index = 0;
            float[] var4 = this.player.getTempOutSamples();
            int var5 = var4.length;

            for (int var6 = 0; var6 < var5; var6++) {
               Float value = var4[var6];
               if (this.tempPlayedSamples.size() < index + 1) {
                  checkChangeScaleAny = true;
                  break;
               }

               Float value1 = this.tempPlayedSamples.get(index);
               if (value != value1) {
                  checkChangeScaleAny = true;
               }

               index++;
            }

            if (checkChangeScaleAny) {
               this.lastChangeTimerOutSamples.reset();
            }

            this.tempPlayedSamples.clear();
            var4 = this.player.getTempOutSamples();
            var5 = var4.length;

            for (int var11 = 0; var11 < var5; var11++) {
               Float valuex = var4[var11];
               this.tempPlayedSamples.add(valuex);
            }
         }
      }

      public float[] getTemporaryPlayerSamples(float scale, boolean abs) {
         if (this.player == null) {
            return new float[32];
         } else {
            List<Float> sampleValuesTempCalc = new ArrayList<>(this.tempPlayedSamples);
            if (abs) {
               sampleValuesTempCalc = sampleValuesTempCalc.stream().map(Math::abs).toList();
            }

            float[] out = new float[sampleValuesTempCalc.size()];

            for (int indexValue = 0; indexValue < sampleValuesTempCalc.size(); indexValue++) {
               out[indexValue] = sampleValuesTempCalc.get(indexValue) * scale;
            }

            return out;
         }
      }

      private void startPlaying(InputStream stream, float volume) {
         try {
            this.player = new AdvancedPlayer(stream);
            this.lastChangeTimerOutSamples.reset();
            if (this.lastVolume != volume) {
               this.player.setVolumePC01(volume);
               this.lastVolume = volume;
            }

            this.playerThread = new Thread(() -> {
               try {
                  this.player.play();
               } catch (JavaLayerException var2x) {
                  this.updatePlayerSamples(true);
                  this.waitPlaying = true;
                  var2x.printStackTrace();
               }
            }, "Radio-Player-Thread - " + new Random(System.nanoTime()).nextLong());
            this.playerThread.setDaemon(true);
            this.playerThread.setPriority(5);
            this.playerThread.start();
            this.waitPlaying = false;
            this.lastChangeTimerOutSamples.reset();
         } catch (JavaLayerException var4) {
            this.waitPlaying = true;
         }
      }

      private void setupEqualizationToPlayer() {
         if (this.getEqualizerRemote().hasAnyChanges()) {
            this.getEqualizerRemote().applyMid();
            this.player.updateEqualizer(this.equalizerRemote.getEqualizer());
         }
      }
   }
}
