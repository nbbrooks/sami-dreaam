package dreaam.developer;

import sami.event.InputEvent;
import sami.event.ReflectedEventSpecification;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

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
    final static int BUTTON_WIDTH = 100;
    final static int BUTTON_HEIGHT = 50;
    final static int BORDER = 5;
    int maxComponentWidth = 100;
    private ScrollPane scrollPane;
    private JPanel paramsPanel;
    private JButton okButton;
    // Maps each field in the event to a variable string name or value
    HashMap<String, Object> fieldNameToDefinition;
    HashMap<Field, JComponent> fieldToValueComponent;
    HashMap<Field, JComboBox> fieldToVariableComboBox;
    HashMap<Field, JTextField> fieldToVariableTextField;
    private ReflectedEventD.EventType eventType;
    private final ReflectedEventSpecification eventSpec;
    public static final int MAX_RECURSION_DEPTH = 2;

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
        fieldToValueComponent = new HashMap<Field, JComponent>();
        fieldToVariableComboBox = new HashMap<Field, JComboBox>();
        fieldToVariableTextField = new HashMap<Field, JTextField>();

        initComponents();
        setTitle("ReflectedEventD");
    }

    protected void addParamComponents(ReflectedEventSpecification eventSpec, HashMap<String, Object> fieldNameToDefinition) {
        LOGGER.log(Level.FINE, "ReflectedEventD adding fields for " + eventSpec + ", fields: " + eventSpec.getRequiredFields());

        for (Field field : eventSpec.getRequiredFields()) {
            JPanel fieldPanel = new JPanel();
            BoxLayout layout = new BoxLayout(fieldPanel, BoxLayout.Y_AXIS);
            fieldPanel.setLayout(layout);
            fieldPanel.setBorder(BorderFactory.createMatteBorder(BORDER, BORDER, BORDER, BORDER, (Color.BLACK)));
            if (!Modifier.isPublic(field.getModifiers())) {
                field.setAccessible(true);
            }
            fieldPanel.add(new JLabel(field.getName() + " (" + field.getType().getSimpleName() + ")"));
            if (eventType == ReflectedEventD.EventType.INPUT) {
                // Add text field for setting variable
                addVariableTextField(field, fieldNameToDefinition, fieldPanel);
            } else if (eventType == ReflectedEventD.EventType.OUTPUT) {
                // Add combo box for selecting variable name
                addVariableComboBox(field, fieldNameToDefinition, fieldPanel);
                // Add component for defining value
                addValueComponent(field, fieldNameToDefinition, fieldPanel);
            }
            paramsPanel.add(fieldPanel);
        }
    }

    protected void addVariableTextField(Field field, HashMap<String, Object> fieldNameToDefinition, JPanel panel) {
        JTextField textField = new JTextField();

        Object fieldDefinition = fieldNameToDefinition.get(field.getName());
        if (fieldDefinition != null && fieldDefinition instanceof String && ((String) fieldDefinition).startsWith("@")) {
            // This field is already defined with a variable name
            textField.setText((String) fieldDefinition);
        }
        fieldToVariableTextField.put(field, textField);
        panel.add(textField);
    }

    protected void addVariableComboBox(Field field, HashMap<String, Object> fieldNameToDefinition, JPanel panel) {
        ArrayList<String> existingVariables = null;
        try {
            existingVariables = (ArrayList<String>) (new Mediator()).getAllVariables().clone();
        } catch (NullPointerException e) {
            existingVariables = new ArrayList<String>();
        }
        existingVariables.add(0, ReflectedEventSpecification.NONE);
        JComboBox comboBox = new JComboBox(existingVariables.toArray());

        Object fieldObject = fieldNameToDefinition.get(field.getName());
        if (fieldObject != null && fieldObject instanceof String && ((String) fieldObject).startsWith("@")) {
            // This field is already defined with a variable name
            if (existingVariables.contains((String) fieldObject)) {
                comboBox.setSelectedItem((String) fieldObject);
            }
        }
        fieldToVariableComboBox.put(field, comboBox);
        panel.add(comboBox);
    }

    protected void addValueComponent(Field field, HashMap<String, Object> fieldNameToDefinition, JPanel panel) {
        JComponent component = UiComponentGenerator.getInstance().getCreationComponent(field.getType());
        Object definition = fieldNameToDefinition.get(field.getName());
        if (component != null && definition != null) {
            // This field already has been defined with a value
            // Earlier we had to replace primitive fields with their wrapper object, take that into account here
            if (definition.getClass().equals(field.getType())
                    || (definition.getClass().equals(Double.class) && field.getType().equals(double.class))
                    || (definition.getClass().equals(Float.class) && field.getType().equals(float.class))
                    || (definition.getClass().equals(Integer.class) && field.getType().equals(int.class))
                    || (definition.getClass().equals(Long.class) && field.getType().equals(long.class))) {
                UiComponentGenerator.getInstance().setComponentValue(definition, component);
            }
        }
        fieldToValueComponent.put(field, component);
        if (component == null) {
            // There is no component that can be used to define a value for this field
            // The field can only be set to a variable name
            // Show a message for now, may remove this later...
            component = new JLabel("No component");
        }
        maxComponentWidth = Math.max(maxComponentWidth, component.getPreferredSize().width);
        panel.add(component);
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        fieldNameToDefinition = eventSpec.getFieldDefinitions();
        if (fieldNameToDefinition == null) {
            fieldNameToDefinition = new HashMap<String, Object>();
        }
        paramsPanel = new javax.swing.JPanel();
        BoxLayout paramsLayout = new BoxLayout(paramsPanel, BoxLayout.Y_AXIS);
        paramsPanel.setLayout(paramsLayout);
        addParamComponents(eventSpec, fieldNameToDefinition);
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
        for (Field field : eventSpec.getRequiredFields()) {
            Object definition = null;
            if (eventType == ReflectedEventD.EventType.INPUT) {
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
            } else if (eventType == ReflectedEventD.EventType.OUTPUT) {
                JComboBox variableComboBox = fieldToVariableComboBox.get(field);
                if (!variableComboBox.getSelectedItem().toString().equalsIgnoreCase(ReflectedEventSpecification.NONE)) {
                    // User selected a variable other than NONE to define the field
                    definition = variableComboBox.getSelectedItem().toString();
                } else {
                    // User selected NONE for variable, now see if they created a value definition
                    JComponent valueComponent = fieldToValueComponent.get(field);
                    if (valueComponent != null) {
                        // Store the value from the component
                        definition = UiComponentGenerator.getInstance().getComponentValue(valueComponent, field);
                    }
                }
            }
            fieldNameToObject.put(field.getName(), definition);
        }
        return fieldNameToObject;
    }

    public static void main(String argv[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {

                ReflectedEventD dialog = new ReflectedEventD(new ReflectedEventSpecification("events.output.uiDirectives.DisplayMessage"), null, true);
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
