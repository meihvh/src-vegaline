package javazoom.jl.decoder;

public final class Equalizer {
   public static final float BAND_NOT_PRESENT = Float.NEGATIVE_INFINITY;
   public static final Equalizer PASS_THRU_EQ = new Equalizer();
   public static final int BANDS = 32;
   private final float[] settings = new float[32];

   public Equalizer() {
   }

   public Equalizer(float[] settings) {
      this.setFrom(settings);
   }

   public Equalizer(Equalizer.EQFunction eq) {
      this.setFrom(eq);
   }

   public void setFrom(float[] eq) {
      this.reset();
      int max = eq.length > 32 ? 32 : eq.length;
      System.arraycopy(eq, 0, this.settings, 0, max);
   }

   public void setFrom(Equalizer.EQFunction eq) {
      this.reset();
      int max = 32;

      for (int i = 0; i < max; i++) {
         this.settings[i] = eq.getBand(i);
      }
   }

   public void setFrom(Equalizer eq) {
      if (eq != this) {
         this.setFrom(eq.settings);
      }
   }

   public void reset() {
      for (int i = 0; i < 32; i++) {
         this.settings[i] = 0.0F;
      }
   }

   public int getBandCount() {
      return this.settings.length;
   }

   public float setBand(int band, float neweq) {
      float eq = 0.0F;
      if (band >= 0 && band < 32) {
         eq = this.settings[band];
         this.settings[band] = neweq;
      }

      return eq;
   }

   public float getBand(int band) {
      float eq = 0.0F;
      if (band >= 0 && band < 32) {
         eq = this.settings[band];
      }

      return eq;
   }

   private float limit(float eq) {
      if (eq == Float.NEGATIVE_INFINITY) {
         return eq;
      } else if (eq > 1.0F) {
         return 1.0F;
      } else {
         return eq < -1.0F ? -1.0F : eq;
      }
   }

   float[] getBandFactors() {
      float[] factors = new float[32];
      int i = 0;

      for (int maxCount = 32; i < maxCount; i++) {
         factors[i] = this.getBandFactor(this.settings[i]);
      }

      return factors;
   }

   float getBandFactor(float eq) {
      return eq == Float.NEGATIVE_INFINITY ? 0.0F : (float)Math.pow(2.0, (double)eq);
   }

   public abstract static class EQFunction {
      public float getBand(int band) {
         return 0.0F;
      }
   }
}
