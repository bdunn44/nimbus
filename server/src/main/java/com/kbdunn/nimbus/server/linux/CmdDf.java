package com.kbdunn.nimbus.server.linux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class CmdDf {
	
	private static final Logger log = LogManager.getLogger(CmdDf.class.getName());
	
	public static void main(String[] args) {
		for (Filesystem fs: CmdDf.execute()) {
			System.out.printf("%s %s %s %s %s\n", fs.devicePath, fs.size, fs.used, fs.devicePath, fs.filesystemType);
		}
	}
	
	public static List<Filesystem> execute() {
		String command = "df --output=source,size,used,target,fstype";
		log.debug("Executing: " + command);
		
		Process p;
		List<Filesystem> result = null;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			result = new ArrayList<Filesystem>();
			String line = reader.readLine(); // Read the first header line
			while ((line = reader.readLine())!= null) 
				result.add(parseOutputLine(line));
			
		} catch (Exception e) {
			log.error(e, e);
		}
		
		return result;
	}
	
	public static Filesystem getByPath(String path) {
		for (Filesystem f : execute()) 
			if (f.devicePath.equals(path)) return f;
		return null;
	}
	
	private static Filesystem parseOutputLine(String line) {
		log.debug(line);
		Filesystem fs = new CmdDf().new Filesystem();
		String[] fields = line.split("\\s+");
		
		fs.devicePath = fields[0];
		fs.size = Long.valueOf(fields[1]) * 1024;
		fs.used = Long.valueOf(fields[2]) * 1024;
		fs.mountedPath = fields[3];
		fs.filesystemType = fields[4];
		
		return fs;
	}
	
	public class Filesystem {
		private String devicePath, mountedPath, filesystemType;
		private Long size, used;
		
		public String getDevicePath() {
			return devicePath;
		}
		
		public String getMountedPath() {
			return mountedPath;
		}
		
		public Long getSize() {
			return size;
		}
		
		public Long getUsed() {
			return used;
		}
		
		public String getFilesystemType() {
			return filesystemType;
		}
	}
}