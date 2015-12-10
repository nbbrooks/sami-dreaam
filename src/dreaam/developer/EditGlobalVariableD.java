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
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import javax.swing.SwingConstants;
import sami.markup.Markup;
import sami.uilanguage.MarkupComponent;
import sami.variable.Variable.ClassQuantity;

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
    private JPanel paramsP, listP;

    private Object value = null;

    MarkupComponent rootComponentSingle;
    ArrayList<MarkupComponent> rootComponentList = new ArrayList<MarkupComponent>();

    HashMap<Field, MarkupComponent> fieldToComponentSingle = new HashMap<Field, MarkupComponent>();
    ArrayList<HashMap<Field, MarkupComponent>> fieldToComponentList = new ArrayList<HashMap<Field, MarkupComponent>>();

    boolean foundComponents, confirmedExit = false;

    // Text field for specifying variable name
    JLabel nameL;
    JTextField nameTF;
    String name;
    // Combo box for selecting variable class
    JLabel classL;
    JComboBox classCB;
    Class selectedClass;
    // Combo box for selecting if the definition is a single instance of the class or a list of the class
    JLabel singleOrListL;
    JComboBox classQuantityCB;
    ClassQuantity selectedClassQuantity;
    // Done/cancel
    private JButton okB, cancelB;

    private ActivityListener activityListener = new ActivityListener();

    ComplexMarkupCreationComponentP componentP;

    public EditGlobalVariableD(java.awt.Frame parent, boolean modal) {
        super(parent, modal);

        initChoiceComponent();
        selectedClassQuantity = (ClassQuantity) classQuantityCB.getSelectedItem();
        selectedClass = (Class) classCB.getSelectedItem();
        initDefinitionComponent();
        setTitle("EditGlobalVariableD");
    }

    public EditGlobalVariableD(java.awt.Frame parent, boolean modal, String variableName, Object variableValue) {
        super(parent, modal);
        this.name = variableName;
        this.value = variableValue;

        setTitle("EditGlobalVariableD");
        initChoiceComponent();

        if (variableName != null && variableValue != null) {

            nameTF.setText(variableName);

            if (variableValue instanceof ArrayList) {
                if (((ArrayList) variableValue).isEmpty()) {
                    LOGGER.severe("Global variable of type ArrayList is empty, cannot determine inner class type");
                    selectedClass = null;
                } else {
                    selectedClass = ((ArrayList) variableValue).get(0).getClass();
                }
                selectedClassQuantity = ClassQuantity.ARRAY_LIST;
            } else {
                selectedClass = variableValue.getClass();
                selectedClassQuantity = ClassQuantity.SINGLE;
            }
            classCB.setSelectedItem(selectedClass);
            classQuantityCB.setSelectedItem(selectedClassQuantity);
        } else {
            selectedClassQuantity = (ClassQuantity) classQuantityCB.getSelectedItem();
            selectedClass = (Class) classCB.getSelectedItem();
        }

        initDefinitionComponent();
    }

    protected void addComplexMarkupComponent() {
        if (selectedClass == null || selectedClassQuantity == null) {
            return;
        }
        componentP = new ComplexMarkupCreationComponentP(selectedClassQuantity, selectedClass, value);

        GridBagConstraints paramsConstraints = new GridBagConstraints();
        paramsConstraints.gridx = 0;
        paramsConstraints.gridy = 0;
        paramsConstraints.fill = GridBagConstraints.BOTH;
        paramsConstraints.weightx = 1.0;
        paramsConstraints.weighty = 1.0;

        paramsP.add(componentP, paramsConstraints);
        paramsConstraints.gridy = paramsConstraints.gridy + 1;

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

    /**
     * Create all the labels, text fields, and combo boxes used to provide
     * information about a variable, but not its definition.
     */
    private void initChoiceComponent() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

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

        // Combo box for selecting if we want a single definition of the class or a list of the class
        // Don't add yet
        singleOrListL = new JLabel("Single definition or list?");

        classQuantityCB = new JComboBox(ClassQuantity.values());
        classQuantityCB.setMaximumSize(new Dimension(Integer.MAX_VALUE, classQuantityCB.getPreferredSize().height));

        // Panel for containing components for each item in the list, which is added to paramsP
        listP = new javax.swing.JPanel();
        listP.setLayout(new GridBagLayout());

        // Combo box for choosing variable class
        classL = new JLabel("Variable class?");
        definitionP.add(classL, definitionConstraints);
        definitionConstraints.gridy = definitionConstraints.gridy + 1;

        ArrayList<Class> creationClasses = UiComponentGenerator.getInstance().getCreationClasses();
        classCB = new JComboBox(creationClasses.toArray());
        classCB.setMaximumSize(new Dimension(Integer.MAX_VALUE, classCB.getPreferredSize().height));
        definitionP.add(classCB, definitionConstraints);
        definitionConstraints.gridy = definitionConstraints.gridy + 1;

        // Now add singleOrListL and singleOrListCB to paramsP
        definitionP.add(singleOrListL, definitionConstraints);
        definitionConstraints.gridy = definitionConstraints.gridy + 1;
        definitionP.add(classQuantityCB, definitionConstraints);
        definitionConstraints.gridy = definitionConstraints.gridy + 1;

        // Component(s) for defining the variable
        paramsP = new javax.swing.JPanel();
        paramsP.setLayout(new GridBagLayout());
        scrollPane = new JScrollPane(paramsP);
        scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
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

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buttonsP, BorderLayout.SOUTH);
        getContentPane().add(definitionP, BorderLayout.NORTH);

        // Adjust dialog size
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenHeight = gd.getDisplayMode().getHeight();
        // Pad width a bit so horizontal scrollbar isn't needed
        setPreferredSize(new Dimension(getPreferredSize().width + 50, (int) (screenHeight * 0.9)));

        pack();
    }

    /**
     * Now that any pre-existing knowledge about the variable has been loaded
     * into the choice components, add listeners and create the
     * ComplexMarkupComponentP.
     */
    private void initDefinitionComponent() {
        classQuantityCB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (selectedClassQuantity == (ClassQuantity) classQuantityCB.getSelectedItem()) {
                    return;
                }
                selectedClassQuantity = (ClassQuantity) classQuantityCB.getSelectedItem();
                value = null;
                rootComponentSingle = null;
                rootComponentList.clear();
                fieldToComponentSingle.clear();
                fieldToComponentList.clear();
                lookForCreationComponents();
            }
        });

        classCB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (selectedClass == (Class) classCB.getSelectedItem()) {
                    return;
                }
                selectedClass = (Class) classCB.getSelectedItem();
                value = null;
                rootComponentSingle = null;
                rootComponentList.clear();
                fieldToComponentSingle.clear();
                fieldToComponentList.clear();
                lookForCreationComponents();
            }
        });

        addComplexMarkupComponent();

        revalidate();
    }

    private void lookForCreationComponents() {
        paramsP.removeAll();
        addComplexMarkupComponent();
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

    public Class getVariableClass() {
        return selectedClass;
    }

    public Object getVariableValue() {
        if (componentP != null) {
            return componentP.getValue();
        }
        return null;
    }

    public boolean isListComponentDefined(int listIndex) {
        if (!foundComponents || selectedClass == null || selectedClassQuantity != ClassQuantity.ARRAY_LIST) {
            return false;
        }
        try {
            // We had to recurse into the fields of the selected class and create multiple components
            Object setValue = selectedClass.newInstance();
            for (Field field : selectedClass.getDeclaredFields()) {
                // We have a component for this specific field
                if (fieldToComponentSingle.containsKey(field)) {
                    MarkupComponent markupComponent = fieldToComponentSingle.get(field);
                    if (markupComponent != null) {
                        // Store the value from the component
                        Object subValue = UiComponentGenerator.getInstance().getComponentValue(markupComponent, field.getType());
                        if (subValue == null) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    // We had to recurse into this field to get components - so now we recurse to get the values
                    LOGGER.info("$$$ field " + field);
                    LOGGER.info("$$$ field.getType() " + field.getType());
                    Object subObject = field.getType().newInstance();
                    field.set(setValue, subObject);
                    boolean success = getSubFieldValues(field, subObject);
                    if (!success) {
                        return false;
                    }
                }
            }
            // All field components had definitions
            return true;
        } catch (InstantiationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public boolean getSubFieldValues(Field field, Object value) {
        for (Field subField : value.getClass().getDeclaredFields()) {
            try {
                // We have a component for this specific field
                if (fieldToComponentSingle.containsKey(subField)) {
                    MarkupComponent markupComponent = fieldToComponentSingle.get(subField);
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
        if (rootComponentSingle != null) {
            // We could directly create a single component for the selected class
            if (UiComponentGenerator.getInstance().getComponentValue(rootComponentSingle, selectedClass) != null) {
                return true;
            }
        } else {
            // We had to recurse into the fields of the selected class and create multiple components
            for (Field field : fieldToComponentSingle.keySet()) {
                MarkupComponent markupComponent = fieldToComponentSingle.get(field);
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

    public static void main(String args[]) throws InstantiationException, IllegalAccessException {
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
