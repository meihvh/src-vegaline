package com.jhlabs.math;

public class MathFunction1D implements Function1D {
   public static final int SIN = 1;
   public static final int COS = 2;
   public static final int TAN = 3;
   public static final int SQRT = 4;
   public static final int ASIN = -1;
   public static final int ACOS = -2;
   public static final int ATAN = -3;
   public static final int SQR = -4;
   private int operation;

   public MathFunction1D(int operation) {
      this.operation = operation;
   }

   @Override
   public float evaluate(float v) {
      switch (this.operation) {
         case -4:
            return v * v;
         case -3:
            return (float)Math.atan((double)v);
         case -2:
            return (float)Math.acos((double)v);
         case -1:
            return (float)Math.asin((double)v);
         case 0:
         default:
            return v;
         case 1:
            return (float)Math.sin((double)v);
         case 2:
            return (float)Math.cos((double)v);
         case 3:
            return (float)Math.tan((double)v);
         case 4:
            return (float)Math.sqrt((double)v);
      }
   }
}
