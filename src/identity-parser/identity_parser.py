import json
import random
import sys

usage = " usage:\n" \
        " python3 ./identity_parser.py <file-name>"

if len(sys.argv) < 2:
    print(usage)
    sys.exit(1)

in_file = sys.argv[1]
out_file = "identities.json"
roles = ["center-a", "center-b", "center-c", "center-d", "center-e"]
identities = []

with open(in_file) as f:
    for line in f:
        device = json.loads(line)
        identity = {
            "role": random.choice(roles),
            "uuid": device["UUID"],
            "password": device["deviceCredentials"]["password"]
        }
        identities.append(identity)

with open(out_file, "w") as f:
    json.dump(identities, fp=f)
