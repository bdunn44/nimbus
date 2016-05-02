package com.kbdunn.nimbus.common.server;

import java.util.List;

import com.kbdunn.nimbus.common.async.AsyncConfiguration;
import com.kbdunn.nimbus.common.async.AsyncOperation;
import com.kbdunn.nimbus.common.async.AsyncOperationQueue;
import com.kbdunn.nimbus.common.async.UploadActionProcessor;
import com.kbdunn.nimbus.common.async.VaadinUploadOperation;
import com.kbdunn.nimbus.common.exception.NimbusException;
import com.kbdunn.nimbus.common.model.Email;
import com.kbdunn.nimbus.common.model.FileConflict;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.StorageDevice;

public interface AsyncService {
	
	void startAsyncOperation(AsyncOperation operation);
	
	AsyncOperationQueue buildAsyncOperationQueue(String name, List<AsyncOperation> operations);
	
	AsyncOperation copyFile(AsyncConfiguration config, NimbusFile source, NimbusFile targetFolder, boolean startOperation);
	
	AsyncOperation copyFile(AsyncConfiguration config, NimbusFile source, NimbusFile targetFolder, List<FileConflict> resolutions, boolean startOperation);
	
	AsyncOperation copyFiles(AsyncConfiguration config, List<NimbusFile> sources, NimbusFile targetFolder, boolean startOperation);
	
	AsyncOperation copyFiles(AsyncConfiguration config, List<NimbusFile> sources, NimbusFile targetFolder, List<FileConflict> resolutions, boolean startOperation);

	AsyncOperation moveFile(AsyncConfiguration config, NimbusFile source, NimbusFile targetFolder, boolean startOperation);
	
	AsyncOperation moveFile(AsyncConfiguration config, NimbusFile source, NimbusFile targetFolder, List<FileConflict> resolutions, boolean startOperation);
	
	AsyncOperation moveFiles(AsyncConfiguration config, List<NimbusFile> sources, NimbusFile targetFolder, boolean startOperation);
	
	AsyncOperation moveFiles(AsyncConfiguration config, List<NimbusFile> sources, NimbusFile targetFolder, List<FileConflict> resolutions, boolean startOperation);
	
	AsyncOperation reconcileStorageDevice(AsyncConfiguration config, StorageDevice device, boolean startOperation);
	
	void pauseStorageDeviceReconciliations();

	void resumeStorageDeviceReconciliations();

	AsyncOperation sendEmail(AsyncConfiguration config, Email email, boolean startOperation) throws NimbusException;

	AsyncOperation sendPasswordResetEmail(AsyncConfiguration config, NimbusUser from, String subject, String message,
			String nimbusUrl, NimbusUser recipient, boolean startOperation) throws NimbusException;

	AsyncOperation sendInvitationEmail(AsyncConfiguration config, NimbusUser from, String subject, String message,
			String nimbusUrl, String recipientEmail, boolean startOperation) throws NimbusException;
	
	AsyncOperation updateMediaLibrary(AsyncConfiguration config, NimbusUser user, boolean startOperation);
	
	VaadinUploadOperation uploadFile(String fileName, long fileSize, UploadActionProcessor controller); // Vaadin Drag & Drop - is started automatically
	
	VaadinUploadOperation uploadFile(UploadActionProcessor controller); // Vaadin Classic Upload - is started automatically
}
