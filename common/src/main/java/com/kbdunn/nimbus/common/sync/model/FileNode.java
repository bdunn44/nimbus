package com.kbdunn.nimbus.common.sync.model;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FileNode {

	private final FileNode parent;
	private final List<FileNode> children;
	private final String path;
	private final File file;
	private final byte[] md5;

	public FileNode(FileNode parent, File file, String path, byte[] md5) {
		this.parent = parent;
		this.file = file;
		this.path = path;
		this.md5 = md5;
		this.children = new ArrayList<FileNode>();
	}

	public FileNode getParent() {
		return parent;
	}

	public List<FileNode> getChildren() {
		if (isFile()) {
			return null;
		}
		return children;
	}
	
	public void addChild(FileNode child) {
		children.add(child);
	}
	
	public void addChildren(List<FileNode> children) {
		this.children.addAll(children);
	}

	public String getName() {
		return file.getName();
	}

	public String getPath() {
		return path;
	}

	public File getFile() {
		return file;
	}

	public boolean isFile() {
		if (file.exists()) {
			return file.isFile();
		} else {
			return md5 != null;
		}
	}

	public boolean isFolder() {
		return !isFile();
	}

	public byte[] getMd5() {
		return md5;
	}

	@Override
	public String toString() {
		return String.format("%s: %s [%s] %s", isFile() ? "File" : "Folder", getPath(), 
				isFile() ? String.format("(MD5: %s)", byteToHex(getMd5())) : "");
	}

	public List<FileNode> toFlatNodeList(boolean addFiles, boolean addFolders) {
		return FileNode.getFlatNodeList(this, addFiles, addFolders);
	}
	
	/**
	 * Returns a list of all nodes in preorder. One can specify whether only files, folders or both is desired
	 */
	public static List<FileNode> getFlatNodeList(FileNode root, boolean addFiles, boolean addFolders) {
		if (!addFiles && !addFolders) {
			throw new IllegalArgumentException("Must visit either files, folders or both");
		}
		List<FileNode> set = new LinkedList<>();
		flattenNode(root, set, addFiles, addFolders);
		return set;
	}
	
	private static void flattenNode(FileNode current, List<FileNode> list, boolean addFiles, boolean addFolders) {
		if (current == null) {
			return;
		}
		
		if (current.isFile() && addFiles || current.isFolder() && addFolders) {
			list.add(current);
		}
		
		if (current.isFolder()) {
			for (FileNode child : current.getChildren()) {
				flattenNode(child, list, addFiles, addFolders);
			}
		}
	}

	private static String byteToHex(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (byte b : data) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}
}
