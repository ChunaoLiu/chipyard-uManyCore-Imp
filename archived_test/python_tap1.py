from scapy.all import *
import time
import binascii
# attach to the tap interface
# modprobe tun
t = TunTapInterface("tap1")
TEST_IFACE = "br0"
NIC_MAC="ff:ff:ff:ff:ff:ff"
MY_MAC="ff:ff:ff:ff:ff:ff"
src_ip="0.0.0.0"
NIC_IP="0.0.0.0"
while True:
    packet = Ether(dst=NIC_MAC, src=MY_MAC) / IP(src=src_ip, dst=NIC_IP) / TCP(sport=333, dport=222, seq=112344) / "Hello From Purdue"
    sendp(packet, iface=TEST_IFACE)
    time.sleep(90)
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
# def prn(x):
#     x.show2()
# sniffer = AsyncSniffer(iface=TEST_IFACE, timeout=20000000, prn=prn)
# sniffer.start()
# time.sleep(1)
# sniffer.join()
# print(sniffer.results)