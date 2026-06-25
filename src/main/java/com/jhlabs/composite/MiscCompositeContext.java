package com.jhlabs.composite;

import java.awt.Color;
import java.awt.CompositeContext;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class MiscCompositeContext implements CompositeContext {
   private int rule;
   private float alpha;
   private ColorModel srcColorModel;
   private ColorModel dstColorModel;
   private ColorSpace srcColorSpace;
   private ColorSpace dstColorSpace;
   private boolean srcNeedsConverting;
   private boolean dstNeedsConverting;

   public MiscCompositeContext(int rule, float alpha, ColorModel srcColorModel, ColorModel dstColorModel) {
      this.rule = rule;
      this.alpha = alpha;
      this.srcColorModel = srcColorModel;
      this.dstColorModel = dstColorModel;
      this.srcColorSpace = srcColorModel.getColorSpace();
      this.dstColorSpace = dstColorModel.getColorSpace();
      ColorModel srgbCM = ColorModel.getRGBdefault();
   }

   @Override
   public void dispose() {
   }

   static int multiply255(int a, int b) {
      int t = a * b + 128;
      return (t >> 8) + t >> 8;
   }

   static int clamp(int a) {
      return a < 0 ? 0 : (a > 255 ? 255 : a);
   }

   @Override
   public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
      float a = 0.0F;
      float ac = 0.0F;
      float alpha = this.alpha;
      float[] sHsv = null;
      float[] diHsv = null;
      float[] doHsv = null;
      switch (this.rule) {
         case 12:
         case 13:
         case 14:
         case 15:
            sHsv = new float[3];
            diHsv = new float[3];
            doHsv = new float[3];
         default:
            int[] srcPix = null;
            int[] dstPix = null;
            int x = dstOut.getMinX();
            int w = dstOut.getWidth();
            int y0 = dstOut.getMinY();
            int y1 = y0 + dstOut.getHeight();

            for (int y = y0; y < y1; y++) {
               srcPix = src.getPixels(x, y, w, 1, srcPix);
               dstPix = dstIn.getPixels(x, y, w, 1, dstPix);
               int i = 0;

               for (int end = w * 4; i < end; i += 4) {
                  int sr = srcPix[i];
                  int dir = dstPix[i];
                  int sg = srcPix[i + 1];
                  int dig = dstPix[i + 1];
                  int sb = srcPix[i + 2];
                  int dib = dstPix[i + 2];
                  int sa = srcPix[i + 3];
                  int dia = dstPix[i + 3];
                  int dor;
                  int dog;
                  int dob;

                  dob = switch (this.rule) {
                     default -> {
                        dor = dir + sr;
                        if (dor > 255) {
                           dor = 255;
                        }

                        dog = dig + sg;
                        if (dog > 255) {
                           dog = 255;
                        }

                        yield dib + sb;
//                        if (dob > 255) {
//                           yield 255;
//                        }
                     }
                     case 2 -> {
                        dor = dir - sr;
                        if (dor < 0) {
                           dor = 0;
                        }

                        dog = dig - sg;
                        if (dog < 0) {
                           dog = 0;
                        }

                        yield dib - sb;
//                        if (dob < 0) {
//                           yield 0;
//                        }
                     }
                     case 3 -> {
                        dor = dir - sr;
                        if (dor < 0) {
                           dor = -dor;
                        }

                        dog = dig - sg;
                        if (dog < 0) {
                           dog = -dog;
                        }

                        yield dib - sb;
//                        if (dob < 0) {
//                           yield -dob;
//                        }
                     }
                     case 4 -> {
                        int t = dir * sr + 128;
                        dor = (t >> 8) + t >> 8;
                        t = dig * sg + 128;
                        dog = (t >> 8) + t >> 8;
                        t = dib * sb + 128;
                        yield (t >> 8) + t >> 8;
                     }
                     case 5 -> {
                        dor = dir < sr ? dir : sr;
                        dog = dig < sg ? dig : sg;
                        yield dib < sb ? dib : sb;
                     }
                     case 6 -> {
                        if (dir != 255) {
                           dor = clamp(255 - (255 - sr << 8) / (dir + 1));
                        } else {
                           dor = sr;
                        }

                        if (dig != 255) {
                           dog = clamp(255 - (255 - sg << 8) / (dig + 1));
                        } else {
                           dog = sg;
                        }

                        if (dib != 255) {
                           yield clamp(255 - (255 - sb << 8) / (dib + 1));
                        } else {
                           yield sb;
                        }
                     }
                     case 7 -> {
                        if (sr != 0) {
                           dor = Math.max(255 - (255 - dir << 8) / sr, 0);
                        } else {
                           dor = sr;
                        }

                        if (sg != 0) {
                           dog = Math.max(255 - (255 - dig << 8) / sg, 0);
                        } else {
                           dog = sg;
                        }

                        if (sb != 0) {
                           yield Math.max(255 - (255 - dib << 8) / sb, 0);
                        } else {
                           yield sb;
                        }
                     }
                     case 8 -> {
                        int t = (255 - dir) * (255 - sr) + 128;
                        dor = 255 - ((t >> 8) + t >> 8);
                        t = (255 - dig) * (255 - sg) + 128;
                        dog = 255 - ((t >> 8) + t >> 8);
                        t = (255 - dib) * (255 - sb) + 128;
                        yield 255 - ((t >> 8) + t >> 8);
                     }
                     case 9 -> {
                        dor = dir > sr ? dir : sr;
                        dog = dig > sg ? dig : sg;
                        yield dib > sb ? dib : sb;
                     }
                     case 10 -> {
                        dor = clamp((sr << 8) / (256 - dir));
                        dog = clamp((sg << 8) / (256 - dig));
                        yield clamp((sb << 8) / (256 - dib));
                     }
                     case 11 -> {
                        if (sr != 255) {
                           dor = Math.min((dir << 8) / (255 - sr), 255);
                        } else {
                           dor = sr;
                        }

                        if (sg != 255) {
                           dog = Math.min((dig << 8) / (255 - sg), 255);
                        } else {
                           dog = sg;
                        }

                        if (sb != 255) {
                           yield Math.min((dib << 8) / (255 - sb), 255);
                        } else {
                           yield sb;
                        }
                     }
                     case 12, 13, 14, 15 -> {
                        Color.RGBtoHSB(sr, sg, sb, sHsv);
                        Color.RGBtoHSB(dir, dig, dib, diHsv);
                        switch (this.rule) {
                           case 12:
                              doHsv[0] = sHsv[0];
                              doHsv[1] = diHsv[1];
                              doHsv[2] = diHsv[2];
                              break;
                           case 13:
                              doHsv[0] = diHsv[0];
                              doHsv[1] = sHsv[1];
                              doHsv[2] = diHsv[2];
                              break;
                           case 14:
                              doHsv[0] = diHsv[0];
                              doHsv[1] = diHsv[1];
                              doHsv[2] = sHsv[2];
                              break;
                           case 15:
                              doHsv[0] = sHsv[0];
                              doHsv[1] = sHsv[1];
                              doHsv[2] = diHsv[2];
                        }

                        int doRGB = Color.HSBtoRGB(doHsv[0], doHsv[1], doHsv[2]);
                        dor = (doRGB & 0xFF0000) >> 16;
                        dog = (doRGB & 0xFF00) >> 8;
                        yield doRGB & 0xFF;
                     }
                     case 16 -> {
                        if (dir < 128) {
                           int t = dir * sr + 128;
                           dor = 2 * ((t >> 8) + t >> 8);
                        } else {
                           int t = (255 - dir) * (255 - sr) + 128;
                           dor = 2 * (255 - ((t >> 8) + t >> 8));
                        }

                        if (dig < 128) {
                           int var37 = dig * sg + 128;
                           dog = 2 * ((var37 >> 8) + var37 >> 8);
                        } else {
                           int var38 = (255 - dig) * (255 - sg) + 128;
                           dog = 2 * (255 - ((var38 >> 8) + var38 >> 8));
                        }

                        if (dib < 128) {
                           int var39 = dib * sb + 128;
                           yield 2 * ((var39 >> 8) + var39 >> 8);
                        } else {
                           int var40 = (255 - dib) * (255 - sb) + 128;
                           yield 2 * (255 - ((var40 >> 8) + var40 >> 8));
                        }
                     }
                     case 17 -> {
                        int d = multiply255(sr, dir);
                        dor = d + multiply255(dir, 255 - multiply255(255 - dir, 255 - sr) - d);
                        d = multiply255(sg, dig);
                        dog = d + multiply255(dig, 255 - multiply255(255 - dig, 255 - sg) - d);
                        d = multiply255(sb, dib);
                        yield d + multiply255(dib, 255 - multiply255(255 - dib, 255 - sb) - d);
                     }
                     case 18 -> {
                        if (sr > 127) {
                           dor = 255 - 2 * multiply255(255 - sr, 255 - dir);
                        } else {
                           dor = 2 * multiply255(sr, dir);
                        }

                        if (sg > 127) {
                           dog = 255 - 2 * multiply255(255 - sg, 255 - dig);
                        } else {
                           dog = 2 * multiply255(sg, dig);
                        }

                        if (sb > 127) {
                           yield 255 - 2 * multiply255(255 - sb, 255 - dib);
                        } else {
                           yield 2 * multiply255(sb, dib);
                        }
                     }
                     case 19 -> {
                        dor = sr > 127 ? Math.max(sr, dir) : Math.min(sr, dir);
                        dog = sg > 127 ? Math.max(sg, dig) : Math.min(sg, dig);
                        yield sb > 127 ? Math.max(sb, dib) : Math.min(sb, dib);
                     }
                     case 20 -> {
                        dor = dir + multiply255(sr, 255 - dir - dir);
                        dog = dig + multiply255(sg, 255 - dig - dig);
                        yield dib + multiply255(sb, 255 - dib - dib);
                     }
                     case 21 -> {
                        dor = 255 - Math.abs(255 - sr - dir);
                        dog = 255 - Math.abs(255 - sg - dig);
                        yield 255 - Math.abs(255 - sb - dib);
                     }
                     case 22 -> {
                        dor = (dir + sr) / 2;
                        dog = (dig + sg) / 2;
                        yield (dib + sb) / 2;
                     }
                  };
                  a = alpha * (float)sa / 255.0F;
                  ac = 1.0F - a;
                  dstPix[i] = (int)(a * (float)dor + ac * (float)dir);
                  dstPix[i + 1] = (int)(a * (float)dog + ac * (float)dig);
                  dstPix[i + 2] = (int)(a * (float)dob + ac * (float)dib);
                  dstPix[i + 3] = (int)((float)sa * alpha + (float)dia * ac);
               }

               dstOut.setPixels(x, y, w, 1, dstPix);
            }
      }
   }
}
