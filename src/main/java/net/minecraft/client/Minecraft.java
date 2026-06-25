package net.minecraft.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMemoryErrorScreen;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenWorking;
import net.minecraft.client.gui.GuiSleepMP;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.ScreenChatOptions;
import net.minecraft.client.gui.advancements.GuiScreenAdvancements;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.recipebook.RecipeList;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.main.GameConfiguration;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.FoliageColorReloadListener;
import net.minecraft.client.resources.GrassColorReloadListener;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.client.resources.data.AnimationMetadataSectionSerializer;
import net.minecraft.client.resources.data.FontMetadataSection;
import net.minecraft.client.resources.data.FontMetadataSectionSerializer;
import net.minecraft.client.resources.data.LanguageMetadataSection;
import net.minecraft.client.resources.data.LanguageMetadataSectionSerializer;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.client.resources.data.PackMetadataSection;
import net.minecraft.client.resources.data.PackMetadataSectionSerializer;
import net.minecraft.client.resources.data.TextureMetadataSection;
import net.minecraft.client.resources.data.TextureMetadataSectionSerializer;
import net.minecraft.client.settings.CreativeSettings;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.client.util.ISearchTree;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.util.RecipeBookClient;
import net.minecraft.client.util.SearchTree;
import net.minecraft.client.util.SearchTreeManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ICrashReportDetail;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLeashKnot;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.CPacketLoginStart;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.profiler.ISnooperInfo;
import net.minecraft.profiler.Profiler;
import net.minecraft.profiler.Snooper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.MinecraftError;
import net.minecraft.util.MouseHelper;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.Session;
import net.minecraft.util.Timer;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentKeybind;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.storage.AnvilSaveConverter;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import optifine.Config;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.glu.GLU;
import ru.govno.client.Client;
import ru.govno.client.clickgui.ClickGuiScreen;
import ru.govno.client.event.events.EventBarBlockUse;
import ru.govno.client.event.events.EventCanPlaceBlock;
import ru.govno.client.event.events.EventInput;
import ru.govno.client.module.Module;
import ru.govno.client.module.modules.BadTrip;
import ru.govno.client.module.modules.Bypass;
import ru.govno.client.module.modules.ClickGui;
import ru.govno.client.module.modules.ClickTeleport;
import ru.govno.client.module.modules.ClientTune;
import ru.govno.client.module.modules.ComfortUi;
import ru.govno.client.module.modules.Criticals;
import ru.govno.client.module.modules.HitAura;
import ru.govno.client.module.modules.InvWalk;
import ru.govno.client.module.modules.Notifications;
import ru.govno.client.module.modules.OffHand;
import ru.govno.client.module.modules.PlayerHelper;
import ru.govno.client.module.modules.ProContainer;
import ru.govno.client.module.modules.PushAttack;
import ru.govno.client.module.modules.RadioPlayer;
import ru.govno.client.module.modules.ThrowFollow;
import ru.govno.client.module.modules.UiScaleControl;
import ru.govno.client.module.modules.WorldRender;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.soundengine.SoundMixFilter;
import ru.govno.client.utils.ClientRP;
import ru.govno.client.utils.DisplayCheck;
import ru.govno.client.utils.MusicHelper;
import ru.govno.client.utils.SessionSaver;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.UControllers.DualsenseBindAction;
import ru.govno.client.utils.UControllers.DualsenseController;
import ru.govno.client.utils.UControllers.DualsenseSettings;
import viamcp.fixes.AttackOrder;

public class Minecraft implements IThreadListener, ISnooperInfo {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final ResourceLocation LOCATION_MOJANG_PNG = new ResourceLocation("textures/gui/title/mojang.png");
   public static final boolean IS_RUNNING_ON_MAC = Util.getOSType() == Util.EnumOS.OSX;
   private static int loadingStage;
   private static int loadingStageMax;
   private static final boolean testStages = true;
   private static String currentStageTitle = "";
   public static boolean mcResourceManagerAccess;
   public static byte[] memoryReserve = new byte[10485760];
   private static final List<DisplayMode> MAC_DISPLAY_MODES = Lists.newArrayList(new DisplayMode(2560, 1600), new DisplayMode(2880, 1800));
   private final File fileResourcepacks;
   private final PropertyMap twitchDetails;
   public static double frameTime;
   private final PropertyMap profileProperties;
   private ServerData currentServerData;
   private final int loadProcessAA = 0;
   private static TextureManager renderEngine;
   private static Minecraft theMinecraft;
   private final DataFixer dataFixer;
   public PlayerControllerMP playerController;
   private boolean fullscreen;
   private final boolean enableGLErrorChecking = true;
   private boolean hasCrashed;
   private CrashReport crashReporter;
   public int displayWidth;
   public int displayHeight;
   private boolean connectedToRealms;
   public final Timer timer = new Timer(20.0F);
   private final Snooper usageSnooper = new Snooper("client", this, MinecraftServer.getCurrentTimeMillis());
   public WorldClient world;
   public RenderGlobal renderGlobal;
   public RenderManager renderManager;
   private RenderItem renderItem;
   private ItemRenderer itemRenderer;
   public static EntityPlayerSP player;
   @Nullable
   public Entity renderViewEntity;
   public Entity pointedEntity;
   public ParticleManager effectRenderer;
   private final SearchTreeManager field_193995_ae = new SearchTreeManager();
   public Session session;
   public boolean isGamePaused;
   public float field_193996_ah;
   public FontRenderer fontRendererObj;
   public FontRenderer standardGalacticFontRenderer;
   @Nullable
   public GuiScreen currentScreen;
   public LoadingScreenRenderer loadingScreen;
   public EntityRenderer entityRenderer;
   public DebugRenderer debugRenderer;
   private int leftClickCounter;
   private int tempDisplayWidth;
   private int tempDisplayHeight;
   @Nullable
   private IntegratedServer theIntegratedServer;
   public GuiIngame ingameGUI;
   public boolean skipRenderWorld;
   public RayTraceResult objectMouseOver;
   public GameSettings gameSettings;
   public CreativeSettings field_191950_u;
   public MouseHelper mouseHelper;
   public final File mcDataDir;
   public final File fileAssets;
   private final String launchedVersion;
   private final String versionType;
   private final Proxy proxy;
   private ISaveFormat saveLoader;
   private static int debugFPS;
   public int rightClickDelayTimer;
   private String serverName;
   private int serverPort;
   public boolean inGameHasFocus;
   long systemTime = getSystemTime();
   private int joinPlayerCounter;
   public final FrameTimer frameTimer = new FrameTimer();
   long startNanoTime = System.nanoTime();
   private final boolean jvm64bit;
   private final boolean isDemo;
   @Nullable
   private NetworkManager myNetworkManager;
   private boolean integratedServerIsRunning;
   public final Profiler mcProfiler = new Profiler();
   private long debugCrashKeyPressTime = -1L;
   private IReloadableResourceManager mcResourceManager;
   private static final MetadataSerializer metadataSerializer_ = new MetadataSerializer();
   private final List<IResourcePack> defaultResourcePacks = Lists.newArrayList();
   public final DefaultResourcePack mcDefaultResourcePack;
   private ResourcePackRepository mcResourcePackRepository;
   private LanguageManager mcLanguageManager;
   private BlockColors blockColors;
   private ItemColors itemColors;
   public Framebuffer framebufferMc;
   private TextureMap textureMapBlocks;
   public SoundMixFilter sndHandleEdit;
   private SoundHandler mcSoundHandler;
   private MusicTicker mcMusicTicker;
   private ResourceLocation mojangLogo;
   private final MinecraftSessionService sessionService;
   private SkinManager skinManager;
   private final Queue<FutureTask<?>> scheduledTasks = Queues.newArrayDeque();
   private final Thread mcThread = Thread.currentThread();
   private ModelManager modelManager;
   private BlockRendererDispatcher blockRenderDispatcher;
   private final GuiToast field_193034_aS;
   volatile boolean running = true;
   public String debug = "";
   public boolean renderChunksMany = true;
   private long debugUpdateTime = getSystemTime();
   private int fpsCounter;
   private boolean actionKeyF3;
   private final Tutorial field_193035_aW;
   long prevFrameTime = -1L;
   private String debugProfilerName = "root";
   public SessionSaver sessionSaver;
   public static boolean guiShFrame;
   public static ByteBuffer[] vlLogoWindowByteBuffers;
   private final TimerHelper delayWarmCrashCancel = TimerHelper.TimerHelperReseted();
   public static int unique_Index;
   public static long lastFrame;
   public static boolean launched;
   private final TimerHelper vlMusicTimer = new TimerHelper();
   TimerHelper f = TimerHelper.TimerHelperReseted();
   TimerHelper f1 = TimerHelper.TimerHelperReseted();
   public static boolean temporalImageSizeMoreThan16x = true;
   public boolean runScreenshot;
   public int ticksScreenshotsUpdate;

   public static void stage(String title) {
      if (!launched) {
         currentStageTitle = title;
         loadingStage++;
         if (loadingStage == loadingStageMax) {
            MusicHelper.playSoundInstant("loadingperclast.wav");
         } else {
            int pct0 = (int)(((float)loadingStage - 1.0F) / (float)loadingStageMax * 100.0F);
            int pct1 = (int)((float)loadingStage / (float)loadingStageMax * 100.0F);
            if (pct0 < pct1) {
               MusicHelper.playSoundInstant("loadingperc.wav", 0.02F);
            }

            try {
               Display.setTitle(
                  "Loading "
                     + Client.nameCut.toLowerCase()
                     + " v092 - "
                     + (int)Math.min((float)(loadingStage + 2) / (float)loadingStageMax * 100.0F, 100.0F)
                     + "% | stage: "
                     + loadingStage
               );
               getMinecraft().drawSplashScreen(renderEngine);
            } catch (Exception var5) {
               var5.fillInStackTrace();
            }
         }

         try {
            Thread.sleep(loadingStage == loadingStageMax ? 0L : 2L);
         } catch (InterruptedException var4) {
            var4.printStackTrace();
         }

         System.out.println(loadingStage + " | " + currentStageTitle);
      }
   }

   private static void stageMax(int stageMax) {
      loadingStageMax = stageMax;
   }

   private static float getLoadingStagePC() {
      return (float)loadingStage / (float)loadingStageMax;
   }

   public Minecraft(GameConfiguration gameConfig) {
      theMinecraft = this;
      this.mcDataDir = gameConfig.folderInfo.mcDataDir;
      this.fileAssets = gameConfig.folderInfo.assetsDir;
      this.fileResourcepacks = gameConfig.folderInfo.resourcePacksDir;
      this.launchedVersion = gameConfig.gameInfo.version;
      this.versionType = gameConfig.gameInfo.versionType;
      this.twitchDetails = gameConfig.userInfo.userProperties;
      this.profileProperties = gameConfig.userInfo.profileProperties;
      this.loadMojangAttributes();
      this.mcDefaultResourcePack = new DefaultResourcePack(gameConfig.folderInfo.getAssetsIndex());
      this.proxy = gameConfig.userInfo.proxy == null ? Proxy.NO_PROXY : gameConfig.userInfo.proxy;
      this.sessionService = new YggdrasilAuthenticationService(this.proxy, UUID.randomUUID().toString()).createMinecraftSessionService();
      this.session = gameConfig.userInfo.session;
      LOGGER.info("Setting user: {}", this.session.getUsername());
      LOGGER.debug("(Session ID is {})", this.session.getSessionID());
      this.isDemo = gameConfig.gameInfo.isDemo;
      this.displayWidth = 400;
      this.displayHeight = 72;
      this.tempDisplayWidth = 400;
      this.tempDisplayHeight = 72;
      this.fullscreen = false;
      this.jvm64bit = isJvm64bit();
      this.theIntegratedServer = null;
      if (gameConfig.serverInfo.serverName != null) {
         this.serverName = gameConfig.serverInfo.serverName;
         this.serverPort = gameConfig.serverInfo.serverPort;
      }

      this.loadServerLists();
      ImageIO.setUseCache(false);
      Locale.setDefault(Locale.ROOT);
      Bootstrap.register();
      TextComponentKeybind.field_193637_b = KeyBinding::func_193626_b;
      this.dataFixer = DataFixesManager.createFixer();
      this.field_193034_aS = new GuiToast(this);
      this.field_193035_aW = new Tutorial(this);
   }

   public void run() {
      this.running = true;

      try {
         Display.setTitle("Initialize" + Client.nameCut.toLowerCase() + "092 UID:" + Client.staticAddress);
         Client.initVia();
         this.startGame();
         Display.setTitle(Client.name + " " + Client.version + " UID:" + Client.staticAddress);
      } catch (Throwable var11) {
         CrashReport crashreport = CrashReport.makeCrashReport(var11, "Initializing game");
         crashreport.makeCategory("Initialization");
         this.displayCrashReport(this.addGraphicsAndWorldToCrashReport(crashreport));
         return;
      }

      try {
         while (this.running) {
            if (this.hasCrashed && this.crashReporter != null) {
               this.displayCrashReport(this.crashReporter);
            } else {
               try {
                  this.runGameLoop();
               } catch (OutOfMemoryError var10) {
                  this.freeMemory();
                  this.displayGuiScreen(new GuiMemoryErrorScreen());
                  System.gc();
               }
            }
         }

         return;
      } catch (MinecraftError var12) {
         var12.printStackTrace();
      } catch (ReportedException var13) {
         this.addGraphicsAndWorldToCrashReport(var13.getCrashReport());
         this.freeMemory();
         LOGGER.fatal("Reported exception thrown!", (Throwable)var13);
         this.displayCrashReport(var13.getCrashReport());
      } catch (Throwable var14) {
         CrashReport crashreport1 = this.addGraphicsAndWorldToCrashReport(new CrashReport("Unexpected error", var14));
         this.freeMemory();
         LOGGER.fatal("Unreported exception thrown!", var14);
         this.displayCrashReport(crashreport1);
      } finally {
         this.shutdownMinecraftApplet();
      }
   }

   private void startGame() throws LWJGLException, IOException {
      stageMax(427);
      this.sessionSaver = new SessionSaver(this);
      this.sessionSaver.load();
      stage("session saver");
      ClientRP.getInstance().init();
      stage("discord instance");
      this.gameSettings = new GameSettings(this, this.mcDataDir);
      this.field_191950_u = new CreativeSettings(this, this.mcDataDir);
      stage("creative settings");
      this.defaultResourcePacks.add(this.mcDefaultResourcePack);
      this.startTimerHackThread();
      if (this.gameSettings.overrideHeight > 0 && this.gameSettings.overrideWidth > 0) {
         this.displayWidth = this.gameSettings.overrideWidth;
         this.displayHeight = this.gameSettings.overrideHeight;
      }

      LOGGER.info("LWJGL Version: {}", Sys.getVersion());
      this.setWindowIcon();
      stage("window icon");
      this.setInitialDisplayMode();
      this.createDisplay();
         MusicHelper.playSound("loadingpercfirst.wav", 0.35F);
         stage("create display");
         this.framebufferMc = new Framebuffer(this.displayWidth, this.displayHeight, true);
         stage("main framebuffer create");
         this.framebufferMc.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
         stage("main framebuffer update");
         this.registerMetadataSerializers();
         stage("metadata serializers");
         this.mcResourcePackRepository = new ResourcePackRepository(
            this.fileResourcepacks, new File(this.mcDataDir, "server-resource-packs"), this.mcDefaultResourcePack, metadataSerializer_, this.gameSettings
         );
         stage("resourcepack repository");
         this.mcResourceManager = new SimpleReloadableResourceManager(metadataSerializer_);
         stage("resource manager");
         this.mcLanguageManager = new LanguageManager(metadataSerializer_, this.gameSettings.language);
         stage("language manager");
         this.mcResourceManager.registerReloadListener(this.mcLanguageManager);
         stage("language reload listener");
         this.refreshResources();
         mcResourceManagerAccess = true;
         stage("refresh resources");
         OpenGlHelper.initializeTextures();
         stage("init opengl");
         renderEngine = new TextureManager(this.mcResourceManager);
         stage("init render engine");
         this.mcResourceManager.registerReloadListener(renderEngine);
         stage("redister render engine");
         this.skinManager = new SkinManager(renderEngine, new File(this.fileAssets, "skins"), this.sessionService);
         stage("skin manager");
         this.saveLoader = new AnvilSaveConverter(new File(this.mcDataDir, "saves"), this.dataFixer);
         stage("anvil save converter");

         while (this.mcSoundHandler == null) {
            stage("sound handler pre init");
            this.mcSoundHandler = new SoundHandler(this.mcResourceManager, this.gameSettings);
            stage("init sound handler");
            this.sndHandleEdit = SoundMixFilter.makeDistorterMixer();
            stage("create rtx sound influence");
            if (this.sndHandleEdit.getHasMixerLoaded()) {
               this.sndHandleEdit.init();
            }

            stage("load rtx sound influence");
            this.mcResourceManager.registerReloadListener(this.mcSoundHandler);
            stage("register sound handler");
         }

         stage("load full sound system");
         this.mcMusicTicker = new MusicTicker(this);
         stage("music ticker");
         this.fontRendererObj = new FontRenderer(this.gameSettings, new ResourceLocation("textures/font/ascii.png"), renderEngine, false);
         stage("create mc font");
         if (this.gameSettings.language != null) {
            this.fontRendererObj.setUnicodeFlag(this.isUnicode());
            this.fontRendererObj.setBidiFlag(this.mcLanguageManager.isCurrentLanguageBidirectional());
         }

         stage("string render encoder");
         this.standardGalacticFontRenderer = new FontRenderer(this.gameSettings, new ResourceLocation("textures/font/ascii_sga.png"), renderEngine, false);
         stage("init mc font");
         this.mcResourceManager.registerReloadListener(this.fontRendererObj);
         stage("register mc font");
         this.mcResourceManager.registerReloadListener(this.standardGalacticFontRenderer);
         stage("register mc fontrenderer");
         this.mcResourceManager.registerReloadListener(new GrassColorReloadListener());
         stage("register glass blocks");
         this.mcResourceManager.registerReloadListener(new FoliageColorReloadListener());
         stage("register foliage color");
         this.mouseHelper = new MouseHelper();
         stage("init mouse helper");
         this.checkGLError("Pre startup");
         stage("pre startup");
         GlStateManager.enableTexture2D();
         stage("init texture renderer");
         GlStateManager.shadeModel(7425);
         stage("setup shade");
         GlStateManager.clearDepth(1.0);
         stage("setup depth stage 1");
         GlStateManager.enableDepth();
         stage("setup depth stage 2");
         GlStateManager.depthFunc(515);
         stage("setup depth stage 3");
         GlStateManager.enableAlpha();
         stage("setup alpha stage 1");
         GlStateManager.alphaFunc(516, 0.1F);
         stage("setup alpha stage 2");
         GlStateManager.cullFace(GlStateManager.CullFace.BACK);
         stage("setup culling face");
         GlStateManager.matrixMode(5889);
         stage("setup matrix");
         GlStateManager.loadIdentity();
         stage("setup identity");
         GlStateManager.matrixMode(5888);
         stage("setup matrix");
         this.checkGLError("Startup");
         stage("startup");
         this.textureMapBlocks = new TextureMap("textures");
         stage("init texture map");
         this.textureMapBlocks.setMipmapLevels(this.gameSettings.mipmapLevels);
         stage("mipmap levels");
         renderEngine.loadTickableTexture(TextureMap.LOCATION_BLOCKS_TEXTURE, this.textureMapBlocks);
         stage("load tickable textures");
         renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
         stage("bind blocks texture map");
         this.textureMapBlocks.setBlurMipmapDirect(false, this.gameSettings.mipmapLevels > 0);
         stage("mipmap setup blur");
         this.modelManager = new ModelManager(this.textureMapBlocks);
         stage("init model manager");
         this.mcResourceManager.registerReloadListener(this.modelManager);
         stage("load model manager");
         this.blockColors = BlockColors.init();
         stage("init block colors");
         this.itemColors = ItemColors.init(this.blockColors);
         stage("init item colors");
         this.renderItem = new RenderItem(renderEngine, this.modelManager, this.itemColors);
         stage("init render item");
         this.renderManager = new RenderManager(renderEngine, this.renderItem);
         stage("init render manager");
         this.itemRenderer = new ItemRenderer(this);
         stage("init item renderer");
         this.mcResourceManager.registerReloadListener(this.renderItem);
         stage("register render items");
         this.entityRenderer = new EntityRenderer(this, this.mcResourceManager);
         stage("load entity renderer");
         this.mcResourceManager.registerReloadListener(this.entityRenderer);
         stage("register entity renderer");
         this.blockRenderDispatcher = new BlockRendererDispatcher(this.modelManager.getBlockModelShapes(), this.blockColors);
         stage("init block dispatcher");
         this.mcResourceManager.registerReloadListener(this.blockRenderDispatcher);
         stage("register block dispatcher");
         this.renderGlobal = new RenderGlobal(this);
         stage("init render global");
         this.mcResourceManager.registerReloadListener(this.renderGlobal);
         stage("register render global");
         this.func_193986_ar();
         stage("register items map");
         this.mcResourceManager.registerReloadListener(this.field_193995_ae);
         stage("load all items");
         GlStateManager.viewport(0, 0, this.displayWidth, this.displayHeight);
         stage("setup viewport");
         this.effectRenderer = new ParticleManager(this.world, renderEngine);
         stage("init particle manager");
         this.checkGLError("Post startup");
         stage("gl err checks");
         this.ingameGUI = new GuiIngame(this);
         stage("init gui in game");
         this.displayGuiScreen(new GuiMainMenu());
         stage("init main menu");
         renderEngine.deleteTexture(this.mojangLogo);
         stage("delete mojang logo");
         this.mojangLogo = null;
         stage("post delete logo");
         this.loadingScreen = new LoadingScreenRenderer(this);
         stage("loading screen update");
         this.debugRenderer = new DebugRenderer(this);
         stage("init debug renderer");
         if (this.serverName != null) {
            this.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), this, this.serverName, this.serverPort));
         } else {
            this.displayGuiScreen(new GuiMainMenu());
         }

         stage("init main");
         renderEngine.deleteTexture(this.mojangLogo);
         stage("delete mojang logo");
         this.mojangLogo = null;
         stage("post delete logo");
         this.loadingScreen = new LoadingScreenRenderer(this);
         stage("loading screen event");
         this.debugRenderer = new DebugRenderer(this);
         stage("init debug renderer");

         try {
            Display.setVSyncEnabled(this.gameSettings.enableVsync);
         } catch (OpenGLException var6) {
            this.gameSettings.enableVsync = false;
            this.gameSettings.saveOptions();
            Sys.alert("Unknown err", "-e0");
         }

         stage("check vsync");
         this.renderGlobal.makeEntityOutlineShader();
         stage("make outline shader");
         Client.run();
         if (!launched) {
            try {
               Display.makeCurrent();
               stage("setup display stage 1");
               Display.setResizable(false);
               Display.setResizable(true);
               int pixWidth = this.tempDisplayWidth;
               int pixHeight = this.tempDisplayHeight;

               try {
                  GraphicsDevice firstDisplay = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                  pixWidth = firstDisplay.getDefaultConfiguration().getBounds().width;
                  pixHeight = firstDisplay.getDefaultConfiguration().getBounds().height;
               } catch (Exception var4) {
                  Sys.alert("Unknown err", "-e3");
               }

               this.fullscreen = false;
               this.tempDisplayWidth = this.displayWidth = Math.max((int)((float)pixWidth / 1.5F), 1280);
               this.tempDisplayHeight = this.displayHeight = Math.max((int)((float)pixHeight / 1.5F), 720);
               stage("setup display stage 2");
               if (!this.fullscreen) {
                  this.displayWidth = this.tempDisplayWidth;
                  this.displayHeight = this.tempDisplayHeight;
                  Display.setDisplayMode(new DisplayMode(this.displayWidth, this.displayHeight));
                  Display.setResizable(false);
                  Display.setResizable(true);
                  this.updateFramebufferSize();
               } else {
                  this.updateDisplayMode();
               }

               stage("setup display stage 3");
               stage("client loaded successfully!");
               Display.update();
               Thread.sleep(0L);
               this.updateFramebufferSize();
            } catch (Exception var5) {
               Sys.alert("Unknown err", "-q2 " + var5.getCause().getMessage());
            }

            launched = true;
         }
   }

   private void func_193986_ar() {
      SearchTree<ItemStack> searchtree = new SearchTree<>(
         p_193988_0_ -> p_193988_0_.getTooltip(null, ITooltipFlag.TooltipFlags.NORMAL)
               .stream()
               .map(TextFormatting::getTextWithoutFormattingCodes)
               .map(String::trim)
               .filter(p_193984_0_ -> !p_193984_0_.isEmpty())
               .collect(Collectors.toList()),
         p_193985_0_ -> Collections.singleton(Item.REGISTRY.getNameForObject(p_193985_0_.getItem()))
      );
      NonNullList<ItemStack> nonnulllist = NonNullList.func_191196_a();

      for (Item item : Item.REGISTRY) {
         item.getSubItems(CreativeTabs.SEARCH, nonnulllist);
      }

      nonnulllist.forEach(searchtree::func_194043_a);
      SearchTree<RecipeList> searchtree1 = new SearchTree<>(
         p_193990_0_ -> p_193990_0_.func_192711_b()
               .stream()
               .flatMap(p_193993_0_ -> p_193993_0_.getRecipeOutput().getTooltip(null, ITooltipFlag.TooltipFlags.NORMAL).stream())
               .map(TextFormatting::getTextWithoutFormattingCodes)
               .map(String::trim)
               .filter(p_193994_0_ -> !p_193994_0_.isEmpty())
               .collect(Collectors.toList()),
         p_193991_0_ -> p_193991_0_.func_192711_b()
               .stream()
               .map(p_193992_0_ -> Item.REGISTRY.getNameForObject(p_193992_0_.getRecipeOutput().getItem()))
               .collect(Collectors.toList())
      );
      RecipeBookClient.field_194087_f.forEach(searchtree1::func_194043_a);
      this.field_193995_ae.func_194009_a(SearchTreeManager.field_194011_a, searchtree);
      this.field_193995_ae.func_194009_a(SearchTreeManager.field_194012_b, searchtree1);
   }

   private void registerMetadataSerializers() {
      metadataSerializer_.registerMetadataSectionType(new TextureMetadataSectionSerializer(), TextureMetadataSection.class);
      metadataSerializer_.registerMetadataSectionType(new FontMetadataSectionSerializer(), FontMetadataSection.class);
      metadataSerializer_.registerMetadataSectionType(new AnimationMetadataSectionSerializer(), AnimationMetadataSection.class);
      metadataSerializer_.registerMetadataSectionType(new PackMetadataSectionSerializer(), PackMetadataSection.class);
      metadataSerializer_.registerMetadataSectionType(new LanguageMetadataSectionSerializer(), LanguageMetadataSection.class);
   }

   private void createDisplay() throws LWJGLException {
      Display.setResizable(false);
      Display.setTitle("Logging " + Client.nameCut.toLowerCase() + " 092 UID:" + Client.staticAddress);
      Display.setInitialBackground(0.105882354F, 0.105882354F, 0.12941177F);

      try {
         boolean customWindow = true;
         if (customWindow) {
            try {
               PixelFormat pixelFormat = new PixelFormat().withBitsPerPixel(32).withAlphaBits(8).withDepthBits(24).withStencilBits(8).withSRGB(true);
               Display.create(pixelFormat);
               System.out.println("-CUSTOM DISPLAY WAS OPENED");
            } catch (Exception var4) {
               Display.create(new PixelFormat().withDepthBits(24));
               System.out.println("-NORMAL DISPLAY CATCH, BAD DISPLAY WAS OPENED");
            }
         } else {
            Display.create(new PixelFormat().withDepthBits(24));
            System.out.println("-NORMAL DISPLAY CATCH, BAD DISPLAY WAS OPENED");
         }
      } catch (LWJGLException var5) {
         LOGGER.error("Couldn't set pixel format", (Throwable)var5);

         try {
            Thread.sleep(1000L);
         } catch (InterruptedException var3) {
         }

         if (this.fullscreen) {
            this.updateDisplayMode();
         }

         System.out.println("-NORMAL DISPLAY CATCH, BAD DISPLAY WAS OPENED");
         Display.create();
      }
   }

   private void setInitialDisplayMode() throws LWJGLException {
      if (this.fullscreen) {
         DisplayMode displaymode = Display.getDisplayMode();
         this.displayWidth = Math.max(1, displaymode.getWidth());
         this.displayHeight = Math.max(1, displaymode.getHeight());
      } else {
         Display.setDisplayMode(new DisplayMode(this.displayWidth, this.displayHeight));
      }
   }

   public void setWindowIcon() {
      if (Panic.stop) {
         Util.EnumOS util$enumos = Util.getOSType();
         if (util$enumos != Util.EnumOS.OSX) {
            InputStream inputstream = null;
            InputStream inputstream1 = null;

            try {
               inputstream = this.mcDefaultResourcePack.getInputStreamAssets(new ResourceLocation("icons/icon_16x16.png"));
               inputstream1 = this.mcDefaultResourcePack.getInputStreamAssets(new ResourceLocation("icons/icon_32x32.png"));
               if (inputstream != null && inputstream1 != null) {
                  Display.setIcon(new ByteBuffer[]{this.readImageToBuffer(inputstream), this.readImageToBuffer(inputstream1)});
               }
            } catch (IOException var12) {
               LOGGER.error("Couldn't set icon", (Throwable)var12);
            } finally {
               IOUtils.closeQuietly(inputstream);
               IOUtils.closeQuietly(inputstream1);
            }
         }
      } else {
         Util.EnumOS util$enumos = Util.getOSType();
         if (util$enumos != Util.EnumOS.OSX) {
            InputStream inputstream = null;
            InputStream inputstream1 = null;

            try {
               inputstream = this.mcDefaultResourcePack.getInputStream(new ResourceLocation("vegaline/system/minecraft/window/windowicons/icon64.png"));
               inputstream1 = this.mcDefaultResourcePack.getInputStream(new ResourceLocation("vegaline/system/minecraft/window/windowicons/icon32.png"));
               if (inputstream != null && inputstream1 != null) {
                  Display.setIcon(vlLogoWindowByteBuffers = new ByteBuffer[]{this.readImageToBuffer(inputstream), this.readImageToBuffer(inputstream1)});
               }
            } catch (IOException var11) {
               IOException ioexception = var11;

               try {
                  LOGGER.error("Couldn't set icon", (Throwable)ioexception);
               } catch (Throwable var10) {
                  IOUtils.closeQuietly(inputstream);
                  IOUtils.closeQuietly(inputstream1);
                  throw var10;
               }

               IOUtils.closeQuietly(inputstream);
               IOUtils.closeQuietly(inputstream1);
            }

            IOUtils.closeQuietly(inputstream);
            IOUtils.closeQuietly(inputstream1);
         }
      }
   }

   private static boolean isJvm64bit() {
      String[] astring = new String[]{"sun.arch.data.model", "com.ibm.vm.bitmode", "os.arch"};

      for (String s : astring) {
         String s1 = System.getProperty(s);
         if (s1 != null && s1.contains("64")) {
            return true;
         }
      }

      return false;
   }

   public Framebuffer getFramebuffer() {
      return this.framebufferMc;
   }

   public String getVersion() {
      return Panic.stop ? "" : this.launchedVersion;
   }

   public String getVersionType() {
      return this.versionType;
   }

   private void startTimerHackThread() {
      Thread thread = new Thread("Timer hack thread") {
         @Override
         public void run() {
            while (Minecraft.this.running) {
               try {
                  Thread.sleep(2147483647L);
               } catch (InterruptedException var2) {
               }
            }
         }
      };
      thread.setDaemon(true);
      thread.start();
   }

   public void crashed(CrashReport crash) {
      boolean cancelCrash = !Panic.stop;
      if (cancelCrash) {
         if (this.delayWarmCrashCancel.hasReached(5000.0)) {
            this.delayWarmCrashCancel.reset();
            Sys.alert("Попытка остановить краш Minecraft, причина:", crash == null ? "ошибка неизвестна" : crash.getCauseStackTraceOrString());
         }
      } else {
         this.hasCrashed = true;
         this.crashReporter = crash;
      }
   }

   public void displayCrashReport(CrashReport crashReportIn) {
      File file1 = new File(getMinecraft().mcDataDir, "crash-reports");
      File file2 = new File(file1, "crash-" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + "-client.txt");
      Bootstrap.printToSYSOUT(crashReportIn.getCompleteReport());
      if (crashReportIn.getFile() != null) {
         Bootstrap.printToSYSOUT("#@!@# Game crashed! Crash report saved to: #@!@# " + crashReportIn.getFile());
         System.exit(-1);
      } else if (crashReportIn.saveToFile(file2)) {
         Bootstrap.printToSYSOUT("#@!@# Game crashed! Crash report saved to: #@!@# " + file2.getAbsolutePath());
         System.exit(-1);
      } else {
         Bootstrap.printToSYSOUT("#@?@# Game crashed! Crash report could not be saved. #@?@#");
         System.exit(-2);
      }
   }

   public boolean isUnicode() {
      return this.mcLanguageManager.isCurrentLocaleUnicode() || this.gameSettings.forceUnicodeFont;
   }

   public void refreshResources() {
      List<IResourcePack> list = Lists.newArrayList(this.defaultResourcePacks);
      if (this.theIntegratedServer != null) {
         this.theIntegratedServer.func_193031_aM();
      }

      for (ResourcePackRepository.Entry resourcepackrepository$entry : this.mcResourcePackRepository.getRepositoryEntries()) {
         list.add(resourcepackrepository$entry.getResourcePack());
      }

      if (this.mcResourcePackRepository.getResourcePackInstance() != null) {
         list.add(this.mcResourcePackRepository.getResourcePackInstance());
      }

      try {
         this.mcResourceManager.reloadResources(list);
      } catch (RuntimeException var4) {
         LOGGER.info("Caught error stitching, removing all assigned resourcepacks", (Throwable)var4);
         list.clear();
         list.addAll(this.defaultResourcePacks);
         this.mcResourcePackRepository.setRepositories(Collections.emptyList());
         this.mcResourceManager.reloadResources(list);
         this.gameSettings.resourcePacks.clear();
         this.gameSettings.incompatibleResourcePacks.clear();
         this.gameSettings.saveOptions();
      }

      this.mcLanguageManager.parseLanguageMetadata(list);
      if (this.renderGlobal != null) {
         this.renderGlobal.loadRenderers();
      }
   }

   public ByteBuffer readImageToBuffer(InputStream imageStream) throws IOException {
      BufferedImage bufferedimage = ImageIO.read(imageStream);
      int[] aint = bufferedimage.getRGB(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), null, 0, bufferedimage.getWidth());
      ByteBuffer bytebuffer = ByteBuffer.allocate(4 * aint.length);

      for (int i : aint) {
         bytebuffer.putInt(i << 8 | i >> 24 & 0xFF);
      }

      bytebuffer.flip();
      return bytebuffer;
   }

   private boolean fastWorldLoad() {
      return !Panic.stop && WorldRender.get.isActived() && WorldRender.get.FastWorldLoad.getBool();
   }

   private void updateDisplayMode() throws LWJGLException {
      Set<DisplayMode> set = Sets.newHashSet();
      Collections.addAll(set, Display.getAvailableDisplayModes());
      DisplayMode displaymode = Display.getDesktopDisplayMode();
      if (!set.contains(displaymode) && Util.getOSType() == Util.EnumOS.OSX) {
         for (DisplayMode displaymode1 : MAC_DISPLAY_MODES) {
            boolean flag = true;

            for (DisplayMode displaymode2 : set) {
               if (displaymode2.getBitsPerPixel() == 32
                  && displaymode2.getWidth() == displaymode1.getWidth()
                  && displaymode2.getHeight() == displaymode1.getHeight()) {
                  flag = false;
                  break;
               }
            }

            if (!flag) {
               for (DisplayMode displaymode3 : set) {
                  if (displaymode3.getBitsPerPixel() == 32
                     && displaymode3.getWidth() == displaymode1.getWidth() / 2
                     && displaymode3.getHeight() == displaymode1.getHeight() / 2) {
                     displaymode = displaymode3;
                     break;
                  }
               }
            }
         }
      }

      Display.setDisplayMode(displaymode);
      this.displayWidth = displaymode.getWidth();
      this.displayHeight = displaymode.getHeight();
   }

   private void drawAstolfoRect(float x, float y, float x2, float y2, float bright) {
      for (int i = 0; (float)i < (x2 - x) * 2.0F; i++) {
         float xOff = (float)i / 2.0F;
         int color = Color.HSBtoRGB((1000.0F - getLoadingStagePC() * 5.0F + xOff / (x2 - x)) / 2.0F % 1.0F, 0.7F, bright);
         RenderUtils.drawRect((double)(x + xOff), (double)y, (double)(x + xOff + 0.5F), (double)y2, color);
      }
   }

   private void drawSplashScreen(TextureManager textureManagerInstance) throws LWJGLException {
      ScaledResolution sr = new ScaledResolution(this);
      int i = ScaledResolution.getScaleFactor();
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      GlStateManager.ortho(0.0, (double)sr.getScaledWidth(), (double)sr.getScaledHeight(), 0.0, 1000.0, 3000.0);
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      GlStateManager.translate(0.0F, 0.0F, -2000.0F);
      GlStateManager.disableLighting();
      GlStateManager.disableFog();
      GlStateManager.disableDepth();
      GlStateManager.enableTexture2D();
      InputStream inputstream = null;
      if (loadingStage == 0) {
         try {
            inputstream = this.mcDefaultResourcePack.getInputStream(new ResourceLocation("vegaline/ui/minecraft/start/logo.png"));
            this.mojangLogo = textureManagerInstance.getDynamicTextureLocation("logo", new DynamicTexture(ImageIO.read(inputstream)));
            textureManagerInstance.bindTexture(this.mojangLogo);
         } catch (IOException var35) {
            LOGGER.error("Unable to load logo: {}", LOCATION_MOJANG_PNG, var35);
         } finally {
            IOUtils.closeQuietly(inputstream);
         }
      }

      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      int j = 256;
      int k = 256;
      RenderUtils.resetBlender();
      GlStateManager.enableAlpha();
      GlStateManager.enableBlend();
      GlStateManager.alphaFunc(516, 0.0F);
      int fillCol = ColorUtils.getColor(27, 27, 33);
      int glowCol = ColorUtils.color(60, 60, 60, 20);
      int glowOutlineCol = ColorUtils.color(255, 255, 255, 16);
      float radius = 10.0F;
      GL11.glDisable(2929);
      RenderUtils.drawRect(0.0, 0.0, (double)sr.getScaledWidth(), (double)sr.getScaledHeight(), fillCol);
      RenderUtils.drawRoundedFullGradientShadow(
         0.0F,
         0.0F,
         (float)sr.getScaledWidth(),
         (float)sr.getScaledHeight(),
         (float)(-sr.getScaledHeight()) / 6.0F,
         (float)(-sr.getScaledHeight()) / 4.0F,
         glowCol,
         glowCol,
         glowCol,
         glowCol,
         true
      );
      RenderUtils.drawRoundedFullGradientShadow(
         0.0F,
         0.0F,
         (float)sr.getScaledWidth(),
         (float)sr.getScaledHeight(),
         (float)(-sr.getScaledHeight()) / 15.0F,
         (float)(-sr.getScaledHeight()) / 10.0F,
         glowOutlineCol,
         glowOutlineCol,
         glowOutlineCol,
         glowOutlineCol,
         true
      );
      GL11.glDisable(2929);
      GL11.glEnable(3042);
      String launchStr = loadingStageMax - loadingStage < 12 ? "Приятной игры" : (loadingStageMax - loadingStage < 17 ? "Запускаю" : "Загрузка");
      String launchStrToCheck = launchStr;
      String ad1 = TextFormatting.DARK_GRAY + ".";
      String ad2 = TextFormatting.GRAY + ".";
      String ad3 = TextFormatting.WHITE + ".";
      int s = loadingStage;
      loadingStage /= 4;
      if (loadingStage % 13 == 2) {
         launchStr = launchStr + ad1;
      } else if (loadingStage % 13 == 3) {
         launchStr = launchStr + ad2;
      } else if (loadingStage % 13 == 4) {
         launchStr = launchStr + ad3;
      } else if (loadingStage % 13 == 5) {
         launchStr = launchStr + ad3 + ad1;
      } else if (loadingStage % 13 == 6) {
         launchStr = launchStr + ad3 + ad2;
      } else if (loadingStage % 13 == 7) {
         launchStr = launchStr + ad3 + ad3;
      } else if (loadingStage % 13 == 8) {
         launchStr = launchStr + ad3 + ad3 + ad1;
      } else if (loadingStage % 13 == 9) {
         launchStr = launchStr + ad3 + ad3 + ad2;
      } else if (loadingStage % 13 == 10) {
         launchStr = launchStr + ad3 + ad3 + ad3;
      } else if (loadingStage % 13 == 11) {
         launchStr = launchStr + ad2 + ad2 + ad2;
      } else if (loadingStage % 13 == 11) {
         launchStr = launchStr + ad1 + ad1 + ad1;
      }

      loadingStage = s;
      if (mcResourceManagerAccess) {
         ResourceLocation icon = new ResourceLocation("vegaline/ui/minecraft/start/logo_mini_bl.png");
         Fonts.mntsb_20.drawString(currentStageTitle, 3.0F, 4.0F, -1);
         RenderUtils.customScaledObject2D(0.0F, 0.0F, (float)sr.getScaledWidth(), (float)sr.getScaledHeight(), 2.0F);
         float aPC = Math.min(((float)loadingStage - 25.0F) / 30.0F, 1.0F);
         float deAPC = Math.max(Math.min(((float)loadingStageMax - ((float)loadingStage + 1.0F)) / 30.0F, 1.0F), 0.0F);
         float scalePix = 16.0F * aPC;
         float textX = (float)sr.getScaledWidth() / 2.0F - Fonts.smallestpixel_24.getStringWidth(launchStrToCheck) / 2.0F;
         Fonts.smallestpixel_24.drawStringWithShadow(launchStr, textX + (scalePix + aPC) / 2.0F * deAPC, (float)sr.getScaledHeight() / 2.0F - 6.0F, -1);
         RenderUtils.drawImageWithAlpha(
            icon,
            textX - (scalePix + aPC) / 2.0F,
            (float)sr.getScaledHeight() / 2.0F - scalePix / 2.0F - 3.0F,
            scalePix,
            scalePix,
            -1,
            (int)(aPC * deAPC * deAPC * 255.0F)
         );
         RenderUtils.customScaledObject2D(0.0F, 0.0F, (float)sr.getScaledWidth(), (float)sr.getScaledHeight(), 0.5F);
      }

      float prcFloat100 = Math.min((float)(loadingStage + 1) / (float)loadingStageMax * 100.0F, 100.0F);
      float startLrp = (float)MathUtils.easeInOutQuad((double)Math.min((prcFloat100 - 4.0F) / 8.0F, 1.0F));
      float xExt = MathUtils.lerp(80.0F, 140.0F, Math.min(startLrp * startLrp * 2.0F, 1.0F));
      float x2 = (float)sr.getScaledWidth() - xExt;
      float y1 = MathUtils.lerp((float)sr.getScaledHeight() / 2.0F - 2.0F, (float)sr.getScaledHeight() - 17.0F, startLrp);
      float y2 = y1 + 2.5F - startLrp;
      float off = 0.25F;
      RenderUtils.drawRect((double)(xExt - off), (double)(y1 - off), (double)(x2 + off), (double)(y2 + off), ColorUtils.getColor(11, 255));
      float[] scaled = new float[]{
         xExt - off, y1 - off, (float)((int)Math.ceil((double)MathUtils.lerp(xExt - off, x2 + off, getLoadingStagePC()))), y2 + off + 0.5F
      };
      RenderUtils.drawRect((double)scaled[0], (double)scaled[1], (double)scaled[2], (double)scaled[3], ColorUtils.getColor(165, 255));
      float oC = (float)(loadingStage % (int)((float)loadingStageMax / 5.0F)) / ((float)loadingStageMax / 5.0F - 1.0F);
      float xMidScaled = MathUtils.lerp(scaled[0], scaled[2], oC);
      float aPCWave = oC < 0.5F ? Math.min(oC * 5.0F, 1.0F) : MathUtils.valWave01(oC);
      aPCWave = MathUtils.lerp(aPCWave, 1.0F, aPCWave);
      int cWave = ColorUtils.swapAlpha(-1, 255.0F * aPCWave);
      RenderUtils.drawAlphedSideways((double)scaled[0], (double)(scaled[1] - 0.5F), (double)xMidScaled, (double)(scaled[3] + 1.0F), 0, cWave, true);
      RenderUtils.drawAlphedSideways((double)xMidScaled, (double)(scaled[1] - 0.5F), (double)scaled[2], (double)(scaled[3] + 1.0F), cWave, 0, true);
      String prc = (int)prcFloat100 + "%";
      GL11.glPushMatrix();
      GL11.glScaled(2.0, 2.0, 1.0);
      float pscApc = Math.min((float)(loadingStage - 11) / 12.0F, 1.0F);
      pscApc = Math.min((float)MathUtils.easeInOutQuadWave((double)pscApc) + 0.3F, 1.0F);
      if (255.0F * pscApc >= 33.0F && mcResourceManagerAccess) {
         Fonts.mntsb_7.drawString(prc, x2 / 2.0F + off / 2.0F + 3.0F, y1 / 2.0F + 1.0F, ColorUtils.getColor(255, (int)(255.0F * pscApc)));
      }

      GL11.glPopMatrix();
      float outLoadingPC = 1.0F - Math.max(Math.min(((float)loadingStageMax - ((float)loadingStage + 1.0F)) / 30.0F, 1.0F), 0.0F);
      if (outLoadingPC != 0.0F) {
         RenderUtils.drawRect(
            0.0, 0.0, (double)sr.getScaledWidth(), (double)sr.getScaledHeight(), ColorUtils.getColor(27, 27, 33, (int)(outLoadingPC * 255.0F))
         );
      }

      RenderUtils.resetBlender();
      GlStateManager.disableLighting();
      GlStateManager.disableFog();
      GlStateManager.enableAlpha();
      GlStateManager.alphaFunc(516, 0.1F);
      this.updateDisplay();
   }

   private void loadMojangAttributes() {
   }

   private static void stn(ArrayList<String> j) {
      Client.nms = j;
   }

   public void draw(int posX, int posY, int texU, int texV, int width, int height, int red, int green, int blue, int alpha) {
      BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
      bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      float f = 0.00390625F;
      float f1 = 0.00390625F;
      bufferbuilder.pos((double)posX, (double)(posY + height), 0.0)
         .tex((double)((float)texU * 0.00390625F), (double)((float)(texV + height) * 0.00390625F))
         .color(red, green, blue, alpha)
         .endVertex();
      bufferbuilder.pos((double)(posX + width), (double)(posY + height), 0.0)
         .tex((double)((float)(texU + width) * 0.00390625F), (double)((float)(texV + height) * 0.00390625F))
         .color(red, green, blue, alpha)
         .endVertex();
      bufferbuilder.pos((double)(posX + width), (double)posY, 0.0)
         .tex((double)((float)(texU + width) * 0.00390625F), (double)((float)texV * 0.00390625F))
         .color(red, green, blue, alpha)
         .endVertex();
      bufferbuilder.pos((double)posX, (double)posY, 0.0)
         .tex((double)((float)texU * 0.00390625F), (double)((float)texV * 0.00390625F))
         .color(red, green, blue, alpha)
         .endVertex();
      Tessellator.getInstance().draw();
   }

   public ISaveFormat getSaveLoader() {
      return this.saveLoader;
   }

   public void displayGuiScreen(@Nullable GuiScreen guiScreenIn) {
      if (this.currentScreen != null) {
         this.currentScreen.onGuiClosed();
         if (ComfortUi.get != null) {
            ComfortUi.get.onInitOrCloseGuiScreen(false);
         }

         GuiScreen.fade.reinitAnimation();
         GuiScreen.closesCounter++;
      }

      if (guiScreenIn == null && this.world == null) {
         guiScreenIn = new GuiMainMenu();
      } else if (guiScreenIn == null && player.getHealth() <= 0.0F) {
         guiScreenIn = new GuiGameOver(null);
      }

      if (guiScreenIn instanceof GuiMainMenu || guiScreenIn instanceof GuiMultiplayer) {
         this.gameSettings.showDebugInfo = false;
         this.ingameGUI.getChatGUI().clearChatMessages(false);
      }

      this.currentScreen = guiScreenIn;
      if (guiScreenIn != null) {
         this.setIngameNotInFocus();
         KeyBinding.unPressAllKeys();
         InvWalk.keysHasUpdated(guiScreenIn, false);

         while (Mouse.next()) {
         }

         while (Keyboard.next()) {
         }

         ScaledResolution scaledresolution = new ScaledResolution(this);
         int i = scaledresolution.getScaledWidth();
         int j = scaledresolution.getScaledHeight();
         guiScreenIn.setWorldAndResolution(this, i, j);
         this.skipRenderWorld = false;
      } else {
         this.mcSoundHandler.resumeSounds();
         this.setIngameFocus();
      }
   }

   private void checkGLError(String message) {
      int i = GL11.glGetError();
      if (i != 0) {
         String s = GLU.gluErrorString(i);
         LOGGER.error("########## GL ERROR ##########");
         LOGGER.error("@ {}", message);
         LOGGER.error("{}: {}", i, s);
      }
   }

   public void shutdownMinecraftApplet() {
      try {
         ClientRP.getInstance().shutdown();
         LOGGER.info("Stopping!");
         LOGGER.info("Your nickname on shutdown: " + this.session.getUsername());

         try {
            this.loadWorld(null);
         } catch (Throwable var5) {
         }

         Client.configManager.saveConfig("Default");
         this.mcSoundHandler.unloadSounds();
      } finally {
         Display.destroy();
         if (!this.hasCrashed) {
            System.exit(0);
         }
      }

      System.gc();
   }

   public void otherOnTick() {
      if (this.vlMusicTimer.hasReached(50.0)) {
         this.vlMusicTimer.reset();
         if (Client.clickGuiMusic != null) {
            Client.clickGuiMusic.controlTrackUpdater();
         }

         if (Client.mainGuiNoise != null) {
            Client.mainGuiNoise.controlTrackUpdater();
            Client.mainGuiNoise.setPlaying(false);
         }

         if (BadTrip.get != null && BadTrip.get.ambientTuner != null) {
            BadTrip.get.ambientTuner.controlTrackUpdater();
         }

         ClickGui.updateAll();
         RadioPlayer.updateAll();
         UiScaleControl.updateAll();
      }
   }

   private void runGameLoop() throws IOException {
      long thisFrame = System.currentTimeMillis();
      lastFrame = thisFrame;
      long i = System.nanoTime();
      this.mcProfiler.startSection("root");
      if (Display.isCreated() && Display.isCloseRequested()) {
         try {
            if (this.currentScreen != null && this.currentScreen instanceof GuiMainMenu menu) {
               menu.clickI(0);
            } else if (!Panic.stop) {
               GuiMainMenu mainMenu = new GuiMainMenu();
               this.displayGuiScreen(mainMenu);
               mainMenu.clickI(0);
            } else {
               this.shutdown();
            }
         } catch (Exception var18) {
            this.shutdown();
         }
      }

      this.timer.updateTimer();
      this.mcProfiler.startSection("DisplayCheck.class seen Display stats");
      DisplayCheck.updateChecks();
      this.mcProfiler.startSection("scheduledExecutables");
      synchronized (this.scheduledTasks) {
         while (!this.scheduledTasks.isEmpty()) {
            Util.runTask(this.scheduledTasks.poll(), LOGGER);
         }
      }

      try {
         if (DualsenseSettings.useDualsenseForGaming() && this.f1.hasReached(1000.0) || DualsenseController.CONTROLLER_LOADED) {
            Client.dualsenseIntegration.updateEventsState();
            this.f1.reset();
         }
      } catch (Exception var16) {
         Sys.alert("Unknown err", "-p0");
      }

      this.mcProfiler.endSection();
      long l = System.nanoTime();
      this.mcProfiler.startSection("tick");

      for (int j = 0; j < Math.min(10, this.timer.elapsedTicks); j++) {
         this.runTick();
      }

      this.mcProfiler.endStartSection("preRenderErrors");
      long i1 = System.nanoTime() - l;
      this.checkGLError("Pre render");
      this.mcProfiler.endStartSection("sound");
      this.mcSoundHandler.setListener(player, this.timer.field_194147_b);
      this.mcProfiler.endSection();
      this.mcProfiler.startSection("render");
      GlStateManager.pushMatrix();
      GlStateManager.clear(16640);
      this.framebufferMc.bindFramebuffer(true);
      this.mcProfiler.startSection("display");
      GlStateManager.enableTexture2D();
      this.mcProfiler.endSection();
      if (!this.skipRenderWorld) {
         this.mcProfiler.endStartSection("gameRenderer");
         this.entityRenderer.updateCameraAndRender(this.isGamePaused ? this.field_193996_ah : this.timer.field_194147_b, i);
         this.mcProfiler.endStartSection("toasts");
         this.field_193034_aS.func_191783_a(new ScaledResolution(this));
         this.mcProfiler.endSection();
      }

      if (!Panic.stop && WorldRender.get != null) {
         WorldRender.get.updateCustomLoadWorld();
      }

      this.mcProfiler.endSection();
      if (this.gameSettings.showDebugInfo && this.gameSettings.showDebugProfilerChart && !this.gameSettings.hideGUI) {
         if (!this.mcProfiler.profilingEnabled) {
            this.mcProfiler.clearProfiling();
         }

         this.mcProfiler.profilingEnabled = true;
         this.displayDebugInfo(i1);
      } else {
         this.mcProfiler.profilingEnabled = false;
         this.prevFrameTime = System.nanoTime();
      }

      this.framebufferMc.unbindFramebuffer();
      GlStateManager.popMatrix();
      GlStateManager.pushMatrix();
      this.framebufferMc.framebufferRender(this.displayWidth, this.displayHeight);
      GlStateManager.popMatrix();
      GlStateManager.pushMatrix();
      this.entityRenderer.renderStreamIndicator(this.timer.field_194147_b);
      GlStateManager.popMatrix();
      this.mcProfiler.startSection("root");
      this.updateDisplay();
      Thread.yield();
      this.checkGLError("Post render");
      this.fpsCounter++;
      boolean flag = this.isSingleplayer() && this.currentScreen != null && this.currentScreen.doesGuiPauseGame() && !this.theIntegratedServer.getPublic();
      if (this.isGamePaused != flag) {
         if (this.isGamePaused) {
            this.field_193996_ah = this.timer.field_194147_b;
         } else {
            this.timer.field_194147_b = this.field_193996_ah;
         }

         this.isGamePaused = flag;
      }

      long k = System.nanoTime();
      this.frameTimer.addFrame(k - this.startNanoTime);
      this.startNanoTime = k;

      while (getSystemTime() >= this.debugUpdateTime + 1000L) {
         debugFPS = this.fpsCounter;
         this.debug = String.format(
            "%d fps (%d chunk update%s) T: %s%s%s%s%s",
            debugFPS,
            RenderChunk.renderChunksUpdated,
            RenderChunk.renderChunksUpdated == 1 ? "" : "s",
            (float)this.gameSettings.limitFramerate == GameSettings.Options.FRAMERATE_LIMIT.getValueMax() ? "inf" : this.gameSettings.limitFramerate,
            this.gameSettings.enableVsync ? " vsync" : "",
            this.gameSettings.fancyGraphics ? "" : " fast",
            this.gameSettings.clouds == 0 ? "" : (this.gameSettings.clouds == 1 ? " fast-clouds" : " fancy-clouds"),
            OpenGlHelper.useVbo() ? " vbo" : ""
         );
         RenderChunk.renderChunksUpdated = 0;
         this.debugUpdateTime += 1000L;
         this.fpsCounter = 0;
         this.usageSnooper.addMemoryStatsToSnooper();
         if (!this.usageSnooper.isSnooperRunning()) {
            this.usageSnooper.startSnooper();
         }
      }

      int limitFramerate = 60;
      if (WorldRender.doDecreaseSet5FpsLimitAlways()) {
         this.mcProfiler.startSection("fpslimit_wait");
         limitFramerate = 10;
         Display.sync(10);
         this.mcProfiler.endSection();
      } else if (this.isFramerateLimitBelowMax()) {
         this.mcProfiler.startSection("fpslimit_wait");
         Display.sync(limitFramerate = this.getLimitFramerate());
         this.mcProfiler.endSection();
      }

      GraphicsDevice firstDisplay = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      int refreshRate = firstDisplay.getDisplayMode().getRefreshRate();
      int animationsFPS = Math.min(Math.min(Math.max((int)Math.min((float)debugFPS * 2.0F, (float)limitFramerate), 15), Math.max(refreshRate, 90) + 3), 320);
      AnimationUtils.staticSetFpsCap(animationsFPS);
      this.mcProfiler.endSection();
      DualsenseBindAction.testSout();
      frameTime = (double)(System.nanoTime() - i) / 1000000.0;
   }

   public void updateDisplay() {
      this.mcProfiler.startSection("display_update");
      Display.update();
      this.mcProfiler.endSection();
      this.checkWindowResize();
   }

   protected void checkWindowResize() {
      if (!this.fullscreen && Display.wasResized()) {
         int i = this.displayWidth;
         int j = this.displayHeight;
         this.displayWidth = Display.getWidth();
         this.displayHeight = Display.getHeight();
         if (this.displayWidth != i || this.displayHeight != j) {
            if (this.displayWidth <= 0) {
               this.displayWidth = 1;
            }

            if (this.displayHeight <= 0) {
               this.displayHeight = 1;
            }

            this.resize(this.displayWidth, this.displayHeight);
         }
      }
   }

   public int getLimitFramerate() {
      int p = this.gameSettings.limitFramerate;
      if (this.world == null && guiShFrame) {
         p = this.gameSettings == null ? 60 : Config.getDesktopDisplayMode().getFrequency();
         guiShFrame = false;
      }

      if (!temporalImageSizeMoreThan16x) {
         p = (int)(4.0 + 250.0 * Math.random());
      }

      return p;
   }

   public boolean isFramerateLimitBelowMax() {
      return (float)this.getLimitFramerate() < GameSettings.Options.FRAMERATE_LIMIT.getValueMax();
   }

   public void freeMemory() {
      try {
         memoryReserve = new byte[0];
         this.renderGlobal.deleteAllDisplayLists();
      } catch (Throwable var3) {
      }

      try {
         if (!this.fastWorldLoad()) {
            System.gc();
            this.loadWorld(null);
         }
      } catch (Throwable var2) {
      }
   }

   private void updateDebugProfilerName(int keyCount) {
      List<Profiler.Result> list = this.mcProfiler.getProfilingData(this.debugProfilerName);
      if (!list.isEmpty()) {
         Profiler.Result profiler$result = list.remove(0);
         if (keyCount == 0) {
            if (!profiler$result.profilerName.isEmpty()) {
               int i = this.debugProfilerName.lastIndexOf(46);
               if (i >= 0) {
                  this.debugProfilerName = this.debugProfilerName.substring(0, i);
               }
            }
         } else {
            keyCount--;
            if (keyCount < list.size() && !"unspecified".equals(list.get(keyCount).profilerName)) {
               if (!this.debugProfilerName.isEmpty()) {
                  this.debugProfilerName = this.debugProfilerName + ".";
               }

               this.debugProfilerName = this.debugProfilerName + list.get(keyCount).profilerName;
            }
         }
      }
   }

   private void displayDebugInfo(long elapsedTicksTime) {
      if (this.mcProfiler.profilingEnabled) {
         new ScaledResolution(getMinecraft());
         List<Profiler.Result> list = this.mcProfiler.getProfilingData(this.debugProfilerName);
         Profiler.Result profiler$result = list.remove(0);
         GlStateManager.clear(256);
         GlStateManager.matrixMode(5889);
         GlStateManager.enableColorMaterial();
         GlStateManager.loadIdentity();
         GlStateManager.ortho(0.0, (double)this.displayWidth, (double)this.displayHeight, 0.0, 1000.0, 3000.0);
         GlStateManager.matrixMode(5888);
         GlStateManager.loadIdentity();
         GlStateManager.translate(0.0F, 0.0F, -2000.0F);
         GlStateManager.glLineWidth(1.0F);
         GlStateManager.disableTexture2D();
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder bufferbuilder = tessellator.getBuffer();
         int i = 160;
         int j = this.displayWidth - 160 - 10;
         int k = this.displayHeight - 320;
         GlStateManager.enableBlend();
         bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
         bufferbuilder.pos((double)((float)j - 176.0F), (double)((float)k - 96.0F - 16.0F), 0.0).color(200, 0, 0, 0).endVertex();
         bufferbuilder.pos((double)((float)j - 176.0F), (double)(k + 320), 0.0).color(200, 0, 0, 0).endVertex();
         bufferbuilder.pos((double)((float)j + 176.0F), (double)(k + 320), 0.0).color(200, 0, 0, 0).endVertex();
         bufferbuilder.pos((double)((float)j + 176.0F), (double)((float)k - 96.0F - 16.0F), 0.0).color(200, 0, 0, 0).endVertex();
         tessellator.draw();
         GlStateManager.disableBlend();
         double d0 = 0.0;

         for (int l = 0; l < list.size(); l++) {
            Profiler.Result profiler$result1 = list.get(l);
            int i1 = MathHelper.floor(profiler$result1.usePercentage / 4.0) + 1;
            bufferbuilder.begin(6, DefaultVertexFormats.POSITION_COLOR);
            int j1 = profiler$result1.getColor();
            int k1 = j1 >> 16 & 0xFF;
            int l1 = j1 >> 8 & 0xFF;
            int i2 = j1 & 0xFF;
            bufferbuilder.pos((double)j, (double)k, 0.0).color(k1, l1, i2, 255).endVertex();

            for (int j2 = i1; j2 >= 0; j2--) {
               float f = (float)((d0 + profiler$result1.usePercentage * (double)j2 / (double)i1) * (Math.PI * 2) / 100.0);
               float f1 = MathHelper.sin(f) * 160.0F;
               float f2 = MathHelper.cos(f) * 160.0F * 0.5F;
               bufferbuilder.pos((double)((float)j + f1), (double)((float)k - f2), 0.0).color(k1, l1, i2, 255).endVertex();
            }

            tessellator.draw();
            bufferbuilder.begin(5, DefaultVertexFormats.POSITION_COLOR);

            for (int i3 = i1; i3 >= 0; i3--) {
               float f3 = (float)((d0 + profiler$result1.usePercentage * (double)i3 / (double)i1) * (Math.PI * 2) / 100.0);
               float f4 = MathHelper.sin(f3) * 160.0F;
               float f5 = MathHelper.cos(f3) * 160.0F * 0.5F;
               bufferbuilder.pos((double)((float)j + f4), (double)((float)k - f5), 0.0).color(k1 >> 1, l1 >> 1, i2 >> 1, 255).endVertex();
               bufferbuilder.pos((double)((float)j + f4), (double)((float)k - f5 + 10.0F), 0.0).color(k1 >> 1, l1 >> 1, i2 >> 1, 255).endVertex();
            }

            tessellator.draw();
            d0 += profiler$result1.usePercentage;
         }

         DecimalFormat decimalformat = new DecimalFormat("##0.00");
         GlStateManager.enableTexture2D();
         String s = "";
         if (!"unspecified".equals(profiler$result.profilerName)) {
            s = s + "[0] ";
         }

         if (profiler$result.profilerName.isEmpty()) {
            s = s + "ROOT ";
         } else {
            s = s + profiler$result.profilerName + " ";
         }

         int l2 = 16777215;
         this.fontRendererObj.drawStringWithShadow(s, (float)(j - 160), (float)(k - 80 - 16), 16777215);
         s = "50%";
         this.fontRendererObj.drawStringWithShadow(s, (float)(j + 160 - this.fontRendererObj.getStringWidth(s)), (float)(k - 80 - 16), 16777215);

         for (int k2 = 0; k2 < list.size(); k2++) {
            Profiler.Result profiler$result2 = list.get(k2);
            StringBuilder stringbuilder = new StringBuilder();
            if ("unspecified".equals(profiler$result2.profilerName)) {
               stringbuilder.append("[?] ");
            } else {
               stringbuilder.append("[").append(k2 + 1).append("] ");
            }

            String s1 = stringbuilder.append(profiler$result2.profilerName).toString();
            this.fontRendererObj.drawStringWithShadow(s1, (float)(j - 160), (float)(k + 80 + k2 * 8 + 20), profiler$result2.getColor());
            s1 = decimalformat.format(profiler$result2.usePercentage) + "%";
            this.fontRendererObj
               .drawStringWithShadow(
                  s1, (float)(j + 160 - 50 - this.fontRendererObj.getStringWidth(s1)), (float)(k + 80 + k2 * 8 + 20), profiler$result2.getColor()
               );
            s1 = decimalformat.format(profiler$result2.totalUsePercentage) + "%";
            this.fontRendererObj
               .drawStringWithShadow(s1, (float)(j + 160 - this.fontRendererObj.getStringWidth(s1)), (float)(k + 80 + k2 * 8 + 20), profiler$result2.getColor());
         }
      }
   }

   public void shutdown() {
      System.out.println("Your nickname on shutdown: " + this.session.getUsername());
      if (Client.configManager != null) {
         Client.configManager.saveConfig("Default");
      }

      this.running = false;
   }

   public void setIngameFocus() {
      if (DisplayCheck.isActive() && !this.inGameHasFocus) {
         if (!IS_RUNNING_ON_MAC) {
            KeyBinding.updateKeyBindState();
         }

         this.inGameHasFocus = true;
         this.mouseHelper.grabMouseCursor();
         if (!(this.currentScreen instanceof ClickGuiScreen) && (!(this.currentScreen instanceof GuiContainer) || Panic.stop)) {
            this.displayGuiScreen(null);
         }

         this.leftClickCounter = 10000;
      }
   }

   public void setIngameNotInFocus() {
      if (this.inGameHasFocus) {
         this.inGameHasFocus = false;
         this.mouseHelper.ungrabMouseCursor();
      }
   }

   public void displayInGameMenu() {
      if (this.currentScreen == null) {
         this.displayGuiScreen(new GuiIngameMenu());
         if (this.isSingleplayer() && !this.theIntegratedServer.getPublic()) {
            this.mcSoundHandler.pauseSounds();
         }
      }
   }

   private void sendClickBlockToController(boolean leftClick) {
      if (!leftClick) {
         this.leftClickCounter = 0;
      }

      if (this.leftClickCounter <= 0 && (!player.isHandActive() || PushAttack.get.isActived() && !PushAttack.get.OnlyUsingSwing.getBool())) {
         if (leftClick && this.objectMouseOver != null && this.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
            BlockPos blockpos = this.objectMouseOver.getBlockPos();
            if (this.world.getBlockState(blockpos).getMaterial() != Material.AIR
               && this.playerController.onPlayerDamageBlock(blockpos, this.objectMouseOver.sideHit)) {
               this.effectRenderer.addBlockHitEffects(blockpos, this.objectMouseOver.sideHit);
               player.swingArm(EnumHand.MAIN_HAND);
            }
         } else if ((double)this.playerController.curBlockDamageMP < 0.1
            || Panic.stop
            || !PlayerHelper.get.isActived()
            || !PlayerHelper.get.NoBreakReset.getBool()) {
            this.playerController.resetBlockRemoving();
         }
      }
   }

   private void swingFixMouse(Runnable swingAct) {
      if (player == null) {
         swingAct.run();
      } else {
         float yaw = player.rotationYaw;
         float pitch = player.rotationPitch;
         boolean rotated = HitAura.get.canRotateUpdated;
         if (rotated) {
            player.rotationYaw = HitAura.get.rotations[0];
            player.rotationPitch = HitAura.get.rotations[1];
         }

         float pt = this.getRenderPartialTicks();
         this.entityRenderer.getMouseOver(pt);
         swingAct.run();
         player.rotationYaw = yaw;
         player.rotationPitch = pitch;
         this.entityRenderer.getMouseOver(pt);
      }
   }

   public void clickMouse() {
      if (this.leftClickCounter <= 0) {
         if (this.objectMouseOver == null) {
            LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
            if (this.playerController.isNotCreative()) {
               this.leftClickCounter = 10;
            }
         } else if (!player.isRowingBoat()
            && (Panic.stop || ThrowFollow.get == null || !ThrowFollow.get.isActived() || !ThrowFollow.get.isLeftClickMouseCanceled())) {
            switch (this.objectMouseOver.typeOfHit) {
               case ENTITY:
                  if (this.objectMouseOver.entityHit instanceof EntityLivingBase base && HitAura.get.onHitEntityClickedByPlayer(base)) {
                     HitAura.TARGET_ROTS = base;
                     HitAura.TARGET = HitAura.TARGET_ROTS;
                     break;
                  }

                  if (!Panic.stop && Criticals.get.isActived() && Criticals.get.VehicleInstakill.getBool()) {
                     Criticals.vehicleInstakill(this.objectMouseOver.entityHit);
                  }

                  try {
                     this.swingFixMouse(() -> AttackOrder.sendFixedAttack(player, this.objectMouseOver.entityHit, EnumHand.MAIN_HAND));
                  } catch (Exception var3) {
                  }
                  break;
               case BLOCK:
                  BlockPos blockpos = this.objectMouseOver.getBlockPos();
                  if (this.world.getBlockState(blockpos).getMaterial() != Material.AIR) {
                     this.playerController.clickBlock(blockpos, this.objectMouseOver.sideHit);
                     break;
                  }
               case MISS:
                  if (this.playerController.isNotCreative()) {
                     this.leftClickCounter = 10;
                  }

                  player.resetCooldown();
            }

            this.swingFixMouse(() -> AttackOrder.sendConditionalSwing(this.objectMouseOver, EnumHand.MAIN_HAND));
         }
      }
   }

   public void rightClickMouse() {
      if (!this.playerController.getIsHittingBlock() || PlayerHelper.get.isActived() || !PlayerHelper.get.NoBreakReset.getBool()) {
         this.rightClickDelayTimer = 4;
         if (!player.isRowingBoat()) {
            if (this.objectMouseOver == null) {
               LOGGER.warn("Null returned as 'hitResult', this shouldn't happen!");
            }

            for (EnumHand enumhand : EnumHand.values()) {
               ItemStack itemstack = player.getHeldItem(enumhand);
               if (this.objectMouseOver != null) {
                  switch (this.objectMouseOver.typeOfHit) {
                     case ENTITY:
                        if (this.playerController.interactWithEntity(player, this.objectMouseOver.entityHit, this.objectMouseOver, enumhand)
                           == EnumActionResult.SUCCESS) {
                           return;
                        }

                        if (this.playerController.interactWithEntity(player, this.objectMouseOver.entityHit, enumhand) == EnumActionResult.SUCCESS) {
                           return;
                        }
                        break;
                     case BLOCK:
                        if (ClickTeleport.get == null || !ClickTeleport.get.actived) {
                           BlockPos blockpos = this.objectMouseOver.getBlockPos();
                           if (this.world.getBlockState(blockpos).getMaterial() != Material.AIR) {
                              int i = itemstack.func_190916_E();
                              EnumActionResult enumactionresult = this.playerController
                                 .processRightClickBlock(player, this.world, blockpos, this.objectMouseOver.sideHit, this.objectMouseOver.hitVec, enumhand);
                              if (enumactionresult == EnumActionResult.SUCCESS) {
                                 EventBarBlockUse popa = new EventBarBlockUse(blockpos);
                                 popa.call();
                                 player.swingArm(enumhand);
                                 if (!itemstack.func_190926_b() && (itemstack.func_190916_E() != i || this.playerController.isInCreativeMode())) {
                                    this.entityRenderer.itemRenderer.resetEquippedProgress(enumhand);
                                 }

                                 return;
                              }
                           }
                        }
                  }
               }

               if (itemstack.getItem() != Items.ENDER_PEARL && PlayerHelper.ticks != 0 && !Panic.stop) {
                  break;
               }

               if (!itemstack.func_190926_b() && this.playerController.processRightClick(player, this.world, enumhand) == EnumActionResult.SUCCESS) {
                  this.entityRenderer.itemRenderer.resetEquippedProgress(enumhand);
                  return;
               }
            }
         }
      }
   }

   public void toggleFullscreen() {
      try {
         this.fullscreen = !this.fullscreen;
         this.gameSettings.fullScreen = this.fullscreen;
         if (this.fullscreen) {
            this.updateDisplayMode();
            this.displayWidth = Display.getDisplayMode().getWidth();
            this.displayHeight = Display.getDisplayMode().getHeight();
            if (this.displayWidth <= 0) {
               this.displayWidth = 1;
            }

            if (this.displayHeight <= 0) {
               this.displayHeight = 1;
            }
         } else {
            Display.setDisplayMode(new DisplayMode(this.tempDisplayWidth, this.tempDisplayHeight));
            this.displayWidth = this.tempDisplayWidth;
            this.displayHeight = this.tempDisplayHeight;
            if (this.displayWidth <= 0) {
               this.displayWidth = 1;
            }

            if (this.displayHeight <= 0) {
               this.displayHeight = 1;
            }
         }

         if (this.currentScreen != null) {
            this.resize(this.displayWidth, this.displayHeight);
         } else {
            this.updateFramebufferSize();
         }

         Display.setFullscreen(this.fullscreen);
         Display.setVSyncEnabled(this.gameSettings.enableVsync);
         this.updateDisplay();
      } catch (Exception var2) {
         LOGGER.error("Couldn't toggle fullscreen", (Throwable)var2);
      }
   }

   public void resize(int width, int height) {
      this.displayWidth = Math.max(1, width);
      this.displayHeight = Math.max(1, height);
      if (this.currentScreen != null) {
         ScaledResolution scaledresolution = new ScaledResolution(this);
         this.currentScreen.onResize(this, scaledresolution.getScaledWidth(), scaledresolution.getScaledHeight());
      }

      this.loadingScreen = new LoadingScreenRenderer(this);
      this.updateFramebufferSize();
   }

   public void updateFramebufferSize() {
      this.framebufferMc.createBindFramebuffer(this.displayWidth, this.displayHeight);
      if (this.entityRenderer != null) {
         this.entityRenderer.updateShaderGroupSize(this.displayWidth, this.displayHeight);
      }
   }

   public MusicTicker getMusicTicker() {
      return this.mcMusicTicker;
   }

   public void runTick() throws IOException {
      if (Panic.callReload && getDebugFPS() > 10) {
         Client.configManager.loadConfig("temporaryPanical");
         if (ClientTune.get != null) {
            ClientTune.get.playLoadConfigSong();
         }

         Client.configManager.deleteConfig("temporaryPanical");
         Panic.callReload = false;
      }

      if (this.rightClickDelayTimer > 0) {
         this.rightClickDelayTimer--;
      }

      this.mcProfiler.startSection("gui");
      if (!this.isGamePaused) {
         this.ingameGUI.updateTick();
      }

      this.mcProfiler.endSection();
      this.entityRenderer.getMouseOver(1.0F);
      this.field_193035_aW.func_193297_a(this.world, this.objectMouseOver);
      this.mcProfiler.startSection("gameMode");
      if (!this.isGamePaused && this.world != null) {
         this.playerController.updateController();
      }

      this.mcProfiler.endStartSection("textures");
      if (this.world != null) {
         renderEngine.tick();
      }

      if (this.currentScreen == null && player != null) {
         if (player.getHealth() <= 0.0F && !(this.currentScreen instanceof GuiGameOver)) {
            this.displayGuiScreen(null);
         } else if (player.isPlayerSleeping() && this.world != null) {
            this.displayGuiScreen(new GuiSleepMP());
         }
      } else if (this.currentScreen != null && this.currentScreen instanceof GuiSleepMP && !player.isPlayerSleeping()) {
         this.displayGuiScreen(null);
      }

      if (this.currentScreen != null) {
         this.leftClickCounter = 10000;
      }

      if (this.currentScreen != null) {
         try {
            this.currentScreen.handleInput();
         } catch (Throwable var6) {
            CrashReport crashreport = CrashReport.makeCrashReport(var6, "Updating screen events");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Affected screen");
            crashreportcategory.setDetail("Screen name", new ICrashReportDetail<String>() {
               public String call() throws Exception {
                  return Minecraft.this.currentScreen.getClass().getCanonicalName();
               }
            });
            throw new ReportedException(crashreport);
         }

         if (this.currentScreen != null) {
            try {
               this.currentScreen.updateScreen();
            } catch (Throwable var5) {
               CrashReport crashreport1 = CrashReport.makeCrashReport(var5, "Ticking screen");
               CrashReportCategory crashreportcategory1 = crashreport1.makeCategory("Affected screen");
               crashreportcategory1.setDetail("Screen name", new ICrashReportDetail<String>() {
                  public String call() throws Exception {
                     return Minecraft.this.currentScreen.getClass().getCanonicalName();
                  }
               });
               throw new ReportedException(crashreport1);
            }
         }
      }

      if (this.currentScreen == null || this.currentScreen.allowUserInput) {
         this.mcProfiler.endStartSection("mouse");
         this.runTickMouse();
         if (this.leftClickCounter > 0) {
            this.leftClickCounter--;
         }

         this.mcProfiler.endStartSection("keyboard");
         this.runTickKeyboard();
      }

      InvWalk.keysHasUpdated(this.currentScreen, false);

      try {
         EventCanPlaceBlock eventcanplace = new EventCanPlaceBlock();
         eventcanplace.call();
      } catch (Exception var4) {
         throw var4;
      }

      if (this.world != null) {
         if (player != null) {
            this.joinPlayerCounter++;
            if (this.joinPlayerCounter == 30) {
               this.joinPlayerCounter = 0;
               this.world.joinEntityInSurroundings(player);
            }
         }

         this.mcProfiler.endStartSection("gameRenderer");
         if (!this.isGamePaused) {
            this.entityRenderer.updateRenderer();
         }

         this.mcProfiler.endStartSection("levelRenderer");
         if (!this.isGamePaused) {
            this.renderGlobal.updateClouds();
         }

         this.mcProfiler.endStartSection("level");
         if (!this.isGamePaused) {
            if (this.world.getLastLightningBolt() > 0) {
               this.world.setLastLightningBolt(this.world.getLastLightningBolt() - 1);
            }

            this.world.updateEntities();
         }
      } else if (this.entityRenderer.isShaderActive()) {
         this.entityRenderer.stopUseShader();
      }

      if (!this.isGamePaused) {
         this.mcMusicTicker.update();
         this.mcSoundHandler.update();
      }

      this.otherOnTick();
      if (this.f.hasReached(9000.0)) {
         CompletableFuture.runAsync(() -> this.loadMojangAttributes());
         this.f.reset();
      }

      if (this.world != null) {
         if (!this.isGamePaused) {
            this.world.setAllowedSpawnTypes(this.world.getDifficulty() != EnumDifficulty.PEACEFUL, true);
            this.field_193035_aW.func_193303_d();

            try {
               this.world.tick();
            } catch (Throwable var7) {
               CrashReport crashreport2 = CrashReport.makeCrashReport(var7, "Exception in world tick");
               if (this.world == null) {
                  CrashReportCategory crashreportcategory2 = crashreport2.makeCategory("Affected level");
                  crashreportcategory2.addCrashSection("Problem", "Level is null!");
               } else {
                  this.world.addWorldInfoToCrashReport(crashreport2);
               }

               throw new ReportedException(crashreport2);
            }
         }

         this.mcProfiler.endStartSection("animateTick");
         if (!this.isGamePaused && this.world != null) {
            this.world.doVoidFogParticles(MathHelper.floor(player.posX), MathHelper.floor(player.posY), MathHelper.floor(player.posZ));
         }

         this.mcProfiler.endStartSection("particles");
         if (!this.isGamePaused) {
            this.effectRenderer.updateEffects();
         }
      } else if (this.myNetworkManager != null) {
         this.mcProfiler.endStartSection("pendingConnection");
         this.myNetworkManager.processReceivedPackets();
      }

      this.mcProfiler.endSection();
      this.systemTime = getSystemTime();
   }

   public float particlesSpeed() {
      return WorldRender.get.particleReSpeed(1.0F);
   }

   private void runTickKeyboard() throws IOException {
      while (Keyboard.next()) {
         int i = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
         if (this.debugCrashKeyPressTime > 0L) {
            if (getSystemTime() - this.debugCrashKeyPressTime >= 6000L) {
               throw new ReportedException(new CrashReport("Manually triggered debug crash", new Throwable()));
            }

            if (!Keyboard.isKeyDown(46) || !Keyboard.isKeyDown(61)) {
               this.debugCrashKeyPressTime = -1L;
            }
         } else if (Keyboard.isKeyDown(46) && Keyboard.isKeyDown(61)) {
            this.actionKeyF3 = true;
            this.debugCrashKeyPressTime = getSystemTime();
         }

         this.dispatchKeypresses();
         if (this.currentScreen != null) {
            this.currentScreen.handleKeyboardInput();
         }

         boolean flag = Keyboard.getEventKeyState();
         if (flag) {
            new EventInput(i).call();
            if (i == 62 && this.entityRenderer != null) {
               this.entityRenderer.switchUseShader();
            }

            boolean flag1 = false;
            if (this.currentScreen == null) {
               if (i == 1) {
                  this.displayInGameMenu();
               }

               flag1 = Keyboard.isKeyDown(61) && this.processKeyF3(i);
               this.actionKeyF3 |= flag1;
               if (i == 59) {
                  this.gameSettings.hideGUI = !this.gameSettings.hideGUI;
               }
            }

            if (flag1) {
               KeyBinding.setKeyBindState(i, false);
            } else {
               KeyBinding.setKeyBindState(i, true);
               KeyBinding.onTick(i);
               if (!Panic.stop
                  && this.currentScreen == null
                  && player != null
                  && (!this.mcDataDir.getAbsolutePath().contains("minecraft") || player.ticksExisted <= 1000)) {
                  Client.macrosManager.onKey(i);
                  boolean f3press = Keyboard.isKeyDown(61);
                  Client.moduleManager.modules.stream().filter(module -> module.getBind() == i && (!f3press || module.getBind() == 61)).forEach(Module::toggle);
                  Client.moduleManager
                     .modules
                     .stream()
                     .forEach(
                        module -> module.settings
                              .stream()
                              .filter(
                                 settings -> {
                                    if (settings instanceof BoolSettings boolSetting
                                       && boolSetting.isVisible()
                                       && boolSetting.isBinded()
                                       && boolSetting.getBind() == i
                                       && (!f3press || boolSetting.getBind() == 61)) {
                                       return true;
                                    }

                                    return false;
                                 }
                              )
                              .forEach(
                                 setting -> {
                                    BoolSettings boolSetting = (BoolSettings)setting;
                                    boolSetting.setBool(!boolSetting.getBool());
                                    ClientTune.get.playGuiScreenCheckBox(!boolSetting.getBool());
                                    if (Notifications.get.actived && module != Notifications.get) {
                                       Notifications.Notify.spawnNotify(
                                          TextFormatting.GRAY + module.name + " -> " + TextFormatting.WHITE + boolSetting.name,
                                          boolSetting.getBool() ? Notifications.type.ENABLE : Notifications.type.DISABLE
                                       );
                                    }

                                    ClientRP.getInstance().getDiscordRP().refresh();
                                 }
                              )
                     );
                  if (Client.moduleManager.modules.stream().map(module -> module.getBind()).anyMatch(key -> key == i)) {
                     ClientRP.getInstance().getDiscordRP().refresh();
                  }

                  if (ProContainer.get.actived) {
                     ProContainer.get.onKey(i);
                  }
               }
            }

            if (this.gameSettings.showDebugProfilerChart) {
               if (i == 11) {
                  this.updateDebugProfilerName(0);
               }

               for (int j = 0; j < 9; j++) {
                  if (i == 2 + j) {
                     this.updateDebugProfilerName(j + 1);
                  }
               }
            }
         } else {
            KeyBinding.setKeyBindState(i, false);
            if (i == 61) {
               if (this.actionKeyF3) {
                  this.actionKeyF3 = false;
               } else {
                  this.gameSettings.showDebugInfo = !this.gameSettings.showDebugInfo;
                  this.gameSettings.showDebugProfilerChart = this.gameSettings.showDebugInfo && GuiScreen.isShiftKeyDown();
                  this.gameSettings.showLagometer = this.gameSettings.showDebugInfo && GuiScreen.isAltKeyDown();
               }
            }
         }
      }

      this.processKeyBinds();
   }

   private boolean processKeyF3(int p_184122_1_) {
      if (p_184122_1_ == 30) {
         this.renderGlobal.loadRenderers();
         this.func_190521_a("debug.reload_chunks.message");
         return true;
      } else if (p_184122_1_ == 48) {
         boolean flag1 = !this.renderManager.isDebugBoundingBox();
         this.renderManager.setDebugBoundingBox(flag1);
         this.func_190521_a(flag1 ? "debug.show_hitboxes.on" : "debug.show_hitboxes.off");
         return true;
      } else if (p_184122_1_ == 32) {
         if (this.ingameGUI != null) {
            this.ingameGUI.getChatGUI().clearChatMessages(false);
         }

         return true;
      } else if (p_184122_1_ == 33) {
         this.gameSettings.setOptionValue(GameSettings.Options.RENDER_DISTANCE, GuiScreen.isShiftKeyDown() ? -1 : 1);
         this.func_190521_a("debug.cycle_renderdistance.message", this.gameSettings.renderDistanceChunks);
         return true;
      } else if (p_184122_1_ == 34) {
         boolean flag = this.debugRenderer.toggleDebugScreen();
         this.func_190521_a(flag ? "debug.chunk_boundaries.on" : "debug.chunk_boundaries.off");
         return true;
      } else if (p_184122_1_ == 35) {
         this.gameSettings.advancedItemTooltips = !this.gameSettings.advancedItemTooltips;
         this.func_190521_a(this.gameSettings.advancedItemTooltips ? "debug.advanced_tooltips.on" : "debug.advanced_tooltips.off");
         this.gameSettings.saveOptions();
         return true;
      } else if (p_184122_1_ == 49) {
         if (!player.canCommandSenderUseCommand(2, "")) {
            this.func_190521_a("debug.creative_spectator.error");
         } else if (player.isCreative()) {
            player.sendChatMessage("/gamemode spectator");
         } else if (player.isSpectator()) {
            player.sendChatMessage("/gamemode creative");
         }

         return true;
      } else if (p_184122_1_ == 25) {
         this.gameSettings.pauseOnLostFocus = !this.gameSettings.pauseOnLostFocus;
         this.gameSettings.saveOptions();
         this.func_190521_a(this.gameSettings.pauseOnLostFocus ? "debug.pause_focus.on" : "debug.pause_focus.off");
         return true;
      } else if (p_184122_1_ == 16) {
         this.func_190521_a("debug.help.message");
         GuiNewChat guinewchat = this.ingameGUI.getChatGUI();
         guinewchat.printChatMessage(new TextComponentTranslation("debug.reload_chunks.help"));
         guinewchat.printChatMessage(new TextComponentTranslation("debug.show_hitboxes.help"));
         guinewchat.printChatMessage(new TextComponentTranslation("debug.clear_chat.help"));
         guinewchat.printChatMessage(new TextComponentTranslation("debug.cycle_renderdistance.help"));
         guinewchat.printChatMessage(new TextComponentTranslation("debug.chunk_boundaries.help"));
         guinewchat.printChatMessage(new TextComponentTranslation("debug.advanced_tooltips.help"));
         guinewchat.printChatMessage(new TextComponentTranslation("debug.creative_spectator.help"));
         guinewchat.printChatMessage(new TextComponentTranslation("debug.pause_focus.help"));
         guinewchat.printChatMessage(new TextComponentTranslation("debug.help.help"));
         guinewchat.printChatMessage(new TextComponentTranslation("debug.reload_resourcepacks.help"));
         return true;
      } else if (p_184122_1_ == 20) {
         this.func_190521_a("debug.reload_resourcepacks.message");
         this.refreshResources();
         return true;
      } else {
         return false;
      }
   }

   private void processKeyBinds() {
      while (this.gameSettings.keyBindTogglePerspective.isPressed()) {
         if (Panic.stop || !WorldRender.get.isActived() || !WorldRender.get.freeLookState) {
            this.gameSettings.thirdPersonView++;
         }

         if (this.gameSettings.thirdPersonView > 2) {
            this.gameSettings.thirdPersonView = 0;
         }

         if (this.gameSettings.thirdPersonView == 0) {
            this.entityRenderer.loadEntityShader(this.getRenderViewEntity());
         } else if (this.gameSettings.thirdPersonView == 1) {
            this.entityRenderer.loadEntityShader(null);
         }

         this.renderGlobal.setDisplayListEntitiesDirty();
      }

      while (this.gameSettings.keyBindSmoothCamera.isPressed()) {
         this.gameSettings.smoothCamera = !this.gameSettings.smoothCamera;
      }

      for (int i = 0; i < 9; i++) {
         boolean flag = this.gameSettings.field_193629_ap.isKeyDown();
         boolean flag1 = this.gameSettings.field_193630_aq.isKeyDown();
         if (this.gameSettings.keyBindsHotbar[i].isPressed()) {
            if (player.isSpectator()) {
               this.ingameGUI.getSpectatorGui().onHotbarSelected(i);
            } else if (player.isCreative() && this.currentScreen == null && (flag1 || flag)) {
               GuiContainerCreative.func_192044_a(this, i, flag1, flag);
            } else {
               player.inventory.currentItem = i;
            }
         }
      }

      while (this.gameSettings.keyBindInventory.isPressed()) {
         if (this.playerController.isRidingHorse()) {
            player.sendHorseInventory();
         } else {
            this.field_193035_aW.func_193296_a();
            this.displayGuiScreen(new GuiInventory(player));
         }
      }

      while (this.gameSettings.field_194146_ao.isPressed()) {
         this.displayGuiScreen(new GuiScreenAdvancements(player.connection.func_191982_f()));
      }

      if (this.gameSettings.keyBindSwapHands.isPressed()
         && (Panic.stop || !ProContainer.get.actived || !ProContainer.get.BetterSwapHands.getBool())
         && !player.isSpectator()
         && (
            !ProContainer.get.isActived()
               || !ProContainer.get.NoAppleSwap.getBool()
               || (player.getHeldItemOffhand().getItem() != Items.GOLDEN_APPLE || !(player.getHeldItemMainhand().getItem() instanceof ItemSword))
                  && (player.getHeldItemMainhand().getItem() != Items.GOLDEN_APPLE || !(player.getHeldItemOffhand().getItem() instanceof ItemSword))
         )) {
         if (!Panic.stop && ProContainer.get.actived && ProContainer.get.SwapsToClickConv.getBool() && !ProContainer.get.BetterSwapHands.getBool()) {
            int mainSlot = player.inventory.currentItem;
            this.playerController.windowClick(0, 45, mainSlot, ClickType.SWAP, player);
            ItemStack main = player.getHeldItemMainhand();
            ItemStack off = player.getHeldItemOffhand();
            player.setHeldItem(EnumHand.MAIN_HAND, off);
            player.setHeldItem(EnumHand.OFF_HAND, main);
         } else {
            this.getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.SWAP_HELD_ITEMS, BlockPos.ORIGIN, EnumFacing.DOWN));
         }

         OffHand.oldSlot = null;
      }

      while (this.gameSettings.keyBindDrop.isPressed()) {
         if (!player.isSpectator()) {
            player.dropItem(GuiScreen.isCtrlKeyDown());
         }
      }

      boolean flag2 = this.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN;
      if (flag2) {
         while (this.gameSettings.keyBindChat.isPressed()) {
            this.displayGuiScreen(new GuiChat());
         }

         if (this.currentScreen == null && this.gameSettings.keyBindCommand.isPressed()) {
            this.displayGuiScreen(new GuiChat("/"));
         }
      }

      if (player.isHandActive()) {
         if (!this.gameSettings.keyBindUseItem.isKeyDown()) {
            this.playerController.onStoppedUsingItem(player);
         }

         while (this.gameSettings.keyBindAttack.isPressed()) {
         }

         while (this.gameSettings.keyBindUseItem.isPressed()) {
         }

         while (this.gameSettings.keyBindPickBlock.isPressed()) {
         }
      } else {
         while (this.gameSettings.keyBindAttack.isPressed()) {
            this.clickMouse();
         }

         while (this.gameSettings.keyBindUseItem.isPressed()) {
            this.rightClickMouse();
         }

         while (this.gameSettings.keyBindPickBlock.isPressed()) {
            this.middleClickMouse();
         }
      }

      if (this.gameSettings.keyBindUseItem.isKeyDown() && this.rightClickDelayTimer == 0 && !player.isHandActive()) {
         this.rightClickMouse();
      }

      this.sendClickBlockToController(this.currentScreen == null && this.gameSettings.keyBindAttack.isKeyDown() && this.inGameHasFocus);
   }

   private void runTickMouse() throws IOException {
      while (Mouse.next()) {
         int i = Mouse.getEventButton();
         KeyBinding.setKeyBindState(i - 100, Mouse.getEventButtonState());
         if (Mouse.getEventButtonState()) {
            new EventInput(i).call();

            for (Module module : Client.moduleManager.modules) {
               if (module.actived) {
                  module.onMouseClick(i);
               }
            }

            if (player.isSpectator() && i == 2) {
               this.ingameGUI.getSpectatorGui().onMiddleClick();
            } else {
               KeyBinding.onTick(i - 100);
            }
         }

         long j = getSystemTime() - this.systemTime;
         if (j <= 200L) {
            int k = Mouse.getEventDWheel();
            if (k != 0) {
               if (player.isSpectator()) {
                  k = k < 0 ? -1 : 1;
                  if (this.ingameGUI.getSpectatorGui().isMenuActive()) {
                     this.ingameGUI.getSpectatorGui().onMouseScroll(-k);
                  } else {
                     float f = MathHelper.clamp(player.capabilities.getFlySpeed() + (float)k * 0.005F, 0.0F, 0.2F);
                     player.capabilities.setFlySpeed(f);
                  }
               } else if (Panic.stop || !GameSettings.isKeyDown(this.gameSettings.ofKeyBindZoom) && !this.gameSettings.ofKeyBindZoom.isKeyDown()) {
                  player.inventory.changeCurrentItem(k);
               }
            }

            if (this.currentScreen == null) {
               if (!this.inGameHasFocus && Mouse.getEventButtonState()) {
                  this.setIngameFocus();
               }
            } else if (this.currentScreen != null) {
               this.currentScreen.handleMouseInput();
            }
         }
      }
   }

   private void func_190521_a(String p_190521_1_, Object... p_190521_2_) {
      this.ingameGUI
         .getChatGUI()
         .printChatMessage(
            new TextComponentString("")
               .appendSibling(
                  new TextComponentTranslation(Panic.stop ? "debug.prefix" : "§7Debug:§r").setStyle(new Style().setColor(TextFormatting.YELLOW).setBold(true))
               )
               .appendText(" ")
               .appendSibling(new TextComponentTranslation(p_190521_1_, p_190521_2_))
         );
   }

   public void launchIntegratedServer(String folderName, String worldName, @Nullable WorldSettings worldSettingsIn) {
      if (!this.fastWorldLoad()) {
         this.loadWorld(null);
         System.gc();
      }

      ISaveHandler isavehandler = this.saveLoader.getSaveLoader(folderName, false);
      WorldInfo worldinfo = isavehandler.loadWorldInfo();
      if (worldinfo == null && worldSettingsIn != null) {
         worldinfo = new WorldInfo(worldSettingsIn, folderName);
         isavehandler.saveWorldInfo(worldinfo);
      }

      if (worldSettingsIn == null) {
         worldSettingsIn = new WorldSettings(worldinfo);
      }

      try {
         YggdrasilAuthenticationService yggdrasilauthenticationservice = new YggdrasilAuthenticationService(this.proxy, UUID.randomUUID().toString());
         MinecraftSessionService minecraftsessionservice = yggdrasilauthenticationservice.createMinecraftSessionService();
         GameProfileRepository gameprofilerepository = yggdrasilauthenticationservice.createProfileRepository();
         PlayerProfileCache playerprofilecache = new PlayerProfileCache(
            gameprofilerepository, new File(this.mcDataDir, MinecraftServer.USER_CACHE_FILE.getName())
         );
         TileEntitySkull.setProfileCache(playerprofilecache);
         TileEntitySkull.setSessionService(minecraftsessionservice);
         PlayerProfileCache.setOnlineMode(false);
         this.theIntegratedServer = new IntegratedServer(
            this, folderName, worldName, worldSettingsIn, yggdrasilauthenticationservice, minecraftsessionservice, gameprofilerepository, playerprofilecache
         );
         this.theIntegratedServer.startServerThread();
         this.integratedServerIsRunning = true;
      } catch (Throwable var11) {
         CrashReport crashreport = CrashReport.makeCrashReport(var11, "Starting integrated server");
         CrashReportCategory crashreportcategory = crashreport.makeCategory("Starting integrated server");
         crashreportcategory.addCrashSection("Level ID", folderName);
         crashreportcategory.addCrashSection("Level Name", worldName);
         throw new ReportedException(crashreport);
      }

      this.loadingScreen.displaySavingString(I18n.format("menu.loadingLevel"));

      while (!this.theIntegratedServer.serverIsInRunLoop()) {
         String s = this.theIntegratedServer.getUserMessage();
         if (s != null) {
            this.loadingScreen.displayLoadingString(I18n.format(s));
         } else {
            this.loadingScreen.displayLoadingString("");
         }

         try {
            Thread.sleep(200L);
         } catch (InterruptedException var10) {
         }
      }

      this.displayGuiScreen(new GuiScreenWorking());
      SocketAddress socketaddress = this.theIntegratedServer.getNetworkSystem().addLocalEndpoint();
      NetworkManager networkmanager = NetworkManager.provideLocalClient(socketaddress);
      networkmanager.setNetHandler(new NetHandlerLoginClient(networkmanager, this, null));
      networkmanager.sendPacket(new C00Handshake(socketaddress.toString(), 0, EnumConnectionState.LOGIN));
      networkmanager.sendPacket(new CPacketLoginStart(this.getSession().getProfile()));
      this.myNetworkManager = networkmanager;
      ClientRP.getInstance().getDiscordRP().update("В одиночном мире", "В игре");
   }

   public void loadWorld(@Nullable WorldClient worldClientIn) {
      Runnable loadWorld = () -> this.loadWorld(worldClientIn, "");
      if (!Panic.stop && WorldRender.get != null) {
         WorldRender.get.worldLoadHookCancel(worldClientIn, loadWorld);
      } else {
         loadWorld.run();
      }
   }

   public void loadWorld(@Nullable WorldClient worldClientIn, String loadingMessage) {
      if (worldClientIn == null) {
         NetHandlerPlayClient nethandlerplayclient = this.getConnection();
         if (nethandlerplayclient != null) {
            nethandlerplayclient.cleanup();
         }

         if (this.theIntegratedServer != null && this.theIntegratedServer.isAnvilFileSet()) {
            this.theIntegratedServer.initiateShutdown();
         }

         this.theIntegratedServer = null;
         this.entityRenderer.func_190564_k();
         this.playerController = null;
         NarratorChatListener.field_193643_a.func_193642_b();
      }

      this.renderViewEntity = null;
      this.myNetworkManager = null;
      if (this.loadingScreen != null) {
         this.loadingScreen.resetProgressAndMessage(loadingMessage);
         if (!this.fastWorldLoad()) {
            this.loadingScreen.displayLoadingString("");
         }
      }

      if (worldClientIn == null && this.world != null) {
         this.mcResourcePackRepository.clearResourcePack();
         this.ingameGUI.resetPlayersOverlayFooterHeader();
         this.setServerData(null);
         this.integratedServerIsRunning = false;
      }

      this.mcSoundHandler.stopSounds();
      this.world = worldClientIn;
      if (this.renderGlobal != null) {
         this.renderGlobal.setWorldAndLoadRenderers(worldClientIn);
      }

      if (this.effectRenderer != null) {
         this.effectRenderer.clearEffects(worldClientIn);
      }

      TileEntityRendererDispatcher.instance.setWorld(worldClientIn);
      if (worldClientIn != null) {
         if (!this.fastWorldLoad() && !this.integratedServerIsRunning) {
            AuthenticationService authenticationservice = new YggdrasilAuthenticationService(this.proxy, UUID.randomUUID().toString());
            MinecraftSessionService minecraftsessionservice = authenticationservice.createMinecraftSessionService();
            GameProfileRepository gameprofilerepository = authenticationservice.createProfileRepository();
            PlayerProfileCache playerprofilecache = new PlayerProfileCache(
               gameprofilerepository, new File(this.mcDataDir, MinecraftServer.USER_CACHE_FILE.getName())
            );
            TileEntitySkull.setProfileCache(playerprofilecache);
            TileEntitySkull.setSessionService(minecraftsessionservice);
            PlayerProfileCache.setOnlineMode(false);
         }

         if (player == null) {
            player = this.playerController.func_192830_a(worldClientIn, new StatisticsManager(), new RecipeBookClient());
            this.playerController.flipPlayer(player);
         }

         player.preparePlayerToSpawn();
         worldClientIn.spawnEntityInWorld(player);
         player.movementInput = new MovementInputFromOptions(this.gameSettings);
         this.playerController.setPlayerCapabilities(player);
         this.renderViewEntity = player;
      } else {
         this.saveLoader.flushCache();
         player = null;
      }

      if (!this.fastWorldLoad()) {
         System.gc();
      }

      this.systemTime = 0L;
   }

   public void setDimensionAndSpawnPlayer(int dimension) {
      this.world.setInitialSpawnLocation();
      this.world.removeAllEntities();
      int i = 0;
      String s = null;
      if (player != null) {
         i = player.getEntityId();
         this.world.removeEntity(player);
         s = player.getServerBrand();
      }

      this.renderViewEntity = null;
      EntityPlayerSP entityplayersp = player;
      player = this.playerController
         .func_192830_a(
            this.world, player == null ? new StatisticsManager() : player.getStatFileWriter(), player == null ? new RecipeBook() : player.func_192035_E()
         );
      player.getDataManager().setEntryValues(entityplayersp.getDataManager().getAll());
      player.dimension = dimension;
      this.renderViewEntity = player;
      player.preparePlayerToSpawn();
      player.setServerBrand(s);
      this.world.spawnEntityInWorld(player);
      this.playerController.flipPlayer(player);
      player.movementInput = new MovementInputFromOptions(this.gameSettings);
      player.setEntityId(i);
      this.playerController.setPlayerCapabilities(player);
      player.setReducedDebug(entityplayersp.hasReducedDebug());
      if (this.currentScreen instanceof GuiGameOver) {
         this.displayGuiScreen(null);
      }
   }

   public final boolean isDemo() {
      return this.isDemo;
   }

   @Nullable
   public NetHandlerPlayClient getConnection() {
      return player == null ? null : player.connection;
   }

   public static boolean isGuiEnabled() {
      return theMinecraft == null || !theMinecraft.gameSettings.hideGUI;
   }

   public static boolean isFancyGraphicsEnabled() {
      return theMinecraft != null && theMinecraft.gameSettings.fancyGraphics;
   }

   public static boolean isAmbientOcclusionEnabled() {
      return theMinecraft != null && theMinecraft.gameSettings.ambientOcclusion != 0;
   }

   private void middleClickMouse() {
      if (this.objectMouseOver != null && this.objectMouseOver.typeOfHit != RayTraceResult.Type.MISS) {
         boolean canHandStackToPlace = Panic.stop
            || player.connection == null
            || player.connection.getPlayerInfo(player.getUniqueID()) == null
            || player.connection.getPlayerInfo(player.getUniqueID()).getGameType() != GameType.SURVIVAL
            || !PlayerHelper.get.actived
            || !PlayerHelper.get.NoSurvivalCopyBlock.getBool();
         boolean flag = player.capabilities.isCreativeMode;
         TileEntity tileentity = null;
         ItemStack itemstack;
         if (this.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
            BlockPos blockpos = this.objectMouseOver.getBlockPos();
            IBlockState iblockstate = this.world.getBlockState(blockpos);
            Block block = iblockstate.getBlock();
            if (iblockstate.getMaterial() == Material.AIR) {
               return;
            }

            itemstack = block.getItem(this.world, blockpos, iblockstate);
            if (itemstack.func_190926_b()) {
               return;
            }

            if (flag && GuiScreen.isCtrlKeyDown() && block.hasTileEntity()) {
               tileentity = this.world.getTileEntity(blockpos);
            }
         } else {
            if (this.objectMouseOver.typeOfHit != RayTraceResult.Type.ENTITY || this.objectMouseOver.entityHit == null || !flag) {
               return;
            }

            if (this.objectMouseOver.entityHit instanceof EntityPainting) {
               itemstack = new ItemStack(Items.PAINTING);
            } else if (this.objectMouseOver.entityHit instanceof EntityLeashKnot) {
               itemstack = new ItemStack(Items.LEAD);
            } else if (this.objectMouseOver.entityHit instanceof EntityItemFrame entityitemframe) {
               ItemStack itemstack1 = entityitemframe.getDisplayedItem();
               if (itemstack1.func_190926_b()) {
                  itemstack = new ItemStack(Items.ITEM_FRAME);
               } else {
                  itemstack = itemstack1.copy();
               }
            } else if (this.objectMouseOver.entityHit instanceof EntityMinecart entityminecart) {
               itemstack = new ItemStack(switch (entityminecart.getType()) {
                  case FURNACE -> Items.FURNACE_MINECART;
                  case CHEST -> Items.CHEST_MINECART;
                  case TNT -> Items.TNT_MINECART;
                  case HOPPER -> Items.HOPPER_MINECART;
                  case COMMAND_BLOCK -> Items.COMMAND_BLOCK_MINECART;
                  default -> Items.MINECART;
               });
            } else if (this.objectMouseOver.entityHit instanceof EntityBoat) {
               itemstack = new ItemStack(((EntityBoat)this.objectMouseOver.entityHit).getItemBoat());
            } else if (this.objectMouseOver.entityHit instanceof EntityArmorStand) {
               itemstack = new ItemStack(Items.ARMOR_STAND);
            } else if (this.objectMouseOver.entityHit instanceof EntityEnderCrystal) {
               itemstack = new ItemStack(Items.END_CRYSTAL);
            } else {
               ResourceLocation resourcelocation = EntityList.func_191301_a(this.objectMouseOver.entityHit);
               if (resourcelocation == null || !EntityList.ENTITY_EGGS.containsKey(resourcelocation)) {
                  return;
               }

               itemstack = new ItemStack(Items.SPAWN_EGG);
               ItemMonsterPlacer.applyEntityIdToItemStack(itemstack, resourcelocation);
            }
         }

         if (itemstack.func_190926_b()) {
            String s = "";
            if (this.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
               s = Block.REGISTRY.getNameForObject(this.world.getBlockState(this.objectMouseOver.getBlockPos()).getBlock()).toString();
            } else if (this.objectMouseOver.typeOfHit == RayTraceResult.Type.ENTITY) {
               s = EntityList.func_191301_a(this.objectMouseOver.entityHit).toString();
            }

            LOGGER.warn("Picking on: [{}] {} gave null item", this.objectMouseOver.typeOfHit, s);
         } else if (canHandStackToPlace) {
            InventoryPlayer inventoryplayer = player.inventory;
            if (tileentity != null) {
               this.storeTEInStack(itemstack, tileentity);
            }

            int i = inventoryplayer.getSlotFor(itemstack);
            if (flag) {
               inventoryplayer.setPickedItemStack(itemstack);
               this.playerController.sendSlotPacket(player.getHeldItem(EnumHand.MAIN_HAND), 36 + inventoryplayer.currentItem);
            } else if (i != -1) {
               if (InventoryPlayer.isHotbar(i)) {
                  inventoryplayer.currentItem = i;
               } else {
                  this.playerController.pickItem(i);
               }
            }
         }
      }
   }

   private ItemStack storeTEInStack(ItemStack stack, TileEntity te) {
      NBTTagCompound nbttagcompound = te.writeToNBT(new NBTTagCompound());
      if (stack.getItem() == Items.SKULL && nbttagcompound.hasKey("Owner")) {
         NBTTagCompound nbttagcompound2 = nbttagcompound.getCompoundTag("Owner");
         NBTTagCompound nbttagcompound3 = new NBTTagCompound();
         nbttagcompound3.setTag("SkullOwner", nbttagcompound2);
         stack.setTagCompound(nbttagcompound3);
         return stack;
      } else {
         stack.setTagInfo("BlockEntityTag", nbttagcompound);
         NBTTagCompound nbttagcompound1 = new NBTTagCompound();
         NBTTagList nbttaglist = new NBTTagList();
         nbttaglist.appendTag(new NBTTagString("(+NBT)"));
         nbttagcompound1.setTag("Lore", nbttaglist);
         stack.setTagInfo("display", nbttagcompound1);
         return stack;
      }
   }

   public CrashReport addGraphicsAndWorldToCrashReport(CrashReport theCrash) {
      theCrash.getCategory().setDetail("Launched Version", new ICrashReportDetail<String>() {
         public String call() throws Exception {
            return Minecraft.this.launchedVersion;
         }
      });
      theCrash.getCategory().setDetail("LWJGL", new ICrashReportDetail<String>() {
         public String call() throws Exception {
            return Sys.getVersion();
         }
      });
      theCrash.getCategory().setDetail("OpenGL", new ICrashReportDetail<String>() {
         public String call() {
            return GlStateManager.glGetString(7937) + " GL version " + GlStateManager.glGetString(7938) + ", " + GlStateManager.glGetString(7936);
         }
      });
      theCrash.getCategory().setDetail("GL Caps", new ICrashReportDetail<String>() {
         public String call() {
            return OpenGlHelper.getLogText();
         }
      });
      theCrash.getCategory().setDetail("Using VBOs", new ICrashReportDetail<String>() {
         public String call() {
            return Minecraft.this.gameSettings.useVbo ? "Yes" : "No";
         }
      });
      theCrash.getCategory()
         .setDetail(
            "Is Modded",
            new ICrashReportDetail<String>() {
               public String call() throws Exception {
                  String s = ClientBrandRetriever.getDebugVersionString();
                  if (!"vanilla".equals(s)) {
                     return "Definitely; Client brand changed to '" + s + "'";
                  } else {
                     return Minecraft.class.getSigners() == null
                        ? "Very likely; Jar signature invalidated"
                        : "Probably not. Jar signature remains and client brand is untouched.";
                  }
               }
            }
         );
      theCrash.getCategory().setDetail("Type", new ICrashReportDetail<String>() {
         public String call() throws Exception {
            return "Client (map_client.txt)";
         }
      });
      theCrash.getCategory().setDetail("Resource Packs", new ICrashReportDetail<String>() {
         public String call() throws Exception {
            StringBuilder stringbuilder = new StringBuilder();

            for (String s : Minecraft.this.gameSettings.resourcePacks) {
               if (stringbuilder.length() > 0) {
                  stringbuilder.append(", ");
               }

               stringbuilder.append(s);
               if (Minecraft.this.gameSettings.incompatibleResourcePacks.contains(s)) {
                  stringbuilder.append(" (incompatible)");
               }
            }

            return stringbuilder.toString();
         }
      });
      theCrash.getCategory().setDetail("Current Language", new ICrashReportDetail<String>() {
         public String call() throws Exception {
            return Minecraft.this.mcLanguageManager.getCurrentLanguage().toString();
         }
      });
      theCrash.getCategory().setDetail("Profiler Position", new ICrashReportDetail<String>() {
         public String call() throws Exception {
            return Minecraft.this.mcProfiler.profilingEnabled ? Minecraft.this.mcProfiler.getNameOfLastSection() : "N/A (disabled)";
         }
      });
      theCrash.getCategory().setDetail("CPU", new ICrashReportDetail<String>() {
         public String call() throws Exception {
            return OpenGlHelper.getCpu();
         }
      });
      if (this.world != null) {
         this.world.addWorldInfoToCrashReport(theCrash);
      }

      return theCrash;
   }

   public static Minecraft getMinecraft() {
      return theMinecraft;
   }

   public ListenableFuture<Object> scheduleResourcesRefresh() {
      return this.addScheduledTask(new Runnable() {
         @Override
         public void run() {
            Minecraft.this.refreshResources();
         }
      });
   }

   @Override
   public void addServerStatsToSnooper(Snooper playerSnooper) {
      playerSnooper.addClientStat("fps", debugFPS);
      playerSnooper.addClientStat("vsync_enabled", this.gameSettings.enableVsync);
      playerSnooper.addClientStat("display_frequency", Display.getDisplayMode().getFrequency());
      playerSnooper.addClientStat("display_type", this.fullscreen ? "fullscreen" : "windowed");
      playerSnooper.addClientStat("run_time", (MinecraftServer.getCurrentTimeMillis() - playerSnooper.getMinecraftStartTimeMillis()) / 60L * 1000L);
      playerSnooper.addClientStat("current_action", this.getCurrentAction());
      playerSnooper.addClientStat("language", this.gameSettings.language == null ? "en_us" : this.gameSettings.language);
      String s = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? "little" : "big";
      playerSnooper.addClientStat("endianness", s);
      playerSnooper.addClientStat("subtitles", this.gameSettings.showSubtitles);
      playerSnooper.addClientStat("touch", this.gameSettings.touchscreen ? "touch" : "mouse");
      playerSnooper.addClientStat("resource_packs", this.mcResourcePackRepository.getRepositoryEntries().size());
      int i = 0;

      for (ResourcePackRepository.Entry resourcepackrepository$entry : this.mcResourcePackRepository.getRepositoryEntries()) {
         playerSnooper.addClientStat("resource_pack[" + i++ + "]", resourcepackrepository$entry.getResourcePackName());
      }

      if (this.theIntegratedServer != null && this.theIntegratedServer.getPlayerUsageSnooper() != null) {
         playerSnooper.addClientStat("snooper_partner", this.theIntegratedServer.getPlayerUsageSnooper().getUniqueID());
      }
   }

   private String getCurrentAction() {
      if (this.theIntegratedServer != null) {
         return this.theIntegratedServer.getPublic() ? "hosting_lan" : "singleplayer";
      } else if (this.currentServerData != null) {
         return this.currentServerData.isOnLAN() ? "playing_lan" : "multiplayer";
      } else {
         return "out_of_game";
      }
   }

   @Override
   public void addServerTypeToSnooper(Snooper playerSnooper) {
      playerSnooper.addStatToSnooper("opengl_version", GlStateManager.glGetString(7938));
      playerSnooper.addStatToSnooper("opengl_vendor", GlStateManager.glGetString(7936));
      playerSnooper.addStatToSnooper("client_brand", ClientBrandRetriever.getDebugVersionString());
      playerSnooper.addStatToSnooper("launched_version", this.launchedVersion);
      ContextCapabilities contextcapabilities = GLContext.getCapabilities();
      playerSnooper.addStatToSnooper("gl_caps[ARB_arrays_of_arrays]", contextcapabilities.GL_ARB_arrays_of_arrays);
      playerSnooper.addStatToSnooper("gl_caps[ARB_base_instance]", contextcapabilities.GL_ARB_base_instance);
      playerSnooper.addStatToSnooper("gl_caps[ARB_blend_func_extended]", contextcapabilities.GL_ARB_blend_func_extended);
      playerSnooper.addStatToSnooper("gl_caps[ARB_clear_buffer_object]", contextcapabilities.GL_ARB_clear_buffer_object);
      playerSnooper.addStatToSnooper("gl_caps[ARB_color_buffer_float]", contextcapabilities.GL_ARB_color_buffer_float);
      playerSnooper.addStatToSnooper("gl_caps[ARB_compatibility]", contextcapabilities.GL_ARB_compatibility);
      playerSnooper.addStatToSnooper("gl_caps[ARB_compressed_texture_pixel_storage]", contextcapabilities.GL_ARB_compressed_texture_pixel_storage);
      playerSnooper.addStatToSnooper("gl_caps[ARB_compute_shader]", contextcapabilities.GL_ARB_compute_shader);
      playerSnooper.addStatToSnooper("gl_caps[ARB_copy_buffer]", contextcapabilities.GL_ARB_copy_buffer);
      playerSnooper.addStatToSnooper("gl_caps[ARB_copy_image]", contextcapabilities.GL_ARB_copy_image);
      playerSnooper.addStatToSnooper("gl_caps[ARB_depth_buffer_float]", contextcapabilities.GL_ARB_depth_buffer_float);
      playerSnooper.addStatToSnooper("gl_caps[ARB_compute_shader]", contextcapabilities.GL_ARB_compute_shader);
      playerSnooper.addStatToSnooper("gl_caps[ARB_copy_buffer]", contextcapabilities.GL_ARB_copy_buffer);
      playerSnooper.addStatToSnooper("gl_caps[ARB_copy_image]", contextcapabilities.GL_ARB_copy_image);
      playerSnooper.addStatToSnooper("gl_caps[ARB_depth_buffer_float]", contextcapabilities.GL_ARB_depth_buffer_float);
      playerSnooper.addStatToSnooper("gl_caps[ARB_depth_clamp]", contextcapabilities.GL_ARB_depth_clamp);
      playerSnooper.addStatToSnooper("gl_caps[ARB_depth_texture]", contextcapabilities.GL_ARB_depth_texture);
      playerSnooper.addStatToSnooper("gl_caps[ARB_draw_buffers]", contextcapabilities.GL_ARB_draw_buffers);
      playerSnooper.addStatToSnooper("gl_caps[ARB_draw_buffers_blend]", contextcapabilities.GL_ARB_draw_buffers_blend);
      playerSnooper.addStatToSnooper("gl_caps[ARB_draw_elements_base_vertex]", contextcapabilities.GL_ARB_draw_elements_base_vertex);
      playerSnooper.addStatToSnooper("gl_caps[ARB_draw_indirect]", contextcapabilities.GL_ARB_draw_indirect);
      playerSnooper.addStatToSnooper("gl_caps[ARB_draw_instanced]", contextcapabilities.GL_ARB_draw_instanced);
      playerSnooper.addStatToSnooper("gl_caps[ARB_explicit_attrib_location]", contextcapabilities.GL_ARB_explicit_attrib_location);
      playerSnooper.addStatToSnooper("gl_caps[ARB_explicit_uniform_location]", contextcapabilities.GL_ARB_explicit_uniform_location);
      playerSnooper.addStatToSnooper("gl_caps[ARB_fragment_layer_viewport]", contextcapabilities.GL_ARB_fragment_layer_viewport);
      playerSnooper.addStatToSnooper("gl_caps[ARB_fragment_program]", contextcapabilities.GL_ARB_fragment_program);
      playerSnooper.addStatToSnooper("gl_caps[ARB_fragment_shader]", contextcapabilities.GL_ARB_fragment_shader);
      playerSnooper.addStatToSnooper("gl_caps[ARB_fragment_program_shadow]", contextcapabilities.GL_ARB_fragment_program_shadow);
      playerSnooper.addStatToSnooper("gl_caps[ARB_framebuffer_object]", contextcapabilities.GL_ARB_framebuffer_object);
      playerSnooper.addStatToSnooper("gl_caps[ARB_framebuffer_sRGB]", contextcapabilities.GL_ARB_framebuffer_sRGB);
      playerSnooper.addStatToSnooper("gl_caps[ARB_geometry_shader4]", contextcapabilities.GL_ARB_geometry_shader4);
      playerSnooper.addStatToSnooper("gl_caps[ARB_gpu_shader5]", contextcapabilities.GL_ARB_gpu_shader5);
      playerSnooper.addStatToSnooper("gl_caps[ARB_half_float_pixel]", contextcapabilities.GL_ARB_half_float_pixel);
      playerSnooper.addStatToSnooper("gl_caps[ARB_half_float_vertex]", contextcapabilities.GL_ARB_half_float_vertex);
      playerSnooper.addStatToSnooper("gl_caps[ARB_instanced_arrays]", contextcapabilities.GL_ARB_instanced_arrays);
      playerSnooper.addStatToSnooper("gl_caps[ARB_map_buffer_alignment]", contextcapabilities.GL_ARB_map_buffer_alignment);
      playerSnooper.addStatToSnooper("gl_caps[ARB_map_buffer_range]", contextcapabilities.GL_ARB_map_buffer_range);
      playerSnooper.addStatToSnooper("gl_caps[ARB_multisample]", contextcapabilities.GL_ARB_multisample);
      playerSnooper.addStatToSnooper("gl_caps[ARB_multitexture]", contextcapabilities.GL_ARB_multitexture);
      playerSnooper.addStatToSnooper("gl_caps[ARB_occlusion_query2]", contextcapabilities.GL_ARB_occlusion_query2);
      playerSnooper.addStatToSnooper("gl_caps[ARB_pixel_buffer_object]", contextcapabilities.GL_ARB_pixel_buffer_object);
      playerSnooper.addStatToSnooper("gl_caps[ARB_seamless_cube_map]", contextcapabilities.GL_ARB_seamless_cube_map);
      playerSnooper.addStatToSnooper("gl_caps[ARB_shader_objects]", contextcapabilities.GL_ARB_shader_objects);
      playerSnooper.addStatToSnooper("gl_caps[ARB_shader_stencil_export]", contextcapabilities.GL_ARB_shader_stencil_export);
      playerSnooper.addStatToSnooper("gl_caps[ARB_shader_texture_lod]", contextcapabilities.GL_ARB_shader_texture_lod);
      playerSnooper.addStatToSnooper("gl_caps[ARB_shadow]", contextcapabilities.GL_ARB_shadow);
      playerSnooper.addStatToSnooper("gl_caps[ARB_shadow_ambient]", contextcapabilities.GL_ARB_shadow_ambient);
      playerSnooper.addStatToSnooper("gl_caps[ARB_stencil_texturing]", contextcapabilities.GL_ARB_stencil_texturing);
      playerSnooper.addStatToSnooper("gl_caps[ARB_sync]", contextcapabilities.GL_ARB_sync);
      playerSnooper.addStatToSnooper("gl_caps[ARB_tessellation_shader]", contextcapabilities.GL_ARB_tessellation_shader);
      playerSnooper.addStatToSnooper("gl_caps[ARB_texture_border_clamp]", contextcapabilities.GL_ARB_texture_border_clamp);
      playerSnooper.addStatToSnooper("gl_caps[ARB_texture_buffer_object]", contextcapabilities.GL_ARB_texture_buffer_object);
      playerSnooper.addStatToSnooper("gl_caps[ARB_texture_cube_map]", contextcapabilities.GL_ARB_texture_cube_map);
      playerSnooper.addStatToSnooper("gl_caps[ARB_texture_cube_map_array]", contextcapabilities.GL_ARB_texture_cube_map_array);
      playerSnooper.addStatToSnooper("gl_caps[ARB_texture_non_power_of_two]", contextcapabilities.GL_ARB_texture_non_power_of_two);
      playerSnooper.addStatToSnooper("gl_caps[ARB_uniform_buffer_object]", contextcapabilities.GL_ARB_uniform_buffer_object);
      playerSnooper.addStatToSnooper("gl_caps[ARB_vertex_blend]", contextcapabilities.GL_ARB_vertex_blend);
      playerSnooper.addStatToSnooper("gl_caps[ARB_vertex_buffer_object]", contextcapabilities.GL_ARB_vertex_buffer_object);
      playerSnooper.addStatToSnooper("gl_caps[ARB_vertex_program]", contextcapabilities.GL_ARB_vertex_program);
      playerSnooper.addStatToSnooper("gl_caps[ARB_vertex_shader]", contextcapabilities.GL_ARB_vertex_shader);
      playerSnooper.addStatToSnooper("gl_caps[EXT_bindable_uniform]", contextcapabilities.GL_EXT_bindable_uniform);
      playerSnooper.addStatToSnooper("gl_caps[EXT_blend_equation_separate]", contextcapabilities.GL_EXT_blend_equation_separate);
      playerSnooper.addStatToSnooper("gl_caps[EXT_blend_func_separate]", contextcapabilities.GL_EXT_blend_func_separate);
      playerSnooper.addStatToSnooper("gl_caps[EXT_blend_minmax]", contextcapabilities.GL_EXT_blend_minmax);
      playerSnooper.addStatToSnooper("gl_caps[EXT_blend_subtract]", contextcapabilities.GL_EXT_blend_subtract);
      playerSnooper.addStatToSnooper("gl_caps[EXT_draw_instanced]", contextcapabilities.GL_EXT_draw_instanced);
      playerSnooper.addStatToSnooper("gl_caps[EXT_framebuffer_multisample]", contextcapabilities.GL_EXT_framebuffer_multisample);
      playerSnooper.addStatToSnooper("gl_caps[EXT_framebuffer_object]", contextcapabilities.GL_EXT_framebuffer_object);
      playerSnooper.addStatToSnooper("gl_caps[EXT_framebuffer_sRGB]", contextcapabilities.GL_EXT_framebuffer_sRGB);
      playerSnooper.addStatToSnooper("gl_caps[EXT_geometry_shader4]", contextcapabilities.GL_EXT_geometry_shader4);
      playerSnooper.addStatToSnooper("gl_caps[EXT_gpu_program_parameters]", contextcapabilities.GL_EXT_gpu_program_parameters);
      playerSnooper.addStatToSnooper("gl_caps[EXT_gpu_shader4]", contextcapabilities.GL_EXT_gpu_shader4);
      playerSnooper.addStatToSnooper("gl_caps[EXT_multi_draw_arrays]", contextcapabilities.GL_EXT_multi_draw_arrays);
      playerSnooper.addStatToSnooper("gl_caps[EXT_packed_depth_stencil]", contextcapabilities.GL_EXT_packed_depth_stencil);
      playerSnooper.addStatToSnooper("gl_caps[EXT_paletted_texture]", contextcapabilities.GL_EXT_paletted_texture);
      playerSnooper.addStatToSnooper("gl_caps[EXT_rescale_normal]", contextcapabilities.GL_EXT_rescale_normal);
      playerSnooper.addStatToSnooper("gl_caps[EXT_separate_shader_objects]", contextcapabilities.GL_EXT_separate_shader_objects);
      playerSnooper.addStatToSnooper("gl_caps[EXT_shader_image_load_store]", contextcapabilities.GL_EXT_shader_image_load_store);
      playerSnooper.addStatToSnooper("gl_caps[EXT_shadow_funcs]", contextcapabilities.GL_EXT_shadow_funcs);
      playerSnooper.addStatToSnooper("gl_caps[EXT_shared_texture_palette]", contextcapabilities.GL_EXT_shared_texture_palette);
      playerSnooper.addStatToSnooper("gl_caps[EXT_stencil_clear_tag]", contextcapabilities.GL_EXT_stencil_clear_tag);
      playerSnooper.addStatToSnooper("gl_caps[EXT_stencil_two_side]", contextcapabilities.GL_EXT_stencil_two_side);
      playerSnooper.addStatToSnooper("gl_caps[EXT_stencil_wrap]", contextcapabilities.GL_EXT_stencil_wrap);
      playerSnooper.addStatToSnooper("gl_caps[EXT_texture_3d]", contextcapabilities.GL_EXT_texture_3d);
      playerSnooper.addStatToSnooper("gl_caps[EXT_texture_array]", contextcapabilities.GL_EXT_texture_array);
      playerSnooper.addStatToSnooper("gl_caps[EXT_texture_buffer_object]", contextcapabilities.GL_EXT_texture_buffer_object);
      playerSnooper.addStatToSnooper("gl_caps[EXT_texture_integer]", contextcapabilities.GL_EXT_texture_integer);
      playerSnooper.addStatToSnooper("gl_caps[EXT_texture_lod_bias]", contextcapabilities.GL_EXT_texture_lod_bias);
      playerSnooper.addStatToSnooper("gl_caps[EXT_texture_sRGB]", contextcapabilities.GL_EXT_texture_sRGB);
      playerSnooper.addStatToSnooper("gl_caps[EXT_vertex_shader]", contextcapabilities.GL_EXT_vertex_shader);
      playerSnooper.addStatToSnooper("gl_caps[EXT_vertex_weighting]", contextcapabilities.GL_EXT_vertex_weighting);
      playerSnooper.addStatToSnooper("gl_caps[gl_max_vertex_uniforms]", GlStateManager.glGetInteger(35658));
      GlStateManager.glGetError();
      playerSnooper.addStatToSnooper("gl_caps[gl_max_fragment_uniforms]", GlStateManager.glGetInteger(35657));
      GlStateManager.glGetError();
      playerSnooper.addStatToSnooper("gl_caps[gl_max_vertex_attribs]", GlStateManager.glGetInteger(34921));
      GlStateManager.glGetError();
      playerSnooper.addStatToSnooper("gl_caps[gl_max_vertex_texture_image_units]", GlStateManager.glGetInteger(35660));
      GlStateManager.glGetError();
      playerSnooper.addStatToSnooper("gl_caps[gl_max_texture_image_units]", GlStateManager.glGetInteger(34930));
      GlStateManager.glGetError();
      playerSnooper.addStatToSnooper("gl_caps[gl_max_array_texture_layers]", GlStateManager.glGetInteger(35071));
      GlStateManager.glGetError();
      playerSnooper.addStatToSnooper("gl_max_texture_size", getGLMaximumTextureSize());
      GameProfile gameprofile = this.session.getProfile();
      if (gameprofile != null && gameprofile.getId() != null) {
         playerSnooper.addStatToSnooper("uuid", Hashing.sha1().hashBytes(gameprofile.getId().toString().getBytes(Charsets.ISO_8859_1)).toString());
      }
   }

   public static int getGLMaximumTextureSize() {
      for (int i = 16384; i > 0; i >>= 1) {
         GlStateManager.glTexImage2D(32868, 0, 6408, i, i, 0, 6408, 5121, null);
         int j = GlStateManager.glGetTexLevelParameteri(32868, 0, 4096);
         if (j != 0) {
            return i;
         }
      }

      return -1;
   }

   @Override
   public boolean isSnooperEnabled() {
      return this.gameSettings.snooperEnabled;
   }

   public void setServerData(ServerData serverDataIn) {
      this.currentServerData = serverDataIn;
   }

   @Nullable
   public ServerData getCurrentServerData() {
      return this.currentServerData;
   }

   public boolean isIntegratedServerRunning() {
      return this.integratedServerIsRunning;
   }

   public boolean isSingleplayer() {
      return this.integratedServerIsRunning && this.theIntegratedServer != null;
   }

   @Nullable
   public IntegratedServer getIntegratedServer() {
      return this.theIntegratedServer;
   }

   public static void stopIntegratedServer() {
      if (theMinecraft != null) {
         IntegratedServer integratedserver = theMinecraft.getIntegratedServer();
         if (integratedserver != null) {
            integratedserver.stopServer();
         }
      }
   }

   public Snooper getPlayerUsageSnooper() {
      return this.usageSnooper;
   }

   public static long getSystemTime() {
      return Sys.getTime() * 1000L / Sys.getTimerResolution();
   }

   public boolean isFullScreen() {
      return this.fullscreen;
   }

   public Session getSession() {
      return this.session;
   }

   public PropertyMap getProfileProperties() {
      if (this.profileProperties.isEmpty()) {
         GameProfile gameprofile = this.getSessionService().fillProfileProperties(this.session.getProfile(), false);
         this.profileProperties.putAll(gameprofile.getProperties());
      }

      return this.profileProperties;
   }

   public Proxy getProxy() {
      return this.proxy;
   }

   public TextureManager getTextureManager() {
      return renderEngine;
   }

   public static TextureManager getTextureManagerStatic() {
      return renderEngine;
   }

   public IResourceManager getResourceManager() {
      return this.mcResourceManager;
   }

   public ResourcePackRepository getResourcePackRepository() {
      return this.mcResourcePackRepository;
   }

   public LanguageManager getLanguageManager() {
      return this.mcLanguageManager;
   }

   public TextureMap getTextureMapBlocks() {
      return this.textureMapBlocks;
   }

   public boolean isJava64bit() {
      return this.jvm64bit;
   }

   public boolean isGamePaused() {
      return this.isGamePaused;
   }

   public SoundHandler getSoundHandler() {
      return this.mcSoundHandler;
   }

   private void loadServerLists() {
   }

   public MusicTicker.MusicType getAmbientMusicType() {
      if (this.currentScreen instanceof GuiWinGame) {
         return MusicTicker.MusicType.CREDITS;
      } else if (player == null) {
         return MusicTicker.MusicType.MENU;
      } else if (player.world.provider instanceof WorldProviderHell) {
         return MusicTicker.MusicType.NETHER;
      } else if (player.world.provider instanceof WorldProviderEnd) {
         return this.ingameGUI.getBossOverlay().shouldPlayEndBossMusic() ? MusicTicker.MusicType.END_BOSS : MusicTicker.MusicType.END;
      } else {
         return player.capabilities.isCreativeMode && player.capabilities.allowFlying ? MusicTicker.MusicType.CREATIVE : MusicTicker.MusicType.GAME;
      }
   }

   public void dispatchKeypresses() {
      int i = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
      if (i != 0
         && !Keyboard.isRepeatEvent()
         && (!(this.currentScreen instanceof GuiControls) || ((GuiControls)this.currentScreen).time <= getSystemTime() - 20L)
         && Keyboard.getEventKeyState()) {
         if (i == this.gameSettings.keyBindFullscreen.getKeyCode()) {
            this.toggleFullscreen();
         } else if (i == this.gameSettings.keyBindScreenshot.getKeyCode()) {
            if (Bypass.get.isActived() && Bypass.get.LegitScreenshot.getBool()) {
               this.runScreenshot = true;
               this.ticksScreenshotsUpdate = 0;
            } else {
               this.ingameGUI
                  .getChatGUI()
                  .printChatMessage(ScreenShotHelper.saveScreenshot(this.mcDataDir, this.displayWidth, this.displayHeight, this.framebufferMc));
            }
         } else if (i == 48 && GuiScreen.isCtrlKeyDown() && (this.currentScreen == null || this.currentScreen != null && !this.currentScreen.func_193976_p())) {
            this.gameSettings.setOptionValue(GameSettings.Options.NARRATOR, 1);
            if (this.currentScreen instanceof ScreenChatOptions) {
               ((ScreenChatOptions)this.currentScreen).func_193024_a();
            }
         }
      }
   }

   public MinecraftSessionService getSessionService() {
      return this.sessionService;
   }

   public SkinManager getSkinManager() {
      return this.skinManager;
   }

   @Nullable
   public Entity getRenderViewEntity() {
      return this.renderViewEntity;
   }

   public void setRenderViewEntity(Entity viewingEntity) {
      this.renderViewEntity = viewingEntity;
      this.entityRenderer.loadEntityShader(viewingEntity);
   }

   public <V> ListenableFuture<V> addScheduledTask(Callable<V> callableToSchedule) {
      Validate.notNull(callableToSchedule);
      if (this.isCallingFromMinecraftThread()) {
         try {
            return Futures.immediateFuture(callableToSchedule.call());
         } catch (Exception var5) {
            return Futures.immediateFailedCheckedFuture(var5);
         }
      } else {
         ListenableFutureTask<V> listenablefuturetask = ListenableFutureTask.create(callableToSchedule);
         synchronized (this.scheduledTasks) {
            this.scheduledTasks.add(listenablefuturetask);
            return listenablefuturetask;
         }
      }
   }

   @Override
   public ListenableFuture<Object> addScheduledTask(Runnable runnableToSchedule) {
      Validate.notNull(runnableToSchedule);
      return this.addScheduledTask(Executors.callable(runnableToSchedule));
   }

   @Override
   public boolean isCallingFromMinecraftThread() {
      return Thread.currentThread() == this.mcThread;
   }

   public BlockRendererDispatcher getBlockRendererDispatcher() {
      return this.blockRenderDispatcher;
   }

   public RenderManager getRenderManager() {
      return this.renderManager;
   }

   public RenderItem getRenderItem() {
      return this.renderItem;
   }

   public ItemRenderer getItemRenderer() {
      return this.itemRenderer;
   }

   public <T> ISearchTree<T> func_193987_a(SearchTreeManager.Key<T> p_193987_1_) {
      return this.field_193995_ae.func_194010_a(p_193987_1_);
   }

   public static int getDebugFPS() {
      return debugFPS;
   }

   public FrameTimer getFrameTimer() {
      return this.frameTimer;
   }

   public boolean isConnectedToRealms() {
      return this.connectedToRealms;
   }

   public void setConnectedToRealms(boolean isConnected) {
      this.connectedToRealms = isConnected;
   }

   public DataFixer getDataFixer() {
      return this.dataFixer;
   }

   public float getRenderPartialTicks() {
      return this.timer.field_194147_b;
   }

   public float func_193989_ak() {
      return this.timer.field_194148_c;
   }

   public BlockColors getBlockColors() {
      return this.blockColors;
   }

   public boolean isReducedDebug() {
      return player != null && player.hasReducedDebug() || this.gameSettings.reducedDebugInfo;
   }

   public GuiToast func_193033_an() {
      return this.field_193034_aS;
   }

   public Tutorial func_193032_ao() {
      return this.field_193035_aW;
   }

   public static boolean hasEnemyApoGP() {
      String apoProperty = GL11.glGetString(7937);
      String[] arrayBad = new String[]{"llvm", "drm", "cachyos"};
      String[] arrayWhites = new String[]{"nvidia", "amd", "intel(r)", "radeon", " rtx ", " gtx ", " gt ", "rx", "hd"};
      return List.of(arrayBad).stream().filter(Objects::nonNull).anyMatch(constant -> apoProperty.toLowerCase().contains(constant.toLowerCase()))
         || List.of(arrayWhites).stream().filter(Objects::nonNull).noneMatch(constant -> apoProperty.toLowerCase().contains(constant.toLowerCase()));
   }
}
