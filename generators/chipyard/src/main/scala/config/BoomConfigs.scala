package chipyard

import freechips.rocketchip.config.{Config}

// ---------------------
// BOOM Configs
// ---------------------

class testConfig extends Config(
  new boom.common.WithNSmallBooms(1) ++   
  new chipyard.example.withTestModule(useAXI4=false, useBlackBox=false) ++
  new chipyard.config.AbstractConfig)

class uManyCoreVillageConfig extends Config(
  // new chipyard.config.WithTraceIO ++
  // 4 Cores with L1 and TLB   
  new boom.common.WithuManyCoreTile() ++
  // Override L2 cache setup from the default config
  new freechips.rocketchip.subsystem.WithInclusiveCache(nWays=16, capacityKB=256, outerLatencyCycles=48) ++
  // TODO: Add NIC Config
  new chipyard.config.AbstractConfig)                            // uManyCore basic tile config

class uManyCoreVillageConfigWithBootRom extends Config(
  // new chipyard.config.WithTraceIO ++
  // 4 Cores with L1 and TLB   
  new boom.common.WithuManyCoreTile() ++
  new boom.common.WithMarshalFedoraBootRom() ++
  // Override L2 cache setup from the default config
  new freechips.rocketchip.subsystem.WithInclusiveCache(nWays=16, capacityKB=256, outerLatencyCycles=48) ++
  // TODO: Add NIC Config
  new chipyard.config.AbstractConfig)                            // uManyCore basic tile config

class SmallBoomConfig extends Config(
  new boom.common.WithNSmallBooms(1) ++                          // small boom config
  new chipyard.config.AbstractConfig)

class MediumBoomConfig extends Config(
  new boom.common.WithNMediumBooms(1) ++                         // medium boom config
  new chipyard.config.AbstractConfig)

class LargeBoomConfig extends Config(
  new boom.common.WithNLargeBooms(1) ++                          // large boom config
  new chipyard.config.WithSystemBusWidth(128) ++
  new chipyard.config.AbstractConfig)

class MegaBoomConfig extends Config(
  new boom.common.WithNMegaBooms(1) ++                           // mega boom config
  new chipyard.config.WithSystemBusWidth(128) ++
  new chipyard.config.AbstractConfig)

class DualSmallBoomConfig extends Config(
  new boom.common.WithNSmallBooms(2) ++                          // 2 boom cores
  new chipyard.config.AbstractConfig)

class Cloned64MegaBoomConfig extends Config(
  new boom.common.WithCloneBoomTiles(63, 0) ++
  new boom.common.WithNMegaBooms(1) ++                           // mega boom config
  new chipyard.config.WithSystemBusWidth(128) ++
  new chipyard.config.AbstractConfig)

class LoopbackNICLargeBoomConfig extends Config(
  new chipyard.harness.WithLoopbackNIC ++                        // drive NIC IOs with loopback
  new icenet.WithIceNIC ++                                       // build a NIC
  new boom.common.WithNLargeBooms(1) ++
  new chipyard.config.WithSystemBusWidth(128) ++
  new chipyard.config.AbstractConfig)

class DromajoBoomConfig extends Config(
  new chipyard.harness.WithSimDromajoBridge ++                   // attach Dromajo
  new chipyard.config.WithTraceIO ++                             // enable the traceio
  new boom.common.WithNSmallBooms(1) ++
  new chipyard.config.WithSystemBusWidth(128) ++
  new chipyard.config.AbstractConfig)

class MediumBoomCosimConfig extends Config(
  new chipyard.harness.WithCospike ++                            // attach spike-cosim
  new chipyard.config.WithTraceIO ++                             // enable the traceio
  new boom.common.WithNMediumBooms(1) ++
  new chipyard.config.AbstractConfig)
