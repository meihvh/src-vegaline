package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.Event3D;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ColorSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Movement.MoveMeHelp;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class JumpCircles extends Module {
   public static JumpCircles get;
   List<List<ResourceLocation>> animatedGroups = Arrays.asList(new ArrayList(), new ArrayList());
   FloatSettings MaxTime;
   FloatSettings Range;
   ModeSettings Animation;
   ModeSettings Texture;
   ModeSettings ColorMode;
   ColorSettings PickColor1;
   ColorSettings PickColor2;
   BoolSettings DeepestLight;
   private final String staticLoc = "vegaline/modules/jumpcircles/default/";
   private final String animatedLoc = "vegaline/modules/jumpcircles/animated/";
   private final ResourceLocation JUMP_CIRCLE = new ResourceLocation("vegaline/modules/jumpcircles/default/circle.png");
   private final ResourceLocation JUMP_KONCHAL = new ResourceLocation("vegaline/modules/jumpcircles/default/konchal.png");
   private static final List<JumpCircles.JumpRenderer> circles = new ArrayList<>();
   private final Tessellator tessellator = Tessellator.getInstance();
   private final BufferBuilder buffer = this.tessellator.getBuffer();

   private ResourceLocation jumpTexture(int index, float progress) {
      String tex = this.Texture.currentMode;
      if (!tex.equalsIgnoreCase("CubicalPieces") && !tex.equalsIgnoreCase("Leeches")) {
         return tex.equalsIgnoreCase("Circle") ? this.JUMP_CIRCLE : this.JUMP_KONCHAL;
      } else {
         List<ResourceLocation> currentGroupTextures = tex.equalsIgnoreCase("CubicalPieces") ? this.animatedGroups.get(0) : this.animatedGroups.get(1);
         boolean animateByProgress = tex.equalsIgnoreCase("Leeches");
         if (tex.equalsIgnoreCase("Leeches")) {
            progress += 0.6F;
         }

         float frameOffset01 = progress % 1.0F;
         if (!animateByProgress) {
            int ms = 1500;
            frameOffset01 = (float)((System.currentTimeMillis() + (long)index) % 1500L) / 1500.0F;
         }

         return currentGroupTextures.get((int)Math.min(frameOffset01 * ((float)currentGroupTextures.size() - 0.5F), (float)currentGroupTextures.size()));
      }
   }

   public JumpCircles() {
      super("JumpCircles", 0, Module.Category.RENDER);
      get = this;
      int[] groupsFramesLength = new int[]{100, 200};
      String[] groupsFramesFormat = new String[]{"jpeg", "png"};
      int groupIndex = groupsFramesLength.length - 1;

      for (boolean anotherSys = !System.getProperty("os.name").startsWith("Win"); groupIndex >= 0; groupIndex--) {
         int framesCounter = 0;

         while (framesCounter < groupsFramesLength[groupIndex]) {
            framesCounter++;
            ResourceLocation loc;
            if (anotherSys) {
               loc = new ResourceLocation(
                  "vegaline/modules/jumpcircles/animated/animation" + (groupIndex + 1) + "/circleframe_" + framesCounter + "." + groupsFramesFormat[groupIndex]
               );
            } else {
               mc.getTextureManager()
                  .bindTexture(
                     loc = new ResourceLocation(
                        "vegaline/modules/jumpcircles/animated/animation"
                           + (groupIndex + 1)
                           + "/circleframe_"
                           + framesCounter
                           + "."
                           + groupsFramesFormat[groupIndex]
                     )
                  );
            }

            this.animatedGroups.get(groupIndex).add(loc);
         }
      }

      this.settings
         .add(this.Animation = new ModeSettings("Animation", "FadeOutRotate", this, new String[]{"FadeInOutRotate", "FadeOutRotate", "FadeInExponent"}));
      this.settings.add(this.MaxTime = new FloatSettings("MaxTime", 3500.0F, 5000.0F, 500.0F, this));
      this.settings.add(this.Range = new FloatSettings("Range", 2.0F, 3.0F, 1.0F, this));
      this.settings.add(this.Texture = new ModeSettings("Texture", "Circle", this, new String[]{"Circle", "KonchalEbal", "CubicalPieces", "Leeches"}));
      this.settings.add(this.ColorMode = new ModeSettings("ColorMode", "Rainbow", this, new String[]{"Client", "Rainbow", "Picker", "PickerFade"}));
      this.settings
         .add(this.PickColor1 = new ColorSettings("PickColor1", ColorUtils.getColor(255, 80, 0), this, () -> this.ColorMode.currentMode.contains("Picker")));
      this.settings
         .add(this.PickColor2 = new ColorSettings("PickColor2", ColorUtils.getColor(255, 142, 0), this, () -> this.ColorMode.currentMode.endsWith("Fade")));
      this.settings.add(this.DeepestLight = new BoolSettings("DeepestLight", true, this));
      this.setDemand(3, 2);
   }

   public static void onEntityMove(Entity entityIn, Vec3d prev) {
      if (entityIn instanceof EntityPlayerSP base && base.isEntityAlive()) {
         double motionY = entityIn.posY - prev.yCoord;
         double[] motions = new double[]{0.42F, 0.20000004768365898};
         if (MoveMeHelp.isBlockAboveHead(entityIn)) {
            motions = new double[]{0.42F, 0.20000004768365898, 0.20000005F, 0.07840000152587834, 0.012500048F};
         }

         boolean spawn = false;
         double[] var7 = motions;
         int var8 = motions.length;

         for (int var9 = 0; var9 < var8; var9++) {
            Double cur = var7[var9];
            if (MathUtils.getDifferenceOf(motionY, cur) < 0.01) {
               spawn = true;
               break;
            }
         }

         if (entityIn.onGround != entityIn.rayGround && motionY > 0.0 || spawn) {
            addCircleForEntity(entityIn);
         }

         entityIn.rayGround = entityIn.onGround;
      }
   }

   private static void addCircleForEntity(Entity entity) {
      Vec3d vec = getVec3dFromEntity(entity).addVector(0.0, 0.001, 0.0);
      BlockPos pos = new BlockPos(vec);
      IBlockState state = mc.world.getBlockState(pos);
      if (state.getBlock() != Blocks.SNOW_LAYER && state.getBlock() != Blocks.SOUL_SAND) {
         for (EnumFacing facing : EnumFacing.values()) {
            if (facing.getAxis().isHorizontal()) {
               state = mc.world.getBlockState(pos.add(facing.getFrontOffsetX(), 0, facing.getFrontOffsetZ()));
               if (state.getBlock() == Blocks.SNOW_LAYER || state.getBlock() == Blocks.SOUL_SAND) {
                  vec = vec.addVector(0.0, 0.14, 0.0);
                  break;
               }
            }
         }
      } else {
         vec = vec.addVector(0.0, 0.14, 0.0);
      }

      circles.add(new JumpCircles.JumpRenderer(vec, circles.size()));
   }

   @EventTarget
   public void onRender3d(Event3D event) {
      if (circles.size() != 0) {
         circles.removeIf(circle -> (double)circle.getDeltaTime() >= 1.0);
         if (!circles.isEmpty()) {
            boolean preBindTex = this.Texture.currentMode.equalsIgnoreCase("CubicalPieces");
            float deepestLightAnim = this.DeepestLight.getAnimation();
            float immersiveStrengh = 0.0F;
            if (deepestLightAnim >= 0.003921569F) {
               String finalImmersiveStrengh = this.Texture.currentMode;
               switch (finalImmersiveStrengh) {
                  case "Circle":
                     immersiveStrengh = 0.05F;
                     break;
                  case "KonchalEbal":
                     immersiveStrengh = 0.04F;
                     break;
                  case "CubicalPieces":
                     immersiveStrengh = 0.08F;
                     break;
                  case "Leeches":
                     immersiveStrengh = 0.15F;
               }
            }

            float finalImmersiveStrengh = immersiveStrengh;
            String animationMode = this.Animation.getMode();
            this.setupDraw(
               () -> circles.forEach(
                     circle -> this.doCircle(
                           circle.pos,
                           (double)this.Range.getAnimation(),
                           1.0F - circle.getDeltaTime(),
                           circle.getIndex() * 30,
                           !preBindTex,
                           deepestLightAnim,
                           finalImmersiveStrengh,
                           animationMode
                        )
                  ),
               preBindTex
            );
         }
      }
   }

   private int getColor(int index, float alphaPC) {
      String colorMode = this.ColorMode.currentMode;
      int color = 0;
      switch (colorMode) {
         case "Client":
            color = ClientColors.getColor1(index, alphaPC);
            break;
         case "Rainbow":
            color = ColorUtils.swapAlpha(ColorUtils.rainbowGui(0, (long)index), 255.0F * alphaPC);
            break;
         case "Picker":
            color = ColorUtils.swapAlpha(this.PickColor1.color, (float)ColorUtils.getAlphaFromColor(this.PickColor1.color) * alphaPC);
            break;
         case "PickerFade":
            color = ColorUtils.fadeColor(
               ColorUtils.swapAlpha(this.PickColor1.color, (float)ColorUtils.getAlphaFromColor(this.PickColor1.color) * alphaPC),
               ColorUtils.swapAlpha(this.PickColor2.color, (float)ColorUtils.getAlphaFromColor(this.PickColor2.color) * alphaPC),
               0.3F,
               (int)((float)index / 0.3F / 8.0F)
            );
      }

      return ColorUtils.getOverallColorFrom(color, ColorUtils.swapAlpha(-1, (float)ColorUtils.getAlphaFromColor(color)), 0.125F);
   }

   private void doCircle(
      Vec3d pos, double maxRadius, float deltaTime, int index, boolean doBindTex, float immersiveShift, float immersiveIntense, String animationMode
   ) {
      boolean immersive = immersiveShift >= 0.003921569F;
      float waveDelta = MathUtils.valWave01(1.0F - deltaTime);
      float alphaPC = 1.0F;
      float radius = 0.0F;
      double rotate = 0.0;
      switch (animationMode) {
         case "FadeInOutRotate":
            alphaPC = (float)MathUtils.easeOutCirc((double)MathUtils.valWave01(1.0F - deltaTime));
            if (deltaTime < 0.5F) {
               alphaPC *= (float)MathUtils.easeInOutExpo((double)alphaPC);
            }

            radius = (float)(
               (deltaTime > 0.5F ? MathUtils.easeOutElastic((double)(waveDelta * waveDelta)) : MathUtils.easeOutBack((double)waveDelta)) * maxRadius
            );
            rotate = MathUtils.easeInOutElastic((double)waveDelta) * 90.0 / (1.0 + (double)waveDelta);
            break;
         case "FadeOutRotate":
            alphaPC = deltaTime < 0.5F ? (float)MathUtils.easeInOutQuad((double)waveDelta) : waveDelta;
            radius = (1.0F - (float)MathUtils.easeInOutQuad((double)deltaTime)) * (float)maxRadius;
            rotate = (double)((float)MathUtils.easeInOutElastic((double)waveDelta) * -90.0F);
            break;
         case "FadeInExponent":
            alphaPC = (float)MathUtils.easeInOutQuad((double)waveDelta);
            float c1 = 1.70158F;
            float c3 = c1 + 1.0F;
            radius = (float)(1.0 + (double)c3 * Math.pow((double)(deltaTime - 1.0F), 3.0) + (double)c1 * Math.pow((double)(deltaTime - 1.0F), 2.0))
               * (float)maxRadius;
      }

      if (doBindTex) {
         mc.getTextureManager().bindTexture(this.jumpTexture(index, deltaTime));
      }

      this.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      this.buffer.pos(0.0, 0.0).tex(0.0, 0.0).color(this.getColor(index, alphaPC)).endVertex();
      this.buffer.pos(0.0, (double)radius).tex(0.0, 1.0).color(this.getColor((int)(324.0F + (float)index), alphaPC)).endVertex();
      this.buffer.pos((double)radius, (double)radius).tex(1.0, 1.0).color(this.getColor((int)(648.0F + (float)index), alphaPC)).endVertex();
      this.buffer.pos((double)radius, 0.0).tex(1.0, 0.0).color(this.getColor((int)(972.0F + (float)index), alphaPC)).endVertex();
      GL11.glPushMatrix();
      GL11.glTranslated(pos.xCoord - (double)radius / 2.0, pos.yCoord, pos.zCoord - (double)radius / 2.0);
      GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
      RenderUtils.customRotatedObject2D(0.0F, 0.0F, radius, radius, rotate);
      this.tessellator.draw(2);
      GL11.glPopMatrix();
      if (immersive) {
         int[] colors = new int[]{
            this.getColor(index, 1.0F),
            this.getColor((int)(324.0F + (float)index), 1.0F),
            this.getColor((int)(648.0F + (float)index), 1.0F),
            this.getColor((int)(972.0F + (float)index), 1.0F)
         };
         float minAPC = immersiveIntense * immersiveShift;
         float maxAPC = (float)MathUtils.easeInOutQuad((double)alphaPC);
         float polygons = 40.0F * maxAPC;
         float extMaxY = radius * maxAPC / 4.0F;
         float extMaxXZ = radius / 12.0F;
         this.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

         for (int i = 1; i < (int)polygons; i++) {
            float iPC = (float)i / polygons;
            float extY = extMaxY * (float)i / polygons - extMaxY / polygons;
            float aPC;
            if (!((aPC = MathUtils.lerp(maxAPC * minAPC, 0.0F, iPC * ((polygons - 1.0F) / polygons))) * 255.0F < 1.0F)) {
               float radiusPost = radius + (float)MathUtils.easeOutCirc((double)MathUtils.valWave01(iPC - 1.5F / polygons)) * extMaxXZ;
               this.buffer
                  .pos((double)(-radiusPost / 2.0F), (double)extY, (double)(-radiusPost / 2.0F))
                  .tex(0.0, 0.0)
                  .color(ColorUtils.toDark(colors[0], aPC))
                  .endVertex();
               this.buffer
                  .pos((double)(-radiusPost / 2.0F), (double)extY, (double)(radiusPost / 2.0F))
                  .tex(0.0, 1.0)
                  .color(ColorUtils.toDark(colors[1], aPC))
                  .endVertex();
               this.buffer
                  .pos((double)(radiusPost / 2.0F), (double)extY, (double)(radiusPost / 2.0F))
                  .tex(1.0, 1.0)
                  .color(ColorUtils.toDark(colors[2], aPC))
                  .endVertex();
               this.buffer
                  .pos((double)(radiusPost / 2.0F), (double)extY, (double)(-radiusPost / 2.0F))
                  .tex(1.0, 0.0)
                  .color(ColorUtils.toDark(colors[3], aPC))
                  .endVertex();
            }
         }

         GL11.glPushMatrix();
         GL11.glTranslated(pos.xCoord, pos.yCoord, pos.zCoord);
         GL11.glRotated(rotate, 0.0, -1.0, 0.0);
         this.tessellator.draw();
         GL11.glPopMatrix();
      }
   }

   @Override
   public void onToggled(boolean actived) {
      circles.clear();
      super.onToggled(actived);
   }

   private static Vec3d getVec3dFromEntity(Entity entityIn) {
      float PT = mc.getRenderPartialTicks();
      double dx = entityIn.posX - entityIn.lastTickPosX;
      double dy = entityIn.posY - entityIn.lastTickPosY;
      double dz = entityIn.posZ - entityIn.lastTickPosZ;
      return new Vec3d(entityIn.lastTickPosX + dx * (double)PT, entityIn.lastTickPosY + dy * (double)PT, entityIn.lastTickPosZ + dz * (double)PT);
   }

   private void setupDraw(Runnable render, boolean preBindTex) {
      EntityRenderer renderer = mc.entityRenderer;
      Vec3d revert = new Vec3d(RenderManager.viewerPosX, RenderManager.viewerPosY, RenderManager.viewerPosZ);
      boolean light = GL11.glIsEnabled(2896);
      boolean doShade = this.ColorMode.currentMode.equalsIgnoreCase("Picker") || this.getColor(0, 1.0F) != this.getColor(90, 1.0F);
      GL11.glPushMatrix();
      GL11.glEnable(3042);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.0F);
      GL11.glDepthMask(false);
      GL11.glDisable(2884);
      if (light) {
         GL11.glDisable(2896);
      }

      renderer.disableLightmap();
      GL11.glShadeModel(doShade ? 7425 : 7424);
      GL11.glBlendFunc(770, 1);
      GL11.glTranslated(-revert.xCoord, -revert.yCoord, -revert.zCoord);
      if (preBindTex) {
         mc.getTextureManager().bindTexture(this.jumpTexture(0, 0.0F));
      }

      GL11.glTexParameteri(3553, 10240, 9728);
      render.run();
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glBlendFunc(770, 771);
      GL11.glColor3f(1.0F, 1.0F, 1.0F);
      GL11.glShadeModel(7424);
      if (light) {
         GL11.glEnable(2896);
      }

      GL11.glEnable(2884);
      GL11.glDepthMask(true);
      GL11.glAlphaFunc(516, 0.1F);
      GL11.glEnable(3008);
      GL11.glPopMatrix();
   }

   public static void drawJumpCircle(Vec3d pos, float radius, float alphaPC, float shadowSize, boolean inside, boolean outside) {
      GlStateManager.pushMatrix();
      GlStateManager.enableBlend();
      GL11.glBlendFunc(770, 1);
      GL11.glEnable(2848);
      GlStateManager.disableDepth();
      GlStateManager.disableTexture2D();
      GlStateManager.disableAlpha();
      GlStateManager.disableLighting();
      GL11.glShadeModel(7425);
      GL11.glDisable(2884);
      GL11.glEnable(2929);
      GlStateManager.depthMask(false);
      Minecraft mc = Minecraft.getMinecraft();
      double x = pos.xCoord;
      double y = pos.yCoord;
      double z = pos.zCoord;
      int stepRad = 6;
      GL11.glBegin(inside && outside ? 8 : 5);
      int[] colors = new int[361];

      for (int i = 0; i < 360; i++) {
         colors[i] = ClientColors.getColor1(i * 3, alphaPC);
      }

      colors[360] = colors[359];
      if (inside && outside) {
         for (int i = 0; i <= 360; i += stepRad) {
            int color = colors[i];
            RenderUtils.glColor(0);
            GL11.glVertex3d(
               x - Math.sin(Math.toRadians((double)i)) * (double)(radius - radius * shadowSize),
               y,
               z + Math.cos(Math.toRadians((double)i)) * (double)(radius - radius * shadowSize)
            );
            RenderUtils.glColor(color);
            GL11.glVertex3d(x - Math.sin(Math.toRadians((double)i)) * (double)radius, y, z + Math.cos(Math.toRadians((double)i)) * (double)radius);
         }

         for (int i = 0; i <= 360; i += stepRad) {
            int color = colors[i];
            RenderUtils.glColor(0);
            GL11.glVertex3d(
               x - Math.sin(Math.toRadians((double)i)) * (double)(radius + radius * shadowSize),
               y,
               z + Math.cos(Math.toRadians((double)i)) * (double)(radius + radius * shadowSize)
            );
            RenderUtils.glColor(color);
            GL11.glVertex3d(x - Math.sin(Math.toRadians((double)i)) * (double)radius, y, z + Math.cos(Math.toRadians((double)i)) * (double)radius);
         }
      } else {
         for (int i = 0; i <= 360; i += stepRad) {
            int color = colors[i];
            RenderUtils.glColor(0);
            if (inside) {
               GL11.glVertex3d(
                  x - Math.sin(Math.toRadians((double)i)) * (double)(radius - radius * shadowSize),
                  y,
                  z + Math.cos(Math.toRadians((double)i)) * (double)(radius - radius * shadowSize)
               );
            } else if (outside) {
               GL11.glVertex3d(
                  x - Math.sin(Math.toRadians((double)i)) * (double)(radius + radius * shadowSize),
                  y,
                  z + Math.cos(Math.toRadians((double)i)) * (double)(radius + radius * shadowSize)
               );
            }

            RenderUtils.glColor(color);
            GL11.glVertex3d(x - Math.sin(Math.toRadians((double)i)) * (double)radius, y, z + Math.cos(Math.toRadians((double)i)) * (double)radius);
         }
      }

      GL11.glEnd();
      GL11.glBlendFunc(770, 771);
      GlStateManager.enableAlpha();
      GL11.glShadeModel(7424);
      GL11.glDisable(2848);
      GL11.glEnable(2884);
      GlStateManager.enableTexture2D();
      GlStateManager.enableDepth();
      GlStateManager.resetColor();
      GlStateManager.popMatrix();
   }

   private static final class JumpRenderer {
      private final long time = System.currentTimeMillis();
      private final Vec3d pos;
      int index;

      private JumpRenderer(Vec3d pos, int index) {
         this.pos = pos;
         this.index = index;
      }

      private float getDeltaTime() {
         return (float)(System.currentTimeMillis() - this.time) / JumpCircles.get.MaxTime.getAnimation();
      }

      private int getIndex() {
         return this.index;
      }
   }
}
