package dreaam.agent.checker;

import sami.mission.RequirementSpecification;
import java.util.ArrayList;
import sami.engine.Mediator;

/**
 *
 * @author pscerri
 */
public class RequirementChecker extends CheckerAgent {

    public RequirementChecker() {
    }

    @Override
    public ArrayList<AgentMessage> getMessages() {
        ArrayList<AgentMessage> msgs = new ArrayList<AgentMessage>();
        ArrayList<RequirementSpecification> reqs = Mediator.getInstance().getProject().getReqs();
        if (reqs != null) {
            for (RequirementSpecification requirementSpecification : reqs) {
                if (!requirementSpecification.isFilled()) {
                    Object[] os = new Object[1];
                    os[0] = requirementSpecification;
                    msgs.add(new AgentMessage(this, "Unfilled requirement", os));
                }
            }

            return msgs;
        } else {
            return null;
        }
    }
}
