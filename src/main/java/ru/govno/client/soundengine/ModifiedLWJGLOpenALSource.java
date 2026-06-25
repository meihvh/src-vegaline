package ru.govno.client.soundengine;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.Vec3d;
import paulscode.sound.FilenameURL;
import paulscode.sound.SoundBuffer;
import paulscode.sound.Source;
import paulscode.sound.libraries.ChannelLWJGLOpenAL;
import paulscode.sound.libraries.SourceLWJGLOpenAL;
import ru.govno.client.soundengine.filters.BaseFilter;
import ru.govno.client.soundengine.filters.FilterException;
import ru.govno.client.soundengine.filters.FilterLowPass;
import ru.govno.client.soundengine.filters.FilterReverb;

public class ModifiedLWJGLOpenALSource extends SourceLWJGLOpenAL {
   public ModifiedLWJGLOpenALSource(FloatBuffer listenerPosition, IntBuffer myBuffer, Source old, SoundBuffer soundBuffer) {
      super(listenerPosition, myBuffer, old, soundBuffer);
   }

   public ModifiedLWJGLOpenALSource(
      FloatBuffer listenerPosition,
      IntBuffer myBuffer,
      boolean priority,
      boolean toStream,
      boolean toLoop,
      String sourcename,
      FilenameURL filenameURL,
      SoundBuffer soundBuffer,
      float x,
      float y,
      float z,
      int attModel,
      float distOrRoll,
      boolean temporary
   ) {
      super(listenerPosition, myBuffer, priority, toStream, toLoop, sourcename, filenameURL, soundBuffer, x, y, z, attModel, distOrRoll, temporary);
   }

   public ModifiedLWJGLOpenALSource(
      FloatBuffer listenerPosition, AudioFormat audioFormat, boolean priority, String sourcename, float x, float y, float z, int attModel, float distOrRoll
   ) {
      super(listenerPosition, audioFormat, priority, sourcename, x, y, z, attModel, distOrRoll);
   }

   @Override
   public boolean stopped() {
      boolean stopped = super.stopped();
      if (this.channel != null && this.channel.attachedSource == this && !stopped && !this.paused()) {
         this.updateFilters();
      }

      return stopped;
   }

   private void updateFilters() {
      SoundMixerBase mixer = Minecraft.getMinecraft().sndHandleEdit.getMixer();
      mixer.updateTemporalSoundPosition(null);
      boolean canUseFilters = (
            !this.toStream || this.position == null || this.position.x != 0.0F || this.position.y != 0.0F || this.position.z != 0.0F || this.attModel != 0
         )
         && !Minecraft.getMinecraft().isGamePaused()
         && Minecraft.getMinecraft().world != null;
      ChannelLWJGLOpenAL alChannel = (ChannelLWJGLOpenAL)this.channel;
      FilterReverb reverbFilter = mixer.getReverbFilter();
      FilterLowPass lowPassFilter = mixer.getLowPassFilter();
      if (canUseFilters) {
         if (this.position != null) {
            mixer.updateTemporalSoundPosition(new Vec3d((double)this.position.x, (double)this.position.y, (double)this.position.z));
         }

         Minecraft.getMinecraft().sndHandleEdit.updateMixerPrePlayInModdedLibrary(true);
         if (reverbFilter.reflectionsDelay > 0.0F && reverbFilter.lateReverbDelay > 0.0F) {
            reverbFilter.enable();
            reverbFilter.loadParameters();
         } else {
            reverbFilter.disable();
         }

         if (this.attModel == 0 || lowPassFilter.gain == 1.0F && lowPassFilter.gainHF == 1.0F) {
            lowPassFilter.disable();
         } else {
            lowPassFilter.enable();
            lowPassFilter.loadParameters();
         }

         try {
            BaseFilter.loadSourceFilter(alChannel.ALSource.get(0), 131077, lowPassFilter);
            BaseFilter.load3SourceFilters(alChannel.ALSource.get(0), 131078, reverbFilter, null, lowPassFilter);
         } catch (FilterException var9) {
            CrashReport crashreport = CrashReport.makeCrashReport(var9, "Updating Sound Filters");
            throw new ReportedException(crashreport);
         }

         Minecraft.getMinecraft().sndHandleEdit.updateMixerPrePlayInModdedLibrary(false);
      } else {
         Minecraft.getMinecraft().sndHandleEdit.updateMixerPrePlayInModdedLibrary(false);
         lowPassFilter.disable();
         reverbFilter.disable();

         try {
            BaseFilter.loadSourceFilter(alChannel.ALSource.get(0), 131077, null);
            BaseFilter.load3SourceFilters(alChannel.ALSource.get(0), 131078, null, null, null);
         } catch (FilterException var8) {
            CrashReport crashreport = CrashReport.makeCrashReport(var8, "Updating Sound Filters");
            throw new ReportedException(crashreport);
         }
      }
   }
}
