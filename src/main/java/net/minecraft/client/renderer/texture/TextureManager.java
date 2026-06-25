package net.minecraft.client.renderer.texture;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ICrashReportDetail;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import optifine.Config;
import optifine.CustomGuis;
import optifine.RandomMobs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shadersmod.client.ShadersTex;

public class TextureManager implements ITickable, IResourceManagerReloadListener {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final ResourceLocation field_194008_a = new ResourceLocation("");
   private final Map<ResourceLocation, ITextureObject> mapTextureObjects = Maps.newHashMap();
   private final List<ITickable> listTickables = Lists.newArrayList();
   private final Map<String, Integer> mapTextureCounters = Maps.newHashMap();
   private final IResourceManager theResourceManager;

   public Map<ResourceLocation, ITextureObject> getMapTextureObjects() {
      return this.mapTextureObjects;
   }

   public TextureManager(IResourceManager resourceManager) {
      this.theResourceManager = resourceManager;
   }

   public void bindTexture(ResourceLocation resource) {
      if (Config.isRandomMobs()) {
         resource = RandomMobs.getTextureLocation(resource);
      }

      if (Config.isCustomGuis()) {
         resource = CustomGuis.getTextureLocation(resource);
      }

      ITextureObject itextureobject = this.mapTextureObjects.get(resource);
      if (itextureobject == null) {
         itextureobject = new SimpleTexture(resource);
         this.loadTexture(resource, itextureobject);
      }

      if (Config.isShaders()) {
         ShadersTex.bindTexture(itextureobject);
      } else {
         TextureUtil.bindTexture(itextureobject.getGlTextureId());
      }
   }

   public boolean loadTickableTexture(ResourceLocation textureLocation, ITickableTextureObject textureObj) {
      if (this.loadTexture(textureLocation, textureObj)) {
         this.listTickables.add(textureObj);
         return true;
      } else {
         return false;
      }
   }

   public boolean loadTexture(ResourceLocation textureLocation, ITextureObject textureObj) {
      boolean flag = true;

      try {
         textureObj.loadTexture(this.theResourceManager);
      } catch (IOException var8) {
         if (textureLocation != field_194008_a) {
            LOGGER.warn("Failed to load texture: {}", textureLocation, var8);
         }

         textureObj = TextureUtil.MISSING_TEXTURE;
         this.mapTextureObjects.put(textureLocation, textureObj);
         flag = false;
      } catch (Throwable var9) {
         CrashReport crashreport = CrashReport.makeCrashReport(var9, "Registering texture");
         CrashReportCategory crashreportcategory = crashreport.makeCategory("Resource location being registered");
         crashreportcategory.addCrashSection("Resource location", textureLocation);
          ITextureObject finalTextureObj = textureObj;
          crashreportcategory.setDetail("Texture object class", new ICrashReportDetail<String>() {
            public String call() throws Exception {
               return finalTextureObj.getClass().getName();
            }
         });
         throw new ReportedException(crashreport);
      }

      this.mapTextureObjects.put(textureLocation, textureObj);
      return flag;
   }

   public ITextureObject getTexture(ResourceLocation textureLocation) {
      return this.mapTextureObjects.get(textureLocation);
   }

   public ResourceLocation getDynamicTextureLocation(String name, DynamicTexture texture) {
      if (name.equals("logo")) {
         texture = Config.getMojangLogoTexture(texture);
      }

      Integer integer = this.mapTextureCounters.get(name);
      if (integer == null) {
         integer = 1;
      } else {
         integer = integer + 1;
      }

      this.mapTextureCounters.put(name, integer);
      ResourceLocation resourcelocation = new ResourceLocation(String.format("dynamic/%s_%d", name, integer));
      this.loadTexture(resourcelocation, texture);
      return resourcelocation;
   }

   public ResourceLocation getDynamicTextureLocationCustom(String name, DynamicTexture texture) {
      Integer integer = this.mapTextureCounters.get(name);
      if (integer == null) {
         integer = 1;
      } else {
         integer = integer + 1;
      }

      this.mapTextureCounters.put(name, integer);
      ResourceLocation resourcelocation = new ResourceLocation(name);
      this.loadTexture(resourcelocation, texture);
      return resourcelocation;
   }

   @Override
   public void tick() {
      for (ITickable itickable : this.listTickables) {
         itickable.tick();
      }
   }

   public void deleteTexture(ResourceLocation textureLocation) {
      ITextureObject itextureobject = this.getTexture(textureLocation);
      if (itextureobject != null) {
         this.mapTextureObjects.remove(textureLocation);
         TextureUtil.deleteTexture(itextureobject.getGlTextureId());
      }
   }

   @Override
   public void onResourceManagerReload(IResourceManager resourceManager) {
      Config.dbg("*** Reloading textures ***");
      Config.log("Resource packs: " + Config.getResourcePackNames());
      Iterator iterator = this.mapTextureObjects.keySet().iterator();

      while (iterator.hasNext()) {
         ResourceLocation resourcelocation = (ResourceLocation)iterator.next();
         String s = resourcelocation.getResourcePath();
         if (s.startsWith("mcpatcher/") || s.startsWith("optifine/")) {
            ITextureObject itextureobject = this.mapTextureObjects.get(resourcelocation);
            if (itextureobject instanceof AbstractTexture abstracttexture) {
               abstracttexture.deleteGlTexture();
            }

            iterator.remove();
         }
      }

      Iterator<Entry<ResourceLocation, ITextureObject>> iterator1 = this.mapTextureObjects.entrySet().iterator();

      while (iterator1.hasNext()) {
         try {
            Entry<ResourceLocation, ITextureObject> entry = iterator1.next();
            ITextureObject itextureobject1 = entry.getValue();
            if (itextureobject1 == TextureUtil.MISSING_TEXTURE) {
               iterator1.remove();
            } else {
               this.loadTexture(entry.getKey(), itextureobject1);
            }
         } catch (Exception var7) {
            var7.printStackTrace();
         }
      }
   }

   public void reloadBannerTextures() {
      for (Entry<ResourceLocation, ITextureObject> entry : this.mapTextureObjects.entrySet()) {
         ResourceLocation resourcelocation = entry.getKey();
         ITextureObject itextureobject = entry.getValue();
         if (itextureobject instanceof LayeredColorMaskTexture) {
            this.loadTexture(resourcelocation, itextureobject);
         }
      }
   }
}
