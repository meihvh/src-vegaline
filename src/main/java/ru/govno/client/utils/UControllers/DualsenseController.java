package ru.govno.client.utils.UControllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.event.events.EventInput;
import ru.govno.client.event.events.EventRender2D;
import ru.govno.client.module.Module;
import ru.govno.client.module.modules.ClientTune;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.DualsenseCachedReader;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.MacroMngr.Macros;
import ru.govno.client.utils.Math.MathUtil;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class DualsenseController {
   public static boolean CONTROLLER_LOADED;
   public static boolean CONTROLLER_USED;
   public static boolean CONTROLLER_BINDS_SYS_ACTIVE;
   public static boolean CONTROLLER_BIND_SETTINGS_OPENED;
   public static boolean SQUARE_PRESSED;
   public static boolean CROSS_PRESSED;
   public static boolean CIRCLE_PRESSED;
   public static boolean TRIANGLE_PRESSED;
   public static boolean UP_PRESSED;
   public static boolean DOWN_PRESSED;
   public static boolean LEFT_PRESSED;
   public static boolean RIGHT_PRESSED;
   public static boolean L1_PRESSED;
   public static boolean R1_PRESSED;
   public static boolean L2_PRESSED;
   public static boolean R2_PRESSED;
   public static boolean L3_PRESSED;
   public static boolean R3_PRESSED;
   public static boolean SHARE_PRESSED;
   public static boolean OPTIONS_PRESSED;
   public static boolean PS_PRESSED;
   public static boolean MUTE_PRESSED;
   public static boolean TOUCHBTN_PRESSED;
   public static boolean TOUCH_PRESSED;
   public static final Minecraft mc = Minecraft.getMinecraft();
   private static DualsenseController.VirtualGamepad gamepad;
   private static final Map<String, ResourceLocation> cachedTextures = new HashMap<>();
   private static int currentIndexSelector = 0;
   private static int currentIndexContent = 0;
   private static int currentScrollBackContent = 0;
   private static final List<String> listSelectors = Arrays.asList("Modules binding", "Macroses binding", "Show binds");
   private static final List<String> listContentsCurrentSelector = new ArrayList<>();
   private static boolean inTheCurrentSelector;
   private static boolean showBinds = true;
   private static final AnimationUtils currentAnimationScrollBackContent = new AnimationUtils(0.0F, 0.0F, 0.07F);

   public static void onEventKeyInput(EventInput event) {
      if (CONTROLLER_LOADED && CONTROLLER_USED) {
         CONTROLLER_USED = false;
      }
   }

   public static void onEventRender2D(EventRender2D event) {
      if (CONTROLLER_LOADED && CONTROLLER_USED) {
         boolean mouseChange = ((float)Math.abs(Mouse.getDX()) > 5.0F || (float)Math.abs(Mouse.getDY()) > 5.0F)
            && DualsenseController.SenseAxis.RIGHT_STICK.getAxisMove(DualsenseController.SenseAxis.RIGHT_STICK.getInputValues()) == 0.0;
         if (!mouseChange
            && mc.currentScreen == null
            && DualsenseController.SenseAxis.RIGHT_STICK.getAxisMove(DualsenseController.SenseAxis.RIGHT_STICK.getInputValues()) == 0.0
            && ((float)Math.abs(Mouse.getEventDX()) > 40.0F || (float)Math.abs(Mouse.getEventDY()) > 40.0F)) {
            mouseChange = true;
         }

         if (mouseChange) {
            CONTROLLER_USED = false;
         }

         if (mc.currentScreen != null) {
            ScaledResolution sr = event.getResolution();
            float mx = (float)GuiScreen.staticMouseX;
            float my = (float)GuiScreen.staticMouseY;
            float lWD2 = 0.25F;
            int col0 = -1;
            int col1 = 0;
            GL11.glPushMatrix();
            GL11.glDisable(2929);
            GL11.glDepthRange(0.0, 0.01F);
            RenderUtils.drawVGradientRect(mx - lWD2, 0.0F, mx + lWD2, my, col1, col0);
            RenderUtils.drawVGradientRect(mx - lWD2, my, mx + lWD2, (float)sr.getScaledHeight(), col0, col1);
            RenderUtils.drawAlphedSideways(0.0, (double)(my - lWD2), (double)mx, (double)(my + lWD2), col1, col0);
            RenderUtils.drawAlphedSideways((double)mx, (double)(my - lWD2), (double)sr.getScaledWidth(), (double)(my + lWD2), col0, col1);
            GL11.glDepthRange(0.0, 1.0);
            GL11.glEnable(2929);
            GL11.glPopMatrix();
         }
      }
   }

   public static void onPressSenseButtonEvent(DualsenseController.SenseButton button, boolean isPush) {
      if (CONTROLLER_LOADED) {
         updateBindingSelectorsOnTypingButtons(button, isPush);
         if (isPush && CONTROLLER_BINDS_SYS_ACTIVE && gamepad != null) {
            gamepad.getBindHandler().onPressSenseButtonBindsExecute(button);
         }
      }
   }

   public static void onMoveSenseAxisEvent(DualsenseController.SenseAxis axis, float[] from, float[] to) {
   }

   private static boolean checkComboPressedAnyway(
      DualsenseController.SenseButton[] comboButtons, DualsenseController.SenseButton eventPressButton, boolean isPressedEvent
   ) {
      if (comboButtons.length != 0 && isPressedEvent) {
         boolean checkCombo = true;

         for (int btnIndex = 0; btnIndex < comboButtons.length; btnIndex++) {
            DualsenseController.SenseButton comboBindToggleButton = comboButtons[btnIndex];
            if (btnIndex == 0 && !comboBindToggleButton.isPressedAnyway()) {
               return false;
            }

            if (!comboBindToggleButton.isPressedAnyway()) {
               checkCombo = false;
               break;
            }

            if (btnIndex == comboButtons.length - 1 && comboBindToggleButton != eventPressButton) {
               checkCombo = false;
               break;
            }
         }

         return checkCombo;
      } else {
         return false;
      }
   }

   public static void findAndUpdateDualsense() {
      if (!Panic.stop && DualsenseSettings.useDualsenseForGaming()) {
         DualsenseCachedReader.update();
         CONTROLLER_LOADED = DualsenseCachedReader.connected;
         if (CONTROLLER_LOADED) {
            if (gamepad == null) {
               gamepad = new DualsenseController.VirtualGamepad();
               gamepad.getBindHandler().getBindsSaver().load(new ArrayList<>());
            }

            gamepad.updateController();
         } else if (!CONTROLLER_LOADED) {
            gamepad = null;
            CONTROLLER_BINDS_SYS_ACTIVE = false;
            CONTROLLER_BIND_SETTINGS_OPENED = false;
         }

         if (!CONTROLLER_LOADED) {
            CONTROLLER_USED = false;
            CONTROLLER_BINDS_SYS_ACTIVE = false;
            CONTROLLER_BIND_SETTINGS_OPENED = false;
         } else if (gamepad != null) {
            if (gamepad.anyInputGamepad()) {
               CONTROLLER_USED = true;
            }

            CONTROLLER_BINDS_SYS_ACTIVE = !CONTROLLER_BIND_SETTINGS_OPENED && DualsenseSettings.bindsTriggerActivator().isPressedAnyway();
         }

         updateButtonsState(CONTROLLER_LOADED && CONTROLLER_USED);
      } else {
         if (CONTROLLER_LOADED) {
            updateButtonsState(false);
         }

         CONTROLLER_LOADED = false;
         CONTROLLER_USED = false;
         CONTROLLER_BINDS_SYS_ACTIVE = false;
         CONTROLLER_BIND_SETTINGS_OPENED = false;
      }
   }

   private static void updateButtonsState(boolean gamepadsLoaded) {
      if (gamepadsLoaded) {
         SQUARE_PRESSED = DualsenseController.SenseButton.SQUARE.isPressed();
         CROSS_PRESSED = DualsenseController.SenseButton.CROSS.isPressed();
         CIRCLE_PRESSED = DualsenseController.SenseButton.CIRCLE.isPressed();
         TRIANGLE_PRESSED = DualsenseController.SenseButton.TRIANGLE.isPressed();
         UP_PRESSED = DualsenseController.SenseButton.UP.isPressed();
         DOWN_PRESSED = DualsenseController.SenseButton.DOWN.isPressed();
         LEFT_PRESSED = DualsenseController.SenseButton.LEFT.isPressed();
         RIGHT_PRESSED = DualsenseController.SenseButton.RIGHT.isPressed();
         L1_PRESSED = DualsenseController.SenseButton.L1.isPressed();
         R1_PRESSED = DualsenseController.SenseButton.R1.isPressed();
         L2_PRESSED = DualsenseController.SenseButton.L2.isPressed();
         R2_PRESSED = DualsenseController.SenseButton.R2.isPressed();
         L3_PRESSED = DualsenseController.SenseButton.L3.isPressed();
         R3_PRESSED = DualsenseController.SenseButton.R3.isPressed();
         SHARE_PRESSED = DualsenseController.SenseButton.SHARE.isPressed();
         OPTIONS_PRESSED = DualsenseController.SenseButton.OPTIONS.isPressed();
         PS_PRESSED = DualsenseController.SenseButton.PS.isPressed();
         MUTE_PRESSED = DualsenseController.SenseButton.MUTE.isPressed();
         TOUCHBTN_PRESSED = DualsenseController.SenseButton.TOUCHBTN.isPressed();
         TOUCH_PRESSED = DualsenseController.SenseButton.TOUCHBTN.isPressed();
      } else {
         SQUARE_PRESSED = false;
         CROSS_PRESSED = false;
         CIRCLE_PRESSED = false;
         TRIANGLE_PRESSED = false;
         UP_PRESSED = false;
         DOWN_PRESSED = false;
         LEFT_PRESSED = false;
         RIGHT_PRESSED = false;
         L1_PRESSED = false;
         R1_PRESSED = false;
         L2_PRESSED = false;
         R2_PRESSED = false;
         L3_PRESSED = false;
         R3_PRESSED = false;
         SHARE_PRESSED = false;
         OPTIONS_PRESSED = false;
         PS_PRESSED = false;
         MUTE_PRESSED = false;
         TOUCHBTN_PRESSED = false;
         TOUCH_PRESSED = false;
      }
   }

   public static boolean[] getWasdInputFromSenseAxis(boolean gamepadsLoaded) {
      if (!gamepadsLoaded) {
         return new boolean[4];
      } else {
         int maxTickInterval = 5;
         long updateMS = 24L;
         String moveMode = DualsenseSettings.moveStickMode();
         switch (moveMode) {
            case "HertzWasd":
               maxTickInterval = 5;
               updateMS = 24L;
               break;
            case "Wasd":
               maxTickInterval = 1;
               updateMS = 24L;
               break;
            case "HertzWasd&Jump":
               maxTickInterval = 5;
               updateMS = 24L;
               break;
            case "Wasd&Jump":
               maxTickInterval = 1;
               updateMS = 24L;
         }

         boolean[] wasd = new boolean[4];
         float[] inputValues = DualsenseController.SenseAxis.LEFT_STICK.getInputValues();
         float xInput = inputValues[0];
         float yInput = inputValues[1];
         long ticksRealTime = System.currentTimeMillis() / updateMS;
         float inputThreshold = 0.175F;
         if (yInput != 0.0F) {
            int tickedMin = (int)(Math.abs(yInput) * (float)(maxTickInterval - 1));
            boolean newInput = ticksRealTime % (long)maxTickInterval <= (long)(tickedMin + 1);
            if (newInput) {
               if (yInput + Math.abs(xInput) * inputThreshold > inputThreshold) {
                  wasd[0] = true;
               } else if (yInput + Math.abs(xInput) * inputThreshold < -inputThreshold) {
                  wasd[2] = true;
               }
            }
         }

         if (xInput != 0.0F) {
            int tickedMin = (int)(Math.abs(xInput) * (float)(maxTickInterval - 1));
            boolean newInput = ticksRealTime % (long)maxTickInterval <= (long)(tickedMin + 1);
            if (newInput) {
               if (xInput + Math.abs(yInput) * inputThreshold < -inputThreshold) {
                  wasd[1] = true;
               } else if (xInput + Math.abs(yInput) * inputThreshold > inputThreshold) {
                  wasd[3] = true;
               }
            }
         }

         return wasd;
      }
   }

   public static void testFeature() {
   }

   private static int btnCol(DualsenseController.SenseButton button) {
      int color = !button.isPressedAnyway() ? ColorUtils.getColor(55, 75) : ColorUtils.getColor(180, 255, 255, 90);
      if (CONTROLLER_BIND_SETTINGS_OPENED) {
         for (DualsenseController.SenseButton buttonFromCombo : DualsenseSettings.bindsSettingsModeComboActivator()) {
            if (buttonFromCombo == button) {
               int secColor = ColorUtils.getColor(255, 160, 40, ColorUtils.getAlphaFromColor(color));
               float blinkAPC = MathUtils.valWave01((float)(System.currentTimeMillis() % 600L) / 600.0F);
               if (blinkAPC > 0.5F) {
                  blinkAPC = 1.0F;
               } else {
                  blinkAPC *= 2.0F;
               }

               color = ColorUtils.getOverallColorFrom(color, secColor, blinkAPC * blinkAPC);
               break;
            }
         }
      } else if (button == DualsenseSettings.bindsSettingsModeComboActivator()[1] && DualsenseSettings.bindsSettingsModeComboActivator()[0].isPressedAnyway()) {
         color = ColorUtils.getOverallColorFrom(
            color, -1, (float)MathUtils.easeInOutQuadWave((double)((float)(System.currentTimeMillis() % 600L) / 600.0F)) * 0.3F
         );
      }

      if (!CONTROLLER_USED) {
         color = ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) * 0.5F);
      }

      return color;
   }

   private static int axisCol(DualsenseController.SenseAxis axis) {
      int color = ColorUtils.getOverallColorFrom(
         ColorUtils.getColor(55, 75),
         ColorUtils.getColor(180, 255, 255, 90),
         axis == DualsenseController.SenseAxis.TOUCHPAD ? 1.0F : MathUtils.clamp((float)axis.getAxisMove(axis.getInputValues()), 0.0F, 1.0F)
      );
      if (!CONTROLLER_USED) {
         color = ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) * 0.5F);
      }

      return color;
   }

   private static void drawTexture(ResourceLocation resourceLocation, float x, float y, float x2, float y2, int color) {
      drawTexture(resourceLocation, x, y, x2, y2, color, true);
   }

   private static void drawTexture(ResourceLocation resourceLocation, float x, float y, float x2, float y2, int color, boolean bloom) {
      if (resourceLocation != null) {
         mc.getTextureManager().bindTexture(resourceLocation);
         float scaleX = x2 - x;
         float scaleY = y2 - y;
         float scaleDel = 4.25F;
         x += scaleX / scaleDel;
         x2 -= scaleX / scaleDel;
         y += scaleY / scaleDel;
         y2 -= scaleY / scaleDel;
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         RenderUtils.buffer.pos((double)x2, (double)y).tex(1.0, 0.0).color(color).endVertex();
         RenderUtils.buffer.pos((double)x, (double)y).tex(0.0, 0.0).color(color).endVertex();
         RenderUtils.buffer.pos((double)x, (double)y2).tex(0.0, 1.0).color(color).endVertex();
         RenderUtils.buffer.pos((double)x2, (double)y2).tex(1.0, 1.0).color(color).endVertex();
         GL11.glEnable(3042);
         GL11.glEnable(3553);
         GL11.glAlphaFunc(516, 0.003921569F);
         GL11.glBlendFunc(770, bloom ? 1 : 771);
         GL11.glDisable(2929);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10241, 9729);
         RenderUtils.tessellator.draw();
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         GL11.glEnable(2929);
         if (bloom) {
            GL11.glBlendFunc(770, 771);
         }

         GL11.glAlphaFunc(516, 0.1F);
      }
   }

   public static void drawInputShape(float x, float y, float x2, float y2, DualsenseController.SenseButton button) {
      boolean hasCurrentAxis = button == DualsenseController.SenseButton.L2 || button == DualsenseController.SenseButton.R2;
      int color = btnCol(button);
      float roundMax = (y2 - y > x2 - x ? x2 - x : y2 - y) / 2.0F;
      boolean isQuad = Math.ceil((double)(x2 - x)) == Math.ceil((double)(y2 - y));
      float round = isQuad ? roundMax : roundMax / 2.0F;
      boolean isStickButtons = button == DualsenseController.SenseButton.L3 || button == DualsenseController.SenseButton.R3;
      if (isStickButtons) {
         DualsenseController.SenseAxis axisAsButton = button == DualsenseController.SenseButton.L3
            ? DualsenseController.SenseAxis.LEFT_STICK
            : DualsenseController.SenseAxis.RIGHT_STICK;
         float[] inputs = axisAsButton.getInputValues();
         float value = MathUtils.clamp((float)axisAsButton.getAxisMove(inputs), 0.0F, 1.0F);
         float stickScale = Math.max(x2 - x, y2 - y);
         float moveMul = 0.3333F;
         float moveX = inputs[0] * stickScale * moveMul;
         float moveY = -inputs[1] * stickScale * moveMul;
         x += moveX;
         y += moveY;
         x2 += moveX;
         y2 += moveY;
      }

      if (!hasCurrentAxis) {
         float bloomAPC = button.isPressedAnyway() ? 1.0F : 0.0F;
         float shadowSizeMul = 1.0F;
         boolean isTouchPad = button == DualsenseController.SenseButton.TOUCH || button == DualsenseController.SenseButton.TOUCHBTN;
         if (isTouchPad) {
            bloomAPC *= 0.5F;
            shadowSizeMul *= 0.666666F;
            if (button == DualsenseController.SenseButton.TOUCH) {
               int fillCol = ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) * 0.15F);
               float growPix = 2.0F;
               x += growPix;
               y += growPix;
               x2 -= growPix;
               y2 -= growPix;
               roundMax = (y2 - y > x2 - x ? x2 - x : y2 - y) / 2.0F;
               round = isQuad ? roundMax : roundMax / 2.0F;
               RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBoolShadowsBoolChangeShadowSize(
                  x, y, x2, y2, Math.max(round, 1.0F) / 1.5F, Math.max(round, 1.0F) / 3.0F, 0.5F, fillCol, fillCol, fillCol, fillCol, false, true, true
               );
               if (round == 0.0F) {
                  RenderUtils.drawRect((double)x, (double)y, (double)x2, (double)y2, fillCol);
               } else {
                  RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                     x, y, x2, y2, round, 0.5F, fillCol, fillCol, fillCol, fillCol, false, true, true
                  );
               }
            } else {
               RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBoolShadowsBoolChangeShadowSize(
                  x, y, x2, y2, Math.max(round, 1.0F) / 1.5F, Math.max(round, 1.0F) / 3.0F, 0.5F, color, color, color, color, false, true, true
               );
            }
         } else {
            int shadowCol = ColorUtils.getColor(0, 0, 0, (float)ColorUtils.getAlphaFromColor(color) * 0.5F);
            RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBoolShadowsBoolChangeShadowSize(
               x,
               y,
               x2,
               y2,
               Math.max(round, 1.0F) / 1.5F,
               Math.max(round, 1.0F) / 3.0F,
               Math.max(round, 1.0F),
               shadowCol,
               shadowCol,
               shadowCol,
               shadowCol,
               false,
               true,
               true
            );
            if (round == 0.0F) {
               RenderUtils.drawRect((double)x, (double)y, (double)x2, (double)y2, color);
            } else {
               RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                  x, y, x2, y2, round, 0.5F, color, color, color, color, false, true, true
               );
            }

            ResourceLocation texture = button.getTexture();
            if (texture != null && button != DualsenseController.SenseButton.MUTE) {
               drawTexture(texture, x, y, x2, y2, color);
            }
         }

         if (bloomAPC > 0.0F) {
            float w = x2 - x;
            float h = y2 - y;
            float cx = x + w / 2.0F;
            float cy = y + h / 2.0F;
            int bloomCol = ColorUtils.toDark(color, bloomAPC * 0.4F);
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               cx,
               cy,
               cx,
               cy,
               0.0F,
               (float)Math.sqrt((double)(w * w + h * h)) * 1.34F * shadowSizeMul,
               bloomCol,
               bloomCol,
               bloomCol,
               bloomCol,
               true,
               false,
               true
            );
         }
      }

      if ((CONTROLLER_BINDS_SYS_ACTIVE || CONTROLLER_BIND_SETTINGS_OPENED) && showBinds && gamepad != null) {
         List<DualsenseBindAction> buttonActions = gamepad.getBindHandler().getAllAsButton(button);
         if (!buttonActions.isEmpty()) {
            CFontRenderer bindFont = Fonts.mntsb_10;
            int textAlpha = ColorUtils.getAlphaFromColor(color);
            int bindTextIndex = 0;
            int buttonActionsCount = buttonActions.size();
            float stringDistanceAtButton = 1.0F;

            for (DualsenseBindAction buttonAction : buttonActions) {
               String displayBind = "unnamed";
               int bindColor = ColorUtils.getColor(130, textAlpha);
               Module tryGetModule = buttonAction.tryGetModule();
               if (tryGetModule != null) {
                  displayBind = tryGetModule.getName();
                  bindColor = tryGetModule.isActived() ? ColorUtils.getColor(60, 255, 60, textAlpha) : ColorUtils.getColor(255, 60, 60, textAlpha);
               }

               Macros tryGetMacros = buttonAction.tryGetMacros();
               if (tryGetMacros != null) {
                  displayBind = "Macro-" + tryGetMacros.getName();
               }

               float textX = 0.0F;
               float textY = 0.0F;
               float strW = bindFont.getStringWidth(displayBind);
               float strH = bindFont.getHeight();
               String side = "center";
               switch (button) {
                  case SQUARE:
                     side = "left";
                     break;
                  case CROSS:
                     side = "down";
                     break;
                  case CIRCLE:
                     side = "right";
                     break;
                  case TRIANGLE:
                     side = "up";
                     break;
                  case L1:
                     side = "center";
                     break;
                  case R1:
                     side = "center";
                     break;
                  case L2:
                     side = "center";
                     break;
                  case R2:
                     side = "center";
                     break;
                  case L3:
                     side = "center";
                     break;
                  case R3:
                     side = "center";
                     break;
                  case UP:
                     side = "up";
                     break;
                  case LEFT:
                     side = "left";
                     break;
                  case RIGHT:
                     side = "right";
                     break;
                  case DOWN:
                     side = "down";
                     break;
                  case SHARE:
                     side = "down";
                     break;
                  case OPTIONS:
                     side = "down";
                     break;
                  case PS:
                     side = "up";
                     break;
                  case MUTE:
                     side = "down";
                     break;
                  case TOUCHBTN:
                     side = "up";
                     break;
                  case TOUCH:
                     side = "center";
               }

               switch (side) {
                  case "right":
                     textX = x2 + stringDistanceAtButton;
                     textY = y + (y2 - y) / 2.0F + (float)(-buttonActionsCount) * strH / 2.0F + (float)bindTextIndex * strH;
                     break;
                  case "left":
                     textX = x - strW - stringDistanceAtButton;
                     textY = y + (y2 - y) / 2.0F + (float)(-buttonActionsCount) * strH / 2.0F + (float)bindTextIndex * strH;
                     break;
                  case "up":
                     textX = x + (x2 - x) / 2.0F - strW / 2.0F;
                     textY = y - strH - stringDistanceAtButton - (float)buttonActionsCount * strH + (float)bindTextIndex * strH;
                     break;
                  case "down":
                     textX = x + (x2 - x) / 2.0F - strW / 2.0F;
                     textY = y2 + stringDistanceAtButton + (float)bindTextIndex * strH;
                     break;
                  case "center":
                     textX = x + (x2 - x) / 2.0F - strW / 2.0F;
                     textY = y + (y2 - y) / 2.0F - (float)buttonActionsCount * strH / 2.0F + (float)bindTextIndex * strH;
               }

               bindFont.drawStringWithShadow(displayBind, textX, textY + 1.0F, bindColor);
               bindTextIndex++;
            }
         }
      }
   }

   public static void drawInputShape(float x, float y, float x2, float y2, DualsenseController.SenseAxis axis) {
      int color = axisCol(axis);
      boolean isStick = axis == DualsenseController.SenseAxis.LEFT_STICK || axis == DualsenseController.SenseAxis.RIGHT_STICK;
      boolean isTrigger = axis == DualsenseController.SenseAxis.LEFT_TRIGGER || axis == DualsenseController.SenseAxis.RIGHT_TRIGGER;
      boolean isTouchPad = axis == DualsenseController.SenseAxis.TOUCHPAD;
      float roundMax = isStick ? (y2 - y > x2 - x ? x2 - x : y2 - y) / 2.0F : (isTrigger ? (y2 - y) / 4.0F : 0.0F);
      float[] inputs = axis.getInputValues();
      float value = MathUtils.clamp((float)axis.getAxisMove(inputs), 0.0F, 1.0F);
      if (!isTouchPad) {
         int shadowCol = ColorUtils.getColor(0, 0, 0, (float)ColorUtils.getAlphaFromColor(color) * 0.5F);
         RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBoolShadowsBoolChangeShadowSize(
            x,
            y,
            x2,
            y2,
            Math.max(roundMax, 1.0F) / 1.5F,
            Math.max(roundMax, 1.0F) / 3.0F,
            Math.max(roundMax, 1.0F),
            shadowCol,
            shadowCol,
            shadowCol,
            shadowCol,
            false,
            true,
            true
         );
      }

      if (isStick) {
         float stickScale = Math.max(x2 - x, y2 - y);
         float movableScaleMul = 0.66666F;
         float moveMul = 0.3333F;
         RenderUtils.drawSmoothCircle((double)(x + (x2 - x) / 2.0F), (double)(y + (y2 - y) / 2.0F), stickScale, ColorUtils.toDark(color, 0.5F));
         float moveX = inputs[0] * stickScale * moveMul;
         float moveY = -inputs[1] * stickScale * moveMul;
         RenderUtils.drawSmoothCircle((double)(x + (x2 - x) / 2.0F + moveX), (double)(y + (y2 - y) / 2.0F + moveY), stickScale * movableScaleMul, color);
      } else if (isTrigger) {
         float y3 = y + (y2 - y) * (1.0F - value);
         float darkBaseMul = 0.5F;
         int darkColor = ColorUtils.toDark(color, darkBaseMul);
         if (roundMax == 0.0F) {
            RenderUtils.drawRect((double)x, (double)y, (double)x2, (double)y2, ColorUtils.toDark(color, 0.5F));
            if (y3 < y2) {
               RenderUtils.drawRect((double)x, (double)y3, (double)x2, (double)y2, color);
            }
         } else {
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               x, y, x2, y2, roundMax, 0.5F, darkColor, darkColor, darkColor, darkColor, false, true, true
            );
            if (y3 < y2) {
               RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                  x, y3, x2, y2, Math.min(roundMax, (y2 - y3) / 2.0F), 0.5F, color, color, color, color, false, true, true
               );
            }
         }
      } else {
         if (isTouchPad) {
            if (DualsenseController.SenseButton.TOUCH.isPressedAnyway()) {
               value = (float)Math.abs(axis.getAxisMove(new float[]{inputs[0] - 0.5F, inputs[1] - 0.5F}) * 2.0);
               float pointerScaleMul = 0.1F;
               float pointerScale = (float)Math.sqrt((double)((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y))) * pointerScaleMul;
               float xF = MathUtil.lerp(x, x2 - pointerScale, inputs[0]);
               float yF = MathUtil.lerp(y, y2 - pointerScale, inputs[1]);
               float xS = xF + pointerScale;
               float yS = yF + pointerScale;
               float xMid = xF + (xS - xF) / 2.0F;
               float yMid = yF + (yS - yF) / 2.0F;
               RenderUtils.drawSmoothCircle((double)xMid, (double)yMid, Math.max(pointerScale / 2.0F - 0.5F, 0.25F), color);
               float bloomAPC = 0.5F + value / 2.0F;
               if (bloomAPC > 0.0F) {
                  int bloomCol = ColorUtils.toDark(color, bloomAPC * 0.4F);
                  RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                     xMid, yMid, xMid, yMid, 0.0F, pointerScale * 1.34F, bloomCol, bloomCol, bloomCol, bloomCol, true, false, true
                  );
               }
            }

            return;
         }

         if (roundMax == 0.0F) {
            RenderUtils.drawRect((double)x, (double)y, (double)x2, (double)y2, color);
         } else {
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               x, y, x2, y2, roundMax, 0.5F, color, color, color, color, false, true, true
            );
         }
      }

      ResourceLocation texture = axis.getTexture();
      if (texture != null) {
         if (isStick) {
            float stickScale = Math.max(x2 - x, y2 - y);
            float moveMul = 0.3333F;
            int darkColor = ColorUtils.toDark(color, value * 0.25F);
            float moveX = inputs[0] * stickScale * moveMul;
            float moveY = -inputs[1] * stickScale * moveMul;
            drawTexture(texture, x + moveX, y + moveY, x + moveX + stickScale, y + moveY + stickScale, darkColor);
         } else {
            drawTexture(texture, x, y, x2, y2, color);
         }
      }

      if (value > 0.0F) {
         float w = x2 - x;
         float h = y2 - y;
         float cx = x + w / 2.0F;
         float cy = y + h / 2.0F;
         int bloomCol = ColorUtils.toDark(color, value * 0.4F);
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            cx, cy, cx, cy, 0.0F, (float)Math.sqrt((double)(w * w + h * h)) * 1.34F, bloomCol, bloomCol, bloomCol, bloomCol, true, false, true
         );
      }
   }

   private static boolean moveSelectorIfIn(int moveIndex) {
      if (inTheCurrentSelector) {
         return false;
      } else {
         if (moveIndex > 0) {
            currentIndexSelector++;
            if (currentIndexSelector > listSelectors.size() - 1) {
               currentIndexSelector = 0;
            }
         } else if (moveIndex < 0) {
            currentIndexSelector--;
            if (currentIndexSelector < 0) {
               currentIndexSelector = listSelectors.size() - 1;
            }
         }

         return true;
      }
   }

   private static boolean interCurrentSelector(int maxLinesHeightIndexes) {
      if (inTheCurrentSelector) {
         return false;
      } else {
         String currentSelector = listSelectors.get(currentIndexSelector % listSelectors.size());
         listContentsCurrentSelector.clear();
         if (currentSelector == null) {
            return false;
         } else {
            if (currentSelector.startsWith("Module")) {
               for (Module.Category category : Module.Category.values()) {
                  List<String> sortedModulesListPartNames = new ArrayList<>();

                  for (Module module : Client.moduleManager.getModuleList()) {
                     if (module.category == category && !module.isLocked() && module.isVisible()) {
                        sortedModulesListPartNames.add(module.getName());
                     }
                  }

                  listContentsCurrentSelector.addAll(sortedModulesListPartNames);
               }

               inTheCurrentSelector = true;
               currentIndexContent = Math.min(maxLinesHeightIndexes / 2, listContentsCurrentSelector.size() - 1);
               currentScrollBackContent = 0;
               currentAnimationScrollBackContent.to = 0.0F;
               currentAnimationScrollBackContent.setAnim(0.0F);
            } else if (currentSelector.startsWith("Macros") && !Client.macrosManager.getMacrosList().isEmpty()) {
               for (Macros macros : Client.macrosManager.getMacrosList()) {
                  if (!macros.getName().isEmpty()) {
                     listContentsCurrentSelector.add(macros.getName());
                  }
               }

               inTheCurrentSelector = true;
               currentIndexContent = Math.min(maxLinesHeightIndexes / 2, listContentsCurrentSelector.size() - 1);
               currentScrollBackContent = 0;
               currentAnimationScrollBackContent.to = 0.0F;
               currentAnimationScrollBackContent.setAnim(0.0F);
            } else if (currentSelector.startsWith("Show")) {
               showBinds = !showBinds;
            }

            return true;
         }
      }
   }

   private static boolean exitCurrentSelector() {
      if (!inTheCurrentSelector) {
         return false;
      } else {
         currentIndexContent = 0;
         currentScrollBackContent = 0;
         currentAnimationScrollBackContent.to = 0.0F;
         currentAnimationScrollBackContent.setAnim(0.0F);
         listContentsCurrentSelector.clear();
         inTheCurrentSelector = false;
         return true;
      }
   }

   private static boolean isInCurrentContent() {
      return inTheCurrentSelector;
   }

   private static boolean moveContentsIfIn(int moveIndex, int maxLinesHeightIndexes) {
      if (!inTheCurrentSelector) {
         return false;
      } else {
         if (moveIndex > 0) {
            currentIndexContent += moveIndex;
            if (currentIndexContent - currentScrollBackContent >= maxLinesHeightIndexes) {
               currentScrollBackContent = currentIndexContent - maxLinesHeightIndexes + 1;
            }

            if (currentIndexContent > listContentsCurrentSelector.size() - 1) {
               currentIndexContent = 0;
               currentScrollBackContent = 0;
            }

            if (currentScrollBackContent > listContentsCurrentSelector.size() - 1) {
               currentScrollBackContent = listContentsCurrentSelector.size() - 1;
            }
         } else if (moveIndex < 0) {
            currentIndexContent += moveIndex;
            if (currentIndexContent < currentScrollBackContent) {
               currentScrollBackContent = currentIndexContent;
            }

            if (currentIndexContent < 0) {
               currentIndexContent = listContentsCurrentSelector.size() - 1;
               currentScrollBackContent = currentIndexContent - maxLinesHeightIndexes + 1;
            }
         }

         if (listContentsCurrentSelector.size() < maxLinesHeightIndexes) {
            currentScrollBackContent = 0;
         }

         currentAnimationScrollBackContent.to = (float)currentScrollBackContent;
         return true;
      }
   }

   private static boolean touchCurrentContent(DualsenseController.SenseButton buttonSelectToBind) {
      if (inTheCurrentSelector && !listContentsCurrentSelector.isEmpty() && gamepad != null && buttonSelectToBind != DualsenseSettings.bindsTriggerActivator()) {
         boolean isBinding = DualsenseSettings.bindsTriggerActivator().isPressedAnyway();
         boolean isToggling = buttonSelectToBind == DualsenseSettings.bindingSelectorInterOrSelectButton();
         String currentContent = listContentsCurrentSelector.get(currentIndexContent % listContentsCurrentSelector.size());
         Module tryFindModule = Client.moduleManager.getModule(currentContent);
         if (tryFindModule != null) {
            if (isBinding) {
               gamepad.getBindHandler().toggleBindToList(buttonSelectToBind, tryFindModule);
            } else if (isToggling) {
               tryFindModule.toggle();
            }
         } else {
            Macros tryFindMacros = Client.macrosManager
               .getMacrosList()
               .stream()
               .filter(macros -> macros.getName().equalsIgnoreCase(currentContent))
               .findAny()
               .orElse(null);
            if (tryFindMacros != null && isBinding) {
               gamepad.getBindHandler().toggleBindToList(buttonSelectToBind, tryFindMacros);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static DualsenseController.SenseButton getContentGamepadAnyBind(int indexContent) {
      if (gamepad != null && !listContentsCurrentSelector.isEmpty()) {
         String content = listContentsCurrentSelector.get(indexContent % listContentsCurrentSelector.size());
         if (content == null) {
            return null;
         } else {
            Module tryFindModule = Client.moduleManager.getModule(content);
            if (tryFindModule != null) {
               return gamepad.getBindHandler().getDualsenseBindButton(tryFindModule);
            } else {
               Macros tryFindMacros = Client.macrosManager
                  .getMacrosList()
                  .stream()
                  .filter(macros -> macros.getName().equalsIgnoreCase(content))
                  .findAny()
                  .orElse(null);
               return tryFindMacros != null ? gamepad.getBindHandler().getDualsenseBindButton(tryFindMacros) : null;
            }
         }
      } else {
         return null;
      }
   }

   private static int maxLinesContentHeight() {
      return 6;
   }

   private static float lineContentsHeight() {
      return isInCurrentContent() ? 16.0F : 24.0F;
   }

   private static void updateBindingSelectorsOnTypingButtons(DualsenseController.SenseButton lastPressedButton, boolean isPushing) {
      if (checkComboPressedAnyway(DualsenseSettings.bindsSettingsModeComboActivator(), lastPressedButton, isPushing)) {
         CONTROLLER_BIND_SETTINGS_OPENED = !CONTROLLER_BIND_SETTINGS_OPENED;
         if (ClientTune.get != null) {
            ClientTune.get.playDualsenseSong((CONTROLLER_BIND_SETTINGS_OPENED ? "open" : "close") + "settings");
         }
      } else {
         if (CONTROLLER_BIND_SETTINGS_OPENED) {
            if (isPushing) {
               if (!isInCurrentContent() && lastPressedButton == DualsenseSettings.bindingSelectorExitButton()) {
                  exitCurrentSelector();
                  if (ClientTune.get != null) {
                     ClientTune.get.playDualsenseSong("closesettings");
                  }

                  CONTROLLER_BIND_SETTINGS_OPENED = false;
               } else if (lastPressedButton == DualsenseSettings.bindingSelectorInterOrSelectButton() && !isInCurrentContent()) {
                  if (interCurrentSelector(maxLinesContentHeight()) && ClientTune.get != null) {
                     ClientTune.get.playDualsenseSong("clicksettings");
                  }
               } else if (lastPressedButton != DualsenseSettings.bindingSelectorExitButton()
                  || isInCurrentContent() && DualsenseSettings.bindsTriggerActivator().isPressedAnyway()) {
                  if ((lastPressedButton == DualsenseController.SenseButton.UP || lastPressedButton == DualsenseController.SenseButton.DOWN)
                     && !DualsenseSettings.bindsTriggerActivator().isPressedAnyway()) {
                     int moveIndex = lastPressedButton == DualsenseController.SenseButton.UP ? -1 : 1;
                     if (isInCurrentContent()) {
                        if (moveContentsIfIn(moveIndex, maxLinesContentHeight()) && ClientTune.get != null) {
                           ClientTune.get.playDualsenseSong("scrollsettings");
                        }
                     } else if (moveSelectorIfIn(moveIndex) && ClientTune.get != null) {
                        ClientTune.get.playDualsenseSong("scrollsettings");
                     }
                  } else if (isInCurrentContent()) {
                     touchCurrentContent(lastPressedButton);
                  }
               } else if (exitCurrentSelector() && ClientTune.get != null) {
                  ClientTune.get.playDualsenseSong("returnsettings");
               }
            }
         } else if (exitCurrentSelector() && ClientTune.get != null) {
            ClientTune.get.playDualsenseSong("closesettings");
         }
      }
   }

   private static float getBindingSelectorsSettingPanelHeight() {
      return CONTROLLER_BIND_SETTINGS_OPENED
         ? (
            isInCurrentContent()
               ? (float)Math.min(listContentsCurrentSelector.size(), maxLinesContentHeight()) * lineContentsHeight()
               : (float)listSelectors.size() * lineContentsHeight()
         )
         : 0.0F;
   }

   private static void drawBindingSelectorsSettingPanel(float x, float y, float x2) {
      if (CONTROLLER_BIND_SETTINGS_OPENED) {
         currentAnimationScrollBackContent.getAnim();
         float calcHeight = getBindingSelectorsSettingPanelHeight();
         int bgCol0 = ColorUtils.getColor(25);
         int bgCol1 = ColorUtils.getColor(10);
         if (!CONTROLLER_USED) {
            bgCol0 = ColorUtils.swapAlpha(bgCol0, (float)ColorUtils.getAlphaFromColor(bgCol0) * 0.25F);
            bgCol1 = ColorUtils.swapAlpha(bgCol1, (float)ColorUtils.getAlphaFromColor(bgCol1) * 0.25F);
         }

         RenderUtils.drawVGradientRect(x + 1.0F, y + 1.0F, x2 - 1.0F, y + calcHeight - 1.0F, bgCol0, bgCol1);
         RenderUtils.drawLightContureRectFullGradient(x + 0.5F, y + 0.5F, x2 - 0.5F, y + calcHeight - 0.5F, bgCol0, bgCol0, bgCol1, bgCol1, false);
         RenderUtils.drawLightContureRectFullGradient(x - 0.5F, y - 0.5F, x2 + 0.5F, y + calcHeight + 0.5F, bgCol0, bgCol0, bgCol1, bgCol1, false);
         float lineHeight = lineContentsHeight();
         float contentsDrawXY = 3.0F;
         boolean bindTriggerButtonPressed = DualsenseSettings.bindsTriggerActivator().isPressedAnyway();
         if (isInCurrentContent()) {
            float animPardonY = lineHeight / 3.0F;
            float tempYDraw = y - currentAnimationScrollBackContent.anim * lineHeight;
            CFontRenderer fontContent = Fonts.comfortaaBold_13;
            float textXExt = 2.5F;
            boolean isMacrosesList = listSelectors.get(currentIndexSelector).startsWith("Macro");

            for (int indexContent = 0; indexContent < listContentsCurrentSelector.size(); indexContent++) {
               float distanceEdgesY = tempYDraw < y
                  ? y - tempYDraw
                  : (tempYDraw + lineHeight > y + calcHeight ? tempYDraw + lineHeight - (y + calcHeight) : 0.0F);
               if (distanceEdgesY < animPardonY) {
                  x += distanceEdgesY * 2.0F;
                  String content0 = listContentsCurrentSelector.get(indexContent);
                  String content = content0;
                  if (isMacrosesList) {
                     Macros tryGetMacros = Client.macrosManager
                        .getMacrosList()
                        .stream()
                        .filter(macros -> macros.getName().equalsIgnoreCase(content0))
                        .findAny()
                        .orElse(null);
                     if (tryGetMacros != null) {
                        content = "Macros(" + tryGetMacros.getName() + ") sends: " + tryGetMacros.getMassage();
                        if (fontContent.getStringWidth(content + "888888") > x2 - x) {
                           content = content0;
                        }
                     }
                  }

                  boolean isCurrent = indexContent == currentIndexContent;
                  DualsenseController.SenseButton tryGetBind = getContentGamepadAnyBind(indexContent);
                  boolean binded = tryGetBind != null;
                  String bindString = isCurrent && bindTriggerButtonPressed
                     ? "["
                        + MathUtils.getStringPercent("...", Math.min((float)(System.currentTimeMillis() % 900L) / 900.0F * 1.666666F, 1.0F))
                        + (binded ? tryGetBind.getName() : "")
                        + "]"
                     : (binded ? "[" + tryGetBind.getName() + "]" : "[NONE]");
                  float apcMul = (1.0F - Math.min(distanceEdgesY / animPardonY * (distanceEdgesY / animPardonY), 1.0F))
                     * (isCurrent ? 1.0F : 0.5F)
                     * (CONTROLLER_USED ? 1.0F : 0.25F);
                  String displayStringContent = content + " " + bindString;
                  float strW = fontContent.getStringWidth(displayStringContent);
                  RenderUtils.drawLightContureRect(
                     (double)(x + contentsDrawXY),
                     (double)(tempYDraw + contentsDrawXY),
                     (double)(x + contentsDrawXY * 2.0F + strW + textXExt),
                     (double)(tempYDraw + lineHeight - contentsDrawXY),
                     ColorUtils.getColor(10, 10, 10, 255.0F * apcMul)
                  );
                  if (isCurrent) {
                     RenderUtils.drawAlphedRect(
                        (double)(x + contentsDrawXY),
                        (double)(tempYDraw + contentsDrawXY),
                        (double)(x + contentsDrawXY * 2.0F + strW + textXExt),
                        (double)(tempYDraw + lineHeight - contentsDrawXY),
                        ColorUtils.getColor(60, 60, 60, 100.0F * apcMul)
                     );
                  }

                  int textColor = binded ? ColorUtils.getColor(0, 255, 0, 255.0F * apcMul) : ColorUtils.getColor(255, 255, 255, 255.0F * apcMul);
                  fontContent.drawString(
                     displayStringContent, x + textXExt + contentsDrawXY, tempYDraw + lineHeight / 2.0F - fontContent.getHeight() / 2.0F, textColor
                  );
                  x -= distanceEdgesY * 2.0F;
               }

               tempYDraw += lineHeight;
            }
         } else {
            float tempYDraw = y;
            int indexDraw = 0;
            float textXExt = 2.5F;
            CFontRenderer fontSelector = Fonts.comfortaaBold_18;

            for (int indexSelector = 0; indexSelector < listSelectors.size(); indexSelector++) {
               String selector = listSelectors.get(indexSelector);
               boolean isCurrent = indexSelector == currentIndexSelector;
               float apcMul = (isCurrent ? 1.0F : 0.5F) * (CONTROLLER_USED ? 1.0F : 0.25F);
               if (selector.startsWith("Macro") && Client.macrosManager.getMacrosList().isEmpty()) {
                  selector = selector + " (empty)";
                  apcMul = 0.5F;
               } else if (selector.startsWith("Show")) {
                  selector = selector + (showBinds ? " enabled" : " disabled");
               }

               float strW = fontSelector.getStringWidth(selector);
               RenderUtils.drawLightContureRect(
                  (double)(x + contentsDrawXY),
                  (double)(tempYDraw + contentsDrawXY),
                  (double)(x + contentsDrawXY * 2.0F + strW + textXExt),
                  (double)(tempYDraw + lineHeight - contentsDrawXY),
                  ColorUtils.getColor(10, 10, 10, 255.0F * apcMul)
               );
               if (isCurrent) {
                  RenderUtils.drawAlphedRect(
                     (double)(x + contentsDrawXY),
                     (double)(tempYDraw + contentsDrawXY),
                     (double)(x + contentsDrawXY * 2.0F + strW + textXExt),
                     (double)(tempYDraw + lineHeight - contentsDrawXY),
                     ColorUtils.getColor(60, 60, 60, 100.0F * apcMul)
                  );
               }

               fontSelector.drawString(selector, x + textXExt + contentsDrawXY, tempYDraw + 8.5F, ColorUtils.getColor(255, 255, 255, 255.0F * apcMul));
               tempYDraw += lineHeight;
               indexDraw++;
            }
         }
      }
   }

   public static void insertUiRenderPanel(ScaledResolution scaledResolution) {
      if (!Panic.stop && CONTROLLER_LOADED) {
         String showPanelMode = DualsenseSettings.showPanelMode();
         boolean hidePanel = false;
         switch (showPanelMode) {
            case "Always":
               hidePanel = false;
               break;
            case "Bindings&Settings":
               hidePanel = !CONTROLLER_BINDS_SYS_ACTIVE && !CONTROLLER_BIND_SETTINGS_OPENED;
               break;
            case "SettingsOnly":
               hidePanel = !CONTROLLER_BIND_SETTINGS_OPENED;
               break;
            case "Never":
               hidePanel = true;
         }

         if (!hidePanel) {
            ScaledResolution.setTempStandardScale(
               () -> {
                  float w = 180.0F;
                  float h = 100.0F;
                  float x = (float)scaledResolution.getScaledWidth() / 2.0F - w / 2.0F;
                  float y = (float)scaledResolution.getScaledHeight() / 1.25F - h;
                  float x2 = x + w;
                  float y2 = y + h;
                  float extYSettings = 4.5F;
                  float moveGamepad = -getBindingSelectorsSettingPanelHeight() / 2.0F - extYSettings / 2.0F;
                  y += moveGamepad;
                  y2 += moveGamepad;
                  float offsetEdgeButtons = 10.0F;
                  float offsetMuteButton = 5.0F;
                  float defaultScaleButton = 12.0F;
                  float miniScaleButtonX = 6.0F;
                  float miniScaleButtonY = 12.0F;
                  float sticksScale = 18.0F;
                  float triggersScaleButtonX = 24.0F;
                  float triggersScaleButtonY = 12.0F;
                  float l1r1ScaleButtonX = 24.0F;
                  float l1r1ScaleButtonY = 6.0F;
                  float muteScaleButtonY = 2.0F;
                  int bgCol0 = ColorUtils.getColor(25);
                  int bgCol1 = ColorUtils.getColor(10);
                  if (!CONTROLLER_USED) {
                     bgCol0 = ColorUtils.swapAlpha(bgCol0, (float)ColorUtils.getAlphaFromColor(bgCol0) * 0.25F);
                     bgCol1 = ColorUtils.swapAlpha(bgCol1, (float)ColorUtils.getAlphaFromColor(bgCol1) * 0.25F);
                  }

                  RenderUtils.drawVGradientRect(x + 1.0F, y + 1.0F, x2 - 1.0F, y2 - 1.0F, bgCol0, bgCol1);
                  RenderUtils.drawLightContureRectFullGradient(x + 0.5F, y + 0.5F, x2 - 0.5F, y2 - 0.5F, bgCol0, bgCol0, bgCol1, bgCol1, false);
                  RenderUtils.drawLightContureRectFullGradient(x - 0.5F, y - 0.5F, x2 + 0.5F, y2 + 0.5F, bgCol0, bgCol0, bgCol1, bgCol1, false);
                  String title = "DualSense";
                  CFontRenderer font = Fonts.comfortaaBold_18;
                  float titleX = x + w / 2.0F - font.getStringWidth(title) / 2.0F;
                  float titleY = y + offsetEdgeButtons + 4.0F;
                  int textColor0 = ColorUtils.getColor(90, 90, 90, CONTROLLER_USED ? 255.0F : 30.0F);
                  int textColor1 = ColorUtils.getColor(200, 200, 200, CONTROLLER_USED ? 170.0F : 30.0F);
                  GL11.glAlphaFunc(516, 0.003921569F);
                  font.drawVGradientString(title, titleX, titleY, textColor0, textColor1);
                  GL11.glAlphaFunc(516, 0.1F);
                  float psX = x + w / 2.0F - defaultScaleButton / 2.0F;
                  float psY = y + h - defaultScaleButton - offsetEdgeButtons - muteScaleButtonY - offsetMuteButton;
                  drawInputShape(psX, psY, psX + defaultScaleButton, psY + defaultScaleButton, DualsenseController.SenseButton.PS);
                  float muteX = x + w / 2.0F - defaultScaleButton / 2.0F;
                  float muteY = y + h - muteScaleButtonY - offsetEdgeButtons;
                  drawInputShape(muteX, muteY, muteX + defaultScaleButton, muteY + muteScaleButtonY, DualsenseController.SenseButton.MUTE);
                  float hSplitLeftKeys = 1.75F;
                  float leftKeyX = x + offsetEdgeButtons;
                  float leftKeyY = y + h / hSplitLeftKeys - defaultScaleButton / 2.0F;
                  float upKeyX = x + offsetEdgeButtons + defaultScaleButton;
                  float upKeyY = y + h / hSplitLeftKeys - defaultScaleButton / 2.0F - defaultScaleButton;
                  float downKeyX = x + offsetEdgeButtons + defaultScaleButton;
                  float downKeyY = y + h / hSplitLeftKeys + defaultScaleButton / 2.0F;
                  float rightKeyX = x + offsetEdgeButtons + defaultScaleButton * 2.0F;
                  float rightKeyY = y + h / hSplitLeftKeys - defaultScaleButton / 2.0F;
                  drawInputShape(leftKeyX, leftKeyY, leftKeyX + defaultScaleButton, leftKeyY + defaultScaleButton, DualsenseController.SenseButton.LEFT);
                  drawInputShape(upKeyX, upKeyY, upKeyX + defaultScaleButton, upKeyY + defaultScaleButton, DualsenseController.SenseButton.UP);
                  drawInputShape(downKeyX, downKeyY, downKeyX + defaultScaleButton, downKeyY + defaultScaleButton, DualsenseController.SenseButton.DOWN);
                  drawInputShape(rightKeyX, rightKeyY, rightKeyX + defaultScaleButton, rightKeyY + defaultScaleButton, DualsenseController.SenseButton.RIGHT);
                  float hSplitRightKeys = 1.75F;
                  float squareKeyX = x + w - offsetEdgeButtons - defaultScaleButton * 3.0F;
                  float squareKeyY = y + h / hSplitRightKeys - defaultScaleButton / 2.0F;
                  float triangleKeyX = x + w - offsetEdgeButtons - defaultScaleButton * 2.0F;
                  float triangleKeyY = y + h / hSplitRightKeys - defaultScaleButton / 2.0F - defaultScaleButton;
                  float crossKeyX = x + w - offsetEdgeButtons - defaultScaleButton * 2.0F;
                  float crossKeyY = y + h / hSplitRightKeys - defaultScaleButton / 2.0F + defaultScaleButton;
                  float circleKeyX = x + w - offsetEdgeButtons - defaultScaleButton;
                  float circleKeyY = y + h / hSplitRightKeys - defaultScaleButton / 2.0F;
                  drawInputShape(
                     squareKeyX, squareKeyY, squareKeyX + defaultScaleButton, squareKeyY + defaultScaleButton, DualsenseController.SenseButton.SQUARE
                  );
                  drawInputShape(
                     triangleKeyX, triangleKeyY, triangleKeyX + defaultScaleButton, triangleKeyY + defaultScaleButton, DualsenseController.SenseButton.TRIANGLE
                  );
                  drawInputShape(crossKeyX, crossKeyY, crossKeyX + defaultScaleButton, crossKeyY + defaultScaleButton, DualsenseController.SenseButton.CROSS);
                  drawInputShape(
                     circleKeyX, circleKeyY, circleKeyX + defaultScaleButton, circleKeyY + defaultScaleButton, DualsenseController.SenseButton.CIRCLE
                  );
                  float xExtMulOfCenterShareAndOptionsKeys = 0.215F;
                  float shareKeyX = x + w / 2.0F - w * xExtMulOfCenterShareAndOptionsKeys - offsetEdgeButtons - miniScaleButtonX / 2.0F;
                  float shareKeyY = y + offsetEdgeButtons;
                  float optionsKeyX = x + w / 2.0F + w * xExtMulOfCenterShareAndOptionsKeys + offsetEdgeButtons - miniScaleButtonX / 2.0F;
                  float optionsKeyY = y + offsetEdgeButtons;
                  drawInputShape(shareKeyX, shareKeyY, shareKeyX + miniScaleButtonX, shareKeyY + miniScaleButtonY, DualsenseController.SenseButton.SHARE);
                  drawInputShape(
                     optionsKeyX, optionsKeyY, optionsKeyX + miniScaleButtonX, optionsKeyY + miniScaleButtonY, DualsenseController.SenseButton.OPTIONS
                  );
                  float sticksHExtMul = 0.2F;
                  float sticksVExtMul = 0.1F;
                  float leftStickX = x + w / 2.0F - w / 2.0F * sticksHExtMul - sticksScale;
                  float leftStickY = y + h - h * sticksVExtMul - offsetEdgeButtons - sticksScale;
                  float leftStickCX = leftStickX + sticksScale / 2.0F;
                  float leftStickCY = leftStickY + sticksScale / 2.0F;
                  float rightStickX = x + w / 2.0F + w / 2.0F * sticksHExtMul;
                  float rightStickY = y + h - h * sticksVExtMul - offsetEdgeButtons - sticksScale;
                  float rightStickCX = rightStickX + sticksScale / 2.0F;
                  float rightStickCY = rightStickY + sticksScale / 2.0F;
                  drawInputShape(leftStickX, leftStickY, leftStickX + sticksScale, leftStickY + sticksScale, DualsenseController.SenseAxis.LEFT_STICK);
                  drawInputShape(rightStickX, rightStickY, rightStickX + sticksScale, rightStickY + sticksScale, DualsenseController.SenseAxis.RIGHT_STICK);
                  float touchXHExtMul = 0.475F;
                  float touchYVExtMul1 = 0.075F;
                  float touchYVExtMul2 = 0.5F;
                  float touchX = x + w / 2.0F - w / 2.0F * touchXHExtMul;
                  float touchX2 = x + w / 2.0F + w / 2.0F * touchXHExtMul;
                  float touchY = y + h * touchYVExtMul1;
                  float touchY2 = y + h * touchYVExtMul2;
                  drawInputShape(touchX, touchY, touchX2, touchY2, DualsenseController.SenseButton.TOUCHBTN);
                  drawInputShape(touchX, touchY, touchX2, touchY2, DualsenseController.SenseButton.TOUCH);
                  drawInputShape(touchX, touchY, touchX2, touchY2, DualsenseController.SenseAxis.TOUCHPAD);
                  float leftStickKeyX = leftStickCX - sticksScale / 2.0F;
                  float leftStickKeyY = leftStickCY - sticksScale / 2.0F;
                  float rightStickKeyX = rightStickCX - sticksScale / 2.0F;
                  float rightStickKeyY = rightStickCY - sticksScale / 2.0F;
                  drawInputShape(leftStickKeyX, leftStickKeyY, leftStickKeyX + sticksScale, leftStickKeyY + sticksScale, DualsenseController.SenseButton.L3);
                  drawInputShape(rightStickKeyX, rightStickKeyY, rightStickKeyX + sticksScale, rightStickKeyY + sticksScale, DualsenseController.SenseButton.R3);
                  float yStepTriggersToL1R1 = 4.0F;
                  float leftTriggerX = x + offsetEdgeButtons;
                  float leftTriggerY = y + offsetEdgeButtons;
                  float rightTriggerX = x + w - offsetEdgeButtons - triggersScaleButtonX;
                  float rightTriggerY = y + offsetEdgeButtons;
                  float leftL1R1X = x + offsetEdgeButtons;
                  float leftL1R1Y = leftTriggerY + triggersScaleButtonY + yStepTriggersToL1R1;
                  float rightL1R1X = x + w - offsetEdgeButtons - l1r1ScaleButtonX;
                  float rightL1R1Y = rightTriggerY + triggersScaleButtonY + yStepTriggersToL1R1;
                  drawInputShape(
                     leftTriggerX,
                     leftTriggerY,
                     leftTriggerX + triggersScaleButtonX,
                     leftTriggerY + triggersScaleButtonY,
                     DualsenseController.SenseAxis.LEFT_TRIGGER
                  );
                  drawInputShape(
                     rightTriggerX,
                     rightTriggerY,
                     rightTriggerX + triggersScaleButtonX,
                     rightTriggerY + triggersScaleButtonY,
                     DualsenseController.SenseAxis.RIGHT_TRIGGER
                  );
                  drawInputShape(
                     leftTriggerX, leftTriggerY, leftTriggerX + triggersScaleButtonX, leftTriggerY + triggersScaleButtonY, DualsenseController.SenseButton.L2
                  );
                  drawInputShape(
                     rightTriggerX,
                     rightTriggerY,
                     rightTriggerX + triggersScaleButtonX,
                     rightTriggerY + triggersScaleButtonY,
                     DualsenseController.SenseButton.R2
                  );
                  drawInputShape(leftL1R1X, leftL1R1Y, leftL1R1X + l1r1ScaleButtonX, leftL1R1Y + l1r1ScaleButtonY, DualsenseController.SenseButton.L1);
                  drawInputShape(rightL1R1X, rightL1R1Y, rightL1R1X + l1r1ScaleButtonX, rightL1R1Y + l1r1ScaleButtonY, DualsenseController.SenseButton.R1);
                  drawBindingSelectorsSettingPanel(x, y + h + extYSettings / 2.0F, x + w);
               }
            );
         }
      }
   }

   public static enum SenseAxis {
      LEFT_STICK(0, "Left_stick"),
      RIGHT_STICK(1, "Right_stick"),
      LEFT_TRIGGER(2, "Left_trigger"),
      RIGHT_TRIGGER(3, "Right_trigger"),
      TOUCHPAD(4, "Touchpad");

      private final int id;
      private final String name;
      private final boolean isStick;
      private final boolean isTrigger;
      private final boolean isTouchpad;

      public boolean hasAxisMove() {
         float[] axisValues = this.getInputValues();
         return Math.sqrt((double)(axisValues[0] * axisValues[0] + axisValues[1] * axisValues[1])) != 0.0;
      }

      public boolean hasAxisMove(float[] axisValues) {
         return Math.sqrt((double)(axisValues[0] * axisValues[0] + axisValues[1] * axisValues[1])) != 0.0;
      }

      public double getAxisMove(float[] axisValues) {
         return Math.sqrt((double)(axisValues[0] * axisValues[0] + axisValues[1] * axisValues[1]));
      }

      public double getsAxisDir(float[] axisValues, float append) {
         return Math.atan2((double)(-axisValues[1]), (double)(-axisValues[0])) * 180.0 / Math.PI - 90.0 + (double)append;
      }

      public float[] getInputValues() {
         float x = 0.0F;
         float y = 0.0F;
         switch (this.id) {
            case 0:
               x = DualsenseCachedReader.connected ? DualsenseCachedReader.lstick.x : 0.0F;
               y = DualsenseCachedReader.connected ? -DualsenseCachedReader.lstick.y : 0.0F;
               break;
            case 1:
               x = DualsenseCachedReader.connected ? DualsenseCachedReader.rstick.x : 0.0F;
               y = DualsenseCachedReader.connected ? -DualsenseCachedReader.rstick.y : 0.0F;
               break;
            case 2:
               x = DualsenseCachedReader.connected ? DualsenseCachedReader.l2 : 0.0F;
               y = DualsenseCachedReader.connected ? DualsenseCachedReader.l2 : 0.0F;
               break;
            case 3:
               x = DualsenseCachedReader.connected ? DualsenseCachedReader.r2 : 0.0F;
               y = DualsenseCachedReader.connected ? DualsenseCachedReader.r2 : 0.0F;
               break;
            case 4:
               x = DualsenseCachedReader.connected && DualsenseCachedReader.touch ? DualsenseCachedReader.touchPos.x : 0.0F;
               y = DualsenseCachedReader.connected && DualsenseCachedReader.touch ? DualsenseCachedReader.touchPos.y : 0.0F;
         }

         float deadZoneIn = DualsenseSettings.inDeadZoneSticks();
         if (Math.abs(x) < deadZoneIn) {
            x = 0.0F;
         } else if (x > 0.0F) {
            x -= deadZoneIn;
            x = Math.max(x, 0.0F);
            x *= 1.0F / (1.0F - deadZoneIn);
         } else {
            x += deadZoneIn;
            x = Math.min(x, 0.0F);
            x *= 1.0F / (1.0F - deadZoneIn);
         }

         if (Math.abs(y) < deadZoneIn) {
            y = 0.0F;
         } else if (y > 0.0F) {
            y -= deadZoneIn;
            y = Math.max(y, 0.0F);
            y *= 1.0F / (1.0F - deadZoneIn);
         } else {
            y += deadZoneIn;
            y = Math.min(y, 0.0F);
            y *= 1.0F / (1.0F - deadZoneIn);
         }

         return new float[]{x, y};
      }

      private SenseAxis(int id, String name) {
         this.id = id;
         this.name = name;
         this.isStick = this.name.contains("stick");
         this.isTrigger = this.name.contains("trigger");
         this.isTouchpad = this.name.contains("touchpad");
      }

      public int getId() {
         return this.id;
      }

      public String getName() {
         return this.name;
      }

      public ResourceLocation getTexture() {
         String path = "vegaline/system/gamepad/dualsense/" + this.getName().toLowerCase().replace("_", "") + ".png";
         return DualsenseController.cachedTextures.get(path) == null
            ? DualsenseController.cachedTextures.put(path, new ResourceLocation(path))
            : DualsenseController.cachedTextures.get(path);
      }
   }

   public static enum SenseButton {
      SQUARE(0, "Square", () -> DualsenseCachedReader.connected && DualsenseCachedReader.square),
      CROSS(1, "Cross", () -> DualsenseCachedReader.connected && DualsenseCachedReader.cross),
      CIRCLE(2, "Circle", () -> DualsenseCachedReader.connected && DualsenseCachedReader.circle),
      TRIANGLE(3, "Triangle", () -> DualsenseCachedReader.connected && DualsenseCachedReader.triangle),
      L1(4, "L1", () -> DualsenseCachedReader.connected && DualsenseCachedReader.l1),
      R1(5, "R1", () -> DualsenseCachedReader.connected && DualsenseCachedReader.r1),
      L2(6, "L2", () -> DualsenseCachedReader.connected && DualsenseCachedReader.l2 > 0.5F),
      R2(7, "R2", () -> DualsenseCachedReader.connected && DualsenseCachedReader.r2 > 0.5F),
      L3(8, "L3", () -> DualsenseCachedReader.connected && DualsenseCachedReader.l3),
      R3(9, "R3", () -> DualsenseCachedReader.connected && DualsenseCachedReader.r3),
      UP(10, "Up", () -> DualsenseCachedReader.connected && DualsenseCachedReader.u),
      LEFT(11, "Left", () -> DualsenseCachedReader.connected && DualsenseCachedReader.l),
      RIGHT(12, "Right", () -> DualsenseCachedReader.connected && DualsenseCachedReader.r),
      DOWN(13, "Down", () -> DualsenseCachedReader.connected && DualsenseCachedReader.d),
      SHARE(14, "Share", () -> DualsenseCachedReader.connected && DualsenseCachedReader.create),
      OPTIONS(15, "Options", () -> DualsenseCachedReader.connected && DualsenseCachedReader.options),
      PS(16, "Ps", () -> DualsenseCachedReader.connected && DualsenseCachedReader.ps),
      MUTE(17, "Mute", () -> DualsenseCachedReader.connected && DualsenseCachedReader.mute),
      TOUCHBTN(18, "Touchbtn", () -> DualsenseCachedReader.connected && DualsenseCachedReader.touchBtn),
      TOUCH(19, "Touch", () -> DualsenseCachedReader.connected && DualsenseCachedReader.touch);

      private final int id;
      private final String name;
      private final Supplier<Boolean> buttonPressed;

      public int getId() {
         return this.id;
      }

      public String getName() {
         return this.name;
      }

      private SenseButton(int id, String name, Supplier<Boolean> buttonPressed) {
         this.id = id;
         this.name = name;
         this.buttonPressed = buttonPressed;
      }

      public boolean isPressedAnyway() {
         return this.buttonPressed.get();
      }

      public boolean isPressed() {
         boolean canPressed = !DualsenseController.CONTROLLER_BINDS_SYS_ACTIVE;
         if (!canPressed) {
            for (DualsenseController.SenseButton buttonWhichIgnoreBindsActive : DualsenseSettings.buttonsWhichIgnoreBindsActiveState()) {
               if (this == buttonWhichIgnoreBindsActive) {
                  canPressed = true;
                  break;
               }
            }
         }

         return this.buttonPressed.get() && canPressed && !DualsenseController.CONTROLLER_BIND_SETTINGS_OPENED;
      }

      public boolean isPressedForBind() {
         return this.buttonPressed.get() && DualsenseController.CONTROLLER_BINDS_SYS_ACTIVE;
      }

      public ResourceLocation getTexture() {
         String path = "vegaline/system/gamepad/dualsense/" + this.getName().toLowerCase().replace("_", "") + ".png";
         return DualsenseController.cachedTextures.get(path) == null
            ? DualsenseController.cachedTextures.put(path, new ResourceLocation(path))
            : DualsenseController.cachedTextures.get(path);
      }
   }

   public static class VirtualGamepad {
      private final DualsenseBindHandler bindHandler = new DualsenseBindHandler();
      private final boolean[] pressedButtons = new boolean[DualsenseController.SenseButton.values().length];
      private final float[][] movedAxises = new float[DualsenseController.SenseAxis.values().length][2];

      private boolean onUpdatePressButton(DualsenseController.SenseButton button, boolean pressed) {
         int index = button.getId();
         if (this.pressedButtons[index] != pressed) {
            this.pressedButtons[index] = pressed;
            DualsenseController.onPressSenseButtonEvent(button, pressed);
            return true;
         } else {
            return false;
         }
      }

      private boolean onUpdateMoveAxis(DualsenseController.SenseAxis axis, float[] values) {
         int index = axis.getId();
         boolean changedAny = false;
         if (this.movedAxises[index][0] != values[0] || this.movedAxises[index][1] != values[1]) {
            DualsenseController.onMoveSenseAxisEvent(axis, this.movedAxises[index], values);
         }

         if (this.movedAxises[index][0] != values[0]) {
            this.movedAxises[index][0] = values[0];
            changedAny = true;
         }

         if (this.movedAxises[index][1] != values[1]) {
            this.movedAxises[index][1] = values[1];
            changedAny = true;
         }

         return changedAny;
      }

      public void updateController() {
         boolean debug = false;

         for (DualsenseController.SenseAxis axis : DualsenseController.SenseAxis.values()) {
            float[] axisInputValues = axis.getInputValues();
            if (axis.hasAxisMove(axis.getInputValues())) {
               if (this.onUpdateMoveAxis(axis, axisInputValues) && debug) {
                  System.out.println("updated status axis " + axis.getName() + " to " + axisInputValues[0] + ", " + axisInputValues[1]);
               }
            } else if (this.onUpdateMoveAxis(axis, axisInputValues) && debug) {
               System.out.println("updated status axis " + axis.getName() + " to " + axisInputValues[0] + ", " + axisInputValues[1]);
            }
         }

         for (DualsenseController.SenseButton button : DualsenseController.SenseButton.values()) {
            if (button.isPressedAnyway()) {
               if (this.onUpdatePressButton(button, true) && debug) {
                  System.out.println("updated status button " + button.getName() + " to true");
               }
            } else if (this.onUpdatePressButton(button, false) && debug) {
               System.out.println("updated status button " + button.getName() + " to false");
            }
         }
      }

      public boolean isButtonPressed(DualsenseController.SenseButton button) {
         return button.isPressed();
      }

      public float[] getAxisValues(DualsenseController.SenseAxis axis) {
         return axis.getInputValues();
      }

      public boolean anyInputGamepad() {
         boolean anyInput = false;

         for (boolean pressed : this.pressedButtons) {
            if (pressed) {
               anyInput = true;
               break;
            }
         }

         if (!anyInput) {
            for (float[] axisInput : this.movedAxises) {
               if (Math.abs(axisInput[0]) >= DualsenseSettings.inDeadZoneSticks() || Math.abs(axisInput[1]) >= DualsenseSettings.inDeadZoneSticks()) {
                  anyInput = true;
                  break;
               }
            }
         }

         return anyInput;
      }

      public DualsenseBindHandler getBindHandler() {
         return this.bindHandler;
      }
   }
}
