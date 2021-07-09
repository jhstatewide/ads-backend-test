@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package com.statewidesoftware

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import jdk.internal.org.xml.sax.SAXException
import org.json.JSONObject
import org.json.XML
import java.io.IOException
import java.io.StringReader
import java.net.URL
import java.nio.file.Files
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator
import kotlin.io.path.*
import kotlin.system.exitProcess


enum class Operation {
    CONVERT_TO_JSON,
    CONVERT_TO_XML,
    VALIDATE_XML
}

enum class InputType {
    JSON, XML
}

data class AddressInput(val inputType: InputType, val content: String)

class AddressBookUtility : CliktCommand(help="""
    This utility performs various operations on an address book XML or JSON file/URL.
    ```
    Pass --input for input source (can be local path or URL)
    Pass --output for desired output path
    
    Pass --operation MODE for desired program mode.
        convert-to-json -- takes XML as input and emits JSON
        convert-to-xml -- takes JSON as input and emits XML
        validate-xml -- validates XML input against the address book schema
    ```
""".trimIndent()) {
    private val input by option("--input", help="Location of input. It can be a local file OR a URL.").required()
    private val output: String? by option(help="Output filename")
    private val operation by option().choice(
        "convert-to-json" to Operation.CONVERT_TO_JSON,
        "convert-to-xml" to Operation.CONVERT_TO_XML,
        "validate-xml" to Operation.VALIDATE_XML
    ).required()

    private fun isURL(input: String): Boolean {
        try {
            URL(input)
        } catch(e: Exception) {
            return false
        }
        return true
    }

    override fun run() {
        val addressInput: AddressInput = readInput()

        if (operation != Operation.VALIDATE_XML && output == null) {
            echo("For conversion options, you MUST supply --output!")
            exitProcess(-1)
        }

        if (output != null) {
            handleConversion(addressInput)
        }

        if (operation == Operation.VALIDATE_XML) {
            handleValidation(addressInput)
        }

        throw UsageError("Cannot figure out what you want to do!")
    }

    private fun handleValidation(addressInput: AddressInput) {
        try {
            val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            val schema: Schema =
                factory.newSchema(this::class.java.getResource("/address_book_schema.xsd").toURI().toURL())
            val validator: Validator = schema.newValidator()
            validator.validate(StreamSource(StringReader(addressInput.content)))
        } catch (e: IOException) {
            echo("Exception validating XML: " + e.message)
            exitProcess(-1)
        } catch (e: SAXException) {
            echo("Exception validating XML: " + e.message)
            exitProcess(-1)
        }
        echo("XML is valid!")
        exitProcess(1)
    }

    private fun handleConversion(addressInput: AddressInput) {
        val outputPath = Path(output!!).toAbsolutePath()
        if (!outputPath.parent.isWritable()) {
            echo("Output path $outputPath is not writable!")
            exitProcess(-1)
        }

        // if the input and output types are the same... this is really easy!
        if (
            addressInput.inputType == InputType.XML && operation == Operation.CONVERT_TO_XML ||
            addressInput.inputType == InputType.JSON && operation == Operation.CONVERT_TO_JSON
        ) {
            outputPath.writeText(addressInput.content)
            exitProcess(1)
        }

        if (addressInput.inputType == InputType.XML && operation == Operation.CONVERT_TO_JSON) {
            val xmlJSONObj: JSONObject = XML.toJSONObject(addressInput.content)
            val jsonOutput = xmlJSONObj.toString(4)
            outputPath.writeText(jsonOutput)
            exitProcess(1)
        }

        if (addressInput.inputType == InputType.JSON && operation == Operation.CONVERT_TO_XML) {
            val json = JSONObject(addressInput.content)
            outputPath.writeText(XML.toString(json))
            exitProcess(1)
        }
    }

    private fun readInput() = if (isURL(input)) {
        val url = URL(input)
        val urlConnection = url.openConnection()
        when (val mimeType = urlConnection.contentType) {
            "application/json" -> {
                AddressInput(InputType.JSON, url.readText())
            }
            "application/xml" -> {
                AddressInput(InputType.XML, url.readText())
            }
            else -> {
                echo("Input file was mime type $mimeType! Must be one of application/json or application/xml!")
                exitProcess(-1)
            }
        }
    } else {
        val inputPath = Path(input)
        if (!inputPath.isReadable()) {
            echo("$inputPath does not exist!")
            exitProcess(-1)
        }
        when (val type = Files.probeContentType(inputPath)) {
            "json" -> {
                AddressInput(InputType.JSON, inputPath.readText())
            }
            "xml" -> {
                AddressInput(InputType.XML, inputPath.readText())
            }
            else -> {
                echo("Input file was type $type! Must be one of json or xml!")
                exitProcess(-1)
            }
        }
    }
}

fun main(args: Array<String>) = AddressBookUtility().main(args)