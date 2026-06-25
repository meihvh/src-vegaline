package javazoom.jl.player;

import java.io.InputStream;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;

public class Player {
   private int frame = 0;
   private Bitstream bitstream;
   private Decoder decoder;
   private AudioDevice audio;
   private boolean closed = false;
   private boolean complete = false;
   private int lastPosition = 0;

   public Player(InputStream stream) throws JavaLayerException {
      this(stream, null);
   }

   public Player(InputStream stream, AudioDevice device) throws JavaLayerException {
      this.bitstream = new Bitstream(stream);
      this.decoder = new Decoder();
      if (device != null) {
         this.audio = device;
      } else {
         FactoryRegistry r = FactoryRegistry.systemRegistry();
         this.audio = r.createAudioDevice();
      }

      this.audio.open(this.decoder);
   }

   public void play() throws JavaLayerException {
      this.play(Integer.MAX_VALUE);
   }

   public boolean play(int frames) throws JavaLayerException {
      boolean ret = true;

      while (frames-- > 0 && ret) {
         ret = this.decodeFrame();
      }

      if (!ret) {
         AudioDevice out = this.audio;
         if (out != null) {
            out.flush();
            synchronized (this) {
               this.complete = !this.closed;
               this.close();
            }
         }
      }

      return ret;
   }

   public synchronized void close() {
      AudioDevice out = this.audio;
      if (out != null) {
         this.closed = true;
         this.audio = null;
         out.close();
         this.lastPosition = out.getPosition();

         try {
            this.bitstream.close();
         } catch (BitstreamException var3) {
         }
      }
   }

   public synchronized boolean isComplete() {
      return this.complete;
   }

   public int getPosition() {
      int position = this.lastPosition;
      AudioDevice out = this.audio;
      if (out != null) {
         position = out.getPosition();
      }

      return position;
   }

   protected boolean decodeFrame() throws JavaLayerException {
      try {
         AudioDevice out = this.audio;
         if (out == null) {
            return false;
         } else {
            Header h = this.bitstream.readFrame();
            if (h == null) {
               return false;
            } else {
               SampleBuffer output = (SampleBuffer)this.decoder.decodeFrame(h, this.bitstream);
               synchronized (this) {
                  out = this.audio;
                  if (out != null) {
                     out.write(output.getBuffer(), 0, output.getBufferLength());
                  }
               }

               this.bitstream.closeFrame();
               return true;
            }
         }
      } catch (RuntimeException var7) {
         throw new JavaLayerException("Exception decoding audio frame", var7);
      }
   }
}
