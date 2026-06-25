package javazoom.jl.player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.DataLine.Info;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.JavaLayerException;

public class JavaSoundAudioDevice extends AudioDeviceBase {
   private SourceDataLine source = null;
   private AudioFormat fmt = null;
   private byte[] byteBuf = new byte[4096];

   protected void setAudioFormat(AudioFormat fmt0) {
      this.fmt = fmt0;
   }

   protected AudioFormat getAudioFormat() {
      if (this.fmt == null) {
         Decoder decoder = this.getDecoder();
         this.fmt = new AudioFormat((float)decoder.getOutputFrequency(), 16, decoder.getOutputChannels(), true, false);
      }

      return this.fmt;
   }

   protected Info getSourceLineInfo() {
      AudioFormat fmt = this.getAudioFormat();
      return new Info(SourceDataLine.class, fmt);
   }

   public void open(AudioFormat fmt) throws JavaLayerException {
      if (!this.isOpen()) {
         this.setAudioFormat(fmt);
         this.openImpl();
         this.setOpen(true);
      }
   }

   @Override
   protected void openImpl() throws JavaLayerException {
   }

   protected void createSource() throws JavaLayerException {
      Throwable t = null;

      try {
         Line line = AudioSystem.getLine(this.getSourceLineInfo());
         if (line instanceof SourceDataLine) {
            this.source = (SourceDataLine)line;
            this.source.open(this.fmt);
            this.source.start();
         }
      } catch (RuntimeException var3) {
         t = var3;
      } catch (LinkageError var4) {
         t = var4;
      } catch (LineUnavailableException var5) {
         t = var5;
      }

      if (this.source == null) {
         throw new JavaLayerException("cannot obtain source audio line", t);
      }
   }

   public int millisecondsToBytes(AudioFormat fmt, int time) {
      return (int)((double)((float)time * fmt.getSampleRate() * (float)fmt.getChannels() * (float)fmt.getSampleSizeInBits()) / 8000.0);
   }

   @Override
   protected void closeImpl() {
      if (this.source != null) {
         this.source.close();
      }
   }

   @Override
   protected void writeImpl(short[] samples, int offs, int len) throws JavaLayerException {
      if (this.source == null) {
         this.createSource();
      }

      byte[] b = this.toByteArray(samples, offs, len);
      this.source.write(b, 0, len * 2);
   }

   @Override
   protected void writeImplVol(short[] samples, int offs, int len, float vol) throws JavaLayerException {
      if (this.source == null) {
         this.createSource();
      }

      for (int i = offs; i < offs + len; i++) {
         samples[i] = (short)((int)((float)samples[i] * vol));
      }

      byte[] b = this.toByteArray(samples, offs, len);
      this.source.write(b, 0, len * 2);
   }

   protected byte[] getByteArray(int length) {
      if (this.byteBuf.length < length) {
         this.byteBuf = new byte[length + 1024];
      }

      return this.byteBuf;
   }

   protected byte[] toByteArray(short[] samples, int offs, int len) {
      byte[] b = this.getByteArray(len * 2);
      int idx = 0;

      while (len-- > 0) {
         short s = samples[offs++];
         b[idx++] = (byte)s;
         b[idx++] = (byte)(s >>> 8);
      }

      return b;
   }

   @Override
   protected void flushImpl() {
      if (this.source != null) {
         this.source.drain();
      }
   }

   @Override
   public int getPosition() {
      int pos = 0;
      if (this.source != null) {
         pos = (int)(this.source.getMicrosecondPosition() / 1000L);
      }

      return pos;
   }

   public void test() throws JavaLayerException {
      try {
         this.open(new AudioFormat(22050.0F, 16, 1, true, false));
         short[] data = new short[2205];
         this.write(data, 0, data.length);
         this.flush();
         this.close();
      } catch (RuntimeException var2) {
         throw new JavaLayerException("Device test failed: " + var2);
      }
   }
}
