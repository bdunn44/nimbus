package com.kbdunn.nimbus.common.async;

import java.util.List;

public interface AsyncOperationQueue extends AsyncOperation {
	int getOperationCount();
	int getFinishedCount();
	List<AsyncOperation> getOperations();
}
