package dreaam.agent.checker;

import sami.event.AbortMissionReceived;
import sami.event.InputEvent;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Transition;
import sami.mission.Vertex;
import java.util.ArrayList;

/**
 *
 * @author pscerri
 */
public class AbortMissionChecker extends CheckerAgent {

    public AbortMissionChecker() {
    }

    @Override
    public ArrayList<AgentMessage> getMessages() {
        boolean hasAbortMissionTransition;
        ArrayList<AgentMessage> msgs = new ArrayList<AgentMessage>();
        // Check that each place has a transition with a AbortMissionReceived trigger
        for (MissionPlanSpecification missionPlanSpecification : mediator.getProject().getAllMissionPlans()) {
            for (Vertex v : missionPlanSpecification.getGraph().getVertices()) {
                if (v instanceof Place && !((Place) v).isEnd()) {
                    hasAbortMissionTransition = false;
                    for (Transition t : ((Place) v).getOutTransitions()) {
                        for (InputEvent ie : t.getInputEvents()) {
                            if (ie instanceof AbortMissionReceived) {
                                hasAbortMissionTransition = true;
                            }
                        }
                    }
                    if (!hasAbortMissionTransition) {
                        Object[] o = new Object[1];
                        o[0] = (Place) v;
                        AgentMessage m = new AgentMessage(this, "Missing transition handling Abort Mission!", o);
                        msgs.add(m);
                    }
                }
            }
        }
        return msgs;
    }
}
