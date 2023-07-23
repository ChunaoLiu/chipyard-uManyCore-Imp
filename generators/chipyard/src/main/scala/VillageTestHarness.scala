package chipyard

import chisel3._

import scala.collection.mutable.{ArrayBuffer, LinkedHashMap}
import freechips.rocketchip.diplomacy.{LazyModule}
import freechips.rocketchip.config.{Field, Parameters}
import freechips.rocketchip.util.{ResetCatchAndSync}
import freechips.rocketchip.prci.{ClockBundle, ClockBundleParameters, ClockSinkParameters, ClockParameters}

import chipyard.harness.{ApplyHarnessBinders, HarnessBinders}
import chipyard.iobinders.HasIOBinders
import chipyard.clocking.{SimplePllConfiguration, ClockDividerN}
import firrtl.FirrtlProtos.Firrtl.Module.ExternalModule.Parameter
import firrtl.FirrtlProtos.Firrtl.Top

// -------------------------------
// Chipyard Test Harness
// -------------------------------

case object SingleTop extends Field[Parameters => LazyModule]((p: Parameters) => new ChipTop()(p))

class TwoTopWrapper(implicit val p:Parameter) extends Module {

    val io = IO(new Bundle {
        val success1 = Output(Bool())
        val success2 = Output(Bool())
    })
    io.success1 := false.B
    io.success2 := false.B
    
    val systemClock = Wire(Input(Clock()))
    val systemReset = Wire(Input(Bool()))



    // val chip1 = withClockAndReset(systemClock, systemReset) {
    //     val chip1 = LazyModule(p(SingleTop)(p))
    //     p(IOBinders).values.map(fn => fn(systemClock, systemReset.asBool, io.success1, chip1))
    //     chip1
    // }
  
    // val chip2 = withClockAndReset(systemClock, systemReset) {
    //     val chip2 = p(SingleTop)(p) 
    //     p(IOBinders).values.map(fn => fn(systemClock, systemReset.asBool, io.success2, chip2))
    //     chip2
    // }
    
    systemReset := reset.asBool
    systemClock := clock
}

