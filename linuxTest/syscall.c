#include <stdlib.h>
#include <stdio.h>

int main() {
    printf("Hello World!\n");
    // system("lscpu");
    FILE *fp = popen("lscpu", "r");
    char path[4096];

    if (fp == NULL) {
        printf("Failed to run command\n" );
        exit(1);
    }

    while (fgets(path, sizeof(path), fp) != NULL) {
        printf("%s", path);
    }

    /* close */
    pclose(fp);

    printf("Goodbye World!\n");
    return 0;
}