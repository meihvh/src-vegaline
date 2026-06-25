package net.minecraft.util.math;

public class Vec2f {
   public float x;
   public float y;

   public Vec2f(float xIn, float yIn) {
      this.x = xIn;
      this.y = yIn;
   }

   public Vec2f add(float xOut, float yOut) {
      this.x += xOut;
      this.y += yOut;
      return this;
   }
}
