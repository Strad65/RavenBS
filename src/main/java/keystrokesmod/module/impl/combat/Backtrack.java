package keystrokesmod.module.impl.combat;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Backtrack extends Module {
    // Core parameters
    public SliderSetting delay;
    public SliderSetting maxRange;

    // Trigger mode
    private String[] targetModes = {"On Attack", "Always"};
    public SliderSetting targetMode;

    // Filters
    public ButtonSetting weaponsOnly;
    public ButtonSetting botCheck;
    public ButtonSetting teams;
    public ButtonSetting invisibles;

    // Visualization
    private String[] showPositionModes = {"Off", "Box", "Box + Outline"};
    public SliderSetting showPosition;

    // State tracking
    private EntityPlayer target = null;
    private Vec3 trackedPosition = null;
    private List<TimedPacket> packetQueue = new ArrayList<>();
    private boolean isTracking = false;

    public Backtrack() {
        super("Backtrack", Module.category.combat, 0);

        this.registerSetting(delay = new SliderSetting("Delay", 200.0, 100.0, 500.0, 10.0));
        this.registerSetting(maxRange = new SliderSetting("Max range", 3.5, 3.0, 6.0, 0.1));

        this.registerSetting(targetMode = new SliderSetting("Target mode", 0, targetModes));

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
        return delay.getInputAsInt() + "ms";
    }

    @Override
    public void onEnable() {
        clearTracking();
    }

    @Override
    public void onDisable() {
        releaseAllPackets();
        clearTracking();
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        // Process packet queue - release packets older than delay
        processPacketQueue();

        // Update target in "Always" mode
        if (targetMode.getInputAsInt() == 1) {
            updateAlwaysTarget();
        }

        // Check if should stop tracking
        if (isTracking && target != null) {
            if (!shouldTrack(target)) {
                stopTracking();
            }
        }
    }

    private void processPacketQueue() {
        long currentTime = System.currentTimeMillis();
        int delayMs = delay.getInputAsInt();

        Iterator<TimedPacket> iterator = packetQueue.iterator();
        while (iterator.hasNext()) {
            TimedPacket timedPacket = iterator.next();
            if (currentTime - timedPacket.timestamp >= delayMs) {
                // Release packet
                PacketUtils.receivePacketNoEvent(timedPacket.packet);
                iterator.remove();
            } else {
                // Packets are ordered by time, so stop when we hit one that's not ready
                break;
            }
        }
    }

    private void updateAlwaysTarget() {
        EntityPlayer closestTarget = findTarget();

        if (closestTarget != null) {
            if (target != closestTarget) {
                // Target changed
                stopTracking();
                startTracking(closestTarget);
            }
        } else {
            // No valid target
            if (isTracking) {
                stopTracking();
            }
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
        packetQueue.clear();
    }

    private void releaseAllPackets() {
        for (TimedPacket timedPacket : packetQueue) {
            PacketUtils.receivePacketNoEvent(timedPacket.packet);
        }
        packetQueue.clear();
    }

    // Handle player attack in "On Attack" mode
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSendPacket(SendPacketEvent event) {
        if (!Utils.nullCheck() || event.isCanceled()) {
            return;
        }

        // Detect attack packet
        if (event.getPacket() instanceof net.minecraft.network.play.client.C02PacketUseEntity) {
            net.minecraft.network.play.client.C02PacketUseEntity packet =
                (net.minecraft.network.play.client.C02PacketUseEntity) event.getPacket();

            if (packet.getAction() == net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK) {
                net.minecraft.entity.Entity attackedEntity = packet.getEntityFromWorld(mc.theWorld);

                if (attackedEntity instanceof EntityPlayer && targetMode.getInputAsInt() == 0) {
                    EntityPlayer player = (EntityPlayer) attackedEntity;

                    if (isValidTarget(player)) {
                        // Start tracking on attack
                        if (target != player) {
                            stopTracking();
                            startTracking(player);
                        }
                    }
                }
            }
        }
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
        boolean shouldQueue = false;

        // Handle entity movement packets
        if (packet instanceof S14PacketEntity) {
            S14PacketEntity entityPacket = (S14PacketEntity) packet;
            int entityId = entityPacket.getEntity(mc.theWorld) != null ?
                entityPacket.getEntity(mc.theWorld).getEntityId() : -1;

            if (entityId == target.getEntityId()) {
                // Update tracked position
                double dx = entityPacket.func_149062_c() / 32.0;
                double dy = entityPacket.func_149061_d() / 32.0;
                double dz = entityPacket.func_149064_e() / 32.0;

                trackedPosition = trackedPosition.addVector(dx, dy, dz);
                shouldQueue = true;
            }
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport teleportPacket = (S18PacketEntityTeleport) packet;

            if (teleportPacket.getEntityId() == target.getEntityId()) {
                // Update tracked position
                trackedPosition = new Vec3(
                    teleportPacket.getX() / 32.0,
                    teleportPacket.getY() / 32.0,
                    teleportPacket.getZ() / 32.0
                );
                shouldQueue = true;
            }
        } else if (packet instanceof S13PacketDestroyEntities) {
            S13PacketDestroyEntities destroyPacket = (S13PacketDestroyEntities) packet;

            for (int entityId : destroyPacket.getEntityIDs()) {
                if (entityId == target.getEntityId()) {
                    // Target was destroyed, stop tracking
                    stopTracking();
                    return;
                }
            }
        } else if (packet instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus statusPacket = (S19PacketEntityStatus) packet;

            if (statusPacket.getEntity(mc.theWorld) == target && statusPacket.getOpCode() == 3) {
                // Target died, stop tracking
                stopTracking();
                return;
            }
        }

        if (shouldQueue) {
            // Add to queue and cancel original packet
            packetQueue.add(new TimedPacket(packet, System.currentTimeMillis()));
            event.setCanceled(true);

            // Safety check: limit queue size
            if (packetQueue.size() > 100) {
                stopTracking();
            }
        }
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

        // White box with higher opacity
        Color boxColor = new Color(255, 255, 255, 150);
        Color outlineColor = new Color(255, 255, 255, 255);

        boolean renderOutline = showPosition.getInputAsInt() == 2;

        // Render box
        RenderUtils.renderBox(
            renderX - target.width / 2.0,
            renderY,
            renderZ - target.width / 2.0,
            target.width,
            target.height,
            target.width,
            boxColor,
            renderOutline,
            true
        );
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
