package ru.govno.client.utils.Voronoi;

import com.google.common.collect.Lists;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import net.minecraft.util.math.MathHelper;

public class VoronoiOfQuad {
   private List<VoronoiOfQuad.Polygon> polygons = Lists.newArrayList();
   private List<VoronoiOfQuad.Polygon> polygonsDontTouch = Lists.newArrayList();
   public float x;
   public float y;
   public float x2;
   public float y2;
   public float cx;
   public float cy;
   private static final Random RANDOM = new Random();
   private final ArrayList<VoronoiOfQuad.Polygon> tempListPolygon = new ArrayList<>();

   public VoronoiOfQuad(float x, float y, float x2, float y2, List<VoronoiOfQuad.Vec2f> points, boolean createInitThread) {
      this.x = x;
      this.y = y;
      this.x2 = x2;
      this.y2 = y2;
      this.cx = this.x + (this.x2 - this.x) / 2.0F;
      this.cy = this.y + (this.y2 - this.y) / 2.0F;
      if (createInitThread) {
         CompletableFuture.runAsync(() -> {
            this.polygons = this.getVoronoiPolygons(x, y, x2, y2, points);
            this.polygonsDontTouch = this.polygons.stream().map(VoronoiOfQuad.Polygon::copy).toList();
         });
      } else {
         this.polygons = this.getVoronoiPolygons(x, y, x2, y2, points);
         this.polygonsDontTouch = this.polygons.stream().map(VoronoiOfQuad.Polygon::copy).toList();
      }
   }

   public VoronoiOfQuad(float x, float y, float x2, float y2, int countOfPoints, boolean createInitThread) {
      this.x = x;
      this.y = y;
      this.x2 = x2;
      this.y2 = y2;
      this.cx = this.x + (this.x2 - this.x) / 2.0F;
      this.cy = this.y + (this.y2 - this.y) / 2.0F;
      if (createInitThread) {
         CompletableFuture.runAsync(() -> {
            this.polygons = this.getVoronoiPolygons(x, y, x2, y2, this.genPointsInBounds(x, y, x2, y2, countOfPoints));
            this.polygonsDontTouch = this.polygons.stream().map(VoronoiOfQuad.Polygon::copy).toList();
         });
      } else {
         this.polygons = this.getVoronoiPolygons(x, y, x2, y2, this.genPointsInBounds(x, y, x2, y2, countOfPoints));
         this.polygonsDontTouch = this.polygons.stream().map(VoronoiOfQuad.Polygon::copy).toList();
      }
   }

   public List<VoronoiOfQuad.Polygon> getPolygons() {
      return this.polygons;
   }

   private List<VoronoiOfQuad.Polygon> asListOfLists(ArrayList<ArrayList<VoronoiOfQuad.Vec2f>> listsInLists) {
      List<VoronoiOfQuad.Polygon> list = new ArrayList<>();

      for (ArrayList<VoronoiOfQuad.Vec2f> listInList : listsInLists) {
         list.add(new VoronoiOfQuad.Polygon(listInList));
      }

      return list;
   }

   private List<VoronoiOfQuad.Vec2f> genPointsInBounds(float x, float y, float x2, float y2, int countOfPoints) {
      return IntStream.range(0, countOfPoints).mapToObj(i -> new VoronoiOfQuad.Vec2f(RANDOM.nextFloat(x, x2), RANDOM.nextFloat(y, y2))).toList();
   }

   public List<VoronoiOfQuad.Polygon> getVoronoiPolygons(float x, float y, float x2, float y2, List<VoronoiOfQuad.Vec2f> points) {
      if (!this.tempListPolygon.isEmpty()) {
         this.tempListPolygon.clear();
      }

      List<VoronoiOfQuad.Vec2f> boundsRectVecList = Arrays.asList(
         new VoronoiOfQuad.Vec2f(x, y), new VoronoiOfQuad.Vec2f(x2, y), new VoronoiOfQuad.Vec2f(x2, y2), new VoronoiOfQuad.Vec2f(x, y2)
      );
      int countOfPoints = points.size();

      for (int iLevel0 = 0; iLevel0 < countOfPoints; iLevel0++) {
         List<VoronoiOfQuad.Vec2f> current = new ArrayList<>(boundsRectVecList);

         for (int iLevel1 = 0; iLevel1 < countOfPoints; iLevel1++) {
            if (iLevel1 != iLevel0) {
               float a = 2.0F * (points.get(iLevel1).x - points.get(iLevel0).x);
               float b = 2.0F * (points.get(iLevel1).y - points.get(iLevel0).y);
               float c = points.get(iLevel0).x * points.get(iLevel0).x
                  + points.get(iLevel0).y * points.get(iLevel0).y
                  - (points.get(iLevel1).x * points.get(iLevel1).x + points.get(iLevel1).y * points.get(iLevel1).y);
               current = this.clipPolygon(current, a, b, c);
               if (current.isEmpty()) {
                  break;
               }
            }
         }

         if (!current.isEmpty()) {
            this.tempListPolygon.add(new VoronoiOfQuad.Polygon(new ArrayList<>(current)));
         }
      }

      return this.tempListPolygon;
   }

   private List<VoronoiOfQuad.Vec2f> clipPolygon(List<VoronoiOfQuad.Vec2f> polygon, float a, float b, float c) {
      List<VoronoiOfQuad.Vec2f> tempListVec2f = new ArrayList<>();
      int n = polygon.size();

      for (int i = 0; i < n; i++) {
         VoronoiOfQuad.Vec2f p1 = polygon.get(i);
         VoronoiOfQuad.Vec2f p2 = polygon.get((i + 1) % n);
         boolean s1 = this.isInside(p1, a, b, c);
         boolean s2 = this.isInside(p2, a, b, c);
         if (s1) {
            if (!s2) {
               VoronoiOfQuad.Vec2f intersect;
               if ((intersect = this.findIntersection(p1, p2, a, b, c)) != null) {
                  tempListVec2f.add(intersect);
               }
            } else {
               tempListVec2f.add(p2);
            }
         } else if (s2) {
            VoronoiOfQuad.Vec2f intersect;
            if ((intersect = this.findIntersection(p1, p2, a, b, c)) != null) {
               tempListVec2f.add(intersect);
            }

            tempListVec2f.add(p2);
         }
      }

      return tempListVec2f;
   }

   private boolean isInside(VoronoiOfQuad.Vec2f p, float a, float b, float c) {
      return a * p.x + b * p.y + c <= 0.0F;
   }

   private VoronoiOfQuad.Vec2f findIntersection(VoronoiOfQuad.Vec2f p1, VoronoiOfQuad.Vec2f p2, float a, float b, float c) {
      float dx = p2.x - p1.x;
      float dy = p2.y - p1.y;
      float denom = a * dx + b * dy;
      if ((double)Math.abs(denom) < 1.0E-4) {
         return null;
      } else {
         float num = -(a * p1.x + b * p1.y + c);
         float t = num / denom;
         return t >= 0.0F && t <= 1.0F ? new VoronoiOfQuad.Vec2f(p1.x + t * dx, p1.y + t * dy) : null;
      }
   }

   public class Polygon {
      public VoronoiOfQuad.Vec2f center;
      public ArrayList<VoronoiOfQuad.Vec2f> list;
      public int uniqueInt = Color.HSBtoRGB(VoronoiOfQuad.RANDOM.nextFloat(), 0.9F, 1.0F);

      public Polygon(ArrayList<VoronoiOfQuad.Vec2f> listVertices) {
         this.list = listVertices;
         this.center = this.getCenter();
      }

      public VoronoiOfQuad.Polygon copy(VoronoiOfQuad.Polygon polygon) {
         return VoronoiOfQuad.this.new Polygon(new ArrayList<>(polygon.list.stream().map(vec -> new VoronoiOfQuad.Vec2f(vec.x, vec.y)).toList()));
      }

      public VoronoiOfQuad.Polygon copy() {
         return VoronoiOfQuad.this.new Polygon(new ArrayList<>(this.list.stream().map(vec -> new VoronoiOfQuad.Vec2f(vec.x, vec.y)).toList()));
      }

      public List<VoronoiOfQuad.Vec2f> getAllVertices() {
         return this.list;
      }

      public VoronoiOfQuad.Vec2f getCenter() {
         VoronoiOfQuad.Vec2f center = new VoronoiOfQuad.Vec2f(0.0F, 0.0F);

         for (VoronoiOfQuad.Vec2f vec : this.list) {
            center.x = center.x + vec.x;
            center.y = center.y + vec.y;
         }

         int size = this.list.size();
         center.x /= (float)size;
         center.y /= (float)size;
         return center;
      }

      public List<VoronoiOfQuad.Vec2f> getBoundsQuad() {
         float xMin = Float.MAX_VALUE;
         float xMax = Float.MIN_VALUE;
         float yMin = Float.MAX_VALUE;
         float yMax = Float.MIN_VALUE;

         for (VoronoiOfQuad.Vec2f vec : this.list) {
            if (xMin > vec.x) {
               xMin = vec.x;
            }

            if (xMax < vec.x) {
               xMax = vec.x;
            }

            if (yMin > vec.y) {
               yMin = vec.y;
            }

            if (yMax < vec.y) {
               yMax = vec.y;
            }
         }

         return Arrays.asList(
            new VoronoiOfQuad.Vec2f(xMin, yMin), new VoronoiOfQuad.Vec2f(xMax, yMin), new VoronoiOfQuad.Vec2f(xMax, yMax), new VoronoiOfQuad.Vec2f(xMin, yMax)
         );
      }

      public VoronoiOfQuad.Polygon moveXY(float mX, float mY) {
         this.center.x += mX;
         this.center.y += mY;

         for (VoronoiOfQuad.Vec2f vec : this.list) {
            vec.x += mX;
            vec.y += mY;
         }

         return this;
      }

      public VoronoiOfQuad.Polygon setPolygonMidPos(float xIn, float yIn) {
         float dx = xIn - this.center.x;
         float dy = yIn - this.center.y;
         return dx == 0.0F && dy == 0.0F ? this : this.moveXY(dx, dy);
      }

      public VoronoiOfQuad.Polygon rotateAngleOfCenter(float angle360) {
         if (angle360 == 0.0F) {
            return this;
         } else {
            for (VoronoiOfQuad.Vec2f vec : this.list) {
               float dx = vec.x - this.center.x;
               float dy = vec.y - this.center.y;
               float radianAngle = MathHelper.toRadians((float)Math.toDegrees(Math.atan2((double)dy, (double)dx)) + angle360 - 90.0F);
               float dst = (float)Math.sqrt((double)(dx * dx + dy * dy));
               vec.x = this.center.x - MathHelper.sin(radianAngle) * dst;
               vec.y = this.center.y + MathHelper.cos(radianAngle) * dst;
            }

            return this;
         }
      }

      public VoronoiOfQuad.Polygon translateToPosLoc(float x, float y, float offset) {
         float dx = this.center.x - x;
         float dy = this.center.y - y;
         float angleRadian = (float)(Math.atan2((double)dy, (double)dx) + (double)MathHelper.toRadians(90.0F));
         return this.moveXY(-MathHelper.sin(angleRadian) * offset, MathHelper.cos(angleRadian) * offset);
      }

      public float distanceToAtCenter(float x, float y) {
         float dx = this.center.x - x;
         float dy = this.center.y - y;
         return (float)Math.sqrt((double)(dx * dx + dy * dy));
      }

      public VoronoiOfQuad.Polygon translateAwayPosLoc(float x, float y, float offset) {
         return this.translateToPosLoc(x, y, -offset);
      }

      public VoronoiOfQuad.Polygon rotateAtXOfAngleAwayPos(float x, float y, float mul) {
         float dx = this.center.x - x;
         float dy = this.center.y - y;
         float angle = dy * mul * (float)(dx > 0.0F ? 1 : -1);
         return this.rotateAngleOfCenter(angle);
      }

      public VoronoiOfQuad.Polygon rotateAtYOfAngleAwayPos(float x, float y, float mul) {
         float dx = this.center.x - x;
         float dy = this.center.y - y;
         float angle = dx * mul * (float)(dy < 0.0F ? 1 : -1);
         return this.rotateAngleOfCenter(angle);
      }
   }

   public static class Vec2f {
      public float x;
      public float y;

      public Vec2f(float x, float y) {
         this.x = x;
         this.y = y;
      }

      public float[] getUVTexXY(float x, float y, float x2, float y2) {
         return new float[]{this.getUVTexX(x, x2), this.getUVTexY(y, y2)};
      }

      public float getUVTexX(float x, float x2) {
         return (x2 - this.x) / (x2 - x);
      }

      public float getUVTexY(float y, float y2) {
         return 1.0F - (y2 - this.y) / (y2 - y);
      }

      public VoronoiOfQuad.Vec2f add(float xAdd, float yAdd) {
         return new VoronoiOfQuad.Vec2f(this.x + xAdd, this.y + yAdd);
      }

      public VoronoiOfQuad.Vec2f scale(float scale) {
         return new VoronoiOfQuad.Vec2f(this.x * scale, this.y * scale);
      }
   }
}
