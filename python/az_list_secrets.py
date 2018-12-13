#!/usr/bin/env python
import sys
import subprocess
import json

vault_name = sys.argv[1]

print("Connecting to Azure Key Vault: " + vault_name)

listing = subprocess.check_output(["az", "keyvault", "secret", "list", "--vault-name", vault_name])

print("\nListing:")

print(listing)

entries = json.loads(listing)

print("Secrets:\n")

for entry in entries:
  secret_id = entry['id']

  raw_secret = subprocess.check_output(["az", "keyvault", "secret", "show", "--id", secret_id, "--vault-name", vault_name])

  secret = json.loads(raw_secret)

  secret_name = secret_id.split("/")[-1]
  secret_value = secret['value']

  print("Name  | " + secret_name)
  print("Value | " + secret_value + "\n")
