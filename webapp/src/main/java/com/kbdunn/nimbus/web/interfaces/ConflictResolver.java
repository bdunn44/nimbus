package com.kbdunn.nimbus.web.interfaces;

import java.util.List;

import com.kbdunn.nimbus.common.model.FileConflict;

public interface ConflictResolver {
	
	public void addResolutionListener(ResolutionListener listener);
	public void removeResolutionListener(ResolutionListener listener);
	public void fireResolutionEvent();
	
	public interface ResolutionListener {
		public void conflictsResolved(List<FileConflict> resolutions);
	}
}