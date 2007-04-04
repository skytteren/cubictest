/*
 * This software is licensed under the terms of the GNU GENERAL PUBLIC LICENSE
 * Version 2, which can be found at http://www.gnu.org/copyleft/gpl.html
*/
package org.cubictest.exporters.selenium.selenese.holders;

import org.cubictest.export.IResultHolder;
import org.cubictest.exporters.selenium.utils.ContextHolder;
import org.cubictest.exporters.selenium.utils.XmlUtils;
import org.jdom.Document;
import org.jdom.Element;

/**
 * Selenium (Selenese) step list. HTML document with test steps (commands / rows).
 * 
 * @author chr_schwarz
 */
public class SeleneseDocument extends ContextHolder implements IResultHolder {

	/** The root element of the document */
	private Element rootElement;
	
	/** The table to add commands (rows) to */
	private Element table;
	
	
	/**
	 * Public constructor.
	 */
	public SeleneseDocument() {

		setUpHtmlPage();
	}


	
	/**
	 * Add command (with value).
	 */
	public Command addCommand(String commandName, String target, String value) {
		Command command = new Command(commandName, target, value);
		table.addContent(command);
		return command;
	}

	
	/**
	 * Add command (without value).
	 */
	public Command addCommand(String commandName, String target) {
		return addCommand(commandName, target, "");
	}
	
	
	/**
	 * Get string representation of the document (for e.g. file write).
	 */
	@Override
	public String toResultString() {
		Document document = new Document(rootElement);
		return XmlUtils.getNewXmlOutputter().outputString(document);
	}
	

	
	
	/**
	 * Sets up a HTML page for the tests, including a table for the commands.
	 */
	private void setUpHtmlPage() {
		rootElement = new Element("html");
		
		Element header = new Element("head");
		Element title = new Element("title");
		title.setText("CubicTest exported test");
		header.addContent(title);
		
		Element style = new Element("style");
		style.setAttribute("type", "text/css");
		style.setText(
				"td {padding-right: 25px}\n" + 
				".comment {color: #AAF; padding-top: 10px;}\n");
		header.addContent(style);
		
		rootElement.addContent(header);
		
		Element body = new Element("body");
		rootElement.addContent(body);
		table = new Element("table");
		table.setAttribute("border", "1");
		table.setAttribute("cellspacing", "0");
		body.addContent(table);
		
		table.addContent(new Command("setTimeout", "10000"));
	}
}