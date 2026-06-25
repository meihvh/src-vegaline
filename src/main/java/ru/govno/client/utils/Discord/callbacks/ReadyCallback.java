package ru.govno.client.utils.Discord.callbacks;

import com.sun.jna.Callback;
import ru.govno.client.utils.Discord.DiscordUser;

public interface ReadyCallback extends Callback {
   void apply(DiscordUser var1);

   void apply(String var1);
}
