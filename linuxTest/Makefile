C_FLG = gcc -g
C_RISCV_ADDON = riscv64-unknown-linux-gnu-gcc -g -std=gnu99 -O2 -fno-common -fno-builtin-printf -Wall -s
C_RISCV = riscv64-unknown-elf-gcc -g
C_SPIKE_OBJ_ARG = -fno-common -fno-builtin-printf -c
C_SPIKE_ARG = -static 

default: syscall syscall_obj syscall_spike

target: syscall syscall_obj syscall_spike

syscall: 
	$(C_FLG) -o syscall_c syscall.c
syscall_obj:
	$(C_RISCV_ADDON) $(C_SPIKE_OBJ_ARG) -o syscall_spike.o syscall.c
syscall_spike:
	$(C_RISCV_ADDON) $(C_SPIKE_ARG) -o syscall_spike.riscv syscall_spike.o