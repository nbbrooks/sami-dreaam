package dreaam.developer;

import dreaam.Helper;
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
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.*;
import sami.config.DomainConfigManager;
import sami.gui.GuiConfig;
import sami.mission.MissionPlanSpecification;
import sami.mission.RequirementSpecification;
import sami.mission.Vertex;
import sami.mission.Vertex.FunctionMode;

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
    DefaultMutableTreeNode playsRoot = null;
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
        playsRoot = (DefaultMutableTreeNode) componentT.getModel().getChild(treeRoot, 0);
//        agentsRoot = (DefaultMutableTreeNode) componentT.getModel().getChild(treeRoot, 1);
        checkersRoot = (DefaultMutableTreeNode) componentT.getModel().getChild(treeRoot, 1);
        helpersRoot = (DefaultMutableTreeNode) componentT.getModel().getChild(treeRoot, 2);
        errorsRoot = (DefaultMutableTreeNode) componentT.getModel().getChild(treeRoot, 3);
        componentT.setCellRenderer(new DREAMMTreeCellRenderer());

        // Main panel
        mainP.setLayout(new BorderLayout());
        MissionPlanSpecification spec = mediator.getProjectSpec().getNewMissionPlanSpecification("Anonymous");
        taskModelEditor = new TaskModelEditor(this, spec);
        mainP.add(taskModelEditor, BorderLayout.CENTER);

        // Add items to side panel
        playsRoot.add(new DefaultMutableTreeNode(spec));
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

                    if (treePath.getLastPathComponent() == playsRoot) {

                        JPopupMenu menu = new JPopupMenu();
                        menu.add(new AbstractAction("Add new play") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                newMissionSpec();
                            }
                        });

                        menu.show(componentT, me.getX(), me.getY());
                    } else if (pathCount > 1
                            && objectPath[pathCount - 2] == playsRoot
                            && ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject() instanceof MissionPlanSpecification) {
                        // Plan spec under Plays menu
                        JPopupMenu menu = new JPopupMenu();
                        menu.add(new AbstractAction("Use as Template") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                MissionPlanSpecification mps = (MissionPlanSpecification) ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();
                                taskModelEditor.addTemplate(mps);
                            }
                        });
                        menu.add(new AbstractAction("Rename") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                String result = JOptionPane.showInputDialog(null, "Rename mission:");
                                if (result != null) {
                                    MissionPlanSpecification mps = (MissionPlanSpecification) ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();
                                    mps.setName(result);
                                    ((DefaultTreeModel) componentT.getModel()).nodeStructureChanged(playsRoot);
                                }
                            }
                        });
                        menu.add(new AbstractAction("Delete") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                MissionPlanSpecification mps = (MissionPlanSpecification) ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();

                                boolean reallyDelete = true;

                                if (mediator.getReqs() != null) {

                                    for (RequirementSpecification requirementSpecification : mediator.getReqs()) {
                                        if (requirementSpecification.getFilledBy() == mps) {
                                            int response = JOptionPane.showConfirmDialog(null, "Are you sure, requirement " + requirementSpecification + " is filled by this specification?");
                                            reallyDelete = reallyDelete && response != JOptionPane.CANCEL_OPTION;
                                        }
                                    }
                                    for (Vertex vertex : mps.getGraph().getVertices()) {
                                        for (RequirementSpecification requirementSpecification : mediator.getReqs()) {
                                            if (requirementSpecification.getFilledBy() == vertex) {
                                                int response = JOptionPane.showConfirmDialog(null, "Are you sure, requirement " + requirementSpecification + " is filled by place " + vertex + " in this mission model");
                                                reallyDelete = reallyDelete && response != JOptionPane.CANCEL_OPTION;
                                            }
                                        }
                                    }
                                }

                                if (reallyDelete) {
                                    //@todo: remove in and out vertices and edges from Vertex
                                    playsRoot.remove((DefaultMutableTreeNode) treePath.getLastPathComponent());
                                    ((DefaultTreeModel) componentT.getModel()).nodeStructureChanged(playsRoot);
                                    mediator.remove(mps);

                                    if (playsRoot.getLeafCount() == 1) {
                                        newMissionSpec();
                                    }
                                    if (componentT.getSelectionPath() != null) {
                                        nodeSelected((DefaultMutableTreeNode) componentT.getSelectionPath().getLastPathComponent());
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
                        System.out.println("Nothing for " + ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject().getClass());
                    }
                } else if (treePath != null) {
                    nodeSelected((DefaultMutableTreeNode) treePath.getLastPathComponent());
                }
            }
        });
        repaint();

        // Try to load the last used DRM file
        Preferences p = Preferences.userRoot();
        try {
            String lastDrmPath = p.get(LAST_DRM_FILE, null);
            if (lastDrmPath != null) {
                if (mediator.open(new File(lastDrmPath))) {
                    // Succeeded loading last used specification
                    _open();
                } else {
                    // Failed to load last used specification
                    Object[] options = {"Load", "New"};
                    int answer = JOptionPane.showOptionDialog(null, "Could not load last used plan specification: create new DCF or load a different DCF?", "Load or new?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                    if (answer == JOptionPane.YES_OPTION) {
                        // Get a different specification to try and load
                        if (mediator.open()) {
                            _open();
                        } else {
                            // Couldn't load this one either, create a new specification
                            JOptionPane.showMessageDialog(null, "Could not load plan specification, creating a new specification");
                        }
                    } else {
                        // New specification
                    }
                }
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Failed to save preferences");
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
        newM = new javax.swing.JMenuItem();
        openM = new javax.swing.JMenuItem();
        saveM = new javax.swing.JMenuItem();
        saveAsM = new javax.swing.JMenuItem();
        editM = new javax.swing.JMenu();
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

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        sideP.setBackground(GuiConfig.BACKGROUND_COLOR);

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
        componentT.setBackground(GuiConfig.BACKGROUND_COLOR);
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

        mainP.setBackground(GuiConfig.BACKGROUND_COLOR);

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

        newM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.META_MASK));
        newM.setText("New");
        newM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMActionPerformed(evt);
            }
        });
        fileM.add(newM);

        openM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.META_MASK));
        openM.setText("Open");
        openM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMActionPerformed(evt);
            }
        });
        fileM.add(openM);

        saveM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.META_MASK));
        saveM.setText("Save");
        saveM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMActionPerformed(evt);
            }
        });
        fileM.add(saveM);

        saveAsM.setText("Save as ...");
        saveAsM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMActionPerformed(evt);
            }
        });
        fileM.add(saveAsM);

        jMenuBar1.add(fileM);

        editM.setText("Edit");
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

    private void newMissionSpec() {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Requesting a new MissionPlanSpecification", this);
        taskModelEditor.writeModel();
        // Dont't want multiple missions named Anonymous if the developer doesn't rename them manually
        ArrayList<String> playNames = new ArrayList<String>();
        Enumeration enumeration = playsRoot.children();
        while (enumeration.hasMoreElements()) {
            playNames.add(((DefaultMutableTreeNode) enumeration.nextElement()).toString());
        }
        String missionName = Helper.getUniqueName("Anonymous", playNames);
        MissionPlanSpecification spec = mediator.getProjectSpec().getNewMissionPlanSpecification(missionName);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(spec);
        playsRoot.add(node);
        taskModelEditor.setGraph(spec);
        ((DefaultTreeModel) componentT.getModel()).nodeStructureChanged(playsRoot);
        componentT.setSelectionPath(getPath(node));
    }

    public void addMissionSpec(MissionPlanSpecification spec) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Adding a MissionPlanSpecification", this);
        taskModelEditor.writeModel();
        mediator.getProjectSpec().addMissionPlan(spec);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(spec);
        playsRoot.add(node);
        ((DefaultTreeModel) componentT.getModel()).nodeStructureChanged(playsRoot);
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

    private void openMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMActionPerformed
        if (mediator.open()) {
            _open();
        }
    }//GEN-LAST:event_openMActionPerformed

    private void _open() {
        // Clear out stuff from previous DRM
        playsRoot.removeAllChildren();
        errorsRoot.removeAllChildren();

        // Load the specification and put them in the JTree
        ArrayList<MissionPlanSpecification> mpSpecs = mediator.getProjectSpec().getMissionPlans();
        if (mpSpecs.size() > 0) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Setting graph", this);
            taskModelEditor.setGraph(mpSpecs.get(0));
            taskModelEditor.setMode(FunctionMode.Nominal);
            for (MissionPlanSpecification mp : mpSpecs) {
                playsRoot.add(new DefaultMutableTreeNode(mp));
            }
        } else {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "No missions to load", this);
        }

//        ArrayList<RequirementSpecification> reqs = mediator.getReqs();
//        if (reqs != null && reqs.size() > 0) {
//            for (RequirementSpecification requirementSpecification : reqs) {
//                requirementsRoot.add(new DefaultMutableTreeNode(requirementSpecification));
//            }
//        }
        // Redraw tree
        ((DefaultTreeModel) componentT.getModel()).nodeStructureChanged(treeRoot);
        // Reset view
        taskModelEditor.vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
        
        setTitle(Preferences.userRoot().get(LAST_DRM_FILE, null));
    }

    private void saveMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMActionPerformed
        taskModelEditor.writeModel();
        mediator.getProjectSpec().addMissionPlan(taskModelEditor.getModel());
        mediator.save();

    }//GEN-LAST:event_saveMActionPerformed

    private void saveAsMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMActionPerformed
        taskModelEditor.writeModel();
        mediator.getProjectSpec().addMissionPlan(taskModelEditor.getModel());
        mediator.saveAs();
    }//GEN-LAST:event_saveAsMActionPerformed

    private void newMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMActionPerformed
        playsRoot.removeAllChildren();
        errorsRoot.removeAllChildren();

        mediator.newSpec();
        _open();
    }//GEN-LAST:event_newMActionPerformed

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
        GuiSpecD d = new GuiSpecD(this, true, mediator.getGuiSpecs());
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

    public void nodeSelected(DefaultMutableTreeNode node) {
        // DefaultMutableTreeNode node = (DefaultMutableTreeNode) componentT.getLastSelectedPathComponent();

        if (node == null) {
            return;
        }

        Object nodeInfo = node.getUserObject();
        if (node.isLeaf()) {
            if (nodeInfo instanceof MissionPlanSpecification) {
                taskModelEditor.writeModel();
                taskModelEditor.setGraph((MissionPlanSpecification) nodeInfo);
                taskModelEditor.setMode(FunctionMode.Nominal);
//                ((MissionPlanSpecification) nodeInfo).printGraph();
            }
        }
        repaint();
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
    private javax.swing.JTree componentT;
    private javax.swing.JMenu editM;
    private javax.swing.JMenuItem editReqsM;
    private javax.swing.JMenu fileM;
    private javax.swing.JMenu guiM;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JPanel mainP;
    private javax.swing.JMenu modeMenu;
    private javax.swing.JMenuItem newM;
    private javax.swing.JMenuItem nominalMode;
    private javax.swing.JMenuItem openM;
    private javax.swing.JMenuItem recoveryMode;
    private javax.swing.JMenu requirementsM;
    private javax.swing.JMenuItem runAgentsM;
    private javax.swing.JMenuItem saveAsM;
    private javax.swing.JMenuItem saveM;
    private javax.swing.JPanel sideP;
    private javax.swing.JMenuItem specGUIM;
    // End of variables declaration//GEN-END:variables
}
