package ru.govno.client.module.modules;

import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import org.lwjgl.opengl.GL11;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class Overlay extends Module {
   public static Overlay get;
   BoolSettings customHotbar;

   public Overlay() {
      super("Overlay", 0, Module.Category.RENDER);
      this.settings.add(this.customHotbar = new BoolSettings("Hotbar rework", true, this));
      get = this;
   }

   @Override
   public void alwaysRender2D(ScaledResolution sr) {
      this.stateAnim.to = this.isActived() ? 1.0F : 0.0F;
      this.stateAnim.getAnim();
      if (MathUtils.getDifferenceOf(this.stateAnim.anim, this.stateAnim.to) < 0.03F) {
         this.stateAnim.setAnim(this.stateAnim.to);
      }
   }

   public void onHotbarRender(ScaledResolution sr, Runnable renderDefault) {
      if (this.customHotbar.canBeRender()) {
         float anim = this.customHotbar.getAnimation() * this.stateAnim.anim;
         float defaultApc = Math.max(1.0F - anim * 2.0F, 0.0F);
         float customApc = Math.min((anim - 0.5F) * 2.0F, 1.0F);
         if (defaultApc != 0.0F) {
            GL11.glPushMatrix();
            RenderUtils.customRotatedObject2D(0.0F, (float)sr.getScaledHeight(), (float)sr.getScaledWidth(), 0.0F, (double)defaultApc);
            renderDefault.run();
            GL11.glPopMatrix();
         } else if (customApc != 1.0F) {
            GL11.glPushMatrix();
            RenderUtils.customRotatedObject2D(0.0F, (float)sr.getScaledHeight(), (float)sr.getScaledWidth(), 0.0F, (double)customApc);
            this.renderCustomHotbar(sr);
            GL11.glPopMatrix();
         } else {
            this.renderCustomHotbar(sr);
         }
      } else {
         renderDefault.run();
      }
   }

   private void renderCustomHotbar(ScaledResolution sr) {
      int pixScale = 16;
      int xPadding = 3;
      int yOffset = 6;
      int yPadding = 2;
      int centerItemsUpPix = 6;
      int slot = Minecraft.player.inventory.currentItem;
      List<ItemStack> stacks = IntStream.range(0, 9).mapToObj(IInt -> Minecraft.player.inventory.getStackInSlot(IInt)).toList();
      float x1 = (float)sr.getScaledWidth() / 2.0F - (float)(stacks.size() * pixScale + (stacks.size() - 1) * xPadding) / 2.0F;
      float x = x1;
      float y1 = (float)(sr.getScaledHeight() - pixScale - yOffset);

      for (int index = 0; index < stacks.size(); index++) {
         ItemStack stack = stacks.get(index);
         float centerPC = (float)MathUtils.easeInOutQuadWave((double)((float)index / (float)stacks.size()));
         boolean selected = slot == index;
         float y = y1 - (float)centerItemsUpPix * centerPC;
         this.drawItemStack(stack, x, y, (int)((float)pixScale * (selected ? 1.5F : 1.0F)), Fonts.neverlose500_13, selected);
         x += (float)pixScale;
         if (index != stacks.size() - 1) {
            x += (float)xPadding;
         }
      }
   }

   private void drawItemStack(ItemStack stack, float x, float y, int pixScale, CFontRenderer font, boolean selected) {
      float scale = (float)pixScale / 16.0F;
      if (!stack.isEmpty() && stack.getItem() != Items.air) {
         if (scale != 0.0F) {
            if (scale != 1.0F) {
               GL11.glPushMatrix();
               RenderUtils.customScaledObject2D(x, y, 16.0F, 16.0F, scale);
            }

            GL11.glTranslated((double)x, (double)y, 0.0);
            if (Minecraft.player.isSwingInProgress && selected) {
               GL11.glPushMatrix();
               RenderUtils.customRotatedObject2D(
                  0.0F, 0.0F, 16.0F, 16.0F, MathUtils.easeInOutQuadWave((double)(Minecraft.player.swingProgress + mc.getRenderPartialTicks() / 10.0F)) * 30.0
               );
            }

            if (Minecraft.player.getActiveHand() == EnumHand.MAIN_HAND && Minecraft.player.isHandActive() && selected) {
               GL11.glPushMatrix();
               float scaleBlob;
               if (Minecraft.player.getActiveItemStack() == null
                  || !(Minecraft.player.getActiveItemStack().getItem() instanceof ItemFood)
                     && !(Minecraft.player.getActiveItemStack().getItem() instanceof ItemPotion)) {
                  float usePC = Math.min(((float)Minecraft.player.getItemInUseMaxCount() + mc.getRenderPartialTicks()) / 8.0F, 1.0F);
                  scaleBlob = usePC * 0.3333F + 6666.0F;
               } else {
                  float usePC = Math.min(((float)Minecraft.player.getItemInUseMaxCount() + mc.getRenderPartialTicks()) / 32.0F, 1.0F);
                  scaleBlob = 1.0F + (float)MathUtils.easeInOutQuadWave((double)MathUtils.valWave01(MathUtils.valWave01(usePC))) * usePC / 3.0F;
               }

               RenderUtils.customScaledObject2D(0.0F, 0.0F, 16.0F, 16.0F, scaleBlob);
            }

            mc.getRenderItem().renderItemIntoGUI(stack, 0, 0);
            if (Minecraft.player.getActiveHand() == EnumHand.MAIN_HAND && Minecraft.player.isHandActive() && selected) {
               GL11.glPopMatrix();
            }

            if (Minecraft.player.isSwingInProgress && selected) {
               GL11.glPopMatrix();
            }

            mc.getRenderItem().renderItemOverlays(font, stack, 0, 0);
            GL11.glTranslated((double)(-x), (double)(-y), 0.0);
            if (scale != 1.0F) {
               GL11.glPopMatrix();
            }
         }
      }
   }
}
