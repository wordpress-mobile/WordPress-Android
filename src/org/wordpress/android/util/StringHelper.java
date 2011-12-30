package org.wordpress.android.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringHelper {

	public static String[] mergeStringArrays(String array1[], String array2[]) {
		if (array1 == null || array1.length == 0)
			return array2;
		if (array2 == null || array2.length == 0)
			return array1;
		List<String> array1List = Arrays.asList(array1);
		List<String> array2List = Arrays.asList(array2);
		List<String> result = new ArrayList<String>(array1List);
		List<String> tmp = new ArrayList<String>(array1List);
		tmp.retainAll(array2List);
		result.addAll(array2List);
		return ((String[]) result.toArray(new String[result.size()]));
	}

	public static String convertHTMLTagsForUpload(String source) {

		// bold
		source = source.replace("<b>", "<strong>");
		source = source.replace("</b>", "</strong>");

		// italics
		source = source.replace("<i>", "<em>");
		source = source.replace("</i>", "</em>");

		return source;

	}

	public static String convertHTMLTagsForDisplay(String source) {

		// bold
		source = source.replace("<strong>", "<b>");
		source = source.replace("</strong>", "</b>");

		// italics
		source = source.replace("<em>", "<i>");
		source = source.replace("</em>", "</i>");

		return source;

	}

	public static String addPTags(String source) {
		String[] asploded = source.split("\n\n");
		String wrappedHTML = "";
		if (asploded.length > 0) {
			for (int i = 0; i < asploded.length; i++) {
				if (asploded[i].trim().length() > 0)
					wrappedHTML += "<p>" + asploded[i].trim() + "</p>";
			}
		} else {
			wrappedHTML = source;
		}
		wrappedHTML = wrappedHTML.replace("<br />", "<br>").replace("<br/>", "<br>");
		wrappedHTML = wrappedHTML.replace("<br>\n", "<br>").replace("\n", "<br>");
		return wrappedHTML;
	}
}
