package chipyard.example

import chisel3._
import chisel3.util._
import chisel3.experimental.{IntParam, BaseModule}
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper.{HasRegMap, RegField}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.UIntIsOneOf

class TestModule extends HasRegMap{
    val io: testModuleTopIO

    implicit val p: Parameters
    def params: testModuleParams
    val clock: Clock
    val reset: Reset

    val inputStatus = Wire(new DecoupledIO(params.width.W))
    val testModuleInput = Wire(new DecoupledIO(UInt(params.width.W)))
    val status = Wire(UInt(2.W))

    val impl = Module(new testModuleInput(params.width))

    impl.io.clock := clock
    impl.io.reset := reset.asBool

    impl.io.input_valid := inputStatus.valid
    inputStatus.ready := impl.io.input_ready

    status.bits := impl.io.testVal
    status.valid := impl.io.output_valid

    impl.io.output_ready := testModuleInput.ready

    status := Cat(impl.io.input_ready, impl.io.output_valid)
    io.busy := impl.io.busy

    regmap(
        0x00 -> Seq(
            RegField.r(2, status)),
        0x04 -> Seq(
            RegField.w(params.width, inputStatus)
        ),
        0x08 -> Seq(
            RegField.r(params.width, testModuleInput)
        )
    )

}

class testModuleImp(val w: Int) extends Module with HasTestIO {
    val s_idle :: s_run :: s_done :: Nil = Enum(3)

    val state = RegInit(s_idle)
    val tmp = Reg(UInt(w.W))
    val testInput = Reg(UInt(w.W))

    // ======================
    // Basic IO Control Block
    // ======================

    // Module is ready only when its state is idle
    io.input_ready := state === s_idle
    // Module output is ready only when its state is done
    io.output_valid := state === s_done
    // Use the input signal as output
    io.testVal := testVal

    when (state === s_idle && io.input_valid) {
        state := s_run
    } .elsewhen (state === s_run && tmp === 0.U) {
        state := s_done
    } .elsewhen (state === s_done && io.output_ready) {
        state := s_idle
    }

    // Actual logic
    when (state === s_idle && io.input_ready) {
        printf("We have received the workload and we can run it!\n");
        testInput := testInput + 1.U
    } .elsewhen (state === s_run) {
        printf("Running workload... Hello World!\n");
    }

    io.busy := state =/= s_idle
}

class TestModuleTileLink(params: TestModuleParam, beatBytes: Int)(implicit p: Parameters) extends TLRegisterRouter(
    params.address, "testmodule", Seq("ucbbar,testmodule"),
    beatBytes = beatBytes)(
        new TLRegBundle(params, _) with TestModuleTopIO)(
            new TLRegModule(params, _, _) with TestModule)

class TestModuleAXI4(params: TestModuleParam, beatBytes: Int)(implicit p: Parameters)
  extends AXI4RegisterRouter(
    params.address,
    beatBytes=beatBytes)(
      new AXI4RegBundle(params, _) with TestModuleTopIO)(
      new AXI4RegModule(params, _, _) with TestModule)


trait HaveTestModule {
    this: BaseSubsystem =>
        private val portName = "TestModule"

        val TestModule = p(ModuleKey) match {
            case Some(params) => {
                if (params.useAXI4) {
                    val TestModule = LazyModule(new TestModuleAXI4(params, pbus.beatBytes)(p))
                    pbus.toSlave(Some(portName)) {
                        gcd.node := AXI4Buffer() := TLToAXI4() := TLFragmenter(pbus.beatBytes, pbus.blockBytes, holdFirstDeny = true)
                    }
                    Some(TestModule)
                } else {
                    val TestModule = LazyModule(new TestModuleTileLink(params, pbus.beatBytes)(p))
                    pbus.toVariableWidthSlave(Some(portName)) { TestModule.node }
                    Some(TestModule)
                }
            }
            case None => None
        }
}

trait HaveTestModuleImp extends LazyModuleImp {
    val outer : HaveTestModule
    val TestModule_busy = outer.TestModulee match {
        case Some(TestModule) => {
            val busy = IO(Output(Bool()))
            busy := TestModule.module.io.busy
            Some(busy)
        }
        case None => None
    }
}