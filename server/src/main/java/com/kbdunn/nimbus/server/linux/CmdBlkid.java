package com.kbdunn.nimbus.server.linux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.LinuxBlock;

public class CmdBlkid {
	
	private static final String CMD = "/sbin/blkid";
	private static final Logger log = LogManager.getLogger(CmdBlkid.class.getName());
	
	public static void main(String[] args) {
		for (LinuxBlock b : CmdBlkid.execute(args)) {
			System.out.printf("%s %s %s %s\n", b.getPath(), b.getLabel(), b.getType(), b.getUuid());
		}
	}
	
	public static List<LinuxBlock> execute(String... args) {
		String command = CMD;
		for (String arg : args) {
			command += " " + arg;
		}
		log.debug("Executing: " + command);
		
		Process p;
		List<LinuxBlock> result = null;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			result = new ArrayList<LinuxBlock>();
			String line = "";
			while ((line = reader.readLine())!= null) {
				result.add(parseOutputLine(line));
			}
		} catch (Exception e) {
			log.error(e, e);
		}
		return result;
	}
	
	private static LinuxBlock parseOutputLine(String line) {
		log.debug(line);
		LinuxBlock b = new LinuxBlock();
		b.setPath(line.substring(0, line.indexOf(":"))); // /dev/sda1:
		
		Pattern p = Pattern.compile("^.*(LABEL|UUID|TYPE)=\"(.+)\".*$");
		Matcher m;
		String k, v;
		while ((m = p.matcher(line)).matches()) {
			k = m.group(1);
			v = m.group(2);
			if (k.equals("LABEL")) b.setLabel(v);
			else if (k.equals("UUID")) b.setUuid(v);
			else if (k.equals("TYPE")) b.setType(v);
			line = line.replace(k + "=\"" + v + "\"", "");
		}
		
		/*for (String s : line.split("\\s+")) {
			if (s.endsWith(":")) {
				b.path = s.substring(0, s.length() - 1);  // /dev/sda1:
			} else if (s.startsWith("LABEL")) {
				b.label = s.substring(7, s.length() - 1); // LABEL="NIMBUS01"
			} else if (s.startsWith("UUID")) {
				b.uuid = s.substring(6, s.length() - 1);  // UUID="A036EFCC36EFA190"
			} else if (s.startsWith("TYPE")) {
				b.type = s.substring(6, s.length() - 1);  // TYPE="ntfs"
			}
		}*/
		return b;
	}
}