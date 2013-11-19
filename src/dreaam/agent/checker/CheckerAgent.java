package dreaam.agent.checker;

import dreaam.developer.Mediator;
import java.util.ArrayList;

/**
 *
 * @author pscerri
 */
public abstract class CheckerAgent {

    private boolean enabled = true;
    public Mediator mediator = new Mediator();

    abstract public ArrayList<AgentMessage> getMessages();

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public class AgentMessage {

        String msg;
        Object[] relevantObjects;
        CheckerAgent agent;

        public AgentMessage(CheckerAgent agent, String msg, Object[] relevantObjects) {
            this.agent = agent;
            this.msg = msg;
            this.relevantObjects = relevantObjects;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(msg + " [for ");

            for (Object object : relevantObjects) {
                sb.append(object + ",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]");

            return sb.toString();
        }
    }
}
