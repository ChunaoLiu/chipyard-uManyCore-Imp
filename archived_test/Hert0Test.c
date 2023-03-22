#include <stdio.h>
#include <unistd.h>
#include <string.h>

static inline unsigned long hartid(void)
{
        unsigned long id;
        asm volatile ("csrr %0, mhartid"  : "=r"(id));
        return id;
}

int main() {
    char hello_bro[18] = "Hello from Hart0\n";
    fflush(stdout);
    unsigned long id = hartid();
    char my_id[10];
    sprintf(my_id, "%ld", id);
    write(1, hello_bro, 18);
    write(1, my_id, 1);
    return 0;
}

int __main() {

    char hello_bro[18] = "Hello from Hart1\n";
    fflush(stdout);
    unsigned long id = hartid();
    char my_id[10];
    sprintf(my_id, "%ld", id);
    write(1, hello_bro, 18);
    write(1, my_id, 1);
    return 0;
}