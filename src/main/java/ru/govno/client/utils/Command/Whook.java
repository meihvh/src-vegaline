package ru.govno.client.utils.Command;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import javax.net.ssl.HttpsURLConnection;

public class Whook {
   private final String url;
   private String content;
   private String username;
   private String avatarUrl;
   private boolean tts;
   private final List<Whook.EmbedObject> embeds = new ArrayList<>();

   public Whook(String url) {
      this.url = url;
   }

   public void setContent(String content) {
      this.content = content;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public void setAvatarUrl(String avatarUrl) {
      this.avatarUrl = avatarUrl;
   }

   public void setTts(boolean tts) {
      this.tts = tts;
   }

   public void addEmbed(Whook.EmbedObject embed) {
      this.embeds.add(embed);
   }

   public void execute() throws IOException {
      if (this.content == null && this.embeds.isEmpty()) {
         throw new IllegalArgumentException("Set content or add at least one EmbedObject");
      } else {
         Whook.JSONObject json = new Whook.JSONObject();
         json.put("content", this.content);
         json.put("username", this.username);
         json.put("avatar_url", this.avatarUrl);
         json.put("tts", this.tts);
         if (!this.embeds.isEmpty()) {
            List<Whook.JSONObject> embedObjects = new ArrayList<>();

            for (Whook.EmbedObject embed : this.embeds) {
               Whook.JSONObject jsonEmbed = new Whook.JSONObject();
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

               Whook.EmbedObject.Footer footer = embed.getFooter();
               Whook.EmbedObject.Image image = embed.getImage();
               Whook.EmbedObject.Thumbnail thumbnail = embed.getThumbnail();
               Whook.EmbedObject.Author author = embed.getAuthor();
               List<Whook.EmbedObject.Field> fields = embed.getFields();
               if (footer != null) {
                  Whook.JSONObject jsonFooter = new Whook.JSONObject();
                  jsonFooter.put("text", footer.getText());
                  jsonFooter.put("icon_url", footer.getIconUrl());
                  jsonEmbed.put("footer", jsonFooter);
               }

               if (image != null) {
                  Whook.JSONObject jsonImage = new Whook.JSONObject();
                  jsonImage.put("url", image.getUrl());
                  jsonEmbed.put("image", jsonImage);
               }

               if (thumbnail != null) {
                  Whook.JSONObject jsonThumbnail = new Whook.JSONObject();
                  jsonThumbnail.put("url", thumbnail.getUrl());
                  jsonEmbed.put("thumbnail", jsonThumbnail);
               }

               if (author != null) {
                  Whook.JSONObject jsonAuthor = new Whook.JSONObject();
                  jsonAuthor.put("name", author.getName());
                  jsonAuthor.put("url", author.getUrl());
                  jsonAuthor.put("icon_url", author.getIconUrl());
                  jsonEmbed.put("author", jsonAuthor);
               }

               List<Whook.JSONObject> jsonFields = new ArrayList<>();

               for (Whook.EmbedObject.Field field : fields) {
                  Whook.JSONObject jsonField = new Whook.JSONObject();
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

         URL url = new URL(this.url);
         HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
         connection.addRequestProperty("Content-Type", "application/json");
         connection.addRequestProperty("User-Agent", "Java-DiscordWebhook-BY-Gelox_");
         connection.setDoOutput(true);
         connection.setRequestMethod("POST");
         OutputStream stream = connection.getOutputStream();
         stream.write(json.toString().getBytes());
         stream.flush();
         stream.close();
         connection.getInputStream().close();
         connection.disconnect();
      }
   }

   public static class EmbedObject {
      private String title;
      private String description;
      private String url;
      private Color color;
      private Whook.EmbedObject.Footer footer;
      private Whook.EmbedObject.Thumbnail thumbnail;
      private Whook.EmbedObject.Image image;
      private Whook.EmbedObject.Author author;
      private final List<Whook.EmbedObject.Field> fields = new ArrayList<>();

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

      public Whook.EmbedObject.Footer getFooter() {
         return this.footer;
      }

      public Whook.EmbedObject.Thumbnail getThumbnail() {
         return this.thumbnail;
      }

      public Whook.EmbedObject.Image getImage() {
         return this.image;
      }

      public Whook.EmbedObject.Author getAuthor() {
         return this.author;
      }

      public List<Whook.EmbedObject.Field> getFields() {
         return this.fields;
      }

      public Whook.EmbedObject setTitle(String title) {
         this.title = title;
         return this;
      }

      public Whook.EmbedObject setDescription(String description) {
         this.description = description;
         return this;
      }

      public Whook.EmbedObject setUrl(String url) {
         this.url = url;
         return this;
      }

      public Whook.EmbedObject setColor(Color color) {
         this.color = color;
         return this;
      }

      public Whook.EmbedObject setFooter(String text, String icon) {
         this.footer = new Whook.EmbedObject.Footer(text, icon);
         return this;
      }

      public Whook.EmbedObject setThumbnail(String url) {
         this.thumbnail = new Whook.EmbedObject.Thumbnail(url);
         return this;
      }

      public Whook.EmbedObject setImage(String url) {
         this.image = new Whook.EmbedObject.Image(url);
         return this;
      }

      public Whook.EmbedObject setAuthor(String name, String url, String icon) {
         this.author = new Whook.EmbedObject.Author(name, url, icon);
         return this;
      }

      public Whook.EmbedObject addField(String name, String value, boolean inline) {
         this.fields.add(new Whook.EmbedObject.Field(name, value, inline));
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

   private class JSONObject {
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
            } else if (val instanceof Whook.JSONObject) {
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
}
