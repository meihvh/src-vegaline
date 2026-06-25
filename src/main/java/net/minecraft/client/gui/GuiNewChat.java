package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import com.mojang.realmsclient.gui.ChatFormatting;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.govno.client.Client;
import ru.govno.client.module.modules.ChatHelper;
import ru.govno.client.module.modules.ClientColors;
import ru.govno.client.module.modules.ComfortUi;
import ru.govno.client.utils.Command.impl.Chat;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Render.StencilUtil;

public class GuiNewChat extends Gui {
   private static final Logger LOGGER = LogManager.getLogger();
   private final Minecraft mc;
   public static List<String> sentMessages = Lists.newArrayList();
   private final List<ChatLine> chatLines = Lists.newArrayList();
   private final List<ChatLine> drawnChatLines = Lists.newArrayList();
   private int scrollPos;
   AnimationUtils scroll = new AnimationUtils(0.0F, 0.0F, 0.1F);
   private boolean isScrolled;
   private String lastMessage;
   private int sameMessageAmount;
   private int line;

   public GuiNewChat(Minecraft mcIn) {
      this.mc = mcIn;
   }

   public void drawChat(int updateCounter) {
      this.scroll.to = (float)this.scrollPos + 0.001F;
      this.scroll.getAnim();
      boolean censure = ChatHelper.get.actived && ChatHelper.get.CensureText.getBool();
      boolean cutBg = ComfortUi.get.isCutChatMsgBg();
      List<ChatLine> drawnChatLines = Panic.stop
         ? this.drawnChatLines.stream().filter(chatLine -> chatLine != null && !chatLine.isClientMessage).collect(Collectors.toList())
         : this.drawnChatLines;
      double chatlineTicksVisible = ComfortUi.get.isFastChatMsgQuit() ? 30.0 : 200.0;
      if (!Panic.stop && ComfortUi.get.isChatAnimations()) {
         if (this.mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN) {
            int i = this.getLineCount();
            int j = drawnChatLines.size();
            float f = this.mc.gameSettings.chatOpacity * 0.9F + 0.1F;
            if (j > 0) {
               boolean flag = this.getChatOpen();
               float f1 = this.getChatScale();
               int k = MathHelper.ceil((float)this.getChatWidth() / f1);
               GlStateManager.pushMatrix();
               GlStateManager.translate(0.0F, 8.0F, 0.0F);
               GlStateManager.scale(f1, f1, 1.0F);
               int l = 0;

               try {
                  boolean stencil = MathUtils.getDifferenceOf(this.scroll.anim, this.scroll.to) > 0.013888889F;
                  if (stencil) {
                     StencilUtil.initStencilToWrite();
                     RenderUtils.drawRect(0.0, (double)(-(this.getChatOpen() ? 190 : 100)), (double)(k + 4), 0.0, -1);
                     StencilUtil.readStencilBuffer(1);
                  }

                  for (int i1 = (int)(this.getChatOpen() ? (float)i + this.scroll.anim - 20.0F : 0.0F); (float)i1 < (float)i + this.scroll.anim; i1++) {
                     ChatLine chatline = drawnChatLines.get(i1);
                     if (chatline != null && (!Panic.stop || !chatline.isClientMessage)) {
                        int j1 = updateCounter - chatline.getUpdatedCounter();
                        if ((double)j1 < chatlineTicksVisible || flag) {
                           double d0 = (double)j1 / chatlineTicksVisible;
                           d0 = 1.0 - d0;
                           d0 *= 10.0;
                           d0 = MathHelper.clamp(d0, 0.0, 1.0);
                           d0 *= d0;
                           int l1 = (int)(255.0 * d0);
                           if (flag) {
                              l1 = 255;
                           }

                           l1 = (int)((float)l1 * f);
                           int j2 = -i1 * 9;
                           String s = chatline.getChatString();
                           s = this.reString(censure, s);
                           chatline.anim.to = (float)this.mc.fontRendererObj.getStringWidth(s);
                           if (chatline.anim.to != chatline.anim.anim) {
                              if (s.contains(" <") && s.contains("x") && s.contains(">")) {
                                 chatline.anim.setAnim(chatline.anim.to);
                              } else if ((double)MathUtils.getDifferenceOf(chatline.anim.anim, chatline.anim.to) < 0.5) {
                                 chatline.anim.setAnim(chatline.anim.to);
                              }

                              chatline.anim.getAnim();
                           }

                           float aPC = chatline.anim.anim / chatline.anim.to;
                           l1 = (int)((float)l1 * aPC);
                           float xTrans = -chatline.anim.to + chatline.anim.anim;
                           float xTrans2 = cutBg ? 2.0F + xTrans : (float)(k + 4);
                           float yTrans = (float)j2 - 9.0F + this.scroll.anim * 9.0F;
                           int color = l1 / 4 << 24;
                           String unformatted = chatline.getChatComponent().getUnformattedText();
                           if (ChatHelper.get.highlightFriends(unformatted)) {
                              int var91 = ColorUtils.getColor(60, 210, 60, 125);
                              RenderUtils.drawAlphedSideways(
                                 -0.5,
                                 (double)yTrans,
                                 cutBg ? (double)(xTrans2 + (float)this.mc.fontRendererObj.getStringWidth(s)) : (double)(k + 4),
                                 (double)(yTrans + 9.0F),
                                 var91,
                                 color,
                                 false
                              );
                           } else if (ChatHelper.get.highlightSelf(unformatted)) {
                              int var90 = ColorUtils.getColor(125, 125, 125, 125);
                              RenderUtils.drawAlphedSideways(
                                 -0.5,
                                 (double)yTrans,
                                 cutBg ? (double)(xTrans2 + (float)this.mc.fontRendererObj.getStringWidth(s)) : (double)(k + 4),
                                 (double)(yTrans + 9.0F),
                                 var90,
                                 color,
                                 false
                              );
                           } else if (!Panic.stop && chatline.isClientMessage) {
                              int alpha = ColorUtils.getAlphaFromColor(color);
                              int var89 = ColorUtils.swapAlpha(ClientColors.getColor1(j2 * 10, 1.0F), (float)alpha);
                              int color2 = ColorUtils.swapAlpha(ClientColors.getColor1((j2 - 10) * 10, 1.0F), (float)alpha);
                              int colorBase0 = ColorUtils.getOverallColorFrom(color, var89, 0.35F);
                              int colorBase2 = ColorUtils.getOverallColorFrom(color, color2, 0.35F);
                              float x0 = -0.5F;
                              float x2 = cutBg ? xTrans2 + (float)this.mc.fontRendererObj.getStringWidth(s) : (float)(k + 4);
                              float x1 = MathUtils.lerp(x0, x2, 0.5F);
                              RenderUtils.drawFullGradientRectPro(x0, yTrans, x1, yTrans + 9.0F, colorBase0, var89, color2, colorBase2, false);
                              RenderUtils.drawFullGradientRectPro(x1, yTrans, x2, yTrans + 9.0F, var89, colorBase0, colorBase2, color2, false);
                           } else {
                              RenderUtils.drawAlphedRect(
                                 -0.5,
                                 (double)yTrans,
                                 cutBg ? (double)(xTrans2 + (float)this.mc.fontRendererObj.getStringWidth(s)) : (double)(k + 4),
                                 (double)(yTrans + 9.0F),
                                 color
                              );
                           }

                           GlStateManager.disableDepth();
                           int c = 16777215 + (l1 << 24);
                           if (ColorUtils.getAlphaFromColor(c) < 26) {
                              c = ColorUtils.swapAlpha(c, 26.0F);
                           }

                           this.mc.fontRendererObj.drawStringWithShadow(s, 1.0F + xTrans, yTrans, c);
                           GlStateManager.enableBlend();
                           GlStateManager.enableAlpha();
                        }
                     }
                  }

                  if (stencil) {
                     StencilUtil.uninitStencilBuffer();
                  }
               } catch (Exception var37) {
               }

               if (flag) {
                  int k2 = this.mc.fontRendererObj.FONT_HEIGHT;
                  GlStateManager.translate(-3.0F, 0.0F, 0.0F);
                  int l2 = j * k2 + j;
                  int i3 = l * k2 + l;
                  int j3 = (int)(this.scroll.anim * (float)i3 / (float)j);
                  int k1 = i3 * i3 / l2;
                  if (l2 != i3) {
                     int k3 = j3 > 0 ? 170 : 96;
                     int l3 = this.isScrolled ? 13382451 : 3355562;
                     int c = ColorUtils.astolfoColorsCool(0, 0);
                     int c2 = ColorUtils.astolfoColorsCool(0, 200);
                     RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                        1.0F, (float)(-j3 - k1 / 2), 2.0F, (float)(-j3), 0.5F, 1.0F, c, c, c2, c2, false, true, true
                     );
                  }
               }

               GlStateManager.popMatrix();
            }
         }
      } else if (this.mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN) {
         int i = this.getLineCount();
         int j = drawnChatLines.size();
         float f = this.mc.gameSettings.chatOpacity * 0.9F + 0.1F;
         if (j > 0) {
            boolean flag = this.getChatOpen();
            float f1 = this.getChatScale();
            int k = MathHelper.ceil((float)this.getChatWidth() / f1);
            GlStateManager.pushMatrix();
            GlStateManager.translate(2.0F, 8.0F, 0.0F);
            GlStateManager.scale(f1, f1, 1.0F);
            int l = 0;

            for (int i1x = 0; i1x + this.scrollPos < drawnChatLines.size() && i1x < i; i1x++) {
               ChatLine chatline = drawnChatLines.get(i1x + this.scrollPos);
               if (chatline != null && (!Panic.stop || !chatline.isClientMessage)) {
                  int j1 = updateCounter - chatline.getUpdatedCounter();
                  if ((double)j1 < chatlineTicksVisible || flag) {
                     double d0x = (double)j1 / chatlineTicksVisible;
                     d0x = 1.0 - d0x;
                     d0x *= 10.0;
                     d0x = MathHelper.clamp(d0x, 0.0, 1.0);
                     d0x *= d0x;
                     int l1x = (int)(255.0 * d0x);
                     if (flag) {
                        l1x = 255;
                     }

                     l1x = (int)((float)l1x * f);
                     l++;
                     if (l1x > 3) {
                        int i2 = 0;
                        int j2x = -i1x * 9;
                        String sx = chatline.getChatString();
                        sx = this.reString(censure, sx);
                        int colorx = l1x / 4 << 24;
                        String unformattedx = chatline.getChatComponent().getUnformattedText();
                        if (ChatHelper.get.highlightFriends(unformattedx)) {
                           int var83 = ColorUtils.getColor(60, 210, 60, 125);
                           RenderUtils.drawAlphedSideways(
                              -0.5,
                              (double)(j2x - 9),
                              cutBg ? (double)(2 + this.mc.fontRendererObj.getStringWidth(sx)) : (double)(k + 4),
                              (double)j2x,
                              var83,
                              colorx,
                              false
                           );
                        } else if (ChatHelper.get.highlightSelf(unformattedx)) {
                           int var82 = ColorUtils.getColor(125, 125, 125, 125);
                           RenderUtils.drawAlphedSideways(
                              -0.5,
                              (double)(j2x - 9),
                              cutBg ? (double)(2 + this.mc.fontRendererObj.getStringWidth(sx)) : (double)(k + 4),
                              (double)j2x,
                              var82,
                              colorx,
                              false
                           );
                        } else if (!Panic.stop && chatline.isClientMessage) {
                           int alpha = ColorUtils.getAlphaFromColor(colorx);
                           int var81 = ColorUtils.swapAlpha(ClientColors.getColor1(j2x * 10, 1.0F), (float)alpha);
                           int color2 = ColorUtils.swapAlpha(ClientColors.getColor1((j2x - 10) * 10, 1.0F), (float)alpha);
                           int colorBase0 = ColorUtils.getOverallColorFrom(colorx, var81, 0.35F);
                           int colorBase2 = ColorUtils.getOverallColorFrom(colorx, color2, 0.35F);
                           float x0 = -0.5F;
                           float x2 = cutBg ? (float)(2 + this.mc.fontRendererObj.getStringWidth(sx)) : (float)(k + 4);
                           float x1 = MathUtils.lerp(x0, x2, 0.5F);
                           RenderUtils.drawFullGradientRectPro(x0, (float)(j2x - 9), x1, (float)j2x, colorBase0, var81, color2, colorBase2, false);
                           RenderUtils.drawFullGradientRectPro(x1, (float)(j2x - 9), x2, (float)j2x, var81, colorBase0, colorBase2, color2, false);
                        } else {
                           RenderUtils.drawAlphedRect(
                              -0.5, (double)(j2x - 9), cutBg ? (double)(2 + this.mc.fontRendererObj.getStringWidth(sx)) : (double)(k + 4), (double)j2x, colorx
                           );
                        }

                        GlStateManager.enableBlend();
                        this.mc.fontRendererObj.drawStringWithShadow(sx, 0.0F, (float)(-i1x * 9 - 8), 16777215 + (l1x << 24));
                        GlStateManager.disableAlpha();
                        GlStateManager.disableBlend();
                     }
                  }
               }
            }

            if (flag) {
               int k2 = this.mc.fontRendererObj.FONT_HEIGHT;
               GlStateManager.translate(-3.0F, 0.0F, 0.0F);
               int l2 = j * k2 + j;
               int i3 = l * k2 + l;
               int j3 = this.scrollPos * i3 / j;
               int k1 = i3 * i3 / l2;
               if (l2 != i3) {
                  int k3 = j3 > 0 ? 170 : 96;
                  int l3 = this.isScrolled ? 13382451 : 3355562;
                  drawRect(0, (double)(-j3), 2.0, (double)(-j3 - k1), l3 + (k3 << 24));
                  drawRect(2, (double)(-j3), 1.0, (double)(-j3 - k1), 13421772 + (k3 << 24));
               }
            }

            GlStateManager.popMatrix();
         }
      }
   }

   public void clearChatMessages(boolean p_146231_1_) {
      this.drawnChatLines.clear();
      this.chatLines.clear();
      sentMessages.clear();
   }

   private String fixString(String str) {
      str = str.replaceAll("\uf8ff", "");
      StringBuilder sb = new StringBuilder();

      for (char c : str.toCharArray()) {
         if (c > '！' && c < '｠') {
            sb.append(Character.toChars(c - 'ﻠ'));
         } else {
            sb.append(c);
         }
      }

      return sb.toString();
   }

   private boolean fixSpam() {
      return !Panic.stop && Client.moduleManager != null && ChatHelper.get.actived && ChatHelper.get.NoExtraCopy.getBool();
   }

   public void printChatMessage(ITextComponent chatComponent) {
      if (Panic.stop || !Chat.stringIsContainsBadMassage(chatComponent.getFormattedText())) {
         String text = this.fixString(chatComponent.getFormattedText());
         if (this.fixSpam()) {
            if (!chatComponent.getFormattedText().startsWith("|")) {
               if (text.equals(this.lastMessage)) {
                  Minecraft.getMinecraft().ingameGUI.getChatGUI().deleteChatLine(this.line);
                  this.sameMessageAmount++;
                  chatComponent.appendText(ChatFormatting.WHITE + " <" + ChatFormatting.GRAY + "x" + this.sameMessageAmount + ChatFormatting.WHITE + ">");
               } else {
                  this.sameMessageAmount = 1;
               }

               boolean infiniteLines = !Panic.stop && ChatHelper.get.isInfiniteChatHistory();
               this.lastMessage = text;
               this.line++;
               if (this.line > (infiniteLines ? 1280 : 256)) {
                  this.line = 0;
               }
            } else {
               this.lastMessage = text;
               this.line = 0;
            }

            this.printChatMessageWithOptionalDeletion(chatComponent, this.line);
         } else {
            this.printChatMessageWithOptionalDeletion(chatComponent, 0);
         }
      }
   }

   public void printChatMessageWithOptionalDeletion(ITextComponent chatComponent, int chatLineId) {
      this.setChatLine(chatComponent, chatLineId, this.mc.ingameGUI.getUpdateCounter(), false);
      LOGGER.info("[CHAT] {}", chatComponent.getUnformattedText().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n"));
   }

   private void setChatLine(ITextComponent chatComponent, int chatLineId, int updateCounter, boolean displayOnly) {
      if (chatLineId != 0) {
         this.deleteChatLine(chatLineId);
      }

      int i = MathHelper.floor((float)this.getChatWidth() / this.getChatScale());
      List<ITextComponent> list = GuiUtilRenderComponents.splitText(chatComponent, i, this.mc.fontRendererObj, false, false);
      boolean flag = this.getChatOpen();

      for (ITextComponent itextcomponent : list) {
         if (flag && this.scrollPos > 0) {
            this.isScrolled = true;
            this.scroll(1);
         }

         this.drawnChatLines.add(0, new ChatLine(updateCounter, itextcomponent, chatLineId));
      }

      int maxLines = (int)(!Panic.stop && ChatHelper.get.isInfiniteChatHistory() ? 896.0 : 100.0);

      while (this.drawnChatLines.size() > maxLines) {
         this.drawnChatLines.remove(this.drawnChatLines.size() - 1);
      }

      if (!displayOnly) {
         this.chatLines.add(0, new ChatLine(updateCounter, chatComponent, chatLineId));

         while (this.chatLines.size() > maxLines) {
            this.chatLines.remove(this.chatLines.size() - 1);
         }
      }
   }

   public void refreshChat() {
      this.drawnChatLines.clear();
      this.resetScroll();

      for (int i = this.chatLines.size() - 1; i >= 0; i--) {
         ChatLine chatline = this.chatLines.get(i);
         this.setChatLine(chatline.getChatComponent(), chatline.getChatLineID(), chatline.getUpdatedCounter(), true);
      }
   }

   public List<String> getSentMessages() {
      return sentMessages;
   }

   public void addToSentMessages(String message) {
      if (sentMessages.isEmpty() || !sentMessages.get(sentMessages.size() - 1).equals(message)) {
         sentMessages.add(message);
      }
   }

   public void resetScroll() {
      this.scrollPos = 0;
      this.isScrolled = false;
   }

   public void scroll(int amount) {
      this.scrollPos += amount;
      int i = (Panic.stop
            ? this.drawnChatLines.stream().filter(chatLine -> chatLine != null && !chatLine.isClientMessage).collect(Collectors.toList())
            : this.drawnChatLines)
         .size();
      if (this.scrollPos > i - this.getLineCount()) {
         this.scrollPos = i - this.getLineCount();
      }

      if (this.scrollPos <= 0) {
         this.scrollPos = 0;
         this.isScrolled = false;
      }
   }

   @Nullable
   public ITextComponent getChatComponent(int mouseX, int mouseY) {
      if (!this.getChatOpen()) {
         return null;
      } else {
         int i = ScaledResolution.getScaleFactor();
         float f = this.getChatScale();
         int j = mouseX / i - 2;
         int k = mouseY / i - 40;
         j = MathHelper.floor((float)j / f);
         k = MathHelper.floor((float)k / f);
         List<ChatLine> drawnChatLines = Panic.stop
            ? this.drawnChatLines.stream().filter(chatLine -> chatLine != null && !chatLine.isClientMessage).collect(Collectors.toList())
            : this.drawnChatLines;
         if (j >= 0 && k >= 0) {
            int l = Math.min(this.getLineCount(), drawnChatLines.size());
            if (j <= MathHelper.floor((float)this.getChatWidth() / this.getChatScale()) && k < this.mc.fontRendererObj.FONT_HEIGHT * l + l) {
               int i1 = k / this.mc.fontRendererObj.FONT_HEIGHT + this.scrollPos;
               if (i1 >= 0 && i1 < drawnChatLines.size()) {
                  ChatLine chatline = drawnChatLines.get(i1);
                  int j1 = 0;

                  for (ITextComponent itextcomponent : chatline.getChatComponent()) {
                     if (itextcomponent instanceof TextComponentString) {
                        j1 += this.mc
                           .fontRendererObj
                           .getStringWidth(GuiUtilRenderComponents.removeTextColorsIfConfigured(((TextComponentString)itextcomponent).getText(), false));
                        if (j1 > j) {
                           return itextcomponent;
                        }
                     }
                  }
               }

               return null;
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public boolean getChatOpen() {
      return this.mc.currentScreen instanceof GuiChat;
   }

   public void deleteChatLine(int id) {
      Iterator<ChatLine> iterator = this.drawnChatLines.iterator();

      while (iterator.hasNext()) {
         ChatLine chatline = iterator.next();
         if (!Panic.stop && Chat.stringIsContainsBadMassage(chatline.getChatString())) {
            iterator.remove();
         }

         if (chatline.getChatLineID() == id) {
            iterator.remove();
         }
      }

      iterator = this.chatLines.iterator();

      while (iterator.hasNext()) {
         ChatLine chatline1 = iterator.next();
         if (chatline1 != null && chatline1.getChatLineID() == id) {
            iterator.remove();
            break;
         }
      }
   }

   public int getChatWidth() {
      return calculateChatboxWidth(this.mc.gameSettings.chatWidth);
   }

   public int getChatHeight() {
      return calculateChatboxHeight(this.getChatOpen() ? this.mc.gameSettings.chatHeightFocused : this.mc.gameSettings.chatHeightUnfocused);
   }

   public float getChatScale() {
      return this.mc.gameSettings.chatScale;
   }

   public static int calculateChatboxWidth(float scale) {
      int i = 320;
      int j = 40;
      return MathHelper.floor(scale * 280.0F + 40.0F);
   }

   public static int calculateChatboxHeight(float scale) {
      int i = 180;
      int j = 20;
      return MathHelper.floor(scale * 160.0F + 20.0F);
   }

   public int getLineCount() {
      return this.getChatHeight() / 9;
   }

   public String reString(boolean censure, String s) {
      String censStart = "";
      String censEnd = "";
      if (censure) {
         s = s.replace("хуй", censStart + "х*й" + censEnd);
         s = s.replace("хуи", censStart + "х*и" + censEnd);
         s = s.replace("пидор", censStart + "пи*ор" + censEnd);
         s = s.replace("пидоры", censStart + "пи*оры" + censEnd);
         s = s.replace("хуйло", censStart + "х**ло" + censEnd);
         s = s.replace("бл¤ть", censStart + "бл*ть" + censEnd);
         s = s.replace("блядь", censStart + "бл*дь" + censEnd);
         s = s.replace("бляди", censStart + "бл*ди" + censEnd);
         s = s.replace("блть", censStart + "б*ть" + censEnd);
         s = s.replace("далбоёб", censStart + "далб**б" + censEnd);
         s = s.replace("пиздабол", censStart + "пи***бол" + censEnd);
         s = s.replace("далбоёбы", censStart + "далб**бы" + censEnd);
         s = s.replace("пиздаболы", censStart + "пи***болы" + censEnd);
         s = s.replace("даун", censStart + "д*ун" + censEnd);
         s = s.replace("дауны", censStart + "д*уны" + censEnd);
         s = s.replace("гандон", censStart + "га**он" + censEnd);
         s = s.replace("гандоны", censStart + "га**оны" + censEnd);
         s = s.replace("чмо", censStart + "ч*о" + censEnd);
         s = s.replace("хуесос", censStart + "х**сос" + censEnd);
         s = s.replace("выблядок", censStart + "выбл**ок" + censEnd);
         s = s.replace("отсоси", censStart + "отс*си" + censEnd);
         s = s.replace("соси", censStart + "со*и" + censEnd);
         s = s.replace("пиздец", censStart + "пиз*ец" + censEnd);
         s = s.replace("хуйня", censStart + "х**н¤" + censEnd);
         s = s.replace("херня", censStart + "х**ня" + censEnd);
         s = s.replace("пизди", censStart + "п**ди" + censEnd);
         s = s.replace("пиздёж", censStart + "пи**ёж" + censEnd);
         s = s.replace("пиздеж", censStart + "пи**еж" + censEnd);
         s = s.replace("залупа", censStart + "зал*па" + censEnd);
         s = s.replace("залупой", censStart + "зал*пой" + censEnd);
         s = s.replace("залупами", censStart + "зал*пами" + censEnd);
         s = s.replace("гавно", censStart + "гав*но" + censEnd);
         s = s.replace("мудак", censStart + "муд*к" + censEnd);
         s = s.replace("мудила", censStart + "муд*ла" + censEnd);
         s = s.replace("мудило", censStart + "муд*ло" + censEnd);
         s = s.replace("пидоры", censStart + "пид*ры" + censEnd);
         s = s.replace("пенис", censStart + "пен*с" + censEnd);
         s = s.replace("уебан", censStart + "уе*ан" + censEnd);
         s = s.replace("уебок", censStart + "уе*ок" + censEnd);
         s = s.replace("уЄбок", censStart + "уЄ*ок" + censEnd);
         s = s.replace("уЄбище", censStart + "уё*ище" + censEnd);
         s = s.replace("уебище", censStart + "уе*ище" + censEnd);
         s = s.replace("ебало", censStart + "еб*ло" + censEnd);
         s = s.replace("ебло", censStart + "е*ло" + censEnd);
         s = s.replace("еблище", censStart + "е**ище" + censEnd);
         s = s.replace("Єбаный", censStart + "Є**ный" + censEnd);
         s = s.replace("Єбаные", censStart + "Є**ные" + censEnd);
         s = s.replace("ебаный", censStart + "е**ный" + censEnd);
         s = s.replace("ебаные", censStart + "е**ные" + censEnd);
         s = s.replace("отьебись", censStart + "отб**ись" + censEnd);
         s = s.replace("ебать", censStart + "е**ть" + censEnd);
         s = s.replace("ахуеть", censStart + "ах**ть" + censEnd);
         s = s.replace("вахуе", censStart + "ва*уе" + censEnd);
         s = s.replace("пиздец", censStart + "пи**ец" + censEnd);
         s = s.replace("доебалс¤", censStart + "дое**лся" + censEnd);
         s = s.replace("ёбнул", censStart + "ё*нул" + censEnd);
         s = s.replace("ебнул", censStart + "е*нул" + censEnd);
         s = s.replace("ебанул", censStart + "е**нул" + censEnd);
         s = s.replace("заебал", censStart + "за**ал" + censEnd);
         s = s.replace("елда", censStart + "ел*а" + censEnd);
         s = s.replace("елдина", censStart + "ел*ина" + censEnd);
         s = s.replace("пизда", censStart + "п*зда" + censEnd);
         s = s.replace("пиздабол", censStart + "пизд*бол" + censEnd);
         s = s.replace("попизди", censStart + "поп**ди" + censEnd);
         s = s.replace("лох ", censStart + "л*х" + censEnd);
         s = s.replace("лоох", censStart + "л**х" + censEnd);
         s = s.replace("лооох", censStart + "л***х" + censEnd);
         s = s.replace("лоооох", censStart + "л****х" + censEnd);
         s = s.replace("лооооох", censStart + "л*****х" + censEnd);
         s = s.replace("лоооооох", censStart + "л******х" + censEnd);
         s = s.replace("пиздуй", censStart + "пиз**й" + censEnd);
         s = s.replace("хуёв", censStart + "х*ёв" + censEnd);
         s = s.replace("хуев", censStart + "х*ев" + censEnd);
         s = s.replace("хуями", censStart + "х**ми" + censEnd);
         s = s.replace("хуёвый", censStart + "х**вый" + censEnd);
         s = s.replace("нахуй", censStart + "на**й" + censEnd);
         s = s.replace("хую", censStart + "х*ю" + censEnd);
         s = s.replace("хуе", censStart + "х*е" + censEnd);
         s = s.replace("сосёшь", censStart + "сос*шь" + censEnd);
         s = s.replace("сосешь", censStart + "сос*шь" + censEnd);
         s = s.replace("отсоси", censStart + "отс*си" + censEnd);
         s = s.replace("отхуярю", censStart + "отх*ярю" + censEnd);
         s = s.replace("отпиздил", censStart + "отп**дил" + censEnd);
         s = s.replace("отмудохал", censStart + "отм**охал" + censEnd);
         s = s.replace("захуярил", censStart + "зах**рил" + censEnd);
         s = s.replace("отхуярил", censStart + "отх**рил" + censEnd);
         s = s.replace("отхуярю", censStart + "отх**рю" + censEnd);
         s = s.replace("ниху¤", censStart + "них*я" + censEnd);
         s = s.replace("хуеглот", censStart + "ху*глот" + censEnd);
         s = s.replace("хуегрыз", censStart + "х**грыз" + censEnd);
         s = s.replace("отсос", censStart + "отс*с" + censEnd);
         s = s.replace("отсоси", censStart + "отс*си" + censEnd);
         s = s.replace("ебал", censStart + "еб*л" + censEnd);
         s = s.replace("чмошник", censStart + "чм**ник" + censEnd);
         s = s.replace("нихера", censStart + "них*ра" + censEnd);
         s = s.replace("шлюха", censStart + "шл*ха" + censEnd);
         s = s.replace("гнида", censStart + "гн*да" + censEnd);
         s = s.replace("хуеплёт", censStart + "х**плёт" + censEnd);
         s = s.replace("пиздуй", censStart + "п**дуй" + censEnd);
         s = s.replace("пидр", censStart + "п*др" + censEnd);
         s = s.replace("выЄбываться", censStart + "вы**ываться" + censEnd);
         s = s.replace("выЄбываешься", censStart + "вы**ываешься" + censEnd);
         s = s.replace("выЄбыватся", censStart + "вы**ыватся" + censEnd);
         s = s.replace("выЄбываешся", censStart + "вы**ываешся" + censEnd);
         s = s.replace("выебнулся", censStart + "вы**нулся" + censEnd);
         s = s.replace("выебываюсь", censStart + "вы**ываюсь" + censEnd);
         s = s.replace("выЄбываюсь", censStart + "вы**ываюсь" + censEnd);
         s = s.replace("выЄбываются", censStart + "вы**ываются" + censEnd);
         s = s.replace("допизделс¤", censStart + "доп**делся" + censEnd);
         s = s.replace("допизделись", censStart + "доп**делись" + censEnd);
         s = s.replace("ху¤ми", censStart + "х*ями" + censEnd);
         s = s.replace("ебу", censStart + "е*у" + censEnd);
         s = s.replace("ебаный", censStart + "еб**ый" + censEnd);
         s = s.replace("ебучий", censStart + "е**чий" + censEnd);
         s = s.replace("ебучие", censStart + "е**чие" + censEnd);
         s = s.replace("еблан", censStart + "е*лан" + censEnd);
         s = s.replace("ебланоид", censStart + "е*ланоид" + censEnd);
         s = s.replace("ебланы", censStart + "е*ланы" + censEnd);
         s = s.replace("ебланоиды", censStart + "е*ланоиды" + censEnd);
         s = s.replace("обоссал", censStart + "обосс*л" + censEnd);
         s = s.replace("наебнулся", censStart + "на**нулс¤" + censEnd);
         s = s.replace("наебнулись", censStart + "на**нулись" + censEnd);
         s = s.replace("наебнул", censStart + "на**нул" + censEnd);
         s = s.replace("sosi", censStart + "s*si" + censEnd);
         s = s.replace("otsosi", censStart + "ots*si" + censEnd);
         s = s.replace("otsos", censStart + "ots*s" + censEnd);
         s = s.replace("отсоси", censStart + "отс*си" + censEnd);
         s = s.replace("отсос", censStart + "отс*с" + censEnd);
         s = s.replace("переебу", censStart + "пере*бу" + censEnd);
         s = s.replace("переебал", censStart + "пере*бал" + censEnd);
         s = s.replace("отьебал", censStart + "оть**ал" + censEnd);
         s = s.replace("отьебу", censStart + "оть**у" + censEnd);
         s = s.replace("лошара", censStart + "л*шара" + censEnd);
         s = s.replace("проебал", censStart + "про**ал" + censEnd);
         s = s.replace("уебал", censStart + "у**ал" + censEnd);
         s = s.replace("уебалс¤", censStart + "у**ался" + censEnd);
         s = s.replace("усрись", censStart + "уср*сь" + censEnd);
         s = s.replace("усрался", censStart + "уср*лс¤" + censEnd);
         s = s.replace("усрираются", censStart + "уср*раются" + censEnd);
         s = s.replace("обосрал", censStart + "обос*ал" + censEnd);
         s = s.replace("обосралс¤", censStart + "обос*алс¤" + censEnd);
         s = s.replace("высер", censStart + "выс*р" + censEnd);
         s = s.replace("шалава", censStart + "шал*ва" + censEnd);
         s = s.replace("гавноюзер", censStart + "ты лучший" + censEnd);
         s = s.replace("говноюзер", censStart + "ты лучший" + censEnd);
         s = s.replace("поебень", censStart + "по**ень" + censEnd);
         s = s.replace("выебан", censStart + "вы**ан" + censEnd);
         s = s.replace("выебал", censStart + "вы**ал" + censEnd);
         s = s.replace("отьебал", censStart + "оть**ал" + censEnd);
         s = s.replace("ебля", censStart + "е*ля" + censEnd);
         s = s.replace("еби", censStart + "е*и" + censEnd);
         s = s.replace("заебись", censStart + "за**ись" + censEnd);
         s = s.replace("заебок", censStart + "за**ок" + censEnd);
         s = s.replace("хуйни", censStart + "х**ни" + censEnd);
         s = s.replace("шалавы", censStart + "шал**ы" + censEnd);
         s = s.replace("разьЄб", censStart + "разь*б" + censEnd);
         s = s.replace("разьеб", censStart + "разь*б" + censEnd);
         s = s.replace("пидарас", censStart + "пид**ас" + censEnd);
         s = s.replace("пидорас", censStart + "пид**ас" + censEnd);
         s = s.replace("пидарасы", censStart + "пид**асы" + censEnd);
         s = s.replace("пидорасы", censStart + "пид**асы" + censEnd);
         s = s.replace("пидарасина", censStart + "пид**асина" + censEnd);
         s = s.replace("пидорасина", censStart + "пид**асина" + censEnd);
         s = s.replace("пидарасины", censStart + "пид**асины" + censEnd);
         s = s.replace("пидорасины", censStart + "пид**асины" + censEnd);
         s = s.replace("ебанутьс¤", censStart + "е**нуться" + censEnd);
         s = s.replace("Єбнутьс¤", censStart + "ё*нуться" + censEnd);
         s = s.replace("Єбнулс¤", censStart + "ё*нулся" + censEnd);
         s = s.replace("сука", censStart + "с*ка" + censEnd);
         s = s.replace("суки", censStart + "с*ки" + censEnd);
         s = s.replace("сучка", censStart + "с*чка" + censEnd);
         s = s.replace("сучки", censStart + "с*чки" + censEnd);
         s = s.replace("далбоёбы", censStart + "далбо**ы" + censEnd);
         s = s.replace("далбоебы", censStart + "далбо**ы" + censEnd);
         s = s.replace("ебатьс¤", censStart + "е**ться" + censEnd);
         s = s.replace("ебатс¤", censStart + "е**тся" + censEnd);
         s = s.replace("ебусь", censStart + "е**сь" + censEnd);
         s = s.replace("вьебу", censStart + "вь*бу" + censEnd);
         s = s.replace("выебу", censStart + "вы*бу" + censEnd);
         s = s.replace("бляди", censStart + "б**ди" + censEnd);
         s = s.replace("выёбывайс¤", censStart + "вы**ывайся" + censEnd);
         s = s.replace("выёбываютс¤", censStart + "вы**ываются" + censEnd);
         s = s.replace("высирает", censStart + "выс*рает" + censEnd);
         s = s.replace("высерает", censStart + "выс*рает" + censEnd);
         s = s.replace("высрал", censStart + "выср*л" + censEnd);
         s = s.replace("говнище", censStart + "говн*ще" + censEnd);
         s = s.replace("говна", censStart + "гов*а" + censEnd);
         s = s.replace("аутист", censStart + "аут*ст" + censEnd);
         s = s.replace("ебанат", censStart + "е*анат" + censEnd);
         s = s.replace("дура", censStart + "д*ра" + censEnd);
         s = s.replace("шлюшки", censStart + "шл*шки" + censEnd);
         s = s.replace("пиздит", censStart + "пиз*ит" + censEnd);
         s = s.replace("пиздят", censStart + "пиз*ят" + censEnd);
         s = s.replace("пиздишь", censStart + "пиз*ишь" + censEnd);
         s = s.replace("пиздиш", censStart + "пиз*иш" + censEnd);
         s = s.replace("виблядка", censStart + "ви**ядка" + censEnd);
         s = s.replace("виблядки", censStart + "ви**ядки" + censEnd);
         s = s.replace("выблядка", censStart + "вы**ядка" + censEnd);
         s = s.replace("ебучая", censStart + "еб*чая" + censEnd);
         s = s.replace("ебучее", censStart + "еб*чее" + censEnd);
         s = s.replace("кончил", censStart + "конч*л" + censEnd);
         s = s.replace("кончал", censStart + "конч*л" + censEnd);
         s = s.replace("конча", censStart + "кон*а" + censEnd);
         s = s.replace("хуярил", censStart + "ху*рил" + censEnd);
         s = s.replace("хуячил", censStart + "ху*чил" + censEnd);
         s = s.replace("хуярю", censStart + "ху*рю" + censEnd);
         s = s.replace("хуячю", censStart + "ху*чю" + censEnd);
         s = s.replace("хуяру", censStart + "ху*ру" + censEnd);
         s = s.replace("бля", censStart + "б**" + censEnd);
         s = s.replace("хуячу", censStart + "ху*чу" + censEnd);
         s = s.replace("свинья", censStart + "свинюшка" + censEnd);
         s = s.replace("чурка", censStart + "ч*рка" + censEnd);
         s = s.replace("чурки", censStart + "ч*рки" + censEnd);
         s = s.replace("Penis", censStart + "Pen*s" + censEnd);
         s = s.replace("penis", censStart + "pen*s" + censEnd);
         s = s.replace("целестиал", censStart + "целепукстиал" + censEnd);
         s = s.replace("акриен", censStart + "акрипук" + censEnd);
         s = s.replace("нурик", censStart + "нурипук" + censEnd);
         s = s.replace("нурсултан", censStart + "нурсулпук" + censEnd);
         s = s.replace("рич", censStart + "срич" + censEnd);
         s = s.replace("rich", censStart + "srich" + censEnd);
         s = s.replace("celestial", censStart + "celepukstial" + censEnd);
         s = s.replace("неверхук", censStart + "неверхрюк" + censEnd);
         s = s.replace("neverhook", censStart + "neverpuk" + censEnd);
         s = s.replace("я люблю этот читерский сервер StormHVH", censStart + "я дурак" + censEnd);
         s = s.replace("YT", censStart + "LOH" + censEnd);
         s = s.replace("akrien", censStart + "akripuk" + censEnd);
         s = s.replace("Celka", censStart + "Dirka" + censEnd);
         s = s.replace("екпенсив", censStart + "експуксив" + censEnd);
         s = s.replace("expensive", censStart + "expuksive" + censEnd);
         s = s.replace("хевен", censStart + "хуевен" + censEnd);
         s = s.replace("heaven", censStart + "hueven" + censEnd);
         s = s.replace("дедкод", censStart + "дедпук" + censEnd);
         s = s.replace("deadcode", censStart + "deadpuk" + censEnd);
         s = s.replace("векс", censStart + "веник" + censEnd);
         s = s.replace("вексайд", censStart + "пуксайд" + censEnd);
         s = s.replace("wexside", censStart + "pukside" + censEnd);
      }

      return s;
   }
}
