package net.minecraft.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.BlockRenderLayer;
import optifine.Config;
import org.lwjgl.opengl.GL11;
import ru.govno.client.module.modules.WorldRender;
import shadersmod.client.ShadersRender;

public class VboRenderList extends ChunkRenderContainer {
   @Override
   public void renderChunkLayer(BlockRenderLayer layer) {
      if (this.initialized) {
         for (RenderChunk renderchunk : this.renderChunks) {
            VertexBuffer vertexbuffer = renderchunk.getVertexBufferByLayer(layer.ordinal());
            GlStateManager.pushMatrix();
            this.preRenderChunk(renderchunk);
            renderchunk.multModelviewMatrix();
            vertexbuffer.bindBuffer();
            this.setupArrayPointers();
            vertexbuffer.drawArrays(7);
            if (WorldRender.get.isActived() && WorldRender.get.BlocksAlignment.getBool()) {
               GL11.glDepthMask(false);
               GL11.glEnable(2832);
               GL11.glEnable(3042);
               GL11.glBlendFunc(770, 1);
               float dHWFactor = Math.min((float)(Minecraft.getMinecraft().displayWidth * Minecraft.getMinecraft().displayHeight) / 9830400.0F * 1.2F, 1.0F);
               GL11.glPointSize(0.25F + dHWFactor * 2.0F);
               vertexbuffer.drawArrays(0);
               GL11.glBlendFunc(770, 771);
               GL11.glPointSize(1.0F);
               GL11.glDepthMask(true);
            }

            GlStateManager.popMatrix();
         }

         OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
         GlStateManager.resetColor();
         this.renderChunks.clear();
      }
   }

   private void setupArrayPointers() {
      if (Config.isShaders()) {
         ShadersRender.setupArrayPointersVbo();
      } else {
         GlStateManager.glVertexPointer(3, 5126, 28, 0);
         GlStateManager.glColorPointer(4, 5121, 28, 12);
         GlStateManager.glTexCoordPointer(2, 5126, 28, 16);
         OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
         GlStateManager.glTexCoordPointer(2, 5122, 28, 24);
         OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
      }
   }
}
