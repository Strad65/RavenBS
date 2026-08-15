package keystrokesmod.module.setting.impl;

import com.google.gson.JsonObject;
import keystrokesmod.module.setting.Setting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class KeySetting extends Setting {
   private int key;
   public GroupSetting group;

   public KeySetting(String name, int key) {
      super(name);
      this.key = key;
   }

   public KeySetting(GroupSetting group, String name, int key) {
      super(name);
      this.group = group;
      this.key = key;
   }

   public int getKey() {
      return this.key;
   }

   @Override
   public String getName() {
      return super.getName();
   }

   public void setKey(int key) {
      this.key = key;
   }

   public boolean isPressed() {
      if (this.getKey() == 0) {
         return false;
      } else {
         return this.getKey() >= 1000 ? Mouse.isButtonDown(this.getKey() - 1000) : Keyboard.isKeyDown(this.getKey());
      }
   }

   @Override
   public void loadProfile(JsonObject data) {
      if (data.has(this.getName()) && data.get(this.getName()).isJsonPrimitive()) {
         int keyValue = this.key;

         try {
            keyValue = data.getAsJsonPrimitive(this.getName()).getAsInt();
         } catch (Exception var4) {
         }

         this.key = keyValue;
      }
   }
}
