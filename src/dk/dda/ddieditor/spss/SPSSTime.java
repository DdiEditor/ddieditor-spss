package dk.dda.ddieditor.spss;

import org.ddialliance.ddiftp.util.DDIFtpException;
import org.ddialliance.ddiftp.util.Translator;

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
 * From optional DDI-L time format expression to SPSS time format expression
 */
public enum SPSSTime {
	DATE_8("dd-mmm-yy", "DATE", "International date"), DATE_10("dd-mmm-yyyy",
			"DATE", "International date"),

	ADATE_8("mm/dd/yy", "ADATE", "American date"), ADATE_10("mm/dd/yyyy",
			"ADATE", "American date"),

	EDATE_8("dd.mm.yy", "EDATE", "European date"), EDATE_10("dd.mm.yyyy",
			"EDATE", "European date"),

	JDATE_5("yyddd", "JDATE", "Julian date"), JDATE_7("yyyyddd", "JDATE",
			"Julian date"),

	SDATE_8("yy/mm/dd", "SDATE", "Sortable date"), SDATE_10("yyyy/mm/dd",
			"SDATE", "Sortable date"),

	QYR_4("q Q yy", "QYR", "Quarter and year"), QYR_6("q Q yyyy", "QYR",
			"Quarter and year"),

	WKYR_6("ww WK yy", "WKYR", "Week and year"), WKYR_8("ww WK yyyy", "WKYR",
			"Week and year"),

	MOYR_6("mmm yy", "MOYR", "Month and year"), MOYR_8("mmm yyyy", "MOYR",
			"Month and year"),

	// TODO add day and month abbreviations
	WKDAY("SU", "WKDAY", "Day of the week (two letter)"), MONTH("JAN", "MONTH",
			"Name of the month (three letter)"),

	TIME_5("hh:mm", "TIME", "Time"), TIME_5_1("hh:mm:ss.s", "TIME", "Time"),

	DTIME_8("dd hh:mm", "DTIME", "Days and time"), DTIME_8_1("dd hh:mm:ss.s",
			"DTIME", "Days and time"), DTIME_8_2("dd hh:mm:ss.ss", "DTIME",
			"Days and time"), DTIME_8_3("dd hh:mm:ss.sss", "DTIME",
			"Days and time"),

	DATETIME_17("dd-mmm-yyyy hh:mm", "DATETIME",
			"Date and time with named month in three letters"), DATETIME_22(
			"dd-mmm-yyyy hh:mm:ss.s", "DATETIME",
			"Date and time with named month in three letters"), DATETIME_22_1(
			"dd-mmm-yyyy hh:mm:ss.s", "DATETIME",
			"Date and time with named month in three letters"), DATETIME_22_2(
			"dd-mmm-yyyy hh:mm:ss.ss", "DATETIME",
			"Date and time with named month in three letters"), DATETIME_22_3(
			"dd-mmm-yyyy hh:mm:ss.sss", "DATETIME",
			"Date and time with named month in three letters");

	String format, type, description;

	private SPSSTime(String format, String type, String description) {
		this.format = format;
		this.type = type;
		this.description = description;
	}

	public String getFormat() {
		return format;
	}

	public String getType() {
		return type;
	}

	public String getDescription() {
		return description;
	}

	public static SPSSTime getSpssType(String format) throws DDIFtpException {
		for (int i = 0; i < SPSSTime.values().length; i++) {
			SPSSTime spssTime = SPSSTime.values()[i];
			if (spssTime.getFormat().equals(format)) {
				return spssTime;
			}
		}

		// not found
		DDIFtpException e = new DDIFtpException(
				Translator.trans("spss.timeformat.notfound"),
				new Object[] { format }, new Throwable());
		throw e;
	}
}
