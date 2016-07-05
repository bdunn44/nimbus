package com.kbdunn.nimbus.common.model;

import com.kbdunn.nimbus.common.util.StringUtil;

public class UsageInformation {
	
	private long used, total;

	public UsageInformation(long used, long total) {
		super();
		this.used = used;
		this.total = total;
	}

	public UsageInformation() {  }

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
	
	public String getUsedString() {
		return StringUtil.toHumanSizeString(used);
	}
	
	public String getTotalString() {
		return StringUtil.toHumanSizeString(total);
	}
	
	public String getPercentageString() {
		return (int)Math.floor((double)used/(double)total*100) + "%";
	}
}