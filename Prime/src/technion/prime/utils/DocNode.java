package technion.prime.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Very simple class to deal with building, saving and loading XMLs.
 * Not guaranteed to be correct. Guaranteed to be simple.
 * @author amishne
 */
public class DocNode {
	protected static final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	protected static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
	
	protected final List<DocNode> children = new LinkedList<DocNode>();
	protected final Map<String, String> attributes = new TreeMap<String, String>();
	protected final DocNode parent;
	protected final String name;
	protected String value;

	/**
	 * Create a new XML node.
	 * @param parent The parent node of this node, or null if this is the root.
	 * @param name The tag for this node.
	 * @param value Text value of this node, null if there is no text.
	 */
	public DocNode(DocNode parent, String name, String value) {
		this.parent = parent;
		this.name = name;
		this.value = value;
	}
	
	/**
	 * Get the value of a given attribute.
	 * @param attrName Name of the attribute.
	 * @return Attribute value.
	 */
	public String getAttribute(String attrName) {
		return attributes.get(attrName);
	}
	
	/**
	 * Set attribute to a given value.
	 * @param attrName Name of the attribute.
	 * @param value Attribute value.
	 */
	public void setAttribute(String attrName, String value) {
		attributes.put(attrName, value);
	}
	
	/**
	 * Save as XML file.
	 * @param filename Filename to save is.
	 * @throws IOException If the file could not be saved for any reason.
	 */
	public void save(String filename) throws IOException {
		OutputStream out = getOutputStream(filename);
		try {
			Document doc = builderFactory.newDocumentBuilder().newDocument();
			doc.appendChild(this.toXmlNode(doc));
			Transformer t = transformerFactory.newTransformer();  
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			Source src = new DOMSource(doc);
			Result dest = new StreamResult(out);
			t.transform(src, dest);
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} catch (TransformerException e) {
			throw new IOException(e);
		} finally {
			out.close();
		}
	}
	
	protected OutputStream getOutputStream(String filename) throws IOException {
		File f = new File(filename);
		f.getParentFile().mkdirs();
		return new FileOutputStream(f);
	}
	
	/**
	 * Create an instance of class from an XML. 
	 * @param filename XML filename.
	 * @return A new DocNode representing the root of the XML file.
	 * @throws IOException If the file could not be loaded for any reason.
	 */
	public static DocNode load(String filename) throws IOException {
		try {
			Document doc = builderFactory.newDocumentBuilder().parse(new File(filename));
			return DocNode.fromXmlNode(null, doc.getFirstChild());
		} catch (SAXException e) {
			throw new IOException(e);
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		}
	}
	
	/**
	 * Add a child node.
	 * @param name Name of child node.
	 * @param value Text value of child node.
	 * @return The new child node.
	 */
	public DocNode add(String name, String value) {
		DocNode newNode = new DocNode(this, name, value);
		children.add(newNode);
		return newNode;
	}
	
	/**
	 * Add a child node.
	 * @param n The child node. Its parent must be this node.
	 */
	public void add(DocNode n) {
		children.add(n);
		assert(n.parent == this);
	}
	
	/**
	 * @return The parent of the current node, or null if this node is the root.
	 */
	public DocNode getParent() {
		return parent;
	}
	
	/**
	 * @return The name of the current node (tag name).
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return The text value of the current node.
	 */
	public String getValue() {
		return value;
	}
	
	/**
	 * @param name Child name.
	 * @return The first child with the given name, or null if no such child exists.
	 */
	public DocNode getChildNamed(String name) {
		for (DocNode child : children) {
			if (child.name.equals(name)) return child;
		}
		return null;
	}
	
	/**
	 * @param name
	 * @return All the children with the given name. May be empty.
	 */
	public Iterable<DocNode> getAllChildrenNamed(String name) {
		final List<DocNode> filtered = new LinkedList<DocNode>();
		for (DocNode child : children) {
			if (child.name.equals(name)) {
				filtered.add(child);
			}
		}
		return new Iterable<DocNode>() {
			@Override
			public Iterator<DocNode> iterator() {
				return filtered.iterator();
			}
		};
	}
	
	protected static DocNode fromXmlNode(DocNode parent, Node n) {
		DocNode result = new DocNode(parent, n.getNodeName(), n.getNodeValue());
		
		// Attributes
		if (n.hasAttributes()) for (int i = 0 ; i < n.getAttributes().getLength() ; i++) {
			Node attr = n.getAttributes().item(i);
			result.attributes.put(attr.getNodeName(), attr.getNodeValue());
		}
		
		// children and values
		NodeList xmlChildren = n.getChildNodes();
		for (int i = 0 ; i < xmlChildren.getLength() ; i++) {
			Node child = xmlChildren.item(i);
			if ((child.getNodeType() & Node.TEXT_NODE) != 0) {
				result.value = child.getTextContent();
			}
			result.children.add(DocNode.fromXmlNode(result, child));
		}
		return result;
	}
	
	protected Node toXmlNode(Document doc) {
		Node xmlNode = doc.createElement(name);
		
		// Attributes
		for (String attrName : attributes.keySet()) {
			Node attrNode = doc.createAttribute(attrName);
			attrNode.setNodeValue(attributes.get(attrName));
			xmlNode.getAttributes().setNamedItem(attrNode);
		}
		
		// Value
		if (value != null) {
			xmlNode.appendChild(doc.createTextNode(value));
		}
		
		// Children
		for (DocNode child : children) {
			xmlNode.appendChild(child.toXmlNode(doc));
		}
		
		return xmlNode;
	}
	
}
