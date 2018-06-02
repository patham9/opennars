package nars.plugin;

import java.io.Serializable;
import nars.main.NAR;

/**
 * NAR plugin interface
 */
public interface Plugin extends Serializable {

    /** called when plugin is activated (enabled = true) / deactivated (enabled=false) */
    default boolean setEnabled(NAR n, boolean enabled) { return true; }
    
    default public CharSequence name() {
        return this.getClass().getSimpleName();
    }
}
