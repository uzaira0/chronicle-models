package com.openlattice.chronicle.fixtures

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

/**
 * Parsed, validated view of `fixtures/payloads/registry.json` (shared-contracts
 * Tranche 3). Validation lives here — factored out of the test methods — so the
 * same rules are unit-testable against deliberately malformed registry JSON. The
 * Python generator (`scripts/generate-domain-contracts.py`) enforces the same
 * rules before embedding the registry into the generated contract artifact.
 */
public data class FixtureFamily(
    val family: String,
    val collectionModuleId: String?,
    val payloadSchemaVersion: Int,
    val jvmClass: String?,
    val fixtureFiles: List<String>,
    val timeSemantics: String,
    val scopingFields: List<String>,
    val backendHandler: String,
    val backendTable: String,
    val notes: String?,
)

public data class FixtureRegistry(
    val schemaVersion: String,
    val families: List<FixtureFamily>,
) {
    public companion object {
        public const val SCHEMA_VERSION: String = "chronicle-fixture-registry/v1"
        private val KEBAB_CASE = Regex("^[a-z][a-z0-9]*(-[a-z0-9]+)*$")
        private val mapper = ObjectMapper()

        /**
         * Parses and validates registry JSON. [knownModuleIds] is the wire-id set
         * from `CollectionModuleId`; a non-null `collectionModuleId` must be in it.
         * Throws [IllegalArgumentException] on any malformed entry.
         */
        public fun parse(json: String, knownModuleIds: Set<String>): FixtureRegistry {
            val root = mapper.readTree(json)
            require(root.isObject) { "Fixture registry root must be a JSON object" }
            val schemaVersion = root.path("schemaVersion").asText("")
            require(schemaVersion == SCHEMA_VERSION) {
                "Fixture registry schemaVersion must be '$SCHEMA_VERSION': '$schemaVersion'"
            }
            val familiesNode = root.path("families")
            require(familiesNode.isArray && familiesNode.size() > 0) {
                "Fixture registry must declare a non-empty 'families' array"
            }
            val families = familiesNode.map { parseFamily(it, knownModuleIds) }
            val names = families.map { it.family }
            require(names.size == names.toSet().size) {
                "Fixture registry family names must be unique: ${names.groupBy { it }.filterValues { it.size > 1 }.keys}"
            }
            return FixtureRegistry(schemaVersion, families)
        }

        /** [parse] plus on-disk checks: every fixtureFile must exist under [repoRoot]. */
        public fun load(repoRoot: File, knownModuleIds: Set<String>): FixtureRegistry {
            val registryFile = File(repoRoot, "fixtures/payloads/registry.json")
            require(registryFile.isFile) { "Missing fixture registry: $registryFile" }
            val registry = parse(registryFile.readText(), knownModuleIds)
            registry.families.forEach { family ->
                family.fixtureFiles.forEach { path ->
                    require(File(repoRoot, path).isFile) {
                        "Fixture file for family '${family.family}' does not exist: $path"
                    }
                }
            }
            return registry
        }

        private fun parseFamily(node: JsonNode, knownModuleIds: Set<String>): FixtureFamily {
            require(node.isObject) { "Fixture family entry must be a JSON object: $node" }
            val family = node.path("family").asText("")
            require(KEBAB_CASE.matches(family)) { "Fixture family name must be kebab-case: '$family'" }

            val moduleNode = node.path("collectionModuleId")
            require(!moduleNode.isMissingNode) { "Family '$family' must declare collectionModuleId (id or null)" }
            val moduleId = if (moduleNode.isNull) null else moduleNode.asText()
            if (moduleId != null) {
                require(moduleId in knownModuleIds) {
                    "Family '$family' references unknown CollectionModuleId: '$moduleId'"
                }
            }

            val versionNode = node.path("payloadSchemaVersion")
            require(versionNode.isInt && versionNode.asInt() >= 1) {
                "Family '$family' payloadSchemaVersion must be an integer >= 1: $versionNode"
            }

            val jvmClassNode = node.path("jvmClass")
            require(!jvmClassNode.isMissingNode) { "Family '$family' must declare jvmClass (FQCN or null)" }
            val jvmClass = if (jvmClassNode.isNull) null else jvmClassNode.asText()
            if (jvmClass != null) {
                require(jvmClass.isNotBlank()) { "Family '$family' jvmClass must not be blank" }
            }

            val filesNode = node.path("fixtureFiles")
            require(filesNode.isArray && filesNode.size() > 0) {
                "Family '$family' must declare a non-empty fixtureFiles array"
            }
            val files = filesNode.map { it.asText() }
            files.forEach { path ->
                require(path.startsWith("fixtures/payloads/$family/")) {
                    "Family '$family' fixture file must live under fixtures/payloads/$family/: '$path'"
                }
            }

            val timeSemantics = node.path("timeSemantics").asText("")
            require(timeSemantics.isNotBlank()) { "Family '$family' must document timeSemantics" }

            val scopingNode = node.path("scopingFields")
            require(scopingNode.isArray && scopingNode.size() > 0) {
                "Family '$family' must declare a non-empty scopingFields array"
            }

            val backendHandler = node.path("backendHandler").asText("")
            require(backendHandler.isNotBlank()) { "Family '$family' must name a backendHandler" }
            val backendTable = node.path("backendTable").asText("")
            require(backendTable.isNotBlank()) { "Family '$family' must name a backendTable" }

            return FixtureFamily(
                family = family,
                collectionModuleId = moduleId,
                payloadSchemaVersion = versionNode.asInt(),
                jvmClass = jvmClass,
                fixtureFiles = files,
                timeSemantics = timeSemantics,
                scopingFields = scopingNode.map { it.asText() },
                backendHandler = backendHandler,
                backendTable = backendTable,
                notes = node.path("notes").takeIf { it.isTextual }?.asText(),
            )
        }
    }
}
