# -*- coding: utf-8 -*-

from argparse import ArgumentParser
from scapy.all import *

def beacon(interface, ssid, mac_address):
    sendp(RadioTap()/
          Dot11(type=0,subtype=8,
                addr1="ff:ff:ff:ff:ff:ff",
                addr2=mac_address,
                addr3=mac_address)/
          Dot11Beacon(cap='ESS')/
          Dot11Elt(ID='SSID', info=ssid, len=len(ssid))/
          Dot11Elt(ID='Rates', info="\x02\x04\x0b\x0c\x12\x16\x18\x24"), # TODO: make parameter
          iface=interface,
          loop=False,
          verbose=False)

parser = ArgumentParser()
parser.add_argument("-i", "--interface", dest="interface", required=True, help="WiFi interface name", metavar="INTERFACE_NAME")
parser.add_argument("-s", "--ssid", dest="ssid", required=True, help="SSID to ask for", metavar="SSID")
parser.add_argument("-m", "--mac", dest="mac", required=True, help="MAC address to use", metavar="MAC_ADDRESS")

args = parser.parse_args()

beacon(args.interface, args.ssid, args.mac)
