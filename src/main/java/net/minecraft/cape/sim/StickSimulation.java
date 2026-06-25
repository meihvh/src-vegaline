package net.minecraft.cape.sim;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.cape.util.Mth;
import ru.govno.client.utils.Render.AnimationUtils;

public class StickSimulation {
   public AnimationUtils moveSpeed = new AnimationUtils(0.0F, 0.0F, 0.015F);
   public List<StickSimulation.Point> points = new ArrayList<>();
   public List<StickSimulation.Stick> sticks = new ArrayList<>();
   public float gravity = 100.0F;
   public int numIterations = 10;
   private final float maxBend = 5.0F;

   public void simulate() {
      float deltaTime = 0.05F;
      StickSimulation.Vector2 down = new StickSimulation.Vector2(0.0F, this.gravity * deltaTime);
      StickSimulation.Vector2 tmp = new StickSimulation.Vector2(0.0F, 0.0F);

      for (StickSimulation.Point p : this.points) {
         if (!p.locked) {
            tmp.copy(p.position);
            p.prevPosition.copy(tmp);
            p.position.subtract(down);
         }
      }

      StickSimulation.Point basePoint = this.points.get(0);

      for (StickSimulation.Point px : this.points) {
         if (px != basePoint && px.position.x - basePoint.position.x > 0.0F) {
            px.position.x = basePoint.position.x - 0.2F;
         }
      }

      for (int i = this.points.size() - 2; i >= 1; i--) {
         double angle = this.getAngle(this.points.get(i).position, this.points.get(i - 1).position, this.points.get(i + 1).position);
         angle *= 57.2958;
         if (angle > 360.0) {
            angle -= 360.0;
         }

         if (angle < -360.0) {
            angle += 360.0;
         }

         double abs = Math.abs(angle);
         if (abs < 175.0) {
            StickSimulation.Vector2 replacement = this.getReplacement(this.points.get(i).position, this.points.get(i - 1).position, angle, 176.0);
            this.points.get(i + 1).position = replacement;
         }

         if (abs > 185.0) {
            StickSimulation.Vector2 replacement = this.getReplacement(this.points.get(i).position, this.points.get(i - 1).position, angle, 184.0);
            this.points.get(i + 1).position = replacement;
         }
      }

      for (int i = 0; i < this.numIterations; i++) {
         for (int x = this.sticks.size() - 1; x >= 0; x--) {
            StickSimulation.Stick stick = this.sticks.get(x);
            StickSimulation.Vector2 stickCentre = stick.pointA.position.clone().add(stick.pointB.position).div(2.0F);
            StickSimulation.Vector2 stickDir = stick.pointA.position.clone().subtract(stick.pointB.position).normalize();
            if (!stick.pointA.locked) {
               stick.pointA.position = stickCentre.clone().add(stickDir.clone().mul(stick.length / 2.0F));
            }

            if (!stick.pointB.locked) {
               stick.pointB.position = stickCentre.clone().subtract(stickDir.clone().mul(stick.length / 2.0F));
            }
         }
      }

      for (int x = 0; x < this.sticks.size(); x++) {
         StickSimulation.Stick stickx = this.sticks.get(x);
         StickSimulation.Vector2 stickDirx = stickx.pointA.position.clone().subtract(stickx.pointB.position).normalize();
         if (!stickx.pointB.locked) {
            stickx.pointB.position = stickx.pointA.position.clone().subtract(stickDirx.mul(stickx.length));
         }
      }
   }

   private StickSimulation.Vector2 getReplacement(StickSimulation.Vector2 middle, StickSimulation.Vector2 prev, double angle, double target) {
      double theta = target / 57.2958;
      float x = prev.x - middle.x;
      float y = prev.y - middle.y;
      if (angle < 0.0) {
         theta *= -1.0;
      }

      double cs = Math.cos(theta);
      double sn = Math.sin(theta);
      return new StickSimulation.Vector2(
         (float)((double)x * cs - (double)y * sn + (double)middle.x), (float)((double)x * sn + (double)y * cs + (double)middle.y)
      );
   }

   private double getAngle(StickSimulation.Vector2 middle, StickSimulation.Vector2 prev, StickSimulation.Vector2 next) {
      return Math.atan2((double)(next.y - middle.y), (double)(next.x - middle.x)) - Math.atan2((double)(prev.y - middle.y), (double)(prev.x - middle.x));
   }

   public static class Point {
      public StickSimulation.Vector2 position = new StickSimulation.Vector2(0.0F, 0.0F);
      public StickSimulation.Vector2 prevPosition = new StickSimulation.Vector2(0.0F, 0.0F);
      public boolean locked;

      public float getLerpX(float delta) {
         return Mth.lerp(delta, this.prevPosition.x, this.position.x);
      }

      public float getLerpY(float delta) {
         return Mth.lerp(delta, this.prevPosition.y, this.position.y);
      }
   }

   public static class Stick {
      public StickSimulation.Point pointA;
      public StickSimulation.Point pointB;
      public float length;

      public Stick(StickSimulation.Point pointA, StickSimulation.Point pointB, float length) {
         this.pointA = pointA;
         this.pointB = pointB;
         this.length = length;
      }
   }

   public static class Vector2 {
      public float x;
      public float y;

      public Vector2(float x, float y) {
         this.x = x;
         this.y = y;
      }

      public StickSimulation.Vector2 clone() {
         return new StickSimulation.Vector2(this.x, this.y);
      }

      public void copy(StickSimulation.Vector2 vec) {
         this.x = vec.x;
         this.y = vec.y;
      }

      public StickSimulation.Vector2 add(StickSimulation.Vector2 vec) {
         this.x = this.x + vec.x;
         this.y = this.y + vec.y;
         return this;
      }

      public StickSimulation.Vector2 subtract(StickSimulation.Vector2 vec) {
         this.x = this.x - vec.x;
         this.y = this.y - vec.y;
         return this;
      }

      public StickSimulation.Vector2 div(float amount) {
         this.x /= amount;
         this.y /= amount;
         return this;
      }

      public StickSimulation.Vector2 mul(float amount) {
         this.x *= amount;
         this.y *= amount;
         return this;
      }

      public StickSimulation.Vector2 normalize() {
         float f = (float)Math.sqrt((double)(this.x * this.x + this.y * this.y));
         if (f < 1.0E-4F) {
            this.x = 0.0F;
            this.y = 0.0F;
         } else {
            this.x /= f;
            this.y /= f;
         }

         return this;
      }

      @Override
      public String toString() {
         return "Vector2 [x=" + this.x + ", y=" + this.y + "]";
      }
   }
}
