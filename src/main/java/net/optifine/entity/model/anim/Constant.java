package net.optifine.entity.model.anim;

public class Constant implements IExpression {
   private float value;

   public Constant(float value) {
      this.value = value;
   }

   @Override
   public float eval() {
      return this.value;
   }

   @Override
   public String toString() {
      return this.value + "";
   }
}
