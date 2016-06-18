package com.kbdunn.nimbus.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class ComparatorUtil {
	
	public static int nullSafeDateComparator(final Date one, final Date two) {
	    if (one == null ^ two == null) {
	        return (one == null) ? -1 : 1;
	    }

	    if (one == null && two == null) {
	        return 0;
	    }
	    
	    ZoneId zone = ZoneId.systemDefault();
	    LocalDateTime ldt1 = LocalDateTime.ofInstant(one.toInstant(), zone).truncatedTo(ChronoUnit.SECONDS);
	    LocalDateTime ldt2 = LocalDateTime.ofInstant(two.toInstant(), zone).truncatedTo(ChronoUnit.SECONDS);
	    return ldt1.compareTo(ldt2);
	    //return one.compareTo(two);
	}
	
	public static int nullSafeLongComparator(final Long one, final Long two) {
	    if (one == null ^ two == null) {
	        return (one == null) ? -1 : 1;
	    }

	    if (one == null && two == null) {
	        return 0;
	    }
	    
	    return one.compareTo(two);
	}
	
	public static int nullSafeBooleanComparator(final Boolean one, final Boolean two) {
	    if (one == null ^ two == null) {
	        return (one == null) ? -1 : 1;
	    }

	    if (one == null && two == null) {
	        return 0;
	    }
	    
	    return one.compareTo(two);
	}
	
	public static int nullSafeStringComparator(final String one, final String two) {
	    if (one == null ^ two == null) {
	        return (one == null) ? -1 : 1;
	    }

	    if (one == null && two == null) {
	        return 0;
	    }

	    return one.compareToIgnoreCase(two);
	}
}
