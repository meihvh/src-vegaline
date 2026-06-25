package javazoom.jl.decoder;

public class Decoder implements DecoderErrors {
   private static final Decoder.Params DEFAULT_PARAMS = new Decoder.Params();
   private Obuffer output;
   private SynthesisFilter filter1;
   private SynthesisFilter filter2;
   private LayerIIIDecoder l3decoder;
   private LayerIIDecoder l2decoder;
   private LayerIDecoder l1decoder;
   private int outputFrequency;
   private int outputChannels;
   private final Equalizer equalizer = new Equalizer();
   private final Decoder.Params params;
   private boolean initialized;

   public float[] getTempOutSamples(float scale) {
      float[] floats = new float[32];
      int index = 0;
      if (this.filter1 != null) {
         for (float val : this.filter1.getTempOutSamples()) {
            floats[index++] += val;
         }
      }

      index = 0;
      if (this.filter2 != null) {
         for (float val : this.filter2.getTempOutSamples()) {
            floats[index++] += val;
         }
      }

      for (int i = 0; i < floats.length; i++) {
         floats[i] *= 0.5F * scale;
      }

      return floats;
   }

   public Decoder() {
      this(null);
   }

   public Decoder(Decoder.Params params0) {
      if (params0 == null) {
         params0 = DEFAULT_PARAMS;
      }

      this.params = params0;
      Equalizer eq = this.params.getInitialEqualizerSettings();
      if (eq != null) {
         this.equalizer.setFrom(eq);
      }
   }

   public void updateEqualizerData(Equalizer equalizer) {
      float[] factors = equalizer == null ? Equalizer.PASS_THRU_EQ.getBandFactors() : equalizer.getBandFactors();
      if (this.filter1 != null) {
         this.filter1.setEQ(factors);
      }

      if (this.filter2 != null) {
         this.filter2.setEQ(factors);
      }
   }

   public static Decoder.Params getDefaultParams() {
      return (Decoder.Params)DEFAULT_PARAMS.clone();
   }

   public void setEqualizer(Equalizer eq) {
      if (eq == null) {
         eq = Equalizer.PASS_THRU_EQ;
      }

      this.equalizer.setFrom(eq);
      float[] factors = this.equalizer.getBandFactors();
      if (this.filter1 != null) {
         this.filter1.setEQ(factors);
      }

      if (this.filter2 != null) {
         this.filter2.setEQ(factors);
      }
   }

   public Obuffer decodeFrame(Header header, Bitstream stream) throws DecoderException {
      if (!this.initialized) {
         this.initialize(header);
      }

      int layer = header.layer();
      this.output.clear_buffer();
      FrameDecoder decoder = this.retrieveDecoder(header, stream, layer);
      decoder.decodeFrame();
      this.output.write_buffer(1);
      return this.output;
   }

   public void setOutputBuffer(Obuffer out) {
      this.output = out;
   }

   public int getOutputFrequency() {
      return this.outputFrequency;
   }

   public int getOutputChannels() {
      return this.outputChannels;
   }

   public int getOutputBlockSize() {
      return 2304;
   }

   protected DecoderException newDecoderException(int errorcode) {
      return new DecoderException(errorcode, null);
   }

   protected DecoderException newDecoderException(int errorcode, Throwable throwable) {
      return new DecoderException(errorcode, throwable);
   }

   protected FrameDecoder retrieveDecoder(Header header, Bitstream stream, int layer) throws DecoderException {
      FrameDecoder decoder = null;
      switch (layer) {
         case 1:
            if (this.l1decoder == null) {
               this.l1decoder = new LayerIDecoder();
               this.l1decoder.create(stream, header, this.filter1, this.filter2, this.output, 0);
            }

            decoder = this.l1decoder;
            break;
         case 2:
            if (this.l2decoder == null) {
               this.l2decoder = new LayerIIDecoder();
               this.l2decoder.create(stream, header, this.filter1, this.filter2, this.output, 0);
            }

            decoder = this.l2decoder;
            break;
         case 3:
            if (this.l3decoder == null) {
               this.l3decoder = new LayerIIIDecoder(stream, header, this.filter1, this.filter2, this.output, 0);
            }

            decoder = this.l3decoder;
      }

      if (decoder == null) {
         throw this.newDecoderException(513, null);
      } else {
         return decoder;
      }
   }

   private void initialize(Header header) throws DecoderException {
      float scalefactor = 32700.0F;
      int mode = header.mode();
      int layer = header.layer();
      int channels = mode == 3 ? 1 : 2;
      if (this.output == null) {
         this.output = new SampleBuffer(header.frequency(), channels);
      }

      float[] factors = this.equalizer.getBandFactors();
      this.filter1 = new SynthesisFilter(0, scalefactor, factors);
      if (channels == 2) {
         this.filter2 = new SynthesisFilter(1, scalefactor, factors);
      }

      this.outputChannels = channels;
      this.outputFrequency = header.frequency();
      this.initialized = true;
   }

   public static class Params implements Cloneable {
      private OutputChannels outputChannels = OutputChannels.BOTH;
      private final Equalizer equalizer = new Equalizer();

      @Override
      public Object clone() {
         try {
            return super.clone();
         } catch (CloneNotSupportedException var2) {
            throw new InternalError(this + ": " + var2);
         }
      }

      public void setOutputChannels(OutputChannels out) {
         if (out == null) {
            throw new NullPointerException("out");
         } else {
            this.outputChannels = out;
         }
      }

      public OutputChannels getOutputChannels() {
         return this.outputChannels;
      }

      public Equalizer getInitialEqualizerSettings() {
         return this.equalizer;
      }
   }
}
