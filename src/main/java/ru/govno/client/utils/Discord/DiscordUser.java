package ru.govno.client.utils.Discord;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

public class DiscordUser extends Structure {
   public String userId;
   public String username;
   @Deprecated
   public String discriminator;
   public String avatar;

   @Override
   protected List<String> getFieldOrder() {
      return Arrays.asList("userId", "username", "discriminator", "avatar");
   }
}
