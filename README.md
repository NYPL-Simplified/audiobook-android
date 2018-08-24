audiobook-android
===

![Travis (.org)](https://img.shields.io/travis/NYPL-Simplified/audiobook-android.svg?style=flat-square)

### Compilation

```
$ ./gradlew clean assembleDebug test publishToMavenLocal
```

### Project Structure

The project is divided into separate modules. Programmers wishing to use the API will primarily be
concerned with the [Core API](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.nypl.audiobook.android.api),
but will also need to add [providers](#providers) to the classpaths of their projects in order
to actually do useful work. The API is designed to make it easy to develop an event-driven user
interface, but this project also includes a ready-made [player UI](#player_ui) that can be embedded
into applications. Additionally, audio engine providers that do not, by themselves, handle downloads
require callers to provide a _download provider_. Normally, this code would be provided directly
by applications (as applications tend to have centralized code to handle downloads), but a
[simple implementation](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.nypl.audiobook.android.downloads)
is available to ease integration.

|Module|Description|
|------|-----------|
| [org.nypl.audiobook.android.api](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.nypl.audiobook.android.api) | Core API
| [org.nypl.audiobook.android.downloads](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.nypl.audiobook.android.downloads) | A generic download provider for non-encrypted audio books
| [org.nypl.audiobook.android.manifest.nypl](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.nypl.audiobook.android.manifest.nypl) | NYPL manifest parser
| [org.nypl.audiobook.android.open_access](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.nypl.audiobook.android.open_access) | ExoPlayer-based audio player provider for non-encrypted audio books
| [org.nypl.audiobook.android.tests](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.nypl.audiobook.android.tests) | Unit tests that can execute without needing a real or emulated device
| [org.nypl.audiobook.android.tests.device](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.nypl.audiobook.android.tests.device) | Unit tests that execute on real or emulated devices
| [org.nypl.audiobook.android.views](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.nypl.audiobook.android.views) | UI components

### Usage

1. Download (or synthesize) an [audio book manifest](#manifest_parsers). [Hadrien Gardeur](https://github.com/HadrienGardeur/audiobook-manifest/) publishes many example manifests in formats supported by the API.
2. Ask the API to [parse the manifest](#using_manifest_parsers).
3. Ask the API to [create an audio engine](#using_audio_engines) from the parsed manifest.
4. Make calls to the resulting [audio book](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.nypl.audiobook.android.api/src/main/java/org/nypl/audiobook/android/api/PlayerAudioBookType.kt) to download and play individual parts of the book.

See the provided [example project](https://github.com/NYPL-Simplified/audiobook-demo-android) for a
complete example that is capable of downloading and playing audio books.

### Dependencies

At a minimum, applications will need the Core API, one or more [manifest parser](#manifest_parsers)
implementations, and one or more [audio engine](#audio_engines) implementations. Use the following
Gradle dependencies to get a manifest parser that can parse the NYPL manifest format, and an audio
engine that can play non-encrypted audio books:

```
ext {
  nypl_audiobook_api_version = "0.0.29"
}

dependencies {
  implementation "org.nypl.audiobook:org.nypl.audiobook.android.manifest.nypl:${nypl_audiobook_api_version}"
  implementation "org.nypl.audiobook:org.nypl.audiobook.android.api:${nypl_audiobook_api_version}"
  implementation "org.nypl.audiobook:org.nypl.audiobook.android.open_access:${nypl_audiobook_api_version}"
}
```

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

Programmers should make calls to the [PlayerManifests](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.nypl.audiobook.android.api/src/main/java/org/nypl/audiobook/android/api/PlayerManifests.kt)
class, passing in an input stream representing the raw bytes of a manifest. The methods return a
`PlayerResult` value providing either the parsed manifest or an exception indicating why parsing
failed. The `PlayerManifests` class asks each registered [manifest parser](#creating_manifest_parsers)
whether or not it can parse the given raw data and picks the first one that claims that it can.
Programmers are not intended to have to use instances of the [PlayerManifestParserType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.nypl.audiobook.android.api/src/main/java/org/nypl/audiobook/android/api/PlayerManifestParserType.kt)
directly.

#### Creating Manifest Parsers <a id="creating_manifest_parsers"/>

Programmers will generally not need to create new manifest parsers, but will instead use one or
more of the [provided implementations](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.nypl.audiobook.android.manifest.nypl).
However, applications needing to use a new and unsupported manifest format will need to
provide and register new manifest parser implementations.

In order to add a new manifest parser, it's necessary to define a new class that implements
the [PlayerManifestParserType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.nypl.audiobook.android.api/src/main/java/org/nypl/audiobook/android/api/PlayerManifestParserType.kt)
and defines a public, no-argument constructor. It's then necessary to register this class so that
`ServiceLoader` can find it by creating a resource file at
`META-INF/services/org.nypl.audiobook.android.api.PlayerManifestParserType` containing the fully
qualified name of the new class. The standard [PlayerManifestParserNYPL](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.nypl.audiobook.android.manifest.nypl/src/main/java/org/nypl/audiobook/android/manifest/nypl/PlayerManifestParserNYPL.kt)
class and its associated [service file](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.nypl.audiobook.android.manifest.nypl/src/main/resources/META-INF/services/org.nypl.audiobook.android.api.PlayerManifestParserType)
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

### Audio Engines <a id="audio_engines"/>

#### Overview

An _audio engine_ is a component that actually downloads and plays a given audio book.

#### Using Audio Engines <a id="using_audio_engines"/>

Given a parsed [manifest](#using_manifest_parsers), programmers should make calls to the methods
defined on the [PlayerAudioEngines](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.nypl.audiobook.android.api/src/main/java/org/nypl/audiobook/android/api/PlayerAudioEngines.kt)
class. Similarly to the `PlayerManifests` class, the `PlayerAudioEngines` class will ask each
registered [audio engine implementation](#creating_audio_engines) in turn if it is capable of
supporting the book described by the given manifest. Please consult the documentation for that
class for information on how to filter and/or prefer particular implementations. The
(somewhat arbitrary) default behaviour is to select all implementations that claim to be able to
support the given book, and then select the implementation that advertises the highest version number.

#### Creating Audio Engines <a id="creating_audio_engines"/>

Implementations must implement the [PlayerAudioEngineProviderType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.nypl.audiobook.android.api/src/main/java/org/nypl/audiobook/android/api/PlayerAudioEngineProviderType.kt)
interface and register themselves in the same manner as [manifest parsers](#creating_manifest_parsers).

Creating a new audio engine provider is a fairly involved process. The provided
[ExoPlayer-based implementation](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.nypl.audiobook.android.open_access/src/main/java/org/nypl/audiobook/android/open_access/ExoEngineProvider.kt)
may serve as an example for new implementations.

In order to reduce duplication of code between audio engines, the downloading of books is
abstracted out into a [PlayerDownloadProviderType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.nypl.audiobook.android.api/src/main/java/org/nypl/audiobook/android/api/PlayerDownloadProviderType.kt)
interface that audio engine implementations can call in order to perform the work of actually
downloading books. Implementations of this interface are actually provided by the calling programmer
as this kind of code is generally provided by the application using the audio engine.

### Player UI <a id="player_ui"/>

#### Overview

The API comes with a set of Android views and fragments that can be embedded into an application
to provide a simple user interface for the player API.

#### Using the UI

1. Declare an `Activity` that implements the [PlayerFragmentListenerType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.nypl.audiobook.android.views/src/main/java/org/nypl/audiobook/android/views/PlayerFragmentListenerType.kt).
2. Load a `PlayerFragment` instance into the activity.

Please consult the provided [example project](https://github.com/NYPL-Simplified/audiobook-demo-android)
and the documentation comments on the `PlayerFragmentListenerType` for details.
