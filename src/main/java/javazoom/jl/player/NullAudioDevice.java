package javazoom.jl.player;

public class NullAudioDevice extends AudioDeviceBase {
   @Override
   public int getPosition() {
      return 0;
   }
}
