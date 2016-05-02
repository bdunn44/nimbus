package com.kbdunn.nimbus.web.tasks;

import java.util.List;

import com.vaadin.ui.VerticalLayout;

public class Taskbar extends VerticalLayout {

	private static final long serialVersionUID = -2287066455681157001L;
	
	private VerticalLayout currentTasks; //, leftTasks, rightTasks;
	
	public Taskbar(ToggleButton toggleButton, ScrollButtons scrollButtons) {
		addStyleName("taskbar");
		
		// Left and Right layouts may be useful for transitions
		/*leftTasks = new VerticalLayout();
		leftTasks.addStyleName("task-list-layout");
		leftTasks.addStyleName("layout-left");
		leftTasks.addStyleName("slide-right");
		addComponent(leftTasks);*/
		
		currentTasks = new VerticalLayout();
		currentTasks.addStyleName("task-list-layout");
		addComponent(currentTasks);
		
		/*rightTasks = new VerticalLayout();
		rightTasks.addStyleName("task-list-layout");
		rightTasks.addStyleName("layout-right");
		rightTasks.addStyleName("slide-left");
		addComponent(rightTasks);*/
		
		addComponent(toggleButton);
		addComponent(scrollButtons);
	}
	
	/*protected void setLeftTasks(List<TaskItem> tasks) {
		leftTasks.removeAllComponents();
		for (TaskItem task : tasks) leftTasks.addComponent(task);
	}*/
	
	protected void setCurrentTasks(List<TaskItem> tasks) {
		currentTasks.removeAllComponents();
		for (TaskItem task : tasks) currentTasks.addComponent(task);
	}
	
	/*protected void setRightTasks(List<TaskItem> tasks) {
		rightTasks.removeAllComponents();
		for (TaskItem task : tasks) rightTasks.addComponent(task);
	}*/
	
	/*protected void slideCurrentTasksLeft() {
		currentTasks.addStyleName("slide-left");
	}
	
	protected void slideCurrentTasksRight() {
		currentTasks.addStyleName("slide-right");
	}*/
	
	protected int getCurrentTaskCount() {
		return currentTasks.getComponentCount();
	}
}
