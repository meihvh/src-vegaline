package javazoom.jl.decoder;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamSource implements Source {
   private final InputStream in;

   public InputStreamSource(InputStream in) {
      if (in == null) {
         throw new NullPointerException("in");
      } else {
         this.in = in;
      }
   }

   @Override
   public int read(byte[] b, int offs, int len) throws IOException {
      return this.in.read(b, offs, len);
   }

   @Override
   public boolean willReadBlock() {
      return true;
   }

   @Override
   public boolean isSeekable() {
      return false;
   }

   @Override
   public long tell() {
      return -1L;
   }

   @Override
   public long seek(long to) {
      return -1L;
   }

   @Override
   public long length() {
      return -1L;
   }
}
