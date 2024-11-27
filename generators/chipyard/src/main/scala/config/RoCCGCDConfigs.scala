package chipyard

import chisel3._
import chisel3.util.{HasBlackBoxResource, RegEnable}
import freechips.rocketchip.diplomacy.{AsynchronousCrossing, LazyModule}
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile.{RoCCIO, LazyRoCC, LazyRoCCModuleImp, OpcodeSet, BuildRoCC}
import org.chipsalliance.cde.config.{Config, Parameters}

// -----------------------------------------------------------------
// Interface description
// -----------------------------------------------------------------
class DecouplerIO (xLen: Int = 32)(implicit p: Parameters) extends Bundle{
  // Control signals
  val clock = Input(Clock())
  val reset = Input(UInt(1.W))

  // RoCC interface
  val rocc_io = new RoCCIO(0, 0)

  // Decoupler-Controller interface
  val controller_io = Flipped(new ControllerIO(xLen))
}

class ControllerIO (xLen: Int = 32)(implicit p: Parameters) extends Bundle {
  // Request
  val rocc_req_rs1      = Input(UInt(xLen.W))
  val rocc_req_rs2      = Input(UInt(xLen.W))
  val rocc_req_valid    = Input(Bool())

  // Response
  val rocc_resp_data    = Output(UInt(xLen.W))
  val rocc_resp_valid   = Output(Bool())
  val rocc_resp_ready   = Output(Bool())
}

class AccelIO ()(implicit p: Parameters) extends Bundle {
  val iClk   = Input(Clock())
  val iRst   = Input(Bool())
  val iValid = Input(Bool())
  val iA     = Input(UInt(16.W))
  val iB     = Input(UInt(16.W))
  val oValid = Output(Bool())
  val oReady = Output(Bool())
  val oC     = Output(UInt(16.W))
}

class AccelControllerIO (xLen: Int = 32)(implicit p: Parameters) extends Bundle{
  // Control signals
  val clock = Input(Clock())
  val reset = Input(UInt(1.W))

  // Controller-Decoupler interface
  val controller_io = new ControllerIO(xLen)

  // Black box interface
  val accel_io = Flipped(new AccelIO())
}

// -----------------------------------------------------------------
// Decoupler description
// -----------------------------------------------------------------
class RoCCDecoupler (xLen: Int = 32)(implicit p: Parameters) extends Module {
  val io = IO(new DecouplerIO())

  // Assign Core request to Controller's input
  io.controller_io.rocc_req_rs1    := io.rocc_io.cmd.bits.rs2
  io.controller_io.rocc_req_rs2    := io.rocc_io.cmd.bits.rs1
  io.controller_io.rocc_req_valid  := io.rocc_io.cmd.valid

  // Assign Controller's output to Core response
  io.rocc_io.cmd.ready      := io.controller_io.rocc_resp_ready
  io.rocc_io.resp.bits.data := io.controller_io.rocc_resp_data
  io.rocc_io.resp.valid     := io.controller_io.rocc_resp_valid

  // Keep it for stable usage
  val rdR = RegEnable(io.rocc_io.cmd.bits.inst.rd, io.rocc_io.cmd.valid)
  io.rocc_io.resp.bits.rd   := rdR

  // Accel status
  io.rocc_io.busy       := false.B
  io.rocc_io.interrupt  := false.B

  // Memory request (disabled)
  io.rocc_io.mem.req.valid          := false.B
  io.rocc_io.mem.req.bits.addr      := 0.U
  io.rocc_io.mem.req.bits.tag       := 0.U
  io.rocc_io.mem.req.bits.cmd       := 0.U
  io.rocc_io.mem.req.bits.size      := 0.U
  io.rocc_io.mem.req.bits.signed    := false.B
  io.rocc_io.mem.req.bits.dprv      := 0.U
  io.rocc_io.mem.req.bits.dv        := false.B
  io.rocc_io.mem.req.bits.phys      := false.B
  io.rocc_io.mem.req.bits.no_alloc  := false.B
  io.rocc_io.mem.req.bits.no_xcpt   := false.B
  io.rocc_io.mem.req.bits.data      := 0.U
  io.rocc_io.mem.req.bits.mask      := 0.U
  // Memory response (disabled)
  io.rocc_io.mem.s1_kill            := false.B
  io.rocc_io.mem.s1_data.data       := 0.U
  io.rocc_io.mem.s1_data.mask       := 0.U
  io.rocc_io.mem.s2_kill            := false.B
  io.rocc_io.mem.keep_clock_enabled := true.B

  // Disable FPU, even you don't have it inside your system
  io.rocc_io.fpu_req.valid            := false.B
  io.rocc_io.fpu_req.bits.rm          := 0.U
  io.rocc_io.fpu_req.bits.fmaCmd      := 0.U
  io.rocc_io.fpu_req.bits.typ         := 0.U
  io.rocc_io.fpu_req.bits.fmt         := 0.U
  io.rocc_io.fpu_req.bits.in1         := 0.U
  io.rocc_io.fpu_req.bits.in2         := 0.U
  io.rocc_io.fpu_req.bits.in3         := 0.U
  io.rocc_io.fpu_req.bits.ldst        := false.B
  io.rocc_io.fpu_req.bits.wen         := false.B
  io.rocc_io.fpu_req.bits.ren1        := false.B
  io.rocc_io.fpu_req.bits.ren2        := false.B
  io.rocc_io.fpu_req.bits.ren3        := false.B
  io.rocc_io.fpu_req.bits.swap12      := false.B
  io.rocc_io.fpu_req.bits.swap23      := false.B
  io.rocc_io.fpu_req.bits.typeTagIn   := 0.U
  io.rocc_io.fpu_req.bits.typeTagOut  := 0.U
  io.rocc_io.fpu_req.bits.fromint     := false.B
  io.rocc_io.fpu_req.bits.toint       := false.B
  io.rocc_io.fpu_req.bits.fastpipe    := false.B
  io.rocc_io.fpu_req.bits.fma         := false.B
  io.rocc_io.fpu_req.bits.div         := false.B
  io.rocc_io.fpu_req.bits.sqrt        := false.B
  io.rocc_io.fpu_req.bits.wflags      := false.B
  io.rocc_io.fpu_resp.ready           := false.B
}

// -----------------------------------------------------------------
// Controller description
// -----------------------------------------------------------------
class RoCCController (xLen: Int = 32)(implicit p: Parameters) extends Module {
  val io = IO(new AccelControllerIO(xLen))

  // Assign signals to Accel's input
  io.accel_io.iClk      := io.clock
  io.accel_io.iRst      := io.reset
  io.accel_io.iValid    := io.controller_io.rocc_req_valid
  io.accel_io.iA        := io.controller_io.rocc_req_rs1
  io.accel_io.iB        := io.controller_io.rocc_req_rs2

  // Assign signals from Accel's output
  io.controller_io.rocc_resp_data  := io.accel_io.oC
  io.controller_io.rocc_resp_valid := io.accel_io.oValid
  io.controller_io.rocc_resp_ready  := io.accel_io.oReady
}

// -----------------------------------------------------------------
// Accelerator description
// -----------------------------------------------------------------
class GCD ()(implicit p: Parameters) extends BlackBox with HasBlackBoxResource {
  val io = IO(new AccelIO())

  addResource("/vsrc/GCD.v")
}

class RoccAccel (opcodes: OpcodeSet)(implicit p: Parameters) extends LazyRoCC (
  opcodes   = opcodes,
  nPTWPorts = 0,
  usesFPU   = false,
  roccCSRs  = Nil) {
  override lazy val module = new RoccAccelImp(this)
}

class RoccAccelImp(outer: RoccAccel)(implicit p: Parameters) extends LazyRoCCModuleImp(outer) {
  val xLen = 32

  // Instantiate the rocc modules
  val roccDecoupler  = Module(new RoCCDecoupler(xLen))
  val roccController = Module(new RoCCController(xLen))
  val roccAccel      = Module(new GCD())

  // Connect all together
  // 1. Connect RoCC interface with decoupler
  io <> roccDecoupler.io.rocc_io
  roccDecoupler.io.clock := clock
  roccDecoupler.io.reset := reset.asUInt
  // 2. Connect Controller interface with Decoupler interface
  roccController.io.controller_io <> roccDecoupler.io.controller_io
  roccController.io.clock := clock
  roccController.io.reset := reset.asUInt
  // 3. COnnect Accel interface with Controller interface  
  roccAccel.io <> roccController.io.accel_io 
}

class WithGCDAccel extends Config ((site, here, up) => {
  case BuildRoCC => up(BuildRoCC) ++ Seq(
    (p: Parameters) => {
      val myrocc = LazyModule.apply(new RoccAccel(OpcodeSet.custom0)(p))
      myrocc
    }
  )
})

