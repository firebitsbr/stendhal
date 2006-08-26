// *********************************
//   unfinished experimental stuff
// *********************************

package games.stendhal.server.scripting;

import games.stendhal.server.StendhalServerExtension;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class ScriptingJava extends StendhalServerExtension {

	public void runIt(String className) throws MalformedURLException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		   // Create new class loader 
		   // with current dir as CLASSPATH
		   File file = new File(".");
		   ClassLoader loader = new URLClassLoader(new URL[] {file.toURL()});
		   // load class through new loader
		   Class aClass = loader.loadClass(className);
		   // run it
		   Object objectParameters[] = {new String[]{}};
		   Class classParameters[] = {objectParameters[0].getClass()};
		   Method theMethod = aClass.getDeclaredMethod("main", classParameters);
		   // Static method, no instance needed
		   theMethod.invoke(null, objectParameters);
	}
	
	
	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

}
