package dreaam.agent.helper;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
import sami.DreaamHelper;
import sami.engine.Mediator;
import sami.event.ProxyAbortMissionReceived;
import sami.event.ReflectedEventSpecification;
import sami.event.SendAbortMission;
import sami.mission.Edge;
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

        for (MissionPlanSpecification missionPlanSpecification : mediator.getProject().getAllMissionPlans()) {
            // Create VisualizationViewer for intelligently placing vertices
            AbstractLayout layout = new StaticLayout<Vertex, Edge>(missionPlanSpecification.getTransientGraph(), new Dimension(600, 600));
            VisualizationViewer vv = new VisualizationViewer<Vertex, Edge>(layout);

            // First check that we actually need an end place (ie have a place that is neither a start nor a stop place)
            boolean needEndPlace = false;
            for (Vertex vertex : missionPlanSpecification.getTransientGraph().getVertices()) {
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
                for (Vertex vertex : missionPlanSpecification.getTransientGraph().getVertices()) {
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
                    endPlace = new Place(VERTEX_NAME, FunctionMode.Recovery, Mediator.getInstance().getProject().getAndIncLastElementId());
                    endPlace.setIsEnd(true);
                    endPlace.setIsSharedSmEnd(true);
                    Point freePoint = getVertexPoint(missionPlanSpecification.getLocations(), false);
                    freePoint = DreaamHelper.getVertexFreePoint(vv, freePoint.x, freePoint.y);
                    missionPlanSpecification.addPlace(endPlace, freePoint);
                    layout.setLocation(endPlace, freePoint);
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
                    for (Vertex vertex : missionPlanSpecification.getTransientGraph().getVertices()) {
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
                for (Vertex vertex : missionPlanSpecification.getTransientGraph().getVertices()) {
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
                    Transition newTransition = new Transition(VERTEX_NAME, FunctionMode.Recovery, Mediator.getInstance().getProject().getAndIncLastElementId());
                    Point freePoint = getVertexPoint(missionPlanSpecification.getLocations(), true);
                    freePoint = DreaamHelper.getVertexFreePoint(vv, freePoint.x, freePoint.y);
                    missionPlanSpecification.addTransition(newTransition, freePoint);
                    layout.setLocation(newTransition, freePoint);
                    // Add ProxyAbortMissionReceived to transition
                    ReflectedEventSpecification proxyAbortReceivedSpec = new ReflectedEventSpecification(ProxyAbortMissionReceived.class.getName());
                    missionPlanSpecification.updateEventSpecList(newTransition, proxyAbortReceivedSpec);
                    newTransition.addEventSpec(proxyAbortReceivedSpec);
                    transitionLookup.put(place, newTransition);

                    // Add edge from nominal place to created transition with Proxy token spec
                    InEdge inEdge = new InEdge(place, newTransition, FunctionMode.Recovery, Mediator.getInstance().getProject().getAndIncLastElementId());
                    inEdge.addTokenRequirement(relProxyTokenReq);
                    missionPlanSpecification.addEdge(inEdge, place, newTransition);

                    // Add edge from created transition to end place
                    OutEdge outEdge = new OutEdge(newTransition, endPlace, FunctionMode.Recovery, Mediator.getInstance().getProject().getAndIncLastElementId());
                    outEdge.addTokenRequirement(takeNoneTokenReq);
                    missionPlanSpecification.addEdge(outEdge, newTransition, endPlace);
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
