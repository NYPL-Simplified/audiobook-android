audiobook-android
===

[![Build Status](https://img.shields.io/github/workflow/status/NYPL-Simplified/audiobook-android/Android%20CI%20(Authenticated)?style=flat-square)](https://github.com/NYPL-Simplified/audiobook-android/actions?query=workflow%3A%22Android+CI+%28Authenticated%29%22)
[![Maven Central](https://img.shields.io/maven-central/v/org.librarysimplified.audiobook/org.librarysimplified.audiobook.api.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.librarysimplified.audiobook%22)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/https/oss.sonatype.org/org.librarysimplified.audiobook/org.librarysimplified.audiobook.api.svg?style=flat-square)](https://oss.sonatype.org/content/repositories/snapshots/org.librarysimplified.audiobook/)

### Compilation

Make sure you clone this repository with `git clone --recursive`. 
If you forgot to use `--recursive`, then execute:

```
$ git submodule init
$ git submodule update --remote --recursive
```

```
$ echo "org.gradle.internal.publish.checksums.insecure=true" >> "$HOME/.gradle/gradle.properties"

$ ./gradlew clean assembleDebug test publishToMavenLocal
```

#### Insecure checksums?

Astute readers may have noticed the `org.gradle.internal.publish.checksums.insecure` property
in the initial build instructions. This is necessary because Gradle 6 currently publishes
checksums that [Maven Central doesn't like](https://github.com/gradle/gradle/issues/11308#issuecomment-554317655).
Until Maven Central is updated to accept SHA256 and SHA512 checksums, this flag is necessary.
As all artifacts published to Maven Central are PGP signed, this is not a serious issue; PGP
signatures combine integrity checking and authentication, so checksum files are essentially
redundant nowadays.

### Project Structure

The project is divided into separate modules. Programmers wishing to use the API will primarily be
concerned with the [Core API](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.api)
and the [Player API](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.player.api),
but will also need to add [providers](#providers) to the classpaths of their projects in order
to actually do useful work.

|Module|Description|
|------|-----------|
|[org.librarysimplified.audiobook.api](org.librarysimplified.audiobook.api)|AudioBook API (API specification)|
|[org.librarysimplified.audiobook.feedbooks](org.librarysimplified.audiobook.feedbooks)|AudioBook API (Feedbooks-specific functionality)|
|[org.librarysimplified.audiobook.json_canon](org.librarysimplified.audiobook.json_canon)|AudioBook API (JSON canonicalization functionality)|
|[org.librarysimplified.audiobook.json_web_token](org.librarysimplified.audiobook.json_web_token)|AudioBook API (JSON web token functionality)|
|[org.librarysimplified.audiobook.lcp.license_status](org.librarysimplified.audiobook.lcp.license_status)|AudioBook API (LCP License Status document support)|
|[org.librarysimplified.audiobook.license_check.api](org.librarysimplified.audiobook.license_check.api)|AudioBook API (License check API)|
|[org.librarysimplified.audiobook.license_check.spi](org.librarysimplified.audiobook.license_check.spi)|AudioBook API (License check SPI)|
|[org.librarysimplified.audiobook.manifest.api](org.librarysimplified.audiobook.manifest.api)|AudioBook API (Manifest types)|
|[org.librarysimplified.audiobook.manifest_parser.api](org.librarysimplified.audiobook.manifest_parser.api)|AudioBook API (Manifest parser API)|
|[org.librarysimplified.audiobook.manifest_parser.extension_spi](org.librarysimplified.audiobook.manifest_parser.extension_spi)|AudioBook API (Manifest parser extension SPI)|
|[org.librarysimplified.audiobook.manifest_parser.webpub](org.librarysimplified.audiobook.manifest_parser.webpub)|AudioBook API (Readium WebPub manifest parser)|
|[org.librarysimplified.audiobook.exoplayer](org.librarysimplified.audiobook.open_access)|AudioBook API (Exoplayer engine implementation)|
|[org.librarysimplified.audiobook.player.api](org.librarysimplified.audiobook.player.api)|AudioBook API (Player API)|
|[org.librarysimplified.audiobook.parser.api](org.librarysimplified.audiobook.parser.api)|AudioBook API (Parser API)|
|[org.librarysimplified.audiobook.tests](org.librarysimplified.audiobook.tests)|AudioBook API (Test suite)|

### Changelog

The project currently uses [com.io7m.changelog](https://www.io7m.com/software/changelog/)
to manage release changelogs.

### Usage

1. Download (or synthesize) an [audio book manifest](#manifest_parsers). [Hadrien Gardeur](https://github.com/HadrienGardeur/audiobook-manifest/) publishes many example manifests in formats supported by the API.
2. Ask the API to [parse the manifest](#using_manifest_parsers).
3. (Optional) Ask the API to [perform license checks](#license_checking).
5. Ask the API to [create a PlayerFactory](#using_audio_engines) from the parsed manifest.
6. Make calls to the resulting [player factory](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.player.api/src/main/java/org/librarysimplified/audiobook/player/api/PlayerFactoryType.kt) to create a
[media2 SessionPlayer](https://developer.android.com/reference/androidx/media2/common/SessionPlayer).

### Dependencies

At a minimum, applications will need the Core API, one or more [manifest parser](#manifest_parsers)
implementations, and one or more [audio engine provider](#audio_engines) implementations. Use the following
Gradle dependencies to get a manifest parser that can parse the Readium WebPub manifest format, and 
an audio engine that can play non-encrypted audio books:

```
ext {
  nypl_audiobook_api_version = "8.0.0"
}

dependencies {
  implementation "org.librarysimplified.audiobook:org.librarysimplified.audiobook.manifest_parser.webpub:${nypl_audiobook_api_version}"
  implementation "org.librarysimplified.audiobook:org.librarysimplified.audiobook.api:${nypl_audiobook_api_version}"
  implementation "org.librarysimplified.audiobook:org.librarysimplified.audiobook.player.api:${nypl_audiobook_api_version}"
  implementation "org.librarysimplified.audiobook:org.librarysimplified.audiobook.exoplayer:${nypl_audiobook_api_version}"
}
```

### Versioning

The API is expected to follow [semantic versioning](https://semver.org/).

### Providers

The API uses a _service provider_ model in order to provide strong _modularity_ and to decouple
consumers of the API from specific implementations of the API. To this end, the API uses
[ServiceLoader](https://docs.oracle.com/javase/10/docs/api/java/util/ServiceLoader.html)
internally in order to allow new implementations of both [manifest parsers](#manifest_parsers) and
[audio engines](#audio_engines) to be registered and made available to client applications without
requiring any changes to the application code.

### Manifest Parsers <a id="manifest_parsers"/>

#### Overview

An audio book is typically delivered to the client via a _manifest_. A manifest is normally a
JSON description of the audio book that includes links to audio files, and other metadata. It is the
responsibility of a _manifest parser_ to turn a JSON AST into a typed manifest data structure
defined in the Core API.

#### Using Manifest Parsers <a id="using_manifest_parsers"/>

Programmers should make calls to the [ManifestParsers](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.manifest_parser.api/src/main/java/org/librarysimplified/audiobook/manifest_parser/api/ManifestParsers.kt)
class, passing in a byte array representing (typically) the raw text of a JSON manifest. The methods return a
`PlayerResult` value providing either the parsed manifest or a list of errors indicating why parsing
failed. The `ManifestParsers` class asks each registered [manifest parser](#creating_manifest_parsers)
whether or not it can parse the given raw data and picks the first one that claims that it can.
Programmers are not intended to have to use instances of the [PlayerManifestParserType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.manifest_parser.api/src/main/java/org/librarysimplified/audiobook/manifest_parser/api/ManifestParserType.kt)
directly.

#### Creating Manifest Parsers <a id="creating_manifest_parsers"/>

Programmers will generally not need to create new manifest parsers, but will instead use one or
more of the [provided implementations](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.manifest_parser.webpub).
However, applications needing to use a new and unsupported manifest format will need to
provide and register new manifest parser implementations.

In order to add a new manifest parser, it's necessary to define a new class that implements
the [PlayerManifestParserType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.manifest_parser.api/src/main/java/org/librarysimplified/audiobook/manifest_parser/api/ManifestParserProviderType.kt)
and defines a public, no-argument constructor. It's then necessary to register this class so that
`ServiceLoader` can find it by creating a resource file at
`META-INF/services/org.librarysimplified.audiobook.manifest_parser.api.ManifestParserProviderType` containing the fully
qualified name of the new class. The standard [WebPubParserProvider](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.manifest_parser.webpub/src/main/java/org/librarysimplified/audiobook/manifest_parser/webpub/WebPubParserProvider.kt)
class and its associated [service file](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.manifest_parser.webpub/src/main/resources/META-INF/services/org.librarysimplified.audiobook.manifest_parser.api.ManifestParserProviderType)
serve as minimal examples for new parser implementations. When a `jar` (or `aar`) file is placed on
the classpath containing both the class and the service file, `ServiceLoader` will find the
implementation automatically when the user asks for parser implementations.

Parsers are responsible for examining the given JSON AST and telling the caller whether or not they
think that they are capable of parsing the AST into a useful structure. For example,
[audio engine providers](#audio_engines) that require DRM might check the AST to see if the
required DRM metadata structures are present. The Core API will ask each parser implementation in
turn if the implementation can parse the given JSON, and the first implementation to respond in the
affirmative will be used. Implementations should take care to be honest; an implementation that
always claimed to be able to parse the given JSON would prevent other (possibly more suitable)
implementations from being considered.

### License Checking <a id="license_checking"/>

The API allows for opt-in _license checking_. Once a manifest has been
parsed, programmers can execute license checks on the manifest to verify
if the listening party actually has permission to hear the given audio
book.

Individual license checks are provided as implementations of the
[SingleLicenseCheckProviderType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.license_check.spi/src/main/java/org/librarysimplified/audiobook/license_check/spi/SingleLicenseCheckProviderType.kt)
type. Programmers should pass in a list of desired single license check providers
to the [LicenseChecks](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.license_check.api/src/main/java/org/librarysimplified/audiobook/license_check/api/LicenseChecks.kt)
API for execution. The `LicenseChecks` API returns a list of the results
of license checks, and provides a simple `true/false` value indicating
whether or not playing should be permitted.

### Audio Engines <a id="audio_engines"/>

#### Overview

An _audio engine_ is a component that actually downloads and plays a given audio book.

#### Using Audio Engines <a id="using_audio_engines"/>

Given a parsed [manifest](#using_manifest_parsers), programmers should make calls to the methods
defined on the [PlayerAudioEngines](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.api/src/main/java/org/librarysimplified/audiobook/api/PlayerAudioEngines.kt)
class. Similarly to the `PlayerManifests` class, the `PlayerAudioEngines` class will ask each
registered [audio engine implementation](#creating_audio_engines) in turn if it is capable of
supporting the book described by the given manifest. Please consult the documentation for that
class for information on how to filter and/or prefer particular implementations. The
(somewhat arbitrary) default behaviour is to select all implementations that claim to be able to
support the given book, and then select the implementation that advertises the highest version number.

#### Creating Audio Engines <a id="creating_audio_engines"/>

Implementations must implement the [PlayerAudioEngineProviderType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.api/src/main/java/org/librarysimplified/audiobook/api/PlayerAudioEngineProviderType.kt)
interface and register themselves in the same manner as [manifest parsers](#creating_manifest_parsers).

Creating a new audio engine provider is a fairly involved process. The provided
[ExoPlayer-based implementation](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.exoplayer/src/main/java/org/librarysimplified/audiobook/exoplayer/ExoPlayerEngineProvider.kt)
may serve as an example for new implementations.

In order to reduce duplication of code between audio engines, the downloading of books is
abstracted out into a [PlayerDownloadProviderType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.api/src/main/java/org/librarysimplified/audiobook/api/PlayerDownloadProviderType.kt)
interface that audio engine implementations can call in order to perform the work of actually
downloading books. Implementations of this interface are actually provided by the calling programmer
as this kind of code is generally provided by the application using the audio engine.

### Testing <a id="testing"/>

#### Overview

The project contains numerous unit tests, many of which are designed
to run _both_ locally and on real or emulated devices. The reason for
this is that, during development, it's desirable to be able to run the
tests locally to quickly experiment with changes; running the entire
suite on the local machine takes just a few seconds. However, prior
to deployment, it's both desirable and necessary to run those same
tests on a real device in order to shake out platform-specific bugs.
Running tests on a real device is slow; it typically takes minutes
to run the entire test suite and it would therefore make development
rather painful if this was the only way to run the tests.

In order to implement this, the project implements tests that must
run locally *and* on devices as abstract classes ("contracts")
in `src/main/java` in the `org.librarysimplified.audiobook.tests`
module. It then defines a set of classes that extend
the abstract test classes in `src/test/java` in the
`org.librarysimplified.audiobook.tests` module. These classes
will run the tests locally.
