package ru.govno.client.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Vec2f;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.UControllers.DualSenseClientLib;

public class DualsenseCachedReader {
   public static boolean connected;
   public static boolean l;
   public static boolean r;
   public static boolean u;
   public static boolean d;
   public static boolean square;
   public static boolean cross;
   public static boolean circle;
   public static boolean triangle;
   public static boolean l1;
   public static boolean r1;
   public static boolean l3;
   public static boolean r3;
   public static boolean create;
   public static boolean options;
   public static boolean ps;
   public static boolean mute;
   public static boolean touch;
   public static boolean touchBtn;
   public static Vec2f lstick = new Vec2f(0.0F, 0.0F);
   public static Vec2f rstick = new Vec2f(0.0F, 0.0F);
   public static Vec2f touchPos = new Vec2f(0.0F, 0.0F);
   public static float l2;
   public static float r2;
   public static final ArrayList<String> tempData = new ArrayList<>();
   private static final ru.govno.client.utils.Math.TimerHelper timer = ru.govno.client.utils.Math.TimerHelper.TimerHelperReseted();

   public static void update() {
      try {
         if (timer.hasReached(4.0)) {
            timer.reset();
            connected = !DualSenseClientLib.tryCallData().isEmpty();
         }

         if (connected) {
            Map<String, String> dualsenseOutputInputs = DualSenseClientLib.getLastData();
            updateDataFrom(dualsenseOutputInputs);
         }
      } catch (Exception var1) {
         var1.printStackTrace();
         connected = false;
      }
   }

   public static void renderTest() {
      try {
         float x = 20.0F;
         float y = 20.0F;
         if (!connected) {
            return;
         }

         for (String dataNamed : tempData) {
            Fonts.comfortaaBold_18.addCachedrawStringWithShadow(dataNamed, x, y, -1);
            y += Fonts.comfortaaBold_18.getHeight() + 3.0F;
         }

         Fonts.comfortaaBold_18.drawAllCaches();
         float scaleTouch = 100.0F;
         RenderUtils.drawLightContureRect((double)x, (double)y, (double)(x + scaleTouch), (double)(y + scaleTouch), -1);
         if (touch) {
            RenderUtils.drawRect((double)x, (double)y, (double)(x + scaleTouch), (double)(y + scaleTouch), ColorUtils.getColor(255, touchBtn ? 100 : 20));
            float tX = x + scaleTouch * touchPos.x;
            float tY = y + scaleTouch * touchPos.y;
            RenderUtils.drawRect((double)(tX - 2.0F), (double)(tY - 2.0F), (double)(tX + 2.0F), (double)(tY + 2.0F), -1);
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }
   }

   private static List<String> updateDataFrom(Map<String, String> dualsenseOutputInputs) {
      if (dualsenseOutputInputs.isEmpty()) {
         tempData.clear();
         return tempData;
      } else {
         tempData.clear();
         String[] staticAllInputsNames = new String[]{
            "l",
            "r",
            "u",
            "d",
            "square",
            "cross",
            "circle",
            "triangle",
            "l1",
            "r1",
            "l2",
            "r2",
            "l3",
            "r3",
            "lstick",
            "rstick",
            "create",
            "options",
            "ps",
            "mute",
            "touchBtn",
            "touch",
            "touchpad"
         };

         for (String valueName : staticAllInputsNames) {
            String value = dualsenseOutputInputs.get(valueName);
            switch (valueName) {
               case "l":
                  tempData.add("l = " + (l = DualSenseClientLib.valueOfB(value)));
                  break;
               case "r":
                  tempData.add("r = " + (r = DualSenseClientLib.valueOfB(value)));
                  break;
               case "u":
                  tempData.add("u = " + (u = DualSenseClientLib.valueOfB(value)));
                  break;
               case "d":
                  tempData.add("d = " + (d = DualSenseClientLib.valueOfB(value)));
                  break;
               case "square":
                  tempData.add("square = " + (square = DualSenseClientLib.valueOfB(value)));
                  break;
               case "cross":
                  tempData.add("cross = " + (cross = DualSenseClientLib.valueOfB(value)));
                  break;
               case "circle":
                  tempData.add("circle = " + (circle = DualSenseClientLib.valueOfB(value)));
                  break;
               case "triangle":
                  tempData.add("triangle = " + (triangle = DualSenseClientLib.valueOfB(value)));
                  break;
               case "l1":
                  tempData.add("l1 = " + (l1 = DualSenseClientLib.valueOfB(value)));
                  break;
               case "r1":
                  tempData.add("r1 = " + (r1 = DualSenseClientLib.valueOfB(value)));
                  break;
               case "l2": {
                  float triggerPercent = DualSenseClientLib.valueOfF(value);
                  ArrayList var17 = tempData;
                  l2 = triggerPercent;
                  var17.add("l2 = " + triggerPercent);
                  break;
               }
               case "r2": {
                  float triggerPercent = DualSenseClientLib.valueOfF(value);
                  ArrayList var16 = tempData;
                  r2 = triggerPercent;
                  var16.add("r2 = " + triggerPercent);
                  break;
               }
               case "l3":
                  tempData.add("l3 = " + (l3 = DualSenseClientLib.valueOfB(value)));
                  break;
               case "r3":
                  tempData.add("r3 = " + (r3 = DualSenseClientLib.valueOfB(value)));
                  break;
               case "lstick": {
                  Vec2f stickCoord = DualSenseClientLib.valueOfV(value);
                  ArrayList var15 = tempData;
                  lstick = stickCoord;
                  var15.add("lstick = " + stickCoord.x + ", " + lstick.y);
                  break;
               }
               case "rstick": {
                  Vec2f stickCoord = DualSenseClientLib.valueOfV(value);
                  ArrayList var14 = tempData;
                  rstick = stickCoord;
                  var14.add("rstick = " + stickCoord.x + ", " + rstick.y);
                  break;
               }
               case "create":
                  tempData.add("create = " + (create = DualSenseClientLib.valueOfB(value)));
                  break;
               case "options":
                  tempData.add("options = " + (options = DualSenseClientLib.valueOfB(value)));
                  break;
               case "ps":
                  tempData.add("ps = " + (ps = DualSenseClientLib.valueOfB(value)));
                  break;
               case "mute":
                  tempData.add("mute = " + (mute = DualSenseClientLib.valueOfB(value)));
                  break;
               case "touch":
                  tempData.add("touchBtn = " + (touchBtn = DualSenseClientLib.valueOfB(value)));
                  break;
               case "touchpad":
                  Vec2f touchCoord = DualSenseClientLib.valueOfV(value);
                  tempData.add("touch = " + (touch = touchCoord != null));
                  if (touchCoord == null) {
                     tempData.add("touchPos = " + (touchPos = new Vec2f(0.5F, 0.5F)).x + ", " + touchPos.y);
                  } else {
                     ArrayList var10000 = tempData;
                     touchPos = touchCoord;
                     var10000.add("touchPos = " + touchCoord.x + ", " + touchPos.y);
                  }
            }
         }

         return tempData;
      }
   }
}
