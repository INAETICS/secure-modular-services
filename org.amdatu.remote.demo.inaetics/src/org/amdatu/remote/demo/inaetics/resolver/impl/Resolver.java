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
    
    public Resolver() {
        
    }
    
    public void setup(String storagedir, String curveparamsFileLocation) throws Exception {
        logger.info("Resolver is setting up the key scheme.");
        KeyPolicyAttributeBasedEncryption kpabe = new KeyPolicyAttributeBasedEncryption();
        String pubfile = storagedir + "publickey";
        String mskfile = storagedir + "mastersecretkey";
        String[] attrs_univ = {"application1", "module1", "solution1"};
        kpabe.setup(pubfile, mskfile, attrs_univ, curveparamsFileLocation);
        
        String prvfile = storagedir + "policy";
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
        gpswabePolicy sub1_policy = new gpswabePolicy("solution1", 1, null);
        // "application1 or module1" (1 out of 2)
        gpswabePolicy sub2_policy = new gpswabePolicy(null, 1, null);
        gpswabePolicy[] sub2_children = new gpswabePolicy[] {new gpswabePolicy("application1", 1, null), new gpswabePolicy("module1", 1, null)};
        sub2_policy.setChildren(sub2_children);
        
        // assemble policy tree into the root
        gpswabePolicy policy = new gpswabePolicy(null, 2, null);
        gpswabePolicy[] policy_children = new gpswabePolicy[] {sub1_policy, sub2_policy};
        policy.setChildren(policy_children);
        // display generated policy
        policy.print();
        kpabe.keygen(pubfile, mskfile, prvfile, policy);
        logger.info("Resolver has finished setting up the scheme.");
        logger.info(String.format("Context saved in %s", storagedir));
    }
    
    public void startSolution() {
        
    }
    
}
