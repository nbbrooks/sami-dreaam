package dreaam.wizard;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.Point;
import sami.DreaamHelper;
import sami.engine.Mediator;
import sami.event.ReflectedEventSpecification;
import sami.mission.Edge;
import sami.mission.InEdge;
import sami.mission.MissionPlanSpecification;
import sami.mission.OutEdge;
import sami.mission.Place;
import sami.mission.Transition;
import sami.mission.Vertex;
import sami.mission.Vertex.FunctionMode;

/**
 *
 * @author nbb
 */
public class UnhandledEventWizard implements EventWizardInt {

    public UnhandledEventWizard() {
    }

    @Override
    public boolean runWizard(String eventClassname, MissionPlanSpecification mSpec, Place p1, Graph<Vertex, Edge> dsgGraph, AbstractLayout<Vertex, Edge> layout, VisualizationViewer<Vertex, Edge> vv) {
        if (!p1.getEventSpecs().isEmpty()) {
            // Don't act on places which already have event specs
            return false;
        }
        Point pPoint = new Point((int) layout.getX(p1), (int) layout.getY(p1));
        ReflectedEventSpecification eventSpec;
        // Create place for OE
        // P1
        p1.setName("");
        eventSpec = new ReflectedEventSpecification(eventClassname);
        p1.addEventSpec(eventSpec);
        mSpec.updateEventSpecList(p1, eventSpec);

        // Create an empty transition
        // T1_2
        Transition t1_2 = new Transition("", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
        Point point = DreaamHelper.getVertexFreePoint(vv, pPoint.getX(), pPoint.getY(), new int[]{1});
        mSpec.addTransition(t1_2, point);
                // Create Edges
        // IE-P1-T1_2n: has RT
        InEdge ie_P1_T1_2 = new InEdge(p1, t1_2, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
        mSpec.addEdge(ie_P1_T1_2, p1, t1_2);

        // Create an empty place
        // P2n
        Place p2 = new Place("", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
        point = DreaamHelper.getVertexFreePoint(vv, point.getX(), point.getY(), new int[]{3});
        mSpec.addPlace(p2, point);
        // OE-T1_2n-P2n
        OutEdge oe_T1_2_P2 = new OutEdge(t1_2, p2, FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
        mSpec.addEdge(oe_T1_2_P2, t1_2, p2);

        return true;
    }

    @Override
    public boolean runWizard(String eventClassname, MissionPlanSpecification mSpec, Point graphPoint, Graph<Vertex, Edge> dsgGraph, AbstractLayout<Vertex, Edge> layout, VisualizationViewer<Vertex, Edge> vv) {
            // We can handle this - create P1
            Place p1 = new Place("", Vertex.FunctionMode.Nominal, Mediator.getInstance().getProject().getAndIncLastElementId());
            graphPoint = DreaamHelper.getVertexFreePoint(vv, graphPoint.getX(), graphPoint.getY(), new int[]{2});
            mSpec.addPlace(p1, graphPoint);
            // Call overloaded method
            return runWizard(eventClassname, mSpec, p1, dsgGraph, layout, vv);
    }

    @Override
    public RequiredTokenType getOeTokenNeeded(String oe) {
        return null;
    }

    @Override
    public RequiredInRequirement getIeMinInReq(String ie) {
        return null;
    }

    @Override
    public RequiredOutRequirement getIeMinOutReq(String ie) {
        return null;
    }
}
