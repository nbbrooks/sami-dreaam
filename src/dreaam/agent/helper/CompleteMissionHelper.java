package dreaam.agent.helper;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Hashtable;
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
            // Fetch transition that handles CompleteMissionReceived events
            Transition endTransition = missionToTransition.get(missionPlanSpecification);
            if (endTransition == null) {
                // First time running helper since creating and/or loading this mission, try to find helper's end place
                for (Vertex vertex : missionPlanSpecification.getGraph().getVertices()) {
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
                missionPlanSpecification.getGraph().addVertex(endTransition);
                Point freePoint = getVertexPoint(missionPlanSpecification.getLocations(), true);
                missionPlanSpecification.getLocations().put(endTransition, freePoint);
                // Add CompleteMissionReceived to transition
                ReflectedEventSpecification eventSpec = new ReflectedEventSpecification(CompleteMissionReceived.class.getName());
                missionPlanSpecification.updateEventSpecList(endTransition, eventSpec);
                endTransition.addEventSpec(eventSpec);
                missionToTransition.put(missionPlanSpecification, endTransition);
                createdTransition = true;
            }

            // Fetch transition that handles CompleteMissionReceived events
            Place endPlace = missionToPlace.get(missionPlanSpecification);
            if (endPlace == null) {
                // First time running helper since creating and/or loading this mission, try to find helper's end place
                for (Vertex vertex : missionPlanSpecification.getGraph().getVertices()) {
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
                missionPlanSpecification.getGraph().addVertex(endPlace);
                Point freePoint2 = getVertexPoint(missionPlanSpecification.getLocations(), false);
                missionPlanSpecification.getLocations().put(endPlace, freePoint2);
//                // Add proxy CompleteMission event to place
//                ReflectedEventSpecification eventSpec2 = new ReflectedEventSpecification(CompleteMission.class.getName());
//                missionPlanSpecification.updateEventSpecList(endPlace, eventSpec2);
//                endPlace.addEventSpec(eventSpec2);
                createdPlace = true;
            }

            // Fetch edge
            Edge endEdge = missionPlanSpecification.getGraph().findEdge(endTransition, endPlace);
            if (endEdge == null) {
                // Add edge
                OutEdge edge = new OutEdge(endTransition, endPlace, FunctionMode.Recovery, Mediator.getInstance().getProject().getAndIncLastElementId());
                edge.addTokenRequirement(takeAllTokenReq);
                endTransition.addOutEdge(edge);
                endPlace.addInEdge(edge);
                missionPlanSpecification.getGraph().addEdge(edge, endTransition, endPlace);
            }

            // Make any additional connections needed
            if (createdTransition || createdPlace) {
                endTransition.addOutPlace(endPlace);
                endPlace.addInTransition(endTransition);
            }

            // Fetch list of places that are connected to the transition
            ArrayList<Place> connectedPlaces = endTransition.getInPlaces();
            // Connect any places that should be connected to the transition but are not
            for (Vertex vertex : missionPlanSpecification.getGraph().getVertices()) {
                if (vertex instanceof Place
                        && !connectedPlaces.contains((Place) vertex)
                        && !((Place) vertex).isEnd()
                        && ((Place) vertex).getFunctionMode() == FunctionMode.Nominal) {
                    // Add edge from place to transition with Proxy token spec
                    Place place = (Place) vertex;
                    InEdge newEdge = new InEdge(place, endTransition, FunctionMode.Recovery, Mediator.getInstance().getProject().getAndIncLastElementId());
                    newEdge.addTokenRequirement(noReqTokenReq);
                    place.addOutEdge(newEdge);
                    endTransition.addInEdge(newEdge);
                    missionPlanSpecification.getGraph().addEdge(newEdge, place, endTransition);
                    place.addOutTransition(endTransition);
                    endTransition.addInPlace(place);
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
                Edge edge = missionPlanSpecification.getGraph().findEdge(place, endTransition);
                missionPlanSpecification.removeEdge(edge);
            }
        }
    }
}
