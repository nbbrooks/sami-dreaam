package dreaam.developer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import sami.mission.MissionPlanSpecification;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import sami.engine.Mediator;
import sami.mission.TaskSpecification;

/**
 *
 * @author pscerri
 */
public class EditSubMissionD extends javax.swing.JDialog {

    private static final Logger LOGGER = Logger.getLogger(EditSubMissionD.class.getName());

    // How to allocate proxies in parent mission to sub mission?
    //  Event: The sub mission begins with a task allocation event which will allocate the mission's tasks to the proxy tokens passed in from the parent mission 
    //  Manual: The plan developer will specify a mapping from task tokens in the parent mission to task tokens in the sub mission:
    //      the proxy assigned to task "A" in the parent mission will be assigned to mapped task "1" in the sub mission when it is instantiated
    public enum TaskMapMethod {

        Event, Manual
    };

    // If a place contains a sub-mission that has already begun and a new set of tokens enters the place, 
    //  Single:
    //  Multiple:
    public enum InstantiationMethod {

        Individual, Shared
    };

    private Mediator mediator = Mediator.getInstance();
    private MissionPlanSpecification parentMSpec;
    private MissionPlanSpecification selectedMSpec = null;
    private ArrayList<TaskSpecification> selectedMTaskSpecList = new ArrayList<TaskSpecification>();
    private InstantiationMethod selectedInstantiationMethod;
    private TaskMapMethod selectedTaskMapMethod;
    // All fields are valid?
    private boolean valid = false;
    private ActivityListener activityListener = new ActivityListener();
    // OK button used to exit the dialog?
    private boolean okExit = false;

    // Scroll list for selecting plan to use as submission
    private JLabel planLabel;
    private JComboBox planCB;
    // Text field for specifying name prefix
    private JLabel namePrefixL;
    private JTextField namePrefixTF;
    // Text field for specifying variable prefix
    private JLabel variablePrefixL;
    private JTextField variablePrefixTF;
    // Combo box for choosing instantiation method
    private JLabel instantiationMethodL;
    private JComboBox instantiationMethodCB;
    // Combo box for choosing task allocation method
    private JLabel allocationMethodL;
    private JComboBox taskMapMethodCB;
    // Combo boxes for mapping task tokens to sub mission (if appropriate)
    private JPanel taskMappingP;
    private JLabel taskMappingL;
    private HashMap<TaskSpecification, JLabel> taskNameToL = new HashMap<TaskSpecification, JLabel>();
    private HashMap<TaskSpecification, JComboBox> taskNameToCB = new HashMap<TaskSpecification, JComboBox>();
    // Done/cancel
    private JButton okB, cancelB;

    // Layout
    private GroupLayout layout;
    private GroupLayout.SequentialGroup rowSeqGroup;
    private GroupLayout.ParallelGroup rowParGroup1;
    private GroupLayout.SequentialGroup colSeqGroup;
    private GroupLayout.ParallelGroup[] colParGroupArr;
    private int row;
    private int maxColWidth;
    private int cumulComponentHeight;
    private final static int BUTTON_WIDTH = 250;
    private final static int BUTTON_HEIGHT = 50;

    public EditSubMissionD(java.awt.Frame parent, boolean modal, MissionPlanSpecification parentMSpec) {
        super(parent, modal);
        this.parentMSpec = parentMSpec;
        initComponents();
        setTitle("SubMissionD");
    }

    public EditSubMissionD(java.awt.Frame parent, boolean modal, MissionPlanSpecification parentMSpec, MissionPlanSpecification subMMSpec, boolean isSharedInstance, HashMap<TaskSpecification, TaskSpecification> taskMapping) {
        // Want to edit existing sub-mission
        this(parent, modal, parentMSpec);
        planCB.setSelectedItem(subMMSpec);
        instantiationMethodCB.setSelectedItem((isSharedInstance ? InstantiationMethod.Shared : InstantiationMethod.Individual));
        if (taskMapping != null) {
            // Sub-mission currently has a Manual task mapping
            for (TaskSpecification taskSpec : taskMapping.keySet()) {
                JComboBox taskCombo = taskNameToCB.get(taskSpec);
                if (taskCombo != null) {
                    taskCombo.setSelectedItem(taskMapping.get(taskSpec));
                } else {
                    // This could legitimately  happen if we remove a task from a child mission and then edit the child mission
                    LOGGER.warning("Could not find combo box for child mission task: " + taskSpec);
                }
            }
        }
    }

    private void addComponent(JComponent component) {
        rowParGroup1.addComponent(component);
        colParGroupArr[row] = layout.createParallelGroup();
        colParGroupArr[row].addComponent(component);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        maxColWidth = Math.max(maxColWidth, (int) component.getPreferredSize().getWidth() + BUTTON_WIDTH);
        cumulComponentHeight += Math.max((int) component.getPreferredSize().getHeight(), BUTTON_HEIGHT);
        row++;
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        int numRows = 13;
        rowSeqGroup = layout.createSequentialGroup();
        rowParGroup1 = layout.createParallelGroup();
        colSeqGroup = layout.createSequentialGroup();
        colParGroupArr = new GroupLayout.ParallelGroup[numRows];
        row = 0;
        maxColWidth = BUTTON_WIDTH;
        cumulComponentHeight = 0;

        // Scroll list for selecting plan to use as submission
        planLabel = new JLabel("Plan to use as sub mission");
        planCB = new JComboBox(mediator.getProject().getRootMissionPlans().toArray());
        selectedMSpec = (MissionPlanSpecification) planCB.getSelectedItem();
        planCB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (selectedMSpec == (MissionPlanSpecification) planCB.getSelectedItem()) {
                    return;
                }
                selectedMSpec = (MissionPlanSpecification) planCB.getSelectedItem();
                selectedMTaskSpecList.clear();
                updateTaskMapping();
            }
        });
        addComponent(planLabel);
        addComponent(planCB);

        // Text field for specifying name prefix
        namePrefixL = new JLabel("Text prefix for sub mission instance's name?");
        namePrefixTF = new JTextField("");
        namePrefixTF.addKeyListener(activityListener);
        namePrefixTF.addFocusListener(activityListener);
        namePrefixTF.addMouseListener(activityListener);
        addComponent(namePrefixL);
        addComponent(namePrefixTF);

        // Text field for specifying variable prefix
        variablePrefixL = new JLabel("Text prefix for sub mission instance's variables?");
        variablePrefixTF = new JTextField("");
        variablePrefixTF.addKeyListener(activityListener);
        variablePrefixTF.addFocusListener(activityListener);
        variablePrefixTF.addMouseListener(activityListener);
        addComponent(variablePrefixL);
        addComponent(variablePrefixTF);

        // Combo box for choosing task allocation method
        instantiationMethodL = new JLabel("Shared or individual sub-mission instances?");
        instantiationMethodCB = new JComboBox(InstantiationMethod.values());
        selectedInstantiationMethod = (InstantiationMethod) instantiationMethodCB.getSelectedItem();
        instantiationMethodCB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (selectedInstantiationMethod == (InstantiationMethod) instantiationMethodCB.getSelectedItem()) {
                    return;
                }
                selectedInstantiationMethod = (InstantiationMethod) instantiationMethodCB.getSelectedItem();
            }
        });
        addComponent(instantiationMethodL);
        addComponent(instantiationMethodCB);

        // Combo box for choosing task allocation method
        allocationMethodL = new JLabel("Task mapping method?");
        taskMapMethodCB = new JComboBox(TaskMapMethod.values());
        selectedTaskMapMethod = (TaskMapMethod) taskMapMethodCB.getSelectedItem();
        taskMapMethodCB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (selectedTaskMapMethod == (TaskMapMethod) taskMapMethodCB.getSelectedItem()) {
                    return;
                }
                selectedTaskMapMethod = (TaskMapMethod) taskMapMethodCB.getSelectedItem();
                updateTaskMapping();
            }
        });
        addComponent(allocationMethodL);
        addComponent(taskMapMethodCB);

        // Combo boxes for mapping task tokens to sub mission (if appropriate)
        taskMappingL = new JLabel();
        taskMappingP = new JPanel(new BorderLayout());
        addComponent(taskMappingP);

        // Done
        okB = new javax.swing.JButton("OK");
        okB.setEnabled(false);
        okB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        addComponent(okB);

        // Cancel
        cancelB = new javax.swing.JButton("Cancel");
        cancelB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
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
        validate();
    }

    private void checkValidity() {
        valid = !namePrefixTF.getText().equals("") && !variablePrefixTF.getText().equals("");
        okB.setEnabled(valid);
    }

    private void updateTaskMapping() {
        if (selectedTaskMapMethod == TaskMapMethod.Event) {
            taskMappingP.removeAll();
            validate();
            return;
        }
        taskMappingP.removeAll();
        taskMappingL.setText("Task mapping from parent mission \"" + parentMSpec.getName() + "\" to sub mission instance of \"" + selectedMSpec.getName() + "\"");
        taskMappingP.add(taskMappingL);
        JPanel taskMappingContentP = new JPanel();
        taskMappingContentP.setLayout(new GridLayout(0, 2));
        taskNameToL.clear();
        taskNameToCB.clear();
        for (TaskSpecification childTaskSpec : selectedMSpec.getTaskSpecList()) {
            JPanel taskP = new JPanel();
            JLabel taskL = new JLabel(childTaskSpec.toString());
            taskNameToL.put(childTaskSpec, taskL);
            taskMappingContentP.add(taskL);
            JComboBox taskMapCB = new JComboBox(parentMSpec.getTaskSpecList().toArray());
            taskNameToCB.put(childTaskSpec, taskMapCB);
            taskMappingContentP.add(taskMapCB);
            taskMappingP.add(taskP);
        }
        taskMappingP.add(taskMappingContentP);
        validate();
    }

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        okExit = true;
        setVisible(false);
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        valid = false;
        setVisible(false);
    }

    public MissionPlanSpecification getSelectedMission() {
        return selectedMSpec;
    }

    public boolean getIsSharedInstantiation() {
        return selectedInstantiationMethod == InstantiationMethod.Shared;
    }

    public TaskMapMethod getTaskMapMethod() {
        return selectedTaskMapMethod;
    }

    public HashMap<TaskSpecification, TaskSpecification> getTaskMapping() {
        HashMap<TaskSpecification, TaskSpecification> taskMapping = new HashMap<TaskSpecification, TaskSpecification>();
        for (TaskSpecification childTaskSpec : selectedMSpec.getTaskSpecList()) {
            taskMapping.put(childTaskSpec, (TaskSpecification) taskNameToCB.get(childTaskSpec).getSelectedItem());
        }
        return taskMapping;
    }

    public String getNamePrefix() {
        return namePrefixTF.getText();
    }

    public String getVariablePrefix() {
        return variablePrefixTF.getText();
    }

    public boolean confirmedExit() {
        return okExit;
    }

    class ActivityListener implements KeyListener, FocusListener, MouseListener {

        @Override
        public void keyTyped(KeyEvent ke) {
            checkValidity();
        }

        @Override
        public void keyPressed(KeyEvent ke) {
        }

        @Override
        public void keyReleased(KeyEvent ke) {
        }

        @Override
        public void focusGained(FocusEvent fe) {
            checkValidity();
        }

        @Override
        public void focusLost(FocusEvent fe) {
            checkValidity();
        }

        @Override
        public void mouseClicked(MouseEvent me) {
        }

        @Override
        public void mousePressed(MouseEvent me) {
        }

        @Override
        public void mouseReleased(MouseEvent me) {
        }

        @Override
        public void mouseEntered(MouseEvent me) {
            checkValidity();
        }

        @Override
        public void mouseExited(MouseEvent me) {
            checkValidity();
        }
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                EditSubMissionD dialog = new EditSubMissionD(new javax.swing.JFrame(), true, null);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
}
