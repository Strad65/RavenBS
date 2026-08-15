package keystrokesmod.mixin.impl.accessor;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@SideOnly(Side.CLIENT)
@Mixin(PlayerControllerMP.class)
public interface IAccessorPlayerControllerMP {
   @Accessor
   float getCurBlockDamageMP();

   @Accessor
   void setCurBlockDamageMP(float var1);

   @Accessor
   int getBlockHitDelay();

   @Accessor
   void setBlockHitDelay(int var1);

   @Invoker("syncCurrentPlayItem")
   void syncCurrentPlayItem();
}
