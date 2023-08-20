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
import freechips.rocketchip.rocket._
import freechips.rocketchip.subsystem.RocketTilesKey
import java.lang.reflect.Parameter

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
    val functionIndexInput = Wire(Flipped(DecoupledIO(UInt(w.W))))
    val functionNumParamInput = Wire(Flipped(DecoupledIO(UInt(w.W))))
    val functionParamInput = Wire(Flipped(DecoupledIO(UInt(w.W))))
    val numCoreScheduled = Wire(Flipped(DecoupledIO(UInt(w.W))))
    val busy = Output(Bool())
}

case class WorkloadContext (val w: Int) (
    funcID: Int = 0,
    params: StreamChannel = new StreamChannel(64)
)

class SchedulerToCoreIO (val w: Int) extends Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val ContextInput = Input(DecoupledIO(WorkloadContext()))
    val isReceived = Output(Bool())
}

class SchedulerCoreIOBundle (val w: Int) extends Bundle {
    val num_tiles = p(RocketTilesKey).size

    val cores = Vec(num_tiles, SchedulerToCoreIO)
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
    val OuterIO: SchedulerCoreIOBundle

    implicit val p: Parameters
    def params: SchedulerParams
    val clock: Clock
    val reset: Reset
    
    // TODO: remove inputStatus, replace ready signal with SchedulerInput
    // val inputStatus = Wire(new DecoupledIO(UInt(params.width.W)))
    val SchedulerInput = Wire(new DecoupledIO(UInt(params.width.W)))
    val SchedulerOutput = Wire(new DecoupledIO(UInt(params.width.W)))
    val status = Wire(UInt(2.W))

    val FunctionIDInput = Wire(new DecoupledIO(UInt(params.width.W)))
    val NumArguments = Wire(new DecoupledIO(UInt(params.width.W)))
    val ArgumentsInput = Wire(new DecoupledIO(UInt(params.width.W)))

    val impl = Module(new SchedulerImp(params.width))

    impl.io.clock := clock
    impl.io.reset := reset.asBool

    impl.io.functionIndexInput := FunctionIDInput
    impl.io.functionNumParamInput := NumArguments
    impl.io.functionParamInput := ArgumentsInput

    var chosenCore = -1

    for (i <- 0 until OuterIO.num_tiles) {
        if (OuterIO.cores(i).ContextInput.isReceived != False) {
            chosenCore = i
            break
        }
    }

    printf("Chosen Core is %d\n", chosenCore)
    
    // Packing up parameters as stream (WIP)
    val sm = DecoupledIO(new StreamChannel(64))
    sm.data := ArgumentsInput
    sm.last := true.B
    sm.keep := 0.U
    
    OuterIO.cores(i).ContextInput.funcID := FunctionIDInput
    OuterIO.cores(i).ContextInput.params := sm

    status := Cat(FunctionIDInput.ready, OuterIO.cores(i).isReceived)

    regmap(
        0x00 -> Seq(
            RegField.r(2, status)
        ),
        0x04 -> Seq(
            RegField.w(params.width, FunctionIDInput)
        ),
        0x08 -> Seq(
            RegField.w(params.width, NumArguments)
        ),
        0x12 -> Seq(
            RegField.r(params.width, SchedulerOutput)
        ),
        0x16 -> Seq(
            RegField.w(params.width, ArgumentsInput)
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
        val SchedulerTiles = tiles;
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

    val num_cores = outer.SchedulerTiles.size
    for (i <- 0 until num_cores) {
        val tile = outer.SchedulerTiles(i)
        outer.Scheduler.OuterIO.cores(i).ContextInput.params := tile.module.net.param_stream
        outer.Scheduler.OuterIO.cores(i).ContextInput.funcID := tile.module.net.funcID
        tile.module.net.isReceived := outer.Scheduler.OuterIO.cores(i).isReceived
    }
}

class withScheduler(useAXI4: Boolean = false, useBlackBox: Boolean = false) extends Config((site, here, up) => {
    case SchedulerKey => Some(SchedulerParams(useAXI4 = useAXI4, useBlackBox = useBlackBox))
})