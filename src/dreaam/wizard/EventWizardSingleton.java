package dreaam.wizard;

import dreaam.wizard.EventWizardInt.RequiredInRequirement;
import dreaam.wizard.EventWizardInt.RequiredOutRequirement;
import dreaam.wizard.EventWizardInt.RequiredTokenType;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.Point;
import java.util.ArrayList;
import java.util.logging.Logger;
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

/**
 *
 * @author nbb
 */
public class EventWizardSingleton {

    private final static Logger LOGGER = Logger.getLogger(EventWizardSingleton.class.getName());
    private static EventWizardSingleton instance = null;
    protected ArrayList<EventWizardInt> wizards = new ArrayList<EventWizardInt>();

    private static class EventWizardGeneratorHolder {

        public static final EventWizardSingleton INSTANCE = new EventWizardSingleton();
    }

    public static EventWizardSingleton getInstance() {
        return EventWizardGeneratorHolder.INSTANCE;
    }

    public EventWizardSingleton() {
        try {
//            ArrayList<String> list = (ArrayList<String>) DomainConfigManager.getInstance().getDomainConfiguration().fromUiMessageGeneratorList.clone();
//            for (String className : list) {
//                Class uiClass = Class.forName(className);
            Class wizardClass = Class.forName("crw.wizard.CrwEventWizard");
            EventWizardInt newWizardObject = (EventWizardInt) wizardClass.newInstance();
            wizards.add(newWizardObject);
//            }
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        } catch (InstantiationException ie) {
            ie.printStackTrace();
        }
    }

    public boolean runWizard(String eventClassname, MissionPlanSpecification mSpec, Place p1, Graph<Vertex, Edge> dsgGraph, AbstractLayout<Vertex, Edge> layout, VisualizationViewer<Vertex, Edge> vv) {
        boolean handled = false;
        for (EventWizardInt wizard : wizards) {
            handled = wizard.runWizard(eventClassname, mSpec, p1, dsgGraph, layout, vv) ? true : handled;
        }
        if (handled) {
            computeRequirements(mSpec);
        }
        return handled;
    }

    public boolean runWizard(String eventClassname, MissionPlanSpecification mSpec, Point graphPoint, Graph<Vertex, Edge> dsgGraph, AbstractLayout<Vertex, Edge> layout, VisualizationViewer<Vertex, Edge> vv) {
        boolean handled = false;
        for (EventWizardInt wizard : wizards) {
            handled = wizard.runWizard(eventClassname, mSpec, graphPoint, dsgGraph, layout, vv) ? true : handled;
        }
        if (handled) {
            computeRequirements(mSpec);
        }
        return handled;
    }

    public void computeRequirements(MissionPlanSpecification mSpec) {
        boolean usesProxyTokens = usesProxies(mSpec);
        boolean usesTaskTokens = !mSpec.getTaskSpecList().isEmpty();

        for (Edge edge : mSpec.getTransientGraph().getEdges()) {
            // If the edge doesn't have any locked requirements, clear and repopulate all the requirements
            if (edge instanceof InEdge) {
                InEdge inEdge = (InEdge) edge;
                if (inEdge.getLockedTokenRequirements().isEmpty()) {
                    // Clear out old computed token requirements
                    inEdge.clearTokenRequirements();
                    // Re calculate token requirements
                    ArrayList<InTokenRequirement> inReqs = getInReqs(inEdge.getEnd(), usesProxyTokens, usesTaskTokens);
                    for (InTokenRequirement itr : inReqs) {
                        inEdge.addTokenRequirement(itr);
                    }
                }
            } else if (edge instanceof OutEdge) {
                OutEdge outEdge = (OutEdge) edge;
                if (outEdge.getLockedTokenRequirements().isEmpty()) {
                    // Clear out old computed token requirements
                    outEdge.clearTokenRequirements();
                    // Re calculate and add token requirements
                    ArrayList<OutTokenRequirement> outReqs = getOutReqs(outEdge.getStart(), usesProxyTokens, usesTaskTokens);
                    for (OutTokenRequirement otr : outReqs) {
                        outEdge.addTokenRequirement(otr);
                    }
                }
            }
        }
    }

    protected boolean usesProxies(MissionPlanSpecification mSpec) {
        for (Vertex vertex : mSpec.getTransientGraph().getVertices()) {
            if (vertex.getFunctionMode() != Vertex.FunctionMode.Nominal) {
                continue;
            }
            for (ReflectedEventSpecification res : vertex.getEventSpecs()) {
                for (EventWizardInt wizard : wizards) {
                    if (vertex instanceof Place) {
                        RequiredTokenType rtt = wizard.getOeTokenNeeded(res.getClassName());
                        if (rtt != null && rtt == RequiredTokenType.Proxy) {
                            return true;
                        }
                    } else if (vertex instanceof Transition) {
                        RequiredOutRequirement ror = wizard.getIeMinOutReq(res.getClassName());
                        if (ror != null && (ror == RequiredOutRequirement.AddRt || ror == RequiredOutRequirement.TakeRt)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected ArrayList<InTokenRequirement> getInReqs(Transition transition, boolean usesProxyTokens, boolean usesTaskTokens) {
        ArrayList<InTokenRequirement> reqs = new ArrayList<InTokenRequirement>();

        // Generic
        reqs.add(min1G);

        // RT
        boolean hasRt = false;
        for (ReflectedEventSpecification res : transition.getEventSpecs()) {
            for (EventWizardInt wizard : wizards) {
                RequiredInRequirement rir = wizard.getIeMinInReq(res.getClassName());
                if (rir != null) {
                    if (!hasRt && rir == RequiredInRequirement.HasRt) {
                        hasRt = true;
                        reqs.add(hasAllRt);
                    }
                }
            }
        }
        return reqs;
    }

    protected ArrayList<OutTokenRequirement> getOutReqs(Transition transition, boolean usesProxyTokens, boolean usesTaskTokens) {
        ArrayList<OutTokenRequirement> reqs = new ArrayList<OutTokenRequirement>();

        // Generic
        reqs.add(take1G);

        // RT / Proxy / Task
        boolean addRt = false, takeRt = false;
        for (ReflectedEventSpecification res : transition.getEventSpecs()) {
            for (EventWizardInt wizard : wizards) {
                RequiredOutRequirement ror = wizard.getIeMinOutReq(res.getClassName());
                if (ror != null) {
                    if (!addRt && ror == RequiredOutRequirement.AddRt) {
                        addRt = true;
                        reqs.add(addAllRt);
                    } else if (!takeRt && ror == RequiredOutRequirement.TakeRt) {
                        takeRt = true;
                        reqs.add(takeAllRt);
                    }
                }
            }
        }
        if (usesProxyTokens && !addRt && !takeRt) {
            reqs.add(takeAllP);
        }
        if (usesTaskTokens && !addRt && !takeRt) {
            reqs.add(takeAllT);
        }
        return reqs;
    }

    private static final InTokenRequirement noReq = new InTokenRequirement(TokenRequirement.MatchCriteria.None, null);
    private static final InTokenRequirement hasAllRt = new InTokenRequirement(TokenRequirement.MatchCriteria.RelevantToken, TokenRequirement.MatchQuantity.All);
    private static final InTokenRequirement noProxies = new InTokenRequirement(TokenRequirement.MatchCriteria.AnyProxy, TokenRequirement.MatchQuantity.None);
    private static final InTokenRequirement min1G = new InTokenRequirement(TokenRequirement.MatchCriteria.Generic, TokenRequirement.MatchQuantity.GreaterThanEqualTo, 1);

    private static final OutTokenRequirement addAllRt = new OutTokenRequirement(TokenRequirement.MatchCriteria.RelevantToken, TokenRequirement.MatchQuantity.All, TokenRequirement.MatchAction.Add);
    private static final OutTokenRequirement takeAllRt = new OutTokenRequirement(TokenRequirement.MatchCriteria.RelevantToken, TokenRequirement.MatchQuantity.All, TokenRequirement.MatchAction.Take);
    private static final OutTokenRequirement add1G = new OutTokenRequirement(TokenRequirement.MatchCriteria.Generic, TokenRequirement.MatchQuantity.Number, TokenRequirement.MatchAction.Add, 1);
    private static final OutTokenRequirement con1G = new OutTokenRequirement(TokenRequirement.MatchCriteria.Generic, TokenRequirement.MatchQuantity.Number, TokenRequirement.MatchAction.Consume, 1);
    private static final OutTokenRequirement takeAllP = new OutTokenRequirement(TokenRequirement.MatchCriteria.AnyProxy, TokenRequirement.MatchQuantity.All, TokenRequirement.MatchAction.Take);
    private static final OutTokenRequirement takeAllT = new OutTokenRequirement(TokenRequirement.MatchCriteria.AnyTask, TokenRequirement.MatchQuantity.All, TokenRequirement.MatchAction.Take);
    private static final OutTokenRequirement take1G = new OutTokenRequirement(TokenRequirement.MatchCriteria.Generic, TokenRequirement.MatchQuantity.Number, TokenRequirement.MatchAction.Take, 1);

}
