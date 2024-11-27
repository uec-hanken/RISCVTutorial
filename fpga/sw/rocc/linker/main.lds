OUTPUT_ARCH("riscv")
ENTRY(_prog_start)

INCLUDE memory.lds

PHDRS
{
  text PT_LOAD;
  data PT_LOAD;
  bss PT_LOAD;
}

SECTIONS
{
  PROVIDE(_ram = ORIGIN(memory_mem));
  PROVIDE(_ram_end = _ram + LENGTH(memory_mem));

  .text ALIGN((ORIGIN(memory_mem) + 0x0), 8) : AT(ALIGN((ORIGIN(memory_mem) + 0x0), 8)) {
    PROVIDE(_ftext = .);
    *(.text.init)
    *(.text.unlikely .text.unlikely.*)
    *(.text .text.* .gnu.linkonce.t.*)
    PROVIDE(_etext = .);
    . += 0x40; /* to create a gap between .text and .data b/c ifetch can fetch ahead from .data */
  } >memory_mem  :text

  .eh_frame ALIGN((ADDR(.text) + SIZEOF(.text)), 8) : AT(ALIGN((LOADADDR(.text) + SIZEOF(.text)), 8)) {
    *(.eh_frame)
  } >memory_mem  :text

  .srodata ALIGN((ADDR(.eh_frame) + SIZEOF(.eh_frame)), 8) : AT(ALIGN((LOADADDR(.eh_frame) + SIZEOF(.eh_frame)), 8)) ALIGN_WITH_INPUT {
    *(.srodata.cst16)
    *(.srodata.cst8)
    *(.srodata.cst4)
    *(.srodata.cst2)
    *(.srodata.*)
  } >memory_mem  :data

  .data ALIGN((ADDR(.srodata) + SIZEOF(.srodata)), 8) : AT(ALIGN((LOADADDR(.srodata) + SIZEOF(.srodata)), 8)) ALIGN_WITH_INPUT {
    *(.data .data.* .gnu.linkonce.d.*)
    *(.tohost) /* TODO: Support sections that aren't explicitly listed in this linker script */
  } >memory_mem  :data

  .sdata ALIGN((ADDR(.data) + SIZEOF(.data)), 8) : AT(ALIGN((LOADADDR(.data) + SIZEOF(.data)), 8)) ALIGN_WITH_INPUT {
    *(.sdata .sdata.* .gnu.linkonce.s.*)
  } >memory_mem  :data

  .rodata ALIGN((ADDR(.sdata) + SIZEOF(.sdata)), 8) : AT(ALIGN((LOADADDR(.sdata) + SIZEOF(.sdata)), 8)) ALIGN_WITH_INPUT {
    *(.rodata .rodata.* .gnu.linkonce.r.*)
    *(.dtb) /* Must be last if this code is added to RC's BootROM */
  } >memory_mem  :data

  PROVIDE(_data = ADDR(.rodata));
  PROVIDE(_data_lma = LOADADDR(.rodata));
  PROVIDE(_edata = .);

  .bss ALIGN((ORIGIN(memory_mem) + 0x0), 8) : AT(ALIGN((ORIGIN(memory_mem) + 0x0), 8)) ALIGN(8) {
    PROVIDE(_fbss = .);
    PROVIDE(__global_pointer$ = . + 0x7C0);
    *(.sbss .sbss.* .gnu.linkonce.sb.*)
    *(.bss .bss.* .gnu.linkonce.b.*)
    . = ALIGN(8);
    PROVIDE(_ebss = .);
  } >memory_mem  :bss

  PROVIDE(_end = .);
  PROVIDE(_sp = .);
  PROVIDE(_heap_end = .);

}
