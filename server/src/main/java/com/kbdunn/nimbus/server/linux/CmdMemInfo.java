package com.kbdunn.nimbus.server.linux;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.MemoryInformation;

public abstract class CmdMemInfo {
	
	private static final Logger log = LogManager.getLogger(CmdMemInfo.class.getName());
	
	public static MemoryInformation execute() {
		log.debug("Executing: grep Mem /proc/meminfo");
		
		final MemoryInformation result = new MemoryInformation();
		
		try {
			Process p = Runtime.getRuntime().exec("grep Mem /proc/meminfo");
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String line = "";
			while ((line = reader.readLine())!= null) {
				log.debug(line);
				String[] fields = line.split("\\s+");

				if (fields[0].equals("MemTotal:")) 
					result.setTotal(Long.valueOf(Long.valueOf(fields[1])*1024L));
				else if (fields[0].equals("MemFree:")) 
					result.setUsed(Long.valueOf(Long.valueOf(fields[1])*1024L));
			}

		} catch (Exception e) {
			log.error(e, e);
		}
		return result;
	}
}