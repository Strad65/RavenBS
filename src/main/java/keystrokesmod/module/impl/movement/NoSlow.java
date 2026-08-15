package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.ReflectionUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSnow;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class NoSlow extends Module {
   public static SliderSetting sword;
   public static SliderSetting mode;
   public static SliderSetting blinkMode;
   public static SliderSetting vanillaMode;
   public static SliderSetting slowed;
   public static ButtonSetting disableBow;
   public static ButtonSetting disableSword;
   public static ButtonSetting autoblockOnly;
   public static ButtonSetting disablePotions;
   public static ButtonSetting swordOnly;
   public ButtonSetting renderTimer;
   private String[] swordMode = new String[]{"Vanilla", "Item mode", "Fake"};
   private String[] modes = new String[]{"Vanilla", "Pre", "Post", "Alpha", "Float", "Blink"};
   private String[] blinkModes = new String[]{"Default", "Begin off ground"};
   private String[] vanillaModes = new String[]{"Default", "Only on ground"};
   private boolean postPlace;
   private boolean canFloat;
   public boolean noSlowing;
   public boolean offset;
   public boolean blockingClient;
   public boolean blink;
   private boolean wentOffGround;
   private boolean requireJump;
   private static boolean fix;
   private boolean didC;
   private boolean md;
   private boolean jumped;
   private boolean setCancelled;
   private boolean setJump;
   private static boolean hasClicked;
   private boolean blocking;
   public static boolean cantBlock;

   public NoSlow() {
      super("NoSlow", Module.category.movement, 0);
      this.registerSetting(new DescriptionSetting("Default is 80% motion reduction."));
      this.registerSetting(sword = new SliderSetting("Sword", 0, this.swordMode));
      this.registerSetting(mode = new SliderSetting("Item", 0, this.modes));
      this.registerSetting(vanillaMode = new SliderSetting("Vanilla mode", 0, this.vanillaModes));
      this.registerSetting(blinkMode = new SliderSetting("Blink Mode", 0, this.blinkModes));
      this.registerSetting(this.renderTimer = new ButtonSetting("Render timer", false));
      this.registerSetting(slowed = new SliderSetting("Slow %", 80.0, 0.0, 80.0, 1.0));
      this.registerSetting(disableSword = new ButtonSetting("Disable sword", false));
      this.registerSetting(disableBow = new ButtonSetting("Disable bow", false));
      this.registerSetting(disablePotions = new ButtonSetting("Disable potions", false));
      this.registerSetting(autoblockOnly = new ButtonSetting("Sword only while autoblock", false));
      this.registerSetting(swordOnly = new ButtonSetting("Sword only", false));
   }

   @Override
   public void guiUpdate() {
      this.renderTimer.setVisible(mode.getInput() == 5.0, this);
      blinkMode.setVisible(mode.getInput() == 5.0, this);
      vanillaMode.setVisible(mode.getInput() == 0.0, this);
   }

   @Override
   public void onDisable() {
      this.resetFloat();
      if (this.blockingClient) {
         ReflectionUtils.setItemInUse(this.blockingClient = false);
      }

      this.blink = this.wentOffGround = false;
      hasClicked = false;
      cantBlock = false;
   }

   @SubscribeEvent
   public void onMouse(MouseEvent e) {
      if (e.button == 1 && e.buttonstate) {
         this.handleFloatSetup();
         if (this.setCancelled) {
            this.setCancelled = false;
            e.setCanceled(true);
         }
      }

      if (sword.getInput() == 2.0 && Utils.tabbedIn() && !ModuleManager.killAura.blockingClient && e.button == 1) {
         EntityLivingBase g = Utils.raytrace(4);
         if (Utils.holdingSword() && g == null && !BlockUtils.isInteractable(mc.objectMouseOver)) {
            e.setCanceled(true);
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOW)
   public void onPreUpdate(PreUpdateEvent e) {
      boolean apply = getSlowed() != 0.2F;
      if (sword.getInput() == 2.0) {
         EntityLivingBase g = Utils.raytrace(4);
         if (Utils.holdingSword() && g == null && !BlockUtils.isInteractable(mc.objectMouseOver)) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
         }
      }

      if (apply && mc.thePlayer.isUsingItem()) {
         this.postPlace = false;
         if (sword.getInput() == 1.0 || !Utils.holdingSword()) {
            switch ((int)mode.getInput()) {
               case 1:
                  if (mc.thePlayer.ticksExisted % 3 == 0 && !keystrokesmod.Raven.packetsHandler.C07.sentCurrentTick.get()) {
                     mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                  }
                  break;
               case 2:
                  this.postPlace = true;
                  break;
               case 3:
                  if (mc.thePlayer.ticksExisted % 3 == 0 && !keystrokesmod.Raven.packetsHandler.C07.sentCurrentTick.get()) {
                     mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 1, null, 0.0F, 0.0F, 0.0F));
                  }
                  break;
               case 4:
                  if (Mouse.isButtonDown(1)) {
                     this.handleFloatSetup();
                  }

                  if (!this.blockConditions()) {
                     this.didC = true;
                     this.requireJump = true;
                  } else if (this.didC && !mc.thePlayer.onGround) {
                     fix = true;
                  }
                  break;
               case 5:
                  if (this.blinkConditions()) {
                     this.blink = true;
                  } else {
                     this.blink = this.wentOffGround = false;
                  }
            }
         }
      } else {
         this.wentOffGround = this.blink = false;
      }
   }

   @SubscribeEvent
   public void onPostMotion(PostMotionEvent e) {
      if (this.postPlace && mode.getInput() == 2.0) {
         if (mc.thePlayer.ticksExisted % 3 == 0 && !keystrokesmod.Raven.packetsHandler.C07.sentCurrentTick.get()) {
            mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
         }

         this.postPlace = false;
      }
   }

   @SubscribeEvent
   public void onPostPlayerInput(PostPlayerInputEvent e) {
      if (this.setJump) {
         mc.thePlayer.movementInput.jump = true;
         this.setJump = false;
      }
   }

   @SubscribeEvent
   public void onPreMotion(PreMotionEvent e) {
      EntityLivingBase g = Utils.raytrace(4);
      if (ModuleManager.killAura.blockingClient) {
         this.blockingClient = false;
      }

      if (this.blockingClient && (!Mouse.isButtonDown(1) || !Utils.holdingSword())) {
         ReflectionUtils.setItemInUse(this.blockingClient = false);
      }

      if (sword.getInput() == 2.0) {
         if (this.blocking
            && (g == null && !BlockUtils.isInteractable(mc.objectMouseOver) || !Utils.holdingSword() && !Utils.keybinds.isMouseDown(1) || !Utils.tabbedIn())) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            this.blocking = false;
            hasClicked = false;
         }

         if (Utils.holdingSword()) {
            if (g == null && !BlockUtils.isInteractable(mc.objectMouseOver)) {
               cantBlock = true;
               if (Utils.tabbedIn() && Mouse.isButtonDown(1)) {
                  this.blockingClient = true;
               }
            } else {
               this.blocking = true;
               cantBlock = false;
               if (ModuleUtils.rcTick == 1) {
                  hasClicked = true;
               }
            }

            if (this.blockingClient) {
               ReflectionUtils.setItemInUse(this.blockingClient = true);
               if (Mouse.isButtonDown(0)) {
                  mc.thePlayer.swingItem();
               }
            }
         } else {
            cantBlock = false;
            hasClicked = false;
         }
      } else {
         cantBlock = false;
         hasClicked = false;
      }

      if (!Mouse.isButtonDown(1)) {
         this.md = false;
      }

      this.postPlace = false;
      if (mode.getInput() == 4.0) {
         boolean apply = getSlowed() != 0.2F;
         if (!Mouse.isButtonDown(1) || !this.holdingUsable() || !Utils.tabbedIn()) {
            this.resetFloat();
         }

         if (apply && !fix && !this.didC) {
            if (!this.canFloat && this.jumped && ModuleUtils.inAirTicks > 1) {
               KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
               this.canFloat = true;
               this.setCancelled = false;
            } else if (this.canFloat && this.canFloat() && !this.requireJump) {
               this.md = true;
               if (!mc.thePlayer.onGround) {
                  e.setPosY(e.getPosY() - 0.001);
               } else {
                  e.setPosY(e.getPosY() + 0.001);
               }
            }
         }
      }
   }

   private void handleFloatSetup() {
      boolean apply = getSlowed() != 0.2F;
      if (mode.getInput() == 4.0) {
         if (apply && !fix && !this.didC && this.holdingUsable() && !this.canFloat && !this.jumped && !BlockUtils.isInteractable(mc.objectMouseOver) && !this.md) {
            if (mc.thePlayer.onGround) {
               this.setJump = true;
               this.jumped = true;
               this.setCancelled = true;
               KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            } else {
               this.canFloat = true;
            }
         }
      }
   }

   private boolean blinkConditions() {
      if (blinkMode.getInput() == 0.0) {
         return true;
      }

      if (blinkMode.getInput() != 1.0 || mc.thePlayer.onGround && !this.wentOffGround) {
         return false;
      }

      this.wentOffGround = true;
      return true;
   }

   private boolean blockConditions() {
      Block block = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
      int edge = (int)Math.round(mc.thePlayer.posY % 1.0 * 100.0);
      if (mc.thePlayer.posY % 1.0 == 0.0) {
         return true;
      } else if (edge < 10) {
         return true;
      } else if (!mc.thePlayer.onGround) {
         return true;
      } else if (block instanceof BlockSnow) {
         return true;
      } else {
         return block instanceof BlockCarpet ? true : block instanceof BlockSlab;
      }
   }

   public static float getSlowed() {
      if (mc.thePlayer.getHeldItem() == null || ModuleManager.noSlow == null || !ModuleManager.noSlow.isEnabled()) {
         return 0.2F;
      } else if (swordOnly.isToggled() && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
         return 0.2F;
      } else if (mc.thePlayer.getHeldItem().getItem() instanceof ItemBow && disableBow.isToggled()) {
         return 0.2F;
      } else if (mc.thePlayer.getHeldItem().getItem() instanceof ItemSword && disableSword.isToggled()) {
         return 0.2F;
      } else if (mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion
         && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage())
         && disablePotions.isToggled()) {
         return 0.2F;
      } else if (fix && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
         return 0.2F;
      } else if (mode.getInput() == 0.0
         && vanillaMode.getInput() == 1.0
         && (!mc.thePlayer.onGround || Utils.jumpDown() || ModuleManager.bhop.isEnabled())
         && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
         return 0.2F;
      } else if (ModuleManager.killAura.hasAutoblocked
         || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)
         || !autoblockOnly.isToggled()
         || sword.getInput() == 2.0 && !hasClicked) {
         return ModuleManager.killAura.autoBlockMode.getInput() == 6.0
               && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword
               && ModuleManager.killAura.hasBlocked
            ? 0.2F
            : (100.0F - (float)slowed.getInput()) / 100.0F;
      } else {
         return 0.2F;
      }
   }

   @Override
   public String getInfo() {
      return this.modes[(int)mode.getInput()];
   }

   private void resetFloat() {
      this.noSlowing = false;
      fix = this.didC = this.requireJump = this.canFloat = this.jumped = this.md = this.setJump = this.setCancelled = false;
   }

   private boolean holdingUsable() {
      ItemStack itemStack = mc.thePlayer.getHeldItem();
      if (itemStack == null) {
         return false;
      } else {
         Item heldItem = itemStack.getItem();
         if (heldItem == null) {
            return false;
         } else {
            return !(heldItem instanceof ItemFood)
                  && !(heldItem instanceof ItemBucketMilk)
                  && (!(heldItem instanceof ItemBow) || !Utils.hasArrows(itemStack))
                  && (!(heldItem instanceof ItemPotion) || ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage()))
               ? sword.getInput() == 1.0 && heldItem instanceof ItemSword
               : true;
         }
      }
   }

   private boolean canFloat() {
      return !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInLava() && !mc.thePlayer.isInWater();
   }

   private ItemStack getMaxBook() {
      ItemStack stack = new ItemStack(Items.golden_apple);
      NBTTagCompound tag = new NBTTagCompound();
      NBTTagList pages = new NBTTagList();

      for (int i = 0; i < 50; i++) {
         pages.appendTag(new NBTTagString("NIGGERNIGGERNIGGERNIGGERNIGGERNIGGERNIGGERNIGGERNIGGERNIGGERNIGGERNIGGER"));
      }

      tag.setTag("pages", pages);
      tag.setString("author", "George Floyd");
      tag.setString("title", "History of the KKK");
      stack.setTagCompound(tag);
      return stack;
   }
}
