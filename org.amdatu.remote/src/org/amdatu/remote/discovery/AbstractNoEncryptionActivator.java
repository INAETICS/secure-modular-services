package org.amdatu.remote.discovery;

import org.apache.felix.dm.DependencyActivatorBase;
import org.osgi.framework.BundleContext;

public abstract class AbstractNoEncryptionActivator extends DependencyActivatorBase {
    
    public void initEncryption(BundleContext context) throws Exception {
        
    }
    
    public String encrypt(String plaintext) throws Exception {
        return plaintext;
    }

    public String decrypt(String ciphertext) throws Exception {
        return ciphertext;
    }
}
