#!/usr/bin/env bash

ip tuntap add mode tap dev tap0 user $USER
ip link set tap0 up
ip addr add 0.0.0.0/24 dev tap0

ip tuntap add mode tap dev tap1 user $USER
ip link set tap1 up
ip addr add 0.0.0.0/24 dev tap1

brctl addbr br0
brctl addif br0 tap0 tap1
ip link set br0 up