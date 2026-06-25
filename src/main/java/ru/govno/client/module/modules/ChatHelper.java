package ru.govno.client.module.modules;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import org.apache.commons.lang3.RandomStringUtils;
import ru.govno.client.Client;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventReceivePacket;
import ru.govno.client.event.events.EventSendPacket;
import ru.govno.client.friendsystem.Friend;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.CrystalField;
import ru.govno.client.utils.MusicHelper;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.ReplaceStrUtils;
import ru.govno.client.utils.Math.TimerHelper;

public class ChatHelper extends Module {
   public static ChatHelper get;
   public BoolSettings CensureText;
   public BoolSettings NoExtraCopy;
   public BoolSettings AntiLog4j;
   public BoolSettings AutoLogin;
   public BoolSettings AutoTpaAccept;
   public BoolSettings OnlyFriends;
   public BoolSettings ChatSpammer;
   public BoolSettings InGlobalChat;
   public BoolSettings SpamBypassTexts;
   public BoolSettings SpamOnTotemPop;
   public BoolSettings SelfNameHighlight;
   public BoolSettings SelfNameNotify;
   public BoolSettings FriendsNameHighlight;
   public BoolSettings FriendsNameNotify;
   public BoolSettings EndlessStory;
   public BoolSettings ChatPrintPrefix;
   public BoolSettings ChatPrintSuffix;
   public BoolSettings GrammarSendFixer;
   public ModeSettings LoginPass;
   public ModeSettings SpamMode;
   public ModeSettings ChatPrefix;
   public ModeSettings ChatSuffix;
   public FloatSettings SpamDelay;
   boolean accept;
   boolean gotoSend = true;
   TimerHelper times = new TimerHelper();
   private boolean send;
   TimerHelper timer = new TimerHelper();
   private final String[] osks = new String[]{
      "не чувствую",
      "ха eбaть ты слабый",
      "чё с ебалом сын шлюхи",
      "купи вегалайн читикс реал а то сосёшь",
      "у тебя в очке мой меч хых",
      "кто из нас умрёт тот пидор",
      "скули псина глотай мои криты сын урны",
      "чё по хп xyйня нищaя",
      "cоси мне старательнее пж",
      "eбaть твой пиздaк сыпется",
      "на нaxyй сын блядиHы",
      "ебу тебя как твою мамку прям",
      "чё ебaть попасть не можешь cвинья бляTь7",
      "oткиcaй бoмжиха eбaнaя",
      "сын шлюхи сын шлюхииии",
      "твоя maть у меня заперта в туалете ободок облизывает чтоб я её покормил",
      "твоя maть в лаве купается",
      "ты умер ёптa",
      "у тебя пиcьки нет",
      "твой тампон провалился слишком глубоко",
      "цветочный лox толстый сиськастый  хyй",
      "ты думал что закинул загубу насвай а закинул мой xyй",
      "ты был создан чтоб сoсать мне",
      "я вaxye какой ты бич",
      "oтxвaтывай nиздюлей шeгол eбyчий",
      "твoя мaмaша тaкая жиpная что её хoдьба зacтaвляет зeмлю крyтиться",
      "oтпинал твoю мaть и тeбя oтпинaю сyчкa",
      "мамку твою хуем рублю",
      "мать твою пнул хуем",
      "мать твою хуем вскрыл",
      "хехе сосешь мой хуй талантливо",
      "мать твоя на хую тухнет",
      "сосешь аналом",
      "сосешь как можешь",
      "твоя мать чавкает мой хуй",
      "твоя мамаша уехала нахуй и больше не вернулась",
      "твоя мама такая жирная что чёрное море её тень",
      "как сосётся а хач7",
      "отмудохаю тебя хуесос как некеда",
      "на по ебалу чубрик",
      "заглаатывай мою жирную елдину"
   };
   private final TimerHelper timeSndMsg = TimerHelper.TimerHelperReseted();
   public final List<ChatHelper.EntityDeathMemory> DEATH_MEMORIES_LIST = new ArrayList<>();

   public ChatHelper() {
      super("ChatHelper", 0, Module.Category.MISC);
      this.settings.add(this.CensureText = new BoolSettings("CensureText", true, this));
      this.settings.add(this.NoExtraCopy = new BoolSettings("NoExtraCopy", true, this));
      this.settings.add(this.AntiLog4j = new BoolSettings("AntiLog4j", true, this));
      this.settings.add(this.AutoLogin = new BoolSettings("AutoLogin", true, this));
      this.settings
         .add(
            this.LoginPass = new ModeSettings(
               "LoginPass", "123123123", this, new String[]{"123123123", "Mam6a_xu9imba", "zalupa228"}, () -> this.AutoLogin.getBool()
            )
         );
      this.settings.add(this.AutoTpaAccept = new BoolSettings("AutoTpaAccept", true, this));
      this.settings.add(this.OnlyFriends = new BoolSettings("OnlyFriends", true, this, () -> this.AutoTpaAccept.getBool()));
      this.settings.add(this.ChatSpammer = new BoolSettings("ChatSpammer", false, this));
      this.settings.add(this.InGlobalChat = new BoolSettings("InGlobalChat", false, this, () -> this.ChatSpammer.getBool()));
      this.settings.add(this.SpamBypassTexts = new BoolSettings("SpamBypassTexts", true, this, () -> this.ChatSpammer.getBool()));
      this.settings.add(this.SpamDelay = new FloatSettings("SpamDelay", 4000.0F, 10000.0F, 100.0F, this, () -> this.ChatSpammer.getBool()));
      this.settings
         .add(
            this.SpamMode = new ModeSettings(
               "SpamMode", "Bulling", this, new String[]{"WarpHVH", "Client", "/tpahere", "Bulling", "Citations", "KillEzz"}, () -> this.ChatSpammer.getBool()
            )
         );
      this.settings
         .add(
            this.SpamOnTotemPop = new BoolSettings(
               "SpamOnTotemPop", true, this, () -> this.ChatSpammer.getBool() && this.SpamMode.getMode().equalsIgnoreCase("KillEzz")
            )
         );
      this.settings.add(this.SelfNameHighlight = new BoolSettings("SelfNameHighlight", true, this));
      this.settings.add(this.SelfNameNotify = new BoolSettings("SelfNameNotify", true, this, () -> this.SelfNameHighlight.getBool()));
      this.settings.add(this.FriendsNameHighlight = new BoolSettings("FriendsNameHighlight", true, this));
      this.settings.add(this.FriendsNameNotify = new BoolSettings("FriendsNameNotify", true, this, () -> this.FriendsNameHighlight.getBool()));
      this.settings.add(this.EndlessStory = new BoolSettings("EndlessStory", false, this));
      this.settings.add(this.ChatPrintPrefix = new BoolSettings("ChatPrintPrefix", false, this));
      this.settings
         .add(
            this.ChatPrefix = new ModeSettings(
               "ChatPrefix",
               "ColorRed",
               this,
               new String[]{
                  "ColorBlack",
                  "ColorDarkBlue",
                  "ColorDarkGreen",
                  "ColorDarkAqua",
                  "ColorDarkRed",
                  "ColorDarkPurple",
                  "ColorGold",
                  "ColorGray",
                  "ColorDarkGray",
                  "ColorBlue",
                  "ColorGreen",
                  "ColorAqua",
                  "ColorRed",
                  "ColorLightPurple",
                  "ColorYellow",
                  "ColorWhite",
                  "FormatBold",
                  "FormatStrike",
                  "FormatUnderline",
                  "FormatItalic",
                  ".i.",
                  "йоу",
                  "PocoX3Pro",
                  "Мяу",
                  "6090TI",
                  "[VL]",
                  "[Vegaline]",
                  "[Вегуля]"
               },
               () -> this.ChatPrintPrefix.getBool()
            )
         );
      this.settings.add(this.ChatPrintSuffix = new BoolSettings("ChatPrintSuffix", false, this));
      this.settings
         .add(
            this.ChatSuffix = new ModeSettings(
               "ChatSuffix", "Vegaline", this, new String[]{"Vegaline", "VL", "RandomSmile"}, () -> this.ChatPrintSuffix.getBool()
            )
         );
      this.settings.add(this.GrammarSendFixer = new BoolSettings("GrammarSendFixer", false, this));
      this.setDemand(0, 2);
      get = this;
   }

   public static String ruAlphabetToEn(String input) {
      String[] ruen = new String[]{
         "йq",
         "цw",
         "уe",
         "кr",
         "еt",
         "нy",
         "гu",
         "шi",
         "щo",
         "зp",
         "фa",
         "ыs",
         "вd",
         "аf",
         "пg",
         "рh",
         "оj",
         "лk",
         "дl",
         "яz",
         "чx",
         "сc",
         "мv",
         "иb",
         "тn",
         "ьm",
         "б,",
         "ю.",
         "х[",
         "ъ]",
         "ж;",
         "э'"
      };
      String input1 = "";

      for (char theChar : input.toCharArray()) {
         String alphabetOut = String.valueOf(theChar);

         for (int i = 0; i < ruen.length; i++) {
            char[] ruenArray = ruen[i].toCharArray();
            String o0 = String.valueOf(ruenArray[0]);
            String o1 = String.valueOf(ruenArray[1]);
            if (alphabetOut.toLowerCase().equals(o0)) {
               alphabetOut = o1;
               break;
            }
         }

         input1 = input1 + alphabetOut;
      }

      return input1.isEmpty() ? input : input1;
   }

   public static String fixGrammarInputChatString(String input) {
      if (!Panic.stop && get != null && get.isActived() && get.GrammarSendFixer.getBool()) {
         String replacementPrefix = null;
         boolean canRevertRoToEn = false;
         Map<String, String> replacements = new HashMap<>();
         if (input.startsWith("/")) {
            replacementPrefix = "/";
            canRevertRoToEn = true;
            replacements.put("rpt", "rtp");
            replacements.put("ptr", "rtp");
            replacements.put("prt", "rtp");
            replacements.put("tpr", "rtp");
            replacements.put("trp", "rtp");
            replacements.put("кез", "rtp");
            replacements.put("екз", "rtp");
            replacements.put("езк", "rtp");
            replacements.put("кзе", "rtp");
            replacements.put("зек", "rtp");
            replacements.put("зке", "rtp");
            replacements.put("homr", "home");
            replacements.put("hpme", "home");
            replacements.put("hpmr", "home");
            replacements.put("рщьу", "home");
            replacements.put("рщьк", "home");
            replacements.put("рзьу", "home");
            replacements.put("рзьк", "home");
            replacements.put("рги", "hub");
            replacements.put("риг", "hub");
            replacements.put("hbu", "hub");
            replacements.put("дщиин", "lobby");
            replacements.put("spwn", "spawn");
            replacements.put("spawm", "spawn");
            replacements.put("spaem", "spawn");
            replacements.put("ызфцт", "spawn");
            replacements.put("ызць", "spawn");
            replacements.put("ыщфць", "spawn");
            replacements.put("лше ыефке", "kit start");
            replacements.put("лше вуафгде", "kit default");
         } else if (input.startsWith(".")) {
            replacementPrefix = ".";
            canRevertRoToEn = true;
         }

         if (replacementPrefix != null) {
            if (canRevertRoToEn) {
               String ruToEn = ruAlphabetToEn(input);
               if (!input.equals(ruToEn)) {
                  Client.msg("§f§lModules:§r §7[§lChatHelper§r§7]: исправил в сообщении " + input + " на " + ruToEn + ".", false);
                  input = ruToEn;
               }
            }

            String inputLower = input.toLowerCase();

            for (Entry<String, String> entry : replacements.entrySet()) {
               String key = entry.getKey();
               String value = entry.getValue();
               String str = replacementPrefix + key;
               if (inputLower.startsWith(str)) {
                  String replTo = replacementPrefix + value;
                  input = inputLower.replaceAll(str, replTo);
                  Client.msg("§f§lModules:§r §7[§lChatHelper§r§7]: исправил в сообщении " + str + " на " + replTo + ".", false);
                  break;
               }
            }
         }
      }

      return input;
   }

   public boolean isInfiniteChatHistory() {
      return this.actived && this.EndlessStory.getBool();
   }

   public boolean highlightSelf(String byChat) {
      return this.isActived() && this.SelfNameHighlight.getBool() ? byChat.contains(mc.getSession().getUsername()) : false;
   }

   public boolean highlightFriends(String byChat) {
      return this.isActived() && this.FriendsNameHighlight.getBool() ? Client.friendManager.isFriendDisplayReturn(byChat) != null : false;
   }

   @EventTarget
   public void onPacketReceive(EventReceivePacket event) {
      if (event.getPacket() instanceof SPacketChat packet && this.isActived()) {
         if (this.AntiLog4j.getBool() && (packet.chatComponent.getFormattedText().startsWith("${") || packet.chatComponent.getFormattedText().contains("}"))) {
            event.setCancelled(true);
            Minecraft.player.addChatMessage(new TextComponentString("Log4jFixer удалил Log4j exploit сообщение."));
         }

         String message = packet.chatComponent.getFormattedText();
         if ((this.highlightSelf(message) && this.SelfNameNotify.getBool() || this.highlightFriends(message) && this.FriendsNameNotify.getBool())
            && this.timeSndMsg.hasReached(450.0)
            && !(mc.currentScreen instanceof GuiChat)
            && Minecraft.player != null
            && Minecraft.player.ticksExisted > 60) {
            MusicHelper.playSound("namenotify2.wav", 0.15F);
            this.timeSndMsg.reset();
         }

         if (this.AutoLogin.getBool()) {
            String str = packet.chatComponent.getFormattedText();
            str = str.toLowerCase();
            str = ReplaceStrUtils.fixString(str);
            str = ReplaceStrUtils.deformatString(str, 1);
            int login = 0;
            if (str.contains("/reg") || str.contains("зарегистрир")) {
               this.times.reset();
               login = 2;
            }

            if (str.contains("/l")) {
               this.times.reset();
               login = 1;
            }

            if (this.AutoLogin.getBool() && login != 0 && this.timer.hasReached(1500.0)) {
               String pass = this.LoginPass.getMode();
               if (login == 2) {
                  Minecraft.player.sendChatMessage("/register " + pass + " " + pass);
                  Client.msg("§f§lModules:§r §7[§lChatHelper§r§7]: Регистрирую аккаунт.", false);
                  StringSelection selection = new StringSelection(pass);
                  Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                  clipboard.setContents(selection, selection);
                  Client.msg("§f§lModules:§r §7[§lChatHelper§r§7]: Пароль скопирован в буффер обмена.", false);
               } else {
                  Minecraft.player.sendChatMessage("/login " + pass);
                  Client.msg("§f§lModules:§r §7[§lChatHelper§r§7]: Логиню аккаунт паролем из настройки.", false);
               }

               this.timer.reset();
            }
         }

         if (this.AutoTpaAccept.getBool()) {
            String strx = packet.chatComponent.getUnformattedText();
            strx = strx.toLowerCase();
            strx = ReplaceStrUtils.fixString(strx);
            strx = ReplaceStrUtils.deformatString(strx, 1);
            strx = strx.trim();
            strx = strx.toLowerCase();
            boolean strContain = (strx.contains("телепорт") || strx.contains("teleport")) && (strx.contains("вам") || strx.contains("you"));
            if (!this.OnlyFriends.getBool()) {
               this.accept = strContain;
            } else {
               for (Friend friend : Client.friendManager.getFriends()) {
                  if (strx.contains(friend.getName().toLowerCase())) {
                     this.accept = strContain;
                     if (this.accept) {
                        break;
                     }
                  }
               }
            }
         }

         return;
      }
   }

   @Override
   public void onUpdateLimitedDelay() {
      if ((this.ChatPrintSuffix.getBool() || this.ChatPrintPrefix.getBool()) && !this.gotoSend) {
         this.gotoSend = true;
      }

      if (this.accept && this.AutoTpaAccept.getBool()) {
         Minecraft.player.sendChatMessage("/tpaccept");
         Client.msg("§f§lModules:§r §7[§lChatHelper§r§7]: Принимаю телепорт.", false);
         this.accept = false;
      }

      if (this.ChatSpammer.getBool()) {
         try {
            this.controlMemories(this.SpamMode.getMode().equalsIgnoreCase("KillEzz"));
         } catch (Exception var4) {
            var4.printStackTrace();
         }

         if (!this.SpamMode.getMode().equalsIgnoreCase("KillEzz") && this.timer.hasReached((double)((int)this.SpamDelay.getFloat()))) {
            String spamMode = this.SpamMode.currentMode;
            if (spamMode.equalsIgnoreCase("WarpHVH")) {
               if (this.SpamBypassTexts.getBool()) {
                  Minecraft.player
                     .connection
                     .sendPacket(
                        new CPacketChatMessage(
                           (this.InGlobalChat.getBool() ? "!" : "")
                              + RandomStringUtils.randomAlphanumeric(5)
                              + " all warp hvh zames z4 ezzz go hvh "
                              + RandomStringUtils.randomAlphanumeric(5)
                        )
                     );
               } else {
                  Minecraft.player
                     .connection
                     .sendPacket(new CPacketChatMessage((this.InGlobalChat.getBool() ? "!" : "") + "all warp hvh zames z4 ezzz go hvh"));
               }
            } else if (spamMode.equalsIgnoreCase("Client")) {
               if (this.SpamBypassTexts.getBool()) {
                  Minecraft.player
                     .connection
                     .sendPacket(
                        new CPacketChatMessage(
                           (this.InGlobalChat.getBool() ? "!" : "")
                              + RandomStringUtils.randomAlphanumeric(5)
                              + " VegaLineClient is a client that pisses your shit clients "
                              + RandomStringUtils.randomAlphanumeric(5)
                        )
                     );
               } else {
                  Minecraft.player
                     .connection
                     .sendPacket(new CPacketChatMessage((this.InGlobalChat.getBool() ? "!" : "") + "vegaline client is a client that pisses your shit clients"));
               }
            } else if (spamMode.equalsIgnoreCase("/tpahere")) {
               for (EntityPlayer e : GuiPlayerTabOverlay.getPlayers2()) {
                  if (e != null && e != Minecraft.player) {
                     Minecraft.player.connection.sendPacket(new CPacketChatMessage("/tpahere " + e.getName()));
                  }
               }
            } else if (spamMode.equalsIgnoreCase("Citations")) {
               String[] citates = new String[]{
                  "Почему женщины много говорят, а мужчины много думают. У женщин двое губ, а у мужчин две головы.",
                  "Гиппопотам — это бегемот или просто очень крутой опотам?",
                  "Я не разбрасываюсь словами, мне потом их трудно подбирать.",
                  "Чем старше человек, тем больше ему лет.",
                  "От короновируса умирали даже те, кто раньше никогда не умирал.",
                  "С помощью дверей можно зайти домой.",
                  "Ещё вчера я думал, завтра будет сегодня.",
                  "Если чего-то не знаешь, просто спроси.",
                  "Если вы провалились в яму, то идите домой за лестницей.",
                  "Если почувствуете, что тонете, просто плывите к берегу.",
                  "Ты не сможешь ничего сказать, кроме слов.",
                  "Левый глаз левее правого.",
                  "Я не настолько глуп, как вы думаете. Просто, у меня столько мыслей в голове, что рот не успевает за ними.",
                  "У мужчины две головы. Одной он думает, а вторая у него на плечах.",
                  "Одна ошибка - и ты ошибся.",
                  "Волк - это не работа... Работа - это ворк, а волк - это ходить.",
                  "Если тебя мучает жажда, то борис лов.",
                  "Порхай как бабочка. Жаль, что твоя мать сдохла.",
                  "В мире есть много типов животных. Простейшие, губки, черви, хордовые, членистоногие, моллюски и целепукстиал юзеры.",
                  "В мире есть много типов животных. Простейшие, губки, черви, хордовые, членистоногие, моллюски и обосрансив юзеры.",
                  "В мире есть много типов животных. Простейшие, губки, черви, хордовые, членистоногие, моллюски и насрултан юзеры.",
                  "Иди хоть что-то нормальное посмотри кроме ютуба.",
                  "Спи там, где волки ссать боятся.",
                  "Если пошёл дождь и не где укрыться, а ты боишься промокнуть - заставь дождь промокнуть вместо тебя.",
                  "Если на тебя напали и воткнули нож - пропиши двоечку ножу чтоб не втыкал.",
                  "Чувствуешь, что время быстро идёт? Догони его и попроси идти немного помедленее.",
                  "Запомни и не забудь - на березах яблоки не растут! Там только бананы.",
                  "Увидел бабушку и хочешь перевести её через дорогу? Запомни, бабки - в жизни не главное.",
                  "Не имей сто друзей, а имей их подруг.",
                  "Любишь срать, люби и унитаз смывать.",
                  "Если тебе больно - не болей.",
                  "Лучше жопой съехать с терки, чем учиться на пятерки.",
                  "Дыши там, где воздух есть и сможешь дышать.",
                  "Если захотелось одновременно и ссать, и пить - считай обе проблемы уже решены.",
                  "Кто не воин, тот не воин.",
                  "Чтобы холодная вода стала горячей, ее нужно подогреть.",
                  "Я не разбрасываюсь словами, мне потом их трудно подбирать.",
                  "Чем старше человек, тем больше ему лет.",
                  "Они хотели обосрать нас, но забыл снять штаны.",
                  "Если закрыть глаза, становится темно.",
                  "Пока не доказано - не ебёт что сказано.",
                  "Не опоздал, а задержался по семейным обстоятельствам.",
                  "Хороший человек плохой воздух в себе держать не будет.",
                  "Пей там где конь пьёт, ведь паразиты не убивают, они делают нас сильнее.",
                  "Обидно что я живу в мире, где у огурца есть горькая попка, но нет сладкой письки.",
                  "Э... Эм... бля опять забыл что такое альцгеймер.",
                  "Лучше посрать и опоздать, чем прийти и обосраться.",
                  "Если тебе отрубили ногу, приклей её клеем.",
                  "Глупый человек жалуется на дырку в кармане. Умный использует ее, чтобы почесать себе яйца.",
                  "Один мужчина сказал очень мудрую вещь, но я ее забыл.",
                  "Если ты заблудился в лесу, иди домой.",
                  "Никогда не сдавайся, ты же не квартира.",
                  "Если ты хочешь пить, а твой друг ссать, то держитесь друг от друга подальше.",
                  "Чтобы сон прошел удачно, не забудь подергать смачно.",
                  "Если не можешь уснуть, просто спи.",
                  "Когда комар сядет тебе на яйцо, лишь тогда ты поймешь что точность важнее силы.",
                  "Мне даже играть не нужно, я знаю что ты, кал, допустил критическую ошибку - запустился на кристальный кит.",
                  "Новости: найден труп очередной жертвы насилия юзера клиента vegaline.",
                  "Внимание: на сервере обнаружен крайне опасный серийный киллер, и это я.",
                  "Я тут щас всех жахну и вновь уйду спать довольным собой.",
                  "Буду ебать всех кто движется а кто не движется, того подвину.",
                  "Буду пялить всех кто движется а кто не движется, того подвину и продолжу пялить.",
                  "Чем кормить курицу что-бы она сносила не яйца а ебальники?",
                  "Что бы я дал человеку, у которого все есть? Я дал бы ему в челюсть.",
                  "Вот так всегда: хорошо скажешь – сглазишь, плохо – накаркаешь!",
                  "Самая красивая месть недоброжелателю, это – забыть о его существовании.",
                  "От того, как ты посмотришь, зависит то, что ты увидишь.",
                  "Сегодняшний прогноз: 100% шанс на победу.",
                  "Я не сошел с ума.. Я просто продал его в Интернете.",
                  "Надежда есть всегда, она придёт и задавит тебя пузом.",
                  "Никто не идеален. Но это не про меня!",
                  "Хочешь конфетку? На бери... глк глк глк...",
                  "Если ты это читаешь, пошёл нахуй.",
                  "Если хочешь срать и тебе лень снимать портки, просто сри.",
                  "Я все еще играю в игры и получаю удовольствие, а ты скучный аморал.",
                  "Ты вышел в топ мира.. по наличию хромосом.",
                  "Пиздец. Одним словом описал ситуацию в стране и в мире, рассказал о проблемах.",
                  "И пизда раз в год стреляет.",
                  "Военком прислал повестку, пошёл на почту отправил назад.",
                  "Не можешь срать не мучай жопу.",
                  "Смотри в пол когда я перед тобой.",
                  "Пивка для рывка, водочки для блатной походочки.",
                  "Матернул училку, её лицо набухло. Ща как бабахнет.",
                  "Выебал медузу + познал морской мир.",
                  "Играл в гольф, твоя мама лежала и вдруг дёрнулась.",
                  "Делай как надо, как не надо не делай.",
                  "Сходил в качалку, позанимался.. Тренажёр стал сильнее.",
                  "Жи Ши пиши от души.",
                  "Когда я иду даже воздух отходит.",
                  "Мёртвое море знаешь? Это я убил!",
                  "Не откладывай на завтра, то, что можно сделать после завтра.",
                  "Я бью два раза, один в ебало, другой по крышке гроба.",
                  "Без бумажки ты какашка, а с бумажкой какашка с бумажкой.",
                  "Фуух! Пробурил пещеру, бля эт плева была. Андрей??"
               };
               String msg = citates[(int)(((float)citates.length - 1.0F) * (float)Math.random() * (((float)citates.length - 1.0F) / (float)citates.length))];
               Minecraft.player.connection.sendPacket(new CPacketChatMessage((this.InGlobalChat.getBool() ? "!" : "") + msg));
            }

            if (spamMode.equalsIgnoreCase("Bulling") && !this.send) {
               this.send = true;
            }

            this.timer.reset();
         }
      }
   }

   @EventTarget
   public void onPacketSend(EventSendPacket event) {
      if (event.getPacket() instanceof CPacketChatMessage packet && !packet.getMessage().isEmpty()) {
         this.timeSndMsg.reset();
      }

      if (event.getPacket() instanceof CPacketUseEntity packet
         && mc.world != null
         && this.SpamMode.getMode().equalsIgnoreCase("Bulling")
         && packet.getAction() == CPacketUseEntity.Action.ATTACK) {
         if (packet.getEntityFromWorld(mc.world) == null) {
            return;
         }

         String nick = packet.getEntityFromWorld(mc.world).getName().replace("entity.", "").replace(".name", "");
         if (this.send) {
            String text = this.osks[new Random().nextInt(this.osks.length)];
            if (this.SpamBypassTexts.getBool()) {
               Minecraft.player
                  .connection
                  .sendPacket(
                     new CPacketChatMessage(
                        (this.InGlobalChat.getBool() ? "!" : "")
                           + RandomStringUtils.randomAlphanumeric(5)
                           + " "
                           + nick
                           + " "
                           + text
                           + " "
                           + RandomStringUtils.randomAlphanumeric(5)
                     )
                  );
               this.send = false;
            } else {
               Minecraft.player.connection.sendPacket(new CPacketChatMessage((this.InGlobalChat.getBool() ? "!" : "") + nick + " " + text));
               this.send = false;
            }
         }
      }

      if (event.getPacket() instanceof CPacketChatMessage massagePacket && this.gotoSend) {
         String massage;
         if ((massage = massagePacket.getMessage()) != null
            && !massage.isEmpty()
            && !Arrays.asList("/", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "#")
               .stream()
               .anyMatch(badStart -> massage.replace(" ", "").startsWith(badStart))
            && massage.length() > 6) {
            if (this.ChatPrintSuffix.getBool()) {
               String[] apps = new String[]{" ", "[", "]"};
               String preCode = this.ChatSuffix.currentMode;
               switch (preCode) {
                  case "Vegaline":
                     massagePacket.appendMessage(apps[0] + apps[1] + this.ChatSuffix.currentMode + apps[2]);
                     break;
                  case "VL":
                     massagePacket.appendMessage(apps[0] + apps[1] + this.ChatSuffix.currentMode + apps[2]);
                     break;
                  case "RandomSmile":
                     String[] smiles = new String[]{
                        "(-_-)",
                        "(8_8)",
                        "(>-<)",
                        ":3",
                        ":D",
                        ":-|",
                        "(x.x)",
                        "Zzzz",
                        "(O_o)",
                        "(o_O)",
                        "(-.-)",
                        "(^_~)",
                        "(u_u)",
                        "(>__<)",
                        "(^_-)",
                        "(^^)",
                        "-_-;",
                        "(^-^)",
                        "(^3^)",
                        "^o^",
                        "u_u",
                        "n_n",
                        "＄=＄",
                        "'-'",
                        "X-X",
                        "T_T",
                        "<0_0>",
                        "G_G",
                        "6_9"
                     };
                     String smile = smiles[(int)(((float)smiles.length - 1.0F) * (float)Math.random() * (((float)smiles.length - 1.0F) / (float)smiles.length))];
                     massagePacket.appendMessage(apps[0] + smile);
               }
            }

            if (this.ChatPrintPrefix.getBool()) {
               String prefix = "";
               String preCode = "&";
               String var19 = this.ChatPrefix.getMode();

               prefix = switch (var19) {
                  case "ColorBlack" -> preCode + TextFormatting.BLACK.formattingCode;
                  case "ColorDarkBlue" -> preCode + TextFormatting.DARK_BLUE.formattingCode;
                  case "ColorDarkGreen" -> preCode + TextFormatting.DARK_GREEN.formattingCode;
                  case "ColorDarkAqua" -> preCode + TextFormatting.DARK_AQUA.formattingCode;
                  case "ColorDarkRed" -> preCode + TextFormatting.DARK_RED.formattingCode;
                  case "ColorDarkPurple" -> preCode + TextFormatting.DARK_PURPLE.formattingCode;
                  case "ColorGold" -> preCode + TextFormatting.GOLD.formattingCode;
                  case "ColorGray" -> preCode + TextFormatting.GRAY.formattingCode;
                  case "ColorDarkGray" -> preCode + TextFormatting.DARK_GRAY.formattingCode;
                  case "ColorBlue" -> preCode + TextFormatting.BLUE.formattingCode;
                  case "ColorAqua" -> preCode + TextFormatting.AQUA.formattingCode;
                  case "ColorRed" -> preCode + TextFormatting.RED.formattingCode;
                  case "ColorLightPurple" -> preCode + TextFormatting.LIGHT_PURPLE.formattingCode;
                  case "ColorYellow" -> preCode + TextFormatting.YELLOW.formattingCode;
                  case "ColorWhite" -> preCode + TextFormatting.WHITE.formattingCode;
                  case "FormatBold" -> preCode + TextFormatting.BOLD.formattingCode;
                  case "FormatStrike" -> preCode + TextFormatting.STRIKETHROUGH.formattingCode;
                  case "FormatUnderline" -> preCode + TextFormatting.UNDERLINE.formattingCode;
                  case "FormatItalic" -> preCode + TextFormatting.ITALIC.formattingCode;
                  default -> prefix + this.ChatPrefix.getMode() + " ";
               };
               if (!prefix.isEmpty()) {
                  boolean isGlobal = massagePacket.getMessage().startsWith("!");
                  String msgPrePrefix = isGlobal ? "!" : "";
                  String msgPostPrefix = isGlobal ? massagePacket.getMessage().substring(1) : massagePacket.getMessage();
                  massagePacket.setMessage(msgPrePrefix + prefix + msgPostPrefix);
               }
            }
         }

         this.gotoSend = false;
      }
   }

   private void sendSpamAutoEzz(EntityLivingBase base, boolean isTotem) {
      if (base != null
         && base instanceof EntityOtherPlayerMP player
         && Minecraft.player != null
         && this.timer.hasReached((double)((int)this.SpamDelay.getFloat()))) {
         String name = base == FakePlayer.fakePlayer
            ? ""
            : ReplaceStrUtils.cutBackString(
               ReplaceStrUtils.deformatString(ReplaceStrUtils.deformatString(ReplaceStrUtils.remoteStringUTF(player.getName()), 1), 0)
            );
         String totemsPopped = base.totemsPopped == 0 ? "" : String.valueOf(base.totemsPopped);
         String[] texts = isTotem
            ? new String[]{
               "на %name% %pops%й тотемчик",
               player.totemsPopped > 3 ? "да скок тотемов %name% у тя нищеё6" : "даа %name% %pops%й тотемчик",
               "я те уже %pops% тотемов дал давай раздуплись слабость",
               player.totemsPopped > 1 ? "%name% ещё минус %pops%й тотем" : "%name% минус %pops%й тотем",
               player.totemsPopped > 3 ? "%name% хватит тотемы жрать дай мне" : "жуй тотем свой %name%",
               "%name% чё скок у тя там тотемов ещё я те уже %pops% влепил",
               "опять %name% тотемнулся дохни давай",
               player.totemsPopped > 7 ? "%name% у тебя тотемов как у мамы ебырей?" : "ещё %pops% тотем пи3данул",
               player.totemsPopped > 2 ? "тотем за тотемом нубяра %name%" : "опа тотемчик %name%",
               "%pops%й тотем %name% выкидывай шмотки на пол или убью",
               player.totemsPopped > 1 ? "%name% сдавайся давай, те не жалко тотемы?" : "щёлкнул %pops%й тотем %name%",
               "вбил %pops% тотем в зубы %name%",
               player.totemsPopped > 4 ? "%name% %pops%-кучу зубов уже выбил сдавайся" : "%name% поделись тотемами мне их жалко",
               "%name% каждый тотем это минус год твоей жизни",
               player.totemsPopped > 1 ? "сбрил %name% %pops% тотемов" : "сбрил %name% тотем",
               player.totemsPopped > 1 ? "%name% тотем за тотемом сучка" : "тотееемчиик, опа щёлкнул %name%",
               player.totemsPopped > 1 ? "%name% вкусные тотемы?" : "%name% вкусный тотем?",
               "на тотем просрись %name%",
               player.totemsPopped > 2 ? "%name% нравится тотемами догоняться?" : "%name% захавайся тупица",
               player.totemsPopped > 1 ? "прям натягиваю твои тотемы себе на хyй %name%" : "%name% натянул тотем твой на хyй се",
               player.totemsPopped > 2 ? "%name% заглот тотемный захавал уже %pops% тотемов" : "%name% в жопу пихни се тотем 2 раза сработает",
               "ой читак %name% выключи AutоTоtеm я те прикол покажу",
               "ну ток не ливай зайка %name% тотемчик номер %pops% пошёл",
               player.totemsPopped > 7 ? "%name% дропай шмотки или пи3дану" : "%name% %pops% тотем треснул скидывайся напол давай",
               player.totemsPopped > 3 ? "впяливаю тотем за тотемом %name% уже " + player.totemsPopped + "й пошёл" : "на %name% тотем впялил"
            }
            : new String[]{
               "отпялил %name% ezz",
               "плотно жахаю %name% у всех на глазах",
               "%name% выeбан",
               "отьeбал %name% и высушил",
               "пялю пялил и буду пялить %name%",
               "поджарил задницу %name%",
               "откукурузил %name% по госту",
               "%name% умер от вони ezz",
               "лошара %name%",
               "хyйнул по башне %name% из под выворота",
               "почухал пи3дёнку %name% на легке",
               "у тя клитор лопнул %name%",
               "обтрaхал тя ртом твоей матери %name%",
               "%name% пора бы купить вегалайн уже ньюкам((",
               "%name% чё грустишь покупай вегaлайн",
               "залил малофьёй за щёки %name%",
               "намана тебя колбасит %name%",
               "услышал хруст твоей извилины %name%",
               "зачем ты слился до и после члена моего? %name%",
               "%name% я использую твою жoпу как пенал",
               "ротовыeбаный %name% покончил с собой",
               "%name% послал сам себя нахyй как подписался на это пвп",
               "узенький рот %name% не выдержал моего огромного пениса",
               "у %name% пи3да пошла по швам",
               "проехался по ебaлу %name%",
               "отрыгнул %name%",
               "как дела %name% дырка тухлая",
               "иди на спавне погуляй %name%",
               "%name% продано",
               "%name% как оно там в жoпе не жмёт?",
               "почему же твой нурикукареку не вывез? %name%",
               "%name% тот самый вованчик с нурипуком",
               "нассал на труп %name%",
               "%name% тот самый вованчик с нурипуком",
               "%name% твоя жопа как бочка чтоб напихивать",
               "%name% расскажи как живётся с такой тупизной и слабостью",
               "у меня сегодня %name% на ужин",
               "%name% в погоне за моим феймом наткнулся на мой хуй",
               "%name% па-па-па-па-па-па-па твоя мaть тyпая пи3да",
               "у тя влаган хлюпает %name% от мои критов",
               "отклимайся %name% куда сток миссов лох",
               "%name% застрял в своих жировых складках",
               "инвалид %name% запутался в своих ногах",
               "следующий чекпоинт %name% это будет мой хyй",
               "nigg%name%er",
               "ну не смущай меня не лезь ко мне под стол зай %name%",
               "придавил яйцами твои яйца %name%",
               "%name% коль сосёте мне делайте это прилично, не надо чавкать, соблюдайте этикет",
               "у %name% в жoпе сдетонировал мой хyй",
               "я б тя стейком запи3дил ваще изи %name%",
               "вальнул-И ВЫEБАЛ %name%",
               "%name% туда тя типок без вегaлайна",
               "иди вегалайн купи %name%",
               "у %name% заклинило килку",
               "%name% не прыгай у тя мозг об стенки черепушки размазывается",
               "напружинил %name%",
               "ну ты ваще %name% 0/10 покупай вл будет 11/10",
               "%name% на тя насрано",
               "знаю тебе хочется постать меня нахyй САМ ИДИ НАХYЙ",
               "открыл врата в жoпу %name%",
               "катись от сюда воняешь %name%",
               "%name% отстал от жизни? покупай вегaлaйн!",
               "%name% да брат мои большие яйца прям мне помогают",
               "только не ябидничай %name%(",
               "%name% тишшш тишшш всё хорошо",
               "%name% а я всего 2 кнопки нажал",
               "оправдайся в яйца ртом своим %name%",
               "почему твою мать трахают талибы на рынке за кусок сала? %name%",
               "хотел в майн погамать а в итоге жопой прыгаешь на хуе моем %name%",
               "%name% использую твою мaть в виде чехла для хyя",
               "обжахал насквозь %name%",
               "хочешь мне в хyй дунуть %name%?",
               "%name% засадил те в хyй рот и вышел из жопы",
               "так жеска заправил в рот %name% что аш зубы выплюнул",
               "тепнул %name% в помойку",
               "%name% похвали меня за то как я хорошо тебя уделал",
               "%name% утонул в навозу",
               "%name% как те слоновий хyй на вкус?",
               "%name% ты свои говнодавы проебал",
               "накормил %name% из жопы",
               "%name% получил разсечение сетчатки ануса",
               "N1GG %name% ER",
               "%name% кланяйся великому %me%",
               "нагaндoнил зaлупoлиза %name%",
               "открыл новую скважину возле очка %name% а то очко прогнило",
               "%name% даа хах погуляй по спавну",
               ""
            };
         String text = texts[(int)(((float)texts.length - 1.0F) * (float)Math.random() * (((float)texts.length - 1.0F) / (float)texts.length))]
            .replace("%name%", name)
            .replace("%me%", mc.getSession().getUsername())
            .replace("%pops%", totemsPopped)
            .replace("  ", " ")
            .trim();
         if (text != null && !text.isEmpty()) {
            if (this.SpamBypassTexts.getBool()) {
               text = RandomStringUtils.randomAlphanumeric(5) + " " + text + " " + RandomStringUtils.randomAlphanumeric(5);
            }

            if (this.InGlobalChat.getBool()) {
               text = "!" + text;
            }

            mc.getConnection().sendPacket(new CPacketChatMessage(text));
            this.timer.reset();
            return;
         }

         return;
      }
   }

   private void onKill(EntityLivingBase base) {
      if (this.ChatSpammer.getBool() && this.SpamMode.getMode().equalsIgnoreCase("KillEzz")) {
         try {
            this.sendSpamAutoEzz(base, false);
         } catch (Exception var3) {
            var3.printStackTrace();
         }
      }
   }

   private void onTotem(EntityLivingBase base) {
      if (this.ChatSpammer.getBool() && this.SpamMode.getMode().equalsIgnoreCase("KillEzz") && this.SpamOnTotemPop.getBool()) {
         try {
            this.sendSpamAutoEzz(base, true);
         } catch (Exception var3) {
            var3.printStackTrace();
         }
      }
   }

   public void controlMemories(boolean using) {
      if (!using) {
         if (!this.DEATH_MEMORIES_LIST.isEmpty()) {
            this.DEATH_MEMORIES_LIST.clear();
         }
      } else {
         List<EntityLivingBase> toUpdateMemoryEntities = new ArrayList<>();
         if (HitAura.TARGET_ROTS != null) {
            toUpdateMemoryEntities.add(HitAura.TARGET_ROTS);
         }

         if (BowAimbot.target != null) {
            toUpdateMemoryEntities.add(BowAimbot.target);
         }

         if (mc.pointedEntity != null && mc.pointedEntity instanceof EntityLivingBase pointedBase && pointedBase != null) {
            toUpdateMemoryEntities.add(pointedBase);
         }

         if (CrystalField.get != null && !CrystalField.listIsEmptyOrNull(CrystalField.get.getTargets())) {
            for (EntityLivingBase crystalFieldTarget : CrystalField.get.getTargets()) {
               if (crystalFieldTarget != null) {
                  toUpdateMemoryEntities.add(crystalFieldTarget);
               }
            }
         }

         toUpdateMemoryEntities.stream().filter(base -> base.getHealth() != 0.0F).forEach(base -> {
            if (base != null) {
               this.controllingAddingMemoryToEntity(base);
            }
         });
         this.DEATH_MEMORIES_LIST.forEach(ChatHelper.EntityDeathMemory::updateMemoryTrigger);
         this.removeAutoMemories();
      }
   }

   public void controllingAddingMemoryToEntity(EntityLivingBase baseTo) {
      if (baseTo != null && !(baseTo instanceof EntityPlayerSP) && baseTo.ticksExisted >= 2 && baseTo.getHealth() != 0.0F) {
         ChatHelper.EntityDeathMemory searchedMemory = null;

         for (ChatHelper.EntityDeathMemory memory : this.DEATH_MEMORIES_LIST) {
            if (memory.isValidMemory() && memory.base.getEntityId() == baseTo.getEntityId()) {
               searchedMemory = memory;
            }
         }

         long maxTimeMemory = 150L;
         if (searchedMemory != null) {
            searchedMemory.resetMemory(baseTo, 150L);
         } else if (baseTo instanceof EntityOtherPlayerMP && this.DEATH_MEMORIES_LIST.isEmpty()) {
            this.DEATH_MEMORIES_LIST.add(new ChatHelper.EntityDeathMemory(baseTo, 150L, () -> this.onKill(baseTo), () -> this.onTotem(baseTo)));
         }
      }
   }

   private void removeAutoMemories() {
      this.DEATH_MEMORIES_LIST.removeIf(ChatHelper.EntityDeathMemory::isNotValidMemory);
   }

   private class EntityDeathMemory {
      private final TimerHelper startTime = new TimerHelper();
      private long maxTimeMemory;
      private EntityLivingBase base;
      private boolean hasReseted;
      private final Runnable onKillTrigger;
      private final Runnable onTotemTrigger;

      public EntityDeathMemory(EntityLivingBase base, long maxTimeMemory, Runnable onKillTrigger, Runnable onTotemTrigger) {
         this.base = base;
         this.startTime.reset();
         this.maxTimeMemory = maxTimeMemory;
         this.onKillTrigger = onKillTrigger;
         this.onTotemTrigger = onTotemTrigger;
      }

      public void resetMemory(EntityLivingBase base, long maxTimeMemory) {
         if (this.base == null || this.base.getHealth() != 0.0F) {
            this.base = base;
            this.maxTimeMemory = maxTimeMemory;
            this.startTime.reset();
         }
      }

      public boolean isValidMemory() {
         return this.base != null && this.maxTimeMemory != 0L && !this.startTime.hasReached((double)this.maxTimeMemory);
      }

      public boolean isNotValidMemory() {
         return !this.isValidMemory();
      }

      public void updateMemoryTrigger() {
         if (this.isValidMemory() && !this.hasReseted) {
            boolean hasSpec = false;
            if (Module.mc.getConnection() != null) {
               NetworkPlayerInfo info = Module.mc.getConnection().getPlayerInfo(this.base.getName());
               if (info != null && info.getGameType() == GameType.SPECTATOR) {
                  hasSpec = true;
               }
            }

            if (this.base.getHealth() == 0.0F
               || this.base.hurtTime > 0
                  && (
                     Module.mc.world.getEntityByID(this.base.getEntityId()) == null && this.base.getDistanceToEntity(Minecraft.player) > 10.0F
                        || this.base instanceof EntityPlayer player && player.isSpectator()
                  )
               || hasSpec) {
               this.onKillTrigger.run();
               this.hasReseted = true;
               this.maxTimeMemory = 300L;
            } else if (this.base.totemTick && this.onTotemTrigger != null) {
               this.onTotemTrigger.run();
            }
         }
      }
   }
}
