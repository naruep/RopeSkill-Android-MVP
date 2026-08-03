package com.ropeskill.app

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BackupBoundaryTest {
    @Test
    fun manifest_disablesBackupAndReferencesExplicitRules() {
        val application =
            parseXml(sourceFile("src/main/AndroidManifest.xml"))
                .getElementsByTagName("application")
                .item(0)
                .attributes

        assertEquals("false", application.getNamedItem("android:allowBackup").nodeValue)
        assertEquals(
            "@xml/backup_rules",
            application.getNamedItem("android:fullBackupContent").nodeValue,
        )
        assertEquals(
            "@xml/data_extraction_rules",
            application.getNamedItem("android:dataExtractionRules").nodeValue,
        )
    }

    @Test
    fun legacyRules_excludeEverySupportedStorageDomain() {
        val rules = parseXml(sourceFile("src/main/res/xml/backup_rules.xml"))

        assertAllDomainsExcluded(rules.getElementsByTagName("exclude"))
    }

    @Test
    fun android12Rules_excludeEveryDomainFromCloudAndDeviceTransfer() {
        val rules = parseXml(sourceFile("src/main/res/xml/data_extraction_rules.xml"))
        val expectedSections = listOf("cloud-backup", "device-transfer")

        expectedSections.forEach { sectionName ->
            val section = rules.getElementsByTagName(sectionName).item(0)
            assertNotNull("$sectionName must be present", section)
            assertAllDomainsExcluded(section.childNodes)
        }
    }

    private fun assertAllDomainsExcluded(nodes: org.w3c.dom.NodeList) {
        val actual =
            buildSet {
                for (index in 0 until nodes.length) {
                    val node = nodes.item(index)
                    if (node.nodeName == "exclude") {
                        assertEquals(".", node.attributes.getNamedItem("path").nodeValue)
                        add(node.attributes.getNamedItem("domain").nodeValue)
                    }
                }
            }

        assertEquals(STORAGE_DOMAINS, actual)
    }

    private fun sourceFile(relativePath: String): File {
        val candidates =
            listOf(
                File(relativePath),
                File("app", relativePath),
            )

        return candidates.firstOrNull(File::isFile)
            ?: error("Cannot find source file $relativePath from ${File(".").absolutePath}")
    }

    private fun parseXml(file: File) =
        DocumentBuilderFactory
            .newInstance()
            .apply {
                isNamespaceAware = false
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }.newDocumentBuilder()
            .parse(file)

    private companion object {
        val STORAGE_DOMAINS =
            setOf(
                "root",
                "file",
                "database",
                "sharedpref",
                "external",
                "device_root",
                "device_file",
                "device_database",
                "device_sharedpref",
            )
    }
}
