from scapy.all import *
import time
import binascii
# attach to the tap interface
# modprobe tun
# t = TunTapInterface("tap0")
TEST_IFACE = "tap0"
# NIC_MAC="08:11:22:33:44:08"
# MY_MAC="08:55:66:77:88:08"
# src_ip="0.0.0.0"
# NIC_IP="0.0.0.0"
# time.sleep(5)
# packet = Ether(dst=NIC_MAC, src=MY_MAC) / IP(src=src_ip, dst=NIC_IP) / TCP(sport=333, dport=222, seq=112344)
# sendp(packet, iface=TEST_IFACE)
print("----------- TEST -----------")
# prn = lambda x: x.show2()
# prn = lambda x: bytes(x)
# def prn(x):
#     if IP in x:
#         pkt = binascii.hexlify(bytes(x[IP]))
#         print("the following is an IP packet....")
#         print(pkt)
#     elif ICMP in x:
#         print("the following is an ICMP packet....")
#         x.show2()
def prn(x):
    if IPv6 in x[0]:
        print("-------")
        print("ICMP in packet.....")
        print("-------")
        return
    else:
        x.show2()
        sendp(x, iface=TEST_IFACE)

sniffer = AsyncSniffer(iface=TEST_IFACE, timeout=20000000, prn=prn)
sniffer.start()
time.sleep(1)
sniffer.join()
print(sniffer.results)