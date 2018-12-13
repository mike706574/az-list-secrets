package main

import (
	"encoding/json"
	"fmt"
	"log"
	"os"
	"os/exec"
	"strings"
	"sync"
)

type ListingEntry struct {
  Id string
}

type ShowEntry struct {
  Value string
}

func listSecrets(vaultName string) []ListingEntry {
	cmd := exec.Command("az", "keyvault", "secret", "list", "--vault-name", vaultName)

  out, err := cmd.CombinedOutput()

	if err != nil {
		log.Fatalf("Listing failed with %s:\n%s\n", err, out)
	}

	fmt.Printf("Listing:\n%s\n", string(out))

	var entries []ListingEntry

	json.Unmarshal(out, &entries)

	return entries
}

func printSecret(vaultName string, secretId string) {
	cmd := exec.Command("az", "keyvault", "secret", "show", "--id", secretId, "--vault-name", vaultName)

	out, err := cmd.CombinedOutput()

	if err != nil {
		log.Fatalf("Show failed with %s:\n%s\n", err, out)
	}

	var showEntry ShowEntry

	json.Unmarshal(out, &showEntry)

	splitId := strings.Split(secretId, "/")
	name := splitId[len(splitId) - 1]

	fmt.Printf("Name  | %s\nValue | %s\n\n", name, showEntry.Value)
}

func main() {
	vaultName := os.Args[1]

	fmt.Printf("Connecting to Azure Key Vault: %s\n\n", vaultName)

	listingEntries := listSecrets(vaultName)

	fmt.Print("Secrets:\n\n")

	var wg sync.WaitGroup
	wg.Add(len(listingEntries))

	for i := 0; i < len(listingEntries); i++ {
		go func(i int) {
			defer wg.Done()
			entry := listingEntries[i]
			printSecret(vaultName, entry.Id)
    }(i)

	}

	wg.Wait()
}
