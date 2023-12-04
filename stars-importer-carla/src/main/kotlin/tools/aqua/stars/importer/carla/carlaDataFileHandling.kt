/*
 * Copyright 2023-2024 The STARS Project Authors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")

package tools.aqua.stars.importer.carla

import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import tools.aqua.stars.core.DEFAULT_MIN_SEGMENT_TICK_COUNT
import tools.aqua.stars.core.DEFAULT_SIMULATION_RUN_PREFETCH_SIZE
import tools.aqua.stars.data.av.dataclasses.Block
import tools.aqua.stars.data.av.dataclasses.Segment
import tools.aqua.stars.importer.carla.dataclasses.*

/** Carla data serializer module. */
val carlaDataSerializerModule: SerializersModule = SerializersModule {
  polymorphic(JsonActor::class) {
    subclass(JsonVehicle::class)
    subclass(JsonTrafficLight::class)
    subclass(JsonTrafficSign::class)
    subclass(JsonPedestrian::class)
  }
}

/** Coroutine scope for simulation file IO. */
val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

/**
 * Returns the parsed Json content for the given [file]. Currently supported file extensions:
 * ".json", ".zip". The generic parameter [T] specifies the class to which the content should be
 * parsed to.
 *
 * @return The parsed JSON content from the given [Path].
 */
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> getJsonContentOfPath(file: Path): T {
  // Create JsonBuilder with correct settings
  val jsonBuilder = Json {
    prettyPrint = true
    isLenient = true
    serializersModule = carlaDataSerializerModule
  }

  // Check if inputFilePath exists
  check(file.exists()) { "The given file path does not exist: ${file.toUri()}" }

  // Check whether the given inputFilePath is a directory
  check(!file.isDirectory()) { "Cannot get InputStream for directory. Path: $file" }

  // If ".json"-file: Just return InputStream of file
  if (file.extension == "json") {
    return jsonBuilder.decodeFromStream<T>(file.inputStream())
  }

  // if ".zip"-file: Extract single archived file
  if (file.extension == "zip") {
    // https://stackoverflow.com/a/46644254
    ZipFile(File(file.toUri())).use { zip ->
      zip.entries().asSequence().forEach { entry ->
        // Add InputStream to inputStreamBuffer
        return jsonBuilder.decodeFromStream<T>(zip.getInputStream(entry))
      }
    }
  }

  // If none of the supported file extensions is present, throw an Exception
  error("Unexpected file extension: ${file.extension}. Supported extensions: '.json', '.zip'")
}

/**
 * Return a [Sequence] of [Block]s from a given [mapDataFile] path.
 *
 * @param mapDataFile The [Path] which points to the file that contains the static map data.
 * @return The loaded [Block]s as a [Sequence] based on the given [mapDataFile] [Path]
 */
fun loadBlocks(mapDataFile: Path): Sequence<Block> =
    calculateStaticBlocks(
            getJsonContentOfPath<List<JsonBlock>>(mapDataFile), mapDataFile.fileName.toString())
        .asSequence()

/**
 * Return a [Sequence] of [Segment]s given a path to a [mapDataFile] in combination with a
 * [dynamicDataFile] path. [useEveryVehicleAsEgo] lets you decide whether to use every vehicle to be
 * used as the ego vehicle. This will multiply the size of the resulting sequence of [Segment]s by
 * the number of vehicles.
 *
 * @param mapDataFile The [Path] to map data file containing all static information.
 * @param dynamicDataFile The [Path] to the data file which contains the timed state data for the
 * simulation.
 * @param useEveryVehicleAsEgo Whether the [Segment]s should be enriched by considering every
 * vehicle as the "ego" vehicle.
 * @param minSegmentTickCount The amount of ticks there should be at minimum to generate a [Segment]
 * .
 * @param orderFilesBySeed Whether the dynamic data files should be sorted by their seeds instead of
 * the map.
 * @param segmentPrefetchSize Size of the simulation run prefetch buffer.
 * @return A [Sequence] of [Segment]s based on the given [mapDataFile] and [dynamicDataFile]
 */
fun loadSegments(
    mapDataFile: Path,
    dynamicDataFile: Path,
    useEveryVehicleAsEgo: Boolean = false,
    minSegmentTickCount: Int = DEFAULT_MIN_SEGMENT_TICK_COUNT,
    orderFilesBySeed: Boolean = false,
    segmentPrefetchSize: Int = DEFAULT_SIMULATION_RUN_PREFETCH_SIZE,
): Sequence<Segment> =
    // Call actual implementation of loadSegments with correct data structure
    loadSegments(
        simulationRunsWrappers =
            listOf(CarlaSimulationRunsWrapper(mapDataFile, listOf(dynamicDataFile))),
        useEveryVehicleAsEgo = useEveryVehicleAsEgo,
        minSegmentTickCount = minSegmentTickCount,
        orderFilesBySeed = orderFilesBySeed,
        simulationRunPrefetchSize = segmentPrefetchSize)

/**
 * Load [Segment]s for one specific map. The map data comes from the file [mapDataFile] and the
 * dynamic data from multiple files [dynamicDataFiles].
 *
 * @param mapDataFile The [Path] to map data file containing all static information.
 * @param dynamicDataFiles A [List] of [Path]s to the data files which contain the timed state data
 * for the simulation.
 * @param useEveryVehicleAsEgo Whether the [Segment]s should be enriched by considering every
 * vehicle as the "ego" vehicle.
 * @param minSegmentTickCount The amount of ticks there should be at minimum to generate a [Segment]
 * .
 * @param orderFilesBySeed Whether the dynamic data files should be sorted by their seeds instead of
 * the map.
 * @param segmentPrefetchSize Size of the simulation run prefetch buffer.
 * @return A [Sequence] of [Segment]s based on the given [mapDataFile] and [dynamicDataFiles]
 */
fun loadSegments(
    mapDataFile: Path,
    dynamicDataFiles: List<Path>,
    useEveryVehicleAsEgo: Boolean = false,
    minSegmentTickCount: Int = DEFAULT_MIN_SEGMENT_TICK_COUNT,
    orderFilesBySeed: Boolean = false,
    segmentPrefetchSize: Int = DEFAULT_SIMULATION_RUN_PREFETCH_SIZE,
): Sequence<Segment> =
    // Call actual implementation of loadSegments with correct data structure
    loadSegments(
        simulationRunsWrappers = listOf(CarlaSimulationRunsWrapper(mapDataFile, dynamicDataFiles)),
        useEveryVehicleAsEgo = useEveryVehicleAsEgo,
        minSegmentTickCount = minSegmentTickCount,
        orderFilesBySeed = orderFilesBySeed,
        simulationRunPrefetchSize = segmentPrefetchSize)

/**
 * Load [Segment]s based on a [Map]. The [Map] needs to have the following structure. Map<[Path],
 * List[Path]> with the semantics: Map<MapDataPath, List<DynamicDataPath>>. As each dynamic data is
 * linked to a map data, they are linked in the [Map].
 *
 * @param mapToDynamicDataFiles Maps the [Path] of the static file to a [List] of [Path]s of dynamic
 * files related to the static file.
 * @param useEveryVehicleAsEgo Whether the [Segment]s should be enriched by considering every
 * vehicle as the "ego" vehicle.
 * @param minSegmentTickCount The amount of ticks there should be at minimum to generate a [Segment]
 * . .
 * @param orderFilesBySeed Whether the dynamic data files should be sorted by their seeds instead of
 * the map.
 * @param segmentPrefetchSize Size of the simulation run prefetch buffer.
 * @return A [Sequence] of [Segment]s based on the given 'mapDataFile' and 'dynamicDataFiles'.
 */
fun loadSegments(
    mapToDynamicDataFiles: Map<Path, List<Path>>,
    useEveryVehicleAsEgo: Boolean = false,
    minSegmentTickCount: Int = DEFAULT_MIN_SEGMENT_TICK_COUNT,
    orderFilesBySeed: Boolean = false,
    segmentPrefetchSize: Int = DEFAULT_SIMULATION_RUN_PREFETCH_SIZE,
): Sequence<Segment> =
    // Call actual implementation of loadSegments with correct data structure
    loadSegments(
        simulationRunsWrappers =
            mapToDynamicDataFiles.map { CarlaSimulationRunsWrapper(it.key, it.value) },
        useEveryVehicleAsEgo = useEveryVehicleAsEgo,
        minSegmentTickCount = minSegmentTickCount,
        orderFilesBySeed = orderFilesBySeed,
        simulationRunPrefetchSize = segmentPrefetchSize)

/**
 * Returns a [Sequence] of [Segment]s given a [List] of [CarlaSimulationRunsWrapper]s. Each
 * [CarlaSimulationRunsWrapper] contains the information about the used map data and the dynamic
 * data, each as [Path]s.
 *
 * @param simulationRunsWrappers The [List] of [CarlaSimulationRunsWrapper]s that wrap the map data
 * to its dynamic data
 * @param useEveryVehicleAsEgo Whether the [Segment]s should be enriched by considering every
 * vehicle as the "ego" vehicle
 * @param minSegmentTickCount The amount of ticks there should be at minimum to generate a [Segment]
 * @param orderFilesBySeed Whether the dynamic data files should be sorted by their seeds instead of
 * the map
 * @param simulationRunPrefetchSize Size of the simulation run prefetch buffer.
 * @return A [Sequence] of [Segment]s based on the given [simulationRunsWrappers]
 */
fun loadSegments(
    simulationRunsWrappers: List<CarlaSimulationRunsWrapper>,
    useEveryVehicleAsEgo: Boolean = false,
    minSegmentTickCount: Int = DEFAULT_MIN_SEGMENT_TICK_COUNT,
    orderFilesBySeed: Boolean = false,
    simulationRunPrefetchSize: Int = DEFAULT_SIMULATION_RUN_PREFETCH_SIZE,
): Sequence<Segment> {
  val simulationRunsWrapperList = loadBlocks(simulationRunsWrappers, orderFilesBySeed)

  val evaluationState = EvaluationState(simulationRunsWrapperList.size)
  val channel = Channel<Segment?>(capacity = simulationRunPrefetchSize)

  scope.launch {
    while (!evaluationState.isFinished.get()) {
      evaluationState.print()
      delay(1000)
    }
  }

  scope.launch {
    startLoadJob(
        simulationRunsWrapperList,
        useEveryVehicleAsEgo,
        minSegmentTickCount,
        channel,
        evaluationState)
  }

  return generateSequence {
    runBlocking {
      channel.receive().also {
        evaluationState.segmentsBuffer.decrementAndGet()
        if (it == null) evaluationState.isFinished.set(true)
      }
    }
  }
}

private fun loadBlocks(
    simulationRunsWrappers: List<CarlaSimulationRunsWrapper>,
    orderFilesBySeed: Boolean
): List<CarlaSimulationRunsWrapper> {
  var simRuns = simulationRunsWrappers

  // Check that every SimulationRunsWrapper has dynamic data files loaded
  simRuns.forEach {
    check(it.dynamicDataFiles.any()) {
      "The SimulationRunsWrapper with map data file '${it.mapDataFile}' has no dynamic files. Dynamic file " +
          "paths: ${it.dynamicDataFiles.map { dynamicDataFilePath ->  dynamicDataFilePath.toUri() }}"
    }
  }

  // If orderFilesBySeed is set, order the dynamic data files by seed instead of grouped by map name
  if (orderFilesBySeed) {
    // Create a single CarlaSimulationRunsWrapper for each dynamic file
    simRuns =
        simRuns
            .flatMap { wrapper ->
              wrapper.dynamicDataFiles.map {
                CarlaSimulationRunsWrapper(wrapper.mapDataFile, listOf(it))
              }
            }
            .sortedBy {
              // Sort the list by the seed name in the dynamic file name
              getSeed(it.dynamicDataFiles.first().fileName.toString())
            }
  }

  // Load Blocks and save in SimulationRunsWrapper
  simRuns.forEach { it.blocks = loadBlocks(it.mapDataFile).toList() }

  return simRuns
}

private suspend fun startLoadJob(
    simulationRunsWrapperList: List<CarlaSimulationRunsWrapper>,
    useEveryVehicleAsEgo: Boolean,
    minSegmentTickCount: Int,
    segmentChannel: Channel<Segment?>,
    evaluationState: EvaluationState,
) = coroutineScope {
  val simulationsChannel =
      Channel<Triple<CarlaSimulationRunsWrapper, Path, List<JsonTickData>>?>(
          capacity = Channel.UNLIMITED)

  scope.launch {
    startSliceJob(
        useEveryVehicleAsEgo,
        minSegmentTickCount,
        segmentChannel,
        simulationsChannel,
        evaluationState)
  }

  // Iterate all maps
  simulationRunsWrapperList.forEach { simRunWrapper ->
    // Iterate all seeds in current map
    simRunWrapper.dynamicDataFiles.forEach { dynamicDataPath ->
      // println("Reading simulation run file: ${dynamicDataPath.toUri()}")
      simulationsChannel.send(
          Triple(
              simRunWrapper,
              dynamicDataPath,
              getJsonContentOfPath<List<JsonTickData>>(dynamicDataPath)))
      evaluationState.simulationRunsBuffer.incrementAndGet()
    }
    evaluationState.readSimulationRuns.incrementAndGet()
  }
  // If there are no Segments nor Files to process, return null to end sequence
  simulationsChannel.send(null)
}

private suspend fun startSliceJob(
    useEveryVehicleAsEgo: Boolean,
    minSegmentTickCount: Int,
    segmentChannel: Channel<Segment?>,
    simulationsChannel: Channel<Triple<CarlaSimulationRunsWrapper, Path, List<JsonTickData>>?>,
    evaluationState: EvaluationState,
) = coroutineScope {
  while (true) {
    val sim = simulationsChannel.receive()
    evaluationState.simulationRunsBuffer.decrementAndGet()

    if (sim == null) {
      // If there are no Segments nor Files to process, return null to end sequence
      segmentChannel.send(null)
      return@coroutineScope
    }

    val segments =
        sliceRunIntoSegments(
            sim.first.blocks,
            sim.third,
            useEveryVehicleAsEgo,
            sim.second.fileName.toString(),
            minSegmentTickCount)
    evaluationState.slicedSimulationRuns.incrementAndGet()
    segments.forEach { t ->
      segmentChannel.send(t)
      evaluationState.segmentsBuffer.incrementAndGet()
    }
  }
}
