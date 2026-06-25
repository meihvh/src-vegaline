package com.jhlabs.math;

public class FBM implements Function2D {
   protected float[] exponents;
   protected float H;
   protected float lacunarity;
   protected float octaves;
   protected Function2D basis;

   public FBM(float H, float lacunarity, float octaves) {
      this(H, lacunarity, octaves, new Noise());
   }

   public FBM(float H, float lacunarity, float octaves, Function2D basis) {
      this.H = H;
      this.lacunarity = lacunarity;
      this.octaves = octaves;
      this.basis = basis;
      this.exponents = new float[(int)octaves + 1];
      float frequency = 1.0F;

      for (int i = 0; i <= (int)octaves; i++) {
         this.exponents[i] = (float)Math.pow((double)frequency, (double)(-H));
         frequency *= lacunarity;
      }
   }

   public void setBasis(Function2D basis) {
      this.basis = basis;
   }

   public Function2D getBasisType() {
      return this.basis;
   }

   @Override
   public float evaluate(float x, float y) {
      float value = 0.0F;
      x += 371.0F;
      y += 529.0F;

      int i;
      for (i = 0; i < (int)this.octaves; i++) {
         value += this.basis.evaluate(x, y) * this.exponents[i];
         x *= this.lacunarity;
         y *= this.lacunarity;
      }

      float remainder = this.octaves - (float)((int)this.octaves);
      if (remainder != 0.0F) {
         value += remainder * this.basis.evaluate(x, y) * this.exponents[i];
      }

      return value;
   }
}
