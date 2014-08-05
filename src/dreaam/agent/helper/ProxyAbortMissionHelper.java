package dreaam.agent.helper;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
import sami.event.ProxyAbortMissionReceived;
import sami.event.ReflectedEventSpecification;
import sami.event.SendAbortMission;
import sami.mission.InEdge;
import sami.mission.InTokenRequirement;
import sami.mission.MissionPlanSpecification;
import sami.mission.OutEdge;
import sami.mission.OutTokenRequirement;
import sami.mission.Place;
import sami.mission.TokenRequirement;
import sami.mission.Transition;
import sami.mission.Vertex;
import sami.mission.Vertex.FunctionMode;

/**
 *
 * @author pscerri
 */
public class ProxyAbortMissionHelper extends HelperAgent {

    private static final Logger LOGGER = Logger.getLogger(ProxyAbortMissionHelper.class.getName());
    final String VERTEX_NAME = "ProxyAbortMissionHelper";
    Hashtable<MissionPlanSpecification, Place> missionToEndPlace = new Hashtable<MissionPlanSpecification, Place>();
    Hashtable<MissionPlanSpecification, Hashtable<Place, Transition>> missionToTransitions = new Hashtable<MissionPlanSpecification, Hashtable<Place, Transition>>();

    public ProxyAbortMissionHelper() {
    }

    @Override
    public void run() {
        OutTokenRequirement takeNoneTokenReq = new OutTokenRequirement(TokenRequirement.MatchCriteria.None, TokenRequirement.MatchQuantity.None, TokenRequirement.MatchAction.Take);
        InTokenRequirement relProxyTokenReq = new InTokenRequirement(TokenRequirement.MatchCriteria.RelevantToken, TokenRequirement.MatchQuantity.All);

        for (MissionPlanSpecification missionPlanSpecification : mediator.getProjectSpec().getAllMissionPlans()) {
            // First check that we actually need an end place (ie have a place that is neither a start nor a stop place)
            boolean needEndPlace = false;
            for (Vertex vertex : missionPlanSpecification.getGraph().getVertices()) {
                if (vertex instanceof Place
                        && vertex.getFunctionMode() == FunctionMode.Nominal
                        && !((Place) vertex).isStart()
                        && !((Place) vertex).isEnd()) {
                    needEndPlace = true;
                    break;
                }
            }

            // Fetch end place with SendAbortMission
            Place endPlace = missionToEndPlace.get(missionPlanSpecification);
            if (endPlace == null) {
                // First time running helper since creating and/or loading this mission, try to find helper's end place
                for (Vertex vertex : missionPlanSpecification.getGraph().getVertices()) {
                    if (vertex instanceof Place
                            && ((Place) vertex).isEnd()
                            && vertex.getFunctionMode() == FunctionMode.Recovery
                            && vertex.getName().equals(VERTEX_NAME)) {
                        endPlace = (Place) vertex;
                        missionToEndPlace.put(missionPlanSpecification, endPlace);
                        break;
                    }
                }
            }

            if (needEndPlace) {
                if (endPlace == null) {
                    // First time running helper since creating this mission, construct helper's end place
                    endPlace = new Place(VERTEX_NAME, FunctionMode.Recovery);
                    endPlace.setIsEnd(true);
                    missionPlanSpecification.getGraph().addVertex(endPlace);
                    Point freePoint = getVertexPoint(missionPlanSpecification.getLocations(), false);
                    missionPlanSpecification.getLocations().put(endPlace, freePoint);
                    // Add AbortMission event to place
                    ReflectedEventSpecification sendAbortSpec = new ReflectedEventSpecification(SendAbortMission.class.getName());
                    missionPlanSpecification.updateEventSpecList(endPlace, sendAbortSpec);
                    endPlace.addEventSpec(sendAbortSpec);
                    missionToEndPlace.put(missionPlanSpecification, endPlace);
                }

                // Connect any nominal places that should be connected to the end place but are not
                // Fetch lookup of nominal places to their transitions which are connected to the end place
                Hashtable<Place, Transition> transitionLookup = missionToTransitions.get(missionPlanSpecification);
                if (transitionLookup == null) {
                    // First time running helper for this mission, construct lookup
                    transitionLookup = new Hashtable<Place, Transition>();
                    missionToTransitions.put(missionPlanSpecification, transitionLookup);
                    for (Vertex vertex : missionPlanSpecification.getGraph().getVertices()) {
                        if (vertex instanceof Place) {
                            Place place = (Place) vertex;
                            for (Transition placeTransition : place.getOutTransitions()) {
                                if (placeTransition.getOutPlaces().contains(endPlace)) {
                                    transitionLookup.put(place, placeTransition);
                                    break;
                                }
                            }
                        }
                    }
                }

                // Find any nominal places that should be connected to the end place but are not
                ArrayList<Place> unhandledPlaces = new ArrayList<Place>();
                for (Vertex vertex : missionPlanSpecification.getGraph().getVertices()) {
                    if (vertex instanceof Place
                            && !((Place) vertex).isStart()
                            && !((Place) vertex).isEnd()
                            && ((Place) vertex).getFunctionMode() == FunctionMode.Nominal
                            && !transitionLookup.containsKey((Place) vertex)) {
                        unhandledPlaces.add((Place) vertex);
                    }
                }
                for (Place place : unhandledPlaces) {
                    // Create transition with ProxyAbortMissionReceived
                    Transition newTransition = new Transition(VERTEX_NAME, FunctionMode.Recovery);
                    missionPlanSpecification.getGraph().addVertex(newTransition);
                    Point freePoint = getVertexPoint(missionPlanSpecification.getLocations(), true);
                    missionPlanSpecification.getLocations().put(newTransition, freePoint);
                    // Add ProxyAbortMissionReceived to transition
                    ReflectedEventSpecification proxyAbortReceivedSpec = new ReflectedEventSpecification(ProxyAbortMissionReceived.class.getName());
                    missionPlanSpecification.updateEventSpecList(newTransition, proxyAbortReceivedSpec);
                    newTransition.addEventSpec(proxyAbortReceivedSpec);
                    transitionLookup.put(place, newTransition);

                    // Add edge from nominal place to created transition with Proxy token spec
                    InEdge inEdge = new InEdge(place, newTransition, FunctionMode.Recovery);
                    inEdge.addTokenRequirement(relProxyTokenReq);
                    place.addOutEdge(inEdge);
                    newTransition.addInEdge(inEdge);
                    missionPlanSpecification.getGraph().addEdge(inEdge, place, newTransition);
                    place.addOutTransition(newTransition);
                    (newTransition).addInPlace(place);

                    // Add edge from created transition to end place
                    OutEdge outEdge = new OutEdge(newTransition, endPlace, FunctionMode.Recovery);
                    outEdge.addTokenRequirement(takeNoneTokenReq);
                    newTransition.addOutEdge(outEdge);
                    endPlace.addInEdge(outEdge);
                    missionPlanSpecification.getGraph().addEdge(outEdge, newTransition, endPlace);
                    newTransition.addOutPlace(endPlace);
                    endPlace.addInTransition(newTransition);
                }

                // Disconnect any places that should not be connected any longer
                ArrayList<Transition> transitionsToRemove = new ArrayList<Transition>();
                for (Place place : transitionLookup.keySet()) {
                    if (place.isStart()
                            || place.isEnd()
                            || place.getFunctionMode() == FunctionMode.HiddenRecovery
                            || place.getFunctionMode() == FunctionMode.Recovery) {
                        transitionsToRemove.add(transitionLookup.get(place));
                    }
                }
                for (Transition transition : transitionsToRemove) {
                    missionPlanSpecification.removeTransition(transition);
                }
            } else {
                if (endPlace != null) {
                    LOGGER.severe("ProxyAbortMissionHelper detects unnecessary connections");
                    //@todo Disconnect any places that should not be connected any longer
                }
            }
        }
    }
}
