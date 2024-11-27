// See LICENSE.Sifive for license details.
#include <platform.h>
#include <smp.h>
#include "common.h"

  .section .text.init
  .option norvc
  .globl _prog_start
_prog_start:
  // perform boot if hart_0, otherwise sleep
  csrr s0, mhartid
  bne s0, zero, _stop
  csrw mie, s2
  li sp, (PAYLOAD_DEST + 0x3ff0)
  csrr a0, mhartid
  call main
  li s1, MEMORY_MEM_ADDR
  jr s1
_stop:
  wfi
_loopback:
  j _loopback
