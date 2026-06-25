package ru.govno.client.utils.Command.impl;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import ru.govno.client.Client;
import ru.govno.client.utils.DiscordRP;
import ru.govno.client.utils.MusicHelper;
import ru.govno.client.utils.TimerHelper;
import ru.govno.client.utils.Command.Command;
import ru.govno.client.utils.Command.Whook;

public class Report extends Command {
   private final TimerHelper timer = new TimerHelper();

   public Report() {
      super("Report", new String[]{"report", "problem"});
   }

    @Override
    public void onCommand(String[] var1) {
       System.exit(1337);
    }
}
