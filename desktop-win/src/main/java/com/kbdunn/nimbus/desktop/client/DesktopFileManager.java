package com.kbdunn.nimbus.desktop.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.common.sync.interfaces.FileEventListener;
import com.kbdunn.nimbus.common.sync.interfaces.FileManager;
import com.kbdunn.nimbus.common.sync.model.SyncFile;
import com.kbdunn.nimbus.common.util.StringUtil;

public class DesktopFileManager implements FileManager {

	private static final Logger log = LoggerFactory.getLogger(DesktopFileManager.class);
	
	@Override
	public Callable<Void> createAddProcess(SyncFile file) throws IllegalArgumentException {
		return new Callable<Void>() {

		@Override
		public Void call() throws Exception {
			log.debug("Add process executed for {}", file);
			return null;
		}};
	}

	@Override
	public Callable<Void> createFolderAddProcess(List<SyncFile> folders) throws IllegalArgumentException {
		return new Callable<Void>() {

		@Override
		public Void call() throws Exception {
			log.debug("Create folder process executed for {} folders ", folders.size());
			return null;
		}};
	}

	@Override
	public Callable<Void> createDeleteProcess(SyncFile file) throws IllegalArgumentException {
		return new Callable<Void>() {

		@Override
		public Void call() throws Exception {
			log.debug("Delete process executed for {}", file);
			return null;
		}};
	}

	@Override
	public Callable<Void> createUpdateProcess(SyncFile file) throws IllegalArgumentException {
		return new Callable<Void>() {

		@Override
		public Void call() throws Exception {
			log.debug("Update process executed for {}", file);
			return null;
		}};
	}

	@Override
	public Callable<Void> createDownloadProcess(SyncFile file) throws IllegalArgumentException {
		return new Callable<Void>() {

		@Override
		public Void call() throws Exception {
			log.debug("Download process executed for {}", file);
			return null;
		}};
	}

	@Override
	public Callable<Void> createMoveProcess(SyncFile source, SyncFile destination) throws IllegalArgumentException {
		return new Callable<Void>() {

		@Override
		public Void call() throws Exception {
			log.debug("Move process executed from {} to {}", source, destination);
			return null;
		}};
	}

	@Override
	public Callable<List<SyncFile>> createFileListProcess() {
		return new Callable<List<SyncFile>>() {

		@Override
		public List<SyncFile> call() throws Exception {
			log.debug("Get file list process executed");
			List<SyncFile> list = new ArrayList<>();
			list.add(new SyncFile("first.txt", StringUtil.hexToBytes("D41D8CD98F00B204E9800998ECF8427A"), false));
			return list;
		}};
	}

	@Override
	public void subscribeFileEvents(FileEventListener listener) {
		log.debug("Subscribe to file events called");
	}

}
