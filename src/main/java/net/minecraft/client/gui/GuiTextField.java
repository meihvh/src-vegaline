package net.minecraft.client.gui;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.math.MathHelper;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class GuiTextField extends Gui {
   private final int id;
   private final FontRenderer fontRendererInstance;
   public int xPosition;
   public int yPosition;
   private final int width;
   private final int height;
   private String text = "";
   private int maxStringLength = 32;
   private int cursorCounter;
   private boolean enableBackgroundDrawing = true;
   private boolean canLoseFocus = true;
   private boolean isFocused;
   private boolean isEnabled = true;
   private int lineScrollOffset;
   private int cursorPosition;
   private int selectionEnd;
   private int enabledColor = 14737632;
   private int disabledColor = 7368816;
   private boolean visible = true;
   private GuiPageButtonList.GuiResponder guiResponder;
   private Predicate<String> validator = Predicates.alwaysTrue();

   public GuiTextField(int componentId, FontRenderer fontrendererObj, int x, int y, int par5Width, int par6Height) {
      this.id = componentId;
      this.fontRendererInstance = fontrendererObj;
      this.xPosition = x;
      this.yPosition = y;
      this.width = par5Width;
      this.height = par6Height;
   }

   public void setGuiResponder(GuiPageButtonList.GuiResponder guiResponderIn) {
      this.guiResponder = guiResponderIn;
   }

   public void updateCursorCounter() {
      this.cursorCounter++;
   }

   public void setText(String textIn) {
      if (this.validator.apply(textIn)) {
         if (textIn.length() > this.maxStringLength) {
            this.text = textIn.substring(0, this.maxStringLength);
         } else {
            this.text = textIn;
         }

         this.setCursorPositionEnd();
      }
   }

   public String getText() {
      return this.text;
   }

   public String getSelectedText() {
      int i = this.cursorPosition < this.selectionEnd ? this.cursorPosition : this.selectionEnd;
      int j = this.cursorPosition < this.selectionEnd ? this.selectionEnd : this.cursorPosition;
      return this.text.substring(i, j);
   }

   public void setValidator(Predicate<String> theValidator) {
      this.validator = theValidator;
   }

   public void writeText(String textToWrite) {
      String s = "";
      String s1 = ChatAllowedCharacters.filterAllowedCharacters(textToWrite);
      int i = this.cursorPosition < this.selectionEnd ? this.cursorPosition : this.selectionEnd;
      int j = this.cursorPosition < this.selectionEnd ? this.selectionEnd : this.cursorPosition;
      int k = this.maxStringLength - this.text.length() - (i - j);
      if (!this.text.isEmpty()) {
         s = s + this.text.substring(0, i);
      }

      int l;
      if (k < s1.length()) {
         s = s + s1.substring(0, k);
         l = k;
      } else {
         s = s + s1;
         l = s1.length();
      }

      if (!this.text.isEmpty() && j < this.text.length()) {
         s = s + this.text.substring(j);
      }

      if (this.validator.apply(s)) {
         this.text = s;
         this.moveCursorBy(i - this.selectionEnd + l);
         this.func_190516_a(this.id, this.text);
      }
   }

   public void func_190516_a(int p_190516_1_, String p_190516_2_) {
      if (this.guiResponder != null) {
         this.guiResponder.setEntryValue(p_190516_1_, p_190516_2_);
      }
   }

   public void deleteWords(int num) {
      if (!this.text.isEmpty()) {
         if (this.selectionEnd != this.cursorPosition) {
            this.writeText("");
         } else {
            this.deleteFromCursor(this.getNthWordFromCursor(num) - this.cursorPosition);
         }
      }
   }

   public void deleteFromCursor(int num) {
      if (!this.text.isEmpty()) {
         if (this.selectionEnd != this.cursorPosition) {
            this.writeText("");
         } else {
            boolean flag = num < 0;
            int i = flag ? this.cursorPosition + num : this.cursorPosition;
            int j = flag ? this.cursorPosition : this.cursorPosition + num;
            String s = "";
            if (i >= 0) {
               s = this.text.substring(0, i);
            }

            if (j < this.text.length()) {
               s = s + this.text.substring(j);
            }

            if (this.validator.apply(s)) {
               this.text = s;
               if (flag) {
                  this.moveCursorBy(num);
               }

               this.func_190516_a(this.id, this.text);
            }
         }
      }
   }

   public int getId() {
      return this.id;
   }

   public int getNthWordFromCursor(int numWords) {
      return this.getNthWordFromPos(numWords, this.getCursorPosition());
   }

   public int getNthWordFromPos(int n, int pos) {
      return this.getNthWordFromPosWS(n, pos, true);
   }

   public int getNthWordFromPosWS(int n, int pos, boolean skipWs) {
      int i = pos;
      boolean flag = n < 0;
      int j = Math.abs(n);

      for (int k = 0; k < j; k++) {
         if (!flag) {
            int l = this.text.length();
            i = this.text.indexOf(32, i);
            if (i == -1) {
               i = l;
            } else {
               while (skipWs && i < l && this.text.charAt(i) == ' ') {
                  i++;
               }
            }
         } else {
            while (skipWs && i > 0 && this.text.charAt(i - 1) == ' ') {
               i--;
            }

            while (i > 0 && this.text.charAt(i - 1) != ' ') {
               i--;
            }
         }
      }

      return i;
   }

   public void moveCursorBy(int num) {
      this.setCursorPosition(this.selectionEnd + num);
   }

   public void setCursorPosition(int pos) {
      this.cursorPosition = pos;
      int i = this.text.length();
      this.cursorPosition = MathHelper.clamp(this.cursorPosition, 0, i);
      this.setSelectionPos(this.cursorPosition);
   }

   public void setCursorPositionZero() {
      this.setCursorPosition(0);
   }

   public void setCursorPositionEnd() {
      this.setCursorPosition(this.text.length());
   }

   public boolean textboxKeyTyped(char typedChar, int keyCode) {
      if (!this.isFocused) {
         return false;
      } else if (GuiScreen.isKeyComboCtrlA(keyCode)) {
         this.setCursorPositionEnd();
         this.setSelectionPos(0);
         return true;
      } else if (GuiScreen.isKeyComboCtrlC(keyCode)) {
         GuiScreen.setClipboardString(this.getSelectedText());
         return true;
      } else if (GuiScreen.isKeyComboCtrlV(keyCode)) {
         if (this.isEnabled) {
            this.writeText(GuiScreen.getClipboardString());
         }

         return true;
      } else if (GuiScreen.isKeyComboCtrlX(keyCode)) {
         GuiScreen.setClipboardString(this.getSelectedText());
         if (this.isEnabled) {
            this.writeText("");
         }

         return true;
      } else {
         switch (keyCode) {
            case 14:
               if (GuiScreen.isCtrlKeyDown()) {
                  if (this.isEnabled) {
                     this.deleteWords(-1);
                  }
               } else if (this.isEnabled) {
                  this.deleteFromCursor(-1);
               }

               return true;
            case 199:
               if (GuiScreen.isShiftKeyDown()) {
                  this.setSelectionPos(0);
               } else {
                  this.setCursorPositionZero();
               }

               return true;
            case 203:
               if (GuiScreen.isShiftKeyDown()) {
                  if (GuiScreen.isCtrlKeyDown()) {
                     this.setSelectionPos(this.getNthWordFromPos(-1, this.getSelectionEnd()));
                  } else {
                     this.setSelectionPos(this.getSelectionEnd() - 1);
                  }
               } else if (GuiScreen.isCtrlKeyDown()) {
                  this.setCursorPosition(this.getNthWordFromCursor(-1));
               } else {
                  this.moveCursorBy(-1);
               }

               return true;
            case 205:
               if (GuiScreen.isShiftKeyDown()) {
                  if (GuiScreen.isCtrlKeyDown()) {
                     this.setSelectionPos(this.getNthWordFromPos(1, this.getSelectionEnd()));
                  } else {
                     this.setSelectionPos(this.getSelectionEnd() + 1);
                  }
               } else if (GuiScreen.isCtrlKeyDown()) {
                  this.setCursorPosition(this.getNthWordFromCursor(1));
               } else {
                  this.moveCursorBy(1);
               }

               return true;
            case 207:
               if (GuiScreen.isShiftKeyDown()) {
                  this.setSelectionPos(this.text.length());
               } else {
                  this.setCursorPositionEnd();
               }

               return true;
            case 211:
               if (GuiScreen.isCtrlKeyDown()) {
                  if (this.isEnabled) {
                     this.deleteWords(1);
                  }
               } else if (this.isEnabled) {
                  this.deleteFromCursor(1);
               }

               return true;
            default:
               if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                  if (this.isEnabled) {
                     this.writeText(Character.toString(typedChar));
                  }

                  return true;
               } else {
                  return false;
               }
         }
      }
   }

   public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
      boolean flag = mouseX >= this.xPosition && mouseX < this.xPosition + this.width && mouseY >= this.yPosition && mouseY < this.yPosition + this.height;
      if (this.canLoseFocus) {
         this.setFocused(flag);
      }

      if (this.isFocused && flag && mouseButton == 0) {
         int i = mouseX - this.xPosition;
         if (this.enableBackgroundDrawing) {
            i -= 4;
         }

         String s = this.fontRendererInstance.trimStringToWidth(this.text.substring(this.lineScrollOffset), this.getWidth());
         this.setCursorPosition(this.fontRendererInstance.trimStringToWidth(s, i).length() + this.lineScrollOffset);
         return true;
      } else {
         return false;
      }
   }

   public void drawTextBox() {
      if (this.getVisible()) {
         if (this.getEnableBackgroundDrawing()) {
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               (float)(this.xPosition + 1),
               (float)(this.yPosition + 1),
               (float)(this.xPosition + this.width - 1),
               (float)(this.yPosition + this.height - 1),
               5.0F,
               1.0F,
               ColorUtils.getColor(40, 80, 225),
               ColorUtils.getColor(40, 80, 225),
               ColorUtils.getColor(40, 80, 225),
               ColorUtils.getColor(40, 80, 225),
               true,
               true,
               true
            );
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               (float)(this.xPosition + 2),
               (float)(this.yPosition + 2),
               (float)(this.xPosition + this.width - 2),
               (float)(this.yPosition + this.height - 2),
               4.0F,
               1.0F,
               -16777216,
               -16777216,
               -16777216,
               -16777216,
               false,
               true,
               true
            );
         }

         int i = this.isEnabled ? this.enabledColor : this.disabledColor;
         int j = this.cursorPosition - this.lineScrollOffset;
         int k = this.selectionEnd - this.lineScrollOffset;
         String s = this.fontRendererInstance.trimStringToWidth(this.text.substring(this.lineScrollOffset), this.getWidth());
         boolean flag = j >= 0 && j <= s.length();
         boolean flag1 = this.isFocused && this.cursorCounter / 6 % 2 == 0 && flag;
         int l = this.enableBackgroundDrawing ? this.xPosition + 4 : this.xPosition;
         int i1 = this.enableBackgroundDrawing ? this.yPosition + (this.height - 8) / 2 : this.yPosition;
         int j1 = l;
         if (k > s.length()) {
            k = s.length();
         }

         if (!s.isEmpty()) {
            String s1 = flag ? s.substring(0, j) : s;
            j1 = this.fontRendererInstance.drawStringWithShadow(s1, (float)l + 2.0F, (float)i1, i);
         }

         boolean flag2 = this.cursorPosition < this.text.length() || this.text.length() >= this.getMaxStringLength();
         int k1 = j1;
         if (!flag) {
            k1 = j > 0 ? l + this.width : l;
         } else if (flag2) {
            k1 = j1 - 1;
            j1--;
         }

         if (!s.isEmpty() && flag && j < s.length()) {
            j1 = this.fontRendererInstance.drawStringWithShadow(s.substring(j), (float)j1, (float)i1, i);
         }

         if (flag1) {
            if (flag2) {
               Gui.drawRect(k1, (double)(i1 - 1), (double)(k1 + 1), (double)(i1 + 1 + this.fontRendererInstance.FONT_HEIGHT), -3092272);
            } else {
               this.fontRendererInstance.drawStringWithShadow("_", (float)k1, (float)i1, i);
            }
         }

         if (k != j) {
            int l1 = l + this.fontRendererInstance.getStringWidth(s.substring(0, k));
            this.drawCursorVertical(k1, i1 - 1, l1 - 1, i1 + 1 + this.fontRendererInstance.FONT_HEIGHT);
         }
      }
   }

   public void drawTextBox(boolean censured) {
      if (this.getVisible()) {
         if (this.getEnableBackgroundDrawing()) {
            RenderUtils.smoothAngleRect(
               (float)(this.xPosition - 1),
               (float)(this.yPosition + 4),
               (float)(this.xPosition + this.width + 1),
               (float)(this.yPosition + this.height - 4),
               ColorUtils.getColor(40, 80, 225)
            );
            RenderUtils.smoothAngleRect(
               (float)this.xPosition, (float)(this.yPosition + 5), (float)(this.xPosition + this.width), (float)(this.yPosition + this.height - 5), -16777216
            );
         }

         int i = this.isEnabled ? this.enabledColor : this.disabledColor;
         int j = this.cursorPosition - this.lineScrollOffset;
         int k = this.selectionEnd - this.lineScrollOffset;
         String s = this.fontRendererInstance.trimStringToWidth(this.text.substring(this.lineScrollOffset), this.getWidth());
         String censStart = "";
         String censEnd = "";
         if (censured) {
            s = s.replace("хуй", censStart + "х*й" + censEnd);
            s = s.replace("хуи", censStart + "х*и" + censEnd);
            s = s.replace("пидор", censStart + "пи*ор" + censEnd);
            s = s.replace("пидоры", censStart + "пи*оры" + censEnd);
            s = s.replace("хуйло", censStart + "х**ло" + censEnd);
            s = s.replace("блять", censStart + "бл*ть" + censEnd);
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
            s = s.replace("хуйня", censStart + "х**ня" + censEnd);
            s = s.replace("херня", censStart + "х**ня" + censEnd);
            s = s.replace("пизди", censStart + "п**ди" + censEnd);
            s = s.replace("пиздёж", censStart + "пи**Єж" + censEnd);
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
            s = s.replace("уЄбище", censStart + "уЄ*ище" + censEnd);
            s = s.replace("уебище", censStart + "уе*ище" + censEnd);
            s = s.replace("ебало", censStart + "еб*ло" + censEnd);
            s = s.replace("ебло", censStart + "е*ло" + censEnd);
            s = s.replace("еблище", censStart + "е**ище" + censEnd);
            s = s.replace("ёбаный", censStart + "Є**ный" + censEnd);
            s = s.replace("ёбаные", censStart + "Є**ные" + censEnd);
            s = s.replace("ебаный", censStart + "е**ный" + censEnd);
            s = s.replace("ебаные", censStart + "е**ные" + censEnd);
            s = s.replace("отьебись", censStart + "отб**ись" + censEnd);
            s = s.replace("ебать", censStart + "е**ть" + censEnd);
            s = s.replace("ахуеть", censStart + "ах**ть" + censEnd);
            s = s.replace("вахуе", censStart + "ва*уе" + censEnd);
            s = s.replace("пиздец", censStart + "пи**ец" + censEnd);
            s = s.replace("доебался", censStart + "дое**лся" + censEnd);
            s = s.replace("Єбнул", censStart + "Є*нул" + censEnd);
            s = s.replace("ебнул", censStart + "е*нул" + censEnd);
            s = s.replace("ебанул", censStart + "е**нул" + censEnd);
            s = s.replace("заебал", censStart + "за**ал" + censEnd);
            s = s.replace("елда", censStart + "ел*а" + censEnd);
            s = s.replace("елдина", censStart + "ел*ина" + censEnd);
            s = s.replace("пизда", censStart + "п*зда" + censEnd);
            s = s.replace("пиздабол", censStart + "пизд*бол" + censEnd);
            s = s.replace("попизди", censStart + "поп**ди" + censEnd);
            s = s.replace("лох", censStart + "л*х" + censEnd);
            s = s.replace("лоох", censStart + "л**х" + censEnd);
            s = s.replace("лооох", censStart + "л***х" + censEnd);
            s = s.replace("лоооох", censStart + "л****х" + censEnd);
            s = s.replace("лооооох", censStart + "л*****х" + censEnd);
            s = s.replace("лоооооох", censStart + "л******х" + censEnd);
            s = s.replace("пиздуй", censStart + "пиз**й" + censEnd);
            s = s.replace("хуёв", censStart + "х*Єв" + censEnd);
            s = s.replace("хуев", censStart + "х*ев" + censEnd);
            s = s.replace("хуями", censStart + "х**ми" + censEnd);
            s = s.replace("хуёвый", censStart + "х**вый" + censEnd);
            s = s.replace("нахуй", censStart + "на**й" + censEnd);
            s = s.replace("хую", censStart + "х*ю" + censEnd);
            s = s.replace("хуе", censStart + "х*е" + censEnd);
            s = s.replace("сосЄшь", censStart + "сос*шь" + censEnd);
            s = s.replace("сосешь", censStart + "сос*шь" + censEnd);
            s = s.replace("отсоси", censStart + "отс*си" + censEnd);
            s = s.replace("отхуярю", censStart + "отх*ярю" + censEnd);
            s = s.replace("отпиздил", censStart + "отп**дил" + censEnd);
            s = s.replace("отмудохал", censStart + "отм**охал" + censEnd);
            s = s.replace("захуярил", censStart + "зах**рил" + censEnd);
            s = s.replace("отхуярил", censStart + "отх**рил" + censEnd);
            s = s.replace("отхуярю", censStart + "отх**рю" + censEnd);
            s = s.replace("нихуя", censStart + "них*я" + censEnd);
            s = s.replace("хуеглот", censStart + "ху*глот" + censEnd);
            s = s.replace("хуегрыз", censStart + "х**грыз" + censEnd);
            s = s.replace("отсос", censStart + "отс*с" + censEnd);
            s = s.replace("отсоси", censStart + "отс*си" + censEnd);
            s = s.replace("ебал", censStart + "еб*л" + censEnd);
            s = s.replace("чмошник", censStart + "чм**ник" + censEnd);
            s = s.replace("нихера", censStart + "них*ра" + censEnd);
            s = s.replace("шлюха", censStart + "шл*ха" + censEnd);
            s = s.replace("гнида", censStart + "гн*да" + censEnd);
            s = s.replace("хуеплЄт", censStart + "х**плЄт" + censEnd);
            s = s.replace("пиздуй", censStart + "п**дуй" + censEnd);
            s = s.replace("пидр", censStart + "п*др" + censEnd);
            s = s.replace("выёбываться", censStart + "вы**ываться" + censEnd);
            s = s.replace("выёбываешься", censStart + "вы**ываешься" + censEnd);
            s = s.replace("выёбыватся", censStart + "вы**ыватся" + censEnd);
            s = s.replace("выёбываешся", censStart + "вы**ываешся" + censEnd);
            s = s.replace("выёбнулся", censStart + "вы**нулся" + censEnd);
            s = s.replace("выёбываюсь", censStart + "вы**ываюсь" + censEnd);
            s = s.replace("выёбываюсь", censStart + "вы**ываюсь" + censEnd);
            s = s.replace("выёбываются", censStart + "вы**ываются" + censEnd);
            s = s.replace("допизделся", censStart + "доп**делся" + censEnd);
            s = s.replace("допизделись", censStart + "доп**делись" + censEnd);
            s = s.replace("хуями", censStart + "х*ями" + censEnd);
            s = s.replace("ебу", censStart + "е*у" + censEnd);
            s = s.replace("ебаный", censStart + "еб**ый" + censEnd);
            s = s.replace("ебучий", censStart + "е**чий" + censEnd);
            s = s.replace("ебучие", censStart + "е**чие" + censEnd);
            s = s.replace("еблан", censStart + "е*лан" + censEnd);
            s = s.replace("ебланоид", censStart + "е*ланоид" + censEnd);
            s = s.replace("ебланы", censStart + "е*ланы" + censEnd);
            s = s.replace("ебланоиды", censStart + "е*ланоиды" + censEnd);
            s = s.replace("обоссал", censStart + "обосс*л" + censEnd);
            s = s.replace("наебнулся", censStart + "на**нулся" + censEnd);
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
            s = s.replace("уебался", censStart + "у**ался" + censEnd);
            s = s.replace("усрись", censStart + "уср*сь" + censEnd);
            s = s.replace("усрался", censStart + "уср*лся" + censEnd);
            s = s.replace("усрираются", censStart + "уср*раются" + censEnd);
            s = s.replace("обосрал", censStart + "обос*ал" + censEnd);
            s = s.replace("обосрался", censStart + "обос*ался" + censEnd);
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
            s = s.replace("разьёб", censStart + "разь*б" + censEnd);
            s = s.replace("разьеб", censStart + "разь*б" + censEnd);
            s = s.replace("пидарас", censStart + "пид**ас" + censEnd);
            s = s.replace("пидорас", censStart + "пид**ас" + censEnd);
            s = s.replace("пидарасы", censStart + "пид**асы" + censEnd);
            s = s.replace("пидорасы", censStart + "пид**асы" + censEnd);
            s = s.replace("пидарасина", censStart + "пид**асина" + censEnd);
            s = s.replace("пидорасина", censStart + "пид**асина" + censEnd);
            s = s.replace("пидарасины", censStart + "пид**асины" + censEnd);
            s = s.replace("пидорасины", censStart + "пид**асины" + censEnd);
            s = s.replace("ебануться", censStart + "е**нуться" + censEnd);
            s = s.replace("ёбнуться", censStart + "Є*нуться" + censEnd);
            s = s.replace("ёбнулся", censStart + "Є*нулся" + censEnd);
            s = s.replace("сука", censStart + "с*ка" + censEnd);
            s = s.replace("суки", censStart + "с*ки" + censEnd);
            s = s.replace("сучка", censStart + "с*чка" + censEnd);
            s = s.replace("сучки", censStart + "с*чки" + censEnd);
            s = s.replace("далбоЄбы", censStart + "далбо**ы" + censEnd);
            s = s.replace("далбоебы", censStart + "далбо**ы" + censEnd);
            s = s.replace("ебаться", censStart + "е**ться" + censEnd);
            s = s.replace("ебатся", censStart + "е**тся" + censEnd);
            s = s.replace("ебусь", censStart + "е**сь" + censEnd);
            s = s.replace("вьебу", censStart + "вь*бу" + censEnd);
            s = s.replace("выебу", censStart + "вы*бу" + censEnd);
            s = s.replace("бляди", censStart + "б**ди" + censEnd);
            s = s.replace("выёбывайся", censStart + "вы**ывайся" + censEnd);
            s = s.replace("выёбываются", censStart + "вы**ываются" + censEnd);
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
            s = s.replace("целестиал", censStart + "целепукстиал" + censEnd);
            s = s.replace("heaven", censStart + "hueven" + censEnd);
            s = s.replace("дедкод", censStart + "дедпук" + censEnd);
            s = s.replace("deadcode", censStart + "deadpuk" + censEnd);
            s = s.replace("векс", censStart + "вепукс" + censEnd);
            s = s.replace("вексайд", censStart + "вепуксайд" + censEnd);
            s = s.replace("wexside", censStart + "pukside" + censEnd);
         }

         boolean flag = j >= 0 && j <= s.length();
         boolean flag1 = this.isFocused && this.cursorCounter / 6 % 2 == 0 && flag;
         int l = this.enableBackgroundDrawing ? this.xPosition + 4 : this.xPosition;
         int i1 = this.enableBackgroundDrawing ? this.yPosition + (this.height - 8) / 2 : this.yPosition;
         int j1 = l;
         if (k > s.length()) {
            k = s.length();
         }

         if (!s.isEmpty()) {
            String s1 = flag ? s.substring(0, j) : s;
            j1 = this.fontRendererInstance.drawStringWithShadow(s1, (float)l + 2.0F, (float)i1, i);
         }

         boolean flag2 = this.cursorPosition < this.text.length() || this.text.length() >= this.getMaxStringLength();
         int k1 = j1;
         if (!flag) {
            k1 = j > 0 ? l + this.width : l;
         } else if (flag2) {
            k1 = j1 - 1;
            j1--;
         }

         if (!s.isEmpty() && flag && j < s.length()) {
            j1 = this.fontRendererInstance.drawStringWithShadow(s.substring(j), (float)j1, (float)i1, i);
         }

         if (flag1) {
            if (flag2) {
               Gui.drawRect(k1, (double)(i1 - 1), (double)(k1 + 1), (double)(i1 + 1 + this.fontRendererInstance.FONT_HEIGHT), -3092272);
            } else {
               this.fontRendererInstance.drawStringWithShadow("_", (float)k1, (float)i1, i);
            }
         }

         if (k != j) {
            int l1 = l + this.fontRendererInstance.getStringWidth(s.substring(0, k));
            this.drawCursorVertical(k1, i1 - 1, l1 - 1, i1 + 1 + this.fontRendererInstance.FONT_HEIGHT);
         }
      }
   }

   private void drawCursorVertical(int startX, int startY, int endX, int endY) {
      if (startX < endX) {
         int i = startX;
         startX = endX;
         endX = i;
      }

      if (startY < endY) {
         int j = startY;
         startY = endY;
         endY = j;
      }

      if (endX > this.xPosition + this.width) {
         endX = this.xPosition + this.width;
      }

      if (startX > this.xPosition + this.width) {
         startX = this.xPosition + this.width;
      }

      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferbuilder = tessellator.getBuffer();
      GlStateManager.color(0.0F, 0.0F, 255.0F, 255.0F);
      GlStateManager.disableTexture2D();
      GlStateManager.enableColorLogic();
      GlStateManager.colorLogicOp(GlStateManager.LogicOp.OR_REVERSE);
      bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
      bufferbuilder.pos((double)startX, (double)endY, 0.0).endVertex();
      bufferbuilder.pos((double)endX, (double)endY, 0.0).endVertex();
      bufferbuilder.pos((double)endX, (double)startY, 0.0).endVertex();
      bufferbuilder.pos((double)startX, (double)startY, 0.0).endVertex();
      tessellator.draw();
      GlStateManager.disableColorLogic();
      GlStateManager.enableTexture2D();
   }

   public void setMaxStringLength(int length) {
      this.maxStringLength = length;
      if (this.text.length() > length) {
         this.text = this.text.substring(0, length);
      }
   }

   public int getMaxStringLength() {
      return this.maxStringLength;
   }

   public int getCursorPosition() {
      return this.cursorPosition;
   }

   public boolean getEnableBackgroundDrawing() {
      return this.enableBackgroundDrawing;
   }

   public void setEnableBackgroundDrawing(boolean enableBackgroundDrawingIn) {
      this.enableBackgroundDrawing = enableBackgroundDrawingIn;
   }

   public void setTextColor(int color) {
      this.enabledColor = color;
   }

   public void setDisabledTextColour(int color) {
      this.disabledColor = color;
   }

   public void setFocused(boolean isFocusedIn) {
      if (isFocusedIn && !this.isFocused) {
         this.cursorCounter = 0;
      }

      this.isFocused = isFocusedIn;
      if (Minecraft.getMinecraft().currentScreen != null) {
         Minecraft.getMinecraft().currentScreen.func_193975_a(isFocusedIn);
      }
   }

   public boolean isFocused() {
      return this.isFocused;
   }

   public void setEnabled(boolean enabled) {
      this.isEnabled = enabled;
   }

   public int getSelectionEnd() {
      return this.selectionEnd;
   }

   public int getWidth() {
      return this.getEnableBackgroundDrawing() ? this.width - 8 : this.width;
   }

   public void setSelectionPos(int position) {
      int i = this.text.length();
      if (position > i) {
         position = i;
      }

      if (position < 0) {
         position = 0;
      }

      this.selectionEnd = position;
      if (this.fontRendererInstance != null) {
         if (this.lineScrollOffset > i) {
            this.lineScrollOffset = i;
         }

         int j = this.getWidth();
         String s = this.fontRendererInstance.trimStringToWidth(this.text.substring(this.lineScrollOffset), j);
         int k = s.length() + this.lineScrollOffset;
         if (position == this.lineScrollOffset) {
            this.lineScrollOffset = this.lineScrollOffset - this.fontRendererInstance.trimStringToWidth(this.text, j, true).length();
         }

         if (position > k) {
            this.lineScrollOffset += position - k;
         } else if (position <= this.lineScrollOffset) {
            this.lineScrollOffset = this.lineScrollOffset - (this.lineScrollOffset - position);
         }

         this.lineScrollOffset = MathHelper.clamp(this.lineScrollOffset, 0, i);
      }
   }

   public void setCanLoseFocus(boolean canLoseFocusIn) {
      this.canLoseFocus = canLoseFocusIn;
   }

   public boolean getVisible() {
      return this.visible;
   }

   public void setVisible(boolean isVisible) {
      this.visible = isVisible;
   }
}
