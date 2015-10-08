package dreaam.agent.checker;

import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Transition;
import sami.mission.Vertex;
import edu.uci.ics.jung.graph.Graph;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import sami.mission.Edge;

/**
 *
 * @author pscerri
 */
public class ReachabilityChecker extends CheckerAgent {

    private static final Logger LOGGER = Logger.getLogger(ReachabilityChecker.class.getName());

    public ReachabilityChecker() {
    }

    @Override
    public ArrayList<AgentMessage> getMessages() {
        ArrayList<AgentMessage> msgs = new ArrayList<AgentMessage>();
        for (MissionPlanSpecification missionPlanSpecification : mediator.getProject().getAllMissionPlans()) {
            Place start = null;
            for (Vertex vertex : missionPlanSpecification.getTransientGraph().getVertices()) {
                if (vertex instanceof Place && ((Place) vertex).isStart()) {
                    start = (Place) vertex;
                }
            }

            if (start != null) {
                Graph g = missionPlanSpecification.getTransientGraph();

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
                    LOGGER.info("Adding unreachable message");
                    AgentMessage m = new AgentMessage(this, "Unreachable states", os);
                    msgs.add(m);
                } else {
                    LOGGER.fine("All states were reachable");
                }
            }

        }

        return msgs;
    }

    public void removeIllegalConnections() {
        for (MissionPlanSpecification missionPlanSpecification : mediator.getProject().getAllMissionPlans()) {
            for (Vertex vertex : missionPlanSpecification.getTransientGraph().getVertices()) {
                // Check for in/out transition/place with no matching edge
                ArrayList<Vertex> verticesToRemove = new ArrayList<Vertex>();
                if (vertex instanceof Place) {
                    Place thisPlace = (Place) vertex;

                    // Check for duplicate entries in connected vertices
                    Set<Transition> set = new HashSet<Transition>(thisPlace.getInTransitions());
                    int numDuplicates = thisPlace.getInTransitions().size() - set.size();
                    if (numDuplicates > 0) {
                        LOGGER.info("Found " + numDuplicates + " dupplicates in inTransitions");
                    }
                    ArrayList<Transition> list = new ArrayList<Transition>();
                    list.addAll(set);
                    thisPlace.setInTransitions(list);

                    // Check for duplicate entries in connected vertices
                    set = new HashSet<Transition>(thisPlace.getOutTransitions());
                    numDuplicates = thisPlace.getOutTransitions().size() - set.size();
                    if (numDuplicates > 0) {
                        LOGGER.info("Found " + numDuplicates + " dupplicates in outTransitions");
                    }
                    list = new ArrayList<Transition>();
                    list.addAll(set);
                    thisPlace.setOutTransitions(list);

                    // Check for connected vertices with no corresponding edge
                    for (Transition t : thisPlace.getInTransitions()) {
                        Edge edge = missionPlanSpecification.getTransientGraph().findEdge(t, vertex);
                        if (edge == null) {
                            LOGGER.info("Missing edge in vertex: " + vertex.getTag()
                                    + "\n\t " + t.getTag()
                                    + "\n\t " + vertex.getTag());
                            verticesToRemove.add(t);
                        }
                    }
                    for (Vertex v : verticesToRemove) {
                        ((Place) vertex).removeInTransition((Transition) v);
                    }

                    verticesToRemove = new ArrayList<Vertex>();
                    for (Transition t : thisPlace.getOutTransitions()) {
                        Edge edge = missionPlanSpecification.getTransientGraph().findEdge(vertex, t);
                        if (edge == null) {
                            LOGGER.info("Missing edge in vertex: " + vertex.getTag()
                                    + "\n\t " + vertex.getTag()
                                    + "\n\t " + t.getTag());
                            verticesToRemove.add(t);
                        }

                    }
                    for (Vertex v : verticesToRemove) {
                        ((Place) vertex).removeOutTransition((Transition) v);
                    }
                } else if (vertex instanceof Transition) {
                    Transition thisTransition = (Transition) vertex;

                    // Check for duplicate entries in connected vertices
                    Set<Place> set = new HashSet<Place>(thisTransition.getInPlaces());
                    int numDuplicates = thisTransition.getInPlaces().size() - set.size();
                    if (numDuplicates > 0) {
                        LOGGER.info("Found " + numDuplicates + " dupplicates in inPlaces");
                    }
                    ArrayList<Place> list = new ArrayList<Place>();
                    list.addAll(set);
                    thisTransition.setInPlaces(list);

                    // Check for duplicate entries in connected vertices
                    set = new HashSet<Place>(thisTransition.getOutPlaces());
                    numDuplicates = thisTransition.getOutPlaces().size() - set.size();
                    if (numDuplicates > 0) {
                        LOGGER.info("Found " + numDuplicates + " dupplicates in outPlaces");
                    }
                    list = new ArrayList<Place>();
                    list.addAll(set);
                    thisTransition.setOutPlaces(list);

                    for (Place p : thisTransition.getInPlaces()) {
                        Edge edge = missionPlanSpecification.getTransientGraph().findEdge(p, vertex);
                        if (edge == null) {
                            LOGGER.info("Missing edge in vertex: " + vertex.getTag()
                                    + "\n\t " + p.getTag()
                                    + "\n\t " + vertex.getTag());
                            verticesToRemove.add(p);
                        }
                    }
                    for (Vertex v : verticesToRemove) {
                        ((Transition) vertex).removeInPlace((Place) v);
                    }

                    verticesToRemove = new ArrayList<Vertex>();
                    for (Place p : thisTransition.getOutPlaces()) {
                        Edge edge = missionPlanSpecification.getTransientGraph().findEdge(vertex, p);
                        if (edge == null) {
                            LOGGER.info("Missing edge in vertex: " + vertex.getTag()
                                    + "\n\t " + vertex.getTag()
                                    + "\n\t " + p.getTag());
                            verticesToRemove.add(p);
                        }
                    }
                    for (Vertex v : verticesToRemove) {
                        ((Transition) vertex).removeOutPlace((Place) v);
                    }
                }
            }
        }
    }
}
