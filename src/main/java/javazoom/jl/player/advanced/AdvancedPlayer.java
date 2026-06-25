package javazoom.jl.player.advanced;

import java.io.InputStream;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Equalizer;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;

public class AdvancedPlayer {
   Equalizer equalizer = new Equalizer();
   private final InputStream stream;
   private final Bitstream bitstream;
   private final Decoder decoder;
   private AudioDevice audio;
   public boolean closed = false;
   public boolean complete = false;
   private int lastPosition = 0;
   private PlaybackListener listener;
   private float volume;

   public boolean isPlaying() {
      return this.audio != null && this.bitstream != null && !this.closed && !this.complete;
   }

   public void setVolumePC01(float volume) {
      this.volume = volume;
   }

   public void updateEqualizer(Equalizer equalizer) {
      this.equalizer.setFrom(equalizer);
      this.decoder.updateEqualizerData(this.equalizer);
   }

   public float[] getTempOutSamples() {
      return this.getTempOutSamples(1.0F);
   }

   public float[] getTempOutSamples(float scale) {
      return this.decoder == null ? new float[32] : this.decoder.getTempOutSamples(scale);
   }

   public AdvancedPlayer(InputStream stream) throws JavaLayerException {
      this(stream, null);
   }

   public AdvancedPlayer(InputStream stream, AudioDevice device) throws JavaLayerException {
      this.stream = stream;
      this.bitstream = new Bitstream(stream);
      if (device != null) {
         this.audio = device;
      } else {
         this.audio = FactoryRegistry.systemRegistry().createAudioDevice();
      }

      this.decoder = new Decoder();
      this.decoder.setEqualizer(this.equalizer);
      this.audio.open(this.decoder);
   }

   public void play() throws JavaLayerException {
      this.play(Integer.MAX_VALUE);
   }

   public boolean play(int frames) throws JavaLayerException {
      boolean ret = this.stream != null && this.audio != null && this.audio.isOpen() && this.bitstream != null;

      try {
         if (this.listener != null) {
            this.listener.playbackStarted(this.createEvent(PlaybackEvent.STARTED));
         }

         while (frames-- > 0 && ret) {
            try {
               ret = this.decodeFrame();
            } catch (JavaLayerException var7) {
               ret = false;
               this.closed = true;
            }
         }

         if (!ret) {
            AudioDevice out = this.audio;
            if (out != null) {
               out.flush();
               synchronized (this) {
                  this.complete = !this.closed;
                  this.close();
               }

               if (this.listener != null) {
                  this.listener.playbackFinished(this.createEvent(out, PlaybackEvent.STOPPED));
               }
            }
         }

         return ret;
      } catch (Exception var8) {
         this.closed = true;
         var8.printStackTrace();
         return false;
      }
   }

   public synchronized void close() {
      AudioDevice out = this.audio;
      if (out != null) {
         this.closed = true;
         this.audio = null;

         try {
            out.close();
         } catch (Exception var4) {
            var4.printStackTrace();
         }

         this.lastPosition = out.getPosition();

         try {
            this.bitstream.close();
         } catch (BitstreamException var3) {
            var3.printStackTrace();
         }
      }
   }

   protected boolean decodeFrame() throws JavaLayerException {
      try {
         AudioDevice out = this.audio;
         if (out != null && this.bitstream != null) {
            Header h = null;

            try {
               h = this.bitstream.readFrame();
            } catch (BitstreamException var7) {
               try {
                  this.bitstream.close();
               } catch (BitstreamException var6) {
                  var6.printStackTrace();
                  return false;
               }

               var7.printStackTrace();
               return false;
            }

            if (h == null) {
               return false;
            } else {
               SampleBuffer output = (SampleBuffer)this.decoder.decodeFrame(h, this.bitstream);
               synchronized (this) {
                  out = this.audio;
                  if (out != null) {
                     out.writeVol(output.getBuffer(), 0, output.getBufferLength(), this.volume);
                  }
               }

               this.bitstream.closeFrame();
               return true;
            }
         } else {
            return false;
         }
      } catch (RuntimeException var9) {
         var9.printStackTrace();
         return false;
      }
   }

   protected boolean skipFrame() throws JavaLayerException {
      Header h = this.bitstream.readFrame();
      if (h == null) {
         return false;
      } else {
         this.bitstream.closeFrame();
         return true;
      }
   }

   public boolean play(int start, int end) throws JavaLayerException {
      boolean ret = true;
      int offset = start;

      while (offset-- > 0 && ret) {
         ret = this.skipFrame();
      }

      return this.play(end - start);
   }

   private PlaybackEvent createEvent(int id) {
      return this.createEvent(this.audio, id);
   }

   private PlaybackEvent createEvent(AudioDevice dev, int id) {
      return new PlaybackEvent(this, id, dev.getPosition());
   }

   public void setPlayBackListener(PlaybackListener listener) {
      this.listener = listener;
   }

   public PlaybackListener getPlayBackListener() {
      return this.listener;
   }

   public void stop() {
      if (this.listener != null) {
         this.listener.playbackFinished(this.createEvent(PlaybackEvent.STOPPED));
      }

      this.close();
   }
}
