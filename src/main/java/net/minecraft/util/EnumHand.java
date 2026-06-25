package net.minecraft.util;

public enum EnumHand {
   MAIN_HAND,
   OFF_HAND;

   public static EnumHand getOpposite(EnumHand hand) {
      return hand == MAIN_HAND ? OFF_HAND : MAIN_HAND;
   }
}
