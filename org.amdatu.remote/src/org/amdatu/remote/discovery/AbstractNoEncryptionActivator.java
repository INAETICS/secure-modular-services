package org.amdatu.remote.discovery;

import org.apache.felix.dm.DependencyActivatorBase;
import org.osgi.framework.BundleContext;
/**
 * Activator that does not use any form of discovery encryption.
 * @author Sudohenk
 *
 */
public abstract class AbstractNoEncryptionActivator extends DependencyActivatorBase {
    
    public void initEncryption(BundleContext context) throws Exception {
        
    }
    /**
     * Simply returns the input string.
     * @param plaintext
     * @return plaintext
     * @throws Exception
     */
    public String encrypt(String plaintext) throws Exception {
        return plaintext;
    }
    /**
     * Simply returns the input string.
     * @param ciphertext
     * @return ciphertext (which should be plaintext, if encryption() was used).
     * @throws Exception
     */
    public String decrypt(String ciphertext) throws Exception {
        return ciphertext;
    }
}
