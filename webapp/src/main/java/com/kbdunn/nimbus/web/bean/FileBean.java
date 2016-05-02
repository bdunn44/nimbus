package com.kbdunn.nimbus.web.bean;

import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesomeLabel;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.util.NimbusFileTypeResolver;

public class FileBean {
	
	public static final String PROPERTY_ITEM_ID = "itemId";
	public static final String PROPERTY_NAME = "name";
	public static final String PROPERTY_UPPER_NAME = "upperName";
	public static final String PROPERTY_MODIFIED = "modifiedDateString";
	public static final String PROPERTY_SIZE = "sizeString";
	public static final String PROPERTY_ICON = "iconResource";
	public static final String PROPERTY_ICON_LABEL = "iconLabel";
	public static final String PROPERTY_IS_DIRECTORY = "directory";
	
	private NimbusFile file;
	
	public FileBean() {  }
	
	public FileBean(NimbusFile file) {
		this.file = file;
	}
	
	public NimbusFile getNimbusFile() {
		return file;
	}
	
	public FontAwesome getIconResource() {
		return NimbusFileTypeResolver.getIcon(file);
	}
	
	public FontAwesomeLabel getIconLabel() {
		return getIconResource().getLabel();
	}
	
	public Object getItemId() {
		return this;
	}
	
	public Long getId() {
		return file.getId();
	}
	
	public String getName() {
		return file.getName();
	}
	
	public String getModifiedDateString() {
		return StringUtil.toDateString(NimbusUI.getFileService().getLastModifiedDate(file));
	}
	
	public String getSizeString() {
		long size = file.isDirectory() 
				? NimbusUI.getFileService().getRecursiveContentSize(file)
				: file.getSize();
		return StringUtil.toHumanSizeString(size);
	}
	
	public boolean isDirectory() {
		return file.isDirectory();
	}
	
	public static String[] convertToSortColumnNames(Object[] propertyNames) {
		String[] result = new String[propertyNames.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = convertToSortColumnName((String) propertyNames[i]);
		}
		return result;
	}
	
	public static String convertToSortColumnName(String propertyName) {
		if(PROPERTY_NAME.equals(propertyName)) {
			return "PATH";
		} else if(PROPERTY_UPPER_NAME.equals(propertyName)) {
			return "PATH";
		} else if(PROPERTY_MODIFIED.equals(propertyName)) {
			return "LAST_UPDATE_DATE";
		} else if(PROPERTY_SIZE.equals(propertyName)) {
			return "SIZE";
		} else if(PROPERTY_IS_DIRECTORY.equals(propertyName)) {
			return "IS_DIRECTORY";
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FileBean))
			return false;
		FileBean other = (FileBean) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		return true;
	}
}
