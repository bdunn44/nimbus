package com.kbdunn.nimbus.server.linux;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.UsageInformation;

public abstract class CmdFree {
	
	private static final Logger log = LogManager.getLogger(CmdFree.class.getName());
	
	public static UsageInformation execute() {
		log.debug("Executing: free -b");
		
		final UsageInformation result = new UsageInformation();
		
		try {
			Process p = Runtime.getRuntime().exec("free -b");
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String line = "";
			while ((line = reader.readLine())!= null) {
				log.debug(line);
				if (!line.contains("buffers/cache")) continue;
				String[] fields = line.split("\\s+");
				result.setUsed(Long.valueOf(fields[2]));
				result.setTotal(result.getUsed() + Long.valueOf(fields[3]));
			}
			
		} catch (Exception e) {
			log.error(e, e);
		}
		return result;
	}
}