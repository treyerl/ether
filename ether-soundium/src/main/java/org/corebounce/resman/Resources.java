package org.corebounce.resman;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.corebounce.soundium.Subsystem;

public class Resources extends Subsystem {
	private final List<FileScanner> scanners = new ArrayList<>();

	public Resources(MetaDB db, PreviewFactory pf, String ... args) {
		super(CFG_PREFIX, args);

		for(int i = -1; ; i++) {
			String key = i < 0 ? "path" : "path"+i;
			String path = configuration.get(key);
			if(path == null) break;
			File dir = new File(path);
			if(dir.exists() && dir.isDirectory())
				scanners.add(new FileScanner(db, pf, dir));
		}
	}

	public static String   CFG_PREFIX = "res";
	public static String[] CFG_OPTIONS = {
			"path<n>=<path>","Path to resource directory"
	};
}
