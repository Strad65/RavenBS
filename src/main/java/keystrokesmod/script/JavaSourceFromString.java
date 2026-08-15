package keystrokesmod.script;

import java.net.URI;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaFileObject.Kind;

public class JavaSourceFromString extends SimpleJavaFileObject {
   private final String code;
   public final String name;
   public int extraLines;

   @Override
   public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return this.code;
   }

   public JavaSourceFromString(String name, String code, int extraLines) {
      super(URI.create("string:///" + name + ".java"), Kind.SOURCE);
      this.code = code;
      this.name = name;
      this.extraLines = extraLines;
   }
}
