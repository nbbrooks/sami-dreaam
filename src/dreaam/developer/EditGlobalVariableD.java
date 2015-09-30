package dreaam.developer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import sami.markup.Markup;
import sami.uilanguage.MarkupComponent;

/**
 * Dialogue for specifying a global variable: (variable name, class, and value).
 *
 * For classes with no MarkupComponent supporting its creation, we allow
 * recursion into the class's fields to get components supporting those fields
 * (up to a certain depth)
 *
 * @author nbb
 */
public class EditGlobalVariableD extends javax.swing.JDialog {

    private final static Logger LOGGER = Logger.getLogger(EditGlobalVariableD.class.getName());
    private final static int BUTTON_WIDTH = 250;
    private final static int BUTTON_HEIGHT = 50;
    private JScrollPane scrollPane;
    private JPanel paramsP;

    private HashMap<Field, MarkupComponent> fieldToComponent;

    private Object value = null;

    MarkupComponent rootComponent;

    boolean foundComponents, confirmedExit = false;

    // Text field for specifying variable name
    JLabel nameL;
    JTextField nameTF;
    String name;
    // Combo box for selecting variable class
    JLabel classL;
    JComboBox classCB;
    Class selectedClass;
    // Done/cancel
    private JButton okB, cancelB;

    private ActivityListener activityListener = new ActivityListener();

    /**
     * Creates new form ReflectedEventD
     */
    public EditGlobalVariableD(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        rootComponent = null;
        fieldToComponent = new HashMap<Field, MarkupComponent>();

        initComponents();
        setTitle("EditGlobalVariableD");
    }

    public EditGlobalVariableD(java.awt.Frame parent, boolean modal, String variableName, Object variableValue) {
//        this(parent, modal);

        super(parent, modal);
        this.name = variableName;
        this.value = variableValue;
        rootComponent = null;
        fieldToComponent = new HashMap<Field, MarkupComponent>();

        initComponents();
        setTitle("EditGlobalVariableD");

        if (variableName != null && variableValue != null) {
            selectedClass = variableValue.getClass();
            nameTF.setText(variableName);
            classCB.setSelectedItem(variableValue.getClass());
        }
    }

    protected void addParamComponents() {
        if (selectedClass == null) {
            return;
        }

        // Make sure we have an instance of the class
        boolean newInstance = value == null;
        if (value == null) {
            try {
                value = selectedClass.newInstance();
            } catch (InstantiationException ex) {
                // Class has no default constructor, usually indicative of it being a primitive
            } catch (IllegalAccessException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                return;
            }
        }

        // Get either
        //  1- Single component which supports creation of the root class
        //  2- Multiple components which support creation of all the fields and sub-fields (if necessary) within the recursion limit
        foundComponents = true;

        GridBagConstraints paramsConstraints = new GridBagConstraints();
        paramsConstraints.gridx = 0;
        paramsConstraints.gridy = 0;
        paramsConstraints.fill = GridBagConstraints.BOTH;
        paramsConstraints.weightx = 1.0;
        paramsConstraints.weighty = 1.0;

        rootComponent = getRootValueComponent(selectedClass, value, newInstance);
        if (rootComponent != null && rootComponent.getComponent() != null) {
            // Option 1- Single component which supports creation of the root class
            paramsP.add(rootComponent.getComponent(), paramsConstraints);
            paramsConstraints.gridy = paramsConstraints.gridy + 1;
        } else {
            // Try option 2- Multiple components which support creation of all the fields and sub-fields (if necessary) within the recursion limit

            // Create JPanel to house sub-field's components
            JPanel recursionPanel = new JPanel();
            recursionPanel.setLayout(new GridBagLayout());
            recursionPanel.setBorder(BorderFactory.createLineBorder(Color.black));

            GridBagConstraints recursionConstraints = new GridBagConstraints();
            recursionConstraints.gridx = 0;
            recursionConstraints.gridy = 0;
            recursionConstraints.fill = GridBagConstraints.BOTH;
            recursionConstraints.weightx = 1.0;
            recursionConstraints.weighty = 1.0;

            JLabel recursionL = new JLabel(selectedClass.getSimpleName());
            recursionPanel.add(recursionL, recursionConstraints);
            recursionConstraints.gridy = recursionConstraints.gridy + 1;

            Field[] classFields = selectedClass.getDeclaredFields();
            for (Field classField : classFields) {
                try {
                    addValueComponent(classField, fieldToComponent, recursionPanel, recursionConstraints, 0);
                } catch (IllegalArgumentException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
            if (foundComponents) {
                // Was able to find components for all subfields without hitting recursion limit
                paramsP.add(recursionPanel, paramsConstraints);
                paramsConstraints.gridy = paramsConstraints.gridy + 1;
            }
        }
    }

    protected void addValueComponent(Field field, HashMap<Field, MarkupComponent> fieldToComponent, JPanel panel, GridBagConstraints constraints, int recursionDepth) {
        JComponent visualization = null;
        if (recursionDepth > 3) {
            visualization = new JLabel(field.getName() + " (" + field.getType().getSimpleName() + "): No component - max recursion", SwingConstants.LEFT);
            foundComponents = false;
        } else {
            MarkupComponent markupComponent = getValueComponent(field, value);

            if (markupComponent != null && markupComponent.getComponent() != null) {
                visualization = markupComponent.getComponent();
                fieldToComponent.put(field, markupComponent);
            } else {
                // There is no component that can directly be used to define a value for this field
                //  Recursively add components for its fields, if possible
                Class c;
                try {
                    c = Class.forName(field.getType().getTypeName());
                    if (Map.class.isAssignableFrom(c)
                            || List.class.isAssignableFrom(c)
                            || Class.class.isAssignableFrom(c)
                            || Hashtable.class.isAssignableFrom(c)) {
                        // Can't do these yet
                        visualization = new JLabel(field.getName() + " (" + field.getType().getSimpleName() + "): No component - unsupported", SwingConstants.LEFT);
                        foundComponents = false;
                    } else {
                        // Create JPanel to house sub-field's components
                        JPanel recursionPanel = new JPanel();
                        recursionPanel.setLayout(new GridBagLayout());
                        recursionPanel.setBorder(BorderFactory.createLineBorder(Color.black));

                        GridBagConstraints recursionConstraints = new GridBagConstraints();
                        recursionConstraints.gridx = 0;
                        recursionConstraints.gridy = 0;
                        recursionConstraints.fill = GridBagConstraints.BOTH;
                        recursionConstraints.weightx = 1.0;
                        recursionConstraints.weighty = 1.0;

                        JLabel recursionL = new JLabel(field.getName() + " (" + field.getType().getSimpleName() + ")");
                        recursionPanel.add(recursionL, recursionConstraints);
                        recursionConstraints.gridy = recursionConstraints.gridy + 1;

                        Field[] classFields = c.getDeclaredFields();
                        for (Field classField : classFields) {

                            //@todo make addValueComponent return boolean?
                            addValueComponent(classField, fieldToComponent, recursionPanel, recursionConstraints, 0);
                            if (!foundComponents) {
                                return;
                            }
                        }
                        visualization = recursionPanel;
                    }
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
        }
        panel.add(new JLabel(field.getName()), constraints);
        constraints.gridy = constraints.gridy + 1;
        panel.add(visualization, constraints);
        constraints.gridy = constraints.gridy + 1;
    }

    protected MarkupComponent getValueComponent(Field field, Object value) {
        MarkupComponent markupComponent = UiComponentGenerator.getInstance().getCreationComponent((java.lang.reflect.Type) field.getType(), field, new ArrayList<Markup>(), null, null);
        if (markupComponent != null && markupComponent.getComponent() != null) {
            Object definition;
            try {
                definition = field.get(value);
                if (definition != null) {
                    // This field already has been defined with a value
                    // Earlier we had to replace primitive fields with their wrapper object, take that into account here
                    if (definition.getClass().equals(field.getType())
                            || (definition.getClass().equals(Double.class) && field.getType().equals(double.class))
                            || (definition.getClass().equals(Float.class) && field.getType().equals(float.class))
                            || (definition.getClass().equals(Integer.class) && field.getType().equals(int.class))
                            || (definition.getClass().equals(Long.class) && field.getType().equals(long.class))
                            || (definition.getClass().equals(Boolean.class) && field.getType().equals(boolean.class))) {
                        UiComponentGenerator.getInstance().setComponentValue(markupComponent, definition);
                    }
                }
            } catch (IllegalArgumentException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        return markupComponent;
    }

    protected MarkupComponent getRootValueComponent(Class valueClass, Object value, boolean newInstance) {
        MarkupComponent markupComponent = UiComponentGenerator.getInstance().getCreationComponent(valueClass, null, new ArrayList<Markup>(), null, null);
        if (markupComponent != null && markupComponent.getComponent() != null) {
            if (value != null) {
                // This field already has been defined with a value
                // Earlier we had to replace primitive fields with their wrapper object, take that into account here
                if (!newInstance
                        && (value.getClass().equals(valueClass)
                        || (value.getClass().equals(Double.class) && valueClass.equals(double.class))
                        || (value.getClass().equals(Float.class) && valueClass.equals(float.class))
                        || (value.getClass().equals(Integer.class) && valueClass.equals(int.class))
                        || (value.getClass().equals(Long.class) && valueClass.equals(long.class))
                        || (value.getClass().equals(Boolean.class) && valueClass.equals(boolean.class)))) {
                    UiComponentGenerator.getInstance().setComponentValue(markupComponent, value);
                }
            }
        }
        return markupComponent;
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        getContentPane().setLayout(new BorderLayout());

        JPanel definitionP = new JPanel(new GridBagLayout());
        GridBagConstraints definitionConstraints = new GridBagConstraints();
        definitionConstraints.gridx = 0;
        definitionConstraints.gridy = 0;
        definitionConstraints.fill = GridBagConstraints.BOTH;
        definitionConstraints.weightx = 1.0;
        definitionConstraints.weighty = 1.0;

        // Text field for specifying variable name
        nameL = new JLabel("Variable name?");
        definitionP.add(nameL, definitionConstraints);
        definitionConstraints.gridy = definitionConstraints.gridy + 1;

        nameTF = new JTextField("");
        nameTF.addKeyListener(activityListener);
        nameTF.addFocusListener(activityListener);
        nameTF.addMouseListener(activityListener);
        definitionP.add(nameTF, definitionConstraints);
        definitionConstraints.gridy = definitionConstraints.gridy + 1;

        // Combo box for choosing variable class
        classL = new JLabel("Variable class?");
        definitionP.add(classL, definitionConstraints);
        definitionConstraints.gridy = definitionConstraints.gridy + 1;

        ArrayList<Class> creationClasses = UiComponentGenerator.getInstance().getCreationClasses();
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
                value = null;
                rootComponent = null;
                fieldToComponent.clear();
                lookForCreationComponents();
            }
        });
        definitionP.add(classCB, definitionConstraints);
        definitionConstraints.gridy = definitionConstraints.gridy + 1;

        // Component(s) for defining the variable
        paramsP = new javax.swing.JPanel();
        paramsP.setLayout(new GridBagLayout());
        addParamComponents();
        scrollPane = new JScrollPane(paramsP);
        definitionP.add(scrollPane, definitionConstraints);
        definitionConstraints.gridy = definitionConstraints.gridy + 1;

        JPanel buttonsP = new JPanel(new GridBagLayout());
        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridx = 0;
        buttonConstraints.gridy = 0;
        buttonConstraints.fill = GridBagConstraints.BOTH;
        buttonConstraints.weightx = 1.0;
        buttonConstraints.weighty = 1.0;

        okB = new javax.swing.JButton("OK");
        okB.addKeyListener(activityListener);
        okB.addFocusListener(activityListener);
        okB.addMouseListener(activityListener);
        okB.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        okB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        buttonsP.add(okB, buttonConstraints);
        buttonConstraints.gridy = buttonConstraints.gridy + 1;

        cancelB = new javax.swing.JButton("Cancel");
        cancelB.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        cancelB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        buttonsP.add(cancelB, buttonConstraints);
        buttonConstraints.gridy = buttonConstraints.gridy + 1;

        getContentPane().add(definitionP, BorderLayout.NORTH);
        getContentPane().add(buttonsP, BorderLayout.SOUTH);

        // Adjust dialog size
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenHeight = gd.getDisplayMode().getHeight();
        setPreferredSize(new Dimension(getPreferredSize().width, (int) (screenHeight * 0.9)));

        pack();
    }

    private void lookForCreationComponents() {
        paramsP.removeAll();
        addParamComponents();
        paramsP.revalidate();
        scrollPane.revalidate();
        validate();
    }

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        value = getVariableValue();
        name = nameTF.getText();
        confirmedExit = true;
        setVisible(false);
    }

    public boolean confirmedExit() {
        return confirmedExit;
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
    }

    public String getVariableName() {
        return name;
    }

    public Object getVariableValue() {
        if (!foundComponents) {
            return null;
        }
        if (rootComponent != null) {
            // We could directly create a single component for the selected class
            return UiComponentGenerator.getInstance().getComponentValue(rootComponent, selectedClass);
        } else {
            try {
                // We had to recurse into the fields of the selected class and create multiple components
                Object setValue = selectedClass.newInstance();
                for (Field field : selectedClass.getDeclaredFields()) {
                    // We have a component for this specific field
                    if (fieldToComponent.containsKey(field)) {
                        MarkupComponent markupComponent = fieldToComponent.get(field);
                        if (markupComponent != null) {
                            // Store the value from the component
                            Object subValue = UiComponentGenerator.getInstance().getComponentValue(markupComponent, field.getType());
                            if (subValue == null) {
                                return null;
                            }
                            field.set(setValue, subValue);
                        } else {
                            return null;
                        }
                    } else {
                        // We had to recurse into this field to get components - so now we recurse to get the values
                        Object subObject = field.getType().newInstance();
                        field.set(setValue, subObject);
                        getSubFieldValues(field, subObject);
                    }
                }
                // All field components had definitions
                return setValue;
            } catch (InstantiationException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    public boolean getSubFieldValues(Field field, Object value) {
        for (Field subField : value.getClass().getDeclaredFields()) {
            try {
                // We have a component for this specific field
                if (fieldToComponent.containsKey(subField)) {
                    MarkupComponent markupComponent = fieldToComponent.get(subField);
                    if (markupComponent != null) {
                        // Store the value from the component
                        Object subValue = UiComponentGenerator.getInstance().getComponentValue(markupComponent, subField.getType());
                        if (subValue == null) {
                            return false;
                        } else {
                            subField.set(value, subValue);
                        }
                    } else {
                        return false;
                    }
                } else {
                    // We had to recurse into this field to get components - so now we recurse to get the values
                    try {
                        Object subObject = subField.getType().newInstance();
                        subField.set(value, subObject);
                        getSubFieldValues(subField, subObject);
                    } catch (InstantiationException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                }
            } catch (IllegalAccessException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    private void checkValidity() {
        boolean valid = !nameTF.getText().equals("") && valueDefined();
        okB.setEnabled(valid);
    }

    public boolean valueDefined() {
        if (rootComponent != null) {
            // We could directly create a single component for the selected class
            if (UiComponentGenerator.getInstance().getComponentValue(rootComponent, selectedClass) != null) {
                return true;
            }
        } else {
            // We had to recurse into the fields of the selected class and create multiple components
            for (Field field : fieldToComponent.keySet()) {
                MarkupComponent markupComponent = fieldToComponent.get(field);
                if (markupComponent != null) {
                    // Store the value from the component
                    Object value = UiComponentGenerator.getInstance().getComponentValue(markupComponent, field.getType());
                    if (value == null) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            // All field components had definitions
            return true;
        }
        return false;
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
