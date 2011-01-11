package prefuse.demos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;



public class Parser {
	
	private Document document;
	private Element racine;
	private ArrayList<Node> tree;
	static String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
	"<!-- This file was written by the JAVA GraphML Library.-->\n"+
	"<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" " +  
	"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
	"xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">\n" +
	  "  <key id=\"name\" for=\"node\" attr.name=\"name\" attr.type=\"string\"/>\n" +
	  "  <key id=\"tags\" for=\"node\" attr.name=\"tags\" attr.type=\"string\"/>\n" +
	  "  <key id=\"author\" for=\"node\" attr.name=\"author\" attr.type=\"string\"/>\n" +
	  "  <key id=\"client\" for=\"node\" attr.name=\"client\" attr.type=\"string\"/>\n" +
	  "  <key id=\"company\" for=\"node\" attr.name=\"company\" attr.type=\"string\"/>\n" + 
	  "  <key id=\"description\" for=\"node\" attr.name=\"description\" attr.type=\"string\"/>\n" +
	  "  <key id=\"urlproject\" for=\"node\" attr.name=\"urlproject\" attr.type=\"string\"/>\n\n "+
	  " <graph id=\"G\" edgedefault=\"undirected\">\n ";
	
	static String tail = "</graph>\n</graphml>";
	
	public Parser(String file){
		SAXBuilder sxb = new SAXBuilder();

		try {
			document = sxb.build(new File(file));
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Erreur d'ouverture de fichier");
			e.printStackTrace();
		}
		racine = document.getRootElement();

	}
	
	private String getUrl(Element current){
		String retour = "";
		String baseUrl = current.getChild("demo_url").getValue();
		//System.out.println(baseUrl);
		if(baseUrl.contains("&")){
			String[] split = baseUrl.split("&");
			retour = split[0];
			for(int i=0; i<split.length-1;i++){
			//	System.out.println(split[i]);
				if(!split[i+1].startsWith("amp;")){
					retour+="&amp;";
					retour+=split[i+1];
				}
			}
		//	System.out.println(retour);
			return retour;
		}
		return baseUrl;
	}
	
	public void parse(){
		
	
		
		List<Element> nodes = racine.getChildren("node");
		
		tree = new ArrayList<Node>();
		Iterator<Element> it = nodes.iterator();
		int count = 0; // ??
		while(it.hasNext()){
			Element current = it.next();
			Node node = new Node();
			node.author = current.getChild("author").getValue();
			node.client = current.getChild("client").getValue();
			node.company = current.getChild("company").getValue();
			node.description = current.getChild("description").getValue();
			node.name = current.getChild("name").getValue();
			node.url = getUrl(current);
			//node.id = Integer.parseInt(current.getChild("id").getValue());
			node.id = count; // ??
			count++; // ??
			
			List<Element> techTags = current.getChild("tags").getChildren("technology_tag");
			Iterator<Element> it2 = techTags.iterator();
			
			node.technology_tags = new ArrayList<String>();
			
			while(it2.hasNext()){
				Element current2 = it2.next();
				if(!current2.getValue().equals("")){
				node.technology_tags.add(current2.getValue());
				}
			}
			
			tree.add(node);
		}
		
		
	}
	
	public void write() throws IOException{
		
	
		FileWriter writer = new FileWriter(new File("data/test-writing.xml"));
		writer.write(Parser.header);
		for(Node n:tree){
			String value = "\n<node id=\""+n.id+"\">\n";
			value += "	  <data key=\"author\">" + n.author + "</data>\n";
			value += "	  <data key=\"client\">" + n.client + "</data>\n";
			value += "	  <data key=\"company\">" + n.company + "</data>\n";
			value += "	  <data key=\"description\">" + n.description + "</data>\n";
			value += "	  <data key=\"name\">" + n.name + "</data>\n";
			//tags
			value += "	  <data key=\"tags\">  ";
			for(String s:n.technology_tags){
			 value+= s+", ";
			}
			value = value.substring(0, value.length()-2);
			value += "</data>\n";
			//fin des tags
			value += "	  <data key=\"urlproject\">" + n.url + "</data>\n";
			value += "  </node>\n";
			writer.write(value);
		}
		writer.write(edges());
		writer.write(tail);
		writer.flush();
		writer.close();
	}
	
	private String edges(){
		
		String aRetourner = "\n<!-- edges should be defined here-->\n";
		for(Node n:tree){
			for(Node m:tree){
				int distance = calculDistance(n, m);
				if(distance > 0 && (m.id != n.id)){
					aRetourner+="\n"+"<edge source=\""+m.id+"\" target=\""+n.id+"\"></edge>";
				}
			}
		}
		return aRetourner;
		
		
		
		
	}
	
	private int calculDistance(Node n, Node m) {
		
		int count = 0;
		
		for(String t:n.technology_tags){
			
			if(m.technology_tags.contains(t)) count ++;
		}
		
		return count;
	}

	public static void main(String[] args){
		Parser p = new Parser("data/web-test.xml");
		p.parse();
		try {
			p.write();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}

class Node {
	String name;
	List<String> technology_tags;
	String author;
	String client;
	String company;
	String description;
	String url;
	int id;
	List<Node> aRelier;
}
