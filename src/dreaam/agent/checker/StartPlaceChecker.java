package dreaam.agent.checker;

import dreaam.agent.checker.CheckerAgent;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Vertex;
import java.util.ArrayList;

/**
 *
 * @author pscerri
 */
public class StartPlaceChecker extends CheckerAgent {

    public StartPlaceChecker() {
    }

    @Override
    public ArrayList<AgentMessage> getMessages() {
        ArrayList<AgentMessage> msgs = new ArrayList<AgentMessage>();
        String msg = null;
        for (MissionPlanSpecification missionPlanSpecification : mediator.getMissions()) {
            msg = null;
            Place start = null;
            for (Vertex vertex : missionPlanSpecification.getGraph().getVertices()) {
                if (vertex instanceof Place && ((Place) vertex).isStart()) {
                    if (start != null) {
                        msg = "The graph has multiple start locations, this is illegal.";
                    } else {
                        start = (Place) vertex;
                    }
                }
            }
            if (start == null) {
                msg = "The graph does not have a start location";
            }
            if (msg != null) {
                msgs.add(new AgentMessage(this, msg, new Object[]{missionPlanSpecification}));
            }
            // Graph g = missionPlanSpecification.getGraph();
        }

        return msgs;
    }
}
