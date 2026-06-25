package ru.govno.client.utils.Voronoi;

import java.util.function.Supplier;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;

public class UVoronoiDispatchElementAnimator {
   private VoronoiOfCopyRenderTemp voronoiOfCopyRenderTemp;
   private Supplier<Boolean> conditionTrigger;
   private boolean wasConditionSwapSetTrue;
   private boolean ruleIfNoAnimation;
   private boolean onlyCapture;
   private boolean genThreadForAnimation;
   private boolean animVLAA;
   private Runnable renderIn;
   private long durationAnim;
   private float mulTransAnim;
   private float mulRotAnim;
   private int countPointsSplits;
   private int tryCaptureLayers;
   private int tryDrawsLayers;
   private final TimerHelper timerAnimation = TimerHelper.TimerHelperReseted();

   private UVoronoiDispatchElementAnimator() {
   }

   public static UVoronoiDispatchElementAnimator build() {
      return new UVoronoiDispatchElementAnimator();
   }

   public UVoronoiDispatchElementAnimator setDurationAnim(long duration) {
      this.durationAnim = duration;
      return this;
   }

   public UVoronoiDispatchElementAnimator setAnimationParameters(
      float mulTransAnim, float mulRotAnim, int countPointsSplits, boolean genThreadForAnimation, int tryCaptureLayers, int tryDrawsLayers, boolean animVLAA
   ) {
      this.mulTransAnim = mulTransAnim;
      this.mulRotAnim = mulRotAnim;
      this.countPointsSplits = countPointsSplits;
      this.genThreadForAnimation = genThreadForAnimation;
      this.tryCaptureLayers = tryCaptureLayers;
      this.tryDrawsLayers = tryDrawsLayers;
      this.animVLAA = animVLAA;
      return this;
   }

   public UVoronoiDispatchElementAnimator setupTriggerDynamicCondition(boolean conditionTrigger, boolean ruleIfNoAnimation) {
      this.ruleIfNoAnimation = ruleIfNoAnimation && !conditionTrigger;
      if (this.ruleIfNoAnimation) {
         this.conditionTrigger = () -> false;
         this.wasConditionSwapSetTrue = false;
         return this;
      } else {
         conditionTrigger = !conditionTrigger;
         if (this.conditionTrigger == null || this.conditionTrigger.get() != conditionTrigger) {
             boolean finalConditionTrigger = conditionTrigger;
             this.conditionTrigger = () -> finalConditionTrigger;
            this.wasConditionSwapSetTrue = !conditionTrigger;
         }

         return this;
      }
   }

   public UVoronoiDispatchElementAnimator insertRenderForEffect(Runnable renderIn, boolean onlyCapture) {
      this.onlyCapture = onlyCapture;
      this.renderIn = renderIn;
      return this;
   }

   public void renderVoronoiEffect(float originalObjX, float originalObjY, float originalObjX2, float originalObjY2, int fillColor, float aPC) {
      if (this.renderIn != null) {
         if (!this.ruleIfNoAnimation && !this.conditionTrigger.get()) {
            if (!this.onlyCapture && this.conditionTrigger != null && this.conditionTrigger.get()) {
               this.renderIn.run();
            }

            if (this.wasConditionSwapSetTrue) {
               this.wasConditionSwapSetTrue = false;
               this.voronoiOfCopyRenderTemp = new VoronoiOfCopyRenderTemp(originalObjX, originalObjY, originalObjX2, originalObjY2, 0, false);
               this.voronoiOfCopyRenderTemp
                  .genFromCaptureRender2d(
                     originalObjX,
                     originalObjY,
                     originalObjX2,
                     originalObjY2,
                     this.renderIn,
                     this.countPointsSplits,
                     this.genThreadForAnimation,
                     this.tryCaptureLayers
                  );
               this.timerAnimation.reset();
            }

            if (!(originalObjX2 <= originalObjX)
               && !(originalObjY2 <= originalObjY)
               && !this.timerAnimation.hasReached((double)this.durationAnim)
               && this.voronoiOfCopyRenderTemp != null) {
               float animTimeDelta = (float)this.timerAnimation.getTime() / (float)this.durationAnim;
               float trans = (float)MathUtils.easeOutCubic((double)animTimeDelta) * this.mulTransAnim;
               float rot = (float)MathUtils.easeInCircle((double)animTimeDelta) * this.mulRotAnim / 360.0F;
               float apc = aPC * (1.0F - (float)MathUtils.easeOutCubic((double)animTimeDelta));
               this.voronoiOfCopyRenderTemp
                  .setupVLAARender(this.animVLAA, false)
                  .renderCapturedSegments2d(true, 9, trans, rot, apc, fillColor, this.tryDrawsLayers);
               if (apc > 0.25F) {
                  apc = (apc - 0.25F) / 0.75F;
                  GL11.glLineWidth((0.125F + apc * 1.5F) * ScaledResolution.lpSCFactor());
                  this.voronoiOfCopyRenderTemp
                     .setupVLAARender(this.animVLAA, false)
                     .renderCapturedSegments2d(true, 2, trans, rot, apc, fillColor, this.tryDrawsLayers + 1);
                  GL11.glLineWidth(1.0F);
               }
            } else {
               this.voronoiOfCopyRenderTemp = null;
            }
         } else {
            this.renderIn.run();
            this.voronoiOfCopyRenderTemp = null;
         }
      }
   }
}
