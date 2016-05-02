package com.kbdunn.nimbus.web.files.editor;

import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

public class TextEditorView extends CssLayout implements View {
	
	private static final long serialVersionUID = -3255678188672044830L;
	
	public static final String NAME = "edit";
	
	private TextEditorController controller;
	private HorizontalLayout controls;
	private TextField filename;
	private ComboBox syntax;
	private AceEditor editor;
	private Button save;
	private boolean layoutBuilt = false;
	
	public TextEditorView(TextEditorController controller) {
		this.controller = controller;
		addStyleName("text-editor");
	}
	
	@Override
	public void enter(ViewChangeEvent event) {
		if (!layoutBuilt) buildLayout();
		controller.handleUri(event.getViewName() + "/" + event.getParameters());
	}
	
	String getFileName() {
		return filename.getValue();
	}
	
	void setFileName(String fileName) {
		filename.setValue(fileName);
	}
	
	AceMode getAceMode() {
		return (AceMode) syntax.getValue();
	}
	
	void setAceMode(AceMode mode) {
		syntax.select(mode);
	}
	
	String getEditorContent() {
		return editor.getValue();
	}
	
	void setEditorContent(String content) {
		editor.setValue(content);
	}
	
	void setSaveEnabled(boolean enabled) {
		save.setEnabled(enabled);
	}
	
	private void buildLayout() {
		setWidth("100%");
		setHeight("750px");
		
		controls = new HorizontalLayout();
		controls.setSizeUndefined();
		controls.setSpacing(true);
		addComponent(controls);
		
		filename = new TextField("Filename");
		controls.addComponent(filename);
		controls.setComponentAlignment(filename, Alignment.BOTTOM_LEFT);
		
		syntax = new ComboBox("Language");
		for (AceMode mode : AceMode.values()) {
			syntax.addItem(mode);
			syntax.setItemCaption(mode, mode.name());
		}
		syntax.addValueChangeListener(new ValueChangeListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void valueChange(ValueChangeEvent event) {
				editor.setMode((syntax.getValue() == null ? AceMode.text : (AceMode) syntax.getValue())); 
			}
		});
		controls.addComponent(syntax);
		controls.setComponentAlignment(syntax, Alignment.BOTTOM_LEFT);
		
		save = new Button("Save", new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				controller.saveFile();
			}
		});
		save.setIcon(FontAwesome.SAVE);
		save.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_RIGHT);
		controls.addComponent(save);
		controls.setComponentAlignment(save, Alignment.BOTTOM_LEFT);

		Button discard = new Button("Discard", new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				controller.refreshView();
			}
		});
		discard.setIcon(FontAwesome.UNDO);
		discard.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_RIGHT);
		controls.addComponent(discard);
		controls.setComponentAlignment(discard, Alignment.BOTTOM_LEFT);
		
		editor = new AceEditor();
		editor.setThemePath("VAADIN/addons/ace");
		editor.setModePath("VAADIN/addons/ace");
		editor.setWorkerPath("VAADIN/addons/ace");
		editor.setWidth("100%");
		editor.setHeight("650px");
		addComponent(editor);
		
		layoutBuilt = true;
	}
}
