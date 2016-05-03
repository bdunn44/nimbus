package com.kbdunn.nimbus.desktop.sync;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hive2hive.core.api.interfaces.IH2HNode;
import org.hive2hive.core.processes.files.list.FileNode;
import org.hive2hive.processframework.exceptions.InvalidProcessStateException;
import org.hive2hive.processframework.exceptions.ProcessExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.desktop.sync.process.GetFileListprocess;

public class NetworkTester {
	
	private static final Logger log = LoggerFactory.getLogger(NetworkTester.class);
	
	private final IH2HNode node;
	
	public NetworkTester(IH2HNode node) {
		this.node = node;
	}
	
	public void printNetworkState() {
		log.debug("Printing current DHT state");
		for (Entry<String, FileNode> e : getFlattenedNetworkState().entrySet()) {
			log.debug("\t'{}' ({}) - ", e.getKey(), e.getValue().getMd5(), e.getValue().getUserPermissions());
		}
	}
	
	public Map<String, FileNode> getFlattenedNetworkState() {
		Map<String, FileNode> flat = new HashMap<>();
		addFileNodesRecursively(getNetworkState(), flat);
		return flat;
	}
	
	private void addFileNodesRecursively(FileNode currentNode, Map<String, FileNode> flatList) {
		if (currentNode == null) return;
		
		flatList.put(currentNode.getPath(), currentNode);
		if (currentNode.isFolder()) {
			for (FileNode childNode : currentNode.getChildren()) {
				addFileNodesRecursively(childNode, flatList);
			}
		}
	}
	
	public FileNode getNetworkState() {
		FileNode rootNode = null;
		try {
			long start = System.currentTimeMillis();
			rootNode = new GetFileListprocess(node.getFileManager()).execute();
			long end = System.currentTimeMillis();
			log.debug("Took {}ms to fetch network file list.", end-start);
		} catch (InvalidProcessStateException | ProcessExecutionException e) {
			log.error("Error fetching network file list.", e);
		}
		return rootNode;
	}
}
