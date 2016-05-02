package com.kbdunn.nimbus.common.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class DateUtil {

	public static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss aa Z";
	
	public static DateFormat getDateFormat() {
		return new SimpleDateFormat(DATE_FORMAT);
	}
}
