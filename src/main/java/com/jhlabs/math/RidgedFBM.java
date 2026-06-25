package com.jhlabs.math;

public class RidgedFBM implements Function2D {
   @Override
   public float evaluate(float x, float y) {
      return 1.0F - Math.abs(Noise.noise2(x, y));
   }
}
