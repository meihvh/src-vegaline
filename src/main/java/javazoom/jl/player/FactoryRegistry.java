package javazoom.jl.player;

import java.util.Enumeration;
import java.util.Hashtable;
import javazoom.jl.decoder.JavaLayerException;

public class FactoryRegistry extends AudioDeviceFactory {
   private static FactoryRegistry instance = null;
   protected Hashtable factories = new Hashtable();

   public static synchronized FactoryRegistry systemRegistry() {
      if (instance == null) {
         instance = new FactoryRegistry();
         instance.registerDefaultFactories();
      }

      return instance;
   }

   public void addFactory(AudioDeviceFactory factory) {
      this.factories.put(factory.getClass(), factory);
   }

   public void removeFactoryType(Class cls) {
      this.factories.remove(cls);
   }

   public void removeFactory(AudioDeviceFactory factory) {
      this.factories.remove(factory.getClass());
   }

   @Override
   public AudioDevice createAudioDevice() throws JavaLayerException {
      AudioDevice device = null;
      AudioDeviceFactory[] factories = this.getFactoriesPriority();
      if (factories == null) {
         throw new JavaLayerException(this + ": no factories registered");
      } else {
         JavaLayerException lastEx = null;

         for (int i = 0; device == null && i < factories.length; i++) {
            try {
               device = factories[i].createAudioDevice();
            } catch (JavaLayerException var6) {
               lastEx = var6;
            }
         }

         if (device == null && lastEx != null) {
            throw new JavaLayerException("Cannot create AudioDevice", lastEx);
         } else {
            return device;
         }
      }
   }

   protected AudioDeviceFactory[] getFactoriesPriority() {
      AudioDeviceFactory[] fa = null;
      synchronized (this.factories) {
         int size = this.factories.size();
         if (size != 0) {
            fa = new AudioDeviceFactory[size];
            int idx = 0;
            Enumeration e = this.factories.elements();

            while (e.hasMoreElements()) {
               AudioDeviceFactory factory = (AudioDeviceFactory)e.nextElement();
               fa[idx++] = factory;
            }
         }

         return fa;
      }
   }

   protected void registerDefaultFactories() {
      this.addFactory(new JavaSoundAudioDeviceFactory());
   }
}
