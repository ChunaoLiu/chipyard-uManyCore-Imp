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

case class TestModuleParams (
    address: BigInt = 0x1000,
    width: Int = 32,
    useAXI4: Boolean = false,
    useBlackBox: Boolean = false
)

case object TestModuleKey extends Field[Option[TestModuleParams]](None)

class TestModuleIO (val w: Int) extends Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val input_ready = Output(Bool())
    val input_valid = Input(Bool())
    val output_ready = Input(Bool())
    val output_valid = Output(Bool())
    val testModuleInput = Input(UInt(w.W))
    val testModuleOutput = Output(UInt(w.W))
    val busy = Output(Bool())
}

trait TestModuleTopIO extends Bundle {
    val TestModule_busy = Output(Bool())
}

trait HasTestModuleIO extends BaseModule {
    val w: Int
    val io = IO(new TestModuleIO(w))
}

trait TestModule extends HasRegMap{
    val io: TestModuleTopIO

    implicit val p: Parameters
    def params: TestModuleParams
    val clock: Clock
    val reset: Reset
    
    // TODO: remove inputStatus, replace ready signal with testModuleInput
    // val inputStatus = Wire(new DecoupledIO(UInt(params.width.W)))
    val testModuleInput = Wire(new DecoupledIO(UInt(params.width.W)))
    val testModuleOutput = Wire(new DecoupledIO(UInt(params.width.W)))
    val status = Wire(UInt(2.W))

    val impl = Module(new TestModuleImp(params.width))

    impl.io.clock := clock
    impl.io.reset := reset.asBool

    testModuleInput.ready := impl.io.input_ready

    impl.io.input_valid := testModuleInput.valid

    // This line doesn't matter in our sample workload
    impl.io.output_ready := testModuleOutput.ready

    impl.io.testModuleInput := testModuleInput.bits

    testModuleOutput.bits := impl.io.testModuleOutput

    testModuleOutput.valid := impl.io.output_valid

    status := Cat(impl.io.input_ready, impl.io.output_valid)
    io.TestModule_busy := impl.io.busy

    when (testModuleInput.valid) {
        printf("Test Module Input is valid and value is %d\n", testModuleInput.bits)
    }
    when (testModuleOutput.valid) {
        printf("Test Module Output is ready and value is %d\n", testModuleOutput.bits)
    }

    regmap(
        0x00 -> Seq(
            RegField.r(2, status)),
        0x04 -> Seq(
            RegField.w(params.width, testModuleInput)
        ),
        0x08 -> Seq(
            RegField.r(params.width, testModuleOutput)
        )
    )

}

class TestModuleImp(val w: Int) extends Module with HasTestModuleIO {
    val s_idle :: s_run :: s_done :: Nil = Enum(3)

    val ValInit   = Reg(UInt(w.W))
    val state = RegInit(s_idle)
    var tmp = Reg(UInt(w.W))
    val isDone = Reg(UInt(w.W))

    // ======================
    // Basic IO Control Block
    // ======================

    // Module is ready only when its state is idle
    io.input_ready := state === s_idle
    // Module output is ready only when its state is done
    io.output_valid := state === s_done

    io.testModuleOutput := tmp

    when (state === s_idle && io.input_valid) {
        state := s_run
        isDone := 0.U
    } .elsewhen (state === s_run && isDone === 1.U) {
        printf("We switch to done\n")
        state := s_done
    } .elsewhen (state === s_done && io.output_ready) {
        printf("We switch to Idle\n")
        state := s_idle
        isDone := 0.U
    }

    // Actual logic
    when (state === s_idle && io.input_valid) {
        printf("We have received the workload and we can run it!\n");
        ValInit := io.testModuleInput
        isDone := 0.U
    } .elsewhen (state === s_run) {
        printf("\nPre: isDone is %d\n", isDone);
        printf("Running workload... Hello World!\n");
        printf("Number we got is %d\n", ValInit);
        tmp := (ValInit + 1.U)
        isDone := 1.U
        printf("Pro: isDone is %d\n", isDone);
    }

    io.busy := state =/= s_idle
}

class TestModuleTileLink(params: TestModuleParams, beatBytes: Int)(implicit p: Parameters) extends TLRegisterRouter(
    params.address, "testmodule", Seq("ucbbar,testmodule"),
    beatBytes = beatBytes)(
        new TLRegBundle(params, _) with TestModuleTopIO)(
            new TLRegModule(params, _, _) with TestModule)

class TestModuleAXI4(params: TestModuleParams, beatBytes: Int)(implicit p: Parameters)
  extends AXI4RegisterRouter(
    params.address,
    beatBytes=beatBytes)(
      new AXI4RegBundle(params, _) with TestModuleTopIO)(
      new AXI4RegModule(params, _, _) with TestModule)


trait HaveTestModule {
    this: BaseSubsystem =>
        private val portName = "TestModule"

        val TestModule = p(TestModuleKey) match {
            case Some(params) => {
                if (params.useAXI4) {
                    val TestModule = LazyModule(new TestModuleAXI4(params, pbus.beatBytes)(p))
                    pbus.toSlave(Some(portName)) {
                        TestModule.node := AXI4Buffer() := TLToAXI4() := TLFragmenter(pbus.beatBytes, pbus.blockBytes, holdFirstDeny = true)
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
    val TestModule_busy = outer.TestModule match {
        case Some(testmodule) => {
            val busy = IO(Output(Bool()))
            busy := testmodule.module.io.TestModule_busy
            Some(busy)
        }
        case None => None
    }
}

class withTestModule(useAXI4: Boolean = false, useBlackBox: Boolean = false) extends Config((site, here, up) => {
    case TestModuleKey => Some(TestModuleParams(useAXI4 = useAXI4, useBlackBox = useBlackBox))
})