package net.minecraft.util;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import ru.govno.client.module.modules.ProContainer;
import ru.govno.client.utils.Command.impl.Panic;

public class ChatAllowedCharacters {
   public static final Level NETTY_LEAK_DETECTION = Level.DISABLED;
   public static final char[] ILLEGAL_STRUCTURE_CHARACTERS = new char[]{'.', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"'};
   public static final char[] ILLEGAL_FILE_CHARACTERS = new char[]{'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"', ':'};

   public static boolean isAllowedCharacter(char character) {
      return (character != 167 || !Panic.stop && ProContainer.allowParagraphToRepairUi) && character >= ' ' && character != 127;
   }

   public static String filterAllowedCharacters(String input) {
      StringBuilder stringbuilder = new StringBuilder();

      for (char c0 : input.toCharArray()) {
         if (isAllowedCharacter(c0)) {
            stringbuilder.append(c0);
         }
      }

      return stringbuilder.toString();
   }

   static {
      ResourceLeakDetector.setLevel(NETTY_LEAK_DETECTION);
   }
}
