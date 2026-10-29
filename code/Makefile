GRADLE ?= ./gradlew
GS_JSON = app/google-services.json

.PHONY: all build
all: build

$(GS_JSON):
	@test -n "$$GOOGLE_SERVICES_B64" || { echo "GOOGLE_SERVICES_B64 is not set"; exit 1; }
	@mkdir -p $(dir $@)
	@printf '%s' "$$GOOGLE_SERVICES_B64" | base64 -d > $@

build: $(GS_JSON)
	$(GRADLE) build --configuration-cache
