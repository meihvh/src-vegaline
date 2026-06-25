package ru.govno.client.utils.Render.Shaders;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL20;
import ru.govno.client.utils.Render.ColorUtils;

public class StringShader {
   public static Minecraft mc = Minecraft.getMinecraft();
   private String shaderSource;
   public int shader;

   public void initShaderSource(String shaderSource) {
      this.shaderSource = shaderSource;
      if (shaderSource != null && !shaderSource.isEmpty()) {
         try {
            int programId = GL20.glCreateProgram();
            int shader = GL20.glCreateShader(35632);
            GL20.glShaderSource(
               shader,
               new BufferedReader(new InputStreamReader(new ByteArrayInputStream(shaderSource.getBytes())))
                  .lines()
                  .map(line -> line + "\n")
                  .collect(Collectors.joining())
            );
            GL20.glCompileShader(shader);
            GL20.glAttachShader(programId, shader);
            GL20.glLinkProgram(programId);
            this.shader = programId;
         } catch (Exception var4) {
            var4.printStackTrace();
         }
      }
   }

   public StringShader(String shaderSource) {
      this.initShaderSource(shaderSource);
   }

   public void attach() {
      GL20.glUseProgram(this.shader);
   }

   public void detach() {
      GL20.glUseProgram(0);
   }

   public void setUniformI(String name, int... args) {
      int loc = GL20.glGetUniformLocation(this.shader, name);
      switch (args.length) {
         case 1:
            ARBShaderObjects.glUniform1iARB(loc, args[0]);
            break;
         case 2:
            ARBShaderObjects.glUniform2iARB(loc, args[0], args[1]);
            break;
         case 3:
            ARBShaderObjects.glUniform3iARB(loc, args[0], args[1], args[2]);
            break;
         case 4:
            ARBShaderObjects.glUniform4iARB(loc, args[0], args[1], args[2], args[3]);
      }
   }

   public void setUniformF(String name, float... args) {
      int loc = GL20.glGetUniformLocation(this.shader, name);
      switch (args.length) {
         case 1:
            ARBShaderObjects.glUniform1fARB(loc, args[0]);
            break;
         case 2:
            ARBShaderObjects.glUniform2fARB(loc, args[0], args[1]);
            break;
         case 3:
            ARBShaderObjects.glUniform3fARB(loc, args[0], args[1], args[2]);
            break;
         case 4:
            ARBShaderObjects.glUniform4fARB(loc, args[0], args[1], args[2], args[3]);
      }
   }

   public void setUniformColor(String name, int color) {
      int loc = GL20.glGetUniformLocation(this.shader, name);
      ARBShaderObjects.glUniform4fARB(
         loc,
         ColorUtils.getGLRedFromColor(color),
         ColorUtils.getGLGreenFromColor(color),
         ColorUtils.getGLBlueFromColor(color),
         ColorUtils.getGLAlphaFromColor(color)
      );
   }

   public void use(Runnable renderers) {
      this.attach();
      renderers.run();
      this.detach();
   }
}
