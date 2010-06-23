package org.wordpress.android;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

public class StringHelper {

	public static String[] mergeStringArrays(String array1[], String array2[]) {  
		if (array1 == null || array1.length == 0)  
		return array2;  
		if (array2 == null || array2.length == 0)  
		return array1;  
		List array1List = Arrays.asList(array1);  
		List array2List = Arrays.asList(array2);  
		List result = new ArrayList(array1List);    
		List tmp = new ArrayList(array1List);  
		tmp.retainAll(array2List);  
		result.addAll(array2List);    
		return ((String[]) result.toArray(new String[result.size()]));  
		}
	
	public static String convertHTMLTagsForUpload(String source){
		
		//bold
		source = source.replace("<b>", "<strong>");
		source = source.replace("</b>", "</strong>");
		
		//italics
		source = source.replace("<i>", "<em>");
		source = source.replace("</i>", "</em>");
		
		return source;
		
	}
	
	public static String convertHTMLTagsForDisplay(String source){
		
		//bold
		source = source.replace("<strong>", "<b>");
		source = source.replace("</strong>", "</b>");
		
		//italics
		source = source.replace("<em>", "<i>");
		source = source.replace("</em>", "</i>");
		
		return source;
		
	}
}
