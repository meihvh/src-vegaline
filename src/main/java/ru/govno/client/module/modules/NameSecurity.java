package ru.govno.client.module.modules;

import java.util.List;
import net.minecraft.util.text.TextFormatting;
import ru.govno.client.module.Module;
import ru.govno.client.ui.login.GuiAltLogin;
import ru.govno.client.utils.Command.impl.Panic;

public class NameSecurity extends Module {
   public static Module get = new Module("", 0, null);

   public NameSecurity() {
      super("NameSecurity", 0, Module.Category.PLAYER);
      this.setDemand(1, 1);
      get = this;
   }

   public static String replacedName() {
      return TextFormatting.LIGHT_PURPLE + "+-LoveSex-+" + TextFormatting.RESET;
   }

   private static List<String> namesToReplaceList() {
      return List.of(mc.session.getUsername());
   }

   public static String replacedIfActive(String name) {
      if (get.actived && !Panic.stop && !(mc.currentScreen instanceof GuiAltLogin)) {
         List<String> names = namesToReplaceList();
         if (names.isEmpty()) {
            return name;
         } else {
            String repl = replacedName();

            for (String Name : names) {
               if (name.length() >= Name.length()) {
                  name = name.replace(Name, repl);
               }
            }

            return name;
         }
      } else {
         return name;
      }
   }
}
