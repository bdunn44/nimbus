package com.kbdunn.nimbus.server.linux;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.LinuxBlock;

public class BlockFinder {
	
	private static final Logger log = LogManager.getLogger(BlockFinder.class);

	public static void main(String[] args) {
		for (LinuxBlock b : new BlockFinder().scan()) {
			System.out.printf("\t%s %s %s %s\n", b.getPath(), b.getLabel(), b.getType(), b.getUuid());
		}
	}
	
	public List<LinuxBlock> scan() {
		log.debug("Scanning for linux blocks...");
		final List<LinuxBlock> result = new ArrayList<>();
		result.addAll(CmdBlkid.execute());
		final int blkid = result.size();
		log.debug("blkid found " + blkid + " block(s)");
		if (blkid == 0) {
			result.addAll(CmdHwinfo.execute());
			log.debug("hwinfo found " + (result.size()-blkid ) + " block(s)");
		}
		return result;
	}
}
