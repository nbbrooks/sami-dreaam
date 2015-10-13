package dreaam.developer;

import dreaam.wizard.EventWizardSingleton;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import sami.config.DomainConfig;
import sami.config.DomainConfigManager;
import sami.mission.Edge;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Vertex;

/**
 * Dialog for selecting an event to add to the mSpec using its associated wizard
 *
 * @author nbb
 */
public class EventWizardD extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(SelectGlobalVariableD.class.getName());
    protected MissionPlanSpecification mSpec;
    protected Place p1;
    protected Point graphPoint;
    protected Graph<Vertex, Edge> dsgGraph;
    protected AbstractLayout<Vertex, Edge> layout;
    protected VisualizationViewer<Vertex, Edge> vv;

    // Layout
    private final static int BUTTON_WIDTH = 250;
    private final static int BUTTON_HEIGHT = 50;

    protected JScrollPane eventsSP;
    protected JPanel eventsP;
    protected javax.swing.JButton cancelB;

    public EventWizardD(java.awt.Frame parent, boolean modal, MissionPlanSpecification mSpec, Place p1, Graph<Vertex, Edge> dsgGraph, AbstractLayout<Vertex, Edge> layout, VisualizationViewer<Vertex, Edge> vv) {
        super(parent, modal);
        this.mSpec = mSpec;
        this.p1 = p1;
        this.dsgGraph = dsgGraph;
        this.layout = layout;
        this.vv = vv;
        initComponents();
        setTitle("EventWizardD");
    }

    public EventWizardD(java.awt.Frame parent, boolean modal, MissionPlanSpecification mSpec, Point graphPoint, Graph<Vertex, Edge> dsgGraph, AbstractLayout<Vertex, Edge> layout, VisualizationViewer<Vertex, Edge> vv) {
        super(parent, modal);
        this.mSpec = mSpec;
        this.graphPoint = graphPoint;
        this.dsgGraph = dsgGraph;
        this.layout = layout;
        this.vv = vv;
        initComponents();
        setTitle("EventWizardD");
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        eventsP = new JPanel();
        eventsP.setLayout(new BoxLayout(eventsP, BoxLayout.Y_AXIS));

        addEvents(eventsP);

        eventsSP = new JScrollPane();
        eventsSP.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        eventsSP.setViewportView(eventsP);

        JPanel buttonsP = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;

        cancelB = new javax.swing.JButton();
        cancelB.setText("Cancel");
        cancelB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelBActionPerformed(evt);
            }
        });
        cancelB.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        buttonsP.add(cancelB, constraints);
        constraints.gridy = constraints.gridy + 1;

        BoxLayout boxLayout = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
        getContentPane().setLayout(boxLayout);
        getContentPane().add(eventsSP);
        getContentPane().add(buttonsP);

        // Adjust dialog size
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenHeight = gd.getDisplayMode().getHeight();
        setPreferredSize(new Dimension(getPreferredSize().width, (int) (screenHeight * 0.9)));

        pack();
    }

    public void addEvents(JPanel panel) {
        DefaultMutableTreeNode eventTree = (DefaultMutableTreeNode) DomainConfigManager.getInstance().getDomainConfiguration().eventTree;
        for (int i = 0; i < eventTree.getChildCount(); i++) {
            addNode(eventTree.getChildAt(i), panel);
        }
    }

    public void addNode(TreeNode aliasNode, JPanel panel) {
        if (aliasNode instanceof DomainConfig.LeafNode) {
            // At a event, add as Jbutton and return
            final DomainConfig.LeafNode leafNode = (DomainConfig.LeafNode) aliasNode;
            JButton eventB = new JButton(leafNode.displayName);
            eventB.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean success;
                    if (p1 != null) {
                        success = EventWizardSingleton.getInstance().runWizard(leafNode.className, mSpec, p1, dsgGraph, layout, vv);
                    } else {
                        success = EventWizardSingleton.getInstance().runWizard(leafNode.className, mSpec, graphPoint, dsgGraph, layout, vv);
                    }
                    setVisible(false);
                }
            });
            panel.add(eventB);
        } else if (aliasNode instanceof DefaultMutableTreeNode) {
            // At a category, add label and recurse
            DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(aliasNode.toString());
            panel.add(new JLabel(categoryNode.toString()));
            for (int i = 0; i < aliasNode.getChildCount(); i++) {
                addNode(aliasNode.getChildAt(i), panel);
            }
            if (categoryNode.getChildCount() == 0) {
                categoryNode.removeFromParent();
            }
        } else {
            LOGGER.severe("Could not handle TreeNode: " + aliasNode + ": of class: " + aliasNode.getClass());
        }
    }

    private void cancelBActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
    }
}
