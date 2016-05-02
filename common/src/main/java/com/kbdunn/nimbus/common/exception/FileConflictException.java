package com.kbdunn.nimbus.common.exception;

import java.util.ArrayList;
import java.util.List;

import com.kbdunn.nimbus.common.model.FileConflict;

public class FileConflictException extends NimbusException {

	private static final long serialVersionUID = -3768467404651869512L;
	private List<FileConflict> conflicts;
	/*private Map<NimbusFile, NimbusFile> conflicts;
	
	public FileConflictException(Map<NimbusFile, NimbusFile> conflicts) {
		this.conflicts = conflicts;
	}
	
	public Map<NimbusFile, NimbusFile> getConflicts() {
		return conflicts;
	}*/
	
	public FileConflictException() {
		super("");
		conflicts = new ArrayList<FileConflict>();
	}
	
	public FileConflictException(FileConflict conflict) {
		super("");
		conflicts = new ArrayList<FileConflict>();
		conflicts.add(conflict);
	}
	
	public FileConflictException(List<FileConflict> conflicts) {
		super("");
		this.conflicts = conflicts;
	}
	
	public void add(FileConflict conflict) {
		conflicts.add(conflict);
	}
	
	public List<FileConflict> getConflicts() {
		return conflicts;
	}
	
	@Override
	public String getMessage() {
		int conflictCount = conflicts.size();
		String message = "There ";
		message += conflictCount > 1 ? "were " : "was ";
		message += conflictCount + " file name conflict";
		message += conflictCount > 1 ? "s." : ".";
		return message;
	}
	
	/*private NimbusFile source;
	private NimbusFile target;
	
	public FileConflictException(NimbusFile source, NimbusFile target) {
		this.source = source;
		this.target = target;
	}
	
	public NimbusFile getSource() {
		return source;
	}
	
	public NimbusFile getTarget() {
		return target;
	}
	
	@Override
	public String getMessage() {
		return "The target file already exists";
	}*/
}