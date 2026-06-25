package ru.govno.client.utils.Command.impl;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import org.lwjgl.Sys;
import ru.govno.client.Client;
import ru.govno.client.utils.Command.Command;
import ru.govno.client.utils.Math.ReplaceStrUtils;

public class ParserTab extends Command {
   Minecraft mc = Minecraft.getMinecraft();
   File parseDirectory = new File(Minecraft.getMinecraft().mcDataDir, "parses");
   private File file;

   public ParserTab() {
      super("ParserTab", new String[]{"parse", "pt", "parsetab"});
   }

   List<ParserTab.ParsedName> getPlayerStrings() {
      return this.mc
         .getConnection()
         .getPlayerInfoMap()
         .stream()
         .filter(net -> net.getGameProfile() != null)
         .map(
            net -> new ParserTab.ParsedName(
                  net.getGameProfile().getName(),
                  net.getDisplayName() == null
                     ? (
                        net.getPlayerTeam() != null
                           ? net.getPlayerTeam().formatString(net.getGameProfile().getName())
                           : net.getDisplayName().getUnformattedText()
                     )
                     : net.getDisplayName().getFormattedText(),
                  net.getPlayerTeam() == null ? null : net.getPlayerTeam().getColorPrefix()
               )
         )
         .toList();
   }

   List<String> getTabPrefixTypeCounted() {
      List<String> prefs = new ArrayList<>();

      for (ParserTab.ParsedName pn : this.getPlayerStrings()) {
         if (!prefs.stream().anyMatch(pref -> pref.equalsIgnoreCase(pn.prefix))) {
            prefs.add(pn.prefix);
         }
      }

      return prefs;
   }

   List<ParserTab.ParsedName> getParsesByPrefix(String prefix) {
      return this.getPlayerStrings().stream().filter(pn -> pn.prefix.equalsIgnoreCase(prefix)).collect(Collectors.toList());
   }

   List<ParserTab.ParsedNameGroup> getParsesGroups() {
      List<ParserTab.ParsedNameGroup> groupParses = new ArrayList<>();
      this.getTabPrefixTypeCounted().forEach(pref -> groupParses.add(new ParserTab.ParsedNameGroup(pref, this.getParsesByPrefix(pref))));
      return groupParses;
   }

   List<String> getParsesFinalStrings(boolean chat) {
      List<String> listParses = new ArrayList<>();
      this.getParsesGroups().forEach(parsed -> {
         String split = " ";
         listParses.add(split);
         String groupName = (parsed.prefix.isEmpty() ? "Players" : parsed.prefix + " `s") + " | count = " + parsed.parse.size();
         listParses.add(groupName);
         int count = 1;

         for (ParserTab.ParsedName parse : parsed.parse) {
            String counter = "№" + count + ": ";
            if (chat) {
               listParses.add(counter + "§r§7Name: " + parse.name + "§r§7|DName: " + parse.displayName + "§r§7|Pref: " + parse.prefix + "§r§7");
            } else {
               listParses.add(counter + "Name: " + parse.name);
               listParses.add(counter + "DisplayName: " + parse.displayName);
               listParses.add(counter + "Prefix: " + parse.displayName);
            }

            count++;
         }
      });
      return listParses;
   }

   public JsonObject getJsonParses(String ip) {
      JsonObject object = new JsonObject();
      object.addProperty("Parses:", "");
      int index1 = 0;

      for (String str : this.getParsesFinalStrings(false)) {
         object.addProperty("№-" + ++index1 + ": ", ReplaceStrUtils.deformatString(ReplaceStrUtils.deformatString(str, 1), 0));
      }

      return object;
   }

   void writeFile() {
      String ip = this.mc.isSingleplayer()
         ? "single"
         : (this.mc.getCurrentServerData() != null && this.mc.getCurrentServerData().serverIP != null ? this.mc.getCurrentServerData().serverIP : "unknown");
      this.parseDirectory = new File(Minecraft.getMinecraft().mcDataDir, "parses/");
      this.file = new File(this.parseDirectory, "parse(" + ip + ").txt");
      if (!this.parseDirectory.exists()) {
         this.parseDirectory.mkdirs();
      }

      if (!this.file.exists()) {
         try {
            this.file.createNewFile();
         } catch (Exception var9) {
            System.err.println("[!] failed to create file: " + this.file.getAbsolutePath());
            var9.printStackTrace();
            return;
         }
      }

      String contentPrettyPrint = new GsonBuilder().setPrettyPrinting().create().toJson((JsonElement)this.getJsonParses(ip));

      try (FileWriter writer = new FileWriter(this.file)) {
         writer.write(contentPrettyPrint);
      } catch (IOException var8) {
         System.err.println("[!] failed to write to file: " + this.file.getAbsolutePath());
         var8.printStackTrace();
      }
   }

   @Override
   public void onCommand(String[] args) {
      try {
         if (args[1].equalsIgnoreCase("write") || args[1].equalsIgnoreCase("get")) {
            List<String> parses = this.getParsesFinalStrings(false);
            if (parses.isEmpty()) {
               Client.msg("§f§lParse:§r §7Лист парсов пуст.", false);
               return;
            }

            this.writeFile();
            Client.msg("§f§lParse:§r §7Лист парсов записан в файл.", false);
         }

         if (args[1].equalsIgnoreCase("view") || args[1].equalsIgnoreCase("show")) {
            List<String> parses = this.getParsesFinalStrings(true);
            Client.msg("§f§lParse:§r §7Лист парсов" + (parses.isEmpty() ? " пуст." : " текущего сервера:"), false);
            parses.forEach(str -> Client.msg(str, false));
         }

         if (args[1].equalsIgnoreCase("dir") || args[1].equalsIgnoreCase("open")) {
            Client.msg("§f§lParse:§r §7Открываю папку парсов.", false);
            Sys.openURL(this.file.getAbsolutePath());
         }
      } catch (Exception var3) {
         Client.msg("§f§lParse:§r §7Комманда написана неверно.", false);
         Client.msg("§f§lParse:§r §7write: write/get", false);
         Client.msg("§f§lParse:§r §7print: view/show", false);
         Client.msg("§f§lParse:§r §7open folder: dir/open", false);
      }
   }

   class ParsedName {
      String name;
      String displayName;
      String prefix;

      ParsedName(String name, String displayName, String prefix) {
         this.name = name == null ? "null" : name;
         this.displayName = displayName == null ? "null" : displayName;
         this.prefix = prefix == null ? "null" : prefix;
      }
   }

   class ParsedNameGroup {
      String prefix;
      List<ParserTab.ParsedName> parse;

      ParsedNameGroup(String prefix, List<ParserTab.ParsedName> parse) {
         this.prefix = prefix;
         this.parse = new ArrayList<>();
         this.parse.addAll(parse);
      }
   }
}
