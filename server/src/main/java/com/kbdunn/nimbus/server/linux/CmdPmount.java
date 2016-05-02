package com.kbdunn.nimbus.server.linux;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.HardDrive;

public abstract class CmdPmount {
	
	private static final Logger log = LogManager.getLogger(CmdPmount.class.getName());
	
	public static boolean mount(HardDrive drive) {
		String command = "pmount -w " + drive.getDevicePath() + " nimbus-" + drive.getUuid();
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