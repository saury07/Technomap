package prefuse.demos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.assignment.SizeAction;
import prefuse.action.filter.GraphDistanceFilter;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.activity.Activity;
import prefuse.controls.ControlAdapter;
import prefuse.controls.DragControl;
import prefuse.controls.FocusControl;
import prefuse.controls.NeighborHighlightControl;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Graph;
import prefuse.data.Table;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.io.DataIOException;
import prefuse.data.io.GraphMLReader;
import prefuse.data.query.SearchQueryBinding;
import prefuse.data.search.SearchTupleSet;
import prefuse.data.tuple.TupleSet;
import prefuse.demos.ZipDecode.ZipColorAction;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.GraphLib;
import prefuse.util.GraphicsLib;
import prefuse.util.display.DisplayLib;
import prefuse.util.display.ItemBoundsListener;
import prefuse.util.force.ForceSimulator;
import prefuse.util.io.IOLib;
import prefuse.util.ui.JForcePanel;
import prefuse.util.ui.JSearchPanel;
import prefuse.util.ui.JValueSlider;
import prefuse.util.ui.UILib;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTable;
import prefuse.visual.expression.InGroupPredicate;

/**
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class GraphView extends JPanel {

	private static final String graph = "graph";
	private static final String nodes = "graph.nodes";
	private static final String edges = "graph.edges";
	private static final String node_criteria = "name";
	private static final String color_criteria = "author";

	private Visualization m_vis;
	public JPanel rightPanel;
	public JPanel optionPanel;
	public JFrame parentWindow;
	public String pathToGraph;
	static String webXML = "http://www.google.com";
	
	public GraphView(Graph g, String label, JFrame parent){
		this(g,label);
		this.parentWindow = parent;
	}

	public GraphView(Graph g, String label) {
		super(new BorderLayout());


		Graph gr1 =null;

		//Let's get the file !
		DataInputStream datastream = null;
		try{
			URL url = new URL(GraphView.webXML);
			InputStream stream = url.openStream();
			datastream = new DataInputStream(new BufferedInputStream(stream));
			String line = "";
			BufferedWriter out = new BufferedWriter(new FileWriter("data/web-output.xml"));
			while((line = datastream.readLine()) != null){
				out.write(line);
			}
			out.close();
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try {
				datastream.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		if(g != null){
			gr1 = g;
			//System.out.println("On recupere le graph parametre");
		}
		else {
			try {
				gr1 = new GraphMLReader().readGraph("data/test-writing.xml");
			} catch (DataIOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.exit(0);
			};
			//System.out.println("On prend le graph defaut");
		}
		final Graph gr = gr1;
		// create a new, empty visualization for our data
		m_vis = new Visualization();

		// --------------------------------------------------------------------
		// set up the renderers

		LabelRenderer lab = new LabelRenderer("name");
		lab.setRoundedCorner(8, 8);

		LabelRenderer tr = new LabelRenderer(null, "image");
		tr.setImageTextPadding(10);
		tr.setImagePosition(Constants.TOP);
		tr.setHorizontalAlignment(Constants.CENTER);
		tr.setVerticalAlignment(Constants.BOTTOM);
		tr.setHorizontalPadding(0);
		tr.setVerticalPadding(0);
		tr.setMaxImageDimensions(100,100);
		DefaultRendererFactory f = new DefaultRendererFactory(tr);
		f.add(new InGroupPredicate("graph.edges.tags"), tr);
		m_vis.setRendererFactory(f);

		// --------------------------------------------------------------------
		// register the data with a visualization

		// adds graph to visualization and sets renderer label field
		setGraph(gr, node_criteria);

		// fix selected focus nodes


		// --------------------------------------------------------------------
		// create actions to process the visual data

		int hops = 30;
		final GraphDistanceFilter filter = new GraphDistanceFilter(graph, hops);

		ColorAction fill = new ColorAction(nodes, 
				VisualItem.FILLCOLOR, ColorLib.rgb(200,200,255));
		fill.add(VisualItem.FIXED, ColorLib.rgb(255,100,100));
		fill.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255,200,125));



		ActionList draw = new ActionList();
		draw.add(filter);
		ActionList animate = new ActionList(Activity.INFINITY);
		animate.add(new ForceDirectedLayout(graph));
		//animate.add(fill);
		//animate.add(new ColorAnimator(Visualization.FOCUS_ITEMS, VisualItem.FILLCOLOR));
		animate.add(new RepaintAction());

		//Color management
		int[] palette = new int[] {
				ColorLib.rgb(255,180,180), ColorLib.rgb(190,190,255), ColorLib.rgb(0, 200, 100),
				ColorLib.rgb(250, 150, 150), ColorLib.rgb(150,250,150), ColorLib.rgb(150, 150, 250)
		};


		NodeColor nodes = new NodeColor("graph.nodes", color_criteria,
				Constants.NOMINAL, VisualItem.FILLCOLOR, palette);

		nodes.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255,200,125));

		ColorAction text = new ColorAction("graph.nodes",
				VisualItem.TEXTCOLOR, ColorLib.gray(0));
		ColorAction edges = new ColorAction("graph.edges",
				VisualItem.STROKECOLOR, ColorLib.gray(200));

		NodeSize nodes_size = new NodeSize();

		ActionList color = new ActionList();
		color.add(text);
		color.add(edges);
		color.add(nodes);
		color.add(nodes_size);


		// finally, we register our ActionList with the Visualization.
		// we can later execute our Actions by invoking a method on our
		// Visualization, using the name we've chosen below.



		m_vis.putAction("draw", draw);
		m_vis.putAction("color", color);
		m_vis.putAction("animate", animate);
		m_vis.runAfter("draw", "animate");


		// --------------------------------------------------------------------
		// set up a display to show the visualization

		Display display = new Display(m_vis);
		display.setSize(700,600);
		display.pan(350, 350);
		display.setForeground(Color.GRAY);
		display.setBackground(Color.WHITE);

		// main display controls
		display.addControlListener(new FocusControl(1));
		display.addControlListener(new DragControl());
		display.addControlListener(new PanControl());
		display.addControlListener(new ZoomControl());
		display.addControlListener(new WheelZoomControl());
		display.addControlListener(new ZoomToFitControl());
		display.addControlListener(new NeighborHighlightControl());
		display.addControlListener(new BackgroundControl(this));	

		// overview display
		//        Display overview = new Display(vis);
		//        overview.setSize(290,290);
		//        overview.addItemBoundsListener(new FitOverviewListener());

		display.setForeground(Color.GRAY);
		display.setBackground(Color.WHITE);

		// --------------------------------------------------------------------        
		// launch the visualization

		// create a panel for editing force values
		ForceSimulator fsim = ((ForceDirectedLayout)animate.get(0)).getForceSimulator();
		JForcePanel fpanel = new JForcePanel(fsim);
		this.optionPanel = fpanel;
		this.optionPanel.setOpaque(false);
		this.rightPanel = new JPanel(){
			protected void paintComponent(Graphics g){
				Graphics2D gr = (Graphics2D) g;
				int w = getWidth( );
				int h = getHeight( );

				// Paint a gradient from top to bottom
				GradientPaint gp = new GradientPaint(
						0, 0, Color.white,
						0, h, Color.pink );

				gr.setPaint( gp );
				gr.fillRect( 0, 0, w, h );
				this.setOpaque(false);
				super.paintComponent(g);
				this.setOpaque(true);
			}
		};
		this.rightPanel.add(this.optionPanel);
		//        JPanel opanel = new JPanel();
		//        opanel.setBorder(BorderFactory.createTitledBorder("Overview"));
		//        opanel.setBackground(Color.WHITE);
		//        opanel.add(overview);

		final JValueSlider slider = new JValueSlider("Distance", 0, hops, hops);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				filter.setDistance(slider.getValue().intValue());
				m_vis.run("draw");
			}
		});
		slider.setBackground(Color.WHITE);
		slider.setPreferredSize(new Dimension(300,30));
		slider.setMaximumSize(new Dimension(300,30));

		Box cf = new Box(BoxLayout.Y_AXIS);
		cf.add(slider);
		cf.setBorder(BorderFactory.createTitledBorder("Connectivity Filter"));
		fpanel.add(cf);

		//fpanel.add(opanel);

		fpanel.add(Box.createVerticalGlue());

		//Field for searching

		//JTextField search0 = new JTextField();
		//search0.setSize(700,100);

		// create a new JSplitPane to present the interface
		JSplitPane split = new JSplitPane();
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.add(display);
		//leftPanel.add(search0);
		split.setLeftComponent(leftPanel);
		split.setRightComponent(this.rightPanel);
		split.setOneTouchExpandable(true);
		split.setContinuousLayout(false);
		split.setDividerLocation(800);

		//create the node viewer panel
		JPanel fp2 = new JPanel();
		fp2.setBackground(Color.blue);
		display.addControlListener(new NodeInfoControl(rightPanel));


		final String FOCUS = Visualization.FOCUS_ITEMS;

		final Action update = new GraphColorAction(FOCUS);
		m_vis.putAction("update", update);



		final TupleSet focus = m_vis.getFocusGroup(FOCUS);
		// create the search query binding
		SearchQueryBinding searchQ = new SearchQueryBinding(gr.getNodeTable(), "author");
		searchQ.addField("name");
		searchQ.addField("client");
		searchQ.addField("company");
		searchQ.addField("tags");
		final SearchTupleSet search = searchQ.getSearchSet(); 

		// create the listener that collects search results into a focus set
		search.addTupleSetListener(new TupleSetListener() {
			public void tupleSetChanged(TupleSet t, Tuple[] add, Tuple[] rem) {
				m_vis.cancel("animate");

				// invalidate changed tuples, add them all to the focus set
				focus.clear();
				System.out.println("Search results : "+add.length);
				System.out.println("Items to remove : "+rem.length);
				for ( int i=0; i<add.length; ++i ) {
					focus.addTuple(add[i]);
				}
				for ( int i=0; i<rem.length; ++i ) {
					//((VisualItem)rem[i]).setValidated(false);
					focus.removeTuple(rem[i]);
				}
				System.out.println("Tuples in search : "+focus.getTupleCount());


				m_vis.run("animate");

				m_vis.run("color");


			}
		});
		m_vis.addFocusGroup(Visualization.SEARCH_ITEMS, search);

		// create and parameterize a search panel for searching on zip code
		final JSearchPanel searcher = searchQ.createSearchPanel();
		searcher.setLabelText("search>"); // the search box label
		searcher.setShowCancel(true); // don't show the cancel query button
		searcher.setShowBorder(true); // don't show the search box border
		searcher.setFont(FontLib.getFont("Georgia", Font.PLAIN, 22));
		//searcher.setBackground(ColorLib.getGrayscale(50));
		searcher.setForeground(ColorLib.getColor(100,100,75));
		leftPanel.add(searcher); // add the search box as a sub-component of the display
		searcher.setBounds(10, getHeight()-40, 120, 30);





		add(split);
		// now we run our action list
		m_vis.run("draw");
		m_vis.run("color");

	}

	public void close(){
		if(this.parentWindow != null)
			this.parentWindow.dispose();
	}

	public void setGraph(Graph g, String label) {
		// update labeling
		DefaultRendererFactory drf = (DefaultRendererFactory)
		m_vis.getRendererFactory();
		((LabelRenderer)drf.getDefaultRenderer()).setTextField(label);

		// update graph
		//m_vis.removeGroup(graph);
		VisualGraph vg = m_vis.addGraph(graph, g);
		m_vis.setValue(edges, null, VisualItem.INTERACTIVE, Boolean.FALSE);
		VisualItem f = (VisualItem)vg.getNode(0);
		m_vis.getGroup(Visualization.FOCUS_ITEMS).setTuple(f);
		f.setFixed(false);
	}

	// ------------------------------------------------------------------------
	// Main and demo methods

	public static void main(String[] args) {
		UILib.setPlatformLookAndFeel();

		// create graphview
		String datafile = null;
		String label = "label";
		if ( args.length > 1 ) {
			datafile = args[0];
			label = args[1];
		}

		JFrame frame = demo(null, label);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public static JFrame demo() {
		return demo((String)null, "label", true);
	}

	public static JFrame demo(String datafile, String label, boolean bool) {
		Graph g = null;
		if ( datafile == null ) {
			g = GraphLib.getGrid(15,15);
			label = "label";
		} else {
			try {
				g = new GraphMLReader().readGraph(datafile);
			} catch ( Exception e ) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		return demo(g, label);
	}


	public static JFrame demo(Graph g, String label) {
		JFrame frame = new JFrame("p r e f u s e  |  g r a p h v i e w");
		final GraphView view = new GraphView(g, label, frame);

		// set up menu
		JMenu dataMenu = new JMenu("Data");
		dataMenu.add(new OpenGraphAction(view));


		JMenuBar menubar = new JMenuBar();
		menubar.add(dataMenu);

		// launch window

		frame.setJMenuBar(menubar);
		frame.setContentPane(view);
		frame.pack();
		frame.setVisible(true);

		frame.addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
				view.m_vis.run("layout");
			}
			public void windowDeactivated(WindowEvent e) {
				view.m_vis.cancel("layout");
			}
		});

		return frame;
	}


	// ------------------------------------------------------------------------

	/**
	 * Swing menu action that loads a graph into the graph viewer.
	 */
	public abstract static class GraphMenuAction extends AbstractAction {
		private GraphView m_view;
		public GraphMenuAction(String name, String accel, GraphView view) {
			m_view = view;
			this.putValue(AbstractAction.NAME, name);
			this.putValue(AbstractAction.ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(accel));
		}
		public void actionPerformed(ActionEvent e) {
			m_view.setGraph(getGraph(), "label");
		}
		protected abstract Graph getGraph();
	}

	public static class OpenGraphAction extends AbstractAction {
		private GraphView m_view;

		public OpenGraphAction(GraphView view) {
			m_view = view;
			this.putValue(AbstractAction.NAME, "Open File...");
			this.putValue(AbstractAction.ACCELERATOR_KEY,
					KeyStroke.getKeyStroke("ctrl O"));
		}
		public void actionPerformed(ActionEvent e) {
			Graph g = IOLib.getGraphFile(null);
			if ( g == null ) return;

			String label = getLabel(m_view, g);
			if ( label != null ) {
				JFrame frame = demo(g, label);
				m_view.close();
				//m_view.setGraph(g, label);
			}
		}
		public static String getLabel(Component c, Graph g) {
			// get the column names
			Table t = g.getNodeTable();
			int  cc = t.getColumnCount();
			String[] names = new String[cc];
			for ( int i=0; i<cc; ++i )
				names[i] = t.getColumnName(i);

			// where to store the result
			final String[] label = new String[1];

			// -- build the dialog -----
			// we need to get the enclosing frame first
			while ( c != null && !(c instanceof JFrame) ) {
				c = c.getParent();
			}
			final JDialog dialog = new JDialog(
					(JFrame)c, "Choose Label Field", true);

			// create the ok/cancel buttons
			final JButton ok = new JButton("OK");
			ok.setEnabled(false);
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dialog.setVisible(false);
				}
			});
			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					label[0] = null;
					dialog.setVisible(false);
				}
			});

			// build the selection list
			final JList list = new JList(names);
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			list.getSelectionModel().addListSelectionListener(
					new ListSelectionListener() {
						public void valueChanged(ListSelectionEvent e) {
							int sel = list.getSelectedIndex(); 
							if ( sel >= 0 ) {
								ok.setEnabled(true);
								label[0] = (String)list.getModel().getElementAt(sel);
							} else {
								ok.setEnabled(false);
								label[0] = null;
							}
						}
					});
			JScrollPane scrollList = new JScrollPane(list);

			JLabel title = new JLabel("Choose a field to use for node labels:");

			// layout the buttons
			Box bbox = new Box(BoxLayout.X_AXIS);
			bbox.add(Box.createHorizontalStrut(5));
			bbox.add(Box.createHorizontalGlue());
			bbox.add(ok);
			bbox.add(Box.createHorizontalStrut(5));
			bbox.add(cancel);
			bbox.add(Box.createHorizontalStrut(5));

			// put everything into a panel
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(title, BorderLayout.NORTH);
			panel.add(scrollList, BorderLayout.CENTER);
			panel.add(bbox, BorderLayout.SOUTH);
			panel.setBorder(BorderFactory.createEmptyBorder(5,2,2,2));

			// show the dialog
			dialog.setContentPane(panel);
			dialog.pack();
			dialog.setLocationRelativeTo(c);
			dialog.setVisible(true);
			dialog.dispose();

			// return the label field selection
			return label[0];
		}
	}

	public static class FitOverviewListener implements ItemBoundsListener {
		private Rectangle2D m_bounds = new Rectangle2D.Double();
		private Rectangle2D m_temp = new Rectangle2D.Double();
		private double m_d = 15;
		public void itemBoundsChanged(Display d) {
			d.getItemBounds(m_temp);
			GraphicsLib.expand(m_temp, 25/d.getScale());

			double dd = m_d/d.getScale();
			double xd = Math.abs(m_temp.getMinX()-m_bounds.getMinX());
			double yd = Math.abs(m_temp.getMinY()-m_bounds.getMinY());
			double wd = Math.abs(m_temp.getWidth()-m_bounds.getWidth());
			double hd = Math.abs(m_temp.getHeight()-m_bounds.getHeight());
			if ( xd>dd || yd>dd || wd>dd || hd>dd ) {
				m_bounds.setFrame(m_temp);
				DisplayLib.fitViewToBounds(d, m_bounds, 0);
			}
		}
	}

} // end of class GraphView

class BackgroundControl extends PanControl {

	private GraphView graph;

	public BackgroundControl(GraphView graph){
		this.graph = graph;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		super.mouseClicked(e);
		//	System.out.println("should show option menu");
		graph.rightPanel.removeAll();
		graph.rightPanel.repaint();
		//	graph.rightPanel.setBackground(Color.RED);
		graph.rightPanel.add(graph.optionPanel);
		graph.rightPanel.validate();
	}

}

class GraphColorAction extends ColorAction {
	public GraphColorAction(String group) {
		super(group, VisualItem.FILLCOLOR);
	}

	public int getColor(VisualItem item) {
		System.out.println("getting color");
		if ( item.isInGroup(Visualization.SEARCH_ITEMS) ) {
			return ColorLib.gray(255);
		} else {
			return ColorLib.rgb(100,100,75);
		}
	}
}

class NodeSize extends SizeAction{
	private ArrayList<VisualItem> liste = new ArrayList<VisualItem>();
	@Override
	public double getSize(VisualItem item) {
		if(item.getRow()==0) liste.clear();				
		Iterator it2 = m_vis.getGroup(Visualization.SEARCH_ITEMS).tuples();
		if(!it2.hasNext())return super.getSize(item);
		while(it2.hasNext()){
			if(!liste.contains(item)){
				Tuple t = (Tuple)it2.next();
				Tuple t1 = (Tuple)item.getSourceTuple();
				if(t.getRow() == t1.getRow()){

					return 2.0;

				}
			}

		}
		liste.add(item);
		return super.getSize(item);
	}
}

class NodeColor extends DataColorAction{

	private ArrayList<VisualItem> liste = new ArrayList<VisualItem>();

	public NodeColor(String group, String dataField, int dataType,
			String colorField, int[] palette) {
		super(group, dataField, dataType, colorField, palette);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getColor(VisualItem item) {
		if(item.getRow()==0) liste.clear();				
		Iterator it2 = m_vis.getGroup(Visualization.SEARCH_ITEMS).tuples();
		if(!it2.hasNext())return super.getColor(item);
		while(it2.hasNext()){
			if(!liste.contains(item)){
				Tuple t = (Tuple)it2.next();
				Tuple t1 = (Tuple)item.getSourceTuple();
				if(t.getRow() == t1.getRow()){

					return super.getColor(item);

				}
			}

		}
		liste.add(item);
		int c = super.getColor(item);
		return ColorLib.setAlpha(c, 60);

	}



}

class NodeInfoControl extends ControlAdapter {

	private JPanel optionPanel;
	private JPanel graphPanel;

	public NodeInfoControl(JPanel option){
		optionPanel = option;

	}

	public NodeInfoControl(JPanel option, JPanel graph){
		optionPanel = option;
		graphPanel = graph;
	}

	public NodeInfoControl(){

	}

	@Override
	public void itemClicked(VisualItem item, MouseEvent e) {
		// TODO Auto-generated method stub
		super.itemClicked(item, e);
		//System.out.println(item.get("author")+" node has been clicked");
		//optionPanel.setBackground(Color.blue);
		optionPanel.removeAll();
		optionPanel.repaint();
		optionPanel.add(generatePaneFromNode(item));
		optionPanel.validate();

	}

	@Override
	public void itemEntered(VisualItem item, MouseEvent e) {
		// TODO Auto-generated method stub
		super.itemEntered(item, e);
	}

	private JPanel generatePaneFromNode(VisualItem item){
		JPanel pane = new JPanel();
		pane.setOpaque(false);
		Box mainBox = new Box(BoxLayout.Y_AXIS);


		mainBox.setAlignmentX(0.5f);

		JLabel title = new JLabel(title(""+item.get("name")));
		title.setAlignmentX(1f);


		final String url = ""+item.get("urlproject");
		ImageIcon image = new ImageIcon(getNodePicture(item));
		JLabel picture = new JLabel(image);

		picture.setAlignmentX(1f);
		picture.setCursor(new Cursor(Cursor.HAND_CURSOR));
		picture.addMouseListener(new MouseAdapter(){

			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (URISyntaxException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		});
		picture.setHorizontalAlignment(SwingConstants.LEFT);
		picture.setText("<html></html>");

		JLabel subTitle = new JLabel(bold(""+item.get("client")));
		subTitle.setAlignmentX(1f);

		JTextArea description = new JTextArea(2,15);
		description.setText(""+item.get("description"));
		description.setEditable(false);
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		description.setAlignmentX(1f);
		description.setOpaque(false);

		JTextArea area = new JTextArea(20,15);
		area.setText(""+item.get("tags"));
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setOpaque(false);
		JScrollPane sp = new JScrollPane(area);  
		sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		area.setCaretPosition(0);
		sp.setAlignmentX(1f);

		JLabel author = new JLabel("<html><b>Author : </b>"+item.get("author")+"</html>");
		author.setAlignmentX(1f);


		mainBox.add(title);
		mainBox.add(Box.createRigidArea(new Dimension(0,5)));
		mainBox.add(picture);
		mainBox.add(Box.createRigidArea(new Dimension(0,5)));
		mainBox.add(subTitle);
		mainBox.add(Box.createRigidArea(new Dimension(0,5)));
		mainBox.add(description);
		mainBox.add(Box.createRigidArea(new Dimension(0,5)));
		mainBox.add(sp);
		mainBox.add(Box.createRigidArea(new Dimension(0,5)));
		mainBox.add(author);


		pane.add(mainBox);

		return pane;
	}

	private String title(String text){
		return "<html><h1>"+text+"</h1></html>";
	}

	private String bold(String text){
		return "<html><b>"+text+"</b></html>";
	}

	private String normal(String text){
		return "<html>"+text+"</html>";
	}

	private Image getNodePicture(VisualItem item){
		URL url = null;
		try {
			url = new URL(getImage(""+item.get("urlproject")));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(url);
		Image ret = java.awt.Toolkit.getDefaultToolkit().getDefaultToolkit().createImage(url);
		ret = ret.getScaledInstance(140, 100, 0);
		return ret;
	}

	private String getImage(String url){
		String ret = "";
		//if(url.endsWith(".jpg") || url.endsWith(".png")) return url;
		if(url.contains("youtube")){
			int start = url.indexOf("v=")+2;
			ret = url.substring(start, url.length());
		}
		else{
			return url;
		}
		return "http://img.youtube.com/vi/"+ret+"/default.jpg";
	}

}
