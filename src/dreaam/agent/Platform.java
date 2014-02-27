package dreaam.agent;

import dreaam.agent.checker.StartPlaceChecker;
import dreaam.agent.checker.RequirementChecker;
import dreaam.agent.checker.ReachabilityChecker;
import dreaam.agent.helper.HelperAgent;
import dreaam.agent.helper.AbortMissionHelper;
import dreaam.agent.helper.ProxyAbortMissionHelper;
import dreaam.agent.checker.ProxyAbortMissionChecker;
import dreaam.agent.checker.CheckerAgent;
import dreaam.agent.checker.AbortMissionChecker;
import sami.config.DomainConfigManager;
import sami.config.DomainConfig.LeafNode;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.regex.Pattern;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author pscerri
 */
public class Platform {

    static private _Platform platform = new _Platform();

    static public class _Platform {

        ArrayList<CheckerAgent> checkerAgents = new ArrayList<CheckerAgent>();
        ArrayList<HelperAgent> helperAgents = new ArrayList<HelperAgent>();

        public _Platform() {
            // @todo load domain agents
            checkerAgents.add(new RequirementChecker());
            checkerAgents.add(new StartPlaceChecker());
            checkerAgents.add(new ReachabilityChecker());
            checkerAgents.add(new AbortMissionChecker());
            checkerAgents.add(new ProxyAbortMissionChecker());
            helperAgents.add(new AbortMissionHelper());
            helperAgents.add(new ProxyAbortMissionHelper());
//            agents.add(new GUIComponentAnalysisAgent());
            // Load domain specific agents
            try {
                DefaultMutableTreeNode agentsTree = (DefaultMutableTreeNode) DomainConfigManager.getInstance().domainConfiguration.agentTree.clone();
                Enumeration e = agentsTree.preorderEnumeration();
                while (e.hasMoreElements()) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
                    if (node instanceof LeafNode) {
                        LeafNode leafNode = (LeafNode) node;
                        Class agentClass = Class.forName(leafNode.className);
                        Object agentInstance = agentClass.getConstructor(new Class[]{}).newInstance();
                        if (agentInstance instanceof CheckerAgent) {
                            checkerAgents.add((CheckerAgent) agentInstance);
                        }
                        if (agentInstance instanceof HelperAgent) {
                            helperAgents.add((HelperAgent) agentInstance);
                        }
                    }
                }
            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            } catch (InstantiationException ie) {
                ie.printStackTrace();
            } catch (IllegalAccessException iae) {
                iae.printStackTrace();
            } catch (NoSuchMethodException nsme) {
                nsme.printStackTrace();
            } catch (InvocationTargetException ite) {
                ite.printStackTrace();
                System.out.println(ite.getCause());
            }
            for (HelperAgent helper : helperAgents) {
                helper.setEnabled(true);
            }
            for (CheckerAgent checker : checkerAgents) {
                checker.setEnabled(true);
            }
        }

        private ArrayList<CheckerAgent.AgentMessage> getMessages() {
            ArrayList<CheckerAgent.AgentMessage> msgs = new ArrayList<CheckerAgent.AgentMessage>();
            for (CheckerAgent agent : checkerAgents) {
                ArrayList<CheckerAgent.AgentMessage> ms = agent.getMessages();
                if (ms != null) {
                    for (CheckerAgent.AgentMessage m : ms) {
                        msgs.add(m);
                    }
                }
            }

            return msgs;
        }

        private void runHelperAgents() {
            for (HelperAgent helper : helperAgents) {
                helper.run();
            }
        }
    }

    public ArrayList<CheckerAgent> getCheckerAgents() {
        return platform.checkerAgents;
    }

    public ArrayList<HelperAgent> getHelperAgents() {
        return platform.helperAgents;
    }

    public ArrayList<CheckerAgent.AgentMessage> getMessages() {
        return platform.getMessages();
    }

    public void runHelperAgents() {
        platform.runHelperAgents();
    }

    private static HashMap<String, String> loadAgents(String uiF) {
        HashMap<String, String> uiElements = new HashMap<String, String>();
        String uiClass, uiDescription;
        Pattern pattern = Pattern.compile("\"[A-Za-z0-9\\.]+\"\\s+\"[^\r\n\"]*\"\\s*");
        try {
            FileInputStream fstream = new FileInputStream(uiF);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.matches(pattern.toString())) {
                    line = line.trim();
                    String[] pairing = splitOnString(line, "\"");
                    if (pairing.length == 4) {
                        uiClass = pairing[1];
                        uiDescription = pairing[3];
                        uiElements.put(uiClass, uiDescription);
                    }
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return uiElements;
    }

    private static String[] splitOnString(String string, String split) {
        ArrayList<String> list = new ArrayList<String>();
        int startIndex = 0;
        int endIndex = string.indexOf(split, startIndex);
        while (endIndex != -1) {
            list.add(string.substring(startIndex, endIndex));
            startIndex = endIndex + 1;
            endIndex = string.indexOf(split, startIndex);
        }
        String[] ret = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            ret[i] = list.get(i);
        }
        return ret;
    }
}
