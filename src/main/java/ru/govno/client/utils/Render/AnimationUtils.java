package ru.govno.client.utils.Render;

import optifine.Config;
import ru.govno.client.utils.Math.MathUtils;

public class AnimationUtils {
   private static float msCapDelay = 16.666666F;
   public long mc;
   public float anim;
   public float to;
   public float speed;

   public static void staticSetFpsCap(int itPS) {
      msCapDelay = 1000.0F / (float)itPS;
   }

   public float getAnimAndSetupInfinitySpeed() {
      float pre = msCapDelay;
      msCapDelay = 0.0F;
      float anim = this.getAnim();
      msCapDelay = pre;
      return anim;
   }

   public AnimationUtils(float anim, float to, float speed) {
      this.anim = anim;
      this.to = to;
      this.speed = speed;
      this.mc = System.nanoTime();
   }

   public float getAnim() {
      if (this.to == this.anim) {
         this.mc = System.nanoTime();
         return this.anim;
      } else if (Math.abs(this.to - this.anim) < 1.0E-4F) {
         this.setAnim(this.to);
         return this.anim;
      } else {
         float msFinished = (float)(System.nanoTime() - this.mc) / 1000000.0F;
         if (msFinished < 1.0F) {
            return this.anim;
         } else {
            if (msFinished >= msCapDelay) {
               this.anim = MathUtils.lerp(this.anim, this.to, Math.min(this.speed * msFinished * 0.125F, 1.0F));
               this.mc = System.nanoTime();
            }

            return this.anim;
         }
      }
   }

   public float getAngleAnim() {
      if (Math.abs(this.to - this.anim) < 1.0E-4F) {
         this.setAnim(this.to);
         return this.anim;
      } else {
         float msFinished = (float)(System.nanoTime() - this.mc) / 1000000.0F;
         if (msFinished >= 1000.0F / (float)Config.getDesktopDisplayMode().getFrequency()) {
            this.anim = (float)this.lerpAngle(this.anim, this.to, Math.min(this.speed * msFinished * 0.125F, 1.0F));
            this.mc = System.nanoTime();
         }

         return MathUtils.wrapAngleTo180_float(this.anim);
      }
   }

   public void setAnim(float anim) {
      this.anim = anim;
      this.mc = System.nanoTime();
   }

   double lerpAngle(float start, float end, float amount) {
      float minAngle = (end - start + 180.0F) % 360.0F - 180.0F;
      return (double)(minAngle * amount + start);
   }
}
