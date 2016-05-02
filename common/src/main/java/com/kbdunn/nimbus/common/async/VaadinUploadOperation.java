package com.kbdunn.nimbus.common.async;

import com.vaadin.server.StreamVariable;
import com.vaadin.ui.Upload.FailedListener;
import com.vaadin.ui.Upload.ProgressListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededListener;

public interface VaadinUploadOperation extends Receiver, StreamVariable, StartedListener, ProgressListener, FailedListener, SucceededListener {
	// Needed to ensure the AsyncOperation implemented on the server side implements necessary Vaadin interfaces
}
