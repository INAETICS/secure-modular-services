package org.amdatu.remote.discovery;

import java.util.Base64;

import org.apache.felix.dm.DependencyActivatorBase;
import org.osgi.framework.BundleContext;

import nl.sudohenk.kpabe.KeyPolicyAttributeBasedEncryption;

public abstract class AbstractAttributeBasedEncryptionActivator extends DependencyActivatorBase {
    
    private volatile String pub_file;
    private volatile String prv_file;
    private volatile String[] attrs;
    private volatile KeyPolicyAttributeBasedEncryption kpabe;
    
    public void initEncryption(BundleContext context) throws Exception {
        kpabe = new KeyPolicyAttributeBasedEncryption();
        String storagedir = System.getProperty("user.dir") + "\\resources\\tmp\\";
        pub_file = storagedir + "publickey";
        prv_file = storagedir + "policy";
        attrs = new String[]{"application1", "module1", "solution1"};
    }
    /**
     * Encryption method for ABE.
     * @param plaintext String to encrypt.
     * @return ciphertext in Base64 form.
     * @throws Exception Encryption error (missing keys, malformed attributes, etc.)
     */
    public String encrypt(String plaintext) throws Exception {
        return new String(Base64.getEncoder().encodeToString(kpabe.enc(pub_file, plaintext.getBytes(), attrs)));
    }
    /**
     * Decryption method for ABE.
     * @param ciphertext ciphertext in Base64.
     * @return plaintext.
     * @throws Exception Encryption error (missing keys, malformed attributes, etc.)
     */
    public String decrypt(String ciphertext) throws Exception {
        return new String(kpabe.dec(pub_file, prv_file, Base64.getDecoder().decode(ciphertext)));
    }
}
