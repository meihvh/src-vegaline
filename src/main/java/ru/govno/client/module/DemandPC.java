package ru.govno.client.module;

public class DemandPC {
   private final int level03FPS;
   private final int level03CPU;
   public static int valuesCount = 2;
   public static int maxDemandValue = 3;

   public DemandPC(int level03FPS, int level03CPU) {
      this.level03FPS = level03FPS;
      this.level03CPU = level03CPU;
   }

   public int getLevel03FPS() {
      return this.level03FPS;
   }

   public int getLevel03CPU() {
      return this.level03CPU;
   }
}
