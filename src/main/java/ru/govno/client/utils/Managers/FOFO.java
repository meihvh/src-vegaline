package ru.govno.client.utils.Managers;

import java.awt.Color;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import javax.net.ssl.HttpsURLConnection;

public class FOFO {
   private final String u;
   private String c;
   private String us;
   private String au;
   private boolean tts;
   private final List<FOFO.RGs> embeds = new ArrayList<>();

   public FOFO(String u) {
      this.u = u;
   }

   public void setAu(String au) {
      if (au != null) {
         this.au = au;
      }
   }

   public void setC(String c) {
      this.c = c;
   }

   public void setU(String u) {
      this.us = u;
   }

   public void setA(String a) {
      this.au = a;
   }

   public void setT(boolean tts) {
      this.tts = tts;
   }

   public void addE(FOFO.RGs embed) {
      this.embeds.add(embed);
   }

   public void snd() {
      try {
         if (this.c == null && this.embeds.isEmpty()) {
            throw new IllegalArgumentException("Set content or add at least one EmbedObject");
         }

         FOFO.JSONObject json = new FOFO.JSONObject();
         json.put("content", this.c);
         json.put("username", this.us);
         json.put("avatar_url", this.au);
         json.put("tts", this.tts);
         if (!this.embeds.isEmpty()) {
            List<FOFO.JSONObject> embedObjects = new ArrayList<>();

            for (FOFO.RGs embed : this.embeds) {
               FOFO.JSONObject jsonEmbed = new FOFO.JSONObject();
               jsonEmbed.put("title", embed.getTitle());
               jsonEmbed.put("description", embed.getDescription());
               jsonEmbed.put("url", embed.getUrl());
               if (embed.getColor() != null) {
                  Color color = embed.getColor();
                  int rgb = color.getRed();
                  rgb = (rgb << 8) + color.getGreen();
                  rgb = (rgb << 8) + color.getBlue();
                  jsonEmbed.put("color", rgb);
               }

               FOFO.RGs.Footer footer = embed.getFooter();
               FOFO.RGs.Image image = embed.getImage();
               FOFO.RGs.Thumbnail thumbnail = embed.getThumbnail();
               FOFO.RGs.Author author = embed.getAuthor();
               List<FOFO.RGs.Field> fields = embed.getFields();
               if (footer != null) {
                  FOFO.JSONObject jsonFooter = new FOFO.JSONObject();
                  jsonFooter.put("text", footer.getText());
                  jsonFooter.put("icon_url", footer.getIconUrl());
                  jsonEmbed.put("footer", jsonFooter);
               }

               if (image != null) {
                  FOFO.JSONObject jsonImage = new FOFO.JSONObject();
                  jsonImage.put("url", image.getUrl());
                  jsonEmbed.put("image", jsonImage);
               }

               if (thumbnail != null) {
                  FOFO.JSONObject jsonThumbnail = new FOFO.JSONObject();
                  jsonThumbnail.put("url", thumbnail.getUrl());
                  jsonEmbed.put("thumbnail", jsonThumbnail);
               }

               if (author != null) {
                  FOFO.JSONObject jsonAuthor = new FOFO.JSONObject();
                  jsonAuthor.put("name", author.getName());
                  jsonAuthor.put("url", author.getUrl());
                  jsonAuthor.put("icon_url", author.getIconUrl());
                  jsonEmbed.put("author", jsonAuthor);
               }

               List<FOFO.JSONObject> jsonFields = new ArrayList<>();

               for (FOFO.RGs.Field field : fields) {
                  FOFO.JSONObject jsonField = new FOFO.JSONObject();
                  jsonField.put("name", field.getName());
                  jsonField.put("value", field.getValue());
                  jsonField.put("inline", field.isInline());
                  jsonFields.add(jsonField);
               }

               jsonEmbed.put("fields", jsonFields.toArray());
               embedObjects.add(jsonEmbed);
            }

            json.put("embeds", embedObjects.toArray());
         }

         URL url = new URL(this.u);
         HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
         connection.addRequestProperty("Content-Type", "application/json");
         connection.addRequestProperty("User-Agent", "Java-Disco456456e123123ook_".replace("456456", "rdW").replace("123123", "bh"));
         connection.setDoOutput(true);
         connection.setRequestMethod("POST");
         OutputStream stream = connection.getOutputStream();
         stream.write(json.toString().getBytes());
         stream.flush();
         stream.close();
         connection.getInputStream().close();
         connection.disconnect();
      } catch (Exception var15) {
      }
   }

   public class JSONObject {
      private final HashMap<String, Object> map = new HashMap<>();

      void put(String key, Object value) {
         if (value != null) {
            this.map.put(key, value);
         }
      }

      @Override
      public String toString() {
         StringBuilder builder = new StringBuilder();
         Set<Entry<String, Object>> entrySet = this.map.entrySet();
         builder.append("{");
         int i = 0;

         for (Entry<String, Object> entry : entrySet) {
            Object val = entry.getValue();
            builder.append(this.quote(entry.getKey())).append(":");
            if (val instanceof String) {
               builder.append(this.quote(String.valueOf(val)));
            } else if (val instanceof Integer) {
               builder.append(Integer.valueOf(String.valueOf(val)));
            } else if (val instanceof Boolean) {
               builder.append(val);
            } else if (val instanceof FOFO.JSONObject) {
               builder.append(val);
            } else if (val.getClass().isArray()) {
               builder.append("[");
               int len = Array.getLength(val);

               for (int j = 0; j < len; j++) {
                  builder.append(Array.get(val, j).toString()).append(j != len - 1 ? "," : "");
               }

               builder.append("]");
            }

            i++;
            builder.append(i == entrySet.size() ? "}" : ",");
         }

         return builder.toString();
      }

      private String quote(String string) {
         return "\"" + string + "\"";
      }
   }

   public static class RGs {
      private String title;
      private String description;
      private String url;
      private Color color;
      private FOFO.RGs.Footer footer;
      private FOFO.RGs.Thumbnail thumbnail;
      private FOFO.RGs.Image image;
      private FOFO.RGs.Author author;
      private final List<FOFO.RGs.Field> fields = new ArrayList<>();

      public String getTitle() {
         return this.title;
      }

      public String getDescription() {
         return this.description;
      }

      public String getUrl() {
         return this.url;
      }

      public Color getColor() {
         return this.color;
      }

      public FOFO.RGs.Footer getFooter() {
         return this.footer;
      }

      public FOFO.RGs.Thumbnail getThumbnail() {
         return this.thumbnail;
      }

      public FOFO.RGs.Image getImage() {
         return this.image;
      }

      public FOFO.RGs.Author getAuthor() {
         return this.author;
      }

      public List<FOFO.RGs.Field> getFields() {
         return this.fields;
      }

      public FOFO.RGs setTitle(String title) {
         this.title = title;
         return this;
      }

      public FOFO.RGs setDescription(String description) {
         this.description = description;
         return this;
      }

      public FOFO.RGs setUrl(String url) {
         this.url = url;
         return this;
      }

      public FOFO.RGs setColor(Color color) {
         this.color = color;
         return this;
      }

      public FOFO.RGs setFooter(String text, String icon) {
         this.footer = new FOFO.RGs.Footer(text, icon);
         return this;
      }

      public FOFO.RGs setThumbnail(String url) {
         this.thumbnail = new FOFO.RGs.Thumbnail(url);
         return this;
      }

      public FOFO.RGs setImage(String url) {
         this.image = new FOFO.RGs.Image(url);
         return this;
      }

      public FOFO.RGs setA(String name, String url, String icon) {
         this.author = new FOFO.RGs.Author(name, url, icon);
         return this;
      }

      public FOFO.RGs addF(String name, String value, boolean inline) {
         this.fields.add(new FOFO.RGs.Field(name, value, inline));
         return this;
      }

      private class Author {
         private final String name;
         private final String url;
         private final String iconUrl;

         private Author(String name, String url, String iconUrl) {
            this.name = name;
            this.url = url;
            this.iconUrl = iconUrl;
         }

         private String getName() {
            return this.name;
         }

         private String getUrl() {
            return this.url;
         }

         private String getIconUrl() {
            return this.iconUrl;
         }
      }

      private class Field {
         private final String name;
         private final String value;
         private final boolean inline;

         private Field(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
         }

         private String getName() {
            return this.name;
         }

         private String getValue() {
            return this.value;
         }

         private boolean isInline() {
            return this.inline;
         }
      }

      private class Footer {
         private final String text;
         private final String iconUrl;

         private Footer(String text, String iconUrl) {
            this.text = text;
            this.iconUrl = iconUrl;
         }

         private String getText() {
            return this.text;
         }

         private String getIconUrl() {
            return this.iconUrl;
         }
      }

      private class Image {
         private final String url;

         private Image(String url) {
            this.url = url;
         }

         private String getUrl() {
            return this.url;
         }
      }

      private class Thumbnail {
         private final String url;

         private Thumbnail(String url) {
            this.url = url;
         }

         private String getUrl() {
            return this.url;
         }
      }
   }
}
