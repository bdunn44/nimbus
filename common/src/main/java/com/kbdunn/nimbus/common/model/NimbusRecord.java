package com.kbdunn.nimbus.common.model;

import java.util.Date;

public interface NimbusRecord {
	Long getId();
	void setId(Long id);
	Date getCreated();
	void setCreated(Date created);
	Date getUpdated();
	void setUpdated(Date updated);
}
