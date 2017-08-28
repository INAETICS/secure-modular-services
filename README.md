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

1. Check in this workspace (import as Bnd-tools projects, use the provided [Eclipse IDE](https://amdatu.org/generaltop/gettingstarted/) from Amdatu).
2. Setup [ETCD](https://github.com/coreos/etcd/releases/). This is the discovery mechanism we use for this demo.
3. Start up one ETCD cluster on localhost with the default settings.
4. Run the `inaeticsdemo.resolver.bndrun` with the `Bnd OSGi Run Launcher`.
5. Run the `inaeticsdemo.module1.bndrun` with the `Bnd OSGi Run Launcher`.
6. Run the `inaeticsdemo.module2.bndrun` with the `Bnd OSGi Run Launcher`.
7. Inspect the keys in ETCD, you will only see ciphertext and no plaintext endpoints.