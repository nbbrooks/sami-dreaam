package dreaam.wizard;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;
import java.awt.geom.Point2D;
import sami.mission.Edge;
import sami.mission.MissionPlanSpecification;
import sami.mission.Vertex;

/**
 *
 * @author nbb
 */
public interface EventWizardInt {

    public boolean runWizard(String eventClassname, MissionPlanSpecification mSpec, Point2D graphPoint, Graph<Vertex, Edge> dsgGraph, AbstractLayout<Vertex, Edge> layout);
}
