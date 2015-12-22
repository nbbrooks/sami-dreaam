package dreaam.developer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import sami.markup.Markup;
import sami.uilanguage.MarkupComponent;
import sami.variable.Variable.ClassQuantity;
import static sami.variable.Variable.ClassQuantity.ARRAY_LIST;
import static sami.variable.Variable.ClassQuantity.SINGLE;

/**
 * This creates a panel containing all the components necessary to define a
 * single or arbitrary number of instances of a particular class.
 *
 * @author nbb
 */
public class ComplexMarkupCreationComponentP extends JPanel {

    private final static Logger LOGGER = Logger.getLogger(ComplexMarkupCreationComponentP.class.getName());
    private final static int BUTTON_WIDTH = 250;
    private final static int BUTTON_HEIGHT = 50;

    // What type of ComplexMarkupComponentP this is
    //  DIRECT_SINGLE: We are defining a single instance of the class, and each instance is defined using a single MarkupComponent in our library
    //  RECURSIVE_SINGLE: We are defining a single instance of the class, and each instance is defined using multiple MarkupComponents for the class's fields
    //  DIRECT_LIST: We are defining a list of instances of the class, and each instance is defined using a single MarkupComponent in our library
    //  RECURSIVE_LIST: We are defining a list of instances of the class, and each instance is defined using multiple MarkupComponents for the class's fields
    private enum ComponentType {
        DIRECT_SINGLE, DIRECT_LIST, RECURSIVE_SINGLE, RECURSIVE_LIST
    };

    ClassQuantity classQuantity;
    Class valueClass;
    Object value;

    // Button to add component(s) to define another instance of this class
    JButton addAnotherB;

    // ComponentType variables
    ComponentType componentType = null;
    MarkupComponent directComponentSingle;
    ArrayList<DirectListP> directListPanels = new ArrayList<DirectListP>();
    RecursionSingleP recursionComponentSingle;
    ArrayList<RecursionListP> recursionListPanels = new ArrayList<RecursionListP>();

    public ComplexMarkupCreationComponentP(Field field) {
        this(field, null);
    }

    public ComplexMarkupCreationComponentP(Field field, Object value) {
        super(new GridBagLayout());
        this.value = value;
        
        // Determine class and ClassQuantity
        Type type = field.getType();
        Class fieldClass = field.getType();

        if (fieldClass instanceof Class) {
            Class creationClass = (Class) type;
            if (creationClass == ArrayList.class) {

                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    Type keyType = parameterizedType.getActualTypeArguments()[0];

                    if (keyType instanceof Class) {
                        Class argumentClass = (Class) keyType;

                        this.classQuantity = ClassQuantity.ARRAY_LIST;
                        this.valueClass = argumentClass;
                    }
                }
            } else if (creationClass == Hashtable.class) {
                LOGGER.warning("ComplexMarkupCreationComponentP cannot handle Hashtables");
                //@todo add Hashtable support
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    Type keyType = parameterizedType.getActualTypeArguments()[0];
                    Type valueType = parameterizedType.getActualTypeArguments()[1];

                    if (keyType instanceof Class && valueType instanceof Class) {
                        Class keyClass = (Class) keyType;
                        Class valueClass = (Class) valueType;
                        //@todo handle Hashtables
                    }
                }
            } else {
                this.classQuantity = ClassQuantity.SINGLE;
                this.valueClass = fieldClass;
            }
        }

        addComponents();

        // If we have an existing value for this instance, load its value(s) into the component(s)
        if (value != null) {
            setValue(value);
            revalidate();
            repaint();
        }
    }

    public ComplexMarkupCreationComponentP(ClassQuantity classQuantity, Class valueClass) {
        this(classQuantity, valueClass, null);
    }

    public ComplexMarkupCreationComponentP(ClassQuantity classQuantity, Class valueClass, Object value) {
        super(new GridBagLayout());

        this.classQuantity = classQuantity;
        this.valueClass = valueClass;
        this.value = value;

        addComponents();

        // If we have an existing value for this instance, load its value(s) into the component(s)
        if (value != null) {
            setValue(value);
            revalidate();
            repaint();
        }
    }

    protected void addComponents() {
        if (classQuantity == null || valueClass == null) {
            return;
        }

        // Make sure we have an instance of the class
        boolean newInstance = (value == null);
        if (newInstance) {
            try {
                switch (classQuantity) {
                    case SINGLE:
                        value = valueClass.newInstance();
                    case ARRAY_LIST:
                        value = new ArrayList();
                }
            } catch (InstantiationException ex) {
                // Class has no default constructor, usually indicative of it being a primitive
            } catch (IllegalAccessException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                return;
            }
        }

        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;

        // See if we can capture the class in a one component
        // YES - direct component
        //      Do we want a single definition or a list of definitions?
        //      SINGLE: 
        //      LIST: 
        //  NO - recursed components
        //      Do we want a single definition or a list of definitions?
        //      SINGLE:
        //      LIST: 
        directComponentSingle = getDirectValueComponent(valueClass);
        if (directComponentSingle != null && directComponentSingle.getComponent() != null) {
            // Option 1- Single component which supports creation of the root class
            if (classQuantity == ClassQuantity.SINGLE) {
                componentType = ComponentType.DIRECT_SINGLE;
                add(directComponentSingle.getComponent(), constraints);
                constraints.gridy = constraints.gridy + 1;
            } else if (classQuantity == ClassQuantity.ARRAY_LIST) {
                componentType = ComponentType.DIRECT_LIST;
                DirectListP listP = createDirectListPanel(directComponentSingle, 0);
                directListPanels.add(listP);

                add(listP, constraints);
                constraints.gridy = constraints.gridy + 1;

                // Add a button for creating additional components
                addAnotherB = new JButton("Add");
                addAnotherB.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        MarkupComponent newComponent = getDirectValueComponent(valueClass);
                        DirectListP newListP = createDirectListPanel(newComponent);
                        directListPanels.add(newListP);

                        remove(addAnotherB);

                        add(newListP, constraints);
                        constraints.gridy = constraints.gridy + 1;

                        add(addAnotherB, constraints);

                        // If we have added this component to something, make it redraw so bounds are recalculated
                        Window window = SwingUtilities.windowForComponent(addAnotherB);
                        if (window != null) {
                            window.revalidate();
                            window.repaint();
                        }
                    }
                });
                addAnotherB.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
                add(addAnotherB, constraints);
                // Don't increment the constraints here or we won't be able to insert the next markup component before the "Add Another" button
            }
        } else {
            // Try option 2- Multiple components which support creation of all the fields and sub-fields (if necessary) within the recursion limit
            recursionComponentSingle = getRecursiveValueComponent(valueClass);

            if (recursionComponentSingle != null) {
                // Option 1- Single component which supports creation of the root class
                if (classQuantity == ClassQuantity.SINGLE) {
                    componentType = ComponentType.RECURSIVE_SINGLE;
                    add(recursionComponentSingle, constraints);
                    constraints.gridy = constraints.gridy + 1;
                } else if (classQuantity == ClassQuantity.ARRAY_LIST) {
                    componentType = ComponentType.RECURSIVE_LIST;
                    RecursionListP listP = createRecursionListPanel(recursionComponentSingle, 0);

                    recursionListPanels.add(listP);

                    add(listP, constraints);
                    constraints.gridy = constraints.gridy + 1;

                    // Add a button for creating additional components
                    addAnotherB = new JButton("Add");
                    addAnotherB.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {

                            RecursionSingleP newRecursiveP = getRecursiveValueComponent(valueClass);
                            RecursionListP newListP = createRecursionListPanel(newRecursiveP);

                            recursionListPanels.add(newListP);

                            remove(addAnotherB);

                            add(newRecursiveP, constraints);
                            constraints.gridy = constraints.gridy + 1;

                            add(addAnotherB, constraints);

                            // If we have added this component to something, make it redraw so bounds are recalculated
                            Window window = SwingUtilities.windowForComponent(addAnotherB);
                            if (window != null) {
                                window.revalidate();
                                window.repaint();
                            }
                        }
                    });
                    addAnotherB.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
                    add(addAnotherB, constraints);
                    // Don't increment the constraints here or we won't be able to insert the next markup component before the "Add Another" button
                }
            } else {
                JLabel failureL = new JLabel("Failed to create components recursively");
                add(failureL, constraints);
                constraints.gridy = constraints.gridy + 1;
            }
        }
    }

    private DirectListP createDirectListPanel(MarkupComponent singleComponent) {
        return createDirectListPanel(singleComponent, getComponentCount());
    }

    private DirectListP createDirectListPanel(MarkupComponent singleComponent, final int index) {
        final DirectListP listP = new DirectListP();
        listP.setLayout(new GridBagLayout());
        GridBagConstraints listConstraints = new GridBagConstraints();
        listConstraints.gridx = 0;
        listConstraints.gridy = 0;
        listConstraints.fill = GridBagConstraints.BOTH;
        listConstraints.weightx = 1.0;
        listConstraints.weighty = 1.0;

        listP.setMarkupComponent(singleComponent);
        listP.add(singleComponent.getComponent(), listConstraints);
        listConstraints.gridy = listConstraints.gridy + 1;

        // Add button for deleting this item in the list
        JButton deleteB = new JButton("Delete");
        deleteB.addActionListener(new ActionListener() {
            final int panelComponentIndex = index;

            @Override
            public void actionPerformed(ActionEvent ae) {
                listP.getComponent(panelComponentIndex).setVisible(false);
            }
        });
        deleteB.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        listP.add(deleteB, listConstraints);
        listConstraints.gridy = listConstraints.gridy + 1;

        return listP;
    }

    protected MarkupComponent getDirectValueComponent(Class valueClass) {
        MarkupComponent markupComponent = UiComponentGenerator.getInstance().getCreationComponent(valueClass, null, new ArrayList<Markup>(), null, null);
        return markupComponent;
    }

    private RecursionSingleP getRecursiveValueComponent(Class valueClass) {
        // Create JPanel to house sub-field's components
        RecursionSingleP recursionP = new RecursionSingleP();
        recursionP.setLayout(new GridBagLayout());
        recursionP.setBorder(BorderFactory.createLineBorder(Color.black));

        GridBagConstraints recursionConstraints = new GridBagConstraints();
        recursionConstraints.gridx = 0;
        recursionConstraints.gridy = 0;
        recursionConstraints.fill = GridBagConstraints.BOTH;
        recursionConstraints.weightx = 1.0;
        recursionConstraints.weighty = 1.0;

        JLabel recursionL = new JLabel(valueClass.getSimpleName());
        recursionP.add(recursionL, recursionConstraints);
        recursionConstraints.gridy = recursionConstraints.gridy + 1;

        Field[] classFields = valueClass.getDeclaredFields();
        HashMap<Field, MarkupComponent> fieldToComponent = new HashMap<Field, MarkupComponent>();
        recursionP.setFieldToComponent(fieldToComponent);
        boolean foundComponents = true;
        for (Field classField : classFields) {
            try {
                boolean success = addValueComponent(classField, fieldToComponent, recursionP, recursionConstraints, 0);
                if (!success) {
                    foundComponents = false;
                }
            } catch (IllegalArgumentException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                foundComponents = false;
            }
        }
        if (foundComponents) {
            return recursionP;
        }
        return null;
    }

    private RecursionListP createRecursionListPanel(RecursionSingleP recursionSingleP) {
        return createRecursionListPanel(recursionSingleP, getComponentCount());
    }

    private RecursionListP createRecursionListPanel(RecursionSingleP recursionSingleP, final int index) {
        final RecursionListP listP = new RecursionListP();
        listP.setLayout(new GridBagLayout());
        GridBagConstraints listConstraints = new GridBagConstraints();
        listConstraints.gridx = 0;
        listConstraints.gridy = 0;
        listConstraints.fill = GridBagConstraints.BOTH;
        listConstraints.weightx = 1.0;
        listConstraints.weighty = 1.0;

        listP.setRecursionSingleP(recursionSingleP);
        listP.add(recursionSingleP, listConstraints);
        listConstraints.gridy = listConstraints.gridy + 1;

        // Add button for deleting this item in the list
        JButton deleteB = new JButton("Delete");
        deleteB.addActionListener(new ActionListener() {
            final int panelComponentIndex = index;

            @Override
            public void actionPerformed(ActionEvent ae) {
                listP.getComponent(panelComponentIndex).setVisible(false);
            }
        });
        deleteB.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        listP.add(deleteB, listConstraints);
        listConstraints.gridy = listConstraints.gridy + 1;

        return listP;
    }

    protected boolean addValueComponent(Field field, HashMap<Field, MarkupComponent> fieldToComponent, JPanel panel, GridBagConstraints constraints, int recursionDepth) {
        JComponent visualization = null;
        boolean foundComponents = true;
        if (recursionDepth > 3) {
            visualization = new JLabel(field.getName() + " (" + field.getType().getSimpleName() + "): No component - max recursion", SwingConstants.LEFT);
            foundComponents = false;
        } else {
            MarkupComponent markupComponent = getValueComponent(field);

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
                            boolean success = addValueComponent(classField, fieldToComponent, recursionPanel, recursionConstraints, recursionDepth + 1);
                            if (!success) {
                                foundComponents = false;
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
        return foundComponents;
    }

    protected MarkupComponent getValueComponent(Field field) {
        MarkupComponent markupComponent = UiComponentGenerator.getInstance().getCreationComponent((java.lang.reflect.Type) field.getType(), field, new ArrayList<Markup>(), null, null);
        return markupComponent;
    }

    public Object getValue() {
        ArrayList<Object> values;
        Object setValue = null;
        HashMap<Field, MarkupComponent> fieldToComponent;
        if(componentType == null) {
            return null;
        }
        switch (componentType) {
            case DIRECT_SINGLE:
                // We could directly create a single component for the selected class and defined a single value
                return UiComponentGenerator.getInstance().getComponentValue(directComponentSingle, valueClass);
            case DIRECT_LIST:
                // We could directly create a single component for the selected class and defined a list of values
                values = new ArrayList<Object>();
                for (DirectListP directListP : directListPanels) {
                    MarkupComponent component = directListP.getMarkupComponent();
                    Object value = UiComponentGenerator.getInstance().getComponentValue(component, valueClass);
                    if (value != null) {
                        values.add(value);
                    }
                }
                return values;
            case RECURSIVE_SINGLE:
                fieldToComponent = recursionComponentSingle.getFieldToComponent();
                // We used recursion to create a set of components for the selected class and defined a single value
                try {
                    // Create an instance of the class
                    setValue = valueClass.newInstance();
                    // Retrieve value for each field of the class
                    for (Field field : valueClass.getDeclaredFields()) {
                        if (fieldToComponent.containsKey(field)) {
                            MarkupComponent markupComponent = fieldToComponent.get(field);
                            if (markupComponent != null) {
                                // We have a component for this specific field
                                // Retrieve and store the value from the component
                                Object subValue = UiComponentGenerator.getInstance().getComponentValue(markupComponent, field.getType());
                                if (subValue == null) {
                                    setValue = null;
                                    break;
                                } else {
                                    field.set(setValue, subValue);
                                }
                            } else {
                                setValue = null;
                                break;
                            }
                        } else {
                            // We had to recurse into this field to get components - so now we recurse to get the values
                            Object subObject = field.getType().newInstance();
                            field.set(setValue, subObject);
                            getSubFieldValues(field, subObject, fieldToComponent);
                        }
                    }
                } catch (InstantiationException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    setValue = null;
                } catch (IllegalAccessException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    setValue = null;
                }
                return setValue;
            case RECURSIVE_LIST:
                // We used recursion to create a set of components for the selected class and defined a list of values
                values = new ArrayList<Object>();
                for (RecursionListP recursionListP : recursionListPanels) {
                    fieldToComponent = recursionListP.getRecursionSingleP().getFieldToComponent();
                    setValue = null;
                    try {
                        // Create an instance of the class
                        setValue = valueClass.newInstance();
                        // Retrieve value for each field of the class
                        for (Field field : valueClass.getDeclaredFields()) {
                            if (fieldToComponent.containsKey(field)) {
                                MarkupComponent markupComponent = fieldToComponent.get(field);
                                if (markupComponent != null) {
                                    // We have a component for this specific field
                                    // Retrieve and store the value from the component
                                    Object subValue = UiComponentGenerator.getInstance().getComponentValue(markupComponent, field.getType());
                                    if (subValue == null) {
                                        setValue = null;
                                        break;
                                    } else {
                                        field.set(setValue, subValue);
                                    }
                                } else {
                                    setValue = null;
                                    break;
                                }
                            } else {
                                // We had to recurse into this field to get components - so now we recurse to get the values
                                Object subObject = field.getType().newInstance();
                                field.set(setValue, subObject);
                                getSubFieldValues(field, subObject, fieldToComponent);
                            }
                        }
                    } catch (InstantiationException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        setValue = null;
                    } catch (IllegalAccessException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        setValue = null;
                    }
                    if (setValue != null) {
                        values.add(setValue);
                    }
                }
                return values;
            default:
                return null;
        }
    }

    public boolean getSubFieldValues(Field field, Object value, HashMap<Field, MarkupComponent> fieldToComponent) {
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
                        getSubFieldValues(subField, subObject, fieldToComponent);
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

    public boolean setValue(Object value) {
        if (value == null) {
            LOGGER.warning("Set value is NULL");
            return false;
        }

        ArrayList list;
        HashMap<Field, MarkupComponent> fieldToComponent;
        boolean overallSuccess;
        switch (componentType) {
            case DIRECT_SINGLE:
                // We could directly create a single component for the selected class and defined a single value
                // Set single component's value
                return directComponentSingle.setComponentValue(value);
            case DIRECT_LIST:
                overallSuccess = true;
                // We could directly create a single component for the selected class and defined a list of values
                if (!(value instanceof ArrayList)) {
                    return false;
                }
                list = (ArrayList) value;
                if (list.isEmpty()) {
                    // List was empty and we already have 1 empty component for creating values
                    return true;
                }
                for (Object listValue : list) {
                    // Set the last component's value to this, then add another component
                    int numComponents = getComponentCount();
                    // Should have at least 1 component - the "Add Another" button
                    if (!directListPanels.isEmpty()) {
                        boolean success = directListPanels.get(directListPanels.size() - 1).getMarkupComponent().setComponentValue(listValue);
                        if (!success) {
                            overallSuccess = false;
                        } else {
                            // Add another MarkupComponent
                            addAnotherB.doClick();
                        }
                    }
                }
                return overallSuccess;
            case RECURSIVE_SINGLE:
                // We used recursion to create a set of components for the selected class and defined a single value
                // Set value on single recursive JPanel
                return setRecursiveComponentValue(valueClass, value, recursionComponentSingle.getFieldToComponent());
            case RECURSIVE_LIST:
                overallSuccess = true;
                // We used recursion to create a set of components for the selected class and defined a list of values
                if (!(value instanceof ArrayList)) {
                    return false;
                }
                list = (ArrayList) value;
                if (list.isEmpty()) {
                    // List was empty and we already have 1 empty recusion panel for creating values
                    return true;
                }
                for (int i = 0; i < list.size(); i++) {
                    Object listValue = list.get(i);

                    // Set the last component's value to this, then add another component
                    // Should have at least 1 component - the "Add Another" button
                    if (!recursionListPanels.isEmpty()) {
                        boolean success = setRecursiveComponentValue(valueClass, listValue, recursionListPanels.get(recursionListPanels.size() - 1).getRecursionSingleP().getFieldToComponent());
                        if (!success) {
                            overallSuccess = false;
                        } else {
                            // Add another MarkupComponent
                            addAnotherB.doClick();
                        }
                    }
                }
                return overallSuccess;
            default:
                LOGGER.warning("Could not set value on NULL type ComplexMarkupComponentP");
                throw new AssertionError(componentType.name());
        }
    }

    public boolean setRecursiveComponentValue(Class valueClass, Object value, HashMap<Field, MarkupComponent> fieldToComponent) {
        boolean overallSuccess = true;
        // Set value for each field of the class
        for (Field field : valueClass.getDeclaredFields()) {
            if (fieldToComponent.containsKey(field)) {
                MarkupComponent markupComponent = fieldToComponent.get(field);
                if (markupComponent != null) {
                    try {
                        // We have a component for this specific field
                        // Retrieve and store the value from the component
                        boolean success = markupComponent.setComponentValue(field.get(value));
                        if (!success) {
                            overallSuccess = false;
                        }
                    } catch (IllegalArgumentException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        overallSuccess = false;
                    } catch (IllegalAccessException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        overallSuccess = false;
                    }
                } else {
                    overallSuccess = false;
                }
            } else {
                // We had to recurse into this field to get components - so now we recurse to get the values
                try {
                    Object subObject = field.get(value);
                    boolean success = setSubFieldValues(subObject, fieldToComponent);
                    if (!success) {
                        overallSuccess = false;
                    }
                } catch (IllegalArgumentException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    overallSuccess = false;
                } catch (IllegalAccessException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    overallSuccess = false;
                }
            }
        }
        return overallSuccess;
    }

    public boolean setSubFieldValues(Object value, HashMap<Field, MarkupComponent> fieldToComponent) {
        boolean overallSuccess = true;
        for (Field field : value.getClass().getDeclaredFields()) {
            // We have a component for this specific field
            if (fieldToComponent.containsKey(field)) {
                MarkupComponent markupComponent = fieldToComponent.get(field);
                if (markupComponent != null) {
                    try {
                        boolean success = markupComponent.setComponentValue(field.get(value));
                        if (!success) {
                            overallSuccess = false;
                        }
                    } catch (IllegalArgumentException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        overallSuccess = false;
                    } catch (IllegalAccessException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        overallSuccess = false;
                    }
                } else {
                    return false;
                }
            } else {
                // We had to recurse into this field to get components - so now we recurse to get the values
                try {
                    Object subObject = field.get(value);
                    boolean success = setSubFieldValues(subObject, fieldToComponent);
                    if (!success) {
                        overallSuccess = false;
                    }
                } catch (IllegalArgumentException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    overallSuccess = false;
                } catch (IllegalAccessException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    overallSuccess = false;
                }
            }
        }
        return overallSuccess;
    }

    // A directly created markup component and a delete button
    class DirectListP extends JPanel {

        MarkupComponent markupComponent;

        public MarkupComponent getMarkupComponent() {
            return markupComponent;
        }

        public void setMarkupComponent(MarkupComponent markupComponent) {
            this.markupComponent = markupComponent;
        }
    }

    // A panel containing recursion created markup components
    class RecursionSingleP extends JPanel {

        HashMap<Field, MarkupComponent> fieldToComponent = new HashMap<Field, MarkupComponent>();

        public HashMap<Field, MarkupComponent> getFieldToComponent() {
            return fieldToComponent;
        }

        public void setFieldToComponent(HashMap<Field, MarkupComponent> fieldToComponent) {
            this.fieldToComponent = fieldToComponent;
        }
    }

    // A panel containing recursion created markup components and a delete button
    class RecursionListP extends JPanel {

        RecursionSingleP recursionSingleP;

        public RecursionSingleP getRecursionSingleP() {
            return recursionSingleP;
        }

        public void setRecursionSingleP(RecursionSingleP recursionSingleP) {
            this.recursionSingleP = recursionSingleP;
        }
    }
}
