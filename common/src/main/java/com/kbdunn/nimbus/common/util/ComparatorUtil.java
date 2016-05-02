package com.kbdunn.nimbus.common.util;

import java.util.Date;

public class ComparatorUtil {
	
	public static int nullSafeDateComparator(final Date one, final Date two) {
	    if (one == null ^ two == null) {
	        return (one == null) ? -1 : 1;
	    }

	    if (one == null && two == null) {
	        return 0;
	    }
	    
	    return one.compareTo(two);
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
