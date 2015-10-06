package dreaam.developer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.*;
import sami.mission.MissionPlanSpecification;
import sami.mission.ProjectSpecification;
import sami.mission.TaskSpecification;

/**
 * Dialog window used to select InputEvents and OutputEvents for a transition or
 * place
 *
 * @author nbb
 */
public class SelectSubMissionD extends javax.swing.JDialog {

    private static final Logger LOGGER = Logger.getLogger(SelectSubMissionD.class.getName());
    private MissionPlanSpecification parentMSpec;
    private ProjectSpecification pSpec;
    private ArrayList<MissionPlanSpecification> subMSpecs;
    private ArrayList<MissionPlanSpecification> createdSubMSpecs = new ArrayList<MissionPlanSpecification>();
    private ArrayList<MissionPlanSpecification> deletedSubMSpecs = new ArrayList<MissionPlanSpecification>();
    private HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>> mSpecTaskMap;
    private HashMap<MissionPlanSpecification, Boolean> mSpecToIsSharedInstance;

    private javax.swing.JButton newB, okB, cancelB;

    // OK button used to exit the dialog?
    private boolean okExit = false;

    // Layout
    private int maxComponentWidth;
    private int cumulComponentHeight;
    private final static int BUTTON_WIDTH = 250;
    private final static int BUTTON_HEIGHT = 50;

    private JScrollPane existingSubMissionsSP;
    private JPanel existingSubMissionsP;

    public SelectSubMissionD(java.awt.Frame parent, boolean modal, MissionPlanSpecification parentMSpec, ProjectSpecification pSpec, ArrayList<MissionPlanSpecification> existingSubMSpecs, HashMap<MissionPlanSpecification, Boolean> existingMSpecToIsSharedInstance, HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>> existingMSpecTaskMap) {
        super(parent, modal);
        this.parentMSpec = parentMSpec;
        this.pSpec = pSpec;
        if (existingSubMSpecs == null) {
            this.subMSpecs = new ArrayList<MissionPlanSpecification>();
        } else {
            this.subMSpecs = (ArrayList<MissionPlanSpecification>) existingSubMSpecs.clone();
        }
        if (existingMSpecToIsSharedInstance == null) {
            this.mSpecToIsSharedInstance = new HashMap<MissionPlanSpecification, Boolean>();
        } else {
            this.mSpecToIsSharedInstance = (HashMap<MissionPlanSpecification, Boolean>) existingMSpecToIsSharedInstance.clone();
        }
        if (existingMSpecTaskMap == null) {
            this.mSpecTaskMap = new HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>>();
        } else {
            this.mSpecTaskMap = (HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>>) existingMSpecTaskMap.clone();
        }
        if (existingMSpecTaskMap == null) {
            this.mSpecTaskMap = new HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>>();
        } else {
            this.mSpecTaskMap = (HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>>) existingMSpecTaskMap.clone();
        }
        initComponents();
        setTitle("SelectSubMissionD");
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        maxComponentWidth = BUTTON_WIDTH;
        cumulComponentHeight = 0;

        existingSubMissionsP = new JPanel();
        existingSubMissionsP.setLayout(new BoxLayout(existingSubMissionsP, BoxLayout.Y_AXIS));
        for (MissionPlanSpecification mSpec : subMSpecs) {
            SubMissionElementP mSpecP = new SubMissionElementP(mSpec);
            existingSubMissionsP.add(mSpecP);
        }

        existingSubMissionsSP = new JScrollPane();
        existingSubMissionsSP.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        existingSubMissionsSP.setViewportView(existingSubMissionsP);
        cumulComponentHeight += Math.max(existingSubMissionsP.getPreferredSize().getHeight(), BUTTON_HEIGHT);
        maxComponentWidth = Math.max(maxComponentWidth, existingSubMissionsP.getPreferredSize().width);

        JPanel buttonsP = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.weightx = 1.0;

        newB = new javax.swing.JButton();
        newB.setText("Add New");
        newB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newBActionPerformed(evt);
            }
        });
        newB.setPreferredSize(new Dimension(maxComponentWidth, BUTTON_HEIGHT));
        cumulComponentHeight += Math.max(newB.getPreferredSize().getHeight(), BUTTON_HEIGHT);
        maxComponentWidth = Math.max(maxComponentWidth, newB.getPreferredSize().width);
        buttonsP.add(newB, constraints);
        constraints.gridy = constraints.gridy + 1;

        okB = new javax.swing.JButton();
        okB.setText("OK");
        okB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okBActionPerformed(evt);
            }
        });
        okB.setPreferredSize(new Dimension(maxComponentWidth, BUTTON_HEIGHT));
        cumulComponentHeight += Math.max(okB.getPreferredSize().getHeight(), BUTTON_HEIGHT);
        maxComponentWidth = Math.max(maxComponentWidth, okB.getPreferredSize().width);
        buttonsP.add(okB, constraints);
        constraints.gridy = constraints.gridy + 1;

        cancelB = new javax.swing.JButton();
        cancelB.setText("Cancel");
        cancelB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelBActionPerformed(evt);
            }
        });
        cancelB.setPreferredSize(new Dimension(maxComponentWidth, BUTTON_HEIGHT));
        cumulComponentHeight += Math.max(cancelB.getPreferredSize().getHeight(), BUTTON_HEIGHT);
        maxComponentWidth = Math.max(maxComponentWidth, cancelB.getPreferredSize().width);
        buttonsP.add(cancelB, constraints);
        constraints.gridy = constraints.gridy + 1;

        BoxLayout boxLayout = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
        getContentPane().setLayout(boxLayout);
        getContentPane().add(existingSubMissionsSP);
        getContentPane().add(buttonsP);

        // Adjust dialog size
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenWidth = gd.getDisplayMode().getWidth();
        int screenHeight = gd.getDisplayMode().getHeight();
        maxComponentWidth = Math.min(maxComponentWidth, screenWidth);
        cumulComponentHeight = Math.min(cumulComponentHeight, screenHeight);
        // Don't use cumulComponentHeight for now
        setSize(new Dimension(maxComponentWidth, (int) (screenHeight * 0.9)));
        setPreferredSize(new Dimension(maxComponentWidth, (int) (screenHeight * 0.9)));

        validate();
    }

    private void newBActionPerformed(java.awt.event.ActionEvent evt) {
        EditSubMissionD subMissionD = new EditSubMissionD(null, true, parentMSpec);
        subMissionD.setVisible(true);

        if (subMissionD.confirmedExit()) {
            MissionPlanSpecification submissionSpec = ((MissionPlanSpecification) subMissionD.getSelectedMission()).getSubmissionInstance(subMissionD.getSelectedMission(), subMissionD.getNamePrefix(), subMissionD.getVariablePrefix(), pSpec.getGlobalVariableToValue());
            mSpecTaskMap.put(submissionSpec, subMissionD.getTaskMapping());
            mSpecToIsSharedInstance.put(submissionSpec, subMissionD.getIsSharedInstantiation());
            subMSpecs.add(submissionSpec);
            createdSubMSpecs.add(submissionSpec);
            if (subMissionD.getTaskMapMethod() == EditSubMissionD.TaskMapMethod.Manual) {
                mSpecTaskMap.put(submissionSpec, subMissionD.getTaskMapping());
            }

            refreshMSpecP();
        }
    }

    private void okBActionPerformed(java.awt.event.ActionEvent evt) {
        okExit = true;
        setVisible(false);
    }

    public boolean confirmedExit() {
        return okExit;
    }

    private void cancelBActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
    }

    public ArrayList<MissionPlanSpecification> getSubMissions() {
        return subMSpecs;
    }

    public ArrayList<MissionPlanSpecification> getCreatedSubMissions() {
        return createdSubMSpecs;
    }

    public ArrayList<MissionPlanSpecification> getDeletedSubMissions() {
        return deletedSubMSpecs;
    }

    public HashMap<MissionPlanSpecification, Boolean> getSubMissionToIsSharedInstance() {
        return mSpecToIsSharedInstance;
    }

    public HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>> getSubMissionToTaskMap() {
        return mSpecTaskMap;
    }

    private void refreshMSpecP() {
        existingSubMissionsP.removeAll();
        for (MissionPlanSpecification mSpec : subMSpecs) {
            SubMissionElementP mSpecP = new SubMissionElementP(mSpec);
            existingSubMissionsP.add(mSpecP);
        }
        validate();
        repaint();
    }

    class SubMissionElementP extends JPanel {

        MissionPlanSpecification subMMSpec;
        JButton modifyB, deleteB;

        public SubMissionElementP(MissionPlanSpecification mSpec) {
            this.subMMSpec = mSpec;
            initComponents();
        }

        private void initComponents() {
            setLayout(new BorderLayout());

            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

            deleteB = new JButton("Delete");
            deleteB.setPreferredSize(new Dimension(10, 10));
            deleteB.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    subMSpecs.remove(subMMSpec);
                    mSpecTaskMap.remove(subMMSpec);
                    deletedSubMSpecs.add(subMMSpec);
                    refreshMSpecP();
                }
            });

            add(new JLabel(subMMSpec.getName()), BorderLayout.WEST);
            add(deleteB, BorderLayout.EAST);

            pack();
        }
    }

//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String args[]) {
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                SelectSubMissionDNew dialog = new SelectSubMissionDNew(new javax.swing.JFrame(), true, null, new ArrayList<String>(), true, true);
//                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
//                    public void windowClosing(java.awt.event.WindowEvent e) {
//                        System.exit(0);
//                    }
//                });
//                dialog.setVisible(true);
//            }
//        });
//    }
}
