package com.jhlabs.composite;

import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;

public final class ColorBurnComposite extends RGBComposite {
   public ColorBurnComposite(float alpha) {
      super(alpha);
   }

   @Override
   public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
      return new ColorBurnComposite.Context(this.extraAlpha, srcColorModel, dstColorModel);
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
            if (sr != 0) {
               dor = Math.max(255 - (255 - dir << 8) / sr, 0);
            } else {
               dor = sr;
            }

            int dog;
            if (sg != 0) {
               dog = Math.max(255 - (255 - dig << 8) / sg, 0);
            } else {
               dog = sg;
            }

            int dob;
            if (sb != 0) {
               dob = Math.max(255 - (255 - dib << 8) / sb, 0);
            } else {
               dob = sb;
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
