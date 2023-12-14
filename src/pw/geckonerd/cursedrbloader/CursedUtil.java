package pw.geckonerd.cursedrbloader;
import java.io.File;

public class CursedUtil {
	public static File getWorkDir() {
		switch(EnumOS.getOS())
		{
			case WINDOWS:
				return new File(System.getProperty("APPDATA"), "rubeta");
			case MACOS:
				return new File(System.getProperty("user.home"), "Library/Application/rubeta");
			case OTHER:
				return new File(System.getProperty("user.home"), "rubeta");
		}
		// should never happen
		return null;
	}
}

enum EnumOS {
	WINDOWS,
	MACOS,
	OTHER;
	
	public static EnumOS getOS() {
		String os = System.getProperty("os.name").toLowerCase();
		if(os.startsWith("mac"))
			return MACOS;
		if(os.startsWith("win"))
			return WINDOWS;
		return OTHER;
	}
	
	public String toString() {
		if(this.equals(WINDOWS)) {
			return "win";
		} else if(this.equals(OTHER)) {
			return "linux";
		} else {
			return "macos";
		}
	}
}
