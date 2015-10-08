package dreaam.agent.checker;

import dreaam.agent.checker.CheckerAgent;
import sami.event.OutputEvent;
import sami.gui.GuiElementSpec;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Vertex;
import dreaam.agent.checker.CheckerAgent.AgentMessage;
import java.util.ArrayList;

/**
 *
 * @author pscerri
 */
public class GuiComponentChecker extends CheckerAgent {

    @Override
    public ArrayList<AgentMessage> getMessages() {
        ArrayList<AgentMessage> msgs = new ArrayList<AgentMessage>();

        boolean hasInformationMessage = false;
        for (MissionPlanSpecification missionPlanSpecification : mediator.getProject().getAllMissionPlans()) {
            for (Vertex vertex : missionPlanSpecification.getTransientGraph().getVertices()) {
                if (vertex instanceof Place) {
                    for (OutputEvent outputEvent : ((Place) vertex).getOutputEvents()) {
//                        if (outputEvent instanceof ) {
//                            hasInformationMessage = true;
//                        }
                    }
                }
            }
        }

        if (hasInformationMessage) {
            boolean hasComponentForMessages = false;
            for (GuiElementSpec s : mediator.getProject().getGuiElements()) {
                if (s.getElementName().equalsIgnoreCase("Alert Window")) {
                    hasComponentForMessages = true;
                }
            }

            if (!hasComponentForMessages) {
                msgs.add(new AgentMessage(this, "Missions output information messages, but there is no GUI component to display them", new Object[]{}));
            }
        }

        return msgs;
    }
}
