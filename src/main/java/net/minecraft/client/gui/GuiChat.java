package net.minecraft.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ITabCompleter;
import net.minecraft.util.TabCompleter;
import net.minecraft.util.Vec2f;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.module.modules.ChatHelper;
import ru.govno.client.module.modules.ClientColors;
import ru.govno.client.module.modules.ClientTune;
import ru.govno.client.module.modules.ComfortUi;
import ru.govno.client.module.modules.CommandGui;
import ru.govno.client.module.modules.FriendsSLink;
import ru.govno.client.module.modules.Hud;
import ru.govno.client.module.modules.MiniMap;
import ru.govno.client.module.modules.RadioPlayer;
import ru.govno.client.module.modules.TargetHUD;
import ru.govno.client.module.modules.Timer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.HoverUtils;
import ru.govno.client.utils.InventoryUtil;
import ru.govno.client.utils.PaintUI;
import ru.govno.client.utils.Command.impl.Clip;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Render.StencilUtil;

public class GuiChat extends GuiScreen implements ITabCompleter {
   public static final PaintUI paintUI = new PaintUI();
   private final AnimationUtils alphaPC = new AnimationUtils(0.0F, 1.0F, 0.0825F);
   private String historyBuffer = "";
   boolean dragging = false;
   boolean dragging2 = false;
   boolean dragging3 = false;
   boolean dragging4 = false;
   boolean dragging5 = false;
   boolean dragging6 = false;
   boolean dragging7 = false;
   boolean dragging8 = false;
   boolean dragging9 = false;
   boolean dragging10 = false;
   boolean dragging11 = false;
   public boolean[] dragging12 = new boolean[2];
   boolean dragging13 = false;
   boolean dragging14 = false;
   int dragX;
   int dragY;
   int dragX2;
   int dragY2;
   int dragX3;
   int dragY3;
   int dragX4;
   int dragY4;
   int dragX5;
   int dragY5;
   int dragX6;
   int dragY6;
   int dragX7;
   int dragY7;
   int dragX8;
   int dragY8;
   int dragX9;
   int dragY9;
   int dragX10;
   int dragY10;
   int dragX11;
   int dragY11;
   int dragX12;
   int dragY12;
   int dragX13;
   int dragY13;
   int dragX14;
   int dragY14;
   float animSpeed = 0.1F;
   AnimationUtils dragAnim = new AnimationUtils(0.0F, 0.0F, this.animSpeed);
   AnimationUtils dragAnim2 = new AnimationUtils(0.0F, 0.0F, this.animSpeed);
   AnimationUtils dragAnim3 = new AnimationUtils(0.0F, 0.0F, this.animSpeed);
   AnimationUtils dragAnim4 = new AnimationUtils(0.0F, 0.0F, this.animSpeed);
   AnimationUtils dragAnim5 = new AnimationUtils(0.0F, 0.0F, this.animSpeed);
   AnimationUtils dragAnim6 = new AnimationUtils(0.0F, 0.0F, this.animSpeed);
   AnimationUtils dragAnim7 = new AnimationUtils(0.0F, 0.0F, this.animSpeed);
   AnimationUtils dragAnim8 = new AnimationUtils(0.0F, 0.0F, this.animSpeed);
   AnimationUtils dragAnim9 = new AnimationUtils(0.0F, 0.0F, this.animSpeed);
   AnimationUtils dragAnim10 = new AnimationUtils(0.0F, 0.0F, this.animSpeed);
   AnimationUtils dragAnim11 = new AnimationUtils(0.0F, 0.0F, this.animSpeed);
   AnimationUtils dragAnim12 = new AnimationUtils(0.0F, 0.0F, this.animSpeed);
   AnimationUtils dragAnim13 = new AnimationUtils(0.0F, 0.0F, this.animSpeed);
   AnimationUtils dragAnim14 = new AnimationUtils(0.0F, 0.0F, this.animSpeed);
   private int sentHistoryCursor = -1;
   private TabCompleter tabCompleter;
   protected GuiTextField inputField;
   private String defaultInputFieldText = "";
   float textwidth;
   float textwidthSmooth;
   static boolean keyHide;
   boolean clickHider;
   float x = TargetHUD.xPosHud;
   float y = TargetHUD.yPosHud;
   float x2 = Hud.wmPosX;
   float y2 = Hud.wmPosY;
   float x4 = Hud.potPosX;
   float y4 = Hud.potPosY;
   float x5 = Hud.armPosX;
   float y5 = Hud.armPosY;
   float x6 = Hud.stPosX;
   float y6 = Hud.stPosY;
   float x7 = Hud.listPosX;
   float y7 = Hud.listPosY;
   float x8 = CommandGui.getWindowCoord()[0];
   float y8 = CommandGui.getWindowCoord()[1];
   float x9 = 0.0F;
   float y9 = 0.0F;
   float x10 = Hud.kbPosX;
   float y10 = Hud.kbPosY;
   float x11 = Hud.pcPosX;
   float y11 = Hud.pcPosY;
   float x12 = 0.0F;
   float y12 = 0.0F;
   float x13 = FriendsSLink.flPosX;
   float y13 = FriendsSLink.flPosY;
   float x14 = RadioPlayer.getWindowCoord()[0];
   float y14 = RadioPlayer.getWindowCoord()[1];
   static int scrollInt = 0;
   static AnimationUtils scroll = new AnimationUtils(0.0F, 0.0F, 0.1F);
   static boolean click = true;

   public GuiChat() {
   }

   public GuiChat(String defaultText) {
      this.defaultInputFieldText = defaultText;
   }

   public float lerp(float start, float end, float step) {
      return start + step * (end - start);
   }

   @Override
   protected void mouseReleased(int mouseX, int mouseY, int state) {
      if (state == 0) {
         this.dragging = false;
         this.dragging2 = false;
         this.dragging3 = false;
         this.dragging4 = false;
         this.dragging5 = false;
         this.dragging6 = false;
         this.dragging7 = false;
         this.dragging8 = false;
         this.dragging9 = false;
         this.dragging10 = false;
         this.dragging11 = false;
         this.dragging12[0] = false;
         this.dragging12[1] = false;
         this.dragging13 = false;
         this.dragAnim.to = 0.0F;
         this.dragAnim2.to = 0.0F;
         this.dragAnim3.to = 0.0F;
         this.dragAnim4.to = 0.0F;
         this.dragAnim5.to = 0.0F;
         this.dragAnim6.to = 0.0F;
         this.dragAnim7.to = 0.0F;
         this.dragAnim8.to = 0.0F;
         this.dragAnim9.to = 0.0F;
         this.dragAnim10.to = 0.0F;
         this.dragAnim11.to = 0.0F;
         this.dragAnim12.to = 0.0F;
         this.dragAnim13.to = 0.0F;
      }

      if (ComfortUi.get.isPaintInChat()) {
         paintUI.mouseReleased(mouseX, mouseY, state);
      }

      super.mouseReleased(mouseX, mouseY, state);
   }

   @Override
   public void initGui() {
      if (ComfortUi.get.isPaintInChat()) {
         paintUI.onCloseOrInit(true);
      }

      this.textwidth = 10.0F;
      Keyboard.enableRepeatEvents(true);
      this.sentHistoryCursor = this.mc.ingameGUI.getChatGUI().getSentMessages().size();
      this.inputField = new GuiTextField(0, this.fontRendererObj, 4, height - 12, width - 4, 12);
      this.inputField.setMaxStringLength(4086);
      this.inputField.setEnableBackgroundDrawing(false);
      this.inputField.setFocused(true);
      this.inputField.setText(this.defaultInputFieldText);
      this.inputField.setCanLoseFocus(false);
      this.tabCompleter = new GuiChat.ChatTabCompleter(this.inputField);
      this.alphaPC.setAnim(0.0F);
      super.initGui();
   }

   @Override
   public void onGuiClosed() {
      if (ComfortUi.get.isPaintInChat()) {
         paintUI.onCloseOrInit(true);
      }

      Keyboard.enableRepeatEvents(false);
      this.mc.ingameGUI.getChatGUI().resetScroll();
   }

   @Override
   public void updateScreen() {
      this.inputField.updateCursorCounter();
   }

   @Override
   protected void keyTyped(char typedChar, int keyCode) throws IOException {
      this.tabCompleter.resetRequested();
      if (keyCode == 15) {
         this.tabCompleter.complete();
      } else {
         this.tabCompleter.resetDidComplete();
      }

      if (keyCode == 1) {
         this.mc.displayGuiScreen(null);
      } else if (keyCode == 28 || keyCode == 156) {
         String s = this.inputField.getText().trim();
         s = ChatHelper.fixGrammarInputChatString(s);
         if (!s.isEmpty()) {
            this.sendChatMessage(s);
         }

         this.mc.displayGuiScreen(null);
      } else if (keyCode == 200) {
         this.getSentHistory(-1);
      } else if (keyCode == 208) {
         this.getSentHistory(1);
      } else if (keyCode == 201) {
         this.mc.ingameGUI.getChatGUI().scroll(this.mc.ingameGUI.getChatGUI().getLineCount() - 1);
      } else if (keyCode == 209) {
         this.mc.ingameGUI.getChatGUI().scroll(-this.mc.ingameGUI.getChatGUI().getLineCount() + 1);
      } else {
         this.inputField.textboxKeyTyped(typedChar, keyCode);
      }
   }

   @Override
   public void handleMouseInput() throws IOException {
      super.handleMouseInput();
      int i = Mouse.getEventDWheel();
      if (i != 0) {
         if (!this.hoveredOnClip()) {
            CommandGui.callWhell(i < 0);
            RadioPlayer.callWhell(i < 0);
         }

         if (i > 1) {
            i = 1;
         }

         if (i < -1) {
            i = -1;
         }

         if (!isShiftKeyDown()) {
            i *= 7;
         }

         if (!this.hoveredOnClip() && !CommandGui.isHoveredToPanel(false) && !RadioPlayer.isHoveredToPanel(false)) {
            this.mc.ingameGUI.getChatGUI().scroll(i);
         }
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
      CommandGui.callClick(mouseX, mouseY, mouseButton);
      RadioPlayer.callClick(mouseX, mouseY, mouseButton);
      if (ComfortUi.get.isPaintInChat()) {
         paintUI.mouseClicked(mouseX, mouseY, mouseButton);
      }

      if (mouseButton == 0) {
         ITextComponent itextcomponent = this.mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());
         if (itextcomponent != null && this.handleComponentClick(itextcomponent)) {
            return;
         }
      }

      this.inputField.mouseClicked(mouseX, mouseY, mouseButton);
      super.mouseClicked(mouseX, mouseY, mouseButton);
   }

   @Override
   protected void setText(String newChatText, boolean shouldOverwrite) {
      if (shouldOverwrite) {
         this.inputField.setText(newChatText);
      } else {
         this.inputField.writeText(newChatText);
      }
   }

   public void getSentHistory(int msgPos) {
      int i = this.sentHistoryCursor + msgPos;
      int j = this.mc.ingameGUI.getChatGUI().getSentMessages().size();
      i = MathHelper.clamp(i, 0, j);
      if (i != this.sentHistoryCursor) {
         if (i == j) {
            this.sentHistoryCursor = j;
            this.inputField.setText(this.historyBuffer);
         } else {
            if (this.sentHistoryCursor == j) {
               this.historyBuffer = this.inputField.getText();
            }

            this.inputField.setText(this.mc.ingameGUI.getChatGUI().getSentMessages().get(i));
            this.sentHistoryCursor = i;
         }
      }
   }

   boolean hoveredOnClip() {
      ScaledResolution scaled = new ScaledResolution(this.mc);
      int yPort = -(MathUtils.getDifferenceOf(scrollInt, 0) < 10 ? scrollInt : (scrollInt > 0 ? 10 : -10) + (scrollInt - (scrollInt > 0 ? 10 : -10)) * 10);
      float x = (float)(scaled.getScaledWidth() / 2 + 70);
      float h = 60.0F;
      float w = MathUtils.clamp(12.0F + Fonts.mntsb_12.getStringWidth(yPort + ""), 23.0F, 100.0F);
      float y = (float)(scaled.getScaledHeight() / 2) - 30.0F;
      return ComfortUi.get.isClipHelperInChat()
         && HoverUtils.isHovered((int)x, (int)y - 10, (int)(x + w), (int)(y + 60.0F) + 10, GuiScreen.staticMouseX, GuiScreen.staticMouseY);
   }

   void cliper(ScaledResolution scaled, int mouseX, int mouseY) {
      if (this.hoveredOnClip()) {
         float dyScroll;
         scrollInt = (int)((float)scrollInt + (dyScroll = (float)MathUtils.clamp(Mouse.getDWheel(), -1, 1)));
         if (Math.abs(dyScroll) > 0.5F) {
            ClientTune.get.playGuiScreenScrollSong();
         }
      }

      scrollInt = MathUtils.clamp(scrollInt, -5000, 5000);
      int yPort = -(MathUtils.getDifferenceOf(scrollInt, 0) < 10 ? scrollInt : (scrollInt > 0 ? 10 : -10) + (scrollInt - (scrollInt > 0 ? 10 : -10)) * 10);
      float x = (float)(scaled.getScaledWidth() / 2 + 70);
      float h = 60.0F;
      float w = MathUtils.clamp(12.0F + Fonts.mntsb_12.getStringWidth(yPort + ""), 23.0F, 100.0F);
      float y = (float)(scaled.getScaledHeight() / 2) - 30.0F;
      scroll.to = (float)(scrollInt * 7);
      RenderUtils.drawRect((double)x, (double)y, (double)(x + w), (double)(y + 60.0F), ColorUtils.getColor(0, 0, 0, 100));
      RenderUtils.drawRect((double)x, (double)(y - 10.0F), (double)(x + w), (double)y, ColorUtils.getColor(0, 180));
      Fonts.comfortaaRegular_13
         .drawStringWithShadow(
            "clip",
            x + w / 2.0F - Fonts.comfortaaRegular_13.getStringWidth("clip") / 2.0F,
            y - 7.0F,
            ColorUtils.getOverallColorFrom(ColorUtils.getFixedWhiteColor(), ClientColors.getColor1(), 0.2F)
         );
      RenderUtils.drawRect((double)x, (double)(y + 60.0F), (double)(x + w), (double)(y + 60.0F + 10.0F), ColorUtils.getColor(0, 180));
      Fonts.comfortaaRegular_13
         .drawStringWithShadow(
            "reset",
            x + w / 2.0F - Fonts.comfortaaRegular_13.getStringWidth("reset") / 2.0F,
            y + 60.0F + 3.0F,
            ColorUtils.getOverallColorFrom(ColorUtils.getFixedWhiteColor(), ClientColors.getColor2(), 0.2F)
         );
      RenderUtils.drawRoundedFullGradientShadow(
         x,
         y - 10.0F,
         x + w,
         y + 60.0F + 10.0F,
         2.0F,
         5.0F,
         ClientColors.getColor1(0, 0.15F),
         ClientColors.getColor1(0, 0.15F),
         ClientColors.getColor2(0, 0.15F),
         ClientColors.getColor2(0, 0.15F),
         true
      );
      RenderUtils.resetBlender();
      if (Mouse.isButtonDown(0)) {
         if (click && this.hoveredOnClip()) {
            if (HoverUtils.isHovered((int)x, (int)y - 10, (int)(x + w), (int)y, mouseX, mouseY)) {
               if (yPort != 0) {
                  Clip.runClip((double)yPort, 0.0, InventoryUtil.getElytra() != -1);
                  ClientTune.get.playUseMacros();
               }
            } else if (HoverUtils.isHovered((int)x, (int)(y + 60.0F), (int)(x + w), (int)(y + 60.0F + 10.0F), mouseX, mouseY)) {
               if (scrollInt != 0) {
                  ClientTune.get.playUseMacros();
               }

               scrollInt = 0;
            }
         }

         click = false;
      } else {
         click = true;
      }

      ArrayList<Vec2f> vecs = new ArrayList<>();
      vecs.add(new Vec2f(x + 0.5F, y + 30.0F - 2.0F));
      vecs.add(new Vec2f(x + 4.5F, y + 30.0F));
      vecs.add(new Vec2f(x + 0.5F, y + 30.0F + 2.0F));
      RenderUtils.drawSome(
         vecs,
         ColorUtils.fadeColor(
            ColorUtils.getOverallColorFrom(ClientColors.getColor1(0, 0.35F), ClientColors.getColor2(0, 0.35F)), ColorUtils.getColor(255, 100), 1.0F
         )
      );
      vecs.clear();
      vecs.add(new Vec2f(x, y + 30.0F - 2.0F));
      vecs.add(new Vec2f(x + 4.0F, y + 30.0F));
      vecs.add(new Vec2f(x, y + 30.0F + 2.0F));
      RenderUtils.drawSome(
         vecs,
         ColorUtils.fadeColor(
            ColorUtils.getOverallColorFrom(ClientColors.getColor1(0, 0.7F), ClientColors.getColor2(0, 0.7F)), ColorUtils.getColor(255, 170), 1.0F
         )
      );
      StencilUtil.initStencilToWrite();
      RenderUtils.drawRect((double)(x + 1.0F), (double)(y + 1.0F), (double)(x + w - 1.0F), (double)(y + 60.0F - 1.0F), ColorUtils.getColor(0, 0, 0, 100));
      StencilUtil.readStencilBuffer(1);

      for (int i = -5000; i < 5000; i++) {
         float sY = y + 30.0F - (float)(i * 7) + scroll.getAnim() - 3.0F;
         if (sY > y - 7.0F && sY < y + 60.0F + 7.0F) {
            int yP = MathUtils.getDifferenceOf(i, 0) < 10 ? i : (i > 0 ? 10 : -10) + (i - (i > 0 ? 10 : -10)) * 10;
            float alpha = (float)(
               (int)MathUtils.clamp(255.0 * (1.0 - MathUtils.getDifferenceOf((double)sY, (double)(y + 30.0F) - 2.5) / 36.363636F), 26.0, 255.0)
            );
            float yDiff = MathUtils.getDifferenceOf(sY + 3.0F, y + 30.0F);
            int c = ColorUtils.getOverallColorFrom(
               ColorUtils.getFixedWhiteColor(),
               ColorUtils.getOverallColorFrom(
                  ClientColors.getColor2(), ClientColors.getColor1(), MathUtils.clamp(MathUtils.getDifferenceOf(sY + 3.0F, y + 60.0F) / 60.0F, 0.0F, 1.0F)
               ),
               0.35F
            );
            Fonts.mntsb_12
               .drawStringWithOutline(
                  -yP + "", x + 2.0F + 7.0F - MathHelper.sin(MathHelper.toRadians(yDiff)) * 14.0F, sY + 2.0F, ColorUtils.swapAlpha(c, alpha)
               );
         }
      }

      StencilUtil.uninitStencilBuffer();
      RenderUtils.drawTwoAlphedSideways(
         (double)x, (double)y, (double)(x + w), (double)(y + 0.5F), ClientColors.getColor1(0, 0.0F), ClientColors.getColor1(0, 0.3F), true
      );
      RenderUtils.drawTwoAlphedSideways(
         (double)x, (double)(y + 60.0F - 0.5F), (double)(x + w), (double)(y + 60.0F), ClientColors.getColor2(0, 0.0F), ClientColors.getColor2(0, 0.3F), true
      );
   }

   private void drawDrag(float x, float y, float w, float h, float round, float outlineW, int fillCol, int outCol, ScaledResolution sr) {
      float x2 = x + w;
      float y2 = y + h;
      RenderUtils.drawRoundOutline(x, y, w, h, round, outlineW, fillCol, outCol, sr);
      x += 3.0F;
      y += 3.0F;
      x2 -= 6.0F;
      y2 -= 6.0F;
      w -= 2.0F;
      h -= 2.0F;
      float alphaPC = ColorUtils.getGLAlphaFromColor(outCol);
      boolean canFill = h > round * 2.0F && alphaPC * 255.0F >= 1.0F;
      if (canFill) {
         this.drawDragOverlay(x + 1.0F, y + 1.0F, x2 - 1.0F, y2 - 1.0F, round, outlineW, alphaPC);
      }
   }

   private void drawDragOverlay(float x, float y, float x2, float y2, float round, float shadow, float alphaPC) {
      if (!(alphaPC * 255.0F < 1.0F)) {
         float expands = 1.5F + alphaPC * 2.0F;
         float[] timePCS = new float[4];
         float timePCInterval01 = 0.4F;
         float timePCDelay = 2000.0F / (1.0F - timePCInterval01);

         for (int i = 0; i < 4; i++) {
            float distance0 = 0.0F;
            switch (i) {
               case 0:
                  distance0 = 0.0F;
                  break;
               case 1:
                  distance0 = 0.075F;
                  break;
               case 2:
                  distance0 = 0.15F;
                  break;
               case 3:
                  distance0 = 0.075F;
            }

            float timePC = (float)(System.currentTimeMillis() % (long)((int)timePCDelay)) / timePCDelay;
            timePC -= timePCInterval01;
            timePC = Math.max(timePC, 0.0F);
            timePC /= 1.0F - timePCInterval01;
            timePC = Math.min(timePC, 1.0F);
            float animation = (1.0F + timePC - distance0) % 1.0F;
            animation = (float)MathUtils.easeInOutQuadWave((double)(animation * animation));
            timePCS[i] = animation;
         }

         int[] bgColors = new int[]{
            ColorUtils.swapAlpha(ColorUtils.toDark(-1, timePCS[0] * timePCS[0]), (float)((int)(255.0F * alphaPC * timePCS[0] * timePCS[0] * 0.7F))),
            ColorUtils.swapAlpha(ColorUtils.toDark(-1, timePCS[1] * timePCS[1]), (float)((int)(255.0F * alphaPC * timePCS[1] * timePCS[1] * 0.7F))),
            ColorUtils.swapAlpha(ColorUtils.toDark(-1, timePCS[2] * timePCS[2]), (float)((int)(255.0F * alphaPC * timePCS[2] * timePCS[2] * 0.7F))),
            ColorUtils.swapAlpha(ColorUtils.toDark(-1, timePCS[3] * timePCS[3]), (float)((int)(255.0F * alphaPC * timePCS[3] * timePCS[3] * 0.7F)))
         };
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            x - expands,
            y - expands,
            x2 + expands * 2.0F,
            y2 + expands * 2.0F,
            round,
            shadow,
            bgColors[0],
            bgColors[1],
            bgColors[2],
            bgColors[3],
            false,
            true,
            false
         );
      }
   }

   public void drawDrags(ScaledResolution sr) {
      if ((double)this.dragAnim.getAnim() > 0.05) {
         int c = ColorUtils.getColor(255, 255, 255, 255.0F * this.dragAnim.getAnim());
         float ext = 2.0F
            + this.dragAnim.getAnim()
            + (float)(!TargetHUD.get.Mode.currentMode.equalsIgnoreCase("WetWorn") && !TargetHUD.get.Mode.currentMode.equalsIgnoreCase("Entire") ? 0 : 1);
         this.drawDrag(
            TargetHUD.xPosHud - ext,
            TargetHUD.yPosHud - ext,
            TargetHUD.widthHud + ext * 2.0F,
            TargetHUD.heightHud + ext * 2.0F,
            8.0F
               * (TargetHUD.get.Mode.currentMode.equalsIgnoreCase("Entire") ? 0.5F : (TargetHUD.get.Mode.currentMode.equalsIgnoreCase("Bushy") ? 0.25F : 1.0F)),
            this.dragAnim.getAnim(),
            0,
            c,
            sr
         );
      }

      if ((double)this.dragAnim2.getAnim() > 0.05) {
         int c = ColorUtils.getColor(255, 255, 255, 255.0F * this.dragAnim2.getAnim());
         float ext = 1.0F + this.dragAnim2.getAnim();
         this.drawDrag(Hud.wmPosX - ext, Hud.wmPosY - ext, Hud.wmWidth + ext * 2.0F, Hud.wmHeight + ext * 2.0F, 2.0F, this.dragAnim2.getAnim(), 0, c, sr);
      }

      if ((double)this.dragAnim4.getAnim() > 0.05) {
         int c = ColorUtils.getColor(255, 255, 255, 255.0F * this.dragAnim4.getAnim());
         float ext = 2.0F + this.dragAnim4.getAnim();
         this.drawDrag(Hud.potPosX - ext, Hud.potPosY - ext, Hud.potWidth + ext * 2.0F, Hud.potHeight + ext * 2.0F, 4.0F, this.dragAnim4.getAnim(), 0, c, sr);
      }

      if ((double)this.dragAnim5.getAnim() > 0.05) {
         int c = ColorUtils.getColor(255, 255, 255, 255.0F * this.dragAnim5.getAnim());
         float ext = 2.0F;
         boolean verticalMode = !(Hud.armPosY > 6.0F)
            || Hud.armPosY > (float)(new ScaledResolution(this.mc).getScaledHeight() - 58)
               && MathUtils.getDifferenceOf(Hud.armPosX, (float)new ScaledResolution(this.mc).getScaledWidth() / 2.0F) < 94.0F
            || !(Hud.armWidth > Hud.armHeight);
         this.drawDrag(
            Hud.armPosX - ext,
            Hud.armPosY - ext - (float)(verticalMode ? 0 : 6),
            Hud.armWidth + ext * 2.0F,
            Hud.armHeight + ext * 2.0F + (float)(verticalMode ? 0 : 6),
            2.0F,
            this.dragAnim5.getAnim(),
            0,
            c,
            sr
         );
      }

      if ((double)this.dragAnim6.getAnim() > 0.05) {
         int c = ColorUtils.getColor(255, 255, 255, 255.0F * this.dragAnim6.getAnim());
         float ext = 2.0F + this.dragAnim6.getAnim();
         this.drawDrag(Hud.stPosX - ext, Hud.stPosY - ext, Hud.stWidth + ext * 2.0F, Hud.stHeight + ext * 2.0F, 3.0F, this.dragAnim6.getAnim(), 0, c, sr);
      }

      if ((double)this.dragAnim7.getAnim() > 0.05) {
         int c = ColorUtils.getColor(255, 255, 255, 255.0F * this.dragAnim7.getAnim());
         float ext = 1.0F + this.dragAnim7.getAnim();
         this.drawDrag(
            Hud.listPosX - ext - Hud.get.getArrayWidth(sr),
            Hud.listPosY - ext,
            Hud.get.getArrayWidth(sr) + ext * 2.0F,
            Hud.get.getArrayHeight() + ext * 2.0F,
            1.0F,
            this.dragAnim7.getAnim() / 4.0F,
            0,
            c,
            sr
         );
      }

      if ((double)this.dragAnim8.getAnim() > 0.05) {
         int c = ColorUtils.getColor(255, 255, 255, 255.0F * this.dragAnim8.getAnim());
         float ext = 2.0F + this.dragAnim8.getAnim();
         float[] pos = CommandGui.getWindowCoord();
         this.drawDrag(
            pos[0] - ext,
            pos[1] - ext,
            CommandGui.getWindowWidth() + ext * 2.0F,
            CommandGui.getWindowHeight() + 2.0F + ext * 2.0F,
            3.0F,
            this.dragAnim8.getAnim(),
            0,
            c,
            sr
         );
      }

      if ((double)this.dragAnim9.getAnim() > 0.05) {
         int c = ColorUtils.getColor(255, 255, 255, 255.0F * this.dragAnim9.getAnim());
         String mode = Timer.get.Render.currentMode;
         boolean isCircle = mode.equalsIgnoreCase("Circle");
         boolean isLine = mode.equalsIgnoreCase("Line");
         boolean isSmoothNine = mode.equalsIgnoreCase("SmoothNine");
         float ext = this.dragAnim8.getAnim() + (isCircle ? 2.5F : (isLine ? 2.0F : (isSmoothNine ? 3.0F : 0.0F)));
         float x = Timer.getX(sr);
         float y = Timer.getY(sr);
         float w = Timer.getWidth();
         float h = Timer.getHeight();
         float dx = (float)sr.getScaledWidth() / 2.0F - (x + w / 2.0F);
         float dy = (float)sr.getScaledHeight() / 2.0F - (y + h / 2.0F);
         boolean middle = Math.sqrt((double)(dx * dx + dy * dy)) < 2.0 && !mode.equalsIgnoreCase("Plate");
         if (middle) {
            x = (float)sr.getScaledWidth() / 2.0F - w / 2.0F;
            y = (float)sr.getScaledHeight() / 2.0F - w / 2.0F;
         }

         this.drawDrag(
            x - ext,
            y - ext,
            w + ext * 2.0F,
            h + ext * 2.0F,
            !isCircle && !isSmoothNine ? 3.0F : h / 2.0F + (float)(isSmoothNine ? 3 : 2),
            this.dragAnim9.getAnim(),
            0,
            c,
            sr
         );
      }

      if ((double)this.dragAnim10.getAnim() > 0.05) {
         int c = ColorUtils.getColor(255, 255, 255, 255.0F * this.dragAnim10.getAnim());
         float ext = 3.0F + this.dragAnim10.getAnim();
         this.drawDrag(Hud.kbPosX - ext, Hud.kbPosY - ext, Hud.kbWidth + ext * 2.0F, Hud.kbHeight + ext * 2.0F, 3.0F, this.dragAnim10.getAnim(), 0, c, sr);
      }

      if ((double)this.dragAnim11.getAnim() > 0.05) {
         int c = ColorUtils.getColor(255, 255, 255, 255.0F * this.dragAnim11.getAnim());
         float ext = 2.0F + this.dragAnim11.getAnim();
         this.drawDrag(Hud.pcPosX - ext, Hud.pcPosY - ext, Hud.pcWidth + ext * 2.0F, Hud.pcHeight + ext * 2.0F, 3.0F, this.dragAnim11.getAnim(), 0, c, sr);
      }

      if ((double)this.dragAnim12.getAnim() > 0.05) {
         int c = ColorUtils.getColor(255, 255, 255, (this.dragging12[1] ? 80.0F : 255.0F) * this.dragAnim12.getAnim());
         float ext = 3.0F + this.dragAnim12.getAnim();
         float x = MiniMap.get.getMapX(sr);
         float y = MiniMap.get.getMapY(sr);
         float s = MiniMap.get.getMapScale();
         if (!this.dragging12[1]) {
            this.drawDrag(x - ext, y - ext, s + ext * 2.0F, s + ext * 2.0F, MiniMap.get.getRound() + ext / 2.0F, this.dragAnim12.getAnim(), 0, c, sr);
         }

         if (!this.dragging12[0]) {
            float movS = 8.0F;
            float xMov = x + s - movS;
            float yMov = y + s - movS;
            this.drawDrag(xMov, yMov, movS, movS, movS / 2.0F, this.dragAnim12.getAnim(), 0, c, sr);
         }
      }

      if ((double)this.dragAnim13.getAnim() > 0.05) {
         int cx = ColorUtils.getColor(255, 255, 255, 255.0F * this.dragAnim13.anim);
         float extx = 2.0F + this.dragAnim13.anim;
         this.drawDrag(
            FriendsSLink.flPosX - extx,
            FriendsSLink.flPosY - extx,
            FriendsSLink.flWidth + extx * 2.0F,
            FriendsSLink.flHeight + extx * 2.0F,
            3.0F,
            this.dragAnim13.anim,
            0,
            cx,
            sr
         );
      }

      if ((double)this.dragAnim14.getAnim() > 0.05) {
         int cx = ColorUtils.getColor(255, 255, 255, 255.0F * this.dragAnim14.getAnim());
         float extx = 2.0F + this.dragAnim14.getAnim();
         float[] pos = RadioPlayer.getWindowCoord();
         this.drawDrag(
            pos[0] - extx,
            pos[1] - extx,
            RadioPlayer.getWindowWidth() + extx * 2.0F,
            RadioPlayer.getWindowHeight() + 2.0F + extx * 2.0F,
            3.0F,
            this.dragAnim14.getAnim(),
            0,
            cx,
            sr
         );
      }
   }

   @Override
   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      if (Panic.stop) {
         drawRect(2, (double)(height - 14), (double)(GuiChat.width - 2), (double)(height - 2), Integer.MIN_VALUE);
         this.inputField.drawTextBox();
         ITextComponent itextcomponent = this.mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());
         if (itextcomponent != null && itextcomponent.getStyle().getHoverEvent() != null) {
            this.handleComponentHover(itextcomponent, mouseX, mouseY);
         }

         super.drawScreen(mouseX, mouseY, partialTicks);
      } else {
         if (ComfortUi.get.isPaintInChat()) {
            paintUI.renderUpdatePanel(mouseX, mouseY);
         }

         CommandGui.updateMousePos(mouseX, mouseY);
         RadioPlayer.updateMousePos(mouseX, mouseY);
         ScaledResolution sr = new ScaledResolution(this.mc);
         this.drawDrags(sr);
         if (ComfortUi.get.isClipHelperInChat()) {
            this.cliper(sr, mouseX, mouseY);
         }

         float scW = (float)sr.getScaledWidth();
         float scH = (float)sr.getScaledHeight();
         this.x = TargetHUD.get.THudX.getFloat() * scW;
         this.y = TargetHUD.get.THudY.getFloat() * scH;
         this.x2 = Hud.get.WX.getFloat() * scW;
         this.y2 = Hud.get.WY.getFloat() * scH;
         this.x4 = Hud.get.PX.getFloat() * scW;
         this.y4 = Hud.get.PY.getFloat() * scH;
         this.x5 = Hud.get.AX.getFloat() * scW;
         this.y5 = Hud.get.AY.getFloat() * scH;
         this.x5 = Hud.get.SX.getFloat() * scW;
         this.y5 = Hud.get.SY.getFloat() * scH;
         this.x6 = Hud.get.LX.getFloat() * scW;
         this.y6 = Hud.get.LY.getFloat() * scH;
         this.x8 = CommandGui.get.WX.getFloat() * scW;
         this.y8 = CommandGui.get.WY.getFloat() * scH;
         this.x9 = Timer.getCoordsSettings()[0] * scW;
         this.y9 = Timer.getCoordsSettings()[1] * scH;
         this.x10 = Hud.get.KX.getFloat() * scW;
         this.y10 = Hud.get.KY.getFloat() * scH;
         this.x12 = MiniMap.get.MapX.getFloat() * scW;
         this.y12 = MiniMap.get.MapY.getFloat() * scH;
         this.x13 = FriendsSLink.get.FLX.getFloat() * scW;
         this.y13 = FriendsSLink.get.FLY.getFloat() * scH;
         this.x14 = RadioPlayer.get.WX.getFloat() * scW;
         this.y14 = RadioPlayer.get.WY.getFloat() * scH;
         if (this.dragging) {
            this.x = (float)(mouseX - this.dragX);
            this.y = (float)(mouseY - this.dragY);
            TargetHUD.get
               .THudX
               .setFloat(MathUtils.clamp(this.x / scW, (3.0F + TargetHUD.widthHud / 2.0F) / scW, 1.0F - (3.0F + TargetHUD.widthHud / 2.0F) / scW));
            TargetHUD.get
               .THudY
               .setFloat(MathUtils.clamp(this.y / scH, (3.0F + TargetHUD.heightHud / 2.0F) / scH, 1.0F - (3.0F + TargetHUD.heightHud / 2.0F) / scH));
         } else {
            this.dragX = (int)((float)mouseX - TargetHUD.get.THudX.getFloat() * scW);
            this.dragY = (int)((float)mouseY - TargetHUD.get.THudY.getFloat() * scH);
         }

         if (this.dragging2) {
            this.x2 = (float)(mouseX - this.dragX2);
            this.y2 = (float)(mouseY - this.dragY2);
            float curX = MathUtils.clamp(this.x2 / scW, 1.0F / scW, 1.0F - Hud.wmWidth / scW - 1.0F / scW);
            float curY = MathUtils.clamp(this.y2 / scH, 1.0F / scH, 1.0F - Hud.wmHeight / scH - 1.0F / scH);
            Hud.get.WX.setFloat(curX);
            Hud.get.WY.setFloat(curY);
         } else {
            this.dragX2 = (int)((float)mouseX - Hud.get.WX.getFloat() * scW);
            this.dragY2 = (int)((float)mouseY - Hud.get.WY.getFloat() * scH);
         }

         if (this.dragging4) {
            this.x4 = (float)(mouseX - this.dragX4);
            this.y4 = (float)(mouseY - this.dragY4);
            float curX = MathUtils.clamp(this.x4 / scW, 2.0F / scW, 1.0F - Hud.potWidth / scW - 2.0F / scW);
            float curY = MathUtils.clamp(this.y4 / scH, 2.0F / scH, 1.0F - Hud.potHeight / scH - 2.0F / scH);
            Hud.get.PX.setFloat(curX);
            Hud.get.PY.setFloat(curY);
         } else {
            this.dragX4 = (int)((float)mouseX - Hud.get.PX.getFloat() * scW);
            this.dragY4 = (int)((float)mouseY - Hud.get.PY.getFloat() * scH);
         }

         if (this.dragging5) {
            this.x5 = (float)(mouseX - this.dragX5);
            this.y5 = (float)(mouseY - this.dragY5);
            float curX = MathUtils.clamp(this.x5 / scW, 2.0F / scW, 1.0F - Hud.armWidth / scW - 2.0F / scW);
            float curY = MathUtils.clamp(this.y5 / scH, 2.0F / scH, 1.0F - Hud.armHeight / scH - 2.0F / scH);
            Hud.get.AX.setFloat(curX);
            Hud.get.AY.setFloat(curY);
         } else {
            this.dragX5 = (int)((float)mouseX - Hud.get.AX.getFloat() * scW);
            this.dragY5 = (int)((float)mouseY - Hud.get.AY.getFloat() * scH);
         }

         if (this.dragging6) {
            this.x6 = (float)(mouseX - this.dragX6);
            this.y6 = (float)(mouseY - this.dragY6);
            float curX = MathUtils.clamp(this.x6 / scW, 2.0F / scW, 1.0F - Hud.stWidth / scW - 2.0F / scW);
            float curY = MathUtils.clamp(this.y6 / scH, 2.0F / scH, 1.0F - Hud.stHeight / scH - 2.0F / scH);
            Hud.get.SX.setFloat(curX);
            Hud.get.SY.setFloat(curY);
         } else {
            this.dragX6 = (int)((float)mouseX - Hud.get.SX.getFloat() * scW);
            this.dragY6 = (int)((float)mouseY - Hud.get.SY.getFloat() * scH);
         }

         if (this.dragging7) {
            this.x7 = (float)(mouseX - this.dragX7);
            this.y7 = (float)(mouseY - this.dragY7);
            Hud.get.LX.setFloat(MathUtils.clamp(this.x7 / scW, (Hud.get.getArrayWidth(sr) + 0.5F) / scW, 1.0F));
            Hud.get.LY.setFloat(MathUtils.clamp(this.y7 / scH, 0.0F, 1.0F - (1.0F - (scH - Hud.get.getArrayHeight()) / scH)));
         } else {
            this.dragX7 = (int)((float)mouseX - Hud.get.LX.getFloat() * scW);
            this.dragY7 = (int)((float)mouseY - Hud.get.LY.getFloat() * scH);
         }

         if (this.dragging8) {
            this.x8 = (float)(mouseX - this.dragX8);
            this.y8 = (float)(mouseY - this.dragY8);
            float curX = MathUtils.clamp(this.x8 / scW, 2.0F / scW, 1.0F - CommandGui.getWindowWidth() / scW - 2.0F / scW);
            float curY = MathUtils.clamp(this.y8 / scH, 2.0F / scH, 1.0F - (CommandGui.getWindowHeight() + 3.0F) / scH - 2.0F / scH);
            float oldX = CommandGui.get.WX.getFloat();
            float oldY = CommandGui.get.WY.getFloat();
            CommandGui.get.WX.setFloat(MathUtils.lerp(oldX, curX, 0.03F * (float)Minecraft.frameTime));
            CommandGui.get.WY.setFloat(MathUtils.lerp(oldY, curY, 0.03F * (float)Minecraft.frameTime));
         } else {
            this.dragX8 = (int)((float)mouseX - CommandGui.get.WX.getFloat() * scW);
            this.dragY8 = (int)((float)mouseY - CommandGui.get.WY.getFloat() * scH);
         }

         if (this.dragging9) {
            this.x9 = (float)(mouseX - this.dragX9);
            this.y9 = (float)(mouseY - this.dragY9);
            float oldX = Timer.getCoordsSettings()[0];
            float oldY = Timer.getCoordsSettings()[1];
            float curX = MathUtils.clamp(this.x9 / scW, (Timer.getWidth() / 2.0F + 2.0F) / scW, 1.0F - Timer.getWidth() / 2.0F / scW - 2.0F / scW);
            float curY = MathUtils.clamp(this.y9 / scH, (Timer.getHeight() / 2.0F + 2.0F) / scH, 1.0F - Timer.getHeight() / 2.0F / scH - 2.0F / scH);
            Timer.setSetsX(MathUtils.lerp(oldX, curX, 0.02F * (float)Minecraft.frameTime));
            Timer.setSetsY(MathUtils.lerp(oldY, curY, 0.02F * (float)Minecraft.frameTime));
         } else {
            this.dragX9 = (int)((float)mouseX - Timer.getCoordsSettings()[0] * scW);
            this.dragY9 = (int)((float)mouseY - Timer.getCoordsSettings()[1] * scH);
         }

         if (this.dragging10) {
            this.x10 = (float)(mouseX - this.dragX10);
            this.y10 = (float)(mouseY - this.dragY10);
            float curX = MathUtils.clamp(this.x10 / scW, 2.0F / scW, 1.0F - Hud.kbWidth / scW - 2.0F / scW);
            float curY = MathUtils.clamp(this.y10 / scH, 2.0F / scH, 1.0F - Hud.kbHeight / scH - 2.0F / scH);
            Hud.get.KX.setFloat(curX);
            Hud.get.KY.setFloat(curY);
         } else {
            this.dragX10 = (int)((float)mouseX - Hud.get.KX.getFloat() * scW);
            this.dragY10 = (int)((float)mouseY - Hud.get.KY.getFloat() * scH);
         }

         if (this.dragging11) {
            this.x11 = (float)(mouseX - this.dragX11);
            this.y11 = (float)(mouseY - this.dragY11);
            float curX = MathUtils.clamp(this.x11 / scW, 2.0F / scW, 1.0F - Hud.pcWidth / scW - 2.0F / scW);
            float curY = MathUtils.clamp(this.y11 / scH, 2.0F / scH, 1.0F - Hud.pcHeight / scH - 2.0F / scH);
            Hud.get.PCX.setFloat(curX);
            Hud.get.PCY.setFloat(curY);
         } else {
            this.dragX11 = (int)((float)mouseX - Hud.get.PCX.getFloat() * scW);
            this.dragY11 = (int)((float)mouseY - Hud.get.PCY.getFloat() * scH);
         }

         if (this.dragging12[1]) {
            float scale = (float)(Mouse.getDX() - Mouse.getDY()) / 1.375F / 2.0F / 1.5F / ScaledResolution.lpSCFactor();
            if (!Float.isNaN(scale)) {
               scale += MiniMap.get.MapScale.getFloat();
               float lpSCFactor = ScaledResolution.lpSCFactor();
               if (scale < MiniMap.get.MapScale.fMin) {
                  scale = MiniMap.get.MapScale.fMin;
               }

               if (scale > MiniMap.get.MapScale.fMax / lpSCFactor) {
                  scale = MiniMap.get.MapScale.fMax / lpSCFactor;
               }

               MiniMap.get.MapScale.setFloat(scale);
            }
         } else if (this.dragging12[0]) {
            this.x12 = (float)(mouseX - this.dragX12);
            this.y12 = (float)(mouseY - this.dragY12);
            float curX = MathUtils.clamp(this.x12 / scW, 2.0F / scW, 1.0F - MiniMap.get.getMapScale() / scW - 2.0F / scW);
            float curY = MathUtils.clamp(this.y12 / scH, 2.0F / scH, 1.0F - MiniMap.get.getMapScale() / scH - 2.0F / scH);
            MiniMap.get.MapX.setFloat(MathUtils.lerp(MiniMap.get.MapX.getFloat(), curX, MathUtils.clamp(0.1F * (float)Minecraft.frameTime, 0.0F, 1.0F)));
            MiniMap.get.MapY.setFloat(MathUtils.lerp(MiniMap.get.MapY.getFloat(), curY, MathUtils.clamp(0.1F * (float)Minecraft.frameTime, 0.0F, 1.0F)));
         } else {
            this.dragX12 = (int)((float)mouseX - MiniMap.get.MapX.getFloat() * scW);
            this.dragY12 = (int)((float)mouseY - MiniMap.get.MapY.getFloat() * scH);
         }

         if (this.dragging13) {
            this.x13 = (float)(mouseX - this.dragX13);
            this.y13 = (float)(mouseY - this.dragY13);
            float curX = MathUtils.clamp(this.x13 / scW, 2.0F / scW, 1.0F - FriendsSLink.flWidth / scW - 2.0F / scW);
            float curY = MathUtils.clamp(this.y13 / scH, 2.0F / scH, 1.0F - FriendsSLink.flHeight / scH - 2.0F / scH);
            FriendsSLink.get.FLX.setFloat(curX);
            FriendsSLink.get.FLY.setFloat(curY);
         } else {
            this.dragX13 = (int)((float)mouseX - FriendsSLink.get.FLX.getFloat() * scW);
            this.dragY13 = (int)((float)mouseY - FriendsSLink.get.FLY.getFloat() * scH);
         }

         if (this.dragging14) {
            this.x14 = (float)(mouseX - this.dragX14);
            this.y14 = (float)(mouseY - this.dragY14);
            float curX = MathUtils.clamp(this.x14 / scW, 2.0F / scW, 1.0F - RadioPlayer.getWindowWidth() / scW - 2.0F / scW);
            float curY = MathUtils.clamp(this.y14 / scH, 2.0F / scH, 1.0F - (RadioPlayer.getWindowHeight() + 3.0F) / scH - 2.0F / scH);
            float oldX = RadioPlayer.get.WX.getFloat();
            float oldY = RadioPlayer.get.WY.getFloat();
            RadioPlayer.get.WX.setFloat(MathUtils.lerp(oldX, curX, 0.03F * (float)Minecraft.frameTime));
            RadioPlayer.get.WY.setFloat(MathUtils.lerp(oldY, curY, 0.03F * (float)Minecraft.frameTime));
         } else {
            this.dragX14 = (int)((float)mouseX - RadioPlayer.get.WX.getFloat() * scW);
            this.dragY14 = (int)((float)mouseY - RadioPlayer.get.WY.getFloat() * scH);
         }

         boolean lb = Mouse.isButtonDown(0);
         if (!ComfortUi.get.isPaintInChat() || !paintUI.isDragging && !paintUI.isHoveredAny(mouseX, mouseY, false)) {
            boolean[] hoveredToMinimap = MiniMap.get.isHoveredToMinimap(mouseX, mouseY, sr);
            if (RadioPlayer.isHoveredToPanel(false)) {
               if (RadioPlayer.isHoveredToPanel(true)) {
                  this.dragging14 = lb
                     && !this.dragging4
                     && !this.dragging
                     && !this.dragging2
                     && !this.dragging3
                     && !this.dragging5
                     && !this.dragging6
                     && !this.dragging7
                     && !this.dragging9
                     && !this.dragging10
                     && !this.dragging11
                     && !this.dragging12[0]
                     && !this.dragging12[1]
                     && !this.dragging13
                     && !this.dragging8;
               }

               this.dragAnim14.to = this.dragging14 ? 1.0F : 0.0F;
            } else if (FriendsSLink.get.isHoveredFriendsInfoHUD(mouseX, mouseY) && lb) {
               this.dragging13 = !this.dragging4
                  && !this.dragging
                  && !this.dragging2
                  && !this.dragging3
                  && !this.dragging5
                  && !this.dragging7
                  && !this.dragging8
                  && !this.dragging9
                  && !this.dragging6
                  && !this.dragging10
                  && !this.dragging11
                  && !this.dragging12[0]
                  && !this.dragging12[1]
                  && !this.dragging14;
               this.dragAnim13.to = this.dragging13 ? 1.0F : 0.0F;
            } else if (hoveredToMinimap[1] && lb && !this.dragging12[0]) {
               this.dragging12[1] = !this.dragging4
                  && !this.dragging
                  && !this.dragging2
                  && !this.dragging3
                  && !this.dragging5
                  && !this.dragging7
                  && !this.dragging8
                  && !this.dragging9
                  && !this.dragging6
                  && !this.dragging10
                  && !this.dragging11
                  && !this.dragging12[0]
                  && !this.dragging13
                  && !this.dragging14;
               this.dragAnim12.to = this.dragging12[1] ? 1.0F : 0.0F;
            } else if (hoveredToMinimap[0] && lb && !this.dragging12[1]) {
               this.dragging12[0] = !this.dragging4
                  && !this.dragging
                  && !this.dragging2
                  && !this.dragging3
                  && !this.dragging5
                  && !this.dragging7
                  && !this.dragging8
                  && !this.dragging9
                  && !this.dragging6
                  && !this.dragging10
                  && !this.dragging11
                  && !this.dragging12[1]
                  && !this.dragging13
                  && !this.dragging14;
               this.dragAnim12.to = this.dragging12[0] ? 1.0F : 0.0F;
            } else if (Hud.get.isHoveredPickupsHUD(mouseX, mouseY)) {
               this.dragging11 = lb
                  && !this.dragging4
                  && !this.dragging
                  && !this.dragging2
                  && !this.dragging3
                  && !this.dragging5
                  && !this.dragging7
                  && !this.dragging8
                  && !this.dragging9
                  && !this.dragging6
                  && !this.dragging10
                  && !this.dragging12[0]
                  && !this.dragging12[1]
                  && !this.dragging13
                  && !this.dragging14;
               this.dragAnim11.to = this.dragging11 ? 1.0F : 0.0F;
            } else if (Hud.get.isHoveredKeyBindsHUD(mouseX, mouseY)) {
               this.dragging10 = lb
                  && !this.dragging4
                  && !this.dragging
                  && !this.dragging2
                  && !this.dragging3
                  && !this.dragging5
                  && !this.dragging7
                  && !this.dragging8
                  && !this.dragging9
                  && !this.dragging6
                  && !this.dragging11
                  && !this.dragging12[0]
                  && !this.dragging12[1]
                  && !this.dragging13
                  && !this.dragging14;
               this.dragAnim10.to = this.dragging10 ? 1.0F : 0.0F;
            } else if (Hud.get.isHoveredStaffListHUD(mouseX, mouseY)) {
               this.dragging6 = lb
                  && !this.dragging4
                  && !this.dragging
                  && !this.dragging2
                  && !this.dragging3
                  && !this.dragging5
                  && !this.dragging7
                  && !this.dragging8
                  && !this.dragging9
                  && !this.dragging10
                  && !this.dragging11
                  && !this.dragging12[0]
                  && !this.dragging12[1]
                  && !this.dragging13
                  && !this.dragging14;
               this.dragAnim6.to = this.dragging6 ? 1.0F : 0.0F;
            } else if (Hud.get.isHoveredToArmorHUD(mouseX, mouseY)) {
               this.dragging5 = lb
                  && !this.dragging4
                  && !this.dragging
                  && !this.dragging2
                  && !this.dragging3
                  && !this.dragging6
                  && !this.dragging7
                  && !this.dragging8
                  && !this.dragging9
                  && !this.dragging10
                  && !this.dragging11
                  && !this.dragging12[0]
                  && !this.dragging12[1]
                  && !this.dragging13
                  && !this.dragging14;
               this.dragAnim5.to = this.dragging5 ? 1.0F : 0.0F;
            } else if (Hud.get.isHoveredToPotionsHUD(mouseX, mouseY)) {
               this.dragging4 = lb
                  && !this.dragging2
                  && !this.dragging
                  && !this.dragging3
                  && !this.dragging5
                  && !this.dragging6
                  && !this.dragging7
                  && !this.dragging8
                  && !this.dragging9
                  && !this.dragging10
                  && !this.dragging11
                  && !this.dragging12[0]
                  && !this.dragging12[1]
                  && !this.dragging13
                  && !this.dragging14;
               this.dragAnim4.to = this.dragging4 ? 1.0F : 0.0F;
            } else if (Hud.get.isHoverToArrayList(mouseX, mouseY, sr)) {
               this.dragging7 = lb
                  && !this.dragging4
                  && !this.dragging
                  && !this.dragging2
                  && !this.dragging3
                  && !this.dragging5
                  && !this.dragging6
                  && !this.dragging8
                  && !this.dragging9
                  && !this.dragging10
                  && !this.dragging11
                  && !this.dragging12[0]
                  && !this.dragging12[1]
                  && !this.dragging13
                  && !this.dragging14;
               this.dragAnim7.to = this.dragging7 ? 1.0F : 0.0F;
            } else if (Timer.isHoveredToTimer(mouseX, mouseY, sr)) {
               this.dragging9 = lb
                  && !this.dragging4
                  && !this.dragging
                  && !this.dragging2
                  && !this.dragging3
                  && !this.dragging5
                  && !this.dragging6
                  && !this.dragging8
                  && !this.dragging7
                  && !this.dragging10
                  && !this.dragging11
                  && !this.dragging12[0]
                  && !this.dragging12[1]
                  && !this.dragging13
                  && !this.dragging14;
               this.dragAnim9.to = this.dragging9 ? 1.0F : 0.0F;
            } else if (HoverUtils.isHovered(
                  (int)(Hud.get.WX.getFloat() * scW),
                  (int)(Hud.get.WY.getFloat() * scH),
                  (int)(Hud.get.WX.getFloat() * scW + Hud.wmWidth),
                  (int)(Hud.get.WY.getFloat() * scH + Hud.wmHeight),
                  mouseX,
                  mouseY
               )
               && Hud.get.isActived()
               && Hud.get.Watermark.getBool()) {
               this.dragging2 = lb
                  && !this.dragging
                  && !this.dragging3
                  && !this.dragging4
                  && !this.dragging5
                  && !this.dragging6
                  && !this.dragging7
                  && !this.dragging8
                  && !this.dragging9
                  && !this.dragging10
                  && !this.dragging11
                  && !this.dragging12[0]
                  && !this.dragging12[1]
                  && !this.dragging13
                  && !this.dragging14;
               this.dragAnim2.to = this.dragging2 ? 1.0F : 0.0F;
            } else if (CommandGui.isHoveredToPanel(false)) {
               if (CommandGui.isHoveredToPanel(true)) {
                  this.dragging8 = lb
                     && !this.dragging4
                     && !this.dragging
                     && !this.dragging2
                     && !this.dragging3
                     && !this.dragging5
                     && !this.dragging6
                     && !this.dragging7
                     && !this.dragging9
                     && !this.dragging10
                     && !this.dragging11
                     && !this.dragging12[0]
                     && !this.dragging12[1]
                     && !this.dragging13
                     && !this.dragging14;
               }

               this.dragAnim8.to = this.dragging8 ? 1.0F : 0.0F;
            } else if (HoverUtils.isHovered(
                  (int)(TargetHUD.get.THudX.getFloat() * scW - TargetHUD.widthHud / 2.0F - 1.5F),
                  (int)(TargetHUD.get.THudY.getFloat() * scH - TargetHUD.heightHud / 2.0F - 1.5F),
                  (int)(TargetHUD.get.THudX.getFloat() * scW + TargetHUD.widthHud / 2.0F + 1.5F),
                  (int)(TargetHUD.get.THudY.getFloat() * scH + TargetHUD.heightHud / 2.0F),
                  mouseX,
                  mouseY
               )
               && TargetHUD.get.isActived()) {
               this.dragging = lb
                  && !this.dragging2
                  && !this.dragging3
                  && !this.dragging4
                  && !this.dragging5
                  && !this.dragging6
                  && !this.dragging7
                  && !this.dragging8
                  && !this.dragging9
                  && !this.dragging10
                  && !this.dragging11
                  && !this.dragging12[0]
                  && !this.dragging12[1]
                  && !this.dragging13
                  && !this.dragging14;
               this.dragAnim.to = this.dragging ? 1.0F : 0.0F;
            }
         } else {
            this.dragging14 = false;
            this.dragging13 = false;
            this.dragging12[0] = false;
            this.dragging12[1] = false;
            this.dragging11 = false;
            this.dragging10 = false;
            this.dragging9 = false;
            this.dragging8 = false;
            this.dragging7 = false;
            this.dragging6 = false;
            this.dragging5 = false;
            this.dragging4 = false;
            this.dragging3 = false;
            this.dragging2 = false;
            this.dragging = false;
            this.dragAnim13.to = 0.0F;
            this.dragAnim12.to = 0.0F;
            this.dragAnim11.to = 0.0F;
            this.dragAnim10.to = 0.0F;
            this.dragAnim9.to = 0.0F;
            this.dragAnim8.to = 0.0F;
            this.dragAnim7.to = 0.0F;
            this.dragAnim6.to = 0.0F;
            this.dragAnim5.to = 0.0F;
            this.dragAnim4.to = 0.0F;
            this.dragAnim3.to = 0.0F;
            this.dragAnim2.to = 0.0F;
            this.dragAnim.to = 0.0F;
         }

         if (ComfortUi.get.isPaintInChat()
            && paintUI.isHoveredAny(mouseX, mouseY, true)
            && (
               this.dragging
                  || this.dragging2
                  || this.dragging3
                  || this.dragging4
                  || this.dragging5
                  || this.dragging6
                  || this.dragging7
                  || this.dragging8
                  || this.dragging9
                  || this.dragging10
                  || this.dragging11
                  || this.dragging12[0]
                  || this.dragging12[1]
                  || this.dragging13
                  || this.dragging14
            )) {
            paintUI.isOpen = false;
         }

         float width = this.mc.fontRendererObj.getStringWidth(this.inputField.getText() + "__") < 10
            ? 3.0F
            : (float)this.mc.fontRendererObj.getStringWidth(this.inputField.getText() + "__");
         this.textwidth = MathUtils.lerp(this.textwidth, width, (float)Minecraft.frameTime * 0.01F);
         this.textwidthSmooth = MathUtils.clamp(
            MathUtils.lerp(
               this.textwidthSmooth,
               this.textwidth + MathUtils.clamp(width * 5.0F - this.textwidth * 5.0F, 0.0F, this.textwidth),
               (float)Minecraft.frameTime * 0.015F
            ),
            0.0F,
            width + 100.0F
         );
         float finalWidth = MathUtils.clamp(this.textwidthSmooth, 0.0F, (float)(GuiScreen.width - 20));
         GL11.glPushMatrix();
         if (!ComfortUi.get.isBetterChatline()) {
            GL11.glTranslated(0.0, (double)(20.0F - this.alphaPC.getAnim() * 20.0F), 0.0);
         }

         if (ComfortUi.get.isAddClientButtons()) {
            if (Mouse.isButtonDown(0)) {
               if (this.clickHider
                  && HoverUtils.isHovered(scW - Fonts.mntsb_20.getStringWidth("Hide info") - 6.0F, scH - 14.0F, scW - 2.0F, scH - 2.0F, mouseX, mouseY)) {
                  keyHide = !keyHide;
               }

               this.clickHider = false;
            } else {
               this.clickHider = true;
            }
         }

         if (keyHide) {
            ColorUtils.getColor(100, 255, 100);
         } else {
            ColorUtils.getColor(100, 100, 100);
         }

         if (ComfortUi.get.isAddClientButtons()) {
            RenderUtils.drawAlphedRect(
               (double)(scW - Fonts.mntsb_20.getStringWidth("Hide info") - 6.0F),
               (double)(scH - 14.0F),
               (double)(scW - 2.0F),
               (double)(scH - 2.0F),
               keyHide ? ColorUtils.getColor(255, 255, 255, 160) : ColorUtils.getColor(255, 255, 255, 80)
            );
            RenderUtils.drawLightContureRect(
               (double)(scW - Fonts.mntsb_20.getStringWidth("Hide info") - 6.0F),
               (double)(scH - 14.0F),
               (double)(scW - 2.0F),
               (double)(scH - 2.0F),
               keyHide ? ColorUtils.getColor(50, 50, 50, 160) : ColorUtils.getColor(50, 50, 50, 80)
            );
            Fonts.mntsb_20
               .drawStringWithShadow(
                  "Hide info",
                  scW - Fonts.mntsb_20.getStringWidth("Hide info") - 3.5F,
                  scH - Fonts.mntsb_20.getHeight() - 4.5F,
                  keyHide ? ColorUtils.getColor(255, 255, 255, 100) : ColorUtils.getColor(255, 255, 255, 30)
               );
         }

         String s = this.inputField.getText();
         if (ComfortUi.get.isBetterChatline()) {
            int bgC = -1;
            int bgC2 = ColorUtils.getColor(0, 0, 0, 160);
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               4.0F, (float)height - 13.5F, 16.0F + finalWidth + 6.0F, (float)height - 3.5F, 2.0F, 2.0F, bgC2, bgC2, bgC2, bgC2, false, true, true
            );
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               5.0F, (float)(height - 12), 6.0F, (float)(height - 5), 0.5F, 1.0F, bgC, bgC, bgC, bgC, false, true, true
            );
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               13.5F + finalWidth + 6.0F,
               (float)(height - 12),
               14.5F + finalWidth + 6.0F,
               (float)(height - 5),
               0.5F,
               1.0F,
               bgC,
               bgC,
               bgC,
               bgC,
               false,
               true,
               true
            );
            GL11.glTranslated(3.0, 0.0, 0.0);
            this.inputField.drawTextBox();
            GL11.glTranslated(-3.0, 0.0, 0.0);
         } else {
            drawRect(
               2,
               (double)(height - 14),
               (double)((float)GuiScreen.width - (ComfortUi.get.isAddClientButtons() ? 7.0F - Fonts.mntsb_20.getStringWidth("Hide info") : 4.0F)),
               (double)(height - 2),
               Integer.MIN_VALUE
            );
            this.inputField.drawTextBox();
         }

         String hideStart = "";
         float w = 0.0F;
         if (this.inputField.getText().startsWith("/") && keyHide) {
            if (s.startsWith("/warp ")) {
               hideStart = "warp";
            }

            if (s.startsWith("/reg ")) {
               hideStart = "reg";
            }

            if (s.startsWith("/register ")) {
               hideStart = "register";
            }

            if (s.startsWith("/changepassword ")) {
               hideStart = "changepassword";
            }

            if (s.startsWith("/l ")) {
               hideStart = "l";
            }

            if (s.startsWith("/login ")) {
               hideStart = "login";
            }

            if (s.startsWith("/home ")) {
               hideStart = "home";
            }

            if (!hideStart.equalsIgnoreCase("")) {
               w = (float)this.mc.fontRendererObj.getStringWidth(hideStart);
               Client.blur
                  .blur(
                     17.0F + w,
                     scH - 13.0F,
                     (float)(14 + this.mc.fontRendererObj.getStringWidth(this.inputField.getText())),
                     scH - 3.0F,
                     5.0F * ScaledResolution.lpSCFactor()
                  );
            }
         }

         ITextComponent itextcomponent = this.mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());
         if (itextcomponent != null && itextcomponent.getStyle().getHoverEvent() != null) {
            this.handleComponentHover(itextcomponent, mouseX, mouseY);
         }

         GL11.glPopMatrix();
         super.drawScreen(mouseX, mouseY, partialTicks);
      }
   }

   @Override
   public boolean doesGuiPauseGame() {
      return false;
   }

   @Override
   public void setCompletions(String... newCompletions) {
      this.tabCompleter.setCompletions(newCompletions);
   }

   public static class ChatTabCompleter extends TabCompleter {
      private final Minecraft clientInstance = Minecraft.getMinecraft();

      public ChatTabCompleter(GuiTextField p_i46749_1_) {
         super(p_i46749_1_, false);
      }

      @Override
      public void complete() {
         super.complete();
         if (this.completions.size() > 1) {
            StringBuilder stringbuilder = new StringBuilder();

            for (String s : this.completions) {
               if (stringbuilder.length() > 0) {
                  stringbuilder.append(", ");
               }

               stringbuilder.append(s);
            }

            this.clientInstance.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new TextComponentString(stringbuilder.toString()), 1);
         }
      }

      @Nullable
      @Override
      public BlockPos getTargetBlockPos() {
         BlockPos blockpos = null;
         if (this.clientInstance.objectMouseOver != null && this.clientInstance.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
            blockpos = this.clientInstance.objectMouseOver.getBlockPos();
         }

         return blockpos;
      }
   }
}
