package pw.geckonerd.cursedrbloader;

public abstract class ModBase {
	public abstract String[] getDependencies();
	public abstract String getModID();
	public abstract int getVersion();
	public abstract String getTargetClientVersion();
	
	public abstract void onEarlyEnable() throws Exception;

	protected void log(String s)  { System.out.println("[" + getModID() + "] [INFO] "  + s); }
	protected void warn(String s) { System.out.println("[" + getModID() + "] [WARN] "  + s); }
	protected void err(String s)  { System.out.println("[" + getModID() + "] [ERROR] " + s); }
}
