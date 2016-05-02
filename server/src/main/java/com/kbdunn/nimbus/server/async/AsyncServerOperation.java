package com.kbdunn.nimbus.server.async;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.async.AsyncConfiguration;
import com.kbdunn.nimbus.common.async.AsyncOperation;
import com.kbdunn.nimbus.common.async.FinishedListener;
import com.kbdunn.nimbus.common.async.ProgressListener;

public abstract class AsyncServerOperation implements AsyncOperation, Runnable  {

	private static final Logger log = Logger.getLogger(AsyncServerOperation.class.getName());
	
	private AsyncConfiguration config;
	private boolean started = false;
	private boolean running = false;
	private boolean succeeded = false;
	private float progress = 0f;
	private float lastProgress = 0f;
	private long lastNotifyTime = 0L;
	private List<ProgressListener> progressListeners = new ArrayList<ProgressListener>();
	private List<FinishedListener> finishedListeners = new ArrayList<FinishedListener>();
	
	public AsyncServerOperation(AsyncConfiguration config) {
		if (config == null) {
			this.config = new AsyncConfiguration();
		} else {
			this.config = config;
		}
	}
	
	protected abstract void doOperation() throws Exception;
	
	@Override
	public void run() {
		try {
			if (started) {
			throw new IllegalStateException("Operation has already been started");
		}
		started = true;
		running = true;
		doOperation();
		running = false;
		setProgress(1f);
		} catch (Exception e) {
			log.error(e, e);
			running = false;
			succeeded = false;
			setProgress(1f);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.service.async.AsyncOperation#getConfiguration()
	 */
	@Override
	public AsyncConfiguration getConfiguration() {
		return config;
	}
	
	@Override
	public boolean started() {
		return started;
	}
	
	@Override
	public boolean running() {
		return running;
	}
	
	@Override
	public boolean finished() {
		return getProgress() == 1f;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.service.async.AsyncOperation#succeeded()
	 */
	@Override
	public boolean succeeded() {
		return succeeded;
	}
	
	protected void setSucceeded(boolean succeeded) {
		this.succeeded = succeeded;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.service.async.AsyncOperation#getProgress()
	 */
	@Override
	public float getProgress() {
		return progress;
	}
	
	protected synchronized void setProgress(float progress) {
		if (this.progress == 1f && progress == 1f) {
			// already finished, can't finish again
			return;
		}
		if (progress > 1 || progress < 0) throw new IllegalArgumentException("Progress cannot be more than 1 or less than 0 (" + progress + ")");
		this.progress = progress;
		
		long time = System.currentTimeMillis();
		
		if ((time - lastNotifyTime >= config.getNotifyInterval() 
				&& progress-lastProgress  > config.getNotifyProgressInterval()) || progress == 1f) {
			lastNotifyTime = time;
			for (ProgressListener listener : getProgressListeners()) {
				listener.operationProgressed(progress);
			}
			
			if (progress == 1f) {
				running = false;
				for (FinishedListener listener: getFinishedListeners()) {
					listener.operationFinished(this);
				}
			}
		}
	}
	
	@Override
	public List<ProgressListener> getProgressListeners() {
		return progressListeners;
	}

	@Override
	public void addProgressListener(ProgressListener listener) {
		progressListeners.add(listener);
	}

	@Override
	public void removeProgressListener(ProgressListener listener) {
		progressListeners.remove(listener);
	}

	@Override
	public List<FinishedListener> getFinishedListeners() {
		return finishedListeners;
	}

	@Override
	public void addFinishedListener(FinishedListener listener) {
		finishedListeners.add(listener);
	}

	@Override
	public void removeFinishedListener(FinishedListener listener) {
		finishedListeners.remove(listener);
	}
}
