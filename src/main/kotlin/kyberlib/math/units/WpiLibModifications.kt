package kyberlib.math.units

import edu.wpi.first.wpilibj.geometry.Pose2d
import edu.wpi.first.wpilibj.geometry.Rotation2d
import edu.wpi.first.wpilibj.geometry.Transform2d
import edu.wpi.first.wpilibj.geometry.Translation2d
import edu.wpi.first.wpilibj.kinematics.ChassisSpeeds
import kyberlib.math.units.extensions.*
import kotlin.math.sqrt

val Pose2d.string: String
    get() = "pose(${this.x}, ${this.y}, ${this.rotation.degrees})"
fun Pose2d(x: Length, y:Length, rotation: Angle): Pose2d = Pose2d(x.meters, y.meters, rotation)
val Pose2d.transform: Transform2d
    get() = this.minus(zeroPose)

val zeroPose = Pose2d(0.0, 0.0, 0.degrees)

val Translation2d.string: String
    get() = "trans(${this.x}, ${this.y})"

fun Translation2d(x: Length, y:Length): Translation2d = Translation2d(x.meters, y.meters)
fun Translation2d(x: Length, rotation: Rotation2d): Translation2d = Translation2d(x.meters, rotation)
val zeroTranslation = Translation2d(0.0, 0.0)
fun Translation2d.towards(translation2d: Translation2d): Rotation2d {
    val dif = translation2d.minus(this)
    return Rotation2d(dif.x, dif.y)
}

val ChassisSpeeds.speed: LinearVelocity
        get() = sqrt(vxMetersPerSecond * vxMetersPerSecond + vyMetersPerSecond * vyMetersPerSecond).metersPerSecond