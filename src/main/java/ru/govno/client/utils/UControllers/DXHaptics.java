package ru.govno.client.utils.UControllers;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;

public class DXHaptics {
   public static void playHapticsTry(AudioInputStream audioInputStream, float volume, int samplesBufferSize) {
      if (DualSenseClientLib.instance() != null && audioInputStream != null) {
         try {
            double volumeMultiplier = calculateVolumeMultiplier((double)Math.max(Math.min(volume, 1.0F), 0.0F));
            byte[] buffer = new byte[samplesBufferSize];

            int bytesRead;
            while ((bytesRead = audioInputStream.read(buffer, 0, samplesBufferSize)) != -1) {
               byte[] dataToSend;
               if (bytesRead < samplesBufferSize) {
                  dataToSend = new byte[bytesRead];
                  System.arraycopy(buffer, 0, dataToSend, 0, bytesRead);
               } else {
                  dataToSend = new byte[samplesBufferSize];
                  System.arraycopy(buffer, 0, dataToSend, 0, samplesBufferSize);
               }

               applyVolume(dataToSend, volumeMultiplier);
               int result = DualSenseClientLib.instance().DS_SendHapticsAudio(1, dataToSend, dataToSend.length);
               System.out.println(result);
            }

            audioInputStream.close();
         } catch (IOException var9) {
            var9.printStackTrace();
         }
      }
   }

   public static void sendAudioToHaptics(int deviceIndex, AudioInputStream audioInputStream, int chunkSize, double volume) throws IOException, UnsupportedAudioFileException {
      if (DualSenseClientLib.instance() == null || audioInputStream == null) {
         throw new IllegalArgumentException("lib and audioInputStream cannot be null");
      } else if (!(volume < 0.0) && !(volume > 1.0)) {
         AudioFormat format = audioInputStream.getFormat();
         AudioFormat targetFormat = new AudioFormat(Encoding.PCM_UNSIGNED, format.getSampleRate(), 8, 1, 1, format.getSampleRate(), false);
         AudioInputStream convertedStream = audioInputStream;
         if (!format.matches(targetFormat)) {
            convertedStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
         }

         double volumeMultiplier = calculateVolumeMultiplier(volume);
         byte[] buffer = new byte[chunkSize];

         int bytesRead;
         while ((bytesRead = convertedStream.read(buffer, 0, chunkSize)) != -1) {
            byte[] dataToSend;
            if (bytesRead < chunkSize) {
               dataToSend = new byte[bytesRead];
               System.arraycopy(buffer, 0, dataToSend, 0, bytesRead);
            } else {
               dataToSend = new byte[chunkSize];
               System.arraycopy(buffer, 0, dataToSend, 0, chunkSize);
            }

            applyVolume(dataToSend, volumeMultiplier);
            int result = DualSenseClientLib.instance().DS_SendHapticsAudio(deviceIndex, dataToSend, dataToSend.length);
            if (result != 0) {
               System.err.println("Error sending haptics audio: " + result);
            }

            try {
               Thread.sleep(1L);
            } catch (InterruptedException var15) {
               Thread.currentThread().interrupt();
               break;
            }
         }

         convertedStream.close();
         if (convertedStream != audioInputStream) {
            audioInputStream.close();
         }
      } else {
         throw new IllegalArgumentException("volume must be between 0.0 and 1.0");
      }
   }

   private static double calculateVolumeMultiplier(double volume) {
      return volume == 0.0 ? 0.0 : Math.pow(10.0, volume * Math.log10(2.0));
   }

   private static void applyVolume(byte[] audioData, double multiplier) {
      for (int i = 0; i < audioData.length; i++) {
         int unsignedValue = audioData[i] & 255;
         int centeredValue = unsignedValue - 128;
         double scaledValue = (double)centeredValue * multiplier;
         int newValue = (int)Math.round(scaledValue + 128.0);
         newValue = Math.max(0, Math.min(255, newValue));
         audioData[i] = (byte)newValue;
      }
   }

   public static void sendAudioToHaptics(int deviceIndex, AudioInputStream audioInputStream) throws IOException, UnsupportedAudioFileException {
      sendAudioToHaptics(deviceIndex, audioInputStream, 32, 1.0);
   }

   public static void sendAudioToHaptics(int deviceIndex, AudioInputStream audioInputStream, double volume) throws IOException, UnsupportedAudioFileException {
      sendAudioToHaptics(deviceIndex, audioInputStream, 32, volume);
   }

   public static void sendAudioFileToHaptics(int deviceIndex, String audioFilePath, int chunkSize, double volume) throws IOException, UnsupportedAudioFileException {
      File audioFile = new File(audioFilePath);
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
      sendAudioToHaptics(deviceIndex, audioInputStream, chunkSize, volume);
   }

   public static void sendAudioFileToHaptics(int deviceIndex, String audioFilePath, int chunkSize) throws IOException, UnsupportedAudioFileException {
      sendAudioFileToHaptics(deviceIndex, audioFilePath, chunkSize, 1.0);
   }

   public static void sendAudioFileToHaptics(int deviceIndex, String audioFilePath, double volume) throws IOException, UnsupportedAudioFileException {
      sendAudioFileToHaptics(deviceIndex, audioFilePath, 32, volume);
   }

   public static void sendAudioFileToHaptics(int deviceIndex, String audioFilePath) throws IOException, UnsupportedAudioFileException {
      sendAudioFileToHaptics(deviceIndex, audioFilePath, 32, 1.0);
   }

   public static void sendAudioBytesToHaptics(int deviceIndex, byte[] audioData, int chunkSize, double volume) {
      if (DualSenseClientLib.instance() == null || audioData == null || audioData.length == 0) {
         throw new IllegalArgumentException("lib and audioData cannot be null, audioData must not be empty");
      } else if (!(volume < 0.0) && !(volume > 1.0)) {
         double volumeMultiplier = calculateVolumeMultiplier(volume);
         int offset = 0;

         while (offset < audioData.length) {
            int currentChunkSize = Math.min(chunkSize, audioData.length - offset);
            byte[] chunk = new byte[currentChunkSize];
            System.arraycopy(audioData, offset, chunk, 0, currentChunkSize);
            applyVolume(chunk, volumeMultiplier);
            int result = DualSenseClientLib.instance().DS_SendHapticsAudio(0, chunk, currentChunkSize);
            if (result != 0) {
               System.err.println("Error sending haptics audio chunk: " + result);
            }

            offset += currentChunkSize;

            try {
               Thread.sleep(1L);
            } catch (InterruptedException var12) {
               Thread.currentThread().interrupt();
               break;
            }
         }
      } else {
         throw new IllegalArgumentException("volume must be between 0.0 and 1.0");
      }
   }

   public static void sendAudioBytesToHaptics(int deviceIndex, byte[] audioData, int chunkSize) {
      sendAudioBytesToHaptics(deviceIndex, audioData, chunkSize, 1.0);
   }

   public static void sendAudioBytesToHaptics(int deviceIndex, byte[] audioData, double volume) {
      sendAudioBytesToHaptics(deviceIndex, audioData, 32, volume);
   }

   public static void sendAudioBytesToHaptics(int deviceIndex, byte[] audioData) {
      sendAudioBytesToHaptics(deviceIndex, audioData, 32, 1.0);
   }
}
