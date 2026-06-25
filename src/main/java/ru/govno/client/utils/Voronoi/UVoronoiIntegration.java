package ru.govno.client.utils.Voronoi;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.ColorUtils;

public class UVoronoiIntegration {
   private VoronoiOfQuad voronoi;

   public static UVoronoiIntegration generateDefault(int pointsCount, boolean createInitThread) {
      return new UVoronoiIntegration(new VoronoiOfQuad(-0.5F, -0.5F, 0.5F, 0.5F, pointsCount, createInitThread));
   }

   public static UVoronoiIntegration generateDefault(int pointsCount, float scale, boolean createInitThread) {
      float ext = 0.5F * scale;
      return new UVoronoiIntegration(new VoronoiOfQuad(-ext, -ext, ext, ext, pointsCount, createInitThread));
   }

   public UVoronoiIntegration(VoronoiOfQuad voronoi) {
      this.voronoi = voronoi;
   }

   public UVoronoiIntegration setVoronoi(VoronoiOfQuad voronoi) {
      this.voronoi = voronoi;
      return this;
   }

   public List<VoronoiOfQuad.Polygon> getSpreadPolygons(boolean asCopied, float mulTrans, float mulRotate) {
      List<VoronoiOfQuad.Polygon> polygons = asCopied
         ? this.voronoi.getPolygons().stream().map(VoronoiOfQuad.Polygon::copy).toList()
         : this.voronoi.getPolygons();
      if (mulTrans == 0.0F && mulRotate == 0.0F) {
         return polygons;
      } else if (mulTrans != 0.0F && mulRotate != 0.0F) {
         return polygons.stream()
            .map(
               poly -> poly.translateAwayPosLoc(this.voronoi.cx, this.voronoi.cy, poly.distanceToAtCenter(this.voronoi.cx, this.voronoi.cy) * mulTrans)
                     .rotateAtYOfAngleAwayPos(this.voronoi.cx, this.voronoi.cy, poly.distanceToAtCenter(this.voronoi.cx, this.voronoi.cy) * mulRotate)
            )
            .toList();
      } else {
         return mulTrans != 0.0F
            ? polygons.stream()
               .map(poly -> poly.translateAwayPosLoc(this.voronoi.cx, this.voronoi.cy, poly.distanceToAtCenter(this.voronoi.cx, this.voronoi.cy) * mulTrans))
               .toList()
            : polygons.stream()
               .map(
                  poly -> poly.rotateAtYOfAngleAwayPos(this.voronoi.cx, this.voronoi.cy, poly.distanceToAtCenter(this.voronoi.cx, this.voronoi.cy) * mulRotate)
               )
               .toList();
      }
   }

   public void renderBindTextureSegments(boolean temporalMode, int begin, float trans, float rot, float aPC, int color, int tryDraws) {
      if (this.voronoi != null && tryDraws > 0) {
         color = ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) * aPC);
         List<VoronoiOfQuad.Polygon> polygonsToDraw = this.getSpreadPolygons(temporalMode, trans, rot);
         if (!polygonsToDraw.isEmpty()) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            int indexPoly = 0;

            for (VoronoiOfQuad.Polygon renderPolygon : polygonsToDraw) {
               VoronoiOfQuad.Polygon staticPolygon = this.voronoi.getPolygons().get(indexPoly);
               if (staticPolygon != null) {
                  List<VoronoiOfQuad.Vec2f> staticVecList = staticPolygon.getAllVertices();
                  List<VoronoiOfQuad.Vec2f> dynamicVecList = renderPolygon.getAllVertices();
                  List<UVoronoiIntegration.Vec2fUVC> verticesTexCol = new ArrayList<>();
                  int vertIndex = 0;

                  for (VoronoiOfQuad.Vec2f forPosVec : dynamicVecList) {
                     VoronoiOfQuad.Vec2f forTexVec = staticVecList.get(vertIndex);
                     if (forTexVec != null) {
                        verticesTexCol.add(
                           UVoronoiIntegration.Vec2fUVC.asVecs2f(
                              forTexVec, forPosVec, renderPolygon.uniqueInt, this.voronoi.x, this.voronoi.y, this.voronoi.x2, this.voronoi.y2
                           )
                        );
                        vertIndex++;
                     }
                  }

                  if (!verticesTexCol.isEmpty()) {
                     buffer.begin(begin, DefaultVertexFormats.POSITION_TEX_COLOR);

                     for (UVoronoiIntegration.Vec2fUVC bufferVec : verticesTexCol) {
                        bufferVec.doVertexTexCol(buffer, color);
                     }

                     this.prepareRenderStart2D();
                     tessellator.draw(tryDraws);
                     this.prepareRenderStop2D();
                  }

                  indexPoly++;
               }
            }
         }
      }
   }

   public void renderBindTextureSegmentsRevUV(boolean temporalMode, int begin, float trans, float rot, float aPC, int color, int tryDraws) {
      if (this.voronoi != null && tryDraws > 0) {
         color = ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) * aPC);
         List<VoronoiOfQuad.Polygon> polygonsToDraw = this.getSpreadPolygons(temporalMode, trans, rot);
         if (!polygonsToDraw.isEmpty()) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            int indexPoly = 0;

            for (VoronoiOfQuad.Polygon renderPolygon : polygonsToDraw) {
               VoronoiOfQuad.Polygon staticPolygon = this.voronoi.getPolygons().get(indexPoly);
               if (staticPolygon != null) {
                  List<VoronoiOfQuad.Vec2f> staticVecList = staticPolygon.getAllVertices();
                  List<VoronoiOfQuad.Vec2f> dynamicVecList = renderPolygon.getAllVertices();
                  List<UVoronoiIntegration.Vec2fUVC> verticesTexCol = new ArrayList<>();
                  int vertIndex = 0;

                  for (VoronoiOfQuad.Vec2f forPosVec : dynamicVecList) {
                     VoronoiOfQuad.Vec2f forTexVec = staticVecList.get(vertIndex);
                     if (forTexVec != null) {
                        verticesTexCol.add(
                           UVoronoiIntegration.Vec2fUVC.asVecs2f(
                              forTexVec, forPosVec, renderPolygon.uniqueInt, this.voronoi.x, this.voronoi.y, this.voronoi.x2, this.voronoi.y2
                           )
                        );
                        vertIndex++;
                     }
                  }

                  if (!verticesTexCol.isEmpty()) {
                     buffer.begin(begin, DefaultVertexFormats.POSITION_TEX_COLOR);

                     for (UVoronoiIntegration.Vec2fUVC bufferVec : verticesTexCol) {
                        bufferVec.doVertexTexColRevUV(buffer, color);
                     }

                     this.prepareRenderStart2D();
                     tessellator.draw(tryDraws);
                     this.prepareRenderStop2D();
                  }

                  indexPoly++;
               }
            }
         }
      }
   }

   public void renderBindTextureSegments(
      boolean temporalMode, int begin, float trans, float rot, float aPC, int color1, int color2, int color3, int color4, int tryDraws
   ) {
      if (this.voronoi != null && tryDraws > 0) {
         List<VoronoiOfQuad.Polygon> polygonsToDraw = this.getSpreadPolygons(temporalMode, trans, rot);
         if (!polygonsToDraw.isEmpty()) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            int indexPoly = 0;

            for (VoronoiOfQuad.Polygon renderPolygon : polygonsToDraw) {
               VoronoiOfQuad.Polygon staticPolygon = this.voronoi.getPolygons().get(indexPoly);
               if (staticPolygon != null) {
                  List<VoronoiOfQuad.Vec2f> staticVecList = staticPolygon.getAllVertices();
                  List<VoronoiOfQuad.Vec2f> dynamicVecList = renderPolygon.getAllVertices();
                  List<UVoronoiIntegration.Vec2fUVC> verticesTexCol = new ArrayList<>();
                  int vertIndex = 0;

                  for (VoronoiOfQuad.Vec2f forPosVec : dynamicVecList) {
                     VoronoiOfQuad.Vec2f forTexVec = staticVecList.get(vertIndex);
                     if (forTexVec != null) {
                        verticesTexCol.add(
                           UVoronoiIntegration.Vec2fUVC.asVecs2f(
                              forTexVec, forPosVec, renderPolygon.uniqueInt, this.voronoi.x, this.voronoi.y, this.voronoi.x2, this.voronoi.y2
                           )
                        );
                        vertIndex++;
                     }
                  }

                  if (!verticesTexCol.isEmpty()) {
                     buffer.begin(begin, DefaultVertexFormats.POSITION_TEX_COLOR);

                     for (UVoronoiIntegration.Vec2fUVC bufferVec : verticesTexCol) {
                        bufferVec.doVertexTexCol(buffer, color1, color2, color3, color4, aPC);
                     }

                     this.prepareRenderStart2D();
                     tessellator.draw(tryDraws);
                     this.prepareRenderStop2D();
                  }

                  indexPoly++;
               }
            }
         }
      }
   }

   public void renderBindTextureSegmentsRevUV(
      boolean temporalMode, int begin, float trans, float rot, float aPC, int color1, int color2, int color3, int color4, int tryDraws
   ) {
      if (this.voronoi != null && tryDraws > 0) {
         List<VoronoiOfQuad.Polygon> polygonsToDraw = this.getSpreadPolygons(temporalMode, trans, rot);
         if (!polygonsToDraw.isEmpty()) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            int indexPoly = 0;

            for (VoronoiOfQuad.Polygon renderPolygon : polygonsToDraw) {
               VoronoiOfQuad.Polygon staticPolygon = this.voronoi.getPolygons().get(indexPoly);
               if (staticPolygon != null) {
                  List<VoronoiOfQuad.Vec2f> staticVecList = staticPolygon.getAllVertices();
                  List<VoronoiOfQuad.Vec2f> dynamicVecList = renderPolygon.getAllVertices();
                  List<UVoronoiIntegration.Vec2fUVC> verticesTexCol = new ArrayList<>();
                  int vertIndex = 0;

                  for (VoronoiOfQuad.Vec2f forPosVec : dynamicVecList) {
                     VoronoiOfQuad.Vec2f forTexVec = staticVecList.get(vertIndex);
                     if (forTexVec != null) {
                        verticesTexCol.add(
                           UVoronoiIntegration.Vec2fUVC.asVecs2f(
                              forTexVec, forPosVec, renderPolygon.uniqueInt, this.voronoi.x, this.voronoi.y, this.voronoi.x2, this.voronoi.y2
                           )
                        );
                        vertIndex++;
                     }
                  }

                  if (!verticesTexCol.isEmpty()) {
                     buffer.begin(begin, DefaultVertexFormats.POSITION_TEX_COLOR);

                     for (UVoronoiIntegration.Vec2fUVC bufferVec : verticesTexCol) {
                        bufferVec.doVertexTexColRevUV(buffer, color1, color2, color3, color4, aPC);
                     }

                     this.prepareRenderStart2D();
                     tessellator.draw(tryDraws);
                     this.prepareRenderStop2D();
                  }

                  indexPoly++;
               }
            }
         }
      }
   }

   private void prepareRenderStart2D() {
   }

   private void prepareRenderStop2D() {
   }

   public static class Vec2fUVC {
      private final float x;
      private final float y;
      private final float u;
      private final float v;
      private int c;

      public Vec2fUVC(float x, float y, float u, float v, int c) {
         this.x = x;
         this.y = y;
         this.u = u;
         this.v = v;
         this.c = c;
      }

      public static UVoronoiIntegration.Vec2fUVC asVecs2f(
         VoronoiOfQuad.Vec2f vecStaticIn, VoronoiOfQuad.Vec2f vecDynamicIn, int color, float imageX, float imageY, float imageX2, float imageY2
      ) {
         float[] uv = vecStaticIn.getUVTexXY(imageX, imageY, imageX2, imageY2);
         return new UVoronoiIntegration.Vec2fUVC(vecDynamicIn.x, vecDynamicIn.y, uv[0], uv[1], color);
      }

      public int updateColorAs4i(int c1, int c2, int c3, int c4) {
         this.c = ColorUtils.getQuadColor(c1, c2, c3, c4, this.u, this.v);
         return this.c;
      }

      public int getAColor(float aPC) {
         return aPC == 1.0F ? this.c : ColorUtils.swapAlpha(this.c, (float)ColorUtils.getAlphaFromColor(this.c) * aPC);
      }

      public void doVertexTexCol(BufferBuilder buffer, int colorIn) {
         buffer.pos((double)this.x, (double)this.y).tex((double)this.u, (double)this.v).color(colorIn).endVertex();
      }

      public void doVertexTexColRevUV(BufferBuilder buffer, int colorIn) {
         buffer.pos((double)this.x, (double)this.y).tex((double)(1.0F - this.u), (double)(1.0F - this.v)).color(colorIn).endVertex();
      }

      public void doVertexTexCol(BufferBuilder buffer, int baseColor1, int baseColor2, int baseColor3, int baseColor4, float alphaPC) {
         float lrpColX = this.u > 0.5F ? MathUtils.lerp(this.u, 1.0F, this.u) : MathUtils.lerp(this.u, 0.0F, this.u);
         float lrpColY = this.v > 0.5F ? MathUtils.lerp(this.v, 1.0F, this.v) : MathUtils.lerp(this.v, 0.0F, this.v);
         int colorMixed = ColorUtils.getQuadColor(baseColor1, baseColor2, baseColor3, baseColor4, lrpColX, lrpColY);
         buffer.pos((double)this.x, (double)this.y)
            .tex((double)this.u, (double)this.v)
            .color(ColorUtils.swapAlpha(colorMixed, (float)ColorUtils.getAlphaFromColor(colorMixed) * alphaPC))
            .endVertex();
      }

      public void doVertexTexColRevUV(BufferBuilder buffer, int baseColor1, int baseColor2, int baseColor3, int baseColor4, float alphaPC) {
         float lrpColX = this.u > 0.5F ? MathUtils.lerp(this.u, 1.0F, this.u) : MathUtils.lerp(this.u, 0.0F, this.u);
         float lrpColY = this.v > 0.5F ? MathUtils.lerp(this.v, 1.0F, this.v) : MathUtils.lerp(this.v, 0.0F, this.v);
         int colorMixed = ColorUtils.getQuadColor(baseColor1, baseColor2, baseColor3, baseColor4, lrpColX, lrpColY);
         buffer.pos((double)this.x, (double)this.y)
            .tex((double)(1.0F - this.u), (double)(1.0F - this.v))
            .color(ColorUtils.swapAlpha(colorMixed, (float)ColorUtils.getAlphaFromColor(colorMixed) * alphaPC))
            .endVertex();
      }
   }
}
