package chipyard.fpga.arty35t

import chisel3.{Bool, Wire}
import freechips.rocketchip.diplomacy.InModuleBody
import org.chipsalliance.cde.config.Parameters
import sifive.fpgashells.shell.xilinx.Series7Shell
import sifive.fpgashells.shell.{ClockInputOverlayKey, ClockInputShellInput, DDROverlayKey, DDRShellInput, GPIOOverlayKey, GPIOShellInput, JTAGDebugOverlayKey, JTAGDebugShellInput, SPIOverlayKey, SPIShellInput, UARTOverlayKey, UARTShellInput}

abstract class Arty35TShellCustomOverlays()(implicit p: Parameters) extends Series7Shell {
  // System
  val pllReset = InModuleBody { Wire(Bool()) }
  val sys_clock = Overlay(ClockInputOverlayKey, new SysClockArtyShellPlacer(this, ClockInputShellInput()))

  // Peripheries
  val jtag  = Overlay(JTAGDebugOverlayKey, new JTAGDebugArtyShellPlacer(this, JTAGDebugShellInput()))
  val uart  = Overlay(UARTOverlayKey, new UARTArtyShellPlacer(this, UARTShellInput()))
  val sdio  = Overlay(SPIOverlayKey, new SDIOArtyShellPlacer(this, SPIShellInput()))
  val gpio  = Seq.tabulate(24)(i => {Overlay(GPIOOverlayKey, new GPIOArtyShellPlacer(this, GPIOShellInput()))})
}
