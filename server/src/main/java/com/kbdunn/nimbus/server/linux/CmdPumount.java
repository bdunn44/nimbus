package com.kbdunn.nimbus.server.linux;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.HardDrive;

public abstract class CmdPumount {
	
	private static final Logger log = LogManager.getLogger(CmdPumount.class.getName());
	
	public static boolean unmount(HardDrive drive) {
		return unmount(drive.getPath());
	}
	
	public static boolean unmount(String path) {
		String command = "pumount " + path;
		log.debug("Executing: " + command);
		
		try {
			Process p;
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			return p.exitValue() == 0;
		} catch (Exception e) {
			log.error(e, e);
		}
		return false;
	}
}