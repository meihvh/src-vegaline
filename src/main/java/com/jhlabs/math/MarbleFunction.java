package com.jhlabs.math;

public class MarbleFunction extends CompoundFunction2D {
   public MarbleFunction() {
      super(new TurbulenceFunction(new Noise(), 6.0F));
   }

   public MarbleFunction(Function2D basis) {
      super(basis);
   }

   @Override
   public float evaluate(float x, float y) {
      return (float)Math.pow(0.5 * (Math.sin(8.0 * (double)this.basis.evaluate(x, y)) + 1.0), 0.77);
   }
}
