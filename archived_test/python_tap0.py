from curses import flash
from scapy.all import *
import time
import binascii
# attach to the tap interface
# modprobe tun
# t = TunTapInterface("tap0")
TEST_IFACE = "tap0"
NIC_MAC="ff:ff:ff:ff:ff:ff"
MY_MAC="08:55:66:77:88:08"
src_ip="0.0.0.0"
NIC_IP="0.0.0.0"
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

counter = 0

def prn(x):

    global counter

    if IPv6 in x[0]:
        print("-------")
        print("ICMP in packet.....")
        print("-------")
        return
    # global flag
    # if flag == False:
    #     x.show()
    #     y = Ether(dst=NIC_MAC, src=MY_MAC) / IP(src=src_ip, dst=NIC_IP) / TCP(sport=333, dport=222, seq=112344) / "CCCCCCCCCCCCCCCCCCCCCCCCCC"
    #     y.show2()
    #     sendp(y, iface="br0", return_packets=True)
    #     flag = True
    
    
    if (counter < 2 and TCP not in x[1]):
        x.show2()
        if(counter == 0):
            y = Ether(dst=NIC_MAC, src=MY_MAC) / IP(src=src_ip, dst=NIC_IP) / TCP(sport=333, dport=222, seq=112344) / "Hello from purdue11111!!" 
        else:
            y = Ether(dst=NIC_MAC, src=MY_MAC) / IP(src=src_ip, dst=NIC_IP) / TCP(sport=333, dport=222, seq=112344) / "Hello from purdue22222!!"
        print("\n\nhere is y.show2()\n\n")
        y.show2()
        sendp(y, iface="br0")
        counter += 1

    
    
    
    # if(x in dict):
    #     if (dict[x] != 2):
    #         x.show2()
    #         print("Payload sent-back is: ")
    #         y = Ether(dst=NIC_MAC, src=MY_MAC) / IP(src=src_ip, dst=NIC_IP) / TCP(sport=333, dport=222, seq=112344) / "Hello from purdue22222!!"
    #         y.show2()
    #         sendp(y, iface="br0")
    #         # z = sendp(y, iface="br0", return_packets=True)
    #         # for packet in z:
    #         #     print("-----------")
    #         #     packet.show2()
    #         dict[x] = 2
    #         dict[y] = 2
    # else:
    #     x.show2()
    #     print("Payload sent-back is: ")
    #     y = Ether(dst=NIC_MAC, src=MY_MAC) / IP(src=src_ip, dst=NIC_IP) / TCP(sport=333, dport=222, seq=112344) / "Hello from purdue11111!!"
    #     y.show2()
    #     sendp(y, iface="br0")
    #     dict[x] = 1
    #     dict[y] = 2

sniffer = AsyncSniffer(iface=TEST_IFACE, timeout=20000000, prn=prn)
sniffer.start()
time.sleep(1)
sniffer.join()
print(sniffer.results)