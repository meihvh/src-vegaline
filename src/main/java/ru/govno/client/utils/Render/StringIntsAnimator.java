package ru.govno.client.utils.Render;

import java.util.ArrayList;
import java.util.List;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.utils.Math.MathUtils;

public class StringIntsAnimator {
   private final List<AnimationUtils> animations = new ArrayList<>();

   public void reset() {
      this.animations.clear();
   }

   public void controlAnimations(int numericToString, float animationSpeed) {
      String numericString = Integer.toString(numericToString);
      int lengthSet = numericString.length();
      int[] alphabetNums = new int[lengthSet];

      for (int i = 0; i < numericString.length(); i++) {
         char theChar = numericString.toCharArray()[i];

         try {
            alphabetNums[i] = Integer.parseInt(String.valueOf(theChar));
         } catch (Exception var9) {
            var9.printStackTrace();
         }
      }

      while (this.animations.size() > lengthSet) {
         this.animations.remove(this.animations.size() - 1);
      }

      for (int index = 0; index < lengthSet; index++) {
         int value = alphabetNums[index];
         if (this.animations.size() < lengthSet) {
            this.animations.add(new AnimationUtils((float)value, (float)value, animationSpeed));
         }

         AnimationUtils currentAnimation = this.animations.get(index);
         if (currentAnimation != null) {
            currentAnimation.speed = Math.min(
               animationSpeed * (1.0F + Math.max(MathUtils.getDifferenceOf(currentAnimation.anim, currentAnimation.to) - 1.0F, 1.0F)), 1.0F
            );
            currentAnimation.to = (float)value;
         }
      }
   }

   public void drawString(CFontRenderer font, float x, float y, int color, boolean centeredX, float numericPaddingPix) {
      if (centeredX) {
         for (AnimationUtils animation : this.animations) {
            x -= font.getCharWidth(String.valueOf((int)animation.to).toCharArray()[0]) / 2.0F;
         }
      }

      float stepY = font.getHeight() + numericPaddingPix;
      float textX = x;

      for (AnimationUtils animation : this.animations) {
         int numberCurrent = (int)animation.to;
         float numberCurrentAnim = animation.getAnim();
         if (MathUtils.getDifferenceOf(numberCurrentAnim, (float)numberCurrent) < 0.1F) {
            animation.setAnim((float)numberCurrent);
            numberCurrentAnim = (float)numberCurrent;
         }

         for (int numberOffset = 0; numberOffset < 10; numberOffset++) {
            float diff = MathUtils.getDifferenceOf(numberCurrentAnim, (float)numberOffset);
            if (!(diff > 0.99F)) {
               int alphaSet = (int)((1.0F - diff) * (1.0F - diff) * (float)ColorUtils.getAlphaFromColor(color));
               float textY = y - stepY * (numberCurrentAnim - (float)numberOffset);
               if ((float)alphaSet >= 33.0F) {
                  font.drawString(String.valueOf(numberOffset), textX, textY, ColorUtils.swapAlpha(color, (float)alphaSet));
               }
            }
         }

         float diff = MathUtils.getDifferenceOf(numberCurrentAnim, (float)numberCurrent);
         textX += MathUtils.lerp(
            font.getCharWidth(String.valueOf((int)animation.to).toCharArray()[0]),
            font.getCharWidth(String.valueOf((int)animation.anim).toCharArray()[0]),
            Math.min(diff, 1.0F)
         );
      }
   }
}
