package dreaam.developer;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import sami.markup.Markup;
import sami.uilanguage.MarkupComponent;

/**
 *
 * @author pscerri
 */
public class EditGlobalVariableD extends javax.swing.JDialog {

    private static final Logger LOGGER = Logger.getLogger(EditGlobalVariableD.class.getName());

    private Mediator mediator = new Mediator();
    // All fields are valid?
    private boolean valid = false;
    private ActivityListener activityListener = new ActivityListener();
    // OK button used to exit the dialog?
    private boolean okExit = false;

    // Text field for specifying variable name
    JLabel nameL;
    JTextField nameTF;
    // Combo box for selecting variable class
    JLabel classL;
    JComboBox classCB;
    Class selectedClass;
    // Component used to define variable
    MarkupComponent markupComponent;
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
    private final static int BUTTON_WIDTH = 100;
    private final static int BUTTON_HEIGHT = 50;

    public EditGlobalVariableD(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setTitle("EditGlobalVariableD");
    }

    public EditGlobalVariableD(java.awt.Frame parent, boolean modal, String name, Object value) {
        // Want to edit existing variable
        this(parent, modal);
        nameTF.setText(name);
        classCB.setSelectedItem(value.getClass());
        markupComponent.setComponentValue(value);
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
        int numRows = 7;
        rowSeqGroup = layout.createSequentialGroup();
        rowParGroup1 = layout.createParallelGroup();
        colSeqGroup = layout.createSequentialGroup();
        colParGroupArr = new GroupLayout.ParallelGroup[numRows];
        row = 0;
        maxColWidth = BUTTON_WIDTH;
        cumulComponentHeight = 0;

        // Text field for specifying variable name
        nameL = new JLabel("Variable name?");
        nameTF = new JTextField("");
        nameTF.addKeyListener(activityListener);
        nameTF.addFocusListener(activityListener);
        nameTF.addMouseListener(activityListener);
        addComponent(nameL);
        addComponent(nameTF);

        // Combo box for choosing variable class
        classL = new JLabel("Variable class?");
        ArrayList<Class> creationClasses = UiComponentGenerator.getInstance().getCreationClasses();
        System.out.println(creationClasses.toString());
        classCB = new JComboBox(creationClasses.toArray());
        selectedClass = (Class) classCB.getSelectedItem();
        classCB.setMaximumSize(new Dimension(Integer.MAX_VALUE, classCB.getPreferredSize().height));
        classCB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (selectedClass == (Class) classCB.getSelectedItem()) {
                    return;
                }
                selectedClass = (Class) classCB.getSelectedItem();
                updateCreationComponent();
            }
        });
        addComponent(classL);
        addComponent(classCB);

        // Component for defining the variable
        markupComponent = UiComponentGenerator.getInstance().getCreationComponent(selectedClass, new ArrayList<Markup>());
        markupComponent.getComponent().addFocusListener(activityListener);
        markupComponent.getComponent().addMouseListener(activityListener);
        addComponent(markupComponent.getComponent());

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

    private void updateCreationComponent() {
        MarkupComponent markupComponentNew = UiComponentGenerator.getInstance().getCreationComponent(selectedClass, new ArrayList<Markup>());
        markupComponent.getComponent().addComponentListener(new ComponentListener() {

            @Override
            public void componentResized(ComponentEvent ce) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void componentMoved(ComponentEvent ce) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void componentShown(ComponentEvent ce) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void componentHidden(ComponentEvent ce) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        markupComponent.getComponent().addContainerListener(new ContainerListener() {

            @Override
            public void componentAdded(ContainerEvent ce) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void componentRemoved(ContainerEvent ce) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        markupComponent.getComponent().addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent me) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void mousePressed(MouseEvent me) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void mouseEntered(MouseEvent me) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void mouseExited(MouseEvent me) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });

        markupComponent.getComponent().removeFocusListener(activityListener);
        markupComponent.getComponent().removeMouseListener(activityListener);
        markupComponentNew.getComponent().addFocusListener(activityListener);
        markupComponentNew.getComponent().addMouseListener(activityListener);
        layout.replace(markupComponent.getComponent(), markupComponentNew.getComponent());
        markupComponent = markupComponentNew;
        validate();
    }

    private void checkValidity() {
        valid = !nameTF.getText().equals("") && UiComponentGenerator.getInstance().getComponentValue(markupComponent, selectedClass) != null;
        okB.setEnabled(valid);
    }

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        okExit = true;
        setVisible(false);
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        valid = false;
        setVisible(false);
    }

    public String getName() {
        return nameTF.getText();
    }

    public Object getValue() {
        return UiComponentGenerator.getInstance().getComponentValue(markupComponent, (Class) classCB.getSelectedItem());
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
                EditGlobalVariableD dialog = new EditGlobalVariableD(new javax.swing.JFrame(), true);
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
