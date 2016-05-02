package com.kbdunn.nimbus.common.async;

import java.util.List;

public interface AsyncOperation {

	AsyncConfiguration getConfiguration();
	boolean started();
	boolean running();
	boolean finished();
	boolean succeeded();
	float getProgress();
	List<ProgressListener> getProgressListeners();
	List<FinishedListener> getFinishedListeners();
	void addProgressListener(ProgressListener listener);
	void removeProgressListener(ProgressListener listener);
	void addFinishedListener(FinishedListener listener);
	void removeFinishedListener(FinishedListener listener);
}