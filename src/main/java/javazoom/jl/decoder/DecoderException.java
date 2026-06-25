package javazoom.jl.decoder;

public class DecoderException extends JavaLayerException implements DecoderErrors {
   private int errorcode = 512;

   public DecoderException(String msg, Throwable t) {
      super(msg, t);
   }

   public DecoderException(int errorcode, Throwable t) {
      this(getErrorString(errorcode), t);
      this.errorcode = errorcode;
   }

   public int getErrorCode() {
      return this.errorcode;
   }

   public static String getErrorString(int errorcode) {
      return "Decoder errorcode " + Integer.toHexString(errorcode);
   }
}
