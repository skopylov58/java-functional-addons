package com.skopylov.ftry;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread local storage for {@link AutoCloseable} resources.
 * 
 * @author skopylov@gmail.com
 *
 */
public class TryAutoCloseable implements AutoCloseable {
    
    private static final ThreadLocal<List<AutoCloseable>> resources = ThreadLocal.withInitial(ArrayList::new);  
    
    protected void addResource(AutoCloseable autoCloseable) {
        resources.get().add(autoCloseable);
    }
    
    protected final void closeResources() {
        List<AutoCloseable> list = resources.get();
        if (list == null || list.isEmpty()) {
            return;
        }
        list.forEach(c -> {
            try {
                c.close();
            } catch (Exception e) {
                //silently
            }
        });
        list.clear();
        resources.remove();
    }

    /**
     * Closes and clear all resources marked by {@link Try#autoClose()}
     */
    @Override
    public void close() throws Exception {
        closeResources();
    }
}
