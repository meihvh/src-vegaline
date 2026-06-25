package com.jhlabs.composite;

import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;

public final class SoftLightComposite extends RGBComposite {
   public SoftLightComposite(float alpha) {
      super(alpha);
   }

   @Override
   public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
      return new SoftLightComposite.Context(this.extraAlpha, srcColorModel, dstColorModel);
   }

   static class Context extends RGBComposite.RGBCompositeContext {
      public Context(float alpha, ColorModel srcColorModel, ColorModel dstColorModel) {
         super(alpha, srcColorModel, dstColorModel);
      }

      @Override
      public void composeRGB(int[] src, int[] dst, float alpha) {
         int w = src.length;

         for (int i = 0; i < w; i += 4) {
            int sr = src[i];
            int dir = dst[i];
            int sg = src[i + 1];
            int dig = dst[i + 1];
            int sb = src[i + 2];
            int dib = dst[i + 2];
            int sa = src[i + 3];
            int dia = dst[i + 3];
            int d = multiply255(sr, dir);
            int dor = d + multiply255(dir, 255 - multiply255(255 - dir, 255 - sr) - d);
            d = multiply255(sg, dig);
            int dog = d + multiply255(dig, 255 - multiply255(255 - dig, 255 - sg) - d);
            d = multiply255(sb, dib);
            int dob = d + multiply255(dib, 255 - multiply255(255 - dib, 255 - sb) - d);
            float a = alpha * (float)sa / 255.0F;
            float ac = 1.0F - a;
            dst[i] = (int)(a * (float)dor + ac * (float)dir);
            dst[i + 1] = (int)(a * (float)dog + ac * (float)dig);
            dst[i + 2] = (int)(a * (float)dob + ac * (float)dib);
            dst[i + 3] = (int)((float)sa * alpha + (float)dia * ac);
         }
      }
   }
}
