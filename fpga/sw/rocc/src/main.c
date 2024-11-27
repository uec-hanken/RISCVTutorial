// #include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <time.h>
#include <riscv-pk/encoding.h>
#include "platform.h"
#include "kprintf.h"
#include "rocc.h"
#define REG32(p, i)	((p)[(i) >> 2])

// gcd in software
int gcd(int a, int b) {
  while (b != 0) {
    if (a > b)
      a = a - b;
    else
      b = b - a;
  }
  return a;
}

int main(int hartid, char **argv) {
  
  REG32(uart, UART_REG_TXCTRL) = UART_TXEN;
  
  // Initialize
  int a = 10240; // first input
  int b = 15;    // second input
  int hardRes = 0; // Store result from RoCC GCD
  int softRes = 0; // Store result from software-based GCD
  
  // Call RoCC GCD by custom instruction
  ROCC_INSTRUCTION_DSS(CUSTOM0, hardRes, a, b, 0);

  // Call software-based GCD
  softRes = gcd(a, b);

  // Print the results from software and hardware
  kprintf("GCD in Software: %l\r\n", softRes);
  kprintf("GCD in RoCC: %l\r\n", hardRes);

  while (1);
  // dead code
  return 0;
}
