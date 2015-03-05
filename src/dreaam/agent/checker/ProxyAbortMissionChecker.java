package dreaam.agent.checker;

import sami.event.InputEvent;
import sami.event.ProxyAbortMissionReceived;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Transition;
import sami.mission.Vertex;
import java.util.ArrayList;

/**
 *
 * @author pscerri
 */
public class ProxyAbortMissionChecker extends CheckerAgent {

    public ProxyAbortMissionChecker() {
    }

    @Override
    public ArrayList<AgentMessage> getMessages() {
        boolean hasProxyAbortMissionTransition;
        ArrayList<AgentMessage> msgs = new ArrayList<AgentMessage>();
        // Check that each place has a transition with a ProxyAbortMissionReceived trigger
        for (MissionPlanSpecification missionPlanSpecification : mediator.getProject().getAllMissionPlans()) {
            for (Vertex v : missionPlanSpecification.getGraph().getVertices()) {
                if (v instanceof Place && !((Place) v).isEnd()) {
                    hasProxyAbortMissionTransition = false;
                    for (Transition t : ((Place) v).getOutTransitions()) {
                        for (InputEvent ie : t.getInputEvents()) {
                            if (ie instanceof ProxyAbortMissionReceived) {
                                hasProxyAbortMissionTransition = true;
                            }
                        }
                    }
                    if (!hasProxyAbortMissionTransition) {
                        Object[] o = new Object[1];
                        o[0] = (Place) v;
                        AgentMessage m = new AgentMessage(this, "Missing transition handling Proxy Abort Mission!", o);
                        msgs.add(m);
                    }
                }
            }
        }
        return msgs;
    }
}
