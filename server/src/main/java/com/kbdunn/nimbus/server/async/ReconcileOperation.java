package com.kbdunn.nimbus.server.async;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.async.AsyncConfiguration;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.service.LocalFileService;
import com.kbdunn.nimbus.server.service.LocalStorageService;
import com.kbdunn.nimbus.server.service.LocalUserService;

public class ReconcileOperation extends AsyncServerOperation {
	
	private static final Logger log = LogManager.getLogger(ReconcileOperation.class.getName());
	private static final List<StorageDevice> runningReconciliations = new ArrayList<StorageDevice>();
	private static boolean paused = false;
	
	private StorageDevice device;
	private LocalStorageService storageService;
	private LocalFileService fileService;
	private LocalUserService userService;
	
	public ReconcileOperation(AsyncConfiguration config, StorageDevice device) { 
		super(config);
		this.device = device;
		fileService = NimbusContext.instance().getFileService();
		storageService = NimbusContext.instance().getStorageService();
		userService = NimbusContext.instance().getUserService();
		super.getConfiguration().setName("Reconciling hard drive " + device);
	}
	
	public static synchronized void setReconciliationsPaused(boolean isPaused) {
		paused = isPaused;
		if (paused && runningReconciliations.size() > 0)
			log.info("Pausing all hard drive reconciliation jobs for 10 minutes");
	}
	
	private static synchronized void setReconciliationFinished(StorageDevice device) {
		runningReconciliations.remove(device);
	}
	
	@Override
	public void doOperation() throws Exception {
		if (runningReconciliations.contains(device)) {
			log.warn("Reconciliation already running for hard drive " + device);
			super.setSucceeded(false);
			super.setProgress(1f);
			return;
		}
		runningReconciliations.add(device);
		
		log.info("Running reconciliation for hard drive " + device);
		
		List<NimbusUser> deviceUsers = storageService.getUsersAssignedToStorageDevice(device);
		float incr = .95f/deviceUsers.size();
		
		for (NimbusUser nu : deviceUsers) {
			if (super.getProgress() == 1f) return; // failed
			reconcileFolder(userService.getUserRootFolder(nu, device));
			super.setProgress(super.getProgress() + incr);
		}
		device.setReconciled(true);
		storageService.save(device);
		setReconciliationFinished(device);
		super.setSucceeded(true);
		log.info("Reconciliation finished for " + device);
	}
	
	private void reconcileFolder(NimbusFile folder) {
		checkPause();
		if (super.getProgress() == 1f) return; // failed
		
		// skip if already reconciled and drive isn't autonomous
		if (!folder.isReconciled() || fileService.getStorageDevice(folder).isAutonomous()) {
			if (!fileService.reconcileFolder(folder)) {
				log.error("There was an error reconciling folder " + folder);
				super.setSucceeded(true);
				setProgress(1f);
				return;
			}
		}
		
		for (NimbusFile subFolder : fileService.getFolderContents(folder)) {
			reconcileFolder(subFolder);
		}
	}
	
	private void checkPause() {
		if (paused) { // sleep 10 minutes
			try {
				Thread.sleep(10 * 60 * 1000);
			} catch (InterruptedException e) {
				log.error(e, e);
			}
		}
	}
}