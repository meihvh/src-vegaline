package net.minecraft.client.gui;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import ru.govno.client.utils.Render.AnimationUtils;

public class ChatLine {
   private final int updateCounterCreated;
   private ITextComponent lineString;
   public boolean isClientMessage;
   AnimationUtils anim = new AnimationUtils(0.0F, 0.0F, 0.125F);
   private final int chatLineID;

   public ChatLine(int p_i45000_1_, ITextComponent p_i45000_2_, int p_i45000_3_) {
      this.lineString = p_i45000_2_;
      this.updateCounterCreated = p_i45000_1_;
      this.chatLineID = p_i45000_3_;
      if (this.isClientMessage = p_i45000_2_.getUnformattedText().startsWith("<<|vlCM|>>")) {
         this.lineString = new TextComponentString(p_i45000_2_.getUnformattedText().replace("<<|vlCM|>>", ""));
      }
   }

   public ITextComponent getChatComponent() {
      return this.lineString;
   }

   public String getChatString() {
      return this.lineString.getFormattedText();
   }

   public int getUpdatedCounter() {
      return this.updateCounterCreated;
   }

   public int getChatLineID() {
      return this.chatLineID;
   }
}
