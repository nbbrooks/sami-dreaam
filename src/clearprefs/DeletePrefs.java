/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package clearprefs;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 *
 * @author NicolÃ² Marchi <marchi.nicolo@gmail.com>
 */
public class DeletePrefs {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws BackingStoreException {
        // TODO code application logic here
        
        Preferences pref = Preferences.userRoot();
        pref.clear();
        
        System.out.println("CLEARED");
    }
    
}
