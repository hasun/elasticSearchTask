package com.saltlux.util.topicrank;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;

public class Utils {

	public static String getStackTrace(Throwable e) 
	{
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
	public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    public static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((Character.isWhitespace(str.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean isEmpty(Object array[]) {
    	if ( array==null ) return true;
    	if ( array.length<=0 ) return true;
    	return false;
    }
    
    public static boolean isNotEmpty(Object array[]) {
    	if ( array==null ) return false;
    	if ( array.length<=0 ) return false;

    	return true;
    }    
    
    public static boolean isEmpty(@SuppressWarnings("rawtypes") Collection c) {
		if ( c==null ) return true;
		if ( c.size()==0 ) return true;
		return false;
    }    
    
	public static boolean isNotEmpty(@SuppressWarnings("rawtypes") Collection c){
		if ( c==null ) return false;
		if ( c.size()==0 ) return false;
		return true;
	}
	
    public static boolean isEmpty(@SuppressWarnings("rawtypes") Map m) {
    	if ( m==null ) return true;
    	if ( m.size()<=0 ) return true;

    	return false;
    }  
    
	public static boolean isNotEmpty(@SuppressWarnings("rawtypes") Map m){
		if ( m==null ) return false;
		if ( m.size()==0 ) return false;
		return true;
	}
	
    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        int len = searchStr.length();
        int max = str.length() - len;
        for (int i = 0; i <= max; i++) {
            if (str.regionMatches(true, i, searchStr, 0, len)) {
                return true;
            }
        }
        return false;
    }
}
