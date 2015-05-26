package dreaam.agent.checker;

import sami.event.InputEvent;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Transition;
import sami.mission.Vertex;
import java.util.ArrayList;
import sami.event.CompleteMissionReceived;

/**
 *
 * @author pscerri
 */
public class CompleteMissionChecker extends CheckerAgent {

    public CompleteMissionChecker() {
    }

    @Override
    public ArrayList<AgentMessage> getMessages() {
        boolean hasCompleteMissionReceivedTransition;
        ArrayList<AgentMessage> msgs = new ArrayList<AgentMessage>();
        // Check that each place has a transition with a CompleteMissionReceived trigger
        for (MissionPlanSpecification missionPlanSpecification : mediator.getProject().getAllMissionPlans()) {
            for (Vertex v : missionPlanSpecification.getGraph().getVertices()) {
                if (v instanceof Place && !((Place) v).isEnd()) {
                    hasCompleteMissionReceivedTransition = false;
                    for (Transition t : ((Place) v).getOutTransitions()) {
                        for (InputEvent ie : t.getInputEvents()) {
                            if (ie instanceof CompleteMissionReceived) {
                                hasCompleteMissionReceivedTransition = true;
                            }
                        }
                    }
                    if (!hasCompleteMissionReceivedTransition) {
                        Object[] o = new Object[1];
                        o[0] = (Place) v;
                        AgentMessage m = new AgentMessage(this, "Missing transition handling CompleteMissionReceived!", o);
                        msgs.add(m);
                    }
                }
            }
        }
        return msgs;
    }
}
