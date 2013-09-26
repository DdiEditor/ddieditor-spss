package org.opendatafoundation.data;

public class ValidationReportElement {
	private String id;
	private String type;
	private String descr;
	
	public ValidationReportElement(String id, String type, String descr) {
		this.id = id;
		this.type = type;
		this.descr = descr;
	}
	
	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public String getDescr() {
		return descr;
	}

}
