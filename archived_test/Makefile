C_FLG = gcc -g
C_RISCV_ADDON = riscv64-unknown-linux-gnu-gcc -g -std=gnu99 -O2 -fno-common -fno-builtin-printf -Wall -s
C_RISCV = riscv64-unknown-elf-gcc -g
C_SPIKE_OBJ_ARG = -fno-common -fno-builtin-printf -specs=htif_nano.specs -c
C_SPIKE_ARG = -static -specs=htif_nano.specs 

default: hello riscv-hello spike_obj spike riscv-pkg Hert0TestObj Hert0Test test_spike_obj test_spike

target: hello riscv-hello spike_obj spike riscv-pkg Hert0TestObj Hert0Test test_spike_obj test_spike

hello: 
	$(C_FLG) -o hello_c hello.c
riscv-hello:
	$(C_RISCV) -o hello_riscv hello.c
riscv-pkg:
	$(C_RISCV_ADDON) -DHAVE_PCAP -o simple_pkg simple_pkg.c
spike_obj:
	$(C_RISCV) $(C_SPIKE_OBJ_ARG) -o hello_spike.o hello.c
spike:
	$(C_RISCV) $(C_SPIKE_ARG) -o hello_spike.riscv hello_spike.o

Hert0TestObj:
	$(C_RISCV) $(C_SPIKE_OBJ_ARG) -o Hert0Test.o Hert0Test.c

Hert0Test:
	$(C_RISCV) $(C_SPIKE_ARG) -o Hert0Test.riscv Hert0Test.o

test_spike_obj:
	$(C_RISCV) $(C_SPIKE_OBJ_ARG) -o TestModuleTester.o TestModuleTester.c
test_spike:
	$(C_RISCV) $(C_SPIKE_ARG) -o TestModuleTester.riscv TestModuleTester.o