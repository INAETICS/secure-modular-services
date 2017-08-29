package org.amdatu.remote.demo.inaetics.resolver.impl;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.sudohenk.kpabe.Example;
import nl.sudohenk.kpabe.KeyPolicyAttributeBasedEncryption;
import nl.sudohenk.kpabe.gpswabe.gpswabePolicy;

public class Resolver {
    private Logger logger = LoggerFactory.getLogger(Resolver.class);
    private KeyPolicyAttributeBasedEncryption kpabe;
    private String pubfile;
    private String mskfile;
    private String storagedir;
    
    public Resolver() {
        
    }
    /**
     * Setup the ABE key scheme.
     * @param storagedir
     * @param curveparamsFileLocation
     * @param attrs_univ
     * @throws Exception
     */
    public void setup(String storagedir, String curveparamsFileLocation, String[] attrs_univ) throws Exception {
        kpabe = new KeyPolicyAttributeBasedEncryption();
        this.storagedir = storagedir;
        logger.info("Resolver is setting up the key scheme.");
        // setup public/private key
        pubfile = this.storagedir + "publickey";
        mskfile = this.storagedir + "mastersecretkey";
        kpabe.setup(pubfile, mskfile, attrs_univ, curveparamsFileLocation);
        
        logger.info("Resolver has finished setting up the scheme.");
        // setup policies
        String solution = attrs_univ[0];
        String application = attrs_univ[1];
        for (int i = 2; i < attrs_univ.length; i++) {
            String module = attrs_univ[i];
            this.generatePolicy(solution, application, module);
        }
        
        logger.info(String.format("Context saved in %s", storagedir));
    }
    
    /**
     * Generate the policy for a given policy structure.
     * (solution AND (application OR module))
     * @param storagedir
     * @param solution
     * @param application
     * @param module
     * @throws Exception 
     */
    private void generatePolicy(String solution, String application, String module) throws Exception {
        String prvfile = String.format("%spolicy-%s", this.storagedir, module);
        // Build up an access tree:
        // Example of what we want to achieve:
        //                          2 of 2
        //                         /      \
        //                  solution1    1 of 2
        //                              /       \
        //                      application1   module1
        //
        // This access tree can also be written as:
        //      solution1 1of1 application1 module1 1of2 2of2
        // Which can be simplified to:
        //      (solution1 AND (application1 OR module1))
        
        // "solution1" (leaf)
        gpswabePolicy sub1_policy = new gpswabePolicy(solution, 1, null);
        // "application1 or module1" (1 out of 2)
        gpswabePolicy sub2_policy = new gpswabePolicy(null, 1, null);
        gpswabePolicy[] sub2_children = new gpswabePolicy[] {new gpswabePolicy(application, 1, null), new gpswabePolicy(module, 1, null)};
        sub2_policy.setChildren(sub2_children);
        
        // assemble policy tree into the root
        gpswabePolicy policy = new gpswabePolicy(null, 2, null);
        gpswabePolicy[] policy_children = new gpswabePolicy[] {sub1_policy, sub2_policy};
        policy.setChildren(policy_children);
        // display generated policy
        logger.info(String.format("Policy generated for %s, stored in %s", module, prvfile));
        policy.print();
        kpabe.keygen(pubfile, mskfile, prvfile, policy);
    }
    
    /**
     * Stub to kick off certain modules. This should be the job of the resolver, but in this PoC we do it manually.
     */
    public void startSolution() {
        // TODO
    }
    
}
