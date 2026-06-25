package ru.govno.client.utils.Render;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL20;

public class ShaderUtil2 {
   private static final Minecraft mc = Minecraft.getMinecraft();
   private int programID;

   public void setUniformf(String name, float... args) {
      try {
         int loc = GL20.glGetUniformLocation(this.programID, name);
         switch (args.length) {
            case 1:
               GL20.glUniform1f(loc, args[0]);
               break;
            case 2:
               GL20.glUniform2f(loc, args[0], args[1]);
               break;
            case 3:
               GL20.glUniform3f(loc, args[0], args[1], args[2]);
               break;
            case 4:
               GL20.glUniform4f(loc, args[0], args[1], args[2], args[3]);
         }
      } catch (Exception var4) {
         var4.fillInStackTrace();
      }
   }

   public void setUniformColor(String name, int color) {
      this.setUniformf(name, (float)RenderUtils.red(color) / 255.0F, (float)RenderUtils.green(color) / 255.0F, (float)RenderUtils.blue(color) / 255.0F);
   }

   public void init() {
      if (this.programID != 0) {
         GL20.glUseProgram(this.programID);
      }
   }

   public void unload() {
      GL20.glUseProgram(0);
   }

   public void setUniformi(String name, int... args) {
      int loc = GL20.glGetUniformLocation(this.programID, name);
      if (args.length > 1) {
         GL20.glUniform2i(loc, args[0], args[1]);
      } else {
         GL20.glUniform1i(loc, args[0]);
      }
   }

   public ShaderUtil2(String fragmentShaderLoc, String vertexShaderLoc) {
      try {
         int program = GL20.glCreateProgram();

         try {
            int fragmentShaderID = this.createShader(mc.getResourceManager().getResource(new ResourceLocation(fragmentShaderLoc)).getInputStream(), 35632);
            GL20.glAttachShader(program, fragmentShaderID);
            int vertexShaderID = this.createShader(mc.getResourceManager().getResource(new ResourceLocation(vertexShaderLoc)).getInputStream(), 35633);
            GL20.glAttachShader(program, vertexShaderID);
         } catch (IOException var6) {
            var6.printStackTrace();
         }

         GL20.glLinkProgram(program);
         if (GL20.glGetProgrami(program, 35714) == 0) {
            throw new IllegalStateException("Shader failed to link!");
         }

         this.programID = program;
      } catch (Exception var7) {
         var7.fillInStackTrace();
      }
   }

   public int getUniform(String name) {
      return GL20.glGetUniformLocation(this.programID, name);
   }

   public ShaderUtil2(String fragmentShaderLoc) {
      this(fragmentShaderLoc, "vegaline/modules/esp/shaders/vertex.vsh");
   }

   public void attach() {
      try {
         GL20.glUseProgram(this.programID);
      } catch (Exception var2) {
         var2.fillInStackTrace();
      }
   }

   public void detach() {
      GL20.glUseProgram(0);
   }

   public static String readInputStream(InputStream inputStream) {
      StringBuilder stringBuilder = new StringBuilder();

      try {
         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

         String line;
         while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append('\n');
         }
      } catch (Exception var4) {
         var4.printStackTrace();
      }

      return stringBuilder.toString();
   }

   private int createShader(InputStream inputStream, int shaderType) {
      try {
         int shader = GL20.glCreateShader(shaderType);
         GL20.glShaderSource(shader, readInputStream(inputStream));
         GL20.glCompileShader(shader);
         return shader;
      } catch (Exception var4) {
         var4.fillInStackTrace();
         return 0;
      }
   }
}
