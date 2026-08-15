package keystrokesmod.module.impl.other;

import com.mojang.authlib.GameProfile;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class Test extends Module {
   public EntityOtherPlayerMP fakeEntity = null;
   private ButtonSetting spawnDummy;
   private GroupSetting groupSetting;
   private SliderSetting testSlider;
   private ButtonSetting test;

   public Test() {
      super("Test", Module.category.other);
      this.registerSetting(this.spawnDummy = new ButtonSetting("Spawn dummy", true));
      this.registerSetting(this.groupSetting = new GroupSetting("Group"));
      this.registerSetting(this.test = new ButtonSetting(this.groupSetting, "Test", true));
      this.registerSetting(this.testSlider = new SliderSetting(this.groupSetting, "Slider", 0, new String[]{"Option 1", "Option 2"}));
   }

   @Override
   public void onEnable() {
      if (this.spawnDummy.isToggled()) {
         this.fakeEntity = new EntityOtherPlayerMP(mc.theWorld, new GameProfile(mc.thePlayer.getUniqueID(), "Dummy"));
         this.fakeEntity.copyLocationAndAnglesFrom(mc.thePlayer);
         mc.theWorld.addEntityToWorld(-8008, this.fakeEntity);
         this.fakeEntity.inventory.armorInventory[0] = new ItemStack(Items.golden_helmet);
         this.fakeEntity.setCurrentItemOrArmor(0, new ItemStack(Blocks.wool));
      }
   }

   @Override
   public void onDisable() {
      if (this.fakeEntity != null) {
         mc.theWorld.removeEntity(this.fakeEntity);
         this.fakeEntity = null;
      }
   }
}
