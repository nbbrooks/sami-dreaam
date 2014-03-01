package dreaam.developer;

import sami.event.InputEvent;
import sami.event.ReflectedEventSpecification;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.ScrollPane;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import sami.event.Event;
import sami.markup.Markup;
import sami.uilanguage.MarkupComponent;

/**
 * Used to receive parameters from operator needed by an Output event
 *
 * @author pscerri
 */
public class ReflectedEventD extends javax.swing.JDialog {

    private enum EventType {

        INPUT, OUTPUT
    };
    private final static Logger LOGGER = Logger.getLogger(ReflectedEventD.class.getName());
    private final static int BUTTON_HEIGHT = 50;
    private final static int BORDER = 5;
    private int maxComponentWidth = 100;
    private ScrollPane scrollPane;
    private JPanel paramsPanel;
    private JButton okButton;
    // Class held by event spec
    private Class eventClass = null;
    private EventType eventType;
    // Maps each field in the event to a variable string name or value
    private HashMap<String, Object> fieldNameToDefinition;
    private HashMap<Field, MarkupComponent> fieldToValueComponent;
    private HashMap<Field, JComboBox> fieldToVariableComboBox;
    private HashMap<Field, JTextField> fieldToVariableTextField;
    private final ReflectedEventSpecification eventSpec;

    /**
     * Creates new form ReflectedEventD
     */
    public ReflectedEventD(ReflectedEventSpecification eventSpec, java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        this.eventSpec = eventSpec;
        try {
            Class eventClass = Class.forName(eventSpec.getClassName());
            if (InputEvent.class.isAssignableFrom(eventClass)) {
                // Input event
                eventType = ReflectedEventD.EventType.INPUT;
            } else {
                // Output event
                eventType = ReflectedEventD.EventType.OUTPUT;
            }
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        fieldToValueComponent = new HashMap<Field, MarkupComponent>();
        fieldToVariableComboBox = new HashMap<Field, JComboBox>();
        fieldToVariableTextField = new HashMap<Field, JTextField>();

        try {
            eventClass = Class.forName(eventSpec.getClassName());
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        initComponents();
        setTitle("ReflectedEventD");
    }

    protected void addParamComponents() {
        try {
            ArrayList<String> fieldNames = (ArrayList<String>) (eventClass.getField("fieldNames").get(null));
            HashMap<String, String> fieldNameToDescription = (HashMap<String, String>) (eventClass.getField("fieldNameToDescription").get(null));
            LOGGER.log(Level.INFO, "ReflectedEventD adding fields for " + eventSpec + ", fields: " + fieldNames.toString());

            for (String fieldName : fieldNames) {
                final Field field = eventClass.getField(fieldName);
                JPanel fieldPanel = new JPanel();
                fieldPanel.setLayout(new GridBagLayout());
                fieldPanel.setBorder(BorderFactory.createLineBorder(Color.black));

                GridBagConstraints constraints = new GridBagConstraints();
                constraints.fill = GridBagConstraints.HORIZONTAL;
                constraints.gridy = 0;
                constraints.gridx = 0;
                constraints.weightx = 1.0;

                // Add description for this field
                JLabel description = new JLabel(fieldNameToDescription.get(fieldName), SwingConstants.LEFT);
                description.setMaximumSize(new Dimension(Integer.MAX_VALUE, description.getPreferredSize().height));
                fieldPanel.add(description, constraints);
                constraints.gridy = constraints.gridy + 1;

                // Field definition components
                if (eventType == ReflectedEventD.EventType.INPUT) {
                    // Add text field for setting variable
                    addVariableTextField(field, fieldNameToDefinition, fieldPanel, constraints);
                    constraints.gridy = constraints.gridy + 1;
                } else if (eventType == ReflectedEventD.EventType.OUTPUT) {
                    // Add combo box for selecting variable name
                    addVariableComboBox(field, fieldNameToDefinition, fieldPanel, constraints);
                    constraints.gridy = constraints.gridy + 1;
                    // Add component for defining value
                    addValueComponent(field, fieldNameToDefinition, fieldPanel, constraints);
                    constraints.gridy = constraints.gridy + 1;
                }

                maxComponentWidth = Math.max(maxComponentWidth, fieldPanel.getPreferredSize().width);
                paramsPanel.add(fieldPanel, constraints);
                constraints.gridy = constraints.gridy + 1;

                // Add space between each enum's interaction area
                paramsPanel.add(Box.createRigidArea(new Dimension(0, 25)), constraints);
                constraints.gridy = constraints.gridy + 1;
            }

        } catch (NoSuchFieldException ex) {
            Logger.getLogger(ReflectedEventD.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(ReflectedEventD.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ReflectedEventD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void addVariableTextField(Field field, HashMap<String, Object> fieldNameToDefinition, JPanel panel, GridBagConstraints constraints) {
        JTextField textField = new JTextField();

        Object fieldDefinition = fieldNameToDefinition.get(field.getName());
        if (fieldDefinition != null && fieldDefinition instanceof String && ((String) fieldDefinition).startsWith("@")) {
            // This field is already defined with a variable name
            textField.setText((String) fieldDefinition);
        }
        fieldToVariableTextField.put(field, textField);
        maxComponentWidth = Math.max(maxComponentWidth, textField.getPreferredSize().width);
        panel.add(textField, constraints);
    }

    protected void addVariableComboBox(Field field, HashMap<String, Object> fieldNameToDefinition, JPanel panel, GridBagConstraints constraints) {
        ArrayList<String> existingVariables = null;
        try {
            existingVariables = (ArrayList<String>) (new Mediator()).getAllVariables().clone();
        } catch (NullPointerException e) {
            existingVariables = new ArrayList<String>();
        }
        existingVariables.add(0, Event.NONE);
        JComboBox comboBox = new JComboBox(existingVariables.toArray());

        Object fieldObject = fieldNameToDefinition.get(field.getName());
        if (fieldObject != null && fieldObject instanceof String && ((String) fieldObject).startsWith("@")) {
            // This field is already defined with a variable name
            if (existingVariables.contains((String) fieldObject)) {
                comboBox.setSelectedItem((String) fieldObject);
            }
        }
        fieldToVariableComboBox.put(field, comboBox);
        maxComponentWidth = Math.max(maxComponentWidth, comboBox.getPreferredSize().width);
        panel.add(comboBox, constraints);
    }

    protected void addValueComponent(Field field, HashMap<String, Object> fieldNameToDefinition, JPanel panel, GridBagConstraints constraints) {
        MarkupComponent markupComponent = UiComponentGenerator.getInstance().getCreationComponent((Type)field.getType(), new ArrayList<Markup>());
        JComponent visualization = null;
        if (markupComponent != null && markupComponent.getComponent() != null) {
            visualization = markupComponent.getComponent();
            Object definition = fieldNameToDefinition.get(field.getName());
            if (definition != null) {
                // This field already has been defined with a value
                // Earlier we had to replace primitive fields with their wrapper object, take that into account here
                if (definition.getClass().equals(field.getType())
                        || (definition.getClass().equals(Double.class) && field.getType().equals(double.class))
                        || (definition.getClass().equals(Float.class) && field.getType().equals(float.class))
                        || (definition.getClass().equals(Integer.class) && field.getType().equals(int.class))
                        || (definition.getClass().equals(Long.class) && field.getType().equals(long.class))) {
                    UiComponentGenerator.getInstance().setComponentValue(markupComponent, definition);
                }
            }
            fieldToValueComponent.put(field, markupComponent);
        } else {
            // There is no component that can be used to define a value for this field
            // The field can only be set to a variable name
            // Show a message for now, may remove this later...
            visualization = new JLabel("No component", SwingConstants.LEFT);
        }
        maxComponentWidth = Math.max(maxComponentWidth, visualization.getPreferredSize().width);
        maxComponentWidth = Math.max(maxComponentWidth, visualization.getPreferredSize().width);
        panel.add(visualization, constraints);
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        // Get previously set field definitions
        fieldNameToDefinition = eventSpec.getFieldDefinitions();
        if (fieldNameToDefinition == null) {
            fieldNameToDefinition = new HashMap<String, Object>();
        }
        paramsPanel = new javax.swing.JPanel();
        paramsPanel.setLayout(new GridBagLayout());
        addParamComponents();
        paramsPanel.revalidate();

        scrollPane = new ScrollPane();
        scrollPane.add(paramsPanel);
        scrollPane.setPreferredSize(paramsPanel.getPreferredSize());

        okButton = new javax.swing.JButton();
        okButton.setText("OK");
        okButton.setPreferredSize(new Dimension(maxComponentWidth, BUTTON_HEIGHT));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        BorderLayout okLayout = new BorderLayout(BORDER, BORDER);
        JPanel okPanel = new JPanel(okLayout);
        okPanel.setPreferredSize(new Dimension(maxComponentWidth, BUTTON_HEIGHT));
        okPanel.setMaximumSize(new Dimension(maxComponentWidth, BUTTON_HEIGHT));
        okPanel.add(okButton);

        BoxLayout paneLayout = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
        getContentPane().setLayout(paneLayout);
        getContentPane().setPreferredSize(new Dimension(maxComponentWidth + 8 * BORDER, 600));
        getContentPane().add(scrollPane);
        getContentPane().add(okPanel);
        pack();
    }

    /**
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
        HashMap<String, Object> fieldNameToDefinition = getDefinitionsFromComponents();
        eventSpec.setFieldDefinitions(fieldNameToDefinition);
    }

    private HashMap<String, Object> getDefinitionsFromComponents() {
        HashMap<String, Object> fieldNameToObject = new HashMap<String, Object>();
        if (eventType == ReflectedEventD.EventType.INPUT) {
            for (Field field : fieldToVariableTextField.keySet()) {
                Object definition = null;
                JTextField variableTextField = fieldToVariableTextField.get(field);
                if (variableTextField != null) {
                    String variable = variableTextField.getText().trim();
                    if (variable.length() > 0 && !variable.startsWith("@")) {
                        variable = "@" + variable;
                    }
                    // Store the value from the component unless nothing was entered
                    if (variable.length() > 0) {
                        definition = variable;
                    }
                }
                fieldNameToObject.put(field.getName(), definition);
            }
        } else if (eventType == ReflectedEventD.EventType.OUTPUT) {
            for (Field field : fieldToVariableComboBox.keySet()) {
                Object definition = null;
                JComboBox variableComboBox = fieldToVariableComboBox.get(field);
                if (!variableComboBox.getSelectedItem().toString().equalsIgnoreCase(Event.NONE)) {
                    // User selected a variable other than NONE to define the field
                    definition = variableComboBox.getSelectedItem().toString();
                } else {
                    // User selected NONE for variable, now see if they created a value definition
                    MarkupComponent markupComponent = fieldToValueComponent.get(field);
                    if (markupComponent != null) {
                        // Store the value from the component
                        definition = UiComponentGenerator.getInstance().getComponentValue(markupComponent, field);
                    }
                }
                fieldNameToObject.put(field.getName(), definition);
            }
        }

        return fieldNameToObject;
    }

    public static void main(String argv[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {

                ReflectedEventD dialog = new ReflectedEventD(new ReflectedEventSpecification("crw.event.output.ui.DisplayMessage"), null, true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
}
