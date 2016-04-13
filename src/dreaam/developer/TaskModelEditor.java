package dreaam.developer;

import sami.DreaamHelper;
import dreaam.developer.SelectTokenD.EdgeType;
import dreaam.wizard.EventWizardSingleton;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.MultiLayerTransformer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractModalGraphMouse;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.TranslatingGraphMousePlugin;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.apache.commons.collections15.Transformer;
import sami.engine.Mediator;
import sami.event.ReflectedEventSpecification;
import sami.gui.GuiConfig;
import sami.mission.Edge;
import sami.mission.InEdge;
import sami.mission.InTokenRequirement;
import sami.mission.MissionPlanSpecification;
import sami.mission.MockupInEdge;
import sami.mission.MockupOutEdge;
import sami.mission.MockupPlace;
import sami.mission.MockupTransition;
import sami.mission.OutEdge;
import sami.mission.OutTokenRequirement;
import sami.mission.Place;
import sami.mission.RequirementSpecification;
import sami.mission.TokenRequirement;
import sami.mission.Transition;
import sami.mission.Vertex;
import sami.mission.Vertex.FunctionMode;

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
    // We edit plans using a DirectedSparseGraph so the directional arrows on the edges are rendered
    //  However, DirectedSparseGraph can not be serialized because one of its fields does not have a no-arg constructor
    //  So we call updateSerializableModel() when we want to write all changes to the mSpec's serializable SparseMultigraph graph
    Graph<Vertex, Edge> transientGraph;
    AbstractLayout<Vertex, Edge> layout;
    MissionPlanSpecification mSpec = null;
    Mediator mediator = Mediator.getInstance();
    VisualizationViewer<Vertex, Edge> vv;   // The visual component and renderer for the graph
    String instructions = "Guess";
    AbstractModalGraphMouse graphMouse;
    private FunctionMode editorMode = FunctionMode.Nominal;
    private Vertex expandedNomVertex = null;
    private DREAAM dreaam;
    TaskModelEditor.MyMouseListener mml;
    final ScalingControl scaler;

    GraphZoomScrollPane panel;
    JPanel controls;

    public TaskModelEditor(DREAAM dreaam, MissionPlanSpecification newSpec) {
        this(newSpec);
        this.dreaam = dreaam;
    }

    private void addVisualizationTransformers() {
        // Visualization settings
        vv.setBackground(GuiConfig.BACKGROUND_COLOR);

        // EDGE
        vv.getRenderContext().setEdgeArrowStrokeTransformer(new Transformer<Edge, Stroke>() {
            @Override
            public Stroke transform(Edge edge) {
                switch (edge.getVisibilityMode()) {
                    case Full:
                        if (edge instanceof MockupInEdge && ((MockupInEdge) edge).getIsHighlighted()) {
                            return GuiConfig.NOMINAL_STROKE_SEL;
                        } else if (edge instanceof MockupOutEdge && ((MockupOutEdge) edge).getIsHighlighted()) {
                            return GuiConfig.NOMINAL_STROKE_SEL;
                        }
                        if (edge.getFunctionMode() == Vertex.FunctionMode.Recovery) {
                            return GuiConfig.RECOVERY_STROKE;
                        } else {
                            return GuiConfig.NOMINAL_STROKE;
                        }
                    case Background:
                    case None:
                    default:
                        return null;
                }
            }
        });

        vv.getRenderContext().setArrowDrawPaintTransformer(new Transformer<Edge, Paint>() {
            public Paint transform(Edge edge) {
                switch (edge.getVisibilityMode()) {
                    case Full:
                        if (edge instanceof MockupInEdge && ((MockupInEdge) edge).getIsHighlighted()) {
                            return GuiConfig.SEL_EDGE_COLOR;
                        } else if (edge instanceof MockupOutEdge && ((MockupOutEdge) edge).getIsHighlighted()) {
                            return GuiConfig.SEL_EDGE_COLOR;
                        }
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
                        if (edge instanceof MockupInEdge && ((MockupInEdge) edge).getIsHighlighted()) {
                            return GuiConfig.SEL_EDGE_COLOR;
                        } else if (edge instanceof MockupOutEdge && ((MockupOutEdge) edge).getIsHighlighted()) {
                            return GuiConfig.SEL_EDGE_COLOR;
                        }
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
                        if (edge instanceof MockupInEdge && ((MockupInEdge) edge).getIsHighlighted()) {
                            return GuiConfig.SEL_EDGE_COLOR;
                        } else if (edge instanceof MockupOutEdge && ((MockupOutEdge) edge).getIsHighlighted()) {
                            return GuiConfig.SEL_EDGE_COLOR;
                        }
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
                        return GuiConfig.TEXT_FONT;
                    case None:
                    default:
                        return null;
                }
            }
        });

        vv.getRenderContext().setLabelOffset(GuiConfig.LABEL_OFFSET);
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
                        if (edge instanceof MockupInEdge && ((MockupInEdge) edge).getIsHighlighted()) {
                            return GuiConfig.NOMINAL_STROKE_SEL;
                        } else if (edge instanceof MockupOutEdge && ((MockupOutEdge) edge).getIsHighlighted()) {
                            return GuiConfig.NOMINAL_STROKE_SEL;
                        }
                        if (edge.getFunctionMode() == Vertex.FunctionMode.Recovery) {
                            return GuiConfig.RECOVERY_STROKE;
                        } else {
                            return GuiConfig.NOMINAL_STROKE;
                        }
                    case Background:
                    case None:
                    default:
                        return null;
                }
            }
        });

        // VERTEX
        vv.getRenderContext().setVertexDrawPaintTransformer(new Transformer<Vertex, Paint>() {
            @Override
            public Paint transform(Vertex vertex) {
                switch (vertex.getVisibilityMode()) {
                    case Full:
                        if (vertex instanceof MockupPlace && ((MockupPlace) vertex).getIsHighlighted()) {
                            return GuiConfig.SEL_VERTEX_COLOR;
                        } else if (vertex instanceof MockupTransition && ((MockupTransition) vertex).getIsHighlighted()) {
                            return GuiConfig.SEL_VERTEX_COLOR;
                        }
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

        vv.getRenderContext().setVertexFillPaintTransformer(new Transformer<Vertex, Paint>() {
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

        vv.getRenderContext().setVertexFontTransformer(new Transformer<Vertex, Font>() {
            @Override
            public Font transform(Vertex vertex) {
                switch (vertex.getVisibilityMode()) {
                    case Full:
                    case Background:
                        return GuiConfig.TEXT_FONT;
                    case None:
                    default:
                        return null;
                }
            }
        });

        vv.getRenderContext().setVertexLabelTransformer(new Transformer<Vertex, String>() {
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

        vv.getRenderContext().setVertexShapeTransformer(new Transformer<Vertex, Shape>() {
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
                        if (vertex instanceof MockupPlace && ((MockupPlace) vertex).getIsHighlighted()) {
                            return GuiConfig.NOMINAL_STROKE_SEL;
                        } else if (vertex instanceof MockupTransition && ((MockupTransition) vertex).getIsHighlighted()) {
                            return GuiConfig.NOMINAL_STROKE_SEL;
                        }
                        if (vertex.getBeingModified()) {
                            if (vertex.getFunctionMode() == FunctionMode.Recovery) {
                                return GuiConfig.RECOVERY_STROKE_SEL;
                            } else {
                                return GuiConfig.NOMINAL_STROKE_SEL;
                            }
                        } else if (vertex.getFunctionMode() == FunctionMode.Recovery) {
                            return GuiConfig.RECOVERY_STROKE;
                        } else {
                            return GuiConfig.NOMINAL_STROKE;
                        }
                    case Background:
                        if (vertex.getBeingModified()) {
                            if (vertex.getFunctionMode() == FunctionMode.Recovery) {
                                return GuiConfig.RECOVERY_STROKE_SEL;
                            } else {
                                return GuiConfig.NOMINAL_STROKE_SEL;
                            }
                        } else if (vertex.getFunctionMode() == FunctionMode.Recovery) {
                            return GuiConfig.RECOVERY_STROKE;
                        } else {
                            return GuiConfig.NOMINAL_STROKE;
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
    }

    public TaskModelEditor(MissionPlanSpecification newSpec) {
        // Create mission view panel
        transientGraph = newSpec.getTransientGraph();
        layout = new StaticLayout<Vertex, Edge>(transientGraph);
        vv = new VisualizationViewer<Vertex, Edge>(layout);
        vv.setBackground(GuiConfig.BACKGROUND_COLOR);
        scaler = new CrossoverScalingControl();

        // Comment this line in and the next four out to switch to my mouse handler
        mml = new TaskModelEditor.MyMouseListener();
        vv.addMouseListener(mml);
        vv.addMouseMotionListener(mml);

        addVisualizationTransformers();
        setMissionSpecification(newSpec);

        setLayout(new BorderLayout());
        panel = new GraphZoomScrollPane(vv);
        add(panel, BorderLayout.CENTER);

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
                EventWizardSingleton.getInstance().computeRequirements(mSpec);
            }
        });

        JButton snapView = new JButton("Snap View");
        snapView.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                snapViewToVisible();
                vv.repaint();
            }
        });

        graphMouse = new DefaultModalGraphMouse() {
            protected void loadPlugins() {
                translatingPlugin = new TranslatingGraphMousePlugin(InputEvent.SHIFT_MASK | InputEvent.BUTTON1_MASK);
                scalingPlugin = new ScalingGraphMousePlugin(scaler, 0);

                add(translatingPlugin);
                add(scalingPlugin);
                setMode(Mode.TRANSFORMING);
            }
        };
        vv.setGraphMouse(graphMouse);
        vv.addKeyListener(graphMouse.getModeKeyListener());

        controls = new JPanel();
        controls.add(plus);
        controls.add(minus);
        controls.add(help);
        controls.add(snapView);
        add(controls, BorderLayout.SOUTH);

    }

    MissionPlanSpecification getModel() {
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
                for (Vertex vertex : transientGraph.getVertices()) {
                    switch (vertex.getFunctionMode()) {
                        case Nominal:
                            vertex.setVisibilityMode(GuiConfig.VisibilityMode.Full);
                            break;
                        default:
                            vertex.setVisibilityMode(GuiConfig.VisibilityMode.None);
                    }
                }
                for (Edge edge : transientGraph.getEdges()) {
                    if (edge.getStart().getFunctionMode() == FunctionMode.Nominal && edge.getEnd().getFunctionMode() == FunctionMode.Nominal) {
                        edge.setVisibilityMode(GuiConfig.VisibilityMode.Full);
                    } else {
                        edge.setVisibilityMode(GuiConfig.VisibilityMode.None);
                    }
                }
                break;
            case Recovery:
                for (Vertex vertex : transientGraph.getVertices()) {
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
                for (Edge edge : transientGraph.getEdges()) {
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
            case All:
                for (Vertex vertex : transientGraph.getVertices()) {
                    switch (vertex.getFunctionMode()) {
                        case Mockup:
                            vertex.setVisibilityMode(GuiConfig.VisibilityMode.None);
                            break;
                        default:
                            vertex.setVisibilityMode(GuiConfig.VisibilityMode.Full);
                    }
                }
                for (Edge edge : transientGraph.getEdges()) {
                    switch (edge.getFunctionMode()) {
                        case Mockup:
                            edge.setVisibilityMode(GuiConfig.VisibilityMode.None);
                            break;
                        default:
                            edge.setVisibilityMode(GuiConfig.VisibilityMode.Full);
                    }
                }
                break;
            case Mockup:
                for (Vertex vertex : transientGraph.getVertices()) {
                    switch (vertex.getFunctionMode()) {
                        case Mockup:
                            vertex.setVisibilityMode(GuiConfig.VisibilityMode.Full);
                            break;
                        default:
                            vertex.setVisibilityMode(GuiConfig.VisibilityMode.None);
                    }
                }
                for (Edge edge : transientGraph.getEdges()) {
                    switch (edge.getFunctionMode()) {
                        case Mockup:
                            edge.setVisibilityMode(GuiConfig.VisibilityMode.Full);
                            break;
                        default:
                            edge.setVisibilityMode(GuiConfig.VisibilityMode.None);
                    }
                }
                break;
        }
    }

    public void resetView() {
        vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
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
//                    continue;
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
//                    continue;
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
//                    continue;
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
//                    continue;
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
//                    continue;
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
//                    continue;
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

    private class MyMouseListener implements MouseListener, MouseMotionListener {

        Vertex selectedVertex = null, edgeStartVertex = null;
        boolean amDraggingVertex = false, amCreatingEdge = false;
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
                case Mockup:
                    if (vertex.getFunctionMode() == FunctionMode.Mockup) {
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
            if (transientGraph.findEdge(edgeStartVertex, vertex) != null) {
                // Edge already exists
                edgeStartVertex.setBeingModified(false);
                resetSelection();
                vv.repaint();
                return;
            }

            if (edgeStartVertex instanceof Transition && vertex instanceof Place) {
                // An edge from this place->transition or transition-> does not exist
                Transition startTransition = (Transition) edgeStartVertex;
                Place endPlace = (Place) vertex;
                boolean isMockup = false;
                if (editorMode == FunctionMode.Mockup
                        && startTransition instanceof MockupTransition
                        && endPlace instanceof MockupPlace) {
                    isMockup = true;
                }
                OutEdge newEdge;
                if (isMockup) {
                    newEdge = new MockupOutEdge((MockupTransition) startTransition, (MockupPlace) endPlace, Mediator.getInstance().getProject().getAndIncLastElementId());
                } else {
                    newEdge = new OutEdge(startTransition, endPlace, editorMode, Mediator.getInstance().getProject().getAndIncLastElementId());
                }
                addEdge(newEdge, startTransition, endPlace);
            } else if (edgeStartVertex instanceof Place && vertex instanceof Transition) {
                // An edge from this place->transition or transition-> does not exist
                Place startPlace = (Place) edgeStartVertex;
                Transition endTransition = (Transition) vertex;
                boolean isMockup = false;
                if (editorMode == FunctionMode.Mockup
                        && startPlace instanceof MockupPlace
                        && endTransition instanceof MockupTransition) {
                    isMockup = true;
                }
                InEdge newEdge;
                if (isMockup) {
                    newEdge = new MockupInEdge((MockupPlace) startPlace, (MockupTransition) endTransition, Mediator.getInstance().getProject().getAndIncLastElementId());
                } else {
                    newEdge = new InEdge(startPlace, endTransition, editorMode, Mediator.getInstance().getProject().getAndIncLastElementId());
                }
                addEdge(newEdge, startPlace, endTransition);
            } else if (edgeStartVertex instanceof Place && vertex instanceof Place) {
                // Trying to create an edge between two places, add an intermittent transition as well
                Place startPlace = (Place) edgeStartVertex;
                Place endPlace = (Place) vertex;
                Transition newTransition;
                boolean isMockup = false;
                if (editorMode == FunctionMode.Mockup
                        && startPlace instanceof MockupPlace
                        && endPlace instanceof MockupPlace) {
                    isMockup = true;
                }
                if (isMockup) {
                    newTransition = new MockupTransition("", Mediator.getInstance().getProject().getAndIncLastElementId());
                } else {
                    newTransition = new Transition("", editorMode, Mediator.getInstance().getProject().getAndIncLastElementId());
                }
                Point freePoint = DreaamHelper.getVertexFreePoint(vv,
                        (int) ((layout.getX(startPlace) + layout.getX(endPlace)) / 2),
                        (int) ((layout.getY(startPlace) + layout.getY(endPlace)) / 2));
                addTransition(newTransition, freePoint);

                InEdge newEdge1;
                if (isMockup) {
                    newEdge1 = new MockupInEdge((MockupPlace) startPlace, (MockupTransition) newTransition, Mediator.getInstance().getProject().getAndIncLastElementId());
                } else {
                    newEdge1 = new InEdge(startPlace, newTransition, editorMode, Mediator.getInstance().getProject().getAndIncLastElementId());
                }
                addEdge(newEdge1, startPlace, newTransition);

                OutEdge newEdge2;
                if (isMockup) {
                    newEdge2 = new MockupOutEdge((MockupTransition) newTransition, (MockupPlace) endPlace, Mediator.getInstance().getProject().getAndIncLastElementId());
                } else {
                    newEdge2 = new OutEdge(newTransition, endPlace, editorMode, Mediator.getInstance().getProject().getAndIncLastElementId());
                }
                addEdge(newEdge2, newTransition, endPlace);
            } else if (edgeStartVertex instanceof Transition && vertex instanceof Transition) {
                // Trying to create an edge between two transitions, add an intermittent place as well
                Transition startTransition = (Transition) edgeStartVertex;
                Transition endTransition = (Transition) vertex;
                Place newPlace;
                boolean isMockup = false;
                if (editorMode == FunctionMode.Mockup
                        && startTransition instanceof MockupTransition
                        && endTransition instanceof MockupTransition) {
                    isMockup = true;
                }
                if (isMockup) {
                    newPlace = new MockupPlace("", Mediator.getInstance().getProject().getAndIncLastElementId());
                } else {
                    newPlace = new Place("", editorMode, Mediator.getInstance().getProject().getAndIncLastElementId());
                }
                Point freePoint = DreaamHelper.getVertexFreePoint(vv,
                        (int) ((layout.getX(startTransition) + layout.getX(endTransition)) / 2),
                        (int) ((layout.getY(startTransition) + layout.getY(endTransition)) / 2));
                addPlace(newPlace, freePoint);

                OutEdge newEdge1;
                if (isMockup) {
                    newEdge1 = new MockupOutEdge((MockupTransition) startTransition, (MockupPlace) newPlace, Mediator.getInstance().getProject().getAndIncLastElementId());
                } else {
                    newEdge1 = new OutEdge(startTransition, newPlace, editorMode, Mediator.getInstance().getProject().getAndIncLastElementId());
                }
                addEdge(newEdge1, startTransition, newPlace);

                InEdge newEdge2;
                if (isMockup) {
                    newEdge2 = new MockupInEdge((MockupPlace) newPlace, (MockupTransition) endTransition, Mediator.getInstance().getProject().getAndIncLastElementId());
                } else {
                    newEdge2 = new InEdge(newPlace, endTransition, editorMode, Mediator.getInstance().getProject().getAndIncLastElementId());
                }
                addEdge(newEdge2, newPlace, endTransition);
            }
            edgeStartVertex.setBeingModified(false);
            resetSelection();
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
            if ((me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK)) == MouseEvent.BUTTON1_DOWN_MASK) {
                // Mouse1
                // Select a vertex (needs to be in mousePressed to begin dragging)
                GraphElementAccessor<Vertex, Edge> pickSupport = vv.getPickSupport();
                if (pickSupport != null) {
                    final Vertex vertex = DreaamHelper.getNearestVertex(vv, framePoint.getX(), framePoint.getY(), CLICK_RADIUS);
                    if (vertex != null) {
                        if (!amCreatingEdge && !amDraggingVertex && selectedVertex == null) {
                            // Select the vertex
                            // !amCreatingEdge - have a selected vertex, on mouse release will connect the two graph elements
                            // !amDraggingVertex - am dragging through another vertex
                            // selectedVertex == null - I think this is redundant
                            selectVertex(vertex);
                            vv.repaint();
                        }
                    } else if (!amDraggingVertex) {
                        // De-select vertex (if one was selected)
                        // !amDraggingVertex - am just dragging really fast (outside click radius)
                        selectVertex(null);
                        vv.repaint();
                    }
                }
            }
        }

        @Override
        public void mouseDragged(MouseEvent me) {
//            System.out.println("Dragged " + me.getButton());

            final Point2D framePoint = me.getPoint();
            final Point2D graphPoint = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(framePoint);

            if (!amDraggingVertex && selectedVertex != null) {
                amDraggingVertex = true;
                mSpec.updateVertexLocation(selectedVertex, DreaamHelper.snapToGrid(graphPoint));
                vv.repaint();
            } else if (amDraggingVertex && selectedVertex != null) {
                mSpec.updateVertexLocation(selectedVertex, DreaamHelper.snapToGrid(graphPoint));
                vv.repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
//            System.out.println("Released " + me.getButton() + "\t" + mouse1 + "\t" + mouse2 + "\t" + mouse3);

            final Point framePoint = me.getPoint();
            final Point2D graphPoint = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(framePoint);
            if (me.getButton() == MouseEvent.BUTTON1
                    && (me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK)) == 0
                    && !amDraggingVertex) {
                // Mouse1 only AND not dragging vertex
                // Select graph element
                GraphElementAccessor<Vertex, Edge> pickSupport = vv.getPickSupport();
                if (pickSupport != null) {
                    final Vertex vertex = DreaamHelper.getNearestVertex(vv, framePoint.getX(), framePoint.getY(), CLICK_RADIUS);
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
            } else if (((me.getButton() == MouseEvent.BUTTON3 && (me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK)) == 0)
                    || (me.getButton() == MouseEvent.BUTTON1 && (me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK)) == MouseEvent.CTRL_DOWN_MASK))
                    && !amDraggingVertex) {
                // (Mouse 3 OR CTRL + Mouse1) AND not dragging vertex
                // Right click menu

                GraphElementAccessor<Vertex, Edge> pickSupport = vv.getPickSupport();
                if (pickSupport != null) {
                    final Vertex vertex = DreaamHelper.getNearestVertex(vv, framePoint.getX(), framePoint.getY(), CLICK_RADIUS);
                    final Edge edge = DreaamHelper.getNearestEdge(vv, framePoint.getX(), framePoint.getY(), CLICK_RADIUS);
                    if (vertex != null && (vertex instanceof MockupPlace || vertex instanceof MockupTransition)) {
                        // Right click place or transition -> show options
                        JPopupMenu popup = new JPopupMenu();
                        popup.add(new AbstractAction("Edit Mockup") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (vertex instanceof MockupPlace) {
                                    MockupPlace mockupPlace = (MockupPlace) vertex;
                                    MockupDetailsD diag = new MockupDetailsD(null, true, mockupPlace);
                                    diag.setVisible(true);
                                    // This part won't run until the Frame closes
                                    mockupPlace.setName(diag.getName());
                                    mockupPlace.setMockupOutputEventMarkups(diag.getMockupOutputEventMarkups());
                                    mockupPlace.setMockupSubMissionType(diag.getMockupSubMissionType());
                                    mockupPlace.setMockupTokens(diag.getMockupTokens());
                                    mockupPlace.setIsHighlighted(diag.getIsHighlighted());
                                    mockupPlace.updateTag();
                                } else if (vertex instanceof MockupTransition) {
                                    MockupTransition mockupTransition = (MockupTransition) vertex;
                                    MockupDetailsD diag = new MockupDetailsD(null, true, mockupTransition);
                                    diag.setVisible(true);
                                    // This part won't run until the Frame closes
                                    mockupTransition.setName(diag.getName());
                                    mockupTransition.setMockupInputEventMarkups(diag.getMockupInputEventMarkups());
                                    mockupTransition.setMockupInputEventStatus(diag.getMockupInputEventStatus());
                                    mockupTransition.setIsHighlighted(diag.getIsHighlighted());
                                    mockupTransition.updateTag();
                                }
                                vv.repaint();
                            }
                        });
                        popup.add(new AbstractAction("Rename") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String name = JOptionPane.showInputDialog(vv, "Vertex name", vertex.getName());
                                if (name == null) {
                                    name = "";
                                }
                                vertex.setName(name);
                                vv.repaint();
                            }
                        });
                        if (vertex instanceof MockupPlace) {
                            final MockupPlace place = (MockupPlace) vertex;
                            if (place.isStart()) {
                                popup.add(new AbstractAction("Unset start") {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        place.setIsStart(false);
                                        vv.repaint();
                                    }
                                });
                            } else {
                                popup.add(new AbstractAction("Set start") {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        place.setIsStart(true);
                                        vv.repaint();
                                    }
                                });
                            }
                            if (place.isEnd()) {
                                popup.add(new AbstractAction("Unset end") {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        place.setIsEnd(false);
                                        vv.repaint();
                                    }
                                });
                            } else {
                                popup.add(new AbstractAction("Set end") {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        place.setIsEnd(true);
                                        if (place.getOutEdges().size() > 0) {
                                            JOptionPane.showMessageDialog(vv, "An end state has output transitions, these will never be used");
                                        }
                                        vv.repaint();
                                    }
                                });
                            }
                        }
                        popup.add(new AbstractAction("Delete") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (vertex instanceof MockupPlace) {
                                    removePlace((MockupPlace) vertex);
                                } else if (vertex instanceof MockupTransition) {
                                    removeTransition((MockupTransition) vertex);
                                }
                            }
                        });
                        popup.show(vv, me.getX(), me.getY());
                    } else if (vertex != null) {
                        // Right click place or transition -> show options
                        JPopupMenu popup = new JPopupMenu();
                        popup.add(new AbstractAction("Rename") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String name = JOptionPane.showInputDialog(vv, "Vertex name", vertex.getName());
                                if (name == null) {
                                    name = "";
                                }
                                vertex.setName(name);
                                vv.repaint();
                            }
                        });
                        popup.add(new AbstractAction("Edit Events") {
                            public void actionPerformed(ActionEvent e) {
                                SelectEventD diag = new SelectEventD(null, true, mSpec.getEventSpecList(vertex), mSpec, vertex instanceof Transition, vertex instanceof Place);

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
                            }
                        });
                        popup.add(new AbstractAction("Edit Markups") {
                            public void actionPerformed(ActionEvent e) {
                                SelectEventForMarkupD eventDiag = new SelectEventForMarkupD(null, true, vertex.getEventSpecs());
                                eventDiag.setVisible(true);
                                // This part won't run until the Frame closes
                                if (eventDiag.selectedEventSpecs == null) {
                                    // If no event was selected to edit, return
                                    return;
                                }

                                for (ReflectedEventSpecification eventSpec : eventDiag.selectedEventSpecs) {
                                    SelectMarkupD markupDiag = new SelectMarkupD(null, true, eventSpec.getMarkupSpecs(), mSpec);
                                    markupDiag.setVisible(true);
                                    // This part won't run until the Frame closes
                                    eventSpec.setMarkupSpecs(markupDiag.getSelectedMarkupSpecs());
                                    vertex.updateTag();
                                    vv.repaint();
                                }
                            }
                        });
                        if (vertex instanceof Place) {
                            final Place place = (Place) vertex;
                            popup.add(new AbstractAction("Use Event Wizard") {
                                @Override
                                public void actionPerformed(ActionEvent ae) {
                                    EventWizardD wizardD = new EventWizardD(null, true, mSpec, place, transientGraph, layout, vv);
                                    wizardD.setVisible(true);

                                    vv.repaint();
                                }
                            });
                            popup.add(new AbstractAction("Edit Sub-missions") {
                                public void actionPerformed(ActionEvent e) {

                                    SelectSubMissionD d = new SelectSubMissionD(null, true, mSpec, mediator.getProject(), place.getSubMissionTemplates(), place.getSubMissionToIsSharedInstance(), place.getSubMissionToTaskMap());
                                    d.setVisible(true);
                                    if (d.confirmedExit()) {
                                        for (MissionPlanSpecification createdSubMMSpec : d.getCreatedSubMissions()) {
                                            mediator.getProject().addSubMissionPlan(createdSubMMSpec, mSpec);
                                        }
                                        for (MissionPlanSpecification deletedSubMMSpec : d.getDeletedSubMissions()) {
                                            mediator.getProject().removeMissionPlan(deletedSubMMSpec);
                                        }
                                        place.setSubMissionTemplates(d.getSubMissions());
                                        place.setSubMissionToIsSharedInstance(d.getSubMissionToIsSharedInstance());
                                        place.setSubMissionToTaskMap(d.getSubMissionToTaskMap());
                                        place.updateTag();
                                        dreaam.refreshMissionTree();
                                        dreaam.selectNode(mediator.getProject().getNode(mSpec));
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
                        popup.add(new AbstractAction("Delete") {
                            public void actionPerformed(ActionEvent e) {
                                boolean reallyRemove = true;
                                // @todo Consider linking requirements to objects to make the deletion process cleaner
                                // @todo GetFilledBy only returns one object, if multiple fulfill, not listed
                                // @todo Cancel/No not handled smoothly when multiple requirements filled by this.
                                if (mediator.getProject().getReqs() != null) {
                                    for (RequirementSpecification requirementSpecification : mediator.getProject().getReqs()) {
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
                        popup.show(vv, me.getX(), me.getY());
                    } else if (edge != null && (edge instanceof MockupInEdge || edge instanceof MockupOutEdge)) {
                        JPopupMenu popup = new JPopupMenu();
                        popup.add(new AbstractAction("Edit Mockup") {
                            public void actionPerformed(ActionEvent e) {
                                if (edge instanceof MockupInEdge) {
                                    MockupInEdge mockupInEdge = (MockupInEdge) edge;
                                    MockupDetailsD diag = new MockupDetailsD(null, true, mockupInEdge);
                                    diag.setVisible(true);
                                    // This part won't run until the Frame closes
                                    mockupInEdge.setMockupTokenRequirements(diag.getMockupTokenRequirements());
                                    mockupInEdge.setIsHighlighted(diag.getIsHighlighted());
                                    mockupInEdge.updateTag();
                                } else if (edge instanceof MockupOutEdge) {
                                    MockupOutEdge mockupOutEdge = (MockupOutEdge) edge;
                                    MockupDetailsD diag = new MockupDetailsD(null, true, mockupOutEdge);
                                    diag.setVisible(true);
                                    // This part won't run until the Frame closes
                                    mockupOutEdge.setMockupTokenRequirements(diag.getMockupTokenRequirements());
                                    mockupOutEdge.setIsHighlighted(diag.getIsHighlighted());
                                    mockupOutEdge.updateTag();
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
                                Place place;
                                if (editorMode == FunctionMode.Mockup) {
                                    place = new MockupPlace("", Mediator.getInstance().getProject().getAndIncLastElementId());
                                } else {
                                    place = new Place("", editorMode, Mediator.getInstance().getProject().getAndIncLastElementId());
                                }
                                addPlace(place, DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY()));
                            }
                        });
                        popup.add(new AbstractAction("New Transition") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                Transition transition;
                                if (editorMode == FunctionMode.Mockup) {
                                    transition = new MockupTransition("", Mediator.getInstance().getProject().getAndIncLastElementId());
                                } else {
                                    transition = new Transition("", editorMode, Mediator.getInstance().getProject().getAndIncLastElementId());
                                }
                                addTransition(transition, DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY()));
                            }
                        });
                        popup.add(new AbstractAction("Use Event Wizard") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                EventWizardD wizardD = new EventWizardD(null, true, mSpec, new Point((int) graphPoint.getX(), (int) graphPoint.getY()), transientGraph, layout, vv);
                                wizardD.setVisible(true);

                                vv.repaint();
                            }
                        });
                        popup.add(new AbstractAction("Edit Global Variables") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                SelectGlobalVariableD variableD = new SelectGlobalVariableD(null, true, mediator.getProject().getGlobalVariableToValue());
                                variableD.setVisible(true);
                                if (variableD.confirmedExit()) {
                                    HashMap<String, Object> variables = variableD.getVariables();
                                    mediator.getProject().setGlobalVariableToValue(variables);
                                }
                            }
                        });
                        popup.show(vv, me.getX(), me.getY());
                    }
                }
            } else {
                resetSelection();
                vv.repaint();
            }
        }

        private void selectVertex(Vertex vertex) {
            if (selectedVertex != null) {
                selectedVertex.setBeingModified(false);
            }
            selectedVertex = vertex;
            if (selectedVertex != null) {
                selectedVertex.setBeingModified(true);
            }
        }

        private void resetSelection() {
            selectVertex(null);
            edgeStartVertex = null;
            amDraggingVertex = false;
            amCreatingEdge = false;
            prevMousePoint = null;
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
    }

    public void setMissionSpecification(MissionPlanSpecification spec) {
        mml.resetSelection();
        this.mSpec = spec;
        transientGraph = spec.getTransientGraph();
        layout = spec.getLayout();
        // Update vizualization's layout
        vv.getModel().setGraphLayout(spec.getLayout());
        layout.setGraph(transientGraph);
        spec.updateTags();
        refreshGraphVisibility();

        MultiLayerTransformer mlt = vv.getRenderContext().getMultiLayerTransformer();
        mlt.getTransformer(Layer.LAYOUT).getTransform().setTransform(spec.getLayoutTransform());
        mlt.getTransformer(Layer.VIEW).getTransform().setTransform(spec.getView());
        if (vv.getCenter() != null) {
            scaler.scale(vv, 1.0f, vv.getCenter());
        }
        vv.repaint();
    }

    public void reloadGraph() {
        setMissionSpecification(mSpec);
    }

    public void addPlace(Place place, Point point) {
        mSpec.addPlace(place, point);
        vv.repaint();
    }

    public void removePlace(Place place) {
        mSpec.removePlace(place);
        vv.repaint();
    }

    public void addTransition(Transition transition, Point point) {
        mSpec.addTransition(transition, point);
        vv.repaint();

    }

    public void removeTransition(Transition transition) {
        mSpec.removeTransition(transition);
        vv.repaint();
    }

    public void addEdge(InEdge e, Place startPlace, Transition endTransition) {
        mSpec.addEdge(e, startPlace, endTransition);
        vv.repaint();
    }

    public void addEdge(OutEdge e, Transition startTransition, Place endPlace) {
        mSpec.addEdge(e, startTransition, endPlace);
        vv.repaint();
    }

    public void removeEdge(Edge e) {
        mSpec.removeEdge(e);
        vv.repaint();
    }

    private void snapViewToVisible() {
        double[] corners = getVisibleMissionDims();
        setCorners(corners);
    }

    /**
     * Adjust visualization transform so that a specified rectangle in vertex
     * space is visible
     *
     * @param corners A double[4] set to {min x point, min y point, max x point,
     * max y point}
     */
    private void setCorners(double[] corners) {
        if (Double.isNaN(corners[0]) || Double.isNaN(corners[1]) || Double.isNaN(corners[2]) || Double.isNaN(corners[3])) {
            return;
        }

        // Pad edges to account for shape dimensions, event/markup text, etc
        // @todo could have this customized to corner vertices' properties
        corners[0] -= 15;
        corners[1] -= 15;
        corners[2] += 30;
        corners[3] += 30;

        // Calculate scale and translation transform
        double[] transform = new double[6];
        transform[0] = -panel.getWidth() / (corners[0] - corners[2]);
        transform[3] = -panel.getHeight() / (corners[1] - corners[3]);
        // Use 1:1 zoom ratio
        double adjScale = Math.min(transform[0], transform[3]);
        transform[0] = adjScale;
        transform[3] = adjScale;
        transform[4] = -corners[0] * transform[0];
        transform[5] = -corners[1] * transform[3];
        AffineTransform at = new AffineTransform(transform);
        // Mimic CrossoverScalingControl zooming
        //  View layer's scale must be <= 1
        //  Layout layers's scale must be >= 1
        MultiLayerTransformer mlt = vv.getRenderContext().getMultiLayerTransformer();
        if (adjScale < 1) {
            mlt.getTransformer(Layer.VIEW).getTransform().setTransform(at);
            mlt.getTransformer(Layer.LAYOUT).getTransform().setToScale(1.0, 1.0);
        } else {
            mlt.getTransformer(Layer.LAYOUT).getTransform().setTransform(at);
            mlt.getTransformer(Layer.VIEW).getTransform().setToScale(1.0, 1.0);
        }
    }

    /**
     * Return area capturing all places with tokens
     *
     * @return A double[4] set to {min x point, min y point, max x point, max y
     * point}
     */
    private double[] getActiveMissionDims() {
        return getCorners(0);
    }

    /**
     * Return area capturing all places with visibility mode full
     *
     * @return A double[4] set to {min x point, min y point, max x point, max y
     * point}
     */
    private double[] getVisibleMissionDims() {
        return getCorners(1);
    }

    /**
     * Return area capturing all vertices, ignoring visibility mode
     *
     * @return A double[4] set to {min x point, min y point, max x point, max y
     * point}
     */
    private double[] getFullMissionDims() {
        return getCorners(2);
    }

    private double[] getCorners(int mode) {
        // Mode
        //  0: Active
        //  1: Visible
        //  2: All

        double[] corners = new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN};
        Map<Vertex, Point2D> locations = mSpec.getLocations();
        for (Vertex vertex : locations.keySet()) {
            boolean add = false;
            switch (mode) {
                case 0:
                default:
                    add = vertex instanceof Place && ((Place) vertex).getIsActive();
                    break;
                case 1:
                    add = vertex.getVisibilityMode() == GuiConfig.VisibilityMode.Full;
                    break;
                case 2:
                    add = true;
                    break;
            }
            if (add) {
                // Expand area to capture this point
                if (locations.get(vertex) != null) {
                    Point2D point = locations.get(vertex);
                    if (Double.isNaN(corners[0]) || point.getX() < corners[0]) {
                        corners[0] = point.getX();
                    }
                    if (Double.isNaN(corners[2]) || point.getX() > corners[2]) {
                        corners[2] = point.getX();
                    }
                    if (Double.isNaN(corners[1]) || point.getY() < corners[1]) {
                        corners[1] = point.getY();
                    }
                    if (Double.isNaN(corners[3]) || point.getY() > corners[3]) {
                        corners[3] = point.getY();
                    }
                } else {
                    LOGGER.severe("Vertex [" + vertex + "] has no graph location");
                }
            }
        }
        return corners;
    }
}
