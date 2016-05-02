package com.kbdunn.nimbus.common.model;

public class MemoryInformation {
	
	private long used, total;

	public MemoryInformation(long used, long total) {
		super();
		this.used = used;
		this.total = total;
	}

	public MemoryInformation() {  }

	public long getUsed() {
		return used;
	}

	public void setUsed(long used) {
		this.used = used;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}
}