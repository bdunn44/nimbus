package com.kbdunn.nimbus.server.linux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.LinuxBlock;

public class CmdHwinfo {
	
	private static final String CMD = "/usr/sbin/hwinfo --block";
	private static final Logger log = LogManager.getLogger(CmdHwinfo.class);
	
	public static void main(String[] args) {
		for (LinuxBlock b : CmdHwinfo.execute(args)) {
			System.out.printf("%s %s %s %s\n", b.getPath(), b.getLabel(), b.getType(), b.getUuid());
		}
	}
	
	public static List<LinuxBlock> execute(String... args) {
		String command = CMD;
		for (String arg : args) {
			command += " " + arg;
		}
		command += " | egrep 'Partition|Files|Model'";
		log.debug("Executing: " + command);
		
		Process p;
		List<LinuxBlock> disks = new ArrayList<>();
		List<LinuxBlock> partitions = new ArrayList<>();
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String line = "";
			String blkinfo = "";
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) continue;
				if (line.startsWith("  ")) { 
					// This is information about a parent block
					blkinfo += "|" + line.trim();
				} else {
					// This is the header line for a block
					if (!blkinfo.isEmpty()) {
						// Add the previous info string to our lists
						if (blkinfo.startsWith("D")) {
							disks.add(parseBlockInfo(blkinfo));
						} else {
							partitions.add(parseBlockInfo(blkinfo));
						}
					}
					// Start a new info string
					blkinfo = line.substring(line.lastIndexOf(" ") + 1).equals("Disk") ? "D" : "P";
				}
			}
			
			// Add the last info string to our lists
			if (!blkinfo.isEmpty()) {
				if (blkinfo.startsWith("D")) {
					disks.add(parseBlockInfo(blkinfo));
				} else {
					partitions.add(parseBlockInfo(blkinfo));
				}
			}
		} catch (Exception e) {
			log.error(e, e);
		}
		
		Iterator<LinuxBlock> i = partitions.iterator();
		while (i.hasNext()) {
			LinuxBlock part = i.next();
			// Remove if no info
			if (part.getPath() == null) {
				i.remove();
				continue;
			}
			
			// Collate disk and partition information
			if (part.getLabel() == null && part.getPath() != null) {
				String ppath = part.getPath();
				for (LinuxBlock disk : disks) {
					String dpath = disk.getPath();
					if (dpath == null) continue;
					if (ppath.startsWith(dpath)) {
						part.setLabel(disk.getLabel());
					}
				}
			}
			
			if (part.getLabel().equals("Partition")) part.setLabel(null);
		}
		
		return partitions;
	}
	
	private static LinuxBlock parseBlockInfo(String blkinfo) {
		blkinfo = blkinfo.substring(2); // Remove D| or P| prefix
		LinuxBlock b = new LinuxBlock();
		// Loop through info lines
		String k = "";
		String v = "";
		String[] l = { };
		for (String s : blkinfo.split("\\|")) {
			if (s.indexOf(":") == -1) continue;
			k = s.substring(0, s.indexOf(":"));
			v = s.substring(s.indexOf(":") + 1, s.length()).trim();
			if (k.isEmpty() || v.isEmpty()) continue;
			if (k.equals("Device Files")) {
				l = v.split(",");
				b.setPath(l[0]);
				for (String f : l) {
					if (f.trim().startsWith("/dev/disk/by-uuid/")) {
						b.setUuid(f.substring(f.lastIndexOf("/") + 1));
					} else if (f.trim().startsWith("/dev/disk/by-label/")) {
						b.setLabel(f.substring(f.lastIndexOf("/") + 1));
					}
				}
			} else if (k.equals("Model")) {
				// Prefer the label from /dev/disk/by-label
				if (b.getLabel() == null) b.setLabel(v.replace("\"", ""));
			}
		}
		return b;
	}
}
