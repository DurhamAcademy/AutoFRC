package kyberlib.auto

import edu.wpi.first.wpilibj.geometry.Pose2d
import edu.wpi.first.wpilibj.geometry.Rotation2d
import edu.wpi.first.wpilibj.geometry.Translation2d
import edu.wpi.first.wpilibj.kinematics.ChassisSpeeds
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveKinematics
import edu.wpi.first.wpilibj.kinematics.MecanumDriveKinematics
import edu.wpi.first.wpilibj.kinematics.SwerveDriveKinematics
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kyberlib.auto.trajectory.KTrajectory
import kyberlib.auto.trajectory.KTrajectoryConfig
import kyberlib.math.units.extensions.*
import kyberlib.math.units.string
import kyberlib.math.units.zeroPose
import kyberlib.sensors.gyros.KGyro
import kyberlib.simulation.field.KField2d

open class Navigator(val gyro: KGyro, startPose: Pose2d = zeroPose) {
    companion object {
        var instance: Navigator? = null
    }
    init {
        instance = this
    }

    val field = KField2d

    /**
     * A probability calculator to guess where the robot is from odometer and vision updates
     */
    private val poseEstimator = DrivePoseEstimator(gyro.heading, startPose)

    /**
     * A object with restrictions on how the robot will move
     */
    var pathingConfig = KTrajectoryConfig(1000.metersPerSecond, 1000.metersPerSecond).apply { KTrajectory.generalConfig = this }
        private set

    fun applyMovementRestrictions(velocity: LinearVelocity, maxAcceleration: LinearVelocity) {
        pathingConfig = KTrajectoryConfig(velocity, maxAcceleration).apply { addConstraints(pathingConfig.constraints) }
        KTrajectory.generalConfig = pathingConfig
    }
    fun applyKinematics(kinematics: DifferentialDriveKinematics) { pathingConfig.setKinematics(kinematics) }
    fun applyKinematics(kinematics: MecanumDriveKinematics) { pathingConfig.setKinematics(kinematics) }
    fun applyKinematics(kinematics: SwerveDriveKinematics) { pathingConfig.setKinematics(kinematics) }

    /**
     * Generate a through a list of positions
     *
     * @param waypoints list of Translation2d that the robot should go through
     */
    fun trajectory(waypoints: MutableList<Translation2d>): KTrajectory {
        val shift = pose.translation - waypoints.first()
        if (shift.norm.meters < 0.5.feet)
            waypoints.removeFirst()
        val endpoint = waypoints.removeLast()
        val finalDelta = endpoint.minus(waypoints.last())
        val finalRotation = Rotation2d(finalDelta.x, finalDelta.y)
        return KTrajectory("auto ${pose.translation.string} -> ${endpoint.string}",
            pose,
            waypoints,
            Pose2d(endpoint, finalRotation),
            pathingConfig
        )
    }
    /**
     * Generate a through a list of positions
     * @param waypoints list of Translation2d that the robot should go through
     */
    fun trajectory(vararg waypoints: Translation2d): KTrajectory = trajectory(waypoints.toMutableList())

    init {
        SmartDashboard.putData("Field", field)
    }

    // ----- public variables ----- //
    // location
    var heading: Angle  // what direction the robot is facing
        get() = gyro.heading
        set(value) {gyro.heading = value}
    var pose: Pose2d  // the location and direction of the robot
        get() = poseEstimator.getEstimatedPosition()
        set(value) {
            poseEstimator.resetPosition(value, heading)
            this.field.robotPose = value
        }
    val position: Translation2d  // the estimated location of the robot
        get() = pose.translation

    /**
     * Update position based on estimated motion
     */
    fun update(speeds: ChassisSpeeds) {  // estimate motion
        poseEstimator.update(heading, speeds)
    }
    /**
     * Update position based on a different position guess
     *
     * @param globalPosition the detected pose of the Robot
     * @param time the time of the detection
     */
    fun update(globalPosition: Pose2d, time: Double) {  // apply global position update
        SmartDashboard.putString("global pose", globalPosition.string)
        poseEstimator.addVisionMeasurement(globalPosition, time)
    }
}