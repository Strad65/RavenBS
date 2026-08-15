package keystrokesmod.script;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.clickgui.components.impl.CategoryComponent;
import keystrokesmod.clickgui.components.impl.ModuleComponent;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityPlayer;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorGuiIngame;
import keystrokesmod.mixin.impl.accessor.IAccessorGuiPlayerTabOverlay;
import keystrokesmod.mixin.impl.accessor.IAccessorGuiScreenBook;
import keystrokesmod.mixin.impl.accessor.IAccessorItemRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.mixin.impl.accessor.IAccessorNetworkManager;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.script.model.Block;
import keystrokesmod.script.model.Bridge;
import keystrokesmod.script.model.Entity;
import keystrokesmod.script.model.Image;
import keystrokesmod.script.model.ItemStack;
import keystrokesmod.script.model.Message;
import keystrokesmod.script.model.NetworkPlayer;
import keystrokesmod.script.model.PlayerState;
import keystrokesmod.script.model.TileEntity;
import keystrokesmod.script.model.Vec3;
import keystrokesmod.script.packet.clientbound.SPacket;
import keystrokesmod.script.packet.serverbound.CPacket;
import keystrokesmod.script.packet.serverbound.PacketHandler;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.ReflectionUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.shader.BlurUtils;
import keystrokesmod.utility.shader.RoundedUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackRepository.Entry;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.Packet;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public class ScriptDefaults {
   private static ExecutorService cachedExecutor;
   private static final Minecraft mc = Minecraft.getMinecraft();
   public static final Bridge bridge = new Bridge();
   private static final LinkedHashMap<String, Module> modulesMap = new LinkedHashMap<>();

   public static void reloadModules() {
      modulesMap.clear();

      for (Module module : keystrokesmod.Raven.getModuleManager().getModules()) {
         modulesMap.put(module.getName(), module);
      }

      for (Module module : keystrokesmod.Raven.scriptManager.scripts.values()) {
         modulesMap.put(module.getName(), module);
      }
   }

   public static class client {
      public static boolean allowFlying() {
         return ScriptDefaults.mc.thePlayer.capabilities.allowFlying;
      }

      public static void removePotionEffect(int id) {
         if (ScriptDefaults.mc.thePlayer != null) {
            ScriptDefaults.mc.thePlayer.removePotionEffectClient(id);
         }
      }

      public static int getUID() {
         return 4;
      }

      public static String getUser() {
         return "mic";
      }

      public static void addEnemy(String username) {
         Utils.addEnemy(username);
      }

      public static void addFriend(String username) {
         Utils.addFriend(username);
      }

      public static void async(Runnable method) {
         if (ScriptDefaults.cachedExecutor == null) {
            ScriptDefaults.cachedExecutor = Executors.newCachedThreadPool();
         }

         ScriptDefaults.cachedExecutor.execute(method);
      }

      public static int getFPS() {
         return Minecraft.getDebugFPS();
      }

      public static void chat(String message) {
         ScriptDefaults.mc.thePlayer.sendChatMessage(message);
      }

      public static void print(String string) {
         Utils.sendRawMessage(string);
      }

      public static void print(Message component) {
         ScriptDefaults.mc.thePlayer.addChatMessage(component.component);
      }

      public static void print(Object object) {
         String s = String.valueOf(object);
         Utils.sendRawMessage(s);
      }

      public static boolean isDiagonal() {
         return Utils.isDiagonal(false);
      }

      public static void setTimer(float timer) {
         ((IAccessorMinecraft)ScriptDefaults.mc).getTimer().timerSpeed = timer;
      }

      public static boolean isCreative() {
         return ScriptDefaults.mc.thePlayer.capabilities.isCreativeMode;
      }

      public static void processPacket(SPacket packet) {
         packet.packet.processPacket(((IAccessorNetworkManager)ScriptDefaults.mc.getNetHandler().getNetworkManager()).getPacketListener());
      }

      public static void multiplyMotion(double factor) {
         ScriptDefaults.mc.thePlayer.motionZ *= factor;
         ScriptDefaults.mc.thePlayer.motionX *= factor;
      }

      public static void processPacketNoEvent(SPacket packet) {
         PacketUtils.receivePacketNoEvent(packet.packet);
      }

      public static String getTitle() {
         return ((IAccessorGuiIngame)ScriptDefaults.mc.ingameGUI).getDisplayedTitle();
      }

      public static String getSubTitle() {
         return ((IAccessorGuiIngame)ScriptDefaults.mc.ingameGUI).getDisplayedSubTitle();
      }

      public static String getRecordPlaying() {
         return ((IAccessorGuiIngame)ScriptDefaults.mc.ingameGUI).getRecordPlaying();
      }

      public static boolean isFlying() {
         return ScriptDefaults.mc.thePlayer.capabilities.isFlying;
      }

      public static void attack(Entity entity) {
         Utils.attackEntity(entity.entity, true, true);
      }

      public static boolean isSinglePlayer() {
         return ScriptDefaults.mc.isSingleplayer();
      }

      public static boolean isSpectator() {
         return ScriptDefaults.mc.thePlayer.isSpectator();
      }

      public static void setFlying(boolean flying) {
         ScriptDefaults.mc.thePlayer.capabilities.isFlying = flying;
      }

      public static void setJump(boolean jump) {
         ScriptDefaults.mc.thePlayer.movementInput.jump = jump;
      }

      public static void setJumping(boolean jump) {
         ScriptDefaults.mc.thePlayer.setJumping(jump);
      }

      public static void setRenderArmPitch(float pitch) {
         ScriptDefaults.mc.thePlayer.prevRenderArmPitch = pitch;
         ScriptDefaults.mc.thePlayer.renderArmPitch = pitch;
      }

      public static float getEquippedProgress() {
         return ((IAccessorItemRenderer)ScriptDefaults.mc.getItemRenderer()).getEquippedProgress();
      }

      public static void disconnect() {
         boolean isLocal = ScriptDefaults.mc.isIntegratedServerRunning();
         boolean isRealms = ScriptDefaults.mc.isConnectedToRealms();
         ScriptDefaults.mc.theWorld.sendQuittingDisconnectingPacket();
         ScriptDefaults.mc.loadWorld(null);
         if (isLocal) {
            ScriptDefaults.mc.displayGuiScreen(new GuiMainMenu());
         } else if (isRealms) {
            new RealmsBridge().switchToRealms(new GuiMainMenu());
         } else {
            ScriptDefaults.mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
         }
      }

      public static float getRenderArmPitch() {
         return ScriptDefaults.mc.thePlayer.renderArmPitch;
      }

      public static void setRenderArmYaw(float yaw) {
         ScriptDefaults.mc.thePlayer.prevRenderArmYaw = yaw;
         ScriptDefaults.mc.thePlayer.renderArmYaw = yaw;
      }

      public static float getRenderArmYaw() {
         return ScriptDefaults.mc.thePlayer.renderArmYaw;
      }

      public static long getTotalMemory() {
         return Runtime.getRuntime().totalMemory();
      }

      public static long getFreeMemory() {
         return Runtime.getRuntime().freeMemory();
      }

      public static long getMaxMemory() {
         return Runtime.getRuntime().maxMemory();
      }

      public static List<String[]> getResourcePacks() {
         List<String[]> packs = new ArrayList<>();
         if (ScriptDefaults.mc.getResourcePackRepository().getRepositoryEntries() != null && !ScriptDefaults.mc.getResourcePackRepository().getRepositoryEntries().isEmpty()) {
            for (Entry entry : ScriptDefaults.mc.getResourcePackRepository().getRepositoryEntries()) {
               packs.add(new String[]{entry.getResourcePackName(), entry.getTexturePackDescription()});
            }
         } else {
            packs.add(new String[]{ScriptDefaults.mc.mcDefaultResourcePack.getPackName(), ""});
         }

         Collections.reverse(packs);
         return packs;
      }

      public static void jump() {
         ScriptDefaults.mc.thePlayer.jump();
      }

      public static boolean allowEditing() {
         return ScriptDefaults.mc.thePlayer != null && ScriptDefaults.mc.thePlayer.capabilities != null
            ? ScriptDefaults.mc.thePlayer.capabilities.allowEdit
            : false;
      }

      public static void setItemInUseCount(int count) {
         ((IAccessorEntityPlayer)ScriptDefaults.mc.thePlayer).setItemInUseCount(count);
      }

      public static int getItemInUseCount() {
         return ScriptDefaults.mc.thePlayer.getItemInUseCount();
      }

      public static int getItemInUseDuration() {
         return ScriptDefaults.mc.thePlayer.getItemInUseDuration();
      }

      public static void log(Object obj) {
         Utils.log.info(obj);
      }

      public static void setSneaking(boolean sneak) {
         ScriptDefaults.mc.thePlayer.setSneaking(sneak);
      }

      public static void setSneak(boolean sneak) {
         ScriptDefaults.mc.thePlayer.movementInput.sneak = sneak;
      }

      public static boolean isSneak() {
         return ScriptDefaults.mc.thePlayer.movementInput.sneak;
      }

      public static Entity getPlayer() {
         return ScriptDefaults.mc != null && ScriptDefaults.mc.thePlayer != null ? Entity.convert(ScriptDefaults.mc.thePlayer) : null;
      }

      public static void removeEnemy(String username) {
         Utils.removeEnemy(username);
      }

      public static void removeFriend(String username) {
         Utils.removeFriend(username);
      }

      public static boolean isRiding() {
         return ScriptDefaults.mc.thePlayer.isRiding();
      }

      public static Vec3 getMotion() {
         return new Vec3(
            ScriptDefaults.mc.thePlayer.motionX, ScriptDefaults.mc.thePlayer.motionY, ScriptDefaults.mc.thePlayer.motionZ
         );
      }

      public static void sleep(long ms) {
         try {
            Thread.sleep(ms);
         } catch (InterruptedException var3) {
         }
      }

      public static void ping() {
         ScriptDefaults.mc.thePlayer.playSound("note.pling", 1.0F, 1.0F);
      }

      public static void playSound(String name, float volume, float pitch) {
         ScriptDefaults.mc.thePlayer.playSound(name, volume, pitch);
      }

      public static boolean isMoving() {
         return Utils.isMoving();
      }

      public static boolean isJump() {
         return ScriptDefaults.mc.thePlayer.movementInput.jump;
      }

      public static float getStrafe() {
         return ScriptDefaults.mc.thePlayer.movementInput.moveStrafe;
      }

      public static void sleep(int ms) {
         try {
            Thread.sleep(ms);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      public static float getForward() {
         return ScriptDefaults.mc.thePlayer.movementInput.moveForward;
      }

      public static void closeScreen() {
         if (ScriptDefaults.mc.currentScreen instanceof ClickGui) {
            ScriptDefaults.mc.displayGuiScreen(null);
         } else {
            ScriptDefaults.mc.thePlayer.closeScreen();
         }
      }

      public static String getScreen() {
         return ScriptDefaults.mc.currentScreen == null ? "" : ScriptDefaults.mc.currentScreen.getClass().getSimpleName();
      }

      public static float[] getRotationsToEntity(Entity entity) {
         return RotationUtils.getRotations(entity.entity);
      }

      public static void sendPacket(CPacket packet) {
         Packet packet1 = PacketHandler.convertCPacket(packet);
         if (packet1 != null) {
            ScriptDefaults.mc.thePlayer.sendQueue.addToSendQueue(packet1);
         }
      }

      public static void sendPacketNoEvent(CPacket packet) {
         Packet packet1 = PacketHandler.convertCPacket(packet);
         if (packet1 != null) {
            PacketUtils.sendPacketNoEvent(packet1);
         }
      }

      public static boolean inFocus() {
         return ScriptDefaults.mc.inGameHasFocus;
      }

      public static void dropItem(boolean dropStack) {
         ScriptDefaults.mc.thePlayer.dropOneItem(dropStack);
      }

      public static void setMotion(double x, double y, double z) {
         ScriptDefaults.mc.thePlayer.motionX = x;
         ScriptDefaults.mc.thePlayer.motionY = y;
         ScriptDefaults.mc.thePlayer.motionZ = z;
      }

      public static void setSpeed(double speed) {
         Utils.setSpeed(speed);
      }

      public static void setForward(float forward) {
         ScriptDefaults.mc.thePlayer.movementInput.moveForward = forward;
      }

      public static void setStrafe(float strafe) {
         ScriptDefaults.mc.thePlayer.movementInput.moveStrafe = strafe;
      }

      public static String getServerIP() {
         return ScriptDefaults.mc.getCurrentServerData() != null && !ScriptDefaults.mc.isSingleplayer() ? ScriptDefaults.mc.getCurrentServerData().serverIP : "";
      }

      public static int[] getDisplaySize() {
         ScaledResolution scaledResolution = new ScaledResolution(ScriptDefaults.mc);
         return new int[]{scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
      }

      public static float getServerDirection(PlayerState state) {
         return state.yaw;
      }

      public static Object[] raycastBlock(double distance) {
         return raycastBlock(distance, ScriptDefaults.mc.thePlayer.rotationYaw, ScriptDefaults.mc.thePlayer.rotationPitch);
      }

      public static Object[] raycastBlock(double distance, float yaw, float pitch) {
         net.minecraft.util.Vec3 eyeVec = ScriptDefaults.mc.thePlayer.getPositionEyes(1.0F);
         net.minecraft.util.Vec3 lookVec = Utils.getLookVec(yaw, pitch);
         net.minecraft.util.Vec3 sumVec = eyeVec.addVector(
            lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance
         );
         MovingObjectPosition mop = ScriptDefaults.mc.theWorld.rayTraceBlocks(eyeVec, sumVec, false, false, false);
         if (mop != null && mop.typeOfHit == MovingObjectType.BLOCK) {
            Vec3 pos = new Vec3(mop.getBlockPos());
            Vec3 offset = new Vec3(mop.hitVec.xCoord - pos.x, mop.hitVec.yCoord - pos.y, mop.hitVec.zCoord - pos.z);
            return new Object[]{pos, offset, mop.sideHit.name()};
         } else {
            return null;
         }
      }

      public static Object[] raycastEntity(double distance) {
         return raycastEntity(distance, ScriptDefaults.mc.thePlayer.rotationYaw, ScriptDefaults.mc.thePlayer.rotationPitch);
      }

      public static Object[] raycastEntity(double distance, float yaw, float pitch) {
         net.minecraft.entity.Entity pointedEntity = null;
         MovingObjectPosition mop = ScriptDefaults.mc.thePlayer.rayTrace(distance, 1.0F);
         net.minecraft.util.Vec3 eyeVec = ScriptDefaults.mc.thePlayer.getPositionEyes(1.0F);
         net.minecraft.util.Vec3 lookVec = Utils.getLookVec(yaw, pitch);
         net.minecraft.util.Vec3 vec32 = eyeVec.addVector(
            lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance
         );
         net.minecraft.util.Vec3 vec33 = null;
         List list = ScriptDefaults.mc
            .theWorld
            .getEntitiesWithinAABBExcludingEntity(
               ScriptDefaults.mc.getRenderViewEntity(),
               ScriptDefaults.mc
                  .getRenderViewEntity()
                  .getEntityBoundingBox()
                  .addCoord(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance)
                  .expand(1.0, 1.0, 1.0)
            );
         double d2 = distance;

         for (int i = 0; i < list.size(); i++) {
            net.minecraft.entity.Entity entity = (net.minecraft.entity.Entity)list.get(i);
            if (entity instanceof EntityLivingBase && entity.canBeCollidedWith() && ((EntityLivingBase)entity).deathTime == 0) {
               float cbs = entity.getCollisionBorderSize();
               AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().expand(cbs, cbs, cbs);
               MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(eyeVec, vec32);
               if (axisalignedbb.isVecInside(eyeVec)) {
                  if (0.0 < d2 || d2 == 0.0) {
                     pointedEntity = entity;
                     vec33 = movingobjectposition == null ? eyeVec : movingobjectposition.hitVec;
                     d2 = 0.0;
                  }
               } else if (movingobjectposition != null) {
                  double d3 = eyeVec.distanceTo(movingobjectposition.hitVec);
                  if (d3 < d2 || d2 == 0.0) {
                     if (entity != ScriptDefaults.mc.getRenderViewEntity().ridingEntity || entity.canRiderInteract()) {
                        pointedEntity = entity;
                        vec33 = movingobjectposition.hitVec;
                        d2 = d3;
                     } else if (d2 == 0.0) {
                        pointedEntity = entity;
                        vec33 = movingobjectposition.hitVec;
                     }
                  }
               }
            }
         }

         if (pointedEntity == null || !(d2 < distance) && mop != null) {
            return null;
         }

         mop = new MovingObjectPosition(pointedEntity, vec33);
         Vec3 offset = new Vec3(
            mop.hitVec.xCoord - pointedEntity.posX,
            mop.hitVec.yCoord - pointedEntity.posY,
            mop.hitVec.zCoord - pointedEntity.posZ
         );
         return new Object[]{new Entity(mop.entityHit), offset, eyeVec.squareDistanceTo(mop.hitVec)};
      }

      public static boolean canPlaceBlock(ItemStack stack, Vec3 pos, String side) {
         return stack != null && stack.itemStack != null && stack.itemStack.getItem() != null && stack.isBlock
            ? ((ItemBlock)stack.itemStack.getItem())
               .canPlaceBlockOnSide(
                  ScriptDefaults.mc.theWorld,
                  Vec3.getBlockPos(pos),
                  Utils.getEnum(EnumFacing.class, side),
                  ScriptDefaults.mc.thePlayer,
                  stack.itemStack
               )
            : false;
      }

      public static boolean placeBlock(Vec3 targetPos, String side, Vec3 hitVec) {
         return ScriptDefaults.mc
            .playerController
            .onPlayerRightClick(
               ScriptDefaults.mc.thePlayer,
               ScriptDefaults.mc.theWorld,
               ScriptDefaults.mc.thePlayer.getHeldItem(),
               Vec3.getBlockPos(targetPos),
               Utils.getEnum(EnumFacing.class, side),
               Vec3.getVec3(hitVec)
            );
      }

      public static void enableMovementFix() {
         RotationHelper.get().forceMovementFix = true;
      }

      public static float[] getRotationsToBlock(Vec3 position) {
         return RotationUtils.getRotations(new BlockPos(position.x, position.y, position.z));
      }

      public static void setSprinting(boolean sprinting) {
         ScriptDefaults.mc.thePlayer.setSprinting(sprinting);
      }

      public static void swing() {
         ScriptDefaults.mc.thePlayer.swingItem();
      }

      public static long time() {
         return System.currentTimeMillis();
      }

      public static boolean isFriend(String username) {
         return Utils.isFriended(username);
      }

      public static boolean isEnemy(String username) {
         return Utils.isEnemy(username);
      }
   }

   public static class config {
      private static String CONFIG_DIR = ScriptDefaults.mc.mcDataDir + File.separator + "keystrokes" + File.separator + "script_config.txt";
      private static String SEPARATOR = ":";
      private static String SEPARATOR_FULL = SEPARATOR + " ";

      private static void ensureConfigFileExists() throws IOException {
         Path configPath = Paths.get(CONFIG_DIR);
         if (Files.notExists(configPath)) {
            Files.createDirectories(configPath.getParent());
            Files.createFile(configPath);
         }
      }

      public static boolean set(String key, String value) {
         if (key != null && !key.isEmpty()) {
            key = key.replace(SEPARATOR, "");
            String entry = key + SEPARATOR_FULL + value;

            try {
               ensureConfigFileExists();
               Path configPath = new File(CONFIG_DIR).toPath();
               List<String> lines = new ArrayList<>(Files.readAllLines(configPath));
               boolean keyExists = false;

               for (int i = 0; i < lines.size(); i++) {
                  String line = lines.get(i);
                  if (line.startsWith(key + SEPARATOR_FULL)) {
                     lines.set(i, entry);
                     keyExists = true;
                     break;
                  }
               }

               if (!keyExists) {
                  lines.add(entry);
               }

               Files.write(configPath, lines);
               return true;
            } catch (IOException ex) {
               return false;
            }
         } else {
            return false;
         }
      }

      public static String get(String key) {
         if (key != null && !key.isEmpty()) {
            try {
               ensureConfigFileExists();
               Path configPath = new File(CONFIG_DIR).toPath();

               for (String line : Files.readAllLines(configPath)) {
                  if (line.startsWith(key + SEPARATOR_FULL)) {
                     return line.substring((key + SEPARATOR_FULL).length());
                  }
               }
            } catch (IOException var5) {
            }

            return null;
         } else {
            return null;
         }
      }
   }

   public static class gl {
      public static void alpha(boolean alpha) {
         if (alpha) {
            GlStateManager.enableAlpha();
         } else {
            GlStateManager.disableAlpha();
         }
      }

      public static void begin(int mode) {
         GL11.glBegin(mode);
      }

      public static void blend(boolean blend) {
         if (blend) {
            GlStateManager.enableBlend();
         } else {
            GlStateManager.disableBlend();
         }
      }

      public static void color(float r, float g, float b, float a) {
         GlStateManager.color(r, g, b, a);
      }

      public static void cull(boolean cull) {
         if (cull) {
            GlStateManager.enableCull();
         } else {
            GlStateManager.disableCull();
         }
      }

      public static void depth(boolean depth) {
         if (depth) {
            GlStateManager.enableDepth();
         } else {
            GlStateManager.disableDepth();
         }
      }

      public static void depthMask(boolean depthMask) {
         GlStateManager.depthMask(depthMask);
      }

      public static void disable(int cap) {
         GL11.glDisable(cap);
      }

      public static void disableItemLighting() {
         RenderHelper.disableStandardItemLighting();
      }

      public static void enable(int cap) {
         GL11.glEnable(cap);
      }

      public static void enableItemLighting(boolean gui) {
         if (gui) {
            RenderHelper.enableGUIStandardItemLighting();
         } else {
            RenderHelper.enableStandardItemLighting();
         }
      }

      public static void resetColor() {
         GlStateManager.resetColor();
      }

      public static void end() {
         GL11.glEnd();
      }

      public static void lighting(boolean lighting) {
         if (lighting) {
            GlStateManager.enableLighting();
         } else {
            GlStateManager.disableLighting();
         }
      }

      public static void lineSmooth(boolean lineSmooth) {
         setGLEnable(2848, lineSmooth);
      }

      public static void lineWidth(float width) {
         GL11.glLineWidth(width);
      }

      public static void normal(float x, float y, float z) {
         GL11.glNormal3f(x, y, z);
      }

      public static void pop() {
         GL11.glPopMatrix();
      }

      public static void push() {
         GL11.glPushMatrix();
      }

      public static void rotate(float angle, float x, float y, float z) {
         GL11.glRotatef(angle, x, y, z);
      }

      public static void scale(float x, float y, float z) {
         GL11.glScalef(x, y, z);
      }

      public static void scissor(int x, int y, int width, int height) {
         GL11.glScissor(x, y, width, height);
      }

      public static void scissor(boolean scissor) {
         setGLEnable(3089, scissor);
      }

      public static void texture2d(boolean texture2d) {
         if (texture2d) {
            GlStateManager.enableTexture2D();
         } else {
            GlStateManager.disableTexture2D();
         }
      }

      public static void translate(float x, float y, float z) {
         GL11.glTranslatef(x, y, z);
      }

      public static void vertex2(float x, float y) {
         GL11.glVertex2f(x, y);
      }

      public static void vertex3(float x, float y, float z) {
         GL11.glVertex3f(x, y, z);
      }

      private static void setGLEnable(int cap, boolean enable) {
         if (enable) {
            GL11.glEnable(cap);
         } else {
            GL11.glDisable(cap);
         }
      }
   }

   public static class inventory {
      public static int getSlot() {
         return ScriptDefaults.mc.thePlayer.inventory.currentItem;
      }

      public static void setSlot(int slot) {
         ScriptDefaults.mc.thePlayer.inventory.currentItem = slot;
      }

      public static void click(int slot, int button, int mode) {
         ScriptDefaults.mc
            .playerController
            .windowClick(ScriptDefaults.mc.thePlayer.openContainer.windowId, slot, button, mode, ScriptDefaults.mc.thePlayer);
      }

      public static List<String> getBookContents() {
         if (ScriptDefaults.mc.currentScreen instanceof GuiScreenBook) {
            List<String> contents = new ArrayList<>();
            List<IChatComponent> bookContents = ((IAccessorGuiScreenBook)ScriptDefaults.mc.currentScreen).getBookContents();
            if (bookContents == null) {
               return contents;
            }

            int max = Math.min(128 / ScriptDefaults.mc.fontRendererObj.FONT_HEIGHT, bookContents.size());

            for (int line = 0; line < max; line++) {
               IChatComponent lineStr = bookContents.get(line);
               contents.add(lineStr.getUnformattedText());
            }

            if (!contents.isEmpty()) {
               return contents;
            }
         }

         return null;
      }

      public static String getChest() {
         if (ScriptDefaults.mc.thePlayer.openContainer instanceof ContainerChest) {
            ContainerChest chest = (ContainerChest)ScriptDefaults.mc.thePlayer.openContainer;
            return chest == null ? "" : chest.getLowerChestInventory().getDisplayName().getUnformattedText();
         } else {
            return "";
         }
      }

      public static String getContainer() {
         if (ScriptDefaults.mc.currentScreen instanceof GuiContainerCreative) {
            CreativeTabs creativetabs = CreativeTabs.creativeTabArray[((GuiContainerCreative)ScriptDefaults.mc.currentScreen).getSelectedTabIndex()];
            if (creativetabs != null) {
               return I18n.format(creativetabs.getTranslatedTabLabel(), new Object[0]);
            }
         } else if (ScriptDefaults.mc.currentScreen != null) {
            try {
               return ((IInventory)ReflectionUtils.containerInventoryPlayer
                     .get(ScriptDefaults.mc.currentScreen.getClass())
                     .get(ScriptDefaults.mc.currentScreen))
                  .getDisplayName()
                  .getUnformattedText();
            } catch (Exception var1) {
            }
         }

         return "";
      }

      public static int getSize() {
         return ScriptDefaults.mc.thePlayer.inventory.getSizeInventory();
      }

      public static int getChestSize() {
         if (ScriptDefaults.mc.thePlayer.openContainer instanceof ContainerChest) {
            ContainerChest chest = (ContainerChest)ScriptDefaults.mc.thePlayer.openContainer;
            return chest == null ? -1 : chest.getLowerChestInventory().getSizeInventory();
         } else {
            return -1;
         }
      }

      public static ItemStack getStackInSlot(int slot) {
         return ScriptDefaults.mc.thePlayer.inventory.getStackInSlot(slot) == null
            ? null
            : new ItemStack(ScriptDefaults.mc.thePlayer.inventory.getStackInSlot(slot), (byte)0);
      }

      public static ItemStack getStackInChestSlot(int slot) {
         if (ScriptDefaults.mc.thePlayer.openContainer instanceof ContainerChest) {
            ContainerChest chest = (ContainerChest)ScriptDefaults.mc.thePlayer.openContainer;
            return chest.getLowerChestInventory().getStackInSlot(slot) == null ? null : new ItemStack(chest.getLowerChestInventory().getStackInSlot(slot), (byte)0);
         } else {
            return null;
         }
      }

      public static ItemStack getStackInCraftingSlot(int slot) {
         if (ScriptDefaults.mc.thePlayer.openContainer instanceof ContainerWorkbench) {
            InventoryCrafting craftMatrix = ((ContainerWorkbench)ScriptDefaults.mc.thePlayer.openContainer).craftMatrix;
            return craftMatrix.getStackInSlot(slot) == null ? null : new ItemStack(craftMatrix.getStackInSlot(slot), (byte)0);
         } else {
            return null;
         }
      }

      public static ItemStack getCraftResult() {
         if (ScriptDefaults.mc.thePlayer.openContainer instanceof ContainerWorkbench) {
            IInventory craftResult = ((ContainerWorkbench)ScriptDefaults.mc.thePlayer.openContainer).craftResult;
            return craftResult.getStackInSlot(0) == null ? null : new ItemStack(craftResult.getStackInSlot(0), (byte)0);
         } else {
            return null;
         }
      }

      public static void open() {
         KeyBinding inventoryKey = ScriptDefaults.mc.gameSettings.keyBindInventory;
         int originalKeyCode = inventoryKey.getKeyCode();
         if (originalKeyCode == 0) {
            inventoryKey.setKeyCode(13);
            KeyBinding.resetKeyBindingArrayAndHash();
         }

         KeyBinding.setKeyBindState(inventoryKey.getKeyCode(), true);
         KeyBinding.onTick(inventoryKey.getKeyCode());
         KeyBinding.setKeyBindState(inventoryKey.getKeyCode(), false);
         if (originalKeyCode == 0) {
            inventoryKey.setKeyCode(0);
            KeyBinding.resetKeyBindingArrayAndHash();
         }
      }
   }

   public static class keybinds {
      public static int[] getMousePosition() {
         return new int[]{Mouse.getX(), Mouse.getY()};
      }

      public static boolean isPressed(String key) {
         KeyBinding keyBind = ReflectionUtils.keybinds.get(key);
         return keyBind != null && keyBind.isKeyDown();
      }

      public static void setPressed(String key, boolean pressed) {
         KeyBinding keyBind = ReflectionUtils.keybinds.get(key);
         if (keyBind != null) {
            KeyBinding.setKeyBindState(keyBind.getKeyCode(), pressed);
            if (pressed) {
               KeyBinding.onTick(keyBind.getKeyCode());
            }
         }
      }

      public static int getKeyCode(String key) {
         KeyBinding keyBind = ReflectionUtils.keybinds.get(key);
         return keyBind != null ? keyBind.getKeyCode() : -1;
      }

      public static int getKeyIndex(String key) {
         return Keyboard.getKeyIndex(key);
      }

      public static boolean isMouseDown(int mouseButton) {
         return Mouse.isButtonDown(mouseButton);
      }

      public static boolean isKeyDown(int key) {
         return Keyboard.isKeyDown(key);
      }

      public static void rightClick() {
         ((IAccessorMinecraft)ScriptDefaults.mc).callRightClickMouse();
      }

      public static void leftClick() {
         ((IAccessorMinecraft)ScriptDefaults.mc).callClickMouse();
      }

      public static int getScroll() {
         return Mouse.getDWheel();
      }
   }

   public static class modules {
      private String superName;

      public modules(String superName) {
         this.superName = superName;
      }

      private static Module getModule(String moduleName) {
         return ScriptDefaults.modulesMap.get(moduleName);
      }

      private static Module getScript(String name) {
         return ScriptDefaults.modulesMap.get(name);
      }

      private static Setting getSetting(Module module, String settingName) {
         if (module == null) {
            return null;
         }

         for (Setting setting : module.getSettings()) {
            if (setting.getName().equals(settingName)) {
               return setting;
            }
         }

         return null;
      }

      private GroupSetting getGroupForString(String group) {
         if (group.isEmpty()) {
            return null;
         }

         for (Setting setting : getScript(this.superName).getSettings()) {
            if (setting instanceof GroupSetting && setting.getName().equals(group)) {
               return (GroupSetting)setting;
            }
         }

         return null;
      }

      public void enable(String moduleName) {
         if (getModule(moduleName) != null) {
            getModule(moduleName).enable();
         }
      }

      public void disable(String moduleName) {
         if (getModule(moduleName) != null) {
            getModule(moduleName).disable();
         }
      }

      public boolean isEnabled(String moduleName) {
         return getModule(moduleName) == null ? false : getModule(moduleName).isEnabled();
      }

      public Entity getKillAuraTarget() {
         return KillAura.target == null ? null : Entity.convert(KillAura.target);
      }

      public Map<String, Object> getSettings(String name) {
         Map<String, Object> settings = new HashMap<>();
         Module module = getModule(name);
         if (module == null) {
            return settings;
         }

         for (Setting setting : module.getSettings()) {
            if (setting instanceof SliderSetting) {
               settings.put(setting.getName(), ((SliderSetting)setting).getInput());
            } else if (setting instanceof ButtonSetting) {
               settings.put(setting.getName(), ((ButtonSetting)setting).isToggled());
            }
         }

         return settings;
      }

      public Map<String, List<String>> getCategories() {
         Map<String, List<String>> categories = new HashMap<>();

         for (CategoryComponent categoryComponent : ClickGui.categories) {
            List<String> modules = new ArrayList<>();

            for (ModuleComponent module : categoryComponent.modules) {
               modules.add(module.mod.getName());
            }

            categories.put(categoryComponent.category.name(), modules);
         }

         return categories;
      }

      public Vec3 getBedAuraPosition() {
         BlockPos blockPos = ModuleManager.bedAura.currentBlock;
         return ModuleManager.bedAura != null && ModuleManager.bedAura.isEnabled() && ModuleManager.bedAura.currentBlock != null
            ? new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ())
            : null;
      }

      public boolean isScaffolding() {
         return ModuleManager.scaffold.isEnabled();
      }

      public boolean isTowering() {
         return ModuleManager.tower.canTower();
      }

      public boolean isHidden(String moduleName) {
         Module module = getModule(moduleName);
         return module != null ? module.isHidden() : false;
      }

      public float[] getBedAuraProgress() {
         return ModuleManager.bedAura != null && ModuleManager.bedAura.isEnabled()
            ? new float[]{ModuleManager.bedAura.breakProgress, ModuleManager.bedAura.vanillaProgress}
            : new float[]{0.0F, 0.0F};
      }

      public void registerGroup(String name) {
         getScript(this.superName).registerSetting(new GroupSetting(name));
      }

      public void registerButton(String name, boolean defaultValue) {
         getScript(this.superName).registerSetting(new ButtonSetting(name, defaultValue));
      }

      public void registerButton(String group, String name, boolean defaultValue) {
         getScript(this.superName).registerSetting(new ButtonSetting(this.getGroupForString(group), name, defaultValue));
      }

      public void registerKey(String group, String name, int defaultKey) {
         getScript(this.superName).registerSetting(new KeySetting(this.getGroupForString(group), name, defaultKey));
      }

      public void registerKey(String name, int defaultKey) {
         getScript(this.superName).registerSetting(new KeySetting(name, defaultKey));
      }

      public void registerSlider(String group, String name, String suffix, double defaultValue, double minimum, double maximum, double interval) {
         getScript(this.superName).registerSetting(new SliderSetting(this.getGroupForString(group), name, suffix, defaultValue, minimum, maximum, interval));
      }

      public void registerSlider(String group, String name, String suffix, int defaultValue, String[] stringArray) {
         getScript(this.superName).registerSetting(new SliderSetting(this.getGroupForString(group), name, suffix, defaultValue, stringArray));
      }

      public void registerSlider(String name, double defaultValue, double minimum, double maximum, double interval) {
         this.registerSlider("", name, "", defaultValue, minimum, maximum, interval);
      }

      public void registerSlider(String name, int defaultValue, String[] stringArray) {
         this.registerSlider("", name, "", defaultValue, stringArray);
      }

      public void registerSlider(String name, String suffix, double defaultValue, double minimum, double maximum, double interval) {
         this.registerSlider("", name, suffix, defaultValue, minimum, maximum, interval);
      }

      public void registerSlider(String name, String suffix, int defaultValue, String[] stringArray) {
         this.registerSlider("", name, suffix, defaultValue, stringArray);
      }

      public void registerDescription(String description) {
         getScript(this.superName).registerSetting(new DescriptionSetting(description));
      }

      public boolean getButton(String moduleName, String name) {
         ButtonSetting setting = (ButtonSetting)getSetting(getModule(moduleName), name);
         return setting == null ? false : setting.isToggled();
      }

      public double getSlider(String moduleName, String name) {
         SliderSetting setting = (SliderSetting)getSetting(getModule(moduleName), name);
         return setting == null ? 0.0 : setting.getInput();
      }

      public boolean getKeyPressed(String moduleName, String name) {
         KeySetting setting = (KeySetting)getSetting(getModule(moduleName), name);
         return setting == null ? false : setting.isPressed();
      }

      public void setButton(String moduleName, String name, boolean value) {
         ButtonSetting setting = (ButtonSetting)getSetting(getModule(moduleName), name);
         if (setting != null) {
            setting.setEnabled(value);
         }
      }

      public void setSlider(String moduleName, String name, double value) {
         SliderSetting setting = (SliderSetting)getSetting(getModule(moduleName), name);
         if (setting != null) {
            setting.setValueRawWithEvent(value);
         }
      }

      public void setKey(String moduleName, String name, int code) {
         KeySetting setting = (KeySetting)getSetting(getModule(moduleName), name);
         if (setting != null) {
            setting.setKey(code);
         }
      }
   }

   public static class render {
      private static final IntBuffer VIEWPORT = GLAllocation.createDirectIntBuffer(16);
      private static final FloatBuffer MODELVIEW = GLAllocation.createDirectFloatBuffer(16);
      private static final FloatBuffer PROJECTION = GLAllocation.createDirectFloatBuffer(16);
      private static final FloatBuffer SCREEN_COORDS = GLAllocation.createDirectFloatBuffer(3);

      public static void block(Vec3 position, int color, boolean outline, boolean shade) {
         RenderUtils.renderBlock(new BlockPos(position.x, position.y, position.z), color, outline, shade);
      }

      public static void block(int x, int y, int z, int color, boolean outline, boolean shade) {
         RenderUtils.renderBlock(new BlockPos(x, y, z), color, outline, shade);
      }

      public static void entity(Entity en, int color, float partialTicks, boolean outline, boolean shade) {
         net.minecraft.entity.Entity e = en.entity;
         double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks - ScriptDefaults.mc.getRenderManager().viewerPosX;
         double y = e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks - ScriptDefaults.mc.getRenderManager().viewerPosY;
         double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks - ScriptDefaults.mc.getRenderManager().viewerPosZ;
         AxisAlignedBB bbox = e.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
         AxisAlignedBB axis = new AxisAlignedBB(
            bbox.minX - e.posX + x,
            bbox.minY - e.posY + y,
            bbox.minZ - e.posZ + z,
            bbox.maxX - e.posX + x,
            bbox.maxY - e.posY + y,
            bbox.maxZ - e.posZ + z
         );
         GL11.glPushMatrix();
         GL11.glBlendFunc(770, 771);
         GL11.glEnable(3042);
         GL11.glDisable(3553);
         GL11.glDisable(2929);
         GL11.glDepthMask(false);
         GL11.glLineWidth(2.0F);
         float a = (color >> 24 & 0xFF) / 255.0F;
         float r = (color >> 16 & 0xFF) / 255.0F;
         float g = (color >> 8 & 0xFF) / 255.0F;
         float b = (color & 0xFF) / 255.0F;
         GL11.glColor4f(r, g, b, a);
         if (outline) {
            RenderGlobal.drawSelectionBoundingBox(axis);
         }

         if (shade) {
            RenderUtils.drawBoundingBox(axis, r, g, b);
         }

         GL11.glEnable(3553);
         GL11.glEnable(2929);
         GL11.glDepthMask(true);
         GL11.glDisable(3042);
         GL11.glPopMatrix();
      }

      public static void entityGui(Entity en, int x, int y, float mouseX, float mouseY, int scale) {
         if (en.isLiving) {
            GL11.glPushMatrix();
            GuiInventory.drawEntityOnScreen(x, y, scale, mouseX, mouseY, (EntityLivingBase)en.entity);
            GL11.glPopMatrix();
         }
      }

      public static void resetEquippedProgress() {
         ScriptDefaults.mc.getItemRenderer().resetEquippedProgress();
      }

      public static void tracer(Entity entity, float lineWidth, int color, float partialTicks) {
         RenderUtils.drawTracerLine(entity.entity, color, lineWidth, partialTicks);
      }

      public static void showGui() {
         ScriptDefaults.mc.displayGuiScreen(new ScriptDefaults.render.EmptyGuiScreen());
      }

      public static void item(ItemStack item, float x, float y, float scale) {
         GlStateManager.pushMatrix();
         ScriptDefaults.mc.entityRenderer.setupOverlayRendering();
         if (scale != 1.0F) {
            GlStateManager.scale(scale, scale, scale);
         }

         RenderHelper.enableGUIStandardItemLighting();
         GlStateManager.disableBlend();
         GlStateManager.translate(x / scale, y / scale, 0.0F);
         ScriptDefaults.mc.getRenderItem().renderItemIntoGUI(item.itemStack, 0, 0);
         GlStateManager.enableBlend();
         RenderHelper.disableStandardItemLighting();
         if (scale != 1.0F) {
            GlStateManager.scale(scale, scale, scale);
         }

         GlStateManager.popMatrix();
      }

      public static void image(Image image, float x, float y, float width, float height) {
         if (image != null && image.isLoaded()) {
            if (image.textureId == -1) {
               DynamicTexture dynamicTexture = new DynamicTexture(image.bufferedImage);
               GL11.glTexParameteri(3553, 10240, 9728);
               dynamicTexture.updateDynamicTexture();
               image.textureId = dynamicTexture.getGlTextureId();
            }

            GlStateManager.pushMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(image.textureId);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glTexParameteri(3553, 10240, 9728);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            worldrenderer.pos(x, y + height, 0.0).tex(0.0, 1.0).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos(x + width, y + height, 0.0).tex(1.0, 1.0).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos(x + width, y, 0.0).tex(1.0, 0.0).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos(x, y, 0.0).tex(0.0, 0.0).color(255, 255, 255, 255).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
         }
      }

      public static Vec3 worldToScreen(double x, double y, double z, int scaleFactor, float partialTicks) {
         x -= ScriptDefaults.mc.getRenderManager().viewerPosX;
         y -= ScriptDefaults.mc.getRenderManager().viewerPosY;
         z -= ScriptDefaults.mc.getRenderManager().viewerPosZ;
         ((IAccessorEntityRenderer)ScriptDefaults.mc.entityRenderer)
            .callSetupCameraTransform(((IAccessorMinecraft)ScriptDefaults.mc).getTimer().renderPartialTicks, 0);
         GL11.glGetFloat(2982, MODELVIEW);
         GL11.glGetFloat(2983, PROJECTION);
         GL11.glGetInteger(2978, VIEWPORT);
         if (GLU.gluProject((float)x, (float)y, (float)z, MODELVIEW, PROJECTION, VIEWPORT, SCREEN_COORDS)) {
            Vec3 vec = new Vec3(SCREEN_COORDS.get(0) / scaleFactor, (Display.getHeight() - SCREEN_COORDS.get(1)) / scaleFactor, SCREEN_COORDS.get(2));
            ScriptDefaults.mc.entityRenderer.setupOverlayRendering();
            return vec;
         } else {
            return null;
         }
      }

      public static void roundedRect(float startX, float startY, float endX, float endY, float radius, int color) {
         RoundedUtils.drawRoundedRectRise(startX, startY, Math.abs(startX - endX), Math.abs(startY - endY), radius, color);
      }

      public static void gradientRect(float startX, float startY, float endX, float endY, int leftColor, int rightColor) {
         gradientRect(startX, startY, endX, endY, leftColor, leftColor, rightColor, rightColor);
      }

      public static void gradientRect(
         float startX, float startY, float endX, float endY, int topLeftColor, int bottomLeftColor, int topRightColor, int bottomRightColor
      ) {
         RenderUtils.drawRoundedGradientRect(startX, startY, endX, endY, 0.0F, topLeftColor, bottomLeftColor, topRightColor, bottomRightColor);
      }

      public static double[] getRotations() {
         return new double[]{ScriptDefaults.mc.getRenderManager().playerViewY, ScriptDefaults.mc.getRenderManager().playerViewX};
      }

      public static double[] getCameraRotations() {
         return new double[]{Utils.getCameraYaw(), Utils.getCameraPitch()};
      }

      public static int getFontWidth(String text) {
         return ScriptDefaults.mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text);
      }

      public static int getFontHeight() {
         return ScriptDefaults.mc.fontRendererObj.FONT_HEIGHT;
      }

      public static Vec3 getPosition() {
         net.minecraft.util.Vec3 position = Utils.getCameraPos(((IAccessorMinecraft)ScriptDefaults.mc).getTimer().renderPartialTicks);
         return new Vec3(position);
      }

      public static void text2d(String text, float x, float y, float scale, int color, boolean shadow) {
         GlStateManager.pushMatrix();
         if (scale != 1.0F) {
            GlStateManager.scale(scale, scale, scale);
         }

         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         ScriptDefaults.mc.fontRendererObj.drawString(text, x / scale, y / scale, color, shadow);
         GlStateManager.disableBlend();
         if (scale != 1.0F) {
            GlStateManager.scale(1.0F, 1.0F, 1.0F);
         }

         GlStateManager.popMatrix();
      }

      public static void text3d(String text, Vec3 position, float scale, boolean shadow, boolean depth, boolean background, int color) {
         ((IAccessorEntityRenderer)ScriptDefaults.mc.entityRenderer)
            .callSetupCameraTransform(((IAccessorMinecraft)ScriptDefaults.mc).getTimer().renderPartialTicks, 0);
         GlStateManager.pushMatrix();
         float partialTicks = ((IAccessorMinecraft)ScriptDefaults.mc).getTimer().renderPartialTicks;
         double px = ScriptDefaults.mc.thePlayer.prevPosX
            + (ScriptDefaults.mc.thePlayer.posX - ScriptDefaults.mc.thePlayer.prevPosX) * partialTicks;
         double py = ScriptDefaults.mc.thePlayer.prevPosY
            + (ScriptDefaults.mc.thePlayer.posY - ScriptDefaults.mc.thePlayer.prevPosY) * partialTicks;
         double pz = ScriptDefaults.mc.thePlayer.prevPosZ
            + (ScriptDefaults.mc.thePlayer.posZ - ScriptDefaults.mc.thePlayer.prevPosZ) * partialTicks;
         GlStateManager.translate((float)position.x - px, (float)position.y - py, (float)position.z - pz);
         GL11.glNormal3f(0.0F, 1.0F, 0.0F);
         GlStateManager.rotate(-ScriptDefaults.mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
         GlStateManager.rotate(ScriptDefaults.mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
         float f1 = 0.02666667F;
         GlStateManager.scale(-f1, -f1, f1);
         if (depth) {
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
         }

         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         if (background) {
            GlStateManager.disableTexture2D();
            int width = ScriptDefaults.mc.fontRendererObj.getStringWidth(text);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldrenderer.pos(-1.0, -1.0, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos(-1.0, 8.0, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos(width + 1, 8.0, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos(width + 1, -1.0, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
         }

         if (scale != 1.0F) {
            GlStateManager.scale(scale, scale, scale);
         }

         ScriptDefaults.mc.fontRendererObj.drawString(text, 0.0F, 0.0F, color, shadow);
         if (scale != 1.0F) {
            GlStateManager.scale(1.0F, 1.0F, 1.0F);
         }

         if (depth) {
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
         }

         GlStateManager.disableBlend();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.popMatrix();
      }

      public static void rect(float startX, float startY, float endX, float endY, int color) {
         if (startX < endX) {
            float i = startX;
            startX = endX;
            endX = i;
         }

         if (startY < endY) {
            float j = startY;
            startY = endY;
            endY = j;
         }

         float f3 = (color >> 24 & 0xFF) / 255.0F;
         float f4 = (color >> 16 & 0xFF) / 255.0F;
         float f5 = (color >> 8 & 0xFF) / 255.0F;
         float f6 = (color & 0xFF) / 255.0F;
         Tessellator tessellator = Tessellator.getInstance();
         WorldRenderer worldrenderer = tessellator.getWorldRenderer();
         GL11.glPushMatrix();
         GlStateManager.enableBlend();
         GlStateManager.disableTexture2D();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.color(f4, f5, f6, f3);
         worldrenderer.begin(7, DefaultVertexFormats.POSITION);
         worldrenderer.pos(startX, endY, 0.0).endVertex();
         worldrenderer.pos(endX, endY, 0.0).endVertex();
         worldrenderer.pos(endX, startY, 0.0).endVertex();
         worldrenderer.pos(startX, startY, 0.0).endVertex();
         tessellator.draw();
         GlStateManager.enableTexture2D();
         GlStateManager.disableBlend();
         GL11.glPopMatrix();
      }

      public static void line2D(double startX, double startY, double endX, double endY, float lineWidth, int color) {
         GL11.glPushMatrix();
         GL11.glEnable(2848);
         GL11.glDisable(3553);
         GL11.glEnable(3042);
         RenderUtils.glColor(color);
         GL11.glLineWidth(lineWidth);
         GL11.glBegin(1);
         GL11.glVertex2d(startX, startY);
         GL11.glVertex2d(endX, endY);
         GL11.glEnd();
         GL11.glDisable(3042);
         GL11.glEnable(3553);
         GL11.glDisable(2848);
         GL11.glPopMatrix();
      }

      public static void line3D(Vec3 pos1, Vec3 pos2, float lineWidth, int color) {
         line3D(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z, lineWidth, color);
      }

      public static void line3D(double startX, double startY, double startZ, double endX, double endY, double endZ, float lineWidth, int color) {
         endX -= ScriptDefaults.mc.getRenderManager().viewerPosX;
         endY -= ScriptDefaults.mc.getRenderManager().viewerPosY;
         endZ -= ScriptDefaults.mc.getRenderManager().viewerPosZ;
         float a = (color >> 24 & 0xFF) / 255.0F;
         float r = (color >> 16 & 0xFF) / 255.0F;
         float g = (color >> 8 & 0xFF) / 255.0F;
         float b = (color & 0xFF) / 255.0F;
         GL11.glPushMatrix();
         GL11.glEnable(3042);
         GL11.glEnable(2848);
         GL11.glDisable(2929);
         GL11.glDisable(3553);
         GL11.glBlendFunc(770, 771);
         GL11.glLineWidth(lineWidth);
         GlStateManager.color(r, g, b, a);
         GL11.glBegin(2);
         GL11.glVertex3d(
            startX - ScriptDefaults.mc.thePlayer.posX,
            startY - ScriptDefaults.mc.thePlayer.posY,
            startZ - ScriptDefaults.mc.thePlayer.posZ
         );
         GL11.glVertex3d(endX, endY, endZ);
         GL11.glEnd();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glEnable(3553);
         GL11.glEnable(2929);
         GL11.glDisable(2848);
         GL11.glDisable(3042);
         GL11.glPopMatrix();
      }

      public static boolean isInView(Entity en) {
         return RenderUtils.isInViewFrustum(en.entity);
      }

      private static class EmptyGuiScreen extends GuiScreen {
         private EmptyGuiScreen() {
         }
      }

      public static class bloom {
         public static void prepare() {
            BlurUtils.prepareBloom();
         }

         public static void apply(int passes, float radius) {
            BlurUtils.bloomEnd(passes, radius);
         }
      }

      public static class blur {
         public static void prepare() {
            BlurUtils.prepareBlur();
         }

         public static void apply(int passes, float radius) {
            BlurUtils.blurEnd(passes, radius);
         }
      }
   }

   public static class util {
      public static final String colorSymbol = "§";

      public static String color(String message) {
         return Utils.formatColor(message);
      }

      public static String strip(String string) {
         return Utils.stripColor(string);
      }

      public static double round(double value, int decimals) {
         return Utils.round(value, decimals);
      }

      public static int randomInt(int min, int max) {
         return Utils.randomizeInt(min, max);
      }

      public static double randomDouble(double min, double max) {
         return Utils.randomizeDouble(min, max);
      }
   }

   public static class world {
      public static Block getBlockAt(int x, int y, int z) {
         IBlockState state = BlockUtils.getBlockState(new BlockPos(x, y, z));
         return state == null ? new Block(Blocks.air, new BlockPos(x, y, z)) : new Block(state, new BlockPos(x, y, z));
      }

      public static Block getBlockAt(Vec3 pos) {
         IBlockState state = BlockUtils.getBlockState(new BlockPos(pos.x, pos.y, pos.z));
         return state == null ? new Block(Blocks.air, new BlockPos(pos.x, pos.y, pos.z)) : new Block(state, new BlockPos(pos.x, pos.y, pos.z));
      }

      public static String getDimension() {
         return ScriptDefaults.mc.theWorld == null ? "" : ScriptDefaults.mc.theWorld.provider.getDimensionName();
      }

      public static List<Entity> getEntities() {
         List<Entity> entities = new ArrayList<>();
         if (ScriptDefaults.mc.theWorld == null) {
            return entities;
         }

         for (net.minecraft.entity.Entity entity : ScriptDefaults.mc.theWorld.loadedEntityList) {
            entities.add(Entity.convert(entity));
         }

         return entities;
      }

      public static Entity getEntityById(int entityId) {
         return ScriptDefaults.mc.theWorld == null ? null : Entity.convert(ScriptDefaults.mc.theWorld.getEntityByID(entityId));
      }

      public static List<NetworkPlayer> getNetworkPlayers() {
         List<NetworkPlayer> entities = new ArrayList<>();

         for (NetworkPlayerInfo networkPlayerInfo : Utils.getTablist(false)) {
            entities.add(NetworkPlayer.convert(networkPlayerInfo));
         }

         return entities;
      }

      public static List<Entity> getPlayerEntities() {
         List<Entity> entities = new ArrayList<>();

         for (net.minecraft.entity.Entity entity : ScriptDefaults.mc.theWorld.playerEntities) {
            entities.add(Entity.convert(entity));
         }

         return entities;
      }

      public static List<String> getScoreboard() {
         List<String> sidebarLines = Utils.getSidebarLines();
         return sidebarLines.isEmpty() ? null : sidebarLines;
      }

      public static String getTabHeader() {
         if (ScriptDefaults.mc != null && ScriptDefaults.mc.ingameGUI != null && ScriptDefaults.mc.ingameGUI.getTabList() != null) {
            IChatComponent header = ((IAccessorGuiPlayerTabOverlay)ScriptDefaults.mc.ingameGUI.getTabList()).getHeader();
            return header != null ? header.getUnformattedText() : "";
         } else {
            return "";
         }
      }

      public static String getTabFooter() {
         if (ScriptDefaults.mc != null && ScriptDefaults.mc.ingameGUI != null && ScriptDefaults.mc.ingameGUI.getTabList() != null) {
            IChatComponent footer = ((IAccessorGuiPlayerTabOverlay)ScriptDefaults.mc.ingameGUI.getTabList()).getFooter();
            return footer != null ? footer.getUnformattedText() : "";
         } else {
            return "";
         }
      }

      public static Map<String, List<String>> getTeams() {
         Map<String, List<String>> teams = new HashMap<>();

         for (Team team : ScriptDefaults.mc.theWorld.getScoreboard().getTeams()) {
            List<String> members = new ArrayList<>();

            for (String member : team.getMembershipCollection()) {
               members.add(member);
            }

            teams.put(team.getRegisteredName(), members);
         }

         return teams;
      }

      public static List<TileEntity> getTileEntities() {
         List<TileEntity> tileEntities = new ArrayList<>();

         for (net.minecraft.tileentity.TileEntity entity : ScriptDefaults.mc.theWorld.loadedTileEntityList) {
            tileEntities.add(new TileEntity(entity));
         }

         return tileEntities;
      }
   }
}
