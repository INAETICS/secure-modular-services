# securing modular services with KP-ABE

**Abstract**:

Research project involving the integration of [Key Policy Attribute Based Encryption](https://gnunet.org/sites/default/files/CCS'06%20-%20Attributed-based%20encryption%20for%20fine-grained%20access%20control%20of%20encrypted%20data.pdf) into the existing INAETICS modular service architecture. This Proof of Concept uses the [Amdatu Remote services](https://amdatu.org/application/remote/) project to simulate the core principles of INAETICS.


## Project setup

* `cnf` - BndTools repository
* `kpabe` - Modified version of [kpabe](https://github.com/LiangZhang716/kpabe). Fixed various bugs.
* `org.amdatu.remote` - Root project for Amdatu remoteservices
* `org.amdatu.remote.demo` - Demo project for Amdatu remoteservices
* `org.amdatu.remote.itest` - Tests

## How to setup

1. Check in workspace as BNDTools project in Eclipse.
2. Run the chatdemo client1, and client2.