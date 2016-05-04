package com.kbdunn.nimbus.desktop.sync;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

public class NimbusFileObserver {
	
	public static final long DEFAULT_OBSERVER_INTERVAL = 3000;
	
	private final FileAlterationObserver observer;
	private final FileAlterationMonitor monitor;

	private boolean isRunning;
	
	/**
	 * A file observer that uses the specified interval to check for file changes.
	 *
	 * @param rootDirectory
	 * @param ms
	 */
	public NimbusFileObserver(File rootDirectory, long ms) {
	  this.observer = new FileAlterationObserver(rootDirectory);
	  this.monitor = new FileAlterationMonitor(ms, observer);
	}
	
	/**
	 * A file observer that uses the default interval to check for file changes.
	 *
	 * @param rootDirectory
	 */
	public NimbusFileObserver(File rootDirectory) {
		this(rootDirectory, DEFAULT_OBSERVER_INTERVAL);
	}
	
	public void start() throws Exception {
		if (!isRunning) {
			monitor.start();
			isRunning = true;
		}
	}
	
	public void stop() throws Exception {
		if (isRunning) {
			monitor.stop();
			isRunning = false;
		}
	}

	public void addFileObserverListener(FileAlterationListener listener) {
		observer.addListener(listener);
	}

	public void removeFileObserverListener(FileAlterationListener listener) {
		observer.removeListener(listener);
	}

	public List<FileAlterationListener> getFileObserverListeners() {
		List<FileAlterationListener> listeners = new ArrayList<>();

		for (FileAlterationListener listener : observer.getListeners()) {
			listeners.add(listener);
		}
		return listeners;
	}

	public long getInterval() {
		return monitor.getInterval();
	}

	public boolean isRunning() {
		return isRunning;
	}
}
