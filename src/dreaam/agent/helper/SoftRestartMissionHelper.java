package dreaam.agent.helper;

import java.util.Hashtable;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Transition;

/**
 *
 * @author nbb
 */
public class SoftRestartMissionHelper extends HelperAgent {

    Hashtable<MissionPlanSpecification, Place> missionToStart = new Hashtable<MissionPlanSpecification, Place>();
    Hashtable<MissionPlanSpecification, Transition> missionToTransition = new Hashtable<MissionPlanSpecification, Transition>();

    public SoftRestartMissionHelper() {
    }

    @Override
    public void run() {
//        TokenSpecification noReqTokenSpec = new TokenSpecification("No Req", TokenSpecification.TokenType.MatchNoReq, null);
//
//        for (MissionPlanSpecification missionPlanSpecification : mediator.getMissions()) {
//            // Fetch transition that handles AbortMissionReceived events
//            Transition transition = missionToTransition.get(missionPlanSpecification);
//            if (transition == null) {
//                // Add transition
//                transition = new Transition("SoftRestartMissionHelper", FunctionMode.Recovery);
//                missionPlanSpecification.getGraph().addVertex(transition);
//                Point freePoint = getVertexPoint(missionPlanSpecification.getLocations(), true);
//                missionPlanSpecification.getLocations().put(transition, freePoint);
//                // Add AbortMissionReceived to transition
//                ReflectedEventSpecification eventSpec = new ReflectedEventSpecification(SoftRestartMissionReceived.class.getName());
//                transition.addEventSpec(eventSpec);
//                missionPlanSpecification.updateEventSpecList(transition, eventSpec);
//                missionToTransition.put(missionPlanSpecification, transition);
//            }
//            // Fetch list of places that are connected to the transition
//            ArrayList<Place> places = transition.getInPlaces();
//            // Connect any places that should be connected to the transition but are not
//            for (Vertex vertex : missionPlanSpecification.getGraph().getVertices()) {
//                if (vertex instanceof Place
//                        && !places.contains((Place) vertex)
//                        && !((Place) vertex).isEnd()
//                        && ((Place) vertex).getFunctionMode() == FunctionMode.Nominal) {
//                    // Add edge from place to transition with Proxy token spec
//                    Edge newEdge = new Edge(vertex, transition, FunctionMode.Recovery);
//                    missionPlanSpecification.updateEdgeToTokenSpecListMap(newEdge, noReqTokenSpec);
//                    newEdge.addTokenName(noReqTokenSpec.toString());
//                    vertex.addOutEdge(newEdge);
//                    transition.addInEdge(newEdge);
//                    missionPlanSpecification.getGraph().addEdge(newEdge, vertex, transition);
//                    ((Place) vertex).addOutTransition(transition);
//                    (transition).addInPlace((Place) vertex);
//                }
//            }
//            // Disconnect any places that should not be connected any longer
//            for (Place place : transition.getInPlaces()) {
//                if (place.isEnd()) {
//                    Edge edge = missionPlanSpecification.getGraph().findEdge(place, transition);
//                    place.removeOutEdge(edge);
//                    transition.removeInEdge(edge);
//                    place.removeOutTransition(transition);
//                    transition.removeInPlace(place);
//                    missionPlanSpecification.removeTokenSpecList(edge);
//                    missionPlanSpecification.getGraph().removeEdge(edge);
//                }
//            }
//
//            // Find start place if necessary and connect it to transition
//            Place startPlace = missionToStart.get(missionPlanSpecification);
//            if (startPlace == null) {
//                for (Vertex vertex : missionPlanSpecification.getGraph().getVertices()) {
//                    if (vertex instanceof Place
//                            && ((Place) vertex).isStart()
//                            && ((Place) vertex).getFunctionMode() == FunctionMode.Nominal) {
//                        missionToStart.put(missionPlanSpecification, (Place) vertex);
//                        // Add edge from transition to start place
//                        Edge newEdge = new Edge(transition, vertex, FunctionMode.Recovery);
//                        // @todo Start place needs to get all tokens from Engine
////                        missionPlanSpecification.updateEdgeToTokenSpecListMap(newEdge, missionPlanSpecification.getAllTokenSpec());
////                        newEdge.addTokenName(missionPlanSpecification.getAllTokenSpec().toString());
//                        transition.addOutEdge(newEdge);
//                        vertex.addInEdge(newEdge);
//                        missionPlanSpecification.getGraph().addEdge(newEdge, transition, vertex);
//                        (transition).addOutPlace((Place) vertex);
//                        ((Place) vertex).addInTransition(transition);
//                    }
//                }
//            }
//        }
    }
}