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

case class SchedulerParams (
    address: BigInt = 0x1000,
    width: Int = 32,
    useAXI4: Boolean = false,
    useBlackBox: Boolean = false
)

case object SchedulerKey extends Field[Option[SchedulerParams]](None)

class SchedulerIO (val w: Int) extends Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val input_ready = Output(Bool())
    val input_valid = Input(Bool())
    val output_ready = Input(Bool())
    val output_valid = Output(Bool())
    val functionIndexOutput = Output(UInt(w.W))
    val functionParamOutput = Output(UInt(w.W))
    val headOfQueue = Output(UInt(w.W))
    val busy = Output(Bool())
}

trait SchedulerTopIO extends Bundle {
    val Scheduler_busy = Output(Bool())
}

class WorkloadIO (val w: Int) extends Bundle {
    val workload_id = Input(Decoupled(UInt(w.W)))
    val function_id = Input(Decoupled(UInt(w.W)))
    // Parameters should be bitstreams. Will think about how to make it bitstream later
    val parameters = VecInit(Seq.fill(10)(0.U(w.W)))
}

trait HasSchedulerIO extends BaseModule {
    val w: Int
    val io = IO(new SchedulerIO(w))
}

trait Scheduler extends HasRegMap{
    val io: SchedulerTopIO

    implicit val p: Parameters
    def params: SchedulerParams
    val clock: Clock
    val reset: Reset
    
    // TODO: remove inputStatus, replace ready signal with SchedulerInput
    // val inputStatus = Wire(new DecoupledIO(UInt(params.width.W)))
    val SchedulerInput = Wire(new DecoupledIO(UInt(params.width.W)))
    val SchedulerOutput = Wire(new DecoupledIO(UInt(params.width.W)))
    val status = Wire(UInt(2.W))

    val impl = Module(new SchedulerImp(params.width))

    impl.io.clock := clock
    impl.io.reset := reset.asBool

    SchedulerInput.ready := impl.io.input_ready

    impl.io.input_valid := SchedulerInput.valid

    // This line doesn't matter in our sample workload
    impl.io.output_ready := SchedulerOutput.ready

    impl.io.SchedulerInput := SchedulerInput.bits

    SchedulerOutput.bits := impl.io.SchedulerOutput

    SchedulerOutput.valid := impl.io.output_valid

    status := Cat(impl.io.input_ready, impl.io.output_valid)
    io.Scheduler_busy := impl.io.busy

    when (SchedulerInput.valid) {
        printf("Test Module Input is valid and value is %d\n", SchedulerInput.bits)
    }
    when (SchedulerOutput.valid) {
        printf("Test Module Output is ready and value is %d\n", SchedulerOutput.bits)
    }

    regmap(
        0x00 -> Seq(
            RegField.r(2, status)),
        0x04 -> Seq(
            RegField.w(params.width, SchedulerInput)
        ),
        0x08 -> Seq(
            RegField.r(params.width, SchedulerOutput)
        )
    )

}

class SchedulerImp(val w: Int) extends Module with HasSchedulerIO {
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

    io.SchedulerOutput := tmp

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
        ValInit := io.SchedulerInput
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

class SchedulerTileLink(params: SchedulerParams, beatBytes: Int)(implicit p: Parameters) extends TLRegisterRouter(
    params.address, "Scheduler", Seq("ucbbar,Scheduler"),
    beatBytes = beatBytes)(
        new TLRegBundle(params, _) with SchedulerTopIO)(
            new TLRegModule(params, _, _) with Scheduler)

class SchedulerAXI4(params: SchedulerParams, beatBytes: Int)(implicit p: Parameters)
  extends AXI4RegisterRouter(
    params.address,
    beatBytes=beatBytes)(
      new AXI4RegBundle(params, _) with SchedulerTopIO)(
      new AXI4RegModule(params, _, _) with Scheduler)


trait HaveScheduler {
    this: BaseSubsystem =>
        private val portName = "Scheduler"

        val Scheduler = p(SchedulerKey) match {
            case Some(params) => {
                if (params.useAXI4) {
                    val Scheduler = LazyModule(new SchedulerAXI4(params, pbus.beatBytes)(p))
                    pbus.toSlave(Some(portName)) {
                        Scheduler.node := AXI4Buffer() := TLToAXI4() := TLFragmenter(pbus.beatBytes, pbus.blockBytes, holdFirstDeny = true)
                    }
                    Some(Scheduler)
                } else {
                    val Scheduler = LazyModule(new SchedulerTileLink(params, pbus.beatBytes)(p))
                    pbus.toVariableWidthSlave(Some(portName)) { Scheduler.node }
                    Some(Scheduler)
                }
            }
            case None => None
        }
}

trait HaveSchedulerImp extends LazyModuleImp {
    val outer : HaveScheduler
    val Scheduler_busy = outer.Scheduler match {
        case Some(Scheduler) => {
            val busy = IO(Output(Bool()))
            busy := Scheduler.module.io.Scheduler_busy
            Some(busy)
        }
        case None => None
    }
}

class withScheduler(useAXI4: Boolean = false, useBlackBox: Boolean = false) extends Config((site, here, up) => {
    case SchedulerKey => Some(SchedulerParams(useAXI4 = useAXI4, useBlackBox = useBlackBox))
})