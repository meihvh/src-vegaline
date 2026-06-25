package ru.govno.client.event.events;

import net.minecraft.util.EnumHandSide;
import ru.govno.client.event.Event;

public class EventTransformSideFirstPerson extends Event {
   private final EnumHandSide enumHandSide;
   private final boolean isActiveHand;

   public EventTransformSideFirstPerson(EnumHandSide enumHandSide, boolean isActiveHand) {
      this.enumHandSide = enumHandSide;
      this.isActiveHand = isActiveHand;
   }

   public EnumHandSide getEnumHandSide() {
      return this.enumHandSide;
   }

   public boolean isActiveHand() {
      return this.isActiveHand;
   }
}
