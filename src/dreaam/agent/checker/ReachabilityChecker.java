package dreaam.agent.checker;

import dreaam.agent.checker.CheckerAgent;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Transition;
import sami.mission.Vertex;
import edu.uci.ics.jung.graph.Graph;
import java.util.ArrayList;

/**
 *
 * @author pscerri
 */
public class ReachabilityChecker extends CheckerAgent {

    public ReachabilityChecker() {
    }

    @Override
    public ArrayList<AgentMessage> getMessages() {
        ArrayList<AgentMessage> msgs = new ArrayList<AgentMessage>();
        for (MissionPlanSpecification missionPlanSpecification : mediator.getMissions()) {
            Place start = null;
            for (Vertex vertex : missionPlanSpecification.getGraph().getVertices()) {
                if (vertex instanceof Place && ((Place) vertex).isStart()) {
                    start = (Place) vertex;
                }
            }

            if (start != null) {
                Graph g = missionPlanSpecification.getGraph();

                ArrayList graphVertices = new ArrayList(g.getVertices());
                ArrayList<Vertex> connectedVertices = new ArrayList<Vertex>();
                connectedVertices.add(start);
                while (!connectedVertices.isEmpty()) {
                    Vertex v = connectedVertices.remove(0);
                    graphVertices.remove(v);
                    // I am currently storing the inter-vertice connections 
                    //  inside the Vertex object, so I don't need to do the 
                    //  connection checking with the Edge object
                    if (v instanceof Place) {
                        for (Transition transition : ((Place) v).getOutTransitions()) {
                            if (graphVertices.contains(transition)) {
                                connectedVertices.add(transition);
                            }
                        }
                    } else if (v instanceof Transition) {
                        for (Place place : ((Transition) v).getOutPlaces()) {
                            if (graphVertices.contains(place)) {
                                connectedVertices.add(place);
                            }
                        }
                    }
                }
                if (!graphVertices.isEmpty()) {
                    Object[] os = graphVertices.toArray();
                    System.out.println("Adding unreachable message");
                    AgentMessage m = new AgentMessage(this, "Unreachable states", os);
                    msgs.add(m);
                } else {
                    System.out.println("All states were reachable");
                }
            }

        }

        return msgs;
    }
}
