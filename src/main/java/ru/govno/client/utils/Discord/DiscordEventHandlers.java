package ru.govno.client.utils.Discord;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
import ru.govno.client.utils.Discord.callbacks.DisconnectedCallback;
import ru.govno.client.utils.Discord.callbacks.ErroredCallback;
import ru.govno.client.utils.Discord.callbacks.JoinGameCallback;
import ru.govno.client.utils.Discord.callbacks.JoinRequestCallback;
import ru.govno.client.utils.Discord.callbacks.ReadyCallback;
import ru.govno.client.utils.Discord.callbacks.SpectateGameCallback;

public class DiscordEventHandlers extends Structure {
   public DisconnectedCallback disconnected;
   public JoinRequestCallback joinRequest;
   public SpectateGameCallback spectateGame;
   public ReadyCallback ready;
   public ErroredCallback errored;
   public JoinGameCallback joinGame;

   @Override
   protected List<String> getFieldOrder() {
      return Arrays.asList("ready", "disconnected", "errored", "joinGame", "spectateGame", "joinRequest");
   }

   public static class Builder {
      private final DiscordEventHandlers handlers = new DiscordEventHandlers();

      public DiscordEventHandlers build() {
         return this.handlers;
      }

      public DiscordEventHandlers.Builder disconnected(DisconnectedCallback disconnected) {
         this.handlers.disconnected = disconnected;
         return this;
      }

      public DiscordEventHandlers.Builder errored(ErroredCallback errored) {
         this.handlers.errored = errored;
         return this;
      }

      public DiscordEventHandlers.Builder ready(ReadyCallback ready) {
         this.handlers.ready = ready;
         return this;
      }

      public DiscordEventHandlers.Builder joinRequest(JoinRequestCallback joinRequest) {
         this.handlers.joinRequest = joinRequest;
         return this;
      }

      public DiscordEventHandlers.Builder joinGame(JoinGameCallback joinGame) {
         this.handlers.joinGame = joinGame;
         return this;
      }

      public DiscordEventHandlers.Builder spectateGame(SpectateGameCallback spectateGame) {
         this.handlers.spectateGame = spectateGame;
         return this;
      }
   }
}
