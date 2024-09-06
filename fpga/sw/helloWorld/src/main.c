// #include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <time.h>
#include <riscv-pk/encoding.h>
#include "platform.h"
#include "kprintf.h"
// #include "libfdt.h"
// #include "kprintf.h"
#define REG32(p, i)	((p)[(i) >> 2])

#define DELAY_TIME 1000000
#define INPUT_MASK 0xffff0000

static volatile uint32_t * const gpio = (void *)(GPIO_CTRL_ADDR);

volatile uint32_t gpio_oval = 1;
volatile uint32_t gpio_ival = 0;

// static int fdt_translate_address(void *fdt, uint64_t reg, int parent, unsigned long *addr)
// {
// 	int i, rlen;
// 	int cell_addr, cell_size;
// 	const fdt32_t *ranges;
// 	uint64_t offset = 0, caddr = 0, paddr = 0, rsize = 0;

// 	cell_addr = fdt_address_cells(fdt, parent);
// 	if (cell_addr < 1)
// 		return -FDT_ERR_NOTFOUND;

// 	cell_size = fdt_size_cells(fdt, parent);
// 	if (cell_size < 0)
// 		return -FDT_ERR_NOTFOUND;

// 	ranges = fdt_getprop(fdt, parent, "ranges", &rlen);
// 	if (ranges && rlen > 0) {
// 		for (i = 0; i < cell_addr; i++)
// 			caddr = (caddr << 32) | fdt32_to_cpu(*ranges++);
// 		for (i = 0; i < cell_addr; i++)
// 			paddr = (paddr << 32) | fdt32_to_cpu(*ranges++);
// 		for (i = 0; i < cell_size; i++)
// 			rsize = (rsize << 32) | fdt32_to_cpu(*ranges++);
// 		if (reg < caddr || caddr >= (reg + rsize )) {
// 			//kprintf("invalid address translation\n");
// 			return -FDT_ERR_NOTFOUND;
// 		}
// 		offset = reg - caddr;
// 		*addr = paddr + offset;
// 	} else {
// 		/* No translation required */
// 		*addr = reg;
// 	}

// 	return 0;
// }

// int fdt_get_node_addr_size(void *fdt, int node, unsigned long *addr,
// 			   unsigned long *size)
// {
// 	int parent, len, i, rc;
// 	int cell_addr, cell_size;
// 	const fdt32_t *prop_addr, *prop_size;
// 	uint64_t temp = 0;

// 	parent = fdt_parent_offset(fdt, node);
// 	if (parent < 0)
// 		return parent;
// 	cell_addr = fdt_address_cells(fdt, parent);
// 	if (cell_addr < 1)
// 		return -FDT_ERR_NOTFOUND;

// 	cell_size = fdt_size_cells(fdt, parent);
// 	if (cell_size < 0)
// 		return -FDT_ERR_NOTFOUND;

// 	prop_addr = fdt_getprop(fdt, node, "reg", &len);
// 	if (!prop_addr)
// 		return -FDT_ERR_NOTFOUND;
// 	prop_size = prop_addr + cell_addr;

// 	if (addr) {
// 		for (i = 0; i < cell_addr; i++)
// 			temp = (temp << 32) | fdt32_to_cpu(*prop_addr++);
// 		do {
// 			if (parent < 0)
// 				break;
// 			rc  = fdt_translate_address(fdt, temp, parent, addr);
// 			if (rc)
// 				break;
// 			parent = fdt_parent_offset(fdt, parent);
// 			temp = *addr;
// 		} while (1);
// 	}
// 	temp = 0;

// 	if (size) {
// 		for (i = 0; i < cell_size; i++)
// 			temp = (temp << 32) | fdt32_to_cpu(*prop_size++);
// 		*size = temp;
// 	}

// 	return 0;
// }

void delay() {
	for (int i = 0; i < DELAY_TIME; i = i + 1);	
}

int main(int hartid, char **argv) {
  
  REG32(uart, UART_REG_TXCTRL) = UART_TXEN;
  kprintf("Hello world!!!\r\n");

  REG32(gpio, GPIO_OUTPUT_EN) = 0x0000ffff;
  REG32(gpio, GPIO_INPUT_EN)  = 0x00010000;

//   uint32_t nodeoffset;
//   uint32_t uart_reg;
//   int err;

//   // 1. Get the uart reg
//   nodeoffset = fdt_path_offset((void*)dtb, "/soc/serial");
//   if (nodeoffset < 0) while(1);
//   err = fdt_get_node_addr_size((void*)dtb, nodeoffset, &uart_reg, NULL);
  
  while (1) {
    gpio_ival = REG32(gpio, GPIO_INPUT_VAL) & INPUT_MASK;
    REG32(gpio, GPIO_OUTPUT_VAL) = gpio_oval;
    if (gpio_ival == 0) {
      gpio_oval = gpio_oval << 1;
      if (gpio_oval == 0x00010000) gpio_oval = 1;
    }
    else {
      if (gpio_oval == 1) gpio_oval = 0x00008000;
      else gpio_oval = gpio_oval >> 1;
    }
    delay();
  }

  return 0;
}
