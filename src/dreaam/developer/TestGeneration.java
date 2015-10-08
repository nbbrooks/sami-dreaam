package dreaam.developer;

import sami.event.Event;
import sami.mission.Edge;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.TestCase;
import sami.mission.Transition;
import sami.mission.Vertex;
import edu.uci.ics.jung.graph.Graph;
import java.util.ArrayList;
import sami.engine.Mediator;

/**
 *
 * @author pscerri
 */
public class TestGeneration {

    private final int maxDepth = 10;
    ArrayList<TestCase> testCases = null;

    public TestGeneration() {
        Mediator mediator = Mediator.getInstance();

        ArrayList<Node> cases = new ArrayList<Node>();

        System.out.println("Starting test case generation");

        for (MissionPlanSpecification missionPlanSpecification : mediator.getProject().getAllMissionPlans()) {
            Graph<Vertex, Edge> graph = missionPlanSpecification.getTransientGraph();
            // Grab the start place
            Place start = null;
            for (Vertex v : graph.getVertices()) {
                if (v instanceof Place && ((Place) v).isStart()) {
                    start = (Place) v;
                }
            }

            if (start != null) {
                ArrayList<Node> queue = new ArrayList<Node>();

                Node n = new Node(start);
                queue.add(n);

                while (!queue.isEmpty()) {
                    Node first = queue.remove(0);
                    System.out.println("Expanding: " + first);
                    ArrayList<Node> expansions = first.expand();

                    // For each of this node's children, add it to the queue if it has a small number of children
                    if (expansions != null) {
                        for (Node node : expansions) {

                            if (node.places.size() < maxDepth) {
                                queue.add(node);
                            } else {
                                System.out.println("TestGeneration max depth exceeded");
                                // Just add what we have so far
                                cases.add(first);
                            }
                        }
                    } else {
                        // Implies an end state
                        cases.add(first);
                    }
                }
            } else {
                System.out.println("TestGeneration: No start state, can't generate test csae");
            }
        }

        testCases = new ArrayList<TestCase>();
        for (Node node : cases) {
            TestCase t = new TestCase(node.events);
            testCases.add(t);
        }

    }

    private class Node {

        ArrayList<Place> places;
        ArrayList<Transition> transitions;
        ArrayList<Event> events;

        public Node(Place p) {
            places = new ArrayList<Place>();
            places.add(p);
            events = new ArrayList<Event>();
        }

        public Node(ArrayList<Place> places, ArrayList<Event> events) {
            this.places = places;
            this.events = events;
        }

        public Node(Node n) {
            places = (ArrayList<Place>) n.places.clone();
            events = (ArrayList<Event>) n.events.clone();
        }

        public ArrayList<Node> expand() {
            Place last = places.get(places.size() - 1);
            if (last.getOutTransitions() != null && last.getOutTransitions().size() > 0) {
                // For each transition from this node, add its resulting place(s) 
                //  and event(s) to a copy of this node's lists
                ArrayList<Node> ret = new ArrayList<Node>();
                for (Transition transition : last.getOutTransitions()) {
                    for (Place outPlace : transition.getOutPlaces()) {
                        ArrayList<Place> ps = (ArrayList<Place>) places.clone();
                        ArrayList<Event> es = (ArrayList<Event>) events.clone();
                        ps.add(outPlace);
                        es.addAll(transition.getInputEvents());
                        es.addAll(outPlace.getOutputEvents());
                        Node n = new Node(ps, es);
                        ret.add(n);
                    }
                }
                return ret;
            }
            return null;
        }
    }
}
