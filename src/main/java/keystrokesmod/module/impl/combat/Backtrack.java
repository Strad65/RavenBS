package keystrokesmod.module.impl.combat;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Backtrack extends Module {
    private static final Random random = new Random();
    private static final int QUEUE_SIZE_LIMIT = 100; // Safety limit

    // Core parameters
    public SliderSetting minDelay;
    public SliderSetting maxDelay;
    public SliderSetting thresholdDistance;
    public SliderSetting maxRange;

    // Filters
    public ButtonSetting weaponsOnly;
    public ButtonSetting botCheck;
    public ButtonSetting teams;
    public ButtonSetting invisibles;

    // Visualization
    private String[] showPositionModes = {"Off", "On"};
    public SliderSetting showPosition;

    // State tracking
    private EntityPlayer target = null;
    private Vec3 trackedPosition = null;
    private List<TimedPacket> packetQueue = new ArrayList<>();
    private boolean isTracking = false;

    // Distance tracking for retreat detection
    private double lastDistance = 0.0;
    private int currentDelay = 0;

    public Backtrack() {
        super("Backtrack", Module.category.combat, 0);

        this.registerSetting(minDelay = new SliderSetting("Min delay", 100.0, 50.0, 300.0, 10.0));
        this.registerSetting(maxDelay = new SliderSetting("Max delay", 200.0, 100.0, 500.0, 10.0));
        this.registerSetting(thresholdDistance = new SliderSetting("Threshold distance", 2.5, 2.0, 3.5, 0.1));
        this.registerSetting(maxRange = new SliderSetting("Max range", 6.0, 3.0, 10.0, 0.1));

        this.registerSetting(weaponsOnly = new ButtonSetting("Weapons only", true));
        this.registerSetting(botCheck = new ButtonSetting("Bot check", true));
        this.registerSetting(teams = new ButtonSetting("Teams", true));
        this.registerSetting(invisibles = new ButtonSetting("Invisibles", false));

        this.registerSetting(showPosition = new SliderSetting("Show position", 1, showPositionModes));
    }

    @Override
    public String getInfo() {
        if (isTracking && target != null) {
            return packetQueue.size() + " pkts";
        }
        return minDelay.getInputAsInt() + "-" + maxDelay.getInputAsInt() + "ms";
    }

    @Override
    public void onEnable() {
        clearTracking();
        lastDistance = 0.0;
        currentDelay = randomDelay();
    }

    @Override
    public void onDisable() {
        releaseAllPackets();
        clearTracking();
        lastDistance = 0.0;
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        // Process packet queue - release packets older than current delay
        processPacketQueue();

        // Update target tracking
        updateTargetTracking();

        // Check if should stop tracking
        if (isTracking && target != null) {
            if (!shouldTrack(target)) {
                stopTracking();
            }
        }
    }

    private int randomDelay() {
        int min = minDelay.getInputAsInt();
        int max = maxDelay.getInputAsInt();
        return min + random.nextInt(Math.max(1, max - min + 1));
    }

    private void processPacketQueue() {
        long currentTime = System.currentTimeMillis();

        Iterator<TimedPacket> iterator = packetQueue.iterator();
        while (iterator.hasNext()) {
            TimedPacket timedPacket = iterator.next();
            if (currentTime - timedPacket.timestamp >= currentDelay) {
                // Release packet
                PacketUtils.receivePacketNoEvent(timedPacket.packet);
                iterator.remove();
            } else {
                // Packets are ordered by time, so stop when we hit one that's not ready
                break;
            }
        }
    }

    private void updateTargetTracking() {
        EntityPlayer closestTarget = findTarget();

        if (closestTarget != null) {
            double currentDistance = mc.thePlayer.getDistanceToEntity(closestTarget);

            // Check if this is a new target
            boolean isNewTarget = (target != closestTarget);

            if (isNewTarget) {
                // New target detected - reset tracking
                if (isTracking) {
                    stopTracking();
                }
                target = closestTarget;
                lastDistance = currentDistance;
                // Don't start tracking yet - need to see if enemy is retreating
            } else if (target == closestTarget) {
                // Same target - check if conditions met to start/continue tracking
                boolean isRetreating = currentDistance > lastDistance;
                boolean isBeyondThreshold = currentDistance > thresholdDistance.getInput();
                boolean withinMaxRange = currentDistance < maxRange.getInput();

                // Start tracking when enemy is retreating beyond threshold
                if (!isTracking && isBeyondThreshold && isRetreating && withinMaxRange) {
                    startTracking(closestTarget);
                }

                // Stop tracking if enemy exceeds max range
                if (isTracking && !withinMaxRange) {
                    stopTracking();
                }

                // Update last distance for next tick
                lastDistance = currentDistance;
            }
        } else {
            // No valid target
            if (isTracking) {
                stopTracking();
            }
            target = null;
            lastDistance = 0.0;
        }
    }

    private EntityPlayer findTarget() {
        EntityPlayer closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Object entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) continue;

            EntityPlayer player = (EntityPlayer) entity;
            if (!isValidTarget(player)) continue;

            double dist = mc.thePlayer.getDistanceToEntity(player);
            if (dist < maxRange.getInput() && dist < closestDist) {
                closest = player;
                closestDist = dist;
            }
        }

        return closest;
    }

    private boolean isValidTarget(EntityPlayer player) {
        // Use our refactored utility method
        if (!Utils.isValidCombatTarget(player, teams.isToggled(), botCheck.isToggled())) {
            return false;
        }

        // Check invisibility
        if (!invisibles.isToggled() && player.isInvisible()) {
            return false;
        }

        return true;
    }

    private boolean shouldTrack(EntityPlayer player) {
        // Check if player is still valid
        if (player == null || !player.isEntityAlive()) {
            return false;
        }

        // Check if player is in world
        if (!mc.theWorld.loadedEntityList.contains(player)) {
            return false;
        }

        // Check if out of range
        if (trackedPosition != null) {
            double distance = trackedPosition.distanceTo(new Vec3(
                mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ
            ));
            if (distance > maxRange.getInput()) {
                return false;
            }
        }

        // Check weapon requirement
        if (weaponsOnly.isToggled() && mc.thePlayer.getHeldItem() == null) {
            return false;
        }

        // Check if target is still valid
        if (!isValidTarget(player)) {
            return false;
        }

        return true;
    }

    private void startTracking(EntityPlayer player) {
        target = player;
        trackedPosition = new Vec3(player.posX, player.posY, player.posZ);
        isTracking = true;
        currentDelay = randomDelay();
        packetQueue.clear();
    }

    private void stopTracking() {
        releaseAllPackets();
        clearTracking();
    }

    private void clearTracking() {
        target = null;
        trackedPosition = null;
        isTracking = false;
        lastDistance = 0.0;
        packetQueue.clear();
    }

    private void releaseAllPackets() {
        for (TimedPacket timedPacket : packetQueue) {
            PacketUtils.receivePacketNoEvent(timedPacket.packet);
        }
        packetQueue.clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onReceivePacket(ReceivePacketEvent event) {
        if (!Utils.nullCheck() || event.isCanceled()) {
            return;
        }

        if (!isTracking || target == null) {
            return;
        }

        Packet<?> packet = event.getPacket();

        // Whitelist: always pass through critical packets
        if (shouldPassThrough(packet)) {
            return;
        }

        // Handle entity movement packets for our target
        Vec3 newTrackedPosition = updateTrackedPositionFromPacket(packet);

        if (newTrackedPosition != null) {
            // Smart flush: do not delay an update that moves the target closer.
            double pendingDistSq = newTrackedPosition.squareDistanceTo(
                new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
            );
            double renderedDistSq = target.getDistanceSqToEntity(mc.thePlayer);

            if (pendingDistSq < renderedDistSq) {
                releaseAllPackets();
                return;
            }

            // Update tracked position
            trackedPosition = newTrackedPosition;

            // Queue the packet
            packetQueue.add(new TimedPacket(packet, System.currentTimeMillis()));
            event.setCanceled(true);

            // Safety check: limit queue size
            if (packetQueue.size() > QUEUE_SIZE_LIMIT) {
                stopTracking();
            }
        }
    }

    private boolean shouldPassThrough(Packet<?> packet) {
        // Teleport packets - always process immediately
        if (packet instanceof S08PacketPlayerPosLook) {
            stopTracking();
            return true;
        }

        // Death/health packets
        if (packet instanceof S06PacketUpdateHealth) {
            S06PacketUpdateHealth healthPacket = (S06PacketUpdateHealth) packet;
            if (healthPacket.getHealth() <= 0) {
                stopTracking();
                return true;
            }
        }

        // Chat messages - always pass
        if (packet instanceof S02PacketChat) {
            return true;
        }

        // Disconnect
        if (packet instanceof S40PacketDisconnect) {
            stopTracking();
            return true;
        }

        return false;
    }

    private Vec3 updateTrackedPositionFromPacket(Packet<?> packet) {
        if (packet instanceof S14PacketEntity) {
            S14PacketEntity entityPacket = (S14PacketEntity) packet;
            net.minecraft.entity.Entity entity = entityPacket.getEntity(mc.theWorld);

            if (entity != null && entity.getEntityId() == target.getEntityId()) {
                // Relative movement
                double dx = entityPacket.func_149062_c() / 32.0;
                double dy = entityPacket.func_149061_d() / 32.0;
                double dz = entityPacket.func_149064_e() / 32.0;

                return trackedPosition.addVector(dx, dy, dz);
            }
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport teleportPacket = (S18PacketEntityTeleport) packet;

            if (teleportPacket.getEntityId() == target.getEntityId()) {
                // Absolute position
                return new Vec3(
                    teleportPacket.getX() / 32.0,
                    teleportPacket.getY() / 32.0,
                    teleportPacket.getZ() / 32.0
                );
            }
        } else if (packet instanceof S13PacketDestroyEntities) {
            S13PacketDestroyEntities destroyPacket = (S13PacketDestroyEntities) packet;

            for (int entityId : destroyPacket.getEntityIDs()) {
                if (entityId == target.getEntityId()) {
                    // Target was destroyed
                    stopTracking();
                    return null;
                }
            }
        } else if (packet instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus statusPacket = (S19PacketEntityStatus) packet;

            if (statusPacket.getEntity(mc.theWorld) == target && statusPacket.getOpCode() == 3) {
                // Target died
                stopTracking();
                return null;
            }
        }

        return null;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!Utils.nullCheck()) return;
        if (showPosition.getInputAsInt() == 0) return;
        if (!isTracking || target == null || trackedPosition == null) return;

        renderTrackedPosition(event.partialTicks);
    }

    private void renderTrackedPosition(float partialTicks) {
        double renderX = trackedPosition.xCoord - mc.getRenderManager().viewerPosX;
        double renderY = trackedPosition.yCoord - mc.getRenderManager().viewerPosY;
        double renderZ = trackedPosition.zCoord - mc.getRenderManager().viewerPosZ;

        // Use HUD theme color
        int color = Theme.getGradient((int)HUD.theme.getInput(), 0.0);
        float a = (color >> 24 & 0xFF) / 255.0F;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        AxisAlignedBB bbox = target.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
        AxisAlignedBB axis = new AxisAlignedBB(
            bbox.minX - target.posX + renderX,
            bbox.minY - target.posY + renderY,
            bbox.minZ - target.posZ + renderZ,
            bbox.maxX - target.posX + renderX,
            bbox.maxY - target.posY + renderY,
            bbox.maxZ - target.posZ + renderZ
        );

        GlStateManager.pushMatrix();
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2.0F);
        GL11.glColor4f(r, g, b, a);
        RenderUtils.drawBoundingBox(axis, r, g, b);
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
        GlStateManager.popMatrix();
    }

    // Helper class to store packets with timestamp
    private static class TimedPacket {
        final Packet<?> packet;
        final long timestamp;

        TimedPacket(Packet<?> packet, long timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }
    }
}
