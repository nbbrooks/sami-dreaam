package dreaam.agent.helper;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Hashtable;
import sami.DreaamHelper;
import sami.engine.Mediator;
import sami.event.CompleteMission;
import sami.event.CompleteMissionReceived;
import sami.event.ReflectedEventSpecification;
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
public class CompleteMissionHelper extends HelperAgent {

    final String VERTEX_NAME = "CompleteMissionReceivedHelper";
    Hashtable<MissionPlanSpecification, Transition> missionToTransition = new Hashtable<MissionPlanSpecification, Transition>();
    Hashtable<MissionPlanSpecification, Place> missionToPlace = new Hashtable<MissionPlanSpecification, Place>();

    public CompleteMissionHelper() {
    }

    @Override
    public void run() {
        InTokenRequirement noReqTokenReq = new InTokenRequirement(TokenRequirement.MatchCriteria.None, null);
        OutTokenRequirement takeAllTokenReq = new OutTokenRequirement(TokenRequirement.MatchCriteria.AnyToken, TokenRequirement.MatchQuantity.All, TokenRequirement.MatchAction.Take);
        boolean createdTransition = false, createdPlace = false;

        for (MissionPlanSpecification missionPlanSpecification : mediator.getProject().getAllMissionPlans()) {
            // Create VisualizationViewer for intelligently placing vertices
            AbstractLayout layout = new StaticLayout<Vertex, Edge>(missionPlanSpecification.getTransientGraph(), new Dimension(600, 600));
            VisualizationViewer vv = new VisualizationViewer<Vertex, Edge>(layout);

            // Fetch transition that handles CompleteMissionReceived events
            Transition endTransition = missionToTransition.get(missionPlanSpecification);
            if (endTransition == null) {
                // First time running helper since creating and/or loading this mission, try to find helper's end place
                for (Vertex vertex : missionPlanSpecification.getTransientGraph().getVertices()) {
                    if (vertex instanceof Transition
                            && vertex.getFunctionMode() == FunctionMode.Recovery
                            && vertex.getName().equals(VERTEX_NAME)) {
                        endTransition = (Transition) vertex;
                        missionToTransition.put(missionPlanSpecification, endTransition);
                        break;
                    }
                }
            }
            if (endTransition == null) {
                // First time running helper since creating this mission, construct helper's end transition
                endTransition = new Transition(VERTEX_NAME, FunctionMode.Recovery, Mediator.getInstance().getProject().getAndIncLastElementId());
                missionPlanSpecification.getTransientGraph().addVertex(endTransition);
                Point freePoint = getVertexPoint(missionPlanSpecification.getLocations(), true);
                freePoint = DreaamHelper.getVertexFreePoint(vv, freePoint.x, freePoint.y);
                missionPlanSpecification.addTransition(endTransition, freePoint);
                layout.setLocation(endTransition, freePoint);
                // Add CompleteMissionReceived to transition
                ReflectedEventSpecification cmReceivedSpec = new ReflectedEventSpecification(CompleteMissionReceived.class.getName());
                missionPlanSpecification.updateEventSpecList(endTransition, cmReceivedSpec);
                endTransition.addEventSpec(cmReceivedSpec, true);
                missionToTransition.put(missionPlanSpecification, endTransition);
                createdTransition = true;
            }

            // Fetch transition that handles CompleteMissionReceived events
            Place endPlace = missionToPlace.get(missionPlanSpecification);
            if (endPlace == null) {
                // First time running helper since creating and/or loading this mission, try to find helper's end place
                for (Vertex vertex : missionPlanSpecification.getTransientGraph().getVertices()) {
                    if (vertex instanceof Place
                            && vertex.getFunctionMode() == FunctionMode.Recovery
                            && vertex.getName().equals(VERTEX_NAME)
                            && ((Place) vertex).isEnd()) {
                        endPlace = (Place) vertex;
                        missionToPlace.put(missionPlanSpecification, endPlace);
                        break;
                    }
                }
            }
            if (endPlace == null) {
                // First time running helper since creating this mission, construct helper's end transition
                endPlace = new Place(VERTEX_NAME, FunctionMode.Recovery, Mediator.getInstance().getProject().getAndIncLastElementId());
                endPlace.setIsEnd(true);
                endPlace.setIsSharedSmEnd(true);
                Point freePoint = getVertexPoint(missionPlanSpecification.getLocations(), false);
                freePoint = DreaamHelper.getVertexFreePoint(vv, freePoint.x, freePoint.y);
                missionPlanSpecification.addPlace(endPlace, freePoint);
                layout.setLocation(endPlace, freePoint);
                // Add proxy CompleteMission event to place
                ReflectedEventSpecification cmSpec = new ReflectedEventSpecification(CompleteMission.class.getName());
                missionPlanSpecification.updateEventSpecList(endPlace, cmSpec);
                endPlace.addEventSpec(cmSpec, true);
                createdPlace = true;
            }

            // Fetch edge
            Edge endEdge = missionPlanSpecification.getTransientGraph().findEdge(endTransition, endPlace);
            if (endEdge == null) {
                // Add edge
                OutEdge edge = new OutEdge(endTransition, endPlace, FunctionMode.Recovery, Mediator.getInstance().getProject().getAndIncLastElementId());
                edge.addTokenRequirement(takeAllTokenReq, true);
                missionPlanSpecification.addEdge(edge, endTransition, endPlace);
            }

            // Make any additional connections needed
            if (createdTransition || createdPlace) {
                endTransition.addOutPlace(endPlace);
                endPlace.addInTransition(endTransition);
            }

            // Fetch list of places that are connected to the transition
            ArrayList<Place> connectedPlaces = endTransition.getInPlaces();
            // Connect any places that should be connected to the transition but are not
            for (Vertex vertex : missionPlanSpecification.getTransientGraph().getVertices()) {
                if (vertex instanceof Place
                        && !connectedPlaces.contains((Place) vertex)
                        && !((Place) vertex).isEnd()
                        && ((Place) vertex).getFunctionMode() == FunctionMode.Nominal) {
                    // Add edge from place to transition with Proxy token spec
                    Place place = (Place) vertex;
                    InEdge newEdge = new InEdge(place, endTransition, FunctionMode.Recovery, Mediator.getInstance().getProject().getAndIncLastElementId());
                    newEdge.addTokenRequirement(noReqTokenReq, true);
                    missionPlanSpecification.addEdge(newEdge, place, endTransition);
                }
            }
            // Disconnect any places that should not be connected any longer
            ArrayList<Place> placesToDisconnect = new ArrayList<Place>();
            for (Place place : endTransition.getInPlaces()) {
                if (place.isEnd()) {
                    placesToDisconnect.add(place);
                }
            }
            for (Place place : placesToDisconnect) {
                Edge edge = missionPlanSpecification.getTransientGraph().findEdge(place, endTransition);
                missionPlanSpecification.removeEdge(edge);
            }
        }
    }
}
