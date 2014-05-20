package dreaam.developer;

import dreaam.agent.Platform;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import sami.event.Event;
import sami.event.ReflectedEventSpecification;
import sami.gui.GuiElementSpec;
import sami.mission.MissionPlanSpecification;
import sami.mission.ProjectSpecification;
import sami.mission.RequirementSpecification;
import sami.mission.Transition;
import sami.mission.Vertex;

/**
 *
 * @author pscerri
 */
public class Mediator {

    private static final Logger LOGGER = Logger.getLogger(Mediator.class.getName());
    private static _Mediator instance = new _Mediator();

    public ProjectSpecification getProjectSpec() {
        return instance.getProjectSpec();
    }

    public boolean open() {
        return instance.open();
    }

    public boolean open(File file) {
        return instance.open(file);
    }

    public void save() {
        instance.save();
    }

    public void saveAs() {
        instance.saveAs();
    }

    public void setGUI(ArrayList<GuiElementSpec> elements) {
        instance.setGUI(elements);
    }

    public void newSpec() {
        instance.newSpec();
    }

    public ArrayList<MissionPlanSpecification> getMissions() {
        return instance.getProjectSpec().getMissionPlans();
    }

    public ArrayList<GuiElementSpec> getGuiSpecs() {
        return instance.getGuiSpecs();
    }

    void setRequirements(ArrayList<RequirementSpecification> reqs) {
        instance.setRequirements(reqs);
    }

    public ArrayList<RequirementSpecification> getReqs() {
        return instance.getReqs();
    }

    public void remove(MissionPlanSpecification mps) {
        instance.remove(mps);
    }

    public ArrayList<String> getAllVariables() {
        return instance.getAllVariables();
    }

    private static class _Mediator {

        ProjectSpecification projectSpec = null;
        private File projectSpecLocation = null;
        private Platform platform = new Platform();

        private ProjectSpecification getProjectSpec() {

            if (projectSpec == null) {
                projectSpec = new ProjectSpecification();
            }

            return projectSpec;
        }

        private void save() {
            if (projectSpecLocation == null) {
                saveAs();
                if (projectSpecLocation == null) {
                    return;
                }
            }
            ObjectOutputStream oos;
            try {
                /*
                 * taskModelEditor.writeModel();
                 * specification.addMissionPlan(taskModelEditor.getModel());
                 *
                 */
                oos = new ObjectOutputStream(new FileOutputStream(projectSpecLocation));
                oos.writeObject(projectSpec);
                System.out.println("Writing projectSpec with " + projectSpec.getMissionPlans());
                projectSpec.saved();
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Saved: " + projectSpec);

                // Update last DRM file
                Preferences p = Preferences.userRoot();
                try {
                    p.put(DREAAM.LAST_DRM_FILE, projectSpecLocation.getAbsolutePath());
                    p.put(DREAAM.LAST_DRM_FOLDER, projectSpecLocation.getParent());
                } catch (AccessControlException e) {
                    LOGGER.severe("Failed to save preferences");
                }
            } catch (IOException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void saveAs() {
            Preferences p = Preferences.userRoot();
            String folder = p.get(DREAAM.LAST_DRM_FOLDER, "");
            JFileChooser chooser = new JFileChooser(folder);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("DREAAM specification files", "drm");
            chooser.setFileFilter(filter);
            int ret = chooser.showSaveDialog(null);
            if (ret == JFileChooser.APPROVE_OPTION) {
                if (chooser.getSelectedFile().getName().endsWith(".drm")) {
                    projectSpecLocation = chooser.getSelectedFile();
                } else {
                    projectSpecLocation = new File(chooser.getSelectedFile().getAbsolutePath() + ".drm");
                }
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Saving as: " + projectSpecLocation.toString());
                save();
            }
        }

        public boolean open() {
            Preferences p = Preferences.userRoot();
            String folder = p.get(DREAAM.LAST_DRM_FOLDER, "");
            JFileChooser chooser = new JFileChooser(folder);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("DREAAM specification files", "drm");
            chooser.setFileFilter(filter);
            int ret = chooser.showOpenDialog(null);
            if (ret == JFileChooser.APPROVE_OPTION) {
                projectSpecLocation = chooser.getSelectedFile();
                return open(projectSpecLocation);
            }
            return false;
        }

        public boolean open(File location) {
            if (location == null) {
                return false;
            }
            try {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Reading: " + location.toString());
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(location));
                projectSpec = (ProjectSpecification) ois.readObject();

                if (projectSpec == null) {
                    return false;
                } else {
                    projectSpecLocation = location;
                    Preferences p = Preferences.userRoot();
                    try {
                        p.put(DREAAM.LAST_DRM_FILE, location.getAbsolutePath());
                        p.put(DREAAM.LAST_DRM_FOLDER, location.getParent());
                    } catch (AccessControlException e) {
                        LOGGER.severe("Failed to save preferences");
                    }
                    return true;
                }
            } catch (Exception ex) {
                LOGGER.severe("Exception in DRM open - incompatible file");
            }

            return false;
        }

        private void newSpec() {
            if (projectSpec != null && projectSpec.needsSaving()) {
                int answer = JOptionPane.showOptionDialog(null, "Save current specification?", "Save first?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                if (answer == JOptionPane.YES_OPTION) {
                    save();
                }
            }
            projectSpec = new ProjectSpecification();
            projectSpec.getNewMissionPlanSpecification("Anonymous");
        }

        private void setGUI(ArrayList<GuiElementSpec> elements) {
            projectSpec.setGuiElements(elements);
        }

        private ArrayList<GuiElementSpec> getGuiSpecs() {
            return projectSpec.getGuiElements();
        }

        private void setRequirements(ArrayList<RequirementSpecification> reqs) {
            projectSpec.setReqs(reqs);
        }

        public ArrayList<RequirementSpecification> getReqs() {
            return projectSpec.getReqs();
        }

        private void remove(MissionPlanSpecification mps) {
            projectSpec.removeMissionPlan(mps);
        }

        public ArrayList<String> getAllVariables() {
            ArrayList<String> ret = new ArrayList<String>();

            for (MissionPlanSpecification missionPlanSpecification : projectSpec.getMissionPlans()) {
                if (missionPlanSpecification.getGraph() != null && missionPlanSpecification.getGraph().getEdges() != null) {
                    for (Vertex v : missionPlanSpecification.getGraph().getVertices()) {
                        if (v instanceof Transition) {
                            if (missionPlanSpecification.getEventSpecList((Transition) v) != null) {
                                for (ReflectedEventSpecification eventSpec : missionPlanSpecification.getEventSpecList((Transition) v)) {
                                    // This is in place of actually working out whether this is an input or output event
                                    if (eventSpec.getFieldDefinitions() != null && eventSpec.getFieldDefinitions().size() > 0) {
                                        for (Object object : eventSpec.getFieldDefinitions().values()) {
                                            if (object instanceof String && ((String) object).startsWith("@") && !((String) object).equals(Event.NONE)) {
                                                ret.add((String) object);
                                                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Adding " + (String) object + " to variable list");
                                            }
                                        }
                                    }
                                }
                            } else {
                                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "No events to check for variables.");
                            }
                        }
                    }
                } else {
                    Logger.getLogger(this.getClass().getName()).log(Level.FINE, "No edges to check for events");
                }
            }

            return ret;
        }
    }
}
