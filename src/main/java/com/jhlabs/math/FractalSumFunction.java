package com.jhlabs.math;

public class FractalSumFunction extends CompoundFunction2D {
   private float octaves = 1.0F;

   public FractalSumFunction(Function2D basis) {
      super(basis);
   }

   @Override
   public float evaluate(float x, float y) {
      float t = 0.0F;

      for (float f = 1.0F; f <= this.octaves; f *= 2.0F) {
         t += this.basis.evaluate(f * x, f * y) / f;
      }

      return t;
   }
}
