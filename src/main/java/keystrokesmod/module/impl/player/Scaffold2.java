package keystrokesmod.module.impl.player;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.BlockAir;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Scaffold2 - Simplified scaffold with GrimAC RotationPlace bypass
 *
 * Features:
 * - No configuration options
 * - Always attempts placement (including while stationary for clutch/self-save)
 * - Auto-selects blocks from hotbar
 * - Movement fix enabled
 * - Silent rotation (1st person view unchanged, 3rd person shows rotation)
 * - Physics-accurate rotation calculation with raytrace validation
 * - Prediction-aware placement timing
 */
public class Scaffold2 extends Module {

    private float yaw;
    private float pitch;
    private BlockData targetBlock;
    private BlockData lastTargetBlock; // Track last target to maintain rotation
    private int blockSlot = -1;
    private boolean hasRotation = false; // Track if we have valid rotation

    public Scaffold2() {
        super("Scaffold2", category.player);
    }

    @Override
    public void onEnable() {
        if (Utils.nullCheck()) {
            this.targetBlock = null;
            this.lastTargetBlock = null;
            this.blockSlot = -1;
            this.hasRotation = false;
        }
    }

    @Override
    public void onDisable() {
        this.targetBlock = null;
        this.lastTargetBlock = null;
        this.blockSlot = -1;
        this.hasRotation = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPreUpdate(PreUpdateEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }

        // Find block slot in hotbar
        this.blockSlot = getBlockSlot();
        if (this.blockSlot == -1) {
            return; // No blocks available
        }

        // Find placement target with prediction
        this.targetBlock = findPlacementTarget();
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }

        // If we have a new target, calculate new rotation
        if (this.targetBlock != null && !isSameTarget(this.targetBlock, this.lastTargetBlock)) {
            float[] rotation = calculateRotation(this.targetBlock);

            if (rotation != null) {
                this.yaw = rotation[0];
                this.pitch = rotation[1];
                this.hasRotation = true;
                this.lastTargetBlock = this.targetBlock;
            }
        }

        // Apply rotation and movement fix if we have valid rotation
        if (this.hasRotation) {
            e.setYaw(this.yaw);
            e.setPitch(this.pitch);
            applyMovementFix(this.yaw);
        }

        // Place block if target is valid
        if (this.targetBlock != null) {
            if (placeBlock(this.targetBlock)) {
                this.targetBlock = null; // Reset after successful placement
            }
        }
    }

    /**
     * Find a block item in the hotbar (slots 0-8)
     */
    private int getBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                ItemBlock itemBlock = (ItemBlock) stack.getItem();
                // Avoid placing certain blocks
                if (itemBlock.getBlock() instanceof BlockAir) {
                    continue;
                }
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the block below player to place on with prediction
     */
    private BlockData findPlacementTarget() {
        // Predict player position slightly ahead for better timing
        double motionX = mc.thePlayer.motionX;
        double motionZ = mc.thePlayer.motionZ;

        // Check current position and slightly predicted position
        BlockPos playerPos = new BlockPos(
            mc.thePlayer.posX + motionX * 2,
            mc.thePlayer.posY,
            mc.thePlayer.posZ + motionZ * 2
        );
        BlockPos belowPos = playerPos.down();

        // Check if block below is air
        if (!(mc.theWorld.getBlockState(belowPos).getBlock() instanceof BlockAir)) {
            return null; // Already has block below
        }

        // Search for a solid block adjacent to belowPos to place against
        // Prioritize DOWN face for better simulation/prediction compatibility
        EnumFacing[] faces = {EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST};

        for (EnumFacing facing : faces) {
            BlockPos adjacentPos = belowPos.offset(facing);

            if (!(mc.theWorld.getBlockState(adjacentPos).getBlock() instanceof BlockAir)) {
                // Found solid block, check if we can reach it
                double distance = mc.thePlayer.getPositionEyes(1.0F).distanceTo(
                    new Vec3(adjacentPos.getX() + 0.5, adjacentPos.getY() + 0.5, adjacentPos.getZ() + 0.5)
                );

                if (distance <= mc.playerController.getBlockReachDistance()) {
                    return new BlockData(adjacentPos, facing.getOpposite());
                }
            }
        }

        return null; // No valid placement target
    }

    /**
     * Check if two targets are the same
     */
    private boolean isSameTarget(BlockData target1, BlockData target2) {
        if (target1 == null || target2 == null) {
            return false;
        }
        return target1.pos.equals(target2.pos) && target1.face == target2.face;
    }

    /**
     * Calculate rotation to aim at the center of the target block face
     * Returns null if raytrace validation fails
     */
    private float[] calculateRotation(BlockData blockData) {
        BlockPos blockPos = blockData.pos;
        EnumFacing facing = blockData.face;

        // Calculate exact center of the clicked face
        double targetX = blockPos.getX() + 0.5 + getFaceOffset(facing, "x");
        double targetY = blockPos.getY() + 0.5 + getFaceOffset(facing, "y");
        double targetZ = blockPos.getZ() + 0.5 + getFaceOffset(facing, "z");

        // Calculate rotation from player eye to target point
        double dx = targetX - mc.thePlayer.posX;
        double dy = targetY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = targetZ - mc.thePlayer.posZ;

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        yaw = mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);

        float pitch = (float) (-(Math.atan2(dy, horizontalDist) * 180.0 / Math.PI));
        pitch = MathHelper.clamp_float(pitch, -90.0F, 90.0F);

        // GrimAC validation: verify rotation hits the target block and face
        MovingObjectPosition mop = RotationUtils.rayTraceCustom(
            mc.playerController.getBlockReachDistance(), yaw, pitch
        );

        if (mop != null
            && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            && mop.getBlockPos().equals(blockPos)
            && mop.sideHit == facing) {
            return new float[]{yaw, pitch};
        }

        return null; // Raytrace validation failed
    }

    /**
     * Get face offset for calculating face center
     * Returns ±0.5 for the perpendicular axis, 0.0 otherwise
     */
    private double getFaceOffset(EnumFacing facing, String axis) {
        switch (axis) {
            case "x":
                return facing == EnumFacing.WEST ? -0.5 : (facing == EnumFacing.EAST ? 0.5 : 0.0);
            case "y":
                return facing == EnumFacing.DOWN ? -0.5 : (facing == EnumFacing.UP ? 0.5 : 0.0);
            case "z":
                return facing == EnumFacing.NORTH ? -0.5 : (facing == EnumFacing.SOUTH ? 0.5 : 0.0);
            default:
                return 0.0;
        }
    }

    /**
     * Apply movement fix to maintain correct movement direction with server rotation
     */
    private void applyMovementFix(float serverYaw) {
        float yawDiff = MathHelper.wrapAngleTo180_float(serverYaw - mc.thePlayer.rotationYaw);
        float rad = (float) Math.toRadians(yawDiff);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float origForward = mc.thePlayer.movementInput.moveForward;
        float origStrafe = mc.thePlayer.movementInput.moveStrafe;

        mc.thePlayer.movementInput.moveForward = origForward * cos - origStrafe * sin;
        mc.thePlayer.movementInput.moveStrafe = origStrafe * cos + origForward * sin;

        Settings.fixedForward = mc.thePlayer.movementInput.moveForward;
        Settings.fixedStrafe = mc.thePlayer.movementInput.moveStrafe;
    }

    /**
     * Place block at target position
     */
    private boolean placeBlock(BlockData blockData) {
        if (this.blockSlot == -1) {
            return false;
        }

        int previousSlot = mc.thePlayer.inventory.currentItem;

        // Switch to block slot
        mc.thePlayer.inventory.currentItem = this.blockSlot;

        // Calculate hit vector (center of clicked face)
        Vec3 hitVec = new Vec3(
            blockData.pos.getX() + 0.5 + getFaceOffset(blockData.face, "x"),
            blockData.pos.getY() + 0.5 + getFaceOffset(blockData.face, "y"),
            blockData.pos.getZ() + 0.5 + getFaceOffset(blockData.face, "z")
        );

        // Place block
        boolean placed = mc.playerController.onPlayerRightClick(
            mc.thePlayer,
            mc.theWorld,
            mc.thePlayer.getHeldItem(),
            blockData.pos,
            blockData.face,
            hitVec
        );

        // Restore previous slot
        mc.thePlayer.inventory.currentItem = previousSlot;

        return placed;
    }

    /**
     * Data class for block placement target
     */
    private static class BlockData {
        public final BlockPos pos;
        public final EnumFacing face;

        public BlockData(BlockPos pos, EnumFacing face) {
            this.pos = pos;
            this.face = face;
        }
    }
}
