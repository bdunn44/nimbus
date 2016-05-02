package com.kbdunn.nimbus.server.async;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.async.AsyncConfiguration;
import com.kbdunn.nimbus.common.async.AsyncOperation;
import com.kbdunn.nimbus.common.async.AsyncOperationQueue;
import com.kbdunn.nimbus.common.async.FinishedListener;
import com.kbdunn.nimbus.common.async.ProgressListener;

public class AsyncServerOperationQueue extends AsyncServerOperation implements AsyncOperationQueue, ProgressListener, FinishedListener {

	private static final Logger log = LogManager.getLogger(AsyncServerOperationQueue.class.getName());
	
	private List<AsyncServerOperation> operations;
	private int total = 0;
	private int finished = 0;
	
	public AsyncServerOperationQueue(String name, List<AsyncServerOperation> operations) {
		super(new AsyncConfiguration(name));
		this.operations = operations;
		total = operations.size();
		
		for (AsyncOperation op : operations) {
			op.addProgressListener(this);
			op.addFinishedListener(this);
		}
	}
	
	@Override
	public int getOperationCount() {
		return total;
	}

	@Override
	public int getFinishedCount() {
		return finished;
	}

	@Override
	public List<AsyncOperation> getOperations() {
		List<AsyncOperation> ops = new ArrayList<>();
		for (AsyncOperation op : operations) {
			ops.add(op);
		}
		return ops;
	}
	
	@Override
	public void operationProgressed(float currentProgress) {
		if (currentProgress != 1f) {
			log.debug("Setting progress. current=" + currentProgress + ". total=" + total + ". finished=" + finished);
			super.setProgress((finished + currentProgress) / total);
		}
	}
	
	@Override
	public void operationFinished(AsyncOperation operation) {
		log.debug("Upload finished");
		if (++finished == 1) {
			super.setSucceeded(operation.succeeded());
		} else {
			super.setSucceeded(super.succeeded() ? operation.succeeded() : false);
		}
		super.setProgress(finished / total);
	}
	
	@Override
	public void doOperation() throws Exception {
		for (AsyncServerOperation op : operations) {
			// Run the operation in the current thread (serially)
			// VaadinUploadOperations are started automatically by the Vaadin framework
			if (!(op instanceof VaadinUploadOperation)) 
				op.run();
		}
	}
}