package com.kbdunn.nimbus.server.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.service.LocalFileService;

public class ZipCompress {
	
	private static final long MAX_IN_MEMORY_SIZE = 209715200; // 200 MB
	private LocalFileService fileService;
	private HashMap<String, NimbusFile> fileMap;
	private long totalSize;
	
	public ZipCompress(List<NimbusFile> NFileList) {
		fileService = NimbusContext.instance().getFileService();
		fileMap = new HashMap<String, NimbusFile>();
		totalSize = 0;
		for (NimbusFile NFile: NFileList)
			addZipEntry(NFile, "");
	}
	
	public InputStream getInputStream() {
		if (totalSize < MAX_IN_MEMORY_SIZE) {
			return buildArchiveInMemory();
		} else {
			return buildArchiveOnDisk();
		}
	}
	
	private InputStream buildArchiveInMemory() {
		byte[] buffer = new byte[1024];
		
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ZipOutputStream zos = new ZipOutputStream(baos);
			
			Iterator<Entry<String, NimbusFile>> it = fileMap.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, NimbusFile> zipEntry = (Entry<String, NimbusFile>) it.next();
				
				ZipEntry ze= new ZipEntry(zipEntry.getKey());
				zos.putNextEntry(ze);
				FileInputStream in = new FileInputStream(zipEntry.getValue().getPath());
				
				int len;
				while ((len = in.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}
				in.close();
			}
			
			zos.closeEntry();
			zos.close();
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			return bais;
		} catch(IOException ex) {
			ex.printStackTrace(); 
			return null;
		}
	}
	
	// TODO: Write to tmp location, delete on session destroy? or periodically
	private InputStream buildArchiveOnDisk() {
		return null;
	}
	
	private void addZipEntry(NimbusFile node, String parentEntry) {
    	String entry = parentEntry.isEmpty() ? node.getName() : parentEntry + "/" + node.getName();
		
		if(!node.isDirectory()) {
			fileMap.put(entry, node);
			totalSize += node.getSize();
		} else {
			for(NimbusFile childNode : fileService.getContents(node)){
				addZipEntry(childNode, entry);
			}
		}
	}
}