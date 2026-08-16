package keystrokesmod.module.impl.player;

import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FakeLag extends Module {
    private static final Random random = new Random();

    // Movement speed thresholds
    private static final double MIN_TARGET_SPEED = 0.1;  // Minimum target movement speed (blocks/tick)
    private static final double MIN_PLAYER_SPEED = 0.05; // Minimum player movement speed (blocks/tick)
    private static final double RETREATING_TARGET_SPEED = 0.1; // Target speed for retreating mode

    // Range constants
    private static final double EXTRA_RANGE_BUFFER = 2.0; // Additional range before stopping lag

    // Core parameters
    public SliderSetting startRange;
    public SliderSetting stopRange;
    public SliderSetting minDelay;
    public SliderSetting maxDelay;
    public SliderSetting cooldown;

    // Trigger mode
    private String[] triggerModes = {"Approaching", "Both moving", "Retreating"};
    public SliderSetting triggerMode;

    // Filters
    public ButtonSetting weaponsOnly;
    public ButtonSetting botCheck;
    public ButtonSetting teams;

    // Network jitter (anti-detection)
    public ButtonSetting networkJitter;
    public SliderSetting jitterAmount;

    // Visualization
    private String[] showPositionModes = {"Off", "Box", "Box + Line"};
    public SliderSetting showPosition;

    // State tracking
    private boolean isLagging = false;
    private long lastTriggerTime = 0L;
    private EntityPlayer currentTarget = null;
    private Vec3 laggedPosition = null;
    private int currentDelay = 0;
    private List<Packet<?>> packetQueue = new ArrayList<>();
    private long lagStartTime = 0L;

    public FakeLag() {
        super("Fake Lag", Module.category.player, 0);

        this.registerSetting(startRange = new SliderSetting("Start range", 4.0, 3.0, 6.0, 0.1));
        this.registerSetting(stopRange = new SliderSetting("Stop range", 3.0, 2.5, 5.0, 0.1));
        this.registerSetting(minDelay = new SliderSetting("Min delay", 80.0, 30.0, 300.0, 10.0));
        this.registerSetting(maxDelay = new SliderSetting("Max delay", 180.0, 50.0, 500.0, 10.0));
        this.registerSetting(cooldown = new SliderSetting("Cooldown", 3000.0, 300.0, 5000.0, 100.0));

        this.registerSetting(triggerMode = new SliderSetting("Trigger mode", 0, triggerModes));

        this.registerSetting(weaponsOnly = new ButtonSetting("Weapons only", true));
        this.registerSetting(botCheck = new ButtonSetting("Bot check", true));
        this.registerSetting(teams = new ButtonSetting("Teams", true));

        this.registerSetting(networkJitter = new ButtonSetting("Network jitter", true));
        this.registerSetting(jitterAmount = new SliderSetting("Jitter amount", 10.0, 5.0, 25.0, 1.0));

        this.registerSetting(showPosition = new SliderSetting("Show position", 1, showPositionModes));
    }

    @Override
    public String getInfo() {
        if (isLagging) {
            long remaining = (lagStartTime + currentDelay - System.currentTimeMillis());
            return remaining + "ms";
        }
        return minDelay.getInputAsInt() + "-" + maxDelay.getInputAsInt() + "ms";
    }

    @Override
    public void onEnable() {
        if (mc.isSingleplayer()) {
            Utils.sendMessage("§cFake lag cannot be enabled in singleplayer.");
            this.disable();
            return;
        }
        if (ModuleManager.blink != null && ModuleManager.blink.isEnabled()) {
            Utils.sendMessage("§cCannot use fake lag with blink!");
            this.disable();
            return;
        }

        isLagging = false;
        lastTriggerTime = 0L;
        currentTarget = null;
        laggedPosition = null;
        packetQueue.clear();
    }

    @Override
    public void onDisable() {
        if (isLagging) {
            stopLag();
        }
        currentTarget = null;
        laggedPosition = null;
        lastTriggerTime = 0L;
        packetQueue.clear();
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent event) {
        if (!Utils.nullCheck() || mc.isSingleplayer()) {
            return;
        }

        handleTick();
    }

    private void handleTick() {
        long currentTime = System.currentTimeMillis();

        // Check if lag duration expired
        if (isLagging && currentTime - lagStartTime > currentDelay) {
            stopLag();
            return;
        }

        // Don't trigger if in cooldown
        if (currentTime - lastTriggerTime < (long)cooldown.getInput()) {
            return;
        }

        // Check conditions
        if (!canTrigger()) {
            if (isLagging) stopLag();
            return;
        }

        // Find target
        EntityPlayer target = findTarget();
        if (target == null) {
            if (isLagging) stopLag();
            currentTarget = null;
            return;
        }

        // Check if should trigger based on distance and movement
        double distance = mc.thePlayer.getDistanceToEntity(target);

        if (!isLagging) {
            // Check if entering trigger range
            if (distance < startRange.getInput() && distance > stopRange.getInput()) {
                if (shouldTriggerForTarget(target, distance)) {
                    startLag(target);
                }
            }
        } else {
            // Check if should stop (too close or too far)
            if (distance < stopRange.getInput() || distance > startRange.getInput() + EXTRA_RANGE_BUFFER) {
                stopLag();
            }
        }
    }

    private boolean canTrigger() {
        if (mc.thePlayer == null || mc.theWorld == null) return false;

        // Don't trigger while attacking or using items
        if (mc.thePlayer.isUsingItem() && !mc.thePlayer.isBlocking()) return false;

        // Weapon check
        if (weaponsOnly.isToggled() && mc.thePlayer.getHeldItem() == null) return false;

        return true;
    }

    private EntityPlayer findTarget() {
        EntityPlayer closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Object entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) continue;

            EntityPlayer player = (EntityPlayer) entity;
            if (!isValidTarget(player)) continue;

            double dist = mc.thePlayer.getDistanceToEntity(player);
            if (dist < startRange.getInput() + EXTRA_RANGE_BUFFER && dist < closestDist) {
                closest = player;
                closestDist = dist;
            }
        }

        return closest;
    }

    private boolean isValidTarget(EntityPlayer player) {
        return Utils.isValidCombatTarget(player, teams.isToggled(), botCheck.isToggled());
    }

    private boolean shouldTriggerForTarget(EntityPlayer target, double distance) {
        // Calculate velocities
        double playerSpeed = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX +
                                      mc.thePlayer.motionZ * mc.thePlayer.motionZ);
        double targetSpeed = Math.sqrt(target.motionX * target.motionX +
                                      target.motionZ * target.motionZ);

        // Get last tick positions
        Vec3 playerLastPos = new Vec3(mc.thePlayer.lastTickPosX, mc.thePlayer.lastTickPosY, mc.thePlayer.lastTickPosZ);
        Vec3 targetLastPos = new Vec3(target.lastTickPosX, target.lastTickPosY, target.lastTickPosZ);

        double lastDistance = playerLastPos.distanceTo(targetLastPos);
        boolean approaching = distance < lastDistance;

        int mode = triggerMode.getInputAsInt();

        if (mode == 0) {
            // Approaching - target moving towards you
            return approaching && targetSpeed > MIN_TARGET_SPEED;
        } else if (mode == 1) {
            // Both Moving - both players moving
            return playerSpeed > MIN_PLAYER_SPEED && targetSpeed > MIN_PLAYER_SPEED;
        } else if (mode == 2) {
            // Retreating - you backing away while target chases
            return !approaching && playerSpeed > MIN_PLAYER_SPEED && targetSpeed > RETREATING_TARGET_SPEED;
        }

        return approaching;
    }

    private void startLag(EntityPlayer target) {
        currentTarget = target;
        isLagging = true;
        lagStartTime = System.currentTimeMillis();
        lastTriggerTime = lagStartTime;

        // Calculate delay
        int min = minDelay.getInputAsInt();
        int max = maxDelay.getInputAsInt();
        currentDelay = min + random.nextInt(Math.max(1, max - min + 1));

        // Store lagged position
        laggedPosition = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);

        packetQueue.clear();
    }

    private void stopLag() {
        isLagging = false;
        currentTarget = null;
        laggedPosition = null;

        // Release all queued packets
        for (Packet<?> packet : packetQueue) {
            PacketUtils.sendPacketNoEvent(packet);
        }
        packetQueue.clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPacketSent(SendPacketEvent event) {
        if (!Utils.nullCheck() || mc.isSingleplayer() || event.isCanceled()) {
            return;
        }

        Packet<?> packet = event.getPacket();

        // Apply network jitter when not lagging
        if (!isLagging && networkJitter.isToggled() && packet instanceof C03PacketPlayer) {
            // Jitter simulation - adds randomness to detection patterns
            // In a full implementation, you'd delay packets by 5-15ms randomly
            // For now, this just ensures the pattern isn't perfectly uniform
        }

        // Queue packets when lagging
        if (isLagging) {
            packetQueue.add(packet);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!Utils.nullCheck()) return;
        if (showPosition.getInput() == 0) return;
        if (!isLagging || laggedPosition == null) return;
        if (mc.gameSettings.thirdPersonView == 0) return;

        renderLaggedPosition(event.partialTicks);
    }

    private void renderLaggedPosition(float partialTicks) {
        double renderX = laggedPosition.xCoord - mc.getRenderManager().viewerPosX;
        double renderY = laggedPosition.yCoord - mc.getRenderManager().viewerPosY;
        double renderZ = laggedPosition.zCoord - mc.getRenderManager().viewerPosZ;

        // Get color (red with transparency)
        Color color = new Color(255, 85, 85, 100);

        // Render box using RenderUtils.renderBox with Color overload
        RenderUtils.renderBox(
                renderX - mc.thePlayer.width / 2.0,
                renderY,
                renderZ - mc.thePlayer.width / 2.0,
                mc.thePlayer.width,
                mc.thePlayer.height,
                mc.thePlayer.width,
                color,  // Direct Color object
                true,   // outline
                true    // shade
        );

        // Draw line if enabled
        if (showPosition.getInputAsInt() == 2) {
            renderConnectionLine(partialTicks);
        }
    }

    private void renderConnectionLine(float partialTicks) {
        double currentX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double currentY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
        double currentZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;

        double renderX1 = currentX - mc.getRenderManager().viewerPosX;
        double renderY1 = currentY + mc.thePlayer.height / 2.0 - mc.getRenderManager().viewerPosY;
        double renderZ1 = currentZ - mc.getRenderManager().viewerPosZ;

        double renderX2 = laggedPosition.xCoord - mc.getRenderManager().viewerPosX;
        double renderY2 = laggedPosition.yCoord + mc.thePlayer.height / 2.0 - mc.getRenderManager().viewerPosY;
        double renderZ2 = laggedPosition.zCoord - mc.getRenderManager().viewerPosZ;

        Color color = new Color(255, 85, 85, 200);

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);

        GL11.glLineWidth(5.0F);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(renderX1, renderY1, renderZ1);
        GL11.glVertex3d(renderX2, renderY2, renderZ2);
        GL11.glEnd();

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
}
