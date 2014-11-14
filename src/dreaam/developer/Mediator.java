package dreaam.developer;

import dreaam.agent.Platform;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import sami.mission.ProjectSpecification;

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

    public void newSpec() {
        instance.newSpec();
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

    private static class _Mediator {

        ProjectSpecification projectSpec = null;
        private File projectSpecLocation = null;
        private Platform platform = new Platform();
        // Variable name to number of references (for garbage collection)
        private HashMap<String, Integer> varToRefCount = new HashMap<String, Integer>();

        private ProjectSpecification getProjectSpec() {

            if (projectSpec == null) {
                projectSpec = new ProjectSpecification();
            }

            return projectSpec;
        }

        private void newSpec() {
            if (projectSpec != null && projectSpec.needsSaving()) {
                int answer = JOptionPane.showOptionDialog(null, "Save current specification?", "Save first?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                if (answer == JOptionPane.YES_OPTION) {
                    save();
                }
            }
            projectSpec = new ProjectSpecification();
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
                LOGGER.info("Writing projectSpec with " + projectSpec.getAllMissionPlans());
                projectSpec.saved();
                LOGGER.info("Saved: " + projectSpec);

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
            } catch (FileNotFoundException ex) {
                LOGGER.severe("Exception in DRM open - DRM file not found");
            } catch (InvalidClassException ex) {
                LOGGER.severe("Exception in DRM open - DRM version mismatch");
            } catch (SecurityException ex) {
                LOGGER.severe("Exception in DRM open - error in JDK SHA implementation");
            } catch (Exception ex) {
                LOGGER.severe("Exception in DRM open: " + ex.getLocalizedMessage());
            }

            return false;
        }
    }
}
