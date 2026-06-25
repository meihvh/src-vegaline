package ru.govno.client.utils.Discord.helpers;

import java.io.Serializable;
import javax.annotation.Nonnull;

@Nonnull
public class RPCButton implements Serializable {
   private final String url;
   private final String label;

   public String getLabel() {
      return this.label;
   }

   public String getUrl() {
      return this.url;
   }

   public static RPCButton create(String substring, String s) {
      substring = substring.substring(0, Math.min(substring.length(), 31));
      return new RPCButton(substring, s);
   }

   protected RPCButton(String label, String url) {
      this.label = label;
      this.url = url;
   }
}
