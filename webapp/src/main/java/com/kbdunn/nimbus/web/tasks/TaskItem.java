package com.kbdunn.nimbus.web.tasks;

import com.kbdunn.nimbus.common.async.AsyncOperation;
import com.kbdunn.nimbus.common.async.FinishedListener;
import com.kbdunn.nimbus.common.async.ProgressListener;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.web.NimbusUI;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

public class TaskItem extends CssLayout implements ProgressListener, FinishedListener {

	private static final long serialVersionUID = 8204251655691181627L;
	
	private AsyncOperation task;
	private Label name, info;
	private ProgressBar progressBar;
	
	public TaskItem(AsyncOperation task) {
		this.task = task;
		task.addProgressListener(this);
		task.addFinishedListener(this);
		
		addStyleName("task-item");
		addStyleName(ValoTheme.LAYOUT_CARD);
		
		buildLayout();
	}
	
	private void buildLayout() {
		name = new Label(task.getConfiguration().getName() + "....");
		name.addStyleName("task-name");
		name.addStyleName(ValoTheme.LABEL_SMALL);
		addComponent(name);
		
		/*if (task.isInterruptable()) {
			cancelButton = new Button();
			cancelButton.addStyleName("cancel-button");
			cancelButton.addClickListener(this);
			cancelButton.setIcon(FontAwesome.TIMES);
			cancelButton.addStyleName(ValoTheme.BUTTON_LINK);
			addComponent(cancelButton);
		}*/
		
		progressBar = new ProgressBar(task.getProgress());
		progressBar.addStyleName("progress-bar");
		addComponent(progressBar);

		info = new Label("0%");
		info.addStyleName("task-info");
		info.addStyleName(ValoTheme.LABEL_SMALL);
		addComponent(info);
	}
	
	@Override
	public void operationProgressed(final float currentProgress) {
		UI.getCurrent().access(new Runnable() {

			@Override
			public void run() {
				progressBar.setValue(currentProgress);
				info.setValue(StringUtil.toHumanPercentage(currentProgress));
				UI.getCurrent().push();
			}
		});
	}
	
	private TaskItem getMe() {
		return this;
	}

	@Override
	public void operationFinished(AsyncOperation operation) {
		taskDone();
	}
	
	private void taskDone() {
		UI.getCurrent().access(new Runnable() {

			@Override
			public void run() {
				if (!task.succeeded()) Notification.show(task.getConfiguration().getName() + " failed!", Notification.Type.ERROR_MESSAGE);
				addStyleName("fade-out");
				NimbusUI.getCurrent().getTaskController().removeTask(getMe());
				UI.getCurrent().push();
			}
		});
	}
}