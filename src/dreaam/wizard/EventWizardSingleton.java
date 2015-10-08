package dreaam.wizard;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.Point;
import java.util.ArrayList;
import java.util.logging.Logger;
import sami.mission.Edge;
import sami.mission.MissionPlanSpecification;
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

    public boolean runWizard(String eventClassname, MissionPlanSpecification mSpec, Point graphPoint, Graph<Vertex, Edge> dsgGraph, AbstractLayout<Vertex, Edge> layout, VisualizationViewer<Vertex, Edge> vv) {
        boolean handled = false;
        for (EventWizardInt wizard : wizards) {
            handled = wizard.runWizard(eventClassname, mSpec, graphPoint, dsgGraph, layout, vv) ? true : handled;
        }
        return handled;
    }
}
