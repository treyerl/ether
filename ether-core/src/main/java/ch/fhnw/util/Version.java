package ch.fhnw.util;

public class Version {
	public static final int WINDOWS_POST_XP_MAJOR = 6;
	public static final int WINDOWS_VISTA_MINOR   = 0;
	public static final int WINDOWS_7_MINOR       = 1;
	public static final int WINDOWS_8_MINOR       = 2;

	public enum OS {
		UNKNOWN("?"), LINUX("linux"), WINDOWS("win32"), MACOSX("macosx");

		private String shortString;

		OS(String shortString) {
			this.shortString = shortString;
		}

		public String shortString() {
			return shortString;
		}

		private static OS[] valuesro;
		public static final OS[] valuesro() {
			if(valuesro == null) valuesro = values();
			return valuesro;
		}
	}

	public enum Architecture {
		UNKNOWN(32), X86(32), X86_64(64);
		int wordSize;
		private Architecture(int wordSize) {
			this.wordSize = wordSize;
		}
	}

	private static OS  os;
	private static int osVersionMajor = -1;
	private static int osVersionMinor = -1;
	private static Architecture architecture;
	private static final int WORD_SIZE = System.getProperty("os.arch").endsWith("64") ? 64 : 32; 


	public static OS getOS() {
		if(os == null) {
			if(System.getProperty("os.name").toLowerCase().startsWith("linux"))
				os = OS.LINUX;
			else if(System.getProperty("os.name").toLowerCase().startsWith("win"))
				os = OS.WINDOWS;
			else if(System.getProperty("os.name").toLowerCase().startsWith("mac os x"))
				os = OS.MACOSX;
			else
				os = OS.UNKNOWN;
		}
		return os;
	}

	public static Architecture getArchitecture() {
		if (architecture == null) {
			String arch = "";
			arch = System.getProperty("os.arch").replace("amd","x86-");
			if (arch.equals("x86")) architecture = Architecture.X86;
			else if (arch.equals("x86-64")) architecture = Architecture.X86_64;
			else architecture = Architecture.UNKNOWN;
		}
		return architecture;
	}

	public static int getWordSize() {
		return WORD_SIZE;
	}

	public static int getOSMajor() {
		if(osVersionMajor == -1) {
			try {
				String[] version = System.getProperty("os.version").split("[.,/;]");
				osVersionMajor   = Integer.parseInt(version[0].trim());
			} catch(Throwable t) {
				osVersionMajor = 0;
			}
		}
		return osVersionMajor;
	}

	public static int getOSMinor() {
		if(osVersionMinor == -1) {
			try {
				String[] version = System.getProperty("os.version").split("[.,/;]");
				osVersionMinor   = Integer.parseInt(version[1].trim());
			} catch(Throwable t) {
				osVersionMinor = 0;
			}
		}
		return osVersionMinor;
	}

	public static boolean greaterOrEqual(int major, int minor) {
		long current = getOSMajor();
		current <<= 32L;
		current |= getOSMinor();

		long majMin = major;
		majMin <<= 32L;
		majMin |= minor;


		return current >= majMin;
	}

	public static boolean isWindowsVista() {
		return getOS() == OS.WINDOWS && getOSMajor() == WINDOWS_POST_XP_MAJOR && getOSMinor() == WINDOWS_VISTA_MINOR;
	}

	public static boolean isWindows7() {
		return getOS() == OS.WINDOWS && getOSMajor() == WINDOWS_POST_XP_MAJOR && getOSMinor() == WINDOWS_7_MINOR;
	}
	
	private Version() {
	}	
}