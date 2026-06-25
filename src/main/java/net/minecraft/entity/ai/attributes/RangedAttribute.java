package net.minecraft.entity.ai.attributes;

import javax.annotation.Nullable;
import net.minecraft.util.math.MathHelper;

public class RangedAttribute extends BaseAttribute {
   private final double minimumValue;
   private final double maximumValue;
   private String description;

   public RangedAttribute(@Nullable IAttribute parentIn, String unlocalizedNameIn, double defaultValue, double minimumValueIn, double maximumValueIn) {
      super(parentIn, unlocalizedNameIn, defaultValue);
      this.minimumValue = minimumValueIn;
      this.maximumValue = maximumValueIn;
   }

   public RangedAttribute setDescription(String descriptionIn) {
      this.description = descriptionIn;
      return this;
   }

   public String getDescription() {
      return this.description;
   }

   @Override
   public double clampValue(double value) {
      return MathHelper.clamp(value, this.minimumValue, this.maximumValue);
   }
}
