package org.cubictest.persistence;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.jaxen.JaxenException;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * Used for upgrading old CubicTest XML files to the newest standard.
 * See the <code>ModelInfo</code> class for info about model versions.
 * @author chr_schwarz
 */
public class LegacyUpgrade {

	/**
	 * Upgrade model XML.
	 * Project may be null, must be handled by the upgrade code.
	 * See the <code>ModelInfo</code> class for info about model versions.
	 * @param xml
	 * @param project
	 * @return
	 */
	public static String upgradeIfNecessary(String xml, IProject project) {
		ModelVersion version = getModelVersion(xml);
		xml = upgradeModel1to2(xml, version);
		xml = upgradeModel2to3(xml, project, version);
		xml = upgradeModel3to4(xml, version);
		xml = upgradeModel4to5(xml, version);
		return xml;
	}

	private static String upgradeModel1to2(String xml, ModelVersion version) {
		if (version.getVersion() != 1) {
			return xml;
		}
		xml = StringUtils.replace(xml, "<aspect", "<common");
		xml = StringUtils.replace(xml, "</aspect", "</common");
		xml = StringUtils.replace(xml, "=\"aspect\"", "=\"common\"");
		xml = StringUtils.replace(xml, "/aspect", "/common");

		xml = StringUtils.replace(xml, "<simpleContext", "<pageSection");
		xml = StringUtils.replace(xml, "</simpleContext", "</pageSection");
		xml = StringUtils.replace(xml, "=\"simpleContext\"", "=\"pageSection\"");
		xml = StringUtils.replace(xml, "/simpleContext", "/pageSection");
		
		xml = StringUtils.replace(xml, "class=\"startPoint\"", "class=\"urlStartPoint\"");
		version.increment();
		return xml;
	}

	
	private static String upgradeModel2to3(String xml, IProject project, ModelVersion version) {
		if (version.getVersion() != 2) {
			return xml;
		}
		if (project != null && xml.indexOf("<filePath>/") == -1) {
			xml = StringUtils.replace(xml, "<filePath>", "<filePath>/" + project.getName() + "/");
		}
		version.increment();
		return xml;
	}

	private static String upgradeModel3to4(String xml, ModelVersion version) {
		if (version.getVersion() != 3) {
			return xml;
		}
		xml = StringUtils.replace(xml, "<pageSection", "<simpleContext");
		xml = StringUtils.replace(xml, "</pageSection", "</simpleContext");
		xml = StringUtils.replace(xml, "=\"pageSection\"", "=\"simpleContext\"");
		xml = StringUtils.replace(xml, "/pageSection", "/simpleContext");

		xml = StringUtils.replace(xml, "<userActions", "<userInteractionsTransition");
		xml = StringUtils.replace(xml, "</userActions", "</userInteractionsTransition");
		xml = StringUtils.replace(xml, "=\"userActions\"", "=\"userInteractionsTransition\"");
		xml = StringUtils.replace(xml, "/userActions", "/userInteractionsTransition");

		xml = StringUtils.replace(xml, "<pageElementAction", "<userInteraction");
		xml = StringUtils.replace(xml, "</pageElementAction", "</userInteraction");
		xml = StringUtils.replace(xml, "=\"pageElementAction\"", "=\"userInteraction\"");
		xml = StringUtils.replace(xml, "/pageElementAction", "/userInteraction");

		xml = StringUtils.replace(xml, "<inputs", "<userInteractions");
		xml = StringUtils.replace(xml, "</inputs", "</userInteractions");
		xml = StringUtils.replace(xml, "/inputs/", "/userInteractions/");

		version.increment();
		return xml;
	}
	
	private static String upgradeModel4to5(String xml, ModelVersion version) {
		if (version.getVersion() != 4) {
			return xml;
		}
		try {
			Document document = new SAXBuilder().build(new StringReader(xml));
			Element rootElement = document.getRootElement();
			//fixing enums...
			JDOMXPath xpath = new JDOMXPath("//sationType|//identifierType|//action");
			for(Object obj : xpath.selectNodes(rootElement)){
				if(obj instanceof Element){
					Element sationType = (Element) obj;
					Attribute ref = sationType.getAttribute("reference");
					if(ref != null){
						JDOMXPath refXpath = new JDOMXPath(ref.getValue());
						Element trueSationType = (Element)refXpath.selectSingleNode(sationType);
						sationType.setText(trueSationType.getText());
						sationType.removeAttribute(ref);
					}
				}
			}
			//Fixing new sationObserver in Identifiers
			xpath = new JDOMXPath("//elements");
			for(Object node : xpath.selectNodes(rootElement)){
				if(node instanceof Element){
					Element elements = (Element) node;
					for(Object child : elements.getChildren()){
						if(child instanceof Element){
							Element pageElement = (Element) child;
							
							Element identifiers = new Element("identifiers");
							pageElement.addContent(identifiers);
							
							Element identifier = new Element("identifier");
							identifiers.addContent(identifier);
							
							Element probability = new Element("probability");
							probability.setText("100");
							identifier.addContent(probability);
							
							Element value = new Element("value");
							Element type = new Element("type");
							Element identifierType = pageElement.getChild("identifierType");
							String idType = "";
							if (identifierType == null) {
								idType = "LABEL";
							}
							else {
								idType = identifierType.getText();
								pageElement.removeContent(identifierType);
							}
							type.setText(idType);
							
							if (idType.equals("LABEL")) {
								value.setText(pageElement.getChildText("description"));
							}
							else {
								value.setText(pageElement.getChildText("description"));
							}

							identifier.addContent(type);
							identifier.addContent(value);
							
							Element directEditID = new Element("directEditIdentifier");
							directEditID.setAttribute("reference", "../identifiers/identifier");
							pageElement.addContent(directEditID);
							
							Element sationType = pageElement.getChild("sationType");
							if(sationType != null){
								Element useI18n = new Element("useI18n");
								Element useParam = new Element("useParam");
								identifier.addContent(useI18n);
								identifier.addContent(useParam);
								Element key = pageElement.getChild("key");
								Element newKey = new Element("paramKey");
								if("NONE".equals(sationType.getText())){
									useI18n.setText("false");
									useParam.setText("false");
								}else if(sationType.getText().contains("PARAM")){
									useI18n.setText("false");
									useParam.setText("true");
								}else if(sationType.getText().contains("INTER")){
									useI18n.setText("true");
									useParam.setText("false");
									newKey = new Element("i18nKey");
								}else{
									useI18n.setText("true");
									useParam.setText("true");
								}
								newKey.setText(key.getText());
								identifier.addContent(newKey);
								pageElement.removeContent(key);
							}
							pageElement.removeContent(sationType);
							Element multiSelect = pageElement.getChild("multiselect");
							if(multiSelect != null){
								identifier = new Element("identifier");
								identifiers.addContent(identifier);
								
								identifierType = new Element("type");
								identifierType.setText("MULTISELECT");
								identifier.addContent(identifierType);
								
								Element useI18n = new Element("useI18n");
								Element useParam = new Element("useParam");
								useI18n.setText("false");
								useParam.setText("false");
								identifier.addContent(useI18n);
								identifier.addContent(useParam);
								
								pageElement.removeContent(multiSelect);
							}
						}
					}
				}
			}
			//fixing new sationObserver in userInteraction
			xpath = new JDOMXPath("//userInteraction");
			for(Object obj : xpath.selectNodes(rootElement)){
				if(obj instanceof Element){
					Element element = (Element) obj;
					
					Element sationType = element.getChild("sationType");
					if(sationType != null){
						Element useI18n = new Element("useI18n");
						Element useParam = new Element("useParam");
						element.addContent(useI18n);
						element.addContent(useParam);
						Element key = element.getChild("key");
						Element newKey = new Element("paramKey");
						if("NONE".equals(sationType.getText())){
							useI18n.setText("false");
							useParam.setText("false");
						}else if(sationType.getText().contains("PARAM")){
							useI18n.setText("false");
							useParam.setText("true");
						}else if(sationType.getText().contains("INTER")){
							useI18n.setText("true");
							useParam.setText("false");
							newKey = new Element("i18nKey");
						}else{
							useI18n.setText("true");
							useParam.setText("true");
						}
						newKey.setText(key.getText());
						element.addContent(newKey);
						element.removeContent(key);
					}
					element.removeContent(sationType);
				}
			}
			xml = new XMLOutputter().outputString(document);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JaxenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		version.increment();
		return xml;
	}

	
	private static ModelVersion getModelVersion(String xml) {
		String start = "<modelVersion>";
		String end = "</modelVersion>";
		int pos1 = xml.indexOf(start) + start.length();
		int pos2 = xml.indexOf(end);
		BigDecimal version = new BigDecimal(xml.substring(pos1, pos2).trim());

		// only supporting integer versions:
		return new ModelVersion(version.intValue());
	}	
	
	static class ModelVersion {
		private int version;
		
		public ModelVersion(int initialVersion) {
			version = initialVersion;
		}
		
		public void increment() {
			version++;
		}
		
		public int getVersion() {
			return version;
		}
		
	}
}
