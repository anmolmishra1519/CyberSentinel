"""
CyberSentinel AI - Training Dataset Generator
==============================================

WHY A GENERATED DATASET (READ THIS FIRST):
Public flow-level intrusion datasets that match our exact feature set
(CICIDS2017, CIC-DDoS2019) are distributed as multi-gigabyte downloads from
university servers that are not reachable from this build environment.
NSL-KDD/KDD'99 are reachable but use connection-level features (service,
flag, num_failed_logins...) that do not map onto plain flow features like
packets/sec or source/destination IP, which the problem statement explicitly
asks for.

Instead, this script generates a labelled dataset using a session-based
simulator that encodes the SAME statistical signatures published intrusion
research (CICIDS2017 in particular) documents for these attack families:

  - DDoS/DoS:     very short flow duration, very high packet rate, small
                  packet size (SYN/UDP floods), many sources concentrating
                  on one destination in a short window.
  - Port Scan:    extremely short flows, 1-3 packets, near-zero bytes,
                  one source contacting many DISTINCT destination ports in
                  a short window.
  - Brute Force:  moderate duration, small-to-moderate packet counts aimed
                  at a fixed auth service port (22/21/3389/23/3306), the
                  same source repeatedly hitting the same destination+port
                  in a short window.
  - Normal:       everyday client/server sessions across common service
                  ports, no abnormal concentration or repetition.

Three CONTEXTUAL/AGGREGATE features are computed the same way real NIDS
tools (CICFlowMeter, Zeek) compute them: by counting related flows inside a
rolling time window, not by reading the label. This is what gives the model
real, non-circular signal to separate Port Scan / DDoS / Brute Force, which
often look similar on a single isolated flow:

  - dst_conn_count_60s          : # flows to the same destination IP in the
                                   last 60s window (high for DDoS)
  - unique_dst_ports_60s        : # distinct destination ports the SAME
                                   source IP touched in the last 60s
                                   (high for Port Scan)
  - repeat_conn_same_dst_port_60s: # repeat attempts by the same source to
                                   the same destination+port in the last 60s
                                   (high for Brute Force)

Run:  python generate_dataset.py
Output: network_flows_dataset.csv (this directory)
"""

import numpy as np
import pandas as pd
import random
import ipaddress
from datetime import datetime, timedelta

random.seed(42)
np.random.seed(42)

PROTOCOLS = ["TCP", "UDP"]
COMMON_SERVICE_PORTS = [80, 443, 53, 22, 25, 110, 143, 3306, 8080, 8443]
AUTH_PORTS = [22, 21, 3389, 23, 3306]

rows = []
flow_id = 0
base_time = datetime(2026, 8, 24, 9, 0, 0)


def rand_ip(octet_pool):
    return f"{octet_pool}.{random.randint(1, 254)}"


INTERNAL_POOL = "192.168.1"
SERVER_POOL = "10.0.0"
EXTERNAL_POOL_BASE = [f"203.0.{n}" for n in range(1, 60)]


def new_row(src_ip, dst_ip, protocol, src_port, dst_port, duration, packet_count,
            avg_pkt_size, label, ts):
    # small measurement-style jitter so no two flows are perfectly clean signals
    duration = duration * np.random.uniform(0.9, 1.1)
    packet_count = packet_count * np.random.uniform(0.92, 1.08)
    avg_pkt_size = avg_pkt_size * np.random.uniform(0.85, 1.15)
    duration = max(duration, 0.001)
    packet_count = max(packet_count, 1)
    byte_count = int(packet_count * avg_pkt_size)
    pps = round(packet_count / duration, 2)
    bps = round(byte_count / duration, 2)
    return {
        "flowId": flow_id,
        "sourceIP": src_ip,
        "destinationIP": dst_ip,
        "protocol": protocol,
        "sourcePort": src_port,
        "destinationPort": dst_port,
        "flowDuration": round(duration, 3),
        "packetCount": int(packet_count),
        "byteCount": byte_count,
        "packetsPerSecond": pps,
        "bytesPerSecond": bps,
        "timestamp": ts.isoformat(),
        "label": label,
    }


# ---------------------------------------------------------------
# 1. NORMAL TRAFFIC — independent client/server sessions.
#    A minority are deliberately "heavy" (streaming, backups, bulk
#    downloads, an admin SSH session) so Normal overlaps the edges of
#    the attack classes instead of being perfectly separable — this is
#    what makes the evaluation numbers below honest rather than
#    artificially perfect.
# ---------------------------------------------------------------
N_NORMAL_SESSIONS = 6400
t = base_time
for i in range(N_NORMAL_SESSIONS):
    flow_id += 1
    src = rand_ip(INTERNAL_POOL)
    dst = rand_ip(SERVER_POOL) if random.random() < 0.5 else random.choice(EXTERNAL_POOL_BASE) + f".{random.randint(1,254)}"
    proto = random.choices(PROTOCOLS, weights=[0.8, 0.2])[0]

    roll = random.random()
    if roll < 0.06:
        # heavy/bursty normal session (video call, big download) — overlaps DDoS region
        dport = random.choice([443, 8443, 80])
        sport = random.randint(1024, 65535)
        duration = np.random.uniform(0.2, 4)
        packet_count = np.random.randint(150, 900)
        avg_pkt_size = max(np.random.normal(300, 90), 50)
    elif roll < 0.10:
        # legitimate single admin login to an auth port — overlaps BruteForce region
        dport = random.choice(AUTH_PORTS)
        sport = random.randint(1024, 65535)
        duration = np.random.uniform(0.3, 3)
        packet_count = np.random.randint(6, 35)
        avg_pkt_size = max(np.random.normal(180, 40), 50)
    elif roll < 0.13:
        # quick DNS/ping-style probe — overlaps PortScan region
        dport = random.choice([53, 443, 80])
        sport = random.randint(1024, 65535)
        duration = np.random.uniform(0.002, 0.1)
        packet_count = np.random.randint(1, 4)
        avg_pkt_size = max(np.random.normal(70, 15), 40)
    else:
        dport = random.choice(COMMON_SERVICE_PORTS)
        sport = random.randint(1024, 65535)
        duration = np.random.uniform(0.4, 30)
        packet_count = np.random.randint(8, 260)
        avg_pkt_size = max(np.random.normal(500, 150), 60)

    t = t + timedelta(seconds=random.uniform(0.2, 4))
    rows.append(new_row(src, dst, proto, sport, dport, duration, packet_count, avg_pkt_size, "Normal", t))

# ---------------------------------------------------------------
# 2. DDoS / DoS — many bot sources flooding ONE destination briefly
# ---------------------------------------------------------------
N_DDOS_SESSIONS = 105
for i in range(N_DDOS_SESSIONS):
    victim = rand_ip(SERVER_POOL)
    dport = random.choice([80, 443, 53])
    proto = random.choices(PROTOCOLS, weights=[0.7, 0.3])[0]
    n_bots = np.random.randint(25, 90)
    window_start = t + timedelta(seconds=random.uniform(5, 20))
    stealthy = random.random() < 0.25  # low-and-slow DDoS variant, harder to separate from bursty normal
    for b in range(n_bots):
        flow_id += 1
        src = random.choice(EXTERNAL_POOL_BASE) + f".{random.randint(1,254)}"
        sport = random.randint(1024, 65535)
        if stealthy:
            duration = np.random.uniform(0.2, 3)
            packet_count = np.random.randint(120, 700)
        else:
            duration = np.random.uniform(0.01, 1.2)
            packet_count = np.random.randint(300, 4500)
        avg_pkt_size = np.random.normal(90, 35)
        avg_pkt_size = max(avg_pkt_size, 40)
        ts = window_start + timedelta(milliseconds=random.uniform(0, 900))
        rows.append(new_row(src, victim, proto, sport, dport, duration, packet_count, avg_pkt_size, "DDoS", ts))
    t = window_start + timedelta(seconds=2)

# ---------------------------------------------------------------
# 3. PORT SCAN — one source sweeping many destination ports quickly
# ---------------------------------------------------------------
N_SCAN_SESSIONS = 38
for i in range(N_SCAN_SESSIONS):
    scanner = random.choice(EXTERNAL_POOL_BASE) + f".{random.randint(1,254)}"
    target = rand_ip(SERVER_POOL)
    proto = "TCP"
    n_ports = np.random.randint(20, 300)
    ports = random.sample(range(1, 10000), min(n_ports, 9999))
    window_start = t + timedelta(seconds=random.uniform(3, 15))
    for j, dport in enumerate(ports):
        flow_id += 1
        sport = random.randint(1024, 65535)
        duration = np.random.uniform(0.001, 0.08)
        packet_count = np.random.randint(1, 3)
        avg_pkt_size = np.random.normal(50, 8)
        avg_pkt_size = max(avg_pkt_size, 40)
        ts = window_start + timedelta(milliseconds=j * random.uniform(1, 15))
        rows.append(new_row(scanner, target, proto, sport, dport, duration, packet_count, avg_pkt_size, "PortScan", ts))
    t = window_start + timedelta(seconds=2)

# ---------------------------------------------------------------
# 4. BRUTE FORCE — one source repeatedly hitting one auth service
# ---------------------------------------------------------------
N_BF_SESSIONS = 90
for i in range(N_BF_SESSIONS):
    attacker = random.choice(EXTERNAL_POOL_BASE) + f".{random.randint(1,254)}"
    target = rand_ip(SERVER_POOL)
    dport = random.choice(AUTH_PORTS)
    proto = "TCP"
    n_attempts = np.random.randint(15, 120)
    window_start = t + timedelta(seconds=random.uniform(3, 15))
    for j in range(n_attempts):
        flow_id += 1
        sport = random.randint(1024, 65535)
        duration = np.random.uniform(0.3, 3.5)
        packet_count = np.random.randint(6, 40)
        avg_pkt_size = np.random.normal(180, 40)
        avg_pkt_size = max(avg_pkt_size, 60)
        ts = window_start + timedelta(seconds=j * random.uniform(0.3, 1.5))
        rows.append(new_row(attacker, target, proto, sport, dport, duration, packet_count, avg_pkt_size, "BruteForce", ts))
    t = window_start + timedelta(seconds=2)

df = pd.DataFrame(rows)
df = df.sort_values("timestamp").reset_index(drop=True)
df["timestamp"] = pd.to_datetime(df["timestamp"])

# ---------------------------------------------------------------
# CONTEXTUAL / AGGREGATE FEATURES (rolling 60s windows, causal — only
# looks backward, exactly like a real streaming NIDS would compute it)
# ---------------------------------------------------------------
df = df.sort_values("timestamp").reset_index(drop=True)
dst_conn_count = np.zeros(len(df), dtype=int)
unique_dst_ports = np.zeros(len(df), dtype=int)
repeat_conn = np.zeros(len(df), dtype=int)

from collections import deque, defaultdict

dst_window = defaultdict(deque)               # destIP -> deque[timestamps]
src_port_window = defaultdict(lambda: defaultdict(deque))  # srcIP -> destPort -> deque[timestamps]
src_dstport_window = defaultdict(deque)        # srcIP -> deque[(ts, dstPort)]

WINDOW = timedelta(seconds=60)

for idx, r in df.iterrows():
    ts = r["timestamp"]

    dq = dst_window[r["destinationIP"]]
    while dq and ts - dq[0] > WINDOW:
        dq.popleft()
    dst_conn_count[idx] = len(dq)
    dq.append(ts)

    sdq = src_dstport_window[r["sourceIP"]]
    while sdq and ts - sdq[0][0] > WINDOW:
        sdq.popleft()
    unique_dst_ports[idx] = len(set(p for _, p in sdq))
    sdq.append((ts, r["destinationPort"]))

    key = (r["sourceIP"], r["destinationIP"], r["destinationPort"])
    rq = src_port_window[r["sourceIP"]][r["destinationPort"]]
    while rq and ts - rq[0] > WINDOW:
        rq.popleft()
    repeat_conn[idx] = len(rq)
    rq.append(ts)

df["dstConnCount60s"] = dst_conn_count
df["uniqueDstPorts60s"] = unique_dst_ports
df["repeatConnSameDstPort60s"] = repeat_conn

df.to_csv("network_flows_dataset.csv", index=False)
print("Generated", len(df), "flow records")
print(df["label"].value_counts())
