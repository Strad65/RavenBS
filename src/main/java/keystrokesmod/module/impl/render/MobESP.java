package keystrokesmod.module.impl.render;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class MobESP extends Module {
   private ButtonSetting healthBar;
   private ButtonSetting shaded;
   private ButtonSetting blaze;
   private ButtonSetting creeper;
   private ButtonSetting enderman;
   private ButtonSetting ghast;
   private ButtonSetting silverfish;
   private ButtonSetting skeleton;
   private ButtonSetting slime;
   private ButtonSetting spider;
   private ButtonSetting zombie;
   private ButtonSetting zombiePigman;
   private final Map<Class<? extends EntityLivingBase>, MobESP.MobSetting> mobRenders = new HashMap<>();

   public MobESP() {
      super("MobESP", Module.category.render);
      this.registerSetting(this.healthBar = new ButtonSetting("Health bar", false));
      this.registerSetting(this.shaded = new ButtonSetting("Shaded", false));
      this.registerSetting(this.blaze = new ButtonSetting("Blaze §6Orange", true));
      this.registerSetting(this.creeper = new ButtonSetting("Creeper §aGreen", true));
      this.registerSetting(this.enderman = new ButtonSetting("Enderman §7Black", true));
      this.registerSetting(this.ghast = new ButtonSetting("Ghast §fWhite", true));
      this.registerSetting(this.silverfish = new ButtonSetting("Silverfish §7Gray", true));
      this.registerSetting(this.skeleton = new ButtonSetting("Skeleton §fWhite", true));
      this.registerSetting(this.slime = new ButtonSetting("Slime §aGreen", true));
      this.registerSetting(this.spider = new ButtonSetting("Spider §7Black", true));
      this.registerSetting(this.zombie = new ButtonSetting("Zombie §1Blue", true));
      this.registerSetting(this.zombiePigman = new ButtonSetting("Zombie Pigman §dPink", true));
      this.mobRenders.put(EntityBlaze.class, new MobESP.MobSetting(this.blaze, Color.orange.getRGB(), 69.0));
      this.mobRenders.put(EntityCreeper.class, new MobESP.MobSetting(this.creeper, Color.green.getRGB(), 69.0));
      this.mobRenders.put(EntityEnderman.class, new MobESP.MobSetting(this.enderman, Color.black.getRGB(), 106.0));
      this.mobRenders.put(EntityGhast.class, new MobESP.MobSetting(this.ghast, Color.white.getRGB(), 143.0));
      this.mobRenders.put(EntitySilverfish.class, new MobESP.MobSetting(this.silverfish, Color.gray.getRGB(), 20.0));
      this.mobRenders.put(EntitySkeleton.class, new MobESP.MobSetting(this.skeleton, Color.white.getRGB(), 69.0));
      this.mobRenders.put(EntitySlime.class, new MobESP.MobSetting(this.slime, Color.green.getRGB()));
      this.mobRenders.put(EntitySpider.class, new MobESP.MobSetting(this.spider, Color.black.getRGB(), 40.0));
      this.mobRenders.put(EntityCaveSpider.class, new MobESP.MobSetting(this.spider, Color.black.getRGB(), 26.0));
      this.mobRenders.put(EntityZombie.class, new MobESP.MobSetting(this.zombie, Color.blue.getRGB()));
      this.mobRenders.put(EntityPigZombie.class, new MobESP.MobSetting(this.zombiePigman, Color.pink.getRGB()));
   }

   private void renderMob(EntityLivingBase entity, double height, int rgb, float partialTicks) {
      if (this.shaded.isToggled()) {
         RenderUtils.renderEntity(entity, 2, 0.0, 0.0, rgb, false);
      }

      if (this.healthBar.isToggled()) {
         this.drawHealthBar(entity, height, partialTicks);
      }
   }

   private void renderEntity(EntityLivingBase entity, float partialTicks) {
      MobESP.MobSetting mobSetting = this.mobRenders.get(entity.getClass());
      if (mobSetting != null && mobSetting.setting.isToggled()) {
         this.renderMob(entity, mobSetting.height, mobSetting.color, partialTicks);
      }
   }

   @SubscribeEvent
   public void onRenderWorldLast(RenderWorldLastEvent e) {
      if (Utils.nullCheck()) {
         for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase && entity != mc.thePlayer && ((EntityLivingBase)entity).deathTime == 0) {
               this.renderEntity((EntityLivingBase)entity, e.partialTicks);
            }
         }
      }
   }

   private void drawHealthBar(EntityLivingBase en, double mobHeight, float partialTicks) {
      double x = en.lastTickPosX + (en.posX - en.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
      double y = en.lastTickPosY + (en.posY - en.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
      double z = en.lastTickPosZ + (en.posZ - en.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;
      GlStateManager.pushMatrix();
      int xOffset = 21;
      double health = en.getHealth() / en.getMaxHealth();
      int height = (int)(mobHeight * health);
      int healthColor = health < 0.3
         ? Color.red.getRGB()
         : (health < 0.5 ? Color.orange.getRGB() : (health < 0.7 ? Color.yellow.getRGB() : Color.green.getRGB()));
      GL11.glTranslated(x, y - 0.2, z);
      GL11.glRotated(-mc.getRenderManager().playerViewY, 0.0, 1.0, 0.0);
      GlStateManager.disableDepth();
      GL11.glScalef(0.03F, 0.03F, 0.03F);
      Gui.drawRect(xOffset, -1, xOffset + 4, (int)(mobHeight + 1.0), Color.black.getRGB());
      Gui.drawRect(xOffset + 1, height, xOffset + 3, (int)mobHeight, Color.darkGray.getRGB());
      Gui.drawRect(xOffset + 1, 0, xOffset + 3, height, healthColor);
      GlStateManager.enableDepth();
      GlStateManager.popMatrix();
   }

   private static class MobSetting {
      ButtonSetting setting;
      int color;
      double height = 74.0;

      public MobSetting(ButtonSetting setting, int color) {
         this.setting = setting;
         this.color = color;
      }

      public MobSetting(ButtonSetting setting, int color, double height) {
         this.setting = setting;
         this.color = color;
         this.height = height;
      }
   }
}
