package pw.geckonerd.cursedrbloader;
import java.io.File;
import java.io.FileInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class CursedRBLoader {
	public static final String version = "v0.1";
	public static final String clientVersion = "20231029";
	public static URLClassLoader loader;
	
	public static void premain(String agentargs, Instrumentation inst) throws Exception
	{
		log("CursedRBLoader " + version + " for client " + clientVersion + " is starting");
		instrumentation = inst;
		
		log("Collecting classes...");
		ModLoadingPair[] mlp = collectMods(new File(CursedUtil.getWorkDir(), "mods_cursed"));
		
		log("Loading mods...");
		loadMods(mlp);
		
		log("Verifying dependencies...");
		verifyDependencies();
		
		log("Initializing mods...");
		initMods();
		
		log("Injecting CursedRB stuff");
		crbInject();
		
		log("Finished, launching RuBeta");
	}
	
	private static void crbInject() throws Exception {
		CursedRBLoader.inject(Class.forName("bj", false, loader), new CursedInjection() {
			@Override public void transform(ClassPool pool, CtClass clazz) throws Exception {
				CtMethod m = clazz.getDeclaredMethod("a", new CtClass[] {CtClass.intType, CtClass.intType, CtClass.floatType});
        		m.insertAfter("{"
        					 + "	b(this.cm, \"CursedRBLoader " + version + "/" + clientVersion + "\", 2, 14, 16777215);"
        					 + "	b(this.cm, \"Loaded " + modList.size() + " mods\", 2, 26, 16777215);"
        					 + "}");
		}});
	}
	
	private static void initMods() throws Exception {
		for(ModBase mod : modList)
		{
			Method modEnableMethod = ModBase.class.getDeclaredMethod("onEarlyEnable", new Class[] {});
			modEnableMethod.invoke(mod, new Object[] {});
		}
	}
	
	private static Attributes getModAttributes(File mod) throws Exception
	{
		JarInputStream js = new JarInputStream(new FileInputStream(mod));
		Manifest mf = js.getManifest();
		Attributes attr = mf.getMainAttributes();
		
		js.close();
		return attr;
	}
	
	private static void verifyDependencies() {
		ArrayList<String> modIDs = new ArrayList<String>();
		for(ModBase mod : modList)
		{
			if(modIDs.contains(mod.getModID()))
			{
				throw new RuntimeException("Mod with ID '" + mod.getModID() + "' already exists!");
			}
			modIDs.add(mod.getModID());
		}
		for(ModBase mod : modList)
		{
			for(String id : mod.getDependencies())
			{
				if(!modIDs.contains(id))
				{
					throw new RuntimeException("Mod '" + mod.getModID() + "' requires '" + id + "', but it was not found.");
				}
			}
			if(!mod.getTargetClientVersion().equals(clientVersion))
			{
				warn("Mod " + mod.getModID() + " has incompatible client version! This mod was made for '" + mod.getTargetClientVersion() + "', however, CursedRBLoader was"
						+ " designed for " + clientVersion + "! Use at your own risk.");
			}
		}
	}
	
	private static ModLoadingPair[] collectMods(File folder) throws Exception {
		folder.mkdir();
		ArrayList<ModLoadingPair> mlp = new ArrayList<ModLoadingPair>();
		
		for(File mod : folder.listFiles())
		{
			if(!(mod.isFile() && mod.getName().endsWith(".jar")))
			{
				err("skipping " + mod + " - not a jar");
				continue;
			}
			try {
				Attributes attr = getModAttributes(mod);
				String cn =  attr.getValue("Horizon-Main");
				mlp.add(new ModLoadingPair(mod, cn));
			}
			catch (Exception e) {
				err("Failed to load mod " + mod + " - " + e);
			}
		}
		
		return mlp.toArray(new ModLoadingPair[mlp.size()]);
	}
	
	private static void loadMods(ModLoadingPair[] mlp) throws Exception{
		ArrayList<URL> jars = new ArrayList<URL>();
		for(ModLoadingPair ml : mlp)
		{
			jars.add(ml.jar.toURI().toURL());
		}
		loader = new URLClassLoader(jars.toArray(new URL[jars.size()]), CursedRBLoader.class.getClassLoader());
		
		for(ModLoadingPair ml : mlp)
		{
			
			try {
				Class<?> mainClass = loader.loadClass(ml.className);
				if(!ModBase.class.isAssignableFrom(mainClass))
				{
					err("Failed to load mod " + ml.jar + " - main class is not ModBase");
					continue;
				}
				ModBase modInstance = (ModBase)mainClass.getConstructor(new Class[] {}).newInstance(new Object[] {});
				modList.add(modInstance);
				log("Loaded " + ml.jar);
			}
			catch (Exception e) {
				e.printStackTrace();
				err("Failed to load mod " + ml.jar + " - " + e);
			}
		}
	}
	
	public static ArrayList<ModBase> modList = new ArrayList<ModBase>();
	private static HashMap<String, ArrayList<Method>> events = new HashMap<String, ArrayList<Method>>();
	public static Instrumentation instrumentation;
	
	public static void dispatchEvent(Event e)
	{
		ArrayList<Method> handlers = events.get(e.getEventID());
		if(handlers == null)
		{
			return;
		}
		
		for(Method handler : handlers)
		{
			try {
				handler.invoke(null, new Object[] {e});
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public static void registerCallback(String event, Method callback)
	{
		ArrayList<Method> handlers = events.get(event);
		if(handlers == null)
		{
			events.put(event, new ArrayList<Method>());
		}
		events.get(event).add(callback);
	}
	
	public static void registerListener(Class<?> clazz)
	{
		Method[] methods = clazz.getMethods();
		for(Method method : methods)
		{
			Parameter[] params = method.getParameters();
			if(params.length != 1)
				continue;
			Class<?> paramClass = params[0].getType();
			if(Event.class.isAssignableFrom(paramClass))
			{
				registerCallback(paramClass.getName(), method);
			}
		}
	}
	
	public static void transformClass(Class<?> targetClass, Instrumentation inst, Class<?> transformer) throws Exception {
		transformClass(targetClass, targetClass.getClassLoader(), inst, transformer);
	}
	
	public static void inject(Class<?> target, CursedInjection injector) throws Exception
	{
		instrumentation.addTransformer((ClassFileTransformer) new CursedInjectionTransformer(target.getName(), target.getClassLoader(), injector), true);
		instrumentation.retransformClasses(target);
	}

	public static void transformClass(Class<?> targetClass, Class<?> transformer) throws Exception {
		transformClass(targetClass, targetClass.getClassLoader(), instrumentation, transformer);
	}
	
	public static void transformClass(Class<?> targetClass, ClassLoader classLoader, Instrumentation inst, Class<?> transformer) throws Exception {
		Object tf = transformer.getConstructor(new Class[] { String.class, ClassLoader.class }).newInstance(targetClass.getName(), classLoader);
		inst.addTransformer((ClassFileTransformer) tf, true);
		inst.retransformClasses(targetClass);
	}
	
	public static Object getModByID(String id) {
		for(ModBase mod : modList)
		{
			if(mod.getModID().equals(id))
				return mod;
		}
		return null;
	}
	
	public static void log(String s)  { System.out.println("[CursedRBLoader] [INFO] "  + s); }
	public static void warn(String s) { System.out.println("[CursedRBLoader] [WARN] "  + s); }
	public static void err(String s)  { System.out.println("[CursedRBLoader] [ERROR] " + s); }
}

class ModLoadingPair {
	public File jar;
	public String className;
	
	public ModLoadingPair(File jar, String className)
	{
		this.jar = jar;
		this.className = className;
	}
}
