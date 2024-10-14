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

package tools.aqua.stars.importer.carla

import tools.aqua.stars.data.av.dataclasses.*
import tools.aqua.stars.importer.carla.dataclasses.JsonTickData
import tools.aqua.stars.importer.carla.dataclasses.JsonVehicle
import kotlin.math.*

/**
 * Returns the name of the map.
 *
 * @param fileName The filename.
 * @throws IllegalStateException When the [fileName] is not empty and does not include "static_data"
 *   or "dynamic_data".
 */
@Suppress("unused")
fun getMapName(fileName: String): String =
    when {
        fileName.isEmpty() -> "test_case"
        fileName.contains("static_data") -> fileName.split("static_data_")[1].split(".zip")[0]
        fileName.contains("dynamic_data") -> fileName.split("dynamic_data_")[1].split("_seed")[0]
        else -> error("Unknown filename format")
    }

/**
 * Returns the seed value for the given [fileName].
 *
 * @param fileName The filename from which the seed value should be calculated from.
 * @throws IllegalStateException When the [fileName] does not include "dynamic_data".
 */
fun getSeed(fileName: String): Int =
    when {
        fileName.isEmpty() -> 0
        fileName.contains("dynamic_data") ->
            fileName.split("dynamic_data_")[1].split("_seed")[1].split(".")[0].toInt()

        fileName.contains("static_data") ->
            error("Cannot get seed name for map data! Analyzed file: $fileName")

        else -> error("Unknown filename format")
    }

/**
 * Returns the lane progress of a vehicle.
 *
 * @param blocks The list of [Block]s.
 * @param jsonSimulationRun The list of [JsonTickData] in current observation.
 * @param vehicle The [JsonVehicle].
 */
fun getLaneProgressionForVehicle(
    blocks: List<Block>,
    jsonSimulationRun: List<JsonTickData>,
    vehicle: JsonVehicle
): MutableList<Pair<Lane?, Boolean>> {
    val roads = blocks.flatMap { it.roads }
    val lanes = roads.flatMap { it.lanes }
    val laneProgression: MutableList<Pair<Lane?, Boolean>> = mutableListOf()

    jsonSimulationRun.forEach { jsonTickData ->
        val vehiclePosition = jsonTickData.actorPositions.firstOrNull { it.actor.id == vehicle.id }

        if (vehiclePosition == null) {
            laneProgression.add(null to false)
            return@forEach
        }

        val vehicleLane =
            lanes.first { it.laneId == vehiclePosition.laneId && it.road.id == vehiclePosition.roadId }
        val vehicleRoad = roads.first { it.id == vehiclePosition.roadId }
        laneProgression.add(vehicleLane to vehicleRoad.isJunction)
    }

    return laneProgression
}

/**
 * Convert Json data.
 *
 * @param blocks The list of [Block]s.
 * @param jsonSimulationRun The list of [JsonTickData] in current observation.
 * @param useEveryVehicleAsEgo Whether to treat every vehicle as own.
 * @param simulationRunId Identifier of the simulation run.
 */
// @Suppress("LABEL_NAME_CLASH")
@Suppress("unused")
fun convertJsonData(
    blocks: List<Block>,
    jsonSimulationRun: List<JsonTickData>,
    useEveryVehicleAsEgo: Boolean,
    simulationRunId: String
): MutableList<Pair<String, List<TickData>>> {
    var egoVehicles: List<JsonVehicle> =
        jsonSimulationRun.first().actorPositions.map { it.actor }.filterIsInstance<JsonVehicle>()

    if (!useEveryVehicleAsEgo && egoVehicles.any { e -> e.egoVehicle }) {
        egoVehicles = egoVehicles.filter { e -> e.egoVehicle }
    }

    // Stores all simulation runs (List<TickData>) for each ego vehicle
    val simulationRuns = mutableListOf<Pair<String, List<TickData>>>()

    // Stores a complete TickData list which will be cloned for each ego vehicle
    var referenceTickData: List<TickData>? = null

    egoVehicles.forEach { egoVehicle ->
        // If UseEveryVehicleAsEgo is false and there was already on simulationRun recorded: skip next
        // vehicles
        if (simulationRuns.isNotEmpty() && !useEveryVehicleAsEgo) {
            return@forEach
        }

        // Either load data from json or clone existing data
        if (referenceTickData == null) {
            referenceTickData =
                jsonSimulationRun.map { jsonTickData ->
                    convertJsonTickDataToTickData(jsonTickData, blocks)
                }
        }
        val egoTickData = checkNotNull(referenceTickData).map { it.clone() }

        // Remove all existing ego flags when useEveryVehicleAsEgo is set
        if (useEveryVehicleAsEgo) {
            egoTickData.forEach { tickData ->
                tickData.actors.filterIsInstance<Vehicle>().forEach { it.isEgo = false }
            }
        }

        // Set egoVehicle flag for each TickData
        var isTickWithoutEgo = false
        egoTickData.forEach { tickData ->
            if (!isTickWithoutEgo) {
                val egoInTickData =
                    tickData.actors.firstOrNull { it is Vehicle && it.id == egoVehicle.id } as? Vehicle
                if (egoInTickData != null) {
                    egoInTickData.isEgo = true
                } else {
                    isTickWithoutEgo = true
                }
            }
        }

        // There were some simulation runs where some vehicles are not always there.
        // Therefore, check if the egoVehicle was found in each tick
        if (!isTickWithoutEgo) {
            simulationRuns.add(simulationRunId to egoTickData)
        }
    }
    // Update actor velocity as it is not in the JSON data
    simulationRuns.forEach { updateActorVelocityForSimulationRun(it.second) }
    return simulationRuns
}

/**
 * Updates velocity of actors.
 *
 * @param simulationRun List of [TickData].
 */
fun updateActorVelocityForSimulationRun(simulationRun: List<TickData>) {
    for (i in 1 until simulationRun.size) {
        val currentTick = simulationRun[i]
        val previousTick = simulationRun[i - 1]
        currentTick.actors.forEach { currentActor ->
            if (currentActor is Vehicle) {
                updateActorVelocityAndAcceleration(
                    currentActor, previousTick.actors.firstOrNull { it.id == currentActor.id })
            }
        }
    }
}

/**
 * Updates velocity and acceleration of [vehicle].
 *
 * @param vehicle The [Vehicle] to update.
 * @param previousActor The previous [Actor].
 * @throws IllegalStateException iff [previousActor] is not [Vehicle].
 */
fun updateActorVelocityAndAcceleration(vehicle: Vehicle, previousActor: Actor?) {

    // When there is no previous actor position, set velocity and acceleration to 0.0
    if (previousActor == null) {
        vehicle.velocity = Vector3D(0.0, 0.0, 0.0)
        vehicle.acceleration = Vector3D(0.0, 0.0, 0.0)
        return
    }

    check(previousActor is Vehicle) {
        "The Actor with id '${previousActor.id}' from the previous tick is of type '${previousActor::class}' " +
                "but '${Vehicle::class}' was expected."
    }

    // Calculate the time difference
    val timeDelta: Double =
        (vehicle.tickData.currentTick - previousActor.tickData.currentTick).differenceSeconds

    check(timeDelta >= 0) {
        "The time delta between the vehicles is less than 0. Maybe you have switched the vehicles? " +
                "Tick of current vehicle: ${vehicle.tickData.currentTick} vs. previous vehicle: ${previousActor.tickData.currentTick}"
    }

    if (timeDelta == 0.0) {
        // If the time difference is exactly 0.0 set default values, as division by 0.0 is not allowed
        vehicle.velocity = Vector3D(0.0, 0.0, 0.0)
        vehicle.acceleration = Vector3D(0.0, 0.0, 0.0)
    } else {
        // Set velocity and acceleration vector based on velocity values for each direction
        vehicle.velocity = (Vector3D(vehicle.location) - Vector3D(previousActor.location)) / timeDelta
        vehicle.acceleration =
            (Vector3D(vehicle.velocity) - Vector3D(previousActor.velocity) / timeDelta)
    }
}

/**
 * Slices run into segments.
 *
 * @param blocks The list of [Block]s.
 * @param jsonSimulationRun The list of [JsonTickData] in current observation.
 * @param useEveryVehicleAsEgo Whether to treat every vehicle as own.
 * @param simulationRunId Identifier of the simulation run.
 * @param minSegmentTickCount Minimal count of ticks per segment.
 */
fun sliceRunIntoSegments(
    blocks: List<Block>,
    jsonSimulationRun: List<JsonTickData>,
    useEveryVehicleAsEgo: Boolean,
    simulationRunId: String,
    minSegmentTickCount: Int,
    maxSegmentTickCount: Int,
    segmentationBy: Segmentation
): List<Segment> {
    cleanJsonData(blocks, jsonSimulationRun)
    val simulationRuns = convertJsonData(blocks, jsonSimulationRun, useEveryVehicleAsEgo, simulationRunId)

    return when (segmentationBy.type) {
        Segmentation.Type.STATIC_SEGMENT_LENGTH_TICKS -> staticSegmentLengthInTicks(simulationRuns, segmentationBy.value, segmentationBy.secondaryValue)
        Segmentation.Type.STATIC_SEGMENT_LENGTH_METERS -> staticSegmentLengthInMeters(simulationRuns, segmentationBy.value, segmentationBy.secondaryValue)
        Segmentation.Type.DYNAMIC_SEGMENT_LENGTH_METERS_SPEED -> dynamicSegmentLengthForSpeedInMeters(simulationRuns, segmentationBy.value, maxSegmentTickCount)
        Segmentation.Type.DYNAMIC_SEGMENT_LENGTH_METERS_ACCELERATION -> dynamicSegmentLengthForAccelerationInMeters(simulationRuns, segmentationBy.value, maxSegmentTickCount)
        Segmentation.Type.DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_1 -> dynamicSegmentLengthForSpeedAndAccelerationInMeters1(simulationRuns, segmentationBy.value, maxSegmentTickCount)
        Segmentation.Type.DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_2 -> dynamicSegmentLengthForSpeedAndAccelerationInMeters2(simulationRuns, segmentationBy.value, maxSegmentTickCount)
        Segmentation.Type.SLIDING_WINDOW_MULTISTART_METERS -> slidingWindowMultiStartMeters(simulationRuns, segmentationBy.value)
        Segmentation.Type.SLIDING_WINDOW_MULTISTART_TICKS -> slidingWindowMultiStartTicks(simulationRuns, segmentationBy.value)
        //==============================================================================================================
        Segmentation.Type.BY_BLOCK -> segmentByBlock(simulationRuns, minSegmentTickCount)
        Segmentation.Type.NONE -> noSegmentation(simulationRuns, minSegmentTickCount)
        Segmentation.Type.EVEN_SIZE -> segmentWithEvenSize(simulationRuns, minSegmentTickCount, segmentationBy.value, segmentationBy.addJunctions)
        Segmentation.Type.BY_LENGTH -> segmentByLength(simulationRuns, minSegmentTickCount, segmentationBy.value, segmentationBy.addJunctions)
        Segmentation.Type.BY_TICKS -> segmentByTicks(simulationRuns, minSegmentTickCount, segmentationBy.value, segmentationBy.addJunctions)
        Segmentation.Type.BY_SPEED_LIMITS -> segmentBySpeedLimits(simulationRuns, minSegmentTickCount, segmentationBy.addJunctions)
        Segmentation.Type.BY_DYNAMIC_SPEED -> segmentByDynamicSpeed(simulationRuns, minSegmentTickCount)
        Segmentation.Type.BY_DYNAMIC_ACCELERATION -> segmentByDynamicAcceleration(simulationRuns, minSegmentTickCount)
        Segmentation.Type.BY_DYNAMIC_TRAFFIC_DENSITY -> segmentByDynamicTrafficDensity(simulationRuns, minSegmentTickCount)
        Segmentation.Type.BY_DYNAMIC_PEDESTRIAN_PROXIMITY -> segmentByDynamicPedestrianProximity(simulationRuns, minSegmentTickCount)
        Segmentation.Type.BY_DYNAMIC_LANE_CHANGES -> segmentByDynamicLaneChanges(simulationRuns, minSegmentTickCount)
        Segmentation.Type.BY_DYNAMIC_VARIABLES -> segmentByDynamicVariables(simulationRuns, minSegmentTickCount, segmentationBy.addJunctions)
        Segmentation.Type.SLIDING_WINDOW -> slidingWindow(simulationRuns, minSegmentTickCount, segmentationBy.value, segmentationBy.secondaryValue, segmentationBy.addJunctions)
        Segmentation.Type.SLIDING_WINDOW_METERS -> slidingWindowInMeters(simulationRuns, minSegmentTickCount, segmentationBy.value, segmentationBy.secondaryValue, segmentationBy.addJunctions)
        Segmentation.Type.SLIDING_WINDOW_BY_BLOCK -> slidingWindowInBlock(simulationRuns, minSegmentTickCount, segmentationBy.value,segmentationBy.secondaryValue, segmentationBy.addJunctions)
        Segmentation.Type.SLIDING_WINDOW_HALVING -> slidingWindowHalving(simulationRuns, minSegmentTickCount)
        Segmentation.Type.SLIDING_WINDOW_HALF_OVERLAP -> slidingWindowHalfOverlap(simulationRuns, minSegmentTickCount, segmentationBy.value, segmentationBy.addJunctions)
        Segmentation.Type.SLIDING_WINDOW_ROTATING -> slidingWindowRotatingWindowSize(simulationRuns, minSegmentTickCount, segmentationBy.secondaryValue, segmentationBy.addJunctions)
        Segmentation.Type.SLIDING_WINDOW_BY_TRAFFIC_DENSITY -> slidingWindowByTrafficDensity(simulationRuns, minSegmentTickCount, segmentationBy.secondaryValue, segmentationBy.addJunctions)
    }
}

fun getJunctionExtensionBeforeStart(
    index: Int,
    simulationRun: List<TickData>
): List<TickData>{
    var startOfJunction = 0
    for(i in index-1 downTo 0){
        if(!simulationRun[i].egoVehicle.lane.road.isJunction){
            startOfJunction = i+1
            break
        }
    }

    return simulationRun.subList(startOfJunction, index)
}

fun getJunctionExtensionAfterEnd(
    index: Int,
    simulationRun: List<TickData>
): List<TickData>{
    var endOfJunction = simulationRun.size
    for(i in index until simulationRun.size){
        if(!simulationRun[i].egoVehicle.lane.road.isJunction){
            endOfJunction = i
            break
        }
    }

    return simulationRun.subList(index, endOfJunction)
}

//addJunction is used
fun staticSegmentLengthInTicks(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    windowSize: Int,
    stepSize: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        if(windowSize <= simulationRun.size){
            segments.addAll(slideTickWindowOverRun(simulationRun, simulationRunId, windowSize, stepSize))
        }else{
            segments += Segment(simulationRun.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, segmentationType = Segmentation.Type.STATIC_SEGMENT_LENGTH_TICKS)
        }
    }

    return segments
}

fun slideTickWindowOverRun(
    simulationRun: List<TickData>,
    simulationRunId: String,
    windowSize: Int,
    stepSize: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val endOfRun = simulationRun.size

    for(i in 0 until endOfRun step stepSize){
        val junctionExtensionStart = if(simulationRun[i].egoVehicle.lane.road.isJunction) getJunctionExtensionBeforeStart(i, simulationRun) else mutableListOf()
        var junctionExtensionEnd = listOf<TickData>()

        if(i+windowSize > endOfRun){
            val segmentTickData = simulationRun.subList(endOfRun-windowSize, endOfRun)
            segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, segmentationType = Segmentation.Type.STATIC_SEGMENT_LENGTH_TICKS)
            break
        }else{
            if(simulationRun[i+windowSize-1].egoVehicle.lane.road.isJunction) junctionExtensionEnd = getJunctionExtensionAfterEnd(i+windowSize, simulationRun)
        }

        val segmentTickData = junctionExtensionStart + simulationRun.subList(i, i + windowSize) + junctionExtensionEnd
        segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, segmentationType = Segmentation.Type.STATIC_SEGMENT_LENGTH_TICKS)
    }

    return segments
}

//addJunctions is used
fun staticSegmentLengthInMeters(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    windowSize: Int,
    stepSize: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        val distanceTraveled = simulationRun.foldIndexed(0.0) { index, sum, tickData ->
            if(index < simulationRun.size-1){
                val currentLocation = tickData.egoVehicle.location
                val nextLocation= simulationRun[index+1].egoVehicle.location

                sum + abs(currentLocation.distanceTo(nextLocation))
            }else{
                sum
            }
        }

        if(windowSize <= distanceTraveled){
            segments.addAll(slideMeterWindowOverRun(simulationRun, simulationRunId, windowSize, stepSize))
        }else{
            segments += Segment(simulationRun.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, segmentationType = Segmentation.Type.STATIC_SEGMENT_LENGTH_METERS)
        }
    }

    return segments
}

fun slideMeterWindowOverRun(
    simulationRun: List<TickData>,
    simulationRunId: String,
    windowSize: Int,
    stepSizeInMeters: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val lastValidTick = getLastValidTickInRunForMeters(simulationRun, windowSize)
    var i = 0

    while(i <= lastValidTick){
        val endOfWindow = getIndexOfTickInXMeters(simulationRun, i, windowSize).first

        val junctionExtensionStart = if(simulationRun[i].egoVehicle.lane.road.isJunction) getJunctionExtensionBeforeStart(i, simulationRun) else mutableListOf()
        val junctionExtensionEnd = if(simulationRun[i+windowSize-1].egoVehicle.lane.road.isJunction) getJunctionExtensionAfterEnd(i+windowSize, simulationRun) else mutableListOf()
        val segmentTickData = junctionExtensionStart + simulationRun.subList(i, endOfWindow+1) + junctionExtensionEnd

        segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, segmentationType = Segmentation.Type.STATIC_SEGMENT_LENGTH_METERS)

        val stepSize = getIndexOfTickInXMeters(simulationRun, i, stepSizeInMeters).first - i
        i+= stepSize
    }

    val segmentTickData = simulationRun.subList(lastValidTick, simulationRun.size)
    segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, segmentationType = Segmentation.Type.STATIC_SEGMENT_LENGTH_METERS)

    return segments
}

fun getLastValidTickInRunForMeters(simulationRun: List<TickData>, windowSize: Int): Int {
    var distanceTravelled = 0.0
    val lastLocation = simulationRun[simulationRun.size-1].egoVehicle.location
    for(i in simulationRun.size-2 downTo 0){
        val currentLocation = simulationRun[i].egoVehicle.location
        distanceTravelled += abs(currentLocation.distanceTo(lastLocation))
        if(distanceTravelled >= windowSize){
            return i
        }
    }
    return 0
}

fun getIndexOfTickInXMeters(
    tickData: List<TickData>,
    start: Int,
    meters: Int
): Pair<Int,Double> {
    var previousPositionOnLane = tickData[start].egoVehicle.positionOnLane
    var previousRoadId = tickData[start].egoVehicle.lane.road.id
    var distanceTraveled = 0.0
    var i = start+1

    while(i < tickData.size){
        val currentPositionOnLane = tickData[i].egoVehicle.positionOnLane
        val currentRoadId = tickData[i].egoVehicle.lane.road.id

        distanceTraveled += if(currentRoadId == previousRoadId){
            abs(currentPositionOnLane - previousPositionOnLane)
        }else{
            tickData[i].egoVehicle.location.distanceTo(tickData[i-1].egoVehicle.location)
        }

        if (distanceTraveled >= meters) {
            return i to distanceTraveled
        }

        previousPositionOnLane = currentPositionOnLane
        previousRoadId = currentRoadId
        i++
    }

    return tickData.size-1 to distanceTraveled
}

// uses maxSegmentTickCount
// includes segments that are shorter than they should be, because they overlap with the end of the run
// lastValidTick ensures that not too many of these short segments exist, because a tick can only be start
// of a segment if it is at least [lookAhead] meters away from the end of the run
fun dynamicSegmentLengthForSpeedInMeters(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    stepSize: Int,
    maxSegmentTickCount: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val scalar = 300
    val lookAhead = 60

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        val lastValidTick = getLastValidTickInRunForMeters(simulationRun, lookAhead)
        var i  = 0
        while (i <= lastValidTick) {
            val currentSpeed = simulationRun[i].egoVehicle.effVelocityInKmPH
            val windowSize = lookAhead * (1+(currentSpeed/scalar))
            val lastTickInWindow = getIndexOfTickInXMeters(simulationRun, i, windowSize.toInt()).first

            val junctionExtensionStart = if(simulationRun[i].egoVehicle.lane.road.isJunction) getJunctionExtensionBeforeStart(i, simulationRun) else mutableListOf()
            val junctionExtensionEnd = if(simulationRun[lastTickInWindow].egoVehicle.lane.road.isJunction) getJunctionExtensionAfterEnd(lastTickInWindow+1, simulationRun) else mutableListOf()
            var segmentTickData = junctionExtensionStart + simulationRun.subList(i, lastTickInWindow+1) + junctionExtensionEnd

            if (segmentTickData.size > maxSegmentTickCount) {
                segmentTickData = segmentTickData.subList(0, maxSegmentTickCount)
            }
            segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, Segmentation.Type.DYNAMIC_SEGMENT_LENGTH_METERS_SPEED)

            val step = getIndexOfTickInXMeters(simulationRun, i, stepSize).first - i
            i += step
        }
    }

    return segments
}

fun dynamicSegmentLengthForAccelerationInMeters(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    stepSize: Int,
    maxSegmentTickCount: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val scalar = 1
    val lookAhead = 60

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        val lastValidTick = getLastValidTickInRunForMeters(simulationRun, lookAhead)
        var i  = 0
        while (i <= lastValidTick) {
            val currentAcceleration = simulationRun[i].egoVehicle.effAccelerationInMPerSSquared
            val windowSize = scalar * currentAcceleration.pow(2.0) + lookAhead
            val lastTickInWindow = getIndexOfTickInXMeters(simulationRun, i, windowSize.toInt()).first

            val junctionExtensionStart = if(simulationRun[i].egoVehicle.lane.road.isJunction) getJunctionExtensionBeforeStart(i, simulationRun) else mutableListOf()
            val junctionExtensionEnd = if(simulationRun[lastTickInWindow].egoVehicle.lane.road.isJunction) getJunctionExtensionAfterEnd(lastTickInWindow+1, simulationRun) else mutableListOf()
            var segmentTickData = junctionExtensionStart + simulationRun.subList(i, lastTickInWindow+1) + junctionExtensionEnd

            if (segmentTickData.size > maxSegmentTickCount) {
                segmentTickData = segmentTickData.subList(0, maxSegmentTickCount)
            }
            segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, Segmentation.Type.DYNAMIC_SEGMENT_LENGTH_METERS_SPEED)

            val step = getIndexOfTickInXMeters(simulationRun, i, stepSize).first - i
            i += step
        }
    }

    return segments
}

// uses maxSegmentTickCount
// includes segments that are shorter than they should be, because they overlap with the end of the run
// lastValidTick ensures that not too many of these short segments exist, because a tick can only be start
// of a segment if it is at least [lookAhead] meters away from the end of the run
fun dynamicSegmentLengthForSpeedAndAccelerationInMeters1(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    stepSize: Int,
    maxSegmentTickCount: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val lookAhead = 30

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        val lastValidTick = getLastValidTickInRunForMeters(simulationRun, lookAhead)
        var i  = 0
        while (i <= lastValidTick) {
            val currentAcceleration = simulationRun[i].egoVehicle.effAccelerationInMPerSSquared
            val currentSpeed = simulationRun[i].egoVehicle.effVelocityInKmPH

            val reactionDistance = (currentAcceleration/2)* 1.2.pow(2.0) + currentSpeed * 1.2
            val brakingDistance = (currentSpeed/10).pow(2.0) * 0.5

            val windowSize = lookAhead + reactionDistance + brakingDistance
            val lastTickInWindow = getIndexOfTickInXMeters(simulationRun, i, windowSize.toInt()).first

            val junctionExtensionStart = if(simulationRun[i].egoVehicle.lane.road.isJunction) getJunctionExtensionBeforeStart(i, simulationRun) else mutableListOf()
            val junctionExtensionEnd = if(simulationRun[lastTickInWindow].egoVehicle.lane.road.isJunction) getJunctionExtensionAfterEnd(lastTickInWindow+1, simulationRun) else mutableListOf()
            var segmentTickData = junctionExtensionStart + simulationRun.subList(i, lastTickInWindow+1) + junctionExtensionEnd

            if (segmentTickData.size > maxSegmentTickCount) {
                segmentTickData = segmentTickData.subList(0, maxSegmentTickCount)
            }
            segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, Segmentation.Type.DYNAMIC_SEGMENT_LENGTH_METERS_ACCELERATION)

            val step = getIndexOfTickInXMeters(simulationRun, i, stepSize).first - i
            i += step
        }
    }

    return segments
}

// uses maxSegmentTickCount
// includes segments that are shorter than they should be, because they overlap with the end of the run
// lastValidTick ensures that not too many of these short segments exist, because a tick can only be start
// of a segment if it is at least [lookAhead] meters away from the end of the run
fun dynamicSegmentLengthForSpeedAndAccelerationInMeters2(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    stepSize: Int,
    maxSegmentTickCount: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val scalar = 30
    val lookAhead = 30

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        val lastValidTick = getLastValidTickInRunForMeters(simulationRun, lookAhead)
        var i  = 0
        while (i <= lastValidTick) {
            val currentSpeed = simulationRun[i].egoVehicle.effVelocityInKmPH
            val currentAcceleration = simulationRun[i].egoVehicle.effAccelerationInMPerSSquared
            val windowSize = lookAhead * (1+(currentSpeed/scalar)) + abs(currentAcceleration)*5
            val lastTickInWindow = getIndexOfTickInXMeters(simulationRun, i, windowSize.toInt()).first

            val junctionExtensionStart = if(simulationRun[i].egoVehicle.lane.road.isJunction) getJunctionExtensionBeforeStart(i, simulationRun) else mutableListOf()
            val junctionExtensionEnd = if(simulationRun[lastTickInWindow].egoVehicle.lane.road.isJunction) getJunctionExtensionAfterEnd(lastTickInWindow+1, simulationRun) else mutableListOf()
            var segmentTickData = junctionExtensionStart + simulationRun.subList(i, lastTickInWindow+1) + junctionExtensionEnd

            if (segmentTickData.size > maxSegmentTickCount) {
                segmentTickData = segmentTickData.subList(0, maxSegmentTickCount)
            }
            segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, Segmentation.Type.DYNAMIC_SEGMENT_LENGTH_METERS_SPEED)

            val step = getIndexOfTickInXMeters(simulationRun, i, stepSize).first - i
            i += step
        }
    }

    return segments
}

fun slidingWindowMultiStartMeters(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    overlap: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val windowSizes = mutableListOf(60,65,70,75,80)

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        windowSizes.forEach { size ->
            val stepSize = max((size*(1-(overlap/100.0))).toInt(),1)
            segments.addAll(slideMeterWindowOverRun(simulationRun, simulationRunId, size, stepSize))
        }
    }

    return segments
}

fun slidingWindowMultiStartTicks(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    overlap: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val windowSizes = mutableListOf(100,110,120,130,140)

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        windowSizes.forEach { size ->
            val stepSize = max((size*(1-(overlap/100.0))).toInt(),1)
            segments.addAll(slideTickWindowOverRun(simulationRun, simulationRunId, size, stepSize))
        }
    }

    return segments
}

//======================================================================================================================
//#region oldSegmentation

fun segmentByBlock(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        val blockRanges = mutableListOf<Pair<TickDataUnitSeconds, TickDataUnitSeconds>>()
        var prevBlockID = simulationRun.first().egoVehicle.lane.road.block.id
        var firstTickInBlock = simulationRun.first().currentTick

        simulationRun.forEachIndexed { index, tick ->
            val currentBlockID = tick.egoVehicle.lane.road.block.id
            if (currentBlockID != prevBlockID) {
                blockRanges += (firstTickInBlock to simulationRun[index - 1].currentTick)
                prevBlockID = currentBlockID
                firstTickInBlock = tick.currentTick
            }
        }
        blockRanges += (firstTickInBlock to simulationRun.last().currentTick)

        blockRanges.forEachIndexed { _, blockRange ->
            val mainSegment =
                simulationRun
                    .filter { it.currentTick in blockRange.first..blockRange.second }
            if (mainSegment.size >= minSegmentTickCount) {
                segments +=
                    Segment(mainSegment.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, Segmentation.Type.BY_BLOCK)
            }
        }
    }

    return segments
}

fun noSegmentation(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        if (simulationRun.size >= minSegmentTickCount) {
            segments += Segment(
                simulationRun,
                simulationRunId = simulationRunId,
                segmentSource = simulationRunId
            )
        }
    }

    return segments
}

fun segmentWithEvenSize(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
    segmentationSize: Int,
    addJunctions: Boolean
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val blocks = segmentByBlock(simulationRuns, minSegmentTickCount)

    blocks.forEach { originalSegment ->
        if (originalSegment.tickData.any { it.egoVehicle.lane.road.isJunction } && addJunctions) {
            segments += originalSegment
        } else {
            val originalSize = originalSegment.tickData.size
            val segmentLength = originalSize / segmentationSize

            for (i in 0 until segmentationSize) {
                val start = i * segmentLength
                val end = if (i == segmentationSize - 1) originalSize else (i + 1) * segmentLength
                val segmentTickData = originalSegment.tickData.subList(start, end)
                if (segmentTickData.size >= minSegmentTickCount) {
                    segments += Segment(
                        segmentTickData,
                        simulationRunId = originalSegment.simulationRunId,
                        segmentSource = originalSegment.simulationRunId
                    )
                } else {
                    println("Segment too short: Id(${originalSegment.simulationRunId}) - Size(${segmentTickData.size})")

                }
            }
        }
    }

    return segments
}

fun segmentByLength(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
    segmentLength: Int,
    addJunctions: Boolean
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val blocks = segmentByBlock(simulationRuns, minSegmentTickCount)

    blocks.forEach { originalSegment ->
        if (originalSegment.tickData.any { it.egoVehicle.lane.road.isJunction } && addJunctions) {
            segments += originalSegment
        } else {
            var previousPositionOnLane = originalSegment.tickData.first().egoVehicle.positionOnLane
            var distanceTraveled = 0.0
            var lastCutIndex = 0

            for (i in 1 until originalSegment.tickData.size) {
                val currentPositionOnLane = originalSegment.tickData[i].egoVehicle.positionOnLane
                distanceTraveled += abs(currentPositionOnLane - previousPositionOnLane)
                if (distanceTraveled >= segmentLength) {
                    val segmentTickData = originalSegment.tickData.subList(lastCutIndex, i)
                    lastCutIndex = i
                    if (segmentTickData.size >= minSegmentTickCount) {
                        segments += Segment(segmentTickData, simulationRunId = originalSegment.simulationRunId, segmentSource = originalSegment.simulationRunId)
                    } else {
                        println("Segment too short: Id(${originalSegment.simulationRunId}) - Size(${segmentTickData.size})")
                    }
                    previousPositionOnLane = currentPositionOnLane
                    distanceTraveled = 0.0
                }
            }

            val segmentTickData = originalSegment.tickData.subList(lastCutIndex, originalSegment.tickData.size)
            if (segmentTickData.size >= minSegmentTickCount) {
                segments += Segment(segmentTickData, simulationRunId = originalSegment.simulationRunId, segmentSource = originalSegment.simulationRunId)
            } else {
                println("Last segment too short: Id(${originalSegment.simulationRunId}) - Size(${segmentTickData.size})")
            }
        }
    }

    return segments
}

fun segmentByTicks(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
    segmentTickCount: Int,
    addJunctions: Boolean
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val blocks = segmentByBlock(simulationRuns, minSegmentTickCount)

    blocks.forEach { originalSegment ->
        if (originalSegment.tickData.any { it.egoVehicle.lane.road.isJunction } && addJunctions) {
            segments += originalSegment
        } else {
            val originalSize = originalSegment.tickData.size
            val segmentCount = ceil(originalSize.toDouble() / segmentTickCount.toDouble()).toInt()

            for (i in 0 until segmentCount) {
                val start = i * segmentTickCount
                val end = if (i == segmentCount - 1) originalSize else (i + 1) * segmentTickCount
                val segmentTickData = originalSegment.tickData.subList(start, end)
                if (segmentTickData.size >= minSegmentTickCount) {
                    segments += Segment(
                        segmentTickData,
                        simulationRunId = originalSegment.simulationRunId,
                        segmentSource = originalSegment.simulationRunId
                    )
                } else {
                    println("Segment too short: Id(${originalSegment.simulationRunId}) - Size(${segmentTickData.size})")
                }
            }
        }
    }

    return segments
}

fun segmentBySpeedLimits(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
    addJunctions: Boolean
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val blocks = segmentByBlock(simulationRuns, minSegmentTickCount)

    blocks.forEach { originalSegment ->
        if (originalSegment.tickData.any { it.egoVehicle.lane.road.isJunction } && addJunctions) {
            segments += originalSegment
        } else {
            var previousSpeedLimit = originalSegment.tickData.first().egoVehicle.applicableSpeedLimit?: SpeedLimit(0.0, 0.0, 0.0)
            var lastCutIndex = 0

            for (i in 1 until originalSegment.tickData.size) {
                val currentSpeedLimit = originalSegment.tickData[i].egoVehicle.applicableSpeedLimit?: SpeedLimit(0.0, 0.0, 0.0)
                if (currentSpeedLimit.speedLimit != previousSpeedLimit.speedLimit) {
                    val segmentTickData = originalSegment.tickData.subList(lastCutIndex, i)
                    lastCutIndex = i
                    if (segmentTickData.size >= minSegmentTickCount) {
                        segments += Segment(segmentTickData, simulationRunId = originalSegment.simulationRunId, segmentSource = originalSegment.simulationRunId)
                    } else {
                        println("Segment too short: Id(${originalSegment.simulationRunId}) - Size(${segmentTickData.size})")
                    }
                    previousSpeedLimit = currentSpeedLimit
                }
            }

            val segmentTickData = originalSegment.tickData.subList(lastCutIndex, originalSegment.tickData.size)
            if (segmentTickData.size >= minSegmentTickCount) {
                segments += Segment(segmentTickData, simulationRunId = originalSegment.simulationRunId, segmentSource = originalSegment.simulationRunId)
            } else {
                println("Last segment too short: Id(${originalSegment.simulationRunId}) - Size(${segmentTickData.size})")
            }
        }
    }

    return segments
}

fun segmentByDynamicSpeed(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
    speedRanges : List<Double> = listOf(15.0,35.0,60.0,90.0,130.0,Double.MAX_VALUE)
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()

    simulationRuns.forEach { (simulationRunId, tickData) ->
        var previousSpeedRangeIndex = speedRanges.indexOfFirst { it > tickData.first().egoVehicle.effVelocityInKmPH }
        var lastCutIndex = 0

        for (i in 1 until tickData.size) {
            val currentSpeedRangeIndex = speedRanges.indexOfFirst { it > tickData[i].egoVehicle.effVelocityInKmPH }
            if (currentSpeedRangeIndex != previousSpeedRangeIndex) {
                val segmentTickData = tickData.subList(lastCutIndex, i)
                lastCutIndex = i
                if (segmentTickData.size >= minSegmentTickCount) {
                    segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, Segmentation.Type.BY_DYNAMIC_SPEED)
                } else {
                    println("Segment too short: Id(${simulationRunId}) - Size(${segmentTickData.size})")
                }
                previousSpeedRangeIndex = currentSpeedRangeIndex
            }
        }

        val segmentTickData = tickData.subList(lastCutIndex, tickData.size)
        if (segmentTickData.size >= minSegmentTickCount) {
            segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, Segmentation.Type.BY_DYNAMIC_SPEED)
        } else {
            println("Last segment too short: Id(${simulationRunId}) - Size(${segmentTickData.size})")
        }
    }

    return segments
}

fun segmentByDynamicTrafficDensity(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val trafficDensityRanges = listOf(6,16,Int.MAX_VALUE)

    simulationRuns.forEach { (simulationRunId, tickData) ->
            val firstTick = tickData.first()
            var previousTrafficDensityRangeIndex = trafficDensityRanges.indexOfFirst { it > firstTick.vehiclesInBlock(firstTick.egoVehicle.lane.road.block).size }
            var lastCutIndex = 0

            for (i in 1 until tickData.size) {
                val currentTick = tickData[i]
                val currentTrafficDensityRangeIndex = trafficDensityRanges.indexOfFirst { it > currentTick.vehiclesInBlock(currentTick.egoVehicle.lane.road.block).size }
                if (currentTrafficDensityRangeIndex != previousTrafficDensityRangeIndex) {
                    val segmentTickData = tickData.subList(lastCutIndex, i)
                    lastCutIndex = i
                    if (segmentTickData.size >= minSegmentTickCount) {
                        segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, Segmentation.Type.BY_DYNAMIC_TRAFFIC_DENSITY)
                    } else {
                        println("Segment too short: Id(${simulationRunId}) - Size(${segmentTickData.size})")
                    }
                    previousTrafficDensityRangeIndex = currentTrafficDensityRangeIndex
                }
            }

            val segmentTickData = tickData.subList(lastCutIndex, tickData.size)
            if (segmentTickData.size >= minSegmentTickCount) {
                segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, Segmentation.Type.BY_DYNAMIC_TRAFFIC_DENSITY)
            } else {
                println("Last segment too short: Id(${simulationRunId}) - Size(${segmentTickData.size})")
            }
        }

    return segments
}

fun segmentByDynamicAcceleration(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val accelerationRanges = listOf(-0.5,0.5,Double.MAX_VALUE)

    simulationRuns.forEach { (simulationRunId, tickData) ->
            var previousAccelerationRangeIndex = accelerationRanges.indexOfFirst { it > tickData.first().egoVehicle.effAccelerationInMPerSSquared }
            var lastCutIndex = 0

            for (i in 1 until tickData.size) {
                val currentAccelerationRangeIndex = accelerationRanges.indexOfFirst { it > tickData[i].egoVehicle.effAccelerationInMPerSSquared }
                if (currentAccelerationRangeIndex != previousAccelerationRangeIndex) {
                    val segmentTickData = tickData.subList(lastCutIndex, i)
                    lastCutIndex = i
                    if (segmentTickData.size >= minSegmentTickCount) {
                        segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, Segmentation.Type.BY_DYNAMIC_ACCELERATION)
                    } else {
                        println("Segment too short: Id(${simulationRunId}) - Size(${segmentTickData.size})")
                    }
                    previousAccelerationRangeIndex = currentAccelerationRangeIndex
                }
            }

            val segmentTickData = tickData.subList(lastCutIndex, tickData.size)
            if (segmentTickData.size >= minSegmentTickCount) {
                segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, Segmentation.Type.BY_DYNAMIC_ACCELERATION)
            } else {
                println("Last segment too short: Id(${simulationRunId}) - Size(${segmentTickData.size})")
            }
        }

    return segments
}

fun segmentByDynamicPedestrianProximity(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()

    simulationRuns.forEach { (simulationRunId, tickData) ->
            val distanceToPreviousPedestrian = tickData.first().pedestrians.map { ped ->
                if(ped.lane.laneType == LaneType.Driving){
                    ped.location.distanceTo(tickData.first().egoVehicle.location)
                } else {
                    Double.MAX_VALUE
                }
            }.toList().minOrNull()?: Double.MAX_VALUE
            var previousPedestrianWasClose = distanceToPreviousPedestrian < 30.0

            var lastCutIndex = 0

            for (i in 0 until tickData.size) {
                val distanceToCurrentPedestrian = tickData[i].pedestrians.map { ped ->
                    if(ped.lane.laneType == LaneType.Driving){
                        ped.location.distanceTo(tickData[i].egoVehicle.location)
                    } else {
                        Double.MAX_VALUE
                    }
                }.toList().minOrNull()?: Double.MAX_VALUE

                val currentPedestrianWasClose = distanceToCurrentPedestrian < 30.0
                if (previousPedestrianWasClose != currentPedestrianWasClose) {
                    val segmentTickData = tickData.subList(lastCutIndex, i)
                    lastCutIndex = i
                    if (segmentTickData.size >= minSegmentTickCount) {
                        segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, Segmentation.Type.BY_DYNAMIC_PEDESTRIAN_PROXIMITY)
                    } else {
                        println("Segment too short: Id(${simulationRunId}) - Size(${segmentTickData.size})")
                    }
                    previousPedestrianWasClose = currentPedestrianWasClose
                }
            }

            val segmentTickData = tickData.subList(lastCutIndex, tickData.size)
            if (segmentTickData.size >= minSegmentTickCount) {
                segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, Segmentation.Type.BY_DYNAMIC_PEDESTRIAN_PROXIMITY)
            } else {
                println("Last segment too short: Id(${simulationRunId}) - Size(${segmentTickData.size})")
            }
        }

    return segments
}

fun segmentByDynamicLaneChanges(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()

    simulationRuns.forEach { (simulationRunId, tickData) ->
            var previousLane = tickData.first().egoVehicle.lane

            for (i in 1 until tickData.size) {
                val currentLane = tickData[i].egoVehicle.lane
                if (currentLane != previousLane) {
                    val segmentTickData = tickData.subList(max(i-10,0), min(i+100,tickData.size))
                    if (segmentTickData.size >= minSegmentTickCount) {
                        segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId, Segmentation.Type.BY_DYNAMIC_LANE_CHANGES)
                    } else {
                        println("Segment too short: Id(${simulationRunId}) - Size(${segmentTickData.size})")
                    }
                    previousLane = currentLane
                }
            }
        }

    return segments
}

fun segmentByDynamicVariables(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
    addJunctions: Boolean
): MutableList<Segment> {
    val segmentsByBlock = segmentByBlock(simulationRuns, minSegmentTickCount)
    val segmentsByAcceleration = segmentByDynamicAcceleration(simulationRuns, minSegmentTickCount)
    val segmentsBySpeed = segmentByDynamicSpeed(simulationRuns, minSegmentTickCount)
    val segmentsByTrafficDensity = segmentByDynamicTrafficDensity(simulationRuns, minSegmentTickCount)
    val segmentsByPedestrianProximity = segmentByDynamicPedestrianProximity(simulationRuns, minSegmentTickCount)
    val segmentsByLaneChanges = segmentByDynamicLaneChanges(simulationRuns, minSegmentTickCount)
    val slidingWindowQuarterOverlap = slidingWindowHalfOverlap(simulationRuns, minSegmentTickCount, 100, addJunctions)

    val allSegments = mutableListOf<Segment>().apply {
        addAll(segmentsByBlock)
        addAll(segmentsByAcceleration)
        addAll(segmentsBySpeed)
        addAll(segmentsByTrafficDensity)
        addAll(segmentsByPedestrianProximity)
        addAll(segmentsByLaneChanges)
        addAll(slidingWindowQuarterOverlap)
    }

    return allSegments
}

fun slidingWindow(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
    size: Int,
    stepSize: Int,
    addJunctions: Boolean
): MutableList<Segment> {
    var windowSize = size
    if(windowSize < minSegmentTickCount){
        println("ADJUSTING WINDOW SIZE: Window size is smaller than minSegmentTickCount. Adjusting window size to $minSegmentTickCount.")
        windowSize = minSegmentTickCount
    }

    val segments = mutableListOf<Segment>()
    if(addJunctions){
        val junctions = segmentByBlock(simulationRuns, minSegmentTickCount).filter { it.tickData.first().egoVehicle.lane.road.isJunction }
        segments += junctions
    }

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        segments.addAll(performSlidingWindowOnSimulationRun(simulationRun, simulationRunId, minSegmentTickCount, windowSize, stepSize))
    }

    return segments
}

fun performSlidingWindowOnSimulationRun(
    simulationRun: List<TickData>,
    simulationRunId: String,
    minSegmentTickCount: Int,
    windowSize: Int,
    stepSize: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val endOfRun = simulationRun.size

    for(i in 0 until endOfRun step stepSize){
        if(i+windowSize >= endOfRun){
            break
        }

        val segmentTickData = simulationRun.subList(i, i + windowSize)
        if (segmentTickData.size >= minSegmentTickCount) {
            segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId)
        }
    }

    return segments
}

fun slidingWindowInMeters(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
    size: Int,
    stepSize: Int,
    addJunctions: Boolean
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    if(addJunctions){
        val junctions = segmentByBlock(simulationRuns, minSegmentTickCount).filter { it.tickData.first().egoVehicle.lane.road.isJunction }
        segments += junctions
    }

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        segments.addAll(performSlidingWindowOnSimulationRunInMeters(simulationRun, simulationRunId, minSegmentTickCount, size, stepSize))
    }

    return segments
}

fun performSlidingWindowOnSimulationRunInMeters(
    simulationRun: List<TickData>,
    simulationRunId: String,
    minSegmentTickCount: Int,
    windowSize: Int,
    stepSizeInMeters: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val endOfRun = simulationRun.size
    var i = 0

    while(i < endOfRun){
        val endOfWindow = getIndexOfTickInXMeters(simulationRun, i, windowSize).first
        val segmentTickData = simulationRun.subList(i, min(endOfWindow+1, endOfRun))

        if (segmentTickData.size >= minSegmentTickCount) {
            segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId)
        } else {
            println("Segment too short: Id(${simulationRunId}) - Size(${segmentTickData.size})")
        }

        val stepSize = getIndexOfTickInXMeters(simulationRun, i, stepSizeInMeters).first - i
        i+= stepSize
    }

    return segments
}

fun slidingWindowInBlock(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
    size: Int,
    stepSize: Int,
    addJunctions: Boolean
): MutableList<Segment> {
    var windowSize = size
    if(windowSize < minSegmentTickCount){
        println("ADJUSTING WINDOW SIZE: Window size is smaller than minSegmentTickCount. Adjusting window size to $minSegmentTickCount.")
        windowSize = minSegmentTickCount
    }

    val segments = mutableListOf<Segment>()

    val blocks = segmentByBlock(simulationRuns, minSegmentTickCount)
    blocks.forEach { originalSegment ->
        if (originalSegment.tickData.any { it.egoVehicle.lane.road.isJunction } && addJunctions) {
            segments += originalSegment
        } else {
            val endOfBlock = originalSegment.tickData.size

            for(i in 0 until endOfBlock step stepSize){
                if(i+windowSize >= endOfBlock){
                    if(i == 0) segments += originalSegment
                    break
                }

                val segmentTickData = originalSegment.tickData.subList(i, i + windowSize)
                if (segmentTickData.size >= minSegmentTickCount) {
                    segments += Segment(segmentTickData, simulationRunId = originalSegment.simulationRunId, segmentSource = originalSegment.simulationRunId)
                }
            }
        }
    }

    return segments
}

fun slidingWindowHalving(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        var size = simulationRun.size
        println(size)
        for(i in 0 until 5){
            if(size >= minSegmentTickCount){
                segments.addAll(performSlidingWindowOnSimulationRun(simulationRun, simulationRunId, minSegmentTickCount, size, (size*0.1).toInt()))
                size /= 2
            }
        }

    }

    return segments
}

fun slidingWindowHalfOverlap(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
    size: Int,
    addJunctions: Boolean
): MutableList<Segment> {
    var windowSize = size
    if(windowSize < minSegmentTickCount){
        println("ADJUSTING WINDOW SIZE: Window size is smaller than minSegmentTickCount. Adjusting window size to $minSegmentTickCount.")
        windowSize = minSegmentTickCount
    }

    val segments = mutableListOf<Segment>()
    if(addJunctions){
        val junctions = segmentByBlock(simulationRuns, minSegmentTickCount).filter { it.tickData.first().egoVehicle.lane.road.isJunction }
        segments += junctions
    }

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        segments.addAll(performSlidingWindowOnSimulationRun(simulationRun, simulationRunId, minSegmentTickCount, windowSize, windowSize/4))
    }

    return segments
}

fun slidingWindowRotatingWindowSize(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
    stepSize: Int,
    addJunctions: Boolean
): MutableList<Segment>{
    val segments = mutableListOf<Segment>()
    val windowSizes = mutableListOf(60,65,70,75,80)

    if(addJunctions){
        val junctions = segmentByBlock(simulationRuns, minSegmentTickCount).filter { it.tickData.first().egoVehicle.lane.road.isJunction }
        segments += junctions
    }

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        segments.addAll(performSlidingRotatingWindowOnSimulationRun(simulationRun, simulationRunId, minSegmentTickCount, windowSizes, stepSize))
    }

    return segments
}

fun performSlidingRotatingWindowOnSimulationRun(
    simulationRun: List<TickData>,
    simulationRunId: String,
    minSegmentTickCount: Int,
    windowSizes: List<Int> = listOf(60,65,70,75,80),
    stepSize: Int
): MutableList<Segment> {
    val segments = mutableListOf<Segment>()
    val endOfRun = simulationRun.size

    for(i in 0 until endOfRun step stepSize){
        val windowSize = windowSizes.random()
        if(i+windowSize >= endOfRun){
            break
        }

        val segmentTickData = simulationRun.subList(i, i + windowSize)
        if (segmentTickData.size >= minSegmentTickCount) {
            segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId)
        }
    }

    return segments
}

fun slidingWindowByTrafficDensity(
    simulationRuns: MutableList<Pair<String, List<TickData>>>,
    minSegmentTickCount: Int,
    stepSize: Int,
    addJunctions: Boolean
): MutableList<Segment>{
    val segments = mutableListOf<Segment>()
    val trafficDensityRanges = listOf(6,16,Int.MAX_VALUE)
    val windowSizes = listOf(60, 70, 80)

    if(addJunctions){
        val junctions = segmentByBlock(simulationRuns, minSegmentTickCount).filter { it.tickData.first().egoVehicle.lane.road.isJunction }
        segments += junctions
    }

    simulationRuns.forEach { (simulationRunId, simulationRun) ->
        val endOfRun = simulationRun.size

        for(i in 0 until endOfRun step stepSize){
            val currentTrafficDensityRangeIndex = trafficDensityRanges.indexOfFirst { it > simulationRun[i].vehiclesInBlock(simulationRun[i].egoVehicle.lane.road.block).size }
            val windowSize = windowSizes[currentTrafficDensityRangeIndex]

            if(i+windowSize >= endOfRun){
                break
            }

            val segmentTickData = simulationRun.subList(i, i + windowSize)
            if (segmentTickData.size >= minSegmentTickCount) {
                segments += Segment(segmentTickData.map { it.clone() }, simulationRunId = simulationRunId, segmentSource = simulationRunId)
            }
        }

    }

    return segments
}
//#endregion


/**
 * Cleans Json data.
 *
 * @param blocks The list of [Block]s.
 * @param jsonSimulationRun The list of [JsonTickData] in current observation.
 */
fun cleanJsonData(blocks: List<Block>, jsonSimulationRun: List<JsonTickData>) {
    val vehicles =
        jsonSimulationRun
            .flatMap { it.actorPositions }
            .map { it.actor }
            .filterIsInstance<JsonVehicle>()
            .distinctBy { it.id }
    vehicles.forEach { vehicle ->
        val laneProgression = getLaneProgressionForVehicle(blocks, jsonSimulationRun, vehicle)

        // Saves the lane progression of the current vehicle as a list of Triple(RoadId, LaneId,
        // IsJunction)
        var previousMultilane: Lane? = null
        var nextMultilane: Lane?
        val currentJunction: MutableList<Pair<Int, Lane>> = mutableListOf()

        laneProgression.forEachIndexed { index: Int, (lane: Lane?, isJunction: Boolean) ->
            if (lane == null) {
                return@forEach
            }
            if (!isJunction) {
                if (currentJunction.isNotEmpty()) {
                    nextMultilane = lane
                    cleanJunctionData(
                        jsonSimulationRun, currentJunction, previousMultilane, nextMultilane, vehicle
                    )
                    currentJunction.clear()
                    previousMultilane = lane
                } else {
                    previousMultilane = lane
                }
            } else {
                currentJunction.add(index to lane)
            }
        }
        // The junction is the last block in the TickData.
        // Call with laneTo=null as there is no successor lane
        if (currentJunction.isNotEmpty()) {
            cleanJunctionData(jsonSimulationRun, currentJunction, previousMultilane, null, vehicle)
        }
    }
}

/**
 * Cleans junction data.
 *
 * @param jsonSimulationRun The list of [JsonTickData] in current observation.
 * @param junctionIndices Indices of the junctions.
 * @param laneFrom Incoming [Lane].
 * @param laneTo Outgoing [Lane].
 * @param vehicle The [JsonVehicle].
 */
private fun cleanJunctionData(
    jsonSimulationRun: List<JsonTickData>,
    junctionIndices: List<Pair<Int, Lane>>,
    laneFrom: Lane?,
    laneTo: Lane?,
    vehicle: JsonVehicle
) {
    // Check if the lanes are already all the same
    val junctionLaneGroups = junctionIndices.groupBy { it.second.toString() }
    if (junctionLaneGroups.size == 1) {
        return
    }
    val newLane: Lane?

    // Check which lane is mostly in the TickData
    var greatestGroup: Pair<Lane?, Int> = null to 0
    junctionLaneGroups.values.forEach {
        if (it.size > greatestGroup.second) {
            greatestGroup = it.first().second to it.size
        }
    }
    // There is at least one outlier: Clean up
    newLane =
        if (laneFrom == null || laneTo == null) {
            // The current junction is at the beginning or the end of the simulation run
            // Just take the lane which occurs more often
            greatestGroup.first
        } else if (laneFrom == laneTo) {
            // When there is a junction outlier in a multilane road just take laneFrom
            laneFrom
        } else {
            // The current junction has TickData which include MultiLane roads
            // Get connecting lane between laneFrom and laneTo
            val laneIntersect = laneFrom.successorLanes.intersect(laneTo.predecessorLanes.toSet())
            if (laneIntersect.isNotEmpty()) {
                laneIntersect.first().lane
            } else {
                // Apparently Roundabouts have connected lanes within the same road
                // To see this run Town3_Opt with seed 8 with the following code in python:
                // road_1608 = rasterizer.get_data_road(1608)
                // rasterizer.debug_road(road_1608)

                // Check for successor/predecessor connection with one step between
                val laneFromSuccessorSuccessors =
                    laneFrom.successorLanes.flatMap { it.lane.successorLanes }
                val laneToPredecessors = laneTo.predecessorLanes
                val junctionIntersect = laneFromSuccessorSuccessors.intersect(laneToPredecessors.toSet())
                if (junctionIntersect.isNotEmpty()) {
                    junctionIntersect.first().lane
                } else {
                    // Lane change in a junction
                    // See Seed34 Lane 483, which is technically a junction but only for the other side
                    null
                }
            }
        }
    if (newLane != null) {
        junctionIndices.forEach { (index, _) ->
            val vehiclePositionToUpdate =
                jsonSimulationRun[index].actorPositions.firstOrNull { it.actor.id == vehicle.id }
            checkNotNull(vehiclePositionToUpdate)
            vehiclePositionToUpdate.laneId = newLane.laneId
            vehiclePositionToUpdate.roadId = newLane.road.id
        }
    }
}
