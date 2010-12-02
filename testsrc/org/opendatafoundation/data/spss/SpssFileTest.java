package org.opendatafoundation.data.spss;

import java.io.File;

import org.junit.Test;
import org.opendatafoundation.data.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

public class SpssFileTest {
	String path = "resources/sps14069.sav";
	File file = new File(path);
	String outFilePath = file.getName() + ".xml";

	@Test
	public void SPSSFile() throws Exception {
		SPSSFile spssFile = new SPSSFile(file);
		spssFile.loadMetadata();
		// spssFile.dumpDDI3();
		ExportOptions exportOptions = new ExportOptions();
		exportOptions.createCategories = true;
		Document doc = spssFile
				.getDDI3LogicalProduct(exportOptions, null, null);

		System.out.println(Utils.nodeToString(doc));
		System.out.println(outFilePath);
		Utils.writeXmlFile(doc, outFilePath);
	}
}
