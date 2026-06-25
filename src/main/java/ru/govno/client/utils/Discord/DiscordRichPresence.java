package ru.govno.client.utils.Discord;

import com.sun.jna.Structure;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import ru.govno.client.utils.Discord.helpers.RPCButton;

public class DiscordRichPresence extends Structure {
   public String largeImageKey;
   public String largeImageText;
   public String smallImageText;
   public String partyPrivacy;
   public long startTimestamp;
   public String button_label_1;
   public int instance;
   public String partyId;
   public int partySize;
   public long endTimestamp;
   public String details;
   public String joinSecret;
   public String spectateSecret;
   public String smallImageKey;
   public String matchSecret;
   public String button_url_2;
   public String button_label_2;
   public String state;
   public String button_url_1;
   public int partyMax;

   public DiscordRichPresence() {
      this.setStringEncoding("UTF-8");
   }

   @Override
   protected List<String> getFieldOrder() {
      return Arrays.asList(
         "state",
         "details",
         "startTimestamp",
         "endTimestamp",
         "largeImageKey",
         "largeImageText",
         "smallImageKey",
         "smallImageText",
         "partyId",
         "partySize",
         "partyMax",
         "partyPrivacy",
         "matchSecret",
         "joinSecret",
         "spectateSecret",
         "button_label_1",
         "button_url_1",
         "button_label_2",
         "button_url_2",
         "instance"
      );
   }

   public static class Builder {
      private final DiscordRichPresence rpc = new DiscordRichPresence();

      public DiscordRichPresence.Builder setSmallImage(String s) {
         return this.setSmallImage(s, "");
      }

      public DiscordRichPresence.Builder setDetails(String s) {
         if (s != null && !s.isEmpty()) {
            this.rpc.details = s.substring(0, Math.min(s.length(), 128));
         }

         return this;
      }

      public DiscordRichPresence.Builder setLargeImage(String largeImageKey, String largeImageText) {
         this.rpc.largeImageKey = largeImageKey;
         this.rpc.largeImageText = largeImageText;
         return this;
      }

      public DiscordRichPresence.Builder setState(String s) {
         if (s != null && !s.isEmpty()) {
            this.rpc.state = s.substring(0, Math.min(s.length(), 128));
         }

         return this;
      }

      public DiscordRichPresence.Builder setInstance(boolean instance) {
         if ((this.rpc.button_label_1 == null || !this.rpc.button_label_1.isEmpty()) && (this.rpc.button_label_2 == null || !this.rpc.button_label_2.isEmpty())
            )
          {
            this.rpc.instance = instance ? 1 : 0;
            return this;
         } else {
            return this;
         }
      }

      public DiscordRichPresence.Builder setButtons(RPCButton o) {
         return this.setButtons(Collections.singletonList(o));
      }

      public DiscordRichPresence.Builder setSmallImage(String smallImageKey, String smallImageText) {
         this.rpc.smallImageKey = smallImageKey;
         this.rpc.smallImageText = smallImageText;
         return this;
      }

      public DiscordRichPresence.Builder setParty(String partyId, int partySize, int partyMax) {
         if ((this.rpc.button_label_1 == null || !this.rpc.button_label_1.isEmpty()) && (this.rpc.button_label_2 == null || !this.rpc.button_label_2.isEmpty())
            )
          {
            this.rpc.partyId = partyId;
            this.rpc.partySize = partySize;
            this.rpc.partyMax = partyMax;
            return this;
         } else {
            return this;
         }
      }

      public DiscordRichPresence.Builder setButtons(List<RPCButton> list) {
         if (list != null && !list.isEmpty()) {
            int min = Math.min(list.size(), 2);
            this.rpc.button_label_1 = list.get(0).getLabel();
            this.rpc.button_url_1 = list.get(0).getUrl();
            if (min == 2) {
               this.rpc.button_label_2 = list.get(1).getLabel();
               this.rpc.button_url_2 = list.get(1).getUrl();
            }
         }

         return this;
      }

      public DiscordRichPresence.Builder setStartTimestamp(OffsetDateTime offsetDateTime) {
         this.rpc.startTimestamp = offsetDateTime.toEpochSecond();
         return this;
      }

      public DiscordRichPresence.Builder setSecrets(String matchSecret, String joinSecret, String spectateSecret) {
         if ((this.rpc.button_label_1 == null || !this.rpc.button_label_1.isEmpty()) && (this.rpc.button_label_2 == null || !this.rpc.button_label_2.isEmpty())
            )
          {
            this.rpc.matchSecret = matchSecret;
            this.rpc.joinSecret = joinSecret;
            this.rpc.spectateSecret = spectateSecret;
            return this;
         } else {
            return this;
         }
      }

      public DiscordRichPresence.Builder setButtons(RPCButton rpcButton, RPCButton rpcButton2) {
         return this.setButtons(Arrays.asList(rpcButton, rpcButton2));
      }

      public DiscordRichPresence.Builder setStartTimestamp(long startTimestamp) {
         this.rpc.startTimestamp = startTimestamp;
         return this;
      }

      public DiscordRichPresence.Builder setSecrets(String joinSecret, String spectateSecret) {
         if ((this.rpc.button_label_1 == null || !this.rpc.button_label_1.isEmpty()) && (this.rpc.button_label_2 == null || !this.rpc.button_label_2.isEmpty())
            )
          {
            this.rpc.joinSecret = joinSecret;
            this.rpc.spectateSecret = spectateSecret;
            return this;
         } else {
            return this;
         }
      }

      public DiscordRichPresence.Builder setEndTimestamp(long endTimestamp) {
         this.rpc.endTimestamp = endTimestamp;
         return this;
      }

      public DiscordRichPresence.Builder setEndTimestamp(OffsetDateTime offsetDateTime) {
         this.rpc.endTimestamp = offsetDateTime.toEpochSecond();
         return this;
      }

      public DiscordRichPresence.Builder setLargeImage(String s) {
         return this.setLargeImage(s, "");
      }

      public DiscordRichPresence build() {
         return this.rpc;
      }
   }
}
