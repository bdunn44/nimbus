package com.kbdunn.nimbus.common.async;

import com.kbdunn.nimbus.common.model.NimbusFile;

public interface UploadActionProcessor {
	public void handleUpload();
	public NimbusFile getUploadDirectory();
	public boolean userCanUploadFile(NimbusFile file);
	public void processFinishedMultiUpload();
	public void processFinishedUpload(NimbusFile file, boolean succeeded);
}