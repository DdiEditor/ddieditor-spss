package dk.dda.ddieditor.spss;

/*
 * Copyright 2011 Danish Data Archive (http://www.dda.dk) 
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either Version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library; if not, write to the 
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
 * Boston, MA  02110-1301  USA
 * The full text of the license is also available on the Internet at 
 * http://www.gnu.org/copyleft/lesser.html
 */

/**
 * SPSS measure int to DDI-L string expression
 */
public enum SPSSMeasure {
	// < 1=nominal, 2=ordinal, 3=scale (copied from record type 7 subtype 11) 
	NOMINAL(1, "Nominal"), ORDINAL(2, "Ordinal"), SCALE(3, "Continuous");
	
	private int type;
	private String typeTxt;
	private SPSSMeasure(int type, String typeTxt) {
		this.type = type;
		this.typeTxt = typeTxt;
	}
	
	public static SPSSMeasure intToSpssMeasure(int spssInt) {
		for (int i = 0; i < values().length; i++) {
			if (values()[i].type==spssInt) {
				return values()[i]; 
			}
		}
		return null;
	}
	
	public String classificationLevel() {
		return typeTxt;
	}
}
