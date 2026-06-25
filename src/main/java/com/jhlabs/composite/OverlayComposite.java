package com.jhlabs.composite;

import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;

public final class OverlayComposite extends RGBComposite {
   public OverlayComposite(float alpha) {
      super(alpha);
   }

   @Override
   public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
      return new OverlayComposite.Context(this.extraAlpha, srcColorModel, dstColorModel);
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
            int dor;
            if (dir < 128) {
               int t = dir * sr + 128;
               dor = 2 * ((t >> 8) + t >> 8);
            } else {
               int t = (255 - dir) * (255 - sr) + 128;
               dor = 2 * (255 - ((t >> 8) + t >> 8));
            }

            int dog;
            if (dig < 128) {
               int var21 = dig * sg + 128;
               dog = 2 * ((var21 >> 8) + var21 >> 8);
            } else {
               int var22 = (255 - dig) * (255 - sg) + 128;
               dog = 2 * (255 - ((var22 >> 8) + var22 >> 8));
            }

            int dob;
            if (dib < 128) {
               int var23 = dib * sb + 128;
               dob = 2 * ((var23 >> 8) + var23 >> 8);
            } else {
               int var24 = (255 - dib) * (255 - sb) + 128;
               dob = 2 * (255 - ((var24 >> 8) + var24 >> 8));
            }

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
