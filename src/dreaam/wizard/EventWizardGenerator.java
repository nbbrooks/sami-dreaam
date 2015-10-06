package dreaam.wizard;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 *
 * @author nbb
 */
public class EventWizardGenerator {

    private final static Logger LOGGER = Logger.getLogger(EventWizardGenerator.class.getName());
    private static EventWizardInt instance = null;

    public static synchronized EventWizardInt getInstance() {
        if (instance == null) {
            instance = createInstance();
        }
        return instance;
    }

    private static EventWizardInt createInstance() {
        try {
//            ArrayList<String> list = (ArrayList<String>) DomainConfigManager.getInstance().getDomainConfiguration().fromUiMessageGeneratorList.clone();
//            for (String className : list) {
//                Class uiClass = Class.forName(className);
            Class uiClass = Class.forName("crw.wizard.CrwEventWizard");
            Method factoryMethod = uiClass.getDeclaredMethod("getInstance");
            Object singleton = factoryMethod.invoke(null, null);
            if (singleton instanceof EventWizardInt) {
                return (EventWizardInt) singleton;
            }
//            }
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        } catch (NoSuchMethodException nsme) {
            nsme.printStackTrace();
        } catch (InvocationTargetException ite) {
            ite.printStackTrace();
            System.out.println(ite.getCause());
        }
        LOGGER.severe("Failed to create instance of FromUiMessageGeneratorInterface");
        return null;
    }

//        for (String className : DomainConfigManager.getInstance().getDomainConfiguration().serverList) {
//            try {
//                Class serverClass = Class.forName(className);
//                Object serverElement = serverClass.getConstructor(new Class[]{}).newInstance();
//                if (serverElement instanceof ProxyServerInt) {
//                    proxyServer = (ProxyServerInt) serverElement;
//                    proxyServer.addListener(this);
//                }
//                if (serverElement instanceof ObserverServerInt) {
//                    observerServer = (ObserverServerInt) serverElement;
//                    observerServer.addListener(this);
//                }
//            } catch (ClassNotFoundException cnfe) {
//                cnfe.printStackTrace();
//            } catch (InstantiationException ie) {
//                ie.printStackTrace();
//            } catch (IllegalAccessException iae) {
//                iae.printStackTrace();
//            } catch (NoSuchMethodException nsme) {
//                nsme.printStackTrace();
//            } catch (InvocationTargetException ite) {
//                ite.printStackTrace();
//            }
//        }
//        if (proxyServer == null) {
//            LOGGER.log(Level.SEVERE, "Failed to find Proxy Server in domain configuration!");
//        }
//        if (observerServer == null) {
//            LOGGER.log(Level.SEVERE, "Failed to find Observer Server in domain configuration!");
//        }
//
//        Hashtable<String, String> handlerMapping = DomainConfigManager.getInstance().getDomainConfiguration().eventHandlerMapping;
//        Class eventClass, handlerClass;
//        EventHandlerInt handlerObject;
//        HashMap<String, EventHandlerInt> handlerObjects = new HashMap<String, EventHandlerInt>();
//        String handlerClassName;
//        for (String ieClassName : handlerMapping.keySet()) {
//            handlerClassName = handlerMapping.get(ieClassName);
//            try {
//                eventClass = Class.forName(ieClassName);
//                handlerClass = Class.forName(handlerClassName);
//                if (!handlerObjects.containsKey(handlerClassName)) {
//                    // First use of this handler class, create an instance and add it to our hashmap
//                    EventHandlerInt newHandlerObject = (EventHandlerInt) handlerClass.newInstance();
//                    if (ProxyServerListenerInt.class.isInstance(newHandlerObject)) {
//                        proxyServer.addListener((ProxyServerListenerInt) newHandlerObject);
//                    }
//                    handlerObjects.put(handlerClassName, newHandlerObject);
//                }
//                handlerObject = handlerObjects.get(handlerClassName);
//                handlers.put(eventClass, handlerObject);
//            } catch (InstantiationException ex) {
//                ex.printStackTrace();
//            } catch (IllegalAccessException ex) {
//                ex.printStackTrace();
//            } catch (ClassNotFoundException ex) {
//                ex.printStackTrace();
//            }
//        }
}
