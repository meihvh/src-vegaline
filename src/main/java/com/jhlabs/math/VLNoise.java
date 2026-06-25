package com.jhlabs.math;

public class VLNoise implements Function2D {
   private float distortion = 10.0F;

   public void setDistortion(float distortion) {
      this.distortion = distortion;
   }

   public float getDistortion() {
      return this.distortion;
   }

   @Override
   public float evaluate(float x, float y) {
      float ox = Noise.noise2(x + 0.5F, y) * this.distortion;
      float oy = Noise.noise2(x, y + 0.5F) * this.distortion;
      return Noise.noise2(x + ox, y + oy);
   }
}
