/**
 * This file is part of GraphView.
 *
 * GraphView is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GraphView is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GraphView.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 *
 * Copyright Jonas Gehring
 */

package com.jjoe64.graphview;

/**
 * if you want to show different labels,
 * you can use this label formatter.
 * As Input you get the raw value (x or y) and
 * you return a String that will be displayed.
 * {@code
 * 		graphView.setCustomLabelFormatter(new CustomLabelFormatter() {
			public String formatLabel(double value, boolean isValueX) {
				if (isValueX) {
					if (value < 5) {
						return "small";
					} else if (value < 15) {
						return "middle";
					} else {
						return "big";
					}
				}
				return null; // let graphview generate Y-axis label for us
			}
		});
 * }
 */
public interface CustomLabelFormatter {

	/**
	 * will be called when the labels were generated
	 * @param value the raw input value (x or y)
	 * @param isValueX true if value is a x-value, false if otherwise
	 * @return the string that will be displayed. return null if you want graphview to generate the label for you.
	 */
	String formatLabel(double value, boolean isValueX);

}
