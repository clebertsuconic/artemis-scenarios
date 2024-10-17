#
### make variables
SHELL                         = /usr/bin/env bash
.SHELLFLAGS                   = -o pipefail -e -c

# project variables
MK_FILE_DIR                   = $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
BIN_DIR                       = $(MK_FILE_DIR)/bin
MANIFESTS_DIR                 = $(MK_FILE_DIR)/manifests

### Commands variables
MVN_DEFAULT_CMD               = mvn --no-transfer-progress --update-snapshots

all: help

.PHONY: clean
clean: ## clean project.
	@echo ""
	@echo "# Running $(@) #"
	@echo ""
	@${MVN_DEFAULT_CMD} clean
	@echo ""

.PHONY: display_mvn_property_updates
display_mvn_property_updates: ## Display dependencies and plugins updates.
	@echo ""
	@echo "# Running $(@) #"
	@echo ""
	@${MVN_DEFAULT_CMD} versions:display-property-updates
	@echo ""

.PHONY: dev
dev: ## Start quarkus dev environment
	@echo ""
	@echo "# Running $(@) #"
	@echo ""
	@${MVN_DEFAULT_CMD} quarkus:dev
	@echo ""

############################
### Additional makefiles ###
############################
include deployments/deployment.mk

####################
### Help targets ###
####################
.PHONY: help
help: ## Display this help.
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage: make \033[36m<target>\033[0m\n\n"} \
		/^[a-zA-Z_0-9-]+:.*?##/ { printf "  \033[36m%-38s\033[0m %s\n", $$1, $$2 } \
		/^##@/ { printf "\n\033[0m%s\033[0m\n", substr($$0, 5) } ' \
		$(MAKEFILE_LIST)
