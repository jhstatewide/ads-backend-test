# ADS Backend Java Test Address Converter

## Description

This program can apply a few operations to an XML file similar to http://www.bindows.net/documentation/download/ab.xml to:

* Convert it to JSON
* If it's JSON, convert to XML
* Validate the XML

## Build Instructions

### Requirements
* JDK 11+

### Process

```shell
$ ./gradlew shadowJar
```

Output "uberjar" will be placed in ./build/libs/address-book-utility.jar.

Test execution with:

```shell
$ java -jar build/libs/address-book-utility.jar --help
```

## Usage

```
address-book-utility [OPTIONS]

This utility performs various operations on an address book XML or JSON file/URL.

Pass --input for input source (can be local path or URL)
Pass --output for desired output path

Pass --operation MODE for desired program mode.
convert-to-json -- takes XML as input and emits JSON
convert-to-xml -- takes JSON as input and emits XML
validate-xml -- validates XML input against the address book schema

Options:
--input TEXT                     Location of input. It can be a local file
OR a URL.
--output TEXT                    Output filename
--operation [convert-to-json|convert-to-xml|validate-xml]
-h, --help                       Show this message and exit

```