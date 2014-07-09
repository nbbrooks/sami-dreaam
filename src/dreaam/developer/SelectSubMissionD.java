package dreaam.developer;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
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
 * Dialog window that lets you select InputEvents and OutputEvents for SAMI
 * transitions
 *
 * @author pscerri
 */
public class SelectSubMissionD extends javax.swing.JDialog {

    private static final Logger LOGGER = Logger.getLogger(SelectSubMissionD.class.getName());
    private MissionPlanSpecification parentMSpec;
    private ProjectSpecification pSpec;
    private ArrayList<MissionPlanSpecification> subMSpecs;
    private ArrayList<MissionPlanSpecification> createdSubMSpecs = new ArrayList<MissionPlanSpecification>();
    private ArrayList<MissionPlanSpecification> deletedSubMSpecs = new ArrayList<MissionPlanSpecification>();
    private HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>> mSpecTaskMap;

    private javax.swing.JButton newB, okB, cancelB;

    // OK button used to exit the dialog?
    private boolean okExit = false;

    // Layout
    private GroupLayout layout;
    private GroupLayout.SequentialGroup rowSeqGroup;
    private GroupLayout.ParallelGroup rowParGroup1;
    private GroupLayout.SequentialGroup colSeqGroup;
    private GroupLayout.ParallelGroup[] colParGroupArr;
    private int row;
    private int maxColWidth;
    private int cumulComponentHeight;
    private final static int BUTTON_WIDTH = 100;
    private final static int BUTTON_HEIGHT = 50;

    private JScrollPane mSpecSP;
    private JPanel existingMSpecP;

    public SelectSubMissionD(java.awt.Frame parent, boolean modal, MissionPlanSpecification parentMSpec, ProjectSpecification pSpec, ArrayList<MissionPlanSpecification> existingSubMSpecs, HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>> existingMSpecTaskMap) {
        super(parent, modal);
        this.parentMSpec = parentMSpec;
        this.pSpec = pSpec;
        this.subMSpecs = (ArrayList<MissionPlanSpecification>) existingSubMSpecs.clone();
        if (subMSpecs == null) {
            subMSpecs = new ArrayList<MissionPlanSpecification>();
        }
        this.mSpecTaskMap = (HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>>) existingMSpecTaskMap.clone();
        if (mSpecTaskMap == null) {
            mSpecTaskMap = new HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>>();
        }
        initComponents();
        setTitle("SelectSubMissionDNew");
    }

    private void addComponent(JComponent component) {
        rowParGroup1.addComponent(component);
        colParGroupArr[row] = layout.createParallelGroup();
        colParGroupArr[row].addComponent(component);
        maxColWidth = Math.max(maxColWidth, (int) component.getPreferredSize().getWidth() + BUTTON_WIDTH);
        cumulComponentHeight += Math.max((int) component.getPreferredSize().getHeight(), BUTTON_HEIGHT);
        row++;
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        int numRows = 4;
        rowSeqGroup = layout.createSequentialGroup();
        rowParGroup1 = layout.createParallelGroup();
        colSeqGroup = layout.createSequentialGroup();
        colParGroupArr = new GroupLayout.ParallelGroup[numRows];
        row = 0;
        maxColWidth = BUTTON_WIDTH;
        cumulComponentHeight = 0;

        existingMSpecP = new JPanel();
        existingMSpecP.setLayout(new BoxLayout(existingMSpecP, BoxLayout.Y_AXIS));
        for (MissionPlanSpecification mSpec : subMSpecs) {
            SubMissionElementP mSpecP = new SubMissionElementP(mSpec);
            existingMSpecP.add(mSpecP);
        }

        mSpecSP = new JScrollPane();
        mSpecSP.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mSpecSP.setViewportView(existingMSpecP);
        addComponent(mSpecSP);

        newB = new javax.swing.JButton();
        newB.setText("Add New");
        newB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newBActionPerformed(evt);
            }
        });
        addComponent(newB);

        okB = new javax.swing.JButton();
        okB.setText("OK");
        okB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okBActionPerformed(evt);
            }
        });
        addComponent(okB);

        cancelB = new javax.swing.JButton();
        cancelB.setText("Cancel");
        cancelB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelBActionPerformed(evt);
            }
        });
        addComponent(cancelB);

        // Finish layout setup
        layout.setHorizontalGroup(rowSeqGroup
                //                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 1, Short.MAX_VALUE) // Spring to right-align
                .addGroup(rowParGroup1));
        for (int i = 0; i < colParGroupArr.length; i++) {
            GroupLayout.ParallelGroup parGroup = colParGroupArr[i];
            colSeqGroup.addGroup(parGroup);
            if (i < colParGroupArr.length - 1) {
                colSeqGroup.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE);
                cumulComponentHeight += 6;
            }
        }
        layout.setVerticalGroup(colSeqGroup);

        // Adjust dialog size
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenWidth = gd.getDisplayMode().getWidth();
        int screenHeight = gd.getDisplayMode().getHeight();
        maxColWidth = Math.min(maxColWidth, screenWidth);
        cumulComponentHeight = Math.min(cumulComponentHeight, screenHeight);
        setSize(new Dimension(maxColWidth, cumulComponentHeight));
        setPreferredSize(new Dimension(maxColWidth, cumulComponentHeight));

//        setPreferredSize(new Dimension(300, 300));
        validate();
    }

    private void newBActionPerformed(java.awt.event.ActionEvent evt) {
        EditSubMissionD subMissionD = new EditSubMissionD(null, true, parentMSpec);
        subMissionD.setVisible(true);

        if (subMissionD.confirmedExit()) {
            if (subMissionD.getTaskMapMethod() == EditSubMissionD.TaskMapMethod.Event) {
                MissionPlanSpecification submissionSpec = ((MissionPlanSpecification) subMissionD.getSelectedMission()).getSubmissionInstance(subMissionD.getSelectedMission(), subMissionD.getNamePrefix(), subMissionD.getVariablePrefix(), pSpec.getGlobalVariableToValue());
                subMSpecs.add(submissionSpec);
                createdSubMSpecs.add(submissionSpec);
            } else if (subMissionD.getTaskMapMethod() == EditSubMissionD.TaskMapMethod.Manual) {
                MissionPlanSpecification submissionSpec = ((MissionPlanSpecification) subMissionD.getSelectedMission()).getSubmissionInstance(subMissionD.getSelectedMission(), subMissionD.getNamePrefix(), subMissionD.getVariablePrefix(), pSpec.getGlobalVariableToValue());
                subMSpecs.add(submissionSpec);
                mSpecTaskMap.put(submissionSpec, subMissionD.getTaskMapping());
                createdSubMSpecs.add(submissionSpec);
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

    public HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>> getSubMissionToTaskMap() {
        return mSpecTaskMap;
    }

    private void refreshMSpecP() {
        existingMSpecP.removeAll();
//        existingMSpecP.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        for (MissionPlanSpecification mSpec : subMSpecs) {
            SubMissionElementP mSpecP = new SubMissionElementP(mSpec);
            existingMSpecP.add(mSpecP);
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
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

//            modifyB = new JButton("Edit");
//            modifyB.setPreferredSize(new Dimension(10, 10));
//            modifyB.addActionListener(new ActionListener() {
//
//                @Override
//                public void actionPerformed(ActionEvent ae) {
//                    SubMissionD subMissionD = new SubMissionD(null, true, parentMSpec, subMMSpec, newMSpecTaskMap.get(subMMSpec));
//                    subMissionD.setVisible(true);
//
//                    if (subMissionD.confirmedExit()) {
//                        // Remove old entry
//                        newSubMSpecs.remove(subMMSpec);
//                        newMSpecTaskMap.remove(subMMSpec);
//                        // Add new entry
//                        if (subMissionD.getTaskMapMethod() == SubMissionD.TaskMapMethod.Event) {
//                            MissionPlanSpecification submissionSpec = ((MissionPlanSpecification) subMissionD.getSelectedMission()).getSubmissionInstance(subMissionD.getSelectedMission(), subMissionD.getNamePrefix(), subMissionD.getVariablePrefix());
//                            newSubMSpecs.add(submissionSpec);
//                        } else if (subMissionD.getTaskMapMethod() == SubMissionD.TaskMapMethod.Manual) {
//                            MissionPlanSpecification submissionSpec = ((MissionPlanSpecification) subMissionD.getSelectedMission()).getSubmissionInstance(subMissionD.getSelectedMission(), subMissionD.getNamePrefix(), subMissionD.getVariablePrefix());
//                            newSubMSpecs.add(submissionSpec);
//                            newMSpecTaskMap.put(submissionSpec, subMissionD.getTaskMapping());
//                        }
//                        refreshMSpecP();
//                    }
//                }
//            });
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

            add(new JLabel(subMMSpec.getName()));
//            add(modifyB);
            add(deleteB);

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
