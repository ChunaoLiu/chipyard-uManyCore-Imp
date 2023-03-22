#include <sys/socket.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <string.h>
#include <net/if.h>
#include <stdio.h>
#include <linux/if.h>
#include <linux/if_tun.h>
#include <unistd.h>

#define IFF_TAP 0x0002
#define IFF_NO_PI 0x1000

int main() {
    printf("Hello World!\n");
    const int fd = open("/dev/net/tun", O_RDWR);
    if (fd != -1) {
        struct ifreq ifr;

        memset(&ifr, 0, sizeof(ifr));
        ifr.ifr_flags = IFF_TAP | IFF_NO_PI;

        strncpy(ifr.ifr_name, "tap0", IFNAMSIZ);
    } else {
        printf("Error: Could not open tap device\n");
        return 1;
    }

    while (1) {
        char buf[512];
        int nread = read(fd, buf, sizeof(buf));
        if (nread < 0) {
            printf("Error: Something happened while reading!\n");
            close(fd);
            return 1;
        }

        if (nread == 0) continue;

        printf("package received: %s\n", buf);
        return 0;

    }
}