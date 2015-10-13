package dreaam.wizard;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.Point;
import sami.mission.Edge;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Vertex;

/**
 *
 * @author nbb
 */
public interface EventWizardInt {

    public enum RequiredTokenType {

        Task, Proxy, Generic, None
    };

    public enum RequiredInRequirement {

        HasRt, None
    };

    public enum RequiredOutRequirement {

        AddRt, TakeRt, None
    };

    public boolean runWizard(String eventClassname, MissionPlanSpecification mSpec, Place place, Graph<Vertex, Edge> dsgGraph, AbstractLayout<Vertex, Edge> layout, VisualizationViewer<Vertex, Edge> vv);

    public boolean runWizard(String eventClassname, MissionPlanSpecification mSpec, Point graphPoint, Graph<Vertex, Edge> dsgGraph, AbstractLayout<Vertex, Edge> layout, VisualizationViewer<Vertex, Edge> vv);

    public RequiredTokenType getOeTokenNeeded(String oe);

    public RequiredInRequirement getIeMinInReq(String ie);

    public RequiredOutRequirement getIeMinOutReq(String ie);
}
