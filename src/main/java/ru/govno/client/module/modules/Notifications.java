package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventRender2D;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.InventoryUtil;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class Notifications extends Module {
   public static Notifications get;
   public ModeSettings Mode;
   private final BoolSettings ArmorNotify;
   private final BoolSettings TotemNotify;
   private final boolean[] doArmorNotifyTriggerMid = new boolean[]{false, false, false, false};
   private final boolean[] doArmorNotifyTriggerLow = new boolean[]{false, false, false, false};
   private final List<String> notifiesArmorStrings = new ArrayList<>();
   boolean haveTotems = false;

   public Notifications() {
      super("Notifications", 0, Module.Category.RENDER);
      this.settings.add(this.Mode = new ModeSettings("Mode", "Colored", this, new String[]{"Colored", "Dark"}));
      this.settings.add(this.ArmorNotify = new BoolSettings("ArmorNotify", true, this));
      this.settings.add(this.TotemNotify = new BoolSettings("TotemNotify", true, this));
      this.setDemand(1, 0);
      get = this;
   }

   @Override
   public void onUpdate() {
      if (this.TotemNotify.getBool() && Minecraft.player != null && Minecraft.player.inventory != null) {
         boolean have = InventoryUtil.getItemInInv(Items.TOTEM) != -1;
         if (have) {
            this.haveTotems = true;
         } else if (this.haveTotems) {
            Notifications.Notify.spawnNotify("У тебя не осталось тотемов!", Notifications.type.ARMOR);
            ClientTune.get.playArmorPreCrackSong(true);
            this.haveTotems = false;
         }
      } else {
         this.haveTotems = false;
      }

      if (this.notifiesArmorStrings.size() > 0) {
         try {
            String notifyString = this.notifiesArmorStrings.get(0);
            Notifications.Notify.spawnNotify(notifyString, Notifications.type.ARMOR);
            ClientTune.get.playArmorPreCrackSong(notifyString.contains("совсем"));
            this.notifiesArmorStrings.remove(0);
         } catch (Exception var10) {
            var10.fillInStackTrace();
         }
      }

      String[] midArmors = new String[]{"", "", "", ""};
      String[] lowArmors = new String[]{"", "", "", ""};
      float minDurPC = 0.25F;
      float criticalMinPC = 0.05F;
      if (this.ArmorNotify.getBool()) {
         for (int i = 0; i < 4; i++) {
            ItemStack stack = Minecraft.player.inventory.armorInventory.get(i);
            if (stack != null && stack.isItemDamaged()) {
               Item durPC = stack.getItem();
               if (durPC instanceof ItemArmor) {
                  ItemArmor armor = (ItemArmor)durPC;
                  float durPCx = MathUtils.clamp(1.0F - (float)stack.getItemDamage() / (float)stack.getMaxDamage(), 0.0F, 1.0F);
                  String armorName = TextFormatting.DARK_GRAY
                     + "Элемент "
                     + TextFormatting.GRAY
                     + "'"
                     + TextFormatting.RED
                     + armor.getItemStackDisplayName(stack)
                     + TextFormatting.GRAY
                     + "'"
                     + TextFormatting.WHITE;
                  if (durPCx <= criticalMinPC) {
                     lowArmors[i] = armorName + " совсем сломан!!!";
                  } else if (durPCx <= minDurPC) {
                     midArmors[i] = armorName + " значительно поломан.";
                  }
               }
            }
         }

         int tempIndex = 0;

         for (String mid : midArmors) {
            if (mid.isEmpty()) {
               this.doArmorNotifyTriggerMid[tempIndex] = true;
            } else if (this.doArmorNotifyTriggerMid[tempIndex]) {
               this.notifiesArmorStrings.add(mid);
               this.doArmorNotifyTriggerMid[tempIndex] = false;
            }

            tempIndex++;
         }

         tempIndex = 0;

         for (String low : lowArmors) {
            if (low.isEmpty()) {
               this.doArmorNotifyTriggerLow[tempIndex] = true;
            } else if (this.doArmorNotifyTriggerLow[tempIndex]) {
               this.notifiesArmorStrings.add(low);
               this.doArmorNotifyTriggerLow[tempIndex] = false;
            }

            tempIndex++;
         }
      } else {
         this.doArmorNotifyTriggerMid[0] = true;
         this.doArmorNotifyTriggerMid[1] = true;
         this.doArmorNotifyTriggerMid[2] = true;
         this.doArmorNotifyTriggerMid[3] = true;
         this.doArmorNotifyTriggerLow[0] = true;
         this.doArmorNotifyTriggerLow[1] = true;
         this.doArmorNotifyTriggerLow[2] = true;
         this.doArmorNotifyTriggerLow[3] = true;
      }
   }

   @EventTarget
   public void onRender2D(EventRender2D event) {
      drawNotifyS();
   }

   public static void drawNotifyS() {
      if (!Notifications.Notify.notifications.isEmpty()) {
         int yDist = Hud.get.isActived() && Hud.get.Information.getBool() && Hud.get.Info.getMode().equalsIgnoreCase("Plastic") ? 2 : 1;

         for (Notifications.Notification notification : Notifications.Notify.notifications) {
            Notifications.Notify.draw(notification, yDist, get.Mode.getMode());
            yDist++;
         }

         Notifications.Notify.notifications.removeIf(huy -> System.currentTimeMillis() - huy.getTime() >= huy.max_time);
      }
   }

   static class Notification {
      private final AnimationUtils animY = new AnimationUtils(1.0F, 1.1F, 0.075F);
      private final AnimationUtils animX = new AnimationUtils(0.0F, 1.0F, 0.075F);
      private final String message;
      private final long time;
      private final long max_time;
      Notifications.type type;

      public Notification(String message, long max_time, Notifications.type type) {
         this.max_time = max_time;
         this.message = message;
         this.time = System.currentTimeMillis();
         this.type = type;
      }

      public long getTime() {
         return this.time;
      }

      public int getColorize() {
         return this.type.color;
      }

      public long getMax_Time() {
         return this.max_time;
      }

      public String getMessage() {
         return this.message;
      }
   }

   public class Notify {
      public static ArrayList<Notifications.Notification> notifications = new ArrayList<>();

      public static void spawnNotify(String message, Notifications.type usedtype) {
         long maxTime = (long)(1200.0F * (usedtype == Notifications.type.ARMOR ? 10.0F : (usedtype == Notifications.type.STAFF ? 2.0F : 1.0F)));
         notifications.add(new Notifications.Notification(message, maxTime, usedtype));
      }

      static void drawIcon(Notifications.type type, float alpha, float x, float y, float size, String mode) {
         if (mode.equalsIgnoreCase("Dark")) {
            int c1 = ColorUtils.swapAlpha(type.color, (float)ColorUtils.getAlphaFromColor(type.color));
            int c2 = ColorUtils.swapAlpha(type.color, (float)ColorUtils.getAlphaFromColor(type.color) / 4.0F);
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               x - size / 2.0F, y - size / 2.0F, x + size / 2.0F, y + size / 2.0F, 0.0F, 3.0F * alpha, c2, c2, c2, c2, true, false, true
            );
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               x - size / 2.0F, y - size / 2.0F, x + size / 2.0F, y + size / 2.0F, 0.0F, 0.0F, c1, c1, c1, c1, true, true, false
            );
         } else {
            ResourceLocation icon = new ResourceLocation("vegaline/modules/notifications/icons/" + type.icon.toLowerCase().replace(" ", "_") + ".png");
            if (icon != null) {
               RenderUtils.drawImageWithAlpha(icon, x, y, size, size, ColorUtils.getFixedWhiteColor(), (int)(255.0F * alpha));
            }
         }
      }

      static void draw(Notifications.Notification notify, int index, String mode) {
         notify.animY.speed = 0.15F;
         notify.animX.speed = 0.075F;
         boolean isDark = mode.equalsIgnoreCase("Dark");
         ScaledResolution sr = new ScaledResolution(Module.mc);
         CFontRenderer font = isDark ? Fonts.comfortaaBold_16 : Fonts.noise_16;
         Notifications.type type = notify.type;
         String text = notify.getMessage();
         String massage = notify.type.icon;
         int colorize = notify.getColorize();
         int colorize2 = ColorUtils.getColor(
            (int)MathUtils.clamp((float)ColorUtils.getRedFromColor(notify.getColorize()) * 1.75F, 0.0F, 255.0F),
            (int)MathUtils.clamp((float)ColorUtils.getGreenFromColor(notify.getColorize()) * 1.75F, 0.0F, 255.0F),
            (int)MathUtils.clamp((float)ColorUtils.getBlueFromColor(notify.getColorize()) * 1.75F, 0.0F, 255.0F)
         );
         float max_time = (float)notify.getMax_Time();
         float time = (float)(System.currentTimeMillis() - notify.getTime());
         String surf = isDark ? " " : " | ";
         String surf2 = isDark ? "§r§f §r" : "§r§f | §r";
         if (time < 50.0F) {
            notify.animY.setAnim((float)index);
         }

         if (time + 100.0F > max_time) {
            notify.animX.speed = 0.125F;
            notify.animX.to = 0.0F;
            if (notify.animY.getAnim() == 1.0F) {
               notify.animY.setAnim((float)index - 1.5F);
            }
         } else {
            notify.animX.to = 1.0F;
            if (index - 1 >= 0) {
               notify.animY.to = (float)(index - 1);
            }
         }

         float width = (isDark ? 18.5F : 24.0F) + font.getStringWidth(text + surf + massage);
         float w = width * notify.animX.getAnim();
         float hStep = (float)(isDark ? 17 : 20) * notify.animY.getAnim();
         float expX = 3.5F;
         float expY = 4.5F;
         float x = (float)sr.getScaledWidth() - w - 3.5F;
         float y = (float)(sr.getScaledHeight() - 16) - 4.5F - hStep;
         float x2 = (float)sr.getScaledWidth() - 3.5F + width - width * notify.animX.getAnim();
         float y2 = (float)sr.getScaledHeight() - 4.5F - hStep;
         float extenderOut = 0.0F;
         if ((double)(time / max_time) > 0.8) {
            extenderOut = (time / max_time - 0.8F) * (float)(isDark ? 10 : 80);
         }

         float alphaPercent = notify.animX.getAnim();
         int c1 = ColorUtils.swapAlpha(isDark ? Integer.MIN_VALUE : colorize, (float)(isDark ? 205 : 80) * alphaPercent);
         int c3 = ColorUtils.swapAlpha(isDark ? Integer.MIN_VALUE : colorize2, (float)(isDark ? 90 : 6) * alphaPercent);
         GL11.glTranslated((double)(x - extenderOut), (double)y, 0.0);
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            0.5F, 0.5F, x2 - x - 0.5F, y2 - y - 0.5F, isDark ? 2.0F : 4.0F, isDark ? 1.0F : 2.5F, c1, c3, c3, c1, false, true, true
         );
         RenderUtils.resetBlender();
         float extX = isDark ? 7.0F : 0.5F;
         float extY = isDark ? 8.0F : 0.5F;
         float size = isDark ? 3.0F : 16.0F;
         drawIcon(type, alphaPercent, extX, extY, size, mode);
         font.drawStringWithShadow(
            "§f" + text + surf2 + massage, isDark ? 13.0F : 19.0F, isDark ? 5.5F : 5.0F, ColorUtils.swapAlpha(colorize2, 255.0F * alphaPercent)
         );
         GL11.glTranslated((double)(-x + extenderOut), (double)(-y), 0.0);
      }
   }

   public static enum type {
      ENABLE(ColorUtils.getColor(32, 143, 50), "Enable"),
      DISABLE(ColorUtils.getColor(175, 35, 37), "Disable"),
      STAFF(ColorUtils.getColor(92, 142, 255), "Staff"),
      ARMOR(ColorUtils.getColor(222, 31, 65), "Armor"),
      FADD(ColorUtils.getColor(190, 250, 140), "Friend added"),
      FDEL(ColorUtils.getColor(250, 140, 140), "Friend removed");

      private final int color;
      private final String icon;

      private type(int color, String icon) {
         this.color = color;
         this.icon = icon;
      }
   }
}
