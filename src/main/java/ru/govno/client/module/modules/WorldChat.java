/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 */
package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.Event3D;
import ru.govno.client.event.events.EventReceivePacket;
import ru.govno.client.module.Module;
import ru.govno.client.module.modules.ClientColors;
import ru.govno.client.module.modules.WallHack;
import ru.govno.client.module.modules.WorldRender;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ColorSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.ReplaceStrUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.MusicHelper;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Render.Vec3dColored;

public class WorldChat
extends Module {
    private final ResourceLocation BLOOM_TEX = new ResourceLocation("vegaline/modules/worldchat/bloom.png");
    private float scale;
    private final Random RANDOM = new Random(8123648L);
    private final TimerHelper timerSFX = TimerHelper.TimerHelperReseted();
    private final FloatSettings ReadTimeMul;
    private final FloatSettings Scale;
    private final ModeSettings ColorMode;
    private final ColorSettings PickColor1;
    private final ColorSettings PickColor2;
    private final BoolSettings SelfDetect;
    private final BoolSettings SoundFX;
    private final BoolSettings TrailsOnFall;
    private final BoolSettings BloomText;
    private final BoolSettings CopyPlayerMovement;
    private final List<String> keyboardAlphabetWhitelist = List.of("\u0410\u0411\u0412\u0413\u0414\u0415\u0401\u0416\u0417\u0418\u0419\u041a\u041b\u041c\u041d\u041e\u041f\u0420\u0421\u0422\u0423\u0424\u0425\u0426\u0427\u0428\u0429\u042a\u042b\u042c\u042d\u042e\u042f\u0430\u0431\u0432\u0433\u0434\u0435\u0451\u0436\u0437\u0438\u0439\u043a\u043b\u043c\u043d\u043e\u043f\u0440\u0441\u0442\u0443\u0444\u0445\u0446\u0447\u0448\u0449\u044a\u044b\u044c\u044d\u044e\u044f ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-+=()[].,!;\u2013:%$`~".toCharArray()).stream().map(t -> String.valueOf(t)).toList();
    private final List<String> chatStringFormats = List.of("0123456789abcdefklmnor".toCharArray()).stream().map(t -> "\u00a7" + String.valueOf(t)).map(t -> "\u00a7" + t).toList();
    private final List<StringToParticles> stringLinesToParticles = new ArrayList<StringToParticles>();
    private int intTicksDelayToSpawnParticles;

    public WorldChat() {
        super("WorldChat", 0, Module.Category.RENDER);
        this.ReadTimeMul = new FloatSettings("ReadTimeMul", 1.0f, 2.0f, 0.5f, this);
        this.settings.add(this.ReadTimeMul);
        this.SoundFX = new BoolSettings("KeyboardSFX", true, this);
        this.settings.add(this.SoundFX);
        this.TrailsOnFall = new BoolSettings("TrailsOnFall", true, this);
        this.settings.add(this.TrailsOnFall);
        this.BloomText = new BoolSettings("BloomText", true, this);
        this.settings.add(this.BloomText);
        this.CopyPlayerMovement = new BoolSettings("CopyPlayerMovement", false, this);
        this.settings.add(this.CopyPlayerMovement);
        this.Scale = new FloatSettings("Scale", 1.0f, 2.0f, 0.25f, this);
        this.settings.add(this.Scale);
        this.ColorMode = new ModeSettings("ColorMode", "Rainbow", this, new String[]{"Client", "Rainbow", "Picker", "PickerFade"});
        this.settings.add(this.ColorMode);
        this.PickColor1 = new ColorSettings("PickColor1", ColorUtils.getColor(255, 80, 0), this, () -> this.ColorMode.currentMode.contains("Picker"));
        this.settings.add(this.PickColor1);
        this.PickColor2 = new ColorSettings("PickColor2", ColorUtils.getColor(255, 142, 0), this, () -> this.ColorMode.currentMode.endsWith("Fade"));
        this.settings.add(this.PickColor2);
        this.SelfDetect = new BoolSettings("SelfDetect", true, this);
        this.settings.add(this.SelfDetect);
    }

    @Override
    public boolean isBetaModule() {
        return true;
    }

    private long playSoundDelay() {
        return this.RANDOM.nextLong(1L, 110L);
    }

    private void playSFX(double posX, double posY, double posZ, boolean isDetach) {
        if (!this.timerSFX.hasReached(this.playSoundDelay() * (long)(isDetach ? 2 : 1)) || !this.SoundFX.getBool()) {
            return;
        }
        double dst = this.cameraPos().distanceTo(new Vec3d(posX, posY, posZ));
        float volume = (float)(1.0 - Math.max(dst / 20.0, 0.0));
        if (volume <= 0.0f || volume > 1.0f) {
            return;
        }
        this.timerSFX.reset();
        String sfxName = (String)(isDetach ? "abcdetouchfx" : "keypressfx" + this.RANDOM.nextInt(1, 6)) + ".wav";
        MusicHelper.playSoundInstant(sfxName, volume * (isDetach ? 0.1f : 0.35f));
    }

    private List<String> getAllPlayersDisplayNames(boolean asDisplayName) {
        if (mc.getConnection() != null && mc.getConnection().getPlayerInfoMap() != null) {
            if (asDisplayName) {
                return mc.getConnection().getPlayerInfoMap().stream().filter(Objects::nonNull).map(info -> info.getDisplayName() == null ? null : info.getDisplayName().getFormattedText()).filter(Objects::nonNull).toList();
            }
            return mc.getConnection().getPlayerInfoMap().stream().filter(Objects::nonNull).map(info -> info.getGameProfile() == null ? null : info.getGameProfile().getName()).filter(Objects::nonNull).toList();
        }
        return new ArrayList<String>();
    }

    private String getMessageAuthor(SPacketChat packet) {
        String[] splitText;
        String text;
        if (packet.getChatComponent() != null && !(text = packet.getChatComponent().getFormattedText()).isEmpty() && text.length() > 4 && (splitText = text.split(" ")).length > 1) {
            List<String> allNames = this.getAllPlayersDisplayNames(false);
            if (allNames.isEmpty()) {
                return null;
            }
            int maxSplitsChecks = 4;
            int splitsCounter = 0;
            String returningName = null;
            for (String split : splitText) {
                for (String netName : allNames) {
                    if (!ReplaceStrUtils.deformatString(ReplaceStrUtils.deformatString(split, 1), 0).contains(netName)) continue;
                    returningName = netName;
                    break;
                }
                if (returningName != null) {
                    return returningName;
                }
                if (++splitsCounter > maxSplitsChecks) break;
            }
        }
        return null;
    }

    private EntityPlayer getMessageAuthorEntity(SPacketChat packet) {
        String authorName = this.getMessageAuthor(packet);
        if (authorName == null || WorldChat.mc.world == null || WorldChat.mc.world.playerEntities.isEmpty()) {
            return null;
        }
        for (EntityPlayer player : WorldChat.mc.world.playerEntities) {
            String nameTrim;
            String tempPlayerName;
            if (player == null || !player.isEntityAlive() || !this.SelfDetect.getBool() && player instanceof EntityPlayerSP || (tempPlayerName = player.getName()).equalsIgnoreCase("Unknown") || !(nameTrim = tempPlayerName.trim()).contains(authorName)) continue;
            return player;
        }
        return null;
    }

    private String prepareFormatToEmptyParagraph(String stringIn) {
        for (String format : this.chatStringFormats) {
            stringIn = stringIn.replaceAll(format, "\u00a7");
        }
        return stringIn;
    }

    private String getClearMessage(SPacketChat packet) {
        if (packet.getChatComponent() != null) {
            String text = packet.getChatComponent().getFormattedText();
            if (text.isEmpty()) {
                return null;
            }
            text = ReplaceStrUtils.deformatString(ReplaceStrUtils.deformatString(text, 1), 0);
            String reverseText = new StringBuilder(text).reverse().toString();
            reverseText = this.prepareFormatToEmptyParagraph(reverseText);
            Object cutReversed = "";
            int attempts = 0;
            int maxAttempts = 2;
            for (char theChar : reverseText.toCharArray()) {
                String abc = String.valueOf(theChar);
                if (attempts >= maxAttempts && this.keyboardAlphabetWhitelist.stream().noneMatch(t -> t.contains(abc))) break;
                ++attempts;
                cutReversed = (String)cutReversed + abc;
            }
            return new StringBuilder(((String)cutReversed).trim()).reverse().toString();
        }
        return null;
    }

    private float getFont3dScale() {
        return this.scale;
    }

    private CFontRenderer getFont() {
        return Fonts.noise_18;
    }

    private float getABCWidth3d(CFontRenderer font, String abc) {
        float value = 0.0f;
        for (char theChar : abc.toCharArray()) {
            float w = font.getCharWidth(theChar) * (String.valueOf(theChar).equalsIgnoreCase(" ") ? 3.0f : 1.0f);
            value += w * this.getFont3dScale();
        }
        return value;
    }

    private float getABCHeight3d(CFontRenderer font, String abc) {
        return (font.getHeight() + 1.5f) * this.getFont3dScale();
    }

    int maxTrailLength() {
        return this.TrailsOnFall.getBool() ? 4 : 0;
    }

    private Vec3d cameraPos() {
        EntityPlayerSP player = Minecraft.player;
        if (player != null) {
            float partialTicks = mc.getRenderPartialTicks();
            float f = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
            float f1 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
            double d0 = player.prevPosX + (player.posX - player.prevPosX) * (double)partialTicks;
            double d1 = player.prevPosY + (player.posY - player.prevPosY) * (double)partialTicks + (double)player.getEyeHeight();
            double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * (double)partialTicks;
            f += WorldRender.get.offPitchOrient;
            f1 += WorldRender.get.offYawOrient;
            if (Minecraft.getMinecraft().gameSettings.thirdPersonView != 0) {
                int sideMul = Minecraft.getMinecraft().gameSettings.thirdPersonView == 1 ? 1 : -1;
                double camDist = WorldRender.get.cameraRedistance(4.0) * (double)sideMul;
                d0 += (double)MathHelper.sin(MathHelper.toRadians(f1)) * camDist;
                d1 += (double)MathHelper.sin(MathHelper.toRadians(f)) * camDist;
                d2 += (double)(-MathHelper.cos(MathHelper.toRadians(f1))) * camDist;
            }
            return new Vec3d(d0, d1, d2).add(WorldRender.get.getLastTranslated());
        }
        return new Vec3d(RenderManager.renderPosX, RenderManager.renderPosY, RenderManager.renderPosZ).add(WorldRender.get.getLastTranslated());
    }

    private Vec3d getRenderSpawnPosStringToParticles(EntityPlayer entityPlayer, float partialTicks) {
        double renderX = MathUtils.lerp(entityPlayer.prevPosX, entityPlayer.posX, (double)partialTicks);
        double renderY = MathUtils.lerp(entityPlayer.prevPosY, entityPlayer.posY, (double)partialTicks);
        double renderZ = MathUtils.lerp(entityPlayer.prevPosZ, entityPlayer.posZ, (double)partialTicks);
        if (entityPlayer instanceof EntityPlayerSP && WorldChat.mc.gameSettings.thirdPersonView == 0) {
            float rotationYawRad = MathHelper.toRadians(MathUtils.lerp(entityPlayer.prevRotationYaw, entityPlayer.rotationYaw, partialTicks));
            float extXZ = 3.0f;
            renderX -= (double)(MathHelper.sin(rotationYawRad) * extXZ);
            renderZ += (double)(MathHelper.cos(rotationYawRad) * extXZ);
        } else {
            renderY += (double)(entityPlayer.height / (entityPlayer.isChild() ? 2.0f : 1.0f) + 0.5f);
        }
        return new Vec3d(renderX, renderY, renderZ);
    }

    private boolean addStringToParticlesToList(EntityPlayer player, String hisMessage) {
        if (WorldChat.mc.world == null || player == null || hisMessage.isEmpty()) {
            return false;
        }
        if (WorldChat.mc.world.getEntityByID(player.getEntityId()) != null) {
            float partialTicks = mc.getRenderPartialTicks();
            boolean doCopyPlayerMovement = this.CopyPlayerMovement.getBool();
            Vec3d spawnPos = this.getRenderSpawnPosStringToParticles(player, partialTicks);
            float readTimeMultiplierAllConst = this.ReadTimeMul.getFloat();
            int textLength = hisMessage.length();
            int waitOnceAlphabet = (int)(50.0f * readTimeMultiplierAllConst);
            int maxRandomReadOffset = (int)(450.0f * readTimeMultiplierAllConst);
            long endAnimTextTime = (long)(500.0f * readTimeMultiplierAllConst) + (long)textLength * (long)waitOnceAlphabet;
            long readTime = (long)(2000.0f * readTimeMultiplierAllConst) + endAnimTextTime;
            long extraMaxLifeTime = readTime + 6500L;
            long layTextTime = 2700L;
            StringToParticles stringToParticlesObj = new StringToParticles(() -> player, spawnPos, this.cameraPos(), hisMessage, this.getFont(), readTime, maxRandomReadOffset, endAnimTextTime, extraMaxLifeTime, layTextTime, doCopyPlayerMovement);
            if (stringToParticlesObj != null) {
                try {
                    this.stringLinesToParticles.stream().filter(stringToParticles -> stringToParticles.entity != null && stringToParticles.entity.get() != null && stringToParticles.entity.get().getEntityId() == player.getEntityId()).forEach(StringToParticles::forceFall);
                    this.stringLinesToParticles.add(stringToParticlesObj);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        }
        return false;
    }

    @EventTarget
    public void onReceive(EventReceivePacket event) {
        String clear;
        SPacketChat packet;
        EntityPlayer player;
        Packet packet2 = event.getPacket();
        if (packet2 instanceof SPacketChat && (player = this.getMessageAuthorEntity(packet = (SPacketChat)packet2)) != null && (clear = this.getClearMessage(packet)) != null && clear.length() > 3 && this.addStringToParticlesToList(player, clear)) {
            this.intTicksDelayToSpawnParticles = 3;
        }
    }

    private float scaleMul() {
        return 0.025f;
    }

    @Override
    public void onUpdate() {
        float curScale = this.Scale.getAnimation() * this.scaleMul();
        if (curScale != this.scale) {
            this.scale = curScale;
            this.stringLinesToParticles.forEach(StringToParticles::forceFall);
        }
        if (this.intTicksDelayToSpawnParticles > 0) {
            --this.intTicksDelayToSpawnParticles;
        }
        if (this.stringLinesToParticles.isEmpty()) {
            return;
        }
        try {
            this.stringLinesToParticles.forEach(StringToParticles::updateParticlesList);
            this.stringLinesToParticles.removeIf(StringToParticles::removeIf);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @Override
    public void onToggled(boolean enable) {
        if (!this.stringLinesToParticles.isEmpty()) {
            this.stringLinesToParticles.clear();
        }
        super.onToggled(enable);
    }

    @EventTarget
    public void onRenderWorld(Event3D event) {
        if (!this.isActived() || this.stringLinesToParticles.isEmpty()) {
            return;
        }
        float curScale = this.Scale.getAnimation() * this.scaleMul();
        if (curScale != this.scale) {
            this.scale = curScale;
            this.stringLinesToParticles.forEach(StringToParticles::forceFall);
        }
        this.scale = this.Scale.getAnimation() * this.scaleMul();
        try {
            for (StringToParticles particles : new ArrayList<StringToParticles>(this.stringLinesToParticles)) {
                try {
                    particles.renderAllAlphabetParticles(event.getPartialTicks());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }
    }

    private class StringToParticles {
        private final CFontRenderer font;
        private final List<SingleStringParticle> abc_parts = new ArrayList<SingleStringParticle>();
        private final Supplier<Entity> entity;
        private boolean forceFall;
        private final boolean doCopyPlayerMovement;

        public StringToParticles(Supplier<Entity> entity, Vec3d spawnPos, Vec3d textSeenToPos, String stringIn, CFontRenderer font, long readTime, long readTimeMaxRandomForward, long endTextAnimTime, long extraMaxLifeTime, long layTime, boolean doCopyPlayerMovement) {
            Entity entity2;
            this.entity = entity;
            endTextAnimTime = Math.min(endTextAnimTime, readTime + readTimeMaxRandomForward);
            this.font = font;
            List<String> lineTexts = MathUtils.stringConvertToWidthLimitLinesWords(stringIn, font, 150.0f);
            float[] rotateToCamera = RotationUtil.getVecNeeded(textSeenToPos, spawnPos);
            if (entity != null && (entity2 = entity.get()) instanceof EntityPlayerSP) {
                EntityPlayerSP self = (EntityPlayerSP)entity2;
                if (Module.mc.gameSettings.thirdPersonView == 0) {
                    rotateToCamera = new float[]{MathUtils.wrapDegrees(Minecraft.player.rotationYaw - 180.0f), 5.0f};
                }
            }
            float yawRadian = MathHelper.toRadians(MathUtils.wrapAngleTo180_float(rotateToCamera[0]));
            float yawRadianLeft = MathHelper.toRadians(MathUtils.wrapAngleTo180_float(rotateToCamera[0] - 90.0f));
            float pitchRadian = MathHelper.toRadians(rotateToCamera[1]);
            float up3dFull = 0.0f;
            for (String text : lineTexts) {
                up3dFull -= WorldChat.this.getABCHeight3d(font, text);
            }
            int index = 0;
            for (String text : lineTexts) {
                float left3dFull = WorldChat.this.getABCWidth3d(font, text);
                float x2dExtTo3d = -left3dFull / 2.0f;
                float y2dExtTo3d = -up3dFull;
                int charIndex = 0;
                double zDance = (float)(-text.length()) / 100.0f;
                for (char theChar : text.toCharArray()) {
                    String alphaBet = String.valueOf(theChar);
                    if (!alphaBet.equalsIgnoreCase(" ")) {
                        float ciclePC = Math.max((float)index / (float)(stringIn.length() - 1), 0.0f);
                        float charPC01 = Math.min((float)charIndex / (float)text.length(), 1.0f);
                        float charPCWave = MathHelper.abs(MathHelper.sin(MathHelper.toRadians(MathUtils.valWave01(charPC01) * 90.0f)));
                        double xOffset = (double)(-MathHelper.sin(yawRadianLeft) * x2dExtTo3d) + (zDance != 0.0 ? (double)(-MathHelper.sin(yawRadian)) * zDance * (double)charPCWave : 0.0);
                        double yOffset = y2dExtTo3d;
                        double zOffset = (double)(MathHelper.cos(yawRadianLeft) * x2dExtTo3d) + (zDance != 0.0 ? (double)MathHelper.cos(yawRadian) * zDance * (double)charPCWave : 0.0);
                        Vec3d alphaBetPos = spawnPos.addVector(xOffset, yOffset, zOffset);
                        long abcWaitToRender = (long)MathUtils.lerp(0.0f, endTextAnimTime, ciclePC);
                        this.abc_parts.add(new SingleStringParticle(alphaBetPos, alphaBet, font, rotateToCamera[0], rotateToCamera[1], abcWaitToRender, WorldChat.this.RANDOM.nextLong(readTime, readTime + readTimeMaxRandomForward), extraMaxLifeTime, layTime, index));
                    }
                    ++index;
                    ++charIndex;
                    x2dExtTo3d += WorldChat.this.getABCWidth3d(font, alphaBet);
                }
                up3dFull += WorldChat.this.getABCHeight3d(font, text);
            }
            this.doCopyPlayerMovement = doCopyPlayerMovement;
        }

        public void forceFall() {
            if (!this.forceFall) {
                this.abc_parts.forEach(SingleStringParticle::forceFall);
                this.forceFall = true;
            }
        }

        public void updateParticlesList() {
            if (this.abc_parts.isEmpty()) {
                return;
            }
            this.abc_parts.removeIf(SingleStringParticle::removeIf);
            if (this.abc_parts.isEmpty()) {
                return;
            }
            if (this.doCopyPlayerMovement && this.entity != null && this.entity.get() != null && this.entity.get().isEntityAlive() && Module.mc.world != null && Module.mc.world.getEntityByID(this.entity.get().getEntityId()) != null && !this.forceFall) {
                this.abc_parts.forEach(part -> part.updateOffsetMoveEntity(this.entity.get()));
            }
            this.abc_parts.forEach(SingleStringParticle::update);
        }

        public void renderAllAlphabetParticles(float partialTicks) {
            this.abc_parts.forEach(particle -> particle.renderParticle(partialTicks));
        }

        public boolean removeIf() {
            return this.abc_parts.isEmpty();
        }
    }

    private class SingleStringParticle {
        private final String alphaBet;
        private final CFontRenderer font;
        private float yawToCamera;
        private float pitchToCamera;
        private final long waitToRender;
        private final long waitToFall;
        private final long extraMaxLifeTime;
        private final long layTime;
        private final int indexInLine;
        private final TimerHelper timerAlive = TimerHelper.TimerHelperReseted();
        private final TimerHelper timerCollide = TimerHelper.TimerHelperReseted();
        private final TimerHelper timerForRender = TimerHelper.TimerHelperReseted();
        private double posX;
        private double posY;
        private double posZ;
        private double prevPosX;
        private double prevPosY;
        private double prevPosZ;
        private double motionX;
        private double motionY;
        private double motionZ;
        private boolean hasCollide;
        private boolean hasPhysics;
        private boolean hasRenderStart;
        private boolean forceFall;
        SingleStringParticleTrail trail;

        public SingleStringParticle(Vec3d spawnPos, String alphaBet, CFontRenderer font, float yawToCamera, float pitchToCamera, long waitToRender, long waitToFall, long extraMaxLifeTime, long layTime, int indexInLine) {
            this.posX = spawnPos.xCoord;
            this.posY = spawnPos.yCoord;
            this.posZ = spawnPos.zCoord;
            this.prevPosX = spawnPos.xCoord;
            this.prevPosY = spawnPos.yCoord;
            this.prevPosZ = spawnPos.zCoord;
            this.alphaBet = alphaBet;
            this.font = font;
            this.yawToCamera = yawToCamera;
            this.pitchToCamera = pitchToCamera;
            this.waitToRender = waitToRender;
            this.waitToFall = waitToFall;
            this.extraMaxLifeTime = extraMaxLifeTime;
            this.layTime = layTime;
            this.indexInLine = indexInLine;
            float xzSpeedMax = 0.3f;
            float yMotionMax = 0.6f;
            this.motionX = WorldChat.this.RANDOM.nextFloat(-xzSpeedMax, xzSpeedMax);
            this.motionY = WorldChat.this.RANDOM.nextFloat(yMotionMax / 2.0f, yMotionMax);
            this.motionZ = WorldChat.this.RANDOM.nextFloat(-xzSpeedMax, xzSpeedMax);
            this.trail = new SingleStringParticleTrail();
        }

        public void updateOffsetMoveEntity(Entity entity) {
            double dx = entity.posX - entity.lastTickPosX;
            double dy = entity.posY - entity.lastTickPosY;
            double dz = entity.posZ - entity.lastTickPosZ;
            if (!this.hasCollide && !this.timerAlive.hasReached(this.waitToFall)) {
                this.prevPosX = this.posX;
                this.prevPosY = this.posY;
                this.prevPosZ = this.posZ;
                this.posX += dx;
                this.posY += dy;
                this.posZ += dz;
            }
        }

        public void update() {
            if (this.timerAlive.hasReached(this.waitToFall) || this.forceFall && this.hasRenderStart) {
                if (!this.hasPhysics) {
                    if (!this.alphaBet.equalsIgnoreCase(" ")) {
                        WorldChat.this.playSFX(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ, true);
                    }
                    this.hasPhysics = true;
                }
                this.prevPosX = this.posX;
                this.prevPosY = this.posY;
                this.prevPosZ = this.posZ;
                this.motionY -= (double)0.09f;
                this.motionX *= (double)0.94f;
                this.motionZ *= (double)0.94f;
                Vec3d posVec = new Vec3d(this.posX, this.posY, this.posZ);
                if (this.hasCollide || !Module.mc.world.getCollisionBoxes(null, new AxisAlignedBB(posVec).expandXyz(Math.sqrt(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ))).isEmpty()) {
                    Vec3d breakVec = posVec.addVector(this.motionX, this.motionY, this.motionZ);
                    RayTraceResult rayTraceResult = Module.mc.world.rayTraceBlocks(posVec, breakVec.add(new Vec3d(this.motionX, this.motionY, this.motionZ).scale(1000.0)), false, false, true);
                    if (rayTraceResult != null && rayTraceResult.hitVec != null) {
                        this.posX = rayTraceResult.hitVec.xCoord;
                        this.posY = rayTraceResult.hitVec.yCoord + (double)0.001f;
                        this.posZ = rayTraceResult.hitVec.zCoord;
                        this.trail.update(null, 0);
                    }
                    this.hasCollide = true;
                } else {
                    this.posX += this.motionX;
                    this.posY += this.motionY;
                    this.posZ += this.motionZ;
                    this.trail.update(new Vec3d(this.posX, this.posY, this.posZ), WorldChat.this.maxTrailLength());
                }
                this.pitchToCamera += 7.0f;
                this.pitchToCamera = Math.min(this.pitchToCamera, 90.0f);
                this.yawToCamera += 1.5f;
            }
            if (!this.hasCollide) {
                this.timerCollide.reset();
            }
        }

        private void renderAlphabetCentered2d(int color, boolean blooming) {
            float scX = this.font.getStringWidth(this.alphaBet) / 2.0f;
            float scY = this.font.getHeight() / 2.0f;
            GL11.glDisable((int)2884);
            int baseCol = blooming ? ColorUtils.getOverallColorFrom(color, ColorUtils.swapAlpha(-1, ColorUtils.getAlphaFromColor(color)), 0.2f) : color;
            float radiusF2 = scY * 2.25f;
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            GL11.glEnable((int)3553);
            Module.mc.getTextureManager().bindTexture(WorldChat.this.BLOOM_TEX);
            RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            int glowCol = ColorUtils.toDark(baseCol, blooming ? 0.15f : 0.07f);
            RenderUtils.buffer.pos(-radiusF2, -radiusF2).tex(0.0, 0.0).color(glowCol).endVertex();
            RenderUtils.buffer.pos(radiusF2, -radiusF2).tex(1.0, 0.0).color(glowCol).endVertex();
            RenderUtils.buffer.pos(radiusF2, radiusF2).tex(1.0, 1.0).color(glowCol).endVertex();
            RenderUtils.buffer.pos(-radiusF2, radiusF2).tex(0.0, 1.0).color(glowCol).endVertex();
            GL11.glBlendFunc((int)770, (int)1);
            RenderUtils.tessellator.draw();
            GL11.glBlendFunc((int)770, (int)771);
            GL11.glEnable((int)2884);
            GL11.glTranslatef((float)(-scX), (float)(-scY), (float)0.0f);
            if (blooming) {
                GL11.glBlendFunc((int)770, (int)1);
            }
            this.font.drawString(this.alphaBet, 0.0f, 0.0f, baseCol);
            if (blooming) {
                GL11.glBlendFunc((int)770, (int)771);
            }
            GL11.glTranslatef((float)scX, (float)scY, (float)0.0f);
        }

        private void renderTrail(int color, float alphaPC) {
            if (!this.trail.getPoses().isEmpty()) {
                int trailCount = this.trail.getPoses().size();
                int trailIndex = 0;
                ArrayList<Vec3dColored> trailPoints = new ArrayList<Vec3dColored>();
                for (Vec3d trailPoint : this.trail.getPoses()) {
                    float pcC = Math.min((float)trailIndex / (float)(trailCount - 1), 1.0f);
                    float trailPAPC = MathUtils.valWave01(pcC);
                    trailPoints.add(new Vec3dColored(trailPoint.xCoord, trailPoint.yCoord, trailPoint.zCoord, ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) * trailPAPC)));
                    ++trailIndex;
                }
                if (trailPoints.size() > 1) {
                    RenderUtils.buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
                    for (Vec3dColored vecTrail : trailPoints) {
                        RenderUtils.buffer.pos(vecTrail.getX(), vecTrail.getY(), vecTrail.getZ()).color(vecTrail.getColor()).endVertex();
                    }
                    GL11.glLineWidth((float)(0.01f + 0.75f * alphaPC));
                    GL11.glBlendFunc((int)770, (int)1);
                    GL11.glEnable((int)2848);
                    GL11.glHint((int)3154, (int)4354);
                    RenderUtils.tessellator.draw();
                    GL11.glHint((int)3154, (int)4352);
                    GL11.glDisable((int)2848);
                    GL11.glBlendFunc((int)770, (int)1);
                    GL11.glLineWidth((float)1.0f);
                }
            }
        }

        private Vec3d getRenderVec(float partialTicks) {
            return new Vec3d(MathUtils.lerp(this.prevPosX, this.posX, (double)partialTicks), MathUtils.lerp(this.prevPosY, this.posY, (double)partialTicks), MathUtils.lerp(this.prevPosZ, this.posZ, (double)partialTicks));
        }

        private float getAlphaPC01() {
            return Math.max(Math.min((1.0f - (float)this.timerCollide.getTime() / (float)this.layTime + (float)(this.timerAlive.getTime() / this.extraMaxLifeTime)) * 1.1f, 1.0f), 0.0f);
        }

        private int getColor(float alphaPC) {
            int color = 0;
            int index = 10000 - this.indexInLine * 40;
            switch (WorldChat.this.ColorMode.currentMode) {
                case "Client": {
                    color = ClientColors.getColor1(index, alphaPC);
                    break;
                }
                case "Rainbow": {
                    color = ColorUtils.swapAlpha(ColorUtils.rainbowGui(0, index), 255.0f * alphaPC);
                    break;
                }
                case "Picker": {
                    color = ColorUtils.swapAlpha(WorldChat.this.PickColor1.color, (float)ColorUtils.getAlphaFromColor(WorldChat.this.PickColor1.color) * alphaPC);
                    break;
                }
                case "PickerFade": {
                    color = ColorUtils.fadeColor(ColorUtils.swapAlpha(WorldChat.this.PickColor1.color, (float)ColorUtils.getAlphaFromColor(WorldChat.this.PickColor1.color) * alphaPC), ColorUtils.swapAlpha(WorldChat.this.PickColor2.color, (float)ColorUtils.getAlphaFromColor(WorldChat.this.PickColor2.color) * alphaPC), 0.3f, (int)((float)index / 0.3f / 8.0f));
                }
            }
            return color;
        }

        public void renderParticle(float partialTicks) {
            if (!this.timerForRender.hasReached(this.waitToRender)) {
                return;
            }
            long renderTime = this.timerForRender.getTime() - this.waitToRender;
            if (!this.hasRenderStart) {
                if (!this.alphaBet.equalsIgnoreCase(" ")) {
                    WorldChat.this.playSFX(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ, false);
                }
                this.hasRenderStart = true;
            }
            float flatAnimPC = this.forceFall ? 1.0f : Math.min((float)renderTime / 800.0f, 1.0f);
            float alphaPC = this.getAlphaPC01() * (0.9f + flatAnimPC * 0.1f);
            int color = this.getColor(alphaPC * (0.5f + 0.5f * MathUtils.lerp(flatAnimPC, 1.0f, MathUtils.lerp(flatAnimPC, 1.0f, flatAnimPC))));
            if ((float)ColorUtils.getAlphaFromColor(color) < 33.0f) {
                return;
            }
            Vec3d renderVec = this.getRenderVec(partialTicks);
            RenderUtils.setup3dForBlockPos(() -> {
                this.renderTrail(color, alphaPC);
                GL11.glEnable((int)3553);
                if (!WallHack.get.isActived() || !WallHack.get.Players.getBool() && !WallHack.get.Friends.getBool()) {
                    GL11.glEnable((int)2929);
                }
                GL11.glDepthMask((boolean)false);
                GL11.glTranslated((double)renderVec.xCoord, (double)renderVec.yCoord, (double)renderVec.zCoord);
                GL11.glNormal3d((double)1.0, (double)1.0, (double)1.0);
                GL11.glRotated((double)this.yawToCamera, (double)0.0, (double)-1.0, (double)0.0);
                GL11.glRotated((double)this.pitchToCamera, (double)1.0, (double)0.0, (double)0.0);
                float spawnAnimPC = 1.0f - (float)MathUtils.easeOutElastic(flatAnimPC);
                float spawnAnimPC2 = 1.0f - (float)MathUtils.easeOutBack(flatAnimPC);
                float scale = WorldChat.this.getFont3dScale() * (1.0f + spawnAnimPC2) * alphaPC;
                GL11.glTranslatef((float)(spawnAnimPC * scale * -8.0f), (float)(spawnAnimPC * scale * 2.0f), (float)(spawnAnimPC * scale * 10.0f));
                GL11.glScalef((float)scale, (float)(-scale), (float)scale);
                GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
                GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
                this.renderAlphabetCentered2d(color, WorldChat.this.BloomText.getBool());
                GL11.glDepthMask((boolean)true);
            }, false);
        }

        public void forceFall() {
            this.forceFall = true;
        }

        public boolean removeIf() {
            return this.timerCollide.hasReached(this.layTime) || this.timerAlive.hasReached(this.extraMaxLifeTime) || this.forceFall && !this.hasRenderStart;
        }
    }

    private class SingleStringParticleTrail {
        public List<Vec3d> poses = new ArrayList<Vec3d>();

        public void update(Vec3d renderPos, int maxCount) {
            if (maxCount <= 0 || renderPos == null) {
                if (!this.poses.isEmpty()) {
                    this.poses.clear();
                }
                return;
            }
            this.poses.add(renderPos);
            this.poses.removeIf(pos -> this.poses.size() > maxCount);
        }

        public List<Vec3d> getPoses() {
            return this.poses;
        }
    }
}
