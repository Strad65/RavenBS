package keystrokesmod.script;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import javax.tools.StandardJavaFileManager;
import keystrokesmod.utility.Utils;
import net.minecraft.launchwrapper.Launch;

public class Script {
   public String name;
   public Class clazz;
   public Object instance;
   public String scriptName;
   public String codeStr;
   public boolean error = false;
   public int STARTING_LINE;
   public ScriptEvents event;
   public File file;

   public Script(String name) {
      this.name = name;
      this.scriptName = "sc_" + name.replace(" ", "").replace(")", "_").replace("(", "_") + "_" + Utils.generateRandomString(5);
   }

   public boolean run() {
      try {
         if (this.scriptName != null && this.codeStr != null) {
            File file = new File(keystrokesmod.Raven.scriptManager.COMPILED_DIR);
            if (!file.exists() || !file.isDirectory()) {
               file.mkdir();
            }

            if (keystrokesmod.Raven.scriptManager.compiler == null) {
               return false;
            }

            ScriptDiagnosticListener bp = new ScriptDiagnosticListener();
            StandardJavaFileManager fileManager = keystrokesmod.Raven.scriptManager.compiler.getStandardFileManager(bp, null, null);
            ArrayList<String> compilationOptions = new ArrayList<>();
            compilationOptions.add("-d");
            compilationOptions.add(keystrokesmod.Raven.scriptManager.COMPILED_DIR);
            compilationOptions.add("-XDuseUnsharedTable");
            if (!(Boolean)Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
               compilationOptions.add("-classpath");
               String s = keystrokesmod.Raven.scriptManager.jarPath;

               try {
                  s = URLDecoder.decode(s, "UTF-8");
               } catch (UnsupportedOperationException var8) {
               }

               compilationOptions.add(s);
            }

            boolean success = keystrokesmod.Raven.scriptManager
               .compiler
               .getTask(
                  null, fileManager, bp, compilationOptions, null, Arrays.asList(new JavaSourceFromString(this.scriptName, this.codeStr, this.STARTING_LINE))
               )
               .call();
            if (!success) {
               this.error = true;
               return false;
            }

            try {
               SecureClassLoader secureClassLoader = new SecureClassLoader(new URL[]{file.toURI().toURL()}, Launch.classLoader);
               this.clazz = secureClassLoader.loadClass(this.scriptName);
               this.instance = this.clazz.newInstance();
               secureClassLoader.close();
               fileManager.close();
            } catch (Throwable e) {
               e.printStackTrace();
               Utils.sendMessage("&7Script &b" + Utils.extractFileName(this.name) + " &7blocked, &cunsafe code&7 detected!");
               this.error = true;
               return false;
            }

            return true;
         } else {
            return false;
         }
      } catch (Exception ex) {
         this.error = true;
         return !this.error;
      }
   }

   public int getBoolean(String s, Object... array) {
      if (this.clazz != null && this.instance != null) {
         Method method = null;

         for (Method method2 : this.clazz.getDeclaredMethods()) {
            if (method2.getName().equalsIgnoreCase(s) && method2.getParameterCount() == array.length && method2.getReturnType().equals(boolean.class)) {
               method = method2;
               break;
            }
         }

         if (method != null) {
            try {
               method.setAccessible(true);
               Object invoke = method.invoke(this.instance, array);
               if (invoke instanceof Boolean) {
                  return (Boolean)invoke ? 1 : 0;
               }
            } catch (IllegalAccessException | InvocationTargetException er) {
               Utils.sendMessage("&7Runtime error during script &b" + this.name);
               if (er.getCause() == null) {
                  Utils.sendMessage(" &7err: &cThrowable");
               } else {
                  Utils.sendMessage(" &7err: &c" + er.getCause().getClass().getSimpleName());
                  StackTraceElement[] stArr = er.getCause().getStackTrace();
                  if (stArr.length > 0) {
                     StackTraceElement st = stArr[0];

                     for (StackTraceElement element : er.getCause().getStackTrace()) {
                        if (element.getClassName().equalsIgnoreCase(this.scriptName)) {
                           st = element;
                           break;
                        }
                     }

                     Utils.sendMessage(" &7line: &c" + (st.getLineNumber() - this.STARTING_LINE));
                     Utils.sendMessage(" &7src: &c" + st.getMethodName());
                  }
               }
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public Float[] getFloatArray(String methodName, Object... args) {
      if (this.clazz != null && this.instance != null) {
         Method method = null;

         for (Method _method : this.clazz.getDeclaredMethods()) {
            if (_method.getName().equals(methodName) && _method.getReturnType().equals(Float[].class) && _method.getParameterCount() == args.length) {
               method = _method;
               break;
            }
         }

         if (method != null) {
            try {
               method.setAccessible(true);
               Object result = method.invoke(this.instance, args);
               if (result instanceof Float[]) {
                  return (Float[])result;
               }
            } catch (IllegalAccessException | InvocationTargetException er) {
               Utils.sendMessage("&7Runtime error during script &b" + this.name);
               if (er.getCause() == null) {
                  Utils.sendMessage(" &7err: &cThrowable");
               } else {
                  Utils.sendMessage(" &7err: &c" + er.getCause().getClass().getSimpleName());
                  StackTraceElement[] stArr = er.getCause().getStackTrace();
                  if (stArr.length > 0) {
                     StackTraceElement st = stArr[0];

                     for (StackTraceElement element : er.getCause().getStackTrace()) {
                        if (element.getClassName().equalsIgnoreCase(this.scriptName)) {
                           st = element;
                           break;
                        }
                     }

                     Utils.sendMessage(" &7line: &c" + (st.getLineNumber() - this.STARTING_LINE));
                     Utils.sendMessage(" &7src: &c" + st.getMethodName());
                  }
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public void delete() {
      this.clazz = null;
      this.instance = null;
      File file = new File(keystrokesmod.Raven.scriptManager.COMPILED_DIR + File.separator + this.scriptName + ".class");
      if (file.exists()) {
         file.delete();
      }
   }

   public void setCode(String code) {
      this.STARTING_LINE = 0;
      StringBuilder fileCodeContents = new StringBuilder();
      Iterator<String> iterator = keystrokesmod.Raven.scriptManager.imports.iterator();

      while (iterator.hasNext()) {
         this.STARTING_LINE++;
         fileCodeContents.append("import ").append(iterator.next()).append(";\n");
      }

      fileCodeContents.append("import keystrokesmod.script.model.*;\n");
      fileCodeContents.append("import keystrokesmod.script.packet.clientbound.*;\n");
      fileCodeContents.append("import keystrokesmod.script.packet.serverbound.*;\n");
      String name = Utils.extractFileName(this.name);
      this.codeStr = fileCodeContents
         + "public class "
         + this.scriptName
         + " extends "
         + ScriptDefaults.class.getName()
         + " {public static final "
         + ScriptDefaults.modules.class.getName().replace("$", ".")
         + " modules = new "
         + ScriptDefaults.modules.class.getName().replace("$", ".")
         + "(\""
         + name
         + "\");public static final String scriptName = \""
         + name
         + "\";\n"
         + code
         + "\n}";
      this.STARTING_LINE += 4;
   }

   public boolean invoke(String s, Object... array) {
      if (this.clazz != null && this.instance != null) {
         Method method = null;

         for (Method method2 : this.clazz.getDeclaredMethods()) {
            if (method2.getName().equalsIgnoreCase(s) && method2.getParameterCount() == array.length && method2.getReturnType().equals(void.class)) {
               method = method2;
               break;
            }
         }

         if (method != null) {
            try {
               method.setAccessible(true);
               method.invoke(this.instance, array);
               return true;
            } catch (IllegalAccessException | InvocationTargetException er) {
               Utils.sendMessage("&7Runtime error during script &b" + this.name);
               if (er.getCause() == null) {
                  Utils.sendMessage(" &7err: &cThrowable");
               } else {
                  Utils.sendMessage(" &7err: &c" + er.getCause().getClass().getSimpleName());
                  StackTraceElement[] stArr = er.getCause().getStackTrace();
                  if (stArr.length > 0) {
                     StackTraceElement st = stArr[0];

                     for (StackTraceElement element : er.getCause().getStackTrace()) {
                        if (element.getClassName().equalsIgnoreCase(this.scriptName)) {
                           st = element;
                           break;
                        }
                     }

                     Utils.sendMessage(" &7line: &c" + (st.getLineNumber() - this.STARTING_LINE));
                     Utils.sendMessage(" &7src: &c" + st.getMethodName());
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
