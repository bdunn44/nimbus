package com.kbdunn.nimbus.web.tasks;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.async.AsyncOperation;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;

public class TaskController {
	
	private static final Logger log = LogManager.getLogger(TaskController.class.getName());
	
	private ToggleButton toggleButton;
	private ScrollButtons scrollButtons;
	private Taskbar taskbar;
	
	private List<TaskItem> tasks = new ArrayList<TaskItem>();
	private boolean expanded;
	private int taskIndex = 0;
	
	public TaskController() {
		toggleButton = new ToggleButton(this);
		scrollButtons = new ScrollButtons(this);
		taskbar = new Taskbar(toggleButton, scrollButtons);
		
		hide();
	}
	
	public Taskbar getTaskbar() {
		return taskbar;
	}
	
	public boolean isExanded() {
		return expanded;
	}
	
	public void expand() {
		log.debug("Expanding the taskbar");
		taskbar.removeStyleName("collapse-1");
		taskbar.removeStyleName("collapse-2");
		taskbar.removeStyleName("collapse-3");
		toggleButton.setVisible(true);
		toggleButton.setIcon(FontAwesome.CHEVRON_UP);
		expanded = true;
	}
	
	public void collapse() {
		log.debug("Collapsing the taskbar");
		
		// Dynamically animate the slide out based on the number of task panels
		int currentTasks = taskbar.getCurrentTaskCount();
		if (currentTasks >= 3) taskbar.addStyleName("collapse-3");
		else if (currentTasks > 0) taskbar.addStyleName("collapse-" + currentTasks);
		
		toggleButton.setIcon(FontAwesome.CHEVRON_DOWN);
		scrollButtons.setVisible(false);
		expanded = false;
	}
	
	public void addAndStartTask(AsyncOperation task) {
		addTask(task);
		NimbusUI.getAsyncService().startAsyncOperation(task);
	}
	
	public void addTask(AsyncOperation task) {
		addTask(new TaskItem(task));
	}
	
	public void addTask(TaskItem task) {
		log.debug("Adding task to taskbar");
		tasks.add(task);
		refreshDisplay();
		expand();
	}
	
	protected void removeTask(TaskItem task) {
		log.debug("Removing task from taskbar");
		tasks.remove(task);
		if (tasks.size() == taskIndex) 
			scrollBack();
		else
			refreshDisplay();
	}
	
	protected void scrollNext() {
		if (tasks.size() > taskIndex+3) taskIndex+=3;
		//taskbar.slideCurrentTasksLeft();
		refreshDisplay();
	}
	
	protected void scrollBack() {
		if (taskIndex-3 >= 0) taskIndex-=3;
		//taskbar.slideCurrentTasksRight();
		refreshDisplay();
	}
	
	private void refreshDisplay() {
		if (tasks.size() == 0) {
			log.debug("No tasks, hiding taskbar");
			hide();
		} else {
			scrollButtons.setVisible(false);
			taskbar.setCurrentTasks(tasks.subList(taskIndex, (tasks.size() > taskIndex+2 ? taskIndex+3 : tasks.size())));
			
			// Set the previous tasks in the scrolling queue
			if (taskIndex == 0) {
				//taskbar.setLeftTasks(Collections.<TaskItem> emptyList());
				scrollButtons.setBackEnabled(false);
			} else {
				//taskbar.setLeftTasks(tasks.subList(taskIndex-3, taskIndex));
				scrollButtons.setBackEnabled(true);
				scrollButtons.setVisible(true);
			}
			
			// Set the forward tasks in the scrolling queue
			if (tasks.size() <= 3 || taskIndex+3 >= tasks.size()) {
				//taskbar.setRightTasks(Collections.<TaskItem> emptyList());
				scrollButtons.setNextEnabled(false);
			} else {
				//taskbar.setRightTasks(tasks.subList(taskIndex+3, (tasks.size() > taskIndex+5 ? taskIndex+6 : tasks.size())));
				scrollButtons.setNextEnabled(true);
				scrollButtons.setVisible(true);
			}
		}
	}
	
	private void hide() {
		toggleButton.setVisible(false);
		collapse();
	}
}