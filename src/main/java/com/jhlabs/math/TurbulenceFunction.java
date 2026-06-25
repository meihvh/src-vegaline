package com.jhlabs.math;

public class TurbulenceFunction extends CompoundFunction2D {
   private float octaves;

   public TurbulenceFunction(Function2D basis, float octaves) {
      super(basis);
      this.octaves = octaves;
   }

   public void setOctaves(float octaves) {
      this.octaves = octaves;
   }

   public float getOctaves() {
      return this.octaves;
   }

   @Override
   public float evaluate(float x, float y) {
      float t = 0.0F;

      for (float f = 1.0F; f <= this.octaves; f *= 2.0F) {
         t += Math.abs(this.basis.evaluate(f * x, f * y)) / f;
      }

      return t;
   }
}
