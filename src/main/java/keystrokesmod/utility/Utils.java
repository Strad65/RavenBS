package keystrokesmod.utility;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.IntStream;
import keystrokesmod.helper.MouseHelper;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityPlayerSP;
import keystrokesmod.mixin.impl.accessor.IAccessorGuiIngame;
import keystrokesmod.mixin.impl.accessor.IAccessorItemFood;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.mixin.impl.accessor.IAccessorPlayerControllerMP;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.combat.AutoClicker;
import keystrokesmod.module.impl.minigames.DuelsStats;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.impl.player.Safewalk;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBanner;
import net.minecraft.block.BlockBeacon;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockClay;
import net.minecraft.block.BlockDaylightDetector;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockEndPortal;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockGravel;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneTorch;
import net.minecraft.block.BlockSand;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockSoulSand;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockWeb;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.potion.Potion;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Utils {
   private static final Random rand = new Random();
   public static final Minecraft mc = Minecraft.getMinecraft();
   public static HashSet<String> friends = new HashSet<>();
   public static HashSet<String> enemies = new HashSet<>();
   public static final Logger log = LogManager.getLogger();
   private static int darkRed = new Color(189, 0, 1).getRGB();
   private static int red = new Color(253, 63, 63).getRGB();
   private static int gold = new Color(215, 162, 50).getRGB();
   private static int yellow = new Color(254, 254, 62).getRGB();
   private static int darkGreen = new Color(0, 191, 4).getRGB();
   private static int green = new Color(64, 253, 62).getRGB();
   private static int aqua = new Color(65, 255, 254).getRGB();
   private static int darkAqua = new Color(0, 190, 189).getRGB();
   private static int darkBlue = new Color(1, 1, 187).getRGB();
   private static int blue = new Color(61, 64, 255).getRGB();
   private static int lightPurple = new Color(254, 63, 255).getRGB();
   private static int darkPurple = new Color(190, 0, 190).getRGB();
   private static int gray = new Color(190, 190, 190).getRGB();
   private static int darkGray = new Color(63, 63, 63).getRGB();
   private static int black = new Color(17, 17, 17).getRGB();
   public static boolean sneakState;

   public static void setSneak(boolean state) {
      KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), state);
      mc.thePlayer.setSneaking(state);
      sneakState = state;
   }

   public static boolean isMining() {
      return tabbedIn()
         && Mouse.isButtonDown(0)
         && mc.objectMouseOver != null
         && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK
         && mc.objectMouseOver.getBlockPos() != null;
   }

   public static void print(Object s) {
      sendRawMessage(String.valueOf(s));
   }

   public static void modulePrint(Object s) {
      sendRawMessage(String.valueOf(s));
   }

   public static void ping() {
      mc.thePlayer.playSound("note.pling", 1.0F, 1.0F);
   }

   public static boolean holdingTNT() {
      return mc.thePlayer.getHeldItem() == null ? false : mc.thePlayer.getHeldItem().getDisplayName().contains("TNT");
   }

   public static float toPositive(float val) {
      return val < 0.0F ? -val * -2.0F - val : val;
   }

   public static boolean sneakDown() {
      return Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
   }

   public static void rsa() {
      EntityPlayerSP p = mc.thePlayer;
      int armSwingEnd = p.isPotionActive(Potion.digSpeed)
         ? 6 - (1 + p.getActivePotionEffect(Potion.digSpeed).getAmplifier())
         : (p.isPotionActive(Potion.digSlowdown) ? 6 + (1 + p.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2 : 6);
      if (!p.isSwingInProgress || p.swingProgressInt >= armSwingEnd / 2 || p.swingProgressInt < 0) {
         p.swingProgressInt = -1;
         p.isSwingInProgress = true;
      }
   }

   public static boolean addEnemy(String name) {
      if (enemies.add(name.toLowerCase())) {
         sendMessage("&7Added enemy&7: &b" + name);
         return true;
      } else {
         return false;
      }
   }

   public static boolean removeEnemy(String name) {
      if (enemies.remove(name.toLowerCase())) {
         sendMessage("&Removed enemy&7: &b" + name);
         return true;
      } else {
         return false;
      }
   }

   public static float getCameraYaw() {
      return (float)Math.toDegrees(Math.atan2(ActiveRenderInfo.getRotationZ(), ActiveRenderInfo.getRotationX()));
   }

   public static float getCameraPitch() {
      return (float)Math.toDegrees(Math.acos(ActiveRenderInfo.getRotationXZ()));
   }

   public static Vec3 getCameraPos(double renderPartialTicks) {
      if (mc.gameSettings.thirdPersonView == 0) {
         return new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
      }

      float cameraDistance = 4.0F;
      if (ModuleManager.extendCamera != null && ModuleManager.extendCamera.isEnabled()) {
         cameraDistance = (float)ModuleManager.extendCamera.distance.getInput();
      }

      Entity renderEntity = mc.getRenderViewEntity();
      float entityEyeHeight = renderEntity.getEyeHeight();
      double interpolatedX = renderEntity.prevPosX + (renderEntity.posX - renderEntity.prevPosX) * renderPartialTicks;
      double interpolatedY = renderEntity.prevPosY + (renderEntity.posY - renderEntity.prevPosY) * renderPartialTicks + entityEyeHeight;
      double interpolatedZ = renderEntity.prevPosZ + (renderEntity.posZ - renderEntity.prevPosZ) * renderPartialTicks;
      double adjustedDistance = cameraDistance;
      float cameraYaw = getCameraYaw();
      float cameraPitch = getCameraPitch();
      double offsetX = -MathHelper.sin(cameraYaw / 180.0F * (float) Math.PI) * MathHelper.cos(cameraPitch / 180.0F * (float) Math.PI)
         * adjustedDistance;
      double offsetZ = MathHelper.cos(cameraYaw / 180.0F * (float) Math.PI) * MathHelper.cos(cameraPitch / 180.0F * (float) Math.PI)
         * adjustedDistance;
      double offsetY = -MathHelper.sin(cameraPitch / 180.0F * (float) Math.PI) * adjustedDistance;
      if (ModuleManager.noCameraClip == null || !ModuleManager.noCameraClip.isEnabled()) {
         for (int i = 0; i < 8; i++) {
            float cornerOffsetX = ((i & 1) * 2 - 1) * 0.1F;
            float cornerOffsetY = ((i >> 1 & 1) * 2 - 1) * 0.1F;
            float cornerOffsetZ = ((i >> 2 & 1) * 2 - 1) * 0.1F;
            MovingObjectPosition rayTraceResult = mc.theWorld
               .rayTraceBlocks(
                  new Vec3(interpolatedX + cornerOffsetX, interpolatedY + cornerOffsetY, interpolatedZ + cornerOffsetZ),
                  new Vec3(
                     interpolatedX - offsetX + cornerOffsetX + cornerOffsetZ, interpolatedY - offsetY + cornerOffsetY, interpolatedZ - offsetZ + cornerOffsetZ
                  )
               );
            if (rayTraceResult != null) {
               double blockHitDistance = rayTraceResult.hitVec.distanceTo(new Vec3(interpolatedX, interpolatedY, interpolatedZ));
               if (blockHitDistance < adjustedDistance) {
                  adjustedDistance = blockHitDistance;
               }
            }
         }
      }

      double finalCameraX = interpolatedX - offsetX * (adjustedDistance / cameraDistance);
      double finalCameraY = interpolatedY - offsetY * (adjustedDistance / cameraDistance);
      double finalCameraZ = interpolatedZ - offsetZ * (adjustedDistance / cameraDistance);
      return new Vec3(finalCameraX, finalCameraY, finalCameraZ);
   }

   public static void printInfo(EntityLivingBase ent) {
      if (ent != null) {
         sendMessage("&7&m-------------------------");
         sendMessage("&eattacking: &r" + ent.getName());
         sendMessage("&7type: &b" + ent.getClass().getSimpleName());
         sendMessage("&7bot: &r" + (ModuleManager.antiBot.isEnabled() ? AntiBot.isBot(ent) : "&cantibot disabled"));
         boolean isPlayer = ent instanceof EntityPlayer;
         sendMessage("&7player: &r" + isPlayer);
         sendMessage("&7dist eye: &d" + round(getDistanceToEye(ent), 2));
         sendMessage("&7min dist: &d" + round(Math.sqrt(raycastDistanceSq(ent, 12.0, false)), 2));
         IChatComponent displayName = ent.getDisplayName();
         boolean hasDisplayName = displayName != null;
         if (isPlayer) {
            EntityPlayer p = (EntityPlayer)ent;
            UUID uuid = p.getUniqueID();
            sendMessage("&7uuid: &d" + uuid.toString() + " &b" + uuid.variant() + " " + uuid.version());
            NetworkPlayerInfo clientPlayer = mc.getNetHandler().getPlayerInfo(p.getUniqueID());
            sendMessage("&7ping: &d" + (clientPlayer == null ? "&cnot found" : clientPlayer.getResponseTime()));
            sendMessage("&7teammate: &r" + isTeammate(p));
            sendMessage("&7tablist: &r" + isInTabList(p));
            if (p.getTeam() != null) {
               ScorePlayerTeam scoreTeam = (ScorePlayerTeam)p.getTeam();
               sendMessage("&7team name: &r" + scoreTeam.getTeamName());
               sendMessage("&7team prefix: &r" + scoreTeam.getColorPrefix());
               sendMessage("&7team suffix: &r" + scoreTeam.getColorSuffix());
            }
         }

         sendMessage("&7display unformatted: &r" + (hasDisplayName ? displayName.getUnformattedText() : "&cnull"));
         sendMessage("&7insertion: &r" + (hasDisplayName ? displayName.getChatStyle().getInsertion() : "&cnull"));
         sendMessage("&7health: &r" + ent.getHealth());
         sendMessage("&7ht: &d" + ent.hurtTime + " &7mht: &d" + ent.maxHurtTime);
         sendMessage("&7ticks existed: &r" + ent.ticksExisted);
         sendMessage("&7invisible: &r" + ent.isInvisible());
         sendMessage("&7dead: &r" + ent.isDead);
      }
   }

   public static double raycastDistanceSq(Entity en, double max_reach, boolean calc_rot) {
      Vec3 eyeVec = mc.thePlayer.getPositionEyes(1.0F);
      float yaw;
      float pitch;
      if (calc_rot) {
         float[] rot = RotationUtils.getRotations(en);
         yaw = rot[0];
         pitch = rot[1];
      } else {
         yaw = mc.thePlayer.rotationYaw;
         pitch = mc.thePlayer.rotationPitch;
      }

      float ff = MathHelper.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float ff2 = MathHelper.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float ff3 = -MathHelper.cos(-pitch * (float) (Math.PI / 180.0));
      float ff4 = MathHelper.sin(-pitch * (float) (Math.PI / 180.0));
      Vec3 lookVec = new Vec3(ff2 * ff3, ff4, ff * ff3);
      double lookVecX = lookVec.xCoord * max_reach;
      double lookVecY = lookVec.yCoord * max_reach;
      double lookVecZ = lookVec.zCoord * max_reach;
      Vec3 sumVec = eyeVec.addVector(lookVecX, lookVecY, lookVecZ);
      List list = mc.theWorld
         .getEntitiesWithinAABBExcludingEntity(mc.getRenderViewEntity(), mc.getRenderViewEntity().getEntityBoundingBox().addCoord(lookVecX, lookVecY, lookVecZ).expand(1.0, 1.0, 1.0));

      for (int i = 0; i < list.size(); i++) {
         Entity entity = (Entity)list.get(i);
         if (entity == en && entity.canBeCollidedWith()) {
            float cbs = entity.getCollisionBorderSize();
            AxisAlignedBB axis = entity.getEntityBoundingBox().expand(cbs, cbs, cbs);
            MovingObjectPosition mop = axis.calculateIntercept(eyeVec, sumVec);
            if (mop != null) {
               return eyeVec.squareDistanceTo(mop.hitVec);
            }
         }
      }

      return -1.0;
   }

   public static double getDistanceToEye(Entity en) {
      return mc.thePlayer.getPositionEyes(1.0F).distanceTo(en.getPositionEyes(1.0F));
   }

   public static boolean isInTabList(EntityPlayer p) {
      for (NetworkPlayerInfo playerInfo : mc.getNetHandler().getPlayerInfoMap()) {
         if (playerInfo.getGameProfile().equals(p.getGameProfile())) {
            return true;
         }
      }

      return false;
   }

   public static String getServerName() {
      return DuelsStats.nick.isEmpty() ? mc.thePlayer.getName() : DuelsStats.nick;
   }

   public static boolean tabbedIn() {
      return mc.currentScreen == null && mc.inGameHasFocus;
   }

   public static String getHardwareIdForLoad(String url) {
      String hashedId = "";

      try {
         MessageDigest instance = MessageDigest.getInstance("MD5");
         instance.update((System.currentTimeMillis() / 20000L + 29062381L + "J{LlrPhHgj8zy:uB").getBytes("UTF-8"));
         hashedId = String.format("%032x", new BigInteger(1, instance.digest()));
         instance.update(
            (System.getenv("COMPUTERNAME")
                  + System.getenv("PROCESSOR_IDENTIFIER")
                  + System.getenv("PROCESSOR_LEVEL")
                  + Runtime.getRuntime().availableProcessors()
                  + url)
               .getBytes("UTF-8")
         );
         return hashedId;
      } catch (Exception ex) {
         ex.printStackTrace();
         return hashedId;
      }
   }

   public static boolean isConsuming(Entity entity) {
      return !(entity instanceof EntityPlayer) ? false : ((EntityPlayer)entity).isUsingItem() && holdingFood((EntityPlayer)entity);
   }

   public static boolean holdingFood(EntityLivingBase entity) {
      return entity.getHeldItem() != null && entity.getHeldItem().getItem() instanceof ItemFood;
   }

   public static int getColorFromEntity(Entity entity) {
      if (entity instanceof EntityPlayer) {
         ScorePlayerTeam scoreplayerteam = (ScorePlayerTeam)((EntityLivingBase)entity).getTeam();
         if (scoreplayerteam != null) {
            String s = FontRenderer.getFormatFromString(scoreplayerteam.getColorPrefix());
            if (s.length() >= 2) {
               return mc.getRenderManager().getFontRenderer().getColorCode(s.charAt(1));
            }
         }
      }

      String displayName = entity.getDisplayName().getFormattedText();
      displayName = removeFormatCodes(displayName);
      if (!displayName.isEmpty() && displayName.startsWith("§") && displayName.charAt(1) != 'f') {
         switch (displayName.charAt(1)) {
            case '0':
               return black;
            case '1':
               return darkBlue;
            case '2':
               return darkGreen;
            case '3':
               return darkAqua;
            case '4':
               return darkRed;
            case '5':
               return darkPurple;
            case '6':
               return gold;
            case '7':
               return gray;
            case '8':
               return darkGray;
            case '9':
               return blue;
            case ':':
            case ';':
            case '<':
            case '=':
            case '>':
            case '?':
            case '@':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case '[':
            case '\\':
            case ']':
            case '^':
            case '_':
            case '`':
            default:
               return -1;
            case 'a':
               return green;
            case 'b':
               return aqua;
            case 'c':
               return red;
            case 'd':
               return lightPurple;
            case 'e':
               return yellow;
         }
      } else {
         return -1;
      }
   }

   public static int getHurttime(Entity entity) {
      return !(entity instanceof EntityLivingBase) ? -1 : ((EntityLivingBase)entity).hurtTime;
   }

   public static boolean overVoid() {
      for (int i = (int)mc.thePlayer.posY; i > -1; i--) {
         if (!(
            mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, i, mc.thePlayer.posZ)).getBlock() instanceof BlockAir
         )) {
            return false;
         }
      }

      return true;
   }

   public static boolean overVoid(double posX, double posY, double posZ) {
      for (int i = (int)posY; i > -1; i--) {
         if (!(mc.theWorld.getBlockState(new BlockPos(posX, i, posZ)).getBlock() instanceof BlockAir)) {
            return false;
         }
      }

      return true;
   }

   public static Block getBlockFromName(String name) {
      return (Block)Block.blockRegistry.getObject(new ResourceLocation("minecraft:" + name));
   }

   public static boolean canPlayerBeSeen(EntityLivingBase player) {
      double x = player.posX;
      double y = player.posY;
      double z = player.posZ;
      Vec3 vecPlayer = mc.thePlayer.getPositionEyes(1.0F);
      double shoulderHeight = player.getEyeHeight() - 0.2;
      if (canSeeVec(vecPlayer, new Vec3(x + 0.3, shoulderHeight, z))) {
         return true;
      }

      if (canSeeVec(vecPlayer, new Vec3(x - 0.3, shoulderHeight, z))) {
         return true;
      }

      if (canSeeVec(vecPlayer, new Vec3(x, shoulderHeight, z + 0.3))) {
         return true;
      }

      if (canSeeVec(vecPlayer, new Vec3(x, shoulderHeight, z - 0.3))) {
         return true;
      }

      for (double d = player.getEyeHeight() + 0.2; d > 0.0; d -= 0.2) {
         Vec3 vecPoint = new Vec3(x, y + d, z);
         if (canSeeVec(vecPlayer, vecPoint)) {
            return true;
         }
      }

      return false;
   }

   public static boolean holdingFireball() {
      return mc.thePlayer.getHeldItem() == null ? false : mc.thePlayer.getHeldItem().getItem() instanceof ItemFireball;
   }

   public static boolean canSeeVec(Vec3 vecPlayer, Vec3 vecTarget) {
      MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(vecPlayer, vecTarget, false, false, false);
      return mop == null || mop.typeOfHit != MovingObjectType.BLOCK;
   }

   public static List<NetworkPlayerInfo> getTablist(boolean removeSelf) {
      ArrayList<NetworkPlayerInfo> list = new ArrayList<>(mc.getNetHandler().getPlayerInfoMap());
      removeDuplicates(list);
      if (removeSelf) {
         list.remove(mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()));
      }

      return list;
   }

   public static void removeDuplicates(ArrayList list) {
      HashSet set = new HashSet(list);
      list.clear();
      list.addAll(set);
   }

   public static boolean removeFriend(String name) {
      if (friends.remove(name.toLowerCase())) {
         sendMessage("&7Removed &afriend&7: &b" + name);
         return true;
      } else {
         return false;
      }
   }

   public static String getCompilerDirectory() {
      String tempDirStr = System.getProperty("java.io.tmpdir") + "cmF2ZW5fc2NyaXB0cw";
      if (System.getProperty("os.name").toLowerCase().contains("linux")) {
         File tempDir = new File(mc.mcDataDir + File.separator + "keystrokes" + File.separator + "scripts", "compiler_temp");
         return !tempDir.exists() && !tempDir.mkdirs() ? tempDirStr : tempDir.getAbsolutePath();
      } else {
         return tempDirStr;
      }
   }

   public static boolean addFriend(String name) {
      if (friends.add(name.toLowerCase())) {
         sendMessage("&7Added &afriend&7: &b" + name);
         if (enemies.contains(name.toLowerCase())) {
            enemies.remove(name.toLowerCase());
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean isWholeNumber(double num) {
      return num == Math.floor(num);
   }

   public static String asWholeNum(double input) {
      return isWholeNumber(input) ? (int)input + "" : String.valueOf(input);
   }

   public static int randomizeInt(int min, int max) {
      return rand.nextInt(max - min + 1) + min;
   }

   public static double randomizeDouble(double min, double max) {
      return min + (max - min) * rand.nextDouble();
   }

   public static boolean inFov(float fov, BlockPos blockPos) {
      return inFov(fov, blockPos.getX(), blockPos.getZ());
   }

   public static boolean inFov(float fov, Entity entity) {
      return inFov(fov, entity.posX, entity.posZ);
   }

   public static boolean inFov(float fov, double posX, double posZ) {
      return inFov(mc.thePlayer, fov, posX, posZ);
   }

   public static boolean inFov(Entity viewPoint, float fov, double posX, double posZ) {
      fov = (float)(fov * 0.5);
      double wrapAngleTo180_double = MathHelper.wrapAngleTo180_double((viewPoint.rotationYaw - RotationUtils.angle(posX, posZ)) % 360.0F);
      if (wrapAngleTo180_double > 0.0) {
         if (wrapAngleTo180_double < fov) {
            return true;
         }
      } else if (wrapAngleTo180_double > -fov) {
         return true;
      }

      return false;
   }

   public static Vec3 getLookVec(float yaw, float pitch) {
      float f = MathHelper.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f1 = MathHelper.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f2 = -MathHelper.cos(-pitch * (float) (Math.PI / 180.0));
      float f3 = MathHelper.sin(-pitch * (float) (Math.PI / 180.0));
      return new Vec3(f1 * f2, f3, f * f2);
   }

   public static boolean holdingBow() {
      return mc.thePlayer.getHeldItem() == null ? false : mc.thePlayer.getHeldItem().getItem() instanceof ItemBow;
   }

   public static boolean bowBackwards() {
      return holdingBow() && mc.thePlayer.moveStrafing == 0.0F && mc.thePlayer.moveForward <= 0.0F && isMoving();
   }

   public static boolean noSlowingBackWithBow() {
      return ModuleManager.noSlow.noSlowing && bowBackwards();
   }

   public static void sendMessage(String txt) {
      if (nullCheck()) {
         String m = formatColor("&7[&dR&7]&r " + txt);
         mc.thePlayer.addChatMessage(new ChatComponentText(m));
      }
   }

   public static void sendMessageStr(String txt) {
      if (nullCheck()) {
         String m = formatColor("&7[&dR&7]&r " + txt);
         mc.thePlayer.addChatMessage(new ChatComponentText(m));
      }
   }

   public static void sendMessage(Object object) {
      String toString = String.valueOf(object);
      sendMessage(toString);
   }

   public static void sendDebugMessage(String message) {
      if (nullCheck()) {
         mc.thePlayer.addChatMessage(new ChatComponentText("§7[§dR§7]§r " + message));
      }
   }

   public static void attackEntity(Entity e, boolean clientSwing, boolean silentSwing) {
      if (clientSwing) {
         mc.thePlayer.swingItem();
      } else if (silentSwing || !silentSwing && !clientSwing) {
         mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
      }

      mc.playerController.attackEntity(mc.thePlayer, e);
   }

   public static void sendRawMessage(String txt) {
      if (nullCheck()) {
         mc.thePlayer.addChatMessage(new ChatComponentText(formatColor(txt)));
      }
   }

   public static float getTotalHealth(EntityLivingBase entity) {
      return entity.getHealth() + entity.getAbsorptionAmount();
   }

   public static String getHealthStr(EntityLivingBase entity, boolean accountDead) {
      float completeHealth = getTotalHealth(entity);
      if (accountDead && entity.isDead) {
         completeHealth = 0.0F;
      }

      return getColorForHealth(entity.getHealth() / entity.getMaxHealth(), completeHealth);
   }

   public static boolean isBindDown(KeyBinding keyBinding) {
      try {
         return Keyboard.isKeyDown(keyBinding.getKeyCode());
      } catch (IndexOutOfBoundsException e) {
         return Mouse.isButtonDown(100 + keyBinding.getKeyCode());
      }
   }

   public static int getTool(Block block) {
      float n = 1.0F;
      int n2 = -1;

      for (int i = 0; i < InventoryPlayer.getHotbarSize(); i++) {
         ItemStack getStackInSlot = mc.thePlayer.inventory.getStackInSlot(i);
         if (getStackInSlot != null) {
            float a = getEfficiency(getStackInSlot, block);
            if (a > n) {
               n = a;
               n2 = i;
            }
         }
      }

      return n2;
   }

   public static boolean onLadder(Entity entity) {
      int posX = MathHelper.floor_double(entity.posX);
      int posY = MathHelper.floor_double(entity.posY - 0.2F);
      int posZ = MathHelper.floor_double(entity.posZ);
      BlockPos blockpos = new BlockPos(posX, posY, posZ);
      Block block1 = mc.theWorld.getBlockState(blockpos).getBlock();
      return block1 instanceof BlockLadder && !entity.onGround;
   }

   public static float getEfficiency(ItemStack itemStack, Block block) {
      float getStrVsBlock = itemStack.getStrVsBlock(block);
      if (getStrVsBlock > 1.0F) {
         int getEnchantmentLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack);
         if (getEnchantmentLevel > 0) {
            getStrVsBlock += getEnchantmentLevel * getEnchantmentLevel + 1;
         }
      }

      return getStrVsBlock;
   }

   public static boolean isEnemy(EntityPlayer entityPlayer) {
      return !enemies.isEmpty() && enemies.contains(entityPlayer.getName().toLowerCase());
   }

   public static boolean isEnemy(String name) {
      return !enemies.isEmpty() && enemies.contains(name.toLowerCase());
   }

   public static String getColorForHealth(double n, double n2) {
      double health = round(n2, 1);
      return (n < 0.3 ? "§c" : (n < 0.5 ? "§6" : (n < 0.7 ? "§e" : "§a"))) + asWholeNum(health);
   }

   public static int getColorForHealth(double health) {
      return health < 0.3 ? -43691 : (health < 0.5 ? -22016 : (health < 0.7 ? -171 : -11141291));
   }

   public static String formatColor(String txt) {
      return txt.replaceAll("&", "§");
   }

   public static String getFirstColorCode(String input) {
      if (input != null && input.length() >= 2) {
         for (int i = 0; i < input.length() - 1; i++) {
            if (input.charAt(i) == 167) {
               char c = input.charAt(i + 1);
               if (c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F') {
                  return "§" + c;
               }
            }
         }

         return "";
      } else {
         return "";
      }
   }

   public static int getBoldWidth(String string) {
      boolean bold = false;
      int additionalWidth = 0;

      for (int i = 0; i < string.length(); i++) {
         char c0 = string.charAt(i);
         if (c0 == 167 && i + 1 < string.length()) {
            int i2 = "0123456789abcdefklmnor".indexOf(string.toLowerCase(Locale.ENGLISH).charAt(i + 1));
            if (i2 == 17) {
               bold = true;
            }

            i++;
         } else if (bold) {
            additionalWidth++;
         }
      }

      return additionalWidth;
   }

   public static void correctValue(SliderSetting c, SliderSetting d) {
      if (c.getInput() > d.getInput()) {
         double p = c.getInput();
         c.setValueWithEvent(d.getInput());
         d.setValueWithEvent(p);
      }
   }

   public static String generateRandomString(int n) {
      char[] array = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
      StringBuilder sb = new StringBuilder();
      IntStream.range(0, n).forEach(p2 -> sb.append(array[rand.nextInt(array.length)]));
      return sb.toString();
   }

   public static boolean isFriended(EntityPlayer entityPlayer) {
      return !friends.isEmpty() && friends.contains(entityPlayer.getName().toLowerCase());
   }

   public static boolean isFriended(String name) {
      return !friends.isEmpty() && friends.contains(name.toLowerCase());
   }

   public static double getRandomValue(SliderSetting a, SliderSetting b, Random r) {
      return a.getInput() == b.getInput() ? a.getInput() : a.getInput() + r.nextDouble() * (b.getInput() - a.getInput());
   }

   public static boolean nullCheck() {
      return mc.thePlayer != null && mc.theWorld != null;
   }

   public static boolean isHypixel() {
      return !mc.isSingleplayer() && mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP.contains("hypixel.net");
   }

   public static String getHitsToKillStr(EntityPlayer entityPlayer, ItemStack itemStack) {
      int n = (int)Math.ceil(getHitsToKill(entityPlayer, itemStack));
      return "§" + (n <= 1 ? "c" : (n <= 3 ? "6" : (n <= 5 ? "e" : "a"))) + n;
   }

   public static double getHitsToKill(EntityPlayer target, ItemStack usedItem) {
      double heldItemDamageLevel = 1.0;
      if (usedItem != null && (usedItem.getItem() instanceof ItemSword || usedItem.getItem() instanceof ItemAxe)) {
         heldItemDamageLevel += getDamageLevel(usedItem);
      }

      double armorProtPercentage = 0.0;
      double totalEPF = 0.0;

      for (int i = 0; i < 4; i++) {
         ItemStack stack = target.inventory.armorItemInSlot(i);
         if (stack != null && stack.getItem() instanceof ItemArmor) {
            armorProtPercentage += ((ItemArmor)stack.getItem()).damageReduceAmount * 0.04;
            int protLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack);
            if (protLevel != 0) {
               double epf = Math.floor(0.75 * (6 + protLevel * protLevel) / 3.0);
               totalEPF += epf;
            }
         }
      }

      totalEPF = 0.04 * Math.min(Math.ceil(Math.min(totalEPF, 25.0) * 0.75), 20.0);
      double armorReduction = armorProtPercentage + totalEPF * (1.0 - armorProtPercentage);
      double damage = heldItemDamageLevel * (1.0 - armorReduction);
      double hitsToKill = getTotalHealth(target) / damage;
      return round(hitsToKill, 1);
   }

   public static float n() {
      return ae(mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveForward, mc.thePlayer.movementInput.moveStrafe);
   }

   public static String extractFileName(String name) {
      int firstIndex = name.indexOf("_");
      int lastIndex = name.lastIndexOf("_");
      return firstIndex != -1 && lastIndex != -1 && lastIndex > firstIndex ? name.substring(firstIndex + 1, lastIndex) : name;
   }

   public static int mergeAlpha(int color, int alpha) {
      return color & 16777215 | alpha << 24;
   }

   public static int clamp(int n) {
      if (n > 255) {
         return 255;
      } else {
         return n < 4 ? 4 : n;
      }
   }

   public static boolean hasArrows(ItemStack stack) {
      boolean flag = mc.thePlayer.capabilities.isCreativeMode || EnchantmentHelper.getEnchantmentLevel(Enchantment.infinity.effectId, stack) > 0;
      return flag || mc.thePlayer.inventory.hasItem(Items.arrow);
   }

   public static int darkenColor(int color, double percent) {
      int alpha = color >> 24 & 0xFF;
      int red = color >> 16 & 0xFF;
      int green = color >> 8 & 0xFF;
      int blue = color & 0xFF;
      percent = (100.0 - percent) / 100.0;
      red = (int)(red * percent);
      green = (int)(green * percent);
      blue = (int)(blue * percent);
      red = clamp(red);
      green = clamp(green);
      blue = clamp(blue);
      return alpha << 24 | red << 16 | green << 8 | blue;
   }

   public static boolean isTeammate(Entity entity) {
      try {
         Entity teamMate = entity;
         if (mc.thePlayer.isOnSameTeam((EntityLivingBase)entity)
            || mc.thePlayer.getDisplayName().getUnformattedText().startsWith(teamMate.getDisplayName().getUnformattedText().substring(0, 2))
            || getNetworkDisplayName().startsWith(teamMate.getDisplayName().getUnformattedText().substring(0, 2))) {
            return true;
         }
      } catch (Exception var2) {
      }

      return false;
   }

   public static String getNetworkDisplayName() {
      try {
         NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
         return ScorePlayerTeam.formatPlayerName(playerInfo.getPlayerTeam(), playerInfo.getGameProfile().getName());
      } catch (Exception var1) {
         return "";
      }
   }

   public static void setSpeed(double n) {
      if (n == 0.0) {
         mc.thePlayer.motionZ = 0.0;
         mc.thePlayer.motionX = 0.0;
      } else {
         float n3 = n();
         mc.thePlayer.motionX = -Math.sin(n3) * n;
         mc.thePlayer.motionZ = Math.cos(n3) * n;
      }
   }

   public static void resetTimer() {
      ((IAccessorMinecraft)mc).getTimer().timerSpeed = 1.0F;
   }

   public static boolean inInventory() {
      return !nullCheck()
         ? false
         : mc.currentScreen != null
            && mc.thePlayer.inventoryContainer != null
            && mc.thePlayer.inventoryContainer instanceof ContainerPlayer
            && mc.currentScreen instanceof GuiInventory;
   }

   public static int getSkyWarsStatus() {
      List<String> sidebar = getSidebarLines();
      if (sidebar != null && !sidebar.isEmpty()) {
         if (!stripColor(sidebar.get(0)).startsWith("SKYWARS")) {
            return -1;
         }

         for (String line : sidebar) {
            line = stripColor(line);
            if (line.equals("Waiting...") || line.startsWith("Starting in ")) {
               return 1;
            }

            if (line.startsWith("Players left: ")) {
               return 2;
            }
         }

         return 0;
      } else {
         return -1;
      }
   }

   public static String getString(JsonObject type, String member) {
      try {
         return type.get(member).getAsString();
      } catch (Exception er) {
         return "";
      }
   }

   public static int getBedwarsStatus() {
      if (!nullCheck()) {
         return -1;
      }

      Scoreboard scoreboard = mc.theWorld.getScoreboard();
      if (scoreboard == null) {
         return -1;
      }

      ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
      if (objective != null && stripString(objective.getDisplayName()).contains("BED WARS")) {
         for (String line : getSidebarLines()) {
            line = stripString(line);
            String[] parts = line.split("  ");
            if (parts.length <= 1) {
               if (!line.equals("Waiting...") && !line.startsWith("Starting in")) {
                  if (!line.startsWith("R Red:") && !line.startsWith("B Blue:")) {
                     continue;
                  }

                  return 2;
               }

               return 1;
            } else if (parts[1].startsWith("L")) {
               return 0;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public static String stripString(String s) {
      char[] nonValidatedString = StringUtils.stripControlCodes(s).toCharArray();
      StringBuilder validated = new StringBuilder();

      for (char c : nonValidatedString) {
         if (c < 127 && c > 20) {
            validated.append(c);
         }
      }

      return validated.toString();
   }

   public static List<String> getSidebarLines() {
      List<String> lines = new ArrayList<>();
      if (mc.theWorld == null) {
         return lines;
      }

      Scoreboard scoreboard = mc.theWorld.getScoreboard();
      if (scoreboard == null) {
         return lines;
      }

      ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
      if (objective == null) {
         return lines;
      }

      Collection<Score> scores = scoreboard.getSortedScores(objective);
      List<Score> list = new ArrayList<>();

      for (Score input : scores) {
         if (input != null && input.getPlayerName() != null && !input.getPlayerName().startsWith("#")) {
            list.add(input);
         }
      }

      if (list.size() > 15) {
         scores = new ArrayList<>(Lists.newArrayList(Iterables.skip(list, list.size() - 15)));
      } else {
         scores = list;
      }

      int index = 0;

      for (Score score : scores) {
         index++;
         ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
         lines.add(ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()));
         if (index == scores.size()) {
            lines.add(objective.getDisplayName());
         }
      }

      Collections.reverse(lines);
      return lines;
   }

   public static Random getRandom() {
      return rand;
   }

   public static boolean isMoving() {
      return mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F;
   }

   public static void aim(Entity en, float offset, boolean sendPacket) {
      if (en != null) {
         float[] t = getRotationsOld(en);
         if (t != null) {
            float y = t[0];
            float p = t[1] + 4.0F + offset;
            if (sendPacket) {
               mc.getNetHandler().addToSendQueue(new C05PacketPlayerLook(y, p, mc.thePlayer.onGround));
            } else {
               mc.thePlayer.rotationYaw = y;
               mc.thePlayer.rotationPitch = p;
            }
         }
      }
   }

   public static float getAngleDifference(float from, float to) {
      float difference = (to - from) % 360.0F;
      if (difference < -180.0F) {
         difference += 360.0F;
      } else if (difference >= 180.0F) {
         difference -= 360.0F;
      }

      return difference;
   }

   public static float getPitchDifference(float from, float to) {
      return (to - from) % 90.0F;
   }

   public static float[] getRotationsOld(Entity q) {
      if (q == null) {
         return null;
      }

      double diffX = q.posX - mc.thePlayer.posX;
      double diffY;
      if (q instanceof EntityLivingBase) {
         EntityLivingBase en = (EntityLivingBase)q;
         diffY = en.posY + en.getEyeHeight() * 0.9 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
      } else {
         diffY = (q.getEntityBoundingBox().minY + q.getEntityBoundingBox().maxY) / 2.0
            - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
      }

      double diffZ = q.posZ - mc.thePlayer.posZ;
      double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
      float yaw = (float)(Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0F;
      float pitch = (float)(-(Math.atan2(diffY, dist) * 180.0 / Math.PI));
      return new float[]{
         mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw),
         mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch)
      };
   }

   public static double aimDifference(Entity en, boolean useServerYaw) {
      return (((useServerYaw ? RotationUtils.serverRotations[0] : mc.thePlayer.rotationYaw) - getYaw(en)) % 360.0 + 540.0) % 360.0 - 180.0;
   }

   public static float getYaw(Entity ent) {
      double x = ent.posX - mc.thePlayer.posX;
      double z = ent.posZ - mc.thePlayer.posZ;
      double yaw = Math.atan2(x, z) * (180.0 / Math.PI);
      return (float)(yaw * -1.0);
   }

   public static void switchSlot(int slot, boolean instant) {
      mc.thePlayer.inventory.currentItem = slot;
      if (instant) {
         ((IAccessorPlayerControllerMP)mc.playerController).syncCurrentPlayItem();
      }
   }

   public static MovingObjectPosition getTarget(double reach) {
      return getTarget(reach, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
   }

   public static MovingObjectPosition getTarget(double reach, float yaw, float pitch) {
      Vec3 eyeVec = mc.thePlayer.getPositionEyes(1.0F);
      float y = -yaw * (float) (Math.PI / 180.0);
      float p = -pitch * (float) (Math.PI / 180.0);
      float f = MathHelper.cos(y - (float) Math.PI);
      float f2 = MathHelper.sin(y - (float) Math.PI);
      float f3 = -MathHelper.cos(p);
      float f4 = MathHelper.sin(p);
      Vec3 lookVec = new Vec3(f2 * f3, f4, f * f3);
      Vec3 sumVec = eyeVec.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach);
      return mc.theWorld.rayTraceBlocks(eyeVec, sumVec, false, false, false);
   }

   public static boolean isPossibleToReach(BlockPos pos, double reach) {
      float[] rot = RotationUtils.getRotations(pos);
      Vec3 eyeVec = mc.thePlayer.getPositionEyes(1.0F);
      float y = -rot[0] * (float) (Math.PI / 180.0);
      float p = -rot[1] * (float) (Math.PI / 180.0);
      float f = MathHelper.cos(y - (float) Math.PI);
      float f2 = MathHelper.sin(y - (float) Math.PI);
      float f3 = -MathHelper.cos(p);
      float f4 = MathHelper.sin(p);
      Vec3 lookVec = new Vec3(f2 * f3, f4, f * f3);
      Vec3 sumVec = eyeVec.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach);
      AxisAlignedBB axis = BlockUtils.getBlock(pos).getCollisionBoundingBox(mc.theWorld, pos, BlockUtils.getBlockState(pos));
      if (axis == null) {
         return false;
      }

      MovingObjectPosition mop = axis.calculateIntercept(eyeVec, sumVec);
      return mop != null;
   }

   public static void setSpeed(double val, boolean checkMoving) {
      if (!checkMoving || isMoving()) {
         mc.thePlayer.motionX = -Math.sin(gd()) * val;
         mc.thePlayer.motionZ = Math.cos(gd()) * val;
      }
   }

   public static boolean keysDown() {
      return Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())
         || Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())
         || Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())
         || Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
   }

   public static boolean jumpDown() {
      return Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
   }

   public static boolean usingBedAura() {
      return ModuleManager.bedAura != null
         && ModuleManager.bedAura.currentBlock != null
         && RotationUtils.inRange(ModuleManager.bedAura.currentBlock, ModuleManager.bedAura.range.getInput());
   }

   public static double distanceToGround() {
      if (mc.thePlayer.onGround) {
         return 0.0;
      }

      double fallDistance = -1.0;
      double y = mc.thePlayer.posY;
      if (mc.thePlayer.posY % 1.0 == 0.0) {
         y--;
      }

      for (int i = (int)Math.floor(y); i > -1; i--) {
         if (!isPlaceable(new BlockPos(mc.thePlayer.posX, i, mc.thePlayer.posZ))) {
            fallDistance = y - i;
            break;
         }
      }

      return fallDistance - 1.0;
   }

   public static double distanceToGround(Entity entity) {
      if (entity.onGround) {
         return 0.0;
      }

      double fallDistance = -1.0;
      double y = entity.posY;
      if (entity.posY % 1.0 == 0.0) {
         y--;
      }

      for (int i = (int)Math.floor(y); i > -1; i--) {
         if (!isPlaceable(new BlockPos(entity.posX, i, entity.posZ))) {
            fallDistance = y - i;
            break;
         }
      }

      return fallDistance - 1.0;
   }

   public static double distanceToGround(Entity entity, int x, int z) {
      if (entity.onGround) {
         return 0.0;
      }

      double fallDistance = -1.0;
      double y = entity.posY;
      if (entity.posY % 1.0 == 0.0) {
         y--;
      }

      for (int i = (int)Math.floor(y); i > -1; i--) {
         if (!isPlaceable(new BlockPos(x, i, z))) {
            fallDistance = y - i;
            break;
         }
      }

      return fallDistance - 1.0;
   }

   public static float gd() {
      float yw = mc.thePlayer.rotationYaw;
      if (mc.thePlayer.moveForward < 0.0F) {
         yw += 180.0F;
      }

      float f;
      if (mc.thePlayer.moveForward < 0.0F) {
         f = -0.5F;
      } else if (mc.thePlayer.moveForward > 0.0F) {
         f = 0.5F;
      } else {
         f = 1.0F;
      }

      if (mc.thePlayer.moveStrafing > 0.0F) {
         yw -= 90.0F * f;
      }

      if (mc.thePlayer.moveStrafing < 0.0F) {
         yw += 90.0F * f;
      }

      return yw * (float) (Math.PI / 180.0);
   }

   public static float ae(float n, float n2, float n3) {
      float n4 = 1.0F;
      if (n2 < 0.0F) {
         n += 180.0F;
         n4 = -0.5F;
      } else if (n2 > 0.0F) {
         n4 = 0.5F;
      }

      if (n3 > 0.0F) {
         n -= 90.0F * n4;
      } else if (n3 < 0.0F) {
         n += 90.0F * n4;
      }

      return n * (float) (Math.PI / 180.0);
   }

   public static double getHorizontalSpeed() {
      return getHorizontalSpeed(mc.thePlayer);
   }

   public static double getHorizontalSpeed(Entity entity) {
      return Math.sqrt(entity.motionX * entity.motionX + entity.motionZ * entity.motionZ);
   }

   public static List<String> getTopLevelLines(String fileContents) {
      List<String> topLevelLines = new ArrayList<>();
      String[] lines = fileContents.split("\\r?\\n");
      int braceLevel = 0;
      boolean inBlockComment = false;

      for (String line : lines) {
         String originalLine = line;
         String processedLine = line.trim();
         if (inBlockComment) {
            if (!processedLine.contains("*/")) {
               continue;
            }

            inBlockComment = false;
            processedLine = processedLine.substring(processedLine.indexOf("*/") + 2).trim();
         }

         if (!processedLine.startsWith("//")) {
            if (processedLine.contains("/*")) {
               inBlockComment = true;
               processedLine = processedLine.substring(0, processedLine.indexOf("/*")).trim();
               if (processedLine.isEmpty()) {
                  continue;
               }
            }

            if (processedLine.contains("//")) {
               processedLine = processedLine.substring(0, processedLine.indexOf("//")).trim();
            }

            if (processedLine.contains("/*") && processedLine.contains("*/")) {
               processedLine = processedLine.substring(0, processedLine.indexOf("/*")) + processedLine.substring(processedLine.indexOf("*/") + 2);
               processedLine = processedLine.trim();
            }

            if (!processedLine.isEmpty()) {
               String lineWithoutStrings = removeStringLiterals(processedLine);
               int openBraces = 0;
               int closeBraces = 0;

               for (char ch : lineWithoutStrings.toCharArray()) {
                  if (ch == '{') {
                     openBraces++;
                  } else if (ch == '}') {
                     closeBraces++;
                  }
               }

               braceLevel += openBraces - closeBraces;
               if (braceLevel == 0 && !processedLine.contains("{") && !processedLine.contains("}") && !processedLine.startsWith("@")) {
                  topLevelLines.add(originalLine.trim());
               }
            }
         }
      }

      return topLevelLines;
   }

   public static boolean holdingEdible(ItemStack stack) {
      if (stack.getItem() instanceof ItemFood && mc.thePlayer.getFoodStats().getFoodLevel() == 20) {
         ItemFood food = (ItemFood)stack.getItem();
         return ((IAccessorItemFood)food).getAlwaysEdible();
      } else {
         return true;
      }
   }

   private static String removeStringLiterals(String line) {
      StringBuilder sb = new StringBuilder();
      boolean inString = false;

      for (int i = 0; i < line.length(); i++) {
         char ch = line.charAt(i);
         if (ch == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
            inString = !inString;
         } else if (!inString) {
            sb.append(ch);
         }
      }

      return sb.toString();
   }

   public static boolean blockAbove() {
      return !(
         BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2.0, mc.thePlayer.posZ)) instanceof BlockAir
      );
   }

   public static boolean onEdge() {
      return onEdge(mc.thePlayer);
   }

   public static boolean onEdge(Entity entity) {
      return mc.theWorld
         .getCollidingBoundingBoxes(entity, entity.getEntityBoundingBox().offset(entity.motionX / 3.0, -1.0, entity.motionZ / 3.0))
         .isEmpty();
   }

   public static boolean lookingAtBlock() {
      return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK && mc.objectMouseOver.getBlockPos() != null;
   }

   public static boolean isDiagonal(boolean strict) {
      float yaw = (mc.thePlayer.rotationYaw % 360.0F + 360.0F) % 360.0F;
      yaw = yaw > 180.0F ? yaw - 360.0F : yaw;
      boolean isYawDiagonal = inBetween(-170.0, 170.0, yaw) && !inBetween(-10.0, 10.0, yaw) && !inBetween(80.0, 100.0, yaw) && !inBetween(-100.0, -80.0, yaw);
      if (strict) {
         isYawDiagonal = inBetween(-178.5, 178.5, yaw) && !inBetween(-1.5, 1.5, yaw) && !inBetween(88.5, 91.5, yaw) && !inBetween(-91.5, -88.5, yaw);
      }

      boolean isStrafing = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())
         || Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
      return isYawDiagonal || isStrafing;
   }

   public static double gbps(Entity en, int d) {
      double x = en.posX - en.prevPosX;
      double z = en.posZ - en.prevPosZ;
      double sp = Math.sqrt(x * x + z * z) * 20.0;
      return d == 0 ? sp : round(sp, d);
   }

   public static boolean inBetween(double min, double max, double value) {
      return value >= min && value <= max;
   }

   public static String removeFormatCodes(String str) {
      return str.replace("§k", "").replace("§l", "").replace("§m", "").replace("§n", "").replace("§o", "").replace("§r", "");
   }

   public static boolean isClicking() {
      return ModuleManager.autoClicker.isEnabled() && AutoClicker.leftClick.isToggled()
         ? Mouse.isButtonDown(0)
         : MouseHelper.f() > 1 && System.currentTimeMillis() - MouseHelper.LL < 300L;
   }

   public static double fallDist() {
      if (overVoid()) {
         return 9999.0;
      }

      double fallDistance = -1.0;
      double y = mc.thePlayer.posY;
      if (mc.thePlayer.posY % 1.0 == 0.0) {
         y--;
      }

      for (int i = (int)Math.floor(y); i > -1; i--) {
         if (!isPlaceable(new BlockPos(mc.thePlayer.posX, i, mc.thePlayer.posZ))) {
            fallDistance = y - i;
            break;
         }
      }

      return fallDistance - 1.0;
   }

   public static double fallDistZ() {
      if (mc.thePlayer.onGround) {
         return 0.0;
      }

      if (overVoid()) {
         return 9999.0;
      }

      double fallDistance = -1.0;
      double y = mc.thePlayer.posY;
      if (mc.thePlayer.posY % 1.0 == 0.0) {
         y--;
      }

      for (int i = (int)Math.floor(y); i > -1; i--) {
         if (!isPlaceable(new BlockPos(mc.thePlayer.posX, i, mc.thePlayer.posZ))) {
            fallDistance = y - i;
            break;
         }
      }

      return fallDistance - 1.0;
   }

   public static long time() {
      return System.currentTimeMillis();
   }

   public static boolean isEdgeOfBlock() {
      BlockPos pos = new BlockPos(
         mc.thePlayer.posX, mc.thePlayer.posY - (mc.thePlayer.posY % 1.0 == 0.0 ? 1 : 0), mc.thePlayer.posZ
      );
      return mc.theWorld.isAirBlock(pos);
   }

   public static long timeBetween(long val, long val2) {
      return Math.abs(val2 - val);
   }

   public static void sendModuleMessage(Module module, String s) {
      sendRawMessage("&3" + module.getName() + "&7: &r" + s);
   }

   public static int getLobbyStatus() {
      if (!nullCheck()) {
         return -1;
      }

      Scoreboard scoreboard = mc.theWorld.getScoreboard();
      if (scoreboard == null) {
         return -1;
      }

      ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
      if (objective == null) {
         return -1;
      }

      for (String line : getSidebarLines()) {
         line = stripString(line);
         String[] parts = line.split("  ");
         if (parts.length > 1 && parts[1].startsWith("L")) {
            return 1;
         }
      }

      return -1;
   }

   public static int hypixelStatus() {
      if (!nullCheck()) {
         return -1;
      }

      Scoreboard scoreboard = mc.theWorld.getScoreboard();
      if (scoreboard == null) {
         return -2;
      }

      ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
      if (objective == null) {
         return -1;
      }

      for (String line : getSidebarLines()) {
         line = stripString(line);
         if (line.startsWith("0") || line.startsWith("1")) {
            return 1;
         }
      }

      return -1;
   }

   public static EntityLivingBase raytrace(int range) {
      Entity entity = null;
      EntityPlayer self = (EntityPlayer)(Freecam.freeEntity == null ? mc.thePlayer : Freecam.freeEntity);
      MovingObjectPosition rayTrace = self.rayTrace(range, 1.0F);
      Vec3 getPositionEyes = self.getPositionEyes(1.0F);
      float rotationYaw = self.rotationYaw;
      float rotationPitch = self.rotationPitch;
      float cos = MathHelper.cos(-rotationYaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float sin = MathHelper.sin(-rotationYaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float n2 = -MathHelper.cos(-rotationPitch * (float) (Math.PI / 180.0));
      Vec3 vec3 = new Vec3(sin * n2, MathHelper.sin(-rotationPitch * (float) (Math.PI / 180.0)), cos * n2);
      Vec3 addVector = getPositionEyes.addVector(vec3.xCoord * range, vec3.yCoord * range, vec3.zCoord * range);
      Vec3 vec4 = null;
      List getEntitiesWithinAABBExcludingEntity = mc.theWorld
         .getEntitiesWithinAABBExcludingEntity(
            mc.getRenderViewEntity(),
            mc.getRenderViewEntity()
               .getEntityBoundingBox()
               .addCoord(vec3.xCoord * range, vec3.yCoord * range, vec3.zCoord * range)
               .expand(1.0, 1.0, 1.0)
         );
      double n3 = range;

      for (int i = 0; i < getEntitiesWithinAABBExcludingEntity.size(); i++) {
         Entity entity2 = (Entity)getEntitiesWithinAABBExcludingEntity.get(i);
         if (entity2.canBeCollidedWith()) {
            float getCollisionBorderSize = entity2.getCollisionBorderSize();
            AxisAlignedBB expand = entity2.getEntityBoundingBox().expand(getCollisionBorderSize, getCollisionBorderSize, getCollisionBorderSize);
            MovingObjectPosition calculateIntercept = expand.calculateIntercept(getPositionEyes, addVector);
            if (expand.isVecInside(getPositionEyes)) {
               if (0.0 < n3 || n3 == 0.0) {
                  entity = entity2;
                  vec4 = calculateIntercept == null ? getPositionEyes : calculateIntercept.hitVec;
                  n3 = 0.0;
               }
            } else if (calculateIntercept != null) {
               double distanceTo = getPositionEyes.distanceTo(calculateIntercept.hitVec);
               if (distanceTo < n3 || n3 == 0.0) {
                  if (entity2 != mc.getRenderViewEntity().ridingEntity || entity2.canRiderInteract()) {
                     entity = entity2;
                     vec4 = calculateIntercept.hitVec;
                     n3 = distanceTo;
                  } else if (n3 == 0.0) {
                     entity = entity2;
                     vec4 = calculateIntercept.hitVec;
                  }
               }
            }
         }
      }

      if (entity != null && (n3 < range || rayTrace == null)) {
         rayTrace = new MovingObjectPosition(entity, vec4);
      }

      return rayTrace != null && rayTrace.typeOfHit == MovingObjectType.ENTITY && rayTrace.entityHit instanceof EntityLivingBase
         ? (EntityLivingBase)rayTrace.entityHit
         : null;
   }

   public static int getChroma(long speed, long... delay) {
      long time = System.currentTimeMillis() + (delay.length > 0 ? delay[0] : 0L);
      return Color.getHSBColor((float)(time % (15000L / speed)) / (15000.0F / (float)speed), 1.0F, 1.0F).getRGB();
   }

   public static double round(double val, int decimalPlaces) {
      if (decimalPlaces == 0) {
         return Math.round(val);
      }

      double p = Math.pow(10.0, decimalPlaces);
      return Math.round(val * p) / p;
   }

   public static String stripColor(String string) {
      if (string.isEmpty()) {
         return string;
      }

      char[] array = StringUtils.stripControlCodes(string).toCharArray();
      StringBuilder sb = new StringBuilder();

      for (char c : array) {
         if (c < 127 && c > 20) {
            sb.append(c);
         }
      }

      return sb.toString();
   }

   public static void addToClipboard(String string) {
      try {
         Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
         StringSelection stringSelection = new StringSelection(string);
         clipboard.setContents(stringSelection, null);
      } catch (Exception e) {
         sendMessage("&cFailed to copy &b" + string);
      }
   }

   public static List<String> getScoreBoardOld() {
      List<String> lines = new ArrayList<>();
      if (mc.theWorld == null) {
         return lines;
      }

      Scoreboard scoreboard = mc.theWorld.getScoreboard();
      if (scoreboard == null) {
         return lines;
      }

      ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
      if (objective == null) {
         return lines;
      }

      Collection<Score> scores = scoreboard.getSortedScores(objective);
      List<Score> list = new ArrayList<>();

      for (Score score : scores) {
         if (score != null && score.getPlayerName() != null && !score.getPlayerName().startsWith("#")) {
            list.add(score);
         }
      }

      if (list.size() > 15) {
         scores = Lists.newArrayList(Iterables.skip(list, scores.size() - 15));
      } else {
         scores = list;
      }

      for (Score score : scores) {
         ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
         lines.add(ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()));
      }

      return lines;
   }

   public static void setSwinging() {
      int armSwingEnd = mc.thePlayer.isPotionActive(Potion.digSpeed)
         ? 6 - (1 + mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier())
         : (mc.thePlayer.isPotionActive(Potion.digSlowdown) ? 6 + (1 + mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2 : 6);
      if (!mc.thePlayer.isSwingInProgress || mc.thePlayer.swingProgressInt >= armSwingEnd / 2 || mc.thePlayer.swingProgressInt < 0) {
         mc.thePlayer.swingProgressInt = -1;
         mc.thePlayer.isSwingInProgress = true;
      }
   }

   public static String uf(String s) {
      return s.substring(0, 1).toUpperCase() + s.substring(1);
   }

   public static boolean isPlaceable(BlockPos blockPos) {
      return BlockUtils.replaceable(blockPos) || BlockUtils.isFluid(BlockUtils.getBlock(blockPos));
   }

   public static boolean spectatorCheck() {
      return mc.thePlayer.inventory.getStackInSlot(8) != null && mc.thePlayer.inventory.getStackInSlot(8).getDisplayName().contains("Return")
         || stripString(((IAccessorGuiIngame)mc.ingameGUI).getDisplayedTitle()).contains("YOU DIED");
   }

   public static boolean holdingWeapon() {
      return holdingWeapon(mc.thePlayer);
   }

   public static boolean holdingWeapon(EntityLivingBase entityLivingBase) {
      if (entityLivingBase.getHeldItem() == null) {
         return false;
      }

      Item getItem = entityLivingBase.getHeldItem().getItem();
      return getItem instanceof ItemSword
         || Settings.weaponAxe.isToggled() && getItem instanceof ItemAxe
         || Settings.weaponRod.isToggled() && getItem instanceof ItemFishingRod
         || Settings.weaponStick.isToggled() && getItem == Items.stick;
   }

   public static boolean holdingSword() {
      return mc.thePlayer.getHeldItem() == null ? false : mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
   }

   public static boolean holdingFishingRod() {
      return mc.thePlayer.getHeldItem() == null ? false : mc.thePlayer.getHeldItem().getItem() instanceof ItemFishingRod;
   }

   public static double getDamageLevel(ItemStack itemStack) {
      double baseDamage = 0.0;
      if (itemStack != null) {
         for (Entry<String, AttributeModifier> entry : itemStack.getAttributeModifiers().entries()) {
            if (entry.getKey().equals("generic.attackDamage")) {
               baseDamage = entry.getValue().getAmount();
               break;
            }
         }
      }

      int sharp_level = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack);
      int fire_level = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, itemStack);
      return baseDamage + sharp_level * 1.25 + (fire_level * 4 - 1);
   }

   public static float getDirection() {
      return getCustomDirection(mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveForward, mc.thePlayer.movementInput.moveStrafe);
   }

   public static boolean isUserMoving() {
      return mc.thePlayer.movementInput.moveForward != 0.0F || mc.thePlayer.movementInput.moveStrafe != 0.0F;
   }

   public static net.minecraft.util.Timer getTimer() {
      return (net.minecraft.util.Timer)ObfuscationReflectionHelper.getPrivateValue(
         Minecraft.class, Minecraft.getMinecraft(), new String[]{"timer", "timer"}
      );
   }

   public static void handleTimer(int color, int ticks) {
      color = Theme.getGradient((int)HUD.theme.getInput(), 0.0);
      int widthOffset = ticks < 10 ? 4 : (ticks >= 10 && ticks < 100 ? 7 : (ticks >= 100 && ticks < 1000 ? 10 : (ticks >= 1000 ? 13 : 16)));
      String text = "" + ticks;
      int width = mc.fontRendererObj.getStringWidth(text) + getBoldWidth(text) / 2;
      ScaledResolution scaledResolution = new ScaledResolution(mc);
      int[] display = new int[]{scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
      mc.fontRendererObj.drawString(text, display[0] / 2 - width + widthOffset, display[1] / 2 + 8, color, true);
   }

   public static float getLastReportedYaw() {
      return ((IAccessorEntityPlayerSP)mc.thePlayer).getLastReportedYaw();
   }

   public static boolean safeWalkBackwards() {
      return Safewalk.canSafeWalk() && mc.thePlayer.moveForward <= -0.5 && mc.thePlayer.moveStrafing == 0.0F;
   }

   public static float getCustomDirection(float yaw, float moveForward, float moveStrafe) {
      float forward = 1.0F;
      if (moveForward < 0.0F) {
         yaw += 180.0F;
         forward = -0.5F;
      } else if (moveForward > 0.0F) {
         forward = 0.5F;
      }

      if (moveStrafe > 0.0F) {
         yaw -= 90.0F * forward;
      } else if (moveStrafe < 0.0F) {
         yaw += 90.0F * forward;
      }

      return yaw * (float) (Math.PI / 180.0);
   }

   public static boolean canBePlaced(ItemBlock itemBlock) {
      Block block = itemBlock.getBlock();
      return block == null
         ? false
         : !BlockUtils.isInteractable(block)
            && !(block instanceof BlockSnow)
            && !(block instanceof BlockWeb)
            && !(block instanceof BlockSapling)
            && !(block instanceof BlockDaylightDetector)
            && !(block instanceof BlockBeacon)
            && !(block instanceof BlockBanner)
            && !(block instanceof BlockEndPortalFrame)
            && !(block instanceof BlockEndPortal)
            && !(block instanceof BlockLever)
            && !(block instanceof BlockButton)
            && !(block instanceof BlockSkull)
            && !(block instanceof BlockLiquid)
            && !(block instanceof BlockCactus)
            && !(block instanceof BlockDoublePlant)
            && !(block instanceof BlockLilyPad)
            && !(block instanceof BlockCarpet)
            && !(block instanceof BlockTripWire)
            && !(block instanceof BlockTripWireHook)
            && !(block instanceof BlockTallGrass)
            && !(block instanceof BlockFlower)
            && !(block instanceof BlockFlowerPot)
            && !(block instanceof BlockSign)
            && !(block instanceof BlockLadder)
            && !(block instanceof BlockTorch)
            && !(block instanceof BlockRedstoneTorch)
            && !(block instanceof BlockStairs)
            && !(block instanceof BlockSlab)
            && !(block instanceof BlockFence)
            && !(block instanceof BlockPane)
            && !(block instanceof BlockStainedGlassPane)
            && !(block instanceof BlockGravel)
            && !(block instanceof BlockClay)
            && !(block instanceof BlockSand)
            && !(block instanceof BlockSoulSand)
            && !(block instanceof BlockRailBase);
   }

   public static <E extends Enum<E>> E getEnum(Class<E> enumClass, String value) {
      for (E enumConstant : enumClass.getEnumConstants()) {
         if (enumConstant.name().equals(value)) {
            return enumConstant;
         }
      }

      return null;
   }

   public static int getSpeedAmplifier() {
      return mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 1 + mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() : 0;
   }

   public static ItemStack getSpoofedItem(ItemStack original) {
      if (ModuleManager.scaffold != null
         && ModuleManager.scaffold.isEnabled
         && ModuleManager.scaffold.autoSwap.isToggled()
         && ModuleManager.autoSwap.spoofItem.isToggled()
         && mc.thePlayer != null) {
         return mc.thePlayer
            .inventory
            .getStackInSlot(ModuleManager.scaffold.lastSlot.get() == -1 ? mc.thePlayer.inventory.currentItem : ModuleManager.scaffold.lastSlot.get());
      } else {
         return ModuleManager.autoTool != null
               && ModuleManager.autoTool.isEnabled()
               && ModuleManager.autoTool.spoofItem.isToggled()
               && mc.thePlayer != null
            ? mc.thePlayer
               .inventory
               .getStackInSlot(ModuleManager.autoTool.previousSlot == -1 ? mc.thePlayer.inventory.currentItem : ModuleManager.autoTool.previousSlot)
            : original;
      }
   }

   public static boolean scaffoldDiagonal(boolean strict) {
      float back = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - ModuleManager.scaffold.hardcodedYaw();
      float yaw = (back % 360.0F + 360.0F) % 360.0F;
      yaw = yaw > 180.0F ? yaw - 360.0F : yaw;
      boolean isYawDiagonal = inBetween(-174.0, 174.0, yaw) && !inBetween(-6.0, 6.0, yaw) && !inBetween(84.0, 96.0, yaw) && !inBetween(-96.0, -84.0, yaw);
      if (strict) {
         isYawDiagonal = inBetween(-178.5, 178.5, yaw) && !inBetween(-1.5, 1.5, yaw) && !inBetween(88.5, 91.5, yaw) && !inBetween(-91.5, -88.5, yaw);
      }

      return isYawDiagonal;
   }

   public static String readInputStream(InputStream inputStream) {
      StringBuilder stringBuilder = new StringBuilder();

      try {
         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

         String line;
         while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append('\n');
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return stringBuilder.toString();
   }

   public static boolean isLobby() {
      if (isHypixel()) {
         List<String> sidebarLines = getSidebarLines();
         if (!sidebarLines.isEmpty()) {
            String[] parts = stripColor(sidebarLines.get(1)).split("  ");
            if (parts.length > 1 && parts[1].charAt(0) == 'L') {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean isBedwarsPracticeOrReplay() {
      if (isHypixel()) {
         if (!nullCheck()) {
            return false;
         }

         Scoreboard scoreboard = mc.theWorld.getScoreboard();
         if (scoreboard == null) {
            return false;
         }

         ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
         if (objective == null) {
            return false;
         }

         String stripped = stripString(objective.getDisplayName());
         return stripped.contains("BED WARS PRACTICE") || stripped.contains("REPLAY");
      } else {
         return false;
      }
   }

   public static boolean isReplay() {
      if (isHypixel()) {
         if (!nullCheck()) {
            return false;
         }

         Scoreboard scoreboard = mc.theWorld.getScoreboard();
         if (scoreboard == null) {
            return false;
         }

         ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
         return objective != null && stripString(objective.getDisplayName()).contains("REPLAY");
      } else {
         return false;
      }
   }

   public static boolean isBedwarsPractice() {
      if (isHypixel()) {
         if (!nullCheck()) {
            return false;
         }

         Scoreboard scoreboard = mc.theWorld.getScoreboard();
         if (scoreboard == null) {
            return false;
         }

         ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
         return objective != null && stripString(objective.getDisplayName()).contains("BED WARS PRACTICE");
      } else {
         return false;
      }
   }

   public static class keybinds {
      public static int[] getMousePosition() {
         return new int[]{Mouse.getX(), Mouse.getY()};
      }

      public static int getKeyCode(String key) {
         return Keyboard.getKeyIndex(key);
      }

      public static boolean isMouseDown(int mouseButton) {
         return Mouse.isButtonDown(mouseButton);
      }

      public static boolean isKeyDown(int key) {
         return Keyboard.isKeyDown(key);
      }

      public static void rightClick() {
         ((IAccessorMinecraft)Utils.mc).callRightClickMouse();
      }

      public static void leftClick() {
         ((IAccessorMinecraft)Utils.mc).callClickMouse();
      }
   }
}
