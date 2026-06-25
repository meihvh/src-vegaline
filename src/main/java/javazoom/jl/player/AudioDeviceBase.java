package javazoom.jl.player;

import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.JavaLayerException;

public abstract class AudioDeviceBase implements AudioDevice {
   private boolean open = false;
   private Decoder decoder = null;

   @Override
   public synchronized void open(Decoder decoder) throws JavaLayerException {
      if (!this.isOpen()) {
         this.decoder = decoder;
         this.openImpl();
         this.setOpen(true);
      }
   }

   protected void openImpl() throws JavaLayerException {
   }

   protected void setOpen(boolean open) {
      this.open = open;
   }

   @Override
   public synchronized boolean isOpen() {
      return this.open;
   }

   @Override
   public synchronized void close() {
      if (this.isOpen()) {
         this.closeImpl();
         this.setOpen(false);
         this.decoder = null;
      }
   }

   protected void closeImpl() {
   }

   @Override
   public void write(short[] samples, int offs, int len) throws JavaLayerException {
      if (this.isOpen()) {
         this.writeImpl(samples, offs, len);
      }
   }

   @Override
   public void writeVol(short[] samples, int offs, int len, float vol) throws JavaLayerException {
      if (this.isOpen()) {
         this.writeImplVol(samples, offs, len, vol);
      }
   }

   protected void writeImpl(short[] samples, int offs, int len) throws JavaLayerException {
   }

   protected void writeImplVol(short[] samples, int offs, int len, float vol) throws JavaLayerException {
   }

   @Override
   public void flush() {
      if (this.isOpen()) {
         this.flushImpl();
      }
   }

   protected void flushImpl() {
   }

   protected Decoder getDecoder() {
      return this.decoder;
   }
}
