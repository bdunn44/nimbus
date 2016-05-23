package com.kbdunn.nimbus.api.client.model;

import java.util.ArrayList;
import java.util.List;

public class SyncFileList extends ArrayList<SyncFile> {

	private static final long serialVersionUID = -1687490329883798429L;

	public SyncFileList(List<SyncFile> source) {
		super(source);
	}
}
