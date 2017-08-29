package org.amdatu.remote.discovery;

import static org.amdatu.remote.ServiceUtil.getConfigStringValue;

import java.util.Base64;

import org.apache.felix.dm.DependencyActivatorBase;
import org.osgi.framework.BundleContext;

import nl.sudohenk.kpabe.KeyPolicyAttributeBasedEncryption;
/**
 * Activator that uses Key-Policy Attribute Based Encryption to make the discovery information confidential.
 * @author Sudohenk
 *
 */
public abstract class AbstractAttributeBasedEncryptionActivator extends DependencyActivatorBase {
    public static final String INAETICS_SCOPE_SOLUTION = "inaetics.solution.name";
    public static final String INAETICS_SCOPE_APPLICATION = "inaetics.application.name";
    public static final String INAETICS_SCOPE_MODULE = "inaetics.module.name";
                    
    private volatile String pub_file;
    private volatile String prv_file;
    private volatile String[] attrs;
    private volatile KeyPolicyAttributeBasedEncryption kpabe;
    
    public void initEncryption(BundleContext context) throws Exception {
        kpabe = new KeyPolicyAttributeBasedEncryption();
        String storagedir = System.getProperty("user.dir") + "\\resources\\tmp\\";
        pub_file = storagedir + "publickey";
        prv_file = storagedir + "policy";
        String scope_solution = getConfigStringValue(context, INAETICS_SCOPE_SOLUTION, null, null);
        String scope_application = getConfigStringValue(context, INAETICS_SCOPE_APPLICATION, null, null);
        String scope_module = getConfigStringValue(context, INAETICS_SCOPE_MODULE, null, null);
        
        attrs = new String[]{scope_solution, scope_application, scope_module};
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
