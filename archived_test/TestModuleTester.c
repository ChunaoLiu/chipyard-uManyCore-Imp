#include "mmio.h"
#include <stdio.h>

#define STATUS 0x1000
#define TEST_MODULE_INPUT 0x1004

int main(void)
{
  uint32_t result, TestModuleInput = 20;

  printf("Begin experiment...\n");

  // wait for peripheral to be ready
  while ((reg_read8(STATUS) & 0x2) == 0) ;

  printf("Finished waiting for TestModule...\n");

  reg_write32(TEST_MODULE_INPUT, TestModuleInput);


  // wait for peripheral to complete
  while ((reg_read8(STATUS) & 0x1) == 0) ;

  result = reg_read32(TEST_MODULE_INPUT);

  printf("Hardware result %d\n", result);
  return 0;
}
