package dreaam.developer;

import dreaam.developer.SelectTokenD.EdgeType;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.MultiLayerTransformer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.apache.commons.collections15.Transformer;
import sami.event.ReflectedEventSpecification;
import sami.gui.GuiConfig;
import sami.mission.Edge;
import sami.mission.InEdge;
import sami.mission.InTokenRequirement;
import sami.mission.MissionPlanSpecification;
import sami.mission.OutEdge;
import sami.mission.OutTokenRequirement;
import sami.mission.Place;
import sami.mission.RequirementSpecification;
import sami.mission.TokenRequirement;
import sami.mission.Transition;
import sami.mission.Vertex;
import sami.mission.Vertex.FunctionMode;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * GUI for building DREAAM plays Controls: Left click: Click Place/Transition to
 * begin/finish an Edge Drag Place/Transition to move it Right click: Click
 * empty space to create new Places/Transitions Click Place/Transition/Edge to
 * edit its attributes or delete it Middle click OR Shift + Left click OR Left
 * click + Right click: Drag to translate view Mouse wheel: Rotate to zoom view
 * in/out
 *
 * @author pscerri
 */
public class TaskModelEditor extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(TaskModelEditor.class.getName());
    // This increases the "grab" radius for Place/Transition/Edge objects to make things easier to select
    public static final int CLICK_RADIUS = 5;
    // Length of grid segment for "snapping" vertices
    public static final int GRID_LENGTH = 50;
    DirectedSparseGraph<Vertex, Edge> graph;
    AbstractLayout<Vertex, Edge> layout;
    MissionPlanSpecification mSpec = null;
    Mediator mediator = new Mediator();
    VisualizationViewer<Vertex, Edge> vv;   // The visual component and renderer for the graph
    String instructions = "Guess";
    EditingModalGraphMouse<Vertex, Edge> graphMouse = null;
    private FunctionMode editorMode = FunctionMode.Nominal;
    private Vertex expandedNomVertex = null;
    private DREAAM dreaam;
    TaskModelEditor.MyMouseListener mml;

    public TaskModelEditor(DREAAM dreaam, MissionPlanSpecification spec) {
        this(spec);
        this.dreaam = dreaam;
    }

    public TaskModelEditor(MissionPlanSpecification spec) {
        graph = new DirectedSparseGraph<Vertex, Edge>();
        layout = new StaticLayout<Vertex, Edge>(graph, new Dimension(600, 600));
        vv = new VisualizationViewer<Vertex, Edge>(layout);
        vv.setBackground(GuiConfig.BACKGROUND_COLOR);
        setGraph(spec);

        // EDGE
        vv.getRenderContext().setArrowDrawPaintTransformer(new Transformer<Edge, Paint>() {
            public Paint transform(Edge edge) {
                switch (edge.getVisibilityMode()) {
                    case Full:
                    case Background:
                        return GuiConfig.EDGE_COLOR;
                    case None:
                    default:
                        return GuiConfig.INVIS_EDGE_COLOR;
                }
            }
        });

        vv.getRenderContext().setArrowFillPaintTransformer(new Transformer<Edge, Paint>() {
            public Paint transform(Edge edge) {
                switch (edge.getVisibilityMode()) {
                    case Full:
                    case Background:
                        return GuiConfig.EDGE_COLOR;
                    case None:
                    default:
                        return GuiConfig.INVIS_EDGE_COLOR;
                }
            }
        });

        vv.getRenderContext().setEdgeDrawPaintTransformer(new Transformer<Edge, Paint>() {
            public Paint transform(Edge edge) {
                switch (edge.getVisibilityMode()) {
                    case Full:
                    case Background:
                        return GuiConfig.EDGE_COLOR;
                    case None:
                    default:
                        return GuiConfig.INVIS_EDGE_COLOR;
                }
            }
        });

        vv.getRenderContext().setEdgeFontTransformer(new Transformer<Edge, Font>() {
            @Override
            public Font transform(Edge edge) {
                switch (edge.getVisibilityMode()) {
                    case Full:
                    case Background:
                        return new java.awt.Font("Dialog", Font.PLAIN, 14);
                    case None:
                    default:
                        return null;
                }
            }
        });

        vv.getRenderContext().setEdgeLabelTransformer(new Transformer<Edge, String>() {
            @Override
            public String transform(Edge edge) {
                switch (edge.getVisibilityMode()) {
                    case Full:
                        return edge.getShortTag();
                    case Background:
                    case None:
                    default:
                        return null;
                }
            }
        });

        vv.getRenderContext().setEdgeStrokeTransformer(new Transformer<Edge, Stroke>() {
            @Override
            public Stroke transform(Edge edge) {
                switch (edge.getVisibilityMode()) {
                    case Full:
                        if (edge.getFunctionMode() == FunctionMode.Recovery) {
                            return new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 10.0f}, 0.0f);
                        } else {
                            return new BasicStroke(1);
                        }
                    case Background:
                    case None:
                    default:
                        return null;
                }
            }
        });

        // VERTEX
        vv.getRenderContext()
                .setVertexDrawPaintTransformer(new Transformer<Vertex, Paint>() {
                    @Override
                    public Paint transform(Vertex vertex) {
                        switch (vertex.getVisibilityMode()) {
                            case Full:
                                if (vertex.getBeingModified()) {
                                    return GuiConfig.SEL_VERTEX_COLOR;
                                } else {
                                    return GuiConfig.VERTEX_COLOR;
                                }
                            case Background:
                                if (vertex.getBeingModified()) {
                                    return GuiConfig.SEL_VERTEX_COLOR;
                                } else {
                                    return GuiConfig.BKGND_VERTEX_COLOR;
                                }
                            case None:
                            default:
                                return null;
                        }
                    }
                });

        vv.getRenderContext()
                .setVertexFillPaintTransformer(new Transformer<Vertex, Paint>() {
                    @Override
                    public Paint transform(Vertex vertex) {
                        switch (vertex.getVisibilityMode()) {
                            case Full:
                                if (vertex instanceof Place) {
                                    Place place = (Place) vertex;
                                    if (place.isStart()) {
                                        return GuiConfig.START_PLACE_COLOR;
                                    } else if (place.isEnd()) {
                                        return GuiConfig.END_PLACE_COLOR;
                                    } else {
                                        return GuiConfig.PLACE_COLOR;
                                    }
                                } else if (vertex instanceof Transition) {
                                    return GuiConfig.TRANSITION_COLOR;
                                }
                                return null;
                            case Background:
                                return GuiConfig.BKGND_VERTEX_COLOR;
                            case None:
                            default:
                                return null;
                        }
                    }
                });

        vv.getRenderContext()
                .setVertexFontTransformer(new Transformer<Vertex, Font>() {
                    @Override
                    public Font transform(Vertex vertex) {
                        switch (vertex.getVisibilityMode()) {
                            case Full:
                            case Background:
                                return new java.awt.Font("Dialog", Font.PLAIN, 14);
                            case None:
                            default:
                                return null;
                        }
                    }
                });

        vv.getRenderContext()
                .setVertexLabelTransformer(new Transformer<Vertex, String>() {
                    @Override
                    public String transform(Vertex vertex) {
                        switch (vertex.getVisibilityMode()) {
                            case Full:
                                return vertex.getShortTag();
                            case Background:
                            case None:
                            default:
                                return null;
                        }
                    }
                });

        vv.getRenderContext()
                .setVertexShapeTransformer(new Transformer<Vertex, Shape>() {
                    @Override
                    public Shape transform(Vertex vertex) {
                        if (vertex instanceof Transition) {
                            return ((Transition) vertex).getShape();
                        } else if (vertex instanceof Place) {
                            return ((Place) vertex).getShape();
                        } else {
                            return null;
                        }
                    }
                });

        vv.getRenderContext().setVertexStrokeTransformer(new Transformer<Vertex, Stroke>() {
            @Override
            public Stroke transform(Vertex vertex) {
                switch (vertex.getVisibilityMode()) {
                    case Full:
                        if (vertex.getBeingModified()) {
                            if (vertex.getFunctionMode() == FunctionMode.Recovery) {
                                return new BasicStroke(10, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 10.0f}, 0.0f);
                            } else {
                                return new BasicStroke(10);
                            }
                        } else {
                            if (vertex.getFunctionMode() == FunctionMode.Recovery) {
                                return new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 10.0f}, 0.0f);
                            } else {
                                return new BasicStroke(1);
                            }
                        }
                    case Background:
                        if (vertex.getBeingModified()) {
                            if (vertex.getFunctionMode() == FunctionMode.Recovery) {
                                return new BasicStroke(10, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 10.0f}, 0.0f);
                            } else {
                                return new BasicStroke(10);
                            }
                        } else {
                            if (vertex.getFunctionMode() == FunctionMode.Recovery) {
                                return new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 10.0f}, 0.0f);
                            } else {
                                return new BasicStroke(1);
                            }
                        }
                    case None:
                    default:
                        return null;
                }
            }
        });

        vv.setVertexToolTipTransformer(
                new Transformer<Vertex, String>() {
                    @Override
                    public String transform(Vertex vertex) {
                        switch (vertex.getVisibilityMode()) {
                            case Full:
                                return vertex.getTag();
                            case Background:
                            case None:
                            default:
                                return null;
                        }
                    }
                });

        final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);

        setLayout(
                new BorderLayout());
        add(panel, BorderLayout.CENTER);
        // Comment this line in and the next four out to switch to my mouse handler
        mml = new TaskModelEditor.MyMouseListener();

        vv.addMouseListener(mml);

        vv.addMouseMotionListener(mml);

        vv.addMouseWheelListener(mml);
        final ScalingControl scaler = new CrossoverScalingControl();
        JButton plus = new JButton("+");

        plus.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        scaler.scale(vv, 1.1f, vv.getCenter());
                    }
                });
        JButton minus = new JButton("-");
        minus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1 / 1.1f, vv.getCenter());
            }
        });

        JButton help = new JButton("Help");
        help.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(vv, instructions);
            }
        });

        JPanel controls = new JPanel();
        controls.add(plus);
        controls.add(minus);
        controls.add(help);
        add(controls, BorderLayout.SOUTH);

    }

    MissionPlanSpecification getModel() {
        writeModel();
        return mSpec;
    }

    public void setMode(FunctionMode mode) {
        this.editorMode = mode;
        refreshGraphVisibility();
        vv.repaint();
    }

    public void refreshGraphVisibility() {
        switch (editorMode) {
            case Nominal:
                for (Vertex vertex : graph.getVertices()) {
                    if (vertex.getFunctionMode() == FunctionMode.Nominal) {
                        vertex.setVisibilityMode(GuiConfig.VisibilityMode.Full);
                    } else if (vertex.getFunctionMode() == FunctionMode.Recovery) {
                        vertex.setVisibilityMode(GuiConfig.VisibilityMode.None);
                    }
                }
                for (Edge edge : graph.getEdges()) {
                    if (edge.getStart().getFunctionMode() == FunctionMode.Nominal && edge.getEnd().getFunctionMode() == FunctionMode.Nominal) {
                        edge.setVisibilityMode(GuiConfig.VisibilityMode.Full);
                    } else {
                        edge.setVisibilityMode(GuiConfig.VisibilityMode.None);
                    }
                }
                break;
            case Recovery:
                for (Vertex vertex : graph.getVertices()) {
                    if (vertex.getFunctionMode() == FunctionMode.Recovery) {
                        vertex.setVisibilityMode(GuiConfig.VisibilityMode.Background);
                    } else if (vertex.getFunctionMode() == FunctionMode.Nominal
                            && vertex instanceof Place) {
                        vertex.setVisibilityMode(GuiConfig.VisibilityMode.Background);
                    } else if (vertex.getFunctionMode() == FunctionMode.Nominal
                            && vertex instanceof Transition) {
                        vertex.setVisibilityMode(GuiConfig.VisibilityMode.Background);
                    }
                }
                for (Edge edge : graph.getEdges()) {
                    edge.setVisibilityMode(GuiConfig.VisibilityMode.None);
                }
                if (expandedNomVertex != null) {
                    expandNeighborVisibility(expandedNomVertex, GuiConfig.VisibilityMode.Full, 1, new ArrayList<Vertex>());
                    fwdChainNeighborVisibility(expandedNomVertex, GuiConfig.VisibilityMode.Full, new ArrayList<Vertex>());
                }
                if (mml.edgeStartVertex != null) {
                    expandNeighborVisibility(mml.edgeStartVertex, GuiConfig.VisibilityMode.Full, 1, new ArrayList<Vertex>());
                }
                break;
        }
    }

    /**
     *
     * @param vertex
     * @param visibilityMode
     * @param applyToEdges
     */
    public void setVertexVisibility(Vertex vertex, GuiConfig.VisibilityMode visibilityMode, boolean applyToEdges) {
        vertex.setVisibilityMode(visibilityMode);
        if (applyToEdges) {
            if (vertex instanceof Place) {
                Place place = (Place) vertex;
                for (Edge edge : place.getInEdges()) {
                    edge.setVisibilityMode(visibilityMode);
                }
                for (Edge edge : place.getOutEdges()) {
                    edge.setVisibilityMode(visibilityMode);
                }
            } else if (vertex instanceof Transition) {
                Transition transition = (Transition) vertex;
                for (Edge edge : transition.getInEdges()) {
                    edge.setVisibilityMode(visibilityMode);
                }
                for (Edge edge : transition.getOutEdges()) {
                    edge.setVisibilityMode(visibilityMode);
                }
            }
        }
    }

    /**
     * Looks at neighbors of the passed in vertex - any matching the visibility
     * mode passed in have their visibility and all their edges' visibility set
     * to makeVisible If chain is true, the call is recursed for neighbors that
     * match the visibility mode
     *
     * @param vertex
     * @param functionMode
     * @param visibilityMode
     * @param chain
     */
    public void setNeighborVisibility(Vertex vertex, FunctionMode functionMode, GuiConfig.VisibilityMode visibilityMode, boolean chain, ArrayList<Vertex> previouslyVisited) {
        if (vertex instanceof Place) {
            Place place = (Place) vertex;
            Transition neighborTransition;
            for (OutEdge edge : place.getInEdges()) {
                neighborTransition = edge.getStart();
                if (previouslyVisited.contains(neighborTransition)) {
                    continue;
                } else if (neighborTransition.getFunctionMode() == functionMode) {
                    edge.setVisibilityMode(visibilityMode);
                    neighborTransition.setVisibilityMode(visibilityMode);
                    for (Edge e2 : neighborTransition.getInEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    for (Edge e2 : neighborTransition.getOutEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    if (chain) {
                        previouslyVisited.add(place);
                        setNeighborVisibility(neighborTransition, functionMode, visibilityMode, chain, previouslyVisited);
                    }
                }
            }
            for (InEdge edge : place.getOutEdges()) {
                neighborTransition = edge.getEnd();
                if (previouslyVisited.contains(neighborTransition)) {
                    continue;
                } else if (neighborTransition.getFunctionMode() == functionMode) {
                    edge.setVisibilityMode(visibilityMode);
                    neighborTransition.setVisibilityMode(visibilityMode);
                    for (Edge e2 : neighborTransition.getInEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    for (Edge e2 : neighborTransition.getOutEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    if (chain) {
                        previouslyVisited.add(place);
                        setNeighborVisibility(neighborTransition, functionMode, visibilityMode, chain, previouslyVisited);
                    }
                }
            }
        } else if (vertex instanceof Transition) {
            Transition transition = (Transition) vertex;
            Place neighborPlace;
            for (InEdge edge : transition.getInEdges()) {
                neighborPlace = edge.getStart();
                if (previouslyVisited.contains(neighborPlace)) {
                    continue;
                } else if (neighborPlace.getFunctionMode() == functionMode) {
                    edge.setVisibilityMode(visibilityMode);
                    neighborPlace.setVisibilityMode(visibilityMode);
                    for (Edge e2 : neighborPlace.getInEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    for (Edge e2 : neighborPlace.getOutEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    if (chain) {
                        previouslyVisited.add(transition);
                        setNeighborVisibility(neighborPlace, functionMode, visibilityMode, chain, previouslyVisited);
                    }
                }
            }
            for (OutEdge edge : transition.getOutEdges()) {
                neighborPlace = edge.getEnd();
                if (previouslyVisited.contains(neighborPlace)) {
                    continue;
                } else if (neighborPlace.getFunctionMode() == functionMode) {
                    edge.setVisibilityMode(visibilityMode);
                    neighborPlace.setVisibilityMode(visibilityMode);
                    for (Edge e2 : neighborPlace.getInEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    for (Edge e2 : neighborPlace.getOutEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    if (chain) {
                        previouslyVisited.add(transition);
                        setNeighborVisibility(neighborPlace, functionMode, visibilityMode, chain, previouslyVisited);
                    }
                }
            }
        }
    }

    public void fwdChainNeighborVisibility(Vertex startVertex, GuiConfig.VisibilityMode visibilityMode, ArrayList<Vertex> previouslyVisited) {
        if (startVertex instanceof Place) {
            Place startPlace = (Place) startVertex;
            Transition endTransition;
            for (InEdge edge : startPlace.getOutEdges()) {
                endTransition = (Transition) edge.getEnd();
                if (previouslyVisited.contains(endTransition)) {
                    continue;
                } else if (endTransition.getFunctionMode() == FunctionMode.Recovery) {
                    edge.setVisibilityMode(visibilityMode);
                    endTransition.setVisibilityMode(visibilityMode);
                    for (Edge e2 : endTransition.getInEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    for (Edge e2 : endTransition.getOutEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    previouslyVisited.add(startPlace);
                    fwdChainNeighborVisibility(endTransition, visibilityMode, previouslyVisited);
                }
            }
        } else if (startVertex instanceof Transition) {
            Transition startTransition = (Transition) startVertex;
            Place endPlace;
            for (OutEdge edge : startTransition.getOutEdges()) {
                endPlace = (Place) edge.getEnd();
                if (previouslyVisited.contains(endPlace)) {
                    continue;
                } else if (endPlace.getFunctionMode() == FunctionMode.Recovery) {
                    edge.setVisibilityMode(visibilityMode);
                    endPlace.setVisibilityMode(visibilityMode);
                    for (Edge e2 : endPlace.getInEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    for (Edge e2 : endPlace.getOutEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    previouslyVisited.add(startTransition);
                    fwdChainNeighborVisibility(endPlace, visibilityMode, previouslyVisited);
                }
            }
        }
    }

    public void expandNeighborVisibility(Vertex vertex, GuiConfig.VisibilityMode visibilityMode, int degrees, ArrayList<Vertex> previouslyVisited) {
        if (degrees < 1) {
            return;
        }
        if (!previouslyVisited.contains(vertex)) {
            vertex.setVisibilityMode(visibilityMode);
        }
        if (vertex instanceof Place) {
            Place place = (Place) vertex;
            Transition neighborTransition;
            for (OutEdge edge : place.getInEdges()) {
                neighborTransition = edge.getStart();
                if (!previouslyVisited.contains(neighborTransition)) {
                    edge.setVisibilityMode(visibilityMode);
                    neighborTransition.setVisibilityMode(visibilityMode);
                    for (Edge e2 : neighborTransition.getInEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    for (Edge e2 : neighborTransition.getOutEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    if (degrees > 1) {
                        previouslyVisited.add(place);
                        expandNeighborVisibility(neighborTransition, visibilityMode, degrees - 1, previouslyVisited);
                    }
                }
            }
            for (InEdge edge : place.getOutEdges()) {
                neighborTransition = edge.getEnd();
                if (!previouslyVisited.contains(neighborTransition)) {
                    edge.setVisibilityMode(visibilityMode);
                    neighborTransition.setVisibilityMode(visibilityMode);
                    for (Edge e2 : neighborTransition.getInEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    for (Edge e2 : neighborTransition.getOutEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    if (degrees > 1) {
                        previouslyVisited.add(place);
                        expandNeighborVisibility(neighborTransition, visibilityMode, degrees - 1, previouslyVisited);
                    }
                }
            }
        } else if (vertex instanceof Transition) {
            Transition transition = (Transition) vertex;
            Place neighborPlace;
            for (InEdge edge : transition.getInEdges()) {
                neighborPlace = edge.getStart();
                if (!previouslyVisited.contains(neighborPlace)) {
                    edge.setVisibilityMode(visibilityMode);
                    neighborPlace.setVisibilityMode(visibilityMode);
                    for (Edge e2 : neighborPlace.getInEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    for (Edge e2 : neighborPlace.getOutEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    if (degrees > 1) {
                        previouslyVisited.add(transition);
                        expandNeighborVisibility(neighborPlace, visibilityMode, degrees - 1, previouslyVisited);
                    }
                }
            }
            for (OutEdge edge : transition.getOutEdges()) {
                neighborPlace = edge.getEnd();
                if (!previouslyVisited.contains(neighborPlace)) {
                    edge.setVisibilityMode(visibilityMode);
                    neighborPlace.setVisibilityMode(visibilityMode);
                    for (Edge e2 : neighborPlace.getInEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    for (Edge e2 : neighborPlace.getOutEdges()) {
                        e2.setVisibilityMode(visibilityMode);
                    }
                    if (degrees > 1) {
                        previouslyVisited.add(transition);
                        expandNeighborVisibility(neighborPlace, visibilityMode, degrees - 1, previouslyVisited);
                    }
                }
            }
        }
    }

    private class MyMouseListener implements MouseListener, MouseMotionListener, MouseWheelListener {

        final CrossoverScalingControl scaler = new CrossoverScalingControl();
        Vertex selectedVertex = null, dragVertex = null, edgeStartVertex = null;
        boolean amDraggingVertex = false, amCreatingEdge = false, amTranslating = false;
        Point2D prevMousePoint = null;
        double translationX = 0, translationY = 0, zoom = 1;
        TaskModelEditor editor;

        public void startVertexSelected(Vertex vertex) {
            switch (editorMode) {
                case Nominal:
                    if (vertex.getFunctionMode() == FunctionMode.Nominal) {
                        amCreatingEdge = true;
                        edgeStartVertex = vertex;
                        edgeStartVertex.setBeingModified(true);
                        refreshGraphVisibility();
                    }
                    break;
                case Recovery:
                    if (vertex != expandedNomVertex) {
                        // Expand this Nominal Vertex
                        expandedNomVertex = vertex;
                        refreshGraphVisibility();
                    } else if (vertex == expandedNomVertex) {
                        amCreatingEdge = true;
                        edgeStartVertex = vertex;
                        edgeStartVertex.setBeingModified(true);
                        refreshGraphVisibility();
                    }
                    break;
            }
            vv.repaint();
        }

        public void endVertexSelected(Vertex vertex) {
            if (graph.findEdge(edgeStartVertex, vertex) != null) {
                // Edge already exists
                amCreatingEdge = false;
                edgeStartVertex.setBeingModified(false);
                edgeStartVertex = null;
                vv.repaint();
                return;
            }

            if (edgeStartVertex instanceof Transition && vertex instanceof Place) {
                // An edge from this place->transition or transition-> does not exist
                Transition startTransition = (Transition) edgeStartVertex;
                Place endPlace = (Place) vertex;
                OutEdge newEdge = new OutEdge(startTransition, endPlace, editorMode);
                startTransition.addOutEdge(newEdge);
                endPlace.addInEdge(newEdge);
                graph.addEdge(newEdge, startTransition, endPlace);

                startTransition.addOutPlace(endPlace);
                endPlace.addInTransition(startTransition);
            } else if (edgeStartVertex instanceof Place && vertex instanceof Transition) {
                // An edge from this place->transition or transition-> does not exist
                Place startPlace = (Place) edgeStartVertex;
                Transition endTransition = (Transition) vertex;
                InEdge newEdge = new InEdge(startPlace, endTransition, editorMode);
                startPlace.addOutEdge(newEdge);
                endTransition.addInEdge(newEdge);
                graph.addEdge(newEdge, startPlace, endTransition);

                startPlace.addOutTransition(endTransition);
                endTransition.addInPlace(startPlace);
            } else if (edgeStartVertex instanceof Place && vertex instanceof Place) {
                // Trying to create an edge between two places, add an intermittent transition as well
                Place startPlace = (Place) edgeStartVertex;
                Place endPlace = (Place) vertex;
                Transition newTransition = new Transition("", editorMode);
                graph.addVertex(newTransition);
                Point freePoint = getVertexFreePoint(
                        (int) ((layout.getX(startPlace) + layout.getX(endPlace)) / 2),
                        (int) ((layout.getY(startPlace) + layout.getY(endPlace)) / 2),
                        CLICK_RADIUS);
                layout.setLocation(newTransition, snapToGrid(freePoint));

                InEdge newEdge1 = new InEdge(startPlace, newTransition, editorMode);
                startPlace.addOutEdge(newEdge1);
                newTransition.addInEdge(newEdge1);
                graph.addEdge(newEdge1, startPlace, newTransition);

                OutEdge newEdge2 = new OutEdge(newTransition, endPlace, editorMode);
                newTransition.addOutEdge(newEdge2);
                endPlace.addInEdge(newEdge2);
                graph.addEdge(newEdge2, newTransition, endPlace);

                startPlace.addOutTransition(newTransition);
                newTransition.addInPlace(startPlace);
                newTransition.addOutPlace(endPlace);
                endPlace.addInTransition(newTransition);
            } else if (edgeStartVertex instanceof Transition && vertex instanceof Transition) {
                // Trying to create an edge between two transitions, add an intermittent place as well
                Transition startTransition = (Transition) edgeStartVertex;
                Transition endTransition = (Transition) vertex;
                Place newPlace = new Place("", editorMode);
                graph.addVertex(newPlace);
                Point freePoint = getVertexFreePoint(
                        (int) ((layout.getX(startTransition) + layout.getX(endTransition)) / 2),
                        (int) ((layout.getY(startTransition) + layout.getY(endTransition)) / 2),
                        CLICK_RADIUS);
                layout.setLocation(newPlace, snapToGrid(freePoint));

                OutEdge newEdge1 = new OutEdge(startTransition, newPlace, editorMode);
                startTransition.addOutEdge(newEdge1);
                newPlace.addInEdge(newEdge1);
                graph.addEdge(newEdge1, startTransition, newPlace);

                InEdge newEdge2 = new InEdge(newPlace, endTransition, editorMode);
                newPlace.addOutEdge(newEdge2);
                endTransition.addInEdge(newEdge2);
                graph.addEdge(newEdge2, newPlace, endTransition);

                startTransition.addOutPlace(newPlace);
                newPlace.addInTransition(startTransition);
                newPlace.addOutTransition(endTransition);
                endTransition.addInPlace(newPlace);
            }
            amCreatingEdge = false;
            edgeStartVertex.setBeingModified(false);
            edgeStartVertex = null;
            refreshGraphVisibility();
            vv.repaint();
        }

        @Override
        public void mouseClicked(MouseEvent me) {
//            System.out.println("Clicked " + me.getButton());
        }

        @Override
        public void mousePressed(MouseEvent me) {
//            System.out.println("Pressed " + me.getButton() + "\t" + mouse1 + "\t" + mouse2 + "\t" + mouse3);
//            System.out.println("\t" + me.getModifiersEx() + "\t" + (me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)));

            final Point2D framePoint = me.getPoint();
            if (((me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) == (MouseEvent.BUTTON1_DOWN_MASK))
                    && !me.isShiftDown()) {
                // Mouse1
                // Select a vertex (needs to be in mousePressed to begin dragging)
                GraphElementAccessor<Vertex, Edge> pickSupport = vv.getPickSupport();
                if (pickSupport != null) {
                    final Vertex vertex = getNearestVertex(framePoint.getX(), framePoint.getY(), CLICK_RADIUS);
                    final Edge edge = getNearestEdge(framePoint.getX(), framePoint.getY(), CLICK_RADIUS);
                    if (vertex != null) {
                        if (!amCreatingEdge && !amDraggingVertex && selectedVertex == null) {
                            selectedVertex = vertex;
                        }
                    }
                }
            } else if (((me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) == MouseEvent.BUTTON2_DOWN_MASK)
                    || ((me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) == MouseEvent.BUTTON1_DOWN_MASK && me.isShiftDown())
                    || (me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) == (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) {
                // Mouse 2 OR Mouse1+Shift OR Mouse1+Mouse3
                // Begin translating
                amTranslating = true;
                prevMousePoint = (Point2D) framePoint.clone();
            } else if (me.getButton() == MouseEvent.BUTTON3) {
            }
        }

        @Override
        public void mouseDragged(MouseEvent me) {
//            System.out.println("Dragged " + me.getButton());

            final Point2D framePoint = me.getPoint();
            final Point2D graphPoint = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(framePoint);

            if (!amDraggingVertex && selectedVertex != null) {
                dragVertex = selectedVertex;
                amDraggingVertex = true;
            } else if (amDraggingVertex && dragVertex != null) {
                layout.setLocation(dragVertex, snapToGrid(graphPoint));
                vv.repaint();
            } else if (amTranslating && prevMousePoint != null) {
                // Translate frame
                // The Render transform doesn't update very quickly, so do it ourselves so translation looks smooth
                MutableTransformer layout = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
                double scale = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).getScale();
                double deltaX = (framePoint.getX() - prevMousePoint.getX()) * 1 / scale;
                double deltaY = (framePoint.getY() - prevMousePoint.getY()) * 1 / scale;
                layout.translate(deltaX, deltaY);
                prevMousePoint = framePoint;
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
//            System.out.println("Released " + me.getButton() + "\t" + mouse1 + "\t" + mouse2 + "\t" + mouse3);

            final Point framePoint = me.getPoint();
            final Point2D graphPoint = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(framePoint);
            if (me.getButton() == MouseEvent.BUTTON1
                    && (me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) == 0
                    && !amDraggingVertex) {
                GraphElementAccessor<Vertex, Edge> pickSupport = vv.getPickSupport();
                if (pickSupport != null) {
                    final Vertex vertex = getNearestVertex(framePoint.getX(), framePoint.getY(), CLICK_RADIUS);
                    if (vertex != null) {
                        if (!amCreatingEdge && edgeStartVertex == null) {
                            // Set start point for new edge
                            startVertexSelected(vertex);
                        } else if (amCreatingEdge && edgeStartVertex != null) {
                            // Set start point for new edge
                            endVertexSelected(vertex);
                        }
                    } else if (vertex == null && amCreatingEdge) {
                        // Invalid end point, cancel edge creation
                        amCreatingEdge = false;
                        edgeStartVertex.setBeingModified(false);
                        edgeStartVertex = null;
                        //System.out.println("Transitions to nowhere not supported");
                        vv.repaint();
                    }
                }
            } else if (me.getButton() == MouseEvent.BUTTON3
                    && (me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) == 0
                    && !amTranslating) {
                GraphElementAccessor<Vertex, Edge> pickSupport = vv.getPickSupport();
                if (pickSupport != null) {
                    final Vertex vertex = getNearestVertex(framePoint.getX(), framePoint.getY(), CLICK_RADIUS);
                    final Edge edge = getNearestEdge(framePoint.getX(), framePoint.getY(), CLICK_RADIUS);
                    if (vertex != null) {
                        // Right click place or transition -> show options
                        JPopupMenu popup = new JPopupMenu();
                        popup.add(new AbstractAction("Rename") {
                            public void actionPerformed(ActionEvent e) {
                                String name = JOptionPane.showInputDialog(vv, "Vertex name", vertex.getName());
                                if (name == null) {
                                    name = "";
                                }
                                vertex.setName(name);
                                //@needed?
                                edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer vertexLabelRenderer = (edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer) vv.getRenderContext().getVertexLabelRenderer();
                                vertexLabelRenderer.repaint();
                                vv.repaint();
                            }
                        });
                        popup.add(new AbstractAction("Edit Events") {
                            public void actionPerformed(ActionEvent e) {
                                // Write things out to make sure that we have variables.
                                writeModel();
//                                System.out.println("b spec list: " + mSpec.getEventSpecList(vertex));
                                SelectEventD diag = new SelectEventD(null, true, mSpec.getEventSpecList(vertex), mSpec.getAllVariables(), vertex instanceof Transition, vertex instanceof Place);

                                // Remove event specs that were mapped to this transition
                                // @todo Probably should edit events instead of clearing and adding
                                mSpec.clearEventSpecList(vertex);

                                diag.setVisible(true);
                                // This part won't run until the Frame closes
                                ArrayList<ReflectedEventSpecification> selectedEventSpecs = diag.getSelectedEventSpecs();
                                if (selectedEventSpecs != null) {
                                    // Have to have the null check in case Ubuntu screws up the JFrame and I have to "x" close the window...
                                    for (ReflectedEventSpecification eventSpec : selectedEventSpecs) {
                                        // Add the selected events to our maps
                                        mSpec.updateEventSpecList(vertex, eventSpec);
                                    }
                                    vertex.setEventSpecs(selectedEventSpecs);
                                }
                                vv.repaint();
//                                System.out.println("a spec list: " + mSpec.getEventSpecList(vertex));
                            }
                        });
                        popup.add(new AbstractAction("Edit Markups") {
                            public void actionPerformed(ActionEvent e) {
                                // Write things out to make sure that we have variables.
                                writeModel();
                                SelectEventForMarkupD eventDiag = new SelectEventForMarkupD(null, true, vertex.getEventSpecs());
                                eventDiag.setVisible(true);
                                // This part won't run until the Frame closes
                                if (eventDiag.selectedEventSpecs == null) {
                                    // If no event was selected to edit, return
                                    return;
                                }

                                for (ReflectedEventSpecification eventSpec : eventDiag.selectedEventSpecs) {
                                    SelectMarkupD markupDiag = new SelectMarkupD(null, true, eventSpec.getMarkupSpecs(), mSpec.getAllVariables());
                                    markupDiag.setVisible(true);
//                                    // This part won't run until the Frame closes
                                    eventSpec.setMarkupSpecs(markupDiag.getSelectedMarkupSpecs());
                                    vertex.updateTag();
                                    vv.repaint();
                                }
                            }
                        });
                        popup.add(new AbstractAction("Delete") {
                            public void actionPerformed(ActionEvent e) {
                                boolean reallyRemove = true;
                                // @todo Consider linking requirements to objects to make the deletion process cleaner
                                // @todo GetFilledBy only returns one object, if multiple fulfill, not listed
                                // @todo Cancel/No not handled smoothly when multiple requirements filled by this.
                                if (mediator.getReqs() != null) {
                                    for (RequirementSpecification requirementSpecification : mediator.getReqs()) {
                                        if (requirementSpecification.getFilledBy() == vertex) {
                                            int ret = JOptionPane.showConfirmDialog(null, "Requirement: " + requirementSpecification + " is filled by this object, really delete?");
                                            if (ret == JOptionPane.NO_OPTION || ret == JOptionPane.CANCEL_OPTION) {
                                                reallyRemove = false;
                                            } else {
                                                requirementSpecification.setFilled(false);
                                                requirementSpecification.setFilledBy(null);
                                            }
                                        } else {
                                            System.out.println("Requirement " + requirementSpecification + " not impacted by " + vertex + " " + requirementSpecification.getFilledBy());
                                        }
                                    }
                                }
                                if (reallyRemove) {
                                    if (vertex instanceof Place) {
                                        removePlace((Place) vertex);
                                    } else if (vertex instanceof Transition) {
                                        removeTransition((Transition) vertex);
                                    }
                                }
                            }
                        });
                        if (vertex instanceof Place) {
                            final Place place = (Place) vertex;
                            popup.add(new AbstractAction("Add Sub-mission") {
                                public void actionPerformed(ActionEvent e) {
                                    SubMissionD d = new SubMissionD(null, true);
                                    d.setVisible(true);
                                    if (d.getSelectedMission() != null) {
                                        MissionPlanSpecification submissionSpec = ((MissionPlanSpecification) d.getSelectedMission()).getSubmissionInstance(mSpec, d.namePrefix, d.variablePrefix);
                                        dreaam.addMissionSpec(submissionSpec);

                                        place.setSubMission(submissionSpec);
                                    }
                                    vv.repaint();
                                }
                            });
                            if (place.isStart()) {
                                popup.add(new AbstractAction("Unset start") {
                                    public void actionPerformed(ActionEvent e) {
                                        place.setIsStart(false);
                                        vv.repaint();
                                    }
                                });
                            } else {
                                popup.add(new AbstractAction("Set start") {
                                    public void actionPerformed(ActionEvent e) {
                                        place.setIsStart(true);
                                        vv.repaint();
                                    }
                                });
                            }
                            if (place.isEnd()) {
                                popup.add(new AbstractAction("Unset end") {
                                    public void actionPerformed(ActionEvent e) {
                                        place.setIsEnd(false);
                                        vv.repaint();
                                    }
                                });
                            } else {
                                popup.add(new AbstractAction("Set end") {
                                    public void actionPerformed(ActionEvent e) {
                                        place.setIsEnd(true);
                                        if (place.getOutEdges().size() > 0) {
                                            JOptionPane.showMessageDialog(vv, "An end state has output transitions, these will never be used");
                                        }
                                        vv.repaint();
                                    }
                                });
                            }
                        } else if (vertex instanceof Transition) {
                            popup.add(new AbstractAction("Requirements") {
                                @Override
                                public void actionPerformed(ActionEvent ae) {
                                    SelectReqD d = new SelectReqD(null, true, edge);
                                    d.setVisible(true);
                                }
                            });
                        }
                        popup.show(vv, me.getX(), me.getY());
                    } else if (edge != null) {
                        JPopupMenu popup = new JPopupMenu();
                        popup.add(new AbstractAction("Edit Tokens") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                boolean isIncomingEdge = (edge.getStart() instanceof Place && edge.getEnd() instanceof Transition);
                                boolean isNominal = edge.getFunctionMode() == FunctionMode.Nominal;
                                EdgeType edgeType = null;
                                if (isIncomingEdge && isNominal) {
                                    edgeType = SelectTokenD.EdgeType.IncomingNominal;
                                } else if (isIncomingEdge && !isNominal) {
                                    edgeType = SelectTokenD.EdgeType.IncomingRecovery;
                                } else if (!isIncomingEdge && isNominal) {
                                    edgeType = SelectTokenD.EdgeType.OutgoingNominal;
                                } else if (!isIncomingEdge && !isNominal) {
                                    edgeType = SelectTokenD.EdgeType.OutgoingRecovery;
                                }
                                SelectTokenD tokenD = new SelectTokenD(null, true, edgeType, edge.getTokenRequirements(), mSpec.getTaskSpecList());
                                edge.clearTokenRequirements();
                                tokenD.setVisible(true);
                                // This won't run until the Frame closes
                                for (TokenRequirement tokenReq : tokenD.getSelectedTokenReqs()) {
                                    if (edge instanceof InEdge && tokenReq instanceof InTokenRequirement) {
                                        ((InEdge) edge).addTokenRequirement((InTokenRequirement) tokenReq);
                                    } else if (edge instanceof OutEdge && tokenReq instanceof OutTokenRequirement) {
                                        ((OutEdge) edge).addTokenRequirement((OutTokenRequirement) tokenReq);
                                    }
                                }
                                vv.repaint();
                            }
                        });
                        popup.add(new AbstractAction("Delete") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                removeEdge(edge);
                            }
                        });
                        popup.show(vv, me.getX(), me.getY());
                    } else {
                        // Right click in empty space -> Create a new place or transition
                        JPopupMenu popup = new JPopupMenu();
                        popup.add(new AbstractAction("New Place") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                //System.out.println("Adding place");
                                Place place = new Place("", editorMode);
                                graph.addVertex(place);
                                layout.setLocation(place, snapToGrid(graphPoint));
                                vv.repaint();
                            }
                        });
                        popup.add(new AbstractAction("New Transition") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                //System.out.println("Adding transition");
                                Transition transition = new Transition("", editorMode);
                                graph.addVertex(transition);
                                layout.setLocation(transition, snapToGrid(graphPoint));
                                vv.repaint();
                            }
                        });
                        popup.show(vv, me.getX(), me.getY());
                    }
                }
            }

            if ((me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) == 0) {
                selectedVertex = null;
                dragVertex = null;
                amDraggingVertex = false;
                amTranslating = false;
                prevMousePoint = null;
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getWheelRotation() < 0) {
                // Zoom in
                scaler.scale(vv, 1.1f, e.getPoint());
                zoom *= 1.1;
            } else if (e.getWheelRotation() > 0) {
                // Zoom out
                scaler.scale(vv, 1 / 1.1f, e.getPoint());
                zoom /= 1.1;
            }
        }

        @Override
        public void mouseMoved(MouseEvent me) {
            // System.out.println("Mouse moved");
        }

        @Override
        public void mouseEntered(MouseEvent me) {
            //System.out.println("Entered");
        }

        @Override
        public void mouseExited(MouseEvent me) {
            //System.out.println("Exited");
        }

        public Point2D snapToGrid(Point2D point) {
            Point2D.Double gridPoint = new Point2D.Double((int) (point.getX() / GRID_LENGTH + 0.5) * GRID_LENGTH, (int) (point.getY() / GRID_LENGTH + 0.5) * GRID_LENGTH);
            return gridPoint;
        }

        public Point getVertexFreePoint(double x, double y, double searchRadius) {
            Point freePoint = new Point((int) x, (int) y);
            while (getNearestVertex(freePoint.getX(), freePoint.getY(), searchRadius) != null) {
                freePoint.setLocation(freePoint.getX() - (2 * searchRadius + 1), freePoint.getY() - (2 * searchRadius + 1));
            }
            return freePoint;
        }

        public Vertex getNearestVertex(double x, double y, double radius) {
            return getNearestVertex(x, y, radius, true);
        }

        public Vertex getNearestVertex(double x, double y, double radius, boolean vertexVisible) {
            GraphElementAccessor<Vertex, Edge> pickSupport = vv.getPickSupport();
            Vertex vertex = pickSupport.getVertex(vv.getGraphLayout(), x, y);
            for (int r = 1; r <= radius && vertex == null; r++) {
                for (int dir = 0; dir < 8 && vertex == null; dir++) {
                    switch (dir) {
                        case (0): // N
                            vertex = pickSupport.getVertex(vv.getGraphLayout(), x, y - r);
                            break;
                        case (1): // NE
                            vertex = pickSupport.getVertex(vv.getGraphLayout(), x + r, y - r);
                            break;
                        case (2): // E
                            vertex = pickSupport.getVertex(vv.getGraphLayout(), x + r, y);
                            break;
                        case (3): // SE
                            vertex = pickSupport.getVertex(vv.getGraphLayout(), x + r, y + r);
                            break;
                        case (4): // S
                            vertex = pickSupport.getVertex(vv.getGraphLayout(), x, y + r);
                            break;
                        case (5): // SW
                            vertex = pickSupport.getVertex(vv.getGraphLayout(), x - r, y + r);
                            break;
                        case (6): // W
                            vertex = pickSupport.getVertex(vv.getGraphLayout(), x - r, y);
                            break;
                        case (7): // NW
                            vertex = pickSupport.getVertex(vv.getGraphLayout(), x - r, y - r);
                            break;
                    }
                    if (vertexVisible && vertex != null && vertex.getVisibilityMode() == GuiConfig.VisibilityMode.None) {
                        vertex = null;
                    }
                }
            }
            return vertex;
        }

        public Edge getNearestEdge(double x, double y, int radius) {
            return getNearestEdge(x, y, radius, true);
        }

        public Edge getNearestEdge(double x, double y, int radius, boolean edgeVisible) {
            GraphElementAccessor<Vertex, Edge> pickSupport = vv.getPickSupport();
            Edge edge = pickSupport.getEdge(vv.getGraphLayout(), x, y);
            for (int r = 1; r <= radius && edge == null; r++) {
                for (int dir = 0; dir < 8 && edge == null; dir++) {
                    switch (dir) {
                        case (0): // N
                            edge = pickSupport.getEdge(vv.getGraphLayout(), x, y - r);
                            break;
                        case (1): // NE
                            edge = pickSupport.getEdge(vv.getGraphLayout(), x + r, y - r);
                            break;
                        case (2): // E
                            edge = pickSupport.getEdge(vv.getGraphLayout(), x + r, y);
                            break;
                        case (3): // SE
                            edge = pickSupport.getEdge(vv.getGraphLayout(), x + r, y + r);
                            break;
                        case (4): // S
                            edge = pickSupport.getEdge(vv.getGraphLayout(), x, y + r);
                            break;
                        case (5): // SW
                            edge = pickSupport.getEdge(vv.getGraphLayout(), x - r, y + r);
                            break;
                        case (6): // W
                            edge = pickSupport.getEdge(vv.getGraphLayout(), x - r, y);
                            break;
                        case (7): // NW
                            edge = pickSupport.getEdge(vv.getGraphLayout(), x - r, y - r);
                            break;
                    }
                    if (edgeVisible && edge != null && edge.getVisibilityMode() == GuiConfig.VisibilityMode.None) {
                        edge = null;
                    }
                }
            }
            return edge;
        }
    }

    public static Point snapToGrid(Point point) {
        Point gridPoint = new Point((int) (point.x / GRID_LENGTH + 0.5) * GRID_LENGTH, (int) (point.y / GRID_LENGTH + 0.5) * GRID_LENGTH);
        return gridPoint;
    }

    public void addTemplate(MissionPlanSpecification spec) {
        throw new NotImplementedException();
//
//        Graph<Vertex, Edge> g = spec.getGraph();
//        if (g != graph && g != null) {
//            HashMap<Vertex, Vertex> vertexMap = new HashMap<Vertex, Vertex>();
//            for (Vertex vertex : g.getVertices()) {
//                if (vertex instanceof Place) {
//                    Place place = (Place) vertex;
//                    Place placeCopy = place.copyWithoutConnections();
//                    vertexMap.put(vertex, placeCopy);
//                    graph.addVertex(placeCopy);
//                    layout.setLocation(placeCopy, spec.getLocations().get(vertex));
//                } else if (vertex instanceof Transition) {
//                    Transition transition = (Transition) vertex;
//                    Transition transitionCopy = transition.copyWithoutConnections();
//                    vertexMap.put(vertex, transitionCopy);
//                    graph.addVertex(transitionCopy);
//                    layout.setLocation(transitionCopy, spec.getLocations().get(vertex));
//                } else {
//                    LOGGER.severe("Vertex is not an instance of Place OR Transition! " + vertex);
//                }
//            }
//
//            for (Edge edge : g.getEdges()) {
//                Edge edgeCopy = edge.copy(vertexMap);
//                graph.addEdge(edgeCopy, edgeCopy.getStart(), edgeCopy.getEnd());
//            }
//            vv.repaint();
//            // System.out.println("Template added");
//        } else {
//            System.out.println("Can't add to self");
//        }
    }

    public void setGraph(MissionPlanSpecification spec) {
        this.mSpec = spec;

        if (spec.getGraph() != null) {
            spec.updateAllTags();

            // this.graph = (DirectedSparseGraph)spec.getGraph();
            SparseMultigraph uGraph = (SparseMultigraph) spec.getGraph();
            this.graph = new DirectedSparseGraph<Vertex, Edge>();
            for (Object o : uGraph.getVertices()) {
                graph.addVertex((Vertex) o);
            }
            for (Object o : uGraph.getEdges()) {
                Edge edge = (Edge) o;
                graph.addEdge(edge, edge.getStart(), edge.getEnd());
            }

            MultiLayerTransformer mlt = vv.getRenderContext().getMultiLayerTransformer();
            mlt.getTransformer(Layer.LAYOUT).getTransform().setTransform(spec.getLayoutTransform());
            mlt.getTransformer(Layer.VIEW).getTransform().setTransform(spec.getView());

            layout.setGraph(graph);

            spec.updateThisLayout(layout);    // @help Why are we doing this?

            vv.repaint();
//            System.out.println("Size " + layout.getSize());
        } else {
            this.graph = new DirectedSparseGraph<Vertex, Edge>();
            // this.graph = new SparseMultigraph<Place, Transition>();
            layout.setGraph(this.graph);
            vv.repaint();
        }
    }

    public void reloadGraph() {
        setGraph(mSpec);
    }

    /*
     * Tells TaskModelEditor to write out the graph to the
     * MissionPlanSpecification
     */
    public void writeModel() {
        if (mSpec != null) {

            // Translate to SparseMultigraph because DirectedSparseGraph is not serializable
            SparseMultigraph<Vertex, Edge> uGraph = new SparseMultigraph<Vertex, Edge>();
            for (Vertex o : graph.getVertices()) {
                uGraph.addVertex(o);
            }
            for (Edge o : graph.getEdges()) {
                uGraph.addEdge(o, o.getStart(), o.getEnd());
            }

            mSpec.setGraph(uGraph, layout);

            mSpec.setLayout(vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).getTransform());
            mSpec.setView(vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).getTransform());
        } else {
            LOGGER.warning("MissionSpecification is null, not writing graph");
        }
    }

    public void removePlace(Place place) {
        // First remove the transition and its edges from our copy of the graph so we don't have to reload the whole thing
        ArrayList<OutEdge> inEdges = (ArrayList<OutEdge>) place.getInEdges();
        for (OutEdge inEdge : inEdges) {
            graph.removeEdge(inEdge);
        }
        ArrayList<InEdge> outEdges = (ArrayList<InEdge>) place.getOutEdges();
        for (InEdge outEdge : outEdges) {
            graph.removeEdge(outEdge);
        }
        // Now we can remove the vertex
        graph.removeVertex(place);

        // Now we can remove the transition and its edges from the mission spec and then the actual data structures
        mSpec.removePlace(place);

        vv.repaint();
    }

    public void removeTransition(Transition transition) {
        // First remove the transition and its edges from our copy of the graph so we don't have to reload the whole thing
        ArrayList<InEdge> inEdges = (ArrayList<InEdge>) transition.getInEdges();
        for (InEdge inEdge : inEdges) {
            graph.removeEdge(inEdge);
        }
        ArrayList<OutEdge> outEdges = (ArrayList<OutEdge>) transition.getOutEdges();
        for (OutEdge outEdge : outEdges) {
            graph.removeEdge(outEdge);
        }
        // Now we can remove the vertex
        graph.removeVertex(transition);

        // Now we can remove the transition and its edges from the mission spec and then the actual data structures
        mSpec.removeTransition(transition);

        vv.repaint();
    }

    public void removeEdge(Edge edge) {
        // First remove the edge from our copy of the graph so we don't have to reload the whole thing
        graph.removeEdge(edge);

        // Now we can remove the edge from the mission spec and then the actual data structure
        mSpec.removeEdge(edge);

        vv.repaint();
    }
}
