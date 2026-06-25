package ru.govno.client.utils;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class RadioStationsBase {
   private static final String stationsStringsURL = "https://gist.githubusercontent.com/DICKAFOTON/07b867087a89c80bbadc9128ea10f078/raw/vl%2520radio%2520stations";
   private static final TimerHelper updateStationsTimer = new TimerHelper();
   private static final List<RadioStationsBase.StationStream> pastebinRadioStreamsCONST = new ArrayList<>();
   private static boolean useNet;
   private static final RadioStationsBase.StationStream EMPTY = new RadioStationsBase.StationStream("", "", "");

   private static long updateStationsDelay() {
      return 30000L;
   }

   public static void setUsingNET(boolean set) {
      useNet = set;
   }

   private static List<RadioStationsBase.StationStream> pastebinRadioStreams() {
      if (updateStationsTimer.hasReached((float)updateStationsDelay())) {
         updateStationsTimer.reset();
         CompletableFuture.runAsync(
            () -> {
               try {
                  List<String> stringsOfSite = Lists.newArrayList();

                  try {
                     Iterator<String> yy = new Scanner(
                        new URL("https://gist.githubusercontent.com/DICKAFOTON/07b867087a89c80bbadc9128ea10f078/raw/vl%2520radio%2520stations").openStream()
                     );

                     while (yy.hasNext()) {
                        stringsOfSite.add(yy.next());
                     }
                  } catch (IOException var10) {
                     pastebinRadioStreamsCONST.clear();
                  }

                  if (!stringsOfSite.isEmpty()) {
                     String REPL = "\\u0022";
                     String SPLIT = "\\u007c";
                     List<RadioStationsBase.StationStream> list = new ArrayList<>();

                     for (String lineRadioStreamData : stringsOfSite) {
                        if (!lineRadioStreamData.isEmpty() && lineRadioStreamData.length() >= 2) {
                           lineRadioStreamData = lineRadioStreamData.replaceAll(REPL, "");
                           String[] data = lineRadioStreamData.split(SPLIT);
                           if (data != null && data.length == 3) {
                              String name = data[0] == null ? null : data[0].replace("_", " ");
                              String link = data[1];
                              String info = data[2] == null ? null : data[2].replace("_", " ");
                              if (name != null && name.length() > 2 && link != null && link.length() > 8) {
                                 list.add(new RadioStationsBase.StationStream(name, link, info));
                              }
                           }
                        }
                     }

                     if (!list.isEmpty()) {
                        pastebinRadioStreamsCONST.clear();
                        pastebinRadioStreamsCONST.addAll(list);
                     }
                  }
               } catch (Exception var11) {
                  var11.printStackTrace();
               }
            }
         );
      }

      return pastebinRadioStreamsCONST;
   }

   public static List<RadioStationsBase.StationStream> getRadioStations() {
      List<RadioStationsBase.StationStream> list = new ArrayList<>();
      List<RadioStationsBase.StationStream> listPastebin;
      if (useNet && !(listPastebin = pastebinRadioStreams()).isEmpty()) {
         list.addAll(listPastebin);
      } else {
         list.addAll(
            Arrays.asList(
               new RadioStationsBase.StationStream("Europe+", "http://ep128.hostingradio.ru:8030/ep128", "Европа Плюс"),
               new RadioStationsBase.StationStream("Europe+ Top", "http://eptop128server.streamr.ru:8033/eptop128", "Европа Плюс с подборками"),
               new RadioStationsBase.StationStream("Anime", "http://pool.anison.fm:9000/AniSonFM(320)?nocache=0.9834540412142996", "В стиле 'Аниме'"),
               new RadioStationsBase.StationStream("DFM", "https://dfm.hostingradio.ru/dfm128.mp3", "Типичное RU"),
               new RadioStationsBase.StationStream("Radio Maximum RU", "https://maximum.hostingradio.ru/maximum128.mp3", "Утреннее настроение"),
               new RadioStationsBase.StationStream("Radio Hermitage RU", "https://hermitage.hostingradio.ru/hermitage128.mp3", "Эрмитаж RU"),
               new RadioStationsBase.StationStream("Euro", "http://stream1.euroradio.fm:8000/euroradio1?seed=1523961466", "Европейский стиль"),
               new RadioStationsBase.StationStream("Discover Trance", "http://paris.discovertrance.com:8006/;stream.nsv", "Энергичный и модерновый стиль Транс"),
               new RadioStationsBase.StationStream("Eurodance 90's", "http://listen1.myradio24.com:9000/5967", "Европейский Танцевальный стиль"),
               new RadioStationsBase.StationStream("Jazz FM", "http://nashe1.hostingradio.ru/jazz-128.mp3", "Иностранный глубокий Джаз"),
               new RadioStationsBase.StationStream("French Jazz", "http://icepe6.infomaniak.ch:80/jazz-wr01-128.mp3", "Только французский Джаз"),
               new RadioStationsBase.StationStream("Premium", "http://listen.rpfm.ru:9000/premium128", "Универсальный стиль нравящийся всем"),
               new RadioStationsBase.StationStream("Chanson RU", "http://chanson.hostingradio.ru:8041/chanson-uncensored128.mp3", "Стиль 'Шансон' RU"),
               new RadioStationsBase.StationStream("Love FM", "https://ice07.fluidstream.net/lovefm.mp3", "Иностранные спокойные подборки"),
               new RadioStationsBase.StationStream("Naxi Love", "http://naxidigital-love128.streaming.rs:8100/", "Иностранные популярные спокойные подборки"),
               new RadioStationsBase.StationStream("Funky Corner", "https://ais-sa2.cdnstream1.com/2447_192.mp3", "Подходит для времени 'поработать'"),
               new RadioStationsBase.StationStream("StarFM Rock", "https://stream.starfm.de/berlin/mp3-192/", "Иностранный Рок"),
               new RadioStationsBase.StationStream("StarFM HardRock", "https://stream.starfm.de/hardrock/mp3-192/", "Иностранный крепкий Рок"),
               new RadioStationsBase.StationStream("StarFM National", "https://stream.starfm.de/national/mp3-192/", "Иностранный национальный Рок"),
               new RadioStationsBase.StationStream("StarFM Alt Rock", "https://stream.starfm.de/alternat/mp3-192/", "Иностранный альтернативный Рок"),
               new RadioStationsBase.StationStream("StarFM From Hell", "https://stream.starfm.de/fromhell/mp3-192/", "Иностранный жестойкий Рок-Металл"),
               new RadioStationsBase.StationStream("StarFM New Metal", "https://stream.starfm.de/newmetal/mp3-192/", "Иностранный Новый металл"),
               new RadioStationsBase.StationStream("ChillOut FM", "https://chill-out-fm.stream.laut.fm/chill-out-fm", "В стиле 'ChillOut' для позитива"),
               new RadioStationsBase.StationStream(
                  "LoFi Focus", "https://stream.bigfm.de/exlofifocus/mp3-192/", "Иностранные подборки Li-Fi для работы и отдыха"
               ),
               new RadioStationsBase.StationStream("True Lo-Fi", "https://study-high.rautemusik.fm/", "Отборные иностранные Lo-Fi треки для отдыха и сна"),
               new RadioStationsBase.StationStream("Electro FM", "http://ldb.rmnradio.net:8022/", "Электронная и немного Рока"),
               new RadioStationsBase.StationStream(
                  "Sonic Universe", "https://sonicstream.out.airtime.pro/sonicstream_b", "В ежином стиле 'Приятно когда тихо играет'"
               ),
               new RadioStationsBase.StationStream(
                  "Nts Rap House", "https://stream-mixtape-geo.ntslive.net/mixtape22", "Подборки иностранного Репа с необычными битами"
               ),
               new RadioStationsBase.StationStream("Low Key", "https://stream-mixtape-geo.ntslive.net/mixtape2", "Иностранный медленный Поп и Альтернатива"),
               new RadioStationsBase.StationStream("Drum&Bass", "https://sc2.dubplate.fm/radio/8030/dnb/uhifi", "Иностранный Драмнбейс"),
               new RadioStationsBase.StationStream("Hard Rock Heaven", "https://ais-sa2.cdnstream1.com/1521_128", "Позитивный крепкий Рок"),
               new RadioStationsBase.StationStream("Vibration Pop-Rock", "http://vibrationpoprock.stream2net.eu:8260/", "Вайбовые подборки Поп-Рока"),
               new RadioStationsBase.StationStream("SexRadio", "https://sex-high.rautemusik.fm/", "Подборка для возбуждённого настроения"),
               new RadioStationsBase.StationStream("LT-Dacha", "https://stream1.relaxfm.lt/rrb128.mp3", "Литовское на дачу"),
               new RadioStationsBase.StationStream("EU-Hit FM", "https://stream.ehr.lt:8443/ehr", "Европейские подборки хитов"),
               new RadioStationsBase.StationStream("A.D.M Hardstyle", "https://kathy.torontocast.com:2930/stream", "Ошеломительно плотный HardStyle"),
               new RadioStationsBase.StationStream("Polska Anime", "http://91.232.4.33:7028", "Милое в стиле Anime"),
               new RadioStationsBase.StationStream("Sunshine HardStyle", "https://stream.sunshine-live.de/hardstyle/mp3-192/", "Мощные HardStyle хиты"),
               new RadioStationsBase.StationStream("Marusya FM Remixes", "https://air.unmixed.ru/marusyafm", "Необычные фанатские ремиксы в стиле HardStyle"),
               new RadioStationsBase.StationStream("HardStyle Rem", "https://streams.ilovemusic.de/iloveradio21.mp3", "Ремиксы в стиле HardStyle"),
               new RadioStationsBase.StationStream("Chill Acoustic", "https://128k.co.uk/stream/1126/", "Акустический чилл-хоп в hd качестве"),
               new RadioStationsBase.StationStream("Chillhop", "https://streams.ilovemusic.de/iloveradio17.mp3", "Обычный спокойный chill-hop hd качества"),
               new RadioStationsBase.StationStream("Lounge Deluxe HD", "https://loungeradiodeluxe-rex.radioca.st/stream", "Амбиентбит в hd качестве"),
               new RadioStationsBase.StationStream("UltraChill HD", "https://www.radioking.com/play/wvro/436105", "Медленный чилл - поп и реп в hd качестве"),
               new RadioStationsBase.StationStream("Disconight HD", "http://88.99.195.180:8015/stream", "Ночное спокойное диско в hd качестве"),
               new RadioStationsBase.StationStream("Erreti HD", "https://erretismart.phon.in/live", "Медленный поп hd качества"),
               new RadioStationsBase.StationStream(
                  "Ambient HD", "https://public.isekoi-radio.com/listen/ambient/ambientradio.mp3", "Амбиент для максимального спокойствия hd качества"
               ),
               new RadioStationsBase.StationStream(
                  "Ambient Too HD", "https://www.partyviberadio.com:8069/;listen.pls", "Амбиент для максимального спокойствия hd качества"
               )
            )
         );
      }

      list.removeIf(o -> o.getName().equalsIgnoreCase(""));
      String REPL = "";
      String SPLIT = "|";

      for (RadioStationsBase.StationStream stationStream : list) {
         String name = stationStream.getName().replace(" ", "_");
         String link = stationStream.getUrl();
         String info = stationStream.getName() == null ? null : stationStream.getInfo().replace(" ", "_");
         String var9 = REPL + name + REPL + SPLIT + REPL + link + REPL + SPLIT + REPL + info + REPL;
      }

      return list;
   }

   public static List<String> getNamesFromStationsList() {
      return getRadioStations().stream().map(station -> station.name).toList();
   }

   public static List<String> getUrlsFromStationsList() {
      return getRadioStations().stream().map(station -> station.url).toList();
   }

   public static List<String> getInfosFromStationsList() {
      return getRadioStations().stream().map(station -> station.info).toList();
   }

   public static String getUrlFromStationName(String name) {
      return name == null
         ? ""
         : getRadioStations()
            .stream()
            .filter(station -> station.name.equalsIgnoreCase(name))
            .findFirst()
            .orElse(new RadioStationsBase.StationStream("", "", ""))
            .getUrl();
   }

   public static String getInfoFromStationName(String name) {
      return name == null
         ? ""
         : getRadioStations()
            .stream()
            .filter(station -> station.name.equalsIgnoreCase(name))
            .findFirst()
            .orElse(new RadioStationsBase.StationStream("", "", ""))
            .getInfo();
   }

   public static String[] getRadioModesForSetting() {
      return getNamesFromStationsList().toArray(new String[0]);
   }

   public static class StationStream {
      private final String name;
      private final String url;
      private final String info;

      public StationStream(String name, String url, String info) {
         this.name = name;
         this.url = url;
         this.info = info != null && !info.isEmpty() ? info : "Описания нет.";
      }

      public String getName() {
         return this.name;
      }

      public String getUrl() {
         return this.url;
      }

      public String getInfo() {
         return this.info;
      }
   }
}
