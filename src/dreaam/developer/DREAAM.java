package dreaam.developer;

import dreaam.DreaamHelper;
import dreaam.agent.Platform;
import dreaam.agent.checker.CheckerAgent;
import dreaam.agent.checker.CheckerAgent.AgentMessage;
import dreaam.agent.helper.HelperAgent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.*;
import sami.config.DomainConfigManager;
import sami.engine.Engine;
import sami.environment.EnvironmentProperties;
import sami.mission.MissionPlanSpecification;
import sami.mission.RequirementSpecification;
import sami.mission.Vertex;
import sami.mission.Vertex.FunctionMode;
import static sami.ui.MissionMonitor.LAST_EPF_FILE;
import static sami.ui.MissionMonitor.LAST_EPF_FOLDER;

/**
 *
 * @author pscerri
 */
public class DREAAM extends javax.swing.JFrame {

    private static final Logger LOGGER = Logger.getLogger(DREAAM.class.getName());
    public static final String LAST_PROJECT_NAME = "LAST_PROJECT_NAME";
    public static final String LAST_DRM_FILE = "LAST_DRM_NAME";
    public static final String LAST_DRM_FOLDER = "LAST_DRM_FOLDER";
    TaskModelEditor taskModelEditor = null;
    DefaultMutableTreeNode treeRoot = null;
    DefaultMutableTreeNode missionsRoot = null;
//    DefaultMutableTreeNode agentsRoot = null;
    DefaultMutableTreeNode checkersRoot = null;
    DefaultMutableTreeNode helpersRoot = null;
    DefaultMutableTreeNode errorsRoot = null;
    Mediator mediator = new Mediator();
    ArrayList<CheckerAgent> checkerAgents;
    ArrayList<HelperAgent> helperAgents;
    Platform platform;

    /**
     * Creates new form DREAAM
     */
    public DREAAM() {
        LOGGER.info("java.version: " + System.getProperty("java.version"));
        LOGGER.info("sun.arch.data.model: " + System.getProperty("sun.arch.data.model"));
        LOGGER.info("java.class.path: " + System.getProperty("java.class.path"));
        LOGGER.info("java.library.path: " + System.getProperty("java.library.path"));
        LOGGER.info("java.ext.dirs: " + System.getProperty("java.ext.dirs"));
        LOGGER.info("java.util.logging.config.file: " + System.getProperty("java.util.logging.config.file"));
        LOGGER.info("domainConfiguration:\n" + DomainConfigManager.getInstance().domainConfiguration.toString());
        LOGGER.info("domainConfiguration:\n" + DomainConfigManager.getInstance().domainConfiguration.toVerboseString());

        initComponents();
        platform = new Platform();
        checkerAgents = platform.getCheckerAgents();
        helperAgents = platform.getHelperAgents();

        // Routine GUI configuration
        // Side panel
        jSplitPane1.setDividerLocation(200);
        treeRoot = (DefaultMutableTreeNode) componentT.getModel().getRoot();
        treeRoot.add(mediator.getProjectSpec().getMissionTree());
        checkersRoot = (DefaultMutableTreeNode) componentT.getModel().getChild(treeRoot, 1);
        helpersRoot = (DefaultMutableTreeNode) componentT.getModel().getChild(treeRoot, 2);
        errorsRoot = (DefaultMutableTreeNode) componentT.getModel().getChild(treeRoot, 3);
        componentT.setCellRenderer(new DREAMMTreeCellRenderer());

        // Main panel
        MissionPlanSpecification mSpec = mediator.getProjectSpec().getNewMissionPlanSpecification("Anonymous");
        taskModelEditor = new TaskModelEditor(this, mSpec);
        mainP.setLayout(new BorderLayout());
        mainP.add(taskModelEditor, BorderLayout.CENTER);

        // Add items to side panel
        for (CheckerAgent agent : checkerAgents) {
            checkersRoot.add(new DefaultMutableTreeNode(agent));
        }
        for (HelperAgent agent : helperAgents) {
            helpersRoot.add(new DefaultMutableTreeNode(agent));
        }

        // Set up the mouse handlers
        componentT.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent me) {
                final TreePath treePath = componentT.getPathForLocation(me.getX(), me.getY());
                if (treePath == null) {
                    return;
                }

                Object[] objectPath = treePath.getPath();
                int pathCount = objectPath.length;
                if ((me.isControlDown() || me.isPopupTrigger())) {

                    if (treePath.getLastPathComponent() == missionsRoot) {

                        JPopupMenu menu = new JPopupMenu();
                        menu.add(new AbstractAction("Add new mission") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                addNewRootMissionSpec();
                            }
                        });

                        menu.show(componentT, me.getX(), me.getY());
                    } else if (pathCount > 1
                            && objectPath[pathCount - 2] == missionsRoot
                            && ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject() instanceof MissionPlanSpecification) {
                        // Plan spec under Plays menu
                        JPopupMenu menu = new JPopupMenu();
//                        menu.add(new AbstractAction("Use as Template") {
//                            @Override
//                            public void actionPerformed(ActionEvent ae) {
//                                MissionPlanSpecification mps = (MissionPlanSpecification) ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();
//                                taskModelEditor.addTemplate(mps);
//                            }
//                        });
                        menu.add(new AbstractAction("Rename") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                String result = JOptionPane.showInputDialog(null, "New mission name:");
                                if (result != null) {
                                    MissionPlanSpecification mps = (MissionPlanSpecification) ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();
                                    mps.setName(result);
                                    refreshMissionTree();
                                }
                            }
                        });
                        menu.add(new AbstractAction("Duplicate") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                String result = JOptionPane.showInputDialog(null, "Duplicate mission name:");
                                if (result != null) {
                                    // Ensure entered mission name is unique
                                    ArrayList<String> playNames = new ArrayList<String>();
                                    ArrayList<MissionPlanSpecification> missions = mediator.getProjectSpec().getAllMissionPlans();
                                    for (MissionPlanSpecification mSpec : missions) {
                                        playNames.add(mSpec.getName());
                                    }
                                    result = DreaamHelper.getUniqueName(result, playNames);
                                    MissionPlanSpecification mps = (MissionPlanSpecification) ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();
                                    LOGGER.info("Duplicating " + mps.getName() + " and naming " + result);

                                    // Save current mission
                                    taskModelEditor.writeModel();

                                    // Clone and add to mission list
                                    MissionPlanSpecification duplicate = mps.deepClone();
                                    duplicate.setName(result);
                                    DefaultMutableTreeNode node = mediator.getProjectSpec().addRootMissionPlan(duplicate);
                                    refreshMissionTree();
                                    selectNode(node);

                                }
                            }
                        });
                        menu.add(new AbstractAction("Delete") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                MissionPlanSpecification mps = (MissionPlanSpecification) ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();

                                boolean reallyDelete = true;

                                if (mediator.getProjectSpec().getReqs() != null) {

                                    for (RequirementSpecification requirementSpecification : mediator.getProjectSpec().getReqs()) {
                                        if (requirementSpecification.getFilledBy() == mps) {
                                            int response = JOptionPane.showConfirmDialog(null, "Are you sure, requirement " + requirementSpecification + " is filled by this specification?");
                                            reallyDelete = reallyDelete && response != JOptionPane.CANCEL_OPTION;
                                        }
                                    }
                                    for (Vertex vertex : mps.getGraph().getVertices()) {
                                        for (RequirementSpecification requirementSpecification : mediator.getProjectSpec().getReqs()) {
                                            if (requirementSpecification.getFilledBy() == vertex) {
                                                int response = JOptionPane.showConfirmDialog(null, "Are you sure, requirement " + requirementSpecification + " is filled by place " + vertex + " in this mission model");
                                                reallyDelete = reallyDelete && response != JOptionPane.CANCEL_OPTION;
                                            }
                                        }
                                    }
                                }

                                if (reallyDelete) {
                                    mediator.getProjectSpec().removeMissionPlanNode((DefaultMutableTreeNode) treePath.getLastPathComponent());
                                    refreshMissionTree();
                                    if (missionsRoot.getChildCount() == 0) {
                                        // No plans, create new one
                                        addNewRootMissionSpec();
                                    } else {
                                        // Pick root mission to display
                                        selectNode(missionsRoot.getNextNode());
                                    }
                                }
                            }
                        });
//                        menu.add(new AbstractAction("Requirements") {
//                            public void actionPerformed(ActionEvent ae) {
//                                MissionPlanSpecification mps = (MissionPlanSpecification) ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();
//                                SelectReqD d = new SelectReqD(null, true, mps);
//                                d.setVisible(true);
//                            }
//                        });

                        menu.show(componentT, me.getX(), me.getY());
                    } else if (treePath.getLastPathComponent() == checkersRoot) {
                        JPopupMenu menu = new JPopupMenu();
                        menu.add(new AbstractAction("Run checker agents") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                taskModelEditor.writeModel();
                                runCheckerAgents();
                            }
                        });
                        menu.show(componentT, me.getX(), me.getY());
                    } else if (pathCount > 1
                            && objectPath[pathCount - 2] == checkersRoot
                            && ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject() instanceof CheckerAgent) {
                        final CheckerAgent checker = (CheckerAgent) ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();
                        final boolean agentEnabled = checker.getEnabled();
                        String text = agentEnabled ? "Disable" : "Enable";
                        JPopupMenu menu = new JPopupMenu();
                        menu.add(new AbstractAction(text) {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                checker.setEnabled(!agentEnabled);
                            }
                        });
                        menu.show(componentT, me.getX(), me.getY());
                    } else if (treePath.getLastPathComponent() == helpersRoot) {
                        JPopupMenu menu = new JPopupMenu();
                        menu.add(new AbstractAction("Run helper agents") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                taskModelEditor.writeModel();
                                runHelperAgents();
                            }
                        });
                        menu.show(componentT, me.getX(), me.getY());
                    } else if (pathCount > 1
                            && objectPath[pathCount - 2] == helpersRoot
                            && ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject() instanceof HelperAgent) {
                        final HelperAgent helper = (HelperAgent) ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();
                        final boolean agentEnabled = helper.getEnabled();
                        String text = agentEnabled ? "Disable" : "Enable";
                        JPopupMenu menu = new JPopupMenu();
                        menu.add(new AbstractAction(text) {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                helper.setEnabled(!agentEnabled);
                            }
                        });
                        menu.show(componentT, me.getX(), me.getY());
                    } else if (treePath.getLastPathComponent() == errorsRoot) {
                        JPopupMenu menu = new JPopupMenu();
                        menu.add(new AbstractAction("Run checker agents") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                taskModelEditor.writeModel();
                                runCheckerAgents();
                            }
                        });
                        menu.show(componentT, me.getX(), me.getY());
                    } else if (pathCount > 1
                            && objectPath[pathCount - 2] == errorsRoot
                            && ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject() instanceof CheckerAgent.AgentMessage) {
                        JPopupMenu menu = new JPopupMenu();
                        menu.add(new AbstractAction("View") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                CheckerAgent.AgentMessage message = (CheckerAgent.AgentMessage) ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();
                                JOptionPane.showMessageDialog(null, message);
                            }
                        });
                        menu.show(componentT, me.getX(), me.getY());
                    } else {
                        LOGGER.info("Nothing for " + ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject().getClass());
                    }
                } else if (treePath != null) {
                    selectNode((DefaultMutableTreeNode) treePath.getLastPathComponent());
                }
            }
        });
        repaint();

        // Try to load the last used DRM file
        boolean loadSuccess = false;
        Preferences p = Preferences.userRoot();
        try {
            String lastDrmPath = p.get(LAST_DRM_FILE, null);
            if (lastDrmPath != null) {
                if (mediator.open(new File(lastDrmPath))) {
                    // Succeeded loading last used specification
                    loadSuccess = true;
                    loadProject();
                } else {
                    // Failed to load last used specification
                    Object[] options = {"Load", "New"};
                    int answer = JOptionPane.showOptionDialog(null, "Could not load last used plan specification (.DRM): create load different file or create new file?", "Load or new?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                    if (answer == JOptionPane.YES_OPTION) {
                        // Get a different specification to try and load
                        if (mediator.open()) {
                            loadSuccess = true;
                            loadProject();
                        } else {
                            // Couldn't load this one either, create a new specification
                            JOptionPane.showMessageDialog(null, "Could not load plan specification (.DRM), creating a new specification");
                        }
                    }
                }
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Failed to save preferences");
        }
        if (!loadSuccess) {
            // New mission
            addNewRootMissionSpec();
            resetSidebar();
        }
        updateTitle();

        // Try to load the last used EPF file
        try {
            String lastEpfPath = p.get(LAST_EPF_FILE, null);
            if (lastEpfPath != null) {
                loadEpf(new File(lastEpfPath));
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Failed to load last used EPF");
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        sideP = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        componentT = new javax.swing.JTree();
        mainP = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileM = new javax.swing.JMenu();
        newDrmM = new javax.swing.JMenuItem();
        openDrmM = new javax.swing.JMenuItem();
        saveDrmM = new javax.swing.JMenuItem();
        saveDrmAsM = new javax.swing.JMenuItem();
        loadEpfM = new javax.swing.JMenuItem();
        editM = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        requirementsM = new javax.swing.JMenu();
        editReqsM = new javax.swing.JMenuItem();
        guiM = new javax.swing.JMenu();
        specGUIM = new javax.swing.JMenuItem();
        agentM = new javax.swing.JMenu();
        runAgentsM = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        modeMenu = new javax.swing.JMenu();
        nominalMode = new javax.swing.JMenuItem();
        recoveryMode = new javax.swing.JMenuItem();
        allMode = new javax.swing.JMenuItem();
        mockupMode = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        sideP.setBackground(sami.gui.GuiConfig.BACKGROUND_COLOR);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Plays");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Checker agents");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Helper agents");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Detected errors");
        treeNode1.add(treeNode2);
        componentT.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        componentT.setBackground(sami.gui.GuiConfig.BACKGROUND_COLOR);
        componentT.setRootVisible(false);
        jScrollPane1.setViewportView(componentT);

        org.jdesktop.layout.GroupLayout sidePLayout = new org.jdesktop.layout.GroupLayout(sideP);
        sideP.setLayout(sidePLayout);
        sidePLayout.setHorizontalGroup(
            sidePLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1102, Short.MAX_VALUE)
        );
        sidePLayout.setVerticalGroup(
            sidePLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 715, Short.MAX_VALUE)
        );

        jSplitPane1.setLeftComponent(sideP);

        mainP.setBackground(sami.gui.GuiConfig.BACKGROUND_COLOR);

        org.jdesktop.layout.GroupLayout mainPLayout = new org.jdesktop.layout.GroupLayout(mainP);
        mainP.setLayout(mainPLayout);
        mainPLayout.setHorizontalGroup(
            mainPLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 81, Short.MAX_VALUE)
        );
        mainPLayout.setVerticalGroup(
            mainPLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 715, Short.MAX_VALUE)
        );

        jSplitPane1.setRightComponent(mainP);

        fileM.setText("File");

        newDrmM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.META_MASK));
        newDrmM.setText("New DRM");
        newDrmM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newDrmMActionPerformed(evt);
            }
        });
        fileM.add(newDrmM);

        openDrmM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.META_MASK));
        openDrmM.setText("Open DRM");
        openDrmM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openDrmMActionPerformed(evt);
            }
        });
        fileM.add(openDrmM);

        saveDrmM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.META_MASK));
        saveDrmM.setText("Save DRM");
        saveDrmM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveDrmMActionPerformed(evt);
            }
        });
        fileM.add(saveDrmM);

        saveDrmAsM.setText("Save DRM as ...");
        saveDrmAsM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveDrmAsMActionPerformed(evt);
            }
        });
        fileM.add(saveDrmAsM);

        loadEpfM.setText("Load EPF");
        loadEpfM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadEpfActionPerformed(evt);
            }
        });
        fileM.add(loadEpfM);

        jMenuBar1.add(fileM);

        editM.setText("Edit");

        jMenuItem2.setText("Rebuild tags");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rebuildTags(evt);
            }
        });
        editM.add(jMenuItem2);

        jMenuBar1.add(editM);

        requirementsM.setText("Requirements");

        editReqsM.setText("Edit");
        editReqsM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editReqsMActionPerformed(evt);
            }
        });
        requirementsM.add(editReqsM);

        jMenuBar1.add(requirementsM);

        guiM.setText("GUI");

        specGUIM.setText("Specify");
        specGUIM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                specGUIMActionPerformed(evt);
            }
        });
        guiM.add(specGUIM);

        jMenuBar1.add(guiM);

        agentM.setText("Agents");

        runAgentsM.setText("Run agents");
        runAgentsM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runAgentsMActionPerformed(evt);
            }
        });
        agentM.add(runAgentsM);

        jMenuBar1.add(agentM);

        jMenu1.setText("Testing");

        jMenuItem1.setText("Generate cases");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuBar1.add(jMenu1);

        modeMenu.setText("Mode");

        nominalMode.setText("Nominal");
        nominalMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nominalModeActionPerformed(evt);
            }
        });
        modeMenu.add(nominalMode);

        recoveryMode.setText("Recovery");
        recoveryMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                recoveryModeActionPerformed(evt);
            }
        });
        modeMenu.add(recoveryMode);

        allMode.setText("All");
        allMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                allModeActionPerformed(evt);
            }
        });
        modeMenu.add(allMode);

        mockupMode.setText("Mockup");
        mockupMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mockupModeActionPerformed(evt);
            }
        });
        modeMenu.add(mockupMode);

        jMenuBar1.add(modeMenu);

        setJMenuBar(jMenuBar1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jSplitPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jSplitPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void addNewRootMissionSpec() {
        // Creates and adds new root mission spec
        // Updates JTree structure
        // Expands and selects created mission spec
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Requesting a new MissionPlanSpecification", this);

        // Save current mission
        taskModelEditor.writeModel();

        // Don't want multiple missions named Anonymous if the developer doesn't rename them manually
        ArrayList<String> playNames = new ArrayList<String>();
        ArrayList<MissionPlanSpecification> missions = mediator.getProjectSpec().getAllMissionPlans();
        for (MissionPlanSpecification mSpec : missions) {
            playNames.add(mSpec.getName());
        }
        String missionName = DreaamHelper.getUniqueName("Anonymous", playNames);

        // Create and add mission
        MissionPlanSpecification spec = mediator.getProjectSpec().getNewMissionPlanSpecification(missionName);
        DefaultMutableTreeNode node = mediator.getProjectSpec().addRootMissionPlan(spec);
        refreshMissionTree();
        selectNode(node);
    }

    public void addSubMissionSpec(MissionPlanSpecification childMSpec, MissionPlanSpecification parentMSpec) {
        // Adds sub-mission spec
        // Updates JTree structure
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Adding a MissionPlanSpecification", this);

        // Save current mission
        taskModelEditor.writeModel();

        // Create and add sub mission
        mediator.getProjectSpec().addSubMissionPlan(childMSpec, parentMSpec);
        refreshMissionTree();
    }

    // Helper function
    private TreePath getPath(TreeNode treeNode) {
        List<Object> nodes = new ArrayList<Object>();
        if (treeNode != null) {
            nodes.add(treeNode);
            treeNode = treeNode.getParent();
            while (treeNode != null) {
                nodes.add(0, treeNode);
                treeNode = treeNode.getParent();
            }
        }

        return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
    }

    private void openDrmMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDrmMActionPerformed
        if (mediator.open()) {
            loadProject();
        } else {
            // Couldn't load the plan
            JOptionPane.showMessageDialog(null, "Could not load plan specification (.DRM) file");
        }
    }//GEN-LAST:event_openDrmMActionPerformed

    private void loadProject() {
        // Clear out errors
        errorsRoot.removeAllChildren();

        // Load the mission tree
        treeRoot.remove(0);
        missionsRoot = mediator.getProjectSpec().getMissionTree();
        treeRoot.insert(missionsRoot, 0);
        refreshMissionTree();

        // Load the first mission into the editor
        ArrayList<MissionPlanSpecification> mpSpecs = mediator.getProjectSpec().getRootMissionPlans();
        if (mpSpecs.size() > 0) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Setting graph", this);
            taskModelEditor.setGraph(mpSpecs.get(0));
            taskModelEditor.setMode(FunctionMode.Nominal);

            selectNode(mediator.getProjectSpec().getNode(mpSpecs.get(0)));
        } else {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "No missions to load", this);
        }

        updateTitle();
    }

    private void updateTitle() {
        if (mediator.getProjectFile() == null) {
            setTitle("Not saved");
        } else {
            setTitle(mediator.getProjectFile().getAbsolutePath());
        }
    }

    private void resetSidebar() {
        // Clear out errors
        errorsRoot.removeAllChildren();

        // Load the mission tree
        treeRoot.remove(0);
        missionsRoot = mediator.getProjectSpec().getMissionTree();
        treeRoot.insert(missionsRoot, 0);
    }

    private void saveDrmMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveDrmMActionPerformed
        taskModelEditor.writeModel();
        mediator.save();
        updateTitle();
    }//GEN-LAST:event_saveDrmMActionPerformed

    private void saveDrmAsMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveDrmAsMActionPerformed
        taskModelEditor.writeModel();
        mediator.saveAs();
        updateTitle();
    }//GEN-LAST:event_saveDrmAsMActionPerformed

    private void newDrmMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newDrmMActionPerformed
        mediator.newSpec();
        addNewRootMissionSpec();
        resetSidebar();
        updateTitle();
    }//GEN-LAST:event_newDrmMActionPerformed

    private void editReqsMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editReqsMActionPerformed
//        Enumeration e = requirementsRoot.breadthFirstEnumeration();
//        ArrayList<RequirementSpecification> oldReqs = null;
//
//        if (e.hasMoreElements()) {
//            oldReqs = new ArrayList<RequirementSpecification>();
//        }
//
//        while (e.hasMoreElements()) {
//            Object obj = ((DefaultMutableTreeNode) e.nextElement()).getUserObject();
//            if (obj instanceof RequirementSpecification) {
//                oldReqs.add((RequirementSpecification) obj);
//            }
//        }
//
//        RequirementsDialog rd = new RequirementsDialog(this, true, oldReqs);
//        rd.setVisible(true);
//
//        ArrayList<RequirementSpecification> reqs = rd.getRequirements();
//
//        requirementsRoot.removeAllChildren();
//        for (RequirementSpecification requirementSpecification : reqs) {
//            requirementsRoot.add(new DefaultMutableTreeNode(requirementSpecification));
//        }
//
//        mediator.setRequirements(reqs);
    }//GEN-LAST:event_editReqsMActionPerformed

    private void specGUIMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_specGUIMActionPerformed
        GuiSpecD d = new GuiSpecD(this, true, mediator.getProjectSpec().getGuiElements());
        d.setVisible(true);
    }//GEN-LAST:event_specGUIMActionPerformed

    private void runAgentsMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runAgentsMActionPerformed
        taskModelEditor.writeModel();
        runHelperAgents();
        runCheckerAgents();
    }//GEN-LAST:event_runAgentsMActionPerformed

    private void runHelperAgents() {
        platform.runHelperAgents();
        refreshMission();
    }

    private void runCheckerAgents() {
        ArrayList<CheckerAgent.AgentMessage> mgs = platform.getMessages();
        errorsRoot.removeAllChildren();
        for (CheckerAgent.AgentMessage agentMessage : mgs) {
            DefaultMutableTreeNode n = new DefaultMutableTreeNode(agentMessage);
            errorsRoot.add(n);
        }
        componentT.scrollPathToVisible(new TreePath(errorsRoot.getPath()));
        ((DefaultTreeModel) componentT.getModel()).nodeStructureChanged(errorsRoot);
    }

    public void refreshMission() {
        taskModelEditor.reloadGraph();
        taskModelEditor.refreshGraphVisibility();
    }

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
//        taskModelEditor.writeModel();
//
//        // Test case generation
//        TestGeneration tg = new TestGeneration();
//        if (tg.testCases != null) {
//            testsRoot.removeAllChildren();
//            for (TestCase testCase : tg.testCases) {
//                DefaultMutableTreeNode n = new DefaultMutableTreeNode(testCase);
//                testsRoot.add(n);
//            }
//            componentT.scrollPathToVisible(new TreePath(testsRoot.getPath()));
//            ((DefaultTreeModel) componentT.getModel()).nodeStructureChanged(testsRoot);
//        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void nominalModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nominalModeActionPerformed
        taskModelEditor.setMode(FunctionMode.Nominal);
    }//GEN-LAST:event_nominalModeActionPerformed

    private void recoveryModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recoveryModeActionPerformed
        taskModelEditor.setMode(FunctionMode.Recovery);
    }//GEN-LAST:event_recoveryModeActionPerformed

    private void loadEpfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadEpfActionPerformed
        File environmentLocation = null;

        if (environmentLocation == null) {
            Preferences p = Preferences.userRoot();
            String lastEpfFolder = p.get(LAST_EPF_FOLDER, "");
            JFileChooser chooser = new JFileChooser(lastEpfFolder);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("EPF specification files", "epf");
            chooser.setFileFilter(filter);
            int ret = chooser.showOpenDialog(null);
            if (ret == JFileChooser.APPROVE_OPTION) {
                environmentLocation = chooser.getSelectedFile();
            }
        }
        loadEpf(environmentLocation);
    }//GEN-LAST:event_loadEpfActionPerformed

    private void rebuildTags(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rebuildTags
        if (mediator.getProjectSpec() != null) {
            mediator.getProjectSpec().updateMissionTags();
        }
        taskModelEditor.repaint();
    }//GEN-LAST:event_rebuildTags

    private void allModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allModeActionPerformed
        taskModelEditor.setMode(FunctionMode.All);
    }//GEN-LAST:event_allModeActionPerformed

    private void mockupModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mockupModeActionPerformed
        taskModelEditor.setMode(FunctionMode.Mockup);
    }//GEN-LAST:event_mockupModeActionPerformed

    public void loadEpf(File epfFile) {
        if (epfFile == null) {
            return;
        }
        EnvironmentProperties environmentProperties = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(epfFile));
            environmentProperties = (EnvironmentProperties) ois.readObject();

            LOGGER.info("Reading environment properties at [" + epfFile + "]");

            if (environmentProperties == null) {
                LOGGER.log(Level.WARNING, "Failed to load environment properties at [" + epfFile + "]");
                JOptionPane.showMessageDialog(null, "Environment properties failed load");
            } else {
                Preferences p = Preferences.userRoot();
                try {
                    p.put(LAST_EPF_FILE, epfFile.getAbsolutePath());
                    p.put(LAST_EPF_FOLDER, epfFile.getParent());
                } catch (AccessControlException e) {
                    LOGGER.severe("Failed to save preferences");
                }

                Engine.getInstance().setEnvironmentProperties(environmentProperties);
            }

        } catch (ClassNotFoundException ex) {
            LOGGER.severe("Class Not Found Exception in EPF load");
        } catch (FileNotFoundException ex) {
            LOGGER.severe("File Not Found Exception on EPF load");
        } catch (IOException ex) {
            LOGGER.severe("IO Exception on EPF load");
        }
    }

    public void selectNode(DefaultMutableTreeNode node) {
        // Changes editor view
        // Updates expanded part of JTree
        // Updates highlighted JTree node
        if (node == null) {
            return;
        }
        Object nodeInfo = node.getUserObject();
        if (nodeInfo instanceof MissionPlanSpecification) {
            taskModelEditor.writeModel();
            taskModelEditor.setGraph((MissionPlanSpecification) nodeInfo);
//            taskModelEditor.setMode(FunctionMode.Nominal);
            componentT.expandPath(getPath(node));
            componentT.setSelectionPath(getPath(node));
        }
        repaint();
    }

    public void expandNode(DefaultMutableTreeNode node) {
        // Updates expanded part of JTree
        if (node == null) {
            return;
        }
        componentT.expandPath(getPath(node));
        refreshMissionTree();
    }

    public void refreshMissionTree() {
        // Refreshes JTree structure
        ((DefaultTreeModel) componentT.getModel()).nodeStructureChanged(missionsRoot);
        componentT.repaint();
    }

    private class DREAMMTreeCellRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree jtree, Object o, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            if (leaf) {
                Object own = ((DefaultMutableTreeNode) o).getUserObject();
                String s;
                JLabel label = null;
                if (own instanceof MissionPlanSpecification) {
                    MissionPlanSpecification mission = (MissionPlanSpecification) own;
                    s = (mission).getName();
                    label = new JLabel(s);
                    if (mission == taskModelEditor.getModel()) {
                        label.setForeground(Color.blue);
                    }
                } else if (own instanceof CheckerAgent) {
                    CheckerAgent checker = (CheckerAgent) own;
                    s = checker.getClass().getSimpleName();
                    label = new JLabel(s);
                    if (checker.getEnabled()) {
                        label.setForeground(Color.blue);
                    }
                } else if (own instanceof HelperAgent) {
                    HelperAgent helper = (HelperAgent) own;
                    s = helper.getClass().getSimpleName();
                    label = new JLabel(s);
                    if (helper.getEnabled()) {
                        label.setForeground(Color.blue);
                    }
                } else if (own instanceof AgentMessage) {
                    AgentMessage message = (AgentMessage) own;
                    s = message.toString();
                    label = new JLabel(s);
                } else {
                    s = own.toString();
                    label = new JLabel(s);
                }

                return label;
            } else {
                return super.getTreeCellRendererComponent(jtree, o, selected, expanded, leaf, row, hasFocus);
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DREAAM().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu agentM;
    private javax.swing.JMenuItem allMode;
    private javax.swing.JTree componentT;
    private javax.swing.JMenu editM;
    private javax.swing.JMenuItem editReqsM;
    private javax.swing.JMenu fileM;
    private javax.swing.JMenu guiM;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JMenuItem loadEpfM;
    private javax.swing.JPanel mainP;
    private javax.swing.JMenuItem mockupMode;
    private javax.swing.JMenu modeMenu;
    private javax.swing.JMenuItem newDrmM;
    private javax.swing.JMenuItem nominalMode;
    private javax.swing.JMenuItem openDrmM;
    private javax.swing.JMenuItem recoveryMode;
    private javax.swing.JMenu requirementsM;
    private javax.swing.JMenuItem runAgentsM;
    private javax.swing.JMenuItem saveDrmAsM;
    private javax.swing.JMenuItem saveDrmM;
    private javax.swing.JPanel sideP;
    private javax.swing.JMenuItem specGUIM;
    // End of variables declaration//GEN-END:variables
}
