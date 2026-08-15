package keystrokesmod.mixin.impl.accessor;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@SideOnly(Side.CLIENT)
@Mixin(EntityRenderer.class)
public interface IAccessorEntityRenderer {
   @Invoker("setupCameraTransform")
   void callSetupCameraTransform(float var1, int var2);

   @Invoker("loadShader")
   void callLoadShader(ResourceLocation var1);

   @Accessor("shaderResourceLocations")
   ResourceLocation[] getShaderResourceLocations();

   @Accessor("useShader")
   boolean getUseShader();

   @Accessor("useShader")
   void setUseShader(boolean var1);

   @Accessor("shaderIndex")
   int getShaderIndex();

   @Accessor("shaderIndex")
   void setShaderIndex(int var1);

   @Accessor("thirdPersonDistance")
   void setThirdPersonDistance(float var1);
}
