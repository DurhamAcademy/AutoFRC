package kyberlib.mechanisms.drivetrain

import edu.wpi.first.wpilibj.geometry.Pose2d
import edu.wpi.first.wpilibj.geometry.Translation2d
import edu.wpi.first.wpilibj.kinematics.ChassisSpeeds
import edu.wpi.first.wpilibj.kinematics.MecanumDriveKinematics
import edu.wpi.first.wpilibj.kinematics.MecanumDriveOdometry
import edu.wpi.first.wpilibj.kinematics.MecanumDriveWheelSpeeds
import edu.wpi.first.wpilibj2.command.SubsystemBase
import kyberlib.math.units.extensions.KRotation
import kyberlib.math.units.extensions.degrees
import kyberlib.math.units.extensions.metersPerSecond
import kyberlib.math.units.zeroPose
import kyberlib.motorcontrol.KMotorController
import kyberlib.sensors.gyros.KGyro

/**
 * Pre-made Drivetrain for MecanumWheels.
 * @param packages pairs of <wheelPosition, wheel motor> (currently requires 4 motors in frontLeft, frontRight, backLeft, backRight order)
 * @param gyro a KGyro that will provide heading information
 */
class MecanumDrivetrain(vararg packages: Pair<Translation2d, KMotorController>, private val gyro: KGyro) : KDrivetrain, SubsystemBase() {
    init {
        assert(packages.size == 4) { "the pre-made drivetrain only accepts 4 wheel mecanum drives" }
    }
    private val motors = packages.map { it.second }.toTypedArray()
    private val frontLeft = motors[0]
    private val frontRight = motors[1]
    private val backLeft = motors[2]
    private val backRight = motors[3]
    private val locations = packages.map { it.first }.toTypedArray()

    // changing this will normalize the top speed that a motor can have
    var maxVelocity = 0.metersPerSecond

    private val kinematics = MecanumDriveKinematics(locations[0], locations[1], locations[2], locations[3])
    private val odometry = MecanumDriveOdometry(kinematics, gyro.heading)

    private val mecanumDriveWheelSpeeds
        get() = MecanumDriveWheelSpeeds(frontLeft.linearVelocity.metersPerSecond, frontRight.linearVelocity.metersPerSecond, backLeft.linearVelocity.metersPerSecond, backRight.linearVelocity.metersPerSecond)

    override fun drive(speeds: ChassisSpeeds) {
        drive(speeds, false)
    }

    fun drive(speeds: ChassisSpeeds, fieldRelative: Boolean = false) {
        if (fieldRelative) {
            val adjusted = ChassisSpeeds.fromFieldRelativeSpeeds(speeds.vxMetersPerSecond, speeds.vyMetersPerSecond, speeds.omegaRadiansPerSecond, gyro.heading)
            drive(kinematics.toWheelSpeeds(adjusted))
        }
        else drive(kinematics.toWheelSpeeds(speeds))
    }

    fun drive(speeds: MecanumDriveWheelSpeeds) {
        if (maxVelocity.value > 0)
            speeds.normalize(maxVelocity.metersPerSecond)
        frontLeft.linearVelocity = speeds.frontLeftMetersPerSecond.metersPerSecond
        frontRight.linearVelocity = speeds.frontRightMetersPerSecond.metersPerSecond
        backLeft.linearVelocity = speeds.rearLeftMetersPerSecond.metersPerSecond
        backRight.linearVelocity = speeds.rearRightMetersPerSecond.metersPerSecond
    }

    override fun periodic() {
        odometry.update(gyro.heading, mecanumDriveWheelSpeeds)
    }

    override fun debug() {
        println(motors.map { it.linearVelocity }.toString())
    }

    override var pose: Pose2d = zeroPose
        get() = odometry.poseMeters
    override var heading: KRotation = 0.degrees
        get() = gyro.heading
}